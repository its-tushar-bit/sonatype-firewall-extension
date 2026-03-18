/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.net.HttpCookie;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.hds.DeprecatedCIResource;
import com.sonatype.insight.brain.hds.DeprecatedCLIResource;
import com.sonatype.insight.brain.hds.RepoManResource;
import com.sonatype.insight.brain.ide.IdeResource;
import com.sonatype.insight.brain.integration.ApplicationSummaryResource;
import com.sonatype.insight.brain.integration.OrganizationSummaryResource;
import com.sonatype.insight.brain.integration.PolicyEvaluationSummaryResource;
import com.sonatype.insight.brain.integration.ProprietaryConfigResource;
import com.sonatype.insight.brain.integration.RepositorySummaryResource;
import com.sonatype.insight.brain.integration.repository.RepositoryResource;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.organization.ApplicationResource;
import com.sonatype.insight.brain.policy.LicensedStagesResource;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateResource;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests authentication aspects of the internal REST API in general.
 */
public class InternalRestApiAuthcTest
    extends AbstractBrainServiceIntegrationTest
{
  private HttpResponse login() throws Exception {
    return restRequest().path(UserSessionResource.RESOURCE_PATH).noCsrfToken().post();
  }

  @Test
  public void testSessionCookieSufficientWithoutCsrfTokenForSafeRequests() throws Exception {
    HttpResponse response = login();
    assertResponseStatus(204, response);

    HttpCookie sessionCookie = response.getSessionCookie();
    assertThat(sessionCookie).isNotNull();

    HttpRequest request = restRequest().path("rest/any/thing").anon().noCsrfToken().cookie(sessionCookie);
    response = request.get();
    assertResponseStatus(404, response);
  }

  @Test
  public void testSessionCookieInsufficientWithoutCsrfTokenForUnsafeRequests() throws Exception {
    HttpResponse response = login();
    assertResponseStatus(204, response);

    HttpCookie sessionCookie = response.getSessionCookie();
    assertThat(sessionCookie).isNotNull();

    HttpRequest request = restRequest().path("rest/any/thing").anon().noCsrfToken().cookie(sessionCookie);
    response = request.put();
    assertResponseStatus(401, response);

    response = request.post();
    assertResponseStatus(401, response);

    response = request.delete();
    assertResponseStatus(401, response);
  }

  @Test
  public void testSessionCookieSufficientWhenCsrfProtectionDisabled() throws Exception {
    ApiConfigurationService configurationService = getCLMServer().getInstance(ApiConfigurationService.class);
    configurationService.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.CSRF_PROTECTION, false);
    configurationService.applyConfigurationToClients(SystemConfigurationProperty.CSRF_PROTECTION);

    HttpResponse response = login();
    assertResponseStatus(204, response);

    HttpCookie sessionCookie = response.getSessionCookie();
    assertThat(sessionCookie).isNotNull();

    HttpRequest request = restRequest().path("rest/any/thing").anon().noCsrfToken().cookie(sessionCookie);
    response = request.get();
    assertResponseStatus(404, response);

    response = request.put();
    assertResponseStatus(404, response);

    response = request.post();
    assertResponseStatus(404, response);

    response = request.delete();
    assertResponseStatus(404, response);

    // form-data based requests manually call AntiCsrfFilter.validate() that we test here
    response = uploadLicense(licenseRequest().anon().noCsrfToken().cookie(sessionCookie));
    assertResponseStatus(200, response);
  }

  @Test
  public void testCsrfCookieWithoutMatchingHeaderInsufficient() throws Exception {
    HttpResponse response = login();
    assertResponseStatus(204, response);

    HttpCookie sessionCookie = response.getSessionCookie();
    assertThat(sessionCookie).isNotNull();

    HttpRequest request = restRequest().path("rest/any/thing").anon().csrfToken("nonce", null).cookie(sessionCookie);
    response = request.put();
    assertResponseStatus(401, response);

    response = request.post();
    assertResponseStatus(401, response);

    response = request.delete();
    assertResponseStatus(401, response);
  }

  @Test
  public void testCsrfHeaderWithoutMatchingCookieInsufficient() throws Exception {
    HttpResponse response = login();
    assertResponseStatus(204, response);

    HttpCookie sessionCookie = response.getSessionCookie();
    assertThat(sessionCookie).isNotNull();

    HttpRequest request = restRequest().path("rest/any/thing").anon().csrfToken(null, "nonce").cookie(sessionCookie);
    response = request.put();
    assertResponseStatus(401, response);

    response = request.post();
    assertResponseStatus(401, response);

    response = request.delete();
    assertResponseStatus(401, response);
  }

  @Test
  public void testExplicitAuthSufficientWithoutCsrfTokenForSafeRequests() throws Exception {
    HttpRequest request = restRequest().path("rest/any/thing").auth().noCsrfToken();
    HttpResponse response = request.get();
    assertResponseStatus(404, response);
  }

  @Test
  public void testExplicitAuthInsufficientWithoutCsrfTokenForUnsafeRequests() throws Exception {
    HttpRequest request = restRequest().path("rest/any/thing").auth().noCsrfToken();
    HttpResponse response = request.put();
    assertResponseStatus(401, response);

    response = request.post();
    assertResponseStatus(401, response);

    response = request.delete();
    assertResponseStatus(401, response);
  }

  @Test
  public void testExplicitAuthSufficientWithoutCsrfTokenForClientIntegrationRequests() throws Exception {
    HttpRequest request = restRequest().auth().noCsrfToken();

    HttpResponse response = request.subpath(PolicyEvaluationSummaryResource.RESOURCE_PATH)
        .parameter("appId", "stageId")
        .get();
    assertResponseStatus(400, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.subpath(IdeResource.RESOURCE_PATH, "scan/simple/appId/hash").get();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.subpath(IdeResource.RESOURCE_PATH, "scan/enhanced/appId/hash").post();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.subpath(ReportResource.RESOURCE_PATH, ReportResource.DOWNLOAD_BUNDLE_PATH)
        .parameter("appId", "scanId")
        .get();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.subpath(ApplicationSummaryResource.RESOURCE_PATH).get();
    assertResponseStatus(200, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.subpath(OrganizationSummaryResource.RESOURCE_PATH).get();
    assertResponseStatus(200, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.subpath(RepositorySummaryResource.RESOURCE_PATH).get();
    assertResponseStatus(200, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.subpath(RepositoryResource.RESOURCE_PATH, RepositoryResource.AUDIT_ENABLE_PATH)
        .parameter(tempEntity.newRepositoryManager().getInstanceId(), "repo", "true")
        .post();
    assertResponseStatus(200, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.subpath(ApplicationResource.RESOURCE_PATH, ApplicationResource.GET_APPLICATION_NAMES).get();
    assertResponseStatus(200, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.subpath(ApplicationResource.RESOURCE_PATH, ApplicationResource.VALIDATE_PATH)
        .parameter("appId")
        .get();
    assertResponseStatus(200, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.subpath(PolicyEvaluateResource.RESOURCE_PATH).parameter("appId").body("").post();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.subpath(RepoManResource.RESOURCE_PATH, RepoManResource.SCAN_PATH).parameter("appId").put();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.subpath(ProprietaryConfigResource.RESOURCE_PATH).get();
    assertResponseStatus(200, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.subpath(LicensedStagesResource.RESOURCE_PATH).get();
    assertResponseStatus(200, response);
    assertThat(response.getSessionCookie()).isNull();
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testExplicitAuthSufficientWithoutCsrfTokenForDeprecatedClientIntegrationRequests() throws Exception {
    HttpRequest request = restRequest().auth().noCsrfToken();

    HttpResponse response =
        request.subpath(DeprecatedCIResource.RESOURCE_PATH, DeprecatedCIResource.SCAN_PATH).parameter("appId").put();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie()).isNull();

    response =
        request.subpath(DeprecatedCLIResource.RESOURCE_PATH, DeprecatedCLIResource.SCAN_PATH).parameter("appId").put();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie()).isNull();
  }

  @Test
  public void testNoSessionCreatedForNonLoginRequests() throws Exception {
    HttpRequest request = restRequest().path("rest/any/thing").auth().noCsrfToken();
    HttpResponse response = request.get();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie()).isNull();
  }
}
