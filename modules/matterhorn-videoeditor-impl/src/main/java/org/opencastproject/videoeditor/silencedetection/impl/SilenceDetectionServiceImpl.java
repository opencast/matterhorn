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
package org.opencastproject.videoeditor.silencedetection.impl;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;
import javax.xml.bind.JAXBException;
import org.gstreamer.Gst;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.videoeditor.api.ProcessFailedException;
import org.opencastproject.videoeditor.gstreamer.exceptions.PipelineBuildException;
import org.opencastproject.videoeditor.silencedetection.api.SilenceDetectionService;
import org.opencastproject.videoeditor.silencedetection.gstreamer.GstreamerSilenceDetector;
import org.opencastproject.workspace.api.Workspace;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of SilenceDetectionService using Gstreamer framework.
 */
public class SilenceDetectionServiceImpl extends AbstractJobProducer implements SilenceDetectionService, ManagedService {

  /**
   * The logging instance
   */
  private static final Logger logger = LoggerFactory.getLogger(SilenceDetectionServiceImpl.class);
  
  private static final String JOB_TYPE = "org.opencastproject.videoeditor.silencedetection";

  private static enum Operation {
    SILENCE_DETECTION
  }
  
  /** Reference to the workspace service */
  private Workspace workspace = null;
  
  /** Reference to the receipt service */
  private ServiceRegistry serviceRegistry;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  /** The security service */
  protected SecurityService securityService = null;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;
  
  private Properties properties;

  public SilenceDetectionServiceImpl() {
    super(JOB_TYPE);
  }

  /**
   * Run silence detection on the source track and returns {@see org.opencastproject.videoeditor.silencedetection.api.MediaSegments} XML as string.
   * Source track should have an audio stream. 
   * All detected {@see org.opencastproject.videoeditor.silencedetection.api.MediaSegment}s (one or more) are non silent sequences.
   * 
   * @param job processing job
   * @param track track where to run silence detection
   * @return {@see MediaSegments} Xml as String
   * @throws ProcessFailedException if an error occures
   */
  protected String detect(Job job, Track track) throws ProcessFailedException {
    try {
      String filePath = workspace.get(track.getURI()).getAbsolutePath();
      GstreamerSilenceDetector silenceDetector = new GstreamerSilenceDetector(properties, track.getIdentifier(), filePath);
      silenceDetector.runDetection();
      return silenceDetector.getMediaSegments().toXml();
      
    } catch (JAXBException ex) {
      throw new ProcessFailedException("Unable to serialize media segments!");
    } catch (PipelineBuildException ex) {
      throw new ProcessFailedException("Unable to build detection Pipeline!");
//    } catch (NotFoundException ex) {
//      throw new ProcessFailedException(ex.getMessage());
//    } catch (IOException ex) {
//      throw new ProcessFailedException(ex.getMessage());
    } catch (Exception ex) {
      throw new ProcessFailedException(ex.getMessage());
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.videoeditor.silencedetection.api.SilenceDetectionService#detect(org.opencastproject.mediapackage.Track) 
   */
  @Override
  public Job detect(Track track) throws ProcessFailedException {
    try {
      if (track == null) {
        throw new ProcessFailedException("Track is null!");
      }

      String trackXML = MediaPackageElementParser.getAsXml(track);
      return serviceRegistry.createJob(
              getJobType(), 
              Operation.SILENCE_DETECTION.toString(), 
              Arrays.asList(trackXML));

    } catch (ServiceRegistryException ex) {
      throw new ProcessFailedException("Unable to create job! " + ex.getMessage());
    } catch (MediaPackageException ex) {
      throw new ProcessFailedException("Unable to serialize track!");
    }
  }

  @Override
  protected String process(Job job) throws Exception {
    if (Operation.SILENCE_DETECTION.toString().equals(job.getOperation())) {
      String trackXml = job.getArguments().get(0);
      if (trackXml == null) {
        throw new ProcessFailedException("Track not set!");
      }

      Track track = (Track) MediaPackageElementParser.getFromXml(trackXml);
      return detect(job, track);
    }

    throw new ProcessFailedException("Can't handle this operation: " + job.getOperation());
  }

  @Override
  protected ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  @Override
  protected SecurityService getSecurityService() {
    return securityService;
  }

  @Override
  protected UserDirectoryService getUserDirectoryService() {
    return userDirectoryService;
  }

  @Override
  protected OrganizationDirectoryService getOrganizationDirectoryService() {
    return organizationDirectoryService;
  }
  
  protected void activate(ComponentContext context) {
    logger.debug("activating...");
    Gst.setUseDefaultContext(true);
    Gst.init();
  }

  protected void deactivate(ComponentContext context) {
    logger.debug("deactivating...");
  }

  @Override
  public void updated(Dictionary properties) throws ConfigurationException {
    this.properties = new Properties();
    Enumeration keys = properties.keys();
    while (keys.hasMoreElements()) {
      Object key = keys.nextElement();
      this.properties.put(key, properties.get(key));
    }
    logger.debug("Properties updated!");
  }
  
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }
  
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }
  
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }
  
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }
  
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }
}
