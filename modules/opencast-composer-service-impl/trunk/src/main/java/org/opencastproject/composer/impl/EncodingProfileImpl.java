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

package org.opencastproject.composer.impl;

import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.util.PathSupport;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation for encoding profiles.
 */
public class EncodingProfileImpl implements EncodingProfile {

  /** The profile identifier, e. g. flash.http */
  protected String identifier = null;

  /** Format description */
  protected String name = null;

  /** Format type */
  protected MediaType outputType = null;

  /** Suffix */
  protected String suffix = null;

  /** The track types that this profile may be applied to */
  protected MediaType[] applicableTypes = null;

  /** Installation-specific properties */
  protected Map<String, String> extension = null;

  /**
   * Private, since the profile should be created using the static factory method.
   * 
   * @param identifier
   *          the profile identifier
   * @param name
   *          the profile name
   */
  EncodingProfileImpl(String identifier, String name) {
    this.identifier = identifier;
    this.name = name;
  }

  /**
   * {@inheritDoc}
   * @see ch.ethz.replay.core.api.common.media.EncodingProfile#getIdentifier()
   */
  public String getIdentifier() {
    return identifier;
  }

  /**
   * {@inheritDoc}
   * @see ch.ethz.replay.core.api.common.media.EncodingProfile#getName()
   */
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   * @see ch.ethz.replay.core.api.common.media.EncodingProfile#getFilename(ch.ethz.replay.core.api.common.bundle.Track)
   */
  public String getFilename(Track track) {
    return PathSupport.removeFileExtension(track.getURL().getPath()) + suffix;
  }

  /**
   * Sets the output type.
   * 
   * @param type  the output type
   */
  void setType(MediaType type) {
    this.outputType = type;
  }
  
  /**
   * {@inheritDoc}
   * @see ch.ethz.replay.core.api.common.media.EncodingProfile#getType()
   */
  public MediaType getType() {
    return outputType;
  }

  /**
   * Sets the suffix for encoded file names.
   * 
   * @param suffix the file suffix
   */
  void setSuffix(String suffix) {
    this.suffix = suffix;
  }
  
  /**
   * {@inheritDoc}
   * @see ch.ethz.replay.core.api.common.media.EncodingProfile#getSuffix()
   */
  public String getSuffix() {
    return suffix;
  }

  /**
   * Sets the types that are applicable for that profile. For example, an audio only-track
   * hardly be applicable to a jpeg-slide extraction.
   * 
   * @param types applicable track types
   */
  void setApplicableTo(MediaType[] types) {
    this.applicableTypes = types;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.composer.api.EncodingProfile#getApplicableTrackTypes()
   */
  public MediaType[] getApplicableTrackTypes() {
    return applicableTypes;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.composer.api.EncodingProfile#isApplicableTo(org.opencastproject.composer.api.EncodingProfile.MediaType)
   */
  public boolean isApplicableTo(MediaType type) {
    if (applicableTypes == null)
      return false;
    for (MediaType t : applicableTypes) {
      if (t.equals(type))
        return true;
    }
    return false;
  }

  /**
   * Sets the extension properties for that profile. These properties may be intepreted
   * by the encoder engine.
   * 
   * @param extension the extension properties
   */
  void setExtension(Map<String, String> extension) {
    this.extension = extension;
  }
  
  /**
   * {@inheritDoc}
   * @see ch.ethz.replay.core.api.common.media.EncodingProfile#getExtension(java.lang.String)
   */
  public String getExtension(String key) {
    if (extension == null)
      return null;
    return extension.get(key);
  }

  /**
   * {@inheritDoc}
   * @see ch.ethz.replay.core.api.common.media.EncodingProfile#getExtensions()
   */
  public Map<String, String> getExtensions() {
    if (extension == null)
      return Collections.emptyMap();
    return extension;
  }

  /**
   * {@inheritDoc}
   * @see ch.ethz.replay.core.api.common.media.EncodingProfile#hasExtensions()
   */
  public boolean hasExtensions() {
    return extension == null || extension.size() == 0;
  }

  /**
   * Adds the given key-value pair to the extended configuration space of this media profile.
   * 
   * @param key
   *          the property key
   * @param value
   *          the property value
   */
  protected void addExtension(String key, String value) {
    if (key == null)
      throw new IllegalArgumentException("Argument 'key' must not be null");
    if (value == null)
      throw new IllegalArgumentException("Argument 'value' must not be null");
    if (extension == null)
      extension = new HashMap<String, String>();
    extension.put(key, value);
  }

  /**
   * Removes the specified property from the extended configuation space and returns either the property value or
   * <code>null</code> if no property was found.
   * 
   * @param key
   *          the property key
   * @return the property value or <code>null</code>
   */
  protected String removeExtension(String key) {
    if (extension == null)
      return null;
    return extension.remove(key);
  }

  /**
   * {@inheritDoc}
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return identifier.hashCode();
  }

  /**
   * {@inheritDoc}
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof EncodingProfile) {
      EncodingProfile mf = (EncodingProfile) obj;
      return identifier.equals(mf.getIdentifier());
    }
    return false;
  }

  /**
   * {@inheritDoc}
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return identifier;
  }

}