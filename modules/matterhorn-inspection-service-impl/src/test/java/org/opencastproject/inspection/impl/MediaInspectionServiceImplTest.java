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
package org.opencastproject.inspection.impl;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.StreamHelper;
import org.opencastproject.workspace.api.Workspace;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;

public class MediaInspectionServiceImplTest {
  
  private MediaInspectionServiceImpl service = null;
  private Workspace workspace = null;
  private ServiceRegistry serviceRegistry = null;

  private URI uriTrack;

  private static final Logger logger = LoggerFactory.getLogger(MediaInspectionServiceImplTest.class);

  /** True to run the tests */
  private static boolean mediainfoInstalled = true;
  
  @BeforeClass
  public static void setupClass() {
    StreamHelper stdout = null;
    StreamHelper stderr = null;
    Process p = null;
    try {
      // Mediainfo requires a track in order to return a status code of 0, indicating that it is workinng as expected
      URI uriTrack = MediaInspectionServiceImpl.class.getResource("/av.mov").toURI();
      File f = new File(uriTrack);
      p = new ProcessBuilder(MediaInfoAnalyzer.MEDIAINFO_BINARY_DEFAULT, f.getAbsolutePath()).start();
      stdout = new StreamHelper(p.getInputStream());
      stderr = new StreamHelper(p.getErrorStream());
      if (p.waitFor() != 0)
        throw new IllegalStateException();
    } catch (Throwable t) {
      logger.warn("Skipping media inspection tests due to unsatisifed mediainfo installation");
      mediainfoInstalled = false;
    } finally {
      IoSupport.closeQuietly(stdout);
      IoSupport.closeQuietly(stderr);
      IoSupport.closeQuietly(p);
    }
  }

  @Before
  public void setup() throws Exception {
    uriTrack = MediaInspectionServiceImpl.class.getResource("/av.mov").toURI();
    File f = new File(uriTrack);
    // set up services and mock objects
    service = new MediaInspectionServiceImpl();
    serviceRegistry = new ServiceRegistryInMemoryImpl(service);

    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(uriTrack)).andReturn(f);
    EasyMock.expect(workspace.get(uriTrack)).andReturn(f);
    EasyMock.expect(workspace.get(uriTrack)).andReturn(f);
    EasyMock.replay(workspace);
    service.setWorkspace(workspace);

    service.setServiceRegistry(serviceRegistry);
  }

  @After
  public void tearDown() throws Exception {
    ((ServiceRegistryInMemoryImpl)serviceRegistry).dispose();
  }

  @Test
  public void testInspection() throws Exception {
    if (!mediainfoInstalled)
      return;
    
    try {
      Job job = service.inspect(uriTrack);
      JobBarrier barrier = new JobBarrier(serviceRegistry, 1000, job);
      barrier.waitForJobs();
      
      Track track = (Track) MediaPackageElementParser.getFromXml(job.getPayload());
      // test the returned values
      Checksum cs = Checksum.create(ChecksumType.fromString("md5"), "9d3523e464f18ad51f59564acde4b95a");
      Assert.assertEquals(track.getChecksum(), cs);
      Assert.assertEquals(track.getMimeType().getType(), "video");
      Assert.assertEquals(track.getMimeType().getSubtype(), "quicktime");
      Assert.assertEquals(track.getDuration(), 14546);
    } catch (IllegalStateException e) {
      System.err.println("Skipped MediaInspectionServiceImplTest#testInspection");
    }
  }

  @Test
  public void testEnrichment() throws Exception {
    if (!mediainfoInstalled)
      return;

    try {
      // init a track with inspect
      Job job = service.inspect(uriTrack);
      JobBarrier barrier = new JobBarrier(serviceRegistry, 1000, job);
      barrier.waitForJobs();

      Track track = (Track) MediaPackageElementParser.getFromXml(job.getPayload());
      // make changes to metadata
      Checksum cs = track.getChecksum();
      track.setChecksum(null);
      MimeType mt = new MimeType("video", "flash");
      track.setMimeType(mt);
      // test the enrich scenario
      Job newJob = service.enrich(track, false);
      barrier = new JobBarrier(serviceRegistry, newJob);
      barrier.waitForJobs();

      Track newTrack = (Track) MediaPackageElementParser.getFromXml(newJob.getPayload());
      Assert.assertEquals(newTrack.getChecksum(), cs);
      Assert.assertEquals(newTrack.getMimeType(), mt);
      Assert.assertEquals(newTrack.getDuration(), 14546);
      // test the override scenario
      newJob = service.enrich(track, true);
      barrier = new JobBarrier(serviceRegistry, newJob);
      barrier.waitForJobs();

      newTrack = (Track) MediaPackageElementParser.getFromXml(newJob.getPayload());
      Assert.assertEquals(newTrack.getChecksum(), cs);
      Assert.assertNotSame(newTrack.getMimeType(), mt);
      Assert.assertEquals(newTrack.getDuration(), 14546);
    } catch (IllegalStateException e) {
      System.err.println("Skipped MediaInspectionServiceImplTest#testInspection");
    }
  }

}
