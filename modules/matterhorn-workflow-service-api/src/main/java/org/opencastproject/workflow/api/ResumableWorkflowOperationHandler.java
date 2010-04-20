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

/**
 * TODO: Comment me!
 */
public interface ResumableWorkflowOperationHandler extends WorkflowOperationHandler {
  /**
   * Continues a suspended {@link WorkflowInstance}.  If the execution fails for some reason, this must
   * throw a {@link WorkflowOperationException} in order to handle the problem gracefully.  Runtime exceptions will
   * cause the entire workflow instance to fail.
   * 
   * If the workflow instance is not in a suspended state, this method should throw an {@link IllegalStateException}.
   * 
   * @param workflowInstance The workflow instance
   * @return the result of this operation
   * @throws WorkflowOperationException If the workflow operation fails to execute properly.
   */
  WorkflowOperationResult resume(WorkflowInstance workflowInstance) throws WorkflowOperationException;  

  /**
   * Gets the URL for the user interface for resuming the workflow.
   * 
   * @param workflowInstance The workflow instance
   * @return The URL for the user interface
   * @throws WorkflowOperationException If the url to the hold state ui can't be created
   */
  URL getHoldStateUserInterfaceURL(WorkflowInstance wokflowInstance) throws WorkflowOperationException;

  /** Returns the title for the link to this operations hold state UI.
   *
   * @return title to be displayed
   */
  String getHoldActionTitle();

}
