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
package org.opencastproject.userdirectory.jpa;

import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * JPA-annotated user object.
 */
@Entity
@Access(AccessType.FIELD)
@Table(name = "MH_USER")
@NamedQuery(name = "roles", query = "select distinct user.roles from JpaUser user")
public class JpaUser {

  /** The java.io.serialization uid */
  private static final long serialVersionUID = -6693877536928844019L;

  @Id
  @Column
  protected String username;

  @Column
  protected String password;

  @Column
  protected String organization;

  @ElementCollection
  @CollectionTable(name = "MH_ROLE", joinColumns = @JoinColumn(name = "USERNAME"))
  @Column(name = "ROLE")
  protected Set<String> roles;

  /**
   * No-arg constructor needed by JPA
   */
  public JpaUser() {
  }

  /**
   * Constructs a user with the specified username, password, and roles.
   * 
   * @param username
   *          the username
   * @param password
   *          the password
   * @param organization
   *          the organization
   * @param roles
   *          the roles
   */
  public JpaUser(String username, String password, String organization, Set<String> roles) {
    super();
    this.username = username;
    this.password = password;
    this.organization = organization;
    this.roles = roles;
  }

  /**
   * Gets this user's clear text password.
   * 
   * @return the user account's password
   */
  public String getPassword() {
    return password;
  }

  /**
   * Gets the user name.
   * 
   * @return the user name
   */
  public String getUsername() {
    return username;
  }

  /**
   * Gets the user's organization.
   * 
   * @return the organization
   */
  public String getOrganization() {
    return organization;
  }

  /**
   * Gets the user's roles.
   * 
   * @return the user's roles
   */
  public Set<String> getRoles() {
    return roles;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "{username=" + username + ", organization=" + organization + ", roles=" + roles + "}";
  }

}
