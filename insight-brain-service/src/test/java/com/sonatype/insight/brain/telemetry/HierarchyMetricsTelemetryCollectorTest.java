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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

public class HierarchyMetricsTelemetryCollectorTest
    extends AbstractComponentTest
{
  @Inject
  private HierarchyMetricsTelemetryCollector telemetryCollector;

  @Test
  public void testCollectData_TelemetryPurpose() throws Exception {
    TelemetryData telemetryData = telemetryCollector.collectData();
    assertThat(telemetryData.getPurpose(), is(TelemetryPurpose.HIERARCHY_METRICS));
  }

  @Test
  public void testCollectData_ZeroApps() throws Exception {
    long expectedMinTimestamp = System.currentTimeMillis();
    TelemetryData telemetryData = telemetryCollector.collectData();
    long expectedMaxTimestamp = System.currentTimeMillis();
    assertThat(telemetryData.getTimestamp(), greaterThanOrEqualTo(expectedMinTimestamp));
    assertThat(telemetryData.getTimestamp(), lessThanOrEqualTo(expectedMaxTimestamp));
    assertThat(telemetryData.getAttributes().get(HierarchyMetricsTelemetryCollector.NUMBER_OF_ORGS), is("0"));
    assertThat(telemetryData.getAttributes().get(HierarchyMetricsTelemetryCollector.NUMBER_OF_APPS), is("0"));
    assertThat(telemetryData.getAttributes().get(HierarchyMetricsTelemetryCollector.MAX_APPS_PER_ORG), is("0"));
    assertThat(telemetryData.getAttributes().get(HierarchyMetricsTelemetryCollector.MIN_APPS_PER_ORG), is("0"));
    assertThat(telemetryData.getAttributes().get(HierarchyMetricsTelemetryCollector.P90_APPS_PER_ORG), is("0"));
  }

  @Test
  public void testCollectData_MaxOneApp() throws Exception {
    createAppsAndOrgs(2);
    long expectedMinTimestamp = System.currentTimeMillis();
    TelemetryData telemetryData = telemetryCollector.collectData();
    long expectedMaxTimestamp = System.currentTimeMillis();
    assertThat(telemetryData.getTimestamp(), greaterThanOrEqualTo(expectedMinTimestamp));
    assertThat(telemetryData.getTimestamp(), lessThanOrEqualTo(expectedMaxTimestamp));
    assertThat(telemetryData.getAttributes().get(HierarchyMetricsTelemetryCollector.NUMBER_OF_ORGS), is("2"));
    assertThat(telemetryData.getAttributes().get(HierarchyMetricsTelemetryCollector.NUMBER_OF_APPS), is("1"));
    assertThat(telemetryData.getAttributes().get(HierarchyMetricsTelemetryCollector.MAX_APPS_PER_ORG), is("1"));
    assertThat(telemetryData.getAttributes().get(HierarchyMetricsTelemetryCollector.MIN_APPS_PER_ORG), is("0"));
    assertThat(telemetryData.getAttributes().get(HierarchyMetricsTelemetryCollector.P90_APPS_PER_ORG), is("1.0"));
  }

  @Test
  public void testCollectData_MaxTenApps() throws Exception {
    createAppsAndOrgs(11);
    long expectedMinTimestamp = System.currentTimeMillis();
    TelemetryData telemetryData = telemetryCollector.collectData();
    long expectedMaxTimestamp = System.currentTimeMillis();
    assertThat(telemetryData.getTimestamp(), greaterThanOrEqualTo(expectedMinTimestamp));
    assertThat(telemetryData.getTimestamp(), lessThanOrEqualTo(expectedMaxTimestamp));
    assertThat(telemetryData.getAttributes().get(HierarchyMetricsTelemetryCollector.NUMBER_OF_ORGS), is("11"));
    assertThat(telemetryData.getAttributes().get(HierarchyMetricsTelemetryCollector.NUMBER_OF_APPS), is("55"));
    assertThat(telemetryData.getAttributes().get(HierarchyMetricsTelemetryCollector.MAX_APPS_PER_ORG), is("10"));
    assertThat(telemetryData.getAttributes().get(HierarchyMetricsTelemetryCollector.MIN_APPS_PER_ORG), is("0"));
    assertThat(telemetryData.getAttributes().get(HierarchyMetricsTelemetryCollector.P90_APPS_PER_ORG), is("9.8"));
  }

  @Test
  public void testCollectData_MaxTwentyApps() throws Exception {
    createAppsAndOrgs(21);
    long expectedMinTimestamp = System.currentTimeMillis();
    TelemetryData telemetryData = telemetryCollector.collectData();
    long expectedMaxTimestamp = System.currentTimeMillis();
    assertThat(telemetryData.getTimestamp(), greaterThanOrEqualTo(expectedMinTimestamp));
    assertThat(telemetryData.getTimestamp(), lessThanOrEqualTo(expectedMaxTimestamp));
    assertThat(telemetryData.getAttributes().get(HierarchyMetricsTelemetryCollector.NUMBER_OF_ORGS), is("21"));
    assertThat(telemetryData.getAttributes().get(HierarchyMetricsTelemetryCollector.NUMBER_OF_APPS), is("210"));
    assertThat(telemetryData.getAttributes().get(HierarchyMetricsTelemetryCollector.MAX_APPS_PER_ORG), is("20"));
    assertThat(telemetryData.getAttributes().get(HierarchyMetricsTelemetryCollector.MIN_APPS_PER_ORG), is("0"));
    assertThat(telemetryData.getAttributes().get(HierarchyMetricsTelemetryCollector.P90_APPS_PER_ORG), is("18.8"));
  }

  @Test
  public void testCollectData_MinOneApp() throws Exception {
    Organization organization = tempEntity.newOrganization();
    tempEntity.newApplication(organization.getId());
    long expectedMinTimestamp = System.currentTimeMillis();
    TelemetryData telemetryData = telemetryCollector.collectData();
    long expectedMaxTimestamp = System.currentTimeMillis();
    assertThat(telemetryData.getTimestamp(), greaterThanOrEqualTo(expectedMinTimestamp));
    assertThat(telemetryData.getTimestamp(), lessThanOrEqualTo(expectedMaxTimestamp));
    assertThat(telemetryData.getAttributes().get(HierarchyMetricsTelemetryCollector.NUMBER_OF_ORGS), is("1"));
    assertThat(telemetryData.getAttributes().get(HierarchyMetricsTelemetryCollector.NUMBER_OF_APPS), is("1"));
    assertThat(telemetryData.getAttributes().get(HierarchyMetricsTelemetryCollector.MAX_APPS_PER_ORG), is("1"));
    assertThat(telemetryData.getAttributes().get(HierarchyMetricsTelemetryCollector.MIN_APPS_PER_ORG), is("1"));
    assertThat(telemetryData.getAttributes().get(HierarchyMetricsTelemetryCollector.P90_APPS_PER_ORG), is("1.0"));
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
