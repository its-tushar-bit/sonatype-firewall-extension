/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiLegacyViolationResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Authorization tests for {@code ApiLegacyViolationResource}.
 * Verifies that READ permission is required for listing legacy violations and
 * WRITE permission is required for grant/revoke operations on the application context.
 */
@IqH2Test
class IqH2ApiLegacyViolationResourceAuthzTest
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
  void testList_Unauthenticated() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
        .path(ApiLegacyViolationResource.APPLICATION_PATH)
        .parameter(app.getPublicId())
        .anon()
        .get();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  void testList_Unauthorized() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
        .path(ApiLegacyViolationResource.APPLICATION_PATH)
        .parameter(app.getPublicId())
        .auth(unauthorized)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  @Test
  void testList_Authorized() throws Exception {
    grantReadPermission(app.getId());

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
        .path(ApiLegacyViolationResource.APPLICATION_PATH)
        .parameter(app.getPublicId())
        .auth(authorized)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  void testGrant_Unauthenticated() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
        .path(ApiLegacyViolationResource.GRANT_PATH)
        .parameter(app.getPublicId())
        .anon()
        .post();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  void testGrant_Unauthorized() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
        .path(ApiLegacyViolationResource.GRANT_PATH)
        .parameter(app.getPublicId())
        .auth(unauthorized)
        .post();

    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  @Test
  void testGrant_Authorized() throws Exception {
    grantWritePermission(app.getId());

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
        .path(ApiLegacyViolationResource.GRANT_PATH)
        .parameter(app.getPublicId())
        .auth(authorized)
        .post();

    // 200 = success; 400 = BadRequest (legacy violations not enabled on app — still proves authz passed).
    // 401/403 would mean the request was rejected before reaching the service layer.
    assertThat(response.getStatusCode()).isNotIn(401, 403);
    assertThat(response.getStatusCode()).isLessThan(500);
  }

  @Test
  void testRevoke_Unauthenticated() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
        .path(ApiLegacyViolationResource.REVOKE_PATH)
        .parameter(app.getPublicId())
        .anon()
        .post();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  void testRevoke_Unauthorized() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
        .path(ApiLegacyViolationResource.REVOKE_PATH)
        .parameter(app.getPublicId())
        .auth(unauthorized)
        .post();

    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  @Test
  void testRevoke_Authorized() throws Exception {
    grantWritePermission(app.getId());

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
        .path(ApiLegacyViolationResource.REVOKE_PATH)
        .parameter(app.getPublicId())
        .auth(authorized)
        .post();

    assertThat(response.getStatusCode()).isEqualTo(200);
  }
}
