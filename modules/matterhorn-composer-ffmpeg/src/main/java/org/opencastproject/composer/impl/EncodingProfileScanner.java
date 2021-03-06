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
package org.opencastproject.composer.impl;

import static org.opencastproject.util.ReadinessIndicator.ARTIFACT;

import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.EncodingProfile.MediaType;
import org.opencastproject.composer.api.EncodingProfileImpl;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.ReadinessIndicator;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * This manager class tries to read encoding profiles from the classpath.
 */
public class EncodingProfileScanner implements ArtifactInstaller {

  /** Prefix for encoding profile property keys **/
  private static final String PROP_PREFIX = "profile.";

  /* Property names */
  private static final String PROP_NAME = ".name";
  private static final String PROP_APPLICABLE = ".input";
  private static final String PROP_OUTPUT = ".output";
  private static final String PROP_SUFFIX = ".suffix";
  private static final String PROP_MIMETYPE = ".mimetype";

  /** OSGi bundle context */
  private BundleContext bundleCtx = null;

  /** Sum of profiles files currently installed */
  private int sumInstalledFiles = 0;

  /** The profiles map */
  private Map<String, EncodingProfile> profiles = new HashMap<String, EncodingProfile>();

  /** The logging instance */
  private static final Logger logger = LoggerFactory.getLogger(EncodingProfileScanner.class);

  /**
   * Returns the list of profiles.
   * 
   * @return the profile definitions
   */
  public Map<String, EncodingProfile> getProfiles() {
    return profiles;
  }

  /**
   * OSGi callback on component activation.
   * 
   * @param ctx
   *          the bundle context
   */
  void activate(BundleContext ctx) {
    this.bundleCtx = ctx;
  }

  /**
   * Returns the encoding profile for the given identifier or <code>null</code> if no such profile has been configured.
   * 
   * @param id
   *          the profile identifier
   * @return the profile
   */
  public EncodingProfile getProfile(String id) {
    return profiles.get(id);
  }

  /**
   * Returns the list of profiles that are applicable for the given track type.
   * 
   * @return the profile definitions
   */
  public Map<String, EncodingProfile> getApplicableProfiles(MediaType type) {
    Map<String, EncodingProfile> result = new HashMap<String, EncodingProfile>();
    for (Map.Entry<String, EncodingProfile> entry : profiles.entrySet()) {
      EncodingProfile profile = entry.getValue();
      if (profile.isApplicableTo(type)) {
        result.put(entry.getKey(), profile);
      }
    }
    return result;
  }

  /**
   * Reads the profiles from the given set of properties.
   * 
   * @param artifact
   *          the properties file
   * @return the profiles found in the properties
   */
  Map<String, EncodingProfile> loadFromProperties(File artifact) throws IOException {
    // Format name
    FileInputStream in = null;
    Properties properties = new Properties();
    try {
      in = new FileInputStream(artifact);
      properties.load(in);
    } finally {
      IOUtils.closeQuietly(in);
    }

    // Find list of formats in properties
    List<String> profileNames = new ArrayList<String>();
    for (Object fullKey : properties.keySet()) {
      String key = fullKey.toString();
      if (key.startsWith(PROP_PREFIX) && key.endsWith(PROP_NAME)) {
        int separatorLocation = fullKey.toString().lastIndexOf('.');
        key = key.substring(PROP_PREFIX.length(), separatorLocation);
        if (!profileNames.contains(key)) {
          profileNames.add(key);
        } else {
          throw new ConfigurationException("Found duplicate definition for encoding profile '" + key + "'");
        }
      }
    }

    // Load the formats
    Map<String, EncodingProfile> profiles = new HashMap<String, EncodingProfile>();
    for (String profileId : profileNames) {
      logger.debug("Enabling media format " + profileId);
      EncodingProfile profile = loadProfile(profileId, properties, artifact);
      profiles.put(profileId, profile);
    }

    return profiles;
  }

  /**
   * Reads the profile from the given properties
   * 
   * @param profile
   * @param properties
   * @param artifact
   * @return the loaded profile or null if profile
   * @throws RuntimeException
   */
  private EncodingProfile loadProfile(String profile, Properties properties, File artifact)
          throws ConfigurationException {
    String identifier = profile;
    List<String> defaultProperties = new ArrayList<String>(10);

    String name = getDefaultProperty(profile, PROP_NAME, properties, defaultProperties);
    if (name == null || "".equals(name))
      throw new ConfigurationException("Distribution profile '" + profile + "' is missing a name (" + PROP_NAME
              + "). (Check web.xml profiles.)");

    EncodingProfileImpl df = new EncodingProfileImpl(identifier, name, artifact);

    // Output Type
    String type = getDefaultProperty(profile, PROP_OUTPUT, properties, defaultProperties);
    if (StringUtils.isBlank(type))
      throw new ConfigurationException("Output type (" + PROP_OUTPUT + ") of profile '" + profile + "' is missing");
    try {
      df.setOutputType(MediaType.parseString(StringUtils.trimToEmpty(type)));
    } catch (IllegalArgumentException e) {
      throw new ConfigurationException("Output type (" + PROP_OUTPUT + ") '" + type + "' of profile '" + profile
              + "' is unknwon");
    }

    // Suffix
    String suffixObj = getDefaultProperty(profile, PROP_SUFFIX, properties, defaultProperties);
    if (StringUtils.isBlank(suffixObj))
      throw new ConfigurationException("Suffix (" + PROP_SUFFIX + ") of profile '" + profile + "' is missing");
    df.setSuffix(StringUtils.trim(suffixObj));

    // Mimetype
    String mimeTypeObj = getDefaultProperty(profile, PROP_MIMETYPE, properties, defaultProperties);
    if (StringUtils.isNotBlank(mimeTypeObj)) {
      MimeType mimeType;
      try {
        mimeType = MimeTypes.parseMimeType(mimeTypeObj);
      } catch (Exception e) {
        throw new ConfigurationException("Mime type (" + PROP_MIMETYPE + ") " + mimeTypeObj
                + " could not be parsed as a mime type! Expressions are not allowed!");
      }
      df.setMimeType(mimeType.toString());
    }

    // Applicable to the following track categories
    String applicableObj = getDefaultProperty(profile, PROP_APPLICABLE, properties, defaultProperties);
    if (StringUtils.isBlank(applicableObj))
      throw new ConfigurationException("Input type (" + PROP_APPLICABLE + ") of profile '" + profile + "' is missing");
    df.setApplicableType(MediaType.parseString(StringUtils.trimToEmpty(applicableObj)));

    // Look for extensions
    String extensionKey = PROP_PREFIX + profile + ".";
    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      String key = entry.getKey().toString();
      if (key.startsWith(extensionKey) && !defaultProperties.contains(key)) {
        String k = key.substring(extensionKey.length());
        String v = StringUtils.trimToEmpty(entry.getValue().toString());
        df.addExtension(k, v);
      }
    }

    return df;
  }

  /**
   * Returns the default property and registers the property key in the list.
   * 
   * @param profile
   *          the profile identifier
   * @param keySuffix
   *          the key suffix, like ".name"
   * @param properties
   *          the properties
   * @param list
   *          the list of default property keys
   * @return the property value or <code>null</code>
   */
  private static String getDefaultProperty(String profile, String keySuffix, Properties properties, List<String> list) {
    StringBuffer buf = new StringBuffer(PROP_PREFIX);
    buf.append(profile);
    buf.append(keySuffix);
    String key = buf.toString();
    list.add(key);
    final String prop = properties.getProperty(key);
    return prop != null ? prop.trim() : prop;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactListener#canHandle(java.io.File)
   */
  @Override
  public boolean canHandle(File artifact) {
    return "encoding".equals(artifact.getParentFile().getName()) && artifact.getName().endsWith(".properties");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactInstaller#install(java.io.File)
   */
  @Override
  public void install(File artifact) throws Exception {
    logger.info("Registering encoding profiles from {}", artifact);
    try {
      Map<String, EncodingProfile> profileMap = loadFromProperties(artifact);
      for (Entry<String, EncodingProfile> entry : profileMap.entrySet()) {
        logger.info("Installed profile {}", entry.getValue().getIdentifier());
        profiles.put(entry.getKey(), entry.getValue());
      }
      sumInstalledFiles++;
    } catch (Exception e) {
      logger.error("Encoding profiles could not be read from {}: {}", artifact, e.getMessage());
    }

    // Determine the number of available profiles
    String[] filesInDirectory = artifact.getParentFile().list(new FilenameFilter() {
      public boolean accept(File arg0, String name) {
        return name.endsWith(".properties");
      }
    });

    // Once all profiles have been loaded, announce readiness
    if (filesInDirectory.length == sumInstalledFiles) {
      Dictionary<String, String> properties = new Hashtable<String, String>();
      properties.put(ARTIFACT, "encodingprofile");
      logger.debug("Indicating readiness of encoding profiles");
      bundleCtx.registerService(ReadinessIndicator.class.getName(), new ReadinessIndicator(), properties);
      logger.info("All {} encoding profiles installed", filesInDirectory.length);
    } else {
      logger.debug("{} of {} encoding profiles installed", sumInstalledFiles, filesInDirectory.length);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactInstaller#uninstall(java.io.File)
   */
  @Override
  public void uninstall(File artifact) throws Exception {
    for (Iterator<EncodingProfile> iter = profiles.values().iterator(); iter.hasNext();) {
      EncodingProfile profile = iter.next();
      if (artifact.equals(profile.getSource())) {
        logger.info("Uninstalling profile {}", profile.getIdentifier());
        iter.remove();
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.felix.fileinstall.ArtifactInstaller#update(java.io.File)
   */
  @Override
  public void update(File artifact) throws Exception {
    uninstall(artifact);
    install(artifact);
  }

}
