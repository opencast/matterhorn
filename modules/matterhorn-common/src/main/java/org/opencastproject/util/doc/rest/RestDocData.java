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
package org.opencastproject.util.doc.rest;

import org.opencastproject.util.doc.DocData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * This is the document model class which holds the data about a set of rest endpoints.
 */
public class RestDocData extends DocData {

  /**
   * Regular expression used to count the number of path parameters in a path.
   */
  public static final String PATH_PARAM_COUNTING_REGEX = "\\{(.+?)\\}";

  /**
   * Regular expression used to validate a path.
   */
  public static final String PATH_VALIDATION_REGEX = "^[\\w\\/{}\\.]+$";

  /**
   * A slash character.
   */
  public static final String SLASH = "/";

  /**
   * List of RestEndpointHolderData which each stores a group of endpoints. Currently there are 2 groups, READ group and
   * WRITE group.
   */
  protected List<RestEndpointHolderData> holders;

  /**
   * Create the base data object for creating REST documentation.
   * 
   * @param name
   *          the name of the set of rest endpoints (must be alphanumeric (includes _) and no spaces or special chars)
   * @param title
   *          [OPTIONAL] the title of the documentation
   * @param url
   *          this is the absolute base URL for this endpoint, do not include the trailing slash (e.g. /workflow)
   * @param notes
   *          [OPTIONAL] an array of notes to add into the end of the documentation
   * @throws IllegalArgumentException
   *           if the url is null or empty
   */
  public RestDocData(String name, String title, String url, String[] notes) throws IllegalArgumentException {
    super(name, title, notes);
    if (url == null || "".equals(url)) {
      throw new IllegalArgumentException("URL cannot be blank.");
    }
    meta.put("url", url);
    // create the endpoint holders
    holders = new Vector<RestEndpointHolderData>(2);
    holders.add(new RestEndpointHolderData(RestEndpointData.Type.READ.toString().toUpperCase(), "Read"));
    holders.add(new RestEndpointHolderData(RestEndpointData.Type.WRITE.toString().toUpperCase(), "Write"));
  }

  /**
   * Verify the integrity of this object. If its data is verified to be okay, it return a map representation of this
   * RestDocData object.
   * 
   * @return a map representation of this RestDocData object if this object passes the verification
   * 
   * @throws IllegalStateException
   *           if any path parameter is not present in the endpoint's path
   */
  @Override
  public Map<String, Object> toMap() throws IllegalStateException {
    LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
    m.put("meta", meta);
    m.put("notes", notes);
    // only pass through the holders with things in them
    ArrayList<RestEndpointHolderData> holdersList = new ArrayList<RestEndpointHolderData>();
    for (RestEndpointHolderData holder : holders) {
      if (!holder.getEndpoints().isEmpty()) {
        for (RestEndpointData endpoint : holder.getEndpoints()) {
          // Validate the endpoint path matches the specified path parameters.
          // First, it makes sure that every path parameter is present in the endpoint's path.
          if (!endpoint.getPathParams().isEmpty()) {
            for (RestParamData param : endpoint.getPathParams()) {
              if (!endpoint.getPath().contains("{" + param.getName() + "}")) {
                throw new IllegalStateException("Path (" + endpoint.getPath() + ") does not match path parameter ("
                        + param.getName() + ") for endpoint (" + endpoint.getName()
                        + "), the path must contain all path parameter names.");
              }
            }
          }
          // Then, it makes sure that the number of path parameter patterns in the path is the same as the number of
          // path parameters in the endpoint.
          // The following part uses a regular expression to find patterns like {something}.
          Pattern pattern = Pattern.compile(PATH_PARAM_COUNTING_REGEX);
          Matcher matcher = pattern.matcher(endpoint.getPath());

          int count = 0;
          while (matcher.find()) {
            count++;
          }
          if (count != endpoint.getPathParams().size()) {
            throw new IllegalStateException("Path (" + endpoint.getPath() + ") does not match path parameters ("
                    + endpoint.getPathParams() + ") for endpoint (" + endpoint.getName()
                    + "), the path must contain the same number of path parameters (" + count
                    + ") as the pathParams list (" + endpoint.getPathParams().size() + ").");
          }
        }
        holdersList.add(holder);
      }
    }
    m.put("endpointHolders", holdersList);
    return m;
  }

  /**
   * Gets the path to the default template (a .xhtml file).
   * 
   * @return the path to the default template file
   */
  @Override
  public String getDefaultTemplatePath() {
    return TEMPLATE_DEFAULT;
  }

  /**
   * Returns a string representation of this object.
   * 
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return "DOC:meta=" + meta + ", notes=" + notes + ", " + holders;
  }

  /**
   * Add an endpoint to this documentation using and assign it to the correct type group (read/write).
   * 
   * @param type
   *          the type of this endpoint (RestEndpointData.Type.READ or RestEndpointData.Type.WRITE)
   * @param endpoint
   *          the endpoint to be added
   * @throws IllegalStateException
   *           if the endpoint cannot be assigned to a group
   */
  private void addEndpoint(RestEndpointData.Type type, RestEndpointData endpoint) throws IllegalStateException {
    RestEndpointHolderData currentHolder = null;
    for (RestEndpointHolderData holder : holders) {
      if (type.name().equals(holder.getName())) {
        currentHolder = holder;
        break;
      }
    }
    if (currentHolder == null) {
      throw new IllegalStateException("Could not find holder of type: " + type.name() + ".");
    }
    currentHolder.addEndPoint(endpoint);
  }

  /**
   * Creates an abstract section which is displayed at the top of the documentation page.
   * 
   * @param abstractText
   *          any text to place at the top of the document, can be html markup but must be valid
   */
  public void setAbstract(String abstractText) {
    if (isBlank(abstractText)) {
      meta.remove("abstract");
    } else {
      meta.put("abstract", abstractText);
    }
  }

  /**
   * Validates paths: VALID: /sample , /sample/{thing} , /{my}/{path}.xml , /my/fancy_path/is/{awesome}.{FORMAT}
   * INVALID: sample, /sample/, /sa#$%mple/path
   * 
   * @param path
   *          the path value to check
   * @return true if this path is valid, false otherwise
   */
  public static boolean isValidPath(String path) {
    boolean valid = true;
    if (isBlank(path)) {
      valid = false;
    } else {
      if (SLASH.equals(path)) {
        valid = true;
      } else if (path.endsWith(SLASH) || !path.startsWith(SLASH)) {
        valid = false;
      } else {
        valid = path.matches(PATH_VALIDATION_REGEX);
      }
    }
    return valid;
  }

  /**
   * Add an endpoint to the Rest documentation.
   * 
   * @param restQuery
   *          the RestQuery annotation type storing information of an endpoint
   * @param produces
   *          the return type(s) of this endpoint, values should be constants from <a
   *          href="http://jackson.codehaus.org/javadoc/jax-rs/1.0/javax/ws/rs/core/MediaType.html"
   *          >javax.ws.rs.core.MediaType</a> or ExtendedMediaType
   *          (org.opencastproject.util.doc.rest.ExtendedMediaType).
   * @param httpMethodString
   *          the HTTP method of this endpoint (e.g. GET, POST)
   * @param path
   *          the path of this endpoint
   */
  public void addEndpoint(RestQuery restQuery, Produces produces, String httpMethodString, Path path) {
    RestEndpointData endpoint = new RestEndpointData(restQuery.name(), httpMethodString, "/" + path.value(),
            restQuery.description());
    // Add return description if needed
    if (!restQuery.returnDescription().isEmpty()) {
      endpoint.addNote("Return value description: " + restQuery.returnDescription());
    }

    // Add formats
    if (produces != null) {
      for (String format : produces.value()) {
        endpoint.addFormat(new RestFormatData(format));
      }
    }

    // Add responses
    for (RestResponse restResp : restQuery.reponses()) {
      endpoint.addStatus(restResp);
    }

    // Add body parameter
    if (restQuery.bodyParameter().type() != RestParameter.Type.NO_PARAMETER) {
      endpoint.addBodyParam(restQuery.bodyParameter());
    }

    // Add path parameter
    for (RestParameter restParam : restQuery.pathParameters()) {
      endpoint.addPathParam(new RestParamData(restParam));
    }

    // Add query parameter (required and optional)
    for (RestParameter restParam : restQuery.queryParameters()) {
      if (restParam.isRequired()) {
        endpoint.addRequiredParam(new RestParamData(restParam));
      } else {
        endpoint.addOptionalParam(new RestParamData(restParam));
      }
    }

    // Set the test form after all parameters are added.
    endpoint.setTestForm(new RestFormData(endpoint));

    // Add the endpoint to the corresponding group based on its HTTP method
    if ((httpMethodString.equalsIgnoreCase("GET")) || (httpMethodString.equalsIgnoreCase("HEAD"))) {
      addEndpoint(RestEndpointData.Type.READ, endpoint);
    } else if ((httpMethodString.equalsIgnoreCase("DELETE")) || (httpMethodString.equalsIgnoreCase("POST"))
            || (httpMethodString.equalsIgnoreCase("PUT"))) {
      addEndpoint(RestEndpointData.Type.WRITE, endpoint);
    }
  }

}
