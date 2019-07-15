/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertiesTelemetryCollectorTest
    extends AbstractComponentTest
{
  @Inject
  private PropertiesTelemetryCollector telemetryCollector;

  @Inject
  private InsightConfig insightConfig;

  @Test
  public void testCollectData_TelemetryPurpose() throws Exception {
    TelemetryData telemetryData = telemetryCollector.collectData();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.CONFIGURATION_PROPERTIES);
  }

  @Test
  public void testCollectData_ReturnsConfiguredReportTimeout() throws Exception {
    int configuredTimeout = 600;
    insightConfig.setReportTimeoutInSeconds(configuredTimeout);
    TelemetryData telemetryData = telemetryCollector.collectData();
    assertThat(telemetryData.getAttributes().get(PropertiesTelemetryCollector.REPORT_TIMEOUT_SECONDS))
        .isEqualTo(configuredTimeout);
  }
}
