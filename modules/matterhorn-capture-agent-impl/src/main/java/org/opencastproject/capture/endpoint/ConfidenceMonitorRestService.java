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

import org.opencastproject.capture.api.ConfidenceMonitor;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Param.Type;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;


@Path("/")
public class ConfidenceMonitorRestService {
  
  private static final Logger logger = LoggerFactory.getLogger(ConfidenceMonitorRestService.class);
  
  private ConfidenceMonitor service;
  
  protected String docs;
  
  protected String generateDocs() {
    DocRestData data = new DocRestData("ConfidenceMonitor", "Confidence Monitor", "/confidence/rest", null);
    
    // grabFrame Endpoint
    RestEndpoint grabFrameEndpoint = new RestEndpoint("grabFrame", RestEndpoint.Method.GET, "/{name}", "Loads a JPEG image from the device specified");
    grabFrameEndpoint.addFormat(new Format("jpeg", "The image of the device", null));
    grabFrameEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, results returned"));
    grabFrameEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("Couldn't grab a frame from specified device"));
    Param device = new Param("name", Type.STRING, null, "The device to grab a frame from");
    grabFrameEndpoint.addPathParam(device);
    grabFrameEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, grabFrameEndpoint);
    
    // list devices endpoint
    RestEndpoint getDevices = new RestEndpoint("getDevices", RestEndpoint.Method.GET, "/devices", "Lists devices accessible on capture agent");
    getDevices.addFormat(new Format("XML", "Devices that support confidence monitoring", null));
    getDevices.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, results returned"));
    getDevices.addStatus(org.opencastproject.util.doc.Status.ERROR("Couldn't list devices"));
    getDevices.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getDevices);
    
    // audio rms endpoint
    RestEndpoint getRMSValues = new RestEndpoint("getRMSValues", RestEndpoint.Method.GET, "/audio/{name}/{timestamp}", "Retrieve all RMS data for device {name} after Unix time {timestamp}");
    getRMSValues.addStatus(org.opencastproject.util.doc.Status.OK(("NONE")));
    getRMSValues.addStatus(org.opencastproject.util.doc.Status.ERROR("Couldn't grab RMS values"));
    getRMSValues.addFormat(new Format("JSON", "start:Unix time to start getting values, interval: time between samples (ns), samples:list of RMS values", null));
    Param audioDevice = new Param("name", Type.STRING, null, "The device to get RMS values from");
    Param timestamp = new Param("timestamp", Type.STRING, null, "The timestamp to start getting RMS values from");
    getRMSValues.addPathParam(audioDevice);
    getRMSValues.addPathParam(timestamp);
    getRMSValues.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getRMSValues);
    
    return DocUtil.generate(data);
  }
  
  public void activate() {
    logger.info("Video Monitoring Service Activated");
  }
  
  public void setService(ConfidenceMonitor service) {
    this.service = service;
  }
  
  public void unsetService(ConfidenceMonitor service) {
    this.service = null;
  }
  
  @GET
  @Produces("image/jpeg")
  @Path("{name}")
  public Response grabFrame(@PathParam("name") String device) {
    if (service == null) {
      //TODO:  What happens here if we return a string?  Is this going to break things in the confidence monitor UI?
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE).entity("Confidence monitor unavailable, please wait...").build();
    }

    CacheControl cc = new CacheControl();
    cc.setNoCache(true);
    return Response.ok(service.grabFrame(device)).cacheControl(cc).build();
  }
  
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("devices")
  public List<AgentDevice> getDevices() {
    //TODO:  Should we be storing this list of device names as a list of AgentDevice objects instead?
    LinkedList<AgentDevice> devices = new LinkedList<AgentDevice>();
    if (service != null) {
      List<String> names = service.getFriendlyNames();
      for (String name : names) {
        String nameType[] = name.split(",");
        devices.add(new AgentDevice(nameType[0], nameType[1]));
      }
    } else {
      logger.warn("Service is null in getDevices()");
    }
    return devices;
  }
  
  /**
   * Returns the RMS values for device after a given Unix timestamp. 
   * @param device Friendly name of the audio device
   * @param timestamp A Unix timestamp (set to 0 to get all values stored)
   * @return application/json with keys start, interval and samples
   */
  @SuppressWarnings("unchecked")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("audio/{name}/{timestamp}")
  public Response getRMSValues(@PathParam("name") String device, @PathParam("timestamp") double timestamp) {
    JSONObject jsonOutput = new JSONObject();
    if (service == null) {
      return Response.serverError().build();
    }
    // Attempt to grab audio information, if exception is thrown the device does not exist
    try {
      List<Double> rmsValues = service.getRMSValues(device, timestamp);
      for (int i = 0; i < rmsValues.size(); i++) {
        double value = rmsValues.get(i);
        value = Math.round(value * 100.00) / 100.00;
        rmsValues.set(i, value);
      }
      jsonOutput.put("start", timestamp);
      jsonOutput.put("interval", "100");
      jsonOutput.put("samples", rmsValues);
      return Response.ok(jsonOutput.toJSONString()).header("Content-Type", MediaType.APPLICATION_JSON).build();
    } catch (NullPointerException e) {
      return Response.ok("Device " + device + " does not exist.").build();
    }
  }
  
  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    if (docs == null) { docs = generateDocs(); }
    return docs;
  }
  
}
