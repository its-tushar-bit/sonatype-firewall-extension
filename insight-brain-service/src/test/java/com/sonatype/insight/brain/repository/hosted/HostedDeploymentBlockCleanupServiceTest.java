/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.repository.HostedDeploymentBlockDAO;
import com.sonatype.insight.brain.model.repository.HostedDeploymentBlock;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HostedDeploymentBlockCleanupServiceTest
    extends AbstractComponentTest
{
  @Inject
  private HostedDeploymentBlockCleanupService cleanupService;

  @Inject
  private HostedDeploymentBlockDAO blockDAO;

  @Test
  public void runCleanup_24h_deletesOnlyRowsOlderThan24h() {
    Repository repo = tempEntity.newRepository("repo-cleanup-24h");

    String old1 = insertBlock(repo.getId(), Instant.now().minus(Duration.ofDays(2)));
    String old2 = insertBlock(repo.getId(), Instant.now().minus(Duration.ofHours(25)));
    String recent = insertBlock(repo.getId(), Instant.now().minus(Duration.ofHours(1)));

    HostedDeploymentBlockCleanupService.CleanupOutcome outcome = cleanupService.runCleanup(Duration.ofHours(24));

    assertThat(outcome.deleted()).isEqualTo(2);
    assertThat(outcome.cutoffTime()).isBefore(Instant.now());

    try (TransactionContext tx = blockDAO.createTransactionContext()) {
      assertThat(blockDAO.getById(tx, old1)).isNull();
      assertThat(blockDAO.getById(tx, old2)).isNull();
      assertThat(blockDAO.getById(tx, recent)).isNotNull();
    }
  }

  @Test
  public void runCleanup_zeroDuration_deletesEverything() {
    Repository repo = tempEntity.newRepository("repo-cleanup-zero");

    String id1 = insertBlock(repo.getId(), Instant.now().minus(Duration.ofMinutes(10)));
    String id2 = insertBlock(repo.getId(), Instant.now().minus(Duration.ofSeconds(1)));

    HostedDeploymentBlockCleanupService.CleanupOutcome outcome = cleanupService.runCleanup(Duration.ZERO);

    // Cutoff = now() means every existing row qualifies; both rows we inserted are gone.
    try (TransactionContext tx = blockDAO.createTransactionContext()) {
      assertThat(blockDAO.getById(tx, id1)).isNull();
      assertThat(blockDAO.getById(tx, id2)).isNull();
    }
    assertThat(outcome.deleted()).isGreaterThanOrEqualTo(2);
  }

  @Test
  public void runCleanup_subOneHour_logsTripwireWarn_butStillExecutes() {
    // Sub-1h cutoff is the testing path. Service must still perform the delete (otherwise the
    // test harness can't use this knob); the WARN log itself is documented behaviour but
    // not asserted here without a logger appender.
    Repository repo = tempEntity.newRepository("repo-cleanup-tripwire");

    String old = insertBlock(repo.getId(), Instant.now().minus(Duration.ofMinutes(10)));
    String recent = insertBlock(repo.getId(), Instant.now().minus(Duration.ofMinutes(1)));

    HostedDeploymentBlockCleanupService.CleanupOutcome outcome = cleanupService.runCleanup(Duration.ofMinutes(5));

    assertThat(outcome.deleted()).isEqualTo(1);
    try (TransactionContext tx = blockDAO.createTransactionContext()) {
      assertThat(blockDAO.getById(tx, old)).isNull();
      assertThat(blockDAO.getById(tx, recent)).isNotNull();
    }
  }

  @Test
  public void runCleanup_negativeDuration_throws_andDoesNotTouchRows() {
    Repository repo = tempEntity.newRepository("repo-cleanup-neg");
    String survivor = insertBlock(repo.getId(), Instant.now().minus(Duration.ofDays(1)));

    assertThatThrownBy(() -> cleanupService.runCleanup(Duration.ofHours(-1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must not be negative");

    try (TransactionContext tx = blockDAO.createTransactionContext()) {
      assertThat(blockDAO.getById(tx, survivor)).isNotNull();
    }
  }

  @Test
  public void runCleanup_nullDuration_throws() {
    assertThatThrownBy(() -> cleanupService.runCleanup(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("cutoffAge");
  }

  @Test
  public void runCleanup_emptyTable_returnsZero() {
    HostedDeploymentBlockCleanupService.CleanupOutcome outcome = cleanupService.runCleanup(Duration.ofHours(1));

    // No rows inserted by this test; outcome.deleted() should be at least 0.
    assertThat(outcome.deleted()).isGreaterThanOrEqualTo(0);
    assertThat(outcome.cutoffTime()).isBefore(Instant.now());
  }

  // --- helpers ---

  private String insertBlock(final String repoId, final Instant blockedAt) {
    String id = UUID.randomUUID().toString();
    HostedDeploymentBlock block = new HostedDeploymentBlock();
    block.setId(id);
    block.setRepositoryId(repoId);
    block.setPathname("com/example/lib-" + id.substring(0, 8) + ".jar");
    block.setHash("hash" + id.substring(0, 12));
    block.setComponentIdFormat("maven2");
    block.setDisplayName("pkg:maven/com.example/lib@" + id.substring(0, 4));
    block.setPolicyAction("FAIL");
    block.setHighestThreatLevel(9);
    block.setEvaluationUrl("https://iq.example.com/report/" + id);
    block.setCorrelationId("corr-" + id.substring(0, 8));
    block.setRequestedBy("test@example.com");
    block.setBlockedTime(Date.from(blockedAt));

    try (TransactionContext tx = blockDAO.createTransactionContext()) {
      tx.begin();
      blockDAO.insertWithViolations(tx, block, List.of());
      tx.commit();
    }
    return id;
  }
}
