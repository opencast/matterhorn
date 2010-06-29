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
package org.opencastproject.series.endpoint;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.series.api.Series;
import org.opencastproject.series.api.SeriesMetadata;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.series.impl.SeriesImpl;
import org.opencastproject.series.impl.SeriesMetadataImpl;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Param.Type;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * REST Endpoint for Scheduler Service
 */
@Path("/")
public class SeriesRestService {
  private static final Logger logger = LoggerFactory.getLogger(SeriesRestService.class);
  private SeriesService service;

  protected String serverUrl = UrlSupport.DEFAULT_BASE_URL;

  /**
   * Method to set the service this REST endpoint uses
   * 
   * @param service
   */
  public void setService(SeriesService service) {
    this.service = service;
  }

  /**
   * Method to unset the service this REST endpoint uses
   * 
   * @param service
   */
  public void unsetService(SeriesService service) {
    this.service = null;
  }

  /**
   * The method that will be called, if the service will be activated
   * 
   * @param cc
   *          The ComponentContext of this service
   */
  public void activate(ComponentContext cc) {
    // Get the configured server URL
    if (cc == null) {
      serverUrl = UrlSupport.DEFAULT_BASE_URL;
    } else {
      String ccServerUrl = cc.getBundleContext().getProperty("org.opencastproject.server.url");
      logger.info("configured server url is {}", ccServerUrl);
      if (ccServerUrl == null) {
        serverUrl = UrlSupport.DEFAULT_BASE_URL;
      } else {
        serverUrl = ccServerUrl;
      }
    }
  }

  /**
   * Get a specific series.
   * 
   * @param seriesID
   *          The unique ID of the series.
   * @return series XML with the data of the series
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("series/{seriesID}")
  public Response getSeries(@PathParam("seriesID") String seriesID) {
    logger.debug("Series Lookup: {}", seriesID);
    try {
      Series s = service.getSeries(seriesID);
      if (s == null)
        return Response.status(Status.BAD_REQUEST).build();
      return Response.ok(new SeriesJaxbImpl(s)).build();
    } catch (Exception e) {
      logger.warn("Series Lookup failed: {}", seriesID);
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }
  }

  @SuppressWarnings("unchecked")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("search")
  public Response searchSeries(@FormParam("term") String pattern) {
    logger.debug("Searching all Series that match the pattern {}", pattern);
    try {
      List<Series> list = service.searchSeries(pattern);
      if (list == null)
        return Response.status(Status.BAD_REQUEST).build();
      JSONArray a = new JSONArray();
      for (Series s : list) {
        JSONObject j = new JSONObject();
        j.put("id", s.getSeriesId());
        j.put("label", s.getDescription());
        j.put("value", s.getDescription());
        a.add(j);
      }
      // TODO convert result to JSON
      return Response.ok(a.toJSONString()).build();
    } catch (Exception e) {
      logger.warn("search for series failed. Pattern: {}", pattern);
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("series/{seriesID}/dublincore")
  public Response getDublinCoreForSeries(@PathParam("seriesID") String seriesID) {
    logger.debug("Series Lookup: {}", seriesID);
    try {
      DublinCore dc = service.getDublinCore(seriesID);
      if (dc == null)
        return Response.status(Status.BAD_REQUEST).build();
      return Response.ok(dc).build();
    } catch (Exception e) {
      logger.warn("Series Lookup failed: {}", seriesID);
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }
  }

  @SuppressWarnings("unchecked")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("new/id")
  public Response newSeriesId() {
    logger.debug("create ne Series Id");
    try {
      String id = service.newSeriesID();
      if (id == null)
        return Response.status(Status.SERVICE_UNAVAILABLE).build();
      JSONObject j = new JSONObject();
      j.put("id", id);
      return Response.ok(j.toString()).build();
    } catch (Exception e) {
      logger.warn("could not create new seriesID");
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }
  }

  /**
   * Stores a new series in the database.
   * 
   * @param s
   *          The Series that should be stored.
   * @return json object, success: true and uuid if succeeded
   */
  @SuppressWarnings("unchecked")
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Path("series")
  public Response addSeries(@FormParam("series") SeriesJaxbImpl s) {
    logger.debug("addseries: {}", s);

    if (s == null) {
      logger.error("series that should be added is null");
      return Response.status(Status.BAD_REQUEST).build();
    }
    Series series = s.getSeries();
    if (series == null) {
      logger.error("series that should be added is null");
      return Response.status(Status.BAD_REQUEST).build();
    }
    boolean result = service.addSeries(series);
    logger.info("Adding series {} ", series.getSeriesId());
    JSONObject j = new JSONObject();
    j.put("success", result);
    j.put("id", series.getSeriesId());
    return Response.ok(j.toString()).build();
  }

  /**
   * 
   * Removes the specified series from the database. Returns true if the series was found and could be removed.
   * 
   * @param seriesID
   *          The unique ID of the series.
   * @return true if the series was found and was deleted.
   */
  @DELETE
  @Produces(MediaType.TEXT_PLAIN)
  @Path("series/{seriesID}")
  public Response deleteSeries(@PathParam("seriesID") String seriesID) {
    return Response.ok(service.removeSeries(seriesID)).build();
  }

  /**
   * Updates an existing series in the database. The series has to be stored in the database already. Will return true,
   * if the event was found and could be updated.
   * 
   * @param series
   *          The series that should be updated
   * @return true if the event was found and could be updated.
   */
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  @Path("series")
  public Response updateSeries(@FormParam("series") SeriesJaxbImpl series) {

    if (series == null) {
      logger.error("series that should be updated is null");
      return Response.status(Status.BAD_REQUEST).build();
    }
    Series s = series.getSeries();
    if (s == null) {
      logger.error("series that should be updated is null");
      return Response.status(Status.BAD_REQUEST).build();
    }
    if (service.updateSeries(s)) {
      return Response.ok(true).build();
    } else {
      return Response.serverError().build();
    }
  }

  /**
   * returns all series
   * 
   * @return List of SchedulerEvents as XML
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("all")
  public Response getAllSeries() {
    logger.debug("getting all series.");
    List<Series> series = service.getAllSeries();
    if (series == null)
      return Response.status(Status.BAD_REQUEST).build();
    LinkedList<SeriesJaxbImpl> list = new LinkedList<SeriesJaxbImpl>();
    for (Series s : series) {
      list.add(new SeriesJaxbImpl(s));
    }
    SeriesListJaxbImpl container = new SeriesListJaxbImpl(list);
    return Response.ok((new GenericEntity<SeriesListJaxbImpl>(container) {
    })).build();
  }

  /**
   * returns the REST documentation
   * 
   * @return the REST documentation, if available
   */
  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    if (docs == null) {
      docs = generateDocs();
    }
    return docs;
  }

  protected String docs;
  private String[] notes = {
          "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
          "If the service is down or not working it will return a status 503, this means the the underlying service is not working and is either restarting or has failed",
          "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In other words, there is a bug! You should file an error report with your server logs from the time when the error occurred: <a href=\"https://issues.opencastproject.org\">Opencast Issue Tracker</a>", };

  /**
   * Generates the REST documentation
   * 
   * @return The HTML with the documentation
   */
  protected String generateDocs() {
    DocRestData data = new DocRestData("Series", "Series Service", "/series/rest", notes);

    // abstract
    data.setAbstract("This service creates, edits and retrieves and helps manage sereies that capture metadata.");

    // add Series
    RestEndpoint addEndpoint = new RestEndpoint("addSeries", RestEndpoint.Method.PUT, "/series",
            "Stores a new series in the database. Returns true if the series was stored");
    addEndpoint.addFormat(new Format("boolean", null, null));
    addEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, boolean returned"));
    addEndpoint.addRequiredParam(new Param("series", Type.TEXT, generateSeries(), "The series that should be stored."));
    addEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, addEndpoint);

    // update Series
    RestEndpoint updateEndpoint = new RestEndpoint(
            "updateSeries",
            RestEndpoint.Method.POST,
            "/series",
            "Updates an existing series in the database. The series-id has to be stored in the database already. Will return true, if the event was found and could be updated.");
    updateEndpoint.addFormat(new Format("boolean", null, null));
    updateEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, boolean returned"));
    updateEndpoint.addRequiredParam(new Param("series", Type.TEXT, generateSeries(),
            "The series that should be updated."));
    updateEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, updateEndpoint);

    // remove Series
    RestEndpoint removeEndpoint = new RestEndpoint("deleteSeries", RestEndpoint.Method.DELETE, "/series/{seriesID}",
            "Removes the specified series from the database. Returns true if the series could be removed.");
    removeEndpoint.addFormat(new Format("boolean", null, null));
    removeEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, boolean returned"));
    removeEndpoint.addPathParam(new Param("seriesID", Type.STRING, "seriesID", "The unique ID of the series."));
    removeEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, removeEndpoint);

    // get Series
    RestEndpoint getEndpoint = new RestEndpoint("getSeries", RestEndpoint.Method.GET, "/series/{seriesID}",
            "Get a specific Series.");
    getEndpoint.addFormat(new Format("xml", null, null));
    getEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, Series XML returned"));
    getEndpoint.addPathParam(new Param("seriesID", Type.STRING, "UUID of the Series", "The unique ID of the Series."));
    getEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getEndpoint);

    // get all series
    RestEndpoint getAllEndpoint = new RestEndpoint("getAllSeries", RestEndpoint.Method.GET, "/all",
            "returns all series");
    getAllEndpoint.addFormat(new Format("xml", null, null));
    getAllEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("OK, valid request, List of Series as XML returned"));
    getAllEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getAllEndpoint);

    // get new seriesID
    RestEndpoint newIdEndpoint = new RestEndpoint("newSeriesId", RestEndpoint.Method.GET, "/new/id",
            "returns a new UUID for a new series");
    newIdEndpoint.addFormat(new Format("text", null, null));
    newIdEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, UUID for a new seriesID"));
    newIdEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, newIdEndpoint);

    // get Dublin Core for Series
    RestEndpoint dcEndpoint = new RestEndpoint("getDublinCoreForSeries", RestEndpoint.Method.GET,
            "/series/{seriesID}/dublincore", "Get the DublinCore metdata for a specific Series.");
    dcEndpoint.addFormat(new Format("xml", null, null));
    dcEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("OK, valid request, DublinCore returned"));
    dcEndpoint.addPathParam(new Param("seriesID", Type.STRING, "UUID of the Series", "The unique ID of the Series."));
    dcEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, dcEndpoint);

    // search Series
    RestEndpoint searchEndpoint = new RestEndpoint("getSeries", RestEndpoint.Method.GET, "/search/{pattern}",
            "Get all Series that match this pattern in their Metadata.");
    searchEndpoint.addFormat(new Format("JSON", null, null));
    searchEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("OK, valid request, JSON Object with UUID of the series and a String describing the series"));
    searchEndpoint.addPathParam(new Param("pattern", Type.STRING, "lecturer", "a part of a metadat value"));
    searchEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, searchEndpoint);

    return DocUtil.generate(data);
  }

  protected String generateSeries() {
    try {
      SeriesBuilder builder = SeriesBuilder.getInstance();

      Series series = new SeriesImpl();

      LinkedList<SeriesMetadata> metadata = new LinkedList<SeriesMetadata>();

      metadata.add(new SeriesMetadataImpl(series, "title", "demo title"));
      metadata.add(new SeriesMetadataImpl(series, "license", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "valid", "" + System.currentTimeMillis()));
      metadata.add(new SeriesMetadataImpl(series, "publisher", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "creator", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "subject", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "temporal", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "audience", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "spatial", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "rightsHolder", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "extent", "3600000"));
      metadata.add(new SeriesMetadataImpl(series, "created", "" + System.currentTimeMillis()));
      metadata.add(new SeriesMetadataImpl(series, "language", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "identifier", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "isReplacedBy", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "type", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "available", "" + System.currentTimeMillis()));
      metadata.add(new SeriesMetadataImpl(series, "modified", "" + System.currentTimeMillis()));
      metadata.add(new SeriesMetadataImpl(series, "replaces", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "contributor", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "description", "demo"));
      metadata.add(new SeriesMetadataImpl(series, "issued", "" + System.currentTimeMillis()));

      series.generateSeriesId();

      series.setMetadata(metadata);

      String result = builder.marshallSeries(series);
      logger.info("Series: " + result);
      return result;
    } catch (Exception e1) {
      logger.warn("Could not marshall example series: {}", e1.getMessage());
      return null;
    }
  }

}
