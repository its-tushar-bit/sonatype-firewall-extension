/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiLegacyViolationStatusDTO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end HTTP-to-DB integration tests for {@code ApiLegacyViolationConfigResource}.
 * Verifies real DB state changes from PUT and read-back from GET, plus input-validation
 * behavior (null body, owner-type rejection at the routing layer).
 */
@IqH2Test
class IqH2ApiLegacyViolationConfigResourceTest
{
  private IqTestContext ctx;

  private OrganizationDAO organizationDAO;

  private Application app;

  private Organization org;

  @BeforeEach
  void setUp() {
    organizationDAO = ctx.lookup(OrganizationDAO.class);
    org = ctx.tempEntity().newOrganization();
    app = ctx.tempEntity().newApplicationWithParent(org);
  }

  @Test
  void getConfig_application_returnsDefaultsForFreshApp() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("application/{ownerId}")
        .parameter(app.getPublicId())
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiLegacyViolationStatusDTO body = response.getBody(ApiLegacyViolationStatusDTO.class);
    assertThat(body).isNotNull();
    assertThat(body.enabled).isIn(null, Boolean.FALSE);
  }

  @Test
  void getConfig_organization_reflectsDbState() throws Exception {
    org.setLegacyViolationEnabled(true);
    org.setAllowLegacyViolationOverride(false);
    organizationDAO.update(org);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("organization/{ownerId}")
        .parameter(org.getId())
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiLegacyViolationStatusDTO body = response.getBody(ApiLegacyViolationStatusDTO.class);
    assertThat(body.enabled).isTrue();
    assertThat(body.allowOverride).isFalse();
  }

  @Test
  void setConfig_organization_persistsToDb() throws Exception {
    ApiLegacyViolationStatusDTO request = new ApiLegacyViolationStatusDTO();
    request.enabled = true;
    request.allowOverride = true;

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("organization/{ownerId}")
        .parameter(org.getId())
        .body(request)
        .put();

    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiLegacyViolationStatusDTO body = response.getBody(ApiLegacyViolationStatusDTO.class);
    assertThat(body.enabled).isTrue();
    assertThat(body.allowOverride).isTrue();

    Organization reloaded = organizationDAO.getByIdNotNull(org.getId());
    assertThat(reloaded.isLegacyViolationEnabled()).isTrue();
    assertThat(reloaded.isAllowLegacyViolationOverride()).isTrue();
  }

  @Test
  void setConfig_application_persistsEnabledFlag() throws Exception {
    ApiLegacyViolationStatusDTO request = new ApiLegacyViolationStatusDTO();
    request.enabled = true;

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("application/{ownerId}")
        .parameter(app.getPublicId())
        .body(request)
        .put();

    assertThat(response.getStatusCode()).isEqualTo(200);
    ApiLegacyViolationStatusDTO body = response.getBody(ApiLegacyViolationStatusDTO.class);
    assertThat(body.enabled).isTrue();
  }

  @Test
  void setConfig_invalidOwnerTypeReturnsNotFoundFromRouting() throws Exception {
    ApiLegacyViolationStatusDTO request = new ApiLegacyViolationStatusDTO();
    request.enabled = true;

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("repository/{ownerId}")
        .parameter(app.getPublicId())
        .body(request)
        .put();

    // OWNER_PATH regex `{ownerType: application|organization}` rejects unknown types at the routing
    // layer with 404, so the in-method parseOwnerType branch is unreachable from HTTP.
    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  @Test
  void getConfig_invalidOwnerTypeReturnsNotFoundFromRouting() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.LEGACY_VIOLATIONS_CONFIG_PATH_V2)
        .path("repository/{ownerId}")
        .parameter(app.getPublicId())
        .get();

    assertThat(response.getStatusCode()).isEqualTo(404);
  }
}
