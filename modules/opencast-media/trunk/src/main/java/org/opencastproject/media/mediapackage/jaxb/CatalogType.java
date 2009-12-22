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

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.3 in JDK 1.6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.09.03 at 02:10:42 PM CEST 
//

package org.opencastproject.media.mediapackage.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for catalogType complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name=&quot;catalogType&quot;&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base=&quot;{http://www.w3.org/2001/XMLSchema}anyType&quot;&gt;
 *       &lt;sequence&gt;
 *         &lt;element name=&quot;mimetype&quot; type=&quot;{http://www.w3.org/2001/XMLSchema}string&quot;/&gt;
 *         &lt;element name=&quot;url&quot; type=&quot;{http://www.w3.org/2001/XMLSchema}string&quot;/&gt;
 *         &lt;element name=&quot;checksum&quot; type=&quot;{}checksumType&quot;/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name=&quot;id&quot; type=&quot;{http://www.w3.org/2001/XMLSchema}string&quot; /&gt;
 *       &lt;attribute name=&quot;type&quot; type=&quot;{http://www.w3.org/2001/XMLSchema}string&quot; /&gt;
 *       &lt;attribute name=&quot;ref&quot; type=&quot;{http://www.w3.org/2001/XMLSchema}string&quot; /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "catalogType", propOrder = { "mimetype", "url", "checksum" })
public class CatalogType {

  @XmlElement(required = true)
  protected String mimetype;
  @XmlElement(required = true)
  protected String url;
  @XmlElement(required = true)
  protected ChecksumType checksum;
  @XmlAttribute
  protected String id;
  @XmlAttribute
  protected String type;
  @XmlAttribute
  protected String ref;

  /**
   * Gets the value of the mimetype property.
   * 
   * @return possible object is {@link String }
   * 
   */
  public String getMimetype() {
    return mimetype;
  }

  /**
   * Sets the value of the mimetype property.
   * 
   * @param value
   *          allowed object is {@link String }
   * 
   */
  public void setMimetype(String value) {
    this.mimetype = value;
  }

  /**
   * Gets the value of the url property.
   * 
   * @return possible object is {@link String }
   * 
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets the value of the url property.
   * 
   * @param value
   *          allowed object is {@link String }
   * 
   */
  public void setUrl(String value) {
    this.url = value;
  }

  /**
   * Gets the value of the checksum property.
   * 
   * @return possible object is {@link ChecksumType }
   * 
   */
  public ChecksumType getChecksum() {
    return checksum;
  }

  /**
   * Sets the value of the checksum property.
   * 
   * @param value
   *          allowed object is {@link ChecksumType }
   * 
   */
  public void setChecksum(ChecksumType value) {
    this.checksum = value;
  }

  /**
   * Gets the value of the id property.
   * 
   * @return possible object is {@link String }
   * 
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the value of the id property.
   * 
   * @param value
   *          allowed object is {@link String }
   * 
   */
  public void setId(String value) {
    this.id = value;
  }

  /**
   * Gets the value of the type property.
   * 
   * @return possible object is {@link String }
   * 
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the value of the type property.
   * 
   * @param value
   *          allowed object is {@link String }
   * 
   */
  public void setType(String value) {
    this.type = value;
  }

  /**
   * Gets the value of the ref property.
   * 
   * @return possible object is {@link String }
   * 
   */
  public String getRef() {
    return ref;
  }

  /**
   * Sets the value of the ref property.
   * 
   * @param value
   *          allowed object is {@link String }
   * 
   */
  public void setRef(String value) {
    this.ref = value;
  }
}
