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
package org.opencastproject.inspection.impl.endpoints;

import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.media.mediapackage.jaxb.MediapackageType;

import java.net.URL;

import javax.jws.WebService;

/**
 * A web service endpoint delegating logic to the {@link MediaInspectionService}
 */
@WebService(name="MediaInspectionService")
public interface MediaInspectionWebService {
  /**
   * Inspect the media file at this URL.
   * 
   * @param url
   * @return
   */
  Track inspect(URL url);
  
  /**
   * A sample method used to test media package JAXB serialization.  Only for testing... this is not for production.
   * 
   * FIXME
   * 
   * @return
   */
  MediapackageType getMediaPackage();
}
