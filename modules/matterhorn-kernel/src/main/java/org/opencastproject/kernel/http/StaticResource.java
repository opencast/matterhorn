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
package org.opencastproject.kernel.http;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A static resource for registration with the http service.
 */
public class StaticResource extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LoggerFactory.getLogger(StaticResource.class);
  private final MimetypesFileTypeMap mimeMap = new MimetypesFileTypeMap(getClass().getClassLoader()
          .getResourceAsStream("mimetypes"));
  protected String classpath;
  protected String alias;
  protected String welcomeFile;
  protected URL defaultUrl;

  protected BundleContext bundleContext;

  /**
   * Default no-arg constructor called by OSGI's declarative services. The static resource will be configured via DS
   * component properties.
   */
  public StaticResource() {
  }

  /**
   * Constructs a static resources.
   * 
   * @param bundleContext
   *          the bundle context of this servlet
   * @param classpath
   *          the classpath to the static resources
   * @param alias
   *          the URL alias
   * @param welcomeFile
   *          the default welcome file
   */
  public StaticResource(BundleContext bundleContext, String classpath, String alias, String welcomeFile) {
    this.classpath = classpath;
    this.alias = alias;
    this.welcomeFile = welcomeFile;
    this.bundleContext = bundleContext;
    String serverUrl = bundleContext.getProperty("org.opencastproject.server.url");
    try {
      defaultUrl = new URL(serverUrl + alias);
    } catch (MalformedURLException e) {
      throw new IllegalStateException("unable to construct URL " + serverUrl + "/" + alias, e);
    }
  }

  /**
   * Activates the static resource when it is instantiated using Declarative Services.
   * 
   * @param componentContext
   *          the DS component context
   */
  public void activate(ComponentContext componentContext) {
    this.bundleContext = componentContext.getBundleContext();
    if (welcomeFile == null)
      welcomeFile = (String) componentContext.getProperties().get("welcome.file");
    boolean welcomeFileSpecified = true;
    if (welcomeFile == null) {
      welcomeFileSpecified = false;
      welcomeFile = "index.html";
    }
    if (alias == null)
      alias = (String) componentContext.getProperties().get("alias");
    if (classpath == null)
      classpath = (String) componentContext.getProperties().get("classpath");
    logger.info("registering classpath:{} at {} with welcome file {} {}", new Object[] { classpath, alias, welcomeFile,
            welcomeFileSpecified ? "" : "(via default)" });
    String serverUrl = componentContext.getBundleContext().getProperty("org.opencastproject.server.url");
    try {
      defaultUrl = new URL(serverUrl + alias);
    } catch (MalformedURLException e) {
      throw new IllegalStateException("unable to construct URL " + serverUrl + "/" + alias, e);
    }
  }

  public URL getDefaultUrl() {
    return defaultUrl;
  }

  @Override
  public String toString() {
    return "StaticResource [alias=" + alias + ", classpath=" + classpath + ", welcome file=" + welcomeFile + "]";
  }

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String pathInfo = req.getPathInfo();
    String servletPath = req.getServletPath();
    String path = pathInfo == null ? servletPath : servletPath + pathInfo;
    logger.debug("handling path {}, pathInfo={}, servletPath={}", new Object[] { path, pathInfo, servletPath });

    // If the URL points to a "directory", redirect to the welcome file
    if ("/".equals(path) || alias.equals(path) || (alias + "/").equals(path)) {
      String redirectPath;
      if ("/".equals(alias)) {
        redirectPath = "/" + welcomeFile;
      } else {
        redirectPath = alias + "/" + welcomeFile;
      }
      logger.debug("redirecting {} to {}", new String[] { path, redirectPath });
      resp.sendRedirect(redirectPath);
      return;
    }

    // Find and deliver the resource
    String classpathToResource;
    if (pathInfo == null) {
      if (!servletPath.equals(alias)) {
        classpathToResource = classpath + servletPath;
      } else {
        classpathToResource = classpath + "/" + welcomeFile;
      }
    } else {
      classpathToResource = classpath + pathInfo;
    }

    // Make sure we are using an absolute path
    if (!classpathToResource.startsWith("/"))
      classpathToResource = "/" + classpathToResource;

    // Try to load the resource from the regular resources section
    URL url = bundleContext.getBundle().getResource(classpathToResource);

    if (url == null) {
      resp.sendError(404);
      return;
    }
    logger.debug("opening url {} {}", new Object[] { classpathToResource, url });
    InputStream in = null;
    try {
      in = url.openStream();
      String md5 = DigestUtils.md5Hex(in);
      if (md5.equals(req.getHeader("If-None-Match"))) {
        resp.setStatus(304);
        return;
      }
      resp.setHeader("ETag", md5);
    } finally {
      IOUtils.closeQuietly(in);
    }
    String contentType = mimeMap.getContentType(url.getFile());
    if (!"application/octet-stream".equals(contentType)) {
      resp.setHeader("Content-Type", contentType);
    }
    try {
      in = url.openStream();
      IOUtils.copy(in, resp.getOutputStream());
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

}
