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
package org.opencastproject.ingest.impl;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workspace.api.Workspace;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class IngestServiceImplTest {
  private IngestServiceImpl service = null;
  private WorkflowService workflowService = null;
  private WorkflowInstance workflowInstance = null;
  private Workspace workspace = null;
  private static URI baseDir;
  private static URI urlTrack;
  private static URI urlTrack1;
  private static URI urlTrack2;
  private static URI urlCatalog;
  private static URI urlCatalog1;
  private static URI urlCatalog2;
  private static URI urlAttachment;
  private static URI urlPackage;
  private static URI urlPackageOld;

  private static File ingestTempDir;
  private static File packageFile;

  private static long workflowInstanceID = 1L;

  @BeforeClass
  public static void beforeClass() throws URISyntaxException {
    baseDir = IngestServiceImplTest.class.getResource("/").toURI();
    urlTrack = IngestServiceImplTest.class.getResource("/av.mov").toURI();
    urlTrack1 = IngestServiceImplTest.class.getResource("/vonly.mov").toURI();
    urlTrack2 = IngestServiceImplTest.class.getResource("/aonly.mov").toURI();
    urlCatalog = IngestServiceImplTest.class.getResource("/mpeg-7.xml").toURI();
    urlCatalog1 = IngestServiceImplTest.class.getResource("/dublincore.xml").toURI();
    urlCatalog2 = IngestServiceImplTest.class.getResource("/series-dublincore.xml").toURI();
    urlAttachment = IngestServiceImplTest.class.getResource("/cover.png").toURI();
    urlPackage = IngestServiceImplTest.class.getResource("/data.zip").toURI();
    urlPackageOld = IngestServiceImplTest.class.getResource("/data.old.zip").toURI();

    ingestTempDir = new File(new File(baseDir), "ingest-temp");
    packageFile = new File(ingestTempDir, baseDir.relativize(urlPackage).toString());
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Before
  public void setUp() throws Exception {

    FileUtils.forceMkdir(ingestTempDir);

    // set up service and mock workspace
    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlTrack);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlCatalog);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlAttachment);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlTrack1);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlTrack2);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlCatalog1);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlCatalog2);
    EasyMock.expect(
            workspace.put((String) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlCatalog);

    EasyMock.expect(
            workspace.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlTrack1);
    EasyMock.expect(
            workspace.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlTrack2);
    EasyMock.expect(
            workspace.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlCatalog1);
    EasyMock.expect(
            workspace.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlCatalog2);
    EasyMock.expect(
            workspace.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlCatalog);

    EasyMock.expect(
            workspace.moveTo((URI) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (String) EasyMock.anyObject())).andReturn(urlTrack1);
    EasyMock.expect(
            workspace.moveTo((URI) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (String) EasyMock.anyObject())).andReturn(urlTrack2);
    EasyMock.expect(
            workspace.moveTo((URI) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (String) EasyMock.anyObject())).andReturn(urlCatalog1);
    EasyMock.expect(
            workspace.moveTo((URI) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (String) EasyMock.anyObject())).andReturn(urlCatalog2);
    EasyMock.expect(
            workspace.moveTo((URI) EasyMock.anyObject(), (String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (String) EasyMock.anyObject())).andReturn(urlCatalog);

    EasyMock.expect(
            workspace.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlPackage);

    EasyMock.expect(
            workspace.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andReturn(urlPackageOld);

    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andReturn(packageFile).anyTimes();

    workflowInstance = EasyMock.createNiceMock(WorkflowInstance.class);
    EasyMock.expect(workflowInstance.getId()).andReturn(workflowInstanceID);

    workflowService = EasyMock.createNiceMock(WorkflowService.class);
    EasyMock.expect(
            workflowService.start((WorkflowDefinition) EasyMock.anyObject(), (MediaPackage) EasyMock.anyObject(),
                    (Map) EasyMock.anyObject())).andReturn(workflowInstance);
    EasyMock.expect(
            workflowService.start((WorkflowDefinition) EasyMock.anyObject(), (MediaPackage) EasyMock.anyObject(),
                    (Map) EasyMock.anyObject())).andReturn(workflowInstance);
    EasyMock.expect(
            workflowService.start((WorkflowDefinition) EasyMock.anyObject(), (MediaPackage) EasyMock.anyObject()))
            .andReturn(workflowInstance);

    EasyMock.replay(workspace, workflowInstance, workflowService);

    User anonymous = new User("anonymous", DefaultOrganization.DEFAULT_ORGANIZATION_ID,
            new String[] { DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS });
    UserDirectoryService userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser((String) EasyMock.anyObject())).andReturn(anonymous).anyTimes();
    EasyMock.replay(userDirectoryService);

    Organization organization = new DefaultOrganization();
    OrganizationDirectoryService organizationDirectoryService = EasyMock.createMock(OrganizationDirectoryService.class);
    EasyMock.expect(organizationDirectoryService.getOrganization((String) EasyMock.anyObject()))
            .andReturn(organization).anyTimes();
    EasyMock.replay(organizationDirectoryService);

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(anonymous).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(organization).anyTimes();
    EasyMock.replay(securityService);

    HttpEntity entity = EasyMock.createMock(HttpEntity.class);
    InputStream is = getClass().getResourceAsStream("/av.mov");
    byte[] movie = IOUtils.toByteArray(is);
    IOUtils.closeQuietly(is);
    EasyMock.expect(entity.getContent()).andReturn(new ByteArrayInputStream(movie)).anyTimes();
    EasyMock.replay(entity);

    StatusLine statusLine = EasyMock.createMock(StatusLine.class);
    EasyMock.expect(statusLine.getStatusCode()).andReturn(200).anyTimes();
    EasyMock.replay(statusLine);

    HttpResponse httpResponse = EasyMock.createMock(HttpResponse.class);
    EasyMock.expect(httpResponse.getStatusLine()).andReturn(statusLine).anyTimes();
    EasyMock.expect(httpResponse.getEntity()).andReturn(entity).anyTimes();
    EasyMock.replay(httpResponse);

    TrustedHttpClient httpClient = EasyMock.createNiceMock(TrustedHttpClient.class);
    EasyMock.expect(httpClient.execute((HttpGet) EasyMock.anyObject())).andReturn(httpResponse).anyTimes();
    EasyMock.replay(httpClient);

    service = new IngestServiceImpl();
    service.setHttpClient(httpClient);
    service.setWorkspace(workspace);
    service.setWorkflowService(workflowService);
    ServiceRegistryInMemoryImpl serviceRegistry = new ServiceRegistryInMemoryImpl(service, securityService,
            userDirectoryService, organizationDirectoryService);
    serviceRegistry.registerService(service);
    service.setServiceRegistry(serviceRegistry);
    service.defaultWorkflowDefinionId = "sample";
    serviceRegistry.registerService(service);
  }

  @After
  public void tearDown() {
    FileUtils.deleteQuietly(ingestTempDir);
  }

  @Test
  public void testThinClient() throws Exception {

    MediaPackage mediaPackage = null;

    mediaPackage = service.createMediaPackage();
    mediaPackage = service.addTrack(urlTrack, null, mediaPackage);
    mediaPackage = service.addCatalog(urlCatalog, MediaPackageElements.EPISODE, mediaPackage);
    mediaPackage = service.addAttachment(urlAttachment, MediaPackageElements.MEDIAPACKAGE_COVER_FLAVOR, mediaPackage);
    WorkflowInstance instance = service.ingest(mediaPackage);
    Assert.assertEquals(1, mediaPackage.getTracks().length);
    Assert.assertEquals(1, mediaPackage.getCatalogs().length);
    Assert.assertEquals(1, mediaPackage.getAttachments().length);
    Assert.assertEquals(workflowInstanceID, instance.getId());
  }

  @Test
  public void testThickClient() throws Exception {

    FileUtils.copyURLToFile(urlPackage.toURL(), packageFile);

    InputStream packageStream = null;
    try {
      packageStream = urlPackage.toURL().openStream();
      WorkflowInstance instance = service.addZippedMediaPackage(packageStream);

      // Assert.assertEquals(2, mediaPackage.getTracks().length);
      // Assert.assertEquals(3, mediaPackage.getCatalogs().length);
      Assert.assertEquals(workflowInstanceID, instance.getId());
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    } finally {
      IOUtils.closeQuietly(packageStream);
    }

  }

  @Test
  public void testThickClientOldMP() throws Exception {

    FileUtils.copyURLToFile(urlPackageOld.toURL(), packageFile);

    InputStream packageStream = null;
    try {
      packageStream = urlPackageOld.toURL().openStream();
      WorkflowInstance instance = service.addZippedMediaPackage(packageStream);

      // Assert.assertEquals(2, mediaPackage.getTracks().length);
      // Assert.assertEquals(3, mediaPackage.getCatalogs().length);
      Assert.assertEquals(workflowInstanceID, instance.getId());
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    } finally {
      IOUtils.closeQuietly(packageStream);
    }

  }

}
