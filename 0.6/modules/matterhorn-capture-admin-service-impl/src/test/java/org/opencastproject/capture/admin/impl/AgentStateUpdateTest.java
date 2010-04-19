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
package org.opencastproject.capture.admin.impl;

import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.AgentState;
import org.opencastproject.capture.admin.api.AgentStateUpdate;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AgentStateUpdateTest {
  private Agent agent = null;
  private AgentStateUpdate asu = null;

  @Before
  public void setup() throws InterruptedException {
    agent = new AgentImpl("test", AgentState.IDLE, null);
    Assert.assertNotNull(agent);
    Thread.sleep(5);
    asu = new AgentStateUpdate(agent);
    Assert.assertNotNull(asu);
  }

  @After
  public void teardown() {
    agent = null;
    asu = null;
  }

  @Test
  public void correctInformation() {
    Assert.assertEquals("test", asu.name);
    Assert.assertEquals(AgentState.IDLE, asu.state);
    if (asu.time_since_last_update <= 1) {
      Assert.fail("Invalid update time in agent state update");
    }
  }

  @Test
  //This is a stupid test, but it gets us up to 100%...
  public void blank() {
    asu = new AgentStateUpdate();
    Assert.assertNotNull(asu);
    Assert.assertNull(asu.name);
    Assert.assertNull(asu.state);
    Assert.assertNull(asu.time_since_last_update);
  }
}
