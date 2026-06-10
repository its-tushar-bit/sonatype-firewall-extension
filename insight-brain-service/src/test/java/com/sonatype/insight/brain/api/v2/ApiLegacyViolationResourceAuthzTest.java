/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Authorization tests for {@link ApiLegacyViolationResource}.
 * Verifies that READ permission is required for listing legacy violations and
 * WRITE permission is required for grant/revoke operations on the application context.
 */
public class ApiLegacyViolationResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testList_Unauthenticated() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
        .path(ApiLegacyViolationResource.APPLICATION_PATH)
        .parameter(app.getPublicId())
        .anon()
        .get();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testList_Unauthorized() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
        .path(ApiLegacyViolationResource.APPLICATION_PATH)
        .parameter(app.getPublicId())
        .auth(unauthorized)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  @Test
  public void testList_Authorized() throws Exception {
    grantReadPermission(app.getId());

    HttpResponse response = restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
        .path(ApiLegacyViolationResource.APPLICATION_PATH)
        .parameter(app.getPublicId())
        .auth(authorized)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  public void testGrant_Unauthenticated() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
        .path(ApiLegacyViolationResource.GRANT_PATH)
        .parameter(app.getPublicId())
        .anon()
        .post();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testGrant_Unauthorized() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
        .path(ApiLegacyViolationResource.GRANT_PATH)
        .parameter(app.getPublicId())
        .auth(unauthorized)
        .post();

    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  @Test
  public void testGrant_Authorized() throws Exception {
    grantWritePermission(app.getId());

    HttpResponse response = restRequest()
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
  public void testRevoke_Unauthenticated() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
        .path(ApiLegacyViolationResource.REVOKE_PATH)
        .parameter(app.getPublicId())
        .anon()
        .post();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testRevoke_Unauthorized() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
        .path(ApiLegacyViolationResource.REVOKE_PATH)
        .parameter(app.getPublicId())
        .auth(unauthorized)
        .post();

    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  @Test
  public void testRevoke_Authorized() throws Exception {
    grantWritePermission(app.getId());

    HttpResponse response = restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
        .path(ApiLegacyViolationResource.REVOKE_PATH)
        .parameter(app.getPublicId())
        .auth(authorized)
        .post();

    assertThat(response.getStatusCode()).isEqualTo(200);
  }
}
