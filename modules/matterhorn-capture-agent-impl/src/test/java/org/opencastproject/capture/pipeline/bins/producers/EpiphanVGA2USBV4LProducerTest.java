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

import org.opencastproject.capture.impl.CaptureAgentImpl;
import org.opencastproject.capture.pipeline.bins.GStreamerProperties;
import org.opencastproject.capture.pipeline.bins.UnableToCreateElementException;

import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.Pad;
import org.gstreamer.State;
import org.gstreamer.elements.AppSrc;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * Test class for {@Link org.opencastproject.capture.pipeline.bins.producers.epiphan.EpiphanVGA2USBV4LProducer}.
 */
public class EpiphanVGA2USBV4LProducerTest extends EpiphanVGA2USBV4LTest {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(EpiphanVGA2USBV4LProducerTest.class);
  
  @Test
  @Ignore
  public void epiphanProducerTest() throws Exception {
    if (!gstreamerInstalled)
      return;

    try {
      EpiphanVGA2USBV4LProducer epiphanBin = new EpiphanVGA2USBV4LProducer(captureDevice, properties);
      Assert.assertNotNull(epiphanBin.getBin());
      Assert.assertNotNull(epiphanBin.deviceBin);
      Assert.assertNotNull(epiphanBin.subBin);
      Assert.assertNotNull(epiphanBin.epiphanPoll);
    } catch (UnableToCreateElementException e) {
      logger.error("testVideoTestSrc in SourceFactoryTest", e);
    }
  }

  @Test
  @Ignore
  public void epiphanProducerFailTest() {
    if (!gstreamerInstalled)
      return;

    try {
      EpiphanVGA2USBV4LProducer epiphanBin = new EpiphanVGA2USBV4LProducer(null, properties);
      Assert.fail();
    } catch (Exception ex) {}
  }

  @Test
  @Ignore
  public void createElementsTest() throws Exception {
    if (!gstreamerInstalled)
      return;
    
    try {
      EpiphanVGA2USBV4LProducer epiphanBin = new EpiphanVGA2USBV4LProducer(captureDevice, properties);
      Assert.assertNotNull(epiphanBin.src);
      Assert.assertTrue(epiphanBin.src instanceof AppSrc);
      Assert.assertNotNull(epiphanBin.identity);
      Assert.assertNotNull(epiphanBin.colorspace);
      Assert.assertNotNull(epiphanBin.videorate);
    } catch (UnableToCreateElementException e) {
      logger.error("testVideoTestSrc in SourceFactoryTest", e);
    }
  }

  @Test
  @Ignore
  public void setElementPropertiesTest() throws Exception {
    if (!gstreamerInstalled)
      return;
    try {
      EpiphanVGA2USBV4LProducer epiphanBin = new EpiphanVGA2USBV4LProducer(captureDevice, properties);
      Assert.assertEquals(epiphanBin.src.get(GStreamerProperties.IS_LIVE), true);
      Assert.assertEquals(epiphanBin.src.get(GStreamerProperties.DO_TIMESTAP), true);
      Assert.assertEquals(epiphanBin.src.get(GStreamerProperties.BLOCK), true);
      Assert.assertEquals(epiphanBin.src.getCaps(), Caps.fromString(epiphanBin.getCaps()));
      Assert.assertEquals(epiphanBin.identity.get(GStreamerProperties.SINGLE_SEGMENT), true);
    } catch (UnableToCreateElementException e) {
      logger.error("testVideoTestSrc in SourceFactoryTest", e);
    }
  }

  @Test
  @Ignore
  public void addElementsToBinTest() throws Exception {
    if (!gstreamerInstalled)
      return;
    try {
      EpiphanVGA2USBV4LProducer epiphanBin = new EpiphanVGA2USBV4LProducer(captureDevice, properties);
      List<Element> elements = epiphanBin.getBin().getElements();
      Assert.assertTrue(elements.size() == 4);
    } catch (UnableToCreateElementException e) {
      logger.error("testVideoTestSrc in SourceFactoryTest", e);
    }
  }

  @Test
  @Ignore
  public void linkElementsTest() throws Exception {
    if (!gstreamerInstalled)
      return;
    try {
      EpiphanVGA2USBV4LProducer epiphanBin = new EpiphanVGA2USBV4LProducer(captureDevice, properties);
      
      // AppSrc -> identity
      Pad pad = epiphanBin.src.getSrcPads().get(0);
      Assert.assertTrue(pad.isLinked());
      Assert.assertTrue(pad.getPeer().getParentElement() == epiphanBin.identity);
  
      // identity -> videorate
      pad = epiphanBin.identity.getSrcPads().get(0);
      Assert.assertTrue(pad.isLinked());
      Assert.assertTrue(pad.getPeer().getParentElement() == epiphanBin.videorate);
  
      // videorate -> collorspace
      pad = epiphanBin.videorate.getSrcPads().get(0);
      Assert.assertTrue(pad.isLinked());
      Assert.assertTrue(pad.getPeer().getParentElement() == epiphanBin.colorspace);
  
      // colorspace -> null
      pad = epiphanBin.colorspace.getSrcPads().get(0);
      Assert.assertTrue(pad.getPeer().getParentElement() == null);
    } catch (UnableToCreateElementException e) {
      logger.error("testVideoTestSrc in SourceFactoryTest", e);
    }
  }

  @Test
  @Ignore
  public void getSrcPadTest() throws Exception {
    if (!gstreamerInstalled)
      return;
    try {
      EpiphanVGA2USBV4LProducer epiphanBin = new EpiphanVGA2USBV4LProducer(captureDevice, properties);
  
      Assert.assertNotNull(epiphanBin.getSrcPad());
      Assert.assertTrue(epiphanBin.getSrcPad() instanceof Pad);
    } catch (UnableToCreateElementException e) {
      logger.error("testVideoTestSrc in SourceFactoryTest", e);
    }
  }

  @Test
  @Ignore
  public void getCapsTest() throws Exception {
    if (!gstreamerInstalled)
      return;
    try {
      EpiphanVGA2USBV4LProducer epiphanBin = new EpiphanVGA2USBV4LProducer(captureDevice, properties);
      String caps = epiphanBin.getCaps();
      Assert.assertNotNull(caps);
      Assert.assertFalse(caps.isEmpty());
      Assert.assertTrue(caps.contains(GStreamerProperties.VIDEO_X_RAW_YUV) &&
              caps.contains(GStreamerProperties.WIDTH) &&
              caps.contains(GStreamerProperties.HEIGHT) &&
              caps.contains(GStreamerProperties.FRAMERATE) &&
              caps.contains("format="));
    } catch (UnableToCreateElementException e) {
      logger.error("testVideoTestSrc in SourceFactoryTest", e);
    }
  }

  @Ignore
  public void startPipeline() throws Exception {
    if (!gstreamerInstalled)
      return;
    try {
      EpiphanVGA2USBV4LProducer epiphanBin = new EpiphanVGA2USBV4LProducer(captureDevice, properties);
  
      // start Bin
      epiphanBin.getBin().setState(State.PLAYING);
  
      State state = epiphanBin.getBin().getState(15 * CaptureAgentImpl.GST_SECOND);
      Assert.assertEquals(state, State.PLAYING);
  
  //    state = epiphanBin.deviceBin.pipeline.getState();
  //    Assert.assertEquals(state, State.PLAYING);
  
      state = ((EpiphanVGA2USBV4LSubAbstractBin)epiphanBin.subBin).bin.getState();
      Assert.assertEquals(state, State.PLAYING);
  
      // stop Bin
      epiphanBin.getBin().setState(State.NULL);
  
      state = epiphanBin.getBin().getState(15 * CaptureAgentImpl.GST_SECOND);
      Assert.assertEquals(state, State.NULL);
  
  //    state = epiphanBin.deviceBin.pipeline.getState();
  //    Assert.assertEquals(state, State.NULL);
      
      state = ((EpiphanVGA2USBV4LSubAbstractBin)epiphanBin.subBin).bin.getState();
      Assert.assertEquals(state, State.NULL);
    } catch (UnableToCreateElementException e) {
      logger.error("testVideoTestSrc in SourceFactoryTest", e);
    }
  }
}
