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
package org.opencastproject.ingestui.impl;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.opencastproject.ingestui.api.Metadata;

/**
 * Implementation of the Metadata interface. Represents a simple set of metadata.
 * @author wulff
 *
 */
@XmlRootElement(name = "metadata")
@XmlAccessorType(XmlAccessType.FIELD)
public class MetadataImpl implements Metadata, Serializable {

  private static final long serialVersionUID = 1L;
  public String filename;
  public String submitter;
  public String title;
  public String presenter;
  public String description;
  public String language;
  public boolean distSakai;
  public boolean distYouTube;
  public boolean distITunes;

  public void setFilename(String name) {
    this.filename = name;
  }

  public void setSubmitter(String submitter) {
    this.submitter = submitter;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setPresenter(String presenter) {
    this.presenter = presenter;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public void setDistSakai(boolean b) {
    this.distSakai = b;
  }

  public void setDistYouTube(boolean b) {
    this.distYouTube = b;
  }

  public void setDistITunes(boolean b) {
    this.distITunes = b;
  }

  public String getFilename() {
    return filename;
  }

  public String getSubmitter() {
    return submitter;
  }

  public String getTitle() {
    return title;
  }

  public String getPresenter() {
    return presenter;
  }

  public String getDescription() {
    return description;
  }

  public String getLanguage() {
    return language;
  }

  public boolean getDistSakai() {
    return distSakai;
  }

  public boolean getDistYouTube() {
    return distYouTube;
  }

  public boolean getDistITunes() {
    return distITunes;
  }

}
