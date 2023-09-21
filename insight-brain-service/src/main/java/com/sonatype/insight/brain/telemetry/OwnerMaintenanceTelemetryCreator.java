/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

@Named
public class OwnerMaintenanceTelemetryCreator
{
  private final TelemetrySender telemetrySender;

  @Inject
  public OwnerMaintenanceTelemetryCreator(final TelemetrySender telemetrySender) {
    this.telemetrySender = telemetrySender;
  }

  public void sendOwnerMaintenanceTelemetry(Application application, String maintenanceType) {
    if (SystemConfigurationPropertyFeature.LOOKER_INTEGRATED_ENTERPRISE_REPORTING.isEnabled()) {
      final OwnerMaintenanceTelemetry ownerMaintenanceTelemetry =
          new OwnerMaintenanceTelemetry(application.getId(), application.getName(), maintenanceType);

      sendOwnerMaintenanceTelemetry(ownerMaintenanceTelemetry);
    }
  }

  private void sendOwnerMaintenanceTelemetry(OwnerMaintenanceTelemetry ownerMaintenanceTelemetry) {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.REAL_OWNER_IDS);
    telemetryData.getAttributes().put(OwnerMaintenanceTelemetry.OWNER_MAINTENANCE_TELEMETRY, ownerMaintenanceTelemetry);
    telemetrySender.send(telemetryData);
  }
}
