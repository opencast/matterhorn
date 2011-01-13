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
package org.opencastproject.remotetest.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import static org.opencastproject.remotetest.Main.BASE_URL;

import org.opencastproject.remotetest.Main;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/**
 * Utility class that deals with workflows.
 */
public final class WorkflowUtils {

  /**
   * This utility class is not meant to be instantiated.
   */
  private WorkflowUtils() {
    // Nothing to do here
  }

  /**
   * Checks whether the given workflow is in the requested operation.
   * 
   * @param workflowId
   *          identifier of the workflow
   * @param operation
   *          the operation that the workflow is expected to be in
   * @return <code>true</code> if the workflow is in the expected state
   * @throws IllegalStateException
   *           if the specified workflow can't be found
   */
  public static boolean isWorkflowInOperation(String workflowId, String operation) throws IllegalStateException,
          Exception {
    HttpGet getWorkflowMethod = new HttpGet(BASE_URL + "/workflow/rest/instance/" + workflowId + ".xml");
    TrustedHttpClient client = Main.getClient();
    String workflow = EntityUtils.toString(client.execute(getWorkflowMethod).getEntity());
    String currentOperation = (String) Utils.xpath(workflow, "//operation[@state='PAUSED']/@id", XPathConstants.STRING);
    Main.returnClient(client);
    return operation.equalsIgnoreCase(currentOperation);
  }

  /**
   * Checks whether the given workflow is in the requested state.
   * 
   * @param workflowId
   *          identifier of the workflow
   * @param state
   *          the state that the workflow is expected to be in
   * @return <code>true</code> if the workflow is in the expected state
   * @throws IllegalStateException
   *           if the specified workflow can't be found
   */
  public static boolean isWorkflowInState(String workflowId, String state) throws IllegalStateException, Exception {
    HttpGet getWorkflowMethod = new HttpGet(BASE_URL + "/workflow/rest/instance/" + workflowId + ".xml");
    TrustedHttpClient client = Main.getClient();
    String workflow = EntityUtils.toString(client.execute(getWorkflowMethod).getEntity());
    String currentState = (String) Utils.xpath(workflow, "/ns2:workflow/@state", XPathConstants.STRING);
    Main.returnClient(client);
    return state.equalsIgnoreCase(currentState);
  }

  /**
   * Parses the workflow instance represented by <code>xml</code> and extracts the workflow identifier.
   * 
   * @param xml
   *          the workflow instance
   * @return the workflow instance
   * @throws Exception
   *           if parsing fails
   */
  public static String getWorkflowInstanceId(String xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(IOUtils.toInputStream(xml, "UTF-8"));
    return ((Element) XPathFactory.newInstance().newXPath().compile("/*").evaluate(doc, XPathConstants.NODE))
            .getAttribute("id");
  }

  /**
   * Parses the workflow instance represented by <code>xml</code> and extracts the workflow state.
   * 
   * @param xml
   *          the workflow instance
   * @return the workflow state
   * @throws Exception
   *           if parsing fails
   */
  public static String getWorkflowState(String xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(IOUtils.toInputStream(xml, "UTF-8"));
    return ((Element) XPathFactory.newInstance().newXPath().compile("/*").evaluate(doc, XPathConstants.NODE))
            .getAttribute("state");
  }

  /**
   * Registers a new workflow definition
   * 
   * @param workflowDefinition
   *          the new workflow definition
   * @return the id of the workflow definition
   */
  public static String registerWorkflowDefinition(String workflowDefinition) throws Exception {
    HttpPut put = new HttpPut(BASE_URL + "/workflow/rest/definition");
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("workflowDefinition", workflowDefinition));
    put.setEntity(new UrlEncodedFormEntity(params));
    TrustedHttpClient client = Main.getClient();
    HttpResponse response = client.execute(put);
    assertEquals(HttpStatus.SC_CREATED, response.getStatusLine().getStatusCode());
    String id = (String) Utils.xpath(workflowDefinition, "/definition/id",
            XPathConstants.STRING);
    assertNotNull(id);
    Main.returnClient(client);
    return id;
  }

  public static void unregisterWorkflowDefinition(String workflowDefinitionId) throws Exception {
    HttpDelete delete = new HttpDelete(BASE_URL + "/workflow/rest/definition/" + workflowDefinitionId);
    TrustedHttpClient client = Main.getClient();
    HttpResponse response = client.execute(delete);
    Main.returnClient(client);
    assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusLine().getStatusCode());
  }
}
