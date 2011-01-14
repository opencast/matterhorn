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
package org.opencastproject.job.api;

import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is a utility implementation that will wait for one or more jobs to change their status to either one of:
 * <ul>
 * <li>{@link Job.Status#FINISHED}</li>
 * <li>{@link Job.Status#FAILED}</li>
 * <li>{@link Job.Status#DELETED}</li>
 * </ul>
 */
public class JobBarrier {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(JobBarrier.class);

  /** Default polling interval is 5 seconds */
  protected static final long DEFAULT_POLLING_INTERVAL = 5000L;

  /** The service registry used to do the polling */
  protected ServiceRegistry serviceRegistry = null;

  /** Time in milliseconds between two pools for the job status */
  protected long pollingInterval = DEFAULT_POLLING_INTERVAL;

  /** An exception that might have been thrown while polling */
  protected Throwable pollingException = null;

  /** The jobs to wait on */
  protected Job[] jobs = null;

  /** The status map */
  protected Result status = null;

  /**
   * Creates a barrier for <code>jobs</code>, using <code>registry</code> to poll for the outcome of the monitored jobs
   * using the default polling interval {@link #DEFAULT_POLLING_INTERVAL}.
   * 
   * @param registry
   *          the registry
   * @param jobs
   *          the jobs to monitor
   */
  public JobBarrier(ServiceRegistry registry, Job... jobs) {
    this(registry, DEFAULT_POLLING_INTERVAL, jobs);
  }

  /**
   * Creates a wrapper for <code>job</code>, using <code>registry</code> to poll for the job outcome.
   * 
   * @param job
   *          the job to poll
   * @param registry
   *          the registry
   * @param pollingInterval
   *          the time in miliseconds between two polling operations
   */
  public JobBarrier(ServiceRegistry registry, long pollingInterval, Job... jobs) {
    if (registry == null)
      throw new IllegalArgumentException("Service registry must not be null");
    if (jobs == null || jobs.length == 0)
      throw new IllegalArgumentException("Jobs must not be null");
    if (pollingInterval < 0)
      throw new IllegalArgumentException("Polling interval must be a positive number");
    this.serviceRegistry = registry;
    this.pollingInterval = pollingInterval;
    this.jobs = jobs;
  }

  /**
   * Waits for a status change and returns the new status.
   * 
   * @return the status
   */
  public Result waitForJobs() {
    return waitForJobs(0);
  }

  /**
   * Waits for a status change on all jobs and returns. If waiting for the status exceeds a certain limit, the method
   * returns even if some or all of the jobs are not yet finished. The same is true if at least one of the jobs fails or
   * gets stopped or deleted.
   */
  public Result waitForJobs(long timeout) {
    synchronized (this) {
      JobStatusUpdater updater = new JobStatusUpdater(timeout);
      try {
        updater.start();
        wait();
      } catch (InterruptedException e) {
        logger.debug("Interrupted while waiting for job");
      } finally {
        updater.interrupt();
      }
    }
    if (pollingException != null)
      throw new IllegalStateException(pollingException);
    return getStatus();
  }

  /**
   * Sets the outcome of the variuos jobs that were monitored.
   * 
   * @param status
   *          the status
   */
  void setStatus(Result status) {
    this.status = status;
  }

  /**
   * Returns the resulting status map.
   * 
   * @return the status of the individual jobs
   */
  public Result getStatus() {
    return status;
  }

  /**
   * Thread that keeps polling for status changes.
   */
  class JobStatusUpdater extends Thread {

    /** Maximum wait in milliseconds or 0 for unlimited waiting */
    private long workTime = 0;

    /**
     * Creates a new status updater that will wait for finished jobs. If <code>0</code> is passed in as the work time,
     * the updater will wait as long as it takes. Otherwise, it will stop after the indicated amount of time has passed.
     * 
     * @param workTime
     *          the work time
     */
    JobStatusUpdater(long workTime) {
      this.workTime = workTime;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {

      long endTime = 0;
      if (workTime > 0) {
        endTime = System.currentTimeMillis() + workTime;
      }

      while (true) {
        boolean allDone = true;
        boolean failedOrDeleted = false;
        Map<Job, Job.Status> status = new HashMap<Job, Job.Status>();

        // Look at all jobs and make sure all of them have reached the expected status
        for (Job job : jobs) {

          // Don't aks what we already know
          if (status.containsKey(job))
            continue;
          
          boolean jobFinished = false;
          
          // Get the job status from the service registry
          try {
            Job processedJob = serviceRegistry.getJob(job.getId());
            Job.Status jobStatus = processedJob.getStatus();
            switch (jobStatus) {
              case DELETED:
              case FAILED:
                failedOrDeleted = true;
                jobFinished = true;
                break;
              case FINISHED:
                jobFinished = true;
                break;
              case PAUSED:
              case QUEUED:
              case RUNNING:
                logger.trace("Job {} is still in the works", JobBarrier.this);
                allDone = false;
                break;
              default:
                logger.error("Unhandled job status '{}' found", jobStatus);
                break;
            }

            // Are we done with this job?
            if (jobFinished) {
              job.setStatus(jobStatus);
              job.setPayload(processedJob.getPayload());
              status.put(job, jobStatus);
            }

          } catch (NotFoundException e) {
            pollingException = e;
            break;
          } catch (ServiceRegistryException e) {
            logger.warn("Error polling service registry {} for job {}: {}", new Object[] { serviceRegistry,
                    JobBarrier.this, e.getMessage() });
          } catch (Throwable t) {
            logger.error("An unexpected error occured while waiting for jobs", t);
            pollingException = t;
            updateAndNotify(status);
            return;
          }

        }

        long time = System.currentTimeMillis();

        // Are we done already?
        if (allDone || failedOrDeleted || endTime >= time) {
          updateAndNotify(status);
          return;
        }

        // Wait a little..
        try {
          long timeToSleep = Math.min(pollingInterval, Math.abs(endTime - time));
          Thread.sleep(timeToSleep);
        } catch (InterruptedException e) {
          logger.debug("Job polling thread was interrupted");
          return;
        }

      }
    }

    /**
     * Notifies listeners about the status change.
     * 
     * @param status
     *          the status
     */
    private void updateAndNotify(Map<Job, Job.Status> status) {
      JobBarrier.this.setStatus(new Result(status));
      synchronized (JobBarrier.this) {
        JobBarrier.this.notifyAll();
      }
    }

  }

  /**
   * Result of a waiting operation on a certain number of jobs.
   */
  public class Result {

    /** The outcome of this barrier */
    private Map<Job, Job.Status> status = null;

    /**
     * Creates a new job barrier result.
     * 
     * @param status
     *          the barrier outcome
     */
    public Result(Map<Job, Job.Status> status) {
      this.status = status;
    }

    /**
     * Returns the status details.
     * 
     * @return the status details
     */
    public Map<Job, Job.Status> getStatus() {
      return status;
    }

    /**
     * Returns <code>true</code> if all jobs are in the <code>{@link Job.Status#FINISHED}</code> state.
     * 
     * @return <code>true</code> if all jobs are finished
     */
    public boolean isSuccess() {
      for (Job.Status state : status.values()) {
        if (!state.equals(Job.Status.FINISHED))
          return false;
      }
      return true;
    }
    
  }

}
