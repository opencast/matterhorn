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
package org.opencastproject.composer.impl;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderEngine;
import org.opencastproject.composer.api.EncoderEngineFactory;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.identifier.IdBuilder;
import org.opencastproject.mediapackage.identifier.IdBuilderFactory;
import org.opencastproject.remote.api.Receipt;
import org.opencastproject.remote.api.RemoteServiceManager;
import org.opencastproject.remote.api.Receipt.Status;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Default implementation of the composer service api.
 */
public class ComposerServiceImpl implements ComposerService {

  /** The logging instance */
  private static final Logger logger = LoggerFactory.getLogger(ComposerServiceImpl.class);

  /** The collection name */
  public static final String COLLECTION = "composer";

  /** Encoding profile manager */
  private EncodingProfileScanner profileScanner = null;

  /** Reference to the media inspection service */
  private MediaInspectionService inspectionService = null;

  /** Reference to the workspace service */
  private Workspace workspace = null;

  /** Reference to the receipt service */
  private RemoteServiceManager remoteServiceManager;

  /** Reference to the encoder engine factory */
  private EncoderEngineFactory encoderEngineFactory;

  /** Id builder used to create ids for encoded tracks */
  private final IdBuilder idBuilder = IdBuilderFactory.newInstance().newIdBuilder();

  /** Thread pool */
  private ExecutorService executor = null;

  /** The server's base URL */
  private String serverUrl = UrlSupport.DEFAULT_BASE_URL;

  /** The configuration property containing the number of concurrent encoding threads to run */
  public static final String CONFIG_THREADS = "composer.threads";

  /** The default number of concurrent encoding threads to run */
  public static final int DEFAULT_THREADS = 2;

  /**
   * Sets the media inspection service
   * 
   * @param mediaInspectionService
   *          an instance of the media inspection service
   */
  public void setMediaInspectionService(MediaInspectionService mediaInspectionService) {
    this.inspectionService = mediaInspectionService;
  }

  /**
   * Sets the encoder engine factory
   * 
   * @param encoderEngineFactory
   *          The encoder engine factory
   */
  public void setEncoderEngineFactory(EncoderEngineFactory encoderEngineFactory) {
    this.encoderEngineFactory = encoderEngineFactory;
  }

  /**
   * Sets the workspace
   * 
   * @param workspace
   *          an instance of the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Sets the receipt service
   * 
   * @param remoteServiceManager
   */
  public void setRemoteServiceManager(RemoteServiceManager remoteServiceManager) {
    this.remoteServiceManager = remoteServiceManager;
  }

  public void setProfileScanner(EncodingProfileScanner scanner) {
    this.profileScanner = scanner;
  }

  /**
   * Activator that will make sure the encoding profiles are loaded.
   */
  protected void activate(ComponentContext cc) {
    // set up threading
    int threads = -1;
    String configredThreads = (String) cc.getBundleContext().getProperty(CONFIG_THREADS);
    // try to parse the value as a number. If it fails to parse, there is a config problem so we throw an exception.
    if (configredThreads == null) {
      threads = DEFAULT_THREADS;
    } else {
      threads = Integer.parseInt(configredThreads);
    }
    if (threads < 1) {
      throw new IllegalStateException("The composer needs one or more threads to function.");
    }
    setExecutorThreads(threads);

    serverUrl = (String) cc.getBundleContext().getProperty("org.opencastproject.server.url");
    // Register as a handler
    remoteServiceManager.registerService(JOB_TYPE, serverUrl);
  }

  /** Separating this from the activate method so it's easier to test */
  void setExecutorThreads(int threads) {
    executor = Executors.newFixedThreadPool(threads);
    logger.info("Thread pool size = {}", threads);
  }

  protected void deactivate() {
    remoteServiceManager.unRegisterService(JOB_TYPE, serverUrl);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#encode(org.opencastproject.mediapackage.Track,
   *      java.lang.String)
   */
  @Override
  public Receipt encode(Track sourceTrack, String profileId) throws EncoderException {
    return encode(sourceTrack, profileId, false);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#encode(org.opencastproject.mediapackage.MediaPackage,
   *      java.lang.String, java.lang.String, boolean)
   */
  @Override
  public Receipt encode(Track sourceTrack, String profileId, boolean block) throws EncoderException {
    return encode(sourceTrack, null, profileId, block);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#mux(org.opencastproject.mediapackage.Track,
   *      org.opencastproject.mediapackage.Track, java.lang.String)
   */
  @Override
  public Receipt mux(Track sourceVideoTrack, Track sourceAudioTrack, String profileId) throws EncoderException {
    return encode(sourceVideoTrack, sourceAudioTrack, profileId, false);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#mux(org.opencastproject.mediapackage.Track,
   *      org.opencastproject.mediapackage.Track, java.lang.String, boolean)
   */
  @Override
  public Receipt mux(final Track videoTrack, final Track audioTrack, final String profileId, final boolean block)
          throws EncoderException {
    return encode(videoTrack, audioTrack, profileId, block);
  }

  /**
   * Encodes audio and video track to a file. If both an audio and a video track are given, they are muxed together into
   * one movie container.
   * @param videoTrack
   *          the video track
   * @param audioTrack
   *          the audio track
   * @param profileId
   *          the encoding profile
   * @param block
   *          <code>true</code> to only return once encoding is finished
   * 
   * @return the receipt
   * @throws EncoderException
   *           if encoding fails
   */
  private Receipt encode(final Track videoTrack, final Track audioTrack, final String profileId, final boolean block)
          throws EncoderException {

    final String targetTrackId = idBuilder.createNew().toString();
    final Receipt composerReceipt = remoteServiceManager.createReceipt(JOB_TYPE);

    // Get the tracks and make sure they exist
    final File audioFile;
    if (audioTrack == null) {
      audioFile = null;
    } else {
      try {
        audioFile = workspace.get(audioTrack.getURI());
      } catch (NotFoundException e) {
        composerReceipt.setStatus(Status.FAILED);
        remoteServiceManager.updateReceipt(composerReceipt);
        throw new EncoderException("Requested audio track " + audioTrack + " is not found");
      } catch (IOException e) {
        composerReceipt.setStatus(Status.FAILED);
        remoteServiceManager.updateReceipt(composerReceipt);
        throw new EncoderException("Unable to access audio track " + audioTrack);
      }
    }

    final File videoFile;
    if (videoTrack == null) {
      videoFile = null;
    } else {
      try {
        videoFile = workspace.get(videoTrack.getURI());
      } catch (NotFoundException e) {
        composerReceipt.setStatus(Status.FAILED);
        remoteServiceManager.updateReceipt(composerReceipt);
        throw new EncoderException("Requested video track " + videoTrack + " is not found");
      } catch (IOException e) {
        composerReceipt.setStatus(Status.FAILED);
        remoteServiceManager.updateReceipt(composerReceipt);
        throw new EncoderException("Unable to access audio track " + audioTrack);
      }
    }

    // Create the engine
    final EncodingProfile profile = profileScanner.getProfile(profileId);
    if (profile == null) {
      composerReceipt.setStatus(Status.FAILED);
      remoteServiceManager.updateReceipt(composerReceipt);
      throw new EncoderException(null, "Profile '" + profileId + " is unkown");
    }
    final EncoderEngine encoderEngine = encoderEngineFactory.newEncoderEngine(profile);
    if (encoderEngine == null) {
      composerReceipt.setStatus(Status.FAILED);
      remoteServiceManager.updateReceipt(composerReceipt);
      throw new EncoderException(null, "No encoder engine available for profile '" + profileId + "'");
    }

    Runnable runnable = new Runnable() {
      public void run() {
        
        if (audioTrack != null && videoTrack != null)
          logger.info("Muxing audio track {} and video track {} into {}", new String[] {
                  audioTrack.getIdentifier(), videoTrack.getIdentifier(), targetTrackId });
        else if (audioTrack == null)
          logger.info("Encoding video track {} to {} using profile '{}'", new String[] {
                  videoTrack.getIdentifier(), targetTrackId, profileId });
        else if (videoTrack == null)
          logger.info("Encoding audio track {} to {} using profile '{}'", new String[] {
                  audioTrack.getIdentifier(), targetTrackId, profileId });
          
        composerReceipt.setStatus(Status.RUNNING);
        remoteServiceManager.updateReceipt(composerReceipt);

        // Do the work
        File encodingOutput = null;
        try {
          encodingOutput = encoderEngine.encode(audioFile, videoFile, profile, null);
        } catch (EncoderException e) {
          composerReceipt.setStatus(Status.FAILED);
          remoteServiceManager.updateReceipt(composerReceipt);
          throw new RuntimeException(e);
        }

        // Put the file in the workspace
        URI returnURL = null;
        InputStream in = null;
        try {
          in = new FileInputStream(encodingOutput);
          returnURL = workspace.putInCollection(COLLECTION, encodingOutput.getName(), in);
          logger.debug("Copied the encoded file to the workspace at {}", returnURL);
          encodingOutput.delete();
          logger.info("Deleted the local copy of the encoded file at {}", encodingOutput.getAbsolutePath());
        } catch (Exception e) {
          composerReceipt.setStatus(Status.FAILED);
          remoteServiceManager.updateReceipt(composerReceipt);
          logger.error("Unable to put the encoded file into the workspace");
          throw new RuntimeException(e);
        } finally {
          IOUtils.closeQuietly(in);
        }
        if (encodingOutput != null)
          encodingOutput.delete(); // clean up the encoding output, since the file is now safely stored in the file repo

        // Have the encoded track inspected and return the result
        Receipt inspectionReceipt = inspectionService.inspect(returnURL, true);
        if (inspectionReceipt.getStatus() == Receipt.Status.FAILED)
          throw new RuntimeException("Media inspection failed");
        Track inspectedTrack = (Track) inspectionReceipt.getElement();
        inspectedTrack.setIdentifier(targetTrackId);

        composerReceipt.setElement(inspectedTrack);
        composerReceipt.setStatus(Status.FINISHED);
        remoteServiceManager.updateReceipt(composerReceipt);
      }
    };
    Future<?> future = executor.submit(runnable);
    if (block) {
      try {
        future.get();
      } catch (Exception e) {
        composerReceipt.setStatus(Status.FAILED);
        remoteServiceManager.updateReceipt(composerReceipt);
        throw new EncoderException(encoderEngine, e);
      }
    }
    return composerReceipt;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#listProfiles()
   */
  public EncodingProfile[] listProfiles() {
    Collection<EncodingProfile> profiles = profileScanner.getProfiles().values();
    return profiles.toArray(new EncodingProfile[profiles.size()]);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#getProfile(java.lang.String)
   */
  @Override
  public EncodingProfile getProfile(String profileId) {
    return profileScanner.getProfiles().get(profileId);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#image(org.opencastproject.mediapackage.Track,
   *      java.lang.String, long)
   */
  public Receipt image(Track sourceTrack, String profileId, long time) throws EncoderException {
    return image(sourceTrack, profileId, time, false);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#image(org.opencastproject.mediapackage.Track,
   *      java.lang.String, long, boolean)
   */
  public Receipt image(final Track sourceTrack, final String profileId, final long time, boolean block)
          throws EncoderException {

    final Receipt receipt = remoteServiceManager.createReceipt(JOB_TYPE);

    // Get the encoding profile
    final EncodingProfile profile = profileScanner.getProfile(profileId);
    if (profile == null) {
      receipt.setStatus(Status.FAILED);
      remoteServiceManager.updateReceipt(receipt);
      throw new EncoderException(null, "Profile '" + profileId + "' is unknown");
    }

    // Create the encoding engine
    final EncoderEngine encoderEngine = encoderEngineFactory.newEncoderEngine(profile);
    if (encoderEngine == null) {
      receipt.setStatus(Status.FAILED);
      remoteServiceManager.updateReceipt(receipt);
      throw new EncoderException(null, "No encoder engine available for profile '" + profileId + "'");
    }

    // ake sure there is a video stream in the track
    if (sourceTrack != null && !sourceTrack.hasVideo()) {
      throw new RuntimeException("Cannot extract an image without a video stream");
    } else if (sourceTrack == null) {
      throw new RuntimeException("SourceTrack cannot be null");
    }

    // The time should not be outside of the track's duration
    if (time < 0 || time > sourceTrack.getDuration()) {
      throw new IllegalArgumentException("Can not extract an image at time " + Long.valueOf(time)
              + " from a track with duration " + Long.valueOf(sourceTrack.getDuration()));
    }

    // Finally get the file that needs to be encoded
    final File videoFile;
    try {
      videoFile = workspace.get(sourceTrack.getURI());
    } catch (NotFoundException e) {
      receipt.setStatus(Status.FAILED);
      remoteServiceManager.updateReceipt(receipt);
      throw new EncoderException("Requested video track " + sourceTrack + " was not found", e);
    } catch (IOException e) {
      receipt.setStatus(Status.FAILED);
      remoteServiceManager.updateReceipt(receipt);
      throw new EncoderException("Error accessing video track " + sourceTrack, e);
    }

    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        logger.info("creating an image using video track {}", sourceTrack.getIdentifier());

        receipt.setStatus(Status.RUNNING);
        remoteServiceManager.updateReceipt(receipt);

        Map<String, String> properties = new HashMap<String, String>();
        String timeAsString = Long.toString(time);
        properties.put("time", timeAsString);
        // Do the work
        File encodingOutput = null;
        try {
          encodingOutput = encoderEngine.encode(videoFile, profile, properties);
        } catch (EncoderException e) {
          throw new RuntimeException(e);
        }

        if (encodingOutput == null || !encodingOutput.isFile()) {
          receipt.setStatus(Status.FAILED);
          remoteServiceManager.updateReceipt(receipt);
          throw new RuntimeException("Image extracttion failed: encoding output doesn't exist at " + encodingOutput);
        }

        // Put the file in the workspace
        URI returnURL = null;
        InputStream in = null;
        try {
          in = new FileInputStream(encodingOutput);
          returnURL = workspace.putInCollection(COLLECTION, encodingOutput.getName(), in);
          logger.debug("Copied the encoded file to the workspace at {}", returnURL);
        } catch (Exception e) {
          receipt.setStatus(Status.FAILED);
          remoteServiceManager.updateReceipt(receipt);
          throw new RuntimeException("unable to put the encoded file into the workspace", e);
        } finally {
          IOUtils.closeQuietly(in);
        }
        if (encodingOutput != null) {
          encodingOutput.delete();
        }

        MediaPackageElementBuilder builder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
        Attachment attachment = (Attachment) builder.elementFromURI(returnURL, Attachment.TYPE, null);
        receipt.setElement(attachment);
        receipt.setStatus(Status.FINISHED);
        remoteServiceManager.updateReceipt(receipt);
      }
    };
    Future<?> future = executor.submit(runnable);
    if (block) {
      try {
        future.get();
      } catch (ExecutionException e) {
        throw new EncoderException(encoderEngine, e);
      } catch (InterruptedException e) {
        throw new EncoderException(encoderEngine, e);
      }
    }
    return receipt;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#getReceipt(java.lang.String)
   */
  public Receipt getReceipt(String id) {
    return remoteServiceManager.getReceipt(id);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#countJobs(org.opencastproject.remote.api.Receipt.Status)
   */
  @Override
  public long countJobs(Status status) {
    return remoteServiceManager.count(JOB_TYPE, status);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#countJobs(org.opencastproject.remote.api.Receipt.Status,
   *      java.lang.String)
   */
  @Override
  public long countJobs(Status status, String host) {
    return remoteServiceManager.count(JOB_TYPE, status, host);
  }

}
