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
package org.opencastproject.rest;

import org.opencastproject.http.SharedHttpContext;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.provider.JSONProvider;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Servlet;
import javax.ws.rs.Path;

/**
 * Listens for JAX-RS annotated services and publishes them to the global URL space using a single shared HttpContext.
 */
public class RestPublisher {
  private static final Logger logger = LoggerFactory.getLogger(RestPublisher.class);
  public static final String SERVICE_PROPERTY = "opencast.rest.url";
  public static final String SERVICE_FILTER = "(" + SERVICE_PROPERTY + "=*)";

  protected ComponentContext componentContext;
  protected ServiceTracker tracker = null;

  protected Map<String, ServiceRegistration> servletRegistrationMap;

  public void activate(ComponentContext componentContext) {
    logger.info("activate()");
    this.componentContext = componentContext;
    this.servletRegistrationMap = new ConcurrentHashMap<String, ServiceRegistration>();
    try {
      tracker = new RestServiceTracker();
    } catch (InvalidSyntaxException e) {
      throw new IllegalStateException(e);
    }
    tracker.open();
  }

  public void deactivate() {
    tracker.close();
  }

  /**
   * Creates a REST endpoint for the JAX-RS annotated service.
   * 
   * @param ref
   *          the osgi service reference
   * @param service
   *          The service itself
   */
  protected void createEndpoint(ServiceReference ref, Object service) {
    Object aliasObj = ref.getProperty(SERVICE_PROPERTY);
    if (aliasObj == null) {
      logger.warn("Unable to publish a REST endpoint for {}", service.getClass().getName());
      return;
    } else if (!(aliasObj instanceof String)) {
      logger.warn("Property '{}' must be a string, but is a {}", SERVICE_PROPERTY, aliasObj.getClass());
      return;
    }
    String alias = (String) aliasObj;
    CXFNonSpringServlet cxf = new CXFNonSpringServlet();
    ServiceRegistration reg = null;
    try {
      Dictionary<String, String> props = new Hashtable<String, String>();
      props.put("contextId", SharedHttpContext.HTTP_CONTEXT_ID);
      props.put("alias", alias);
      reg = componentContext.getBundleContext().registerService(Servlet.class.getName(), cxf, props);
    } catch (Exception e) {
      logger.info("Problem registering REST endpoint {} : {}", alias, e.getMessage());
      return;
    }
    servletRegistrationMap.put(alias, reg);
    
    // Set up cxf
    Bus bus = cxf.getBus();
    JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
    factory.setBus(bus);
    
    // Remove namespaces from json, since it's not useful for us
    JSONProvider jsonProvider = new JSONProvider();
    jsonProvider.setIgnoreNamespaces(true);
    factory.setProvider(jsonProvider);
    
    // Set the service class
    factory.setServiceClass(service.getClass());
    factory.setResourceProvider(service.getClass(), new SingletonResourceProvider(service));

    // Set the address to '/', which will force the use of the http service
    factory.setAddress("/");
    
    // Use the cxf classloader itself to create the cxf server
    ClassLoader bundleClassLoader = Thread.currentThread().getContextClassLoader();
    ClassLoader delegateClassLoader = JAXRSServerFactoryBean.class.getClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(delegateClassLoader);
      factory.create();
    } finally {
      Thread.currentThread().setContextClassLoader(bundleClassLoader);
    }
    logger.info("Registered REST endpoint at " + alias);
  }

  /**
   * Removes an endpoint
   * 
   * @param alias
   *          The URL space to reclaim
   */
  protected void destroyEndpoint(String alias) {
    ServiceRegistration reg = servletRegistrationMap.remove(alias);
    if(reg != null) {
      reg.unregister();
    }
  }

  public void deactivate(ComponentContext componentContext) {
    logger.info("deactivate()");
  }

  /**
   * A custom ServiceTracker that published JAX-RS annotated services with the {@link RestPublisher#SERVICE_PROPERTY}
   * property set to some non-null value.
   */
  class RestServiceTracker extends ServiceTracker {
    
    RestServiceTracker() throws InvalidSyntaxException {
      super(componentContext.getBundleContext(), componentContext.getBundleContext().createFilter(SERVICE_FILTER), null);
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
      destroyEndpoint(reference.getProperty(SERVICE_PROPERTY).toString());
      super.removedService(reference, service);
    }

    @Override
    public Object addingService(ServiceReference reference) {
      Object service = componentContext.getBundleContext().getService(reference);
      Path pathAnnotation = service.getClass().getAnnotation(Path.class);
      if (pathAnnotation == null) {
        logger.warn("{} was registered with '{}={}', but the service is not annotated with the JAX-RS "
                + "@Path annotation",
                new Object[] { service, SERVICE_PROPERTY, reference.getProperty(SERVICE_PROPERTY) });
        return null;
      }
      createEndpoint(reference, service);
      return super.addingService(reference);
    }
  }
}
