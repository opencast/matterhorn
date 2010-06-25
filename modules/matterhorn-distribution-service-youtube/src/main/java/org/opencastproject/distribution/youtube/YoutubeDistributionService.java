/**
 *  Copyright 2009, 2010 The Regents of the University of California
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
package org.opencastproject.distribution.youtube;

import org.opencastproject.deliver.schedule.Schedule;
import org.opencastproject.deliver.schedule.Task;
import org.opencastproject.deliver.youtube.YouTubeConfiguration;
import org.opencastproject.deliver.youtube.YouTubeDeliveryAction;
import org.opencastproject.deliver.youtube.YouTubeRemoveAction;
import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.remote.api.RemoteServiceManager;
import org.opencastproject.workspace.api.Workspace;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;


/**
 * Distributes media to a Youtube play list.
 */
public class YoutubeDistributionService implements DistributionService {
  /** logger instance */
  private static final Logger logger = LoggerFactory.getLogger(YoutubeDistributionService.class);

  /** workspace instance */
  protected Workspace workspace = null;

  /** Youtube configuration instance */
  private static YouTubeConfiguration config = null;

  /** playlist ID */
  private static String destination;
  
  /** URL for uploading */
  private static final String uploadURL = "http://uploads.gdata.youtube.com/feeds/api/users/default/uploads";
  
  /** category of the video - default to "Education" */
  private static final String category = "Education";
  
  /** prefix of the URL generated by gdata API */
  private static final String gdataURLPrefix = 
    "http://gdata.youtube.com/feeds/api/users/utubedelivery/uploads/";
  
  /** prefix of the Youtube URL */
  private static final String youtubeURLPrefix = "http://www.youtube.com/watch?v=";
  
  /** only one scheduler instance for this service */
  private static Schedule schedule;

  /** context strategy for the distribution service */
  YoutubeDistributionContextStrategy contextStrategy;

  /** the remote services registry */
  protected RemoteServiceManager remoteServiceManager;

  /** this server's base URL */
  protected String serverUrl = null;

  /* the configured id for this distribution channel */
  protected String distChannelId = null;

  /**
   * Called when service activates. Defined in OSGi resource file.
   */
  public void activate(ComponentContext cc) {
    String username = cc.getBundleContext().getProperty("youtube.username"); // "utubedelivery"
    String password = cc.getBundleContext().getProperty("youtube.password"); // "utubedelivery"
    String clientid = cc.getBundleContext().getProperty("youtube.clientid"); // "abcde"
    String developerkey = cc.getBundleContext().getProperty("youtube.developerkey");
    // "AI39si7bx2AbnOM6RM8J7mdrljfZCzisYzDkqvIqEjV3zjbqQIr6-u_bg3R0MLAVVXLqKjSsxu4ReytWFn7ylIlDk6OC7pdXpQ"

    config = YouTubeConfiguration.getInstance();
    // client ID may not be necessary
    config.setClientId(clientid);
    config.setDeveloperKey(developerkey);
    config.setUploadUrl(uploadURL);
    config.setUserId(username);
    config.setPassword(password);
    config.setCategory(category);

    // create context strategy
    contextStrategy = new YoutubeDistributionContextStrategy();
    // default destination
    destination = cc.getBundleContext().getProperty("youtube.playlist"); // "B8B47104C2C1663B"

    // create the scheduler using file system store
    String directory_name = cc.getBundleContext().getProperty("youtube.task");
    if (directory_name == null || directory_name.equals("")) {
      directory_name = "/tmp/youtube";
    }
    logger.info("Task file directory: {}" + directory_name);
    File data_directory = new File(directory_name);
    data_directory.mkdirs();
    schedule = new Schedule(data_directory);

    serverUrl = (String)cc.getBundleContext().getProperty("org.opencastproject.server.url");
    distChannelId = (String)cc.getProperties().get("distribution.channel");
    remoteServiceManager.registerService(JOB_TYPE_PREFIX + distChannelId, serverUrl);
  }

  /**
   * Called when service deactivates. Defined in OSGi resource file.
   */
  public void deactivate() {
    // unregister as a handler for youtube distribution
    remoteServiceManager.unRegisterService(JOB_TYPE_PREFIX + distChannelId, serverUrl);

    // shutdown the scheduler
    schedule.shutdown();
  }

  /**
   * Gets task name given the media package ID and the track ID.
   *
   * @param mediaPackge ID of the package
   * @param track ID of the track
   * @return task identifier
   */  
  private String getTaskID(String mediaPackage, String track) {
    // use "YOUTUBE" + media package identifier + track identifier as task identifier
    return "YOUTUBE-" + mediaPackage.replaceAll("\\.", "-") + "-" + track;
  }

  /**
   * Removes the media delivered by the given task.
   *
   * @param name task identifier
   */  
  private void remove(String name) throws DistributionException {
     logger.info("Publish task: {}", name);
     
     YouTubeRemoveAction ract = new YouTubeRemoveAction();
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
   * {@inheritDoc}
   * @see org.opencastproject.distribution.api.DistributionService#distribute(org.opencastproject.mediapackage.MediaPackage, java.lang.String[])
   */
  @Override
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
      
      YouTubeDeliveryAction act = new YouTubeDeliveryAction();
      act.setName(name);
      act.setTitle(sourceFile.getName());
      // CHNAGE ME: set metadata elements here
      act.setTags(new String [] {"whatever"});
      act.setAbstract("Opencast Distribution Service - Youtube");
      act.setMediaPath(sourceFile.getAbsolutePath());

      // get playlist ID from context strategy
      String contextDestination = contextStrategy.getContextName(mediaPackage);
      if (contextDestination != null) {
        // use the destination from context strategy
        destination = contextDestination;
      }

      // deliver to a play list
      act.setDestination(destination); // FIXME: replace this with a playlist based on the episode's series

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
            String videoURL = act.getEntryUrl();
            // convert the entry URL to a user-oriented URL
            videoURL = videoURL.replace("?client=" + config.getClientId(), "");
            videoURL = videoURL.replace(gdataURLPrefix, youtubeURLPrefix);
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
      e.printStackTrace();
      throw new RuntimeException(e);
    } finally {
    }

    return mediaPackage;
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  public void setRemoteServiceManager(RemoteServiceManager remoteServiceManager) {
    this.remoteServiceManager = remoteServiceManager;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.distribution.api.DistributionService#retract(org.opencastproject.mediapackage.MediaPackage)
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
