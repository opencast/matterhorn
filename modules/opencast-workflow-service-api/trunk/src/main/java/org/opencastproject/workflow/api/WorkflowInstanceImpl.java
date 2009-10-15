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
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.jaxb.MediapackageType;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name="workflow-instance", namespace="http://workflow.opencastproject.org/")
@XmlRootElement(name="workflow-instance", namespace="http://workflow.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowInstanceImpl implements WorkflowInstance {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowInstanceImpl.class);

  public WorkflowInstanceImpl() {}
  
  public WorkflowInstanceImpl(WorkflowDefinition def) {
    this.title = def.getTitle();
    this.description = def.getDescription();
    this.workflowOperationDefinitionList = def.getOperations();
  }

  @XmlID
  @XmlAttribute()
  private String id;
  
  @XmlAttribute()
  private String state;

  @XmlElement(name="title")
  private String title;

  @XmlElement(name="description")
  private String description;

  @XmlElementWrapper(name="properties")
  private HashMap<String, String> properties;

  @XmlElement(name="mediapackage")
  private MediapackageType sourceMediaPackageType;
  
  @XmlElement(name="operation-definitions")
  private WorkflowOperationDefinitionList workflowOperationDefinitionList;

  @XmlElement(name="operation-instances")
  public WorkflowOperationInstanceList workflowOperationInstanceList;

  public String getId() {
    return id;
  }
  
  public void setId(String id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }
  
  public void setTitle(String title) {
    this.title = title;
  }
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }

  public State getState() {
    return State.valueOf(state);
  }
  public void setState(String state) {
    this.state = state;
  }

  public MediapackageType getSourceMediaPackageType() {
    return sourceMediaPackageType;
  }
  public void setSourceMediaPackageType(MediapackageType sourceMediaPackageType) {
    this.sourceMediaPackageType = sourceMediaPackageType;
  }
  
  public HashMap<String, String> getProperties() {
    return properties;
  }
  public void setProperties(HashMap<String, String> properties) {
    this.properties = properties;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#getSourceMediaPackage()
   */
  public MediaPackage getSourceMediaPackage() {
    if(sourceMediaPackageType == null) return null;
    try {
      return MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromManifest(IOUtils.toInputStream(sourceMediaPackageType.toXml()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#getCurrentOperation()
   */
  public WorkflowOperationInstance getCurrentOperation() {
    // Since we add operation instances only once they start, we can just return the last one in the list.
    List<WorkflowOperationInstance> list = workflowOperationInstanceList.getOperationInstance();
    if(list.isEmpty()) return null;
    return list.get(list.size()-1);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#getProperty(java.lang.String)
   */
  public String getProperty(String name) {
    return properties.get(name);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#removeProperty(java.lang.String)
   */
  public void removeProperty(String name) {
    properties.remove(name);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#setProperty(java.lang.String, java.lang.String)
   */
  public void setProperty(String name, String value) {
    properties.put(name, value);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#getWorkflowOperations()
   */
  public WorkflowOperationInstanceList getWorkflowOperationInstanceList() {
    if(workflowOperationInstanceList == null) workflowOperationInstanceList = new WorkflowOperationInstanceListImpl();
    return workflowOperationInstanceList;
  }

  public void setWorkflowOperationInstanceList(WorkflowOperationInstanceList workflowOperationInstanceList) {
    this.workflowOperationInstanceList = workflowOperationInstanceList;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#addWorkflowOperationDefinition(org.opencastproject.workflow.api.WorkflowOperationDefinition)
   */
  public void addWorkflowOperationDefinition(WorkflowOperationDefinition operation) {
    // TODO Auto-generated method stub
    
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#addWorkflowOperationDefinition(int, org.opencastproject.workflow.api.WorkflowOperationDefinition)
   */
  public void addWorkflowOperationDefinition(int location, WorkflowOperationDefinition operation) {
    // TODO Auto-generated method stub
    
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#addWorkflowOperationDefinitions(org.opencastproject.workflow.api.WorkflowOperationDefinitionList)
   */
  public void addWorkflowOperationDefinitions(WorkflowOperationDefinitionList operations) {
    getWorkflowOperationDefinitionList().getOperation().addAll(operations.getOperation());
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#addWorkflowOperationDefinitions(int, org.opencastproject.workflow.api.WorkflowOperationDefinitionList)
   */
  public void addWorkflowOperationDefinitions(int location, WorkflowOperationDefinitionList operations) {
    getWorkflowOperationDefinitionList().getOperation().addAll(location, operations.getOperation());
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workflow.api.WorkflowInstance#getWorkflowOperationDefinitionList()
   */
  public WorkflowOperationDefinitionList getWorkflowOperationDefinitionList() {
    return workflowOperationDefinitionList;
  }
  
  public void setWorkflowOperationDefinitionList(WorkflowOperationDefinitionList workflowOperationDefinitionList) {
    this.workflowOperationDefinitionList = workflowOperationDefinitionList;
  }

}
