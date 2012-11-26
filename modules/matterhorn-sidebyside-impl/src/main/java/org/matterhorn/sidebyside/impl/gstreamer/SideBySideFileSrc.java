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
package org.matterhorn.sidebyside.impl.gstreamer;

import org.gstreamer.Bin;
import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.GhostPad;
import org.gstreamer.Pad;
import org.gstreamer.PadLinkReturn;
import org.gstreamer.State;
import org.gstreamer.elements.DecodeBin2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Provides a simple file source for the SideBySideComposer
 */
public class SideBySideFileSrc extends Bin {                                                       

  private static final Logger logger = LoggerFactory.getLogger(SideBySideFileSrc.class);

  private static final String OUTPUT_VIDEO_PAD_NAME = "video-src";
  private static final String OUTPUT_AUDIO_PAD_NAME = "audio-src";
  private static final String CAPS_FORMAT_STRING = "video/x-raw-yuv, width=%d, height=%d, framerate=%d/1";


  /**
   * Initializes this source video Bin
   * @param srcFile the location of the source file 
   * @param width width of the output video frame
   * @param height height of the output video frame
   * @param framerate number of frames per second in the output
   */
  public SideBySideFileSrc(File srcFile, final int width, final int height, final int framerate) {

    // Initializes this bin
    super();

    // Creates necessary elements
    Element fileSrc = ElementFactory.make("filesrc", null);
    DecodeBin2 videoDecoder = new DecodeBin2((String)null);
    final VideoAdapter videoBin = new VideoAdapter(width, height, framerate);

    //videoDecoder.set("expose-all-streams", true);
    
    // Set video location
    fileSrc.set("location", srcFile.getAbsolutePath()); 
    
    // Add elements to this bin
    this.addMany(fileSrc, videoDecoder, videoBin);
    
    // Link elements 
    fileSrc.link(videoDecoder);
                  
    // Add handler for the decoder
    videoDecoder.connect(new DecodeBin2.NEW_DECODED_PAD() {
      
      @Override
      public void newDecodedPad(DecodeBin2 decoder, Pad pad, boolean foo) {
        
        logger.info("Decoded pad caps: {}", pad.getCaps());
        
        if (videoBin.getSinkPads().get(0).isLinked())
          return;
                
        // The parent is the SideBySideFileSrc object itself
        //Bin parent = (Bin)decoder.getParent();

        logger.info("Fake link: {}", pad.link(videoBin.getSinkPads().get(0)));
        logger.info("VideoBin sink caps: {}", videoBin.getSinkPads().get(0).getCaps().toString());
        //if (pad.getCaps().getStructure(0).getName().startsWith("video/")) {
          // This is a video pad
          /*
          parent.add(videoBin);
          PadLinkReturn result = pad.link(videoBin.getSinkPads().get(0));
          logger.debug("Linking {}.{} and {}.{}. Result: {}", new String[] {
                  decoder.getName(), pad.getName(),
                  videoBin.getName(), videoBin.getSinkPads().get(0).getName(),
                  result.toString()});

          // Create an output Pad for this bin, attached to videoBin's output pad
          Pad gp = new GhostPad(OUTPUT_VIDEO_PAD_NAME, videoBin.getSrcPads().get(0));
          videoBin.getSrcPads().get(0).setActive(true);
          gp.setActive(true);
          parent.addPad(gp);
          
          // Get the state which the parent element is transitioning to
          //State[] states = new State[2];
          //parent.getState(0, states);
          
          // states[0] represents the current state while states[1] the state which the element is transitioning to
          //if (states[1] != State.VOID_PENDING)
            logger.info("SetState: {}", videoBin.setState(State.PLAYING));
          //else
            //logger.info("SetState: {}", videoBin.setState(states[0]));
             */
        //}
        
        /*if (pad.getCaps().getStructure(0).getName().startsWith("audio/")) {
          // This is an audio pad --it does not need further adaptation in here
          Pad gp = new GhostPad(OUTPUT_AUDIO_PAD_NAME, pad);
          gp.setActive(true);
          parent.addPad(gp);
        }*/
      }
    });
  }
}