package org.opencastproject.capture.pipeline.bins.sinks;

import static org.easymock.EasyMock.createMock;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import org.gstreamer.Gst;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.opencastproject.capture.api.CaptureAgent;
import org.opencastproject.capture.pipeline.SinkDeviceName;
import org.opencastproject.capture.pipeline.SourceDeviceName;
import org.opencastproject.capture.pipeline.bins.BinTestHelpers;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceBinTest;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore
public class SinkFactoryTest {

CaptureAgent captureAgentMock;
  
  /** Capture Device Properties created for unit testing **/
  CaptureDevice captureDevice = null;
  
  /** Properties specifically designed for unit testing */
  private static Properties properties = null;
  
  /** True to run the tests */
  private static boolean gstreamerInstalled = true;
  
  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CaptureDeviceBinTest.class);

  @BeforeClass
  public static void testGst() {
    try {
      Gst.init();
    } catch (Throwable t) {
      logger.warn("Skipping agent tests due to unsatisifed gstreamer installation");
      gstreamerInstalled = false;
    }
  }
   
  @Before
  public void setup() throws ConfigurationException, IOException, URISyntaxException {
    if (!gstreamerInstalled)
      return;
    
    captureAgentMock = createMock(CaptureAgent.class);
    
    Properties captureDeviceProperties = BinTestHelpers.createCaptureDeviceProperties(null, null, null, null, null, null, null, null, null);
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.EPIPHAN_VGA2USB, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties); 
    
    properties = BinTestHelpers.createConfidenceMonitoringProperties();
  }

  @After
  public void tearDown() {
    if (!gstreamerInstalled)
      return;
    properties = null;
    captureDevice = null;
  }
  
  @Test
  public void testXVImageSink() {
    if (!gstreamerInstalled)
      return;
    SinkBin sinkBin = getSink(SinkDeviceName.XVIMAGESINK);
    Assert.assertTrue(sinkBin instanceof XVImageSinkBin);
    Assert.assertTrue(sinkBin.getSrc() != null);
  }
  
  @Test
  public void testVideoFileSink() {
    if (!gstreamerInstalled)
      return;
    SinkBin sinkBin = getSink( SinkDeviceName.VIDEO_FILE_SINK);
    Assert.assertTrue(sinkBin instanceof VideoFileSinkBin);
    Assert.assertTrue(sinkBin.getSrc() != null);
  }
  
  @Test
  public void testAudioFileSink() {
    if (!gstreamerInstalled)
      return;
    SinkBin sinkBin = getSink( SinkDeviceName.AUDIO_FILE_SINK);
    Assert.assertTrue(sinkBin instanceof AudioFileSinkBin);
    Assert.assertTrue(sinkBin.getSrc() != null);
  }
  
  private SinkBin getSink(SinkDeviceName sinkDeviceName) {
    SinkBin sinkBin = null;
    try {
      sinkBin = SinkFactory.getInstance().getSink(sinkDeviceName, captureDevice, properties);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
    return sinkBin;
  }
}
