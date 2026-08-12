/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiLegacyViolationStatusDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Authorization tests for {@code ApiLegacyViolationConfigResource}.
 * Verifies that READ permission is required for getConfig and WRITE permission for setConfig
 * on both APPLICATION and ORGANIZATION owners.
 */
@IqH2Test
class IqH2ApiLegacyViolationConfigResourceAuthzTest
{
  private IqTestContext ctx;

  private Organization org;

  private Application app;

  private User unauthorized;

  private User authorized;

  @BeforeEach
  void createEntities() {
    org = ctx.tempEntity().newOrganization();
    app = ctx.tempEntity().newApplication(org.getId());
    unauthorized = ctx.tempEntity().newUser();
    authorized = ctx.tempEntity().newUser();
  }

  private void grantPermission(String contextId, Permission permission) {
    Role role = ctx.tempEntity().newRole(false /* global */, permission);
    ctx.tempEntity().newMembershipMapping(contextId, role.getId(), authorized.getUsername());
  }

  private void grantReadPermission(String contextId) {
    grantPermission(contextId, Permission.READ);
  }

  private void grantWritePermission(String contextId) {
    grantPermission(contextId, Permission.WRITE);
  }

  @Test
  void testGetConfig_Application_Unauthenticated() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("application/{ownerId}")
        .parameter(app.getPublicId())
        .anon()
        .get();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  void testGetConfig_Application_Unauthorized() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("application/{ownerId}")
        .parameter(app.getPublicId())
        .auth(unauthorized)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  @Test
  void testGetConfig_Application_Authorized() throws Exception {
    grantReadPermission(app.getId());

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("application/{ownerId}")
        .parameter(app.getPublicId())
        .auth(authorized)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  void testGetConfig_Organization_Unauthenticated() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("organization/{ownerId}")
        .parameter(org.getId())
        .anon()
        .get();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  void testGetConfig_Organization_Unauthorized() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("organization/{ownerId}")
        .parameter(org.getId())
        .auth(unauthorized)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  @Test
  void testGetConfig_Organization_Authorized() throws Exception {
    grantReadPermission(org.getId());

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("organization/{ownerId}")
        .parameter(org.getId())
        .auth(authorized)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  void testSetConfig_Application_Unauthenticated() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("application/{ownerId}")
        .parameter(app.getPublicId())
        .anon()
        .body(newRequest(true))
        .put();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  void testSetConfig_Application_Unauthorized() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("application/{ownerId}")
        .parameter(app.getPublicId())
        .auth(unauthorized)
        .body(newRequest(true))
        .put();

    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  @Test
  void testSetConfig_Application_Authorized() throws Exception {
    grantWritePermission(app.getId());

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("application/{ownerId}")
        .parameter(app.getPublicId())
        .auth(authorized)
        .body(newRequest(true))
        .put();

    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  void testSetConfig_Organization_Unauthenticated() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("organization/{ownerId}")
        .parameter(org.getId())
        .anon()
        .body(newRequest(true))
        .put();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  void testSetConfig_Organization_Unauthorized() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("organization/{ownerId}")
        .parameter(org.getId())
        .auth(unauthorized)
        .body(newRequest(true))
        .put();

    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  @Test
  void testSetConfig_Organization_Authorized() throws Exception {
    grantWritePermission(org.getId());

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("organization/{ownerId}")
        .parameter(org.getId())
        .auth(authorized)
        .body(newRequest(true))
        .put();

    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  void testGetConfig_InvalidOwnerType_BadRequest() throws Exception {
    grantReadPermission(app.getId());

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("repository/{ownerId}")
        .parameter(app.getPublicId())
        .auth(authorized)
        .get();

    assertThat(response.getStatusCode()).isIn(400, 404);
  }

  private ApiLegacyViolationStatusDTO newRequest(boolean enabled) {
    ApiLegacyViolationStatusDTO dto = new ApiLegacyViolationStatusDTO();
    dto.enabled = enabled;
    return dto;
  }
}
