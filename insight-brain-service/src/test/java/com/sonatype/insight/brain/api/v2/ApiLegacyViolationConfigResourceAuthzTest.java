/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiLegacyViolationStatusDTO;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Authorization tests for {@link ApiLegacyViolationConfigResource}.
 * Verifies that READ permission is required for getConfig and WRITE permission for setConfig
 * on both APPLICATION and ORGANIZATION owners.
 */
public class ApiLegacyViolationConfigResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testGetConfig_Application_Unauthenticated() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("application/{ownerId}")
        .parameter(app.getPublicId())
        .anon()
        .get();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testGetConfig_Application_Unauthorized() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("application/{ownerId}")
        .parameter(app.getPublicId())
        .auth(unauthorized)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  @Test
  public void testGetConfig_Application_Authorized() throws Exception {
    grantReadPermission(app.getId());

    HttpResponse response = restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("application/{ownerId}")
        .parameter(app.getPublicId())
        .auth(authorized)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  public void testGetConfig_Organization_Unauthenticated() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("organization/{ownerId}")
        .parameter(org.getId())
        .anon()
        .get();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testGetConfig_Organization_Unauthorized() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("organization/{ownerId}")
        .parameter(org.getId())
        .auth(unauthorized)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  @Test
  public void testGetConfig_Organization_Authorized() throws Exception {
    grantReadPermission(org.getId());

    HttpResponse response = restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("organization/{ownerId}")
        .parameter(org.getId())
        .auth(authorized)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  public void testSetConfig_Application_Unauthenticated() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("application/{ownerId}")
        .parameter(app.getPublicId())
        .anon()
        .body(newRequest(true))
        .put();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testSetConfig_Application_Unauthorized() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("application/{ownerId}")
        .parameter(app.getPublicId())
        .auth(unauthorized)
        .body(newRequest(true))
        .put();

    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  @Test
  public void testSetConfig_Application_Authorized() throws Exception {
    grantWritePermission(app.getId());

    HttpResponse response = restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("application/{ownerId}")
        .parameter(app.getPublicId())
        .auth(authorized)
        .body(newRequest(true))
        .put();

    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  public void testSetConfig_Organization_Unauthenticated() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("organization/{ownerId}")
        .parameter(org.getId())
        .anon()
        .body(newRequest(true))
        .put();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testSetConfig_Organization_Unauthorized() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("organization/{ownerId}")
        .parameter(org.getId())
        .auth(unauthorized)
        .body(newRequest(true))
        .put();

    assertThat(response.getStatusCode()).isEqualTo(403);
  }

  @Test
  public void testSetConfig_Organization_Authorized() throws Exception {
    grantWritePermission(org.getId());

    HttpResponse response = restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("organization/{ownerId}")
        .parameter(org.getId())
        .auth(authorized)
        .body(newRequest(true))
        .put();

    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  public void testGetConfig_InvalidOwnerType_BadRequest() throws Exception {
    grantReadPermission(app.getId());

    HttpResponse response = restRequest()
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
