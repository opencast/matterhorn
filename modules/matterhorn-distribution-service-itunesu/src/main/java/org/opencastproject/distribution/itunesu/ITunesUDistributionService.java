/**
 *  Copyright 2009 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.distribution.itunesu;

import org.opencastproject.deliver.store.JPAStore;
import org.opencastproject.deliver.schedule.TaskSerializer;

import org.opencastproject.deliver.itunesu.ITunesConfiguration;
import org.opencastproject.deliver.itunesu.ITunesDeliveryAction;
import org.opencastproject.deliver.itunesu.ITunesRemoveAction;
import org.opencastproject.deliver.schedule.Schedule;
import org.opencastproject.deliver.schedule.Task;
import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.workspace.api.Workspace;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;


/**
 * Distributes media to a iTunes U group.
 */
public class ITunesUDistributionService implements DistributionService {
  /** logger instance */
  private static final Logger logger = LoggerFactory.getLogger(ITunesUDistributionService.class);
  /** workspace instance */
  protected Workspace workspace = null;

  /** iTunes configuration instance */
  private static ITunesConfiguration config = null;
  /** group handle */
  private static String destination;
  /** only one scheduler instance for this service */
  private static Schedule schedule;
  
  /** context strategy for the distribution service */
  ITunesUDistributionContextStrategy contextStrategy;

  /** The JPA provider */
  protected PersistenceProvider persistenceProvider;

  /**
   * @param persistenceProvider the persistenceProvider to set
   */
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }
  
  @SuppressWarnings("unchecked")
  protected Map persistenceProperties;

  /**
   * @param persistenceProperties the persistenceProperties to set
   */
  @SuppressWarnings("unchecked")
  public void setPersistenceProperties(Map persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

  /** The entity manager used for persisting Java objects. */
  protected EntityManager em = null;

  /** The factory used to generate the entity manager */
  protected EntityManagerFactory emf = null;

  /**
   * Called when service activates. Defined in OSGi resource file.
   */
  public void activate(ComponentContext cc) {
    String siteURL = cc.getBundleContext().getProperty("itunesu.siteurl");
    String administratorCredential = cc.getBundleContext().getProperty("itunesu.credential");
    String sharedSecret = cc.getBundleContext().getProperty("itunesu.sharedsecret");

    // create context strategy
    contextStrategy = new ITunesUDistributionContextStrategy();
    // default destination
    destination = cc.getBundleContext().getProperty("itunesu.group");

    config = ITunesConfiguration.getInstance();
    // client ID may not be necessary
    config.setSiteURL(siteURL);
    config.setAdministratorCredential(administratorCredential);
    config.setSharedSecret(sharedSecret);

    // create JPA instances
    emf = persistenceProvider.createEntityManagerFactory("ITunesUDistributionPersistenceUnit", 
                                                         persistenceProperties);
    em = emf.createEntityManager();

    // create the scheduler using memory store
    // schedule = new Schedule();
    // create the scheduler using JPA store
    schedule = new Schedule(new JPAStore<Task>(new TaskSerializer(), em, "ITUNESU_ACTIVE"),
                            new JPAStore<Task>(new TaskSerializer(), em, "ITUNESU_COMPLETED"));
  }

  /**
   * Called when service deactivates. Defined in OSGi resource file.
   */
  public void deactivate() {
    // shutdown the scheduler
    schedule.shutdown();
    // destroy JPA instances
    em.close();
    emf.close();
  }

  /**
   * Gets task name given the media package ID and the track ID.
   *
   * @param mediaPackge ID of the package
   * @param track ID of the track
   * @return task identifier
   */  
  private String getTaskID(String mediaPackage, String track) {
    // use "ITUNESU" + media package identifier + track identifier as task identifier
    return "ITUNESU_" + mediaPackage + "_" + track;
  }

  /**
   * Removes the media delivered by the given task.
   *
   * @param name task identifier
   */  
  private void remove(String name) throws DistributionException {
     logger.info("Publish task: {}", name);
     
     ITunesRemoveAction ract = new ITunesRemoveAction();
     ract.setName(name + "_r");
     ract.setPublishTask(name);
     schedule.start(ract);

     while (true) {
       Task rTask = schedule.getTask(name + "_r");
       synchronized (rTask) {
         Task.State state = rTask.getState();
         if (state == Task.State.INITIAL || state == Task.State.ACTIVE) {
           try {
             Thread.sleep(1000L);
           } catch (Exception e) {
             throw new RuntimeException(e);
           }
           // still running
           continue;
         }
         else if (state == Task.State.COMPLETE) {
           logger.info("Succeeded retracting media");
           break;
         }
         else if (state == Task.State.FAILED) {
           // fail to remove
           throw new DistributionException("Failed to remove media");
         }
      } // end of synchronized
    } // end of schedule loop
  }

  /**
   * Uploads media files to iTunes U under a group. {@inheritDoc}
   * 
   * @see org.opencastproject.distribution.api.DistributionService#distribute(org.opencastproject.media.mediapackage.MediaPackage)
   */
  public MediaPackage distribute(MediaPackage mediaPackage, String... elementIds) throws DistributionException {

    try {
      String trackID = "";
      MediaPackageElement element = null;
      for (String id : elementIds) {
        element = mediaPackage.getElementById(id);
        switch (element.getElementType()) {
        case Track:
          trackID = id;
          break;
        case Catalog:
          continue;
        case Attachment:
          continue;
        default:
          throw new IllegalStateException("Someone is trying to distribute strange things here");
        }
      }

      File sourceFile = workspace.get(element.getURI());
      if( ! sourceFile.exists() || ! sourceFile.isFile()) {
        throw new IllegalStateException("Could not retrieve a file for element " + element.getIdentifier());
      }
      
      // get task name
      String name = getTaskID(mediaPackage.getIdentifier().compact(), trackID);

      // check if the file has already been delivered
      Task savedTask = schedule.getSavedTask(name);

      if (savedTask != null && savedTask.getState() == Task.State.COMPLETE) {
          // has been successfully delivered
          // remove the media
          remove(name);
      }
      
      ITunesDeliveryAction act = new ITunesDeliveryAction();
      act.setName(name);
      act.setTitle(sourceFile.getName());
      // CHNAGE ME: set metadata elements here
      act.setTags(new String [] {"whatever"});
      act.setAbstract("Opencast Distribution Service - iTunes U");
      act.setMediaPath(sourceFile.getAbsolutePath());

      // get playlist ID from context strategy
      String contextDestination = contextStrategy.getContextName(mediaPackage);
      if (contextDestination != null) {
        // use the destination from context strategy
        destination = contextDestination;
      }

      // deliver to a tab
      act.setDestination(destination); // FIXME: replace this with a tab based on the episode's series

      logger.info("Delivering from {}", sourceFile.getAbsolutePath());

      // start the scheduler
      schedule.start(act);
      
      while (true) {
        Task task = schedule.getTask(name);
        synchronized (task) {
          Task.State state = task.getState();
          if (state == Task.State.INITIAL || state == Task.State.ACTIVE) {
            try {
              Thread.sleep(1000L);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
            // still running
            continue;
          }
          else if (state == Task.State.COMPLETE) {
            logger.info("Succeeded delivering from {}", sourceFile.getAbsolutePath());
            String videoURL = act.getTrackURL();
            URI newTrackUri = new URI(videoURL);
            MediaPackageElement newElement = 
                MediaPackageElementBuilderFactory.newInstance().newElementBuilder().elementFromURI(
                newTrackUri, element.getElementType(), element.getFlavor());
            newElement.setIdentifier(element.getIdentifier() + "-dist");
            mediaPackage.addDerived(newElement, element);
            break;
          }
          else if (state == Task.State.FAILED) {
            logger.info("Failed delivering from {}", sourceFile.getAbsolutePath());
            break;
          }
        }
      } // end of schedule loop
    } catch (Exception e) {
      throw new DistributionException(e);
    } finally {
    }

    return mediaPackage;
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.distribution.api.DistributionService#retract(org.opencastproject.media.mediapackage.MediaPackage)
   */
  @Override
  public void retract(MediaPackage mediaPackage) throws DistributionException {
    // throw new UnsupportedOperationException();
    String trackID = "";
    
    for (MediaPackageElement element : mediaPackage.getElements()) {
      switch (element.getElementType()) {
      case Track:
        trackID = element.getIdentifier();
        break;
      case Catalog:
        continue;
      case Attachment:
        continue;
      default:
        throw new IllegalStateException("Someone is trying to distribute strange things here");
      }
    }
  
    // get task name
    String name = getTaskID(mediaPackage.getIdentifier().compact(), trackID);

    // check if the file has already been delivered
    Task savedTask = schedule.getSavedTask(name);

    if (savedTask == null || savedTask.getState() != Task.State.COMPLETE) {
        // has not been successfully delivered
         throw new DistributionException("Media has not been distributed!");
    }

    // remove the media
    remove(name);
  }
}
