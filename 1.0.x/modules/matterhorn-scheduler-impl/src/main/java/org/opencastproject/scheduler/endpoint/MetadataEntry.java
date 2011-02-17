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
package org.opencastproject.scheduler.endpoint;

import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * JaxB implementation of the entry of a Hashtable, so that the element can be serialized in the intendet way
 * The Entry now looks <item key="key"><value>value</value></item>
 *
 */
@XmlType(name="MetadataEntry")
public class MetadataEntry implements Entry<String, String> {
  @XmlAttribute(name="key") 
  String key;
  // I would like this to be XmlValue but the JaxB parser the throws an exception
  String value;
  
  public MetadataEntry () {}
  public MetadataEntry (String key, String value) {
    this.key = key;
    this.value = value;
  }
  
  /**
   * {@inheritDoc}
   * @see java.util.Map.Entry#getKey()
   */
  public String getKey() {
    return key;
  }

  /**
   * {@inheritDoc}
   * @see java.util.Map.Entry#getValue()
   */
  public String getValue() {
    return value;
  }

  /**
   * {@inheritDoc}
   * @see java.util.Map.Entry#setValue(java.lang.Object)
   */
  public String setValue(String value) {
    this.value = value;
    return this.value;
  }
  
}
