/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableMap;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import jakarta.inject.Inject;
import java.sql.Connection;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

@ComponentH2Test
public class ClusterTelemetryCollectorTest
    extends AbstractComponentH2Test
{
  @Inject
  private ClusterTelemetryCollector collector;

  @Mock
  private ProductLicense productLicense;

  @Inject
  private TaskScheduler taskScheduler;

  @Inject
  private OperationalDataStore operationalDataStore;

  @BeforeEach
  public void before() {
    taskScheduler.createScheduler();
  }

  @AfterEach
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
    try (Connection connection = operationalDataStore.getDataSource().getConnection()) {
      DSL.using(connection)
          .insertInto(DSL.table(operationalDataStore.getDatabaseSchema() + ".QRTZ_SCHEDULER_STATE"))
          .columns(
              DSL.field("SCHED_NAME"),
              DSL.field("INSTANCE_NAME"),
              DSL.field("LAST_CHECKIN_TIME"),
              DSL.field("CHECKIN_INTERVAL"))
          .values(
              taskScheduler.getScheduler().getSchedulerName(),
              instanceId,
              checkinTimestamp,
              5L)
          .execute();
    }
  }

  private void deleteAllSchedulerStateRecords() throws Exception {
    try (Connection connection = operationalDataStore.getDataSource().getConnection()) {
      DSL.using(connection)
          .deleteFrom(DSL.table(operationalDataStore.getDatabaseSchema() + ".QRTZ_SCHEDULER_STATE"))
          .execute();
    }
  }
}
