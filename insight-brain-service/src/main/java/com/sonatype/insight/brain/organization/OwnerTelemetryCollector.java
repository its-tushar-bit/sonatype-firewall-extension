/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;

public abstract class OwnerTelemetryCollector
{
  public static final String ALL_OWNER_IDS_NAMES = "all_owner_ids_and_names";

  private final TelemetryUtils telemetryUtils;

  public OwnerTelemetryCollector(TelemetryUtils telemetryUtils) {
    this.telemetryUtils = telemetryUtils;
  }

  protected OwnerData createOwnerData(Owner owner) {
    String type = null;
    if (owner instanceof Application) {
      type = OwnerType.APPLICATION.toString();
    }
    else if (owner instanceof Organization) {
      type = OwnerType.ORGANIZATION.toString();
    }

    return new OwnerData(
        telemetryUtils.obfuscateIfAdvancedReportingDisabled(owner.getId()),
        type,
        telemetryUtils.obfuscateIfAdvancedReportingDisabled(owner.getName()),
        telemetryUtils.obfuscateIfAdvancedReportingDisabled(owner.getParentOwnerId()));
  }

  public static class OwnerData
  {
    private String ownerId;

    private String ownerType;

    private String ownerName;

    private String parentOwnerId;

    public OwnerData() {
      // for serialization
    }

    public OwnerData(String ownerId, String ownerType, String ownerName, String parentOwnerId) {
      this.ownerId = ownerId;
      this.ownerType = ownerType;
      this.ownerName = ownerName;
      this.parentOwnerId = parentOwnerId;
    }

    public String getOwnerId() {
      return ownerId;
    }

    public String getOwnerType() {
      return ownerType;
    }

    public String getOwnerName() {
      return ownerName;
    }

    public String getParentOwnerId() {
      return parentOwnerId;
    }
  }
}
