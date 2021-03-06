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
package org.opencastproject.migration;

import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowParsingException;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class WorkflowMigrationServiceTest {

  /**
   * Test class for the workflow migration service
   */
  private WorkflowMigrationService workflowMigrationService = new WorkflowMigrationService();

  @Test
  public void testConvertionTo14() throws IOException {
    workflowMigrationService.wfrUrl = "http://media.uct.ac.za/files/mediapackage";
    workflowMigrationService.downloadUrl = "http://media.uct.ac.za/static";

    InputStream in = getClass().getResourceAsStream("/workflow13.xml");
    String workflow = IOUtils.toString(in, "UTF-8");
    try {
      WorkflowInstance instance = workflowMigrationService.parseOldWorkflowInstance(workflow, "mh_default_org");
      Attachment[] attachments = instance.getMediaPackage().getAttachments(MediaPackageElements.XACML_POLICY);
      Assert.assertEquals(
              "http://media.uct.ac.za/static/engage-player/dacb60eb-f7dd-4824-be85-31d2d9060c31/security-policy/xacml.xml",
              attachments[0].getURI().toString());
    } catch (WorkflowParsingException e) {
      Assert.fail(e.getMessage());
    }
  }
}
