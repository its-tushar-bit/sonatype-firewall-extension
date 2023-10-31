/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.ws.rs.core.HttpHeaders;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.admin.authorization.AuthorizationTestHelper;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.AbstractMultiTenantBrainServiceTest;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.Test;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_TENANT_CONFIG_FEATURES_PATH;
import static com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.FEATURE_SAAS_LIFECYCLE_SCM_ENABLED;
import static org.assertj.core.api.Assertions.assertThat;

public class MtiqApiSourceControlResourceTest
    extends AbstractMultiTenantBrainServiceTest
{
  private static final String OWNER_TYPE = "{ownerType:application|organization}";

  private static final String OWNER_ID = "{internalOwnerId}";

  private static final String BY_OWNER = OWNER_TYPE + "/" + OWNER_ID;

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.SOURCE_CONTROL_PATH_V2).auth();
  }

  @Test
  public void testMtiqSupportsGithub() {
    testAsTestTenant(tenant -> {
      HttpResponse response = callConfigFeaturesEndpoint(tenant.tenantSlug)
          .path(FEATURE_SAAS_LIFECYCLE_SCM_ENABLED)
          .post();
      assertResponseStatus(204, response);

      response = sendSourceControlConfigWithProvider(SourceControlProvider.GITHUB);
      assertResponseStatus(200, response);
    });
  }

  @Test
  public void testMtiqDoesNotSupportGithubWithoutFeatureEnabled() {
    testAsTestTenant(tenant -> {
      HttpResponse response = sendSourceControlConfigWithProvider(SourceControlProvider.GITHUB);
      assertResponseStatus(404, response);
    });
  }

  @Test
  public void testMtiqDoesNotSupportOtherProviders() {
    testAsTestTenant(tenant -> {
      HttpResponse response = callConfigFeaturesEndpoint(tenant.tenantSlug)
          .path(FEATURE_SAAS_LIFECYCLE_SCM_ENABLED)
          .post();
      assertResponseStatus(204, response);

      response = sendSourceControlConfigWithProvider(SourceControlProvider.AZURE);
      assertSourceControlFailed(response, SourceControlProvider.AZURE);

      response = sendSourceControlConfigWithProvider(SourceControlProvider.GITLAB);
      assertSourceControlFailed(response, SourceControlProvider.GITLAB);

      response = sendSourceControlConfigWithProvider(SourceControlProvider.BITBUCKET);
      assertSourceControlFailed(response, SourceControlProvider.BITBUCKET);
    });
  }

  private HttpResponse sendSourceControlConfigWithProvider(SourceControlProvider gitlab) throws Exception {
    Organization org = tempEntity.newOrganization();

    ApiSourceControlDTO sourceControl = ApiSourceControlAdapter.convertToDTO(
        new SourceControl.Builder().setProvider(gitlab)
            .setOwnerId(org.getId()).setToken("token").setCommitStatusEnabled(false)
            .build());

    return restRequest()
        .path(BY_OWNER)
        .parameter(OwnerType.ORGANIZATION, org.getId())
        .body(sourceControl)
        .post();
  }

  private static void assertSourceControlFailed(HttpResponse response, SourceControlProvider provider) {
    assertResponseStatus(400, response);
    String message = String.format("SourceControl provider value '%s' is invalid, valid options are: github", provider);
    assertThat(response.getBodyText()).contains(message);
  }

  private HttpRequest callConfigFeaturesEndpoint(String tenantSlug) throws Exception {
    return adminRestRequest(ADMIN_TENANT_CONFIG_FEATURES_PATH)
        .parameter(tenantSlug)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + AuthorizationTestHelper.createJwt());
  }

  protected HttpRequest adminRestRequest(String path) {
    return super.adminRequest().path("api/").path(path);
  }
}
