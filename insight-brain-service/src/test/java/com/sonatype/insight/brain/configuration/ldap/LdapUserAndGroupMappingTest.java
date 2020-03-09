/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.configuration.ldap.LdapAuthenticationMethod;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;

import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mapping tests ported from nexus-ldap-common; LDAP schema files (ldif) were not changed.
 * 
 * @since 1.7
 */
public class LdapUserAndGroupMappingTest
    extends InjectedTest
{
  private LdapAuthenticationMethod authentication;

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Rule
  public TestLdapServer ldapServer = new TestLdapServer();

  private LdapServer serverDetails;

  @Inject
  private LdapService ldapService;

  @Before
  public void initialize() {
    authentication = LdapAuthenticationMethod.SIMPLE;
  }

  @Test
  public void testSimpleLdapSchema() throws Exception {
    startLdapServer().loadData("SimpleLdapSchema.ldif");

    LdapConnection conn = createLdapConnection();
    LdapUserMapping umap = createUserMapping();

    checkMapping(conn, umap);
  }

  @Test
  public void testCramMd5AuthLdapSchema() throws Exception {
    withCramAuth().startLdapServer().loadData("CramMd5AuthLdapSchema.ldif");

    LdapConnection conn = createLdapConnection();
    LdapUserMapping umap = createUserMapping();

    conn.setAuthenticationMethod(LdapAuthenticationMethod.CRAMMD5);
    conn.setSystemUsername(ldapServer.getSystemUser());
    conn.setSaslRealm("localhost");

    checkMapping(conn, umap);
  }

  @Test
  public void testDigestMd5AuthLdapSchema() throws Exception {
    withDigestAuth().startLdapServer().loadData("DigestMd5AuthLdapSchema.ldif");

    LdapConnection conn = createLdapConnection();
    LdapUserMapping umap = createUserMapping();

    conn.setAuthenticationMethod(LdapAuthenticationMethod.DIGESTMD5);
    conn.setSystemUsername(ldapServer.getSystemUser());
    conn.setSaslRealm("localhost");

    checkMapping(conn, umap);
  }

  @Test
  public void testDigestMd5NoRealmLdapSchema() throws Exception {
    withDigestAuth().startLdapServer().loadData("DigestMd5NoRealmLdapSchema.ldif");

    LdapConnection conn = createLdapConnection();
    LdapUserMapping umap = createUserMapping();

    conn.setAuthenticationMethod(LdapAuthenticationMethod.DIGESTMD5);
    conn.setSystemUsername(ldapServer.getSystemUser());

    checkMapping(conn, umap);
  }

  @Test
  public void testEncryptedPassSchema() throws Exception {
    startLdapServer().loadData("EncryptedPassSchema.ldif");

    LdapConnection conn = createLdapConnection();
    LdapUserMapping umap = createUserMapping();

    umap.setUserPasswordAttribute(null);

    checkMapping(conn, umap);
  }

  @Test
  public void testUserHasGroupLdapSchema() throws Exception {
    startLdapServer().loadData("UserHasGroupLdapSchema.ldif");

    LdapConnection conn = createLdapConnection();
    LdapUserMapping umap = createUserMapping();

    umap.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    umap.setUserMemberOfGroupAttribute("businesscategory");

    // reset unused static mappings...
    umap.setGroupObjectClass(null);
    umap.setGroupBaseDN(null);
    umap.setGroupIDAttribute(null);
    umap.setGroupMemberAttribute(null);
    umap.setGroupMemberFormat(null);

    checkMapping(conn, umap);
  }

  @Test
  public void testDynaGroupMissingSchema() throws Exception {
    startLdapServer().loadData("DynaGroupMissingSchema.ldif");

    LdapConnection conn = createLdapConnection();
    LdapUserMapping umap = createUserMapping();

    umap.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    umap.setUserMemberOfGroupAttribute("businesscategory");

    // reset unused static mappings...
    umap.setGroupObjectClass(null);
    umap.setGroupBaseDN(null);
    umap.setGroupIDAttribute(null);
    umap.setGroupMemberAttribute(null);
    umap.setGroupMemberFormat(null);

    checkMapping(conn, umap);
  }

  @Test
  public void testDynamicNoUserBaseDnLdapSchema() throws Exception {
    startLdapServer().loadData("DynamicNoUserBaseDnLdapSchema.ldif");

    LdapConnection conn = createLdapConnection();
    LdapUserMapping umap = createUserMapping();

    umap.setUserBaseDN(null);
    umap.setUserSubtree(true);

    umap.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    umap.setUserMemberOfGroupAttribute("businesscategory");

    // reset unused static mappings...
    umap.setGroupObjectClass(null);
    umap.setGroupBaseDN(null);
    umap.setGroupIDAttribute(null);
    umap.setGroupMemberAttribute(null);
    umap.setGroupMemberFormat(null);

    checkMapping(conn, umap);
  }

  @Test
  public void testNestedGroupsLdapSchema() throws Exception {
    startLdapServer().loadData("NestedGroupsLdapSchema.ldif");

    LdapConnection conn = createLdapConnection();
    LdapUserMapping umap = createUserMapping();

    umap.setGroupSubtree(true);

    checkMapping(conn, umap);
  }

  @Test
  public void testNestedGroupsNoGroupDNLdapSchema() throws Exception {
    startLdapServer().loadData("NestedGroupsNoGroupDNLdapSchema.ldif");

    LdapConnection conn = createLdapConnection();
    LdapUserMapping umap = createUserMapping();

    umap.setGroupBaseDN(null);
    umap.setGroupSubtree(true);

    checkMapping(conn, umap);
  }

  @Test
  public void testNestedUsersDnGroupLdapSchema() throws Exception {
    startLdapServer().loadData("NestedUsersDnGroupLdapSchema.ldif");

    LdapConnection conn = createLdapConnection();
    LdapUserMapping umap = createUserMapping();

    umap.setUserSubtree(true);
    umap.setGroupMemberFormat("cn=${username}*");

    checkMapping(conn, umap);
  }

  @Test
  public void testNestedUsersLdapSchema() throws Exception {
    startLdapServer().loadData("NestedUsersLdapSchema.ldif");

    LdapConnection conn = createLdapConnection();
    LdapUserMapping umap = createUserMapping();

    umap.setUserSubtree(true);
    umap.setGroupObjectClass("posixGroup");
    umap.setGroupMemberAttribute("memberUid");
    umap.setGroupMemberFormat("${username}");

    checkMapping(conn, umap);
  }

  private LdapUserAndGroupMappingTest withDigestAuth() {
    authentication = LdapAuthenticationMethod.DIGESTMD5;
    return this;
  }

  private LdapUserAndGroupMappingTest withCramAuth() {
    authentication = LdapAuthenticationMethod.CRAMMD5;
    return this;
  }

  private LdapUserAndGroupMappingTest checkMapping(LdapConnection conn, LdapUserMapping umap) throws Exception {
    ldapService.upsertLdapConnection(conn);
    List<LdapUser> users = ldapService.testUserMapping(umap, -1);
    Collections.sort(users);

    LdapUser user;

    user = users.get(0);
    assertThat(user.getUsername()).isEqualTo("brianf");
    assertThat(user.getRealName()).isEqualTo("Brian Fox");
    assertThat(user.getEmail()).isEqualTo("brianf@sonatype.com");
    assertThat(user.getMembership()).containsExactlyInAnyOrder("public", "releases");

    ldapService.testUserLogin(umap, "brianf", "brianf123".toCharArray());

    user = users.get(1);
    assertThat(user.getUsername()).isEqualTo("cstamas");
    assertThat(user.getRealName()).isEqualTo("Tamas Cservenak");
    assertThat(user.getEmail()).isEqualTo("cstamas@sonatype.com");
    assertThat(user.getMembership()).containsExactlyInAnyOrder("public", "snapshots");

    ldapService.testUserLogin(umap, "cstamas", "cstamas123".toCharArray());

    user = users.get(2);
    assertThat(user.getUsername()).isEqualTo("jvanzyl");
    assertThat(user.getRealName()).isEqualTo("Jason Van Zyl");
    assertThat(user.getEmail()).isEqualTo("jvanzyl@sonatype.com");
    assertThat(user.getMembership()).containsExactlyInAnyOrder("public", "releases", "snapshots");

    ldapService.testUserLogin(umap, "jvanzyl", "jvanzyl123".toCharArray());

    return this;
  }

  private LdapUserAndGroupMappingTest startLdapServer() throws Exception {
    serverDetails = tempEntity.newLdapServer("Test Server");

    if (authentication == LdapAuthenticationMethod.SIMPLE) {
      ldapServer.setAuthenticationSimple();
    }
    else if (authentication == LdapAuthenticationMethod.DIGESTMD5) {
      ldapServer.setAuthenticationSasl(SupportedSaslMechanisms.DIGEST_MD5);
    }
    else if (authentication == LdapAuthenticationMethod.CRAMMD5) {
      ldapServer.setAuthenticationSasl(SupportedSaslMechanisms.CRAM_MD5);
    }
    ldapServer.start();

    return this;
  }

  private LdapUserAndGroupMappingTest loadData(String resource) throws Exception {
    ldapServer.loadData("/" + getClass().getSimpleName() + "/" + resource);

    return this;
  }

  private LdapConnection createLdapConnection() {
    LdapConnection conn = ldapService.getLdapConnection(serverDetails.getId());
    conn.setServerId(serverDetails.getId());

    conn.setProtocol(LdapProtocol.LDAP);
    conn.setHostname(ldapServer.getHostname());
    conn.setPort(ldapServer.getPort());
    conn.setSearchBase("o=sonatype");

    conn.setAuthenticationMethod(LdapAuthenticationMethod.SIMPLE);
    conn.setSystemUsername(ldapServer.getSystemUserDN());
    conn.setSystemPassword(ldapServer.getSystemUserPassword());

    return conn;
  }

  private LdapUserMapping createUserMapping() {
    LdapUserMapping umap = new LdapUserMapping();
    umap.setServerId(serverDetails.getId());

    umap.setUserBaseDN("ou=people");
    umap.setUserObjectClass("inetOrgPerson");
    umap.setUserSubtree(false);
    umap.setUserIDAttribute("uid");
    umap.setUserRealNameAttribute("sn");
    umap.setUserEmailAttribute("mail");
    umap.setUserPasswordAttribute("userPassword");
    umap.setUserMemberOfGroupAttribute(null);
    umap.setUserFilter(null);

    umap.setGroupMappingType(LdapGroupMappingType.STATIC);

    umap.setGroupBaseDN("ou=groups");
    umap.setGroupObjectClass("groupOfUniqueNames");
    umap.setGroupSubtree(false);
    umap.setGroupIDAttribute("cn");
    umap.setGroupMemberAttribute("uniqueMember");
    umap.setGroupMemberFormat("uid=${username},ou=people,o=sonatype");

    return umap;
  }
}
