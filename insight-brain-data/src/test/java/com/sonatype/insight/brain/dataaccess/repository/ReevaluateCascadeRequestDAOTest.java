/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeProgressStatus;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeRequest;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeRequestStatus;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReevaluateCascadeRequestDAOTest
    extends AbstractDbDAOTest
{
  private ReevaluateCascadeRequestDAO dao;

  private ReevaluateCascadeProgressDAO progressDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createReevaluateCascadeRequestDAO();
    progressDAO = daoFactory.createReevaluateCascadeProgressDAO();
  }

  @Test
  public void testCreateAndGetByComponentHash() {
    // Arrange
    String requestId = "cascade_test_123";
    String componentHash = "abc123def456";
    String username = "testUser";

    tempEntity.newReevaluateCascadeRequest(requestId, componentHash, username);

    // Act - Find by component hash
    List<ReevaluateCascadeRequest> found = dao.getByComponentHash(componentHash);

    // Assert
    assertThat(found).hasSize(1);

    ReevaluateCascadeRequest foundRequest = found.get(0);
    assertThat(foundRequest.getId()).isEqualTo(requestId);
    assertThat(foundRequest.getComponentReferenceHash()).isEqualTo(componentHash);
    assertThat(foundRequest.getCreatedByUsername()).isEqualTo(username);
    assertThat(foundRequest.getCreatedAt()).isNotNull();
  }

  @Test
  public void testGetByComponentHash_MultipleRequests() {
    // Arrange
    String componentHash = "shared_hash_456";
    String username = "testUser";

    tempEntity.newReevaluateCascadeRequest("cascade_1", componentHash, username);
    tempEntity.newReevaluateCascadeRequest("cascade_2", componentHash, username);
    tempEntity.newReevaluateCascadeRequest("cascade_3", "different_hash", username);

    // Act - Find by component hash
    List<ReevaluateCascadeRequest> found = dao.getByComponentHash(componentHash);

    // Assert - Should find only the 2 requests with matching component hash
    assertThat(found).hasSize(2);
    assertThat(found).allMatch(request -> componentHash.equals(request.getComponentReferenceHash()));

    List<String> requestIds = found.stream().map(ReevaluateCascadeRequest::getId).toList();
    assertThat(requestIds).containsExactlyInAnyOrder("cascade_1", "cascade_2");
  }

  @Test
  public void testFindBeforeOrOn() {
    Date cutoffDate = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000); // 24 hours ago
    Date beforeCutoff = new Date(cutoffDate.getTime() - 60 * 60 * 1000); // 1 hour before cutoff
    Date afterCutoff = new Date(cutoffDate.getTime() + 60 * 60 * 1000); // 1 hour after cutoff

    String username = "testUser";

    // Create requests with different statuses and dates - all should be found if before cutoff
    ReevaluateCascadeRequest oldCompleted = tempEntity.newReevaluateCascadeRequest(
        "old_completed", "hash1", username);
    oldCompleted.setStatus(ReevaluateCascadeRequestStatus.COMPLETED);
    oldCompleted.setCreatedAt(beforeCutoff);

    ReevaluateCascadeRequest oldPending = tempEntity.newReevaluateCascadeRequest(
        "old_pending", "hash2", username);
    oldPending.setStatus(ReevaluateCascadeRequestStatus.PENDING);
    oldPending.setCreatedAt(beforeCutoff);

    ReevaluateCascadeRequest oldInProgress = tempEntity.newReevaluateCascadeRequest(
        "old_in_progress", "hash3", username);
    oldInProgress.setStatus(ReevaluateCascadeRequestStatus.IN_PROGRESS);
    oldInProgress.setCreatedAt(beforeCutoff);

    ReevaluateCascadeRequest oldFailed = tempEntity.newReevaluateCascadeRequest(
        "old_failed", "hash4", username);
    oldFailed.setStatus(ReevaluateCascadeRequestStatus.FAILED);
    oldFailed.setCreatedAt(beforeCutoff);

    // Request created exactly at cutoff (should be found)
    ReevaluateCascadeRequest exactlyCutoff = tempEntity.newReevaluateCascadeRequest(
        "exactly_cutoff", "hash5", username);
    exactlyCutoff.setStatus(ReevaluateCascadeRequestStatus.PENDING);
    exactlyCutoff.setCreatedAt(cutoffDate);

    // New request (should NOT be found)
    ReevaluateCascadeRequest newPending = tempEntity.newReevaluateCascadeRequest(
        "new_pending", "hash6", username);
    newPending.setStatus(ReevaluateCascadeRequestStatus.PENDING);
    newPending.setCreatedAt(afterCutoff);

    // Persist the modified entities
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.update(tx, oldCompleted);
      dao.update(tx, oldPending);
      dao.update(tx, oldInProgress);
      dao.update(tx, oldFailed);
      dao.update(tx, exactlyCutoff);
      dao.update(tx, newPending);
      tx.commit();
    }

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      List<ReevaluateCascadeRequest> found = dao.findBeforeOrOn(tx, cutoffDate);
      tx.commit();

      assertThat(found).hasSize(5);
      List<String> foundIds = found.stream().map(ReevaluateCascadeRequest::getId).toList();
      assertThat(foundIds).containsExactlyInAnyOrder(
          "old_completed", "old_pending", "old_in_progress", "old_failed", "exactly_cutoff");

      // Verify all found requests are at or before cutoff
      assertThat(found).allMatch(request -> request.getCreatedAt().getTime() <= cutoffDate.getTime());
    }
  }

  @Test
  public void testDeleteByRequestIds() {
    Date cutoffDate = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
    Date beforeCutoff = new Date(cutoffDate.getTime() - 60 * 60 * 1000);

    String username = "testUser";

    // Create requests with different statuses and dates
    ReevaluateCascadeRequest oldCompleted = tempEntity.newReevaluateCascadeRequest(
        "delete_by_id_completed", "hash1", username);
    oldCompleted.setStatus(ReevaluateCascadeRequestStatus.COMPLETED);
    oldCompleted.setCreatedAt(beforeCutoff);

    ReevaluateCascadeRequest oldPending = tempEntity.newReevaluateCascadeRequest(
        "delete_by_id_pending", "hash2", username);
    oldPending.setStatus(ReevaluateCascadeRequestStatus.PENDING);
    oldPending.setCreatedAt(beforeCutoff);

    ReevaluateCascadeRequest shouldRemain = tempEntity.newReevaluateCascadeRequest(
        "should_remain", "hash3", username);
    shouldRemain.setStatus(ReevaluateCascadeRequestStatus.PENDING);
    shouldRemain.setCreatedAt(beforeCutoff);

    dao.update(oldCompleted);
    dao.update(oldPending);
    dao.update(shouldRemain);

    // Verify initial state
    assertThat(dao.getByComponentHash("hash1")).hasSize(1);
    assertThat(dao.getByComponentHash("hash2")).hasSize(1);
    assertThat(dao.getByComponentHash("hash3")).hasSize(1);

    // Act - Delete specific requests by ID
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByRequestIds(tx, Set.of("delete_by_id_completed", "delete_by_id_pending"));
      tx.commit();
    }

    // Assert - Only specific requests should be deleted
    assertThat(dao.getByComponentHash("hash1")).isEmpty(); // deleted
    assertThat(dao.getByComponentHash("hash2")).isEmpty(); // deleted
    assertThat(dao.getByComponentHash("hash3")).hasSize(1); // remains
  }

  @Test
  public void testDeleteByRequestIds_EmptySet() {
    String username = "testUser";
    ReevaluateCascadeRequest request = tempEntity.newReevaluateCascadeRequest(
        "should_not_be_deleted", "hash1", username);
    dao.update(request);

    // Verify initial state
    assertThat(dao.getByComponentHash("hash1")).hasSize(1);

    // Act - Delete with empty set (should be no-op)
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByRequestIds(tx, Set.of());
      tx.commit();
    }

    // Assert - Nothing should be deleted
    assertThat(dao.getByComponentHash("hash1")).hasSize(1);
  }

  @Test
  public void testDeleteByRequestIds_NonExistentIds() {
    String username = "testUser";
    ReevaluateCascadeRequest existingRequest = tempEntity.newReevaluateCascadeRequest(
        "existing_request", "hash1", username);
    dao.update(existingRequest);

    assertThat(dao.getByComponentHash("hash1")).hasSize(1);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByRequestIds(tx, Set.of("non_existent_1", "non_existent_2"));
      tx.commit();
    }

    assertThat(dao.getByComponentHash("hash1")).hasSize(1);
  }

  @Test
  public void testDeleteByRequestIds_MixedExistentAndNonExistent() {
    String username = "testUser";
    ReevaluateCascadeRequest existingRequest = tempEntity.newReevaluateCascadeRequest(
        "existing_request", "hash1", username);
    ReevaluateCascadeRequest anotherRequest = tempEntity.newReevaluateCascadeRequest(
        "another_request", "hash2", username);
    dao.update(existingRequest);
    dao.update(anotherRequest);

    assertThat(dao.getByComponentHash("hash1")).hasSize(1);
    assertThat(dao.getByComponentHash("hash2")).hasSize(1);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByRequestIds(tx, Set.of("existing_request", "non_existent_id"));
      tx.commit();
    }

    assertThat(dao.getByComponentHash("hash1")).isEmpty(); // deleted
    assertThat(dao.getByComponentHash("hash2")).hasSize(1); // remains
  }

  @Test
  public void testFindBeforeOrOn_NoMatches() {
    Date cutoffDate = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000); // 24 hours ago
    Date afterCutoff = new Date(cutoffDate.getTime() + 60 * 60 * 1000); // 1 hour after cutoff

    String username = "testUser";

    ReevaluateCascadeRequest newRequest = tempEntity.newReevaluateCascadeRequest(
        "new_request", "hash1", username);
    newRequest.setCreatedAt(afterCutoff);
    dao.update(newRequest);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      List<ReevaluateCascadeRequest> found = dao.findBeforeOrOn(tx, cutoffDate);
      tx.commit();

      assertThat(found).isEmpty();
    }
  }

  @Test
  public void testDeleteByRequestIds_CascadeDeletion() {
    String username = "testUser";

    ReevaluateCascadeRequest request = tempEntity.newReevaluateCascadeRequest(
        "cascade_deletion_test", "hash1", username);
    dao.update(request);

    Repository repository1 = tempEntity.newRepository("test-repo-1");
    Repository repository2 = tempEntity.newRepository("test-repo-2");
    RepositoryComponent component1 = tempEntity.newRepositoryComponent(repository1.getId(), "component-path-1");
    RepositoryComponent component2 = tempEntity.newRepositoryComponent(repository2.getId(), "component-path-2");

    tempEntity.newReevaluateCascadeProgress(
        "progress_1", "cascade_deletion_test", repository1.getId(), component1.getId(),
        ReevaluateCascadeProgressStatus.PENDING.name());
    tempEntity.newReevaluateCascadeProgress(
        "progress_2", "cascade_deletion_test", repository2.getId(), component2.getId(),
        ReevaluateCascadeProgressStatus.COMPLETED.name());

    // Create another request that should not be affected
    ReevaluateCascadeRequest unrelatedRequest = tempEntity.newReevaluateCascadeRequest(
        "unrelated_request", "hash2", username);
    dao.update(unrelatedRequest);

    tempEntity.newReevaluateCascadeProgress(
        "unrelated_progress", "unrelated_request", repository1.getId(), component1.getId(),
        ReevaluateCascadeProgressStatus.PENDING.name());

    assertThat(dao.getByComponentHash("hash1")).hasSize(1);
    assertThat(dao.getByComponentHash("hash2")).hasSize(1);
    assertThat(progressDAO.getByRequestId("cascade_deletion_test")).hasSize(2);
    assertThat(progressDAO.getByRequestId("unrelated_request")).hasSize(1);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByRequestIds(tx, Set.of("cascade_deletion_test"));
      tx.commit();
    }

    assertThat(dao.getByComponentHash("hash1")).isEmpty(); // request deleted
    assertThat(dao.getByComponentHash("hash2")).hasSize(1); // unrelated request remains
    assertThat(progressDAO.getByRequestId("cascade_deletion_test")).isEmpty();
    assertThat(progressDAO.getByRequestId("unrelated_request")).hasSize(1);
  }
}
