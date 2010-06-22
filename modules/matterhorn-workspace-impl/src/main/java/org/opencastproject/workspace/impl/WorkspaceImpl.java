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
package org.opencastproject.workspace.impl;

import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;
import org.opencastproject.workingfilerepository.api.PathMappable;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.TeeInputStream;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Implements a simple cache for remote URIs. Delegates methods to {@link WorkingFileRepository} wherever possible.
 * <p>
 * Note that if you are running the workspace on the same machine as the singleton working file repository, you can save
 * a lot of space if you configure both root directories onto the same volume (that is, if your file system supports
 * hard links).
 * 
 * TODO Implement cache invalidation using the caching headers, if provided, from the remote server.
 */
public class WorkspaceImpl implements Workspace {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(WorkspaceImpl.class);

  /** Configuration key for the workspace root directory */
  public static final String WORKSPACE_ROOTDIR_KEY = "org.opencastproject.workspace.rootdir";

  protected String wsRoot = null;
  protected long maxAgeInSeconds = -1;
  protected long garbageCollectionPeriodInSeconds = -1;
  protected Timer garbageFileCollector;
  protected boolean linkingEnabled = false;

  protected TrustedHttpClient trustedHttpClient;

  protected WorkingFileRepository wfr = null;
  protected String wfrRoot = null;
  protected String wfrUrl = null;

  public WorkspaceImpl() {
  }

  /**
   * Creates a workspace implementation which is located at the given root directory.
   * <p>
   * Note that if you are running the workspace on the same machine as the singleton working file repository, you can
   * save a lot of space if you configure both root directories onto the same volume (that is, if your file system
   * supports hard links).
   * 
   * @param rootDirectory
   *          the repository root directory
   */
  protected WorkspaceImpl(String rootDirectory) {
    this.wsRoot = rootDirectory;
  }

  /**
   * OSGi service activation callback.
   * 
   * @param cc
   *          the OSGi component context
   */
  public void activate(ComponentContext cc) {
    if (this.wsRoot == null) {
      if (cc != null && cc.getBundleContext().getProperty(WORKSPACE_ROOTDIR_KEY) != null) {
        // use rootDir from CONFIG
        this.wsRoot = cc.getBundleContext().getProperty(WORKSPACE_ROOTDIR_KEY);
        logger.info("CONFIG " + WORKSPACE_ROOTDIR_KEY + ": " + this.wsRoot);
      } else if (cc != null && cc.getBundleContext().getProperty("org.opencastproject.storage.dir") != null) {
        // create rootDir by adding "workspace" to the default data directory
        this.wsRoot = PathSupport.concat(cc.getBundleContext().getProperty("org.opencastproject.storage.dir"),
                "workspace");
        logger.warn("CONFIG " + WORKSPACE_ROOTDIR_KEY + " is missing: falling back to " + this.wsRoot);
      } else {
        throw new IllegalStateException("Configuration '" + WORKSPACE_ROOTDIR_KEY + "' is missing");
      }
    }

    // Create the root directory
    File f = new File(this.wsRoot);
    if (!f.exists()) {
      try {
        FileUtils.forceMkdir(f);
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }

    // Set up the garbage file collection timer
    if (cc != null && cc.getBundleContext().getProperty("org.opencastproject.workspace.gc.period") != null) {
      String period = cc.getBundleContext().getProperty("org.opencastproject.workspace.gc.period");
      if (period != null) {
        try {
          garbageCollectionPeriodInSeconds = Long.parseLong(period);
        } catch (NumberFormatException e) {
          logger.warn("Workspace garbage collection period can not be set to {}. Please choose a valid number "
                  + "for the 'org.opencastproject.workspace.gc.period' setting", period);
        }
      }
    }

    // Test whether hard linking between working file repository and workspace is possible
    if (wfr instanceof PathMappable) {
      File srcFile = new File(wfrRoot, ".linktest");
      File targetFile = new File(wsRoot, ".linktest");
      try {
        FileUtils.touch(srcFile);
      } catch (IOException e) {
        throw new IllegalStateException("The working file repository seems read-only", e);
      }
      linkingEnabled = FileSupport.supportsLinking(srcFile, targetFile);
      if (linkingEnabled)
        logger.info("Hard links between the working file repository and the workspace enabled");
      else {
        logger.warn("Hard links between the working file repository and the workspace are not possible");
        logger.warn("This will increase the overall amount of disk space used");
      }
    }

    // Activate garbage collection
    if (cc != null && cc.getBundleContext().getProperty("org.opencastproject.workspace.gc.max.age") != null) {
      String age = cc.getBundleContext().getProperty("org.opencastproject.workspace.gc.max.age");
      if (age != null) {
        try {
          maxAgeInSeconds = Long.parseLong(age);
        } catch (NumberFormatException e) {
          logger.warn("Workspace garbage collection max age can not be set to {}. Please choose a valid number "
                  + "for the 'org.opencastproject.workspace.gc.max.age' setting", age);
        }
      }
    }
    activateGarbageFileCollectionTimer();
  }

  /**
   * Activate the garbage collection timer
   */
  protected void activateGarbageFileCollectionTimer() {
    if (garbageCollectionPeriodInSeconds > 0 && maxAgeInSeconds > 0) {
      logger.info("Workspace garbage collection policy: delete files older than {} seconds, scan every {} seconds.",
              maxAgeInSeconds, garbageCollectionPeriodInSeconds);
      garbageFileCollector = new Timer("Workspace Garbage File Collector");
      garbageFileCollector.schedule(new GarbageCollectionTimer(), 0, garbageCollectionPeriodInSeconds * 1000);
    }
  }

  /**
   * Deactivate the garbage collection timer.
   */
  protected void deactivateGarbageFileCollectionTimer() {
    if (garbageFileCollector != null) {
      garbageFileCollector.cancel();
    }
  }

  /**
   * Callback from OSGi on service deactivation.
   */
  public void deactivate() {
    deactivateGarbageFileCollectionTimer();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workspace.api.Workspace#get(java.net.URI)
   */
  public File get(URI uri) throws NotFoundException, IOException {
    String urlString = uri.toString();
    String safeFilename = PathSupport.toSafeName(urlString);
    File f = new File(PathSupport.concat(wsRoot, safeFilename));

    // Does the file exist and is it up to date?
    if (f.isFile()) {
      // We assume that this file is only being accessed through the workspace rather than through the working file
      // repository. Since we do all the writing ourselves, it's safe to assume that it's up-to-date.
      // TODO: Store ETags? What about remote servers that don't support them?
      return f;
    }

    if (wfrRoot != null && wfrUrl != null) {
      if (uri.toString().startsWith(wfrUrl)) {
        String localPath = uri.toString().substring(wfrUrl.length());
        File wfrCopy = new File(PathSupport.concat(wfrRoot, localPath));
        if (f.isFile()) {
          File workspaceCopy = new File(wsRoot, safeFilename);
          if (linkingEnabled) {
            FileSupport.link(f, workspaceCopy);
          } else {
            FileSupport.copy(wfrCopy, workspaceCopy);
          }
          logger.debug("Getting {} directly from working file repository root at {}", uri, f);
          return workspaceCopy;
        } else {
          throw new NotFoundException("The file " + uri + " does not exist");
        }
      }
    }

    logger.info("Downloading {} to {}", urlString, f.getAbsolutePath());
    HttpGet get = new HttpGet(urlString);
    InputStream in = null;
    OutputStream out = null;
    try {
      HttpResponse response = trustedHttpClient.execute(get);
      in = response.getEntity().getContent();
      out = new FileOutputStream(f);
      IOUtils.copyLarge(in, out);
    } catch (Exception e) {
      logger.warn("Could not copy {} to {}", urlString, f.getAbsolutePath());
      throw new NotFoundException(e);
    } finally {
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(out);
    }

    return f;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workspace.api.Workspace#delete(java.lang.String, java.lang.String)
   */
  public void delete(String mediaPackageID, String mediaPackageElementID) throws NotFoundException, IOException {
    wfr.delete(mediaPackageID, mediaPackageElementID);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workspace.api.Workspace#put(java.lang.String, java.lang.String, java.lang.String,
   *      java.io.InputStream)
   */
  @Override
  public URI put(String mediaPackageID, String mediaPackageElementID, String fileName, InputStream in)
          throws IOException {
    String safeFileName = PathSupport.toSafeName(fileName);
    URI uri = wfr.getURI(mediaPackageID, mediaPackageElementID, fileName);

    // Determine the target location in the workspace
    InputStream tee = null;
    File workspaceFile = null;
    FileOutputStream out = null;
    try {
      workspaceFile = new File(wsRoot, PathSupport.toSafeName(uri.toString()));
      out = new FileOutputStream(workspaceFile);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // Try hard linking first and fall back to tee-ing to both the working file repository and the workspace
    if (linkingEnabled) {
      tee = in;
      wfr.put(mediaPackageID, mediaPackageElementID, fileName, tee);
      FileUtils.forceMkdir(workspaceFile.getParentFile());
      File workingFileRepoDirectory = new File(PathSupport.concat(new String[] { wfrRoot,
              WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX, mediaPackageID, mediaPackageElementID }));
      File workingFileRepoCopy = new File(workingFileRepoDirectory, safeFileName);
      FileSupport.link(workingFileRepoCopy, workspaceFile, true);
    } else {
      tee = new TeeInputStream(in, out, true);
      wfr.put(mediaPackageID, mediaPackageElementID, fileName, tee);
    }

    // Cleanup
    try {
      tee.close();
    } catch (IOException e) {
      logger.warn("Unable to close file stream: " + e.getLocalizedMessage());
    }

    return uri;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workspace.api.Workspace#putInCollection(java.lang.String, java.lang.String,
   *      java.io.InputStream)
   */
  @Override
  public URI putInCollection(String collectionId, String fileName, InputStream in) throws IOException {
    String safeFileName = PathSupport.toSafeName(fileName);
    URI uri = wfr.getCollectionURI(collectionId, fileName);

    // Determine the target location in the workspace
    InputStream tee = null;
    File tempFile = null;
    FileOutputStream out = null;
    try {
      tempFile = new File(wsRoot, PathSupport.toSafeName(uri.toString()));
      out = new FileOutputStream(tempFile);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // Try hard linking first and fall back to tee-ing to both the working file repository and the workspace
    if (linkingEnabled) {
      tee = in;
      wfr.putInCollection(collectionId, fileName, tee);
      FileUtils.forceMkdir(tempFile.getParentFile());
      File workingFileRepoDirectory = new File(PathSupport.concat(new String[] { wfrRoot,
              WorkingFileRepository.COLLECTION_PATH_PREFIX, collectionId }));
      File workingFileRepoCopy = new File(workingFileRepoDirectory, safeFileName);
      FileSupport.link(workingFileRepoCopy, tempFile, true);
    } else {
      tee = new TeeInputStream(in, out, true);
      wfr.putInCollection(collectionId, fileName, tee);
    }

    // Cleanup
    try {
      tee.close();
    } catch (IOException e) {
      logger.warn("Unable to close file stream: " + e.getLocalizedMessage());
    }

    return uri;
  }

  public void setRepository(WorkingFileRepository repo) {
    this.wfr = repo;
    if (repo instanceof PathMappable) {
      this.wfrRoot = ((PathMappable) repo).getPathPrefix();
      logger.info("Mapping workspace to working file repository using {}", wfrRoot);
    }
  }

  public void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    this.trustedHttpClient = trustedHttpClient;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workspace.api.Workspace#getURI(java.lang.String, java.lang.String)
   */
  public URI getURI(String mediaPackageID, String mediaPackageElementID) {
    return wfr.getURI(mediaPackageID, mediaPackageElementID);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workspace.api.Workspace#getCollectionURI(java.lang.String, java.lang.String)
   */
  @Override
  public URI getCollectionURI(String collectionID, String fileName) {
    return wfr.getCollectionURI(collectionID, fileName);
  }

  class GarbageCollectionTimer extends TimerTask {

    /**
     * {@inheritDoc}
     * 
     * @see java.util.TimerTask#run()
     */
    @Override
    public void run() {
      logger.info("Running workspace garbage file collection");
      // Remove any file that was created more than maxAge seconds ago
      File root = new File(wsRoot);
      File[] oldFiles = root.listFiles(new FileFilter() {
        public boolean accept(File pathname) {
          long ageInSeconds = (System.currentTimeMillis() - pathname.lastModified()) / 1000;
          return ageInSeconds > maxAgeInSeconds;
        }
      });
      for (File oldFile : oldFiles) {
        long ageInSeconds = (System.currentTimeMillis() - oldFile.lastModified()) / 1000;
        Object[] loggingArgs = new Object[] { oldFile, ageInSeconds - maxAgeInSeconds, maxAgeInSeconds };
        if (oldFile.delete()) {
          logger.info("Deleted {}, since its age was {} seconds older than the maximum age, {}", loggingArgs);
        } else {
          logger.warn("Can not delete {}, even though it is {} seconds older than the maximum age, {}", loggingArgs);
        }
      }
    }

  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workspace.api.Workspace#getCollectionContents(java.lang.String)
   */
  @Override
  public URI[] getCollectionContents(String collectionId) throws IOException {
    return wfr.getCollectionContents(collectionId);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workspace.api.Workspace#deleteFromCollection(java.lang.String, java.lang.String)
   */
  @Override
  public void deleteFromCollection(String collectionId, String fileName) throws NotFoundException, IOException {
    wfr.removeFromCollection(collectionId, fileName);
  }

}
