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

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.AbstractMediaPackageElement;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageReference;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The workflow definition for handling "distribute" operations
 */
public class DistributeWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(DistributeWorkflowOperationHandler.class);

  /** The distribution service */
  private DistributionService distributionService = null;

  /**
   * Callback for the OSGi declarative services configuration.
   * 
   * @param distributionService
   *          the distribution service
   */
  protected void setDistributionService(DistributionService distributionService) {
    this.distributionService = distributionService;
  }

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put("source-tags",
            "Distribute any mediapackage elements with one of these (comma separated) tags.  If a source-tag "
                    + "starts with a '-', mediapackage elements with this tag will be excluded from distribution.");
    CONFIG_OPTIONS.put("target-tags",
            "Apple these (comma separated) tags to any mediapackage elements produced as a result of distribution");
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
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance)
   */
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance) throws WorkflowOperationException {
    logger.debug("Running distribution workflow operation");

    MediaPackage mediaPackage = workflowInstance.getMediaPackage();

    try {

      // Check which tags have been configured
      String sourceTags = workflowInstance.getCurrentOperation().getConfiguration("source-tags");
      String targetTags = workflowInstance.getCurrentOperation().getConfiguration("target-tags");
      if (StringUtils.trimToNull(sourceTags) == null) {
        logger.warn("No tags have been specified");
        return createResult(mediaPackage, Action.CONTINUE);
      }

      // Look for elements matching the tag
      Set<String> elementIds = new HashSet<String>();
      MediaPackageElement[] elts = mediaPackage.getElementsByTags(asList(sourceTags));
      for (MediaPackageElement e : elts) {
        if (elementIds.add(e.getIdentifier())) {
          logger.info("Distributing '{}' to the local repository", e.getIdentifier(), mediaPackage);
        }
      }

      // Also distribute all of the metadata catalogs
      for (Catalog c : mediaPackage.getCatalogs())
        elementIds.add(c.getIdentifier());

      // Finally, push the elements to the distribution channel
      List<String> targetTagList = asList(targetTags);

      try {
        for (String elementId : elementIds) {
          MediaPackageElement element = mediaPackage.getElementById(elementId);
          if (element == null)
            throw new WorkflowOperationException("Unable to find element " + elementId);
          Job job = distributionService.distribute(mediaPackage.getIdentifier().compact(), element, true);
          if (job == null || !Job.Status.FINISHED.equals(job.getStatus())) {
            throw new WorkflowOperationException("Distribution job " + job + " did not complete successfully");
          }

          // If there is no payload, then the item has not been distributed.
          if (job.getPayload() == null) {
            continue;
          }

          MediaPackageElement newElement = null;
          try {
            newElement = AbstractMediaPackageElement.getFromXml(job.getPayload());
          } catch (MediaPackageException e) {
            throw new WorkflowOperationException(e);
          }
          // If the job finished successfully, but returned no new element, the channel simply doesn't support this
          // kind of element. So we just keep on looping.
          if (newElement == null) {
            continue;
          }
          newElement.setIdentifier(null);
          MediaPackageReference ref = element.getReference();
          if (ref != null && mediaPackage.getElementByReference(ref) != null) {
            newElement.setReference((MediaPackageReference) ref.clone());
            mediaPackage.add(newElement);
          } else {
            mediaPackage.addDerived(newElement, element);
            if (ref != null) {
              Map<String, String> props = ref.getProperties();
              newElement.getReference().getProperties().putAll(props);
            }
          }

          for (String tag : targetTagList) {
            if (StringUtils.trimToNull(tag) == null)
              continue;
            newElement.addTag(tag);
          }
        }
      } catch (DistributionException e) {
        throw new WorkflowOperationException(e);
      }
      logger.debug("Distribute operation completed");
    } catch (Exception e) {
      if (e instanceof WorkflowOperationException) {
        throw (WorkflowOperationException) e;
      } else {
        throw new WorkflowOperationException(e);
      }
    }
    return createResult(mediaPackage, Action.CONTINUE);
  }

}
