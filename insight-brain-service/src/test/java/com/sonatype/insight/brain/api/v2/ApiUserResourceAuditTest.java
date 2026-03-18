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
import com.sonatype.insight.brain.model.security.OAuth2User;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.SamlRealm;
import com.sonatype.insight.brain.security.oauth2.OAuth2Realm;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.ApiUserTestSupport.createUserDTOToAdd;
import static com.sonatype.insight.brain.api.v2.ApiUserTestSupport.createUserDTOToUpdate;

public class ApiUserResourceAuditTest
    extends AbstractAuditTest
{
  private UserDAO userDAO;

  @Before
  public void before() {
    userDAO = lookup(UserDAO.class);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.USER_RESOURCE_PATH_V2);
  }

  @Test
  public void testAdd() throws Exception {
    ApiUserDTO inputUserDTO = createUserDTOToAdd();

    restRequest().body(inputUserDTO).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_USER, null);
    assertUserData(auditDTO, userDAO.getByUsernameNotNull(inputUserDTO.username));
  }

  @Test
  public void testAdd_Unauthorized() throws Exception {
    ApiUserDTO inputUserDTO = createUserDTOToAdd();

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
    restRequest().path(ApiUserResource.USERNAME_PATH)
        .parameter(inputUserDTO.username)
        .with(unauthorizedUser())
        .body(inputUserDTO)
        .put();

    assertAuditLog(AuditEvent.UPDATE_USER, "unauthorized");
  }

  @Test
  public void testDelete_InternalUser() throws Exception {
    User user = tempEntity.newUser();

    restRequest().path(ApiUserResource.USERNAME_PATH).parameter(user.getUsername()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_USER, null);
    assertUserData(auditDTO, User.INTERNAL_REALM_ID, user);
  }

  @Test
  public void testDelete_SamlUser() throws Exception {
    SamlUser samlUser = tempEntity.newSamlUser();

    restRequest().path(ApiUserResource.USERNAME_PATH)
        .parameter(samlUser.getUsername())
        .query("realm", SamlRealm.ID)
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_USER, null);
    assertUserData(auditDTO, SamlRealm.ID, samlUser);
  }

  @Test
  public void testDelete_OAuthUser() throws Exception {
    enableSsoWithOAuth2();
    OAuth2User oAuth2User = tempEntity.newOAuth2User();

    restRequest().path(ApiUserResource.USERNAME_PATH)
        .parameter(oAuth2User.getUsername())
        .query("realm", OAuth2Realm.ID)
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_USER, null);
    assertUserData(auditDTO, OAuth2Realm.ID, oAuth2User);
  }

  @Test
  public void testDelete_Unauthorized() throws Exception {
    restRequest().with(unauthorizedUser())
        .path(ApiUserResource.USERNAME_PATH)
        .parameter(tempEntity.newUser().getUsername())
        .delete();

    assertAuditLog(AuditEvent.DELETE_USER, "unauthorized");
  }
}
