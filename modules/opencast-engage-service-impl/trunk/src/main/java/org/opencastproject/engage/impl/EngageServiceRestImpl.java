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
package org.opencastproject.engage.impl;

import org.opencastproject.engage.api.EngageService;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * FIXME -- Add javadocs
 */
@Path("/")
public class EngageServiceRestImpl implements EngageService {
  private static final Logger logger = LoggerFactory.getLogger(EngageServiceImpl.class);
  private EngageService service;
  public void setService(EngageService service) {
    this.service = service;
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return docs;
  }

  protected final String docs;
  
  public EngageServiceRestImpl() {
    String docsFromClassloader = null;
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/html/index.html");
      docsFromClassloader = IOUtils.toString(in);
    } catch (IOException e) {
      logger.error("failed to read documentation", e);
      docsFromClassloader = "unable to load documentation for " + EngageServiceRestImpl.class.getName();
    } finally {
      IOUtils.closeQuietly(in);
    }
    docs = docsFromClassloader;
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("play/{filename}")
  public String deliverPlayerGET(@PathParam("filename") String filename, @Context HttpServletRequest request) {
    String mediaHost = "mediahost";
    
      try {
        InetAddress addr = InetAddress.getLocalHost();
        mediaHost = addr.getHostAddress();
      } catch (UnknownHostException e) {
        e.printStackTrace();
      }
    // FIXME find out the hostname of the machine the service is running on
    return deliverPlayer(filename, "vm081.rz.uos.de:8080");
  }
  
  public String deliverPlayer(String filename, String mediaHost) {
    return service.deliverPlayer(filename, mediaHost);
  }
  
  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("available")
  public String listRecordings() {
    return service.listRecordings();
  }
  
}
