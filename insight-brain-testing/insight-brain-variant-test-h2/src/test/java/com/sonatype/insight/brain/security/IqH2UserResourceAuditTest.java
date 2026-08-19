/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.UserService.ChangePasswordDTO;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@IqH2Test
class IqH2UserResourceAuditTest
    implements AuditTestSupport
{
  // Injected by IqH2ServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private User unauthorizedUser;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
  }

  @AfterEach
  void after() {
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
    return ctx.restRequest().path(UserResource.RESOURCE_PATH);
  }

  @Test
  void testAddUser() throws Exception {
    User user = new User("john.doe", "secret", "John", "Doe", "john.doe@sonatype.com");
    restRequest().body(user).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_USER, null);
    assertUserData(auditDTO, user);
  }

  @Test
  void testAddUser_Unauthorized() throws Exception {
    User user = new User("john.doe", "secret", "John", "Doe", "john.doe@sonatype.com");
    restRequest().with(unauthorizedUser()).body(user).post();

    assertAuditLog(AuditEvent.CREATE_USER, "unauthorized");
  }

  @Test
  void testUpdateUser() throws Exception {
    User user = ctx.tempEntity().newUser("jane.doe", "Jane", "Doe", "jane.doe@sonatype.com");
    user.setPassword(UserService.FAKE_PASSWORD);
    restRequest().body(user).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_USER, null);
    assertUserData(auditDTO, user);
  }

  @Test
  void testUpdateUser_Unauthorized() throws Exception {
    User user = ctx.tempEntity().newUser("jane.doe", "Jane", "Doe", "jane.doe@sonatype.com");
    restRequest().with(unauthorizedUser()).body(user).put();

    assertAuditLog(AuditEvent.UPDATE_USER, "unauthorized");
  }

  @Test
  void testDeleteUser() throws Exception {
    User user = ctx.tempEntity().newUser("jane.doe", "Jane", "Doe", "jane.doe@sonatype.com");
    restRequest().path(user.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_USER, null);
    assertUserData(auditDTO, User.INTERNAL_REALM_ID, user);
  }

  @Test
  void testDeleteUser_Unauthorized() throws Exception {
    User user = ctx.tempEntity().newUser("jane.doe", "Jane", "Doe", "jane.doe@sonatype.com");
    restRequest().path(user.getId()).with(unauthorizedUser()).delete();

    assertAuditLog(AuditEvent.DELETE_USER, "unauthorized");
  }

  @Test
  void testChangeMyPassword() throws Exception {
    User user = ctx.tempEntity().newUser("john.smith", "John", "Smith", "john.smith@sonatype.com");
    ChangePasswordDTO passwordDTO = new ChangePasswordDTO();
    passwordDTO.oldPassword = TemporaryEntity.USER_PASSWORD_CLEAR;
    passwordDTO.newPassword = "still-secret";
    restRequest().auth(user).path(UserResource.MY_PASSWORD_PATH).body(passwordDTO).put();

    assertAuditLog(AuditEvent.UPDATE_USER_PASSWORD, null, user.getUsername());
  }

  @Test
  void testResetPassword() throws Exception {
    User user = ctx.tempEntity().newUser("john.smith", "John", "Smith", "john.smith@sonatype.com");
    restRequest().path(UserResource.RESET_PASSWORD_PATH).parameter(user.getId()).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.RESET_USER_PASSWORD, null);
    assertCustomData(auditDTO, "username", user.getUsername());
  }

  @Test
  void testResetPassword_Unauthorized() throws Exception {
    User user = ctx.tempEntity().newUser("john.smith", "John", "Smith", "john.smith@sonatype.com");
    restRequest().with(unauthorizedUser()).path(UserResource.RESET_PASSWORD_PATH).parameter(user.getId()).put();

    assertAuditLog(AuditEvent.RESET_USER_PASSWORD, "unauthorized");
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
