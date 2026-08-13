/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlAdapter;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_TENANT_CONFIG_FEATURES_PATH;
import static com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.FEATURE_SAAS_LIFECYCLE_SCM_ENABLED;

/**
 * MTIQ variant conversion of {@code MtiqApiSourceControlResourceTest} (which extended
 * {@code AbstractMultiTenantBaseIntegrationTest}). No base class, an injected {@link MtiqTestContext} supplies the
 * reused multi-tenant server, a fresh per-test tenant context, and REST/lookup access.
 */
@MtiqTest
class MtiqApiSourceControlResourceTest
{
  private MtiqTestContext ctx;

  private ApiSourceControlAdapter apiSourceControlAdapter;

  private static final String OWNER_TYPE = "{ownerType:application|organization}";

  private static final String OWNER_ID = "{internalOwnerId}";

  private static final String BY_OWNER = OWNER_TYPE + "/" + OWNER_ID;

  @BeforeEach
  void setup() {
    apiSourceControlAdapter = ctx.lookup(ApiSourceControlAdapter.class);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.SOURCE_CONTROL_PATH_V2).auth();
  }

  @Test
  void testMtiqSupportsGithub() {
    ctx.testAsTestTenant(tenant -> {
      HttpResponse response = sendSourceControlConfigWithProvider(SourceControlProvider.GITHUB);
      ctx.assertResponseStatus(200, response);
    });
  }

  @Test
  void testMtiqSupportsAzure() {
    ctx.testAsTestTenant(tenant -> {
      HttpResponse response = sendSourceControlConfigWithProvider(SourceControlProvider.AZURE);
      ctx.assertResponseStatus(200, response);
    });
  }

  @Test
  void testMtiqSupportsBitBucket() {
    ctx.testAsTestTenant(tenant -> {
      HttpResponse response = sendSourceControlConfigWithProvider(SourceControlProvider.BITBUCKET);
      ctx.assertResponseStatus(200, response);
    });
  }

  @Test
  void testMtiqSupportsGitlab() {
    ctx.testAsTestTenant(tenant -> {
      HttpResponse response = sendSourceControlConfigWithProvider(SourceControlProvider.GITLAB);
      ctx.assertResponseStatus(200, response);
    });
  }

  @Test
  void testMtiqDoesNotSupportGithubWithoutFeatureEnabled() {
    ctx.testAsTestTenant(tenant -> {
      HttpResponse response = callConfigFeaturesEndpoint(tenant.tenantSlug)
          .path(FEATURE_SAAS_LIFECYCLE_SCM_ENABLED)
          .delete();
      ctx.assertResponseStatus(204, response);

      response = sendSourceControlConfigWithProvider(SourceControlProvider.GITHUB);
      ctx.assertResponseStatus(404, response);
    });
  }

  @Test
  void testMtiqDoesNotSupportAzureWithoutFeatureEnabled() {
    ctx.testAsTestTenant(tenant -> {
      HttpResponse response = callConfigFeaturesEndpoint(tenant.tenantSlug)
          .path(FEATURE_SAAS_LIFECYCLE_SCM_ENABLED)
          .delete();
      ctx.assertResponseStatus(204, response);

      response = sendSourceControlConfigWithProvider(SourceControlProvider.AZURE);
      ctx.assertResponseStatus(404, response);
    });
  }

  @Test
  void testMtiqDoesNotSupportGitlabWithoutFeatureEnabled() {
    ctx.testAsTestTenant(tenant -> {
      HttpResponse response = callConfigFeaturesEndpoint(tenant.tenantSlug)
          .path(FEATURE_SAAS_LIFECYCLE_SCM_ENABLED)
          .delete();
      ctx.assertResponseStatus(204, response);

      response = sendSourceControlConfigWithProvider(SourceControlProvider.GITLAB);
      ctx.assertResponseStatus(404, response);
    });
  }

  @Test
  void testMtiqDoesNotSupportBitBucketWithoutFeatureEnabled() {
    ctx.testAsTestTenant(tenant -> {
      HttpResponse response = callConfigFeaturesEndpoint(tenant.tenantSlug)
          .path(FEATURE_SAAS_LIFECYCLE_SCM_ENABLED)
          .delete();
      ctx.assertResponseStatus(204, response);

      response = sendSourceControlConfigWithProvider(SourceControlProvider.BITBUCKET);
      ctx.assertResponseStatus(404, response);
    });
  }

  private HttpResponse sendSourceControlConfigWithProvider(final SourceControlProvider provider) throws Exception {
    Organization org = ctx.tempEntity().newOrganization();

    ApiSourceControlDTO sourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setProvider(provider)
            .setOwnerId(org.getId())
            .setToken("token")
            .setCommitStatusEnabled(false)
            .build());

    return restRequest()
        .path(BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, org.getId())
        .body(sourceControl)
        .post();
  }

  private HttpRequest callConfigFeaturesEndpoint(final String tenantSlug) {
    return ctx.adminRestRequest(ADMIN_TENANT_CONFIG_FEATURES_PATH)
        .parameter(tenantSlug);
  }
}
