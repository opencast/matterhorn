/**
 *  Copyright 2009 The Regents of the University of California
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
package org.opencastproject.media.mediapackage.attachment;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.media.mediapackage.Attachment;
import org.opencastproject.media.mediapackage.MediaPackageBuilderTest;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.media.mediapackage.UnsupportedElementException;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URI;

/**
 * Test case for the {@link AttachmentImpl} attachment implementation.
 */
public class AttachmentTest {

  /* png test image */
  private File coverFile = null;

  /**
   * Creates everything that is needed to test a media package.
   * 
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    coverFile = new File(MediaPackageBuilderTest.class.getResource("/cover.png").getPath());
    assertTrue(coverFile.exists() && coverFile.canRead());
  }
  
  /**
   * Test method for {@link org.opencastproject.media.mediapackage.attachment.AttachmentImpl#toManifest(org.w3c.dom.Document, org.opencastproject.media.mediapackage.MediaPackageSerializer)}.
   */
  @Test
  public void testToManifest() {
    //fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for {@link org.opencastproject.media.mediapackage.attachment.AttachmentImpl#fromURI(URI)}.
   */
  @Test
  public void testFromURL() {
    MediaPackageElementBuilderFactory factory = MediaPackageElementBuilderFactory.newInstance();
    MediaPackageElementBuilder builder = factory.newElementBuilder();
    MediaPackageElement packageElement = null;
    Attachment attachment = null;
    
    // Create the element
    try {
      packageElement = builder.elementFromURI(coverFile.toURI());
    } catch (UnsupportedElementException e) {
      fail("Attachment is unsupported: " + e.getMessage());
    }
    
    // Type test
    try {
      attachment = (Attachment)packageElement;
    } catch (ClassCastException e) {
      fail("Type mismatch: " + e.getMessage());
    }
    
  }

}