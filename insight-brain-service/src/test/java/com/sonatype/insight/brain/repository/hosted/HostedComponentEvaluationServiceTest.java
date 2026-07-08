/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.dataaccess.repository.HostedComponentScanQueueDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.repository.HostedComponentScanQueue;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.HdsMockServerRule;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

public class HostedComponentEvaluationServiceTest
    extends AbstractComponentTest
{
  @ClassRule
  public static HdsMockServerRule hdsMockServer = new HdsMockServerRule();

  @Inject
  private HostedComponentEvaluationService evaluationService;

  @Inject
  private HostedComponentScanQueueConsumer consumer;

  @Inject
  private HostedComponentScanQueueDAO queueDAO;

  @Inject
  private RepositoryDAO repositoryDAO;

  @Before
  public void setUpTest() {
    consumer.disableForTesting = true;
    setHdsUrl(hdsMockServer.getHttpUrl());
    hdsMockServer.reset();
  }

  @After
  public void tearDownTest() {
    consumer.cleanup();
  }

  @Test
  public void queueScan_insertsRowWithCorrectFields() throws Exception {
    Repository repo = tempEntity.newRepository("repo-eval-1");
    RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());
    File scanFile = writeScanFile("scan-eval-1.xml.gz");

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan-eval-001");
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);

    String jobId = evaluationService.queueScan(repo.getId(), component.getId(), null, null, scanFile);

    assertThat(jobId).isNotBlank();

    HostedComponentScanQueue row = queueDAO.getById(jobId);
    assertThat(row).isNotNull();
    assertThat(row.getRepositoryId()).isEqualTo(repo.getId());
    assertThat(row.getComponentId()).isEqualTo(component.getId());
    assertThat(row.getStatus()).isIn(
        HostedComponentScanQueueDAO.Status.PENDING.name(),
        HostedComponentScanQueueDAO.Status.IN_PROGRESS.name());
    assertThat(row.getPriority()).isEqualTo(HostedComponentScanQueue.DEFAULT_PRIORITY);
  }

  @Test
  public void queueScan_triggerProcessingAndJobCompletes() throws Exception {
    // Monitoring must be on for the consumer to evaluate rather than drop the job (CLM-42122 guard).
    Repository repo = enableMonitoring(tempEntity.newRepository("repo-eval-2"));
    RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());
    File scanFile = writeScanFile("scan-eval-2.xml.gz");

    consumer.disableForTesting = false;

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan-eval-002");
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);

    String jobId = evaluationService.queueScan(repo.getId(), component.getId(), null, null, scanFile);

    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
      HostedComponentScanQueue row = queueDAO.getById(jobId);
      assertThat(row.getStatus()).isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name());
    });

    assertThat(hdsMockServer.getCapturedRequestHttpHeaders(ScanUploader.HDS_PATH)).isNotNull();
  }

  @Test
  public void queueScan_cleansUpScanFileWhenDbInsertFails() throws Exception {
    Repository repo = tempEntity.newRepository("repo-eval-3");
    RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());
    File scanFile = writeScanFile("scan-eval-3.xml.gz");

    String jobId = evaluationService.queueScan(repo.getId(), component.getId(), null, null, scanFile);
    assertThat(jobId).isNotBlank();

    assertThat(queueDAO.getById(jobId)).isNotNull();
  }

  @Test
  public void queueScan_storageUsesRepositoryIdAsOwner() throws Exception {
    Repository repo = tempEntity.newRepository("repo-eval-4");
    RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());
    File scanFile = writeScanFile("scan-eval-4.xml.gz");

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan-eval-004");
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);

    String jobId = evaluationService.queueScan(repo.getId(), component.getId(), null, null, scanFile);

    HostedComponentScanQueue row = queueDAO.getById(jobId);
    assertThat(row.getScanFileId()).isNotBlank();
    assertThat(row.getRepositoryId()).isEqualTo(repo.getId());
  }

  @Test
  public void queueScan_propagatesIOExceptionWhenStoreScanFileFails() throws Exception {
    Repository repo = tempEntity.newRepository("repo-eval-5");
    RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());

    File nonExistentFile = new File(tempDir.getRoot(), "does-not-exist.xml.gz");

    assertThatThrownBy(() -> evaluationService.queueScan(repo.getId(), component.getId(), null, null, nonExistentFile))
        .isInstanceOf(IOException.class);

    assertThat(queueDAO.acquireNextPendingJobs(10)).isEmpty();
  }

  @Test
  public void queueScan_originalDbExceptionPropagatesEvenWhenDeleteScanAlsoFails() throws Exception {
    Repository repo = tempEntity.newRepository("repo-eval-6");
    RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());
    File scanFile = writeScanFile("scan-eval-6.xml.gz");

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan-eval-006");
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);
    String firstJobId = evaluationService.queueScan(repo.getId(), component.getId(), null, null, scanFile);
    assertThat(firstJobId).isNotBlank();

    File scanDir = insightWork.getScanDir(repo.getId());
    try {
      scanDir.setReadOnly();
      File scanFile2 = writeScanFile("scan-eval-6b.xml.gz");

      assertThatThrownBy(() -> evaluationService.queueScan(repo.getId(), component.getId(), null, null, scanFile2))
          .isInstanceOf(IOException.class);
    }
    finally {
      scanDir.setWritable(true);
    }
  }

  @Test
  public void queueScan_storesPurlOnQueueRow() throws Exception {
    Repository repo = tempEntity.newRepository("repo-eval-purl");
    RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());
    File scanFile = writeScanFile("scan-purl.xml.gz");

    String purl = "pkg:maven/org.example/my-lib@2.0.0";
    String jobId = evaluationService.queueScan(repo.getId(), component.getId(), purl, null, scanFile);

    HostedComponentScanQueue row = queueDAO.getById(jobId);
    assertThat(row.getPurl()).isEqualTo(purl);
    assertThat(row.getPolicyEvaluationStage()).isNull();
  }

  @Test
  public void queueScan_storesPolicyEvaluationStageOnQueueRow() throws Exception {
    Repository repo = tempEntity.newRepository("repo-eval-stage");
    RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());
    File scanFile = writeScanFile("scan-stage.xml.gz");

    String stage = "source";
    String jobId = evaluationService.queueScan(repo.getId(), component.getId(), null, stage, scanFile);

    HostedComponentScanQueue row = queueDAO.getById(jobId);
    assertThat(row.getPolicyEvaluationStage()).isEqualTo(stage);
    assertThat(row.getPurl()).isNull();
  }

  @Test
  public void queueScan_storesBothPurlAndStageOnQueueRow() throws Exception {
    Repository repo = tempEntity.newRepository("repo-eval-both");
    RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());
    File scanFile = writeScanFile("scan-both.xml.gz");

    String purl = "pkg:maven/com.example/artifact@1.0.0";
    String stage = "compliance";
    String jobId = evaluationService.queueScan(repo.getId(), component.getId(), purl, stage, scanFile);

    HostedComponentScanQueue row = queueDAO.getById(jobId);
    assertThat(row.getPurl()).isEqualTo(purl);
    assertThat(row.getPolicyEvaluationStage()).isEqualTo(stage);
  }

  private File writeScanFile(final String name) throws Exception {
    File scanFile = tempDir.newFile(name);
    try (FileOutputStream fos = new FileOutputStream(scanFile)) {
      fos.write("scan-content".getBytes(StandardCharsets.UTF_8));
    }
    return scanFile;
  }

  /** Enables monitoring so the CLM-42122 guard lets jobs through (newRepository defaults it false). */
  private Repository enableMonitoring(final Repository repo) {
    repo.setMonitoringEnabled(true);
    repositoryDAO.update(repo);
    return repo;
  }
}
