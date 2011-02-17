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

package org.opencastproject.analysis.api;

import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.remote.api.Receipt;

/**
 * Api for media analysis implementations, aimed at extracting metadata information from audio and audiovisual tracks.
 */
public interface MediaAnalysisService {

  /**
   * Takes the given element and returns a receipt that can be used to get the resulting
   * {@link org.opencastproject.metadata.mpeg7.Mpeg7Catalog}.
   * 
   * @param element
   *          element to analyze
   * @param block
   *          whether to block the calling thread until the analysis is complete
   * @return the metadata
   */
  Receipt analyze(MediaPackageElement element, boolean block) throws MediaAnalysisException;

  /**
   * Gets the receipt with this identifier
   * 
   * @param id
   *          The ID of the receipt
   * @return The receipt, or null if none is found.
   */
  Receipt getReceipt(String id);

  /**
   * Returns the flavor that this media analysis service implementation produces. The flavor will usually be of type
   * <code>mpeg-7</code> and contain a specific subtype such as <code>segments</code> or <code>text</code>.
   * 
   * @return the flavor that is produced by this analysis service
   */
  MediaPackageElementFlavor produces();

  /**
   * Returns a list of catalog flavors that need to be present inside a mediapackage in order to enable this analyzer to
   * successfully deliver results.
   * <p>
   * Media analysis implementations that don't have requirements will return an empty array.
   * 
   * @return the required catalog flavors
   */
  MediaPackageElementFlavor[] requires();

  /**
   * Returns the type of analysis this service performs.
   * 
   * @return The media analysis type
   */
  String getAnalysisType();
}
