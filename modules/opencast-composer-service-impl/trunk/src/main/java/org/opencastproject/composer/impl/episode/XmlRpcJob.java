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

package org.opencastproject.composer.impl.episode;

import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.media.mediapackage.Track;

import java.util.Map;

/**
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: XmlRpcJob.java 2941 2009-08-19 08:00:12Z ced $
 */
public class XmlRpcJob {

  /** The job states as returned by the xmlrpc service */
  enum XmlRpcJobState {

    Created, Queued, Running, Stopped, Finished, Failed;

    static XmlRpcJobState parseResult(Map<String, Object> status) {
      int state = ((Integer) status.get("state")).intValue();
      switch (state) {
      case 0:
        return Created;
      case 1:
        return Queued;
      case 2:
        return Running;
      case 3:
        return Stopped;
      case 4:
        return Finished;
      case 5:
        return Failed;
      default:
        throw new IllegalArgumentException("Episode engine returned an illegal state value " + state);
      }
    }
  };

  /** Reasons given for job states */
  enum XmlRpcReason {

    Unspecified, NoStarted, BadCommunication, BadJob, Failed, Crashed, Lost, Canceled, Aborted, Finished;

    @SuppressWarnings("unchecked")
    static XmlRpcReason parseResult(Object o) {
      if (!(o instanceof Map))
        throw new IllegalArgumentException("Episode engine returned an illegal reason value");
      Map<String, Object> response = (Map<String, Object>) o;
      Map<String, Object> status = (Map<String, Object>) response.get("currentStatus");
      int state = ((Integer) status.get("reason")).intValue();
      switch (state) {
      case 0:
        return Unspecified;
      case 1:
        return NoStarted;
      case 2:
        return BadCommunication;
      case 3:
        return BadJob;
      case 4:
        return Failed;
      case 5:
        return Failed;
      case 6:
        return Crashed;
      case 7:
        return Lost;
      case 8:
        return Canceled;
      case 9:
        return Aborted;
      case 10:
        return Finished;
      default:
        throw new IllegalArgumentException("Episode engine returned an illegal reason value " + state);
      }
    }

  };

  /** The job identifier */
  private int id = -1;

  /** The track to encode */
  private Track track = null;

  /** The media format */
  private EncodingProfile encodingProfile = null;

  /** The settings object */
  private EpisodeSettings settings = null;

  /** The progress */
  private int progress = 0;

  /** The current job state */
  private XmlRpcJobState state = XmlRpcJobState.Created;

  /**
   * Information and state container for jobs that have been submitted to episode engine by xmlrpc.
   * 
   * @param id
   *          the job id
   * @param track
   *          the track
   * @param profile
   *          the profile
   * @param settings
   *          the settings
   */
  XmlRpcJob(int id, Track track, EncodingProfile profile, EpisodeSettings settings) {
    this.id = id;
    this.track = track;
    this.encodingProfile = profile;
    this.settings = settings;
    this.state = XmlRpcJobState.Created;
  }

  /**
   * Sets the job state.
   * 
   * @param state
   *          the state
   */
  void setState(XmlRpcJobState state) {
    this.state = state;
  }

  /**
   * Returns the current job state.
   * 
   * @return the state
   */
  XmlRpcJobState getState() {
    return state;
  }

  /**
   * Returns the job identifier.
   * 
   * @return the identifier
   */
  int getIdentifier() {
    return id;
  }

  /**
   * Returns the track that is encoded.
   * 
   * @return the track
   */
  Track getTrack() {
    return track;
  }

  /**
   * Returns the media format.
   * 
   * @return the format
   */
  EncodingProfile getEncodingProfile() {
    return encodingProfile;
  }

  /**
   * Returns the job's episode settings.
   * 
   * @return the settings
   */
  EpisodeSettings getSettings() {
    return settings;
  }

  /**
   * Returns the progress.
   * 
   * @return the progress
   */
  int getProgress() {
    return progress;
  }

  /**
   * Sets the progress.
   * 
   * @param progress
   *          the progress value
   */
  void setProgress(int progress) {
    this.progress = progress;
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return id;
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof XmlRpcJob)
      return (id == ((XmlRpcJob) obj).id);
    return false;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer("[");
    buf.append(id);
    buf.append("] ");
    buf.append(track);
    buf.append(" to ");
    buf.append(encodingProfile);
    return buf.toString();
  }

}