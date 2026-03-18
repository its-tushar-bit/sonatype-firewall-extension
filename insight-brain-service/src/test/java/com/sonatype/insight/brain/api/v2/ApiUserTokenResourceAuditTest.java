/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiUserTokenDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.configuration.ldap.TestLdapServer;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiUserTokenResourceAuditTest
    extends AbstractAuditTest
{
  @Rule
  public TestLdapServer embeddedTestLdapServer = new TestLdapServer();

  @Test
  public void testCreateUserToken() throws Exception {
    String username = "FearlessTuring";

    tempEntity.newUser(username);

    HttpResponse response = restRequest()
        .path(ApiUserTokenResource.CURRENT_USER)
        .auth(username, TemporaryEntity.USER_PASSWORD_CLEAR)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_USER_TOKEN, null, username);
    assertThat(auditDTO.data).containsEntry("username", username);
    assertThat(auditDTO.data).containsEntry("userCode", response.getBody(ApiUserTokenDTO.class).userCode);
  }

  @Test
  public void testCreateUserToken_TokenExists() throws Exception {
    String username = "FearlessTuring";

    tempEntity.newUser(username);
    tempEntity.newUserToken(username, InternalRealm.ID);

    restRequest()
        .path(ApiUserTokenResource.CURRENT_USER)
        .auth(username, TemporaryEntity.USER_PASSWORD_CLEAR)
        .post();

    assertAuditLog(AuditEvent.CREATE_USER_TOKEN, "bad-request", username);
  }

  @Test
  public void testDeleteCurrentUserToken() throws Exception {
    String username = "FearlessTuring";

    tempEntity.newUser(username);
    UserToken userToken = tempEntity.newUserToken(username, InternalRealm.ID);

    restRequest()
        .path(ApiUserTokenResource.CURRENT_USER)
        .auth(username, TemporaryEntity.USER_PASSWORD_CLEAR)
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_USER_TOKEN, null, username);
    assertThat(auditDTO.data).containsEntry("username", username);
    assertThat(auditDTO.data).containsEntry("userCode", userToken.getUserCode());
  }

  @Test
  public void testDeleteCurrentUserToken_TokenDoesNotExist() throws Exception {
    String username = "FearlessTuring";

    tempEntity.newUser(username);

    restRequest()
        .path(ApiUserTokenResource.CURRENT_USER)
        .auth(username, TemporaryEntity.USER_PASSWORD_CLEAR)
        .delete();

    assertAuditLog(AuditEvent.DELETE_USER_TOKEN, "not-found", username);
  }

  @Test
  public void testPurgeUserTokens() throws Exception {
    embeddedTestLdapServer.start();
    embeddedTestLdapServer.loadData("/" + getClass().getSimpleName() + "/ldap_users.ldif");

    LdapServer ldapServer = tempEntity.newLdapServer("test");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedTestLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    // Token for non-existing LDAP user, should be purged.
    UserToken userTokenLdapUseInvalid = tempEntity.newUserToken("no-such-user", ldapServer.getId());

    restRequest() //
        .path(ApiUserTokenResource.PURGE) //
        .delete();

    assertAuditLog(AuditEvent.PURGE_USER_TOKENS, null, User.ADMIN_USERNAME);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_USER_TOKEN, null, User.ADMIN_USERNAME);
    assertThat(auditDTO.data).containsEntry("username", userTokenLdapUseInvalid.getUsername());
    assertThat(auditDTO.data).containsEntry("userCode", userTokenLdapUseInvalid.getUserCode());
  }

  @Test
  public void testPurgeUserTokens_Unauthorized() throws Exception {
    restRequest() //
        .path(ApiUserTokenResource.PURGE) //
        .with(unauthorizedUser())
        .delete();

    assertAuditLog(AuditEvent.PURGE_USER_TOKENS, "unauthorized");
  }

  @Test
  public void testDeleteUserTokenByUserCode() throws Exception {
    String username = "FearlessTuring";

    tempEntity.newUser(username);
    UserToken userToken = tempEntity.newUserToken(username, InternalRealm.ID);

    restRequest()
        .path(ApiUserTokenResource.USER_CODE)
        .parameter(userToken.getUserCode())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_USER_TOKEN, null, "admin");
    assertThat(auditDTO.data).containsEntry("username", username);
    assertThat(auditDTO.data).containsEntry("userCode", userToken.getUserCode());
  }

  @Test
  public void testDeleteUserTokenByUserCode_TokenDoesNotExist() throws Exception {
    restRequest()
        .path(ApiUserTokenResource.USER_CODE)
        .parameter("void-code")
        .delete();

    assertAuditLog(AuditEvent.DELETE_USER_TOKEN, "not-found", "admin");
  }

  @Test
  public void testDeleteUserTokenByUserCode_Unauthorized() throws Exception {
    String username = "FearlessTuring";

    tempEntity.newUser(username);
    UserToken userToken = tempEntity.newUserToken(username, InternalRealm.ID);

    restRequest()
        .path(ApiUserTokenResource.USER_CODE)
        .parameter(userToken.getUserCode())
        .auth(username, TemporaryEntity.USER_PASSWORD_CLEAR)
        .delete();

    assertAuditLog(AuditEvent.DELETE_USER_TOKEN, "unauthorized", username);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.USER_TOKEN_RESOURCE_PATH_V2);
  }
}
