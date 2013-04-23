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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.xml.bind.JAXBException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.smil.api.SmilException;
import org.opencastproject.smil.api.SmilResponse;
import org.opencastproject.smil.api.SmilService;
import org.opencastproject.smil.entity.api.Smil;
import org.opencastproject.smil.entity.media.api.SmilMediaObject;
import org.opencastproject.smil.entity.media.container.api.SmilMediaContainer;
import org.opencastproject.smil.entity.media.element.api.SmilMediaElement;
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
import org.xml.sax.SAXException;

public class VideoEditorWorkflowOperationHandler extends ResumableWorkflowOperationHandlerBase {

	private static final Logger logger = LoggerFactory
			.getLogger(VideoEditorWorkflowOperationHandler.class);
	/**
	 * Path to the hold ui resources
	 */
	private static final String HOLD_UI_PATH = "/ui/operation/editor/index.html";
	
	/**
	 * Name of the configuration option that provides the source flavors we are
	 * looking for
	 */
	private static final String SOURCE_FLAVOR_PROPERTY = "source-flavor";
	/**
	 * Name of the configuration option that provides the preview flavors we are
	 * looking for
	 */
	private static final String PREVIEW_FLAVOR_PROPERTY = "preview-flavor";
	/**
	 * Name of the configuration option that provides the smil flavor
	 */
	private static final String SMIL_FLAVOR_PROPERTY = "smil-flavor";
	/**
	 * Name of the configuration that provides the target flavor subtype we will produce
	 */
	private static final String TARGET_FLAVOR_SUBTYPE_PROPERTY = "target-flavor-subtype";
	/**
	 * Name of the configuration that provides the smil file name
	 */
	private static final String SMIL_FILE_NAME = "smil.smil";

	/** The configuration options for this handler */
	private static final SortedMap<String, String> CONFIG_OPTIONS;
	static {
		CONFIG_OPTIONS = new TreeMap<String, String>();
		CONFIG_OPTIONS.put(SOURCE_FLAVOR_PROPERTY, "The flavor for work files (tracks to edit).");
		CONFIG_OPTIONS.put(PREVIEW_FLAVOR_PROPERTY, "The flavor for preview files (tracks to show in edit UI as webm).");
		CONFIG_OPTIONS.put(SMIL_FLAVOR_PROPERTY, "The flavor for smil files.");
		CONFIG_OPTIONS.put(TARGET_FLAVOR_SUBTYPE_PROPERTY, "The flavor subtype for target (generated) files.");
	}

	/**
	 * The Smil service to modify smil files.
	 */
	private SmilService smilService;
	/**
	 * The VideoEditor service to edit files.
	 */
	private VideoEditorService videoEditorService;
	/**
	 * The Ingest service to ingest produced files.
	 */
	private IngestService ingestService;
	/**
	 * The workspace.
	 */
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
	 * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
	 */
	@Override
	public SortedMap<String, String> getConfigurationOptions() {
		return CONFIG_OPTIONS;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see
	 * org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
	 * JobContext)
	 */
	@Override
	public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
			throws WorkflowOperationException {
		MediaPackage mp = null;
		try {
			mp = workflowInstance.getMediaPackage();
			// get preview tracks
			MediaPackageElementFlavor previewFlavor = getFlavor(workflowInstance, PREVIEW_FLAVOR_PROPERTY);
			Track[] previewTracks = getTracksFromFlavor(mp, previewFlavor);
			// get smil
			MediaPackageElementFlavor smilFlavor = getFlavor(workflowInstance, SMIL_FLAVOR_PROPERTY);
			Catalog[] smilCatalogs = mp.getCatalogs(smilFlavor);

			Smil smil = null;
			SmilResponse smilResponse = null;
			if (smilCatalogs.length == 0) {
				// mediapackage does not contain any smil, create new
				smilResponse = smilService.createNewSmil(mp);
				smilResponse = smilService.addParallel(smilResponse.getSmil());
				smilResponse = smilService.addClips(smilResponse.getSmil(),
						smilResponse.getEntity().getId(),
						previewTracks, 0L, previewTracks[0].getDuration());
				smil = smilResponse.getSmil();
				// ingest new smil
				mp = ingestService.addCatalog(IOUtils.toInputStream(smil.toXML(), "UTF-8"),
						SMIL_FILE_NAME, smilFlavor, mp);
        workflowInstance.setMediaPackage(mp);
			} else {
				// mediapackage contain a smil, test preview tracks set
				File smilFile = workspace.get(smilCatalogs[0].getURI());
				smilResponse = smilService.fromXml(smilFile);
				smil = replaceAllTracksWith(smilResponse.getSmil(), previewTracks);
        workspace.put(mp.getIdentifier().compact(), smilCatalogs[0].getIdentifier(),
                SMIL_FILE_NAME, IOUtils.toInputStream(smil.toXML(), "UTF-8"));
      }
		} catch (Exception e) {
			throw new WorkflowOperationException(e.getMessage(), e);
		}

		logger.info("Holding for video edit...");
		return createResult(mp, Action.PAUSE);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see
	 * org.opencastproject.workflow.api.AbstractWorkflowOperationHandler#skip(org.opencastproject.workflow.api.WorkflowInstance,
	 * JobContext)
	 */
	@Override
	public WorkflowOperationResult skip(WorkflowInstance workflowInstance, JobContext context)
			throws WorkflowOperationException {
		// If we do not hold for trim, we still need to put tracks in the mediapackage with the target
		// flavor
		MediaPackage mediaPackage = workflowInstance.getMediaPackage();
		WorkflowOperationInstance currentOperation = workflowInstance.getCurrentOperation();
		String configuredSourceFlavor = currentOperation.getConfiguration(SOURCE_FLAVOR_PROPERTY);
		String configuredTargetFlavorSubtype = currentOperation
				.getConfiguration(TARGET_FLAVOR_SUBTYPE_PROPERTY);
		MediaPackageElementFlavor matchingFlavor = MediaPackageElementFlavor
				.parseFlavor(configuredSourceFlavor);
		for (Track t : getTracksFromFlavor(mediaPackage, matchingFlavor)) {
			Track clonedTrack = (Track) t.clone();
			clonedTrack.setIdentifier(null);
			clonedTrack.setURI(t.getURI()); // use the same URI as the original
			clonedTrack.setFlavor(new MediaPackageElementFlavor(t.getFlavor().getType(),
					configuredTargetFlavorSubtype));
			mediaPackage.addDerived(clonedTrack, t);
		}
		return createResult(mediaPackage, Action.SKIP);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.opencastproject.workflow.api.ResumableWorkflowOperationHandler#resume(org.opencastproject.workflow.api.WorkflowInstance, JobContext, java.util.Map)
	 */
	@Override
	public WorkflowOperationResult resume(WorkflowInstance workflowInstance, JobContext context,
			Map<String, String> properties)
			throws WorkflowOperationException {

		logger.info("VideoEdit workflow {} using SMIL Document", workflowInstance.getId());
		MediaPackage mp = workflowInstance.getMediaPackage();

		// retrieve smil from mediapackage
		MediaPackageElementFlavor smilFlavor = getFlavor(workflowInstance, SMIL_FLAVOR_PROPERTY);
		Catalog[] catalogs = mp.getCatalogs(smilFlavor);
		if (catalogs.length == 0) {
			throw new WorkflowOperationException("MediaPackage does not contain a SMIL document.");
		}
		
		MediaPackageElementFlavor sourceFlavor = getFlavor(workflowInstance, SOURCE_FLAVOR_PROPERTY);
		Track[] sourceTracks = getTracksFromFlavor(mp, sourceFlavor);

		File smilFile;
		Smil smil = null;
		try {
			smilFile = workspace.get(catalogs[0].getURI());
			smil = smilService.fromXml(smilFile).getSmil();
			smil = replaceAllTracksWith(smil, sourceTracks);
      workspace.put(mp.getIdentifier().compact(), catalogs[0].getIdentifier(),
              SMIL_FILE_NAME, IOUtils.toInputStream(smil.toXML(), "UTF-8"));
		} catch (NotFoundException ex) {
			throw new WorkflowOperationException("MediaPackage does not contain a smil catalog.");
		} catch (IOException ex) {
			throw new WorkflowOperationException("Failed to read smil catalog.", ex);
		} catch (SmilException ex) {
			throw new WorkflowOperationException(ex);
		} catch (JAXBException ex) {
      throw new WorkflowOperationException("Unable to serialize SMIL working version.", ex);
    } catch (SAXException ex) {
      throw new WorkflowOperationException("Unable to serialize SMIL working version.", ex);
    }

		// create video edit jobs and run them
		List<Job> jobs = null;
		try {
			logger.info("Create processing jobs for smil " + smil.getId());
			jobs = videoEditorService.processSmil(smil);
			if (!waitForStatus(jobs.toArray(new Job[jobs.size()])).isSuccess()) {
				throw new WorkflowOperationException("Smil processing jobs for smil "
						+ smil.getId() + " are ended unsuccessfull.");
			}
			logger.info("Smil " + smil.getId() + " processing finished.");
		} catch (ProcessFailedException ex) {
			throw new WorkflowOperationException("Processing smil " + smil.getId() + " failed", ex);
		}

		String targetFlavorSubType = workflowInstance.getCurrentOperation().getConfiguration(TARGET_FLAVOR_SUBTYPE_PROPERTY);

		// move edited tracks to work location and set target flavor
		Track sourceTrack;
		Track editedTrack;
		for (Job job : jobs) {
			try {
				editedTrack = (Track) MediaPackageElementParser.getFromXml(job.getPayload());
				MediaPackageElementFlavor editedTrackFlavor = editedTrack.getFlavor();
				sourceTrack = null;
				for (Track track : sourceTracks) {
					if (track.getFlavor().getType().equals(editedTrackFlavor.getType())) {
						sourceTrack = track;
						break;
					}
				}
				if (sourceTrack == null) {
					throw new WorkflowOperationException(String.format("MediaPackage does not contain track with %s flavor.",
							new MediaPackageElementFlavor(editedTrack.getFlavor().getType(), targetFlavorSubType).toString()));
				}

				String newTrackFileName = String.format("%s-%s.%s", editedTrackFlavor.getType(), targetFlavorSubType,
						FilenameUtils.getExtension(editedTrack.getURI().getPath().toString()));
				URI newUri = workspace.moveTo(editedTrack.getURI(), mp.getIdentifier().compact(),
						editedTrack.getIdentifier(), newTrackFileName);
				editedTrack.setURI(newUri);
				editedTrack.setFlavor(new MediaPackageElementFlavor(editedTrack.getFlavor().getType(), targetFlavorSubType));
				mp.addDerived(editedTrack, sourceTrack);
			} catch (MediaPackageException ex) {
				throw new WorkflowOperationException("Failed to get edited track information.", ex);
			} catch (NotFoundException ex) {
				throw new WorkflowOperationException("Moving edited track to work location failed.", ex);
			} catch (IOException ex) {
				throw new WorkflowOperationException("Moving edited track to work location failed.", ex);
			} catch (IllegalArgumentException ex) {
				throw new WorkflowOperationException("Moving edited track to work location failed.", ex);
			}
		}

		logger.info("VideoEdit workflow {} finished", workflowInstance.getId());

		return createResult(mp, Action.CONTINUE);

	}

	protected MediaPackageElementFlavor getFlavor(WorkflowInstance workflowInstance, String configurationKey) {
		String flavor = workflowInstance.getCurrentOperation().getConfiguration(configurationKey);
		return MediaPackageElementFlavor.parseFlavor(flavor);
	}

	protected Track[] getTracksFromFlavor(MediaPackage mediaPackage, MediaPackageElementFlavor flavor) {
		List<Track> tracks = new LinkedList<Track>();
		for (Track track : mediaPackage.getTracks()) {
			MediaPackageElementFlavor trackFlavor = track.getFlavor();
			if (trackFlavor.matches(flavor)) {
				tracks.add(track);
			}
		}
		return tracks.toArray(new Track[tracks.size()]);
	}

	protected Smil replaceAllTracksWith(Smil smil, Track[] otherTracks) throws SmilException {
		SmilResponse smilResponse;
		try {
			// copy smil to work with
			smilResponse = smilService.fromXml(smil.toXML());
		} catch (Exception ex) {
			throw new SmilException("Can't parse smil.");
		}
		
		long start;
		long end;
		// iterate over all elements inside smil body
		for (SmilMediaObject elem : smil.getBody().getMediaElements()) {
			start = -1L;
			end = -1L;
			// body should contain par elements (container)
			if (elem.isContainer()) {
				// iterate over all elements in container
				for (SmilMediaObject child : ((SmilMediaContainer)elem).getElements())  {
					// second depth should contain media elements like audio or video
					if (!child.isContainer() && child instanceof SmilMediaElement) {
						SmilMediaElement media = (SmilMediaElement) child;
						start = media.getClipBeginMS();
						end = media.getClipEndMS();
						// remove it
						smilResponse = smilService.removeSmilElement(smilResponse.getSmil(), media.getId());
					}
				}
				if (start != -1L && end != -1L) {
					// add the new tracks inside
					smilResponse = smilService.addClips(smilResponse.getSmil(), elem.getId(), otherTracks, start, end - start);
				}
			} else if (elem instanceof SmilMediaElement) {
				throw new SmilException("Media elements inside smil body are not supported yet.");
			}
		}
		return smilResponse.getSmil();
	}

	public void setSmilService(SmilService smilService) {
		this.smilService = smilService;
	}

	public void setVideoEditorService(VideoEditorService editor) {
		this.videoEditorService = editor;
	}

	public void setIngestService(IngestService ingestService) {
		this.ingestService = ingestService;
	}

	public void setWorkspace(Workspace workspace) {
		this.workspace = workspace;
	}
}
