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
package org.opencastproject.userdirectory;

import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserProvider;

import org.osgi.service.component.ComponentContext;

import java.util.HashMap;
import java.util.Map;

/**
 * An in-memory user directory containing the users and roles used by the system.
 */
public class InMemoryUserProvider implements UserProvider, RoleProvider {

  /** The roles associated with the matterhorn system account */
  public static final String[] MH_SYSTEM_ROLES = new String[] { "ROLE_ADMIN", "ROLE_USER" };

  /**
   * A collection of accounts internal to Matterhorn.
   */
  protected Map<String, User> internalAccounts = null;

  /**
   * Callback to activate the component.
   * 
   * @param cc
   *          the declarative services component context
   */
  protected void activate(ComponentContext cc) {
    internalAccounts = new HashMap<String, User>();
    String digestUsername = cc.getBundleContext().getProperty("org.opencastproject.security.digest.user");
    String digestUserPass = cc.getBundleContext().getProperty("org.opencastproject.security.digest.pass");
    User systemAccount = new User(digestUsername, digestUserPass, MH_SYSTEM_ROLES);
    internalAccounts.put(digestUsername, systemAccount);
    
    String adminUsername = cc.getBundleContext().getProperty("org.opencastproject.security.demo.admin.user");
    String adminUserPass = cc.getBundleContext().getProperty("org.opencastproject.security.demo.admin.pass");
    User administrator = new User(adminUsername, adminUserPass, MH_SYSTEM_ROLES);
    internalAccounts.put(adminUsername, administrator);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.RoleProvider#getRoles()
   */
  @Override
  public String[] getRoles() {
    return MH_SYSTEM_ROLES;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.UserProvider#loadUser(java.lang.String)
   */
  @Override
  public User loadUser(String userName) {
    return internalAccounts.get(userName);
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return getClass().getName();
  }
}
