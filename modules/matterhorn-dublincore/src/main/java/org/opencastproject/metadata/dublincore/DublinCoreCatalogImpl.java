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

package org.opencastproject.metadata.dublincore;

import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.XMLCatalogImpl;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.MimeTypes;

import org.apache.commons.collections.Closure;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerException;

/**
 * Implements the dublin core metadata container.
 */
public class DublinCoreCatalogImpl extends XMLCatalogImpl implements DublinCoreCatalog {

  /** Serial version UID */
  private static final long serialVersionUID = -4240831568101931784L;

  /** the logging facility provided by log4j */
  static final Logger logger = LoggerFactory.getLogger(DublinCoreCatalogImpl.class);

  /** Opencast namespace uri */
  public static final String OC_NS_URI = "http://www.opencastproject.org/matterhorn";

  /** Opencast namespace prefix */
  public static final String OC_NS_PREFIX = "matterhorn";

  /**
   * Namespace name of Dublin Core metadata generated by matterhorn. By default this namespace is the default namespace
   * of xml documents generated by this class.
   */
  public static final String OPENCASTPROJECT_DUBLIN_CORE_NS_URI = "http://www.opencastproject.org/xsd/1.0/dublincore/";

  public static final EName PROPERTY_PROMOTED = new EName(OC_NS_URI, "promoted");
  public static final EName PROPERTY_ADVERTISED = new EName(OC_NS_URI, "advertised");

  /** The dc root document */
  static final EName ROOT_ELEMENT = new EName(OPENCASTPROJECT_DUBLIN_CORE_NS_URI, "dublincore");

  /** The elements */
  private static final Set<EName> PROPERTIES = new HashSet<EName>();

  // Build property set
  static {
    for (Field f : DublinCoreCatalogImpl.class.getFields()) {
      if (Modifier.isStatic(f.getModifiers()) && EName.class.isAssignableFrom(f.getType())
              && f.getName().startsWith("PROPERTY_")) {
        try {
          PROPERTIES.add((EName) f.get(null));
        } catch (IllegalAccessException ignore) {
        }
      }
    }
  }

  /**
   * Creates a new dublin core metadata container.
   * 
   * @param id
   *          the element identifier withing the package
   * @param uri
   *          the document location
   * @param size
   *          the catalog size in bytes
   * @param checksum
   *          the catalog checksum
   */
  protected DublinCoreCatalogImpl(String id, URI uri, MediaPackageElementFlavor flavor, long size, Checksum checksum) {
    super(id, flavor, uri, size, checksum, MimeTypes.XML);
    bindings.bindPrefix(XMLConstants.DEFAULT_NS_PREFIX, OPENCASTPROJECT_DUBLIN_CORE_NS_URI);
    bindings.bindPrefix("xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
    bindings.bindPrefix("dc", ELEMENTS_1_1_NS_URI);
    bindings.bindPrefix("dcterms", TERMS_NS_URI);
    bindings.bindPrefix("oc", OC_NS_URI);
  }

  /**
   * Creates a new dublin core metadata container, defaulting the flavor to {@link MediaPackageElements#EPISODE}.
   * 
   * @param uri
   *          the document location
   * @param size
   *          the catalog size in bytes
   * @param checksum
   *          the catalog checksum
   */
  protected DublinCoreCatalogImpl(URI uri, long size, Checksum checksum) {
    this(null, uri, MediaPackageElements.EPISODE, size, checksum);
  }

  /**
   * Creates a new dublin core metadata container.
   * 
   * @param id
   *          the element identifier withing the package
   */
  protected DublinCoreCatalogImpl(String id) {
    this(id, null, MediaPackageElements.EPISODE, 0, null);
  }

  /**
   * Creates a new dublin core metadata container.
   */
  protected DublinCoreCatalogImpl() {
    this(null, null, MediaPackageElements.EPISODE, 0, null);
  }

  /**
   * Creates a new dublin core metadata container.
   * 
   * @return the new dublin core metadata container
   */
  public static DublinCoreCatalogImpl newInstance() {
    return new DublinCoreCatalogImpl();
  }

  /**
   * Reads the metadata from the specified file and returns it encapsulated in a {@link DublinCoreCatalog} object.
   * 
   * @param in
   *          the stream containing the dublin core metadata
   */
  public DublinCoreCatalogImpl(InputStream in) {
    this();
    loadCatalogData(in);
  }

  /**
   * Loads the dublin core catalog.
   */
  private void loadCatalogData(InputStream in) throws IllegalStateException {
    DublinCoreParser parser = new DublinCoreParser(this);
    try {
      parser.parse(in);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to load dc catalog data:" + e.getMessage(), e);
    }
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer("dublin core");
    if (getIdentifier() != null) {
      buf.append(" '").append(getIdentifier()).append("'");
    }
    return buf.toString();
  }

  @Override
  public void bindPrefix(String prefix, String namespaceName) {
    super.bindPrefix(prefix, namespaceName);
  }

  @SuppressWarnings("unchecked")
  public List<String> get(EName property, final String language) {
    if (property == null)
      throw new IllegalArgumentException("Property name must not be null");
    if (language == null)
      throw new IllegalArgumentException("Language code must not be null");
    if (LANGUAGE_ANY.equals(language)) {
      return (List<String>) CollectionUtils.collect(getValuesAsList(property), new Transformer() {
        public Object transform(Object o) {
          return ((CatalogEntry) o).getValue();
        }
      });
    } else {
      final List<String> values = new ArrayList<String>();
      final boolean langUndef = LANGUAGE_UNDEFINED.equals(language);
      CollectionUtils.forAllDo(getValuesAsList(property), new Closure() {
        public void execute(Object o) {
          CatalogEntry c = (CatalogEntry) o;
          String lang = c.getAttribute(XML_LANG_ATTR);
          if ((langUndef && lang == null) || (language.equals(lang)))
            values.add(c.getValue());
        }
      });
      return values;
    }
  }

  @SuppressWarnings("unchecked")
  public List<DublinCoreValue> get(EName property) {
    if (property == null)
      throw new IllegalArgumentException("Property name must not be null");
    return (List<DublinCoreValue>) CollectionUtils.collect(getValuesAsList(property), new Transformer() {
      public Object transform(Object o) {
        CatalogEntry entry = (CatalogEntry) o;
        String lang = entry.getAttribute(XML_LANG_ATTR);
        return new DublinCoreValue(entry.getValue(), lang != null ? lang : LANGUAGE_UNDEFINED);
      }
    });
  }

  public String getFirst(EName property, String language) {
    if (property == null)
      throw new IllegalArgumentException("Property name must not be null");
    if (language == null)
      throw new IllegalArgumentException("Language code must not be null");

    return getFirstValue(property, language);
  }

  public String getFirst(EName property) {
    if (property == null)
      throw new IllegalArgumentException("Property name must not be null");

    return getFirstValue(property, LANGUAGE_ANY);
  }

  private String getFirstValue(EName property, String language) {
    CatalogEntry entry = null;
    if (LANGUAGE_UNDEFINED.equals(language)) {
      entry = getFirstLocalizedValue(property, null);
    } else if (LANGUAGE_ANY.equals(language)) {
      for (CatalogEntry value : getValuesAsList(property)) {
        entry = value;
        // Prefer values without language information
        if (!value.hasAttribute(XML_LANG_ATTR))
          break;
      }
    } else {
      entry = getFirstLocalizedValue(property, language);
    }
    return entry != null ? entry.getValue() : null;
  }

  public String getAsText(EName property, String language, String delimiter) {
    if (property == null)
      throw new IllegalArgumentException("Property name must not be null");
    if (language == null)
      throw new IllegalArgumentException("Language code must not be null");
    if (delimiter == null)
      delimiter = "";
    List<CatalogEntry> values;
    if (LANGUAGE_UNDEFINED.equals(language)) {
      values = getLocalizedValuesAsList(property, null);
    } else if (LANGUAGE_ANY.equals(language)) {
      values = getValuesAsList(property);
    } else {
      values = getLocalizedValuesAsList(property, language);
    }
    return values.size() > 0 ? StringUtils.join(CollectionUtils.collect(values, new Transformer() {
      public Object transform(Object o) {
        return ((CatalogEntry) o).getValue();
      }
    }), delimiter) : null;
  }

  public Set<String> getLanguages(EName property) {
    if (property == null)
      throw new IllegalArgumentException("Property name must not be null");
    Set<String> languages = new HashSet<String>();
    for (CatalogEntry entry : getValuesAsList(property)) {
      String language = entry.getAttribute(XML_LANG_ATTR);
      if (language != null)
        languages.add(language);
      else
        languages.add(LANGUAGE_UNDEFINED);
    }
    return languages;
  }

  public boolean hasMultipleValues(EName property, String language) {
    if (property == null)
      throw new IllegalArgumentException("Property name must not be null");
    if (language == null)
      throw new IllegalArgumentException("Language code must not be null");
    return hasMultiplePropertyValues(property, language);
  }

  public boolean hasMultipleValues(EName property) {
    if (property == null)
      throw new IllegalArgumentException("Property name must not be null");
    return hasMultiplePropertyValues(property, LANGUAGE_ANY);
  }

  private boolean hasMultiplePropertyValues(EName property, String language) {
    if (LANGUAGE_ANY.equals(language)) {
      return getValuesAsList(property).size() > 1;
    } else {
      int counter = 0;
      for (CatalogEntry entry : getValuesAsList(property)) {
        if (equalLanguage(language, entry.getAttribute(XML_LANG_ATTR)))
          counter++;
        if (counter > 1)
          return true;
      }
      return false;
    }
  }

  public boolean hasValue(EName property, String language) {
    if (property == null)
      throw new IllegalArgumentException("Property name must not be null");
    if (language == null)
      throw new IllegalArgumentException("Language code must not be null");
    return hasPropertyValue(property, language);
  }

  public boolean hasValue(EName property) {
    if (property == null)
      throw new IllegalArgumentException("Property name must not be null");
    return hasPropertyValue(property, LANGUAGE_ANY);
  }

  private boolean hasPropertyValue(EName property, final String language) {
    if (LANGUAGE_ANY.equals(language)) {
      return getValuesAsList(property).size() > 0;
    } else {
      return CollectionUtils.find(getValuesAsList(property), new Predicate() {
        public boolean evaluate(Object o) {
          return equalLanguage(((CatalogEntry) o).getAttribute(XML_LANG_ATTR), language);
        }
      }) != null;
    }
  }

  public void set(EName property, String value, String language) {
    if (property == null)
      throw new IllegalArgumentException("Property name must not be null");
    if (language == null || LANGUAGE_ANY.equals(language))
      throw new IllegalArgumentException("Language code may not be null or LANGUAGE_ANY");
    setValue(property, value, language, null);
  }

  public void set(EName property, String value) {
    if (property == null)
      throw new IllegalArgumentException("Property name must not be null");
    setValue(property, value, LANGUAGE_UNDEFINED, null);
  }

  public void set(EName property, DublinCoreValue value) {
    if (property == null)
      throw new IllegalArgumentException("Property name must not be null");
    if (value != null) {
      setValue(property, value.getValue(), value.getLanguage(), value.getEncodingScheme());
    } else {
      removeValue(property, LANGUAGE_ANY);
    }
  }

  public void set(EName property, List<DublinCoreValue> values) {
    if (property == null)
      throw new IllegalArgumentException("Property name must not be null");
    if (values == null)
      throw new IllegalArgumentException("Values must not be null");
    removeValue(property, LANGUAGE_ANY);
    for (DublinCoreValue v : values)
      add(property, v);
  }

  private void setValue(EName property, String value, String language, EName encodingScheme) {
    if (value == null) {
      // No value, remove the whole element
      removeValue(property, language);
    } else {
      String lang = !LANGUAGE_UNDEFINED.equals(language) ? language : null;
      removeLocalizedValues(property, lang);
      add(property, value, language, encodingScheme);
    }
  }

  public void add(EName property, String value) {
    if (property == null)
      throw new IllegalArgumentException("Property name must not be null");
    if (value == null)
      throw new IllegalArgumentException("Value must not be null");

    add(property, value, LANGUAGE_UNDEFINED, null);
  }

  public void add(EName property, String value, String language) {
    if (property == null)
      throw new IllegalArgumentException("Property name must not be null");
    if (value == null)
      throw new IllegalArgumentException("Value must not be null");
    if (language == null || LANGUAGE_ANY.equals(language))
      throw new IllegalArgumentException("Language code may not be null or LANGUAGE_ANY");

    add(property, value, language, null);
  }

  public void add(EName property, DublinCoreValue value) {
    if (property == null)
      throw new IllegalArgumentException("Property name must not be null");
    if (value == null)
      throw new IllegalArgumentException("Value must not be null");

    add(property, value.getValue(), value.getLanguage(), value.getEncodingScheme());
  }

  private void add(EName property, String value, String language, EName encodingScheme) {
    if (LANGUAGE_UNDEFINED.equals(language)) {
      if (encodingScheme == null) {
        addElement(property, value);
      } else {
        addTypedElement(property, value, encodingScheme);
      }
    } else {
      // Language defined
      if (encodingScheme == null) {
        addLocalizedElement(property, value, language);
      } else {
        addTypedLocalizedElement(property, value, language, encodingScheme);
      }
    }
  }

  public void remove(EName property, String language) {
    if (property == null)
      throw new IllegalArgumentException("Property name must not be null");
    if (language == null)
      throw new IllegalArgumentException("Language code must not be null");
    removeValue(property, language);
  }

  public void remove(EName property) {
    if (property == null)
      throw new IllegalArgumentException("Property name must not be null");
    removeValue(property, LANGUAGE_ANY);
  }

  private void removeValue(EName property, String language) {
    if (LANGUAGE_ANY.equals(language)) {
      removeElement(property);
    } else if (LANGUAGE_UNDEFINED.equals(language)) {
      removeLocalizedValues(property, null);
    } else {
      removeLocalizedValues(property, language);
    }
  }

  public void clear() {
    super.clear();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object clone() {
    DublinCoreCatalogImpl clone = new DublinCoreCatalogImpl();
    clone.data = (Map<EName, List<CatalogEntry>>) ((HashMap<EName, List<CatalogEntry>>) data).clone();
    return clone;
  }

  public Set<EName> getProperties() {
    return PROPERTIES;
  }

  protected boolean equalLanguage(String a, String b) {
    return (a == null && b == LANGUAGE_UNDEFINED) || (a == LANGUAGE_UNDEFINED && b == null) || (a == LANGUAGE_ANY)
            || (b == LANGUAGE_ANY) || (a != null && a.equals(b));
  }

  /**
   * Saves the dublin core metadata container to a dom.
   * 
   * @throws ParserConfigurationException
   *           if the xml parser environment is not correctly configured
   * @throws TransformerException
   *           if serialization of the metadata document fails
   * @throws IOException
   *           if an error with catalog serialization occurs
   */
  public Document toXml() throws ParserConfigurationException, TransformerException, IOException {
    // Create the DOM document
    Document doc = newDocument();
    Element rootElement = doc.createElementNS(ROOT_ELEMENT.getNamespaceName(), toQName(ROOT_ELEMENT));
    doc.appendChild(rootElement);
    // Add the elements
    for (EName property : PROPERTIES) {
      boolean required = property.equals(PROPERTY_TITLE);
      addElement(doc, rootElement, property, required);
    }
    return doc;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.mediapackage.XMLCatalogImpl#toJson()
   */
  @SuppressWarnings("unchecked")
  public String toJson() throws IOException {
    // The top-level json object
    JSONObject json = new JSONObject();

    // First collect all namespaces
    SortedSet<String> namespaces = new TreeSet<String>();
    for (Entry<EName, List<CatalogEntry>> entry : data.entrySet()) {
      namespaces.add(entry.getKey().getNamespaceName());
    }

    // Add a json object for each namespace
    for (String namespace : namespaces) {
      json.put(namespace, new JSONObject());
    }

    // Add the data into the appropriate array
    for (Entry<EName, List<CatalogEntry>> entry : data.entrySet()) {

      EName ename = entry.getKey();
      String namespace = ename.getNamespaceName();
      String localName = ename.getLocalName();

      JSONObject namespaceObject = (JSONObject) json.get(namespace);

      JSONArray localNameArray = (JSONArray) namespaceObject.get(localName);
      if (localNameArray == null) {
        localNameArray = new JSONArray();
        namespaceObject.put(localName, localNameArray);
      }

      for (CatalogEntry catalogEntry : entry.getValue()) {
        Map<EName, String> attributes = catalogEntry.getAttributes();
        if (attributes == null || attributes.isEmpty()) {
          JSONObject value = new JSONObject();
          value.put("value", catalogEntry.getValue());
          localNameArray.add(value);
        } else {
          for (Entry<EName, String> attributeEntry : attributes.entrySet()) {
            JSONObject value = new JSONObject();
            value.put(attributeEntry.getKey().getLocalName(), attributeEntry.getValue());
            value.put("value", catalogEntry.getValue());
            localNameArray.add(value);
          }
        }
      }
    }

    return json.toJSONString();
  }

  /**
   * Adds the dublin core property identified by <code>name</code> to the root node if it's value is different from
   * <code>null</code>.
   * 
   * @param document
   *          the document
   * @param root
   *          the root node
   * @param property
   *          the property name
   * @param required
   *          <code>true</code> if this property is required
   * @return <code>true</code> if the element was added
   * @throws IllegalStateException
   *           if a required property is missing
   */
  private boolean addElement(Document document, Element root, EName property, boolean required)
          throws IllegalStateException {
    CatalogEntry[] elements = getValues(property);
    boolean changed = false;
    if (elements.length > 0) {
      for (CatalogEntry element : elements) {
        changed |= (root.appendChild(element.toXml(document)) != null);
      }
      return changed;
    } else if (required) {
      throw new IllegalStateException("Required property '" + property + "' is missing");
    }
    return false;
  }

  /**
   * Class to parse a dublin core file.
   */
  static class DublinCoreParser extends DefaultHandler {

    /**
     * The manifest
     */
    private DublinCoreCatalogImpl dcDoc = null;

    /**
     * The element content
     */
    private StringBuffer content = new StringBuffer();

    /**
     * The node attributes
     */
    private Attributes attributes = null;

    /**
     * Flag to check if this is not just an arbitrary xml document
     */
    private boolean isDublinCore = false;

    /**
     * Creates a new parser for dublin core documents.
     */
    DublinCoreParser() {
    }

    /**
     * Creates a new parser for dublin core documents. The parsed data will be added to <code>dcCatalog</code>.
     * 
     * @param dcCatalog
     *          the catalog to populate
     */
    DublinCoreParser(DublinCoreCatalogImpl dcCatalog) {
      this.dcDoc = dcCatalog;
    }

    /**
     * Parses the catalog and returns an object representation for it.
     * 
     * @param is
     *          the input stream containing the dublin core catalog
     * @return the catalog representation
     * @throws javax.xml.parsers.ParserConfigurationException
     *           if setting up the parser failed
     * @throws org.xml.sax.SAXException
     *           if an error occured while parsing the document
     * @throws java.io.IOException
     *           if the stream cannot be accessed in a proper way
     * @throws IllegalArgumentException
     *           if the document is not a dublincore document
     */
    public DublinCoreCatalogImpl parse(InputStream is) throws SAXException, IOException, ParserConfigurationException {
      if (dcDoc == null)
        this.dcDoc = new DublinCoreCatalogImpl();
      SAXParserFactory factory = SAXParserFactory.newInstance();
      // REPLAY does not use a DTD here
      factory.setValidating(false);
      factory.setNamespaceAware(true);
      SAXParser parser = factory.newSAXParser();
      parser.parse(is, this);

      // Did we parse a dublin core document?
      if (!isDublinCore)
        throw new IllegalArgumentException("Stream does not contain a dublin core document");

      return dcDoc;
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      super.characters(ch, start, length);
      content.append(ch, start, length);
    }

    /**
     * Returns the element content.
     * 
     * @return the element content
     */
    private String getContent() {
      String str = content.toString().trim();
      content = new StringBuffer();
      return str;
    }

    /**
     * Read <code>type</code> attribute from track or catalog element.
     * 
     * @see org.xml.sax.helpers.DefaultHandler#startElement(String, String, String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
      super.startElement(uri, localName, name, attributes);
      this.attributes = attributes;

      // Make sure this is a dublin core catalog
      // TODO: Improve this test, add namespace awareness
      if (!isDublinCore && ROOT_ELEMENT.getNamespaceName().equals(uri))
        isDublinCore = true;
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {
      super.endElement(uri, localName, name);
      dcDoc.addElement(new EName(uri, localName), getContent(), attributes);
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
      logger.warn("Error parsing dublincore catalog: " + e.getMessage());
      super.error(e);
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
      logger.warn("Fatal error parsing dublincore catalog: " + e.getMessage());
      super.fatalError(e);
    }

    @Override
    public void warning(SAXParseException e) throws SAXException {
      logger.warn("Warning parsing dublincore catalog: " + e.getMessage());
      super.warning(e);
    }
  }
}
