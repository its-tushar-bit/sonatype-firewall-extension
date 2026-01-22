/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Map;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.scheduler.QuartzJobStoreTX;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.quartz.JobPersistenceException;

@Named
@Singleton
public class ClusterTelemetryCollector
    implements TelemetryCollector
{
  private final ProductLicense productLicense;

  private static final String NODE_COUNT = "node_count";

  @Inject
  QuartzJobStoreTX quartzJobStoreTX;

  @Inject
  public ClusterTelemetryCollector(
      final ProductLicense productLicense)
  {
    this.productLicense = productLicense;
  }

  @Override
  public TelemetryData collectData() {
    if (productLicense.hasFeature(LicensedFeature.NODE_CLUSTERING)) {
      TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.CLUSTER_USAGE);
      Map<String, Object> attributes = telemetryData.getAttributes();
      attributes.put(NODE_COUNT, getClusterSize());
      return telemetryData;
    }
    else {
      return null;
    }
  }

  private Integer getClusterSize() {
    try {
      return quartzJobStoreTX.getSchedulerStateRecords().size();
    }
    catch (JobPersistenceException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isClusterTelemetry() {
    return true;
  }
}
