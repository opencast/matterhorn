/** 
  * Licensed to the Apache Software Foundation (ASF) under one 
  * or more contributor license agreements. See the NOTICE file 
  * distributed with this work for additional information 
  * regarding copyright ownership. The ASF licenses this file 
  * to you under the Apache License, Version 2.0 (the 
  * "License"); you may not use this file except in compliance 
  * with the License. You may obtain a copy of the License at 
  * 
  * http://www.apache.org/licenses/LICENSE-2.0 
  * 
  * Unless required by applicable law or agreed to in writing, 
  * software distributed under the License is distributed on an 
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
  * KIND, either express or implied. See the License for the 
  * specific language governing permissions and limitations 
  * under the License. 
  */
package org.apache.cxf.dosgi.dsw;

public class Constants {
    // Constants from RFC 119, they should ultimately be picked up from an OSGi class.
    public static final String EXPORTED_INTERFACES = "service.exported.interfaces";
    public static final String EXPORTED_INTERFACES_OLD = "osgi.remote.interfaces"; // for BW compatibility

    public static final String EXPORTED_CONFIGS = "service.exported.configs";
    public static final String EXPORTED_CONFIGS_OLD = "osgi.remote.configuration.type"; // for BW comp.
    
    public static final String EXPORTED_INTENTS = "service.exported.intents";
    public static final String EXPORTED_INTENTS_EXTRA = "service.exported.intents.extra";
    public static final String EXPORTED_INTENTS_OLD = "osgi.remote.requires.intents";
    
    public static final String IMPORTED = "service.imported";
    public static final String IMPORTD_CONFIGS = "service.imported.configs";
    
    public static final String INTENTS = "service.intents";
        
    // WSDL
    public static final String WSDL_CONFIG_TYPE = "wsdl";
    public static final String WSDL_CONFIG_PREFIX = "osgi.remote.configuration" + "." + WSDL_CONFIG_TYPE;
    public static final String SERVICE_NAMESPACE = WSDL_CONFIG_PREFIX + ".service.ns";
    
    // Provider prefix
    public static final String PROVIDER_PREFIX = "org.apache.cxf";
    
    // WS
    public static final String WS_CONFIG_TYPE = PROVIDER_PREFIX + ".ws";
    public static final String WS_ADDRESS_PROPERTY = WS_CONFIG_TYPE + ".address";
    public static final String WS_HTTP_SERVICE_CONTEXT = WS_CONFIG_TYPE + ".httpservice.context";
    public static final String WS_FRONTEND_PROP_KEY = WS_CONFIG_TYPE + ".frontend";
    public static final String WS_DATABINDING_PROP_KEY = WS_CONFIG_TYPE + ".databinding";
    
    // Rest
    public static final String RS_CONFIG_TYPE = PROVIDER_PREFIX + ".rs";
    public static final String RS_ADDRESS_PROPERTY = RS_CONFIG_TYPE + ".address";
    public static final String RS_HTTP_SERVICE_CONTEXT = RS_CONFIG_TYPE + ".httpservice.context";
    public static final String RS_DATABINDING_PROP_KEY = RS_CONFIG_TYPE + ".databinding";
    public static final String RS_PROVIDER_PROP_KEY = RS_CONFIG_TYPE + ".provider";
    public static final String RS_PROVIDER_EXPECTED_PROP_KEY = RS_PROVIDER_PROP_KEY + ".expected";
    public static final String RS_PROVIDER_GLOBAL_PROP_KEY = RS_PROVIDER_PROP_KEY + ".globalquery";
    
    // POJO (old value for WS)
    public static final String WS_CONFIG_TYPE_OLD = "pojo";
    public static final String WS_CONFIG_OLD_PREFIX = "osgi.remote.configuration." + WS_CONFIG_TYPE_OLD;
    public static final String WS_ADDRESS_PROPERTY_OLD = WS_CONFIG_OLD_PREFIX + ".address";
    public static final String WS_HTTP_SERVICE_CONTEXT_OLD = WS_CONFIG_OLD_PREFIX + ".httpservice.context"; 

    // Common Configuration Properties
    public static final String CHECK_BUNDLE = "check.bundle";
    public static final String DEFAULT_PORT_CONFIG = "default.port";
    public static final String DEFAULT_HOST_CONFIG = "default.host";
    public static final String DEFAULT_PORT_VALUE = "9000";
    public static final String DEFAULT_HOST_VALUE = "localhost";
    public final static String USE_MASTER_MAP = "use.master.map";
    
    
    //DSW Identification - TODO do we really need this one?
    public static final String DSW_CLIENT_ID = PROVIDER_PREFIX + ".remote.dsw.client";
    
}
