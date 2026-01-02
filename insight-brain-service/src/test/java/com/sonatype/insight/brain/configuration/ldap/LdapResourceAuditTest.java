/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;
import com.sonatype.insight.brain.common.test.SlowTest;


import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
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

import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class LdapResourceAuditTest
    extends AbstractAuditTest
{
  private LdapServerDAO ldapServerDAO;

  private LdapServer ldapServer;

  @Before
  public void before() {
    ldapServerDAO = lookup(LdapServerDAO.class);
    ldapServer = tempEntity.newLdapServer(TemporaryEntity.uuid());
  }

  @Test
  public void testAddLdapServer() throws Exception {
    LdapServer ldapServer = new LdapServer(TemporaryEntity.uuid());
    LdapServer persistedLdapServer = ldapRequest().body(ldapServer).post().getBody(LdapServer.class);
    try {
      AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_LDAP_SERVER, null);
      assertThat(persistedLdapServer).isNotNull();
      assertLdapServerData(auditDTO, persistedLdapServer);
    }
    finally {
      cleanUp(persistedLdapServer);
    }
  }

  @Test
  public void testAddLdapServer_Unauthorized() throws Exception {
    LdapServer ldapServer = new LdapServer(TemporaryEntity.uuid());
    ldapRequest().with(unauthorizedUser()).body(ldapServer).post();

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
  public void testUpsertLdapConnection_AuthenticationMethodNONE() throws Exception {
    testUpsertLdapConnection(LdapAuthenticationMethod.NONE, "none");
  }

  @Test
  public void testUpsertLdapConnection_AuthenticationMethodDIGESTMD5() throws Exception {
    testUpsertLdapConnection(LdapAuthenticationMethod.DIGESTMD5, "digest-md5");
  }

  @Test
  public void testUpsertLdapConnection_Unauthorized() throws Exception {
    LdapConnection ldapConnection = createLdapConnection(LdapAuthenticationMethod.NONE);
    ldapRequest().path(LdapResource.CONNECTION_PATH).parameter(ldapServer.getId()).with(unauthorizedUser())
        .body(ldapConnection).put();

    assertAuditLog(AuditEvent.CONFIGURE_LDAP_CONNECTION, "unauthorized");
  }

  @Test
  public void testUpsertUserMapping_GroupMappingTypeNONE_Enabled() throws Exception {
    testUpsertUserMapping(LdapGroupMappingType.NONE, "none", true);
  }

  @Test
  public void testUpsertUserMapping_GroupMappingTypeNONE_Disabled() throws Exception {
    testUpsertUserMapping(LdapGroupMappingType.NONE, "none", false);
  }

  @Test
  public void testUpsertUserMapping_GroupMappingTypeSTATIC_Enabled() throws Exception {
    testUpsertUserMapping(LdapGroupMappingType.STATIC, "static", true);
  }

  @Test
  public void testUpsertUserMapping_GroupMappingTypeSTATIC_Disabled() throws Exception {
    testUpsertUserMapping(LdapGroupMappingType.STATIC, "static", false);
  }

  @Test
  public void testUpsertUserMapping_GroupMappingTypeDYNAMIC_Enabled() throws Exception {
    testUpsertUserMapping(LdapGroupMappingType.DYNAMIC, "dynamic", true);
  }

  @Test
  public void testUpsertUserMapping_GroupMappingTypeDYNAMIC_Disabled() throws Exception {
    testUpsertUserMapping(LdapGroupMappingType.DYNAMIC, "dynamic", false);
  }

  @Test
  public void testUpsertUserMapping_Unauthorized() throws Exception {
    LdapUserMapping ldapUserMapping = newUserMapping(LdapGroupMappingType.NONE, false);
    ldapRequest().path(LdapResource.USER_MAPPING_PATH).parameter(ldapServer.getId()).with(unauthorizedUser())
        .body(ldapUserMapping).put();

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
    ldapRequest().path(LdapResource.PRIORITY_PATH).with(unauthorizedUser()).body(
        Collections.singletonList(ldapServer.getId())).put();

    assertAuditLog(AuditEvent.PRIORITIZE_LDAP, "unauthorized");
  }

  private void testUpsertUserMapping(
      final LdapGroupMappingType groupMappingType,
      final String expectedGroupMappingType,
      boolean enabledDisabled) throws Exception
  {
    LdapUserMapping ldapUserMapping = newUserMapping(groupMappingType, enabledDisabled);
    ldapRequest().path(LdapResource.USER_MAPPING_PATH).parameter(ldapServer.getId()).body(ldapUserMapping).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_LDAP_USER_MAPPING, null);
    assertLdapServerData(auditDTO, ldapServer);
    assertCustomData(auditDTO, "ldapUserBaseDn", ldapUserMapping.getUserBaseDN());
    assertCustomData(auditDTO, "ldapUserSubtree", enabledDisabled ? "enabled" : "disabled");
    assertCustomData(auditDTO, "ldapUserObjectClass", ldapUserMapping.getUserObjectClass());
    assertCustomData(auditDTO, "ldapUserFilter", ldapUserMapping.getUserFilter());
    assertCustomData(auditDTO, "ldapUserIdAttribute", ldapUserMapping.getUserIDAttribute());
    assertCustomData(auditDTO, "ldapUserRealNameAttribute", ldapUserMapping.getUserRealNameAttribute());
    assertCustomData(auditDTO, "ldapUserEmailAttribute", ldapUserMapping.getUserEmailAttribute());
    assertCustomData(auditDTO, "ldapUserPasswordAttribute", ldapUserMapping.getUserPasswordAttribute());
    assertCustomData(auditDTO, "ldapGroupType", expectedGroupMappingType);
    if (groupMappingType.equals(LdapGroupMappingType.STATIC)) {
      assertCustomData(auditDTO, "ldapStaticGroupBaseDn", ldapUserMapping.getGroupBaseDN());
      assertCustomData(auditDTO, "ldapStaticGroupSubtree", enabledDisabled ? "enabled" : "disabled");
      assertCustomData(auditDTO, "ldapStaticGroupObjectClass", ldapUserMapping.getGroupObjectClass());
      assertCustomData(auditDTO, "ldapStaticGroupIdAttribute", ldapUserMapping.getGroupIDAttribute());
      assertCustomData(auditDTO, "ldapStaticGroupMemberAttribute", ldapUserMapping.getGroupMemberAttribute());
      assertCustomData(auditDTO, "ldapStaticGroupMemberFormat", ldapUserMapping.getGroupMemberFormat());
    }
    if (groupMappingType.equals(LdapGroupMappingType.DYNAMIC)) {
      assertCustomData(auditDTO, "ldapDynamicGroupMemberOfAttribute", ldapUserMapping.getUserMemberOfGroupAttribute());
      assertCustomData(auditDTO, "ldapDynamicGroupSearch",
          enabledDisabled ? "enabled" : "disabled");
    }
  }

  private void testUpsertLdapConnection(
      final LdapAuthenticationMethod ldapAuthenticationMethod,
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

  private void assertLdapServerData(final AuditDTO auditDTO, final LdapServer ldapServer) {
    assertCustomData(auditDTO, "ldapServerId", ldapServer.getId());
    assertCustomData(auditDTO, "ldapServerName", ldapServer.getName());
  }

  private HttpRequest ldapRequest() {
    return restRequest().path(LdapResource.RESOURCE_PATH);
  }

  private void cleanUp(final LdapServer ldapServer) {
    ldapServerDAO.delete(ldapServer); //to avoid conflicts with other tests relies on order
  }

  private LdapConnection createLdapConnection(LdapAuthenticationMethod ldapAuthenticationMethod) {
    LdapConnection ldapConnection = new LdapConnection();
    ldapConnection.setServerId(ldapServer.getId());
    ldapConnection.setProtocol(LdapProtocol.LDAP);
    ldapConnection.setHostname("localhost");
    ldapConnection.setPort(389);
    ldapConnection.setAuthenticationMethod(ldapAuthenticationMethod);
    if (!ldapAuthenticationMethod.equals(LdapAuthenticationMethod.NONE)) {
      ldapConnection.setSaslRealm("sasl-realm");
      ldapConnection.setSystemUsername("system");
      ldapConnection.setSystemPassword("password".toCharArray());
    }
    ldapConnection.setConnectionTimeout(10);
    ldapConnection.setRetryDelay(20);
    return ldapConnection;
  }

  private LdapUserMapping newUserMapping(LdapGroupMappingType groupMappingType, boolean enabledDisabled) {
    LdapUserMapping ldapUserMapping = new LdapUserMapping();
    ldapUserMapping.setServerId(ldapServer.getId());
    ldapUserMapping.setUserBaseDN("userBaseDN");
    ldapUserMapping.setUserSubtree(enabledDisabled);
    ldapUserMapping.setUserObjectClass("userObjectClass");
    ldapUserMapping.setUserFilter("userFilter");
    ldapUserMapping.setUserIDAttribute("userIDAttribute");
    ldapUserMapping.setUserRealNameAttribute("realNameAttribute");
    ldapUserMapping.setUserEmailAttribute("emailAttribute");
    ldapUserMapping.setUserPasswordAttribute("passwordAttribute");
    ldapUserMapping.setGroupMappingType(groupMappingType);
    if (groupMappingType.equals(LdapGroupMappingType.STATIC)) {
      ldapUserMapping.setGroupBaseDN("groupBaseDN");
      ldapUserMapping.setGroupSubtree(enabledDisabled);
      ldapUserMapping.setGroupObjectClass("groupObjectClass");
      ldapUserMapping.setGroupIDAttribute("groupIDAttribute");
      ldapUserMapping.setGroupMemberAttribute("groupMemberAttribute");
      ldapUserMapping.setGroupMemberFormat("groupMemberFormat");
    }
    if (groupMappingType.equals(LdapGroupMappingType.DYNAMIC)) {
      ldapUserMapping.setUserMemberOfGroupAttribute("userMemberOfGroupAttribute");
      ldapUserMapping.setDynamicGroupSearchEnabled(enabledDisabled);
    }
    return ldapUserMapping;
  }
}
