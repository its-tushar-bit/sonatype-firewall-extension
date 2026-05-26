/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapConnectionDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapAuthenticationMethod;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.security.SecurityAspectControl;
import com.sonatype.insight.brain.security.TestEncryptionKeyStore;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;

import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Mapping tests ported from nexus-ldap-common; LDAP schema files (ldif) were not changed.
 *
 * @since 1.7
 */
public class LdapUserAndGroupMappingTest
{
  private LdapAuthenticationMethod authentication;

  @Rule
  public TestLdapServer testLdapServer = new TestLdapServer();

  private LdapServer ldapServer;

  private LdapService ldapService;

  // In-memory DAO storage
  private final Map<String, LdapServer> ldapServerStore = new HashMap<>();

  private final Map<String, LdapConnection> ldapConnectionStore = new HashMap<>();

  private final Map<String, LdapUserMapping> ldapUserMappingStore = new HashMap<>();

  @Before
  public void bindSecurityManager() {
    SecurityManager securityManager = mock(SecurityManager.class);
    ThreadContext.bind(securityManager);
    SecurityAspectControl.disableEnforcement();
    SimplePrincipalCollection principals = new SimplePrincipalCollection("testUser", "testRealm");
    Subject subject = new Subject.Builder(securityManager)
        .principals(principals)
        .authenticated(true)
        .buildSubject();
    ThreadContext.bind(subject);
  }

  @After
  public void unbindSecurityManager() {
    SecurityAspectControl.enableEnforcement();
    ThreadContext.unbindSubject();
    ThreadContext.unbindSecurityManager();
  }

  @Before
  public void initialize() {
    authentication = LdapAuthenticationMethod.SIMPLE;

    ldapServerStore.clear();
    ldapConnectionStore.clear();
    ldapUserMappingStore.clear();

    PasswordHandler passwordHandler = new PasswordHandler(new TestEncryptionKeyStore());

    LdapServerDAO ldapServerDAO = mock(LdapServerDAO.class);
    LdapConnectionDAO ldapConnectionDAO = mock(LdapConnectionDAO.class);
    LdapUserMappingDAO ldapUserMappingDAO = mock(LdapUserMappingDAO.class);

    when(ldapServerDAO.getAll()).thenAnswer(inv -> new ArrayList<>(ldapServerStore.values()));
    when(ldapServerDAO.getByIdNotNull(anyString())).thenAnswer(inv -> {
      String id = inv.getArgument(0);
      LdapServer server = ldapServerStore.get(id);
      if (server == null) {
        throw new RuntimeException("LdapServer not found: " + id);
      }
      return server;
    });

    doAnswer(inv -> {
      LdapConnection conn = inv.getArgument(0);
      if (conn.getId() == null) {
        conn.setId(UUID.randomUUID().toString());
      }
      ldapConnectionStore.put(conn.getServerId(), conn);
      return null;
    }).when(ldapConnectionDAO).insert(any(LdapConnection.class));

    doAnswer(inv -> {
      LdapConnection conn = inv.getArgument(0);
      ldapConnectionStore.put(conn.getServerId(), conn);
      return null;
    }).when(ldapConnectionDAO).update(any(LdapConnection.class));

    when(ldapConnectionDAO.getByServerId(anyString())).thenAnswer(inv -> {
      String serverId = inv.getArgument(0);
      LdapConnection conn = ldapConnectionStore.get(serverId);
      return conn != null ? new LdapConnection(conn) : null;
    });

    when(ldapUserMappingDAO.getByServerId(anyString())).thenAnswer(inv -> {
      String serverId = inv.getArgument(0);
      return ldapUserMappingStore.get(serverId);
    });

    ldapService = new LdapService(passwordHandler, ldapServerDAO, ldapConnectionDAO, ldapUserMappingDAO);
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
    // Create LdapServer with a manual ID instead of tempEntity.newLdapServer()
    ldapServer = new LdapServer("Test Server");
    ldapServer.setId(UUID.randomUUID().toString());
    ldapServerStore.put(ldapServer.getId(), ldapServer);

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

  // Constructed directly rather than via ldapService.getLdapConnection() to avoid
  // needing a pre-persisted connection — the mock DAO handles persistence in upsertLdapConnection.
  private LdapConnection createLdapConnection() {
    LdapConnection ldapConnection = new LdapConnection();
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
