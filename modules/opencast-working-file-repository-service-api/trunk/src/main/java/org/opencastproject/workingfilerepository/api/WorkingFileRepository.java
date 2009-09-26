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
package org.opencastproject.workingfilerepository.api;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * The Working File Repository is a file storage service that supports the lecture capture system.
 * It may be used by other clients, but is neither intended nor required to be used by other systems.
 */
public interface WorkingFileRepository {
  /**
   * Store the data stream under the given media package and element IDs.
   * @param mediaPackageID
   * @param mediaPackageElementID
   * @param in
   */
  void put(String mediaPackageID, String mediaPackageElementID, InputStream in);
  
  /**
   * Stream the file stored under the given media package and element IDs.
   * @param mediaPackageID
   * @param mediaPackageElementID
   * @return
   */
  InputStream get(String mediaPackageID, String mediaPackageElementID);
  
  /**
   * Get the URL of the file stored under the given media package and element IDs.
   * @param mediaPackageID
   * @param mediaPackageElementID
   * @return
   * @throws MalformedURLException 
   */
  URL getURL(String mediaPackageID, String mediaPackageElementID) throws MalformedURLException;
  
  /**
   * Delete the file stored at the given media package and element IDs.
   * @param mediaPackageID
   * @param mediaPackageElementID
   */
  void delete(String mediaPackageID, String mediaPackageElementID);
}
