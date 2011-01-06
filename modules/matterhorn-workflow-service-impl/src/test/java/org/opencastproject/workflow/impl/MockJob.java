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
package org.opencastproject.workflow.impl;

import org.opencastproject.job.api.Job;

import java.util.Date;
import java.util.List;

/**
 * A mock job implementation for unit testing.
 */
public class MockJob implements Job {

  MockJob() {
    this.id = (long) (Math.random() * Long.MAX_VALUE);
  }

  long id;
  Status status;
  String payload;

  public String getProcessingHost() {
    return null;
  }

  public long getId() {
    return id;
  }

  public Status getStatus() {
    return status;
  }

  public String getJobType() {
    return "workflow-test";
  }

  public void setHost(String host) {
  }

  public void setId(long id) {
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public void setType(String type) {
  }

  public String toXml() {
    return null;
  }

  public Date getDateCompleted() {
    return null;
  }

  public Date getDateCreated() {
    return null;
  }

  public Date getDateStarted() {
    return null;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public int getVersion() {
    return 0;
  }

  public String getOperationType() {
    return null;
  }

  public List<String> getArguments() {
    return null;
  }

  public String getCreatedHost() {
    return null;
  }
}
