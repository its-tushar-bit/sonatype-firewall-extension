/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global.catalog;

import java.time.Duration;
import java.util.function.Function;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.utils.Retry;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.client.utils.HttpClientUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Multimap;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dedicated HDS transport for the Global Search catalog leg, shared by the results-endpoint catalog
 * client (and, later, the suggest-endpoint catalog client). Sibling of
 * {@link com.sonatype.insight.brain.hds.FirewallQuarantineHdsClient}, following the same
 * own-connection-pool precedent that protects unrelated HDS callers when one path saturates.
 *
 * <h3>Behaviour deltas from the base {@link HdsClient}</h3>
 *
 * <ul>
 * <li><b>Own pool.</b> Default {@value #DEFAULT_POOL_SIZE} connections, overridable via
 * {@value #NXIQ_GLOBAL_SEARCH_CATALOG_HDS_POOL_SIZE_ENV_VAR} (range 1-{@value #MAX_POOL_SIZE}).
 * Out-of-range values fall back to the default.</li>
 * <li><b>150 ms timeout.</b> Connect and socket timeouts both pinned to
 * {@value #CATALOG_SOCKET_TIMEOUT_MILLIS} ms. The catalog call is a "best-effort,
 * never-blocks-the-IQ-leg" call; anything slower is treated as a soft failure.</li>
 * <li><b>No retries.</b> Custom {@link Retry} creator with zero retries on every path. The base
 * {@code HdsClient} retries 4× on {@code BadGatewayException}; on the catalog path each retry
 * would burn the per-call deadline and produce a worse user experience than failing fast and
 * degrading the catalog section.</li>
 * </ul>
 *
 * <p>
 * The pool size is read once at injection time. The base {@code HdsClient.serverConfigurationChanged()}
 * hook does not resize this pool — pool resizing requires a restart, matching the
 * {@code FirewallQuarantineHdsClient} precedent for support-only tunables. The env var is intended
 * for support use only; customers should not adjust it without guidance from Sonatype support.
 *
 * <p>
 * <b>Redacted DEBUG logging.</b> This subclass overrides {@link #getWithMultimap} to emit a
 * privacy-preserving DEBUG line on its own logger BEFORE delegating to the base client: the raw
 * {@code query} parameter is replaced with its character count, and the {@code X-CLM-Token}
 * header is masked as {@code ****}. Operators tracing catalog calls should raise this class's
 * logger to DEBUG rather than the base {@code com.sonatype.insight.brain.hds.HdsClient} logger,
 * which still emits the raw URI at DEBUG when directly enabled.
 */
@Named
@Singleton
public class GlobalSearchCatalogHdsClient
    extends HdsClient
{
  public static final int DEFAULT_POOL_SIZE = 20;

  public static final int MAX_POOL_SIZE = 50;

  /**
   * Per-call connect and socket timeout in milliseconds. The catalog call is a best-effort leg on a
   * 150 ms budget; anything slower is treated as a soft failure and the catalog section degrades.
   */
  public static final int CATALOG_SOCKET_TIMEOUT_MILLIS = 150;

  public static final String NXIQ_GLOBAL_SEARCH_CATALOG_HDS_POOL_SIZE_ENV_VAR =
      "NXIQ_GLOBAL_SEARCH_CATALOG_HDS_POOL_SIZE";

  private static final Function<String, Retry> noRetryCreator =
      name -> new Retry(name, 0, null, e -> false, i -> Duration.ZERO);

  private static final Logger log = LoggerFactory.getLogger(GlobalSearchCatalogHdsClient.class);

  @Inject
  public GlobalSearchCatalogHdsClient(
      final InsightProxy proxy,
      final ProductLicense productLicense,
      final Configuration configuration,
      final VersionService versionService,
      final TelemetryId telemetryId,
      final CurrentUser currentUser)
  {
    super(proxy, productLicense, configuration, versionService, telemetryId, currentUser,
        resolvePoolSize(System.getenv(NXIQ_GLOBAL_SEARCH_CATALOG_HDS_POOL_SIZE_ENV_VAR)),
        noRetryCreator);
  }

  @Override
  protected void customizeConfiguration(final HttpClientUtils.Configuration configuration) {
    configuration.setConnectTimeout(CATALOG_SOCKET_TIMEOUT_MILLIS);
    configuration.setSocketTimeout(CATALOG_SOCKET_TIMEOUT_MILLIS);
  }

  @Override
  public <T> T getWithMultimap(
      final Class<T> clazz,
      final String path,
      final Multimap<String, String> queryParams,
      final String... uriParams)
  {
    logRedactedRequest(path, queryParams);
    return super.getWithMultimap(clazz, path, queryParams, uriParams);
  }

  /**
   * Emits a redacted DEBUG line about the outgoing HDS call. The raw {@code query} value is
   * replaced by its character count and the token is masked. Any other query parameter is echoed
   * as-is — callers should avoid passing PII through the remaining parameters.
   */
  @VisibleForTesting
  static void logRedactedRequest(final String path, final Multimap<String, String> queryParams) {
    if (!log.isDebugEnabled()) {
      return;
    }
    final StringBuilder redacted = new StringBuilder();
    for (String key : queryParams.keySet()) {
      if ("query".equals(key)) {
        int totalChars = 0;
        for (String v : queryParams.get(key)) {
          totalChars += v == null ? 0 : v.length();
        }
        appendParam(redacted, key, "<" + totalChars + " chars>");
      }
      else {
        for (String v : queryParams.get(key)) {
          appendParam(redacted, key, v);
        }
      }
    }
    log.debug("GET /{}?{} [token=****]", path, redacted);
  }

  private static void appendParam(final StringBuilder sb, final String key, final String value) {
    if (sb.length() > 0) {
      sb.append('&');
    }
    sb.append(key).append('=').append(value);
  }

  /**
   * Resolves the requested pool size from the env-var value. Null, blank, non-numeric, or
   * out-of-range values fall back silently to {@link #DEFAULT_POOL_SIZE}. A valid override emits
   * a single startup WARN so operators can see the pool has been tuned.
   */
  @VisibleForTesting
  static int resolvePoolSize(final String envValue) {
    if (envValue == null || envValue.isBlank()) {
      return DEFAULT_POOL_SIZE;
    }
    final int parsed;
    try {
      parsed = Integer.parseInt(envValue.trim());
    }
    catch (NumberFormatException nfe) {
      return DEFAULT_POOL_SIZE;
    }
    if (parsed <= 0 || parsed > MAX_POOL_SIZE) {
      return DEFAULT_POOL_SIZE;
    }
    if (parsed != DEFAULT_POOL_SIZE) {
      log.warn("{} overridden from default {} to {}",
          NXIQ_GLOBAL_SEARCH_CATALOG_HDS_POOL_SIZE_ENV_VAR, DEFAULT_POOL_SIZE, parsed);
    }
    return parsed;
  }
}
