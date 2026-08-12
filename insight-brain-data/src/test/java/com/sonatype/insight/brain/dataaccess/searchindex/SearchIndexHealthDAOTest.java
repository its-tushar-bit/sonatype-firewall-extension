/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.searchindex;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.searchindex.SearchIndexHealth;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SearchIndexHealthDAOTest
    extends AbstractDbDAOTest
{
  private SearchIndexHealthDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = new SearchIndexHealthDAO(databaseRule.getOperationalDataStore());
    resetCurrent();
  }

  @Test
  public void recordAbandonedChanges_accumulatesAndOpensTheWindowOnce() {
    dao.recordAbandonedChanges(2L);
    Date openedAt = dao.getCurrent().getFailedChangeWindowStart();
    assertThat(openedAt).isNotNull();

    dao.recordAbandonedChanges(3L);

    SearchIndexHealth health = dao.getCurrent();
    assertThat(health.getFailedChangeCount()).isEqualTo(5L);
    assertThat(health.getFailedChangeWindowStart()).as("window start marks the first failure, not the latest")
        .isEqualTo(openedAt);
  }

  @Test
  public void recordAbandonedChanges_ignoresBatchesThatAbandonedNothing() {
    dao.recordAbandonedChanges(0L);

    SearchIndexHealth health = dao.getCurrent();
    assertThat(health.getFailedChangeCount()).isZero();
    assertThat(health.getFailedChangeWindowStart()).isNull();
  }

  /**
   * Queue depth is counted out of the outbox and handed to this write, so the stored gauge tracks
   * whatever the caller measured rather than a running total that can drift from the rows.
   */
  @Test
  public void updateDerivedStatus_storesTheMeasuredDepthAndPointer() {
    Date oldest = new Date(System.currentTimeMillis() - 60_000L);

    dao.updateDerivedStatus(SearchIndexHealth.STATUS_WARNING, SearchIndexHealth.OP_NONE, 60L, "job-1", 7L, oldest);

    SearchIndexHealth health = dao.getCurrent();
    assertThat(health.getHealthStatus()).isEqualTo(SearchIndexHealth.STATUS_WARNING);
    assertThat(health.getQueueLagSeconds()).isEqualTo(60L);
    assertThat(health.getActiveJobId()).isEqualTo("job-1");
    assertThat(health.getPendingChangeCount()).isEqualTo(7L);
    assertThat(health.getOldestPendingCreatedAt().getTime()).isEqualTo(oldest.getTime());
  }

  /**
   * A drained queue has to clear the pointer, otherwise lag keeps being measured from a change that
   * has already been applied and the tenant never returns to healthy.
   */
  @Test
  public void updateDerivedStatus_clearsThePointerWhenNothingIsPending() {
    dao.updateDerivedStatus(SearchIndexHealth.STATUS_WARNING, SearchIndexHealth.OP_NONE, 60L, null, 1L,
        new Date(System.currentTimeMillis() - 60_000L));

    dao.updateDerivedStatus(SearchIndexHealth.STATUS_HEALTHY, SearchIndexHealth.OP_NONE, 0L, null, 0L, null);

    SearchIndexHealth health = dao.getCurrent();
    assertThat(health.getPendingChangeCount()).isZero();
    assertThat(health.getOldestPendingCreatedAt()).isNull();
  }

  /**
   * A rebuild reconstructs every document from source, so the failed tally has to clear. Without
   * this the count only grows and the tenant stays unhealthy for the life of the install.
   */
  @Test
  public void resetFailedChanges_letsHealthRecoverAfterARebuild() {
    dao.recordAbandonedChanges(1L);
    assertThat(dao.getCurrent().getFailedChangeCount()).isEqualTo(1L);

    dao.resetFailedChanges();

    SearchIndexHealth health = dao.getCurrent();
    assertThat(health.getFailedChangeCount()).isZero();
    assertThat(health.getFailedChangeWindowStart()).isNull();
  }

  /**
   * The CURRENT row is initial data installed by the migration rather than something a test owns, so
   * re-creating it is not a leak. This mirrors how {@code TemporaryEntity} already exempts migration
   * trackers and system configuration properties that get re-inserted during a test.
   */
  @After
  public void forgetTheReseededSingleton() {
    AbstractOperationalSqlDAO.testEntityLeaksDetectionData.remove(SearchIndexHealth.CURRENT_ID);
  }

  /**
   * A schema provisioned without this feature's incremental has no CURRENT row, which used to fail
   * every health read. Seeding on demand keeps those installs serving instead of erroring.
   */
  @Test
  public void getOrSeedCurrent_seedsTheRowWhenTheSchemaHasNone() {
    dao.delete(dao.getCurrent());
    assertThat(dao.getCurrent()).isNull();

    SearchIndexHealth seeded = dao.getOrSeedCurrent();

    assertThat(seeded).isNotNull();
    assertThat(seeded.getHealthStatus()).isEqualTo(SearchIndexHealth.STATUS_HEALTHY);
    assertThat(seeded.getNouxUnlockState()).isEqualTo(SearchIndexHealth.UNLOCK_NOT_STARTED);
    assertThat(seeded.getPendingChangeCount()).isZero();
    assertThat(dao.getCurrent()).as("seed is durable, not just returned").isNotNull();
  }

  @Test
  public void getOrSeedCurrent_leavesAnExistingRowAlone() {
    dao.recordAbandonedChanges(4L);

    SearchIndexHealth health = dao.getOrSeedCurrent();

    assertThat(health.getFailedChangeCount()).as("existing state is not reset by a seed attempt")
        .isEqualTo(4L);
  }

  private void resetCurrent() {
    SearchIndexHealth health = dao.getCurrent();
    assertThat(health).as("schema seed CURRENT row").isNotNull();
    health.setPendingChangeCount(0);
    health.setFailedChangeCount(0);
    health.setFailedChangeWindowStart(null);
    health.setOldestPendingCreatedAt(null);
    health.setQueueLagSeconds(0);
    health.setHealthStatus(SearchIndexHealth.STATUS_HEALTHY);
    health.setRecommendedOp(SearchIndexHealth.OP_NONE);
    health.setActiveJobId(null);
    health.setUpdatedAt(new Date());
    dao.update(health);
  }
}
