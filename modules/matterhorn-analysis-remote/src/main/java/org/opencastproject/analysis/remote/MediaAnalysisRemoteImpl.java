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
package org.opencastproject.analysis.remote;

import org.opencastproject.analysis.api.MediaAnalysisException;
import org.opencastproject.analysis.api.MediaAnalysisService;
import org.opencastproject.analysis.api.MediaAnalysisServiceSupport;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.remote.api.Receipt;
import org.opencastproject.remote.api.ReceiptService;
import org.opencastproject.remote.api.Receipt.Status;
import org.opencastproject.security.api.TrustedHttpClient;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class MediaAnalysisRemoteImpl extends MediaAnalysisServiceSupport implements MediaAnalysisService {
  private static final Logger logger = LoggerFactory.getLogger(MediaAnalysisRemoteImpl.class);

  public static final String REMOTE_SERVICE_KEY = "remote.analysis";

  protected ResponseHandler<Receipt> receiptResponseHandler = new ReceiptResponseHandler();
  protected String remoteHost;
  protected String analysisType;
  
  protected ReceiptService receiptService;
  protected TrustedHttpClient trustedHttpClient;

  public void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    this.trustedHttpClient = trustedHttpClient;
  }

  public void setRemoteHost(String remoteHost) {
    this.remoteHost = remoteHost;
  }

  public void setReceiptService(ReceiptService receiptService) {
    this.receiptService = receiptService;
  }

  public void activate(ComponentContext cc) {
    this.remoteHost = cc.getBundleContext().getProperty(REMOTE_SERVICE_KEY);
    this.analysisType = (String)cc.getProperties().get("analysis.type");
    super.resultingFlavor = MediaPackageElementFlavor.parseFlavor((String)cc.getProperties().get("resulting.flavor"));
    Object requiredFlavorsObj = cc.getProperties().get("required.flavors");
    if(requiredFlavorsObj != null) {
      if(requiredFlavorsObj instanceof String) {
        super.requiredFlavors = new MediaPackageElementFlavor[1];
        super.requiredFlavors[0] = MediaPackageElementFlavor.parseFlavor((String)requiredFlavorsObj);      
      } else if(requiredFlavorsObj instanceof String[]) {
        String[] flavorStrings = (String[])requiredFlavorsObj;
        super.requiredFlavors = new MediaPackageElementFlavor[flavorStrings.length];
        for(int i=0; i<flavorStrings.length;i++) {
          super.requiredFlavors[i] = MediaPackageElementFlavor.parseFlavor(flavorStrings[i]);
        }
      }
    }
    super.resultingFlavor = MediaPackageElementFlavor.parseFlavor((String)cc.getProperties().get("resulting.flavor"));
  }

  @Override
  public Receipt analyze(MediaPackageElement element, boolean block) throws MediaAnalysisException {
    logger.info("Analyzing a MediaPackageElement(" + element.getIdentifier() + ") on a remote server: " + remoteHost);
    String remoteHostMethod = remoteHost + "/analysis/rest/" + analysisType;
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    UrlEncodedFormEntity entity;
    try {
      params.add(new BasicNameValuePair("track", getXML(element)));
      entity = new UrlEncodedFormEntity(params);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    HttpPost post = new HttpPost(remoteHostMethod);
    post.setEntity(entity);
    Receipt receipt = trustedHttpClient.execute(post, receiptResponseHandler);
    if(block) {
      receipt = poll(receipt.getId());
    }
    return receipt;
  }


  /**
   * Polls for receipts until they return a status of {@link Status#FINISHED} or {@link Status#FAILED}
   * 
   * @param r
   *          The receipt id
   * @return The receipt
   */
  private Receipt poll(String id) {
    Receipt r = getReceipt(id);
    while (r.getStatus() != Status.FAILED && r.getStatus() != Status.FINISHED) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        logger.warn("polling interrupted");
      }
      r = getReceipt(id);
    }
    return r;
  }

  public String getXML(MediaPackageElement element) throws Exception {
    if (element == null)
      return null;
    DocumentBuilder docBuilder;
    docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = docBuilder.newDocument();
    Node node = element.toManifest(doc, null);
    DOMSource domSource = new DOMSource(node);
    StringWriter writer = new StringWriter();
    StreamResult result = new StreamResult(writer);
    Transformer transformer;
    transformer = TransformerFactory.newInstance().newTransformer();
    transformer.transform(domSource, result);
    return writer.toString();
  }

  @Override
  public Receipt getReceipt(String id) {
    logger.info("Returning a Receipt(" + id + ") from a remote server: " + remoteHost);
    String url = remoteHost + "/analyze/rest/" + analysisType + "/" + id + ".xml";
    HttpGet get = new HttpGet(url);
    try {
      return trustedHttpClient.execute(get, receiptResponseHandler);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  class ReceiptResponseHandler implements ResponseHandler<Receipt> {
    public Receipt handleResponse(final HttpResponse response) throws HttpResponseException, IOException {
      StatusLine statusLine = response.getStatusLine();
      if (statusLine.getStatusCode() == 404) {
        return null;
      } else if (statusLine.getStatusCode() >= 300) {
        throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
      }
      HttpEntity entity = response.getEntity();
      try {
        return entity == null ? null : receiptService.parseReceipt(entity.getContent());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

}
