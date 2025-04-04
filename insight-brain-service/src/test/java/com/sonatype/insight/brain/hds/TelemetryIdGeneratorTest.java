/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.InsightConfig;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.sonatype.insight.brain.hds.TelemetryIdGenerator.TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME;
import static com.sonatype.insight.brain.hds.TelemetryIdGenerator.TELEMETRY_ID_PATTERN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class TelemetryIdGeneratorTest
{
  @Mock
  private InsightConfig mockInsightConfig;

  @Mock
  private SystemConfigurationPropertyDAO mockSystemConfigurationPropertyDAO;

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
    final var corruptedInstanceId = new SystemConfigurationProperty(TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME, "****");
    when(mockSystemConfigurationPropertyDAO.getByName(TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME))
        .thenReturn(corruptedInstanceId);

    // when:
    final var generatedId = TelemetryIdGenerator.generateId(mockInsightConfig, mockSystemConfigurationPropertyDAO);

    // then: the generated ID has been fixed
    assertThat(generatedId).matches(TELEMETRY_ID_PATTERN);
  }

  @Test
  public void testGenerateId_acceptableValueUnchanged() {
    // given: a corrupted instance ID in the database
    final var telemetryHost = "cde78";
    final var validInstanceId =
        new SystemConfigurationProperty(TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME, telemetryHost);
    when(mockSystemConfigurationPropertyDAO.getByName(TELEMETRY_GENERATED_INSTANCE_ID_PROPNAME))
        .thenReturn(validInstanceId);

    // when:
    final var generatedId = TelemetryIdGenerator.generateId(mockInsightConfig, mockSystemConfigurationPropertyDAO);

    // then: the telemetry host is unchanged
    assertThat(generatedId).matches(TELEMETRY_ID_PATTERN)
         .startsWith(telemetryHost + "-");
  }
}
