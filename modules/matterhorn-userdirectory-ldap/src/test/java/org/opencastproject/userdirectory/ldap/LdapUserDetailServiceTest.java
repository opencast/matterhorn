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
package org.opencastproject.userdirectory.ldap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Dictionary;
import java.util.Hashtable;


public class LdapUserDetailServiceTest {
  
  protected LdapUserDetailService service = null;
  
  @Before
  public void setup() throws Exception {
    Dictionary<String, String> props = new Hashtable<String, String>();
    props.put("org.opencastproject.userdirectory.ldap.searchbase", "ou=people,dc=berkeley,dc=edu");
    props.put("org.opencastproject.userdirectory.ldap.searchfilter", "(uid={0})");
    props.put("org.opencastproject.userdirectory.ldap.url", "ldap://ldap.berkeley.edu");
    props.put("org.opencastproject.userdirectory.ldap.roleattributes", "berkeleyEduAffiliations,departmentNumber");

    service = new LdapUserDetailService();
    service.updated(props);
  }
  
  @Ignore("Ignore this test by default, since it requires internet connectivity")
  @Test
  public void testLookup() throws Exception {
    UserDetails user = service.loadUserByUsername("231693");
    Assert.assertNotNull(user);
    Assert.assertTrue(user.getAuthorities().contains(new GrantedAuthorityImpl("ROLE_URSET")));
    Assert.assertTrue(user.getAuthorities().contains(new GrantedAuthorityImpl("ROLE_EMPLOYEE-TYPE-STAFF")));
  }
  
  /* At the time this test was written, the LDAP attributes for this user included the following:
    $ /usr/bin/ldapsearch -LLL -x -H ldap://ldap.berkeley.edu -b 'ou=people,dc=berkeley,dc=edu' '(uid=231693)'
    dn: uid=231693,ou=people,dc=berkeley,dc=edu
    berkeleyEduUnitCalNetDeptName: Ed Tech
    mail: jholtzman@berkeley.edu
    title: Senior Software Developer
    postalAddress: 9 Dwinelle Hall$Berkeley, CA 94720-2535
    postalCode: 94720-2535
    st: CA
    l: Berkeley
    street: 9 Dwinelle Hall
    telephoneNumber: +1 510 269-4829
    displayName: Josh Holtzman
    cn: Holtzman, Josh
    givenName: Josh
    sn: Holtzman
    berkeleyEduAffiliations: EMPLOYEE-TYPE-STAFF
    berkeleyEduModDate: 20110217155308Z
    berkeleyEduDeptUnitHierarchyString: UCBKL-EVCP2-VPAPF-URMED-URSET
    departmentNumber: URSET
    labeledUri: http://josh.media.berkeley.edu/
    o: University of California, Berkeley
    berkeleyEduPrimaryDeptUnitHierarchyString: UCBKL-EVCP2-VPAPF-URMED-URSET
    berkeleyEduUnitHRDeptName: ETS EducTechnology
    uid: 231693
    ou: people
    objectClass: top
    objectClass: person
    objectClass: organizationalperson
    objectClass: inetorgperson
    objectClass: berkeleyEduPerson
    objectClass: eduPerson
    objectClass: ucEduPerson
    berkeleyEduPrimaryDeptUnit: URSET
   */
}
