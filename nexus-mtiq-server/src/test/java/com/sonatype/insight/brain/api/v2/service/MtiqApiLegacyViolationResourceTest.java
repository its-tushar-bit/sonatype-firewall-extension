/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiLegacyViolationResource;
import com.sonatype.insight.brain.api.v2.dto.ApiLegacyViolationStatusDTO;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Multi-tenant integration tests for the legacy violations public API.
 * Verifies that operations are scoped to the calling tenant and that an
 * application's publicId from one tenant is not accessible from another tenant.
 */
@Category(SlowTest.class)
public class MtiqApiLegacyViolationResourceTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().auth();
  }

  @Test
  public void testListLegacyViolations_ScopedToCallingTenant() {
    testAsTestTenant(tenant -> {
      Application app = tenantTemporaryEntity.newApplicationWithParent();

      HttpResponse response = restRequest()
          .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
          .path(ApiLegacyViolationResource.APPLICATION_PATH)
          .parameter(app.getPublicId())
          .get();

      assertThat(response.getStatusCode()).isEqualTo(200);
    });
  }

  @Test
  public void testListLegacyViolations_AppFromOtherTenantNotVisible() {
    final String[] otherTenantAppPublicId = new String[1];

    testAsGlobal(global -> {
      Application app = tenantTemporaryEntity.newApplicationWithParent();
      otherTenantAppPublicId[0] = app.getPublicId();
    });

    testAsTestTenant(tenant -> {
      HttpResponse response = restRequest()
          .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
          .path(ApiLegacyViolationResource.APPLICATION_PATH)
          .parameter(otherTenantAppPublicId[0])
          .get();

      assertThat(response.getStatusCode()).isIn(404, 403);
    });
  }

  @Test
  public void testGetConfig_ScopedToCallingTenant() {
    testAsTestTenant(tenant -> {
      Application app = tenantTemporaryEntity.newApplicationWithParent();

      HttpResponse response = restRequest()
          .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
          .path("application/{ownerId}")
          .parameter(app.getPublicId())
          .get();

      assertThat(response.getStatusCode()).isEqualTo(200);
    });
  }

  @Test
  public void testGetConfig_AppFromOtherTenantNotVisible() {
    final String[] otherTenantAppPublicId = new String[1];

    testAsGlobal(global -> {
      Application app = tenantTemporaryEntity.newApplicationWithParent();
      otherTenantAppPublicId[0] = app.getPublicId();
    });

    testAsTestTenant(tenant -> {
      HttpResponse response = restRequest()
          .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
          .path("application/{ownerId}")
          .parameter(otherTenantAppPublicId[0])
          .get();

      assertThat(response.getStatusCode()).isIn(404, 403);
    });
  }

  @Test
  public void testSetConfig_ScopedToCallingTenant() {
    testAsTestTenant(tenant -> {
      Application app = tenantTemporaryEntity.newApplicationWithParent();

      ApiLegacyViolationStatusDTO request = new ApiLegacyViolationStatusDTO();
      request.enabled = true;

      HttpResponse response = restRequest()
          .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
          .path("application/{ownerId}")
          .parameter(app.getPublicId())
          .body(request)
          .put();

      assertThat(response.getStatusCode()).isEqualTo(200);
    });
  }

  @Test
  public void testSetConfig_AppFromOtherTenantNotMutable() {
    final String[] otherTenantAppPublicId = new String[1];

    testAsGlobal(global -> {
      Application app = tenantTemporaryEntity.newApplicationWithParent();
      otherTenantAppPublicId[0] = app.getPublicId();
    });

    testAsTestTenant(tenant -> {
      ApiLegacyViolationStatusDTO request = new ApiLegacyViolationStatusDTO();
      request.enabled = true;

      HttpResponse response = restRequest()
          .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
          .path("application/{ownerId}")
          .parameter(otherTenantAppPublicId[0])
          .body(request)
          .put();

      assertThat(response.getStatusCode()).isIn(404, 403);
    });
  }

  @Test
  public void testGrant_AppFromOtherTenantNotMutable() {
    final String[] otherTenantAppPublicId = new String[1];

    testAsGlobal(global -> {
      Application app = tenantTemporaryEntity.newApplicationWithParent();
      otherTenantAppPublicId[0] = app.getPublicId();
    });

    testAsTestTenant(tenant -> {
      HttpResponse response = restRequest()
          .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
          .path(ApiLegacyViolationResource.GRANT_PATH)
          .parameter(otherTenantAppPublicId[0])
          .post();

      assertThat(response.getStatusCode()).isIn(404, 403);
    });
  }

  @Test
  public void testRevoke_AppFromOtherTenantNotMutable() {
    final String[] otherTenantAppPublicId = new String[1];

    testAsGlobal(global -> {
      Application app = tenantTemporaryEntity.newApplicationWithParent();
      otherTenantAppPublicId[0] = app.getPublicId();
    });

    testAsTestTenant(tenant -> {
      HttpResponse response = restRequest()
          .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
          .path(ApiLegacyViolationResource.REVOKE_PATH)
          .parameter(otherTenantAppPublicId[0])
          .post();

      assertThat(response.getStatusCode()).isIn(404, 403);
    });
  }
}
