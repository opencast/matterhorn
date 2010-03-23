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
package org.opencastproject.feedback.api;


/**
 * A class that represents an annotation
 * 
 */
public interface Annotation {

  int getAnnotationId();

  void setAnnotationId(int annotationId);

  String getMediapackageId();

  void setMediapackageId(String mediapackageId);

  int getSessionId();

  void setSessionId(int sessionId);

  int getInpoint();

  void setInpoint(int inpoint);

  int getOutpoint();

  void setOutpoint(int outpoint);

  String getKey();

  void setKey(String key);

  String getValue();

  void setValue(String value);

  String getCreated();

  void setCreated(String created);

}
