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
package org.opencastproject.scheduler.impl.jpa;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.spi.PersistenceProvider;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.ValidationException;

import org.opencastproject.scheduler.api.SchedulerEvent;
import org.opencastproject.scheduler.api.SchedulerFilter;
import org.opencastproject.scheduler.impl.CalendarGenerator;
import org.opencastproject.scheduler.impl.SchedulerEventImpl;
import org.opencastproject.scheduler.impl.SchedulerServiceImpl;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of the Scheduler service based on JPA. This version knows about series too.
 *
 */
public class SchedulerServiceImplJPA extends SchedulerServiceImpl {
  
  private static final Logger logger = LoggerFactory.getLogger(SchedulerServiceImplJPA.class);

  protected PersistenceProvider persistenceProvider;
  protected Map<String, Object> persistenceProperties;
  protected EntityManagerFactory emf = null;
  
  private long updated = System.currentTimeMillis();
  private long updatedCalendar = 0;
  private long updatedAllEvents = 0;
  
  private Hashtable<String, String> calendars;
  private Event [] cachedEvents;
  
  public Map<String, Object> getPersistenceProperties() {
    return persistenceProperties;
  }

  public void setPersistenceProperties(Map<String, Object> persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }
  
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }
  
  public PersistenceProvider getPersistenceProvider() {
    return persistenceProvider;
  }

  protected Event makeIdUnique (Event e) {
    EntityManager em = emf.createEntityManager();
    if (e.getEventId() == null) e.generateId();
    try {
      Event found = em.find(Event.class, e.getEventId());
      while (found != null) {
        e.generateId();
        found = em.find(Event.class, e.getEventId());
      }
    } finally {
      em.close();
    }
    return e;
  }
  
  protected RecurringEvent makeIdUnique (RecurringEvent e) {
    EntityManager em = emf.createEntityManager();
    if (e.getRecurringEventId() == null) e.generateId();
    try {
      RecurringEvent found = em.find(RecurringEvent.class, e.getRecurringEventId());
      while (found != null) {
        e.generateId();
        found = em.find(RecurringEvent.class, e.getRecurringEventId());
      }
    } finally {
      em.close();
    }
    return e;
  } 
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.impl.SchedulerServiceImpl#addEvent(org.opencastproject.scheduler.api.SchedulerEvent)
   */
  @Override
  public SchedulerEvent addEvent(SchedulerEvent e) {
    return addEvent(((SchedulerEventImpl) e).toEvent()).toSchedulerEvent();
  }

  public Event addEvent(Event e) {
    EntityManager em = emf.createEntityManager();
    e = makeIdUnique(e);
    e.setEntityManagerFactory(emf);
    try {
      EntityTransaction tx = em.getTransaction();
      tx.begin();
      em.persist(e);
      tx.commit();
    } finally {
      em.close();
      updated = System.currentTimeMillis();
    }
    
    return e;
  }
  
  public RecurringEvent addRecurringEvent(RecurringEvent e) {
    EntityManager em = emf.createEntityManager();
    e = makeIdUnique(e);
    e.setEntityManagerFactory(emf);
    e.generatedEvents();
    try {
      EntityTransaction tx = em.getTransaction();
      tx.begin();
      em.persist(e);
      tx.commit();
    } finally {
      em.close();
      updated = System.currentTimeMillis();
    }
    
    return e;
  }  
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.impl.SchedulerServiceImpl#getEvent(java.lang.String)
   */
  @Override
  public SchedulerEvent getEvent(String eventID) {
    Event e = getEventJPA(eventID);
    if (e == null) return null;
    return e.toSchedulerEvent();
  }
  
  public Event getEventJPA(String eventID) {
    return Event.find(eventID, emf);
  }  

  public RecurringEvent getRecurringEvent(String recurringEventID) {
    logger.debug("loading recurring event with the ID {}", recurringEventID);
    EntityManager em = emf.createEntityManager();
    RecurringEvent e = null;
    try {
       e = em.find(RecurringEvent.class, recurringEventID);
    } finally {
      em.close();
    }
    e.setEntityManagerFactory(emf);
    return e;
  }  
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.impl.SchedulerServiceImpl#getEvents(org.opencastproject.scheduler.api.SchedulerFilter)
   */
  @Override
  public SchedulerEvent[] getEvents(SchedulerFilter filter) { 
    SchedulerEvent[] results = null;
    Event [] events = getEventsJPA(filter);
    results = new SchedulerEvent [events.length];
    for (int i = 0; i < events.length; i++) results[i] = events[i].toSchedulerEvent();
    return results;
  }
  
  public Event [] getEventsJPA (SchedulerFilter filter) {
    if (updatedCalendar < updated) calendars = new Hashtable<String, String>(); // reset all calendars, if data has been changed 
    if (filter == null) {
      logger.debug("returning all events");
      return getAllEvents();
    }
    List<Event> events = new LinkedList<Event>();
    // catch the case that the event id is given, what may be unrealistic
    if (filter.getEventIDFilter() != null && filter.getEventIDFilter().length() > 0) {
      Event e = getEventJPA(filter.getEventIDFilter());
      if (e != null) {
        logger.debug("using only single event with id {}.", filter.getEventIDFilter());
        events.add(e);
      } 
    } else {
        // all other cases
        events = new LinkedList<Event>(Arrays.asList(getAllEvents()));
        logger.debug("using all {} events.", events.size());
    }
    
    // filter for device
    if (filter.getDeviceFilter() != null && filter.getDeviceFilter().length() > 0) {
      events = filterEventsForExactValue(events, "device", filter.getDeviceFilter());
      logger.debug("filtered for device. {} events left.", events.size());
    }
    
    // filter for title
    if (filter.getTitleFilter() != null && filter.getTitleFilter().length() > 0) {
      events = filterEvents(events, "title", filter.getTitleFilter());
      logger.debug("filtered for Title. {} events left.", events.size());
    }
    
    // filter for creator
    if (filter.getCreatorFilter() != null && filter.getCreatorFilter().length() > 0) {
      events = filterEvents(events, "creator", filter.getCreatorFilter());
      logger.debug("filtered for creator. {} events left.", events.size());
    }
    
    // filter for abstract
    if (filter.getAbstractFilter() != null && filter.getAbstractFilter().length() > 0) {
      events = filterEvents(events, "abstract", filter.getAbstractFilter());
      logger.debug("filtered for abstract. {} events left.", events.size());
    }
    
    // filter for contributor
    if (filter.getContributorFilter() != null && filter.getContributorFilter().length() > 0) {
      events = filterEvents(events, "contributor", filter.getContributorFilter());
      logger.debug("filtered for contributor. {} events left.", events.size());
    }
    
    // filter for location
    if (filter.getLocationFilter() != null && filter.getLocationFilter().length() > 0) {
      events = filterEventsForExactValue(events, "location", filter.getLocationFilter());
      logger.debug("filtered for location. {} events left.", events.size());
    }
    
    // filter for series
    if (filter.getSeriesIDFilter() != null && filter.getSeriesIDFilter().length() > 0) {
      events = filterEventsForExactValue(events, "series-id", filter.getSeriesIDFilter());
      logger.debug("filtered for series. {} events left.", events.size());
    }
    
    // filter for channel
    if (filter.getChannelIDFilter() != null && filter.getChannelIDFilter().length() > 0) {
      events = filterEvents(events, "channel-id", filter.getChannelIDFilter());
      logger.debug("filtered for channel. {} events left.", events.size());
    }
    
    // filter for resources
    if (filter.getResourceFilter() != null && filter.getResourceFilter().length() > 0) {
      events = filterEvents(events, "resources", filter.getResourceFilter());
      logger.debug("filtered for resources. {} events left.", events.size());
    }
    
    // filter for attendees
    if (filter.getAttendeeFilter() != null && filter.getAttendeeFilter().length() > 0) {
      events = filterEvents(events, "attendes", filter.getAttendeeFilter());
      logger.debug("filtered for attendees. {} events left.", events.size());
    }
    
    // filter for later Dates
    if (filter.getStart() != null && filter.getStart().getTime() > 0) {
      events = filterEventsForAfterDate(events, filter.getStart());
      logger.debug("Setting start date. {} events left.", events.size());
    }
    
    // filter for later Dates
    if (filter.getEnd() != null && filter.getEnd().getTime() > 0) {
      events = filterEventsForBeforeDate(events, filter.getEnd());
      logger.debug("Setting end date. {} events left.", events.size());
    }
    
    return events.toArray(new Event[0]);
  }  
  
  private List<Event> filterEvents (List<Event> list, String key, String value) {
    LinkedList<Event> marked = new LinkedList<Event>(); //needed because loop will not terminate correctly, if list is modified
    for (Event e: list) {
      if (! e.containsKey(key)) marked.add(e);
      else if (! e.getValue(key).contains(value)) marked.add(e);
    }
    for (Event e: marked) list.remove(e);
    
    return list;    
  }
  
  private List<Event> filterEventsForExactValue (List<Event> list, String key, String value) {
    LinkedList<Event> marked = new LinkedList<Event>(); //needed because loop will not terminate correctly, if list is modified
    for (Event e: list) {
      if (! e.containsKey(key)) marked.add(e);
      else if (! e.getValue(key).equals(value)) marked.add(e);
    }
    for (Event e: marked) list.remove(e);
    
    return list;
  }
  
  private List<Event> filterEventsForBeforeDate (List<Event> list, Date time) {
    if (time == null) return list;
    LinkedList<Event> marked = new LinkedList<Event>(); //needed because loop will not terminate correctly, if list is modified
    for (Event e: list) {
      if (e.getStartdate() == null || (! e.getStartdate().before(time))) marked.add(e);
    }
    for (Event e: marked) list.remove(e);
    
    return list;
  }  
  
  public List<RecurringEvent> filterRecurringEventsForBeforeDate (List<RecurringEvent> list, String key, Date time) {
    LinkedList<RecurringEvent> marked = new LinkedList<RecurringEvent>(); //needed because loop will not terminate correctly, if list is modified
    for (RecurringEvent e: list) {
      if (! e.containsKey(key)) marked.add(e);
      else if (! e.getValueAsDate(key).before(time)) marked.add(e);
    }
    for (RecurringEvent e: marked) list.remove(e);
    
    return list;
  }  
  
  private List<Event> filterEventsForAfterDate (List<Event> list, Date time) {
    if (time == null) return list;
    LinkedList<Event> marked = new LinkedList<Event>(); //needed because loop will not terminate correctly, if list is modified
    for (Event e: list) {
      if (e.getEnddate() == null || (! e.getEnddate().after(time))) marked.add(e);    
    }
    for (Event e: marked) list.remove(e);
    
    return list;    
  }  
  
  @SuppressWarnings("unchecked")
  public Event [] getAllEvents () {
    if (updatedAllEvents > updated && cachedEvents != null) {
      return cachedEvents;
    }
    EntityManager em = emf.createEntityManager();
    Query query = em.createNamedQuery("Event.getAll");
    List<Event> events = null;
    try {
      events = (List<Event>) query.getResultList();
    } finally {
      em.close();
    }
    for (Event e : events) e.setEntityManagerFactory(emf);
    cachedEvents = events.toArray(new Event[0]);
    updatedAllEvents = System.currentTimeMillis();
    return cachedEvents;
  }
  
  public RecurringEvent [] getAllRecurringEvents () {
    EntityManager em = emf.createEntityManager();
    Query query = em.createNamedQuery("RecurringEvent.getAll");
    List<RecurringEvent> events = null;
    try {
      events = (List<RecurringEvent>) query.getResultList();
    } finally {
      em.close();
    }
    for (RecurringEvent e : events) e.setEntityManagerFactory(emf);
    return events.toArray(new RecurringEvent[0]);
  }  
    
  public List<Event> getUpcoming (List<Event> list) {
    Date now = new Date(System.currentTimeMillis());
    for (Event e : list) {
      Date enddate = e.getEnddate();
      if (!(enddate == null) && ! enddate.after(now)) list.remove(e);
    }
    return list;
  }

  public SchedulerEvent[] getCapturingEvents() {
    LinkedList<Event> recording = new LinkedList<Event>();
    Date now = new Date(System.currentTimeMillis());
    for (Event e : getAllEvents()) {
      try {
        if (e.getEnddate().after(now) && e.getStartdate().before(now)) recording.add(e);
      } catch (NullPointerException e1) {
        logger.warn("Event has no start- or end-date: {}", e);
      }
    }
    SchedulerEvent [] result = new SchedulerEvent [recording.size()];
    for (int i = 0; i < recording.size(); i++) 
      result[i] = recording.get(i).toSchedulerEvent();
    return result;
  }
  
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.impl.SchedulerServiceImpl#removeEvent(java.lang.String)
   */
  @Override
  public boolean removeEvent(String eventID) {
    logger.debug("Removing event with the ID {}", eventID);
    Event event;
    EntityManager em = emf.createEntityManager();
    try {
      em.getTransaction().begin();
      event = em.find(Event.class, eventID);
      if (event == null) return false; // Event not in database
      String rEventID = event.getRecurringEventId();
      if (rEventID != null) { // remove Event from recuring Event list, if necessary
        RecurringEvent rEvent = em.find(RecurringEvent.class, rEventID);
        rEvent.removeEvent(event);
        em.merge(rEvent);
      }
      em.remove(event);
      em.getTransaction().commit();
    } finally {
      em.close();
      updated = System.currentTimeMillis();
    }
    return true; 
  }
  
  public boolean removeRecurringEvent(String rEventID) {
    logger.debug("Removing recurring event with the ID {}", rEventID);
    RecurringEvent event;
    EntityManager em = emf.createEntityManager();
    try {
      em.getTransaction().begin();
      event = em.find(RecurringEvent.class, rEventID);
      if (event == null) return false; // Event not in database
      em.remove(event);
      em.getTransaction().commit();
    } finally {
      em.close();
      updated = System.currentTimeMillis();
    }
    return true; 
  }
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.impl.SchedulerServiceImpl#updateEvent(org.opencastproject.scheduler.api.SchedulerEvent)
   */
  @Override
  public boolean updateEvent(SchedulerEvent e) {
    return updateEvent(((SchedulerEventImpl)e).toEvent());
  }
  
  public boolean updateEvent(Event e) {
    
    EntityManager em = emf.createEntityManager();
    try {
      em.getTransaction().begin();
      Event storedEvent =  getEventJPA(e.getEventId());
      logger.debug("Found stored event. {} -", storedEvent);
      if (storedEvent == null) return false; //nothing found to update
      storedEvent.setEntityManagerFactory(emf);
      storedEvent.update(e);
      em.merge(storedEvent);
      em.getTransaction().commit();
    } catch (Exception e1) {
      logger.warn("Could not update event {}. Reason: {}",e,e1.getMessage());
      return false;
    } finally {
      em.close();
      updated = System.currentTimeMillis();
    }
    return true;
  }  
  
  public boolean updateRecurringEvent(RecurringEvent e) {
    EntityManager em = emf.createEntityManager();
    try {
      em.getTransaction().begin();
      RecurringEvent storedEvent =  getRecurringEvent(e.getRecurringEventId());
      if (storedEvent == null) return false;
      storedEvent.setEntityManagerFactory(emf);
      storedEvent.update(e);      
      em.merge(storedEvent);
      em.getTransaction().commit();
    } finally {
      em.close();
      updated = System.currentTimeMillis();
    }
    return true;
  }  

  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#findConflictingEvents(org.opencastproject.scheduler.api.SchedulerEvent)
   */
  @Override
  public SchedulerEvent[] findConflictingEvents(SchedulerEvent e) {
    SchedulerEvent[] results = null;
    Event event = ((SchedulerEventImpl) e).toEvent();
    Event [] events = findConflictingEvents(event);
    results = new SchedulerEvent [events.length];
    for (int i = 0; i < events.length; i++) results[i] = events[i].toSchedulerEvent();
    return results;
  }
  
  public Event[] findConflictingEvents (Event e) {
    List<Event> events = new LinkedList<Event>(Arrays.asList(getAllEvents()));
    //reduce to device first
    events = filterEventsForExactValue(events, "device", e.getValue("device"));
    
    //all events that start at the same time or later
    long start = e.getStartdate().getTime() -1; // make sure that the same start time is included too;
    events = filterEventsForAfterDate(events, new DateTime(start));
    
    //all events that stop at the same time or earlier
    long end = e.getEnddate().getTime() +1; // make sure that the same stop time is included too;
    events = filterEventsForBeforeDate(events, new DateTime(end));
    
    return events.toArray(new Event[0]);
  }
  
  public Event[] findConflictingEvents (RecurringEvent rEvent) throws IncompleteDataException {
    rEvent.buildMetadataTable(rEvent.getMetadata());
    if (rEvent.getRecurrence() == null || rEvent.getValue("recurrenceStart") == null || rEvent.getValue("device") == null ||
            rEvent.getValue("recurrenceEnd") == null || rEvent.getValue("recurrenceDuration") == null) 
      throw new IncompleteDataException();
    List<Event> events = rEvent.generatedEvents();
    HashSet<Event> results = new HashSet<Event>();
    for (Event event : events) {
      event.setRecurringEvent(rEvent);
      Event [] conflicts = findConflictingEvents(event);
      results.addAll(Arrays.asList(conflicts));
    }
    
    return results.toArray(new Event [0]);
  }  
  
  public void activate (ComponentContext cc) {
    super.activate(cc); 
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.scheduler.impl", persistenceProperties);
  }
  
  public void destroy() {
    emf.close();
  }  
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.scheduler.api.SchedulerService#getCalendarForCaptureAgent(java.lang.String)
   */
  public String getCalendarForCaptureAgent(String captureAgentID) {
    if (updatedCalendar > updated && calendars.containsKey(captureAgentID) && calendars.get(captureAgentID) != null) {
      logger.debug("Using cached calendar for {}", captureAgentID);
      return calendars.get(captureAgentID);
    } 
    if (updatedCalendar < updated) calendars = new Hashtable<String, String>(); // reset all calendars, if data has been changed 
    
    SchedulerFilter filter = getFilterForCaptureAgent (captureAgentID); 
    CalendarGenerator cal = new CalendarGenerator(dcGenerator, caGenerator, seriesService);
    Event[] events = getEventsJPA(filter);
    
    for (int i = 0; i < events.length; i++) cal.addEvent(events[i]);
    
    try {
      cal.getCalendar().validate();
    } catch (ValidationException e1) {
      logger.warn("Could not validate Calendar: {}", e1.getMessage());
    }
    
    String result = cal.getCalendar().toString(); // CalendarOutputter performance sucks (jmh)
    
    updatedCalendar = System.currentTimeMillis();
    calendars.put(captureAgentID, result);
    return result;
  }  
  
}
