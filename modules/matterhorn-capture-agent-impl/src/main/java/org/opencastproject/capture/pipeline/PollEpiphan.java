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

import net.luniks.linux.jv4linfo.JV4LInfo;
import net.luniks.linux.jv4linfo.JV4LInfoException;
import net.luniks.linux.jv4linfo.V4LInfo;

import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Event;
import org.gstreamer.Pad;
import org.gstreamer.Pipeline;
import org.gstreamer.State;

/**
 * Thread that will continually poll to check if a VGA signal has been
 * reconnected
 *
 */
class PollEpiphan implements Runnable {

  /** Location of the Epiphan device */
  private String location;

  /** The main capture agent's running pipeline */
  private Pipeline pipeline;

  /** Object used to manage race conditions when trying to access pipeline */
  public static Object enabled = new Object();

  /**
   * Thread constructor.
   * @param pipeline
   * @param location
   */
  public PollEpiphan(Pipeline pipeline, String location) {
    this.location = location;
    this.pipeline = pipeline;
  }

  /**
   * When we have lost a VGA signal, this method can be continually executed
   * to test for a new signal.
   * 
   * @param device the absolute path to the device
   * @return true iff there is a VGA signal
   */
  private boolean checkEpiphan(String device) {
    try {
      V4LInfo v4linfo = JV4LInfo.getV4LInfo(device);
      String deviceName = v4linfo.getVideoCapability().getName();
      if (deviceName.equals("Epiphan VGA2USB")) {
        return true;
      }
    } catch (JV4LInfoException e) {
      return false;
    }
    return false;
  }

  
  /**
   * {@inheritDoc}
   * @see java.lang.Runnable#run()
   */
  public void run() {

    while (true) {
      // Check if the EOS has been sent
      synchronized (enabled) {
        if (PipelineFactory.broken) {
          // Check to see if the Epiphan device has been reconnected to the machine
          if (checkEpiphan(location)) {
            // Indicate to the pipeline that the Epiphan card is no longer disconnected
            PipelineFactory.broken = false;

            // Reconnect the device
            PipelineFactory.logger.debug("Attempting to reconnect to v4lsrc.");
            Element src = ElementFactory.make("v4lsrc", "v4lsrc_" + location + "_" + ++PipelineFactory.v4LSrcIndex);
            Pad v4lsrcPad = src.getStaticPad("src");
            v4lsrcPad.addEventProbe(new Pad.EVENT_PROBE() {
              public boolean eventReceived(Pad pad, Event event) {
                //TODO: Why do we have to supress all messages coming out of this source?
                // TODO: Understand wtf this is false instead of true, the underlying library negates the value we
                // return, but why!?!
                return false;  
              }
            });
            
            src.set("device", location);
            Element identity = pipeline.getElementByName(location + "_v4l_identity");
            pipeline.add(src);
            src.setState(State.PAUSED);
            src.link(identity);
            src.setState(State.PLAYING);

            // Tell the input-selector
            Element selector = pipeline.getElementByName(location + "_selector");
            Pad newPad = selector.getStaticPad("sink0");
            selector.set("active-pad", newPad);
          }
        }
        try {
          enabled.wait(1000);
        } catch (InterruptedException e) {
          PipelineFactory.logger.info("Shutting down Epiphan polling thread.");
          return;
        }
      }
    }
  }
}
