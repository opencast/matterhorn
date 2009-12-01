/**
 *  Copyright 2009 The Regents of the University of California
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
package org.opencastproject.util;

import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Testing the DocUtils
 */
public class DocUtilTest {

  @Before
  public void setup() throws Exception {
    DocUtil.reset();
  }

  @After
  public void teardown() throws Exception {
  }

  @Test
  public void testGenerate() throws Exception {
    // extremely basic first
    String name = "AZ_test";
    String title = "AZ REST DOC";
    DocRestData data;
    String document;

    data = new DocRestData(name, title, "/azservice/rest", new String[] {"My first note"});
    document = DocUtil.generate(data);
    assertNotNull(document);
    assertFalse(document.startsWith("ERROR::"));
    assertTrue(document.contains(title));

    // now for one with some real endpoint data
    data = new DocRestData(name, title, "/azservice/rest", new String[] {"My first note"});
    data.addNote("my second note");
    RestEndpoint endpoint = new RestEndpoint("name1",RestEndpoint.Method.GET,"/path1",null);
    endpoint.addRequiredParam( new Param("rp1",Param.Type.STRING,null,null,null) );
    endpoint.addFormat( Format.json() );
    endpoint.addStatus( Status.OK(null) );
    endpoint.addStatus( new Status(500, null) );
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);
    RestEndpoint endpoint2 = new RestEndpoint("name2",RestEndpoint.Method.POST,"/path2","description for this thing 2");
    endpoint2.addRequiredParam( new Param("rp2",Param.Type.STRING,"description r p 2","default-rp2",null) );
    endpoint2.addOptionalParam( new Param("rp3",Param.Type.BOOLEAN,"description r p 3","true",null) );
    endpoint2.addOptionalParam( new Param("rp4",Param.Type.ENUM,"description r p 4","choice2",new String[] {"choice1","choice2","choice3"}) );
    endpoint2.addOptionalParam( new Param("rp5",Param.Type.FILE,"description r p 5",null,null) );
    endpoint2.addOptionalParam( new Param("rp6",Param.Type.STRING,"description r p 6","default-rp6",null) );
    endpoint2.addOptionalParam( new Param("rp7",Param.Type.TEXT,"description r p 7","<xml>\n  <thing>this is something</thing>\n</xml>",null) );
    endpoint2.addFormat( new Format(Format.JSON, "json is a format that is cool or something", null) );
    endpoint2.addFormat( Format.xml() );
    endpoint2.addStatus( new Status(201, "created the new thingy") );
    endpoint2.addStatus( Status.BAD_REQUEST("oopsy") );
    endpoint2.addStatus( Status.ERROR("Something is broke!") );
    endpoint2.addNote("this is the first note for this endpoint");
    endpoint2.addNote("this is the second note for this endpoint");
    endpoint2.setTestForm( RestTestForm.auto() );
    data.addEndpoint(RestEndpoint.Type.WRITE, endpoint2);
    document = DocUtil.generate(data);
    assertNotNull(document);
    assertFalse(document.startsWith("ERROR::"));
    assertTrue(document.contains(title));
  }

}
