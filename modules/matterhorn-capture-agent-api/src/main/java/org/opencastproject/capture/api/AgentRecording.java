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
package org.opencastproject.capture.api;

import org.opencastproject.capture.admin.api.Recording;
import org.opencastproject.media.mediapackage.MediaPackage;

import java.io.File;
import java.util.Properties;

public interface AgentRecording extends Recording {
  /**
   * Gets a property from the local {@code Properties} object
   * @param key The property name
   * @return The property value, or {@code null} if it doesn't exist
   * @see java.util.Properties#getProperty(String)
   */
  public String getProperty(String key);
  
  /**
   * Sets a property in the local {@code Properties} object (by simply calling its own setProperty method)
   * @param  key The property name
   * @param  value The value to be set
   * @return The previous value of the specified key in this property list, or null if it did not have one.
   * @see java.util.Properties#setProperty(String, String)
   */
  public String setProperty(String key, String value);

  /**
   * @return The {@code Properties} object associated with the recording
   */
  public Properties getProperties();

  /**
   * @param props A {@code Properties} object to associate to the recording
   */
  public void setProps(Properties props);

  /**
   * @return The current {@code MediaPackage}
   */
  public MediaPackage getMediaPackage();

  /**
   * @return A {@code File} object pointing to the directory where this recording files are
   */
  public File getDir();
}
