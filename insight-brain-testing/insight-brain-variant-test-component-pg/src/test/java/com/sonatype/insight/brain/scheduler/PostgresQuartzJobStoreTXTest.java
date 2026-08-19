/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.variant.AbstractComponentPgTest;
import com.sonatype.insight.brain.variant.ComponentPgTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.impl.jdbcjobstore.JobStoreTX;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL Quartz job-store initialization assertion relocated from {@code QuartzJobStoreTXTest} (CLM-45235). The
 * H2 coverage (and the doCheckin/clustering suite) stays in the origin {@code QuartzJobStoreTXTest}.
 */
@ComponentPgTest
public class PostgresQuartzJobStoreTXTest
    extends AbstractComponentPgTest
{
  // Actually an instance of TestQuartzJobStoreTx (bound for injection in the shared test configuration).
  @Inject
  private QuartzJobStoreTX quartzJobStoreTX;

  @Inject
  private TaskScheduler taskScheduler;

  @BeforeEach
  public void before() {
    taskScheduler.createScheduler();
  }

  @Test
  public void testInitialize_Postgres() throws Exception {
    quartzJobStoreTX.initialize();

    assertThat(quartzJobStoreTX.getDriverDelegateClass()).isEqualTo(QuartzPostgreSQLDelegate.class.getName());
    assertThat(quartzJobStoreTX.isClustered()).isTrue();
    assertJobStoreTX(quartzJobStoreTX);
  }

  private void assertJobStoreTX(JobStoreTX jobStoreTX) {
    assertThat(jobStoreTX.getDataSource()).isEqualTo("ods");
    assertThat(jobStoreTX.getTablePrefix()).isEqualTo(OperationalDataStore.ID + ".QRTZ_");
    assertThat(jobStoreTX.canUseProperties()).isTrue();
    assertThat(jobStoreTX.getClusterCheckinInterval()).isEqualTo(QuartzJobStoreTX.CLUSTER_CHECKIN_INTERVAL_MILLIS);
  }
}
