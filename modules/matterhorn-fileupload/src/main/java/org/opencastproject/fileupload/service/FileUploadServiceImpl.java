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
package org.opencastproject.fileupload.service;

import org.opencastproject.fileupload.api.FileUploadService;
import org.opencastproject.fileupload.api.exception.FileUploadException;
import org.opencastproject.fileupload.api.job.Chunk;
import org.opencastproject.fileupload.api.job.FileUploadJob;
import org.opencastproject.fileupload.api.job.Payload;
import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.data.Function2;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.functions.Functions;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 * A service for big file uploads via HTTP.
 * 
 */
public class FileUploadServiceImpl implements FileUploadService {

  final String PROPKEY_STORAGE_DIR = "org.opencastproject.storage.dir";
  final String DIRNAME_WORK_ROOT = "fileupload-tmp";
  final String UPLOAD_COLLECTION = "uploaded";
  final String FILEEXT_DATAFILE = ".payload";
  final String FILENAME_CHUNKFILE = "chunk.part";
  final String FILENAME_JOBFILE = "job.xml";
  final int READ_BUFFER_LENGTH = 512;
  private static final Logger log = LoggerFactory.getLogger(FileUploadServiceImpl.class);
  private File workRoot;
  private IngestService ingestService;
  private Workspace workspace;
  private Marshaller jobMarshaller;
  private Unmarshaller jobUnmarshaller;
  private HashMap<String, FileUploadJob> jobCache = new HashMap<String, FileUploadJob>();
  private byte[] readBuffer = new byte[READ_BUFFER_LENGTH];
  private FileUploadServiceCleaner cleaner;

  // <editor-fold defaultstate="collapsed" desc="OSGi Service Stuff" >
  protected void activate(ComponentContext cc) throws Exception {
    // ensure existence of working directory
    String dirname = cc.getBundleContext().getProperty(PROPKEY_STORAGE_DIR);
    if (dirname != null) {
      workRoot = new File(dirname + File.separator + DIRNAME_WORK_ROOT);
      if (!workRoot.exists()) {
        FileUtils.forceMkdir(workRoot);
      }
    } else {
      throw new RuntimeException("Storage directory must be defined with framework property " + PROPKEY_STORAGE_DIR);
    }

    // set up de-/serialization
    ClassLoader cl = FileUploadJob.class.getClassLoader();
    JAXBContext jctx = JAXBContext.newInstance("org.opencastproject.fileupload.api.job", cl);
    jobMarshaller = jctx.createMarshaller();
    jobUnmarshaller = jctx.createUnmarshaller();

    log.info("File Upload Service activated. Storage directory is {}", workRoot.getAbsolutePath());

    cleaner = new FileUploadServiceCleaner(this);
    cleaner.schedule();
  }

  protected void deactivate(ComponentContext cc) {
    log.info("File Upload Service deactivated");
    cleaner.shutdown();
  }

  protected void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  protected void setIngestService(IngestService ingestService) {
    this.ingestService = ingestService;
  }

  // </editor-fold>

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.fileupload.api.FileUploadService#createJob(String filename, long filesize, int chunksize)
   */
  @Override
  public FileUploadJob createJob(String filename, long filesize, int chunksize, MediaPackage mp,
          MediaPackageElementFlavor flavor) throws FileUploadException {
    FileUploadJob job = new FileUploadJob(filename, filesize, chunksize, mp, flavor);
    log.info("Creating new upload job: {}", job);
    try {
      File jobDir = getJobDir(job.getId()); // create working dir
      FileUtils.forceMkdir(jobDir);
      ensureExists(getPayloadFile(job.getId())); // create empty payload file
      storeJob(job); // create job file
    } catch (FileUploadException e) {
      deleteJob(job.getId());
      String message = new StringBuilder("Could not create job file in ").append(workRoot.getAbsolutePath())
              .append(": ").append(e.getMessage()).toString();
      log.error(message, e);
      throw new FileUploadException(message, e);
    } catch (IOException e) {
      deleteJob(job.getId());
      String message = new StringBuilder("Could not create upload job directory in ")
              .append(workRoot.getAbsolutePath()).append(": ").append(e.getMessage()).toString();
      log.error(message, e);
      throw new FileUploadException(message, e);
    }
    return job;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.fileupload.api.FileUploadService#hasJob(String id)
   */
  @Override
  public boolean hasJob(String id) {
    try {
      if (jobCache.containsKey(id)) {
        return true;
      } else {
        File jobFile = getJobFile(id);
        return jobFile.exists();
      }
    } catch (Exception e) {
      log.warn("Error while looking for upload job: " + e.getMessage());
      return false;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.fileupload.api.FileUploadService#getJob(String id)
   */
  @Override
  public FileUploadJob getJob(String id) throws FileUploadException {
    if (jobCache.containsKey(id)) {
      return jobCache.get(id);
    } else {
      try {
        synchronized (this) {
          File jobFile = getJobFile(id);
          FileUploadJob job = (FileUploadJob) jobUnmarshaller.unmarshal(jobFile);
          return job;
        }
      } catch (Exception e) {
        log.warn("Failed to load job " + id + " from file.");
        throw new FileUploadException("Error retrieving job " + id, e);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.fileupload.api.FileUploadService#cleanOutdatedJobs()
   */
  @Override
  public void cleanOutdatedJobs() throws IOException {
    for (File f : workRoot.listFiles()) {
      if (f.getParentFile().equals(workRoot) && f.isDirectory()) {
        FileUploadJob job = jobCache.get(f.getName());
        if (job == null) {
          FileUtils.forceDelete(f);
          log.info("Deleted outdated job {}", f.getName());
        } else {
          Calendar cal = Calendar.getInstance();
          cal.add(Calendar.HOUR, -6);
          if (f.lastModified() < cal.getTimeInMillis()) {
            FileUtils.forceDelete(f);
            jobCache.remove(job.getId());
            log.info("Deleted outdated job {}", job);
          }
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.fileupload.api.FileUploadService#storeJob(org.opencastproject.fileupload.api.job.FileUploadJob
   *      job)
   */
  @Override
  public void storeJob(FileUploadJob job) throws FileUploadException {
    try {
      log.debug("Attempting to store job {}", job.getId());
      File jobFile = ensureExists(getJobFile(job.getId()));
      jobMarshaller.marshal(job, jobFile);
    } catch (Exception e) {
      log.warn("Error while storing upload job: " + e.getMessage());
      throw new FileUploadException("Failed to write job file.");
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.fileupload.api.FileUploadService#deleteJob(String id)
   */
  @Override
  public void deleteJob(String id) throws FileUploadException {
    try {
      log.debug("Attempting to delete job " + id);
      if (isLocked(id)) {
        jobCache.remove(id);
      }
      File jobDir = new File(workRoot.getAbsolutePath() + File.separator + id);
      FileUtils.forceDelete(jobDir);
    } catch (Exception e) {
      log.warn("Error while deleting upload job: " + e.getMessage());
      throw new FileUploadException("Error deleting job", e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.fileupload.api.FileUploadService#acceptChunk(org.opencastproject.fileupload.api.job.FileUploadJob
   *      job, long chunk, InputStream content)
   */
  @Override
  public void acceptChunk(FileUploadJob job, long chunkNumber, InputStream content) throws FileUploadException {
    // job already completed?
    if (job.getState().equals(FileUploadJob.JobState.COMPLETE)) {
      removeFromCache(job);
      throw new FileUploadException("Job is already complete!");
    }

    // job ready to recieve data?
    if (isLocked(job.getId())) {
      throw new FileUploadException("Job is locked. Seems like a concurrent upload to this job is in progress.");
    } else {
      lock(job);
    }

    // right chunk offered?
    int supposedChunk = job.getCurrentChunk().getNumber() + 1;
    if (chunkNumber != supposedChunk) {
      StringBuilder sb = new StringBuilder().append("Wrong chunk number! Awaiting #").append(supposedChunk)
              .append(" but #").append(Long.toString(chunkNumber)).append(" was offered.");
      removeFromCache(job);
      throw new FileUploadException(sb.toString());
    }
    log.debug("Recieving chunk #" + chunkNumber + " of job {}", job);

    // write chunk to temp file
    job.getCurrentChunk().incrementNumber();
    File chunkFile = ensureExists(getChunkFile(job.getId()));
    OutputStream out = null;
    try {
      out = new FileOutputStream(chunkFile, false);
      int bytesRead = 0;
      long bytesReadTotal = 0l;
      Chunk currentChunk = job.getCurrentChunk(); // copy manually (instead of using IOUtils.copy()) so we can count the
                                                  // number of bytes
      do {
        bytesRead = content.read(readBuffer);
        if (bytesRead > 0) {
          out.write(readBuffer, 0, bytesRead);
          bytesReadTotal += bytesRead;
          currentChunk.setRecieved(bytesReadTotal);
        }
      } while (bytesRead != -1);
      if (job.getPayload().getTotalSize() == -1 && job.getChunksTotal() == 1) { // set totalSize in case of ordinary
                                                                                // from submit
        job.getPayload().setTotalSize(bytesReadTotal);
      }
    } catch (Exception e) {
      removeFromCache(job);
      throw new FileUploadException("Failed to store chunk data!", e);
    } finally {
      IOUtils.closeQuietly(content);
      IOUtils.closeQuietly(out);
    }

    // check if chunk has right size
    long actualSize = chunkFile.length();
    long supposedSize;
    if (chunkNumber == job.getChunksTotal() - 1) {
      supposedSize = job.getPayload().getTotalSize() % job.getChunksize();
      supposedSize = supposedSize == 0 ? job.getChunksize() : supposedSize; // a not so nice workaround for the rare
                                                                            // case that file size is a multiple of the
                                                                            // chunk size
    } else {
      supposedSize = job.getChunksize();
    }
    if (actualSize == supposedSize || (job.getChunksTotal() == 1 && job.getChunksize() == -1)) {

      // append chunk to payload file
      FileInputStream in = null;
      try {
        File payloadFile = getPayloadFile(job.getId());
        in = new FileInputStream(chunkFile);
        out = new FileOutputStream(payloadFile, true);
        IOUtils.copy(in, out);
        Payload payload = job.getPayload();
        payload.setCurrentSize(payload.getCurrentSize() + actualSize);

      } catch (IOException e) {
        log.error("Failed to append chunk data.", e);
        removeFromCache(job);
        throw new FileUploadException("Could not append chunk data", e);

      } finally {
        IOUtils.closeQuietly(in);
        IOUtils.closeQuietly(out);
        deleteChunkFile(job.getId());
      }

    } else {
      StringBuilder sb = new StringBuilder().append("Chunk has wrong size. Awaited: ").append(supposedSize)
              .append(" bytes, recieved: ").append(actualSize).append(" bytes.");
      removeFromCache(job);
      throw new FileUploadException(sb.toString());
    }

    // update job
    if (chunkNumber == job.getChunksTotal() - 1) { // upload is complete
      finalizeJob(job);
      log.info("Upload job completed: {}", job);
    } else {
      job.setState(FileUploadJob.JobState.READY); // upload still incomplete
    }
    storeJob(job);
    removeFromCache(job);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.fileupload.api.FileUploadService#getPayload(org.opencastproject.fileupload.api.job.FileUploadJob
   *      job)
   */
  @Override
  public InputStream getPayload(FileUploadJob job) throws FileUploadException {
    // job not locked?
    if (isLocked(job.getId())) {
      throw new FileUploadException(
              "Job is locked. Download is only permitted while no upload to this job is in progress.");
    } else {
      lock(job);
    }

    try {
      FileInputStream payload = new FileInputStream(getPayloadFile(job.getId()));
      return payload;
    } catch (FileNotFoundException e) {
      throw new FileUploadException("Failed to retrieve file from job " + job.getId());
    }
  }

  /**
   * Locks an upload job and puts it in job cache.
   * 
   * @param job
   *          job to lock
   */
  private void lock(FileUploadJob job) {
    jobCache.put(job.getId(), job);
    job.setState(FileUploadJob.JobState.INPROGRESS);
  }

  /**
   * Returns true if the job with the given ID is currently locked.
   * 
   * @param id
   *          ID of the job in question
   * @return true if job is locked, false otherwise
   */
  private boolean isLocked(String id) {
    if (jobCache.containsKey(id)) {
      FileUploadJob job = jobCache.get(id);
      return job.getState().equals(FileUploadJob.JobState.INPROGRESS)
              || job.getState().equals(FileUploadJob.JobState.FINALIZING);
    } else {
      return false;
    }
  }

  /**
   * Removes upload job from job cache.
   * 
   * @param job
   *          job to remove from cache
   * @throws FileUploadException
   */
  private void removeFromCache(FileUploadJob job) throws FileUploadException {
    jobCache.remove(job.getId());
  }

  /**
   * Unlocks an finalizes an upload job.
   * 
   * @param job
   *          job to finalize
   * @throws FileUploadException
   */
  private void finalizeJob(FileUploadJob job) throws FileUploadException {
    job.setState(FileUploadJob.JobState.FINALIZING);

    if (job.getPayload().getMediaPackage() == null) { // do we have a target mediaPackge ?
      job.getPayload().setUrl(putPayloadIntoCollection(job)); // if not, put file into upload collection in WFR
    } else {
      job.getPayload().setUrl(putPayloadIntoMediaPackage(job)); // else add file to target MP
    }
    deletePayloadFile(job.getId()); // delete payload in temp directory

    job.setState(FileUploadJob.JobState.COMPLETE);
  }

  /**
   * Function that writes the given file to the uploaded collection.
   * 
   */
  private Function2<InputStream, File, Option<URI>> putInCollection = new Function2<InputStream, File, Option<URI>>() {

    @Override
    public Option<URI> apply(InputStream is, File f) {
      try {
        URI uri = workspace.putInCollection(UPLOAD_COLLECTION, f.getName(), is); // storing file with jod id as name
                                                                                 // instead of original filename to
                                                                                 // avoid collisions (original filename
                                                                                 // can be obtained from upload job)
        return Option.some(uri);
      } catch (IOException e) {
        log.error("Could not add file to collection.", e);
        return Option.none();
      }
    }
  };

  /**
   * Puts the payload of an upload job into the upload collection in the WFR and returns the URL to the file in the WFR.
   * 
   * @param job
   * @return URL of the file in the WFR
   * @throws FileUploadException
   */
  private URL putPayloadIntoCollection(FileUploadJob job) throws FileUploadException {
    log.info("Moving payload of job " + job.getId() + " to collection " + UPLOAD_COLLECTION);
    Option<URI> result = IoSupport.withFile(getPayloadFile(job.getId()), putInCollection).flatMap(
            Functions.<Option<URI>> identity());
    if (result.isSome()) {
      try {
        return result.get().toURL();
      } catch (MalformedURLException e) {
        throw new FileUploadException("Unable to return URL of payloads final destination.", e);
      }
    } else {
      throw new FileUploadException("Failed to put payload in collection.");
    }
  }

  /**
   * Puts the payload of an upload job into a MediaPackage in the WFR, adds the files as a track to the MediaPackage and
   * returns the files URL in the WFR.
   * 
   * @param job
   * @return URL of the file in the WFR
   * @throws FileUploadException
   */
  private URL putPayloadIntoMediaPackage(FileUploadJob job) throws FileUploadException {
    MediaPackage mediaPackage = job.getPayload().getMediaPackage();
    MediaPackageElementFlavor flavor = job.getPayload().getFlavor();
    List<Track> excludeTracks = Arrays.asList(mediaPackage.getTracks(flavor));

    FileInputStream fileInputStream = null;
    try {
      fileInputStream = new FileInputStream(getPayloadFile(job.getId()));
      MediaPackage mp = ingestService.addTrack(fileInputStream, job.getPayload().getFilename(), job.getPayload()
              .getFlavor(), mediaPackage);

      List<Track> tracks = new ArrayList<Track>(Arrays.asList(mp.getTracks(flavor)));
      tracks.removeAll(excludeTracks);
      if (tracks.size() != 1)
        throw new FileUploadException("Ingested track not found");

      return tracks.get(0).getURI().toURL();
    } catch (Exception e) {
      throw new FileUploadException("Failed to add payload to MediaPackage.", e);
    } finally {
      IOUtils.closeQuietly(fileInputStream);
    }
  }

  /**
   * Deletes the chunk file from working directory.
   * 
   * @param id
   *          ID of the job of which the chunk file should be deleted
   */
  private void deleteChunkFile(String id) {
    File chunkFile = getChunkFile(id);
    try {
      log.debug("Attempting to delete chunk file of job " + id);
      if (!chunkFile.delete()) {
        throw new RuntimeException("Could not delete chunk file");
      }
    } catch (Exception e) {
      log.warn("Could not delete chunk file " + chunkFile.getAbsolutePath());
    }
  }

  /**
   * Deletes the payload file from working directory.
   * 
   * @param id
   *          ID of the job of which the chunk file should be deleted
   */
  private void deletePayloadFile(String id) {
    File payloadFile = getPayloadFile(id);
    try {
      log.debug("Attempting to delete payload file of job " + id);
      if (!payloadFile.delete()) {
        throw new RuntimeException("Could not delete chunk file");
      }
    } catch (Exception e) {
      log.warn("Could not delete chunk file " + payloadFile.getAbsolutePath());
    }
  }

  /**
   * Ensures the existence of a given file.
   * 
   * @param file
   * @return File existing file
   * @throws IllegalStateException
   */
  private File ensureExists(File file) throws IllegalStateException {
    if (!file.exists()) {
      try {
        file.createNewFile();
      } catch (IOException e) {
        throw new IllegalStateException("Failed to create chunk file!");
      }
    }
    return file;
  }

  /**
   * Returns the directory for a given job ID.
   * 
   * @param id
   *          ID for which a directory name should be generated
   * @return File job directory
   */
  private File getJobDir(String id) {
    StringBuilder sb = new StringBuilder().append(workRoot.getAbsolutePath()).append(File.separator).append(id);
    return new File(sb.toString());
  }

  /**
   * Returns the job information file for a given job ID.
   * 
   * @param id
   *          ID for which a job file name should be generated
   * @return File job file
   */
  private File getJobFile(String id) {
    StringBuilder sb = new StringBuilder().append(workRoot.getAbsolutePath()).append(File.separator).append(id)
            .append(File.separator).append(FILENAME_JOBFILE);
    return new File(sb.toString());
  }

  /**
   * Returns the chunk file for a given job ID.
   * 
   * @param id
   *          ID for which a chunk file name should be generated
   * @return File chunk file
   */
  private File getChunkFile(String id) {
    StringBuilder sb = new StringBuilder().append(workRoot.getAbsolutePath()).append(File.separator).append(id)
            .append(File.separator).append(FILENAME_CHUNKFILE);
    return new File(sb.toString());
  }

  /**
   * Returns the payload file for a given job ID.
   * 
   * @param id
   *          ID for which a payload file name should be generated
   * @return File job file
   */
  private File getPayloadFile(String id) {
    StringBuilder sb = new StringBuilder().append(workRoot.getAbsolutePath()).append(File.separator).append(id)
            .append(File.separator).append(id).append(FILEEXT_DATAFILE);
    return new File(sb.toString());
  }

}
