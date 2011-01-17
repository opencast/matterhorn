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
import org.opencastproject.remotetest.util.JobUtils;
import org.opencastproject.remotetest.util.SampleUtils;
import org.opencastproject.remotetest.util.TrustedHttpClient;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/**
 * Tests the functionality of a remote workflow service rest endpoint
 */
public class ComposerRestEndpointTest {
  TrustedHttpClient client;

  private static final Logger logger = LoggerFactory.getLogger(ComposerRestEndpointTest.class);

  @BeforeClass
  public static void setupClass() throws Exception {
    logger.info("Running " + ComposerRestEndpointTest.class.getName());
  }

  @Before
  public void setup() throws Exception {
    client = Main.getClient();
  }

  @After
  public void tearDown() throws Exception {
    Main.returnClient(client);
  }

  @Test
  public void testEncodeVideoTracks() throws Exception {
    // Start an encoding job via the rest endpoint
    HttpPost postEncode = new HttpPost(BASE_URL + "/composer/rest/encode");
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();
    formParams.add(new BasicNameValuePair("sourceTrack", SampleUtils.generateVideoTrack(BASE_URL)));
    formParams.add(new BasicNameValuePair("profileId", "flash.http"));
    postEncode.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));

    // Grab the job from the response
    HttpResponse postResponse = client.execute(postEncode);
    Assert.assertEquals(200, postResponse.getStatusLine().getStatusCode());
    String postResponseXml = EntityUtils.toString(postResponse.getEntity());
    String jobId = getJobId(postResponseXml);

    // Poll the service for the status of the job.
    while (!JobUtils.isJobInState(jobId, "FINISHED")) {
      Thread.sleep(2000); // wait and try again
      System.out.println("Waiting for encoding job " + jobId + " to finish");
      if(JobUtils.isJobInState(jobId, "FAILED")) {
        Assert.fail();
      }
    }
  }

  @Test
  public void testImageExtraction() throws Exception {
    HttpPost postEncode = new HttpPost(BASE_URL + "/composer/rest/image");
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();
    formParams.add(new BasicNameValuePair("sourceTrack", SampleUtils.generateVideoTrack(BASE_URL)));
    formParams.add(new BasicNameValuePair("time", "1"));
    formParams.add(new BasicNameValuePair("profileId", "feed-cover.http"));
    postEncode.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));

    // Grab the job from the response
    HttpResponse postResponse = client.execute(postEncode);
    String postResponseXml = EntityUtils.toString(postResponse.getEntity());
    Assert.assertEquals(200, postResponse.getStatusLine().getStatusCode());
    Assert.assertTrue(postResponseXml.contains("job"));
  }

  @Test
  public void testTrimming() throws Exception {
    HttpPost postEncode = new HttpPost(BASE_URL + "/composer/rest/trim");
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();
    formParams.add(new BasicNameValuePair("sourceTrack", SampleUtils.generateVideoTrack(BASE_URL)));
    formParams.add(new BasicNameValuePair("start", "2000"));
    formParams.add(new BasicNameValuePair("duration", "5000"));
    formParams.add(new BasicNameValuePair("profileId", "trim.work"));
    postEncode.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));

    // Grab the job from the response
    HttpResponse postResponse = client.execute(postEncode);
    String postResponseXml = EntityUtils.toString(postResponse.getEntity());
    Assert.assertEquals(200, postResponse.getStatusLine().getStatusCode());
    Assert.assertTrue(postResponseXml.contains("job"));

    // Poll for the finished composer job
    // Poll the service for the status of the job.
    String jobId = getJobId(postResponseXml);
    while (!JobUtils.isJobInState(jobId, "FINISHED")) {
      Thread.sleep(2000); // wait and try again
      System.out.println("Waiting for encoding job " + jobId + " to finish");
      if(JobUtils.isJobInState(jobId, "FAILED")) {
        Assert.fail();
      }

    }

    // Get the track xml from the job
    HttpGet pollRequest = new HttpGet(BASE_URL + "/composer/rest/job/" + jobId + ".xml");
    HttpResponse pollResponse = client.execute(pollRequest);
    long duration = getDurationFromJob(pollResponse);
    Assert.assertTrue(duration < 14546);
  }

  /**
   * Gets the mediapackage element from a job polling response
   * 
   * @param pollResponse
   *          the http response
   * @return the mediapackage elemet as an xml string
   */
  protected long getDurationFromJob(HttpResponse pollResponse) throws Exception {
    String jobXml = EntityUtils.toString(pollResponse.getEntity());
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(IOUtils.toInputStream(jobXml, "UTF-8"));
    String payload = (String) XPathFactory.newInstance().newXPath().compile("//payload")
            .evaluate(doc, XPathConstants.STRING);
    Document payloadDoc = builder.parse(IOUtils.toInputStream(payload, "UTF-8"));
    Element element = ((Element) XPathFactory.newInstance().newXPath().compile("//duration[1]")
            .evaluate(payloadDoc, XPathConstants.NODE));
    if (element == null)
      throw new IllegalStateException("Track doesn't contain a duration");

    return Long.parseLong(element.getFirstChild().getNodeValue());
  }

  protected String getJobId(String xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(IOUtils.toInputStream(xml, "UTF-8"));
    return ((Element) XPathFactory.newInstance().newXPath().compile("/*").evaluate(doc, XPathConstants.NODE))
            .getAttribute("id");
  }

}
