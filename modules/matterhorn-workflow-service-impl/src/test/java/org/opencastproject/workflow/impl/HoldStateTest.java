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

import org.opencastproject.media.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilder;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.workflow.api.AbstractResumableWorkflowOperationHandler;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.impl.WorkflowServiceImpl.HandlerRegistration;
import org.opencastproject.workingfilerepository.impl.WorkingFileRepositoryImpl;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HoldStateTest {
  /** The solr root directory */
  protected static final String storageRoot = "." + File.separator + "target" + File.separator + "workflow-test-db";

  private WorkflowServiceImpl service = null;
  private WorkflowDefinition def = null;
  private WorkflowInstance workflow = null;
  private MediaPackage mp = null;
  private WorkflowServiceImplDaoFileImpl dao = null;
  private WorkingFileRepositoryImpl repo = null;

  @Before
  public void setup() throws Exception {
    // always start with a fresh solr root directory
    File sRoot = new File(storageRoot);
    try {
      FileUtils.deleteDirectory(sRoot);
      FileUtils.forceMkdir(sRoot);
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }

    MediaPackageBuilder mediaPackageBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(new File("target/test-classes")));
    mp = mediaPackageBuilder.loadFromXml(WorkflowServiceImplTest.class.getResourceAsStream("/mediapackage-1.xml"));

    // create operation handlers for our workflows
    final Set<HandlerRegistration> handlerRegistrations = new HashSet<HandlerRegistration>();
    handlerRegistrations.add(new HandlerRegistration("op1", new HoldingWorkflowOperationHandler()));
    handlerRegistrations.add(new HandlerRegistration("op2", new ContinuingWorkflowOperationHandler()));

    // instantiate a service implementation and its DAO, overriding the methods that depend on the osgi runtime
    service = new WorkflowServiceImpl() {
      protected Set<HandlerRegistration> getRegisteredHandlers() {
        return handlerRegistrations;
      }
    };

    repo = new WorkingFileRepositoryImpl(storageRoot, sRoot.toURI().toString());
    dao = new WorkflowServiceImplDaoFileImpl();
    dao.setRepository(repo);
    dao.setStorageRoot(storageRoot + File.separator + "lucene");
    dao.activate();
    service.setDao(dao);
    service.activate(null);

    def = WorkflowBuilder.getInstance().parseWorkflowDefinition(
            WorkflowServiceImplTest.class.getResourceAsStream("/workflow-definition-holdstate.xml"));
    service.registerWorkflowDefinition(def);
  }

  @After
  public void teardown() throws Exception {
    System.out.println("All tests finished... tearing down...");
    if(workflow != null) {
      while (!service.getWorkflowById(workflow.getId()).getState().equals(WorkflowState.SUCCEEDED)) {
        System.out.println("Waiting for workflow to complete, current state is " + service.getWorkflowById(workflow.getId()).getState());
        Thread.sleep(1000);
      }
    }
    dao.deactivate();
    service.deactivate();
  }

  @Test
  public void testHoldAndResume() throws Exception {
    Map<String, String> initialProps = new HashMap<String, String>();
    initialProps.put("testproperty", "foo");
    workflow = service.start(def, mp, initialProps);
    while (!service.getWorkflowById(workflow.getId()).getState().equals(WorkflowState.PAUSED)) {
      System.out.println("Waiting for workflow to enter paused state...");
      try {Thread.sleep(1000);} catch (InterruptedException e) {}
    }

    // The variable "testproperty" should have been replaced by "foo", but not "anotherproperty"
    String xml = WorkflowBuilder.getInstance().toXml(workflow);
    Assert.assertTrue(xml.contains("foo"));
    Assert.assertTrue(xml.contains("anotherproperty"));

    // Simulate a user resuming and submitting new properties (this time, with a value for "anotherproperty") to the workflow
    Map<String, String> resumeProps = new HashMap<String, String>();
    resumeProps.put("anotherproperty", "bar");
    service.resume(workflow.getId(), resumeProps);

    WorkflowInstance fromDb = service.getWorkflowById(workflow.getId());
    String xmlFromDb = WorkflowBuilder.getInstance().toXml(fromDb);
    Assert.assertTrue(!xmlFromDb.contains("anotherproperty"));
    Assert.assertTrue(xmlFromDb.contains("foo"));
    Assert.assertTrue(xmlFromDb.contains("bar"));
  }

  class HoldingWorkflowOperationHandler extends AbstractResumableWorkflowOperationHandler {}

  class ContinuingWorkflowOperationHandler extends AbstractWorkflowOperationHandler {}

}
