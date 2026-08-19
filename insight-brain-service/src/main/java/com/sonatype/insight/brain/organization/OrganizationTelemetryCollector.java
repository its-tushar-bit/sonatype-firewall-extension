/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.telemetry.TelemetryCollector;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

@Named
@Singleton
public class OrganizationTelemetryCollector
    extends OwnerTelemetryCollector
    implements TelemetryCollector
{
  private final OrganizationDAO organizationDAO;

  @Inject
  public OrganizationTelemetryCollector(OrganizationDAO organizationDAO, TelemetryUtils telemetryUtils) {
    super(telemetryUtils);
    this.organizationDAO = organizationDAO;
  }

  @Override
  public TelemetryData collectData() {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.REAL_OWNER_IDS);
    List<OwnerTelemetryCollector.OwnerData> ownerData = organizationDAO.getAll()
        .stream()
        .map(this::createOwnerData)
        .collect(Collectors.toList());
    telemetryData.put(ALL_OWNER_IDS_NAMES, ownerData);
    return telemetryData;
  }

  @Override
  public boolean isClusterTelemetry() {
    return true;
  }
}
