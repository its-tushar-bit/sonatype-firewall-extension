/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

import com.sonatype.insight.brain.model.configuration.ldap.LdapAuthenticationMethod;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.testing.BrainInjectedTest;

import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mapping tests ported from nexus-ldap-common; LDAP schema files (ldif) were not changed.
 * 
 * @since 1.7
 */
@Category(SlowTest.class)
public class LdapUserAndGroupMappingTest
    extends BrainInjectedTest
{
  private LdapAuthenticationMethod authentication;

  @Rule
  public TestLdapServer testLdapServer = new TestLdapServer();

  private LdapServer ldapServer;

  @Inject
  private LdapService ldapService;

  @Before
  public void initialize() {
    authentication = LdapAuthenticationMethod.SIMPLE;
  }

  @Test
  public void testSimpleLdapSchema() throws Exception {
    startLdapServer().loadData("SimpleLdapSchema.ldif");

    LdapConnection ldapConnection = createLdapConnection();
    LdapUserMapping ldapUserMapping = createUserMapping();

    checkMapping(ldapConnection, ldapUserMapping);
  }

  @Test
  public void testCramMd5AuthLdapSchema() throws Exception {
    withCramAuth().startLdapServer().loadData("CramMd5AuthLdapSchema.ldif");

    LdapConnection ldapConnection = createLdapConnection();
    LdapUserMapping ldapUserMapping = createUserMapping();

    ldapConnection.setAuthenticationMethod(LdapAuthenticationMethod.CRAMMD5);
    ldapConnection.setSystemUsername(testLdapServer.getSystemUser());
    ldapConnection.setSaslRealm("localhost");

    checkMapping(ldapConnection, ldapUserMapping);
  }

  @Test
  public void testDigestMd5AuthLdapSchema() throws Exception {
    withDigestAuth().startLdapServer().loadData("DigestMd5AuthLdapSchema.ldif");

    LdapConnection ldapConnection = createLdapConnection();
    LdapUserMapping ldapUserMapping = createUserMapping();

    ldapConnection.setAuthenticationMethod(LdapAuthenticationMethod.DIGESTMD5);
    ldapConnection.setSystemUsername(testLdapServer.getSystemUser());
    ldapConnection.setSaslRealm("localhost");

    checkMapping(ldapConnection, ldapUserMapping);
  }

  @Test
  public void testDigestMd5NoRealmLdapSchema() throws Exception {
    withDigestAuth().startLdapServer().loadData("DigestMd5NoRealmLdapSchema.ldif");

    LdapConnection ldapConnection = createLdapConnection();
    LdapUserMapping ldapUserMapping = createUserMapping();

    ldapConnection.setAuthenticationMethod(LdapAuthenticationMethod.DIGESTMD5);
    ldapConnection.setSystemUsername(testLdapServer.getSystemUser());

    checkMapping(ldapConnection, ldapUserMapping);
  }

  @Test
  public void testEncryptedPassSchema() throws Exception {
    startLdapServer().loadData("EncryptedPassSchema.ldif");

    LdapConnection ldapConnection = createLdapConnection();
    LdapUserMapping ldapUserMapping = createUserMapping();

    ldapUserMapping.setUserPasswordAttribute(null);

    checkMapping(ldapConnection, ldapUserMapping);
  }

  @Test
  public void testUserHasGroupLdapSchema() throws Exception {
    startLdapServer().loadData("UserHasGroupLdapSchema.ldif");

    LdapConnection ldapConnection = createLdapConnection();
    LdapUserMapping ldapUserMapping = createUserMapping();

    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    ldapUserMapping.setUserMemberOfGroupAttribute("businesscategory");

    // reset unused static mappings...
    ldapUserMapping.setGroupObjectClass(null);
    ldapUserMapping.setGroupBaseDN(null);
    ldapUserMapping.setGroupIDAttribute(null);
    ldapUserMapping.setGroupMemberAttribute(null);
    ldapUserMapping.setGroupMemberFormat(null);

    checkMapping(ldapConnection, ldapUserMapping);
  }

  @Test
  public void testDynaGroupMissingSchema() throws Exception {
    startLdapServer().loadData("DynaGroupMissingSchema.ldif");

    LdapConnection ldapConnection = createLdapConnection();
    LdapUserMapping ldapUserMapping = createUserMapping();

    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    ldapUserMapping.setUserMemberOfGroupAttribute("businesscategory");

    // reset unused static mappings...
    ldapUserMapping.setGroupObjectClass(null);
    ldapUserMapping.setGroupBaseDN(null);
    ldapUserMapping.setGroupIDAttribute(null);
    ldapUserMapping.setGroupMemberAttribute(null);
    ldapUserMapping.setGroupMemberFormat(null);

    checkMapping(ldapConnection, ldapUserMapping);
  }

  @Test
  public void testDynamicNoUserBaseDnLdapSchema() throws Exception {
    startLdapServer().loadData("DynamicNoUserBaseDnLdapSchema.ldif");

    LdapConnection ldapConnection = createLdapConnection();
    LdapUserMapping ldapUserMapping = createUserMapping();

    ldapUserMapping.setUserBaseDN(null);
    ldapUserMapping.setUserSubtree(true);

    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    ldapUserMapping.setUserMemberOfGroupAttribute("businesscategory");

    // reset unused static mappings...
    ldapUserMapping.setGroupObjectClass(null);
    ldapUserMapping.setGroupBaseDN(null);
    ldapUserMapping.setGroupIDAttribute(null);
    ldapUserMapping.setGroupMemberAttribute(null);
    ldapUserMapping.setGroupMemberFormat(null);

    checkMapping(ldapConnection, ldapUserMapping);
  }

  @Test
  public void testNestedGroupsLdapSchema() throws Exception {
    startLdapServer().loadData("NestedGroupsLdapSchema.ldif");

    LdapConnection ldapConnection = createLdapConnection();
    LdapUserMapping ldapUserMapping = createUserMapping();

    ldapUserMapping.setGroupSubtree(true);

    checkMapping(ldapConnection, ldapUserMapping);
  }

  @Test
  public void testNestedGroupsNoGroupDNLdapSchema() throws Exception {
    startLdapServer().loadData("NestedGroupsNoGroupDNLdapSchema.ldif");

    LdapConnection ldapConnection = createLdapConnection();
    LdapUserMapping ldapUserMapping = createUserMapping();

    ldapUserMapping.setGroupBaseDN(null);
    ldapUserMapping.setGroupSubtree(true);

    checkMapping(ldapConnection, ldapUserMapping);
  }

  @Test
  public void testNestedUsersDnGroupLdapSchema() throws Exception {
    startLdapServer().loadData("NestedUsersDnGroupLdapSchema.ldif");

    LdapConnection ldapConnection = createLdapConnection();
    LdapUserMapping ldapUserMapping = createUserMapping();

    ldapUserMapping.setUserSubtree(true);
    ldapUserMapping.setGroupMemberFormat("cn=${username}*");

    checkMapping(ldapConnection, ldapUserMapping);
  }

  @Test
  public void testNestedUsersLdapSchema() throws Exception {
    startLdapServer().loadData("NestedUsersLdapSchema.ldif");

    LdapConnection ldapConnection = createLdapConnection();
    LdapUserMapping ldapUserMapping = createUserMapping();

    ldapUserMapping.setUserSubtree(true);
    ldapUserMapping.setGroupObjectClass("posixGroup");
    ldapUserMapping.setGroupMemberAttribute("memberUid");
    ldapUserMapping.setGroupMemberFormat("${username}");

    checkMapping(ldapConnection, ldapUserMapping);
  }

  private LdapUserAndGroupMappingTest withDigestAuth() {
    authentication = LdapAuthenticationMethod.DIGESTMD5;
    return this;
  }

  private LdapUserAndGroupMappingTest withCramAuth() {
    authentication = LdapAuthenticationMethod.CRAMMD5;
    return this;
  }

  private LdapUserAndGroupMappingTest checkMapping(
      LdapConnection ldapConnection,
      LdapUserMapping ldapUserMapping)
  {
    ldapService.upsertLdapConnection(ldapConnection);
    List<LdapUser> users = ldapService.testLdapUserMapping(ldapUserMapping.getServerId(), ldapUserMapping, -1);
    Collections.sort(users);

    LdapUser user;

    user = users.get(0);
    assertThat(user.getUsername()).isEqualTo("brianf");
    assertThat(user.getRealName()).isEqualTo("Brian Fox");
    assertThat(user.getEmail()).isEqualTo("brianf@sonatype.com");
    assertThat(user.getMembership()).containsExactlyInAnyOrder("public", "releases");

    ldapService.testUserLogin(ldapUserMapping.getServerId(), ldapUserMapping, "brianf", "brianf123".toCharArray());

    user = users.get(1);
    assertThat(user.getUsername()).isEqualTo("cstamas");
    assertThat(user.getRealName()).isEqualTo("Tamas Cservenak");
    assertThat(user.getEmail()).isEqualTo("cstamas@sonatype.com");
    assertThat(user.getMembership()).containsExactlyInAnyOrder("public", "snapshots");

    ldapService.testUserLogin(ldapUserMapping.getServerId(), ldapUserMapping, "cstamas", "cstamas123".toCharArray());

    user = users.get(2);
    assertThat(user.getUsername()).isEqualTo("jvanzyl");
    assertThat(user.getRealName()).isEqualTo("Jason Van Zyl");
    assertThat(user.getEmail()).isEqualTo("jvanzyl@sonatype.com");
    assertThat(user.getMembership()).containsExactlyInAnyOrder("public", "releases", "snapshots");

    ldapService.testUserLogin(ldapUserMapping.getServerId(), ldapUserMapping, "jvanzyl", "jvanzyl123".toCharArray());

    return this;
  }

  private LdapUserAndGroupMappingTest startLdapServer() throws Exception {
    ldapServer = tempEntity.newLdapServer("Test Server");

    if (authentication == LdapAuthenticationMethod.SIMPLE) {
      testLdapServer.setAuthenticationSimple();
    }
    else if (authentication == LdapAuthenticationMethod.DIGESTMD5) {
      testLdapServer.setAuthenticationSasl(SupportedSaslMechanisms.DIGEST_MD5);
    }
    else if (authentication == LdapAuthenticationMethod.CRAMMD5) {
      testLdapServer.setAuthenticationSasl(SupportedSaslMechanisms.CRAM_MD5);
    }
    testLdapServer.start();

    return this;
  }

  private LdapUserAndGroupMappingTest loadData(String resource) throws Exception {
    testLdapServer.loadData("/" + getClass().getSimpleName() + "/" + resource);

    return this;
  }

  private LdapConnection createLdapConnection() {
    LdapConnection ldapConnection = ldapService.getLdapConnection(ldapServer.getId());
    ldapConnection.setServerId(ldapServer.getId());

    ldapConnection.setProtocol(LdapProtocol.LDAP);
    ldapConnection.setHostname(testLdapServer.getHostname());
    ldapConnection.setPort(testLdapServer.getPort());
    ldapConnection.setSearchBase("o=sonatype");

    ldapConnection.setAuthenticationMethod(LdapAuthenticationMethod.SIMPLE);
    ldapConnection.setSystemUsername(testLdapServer.getSystemUserDN());
    ldapConnection.setSystemPassword(testLdapServer.getSystemUserPassword());

    return ldapConnection;
  }

  private LdapUserMapping createUserMapping() {
    LdapUserMapping ldapUserMapping = new LdapUserMapping();
    ldapUserMapping.setServerId(ldapServer.getId());

    ldapUserMapping.setUserBaseDN("ou=people");
    ldapUserMapping.setUserObjectClass("inetOrgPerson");
    ldapUserMapping.setUserSubtree(false);
    ldapUserMapping.setUserIDAttribute("uid");
    ldapUserMapping.setUserRealNameAttribute("sn");
    ldapUserMapping.setUserEmailAttribute("mail");
    ldapUserMapping.setUserPasswordAttribute("userPassword");
    ldapUserMapping.setUserMemberOfGroupAttribute(null);
    ldapUserMapping.setUserFilter(null);

    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.STATIC);

    ldapUserMapping.setGroupBaseDN("ou=groups");
    ldapUserMapping.setGroupObjectClass("groupOfUniqueNames");
    ldapUserMapping.setGroupSubtree(false);
    ldapUserMapping.setGroupIDAttribute("cn");
    ldapUserMapping.setGroupMemberAttribute("uniqueMember");
    ldapUserMapping.setGroupMemberFormat("uid=${username},ou=people,o=sonatype");

    return ldapUserMapping;
  }
}
