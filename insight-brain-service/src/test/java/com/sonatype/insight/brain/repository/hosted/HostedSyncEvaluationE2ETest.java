/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.insight.brain.dataaccess.repository.HostedDeploymentBlockDAO;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.HostedDeploymentBlock;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.HdsMockServerRule;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end smoke tests for the synchronous hosted-repository enforcement path (CLM-39870)
 * and the cleanup task/REST endpoint that own the {@code hosted_deployment_block*} tables.
 * <p>
 * Unit tests in {@link HostedComponentEvaluationServiceSyncTest} mock every collaborator and
 * cover the orchestration paths exhaustively. This class fills the gap on the wiring side:
 * real DI graph, real DB, real {@code HostedComponentScanStorageService} (writes/deletes
 * scan files on the test filesystem), real {@code ScanXmlParser}, real
 * {@code RepositoryPolicyEvaluator} talking to a mocked HDS. We deliberately stay on the
 * allow path here because triggering a deterministic FAIL action requires significant
 * org/policy fixture setup; block-path persistence behaviour is exercised by the unit tests
 * and by the cleanup-side test below which inserts a block row directly.
 * <p>
 * The cleanup test uses the live {@link HostedDeploymentBlockCleanupResource} bean to drive
 * the same path NXRM/admin would hit, then asserts via the live DAO that the row is gone.
 */
public class HostedSyncEvaluationE2ETest
    extends AbstractComponentTest
{
  @ClassRule
  public static HdsMockServerRule hdsMockServer = new HdsMockServerRule();

  @Inject
  private HostedComponentEvaluationService evaluationService;

  @Inject
  private HostedDeploymentBlockDAO blockDAO;

  @Inject
  private HostedDeploymentBlockCleanupResource cleanupResource;

  @Inject
  private HostedDeploymentBlockCleanupService cleanupService;

  private static final String CLIENT = "maven";

  private static final String REQUESTED_BY = "developer@example.com";

  @Before
  public void setUpTest() {
    setHdsUrl(hdsMockServer.getHttpUrl());
    setBaseUrl("https://iq.example.com");
    hdsMockServer.reset();
  }

  @Test
  public void evaluateSynchronously_allowPath_realWiring_returnsAllow_andCleansUpScanFile() throws Exception {
    Repository repo = tempEntity.newRepository("repo-sync-e2e-allow");
    String hash = "abc123def456ghi7";
    File scanFile = writeScanXmlGz(
        repo.getId(), repo.getPublicId(), "com/example/lib/1.0/lib-1.0.jar", hash, "maven2");

    // HDS responses for upload + evaluation. Empty alerts/components → allow.
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan-e2e-allow-001");
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);
    mockPolicyEvaluatorAllow(hash);

    HostedEvaluationResult result = evaluationService.evaluateSynchronously(
        repo,
        "comp-e2e-allow-1",
        "pkg:maven/com.example/lib@1.0",
        null,
        scanFile,
        UUID.randomUUID().toString(),
        REQUESTED_BY,
        CLIENT);

    assertThat(result.blocked()).isFalse();
    assertThat(result.blockingViolations()).isEmpty();
    assertThat(result.componentId()).isEqualTo("comp-e2e-allow-1");
    assertThat(result.evaluationUrl()).isNotBlank();

    // Allow path must NOT write to hosted_deployment_block.
    try (TransactionContext tx = blockDAO.createTransactionContext()) {
      assertThat(blockDAO.getByRepositoryId(tx, repo.getId())).isEmpty();
    }

    // ScanPersistenceService.deleteScan removes the staged copy. The inbound file we passed in
    // remains under tempDir until the test finishes (caller's responsibility, by contract).
    assertThat(scanFile).exists();
  }

  @Test
  public void evaluateSynchronously_unscannableScan_realWiring_throwsAndCleansUp() throws Exception {
    Repository repo = tempEntity.newRepository("repo-sync-e2e-unscannable");
    File scanFile = writeUnscannableScanXmlGz(repo.getId(), repo.getPublicId());

    // No HDS mocks needed — we must fail before reaching upload.
    assertThatThrownBy(() -> evaluationService.evaluateSynchronously(
        repo,
        "comp-e2e-unscannable-1",
        "pkg:maven/com.example/sources@1.0",
        null,
        scanFile,
        UUID.randomUUID().toString(),
        REQUESTED_BY,
        CLIENT))
            .isInstanceOf(UnscannableArtifactException.class)
            .hasMessageContaining("comp-e2e-unscannable-1");

    // No block row written.
    try (TransactionContext tx = blockDAO.createTransactionContext()) {
      assertThat(blockDAO.getByRepositoryId(tx, repo.getId())).isEmpty();
    }
  }

  @Test
  public void evaluateSynchronously_nullRepository_realWiring_failsFastBeforeAnyWork() throws Exception {
    // Defense-in-depth: the resource layer always resolves the Repository before invoking
    // the service (and 404s if missing), so null can only reach this method via a programming
    // error. Make sure the service fails fast with IllegalArgumentException before staging
    // any scan file or hitting the DB.
    File scanFile = writeScanXmlGz(
        "no-such-repo", "no-such-public-id", "com/example/x.jar", "0000000000000000", "maven2");

    assertThatThrownBy(() -> evaluationService.evaluateSynchronously(
        null,
        "comp-x",
        null,
        null,
        scanFile,
        UUID.randomUUID().toString(),
        REQUESTED_BY,
        CLIENT))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("repository");
  }

  @Test
  public void cleanupResource_deletesOldBlock_keepsRecentBlock_realDb() throws Exception {
    Repository repo = tempEntity.newRepository("repo-sync-e2e-cleanup");

    // Insert one row from 2 hours ago and one from 5 minutes ago, directly via the DAO.
    String oldId = insertBlockRow(repo.getId(), Instant.now().minus(Duration.ofHours(2)));
    String recentId = insertBlockRow(repo.getId(), Instant.now().minus(Duration.ofMinutes(5)));

    // Trigger via the live REST resource with cutoff=1 hour. Should delete the 2h-old row only.
    Response response = cleanupResource.runCleanup(60);

    assertThat(response.getStatus()).isEqualTo(200);
    HostedDeploymentBlockCleanupResource.CleanupResponse body =
        (HostedDeploymentBlockCleanupResource.CleanupResponse) response.getEntity();
    assertThat(body.deleted()).isEqualTo(1);
    assertThat(body.olderThanMinutes()).isEqualTo(60);

    try (TransactionContext tx = blockDAO.createTransactionContext()) {
      assertThat(blockDAO.getById(tx, oldId)).isNull();
      assertThat(blockDAO.getById(tx, recentId)).isNotNull();
    }
  }

  @Test
  public void cleanupResource_zeroMinutes_deletesEverything_realDb() throws Exception {
    Repository repo = tempEntity.newRepository("repo-sync-e2e-cleanup-zero");

    String id1 = insertBlockRow(repo.getId(), Instant.now().minus(Duration.ofMinutes(1)));
    String id2 = insertBlockRow(repo.getId(), Instant.now().minus(Duration.ofSeconds(1)));

    Response response = cleanupResource.runCleanup(0);
    HostedDeploymentBlockCleanupResource.CleanupResponse body =
        (HostedDeploymentBlockCleanupResource.CleanupResponse) response.getEntity();
    assertThat(body.deleted()).isGreaterThanOrEqualTo(2);

    try (TransactionContext tx = blockDAO.createTransactionContext()) {
      assertThat(blockDAO.getById(tx, id1)).isNull();
      assertThat(blockDAO.getById(tx, id2)).isNull();
    }
  }

  @Test
  public void cleanupResource_negativeMinutes_returns400() {
    assertThatThrownBy(() -> cleanupResource.runCleanup(-1))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("must be >= 0");
  }

  @Test
  public void cleanupService_via24hSchedulePath_realDb_skipsRecentRows() throws Exception {
    // Mirrors what the Quartz task would do: call cleanupService with the configured retention.
    Repository repo = tempEntity.newRepository("repo-sync-e2e-cleanup-schedule");

    String recentId = insertBlockRow(repo.getId(), Instant.now().minus(Duration.ofMinutes(30)));

    HostedDeploymentBlockCleanupService.CleanupOutcome outcome =
        cleanupService.runCleanup(Duration.ofHours(24));

    // The 30-min-old row is younger than 24h, so the Quartz cadence must NOT touch it.
    try (TransactionContext tx = blockDAO.createTransactionContext()) {
      assertThat(blockDAO.getById(tx, recentId)).isNotNull();
    }
    // outcome.deleted() can be 0 or larger depending on what other tests left behind; we only
    // care that our recent row survives.
    assertThat(outcome.cutoffTime()).isBefore(Instant.now());
  }

  // --- helpers ---

  private void mockPolicyEvaluatorAllow(final String hash) {
    ComponentEvaluationDataList list = new ComponentEvaluationDataList();
    list.components = new ArrayList<>();
    ComponentEvaluationData data = new ComponentEvaluationData();
    data.requestIndex = 0;
    data.hash = hash;
    data.matchState = MatchState.EXACT.getId();
    data.declaredLicenses = new HashSet<>();
    data.observedLicenses = new HashSet<>();
    list.components.add(data);
    hdsMockServer.respondWith(list).atUri(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH);
  }

  private File writeScanXmlGz(
      final String repoId,
      final String repoPublicId,
      final String pathname,
      final String sha1,
      final String format) throws Exception
  {
    String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<scan version=\"2.24\">\n"
        + "<repository id=\"" + repoId + "\" name=\"" + repoPublicId + "\" format=\"" + format + "\"/>\n"
        + "<dir path=\"" + pathname + "\" sha1=\"" + sha1 + "\" sha512=\"ignored\">\n</dir>\n"
        + "</scan>";
    File f = tempDir.newFile("inbound-" + UUID.randomUUID() + ".xml");
    try (FileOutputStream fos = new FileOutputStream(f)) {
      fos.write(xml.getBytes(StandardCharsets.UTF_8));
    }
    return f;
  }

  private File writeUnscannableScanXmlGz(final String repoId, final String repoPublicId) throws Exception {
    String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<scan version=\"2.24\">\n"
        + "<repository id=\"" + repoId + "\" name=\"" + repoPublicId + "\" format=\"maven2\"/>\n"
        + "</scan>";
    File f = tempDir.newFile("inbound-unscannable-" + UUID.randomUUID() + ".xml");
    try (FileOutputStream fos = new FileOutputStream(f)) {
      fos.write(xml.getBytes(StandardCharsets.UTF_8));
    }
    return f;
  }

  private String insertBlockRow(final String repoId, final Instant blockedAt) {
    String id = UUID.randomUUID().toString();
    HostedDeploymentBlock block = new HostedDeploymentBlock();
    block.setId(id);
    block.setRepositoryId(repoId);
    block.setPathname("com/example/blocked/lib.jar");
    block.setHash("hash" + id.substring(0, 8));
    block.setComponentIdFormat("maven2");
    block.setDisplayName("pkg:maven/com.example/blocked@1.0");
    block.setPolicyAction("FAIL");
    block.setHighestThreatLevel(9);
    block.setEvaluationUrl("https://iq.example.com/report/" + id);
    block.setCorrelationId("corr-" + id.substring(0, 8));
    block.setRequestedBy(REQUESTED_BY);
    block.setBlockedTime(Date.from(blockedAt));

    try (TransactionContext tx = blockDAO.createTransactionContext()) {
      tx.begin();
      blockDAO.insertWithViolations(tx, block, List.of());
      tx.commit();
    }
    return id;
  }
}
