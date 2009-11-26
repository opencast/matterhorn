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
package org.opencastproject.capture.api;

import java.util.Set;

/**
 * OSGi service for querying the capture device's current state (MH-58)
 */
public interface StatusService {

  /**
   * Gets the state of the agent
   * @return The state of the agent (should be defined in AgentState)
   * @see org.opencastproject.capture.api.AgentState
   */
  public String getAgentState();

  /**
   * Returns the set of recording IDs that are active within this capture agent.
   * @return The set of recording IDs
   */
  public Set<String> getRecordings();

  /**
   * Gets the state of a recording
   * 
   * @param recordingID The ID of the recording in question
   * @return A state (should be defined in RecordingState)
   * @see org.opencastproject.capture.api.RecordingState
   */
  public String getRecordingState(String recordingID);
}

