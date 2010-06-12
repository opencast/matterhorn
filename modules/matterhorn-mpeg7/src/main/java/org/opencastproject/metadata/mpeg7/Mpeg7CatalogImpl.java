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

package org.opencastproject.metadata.mpeg7;

import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.CatalogImpl;
import org.opencastproject.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.MimeTypes;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * Implements the mpeg-7 metadata container.
 */
public class Mpeg7CatalogImpl extends CatalogImpl implements Mpeg7Catalog {

  /** Serial version UID */
  private static final long serialVersionUID = 5521535164920498997L;

  /** The multimedia content list */
  private HashMap<MultimediaContent.Type, MultimediaContentImpl<? extends MultimediaContentType>> multimediaContent = null;

  /** The default element namespace */
  public static final String NS = "mpeg7";

  /** Flag used for lazy loading */
  protected boolean isLoaded = false;

  protected TrustedHttpClient trustedHttpClient;

  public void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    this.trustedHttpClient = trustedHttpClient;
  }
  
  /** the logging facility provided by log4j */
  final static Logger log_ = LoggerFactory.getLogger(Mpeg7CatalogImpl.class.getName());

  /**
   * Creates a new mpeg-7 metadata container.
   * 
   * @param id
   *          the element identifier within the package
   * @param uri
   *          the document location
   * @param size
   *          the catalog size in bytes
   * @param checksum
   *          the catalog checksum
   */
  protected Mpeg7CatalogImpl(String id, URI uri, long size, Checksum checksum) {
    super(id, Mpeg7Catalog.FLAVOR, uri, size, checksum, MimeTypes.XML);
    multimediaContent = new HashMap<MultimediaContent.Type, MultimediaContentImpl<? extends MultimediaContentType>>();
  }

  /**
   * Creates a new mpeg-7 metadata container.
   * 
   * @param uri
   *          the document location
   * @param size
   *          the catalog size in bytes
   * @param checksum
   *          the catalog checksum
   */
  protected Mpeg7CatalogImpl(URI uri, long size, Checksum checksum) {
    this(null, uri, size, checksum);
  }

  /**
   * Creates a new mpeg-7 metadata container.
   * 
   * @param id
   *          the element identifier withing the package
   */
  protected Mpeg7CatalogImpl(String id) {
    this(id, null, 0, null);
  }

  /**
   * Creates a new mpeg-7 metadata container.
   */
  protected Mpeg7CatalogImpl() {
    this(null, null, 0, null);
  }

  /**
   * @param cat The original catalog to use as a template
   */
  public Mpeg7CatalogImpl(Catalog cat) {
    this(cat.getURI(), cat.getSize(), cat.getChecksum());
    this.setIdentifier(cat.getIdentifier());
    this.mimeType = cat.getMimeType();
    this.tags = new TreeSet<String>();
    for(String t : cat.getTags()) tags.add(t);
    this.flavor = Mpeg7Catalog.FLAVOR;
    if (cat.getReference() != null) {
      this.reference = (MediaPackageReferenceImpl)cat.getReference().clone();
    }
  }

  /**
   * Reads the metadata from the specified file and returns it encapsulated in a {@link Mpeg7Catalog} object.
   * 
   * @param uri
   *          the mpeg7 metadata container file
   * @return the mpeg7 catalog
   */
  public static Mpeg7Catalog fromURI(URI uri) {
    Mpeg7CatalogImpl doc = new Mpeg7CatalogImpl();
    doc.setURI(uri);
    return doc;
  }

  /**
   * Reads the metadata from the specified file and returns it encapsulated in a {@link Mpeg7Catalog} object.
   * 
   * @param file
   *          the mpeg7 metadata container file
   * @return the mpeg7 catalog
   */
  public static Mpeg7Catalog fromFile(File file) {
    return fromURI(file.toURI());
  }

  /**
   * Populates the catalog.
   * 
   * @throws IllegalStateException
   *           if reading the catalog fails
   */
  private void loadCatalogData() throws IllegalStateException {
    Mpeg7Parser parser = new Mpeg7Parser(this);
    InputStream in = null;
    try {
      log_.debug("Reading mpeg-7 catalog content from " + uri);
      if(uri.toURL().getProtocol().startsWith("http")) {
        HttpGet get = new HttpGet(uri);
        in = trustedHttpClient.execute(get).getEntity().getContent();
      } else {
        in = uri.toURL().openStream();
      }
      isLoaded = true;
      parser.parse(in);
    } catch (Exception e) {
      isLoaded = false;
      throw new IllegalStateException("Unable to load mpeg-7 catalog data from " + uri + ":" + e.getMessage(), e);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  /**
   * Creates a new mpeg-7 metadata container file.
   * 
   * @return the new mpeg-7 metadata container
   */
  public static Mpeg7CatalogImpl newInstance() {
    Mpeg7CatalogImpl mpeg7 = new Mpeg7CatalogImpl();
    return mpeg7;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Mpeg7#multimediaContent()
   */
  public Iterator<MultimediaContent<? extends MultimediaContentType>> multimediaContent() {
    if (!isLoaded())
      loadCatalogData();
    List<MultimediaContent<? extends MultimediaContentType>> result = new ArrayList<MultimediaContent<? extends MultimediaContentType>>();
    for (MultimediaContent<? extends MultimediaContentType> o : multimediaContent.values()) {
      result.add(o);
    }
    return result.iterator();
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Mpeg7#getMultimediaContent(org.opencastproject.mediapackage.mpeg7.MultimediaContent.Type)
   */
  public MultimediaContent<? extends MultimediaContentType> getMultimediaContent(MultimediaContent.Type type) {
    if (!isLoaded())
      loadCatalogData();
    return multimediaContent.get(type);
  }

  /**
   * Saves the mpeg-7 metadata container to disk.
   * 
   * @throws ParserConfigurationException
   *           if the xml parser environment is not correctly configured
   * @throws TransformerException
   *           if serialization of the metadata document fails
   * @throws IOException
   *           if an error with catalog file handling occurs
   */
  public Document toXml() throws ParserConfigurationException, TransformerException, IOException {
    if (!isLoaded())
      loadCatalogData();
    Document doc = createDocument();

    // Root element
    Element root = doc.getDocumentElement();

    // Description
    Element descriptionNode = doc.createElement("Description");
    descriptionNode.setAttribute("xsi:type", "ContentEntityType");
    root.appendChild(descriptionNode);

    // MultimediaContent
    for (MultimediaContent<? extends MultimediaContentType> mc : multimediaContent.values()) {
      descriptionNode.appendChild(mc.toXml(doc));
    }

    return doc;
  }

  /**
   * Create a DOM representation of the Mpeg-7.
   */
  private Document createDocument() throws ParserConfigurationException {
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    Element rootElement = doc.createElementNS("urn:mpeg:mpeg7:schema:2001", "Mpeg7");
    rootElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:mpeg7", "urn:mpeg7:schema:2001");
    rootElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xsi",
            "http://www.w3.org/2001/XMLSchema-instance/");
    doc.appendChild(rootElement);
    return doc;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Mpeg7#addAudioContent(java.lang.String,
   *      org.opencastproject.mediapackage.mpeg7.MediaTime,
   *      org.opencastproject.mediapackage.mpeg7.MediaLocator)
   */
  @SuppressWarnings("unchecked")
  public Audio addAudioContent(String id, MediaTime time, MediaLocator locator) {
    if (!isLoaded())
      loadCatalogData();
    this.isLoaded = true;
    MultimediaContentImpl<Audio> content = (MultimediaContentImpl<Audio>) getMultimediaContent(MultimediaContent.Type.AudioType);
    if (content == null) {
      content = new MultimediaContentImpl<Audio>(MultimediaContent.Type.AudioType);
      multimediaContent.put(MultimediaContent.Type.AudioType, content);
    }
    MultimediaContentTypeImpl audio = new MultimediaContentTypeImpl(MultimediaContentType.Type.Audio, id);
    audio.setMediaTime(time);
    audio.setMediaLocator(locator);
    content.add(audio);
    return audio;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Mpeg7#removeAudioContent(java.lang.String)
   */
  public Audio removeAudioContent(String id) {
    if (!isLoaded())
      loadCatalogData();
    this.isLoaded = true;
    MultimediaContentType element = removeContentElement(id, MultimediaContent.Type.AudioType);
    if (element != null)
      return (Audio) element;
    return null;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Mpeg7#hasAudioContent()
   */
  public boolean hasAudioContent() {
    if (!isLoaded())
      loadCatalogData();
    MultimediaContent<?> content = getMultimediaContent(MultimediaContent.Type.AudioType);
    return content != null && content.size() > 0;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Mpeg7#audioContent()
   */
  @SuppressWarnings("unchecked")
  public Iterator<Audio> audioContent() {
    if (!isLoaded())
      loadCatalogData();
    MultimediaContent<Audio> content = (MultimediaContent<Audio>) getMultimediaContent(MultimediaContent.Type.AudioType);
    if (content != null)
      return content.elements();
    return null;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Mpeg7#addVideoContent(java.lang.String,
   *      org.opencastproject.mediapackage.mpeg7.MediaTime,
   *      org.opencastproject.mediapackage.mpeg7.MediaLocator)
   */
  @SuppressWarnings("unchecked")
  public Video addVideoContent(String id, MediaTime time, MediaLocator locator) {
    if (!isLoaded())
      loadCatalogData();
    this.isLoaded = true;
    MultimediaContentImpl<Video> content = (MultimediaContentImpl<Video>) getMultimediaContent(MultimediaContent.Type.VideoType);
    if (content == null) {
      content = new MultimediaContentImpl<Video>(MultimediaContent.Type.VideoType);
      multimediaContent.put(MultimediaContent.Type.VideoType, content);
    }
    MultimediaContentTypeImpl video = new MultimediaContentTypeImpl(MultimediaContentType.Type.Video, id);
    content.add(video);
    video.setMediaTime(time);
    video.setMediaLocator(locator);
    return video;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Mpeg7#removeVideoContent(java.lang.String)
   */
  public Video removeVideoContent(String id) {
    if (!isLoaded())
      loadCatalogData();
    this.isLoaded = true;
    MultimediaContentType element = removeContentElement(id, MultimediaContent.Type.VideoType);
    if (element != null)
      return (Video) element;
    return null;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Mpeg7#hasVideoContent()
   */
  public boolean hasVideoContent() {
    if (!isLoaded())
      loadCatalogData();
    MultimediaContent<?> content = getMultimediaContent(MultimediaContent.Type.VideoType);
    return content != null && content.size() > 0;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Mpeg7#videoContent()
   */
  @SuppressWarnings("unchecked")
  public Iterator<Video> videoContent() {
    if (!isLoaded())
      loadCatalogData();
    MultimediaContent<Video> content = (MultimediaContent<Video>) getMultimediaContent(MultimediaContent.Type.VideoType);
    if (content != null)
      return content.elements();
    return null;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Mpeg7#addAudioVisualContent(java.lang.String,
   *      org.opencastproject.mediapackage.mpeg7.MediaTime,
   *      org.opencastproject.mediapackage.mpeg7.MediaLocator)
   */
  @SuppressWarnings("unchecked")
  public AudioVisual addAudioVisualContent(String id, MediaTime time, MediaLocator locator) {
    if (!isLoaded())
      loadCatalogData();
    this.isLoaded = true;
    MultimediaContentImpl<AudioVisual> content = (MultimediaContentImpl<AudioVisual>) getMultimediaContent(MultimediaContent.Type.AudioVisualType);
    if (content == null) {
      content = new MultimediaContentImpl<AudioVisual>(MultimediaContent.Type.AudioVisualType);
      multimediaContent.put(MultimediaContent.Type.AudioVisualType, content);
    }
    MultimediaContentTypeImpl audioVisual = new MultimediaContentTypeImpl(MultimediaContentType.Type.AudioVisual, id);
    audioVisual.setMediaTime(time);
    audioVisual.setMediaLocator(locator);
    content.add(audioVisual);
    return audioVisual;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Mpeg7#removeAudioVisualContent(java.lang.String)
   */
  public AudioVisual removeAudioVisualContent(String id) {
    if (!isLoaded())
      loadCatalogData();
    this.isLoaded = true;
    MultimediaContentType element = removeContentElement(id, MultimediaContent.Type.AudioVisualType);
    if (element != null)
      return (AudioVisual) element;
    return null;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Mpeg7#hasAudioVisualContent()
   */
  public boolean hasAudioVisualContent() {
    if (!isLoaded())
      loadCatalogData();
    MultimediaContent<?> content = getMultimediaContent(MultimediaContent.Type.AudioVisualType);
    return content != null && content.size() > 0;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Mpeg7#audiovisualContent()
   */
  @SuppressWarnings("unchecked")
  public Iterator<AudioVisual> audiovisualContent() {
    if (!isLoaded())
      loadCatalogData();
    MultimediaContent<AudioVisual> content = (MultimediaContent<AudioVisual>) getMultimediaContent(MultimediaContent.Type.AudioVisualType);
    if (content != null)
      return content.elements();
    return null;
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Mpeg7#getAudioById(java.lang.String)
   */
  @SuppressWarnings("unchecked")
  public Audio getAudioById(String id) {
    if (!isLoaded())
      loadCatalogData();
    MultimediaContent<Audio> content = (MultimediaContent<Audio>) getMultimediaContent(MultimediaContent.Type.AudioType);
    if (content == null)
      return null;
    return content.getElementById(id);
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Mpeg7#getAudioVisualById(java.lang.String)
   */
  @SuppressWarnings("unchecked")
  public AudioVisual getAudioVisualById(String id) {
    if (!isLoaded())
      loadCatalogData();
    MultimediaContent<AudioVisual> content = (MultimediaContent<AudioVisual>) getMultimediaContent(MultimediaContent.Type.AudioVisualType);
    if (content == null)
      return null;
    return content.getElementById(id);
  }

  /**
   * @see org.opencastproject.mediapackage.mpeg7.Mpeg7#getVideoById(java.lang.String)
   */
  @SuppressWarnings("unchecked")
  public Video getVideoById(String id) {
    if (!isLoaded())
      loadCatalogData();
    MultimediaContent<Video> content = (MultimediaContent<Video>) getMultimediaContent(MultimediaContent.Type.VideoType);
    if (content == null)
      return null;
    return content.getElementById(id);
  }

  /**
   * Removes the content element of the specified type with the given identifier.
   * 
   * @param id
   *          the content element identifier
   * @param type
   *          the content type
   * @return the element or <code>null</code>
   */
  private MultimediaContentType removeContentElement(String id, MultimediaContent.Type type) {
    MultimediaContentImpl<? extends MultimediaContentType> content = multimediaContent.get(type);
    if (content != null)
      return content.remove(id);
    return null;
  }

  /**
   * Returns <code>true</code> for catalogs that are loaded or that are
   * in-memory only.
   * 
   * @return <code>true</code> if everything is loaded
   */
  private boolean isLoaded() {
    return isLoaded || uri == null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object clone() {
    Mpeg7CatalogImpl clone = new Mpeg7CatalogImpl(this);
    clone.multimediaContent = (HashMap<MultimediaContent.Type, MultimediaContentImpl<? extends MultimediaContentType>>)this.multimediaContent.clone();
    return clone;
  }
}
