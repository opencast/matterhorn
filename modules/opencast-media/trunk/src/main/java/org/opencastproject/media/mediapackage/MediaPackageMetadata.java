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
package org.opencastproject.media.mediapackage;

import java.util.Date;

/**
 * Provides metadata for a {@link MediaPackageMetadata}
 *
 */
public interface MediaPackageMetadata {

  /**
   * Returns the title for the associated series, if any.
   * 
   * @return The series title
   */
  String getSeriesTitle();
  
  /**
   * Returns the title of the episode that this mediapackage represents.
   * 
   * @return The episode title
   */
  String getTitle();

  /**
   * The names of the creators.
   * 
   * @return
   */
  String[] getCreators();

  /**
   * The series, if any, that this episode belongs to.
   * @return
   */
  String getSeriesIdentifier();

  /**
   * The license under which this episode is available
   * 
   * @return
   */
  String getLicense();

  /**
   * The contributors
   * 
   * @return
   */
  String[] getContributors();

  /**
   * The language spoken in the media
   * 
   * @return
   */
  String getLanguage();

  /**
   * The subjects
   * 
   * @return
   */
  String[] getSubjects();

  /**
   * Returns the media package start time.
   * 
   * @return the start time
   */
  Date getDate();

}
