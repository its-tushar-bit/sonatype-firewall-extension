/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import java.util.Map;

import com.sonatype.insight.brain.dataaccess.zscaler.ZScalerMetricsDAO;
import com.sonatype.insight.brain.model.zscaler.ZScalerMetrics;
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
public class ZScalerMetricsTelemetryCollectorTest
{
  @Mock
  private ZScalerMetricsDAO zScalerMetricsDAO;

  @Mock
  private ProductLicense productLicense;

  private ZScalerMetricsTelemetryCollector underTest;

  @BeforeEach
  public void setup() {
    underTest = new ZScalerMetricsTelemetryCollector(zScalerMetricsDAO, productLicense);
  }

  @Test
  public void collectReturnsNullWhenNoFeatureEnabled() {
    when(productLicense.hasFeature(LicensedFeature.FIREWALL)).thenReturn(false);
    lenient().when(zScalerMetricsDAO.get()).thenReturn(new ZScalerMetrics());

    TelemetryData telemetryData = underTest.collectData();

    assertThat(telemetryData).isNull();
  }

  @Test
  public void collectDataReturnsNullWhenNoMetricsAvailable() {
    when(productLicense.hasFeature(LicensedFeature.FIREWALL)).thenReturn(true);
    when(zScalerMetricsDAO.get()).thenReturn(null);

    TelemetryData telemetryData = underTest.collectData();

    assertThat(telemetryData).isNull();
  }

  @Test
  public void collectDataReturnsCorrectTelemetryDataForValidMetrics() {
    when(productLicense.hasFeature(LicensedFeature.FIREWALL)).thenReturn(true);

    ZScalerMetrics metrics = new ZScalerMetrics();
    metrics.setMavenUrlsFromHds(10);
    metrics.setNpmUrlsFromHds(20);
    metrics.setPypiUrlsFromHds(30);
    metrics.setNugetUrlsFromHds(40);
    metrics.setMavenUrlsToZscaler(5);
    metrics.setNpmUrlsToZscaler(15);
    metrics.setPypiUrlsToZscaler(25);
    metrics.setNugetUrlsToZscaler(35);
    when(zScalerMetricsDAO.get()).thenReturn(metrics);

    TelemetryData telemetryData = underTest.collectData();

    Map<String, Object> attributes = telemetryData.getAttributes();
    assertThat(attributes.get("zscaler_total_urls_from_hds")).isEqualTo(100);
    assertThat(attributes.get("zscaler_total_urls_to_zscaler")).isEqualTo(80);
    assertThat(attributes.get("zscaler_maven_urls_from_hds")).isEqualTo(10);
    assertThat(attributes.get("zscaler_npm_urls_from_hds")).isEqualTo(20);
    assertThat(attributes.get("zscaler_pypi_urls_from_hds")).isEqualTo(30);
    assertThat(attributes.get("zscaler_nuget_urls_from_hds")).isEqualTo(40);
    assertThat(attributes.get("zscaler_maven_urls_to_zscaler")).isEqualTo(5);
    assertThat(attributes.get("zscaler_npm_urls_to_zscaler")).isEqualTo(15);
    assertThat(attributes.get("zscaler_pypi_urls_to_zscaler")).isEqualTo(25);
    assertThat(attributes.get("zscaler_nuget_urls_to_zscaler")).isEqualTo(35);
  }

  @Test
  public void isClusterTelemetryReturnsTrue() {
    boolean attributes = underTest.isClusterTelemetry();

    assertThat(attributes).isTrue();
  }
}
