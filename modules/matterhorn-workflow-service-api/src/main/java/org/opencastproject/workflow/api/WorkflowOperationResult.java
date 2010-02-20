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
package org.opencastproject.workflow.api;

import org.opencastproject.media.mediapackage.MediaPackage;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * The result of a workflow operation.
 */
@XmlJavaTypeAdapter(WorkflowOperationResultImpl.Adapter.class)
public interface WorkflowOperationResult {
  public enum Action { CONTINUE, PAUSE }

  /**
   * @return The media package that results from the execution of a workflow operation.
   */
  MediaPackage getMediaPackage();

  /**
   * Operations may optionally request that the workflow be placed in a certain state.
   * @return The action that the workflow service should take on this workflow instance.
   */
  Action getAction();
}
