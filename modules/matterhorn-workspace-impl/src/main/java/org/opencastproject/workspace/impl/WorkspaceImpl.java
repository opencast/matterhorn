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
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.opencastproject.workspace.api.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
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
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.Map.Entry;

/**
 * Implements a simple cache for remote URIs. Delegates methods to {@link WorkingFileRepository} wherever possible.
 * 
 * TODO Implement cache invalidation using the caching headers, if provided, from the remote server.
 */
public class WorkspaceImpl implements Workspace {
  public static final String WORKSPACE_ROOTDIR_KEY = "org.opencastproject.workspace.rootdir";
  private static final Logger logger = LoggerFactory.getLogger(WorkspaceImpl.class);

  protected WorkingFileRepository repo;
  protected TrustedHttpClient trustedHttpClient;
  protected String rootDirectory = null;
  protected String collectionsDir = null;
  protected Map<String, String> filesystemMappings;
  protected long maxAgeInSeconds = -1;
  protected long garbageCollectionPeriodInSeconds = -1;
  protected Timer garbageFileCollector;

  public WorkspaceImpl() {
  }

  public WorkspaceImpl(String rootDirectory) {
    this.rootDirectory = rootDirectory;
  }

  public void activate(ComponentContext cc) {
    // NOTE: warning - the test calls activate() with a NULL cc
    if (this.rootDirectory == null) {
      if (cc != null && cc.getBundleContext().getProperty(WORKSPACE_ROOTDIR_KEY) != null) {
        // use rootDir from CONFIG
        this.rootDirectory = cc.getBundleContext().getProperty(WORKSPACE_ROOTDIR_KEY);
        logger.info("CONFIG " + WORKSPACE_ROOTDIR_KEY + ": " + this.rootDirectory);
      } else if (cc != null && cc.getBundleContext().getProperty("org.opencastproject.storage.dir") != null) {
        // create rootDir by adding "workspace" to the default data directory
        this.rootDirectory = PathSupport.concat(cc.getBundleContext().getProperty("org.opencastproject.storage.dir"),
                "workspace");
        logger.warn("CONFIG " + WORKSPACE_ROOTDIR_KEY + " is missing: falling back to " + this.rootDirectory);
      } else {
        throw new IllegalStateException("Configuration '" + WORKSPACE_ROOTDIR_KEY + "' is missing");
      }
    }

    createRootDirectory();

    filesystemMappings = new HashMap<String, String>();
    String filesUrl;
    if (cc == null || cc.getBundleContext().getProperty("org.opencastproject.server.url") == null) {
      filesUrl = UrlSupport.DEFAULT_BASE_URL + "/files/mp";
    } else {
      filesUrl = cc.getBundleContext().getProperty("org.opencastproject.server.url") + "/files/mp";
    }

    // Find the working file repository's root directory
    // Note: we only map mediapackage elements, not collections. FIXME?
    String repoRoot;
    if (cc != null && cc.getBundleContext().getProperty("org.opencastproject.file.repo.path") != null) {
      repoRoot = cc.getBundleContext().getProperty("org.opencastproject.file.repo.path");
      logger.info("Workspace filesystem mapping " + filesUrl + " => " + repoRoot + "/mp");
      filesystemMappings.put(filesUrl, repoRoot + "/mp");
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
   * If this URL is available on a mounted filesystem, return the file handle (otherwise, null).
   * 
   * @param urlString
   *          The URL as a string
   * @return The file, or null if the file is not on a configured mount.
   */
  protected File getLocallyMountedFile(String urlString) {
    if (urlString.startsWith("file")) {
      try {
        File f = new File(new URI(urlString));
        if (f.exists() && f.canRead())
          return f;
      } catch (URISyntaxException e) {
      }
    }
    for (Entry<String, String> entry : filesystemMappings.entrySet()) {
      String baseUrl = entry.getKey();
      String baseFilesystemPath = entry.getValue();
      if (urlString.startsWith(baseUrl)) {
        String pathExtraInfo = urlString.substring(baseUrl.length());
        File f = new File(baseFilesystemPath + pathExtraInfo);
        if (f.exists() && f.isFile() && f.canRead()) {
          logger.debug("found local file {} for URL {}", f.getAbsolutePath(), urlString);
          return f;
        }
      }
    }
    return null;
  }

  public File get(URI uri) throws NotFoundException {
    String urlString = uri.toString();

    // If any local filesystem mappings match this uri, just return the file handle
    File localFile = getLocallyMountedFile(urlString);
    if (localFile != null)
      return localFile;

    String safeFilename = toFilesystemSafeName(urlString);
    String fullPath = rootDirectory + File.separator + safeFilename;
    // See if there's a matching file under the root directory
    File f = new File(fullPath);
    if (f.exists()) {
      return f;
    } else {
      logger.info("Copying {} to {}", urlString, f.getAbsolutePath());
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
  }

  protected String toFilesystemSafeName(String urlString) {
    String urlExtension = FilenameUtils.getExtension(urlString);
    String baseName = urlString.substring(0, urlString.length() - (urlExtension.length() + 1));
    String safeBaseName = baseName.replaceAll("\\W", "_"); // TODO -- ensure that this filename is safe on all platforms
    String safeString = null;
    if ("".equals(urlExtension)) {
      safeString = safeBaseName;
    } else {
      safeString = safeBaseName + "." + urlExtension;
    }
    if (safeString.length() < 255)
      return safeString;
    String random = UUID.randomUUID().toString();
    random = random.concat(".").concat(urlExtension);
    logger.info("using '{}' to represent url '{}', which is too long to store as a filename", random, urlString);
    return random;
  }

  public void delete(String mediaPackageID, String mediaPackageElementID) throws NotFoundException {
    repo.delete(mediaPackageID, mediaPackageElementID);
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workspace.api.Workspace#put(java.lang.String, java.lang.String, java.lang.String, java.io.InputStream)
   */
  @Override
  public URI put(String mediaPackageID, String mediaPackageElementID, String fileName, InputStream in) {
    String safeFileName = toFilesystemSafeName(fileName);
    String shortSafeFileName = safeFileName.lastIndexOf("_") > 0 ? safeFileName
            .substring(safeFileName.lastIndexOf("_") + 1) : safeFileName;
    URI uri = repo.getURI(mediaPackageID, mediaPackageElementID, shortSafeFileName);

    //  if this isn't a locally mounted file, branch the file to the workspace where it would be placed by get(URI)
    File localFile = getLocallyMountedFile(uri.toString());
    InputStream tee = null;
    if (localFile == null) {
      File tempFile = null;
      FileOutputStream out = null;
      try {
        tempFile = new File(rootDirectory, toFilesystemSafeName(uri.toString()));
        out = new FileOutputStream(tempFile);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      tee = new TeeInputStream(in, out, true);
    } else {
      tee = in;
    }
    repo.put(mediaPackageID, mediaPackageElementID, shortSafeFileName, tee);
    try {
      tee.close();
    } catch (IOException e) {
      logger.warn("Unable to close file stream: " + e.getLocalizedMessage());
    }
    return uri;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workspace.api.Workspace#getFromCollection(java.lang.String, java.lang.String)
   */
  @Override
  public File getFromCollection(String collectionId, String fileName) throws NotFoundException {
    File collectionDirectory = new File(collectionsDir, collectionId);
    try {
      FileUtils.forceMkdir(collectionDirectory);
    } catch(IOException e) {
      throw new IllegalStateException("unable to create directory " + collectionDirectory.getAbsolutePath());
    }
    File outFile = new File(collectionDirectory, fileName);
    InputStream in = null;
    OutputStream out = null;
    try {
      in = repo.getFromCollection(collectionId, fileName);
      out = new FileOutputStream(outFile);
      IOUtils.copy(in, out);
    } catch(IOException e) {
      throw new NotFoundException(e);
    } finally {
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(out);
    }
    return outFile;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workspace.api.Workspace#putInCollection(java.lang.String, java.lang.String, java.io.InputStream)
   */
  @Override
  public URI putInCollection(String collectionId, String fileName, InputStream in) throws URISyntaxException {
    return repo.putInCollection(collectionId, fileName, in);
  }


  public void setRepository(WorkingFileRepository repo) {
    this.repo = repo;
  }

  public void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    this.trustedHttpClient = trustedHttpClient;
  }

  private void createRootDirectory() {
    File f = new File(this.rootDirectory);
    if (!f.exists()) {
      try {
        FileUtils.forceMkdir(f);
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }
    // also create a parent directory for collections
    File collectionsDir = new File(f, "_collections");
    if (!f.exists()) {
      try {
        FileUtils.forceMkdir(collectionsDir);
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }
    this.collectionsDir = collectionsDir.getAbsolutePath();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workspace.api.Workspace#getURI(java.lang.String, java.lang.String)
   */
  public URI getURI(String mediaPackageID, String mediaPackageElementID) throws NotFoundException {
    return repo.getURI(mediaPackageID, mediaPackageElementID);
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
      File root = new File(rootDirectory);
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
}
