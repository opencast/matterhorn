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
package org.opencastproject.capture.pipeline.bins.sources;

import java.util.Properties;

import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.GhostPad;
import org.gstreamer.Pad;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.PartialBin;

public abstract class SrcBin extends PartialBin{
  public static final String GHOST_PAD_NAME = "src";
  Element queue;
  
  /** SrcBin is the super class for all sources for matterhorn including both audio and video sources. 
   * 
   * @param captureDevice - The details of the capture device in the configuration file we are dealing with.
   * @param confidenceMonitoringProperties - The details in the configuration file about confidence monitoring.
   * @throws Exception 
   */
  public SrcBin(CaptureDevice captureDevice, Properties properties) throws Exception{
   super(captureDevice, properties);
  }

  /** Create all elements necessary by all capture devices, in this case a queue. **/
  @Override
  protected void createElements(){
    queue = ElementFactory.make("queue", null);
  }
  
  /** Create the Ghost Pads necessary to link the source to the tee in the @code{CaptureDeviceBin}. **/
  @Override
  protected void createGhostPads() throws Exception {
    //Pad ghostPadElement = this.getSrcPad().getStaticPad("src");    
    Pad ghostPadElement = this.getSrcPad();
    if(ghostPadElement == null || !bin.addPad(new GhostPad(GHOST_PAD_NAME, ghostPadElement))){
      throw new Exception("Could not create new Ghost Pad with " + this.getSrcPad());
    }
  }
  
  /** Abstract method so that we can get the sink for the source that we will then use to create the ghost pads
   * for this bin. If you are creating a source just return the last Element in your pipeline for the source.
   * @return Returns the sink element whatever it happens to be for the source.
   */
  protected abstract Pad getSrcPad();
}
