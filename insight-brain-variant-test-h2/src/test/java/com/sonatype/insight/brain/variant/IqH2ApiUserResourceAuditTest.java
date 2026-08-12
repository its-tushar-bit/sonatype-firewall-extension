/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiUserResource;
import com.sonatype.insight.brain.api.v2.dto.ApiUserDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.OAuth2User;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.SamlRealm;
import com.sonatype.insight.brain.security.SsoUserService;
import com.sonatype.insight.brain.security.oauth2.OAuth2Realm;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.api.v2.ApiUserTestSupport.createUserDTOToAdd;
import static com.sonatype.insight.brain.api.v2.ApiUserTestSupport.createUserDTOToUpdate;

@IqH2Test
class IqH2ApiUserResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private UserDAO userDAO;

  private User unauthorizedUser;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
    userDAO = ctx.lookup(UserDAO.class);
  }

  @AfterEach
  void after() {
    logOutput.tearDown();
    // enableSsoWithOAuth2() below flips a process-wide static feature flag on the reused server; reset it so it
    // does not leak into sibling tests/classes.
    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(false);
    ctx.lookup(SsoUserService.class).loadSsoConfiguration();
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

  private void enableSsoWithOAuth2() {
    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(true);
    ctx.tempEntity().newOAuth2Configuration();
    ctx.lookup(SsoUserService.class).loadSsoConfiguration();
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.USER_RESOURCE_PATH_V2);
  }

  @Test
  void testAdd() throws Exception {
    ApiUserDTO inputUserDTO = createUserDTOToAdd();

    restRequest().body(inputUserDTO).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_USER, null);
    assertUserData(auditDTO, userDAO.getByUsernameNotNull(inputUserDTO.username));
  }

  @Test
  void testAdd_Unauthorized() throws Exception {
    ApiUserDTO inputUserDTO = createUserDTOToAdd();

    restRequest().with(unauthorizedUser()).body(inputUserDTO).post();

    assertAuditLog(AuditEvent.CREATE_USER, "unauthorized");
  }

  @Test
  void testUpdate() throws Exception {
    ApiUserDTO inputUserDTO = createUserDTOToUpdate(ctx.tempEntity().newUser());

    restRequest().path(ApiUserResource.USERNAME_PATH).parameter(inputUserDTO.username).body(inputUserDTO).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_USER, null);
    assertUserData(auditDTO, userDAO.getByUsernameNotNull(inputUserDTO.username));
  }

  @Test
  void testUpdate_Unauthorized() throws Exception {
    ApiUserDTO inputUserDTO = createUserDTOToUpdate(ctx.tempEntity().newUser());
    restRequest().path(ApiUserResource.USERNAME_PATH)
        .parameter(inputUserDTO.username)
        .with(unauthorizedUser())
        .body(inputUserDTO)
        .put();

    assertAuditLog(AuditEvent.UPDATE_USER, "unauthorized");
  }

  @Test
  void testDelete_InternalUser() throws Exception {
    User user = ctx.tempEntity().newUser();

    restRequest().path(ApiUserResource.USERNAME_PATH).parameter(user.getUsername()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_USER, null);
    assertUserData(auditDTO, User.INTERNAL_REALM_ID, user);
  }

  @Test
  void testDelete_SamlUser() throws Exception {
    SamlUser samlUser = ctx.tempEntity().newSamlUser();

    restRequest().path(ApiUserResource.USERNAME_PATH)
        .parameter(samlUser.getUsername())
        .query("realm", SamlRealm.ID)
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_USER, null);
    assertUserData(auditDTO, SamlRealm.ID, samlUser);
  }

  @Test
  void testDelete_OAuthUser() throws Exception {
    enableSsoWithOAuth2();
    OAuth2User oAuth2User = ctx.tempEntity().newOAuth2User();

    restRequest().path(ApiUserResource.USERNAME_PATH)
        .parameter(oAuth2User.getUsername())
        .query("realm", OAuth2Realm.ID)
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_USER, null);
    assertUserData(auditDTO, OAuth2Realm.ID, oAuth2User);
  }

  @Test
  void testDelete_Unauthorized() throws Exception {
    restRequest().with(unauthorizedUser())
        .path(ApiUserResource.USERNAME_PATH)
        .parameter(ctx.tempEntity().newUser().getUsername())
        .delete();

    assertAuditLog(AuditEvent.DELETE_USER, "unauthorized");
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
