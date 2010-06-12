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

import org.opencastproject.analysis.api.MediaAnalysisService;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogImpl;
import org.opencastproject.remote.api.Receipt;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * The workflow definition will run suitable recordings by the video segmentation.
 */
public class VideoSegmenterWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(VideoSegmenterWorkflowOperationHandler.class);
  
  /** Name of the configuration key that specifies the flavor of the track to analyze */ 
  private static final String PROP_ANALYSIS_TRACK_FLAVOR = "source-flavor";

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put(PROP_ANALYSIS_TRACK_FLAVOR, "The flavor of the track to analyze.  If multiple tracks match this flavor, the first will be used.");
  }

  /** The local workspace */
  private Workspace workspace = null;

  /** The composer service */
  private MediaAnalysisService videosegmenter = null;

  /** The trusted http client, used to load mpeg7 catalogs */
  private TrustedHttpClient trustedHttpClient = null;

  /**
   * Sets the http client
   * @param trustedHttpClient
   */
  protected void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    this.trustedHttpClient = trustedHttpClient;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance)
   */
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance) throws WorkflowOperationException {
    logger.debug("Running video segmentation on workflow {}", workflowInstance.getId());
    
    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();
    MediaPackage mediaPackage = workflowInstance.getMediaPackage();

    // Find movie track to analyze
    String trackFlavor = StringUtils.trimToNull(operation.getConfiguration(PROP_ANALYSIS_TRACK_FLAVOR));
    List<Track> candidates = new ArrayList<Track>();
    if (trackFlavor != null)
      candidates.addAll(Arrays.asList(mediaPackage.getTracks(MediaPackageElementFlavor.parseFlavor(trackFlavor))));
    else
      candidates.addAll(Arrays.asList(mediaPackage.getTracks(MediaPackageElements.PRESENTATION_SOURCE)));
    if (candidates.size() == 0) {
      logger.info("No matching tracks available for video segmentation in workflow {}", workflowInstance);
      return WorkflowBuilder.getInstance().buildWorkflowOperationResult(Action.CONTINUE);
    }

    // Remove unsupported tracks (only those containing video can be segmented)
    Iterator<Track> ti = candidates.iterator();
    while (ti.hasNext()) {
      Track t = ti.next();
      if (!t.hasVideo())
        ti.remove();
    }

    // More than one left? Let's be pragmatic...
    if (candidates.size() > 1) {
      logger.info("Found more than one track to segment, choosing the first one ({})", candidates.get(0));
    }
    Track track = candidates.get(0);
    
    // Segment the media package
    Mpeg7CatalogImpl mpeg7 = null;
    try {
      Receipt receipt = videosegmenter.analyze(track, true);
      if (receipt.getStatus().equals(Receipt.Status.FAILED)) {
        throw new WorkflowOperationException("Videosegmentation on " + track + " failed");
      }
      mpeg7 = new Mpeg7CatalogImpl((Catalog)receipt.getElement());
      mpeg7.setTrustedHttpClient(trustedHttpClient);
      mpeg7.setFlavor(MediaPackageElements.SEGMENTS_FLAVOR);
      mediaPackage.add(mpeg7);
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }
    
    // Store the catalog in the workspace
    // TODO: this is too complicated! Add update() method to metadata service
    try {
      Transformer tf = TransformerFactory.newInstance().newTransformer();
      DOMSource xmlSource = new DOMSource(mpeg7.toXml());
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      tf.transform(xmlSource, new StreamResult(out));
      InputStream in = new ByteArrayInputStream(out.toByteArray());
      
      // Put the result into the workspace
      String mediaPackageId = mediaPackage.getIdentifier().toString();
      String filename = track.getIdentifier() + "-segments.xml";
      URI workspaceURI = workspace.put(mediaPackageId, mpeg7.getIdentifier(), filename, in);
      mpeg7.setURI(workspaceURI);
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }

    logger.debug("Video segmentation completed");
    return WorkflowBuilder.getInstance().buildWorkflowOperationResult(mediaPackage, Action.CONTINUE);
  }

  /**
   * Callback for declarative services configuration that will introduce us to the local workspace service.
   * Implementation assumes that the reference is configured as being static.
   * 
   * @param workspace
   *          an instance of the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Callback for declarative services configuration that will introduce us to the videosegmenter service.
   * Implementation assumes that the reference is configured as being static.
   * 
   * @param videosegmenter
   *          the video segmenter
   */
  protected void setVideoSegmenter(MediaAnalysisService videosegmenter) {
    this.videosegmenter = videosegmenter;
  }

}
