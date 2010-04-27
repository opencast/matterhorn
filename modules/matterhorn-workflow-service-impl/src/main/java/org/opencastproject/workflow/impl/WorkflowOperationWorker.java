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
package org.opencastproject.workflow.impl;

import org.opencastproject.workflow.api.ResumableWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles execution of a workflow operation.
 */
final class WorkflowOperationWorker implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(WorkflowOperationWorker.class);
  
  protected WorkflowOperationHandler handler = null;
  protected WorkflowInstance workflow = null;
  protected WorkflowServiceImpl service = null;

  public WorkflowOperationWorker(WorkflowOperationHandler handler, WorkflowInstance workflow, WorkflowServiceImpl service) {
    this.handler = handler;
    this.workflow = workflow;
    this.service = service;
  }

  /**
   * {@inheritDoc}
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    WorkflowOperationInstance operation = workflow.getCurrentOperation();
    try {
      WorkflowOperationResult result = null;
      switch(operation.getState()) {
      case INSTANTIATED :
        result = start();
        break;
      case RUNNING :
        result = start();
        break;
      case PAUSED :
        result = resume();
        break;
      }
      if(result == null || Action.CONTINUE.equals(result.getAction())) handler.destroy(workflow);
      service.handleOperationResult(workflow, result);
    } catch(WorkflowOperationException e) {
      logger.error("Workflow operation '{}' failed with error: {}", new Object[] {handler, e.getMessage(), e});
      operation.setState(OperationState.FAILED);
      service.handleOperationException(workflow, e);
    }
  }

  public WorkflowOperationResult start() throws WorkflowOperationException {
    WorkflowOperationInstance operation = workflow.getCurrentOperation();
    try {
      WorkflowOperationResult result = handler.start(workflow);
      if(result != null && Action.PAUSE.equals(result.getAction())) {
        operation.setState(OperationState.PAUSED);
      } else {
        operation.setState(OperationState.SUCCEEDED);
      }
      return result;
    } catch(Exception e) {
      operation.setState(OperationState.FAILED);
      if(e instanceof WorkflowOperationException) throw (WorkflowOperationException)e;
      throw new WorkflowOperationException(e);
    }
  }
  
  public WorkflowOperationResult resume() throws WorkflowOperationException {
    if( ! (handler instanceof ResumableWorkflowOperationHandler)) {
      throw new IllegalStateException("an attempt was made to resume a non-resumable operation");
    }
    ResumableWorkflowOperationHandler resumableHandler = (ResumableWorkflowOperationHandler) handler;
    WorkflowOperationInstance operation = workflow.getCurrentOperation();
    try {
      WorkflowOperationResult result = resumableHandler.resume(workflow);
      if(result == null || Action.CONTINUE.equals(result.getAction())) {
        operation.setState(OperationState.SUCCEEDED);
      }
      if(result != null && Action.PAUSE.equals(result.getAction())) {
        operation.setState(OperationState.PAUSED);
      }
      return result;
    } catch(Exception e) {
      operation.setState(OperationState.FAILED);
      if(e instanceof WorkflowOperationException) throw (WorkflowOperationException)e;
      throw new WorkflowOperationException(e);
    }
  }
}
