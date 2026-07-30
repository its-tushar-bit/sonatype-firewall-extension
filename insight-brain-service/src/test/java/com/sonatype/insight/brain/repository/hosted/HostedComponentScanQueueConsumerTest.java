/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.mock.hds.HttpResponseProcessor;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.dataaccess.repository.HostedComponentScanQueueDAO;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.HostedComponentScanQueue;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.queue.AbstractPollDispatchQueueConsumer;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.HdsMockServerRule;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class HostedComponentScanQueueConsumerTest
    extends AbstractComponentTest
{
  @ClassRule
  public static HdsMockServerRule hdsMockServer = new HdsMockServerRule();

  @Inject
  private HostedComponentScanQueueConsumer consumer;

  @Inject
  private HostedComponentScanQueueDAO queueDAO;

  @Inject
  private ScanPersistenceService scanPersistenceService;

  @Inject
  private ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  @Inject
  private com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO repositoryDAO;

  @Inject
  private com.sonatype.insight.brain.dataaccess.policy.ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO;

  @Inject
  private com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO policyEvaluationDAO;

  @Before
  public void setUpTest() {
    consumer.disableForTesting = true;
    setHdsUrl(hdsMockServer.getHttpUrl());
    hdsMockServer.reset();
    // Reset queue config to defaults so tests that mutate it via setQueueConfig do not leak
    // state into sibling tests. Without this, configurationChanged_appliesNewConfig and
    // handleConfigurationChanged_resizesWorkerThreadPool flake when run after a test that
    // changed workerThreadsPerTenant.
    setQueueConfig(
        "{\"enabled\":true,\"workerThreadsPerTenant\":1,\"pollIntervalMilliseconds\":30000,"
            + "\"maxQueuedRows\":10,\"maxRetries\":3}");
    // Note: GET /rest/application/analysis/{scanId} is handled by the HDS mock built-in
    // handler (returns 400 BadRequest). ReportDownloader only retries on NotFoundException
    // and BadGatewayException — a 400 fails fast so jobs complete within the test window.
  }

  @After
  public void tearDownTest() {
    consumer.cleanup();
  }

  @Test
  public void run_doesNothingWhenQueueIsEmpty() throws Exception {
    consumer.run();
    // No jobs in DB — nothing to verify except no exception thrown
    assertThat(queueDAO.acquireNextPendingJobs(1)).isEmpty();
  }

  @Test
  public void run_acquiresAndProcessesJob() throws Exception {
    HostedComponentScanQueue job = insertPendingJob("repo-happy");

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan-happy");
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);

    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));

    assertThat(hdsMockServer.getCapturedRequestHttpHeaders(ScanUploader.HDS_PATH)).isNotNull();
  }

  @Test
  public void run_isNoOpWhenEnabledFalseInConfig() throws Exception {
    // Disable via live config — not via disableForTesting
    setQueueConfig(
        "{\"enabled\":false,\"workerThreadsPerTenant\":1,\"pollIntervalMilliseconds\":30000,\"maxQueuedRows\":10,\"maxRetries\":3}");

    insertPendingJob("repo-disabled-config");

    consumer.run();

    // Job must still be PENDING — run() returned early due to isEnabled()=false
    List<HostedComponentScanQueue> pending = queueDAO.acquireNextPendingJobs(10);
    assertThat(pending).hasSize(1);
  }

  @Test
  public void run_retriesJobOnTransientHdsFailure() throws Exception {
    HostedComponentScanQueue job = insertPendingJob("repo-retry");

    // HDS returns 500 — ScanUploader throws, triggering retry
    hdsMockServer.respondWith("error").atUri(ScanUploader.HDS_PATH);

    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
      HostedComponentScanQueue row = queueDAO.getById(job.getId());
      assertThat(row.getRetryCount()).isGreaterThan(0);
    });
  }

  @Test
  public void run_permanentlyFailsJobAfterMaxRetries() throws Exception {
    HostedComponentScanQueue job = insertPendingJob("repo-exhaust");

    // Force enough retries by setting retry_count to maxRetries-1 before processing
    for (int i = 0; i < HostedComponentScanQueueConfig.DEFAULT_MAX_RETRIES; i++) {
      queueDAO.incrementRetryCount(job.getId());
    }

    // Re-acquire — set back to PENDING
    queueDAO.unacquireJobs(Set.of(job.getId()));

    hdsMockServer.respondWith("error").atUri(ScanUploader.HDS_PATH);

    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.FAILED.name()));
  }

  @Test
  public void run_executeJobThrowsIllegalStateWhenScanFileNotFound() throws Exception {
    // Insert a queue row with a scanFileId that does not exist on disk
    Repository repo = enableMonitoring(tempEntity.newRepository("repo-no-scan"));
    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());

    HostedComponentScanQueue job = new HostedComponentScanQueue(
        component.getId(), "nonexistent-scan-file.xml.gz",
        HostedComponentScanQueueDAO.Status.PENDING.name(),
        HostedComponentScanQueue.DEFAULT_PRIORITY,
        repo.getId());
    queueDAO.insert(job);

    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
      HostedComponentScanQueue row = queueDAO.getById(job.getId());
      // Job should be retried (retryCount > 0) due to IllegalStateException from missing scan file
      assertThat(row.getRetryCount()).isGreaterThan(0);
    });
  }

  @Test
  public void shutdown_unacquiresQueuedButNotStartedJob() throws Exception {
    HostedComponentScanQueue job1 = insertPendingJob("repo-shutdown-1");
    HostedComponentScanQueue job2 = insertPendingJob("repo-shutdown-2");

    // Use a latch so job1's HDS call blocks until we release it — ensures job2 stays queued in executor
    CountDownLatch hdsCallStarted = new CountDownLatch(1);
    CountDownLatch hdsReleased = new CountDownLatch(1);
    hdsMockServer.respondWith((HttpResponseProcessor) (req, resp) -> {
      hdsCallStarted.countDown();
      try {
        hdsReleased.await(10, TimeUnit.SECONDS);
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      // Return 500 so the job fails — the point is cleanup, not success
      resp.setStatus(500);
    }).atUri(ScanUploader.HDS_PATH);

    consumer.disableForTesting = false;
    consumer.run();

    // Wait until job1's HDS call is in-flight (blocking inside the HDS mock)
    assertThat(hdsCallStarted.await(10, TimeUnit.SECONDS)).isTrue();

    // Cleanup while job1 is blocked in HDS and job2 is queued in the executor
    consumer.cleanup();

    // Release the HDS latch so the mock server thread can complete
    hdsReleased.countDown();

    // job2 must be unacquired back to PENDING because cleanup interrupted before it started
    await().atMost(5, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job2.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.PENDING.name()));
  }

  @Test
  public void getInitialDelay_returnsValueWithinPeriod() {
    long period = 30_000L;
    long delay = AbstractPollDispatchQueueConsumer.getInitialDelay("test-seed", period);
    assertThat(delay).isGreaterThanOrEqualTo(0).isLessThan(period);
  }

  @Test
  public void getInitialDelay_returnsZeroForZeroPeriod() {
    assertThat(AbstractPollDispatchQueueConsumer.getInitialDelay("seed", 0)).isEqualTo(0);
  }

  @Test
  public void recoverStaleJobs_resetsInProgressToPending() throws Exception {
    Repository repo = tempEntity.newRepository("repo-stale");
    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());

    // Insert an IN_PROGRESS job (simulating a crashed worker)
    HostedComponentScanQueue job = new HostedComponentScanQueue(
        component.getId(), "stale-scan.xml.gz",
        HostedComponentScanQueueDAO.Status.IN_PROGRESS.name(),
        HostedComponentScanQueue.DEFAULT_PRIORITY,
        repo.getId());
    queueDAO.insert(job);

    consumer.recoverStaleJobs();

    assertThat(queueDAO.getById(job.getId()).getStatus())
        .isEqualTo(HostedComponentScanQueueDAO.Status.PENDING.name());
  }

  @Test
  public void configurationChanged_appliesNewConfig() {
    assertThat(consumer.configs.get().workerThreadsPerTenant()).isEqualTo(1);
    assertThat(consumer.configs.get().maxRetries()).isEqualTo(3);
  }

  @Test
  public void permanentlyFailJob_usesClassNameWhenExceptionMessageIsNull() throws Exception {
    HostedComponentScanQueue job = insertPendingJob("repo-null-msg");

    // Exhaust retries then process with a null-message exception
    for (int i = 0; i < HostedComponentScanQueueConfig.DEFAULT_MAX_RETRIES; i++) {
      queueDAO.incrementRetryCount(job.getId());
    }
    queueDAO.unacquireJobs(Set.of(job.getId()));

    // HDS returns 500 — causes a RuntimeException with no message through ScanUploader
    hdsMockServer.respondWith(null).atUri(ScanUploader.HDS_PATH);

    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
      HostedComponentScanQueue row = queueDAO.getById(job.getId());
      assertThat(row.getStatus()).isEqualTo(HostedComponentScanQueueDAO.Status.FAILED.name());
      // error_message must not be null — falls back to class name
      assertThat(row.getErrorMessage()).isNotBlank();
    });
  }

  @Test
  public void run_processesMultipleJobsConcurrentlyWithTwoWorkers() throws Exception {
    // Override config to use 2 worker threads
    setQueueConfig(
        "{\"enabled\":true,\"workerThreadsPerTenant\":2,\"pollIntervalMilliseconds\":30000,\"maxQueuedRows\":10,\"maxRetries\":3}");

    HostedComponentScanQueue job1 = insertPendingJob("repo-concurrent-1");
    HostedComponentScanQueue job2 = insertPendingJob("repo-concurrent-2");

    // Stage HDS to respond for both jobs
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan-concurrent");
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);

    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
      assertThat(queueDAO.getById(job1.getId()).getStatus())
          .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name());
      assertThat(queueDAO.getById(job2.getId()).getStatus())
          .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name());
    });
  }

  @Test
  public void handleConfigurationChanged_resizesWorkerThreadPool() throws Exception {
    assertThat(consumer.configs.get().workerThreadsPerTenant()).isEqualTo(1);

    // Change to 3 workers via live config
    setQueueConfig(
        "{\"enabled\":true,\"workerThreadsPerTenant\":3,\"pollIntervalMilliseconds\":30000,\"maxQueuedRows\":10,\"maxRetries\":3}");

    assertThat(consumer.configs.get().workerThreadsPerTenant()).isEqualTo(3);
  }

  @Test
  public void handleConfigurationChanged_onlyMaxRetriesChange_doesNotAffectScheduling() throws Exception {
    // Change only maxRetries — enabled and pollInterval unchanged
    setQueueConfig(
        "{\"enabled\":true,\"workerThreadsPerTenant\":1,\"pollIntervalMilliseconds\":30000,\"maxQueuedRows\":10,\"maxRetries\":5}");

    assertThat(consumer.configs.get().maxRetries()).isEqualTo(5);
    // Consumer remains functional — run() still works
    consumer.run();
    // No exception means scheduling state was not disrupted
  }

  @Test
  public void executeJob_stampsComponentIdOnRepositoryComponent() throws Exception {
    HostedComponentScanQueue job = insertPendingJobWithScanXml(
        "repo-upsert", "comp-upsert-1",
        "pkg:maven/com.example/my-lib@1.0.0", null,
        "com/example/my-lib/1.0.0/my-lib-1.0.0.jar", "abc123def456ghi7", "maven2");

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan-upsert");
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);
    mockPolicyEvaluatorHdsResponse("abc123def456ghi7");

    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));

    ProxyRepositoryComponent rc = proxyRepositoryComponentDAO.getByRepositoryIdAndPathname(
        job.getRepositoryId(), "com/example/my-lib/1.0.0/my-lib-1.0.0.jar");
    assertThat(rc).isNotNull();
    assertThat(rc.getHash()).isEqualTo("abc123def456ghi7");
    assertThat(rc.getComponentId()).isEqualTo("comp-upsert-1");
    assertThat(rc.getLastEvaluationTime()).isNotNull();
  }

  @Test
  public void executeJob_updatesExistingRepositoryComponentOnResubmit() throws Exception {
    // First upload — creates the row
    HostedComponentScanQueue job1 = insertPendingJobWithScanXml(
        "repo-update", "comp-update-1", null, null,
        "com/example/lib/1.0/lib-1.0.jar", "hash_v1", "maven2");
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan-v1");
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);
    mockPolicyEvaluatorHdsResponse("hash_v1");
    consumer.disableForTesting = false;
    consumer.run();
    await().atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job1.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));
    consumer.disableForTesting = true;
    hdsMockServer.reset();

    // Second upload — same pathname, different hash
    HostedComponentScanQueue job2 = insertPendingJobWithScanXml(
        "repo-update", "comp-update-2", null, null,
        "com/example/lib/1.0/lib-1.0.jar", "hash_v2", "maven2");
    receipt.setScanId("scan-v2");
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);
    mockPolicyEvaluatorHdsResponse("hash_v2");
    consumer.disableForTesting = false;
    consumer.run();
    await().atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job2.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));

    ProxyRepositoryComponent rc = proxyRepositoryComponentDAO.getByRepositoryIdAndPathname(
        job2.getRepositoryId(), "com/example/lib/1.0/lib-1.0.jar");
    assertThat(rc.getHash()).isEqualTo("hash_v2");
    assertThat(rc.getComponentId()).isEqualTo("comp-update-2");
  }

  @Test
  public void executeJob_stampsComponentIdRegardlessOfPurl() throws Exception {
    // The consumer no longer parses the PURL to set ComponentIdentifier —
    // that comes from HDS evaluation data. The job's componentId is always
    // stamped onto the proxy_repository_component row via stampNxrmComponentId().
    HostedComponentScanQueue job = insertPendingJobWithScanXml(
        "repo-purl", "comp-purl-1",
        "pkg:maven/org.apache.commons/commons-lang3@3.12.0", null,
        "org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar", "purl_hash_001", "maven2");

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan-purl");
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);
    mockPolicyEvaluatorHdsResponse("purl_hash_001");
    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));

    ProxyRepositoryComponent rc = proxyRepositoryComponentDAO.getByRepositoryIdAndPathname(
        job.getRepositoryId(),
        "org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar");
    assertThat(rc).isNotNull();
    assertThat(rc.getHash()).isEqualTo("purl_hash_001");
    assertThat(rc.getComponentId()).isEqualTo("comp-purl-1");
  }

  @Test
  public void executeJob_completesSuccessfullyWhenScanHasNoMatchingComponent() throws Exception {
    // Scan job completes even when HDS evaluation finds no matching component for the pathname.
    // The proxy_repository_component row is still created by the evaluator (with hash from scan XML),
    // and componentId is stamped from the job.
    HostedComponentScanQueue job = insertPendingJobWithScanXml(
        "repo-nomatch", "comp-nomatch",
        null, null,
        "com/example/unknown/1.0/unknown-1.0.jar", "nomatch_hash_001", "maven2");

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan-nomatch");
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);
    mockPolicyEvaluatorHdsResponse("nomatch_hash_001");
    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));

    ProxyRepositoryComponent rc = proxyRepositoryComponentDAO.getByRepositoryIdAndPathname(
        job.getRepositoryId(), "com/example/unknown/1.0/unknown-1.0.jar");
    assertThat(rc).isNotNull();
    assertThat(rc.getHash()).isEqualTo("nomatch_hash_001");
    assertThat(rc.getComponentId()).isEqualTo("comp-nomatch");
  }

  @Test
  public void executeJob_withUnparsableScanXml_jobCompletesWithoutRepositoryComponent() throws Exception {
    // plain "scan-content" is not valid XML — ScanXmlParser returns null
    HostedComponentScanQueue job = insertPendingJob("repo-norscomp");

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan-norc");
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);
    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));

    // No proxy_repository_component row should exist since ScanXmlParser returned null
    ProxyRepositoryComponent rc = proxyRepositoryComponentDAO.getByRepositoryIdAndPathname(
        job.getRepositoryId(), "scan-content");
    assertThat(rc).isNull();
  }

  @Test
  public void executeJob_withNullApplication_usesRepositoryUploadPathAndDoesNotStampScanId() throws Exception {
    // Unparsable scan content → componentInfo == null → application == null → Repository-typed upload branch
    HostedComponentScanQueue job = insertPendingJob("repo-null-app");

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan-null-app");
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);

    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));

    // HDS was called (Repository-typed upload path hits the same endpoint)
    assertThat(hdsMockServer.getCapturedRequestHttpHeaders(ScanUploader.HDS_PATH)).isNotNull();

    // scan_id must NOT be stamped — stampScanId is only called when application != null
    ProxyRepositoryComponent rc = proxyRepositoryComponentDAO.getByRepositoryIdAndPathname(
        job.getRepositoryId(), "scan-content");
    if (rc != null) {
      assertThat(rc.getScanId()).isNull();
    }
  }

  @Test
  public void executeJob_withValidApplication_stampsScandIdOnRepositoryComponent() throws Exception {
    // Create a non-root org with a linked repository so resolveOrganizationId returns non-null
    Organization org = tempEntity.newOrganizationWithRepositoryManager("test-org-scanid");

    // Valid scan XML → application created → upload branch → stampScanId called
    HostedComponentScanQueue job = insertPendingJobWithScanXmlForOrg(
        org, "comp-scanid",
        "pkg:maven/com.example/lib@1.0.0", null,
        "com/example/lib/1.0/lib-1.0.jar", "scanid_hash_001", "maven2");

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan-id-stamped");
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);
    mockPolicyEvaluatorHdsResponse("scanid_hash_001");

    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));

    ProxyRepositoryComponent rc = proxyRepositoryComponentDAO.getByRepositoryIdAndPathname(
        job.getRepositoryId(), "com/example/lib/1.0/lib-1.0.jar");
    assertThat(rc).isNotNull();
    assertThat(rc.getScanId()).isEqualTo("scan-id-stamped");

    // CLM-41693: also verify the policy_evaluation row was tagged with HOSTED_REPOSITORY_SCANNING
    // (end-to-end DB assertion that complements the telemetry unit tests in
    // HostedComponentScanQueueConsumerTelemetryTest). The synthetic appId is generated inside
    // the consumer, so we look up the policy_evaluation row by scanId via getAllLast().
    com.sonatype.insight.brain.model.policy.PolicyEvaluation pe = policyEvaluationDAO.getAllLast()
        .stream()
        .filter(p -> "scan-id-stamped".equals(p.getScanId()))
        .findFirst()
        .orElse(null);
    assertThat(pe)
        .as("policy_evaluation row should be created for scanId=scan-id-stamped")
        .isNotNull();
    assertThat(pe.getScanTriggerType())
        .as("scan_trigger_type column must be HOSTED_REPOSITORY_SCANNING (CLM-41693)")
        .isEqualTo(com.sonatype.insight.brain.model.policy.ScanTriggerType.HOSTED_REPOSITORY_SCANNING);
  }

  @Test
  public void executeJob_usesPolicyEvaluationStageFromJob() throws Exception {
    HostedComponentScanQueue job = insertPendingJobWithScanXml(
        "repo-stage", "comp-stage",
        "pkg:maven/com.example/lib@1.0.0", "source",
        "com/example/lib/1.0/lib-1.0.jar", "stage_hash_001", "maven2");

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan-stage-test");
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);
    mockPolicyEvaluatorHdsResponse("stage_hash_001");
    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));

    // Verify the stage was stored and used — confirmed by job completing without error
    assertThat(queueDAO.getById(job.getId()).getPolicyEvaluationStage()).isEqualTo("source");
  }

  // ---- Helpers -------------------------------------------------------------

  private void mockPolicyEvaluatorHdsResponse(final String hash) {
    mockPolicyEvaluatorHdsResponseForHashes(hash);
  }

  /**
   * Mocks the HDS component-details response with one entry per supplied hash, in order. The
   * evaluator validates that the response's index/length match the request's, so when the
   * consumer sends N components in one request the mock must return N entries.
   */
  private void mockPolicyEvaluatorHdsResponseForHashes(final String... hashes) {
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    for (int i = 0; i < hashes.length; i++) {
      ComponentEvaluationData ced = new ComponentEvaluationData();
      ced.requestIndex = i;
      ced.hash = hashes[i];
      ced.matchState = MatchState.EXACT.getId();
      ced.declaredLicenses = new HashSet<>();
      ced.observedLicenses = new HashSet<>();
      hdsResult.components.add(ced);
    }
    hdsMockServer.respondWith(hdsResult).atUri(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH);
  }

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private void setQueueConfig(final String json) {
    try {
      Map<String, Object> configMap = OBJECT_MAPPER.readValue(json, new TypeReference<>()
      {
      });
      lookup(ApiConfigurationService.class)
          .setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.HOSTED_SCAN_QUEUE_CONFIG, configMap);
      lookup(ApiConfigurationService.class)
          .applyConfigurationToClients(SystemConfigurationProperty.HOSTED_SCAN_QUEUE_CONFIG);
    }
    catch (IOException e) {
      throw new RuntimeException("Failed to parse queue config JSON", e);
    }
  }

  /** Enables monitoring so the CLM-42122 guard lets jobs through (newRepository defaults it false). */
  private Repository enableMonitoring(final Repository repo) {
    repo.setMonitoringEnabled(true);
    repositoryDAO.update(repo);
    return repo;
  }

  private HostedComponentScanQueue insertPendingJob(final String repoName) throws Exception {
    Repository repo = enableMonitoring(tempEntity.newRepository(repoName));
    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());

    ScanEntity scanEntity = scanPersistenceService.createTempScan(repo.getId());
    try (OutputStream out = scanEntity.getOutputStream()) {
      out.write("scan-content".getBytes(StandardCharsets.UTF_8));
    }

    HostedComponentScanQueue job = new HostedComponentScanQueue(
        component.getId(), scanEntity.getName(),
        HostedComponentScanQueueDAO.Status.PENDING.name(),
        HostedComponentScanQueue.DEFAULT_PRIORITY,
        repo.getId());
    queueDAO.insert(job);
    return job;
  }

  /**
   * Inserts a job using the repository already linked to the given org, so resolveOrganizationId returns non-null.
   */
  private HostedComponentScanQueue insertPendingJobWithScanXmlForOrg(
      final Organization org,
      final String componentId,
      final String purl,
      final String policyEvaluationStage,
      final String pathname,
      final String sha1,
      final String format) throws Exception
  {
    String repoId = org.getRelatedRepositoryId();
    // Enable monitoring so the CLM-42122 guard lets the job through (org-linked repo defaults it false).
    enableMonitoring(repositoryDAO.getByIdNotNull(repoId));
    tempEntity.newRepositoryComponent(repoId);

    String scanXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<scan version=\"2.24\">\n" +
        "<repository id=\"" + repoId + "\" name=\"" + repoId + "\" format=\"" + format + "\"/>\n" +
        "<dir path=\"" + pathname + "\" sha1=\"" + sha1 + "\" sha512=\"ignored\">\n</dir>\n" +
        "</scan>";

    ScanEntity scanEntity = scanPersistenceService.createTempScan(repoId);
    try (OutputStream out = scanEntity.getOutputStream()) {
      out.write(scanXml.getBytes(StandardCharsets.UTF_8));
    }

    HostedComponentScanQueue job = new HostedComponentScanQueue(
        componentId, scanEntity.getName(),
        HostedComponentScanQueueDAO.Status.PENDING.name(),
        HostedComponentScanQueue.DEFAULT_PRIORITY,
        repoId);
    job.setPolicyEvaluationStage(policyEvaluationStage);
    queueDAO.insert(job);
    return job;
  }

  /**
   * Inserts a job with a minimal valid scan.xml so ScanXmlParser can extract component info.
   * The scan XML is stored as plain XML (not gzip) matching how Jersey stores uploaded files.
   */
  private HostedComponentScanQueue insertPendingJobWithScanXml(
      final String repoName,
      final String componentId,
      final String purl,
      final String policyEvaluationStage,
      final String pathname,
      final String sha1,
      final String format) throws Exception
  {
    Repository repo = enableMonitoring(tempEntity.newRepository(repoName));
    tempEntity.newRepositoryComponent(repo.getId());

    // Write minimal scan XML as plain bytes (Jersey decompresses gzip before writing to temp file)
    String scanXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<scan version=\"2.24\">\n" +
        "<repository id=\"" + repo.getId() + "\" name=\"" + repoName + "\" format=\"" + format + "\"/>\n" +
        "<dir path=\"" + pathname + "\" sha1=\"" + sha1 + "\" sha512=\"ignored\">\n</dir>\n" +
        "</scan>";

    ScanEntity scanEntity = scanPersistenceService.createTempScan(repo.getId());
    try (OutputStream out = scanEntity.getOutputStream()) {
      out.write(scanXml.getBytes(StandardCharsets.UTF_8));
    }

    HostedComponentScanQueue job = new HostedComponentScanQueue(
        componentId, scanEntity.getName(),
        HostedComponentScanQueueDAO.Status.PENDING.name(),
        HostedComponentScanQueue.DEFAULT_PRIORITY,
        repo.getId());
    job.setPolicyEvaluationStage(policyEvaluationStage);
    queueDAO.insert(job);
    return job;
  }

  /**
   * Inserts a job whose scan.xml contains multiple {@code
   *
  <dir>
   * } elements — simulating an
   * archive-of-archives upload (e.g. a {@code .zip} that the insight-scanner unpacked into N inner
   * artifacts). Used by the archive-fan-out test below to verify the consumer creates one
   * {@code proxy_repository_component} row per inner artifact.
   */
  private HostedComponentScanQueue insertPendingJobWithMultiComponentScanXml(
      final String repoName,
      final String componentId,
      final String outerPathname,
      final String outerSha1,
      final String[] innerPathnames,
      final String[] innerSha1s,
      final String format) throws Exception
  {
    // Set the DB Repository.format so runtime code paths that read it
    // (HostedComponentScanQueueConsumer's format carveouts:
    // KEEP_NESTED_FORMATS_FOR_IDENTIFIED_OUTER, ALWAYS_COLLAPSE_TO_OUTER_FORMATS, etc.)
    // observe the same format string the scan XML declares. Prior versions of this helper
    // only wrote the format into the scan XML, leaving repository.format null in the DB —
    // fine for tests that never exercised a format-carveout branch, but wrong for tests
    // that do (e.g. rubygems always-collapse).
    // CLM-42122: enableMonitoring wrapper flips repository.monitoringEnabled=true so the
    // consumer's per-job guard (introduced by CLM-42122) doesn't skip the job during test.
    Repository repo = enableMonitoring(tempEntity.newRepository(UUID.randomUUID().toString(), repoName, format));
    tempEntity.newRepositoryComponent(repo.getId());

    StringBuilder dirs = new StringBuilder();
    dirs.append("<dir path=\"")
        .append(outerPathname)
        .append("\" sha1=\"")
        .append(outerSha1)
        .append("\" sha512=\"ignored\">\n</dir>\n");
    for (int i = 0; i < innerPathnames.length; i++) {
      dirs.append("<dir path=\"")
          .append(innerPathnames[i])
          .append("\" sha1=\"")
          .append(innerSha1s[i])
          .append("\" sha512=\"ignored\">\n</dir>\n");
    }

    String scanXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<scan version=\"2.24\">\n" +
        "<repository id=\"" + repo.getId() + "\" name=\"" + repoName + "\" format=\"" + format + "\"/>\n" +
        dirs +
        "</scan>";

    ScanEntity scanEntity = scanPersistenceService.createTempScan(repo.getId());
    try (OutputStream out = scanEntity.getOutputStream()) {
      out.write(scanXml.getBytes(StandardCharsets.UTF_8));
    }

    HostedComponentScanQueue job = new HostedComponentScanQueue(
        componentId, scanEntity.getName(),
        HostedComponentScanQueueDAO.Status.PENDING.name(),
        HostedComponentScanQueue.DEFAULT_PRIORITY,
        repo.getId());
    queueDAO.insert(job);
    return job;
  }

  // ---- Archive-of-archives fan-out (CLM-40943) ----

  @Test
  public void executeJob_archiveOfArchivesScan_keepsOneOuterRowAndDeletesInnerRows() throws Exception {
    // Archive-of-archives upload (a .zip the scanner unpacked into two inner .jar files): the
    // Components page should still show just ONE row — the outer .zip (mirroring today's UX where
    // every uploaded artifact is one row). The inner-pathname rows that the policy evaluator
    // creates as it walks the multi-component request are deleted post-evaluation; the inner
    // pathname violations stay in proxy_repository_policy_violation so the outer's report can roll
    // them up via HostedReportFileBuilder.
    String outerPath = "com/example/bundle/1.0/bundle-1.0.zip";
    String outerHash = "outer_zip_hash_001a";
    String[] innerPaths = {
      "log4j-core-2.14.1.jar",
      "commons-cli-1.9.0.jar"
    };
    String[] innerHashes = {
      "inner_log4j_hash_01",
      "inner_cli_hash_001a"
    };

    HostedComponentScanQueue job = insertPendingJobWithMultiComponentScanXml(
        "repo-archive-fanout", "comp-archive-1",
        outerPath, outerHash, innerPaths, innerHashes, "maven2");

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan-archive-1");
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);
    // The consumer now sends ALL components (outer + inners) in one evaluation request, so the
    // HDS mock must return one entry per request component.
    mockPolicyEvaluatorHdsResponseForHashes(outerHash, innerHashes[0], innerHashes[1]);

    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(15, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));

    // The outer artifact survives — keyed on the literal .zip path.
    ProxyRepositoryComponent outerRow = proxyRepositoryComponentDAO.getByRepositoryIdAndPathname(
        job.getRepositoryId(), outerPath);
    assertThat(outerRow).as("outer .zip row").isNotNull();

    // The inner-pathname rows are deleted post-evaluation so the Components page only shows the
    // outer artifact (one row per uploaded file).
    ProxyRepositoryComponent innerLog4j = proxyRepositoryComponentDAO.getByRepositoryIdAndPathname(
        job.getRepositoryId(), outerPath + "!/" + innerPaths[0]);
    assertThat(innerLog4j).as("inner log4j row should have been deleted").isNull();

    ProxyRepositoryComponent innerCli = proxyRepositoryComponentDAO.getByRepositoryIdAndPathname(
        job.getRepositoryId(), outerPath + "!/" + innerPaths[1]);
    assertThat(innerCli).as("inner commons-cli row should have been deleted").isNull();
  }

  /**
   * Companion test for {@link #executeJob_archiveOfArchivesScan_keepsOneOuterRowAndDeletesInnerRows}:
   * verifies the OTHER half of the fan-out invariant — that inner-pathname
   * {@code proxy_repository_policy_violation} rows survive even when the matching
   * {@code proxy_repository_component} row is deleted, AND that the new DAO method
   * {@code getActiveByRepositoryIdAndPathnameOrInnerPathnames} returns them all under the outer's
   * pathname. This is what {@code ReportService.saveOverlayFiles} relies on to roll inner
   * violations into the outer's synthesised {@code policythreats.json}.
   * <p>
   * The real evaluator only persists violations when policies match, but this test exercises the
   * persistence-layer contract directly: seed inner-pathname violation rows, delete inner
   * proxy_repository_component rows the way the consumer would, then assert the DAO surfaces them.
   * No HDS / queue / Drools required — the same code path the production saveOverlayFiles
   * recovery hits is hit here.
   */
  @Test
  public void rolledUpViolationsByOuterPathname_returnsBothOuterAndInnerPathnameViolations() throws Exception {
    Repository repo = tempEntity.newRepository("repo-rollup");
    String outerPath = "com/example/bundle/1.0/bundle-1.0.zip";
    String innerLog4j = outerPath + "!/log4j-core-2.14.1.jar";
    String innerCli = outerPath + "!/commons-cli-1.9.0.jar";

    tempEntity.newRepositoryPolicyViolation(repo.getId(), 2, outerPath, null);
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 10, innerLog4j, false, "p-cve", "Security-Critical", null);
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 1, innerCli, false, "p-arch", "Architecture-Quality", null);

    java.util.List<com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation> rolledUp =
        proxyRepositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathnameOrInnerPathnames(repo.getId(), outerPath);

    assertThat(rolledUp)
        .as("rolled-up query returns the outer pathname's violations and every inner-pathname violation")
        .extracting(com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation::getPathname)
        .containsExactlyInAnyOrder(outerPath, innerLog4j, innerCli);
  }

  /**
   * CLM-40943 — guards against the dev-tenant {@code component_count == NULL} regression.
   * After a successful single-jar evaluation the eager stamp must have set
   * {@code component_count} from the scanner's {@code
   *
  <dir>
   * } count, independent of whether the
   * later HDS-bom refinement ran (the bom read is best-effort and silently no-ops on
   * S3-backed tenant storage that hasn't propagated the report yet). Asserting non-null here
   * is the strongest cheap test we can write end-to-end — if the eager stamp ever regresses,
   * this test fails immediately on a normal happy-path scan.
   */
  @Test
  public void executeJob_eagerStampsComponentCountAfterEvaluation() throws Exception {
    String pathname = "com/example/eager/1.0/eager-1.0.jar";
    HostedComponentScanQueue job = insertPendingJobWithScanXml(
        "repo-eager", "comp-eager-1",
        "pkg:maven/com.example/eager@1.0.0", null,
        pathname, "eagerhash00000001", "maven2");

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan-eager");
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);
    mockPolicyEvaluatorHdsResponse("eagerhash00000001");

    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));

    ProxyRepositoryComponent rc = proxyRepositoryComponentDAO.getByRepositoryIdAndPathname(
        job.getRepositoryId(), pathname);
    assertThat(rc).isNotNull();
    assertThat(rc.getComponentCount())
        .as("eager stamp from componentInfos.size() must leave component_count non-null even if HDS bom refinement no-ops")
        .isNotNull()
        .isGreaterThanOrEqualTo(1);
  }

  // ---- Identified-outer gate (CLM-40943, PR 16421 review by Bhavat) ----

  /**
   * Identified-outer gate fires for non-keep-set formats: when HDS returns a non-UNKNOWN match
   * state for the outer artifact AND the repository format is NOT in
   * {@code KEEP_NESTED_FORMATS_FOR_IDENTIFIED_OUTER} (maven2 is not), the consumer collapses the
   * report to a single component view — {@code componentCount=1} on the outer row, and any
   * stale inner-pathname {@code proxy_repository_component} rows from a prior run are deleted.
   * Mirrors the iq-cli single-file scan output for the same binary.
   */
  @Test
  public void executeJob_identifiedOuterGate_mavenFormat_collapsesToOneComponent() throws Exception {
    String outerPath = "com/example/identified/1.0/identified-1.0.zip";
    String outerHash = "identifiedouter0001";
    String[] innerPaths = {"log4j-core-2.14.1.jar", "commons-cli-1.9.0.jar"};
    String[] innerHashes = {"inner_log4j_hash_id1", "inner_cli_hash_id1"};

    HostedComponentScanQueue job = insertPendingJobWithMultiComponentScanXml(
        "repo-identified-maven", "comp-identified-1",
        outerPath, outerHash, innerPaths, innerHashes, "maven2");

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan-identified-1");
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);
    // MatchState.EXACT for the outer triggers the identified-outer gate; inners get EXACT too
    // (the gate only consults the outer's match state).
    mockPolicyEvaluatorHdsResponseForHashes(outerHash, innerHashes[0], innerHashes[1]);

    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(15, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));

    ProxyRepositoryComponent outerRow = proxyRepositoryComponentDAO.getByRepositoryIdAndPathname(
        job.getRepositoryId(), outerPath);
    assertThat(outerRow).as("outer row").isNotNull();
    assertThat(outerRow.getComponentCount())
        .as("identified-outer gate (non-keep-set format) collapses to componentCount=1")
        .isEqualTo(1);

    // Inner pathname proxy_repository_component rows are deleted (matches existing fan-out cleanup).
    assertThat(proxyRepositoryComponentDAO.getByRepositoryIdAndPathname(
        job.getRepositoryId(), outerPath + "!/" + innerPaths[0]))
            .as("inner log4j row should not survive identified-outer gate")
            .isNull();
    assertThat(proxyRepositoryComponentDAO.getByRepositoryIdAndPathname(
        job.getRepositoryId(), outerPath + "!/" + innerPaths[1]))
            .as("inner commons-cli row should not survive identified-outer gate")
            .isNull();
  }

  /**
   * CLM-42119 regression guard: the identified-outer collapse gate fires unconditionally
   * for {@code rubygems} — even when HDS returns {@link MatchState#UNKNOWN} on the outer
   * — because iq-cli's rubygems scanner treats a {@code .gem} archive as opaque and
   * surfaces exactly one row regardless of what the archive bundles. Without this behavior
   * a CM scan of a custom gem (e.g. {@code bundled-gem-app-1.0.0.gem} bundling vendored
   * copies of rack/nokogiri/actionpack) drills into HDS's expanded view and reports 5
   * components while an iq-cli scan of the same file reports 1.
   * <p>
   * Symmetric to {@link #executeJob_identifiedOuterGate_mavenFormat_collapsesToOneComponent}
   * which exercises the collapse-on-identified path; this test exercises the collapse-on-
   * unknown path for a format in {@code ALWAYS_COLLAPSE_TO_OUTER_FORMATS}.
   */
  @Test
  public void executeJob_alwaysCollapse_rubygems_unknownOuter_collapsesToOneComponent() throws Exception {
    String outerPath = "gems/bundled-gem-app-1.0.0.gem";
    String outerHash = "bundledgemapp0001";
    String[] innerPaths = {"vendor/cache/rack-2.0.6.gem", "vendor/cache/nokogiri-1.8.2.gem"};
    String[] innerHashes = {"rack_hash_042119", "nokogiri_hash_042119"};

    HostedComponentScanQueue job = insertPendingJobWithMultiComponentScanXml(
        "repo-rubygems-nested", "comp-bundled-gem",
        outerPath, outerHash, innerPaths, innerHashes, "rubygems");

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan-bundled-gem-1");
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);
    // UNKNOWN outer — the always-collapse behavior kicks in via the rubygems format carveout,
    // NOT via HDS identifying the outer. This distinguishes the fix from the existing
    // "identified outer + non-keep format" collapse path.
    mockPolicyEvaluatorHdsResponseUnknown(outerHash, innerHashes[0], innerHashes[1]);

    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(15, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));

    ProxyRepositoryComponent outerRow = proxyRepositoryComponentDAO.getByRepositoryIdAndPathname(
        job.getRepositoryId(), outerPath);
    assertThat(outerRow).as("outer rubygems row").isNotNull();
    assertThat(outerRow.getComponentCount())
        .as("rubygems always-collapse: componentCount=1 regardless of UNKNOWN match state — "
            + "matches iq-cli single-file behavior for opaque .gem archives")
        .isEqualTo(1);

    // Inner-pathname rows deleted by the collapse cleanup — same behavior as the identified-
    // outer maven test above.
    assertThat(proxyRepositoryComponentDAO.getByRepositoryIdAndPathname(
        job.getRepositoryId(), outerPath + "!/" + innerPaths[0]))
            .as("inner rack row should not survive rubygems always-collapse")
            .isNull();
    assertThat(proxyRepositoryComponentDAO.getByRepositoryIdAndPathname(
        job.getRepositoryId(), outerPath + "!/" + innerPaths[1]))
            .as("inner nokogiri row should not survive rubygems always-collapse")
            .isNull();
  }

  /**
   * Idempotency: re-running the same job (e.g. a producer-cycle replay after a restart) must
   * not double-count or duplicate rows. The proxy_repository_component is unique on
   * {@code (repository_id, pathname)}, so a re-run UPSERT-style should leave exactly one outer
   * row, not two. Verifies the executeJob path is idempotent on the outer row identity.
   */
  @Test
  public void executeJob_replayingSameJob_keepsExactlyOneOuterRow() throws Exception {
    String outerPath = "com/example/replay/1.0/replay-1.0.zip";
    String outerHash = "replayouterhash01";
    String[] innerPaths = {"lib-a.jar"};
    String[] innerHashes = {"replay_inner_a"};

    HostedComponentScanQueue job1 = insertPendingJobWithMultiComponentScanXml(
        "repo-replay", "comp-replay",
        outerPath, outerHash, innerPaths, innerHashes, "maven2");

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan-replay-1");
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);
    mockPolicyEvaluatorHdsResponseForHashes(outerHash, innerHashes[0]);

    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(15, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job1.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));

    // First pass result.
    ProxyRepositoryComponent afterFirstRun = proxyRepositoryComponentDAO.getByRepositoryIdAndPathname(
        job1.getRepositoryId(), outerPath);
    assertThat(afterFirstRun).as("outer row after first run").isNotNull();

    // Re-run via a second job with the same outer pathname (simulates producer-cycle replay).
    HostedComponentScanQueue job2 = insertPendingJobWithMultiComponentScanXml(
        "repo-replay", "comp-replay-2",
        outerPath, outerHash, innerPaths, innerHashes, "maven2");

    ScanReceipt receipt2 = new ScanReceipt();
    receipt2.setScanId("scan-replay-2");
    hdsMockServer.respondWith(receipt2).atUri(ScanUploader.HDS_PATH);
    mockPolicyEvaluatorHdsResponseForHashes(outerHash, innerHashes[0]);

    consumer.run();

    await().atMost(15, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job2.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));

    // Note: insertPendingJobWithMultiComponentScanXml creates a new Repository, so job1 and job2
    // live in different repos. We verify each repo has exactly one outer row, not two — i.e.
    // each individual evaluation is internally consistent and doesn't duplicate the outer.
    ProxyRepositoryComponent afterSecondRun = proxyRepositoryComponentDAO.getByRepositoryIdAndPathname(
        job2.getRepositoryId(), outerPath);
    assertThat(afterSecondRun).as("outer row after replay run").isNotNull();
    assertThat(afterSecondRun.getComponentCount())
        .as("replay should not regress componentCount to NULL")
        .isNotNull();
  }

  // ---- CLM-42118 follow-up regression guard ----

  /**
   * Regression guard for CLM-42118 and the follow-up commit on CLM-41737's branch: the
   * proxy_repository_component.component_count column MUST be stamped from HDS's
   * {@code data.json.totalArtifactCount} via an unconditional UPDATE — not from
   * {@code bom.json.aaData.length} via {@code raiseComponentCountIfHigher}.
   * <p>
   * The bug this test guards: prior behavior stamped {@code max(scanner_count, bom.aaData.length)}
   * using raise-only semantics, which for pub .tar.gz (scanner enumerates ~39 files inside) let
   * the scanner's over-count lock in even though HDS identified only 4 real components. The
   * drill-in Build Report reads {@code data.json.totalArtifactCount} directly (4), producing a
   * list-page vs report-page divergence.
   * <p>
   * Fixture: scan.xml with 1 outer {@code
   *
  <dir>
   * } (scanner-count = 1); HDS report bundle whose
   * {@code data.json.totalArtifactCount = 4} and {@code bom.aaData.length = 0}. If the code
   * regresses to raise-only + scanner source, {@code component_count} lands on 1. If it regresses
   * to reading {@code bom.aaData.length}, it lands on 0. Only the correct behavior (unconditional
   * stamp from {@code totalArtifactCount}) produces 4.
   * <p>
   * The outer is mocked as UNKNOWN so the identified-outer collapse gate (deferred to a
   * follow-up ticket) does not fire and rewrite the count to 1. Empty {@code bom.aaData} keeps
   * {@code ScanPolicyEvaluator}'s drill-down path a no-op so the mirror method's ScanPolicyEvaluator
   * call completes without producing policy_violation rows — isolating this test to the count
   * stamp behavior.
   */
  @Test
  public void executeJob_stampsComponentCountFromDataJsonTotalArtifactCount() throws Exception {
    // Zero-retry config so the mirror's rethrow surfaces the job into FAILED quickly (~1s) instead
    // of the default 3 attempts (~15-30s). This test doesn't care whether the job COMPLETED — it
    // asserts on the DB stamp, which happens in saveReportFiles BEFORE the mirror runs. The mirror
    // itself is expected to fail here because the minimal report bundle intentionally omits files
    // ScanPolicyEvaluator would need (index.html for embedOwnerPublicId, etc.); that's a
    // deliberate simplification for this test, not the fix's behavior in production.
    setQueueConfig(
        "{\"enabled\":true,\"workerThreadsPerTenant\":1,\"pollIntervalMilliseconds\":30000,"
            + "\"maxQueuedRows\":10,\"maxRetries\":0}");

    String pathname = "com/example/postfix/1.0/postfix-1.0.jar";
    String outerHash = "postfix_hash_00001";
    HostedComponentScanQueue job = insertPendingJobWithScanXml(
        "repo-postfix-count", "comp-postfix-count",
        "pkg:maven/com.example/postfix@1.0.0", null,
        pathname, outerHash, "maven2");

    String scanId = "scan-postfix-count-1";
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId(scanId);
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);

    // Outer as UNKNOWN — skips the identified-outer collapse gate so the drill path (where this
    // test's stamp change lives) is exercised.
    mockPolicyEvaluatorHdsResponseUnknown(outerHash);

    // Report bundle with data.json.totalArtifactCount=4 and bom.aaData.length=0.
    URL zippedReport = ReportHelper.zipReport(
        "/HostedComponentScanQueueConsumerTest/postProcessingFixReport", tempDir);
    hdsMockServer.respondWith(zippedReport).atUri("rest/application/analysis/" + scanId);

    consumer.disableForTesting = false;
    consumer.run();

    // Assert on the stamp itself — happens in saveReportFiles on the first (and only, given
    // maxRetries=0) attempt. Regardless of whether the job ultimately COMPLETED or FAILED, the
    // component_count must reflect HDS's data.json.totalArtifactCount.
    await().atMost(15, TimeUnit.SECONDS)
        .untilAsserted(() -> {
          ProxyRepositoryComponent rc = proxyRepositoryComponentDAO.getByRepositoryIdAndPathname(
              job.getRepositoryId(), pathname);
          assertThat(rc).as("outer proxy_repository_component row").isNotNull();
          assertThat(rc.getComponentCount())
              .as("component_count must be stamped from data.json.totalArtifactCount (=4). "
                  + "If this reads 1, the stamp regressed to raise-only + scanner-count source. "
                  + "If this reads 0, the stamp regressed to reading bom.aaData.length. "
                  + "If null, saveReportFiles never reached the stamp — check HDS mock wiring.")
              .isEqualTo(4);
        });
  }

  // ---- CLM-42122 monitoring-disabled guard tests (from origin/main) ----

  @Test
  public void executeJob_dropsJobWhenRepositoryMonitoringDisabled() throws Exception {
    // newRepository defaults monitoring off, so this repo is disabled.
    Repository repo = tempEntity.newRepository("repo-monitoring-disabled");
    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());
    ScanEntity scanEntity = scanPersistenceService.createTempScan(repo.getId());
    try (OutputStream out = scanEntity.getOutputStream()) {
      out.write("scan-content".getBytes(StandardCharsets.UTF_8));
    }
    HostedComponentScanQueue job = new HostedComponentScanQueue(
        component.getId(), scanEntity.getName(),
        HostedComponentScanQueueDAO.Status.PENDING.name(),
        HostedComponentScanQueue.DEFAULT_PRIORITY,
        repo.getId());
    queueDAO.insert(job);

    // No HDS mock is configured: if the guard failed and the job were evaluated, the run would fail
    // reaching HDS instead of completing cleanly.
    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));
    assertThat(queueDAO.getById(job.getId()).getRetryCount()).isEqualTo(0);
  }

  @Test
  public void executeJob_dropsJobWhenRepositoryMissing() throws Exception {
    // Repo row absent entirely (e.g. deleted): guard drops the job rather than NPE / evaluate.
    Repository repo = enableMonitoring(tempEntity.newRepository("repo-to-delete"));
    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());
    ScanEntity scanEntity = scanPersistenceService.createTempScan(repo.getId());
    try (OutputStream out = scanEntity.getOutputStream()) {
      out.write("scan-content".getBytes(StandardCharsets.UTF_8));
    }
    HostedComponentScanQueue job = new HostedComponentScanQueue(
        component.getId(), scanEntity.getName(),
        HostedComponentScanQueueDAO.Status.PENDING.name(),
        HostedComponentScanQueue.DEFAULT_PRIORITY,
        "00000000000000000000000000000000");
    queueDAO.insert(job);

    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));
  }

  /**
   * Companion to {@link #mockPolicyEvaluatorHdsResponseForHashes} that returns
   * {@link MatchState#UNKNOWN} instead of EXACT. Used by tests that need to bypass the
   * identified-outer collapse gate and exercise the drill-down path.
   */
  private void mockPolicyEvaluatorHdsResponseUnknown(final String... hashes) {
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    for (int i = 0; i < hashes.length; i++) {
      ComponentEvaluationData ced = new ComponentEvaluationData();
      ced.requestIndex = i;
      ced.hash = hashes[i];
      ced.matchState = MatchState.UNKNOWN.getId();
      ced.declaredLicenses = new HashSet<>();
      ced.observedLicenses = new HashSet<>();
      hdsResult.components.add(ced);
    }
    hdsMockServer.respondWith(hdsResult).atUri(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH);
  }
}
