/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.telemetry.TelemetryCollector;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

/**
 * @since 1.168
 */
@Named
@Singleton
public class ApplicationTelemetryCollector
    implements TelemetryCollector
{
  public static final String ALL_OWNER_IDS_NAMES = "all_owner_ids_and_names";

  private final ApplicationDAO applicationDAO;

  private final TelemetryUtils telemetryUtils;

  @Inject
  public ApplicationTelemetryCollector(final ApplicationDAO applicationDAO, TelemetryUtils telemetryUtils) {
    this.applicationDAO = applicationDAO;
    this.telemetryUtils = telemetryUtils;
  }

  @Override
  public TelemetryData collectData() {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.REAL_OWNER_IDS);
    List<OwnerData> ownerData = applicationDAO.getAll().stream()
        .map(this::createOwnerDataFromApplication)
        .collect(Collectors.toList());
    telemetryData.put(ALL_OWNER_IDS_NAMES, ownerData);
    return telemetryData;
  }

  private OwnerData createOwnerDataFromApplication(Application app) {
    return new OwnerData(telemetryUtils.obfuscateIfAdvancedReportingDisabled(app.getId()),
        OwnerType.APPLICATION.toString(), telemetryUtils.obfuscateIfAdvancedReportingDisabled(app.getName()));
  }

  @Override
  public boolean isClusterTelemetry() {
    return true;
  }

  public class OwnerData
  {
    private String ownerId;

    private String ownerType;

    private String ownerName;

    public OwnerData() {
      //for serialization
    }

    public OwnerData(final String ownerId, final String ownerType, final String ownerName) {
      this.ownerId = ownerId;
      this.ownerType = ownerType;
      this.ownerName = ownerName;
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
  }
}
