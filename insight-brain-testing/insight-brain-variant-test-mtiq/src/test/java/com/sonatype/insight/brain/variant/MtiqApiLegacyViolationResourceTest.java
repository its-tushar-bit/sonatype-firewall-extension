/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiLegacyViolationResource;
import com.sonatype.insight.brain.api.v2.dto.ApiLegacyViolationStatusDTO;
import com.sonatype.insight.brain.model.Application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MTIQ variant conversion of {@code MtiqApiLegacyViolationResourceTest} (which extended
 * {@code AbstractMultiTenantBaseIntegrationTest}). Verifies that operations against the legacy violations public
 * API are scoped to the calling tenant and that an application's publicId from one tenant is not accessible from
 * another tenant. No base class; an injected {@link MtiqTestContext} supplies the reused multi-tenant server, a
 * fresh per-test tenant, and REST/tenant access.
 */
@MtiqTest
class MtiqApiLegacyViolationResourceTest
{
  // Injected by MtiqServerExtension: the reused multi-tenant server + a fresh per-test tenant context.
  private MtiqTestContext ctx;

  @Test
  void testListLegacyViolations_ScopedToCallingTenant() {
    ctx.testAsTestTenant(tenant -> {
      Application app = ctx.tempEntity().newApplicationWithParent();

      HttpResponse response = ctx.restRequest()
          .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
          .path(ApiLegacyViolationResource.APPLICATION_PATH)
          .parameter(app.getPublicId())
          .get();

      assertThat(response.getStatusCode()).isEqualTo(200);
    });
  }

  @Test
  void testListLegacyViolations_AppFromOtherTenantNotVisible() {
    final String[] otherTenantAppPublicId = new String[1];

    ctx.testAsGlobal(global -> {
      Application app = ctx.tempEntity().newApplicationWithParent();
      otherTenantAppPublicId[0] = app.getPublicId();
    });

    ctx.testAsTestTenant(tenant -> {
      HttpResponse response = ctx.restRequest()
          .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
          .path(ApiLegacyViolationResource.APPLICATION_PATH)
          .parameter(otherTenantAppPublicId[0])
          .get();

      assertThat(response.getStatusCode()).isIn(404, 403);
    });
  }

  @Test
  void testGetConfig_ScopedToCallingTenant() {
    ctx.testAsTestTenant(tenant -> {
      Application app = ctx.tempEntity().newApplicationWithParent();

      HttpResponse response = ctx.restRequest()
          .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
          .path("application/{ownerId}")
          .parameter(app.getPublicId())
          .get();

      assertThat(response.getStatusCode()).isEqualTo(200);
    });
  }

  @Test
  void testGetConfig_AppFromOtherTenantNotVisible() {
    final String[] otherTenantAppPublicId = new String[1];

    ctx.testAsGlobal(global -> {
      Application app = ctx.tempEntity().newApplicationWithParent();
      otherTenantAppPublicId[0] = app.getPublicId();
    });

    ctx.testAsTestTenant(tenant -> {
      HttpResponse response = ctx.restRequest()
          .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
          .path("application/{ownerId}")
          .parameter(otherTenantAppPublicId[0])
          .get();

      assertThat(response.getStatusCode()).isIn(404, 403);
    });
  }

  @Test
  void testSetConfig_ScopedToCallingTenant() {
    ctx.testAsTestTenant(tenant -> {
      Application app = ctx.tempEntity().newApplicationWithParent();

      ApiLegacyViolationStatusDTO request = new ApiLegacyViolationStatusDTO();
      request.enabled = true;

      HttpResponse response = ctx.restRequest()
          .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
          .path("application/{ownerId}")
          .parameter(app.getPublicId())
          .body(request)
          .put();

      assertThat(response.getStatusCode()).isEqualTo(200);
    });
  }

  @Test
  void testSetConfig_AppFromOtherTenantNotMutable() {
    final String[] otherTenantAppPublicId = new String[1];

    ctx.testAsGlobal(global -> {
      Application app = ctx.tempEntity().newApplicationWithParent();
      otherTenantAppPublicId[0] = app.getPublicId();
    });

    ctx.testAsTestTenant(tenant -> {
      ApiLegacyViolationStatusDTO request = new ApiLegacyViolationStatusDTO();
      request.enabled = true;

      HttpResponse response = ctx.restRequest()
          .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
          .path("application/{ownerId}")
          .parameter(otherTenantAppPublicId[0])
          .body(request)
          .put();

      assertThat(response.getStatusCode()).isIn(404, 403);
    });
  }

  @Test
  void testGrant_AppFromOtherTenantNotMutable() {
    final String[] otherTenantAppPublicId = new String[1];

    ctx.testAsGlobal(global -> {
      Application app = ctx.tempEntity().newApplicationWithParent();
      otherTenantAppPublicId[0] = app.getPublicId();
    });

    ctx.testAsTestTenant(tenant -> {
      HttpResponse response = ctx.restRequest()
          .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
          .path(ApiLegacyViolationResource.GRANT_PATH)
          .parameter(otherTenantAppPublicId[0])
          .post();

      assertThat(response.getStatusCode()).isIn(404, 403);
    });
  }

  @Test
  void testRevoke_AppFromOtherTenantNotMutable() {
    final String[] otherTenantAppPublicId = new String[1];

    ctx.testAsGlobal(global -> {
      Application app = ctx.tempEntity().newApplicationWithParent();
      otherTenantAppPublicId[0] = app.getPublicId();
    });

    ctx.testAsTestTenant(tenant -> {
      HttpResponse response = ctx.restRequest()
          .path(PublicApiPaths.LEGACY_VIOLATIONS_PATH_V2)
          .path(ApiLegacyViolationResource.REVOKE_PATH)
          .parameter(otherTenantAppPublicId[0])
          .post();

      assertThat(response.getStatusCode()).isIn(404, 403);
    });
  }
}
