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

package org.opencastproject.media.mediapackage;

import org.opencastproject.media.mediapackage.identifier.Id;

import org.w3c.dom.Document;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Date;
import java.util.Map;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Interface for a media package, which is a data container moving through the system, containing metadata, tracks and
 * attachments.
 */
@XmlJavaTypeAdapter(MediaPackageImpl.Adapter.class)
public interface MediaPackage extends Cloneable {

  /**
   * Returns the media package identifier.
   * 
   * @return the identifier
   */
  Id getIdentifier();

  void setIdentifier(Id id);

  void setTitle(String title);

  /**
   * Returns the title for the associated series, if any.
   * 
   * @return The series title
   */
  String getSeriesTitle();

  void setSeriesTitle(String seriesTitle);

  /**
   * Returns the title of the episode that this mediapackage represents.
   * 
   * @return The episode title
   */
  String getTitle();

  void addCreator(String creator);

  void removeCreator(String creator);

  /**
   * TODO: Comment me
   * 
   * @return
   */
  String[] getCreators();

  void setSeries(String identifier);

  /**
   * TODO: Comment me
   * 
   * @return
   */
  String getSeries();

  void setLicense(String license);

  /**
   * TODO: Comment me
   * 
   * @return
   */
  String getLicense();

  void addContributor(String contributor);

  void removeContributor(String contributor);

  /**
   * TODO: Comment me
   * 
   * @return
   */
  String[] getContributors();

  void setLanguage(String language);

  /**
   * TODO: Comment me
   * 
   * @return
   */
  String getLanguage();

  void addSubject(String subject);

  void removeSubject(String subject);

  /**
   * TODO: Comment me
   * 
   * @return
   */
  String[] getSubjects();

  void setDate(Date date);

  /**
   * Returns the media package start time.
   * 
   * @return the start time
   */
  Date getDate();

  /**
   * Returns the media package duration in milliseconds. The actual duration is detected from the included tracks, so
   * there is no setter in place.
   * 
   * @return the duration
   */
  long getDuration();

  /**
   * Returns <code>true</code> if the given element is part of the media package.
   * 
   * @param element
   *          the element
   * @return <code>true</code> if the element belongs to the media package
   */
  boolean contains(MediaPackageElement element);

  /**
   * Returns an iteration of the media package elements.
   * 
   * @return the media package elements
   */
  Iterable<MediaPackageElement> elements();

  /**
   * Returns all of the elements.
   * 
   * @return the elements
   */
  MediaPackageElement[] getElements();

  /**
   * Returns the element that is identified by the given reference or <code>null</code> if no such element exists.
   * 
   * @param reference
   *          the reference
   * @return the element
   */
  MediaPackageElement getElementByReference(MediaPackageReference reference);

  /**
   * Returns the element that is identified by the given reference or <code>null</code> if no such element exists.
   * 
   * @param reference
   *          the reference
   * @param includeDerived
   *          <code>true</code> to also include derived elements
   * @return the element
   */
  MediaPackageElement[] getElementsByReference(MediaPackageReference reference, boolean includeDerived);

  /**
   * Returns the element that is identified by the given identifier or <code>null</code> if no such element exists.
   * 
   * @param id
   *          the element identifier
   * @return the element
   */
  MediaPackageElement getElementById(String id);

  /**
   * Returns the elements that are tagged with the given tag or an empty array if no such elements are found.
   * 
   * @param tag
   *          the tag
   * @return the elements
   */
  MediaPackageElement[] getElementsByTag(String tag);

  /**
   * Returns all elements of this media package with the given flavor.
   * 
   * @return the media package elements
   */
  MediaPackageElement[] getElementsByFlavor(MediaPackageElementFlavor flavor);

  /**
   * Returns the track identified by <code>trackId</code> or <code>null</code> if that track doesn't exists.
   * 
   * @param trackId
   *          the track identifier
   * @return the tracks
   */
  Track getTrack(String trackId);

  /**
   * Returns the tracks that are part of this media package.
   * 
   * @return the tracks
   */
  Track[] getTracks();

  /**
   * Returns the tracks that are tagged with the given tag or an empty array if no such tracks are found.
   * 
   * @param tag
   *          the tag
   * @return the tracks
   */
  Track[] getTracksByTag(String tag);

  /**
   * Returns the tracks that are part of this media package and match the given flavor as defined in {@link Track}.
   * 
   * @param flavor
   *          the track's flavor
   * @return the tracks with the specified flavor
   */
  Track[] getTracks(MediaPackageElementFlavor flavor);

  /**
   * Returns the tracks that are part of this media package and are refering to the element identified by
   * <code>reference</code>.
   * 
   * @param reference
   *          the reference
   * @return the tracks with the specified reference
   */
  Track[] getTracks(MediaPackageReference reference);

  /**
   * Returns the tracks that are part of this media package and are refering to the element identified by
   * <code>reference</code>.
   * 
   * @param reference
   *          the reference
   * @param includeDerived
   *          <code>true</code> to also include derived elements
   * @return the tracks with the specified reference
   */
  Track[] getTracks(MediaPackageReference reference, boolean includeDerived);

  /**
   * Returns the tracks that are part of this media package and are refering to the element identified by
   * <code>reference</code>.
   * 
   * @param flavor
   *          the element flavor
   * @param reference
   *          the reference
   * @return the tracks with the specified reference
   */
  Track[] getTracks(MediaPackageElementFlavor flavor, MediaPackageReference reference);

  /**
   * Returns <code>true</code> if the media package contains media tracks of any kind.
   * 
   * @return <code>true</code> if the media package contains tracks
   */
  boolean hasTracks();

  /**
   * Returns the attachment identified by <code>attachmentId</code> or <code>null</code> if that attachment doesn't
   * exists.
   * 
   * @param attachmentId
   *          the attachment identifier
   * @return the attachments
   */
  Attachment getAttachment(String attachmentId);

  /**
   * Returns the attachments that are part of this media package.
   * 
   * @return the attachments
   */
  Attachment[] getAttachments();

  /**
   * Returns the attachments that are tagged with the given tag or an empty array if no such attachments are found.
   * 
   * @param tag
   *          the tag
   * @return the attachments
   */
  Attachment[] getAttachmentsByTag(String tag);

  /**
   * Returns the attachments that are part of this media package and match the specified flavor.
   * 
   * @param flavor
   *          the attachment flavor
   * @return the attachments
   */
  Attachment[] getAttachments(MediaPackageElementFlavor flavor);

  /**
   * Returns the attachments that are part of this media package and are refering to the element identified by
   * <code>reference</code>.
   * 
   * @param reference
   *          the reference
   * @return the attachments with the specified reference
   */
  Attachment[] getAttachments(MediaPackageReference reference);

  /**
   * Returns the attachments that are part of this media package and are refering to the element identified by
   * <code>reference</code>.
   * 
   * @param reference
   *          the reference
   * @param includeDerived
   *          <code>true</code> to also include derived elements
   * @return the attachments with the specified reference
   */
  Attachment[] getAttachments(MediaPackageReference reference, boolean includeDerived);

  /**
   * Returns the attachments that are part of this media package and are refering to the element identified by
   * <code>reference</code>.
   * 
   * @param flavor
   *          the element flavor
   * @param reference
   *          the reference
   * @return the attachments with the specified reference
   */
  Attachment[] getAttachments(MediaPackageElementFlavor flavor, MediaPackageReference reference);

  /**
   * Returns <code>true</code> if the media package contains attachments of any kind.
   * 
   * @return <code>true</code> if the media package contains attachments
   */
  boolean hasAttachments();

  /**
   * Returns the catalog identified by <code>catalogId</code> or <code>null</code> if that catalog doesn't exists.
   * 
   * @param catalogId
   *          the catalog identifier
   * @return the catalogs
   */
  Catalog getCatalog(String catalogId);

  /**
   * Returns the catalogs associated with this media package.
   * 
   * @return the catalogs
   */
  Catalog[] getCatalogs();

  /**
   * Returns the catalogs that are tagged with the given tag or an empty array if no such catalogs are found.
   * 
   * @param tag
   *          the tag
   * @return the catalogs
   */
  Catalog[] getCatalogsByTag(String tag);

  /**
   * Returns the catalogs associated with this media package that matches the specified flavor.
   * 
   * @param flavor
   *          the catalog type
   * @return the media package catalogs
   */
  Catalog[] getCatalogs(MediaPackageElementFlavor flavor);

  /**
   * Returns the catalogs that are part of this media package and are refering to the element identified by
   * <code>reference</code>.
   * 
   * @param reference
   *          the reference
   * @return the catalogs with the specified reference
   */
  Catalog[] getCatalogs(MediaPackageReference reference);

  /**
   * Returns the catalogs that are part of this media package and are refering to the element identified by
   * <code>reference</code>.
   * 
   * @param reference
   *          the reference
   * @param includeDerived
   *          <code>true</code> to also include derived elements
   * @return the catalogs with the specified reference
   */
  Catalog[] getCatalogs(MediaPackageReference reference, boolean includeDerived);

  /**
   * Returns the catalogs that are part of this media package and are refering to the element identified by
   * <code>reference</code>.
   * 
   * @param flavor
   *          the element flavor
   * @param reference
   *          the reference
   * @return the catalogs with the specified reference
   */
  Catalog[] getCatalogs(MediaPackageElementFlavor flavor, MediaPackageReference reference);

  /**
   * Returns <code>true</code> if the media package contains catalogs of any kind.
   * 
   * @return <code>true</code> if the media package contains catalogs
   */
  boolean hasCatalogs();

  /**
   * Returns media package elements that are neither, attachments, catalogs nor tracks.
   * 
   * @return the other media package elements
   */
  MediaPackageElement[] getUnclassifiedElements();

  /**
   * Returns media package elements that are neither, attachments, catalogs nor tracks but have the given element
   * flavor.
   * 
   * @param flavor
   *          the element flavor
   * @return the other media package elements
   */
  MediaPackageElement[] getUnclassifiedElements(MediaPackageElementFlavor flavor);

  /**
   * Returns <code>true</code> if the media package contains unclassified elements.
   * 
   * @return <code>true</code> if the media package contains unclassified elements
   */
  boolean hasUnclassifiedElements();

  /**
   * Returns <code>true</code> if the media package contains unclassified elements matching the specified element type.
   * 
   * @param flavor
   *          element flavor of the unclassified element
   * @return <code>true</code> if the media package contains unclassified elements
   */
  boolean hasUnclassifiedElements(MediaPackageElementFlavor flavor);

  /**
   * Adds an arbitrary {@link URI} to this media package, utilizing a {@link MediaPackageBuilder} to create a suitable
   * media package element out of the url. If the content cannot be recognized as being either a metadata catalog or
   * multimedia track, it is added as an attachment.
   * 
   * @param uri
   *          the element location
   * @throws UnsupportedElementException
   *           if the element is of an unsupported format
   */
  MediaPackageElement add(URI uri) throws UnsupportedElementException;

  /**
   * Adds an arbitrary {@link URI} to this media package, utilizing a {@link MediaPackageBuilder} to create a suitable
   * media package element out of the url. If the content cannot be recognized as being either a metadata catalog or
   * multimedia track, it is added as an attachment.
   * 
   * @param uri
   *          the element location
   * @param type
   *          the element type
   * @param flavor
   *          the element flavor
   * @throws UnsupportedElementException
   *           if the element is of an unsupported format
   */
  MediaPackageElement add(URI uri, MediaPackageElement.Type type, MediaPackageElementFlavor flavor)
          throws UnsupportedElementException;

  /**
   * Adds an arbitrary {@link MediaPackageElement} to this media package.
   * 
   * @param element
   *          the element
   * @throws UnsupportedElementException
   *           if the element is of an unsupported format
   */
  void add(MediaPackageElement element) throws UnsupportedElementException;

  /**
   * Adds a track to this media package, actually <em>moving</em> the underlying file in the filesystem. Use this method
   * <em>only</em> if you do not need the track in its originial place anymore.
   * <p>
   * Depending on the implementation, this method may provide significant performance benefits over copying the track.
   * 
   * @param track
   *          the track
   * @throws UnsupportedElementException
   *           if the track is of an unsupported format
   */
  void add(Track track) throws UnsupportedElementException;

  /**
   * Removes the track from the media package.
   * 
   * @param track
   *          the track
   */
  void remove(Track track);

  /**
   * Adds catalog information to this media package.
   * 
   * @param catalog
   *          the catalog
   * @throws UnsupportedElementException
   *           if the catalog is of an unsupported format
   */
  void add(Catalog catalog) throws UnsupportedElementException;

  /**
   * Removes the catalog from the media package.
   * 
   * @param catalog
   *          the catalog
   */
  void remove(Catalog catalog);

  /**
   * Adds an attachment to this media package.
   * 
   * @param attachment
   *          the attachment
   * @throws UnsupportedElementException
   *           if the attachment is of an unsupported format
   */
  void add(Attachment attachment) throws UnsupportedElementException;

  /**
   * Removes an arbitrary media package element.
   * 
   * @param element
   *          the media package element
   */
  void remove(MediaPackageElement element);

  /**
   * Removes the attachment from the media package.
   * 
   * @param attachment
   *          the attachment
   */
  void remove(Attachment attachment);

  /**
   * Adds an element to this media package that represents a derived version of <code>sourceElement</code>. Examples of
   * a derived element could be an encoded version of a track or a converted version of a time text captions file.
   * <p>
   * This method will add <code>derviedElement</code> to the media package and add a reference to the original element
   * <code>sourceElement</code>. Make sure that <code>derivedElement</code> features the right flavor, so that you are
   * later able to look up derived work using {@link #getDerived(MediaPackageElement, MediaPackageElementFlavor)}.
   * 
   * @param derivedElement
   *          the derived element
   * @param sourceElement
   *          the source element
   * @throws UnsupportedElementException
   *           if for some reason <code>derivedElement</code> cannot be added to the media package
   */
  void addDerived(MediaPackageElement derivedElement, MediaPackageElement sourceElement)
          throws UnsupportedElementException;

  /**
   * Adds an element to this media package that represents a derived version of <code>sourceElement</code>. Examples of
   * a derived element could be an encoded version of a track or a converted version of a time text captions file.
   * <p>
   * This method will add <code>derviedElement</code> to the media package and add a reference to the original element
   * <code>sourceElement</code>. Make sure that <code>derivedElement</code> features the right flavor, so that you are
   * later able to look up derived work using {@link #getDerived(MediaPackageElement, MediaPackageElementFlavor)}.
   * 
   * @param derivedElement
   *          the derived element
   * @param sourceElement
   *          the source element
   * @param properties
   *          properties for the reference that is being created
   * @throws UnsupportedElementException
   *           if for some reason <code>derivedElement</code> cannot be added to the media package
   */
  void addDerived(MediaPackageElement derivedElement, MediaPackageElement sourceElement, Map<String, String> properties)
          throws UnsupportedElementException;

  /**
   * Returns those media package elements that are derivates of <code>sourceElement</code> and feature the flavor
   * <code>derivateFlavor</code>. Using this method, you could easily look up e. g. flash-encoded versions of the
   * presenter track or converted versions of a time text captions file.
   * 
   * @param sourceElement
   *          the original track, catalog or attachment
   * @param derivateFlavor
   *          the derivate flavor you are looking for
   * @return the derivates
   */
  MediaPackageElement[] getDerived(MediaPackageElement sourceElement, MediaPackageElementFlavor derivateFlavor);

  /**
   * Returns the media package's cover or <code>null</code> if no cover is defined.
   * 
   * @return the cover
   */
  Attachment getCover();

  /**
   * Adds a cover to this media package.
   * 
   * @param cover
   *          the cover
   * @throws MediaPackageException
   *           if the cover cannot be accessed
   * @throws UnsupportedElementException
   *           if the cover is of an unsupported format
   */
  void setCover(Attachment cover) throws MediaPackageException, UnsupportedElementException;

  /**
   * Removes the cover from the media package.
   */
  void removeCover();

  /**
   * Adds <code>observer</code> to the list of observers of this media package.
   * 
   * @param observer
   *          the observer
   */
  void addObserver(MediaPackageObserver observer);

  /**
   * Removes <code>observer</code> from the list of observers of this media package.
   * 
   * @param observer
   *          the observer
   */
  void removeObserver(MediaPackageObserver observer);

  /**
   * Saves the media package using the given packager to the output stream.
   * 
   * @param packager
   *          the packager
   * @param out
   *          the output stream
   * @throws IOException
   *           if an error occurs when writing to the package file
   * @throws MediaPackageException
   *           if errors occur while packaging the media package
   */
  void pack(MediaPackagePackager packager, OutputStream out) throws IOException, MediaPackageException;

  /**
   * Verifies the media package consistency by checking the media package elements for mimetypes and checksums.
   * 
   * @throws MediaPackageException
   *           if an error occurs while checking the media package
   */
  void verify() throws MediaPackageException;

  /**
   * Writes an xml representation of this MediaPackage to a string.
   * 
   * @return the media package serialized to a string
   * @throws MediaPackageException
   *           if serializing or reading from a serialized media package fails
   */
  String toXml() throws MediaPackageException;

  /**
   * Writes an xml representation of this MediaPackage to a stream.
   * 
   * @param out
   *          The output stream
   * @param format
   *          Whether to format the output for readability, or not (false gives better performance)
   * @throws MediaPackageException
   *           if serializing or reading from a serialized media package fails
   */
  void toXml(OutputStream out, boolean format) throws MediaPackageException;

  /**
   * Saves the media package, utilizing the serializer when it comes to creating paths from urls.
   * 
   * @param serializer
   *          the media package serializer
   * @throws MediaPackageException
   *           if saving the manifest failed
   */
  Document toXml(MediaPackageSerializer serializer) throws MediaPackageException;

  /**
   * Renames the media package to the new identifier.
   * 
   * @param identifier
   *          the identifier TODO @return <code>true</code> if the media package could be renamed
   */
  void renameTo(Id identifier);

  /**
   * Creates a deep copy of the media package.
   * 
   * @return the cloned media package
   */
  Object clone();

}
