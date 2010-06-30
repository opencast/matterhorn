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

import org.opencastproject.composer.api.EncoderEngine;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncoderListener;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.util.IoSupport;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.MimetypesFileTypeMap;

/**
 * Wrapper around any kind of command line controllable encoder.
 * <p/>
 * <strong>Note:</strong> Registered {@link EncoderListener}s <em>won't</em> receive a file in method
 * {@link EncoderListener#fileEncoded(EncoderEngine, EncodingProfile, File...)} because it cannot be guaranteed that
 * only <em>one</em> file will be the result of the encoding. Imagine encoding to an image series.
 */
public abstract class AbstractCmdlineEncoderEngine extends AbstractEncoderEngine implements EncoderEngine {

  /**
   * If true STDERR and STDOUT of the spawned process will be mixed so that both can be read via STDIN
   */
  private static final boolean REDIRECT_ERROR_STREAM = true;

  /** the encoder binary */
  private String binary = null;

  /** the command line options */
  private String cmdlineOptions = "";

  /** parameters substituted in the command line options string */
  private Map<String, String> params = new HashMap<String, String>();

  /** the logging facility provided by log4j */
  private final static Logger logger = LoggerFactory.getLogger(AbstractCmdlineEncoderEngine.class.getName());

  /**
   * Creates a new CmdlineEncoderEngine with <code>binary</code> as the workhorse.
   */
  public AbstractCmdlineEncoderEngine(String binary) {
    super(false);

    if (binary == null)
      throw new IllegalArgumentException("binary is null");

    this.binary = binary;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.EncoderEngine#needsLocalWorkCopy()
   */
  public boolean needsLocalWorkCopy() {
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.EncoderEngine#encode(java.io.File,
   *      org.opencastproject.composer.api.EncodingProfile, java.util.Map)
   */
  @Override
  public File encode(File mediaSource, EncodingProfile format, Map<String, String> properties) throws EncoderException {
    return encode(null, mediaSource, format, properties);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.EncoderEngine#encode(java.io.File, java.io.File,
   *      org.opencastproject.composer.api.EncodingProfile, java.util.Map)
   */
  @Override
  public File encode(File audioSource, File videoSource, EncodingProfile profile, Map<String, String> properties)
          throws EncoderException {
    // Fist, update the parameters
    if (properties != null)
      params.putAll(properties);

    // build command
    BufferedReader in = null;
    Process encoderProcess = null;
    if (videoSource == null && audioSource == null) {
      throw new IllegalArgumentException("At least one track must be specified.");
    }
    try {
      // Set encoding parameters
      String audioInput = null;
      if (audioSource != null) {
        audioInput = FilenameUtils.normalize(audioSource.getAbsolutePath());
        params.put("in.audio.path", audioInput);
        params.put("in.audio.name", FilenameUtils.getBaseName(audioInput));
        params.put("in.audio.suffix", FilenameUtils.getExtension(audioInput));
        params.put("in.audio.filename", FilenameUtils.getName(audioInput));
        params.put("in.audio.mimetype", MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(audioInput));
      }
      if (videoSource != null) {
        String videoInput = FilenameUtils.normalize(videoSource.getAbsolutePath());
        params.put("in.video.path", videoInput);
        params.put("in.video.name", FilenameUtils.getBaseName(videoInput));
        params.put("in.video.suffix", FilenameUtils.getExtension(videoInput));
        params.put("in.video.filename", FilenameUtils.getName(videoInput));
        params.put("in.video.mimetype", MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(videoInput));
      }
      File parentFile;
      if (videoSource == null) {
        parentFile = audioSource;
      } else {
        parentFile = videoSource;
      }
      params.put("out.dir", parentFile.getParent());
      params.put("out.name", FilenameUtils.getBaseName(parentFile.getName()));
      params.put("out.suffix", processParameters(profile.getSuffix()));

      // create encoder process.
      // no special working dir is set which means the working dir of the
      // current java process is used.
      // TODO: Parallelisation (threading)
      List<String> command = buildCommand(profile);
      StringBuilder sb = new StringBuilder();
      for (String cmd : command) {
        sb.append(cmd);
        sb.append(" ");
      }
      logger.info("Executing encoding command: {}", sb);
      ProcessBuilder pbuilder = new ProcessBuilder(command);
      pbuilder.redirectErrorStream(REDIRECT_ERROR_STREAM);
      encoderProcess = pbuilder.start();

      // tell encoder listeners about output
      in = new BufferedReader(new InputStreamReader(encoderProcess.getInputStream()));
      String line;
      while ((line = in.readLine()) != null) {
        handleEncoderOutput(profile, line, audioSource, videoSource);
      }

      // wait until the task is finished
      encoderProcess.waitFor();
      int exitCode = encoderProcess.exitValue();
      if (exitCode != 0) {
        throw new EncoderException(this, "Encoder exited abnormally with status " + exitCode);
      }

      if (audioSource != null) {
        logger.info("Audio track {} and video track {} successfully encoded using profile '{}'", new String[] {
                (audioSource == null ? "N/A" : audioSource.getName()),
                (videoSource == null ? "N/A" : videoSource.getName()), profile.getIdentifier() });
      } else {
        logger.info("Video track {} successfully encoded using profile '{}'", new String[] { videoSource.getName(),
                profile.getIdentifier() });
      }
      fireEncoded(this, profile, audioSource, videoSource);
      return new File(parentFile.getParent(), FilenameUtils.getBaseName(parentFile.getCanonicalPath())
              + processParameters(profile.getSuffix()));
    } catch (EncoderException e) {
      if (audioSource != null) {
        logger.warn("Error while encoding audio track {} and video track {} using '{}': {}", new String[] {
                (audioSource == null ? "N/A" : audioSource.getName()),
                (videoSource == null ? "N/A" : videoSource.getName()), profile.getIdentifier(), e.getMessage() });
      } else {
        logger.warn("Error while encoding video track {} using '{}': {}", new String[] {
                (videoSource == null ? "N/A" : videoSource.getName()), profile.getIdentifier(), e.getMessage() });
      }
      fireEncodingFailed(this, profile, e, audioSource, videoSource);
      throw e;
    } catch (Exception e) {
      logger.warn("Error while encoding audio {} and video {} to {}:{}, {}", new Object[] {
              (audioSource == null ? "N/A" : audioSource.getName()),
              (videoSource == null ? "N/A" : videoSource.getName()), profile.getName(), e.getMessage() });
      fireEncodingFailed(this, profile, e, audioSource, videoSource);
      throw new EncoderException(this, e.getMessage(), e);
    } finally {
      IoSupport.closeQuietly(in);
      IoSupport.closeQuietly(encoderProcess);
    }
  }

  /**
   * Handles the encoder output by analyzing it first and then firing it off to the registered listeners.
   * 
   * @param format
   *          the target media format
   * @param message
   *          the message returned by the encoder
   * @param sourceFiles
   *          the source files that are being encoded
   */
  protected void handleEncoderOutput(EncodingProfile format, String message, File... sourceFiles) {
    message = message.trim();
    fireEncoderMessage(format, message, sourceFiles);
  }

  /**
   * Specifies the encoder binary.
   * 
   * @param binary
   *          path to the binary
   */
  protected void setBinary(String binary) {
    if (binary == null)
      throw new IllegalArgumentException("binary is null");
    this.binary = binary;
  }

  /**
   * Returns the parameters that will replace the variable placeholders on the commandline, such as
   * <code>in.video.name</code> etc.
   * 
   * @return the parameters
   */
  protected Map<String, String> getCommandlineParameters() {
    return params;
  }

  /**
   * Creates the command that is sent to the commandline encoder.
   * 
   * @return the commandline
   * @throws EncoderException
   *           in case of any error
   */
  protected List<String> buildCommand(EncodingProfile profile) throws EncoderException {
    List<String> command = new ArrayList<String>();
    command.add(binary);
    List<String> arguments = buildArgumentList(profile);
    for (String arg : arguments) {
      for (Map.Entry<String, String> e : params.entrySet()) {
        arg = arg.replace("#{" + e.getKey() + "}", e.getValue());
      }
      command.add(arg);
    }
    return command;
  }

  /**
   * Creates the arguments for the commandline.
   * 
   * @param format
   *          the encoding profile
   * @return the argument list
   * @throws EncoderException
   *           in case of any error
   */
  protected List<String> buildArgumentList(EncodingProfile format) throws EncoderException {
    String optionString = processParameters(cmdlineOptions);
    String[] options = optionString.split(" ");
    List<String> arguments = new ArrayList<String>(options.length);
    arguments.addAll(Arrays.asList(options));
    return arguments;
  }

  /**
   * Processes the command options by replacing the templates with their actual values.
   * 
   * @return the commandline
   */
  protected String processParameters(String cmd) {
    String r = cmd;
    for (Map.Entry<String, String> e : params.entrySet()) {
      r = r.replace("#{" + e.getKey() + "}", e.getValue());
    }
    return r;
  }

  // -- Attributes

  /**
   * Set the commandline options in a single string. Parameters in the form of <code>#{param}</code> will be
   * substituted.
   * 
   * @see #addParam(String, String)
   */
  public void setCmdlineOptions(String cmdlineOptions) {
    this.cmdlineOptions = cmdlineOptions;
  }

  /**
   * Adds a command line parameter that will be substituted along with the default parameters.
   * 
   * @see #setCmdlineOptions(String)
   */
  public void addParam(String name, String value) {
    params.put(name, value);
  }

  /**
   * Tells the registered listeners that the given track has been encoded into <code>file</code>, using the encoding
   * format <code>format</code>.
   * 
   * @param sourceFiles
   *          the original files
   * @param format
   *          the used format
   * @param message
   *          the message
   */
  protected void fireEncoderMessage(EncodingProfile format, String message, File... sourceFiles) {
    for (EncoderListener l : this.listeners) {
      if (l instanceof CmdlineEncoderListener) {
        try {
          ((CmdlineEncoderListener) l).notifyEncoderOutput(format, message, sourceFiles);
        } catch (Throwable th) {
          logger.error("EncoderListener " + l + " threw exception while processing callback", th);
        }
      }
    }
  }

}
