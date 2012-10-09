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
package org.opencastproject.videoeditor.gstreamer;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import junit.framework.Assert;
import org.junit.Test;
import org.opencastproject.videoeditor.gstreamer.sources.SourceBinsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author wsmirnow
 */
public class VideoEditorPipelineTest extends GstreamerAbstractTest {
  
  /** The logging instance */
  private static final Logger logger = LoggerFactory.getLogger(VideoEditorPipelineTest.class);
      
  /**
   * Test of run and stop methods, of class VideoEditorPipeline.
   */
  @Test
  public void testRunStopDemuxedSourceFiles() {
    logger.info("Test pipeline with demuxed source files...");
    try {
      SourceBinsFactory sourceBins = new SourceBinsFactory(new File(outputFilePath).getAbsolutePath());
      sourceBins.addFileSource(new File(audioFilePath).getAbsolutePath(), 
              TimeUnit.SECONDS.toMillis(0), TimeUnit.SECONDS.toMillis(10));
      sourceBins.addFileSource(new File(audioFilePath).getAbsolutePath(), 
              TimeUnit.SECONDS.toMillis(60), TimeUnit.SECONDS.toMillis(10));
      
      sourceBins.addFileSource(new File(videoFilePath).getAbsolutePath(), 
              TimeUnit.SECONDS.toMillis(0), TimeUnit.SECONDS.toMillis(10));
      sourceBins.addFileSource(new File(videoFilePath).getAbsolutePath(), 
              TimeUnit.SECONDS.toMillis(60), TimeUnit.SECONDS.toMillis(10));
      
      VideoEditorPipeline pipeline = new VideoEditorPipeline(new Properties());
      pipeline.addSourceBinsAndCreatePipeline(sourceBins);
      
      pipeline.run();
      pipeline.mainLoop();
      
      String lastError = pipeline.getLastErrorMessage();
      Assert.assertNull("Last error should be null but it is: " + pipeline.getLastErrorMessage(), lastError);
      
    } catch (Exception ex) {
      Assert.fail();
    }
  }
  
  /**
   * Test of run and stop methods, of class VideoEditorPipeline.
   */
  @Test
  public void testRunStopMuxedSourceFile() {
    logger.info("Test pipeline with muxed source file...");
        
    try {
      SourceBinsFactory sourceBins = new SourceBinsFactory(new File(outputFilePath).getAbsolutePath());
      sourceBins.addFileSource(new File(muxedFilePath).getAbsolutePath(), 
              TimeUnit.SECONDS.toMillis(0), TimeUnit.SECONDS.toMillis(10));
      sourceBins.addFileSource(new File(muxedFilePath).getAbsolutePath(), 
              TimeUnit.SECONDS.toMillis(20), TimeUnit.SECONDS.toMillis(10));
            
      VideoEditorPipeline pipeline = new VideoEditorPipeline(new Properties());
      pipeline.addSourceBinsAndCreatePipeline(sourceBins);
      
      pipeline.run();
      pipeline.mainLoop();
      
      String lastError = pipeline.getLastErrorMessage();
      Assert.assertNull("Last error should be null but it is: " + pipeline.getLastErrorMessage(), lastError);
      
    } catch (Exception ex) {
      Assert.fail();
    }
  }
}
