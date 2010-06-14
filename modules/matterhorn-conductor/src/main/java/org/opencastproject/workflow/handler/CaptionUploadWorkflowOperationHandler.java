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
package org.opencastproject.workflow.handler;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.workflow.api.AbstractResumableWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Workflow operation that waits for a caption file to be uploaded.
 */
public class CaptionUploadWorkflowOperationHandler extends AbstractResumableWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CaptionUploadWorkflowOperationHandler.class);

  /** The key used to find the configured caption flavor */
  private static final String FLAVOR_PROPERTY = "caption-flavor";

  /** The default caption flavor if none is configured */
  private static final MediaPackageElementFlavor DEFAULT_FLAVOR = MediaPackageElements.CAPTION_DFXP_FLAVOR;

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  /** Path to the caption upload ui resources */
  private static final String HOLD_UI_PATH = "/ui/operation/caption/index.html";

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put(REQUIRED_PROPERTY, "The configuration key that must be set to true for this operation to run.");
    CONFIG_OPTIONS.put(FLAVOR_PROPERTY, "The configuration key that identifies the required caption flavor.");
  }

  /** The http service to use in registering the caption upload UI */
  protected HttpService httpService;
  
  /** The http context to use in registering the caption upload UI */
  protected HttpContext httpContext;

  public void setHttpService(HttpService service) {
    super.httpService = service;
  }
  
  public void setHttpContext(HttpContext httpContext) {
    super.httpContext = httpContext;
  }

  public void activate(ComponentContext cc) {
    super.activate(cc);
    setHoldActionTitle("Caption Upload");
    registerHoldStateUserInterface(HOLD_UI_PATH);
    logger.info("Registering caption upload hold state ui from classpath {}", HOLD_UI_PATH);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.AbstractResumableWorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance) throws WorkflowOperationException {
    MediaPackageElementFlavor flavor = getFlavor(workflowInstance.getCurrentOperation());
    if ("true".equalsIgnoreCase(workflowInstance.getCurrentOperation().getConfiguration(REQUIRED_PROPERTY))
            && !hasCaptions(workflowInstance.getMediaPackage(), flavor))
      return WorkflowBuilder.getInstance().buildWorkflowOperationResult(Action.PAUSE);
    else
      return WorkflowBuilder.getInstance().buildWorkflowOperationResult(Action.CONTINUE);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.AbstractResumableWorkflowOperationHandler#resume(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public WorkflowOperationResult resume(WorkflowInstance workflowInstance) throws WorkflowOperationException {
    MediaPackageElementFlavor flavor = getFlavor(workflowInstance.getCurrentOperation());
    boolean hasCaptions = hasCaptions(workflowInstance.getMediaPackage(), flavor);
    if (hasCaptions) {
      return WorkflowBuilder.getInstance().buildWorkflowOperationResult(Action.CONTINUE);
    } else {
      // The user should have verified the existence of a caption file in, or if necessary added one to, the mediapackage
      logger.info("No DFXP caption file attached, keeping workflow {} in the hold state", workflowInstance);
      return WorkflowBuilder.getInstance().buildWorkflowOperationResult(Action.PAUSE);
    }
  }

  protected boolean hasCaptions(MediaPackage mp, MediaPackageElementFlavor flavor) {
    return mp.getElementsByFlavor(flavor).length > 0;
  }

  protected MediaPackageElementFlavor getFlavor(WorkflowOperationInstance operation) throws WorkflowOperationException {
    String configuredFlavor = operation.getConfiguration(FLAVOR_PROPERTY);
    if (configuredFlavor == null) {
      return DEFAULT_FLAVOR;
    } else {
      try {
        return MediaPackageElementFlavor.parseFlavor(configuredFlavor);
      } catch (IllegalArgumentException e) {
        throw new WorkflowOperationException(e.getMessage(), e);
      }
    }
  }
}
