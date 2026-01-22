/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Map;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RuntimeEnvironmentTelemetryCollectorTest
    extends AbstractComponentTest
{
  @Inject
  private RuntimeEnvironmentTelemetryCollector telemetryCollector;

  @Test
  public void testCollectData() {
    TelemetryData telemetryData = telemetryCollector.collectData();

    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.RUNTIME_ENVIRONMENT);

    Map<String, Object> attributes = telemetryCollector.collectData().getAttributes();
    assertThat(attributes.get(RuntimeEnvironmentTelemetryCollector.JVM_NAME))
        .isEqualTo(System.getProperty("java.vm.name"));
    assertThat(attributes.get(RuntimeEnvironmentTelemetryCollector.JVM_VERSION))
        .isEqualTo(System.getProperty("java.version"));
    assertThat(attributes.get(RuntimeEnvironmentTelemetryCollector.JVM_VENDOR))
        .isEqualTo(System.getProperty("java.vendor"));
    assertThat(attributes.get(RuntimeEnvironmentTelemetryCollector.OS_NAME)).isEqualTo(System.getProperty("os.name"));
    assertThat(attributes.get(RuntimeEnvironmentTelemetryCollector.OS_VERSION))
        .isEqualTo(System.getProperty("os.version"));
    assertThat(attributes.get(RuntimeEnvironmentTelemetryCollector.OS_ARCHITECTURE))
        .isEqualTo(System.getProperty("os.arch"));
  }

  @Test
  public void testIsClusterTelemetry() {
    assertThat(telemetryCollector.isClusterTelemetry()).isFalse();
  }
}
