/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

@Named
@Singleton
public class SourceControlMetricsTelemetryCollector
    implements TelemetryCollector
{
  public static final String TOTAL_SC_WITH_PR_ENABLED = "total_source_control_entries_with_pr_enabled";

  public static final String TOTAL_APPLICATION_SC_ENTRIES = "total_source_control_applications";

  public static final String TOTAL_APPLICATIONS = "total_applications";

  private final SourceControlDAO sourceControlDAO;

  private final ApplicationDAO applicationDAO;

  @Inject
  public SourceControlMetricsTelemetryCollector(SourceControlDAO sourceControlDAO, ApplicationDAO applicationDAO) {
    this.sourceControlDAO = sourceControlDAO;
    this.applicationDAO = applicationDAO;
  }

  @Override
  public TelemetryData collectData() {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.SOURCE_CONTROL_METRICS);
    Map<String, Object> attributes = telemetryData.getAttributes();

    attributes
        .put(TOTAL_SC_WITH_PR_ENABLED, String.valueOf(sourceControlDAO.getApplicationsWithPullReqsEnabled().size()));
    attributes.put(TOTAL_APPLICATION_SC_ENTRIES, String.valueOf(sourceControlDAO.getByApplication().size()));
    attributes.put(TOTAL_APPLICATIONS, String.valueOf(applicationDAO.getAll().size()));

    return telemetryData;
  }
}
