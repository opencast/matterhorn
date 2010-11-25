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

import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Abstract base implementation for an operation handler, which implements a simple start operation that returns a
 * {@link WorkflowOperationResult} with the current mediapackage and {@link Action#CONTINUE}.
 */
public abstract class AbstractWorkflowOperationHandler implements WorkflowOperationHandler {

  /** The ID of this operation handler */
  protected String id = null;

  /** The description of what this handler actually does */
  protected String description = null;

  /** The configuration options for this operation handler */
  protected SortedMap<String, String> options = new TreeMap<String, String>();

  /**
   * Activates this component with its properties once all of the collaborating services have been set
   * 
   * @param cc
   *          The component's context, containing the properties used for configuration
   */
  protected void activate(ComponentContext cc) {
    this.id = (String) cc.getProperties().get(WorkflowService.WORKFLOW_OPERATION_PROPERTY);
    this.description = (String) cc.getProperties().get(Constants.SERVICE_DESCRIPTION);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public abstract WorkflowOperationResult start(WorkflowInstance workflowInstance) throws WorkflowOperationException;

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#destroy(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public void destroy(WorkflowInstance workflowInstance) throws WorkflowOperationException {
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return options;
  }

  /**
   * Adds a configuration option to the list of possible configuration options.
   * 
   * @param name
   *          the option name
   * @param description
   *          the option description
   */
  public void addConfigurationOption(String name, String description) {
    options.put(name, description);
  }

  /**
   * Removes the configuration option from the list of possible configuration options.
   * 
   * @param name
   *          the option name
   */
  public void removeConfigurationOption(String name) {
    options.remove(name);
  }

  /**
   * Converts a comma separated string into a set of values. Useful for converting operation configuration strings into
   * multi-valued sets.
   * 
   * @param commaSeparated
   *          The comma separated string
   * @return the set of values
   */
  protected List<String> asList(String commaSeparated) {
    commaSeparated = StringUtils.trimToNull(commaSeparated);
    List<String> list = new ArrayList<String>();
    if (commaSeparated != null) {
      for (String s : commaSeparated.split(",")) {
        if (StringUtils.trimToNull(s) != null) {
          list.add(s.trim());
        }
      }
    }
    return list;
  }

  /**
   * Generates a filename using the base name of a source element and the extension of a derived element.
   * 
   * @param source
   *          the source mediapackage element
   * @param derived
   *          the derived mediapackage element
   * @return the filename
   */
  protected String getFileNameFromElements(MediaPackageElement source, MediaPackageElement derived) {
    String fileName = FilenameUtils.getBaseName(source.getURI().toString());
    String fileExtension = FilenameUtils.getExtension(derived.getURI().toString());
    return fileName + "." + fileExtension;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getId()
   */
  @Override
  public String getId() {
    return id;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getDescription()
   */
  @Override
  public String getDescription() {
    return description;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : super.hashCode();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof WorkflowOperationHandler) {
      if (id != null)
        return id.equals(((WorkflowOperationHandler) obj).getId());
      else
        return this == obj;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return getId();
  }

}
