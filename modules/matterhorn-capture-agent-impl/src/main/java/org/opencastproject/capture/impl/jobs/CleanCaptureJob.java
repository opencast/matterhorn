/**
 *  Copyright 2009 The Regents of the University of California
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
package org.opencastproject.capture.impl.jobs;

import java.io.File;
import java.util.Properties;

import org.opencastproject.capture.impl.CaptureAgentImpl;
import org.opencastproject.capture.impl.CaptureParameters;
import org.opencastproject.capture.impl.ConfigurationManager;
import org.opencastproject.capture.impl.RecordingImpl;
import org.opencastproject.util.FileSupport;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class which cleans up captures if the capture has been successfully ingested and the 
 * remaining diskspace is below a minimum threshold or above a maximum archive days threshold.
 */
public class CleanCaptureJob implements Job {

  private static final Logger logger = LoggerFactory.getLogger(CleanCaptureJob.class);

  /** File signifying ingestion of media has been completed */
  public static final String CAPTURE_INGESTED = "captured.ingested";

  /** The length of one day represented in milliseconds */
  public static final long DAY_LENGTH_MILLIS = 86400000;

  long minDiskSpace = 0;
  long maxArchivalDays = Long.MAX_VALUE;
  boolean checkArchivalDays = true;
  boolean checkDiskSpace = true;
  boolean underMinSpace = false;
  
  /**
   * Cleans up lectures which no longer need to be stored on the capture agent itself.
   * {@inheritDoc}
   * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
   */
  public void execute(JobExecutionContext ctx) throws JobExecutionException {

    ConfigurationManager cm = (ConfigurationManager) ctx.getMergedJobDataMap().get(JobParameters.CONFIG_SERVICE);
    CaptureAgentImpl service = (CaptureAgentImpl)ctx.getMergedJobDataMap().get(JobParameters.CAPTURE_AGENT);
    Properties p = cm.getAllProperties();

    doCleaning(p, service.getRecordings());
    
  }
  
  /**
   * This method is public to allow an easy testing without having to schedule anything
   * @param p Properties including the keys for maximum archival days and minimum disk space
   * @param service The capture agent
   */
  public void doCleaning(Properties p , RecordingImpl[] recordings) {
    // Parse the necessary values for minimum disk space and maximum archival days. 
    // Note that if some of those is not specified, the corresponding cleaning is not done.
    
    try {
      maxArchivalDays = Long.parseLong(p.getProperty(CaptureParameters.CAPTURE_CLEANER_MAX_ARCHIVAL_DAYS));
      // If the value is < 0 (no matter if it's because of an overflow), it's invalid.
      // MAX_VALUE is considered infinity, and therefore there is no limit for arquiving the recordings
      if ((maxArchivalDays < 0) || (maxArchivalDays == Long.MAX_VALUE))
        checkArchivalDays = false;
    } catch (NumberFormatException e) {
      logger.warn("No maximum archival days value specified in properties");
      checkArchivalDays = false;
    }

    try {
      minDiskSpace = Long.parseLong(p.getProperty(CaptureParameters.CAPTURE_CLEANER_MIN_DISK_SPACE));
      if (minDiskSpace <= 0)
        checkDiskSpace = false;
    } catch (NumberFormatException e) {
      logger.warn("No minimum disk space value specified in properties");
      checkDiskSpace = false;
    }

    // If none of this parameters has been specified, the cleanup cannot be performed
    if (!checkArchivalDays && !checkDiskSpace) {
      logger.info("No capture cleaning was made, according to the parameters");
      return;
    }

    // Gets all the recording IDs for this agent, and iterates over them
    for (RecordingImpl theRec : recordings) {
      File recDir = theRec.getDir();
      File ingested = new File(recDir, CaptureParameters.CAPTURE_INGESTED_FILE);

      // If the capture.ingested file does not exist we cannot delete the data
      if (!ingested.exists()) {
        logger.info("Skipped cleaning for {}. Ingestion has not been completed.", theRec.getID());
        continue;
      }

      // Clean up if we are running out of disk space 
      // TODO: Support Java 1.5 (dir.getFreeSpace() is 1.6 only)
      if (checkDiskSpace) {
        long freeSpace = recDir.getFreeSpace();
        if (freeSpace < minDiskSpace) {
          underMinSpace = true;
          logger.info("Removing capture {} archives in {}. Under minimum free disk space.", theRec.getID(), recDir.getAbsolutePath());
          FileSupport.delete(recDir, true);
          continue;
        }
        else {
          underMinSpace = false;
          logger.debug("Archive: recording {} not removed, enough disk space remains for archive.", theRec.getID());
        }
      }

      // Clean up capture if its age of ingestion is higher than max archival days property
      if (checkArchivalDays) {
        long age = ingested.lastModified();
        long currentTime = System.currentTimeMillis();
        if (currentTime - age > maxArchivalDays * DAY_LENGTH_MILLIS) {
          logger.info("Removing capture {} archives at {}.\nExceeded the maximum archival days.", theRec.getID(), recDir.getAbsolutePath());
          FileSupport.delete(recDir, true);
          continue;
        }
        else {
          logger.debug("Recording {} has NOT yet exceeded the maximum archival days. Keeping {}", theRec.getID(), recDir.getAbsolutePath());
        }
      }
      logger.debug("Recording {} ({}) not deleted.", theRec.getID(), recDir.getAbsolutePath());
    }
    
    if (underMinSpace)
      logger.warn("Free space is under the minimum disk space limit!");
    
  }
}
