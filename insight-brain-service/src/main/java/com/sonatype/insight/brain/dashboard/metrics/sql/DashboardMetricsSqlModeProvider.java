/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics.sql;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_METRICS_SQL_MODE;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_METRICS_SQL_READINESS_GRACE_MINUTES;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_METRICS_SQL_SHADOW_SAMPLE_DENOMINATOR;

@Named
@Singleton
public class DashboardMetricsSqlModeProvider
{
  private static final Logger log = LoggerFactory.getLogger(DashboardMetricsSqlModeProvider.class);

  private static final int DEFAULT_SAMPLE_DENOMINATOR = 20;

  private static final int DEFAULT_READINESS_GRACE_MINUTES = 60;

  private static final Duration DENOMINATOR_CACHE_TTL = Duration.ofSeconds(5);

  private final SystemConfigurationPropertyDAO propertyDAO;

  private final Clock clock;

  private volatile CachedDenominator cachedDenominator;

  private final AtomicBoolean fullDualRunDenominatorWarned = new AtomicBoolean();

  @Inject
  public DashboardMetricsSqlModeProvider(final SystemConfigurationPropertyDAO propertyDAO) {
    this(propertyDAO, Clock.systemUTC());
  }

  DashboardMetricsSqlModeProvider(final SystemConfigurationPropertyDAO propertyDAO, final Clock clock) {
    this.propertyDAO = propertyDAO;
    this.clock = clock;
  }

  public DashboardMetricsSqlMode configuredMode() {
    String raw = propertyDAO.get(DASHBOARD_METRICS_SQL_MODE);
    if (raw == null || raw.isBlank()) {
      return DashboardMetricsSqlMode.OFF;
    }

    try {
      return DashboardMetricsSqlMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }
    catch (IllegalArgumentException e) {
      log.warn("Invalid {} value; using OFF", DASHBOARD_METRICS_SQL_MODE);
      return DashboardMetricsSqlMode.OFF;
    }
  }

  public OptionalInt shadowSampleDenominator() {
    Instant now = clock.instant();
    CachedDenominator cached = cachedDenominator;
    if (cached != null && now.isBefore(cached.expiresAt())) {
      return cached.value();
    }

    OptionalInt value = loadShadowSampleDenominator();
    cachedDenominator = new CachedDenominator(value, now.plus(DENOMINATOR_CACHE_TTL));
    return value;
  }

  private OptionalInt loadShadowSampleDenominator() {
    String raw = propertyDAO.get(DASHBOARD_METRICS_SQL_SHADOW_SAMPLE_DENOMINATOR);
    int denominator = parseIntOrDefault(
        raw, DEFAULT_SAMPLE_DENOMINATOR, DASHBOARD_METRICS_SQL_SHADOW_SAMPLE_DENOMINATOR);
    if (denominator < 1) {
      fullDualRunDenominatorWarned.set(false);
      log.warn("{} must be at least 1; shadow sampling is disabled",
          DASHBOARD_METRICS_SQL_SHADOW_SAMPLE_DENOMINATOR);
      return OptionalInt.empty();
    }
    if (denominator == 1) {
      // Emit once per transition into denom=1; reloading every cache TTL must not spam WARN.
      if (fullDualRunDenominatorWarned.compareAndSet(false, true)) {
        log.warn(
            "{} is 1: every SHADOW request runs a full duplicate SQL comparison load. "
                + "Use this only for intentional staging validation; prefer >= 20 in production.",
            DASHBOARD_METRICS_SQL_SHADOW_SAMPLE_DENOMINATOR);
      }
    }
    else {
      fullDualRunDenominatorWarned.set(false);
    }
    return OptionalInt.of(denominator);
  }

  public int readinessGraceMinutes() {
    int graceMinutes = parseIntOrDefault(
        propertyDAO.get(DASHBOARD_METRICS_SQL_READINESS_GRACE_MINUTES),
        DEFAULT_READINESS_GRACE_MINUTES,
        DASHBOARD_METRICS_SQL_READINESS_GRACE_MINUTES);
    if (graceMinutes < 0) {
      log.warn("{} must not be negative; using {}",
          DASHBOARD_METRICS_SQL_READINESS_GRACE_MINUTES,
          DEFAULT_READINESS_GRACE_MINUTES);
      return DEFAULT_READINESS_GRACE_MINUTES;
    }
    return graceMinutes;
  }

  private int parseIntOrDefault(final String raw, final int defaultValue, final String propertyName) {
    if (raw == null || raw.isBlank()) {
      return defaultValue;
    }

    try {
      return Integer.parseInt(raw.trim());
    }
    catch (NumberFormatException e) {
      log.warn("Invalid {} value; using {}", propertyName, defaultValue);
      return defaultValue;
    }
  }

  private record CachedDenominator(OptionalInt value, Instant expiresAt)
  {
  }
}
