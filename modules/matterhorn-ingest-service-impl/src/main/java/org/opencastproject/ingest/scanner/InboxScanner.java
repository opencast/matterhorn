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
package org.opencastproject.ingest.scanner;

import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Dictionary;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Places a file named "inbox*" from any fileinstall watch directory into the inbox collection. Fileinstall takes care
 * of installing artifacts only once they are fully copied into the watch directory.
 */
public class InboxScanner implements ArtifactInstaller, ManagedService {

  /** The logger */
  protected static final Logger logger = LoggerFactory.getLogger(InboxScanner.class);

  /** The configuration key to use for determining the user to run as for ingest */
  public static final String USER_NAME = "user.name";

  /** The configuration key to use for determining the workflow definition to use for ingest */
  public static final String WORKFLOW_DEFINITION = "workflow.definition";

  /** The workspace */
  protected Workspace workspace = null;

  /** The ingest service */
  protected IngestService ingestService = null;

  /** The local thread pool */
  protected ExecutorService executorService = null;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;

  /** The security service */
  protected SecurityService securityService = null;

  /** The user to run as during ingest */
  protected User user = null;

  /** The workflow definition ID to use during ingest */
  protected String workflowDefinition = null;

  /**
   * Sets the ingest service
   * 
   * @param ingestService
   *          the ingest service
   */
  public void setIngestService(IngestService ingestService) {
    this.ingestService = ingestService;
  }

  /**
   * Sets the workspace
   * 
   * @param workspace
   *          an instance of the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Callback for setting the security service.
   * 
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Sets the user directory
   * 
   * @param userDirectoryService
   *          the userDirectoryService to set
   */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  protected void activate(ComponentContext cc) {
    int maxThreads = 1;
    if (cc != null && cc.getBundleContext().getProperty("org.opencastproject.inbox.threads") != null) {
      try {
        maxThreads = Integer.parseInt(cc.getBundleContext().getProperty("org.opencastproject.inbox.threads"));
      } catch (NumberFormatException e) {
        logger.warn("Illegal value set for org.opencastproject.inbox.threads. Using default value of 1 inbox ingest at a time.");
      }
    }
    this.executorService = Executors.newFixedThreadPool(maxThreads);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactInstaller#install(java.io.File)
   */
  public void install(File artifact) throws Exception {
    executorService.execute(getInstallRunnable(artifact));
  }

  protected Runnable getInstallRunnable(final File artifact) {
    return new Runnable() {
      public void run() {
        try {
          securityService.setUser(user);
          boolean mediaPackageIngestSuccess = false;
          if ("zip".equals(FilenameUtils.getExtension(artifact.getName()))) {
            FileInputStream in = null;
            try {
              in = new FileInputStream(artifact);
              ingestService.addZippedMediaPackage(in, workflowDefinition);
              logger.info("Ingested '{}' as a mediapackage", artifact.getAbsolutePath());
              mediaPackageIngestSuccess = true;
            } catch (Exception e) {
              logger.warn("Unable to ingest mediapackage '{}', {}", artifact.getAbsolutePath(), e);
            } finally {
              IOUtils.closeQuietly(in);
            }
          }

          if (!mediaPackageIngestSuccess) {
            FileInputStream in = null;
            try {
              in = new FileInputStream(artifact);
              workspace.putInCollection("inbox", artifact.getName(), in);
              logger.info("Ingested '{}' as an inbox file", artifact.getAbsolutePath());
            } catch (IOException e) {
              logger.warn("Unable to process inbox file '{}', {}", artifact.getAbsolutePath(), e);
            } finally {
              IOUtils.closeQuietly(in);
            }
          }

          try {
            FileUtils.forceDelete(artifact);
          } catch (IOException e) {
            logger.warn("Unable to delete file {}, {}", artifact.getAbsolutePath(), e);
          }
        } finally {
          securityService.setUser(null);
        }
      }
    };
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactInstaller#uninstall(java.io.File)
   */
  public void uninstall(File artifact) throws Exception {
    // nothing to do
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactInstaller#update(java.io.File)
   */
  public void update(File artifact) throws Exception {
    // nothing to do
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactListener#canHandle(java.io.File)
   */
  public boolean canHandle(File artifact) {
    return "inbox".equals(artifact.getParentFile().getName());
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @SuppressWarnings("rawtypes")
  @Override
  public void updated(Dictionary properties) throws ConfigurationException {
    String userNameConfig = (String) properties.get(USER_NAME);
    if (StringUtils.isNotBlank(userNameConfig)) {
      user = userDirectoryService.loadUser(userNameConfig);
    }
    String workflowConfig = (String) properties.get(WORKFLOW_DEFINITION);
    if (StringUtils.isNotBlank(workflowConfig)) {
      workflowDefinition = workflowConfig;
    }
  }
}
