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
package org.opencastproject.inspection.remote;

import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.mediapackage.AbstractMediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.remote.api.Receipt;
import org.opencastproject.remote.api.RemoteBase;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Proxies a remote media inspection service for use as a JVM-local service.
 */
public class MediaInspectionServiceRemoteImpl extends RemoteBase implements MediaInspectionService {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(MediaInspectionServiceRemoteImpl.class);

  /**
   * Constructs a new remote media inspection service proxy
   */
  public MediaInspectionServiceRemoteImpl() {
    super(JOB_TYPE);
  }
  
  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.inspection.api.MediaInspectionService#inspect(java.net.URI, boolean)
   */
  @Override
  public Receipt inspect(URI uri, boolean block) {
    List<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
    queryStringParams.add(new BasicNameValuePair("uri", uri.toString()));
    String url = "/inspection/rest/inspect?" + URLEncodedUtils.format(queryStringParams, "UTF-8");
    logger.info("Inspecting media file at {} using a remote media inspection service", uri);
    HttpResponse response = null;
    try {
      HttpGet get = new HttpGet(url);
      response = getResponse(get);
      if(response != null) {
        Receipt receipt = remoteServiceManager.parseReceipt(response.getEntity().getContent());
        if (block) {
          receipt = poll(receipt.getId());
        }
        return receipt;
      }
    } catch (Exception e) {
      throw new RuntimeException("Unable to inspect " + uri + " using a remote inspection service");
    } finally {
      closeConnection(response);
    }
    throw new RuntimeException("Unable to inspect " + uri + " using a remote inspection service");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.inspection.api.MediaInspectionService#enrich(org.opencastproject.mediapackage.AbstractMediaPackageElement,
   *      boolean, boolean)
   */
  @Override
  public Receipt enrich(MediaPackageElement original, boolean override, boolean block) {
    String url = "/inspection/rest/enrich";
    List<NameValuePair> params = new ArrayList<NameValuePair>();
    try {
      params.add(new BasicNameValuePair("mediaPackageElement", ((AbstractMediaPackageElement) original).getAsXml()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    params.add(new BasicNameValuePair("override", new Boolean(override).toString()));
    logger.info("Enriching {} using a remote media inspection service", original);
    HttpResponse response = null;
    try {
      HttpPost post = new HttpPost(url);
      HttpEntity entity = new UrlEncodedFormEntity(params);
      post.setEntity(entity);
      response = getResponse(post);
      if(response != null) {
        Receipt receipt = remoteServiceManager.parseReceipt(response.getEntity().getContent());
        if (block) {
          receipt = poll(receipt.getId());
        }
        return receipt;
      }
    } catch (Exception e) {
      throw new RuntimeException("Unable to enrich " + original + " using a remote inspection service", e);
    } finally {
      closeConnection(response);
    }
    throw new RuntimeException("Unable to enrich " + original + " using a remote inspection service");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.inspection.api.MediaInspectionService#getReceipt(java.lang.String)
   */
  @Override
  public Receipt getReceipt(String id) {
    return super.remoteServiceManager.getReceipt(id);
  }

}
