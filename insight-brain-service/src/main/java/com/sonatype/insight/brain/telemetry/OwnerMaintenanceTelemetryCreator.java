/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

@Named
public class OwnerMaintenanceTelemetryCreator
{
  private final TelemetrySender telemetrySender;

  private final TelemetryUtils telemetryUtils;

  @Inject
  public OwnerMaintenanceTelemetryCreator(final TelemetrySender telemetrySender, TelemetryUtils telemetryUtils) {
    this.telemetrySender = telemetrySender;
    this.telemetryUtils = telemetryUtils;
  }

  public void sendOwnerMaintenanceTelemetry(Application application, String maintenanceType) {
    String applicationId = telemetryUtils.obfuscateIfAdvancedReportingDisabled(application.getId());
    String applicationName = telemetryUtils.obfuscateIfAdvancedReportingDisabled(application.getName());
    final OwnerMaintenanceTelemetry ownerMaintenanceTelemetry =
        new OwnerMaintenanceTelemetry(applicationId, applicationName, maintenanceType);

    sendOwnerMaintenanceTelemetry(ownerMaintenanceTelemetry);
  }

  private void sendOwnerMaintenanceTelemetry(OwnerMaintenanceTelemetry ownerMaintenanceTelemetry) {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.REAL_OWNER_IDS);
    telemetryData.getAttributes().put(OwnerMaintenanceTelemetry.OWNER_MAINTENANCE_TELEMETRY, ownerMaintenanceTelemetry);
    telemetrySender.send(telemetryData);
  }
}
