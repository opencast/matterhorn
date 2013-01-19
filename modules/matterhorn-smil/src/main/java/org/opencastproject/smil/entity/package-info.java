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
@XmlSchema(
           elementFormDefault = XmlNsForm.QUALIFIED,
           attributeFormDefault = XmlNsForm.UNQUALIFIED,
           namespace = "http://www.w3.org/2006/SMIL30/WD/Language",
           xmlns = {
               @XmlNs(
                      namespaceURI = "http://www.opencastproject.org/matterhorn/",
                      prefix = "oc"),
               @XmlNs(namespaceURI = "http://www.w3.org/2006/SMIL30/WD/Language",
                      prefix = "")

           })
package org.opencastproject.smil.entity;

import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;