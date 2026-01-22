/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.configuration.ZScalerConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.ZScalerConfiguration;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.telemetry.TelemetryCollector;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

@Named
@Singleton
public class ZScalerConfigTelemetryCollector
    implements TelemetryCollector
{
  private final ZScalerConfigurationDAO zScalerConfigurationDAO;

  private final ProductLicense productLicense;

  @Inject
  public ZScalerConfigTelemetryCollector(
      final ZScalerConfigurationDAO zScalerConfigurationDAO,
      final ProductLicense productLicense)
  {
    this.zScalerConfigurationDAO = zScalerConfigurationDAO;
    this.productLicense = productLicense;
  }

  @Override
  public TelemetryData collectData() {
    if (!productLicense.hasFeature(LicensedFeature.FIREWALL)) {
      return null;
    }

    ZScalerConfiguration zScalerConfiguration = zScalerConfigurationDAO.get();
    if (zScalerConfiguration == null) {
      // Returning null prevents the sender from sending telemetry data.
      return null;
    }
    else {
      TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.ZSCALER_CONFIGURATION);
      telemetryData.put("zscaler_host", zScalerConfiguration.getHostname());
      telemetryData.put("zscaler_eula_agreed", true);
      return telemetryData;
    }
  }

  @Override
  public boolean isClusterTelemetry() {
    return true;
  }
}
