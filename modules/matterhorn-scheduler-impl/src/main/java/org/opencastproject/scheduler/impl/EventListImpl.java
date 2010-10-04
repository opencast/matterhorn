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

import org.opencastproject.scheduler.api.Event;

import java.util.List;
import java.util.LinkedList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="events")
public class EventListImpl {

  @XmlElement(name="event")
  protected List<EventImpl> events;

  public EventListImpl() {
    this.events = new LinkedList<EventImpl>();
  }
  
  public EventListImpl(List<Event> eventList){
    this.events = new LinkedList<EventImpl>();
    this.setEvents(eventList);
  }
  
  public void setEvents(List<Event> eventList) {
    if(!this.events.isEmpty()){
      this.events.clear();
    }
    for(Event e : eventList){
      this.events.add((EventImpl) e);
    }
  }
}
