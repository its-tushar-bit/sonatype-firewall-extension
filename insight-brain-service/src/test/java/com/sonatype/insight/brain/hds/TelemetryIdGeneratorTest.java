/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import static com.sonatype.insight.brain.hds.TelemetryIdGenerator.TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME;
import static com.sonatype.insight.brain.hds.TelemetryIdGenerator.TELEMETRY_ID_PATTERN;
import static org.assertj.core.api.Assertions.assertThat;

public class TelemetryIdGeneratorTest
    extends AbstractComponentTest
{
  @Inject
  private InsightConfig insightConfig;

  @Before
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testCalculateDerivedId() {
    List<byte[]> hardwareAddresses = new ArrayList<>();
    hardwareAddresses.add("123456789ABC".getBytes(StandardCharsets.UTF_8));
    assertThat(TelemetryIdGenerator.calculateDerivedId("somehost", "7788", hardwareAddresses)).isEqualTo("e7c7e");

    assertThat(TelemetryIdGenerator.calculateDerivedId("otherhost", "7788", hardwareAddresses)).isEqualTo("9ee29");

    assertThat(TelemetryIdGenerator.calculateDerivedId("somehost", "8899", hardwareAddresses)).isEqualTo("17868");

    hardwareAddresses.add("123456789DEF".getBytes(StandardCharsets.UTF_8));
    assertThat(TelemetryIdGenerator.calculateDerivedId("somehost", "7788", hardwareAddresses)).isEqualTo("e1380");
  }

  @Test
  public void testGenerateId_corruptedValue() {
    // given: a corrupted instance ID in the database
    tempEntity.newSystemConfigurationProperty(TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME, "****");

    // when:
    final var generatedId = TelemetryIdGenerator.generateId(insightConfig, systemConfigurationPropertyDAO);

    // then: the generated ID has been fixed
    assertThat(generatedId).matches(TELEMETRY_ID_PATTERN);
  }

  @Test
  public void testGenerateId_acceptableValueUnchanged() {
    // given: a valid instance ID in the database
    final var telemetryHost = "cde78";
    tempEntity.newSystemConfigurationProperty(TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME, telemetryHost);

    // when:
    final var generatedId = TelemetryIdGenerator.generateId(insightConfig, systemConfigurationPropertyDAO);

    // then: the telemetry host is unchanged
    assertThat(generatedId).matches(TELEMETRY_ID_PATTERN)
         .startsWith(telemetryHost + "-");
  }
}
