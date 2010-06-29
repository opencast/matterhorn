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
package org.opencastproject.adminui.endpoint;

/* Documentation generation imports
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Status; */

/*JaxRS REST endpoint imports */
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class VersionRestService {
  private static final Logger logger = LoggerFactory.getLogger(VersionRestService.class);
  public VersionRestService() {}
  
  /* GET current - get the current version
   * @returns
   */
  @GET
  @Path("current")
  @SuppressWarnings("unchecked")
  public Response getCurrentVersion() {
    String curr = "";
    StringWriter writer = new StringWriter();
    InputStream stream = getClass().getClassLoader().getResourceAsStream("/ui/version.txt");
    try {
      IOUtils.copy(stream, writer);
    } catch (Exception e) { 
      logger.debug(e.toString());
      e.printStackTrace();
      return Response.status(500).build();
    }
    curr = writer.toString();
    JSONObject json = new JSONObject();
    json.put("version", curr);
    return Response.ok(json.toJSONString()).header("Content-Type", MediaType.APPLICATION_JSON).build();
  }
  
  @GET
  @Path("newest")
  @SuppressWarnings("unchecked")
  public Response getNewestVersion() {
    JSONObject json = new JSONObject();
    try {
      URL url = new URL("http://opencast.jira.com/svn/MH/trunk/");
      URLConnection conn = url.openConnection();
      String etag = conn.getHeaderField("etag");
      String[] parts = etag.split("\"");
      parts = parts[1].split("/");
      json.put("version", parts[0]);
    } catch (IOException e) {
      logger.debug(e.toString());
      json.put("version", "N/A");
    }
    return Response.ok(json.toJSONString()).header("Content-Type", MediaType.APPLICATION_JSON).build();
  }
}
