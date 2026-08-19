/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.continuousmonitoring;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringFlowType;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringHostedRepoItem;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringQueueItem;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ContinuousMonitoringHostedRepoItemDAO}. Exercises only satellite-table
 * behaviour — parent-table behaviour and the producer-side orchestration are covered by
 * {@link ContinuousMonitoringQueueItemDAOTest}.
 */
public class ContinuousMonitoringHostedRepoItemDAOTest
    extends AbstractDbDAOTest
{
  private ContinuousMonitoringQueueItemDAO queueItemDAO;

  private ContinuousMonitoringHostedRepoItemDAO hostedRepoItemDAO;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    queueItemDAO = daoFactory.createContinuousMonitoringQueueItemDAO();
    hostedRepoItemDAO = daoFactory.createContinuousMonitoringHostedRepoItemDAO();
  }

  @Test
  public void testInsertIgnoreDuplicateKey_doesNothingForEmptyOrNullList() {
    try (TransactionContext tx = hostedRepoItemDAO.createTransactionContext()) {
      tx.begin();
      hostedRepoItemDAO.insertIgnoreDuplicateKey(tx, List.of());
      hostedRepoItemDAO.insertIgnoreDuplicateKey(tx, null);
      tx.commit();
    }
    // No row should have been inserted.
    try (TransactionContext tx = hostedRepoItemDAO.createTransactionContext()) {
      tx.begin();
      assertThat(hostedRepoItemDAO.getByQueueIds(tx, List.of(UUID.randomUUID().toString()))).isEmpty();
      tx.commit();
    }
  }

  @Test
  public void testInsertIgnoreDuplicateKey_silentlyDropsDuplicateNaturalKey() {
    // Insert two parent rows up-front so the FK is satisfied for both satellites.
    String parentIdFirst = insertParent();
    String parentIdSecond = insertParent();

    ContinuousMonitoringHostedRepoItem first =
        new ContinuousMonitoringHostedRepoItem(parentIdFirst, "repo-1", "hash-A");
    ContinuousMonitoringHostedRepoItem secondWithSameNaturalKey =
        new ContinuousMonitoringHostedRepoItem(parentIdSecond, "repo-1", "hash-A");

    try (TransactionContext tx = hostedRepoItemDAO.createTransactionContext()) {
      tx.begin();
      hostedRepoItemDAO.insertIgnoreDuplicateKey(tx, List.of(first));
      hostedRepoItemDAO.insertIgnoreDuplicateKey(tx, List.of(secondWithSameNaturalKey));
      tx.commit();
    }

    try (TransactionContext tx = hostedRepoItemDAO.createTransactionContext()) {
      tx.begin();
      // First insert wins; second is silently dropped on the natural-key UNIQUE constraint.
      assertThat(hostedRepoItemDAO.getByQueueIds(tx, List.of(parentIdFirst))).hasSize(1);
      assertThat(hostedRepoItemDAO.getByQueueIds(tx, List.of(parentIdSecond))).isEmpty();
      tx.commit();
    }
  }

  @Test
  public void testInsertIgnoreDuplicateKey_propagatesNonDuplicateKeySqlException() {
    // The DAO swallows SQLState 23xxx (integrity-constraint) and re-throws everything else.
    // component_hash is varchar(255); a 10k-char value triggers SQLState 22001
    // (string-data-right-truncation), which must propagate as an exception.
    String parentId = insertParent();
    String overlongHash = "x".repeat(10_000);
    ContinuousMonitoringHostedRepoItem satellite =
        new ContinuousMonitoringHostedRepoItem(parentId, "repo-1", overlongHash);

    try (TransactionContext tx = hostedRepoItemDAO.createTransactionContext()) {
      tx.begin();
      assertThatThrownBy(() -> hostedRepoItemDAO.insertIgnoreDuplicateKey(tx, List.of(satellite)))
          .isInstanceOf(RuntimeException.class);
      tx.rollback();
    }
  }

  @Test
  public void testGetByQueueIds_returnsOnlyMatchingSatellites() {
    String idA = tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-A", 0L).getId();
    String idB = tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-B", 0L).getId();

    try (TransactionContext tx = hostedRepoItemDAO.createTransactionContext()) {
      tx.begin();
      List<ContinuousMonitoringHostedRepoItem> sats = hostedRepoItemDAO.getByQueueIds(tx, List.of(idA));
      tx.commit();
      assertThat(sats).hasSize(1);
      assertThat(sats.get(0).getQueueId()).isEqualTo(idA);
      assertThat(sats.get(0).getComponentHash()).isEqualTo("hash-A");
    }

    try (TransactionContext tx = hostedRepoItemDAO.createTransactionContext()) {
      tx.begin();
      List<ContinuousMonitoringHostedRepoItem> sats = hostedRepoItemDAO.getByQueueIds(tx, List.of(idA, idB));
      tx.commit();
      assertThat(sats)
          .extracting(ContinuousMonitoringHostedRepoItem::getQueueId)
          .containsExactlyInAnyOrder(idA, idB);
    }
  }

  /**
   * Inserts a parent row directly (bypassing tempEntity, which would also insert a satellite)
   * so the satellite-only tests can target the satellite DAO in isolation.
   */
  private String insertParent() {
    String id = UUID.randomUUID().toString();
    ContinuousMonitoringQueueItem parent =
        new ContinuousMonitoringQueueItem(id, ContinuousMonitoringFlowType.HOSTED_REPO, 0L, new Date());
    try (TransactionContext tx = queueItemDAO.createTransactionContext()) {
      tx.begin();
      queueItemDAO.insertBatch(tx, List.of(parent), false);
      tx.commit();
    }
    return id;
  }
}
