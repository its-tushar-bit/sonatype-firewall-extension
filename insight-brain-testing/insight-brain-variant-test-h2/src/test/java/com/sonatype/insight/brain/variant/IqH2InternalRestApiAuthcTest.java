/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.net.HttpCookie;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests authentication aspects of the internal REST API in general.
 */
@IqH2Test
class IqH2InternalRestApiAuthcTest
{
  private IqTestContext ctx;

  @org.junit.jupiter.api.AfterEach
  void resetCsrfProtection() {
    // testSessionCookieSufficientWhenCsrfProtectionDisabled disables CSRF on the reused server; restore the
    // default so sibling tests (and other classes on the shared fork) see CSRF enforcement again.
    ctx.resetProperties(SystemConfigurationProperty.CSRF_PROTECTION);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest();
  }

  private HttpRequest licenseRequest() {
    return ctx.restRequest()
        .path(PublicApiPaths.PRODUCT_LICENSE_RESOURCE_PATH)
        .part("file", "sonatype.lic", new byte[1]);
  }

  private HttpResponse uploadLicense(HttpRequest licenseRequest) throws Exception {
    return licenseRequest.post();
  }

  private HttpResponse login() throws Exception {
    return restRequest().path(com.sonatype.insight.brain.security.UserSessionResource.RESOURCE_PATH)
        .noCsrfToken()
        .post();
  }

  @Test
  void testSessionCookieSufficientWithoutCsrfTokenForSafeRequests() throws Exception {
    HttpResponse response = login();
    ctx.assertResponseStatus(204, response);

    HttpCookie sessionCookie = response.getSessionCookie();
    assertThat(sessionCookie).isNotNull();

    HttpRequest request = restRequest().path("rest/any/thing").anon().noCsrfToken().cookie(sessionCookie);
    response = request.get();
    ctx.assertResponseStatus(404, response);
  }

  @Test
  void testSessionCookieInsufficientWithoutCsrfTokenForUnsafeRequests() throws Exception {
    HttpResponse response = login();
    ctx.assertResponseStatus(204, response);

    HttpCookie sessionCookie = response.getSessionCookie();
    assertThat(sessionCookie).isNotNull();

    HttpRequest request = restRequest().path("rest/any/thing").anon().noCsrfToken().cookie(sessionCookie);
    response = request.put();
    ctx.assertResponseStatus(401, response);

    response = request.post();
    ctx.assertResponseStatus(401, response);

    response = request.delete();
    ctx.assertResponseStatus(401, response);
  }

  @Test
  void testSessionCookieSufficientWhenCsrfProtectionDisabled() throws Exception {
    ApiConfigurationService configurationService = ctx.lookup(ApiConfigurationService.class);
    configurationService.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.CSRF_PROTECTION, false);
    configurationService.applyConfigurationToClients(SystemConfigurationProperty.CSRF_PROTECTION);

    HttpResponse response = login();
    ctx.assertResponseStatus(204, response);

    HttpCookie sessionCookie = response.getSessionCookie();
    assertThat(sessionCookie).isNotNull();

    HttpRequest request = restRequest().path("rest/any/thing").anon().noCsrfToken().cookie(sessionCookie);
    response = request.get();
    ctx.assertResponseStatus(404, response);

    response = request.put();
    ctx.assertResponseStatus(404, response);

    response = request.post();
    ctx.assertResponseStatus(404, response);

    response = request.delete();
    ctx.assertResponseStatus(404, response);

    // form-data based requests manually call AntiCsrfFilter.validate() that we test here
    response = uploadLicense(licenseRequest().anon().noCsrfToken().cookie(sessionCookie));
    ctx.assertResponseStatus(200, response);
  }

  @Test
  void testCsrfCookieWithoutMatchingHeaderInsufficient() throws Exception {
    HttpResponse response = login();
    ctx.assertResponseStatus(204, response);

    HttpCookie sessionCookie = response.getSessionCookie();
    assertThat(sessionCookie).isNotNull();

    HttpRequest request = restRequest().path("rest/any/thing").anon().csrfToken("nonce", null).cookie(sessionCookie);
    response = request.put();
    ctx.assertResponseStatus(401, response);

    response = request.post();
    ctx.assertResponseStatus(401, response);

    response = request.delete();
    ctx.assertResponseStatus(401, response);
  }

  @Test
  void testCsrfHeaderWithoutMatchingCookieInsufficient() throws Exception {
    HttpResponse response = login();
    ctx.assertResponseStatus(204, response);

    HttpCookie sessionCookie = response.getSessionCookie();
    assertThat(sessionCookie).isNotNull();

    HttpRequest request = restRequest().path("rest/any/thing").anon().csrfToken(null, "nonce").cookie(sessionCookie);
    response = request.put();
    ctx.assertResponseStatus(401, response);

    response = request.post();
    ctx.assertResponseStatus(401, response);

    response = request.delete();
    ctx.assertResponseStatus(401, response);
  }

  @Test
  void testExplicitAuthSufficientWithoutCsrfTokenForSafeRequests() throws Exception {
    HttpRequest request = restRequest().path("rest/any/thing").auth().noCsrfToken();
    HttpResponse response = request.get();
    ctx.assertResponseStatus(404, response);
  }

  @Test
  void testExplicitAuthInsufficientWithoutCsrfTokenForUnsafeRequests() throws Exception {
    HttpRequest request = restRequest().path("rest/any/thing").auth().noCsrfToken();
    HttpResponse response = request.put();
    ctx.assertResponseStatus(401, response);

    response = request.post();
    ctx.assertResponseStatus(401, response);

    response = request.delete();
    ctx.assertResponseStatus(401, response);
  }

  @Test
  void testExplicitAuthSufficientWithoutCsrfTokenForClientIntegrationRequests() throws Exception {
    HttpRequest request = restRequest().auth().noCsrfToken();

    HttpResponse response = request.subpath(PolicyEvaluationSummaryResource.RESOURCE_PATH)
        .parameter("appId", "stageId")
        .get();
    ctx.assertResponseStatus(400, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.subpath(IdeResource.RESOURCE_PATH, "scan/simple/appId/hash").get();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.subpath(IdeResource.RESOURCE_PATH, "scan/enhanced/appId/hash").post();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.subpath(ReportResource.RESOURCE_PATH, ReportResource.DOWNLOAD_BUNDLE_PATH)
        .parameter("appId", "scanId")
        .get();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.subpath(ApplicationSummaryResource.RESOURCE_PATH).get();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.subpath(OrganizationSummaryResource.RESOURCE_PATH).get();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.subpath(RepositorySummaryResource.RESOURCE_PATH).get();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.subpath(RepositoryResource.RESOURCE_PATH, RepositoryResource.AUDIT_ENABLE_PATH)
        .parameter(ctx.tempEntity().newRepositoryManager().getInstanceId(), "repo", "true")
        .post();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.subpath(ApplicationResource.RESOURCE_PATH, ApplicationResource.GET_APPLICATION_NAMES).get();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.subpath(ApplicationResource.RESOURCE_PATH, ApplicationResource.VALIDATE_PATH)
        .parameter("appId")
        .get();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.subpath(PolicyEvaluateResource.RESOURCE_PATH).parameter("appId").body("").post();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.subpath(RepoManResource.RESOURCE_PATH, RepoManResource.SCAN_PATH).parameter("appId").put();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.subpath(ProprietaryConfigResource.RESOURCE_PATH).get();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.subpath(LicensedStagesResource.RESOURCE_PATH).get();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getSessionCookie()).isNull();
  }

  @SuppressWarnings("deprecation")
  @Test
  void testExplicitAuthSufficientWithoutCsrfTokenForDeprecatedClientIntegrationRequests() throws Exception {
    HttpRequest request = restRequest().auth().noCsrfToken();

    HttpResponse response =
        request.subpath(DeprecatedCIResource.RESOURCE_PATH, DeprecatedCIResource.SCAN_PATH).parameter("appId").put();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getSessionCookie()).isNull();

    response =
        request.subpath(DeprecatedCLIResource.RESOURCE_PATH, DeprecatedCLIResource.SCAN_PATH).parameter("appId").put();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getSessionCookie()).isNull();
  }

  @Test
  void testNoSessionCreatedForNonLoginRequests() throws Exception {
    HttpRequest request = restRequest().path("rest/any/thing").auth().noCsrfToken();
    HttpResponse response = request.get();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getSessionCookie()).isNull();
  }
}
