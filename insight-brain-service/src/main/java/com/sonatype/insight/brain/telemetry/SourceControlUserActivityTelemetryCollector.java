/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlUserActivityDAO;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.apache.shiro.util.CollectionUtils;

/**
 * @since 1.170
 */
@Named
@Singleton
public class SourceControlUserActivityTelemetryCollector
    implements TelemetryCollector
{
  public static final String USER_ACTIVITIES = "user_activities";

  private final SourceControlUserActivityDAO sourceControlUserActivityDAO;

  @Inject
  public SourceControlUserActivityTelemetryCollector(SourceControlUserActivityDAO sourceControlUserActivityDAO) {
    this.sourceControlUserActivityDAO = sourceControlUserActivityDAO;
  }

  @Override
  public TelemetryData collectData() {
    return addTelemetry(fetchData());
  }

  private Map<String, Map<String, List<String>>> fetchData() {
    Map<String, Map<String, List<String>>> dataCollected = new HashMap<>();
    Set<String> activitiesIdToUpdate = new HashSet<>();
    fetchData(dataCollected, activitiesIdToUpdate);
    updateUserActivitiesSentToTelemetry(activitiesIdToUpdate);
    return dataCollected;
  }

  @Override
  public boolean isClusterTelemetry() {
    return false;
  }

  private TelemetryData addTelemetry(
      final Map<String, Map<String, List<String>>> sourceControlUserActivityTelemetry)
  {
    TelemetryData telemetryData = null;
    if (!CollectionUtils.isEmpty(sourceControlUserActivityTelemetry)) {
      telemetryData = new TelemetryData(TelemetryPurpose.SOURCE_CONTROL_USER_ACTIVITY);
      telemetryData.put(USER_ACTIVITIES, sourceControlUserActivityTelemetry);
    }
    return telemetryData;
  }

  private void fetchData(
      final Map<String, Map<String, List<String>>> dataCollected,
      final Set<String> activitiesIdToUpdate)
  {
    sourceControlUserActivityDAO.getActivitiesNotSentToTelemetry()
        .forEach(userActivity -> {
          dataCollected.computeIfAbsent(HdsClientAnalytics.obfuscate(userActivity.getApplicationId()),
              value -> new HashMap<>());
          dataCollected.get(HdsClientAnalytics.obfuscate(userActivity.getApplicationId()))
              .computeIfAbsent(HdsClientAnalytics.obfuscate(userActivity.getEmail()), k -> new ArrayList<>())
              .add(userActivity.getCommitYearMonth().toString());
          activitiesIdToUpdate.add(userActivity.getSourceControlUserActivityId());
        });
  }

  private void updateUserActivitiesSentToTelemetry(Set<String> activitiesIdToUpdate) {
    sourceControlUserActivityDAO.updateActivitiesSentToTelemetry(activitiesIdToUpdate);
  }
}
