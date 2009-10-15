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

package org.opencastproject.workflow.impl.solr;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BinaryResponseParser;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.OpencastSolrConfig;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.BinaryResponseWriter;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryResponse;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.servlet.SolrRequestParsers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The solr connection.
 */
public class SolrConnection {

  /** Logging facility */
  static Logger log_ = LoggerFactory.getLogger(SolrConnection.class);

  /** The solr core */
  private SolrCore core = null;

  /** The solr request parser */
  private SolrRequestParsers parser = null;

  /**
   * Constructor. Prepares solr connection.
   * 
   * @param solrDir
   *          The directory of the solr instance.
   * @param dataDir
   *          The directory of the solr index data.
   */
  public SolrConnection(String solrDir, String dataDir) {
    // Initialize SolrConfig
    SolrConfig config = null;
    try {
      config = new OpencastSolrConfig(solrDir, SolrConfig.DEFAULT_CONF_FILE, null);
      solrDir = config.getResourceLoader().getInstanceDir();

      // Initialize SolrCore directly
      IndexSchema schema = new IndexSchema(config, solrDir + "/conf/schema.xml", null);
      core = new SolrCore(null, dataDir, config, schema, null);
      parser = new SolrRequestParsers(config);
    } catch (Exception ee) {
      throw new RuntimeException(ee);
    }
  }

  /**
   * Closes the solr connection.
   */
  public void destroy() {
    if (core != null)
      core.close();
  }

  /**
   * Process a request to query the solr core.
   * 
   * @param query
   *          The solr query as string.
   * @return The query response.
   * @throws Exception
   */
  public QueryResponse request(String query) throws Exception {

    String handlerName = "select";
    SolrParams params = SolrRequestParsers.parseQueryString(query);
    SolrRequestHandler handler = core.getRequestHandler(handlerName);

    String qt = params.get(CommonParams.QT);
    handler = core.getRequestHandler(qt);

    if (handler == null) {
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "unknown handler: " + handlerName);
    }

    List<ContentStream> streams = new ArrayList<ContentStream>(1);

    SolrQueryRequest req = null;
    try {
      req = parser.buildRequestFrom(core, params, streams);
      SolrQueryResponse rsp = new SolrQueryResponse();
      core.execute(handler, req, rsp);

      if (rsp.getException() != null) {
        log_.warn(rsp.getException().toString());
        throw rsp.getException();
      }

      // create solrj response.
      QueryResponse qrsp = new QueryResponse();
      qrsp.setResponse(getParsedResponse(req, rsp));

      return qrsp;
    } finally {
      if (req != null) {
        req.close();
      }
    }
  }

  /**
   * Process a request to query the solr core.
   * 
   * @param request
   *          The solr request
   * @return The query response.
   * @throws Exception
   */
  public QueryResponse update(UpdateRequest request) throws Exception {
    String path = request.getPath();
    if (path == null || !path.startsWith("/")) {
      path = "/select";
    }

    if (core == null) {
      throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Solr core is null. ");
    }

    SolrParams params = request.getParams();
    if (params == null) {
      params = new ModifiableSolrParams();
    }

    // Extract the handler from the path or params
    SolrRequestHandler handler = core.getRequestHandler(path);
    if (handler == null) {
      if ("/select".equals(path) || "/select/".equalsIgnoreCase(path)) {
        String qt = params.get(CommonParams.QT);
        handler = core.getRequestHandler(qt);
        if (handler == null) {
          throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "unknown handler: " + qt);
        }
      }
    }

    if (handler == null) {
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "unknown handler: " + path);
    }

    try {
      SolrQueryRequest req = parser.buildRequestFrom(core, params, request.getContentStreams());
      req.getContext().put("path", path);
      SolrQueryResponse rsp = new SolrQueryResponse();
      core.execute(handler, req, rsp);
      if (rsp.getException() != null) {
        throw new SolrServerException(rsp.getException());
      }

      // Now write it out
      NamedList<Object> normalized = getParsedResponse(req, rsp);
      req.close();
      QueryResponse res = new QueryResponse();
      res.setResponse(normalized);
      return res;

    } catch (IOException iox) {
      throw iox;
    } catch (Exception ex) {
      throw new SolrServerException(ex);
    }

  }

  /**
   * Parse the solr response to named list (need to create solrj query respond).
   * 
   * @param req
   *          The request.
   * @param rsp
   *          The response.
   * @return The named list.
   */
  public NamedList<Object> getParsedResponse(SolrQueryRequest req, SolrQueryResponse rsp) {
    try {
      BinaryResponseWriter writer = new BinaryResponseWriter();
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      writer.write(bos, req, rsp);
      BinaryResponseParser parser = new BinaryResponseParser();
      return parser.processResponse(new ByteArrayInputStream(bos.toByteArray()), "UTF-8");
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * The solr core, used by this connection.
   * 
   * @return The solr core.
   */
  public SolrCore getCore() {
    return this.core;
  }

}