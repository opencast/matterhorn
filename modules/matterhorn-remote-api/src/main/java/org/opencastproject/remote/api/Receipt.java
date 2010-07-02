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
package org.opencastproject.remote.api;

import org.opencastproject.mediapackage.MediaPackageElement;

/**
 * A receipt for an long running, asynchronous job.  A Receipt may be used to track any task once it has been queued.
 */
public interface Receipt {
  /** The status of the job that this receipt represents */
  public static enum Status {QUEUED, RUNNING, FINISHED, FAILED}

  /** Gets the receipt identifier */
  public String getId();

  /** Sets the receipt identifier */
  public void setId(String id);

  /** Gets the receipt type, which determines the type of service that runs the job */
  public String getType();
  
  /** Sets the receipt type */
  public void setType(String type);
  
  /** Gets the receipt's current {@link Status} */
  public Status getStatus();

  /** Sets the receipt's current {@link Status} */
  public void setStatus(Status status);

  /** Gets the host responsible for queuing and running this job */
  public String getHost();

  /** Sets the host responsible for queuing and running this job */
  public void setHost(String host);
  
//  /** Gets the execution context for this receipt.  This can be a workflow identifier or any other reference. */
//  public String getContext();
//  
//  /** Sets the execution context for this receipt. */
//  public void setContext(String context);

  /**
   * Gets the mediapackage element that was produced by this job, or null if none was produced, or if it has yet
   * to be produced.
   * 
   * @return the mediapackage element
   */
  public MediaPackageElement getElement();

  /** Sets the mediapackage element produced by this job. */
  public void setElement(MediaPackageElement element);

  /** Gets an xml representation of this receipt */
  public String toXml();
}
