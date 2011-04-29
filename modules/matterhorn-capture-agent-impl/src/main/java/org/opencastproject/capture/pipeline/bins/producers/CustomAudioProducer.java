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
package org.opencastproject.capture.pipeline.bins.producers;

import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceNullPointerException;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;
import org.opencastproject.capture.pipeline.bins.UnableToCreateGhostPadsForBinException;
import org.opencastproject.capture.pipeline.bins.UnableToLinkGStreamerElementsException;
import org.opencastproject.capture.pipeline.bins.UnableToSetElementPropertyBecauseElementWasNullException;

import org.gstreamer.Bin;
import org.gstreamer.Element;
import org.gstreamer.Pad;
import org.gstreamer.event.EOSEvent;

import java.util.Properties;

public class CustomAudioProducer extends ProducerBin {

  private static final boolean LINK_UNUSED_GHOST_PADS = true;

  /**
   * The GStreamer commandline syntax can be used to create a custom audio Producer.
   * 
   * @throws UnableToLinkGStreamerElementsException
   *           Not thrown by this class but could be thrown by a parent.
   * @throws UnableToCreateGhostPadsForBinException
   *           Not thrown by this class because the Bin.launch function has a parameter to create the ghostpads
   *           automatically.
   * @throws UnableToSetElementPropertyBecauseElementWasNullException
   *           Since there are no properties to specify could be thrown by a parent.
   * @throws CaptureDeviceNullPointerException
   *           The captureDevice parameter is required to create this custom Producer so an Exception is thrown when it
   *           is null.
   * @throws UnableToCreateElementException
   *           Not thrown by this class.
   * **/
  public CustomAudioProducer(CaptureDevice captureDevice, Properties properties)
          throws UnableToLinkGStreamerElementsException, UnableToCreateGhostPadsForBinException,
          UnableToSetElementPropertyBecauseElementWasNullException, CaptureDeviceNullPointerException,
          UnableToCreateElementException {
    super(captureDevice, properties);
  }

  @Override
  protected Pad getSrcPad() throws UnableToCreateGhostPadsForBinException {
    synchronized (bin) {
      if (bin.getSinks().size() >= 1) {
        return bin.getSinks().get(0).getStaticPad(GStreamerProperties.SRC);
      } else {
        throw new UnableToCreateGhostPadsForBinException("Unable to ghost pads for Custom Audio Source");
      }
    }
  }

  /**
   * Creates the Bin for this class using the GStreamer Java Bin.launch command. Users can specify an audio Producer in
   * this way using a gst-launch like syntax (e.g. "fakesrc ! fakesink")
   **/
  @Override
  protected void createElements() {
    logger.info("Custom Audio Producer is using Pipeline: \"" + captureDeviceProperties.getCustomProducer() + "\"");
    bin = Bin.launch(captureDeviceProperties.getCustomProducer(), LINK_UNUSED_GHOST_PADS);
  }

  /**
   * Need an empty method for createGhostPads because the Bin.launch will create the ghost pads all on its own.
   **/
  @Override
  protected void createGhostPads() throws UnableToCreateGhostPadsForBinException {

  }

  /** Set to true so that only Audio Consumers will be used. **/
  @Override
  public boolean isAudioDevice() {
    return true;
  }
  
  /** 
   * Send an EOS to all of the source elements for this Bin.  
   **/
  @Override
  public void shutdown() {
    for (Element element : bin.getSources()) {
      logger.info("Sending EOS to stop " + element.getName());
      element.sendEvent(new EOSEvent());
    }
  }
}
