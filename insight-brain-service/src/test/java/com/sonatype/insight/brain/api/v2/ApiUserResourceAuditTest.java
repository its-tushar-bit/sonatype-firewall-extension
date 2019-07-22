/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiUserDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.ApiUserTestSupport.createUserDTOToAdd;
import static com.sonatype.insight.brain.api.v2.ApiUserTestSupport.createUserDTOToUpdate;

public class ApiUserResourceAuditTest
    extends AbstractAuditTest
{
  private UserDAO userDAO = new UserDAO();

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.USER_RESOURCE_PATH_V2);
  }

  @Test
  public void testAdd() throws Exception {
    ApiUserDTO inputUserDTO = createUserDTOToAdd();
    tempEntity.registerUsernames(inputUserDTO.username);

    restRequest().body(inputUserDTO).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_USER, null);
    assertUserData(auditDTO, userDAO.getByUsernameNotNull(inputUserDTO.username));
  }

  @Test
  public void testAdd_Unauthorized() throws Exception {
    ApiUserDTO inputUserDTO = createUserDTOToAdd();
    tempEntity.registerUsernames(inputUserDTO.username);
    
    restRequest().with(unauthorizedUser()).body(inputUserDTO).post();

    assertAuditLog(AuditEvent.CREATE_USER, "unauthorized");
  }

  @Test
  public void testUpdate() throws Exception {
    ApiUserDTO inputUserDTO = createUserDTOToUpdate(tempEntity.newUser());

    restRequest().path(ApiUserResource.USERNAME_PATH).parameter(inputUserDTO.username).body(inputUserDTO).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_USER, null);
    assertUserData(auditDTO, userDAO.getByUsernameNotNull(inputUserDTO.username));
  }

  @Test
  public void testUpdate_Unauthorized() throws Exception {
    ApiUserDTO inputUserDTO = createUserDTOToUpdate(tempEntity.newUser());
    restRequest().path(ApiUserResource.USERNAME_PATH).parameter(inputUserDTO.username).with(unauthorizedUser())
        .body(inputUserDTO).put();

    assertAuditLog(AuditEvent.UPDATE_USER, "unauthorized");
  }

  @Test
  public void testDelete() throws Exception {
    User user = tempEntity.newUser();

    restRequest().path(ApiUserResource.USERNAME_PATH).parameter(user.getUsername()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_USER, null);
    assertUserData(auditDTO, user);
  }

  @Test
  public void testDelete_Unauthorized() throws Exception {
    restRequest().with(unauthorizedUser()).path(ApiUserResource.USERNAME_PATH)
        .parameter(tempEntity.newUser().getUsername()).delete();

    assertAuditLog(AuditEvent.DELETE_USER, "unauthorized");
  }
}
