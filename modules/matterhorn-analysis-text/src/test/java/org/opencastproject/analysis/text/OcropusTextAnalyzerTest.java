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
package org.opencastproject.analysis.text;

import static org.junit.Assert.assertTrue;

import static org.junit.Assert.assertEquals;

import org.opencastproject.analysis.text.ocropus.OcropusTextAnalyzer;
import org.opencastproject.analysis.text.ocropus.OcropusTextFrame;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;

/**
 * Test case for class {@link OcropusTextAnalyzer}.
 */
public class OcropusTextAnalyzerTest {

  /** Path to the test image */
  protected String testPath = "/image.jpg";

  /** Test image */
  protected File testFile = null;

  /** Path to the ocropus binary */
  protected String ocropusbinary = "/usr/local/bin/ocrocmd";

  /** The ocropus text analyzer */
  protected OcropusTextAnalyzer analyzer = null;
  
  /** The text without punctuation */
  protected String text = "Land and Vegetation Key players on the";

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    URL imageUrl = this.getClass().getResource(testPath);
    testFile = File.createTempFile("ocrtest", ".jpg");
    FileUtils.copyURLToFile(imageUrl, testFile);
    analyzer = new OcropusTextAnalyzer(ocropusbinary);
  }

  /**
   * Test method for {@link org.opencastproject.analysis.text.ocropus.OcropusTextAnalyzer#getBinary()}.
   */
  @Test
  public void testGetBinary() {
    assertEquals(ocropusbinary, analyzer.getBinary());
  }

  /**
   * Test method for {@link org.opencastproject.analysis.text.ocropus.OcropusTextAnalyzer#analyze(java.io.File)}.
   */
  @Test
  public void testAnalyze() {
    if (!new File(ocropusbinary).exists())
      return;
    OcropusTextFrame frame = analyzer.analyze(testFile);
    assertTrue(frame.hasText());
    assertEquals(text, frame.getLines()[0].getText());
  }

}
