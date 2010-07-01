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
package org.opencastproject.search.remote;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.remote.api.RemoteBase;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultImpl;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.util.UrlSupport;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A proxy to a remote search service.
 */
public class SearchServiceRemoteImpl extends RemoteBase implements SearchService {
  private static final Logger logger = LoggerFactory.getLogger(SearchServiceRemoteImpl.class);

  public SearchServiceRemoteImpl() {
    super(JOB_TYPE);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.api.SearchService#add(org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public void add(MediaPackage mediaPackage) throws SearchException {
    String urlSuffix = UrlSupport.concat(new String[] { "/search", "rest", "add" });
    HttpPost post = new HttpPost(urlSuffix);
    HttpResponse response = getResponse(post, HttpStatus.SC_NO_CONTENT);
    if (response == null) {
      throw new SearchException("Unable to add mediapackage " + mediaPackage + " using the remote search services");
    } else {
      closeConnection(response);
    }
    logger.info("Successfully added {} to the search service", mediaPackage);
    return;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.api.SearchService#clear()
   */
  @Override
  public void clear() throws SearchException {
    HttpResponse response = null;
    try {
      HttpPost post = new HttpPost("/search/rest/clear");
      response = getResponse(post, HttpStatus.SC_NO_CONTENT);
      if (response == null)
        throw new SearchException("Unable to clear remote search index");
      StatusLine status = response.getStatusLine();
      if (status.getStatusCode() == HttpStatus.SC_NO_CONTENT) {
        logger.info("Successfully cleared remote search index");
      } else {
        throw new SearchException("Unable to clear remote search index, http status = " + status);
      }
    } finally {
      closeConnection(response);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.api.SearchService#delete(java.lang.String)
   */
  @Override
  public void delete(String mediaPackageId) throws SearchException {
    String urlprefix = "/search/rest/" + mediaPackageId;
    HttpDelete del = new HttpDelete(urlprefix);
    HttpResponse response = null;
    try {
      response = getResponse(del, HttpStatus.SC_NO_CONTENT);
      if (response == null) {
        throw new SearchException("Unable to remove " + mediaPackageId + " from a remote search index");
      }
      int status = response.getStatusLine().getStatusCode();
      if (status == HttpStatus.SC_NO_CONTENT) {
        logger.info("Successfully deleted {} from the remote search index", mediaPackageId);
      } else {
        throw new SearchException("Unable to remove " + mediaPackageId + " from a remote search index, http status="
                + status);
      }
    } finally {
      closeConnection(response);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.api.SearchService#getByQuery(org.opencastproject.search.api.SearchQuery)
   */
  @Override
  public SearchResult getByQuery(SearchQuery q) throws SearchException {
    StringBuilder url = new StringBuilder();
    List<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
    if (q.getText() != null) {
      queryStringParams.add(new BasicNameValuePair("q", q.getText()));
    }
    queryStringParams.add(new BasicNameValuePair("limit", Integer.toString(q.getLimit())));
    queryStringParams.add(new BasicNameValuePair("offset", Integer.toString(q.getOffset())));
    if (!q.isIncludeEpisodes() && q.isIncludeSeries()) {
      url.append("/search/rest/series?");
    } else if (q.isIncludeEpisodes() && !q.isIncludeSeries()) {
      url.append("/search/rest/episode?");
    } else {
      url.append("/search/rest/?");
    }
    url.append(URLEncodedUtils.format(queryStringParams, "UTF-8"));
    HttpGet get = new HttpGet(url.toString());
    HttpResponse response = null;
    try {
      response = getResponse(get);
      if (response != null) {
        return SearchResultImpl.valueOf(response.getEntity().getContent());
      }
    } catch (Exception e) {
      throw new SearchException("Unable to parse results of a getByQuery request from remote search index: ", e);
    } finally {
      closeConnection(response);
    }
    throw new SearchException("Unable to perform getByQuery from remote search index");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.api.SearchService#getByQuery(java.lang.String, int, int)
   */
  @Override
  public SearchResult getByQuery(String query, int limit, int offset) throws SearchException {
    List<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
    queryStringParams.add(new BasicNameValuePair("q", query));
    queryStringParams.add(new BasicNameValuePair("limit", Integer.toString(limit)));
    queryStringParams.add(new BasicNameValuePair("offset", Integer.toString(offset)));

    StringBuilder url = new StringBuilder();
    url.append("/search/rest/lucene?");
    url.append(URLEncodedUtils.format(queryStringParams, "UTF-8"));

    HttpGet get = new HttpGet(url.toString());
    logger.debug("Sending remote query '{}'", get.getRequestLine().toString());
    HttpResponse response = null;
    try {
      response = getResponse(get);
      if (response != null) {
        return SearchResultImpl.valueOf(response.getEntity().getContent());
      }
    } catch (Exception e) {
      throw new SearchException("Unable to parse getByQuery response from remote search index", e);
    } finally {
      closeConnection(response);
    }
    throw new SearchException("Unable to perform getByQuery from remote search index");
  }
}
