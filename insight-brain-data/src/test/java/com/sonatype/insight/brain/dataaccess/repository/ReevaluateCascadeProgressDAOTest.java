/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeProgress;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeProgressStatus;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeRequest;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeRequestStatus;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
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
    ProxyRepositoryComponent proxyRepositoryComponent = tempEntity.newRepositoryComponent(repository.getId(),
        "test-component-path");
    createTestDataForCascadeRequest(requestId, "test-hash-123");

    tempEntity.newReevaluateCascadeProgress(
        progressId, requestId, repository.getId(), proxyRepositoryComponent.getId(), status.name());

    // Act - Find by request ID
    List<ReevaluateCascadeProgress> found = dao.getByRequestId(requestId);

    // Assert
    assertThat(found).hasSize(1);

    ReevaluateCascadeProgress foundProgress = found.get(0);
    assertThat(foundProgress.getId()).isEqualTo(progressId);
    assertThat(foundProgress.getReevaluateCascadeRequestId()).isEqualTo(requestId);
    assertThat(foundProgress.getRepositoryId()).isEqualTo(repository.getId());
    assertThat(foundProgress.getProxyRepositoryComponentId()).isEqualTo(proxyRepositoryComponent.getId());
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
    ProxyRepositoryComponent component1 = tempEntity.newRepositoryComponent(repository1.getId(), "component-path-1");
    ProxyRepositoryComponent component2 = tempEntity.newRepositoryComponent(repository1.getId(), "component-path-2");
    ProxyRepositoryComponent component3 = tempEntity.newRepositoryComponent(repository2.getId(), "component-path-3");
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
    ProxyRepositoryComponent component1 = tempEntity.newRepositoryComponent(repository1.getId(), "component-path-1");
    ProxyRepositoryComponent component2 = tempEntity.newRepositoryComponent(repository2.getId(), "component-path-2");
    ProxyRepositoryComponent component3 = tempEntity.newRepositoryComponent(repository3.getId(), "component-path-3");
    ProxyRepositoryComponent component4 = tempEntity.newRepositoryComponent(repository4.getId(), "component-path-4");
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
    ProxyRepositoryComponent component1 = tempEntity.newRepositoryComponent(repository1.getId(), "component-path-1");
    ProxyRepositoryComponent component2 = tempEntity.newRepositoryComponent(repository2.getId(), "component-path-2");
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
    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(), "component-path-1");
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
    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(), "component-path-1");
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

  @Test
  public void testDeleteByRequestIds() {
    // Arrange
    String requestId1 = "test8_delete_all_request_1";
    String requestId2 = "test8_delete_all_request_2";
    String requestId3 = "test8_delete_all_request_3";

    // Create repositories and cascade requests
    Repository repository1 = tempEntity.newRepository("test-delete-all-repo-1");
    Repository repository2 = tempEntity.newRepository("test-delete-all-repo-2");
    Repository repository3 = tempEntity.newRepository("test-delete-all-repo-3");
    Repository repository4 = tempEntity.newRepository("test-delete-all-repo-4");
    Repository repository5 = tempEntity.newRepository("test-delete-all-repo-5");
    ProxyRepositoryComponent component1 = tempEntity.newRepositoryComponent(repository1.getId(), "component-path-1");
    ProxyRepositoryComponent component2 = tempEntity.newRepositoryComponent(repository2.getId(), "component-path-2");
    ProxyRepositoryComponent component3 = tempEntity.newRepositoryComponent(repository3.getId(), "component-path-3");
    ProxyRepositoryComponent component4 = tempEntity.newRepositoryComponent(repository4.getId(), "component-path-4");
    ProxyRepositoryComponent component5 = tempEntity.newRepositoryComponent(repository5.getId(), "component-path-5");
    createTestDataForCascadeRequest(requestId1, "test-hash-delete-all-1");
    createTestDataForCascadeRequest(requestId2, "test-hash-delete-all-2");
    createTestDataForCascadeRequest(requestId3, "test-hash-delete-all-3");

    // Request 1: Mixed statuses (ALL should be deleted)
    tempEntity.newReevaluateCascadeProgress(
        "completed_delete_all_1", requestId1, repository1.getId(), component1.getId(),
        ReevaluateCascadeProgressStatus.COMPLETED.name());
    tempEntity.newReevaluateCascadeProgress(
        "failed_delete_all_1", requestId1, repository2.getId(), component2.getId(),
        ReevaluateCascadeProgressStatus.FAILED.name());
    tempEntity.newReevaluateCascadeProgress(
        "pending_delete_all_1", requestId1, repository5.getId(), component5.getId(),
        ReevaluateCascadeProgressStatus.PENDING.name());

    // Request 2: Only PENDING entries (should ALL be deleted)
    tempEntity.newReevaluateCascadeProgress(
        "pending_delete_all_2", requestId2, repository3.getId(), component3.getId(),
        ReevaluateCascadeProgressStatus.PENDING.name());

    // Request 3: NOT in deletion set (should remain untouched)
    tempEntity.newReevaluateCascadeProgress(
        "pending_keep_all_1", requestId3, repository4.getId(), component4.getId(),
        ReevaluateCascadeProgressStatus.PENDING.name());

    // Verify initial state
    assertThat(dao.getByRequestId(requestId1)).hasSize(3); // 3 entries with mixed statuses
    assertThat(dao.getByRequestId(requestId2)).hasSize(1); // 1 pending entry
    assertThat(dao.getByRequestId(requestId3)).hasSize(1); // 1 pending entry

    // Act - Delete ALL entries for request1 and request2 (regardless of status)
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByRequestIds(tx, Set.of(requestId1, requestId2));
      tx.commit();
    }

    // Assert - ALL entries for request1 and request2 should be deleted, request3 should remain
    assertThat(dao.getByRequestId(requestId1)).isEmpty(); // All entries deleted
    assertThat(dao.getByRequestId(requestId2)).isEmpty(); // All entries deleted

    List<ReevaluateCascadeProgress> request3Remaining = dao.getByRequestId(requestId3);
    assertThat(request3Remaining).hasSize(1); // Not in deletion set, should remain untouched
    assertThat(request3Remaining.get(0).getId()).isEqualTo("pending_keep_all_1");
    assertThat(request3Remaining.get(0).getStatus()).isEqualTo(ReevaluateCascadeProgressStatus.PENDING);
  }

  @Test
  public void testDeleteByRequestIdsWithEmptySet() {
    String requestId = "test9_empty_set_all_request";

    Repository repository = tempEntity.newRepository("test-empty-all-repo");
    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(), "component-path");
    createTestDataForCascadeRequest(requestId, "test-hash-empty-all");

    tempEntity.newReevaluateCascadeProgress(
        "pending_before_empty_all", requestId, repository.getId(), component.getId(),
        ReevaluateCascadeProgressStatus.PENDING.name());

    assertThat(dao.getByRequestId(requestId)).hasSize(1);
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByRequestIds(tx, Set.of());
      tx.commit();
    }

    assertThat(dao.getByRequestId(requestId)).hasSize(1);
  }

  @Test
  public void testDeleteByRequestIdsWithNonExistentIds() {
    String requestId = "test10_existing_all_request";
    String nonExistentRequestId = "test10_non_existent_all_request";

    Repository repository = tempEntity.newRepository("test-non-existent-all-repo");
    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(), "component-path");
    createTestDataForCascadeRequest(requestId, "test-hash-non-existent-all");

    tempEntity.newReevaluateCascadeProgress(
        "pending_existing_all", requestId, repository.getId(), component.getId(),
        ReevaluateCascadeProgressStatus.PENDING.name());

    assertThat(dao.getByRequestId(requestId)).hasSize(1);

    // Act - Try to delete with both existing and non-existent request IDs
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByRequestIds(tx, Set.of(requestId, nonExistentRequestId));
      tx.commit();
    }

    // Assert - Existing entry should be deleted, non-existent ID has no effect
    assertThat(dao.getByRequestId(requestId)).isEmpty(); // Existing entry deleted
    assertThat(dao.getByRequestId(nonExistentRequestId)).isEmpty(); // Non-existent ID has no effect
  }
}
