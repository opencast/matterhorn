package org.opencastproject.composer.impl;


import org.opencastproject.composer.api.Receipt;
import org.opencastproject.composer.api.Receipt.Status;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.Track;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URI;

import javax.ws.rs.WebApplicationException;

/**
 * This test makes assumptions about a running application at localhost:8080, with network access to svn.  It is
 * therefore @Ignored by default.
 */
@Ignore
public class ComposerRemoteServiceTest {
  ComposerServiceRemoteImpl service;
  
  @Before
  public void setUp() throws Exception {
    service = new ComposerServiceRemoteImpl("http://localhost:8080");
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testCount() throws Exception {
    try {
      Assert.assertTrue(service.countJobs(Status.FINISHED) > -1);
    } catch(WebApplicationException e) {
      e.printStackTrace();
      Assert.fail();
    }
  }
  
  @Test
  public void testProfiles() throws Exception {
    Assert.assertTrue(service.listProfiles().length > 0);
  }
  
  @Test
  public void testReceipts() throws Exception {
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    Track track = (Track)MediaPackageElementBuilderFactory.newInstance().newElementBuilder().elementFromURI(
            new URI("https://opencast.jira.com/svn/MH/trunk/modules/matterhorn-media/src/test/resources/av.mov"),
            Track.TYPE, MediaPackageElements.PRESENTER_SOURCE);
    mp.add(track);
    Receipt r = service.encode(mp, track.getIdentifier(), "m4a.http");
    Assert.assertNotNull(service.getReceipt(r.getId()));
    Assert.assertNull(service.getReceipt("badId"));
  }
  
  
}
