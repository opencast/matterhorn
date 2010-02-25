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
package org.opencastproject.media.mediapackage.selector;

import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.Track;

/**
 * This <code>MediaPackageElementSelector</code> will return zero or one track from the media package by looking at the
 * flavors and following this order:
 * <ul>
 * <li>{@link MediaPackageElements#PRESENTER_TRACK}</li>
 * <li>{@link MediaPackageElements#PRESENTATION_TRACK}</li>
 * <li>{@link MediaPackageElements#DOCUMENTS_TRACK}</li>
 * <li>{@link MediaPackageElements#AUDIENCE_TRACK}</li>
 * <li></li>
 * <ul>
 * 
 * This basically means that if there is a presenter track, this is the one that will be returnd. If not, then the
 * selctor will try to find a presentation track and so on.
 */
public class PresentationFirstSelector extends FlavorPrioritySelector<Track> {

  /**
   * Creates a new presenter first selector.
   */
  public PresentationFirstSelector() {
    addFlavor(MediaPackageElements.PRESENTER_TRACK);
    addFlavor(MediaPackageElements.PRESENTATION_TRACK);
    addFlavor(MediaPackageElements.DOCUMENTS_TRACK);
    addFlavor(MediaPackageElements.AUDIENCE_TRACK);
  }

}
