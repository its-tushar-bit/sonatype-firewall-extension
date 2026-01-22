/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Test;

import static com.sonatype.insight.brain.telemetry.RealmTelemetryCollector.SAML_CONFIGURED;
import static org.assertj.core.api.Assertions.assertThat;

public class RealmTelemetryCollectorTest
    extends AbstractComponentTest
{
  @Inject
  private RealmTelemetryCollector telemetryCollector;

  @Test
  public void testIsClusterTelemetry() {
    assertThat(telemetryCollector.isClusterTelemetry()).isTrue();
  }

  @Test
  public void testCollectData_SamlNotConfigured() {
    TelemetryData telemetryData = telemetryCollector.collectData();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.REALM);
    assertThat(telemetryData.getAttributes()).containsEntry(SAML_CONFIGURED, "false");
  }

  @Test
  public void testCollectData_SamlConfigured() {
    samlConfigurationService.insert(tempEntity.newSamlConfiguration());
    TelemetryData telemetryData = telemetryCollector.collectData();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.REALM);
    assertThat(telemetryData.getAttributes()).containsEntry(SAML_CONFIGURED, "true");
  }
}
