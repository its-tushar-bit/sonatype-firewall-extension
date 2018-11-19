/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

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
      assertThat(persistedServer, is(notNullValue()));
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
}
