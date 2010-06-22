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
package org.opencastproject.analysis.rest;

import org.opencastproject.analysis.api.MediaAnalysisService;
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.remote.api.Receipt;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Param.Type;

import org.apache.commons.io.IOUtils;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * The REST endpoint for {@link MediaAnalysisService}s
 */
@Path("")
public class MediaAnalysisRestEndpoint {
  private static final Logger logger = LoggerFactory.getLogger(MediaAnalysisRestEndpoint.class);
  protected String docs;
  protected ComponentContext componentContext;

  public void activate(ComponentContext componentContext) {
    this.componentContext = componentContext;
    this.docs = generateDocs();
  }

  public void deactivate() {
  }

  protected MediaAnalysisService getMediaAnalysiService(String analysisType) {
    ServiceReference[] refs = null;
    try {
      refs = componentContext.getBundleContext().getAllServiceReferences(MediaAnalysisService.class.getName(), null);
    } catch (InvalidSyntaxException e) {
      throw new IllegalArgumentException("Unable to locate media analysis services");
    }
    if(refs == null || refs.length == 0) {
      throw new IllegalArgumentException("No media analysis services located");
    }
    for(ServiceReference ref : refs) {
      MediaAnalysisService service = (MediaAnalysisService)componentContext.getBundleContext().getService(ref);
      if(analysisType.equals(service.getAnalysisType())) return service;
    }
    throw new IllegalArgumentException("Unable to find a media analysis service with analysis.type property = "
            + analysisType);
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("/{analysisType}")
  public Response analyze(@PathParam("analysisType") String analysisType, @FormParam("track") String trackAsXml) {
    try {
      MediaAnalysisService service = getMediaAnalysiService(analysisType);
      DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document doc = docBuilder.parse(IOUtils.toInputStream(trackAsXml));
      MediaPackageElement element = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
              .elementFromManifest(doc.getDocumentElement(), new DefaultMediaPackageSerializerImpl());
      Receipt receipt = service.analyze(element, false);
      return Response.ok(receipt.toXml()).build();
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().build();
    }
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("/{analysisType}/{id}.xml")
  public Response getReceipt(@PathParam("analysisType") String analysisType, @PathParam("id") String id) {
    MediaAnalysisService service = getMediaAnalysiService(analysisType);
    Receipt receipt = service.getReceipt(id);
    if (receipt == null) {
      return Response.status(Status.NOT_FOUND).type(MediaType.TEXT_HTML).build();
    } else {
      return Response.ok(receipt.toXml()).build();
    }
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocs() {
    return docs;
  }

  protected String generateDocs() {
    DocRestData data = new DocRestData("MediaAnalysis", "Media Analysis Service", "/analysis/rest",
            new String[] { "$Rev$" });
    // analyze
    RestEndpoint analyzeEndpoint = new RestEndpoint("analyze", RestEndpoint.Method.POST, "/{analysisType}",
            "Submit a track for analysis");
    analyzeEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("The receipt to use when polling for the resulting mpeg7 catalog"));
    analyzeEndpoint.addPathParam(new Param("analysisType", Type.STRING, "org.opencastproject.analysis.vsegmenter",
            "The 'analysis.type' property that uniquely identifies the desired analysis service"));
    analyzeEndpoint.addRequiredParam(new Param("track", Type.TEXT, "",
            "The track to analyze.  For video segmentation, this must be a motion jpeg file."));
    analyzeEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, analyzeEndpoint);

    // receipt
    RestEndpoint receiptEndpoint = new RestEndpoint("receipt", RestEndpoint.Method.GET, "/{analysisType}/{id}.xml",
            "Retrieve a receipt for an analysis task");
    receiptEndpoint.addStatus(org.opencastproject.util.doc.Status.OK("Results in an xml document containing the "
            + "status of the analysis job, and the catalog produced by this analysis job if it the task is finished"));
    receiptEndpoint.addPathParam(new Param("analysisType", Type.STRING, "org.opencastproject.analysis.vsegmenter",
            "The 'analysis.type' property that uniquely identifies the desired analysis service"));
    receiptEndpoint.addPathParam(new Param("id", Param.Type.STRING, null, "the receipt id"));
    receiptEndpoint.addFormat(new Format("xml", null, null));
    receiptEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, receiptEndpoint);

    return DocUtil.generate(data);
  }
}
