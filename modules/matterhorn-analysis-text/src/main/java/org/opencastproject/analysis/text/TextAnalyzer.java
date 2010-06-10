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
package org.opencastproject.analysis.text;

import org.opencastproject.analysis.api.MediaAnalysisException;
import org.opencastproject.analysis.api.MediaAnalysisServiceSupport;
import org.opencastproject.analysis.text.ocropus.OcropusTextAnalyzer;
import org.opencastproject.analysis.text.ocropus.OcropusTextFrame;
import org.opencastproject.analysis.text.ocropus.OcropusWord;
import org.opencastproject.media.mediapackage.Attachment;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.metadata.mpeg7.AudioVisual;
import org.opencastproject.metadata.mpeg7.MediaTime;
import org.opencastproject.metadata.mpeg7.MediaTimeImpl;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogImpl;
import org.opencastproject.metadata.mpeg7.SpatioTemporalDecomposition;
import org.opencastproject.metadata.mpeg7.TemporalDecomposition;
import org.opencastproject.metadata.mpeg7.Textual;
import org.opencastproject.metadata.mpeg7.TextualImpl;
import org.opencastproject.metadata.mpeg7.VideoSegment;
import org.opencastproject.metadata.mpeg7.VideoText;
import org.opencastproject.metadata.mpeg7.VideoTextImpl;
import org.opencastproject.remote.api.Receipt;
import org.opencastproject.remote.api.RemoteServiceManager;
import org.opencastproject.remote.api.Receipt.Status;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.opencastproject.workspace.api.Workspace;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Media analysis service that takes takes an image and returns text as extracted from that image.
 */
public class TextAnalyzer extends MediaAnalysisServiceSupport {

  /** Receipt type */
  public static final String RECEIPT_TYPE = "org.opencastproject.analysis.text";

  /** Resulting collection in the working file repository */
  public static final String COLLECTION_ID = "ocrtext";

  /** The configuration key for setting the number of worker threads */
  public static final String CONFIG_THREADS = "textanalyzer.threads";

  /** The default worker thread pool size to use if no configuration is specified */
  public static final int DEFAULT_THREADS = 2;

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(TextAnalyzer.class);

  /** Reference to the receipt service */
  private RemoteServiceManager remoteServiceManager = null;

  /** The repository to store the mpeg7 catalogs */
  private WorkingFileRepository repository = null;

  /** The workspace to ue when retrieving remote media files */
  private Workspace workspace = null;

  /** The http client to use for retrieving protected mpeg7 files */
  protected TrustedHttpClient trustedHttpClient = null;

  /** The executor service used to queue and run jobs */
  private ExecutorService executor = null;

  /** This server's base URL */
  private String serverUrl = null;

  /** Path to the ocropus binary */
  private String ocropusbinary = "/usr/local/bin/ocrocmd";

  /**
   * Creates a new text analzer.
   */
  public TextAnalyzer() {
    super(MediaPackageElements.TEXTS_FLAVOR);
  }

  protected void activate(ComponentContext cc) {
    // set up threading
    int threads = -1;
    String configredThreads = (String) cc.getBundleContext().getProperty(CONFIG_THREADS);
    // try to parse the value as a number. If it fails to parse, there is a config problem so we throw an exception.
    if (configredThreads == null) {
      threads = DEFAULT_THREADS;
    } else {
      threads = Integer.parseInt(configredThreads);
    }
    if (threads < 1) {
      throw new IllegalStateException("The text analyzer needs one or more threads to function.");
    }
    setExecutorThreads(threads);

    // register as a handler for "org.opencastproject.analysis.text" jobs
    serverUrl = (String) cc.getBundleContext().getProperty("org.opencastproject.server.url");
    if (cc.getBundleContext().getProperty("textanalyzer.ocrocmd") != null)
      ocropusbinary = (String) cc.getBundleContext().getProperty("textanalyzer.ocrocmd");
    remoteServiceManager.registerService(RECEIPT_TYPE, serverUrl);
  }

  protected void deactivate() {
    remoteServiceManager.unRegisterService(RECEIPT_TYPE, serverUrl);
  }

  /**
   * Separating this from the activate method so it's easier to test
   */
  void setExecutorThreads(int threads) {
    executor = Executors.newFixedThreadPool(threads);
    logger.info("Thread pool size = {}", threads);
  }

  /**
   * Starts text extraction on the image and returns a receipt containing the final result in the form of a {@link
   * import org.opencastproject.metadata.mpeg7.Mpeg7Catalog}.
   * 
   * @param element
   *          the element to analyze
   * @param mediapackageId
   *          the media package identifier
   * @param elementId
   *          element identifier
   * @param block
   *          <code>true</code> to make this operation synchronous
   * @return a receipt containing the resulting mpeg-7 catalog
   * @throws MediaAnalysisException
   */
  public Receipt analyze(final MediaPackageElement element, boolean block) throws MediaAnalysisException {
    final RemoteServiceManager rs = remoteServiceManager;
    final Receipt receipt = rs.createReceipt(RECEIPT_TYPE);

    final Attachment attachment = (Attachment) element;
    final URI imageUrl = attachment.getURI();

    Runnable command = new Runnable() {
      @SuppressWarnings("unchecked")
      public void run() {
        receipt.setStatus(Status.RUNNING);
        rs.updateReceipt(receipt);

        Mpeg7CatalogImpl mpeg7 = Mpeg7CatalogImpl.newInstance();

        try {

          logger.info("Starting text extraction from {}", imageUrl);

          File imageFile = workspace.get(imageUrl);
          VideoText[] videoTexts = analyze(imageFile, element.getIdentifier());

          // Create a temporal decomposition
          MediaTime mediaTime = new MediaTimeImpl(0, 0);
          AudioVisual avContent = mpeg7.addAudioVisualContent(element.getIdentifier(), mediaTime, null);
          TemporalDecomposition<VideoSegment> temporalDecomposition = (TemporalDecomposition<VideoSegment>) avContent
                  .getTemporalDecomposition();

          // Add a segment
          VideoSegment videoSegment = temporalDecomposition.createSegment("segment-0");
          videoSegment.setMediaTime(mediaTime);

          // Add the video text to the spacio temporal decomposition of the segment
          SpatioTemporalDecomposition spatioTemporalDecomposition = videoSegment.createSpatioTemporalDecomposition(true, false);
          for (VideoText videoText : videoTexts) {
            spatioTemporalDecomposition.addVideoText(videoText);
          }

          logger.info("Text extraction of {} finished, {} words found", attachment.getURI(), videoTexts.length);

          URI uri = uploadMpeg7(mpeg7);
          mpeg7.setURI(uri);
          mpeg7.setFlavor(MediaPackageElements.TEXTS_FLAVOR);
          mpeg7.setReference(new MediaPackageReferenceImpl(element));
          mpeg7.setTrustedHttpClient(trustedHttpClient);

          receipt.setElement(mpeg7);
          receipt.setStatus(Status.FINISHED);
          rs.updateReceipt(receipt);

          logger.info("Finished text extraction of {}", imageUrl);

        } catch (MediaAnalysisException e) {
          receipt.setStatus(Status.FAILED);
          rs.updateReceipt(receipt);
          throw e;
        } catch (Exception e) {
          receipt.setStatus(Status.FAILED);
          rs.updateReceipt(receipt);
          throw new MediaAnalysisException(e);
        }
      }
    };

    Future<?> future = executor.submit(command);
    if (block) {
      try {
        future.get();
      } catch (Exception e) {
        receipt.setStatus(Status.FAILED);
        remoteServiceManager.updateReceipt(receipt);
        throw new MediaAnalysisException(e);
      }
    }
    return receipt;
  }

  /**
   * Stores the mpeg-7 catalog in the working file repository.
   * 
   * @param catalog
   *          the catalog
   * @return the catalog's URI in the working file repository
   * @throws TransformerFactoryConfigurationError
   *           if serializing the catalog to xml fails
   * @throws IOException
   *           if writing the catalog to the working file repository fails
   * @throws ParserConfigurationException
   *           if the xml parser is not set up correctly
   * @throws TransformerException
   *           if creating the xml representation from the dom tree fails
   * @throws URISyntaxException
   *           if the working file repository created an invalid uri
   */
  protected URI uploadMpeg7(Mpeg7CatalogImpl catalog) throws TransformerFactoryConfigurationError,
          TransformerException, ParserConfigurationException, IOException, URISyntaxException {
    // Store the mpeg7 in the file repository, and store the mpeg7 catalog in the receipt
    // Write the catalog to a byte[]
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    transformer.transform(new DOMSource(catalog.toXml()), new StreamResult(out));

    // Store the bytes in the file repository
    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    return repository.putInCollection(COLLECTION_ID, UUID.randomUUID().toString(), in);
  }

  /**
   * Returns the receipt.
   * 
   * @param id
   *          the receipt identifier
   * @return the receipt
   */
  public Receipt getReceipt(String id) {
    return remoteServiceManager.getReceipt(id);
  }

  /**
   * Returns the video text element for the given image.
   * 
   * @param imageFile
   *          the image
   * @param id
   *          the video text id
   * @return the video text found on the image
   * @throws IOException
   *           if accessing the image fails
   */
  protected VideoText[] analyze(File imageFile, String id) throws IOException {
    List<VideoText> videoTexts = new ArrayList<VideoText>();
    OcropusTextAnalyzer analyzer = new OcropusTextAnalyzer(ocropusbinary);
    OcropusTextFrame textFrame = analyzer.analyze(imageFile);
    int i = 1;
    for (OcropusWord word : textFrame.getWords()) {
      VideoText videoText = new VideoTextImpl(id + "-" + i);
      videoText.setBoundary(word.getBoundaries());
      Textual text = new TextualImpl(word.getWord());
      videoText.setText(text);
      videoTexts.add(videoText);
    }
    return videoTexts.toArray(new VideoText[videoTexts.size()]);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.analysis.api.MediaAnalysisService#getAnalysisType()
   */
  @Override
  public String getAnalysisType() {
    return RECEIPT_TYPE;
  }

  /**
   * Sets the receipt service
   * 
   * @param remoteServiceManager
   *          the receipt service
   */
  public void setRemoteServiceManager(RemoteServiceManager remoteServiceManager) {
    this.remoteServiceManager = remoteServiceManager;
  }

  /**
   * Sets the file repository
   * 
   * @param repository
   *          an instance of the working file repository
   */
  public void setFileRepository(WorkingFileRepository repository) {
    this.repository = repository;
  }

  /**
   * Sets the workspace
   * 
   * @param workspace
   *          an instance of the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Sets the trusted http client which is used for authenticated service distribution.
   * 
   * @param trustedHttpClient
   *          the trusted http client
   */
  public void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    this.trustedHttpClient = trustedHttpClient;
  }

}
