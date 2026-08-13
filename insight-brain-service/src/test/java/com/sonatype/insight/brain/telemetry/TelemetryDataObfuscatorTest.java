/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.service.Configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TelemetryDataObfuscatorTest
{
  @Mock
  private Configuration configuration;

  @InjectMocks
  private TelemetryDataObfuscator telemetryDataObfuscator;

  @Test
  public void testObfuscate() {
    String potentialApplicationId = "potentialApplicationId";
    String obfuscated = telemetryDataObfuscator.obfuscate(potentialApplicationId);
    assertThat(obfuscated).isEqualTo(HdsClientAnalytics.obfuscate(potentialApplicationId));
  }

  @Test
  public void testObfuscateIfAdvancedReportingDisabled_propertyIsEnabled_doesNotObfuscate() {
    when(configuration.getAdvanceReportingInsightsEnabled()).thenReturn(true);

    String potentialApplicationId = "potentialApplicationId";
    String obfuscated = telemetryDataObfuscator.obfuscateIfAdvancedReportingDisabled(potentialApplicationId);
    assertThat(obfuscated).isEqualTo(potentialApplicationId);
  }

  @Test
  public void testObfuscateIfAdvancedReportingDisabled_propertyIsDisabled() {
    when(configuration.getAdvanceReportingInsightsEnabled()).thenReturn(false);

    String potentialApplicationId = "potentialApplicationId";
    String obfuscated = telemetryDataObfuscator.obfuscateIfAdvancedReportingDisabled(potentialApplicationId);
    assertThat(obfuscated).isEqualTo(telemetryDataObfuscator.obfuscate(potentialApplicationId));
  }
}
