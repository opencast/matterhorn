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
import static org.opencastproject.remotetest.Main.PASSWORD;
import static org.opencastproject.remotetest.Main.USERNAME;

import org.opencastproject.security.TrustedHttpClientImpl;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/**
 * Tests the functionality of a remote workflow service rest endpoint
 */
public class ComposerRestEndpointTest {
  TrustedHttpClientImpl client;

  @Before
  public void setup() throws Exception {
    client = new TrustedHttpClientImpl(USERNAME, PASSWORD);
  }

  @Test
  public void testEncodeVideoTracks() throws Exception {
    // Start an encoding job via the rest endpoint
    HttpPost postEncode = new HttpPost(BASE_URL + "/composer/rest/encode");
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();
    formParams.add(new BasicNameValuePair("sourceTrack", generateVideoTrack(BASE_URL)));
    formParams.add(new BasicNameValuePair("profileId", "flash.http"));
    postEncode.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));

    // Grab the receipt from the response
    HttpResponse postResponse = client.execute(postEncode);
    Assert.assertEquals(200, postResponse.getStatusLine().getStatusCode());
    String postResponseXml = EntityUtils.toString(postResponse.getEntity());
    String receiptId = getReceiptId(postResponseXml);

    // Poll the service for the status of the receipt.
    String status = null;
    while (status == null || "RUNNING".equals(status) || "QUEUED".equals(status)) {
      Thread.sleep(5000); // wait and try again
      HttpGet pollRequest = new HttpGet(BASE_URL + "/composer/rest/receipt/" + receiptId + ".xml");
      status = getReceiptStatus(client.execute(pollRequest));
      System.out.println("encoding job " + receiptId + " is " + status);
    }
    if (!"FINISHED".equals(status)) {
      Assert.fail("receipt status terminated with status=" + status);
    }
  }

  @Test
  public void testImageExtraction() throws Exception {
    HttpPost postEncode = new HttpPost(BASE_URL + "/composer/rest/image");
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();
    formParams.add(new BasicNameValuePair("sourceTrack", generateVideoTrack(BASE_URL)));
    formParams.add(new BasicNameValuePair("time", "1"));
    formParams.add(new BasicNameValuePair("profileId", "feed-cover.http"));
    postEncode.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));

    // Grab the receipt from the response
    HttpResponse postResponse = client.execute(postEncode);
    String postResponseXml = EntityUtils.toString(postResponse.getEntity());
    Assert.assertEquals(200, postResponse.getStatusLine().getStatusCode());
    Assert.assertTrue(postResponseXml.contains("receipt"));
  }

  @Test
  public void testTrimming() throws Exception {
    HttpPost postEncode = new HttpPost(BASE_URL + "/composer/rest/trim");
    List<NameValuePair> formParams = new ArrayList<NameValuePair>();
    formParams.add(new BasicNameValuePair("sourceTrack", generateVideoTrack(BASE_URL)));
    formParams.add(new BasicNameValuePair("start", "2000"));
    formParams.add(new BasicNameValuePair("duration", "5000"));
    formParams.add(new BasicNameValuePair("profileId", "trim.work"));
    postEncode.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));

    // Grab the receipt from the response
    HttpResponse postResponse = client.execute(postEncode);
    String postResponseXml = EntityUtils.toString(postResponse.getEntity());
    Assert.assertEquals(200, postResponse.getStatusLine().getStatusCode());
    Assert.assertTrue(postResponseXml.contains("receipt"));

    // Poll for the finished composer job
    // Poll the service for the status of the receipt.
    String receiptId = getReceiptId(postResponseXml);
    String status = null;
    while (status == null || "RUNNING".equals(status) || "QUEUED".equals(status)) {
      Thread.sleep(5000); // wait and try again
      HttpGet pollRequest = new HttpGet(BASE_URL + "/composer/rest/receipt/" + receiptId + ".xml");
      status = getReceiptStatus(client.execute(pollRequest));
      System.out.println("encoding job " + receiptId + " is " + status);
    }
    if (!"FINISHED".equals(status)) {
      Assert.fail("receipt status terminated with status=" + status);
    }

    // Get the track xml from the receipt
    HttpGet pollRequest = new HttpGet(BASE_URL + "/composer/rest/receipt/" + receiptId + ".xml");
    HttpResponse pollResponse = client.execute(pollRequest);
    long duration = getDurationFromReceipt(pollResponse);
    Assert.assertTrue(duration < 14546);
  }

  /**
   * Gets the mediapackage element from a receipt polling response
   * 
   * @param pollResponse
   *          the http response
   * @return the mediapackage elemet as an xml string
   */
  protected long getDurationFromReceipt(HttpResponse pollResponse) throws Exception {
    String receiptXml = EntityUtils.toString(pollResponse.getEntity());
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(IOUtils.toInputStream(receiptXml, "UTF-8"));
    Element element = ((Element) XPathFactory.newInstance().newXPath().compile("/track/duration")
            .evaluate(doc, XPathConstants.NODE));
    if (element == null)
      throw new IllegalStateException("Track doesn't contain a duration");

    TransformerFactory transFactory = TransformerFactory.newInstance();
    Transformer transformer = transFactory.newTransformer();
    StringWriter buffer = new StringWriter();
    transformer.transform(new DOMSource(element), new StreamResult(buffer));
    return Long.parseLong(buffer.toString());
  }

  protected String getReceiptId(String xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(IOUtils.toInputStream(xml, "UTF-8"));
    return ((Element) XPathFactory.newInstance().newXPath().compile("/*").evaluate(doc, XPathConstants.NODE))
            .getAttribute("id");
  }

  protected String getReceiptStatus(HttpResponse pollResponse) throws Exception {
    String xml = EntityUtils.toString(pollResponse.getEntity());
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(IOUtils.toInputStream(xml, "UTF-8"));
    String status = ((Element) XPathFactory.newInstance().newXPath().compile("/*").evaluate(doc, XPathConstants.NODE))
            .getAttribute("status");
    return status;
  }

  protected String generateVideoTrack(String serverUrl) {
    return "<track id=\"track-1\" type=\"presentation/source\">\n" + "  <mimetype>video/quicktime</mimetype>\n"
            + "  <url>" + serverUrl + "/workflow/samples/camera.mpg</url>\n"
            + "  <checksum type=\"md5\">43b7d843b02c4a429b2f547a4f230d31</checksum>\n"
            + "  <duration>14546</duration>\n" + "  <video>\n"
            + "    <device type=\"UFG03\" version=\"30112007\" vendor=\"Unigraf\" />\n"
            + "    <encoder type=\"H.264\" version=\"7.4\" vendor=\"Apple Inc\" />\n"
            + "    <resolution>640x480</resolution>\n" + "    <scanType type=\"progressive\" />\n"
            + "    <bitrate>540520</bitrate>\n" + "    <frameRate>2</frameRate>\n" + "  </video>\n" + "</track>";
  }

}
