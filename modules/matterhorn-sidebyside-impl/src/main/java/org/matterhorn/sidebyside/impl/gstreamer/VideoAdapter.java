package org.matterhorn.sidebyside.impl.gstreamer;

import org.gstreamer.Bin;
import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.GhostPad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VideoAdapter extends Bin {

  private static final Logger logger = LoggerFactory.getLogger(VideoAdapter.class);
  
  private static final String SRC_PAD_NAME = "src";
  private static final String SINK_PAD_NAME = "sink";
  private static final String CAPS_FORMAT_STRING = "video/x-raw-yuv, width=%d, height=%d, framerate=%d/1";
  
  protected VideoAdapter(int width, int height, int framerate) {
    super();
    
    //Element ratioCrop = ElementFactory.make("aspectratiocrop", null);
    Element videoScale = ElementFactory.make("videoscale", null);
    Element videoRate = ElementFactory.make("videorate", null);
    Element colorSpace = ElementFactory.make("ffmpegcolorspace", null);
    Element outputCaps = ElementFactory.make("capsfilter", null);
    Element fake = ElementFactory.make("fakesink", null);
    
    // Set ratio crop
    //ratioCrop.set("aspect-ratio", 4/3);

    // Set output caps
    //outputCaps.set("caps", new Caps(String.format(CAPS_FORMAT_STRING, width, height, framerate)));
    outputCaps.set("caps", Caps.anyCaps());
    
    logger.info("VideoAdapter SrcPads: {}", colorSpace.getSrcPads().get(0).getCaps());

    // Add and link elements
    this.addMany(colorSpace, videoScale, videoRate, outputCaps, fake);
    this.link(colorSpace, videoScale, videoRate, outputCaps, fake); 
    
    // Create ghost pads
    logger.info("VideoBinAddPad: {}", this.addPad(new GhostPad(SINK_PAD_NAME, colorSpace.getSinkPads().get(0))));
    //GhostPad srcPad = new GhostPad(SRC_PAD_NAME, outputCaps.getStaticPad(SRC_PAD_NAME));
    //this.addPad(srcPad);
  }

}
