/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.model.Owner;
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

  public void sendOwnerMaintenanceTelemetry(Owner owner, String maintenanceType) {
    String ownerId = telemetryUtils.obfuscateIfAdvancedReportingDisabled(owner.getId());
    String ownerName = telemetryUtils.obfuscateIfAdvancedReportingDisabled(owner.getName());
    String parentOwnerId = telemetryUtils.obfuscateIfAdvancedReportingDisabled(owner.getParentOwnerId());
    String ownerType = owner.getType().toString();
    final OwnerMaintenanceTelemetry ownerMaintenanceTelemetry =
        new OwnerMaintenanceTelemetry(ownerId, ownerName, parentOwnerId, ownerType, maintenanceType);

    sendOwnerMaintenanceTelemetry(ownerMaintenanceTelemetry);
  }

  private void sendOwnerMaintenanceTelemetry(OwnerMaintenanceTelemetry ownerMaintenanceTelemetry) {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.REAL_OWNER_IDS);
    telemetryData.getAttributes().put(OwnerMaintenanceTelemetry.OWNER_MAINTENANCE_TELEMETRY, ownerMaintenanceTelemetry);
    telemetrySender.send(telemetryData);
  }
}
