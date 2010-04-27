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

import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.receipt.api.Receipt;
import org.opencastproject.receipt.api.ReceiptService;
import org.opencastproject.security.api.TrustedHttpClient;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Proxies a remote media inspection service for use as a JVM-local service.
 */
public class MediaInspectionServiceRemoteImpl implements MediaInspectionService {
  private static final Logger logger = LoggerFactory.getLogger(MediaInspectionServiceRemoteImpl.class);

  public static final String REMOTE_MEDIA_INSPECTION = "remote.mediainspection";
  protected ResponseHandler<Receipt> receiptResponseHandler = new ReceiptResponseHandler();
  protected String remoteHost;
  protected ReceiptService receiptService;
  protected TrustedHttpClient trustedHttpClient;

  public void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    this.trustedHttpClient = trustedHttpClient;
  }

  public void setRemoteHost(String remoteHost) {
    this.remoteHost = remoteHost;
  }

  public MediaInspectionServiceRemoteImpl() {
  }

  public MediaInspectionServiceRemoteImpl(String remoteHost) {
    this.remoteHost = remoteHost;
  }

  public void setReceiptService(ReceiptService receiptService) {
    this.receiptService = receiptService;
  }

  public void activate(ComponentContext cc) {
    this.remoteHost = cc.getBundleContext().getProperty(REMOTE_MEDIA_INSPECTION);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.inspection.api.MediaInspectionService#inspect(java.net.URI)
   */
  @Override
  public Receipt inspect(URI uri, boolean block) {
    logger.info("Inspecting a Track(" + uri + ") on a remote server: " + remoteHost);
    List<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
    queryStringParams.add(new BasicNameValuePair("uri", uri.toString()));

    String url = remoteHost + "/inspection/rest/inspect?" + URLEncodedUtils.format(queryStringParams, "UTF-8");
    HttpGet get = new HttpGet(url);
    try {
      return trustedHttpClient.execute(get, receiptResponseHandler);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Receipt enrich(MediaPackageElement original, boolean override, boolean block) {
    // TODO make enrich method available in rest endpoint
    return null;
  }

  // @Override
  // public Track enrich(Track originalTrack, Boolean override) {
  // URI uri = originalTrack.getURI();
  // logger.info("Enriching a Track(" + uri + ") on a remote server: " + remoteHost);
  // List<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
  //    
  // queryStringParams.add(new BasicNameValuePair("track", originalTrack));
  // queryStringParams.add(new BasicNameValuePair("override", uri.toString()));
  //
  // String url = remoteHost + "/inspection/rest/inspect/" + URLEncodedUtils.format(queryStringParams, "UTF-8");
  // HttpGet get = new HttpGet(url);
  // try {
  // return trustedHttpClient.execute(get, trackResponseHandler);
  // } catch (Exception e) {
  // throw new RuntimeException(e);
  // }
  // }

  @Override
  public Receipt getReceipt(String id) {
    logger.info("Returning a Receipt(" + id + ") from a remote server: " + remoteHost);
    String url = remoteHost + "/inspection/rest/receipt/" + id + ".xml";
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
