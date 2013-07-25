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
package org.opencastproject.workflow.handler;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.functions.Strings.toBool;
import static org.opencastproject.util.data.functions.Strings.trimToNone;
import static org.opencastproject.workflow.handler.EngagePublicationChannel.CHANNEL_ID;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageReference;
import org.opencastproject.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.PublicationImpl;
import org.opencastproject.mediapackage.selector.SimpleElementSelector;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URIUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

/**
 * The workflow definition for handling "engage publication" operations
 */
public class PublishEngageWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(PublishEngageWorkflowOperationHandler.class);

  /** Configuration properties id */
  private static final String SERVER_URL_PROPERTY = "org.opencastproject.server.url";
  private static final String ENGAGE_URL_PROPERTY = "org.opencastproject.engage.ui.url";
  private static final String STREAMING_URL_PROPERTY = "org.opencastproject.streaming.url";

  /** Workflow configuration option keys */
  private static final String DOWNLOAD_SOURCE_FLAVORS = "download-source-flavors";
  private static final String DOWNLOAD_TARGET_SUBFLAVOR = "download-target-subflavor";
  private static final String DOWNLOAD_SOURCE_TAGS = "download-source-tags";
  private static final String DOWNLOAD_TARGET_TAGS = "download-target-tags";
  private static final String STREAMING_SOURCE_TAGS = "streaming-source-tags";
  private static final String STREAMING_TARGET_TAGS = "streaming-target-tags";
  private static final String STREAMING_SOURCE_FLAVORS = "streaming-source-flavors";
  private static final String STREAMING_TARGET_SUBFLAVOR = "streaming-target-subflavor";
  private static final String CHECK_AVAILABILITY = "check-availability";

  /** The streaming distribution service */
  private DistributionService streamingDistributionService = null;

  /** The download distribution service */
  private DownloadDistributionService downloadDistributionService = null;

  /** The search service */
  private SearchService searchService = null;

  /** The base url to engage */
  private URL engageBaseUrl;

  /** Whether to distribute to streaming server */
  private boolean distributeStreaming = false;

  /**
   * Callback for the OSGi declarative services configuration.
   * 
   * @param streamingDistributionService
   *          the streaming distribution service
   */
  protected void setStreamingDistributionService(DistributionService streamingDistributionService) {
    this.streamingDistributionService = streamingDistributionService;
  }

  /**
   * Callback for the OSGi declarative services configuration.
   * 
   * @param downloadDistributionService
   *          the download distribution service
   */
  protected void setDownloadDistributionService(DownloadDistributionService downloadDistributionService) {
    this.downloadDistributionService = downloadDistributionService;
  }

  /**
   * Callback for declarative services configuration that will introduce us to the search service. Implementation
   * assumes that the reference is configured as being static.
   * 
   * @param searchService
   *          an instance of the search service
   */
  protected void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put(DOWNLOAD_SOURCE_FLAVORS,
            "Distribute any mediapackage elements with one of these (comma separated) flavors to download");
    CONFIG_OPTIONS.put(STREAMING_TARGET_SUBFLAVOR,
            "Target subflavor for elements that have been distributed for downloads");
    CONFIG_OPTIONS.put(DOWNLOAD_SOURCE_TAGS,
            "Distribute any mediapackage elements with one of these (comma separated) tags to download.");
    CONFIG_OPTIONS.put(DOWNLOAD_TARGET_TAGS,
            "Add all of these comma separated tags to elements that have been distributed for download.");
    CONFIG_OPTIONS.put(STREAMING_SOURCE_FLAVORS,
            "Distribute any mediapackage elements with one of these (comma separated) flavors to streaming");
    CONFIG_OPTIONS.put(STREAMING_TARGET_SUBFLAVOR,
            "Target subflavor for elements that have been distributed for streaming");
    CONFIG_OPTIONS.put(STREAMING_SOURCE_TAGS,
            "Distribute any mediapackage elements with one of these (comma separated) tags to streaming.");
    CONFIG_OPTIONS.put(STREAMING_TARGET_TAGS,
            "Add all of these comma separated tags to elements that have been distributed for download.");
    CONFIG_OPTIONS.put(CHECK_AVAILABILITY,
            "( true | false ) defaults to true. Check if the distributed download artifact is available at its URL");
  }

  @Override
  protected void activate(ComponentContext cc) {
    super.activate(cc);
    BundleContext bundleContext = cc.getBundleContext();

    // Get engage UI url
    try {
      String engageBaseUrlStr = bundleContext.getProperty(ENGAGE_URL_PROPERTY);
      if (StringUtils.isNotBlank(engageBaseUrlStr)) {
        engageBaseUrl = new URL(engageBaseUrlStr);
      } else {
        engageBaseUrl = new URL(bundleContext.getProperty(SERVER_URL_PROPERTY));
      }
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(e);
    }

    if (StringUtils.isNotBlank(bundleContext.getProperty(STREAMING_URL_PROPERTY)))
      distributeStreaming = true;

    logger.debug("Default engage server url is {}", engageBaseUrl);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTIONS;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  @Override
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running engage publication workflow operation");

    MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    WorkflowOperationInstance op = workflowInstance.getCurrentOperation();

    // Check which tags have been configured
    String downloadSourceTags = StringUtils.trimToEmpty(op.getConfiguration(DOWNLOAD_SOURCE_TAGS));
    String downloadTargetTags = StringUtils.trimToEmpty(op.getConfiguration(DOWNLOAD_TARGET_TAGS));
    String downloadSourceFlavors = StringUtils.trimToEmpty(op.getConfiguration(DOWNLOAD_SOURCE_FLAVORS));
    String downloadTargetSubflavor = StringUtils.trimToNull(op.getConfiguration(DOWNLOAD_TARGET_SUBFLAVOR));
    String streamingSourceTags = StringUtils.trimToEmpty(op.getConfiguration(STREAMING_SOURCE_TAGS));
    String streamingTargetTags = StringUtils.trimToEmpty(op.getConfiguration(STREAMING_TARGET_TAGS));
    String streamingSourceFlavors = StringUtils.trimToEmpty(op.getConfiguration(STREAMING_SOURCE_FLAVORS));
    String streamingTargetSubflavor = StringUtils.trimToNull(op.getConfiguration(STREAMING_TARGET_SUBFLAVOR));
    boolean checkAvailability = option(op.getConfiguration(CHECK_AVAILABILITY)).bind(trimToNone).map(toBool)
            .getOrElse(true);

    String[] sourceDownloadTags = StringUtils.split(downloadSourceTags, ",");
    String[] targetDownloadTags = StringUtils.split(downloadTargetTags, ",");
    String[] sourceDownloadFlavors = StringUtils.split(downloadSourceFlavors, ",");
    String[] sourceStreamingTags = StringUtils.split(streamingSourceTags, ",");
    String[] targetStreamingTags = StringUtils.split(streamingTargetTags, ",");
    String[] sourceStreamingFlavors = StringUtils.split(streamingSourceFlavors, ",");

    if (sourceDownloadTags.length == 0 && sourceDownloadFlavors.length == 0 && sourceStreamingTags.length == 0
            && sourceStreamingFlavors.length == 0) {
      logger.warn("No tags or flavors have been specified, so nothing will be published to the engage publication channel");
      return createResult(mediaPackage, Action.CONTINUE);
    }

    // Parse the download target flavor
    MediaPackageElementFlavor downloadSubflavor = null;
    if (downloadTargetSubflavor != null) {
      try {
        downloadSubflavor = MediaPackageElementFlavor.parseFlavor(downloadTargetSubflavor);
      } catch (IllegalArgumentException e) {
        throw new WorkflowOperationException(e);
      }
    }

    // Parse the streaming target flavor
    MediaPackageElementFlavor streamingSubflavor = null;
    if (streamingTargetSubflavor != null) {
      try {
        streamingSubflavor = MediaPackageElementFlavor.parseFlavor(streamingTargetSubflavor);
      } catch (IllegalArgumentException e) {
        throw new WorkflowOperationException(e);
      }
    }

    // Configure the download element selector
    SimpleElementSelector downloadElementSelector = new SimpleElementSelector();
    for (String flavor : sourceDownloadFlavors) {
      downloadElementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
    }
    for (String tag : sourceDownloadTags) {
      downloadElementSelector.addTag(tag);
    }

    // Configure the streaming element selector
    SimpleElementSelector streamingElementSelector = new SimpleElementSelector();
    for (String flavor : sourceStreamingFlavors) {
      streamingElementSelector.addFlavor(MediaPackageElementFlavor.parseFlavor(flavor));
    }
    for (String tag : sourceStreamingTags) {
      streamingElementSelector.addTag(tag);
    }

    // Select the appropriate elements for download and streaming
    Collection<MediaPackageElement> downloadElements = downloadElementSelector.select(mediaPackage, false);
    Collection<MediaPackageElement> streamingElements = streamingElementSelector.select(mediaPackage, false);

    try {
      Set<String> downloadElementIds = new HashSet<String>();
      Set<String> streamingElementIds = new HashSet<String>();

      // Look for elements matching the tag
      for (MediaPackageElement elem : downloadElements) {
        downloadElementIds.add(elem.getIdentifier());
      }
      for (MediaPackageElement elem : streamingElements) {
        streamingElementIds.add(elem.getIdentifier());
      }

      // Also distribute the security configuration
      // -----
      // This was removed in the meantime by a fix for MH-8515, but could now be used again.
      // -----
      Attachment[] securityAttachments = mediaPackage.getAttachments(MediaPackageElements.XACML_POLICY);
      if (securityAttachments != null && securityAttachments.length > 0) {
        for (Attachment a : securityAttachments) {
          downloadElementIds.add(a.getIdentifier());
          streamingElementIds.add(a.getIdentifier());
        }
      }

      List<Job> jobs = new ArrayList<Job>();
      try {
        for (String elementId : downloadElementIds) {
          Job job = downloadDistributionService.distribute(CHANNEL_ID, mediaPackage, elementId, checkAvailability);
          if (job == null)
            continue;
          jobs.add(job);
        }
        if (distributeStreaming) {
          for (String elementId : streamingElementIds) {
            Job job = streamingDistributionService.distribute(CHANNEL_ID, mediaPackage, elementId);
            if (job == null)
              continue;
            jobs.add(job);
          }
        }
      } catch (DistributionException e) {
        throw new WorkflowOperationException(e);
      }

      if (jobs.size() < 1) {
        logger.info("No mediapackage element was found for distribution to engage");
        return createResult(mediaPackage, Action.CONTINUE);
      }

      // Wait until all distribution jobs have returned
      if (!waitForStatus(jobs.toArray(new Job[jobs.size()])).isSuccess())
        throw new WorkflowOperationException("One of the distribution jobs did not complete successfully");

      logger.debug("Distribute of mediapackage {} completed", mediaPackage);

      try {
        MediaPackage mediaPackageForSearch = getMediaPackageForSearchIndex(mediaPackage, jobs, downloadSubflavor,
                targetDownloadTags, downloadElementIds, streamingSubflavor, streamingElementIds, targetStreamingTags);
        if (!isPublishable(mediaPackageForSearch))
          throw new WorkflowOperationException("Media package does not meet criteria for publication");

        logger.info("Publishing media package {} to search index", mediaPackageForSearch);

        // Create new distribution element
        URI engageUri = URIUtils.resolve(engageBaseUrl.toURI(), "/engage/ui/watch.html?id="
                + mediaPackage.getIdentifier().compact());
        Publication publicationElement = PublicationImpl.publication(UUID.randomUUID().toString(), CHANNEL_ID,
                engageUri, MimeTypes.parseMimeType("text/html"));
        mediaPackage.add(publicationElement);

        // Adding media package to the search index
        Job publishJob = null;
        try {
          publishJob = searchService.add(mediaPackageForSearch);
          if (!waitForStatus(publishJob).isSuccess()) {
            throw new WorkflowOperationException("Mediapackage " + mediaPackageForSearch.getIdentifier()
                    + " could not be published");
          }
        } catch (SearchException e) {
          throw new WorkflowOperationException("Error publishing media package", e);
        } catch (MediaPackageException e) {
          throw new WorkflowOperationException("Error parsing media package", e);
        }

        logger.debug("Publishing of mediapackage {} completed", mediaPackage);
        return createResult(mediaPackage, Action.CONTINUE);
      } catch (Throwable t) {
        if (t instanceof WorkflowOperationException)
          throw (WorkflowOperationException) t;
        else
          throw new WorkflowOperationException(t);
      }
    } catch (Exception e) {
      if (e instanceof WorkflowOperationException) {
        throw (WorkflowOperationException) e;
      } else {
        throw new WorkflowOperationException(e);
      }
    }
  }

  /**
   * Returns a mediapackage that only contains elements that are marked for distribution.
   * 
   * @param current
   *          the current mediapackage
   * @param jobs
   *          the distribution jobs
   * @param downloadSubflavor
   *          flavor to be applied to elements distributed to download
   * @param downloadTargetTags
   *          tags to be applied to elements distributed to downloads
   * @param downloadElementIds
   *          identifiers for elements that have been distributed to downloads
   * @param streamingSubflavor
   *          flavor to be applied to elements distributed to streaming
   * @param streamingElementIds
   *          identifiers for elements that have been distributed to streaming
   * @param streamingTargetTags
   *          tags to be applied to elements distributed to streaming
   * @return the new mediapackage
   */
  protected MediaPackage getMediaPackageForSearchIndex(MediaPackage current, List<Job> jobs,
          MediaPackageElementFlavor downloadSubflavor, String[] downloadTargetTags, Set<String> downloadElementIds,
          MediaPackageElementFlavor streamingSubflavor, Set<String> streamingElementIds, String[] streamingTargetTags)
          throws MediaPackageException, NotFoundException, ServiceRegistryException, WorkflowOperationException {
    MediaPackage mp = (MediaPackage) current.clone();

    // All the jobs have passed, let's update the mediapackage with references to the distributed elements
    List<String> elementsToPublish = new ArrayList<String>();
    Map<String, String> distributedElementIds = new HashMap<String, String>();
    
    for (Job entry : jobs) {
      Job job = serviceRegistry.getJob(entry.getId());
      String sourceElementId = job.getArguments().get(2);
      MediaPackageElement sourceElement = mp.getElementById(sourceElementId);

      // If there is no payload, then the item has not been distributed.
      if (job.getPayload() == null)
        continue;

      MediaPackageElement distributedElement = null;
      try {
        distributedElement = MediaPackageElementParser.getFromXml(job.getPayload());
      } catch (MediaPackageException e) {
        throw new WorkflowOperationException(e);
      }

      // If the job finished successfully, but returned no new element, the channel simply doesn't support this
      // kind of element. So we just keep on looping.
      if (distributedElement == null)
        continue;

      // Make sure the mediapackage is prompted to create a new identifier for this element
      distributedElement.setIdentifier(null);

      // Adjust the flavor and tags for downloadable elements
      if (downloadElementIds.contains(sourceElementId)) {
        if (downloadSubflavor != null) {
          MediaPackageElementFlavor flavor = sourceElement.getFlavor();
          if (flavor != null) {
            MediaPackageElementFlavor newFlavor = new MediaPackageElementFlavor(flavor.getType(),
                    downloadSubflavor.getSubtype());
            distributedElement.setFlavor(newFlavor);
          }
        }
        for (String tag : downloadTargetTags) {
          distributedElement.addTag(tag);
        }
      }

      // Adjust the flavor and tags for streaming elements
      else if (streamingElementIds.contains(sourceElementId)) {
        if (streamingSubflavor != null && streamingElementIds.contains(sourceElementId)) {
          MediaPackageElementFlavor flavor = sourceElement.getFlavor();
          if (flavor != null) {
            MediaPackageElementFlavor newFlavor = new MediaPackageElementFlavor(flavor.getType(),
                    streamingSubflavor.getSubtype());
            distributedElement.setFlavor(newFlavor);
          }
        }
        for (String tag : streamingTargetTags) {
          distributedElement.addTag(tag);
        }
      }

      // Copy references from the source elements to the distributed elements
      MediaPackageReference ref = sourceElement.getReference();
      if (ref != null && mp.getElementByReference(ref) != null) {
        MediaPackageReference newReference = (MediaPackageReference) ref.clone();
        distributedElement.setReference(newReference);
      }

      // Add the new element to the mediapackage
      mp.add(distributedElement);
      elementsToPublish.add(distributedElement.getIdentifier());
      distributedElementIds.put(sourceElementId, distributedElement.getIdentifier());

    }

    // Mark everything that is set for removal
    List<MediaPackageElement> removals = new ArrayList<MediaPackageElement>();
    for (MediaPackageElement element : mp.getElements()) {
      if (!elementsToPublish.contains(element.getIdentifier())) {
        removals.add(element);
      }
    }

    // Translate references to the distributed artifacts
    for (MediaPackageElement element : mp.getElements()) {

      if (removals.contains(element))
        continue;

      // Is the element referencing anything?
      MediaPackageReference reference = element.getReference();
      if (reference == null)
        continue;

      // See if the element has been distributed
      String distributedElementId = distributedElementIds.get(reference.getIdentifier());
      if (distributedElementId == null)
        continue;
      
      MediaPackageReference translatedReference = new MediaPackageReferenceImpl(mp.getElementById(distributedElementId));
      if (reference.getProperties() != null) {
        translatedReference.getProperties().putAll(reference.getProperties());
      }

      // Set the new reference
      element.setReference(translatedReference);

    }

    // Remove everything we don't want to add to publish
    for (MediaPackageElement element : removals) {
      mp.remove(element);
    }
    return mp;
  }

  /** Media package must meet these criteria in order to be published. */
  private boolean isPublishable(MediaPackage mp) {
    boolean hasTitle = !isBlank(mp.getTitle());
    if (!hasTitle)
      logger.warn("Media package does not meet criteria for publication: There is no title");

    boolean hasTracks = mp.hasTracks();
    if (!hasTracks)
      logger.warn("Media package does not meet criteria for publication: There are no tracks");

    return hasTitle && hasTracks;
  }

}
