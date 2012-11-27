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

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.smil.api.SmilException;
import org.opencastproject.smil.api.SmilService;
import org.opencastproject.smil.entity.Smil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.videoeditor.api.ProcessFailedException;
import org.opencastproject.videoeditor.api.VideoEditorService;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VideoEditorWorkflowOperationHandler extends ResumableWorkflowOperationHandlerBase {

  private static final Logger logger = LoggerFactory
      .getLogger(VideoEditorWorkflowOperationHandler.class);

  /** Path to the hold ui resources */
  private static final String HOLD_UI_PATH = "/ui/operation/editor/index.html";

  /** Name of the configuration option that provides the source flavors we are looking for */
  private static final String SOURCE_FLAVOR_PROPERTY = "source-flavor";

  /** Name of the configuration option that provides the target flavors we will produce */
  private static final String TARGET_FLAVOR_SUBTYPE_PROPERTY = "target-flavor-subtype";

  /**
   * the smil service
   */
  private SmilService smilService;

  private VideoEditorService videoEditorService;
  
  private Workspace workspace;

  @Override
  public void activate(ComponentContext cc) {
    super.activate(cc);
    setHoldActionTitle("Review / VideoEdit");
    registerHoldStateUserInterface(HOLD_UI_PATH);
    logger.info("Registering videoEditor hold state ui from classpath {}", HOLD_UI_PATH);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
      throws WorkflowOperationException {
    MediaPackage mp = null;
    try {
      mp = workflowInstance.getMediaPackage();
      if (mp.getCatalogs(MediaPackageElementFlavor.parseFlavor("smil/smil")).length == 0) {
        logger.debug("adding SMIL to workflow");
        smilService.createNewSmil(workflowInstance.getId());
      }
    } catch (Exception e) {
      logger.error(e.getMessage());
      throw new WorkflowOperationException(e.getMessage(), e);
    }
    logger.info("Holding for video edit...");
    return createResult(mp, Action.PAUSE);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler#skip(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  @Override
  public WorkflowOperationResult skip(WorkflowInstance workflowInstance, JobContext context)
      throws WorkflowOperationException {
    // If we do not hold for trim, we still need to put tracks in the mediapackage with the right
    // flavor
    MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    WorkflowOperationInstance currentOperation = workflowInstance.getCurrentOperation();
    String configuredSourceFlavor = currentOperation.getConfiguration(SOURCE_FLAVOR_PROPERTY);
    String configuredTargetFlavorSubtype = currentOperation
        .getConfiguration(TARGET_FLAVOR_SUBTYPE_PROPERTY);
    MediaPackageElementFlavor matchingFlavor = MediaPackageElementFlavor
        .parseFlavor(configuredSourceFlavor);
    for (Track t : workflowInstance.getMediaPackage().getTracks()) {
      MediaPackageElementFlavor trackFlavor = t.getFlavor();
      if (trackFlavor != null && trackFlavor.matches(matchingFlavor)) {
        Track clonedTrack = (Track) t.clone();
        clonedTrack.setIdentifier(null);
        clonedTrack.setURI(t.getURI()); // use the same URI as the original
        clonedTrack.setFlavor(new MediaPackageElementFlavor(trackFlavor.getType(),
            configuredTargetFlavorSubtype));
        mediaPackage.addDerived(clonedTrack, t);
      }
    }
    return createResult(mediaPackage, Action.SKIP);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.ResumableWorkflowOperationHandler#resume(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext, java.util.Map)
   */
  @Override
  public WorkflowOperationResult resume(WorkflowInstance workflowInstance, JobContext context,
                                            Map<String, String> properties)
      throws WorkflowOperationException {

    logger.info("VideoEdit workflow {} using SMIL Document", workflowInstance.getId());

    Smil smil = null;
	try {
      smil = smilService.getSmil(workflowInstance.getId());
      logger.debug("SMIL is ready for processing");
    } catch (SmilException e) {
      throw new WorkflowOperationException("SMIL Exception", e);
    } catch (NotFoundException e) {
      throw new WorkflowOperationException("SMIL NOT FOUND", e);
    }
	
	MediaPackage mp = workflowInstance.getMediaPackage();
	WorkflowOperationInstance currentOperation = workflowInstance.getCurrentOperation();
    String configuredTargetFlavorSubtype = currentOperation.getConfiguration(TARGET_FLAVOR_SUBTYPE_PROPERTY);
	
	List<Job> jobs = null;
	try {
		jobs = videoEditorService.processSmil(smil);
		if (!waitForStatus(jobs.toArray(new Job[jobs.size()])).isSuccess()) {
			throw new WorkflowOperationException("Smil processing jobs are ended unsuccessfull");
		}
		
		for (Job job : jobs) {
			// throws MediaPackageException
			Track sourceTrack = (Track) MediaPackageElementParser.getFromXml(job.getArguments().get(1));
			// throws MediaPackageException
			Track encodedTrack = (Track) MediaPackageElementParser.getFromXml(job.getPayload());
			String encodedTrackFileName = FilenameUtils.getBaseName(encodedTrack.getURI().getPath().toString())
					+ "." + FilenameUtils.getExtension(encodedTrack.getURI().getPath().toString());
			// throws NotFoundException IOException IllegalArgumentException
			URI newUri = workspace.moveTo(encodedTrack.getURI(), mp.getIdentifier().compact(), 
					encodedTrack.getIdentifier(), encodedTrackFileName);
            encodedTrack.setURI(newUri);
			encodedTrack.setFlavor(new MediaPackageElementFlavor(sourceTrack.getFlavor().getType(), configuredTargetFlavorSubtype));
			mp.addDerived(encodedTrack, sourceTrack);
		}
		logger.info("SMIL {} processing finished", smil.getId());
		
		return createResult(mp, Action.CONTINUE);
		
	} catch (NotFoundException ex) {
		throw new WorkflowOperationException(ex);
	} catch (IOException ex) {
		throw new WorkflowOperationException(ex);
	} catch (IllegalArgumentException ex) {
		throw new WorkflowOperationException(ex);
	} catch (ProcessFailedException ex) {
		throw new WorkflowOperationException(ex);
	} catch (MediaPackageException ex) {
		throw new WorkflowOperationException(ex);
	} catch (Exception ex) {
		throw new WorkflowOperationException(ex);
	}
  }

  public void setSmilService(SmilService smilService) {
    this.smilService = smilService;
  }
  
  public void setVideoEditorService(VideoEditorService editor) {
    this.videoEditorService = editor;
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }
}
