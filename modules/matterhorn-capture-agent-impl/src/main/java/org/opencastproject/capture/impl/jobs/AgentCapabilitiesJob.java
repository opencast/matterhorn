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

import org.opencastproject.capture.api.CaptureAgent;
import org.opencastproject.capture.api.CaptureParameters;
import org.opencastproject.capture.impl.ConfigurationManager;
import org.opencastproject.security.api.TrustedHttpClient;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * This class is responsible for pushing the agent's state to the remote state service.
 */
public class AgentCapabilitiesJob implements Job {

  private static final Logger logger = LoggerFactory.getLogger(AgentStateJob.class);

  private ConfigurationManager config = null;
  private CaptureAgent agent = null;
  private TrustedHttpClient client = null;

  /**
   * Pushes the agent's capabilities to the remote state service.
   * {@inheritDoc}
   * @see org.quartz.Job#execute(JobExecutionContext)
   * @throws JobExecutionException
   */
  public void execute(JobExecutionContext ctx) throws JobExecutionException {
    config = (ConfigurationManager) ctx.getMergedJobDataMap().get(JobParameters.CONFIG_SERVICE);
    agent = (CaptureAgent) ctx.getMergedJobDataMap().get(JobParameters.STATE_SERVICE);
    client = (TrustedHttpClient) ctx.getMergedJobDataMap().get(JobParameters.TRUSTED_CLIENT);

    //Figure out where we're sending the data
    String url = config.getItem(CaptureParameters.AGENT_STATE_REMOTE_ENDPOINT_URL);
    if (url == null) {
      logger.warn("URL for {} is invalid, unable to push capabilities to remote server.", CaptureParameters.AGENT_STATE_REMOTE_ENDPOINT_URL);
      return;
    }
    try {
      if (url.charAt(url.length()-1) == '/') {
        url += config.getItem(CaptureParameters.AGENT_NAME) + "/capabilities";
      } else {
        url += "/" + config.getItem(CaptureParameters.AGENT_NAME) + "/capabilities";
      }
    } catch (StringIndexOutOfBoundsException e) {
      logger.warn("Unable to build valid capabilities endpoint for agents.");
      return;
    }

    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    HttpResponse resp = null;
    try {
      agent.getAgentCapabilities().storeToXML(baos, "Capabilities for the agent " + agent.getAgentName());
      HttpPost remoteServer = new HttpPost(url);
      remoteServer.setEntity(new StringEntity(baos.toString()));
      resp = client.execute(remoteServer);
      if (resp.getStatusLine().getStatusCode() != 200) {
        logger.info("Capabilities push to {} failed with code {}.", url, resp.getStatusLine().getStatusCode());
      }
    } catch (IOException e) {
      logger.error ("Unexpected I/O exception: {}", e.getMessage());
    } catch (Exception e) {
      logger.error("Unable to push update to remote server: {}.", e.getMessage());
    } finally {
      if (resp != null) {
        client.close(resp);
      }
    }
  }
}
