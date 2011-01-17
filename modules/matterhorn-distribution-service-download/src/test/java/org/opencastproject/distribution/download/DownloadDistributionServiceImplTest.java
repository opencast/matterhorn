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
package org.opencastproject.distribution.download;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workspace.api.Workspace;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.net.URI;

public class DownloadDistributionServiceImplTest {
  
  private DownloadDistributionService service = null;
  private MediaPackage mp = null;
  private File distributionRoot = null;
  private ServiceRegistry serviceRegistry = null;

  @Before
  public void setup() throws Exception {
    File mediaPackageRoot = new File("./target/test-classes");
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    builder.setSerializer(new DefaultMediaPackageSerializerImpl(mediaPackageRoot));
    InputStream is = null;
    try {
      is = getClass().getResourceAsStream("/mediapackage.xml");
      mp = builder.loadFromXml(is);
    } finally {
      IOUtils.closeQuietly(is);
    }
    
    distributionRoot = new File("./target/static");
    service = new DownloadDistributionService();

    serviceRegistry = new ServiceRegistryInMemoryImpl(service);
    service.setServiceRegistry(serviceRegistry);
    
    service.distributionDirectory = distributionRoot;
    service.serviceUrl = UrlSupport.DEFAULT_BASE_URL;
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    service.setWorkspace(workspace);
    
    EasyMock.expect(workspace.get((URI)EasyMock.anyObject())).andReturn(new File(mediaPackageRoot, "media.mov"));
    EasyMock.expect(workspace.get((URI)EasyMock.anyObject())).andReturn(new File(mediaPackageRoot, "dublincore.xml"));
    EasyMock.expect(workspace.get((URI)EasyMock.anyObject())).andReturn(new File(mediaPackageRoot, "mpeg7.xml"));
    EasyMock.expect(workspace.get((URI)EasyMock.anyObject())).andReturn(new File(mediaPackageRoot, "attachment.txt"));
    EasyMock.replay(workspace);
  }

  @After
  public void teardown() throws Exception {
    FileUtils.deleteDirectory(distributionRoot);
    ((ServiceRegistryInMemoryImpl)serviceRegistry).dispose();
  }
  
  @Test
  public void testPartialDistribution() throws Exception {
    // Distribute only some of the elements in the mediapackage
    Job job1 = service.distribute(mp.getIdentifier().compact(), mp.getTrack("track-1")); // "catalog-2" and "notes" are not to be distributed
    Job job2 = service.distribute(mp.getIdentifier().compact(), mp.getCatalog("catalog-1")); // "catalog-2" and "notes" are not to be distributed
    JobBarrier jobBarrier = new JobBarrier(serviceRegistry, 500, job1, job2);
    jobBarrier.waitForJobs();

    File mpDir = new File(distributionRoot, mp.getIdentifier().compact());
    Assert.assertTrue(mpDir.exists());
    File mediaDir = new File(mpDir, "track-1");
    File metadataDir = new File(mpDir, "catalog-1");
    File attachmentsDir = new File(mpDir, "notes");
    Assert.assertTrue(mediaDir.exists());
    Assert.assertTrue(metadataDir.exists());
    Assert.assertFalse(attachmentsDir.exists());
    Assert.assertTrue(new File(mediaDir, "media.mov").exists()); // the filenames are changed to reflect the element ID
    Assert.assertTrue(new File(metadataDir, "dublincore.xml").exists());
    Assert.assertFalse(new File(metadataDir, "mpeg7.xml").exists());
    Assert.assertFalse(new File(attachmentsDir, "attachment.txt").exists());
  }

  @Test
  public void testRetract() throws Exception {
    // Distribute the mediapackage and all of its elements
    Job job1 = service.distribute(mp.getIdentifier().compact(), mp.getTrack("track-1"));
    Job job2 = service.distribute(mp.getIdentifier().compact(), mp.getCatalog("catalog-1"));
    Job job3 = service.distribute(mp.getIdentifier().compact(), mp.getCatalog("catalog-2"));
    Job job4 = service.distribute(mp.getIdentifier().compact(), mp.getAttachment("notes"));
    JobBarrier jobBarrier = new JobBarrier(serviceRegistry, 500, job1, job2, job3, job4);
    jobBarrier.waitForJobs();

    File mpDir = new File(distributionRoot, mp.getIdentifier().compact());
    File mediaDir = new File(mpDir, "track-1");
    File metadata1Dir = new File(mpDir, "catalog-1");
    File metadata2Dir = new File(mpDir, "catalog-2");
    File attachmentsDir = new File(mpDir, "notes");
    Assert.assertTrue(mediaDir.exists());
    Assert.assertTrue(metadata1Dir.exists());
    Assert.assertTrue(metadata2Dir.exists());
    Assert.assertTrue(attachmentsDir.exists());
    Assert.assertTrue(new File(mediaDir, "media.mov").exists()); // the filenames are changed to reflect the element ID
    Assert.assertTrue(new File(metadata1Dir, "dublincore.xml").exists());
    Assert.assertTrue(new File(metadata2Dir, "mpeg7.xml").exists());
    Assert.assertTrue(new File(attachmentsDir, "attachment.txt").exists());

    // Now retract the mediapackage and ensure that the distributed files have been removed
    Job job5 = service.retract(mp.getIdentifier().compact());
    jobBarrier = new JobBarrier(serviceRegistry, 500, job5);
    jobBarrier.waitForJobs();
    
    Assert.assertFalse(service.getDistributionFile(mp.getIdentifier().compact(), mp.getElementById("track-1")).isFile());
    Assert.assertFalse(service.getDistributionFile(mp.getIdentifier().compact(), mp.getElementById("catalog-1")).isFile());
    Assert.assertFalse(service.getDistributionFile(mp.getIdentifier().compact(), mp.getElementById("catalog-2")).isFile());
    Assert.assertFalse(service.getDistributionFile(mp.getIdentifier().compact(), mp.getElementById("notes")).isFile());
  }

}
