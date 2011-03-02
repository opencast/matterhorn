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
package org.opencastproject.remotetest.server;

import static org.opencastproject.remotetest.Main.BASE_URL;

import org.opencastproject.remotetest.Main;
import org.opencastproject.remotetest.util.TrustedHttpClient;
import org.opencastproject.remotetest.util.Utils;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPathConstants;

/**
 * Tests the functionality of the local distribution service rest endpoint
 */
public class DistributionDownloadRestEndpointTest {
  TrustedHttpClient client;

  private static final Logger logger = LoggerFactory.getLogger(DistributionDownloadRestEndpointTest.class);

  @BeforeClass
  public static void setupClass() throws Exception {
    logger.info("Running " + DistributionDownloadRestEndpointTest.class.getName());
  }

  @Before
  public void setup() throws Exception {
    client = Main.getClient();
  }

  @After
  public void teardown() throws Exception {
    Main.returnClient(client);
  }

  @Test
  public void testDocs() throws Exception {
    HttpGet get = new HttpGet(BASE_URL + "/distribution/download/docs");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testWadl() throws Exception {
    HttpGet get = new HttpGet(BASE_URL + "/distribution/download/?_wadl");
    HttpResponse response = client.execute(get);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
  }

  @Test
  public void testDistribute() throws Exception {
    Document mp = getSampleMediaPackage();
    String mpId = (String)Utils.xpath(mp, "/oc:mediapackage/@id", XPathConstants.STRING);
    String track = Utils.nodeToString((Node)Utils.xpath(mp, "//media/track[@id=\"track-1\"]", XPathConstants.NODE));
    
    HttpPost post = new HttpPost(BASE_URL + "/distribution/download");
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();
    formParams.add(new BasicNameValuePair("mediapackageId", mpId));
    formParams.add(new BasicNameValuePair("element", track));
    post.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));

    // Ensure we get a 200 OK (we don't really care about the body of the result)
    HttpResponse response = client.execute(post);
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    
    // Ideally, we would check to see that the files have been copied to the right location, too, but we can't assume
    // that the server is local.  So we'll just have to trust that our unit tests cover that.
  }

  protected Document getSampleMediaPackage() throws Exception {
    String template = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("mediapackage-1.xml"), "UTF-8");
    template = template.replaceAll("@SAMPLES_URL@", BASE_URL + "/workflow/samples");
    return Utils.parseXml(IOUtils.toInputStream(template, "UTF-8"));
  }

}
