/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.net.HttpCookie;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.service.ApiReverseProxyAuthenticationConfigurationService;
import com.sonatype.insight.brain.model.configuration.ReverseProxyAuthenticationConfiguration;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;

import com.google.common.net.HttpHeaders;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests authentication aspects of the public REST API in general.
 */
public class PublicRestApiAuthcTest
    extends AbstractBrainServiceIntegrationTest
{
  @Test
  public void testSessionCookieSufficientWithoutCsrfTokenForSafeRequests() throws Exception {
    HttpResponse response = restRequest().path(UserSessionResource.RESOURCE_PATH).post();
    assertResponseStatus(204, response);

    HttpCookie sessionCookie = response.getSessionCookie();
    assertThat(sessionCookie).isNotNull();

    HttpRequest request =
        restRequest().path(PublicApiPaths.BASE_PATH, "any/thing").anon().noCsrfToken().cookie(sessionCookie);
    response = request.get();
    assertResponseStatus(404, response);
  }

  @Test
  public void testSessionCookieInsufficientWithoutCsrfTokenForUnsafeRequests() throws Exception {
    HttpResponse response = restRequest().path(UserSessionResource.RESOURCE_PATH).post();
    assertResponseStatus(204, response);

    HttpCookie sessionCookie = response.getSessionCookie();
    assertThat(sessionCookie).isNotNull();

    HttpRequest request =
        restRequest().path(PublicApiPaths.BASE_PATH, "any/thing").anon().noCsrfToken().cookie(sessionCookie);
    response = request.put();
    assertResponseStatus(401, response);

    response = request.post();
    assertResponseStatus(401, response);

    response = request.delete();
    assertResponseStatus(401, response);
  }

  @Test
  public void testSessionCookieWithCsrfTokenSufficientForUnsafeRequests() throws Exception {
    HttpResponse response = restRequest().path(UserSessionResource.RESOURCE_PATH).post();
    assertResponseStatus(204, response);

    HttpCookie sessionCookie = response.getSessionCookie();
    assertThat(sessionCookie).isNotNull();

    HttpRequest request = restRequest().path(PublicApiPaths.BASE_PATH, "any/thing")
        .anon()
        .csrfToken("nonce", "nonce")
        .cookie(sessionCookie);
    response = request.put();
    assertResponseStatus(404, response);

    response = request.post();
    assertResponseStatus(404, response);

    response = request.delete();
    assertResponseStatus(404, response);
  }

  @Test
  public void testNoAuthentication() throws Exception {
    testBadAuthentication(null, null, ErrorResponseGenerator.MSG_MISSING_CREDENTIALS);
  }

  @Test
  public void testInvalidUser() throws Exception {
    testBadAuthentication("yeti", "password", ErrorResponseGenerator.MSG_LOGIN_FAILURE_DEFAULT);
  }

  @Test
  public void testInvalidPassword() throws Exception {
    testBadAuthentication("admin", "wrong password", ErrorResponseGenerator.MSG_LOGIN_FAILURE_DEFAULT);
  }

  private void testBadAuthentication(String username, String password, String expectedMessage) throws Exception {
    HttpRequest request = restRequest().path(PublicApiPaths.BASE_PATH, "any/thing").auth(username, password);
    HttpResponse response = request.get();
    assertResponseStatus(401, response);
    assertResponse401(response, expectedMessage);

    response = request.put();
    assertResponse401(response, expectedMessage);

    response = request.post();
    assertResponse401(response, expectedMessage);

    response = request.delete();
    assertResponse401(response, expectedMessage);
  }

  @Test
  public void testExplicitCredentialsSufficientForAuthentication() throws Exception {
    HttpRequest request = restRequest().path(PublicApiPaths.BASE_PATH, "any/thing").noCsrfToken();
    assertResponses(request, 404);
  }

  @Test
  public void testRequestsDoNotCreateSession() throws Exception {
    HttpRequest request = restRequest().path(PublicApiPaths.BASE_PATH, "any/thing");
    HttpResponse response = request.get();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.put();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.post();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.delete();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie()).isNull();

    startIqTestServer();
    enableReverseProxyAuthentication();
    request = restRequest().header("REMOTE_USER", "admin").anon();

    request.path(PublicApiPaths.BASE_PATH, "any/thing");
    response = request.get();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.put();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.post();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie()).isNull();

    response = request.delete();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie()).isNull();
  }

  @Test
  public void testReverseProxy() throws Exception {
    enableReverseProxyAuthentication();

    HttpRequest request = restRequest().header("REMOTE_USER", "admin").anon();

    request.path(PublicApiPaths.BASE_PATH, "any/thing");
    assertResponses(request, 404);
  }

  @Test
  public void testReverseProxyBeforeBasicAuthentication() throws Exception {
    enableReverseProxyAuthentication();

    HttpRequest request = restRequest().header("REMOTE_USER", "admin").auth("admin", "wrong password");

    request.path(PublicApiPaths.BASE_PATH, "any/thing");
    assertResponses(request, 404);
  }

  @Test
  public void testReverseProxyMissingHeaderFallbackToBasicAuthentication() throws Exception {
    enableReverseProxyAuthentication();

    HttpRequest request = restRequest();

    request.path(PublicApiPaths.BASE_PATH, "any/thing");
    assertResponses(request, 404);
  }

  private void assertResponse401(HttpResponse response, String expectedMessage) {
    assertResponseStatus(401, response);
    assertThat(response.getBodyText()).isEqualTo(expectedMessage);
    assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isNull();
  }

  private void assertResponses(HttpRequest request, int status) throws Exception {
    HttpResponse response = request.get();
    assertResponseStatus(status, response);

    response = request.put();
    assertResponseStatus(status, response);

    response = request.post();
    assertResponseStatus(status, response);

    response = request.delete();
    assertResponseStatus(status, response);
  }

  @Test
  public void testReverseProxyAuthenticationRequiresCsrfTokenForUnsafeRequests() throws Exception {
    enableReverseProxyAuthentication();

    HttpRequest request = restRequest().header("REMOTE_USER", "admin").anon().noCsrfToken();
    request.path(PublicApiPaths.BASE_PATH, "any/thing");

    HttpResponse response = request.get();
    assertResponseStatus(404, response);

    response = request.put();
    assertResponse401(response, AntiCsrfFilter.ERROR_MSG);

    response = request.post();
    assertResponse401(response, AntiCsrfFilter.ERROR_MSG);

    response = request.delete();
    assertResponse401(response, AntiCsrfFilter.ERROR_MSG);
  }

  private void enableReverseProxyAuthentication() {
    tempEntity.newReverseProxyAuthenticationConfiguration(true,
        ReverseProxyAuthenticationConfiguration.DEFAULT_USERNAME_HEADER, false, null);
    getCLMServer().getInstance(ApiReverseProxyAuthenticationConfigurationService.class)
        .applyReverseProxyAuthenticationConfigurationToClients();
  }
}
