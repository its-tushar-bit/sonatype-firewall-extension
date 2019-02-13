/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapAuthenticationMethod;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class LdapResourceAuditTest
    extends AbstractAuditTest
{
  private LdapServer ldapServer;

  @Before
  public void before() {
    ldapServer = tempEntity.newLdapServer(tempEntity.uuid());
  }

  @Test
  public void testAddLdapServer() throws Exception {
    LdapServer server = new LdapServer(tempEntity.uuid());
    LdapServer persistedServer = ldapRequest().body(server).post().getBody(LdapServer.class);
    try {
      AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_LDAP_SERVER, null);
      assertThat(persistedServer).isNotNull();
      assertLdapServerData(auditDTO, persistedServer);
    }
    finally {
      cleanUp(persistedServer);
    }
  }

  @Test
  public void testAddLdapServer_Unauthorized() throws Exception {
    LdapServer server = new LdapServer(tempEntity.uuid());
    ldapRequest().with(unauthorizedUser()).body(server).post();

    assertAuditLog(AuditEvent.CREATE_LDAP_SERVER, "unauthorized");
  }

  @Test
  public void testUpdateLdapServer() throws Exception {
    ldapServer.setName("updated-name");
    ldapRequest().body(ldapServer).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_LDAP_SERVER, null);
    assertLdapServerData(auditDTO, ldapServer);
  }

  @Test
  public void testUpdateLdapServer_Unauthorized() throws Exception {
    ldapRequest().with(unauthorizedUser()).body(ldapServer).put();

    assertAuditLog(AuditEvent.UPDATE_LDAP_SERVER, "unauthorized");
  }

  @Test
  public void testDeleteLdapServer() throws Exception {
    ldapRequest().path(ldapServer.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_LDAP_SERVER, null);
    assertLdapServerData(auditDTO, ldapServer);
  }

  @Test
  public void testDeleteLdapServer_Unauthorized() throws Exception {
    ldapRequest().path(ldapServer.getId()).with(unauthorizedUser()).delete();

    assertAuditLog(AuditEvent.DELETE_LDAP_SERVER, "unauthorized");
  }

  @Test
  public void testUpdateLdapConnection_AuthenticationMethodNONE() throws Exception {
    testUpdateLdapConnection(LdapAuthenticationMethod.NONE, "none");
  }

  @Test
  public void testUpdateLdapConnection_AuthenticationMethodDIGESTMD5() throws Exception {
    testUpdateLdapConnection(LdapAuthenticationMethod.DIGESTMD5, "digest-md5");
  }

  @Test
  public void testUpdateLdapConnection_Unauthorized() throws Exception {
    LdapConnection ldapConnection = createLdapConnection(LdapAuthenticationMethod.NONE);
    ldapRequest().path(LdapResource.CONNECTION_PATH).parameter(ldapServer.getId()).with(unauthorizedUser())
        .body(ldapConnection).put();

    assertAuditLog(AuditEvent.CONFIGURE_LDAP_CONNECTION, "unauthorized");
  }

  @Test
  public void testUpdateUserMapping_GroupMappingTypeNONE_Enabled() throws Exception {
    testUpdateUserMapping(LdapGroupMappingType.NONE, "none", true);
  }

  @Test
  public void testUpdateUserMapping_GroupMappingTypeNONE_Disabled() throws Exception {
    testUpdateUserMapping(LdapGroupMappingType.NONE, "none", false);
  }

  @Test
  public void testUpdateUserMapping_GroupMappingTypeSTATIC_Enabled() throws Exception {
    testUpdateUserMapping(LdapGroupMappingType.STATIC, "static", true);
  }

  @Test
  public void testUpdateUserMapping_GroupMappingTypeSTATIC_Disabled() throws Exception {
    testUpdateUserMapping(LdapGroupMappingType.STATIC, "static", false);
  }

  @Test
  public void testUpdateUserMapping_GroupMappingTypeDYNAMIC_Enabled() throws Exception {
    testUpdateUserMapping(LdapGroupMappingType.DYNAMIC, "dynamic", true);
  }

  @Test
  public void testUpdateUserMapping_GroupMappingTypeDYNAMIC_Disabled() throws Exception {
    testUpdateUserMapping(LdapGroupMappingType.DYNAMIC, "dynamic", false);
  }

  @Test
  public void testUpdateUserMapping_Unauthorized() throws Exception {
    LdapUserMapping userMapping = newUserMapping(LdapGroupMappingType.NONE, false);
    ldapRequest().path(LdapResource.USER_MAPPING_PATH).parameter(ldapServer.getId()).with(unauthorizedUser())
        .body(userMapping).put();

    assertAuditLog(AuditEvent.CONFIGURE_LDAP_USER_MAPPING, "unauthorized");
  }

  @Test
  public void testUpdatePriority() throws Exception {
    LdapServer ldapServer2 = tempEntity.newLdapServer("server 2");
    LdapServer ldapServer3 = tempEntity.newLdapServer("server 3");
    List<LdapServer> servers = asList(ldapServer3, ldapServer2, ldapServer);
    ldapRequest().path(LdapResource.PRIORITY_PATH)
        .body(servers.stream().map(LdapServer::getId).collect(Collectors.toList())).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.PRIORITIZE_LDAP, null);
    assertCustomObject(auditDTO, "ldapServerOrder",
        servers.stream().map(LdapServerDTO::new).collect(Collectors.toList()));
  }

  @Test
  public void testUpdatePriority_Unauthorized() throws Exception {
    ldapRequest().path(LdapResource.PRIORITY_PATH).with(unauthorizedUser()).body(asList(ldapServer.getId())).put();

    assertAuditLog(AuditEvent.PRIORITIZE_LDAP, "unauthorized");
  }

  private void testUpdateUserMapping(final LdapGroupMappingType groupMappingType,
                                     final String expectedGroupMappingType,
                                     boolean enabledDisabled)
      throws Exception
  {
    LdapUserMapping userMapping = newUserMapping(groupMappingType, enabledDisabled);
    ldapRequest().path(LdapResource.USER_MAPPING_PATH).parameter(ldapServer.getId()).body(userMapping).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_LDAP_USER_MAPPING, null);
    assertLdapServerData(auditDTO, ldapServer);
    assertCustomData(auditDTO, "ldapUserBaseDn", userMapping.getUserBaseDN());
    assertCustomData(auditDTO, "ldapUserSubtree", enabledDisabled ? "enabled" : "disabled");
    assertCustomData(auditDTO, "ldapUserObjectClass", userMapping.getUserObjectClass());
    assertCustomData(auditDTO, "ldapUserFilter", userMapping.getUserFilter());
    assertCustomData(auditDTO, "ldapUserIdAttribute", userMapping.getUserIDAttribute());
    assertCustomData(auditDTO, "ldapUserRealNameAttribute", userMapping.getUserRealNameAttribute());
    assertCustomData(auditDTO, "ldapUserEmailAttribute", userMapping.getUserEmailAttribute());
    assertCustomData(auditDTO, "ldapUserPasswordAttribute", userMapping.getUserPasswordAttribute());
    assertCustomData(auditDTO, "ldapGroupType", expectedGroupMappingType);
    if (groupMappingType.equals(LdapGroupMappingType.STATIC)) {
      assertCustomData(auditDTO, "ldapStaticGroupBaseDn", userMapping.getGroupBaseDN());
      assertCustomData(auditDTO, "ldapStaticGroupSubtree", enabledDisabled ? "enabled" : "disabled");
      assertCustomData(auditDTO, "ldapStaticGroupObjectClass", userMapping.getGroupObjectClass());
      assertCustomData(auditDTO, "ldapStaticGroupIdAttribute", userMapping.getGroupIDAttribute());
      assertCustomData(auditDTO, "ldapStaticGroupMemberAttribute", userMapping.getGroupMemberAttribute());
      assertCustomData(auditDTO, "ldapStaticGroupMemberFormat", userMapping.getGroupMemberFormat());
    }
    if (groupMappingType.equals(LdapGroupMappingType.DYNAMIC)) {
      assertCustomData(auditDTO, "ldapDynamicGroupMemberOfAttribute", userMapping.getUserMemberOfGroupAttribute());
      assertCustomData(auditDTO, "ldapDynamicGroupSearch",
          enabledDisabled ? "enabled" : "disabled");
    }
  }

  private void testUpdateLdapConnection(final LdapAuthenticationMethod ldapAuthenticationMethod,
                                        final String expectedAuthMethodOutput) throws Exception
  {
    LdapConnection ldapConnection = createLdapConnection(ldapAuthenticationMethod);
    ldapRequest().path(LdapResource.CONNECTION_PATH).parameter(ldapServer.getId()).body(ldapConnection).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_LDAP_CONNECTION, null);
    assertLdapServerData(auditDTO, ldapServer);
    assertLdapConnectionData(auditDTO, ldapConnection, expectedAuthMethodOutput);
  }

  private void assertLdapConnectionData(final AuditDTO auditDTO,
                                        final LdapConnection ldapConnection,
                                        String expectedAuthMethodOutput)
  {
    assertCustomData(auditDTO, "ldapProtocol", ldapConnection.getProtocol().getProtocol());
    assertCustomData(auditDTO, "ldapHostname", ldapConnection.getHostname());
    assertCustomData(auditDTO, "ldapPort", ldapConnection.getPort());
    assertCustomData(auditDTO, "ldapSearchBaseDn", ldapConnection.getSearchBase());
    assertCustomData(auditDTO, "ldapAuthenticationMethod", expectedAuthMethodOutput);
    assertCustomData(auditDTO, "ldapSaslRealm", ldapConnection.getSaslRealm());
    assertCustomData(auditDTO, "ldapUsername", ldapConnection.getSystemUsername());
    assertCustomData(auditDTO, "ldapConnectionTimeoutInSeconds", ldapConnection.getConnectionTimeout());
    assertCustomData(auditDTO, "ldapRetryDelayInSeconds", ldapConnection.getRetryDelay());
  }

  private void assertLdapServerData(final AuditDTO auditDTO, final LdapServer server) {
    assertCustomData(auditDTO, "ldapServerId", server.getId());
    assertCustomData(auditDTO, "ldapServerName", server.getName());
  }

  private HttpRequest ldapRequest() {
    return restRequest().path(LdapResource.RESOURCE_PATH);
  }

  private void cleanUp(final LdapServer ldapServer) {
    new LdapServerDAO().delete(ldapServer); //to avoid conflicts with other tests relies on order
  }

  private LdapConnection createLdapConnection(LdapAuthenticationMethod ldapAuthenticationMethod) {
    LdapConnection conn = new LdapConnection();
    conn.setServerId(ldapServer.getId());
    conn.setProtocol(LdapProtocol.LDAP);
    conn.setHostname("localhost");
    conn.setPort(389);
    conn.setAuthenticationMethod(ldapAuthenticationMethod);
    if (!ldapAuthenticationMethod.equals(LdapAuthenticationMethod.NONE)) {
      conn.setSaslRealm("sasl-realm");
      conn.setSystemUsername("system");
      conn.setSystemPassword("password");
    }
    conn.setConnectionTimeout(10);
    conn.setRetryDelay(20);
    return conn;
  }

  private LdapUserMapping newUserMapping(LdapGroupMappingType groupMappingType, boolean enabledDisabled) {
    LdapUserMapping umap = new LdapUserMapping();
    umap.setServerId(ldapServer.getId());
    umap.setUserBaseDN("userBaseDN");
    umap.setUserSubtree(enabledDisabled);
    umap.setUserObjectClass("userObjectClass");
    umap.setUserFilter("userFilter");
    umap.setUserIDAttribute("userIDAttribute");
    umap.setUserRealNameAttribute("realNameAttribute");
    umap.setUserEmailAttribute("emailAttribute");
    umap.setUserPasswordAttribute("passwordAttribute");
    umap.setGroupMappingType(groupMappingType);
    if (groupMappingType.equals(LdapGroupMappingType.STATIC)) {
      umap.setGroupBaseDN("groupBaseDN");
      umap.setGroupSubtree(enabledDisabled);
      umap.setGroupObjectClass("groupObjectClass");
      umap.setGroupIDAttribute("groupIDAttribute");
      umap.setGroupMemberAttribute("groupMemberAttribute");
      umap.setGroupMemberFormat("groupMemberFormat");
    }
    if (groupMappingType.equals(LdapGroupMappingType.DYNAMIC)) {
      umap.setUserMemberOfGroupAttribute("userMemberOfGroupAttribute");
      umap.setDynamicGroupSearchEnabled(enabledDisabled);
    }
    return umap;
  }
}
