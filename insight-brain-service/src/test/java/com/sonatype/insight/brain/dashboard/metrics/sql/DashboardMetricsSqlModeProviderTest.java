/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics.sql;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_METRICS_SQL_MODE;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_METRICS_SQL_READINESS_GRACE_MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DashboardMetricsSqlModeProviderTest
{
  @Mock
  private SystemConfigurationPropertyDAO propertyDAO;

  private DashboardMetricsSqlModeProvider underTest;

  @Before
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
  public void missingReadinessGraceDefaultsToSixtyMinutes() {
    assertThat(underTest.readinessGraceMinutes()).isEqualTo(60);
  }

  @Test
  public void negativeReadinessGraceDefaultsToSixtyMinutes() {
    when(propertyDAO.get(DASHBOARD_METRICS_SQL_READINESS_GRACE_MINUTES)).thenReturn("-1");

    assertThat(underTest.readinessGraceMinutes()).isEqualTo(60);
  }
}
