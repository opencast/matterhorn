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

import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Provides a mechanism to build a workflow definition from an xml inputstream.
 */
public class WorkflowBuilder {

  /** The singleton instance for this factory */
  private static WorkflowBuilder instance = null;

  protected JAXBContext jaxbContext = null;
  
  private WorkflowBuilder() throws JAXBException {
    StringBuilder sb = new StringBuilder();
    sb.append("org.opencastproject.media.mediapackage");
    sb.append(":org.opencastproject.workflow.api");
    jaxbContext= JAXBContext.newInstance(sb.toString(), WorkflowBuilder.class.getClassLoader());
  }
  
  /**
   * Returns an instance of the {@link WorkflowBuilder}.
   * 
   * @return a factory
   */
  public static WorkflowBuilder getInstance() {
    if (instance == null) {
      try {
        instance = new WorkflowBuilder();
      } catch (JAXBException e) {
        throw new RuntimeException(e.getLinkedException() != null ? e.getLinkedException() : e);
      }
    }
    return instance;
  }

  /**
   * Loads a workflow definition from the given input stream.
   * 
   * @param is
   *          the input stream
   * @return the workflow definition
   * @throws Exception
   *           if creating the workflow definition fails
   */
  public WorkflowDefinition parseWorkflowDefinition(InputStream in) throws Exception {
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    return unmarshaller.unmarshal(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in),
            WorkflowDefinitionImpl.class).getValue();
  }
  
  /**
   * Loads a workflow definition from the xml fragement.
   * 
   * @param definition
   *          xml fragment of the workflow definition
   * @return the workflow definition
   * @throws Exception
   *           if creating the workflow definition fails
   */
  public WorkflowDefinition parseWorkflowDefinition(String in) throws Exception {
    return parseWorkflowDefinition(IOUtils.toInputStream(in, "UTF8"));
  }
  
  
  /**
   * Loads a workflow instance from the given input stream.
   * 
   * @param is
   *          the input stream
   * @return the workflow instance
   * @throws Exception
   *           if creating the workflow instance fails
   */
  public WorkflowInstance parseWorkflowInstance(InputStream in) throws Exception {
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    WorkflowInstanceImpl workflow = unmarshaller.unmarshal(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in),
            WorkflowInstanceImpl.class).getValue();
    workflow.init();
    return workflow;
  }

  /**
   * Loads a workflow instance from the xml fragement.
   * 
   * @param instance
   *          xml fragment of the workflow instance
   * @return the workflow definition
   * @throws Exception
   *           if creating the workflow instance fails
   */
  public WorkflowInstance parseWorkflowInstance(String in) throws Exception {
    return parseWorkflowInstance(IOUtils.toInputStream(in, "UTF8"));
  }

  public String toXml(WorkflowInstance workflowInstance) throws Exception {
    Marshaller marshaller = jaxbContext.createMarshaller();
    Writer writer = new StringWriter();
    marshaller.marshal(workflowInstance, writer);
    return writer.toString();
  }

  public String toXml(WorkflowDefinition workflowDefinition) throws Exception {
    Marshaller marshaller = jaxbContext.createMarshaller();
    Writer writer = new StringWriter();
    marshaller.marshal(workflowDefinition, writer);
    return writer.toString();
  }

  public String toXml(List<WorkflowDefinition> list) throws Exception {
    Marshaller marshaller = jaxbContext.createMarshaller();
    Writer writer = new StringWriter();
    marshaller.marshal(new WorkflowDefinitionSet(list), writer);
    return writer.toString();
  }

  public String toXml(WorkflowSet set) throws Exception {
    Marshaller marshaller = jaxbContext.createMarshaller();
    Writer writer = new StringWriter();
    marshaller.marshal(set, writer);
    return writer.toString();
  }
  
  public WorkflowOperationResult buildWorkflowOperationResult(MediaPackage mediaPackage, Action action) {
    return new WorkflowOperationResultImpl(mediaPackage, action);
  }

}
