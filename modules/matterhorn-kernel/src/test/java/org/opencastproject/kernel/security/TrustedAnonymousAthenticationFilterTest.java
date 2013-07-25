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
package org.opencastproject.kernel.security;

import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

/**
 * Tests the {@link TrustedAnonymousAuthenticationFilter}
 */
public class TrustedAnonymousAthenticationFilterTest {

  private SecurityService securityService;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    securityService = EasyMock.createNiceMock(SecurityService.class);
    User user = new User("admin", DefaultOrganization.DEFAULT_ORGANIZATION_ID,
            new String[] { SecurityConstants.GLOBAL_ADMIN_ROLE });
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.replay(securityService);
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testTrusedAnonymousAuthenticationFilter() {
    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getHeader(SecurityConstants.AUTHORIZATION_HEADER)).andReturn("true");
    EasyMock.expect(request.getHeader(SecurityConstants.AUTHORIZATION_HEADER)).andReturn(null);
    EasyMock.replay(request);

    TrustedAnonymousAuthenticationFilter filter = new TrustedAnonymousAuthenticationFilter();
    boolean isAnonymousRequest = filter.applyAnonymousForThisRequest(request);
    Assert.assertFalse(isAnonymousRequest);

    isAnonymousRequest = filter.applyAnonymousForThisRequest(request);
    Assert.assertTrue(isAnonymousRequest);
  }

}
