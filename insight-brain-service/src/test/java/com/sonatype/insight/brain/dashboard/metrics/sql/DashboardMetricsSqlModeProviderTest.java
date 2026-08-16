/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics.sql;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_METRICS_SQL_MODE;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_METRICS_SQL_READINESS_GRACE_MINUTES;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_METRICS_SQL_SHADOW_SAMPLE_DENOMINATOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DashboardMetricsSqlModeProviderTest
{
  @RegisterExtension
  public LogOutput logOutput = new LogOutput(DashboardMetricsSqlModeProvider.class);

  @Mock
  private SystemConfigurationPropertyDAO propertyDAO;

  private DashboardMetricsSqlModeProvider underTest;

  @BeforeEach
  public void setUp() {
    underTest = new DashboardMetricsSqlModeProvider(propertyDAO);
  }

  @Test
  public void missingModeDefaultsToOff() {
    assertThat(underTest.configuredMode()).isEqualTo(DashboardMetricsSqlMode.OFF);
  }

  @Test
  public void parsesOffShadowAndOnCaseInsensitively() {
    when(propertyDAO.get(DASHBOARD_METRICS_SQL_MODE)).thenReturn("off", "ShAdOw", "ON");

    assertThat(underTest.configuredMode()).isEqualTo(DashboardMetricsSqlMode.OFF);
    assertThat(underTest.configuredMode()).isEqualTo(DashboardMetricsSqlMode.SHADOW);
    assertThat(underTest.configuredMode()).isEqualTo(DashboardMetricsSqlMode.ON);
  }

  @Test
  public void invalidModeFailsSafeToOff() {
    when(propertyDAO.get(DASHBOARD_METRICS_SQL_MODE)).thenReturn("invalid");

    assertThat(underTest.configuredMode()).isEqualTo(DashboardMetricsSqlMode.OFF);
  }

  @Test
  public void missingSampleDenominatorDefaultsToTwenty() {
    assertThat(underTest.shadowSampleDenominator()).hasValue(20);
  }

  @Test
  public void denominatorBelowOneDisablesSampling() {
    when(propertyDAO.get(DASHBOARD_METRICS_SQL_SHADOW_SAMPLE_DENOMINATOR)).thenReturn("0");

    assertThat(underTest.shadowSampleDenominator()).isEmpty();
  }

  @Test
  public void denominatorOfOneWarnsAboutFullDuplicateSqlLoad() {
    when(propertyDAO.get(DASHBOARD_METRICS_SQL_SHADOW_SAMPLE_DENOMINATOR)).thenReturn("1");

    assertThat(underTest.shadowSampleDenominator()).hasValue(1);
    assertThat(logOutput.getWarnMessages(DashboardMetricsSqlModeProvider.class.getName()))
        .anyMatch(message -> message.contains("full duplicate SQL comparison load"));
  }

  @Test
  public void denominatorOfOneWarnsOnlyOnceAcrossCacheReloads() {
    AtomicReference<Instant> now = new AtomicReference<>(Instant.EPOCH);
    Clock clock = new Clock()
    {
      @Override
      public ZoneId getZone() {
        return ZoneOffset.UTC;
      }

      @Override
      public Clock withZone(final ZoneId zone) {
        return this;
      }

      @Override
      public Instant instant() {
        return now.get();
      }
    };
    underTest = new DashboardMetricsSqlModeProvider(propertyDAO, clock);
    when(propertyDAO.get(DASHBOARD_METRICS_SQL_SHADOW_SAMPLE_DENOMINATOR)).thenReturn("1");

    assertThat(underTest.shadowSampleDenominator()).hasValue(1);
    now.set(Instant.EPOCH.plusSeconds(10));
    assertThat(underTest.shadowSampleDenominator()).hasValue(1);

    assertThat(logOutput.getWarnMessages(DashboardMetricsSqlModeProvider.class.getName())
        .stream()
        .filter(message -> message.contains("full duplicate SQL comparison load"))
        .count()).isEqualTo(1);
    verify(propertyDAO, times(2)).get(DASHBOARD_METRICS_SQL_SHADOW_SAMPLE_DENOMINATOR);
  }

  @Test
  public void denominatorReadsAreCachedAcrossRequestBursts() {
    Clock clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
    underTest = new DashboardMetricsSqlModeProvider(propertyDAO, clock);
    when(propertyDAO.get(DASHBOARD_METRICS_SQL_SHADOW_SAMPLE_DENOMINATOR)).thenReturn("20");

    assertThat(underTest.shadowSampleDenominator()).hasValue(20);
    assertThat(underTest.shadowSampleDenominator()).hasValue(20);

    verify(propertyDAO, times(1)).get(DASHBOARD_METRICS_SQL_SHADOW_SAMPLE_DENOMINATOR);
  }

  @Test
  public void missingReadinessGraceDefaultsToSixtyMinutes() {
    assertThat(underTest.readinessGraceMinutes()).isEqualTo(60);
  }

  @Test
  public void negativeReadinessGraceDefaultsToSixtyMinutes() {
    when(propertyDAO.get(DASHBOARD_METRICS_SQL_READINESS_GRACE_MINUTES)).thenReturn("-1");

    assertThat(underTest.readinessGraceMinutes()).isEqualTo(60);
  }
}
