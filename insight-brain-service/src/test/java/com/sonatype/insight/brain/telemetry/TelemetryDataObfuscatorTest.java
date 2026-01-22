/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Collections;
import java.util.Map;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.testing.BrainInjectedTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TelemetryDataObfuscatorTest
    extends BrainInjectedTest
{
  @Inject
  private TelemetryDataObfuscator telemetryDataObfuscator;

  @Inject
  private Configuration configuration;

  @Inject
  private ApiConfigurationService configurationService;

  @Test
  public void testObfuscate() {
    String potentialApplicationId = "potentialApplicationId";
    String obfuscated = telemetryDataObfuscator.obfuscate(potentialApplicationId);
    assertThat(obfuscated).isEqualTo(HdsClientAnalytics.obfuscate(potentialApplicationId));
  }

  @Test
  public void testObfuscateIfAdvancedReportingDisabled_propertyIsEnabled_doesNotObfuscate() {
    Map<String, Object> properties =
        Collections.singletonMap(SystemConfigurationProperty.ADVANCED_REPORTING_INSIGHTS_ENABLED, true);
    configurationService.setConfigurationInDatabaseNoAuthz(properties);
    configuration.configurationChanged(properties.keySet());

    String potentialApplicationId = "potentialApplicationId";
    String obfuscated = telemetryDataObfuscator.obfuscateIfAdvancedReportingDisabled(potentialApplicationId);
    assertThat(obfuscated).isEqualTo(potentialApplicationId);
  }

  @Test
  public void testObfuscateIfAdvancedReportingDisabled_propertyIsDisabled() {
    Map<String, Object> properties =
        Collections.singletonMap(SystemConfigurationProperty.ADVANCED_REPORTING_INSIGHTS_ENABLED, false);
    configurationService.setConfigurationInDatabaseNoAuthz(properties);
    configuration.configurationChanged(properties.keySet());

    String potentialApplicationId = "potentialApplicationId";
    String obfuscated = telemetryDataObfuscator.obfuscateIfAdvancedReportingDisabled(potentialApplicationId);
    assertThat(obfuscated).isEqualTo(telemetryDataObfuscator.obfuscate(potentialApplicationId));
  }
}
