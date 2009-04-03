/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
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
package org.opencastproject.jcr.impl;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.opencastproject.api.OpencastJcrServer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.jcr.Repository;

public class OsgiActivator implements BundleActivator{
  private static final Logger logger = LoggerFactory.getLogger(OsgiActivator.class);
  protected ServiceRegistration serviceRegistration;
  protected Repository repo;
  public void start(BundleContext context) throws Exception {
    // Open the configuration template from within this bundle
    InputStream configTemplate = context.getBundle().getEntry("/cluster-repository-template.xml")
      .openStream();

    // Get the configuration settings from the framework or system properties
    String nodeId = context.getProperty("matterhorn.jcr.nodeId");
    String repoHome = context.getProperty("matterhorn.jcr.repoPath");
    String dbUrl = context.getProperty("matterhorn.jcr.db.url");
    String dbDriver = context.getProperty("matterhorn.jcr.db.driver");
    String dbUser = context.getProperty("matterhorn.jcr.db.user");
    String dbPass = context.getProperty("matterhorn.jcr.db.password");
    String pm = context.getProperty("matterhorn.jcr.persistence.manager");

    // This is just a hack... better to use an xml parser
    BufferedReader br = new BufferedReader(new InputStreamReader(
        configTemplate));
    StringBuilder sb = new StringBuilder();
    String line = null;
    while ((line = br.readLine()) != null) {
      sb.append(line);
    }
    br.close();
    configTemplate.close();
    String templateString = sb.toString();
    String configString = templateString
      .replaceAll("PERSISTENCE_MGR", pm)
      .replaceAll("NODE_ID", nodeId)
      .replaceAll("DB_DRIVER", dbDriver)
      .replaceAll("DB_USER", dbUser)
      .replaceAll("DB_PASS", dbPass)
      .replaceAll("DB_URL", dbUrl);
    
    InputStream config = new ByteArrayInputStream(configString
        .getBytes("UTF8"));

    // Build the repository
    RepositoryConfig rc = RepositoryConfig.create(config, repoHome);

    logger.info("Creating a new JCR instance with cluster id="
        + rc.getClusterConfig().getId());

    repo = RepositoryImpl.create(rc);

    serviceRegistration = context.registerService(OpencastJcrServer.class.getName(),
        new OpencastJcrServerOsgiImpl(repo), null);
  }

  public void stop(BundleContext context) throws Exception {
    serviceRegistration.unregister();
    if(repo != null) {
      ((JackrabbitRepository) repo).shutdown();
    }
  }
}
