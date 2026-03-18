/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.sql.Connection;
import java.sql.PreparedStatement;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ClusterTelemetryCollectorTest
    extends AbstractComponentTest
{
  @Inject
  private ClusterTelemetryCollector collector;

  @Mock
  private ProductLicense productLicense;

  @Inject
  private TaskScheduler taskScheduler;

  @Inject
  private OperationalDataStore operationalDataStore;

  @Override
  public void configure(Binder binder) {
    binder.bind(ProductLicense.class).toInstance(productLicense);
    super.configure(binder);
  }

  @Before
  public void before() {
    taskScheduler.createScheduler();
  }

  @After
  public void after() throws Exception {
    taskScheduler.stop();
    deleteAllSchedulerStateRecords();
  }

  @Test
  public void testCollectData_TelemetryPurpose() {
    Mockito.when(productLicense.hasFeature(LicensedFeature.NODE_CLUSTERING)).thenReturn(true);
    TelemetryData telemetryData = collector.collectData();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.CLUSTER_USAGE);
  }

  @Test
  public void testCollectData_Attributes() throws Exception {
    Mockito.when(productLicense.hasFeature(LicensedFeature.NODE_CLUSTERING)).thenReturn(true);
    createSchedulerStateRecord("node1", 1);
    createSchedulerStateRecord("node2", 1);
    TelemetryData telemetryData = collector.collectData();
    assertThat(telemetryData.getAttributes()).containsAllEntriesOf(
        ImmutableMap.of("node_count", 2));
  }

  @Test
  public void testCollectData_throwsRuntimeException() {
    collector = new ClusterTelemetryCollector(null);
    assertThatThrownBy(() -> collector.collectData()).isInstanceOf(RuntimeException.class);
  }

  @Test
  public void testCollectData_featureDisabled() {
    Mockito.when(productLicense.hasFeature(LicensedFeature.NODE_CLUSTERING)).thenReturn(false);
    assertThat(collector.collectData()).isNull();
  }

  private void createSchedulerStateRecord(String instanceId, long checkinTimestamp) throws Exception {
    String sQuery = "INSERT INTO " + operationalDataStore.getDatabaseSchema() + ".QRTZ_SCHEDULER_STATE" + //
        " (SCHED_NAME, INSTANCE_NAME, LAST_CHECKIN_TIME, CHECKIN_INTERVAL) " + //
        " VALUES (?1, ?2, ?3, ?4)";
    try (Connection connection = operationalDataStore.getDataSource()
        .getConnection(); PreparedStatement statement = connection.prepareStatement(sQuery))
    {
      statement.setString(1, taskScheduler.getScheduler().getSchedulerName());
      statement.setString(2, instanceId);
      statement.setLong(3, checkinTimestamp);
      statement.setLong(4, 5);
      statement.execute();
    }
  }

  private void deleteAllSchedulerStateRecords() throws Exception {
    String sQuery = "DELETE FROM " + operationalDataStore.getDatabaseSchema() + ".QRTZ_SCHEDULER_STATE";
    try (Connection connection = operationalDataStore.getDataSource()
        .getConnection(); PreparedStatement statement = connection.prepareStatement(sQuery))
    {
      statement.execute();
    }
  }
}
