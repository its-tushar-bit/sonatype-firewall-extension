/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiUserTokenResource;
import com.sonatype.insight.brain.api.v2.dto.ApiUserTokenDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiUserTokenResourceAuditTest
    extends AbstractAuditTest
{
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
    tempEntity.newUserToken(username, true);

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
    UserToken userToken = tempEntity.newUserToken(username, true);

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
  public void testDeleteUserToken() throws Exception {
    String username = "FearlessTuring";

    tempEntity.newUser(username);
    UserToken userToken = tempEntity.newUserToken(username, true);

    restRequest()
        .path(ApiUserTokenResource.DELETE_BY_USERNAME).parameter(username)
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_USER_TOKEN, null, User.ADMIN_USERNAME);
    assertThat(auditDTO.data).containsEntry("username", username);
    assertThat(auditDTO.data).containsEntry("userCode", userToken.getUserCode());
  }

  @Test
  public void testDeleteUserToken_Unauthorized() throws Exception {
    restRequest()
        .path(ApiUserTokenResource.DELETE_BY_USERNAME)
        .parameter("john.doe")
        .with(unauthorizedUser())
        .delete();

    assertAuditLog(AuditEvent.DELETE_USER_TOKEN, "unauthorized");
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.USER_TOKEN_RESOURCE_PATH_V2);
  }
}
