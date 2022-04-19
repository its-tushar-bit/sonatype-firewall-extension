/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.UserService.ChangePasswordDTO;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

public class UserResourceAuditTest
    extends AbstractAuditTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(UserResource.RESOURCE_PATH);
  }

  @Test
  public void testAddUser() throws Exception {
    User user = new User("john.doe", "secret", "John", "Doe", "john.doe@sonatype.com");
    tempEntity.register(restRequest().body(user).post().getBody(User.class));

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_USER, null);
    assertUserData(auditDTO, user);
  }

  @Test
  public void testAddUser_Unauthorized() throws Exception {
    User user = new User("john.doe", "secret", "John", "Doe", "john.doe@sonatype.com");
    restRequest().with(unauthorizedUser()).body(user).post();

    assertAuditLog(AuditEvent.CREATE_USER, "unauthorized");
  }

  @Test
  public void testUpdateUser() throws Exception {
    User user = tempEntity.newUser("jane.doe", "Jane", "Doe", "jane.doe@sonatype.com");
    user.setPassword(UserService.FAKE_PASSWORD);
    restRequest().body(user).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_USER, null);
    assertUserData(auditDTO, user);
  }

  @Test
  public void testUpdateUser_Unauthorized() throws Exception {
    User user = tempEntity.newUser("jane.doe", "Jane", "Doe", "jane.doe@sonatype.com");
    restRequest().with(unauthorizedUser()).body(user).put();

    assertAuditLog(AuditEvent.UPDATE_USER, "unauthorized");
  }

  @Test
  public void testDeleteUser() throws Exception {
    User user = tempEntity.newUser("jane.doe", "Jane", "Doe", "jane.doe@sonatype.com");
    restRequest().path(user.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_USER, null);
    assertUserData(auditDTO, User.INTERNAL_REALM_ID, user);
  }

  @Test
  public void testDeleteUser_Unauthorized() throws Exception {
    User user = tempEntity.newUser("jane.doe", "Jane", "Doe", "jane.doe@sonatype.com");
    restRequest().path(user.getId()).with(unauthorizedUser()).delete();

    assertAuditLog(AuditEvent.DELETE_USER, "unauthorized");
  }

  @Test
  public void testChangeMyPassword() throws Exception {
    User user = tempEntity.newUser("john.smith", "John", "Smith", "john.smith@sonatype.com");
    ChangePasswordDTO passwordDTO = new ChangePasswordDTO();
    passwordDTO.oldPassword = TemporaryEntity.USER_PASSWORD_CLEAR;
    passwordDTO.newPassword = "still-secret";
    restRequest().auth(user).path(UserResource.MY_PASSWORD_PATH).body(passwordDTO).put();

    assertAuditLog(AuditEvent.UPDATE_USER_PASSWORD, null, user.getUsername());
  }

  @Test
  public void testResetPassword() throws Exception {
    User user = tempEntity.newUser("john.smith", "John", "Smith", "john.smith@sonatype.com");
    restRequest().path(UserResource.RESET_PASSWORD_PATH).parameter(user.getId()).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.RESET_USER_PASSWORD, null);
    assertCustomData(auditDTO, "username", user.getUsername());
  }

  @Test
  public void testResetPassword_Unauthorized() throws Exception {
    User user = tempEntity.newUser("john.smith", "John", "Smith", "john.smith@sonatype.com");
    restRequest().with(unauthorizedUser()).path(UserResource.RESET_PASSWORD_PATH).parameter(user.getId()).put();

    assertAuditLog(AuditEvent.RESET_USER_PASSWORD, "unauthorized");
  }
}
