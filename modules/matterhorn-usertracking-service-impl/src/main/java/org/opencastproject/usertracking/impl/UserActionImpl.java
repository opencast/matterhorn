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
package org.opencastproject.usertracking.impl;

import org.opencastproject.usertracking.api.UserAction;

import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A JAXB-annotated implementation of {@link UserAction}
 */
@Entity(name = "UserAction")
@Table(name = "USER_ACTION")
@NamedQueries({
        @NamedQuery(name = "findUserActions", query = "SELECT a FROM UserAction a"),
        @NamedQuery(name = "countSessionsGroupByMediapackage", query = "SELECT a.mediapackageId, COUNT(distinct a.sessionId), SUM(a.length) FROM UserAction a GROUP BY a.mediapackageId"),
        @NamedQuery(name = "countSessionsGroupByMediapackageByIntervall", query = "SELECT a.mediapackageId, COUNT(distinct a.sessionId), SUM(a.length) FROM UserAction a WHERE :begin <= a.created AND a.created <= :end GROUP BY a.mediapackageId"),
        @NamedQuery(name = "countSessionsOfMediapackage", query = "SELECT COUNT(distinct a.sessionId) FROM UserAction a WHERE a.mediapackageId = :mediapackageId"),
        @NamedQuery(name = "findLastUserActionsOfSession", query = "SELECT a FROM UserAction a  WHERE a.sessionId = :sessionId ORDER BY a.created DESC"),
        @NamedQuery(name = "findUserActionsByType", query = "SELECT a FROM UserAction a WHERE a.type = :type"),
        @NamedQuery(name = "findUserActionsByTypeAndMediapackageId", query = "SELECT a FROM UserAction a WHERE a.mediapackageId = :mediapackageId AND a.type = :type"),
        @NamedQuery(name = "findUserActionsByTypeAndMediapackageIdOrderByOutpointDESC", query = "SELECT a FROM UserAction a WHERE a.mediapackageId = :mediapackageId AND a.type = :type ORDER BY a.outpoint DESC"),
        @NamedQuery(name = "findUserActionsByIntervall", query = "SELECT a FROM UserAction a WHERE :begin <= a.created AND a.created <= :end"),
        @NamedQuery(name = "findUserActionsByTypeAndIntervall", query = "SELECT a FROM UserAction a WHERE :begin <= a.created AND a.created <= :end AND a.type = :type"),
        @NamedQuery(name = "findTotal", query = "SELECT COUNT(a) FROM UserAction a"),
        @NamedQuery(name = "findTotalByType", query = "SELECT COUNT(a) FROM UserAction a WHERE a.type = :type"),
        @NamedQuery(name = "findTotalByTypeAndMediapackageId", query = "SELECT COUNT(a) FROM UserAction a WHERE a.mediapackageId = :mediapackageId AND a.type = :type"),
        @NamedQuery(name = "findTotalByIntervall", query = "SELECT COUNT(a) FROM UserAction a WHERE :begin <= a.created AND a.created <= :end"),
        @NamedQuery(name = "findDistinctEpisodeIdTotalByIntervall", query = "SELECT COUNT(distinct a.mediapackageId) FROM UserAction a WHERE :begin <= a.created AND a.created <= :end"),
        @NamedQuery(name = "findTotalByTypeAndIntervall", query = "SELECT COUNT(a) FROM UserAction a WHERE :begin <= a.created AND a.created <= :end AND a.type = :type") })
@XmlType(name = "action", namespace = "http://usertracking.opencastproject.org/")
@XmlRootElement(name = "action", namespace = "http://usertracking.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class UserActionImpl implements UserAction {

  @Id
  @Column(name = "ID")
  @GeneratedValue(strategy = GenerationType.AUTO)
  @XmlElement(name = "id")
  private Long id;

  @Column(name = "MEDIA_PACKAGE_ID", length = 128)
  @XmlElement(name = "mediapackageId")
  private String mediapackageId;

  @Column(name = "USER_ID")
  @XmlElement(name = "userId")
  private String userId;

  @Column(name = "SESSION_ID")
  @XmlElement(name = "sessionId")
  private String sessionId;

  @Column(name = "INPOINT")
  @XmlElement(name = "inpoint")
  private int inpoint;

  @Column(name = "OUTPOINT")
  @XmlElement(name = "outpoint")
  private int outpoint;

  @Column(name = "LENGTH")
  @XmlElement(name = "length")
  private int length;

  @Column(name = "TYPE")
  @XmlElement(name = "type")
  private String type;

  @Basic(optional = false)
  @Column(name = "CREATED")
  @Temporal(TemporalType.TIMESTAMP)
  @XmlElement(name = "created")
  private Date created = new Date();

  /**
   * A no-arg constructor needed by JAXB
   */
  public UserActionImpl() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getMediapackageId() {
    return mediapackageId;
  }

  public void setMediapackageId(String mediapackageId) {
    this.mediapackageId = mediapackageId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public int getInpoint() {
    return inpoint;
  }

  public void setInpoint(int inpoint) {
    this.inpoint = inpoint;
    updateLength();
  }

  public int getOutpoint() {
    return outpoint;
  }

  public void setOutpoint(int outpoint) {
    this.outpoint = outpoint;
    updateLength();
  }

  public int getLength() {
    return length;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  private void updateLength() {
    this.length = this.outpoint - this.inpoint;
  }
}
