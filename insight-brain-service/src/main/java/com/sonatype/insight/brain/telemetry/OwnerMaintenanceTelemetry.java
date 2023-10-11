/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

public class OwnerMaintenanceTelemetry
{
  public static final String OWNER_MAINTENANCE_TELEMETRY = "owner_maintenance_telemetry";

  public static final String TYPE_ADD = "Add";

  public static final String TYPE_UPDATE = "Update";

  public static final String TYPE_DELETE = "Delete";

  public OwnerMaintenanceTelemetry(
      final String realApplicationId,
      final String applicationName,
      final String ownerMaintenanceType)
  {
    this.realApplicationId = realApplicationId;
    this.applicationName = applicationName;
    this.ownerMaintenanceType = ownerMaintenanceType;
  }

  private String realApplicationId;

  private String applicationName;

  private String ownerMaintenanceType;

  public String getRealApplicationId() {
    return realApplicationId;
  }

  public void setRealApplicationId(final String realApplicationId) {
    this.realApplicationId = realApplicationId;
  }

  public String getApplicationName() {
    return applicationName;
  }

  public void setApplicationName(final String applicationName) {
    this.applicationName = applicationName;
  }

  public String getOwnerMaintenanceType() {
    return ownerMaintenanceType;
  }

  public void setOwnerMaintenanceType(final String ownerMaintenanceType) {
    this.ownerMaintenanceType = ownerMaintenanceType;
  }
}
