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

package org.opencastproject.episode.impl;

import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opencastproject.episode.api.EpisodeQuery;
import org.opencastproject.episode.api.EpisodeService;
import org.opencastproject.episode.api.SearchResult;
import org.opencastproject.episode.api.SearchResultItem;
import org.opencastproject.episode.api.Version;
import org.opencastproject.episode.impl.elementstore.DeletionSelector;
import org.opencastproject.episode.impl.elementstore.ElementStore;
import org.opencastproject.episode.impl.persistence.AbstractEpisodeServiceDatabase;
import org.opencastproject.episode.impl.persistence.EpisodeServiceDatabase;
import org.opencastproject.episode.impl.solr.SolrIndexManager;
import org.opencastproject.episode.impl.solr.SolrRequester;
import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.identifier.IdBuilderFactory;
import org.opencastproject.metadata.api.StaticMetadataService;
import org.opencastproject.metadata.dublincore.StaticMetadataServiceDublinCoreImpl;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.series.impl.SeriesServiceImpl;
import org.opencastproject.series.impl.solr.SeriesServiceSolrIndex;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.solr.SolrServerFactory;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.persistence.PersistenceEnv;
import org.opencastproject.util.persistence.PersistenceUtil;
import org.opencastproject.workspace.api.Workspace;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.security.api.SecurityConstants.DEFAULT_ORGANIZATION_ADMIN;
import static org.opencastproject.security.api.SecurityConstants.DEFAULT_ORGANIZATION_ANONYMOUS;
import static org.opencastproject.security.api.SecurityConstants.DEFAULT_ORGANIZATION_ID;
import static org.opencastproject.security.api.SecurityConstants.DEFAULT_ORGANIZATION_NAME;
import static org.opencastproject.util.UrlSupport.DEFAULT_BASE_URL;
import static org.opencastproject.util.data.VCell.cell;

/**
 * Tests the functionality of the search service.
 * 
 * todo setup scenario where gathering metadata from both the media package and the dublin core is required
 * (StaticMetadataServiceMediaPackageImpl, StaticMetadataServiceDublinCoreImpl)
 */
public class EpisodeServiceImplTest {

  /** The search service */
  private EpisodeServiceImpl service = null;

  /** The solr root directory */
  private final String solrRoot = "target" + File.separator + "opencast" + File.separator + "searchindex";

  /** The access control list returned by the mocked authorization service */
  private AccessControlList acl = null;

  /** A user with permissions. */
  private final User userWithPermissions = new User("sample", "opencastproject.org", new String[] { "ROLE_STUDENT",
          "ROLE_OTHERSTUDENT", new DefaultOrganization().getAnonymousRole() });

  /** A user without permissions. */
  private final User userWithoutPermissions = new User("sample", "opencastproject.org", new String[] { "ROLE_NOTHING" });

  private final Organization defaultOrganization = new DefaultOrganization();
  private final User defaultUser = userWithPermissions;

  private SolrServer solrServer;
  private Responder<User> userResponder;
  private Responder<Organization> organizationResponder;
  private EpisodeServiceDatabase episodeDatabase;
  private PersistenceEnv penv;
  private String storage;

  private static class Responder<A> implements IAnswer<A> {
    private A response;

    Responder(A response) {
      this.response = response;
    }

    public void setResponse(A response) {
      this.response = response;
    }

    @Override
    public A answer() throws Throwable {
      return response;
    }
  }

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    final File dcFile = new File(getClass().getResource("/dublincore.xml").toURI());
    final File dcSeriesFile = new File(getClass().getResource("/series-dublincore.xml").toURI());
    Assert.assertNotNull(dcFile);

    // workspace
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andAnswer(new IAnswer<File>() {
      public File answer() throws Throwable {
        return EasyMock.getCurrentArguments()[0].toString().contains("series") ? dcSeriesFile : dcFile;
      }
    }).anyTimes();
    EasyMock.replay(workspace);

    // service registry
    ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.expect(
            serviceRegistry.createJob((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (List<String>) EasyMock.anyObject(), (String) EasyMock.anyObject(), EasyMock.anyBoolean()))
            .andReturn(new JaxbJob()).anyTimes();
    EasyMock.expect(serviceRegistry.updateJob((Job) EasyMock.anyObject())).andReturn(new JaxbJob()).anyTimes();
    EasyMock.expect(serviceRegistry.getJobs((String) EasyMock.anyObject(), (Status) EasyMock.anyObject()))
            .andReturn(new ArrayList<Job>()).anyTimes();
    EasyMock.replay(serviceRegistry);

    ElementStore elementStore = EasyMock.createNiceMock(ElementStore.class);
    EasyMock.expect(elementStore.delete(EasyMock.<DeletionSelector>anyObject()))
            .andReturn(true).once();
    EasyMock.replay(elementStore);

    // mpeg7 service
    Mpeg7CatalogService mpeg7CatalogService = new Mpeg7CatalogService();

    // security service
    userResponder = new Responder<User>(defaultUser);
    organizationResponder = new Responder<Organization>(defaultOrganization);
    final SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andAnswer(userResponder).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andAnswer(organizationResponder).anyTimes();
    EasyMock.replay(securityService);

    long currentTime = System.currentTimeMillis();
    storage = PathSupport.concat("target", "db" + currentTime + ".h2.db");

    // Persistence storage
    penv = PersistenceUtil.newTestPersistenceEnv("org.opencastproject.episode.impl.persistence");
    episodeDatabase = new AbstractEpisodeServiceDatabase() {
      @Override protected PersistenceEnv getPenv() {
        return penv;
      }

      @Override protected SecurityService getSecurityService() {
        return securityService;
      }
    };

    // acl
    String anonymousRole = securityService.getOrganization().getAnonymousRole();
    acl = new AccessControlList(new AccessControlEntry(anonymousRole, "read", true));
    /* The authorization service */
    AuthorizationService authorizationService = EasyMock.createNiceMock(AuthorizationService.class);
    EasyMock.expect(authorizationService.getAccessControlList((MediaPackage) EasyMock.anyObject())).andReturn(acl)
            .anyTimes();
    EasyMock.expect(
            authorizationService.hasPermission((MediaPackage) EasyMock.anyObject(), (String) EasyMock.anyObject()))
            .andReturn(true).anyTimes();

//    ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
//
//    EasyMock.expect(
//            serviceRegistry.createJob((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
//                                      (List<String>) EasyMock.anyObject(), (String) EasyMock.anyObject(), EasyMock.anyBoolean()))
//            .andReturn(new JaxbJob()).anyTimes();
//    EasyMock.expect(serviceRegistry.updateJob((Job) EasyMock.anyObject())).andReturn(new JaxbJob()).anyTimes();
//    EasyMock.expect(serviceRegistry.getJobs((String) EasyMock.anyObject(), (Status) EasyMock.anyObject()))
//            .andReturn(jobs).anyTimes();
//    EasyMock.replay(serviceRegistry);
//
//    service.setServiceRegistry(serviceRegistry);

    OrganizationDirectoryService orgDirectory = EasyMock.createNiceMock(OrganizationDirectoryService.class);
    EasyMock.expect(orgDirectory.getOrganization((String) EasyMock.anyObject())).andReturn(new DefaultOrganization())
            .anyTimes();
    EasyMock.replay(orgDirectory);

    // episode service
    solrServer = EpisodeServicePublisher.setupSolr(new File(solrRoot));
    StaticMetadataService mdService = newStaticMetadataService(workspace);
    SeriesService seriesService = newSeriesService();
    service = new EpisodeServiceImpl(new SolrRequester(solrServer, securityService),
                                     new SolrIndexManager(solrServer,
                                                          workspace,
                                                          cell(Arrays.asList(mdService)),
                                                          seriesService,
                                                          mpeg7CatalogService,
                                                          securityService),
                                     securityService,
                                     authorizationService,
                                     orgDirectory,
                                     serviceRegistry,
                                     null,
                                     null,
                                     episodeDatabase,
                                     elementStore);
    EasyMock.replay(authorizationService);
  }

  private StaticMetadataService newStaticMetadataService(Workspace workspace) {
    StaticMetadataServiceDublinCoreImpl service = new StaticMetadataServiceDublinCoreImpl();
    service.setWorkspace(workspace);
    return service;
  }

  private SeriesService newSeriesService() {
    SeriesServiceImpl service = new SeriesServiceImpl();
    service.setIndex(new SeriesServiceSolrIndex());
    return service;
  }

  @After
  public void tearDown() throws Exception {
    penv.close();
    FileUtils.deleteQuietly(new File(storage));
    episodeDatabase = null;
    SolrServerFactory.shutdown(solrServer);
    FileUtils.deleteDirectory(new File(solrRoot));
    service = null;
  }

  /**
   * Test whether an empty search index will work.
   */
  @Test
  public void testEmptySearchIndex() {
    SearchResult result = service.find(new EpisodeQuery().withId("foo"));
    assertEquals(0, result.size());
  }

  /**
   * Adds a simple media package that has a dublin core for the episode only.
   */
  @Test
  public void testGetMediaPackage() throws Exception {
    MediaPackage mediaPackage = getMediaPackage("/manifest-simple.xml");

    // Make sure our mocked ACL has the read and write permission
    acl.getEntries().add(
            new AccessControlEntry(userWithPermissions.getRoles()[0], EpisodeService.READ_PERMISSION, true));
    acl.getEntries().add(
            new AccessControlEntry(userWithPermissions.getRoles()[0], EpisodeService.WRITE_PERMISSION, true));

    // Add the media package to the search index
    service.add(mediaPackage);

    // Make sure it's properly indexed and returned for authorized users
    EpisodeQuery q = new EpisodeQuery();
    q.withId("10.0000/1");
    SearchResult result = service.find(q);
    assertEquals(1, result.size());
    assertFalse(result.getItems().get(0).getOcLocked());

    service.delete(mediaPackage.getIdentifier().toString());

    q = new EpisodeQuery();
    q.withId("10.0000/1");
    result = service.find(q);
    assertEquals(0, result.size());

    acl.getEntries().clear();
    acl.getEntries().add(new AccessControlEntry("ROLE_UNKNOWN", EpisodeService.READ_PERMISSION, true));
    acl.getEntries().add(
            new AccessControlEntry(userWithPermissions.getRoles()[0], EpisodeService.WRITE_PERMISSION, true));

    // Add the media package to the search index
    service.add(mediaPackage);

    // This mediapackage should not be readable by the current user (due to the lack of role ROLE_UNKNOWN)
    q = new EpisodeQuery();
    q.withId("10.0000/1");
    assertEquals(0, service.find(q).size());
  }

//  @Test
//  public void testLockMediaPackage() throws Exception {
//    MediaPackage mediaPackage = getMediaPackage("/manifest-simple.xml");
//
//    // Make sure our mocked ACL has the read and write permission
//    acl.getEntries().add(
//            new AccessControlEntry(userWithPermissions.getRoles()[0], EpisodeService.READ_PERMISSION, true));
//    acl.getEntries().add(
//            new AccessControlEntry(userWithPermissions.getRoles()[0], EpisodeService.WRITE_PERMISSION, true));
//
//    // Add the media package to the episode service
//    service.add(mediaPackage);
//
//    // lock it
//    service.lock("10.0000/1", true);
//    {
//      EpisodeQuery q = new EpisodeQuery();
//      q.withId("10.0000/1");
//      SearchResult result = service.find(q);
//      assertEquals(1, result.size());
//      assertTrue(result.getItems().get(0).getOcLocked());
//    }
//
//    // then unlock
//    service.lock("10.0000/1", false);
//    {
//      EpisodeQuery q = new EpisodeQuery();
//      q.withId("10.0000/1");
//      SearchResult result = service.find(q);
//      assertEquals(1, result.size());
//      assertFalse(result.getItems().get(0).getOcLocked());
//    }
//  }

  private MediaPackage getMediaPackage(String path) throws MediaPackageException {
    MediaPackageBuilderFactory builderFactory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder mediaPackageBuilder = builderFactory.newMediaPackageBuilder();
    URL rootUrl = EpisodeServiceImplTest.class.getResource("/");
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(rootUrl));

    // Load the simple media package
    MediaPackage mediaPackage = null;
    InputStream is = null;
    try {
      is = EpisodeServiceImplTest.class.getResourceAsStream(path);
      mediaPackage = mediaPackageBuilder.loadFromXml(is);
    } finally {
      IOUtils.closeQuietly(is);
    }
    return mediaPackage;
  }

  /**
   * Tests whether an episode can be found based on its series metadata.
   */
  @Test
  @Ignore
  public void testSearchForEpisodeWithSeriesMetadata() throws Exception {
    MediaPackage mediaPackage = getMediaPackage("/manifest-full.xml");
    service.add(mediaPackage);

    SearchResult episodeMetadataResult = service.find(new EpisodeQuery().withText("Vegetation"));
    SearchResult seriesMetadataResult = service.find(new EpisodeQuery().withText("Atmospheric Science"));

    assertEquals(1, episodeMetadataResult.getItems().size());
    assertEquals(1, seriesMetadataResult.getItems().size());
  }

  /**
   * Adds a simple media package that has a dublin core for the episode only.
   */
  @Test
  public void testAddSimpleMediaPackage() throws Exception {
    MediaPackage mediaPackage = getMediaPackage("/manifest-simple.xml");

    // Make sure our mocked ACL has the read and write permission
    acl.getEntries().add(
            new AccessControlEntry(userWithPermissions.getRoles()[0], EpisodeService.READ_PERMISSION, true));
    acl.getEntries().add(
            new AccessControlEntry(userWithPermissions.getRoles()[0], EpisodeService.WRITE_PERMISSION, true));

    // Add the media package to the search index
    service.add(mediaPackage);

    // Make sure it's properly indexed and returned
    EpisodeQuery q = new EpisodeQuery();
    q.withId("10.0000/1");
    assertEquals(1, service.find(q).size());

    q = new EpisodeQuery();

    assertEquals(1, service.find(q).size());

    // Test for various fields
    q = new EpisodeQuery();
    q.withId("10.0000/1");
    SearchResult result = service.find(q);
    assertEquals(1, result.getTotalSize());
    SearchResultItem resultItem = result.getItems().get(0);
    assertNotNull(resultItem.getMediaPackage());
    assertEquals(1, resultItem.getMediaPackage().getCatalogs().length);
  }

  /**
   * Adds a simple media package that has a dublin core for the episode only.
   */
  @Test
  public void testGetOnlyLastVersion() throws Exception {
    MediaPackage mediaPackage = getMediaPackage("/manifest-simple.xml");

    // Make sure our mocked ACL has the read and write permission
    acl.getEntries().add(
            new AccessControlEntry(userWithPermissions.getRoles()[0], EpisodeService.READ_PERMISSION, true));
    acl.getEntries().add(
            new AccessControlEntry(userWithPermissions.getRoles()[0], EpisodeService.WRITE_PERMISSION, true));

    // Add the media package to the search index
    service.add(mediaPackage);
    service.add(mediaPackage);

    // Make sure it's properly indexed and returned
    EpisodeQuery q = new EpisodeQuery().withId("10.0000/1");

    assertEquals(2, service.find(q).size());

    q = new EpisodeQuery().withId("10.0000/1").withOnlyLastVersion();

    SearchResult byQuery = service.find(q);
    assertEquals(1, byQuery.size());
  }

  /**
   * Ads a simple media package that has a dublin core for the episode only.
   */
  @Test
  public void testAddFullMediaPackage() throws Exception {
    MediaPackageBuilderFactory builderFactory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder mediaPackageBuilder = builderFactory.newMediaPackageBuilder();
    URL rootUrl = EpisodeServiceImplTest.class.getResource("/");
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(rootUrl));

    // Load the simple media package
    MediaPackage mediaPackage = null;
    InputStream is = null;
    try {
      is = EpisodeServiceImplTest.class.getResourceAsStream("/manifest-full.xml");
      mediaPackage = mediaPackageBuilder.loadFromXml(is);
    } finally {
      IOUtils.closeQuietly(is);
    }

    // Make sure our mocked ACL has the read and write permission
    acl.getEntries().add(
            new AccessControlEntry(userWithPermissions.getRoles()[0], EpisodeService.READ_PERMISSION, true));
    acl.getEntries().add(
            new AccessControlEntry(userWithPermissions.getRoles()[0], EpisodeService.WRITE_PERMISSION, true));

    // Add the media package to the search index
    service.add(mediaPackage);

    // Make sure it's properly indexed and returned
    EpisodeQuery q = new EpisodeQuery();
    q.withId("10.0000/2");
    assertEquals(1, service.find(q).size());
    q.withId(null); // Clear the ID requirement
    assertEquals(1, service.find(q).size());
  }

  @Test
  public void testDelete() throws Exception {
    final MediaPackage mp1 = getMediaPackage("/manifest-simple.xml");
    final MediaPackage mp2 = getMediaPackage("/manifest-full.xml");
    final String mpId = mp1.getIdentifier().toString();
    // Make sure our mocked ACL has the read and write permission
    acl.getEntries().add(
            new AccessControlEntry(userWithPermissions.getRoles()[0], EpisodeService.READ_PERMISSION, true));
    acl.getEntries().add(
            new AccessControlEntry(userWithPermissions.getRoles()[0], EpisodeService.WRITE_PERMISSION, true));
    // add both media packages
    service.add(mp1);
    service.add(mp2);
    assertTrue(service.delete(mpId));
    assertEquals(1L, service.find(new EpisodeQuery().withId(mpId).includeDeleted(true)).getTotalSize());
    assertEquals(2L, service.find(new EpisodeQuery().includeDeleted(true)).getTotalSize());
    assertEquals(1L, service.find(new EpisodeQuery().includeDeleted(false).withDeletedSince(new Date(0L))).getTotalSize());
    assertEquals(1L, service.find(new EpisodeQuery()).getTotalSize());
  }

  /**
   * Test removal from the search index.
   */
  @Test
  public void testDeleteMediaPackage() throws Exception {
    MediaPackage mediaPackage = getMediaPackage("/manifest-simple.xml");

    // Make sure our mocked ACL has the read and write permission
    acl.getEntries().add(
            new AccessControlEntry(userWithPermissions.getRoles()[0], EpisodeService.READ_PERMISSION, true));
    acl.getEntries().add(
            new AccessControlEntry(userWithPermissions.getRoles()[0], EpisodeService.WRITE_PERMISSION, true));

    // Add the media package to the search index
    service.add(mediaPackage);

    // Now take the role away from the user
    userResponder.setResponse(userWithoutPermissions);
    organizationResponder.setResponse(new Organization(DEFAULT_ORGANIZATION_ID, DEFAULT_ORGANIZATION_NAME,
            DEFAULT_BASE_URL, DEFAULT_ORGANIZATION_ADMIN, DEFAULT_ORGANIZATION_ANONYMOUS));

    // Try to delete it
    Assert.assertFalse("Unauthorized user was able to delete a mediapackage",
            service.delete(mediaPackage.getIdentifier().toString()));

    // Second try with a "fixed" roleset
    User adminUser = new User("admin", "opencastproject.org", new String[] { new DefaultOrganization().getAdminRole() });
    userResponder.setResponse(adminUser);
    Date deletedDate = new Date();
    assertTrue(service.delete(mediaPackage.getIdentifier().toString()));

    // Now go back to the original security service and user
    userResponder.setResponse(defaultUser);
    organizationResponder.setResponse(defaultOrganization);

    EpisodeQuery q = new EpisodeQuery();
    q.withId("10.0000/1");
    assertEquals(0, service.find(q).size());
    q.withId(null); // Clear the ID requirement
    assertEquals(0, service.find(q).size());

    q = new EpisodeQuery();
    q.withDeletedSince(deletedDate);
    assertEquals(1, service.find(q).size());
  }

  /**
   * Ads a media package with one dublin core for the episode and one for the series.
   * 
   * todo media package needs to return a series id for this test to work
   */
  @Test
  @Ignore
  public void testAddSeriesMediaPackage() throws Exception {
    MediaPackageBuilderFactory builderFactory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder mediaPackageBuilder = builderFactory.newMediaPackageBuilder();
    URL rootUrl = EpisodeServiceImplTest.class.getResource("/");
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(rootUrl));

    // Load the simple media package
    MediaPackage mediaPackage = null;
    InputStream is = null;
    try {
      is = EpisodeServiceImplTest.class.getResourceAsStream("/manifest-full.xml");
      mediaPackage = mediaPackageBuilder.loadFromXml(is);
    } catch (MediaPackageException e) {
      fail("Error loading full media package");
    } finally {
      IOUtils.closeQuietly(is);
    }

    // Make sure our mocked ACL has the read and write permission
    acl.getEntries().add(
            new AccessControlEntry(userWithPermissions.getRoles()[0], EpisodeService.READ_PERMISSION, true));
    acl.getEntries().add(
            new AccessControlEntry(userWithPermissions.getRoles()[0], EpisodeService.WRITE_PERMISSION, true));

    // Add the media package to the search index
    service.add(mediaPackage);

    // Make sure it's properly indexed and returned
    EpisodeQuery q = new EpisodeQuery();

    SearchResult result = service.find(q);
    assertEquals(1, result.size());
    assertEquals("foobar-serie", result.getItems().get(0).getId());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testPopulateIndex() throws Exception {
    // This service registry must return a list of jobs
    List<String> args = new ArrayList<String>();
    args.add(new DefaultOrganization().getId());

    List<Job> jobs = new ArrayList<Job>();
    for (long i = 0; i < 10; i++) {
      MediaPackage mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
      mediaPackage.setIdentifier(IdBuilderFactory.newInstance().newIdBuilder().createNew());
      episodeDatabase.storeEpisode(mediaPackage, acl, new Date(), Version.version(i + 1));
      String payload = MediaPackageParser.getAsXml(mediaPackage);
      JaxbJob job = new JaxbJob();
      job.setId(i);
      job.setArguments(args);
      job.setPayload(payload);
      job.setStatus(Status.FINISHED);
      jobs.add(job);
    }

    // We should have nothing in the search index
    assertEquals(0, service.find(new EpisodeQuery()).size());

    service.populateIndex();

    // This time we should have 10 results
    assertEquals(10, service.find(new EpisodeQuery()).size());
  }
}
