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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@RunWith(MockitoJUnitRunner.class)
public class IdeComponentDetailsHdsClientTest
    extends AbstractHdsClientTest
{
  @Inject
  private Configuration configuration;

  private ProductLicense mockProductLicense;

  private IdeComponentDetailsHdsClient ideClient;

  private HttpServletRequest mockRequest;

  @Override
  protected void initClient() {
    mockProductLicense = mock(ProductLicense.class);
    when(mockProductLicense.isValid()).thenReturn(true);
    when(mockProductLicense.getFingerprint()).thenReturn("license-fingerprint");
    ideClient = new IdeComponentDetailsHdsClient(new InsightProxy(configuration, passwordHandler),
        mockProductLicense, configuration, new DefaultVersionService(), telemetryId, null);
    client = ideClient;

    mockRequest = mock(HttpServletRequest.class);
    lenient().when(mockRequest.getMethod()).thenReturn("GET");
    lenient().when(mockRequest.getRequestURI()).thenReturn("/rest/test");
    lenient().when(mockRequest.getHeaderNames()).thenReturn(Collections.enumeration(Collections.emptyList()));
  }

  private IdeComponentDetailsHdsClient newClientWithSocketTimeoutSeconds(int seconds) {
    // Property changes only propagate (via Configuration.serverConfigurationChanged) to HdsClient beans
    // managed by DI - ideClient was constructed directly with `new` in initClient(), so it never
    // picks up a later property change. Set the property first, then construct a fresh client, mirroring
    // how initClient() itself reads currently-set properties at construction time.
    configurationService.setConfigurationInDatabaseNoAuthz(
        SystemConfigurationProperty.IDE_COMPONENT_DETAILS_HDS_SOCKET_TIMEOUT_IN_SECONDS, seconds);
    configurationService
        .applyConfigurationToClients(SystemConfigurationProperty.IDE_COMPONENT_DETAILS_HDS_SOCKET_TIMEOUT_IN_SECONDS);
    return new IdeComponentDetailsHdsClient(new InsightProxy(configuration, passwordHandler), mockProductLicense,
        configuration, new DefaultVersionService(), telemetryId, null);
  }

  private IdeComponentDetailsHdsClient newClientWithConnectTimeoutSeconds(int seconds) {
    configurationService.setConfigurationInDatabaseNoAuthz(
        SystemConfigurationProperty.IDE_COMPONENT_DETAILS_HDS_CONNECT_TIMEOUT_IN_SECONDS, seconds);
    configurationService
        .applyConfigurationToClients(SystemConfigurationProperty.IDE_COMPONENT_DETAILS_HDS_CONNECT_TIMEOUT_IN_SECONDS);
    return new IdeComponentDetailsHdsClient(new InsightProxy(configuration, passwordHandler), mockProductLicense,
        configuration, new DefaultVersionService(), telemetryId, null);
  }

  @Test
  public void testValidatePoolSize_throwsOnZero() {
    assertThatThrownBy(() -> IdeComponentDetailsHdsClient.validatePoolSize(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ideComponentDetailsHdsPoolSize")
        .hasMessageContaining("must be between 1 and 50")
        .hasMessageContaining("got: 0");
  }

  @Test
  public void testValidatePoolSize_throwsOnNegative() {
    assertThatThrownBy(() -> IdeComponentDetailsHdsClient.validatePoolSize(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("got: -1");
  }

  @Test
  public void testValidatePoolSize_throwsAboveMax() {
    assertThatThrownBy(() -> IdeComponentDetailsHdsClient.validatePoolSize(51))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("got: 51");
  }

  @Test
  public void testValidatePoolSize_acceptsMinimum() {
    assertThat(IdeComponentDetailsHdsClient.validatePoolSize(1)).isEqualTo(1);
  }

  @Test
  public void testValidatePoolSize_acceptsDefault() {
    assertThat(IdeComponentDetailsHdsClient.validatePoolSize(20)).isEqualTo(20);
  }

  @Test
  public void testValidatePoolSize_acceptsMaximum() {
    assertThat(IdeComponentDetailsHdsClient.validatePoolSize(50)).isEqualTo(50);
  }

  @Test
  public void testSocketTimeout_usesDedicatedTimeoutNotGlobalDefault() throws Exception {
    // Uses the property's own minimum (1s) rather than the 20s default so this test runs in a few
    // seconds instead of ~50s, while still proving a dedicated (not the 180s global) timeout is in effect.
    int socketTimeoutSeconds = IdeComponentDetailsHdsClient.MIN_TIMEOUT_SECONDS;
    IdeComponentDetailsHdsClient fastTimeoutClient = newClientWithSocketTimeoutSeconds(socketTimeoutSeconds);
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
          response.setContentType("application/json");
          response.getWriter().println("{}");
        }
      };
      long start = System.currentTimeMillis();

      assertThatThrownBy(
          () -> fastTimeoutClient.relay(mockRequest, String.class, "/rest/test", Collections.emptyMap()))
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
    int connectTimeoutSeconds = IdeComponentDetailsHdsClient.MIN_TIMEOUT_SECONDS;
    setHdsUrl("http://192.0.2.1/");
    IdeComponentDetailsHdsClient unroutableClient = newClientWithConnectTimeoutSeconds(connectTimeoutSeconds);
    try {
      long start = System.currentTimeMillis();

      assertThatThrownBy(
          () -> unroutableClient.relay(mockRequest, String.class, "/rest/test", Collections.emptyMap()))
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

    // 5 consecutive relay() failures, each retried exactly once (503 -> BadGatewayException, which is in
    // IDE_RETRY_CREATOR's retryable predicate) trip the breaker: 10 servlet hits, deterministically.
    // Asserting the exact count (not just >0) proves the retry actually fired, not just that one request
    // per call reached the servlet.
    for (int i = 0; i < 5; i++) {
      assertThatThrownBy(
          () -> ideClient.relay(mockRequest, String.class, "/rest/test", Collections.emptyMap()))
              .isInstanceOf(BadGatewayException.class);
    }
    int hitsBeforeBreakerOpen = requestCount.get();
    assertThat(hitsBeforeBreakerOpen).isEqualTo(10);

    // Breaker is now open: the next call fails fast without any additional servlet hit.
    assertThatThrownBy(
        () -> ideClient.relay(mockRequest, String.class, "/rest/test", Collections.emptyMap()))
            .isInstanceOf(GatewayTimeoutException.class)
            .hasMessageContaining("temporarily unavailable");
    assertThat(requestCount.get()).isEqualTo(hitsBeforeBreakerOpen);
  }

  @Test
  public void testCircuitBreaker_get_opensAfterConsecutiveFailuresThenFailsFast() {
    // get() (used by ComponentInfoService.getInformationVersionsHds, backing the /allVersions and
    // deprecated /list endpoints) shares the same circuitBreaker instance as relay() - this test
    // confirms get()'s override independently wires recordFailure()/allowRequest() into it, not just
    // that relay()'s override does.
    AtomicInteger requestCount = new AtomicInteger();
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response) {
        requestCount.incrementAndGet();
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE_503);
      }
    };

    for (int i = 0; i < 5; i++) {
      assertThatThrownBy(
          () -> ideClient.get(String.class, "/rest/test", Collections.emptyMap()))
              .isInstanceOf(BadGatewayException.class);
    }
    int hitsBeforeBreakerOpen = requestCount.get();
    assertThat(hitsBeforeBreakerOpen).isEqualTo(10);

    assertThatThrownBy(
        () -> ideClient.get(String.class, "/rest/test", Collections.emptyMap()))
            .isInstanceOf(GatewayTimeoutException.class)
            .hasMessageContaining("temporarily unavailable");
    assertThat(requestCount.get()).isEqualTo(hitsBeforeBreakerOpen);
  }

  @Test
  public void testCircuitBreaker_opensAfterConsecutiveConnectFailuresThenFailsFast() throws Exception {
    int unusedPort;
    try (ServerSocket probeSocket = new ServerSocket(0)) {
      unusedPort = probeSocket.getLocalPort();
    }
    setHdsUrl("http://localhost:" + unusedPort);
    // setHdsUrl() only reconfigures clients that read the URL at construction time - ideClient
    // (built in initClient(), before this URL change) would keep pointing at the shared Jetty server.
    // Build a dedicated client here, after the URL change, so it actually targets the closed port.
    IdeComponentDetailsHdsClient unreachableClient = new IdeComponentDetailsHdsClient(
        new InsightProxy(configuration, passwordHandler), mockProductLicense, configuration,
        new DefaultVersionService(), telemetryId, null);

    try {
      // 5 consecutive connection-refused failures (GatewayTimeoutException - not retried, since it
      // is not in IDE_RETRY_CREATOR's retryable predicate) trip the breaker.
      for (int i = 0; i < 5; i++) {
        assertThatThrownBy(
            () -> unreachableClient.relay(mockRequest, String.class, "/rest/test", Collections.emptyMap()))
                .isInstanceOf(GatewayTimeoutException.class)
                .hasMessageNotContaining("circuit breaker");
      }

      // Breaker is now open: the next call fails fast with the breaker's own message rather than
      // attempting a fresh connection.
      assertThatThrownBy(
          () -> unreachableClient.relay(mockRequest, String.class, "/rest/test", Collections.emptyMap()))
              .isInstanceOf(GatewayTimeoutException.class)
              .hasMessageContaining("temporarily unavailable");
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

    // 5 consecutive 404s (NotFoundException - not caught by the relay() override's catch clause, so
    // never recorded as a breaker failure, and not retried either since it isn't in
    // IDE_RETRY_CREATOR's retryable predicate) must not open the breaker.
    for (int i = 0; i < 5; i++) {
      assertThatThrownBy(
          () -> ideClient.relay(mockRequest, String.class, "/rest/test", Collections.emptyMap()))
              .isInstanceOf(NotFoundException.class);
    }
    assertThat(requestCount.get()).isEqualTo(5);

    // Breaker must still be closed: the 6th call reaches the servlet again - a real 404, not a
    // fail-fast breaker-open GatewayTimeoutException.
    assertThatThrownBy(
        () -> ideClient.relay(mockRequest, String.class, "/rest/test", Collections.emptyMap()))
            .isInstanceOf(NotFoundException.class);
    assertThat(requestCount.get()).isEqualTo(6);
  }

  @Test
  public void testCircuitBreaker_doesNotOpenOnInternalServerError() {
    AtomicInteger requestCount = new AtomicInteger();
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response) {
        requestCount.incrementAndGet();
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
      }
    };

    // 10 consecutive 500s (InternalServerErrorException - not caught by the relay() override's catch
    // clause, so never recorded as a breaker failure) must not open the breaker. This is a deliberate
    // design decision: HTTP 500 means HDS accepted and started processing the request, so it reflects
    // a data-specific bug rather than HDS-wide degradation. Treating 500 as a breaker failure would
    // block unrelated healthy requests on a narrow, data-dependent bug.
    for (int i = 0; i < 10; i++) {
      assertThatThrownBy(
          () -> ideClient.relay(mockRequest, String.class, "/rest/test", Collections.emptyMap()))
              .isInstanceOf(jakarta.ws.rs.InternalServerErrorException.class);
    }
    assertThat(requestCount.get()).isEqualTo(10);

    // Breaker must still be closed: the 11th call reaches the servlet again - a real 500, not a
    // fail-fast breaker-open GatewayTimeoutException.
    assertThatThrownBy(
        () -> ideClient.relay(mockRequest, String.class, "/rest/test", Collections.emptyMap()))
            .isInstanceOf(jakarta.ws.rs.InternalServerErrorException.class);
    assertThat(requestCount.get()).isEqualTo(11);
  }

  @Test
  public void testCircuitBreaker_closesAfterCooldownAndSuccessfulProbe() throws Exception {
    // IdeComponentDetailsHdsClient's breaker cooldown is a hardcoded 30s (CIRCUIT_BREAKER_COOLDOWN),
    // not injectable - CircuitBreakerTest already covers the cooldown-elapses-then-closes transition
    // in isolation with short durations, so this test's purpose is narrower: prove relay()'s override
    // actually wires recordSuccess()/recordFailure() into *this* client's breaker end-to-end, not just
    // that CircuitBreaker's own logic works. Requires a real 30s+ wait since the cooldown isn't
    // parameterized per-test (see HdsClientTest, PingHdsClientTest for similar real-time-wait tests);
    // this test is slower than the rest of this class's fast tests but does not affect their runtime.
    AtomicInteger requestCount = new AtomicInteger();
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response) {
        requestCount.incrementAndGet();
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE_503);
      }
    };

    // 5 consecutive failures (10 servlet hits, since each is retried once) trip the breaker.
    for (int i = 0; i < 5; i++) {
      assertThatThrownBy(
          () -> ideClient.relay(mockRequest, String.class, "/rest/test", Collections.emptyMap()))
              .isInstanceOf(BadGatewayException.class);
    }
    int hitsBeforeBreakerOpen = requestCount.get();
    assertThat(hitsBeforeBreakerOpen).isEqualTo(10);

    // Breaker is open: fails fast without a servlet hit.
    assertThatThrownBy(
        () -> ideClient.relay(mockRequest, String.class, "/rest/test", Collections.emptyMap()))
            .isInstanceOf(GatewayTimeoutException.class)
            .hasMessageContaining("temporarily unavailable");
    assertThat(requestCount.get()).isEqualTo(hitsBeforeBreakerOpen);

    // Once the servlet starts responding successfully and the cooldown elapses, the next call is
    // allowed through as a probe and, on success, closes the breaker via relay()'s recordSuccess().
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        requestCount.incrementAndGet();
        response.setStatus(HttpStatus.OK_200);
        response.setContentType("application/json");
        response.getWriter().println("{}");
      }
    };
    // 5s margin (not 1s) over the 30s cooldown to absorb CI/GC scheduling jitter - a probe that
    // arrives before the cooldown has actually elapsed would make this test flaky rather than fail.
    Thread.sleep(Duration.ofSeconds(35).toMillis());

    HdsClient.RelayResponse<String> probeResult =
        ideClient.relay(mockRequest, String.class, "/rest/test", Collections.emptyMap());
    assertThat(probeResult.content).isEqualTo("{}\n");
    assertThat(requestCount.get()).isEqualTo(hitsBeforeBreakerOpen + 1);

    // Breaker is now closed: a subsequent failure needs a fresh run of 5 consecutive failures to
    // reopen it, rather than failing fast immediately - confirms recordSuccess() reset the counter.
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response) {
        requestCount.incrementAndGet();
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE_503);
      }
    };
    assertThatThrownBy(
        () -> ideClient.relay(mockRequest, String.class, "/rest/test", Collections.emptyMap()))
            .isInstanceOf(BadGatewayException.class)
            .hasMessageNotContaining("circuit breaker");
  }
}
