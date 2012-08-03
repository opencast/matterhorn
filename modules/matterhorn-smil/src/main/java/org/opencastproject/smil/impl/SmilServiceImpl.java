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
package org.opencastproject.smil.impl;

import org.opencastproject.smil.api.SmilService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmilServiceImpl implements SmilService {

  private static final Logger logger = LoggerFactory.getLogger(SmilServiceImpl.class);

  public void activate(final ComponentContext cc) throws IllegalStateException {
    logger.info("activating SMIL Service");
  }

  public void deactivate() {
    logger.info("deactivating SMIL Service");
  }

}
