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
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.remote.api.Receipt;
import org.opencastproject.remote.api.RemoteBase;

import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class MediaAnalysisRemoteImpl extends RemoteBase implements MediaAnalysisService {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(MediaAnalysisRemoteImpl.class);

  /** The type of analysis this remote service proxy handles */
  protected String analysisType = null;
  
  /** The catalog flavor that is produced by this implementation */
  protected MediaPackageElementFlavor resultingFlavor = null;

  /** The flavors that are required by this media analysis */
  protected MediaPackageElementFlavor[] requiredFlavors = new MediaPackageElementFlavor[] {};

  public MediaAnalysisRemoteImpl() {
    // the service type is not available at construction time.  we need to wait for activation to set this value
    super("waiting for activation");
  }
  
  /** activate the component */
  public void activate(ComponentContext cc) {
    this.analysisType = (String)cc.getProperties().get("analysis.type");
    this.resultingFlavor = MediaPackageElementFlavor.parseFlavor((String)cc.getProperties().get("resulting.flavor"));
    Object requiredFlavorsObj = cc.getProperties().get("required.flavors");
    if(requiredFlavorsObj != null) {
      if(requiredFlavorsObj instanceof String) {
        this.requiredFlavors = new MediaPackageElementFlavor[1];
        this.requiredFlavors[0] = MediaPackageElementFlavor.parseFlavor((String)requiredFlavorsObj);      
      } else if(requiredFlavorsObj instanceof String[]) {
        String[] flavorStrings = (String[])requiredFlavorsObj;
        this.requiredFlavors = new MediaPackageElementFlavor[flavorStrings.length];
        for(int i=0; i<flavorStrings.length;i++) {
          this.requiredFlavors[i] = MediaPackageElementFlavor.parseFlavor(flavorStrings[i]);
        }
      }
    }
    this.resultingFlavor = MediaPackageElementFlavor.parseFlavor((String)cc.getProperties().get("resulting.flavor"));
    super.serviceType = analysisType;
  }

  @Override
  public Receipt analyze(MediaPackageElement element, boolean block) throws MediaAnalysisException {
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    UrlEncodedFormEntity entity;
    try {
      params.add(new BasicNameValuePair("track", getXML(element)));
      entity = new UrlEncodedFormEntity(params);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    Receipt receipt = null;
    logger.info("Analyzing {} on a remote analysis server", element);
    String remoteHostMethod = "/analysis/rest/" + analysisType;
    HttpPost post = new HttpPost(remoteHostMethod);
    post.setEntity(entity);
    HttpResponse response = null;
    try {
      response = getResponse(post);
      if(response == null) {
        throw new MediaAnalysisException("Unable to analyze element '" + element + "' using a remote analysis service");
      }
      try {
        receipt = remoteServiceManager.parseReceipt(response.getEntity().getContent());
        if(block) {
          receipt = poll(receipt.getId());
        }
        return receipt;
      } catch (Exception e) {
        throw new MediaAnalysisException("Unable to analyze element '" + element + "' using a remote analysis service", e);
      }
    } finally {
      closeConnection(response);
    }
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

  /**
   * {@inheritDoc}
   * @see org.opencastproject.remote.api.RemoteBase#getReceipt(java.lang.String)
   */
  @Override
  public Receipt getReceipt(String id) {
    return super.getReceipt(id);
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.analysis.api.MediaAnalysisService#getAnalysisType()
   */
  @Override
  public String getAnalysisType() {
    return analysisType;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.analysis.api.MediaAnalysisService#produces()
   */
  @Override
  public MediaPackageElementFlavor produces() {
    return resultingFlavor;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.analysis.api.MediaAnalysisService#requires()
   */
  @Override
  public MediaPackageElementFlavor[] requires() {
    return requiredFlavors;
  }

}
