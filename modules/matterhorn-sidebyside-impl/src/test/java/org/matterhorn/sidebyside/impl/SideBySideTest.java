package org.matterhorn.sidebyside.impl;

import org.gstreamer.Bin;
import org.gstreamer.Bus;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Gst;
import org.gstreamer.GstObject;
import org.gstreamer.Pad;
import org.gstreamer.Pipeline;
import org.gstreamer.State;
import org.gstreamer.lowlevel.MainLoop;

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.matterhorn.sidebyside.impl.gstreamer.SideBySideFileSrc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URISyntaxException;


/**
 * Unit tests for the SideBySide service
 */
public class SideBySideTest {

  private static final Logger logger = LoggerFactory.getLogger(SideBySideTest.class);
  private static File baseDir, presenterFile, presentationFile, outputFile;
  
  @BeforeClass
  public static void setUp() throws URISyntaxException {
    baseDir = new File(SideBySideTest.class.getResource("/").toURI());
    presenterFile = new  File(baseDir, "CAMERA.mp4");
    presentationFile = new File(baseDir, "SCREEN.mp4");
    outputFile = new File(baseDir, "output.mpg");
    outputFile.delete();
        
    assertTrue(presenterFile.isFile());
    assertTrue(presentationFile.isFile());
    
    //logger.debug("Debug");
    //logger.info("Info");
    //logger.warn("Warn");
    //logger.error("Error");
    
  }
  
  @Test
  public void proba() {
    Gst.init("bla", new String[]{"--gst-debug-level=2"});
    //Gst.init();
    
    String pipeFormatStr = "filesrc location=%s ! " +
    		"decodebin2 ! fakesink"; // + videoscale ! videorate ! ffmpegcolorspace ! " +
    		//"video/x-raw-yuv, width=%d, height=%d, framerate=%d/1 ! " +
    		//"queue ! mpeg2enc ! mpegpsmux !*/ "filesink location=%s";
    
    String pipeFinalStr = String.format(pipeFormatStr,
            presentationFile.getAbsolutePath(),
            /*800, 600, 30,*/ outputFile.getAbsolutePath());
    
    logger.info(pipeFinalStr);            
            
    final Pipeline pipe = Pipeline.launch(pipeFinalStr);
        
    //final Pipeline pipe = new Pipeline();    
    final MainLoop mainLoop = new MainLoop();
    
    pipe.getBus().connect(new Bus.EOS() {

      /**
       * {@inheritDoc}
       * 
       * @see org.gstreamer.Bus.EOS#endOfStream(org.gstreamer.GstObject)
       */
      @Override
      public void endOfStream(GstObject source) {
        logger.debug("EOS from {}: stop pipeline", new String[] { source.getName() });
        mainLoop.quit();
      }
    });
    
    pipe.getBus().connect(new Bus.INFO() {

      /**
       * {@inheritDoc}
       * 
       * @see org.gstreamer.Bus.INFO#infoMessage(org.gstreamer.GstObject, int, java.lang.String)
       */
      @Override
      public void infoMessage(GstObject source, int code, String message) {
        logger.debug("INFO from {}: ", source.getName(), message);
      }
    });
    
    pipe.getBus().connect(new Bus.ERROR() {

      /**
       * {@inheritDoc}
       * 
       * @see org.gstreamer.Bus.ERROR#errorMessage(org.gstreamer.GstObject, int, java.lang.String)
       */
      @Override
      public void errorMessage(GstObject source, int code, String message) {
        logger.error("ERROR from {}: ", source.getName(), message);
        mainLoop.quit();
      }
    });
    
    
    pipe.getBus().connect(new Bus.WARNING() {

      /**
       * {@inheritDoc}
       * 
       * @see org.gstreamer.Bus.WARNING#warningMessage(org.gstreamer.GstObject, int, java.lang.String)
       */
      @Override
      public void warningMessage(GstObject source, int code, String message) {
        logger.warn("WARNING from {}: ", source.getName(), message);
      }
    });
    
    /*pipe.getBus().connect(new Bus.STATE_CHANGED() {

      @Override
      public void stateChanged(GstObject source, State old, State current, State pending) {
        if (source instanceof Pipeline) {
          logger.debug("{} changed state to {}", new String[] {
            source.getName(), current.toString()
          });
          if (current == State.READY || current == State.PLAYING) {
            pipe.debugToDotFile(Pipeline.DEBUG_GRAPH_SHOW_NON_DEFAULT_PARAMS | Pipeline.DEBUG_GRAPH_SHOW_STATES, 
                    "videoeditor-pipeline-" + current.name(), true);
          }
        }
      }
    });*/
    
    SideBySideFileSrc proba = new SideBySideFileSrc(presenterFile, 800, 600, 30);
    Element queue = ElementFactory.make("queue", null);
    Element cspace = ElementFactory.make("ffmpegcolorspace", null);
    Element rate = ElementFactory.make("videorate", null);
    Element scale = ElementFactory.make("videoscale", null);
    Element encoder = ElementFactory.make("mpeg2enc", null);
    Element muxer = ElementFactory.make("mpegpsmux", null);
    Element sink = ElementFactory.make("filesink", null);
    
    pipe.addMany(proba); //cspace, rate, scale, queue, encoder,muxer, sink);
/*    proba.connect(new Element.PAD_ADDED() {
      
      @Override
      public void padAdded(Element element, Pad pad) {
        if (pad.isLinked())
          return;
        
        final Element fake = ElementFactory.make("fakesink", null);
        Bin parent = (Bin)element.getParent();
        
        if (pad.getCaps().getStructure(0).getName().startsWith("video/"))
          fake.setName("video-sink");
        //else if (pad.getCaps().getStructure(0).getName().startsWith("audio/"))
          //  fakesink.setName("audio-sink");          
        
        parent.add(fake);
        logger.info("Linkando fakesink: {}", pad.link(fake.getSinkPads().get(0)).toString());
        
        //State[] states = new State[2];
        //parent.getState(0, states);
        
        //if (states[1] != State.VOID_PENDING)
          logger.info("FakeState: {}", fake.setState(State.PLAYING));
        //else 
          //fake.setState(states[0]);
      }
    });
  */  
    //pipe.link(proba, fake); //queue, cspace, rate, scale, encoder, muxer, sink);
    
    sink.set("location", outputFile.getAbsolutePath());
    
    pipe.play();
    
    mainLoop.run();
    
    pipe.setState(State.NULL);
    
    assertTrue(outputFile.isFile());
    assertTrue(outputFile.length() > 0);
  }
  
  
}