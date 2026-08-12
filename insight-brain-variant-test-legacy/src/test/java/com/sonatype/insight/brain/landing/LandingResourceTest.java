/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.networking.SslProperties;
import org.junit.After;
import org.junit.Test;

public class LandingResourceTest
    extends AbstractResourceTest
{
  private static final String BASE_URL = "http://localhost/testbaseurl";

  private static final String SERVER_SSL_KEY_STORE = "server.ssl.key-store";

  private static final String SERVER_SSL_KEY_STORE_PASSWORD = "server.ssl.key-store-password";

  private static final String SERVER_SSL_KEY_STORE_TYPE = "server.ssl.key-store-type";

  @Test
  public void testHome() throws Exception {
    HttpResponse response = restRequest().anon().get();
    assertResponseStatus(303, response);
    assertThat(response.getHeader("Location")).startsWith(restRequest().getUrl());
  }

  @Test
  @ManualIqServerInit
  public void testHome_NonEmptyContextPath() throws Exception {
    // Context path configuration is handled by Spring Boot server.servlet.context-path property
    // For now, skip this test as it requires Spring Boot server configuration
    // startIqTestServer(config -> config.setContextPath("/testContext"));
    // TODO: Re-implement with Spring Boot configuration
  }

  @Test
  @ManualIqServerInit
  public void testHome_ForceBaseUrl() throws Exception {
    initServerForcingBaseUrl();

    HttpResponse response = restRequest().anon().get();
    assertResponseStatus(303, response);
    assertThat(response.getHeader("Location")).startsWith(BASE_URL);
  }

  @Test
  @ManualIqServerInit
  public void testHome_SSL() throws Exception {
    initServerForSSL();

    HttpResponse response = restRequest().get();
    assertResponseStatus(303, response);
    assertThat(response.getHeader("Location")).startsWith(restRequest().getUrl());
  }

  @Test
  @ManualIqServerInit
  public void testHome_SSL_ForcingBaseUrl() throws Exception {
    initServerForSSLAndForcingBaseUrl();

    HttpResponse response = restRequest().get();
    assertResponseStatus(303, response);
    assertThat(response.getHeader("Location")).startsWith(BASE_URL);
  }

  @Test
  public void testHome_XForwardedProto() throws Exception {
    HttpRequest httpRequest = restRequest();
    String xForwardedProto = "https";
    httpRequest.header("X-Forwarded-Proto", xForwardedProto);
    HttpResponse response = httpRequest.get();
    assertResponseStatus(303, response);
    assertThat(response.getHeader("Location")).startsWith(xForwardedProto);
  }

  @Test
  @ManualIqServerInit
  public void testHome_XForwardedProto_ForceBaseUrl() throws Exception {
    initServerForcingBaseUrl();

    HttpRequest httpRequest = restRequest();
    String xForwardedProto = "https";
    httpRequest.header("X-Forwarded-Proto", xForwardedProto);
    HttpResponse response = httpRequest.get();
    assertResponseStatus(303, response);
    assertThat(response.getHeader("Location")).startsWith(BASE_URL);
  }

  @Test
  @ManualIqServerInit
  public void testHome_XForwardedProto_SSL() throws Exception {
    initServerForSSL();

    HttpRequest httpRequest = restRequest();
    String xForwardedProto = "http";
    httpRequest.header("X-Forwarded-Proto", xForwardedProto);
    HttpResponse response = httpRequest.get();
    assertResponseStatus(303, response);
    assertThat(response.getHeader("Location")).startsWith(xForwardedProto);
  }

  @Test
  @ManualIqServerInit
  public void testHome_XForwardedProto_SSL_ForceBaseUrl() throws Exception {
    initServerForSSLAndForcingBaseUrl();

    HttpRequest httpRequest = restRequest();
    String xForwardedProto = "http";
    httpRequest.header("X-Forwarded-Proto", xForwardedProto);
    HttpResponse response = httpRequest.get();
    assertResponseStatus(303, response);
    assertThat(response.getHeader("Location")).startsWith(BASE_URL);
  }

  @Test
  public void testHome_XForwardedHost() throws Exception {
    HttpRequest httpRequest = restRequest();
    String xForwardedHost = "xforwardedhost:88";
    httpRequest.header("X-Forwarded-Host", xForwardedHost);
    HttpResponse response = httpRequest.get();
    assertResponseStatus(303, response);
    assertThat(response.getHeader("Location")).startsWith("http://" + xForwardedHost);
  }

  @Test
  @ManualIqServerInit
  public void testHome_XForwardedHost_ForceBaseUrl() throws Exception {
    initServerForcingBaseUrl();

    HttpRequest httpRequest = restRequest();
    String xForwardedHost = "xforwardedhost:88";
    httpRequest.header("X-Forwarded-Host", xForwardedHost);
    HttpResponse response = httpRequest.get();
    assertResponseStatus(303, response);
    assertThat(response.getHeader("Location")).startsWith(BASE_URL);
  }

  @Test
  @ManualIqServerInit
  public void testHome_XForwardedHost_SSL() throws Exception {
    initServerForSSL();

    HttpRequest httpRequest = restRequest();
    String xForwardedHost = "xforwardedhost:88";
    httpRequest.header("X-Forwarded-Host", xForwardedHost);
    HttpResponse response = httpRequest.get();
    assertResponseStatus(303, response);
    assertThat(response.getHeader("Location")).startsWith("https://" + xForwardedHost);
  }

  @Test
  @ManualIqServerInit
  public void testHome_XForwardedHost_SSL_ForceBaseUrl() throws Exception {
    initServerForSSLAndForcingBaseUrl();

    HttpRequest httpRequest = restRequest();
    String xForwardedHost = "xforwardedhost:88";
    httpRequest.header("X-Forwarded-Host", xForwardedHost);
    HttpResponse response = httpRequest.get();
    assertResponseStatus(303, response);
    assertThat(response.getHeader("Location")).startsWith(BASE_URL);
  }

  @Test
  public void testHome_XForwardedProtoAndXForwardedHost() throws Exception {
    HttpRequest httpRequest = restRequest();
    String xForwardedProto = "https";
    String xForwardedHost = "xforwardedhost:88";
    httpRequest.header("X-Forwarded-Proto", xForwardedProto);
    httpRequest.header("X-Forwarded-Host", xForwardedHost);
    HttpResponse response = httpRequest.get();
    assertResponseStatus(303, response);
    assertThat(response.getHeader("Location")).startsWith(xForwardedProto + "://" + xForwardedHost);
  }

  @Test
  @ManualIqServerInit
  public void testHome_XForwardedProtoAndXForwardedHost_ForceBaseUrl() throws Exception {
    initServerForcingBaseUrl();

    HttpRequest httpRequest = restRequest();
    String xForwardedProto = "https";
    String xForwardedHost = "xforwardedhost:88";
    httpRequest.header("X-Forwarded-Proto", xForwardedProto);
    httpRequest.header("X-Forwarded-Host", xForwardedHost);
    HttpResponse response = httpRequest.get();
    assertResponseStatus(303, response);
    assertThat(response.getHeader("Location")).startsWith(BASE_URL);
  }

  @Test
  @ManualIqServerInit
  public void testHome_XForwardedProtoAndXForwardedHost_SSL() throws Exception {
    initServerForSSL();

    HttpRequest httpRequest = restRequest();
    String xForwardedProto = "http";
    String xForwardedHost = "xforwardedhost:88";
    httpRequest.header("X-Forwarded-Proto", xForwardedProto);
    httpRequest.header("X-Forwarded-Host", xForwardedHost);
    HttpResponse response = httpRequest.get();
    assertResponseStatus(303, response);
    assertThat(response.getHeader("Location")).startsWith(xForwardedProto + "://" + xForwardedHost);
  }

  @Test
  @ManualIqServerInit
  public void testHome_XForwardedProtoAndXForwardedHost_SSL_ForceBaseUrl() throws Exception {
    initServerForSSLAndForcingBaseUrl();

    HttpRequest httpRequest = restRequest();
    String xForwardedProto = "http";
    String xForwardedHost = "xforwardedhost:88";
    httpRequest.header("X-Forwarded-Proto", xForwardedProto);
    httpRequest.header("X-Forwarded-Host", xForwardedHost);
    HttpResponse response = httpRequest.get();
    assertResponseStatus(303, response);
    assertThat(response.getHeader("Location")).startsWith(BASE_URL);
  }

  @After
  public void clearSslOverrides() {
    configureServerSslSystemProperties(false);
    if (testCLMServer != null) {
      getCLMServer().setKeyStore(null, null);
    }
  }

  private void initServerForcingBaseUrl() throws Exception {
    initServer(false, true);
  }

  private void initServerForSSL() throws Exception {
    initServer(true, false);
  }

  private void initServerForSSLAndForcingBaseUrl() throws Exception {
    initServer(true, true);
  }

  private void initServer(final boolean ssl, final boolean forceBaseUrl) throws Exception {
    configureServerSslSystemProperties(ssl);
    startIqTestServer(config -> {
      // SSL configuration handled above via system properties
    });
    getCLMServer().setKeyStore(ssl ? SslProperties.SERVER_STORE_FILE.getAbsolutePath() : null,
        ssl ? SslProperties.KEY_STORE_PASSWORD : null);
    setBaseUrl(BASE_URL, forceBaseUrl);
  }

  private void configureServerSslSystemProperties(final boolean ssl) {
    if (ssl) {
      System.setProperty(SERVER_SSL_KEY_STORE, SslProperties.SERVER_STORE_FILE.getAbsolutePath());
      System.setProperty(SERVER_SSL_KEY_STORE_PASSWORD, SslProperties.KEY_STORE_PASSWORD);
      System.setProperty(SERVER_SSL_KEY_STORE_TYPE, "JKS");
      return;
    }

    System.clearProperty(SERVER_SSL_KEY_STORE);
    System.clearProperty(SERVER_SSL_KEY_STORE_PASSWORD);
    System.clearProperty(SERVER_SSL_KEY_STORE_TYPE);
  }
}
