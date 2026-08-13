/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlUserActivityDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlUserActivityTelemetryDTO;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ComponentH2Test
public class SourceControlUserActivityTelemetryCollectorTest
    extends AbstractComponentH2Test
{
  @Mock
  private SourceControlUserActivityDAO sourceControlUserActivityDAO;

  private SourceControlUserActivityTelemetryCollector collector;

  @BeforeEach
  public void setup() {
    collector =
        new SourceControlUserActivityTelemetryCollector(sourceControlUserActivityDAO);
  }

  @Test
  public void testCollectData() {
    // given: User activities for two apps and 4 timestamps
    SourceControlUserActivityTelemetryDTO activity1 =
        new SourceControlUserActivityTelemetryDTO("id1", "test@test.com", "app1",
            LocalDate.now().minusMonths(1));
    SourceControlUserActivityTelemetryDTO activity2 =
        new SourceControlUserActivityTelemetryDTO("id2", "test@test.com", "app1",
            LocalDate.now().minusMonths(2));
    SourceControlUserActivityTelemetryDTO activity3 =
        new SourceControlUserActivityTelemetryDTO("id3", "test2@test.com", "app1",
            LocalDate.now().minusMonths(3));
    SourceControlUserActivityTelemetryDTO activity4 =
        new SourceControlUserActivityTelemetryDTO("id4", "test3@test.com", "app2",
            LocalDate.now().minusMonths(4));

    Map<String, List<String>> app1ActivityMap = new HashMap<>();
    app1ActivityMap.put(HdsClientAnalytics.obfuscate(activity1.getEmail()),
        Arrays.asList(activity1.getCommitYearMonth().toString(), activity2.getCommitYearMonth().toString()));

    app1ActivityMap.put(HdsClientAnalytics.obfuscate(activity3.getEmail()),
        Collections.singletonList(activity3.getCommitYearMonth().toString()));

    Map<String, List<String>> app2ActivityMap = new HashMap<>();
    app2ActivityMap.put(HdsClientAnalytics.obfuscate(activity4.getEmail()),
        Collections.singletonList(activity4.getCommitYearMonth().toString()));

    when(sourceControlUserActivityDAO.getActivitiesNotSentToTelemetry()).thenReturn(
        Arrays.asList(activity1, activity2, activity3, activity4));

    Map<String, Map<String, List<String>>> sourceControlUserActivitiesMap = new HashMap<>();
    sourceControlUserActivitiesMap.put(HdsClientAnalytics.obfuscate(activity1.getApplicationId()), app1ActivityMap);
    sourceControlUserActivitiesMap.put(HdsClientAnalytics.obfuscate(activity4.getApplicationId()), app2ActivityMap);

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.SOURCE_CONTROL_USER_ACTIVITY);
    telemetryData.put(SourceControlUserActivityTelemetryCollector.USER_ACTIVITIES,
        sourceControlUserActivitiesMap);

    // when: fetch user activities info
    TelemetryData telemetryDataListFromCollector = collector.collectData();

    // then: the number of telemetry objects we'd expect were created
    Map<String, Map<String, List<String>>> dataFromCollector =
        (Map<String, Map<String, List<String>>>) telemetryDataListFromCollector.getAttributes()
            .get(SourceControlUserActivityTelemetryCollector.USER_ACTIVITIES);

    assertThat(dataFromCollector).hasSize(2);
    assertThat(dataFromCollector).usingRecursiveComparison().isEqualTo(sourceControlUserActivitiesMap);
  }

  @Test
  public void testCollectData_NoActivities() {
    when(sourceControlUserActivityDAO.getActivitiesNotSentToTelemetry()).thenReturn(
        Collections.emptyList());
    TelemetryData telemetryDataFromCollector = collector.collectData();
    assertThat(telemetryDataFromCollector).isNull();
  }

  @Test
  public void testCollectAllData_NoActivities() {
    when(sourceControlUserActivityDAO.getActivitiesNotSentToTelemetry()).thenReturn(
        Collections.emptyList());
    List<TelemetryData> telemetryDataListFromCollector = collector.collectAllData();
    assertThat(telemetryDataListFromCollector).isEmpty();
  }

  @Test
  public void testIsClusterTelemetry() {
    assertThat(collector.isClusterTelemetry()).isFalse();
  }
}
