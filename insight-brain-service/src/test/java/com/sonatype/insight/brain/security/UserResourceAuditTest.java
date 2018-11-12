/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

public class UserResourceAuditTest
    extends AbstractAuditTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(UserResource.RESOURCE_PATH);
  }

  private void assertUserData(AuditDTO auditDTO, User user) {
    assertCustomData(auditDTO, "username", user.getUsername());
    assertCustomData(auditDTO, "firstName", user.getFirstName());
    assertCustomData(auditDTO, "lastName", user.getLastName());
    assertCustomData(auditDTO, "emailAddress", user.getEmail());
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
    assertUserData(auditDTO, user);
  }

  @Test
  public void testDeleteUser_Unauthorized() throws Exception {
    User user = tempEntity.newUser("jane.doe", "Jane", "Doe", "jane.doe@sonatype.com");
    restRequest().path(user.getId()).with(unauthorizedUser()).delete();

    assertAuditLog(AuditEvent.DELETE_USER, "unauthorized");
  }
}
