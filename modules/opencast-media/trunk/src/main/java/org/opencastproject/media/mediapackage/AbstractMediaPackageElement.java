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

import org.opencastproject.util.Checksum;
import org.opencastproject.util.MimeType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.Serializable;
import java.net.URL;

/**
 * This class provides base functionality for media package elements.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: AbstractMediaPackageElement.java 2905 2009-07-15 16:16:05Z ced $
 */
public abstract class AbstractMediaPackageElement implements MediaPackageElement, Serializable {

  /** Serial version uid */
  private static final long serialVersionUID = 1L;

  /** The element identifier */
  protected String id = null;

  /** The element's type whithin the manifest: Track, Catalog etc. */
  protected Type elementType = null;

  /** The element's description */
  protected String description = null;

  /** The element's mime type, e. g. 'audio/mp3' */
  protected MimeType mimeType = null;

  /** The element's type, e. g. 'track/slide' */
  protected MediaPackageElementFlavor flavor = null;

  /** The element's location */
  protected URL url = null;

  /** Size in bytes */
  protected long size = -1L;

  /** The element's checksum */
  protected Checksum checksum = null;

  /** The parent media package */
  protected MediaPackage mediaPackage = null;

  /** The optional reference to other elements or series */
  protected MediaPackageReference reference = null;

  /**
   * Creates a new media package element.
   * 
   * @param elementType
   *          the type, e. g. Track, Catalog etc.
   * @param flavor
   *          the flavor
   * @param url
   *          the elements location
   */
  protected AbstractMediaPackageElement(Type elementType, MediaPackageElementFlavor flavor, URL url) {
    this(null, elementType, flavor, url, -1, null, null);
  }

  /**
   * Creates a new media package element.
   * 
   * @param elementType
   *          the type, e. g. Track, Catalog etc.
   * @param flavor
   *          the flavor
   * @param url
   *          the elements location
   * @param size
   *          the element size in bytes
   * @param checksum
   *          the element checksum
   * @param mimeType
   *          the element mime type
   */
  protected AbstractMediaPackageElement(Type elementType, MediaPackageElementFlavor flavor, URL url, long size,
          Checksum checksum, MimeType mimeType) {
    this(null, elementType, flavor, url, size, checksum, mimeType);
  }

  /**
   * Creates a new media package element.
   * 
   * @param id
   *          the element identifier withing the package
   * @param elementType
   *          the type, e. g. Track, Catalog etc.
   * @param flavor
   *          the flavor
   * @param url
   *          the elements location
   * @param size
   *          the element size in bytes
   * @param checksum
   *          the element checksum
   * @param mimeType
   *          the element mime type
   */
  protected AbstractMediaPackageElement(String id, Type elementType, MediaPackageElementFlavor flavor, URL url,
          long size, Checksum checksum, MimeType mimeType) {
    if (elementType == null)
      throw new IllegalArgumentException("Argument 'elementType' is null");
    this.id = id;
    this.elementType = elementType;
    this.flavor = flavor;
    this.mimeType = mimeType;
    this.url = url;
    this.checksum = checksum;
  }

  /**
   * Sets the element id.
   * 
   * @param id
   *          the new id
   */
  public void setIdentifier(String id) {
    this.id = id;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#getIdentifier()
   */
  public String getIdentifier() {
    return id;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#getMediaPackage()
   */
  public MediaPackage getMediaPackage() {
    return mediaPackage;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#getElementType()
   */
  public Type getElementType() {
    return elementType;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#getElementDescription()
   */
  public String getElementDescription() {
    return (description != null) ? description : url.toString();
  }

  /**
   * Sets the element name.
   * 
   * @param name
   *          the name
   */
  public void setElementDescription(String name) {
    this.description = name;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#getReference()
   */
  public MediaPackageReference getReference() {
    return reference;
  }

  /**
   * Sets the media package element's reference.
   * 
   * @param reference
   *          the reference
   */
  public void setReference(MediaPackageReference reference) {
    this.reference = reference;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#getURL()
   */
  public URL getURL() {
    return url;
  }

  /**
   * Sets the url that is used to store the media package element.
   * <p>
   * Make sure you know what you are doing, since usually, the media package will take care of the elements locations.
   * 
   * @param url
   *          the elements url
   */
  public void setURL(URL url) {
    this.url = url;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#getChecksum()
   */
  public Checksum getChecksum() {
    return checksum;
  }

  /**
   * Sets the element checksum.
   * 
   * @param checksum
   *          the checksum
   */
  public void setChecksum(Checksum checksum) {
    this.checksum = checksum;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#getMimeType()
   */
  public MimeType getMimeType() {
    return mimeType;
  }

  /**
   * Sets the element mimetype.
   * 
   * @param mimeType
   *          the element mimetype
   */
  public void setMimeType(MimeType mimeType) {
    this.mimeType = mimeType;
  }

  /**
   * Sets the element's flavor.
   * 
   * @param flavor
   *          the flavor
   */
  public void setFlavor(MediaPackageElementFlavor flavor) {
    this.flavor = flavor;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#getFlavor()
   */
  public MediaPackageElementFlavor getFlavor() {
    return flavor;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#getSize()
   */
  public long getSize() {
    return size;
  }

  /**
   * Sets the element size in bytes.
   * 
   * @param size
   *          size in bytes
   */
  public void setSize(long size) {
    this.size = size;
  }

  /**
   * Sets the parent media package.
   * <p>
   * <b>Note</b> This method is only used by the media package and should not be called from elsewhere.
   * 
   * @param mediaPackage
   *          the parent media package
   */
  void setMediaPackage(MediaPackage mediaPackage) {
    this.mediaPackage = mediaPackage;

    // Adjust the reference
    if (reference == null) {
      if (mediaPackage != null)
        reference = new MediaPackageReferenceImpl();
      else
        reference = null;
    }
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#referTo(org.opencastproject.media.mediapackage.MediaPackage)
   */
  public void referTo(MediaPackage mediaPackage) {
    referTo(new MediaPackageReferenceImpl(mediaPackage));
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#referTo(org.opencastproject.media.mediapackage.MediaPackageElement)
   */
  public void referTo(MediaPackageElement element) {
    referTo(new MediaPackageReferenceImpl(element));
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#referTo(org.opencastproject.media.mediapackage.MediaPackageReference)
   */
  public void referTo(MediaPackageReference reference) {
    // TODO: Check reference consistency
    this.reference = reference;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#clearReference()
   */
  public void clearReference() {
    this.reference = null;
  }

  /**
   * @see org.opencastproject.media.mediapackage.MediaPackageElement#verify()
   */
  public void verify() throws MediaPackageException {
    // TODO: Check availability at url
    // TODO: Download (?) and check checksum
    // Checksum c = calculateChecksum();
    // if (checksum != null && !checksum.equals(c)) {
    // throw new MediaPackageException("Checksum mismatch for " + this);
    // }
    // checksum = c;
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(MediaPackageElement o) {
    return url.toString().compareTo(o.getURL().toString());
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof MediaPackageElement) {
      MediaPackageElement e = (MediaPackageElement) obj;
      if (mediaPackage != null && !mediaPackage.equals(e.getMediaPackage()))
        return false;
      return url.equals(e.getURL());
    }
    return false;
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return url.hashCode();
  }

  /**
   * @see org.opencastproject.media.mediapackage.ManifestContributor#toManifest(org.w3c.dom.Document, org.opencastproject.media.mediapackage.MediaPackageSerializer)
   */
  public Node toManifest(Document document, MediaPackageSerializer serializer) {
    Element node = document.createElement(elementType.toString().toLowerCase());
    if(id != null) node.setAttribute("id", id);

    // Flavor
    if (flavor != null)
      node.setAttribute("type", flavor.toString());

    // Reference
    if (reference != null)
      if (mediaPackage == null || !reference.matches(new MediaPackageReferenceImpl(mediaPackage)))
        node.setAttribute("ref", reference.toString());

    // Description
    if (description != null) {
      Element descriptionNode = document.createElement("description");
      descriptionNode.appendChild(document.createTextNode(description));
      node.appendChild(descriptionNode);
    }

    // Url
    Element urlNode = document.createElement("url");
    String urlValue = (serializer != null) ? serializer.encodeURL(url) : url.toExternalForm();
    urlNode.appendChild(document.createTextNode(urlValue));
    node.appendChild(urlNode);

    // MimeType
    if (mimeType != null) {
      Element mimeNode = document.createElement("mimetype");
      mimeNode.appendChild(document.createTextNode(mimeType.toString()));
      node.appendChild(mimeNode);
    }

    // Size
    if (size != -1) {
      Element sizeNode = document.createElement("size");
      sizeNode.appendChild(document.createTextNode(Long.toString(size)));
      node.appendChild(sizeNode);
    }

    // Checksum
    if (checksum != null) {
      Element checksumNode = document.createElement("checksum");
      checksumNode.setAttribute("type", checksum.getType().getName());
      checksumNode.appendChild(document.createTextNode(checksum.getValue()));
      node.appendChild(checksumNode);
    }

    return node;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    String s = (description != null) ? description : url.toString();
    s += " (" + flavor + ", " + mimeType + ")";
    return s.toLowerCase();
  }

}