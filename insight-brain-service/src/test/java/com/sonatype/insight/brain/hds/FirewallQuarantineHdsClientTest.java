/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.version.DefaultVersionService;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.GatewayTimeoutException;
import com.sonatype.insight.error.exception.NotFoundException;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FirewallQuarantineHdsClientTest
    extends AbstractHdsClientTest
{
  @Inject
  private Configuration configuration;

  private ProductLicense mockProductLicense;

  private FirewallQuarantineHdsClient quarantineClient;

  @Override
  protected void initClient() {
    mockProductLicense = mock(ProductLicense.class);
    when(mockProductLicense.isValid()).thenReturn(true);
    when(mockProductLicense.getFingerprint()).thenReturn("license-fingerprint");
    quarantineClient = new FirewallQuarantineHdsClient(new InsightProxy(configuration, passwordHandler),
        mockProductLicense, configuration, new DefaultVersionService(), telemetryId, null);
    client = quarantineClient;
  }

  private FirewallQuarantineHdsClient newClientWithSocketTimeoutSeconds(int seconds) {
    // Property changes only propagate (via Configuration.serverConfigurationChanged) to HdsClient beans
    // managed by DI - quarantineClient was constructed directly with `new` in initClient(), so it never
    // picks up a later property change. Set the property first, then construct a fresh client, mirroring
    // how initClient() itself reads currently-set properties at construction time.
    configurationService.setConfigurationInDatabaseNoAuthz(
        SystemConfigurationProperty.FIREWALL_QUARANTINE_HDS_SOCKET_TIMEOUT_IN_SECONDS, seconds);
    configurationService
        .applyConfigurationToClients(SystemConfigurationProperty.FIREWALL_QUARANTINE_HDS_SOCKET_TIMEOUT_IN_SECONDS);
    return new FirewallQuarantineHdsClient(new InsightProxy(configuration, passwordHandler), mockProductLicense,
        configuration, new DefaultVersionService(), telemetryId, null);
  }

  private FirewallQuarantineHdsClient newClientWithConnectTimeoutSeconds(int seconds) {
    configurationService.setConfigurationInDatabaseNoAuthz(
        SystemConfigurationProperty.FIREWALL_QUARANTINE_HDS_CONNECT_TIMEOUT_IN_SECONDS, seconds);
    configurationService
        .applyConfigurationToClients(SystemConfigurationProperty.FIREWALL_QUARANTINE_HDS_CONNECT_TIMEOUT_IN_SECONDS);
    return new FirewallQuarantineHdsClient(new InsightProxy(configuration, passwordHandler), mockProductLicense,
        configuration, new DefaultVersionService(), telemetryId, null);
  }

  @Test
  public void testValidatePoolSize_throwsOnZero() {
    assertThatThrownBy(() -> FirewallQuarantineHdsClient.validatePoolSize(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("nexus.firewall.hds.quarantine.pool.size must be between 1 and 50")
        .hasMessageContaining("got: 0");
  }

  @Test
  public void testValidatePoolSize_throwsOnNegative() {
    assertThatThrownBy(() -> FirewallQuarantineHdsClient.validatePoolSize(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("got: -1");
  }

  @Test
  public void testValidatePoolSize_throwsAboveMax() {
    assertThatThrownBy(() -> FirewallQuarantineHdsClient.validatePoolSize(51))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("got: 51");
  }

  @Test
  public void testValidatePoolSize_acceptsMinimum() {
    assertThat(FirewallQuarantineHdsClient.validatePoolSize(1)).isEqualTo(1);
  }

  @Test
  public void testValidatePoolSize_acceptsDefault() {
    assertThat(FirewallQuarantineHdsClient.validatePoolSize(20)).isEqualTo(20);
  }

  @Test
  public void testValidatePoolSize_acceptsMaximum() {
    assertThat(FirewallQuarantineHdsClient.validatePoolSize(50)).isEqualTo(50);
  }

  @Test
  public void testSocketTimeout_usesDedicatedTimeoutNotGlobalDefault() throws Exception {
    // Uses the property's own minimum (1s) rather than the 20s default so this test runs in a few
    // seconds instead of ~50s, while still proving a dedicated (not the 180s global) timeout is in effect.
    int socketTimeoutSeconds = FirewallQuarantineHdsClient.MIN_TIMEOUT_SECONDS;
    FirewallQuarantineHdsClient fastTimeoutClient = newClientWithSocketTimeoutSeconds(socketTimeoutSeconds);
    try {
      handler = new HttpServlet()
      {
        @Override
        protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
          try {
            Thread.sleep(Duration.ofSeconds(socketTimeoutSeconds).toMillis() + 1000);
          }
          catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
          response.setStatus(HttpStatus.OK_200);
          response.setContentType("text/plain;charset=UTF-8");
          response.getWriter().println("alive");
        }
      };
      long start = System.currentTimeMillis();

      assertThatThrownBy(
          () -> fastTimeoutClient.get(String.class, "/rest/test", "test-agent", Collections.emptyMap()))
              .isInstanceOf(BadGatewayException.class);

      long elapsed = System.currentTimeMillis() - start;
      long socketTimeoutMillis = Duration.ofSeconds(socketTimeoutSeconds).toMillis();
      // one initial attempt + one retry, each bounded by the dedicated socket timeout - asserting 2x
      // (not 1x) the timeout confirms the retry actually fired, not just that one attempt timed out.
      assertThat(elapsed).isGreaterThanOrEqualTo(2 * socketTimeoutMillis);
      // well under the shared global default of 180s - confirms the dedicated override is in effect,
      // not the global timeout. Generous multiplier since the absolute margin is tiny at this timeout.
      assertThat(elapsed).isLessThan(30 * socketTimeoutMillis + 60_000);
    }
    finally {
      fastTimeoutClient.stop();
    }
  }

  @Test
  public void testConnectTimeout_usesDedicatedTimeoutNotGlobalDefault() throws Exception {
    // 192.0.2.1 is in TEST-NET-1 (RFC 5737), reserved for documentation and guaranteed unroutable on
    // real networks - packets are silently dropped rather than refused, so the connect attempt hangs
    // until the connect timeout fires, exercising the timeout duration itself (unlike a closed local
    // port, which refuses instantly regardless of the configured timeout).
    int connectTimeoutSeconds = FirewallQuarantineHdsClient.MIN_TIMEOUT_SECONDS;
    setHdsUrl("http://192.0.2.1/");
    FirewallQuarantineHdsClient unroutableClient = newClientWithConnectTimeoutSeconds(connectTimeoutSeconds);
    try {
      long start = System.currentTimeMillis();

      assertThatThrownBy(
          () -> unroutableClient.get(String.class, "/rest/test", "test-agent", Collections.emptyMap()))
              .isInstanceOf(GatewayTimeoutException.class);

      long elapsed = System.currentTimeMillis() - start;
      long connectTimeoutMillis = Duration.ofSeconds(connectTimeoutSeconds).toMillis();
      // GatewayTimeoutException (connect failures) is not retried, so exactly one attempt.
      assertThat(elapsed).isGreaterThanOrEqualTo(connectTimeoutMillis);
      // generous multiplier since the absolute margin is tiny at this timeout value.
      assertThat(elapsed).isLessThan(30 * connectTimeoutMillis + 60_000);
    }
    finally {
      unroutableClient.stop();
    }
  }

  @Test
  public void testCircuitBreaker_opensAfterConsecutiveFailuresThenFailsFast() {
    AtomicInteger requestCount = new AtomicInteger();
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response) {
        requestCount.incrementAndGet();
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE_503);
      }
    };

    // 5 consecutive get() failures, each retried exactly once (503 -> BadGatewayException, which is in
    // QUARANTINE_RETRY_CREATOR's retryable predicate) trip the breaker: 10 servlet hits, deterministically.
    // Asserting the exact count (not just >0) proves the retry actually fired, not just that one request
    // per call reached the servlet.
    for (int i = 0; i < 5; i++) {
      assertThatThrownBy(
          () -> quarantineClient.get(String.class, "/rest/test", "test-agent", Collections.emptyMap()))
              .isInstanceOf(BadGatewayException.class);
    }
    int hitsBeforeBreakerOpen = requestCount.get();
    assertThat(hitsBeforeBreakerOpen).isEqualTo(10);

    // Breaker is now open: the next call fails fast without any additional servlet hit.
    assertThatThrownBy(
        () -> quarantineClient.get(String.class, "/rest/test", "test-agent", Collections.emptyMap()))
            .isInstanceOf(GatewayTimeoutException.class)
            .hasMessageContaining("circuit breaker is open");
    assertThat(requestCount.get()).isEqualTo(hitsBeforeBreakerOpen);
  }

  @Test
  public void testCircuitBreaker_opensAfterConsecutiveConnectFailuresThenFailsFast() throws Exception {
    int unusedPort;
    try (ServerSocket probeSocket = new ServerSocket(0)) {
      unusedPort = probeSocket.getLocalPort();
    }
    setHdsUrl("http://localhost:" + unusedPort);
    // setHdsUrl() only reconfigures clients that read the URL at construction time - quarantineClient
    // (built in initClient(), before this URL change) would keep pointing at the shared Jetty server.
    // Build a dedicated client here, after the URL change, so it actually targets the closed port.
    FirewallQuarantineHdsClient unreachableClient = new FirewallQuarantineHdsClient(
        new InsightProxy(configuration, passwordHandler), mockProductLicense, configuration,
        new DefaultVersionService(), telemetryId, null);

    try {
      // 5 consecutive connection-refused failures (GatewayTimeoutException - not retried, since it
      // is not in QUARANTINE_RETRY_CREATOR's retryable predicate) trip the breaker.
      for (int i = 0; i < 5; i++) {
        assertThatThrownBy(
            () -> unreachableClient.get(String.class, "/rest/test", "test-agent", Collections.emptyMap()))
                .isInstanceOf(GatewayTimeoutException.class)
                .hasMessageNotContaining("circuit breaker is open");
      }

      // Breaker is now open: the next call fails fast with the breaker's own message rather than
      // attempting a fresh connection.
      assertThatThrownBy(
          () -> unreachableClient.get(String.class, "/rest/test", "test-agent", Collections.emptyMap()))
              .isInstanceOf(GatewayTimeoutException.class)
              .hasMessageContaining("circuit breaker is open");
    }
    finally {
      unreachableClient.stop();
    }
  }

  @Test
  public void testCircuitBreaker_doesNotOpenOnClientErrors() {
    AtomicInteger requestCount = new AtomicInteger();
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response) {
        requestCount.incrementAndGet();
        response.setStatus(HttpStatus.NOT_FOUND_404);
      }
    };

    // 5 consecutive 404s (NotFoundException - not caught by the get() override's catch clause, so
    // never recorded as a breaker failure, and not retried either since it isn't in
    // QUARANTINE_RETRY_CREATOR's retryable predicate) must not open the breaker.
    for (int i = 0; i < 5; i++) {
      assertThatThrownBy(
          () -> quarantineClient.get(String.class, "/rest/test", "test-agent", Collections.emptyMap()))
              .isInstanceOf(NotFoundException.class);
    }
    assertThat(requestCount.get()).isEqualTo(5);

    // Breaker must still be closed: the 6th call reaches the servlet again - a real 404, not a
    // fail-fast breaker-open GatewayTimeoutException.
    assertThatThrownBy(
        () -> quarantineClient.get(String.class, "/rest/test", "test-agent", Collections.emptyMap()))
            .isInstanceOf(NotFoundException.class);
    assertThat(requestCount.get()).isEqualTo(6);
  }
}
