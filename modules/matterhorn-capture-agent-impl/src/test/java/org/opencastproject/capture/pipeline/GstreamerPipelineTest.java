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
package org.opencastproject.capture.pipeline;

import org.opencastproject.capture.api.CaptureParameters;
import org.opencastproject.capture.impl.CaptureAgentImpl;
import org.opencastproject.capture.impl.ConfigurationManager;
import org.opencastproject.capture.impl.XProperties;
import org.opencastproject.capture.pipeline.bins.GStreamerElements;
import org.opencastproject.capture.pipeline.bins.consumers.AudioFilesinkConsumer;
import org.opencastproject.capture.pipeline.bins.consumers.VideoFilesinkConsumer;
import org.opencastproject.capture.pipeline.bins.producers.ProducerFactory.ProducerType;

import org.apache.commons.io.FileUtils;
import org.easymock.classextension.EasyMock;
import org.gstreamer.Bin;
import org.gstreamer.Element;
import org.gstreamer.Gst;
import org.gstreamer.Pipeline;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

/**
 * TODO: Clarify how gstreamer testing should be done.
 */
public class GstreamerPipelineTest {

  private static final Logger logger = LoggerFactory.getLogger(GstreamerPipelineTest.class);

  private static ArrayList<String> devices;

  private Pipeline testPipeline;
  private static boolean gstreamerInstalled;
  private Properties properties;
  private File fakeCaptureDevice = new File("./target", "fakeCapture");
  private int numberOfProducers;
  private String deviceNames;
  private GStreamerPipeline gstreamerPipeline;
  private CaptureAgentImpl captureAgentImpl;
  
  @BeforeClass
  public static void setupClass() {
    try {
      Gst.init();
      gstreamerInstalled = true;
    } catch (Throwable t) {
      logger.warn("Skipping agent tests due to unsatisifed gstreamer installation");
      gstreamerInstalled = false;
    }

    devices = new ArrayList<String>();
    // determine devices to test from command line parameters
    if (System.getProperty("testHauppauge") != null) {
      String hauppaugeDevice = System.getProperty("testHauppauge");
      if (new File(hauppaugeDevice).exists()) {
        devices.add(hauppaugeDevice);
        logger.info("Testing Hauppauge card at: " + hauppaugeDevice);
      } else {
        logger.error("File does not exist: " + hauppaugeDevice);
        Assert.fail();
      }
    }
    if (System.getProperty("testEpiphan") != null) {
      String epiphanDevice = System.getProperty("testEpiphan");
      if (new File(epiphanDevice).exists()) {
        devices.add(epiphanDevice);
        logger.info("Testing Epiphan card at: " + epiphanDevice);
      } else {
        logger.error("File does not exist: " + epiphanDevice);
        Assert.fail();
      }
    }
    if (System.getProperty("testBt878") != null) {
      String bt878Device = System.getProperty("testBt878");
      if (new File(bt878Device).exists()) {
        devices.add(bt878Device);
        logger.info("Testing BT878 card at: " + bt878Device);
      } else {
        logger.error("File does not exist: " + bt878Device);
        Assert.fail();
      }
    }
    if (System.getProperty("testAlsa") != null) {
      String alsaDevice = System.getProperty("testAlsa");
      devices.add(alsaDevice);
      logger.info("Testing ALSA source: " + alsaDevice);
    }
  }

  @AfterClass
  public static void tearDownClass() {
    if (gstreamerInstalled) {
      Gst.deinit();
    }
    devices = null;
  }

  @Before
  public void setupTest() throws IOException {
    long timeout = 5L;
    properties = new Properties();
    File testCaptureDirectory = new File("./target", "pipeline-factory-test");
    FileUtils.forceMkdir(testCaptureDirectory);
    Assert.assertTrue("Can't read from test directory " + testCaptureDirectory.getAbsolutePath(),
            testCaptureDirectory.canRead());
    Assert.assertTrue("Can't write to test directory " + testCaptureDirectory.getAbsolutePath(),
            testCaptureDirectory.canWrite());
    properties.put("org.opencastproject.storage.dir", testCaptureDirectory.getAbsolutePath());
    properties.setProperty(CaptureParameters.RECORDING_ROOT_URL, testCaptureDirectory.getAbsolutePath());
    numberOfProducers = 0;
    deviceNames = "";
    FileUtils.touch(fakeCaptureDevice);
    captureAgentImpl = EasyMock.createNiceMock(CaptureAgentImpl.class);
    gstreamerPipeline = new GStreamerPipeline(captureAgentImpl);
  }

  @After
  public void tearDownTest() {
    properties = null;
    FileUtils.deleteQuietly(new File("./target", "pipeline-factory-test"));
    FileUtils.deleteQuietly(new File("./target", "fakeCapture"));
  }

  @Test
  public void testDevices() {
    // if we have something to test
    if (!devices.isEmpty()) {

      String deviceNames = setupCaptureDevices();
      properties.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, deviceNames);
      testPipeline = gstreamerPipeline.create(properties, false);
      Assert.assertEquals(devices.size(), testPipeline.getElements().size());
    }
  }

  @Test
  public void testNullProperties() {
    boolean success = false;
    try {
      gstreamerPipeline.create(null, false);
    } catch (NullPointerException e) {
      success = true;
    }
    Assert.assertTrue(success);
  }

  @Test
  public void testEmptyProperties() {
    Properties p = new Properties();
    Assert.assertNull(gstreamerPipeline.create(p, false));
  }

  @Test
  public void initDevicesCreatesDevicesSuccessfullyWithCorrectTypes() {

    if (!gstreamerInstalled)
      return;

    ConfigurationManager config = new ConfigurationManager();

    Properties p = null;
    try {
      p = loadProperties("config/capture.properties");
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }
    p.put("org.opencastproject.storage.dir",
            new File("./target", "capture-agent-test").getAbsolutePath());
    p.put("org.opencastproject.server.url", "http://localhost:8080");
    p.put(CaptureParameters.CAPTURE_SCHEDULE_REMOTE_POLLING_INTERVAL, -1);
    p.put("M2_REPO", getClass().getClassLoader().getResource("m2_repo").getFile());
    try {
      config.updated(p);
    } catch (ConfigurationException e) {
      e.printStackTrace();
      Assert.fail();
    }
    gstreamerPipeline.create(config.getAllProperties(), false);
  }

  private XProperties loadProperties(String location) throws IOException {
    XProperties props = new XProperties();
    InputStream s = getClass().getClassLoader().getResourceAsStream(location);
    if (s == null) {
      throw new RuntimeException("Unable to load configuration file from " + location);
    }
    props.load(s);
    return props;
  }

  @Test
  public void pipelineFactoryWillCaptureWithExtraCapabilities() {
    if (!gstreamerInstalled)
      return;
    // if we have something to test
    if (!devices.isEmpty()) {
      String deviceNames = setupCaptureDevices();
      String extraDevice = "Extra Device";
      properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + extraDevice
              + CaptureParameters.CAPTURE_DEVICE_SOURCE, "/bad/path");
      properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + extraDevice
              + CaptureParameters.CAPTURE_DEVICE_DEST, extraDevice + ".out");
      deviceNames += "Not A Device" + ",";
      properties.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, deviceNames);
      testPipeline = gstreamerPipeline.create(properties, false);
      Assert.assertEquals(devices.size(), testPipeline.getElements().size());
    }
  }

  private String setupCaptureDevices() {
    // setup capture properties
    String deviceNames = "";
    for (int i = 0; i < devices.size(); i++) {
      properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + i + CaptureParameters.CAPTURE_DEVICE_SOURCE,
              devices.get(i));
      properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + i + CaptureParameters.CAPTURE_DEVICE_DEST, i
              + ".out");
      deviceNames += i + ",";
    }
    return deviceNames;
  }

  private boolean addProducerTypeDeviceToPropertiesWithSourceLocation(String elementType, ProducerType producerType) {
    boolean canCreateElement = addProducerTypeDeviceToPropertiesWithoutSourceLocation(elementType, producerType);
    if (canCreateElement) {
      // Set the source location property.
      properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + producerType.toString()
              + CaptureParameters.CAPTURE_DEVICE_SOURCE, fakeCaptureDevice.getAbsolutePath());
      return true;
    } else {
      return false;
    }
  }

  private boolean addProducerTypeDeviceToPropertiesWithoutSourceLocation(String elementType, ProducerType producerType) {
    if (PipelineTestHelpers.testGstreamerElement(elementType)) {
      // Set the output location property.
      properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + producerType.toString()
              + CaptureParameters.CAPTURE_DEVICE_DEST, producerType.toString() + ".out");
      // Set the type property.
      properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + producerType.toString()
              + CaptureParameters.CAPTURE_DEVICE_TYPE, producerType.toString());
      if (producerType == ProducerType.CUSTOM_VIDEO_SRC || producerType == ProducerType.CUSTOM_AUDIO_SRC) {
        // Set the customProducer property
        properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + producerType.toString()
                + CaptureParameters.CAPTURE_DEVICE_CUSTOM_PRODUCER, GStreamerElements.FAKESRC);
      }
      deviceNames += producerType.toString() + ",";
      return true;
    } else {
      // Since we can't build an the element, remove it as one of the devices.
      numberOfProducers--;
      return false;
    }
  }

  @Test
  public void pipelineFactoryWillIgnoreSrcPropertiesOnAllRelevantProducerTypes() {
    if (!gstreamerInstalled || !PipelineTestHelpers.isLinux()
            || PipelineTestHelpers.testGstreamerElement(AudioFilesinkConsumer.DEFAULT_ENCODER)
            || PipelineTestHelpers.testGstreamerElement(AudioFilesinkConsumer.DEFAULT_MUXER)
            || PipelineTestHelpers.testGstreamerElement(VideoFilesinkConsumer.DEFAULT_ENCODER)
            || PipelineTestHelpers.testGstreamerElement(VideoFilesinkConsumer.DEFAULT_MUXER))
      return;
    numberOfProducers = 14;
    
    // Devices that don't need a source
    addProducerTypeDeviceToPropertiesWithoutSourceLocation(GStreamerElements.FAKESRC, ProducerType.CUSTOM_VIDEO_SRC);
    addProducerTypeDeviceToPropertiesWithoutSourceLocation(GStreamerElements.FAKESRC, ProducerType.CUSTOM_AUDIO_SRC);
    addProducerTypeDeviceToPropertiesWithoutSourceLocation(GStreamerElements.VIDEOTESTSRC, ProducerType.VIDEOTESTSRC);
    addProducerTypeDeviceToPropertiesWithoutSourceLocation(GStreamerElements.AUDIOTESTSRC, ProducerType.AUDIOTESTSRC);
    addProducerTypeDeviceToPropertiesWithoutSourceLocation(GStreamerElements.ALSASRC, ProducerType.ALSASRC);
    addProducerTypeDeviceToPropertiesWithoutSourceLocation(GStreamerElements.PULSESRC, ProducerType.PULSESRC);

    // Devices that need a source
    addProducerTypeDeviceToPropertiesWithSourceLocation(GStreamerElements.V4LSRC, ProducerType.V4LSRC);
    addProducerTypeDeviceToPropertiesWithSourceLocation(GStreamerElements.V4L2SRC, ProducerType.V4L2SRC);
    addProducerTypeDeviceToPropertiesWithSourceLocation(GStreamerElements.FILESRC, ProducerType.FILE_DEVICE);
    addProducerTypeDeviceToPropertiesWithSourceLocation(GStreamerElements.V4LSRC, ProducerType.EPIPHAN_VGA2USB);
    addProducerTypeDeviceToPropertiesWithSourceLocation(GStreamerElements.FILESRC, ProducerType.HAUPPAUGE_WINTV);
    addProducerTypeDeviceToPropertiesWithSourceLocation(GStreamerElements.V4L2SRC, ProducerType.BLUECHERRY_PROVIDEO);
    addProducerTypeDeviceToPropertiesWithSourceLocation(GStreamerElements.FILESRC, ProducerType.FILE);
    addProducerTypeDeviceToPropertiesWithSourceLocation(GStreamerElements.DV1394SRC, ProducerType.DV_1394);
    properties.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, deviceNames);
    Pipeline pipeline = gstreamerPipeline.create(properties, false);
    Assert.assertEquals(numberOfProducers, pipeline.getElements().size());
  }

  @Test
  public void pipelineFactoryWillCrashWhenMissingSrcLocations() {
    if (!gstreamerInstalled || !PipelineTestHelpers.isLinux()
            || PipelineTestHelpers.testGstreamerElement(AudioFilesinkConsumer.DEFAULT_ENCODER)
            || PipelineTestHelpers.testGstreamerElement(AudioFilesinkConsumer.DEFAULT_MUXER)
            || PipelineTestHelpers.testGstreamerElement(VideoFilesinkConsumer.DEFAULT_ENCODER)
            || PipelineTestHelpers.testGstreamerElement(VideoFilesinkConsumer.DEFAULT_MUXER))
      return;
    numberOfProducers = 6;
    logger.info("A lot of \"Can't find source file or device\" exceptions should follow, this is normal. ");
    // Devices that don't need a source
    addProducerTypeDeviceToPropertiesWithSourceLocation(GStreamerElements.FAKESRC, ProducerType.CUSTOM_VIDEO_SRC);
    addProducerTypeDeviceToPropertiesWithSourceLocation(GStreamerElements.FAKESRC, ProducerType.CUSTOM_AUDIO_SRC);
    addProducerTypeDeviceToPropertiesWithSourceLocation(GStreamerElements.VIDEOTESTSRC, ProducerType.VIDEOTESTSRC);
    addProducerTypeDeviceToPropertiesWithSourceLocation(GStreamerElements.AUDIOTESTSRC,ProducerType.AUDIOTESTSRC);
    addProducerTypeDeviceToPropertiesWithSourceLocation(GStreamerElements.ALSASRC, ProducerType.ALSASRC);
    addProducerTypeDeviceToPropertiesWithSourceLocation(GStreamerElements.PULSESRC, ProducerType.PULSESRC);

    // Devices that need a source
    addProducerTypeDeviceToPropertiesWithoutSourceLocation(GStreamerElements.V4LSRC, ProducerType.V4LSRC);
    addProducerTypeDeviceToPropertiesWithoutSourceLocation(GStreamerElements.V4L2SRC, ProducerType.V4L2SRC);
    addProducerTypeDeviceToPropertiesWithoutSourceLocation(GStreamerElements.FILESRC, ProducerType.FILE_DEVICE);
    addProducerTypeDeviceToPropertiesWithoutSourceLocation(GStreamerElements.V4LSRC, ProducerType.EPIPHAN_VGA2USB);
    addProducerTypeDeviceToPropertiesWithoutSourceLocation(GStreamerElements.FILESRC, ProducerType.HAUPPAUGE_WINTV);
    addProducerTypeDeviceToPropertiesWithoutSourceLocation(GStreamerElements.V4L2SRC, ProducerType.BLUECHERRY_PROVIDEO);
    addProducerTypeDeviceToPropertiesWithoutSourceLocation(GStreamerElements.FILESRC, ProducerType.FILE);
    addProducerTypeDeviceToPropertiesWithoutSourceLocation(GStreamerElements.DV1394SRC, ProducerType.DV_1394);
    properties.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, deviceNames);

    Pipeline pipeline = gstreamerPipeline.create(properties, false);
    for (Element element : pipeline.getElements()) {
      Bin bin = (Bin)element;
      System.out.println(bin.getElements());
    }
    Assert.assertTrue("There are " + numberOfProducers + " elements that should be created with or without a source location. ", pipeline
            .getElements().size() == numberOfProducers);
    logger.info("This should be the end of the \"Can't find source file or device\" exceptions.");
  }

  /*
   * @Test public void testWithInvalidFileSource() { Properties properties = new Properties();
   * properties.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "INVALID");
   * properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "INVALID" +
   * CaptureParameters.CAPTURE_DEVICE_SOURCE, "capture/invalid.mpg");
   * properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "INVALID" + CaptureParameters.CAPTURE_DEVICE_DEST,
   * "invalid_out.mpg");
   * 
   * Pipeline pipeline = PipelineFactory.create(properties); Assert.assertNull(pipeline); }
   * 
   * @Test public void testWithFileSource() { URL testScreen =
   * getClass().getClassLoader().getResource("capture/screen.mpg"); URL testPresenter =
   * getClass().getClassLoader().getResource("capture/camera.mpg"); URL testAudio =
   * getClass().getClassLoader().getResource("capture/audio.mp3"); Properties properties = new Properties();
   * properties.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "SCREEN,PRESENTER,AUDIO");
   * properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "PRESENTER" +
   * CaptureParameters.CAPTURE_DEVICE_SOURCE, testPresenter.getPath());
   * properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "PRESENTER" +
   * CaptureParameters.CAPTURE_DEVICE_DEST, "camera_out.mpg");
   * properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "SCREEN" +
   * CaptureParameters.CAPTURE_DEVICE_SOURCE, testScreen.getPath());
   * properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "SCREEN" + CaptureParameters.CAPTURE_DEVICE_DEST,
   * "screen_out.mpg"); properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "AUDIO" +
   * CaptureParameters.CAPTURE_DEVICE_SOURCE, testAudio.getPath());
   * properties.setProperty(CaptureParameters.CAPTURE_DEVICE_PREFIX + "AUDIO" + CaptureParameters.CAPTURE_DEVICE_DEST,
   * "audio_out.mp3");
   * 
   * Pipeline pipeline = PipelineFactory.create(properties); Assert.assertNotNull(pipeline); }
   * 
   * @Test public void testAddPipeline() { DeviceName[] deviceList = { DeviceName.EPIPHAN_VGA2USB, DeviceName.ALSASRC,
   * DeviceName.BLUECHERRY_PROVIDEO, DeviceName.FILE, DeviceName.HAUPPAUGE_WINTV }; String source = "source"; String
   * dest = "destination"; boolean ret; for (DeviceName device : deviceList) { CaptureDevice captureDevice = new
   * CaptureDevice(source, device, dest); Pipeline pipeline = new Pipeline(); ret =
   * PipelineFactory.addPipeline(captureDevice, pipeline); Assert.assertTrue(ret); } }
   */
}