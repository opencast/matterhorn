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

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

/**
 * Captions REST resources
 * @author jamiehodge
 *
 */

public class CaptionsResources {
  public static Client c = Client.create();
  public static WebResource r = c.resource(IntegrationTests.BASE_URL + "/captions/rest/");
  
  static {
	  c.addFilter(new HTTPBasicAuthFilter(IntegrationTests.USER, IntegrationTests.PASS));
  }
  
  public static ClientResponse search() throws UniformInterfaceException {
    return r.path("search").get(ClientResponse.class);
  }
  
  public static ClientResponse episode(String id) throws UniformInterfaceException {
    return r.path(id).get(ClientResponse.class);
  }
  
  public static ClientResponse add(String id, String doc, String format) throws UniformInterfaceException {
    return r.path(id + '/' + format).post(ClientResponse.class, doc);
  }
}
