/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.repository.HostedDeploymentBlock;
import com.sonatype.insight.brain.model.repository.HostedDeploymentBlockViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HostedDeploymentBlockDAOTest
    extends AbstractDbDAOTest
{
  private HostedDeploymentBlockDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createHostedDeploymentBlockDAO();
  }

  @Test
  public void insertWithViolations_persistsParentAndChildren() {
    Repository repo = tempEntity.newRepository("repo-1");
    HostedDeploymentBlock block = newBlock(repo.getId());
    List<HostedDeploymentBlockViolation> violations = List.of(
        newViolation("Critical Security Policy", "Critical CVSS", "critical vuln"),
        newViolation("License Policy", "GPL-3.0", "disallowed license"));

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insertWithViolations(tx, block, violations);
      tx.commit();
    }

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      HostedDeploymentBlock read = dao.getById(tx, block.getId());
      assertThat(read).isNotNull();
      assertThat(read.getRepositoryId()).isEqualTo(repo.getId());
      assertThat(read.getPolicyAction()).isEqualTo("FAIL");
      assertThat(read.getHighestThreatLevel()).isEqualTo(9);
      assertThat(read.getPathname()).isEqualTo(block.getPathname());
      assertThat(read.getCorrelationId()).isEqualTo(block.getCorrelationId());

      List<HostedDeploymentBlockViolation> readViolations = dao.getViolationsByBlockId(tx, block.getId());
      assertThat(readViolations).hasSize(2);
      assertThat(readViolations).extracting(HostedDeploymentBlockViolation::getPolicyName)
          .containsExactlyInAnyOrder("Critical Security Policy", "License Policy");
      assertThat(readViolations).allMatch(v -> v.getHostedDeploymentBlockId().equals(block.getId()));
      tx.commit();
    }
  }

  @Test
  public void insertWithViolations_emptyViolationsList_persistsBlockOnly() {
    Repository repo = tempEntity.newRepository("repo-empty-violations");
    HostedDeploymentBlock block = newBlock(repo.getId());

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insertWithViolations(tx, block, List.of());
      tx.commit();
    }

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      assertThat(dao.getById(tx, block.getId())).isNotNull();
      assertThat(dao.getViolationsByBlockId(tx, block.getId())).isEmpty();
      tx.commit();
    }
  }

  @Test
  public void getByRepositoryId_returnsBlocksNewestFirst() {
    Repository repo = tempEntity.newRepository("repo-timeline");
    Instant now = Instant.now();

    HostedDeploymentBlock older = newBlock(repo.getId());
    older.setBlockedTime(Date.from(now.minus(2, ChronoUnit.HOURS)));
    older.setPathname("com/acme/lib/1.0/lib-1.0.jar");

    HostedDeploymentBlock newer = newBlock(repo.getId());
    newer.setBlockedTime(Date.from(now.minus(1, ChronoUnit.HOURS)));
    newer.setPathname("com/acme/lib/1.0/lib-1.0.jar"); // same pathname — retries allowed

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insertWithViolations(tx, older, List.of());
      dao.insertWithViolations(tx, newer, List.of());
      tx.commit();
    }

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      List<HostedDeploymentBlock> result = dao.getByRepositoryId(tx, repo.getId());
      assertThat(result).hasSize(2);
      assertThat(result.get(0).getId()).isEqualTo(newer.getId());
      assertThat(result.get(1).getId()).isEqualTo(older.getId());
      tx.commit();
    }
  }

  @Test
  public void deleteOlderThan_removesOldRowsAndCascadesViolations() {
    Repository repo = tempEntity.newRepository("repo-cleanup");
    Instant now = Instant.now();

    HostedDeploymentBlock oldBlock = newBlock(repo.getId());
    oldBlock.setBlockedTime(Date.from(now.minus(48, ChronoUnit.HOURS)));
    HostedDeploymentBlock recentBlock = newBlock(repo.getId());
    recentBlock.setBlockedTime(Date.from(now.minus(1, ChronoUnit.HOURS)));

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insertWithViolations(tx, oldBlock, List.of(newViolation("p", "c", "r")));
      dao.insertWithViolations(tx, recentBlock, List.of(newViolation("p", "c", "r")));
      tx.commit();
    }

    int deleted;
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      deleted = dao.deleteOlderThan(tx, now.minus(24, ChronoUnit.HOURS));
      tx.commit();
    }

    assertThat(deleted).isEqualTo(1);
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      assertThat(dao.getById(tx, oldBlock.getId())).isNull();
      assertThat(dao.getById(tx, recentBlock.getId())).isNotNull();
      // Violations on the deleted block cascaded away.
      assertThat(dao.getViolationsByBlockId(tx, oldBlock.getId())).isEmpty();
      assertThat(dao.getViolationsByBlockId(tx, recentBlock.getId())).hasSize(1);
      tx.commit();
    }
  }

  @Test
  public void getById_nonexistent_returnsNull() {
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      assertThat(dao.getById(tx, "does-not-exist")).isNull();
      tx.commit();
    }
  }

  private HostedDeploymentBlock newBlock(final String repositoryId) {
    HostedDeploymentBlock block = new HostedDeploymentBlock();
    block.setId(UUID.randomUUID().toString());
    block.setRepositoryId(repositoryId);
    block.setPathname("com/acme/lib/1.2.3/lib-1.2.3.jar");
    block.setHash("abc123");
    block.setComponentIdFormat("maven");
    block.setPolicyAction("FAIL");
    block.setHighestThreatLevel(9);
    block.setEvaluationUrl("https://iq.example.com/report/" + block.getId());
    block.setCorrelationId("nxrm-upload-" + block.getId());
    block.setRequestedBy("developer@example.com");
    block.setBlockedTime(new Date());
    return block;
  }

  private HostedDeploymentBlockViolation newViolation(
      final String policyName,
      final String constraintName,
      final String reason)
  {
    HostedDeploymentBlockViolation v = new HostedDeploymentBlockViolation();
    v.setId(UUID.randomUUID().toString());
    v.setPolicyName(policyName);
    v.setConstraintName(constraintName);
    v.setReason(reason);
    v.setComponentIdentifier("pkg:maven/com.acme/lib@1.2.3");
    return v;
  }
}
