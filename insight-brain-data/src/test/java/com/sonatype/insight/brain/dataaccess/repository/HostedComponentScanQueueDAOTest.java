/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.repository.HostedComponentScanQueue;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;

import com.sonatype.insight.brain.dataaccess.AbstractSqlDAO;

import static org.assertj.core.api.Assertions.assertThat;

public class HostedComponentScanQueueDAOTest
    extends AbstractDbDAOTest
{
  private HostedComponentScanQueueDAO hostedComponentScanQueueDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    hostedComponentScanQueueDAO = daoFactory.createHostedComponentScanQueueDAO();
  }

  @Test
  public void testCRUD() {
    // Create repository and component first for FK constraint
    final Repository repo = tempEntity.newRepository("repo-1");
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());

    final HostedComponentScanQueue queueEntry = new HostedComponentScanQueue();
    queueEntry.setComponentId(component.getId());
    queueEntry.setScanFileId("scan-file-1");
    queueEntry.setStatus(HostedComponentScanQueueDAO.Status.PENDING.name());
    queueEntry.setPriority(5);
    queueEntry.setRepositoryId(repo.getId());

    // Insert
    hostedComponentScanQueueDAO.insert(queueEntry);
    String queueId = queueEntry.getId();
    assertThat(queueId).isNotNull();

    // Get
    final HostedComponentScanQueue result = hostedComponentScanQueueDAO.getById(queueId);
    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo(HostedComponentScanQueueDAO.Status.PENDING.name());
    assertThat(result.getPriority()).isEqualTo(5);

    // Update
    queueEntry.setStatus(HostedComponentScanQueueDAO.Status.IN_PROGRESS.name());
    hostedComponentScanQueueDAO.update(queueEntry);

    final HostedComponentScanQueue result2 = hostedComponentScanQueueDAO.getById(queueId);
    assertThat(result2).isNotNull();
    assertThat(result2.getStatus()).isEqualTo(HostedComponentScanQueueDAO.Status.IN_PROGRESS.name());

    // Delete
    hostedComponentScanQueueDAO.delete(queueEntry);
    assertThat(hostedComponentScanQueueDAO.getById(queueId)).isNull();
  }

  @Test
  public void testGetByStatus() {
    // Create repository and component first for FK constraint
    final Repository repo = tempEntity.newRepository("repo-1");
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());

    // Create queue entries with different statuses
    final HostedComponentScanQueue pending1 = new HostedComponentScanQueue();
    pending1.setComponentId(component.getId());
    pending1.setScanFileId("scan-1");
    pending1.setStatus(HostedComponentScanQueueDAO.Status.PENDING.name());
    pending1.setPriority(5);
    pending1.setRepositoryId(repo.getId());
    hostedComponentScanQueueDAO.insert(pending1);

    final HostedComponentScanQueue pending2 = new HostedComponentScanQueue();
    pending2.setComponentId(component.getId());
    pending2.setScanFileId("scan-2");
    pending2.setStatus(HostedComponentScanQueueDAO.Status.PENDING.name());
    pending2.setPriority(3);
    pending2.setRepositoryId(repo.getId());
    hostedComponentScanQueueDAO.insert(pending2);

    final HostedComponentScanQueue completed = new HostedComponentScanQueue();
    completed.setComponentId(component.getId());
    completed.setScanFileId("scan-3");
    completed.setStatus(HostedComponentScanQueueDAO.Status.COMPLETED.name());
    completed.setPriority(5);
    completed.setRepositoryId(repo.getId());
    hostedComponentScanQueueDAO.insert(completed);

    try (TransactionContext tx = hostedComponentScanQueueDAO.createTransactionContext()) {
      final List<HostedComponentScanQueue> results =
          hostedComponentScanQueueDAO.getByStatus(tx, HostedComponentScanQueueDAO.Status.PENDING);
      assertThat(results).hasSize(2);
    }
  }

  @Test
  public void testAcquireJob() {
    final Repository repo = tempEntity.newRepository("repo-1");
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());

    final HostedComponentScanQueue queueEntry = new HostedComponentScanQueue();
    queueEntry.setComponentId(component.getId());
    queueEntry.setScanFileId("scan-1");
    queueEntry.setStatus(HostedComponentScanQueueDAO.Status.PENDING.name());
    queueEntry.setPriority(5);
    queueEntry.setRepositoryId(repo.getId());
    hostedComponentScanQueueDAO.insert(queueEntry);

    List<HostedComponentScanQueue> acquired = hostedComponentScanQueueDAO.acquireNextPendingJobs(1);

    assertThat(acquired).hasSize(1);
    assertThat(acquired.get(0).getId()).isEqualTo(queueEntry.getId());
    assertThat(acquired.get(0).getStatus()).isEqualTo(HostedComponentScanQueueDAO.Status.IN_PROGRESS.name());
    assertThat(acquired.get(0).getAcquiredAt()).isNotNull();
  }

  @Test
  public void testCompleteJob() {
    // Create repository and component first for FK constraint
    final Repository repo = tempEntity.newRepository("repo-1");
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());

    final HostedComponentScanQueue queueEntry = new HostedComponentScanQueue();
    queueEntry.setComponentId(component.getId());
    queueEntry.setScanFileId("scan-1");
    queueEntry.setStatus(HostedComponentScanQueueDAO.Status.IN_PROGRESS.name());
    queueEntry.setPriority(5);
    queueEntry.setRepositoryId(repo.getId());
    hostedComponentScanQueueDAO.insert(queueEntry);

    try (TransactionContext tx = hostedComponentScanQueueDAO.createTransactionContext()) {
      int updated = hostedComponentScanQueueDAO.completeJob(tx, queueEntry.getId());
      assertThat(updated).isEqualTo(1);
      tx.commit();
    }

    final HostedComponentScanQueue result = hostedComponentScanQueueDAO.getById(queueEntry.getId());
    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name());
  }

  @Test
  public void testFailJob() {
    // Create repository and component first for FK constraint
    final Repository repo = tempEntity.newRepository("repo-1");
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());

    final HostedComponentScanQueue queueEntry = new HostedComponentScanQueue();
    queueEntry.setComponentId(component.getId());
    queueEntry.setScanFileId("scan-1");
    queueEntry.setStatus(HostedComponentScanQueueDAO.Status.IN_PROGRESS.name());
    queueEntry.setPriority(5);
    queueEntry.setRepositoryId(repo.getId());
    hostedComponentScanQueueDAO.insert(queueEntry);

    try (TransactionContext tx = hostedComponentScanQueueDAO.createTransactionContext()) {
      int updated = hostedComponentScanQueueDAO.failJob(tx, queueEntry.getId(), "Test error message");
      assertThat(updated).isEqualTo(1);
      tx.commit();
    }

    final HostedComponentScanQueue result = hostedComponentScanQueueDAO.getById(queueEntry.getId());
    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo(HostedComponentScanQueueDAO.Status.FAILED.name());
    assertThat(result.getErrorMessage()).isEqualTo("Test error message");
  }

  @Test
  public void testInvalidStateTransition_CompletedToInProgress() {
    final Repository repo = tempEntity.newRepository("repo-1");
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());

    // Insert a COMPLETED job — acquireNextPendingJobs must not pick it up
    final HostedComponentScanQueue queueEntry = new HostedComponentScanQueue();
    queueEntry.setComponentId(component.getId());
    queueEntry.setScanFileId("scan-1");
    queueEntry.setStatus(HostedComponentScanQueueDAO.Status.COMPLETED.name());
    queueEntry.setPriority(5);
    queueEntry.setRepositoryId(repo.getId());
    hostedComponentScanQueueDAO.insert(queueEntry);

    List<HostedComponentScanQueue> acquired = hostedComponentScanQueueDAO.acquireNextPendingJobs(10);
    assertThat(acquired).isEmpty();

    final HostedComponentScanQueue result = hostedComponentScanQueueDAO.getById(queueEntry.getId());
    assertThat(result.getStatus()).isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name());
  }

  @Test
  public void testConcurrentJobAcquisition_ShouldNotReturnSameJob() {
    final Repository repo = tempEntity.newRepository("repo-1");
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());

    final HostedComponentScanQueue queueEntry = new HostedComponentScanQueue();
    queueEntry.setComponentId(component.getId());
    queueEntry.setScanFileId("scan-1");
    queueEntry.setStatus(HostedComponentScanQueueDAO.Status.PENDING.name());
    queueEntry.setPriority(5);
    queueEntry.setRepositoryId(repo.getId());
    hostedComponentScanQueueDAO.insert(queueEntry);

    // First acquisition picks up the job
    List<HostedComponentScanQueue> first = hostedComponentScanQueueDAO.acquireNextPendingJobs(1);
    assertThat(first).hasSize(1);
    assertThat(first.get(0).getId()).isEqualTo(queueEntry.getId());

    // Second acquisition finds nothing — job is already IN_PROGRESS
    List<HostedComponentScanQueue> second = hostedComponentScanQueueDAO.acquireNextPendingJobs(1);
    assertThat(second).isEmpty();
  }

  @Test
  public void testAcquireNextPendingJobs_RespectsPriorityOrdering() {
    final Repository repo = tempEntity.newRepository("repo-1");
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());

    final HostedComponentScanQueue lowPriority = new HostedComponentScanQueue();
    lowPriority.setComponentId(component.getId());
    lowPriority.setScanFileId("scan-low");
    lowPriority.setStatus(HostedComponentScanQueueDAO.Status.PENDING.name());
    lowPriority.setPriority(1);
    lowPriority.setRepositoryId(repo.getId());
    hostedComponentScanQueueDAO.insert(lowPriority);

    final HostedComponentScanQueue mediumPriority = new HostedComponentScanQueue();
    mediumPriority.setComponentId(component.getId());
    mediumPriority.setScanFileId("scan-medium");
    mediumPriority.setStatus(HostedComponentScanQueueDAO.Status.PENDING.name());
    mediumPriority.setPriority(5);
    mediumPriority.setRepositoryId(repo.getId());
    hostedComponentScanQueueDAO.insert(mediumPriority);

    final HostedComponentScanQueue highPriority = new HostedComponentScanQueue();
    highPriority.setComponentId(component.getId());
    highPriority.setScanFileId("scan-high");
    highPriority.setStatus(HostedComponentScanQueueDAO.Status.PENDING.name());
    highPriority.setPriority(10);
    highPriority.setRepositoryId(repo.getId());
    hostedComponentScanQueueDAO.insert(highPriority);

    List<HostedComponentScanQueue> results = hostedComponentScanQueueDAO.acquireNextPendingJobs(10);
    assertThat(results).hasSize(3);
    assertThat(results.get(0).getPriority()).isEqualTo(1);
    assertThat(results.get(1).getPriority()).isEqualTo(5);
    assertThat(results.get(2).getPriority()).isEqualTo(10);
    // All acquired atomically — status is IN_PROGRESS
    results.forEach(r -> assertThat(r.getStatus())
        .isEqualTo(HostedComponentScanQueueDAO.Status.IN_PROGRESS.name()));
  }

  @Test
  public void testDeleteCompletedJobs_WithExactDateBoundary() {
    // Create repository and component first for FK constraint
    final Repository repo = tempEntity.newRepository("repo-1");
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());

    // Create completed jobs with specific acquired dates
    Instant oldDate = Instant.now().minusSeconds(7 * 24 * 60 * 60); // 7 days ago
    Instant recentDate = Instant.now().minusSeconds(1 * 24 * 60 * 60); // 1 day ago

    final HostedComponentScanQueue oldJob = new HostedComponentScanQueue();
    oldJob.setComponentId(component.getId());
    oldJob.setScanFileId("scan-old");
    oldJob.setStatus(HostedComponentScanQueueDAO.Status.COMPLETED.name());
    oldJob.setPriority(5);
    oldJob.setRepositoryId(repo.getId());
    oldJob.setAcquiredAt(Date.from(oldDate));
    hostedComponentScanQueueDAO.insert(oldJob);

    final HostedComponentScanQueue recentJob = new HostedComponentScanQueue();
    recentJob.setComponentId(component.getId());
    recentJob.setScanFileId("scan-recent");
    recentJob.setStatus(HostedComponentScanQueueDAO.Status.COMPLETED.name());
    recentJob.setPriority(5);
    recentJob.setRepositoryId(repo.getId());
    recentJob.setAcquiredAt(Date.from(recentDate));
    hostedComponentScanQueueDAO.insert(recentJob);

    // Delete jobs older than 3 days
    Instant threshold = Instant.now().minusSeconds(3 * 24 * 60 * 60);
    try (TransactionContext tx = hostedComponentScanQueueDAO.createTransactionContext()) {
      int deleted = hostedComponentScanQueueDAO.deleteCompletedAndFailedJobs(tx, threshold);
      assertThat(deleted).isEqualTo(1); // Only old job deleted
      tx.commit();
    }

    // Verify old job deleted, recent job remains
    assertThat(hostedComponentScanQueueDAO.getById(oldJob.getId())).isNull();
    assertThat(hostedComponentScanQueueDAO.getById(recentJob.getId())).isNotNull();
  }

  @Test
  public void testDeleteCompletedJobs_WithNullAcquiredAt() {
    // Create repository and component first for FK constraint
    final Repository repo = tempEntity.newRepository("repo-1");
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());

    // Create a FAILED job that was never acquired (acquired_at is null)
    final HostedComponentScanQueue failedJob = new HostedComponentScanQueue();
    failedJob.setComponentId(component.getId());
    failedJob.setScanFileId("scan-failed");
    failedJob.setStatus(HostedComponentScanQueueDAO.Status.FAILED.name());
    failedJob.setPriority(5);
    failedJob.setRepositoryId(repo.getId());
    // Intentionally do not set acquiredAt - simulates failure before acquisition
    hostedComponentScanQueueDAO.insert(failedJob);

    // Create a PENDING job (should NOT be deleted)
    final HostedComponentScanQueue pendingJob = new HostedComponentScanQueue();
    pendingJob.setComponentId(component.getId());
    pendingJob.setScanFileId("scan-pending");
    pendingJob.setStatus(HostedComponentScanQueueDAO.Status.PENDING.name());
    pendingJob.setPriority(5);
    pendingJob.setRepositoryId(repo.getId());
    hostedComponentScanQueueDAO.insert(pendingJob);

    // Delete all completed/failed jobs
    Instant threshold = Instant.now().plusSeconds(1); // Future instant to catch all
    try (TransactionContext tx = hostedComponentScanQueueDAO.createTransactionContext()) {
      int deleted = hostedComponentScanQueueDAO.deleteCompletedAndFailedJobs(tx, threshold);
      assertThat(deleted).isEqualTo(1); // Only failed job deleted
      tx.commit();
    }

    // Verify FAILED job with null acquired_at was deleted
    assertThat(hostedComponentScanQueueDAO.getById(failedJob.getId())).isNull();
    // Verify PENDING job was NOT deleted
    assertThat(hostedComponentScanQueueDAO.getById(pendingJob.getId())).isNotNull();
  }

  @Test
  public void testAcquireNextPendingJobs_AcquiresAndTransitionsToInProgress() {
    final Repository repo = tempEntity.newRepository("repo-1");
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());

    final HostedComponentScanQueue job1 = new HostedComponentScanQueue();
    job1.setComponentId(component.getId());
    job1.setScanFileId("scan-1");
    job1.setStatus(HostedComponentScanQueueDAO.Status.PENDING.name());
    job1.setPriority(10);
    job1.setRepositoryId(repo.getId());
    hostedComponentScanQueueDAO.insert(job1);

    final HostedComponentScanQueue job2 = new HostedComponentScanQueue();
    job2.setComponentId(component.getId());
    job2.setScanFileId("scan-2");
    job2.setStatus(HostedComponentScanQueueDAO.Status.PENDING.name());
    job2.setPriority(5);
    job2.setRepositoryId(repo.getId());
    hostedComponentScanQueueDAO.insert(job2);

    Instant acquiredAt = Instant.now();
    try (TransactionContext tx = hostedComponentScanQueueDAO.createTransactionContext()) {
      tx.begin();
      List<HostedComponentScanQueue> acquired = hostedComponentScanQueueDAO.acquireNextPendingJobs(tx, 2, acquiredAt);
      tx.commit();

      assertThat(acquired).hasSize(2);
      assertThat(acquired.get(0).getPriority()).isEqualTo(5);
      assertThat(acquired.get(1).getPriority()).isEqualTo(10);
    }

    HostedComponentScanQueue result1 = hostedComponentScanQueueDAO.getById(job1.getId());
    assertThat(result1.getStatus()).isEqualTo(HostedComponentScanQueueDAO.Status.IN_PROGRESS.name());
    assertThat(result1.getAcquiredAt()).isNotNull();

    HostedComponentScanQueue result2 = hostedComponentScanQueueDAO.getById(job2.getId());
    assertThat(result2.getStatus()).isEqualTo(HostedComponentScanQueueDAO.Status.IN_PROGRESS.name());
  }

  @Test
  public void testAcquireNextPendingJobs_RespectsLimit() {
    final Repository repo = tempEntity.newRepository("repo-1");
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());

    for (int i = 0; i < 5; i++) {
      final HostedComponentScanQueue job = new HostedComponentScanQueue();
      job.setComponentId(component.getId());
      job.setScanFileId("scan-" + i);
      job.setStatus(HostedComponentScanQueueDAO.Status.PENDING.name());
      job.setPriority(i);
      job.setRepositoryId(repo.getId());
      hostedComponentScanQueueDAO.insert(job);
    }

    Instant acquiredAt = Instant.now();
    try (TransactionContext tx = hostedComponentScanQueueDAO.createTransactionContext()) {
      tx.begin();
      List<HostedComponentScanQueue> acquired = hostedComponentScanQueueDAO.acquireNextPendingJobs(tx, 2, acquiredAt);
      tx.commit();

      assertThat(acquired).hasSize(2);
    }

    try (TransactionContext tx = hostedComponentScanQueueDAO.createTransactionContext()) {
      List<HostedComponentScanQueue> stillPending =
          hostedComponentScanQueueDAO.getByStatus(tx, HostedComponentScanQueueDAO.Status.PENDING);
      assertThat(stillPending).hasSize(3);
    }
  }

  @Test
  public void testAcquireNextPendingJobs_ReturnsEmptyWhenNoPendingJobs() {
    Instant acquiredAt = Instant.now();
    try (TransactionContext tx = hostedComponentScanQueueDAO.createTransactionContext()) {
      tx.begin();
      List<HostedComponentScanQueue> acquired = hostedComponentScanQueueDAO.acquireNextPendingJobs(tx, 5, acquiredAt);
      tx.commit();

      assertThat(acquired).isEmpty();
    }
  }

  @Test
  public void testAcquireNextPendingJobs_ReturnsEmptyForZeroLimit() {
    final Repository repo = tempEntity.newRepository("repo-limit-zero");
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());
    insertJob(component, repo, "scan-z", HostedComponentScanQueueDAO.Status.PENDING, 5);

    assertThat(hostedComponentScanQueueDAO.acquireNextPendingJobs(0)).isEmpty();
  }

  @Test
  public void testAcquireNextPendingJobs_ReturnsEmptyForNegativeLimit() {
    final Repository repo = tempEntity.newRepository("repo-limit-neg");
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());
    insertJob(component, repo, "scan-n", HostedComponentScanQueueDAO.Status.PENDING, 5);

    assertThat(hostedComponentScanQueueDAO.acquireNextPendingJobs(-1)).isEmpty();
  }

  @Test
  public void testAcquireNextPendingJobs_SkipsNonPendingJobs() {
    final Repository repo = tempEntity.newRepository("repo-1");
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());

    final HostedComponentScanQueue inProgress = new HostedComponentScanQueue();
    inProgress.setComponentId(component.getId());
    inProgress.setScanFileId("scan-ip");
    inProgress.setStatus(HostedComponentScanQueueDAO.Status.IN_PROGRESS.name());
    inProgress.setPriority(10);
    inProgress.setRepositoryId(repo.getId());
    hostedComponentScanQueueDAO.insert(inProgress);

    final HostedComponentScanQueue pending = new HostedComponentScanQueue();
    pending.setComponentId(component.getId());
    pending.setScanFileId("scan-pending");
    pending.setStatus(HostedComponentScanQueueDAO.Status.PENDING.name());
    pending.setPriority(5);
    pending.setRepositoryId(repo.getId());
    hostedComponentScanQueueDAO.insert(pending);

    Instant acquiredAt = Instant.now();
    try (TransactionContext tx = hostedComponentScanQueueDAO.createTransactionContext()) {
      tx.begin();
      List<HostedComponentScanQueue> acquired = hostedComponentScanQueueDAO.acquireNextPendingJobs(tx, 5, acquiredAt);
      tx.commit();

      assertThat(acquired).hasSize(1);
      assertThat(acquired.get(0).getId()).isEqualTo(pending.getId());
    }
  }

  @Test
  public void testUnacquireJobs_EmptySetIsNoOp() {
    final Repository repo = tempEntity.newRepository("repo-unacquire-empty");
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());
    HostedComponentScanQueue job = insertJob(component, repo, "scan-ue",
        HostedComponentScanQueueDAO.Status.IN_PROGRESS, 5);

    hostedComponentScanQueueDAO.unacquireJobs(Set.of());

    assertThat(hostedComponentScanQueueDAO.getById(job.getId()).getStatus())
        .isEqualTo(HostedComponentScanQueueDAO.Status.IN_PROGRESS.name());
  }

  @Test
  public void testUnacquireJobs_OnlyAffectsInProgressJobs() {
    final Repository repo = tempEntity.newRepository("repo-unacquire-selective");
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());

    HostedComponentScanQueue inProgress = insertJob(component, repo, "scan-ip2",
        HostedComponentScanQueueDAO.Status.IN_PROGRESS, 5);
    HostedComponentScanQueue pending = insertJob(component, repo, "scan-p2",
        HostedComponentScanQueueDAO.Status.PENDING, 5);
    HostedComponentScanQueue completed = insertJob(component, repo, "scan-c2",
        HostedComponentScanQueueDAO.Status.COMPLETED, 5);

    hostedComponentScanQueueDAO.unacquireJobs(
        Set.of(inProgress.getId(), pending.getId(), completed.getId()));

    assertThat(hostedComponentScanQueueDAO.getById(inProgress.getId()).getStatus())
        .isEqualTo(HostedComponentScanQueueDAO.Status.PENDING.name());
    assertThat(hostedComponentScanQueueDAO.getById(pending.getId()).getStatus())
        .isEqualTo(HostedComponentScanQueueDAO.Status.PENDING.name());
    assertThat(hostedComponentScanQueueDAO.getById(completed.getId()).getStatus())
        .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name());
  }

  @Test
  public void testUnacquireJobs_LargeSetExceedingInOperatorThreshold() {
    final Repository repo = tempEntity.newRepository("repo-unacquire-large");
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());

    int count = AbstractSqlDAO.H2_IN_OPERATOR_THRESHOLD + 10;
    Set<String> ids = new HashSet<>();
    for (int i = 0; i < count; i++) {
      HostedComponentScanQueue job = insertJob(component, repo, "scan-large-" + i,
          HostedComponentScanQueueDAO.Status.IN_PROGRESS, 5);
      ids.add(job.getId());
    }

    // Should not throw — partition logic handles sets larger than IN operator threshold
    hostedComponentScanQueueDAO.unacquireJobs(ids);

    // Spot-check a few — all should be PENDING
    ids.stream()
        .limit(5)
        .forEach(id -> assertThat(hostedComponentScanQueueDAO.getById(id).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.PENDING.name()));
  }

  @Test
  public void testIncrementRetryCount_IncrementsFromZero() {
    final Repository repo = tempEntity.newRepository("repo-retry-inc");
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());
    HostedComponentScanQueue job = insertJob(component, repo, "scan-retry",
        HostedComponentScanQueueDAO.Status.IN_PROGRESS, 5);

    assertThat(hostedComponentScanQueueDAO.getById(job.getId()).getRetryCount()).isEqualTo(0);

    int count1 = hostedComponentScanQueueDAO.incrementRetryCount(job.getId());
    assertThat(count1).isEqualTo(1);
    assertThat(hostedComponentScanQueueDAO.getById(job.getId()).getRetryCount()).isEqualTo(1);

    int count2 = hostedComponentScanQueueDAO.incrementRetryCount(job.getId());
    assertThat(count2).isEqualTo(2);
    assertThat(hostedComponentScanQueueDAO.getById(job.getId()).getRetryCount()).isEqualTo(2);
  }

  @Test
  public void testIncrementRetryCount_NonExistentJobReturnsZero() {
    int result = hostedComponentScanQueueDAO.incrementRetryCount("non-existent-id");
    assertThat(result).isEqualTo(0);
  }

  @Test
  public void testFailJob_TruncatesErrorMessageExceeding2000Chars() {
    final Repository repo = tempEntity.newRepository("repo-fail-trunc");
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());
    HostedComponentScanQueue job = insertJob(component, repo, "scan-fail-trunc",
        HostedComponentScanQueueDAO.Status.IN_PROGRESS, 5);

    String longMessage = "x".repeat(2500);
    hostedComponentScanQueueDAO.failJob(job.getId(), longMessage);

    HostedComponentScanQueue result = hostedComponentScanQueueDAO.getById(job.getId());
    assertThat(result.getErrorMessage()).hasSize(2000);
    assertThat(result.getStatus()).isEqualTo(HostedComponentScanQueueDAO.Status.FAILED.name());
  }

  @Test
  public void testFailJob_NullErrorMessageDoesNotThrow() {
    final Repository repo = tempEntity.newRepository("repo-fail-null");
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());
    HostedComponentScanQueue job = insertJob(component, repo, "scan-fail-null",
        HostedComponentScanQueueDAO.Status.IN_PROGRESS, 5);

    hostedComponentScanQueueDAO.failJob(job.getId(), null);

    HostedComponentScanQueue result = hostedComponentScanQueueDAO.getById(job.getId());
    assertThat(result.getStatus()).isEqualTo(HostedComponentScanQueueDAO.Status.FAILED.name());
    assertThat(result.getErrorMessage()).isNull();
  }

  @Test
  public void testUnacquireJobs_ResetsInProgressToPending() {
    final Repository repo = tempEntity.newRepository("repo-1");
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());

    final HostedComponentScanQueue job = new HostedComponentScanQueue();
    job.setComponentId(component.getId());
    job.setScanFileId("scan-1");
    job.setStatus(HostedComponentScanQueueDAO.Status.IN_PROGRESS.name());
    job.setPriority(5);
    job.setRepositoryId(repo.getId());
    job.setAcquiredAt(Date.from(Instant.now()));
    hostedComponentScanQueueDAO.insert(job);

    hostedComponentScanQueueDAO.unacquireJobs(Set.of(job.getId()));

    HostedComponentScanQueue result = hostedComponentScanQueueDAO.getById(job.getId());
    assertThat(result.getStatus()).isEqualTo(HostedComponentScanQueueDAO.Status.PENDING.name());
    assertThat(result.getAcquiredAt()).isNull();
  }

  @Test
  public void testUnacquireJobs_DoesNotAffectNonInProgressJobs() {
    final Repository repo = tempEntity.newRepository("repo-1");
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());

    final HostedComponentScanQueue completedJob = new HostedComponentScanQueue();
    completedJob.setComponentId(component.getId());
    completedJob.setScanFileId("scan-completed");
    completedJob.setStatus(HostedComponentScanQueueDAO.Status.COMPLETED.name());
    completedJob.setPriority(5);
    completedJob.setRepositoryId(repo.getId());
    hostedComponentScanQueueDAO.insert(completedJob);

    hostedComponentScanQueueDAO.unacquireJobs(Set.of(completedJob.getId()));

    HostedComponentScanQueue result = hostedComponentScanQueueDAO.getById(completedJob.getId());
    assertThat(result.getStatus()).isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name());
  }

  @Test
  public void testResetInProgressToPending() {
    final Repository repo = tempEntity.newRepository("repo-1");
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());

    final HostedComponentScanQueue inProgress1 = new HostedComponentScanQueue();
    inProgress1.setComponentId(component.getId());
    inProgress1.setScanFileId("scan-ip1");
    inProgress1.setStatus(HostedComponentScanQueueDAO.Status.IN_PROGRESS.name());
    inProgress1.setPriority(5);
    inProgress1.setRepositoryId(repo.getId());
    inProgress1.setAcquiredAt(Date.from(Instant.now()));
    hostedComponentScanQueueDAO.insert(inProgress1);

    final HostedComponentScanQueue inProgress2 = new HostedComponentScanQueue();
    inProgress2.setComponentId(component.getId());
    inProgress2.setScanFileId("scan-ip2");
    inProgress2.setStatus(HostedComponentScanQueueDAO.Status.IN_PROGRESS.name());
    inProgress2.setPriority(5);
    inProgress2.setRepositoryId(repo.getId());
    inProgress2.setAcquiredAt(Date.from(Instant.now()));
    hostedComponentScanQueueDAO.insert(inProgress2);

    final HostedComponentScanQueue pending = new HostedComponentScanQueue();
    pending.setComponentId(component.getId());
    pending.setScanFileId("scan-pending");
    pending.setStatus(HostedComponentScanQueueDAO.Status.PENDING.name());
    pending.setPriority(5);
    pending.setRepositoryId(repo.getId());
    hostedComponentScanQueueDAO.insert(pending);

    try (TransactionContext tx = hostedComponentScanQueueDAO.createTransactionContext()) {
      tx.begin();
      int reset = hostedComponentScanQueueDAO.resetInProgressToPending(tx);
      tx.commit();
      assertThat(reset).isEqualTo(2);
    }

    HostedComponentScanQueue result1 = hostedComponentScanQueueDAO.getById(inProgress1.getId());
    assertThat(result1.getStatus()).isEqualTo(HostedComponentScanQueueDAO.Status.PENDING.name());
    assertThat(result1.getAcquiredAt()).isNull();

    HostedComponentScanQueue result2 = hostedComponentScanQueueDAO.getById(inProgress2.getId());
    assertThat(result2.getStatus()).isEqualTo(HostedComponentScanQueueDAO.Status.PENDING.name());

    HostedComponentScanQueue resultPending = hostedComponentScanQueueDAO.getById(pending.getId());
    assertThat(resultPending.getStatus()).isEqualTo(HostedComponentScanQueueDAO.Status.PENDING.name());
  }

  private HostedComponentScanQueue insertJob(
      final RepositoryComponent component,
      final Repository repo,
      final String scanFileId,
      final HostedComponentScanQueueDAO.Status status,
      final int priority)
  {
    HostedComponentScanQueue job = new HostedComponentScanQueue();
    job.setComponentId(component.getId());
    job.setScanFileId(scanFileId);
    job.setStatus(status.name());
    job.setPriority(priority);
    job.setRepositoryId(repo.getId());
    if (status == HostedComponentScanQueueDAO.Status.IN_PROGRESS) {
      job.setAcquiredAt(Date.from(Instant.now()));
    }
    hostedComponentScanQueueDAO.insert(job);
    return job;
  }

  @Test
  public void testHasInProgressByComponentIds_ReturnsTrueWhenInProgressExists() {
    final Repository repo = tempEntity.newRepository("repo-1");
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());

    final HostedComponentScanQueue inProgress = new HostedComponentScanQueue();
    inProgress.setComponentId(component.getId());
    inProgress.setScanFileId("scan-in-progress");
    inProgress.setStatus(HostedComponentScanQueueDAO.Status.IN_PROGRESS.name());
    inProgress.setPriority(5);
    inProgress.setRepositoryId(repo.getId());
    hostedComponentScanQueueDAO.insert(inProgress);

    try (TransactionContext tx = hostedComponentScanQueueDAO.createTransactionContext()) {
      assertThat(hostedComponentScanQueueDAO.hasInProgressByComponentIds(tx, List.of(component.getId()))).isTrue();
    }
  }

  @Test
  public void testHasInProgressByComponentIds_ReturnsFalseWhenOnlyPending() {
    final Repository repo = tempEntity.newRepository("repo-1");
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());

    final HostedComponentScanQueue pending = new HostedComponentScanQueue();
    pending.setComponentId(component.getId());
    pending.setScanFileId("scan-pending");
    pending.setStatus(HostedComponentScanQueueDAO.Status.PENDING.name());
    pending.setPriority(5);
    pending.setRepositoryId(repo.getId());
    hostedComponentScanQueueDAO.insert(pending);

    try (TransactionContext tx = hostedComponentScanQueueDAO.createTransactionContext()) {
      assertThat(hostedComponentScanQueueDAO.hasInProgressByComponentIds(tx, List.of(component.getId()))).isFalse();
    }
  }

  @Test
  public void testHasInProgressByComponentIds_EmptyListReturnsFalse() {
    try (TransactionContext tx = hostedComponentScanQueueDAO.createTransactionContext()) {
      assertThat(hostedComponentScanQueueDAO.hasInProgressByComponentIds(tx, List.of())).isFalse();
    }
  }

  @Test
  public void testDeletePendingByComponentIds_BulkDeletesAcrossMultipleComponents() {
    final Repository repo = tempEntity.newRepository("repo-bulk");
    final RepositoryComponent component1 = tempEntity.newRepositoryComponent(repo.getId(), "path-bulk-1");
    final RepositoryComponent component2 = tempEntity.newRepositoryComponent(repo.getId(), "path-bulk-2");

    final HostedComponentScanQueue pending1 = new HostedComponentScanQueue();
    pending1.setComponentId(component1.getId());
    pending1.setScanFileId("bulk-scan-1");
    pending1.setStatus(HostedComponentScanQueueDAO.Status.PENDING.name());
    pending1.setPriority(5);
    pending1.setRepositoryId(repo.getId());
    hostedComponentScanQueueDAO.insert(pending1);

    final HostedComponentScanQueue pending2 = new HostedComponentScanQueue();
    pending2.setComponentId(component2.getId());
    pending2.setScanFileId("bulk-scan-2");
    pending2.setStatus(HostedComponentScanQueueDAO.Status.PENDING.name());
    pending2.setPriority(5);
    pending2.setRepositoryId(repo.getId());
    hostedComponentScanQueueDAO.insert(pending2);

    // IN_PROGRESS entry for component1 — should NOT be deleted
    final HostedComponentScanQueue inProgress = new HostedComponentScanQueue();
    inProgress.setComponentId(component1.getId());
    inProgress.setScanFileId("bulk-scan-in-progress");
    inProgress.setStatus(HostedComponentScanQueueDAO.Status.IN_PROGRESS.name());
    inProgress.setPriority(5);
    inProgress.setRepositoryId(repo.getId());
    hostedComponentScanQueueDAO.insert(inProgress);

    try (TransactionContext tx = hostedComponentScanQueueDAO.createTransactionContext()) {
      int deleted = hostedComponentScanQueueDAO.deletePendingByComponentIds(tx,
          List.of(component1.getId(), component2.getId()));
      assertThat(deleted).isEqualTo(2);
      tx.commit();
    }

    assertThat(hostedComponentScanQueueDAO.getById(pending1.getId())).isNull();
    assertThat(hostedComponentScanQueueDAO.getById(pending2.getId())).isNull();
    assertThat(hostedComponentScanQueueDAO.getById(inProgress.getId())).isNotNull();
  }

  @Test
  public void testDeletePendingByComponentIds_EmptyListReturnsZero() {
    try (TransactionContext tx = hostedComponentScanQueueDAO.createTransactionContext()) {
      int deleted = hostedComponentScanQueueDAO.deletePendingByComponentIds(tx, List.of());
      assertThat(deleted).isEqualTo(0);
    }
  }

}
