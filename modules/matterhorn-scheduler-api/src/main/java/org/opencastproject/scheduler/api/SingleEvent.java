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
package org.opencastproject.scheduler.api;

import java.util.List;
import java.util.Date;

public interface SingleEvent {
  public List<? extends Metadata> getCompleteMetadata();
  public Date getEnddate();
  public Date getStartdate();
  public int getPositionInRecurrence();
  public void setPositionInRecurrence(int positionInRecurrence);
  public RecurringEvent getRecurringEvent();
  public void setRecurringEvent(RecurringEvent rEvent);
  public String getRecurringEventId();
  public void setRecurrginEventId(String id);
}
