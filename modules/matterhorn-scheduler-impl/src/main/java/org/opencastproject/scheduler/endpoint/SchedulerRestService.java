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
package org.opencastproject.scheduler.endpoint;

import org.opencastproject.rest.RestPublisher;
import org.opencastproject.scheduler.api.Event;
import org.opencastproject.scheduler.api.Metadata;
import org.opencastproject.scheduler.api.RecurringEvent;
import org.opencastproject.scheduler.api.SchedulerFilter;
import org.opencastproject.scheduler.impl.EventImpl;
import org.opencastproject.scheduler.impl.EventListImpl;
import org.opencastproject.scheduler.impl.MetadataImpl;
import org.opencastproject.scheduler.impl.RecurringEventImpl;
import org.opencastproject.scheduler.impl.RecurringEventListImpl;
import org.opencastproject.scheduler.impl.SchedulerServiceImpl;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.Param.Type;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;

import org.json.simple.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * REST Endpoint for Scheduler Service
 */
@Path("/")
public class SchedulerRestService {
  /** The expected date format used when checking for conflicting recordings */
  public static final String DATE_PATTERN = "yyyy:MM:dd:HH:mm:ss";

  private static final Logger logger = LoggerFactory.getLogger(SchedulerRestService.class);
  private SchedulerServiceImpl service;

  protected String serverUrl = UrlSupport.DEFAULT_BASE_URL;

  /**
   * Method to set the service this REST endpoint uses
   * 
   * @param service
   */
  public void setService(SchedulerServiceImpl service) {
    this.service = service;
  }

  /**
   * Method to unset the service this REST endpoint uses
   * 
   * @param service
   */
  public void unsetService(SchedulerServiceImpl service) {
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
      String serviceUrl = (String) cc.getProperties().get(RestPublisher.SERVICE_PATH_PROPERTY);
      docs = generateDocs(serviceUrl);
    }
  }

  /**
   * Get a specific scheduled event.
   * 
   * @param eventID
   *          The unique ID of the event.
   * @return event XML with the data of the event
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("event/{eventID}.xml")
  public Response getSingleEventXml(@PathParam("eventID") String eventId) {
    return getSingleEvent(eventId);
  }

  /**
   * Get a specific scheduled event.
   * 
   * @param eventID
   *          The unique ID of the event.
   * @return event XML with the data of the event
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("event/{eventID}.json")
  public Response getSingleEventJson(@PathParam("eventID") String eventId) {
    return getSingleEvent(eventId);
  }

  private Response getSingleEvent(String eventId) {
    logger.debug("Single event Lookup: {}", eventId);
    if (!eventId.isEmpty()) {
      try {
        Event e = service.getEvent(eventId);
        if (e != null) {
          return Response.ok(e).build();
        } else {
          return Response.status(Status.NOT_FOUND).build();
        }
      } catch (Exception e) {
        logger.warn("Error occured while looking for event '{}': {}", eventId, e);
        return Response.serverError().build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).build();
    }
  }

  /**
   * add a recurring event.
   * 
   * @param event
   *          The recurring event to be added
   * @return HTTP 201 Created, indicating succesful creation.
   */
  @PUT
  @Path("recurring/{recurringEventId}")
  public Response addRecurringEvent(@PathParam("recurringEventId") String recurringEventId,
          @FormParam("recurringEvent") RecurringEventImpl recurringEvent) {
    logger.debug("add Recurrent event: {}", recurringEvent);
    if (!recurringEventId.isEmpty() && recurringEvent != null) {
      try {
        RecurringEvent result = service.addRecurringEvent(recurringEvent);
        if (result != null) {
          return Response.status(Status.CREATED).type("").build(); // remove content-type, no message-body.
        } else {
          return Response.serverError().build();
        }
      } catch (Exception e) {
        logger.warn("could not add recurring event '{}': {}", recurringEvent, e);
        return Response.serverError().build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).build();
    }
  }

  /**
   * update a recurring event.
   * 
   * @param event
   *          The recurring event to be added
   * @return HTTP 204 No Content, indicating succesful update.
   */
  @POST
  @Path("recurring/{recurringEventId}")
  public Response updateRecurringEvent(@PathParam("recurringEventId") String recurringEventId,
          @FormParam("recurringEvent") RecurringEventImpl event) {
    logger.debug("update Recurrent event: {}", event);
    if (!recurringEventId.isEmpty() && event != null) {
      try {
        if (service.updateRecurringEvent(event)) {
          return Response.noContent().type("").build(); // Remove content-type, no message-body.
        } else {
          return Response.serverError().build();
        }
      } catch (Exception e) {
        logger.warn("could not update recurring event {}: {}", event, e);
        return Response.serverError().build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).build();
    }
  }

  /**
   * Get a specific scheduled recurring event.
   * 
   * @param eventID
   *          The unique ID of the event.
   * @return event XML with the data of the recurring event
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("recurring/{recurringEventID}.xml")
  public Response getRecurringEventXml(@PathParam("recurringEventID") String eventId) {
    return getRecurringEvent(eventId);
  }

  /**
   * Get a specific scheduled recurring event.
   * 
   * @param eventID
   *          The unique ID of the event.
   * @return event JSON with the data of the recurring event
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("recurring/{recurringEventID}.json")
  public Response getRecurringEventJson(@PathParam("recurringEventID") String eventId) {
    return getRecurringEvent(eventId);
  }

  private Response getRecurringEvent(String eventId) {
    logger.debug("Recurrent event Lookup: {}", eventId);
    if (!eventId.isEmpty()) {
      try {
        RecurringEvent event = service.getRecurringEvent(eventId);
        if (event != null) {
          return Response.ok(event).build();
        } else {
          return Response.status(Status.NOT_FOUND).build();
        }
      } catch (Exception e) {
        logger.warn("Recurrent event Lookup failed for '{}': ", eventId, e);
        return Response.serverError().build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).build();
    }
  }

  /**
   * Get list of events that belong to the recurring event.
   * 
   * @param eventID
   *          The unique ID of the RecurringEvent.
   * @return List of events XML with the data of the events that belong to the recurring event
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("recurring/{recurringEventID}/events.xml")
  public Response getEventsFromRecurringEventXml(@PathParam("recurringEventID") String eventId) {
    return getEventsFromRecurringEvent(eventId);
  }

  /**
   * Get list of events that belong to the recurring event.
   * 
   * @param eventID
   *          The unique ID of the RecurringEvent.
   * @return List of events XML with the data of the events that belong to the recurring event
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("recurring/{recurringEventID}/events.json")
  public Response getEventsFromRecurringEventJson(@PathParam("recurringEventID") String eventId) {
    return getEventsFromRecurringEvent(eventId);
  }

  private Response getEventsFromRecurringEvent(String eventId) {
    logger.debug("Getting events from recurrent event: {}", eventId);
    if (!eventId.isEmpty()) {
      try {
        RecurringEvent event = service.getRecurringEvent(eventId);
        if (event != null) {
          EventListImpl eventList = new EventListImpl(event.getEvents());
          return Response.ok(eventList).build();
        } else {
          return Response.status(Status.NOT_FOUND).build();
        }
      } catch (Exception e) {
        logger.warn("Getting events from recurrent event failed for '{}': {}", eventId, e);
        return Response.serverError().build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).build();
    }
  }

  /**
   * Delete a recurring event
   * 
   * @param eventID
   *          The unique ID of the RecurringEvent.
   * @return HTTP Status 204 No Content if successfully deleted.
   */
  @DELETE
  @Path("recurring/{recurringEventID}")
  public Response deleteRecurringEvent(@PathParam("recurringEventID") String eventId) {
    logger.debug("delete recurring event: {}", eventId);
    if (!eventId.isEmpty()) {
      try {
        if (service.removeRecurringEvent(eventId)) {
          return Response.noContent().type("").build(); // remove content-type, no message-body.
        } else {
          return Response.status(Status.NOT_FOUND).build();
        }
      } catch (Exception e) {
        logger.warn("removing events from recurrent event failed: {}", eventId);
        return Response.serverError().build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).build();
    }
  }

  /**
   * Gets a XML with the Dublin Core metadata for the specified event.
   * 
   * @param eventID
   *          The unique ID of the event.
   * @return Dublin Core XML for the event
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("event/{eventId}/dublincore.xml")
  public Response getDublinCoreMetadata(@PathParam("eventId") String eventId) {
    if (!eventId.isEmpty()) {
      try {
        String result = service.getDublinCoreMetadata(eventId);
        if (result != null) {
          return Response.ok(result).build();
        } else {
          return Response.status(Status.NOT_FOUND).build();
        }
      } catch (Exception e) {
        logger.warn("Unable to get dublincore Metadata for id '{}': {}", eventId, e);
        return Response.serverError().build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).build();
    }
  }

  /**
   * Gets java Properties file with technical metadata for the specified event.
   * 
   * @param eventID
   *          The unique ID of the event.
   * @return Java Properties File with the metadata for the event
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("event/{eventId}.properties")
  public Response getCaptureAgentMetadata(@PathParam("eventId") String eventId) {
    if (!eventId.isEmpty()) {
      try {
        String result = service.getCaptureAgentMetadata(eventId);
        if (result != null) {
          return Response.ok(result).build();
        } else {
          return Response.status(Status.NOT_FOUND).build();
        }
      } catch (Exception e) {
        logger.warn("Unable to get capture agent metadata for id '{}': {}", eventId, e);
        return Response.serverError().build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).build();
    }
  }

  /**
   * Stores a new event in the database. As a result the event will be returned, but some fields especially the event-id
   * may have been updated. Within the metadata section it is possible to add any additional metadata as a key-value
   * pair. The information will be stored even if the key is yet unknown.
   * 
   * @param e
   *          The SchedulerEvent that should be stored.
   * @return The same event with some updated fields.
   */
  @PUT
  @Produces(MediaType.TEXT_XML)
  @Path("event/{eventId}")
  public Response addEvent(@PathParam("eventId") String eventId, @FormParam("event") EventImpl event) {
    logger.debug("addEvent(e): {}", event);
    if (!eventId.isEmpty() && event != null) {
      try {
        Event result = service.addEvent(event);
        if (result != null) { // TODO: addEvent never returns null. When it's updated to throw EntityExistsException
                              // Handle it though...
          return Response.status(Status.CREATED).type("").build();
        } else {
          logger.error("Event that should be added is null");
          return Response.status(Status.NOT_FOUND).build();
        }
      } catch (Exception e) {
        logger.warn("Unable to create new event with id '{}': {}", eventId, e);
        return Response.serverError().build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).build();
    }
  }

  /**
   * 
   * Removes the specified event from the database. Returns true if the event was found and could be removed.
   * 
   * @param eventID
   *          The unique ID of the event.
   * @return true if the event was found and could be deleted.
   */
  @DELETE
  @Path("event/{eventId}")
  public Response deleteEvent(@PathParam("eventId") String eventId) {
    if (!eventId.isEmpty()) {
      try {
        if (service.removeEvent(eventId)) {
          return Response.noContent().type("").build(); // remove content-type, no message-body.
        } else {
          return Response.status(Status.NOT_FOUND).build();
        }
      } catch (Exception e) {
        logger.warn("Unable to delete event with id '{}': {}", eventId, e);
        return Response.serverError().build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).build();
    }
  }

  /**
   * Updates an existing event in the database. The event-id has to be stored in the database already. Will return true,
   * if the event was found and could be updated.
   * 
   * @param e
   *          The SchedulerEvent that should be updated
   * @return true if the event was found and could be updated.
   */
  @POST
  @Path("event/{eventId}")
  public Response updateEvent(@PathParam("eventId") String eventId, @FormParam("event") EventImpl event) {
    if (!eventId.isEmpty() && event != null) {
      try {
        if (service.updateEvent(event)) {
          return Response.noContent().type("").build(); // remove content-type, no message-body.
        } else {
          return Response.status(Status.NOT_FOUND).build();
        }
      } catch (Exception e) {
        logger.warn("Unable to update event with id '{}': {}", eventId, e);
        return Response.serverError().build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).build();
    }
  }

  /**
   * returns scheduled events, that pass the filter. filter: an xml definition of the filter. Tags that are not included
   * will not be filtered. Possible values for order by are
   * title,creator,series,time-asc,time-desc,contributor,channel,location,device
   * 
   * @param filter
   *          exact id to search for pattern to search for pattern to search for A short description of the content of
   *          the lecture begin of the period of valid events end of the period of valid events pattern to search for ID
   *          of the series which will be filtered ID of the channel that will be filtered pattern to search for pattern
   *          to search for pattern to search for
   *          title|creator|series|time-asc|time-desc|contributor|channel|location|device">
   * @return List of SchedulerEvents as XML
   */
  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("events.xml")
  public Response eventsXml(@QueryParam("devicePattern") String device, @QueryParam("title") String title,
          @QueryParam("creator") String creator, @QueryParam("order") String order,
          @QueryParam("start") String startStr, @QueryParam("stop") String stopStr) {
    return eventsJson(device, title, creator, order, startStr, stopStr);
  }

  /**
   * returns scheduled events, that pass the filter. filter: an xml definition of the filter. Tags that are not included
   * will not be filtered. Possible values for order by are
   * title,creator,series,time-asc,time-desc,contributor,channel,location,device
   * 
   * @param filter
   *          exact id to search for pattern to search for pattern to search for A short description of the content of
   *          the lecture begin of the period of valid events end of the period of valid events pattern to search for ID
   *          of the series which will be filtered ID of the channel that will be filtered pattern to search for pattern
   *          to search for pattern to search for
   *          title|creator|series|time-asc|time-desc|contributor|channel|location|device">
   * @return List of SchedulerEvents as JSON
   */
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("events.json")
  public Response eventsJson(@QueryParam("devicePattern") String device, @QueryParam("title") String title,
          @QueryParam("creator") String creator, @QueryParam("order") String order,
          @QueryParam("start") String startStr, @QueryParam("stop") String stopStr) {
    SchedulerFilter filter = new SchedulerFilter().withCreatorFilter(creator).withDeviceFilter(device)
            .withTitleFilter(title).withOrder(order).withStart(parseDate(startStr)).withStop(parseDate(stopStr));
    return getEvents(filter);
  }

  /**
   * Looks for events that are conflicting with the given event, because they use the same recorder at the same time.
   * 
   * @param e
   *          The event that should be checked for conflicts
   * @return An XML with the list of conflicting events
   */
  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("event/conflict.xml")
  public Response getConflictingEventsXml(@QueryParam("device") String device, @QueryParam("start") String startStr,
          @QueryParam("stop") String stopStr) {
    Date start = parseDate(startStr);
    Date stop = parseDate(stopStr);
    return getConflictingEvents(device, start, stop);
  }

  /**
   * Parses a string into a date using the DATE_PATTERN pattern. Returns null if the string can not be parsed.
   * 
   * @param dateStr
   *          the string representing the date
   * @return the date, or null
   */
  private Date parseDate(String dateStr) {
    SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
    try {
      return sdf.parse(dateStr);
    } catch (ParseException e) {
      logger.debug("Unable to parse '{}' as a date using pattern '{}'", dateStr, DATE_PATTERN);
    }
    return null;
  }

  /**
   * Looks for events that are conflicting with the given event, because they use the same recorder at the same time.
   * 
   * @param e
   *          The event that should be checked for conflicts
   * @return An JSON with the list of conflicting events
   */
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("event/conflict.json")
  public Response getConflictingEventsJson(@QueryParam("device") String device, @QueryParam("start") String startStr,
          @QueryParam("stop") String stopStr) {
    return getConflictingEventsXml(device, startStr, stopStr);
  }

  private Response getConflictingEvents(String deviceId, Date start, Date stop) {
    if (start == null || stop == null) {
      return Response.status(Status.BAD_REQUEST).entity("Start and stop dates must be specified").build();
    }
    List<Event> events = service.findConflictingEvents(deviceId, start, stop);
    if (events.isEmpty()) {
      return Response.noContent().build();
    } else {
      EventListImpl eventList = new EventListImpl(events);
      return Response.ok(eventList).build();
    }
  }

  /**
   * Lists all Recurring events in the database
   * 
   * @return XML with all events
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("recurring.xml")
  public Response getAllRecurringEventsXml() {
    return getAllRecurringEvents();
  }

  /**
   * Lists all Recurring events in the database
   * 
   * @return JSON with all events
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("recurring.json")
  public Response getAllRecurringEventsJson() {
    return getAllRecurringEvents();
  }

  private Response getAllRecurringEvents() {
    try {
      RecurringEventListImpl eventList = new RecurringEventListImpl(service.getAllRecurringEvents());
      return Response.ok(eventList).build();
    } catch (Exception e) {
      logger.error("Unable to return all recurring events: {}", e);
      return Response.serverError().build();
    }
  }

  /**
   * Lists all future events in the database, without any filter
   * 
   * @return XML with all events
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("upcoming.xml")
  public Response getUpcomingEventsXml() {
    return getEvents(new SchedulerFilter().withStart(new Date()));
  }

  /**
   * Lists all future events in the database, without any filter
   * 
   * @return JSON with all events
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("upcoming.json")
  public Response getUpcomingEventsJson() {
    return getEvents(new SchedulerFilter().withStart(new Date()));
  }

  /**
   * Returns all events matching the supplied filter
   * 
   * @param filter
   *          the filter
   * @return the events
   */
  private Response getEvents(SchedulerFilter filter) {
    try {
      EventListImpl eventList = new EventListImpl(service.getEvents(filter));
      return Response.ok(eventList).build();
    } catch (Exception e) {
      logger.error("Unable to return upcoming events: {}", e);
      return Response.serverError().build();
    }
  }

  /**
   * Gets the iCalendar with all (even old) events for the specified capture agent id.
   * 
   * @param captureAgentID
   *          The ID that specifies the capture agent.
   * @return an iCalendar
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("{captureAgentID}/calendar")
  public Response getCalendarForCaptureAgent(@PathParam("captureAgentID") String captureAgentId) {
    if (!captureAgentId.isEmpty()) {
      try {
        String result = service.getCalendarForCaptureAgent(captureAgentId);
        if (!result.isEmpty()) {
          return Response.ok(result).build();
        } else {
          return Response.status(Status.NOT_FOUND).build();
        }
      } catch (Exception e) {
        logger.error("Unable to get calendar for capture agent '{}': {}", captureAgentId, e);
        return Response.serverError().build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).build();
    }
  }

  @SuppressWarnings("unchecked")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("uuid")
  public Response getUniqueId() {
    try {
      String id = UUID.randomUUID().toString();
      JSONObject j = new JSONObject();
      j.put("id", id);
      return Response.ok(j.toString()).build();
    } catch (Exception e) {
      logger.warn("could not create new seriesID");
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }
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
  protected String generateDocs(String serviceUrl) {
    DocRestData data = new DocRestData("Scheduler", "Scheduler Service", serviceUrl, notes);

    // abstract
    data.setAbstract("This service creates, edits and retrieves and helps manage scheduled capture events.");

    // Scheduler addEvent
    RestEndpoint addEventEndpoint = new RestEndpoint("addEvent", RestEndpoint.Method.PUT, "/event/{eventId}",
            "Stores a new event in the database.");
    addEventEndpoint.addStatus(org.opencastproject.util.doc.Status.CREATED("Event was successfully created."));
    addEventEndpoint.addPathParam(new Param("eventId", Type.STRING, "", "A UUID for the new event."));
    addEventEndpoint
            .addRequiredParam(new Param("event", Type.TEXT, generateEvent(), "The Event that should be stored."));
    addEventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, addEventEndpoint);

    // Scheduler addEvent
    RestEndpoint addREventEndpoint = new RestEndpoint(
            "addREvent",
            RestEndpoint.Method.PUT,
            "/recurring/{rEventId}",
            "Stores a new recurring event in the database. All member events as specified by the recurrence rule will be created as well.");
    addREventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .CREATED("Recurrence and member events successfully created."));
    addREventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .BAD_REQUEST("Supplied rEventId or RecurringEvent is invalid or missing."));
    addREventEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("A server error occured."));
    addREventEndpoint.addPathParam(new Param("rEventId", Type.STRING, "", "A UUID for the new recurring event."));
    addREventEndpoint.addRequiredParam(new Param("recurringEvent", Type.TEXT, generateRecurringEvent(),
            "The Recurring Event that should be stored."));
    addREventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, addREventEndpoint);

    // Scheduler updateEvent
    RestEndpoint updateEventEndpoint = new RestEndpoint("updateEvent", RestEndpoint.Method.POST, "/event/{eventId}",
            "Updates an existing event in the database. The event-id has to be stored in the database already.");
    updateEventEndpoint.addStatus(org.opencastproject.util.doc.Status.NO_CONTENT("Event successfully updated."));
    updateEventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .BAD_REQUEST("Supplied eventId or Event are incorrect or missing."));
    updateEventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .NOT_FOUND("An event matching the supplied eventId was not found."));
    updateEventEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("A server error occured."));
    updateEventEndpoint.addPathParam(new Param("eventId", Type.STRING, "", "The UUID of the event to update."));
    updateEventEndpoint.addRequiredParam(new Param("event", Type.TEXT, generateEvent(),
            "The Event that should be updated."));
    updateEventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, updateEventEndpoint);

    // Scheduler updateRecurringEvent
    RestEndpoint updateREventEndpoint = new RestEndpoint("updateREvent", RestEndpoint.Method.POST,
            "/recurring/{rEventId}",
            "Updates an existing recurrence event in the database. The recurringEventId has to be stored in the database already.");
    updateREventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .NO_CONTENT("Recurring event successfully updated."));
    updateREventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .BAD_REQUEST("Supplied rEventId or RecurringEvent are incorrect or missing."));
    updateREventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .NOT_FOUND("A RecurringEvent matching the supplied rEventId was not found."));
    updateREventEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("A server error occured."));
    updateREventEndpoint.addPathParam(new Param("rEventId", Type.STRING, "",
            "The UUID of the recurring event to update."));
    updateREventEndpoint.addRequiredParam(new Param("recurringEvent", Type.TEXT, generateRecurringEvent(),
            "The RecurringEvent that should be updated."));
    updateREventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, updateREventEndpoint);

    // Scheduler removeEvent
    RestEndpoint removeEventEndpoint = new RestEndpoint("removeEvent", RestEndpoint.Method.DELETE, "/event/{eventId}",
            "Removes the specified event from the database.");
    removeEventEndpoint.addStatus(org.opencastproject.util.doc.Status.NO_CONTENT("Event successfully deleted."));
    removeEventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .BAD_REQUEST("Supplied eventId is incorrect or missing."));
    removeEventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .NOT_FOUND("An Event matching the supplied eventId was not found."));
    removeEventEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("A server error occured."));
    removeEventEndpoint.addPathParam(new Param("eventId", Type.STRING, "EventId", "The UUID of the event."));
    removeEventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, removeEventEndpoint);

    // Scheduler removeRecurringEvent
    RestEndpoint removeREventEndpoint = new RestEndpoint("removeREvent", RestEndpoint.Method.DELETE,
            "/recurring/{recurringEventID}", "Removes the specified recurringEvent from the database.");
    removeREventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .NO_CONTENT("RecurringEvent successfully deleted."));
    removeREventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .BAD_REQUEST("Supplied rEventId is incorrect or missing."));
    removeREventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .NOT_FOUND("An RecurringEvent matching the supplied rEventId was not found."));
    removeREventEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("A server error occured."));
    removeREventEndpoint.addPathParam(new Param("recurringEventID", Type.STRING, "RecurrentEventID",
            "The UUID of the event."));
    removeREventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, removeREventEndpoint);

    // Scheduler getEvent
    RestEndpoint getEventEndpoint = new RestEndpoint("getEvent", RestEndpoint.Method.GET, "/event/{eventId}",
            "Get a specific scheduled event.");
    getEventEndpoint.addFormat(Format.xml("XML representation of the event."));
    getEventEndpoint.addFormat(Format.json("JSON representation of the event."));
    getEventEndpoint.setAutoPathFormat(true);
    getEventEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("XML or JSON representation of the Event."));
    getEventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .BAD_REQUEST("Supplied eventId is incorrect or missing."));
    getEventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .NOT_FOUND("An Event matching the supplied eventId was not found."));
    getEventEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("A server error occured."));
    getEventEndpoint.addPathParam(new Param("eventId", Type.STRING, "c0e3d8a7-7ecc-479b-aee7-8da369e445f2",
            "The UUID of the event."));
    getEventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getEventEndpoint);

    // Scheduler get recurring event
    RestEndpoint getREventEndpoint = new RestEndpoint("getREvent", RestEndpoint.Method.GET, "/recurring/{rEventId}",
            "Get a specific scheduled recurrent event.");
    getREventEndpoint.addFormat(Format.xml("XML representation of the recurring event."));
    getREventEndpoint.addFormat(Format.json("JSON representation of the recurring event."));
    getREventEndpoint.setAutoPathFormat(true);
    getREventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("XML or JSON representation of the RecurringEvent."));
    getREventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .BAD_REQUEST("Supplied rEventId is incorrect or missing."));
    getREventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .NOT_FOUND("A RecurringEvent matching the supplied rEventId was not found."));
    getREventEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("A server error occured."));
    getREventEndpoint.addPathParam(new Param("rEventId", Type.STRING, "Recurring Event ID",
            "The unique ID of the recurring event."));
    getREventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getREventEndpoint);

    // Scheduler getEventsFromRecurringEvent
    RestEndpoint getEventsFromRecurringEventEndpoint = new RestEndpoint("getEventsFromRecurringEvent",
            RestEndpoint.Method.GET, "/recurring/{rEventId}/events",
            "returns all events that belong to a recurring event.");
    getEventsFromRecurringEventEndpoint.addFormat(Format
            .xml("XML representation of a list of the events belonging to a recurring event."));
    getEventsFromRecurringEventEndpoint.addFormat(Format
            .json("JSON representation of a list of the event belonging to a recurring event."));
    getEventsFromRecurringEventEndpoint.setAutoPathFormat(true);
    getEventsFromRecurringEventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("XML or JSON representation of a list of events belonging to a recurring event."));
    getEventsFromRecurringEventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .BAD_REQUEST("Supplied rEventId is incorrect or missing."));
    getEventsFromRecurringEventEndpoint.addStatus(org.opencastproject.util.doc.Status
            .NOT_FOUND("A RecurringEvent matching the supplied rEventId was not found."));
    getEventsFromRecurringEventEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("A server error occured."));
    getEventsFromRecurringEventEndpoint.addPathParam(new Param("rEventId", Type.STRING, "Recurring Event ID",
            "The unique ID of the recurring event."));
    getEventsFromRecurringEventEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getEventsFromRecurringEventEndpoint);

    // Scheduler filterEventsEndpoint
    RestEndpoint filterEventsEndpoint = new RestEndpoint(
            "filterEvents",
            RestEndpoint.Method.POST,
            "/filter/events",
            "returns scheduled events, that pass the filter.\nfilter: an xml definition of the filter. Tags that are not included will noct be filtered. Possible values for order by are title,creator,series,time-asc,time-desc,contributor,channel,location,device");
    filterEventsEndpoint.addFormat(Format
            .xml("XML representation of a list of the events conforming to the supplied filter."));
    filterEventsEndpoint.addFormat(Format
            .json("JSON representation of a list of the event conforming to the supplied filter."));
    filterEventsEndpoint.setAutoPathFormat(true);
    filterEventsEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("XML or JSON representation of a list of events belonging to a recurring event."));
    filterEventsEndpoint.addStatus(org.opencastproject.util.doc.Status
            .BAD_REQUEST("Supplied filter is incorrect or missing."));
    filterEventsEndpoint.addOptionalParam(new Param("filter", Type.TEXT, generateSchedulerFilter(),
            "The SchedulerFilter that should be applied."));
    filterEventsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, filterEventsEndpoint);

    // Scheduler getEvents
    RestEndpoint getAllEventsEndpoint = new RestEndpoint("allEvents", RestEndpoint.Method.GET, "/all/events",
            "returns all scheduled events");
    getAllEventsEndpoint.addFormat(Format.xml("XML representation of a list of all events."));
    getAllEventsEndpoint.addFormat(Format.json("JSON representation of a list of all events."));
    getAllEventsEndpoint.setAutoPathFormat(true);
    getAllEventsEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("XML or JSON representation of a list of all events."));
    getAllEventsEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("A server error occured."));
    getAllEventsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getAllEventsEndpoint);

    // Scheduler getAllRecurringEvents
    RestEndpoint getAllREventsEndpoint = new RestEndpoint("allRecurringEvents", RestEndpoint.Method.GET,
            "/all/recurring", "returns all scheduled events");
    getAllREventsEndpoint.addFormat(Format.xml("XML representation of a list of all events."));
    getAllREventsEndpoint.addFormat(Format.json("JSON representation of a list of all events."));
    getAllREventsEndpoint.setAutoPathFormat(true);
    getAllREventsEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("XML or JSON representation of a list of all recurring events."));
    getAllREventsEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("A server error occured."));
    getAllREventsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getAllREventsEndpoint);

    // Scheduler getUpcomingEvents
    RestEndpoint getUpcomingEventsEndpoint = new RestEndpoint("upcomingEvents", RestEndpoint.Method.GET, "/upcoming",
            "returns all upcoming events. Returns true if the event was found and could be removed.");
    getUpcomingEventsEndpoint.addFormat(Format.xml("XML representation of a list of all events."));
    getUpcomingEventsEndpoint.addFormat(Format.json("JSON representation of a list of all events."));
    getUpcomingEventsEndpoint.setAutoPathFormat(true);
    getUpcomingEventsEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("XML or JSON representation of a list of all upcoming events."));
    getUpcomingEventsEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("A server error occured."));
    getUpcomingEventsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getUpcomingEventsEndpoint);

    // Scheduler findConflictingEvents
    RestEndpoint findConflictingEventsEndpoint = new RestEndpoint("findConflictingEvents", RestEndpoint.Method.POST,
            "/event/conflict",
            "Looks for events that are conflicting with the given event, because they use the same recorder at the same time ");
    findConflictingEventsEndpoint.addFormat(Format
            .xml("XML representation of a list of all events that conflict with supplied event."));
    findConflictingEventsEndpoint.addFormat(Format
            .json("JSON representation of a list of all events that conflict with supplied event."));
    findConflictingEventsEndpoint.setAutoPathFormat(true);
    findConflictingEventsEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("XML or JSON representation of a list of all upcoming events."));
    findConflictingEventsEndpoint.addStatus(org.opencastproject.util.doc.Status
            .BAD_REQUEST("Supplied Event is invalid or missing."));
    findConflictingEventsEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("A server error occured."));
    findConflictingEventsEndpoint.addRequiredParam(new Param("event", Type.TEXT, generateEvent(),
            "The Event that should be checked for conflicts."));
    findConflictingEventsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, findConflictingEventsEndpoint);

    // Scheduler findConflictingREvents
    RestEndpoint findConflictingREventsEndpoint = new RestEndpoint("findConflictingRecurringEvents",
            RestEndpoint.Method.POST, "/recurring/conflict",
            "Looks for events that are conflicting with the given event, because they use the same recorder at the same time ");
    findConflictingREventsEndpoint.addFormat(Format
            .xml("XML representation of a list of all events that conflict with supplied recurring event."));
    findConflictingREventsEndpoint.addFormat(Format
            .json("JSON representation of a list of all events that conflict with supplied recurring event."));
    findConflictingREventsEndpoint.setAutoPathFormat(true);
    findConflictingREventsEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("XML or JSON representation of a list of all upcoming events."));
    findConflictingREventsEndpoint.addStatus(org.opencastproject.util.doc.Status
            .BAD_REQUEST("Supplied RecurringEvent is invalid or missing."));
    findConflictingREventsEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("A server error occured."));
    findConflictingREventsEndpoint.addRequiredParam(new Param("recurringEvent", Type.TEXT, generateEvent(),
            "The RecurringEvent that should be checked for conflicts. "));
    findConflictingREventsEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, findConflictingREventsEndpoint);

    // Scheduler getDublinCoreMetadata
    RestEndpoint getDublinCoreMetadataEndpoint = new RestEndpoint("getDublinCoreMetadata", RestEndpoint.Method.GET,
            "/event/{eventId}/dublincore", "Gets a XML with the Dublin Core metadata for the specified event. ");
    getDublinCoreMetadataEndpoint.addFormat(Format.xml("Dublincore metadata for the supplied eventId."));
    getDublinCoreMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("Dublinecore XML document containing the event's metadata."));
    getDublinCoreMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status
            .BAD_REQUEST("Supplied eventId is invalid or missing."));
    getDublinCoreMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status
            .NOT_FOUND("No Event matching the supplied eventId was found."));
    getDublinCoreMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("A server error occured."));
    getDublinCoreMetadataEndpoint.addPathParam(new Param("eventId", Type.STRING,
            "c0e3d8a7-7ecc-479b-aee7-8da369e445f2", "The unique ID of the event."));
    getDublinCoreMetadataEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getDublinCoreMetadataEndpoint);

    // Scheduler getCaptureAgentMetadata
    RestEndpoint getCaptureAgentMetadataEndpoint = new RestEndpoint("getCaptureAgentMetadata", RestEndpoint.Method.GET,
            "/event/{eventId}/captureAgentMetadata",
            "Gets java Properties file with technical metadata for the specified event. ");
    getCaptureAgentMetadataEndpoint.addFormat(new Format("properties",
            "Java Properties files that is needed by the capture agent.", null));
    getCaptureAgentMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("Java Properties file for the event."));
    getCaptureAgentMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status
            .BAD_REQUEST("Supplied eventId is invalid or missing."));
    getCaptureAgentMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status
            .NOT_FOUND("No Event matching the supplied eventId was found."));
    getCaptureAgentMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("A server error occured."));
    getCaptureAgentMetadataEndpoint.addPathParam(new Param("eventId", Type.STRING,
            "c0e3d8a7-7ecc-479b-aee7-8da369e445f2", "The unique ID of the event."));
    getCaptureAgentMetadataEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getCaptureAgentMetadataEndpoint);

    // Scheduler getCalendarForCaptureAgent
    RestEndpoint getCalendarForCaptureAgentEndpoint = new RestEndpoint("getCalendarForCaptureAgent",
            RestEndpoint.Method.GET, "/{captureAgentId}/calendar",
            "Gets the iCalendar with all upcoming events for the specified capture agent id. ");
    getCalendarForCaptureAgentEndpoint.addFormat(new Format("ics", "iCalendar", "http://tools.ietf.org/html/rfc2445"));
    getCalendarForCaptureAgentEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("iCalendar file containing the scheduled Events for the capture agent."));
    getCaptureAgentMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status
            .BAD_REQUEST("Supplied captureAgentId is invalid or missing."));
    getCaptureAgentMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status
            .NOT_FOUND("No capture agent matching the supplied catureAgentId was found."));
    getCaptureAgentMetadataEndpoint.addStatus(org.opencastproject.util.doc.Status.ERROR("A server error occured."));
    getCalendarForCaptureAgentEndpoint.addPathParam(new Param("captureAgentId", Type.STRING, "recorder",
            "The ID that specifies the capture agent."));
    getCalendarForCaptureAgentEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, getCalendarForCaptureAgentEndpoint);

    return DocUtil.generate(data);
  }

  /**
   * Creates an example XML of an Event for the documentation.
   * 
   * @return A XML with a Event
   */
  private String generateEvent() {
    Event e = new EventImpl();
    e.setEventId(UUID.randomUUID().toString());
    e.setStartDate(new Date());
    e.setStopDate(new Date(e.getStartDate().getTime() + 1000*60*60)); // one hour
    LinkedList<Metadata> metadata = new LinkedList<Metadata>();
    metadata.add(new MetadataImpl("title", "demo title"));
    metadata.add(new MetadataImpl("location", "demo location"));
    metadata.add(new MetadataImpl("abstract", "demo abstract"));
    metadata.add(new MetadataImpl("creator", "demo creator"));
    metadata.add(new MetadataImpl("contributor", "demo contributor"));
    metadata.add(new MetadataImpl("time.start", "1317499200000"));
    metadata.add(new MetadataImpl("time.end", "1317507300000"));
    metadata.add(new MetadataImpl("device", "demo"));
    metadata.add(new MetadataImpl("resources", "vga, audio"));
    metadata.add(new MetadataImpl("series-id", "demo series"));
    e.setMetadata(metadata);

    SchedulerBuilder builder = SchedulerBuilder.getInstance();
    try {
      String result = builder.marshallEvent((EventImpl) e);
      logger.info("Event: " + result);
      return result;
    } catch (Exception e1) {
      logger.warn("Could not marshall example event: {}", e1.getMessage());
      return null;
    }
  }

  /**
   * Creates an example XML of an RecurringEvent for the documentation.
   * 
   * @return A XML with a RecurringEvent
   */
  private String generateRecurringEvent() {
    RecurringEvent e = new RecurringEventImpl();
    e.setRecurringEventId("demo-recurring-event");
    e.setRecurrenceStart(new Date());
    e.setRecurrenceStop(new Date(e.getRecurrenceStart().getTime() + 1000*60*60*24*30)); // approximately one month
    LinkedList<Metadata> metadata = new LinkedList<Metadata>();
    metadata.add(new MetadataImpl("title", "demo title"));
    metadata.add(new MetadataImpl("location", "demo location"));
    metadata.add(new MetadataImpl("abstract", "demo abstract"));
    metadata.add(new MetadataImpl("creator", "demo creator"));
    metadata.add(new MetadataImpl("contributor", "demo contributor"));
    metadata.add(new MetadataImpl("recurrence.start", "1317499200000"));
    metadata.add(new MetadataImpl("recurrence.end", "1329350400000"));
    metadata.add(new MetadataImpl("recurrence.duration", "3600000"));
    metadata.add(new MetadataImpl("device", "demo"));
    metadata.add(new MetadataImpl("resources", "vga, audio"));
    metadata.add(new MetadataImpl("series-id", "demo series"));
    e.setMetadata(metadata);

    e.setRecurrence("FREQ=WEEKLY;BYDAY=TU;BYHOUR=9;BYMINUTE=15");

    SchedulerBuilder builder = SchedulerBuilder.getInstance();
    try {

      String result = builder.marshallRecurringEvent((RecurringEventImpl) e);
      logger.info("Event: " + result);
      return result;
    } catch (Exception e1) {
      logger.warn("Could not marshall example event: {}", e1.getMessage());
      return null;
    }
  }

  /**
   * Creates an example XML of an SchedulerFilter for the documentation.
   * 
   * @return A XML with a SchedulerFilter
   */
  private String generateSchedulerFilter() {
    return "<ns2:SchedulerFilter xmlns:ns2=\"http://scheduler.opencastproject.org/\">\n"
            + " <event-id>exact id to search for</event-id>\n" + " <device>pattern to search for</device>\n"
            + " <title>pattern to search for</title>\n" + " <creator>pattern to search for</creator>\n"
            + " <abstract>A short description of the content of the lecture</abstract>\n"
            + " <startdate>begin of the period of valid events</startdate>\n"
            + " <enddate>end of the period of valid events</enddate>\n"
            + " <contributor>pattern to search for</contributor>\n"
            + " <series-id>ID of the series which will be filtered</series-id>\n"
            + " <channel-id>ID of the channel that will be filtered</channel-id>\n"
            + " <location>pattern to search for</location>\n" + " <attendee>pattern to search for</attendee>\n"
            + " <resource>pattern to search for</resource>\n"
            + " <order-by>title|creator|series|time-asc|time-desc|contributor|channel|location|device</order-by>\n"
            + "</ns2:SchedulerFilter>";
  }

}
