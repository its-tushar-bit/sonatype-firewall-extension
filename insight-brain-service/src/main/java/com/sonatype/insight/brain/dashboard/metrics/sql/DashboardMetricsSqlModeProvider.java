/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics.sql;

import java.util.Locale;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_METRICS_SQL_MODE;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_METRICS_SQL_READINESS_GRACE_MINUTES;

@Named
@Singleton
public class DashboardMetricsSqlModeProvider
{
  private static final Logger log = LoggerFactory.getLogger(DashboardMetricsSqlModeProvider.class);

  private static final int DEFAULT_READINESS_GRACE_MINUTES = 60;

  private final SystemConfigurationPropertyDAO propertyDAO;

  @Inject
  public DashboardMetricsSqlModeProvider(final SystemConfigurationPropertyDAO propertyDAO) {
    this.propertyDAO = propertyDAO;
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
}
