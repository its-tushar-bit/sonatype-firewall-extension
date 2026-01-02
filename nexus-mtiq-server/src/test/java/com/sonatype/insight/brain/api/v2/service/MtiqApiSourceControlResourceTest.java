/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Inject;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.insight.brain.common.test.SlowTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_TENANT_CONFIG_FEATURES_PATH;
import static com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.FEATURE_SAAS_LIFECYCLE_SCM_ENABLED;

@Category(SlowTest.class)
public class MtiqApiSourceControlResourceTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  @Inject
  private ApiSourceControlAdapter apiSourceControlAdapter;

  private static final String OWNER_TYPE = "{ownerType:application|organization}";

  private static final String OWNER_ID = "{internalOwnerId}";

  private static final String BY_OWNER = OWNER_TYPE + "/" + OWNER_ID;

  @Before
  public void setup() {
    apiSourceControlAdapter = lookup(ApiSourceControlAdapter.class);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.SOURCE_CONTROL_PATH_V2).auth();
  }

  @Test
  public void testMtiqSupportsGithub() {
    testAsTestTenant(tenant -> {
      HttpResponse response = sendSourceControlConfigWithProvider(SourceControlProvider.GITHUB);
      assertResponseStatus(200, response);
    });
  }

  @Test
  public void testMtiqSupportsAzure() {
    testAsTestTenant(tenant -> {
      HttpResponse response = sendSourceControlConfigWithProvider(SourceControlProvider.AZURE);
      assertResponseStatus(200, response);
    });
  }

  @Test
  public void testMtiqSupportsBitBucket() {
    testAsTestTenant(tenant -> {
      HttpResponse response = sendSourceControlConfigWithProvider(SourceControlProvider.BITBUCKET);
      assertResponseStatus(200, response);
    });
  }

  @Test
  public void testMtiqSupportsGitlab() {
    testAsTestTenant(tenant -> {
      HttpResponse response = sendSourceControlConfigWithProvider(SourceControlProvider.GITLAB);
      assertResponseStatus(200, response);
    });
  }

  @Test
  public void testMtiqDoesNotSupportGithubWithoutFeatureEnabled() {
    testAsTestTenant(tenant -> {
      HttpResponse response = callConfigFeaturesEndpoint(tenant.tenantSlug)
          .path(FEATURE_SAAS_LIFECYCLE_SCM_ENABLED)
          .delete();
      assertResponseStatus(204, response);

      response = sendSourceControlConfigWithProvider(SourceControlProvider.GITHUB);
      assertResponseStatus(404, response);
    });
  }

  @Test
  public void testMtiqDoesNotSupportAzureWithoutFeatureEnabled() {
    testAsTestTenant(tenant -> {
      HttpResponse response = callConfigFeaturesEndpoint(tenant.tenantSlug)
          .path(FEATURE_SAAS_LIFECYCLE_SCM_ENABLED)
          .delete();
      assertResponseStatus(204, response);

      response = sendSourceControlConfigWithProvider(SourceControlProvider.AZURE);
      assertResponseStatus(404, response);
    });
  }

  @Test
  public void testMtiqDoesNotSupportGitlabWithoutFeatureEnabled() {
    testAsTestTenant(tenant -> {
      HttpResponse response = callConfigFeaturesEndpoint(tenant.tenantSlug)
              .path(FEATURE_SAAS_LIFECYCLE_SCM_ENABLED)
              .delete();
      assertResponseStatus(204, response);

      response = sendSourceControlConfigWithProvider(SourceControlProvider.GITLAB);
      assertResponseStatus(404, response);
    });
  }

  @Test
  public void testMtiqDoesNotSupportBitBucketWithoutFeatureEnabled() {
    testAsTestTenant(tenant -> {
      HttpResponse response = callConfigFeaturesEndpoint(tenant.tenantSlug)
          .path(FEATURE_SAAS_LIFECYCLE_SCM_ENABLED)
          .delete();
      assertResponseStatus(204, response);

      response = sendSourceControlConfigWithProvider(SourceControlProvider.BITBUCKET);
      assertResponseStatus(404, response);
    });
  }

  private HttpResponse sendSourceControlConfigWithProvider(SourceControlProvider gitlab) throws Exception {
    Organization org = tenantTemporaryEntity.newOrganization();

    ApiSourceControlDTO sourceControl = apiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setProvider(gitlab)
            .setOwnerId(org.getId()).setToken("token").setCommitStatusEnabled(false)
            .build());

    return restRequest()
        .path(BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, org.getId())
        .body(sourceControl)
        .post();
  }

  private HttpRequest callConfigFeaturesEndpoint(String tenantSlug) {
    return adminRestRequest(ADMIN_TENANT_CONFIG_FEATURES_PATH)
        .parameter(tenantSlug);
  }
}
