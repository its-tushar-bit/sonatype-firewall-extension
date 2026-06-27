/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.continuousmonitoring;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.continuousmonitoring.ContinuousMonitoringHostedRepoItemDAO;
import com.sonatype.insight.brain.dataaccess.continuousmonitoring.ContinuousMonitoringQueueItemDAO;
import com.sonatype.insight.brain.db.AbstractMultiTenantDatabaseTest;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringFlowType;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringHostedRepoItem;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringQueueItem;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ContinuousMonitoringHostedRepoItem.CONTINUOUS_MONITORING_HOSTED_REPO_ITEM;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * MTIQ tenant-isolation IT for the unified continuous monitoring queue (CLM-40039 §10 / DoD).
 * Verifies that rows enqueued under one tenant are completely invisible to another tenant —
 * countPending, acquirePending, and the satellite lookup all observe only the active tenant's
 * rows. This is the regression net for the schema-per-tenant assumption that the queue inherits
 * by virtue of running through {@link com.sonatype.insight.brain.db.datastore.OperationalDataStore}.
 */
@Category(SlowTest.class)
public class ContinuousMonitoringQueueMultiTenantTest
    extends AbstractMultiTenantDatabaseTest
{
  @Test
  public void rowsEnqueuedInOneTenantAreInvisibleToAnother() {
    String idA = "qid-tenant-a";
    String idB = "qid-tenant-b";

    testAsNewTenant(tenantA -> insert(idA, "repo-a", "hash-a"));
    testAsNewTenant(tenantB -> {
      insert(idB, "repo-b", "hash-b");

      ContinuousMonitoringQueueItemDAO dao = daoFactory.createContinuousMonitoringQueueItemDAO();
      ContinuousMonitoringHostedRepoItemDAO satelliteDAO = daoFactory.createContinuousMonitoringHostedRepoItemDAO();
      assertThat(dao.countPending(ContinuousMonitoringFlowType.HOSTED_REPO)).isEqualTo(1L);

      List<ContinuousMonitoringQueueItem> acquired =
          dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-b", 10);
      assertThat(acquired).extracting(ContinuousMonitoringQueueItem::getId).containsExactly(idB);

      try (TransactionContext tx = satelliteDAO.createTransactionContext()) {
        tx.begin();
        List<ContinuousMonitoringHostedRepoItem> sats = satelliteDAO.getByQueueIds(tx, List.of(idA, idB));
        tx.commit();
        // Tenant A's id is not present in this tenant's satellite table; only the local row resolves.
        assertThat(sats).extracting(ContinuousMonitoringHostedRepoItem::getQueueId).containsExactly(idB);
      }
    });
  }

  @Test
  public void deleteInOneTenantDoesNotAffectAnother() {
    String idA = "qid-isolation-a";
    String idB = "qid-isolation-b";

    // Insert row under tenantA
    testAsNewTenant(tenantA -> insert(idA, "repo-a", "hash-a"));

    // Insert row under tenantB, delete tenantA's row while operating as tenantB,
    // then assert tenantB's row is still present.
    testAsNewTenant(tenantB -> {
      insert(idB, "repo-b", "hash-b");

      ContinuousMonitoringQueueItemDAO dao = daoFactory.createContinuousMonitoringQueueItemDAO();
      assertThat(dao.countPending(ContinuousMonitoringFlowType.HOSTED_REPO)).isEqualTo(1L);

      // Attempt to delete tenantA's row while operating as tenantB (should be a no-op
      // due to tenant isolation — the row doesn't exist in this tenant's schema).
      int deleted = dao.deleteById(idA, "worker-b");
      assertThat(deleted).isZero();

      // Verify tenantB's row is still present after the failed cross-tenant delete.
      assertThat(dao.countPending(ContinuousMonitoringFlowType.HOSTED_REPO)).isEqualTo(1L);
      List<ContinuousMonitoringQueueItem> acquired =
          dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-b", 10);
      assertThat(acquired).extracting(ContinuousMonitoringQueueItem::getId).containsExactly(idB);
    });
  }

  /**
   * Verifies {@code resetInProgressToPending} is scoped to the active tenant. The method issues an
   * unfiltered {@code UPDATE ... WHERE status = 'IN_PROGRESS'} with no id predicate; if schema
   * routing ever broke, this bulk operation could reset rows across tenant boundaries. Two tenants
   * each acquire (move to IN_PROGRESS) their own row; resetting under tenantB must leave tenantA's
   * row untouched.
   */
  @Test
  public void resetInProgressToPendingInOneTenantDoesNotAffectAnother() {
    String idA = "qid-reset-a";
    String idB = "qid-reset-b";

    // Insert and acquire (move to IN_PROGRESS) under tenantA; row stays IN_PROGRESS at tenant exit.
    // Capture the Tenant so we can re-enter the SAME schema in the verification block below.
    Tenant tenantA = testAsNewTenant(t -> {
      insert(idA, "repo-a", "hash-a");
      ContinuousMonitoringQueueItemDAO daoA = daoFactory.createContinuousMonitoringQueueItemDAO();
      List<ContinuousMonitoringQueueItem> acquiredA =
          daoA.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-a", 10);
      assertThat(acquiredA).extracting(ContinuousMonitoringQueueItem::getId).containsExactly(idA);
      assertThat(daoA.countPending(ContinuousMonitoringFlowType.HOSTED_REPO)).isZero();
    });

    // Under tenantB, insert + acquire its own row, then reset. tenantA's IN_PROGRESS row must
    // remain untouched because the reset is scoped to tenantB's schema.
    testAsNewTenant(tenantB -> {
      insert(idB, "repo-b", "hash-b");
      ContinuousMonitoringQueueItemDAO daoB = daoFactory.createContinuousMonitoringQueueItemDAO();
      daoB.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-b", 10);
      assertThat(daoB.countPending(ContinuousMonitoringFlowType.HOSTED_REPO)).isZero();

      int reset = daoB.resetInProgressToPending("worker-b");
      assertThat(reset).isEqualTo(1);
      // Only tenantB's own row was reset back to PENDING.
      assertThat(daoB.countPending(ContinuousMonitoringFlowType.HOSTED_REPO)).isEqualTo(1L);
      List<ContinuousMonitoringQueueItem> reAcquired =
          daoB.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-b", 10);
      assertThat(reAcquired).extracting(ContinuousMonitoringQueueItem::getId).containsExactly(idB);
    });

    // Belt-and-suspenders: re-enter the SAME tenantA schema (not a new tenant) to verify its
    // IN_PROGRESS row was NOT reset by tenantB's bulk operation. This closes the highest-impact
    // failure mode where a broken implementation could return the correct result to tenantB
    // (reset=1) while silently corrupting tenantA's state as a side-effect.
    testAsTenant(tenantA, t -> {
      ContinuousMonitoringQueueItemDAO daoA = daoFactory.createContinuousMonitoringQueueItemDAO();
      assertThat(daoA.countPending(ContinuousMonitoringFlowType.HOSTED_REPO)).isZero(); // still IN_PROGRESS
      List<ContinuousMonitoringQueueItem> acquiredA =
          daoA.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-a", 10);
      assertThat(acquiredA).isEmpty(); // nothing in PENDING to acquire
    });
  }

  /**
   * Inserts a parent + satellite pair using the producer-side orchestration sequence — same
   * three calls (insertBatch + insertIgnoreDuplicateKey + deleteOrphanParentsForSatelliteTable)
   * the production producer uses, executed atomically on a single tenant-scoped transaction.
   */
  private void insert(final String queueId, final String repositoryId, final String componentHash) {
    ContinuousMonitoringQueueItemDAO queueItemDAO = daoFactory.createContinuousMonitoringQueueItemDAO();
    ContinuousMonitoringHostedRepoItemDAO hostedRepoItemDAO = daoFactory.createContinuousMonitoringHostedRepoItemDAO();
    // priority=0 / createTime=now: not material to isolation; chosen arbitrarily
    Date now = new Date();
    ContinuousMonitoringQueueItem parent =
        new ContinuousMonitoringQueueItem(queueId, ContinuousMonitoringFlowType.HOSTED_REPO, 0L, now);
    ContinuousMonitoringHostedRepoItem satellite =
        new ContinuousMonitoringHostedRepoItem(queueId, repositoryId, componentHash);
    try (TransactionContext tx = queueItemDAO.createTransactionContext()) {
      tx.begin();
      queueItemDAO.insertBatch(tx, List.of(parent), false);
      hostedRepoItemDAO.insertIgnoreDuplicateKey(tx, List.of(satellite));
      queueItemDAO.deleteOrphanParentsForSatelliteTable(
          tx,
          List.of(queueId),
          CONTINUOUS_MONITORING_HOSTED_REPO_ITEM.QUEUE_ID);
      tx.commit();
    }
  }
}
