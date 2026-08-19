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
      final String ownerId,
      final String ownerName,
      final String parentOwnerId,
      final String ownerType,
      final String ownerMaintenanceType)
  {
    this.ownerId = ownerId;
    this.ownerName = ownerName;
    this.parentOwnerId = parentOwnerId;
    this.ownerType = ownerType;
    this.ownerMaintenanceType = ownerMaintenanceType;
  }

  private String ownerId;

  private String ownerName;

  private String parentOwnerId;

  private String ownerType;

  private String ownerMaintenanceType;

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(final String ownerId) {
    this.ownerId = ownerId;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public void setOwnerName(final String ownerName) {
    this.ownerName = ownerName;
  }

  public String getParentOwnerId() {
    return parentOwnerId;
  }

  public void setParentOwnerId(String parentOwnerId) {
    this.parentOwnerId = parentOwnerId;
  }

  public String getOwnerType() {
    return ownerType;
  }

  public void setOwnerType(String ownerType) {
    this.ownerType = ownerType;
  }

  public String getOwnerMaintenanceType() {
    return ownerMaintenanceType;
  }

  public void setOwnerMaintenanceType(final String ownerMaintenanceType) {
    this.ownerMaintenanceType = ownerMaintenanceType;
  }
}
