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
package org.opencastproject.serviceregistry.remote;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.message.BasicNameValuePair;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.serviceregistry.api.JaxbServiceRegistrationList;
import org.opencastproject.serviceregistry.api.JaxbServiceStatistics;
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistrationParser;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.ServiceStatistics;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.osgi.framework.ServiceException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation of the remote service registry is able to provide the functionality specified by the api over
 * <code>HTTP</code> rather than by directly connecting to the database that is backing the service.
 * <p>
 * This means that it is suited to run inside protected environments as long as there is an implementation of the
 * service running somwhere that provides the matching communication endpoint, which is the case with the default
 * implementation at {@link org.opencastproject.serviceregistry.impl.ServiceRegistryJpaImpl}.
 * <p>
 * Other than with the other <code>-remote</code> implementations, this one needs to be configured to find it's
 * counterpart implementation. It may either point to a load balancer hiding a number of running instances or to one
 * specific instance.
 */
public class ServiceRegistryRemoteImpl implements ServiceRegistry {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(ServiceRegistryRemoteImpl.class);

  /** Configuration key for the service registry */
  public static final String OPT_SERVICE_REGISTRY_URL = "org.opencastproject.serviceregistry.url";

  /** The http client to use when connecting to remote servers */
  protected TrustedHttpClient client = null;

  /** Url of the actual service implementation */
  protected String serviceURL = null;

  /**
   * Callback for the OSGi environment that is called upon service activation.
   * 
   * @param context
   *          the component context
   */
  protected void activate(ComponentContext context) {
    String serviceURLProperty = StringUtils.trimToNull((String) context.getProperties().get(OPT_SERVICE_REGISTRY_URL));
    if (serviceURLProperty == null)
      throw new ServiceException("Remote service registry can't find " + OPT_SERVICE_REGISTRY_URL);
    try {
      serviceURL = new URL(serviceURLProperty).toExternalForm();
    } catch (MalformedURLException e) {
      throw new ServiceException(OPT_SERVICE_REGISTRY_URL + " is malformed: " + serviceURLProperty);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#registerService(java.lang.String, java.lang.String,
   *      java.lang.String)
   */
  @Override
  public ServiceRegistration registerService(String serviceType, String host, String path)
          throws ServiceRegistryException {
    return registerService(serviceType, host, path, false);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#registerService(java.lang.String, java.lang.String,
   *      java.lang.String, boolean)
   */
  @Override
  public ServiceRegistration registerService(String serviceType, String host, String path, boolean jobProducer)
          throws ServiceRegistryException {
    String servicePath = "register";
    HttpPost post = new HttpPost(UrlSupport.concat(serviceURL, servicePath));
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("serviceType", serviceType));
      params.add(new BasicNameValuePair("host", host));
      params.add(new BasicNameValuePair("path", path));
      params.add(new BasicNameValuePair("jobProducer", Boolean.toString(jobProducer)));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
      post.setEntity(entity);
    } catch (UnsupportedEncodingException e) {
      throw new ServiceRegistryException("Can not url encode post parameters", e);
    }
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(post);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_OK) {
        logger.info("Registered '" + serviceType + "' on host '" + host + "' with path '" + path + "'.");
        return ServiceRegistrationParser.parse(response.getEntity().getContent());
      }
    } catch (Exception e) {
      throw new ServiceRegistryException("Unable to register '" + serviceType + "' on host '" + host + "' with path '"
              + path + "'.", e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to register '" + serviceType + "' on host '" + host + "' with path '"
            + path + "'. HTTP status=" + responseStatusCode);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#unRegisterService(java.lang.String, java.lang.String,
   *      java.lang.String)
   */
  @Override
  public void unRegisterService(String serviceType, String host, String path) throws ServiceRegistryException {
    String servicePath = "unregister";
    HttpPost post = new HttpPost(UrlSupport.concat(serviceURL, servicePath));
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("serviceType", serviceType));
      params.add(new BasicNameValuePair("host", host));
      params.add(new BasicNameValuePair("path", path));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
      post.setEntity(entity);
    } catch (UnsupportedEncodingException e) {
      throw new ServiceRegistryException("Can not url encode post parameters", e);
    }
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(post);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_NO_CONTENT) {
        logger.info("Unregistered '" + serviceType + "' on host '" + host + "' with path '" + path + "'.");
        return;
      }
    } catch (Exception e) {
      throw new ServiceRegistryException("Unable to register " + serviceType + " from host " + host + " on path "
              + path, e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to unregister '" + serviceType + "' on host '" + host + "' with path '"
            + path + "'. HTTP status=" + responseStatusCode);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#setMaintenanceStatus(java.lang.String,
   *      java.lang.String, boolean)
   */
  @Override
  public void setMaintenanceStatus(String serviceType, String host, boolean maintenance) throws IllegalStateException,
          ServiceRegistryException {
    String servicePath = "maintenance";
    HttpPost post = new HttpPost(UrlSupport.concat(serviceURL, servicePath));
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("serviceType", serviceType));
      params.add(new BasicNameValuePair("host", host));
      params.add(new BasicNameValuePair("maintenance", Boolean.toString(maintenance)));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
      post.setEntity(entity);
    } catch (UnsupportedEncodingException e) {
      throw new ServiceRegistryException("Can not url encode post parameters", e);
    }
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(post);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_NO_CONTENT) {
        logger.info("Set maintenance mode on '" + serviceType + "' host '" + host + "' to '" + maintenance + "'.");
        return;
      }
    } catch (Exception e) {
      throw new ServiceRegistryException("Unable to set maintenance mode on " + serviceType + " host " + host + " to '"
              + maintenance + "'", e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to set maintenace mode on '" + serviceType + "' host '" + host
            + "' to '" + maintenance + "'. HTTP status=" + responseStatusCode);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String)
   */
  @Override
  public Job createJob(String type) throws ServiceRegistryException {
    String servicePath = "job";
    HttpPost post = new HttpPost(UrlSupport.concat(serviceURL, servicePath));
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("jobType", type));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
      post.setEntity(entity);
    } catch (UnsupportedEncodingException e) {
      throw new ServiceRegistryException("Can not url encode post parameters", e);
    }
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(post);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_OK) {
        Job job = JobParser.parseJob(response.getEntity().getContent());
        logger.debug("Created a new job '{}'", job);
        return job;
      }
    } catch (Exception e) {
      throw new ServiceRegistryException("Unable to create a job of type '" + type, e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to create a job of type '" + type);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#updateJob(org.opencastproject.job.api.Job)
   */
  @Override
  public void updateJob(Job job) throws ServiceRegistryException {
    String servicePath = "job/" + job.getId() + ".xml";
    String jobXml;
    try {
      jobXml = JobParser.serializeToString(job);
    } catch (IOException e) {
      throw new ServiceRegistryException("Can not serialize job " + job, e);
    }
    HttpPut put = new HttpPut(UrlSupport.concat(serviceURL, servicePath));
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("job", jobXml));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
      put.setEntity(entity);
    } catch (UnsupportedEncodingException e) {
      throw new ServiceRegistryException("Can not url encode post parameters", e);
    }
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(put);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_NO_CONTENT) {
        logger.info("Updated job '{}'", job);
        return;
      }
    } catch (Exception e) {
      throw new ServiceRegistryException("Unable to update " + job, e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to update " + job);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getJob(java.lang.String)
   */
  @Override
  public Job getJob(String id) throws NotFoundException, ServiceRegistryException {
    String servicePath = "job/" + id + ".xml";
    HttpGet get = new HttpGet(UrlSupport.concat(serviceURL, servicePath));
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(get);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_NOT_FOUND) {
        throw new NotFoundException("Unable to locate job " + id);
      }
      if (responseStatusCode == HttpStatus.SC_OK) {
        return JobParser.parseJob(response.getEntity().getContent());
      }
    } catch (IOException e) {
      throw new ServiceRegistryException("Unable to get job id=" + id, e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to retrieve job " + id);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrations(java.lang.String)
   */
  @Override
  public List<ServiceRegistration> getServiceRegistrations(String serviceType) throws ServiceRegistryException {
    String servicePath = "services.xml";
    if (isNotBlank(serviceType)) {
      servicePath += ("?serviceType=" + serviceType);
    }
    HttpGet get = new HttpGet(UrlSupport.concat(serviceURL, servicePath));
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(get);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_OK) {
        JaxbServiceRegistrationList serviceList = ServiceRegistrationParser.parseRegistrations(response.getEntity()
                .getContent());
        return new ArrayList<ServiceRegistration>(serviceList.getRegistrations());
      }
    } catch (IOException e) {
      throw new ServiceRegistryException("Unable to get service registrations", e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to get service registrations");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistration(java.lang.String,
   *      java.lang.String)
   */
  @Override
  public ServiceRegistration getServiceRegistration(String serviceType, String host) throws ServiceRegistryException {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrations()
   */
  @Override
  public List<ServiceRegistration> getServiceRegistrations() throws ServiceRegistryException {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceStatistics()
   */
  @Override
  public List<ServiceStatistics> getServiceStatistics() throws ServiceRegistryException {
    String servicePath = "statistics.xml";
    HttpGet get = new HttpGet(UrlSupport.concat(serviceURL, servicePath));
    HttpResponse response = null;
    int responseStatusCode;
    try {
      response = client.execute(get);
      responseStatusCode = response.getStatusLine().getStatusCode();
      if (responseStatusCode == HttpStatus.SC_OK) {
        List<JaxbServiceStatistics> stats = ServiceRegistrationParser
                .parseStatistics(response.getEntity().getContent()).getStats();
        return new ArrayList<ServiceStatistics>(stats);
      }
    } catch (IOException e) {
      throw new ServiceRegistryException("Unable to get service statistics", e);
    } finally {
      client.close(response);
    }
    throw new ServiceRegistryException("Unable to get service statistics");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#count(java.lang.String,
   *      org.opencastproject.job.api.Job.Status)
   */
  @Override
  public long count(String serviceType, Status status) throws ServiceRegistryException {
    // TODO Auto-generated method stub
    return 0;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#count(java.lang.String,
   *      org.opencastproject.job.api.Job.Status, java.lang.String)
   */
  @Override
  public long count(String serviceType, Status status, String host) throws ServiceRegistryException {
    // TODO Auto-generated method stub
    return 0;
  }

  /**
   * Sets the trusted http client.
   * 
   * @param client
   *          the trusted http client
   */
  protected void setTrustedHttpClient(TrustedHttpClient client) {
    this.client = client;
  }

}
