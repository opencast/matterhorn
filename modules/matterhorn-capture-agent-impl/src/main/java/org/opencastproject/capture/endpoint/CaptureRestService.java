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
package org.opencastproject.capture.endpoint;

import org.opencastproject.capture.api.CaptureAgent;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Param.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The REST endpoint for the capture agent service on the capture device
 */
@Path("/")
public class CaptureRestService {
  private static final Logger logger = LoggerFactory.getLogger(CaptureRestService.class);
  private CaptureAgent service;
  protected String docs = null;


  protected String generateDocs() {
    DocRestData data = new DocRestData("CaptureAgent", "Capture Agent", "/capture/rest", null);
    //// startCapture signatures
    // startCapture()
    RestEndpoint startNoParamEndpoint = new RestEndpoint("startNP", RestEndpoint.Method.GET, "/startCapture", "Starts a capture with the default parameters");
    startNoParamEndpoint.addFormat(new Format("String", "The recording ID for the capture started", null));
    startNoParamEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("valid request, results returned"));
    startNoParamEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("couldn't start capture with default parameters"));
    startNoParamEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, startNoParamEndpoint);
    // startCapture(Properties)
    RestEndpoint startPropEndpoint = new RestEndpoint("startMP", RestEndpoint.Method.POST, "/startCapture", "Starts a capture with the default properties and a provided MediaPackage");
    startPropEndpoint.addFormat(new Format("String", "The recording ID for the capture started", null));
    startPropEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("valid request, results returned"));
    startPropEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("couldn't start capture with provided parameters"));
    // This is to get the default value for capture.properties from source.opencastproject.org
    Param config = 
      new Param (
              "config", 
              Type.STRING, 
              null, 
              "The properties to set for this recording. " +
              "Those are specified in key-value pairs as described in " +
              "<a href=\"http://java.sun.com/javase/6/docs/api/java/util/Properties.html#load(java.io.Reader)\"> " +
              "this JavaDoc</a>. The current default properties can be found at " +
              "<a href=\"http://opencast.jira.com/svn/MH/trunk/docs/felix/conf/services/org.opencastproject.capture.impl.ConfigurationManager.properties\"> " +
              "this location</a>"
      );
    startPropEndpoint.addRequiredParam(config);
    startPropEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, startPropEndpoint);

    //// stopCapture signatures
    // stopCapture()
    RestEndpoint stopNoParamEndpoint = new RestEndpoint("stopNP", RestEndpoint.Method.GET, "/stopCapture", "Stops the current capture");
    stopNoParamEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("recording properly stopped"));
    stopNoParamEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("failed to stop the capture, or no current active capture"));
    stopNoParamEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, stopNoParamEndpoint);
    // stopCapture(recordingID)
    RestEndpoint stopIDEndpoint = new RestEndpoint("stopID", RestEndpoint.Method.POST, "/stopCapture", "Stops the current capture if its ID matches the argument");
    stopIDEndpoint.addRequiredParam(new Param("recordingID", Type.STRING, null, "The ID for the recording to stop"));
    stopIDEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("current capture with the specified ID stopped succesfully"));
    // TODO: check if this can be returned
    //stopIDEndpoint.addStatus(org.opencastproject.util.doc.Status.NOT_FOUND("A workflow instance with this ID was not found"));
    stopIDEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("failed to stop the capture, no current active capture, or no matching ID"));
    stopIDEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, stopIDEndpoint);

    //// ingest(recordingID)
    RestEndpoint ingestEndpoint = new RestEndpoint("ingest", RestEndpoint.Method.POST, "/ingest", "Ingests the specified capture");
    ingestEndpoint.addRequiredParam(new Param("recordingID", Type.STRING, null, "The ID for the recording to ingest"));
    ingestEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("capture ingested succesfully"));
    ingestEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("ingestion failed"));
    ingestEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, ingestEndpoint);

    //// configuration()
    RestEndpoint configEndpoint = new RestEndpoint("config", RestEndpoint.Method.GET, "/configuration", "Returns a list with the default agent configuration properties");
    configEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("the configuration values are returned"));
    configEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("the configuration properties could not be retrieved"));
    configEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, configEndpoint);

    return DocUtil.generate(data);
  }  

  public void setService(CaptureAgent service) {
    this.service = service;
  }

  public void unsetService(CaptureAgent service) {
    this.service = null;
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("startCapture")
  public Response startCapture() {
    if (service == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE).entity("Capture Agent is unavailable, please wait...").build();
    }

    String out;
    try {
      out = service.startCapture();
      if (out != null) {
        return Response.ok("Start Capture OK. OUT: " + out).build();
      } else {
        return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).entity("There was a problem starting your capture, please check the logs.").build();
      }
    } catch (Exception e) {
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).entity("Exception while trying to start capture: " + e.getMessage() + ".").build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_PLAIN)
  @Path("startCapture")
  public Response startCapture(@FormParam("config") String config) {
    logger.debug("Capture configuration received: \n{}", config);
    if (service == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE).entity("Capture Agent is unavailable, please wait...").build();
    }

    Properties configuration = new Properties();
    try {
      configuration.load(new StringReader(config));
    } catch (IOException e1) {
      logger.warn("Unable to parse configuration string into valid capture config.  Continuing with default settings.");
    }

    String out;
    try {
      out = service.startCapture(configuration);
      if (out != null)
        return Response.ok("Started capture " + out).build();
      else
        return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).entity("There was a problem starting your capture, please check the logs.").build();
    } catch (Exception e) {
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).entity("Exception while trying to start capture: " + e.getMessage() + ".").build();
    }
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    if (docs == null)
      docs = generateDocs();
    
    return docs;
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("stopCapture")
  public Response stopCapture() {
    if (service == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE).entity("Capture Agent is unavailable, please wait...").build();
    }

    boolean out;
    try {
      out = service.stopCapture(true);
      if (out)
        return Response.ok("Stop Capture OK. OUT: " + out).build();
      else
        return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).entity("There was a problem stopping your capture, please check the logs.").build();
    } catch (Exception e) {
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).entity("Exception while trying to stop capture: " + e.getMessage() + ".").build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_PLAIN)
  @Path("stopCapture")
  public Response stopCapture(@FormParam("recordingID") String recordingID) {
    if (service == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE).entity("Capture Agent is unavailable, please wait...").build();
    }

    boolean out;
    try {
      out = service.stopCapture(recordingID, true);
      if (out)
        return Response.ok("Stopped Capture").build();
      else
        return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).entity("There was a problem stopping your capture, please check the logs.").build();
    } catch (Exception e) {
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).entity("Exception while trying to stop capture: " + e.getMessage() + ".").build();
    }
  }
  
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("configuration")
  public Response getConfiguration() {
    try {
      return Response.ok(service.getDefaultAgentPropertiesAsString()).build();
    } catch (Exception e) {
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).entity("Exception while trying to obtain metadata: " + e.getMessage() + ".").build();
    }
  }

}
