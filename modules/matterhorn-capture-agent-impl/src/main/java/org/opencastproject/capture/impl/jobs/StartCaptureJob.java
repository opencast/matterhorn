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
package org.opencastproject.capture.impl.jobs;

import org.opencastproject.capture.api.CaptureParameters;
import org.opencastproject.capture.impl.CaptureAgentImpl;
import org.opencastproject.mediapackage.MediaPackage;

import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Properties;

/**
 * The class responsible for starting a capture.
 */
public class StartCaptureJob implements Job {

  private static final Logger logger = LoggerFactory.getLogger(StartCaptureJob.class);

  /**
   * Starts the capture itself.  Also schedules a StopCaptureJob.
   * {@inheritDoc}
   * @see org.quartz.Job#execute(JobExecutionContext)
   * @throws JobExecutionException
   */
  public void execute(JobExecutionContext ctx) throws JobExecutionException {
    
    logger.info("Initiating StartCaptureJob.");
    CaptureAgentImpl captureAgentImpl = null;
    MediaPackage mediaPackage = null;
    Properties properties = null;
    Scheduler sched = null;

    //// Extracts the necessary parameters for calling startCapture()
    // The capture agent
    captureAgentImpl = (CaptureAgentImpl)ctx.getMergedJobDataMap().get(JobParameters.CAPTURE_AGENT);
    // The MediaPackage
    mediaPackage = (MediaPackage)ctx.getMergedJobDataMap().get(JobParameters.MEDIA_PACKAGE);
    // The capture Properties
    properties = (Properties)ctx.getMergedJobDataMap().get(JobParameters.CAPTURE_PROPS);
    // The scheduler to use.  We do not use ctx.getScheduler() because this job's scheduler might be gone (see SchedulerImpl)
    sched = (Scheduler)ctx.getMergedJobDataMap().get(JobParameters.SCHEDULER);

    if (captureAgentImpl == null) {
      logger.error("No capture agent provided! Capture Interrupted");
      return;
    }

    //We require both objects to exist, if either of them doesn't then the scheduler didn't do its job properly
    if (properties == null || mediaPackage == null) {
      logger.error("Insufficient parameters provided. startCapture() needs Properties and a MediaPackage to proceed");
      return;
    }

    String postfix = properties.getProperty(JobParameters.JOB_POSTFIX);
    if (postfix == null) {
      logger.error("Key {} not found in job properties, cannot continue!", JobParameters.JOB_POSTFIX);
      return;
    }
    
    try {

      // Get the stopCaptureJob scheduling ready in case something happens, so we don't need to stop the capture afterwards
      String time2Stop = properties.getProperty(CaptureParameters.RECORDING_END);

      JobDetail job = new JobDetail("StopCapture-" + postfix, JobParameters.CAPTURE_RELATED_TYPE, StopCaptureJob.class);
      CronTrigger trigger = new CronTrigger("StopCaptureTrigger-" + postfix, JobParameters.CAPTURE_RELATED_TYPE, time2Stop);
      trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);

      trigger.getJobDataMap().put(JobParameters.CAPTURE_AGENT, captureAgentImpl);
      trigger.getJobDataMap().put(JobParameters.JOB_POSTFIX, postfix);
      trigger.getJobDataMap().put(JobParameters.SCHEDULER, sched);

      // Actually does the service
      String recordingID = captureAgentImpl.startCapture(mediaPackage, properties);
      if (recordingID == null) {
        logger.error("Capture agent returned null when trying to start capture!");
        return;
      }

      // Stores the recordingID so that it can be passed from one job to the other
      trigger.getJobDataMap().put(CaptureParameters.RECORDING_ID, recordingID);

      // Schedules the stop event
      sched.scheduleJob(job, trigger);
      logger.debug("stopCapture scheduled for: {}", trigger);

      //Remove this job from the system if my scheduler still exists
      JobDetail mine = ctx.getJobDetail();
      try {
        if (!ctx.getScheduler().isShutdown()) {
          ctx.getScheduler().deleteJob(mine.getName(), mine.getGroup());
        }
      } catch (SchedulerException e) {
        logger.warn("Unable to delete start capture job {}!", mine.getName());
        e.printStackTrace();
      }
      
    } catch (SchedulerException e) {
      logger.error("Couldn't schedule task: {}", e.getMessage());
      //e.printStackTrace();
    } catch (ParseException e) {
      logger.error("Invalid time for stopping capture: {}. Aborting.\n{}", properties.get(CaptureParameters.RECORDING_END), e.getMessage());
      //e.printStackTrace();
    } catch (Exception e) {
      logger.error("Unexpected exception: {}\nJob may have not been executed", e.getMessage());
      e.printStackTrace();
    }
  }
}
