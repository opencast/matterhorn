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
package org.opencastproject.integrationtest;

import javax.ws.rs.core.MultivaluedMap;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * Files REST resources
 * @author jamiehodge
 *
 */

public class FilesResources {
  public static Client c = Client.create();
  public static WebResource r = c.resource(IntegrationTests.BASE_URL + "/files/");
  
  static {
	  c.addFilter(new HTTPBasicAuthFilter(IntegrationTests.USER, IntegrationTests.PASS));
  }
  
  public static ClientResponse getFile(String mediaPackageID, String mediaPackageElementID) throws UniformInterfaceException {
    return r.path(mediaPackageID + '/' + mediaPackageElementID).get(ClientResponse.class);
  }
  
  public static ClientResponse getFile(String mediaPackageID, 
      String mediaPackageElementID, String fileName) throws UniformInterfaceException {
    return r.path(mediaPackageID + '/' + mediaPackageElementID + '/' + fileName).get(ClientResponse.class);
  }
  
  // FIXME
  public static ClientResponse postFile(String mediaPackageID,
      String mediaPackageElementID, String media) throws UniformInterfaceException {
    MultivaluedMap<String, String> params = new MultivaluedMapImpl();
    params.add("mediaPackageID", mediaPackageID);
    params.add("mediaPackageElementID", mediaPackageElementID);
    return r.entity(media).post(ClientResponse.class, params);
  }
  
  public static ClientResponse deleteFile(String mediaPackageID,
      String mediaPackageElementID) throws UniformInterfaceException {
    MultivaluedMap<String, String> params = new MultivaluedMapImpl();
    params.add("mediaPackageID", mediaPackageID);
    params.add("mediaPackageElementID", mediaPackageElementID);
    return r.delete(ClientResponse.class, params);
  }
}
