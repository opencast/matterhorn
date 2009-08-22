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

package org.opencastproject.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * This class represents the mime type registry that is responsible for
 * providing resolving mime types through all system components.
 * <p>
 * The registry is initialized from the file <code>MimeTypes.xml</code>.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: MimeTypes.java 1637 2008-12-08 10:44:18Z ced $
 */
public class MimeTypes {

  /** Name of the mime type files */
  public final static String DEFINITION_FILE = "/MimeTypes.xml";

  /** The mime types */
  private static List<MimeType> mimeTypes_ = null;

  /** the logging facility provided by log4j */
  private final static Logger log_ = LoggerFactory.getLogger(MimeType.class);

  /** Common mime types */
  public static MimeType XML = null;
  public static MimeType TEXT = null;
  public static MimeType JPG = null;
  public static MimeType MPEG4 = null;
  public static MimeType MPEG4_AAC = null;
  public static MimeType DV = null;
  public static MimeType MJPEG2000 = null;
  public static MimeType MP3 = null;
  public static MimeType AAC = null;
  public static MimeType CALENDAR = null;
  public static MimeType ZIP = null;
  public static MimeType JAR = null;

  // Initialize common mime types
  static {
    try {
      XML = MimeTypes.parseMimeType("text/xml");
      TEXT = MimeTypes.parseMimeType("text/plain");
      JPG = MimeTypes.parseMimeType("image/jpg");
      MPEG4 = MimeTypes.parseMimeType("video/mp4");
      MPEG4_AAC = MimeTypes.parseMimeType("video/x-m4v");
      DV = MimeTypes.parseMimeType("video/x-dv");
      MJPEG2000 = MimeTypes.parseMimeType("video/mj2");
      MP3 = MimeTypes.parseMimeType("audio/mpeg");
      AAC = MimeTypes.parseMimeType("audio/x-m4a");
      CALENDAR = MimeTypes.parseMimeType("text/calendar");
      ZIP = MimeTypes.parseMimeType("application/zip");
      JAR = MimeTypes.parseMimeType("application/java-archive");
    } catch (UnknownFileTypeException e) {
      log_.error("Error initializing common mime types: " + e.getMessage());
    }
  }

  /**
   * Initializes the mime type registry from the given file.
   */
  static void initFromFile() {
    try {
      mimeTypes_ = new ArrayList<MimeType>();
      String definitions = null;
      InputStream is = MimeTypes.class.getResourceAsStream(DEFINITION_FILE);
      InputStreamReader isr = null;
      StringBuffer buf = new StringBuffer();
      if (is == null)
        throw new FileNotFoundException(DEFINITION_FILE);

      try {
        isr = new InputStreamReader(is);
        char[] chars = new char[1024];
        int count = 0;
        while ((count = isr.read(chars)) > 0) {
          for (int i = 0; i < count; buf.append(chars[i++]))
            ;
        }
        definitions = buf.toString();
      } finally {
        try {
          isr.close();
          is.close();
        } catch (Exception e) {
        }
      }
      SAXParserFactory parserFactory = SAXParserFactory.newInstance();
      SAXParser parser = parserFactory.newSAXParser();
      DefaultHandler handler = new MimeTypeParser(mimeTypes_);
      parser.parse(new InputSource(new StringReader(definitions)), handler);
    } catch (FileNotFoundException e) {
      log_
          .error("Error initializing mime type registry: definition file not found!");
    } catch (IOException e) {
      log_.error("Error initializing mime type registry: " + e.getMessage());
    } catch (ParserConfigurationException e) {
      log_.error("Configuration error while parsing mime type registry: "
          + e.getMessage());
    } catch (SAXException e) {
      log_.error("Error parsing mime type registry: " + e.getMessage());
    }
  }

  /**
   * Initializes the mime type registry with those types that are relevant for
   * matterhorn, e. g. <code>video/dv</code> and <code>video/mj2</code>.
   */
  static void initFromDefaults() {
    MimeType mimeType = null;

    // Plain Text
    mimeType = new MimeType("text", "plain", "txt");
    mimeTypes_.add(mimeType);

    // TYPE_XML
    mimeType = new MimeType("text", "xml", "xml");
    mimeTypes_.add(mimeType);

    // MPEG-4
    mimeType = new MimeType("video", "mp4", "mp4");
    mimeTypes_.add(mimeType);

    // MPEG-4 with AAC Audio
    mimeType = new MimeType("video", "x-m4v", "m4v");
    mimeTypes_.add(mimeType);

    // DV
    mimeType = new MimeType("video", "x-dv", "dv");
    mimeType.addEquivalent("application", "x-dv");
    mimeTypes_.add(mimeType);

    // ISO Motion JPEG 2000
    mimeType = new MimeType("video", "mj2", "mj2");
    mimeType.addSuffix("mjp2");
    mimeTypes_.add(mimeType);

    // MPEG Audio
    mimeType = new MimeType("audio", "mpeg", "mp3");
    mimeTypes_.add(mimeType);

    // AAC Audio
    mimeType = new MimeType("audio", "x-m4a", "m4a");
    mimeTypes_.add(mimeType);

  }

  /**
   * Returns a mime type for the given type and subtype, e. g.
   * <code>video/mj2</code>.
   * <p>
   * If no mime type can be derived a <code>UnknownFileTypeException</code> is
   * thrown.
   * 
   * @param mimeType
   *          the mime type
   * @return the corresponding mime type
   * @throws UnknownFileTypeException
   *           if the mime type is unknown
   */
  public static MimeType parseMimeType(String mimeType)
      throws UnknownFileTypeException {
    if (mimeType == null)
      throw new IllegalArgumentException("Argument 'mimeType' was null");

    // Check if registry has been initialized
    if (mimeTypes_ == null) {
      initFromFile();
    }

    String[] t = mimeType.trim().split("/");
    if (t.length < 2) {
      throw new IllegalArgumentException("Argument 'mimeType' is malformed");
    }

    for (MimeType m : mimeTypes_) {
      if (m.getType().equals(t[0]) && m.getSubtype().equals(t[1]))
        try {
          return m.clone();
        } catch (CloneNotSupportedException e) {
          // MimeTypeImpl.clone() is implemented, so this will never happen.
        }
    }
    throw new UnknownFileTypeException("MimeType " + mimeType + " is unknown");
  }

  /**
   * Returns a mime type for the provided file suffix.
   * <p>
   * For example, if the suffix is <code>mj2</code>, the mime type will be that
   * of a ISO Motion JPEG 2000 document.
   * <p>
   * If no mime type is found for the suffix, a
   * <code>UnknownFileTypeException</code> is thrown.
   * 
   * @param suffix
   *          the file suffix
   * @return the corresponding mime type
   * @throws UnknownFileTypeException
   *           if the suffix does not map to a mime type
   */
  public static MimeType fromSuffix(String suffix)
      throws UnknownFileTypeException {
    if (suffix == null)
      throw new IllegalArgumentException("Argument 'suffix' was null!");

    // Check if registry has been initialized
    if (mimeTypes_ == null) {
      initFromFile();
    }

    for (MimeType m : mimeTypes_) {
      if (m.supportsSuffix(suffix))
        try {
          return m.clone();
        } catch (CloneNotSupportedException e) {
          // clone() is implemented, so this will never happen.
        }
    }
    throw new UnknownFileTypeException("File suffix '" + suffix
        + "' cannot be matched to any mime type");
  }

  /**
   * Returns a mime type for the provided file.
   * <p>
   * This method tries various ways to extract mime type information from the
   * files name or its contents.
   * <p>
   * If no mime type can be derived from either the file name or its contents, a
   * <code>UnknownFileTypeException</code> is thrown.
   * 
   * @param url
   *          the file
   * @return the corresponding mime type
   * @throws UnknownFileTypeException
   *           if the mime type cannot be derived from the file
   */
  public static MimeType fromURL(URL url) throws UnknownFileTypeException {
    if (url == null)
      throw new IllegalArgumentException("Argument 'file' is null");

    MimeType mimeType = null;

    // Extract suffix
    String filename = url.getFile();
    String suffix = null;
    int separatorPos = filename.lastIndexOf('.');
    if (separatorPos > 0 && separatorPos < filename.length() - 1) {
      suffix = filename.substring(separatorPos + 1);
    } else {
      throw new UnknownFileTypeException("Unable to get mime type without suffix");
    }

    // Try to get mime type for file suffix
    try {
      mimeType = fromSuffix(suffix);
      if (mimeType != null)
        return mimeType;
    } catch (UnknownFileTypeException e) {
      throw e;
    }

    // TODO
    // Try to match according to file contents
    // if (mimeType == null) {
    // for (MimeType m : mimeTypes_.values()) {
    // // TODO: Search file contents for mime type using magic bits
    // }
    // }

    throw new UnknownFileTypeException("File '" + url
        + "' cannot be matched to any mime type");
  }

  /**
   * Reads the mime type definitions from the xml file comming with this
   * distribution.
   * 
   * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
   */
  private static class MimeTypeParser extends DefaultHandler {

    /** The mime types */
    private List<MimeType> registry = null;

    /** Element content */
    private StringBuffer content = new StringBuffer();

    /** Type */
    private String type = null;

    /** Description */
    private String description = null;

    /** Extensions, comma separated */
    private String extensions = null;

    /**
     * Creates a new mime type reader.
     * 
     * @param registry
     *          the registry
     */
    MimeTypeParser(List<MimeType> registry) {
      this.registry = registry;
    }

    @Override
    public void characters(char[] ch, int start, int length)
        throws SAXException {
      super.characters(ch, start, length);
      content.append(ch, start, length);
    }

    /**
     * Returns the element content.
     * 
     * @return the element content
     */
    private String getContent() {
      String str = content.toString();
      content = new StringBuffer();
      return str;
    }

    @Override
    public void endElement(String uri, String localName, String name)
        throws SAXException {
      super.endElement(uri, localName, name);

      if ("Type".equals(name)) {
        this.type = getContent();
        return;
      } else if ("Description".equals(name)) {
        this.description = getContent();
        return;
      }
      if ("Extensions".equals(name)) {
        this.extensions = getContent();
        return;
      } else if ("MimeType".equals(name)) {
        String[] t = type.split("/");
        String[] exts = extensions.split(",");
        MimeType mimeType = new MimeType(t[0].trim(), t[1].trim(), exts[0]
            .trim());
        if (description != null)
          mimeType.setDescription(description);
        registry.add(mimeType);
      }
    }

    @Override
    public void warning(SAXParseException e) throws SAXException {
      super.warning(e);
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
      super.error(e);
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
      super.fatalError(e);
    }

  }

}