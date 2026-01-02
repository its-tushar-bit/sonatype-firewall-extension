/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import java.util.Collections;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.networking.SslProperties;

import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.jetty.HttpsConnectorFactory;
import io.dropwizard.core.server.DefaultServerFactory;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

@Category(SlowTest.class)
public class LandingResourceTest
    extends AbstractResourceTest
{
  private static final String BASE_URL = "http://localhost/testbaseurl";

  @Test
  public void testHome() throws Exception {
    HttpResponse response = restRequest().anon().get();
    assertResponseStatus(303, response);
    assertThat(response.getHeader("Location")).startsWith(restRequest().getUrl());
  }

  @Test
  @ManualIqServerInit
  public void testHome_NonEmptyContextPath() throws Exception {
    startIqTestServer(
        config -> ((DefaultServerFactory) config.getServerFactory()).setApplicationContextPath("/testContext"));
    assertThat(restRequest().getUrl()).contains("/testContext/");

    HttpResponse response = restRequest().anon().get();
    assertResponseStatus(303, response);
    assertThat(response.getHeader("Location")).startsWith(restRequest().getUrl());
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
    startIqTestServer(config -> {
      if (ssl) {
        HttpsConnectorFactory applicationHttpsConnector = new HttpsConnectorFactory();
        applicationHttpsConnector.setUseForwardedHeaders(true);
        applicationHttpsConnector.setKeyStorePath(SslProperties.SERVER_STORE_FILE.getAbsolutePath());
        applicationHttpsConnector.setKeyStorePassword(SslProperties.KEY_STORE_PASSWORD);
        applicationHttpsConnector.setDisableSniHostCheck(true);
        DefaultServerFactory defaultServerFactory = (DefaultServerFactory) config.getServerFactory();
        applicationHttpsConnector
            .setPort(((HttpConnectorFactory) defaultServerFactory.getApplicationConnectors().get(0)).getPort());
        defaultServerFactory.setApplicationConnectors(Collections.singletonList(applicationHttpsConnector));
      }
    });
    setBaseUrl(BASE_URL, forceBaseUrl);
  }
}
