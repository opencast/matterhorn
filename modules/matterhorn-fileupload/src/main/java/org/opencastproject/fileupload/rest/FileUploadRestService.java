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
package org.opencastproject.fileupload.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.opencastproject.fileupload.api.FileUploadService;
import org.opencastproject.fileupload.api.exception.FileUploadException;
import org.opencastproject.fileupload.api.job.FileUploadJob;
import org.opencastproject.fileupload.service.UploadProgressListener;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** REST endpoint for large file uploads.
 * 
 * FIXME the UploadListener counts the bytes of the whole request -> count only bytes of the filedata field
 *
 */
@Path("/")
@RestService(name = "fileupload", title = "Big File Upload Service",
notes = {
  "All paths above are relative to the REST endpoint base (something like http://your.server/upload)",
  "When posting chunk data the field holding the actual data MUST be the LAST FIELD transmitted in the request otherwise the endpoint doesn't get the chunk metadata and fails."
},
abstractText = "This service provides a facility to upload files that exceed the 2 GB boundry imposed by most browsers through chunked uploads via HTTP.")
public class FileUploadRestService {

  // message field names
  final String REQUESTFIELD_FILENAME = "filename";
  final String REQUESTFIELD_FILESIZE = "filesize";
  final String REQUESTFIELD_DATA = "filedata";
  final String REQUESTFIELD_CHUNKSIZE = "chunksize";
  final String REQUESTFIELD_CHUNKNUM = "chunknumber";
  private static final Logger log = LoggerFactory.getLogger(FileUploadRestService.class);
  private FileUploadService uploadService;

  // <editor-fold defaultstate="collapsed" desc="OSGi Service Stuff" >
  protected void setFileUploadService(FileUploadService service) {
    uploadService = service;
  }

  protected void unsetFileUploadService(FileUploadService service) {
    uploadService = null;
  }

  protected void activate(ComponentContext cc) {
    log.info("File Upload REST Endpoint activated");
  }

  protected void deactivate(ComponentContext cc) {
    log.info("File Upload REST Endpoint deactivated");
  }
  // </editor-fold>

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("newjob")
  @RestQuery(name = "newjob", description = "Creates a new upload job and returns the jobs ID.", restParameters = {
    @RestParameter(description = "The name of the file that will be uploaded", isRequired = false, name = REQUESTFIELD_FILENAME, type = RestParameter.Type.STRING),
    @RestParameter(description = "The size of the file that will be uploaded", isRequired = false, name = REQUESTFIELD_FILESIZE, type = RestParameter.Type.STRING),
    @RestParameter(description = "The size of the chunks that will be uploaded", isRequired = false, name = REQUESTFIELD_CHUNKSIZE, type = RestParameter.Type.STRING)},
  reponses = {
    @RestResponse(description = "job was successfully created", responseCode = HttpServletResponse.SC_OK),
    @RestResponse(description = "upload service gave an error", responseCode = HttpServletResponse.SC_NO_CONTENT)
  }, returnDescription = "The ID of the newly created upload job")
  public Response getNewJob(
          @FormParam(REQUESTFIELD_FILENAME) String filename,
          @FormParam(REQUESTFIELD_FILESIZE) long filesize,
          @FormParam(REQUESTFIELD_CHUNKSIZE) int chunksize) {
    try {
      if (filename == null || filename.trim().length() == 0) {
        filename = "unknown";
      }
      if (filesize < 1) {
        filesize = -1;
      }
      if (chunksize < 1) {
        chunksize = -1;
      }
      FileUploadJob job = uploadService.createJob(filename, filesize, chunksize);
      log.info("New upload job created: {}", job.getId());
      return Response.ok(job.getId()).build();
    } catch (FileUploadException e) {
      return Response.status(Response.Status.NO_CONTENT).entity(e.getMessage()).build();
    } catch (Exception e) {
      return Response.serverError().entity(buildUnexpectedErrorMessage(e)).build();
    }
  }

  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @Path("job/{jobID}.{format:xml|json}")
  @RestQuery(name = "job", description = "Returns the XML or the JSON representation of an upload job.",
    pathParameters = {
      @RestParameter(description = "The ID of the upload job", isRequired = false, name = "jobID", type = RestParameter.Type.STRING),
      @RestParameter(description = "The output format (json or xml) of the response body.", isRequired = true, name = "format", type = RestParameter.Type.STRING)
    },
  reponses = {
    @RestResponse(description = "the job was successfully retrieved.", responseCode = HttpServletResponse.SC_OK),
    @RestResponse(description = "the job was not found.", responseCode = HttpServletResponse.SC_NOT_FOUND)
  }, returnDescription = "The XML representation of the requested upload job.")
  public Response getJob(@PathParam("jobID") String id,
          @PathParam("format") String format) {
    try {
      if (uploadService.hasJob(id)) {
        FileUploadJob job = uploadService.getJob(id);
        // Return the results using the requested format
        final String type = "json".equals(format) ? MediaType.APPLICATION_JSON : MediaType.APPLICATION_XML;
        return Response.ok().entity(job).type(type).build();
      } else {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
    } catch (Exception e) {
      return Response.serverError().entity(buildUnexpectedErrorMessage(e)).build();
    }
  }

  @POST
  @Produces(MediaType.APPLICATION_XML)
  @Path("job/{jobID}")
  @RestQuery(name = "newjob", description = "Appends the next chunk of data to the file on the server.", pathParameters = {
    @RestParameter(description = "The ID of the upload job", isRequired = false, name = "jobID", type = RestParameter.Type.STRING)
  },
  restParameters = {
    @RestParameter(description = "The number of the current chunk", isRequired = false, name = "chunknumber", type = RestParameter.Type.STRING),
    @RestParameter(description = "The payload", isRequired = false, name = "filedata", type = RestParameter.Type.FILE)},
  reponses = {
    @RestResponse(description = "the chunk data was successfully appended to file on server", responseCode = HttpServletResponse.SC_OK),
    @RestResponse(description = "the upload job was not found", responseCode = HttpServletResponse.SC_NOT_FOUND),
    @RestResponse(description = "the request was malformed", responseCode = HttpServletResponse.SC_BAD_REQUEST)
  }, returnDescription = "The XML representation of the updated upload job")
  public Response postPayload(@PathParam("jobID") String jobId, @Context HttpServletRequest request) {
    try {
      // make sure request is "multipart/form-data"
      if (!ServletFileUpload.isMultipartContent(request)) {
        throw new FileUploadException("Request is not of type multipart/form-data");
      }
      if (uploadService.hasJob(jobId)) {    // testing for existence of job here already so we can generate a 404
        long chunkNum = 0;
        FileUploadJob job = uploadService.getJob(jobId);
        ServletFileUpload upload = new ServletFileUpload();
        UploadProgressListener listener = new UploadProgressListener(job);
        upload.setProgressListener(listener);
        for (FileItemIterator iter = upload.getItemIterator(request); iter.hasNext();) {
          FileItemStream item = iter.next();
          if (item.isFormField()) {
            String name = item.getFieldName();
            if (REQUESTFIELD_CHUNKNUM.equalsIgnoreCase(name)) {
              chunkNum = Long.parseLong(Streams.asString(item.openStream()));
            }
          } else if (REQUESTFIELD_DATA.equalsIgnoreCase(item.getFieldName())) {
            uploadService.acceptChunk(job, chunkNum, item.openStream());
            return Response.ok(job).build();
          }
        }
        throw new FileUploadException("No payload!");
      } else {
        log.warn("Upload job not found: " + jobId);
        return Response.status(Response.Status.NOT_FOUND).build();
      }
    } catch (FileUploadException e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
    } catch (Exception e) {
      return Response.serverError().entity(buildUnexpectedErrorMessage(e)).build();
    }
  }

  @DELETE
  @Produces(MediaType.TEXT_PLAIN)
  @Path("job/{jobID}")
  @RestQuery(name = "job", description = "Deletes an upload job on the server.", pathParameters = {
    @RestParameter(description = "The ID of the upload job to be deleted", isRequired = false, name = "jobID", type = RestParameter.Type.STRING)},
  reponses = {
    @RestResponse(description = "the job was successfully deleted.", responseCode = HttpServletResponse.SC_OK),
    @RestResponse(description = "the job was not found.", responseCode = HttpServletResponse.SC_NOT_FOUND)
  }, returnDescription = "A success message that starts with OK")
  public Response deleteJob(@PathParam("jobID") String id) {
    try {
      if (uploadService.hasJob(id)) {
        uploadService.deleteJob(id);
        StringBuilder okMessage = new StringBuilder().append("OK: Deleted job ").append(id);
        return Response.ok(okMessage.toString()).build();
      } else {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
    } catch (Exception e) {
      return Response.serverError().entity(buildUnexpectedErrorMessage(e)).build();
    }
  }

  /** Builds an error message in case of an unexpected error in an endpoint method,
   * includes the exception type and message if existing.
   *
   * TODO append stack trace
   *
   * @param e Exception that was thrown
   * @return error message
   */
  private String buildUnexpectedErrorMessage(Exception e) {
    StringBuilder sb = new StringBuilder();
    sb.append("Unexpected error (").append(e.getClass().getName()).append(")");
    String message = e.getMessage();
    if (message != null && message.length() > 0) {
      sb.append(": ").append(message);
    }
    return sb.toString();
  }
}
