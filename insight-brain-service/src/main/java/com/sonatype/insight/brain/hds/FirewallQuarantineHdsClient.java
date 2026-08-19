/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

import com.google.common.annotations.VisibleForTesting;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.utils.CircuitBreaker;
import com.sonatype.insight.brain.utils.Retry;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.client.utils.HttpClientUtils;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.GatewayTimeoutException;

/**
 * Dedicated HTTP client with separate connection pool, timeout, and retry policy for accessing HDS in
 * context of repository firewall quarantine / component-metadata evaluation.
 *
 * <p>
 * The pool size is configurable via the environment variable {@code NXIQ_FIREWALL_QUARANTINE_HDS_POOL_SIZE}
 * or the admin configuration property {@code firewallQuarantineHdsPoolSize}
 * (default: 20, range: 1-50). Increase this value if HDS pool exhaustion is observed under high HA load.
 * Do not exceed 50 without confirming HDS datamart DB pool capacity with the HDS team.
 * Changes to this property take effect only after a server restart.
 * This property is intended for support use only and should not be publicly documented.
 * Customers should not adjust this value without guidance from Sonatype support.
 *
 * <p>
 * Connect/socket timeout are dedicated to this client via the admin configuration properties
 * {@code firewallQuarantineHdsConnectTimeoutInSeconds}/{@code firewallQuarantineHdsSocketTimeoutInSeconds}
 * (defaults: {@value #DEFAULT_CONNECT_TIMEOUT_SECONDS}s/{@value #DEFAULT_SOCKET_TIMEOUT_SECONDS}s, range:
 * {@value #MIN_TIMEOUT_SECONDS}-{@value #MAX_TIMEOUT_SECONDS}s each), independent of the shared global
 * {@code connectTimeoutInSeconds}/{@code socketTimeoutInSeconds} defaults used by every other
 * {@link HdsClient} integration, so a slow or degraded HDS bounds this endpoint's latency without
 * affecting them. Bounded well below the global socket-timeout default (180s) to preserve that intent
 * even when admin-tuned. Changes take effect immediately, no restart required. Retry count is a fixed
 * constant (not configurable): see CLM-39511.
 *
 * <p>
 * Only {@link #get(Retry, Class, String, String, Map, String...)} (the {@code clientUserAgent}-bearing
 * overload, used by the current production call site) is circuit-breaker protected. The sibling
 * {@code get} overloads without a {@code clientUserAgent}, and all inherited {@code post}/{@code put}/
 * {@code relay}/{@code getWithMultimap} methods, dispatch directly to {@link HdsClient}'s internals and
 * bypass the breaker. Add an explicit override here if a future caller needs one of those paths protected.
 *
 * <p>
 * Only {@link BadGatewayException}/{@link GatewayTimeoutException} count as breaker failures -
 * deliberately excludes {@code InternalServerErrorException} (HTTP 500). Unlike 502/503/504, which are
 * gateway-level signals about HDS's overall reachability/health, a 500 means HDS already accepted and
 * started processing the specific request, so it more likely reflects a bug tied to that request's data
 * than HDS-wide degradation; treating it as a breaker failure risks blocking unrelated, healthy requests
 * on a narrow, data-dependent bug. This mirrors the existing retry predicate, which has never retried on
 * 500 either.
 *
 * @since 1.18
 */
@Named
@Singleton
public class FirewallQuarantineHdsClient
    extends HdsClient
{

  public static final int MAX_POOL_SIZE = 50;

  public static final int DEFAULT_POOL_SIZE = 20;

  public static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 10;

  public static final int DEFAULT_SOCKET_TIMEOUT_SECONDS = 20;

  public static final int MIN_TIMEOUT_SECONDS = 1;

  public static final int MAX_TIMEOUT_SECONDS = 60;

  // Mirrors HdsClient.DEFAULT_RETRY_CREATOR's predicate shape (BadGatewayException only, reduced from
  // 4 retries to 1) - a deliberate asymmetry with the circuit breaker's failure predicate below, which
  // also counts GatewayTimeoutException (connection-refused/connect-timeout). A connect failure gets no
  // retry (matches the pre-existing global behavior) but still counts toward the breaker immediately;
  // a 502/503/504 gets 1 retry before counting.
  private static final Function<String, Retry> QUARANTINE_RETRY_CREATOR =
      name -> new Retry(name, 1, null, BadGatewayException.class::isInstance, i -> Duration.ofSeconds(1));

  private static final int CIRCUIT_BREAKER_FAILURE_THRESHOLD = 5;

  private static final Duration CIRCUIT_BREAKER_COOLDOWN = Duration.ofSeconds(30);

  private final CircuitBreaker circuitBreaker =
      new CircuitBreaker("hds-quarantine", CIRCUIT_BREAKER_FAILURE_THRESHOLD, CIRCUIT_BREAKER_COOLDOWN);

  @Inject
  public FirewallQuarantineHdsClient(
      InsightProxy proxy,
      ProductLicense productLicense,
      Configuration configuration,
      VersionService versionService,
      TelemetryId telemetryId,
      CurrentUser currentUser)
  {
    super(proxy, productLicense, configuration, versionService, telemetryId, currentUser,
        validatePoolSize(configuration.getFirewallQuarantineHdsPoolSize()), QUARANTINE_RETRY_CREATOR);
  }

  // Defensive guard for callers outside the normal injection path (e.g. tests, future direct construction).
  // Under normal startup, ConfigurationUtils.getFirewallQuarantineHdsPoolSize() already clamps out-of-range
  // values to the default before this method is reached, so this throw is not reachable in production.
  @VisibleForTesting
  static int validatePoolSize(int poolSize) {
    if (poolSize <= 0 || poolSize > MAX_POOL_SIZE) {
      throw new IllegalArgumentException(
          "nexus.firewall.hds.quarantine.pool.size must be between 1 and " + MAX_POOL_SIZE + ", got: " + poolSize);
    }

    return poolSize;
  }

  @Override
  protected void customizeConfiguration(HttpClientUtils.Configuration configuration) {
    configuration.setConnectTimeout(getConfiguration().getFirewallQuarantineHdsConnectTimeoutInSeconds() * 1000);
    configuration.setSocketTimeout(getConfiguration().getFirewallQuarantineHdsSocketTimeoutInSeconds() * 1000);
  }

  @Override
  public <T> T get(
      Retry retry,
      Class<T> clazz,
      String path,
      String clientUserAgent,
      Map<String, String> queryParams,
      String... uriParams)
  {
    if (!circuitBreaker.allowRequest()) {
      throw new GatewayTimeoutException(
          "Sonatype Data Services component-metadata circuit breaker is open; failing fast.");
    }
    try {
      T result = super.get(retry, clazz, path, clientUserAgent, queryParams, uriParams);
      circuitBreaker.recordSuccess();
      return result;
    }
    catch (BadGatewayException | GatewayTimeoutException e) {
      circuitBreaker.recordFailure();
      throw e;
    }
  }
}
