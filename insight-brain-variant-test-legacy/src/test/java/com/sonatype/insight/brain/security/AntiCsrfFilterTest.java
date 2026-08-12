/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.service.ApiReverseProxyAuthenticationConfigurationService;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.configuration.ReverseProxyAuthenticationConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.ReverseProxyAuthenticationConfiguration;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;
import com.sonatype.insight.test.networking.SslProperties;
import java.net.HttpCookie;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class AntiCsrfFilterTest
    extends AbstractBrainServiceIntegrationTest
{
  // a known rest endpoint defined from SecurityModule for AntiCsrfFilter for integrations
  private static final String REST_PATH = "rest/ci/scan/testApp";

  private ReverseProxyAuthenticationConfigurationDAO reverseProxyAuthenticationConfigurationDAO;

  @Before
  public void setUp() {
    reverseProxyAuthenticationConfigurationDAO = lookup(ReverseProxyAuthenticationConfigurationDAO.class);
    if (reverseProxyAuthenticationConfigurationDAO != null) {
      // Configure reverse proxy authentication for CSRF testing.
      // Use set() (upsert) to handle both fresh databases and those where the
      // DataMigrator has already inserted a default record during server boot.
      ReverseProxyAuthenticationConfiguration rpConfig =
          new ReverseProxyAuthenticationConfiguration(true,
              ReverseProxyAuthenticationConfiguration.DEFAULT_USERNAME_HEADER, false, null);
      reverseProxyAuthenticationConfigurationDAO.set(rpConfig);
      getCLMServer().getInstance(ApiReverseProxyAuthenticationConfigurationService.class)
          .applyReverseProxyAuthenticationConfigurationToClients();
    }
  }

  @Test
  public void testRequestWithBasicAuthWithCsrfCookieAndHeader_Allowed() throws Exception {
    HttpResponse response = restRequest().auth().csrfToken("nonce", "nonce").put();
    assertAccessIsAllowed(response);
  }

  @Test
  public void testRequestWithBasicAuthWithMismatchCsrfCookieAndHeader_Allowed() throws Exception {
    HttpResponse response = restRequest().auth().csrfToken("WRONG", "nonce").put();
    assertAccessIsAllowed(response);
  }

  @Test
  public void testRequestWithBasicAuthWithoutCsrfCookieAndHeader_Allowed() throws Exception {
    HttpResponse response = restRequest().auth().noCsrfToken().put();
    assertAccessIsAllowed(response);
  }

  @Test
  public void testRequestWithRUTWithoutCsrfCookieAndHeader_NotAllowed() throws Exception {
    HttpResponse response = restRequest().header("REMOTE_USER", "admin").noCsrfToken().put();
    assertCrossSiteRequestForgery(response);
  }

  @Test
  public void testRequestWithRUTWithoutCsrfCookie_NotAllowed() throws Exception {
    HttpResponse response = restRequest().header("REMOTE_USER", "admin").csrfToken(null, "nonce").put();
    assertCrossSiteRequestForgery(response);
  }

  @Test
  public void testRequestWithRUTWithoutCsrfHeader_NotAllowed() throws Exception {
    HttpResponse response = restRequest().header("REMOTE_USER", "admin").csrfToken("nonce", null).put();
    assertCrossSiteRequestForgery(response);
  }

  @Test
  public void testRequestWithRUTWithCsrfCookieAndHeader_Allowed() throws Exception {
    HttpResponse response = restRequest().header("REMOTE_USER", "admin").csrfToken("nonce", "nonce").put();
    assertAccessIsAllowed(response);
  }

  @Test
  public void testRequestWithRUTWithCsrfCookieAndHeaderAndDisabledCsrfProtection_Allowed() throws Exception {
    reverseProxyAuthenticationConfigurationDAO.delete();
    tempEntity.newReverseProxyAuthenticationConfiguration(true,
        ReverseProxyAuthenticationConfiguration.DEFAULT_USERNAME_HEADER, true, null);
    getCLMServer().getInstance(ApiReverseProxyAuthenticationConfigurationService.class)
        .applyReverseProxyAuthenticationConfigurationToClients();
    HttpResponse response = restRequest().header("REMOTE_USER", "admin").csrfToken("nonce", "nonce").put();
    assertAccessIsAllowed(response);
  }

  @Test
  public void testRequestWithRUTWithoutCsrfCookieAndHeaderAndDisabledCsrfProtection_Allowed() throws Exception {
    reverseProxyAuthenticationConfigurationDAO.delete();
    tempEntity.newReverseProxyAuthenticationConfiguration(true,
        ReverseProxyAuthenticationConfiguration.DEFAULT_USERNAME_HEADER, true, null);
    getCLMServer().getInstance(ApiReverseProxyAuthenticationConfigurationService.class)
        .applyReverseProxyAuthenticationConfigurationToClients();
    HttpResponse response = restRequest().header("REMOTE_USER", "admin").noCsrfToken().put();
    assertAccessIsAllowed(response);
  }

  @Test
  public void testRequestWithAnonymousWithCsrfCookieAndHeader() throws Exception {
    HttpResponse response = restRequest().csrfToken("nonce", "nonce").put();
    assertLoginFailure(response);
  }

  @Test
  public void testRequestToIndexPageInitializesCsrfCookie() throws Exception {
    HttpResponse response = super.restRequest().header("REMOTE_USER", "admin")
        .noCsrfToken()
        .path("/assets/index.html")
        .get();

    HttpCookie csrfCookie = response.getCookie(AntiCsrfFilter.CSRF_COOKIE_NAME);
    assertThat(csrfCookie).isNotNull();
    assertThat(csrfCookie.getValue()).isNotNull();
  }

  @Test
  public void testRequestWithAnonymousWithoutCsrfCookieAndHeader() throws Exception {
    HttpResponse response = restRequest().noCsrfToken().put();
    assertLoginFailure(response);
  }

  @Test
  public void testRequestUsingHttpInitializesCsrfCookieWithoutSecure() throws Exception {
    HttpResponse response = super.restRequest().noCsrfToken().path("/assets/index.html").get();

    HttpCookie csrfCookie = response.getCookie(AntiCsrfFilter.CSRF_COOKIE_NAME);
    assertThat(csrfCookie).isNotNull();
    assertThat(csrfCookie.getValue()).isNotNull();
    assertThat(csrfCookie.getSecure()).isFalse();
  }

  @Test
  @ManualIqServerInit
  public void testRequestUsingHttpsInitializesCsrfCookieWithSecure() throws Exception {
    try {
      // SSL configuration is now handled by Spring Boot server.ssl.* properties
      // Configure SSL via system properties for tests
      System.setProperty("server.ssl.key-store", SslProperties.SERVER_STORE_FILE.getAbsolutePath());
      System.setProperty("server.ssl.key-store-password", SslProperties.KEY_STORE_PASSWORD);
      System.setProperty("server.ssl.key-store-type", "JKS");
      startIqTestServer(config -> {
        // SSL configuration handled above via system properties
      });
      // Tell the test framework about SSL so client URLs use https://
      getCLMServer().setKeyStore(SslProperties.SERVER_STORE_FILE.getAbsolutePath(), SslProperties.KEY_STORE_PASSWORD);
      HttpResponse response = super.restRequest().noCsrfToken().path("/assets/index.html").get();
      HttpCookie csrfCookie = response.getCookie(AntiCsrfFilter.CSRF_COOKIE_NAME);
      assertThat(csrfCookie).isNotNull();
      assertThat(csrfCookie.getValue()).isNotNull();
      assertThat(csrfCookie.getSecure()).isTrue();
    }
    finally {
      System.clearProperty("server.ssl.key-store");
      System.clearProperty("server.ssl.key-store-password");
      System.clearProperty("server.ssl.key-store-type");
    }
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(REST_PATH).anon();
  }

  private void assertAccessIsAllowed(HttpResponse response) {
    // since we aren't providing a proper app id, we will get an error message back from the DAO, which means the
    // request filter passed and endpoint mapping worked.
    assertThat(response.getBodyText()).isEqualTo("Could not find an application with public ID testApp.");
    assertResponseStatus(404, response); // 404 since we are providing an app id that can't be found.
  }

  private void assertCrossSiteRequestForgery(HttpResponse response) {
    assertThat(response.getBodyText()).isEqualTo("Invalid cross-site request forgery token");
    assertResponseStatus(401, response);
  }

  private void assertLoginFailure(final HttpResponse response) {
    assertThat(response.getBodyText()).isEqualTo(ErrorResponseGenerator.MSG_MISSING_CREDENTIALS);
    assertResponseStatus(401, response);
  }
}
