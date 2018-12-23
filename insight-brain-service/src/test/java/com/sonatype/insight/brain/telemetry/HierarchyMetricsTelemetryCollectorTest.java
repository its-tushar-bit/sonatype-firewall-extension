/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HierarchyMetricsTelemetryCollectorTest
    extends AbstractComponentTest
{
  @Inject
  private HierarchyMetricsTelemetryCollector telemetryCollector;

  @Test
  public void testCollectData_TelemetryPurpose() throws Exception {
    TelemetryData telemetryData = telemetryCollector.collectData();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.HIERARCHY_METRICS);
  }

  @Test
  public void testCollectData_ZeroApps() throws Exception {
    long expectedMinTimestamp = System.currentTimeMillis();
    TelemetryData telemetryData = telemetryCollector.collectData();
    long expectedMaxTimestamp = System.currentTimeMillis();
    assertThat(telemetryData.getTimestamp()).isBetween(expectedMinTimestamp, expectedMaxTimestamp);
    assertThat(telemetryData.getAttributes()) //
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_ORGS, "0")
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_APPS, "0")
        .containsEntry(HierarchyMetricsTelemetryCollector.MAX_APPS_PER_ORG, "0")
        .containsEntry(HierarchyMetricsTelemetryCollector.MIN_APPS_PER_ORG, "0")
        .containsEntry(HierarchyMetricsTelemetryCollector.P90_APPS_PER_ORG, "0");
  }

  @Test
  public void testCollectData_MaxOneApp() throws Exception {
    createAppsAndOrgs(2);
    long expectedMinTimestamp = System.currentTimeMillis();
    TelemetryData telemetryData = telemetryCollector.collectData();
    long expectedMaxTimestamp = System.currentTimeMillis();
    assertThat(telemetryData.getTimestamp()).isBetween(expectedMinTimestamp, expectedMaxTimestamp);
    assertThat(telemetryData.getAttributes()) //
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_ORGS, "2")
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_APPS, "1")
        .containsEntry(HierarchyMetricsTelemetryCollector.MAX_APPS_PER_ORG, "1")
        .containsEntry(HierarchyMetricsTelemetryCollector.MIN_APPS_PER_ORG, "0")
        .containsEntry(HierarchyMetricsTelemetryCollector.P90_APPS_PER_ORG, "1.0");
  }

  @Test
  public void testCollectData_MaxTenApps() throws Exception {
    createAppsAndOrgs(11);
    long expectedMinTimestamp = System.currentTimeMillis();
    TelemetryData telemetryData = telemetryCollector.collectData();
    long expectedMaxTimestamp = System.currentTimeMillis();
    assertThat(telemetryData.getTimestamp()).isBetween(expectedMinTimestamp, expectedMaxTimestamp);
    assertThat(telemetryData.getAttributes()) //
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_ORGS, "11")
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_APPS, "55")
        .containsEntry(HierarchyMetricsTelemetryCollector.MAX_APPS_PER_ORG, "10")
        .containsEntry(HierarchyMetricsTelemetryCollector.MIN_APPS_PER_ORG, "0")
        .containsEntry(HierarchyMetricsTelemetryCollector.P90_APPS_PER_ORG, "9.8");
  }

  @Test
  public void testCollectData_MaxTwentyApps() throws Exception {
    createAppsAndOrgs(21);
    long expectedMinTimestamp = System.currentTimeMillis();
    TelemetryData telemetryData = telemetryCollector.collectData();
    long expectedMaxTimestamp = System.currentTimeMillis();
    assertThat(telemetryData.getTimestamp()).isBetween(expectedMinTimestamp, expectedMaxTimestamp);
    assertThat(telemetryData.getAttributes()) //
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_ORGS, "21")
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_APPS, "210")
        .containsEntry(HierarchyMetricsTelemetryCollector.MAX_APPS_PER_ORG, "20")
        .containsEntry(HierarchyMetricsTelemetryCollector.MIN_APPS_PER_ORG, "0")
        .containsEntry(HierarchyMetricsTelemetryCollector.P90_APPS_PER_ORG, "18.8");
  }

  @Test
  public void testCollectData_MinOneApp() throws Exception {
    Organization organization = tempEntity.newOrganization();
    tempEntity.newApplication(organization.getId());
    long expectedMinTimestamp = System.currentTimeMillis();
    TelemetryData telemetryData = telemetryCollector.collectData();
    long expectedMaxTimestamp = System.currentTimeMillis();
    assertThat(telemetryData.getTimestamp()).isBetween(expectedMinTimestamp, expectedMaxTimestamp);
    assertThat(telemetryData.getAttributes()) //
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_ORGS, "1")
        .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_APPS, "1")
        .containsEntry(HierarchyMetricsTelemetryCollector.MAX_APPS_PER_ORG, "1")
        .containsEntry(HierarchyMetricsTelemetryCollector.MIN_APPS_PER_ORG, "1")
        .containsEntry(HierarchyMetricsTelemetryCollector.P90_APPS_PER_ORG, "1.0");
  }

  private void createAppsAndOrgs(int numberOfOrgs) {
    for (int i = 0; i < numberOfOrgs; i++) {
      Organization organization = tempEntity.newOrganization();
      for (int j = 0; j < i; j++) {
        tempEntity.newApplication(organization.getId());
      }
    }
  }
}
