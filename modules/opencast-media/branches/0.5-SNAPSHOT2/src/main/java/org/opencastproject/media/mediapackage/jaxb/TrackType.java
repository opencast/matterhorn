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

import org.opencastproject.media.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.media.mediapackage.Track;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * <p>
 * Java class for trackType complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name=&quot;trackType&quot;&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base=&quot;{http://www.w3.org/2001/XMLSchema}anyType&quot;&gt;
 *       &lt;sequence&gt;
 *         &lt;element name=&quot;mimetype&quot; type=&quot;{http://www.w3.org/2001/XMLSchema}string&quot;/&gt;
 *         &lt;element name=&quot;url&quot; type=&quot;{http://www.w3.org/2001/XMLSchema}string&quot;/&gt;
 *         &lt;element name=&quot;checksum&quot; type=&quot;{}checksumType&quot;/&gt;
 *         &lt;element name=&quot;duration&quot; type=&quot;{http://www.w3.org/2001/XMLSchema}long&quot;/&gt;
 *         &lt;element name=&quot;video&quot; type=&quot;{}videoType&quot; minOccurs=&quot;0&quot;/&gt;
 *         &lt;element name=&quot;audio&quot; type=&quot;{}audioType&quot; minOccurs=&quot;0&quot;/&gt;
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
@XmlType(name = "trackType", propOrder = { "mimetype", "url", "checksum", "duration", "video", "audio", "tags" })
@XmlRootElement(name = "track")
public class TrackType {
  private static final Logger logger = LoggerFactory.getLogger(TrackType.class);
  @XmlElement(required = true)
  protected String mimetype;
  @XmlElement(required = true)
  protected String url;
  @XmlElement(required = true)
  protected ChecksumType checksum;
  protected long duration = -1;
  protected VideoType video;
  protected AudioType audio;
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
   * Gets the value of the duration property.
   * 
   */
  public long getDuration() {
    return duration;
  }

  /**
   * Sets the value of the duration property.
   * 
   */
  public void setDuration(long value) {
    this.duration = value;
  }

  /**
   * Gets the value of the video property.
   * 
   * @return possible object is {@link VideoType }
   * 
   */
  public VideoType getVideo() {
    return video;
  }

  /**
   * Sets the value of the video property.
   * 
   * @param value
   *          allowed object is {@link VideoType }
   * 
   */
  public void setVideo(VideoType value) {
    this.video = value;
  }

  /**
   * Gets the value of the audio property.
   * 
   * @return possible object is {@link AudioType }
   * 
   */
  public AudioType getAudio() {
    return audio;
  }

  /**
   * Sets the value of the audio property.
   * 
   * @param value
   *          allowed object is {@link AudioType }
   * 
   */
  public void setAudio(AudioType value) {
    this.audio = value;
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

  static JAXBContext jaxbContext = null;
  
  static {
    try {
      jaxbContext = JAXBContext.newInstance("org.opencastproject.media.mediapackage.jaxb", TrackType.class.getClassLoader());
    } catch (JAXBException e) {
      throw new RuntimeException("Unable to initialize JAXB context for the mediapackage.jaxb package");
    }
  }

  public String toXml() throws Exception {
    StringWriter sw = new StringWriter();
    Marshaller marshaller = jaxbContext.createMarshaller();
    marshaller.marshal(this, sw);
    return sw.toString();
  }

  public Track toTrack() throws Exception {
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
            new InputSource(new StringReader(toXml())));
    return (Track)MediaPackageElementBuilderFactory.newInstance().newElementBuilder().elementFromManifest(doc, null);
  }

  public static TrackType fromXml(Document trackXml) throws Exception {
    if(trackXml == null) throw new RuntimeException("trackXml must not be null");
    if(trackXml.getDocumentElement() == null) throw new RuntimeException("trackXml must have a document element");
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    return unmarshaller.unmarshal(trackXml, TrackType.class).getValue();
  }

  public static TrackType valueOf(String xmlString) throws Exception {
    return fromXml(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
            new InputSource(new StringReader(xmlString))));
  }
  
  public static TrackType fromTrack(Track track) throws Exception {
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    doc.appendChild(track.toManifest(doc, null));
    return fromXml(doc);
  }

  @XmlElementWrapper(name="tags")
  @XmlElement(name="tag")
  protected SortedSet<String> tags;

  public SortedSet<String> getTags() {
    if(tags == null) tags = new TreeSet<String>();
    return tags;
  }

  public void setTags(SortedSet<String> tags) {
    this.tags = tags;
  }
}
