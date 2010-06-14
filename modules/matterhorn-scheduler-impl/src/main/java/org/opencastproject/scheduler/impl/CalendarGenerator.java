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
package org.opencastproject.scheduler.impl;

import org.opencastproject.scheduler.api.SchedulerEvent;
import org.opencastproject.scheduler.impl.jpa.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.ResourceList;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.Encoding;
import net.fortuna.ical4j.model.parameter.FmtType;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.parameter.XParameter;
import net.fortuna.ical4j.model.property.Attach;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.RelatedTo;
import net.fortuna.ical4j.model.property.Resources;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;

/**
 *Create an iCalendar from the provided SchedulerEvents
 *
 */
public class CalendarGenerator {
  private static final Logger logger = LoggerFactory.getLogger(CalendarGenerator.class);
  
  Calendar cal;
  DublinCoreGenerator dcGenerator;
  CaptureAgentMetadataGenerator caGenerator;
  
  
  /**
   * default constructor that creates a CalendarGenerator object
   * @param dcGenerator A DublinCoreGenerator is needed but cannot be constructed in this object
   * @param caGenerator A CaptureAgentMetadataGenerator is needed but cannot be constructed in this object
   */
  public CalendarGenerator (DublinCoreGenerator dcGenerator, CaptureAgentMetadataGenerator caGenerator) {
    cal = new Calendar();
    cal.getProperties().add(new ProdId("Opencast Matterhorn Calendar File 0.5"));
    cal.getProperties().add(Version.VERSION_2_0);
    cal.getProperties().add(CalScale.GREGORIAN);
    this.dcGenerator = dcGenerator;
    this.caGenerator = caGenerator;
  }
  
  /**
   * gets the iCalendar creates by this object.
   * @return the iCalendar
   */
  public Calendar getCalendar() {
    return cal;
  }

  /**
   * Sets an iCalender to work with
   * @param cal the iCalendar to set
   */
  public void setCalendar(Calendar cal) {
    this.cal = cal;
  }

  public boolean addEvent (SchedulerEvent e) {
    return addEvent (((SchedulerEventImpl)e).toEvent());
  }
  
  /**
   * adds an SchedulerEvent as a new entry to this iCalendar 
   * @param e the event to add 
   * @return true if the event could be added. 
   */
  public boolean addEvent (Event e) {
    logger.debug("creating iCal VEvent from SchedulerEvent: {}", e.toString());
    DateTime startDate = new DateTime(e.getStartdate());
    DateTime endDate = new DateTime(e.getEnddate());
    startDate.setUtc(true);
    endDate.setUtc(true);
    VEvent event = new VEvent(startDate, endDate, e.getValue("title"));
    try {
      ParameterList pl = new ParameterList();
      pl.add(new Cn(e.getValue("creator")));
      event.getProperties().add(new Uid(e.getEventId()));
      
      // TODO Organizer should be URI (email-address?) created fake adress
      if (e.containsKey("creator") && ! e.getValue("creator").equalsIgnoreCase("null")) event.getProperties().add(new Organizer(pl ,e.getValue("creator").replace(" ", "_")+"@matterhorn.opencast"));
      if (e.containsKey("abstract") && ! e.getValue("abstract").equalsIgnoreCase("null")) event.getProperties().add(new Description(e.getValue("abstract")));
      if (e.containsKey("location") && ! e.getValue("location").equalsIgnoreCase("null")) event.getProperties().add(new Location(e.getValue("location")));
      if (e.containsKey("seriesid") && ! e.getValue("seriesid").equalsIgnoreCase("null")) event.getProperties().add(new RelatedTo(e.getValue("seriesid")));
      
// Not used anyway, so skip it
/*      if (e.getAttendees() != null) {
        String [] attendees = e.getAttendees();
        for (int i = 0; i < attendees.length; i++) {
          ParameterList plAtt = new ParameterList();
          plAtt.add(new Cn(attendees[i]));
          // TODO Organizer should be URI (email-address?) created fake adress
          if ( ! attendees[i].equals(e.getDevice())) event.getProperties().add(new Attendee(plAtt, attendees[i].replace(" ","_")+"@matterhorn.opencast"));
        }
      }*/
/*      if (e.getResources() != null) {
        String [] resources = e.getResources();
        ResourceList resList = new ResourceList();
        for (int i = 0; i < resources.length; i++) resList.add(resources[i]);     
        event.getProperties().add(new Resources(resList));
      }*/

        ParameterList dcParameters = new ParameterList();
        dcParameters.add(new FmtType("application/xml"));
        dcParameters.add(Value.BINARY);
        dcParameters.add(Encoding.BASE64);
        dcParameters.add(new XParameter("X-APPLE-FILENAME", "metadata.xml"));
        Attach metadataAttachment = new Attach(dcParameters, dcGenerator.generateAsString(e).getBytes("UTF-8"));
        event.getProperties().add(metadataAttachment);
        
        ParameterList caParameters = new ParameterList(); 
        caParameters.add(new FmtType("application/text"));        
        caParameters.add(Value.BINARY);
        caParameters.add(Encoding.BASE64);
        caParameters.add(new XParameter("X-APPLE-FILENAME", "org.opencastproject.capture.agent.properties"));
        Attach agentsAttachment = new Attach(caParameters, caGenerator.generateAsString(e).getBytes("UTF-8"));
        event.getProperties().add(agentsAttachment);

        
    } catch (Exception e1) {
      logger.error("could not create Calendar entry for Event {}. Reason : {} ", e.toString(), e1.getMessage());
      return false;
    }
    cal.getComponents().add(event);
    
    logger.debug("new VEvent = {} ", event.toString() );
    return true;
  }
}
