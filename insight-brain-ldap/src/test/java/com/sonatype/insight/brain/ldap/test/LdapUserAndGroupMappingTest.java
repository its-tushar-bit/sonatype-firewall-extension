/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ldap.test;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.configuration.ldap.LdapAuthenticationMethod;
import com.sonatype.insight.brain.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.ldap.EmbeddedLdapServer;
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.ldap.LdapUser;

import org.sonatype.guice.bean.containers.InjectedTest;

import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.ldap.EmbeddedLdapServer.newEmbeddedLdapServer;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

/**
 * Mapping tests ported from nexus-ldap-common; LDAP schema files (ldif) were not changed.
 * 
 * @since 1.7
 */
public class LdapUserAndGroupMappingTest
    extends InjectedTest
{
  private final LdapServerDAO serverDao = new LdapServerDAO();

  private LdapAuthenticationMethod authentication;

  private EmbeddedLdapServer ldapServer;

  private LdapServer serverDetails;

  @Inject
  private LdapManager manager;

  @Before
  public void initialize() {
    authentication = LdapAuthenticationMethod.SIMPLE;
  }

  @Test
  public void testSimpleLdapSchema() throws Exception {
    startLdapServer().loadData("/schemas/SimpleLdapSchema.ldif");

    LdapConnection conn = createLdapConnection();
    LdapUserMapping umap = createUserMapping();

    checkMapping(conn, umap);
  }

  @Test
  public void testCramMd5AuthLdapSchema() throws Exception {
    withCramAuth().startLdapServer().loadData("/schemas/CramMd5AuthLdapSchema.ldif");

    LdapConnection conn = createLdapConnection();
    LdapUserMapping umap = createUserMapping();

    conn.setAuthenticationMethod(LdapAuthenticationMethod.CRAMMD5);
    conn.setSystemUsername(ldapServer.getSystemUser());
    conn.setSaslRealm("localhost");

    checkMapping(conn, umap);
  }

  @Test
  public void testDigestMd5AuthLdapSchema() throws Exception {
    withDigestAuth().startLdapServer().loadData("/schemas/DigestMd5AuthLdapSchema.ldif");

    LdapConnection conn = createLdapConnection();
    LdapUserMapping umap = createUserMapping();

    conn.setAuthenticationMethod(LdapAuthenticationMethod.DIGESTMD5);
    conn.setSystemUsername(ldapServer.getSystemUser());
    conn.setSaslRealm("localhost");

    checkMapping(conn, umap);
  }

  @Test
  public void testDigestMd5NoRealmLdapSchema() throws Exception {
    withDigestAuth().startLdapServer().loadData("/schemas/DigestMd5NoRealmLdapSchema.ldif");

    LdapConnection conn = createLdapConnection();
    LdapUserMapping umap = createUserMapping();

    conn.setAuthenticationMethod(LdapAuthenticationMethod.DIGESTMD5);
    conn.setSystemUsername(ldapServer.getSystemUser());

    checkMapping(conn, umap);
  }

  @Test
  public void testEncryptedPassSchema() throws Exception {
    startLdapServer().loadData("/schemas/EncryptedPassSchema.ldif");

    LdapConnection conn = createLdapConnection();
    LdapUserMapping umap = createUserMapping();

    umap.setUserPasswordAttribute(null);

    checkMapping(conn, umap);
  }

  @Test
  public void testUserHasGroupLdapSchema() throws Exception {
    startLdapServer().loadData("/schemas/UserHasGroupLdapSchema.ldif");

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
    startLdapServer().loadData("/schemas/DynaGroupMissingSchema.ldif");

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
    startLdapServer().loadData("/schemas/DynamicNoUserBaseDnLdapSchema.ldif");

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
    startLdapServer().loadData("/schemas/NestedGroupsLdapSchema.ldif");

    LdapConnection conn = createLdapConnection();
    LdapUserMapping umap = createUserMapping();

    umap.setGroupSubtree(true);

    checkMapping(conn, umap);
  }

  @Test
  public void testNestedGroupsNoGroupDNLdapSchema() throws Exception {
    startLdapServer().loadData("/schemas/NestedGroupsNoGroupDNLdapSchema.ldif");

    LdapConnection conn = createLdapConnection();
    LdapUserMapping umap = createUserMapping();

    umap.setGroupBaseDN(null);
    umap.setGroupSubtree(true);

    checkMapping(conn, umap);
  }

  @Test
  public void testNestedUsersDnGroupLdapSchema() throws Exception {
    startLdapServer().loadData("/schemas/NestedUsersDnGroupLdapSchema.ldif");

    LdapConnection conn = createLdapConnection();
    LdapUserMapping umap = createUserMapping();

    umap.setUserSubtree(true);
    umap.setGroupMemberFormat("cn=${username}*");

    checkMapping(conn, umap);
  }

  @Test
  public void testNestedUsersLdapSchema() throws Exception {
    startLdapServer().loadData("/schemas/NestedUsersLdapSchema.ldif");

    LdapConnection conn = createLdapConnection();
    LdapUserMapping umap = createUserMapping();

    umap.setUserSubtree(true);
    umap.setGroupObjectClass("posixGroup");
    umap.setGroupMemberAttribute("memberUid");
    umap.setGroupMemberFormat("${username}");

    checkMapping(conn, umap);
  }

  public LdapUserAndGroupMappingTest withDigestAuth() {
    authentication = LdapAuthenticationMethod.DIGESTMD5;
    return this;
  }

  public LdapUserAndGroupMappingTest withCramAuth() {
    authentication = LdapAuthenticationMethod.CRAMMD5;
    return this;
  }

  public LdapUserAndGroupMappingTest checkMapping(LdapConnection conn, LdapUserMapping umap) throws Exception {
    manager.saveConnection(conn);
    List<LdapUser> users = manager.testUserMapping(umap, -1);
    Collections.sort(users);

    LdapUser user;

    user = users.get(0);
    assertThat(user.getUsername(), is("brianf"));
    assertThat(user.getRealName(), is("Brian Fox"));
    assertThat(user.getEmail(), is("brianf@sonatype.com"));
    assertThat(user.getMembership(), containsInAnyOrder("public", "releases"));

    manager.testUserLogin(umap, "brianf", "brianf123".toCharArray());

    user = users.get(1);
    assertThat(user.getUsername(), is("cstamas"));
    assertThat(user.getRealName(), is("Tamas Cservenak"));
    assertThat(user.getEmail(), is("cstamas@sonatype.com"));
    assertThat(user.getMembership(), containsInAnyOrder("public", "snapshots"));

    manager.testUserLogin(umap, "cstamas", "cstamas123".toCharArray());

    user = users.get(2);
    assertThat(user.getUsername(), is("jvanzyl"));
    assertThat(user.getRealName(), is("Jason Van Zyl"));
    assertThat(user.getEmail(), is("jvanzyl@sonatype.com"));
    assertThat(user.getMembership(), containsInAnyOrder("public", "releases", "snapshots"));

    manager.testUserLogin(umap, "jvanzyl", "jvanzyl123".toCharArray());

    return this;
  }

  public LdapUserAndGroupMappingTest startLdapServer() throws Exception {
    serverDetails = new LdapServer();
    serverDetails.setName("Test Server");
    serverDao.insert(serverDetails);

    ldapServer = newEmbeddedLdapServer();
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

  public LdapUserAndGroupMappingTest loadData(String resource) throws Exception {
    ldapServer.loadData(resource);

    return this;
  }

  @After
  public void stopLdapServer() throws Exception {
    if (ldapServer != null) {
      ldapServer.stop();
      ldapServer = null;
    }

    for (LdapServer s : serverDao.getAll()) {
      serverDao.delete(s);
    }
    assertThat(serverDao.getAll(), is(empty()));
  }

  protected LdapConnection createLdapConnection() {
    LdapConnection conn = manager.loadConnection(serverDetails.getId());
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

  protected LdapUserMapping createUserMapping() {
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
