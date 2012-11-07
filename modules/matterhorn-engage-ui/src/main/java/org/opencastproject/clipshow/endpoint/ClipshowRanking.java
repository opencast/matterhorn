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
package org.opencastproject.clipshow.endpoint;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "clipshow-ranking-blob", namespace = "http://clipshow.engage.opencastproject.org")
@XmlRootElement(name = "clipshow-ranking-blob", namespace = "http://clipshow.engage.opencastproject.org")
@XmlAccessorType(XmlAccessType.FIELD)
public class ClipshowRanking {

  @XmlElement(name = "top_users")
  private List<String> topUsers;

  @XmlElement(name = "my_score")
  private Integer myScore;

  public ClipshowRanking() { }

  public List<String> getTopUsers() {
    return topUsers;
  }

  public void setTopUsers(List<String> topUsers) {
    this.topUsers = topUsers;
  }

  public Integer getMyScore() {
    return myScore;
  }

  public void setMyScore(Integer myScore) {
    this.myScore = myScore;
  }

}
