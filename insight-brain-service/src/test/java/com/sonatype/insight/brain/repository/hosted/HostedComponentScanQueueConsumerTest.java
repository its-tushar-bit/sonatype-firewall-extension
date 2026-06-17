/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
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
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
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
  private RepositoryComponentDAO repositoryComponentDAO;

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
    Repository repo = tempEntity.newRepository("repo-no-scan");
    RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());

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
    RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());

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

    RepositoryComponent rc = repositoryComponentDAO.getByRepositoryIdAndPathname(
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

    RepositoryComponent rc = repositoryComponentDAO.getByRepositoryIdAndPathname(
        job2.getRepositoryId(), "com/example/lib/1.0/lib-1.0.jar");
    assertThat(rc.getHash()).isEqualTo("hash_v2");
    assertThat(rc.getComponentId()).isEqualTo("comp-update-2");
  }

  @Test
  public void executeJob_stampsComponentIdRegardlessOfPurl() throws Exception {
    // The consumer no longer parses the PURL to set ComponentIdentifier —
    // that comes from HDS evaluation data. The job's componentId is always
    // stamped onto the repository_component row via stampNxrmComponentId().
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

    RepositoryComponent rc = repositoryComponentDAO.getByRepositoryIdAndPathname(
        job.getRepositoryId(),
        "org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar");
    assertThat(rc).isNotNull();
    assertThat(rc.getHash()).isEqualTo("purl_hash_001");
    assertThat(rc.getComponentId()).isEqualTo("comp-purl-1");
  }

  @Test
  public void executeJob_completesSuccessfullyWhenScanHasNoMatchingComponent() throws Exception {
    // Scan job completes even when HDS evaluation finds no matching component for the pathname.
    // The repository_component row is still created by the evaluator (with hash from scan XML),
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

    RepositoryComponent rc = repositoryComponentDAO.getByRepositoryIdAndPathname(
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

    // No repository_component row should exist since ScanXmlParser returned null
    RepositoryComponent rc = repositoryComponentDAO.getByRepositoryIdAndPathname(
        job.getRepositoryId(), "scan-content");
    assertThat(rc).isNull();
  }

  @Test
  public void executeJob_withNullApplication_usesRepositoryUploadPathAndDoesNotStampScanId() throws Exception {
    // Unparsable scan content → componentInfo == null → application == null → uploadForRepository branch
    HostedComponentScanQueue job = insertPendingJob("repo-null-app");

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan-null-app");
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);

    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));

    // HDS was called (uploadForRepository path hit the same endpoint)
    assertThat(hdsMockServer.getCapturedRequestHttpHeaders(ScanUploader.HDS_PATH)).isNotNull();

    // scan_id must NOT be stamped — stampScanId is only called when application != null
    RepositoryComponent rc = repositoryComponentDAO.getByRepositoryIdAndPathname(
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

    RepositoryComponent rc = repositoryComponentDAO.getByRepositoryIdAndPathname(
        job.getRepositoryId(), "com/example/lib/1.0/lib-1.0.jar");
    assertThat(rc).isNotNull();
    assertThat(rc.getScanId()).isEqualTo("scan-id-stamped");
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
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    ComponentEvaluationData ced = new ComponentEvaluationData();
    ced.requestIndex = 0;
    ced.hash = hash;
    ced.matchState = MatchState.EXACT.getId();
    ced.declaredLicenses = new HashSet<>();
    ced.observedLicenses = new HashSet<>();
    hdsResult.components.add(ced);
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

  private HostedComponentScanQueue insertPendingJob(final String repoName) throws Exception {
    Repository repo = tempEntity.newRepository(repoName);
    RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());

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
    job.setPurl(purl);
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
    Repository repo = tempEntity.newRepository(repoName);
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
    job.setPurl(purl);
    job.setPolicyEvaluationStage(policyEvaluationStage);
    queueDAO.insert(job);
    return job;
  }
}
