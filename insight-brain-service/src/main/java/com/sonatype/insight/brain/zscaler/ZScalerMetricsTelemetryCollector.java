/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.zscaler.ZScalerMetricsDAO;
import com.sonatype.insight.brain.model.zscaler.ZScalerMetrics;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.telemetry.TelemetryCollector;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

@Named
@Singleton
public class ZScalerMetricsTelemetryCollector
    implements TelemetryCollector
{
  private static final String TOTAL_URLS_FROM_HDS = "zscaler_total_urls_from_hds";

  private static final String TOTAL_URLS_TO_ZSCALER = "zscaler_total_urls_to_zscaler";

  public static final String UPDATED_AT = "zscaler_updated_at";

  private static final String MAVEN_URLS_FROM_HDS = "zscaler_maven_urls_from_hds";

  public static final String NPM_URLS_FROM_HDS = "zscaler_npm_urls_from_hds";

  public static final String PYPI_URLS_FROM_HDS = "zscaler_pypi_urls_from_hds";

  public static final String NUGET_URLS_FROM_HDS = "zscaler_nuget_urls_from_hds";

  public static final String MAVEN_URLS_TO_ZSCALER = "zscaler_maven_urls_to_zscaler";

  public static final String NPM_URLS_TO_ZSCALER = "zscaler_npm_urls_to_zscaler";

  public static final String PYPI_URLS_TO_ZSCALER = "zscaler_pypi_urls_to_zscaler";

  public static final String NUGET_URLS_TO_ZSCALER = "zscaler_nuget_urls_to_zscaler";

  private final ZScalerMetricsDAO zScalerMetricsDAO;

  private final ProductLicense productLicense;

  @Inject
  public ZScalerMetricsTelemetryCollector(
      final ZScalerMetricsDAO zScalerMetricsDAO,
      final ProductLicense productLicense)
  {
    this.zScalerMetricsDAO = zScalerMetricsDAO;
    this.productLicense = productLicense;
  }

  @Override
  public TelemetryData collectData() {
    if (!productLicense.hasFeature(LicensedFeature.FIREWALL)) {
      return null;
    }

    ZScalerMetrics zScalerMetrics = zScalerMetricsDAO.get();
    if (zScalerMetrics == null) {
      return null;
    }

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.ZSCALER_METRICS);
    int totalFromHds = zScalerMetrics.getMavenUrlsFromHds() + zScalerMetrics.getNpmUrlsFromHds() +
        zScalerMetrics.getPypiUrlsFromHds() + zScalerMetrics.getNugetUrlsFromHds();
    int totalToZscaler = zScalerMetrics.getMavenUrlsToZscaler() + zScalerMetrics.getNpmUrlsToZscaler() +
        zScalerMetrics.getPypiUrlsToZscaler() + zScalerMetrics.getNugetUrlsToZscaler();

    telemetryData.put(TOTAL_URLS_FROM_HDS, totalFromHds);
    telemetryData.put(TOTAL_URLS_TO_ZSCALER, totalToZscaler);
    telemetryData.put(UPDATED_AT, zScalerMetrics.getUpdatedAt());

    telemetryData.put(MAVEN_URLS_FROM_HDS, zScalerMetrics.getMavenUrlsFromHds());
    telemetryData.put(NPM_URLS_FROM_HDS, zScalerMetrics.getNpmUrlsFromHds());
    telemetryData.put(PYPI_URLS_FROM_HDS, zScalerMetrics.getPypiUrlsFromHds());
    telemetryData.put(NUGET_URLS_FROM_HDS, zScalerMetrics.getNugetUrlsFromHds());

    telemetryData.put(MAVEN_URLS_TO_ZSCALER, zScalerMetrics.getMavenUrlsToZscaler());
    telemetryData.put(NPM_URLS_TO_ZSCALER, zScalerMetrics.getNpmUrlsToZscaler());
    telemetryData.put(PYPI_URLS_TO_ZSCALER, zScalerMetrics.getPypiUrlsToZscaler());
    telemetryData.put(NUGET_URLS_TO_ZSCALER, zScalerMetrics.getNugetUrlsToZscaler());
    return telemetryData;
  }

  @Override
  public boolean isClusterTelemetry() {
    return true;
  }
}
