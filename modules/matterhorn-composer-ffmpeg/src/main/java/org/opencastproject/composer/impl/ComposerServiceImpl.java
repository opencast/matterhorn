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
import org.opencastproject.composer.api.EmbedderEngine;
import org.opencastproject.composer.api.EmbedderEngineFactory;
import org.opencastproject.composer.api.EmbedderException;
import org.opencastproject.composer.api.EncoderEngine;
import org.opencastproject.composer.api.EncoderEngineFactory;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.inspection.api.MediaInspectionException;
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Stream;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.VideoStream;
import org.opencastproject.mediapackage.identifier.IdBuilder;
import org.opencastproject.mediapackage.identifier.IdBuilderFactory;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.ServiceUnavailableException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FFMPEG based implementation of the composer service api.
 */
public class ComposerServiceImpl implements ComposerService, JobProducer {

  /** The logging instance */
  private static final Logger logger = LoggerFactory.getLogger(ComposerServiceImpl.class);

  /** The collection name */
  public static final String COLLECTION = "composer";

  /** List of available operations on jobs */
  private enum Operation {
    Caption, Encode, Image, Mux, Trim
  };

  /** Encoding profile manager */
  private EncodingProfileScanner profileScanner = null;

  /** Reference to the media inspection service */
  private MediaInspectionService inspectionService = null;

  /** Reference to the workspace service */
  private Workspace workspace = null;

  /** Reference to the receipt service */
  private ServiceRegistry serviceRegistry;

  /** Reference to the encoder engine factory */
  private EncoderEngineFactory encoderEngineFactory;

  /** Reference to the embedder engine factory */
  private EmbedderEngineFactory embedderEngineFactory;

  /** Id builder used to create ids for encoded tracks */
  private final IdBuilder idBuilder = IdBuilderFactory.newInstance().newIdBuilder();

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#encode(org.opencastproject.mediapackage.Track,
   *      java.lang.String)
   */
  @Override
  public Job encode(Track sourceTrack, String profileId) throws EncoderException, MediaPackageException {
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Encode.toString(),
              Arrays.asList(MediaPackageElementParser.getAsXml(sourceTrack), profileId));
    } catch (ServiceUnavailableException e) {
      throw new EncoderException("The " + JOB_TYPE
              + " service is not registered on this host, so no job can be created", e);
    } catch (ServiceRegistryException e) {
      throw new EncoderException("Unable to create a job", e);
    }
  }

  /**
   * Encodes audio and video track to a file. If both an audio and a video track are given, they are muxed together into
   * one movie container.
   * 
   * @param videoTrack
   *          the video track
   * @param audioTrack
   *          the audio track
   * @param profileId
   *          the encoding profile
   * @param properties
   *          encoding properties
   * @param block
   *          <code>true</code> to only return once encoding is finished
   * @return the receipt
   * @throws EncoderException
   *           if encoding fails
   */
  private Track encode(Job job, Track videoTrack, Track audioTrack, String profileId,
          Dictionary<String, String> properties) throws EncoderException, MediaPackageException {

    final String targetTrackId = idBuilder.createNew().toString();

    try {
      job.setStatus(Status.RUNNING);
      updateJob(job);

      // Get the tracks and make sure they exist
      final File audioFile;
      if (audioTrack == null) {
        audioFile = null;
      } else {
        try {
          audioFile = workspace.get(audioTrack.getURI());
        } catch (NotFoundException e) {
          throw new EncoderException("Requested audio track " + audioTrack + " is not found");
        } catch (IOException e) {
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
          throw new EncoderException("Requested video track " + videoTrack + " is not found");
        } catch (IOException e) {
          throw new EncoderException("Unable to access audio track " + audioTrack);
        }
      }

      // Create the engine
      final EncodingProfile profile = profileScanner.getProfile(profileId);
      if (profile == null) {
        throw new EncoderException(null, "Profile '" + profileId + " is unknown");
      }
      final EncoderEngine encoderEngine = encoderEngineFactory.newEncoderEngine(profile);
      if (encoderEngine == null) {
        throw new EncoderException(null, "No encoder engine available for profile '" + profileId + "'");
      }

      if (audioTrack != null && videoTrack != null)
        logger.info("Muxing audio track {} and video track {} into {}", new String[] { audioTrack.getIdentifier(),
                videoTrack.getIdentifier(), targetTrackId });
      else if (audioTrack == null)
        logger.info("Encoding video track {} to {} using profile '{}'", new String[] { videoTrack.getIdentifier(),
                targetTrackId, profileId });
      else if (videoTrack == null)
        logger.info("Encoding audio track {} to {} using profile '{}'", new String[] { audioTrack.getIdentifier(),
                targetTrackId, profileId });

      // Do the work
      File encodingOutput = encoderEngine.mux(audioFile, videoFile, profile, null);

      // Put the file in the workspace
      URI returnURL = null;
      InputStream in = null;
      try {
        in = new FileInputStream(encodingOutput);
        returnURL = workspace.putInCollection(COLLECTION,
                job.getId() + "." + FilenameUtils.getExtension(encodingOutput.getAbsolutePath()), in);
        logger.info("Copied the encoded file to the workspace at {}", returnURL);
        encodingOutput.delete();
        logger.info("Deleted the local copy of the encoded file at {}", encodingOutput.getAbsolutePath());
      } catch (Exception e) {
        throw new EncoderException("Unable to put the encoded file into the workspace", e);
      } finally {
        IOUtils.closeQuietly(in);
      }

      // clean up the encoding output, since the file is now safely stored in the file repo
      if (encodingOutput != null && !encodingOutput.delete()) {
        logger.warn("Unable to delete the encoding output at {}", encodingOutput);
      }

      // Have the encoded track inspected and return the result
      Job inspectionJob = null;
      try {
        inspectionJob = inspectionService.inspect(returnURL);
        JobBarrier barrier = new JobBarrier(serviceRegistry, inspectionJob);
        if (!barrier.waitForJobs().isSuccess()) {
          throw new EncoderException("Media inspection of " + returnURL + " failed");
        }
      } catch (MediaInspectionException e) {
        throw new EncoderException("Media inspection of " + returnURL + " failed", e);
      }

      Track inspectedTrack = (Track) MediaPackageElementParser.getFromXml(inspectionJob.getPayload());
      inspectedTrack.setIdentifier(targetTrackId);

      job.setPayload(MediaPackageElementParser.getAsXml(inspectedTrack));
      job.setStatus(Status.FINISHED);
      updateJob(job);

      return null;
    } catch (Exception e) {
      logger.warn("Error encoding " + videoTrack + " and " + audioTrack, e);
      try {
        job.setStatus(Status.FAILED);
        updateJob(job);
      } catch (Exception failureToFail) {
        logger.warn("Unable to update job to failed state", failureToFail);
      }
      if (e instanceof EncoderException) {
        throw (EncoderException) e;
      } else {
        throw new EncoderException(e);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#trim(org.opencastproject.mediapackage.Track,
   *      java.lang.String, long, long)
   */
  @Override
  public Job trim(final Track sourceTrack, final String profileId, final long start, final long duration)
          throws EncoderException, MediaPackageException {
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Trim.toString(),
              Arrays.asList(MediaPackageElementParser.getAsXml(sourceTrack), profileId, Long.toString(start), Long.toString(duration)));
    } catch (ServiceUnavailableException e) {
      throw new EncoderException("The " + JOB_TYPE
              + " service is not registered on this host, so no job can be created", e);
    } catch (ServiceRegistryException e) {
      throw new EncoderException("Unable to create a job", e);
    }
  }

  /**
   * Trims the given track using the encoding profile <code>profileId</code> and the given starting point and duration
   * in miliseconds.
   * 
   * @param job
   *          the associated job
   * @param sourceTrack
   *          the source track
   * @param profileId
   *          the encoding profile identifier
   * @param start
   *          the trimming in-point
   * @param duration
   *          the trimming duration
   * @return the trimmed track
   * @throws EncoderException
   *           if trimming fails
   */
  private Track trim(Job job, Track sourceTrack, String profileId, long start, long duration) throws EncoderException {
    try {
      job.setStatus(Status.RUNNING);
      updateJob(job);

      String targetTrackId = idBuilder.createNew().toString();

      // Get the track and make sure it exists
      final File trackFile;
      try {
        trackFile = workspace.get(sourceTrack.getURI());
      } catch (NotFoundException e) {
        throw new EncoderException("Requested track " + sourceTrack + " is not found");
      } catch (IOException e) {
        throw new EncoderException("Unable to access track " + sourceTrack);
      }

      // Get the encoding profile
      final EncodingProfile profile = profileScanner.getProfile(profileId);
      if (profile == null) {
        throw new EncoderException("Profile '" + profileId + " is unknown");
      }

      // Create the engine
      final EncoderEngine encoderEngine = encoderEngineFactory.newEncoderEngine(profile);
      if (encoderEngine == null) {
        throw new EncoderException(encoderEngine, "No encoder engine available for profile '" + profileId + "'");
      }

      // Do the work
      File encodingOutput = encoderEngine.trim(trackFile, profile, start, duration, null);

      // Put the file in the workspace
      URI returnURL = null;
      InputStream in = null;
      try {
        in = new FileInputStream(encodingOutput);
        returnURL = workspace.putInCollection(COLLECTION,
                job.getId() + "." + FilenameUtils.getExtension(encodingOutput.getAbsolutePath()), in);
        logger.info("Copied the trimmed file to the workspace at {}", returnURL);
        encodingOutput.delete();
        logger.info("Deleted the local copy of the trimmed file at {}", encodingOutput.getAbsolutePath());
      } catch (FileNotFoundException e) {
        throw new EncoderException("Encoded file " + encodingOutput + " not found", e);
      } catch (IOException e) {
        throw new EncoderException("Error putting " + encodingOutput + " into the workspace", e);
      } finally {
        IOUtils.closeQuietly(in);
      }
      if (encodingOutput != null)
        encodingOutput.delete(); // clean up the encoding output, since the file is now safely stored in the file
                                 // repo

      // Have the encoded track inspected and return the result
      Job inspectionJob = null;
      try {
        inspectionJob = inspectionService.inspect(returnURL);
        JobBarrier barrier = new JobBarrier(serviceRegistry, inspectionJob);
        if (!barrier.waitForJobs().isSuccess()) {
          throw new EncoderException("Media inspection of " + returnURL + " failed");
        }
      } catch (MediaInspectionException e) {
        throw new EncoderException("Media inspection of " + returnURL + " failed", e);
      }

      Track inspectedTrack = (Track) MediaPackageElementParser.getFromXml(inspectionJob.getPayload());
      inspectedTrack.setIdentifier(targetTrackId);

      job.setPayload(MediaPackageElementParser.getAsXml(inspectedTrack));
      job.setStatus(Status.FINISHED);
      updateJob(job);

      return inspectedTrack;
    } catch (Exception e) {
      logger.warn("Error trimming " + sourceTrack, e);
      try {
        job.setStatus(Status.FAILED);
        updateJob(job);
      } catch (Exception failureToFail) {
        logger.warn("Unable to update job to failed state", failureToFail);
      }
      if (e instanceof EncoderException) {
        throw (EncoderException) e;
      } else {
        throw new EncoderException(e);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#mux(org.opencastproject.mediapackage.Track,
   *      org.opencastproject.mediapackage.Track, java.lang.String)
   */
  @Override
  public Job mux(Track videoTrack, Track audioTrack, String profileId) throws EncoderException, MediaPackageException {
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Mux.toString(),
              Arrays.asList(MediaPackageElementParser.getAsXml(videoTrack), MediaPackageElementParser.getAsXml(audioTrack), profileId));
    } catch (ServiceUnavailableException e) {
      throw new EncoderException("The " + JOB_TYPE
              + " service is not registered on this host, so no job can be created", e);
    } catch (ServiceRegistryException e) {
      throw new EncoderException("Unable to create a job", e);
    }
  }

  /**
   * Muxes the audio and video track into one movie container.
   * 
   * @param job
   *          the associated job
   * @param videoTrack
   *          the video track
   * @param audioTrack
   *          the audio track
   * @param profileId
   *          the profile identifier
   * @return the muxed track
   * @throws EncoderException
   *           if encoding fails
   * @throws MediaPackageException
   *           if serializing the mediapackage elements fails
   */
  private Track mux(Job job, Track videoTrack, Track audioTrack, String profileId) throws EncoderException,
          MediaPackageException {
    return encode(job, videoTrack, audioTrack, profileId, null);
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
  public Job image(Track sourceTrack, String profileId, long time) throws EncoderException, MediaPackageException {

    if (sourceTrack == null)
      throw new IllegalArgumentException("SourceTrack cannot be null");

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Image.toString(),
              Arrays.asList(MediaPackageElementParser.getAsXml(sourceTrack), profileId, Long.toString(time)));
    } catch (ServiceUnavailableException e) {
      throw new EncoderException("The " + JOB_TYPE
              + " service is not registered on this host, so no job can be created", e);
    } catch (ServiceRegistryException e) {
      throw new EncoderException("Unable to create a job", e);
    }
  }

  /**
   * Extracts an image from <code>sourceTrack</code> at the given point in time.
   * 
   * @param job
   *          the associated job
   * @param sourceTrack
   *          the source track
   * @param profileId
   *          the identifer of the encoding profile to use
   * @param time
   *          the time in miliseconds
   * @return the image as an attachment element
   * @throws EncoderException
   *           if extracting the image fails
   */
  private Attachment image(Job job, Track sourceTrack, String profileId, long time) throws EncoderException,
          MediaPackageException {

    if (sourceTrack == null)
      throw new EncoderException("SourceTrack cannot be null");

    try {
      job.setStatus(Status.RUNNING);
      updateJob(job);

      logger.info("creating an image using video track {}", sourceTrack.getIdentifier());

      job.setStatus(Status.RUNNING);
      updateJob(job);

      // Get the encoding profile
      final EncodingProfile profile = profileScanner.getProfile(profileId);
      if (profile == null) {
        throw new EncoderException("Profile '" + profileId + "' is unknown");
      }

      // Create the encoding engine
      final EncoderEngine encoderEngine = encoderEngineFactory.newEncoderEngine(profile);
      if (encoderEngine == null) {
        throw new EncoderException("No encoder engine available for profile '" + profileId + "'");
      }

      // make sure there is a video stream in the track
      if (sourceTrack != null && !sourceTrack.hasVideo()) {
        throw new EncoderException("Cannot extract an image without a video stream");
      }

      // The time should not be outside of the track's duration
      if (time < 0 || time > sourceTrack.getDuration()) {
        throw new EncoderException("Can not extract an image at time " + Long.valueOf(time)
                + " from a track with duration " + Long.valueOf(sourceTrack.getDuration()));
      }

      // Finally get the file that needs to be encoded
      final File videoFile;
      try {
        videoFile = workspace.get(sourceTrack.getURI());
      } catch (NotFoundException e) {
        throw new EncoderException("Requested video track " + sourceTrack + " was not found", e);
      } catch (IOException e) {
        throw new EncoderException("Error accessing video track " + sourceTrack, e);
      }

      Map<String, String> properties = new HashMap<String, String>();
      String timeAsString = Long.toString(time);
      properties.put("time", timeAsString);

      // Do the work
      File encodingOutput = encoderEngine.encode(videoFile, profile, properties);

      if (encodingOutput == null || !encodingOutput.isFile()) {
        throw new EncoderException("Image extraction failed: encoding output doesn't exist at " + encodingOutput);
      }

      // Put the file in the workspace
      URI returnURL = null;
      InputStream in = null;
      try {
        in = new FileInputStream(encodingOutput);
        returnURL = workspace.putInCollection(COLLECTION,
                job.getId() + "." + FilenameUtils.getExtension(encodingOutput.getAbsolutePath()), in);
        logger.debug("Copied the encoded file to the workspace at {}", returnURL);
      } catch (Exception e) {
        throw new EncoderException("unable to put the encoded file into the workspace", e);
      } finally {
        IOUtils.closeQuietly(in);
      }
      if (encodingOutput != null && !encodingOutput.delete()) {
        logger.warn("Unable to delete encoded file '{}'", encodingOutput);
      }

      MediaPackageElementBuilder builder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
      Attachment attachment = (Attachment) builder.elementFromURI(returnURL, Attachment.TYPE, null);

      job.setPayload(MediaPackageElementParser.getAsXml(attachment));
      job.setStatus(Status.FINISHED);
      updateJob(job);

      return attachment;
    } catch (Exception e) {
      logger.warn("Error extracting image from " + sourceTrack, e);
      try {
        job.setStatus(Status.FAILED);
        updateJob(job);
      } catch (Exception failureToFail) {
        logger.warn("Unable to update job to failed state", failureToFail);
      }
      if (e instanceof EncoderException) {
        throw (EncoderException) e;
      } else {
        throw new EncoderException(e);
      }
    }

  }

  /**
   * {@inheritDoc}
   * 
   * Supports inserting captions in QuickTime files.
   * 
   * @see org.opencastproject.composer.api.ComposerService#captions(org.opencastproject.mediapackage.Track,
   *      org.opencastproject.mediapackage.Attachment, java.lang.String)
   */
  @Override
  public Job captions(final Track mediaTrack, final Catalog[] captions) throws EmbedderException, MediaPackageException {

    List<String> args = new ArrayList<String>();
    args.set(0, MediaPackageElementParser.getAsXml(mediaTrack));
    for (int i = 0; i < captions.length; i++) {
      args.set(i + 1, MediaPackageElementParser.getAsXml(captions[i]));
    }

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Caption.toString(), args);
    } catch (ServiceUnavailableException e) {
      throw new EmbedderException("The " + JOB_TYPE
              + " service is not registered on this host, so no job can be created", e);
    } catch (ServiceRegistryException e) {
      throw new EmbedderException("Unable to create a job", e);
    }
  }

  /**
   * Adds the closed captions contained in the <code>captions</code> catalog collection to <code>mediaTrack</code>.
   * 
   * @param job
   *          the associated job
   * @param mediaTrack
   *          the source track
   * @param captions
   *          the caption catalogs
   * @return the captioned track
   * @throws EmbedderException if embedding captions into the track fails
   */
  private Track captions(Job job, Track mediaTrack, Catalog[] captions) throws EmbedderException {
    try {
      job.setStatus(Status.RUNNING);
      updateEmbedderJob(job);

      logger.info("Atempting to create and embed subtitles to video track");

      final String targetTrackId = idBuilder.createNew().toString();

      // get embedder engine
      final EmbedderEngine engine = embedderEngineFactory.newEmbedderEngine();
      if (engine == null) {
        throw new EmbedderException("Embedder engine not available");
      }

      // check if media file has video track
      if (mediaTrack == null || !mediaTrack.hasVideo()) {
        throw new EmbedderException("Media track must contain video stream");
      }
      // get video height
      Integer videoHeigth = null;
      for (Stream s : mediaTrack.getStreams()) {
        if (s instanceof VideoStream) {
          videoHeigth = ((VideoStream) s).getFrameHeight();
          break;
        }
      }
      final int subHeight;
      if (videoHeigth != null) {
        // get 1/8 of track height
        // smallest size is 60 pixels
        subHeight = videoHeigth > 8 * 60 ? videoHeigth / 8 : 60;
      } else {
        // no information about video height retrieved, use 60 pixels
        subHeight = 60;
      }

      // retrieve media file
      final File mediaFile;
      try {
        mediaFile = workspace.get(mediaTrack.getURI());
      } catch (NotFoundException e) {
        throw new EmbedderException("Could not find track: " + mediaTrack);
      } catch (IOException e) {
        throw new EmbedderException("Error accessing track: " + mediaTrack);
      }

      final File[] captionFiles = new File[captions.length];
      final String[] captionLanguages = new String[captions.length];
      for (int i = 0; i < captions.length; i++) {
        // get file
        try {
          captionFiles[i] = workspace.get(captions[i].getURI());
        } catch (NotFoundException e) {
          throw new EmbedderException("Could not found captions at: " + captions[i]);
        } catch (IOException e) {
          throw new EmbedderException("Error accessing captions at: " + captions[i]);
        }
        // get language
        captionLanguages[i] = getLanguageFromTags(captions[i].getTags());
        if (captionLanguages[i] == null) {
          throw new EmbedderException("Missing caption language information for captions at: " + captions[i]);
        }
      }

      // set properties
      Map<String, String> properties = new HashMap<String, String>();
      properties.put("param.trackh", String.valueOf(subHeight));
      properties.put("param.offset", String.valueOf(subHeight / 2));

      File output = engine.embed(mediaFile, captionFiles, captionLanguages, properties);

      URI returnURL = null;
      InputStream in = null;
      try {
        in = new FileInputStream(output);
        returnURL = workspace.putInCollection(COLLECTION,
                job.getId() + "." + FilenameUtils.getExtension(output.getAbsolutePath()), in);
        logger.info("Copied the encoded file to the workspace at {}", returnURL);
      } catch (Exception e) {
        throw new EmbedderException("Unable to put the encoded file into the workspace", e);
      } finally {
        IOUtils.closeQuietly(in);
        logger.info("Deleting the local copy of the embedded file at {}", output.getAbsolutePath());
        if (!output.delete()) {
          logger.warn("Could not delete local copy of file at {}", output.getAbsolutePath());
        }
      }

      // Have the encoded track inspected and return the result
      Job inspectionJob;
      try {
        inspectionJob = inspectionService.inspect(returnURL);
        JobBarrier barrier = new JobBarrier(serviceRegistry, inspectionJob);
        if (!barrier.waitForJobs().isSuccess()) {
          throw new EncoderException("Media inspection of " + returnURL + " failed");
        }
      } catch (MediaInspectionException e) {
        throw new EmbedderException("Media inspection of " + returnURL + " failed", e);
      }

      Track inspectedTrack = (Track) MediaPackageElementParser.getFromXml(inspectionJob.getPayload());
      inspectedTrack.setIdentifier(targetTrackId);

      job.setPayload(MediaPackageElementParser.getAsXml(inspectedTrack));
      job.setStatus(Status.FINISHED);
      updateEmbedderJob(job);

      return inspectedTrack;
    } catch (Exception e) {
      logger.warn("Error embedding captions into " + mediaTrack, e);
      try {
        job.setStatus(Status.FAILED);
        updateJob(job);
      } catch (Exception failureToFail) {
        logger.warn("Unable to update job to failed state", failureToFail);
      }
      if (e instanceof EncoderException) {
        throw (EmbedderException) e;
      } else {
        throw new EmbedderException(e);
      }
    }
  }

  /**
   * Helper function that iterates tags and returns language from tag in form lang:&lt;lang&gt;
   * 
   * @param tags
   *          catalog tags
   * @return language or null if no corresponding tag was found
   */
  private String getLanguageFromTags(String[] tags) {
    for (String tag : tags) {
      if (tag.startsWith("lang:") && tag.length() > 5) {
        return tag.substring(5);
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducer#startJob(org.opencastproject.job.api.Job, java.lang.String,
   *      java.util.List)
   */
  @Override
  public void startJob(Job job, String operation, List<String> arguments) throws ServiceRegistryException {
    Operation op = null;
    try {
      op = Operation.valueOf(operation);
      Track firstTrack = null;
      Track secondTrack = null;
      String encodingProfile = null;
      switch (op) {
        case Caption:
          firstTrack = (Track)MediaPackageElementParser.getFromXml(arguments.get(0));
          Catalog[] catalogs = new Catalog[arguments.size() - 1];
          for (int i = 1; i < arguments.size(); i++) {
            catalogs[i] = (Catalog)MediaPackageElementParser.getFromXml(arguments.get(i));
          }
          captions(job, firstTrack, catalogs);
          break;
        case Encode:
          firstTrack = (Track)MediaPackageElementParser.getFromXml(arguments.get(0));
          encodingProfile = arguments.get(1);
          encode(job, firstTrack, null, encodingProfile, null);
          break;
        case Image:
          firstTrack = (Track)MediaPackageElementParser.getFromXml(arguments.get(0));
          encodingProfile = arguments.get(1);
          long time = Long.parseLong(arguments.get(2));
          image(job, firstTrack, encodingProfile, time);
          break;
        case Mux:
          firstTrack = (Track)MediaPackageElementParser.getFromXml(arguments.get(0));
          secondTrack = (Track)MediaPackageElementParser.getFromXml(arguments.get(1));
          encodingProfile = arguments.get(2);
          mux(job, firstTrack, secondTrack, encodingProfile);
          break;
        case Trim:
          firstTrack = (Track)MediaPackageElementParser.getFromXml(arguments.get(0));
          encodingProfile = arguments.get(1);
          long start = Long.parseLong(arguments.get(2));
          long duration = Long.parseLong(arguments.get(3));
          trim(job, firstTrack, encodingProfile, start, duration);
          break;
        default:
          throw new IllegalStateException("Don't know how to handle operation '" + operation + "'");
      }
    } catch (IllegalArgumentException e) {
      throw new ServiceRegistryException("This service can't handle operations of type '" + op + "'");
    } catch (IndexOutOfBoundsException e) {
      throw new ServiceRegistryException("This argument list for operation '" + op + "' does not meet expectations");
    } catch (Exception e) {
      throw new ServiceRegistryException("Error handling operation '" + op + "'");
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducer#getJob(long)
   */
  public Job getJob(long id) throws NotFoundException, ServiceRegistryException {
    return serviceRegistry.getJob(id);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.JobProducer#getJobType()
   */
  @Override
  public String getJobType() {
    return JOB_TYPE;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#countJobs(org.opencastproject.job.api.Job.Status)
   */
  @Override
  public long countJobs(Status status) throws ServiceRegistryException {
    return serviceRegistry.count(JOB_TYPE, status);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#countJobs(org.opencastproject.job.api.Job.Status,
   *      java.lang.String)
   */
  @Override
  public long countJobs(Status status, String host) throws ServiceRegistryException {
    return serviceRegistry.count(JOB_TYPE, status, host);
  }

  /**
   * Updates the job in the service registry. The exceptions that are possibly been thrown are wrapped in a
   * {@link EncoderException}.
   * 
   * @param job
   *          the job to update
   * @throws EncoderException
   *           the exception that is being thrown
   */
  private void updateJob(Job job) throws EncoderException {
    try {
      serviceRegistry.updateJob(job);
    } catch (NotFoundException notFound) {
      throw new EncoderException("Unable to find job " + job, notFound);
    } catch (ServiceRegistryException serviceRegException) {
      throw new EncoderException("Unable to update job '" + job + "' in service registry", serviceRegException);
    } catch (ServiceUnavailableException e) {
      throw new EncoderException("No service of type '" + JOB_TYPE + "' available", e);
    }
  }

  /**
   * Updates embedder jobs in the service registry. The exceptions that are possibly been thrown are wrapped in a
   * {@link EncoderException}.
   * 
   * @param job
   *          the job to update
   * @throws EmbedderException
   *           the exception that is being thrown
   */
  private void updateEmbedderJob(Job job) throws EmbedderException {
    try {
      serviceRegistry.updateJob(job);
    } catch (NotFoundException notFound) {
      throw new EmbedderException("Unable to find job " + job, notFound);
    } catch (ServiceRegistryException serviceRegException) {
      throw new EmbedderException("Unable to update job '" + job + "' in service registry", serviceRegException);
    } catch (ServiceUnavailableException e) {
      throw new EmbedderException("No service of type '" + JOB_TYPE + "' available", e);
    }
  }

  /**
   * Sets the media inspection service
   * 
   * @param mediaInspectionService
   *          an instance of the media inspection service
   */
  void setMediaInspectionService(MediaInspectionService mediaInspectionService) {
    this.inspectionService = mediaInspectionService;
  }

  /**
   * Sets the encoder engine factory
   * 
   * @param encoderEngineFactory
   *          The encoder engine factory
   */
  void setEncoderEngineFactory(EncoderEngineFactory encoderEngineFactory) {
    this.encoderEngineFactory = encoderEngineFactory;
  }

  /**
   * Sets the embedder engine factoy
   * 
   * @param embedderEngineFactory
   *          The embedder engine factory
   */
  void setEmbedderEngineFactory(EmbedderEngineFactory embedderEngineFactory) {
    this.embedderEngineFactory = embedderEngineFactory;
  }

  /**
   * Sets the workspace
   * 
   * @param workspace
   *          an instance of the workspace
   */
  void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Sets the receipt service
   * 
   * @param remoteServiceManager
   */
  void setRemoteServiceManager(ServiceRegistry remoteServiceManager) {
    this.serviceRegistry = remoteServiceManager;
  }

  void setProfileScanner(EncodingProfileScanner scanner) {
    this.profileScanner = scanner;
  }

}
