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
import com.sonatype.insight.brain.configuration.ldap.EmbeddedLdapServerExtension;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kept in the original package/simple name because {@link #testPurgeUserTokens} resolves the LDAP fixture via
 * {@code getClass().getSimpleName()}.
 */
@IqH2Test
class ApiUserTokenResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  @RegisterExtension
  private final EmbeddedLdapServerExtension embeddedTestLdapServer = new EmbeddedLdapServerExtension();

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  private User unauthorizedUser;

  @BeforeEach
  void setUp() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
  }

  @AfterEach
  void tearDown() throws Exception {
    logOutput.tearDown();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public String getUnauthorizedUsername() {
    return unauthorizedUser.getUsername();
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private java.util.function.Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.USER_TOKEN_RESOURCE_PATH_V2);
  }

  @Test
  void testCreateUserToken() throws Exception {
    String username = "FearlessTuring";

    ctx.tempEntity().newUser(username);

    HttpResponse response = restRequest()
        .path(ApiUserTokenResource.CURRENT_USER)
        .auth(username, TemporaryEntity.USER_PASSWORD_CLEAR)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_USER_TOKEN, null, username);
    assertThat(auditDTO.data).containsEntry("username", username);
    assertThat(auditDTO.data).containsEntry("userCode", response.getBody(ApiUserTokenDTO.class).userCode);
  }

  @Test
  void testCreateUserToken_TokenExists() throws Exception {
    String username = "FearlessTuring";

    ctx.tempEntity().newUser(username);
    ctx.tempEntity().newUserToken(username, InternalRealm.ID);

    restRequest()
        .path(ApiUserTokenResource.CURRENT_USER)
        .auth(username, TemporaryEntity.USER_PASSWORD_CLEAR)
        .post();

    assertAuditLog(AuditEvent.CREATE_USER_TOKEN, "bad-request", username);
  }

  @Test
  void testDeleteCurrentUserToken() throws Exception {
    String username = "FearlessTuring";

    ctx.tempEntity().newUser(username);
    UserToken userToken = ctx.tempEntity().newUserToken(username, InternalRealm.ID);

    restRequest()
        .path(ApiUserTokenResource.CURRENT_USER)
        .auth(username, TemporaryEntity.USER_PASSWORD_CLEAR)
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_USER_TOKEN, null, username);
    assertThat(auditDTO.data).containsEntry("username", username);
    assertThat(auditDTO.data).containsEntry("userCode", userToken.getUserCode());
  }

  @Test
  void testDeleteCurrentUserToken_TokenDoesNotExist() throws Exception {
    String username = "FearlessTuring";

    ctx.tempEntity().newUser(username);

    restRequest()
        .path(ApiUserTokenResource.CURRENT_USER)
        .auth(username, TemporaryEntity.USER_PASSWORD_CLEAR)
        .delete();

    assertAuditLog(AuditEvent.DELETE_USER_TOKEN, "not-found", username);
  }

  @Test
  void testPurgeUserTokens() throws Exception {
    embeddedTestLdapServer.start();
    embeddedTestLdapServer.loadData("/" + getClass().getSimpleName() + "/ldap_users.ldif");

    LdapServer ldapServer = ctx.tempEntity().newLdapServer("test");
    ctx.tempEntity().newLdapConnection(ldapServer.getId(), embeddedTestLdapServer.getPort());
    ctx.tempEntity().newLdapUserMapping(ldapServer.getId());

    // Token for non-existing LDAP user, should be purged.
    UserToken userTokenLdapUseInvalid = ctx.tempEntity().newUserToken("no-such-user", ldapServer.getId());

    restRequest() //
        .path(ApiUserTokenResource.PURGE) //
        .delete();

    assertAuditLog(AuditEvent.PURGE_USER_TOKENS, null, User.ADMIN_USERNAME);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_USER_TOKEN, null, User.ADMIN_USERNAME);
    assertThat(auditDTO.data).containsEntry("username", userTokenLdapUseInvalid.getUsername());
    assertThat(auditDTO.data).containsEntry("userCode", userTokenLdapUseInvalid.getUserCode());
  }

  @Test
  void testPurgeUserTokens_Unauthorized() throws Exception {
    restRequest() //
        .path(ApiUserTokenResource.PURGE) //
        .with(unauthorizedUser())
        .delete();

    assertAuditLog(AuditEvent.PURGE_USER_TOKENS, "unauthorized");
  }

  @Test
  void testDeleteUserTokenByUserCode() throws Exception {
    String username = "FearlessTuring";

    ctx.tempEntity().newUser(username);
    UserToken userToken = ctx.tempEntity().newUserToken(username, InternalRealm.ID);

    restRequest()
        .path(ApiUserTokenResource.USER_CODE)
        .parameter(userToken.getUserCode())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_USER_TOKEN, null, "admin");
    assertThat(auditDTO.data).containsEntry("username", username);
    assertThat(auditDTO.data).containsEntry("userCode", userToken.getUserCode());
  }

  @Test
  void testDeleteUserTokenByUserCode_TokenDoesNotExist() throws Exception {
    restRequest()
        .path(ApiUserTokenResource.USER_CODE)
        .parameter("void-code")
        .delete();

    assertAuditLog(AuditEvent.DELETE_USER_TOKEN, "not-found", "admin");
  }

  @Test
  void testDeleteUserTokenByUserCode_Unauthorized() throws Exception {
    String username = "FearlessTuring";

    ctx.tempEntity().newUser(username);
    UserToken userToken = ctx.tempEntity().newUserToken(username, InternalRealm.ID);

    restRequest()
        .path(ApiUserTokenResource.USER_CODE)
        .parameter(userToken.getUserCode())
        .auth(username, TemporaryEntity.USER_PASSWORD_CLEAR)
        .delete();

    assertAuditLog(AuditEvent.DELETE_USER_TOKEN, "unauthorized", username);
  }

  private static final class TestLogOutput
      extends LogOutput
  {
    TestLogOutput(String... loggerNames) {
      super(loggerNames);
    }

    void tearDown() {
      after();
    }
  }
}
