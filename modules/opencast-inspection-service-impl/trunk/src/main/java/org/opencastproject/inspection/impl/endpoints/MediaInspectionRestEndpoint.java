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
package org.opencastproject.inspection.impl.endpoints;

import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.media.mediapackage.Track;

import java.net.URL;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * A service endpoint to expose the {@link MediaInspectionService} via REST.
 */
@Path("/")
public class MediaInspectionRestEndpoint {
  protected MediaInspectionService service;
  public void setService(MediaInspectionService service) {
    this.service = service;
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  public Track getTrack(@QueryParam("url") URL url) {
    return service.inspect(url);
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    return "Analyze media files by specifying a URL.  For example:" + 
      "<a href=\"./?url=http://source.opencastproject.org/svn/modules/opencast-media/trunk/src/test/resources/aonly.mov\">" +
      "analyze a sample movie file</a> at http://source.opencastproject.org/svn/modules/opencast-media/trunk/src/test/resources/aonly.mov";
  }
}
