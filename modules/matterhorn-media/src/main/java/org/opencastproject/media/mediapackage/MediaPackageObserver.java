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

/**
 * Interface for classes that are observing a media package.
 */
public interface MediaPackageObserver {

  /**
   * This method is called if an element is added to the media package.
   * 
   * @param element
   *          the added element
   */
  void elementAdded(MediaPackageElement element);

  /**
   * This method is called if an element is removed from a media package.
   * 
   * @param element
   *          the removed element
   */
  void elementRemoved(MediaPackageElement element);

  /**
   * This method is called if the media package was moved to another location.
   * 
   * @param mediaPackage
   *          the moved media package
   */
  void packageMoved(MediaPackage mediaPackage);

}
