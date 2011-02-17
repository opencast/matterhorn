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
package org.opencastproject.series.endpoint;

import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.opencastproject.series.api.SeriesMetadata;
import org.opencastproject.series.impl.SeriesMetadataImpl;

/**
 * TODO: Comment me!
 *
 */
@XmlType(name="seriesMetadata", namespace="http://series.opencastproject.org")
@XmlRootElement(name="series")
@XmlAccessorType(XmlAccessType.FIELD)
public class SeriesMetadataJaxbImpl {
  String key;
  String value;
  
  public SeriesMetadataJaxbImpl () {
    
  }
  
  public SeriesMetadataJaxbImpl (SeriesMetadata m) {
    key = m.getKey();
    value = m.getValue();
  }

  @Transient
  public SeriesMetadataImpl getSeriesMetadata() {
    return new SeriesMetadataImpl(key,value);
  }
  
  public static SeriesMetadataJaxbImpl valueOf(String xmlString) throws Exception {
    return (SeriesMetadataJaxbImpl) SeriesBuilder.getInstance().parseSeriesMetadataJaxbImpl(xmlString);
  }
  
}
