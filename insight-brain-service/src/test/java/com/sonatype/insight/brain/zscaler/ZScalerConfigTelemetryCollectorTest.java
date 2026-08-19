/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import java.util.Map;

import com.sonatype.insight.brain.dataaccess.configuration.ZScalerConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.ZScalerConfiguration;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.telemetry.model.TelemetryData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ZScalerConfigTelemetryCollectorTest
{
  @Mock
  private ZScalerConfigurationDAO zScalerConfigurationDAO;

  @Mock
  private ProductLicense productLicense;

  private ZScalerConfigTelemetryCollector underTest;

  @BeforeEach
  public void setup() {
    underTest = new ZScalerConfigTelemetryCollector(zScalerConfigurationDAO, productLicense);
  }

  @Test
  public void collectDataReturnsNullWhenFeatureIsNotEnabled() {
    when(productLicense.hasFeature(LicensedFeature.FIREWALL)).thenReturn(false);
    lenient().when(zScalerConfigurationDAO.get()).thenReturn(new ZScalerConfiguration());

    TelemetryData telemetryData = underTest.collectData();

    assertThat(telemetryData).isNull();
  }

  @Test
  public void collectDataReturnsNullWhenConfigurationIsNull() {
    when(productLicense.hasFeature(LicensedFeature.FIREWALL)).thenReturn(true);
    when(zScalerConfigurationDAO.get()).thenReturn(null);

    TelemetryData telemetryData = underTest.collectData();

    assertThat(telemetryData).isNull();
  }

  @Test
  public void collectDataReturnsTelemetryDataWithValidConfiguration() {
    when(productLicense.hasFeature(LicensedFeature.FIREWALL)).thenReturn(true);

    ZScalerConfiguration configuration = new ZScalerConfiguration();
    configuration.setHostname("test-host");
    when(zScalerConfigurationDAO.get()).thenReturn(configuration);

    TelemetryData telemetryData = underTest.collectData();

    Map<String, Object> attributes = telemetryData.getAttributes();
    assertThat(telemetryData).isNotNull();
    assertThat(attributes.get("zscaler_host")).isEqualTo("test-host");
    assertThat(attributes.get("zscaler_eula_agreed")).isEqualTo(true);
  }

  @Test
  public void isClusterTelemetryAlwaysReturnsTrue() {
    boolean result = underTest.isClusterTelemetry();

    assertThat(result).isTrue();
  }
}
