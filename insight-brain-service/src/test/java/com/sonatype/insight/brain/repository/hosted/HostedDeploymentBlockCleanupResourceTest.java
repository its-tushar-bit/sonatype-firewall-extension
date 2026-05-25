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
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.dataaccess.repository.HostedDeploymentBlockDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.repository.HostedDeploymentBlock;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HostedDeploymentBlockCleanupResourceTest
    extends AbstractComponentTest
{
  @Inject
  private HostedDeploymentBlockCleanupResource cleanupResource;

  @Inject
  private HostedDeploymentBlockDAO blockDAO;

  @Test
  public void runCleanup_paramAbsent_usesConfiguredRetention_default24h() {
    // Default retention is 24h. Insert one 25h-old row and one 1h-old row; default cleanup
    // should delete the 25h row only.
    Repository repo = tempEntity.newRepository("repo-resource-default");
    String old = insertBlock(repo.getId(), Instant.now().minus(Duration.ofHours(25)));
    String recent = insertBlock(repo.getId(), Instant.now().minus(Duration.ofHours(1)));

    Response response = cleanupResource.runCleanup(null);

    assertThat(response.getStatus()).isEqualTo(200);
    HostedDeploymentBlockCleanupResource.CleanupResponse body =
        (HostedDeploymentBlockCleanupResource.CleanupResponse) response.getEntity();
    assertThat(body.olderThanMinutes()).isEqualTo(Duration.ofHours(24).toMinutes());

    try (TransactionContext tx = blockDAO.createTransactionContext()) {
      assertThat(blockDAO.getById(tx, old)).isNull();
      assertThat(blockDAO.getById(tx, recent)).isNotNull();
    }
  }

  @Test
  public void runCleanup_explicitFiveMinutes_deletesRowsOlderThanFiveMinutes() {
    Repository repo = tempEntity.newRepository("repo-resource-5min");
    String old = insertBlock(repo.getId(), Instant.now().minus(Duration.ofMinutes(10)));
    String recent = insertBlock(repo.getId(), Instant.now().minus(Duration.ofMinutes(1)));

    Response response = cleanupResource.runCleanup(5);

    assertThat(response.getStatus()).isEqualTo(200);
    HostedDeploymentBlockCleanupResource.CleanupResponse body =
        (HostedDeploymentBlockCleanupResource.CleanupResponse) response.getEntity();
    assertThat(body.olderThanMinutes()).isEqualTo(5);

    try (TransactionContext tx = blockDAO.createTransactionContext()) {
      assertThat(blockDAO.getById(tx, old)).isNull();
      assertThat(blockDAO.getById(tx, recent)).isNotNull();
    }
  }

  @Test
  public void runCleanup_explicitZero_deletesEverything() {
    Repository repo = tempEntity.newRepository("repo-resource-zero");
    String id1 = insertBlock(repo.getId(), Instant.now().minus(Duration.ofMinutes(1)));
    String id2 = insertBlock(repo.getId(), Instant.now().minus(Duration.ofSeconds(1)));

    Response response = cleanupResource.runCleanup(0);

    assertThat(response.getStatus()).isEqualTo(200);
    try (TransactionContext tx = blockDAO.createTransactionContext()) {
      assertThat(blockDAO.getById(tx, id1)).isNull();
      assertThat(blockDAO.getById(tx, id2)).isNull();
    }
  }

  @Test
  public void runCleanup_negative_throwsBadRequest_andDoesNotTouchRows() {
    Repository repo = tempEntity.newRepository("repo-resource-neg");
    String survivor = insertBlock(repo.getId(), Instant.now().minus(Duration.ofDays(1)));

    assertThatThrownBy(() -> cleanupResource.runCleanup(-1))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("olderThanMinutes must be >= 0");

    try (TransactionContext tx = blockDAO.createTransactionContext()) {
      assertThat(blockDAO.getById(tx, survivor)).isNotNull();
    }
  }

  @Test
  public void runCleanup_paramAbsent_honoursOverriddenRetentionProperty() throws Exception {
    setRetentionHours(1);
    try {
      Repository repo = tempEntity.newRepository("repo-resource-override");
      String old = insertBlock(repo.getId(), Instant.now().minus(Duration.ofHours(2)));
      String recent = insertBlock(repo.getId(), Instant.now().minus(Duration.ofMinutes(30)));

      Response response = cleanupResource.runCleanup(null);

      HostedDeploymentBlockCleanupResource.CleanupResponse body =
          (HostedDeploymentBlockCleanupResource.CleanupResponse) response.getEntity();
      assertThat(body.olderThanMinutes()).isEqualTo(Duration.ofHours(1).toMinutes());

      try (TransactionContext tx = blockDAO.createTransactionContext()) {
        assertThat(blockDAO.getById(tx, old)).isNull();
        assertThat(blockDAO.getById(tx, recent)).isNotNull();
      }
    }
    finally {
      setRetentionHours(24);
    }
  }

  @Test
  public void runCleanup_largeValue_passesThroughUnclamped() {
    // A retention larger than any row's age should delete nothing. Verifies no upper-bound
    // clamping silently changes the cutoff.
    Repository repo = tempEntity.newRepository("repo-resource-large");
    String survivor = insertBlock(repo.getId(), Instant.now().minus(Duration.ofMinutes(5)));

    Response response = cleanupResource.runCleanup(999_999);

    HostedDeploymentBlockCleanupResource.CleanupResponse body =
        (HostedDeploymentBlockCleanupResource.CleanupResponse) response.getEntity();
    assertThat(body.olderThanMinutes()).isEqualTo(999_999);

    try (TransactionContext tx = blockDAO.createTransactionContext()) {
      assertThat(blockDAO.getById(tx, survivor)).isNotNull();
    }
  }

  // --- helpers ---

  private void setRetentionHours(final int hours) {
    lookup(ApiConfigurationService.class)
        .setConfigurationInDatabaseNoAuthz(
            SystemConfigurationProperty.HOSTED_DEPLOYMENT_BLOCK_RETENTION_HOURS, Integer.valueOf(hours));
    lookup(ApiConfigurationService.class)
        .applyConfigurationToClients(SystemConfigurationProperty.HOSTED_DEPLOYMENT_BLOCK_RETENTION_HOURS);
  }

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
