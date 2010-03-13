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
package org.opencastproject.conductor.impl;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.Receipt;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.media.mediapackage.UnsupportedElementException;
import org.opencastproject.media.mediapackage.selector.AudioVisualElementSelector;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

/**
 * The workflow definition for handling "compose" operations
 */
public class ComposeWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ComposeWorkflowOperationHandler.class);

  /** The composer service */
  private ComposerService composerService = null;

  /** Remote services handle (R-OSGI) */
//  private RemoteOSGiService remote = null;

//  /** Available composer services */
//  private ComposerService[] allComposerServices;

  /** Configuration handle for remote servers */
  public static final String CONFIG_REMOTE_SERVERS = "remote.servers";

  /**
   * Callback for the OSGi declarative services configuration.
   * 
   * @param composerService
   *          the local composer service
   */
  protected void setComposerService(ComposerService composerService) {
    this.composerService = composerService;
  }

  /**
   * Callback for the OSGi declarative services configuration.
   * 
   * @param remote
   *          the R-OSGI wraper service for remote services
   */
//  protected void setRemoteServices(RemoteOSGiService remote) {
//    this.remote = remote;
//    listAllComposerServices();
//  }

  protected void listAllComposerServices() {
//    ArrayList<ComposerService> tempAllComposerServices = new ArrayList<ComposerService>();

    // add local composer service
//    tempAllComposerServices.add(localComposerService);
//    allComposerServices = new ComposerService[] { localComposerService };

    // get remote servers list from config
    Properties serversProperties = new Properties();
    try {
      serversProperties.load(getClass().getClassLoader().getResourceAsStream("config/remote.servers.properties"));
    } catch (IOException e) {
      logger.warn("Unable to load configuration for remote servers");
      return;
    }
    String composerServicesList = serversProperties.getProperty("remote.servers");
    if (composerServicesList == null) {
      logger.warn("Remote Composer servers are not configured");
      return;
    }

    // try to connect to remote servers and add remote services to list
//    String[] allServicesTxtArr = composerServicesList.split(",");
//    String s = "";
//    for (int i = 0; i < allServicesTxtArr.length; i++) {
//      try {
//        s = allServicesTxtArr[i];
//        URI uri = new URI(s);
//        remote.connect(uri);
//        RemoteServiceReference[] remoteServer = remote.getRemoteServiceReferences(uri, ComposerService.class.getName(),
//                null);
//        for (RemoteServiceReference r : remoteServer) {
//          tempAllComposerServices.add((ComposerService) remote.getRemoteService(r));
//        }
//      } catch (Exception e) {
//        logger.warn("Remote server:'" + s + "' could not be accessed!");
//      }
//    }
//
//    allComposerServices = (ComposerService[]) tempAllComposerServices
//            .toArray(new ComposerService[tempAllComposerServices.size()]);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance)
   */
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance) throws WorkflowOperationException {
    logger.debug("Running compose workflow operation on workflow {}", workflowInstance.getId());

    // Encode the media package
    MediaPackage resultingMediaPackage = null;
    try {
      resultingMediaPackage = encode(workflowInstance.getMediaPackage(), workflowInstance.getCurrentOperation());
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }

    logger.debug("Compose operation completed");

    return WorkflowBuilder.getInstance().buildWorkflowOperationResult(resultingMediaPackage, Action.CONTINUE);
  }

  /**
   * Encode tracks from MediaPackage using profiles stored in properties and updates current MediaPackage.
   * 
   * @param src
   *          The source media package
   * @param properties
   * @return
   * @throws EncoderException
   * @throws MediaPackageException
   * @throws UnsupportedElementException
   * @throws ExecutionException
   * @throws InterruptedException
   */
  private MediaPackage encode(MediaPackage src, WorkflowOperationInstance operation) throws EncoderException,
          MediaPackageException, UnsupportedElementException, InterruptedException, ExecutionException {
    MediaPackage mediaPackage = (MediaPackage) src.clone();
    // Read the configuration properties
    String sourceVideoFlavor = StringUtils.trimToNull(operation.getConfiguration("source-video-flavor"));
    String sourceAudioFlavor = StringUtils.trimToNull(operation.getConfiguration("source-audio-flavor"));
    String targetTrackTags = StringUtils.trimToNull(operation.getConfiguration("target-tags"));
    String targetTrackFlavor = StringUtils.trimToNull(operation.getConfiguration("target-flavor"));
    String encodingProfileName = StringUtils.trimToNull(operation.getConfiguration("encoding-profile"));

    if (sourceAudioFlavor == null && sourceVideoFlavor == null)
      throw new IllegalStateException("either source audio flavor or source video flavor or both must be specified");

    // Find the encoding profile
    EncodingProfile profile = null;
    for (EncodingProfile p : composerService.listProfiles()) {
      if (p.getIdentifier().equals(encodingProfileName)) {
        profile = p;
        break;
      }
    }
    if (profile == null) {
      throw new IllegalStateException("Encoding profile '" + encodingProfileName + "' was not found");
    }

    // Depending on the input type of the profile and the configured flavors and
    // tags,
    // make sure we have the required tracks:
    AudioVisualElementSelector avSelector = new AudioVisualElementSelector();
    avSelector.setVideoFlavor(sourceVideoFlavor);
    avSelector.setAudioFlavor(sourceAudioFlavor);
    switch (profile.getApplicableMediaType()) {
    case Stream:
      avSelector.setRequireVideoTrack(false);
      avSelector.setRequireAudioTrack(false);
      break;
    case AudioVisual:
      avSelector.setRequireVideoTrack(true);
      avSelector.setRequireAudioTrack(true);
      break;
    case Visual:
      avSelector.setRequireVideoTrack(true);
      avSelector.setRequireAudioTrack(false);
      break;
    case Audio:
      avSelector.setRequireVideoTrack(false);
      avSelector.setRequireAudioTrack(true);
      break;
    default:
      logger.warn("Don't know if the current track is applicable to encoding profile '" + profile + "' based on type "
              + profile.getApplicableMediaType());
      return mediaPackage;
    }
    Collection<Track> tracks = avSelector.select(mediaPackage);

    String sourceVideoTrackId = null;
    String sourceAudioTrackId = null;

    // Did we get the set of tracks that we need?
    if (tracks.size() == 0) {
      logger.debug("Skipping encoding of media package to '{}': no suitable input tracks found", profile);
      return mediaPackage;
    } else {
      for (Track t : tracks) {
        if (sourceVideoTrackId == null && t.hasVideo()) {
          sourceVideoTrackId = t.getIdentifier();
        }
        if (sourceAudioTrackId == null && t.hasAudio()) {
          sourceAudioTrackId = t.getIdentifier();
        }
      }
    }

    // Don't pass the same track as audio *and* video
    if (sourceVideoTrackId != null && sourceVideoTrackId.equals(sourceAudioTrackId)) {
      switch (profile.getOutputType()) {
      case Audio:
        sourceVideoTrackId = null;
        break;
      default:
        sourceAudioTrackId = null;
        break;
      }
    }

    // choose composer service with least running jobs
//    listAllComposerServices();
//    ComposerService cs = allComposerServices[0];
//    for (ComposerService c : allComposerServices) {
//      if (c.countJobs() < cs.countJobs()) {
//        cs = c;
//      }
//    }
//    logger.debug("Media will be encoded on {}", cs.toString());

    // Start encoding and wait for the result
    final Receipt receipt = composerService.encode(mediaPackage, sourceVideoTrackId, sourceAudioTrackId, profile
            .getIdentifier(), true);
    Track composedTrack = (Track)receipt.getElement();
    updateTrack(composedTrack, operation, profile);

    // store new tracks to mediaPackage
    // FIXME derived media comes from multiple sources, so how do we choose
    // which is the "parent" of the derived
    // media?
    String parentId = sourceVideoTrackId == null ? sourceAudioTrackId : sourceVideoTrackId;
    mediaPackage.addDerived(composedTrack, mediaPackage.getElementById(parentId));

    // Add the flavor and tags, if specified
    if(targetTrackFlavor != null) {
      MediaPackageElementFlavor targetFlavor = MediaPackageElementFlavor.parseFlavor(targetTrackFlavor);
      composedTrack.setFlavor(targetFlavor);
    }
    if(targetTrackTags != null) {
      String[] tags = targetTrackTags.split("\\W");
      if(tags.length > 0) for(String tag : tags) composedTrack.addTag(tag);
    }
    return mediaPackage;
  }

  // Update the newly composed track with metadata
  private void updateTrack(Track composedTrack, WorkflowOperationInstance operation, EncodingProfile profile) {
    // Read the configuration properties
    String targetTrackTags = StringUtils.trimToNull(operation.getConfiguration("target-tags"));
    String targetTrackFlavor = StringUtils.trimToNull(operation.getConfiguration("target-flavor"));

    if (composedTrack == null)
      throw new RuntimeException("unable to retrieve composed track");

    // Add the flavor, either from the operation configuration or from the
    // composer
    if (targetTrackFlavor != null)
      composedTrack.setFlavor(MediaPackageElementFlavor.parseFlavor(targetTrackFlavor));
    logger.debug("Composed track has flavor '{}'", composedTrack.getFlavor());

    // Set the mimetype
    if (profile.getMimeType() != null)
      composedTrack.setMimeType(MimeTypes.parseMimeType(profile.getMimeType()));

    // Add tags
    if (targetTrackTags != null) {
      for (String tag : targetTrackTags.split("\\W")) {
        if (StringUtils.trimToNull(tag) == null)
          continue;
        logger.trace("Tagging composed track with '{}'", tag);
        composedTrack.addTag(tag);
      }
    }
  }
}
