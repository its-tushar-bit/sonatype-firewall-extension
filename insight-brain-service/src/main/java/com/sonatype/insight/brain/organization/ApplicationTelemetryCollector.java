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

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.telemetry.TelemetryCollector;
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

  @Inject
  public ApplicationTelemetryCollector(final ApplicationDAO applicationDAO) {
    this.applicationDAO = applicationDAO;
  }

  @Override
  public TelemetryData collectData() {
    if (SystemConfigurationPropertyFeature.INTEGRATED_ENTERPRISE_REPORTING.isEnabled()) {
      TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.REAL_OWNER_IDS);
      List<OwnerData> ownerData = applicationDAO.getAll().stream()
          .map(app -> new OwnerData(app.getId(), OwnerType.APPLICATION.toString(), app.getName()))
          .collect(Collectors.toList());
      telemetryData.put(ALL_OWNER_IDS_NAMES, ownerData);
      return telemetryData;
    }
    else {
      return null;
    }
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
