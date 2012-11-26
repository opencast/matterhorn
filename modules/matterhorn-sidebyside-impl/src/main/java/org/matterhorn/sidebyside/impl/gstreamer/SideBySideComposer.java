package org.matterhorn.sidebyside.impl.gstreamer;

import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Pad;
import org.gstreamer.Pipeline;

import java.awt.Rectangle;
import java.io.File;

public class SideBySideComposer extends Pipeline {

  /*"""                                                                                                                                                                                                   
  videomixer2 name=mix background=1                                                                                                                                                                                
  \                                                                                                                                                                                                                
  sink_0::xpos=0 sink_0::ypos=120 sink_0::zorder=0                                                                                                                                                                 
  sink_1::xpos=640 sink_1::ypos=120 sink_1::zorder=0                                                                                                                                                          
  \                                                                                                                                                                                                                
  ffmpegcolorspace name=colorsp_saida !                                                                                                                                                                            
  video/x-raw-yuv, format=(fourcc)I420, width=1280, height=720, framerate=25/1 !                                                                                                                                   
  identity name=mixersink                                                                                                                                                                                          
  \                                                                                                                                                                                                                
  filesrc name=screensrc ! decodebin2 name=dbscreen !                                                                                                                                                              
  aspectratiocrop aspect-ratio=4/3 ! videoscale ! videorate !                                                                                                                                                      
  ffmpegcolorspace name=colorsp_screen !                                                                                                                                                                           
  video/x-raw-yuv, format=(fourcc)AYUV, framerate=25/1, width=640, height=480 !                                                                                                                                    
  mix.sink_0                                                                                                                                                                                                       
  \                                                                                                                                                                                                                
  filesrc name=camerasrc ! decodebin2 name=dbcam !                                                                                                                                                                 
  aspectratiocrop aspect-ratio=4/3 ! videoscale ! videorate !                                                                                                                                                      
  ffmpegcolorspace name=colorsp_camera !                                                                                                                                                                           
  video/x-raw-yuv, format=(fourcc)AYUV, framerate=25/1, width=640, height=480 !                                                                                                                                    
  mix.sink_1                                                                                                                                                                                                                                                                                                                                              
"""
*/ 

  private static final String OUTPUT_CAPS_PATTERN = "video/x-raw-yuv, width=%d, height=%d, framerate=%d/1"; 
  private static final String DEFAULT_VIDEO_ENCODER = "x264enc";
  private static final String DEFAULT_MUXER = "mp4mux";
  private static final int DEFAULT_FRAMERATE = 30; //fps
  
  public SideBySideComposer(File presenterFile, Rectangle presenterRect,
          File presentationFile, Rectangle presentationRect,
          Rectangle layoutRect, File destinationFile) {
    
    super();
    
    // Create the video sources
    SideBySideFileSrc presenterBin = new SideBySideFileSrc(
            presenterFile, presenterRect.width, presenterRect.height, DEFAULT_FRAMERATE);
    SideBySideFileSrc presentationBin = new SideBySideFileSrc(
            presentationFile, presentationRect.width, presentationRect.height, DEFAULT_FRAMERATE);
    
    // Create other regular gstreamer elements
    Element videoMixer = ElementFactory.make("videomixer2", "output-mixer");
    Element outputCaps = ElementFactory.make("capsfilter", "output-caps");
    Element colorSpace = ElementFactory.make("ffmpegcolorspace", "output-cspace");
    Element videoRate = ElementFactory.make("videorate", "output-videorate");
    Element videoScale = ElementFactory.make("videoscale", "output-videoscale");
    Element queue = ElementFactory.make("queue", "output-queue");
    Element videoEncoder = ElementFactory.make(DEFAULT_VIDEO_ENCODER, "output-encoder");
    Element mixer = ElementFactory.make(DEFAULT_MUXER, "output-muxer");
    Element fileSink = ElementFactory.make("filesink", "output-filesink");
   
    // Add the elements to the pipeline and link them
    this.addMany(presenterBin, presentationBin,
            videoMixer, outputCaps, colorSpace, videoRate, videoScale,
            queue, videoEncoder, mixer, fileSink);
    
    this.link(videoMixer, outputCaps, colorSpace, videoRate, videoScale, queue, videoEncoder, mixer, fileSink);
    presenterBin.link(videoMixer);
    presentationBin.link(videoMixer);

    // Get the mixer pads corresponding to each input source
    Pad presenterMixerPad = presenterBin.getStaticPad("src").getPeer();
    Pad presentationMixerPad = presentationBin.getStaticPad("src").getPeer();
    
    // Set up some parameters in the elements (and the mixer pads)
    presenterMixerPad.set("xpos", presenterRect.x);
    presenterMixerPad.set("ypos", presenterRect.y);
    presentationMixerPad.set("xpos", presentationRect.x);
    presentationMixerPad.set("ypos", presentationRect.y);
    outputCaps.set("caps", new Caps(String.format(OUTPUT_CAPS_PATTERN, 
            layoutRect.width, layoutRect.height, DEFAULT_FRAMERATE)));
    fileSink.set("location", destinationFile.getAbsolutePath());
  }
}
