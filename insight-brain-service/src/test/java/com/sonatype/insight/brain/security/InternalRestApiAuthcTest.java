/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.net.HttpCookie;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.hds.CIResource;
import com.sonatype.insight.brain.hds.RepoManResource;
import com.sonatype.insight.brain.ide.IdeResource;
import com.sonatype.insight.brain.integration.ApplicationSummaryResource;
import com.sonatype.insight.brain.integration.PolicyEvaluationSummaryResource;
import com.sonatype.insight.brain.integration.repository.RepositoryResource;
import com.sonatype.insight.brain.organization.ApplicationResource;
import com.sonatype.insight.brain.policy.LicensedStagesResource;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateResource;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigResource;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests authentication aspects of the internal REST API in general.
 */
public class InternalRestApiAuthcTest
    extends AbstractBrainServiceTest
{
  private HttpResponse login() throws Exception {
    return restRequest().path(UserSessionResource.SERVICE_PATH).noCsrfToken().post();
  }

  @Test
  public void testSessionCookieSufficientWithoutCsrfTokenForSafeRequests() throws Exception {
    HttpResponse response = login();
    assertResponseStatus(204, response);

    HttpCookie sessionCookie = response.getSessionCookie();
    assertThat(sessionCookie, is(notNullValue()));

    HttpRequest request = restRequest().path("rest/any/thing").anon().noCsrfToken().cookie(sessionCookie);
    response = request.get();
    assertResponseStatus(404, response);
  }

  @Test
  public void testSessionCookieInsufficientWithoutCsrfTokenForUnsafeRequests() throws Exception {
    HttpResponse response = login();
    assertResponseStatus(204, response);

    HttpCookie sessionCookie = response.getSessionCookie();
    assertThat(sessionCookie, is(notNullValue()));

    HttpRequest request = restRequest().path("rest/any/thing").anon().noCsrfToken().cookie(sessionCookie);
    response = request.put();
    assertResponseStatus(401, response);

    response = request.post();
    assertResponseStatus(401, response);

    response = request.delete();
    assertResponseStatus(401, response);
  }

  @Test
  @ManualServerInit
  public void testSessionCookieSufficientWhenCsrfProtectionDisabled() throws Exception {
    initServer(new Configurator()
    {
      @Override
      public void configure(InsightConfig config) {
        config.setCsrfProtection(false);
      }
    });

    HttpResponse response = login();
    assertResponseStatus(204, response);

    HttpCookie sessionCookie = response.getSessionCookie();
    assertThat(sessionCookie, is(notNullValue()));

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
    assertThat(sessionCookie, is(notNullValue()));

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
    assertThat(sessionCookie, is(notNullValue()));

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

    HttpResponse response = request.subpath(PolicyEvaluationSummaryResource.SERVICE_PATH).parameter("appId", "stageId")
        .get();
    assertResponseStatus(400, response);
    assertThat(response.getSessionCookie(), is(nullValue()));

    response = request.subpath(IdeResource.SERVICE_PATH, "scan/simple/appId/hash").get();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie(), is(nullValue()));

    response = request.subpath(IdeResource.SERVICE_PATH, "scan/enhanced/appId/hash").post();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie(), is(nullValue()));

    response = request.subpath(ReportResource.SERVICE_PATH, ReportResource.DOWNLOAD_BUNDLE_PATH)
        .parameter("appId", "scanId").get();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie(), is(nullValue()));

    response = request.subpath(ApplicationSummaryResource.SERVICE_PATH).get();
    assertResponseStatus(200, response);
    assertThat(response.getSessionCookie(), is(nullValue()));

    response = request.subpath(RepositoryResource.SERVICE_PATH, "manager", "repo").post();
    assertResponseStatus(204, response);
    assertThat(response.getSessionCookie(), is(nullValue()));

    response = request.subpath(ApplicationResource.SERVICE_PATH, ApplicationResource.GET_APPLICATION_NAMES).get();
    assertResponseStatus(200, response);
    assertThat(response.getSessionCookie(), is(nullValue()));

    response = request.subpath(ApplicationResource.SERVICE_PATH, ApplicationResource.VALIDATE_PATH).parameter("appId")
        .get();
    assertResponseStatus(200, response);
    assertThat(response.getSessionCookie(), is(nullValue()));

    response = request.subpath(PolicyEvaluateResource.SERVICE_PATH).parameter("appId").body("").post();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie(), is(nullValue()));

    response = request.subpath(CIResource.SERVICE_PATH, CIResource.SCAN_PATH).parameter("appId").put();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie(), is(nullValue()));

    response = request.subpath(RepoManResource.SERVICE_PATH, RepoManResource.SCAN_PATH).parameter("appId").put();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie(), is(nullValue()));

    response = request.subpath(ProprietaryConfigResource.SERVICE_PATH).get();
    assertResponseStatus(200, response);
    assertThat(response.getSessionCookie(), is(nullValue()));

    response = request.subpath(LicensedStagesResource.SERVICE_PATH).get();
    assertResponseStatus(200, response);
    assertThat(response.getSessionCookie(), is(nullValue()));
  }

  @Test
  public void testNoSessionCreatedForNonLoginRequests() throws Exception {
    HttpRequest request = restRequest().path("rest/any/thing").auth().noCsrfToken();
    HttpResponse response = request.get();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie(), is(nullValue()));
  }
}
