/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import javax.inject.Inject;

import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.DatabaseConfigProvider;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.telemetry.model.TelemetryData;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class TelemetryCollectorTest
    extends AbstractComponentTest
{
  @Inject
  private TelemetryCollector telemetryCollector;

  @Test
  public void testCollectData_ZeroApps() throws Exception {
    long expectedMinTimestamp = System.currentTimeMillis();
    TelemetryData telemetryData = telemetryCollector.collectData();
    long expectedMaxTimestamp = System.currentTimeMillis();
    assertThat(telemetryData.getTimestamp(), greaterThanOrEqualTo(expectedMinTimestamp));
    assertThat(telemetryData.getTimestamp(), lessThanOrEqualTo(expectedMaxTimestamp));
    assertThat(telemetryData.getAttributes().get(TelemetryCollector.NUMBER_OF_ORGS), is("0"));
    assertThat(telemetryData.getAttributes().get(TelemetryCollector.NUMBER_OF_APPS), is("0"));
    assertThat(telemetryData.getAttributes().get(TelemetryCollector.MAX_APPS_PER_ORG), is("0"));
    assertThat(telemetryData.getAttributes().get(TelemetryCollector.MIN_APPS_PER_ORG), is("0"));
    assertThat(telemetryData.getAttributes().get(TelemetryCollector.P90_APPS_PER_ORG), is("0"));
  }

  @Test
  public void testCollectData_MaxOneApp() throws Exception {
    createAppsAndOrgs(2);
    long expectedMinTimestamp = System.currentTimeMillis();
    TelemetryData telemetryData = telemetryCollector.collectData();
    long expectedMaxTimestamp = System.currentTimeMillis();
    assertThat(telemetryData.getTimestamp(), greaterThanOrEqualTo(expectedMinTimestamp));
    assertThat(telemetryData.getTimestamp(), lessThanOrEqualTo(expectedMaxTimestamp));
    assertThat(telemetryData.getAttributes().get(TelemetryCollector.NUMBER_OF_ORGS), is("2"));
    assertThat(telemetryData.getAttributes().get(TelemetryCollector.NUMBER_OF_APPS), is("1"));
    assertThat(telemetryData.getAttributes().get(TelemetryCollector.MAX_APPS_PER_ORG), is("1"));
    assertThat(telemetryData.getAttributes().get(TelemetryCollector.MIN_APPS_PER_ORG), is("0"));
    assertThat(telemetryData.getAttributes().get(TelemetryCollector.P90_APPS_PER_ORG), is("1.0"));
  }

  @Test
  public void testCollectData_MaxTenApps() throws Exception {
    createAppsAndOrgs(11);
    long expectedMinTimestamp = System.currentTimeMillis();
    TelemetryData telemetryData = telemetryCollector.collectData();
    long expectedMaxTimestamp = System.currentTimeMillis();
    assertThat(telemetryData.getTimestamp(), greaterThanOrEqualTo(expectedMinTimestamp));
    assertThat(telemetryData.getTimestamp(), lessThanOrEqualTo(expectedMaxTimestamp));
    assertThat(telemetryData.getAttributes().get(TelemetryCollector.NUMBER_OF_ORGS), is("11"));
    assertThat(telemetryData.getAttributes().get(TelemetryCollector.NUMBER_OF_APPS), is("55"));
    assertThat(telemetryData.getAttributes().get(TelemetryCollector.MAX_APPS_PER_ORG), is("10"));
    assertThat(telemetryData.getAttributes().get(TelemetryCollector.MIN_APPS_PER_ORG), is("0"));
    assertThat(telemetryData.getAttributes().get(TelemetryCollector.P90_APPS_PER_ORG), is("9.8"));
  }

  @Test
  public void testCollectData_MaxTwentyApps() throws Exception {
    createAppsAndOrgs(21);
    long expectedMinTimestamp = System.currentTimeMillis();
    TelemetryData telemetryData = telemetryCollector.collectData();
    long expectedMaxTimestamp = System.currentTimeMillis();
    assertThat(telemetryData.getTimestamp(), greaterThanOrEqualTo(expectedMinTimestamp));
    assertThat(telemetryData.getTimestamp(), lessThanOrEqualTo(expectedMaxTimestamp));
    assertThat(telemetryData.getAttributes().get(TelemetryCollector.NUMBER_OF_ORGS), is("21"));
    assertThat(telemetryData.getAttributes().get(TelemetryCollector.NUMBER_OF_APPS), is("210"));
    assertThat(telemetryData.getAttributes().get(TelemetryCollector.MAX_APPS_PER_ORG), is("20"));
    assertThat(telemetryData.getAttributes().get(TelemetryCollector.MIN_APPS_PER_ORG), is("0"));
    assertThat(telemetryData.getAttributes().get(TelemetryCollector.P90_APPS_PER_ORG), is("18.8"));
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
    assertThat(telemetryData.getAttributes().get(TelemetryCollector.NUMBER_OF_ORGS), is("1"));
    assertThat(telemetryData.getAttributes().get(TelemetryCollector.NUMBER_OF_APPS), is("1"));
    assertThat(telemetryData.getAttributes().get(TelemetryCollector.MAX_APPS_PER_ORG), is("1"));
    assertThat(telemetryData.getAttributes().get(TelemetryCollector.MIN_APPS_PER_ORG), is("1"));
    assertThat(telemetryData.getAttributes().get(TelemetryCollector.P90_APPS_PER_ORG), is("1.0"));
  }

  @Test
  public void testCollectData_OdsSizeBytes_InMemory() throws Exception {
    assertThat(telemetryCollector.collectData().getAttributes().get(TelemetryCollector.ODS_SIZE_BYTES),
        is(nullValue()));
  }

  @Test
  public void testCollectData_OdsSizeBytes_InFile() throws Exception {
    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(tempDir.getRoot().getAbsolutePath());
    DataSourceFactory.clear_ForTestsOnly();
    try {
      OperationalDataStoreProvider.init(new DatabaseConfigProvider(insightConfig).getDatabaseConfig(DatabaseName.ods),
          false);
      String odsSizeBytes = (String) telemetryCollector.collectData().getAttributes()
          .get(TelemetryCollector.ODS_SIZE_BYTES);
      assertThat(odsSizeBytes, is(notNullValue()));
      assertThat(Long.valueOf(odsSizeBytes), is(greaterThan(0L)));
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
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
