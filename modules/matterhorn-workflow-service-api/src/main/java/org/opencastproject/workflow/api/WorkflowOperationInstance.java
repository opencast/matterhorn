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
package org.opencastproject.workflow.api;

import java.net.URL;
import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * A workflow operation belonging to a workflow instance.
 */
@XmlJavaTypeAdapter(WorkflowOperationInstanceImpl.Adapter.class)
public interface WorkflowOperationInstance extends Configurable {
  public enum OperationState { INSTANTIATED, RUNNING, PAUSED, SUCCEEDED, FAILED }

  String getId();
  
  String getDescription();

  /**
   * The state of this operation.
   */
  OperationState getState();
  
  /**
   * Sets the state of this operation
   * @param state the state to set
   */
  void setState(OperationState state);
  
  /**
   * Gets the URL for the hold state.
   * @return the URL of the hold state, if any, for this operation
   */
  URL getHoldStateUserInterfaceUrl();

  /** Returns the title for the link to this operations hold state UI,
   *  a default String if no title is set.
   *
   * @return title to be displayed
   */
  String getHoldActionTitle();
  
  /** The workflow to run if an exception is thrown while this operation is running. */
  String getExceptionHandlingWorkflow();

  /**
   * If true, this workflow will be put into a failed (or failing, if getExceptionHandlingWorkflow() is not null) state
   * when exceptions are thrown during an operation.
   */
  boolean isFailWorkflowOnException();
  
  /** The timestamp this operation started. If the job was queued, this can be significantly later than the date created. */
  public Date getDateStarted();

  /** The number of milliseconds this operation waited in a service queue */
  public long getTimeInQueue();
  
  /** The timestamp this operation completed */
  public Date getDateCompleted();
}
