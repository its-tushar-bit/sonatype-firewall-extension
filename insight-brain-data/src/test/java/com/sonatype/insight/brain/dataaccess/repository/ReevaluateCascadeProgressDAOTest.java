/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeProgress;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeProgressStatus;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeRequest;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeRequestStatus;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReevaluateCascadeProgressDAOTest
    extends AbstractDbDAOTest
{
  private ReevaluateCascadeProgressDAO dao;

  private ReevaluateCascadeRequestDAO requestDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createReevaluateCascadeProgressDAO();
    requestDAO = daoFactory.createReevaluateCascadeRequestDAO();
  }

  private void createTestDataForCascadeRequest(String requestId, String componentHash) {
    // Create cascade request with the repository's internal ID
    ReevaluateCascadeRequest request =
        new ReevaluateCascadeRequest(componentHash, "testUser", ReevaluateCascadeRequestStatus.PENDING);
    request.setId(requestId);
    try (TransactionContext tx = requestDAO.createTransactionContext()) {
      tx.begin();
      requestDAO.insert(tx, request);
      tx.commit();
    }
  }

  @Test
  public void testCreateAndGetByRequestId() {
    // Arrange
    String progressId = "progress_test_123";
    String requestId = "test1_cascade_request_456";
    ReevaluateCascadeProgressStatus status = ReevaluateCascadeProgressStatus.PENDING;

    // Create parent cascade request first
    Repository repository = tempEntity.newRepository("test-repo-123");
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(),
        "test-component-path");
    createTestDataForCascadeRequest(requestId, "test-hash-123");

    tempEntity.newReevaluateCascadeProgress(
        progressId, requestId, repository.getId(), repositoryComponent.getId(), status.name());

    // Act - Find by request ID
    List<ReevaluateCascadeProgress> found = dao.getByRequestId(requestId);

    // Assert
    assertThat(found).hasSize(1);

    ReevaluateCascadeProgress foundProgress = found.get(0);
    assertThat(foundProgress.getId()).isEqualTo(progressId);
    assertThat(foundProgress.getReevaluateCascadeRequestId()).isEqualTo(requestId);
    assertThat(foundProgress.getRepositoryId()).isEqualTo(repository.getId());
    assertThat(foundProgress.getRepositoryComponentId()).isEqualTo(repositoryComponent.getId());
    assertThat(foundProgress.getStatus()).isEqualTo(status);
    assertThat(foundProgress.isQuarantined()).isNull();
  }

  @Test
  public void testGetByRepositoryId() {
    // Arrange
    String requestId1 = "test2_request-1";
    String requestId2 = "test2_request-2";

    // Create repositories and cascade requests
    Repository repository1 = tempEntity.newRepository("test-repo-1");
    Repository repository2 = tempEntity.newRepository("test-repo-2");
    RepositoryComponent component1 = tempEntity.newRepositoryComponent(repository1.getId(), "component-path-1");
    RepositoryComponent component2 = tempEntity.newRepositoryComponent(repository1.getId(), "component-path-2");
    RepositoryComponent component3 = tempEntity.newRepositoryComponent(repository2.getId(), "component-path-3");
    createTestDataForCascadeRequest(requestId1, "test-hash-1");
    createTestDataForCascadeRequest(requestId2, "test-hash-2");

    // Create multiple progress entries for same repository
    tempEntity.newReevaluateCascadeProgress(
        "progress_1", requestId1, repository1.getId(), component1.getId(),
        ReevaluateCascadeProgressStatus.PENDING.name());
    tempEntity.newReevaluateCascadeProgress(
        "progress_2", requestId2, repository1.getId(), component2.getId(),
        ReevaluateCascadeProgressStatus.COMPLETED.name());
    tempEntity.newReevaluateCascadeProgress(
        "progress_3", requestId1, repository2.getId(), component3.getId(),
        ReevaluateCascadeProgressStatus.PENDING.name());

    // Act - Find by repository ID
    List<ReevaluateCascadeProgress> found = dao.getByRepositoryId(repository1.getId());

    // Assert - Should find only the 2 progress entries for the target repository
    assertThat(found).hasSize(2);
    assertThat(found).allMatch(progress -> repository1.getId().equals(progress.getRepositoryId()));

    List<String> progressIds = found.stream().map(ReevaluateCascadeProgress::getId).toList();
    assertThat(progressIds).containsExactlyInAnyOrder("progress_1", "progress_2");
  }

  @Test
  public void testCountMethods() {
    // Arrange
    String requestId = "test4_count_test_request";

    // Create repositories and cascade request
    Repository repository1 = tempEntity.newRepository("test-repo-1");
    Repository repository2 = tempEntity.newRepository("test-repo-2");
    Repository repository3 = tempEntity.newRepository("test-repo-3");
    Repository repository4 = tempEntity.newRepository("test-repo-4");
    RepositoryComponent component1 = tempEntity.newRepositoryComponent(repository1.getId(), "component-path-1");
    RepositoryComponent component2 = tempEntity.newRepositoryComponent(repository2.getId(), "component-path-2");
    RepositoryComponent component3 = tempEntity.newRepositoryComponent(repository3.getId(), "component-path-3");
    RepositoryComponent component4 = tempEntity.newRepositoryComponent(repository4.getId(), "component-path-4");
    createTestDataForCascadeRequest(requestId, "test-hash-count");

    // Create progress entries with different statuses
    tempEntity.newReevaluateCascadeProgress(
        "pending_1", requestId, repository1.getId(), component1.getId(),
        ReevaluateCascadeProgressStatus.PENDING.name());
    tempEntity.newReevaluateCascadeProgress(
        "pending_2", requestId, repository2.getId(), component2.getId(),
        ReevaluateCascadeProgressStatus.PENDING.name());
    tempEntity.newReevaluateCascadeProgress(
        "completed_1", requestId, repository3.getId(), component3.getId(),
        ReevaluateCascadeProgressStatus.COMPLETED.name());
    tempEntity.newReevaluateCascadeProgress(
        "failed_1", requestId, repository4.getId(), component4.getId(), ReevaluateCascadeProgressStatus.FAILED.name());

    // Act & Assert - Test count methods
    assertThat(dao.countPendingByRequestId(requestId)).isEqualTo(2);
    assertThat(dao.countCompletedByRequestId(requestId)).isEqualTo(1);
    assertThat(dao.countFailedByRequestId(requestId)).isEqualTo(1);

    // Should not be complete since there are pending entries
    assertThat(dao.isRequestComplete(requestId)).isFalse();
  }

  @Test
  public void testIsRequestComplete() {
    // Arrange
    String requestId = "test5_complete_test_request";

    // Create repositories and cascade request
    Repository repository1 = tempEntity.newRepository("test-repo-1");
    Repository repository2 = tempEntity.newRepository("test-repo-2");
    RepositoryComponent component1 = tempEntity.newRepositoryComponent(repository1.getId(), "component-path-1");
    RepositoryComponent component2 = tempEntity.newRepositoryComponent(repository2.getId(), "component-path-2");
    createTestDataForCascadeRequest(requestId, "test-hash-complete");

    // Create progress entries - all completed/failed (no pending)
    tempEntity.newReevaluateCascadeProgress(
        "completed_final_1", requestId, repository1.getId(), component1.getId(),
        ReevaluateCascadeProgressStatus.COMPLETED.name());
    tempEntity.newReevaluateCascadeProgress(
        "failed_final_1", requestId, repository2.getId(), component2.getId(),
        ReevaluateCascadeProgressStatus.FAILED.name());

    // Act & Assert - Should be complete since no pending entries
    assertThat(dao.isRequestComplete(requestId)).isTrue();
    assertThat(dao.countPendingByRequestId(requestId)).isEqualTo(0);
    assertThat(dao.countCompletedByRequestId(requestId)).isEqualTo(1);
    assertThat(dao.countFailedByRequestId(requestId)).isEqualTo(1);
  }

  @Test
  public void testMarkCompleted() {
    // Arrange
    String progressId = "progress_mark_completed";
    String requestId = "test6_mark_test_request";

    // Create repository and cascade request first
    Repository repository = tempEntity.newRepository("test-repo-1");
    RepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(), "component-path-1");
    createTestDataForCascadeRequest(requestId, "test-hash-mark");

    ReevaluateCascadeProgress progress = tempEntity.newReevaluateCascadeProgress(
        progressId, requestId, repository.getId(), component.getId(), ReevaluateCascadeProgressStatus.PENDING.name());

    // Act - Mark as completed
    progress.markCompleted();

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.update(tx, progress);
      tx.commit();
    }

    // Assert - Status should be updated
    List<ReevaluateCascadeProgress> found = dao.getByRequestId(requestId);
    assertThat(found).hasSize(1);
    assertThat(found.get(0).getStatus()).isEqualTo(ReevaluateCascadeProgressStatus.COMPLETED);
  }

  @Test
  public void testMarkFailed() {
    // Arrange
    String progressId = "progress_mark_failed";
    String requestId = "test7_fail_test_request";

    // Create repository and cascade request first
    Repository repository = tempEntity.newRepository("test-repo-1");
    RepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(), "component-path-1");
    createTestDataForCascadeRequest(requestId, "test-hash-fail");

    ReevaluateCascadeProgress progress = tempEntity.newReevaluateCascadeProgress(
        progressId, requestId, repository.getId(), component.getId(), ReevaluateCascadeProgressStatus.PENDING.name());

    // Act - Mark as failed
    progress.markFailed();

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.update(tx, progress);
      tx.commit();
    }

    // Assert - Status and error message should be updated
    List<ReevaluateCascadeProgress> found = dao.getByRequestId(requestId);
    assertThat(found).hasSize(1);
    assertThat(found.get(0).getStatus()).isEqualTo(ReevaluateCascadeProgressStatus.FAILED);
  }
}
