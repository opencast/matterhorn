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

package org.opencastproject.search.impl;

import org.opencastproject.search.api.MediaSegmentPreview;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * Part of a search result that models the preview url for a video segment.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "preview", namespace = "http://search.opencastproject.org/")
@XmlRootElement(name = "preview", namespace = "http://search.opencastproject.org/")
public class MediaSegmentPreviewImpl implements MediaSegmentPreview {

  /** The preview type **/
  @XmlAttribute(name = "type")
  private String type = null;

  /** The preview url */
  @XmlValue
  private String url = null;

  /**
   * Needed by JAXB.
   */
  public MediaSegmentPreviewImpl() {
  }

  /**
   * Creates a new preview url.
   * 
   * @param url
   *          url to the preview image
   * @param type
   *          preview image type
   */
  public MediaSegmentPreviewImpl(String url, String type) {
    this.url = url;
    this.type = type;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.api.MediaSegmentPreview#getType()
   */
  @Override
  public String getType() {
    return type;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.api.MediaSegmentPreview#getUrl()
   */
  @Override
  public String getUrl() {
    return url;
  }

}
