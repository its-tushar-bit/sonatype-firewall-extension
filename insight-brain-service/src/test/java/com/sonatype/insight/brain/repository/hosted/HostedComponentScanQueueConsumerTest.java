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
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.mock.hds.HttpResponseProcessor;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.dataaccess.repository.HostedComponentScanQueueDAO;
import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.HostedComponentScanQueue;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.queue.AbstractPollDispatchQueueConsumer;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.HdsMockServerRule;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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

  @Inject
  private HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  @Inject
  private com.sonatype.insight.brain.dataaccess.ApplicationDAO applicationDAO;

  @Inject
  private com.sonatype.insight.brain.report.ReportService reportService;

  @Inject
  private com.sonatype.insight.brain.report.ReportDataStore reportDataStore;

  @Inject
  private com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO policyViolationDAO;

  @Inject
  private com.sonatype.insight.brain.continuousmonitoring.RepositoryContinuousMonitoringFlowProcessor continuousMonitoringFlowProcessor;

  @Inject
  private com.sonatype.insight.brain.report.HostedRepositoryComponentReportResource hostedRepositoryComponentReportResource;

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
  public void executeJob_updatesExistingRepositoryComponentOnResubmit() throws Exception {
    String pathname = "com/example/lib/1.0/lib-1.0.jar";

    // First upload — creates the hosted_repository_component row
    HostedComponentScanQueue job1 = insertPendingJobWithScanXml(
        "repo-update", "comp-update-1", null, null,
        pathname, "hash_v1", "maven2");
    String scanId1 = "scan-v1";
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId(scanId1);
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);
    URL zippedReport1 = ReportHelper.zipReport("/ScanServiceTest/report", tempDir);
    hdsMockServer.respondWith(zippedReport1).atUri("rest/application/analysis/" + scanId1);
    consumer.disableForTesting = false;
    consumer.run();
    await().atMost(15, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job1.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));
    consumer.disableForTesting = true;
    hdsMockServer.reset();

    // Second upload — same repository × pathname, different hash and componentId
    HostedComponentScanQueue job2 = insertPendingJobWithScanXml(
        "repo-update", "comp-update-2", null, null,
        pathname, "hash_v2", "maven2");
    String repositoryId = job2.getRepositoryId();
    String scanId2 = "scan-v2";
    receipt.setScanId(scanId2);
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);
    URL zippedReport2 = ReportHelper.zipReport("/ScanServiceTest/report", tempDir);
    hdsMockServer.respondWith(zippedReport2).atUri("rest/application/analysis/" + scanId2);
    consumer.disableForTesting = false;
    consumer.run();
    await().atMost(15, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job2.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));

    HostedRepositoryComponent hrc = getHrc(repositoryId, pathname);
    assertThat(hrc)
        .as("hosted_repository_component row for %s should be updated in place on resubmit", pathname)
        .isNotNull();
    assertThat(hrc.getHash()).isEqualTo("hash_v2");
    assertThat(hrc.getComponentId()).isEqualTo("comp-update-2");
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
  public void executeJob_withOrgLinkedRepository_createsHrcAndPolicyEvaluation() throws Exception {
    // Org-linked repository (has a related Organization) — exercises the same resolver/SPE
    // pipeline as a repository with no org linkage.
    Organization org = tempEntity.newOrganizationWithRepositoryManager("test-org-scanid");
    String pathname = "com/example/lib/1.0/lib-1.0.jar";

    HostedComponentScanQueue job = insertPendingJobWithScanXmlForOrg(
        org, "comp-scanid",
        "pkg:maven/com.example/lib@1.0.0", null,
        pathname, "scanid_hash_001", "maven2");

    String scanId = "scan-id-stamped";
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId(scanId);
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);
    URL zippedReport = ReportHelper.zipReport("/ScanServiceTest/report", tempDir);
    hdsMockServer.respondWith(zippedReport).atUri("rest/application/analysis/" + scanId);

    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(15, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));

    HostedRepositoryComponent hrc = getHrc(job.getRepositoryId(), pathname);
    assertThat(hrc).as("hosted_repository_component row for the org-linked repository upload").isNotNull();
    assertThat(hrc.getComponentId()).isEqualTo("comp-scanid");

    // CLM-41693: also verify the policy_evaluation row was tagged with HOSTED_REPOSITORY_SCANNING
    // (end-to-end DB assertion that complements the telemetry unit tests in
    // HostedComponentScanQueueConsumerTelemetryTest).
    com.sonatype.insight.brain.model.policy.PolicyEvaluation pe = policyEvaluationDAO.getAllLast()
        .stream()
        .filter(p -> hrc.getId().equals(p.getOwnerId()))
        .findFirst()
        .orElse(null);
    assertThat(pe)
        .as("policy_evaluation row keyed on the HRC's owner id")
        .isNotNull();
    assertThat(pe.getScanId()).isEqualTo(scanId);
    assertThat(pe.getScanTriggerType())
        .as("scan_trigger_type column must be HOSTED_REPOSITORY_SCANNING (CLM-41693)")
        .isEqualTo(com.sonatype.insight.brain.model.policy.ScanTriggerType.HOSTED_REPOSITORY_SCANNING);
  }

  @Test
  public void executeJob_usesPolicyEvaluationStageFromJob() throws Exception {
    String pathname = "com/example/lib/1.0/lib-1.0.jar";
    HostedComponentScanQueue job = insertPendingJobWithScanXml(
        "repo-stage", "comp-stage",
        "pkg:maven/com.example/lib@1.0.0", "source",
        pathname, "stage_hash_001", "maven2");

    String scanId = "scan-stage-test";
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId(scanId);
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);
    URL zippedReport = ReportHelper.zipReport("/ScanServiceTest/report", tempDir);
    hdsMockServer.respondWith(zippedReport).atUri("rest/application/analysis/" + scanId);
    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(15, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));

    // The job's policyEvaluationStage ("source") must be the stage actually passed to
    // ScanPolicyEvaluator.evaluate — confirmed via the persisted policy_evaluation row.
    com.sonatype.insight.brain.model.policy.PolicyEvaluation pe = policyEvaluationDAO.getAllLast()
        .stream()
        .filter(p -> scanId.equals(p.getScanId()))
        .findFirst()
        .orElse(null);
    assertThat(pe).as("policy_evaluation row for scanId=%s", scanId).isNotNull();
    assertThat(pe.getStageTypeId())
        .as("stage from the job's policyEvaluationStage must reach ScanPolicyEvaluator unchanged")
        .isEqualTo("source");
  }

  // ---- HostedRepositoryComponentResolver / ScanPolicyEvaluator pipeline (CLM-43710) ----

  @Test
  public void executeJob_happyPath_createsHrcAndPolicyEvaluationKeyedOnIt() throws Exception {
    String pathname = "tomcat-util-5.5.23.jar";
    String hash = "1249e25aebb15358bedd";
    HostedComponentScanQueue job = insertPendingJobWithScanXml(
        "repo-hrc-happy", "comp-hrc-happy",
        "pkg:maven/tomcat/tomcat-util@5.5.23", null,
        pathname, hash, "maven2");

    String scanId = "scan-hrc-happy-1";
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId(scanId);
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);

    URL zippedReport = ReportHelper.zipReport("/ScanServiceTest/report", tempDir);
    hdsMockServer.respondWith(zippedReport).atUri("rest/application/analysis/" + scanId);

    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(15, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));
    assertThat(queueDAO.getById(job.getId()).getRetryCount()).isEqualTo(0);

    HostedRepositoryComponent hrc = getHrc(job.getRepositoryId(), pathname);
    assertThat(hrc).as("hosted_repository_component row for the uploaded artifact").isNotNull();
    assertThat(hrc.getHash()).isEqualTo(hash);
    assertThat(hrc.getComponentId()).isEqualTo("comp-hrc-happy");

    com.sonatype.insight.brain.model.policy.PolicyEvaluation pe = policyEvaluationDAO.getAllLast()
        .stream()
        .filter(p -> hrc.getId().equals(p.getOwnerId()))
        .findFirst()
        .orElse(null);
    assertThat(pe)
        .as("policy_evaluation row keyed on the HRC's owner id")
        .isNotNull();
    assertThat(pe.getScanId()).isEqualTo(scanId);
    assertThat(pe.getScanTriggerType())
        .as("scan_trigger_type column must be HOSTED_REPOSITORY_SCANNING (CLM-41693)")
        .isEqualTo(com.sonatype.insight.brain.model.policy.ScanTriggerType.HOSTED_REPOSITORY_SCANNING);

    // resolver.pinOwnerComponent ran after evaluation and found a match for the outer artifact.
    HostedRepositoryComponent pinned = getHrc(job.getRepositoryId(), pathname);
    assertThat(pinned.getOwnerComponentId())
        .as("owner_component_id should be pinned once ScanPolicyEvaluator inserts the matching row")
        .isNotNull();
  }

  @Test
  public void executeJob_emptyComponentInfos_uploadsViaRepositoryAndSkipsResolverAndEvaluator() throws Exception {
    // Unparsable scan content -> componentInfos is empty -> upload via repository Owner and bail
    // before resolver.getOrCreate / ScanPolicyEvaluator are ever invoked.
    HostedComponentScanQueue job = insertPendingJob("repo-empty-infos");

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan-empty-infos");
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);

    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));
    assertThat(queueDAO.getById(job.getId()).getRetryCount()).isEqualTo(0);

    // Upload still happened via the repository pipeline (HDS audit trail preserved).
    assertThat(hdsMockServer.getCapturedRequestHttpHeaders(ScanUploader.HDS_PATH)).isNotNull();

    // No HRC row was created — resolver.getOrCreate was never reached on this bail path.
    assertThat(getHrc(job.getRepositoryId(), "scan-content"))
        .as("no hosted_repository_component row should exist when componentInfos is empty")
        .isNull();

    // No policy_evaluation row was produced either — ScanPolicyEvaluator was never invoked.
    boolean anyEvaluationForScan = policyEvaluationDAO.getAllLast()
        .stream()
        .anyMatch(p -> "scan-empty-infos".equals(p.getScanId()));
    assertThat(anyEvaluationForScan)
        .as("no policy_evaluation row should exist for the empty-componentInfos bail path")
        .isFalse();
  }

  @Test
  public void executeJob_normalizesUnderscoreStageToHyphen() throws Exception {
    // CLM-42079: normalizeStage canonicalizes NXRM's "STAGE_RELEASE" to "stage-release" before it
    // is passed to ScanUploader/ScanPolicyEvaluator.
    HostedComponentScanQueue job = insertPendingJobWithScanXml(
        "repo-stage-normalize", "comp-stage-normalize",
        "pkg:maven/com.example/lib@1.0.0", "STAGE_RELEASE",
        "com/example/lib/1.0/lib-1.0.jar", "stage_norm_hash_001", "maven2");

    String scanId = "scan-stage-normalize-1";
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId(scanId);
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);

    URL zippedReport = ReportHelper.zipReport("/ScanServiceTest/report", tempDir);
    hdsMockServer.respondWith(zippedReport).atUri("rest/application/analysis/" + scanId);

    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(15, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));

    com.sonatype.insight.brain.model.policy.PolicyEvaluation pe = policyEvaluationDAO.getAllLast()
        .stream()
        .filter(p -> scanId.equals(p.getScanId()))
        .findFirst()
        .orElse(null);
    assertThat(pe).as("policy_evaluation row for the normalized-stage scan").isNotNull();
    assertThat(pe.getStageTypeId())
        .as("normalizeStage must canonicalize STAGE_RELEASE to stage-release before evaluation")
        .isEqualTo("stage-release");
  }

  // ---- Format-carveout collapse gate, driven via the Continuous Monitoring refresh path ----
  //
  // The KEEP_NESTED_FORMATS_FOR_IDENTIFIED_OUTER / ALWAYS_COLLAPSE_TO_OUTER_FORMATS gate lives
  // inside mirrorNestedComponentViolationsFromApplicationEvaluation, which is reached only from
  // ReportService.refreshHostedComponentAfterEvaluation (Continuous Monitoring, via
  // RepositoryContinuousMonitoringFlowProcessor). These two tests drive the gate through
  // refreshHostedComponentAfterEvaluation directly, seeding the outer proxy_repository_component
  // row with the matchStateId the gate reads, since that row is populated by an earlier
  // evaluation cycle rather than by refreshHostedComponentAfterEvaluation itself.
  // refreshHostedComponentAfterEvaluation's saveOverlayFiles step reads bom.json from a
  // report.zip that must already be cached on local disk for (application, scanId) — in
  // production this is populated by the original evaluation cycle that produced the outer row,
  // so the tests pre-fetch it the same way via reportDataStore.downloadReport beforehand.

  @Test
  public void refreshHostedComponentAfterEvaluation_identifiedOuterGate_mavenFormat_collapsesToOneComponent() throws Exception {
    // CLM-40943: when the outer artifact was identified (matchStateId != UNKNOWN) by an earlier
    // evaluation and the repository's format is not in the keep-nested set, the gate collapses
    // the component to one row and cleans up stale inner-pathname violations.
    String outerPathname = "outer-maven.jar";
    String innerPathname = outerPathname + "!/inner-maven.jar";
    String scanId = "scan-identified-outer-gate";
    String stageTypeId = "build";

    Application application = tempEntity.newApplication(tempEntity.newOrganization().getId());
    Repository repository = enableMonitoring(
        tempEntity.newRepository(UUID.randomUUID().toString(), "repo-identified-outer-gate", "maven2"));
    ProxyRepositoryComponent outerComponent = seedOuterComponent(
        repository, outerPathname, MatchState.EXACT, "outer-hash-ident1",
        ComponentIdentifier.createMavenCoordinates("g", "a", "1.0"), scanId, 2);
    seedInnerViolation(repository, application, innerPathname, "inner-hash-ident1",
        ComponentIdentifier.createMavenCoordinates("g", "b", "1.0"));

    mockPolicyEvaluatorHdsResponseForHashes(outerComponent.getHash());
    URL zippedReport = ReportHelper.zipReport("/ScanServiceTest/report", tempDir);
    hdsMockServer.respondWith(zippedReport).atUri("rest/application/analysis/" + scanId);
    reportDataStore.downloadReport(application, scanId, (sid, r, aid) -> {
    });

    reportService.refreshHostedComponentAfterEvaluation(
        outerComponent, repository, application, application.getId(), scanId, stageTypeId, false);

    ProxyRepositoryComponent updatedOuter =
        proxyRepositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(), outerPathname);
    assertThat(updatedOuter.getComponentCount())
        .as("identified-outer gate forces the outer row's componentCount to 1")
        .isEqualTo(1);

    List<ProxyRepositoryPolicyViolation> remaining = proxyRepositoryPolicyViolationDAO
        .getActiveByRepositoryIdAndPathnameOrInnerPathnames(repository.getId(), outerPathname);
    assertThat(remaining)
        .as("stale inner-pathname violation is deleted once the gate collapses the component")
        .noneMatch(v -> innerPathname.equals(v.getPathname()));
  }

  @Test
  public void refreshHostedComponentAfterEvaluation_alwaysCollapse_rubygems_unknownOuter_collapsesToOneComponent() throws Exception {
    // CLM-42119: rubygems collapses to one component regardless of the outer's persisted match
    // state — ALWAYS_COLLAPSE_TO_OUTER_FORMATS fires even when the outer is UNKNOWN.
    String outerPathname = "outer.gem";
    String innerPathname = outerPathname + "!/inner.gem";
    String scanId = "scan-always-collapse-rubygems";
    String stageTypeId = "build";

    Application application = tempEntity.newApplication(tempEntity.newOrganization().getId());
    Repository repository = enableMonitoring(
        tempEntity.newRepository(UUID.randomUUID().toString(), "repo-always-collapse-rubygems", "rubygems"));
    ProxyRepositoryComponent outerComponent = seedOuterComponent(
        repository, outerPathname, MatchState.UNKNOWN, "outer-hash-ruby1",
        ComponentIdentifier.createRubyGemsCoordinates("outer-gem", "1.0", "ruby"), scanId, 2);
    seedInnerViolation(repository, application, innerPathname, "inner-hash-ruby1",
        ComponentIdentifier.createRubyGemsCoordinates("inner-gem", "1.0", "ruby"));

    mockPolicyEvaluatorHdsResponseUnknown(outerComponent.getHash());
    URL zippedReport = ReportHelper.zipReport("/ScanServiceTest/report", tempDir);
    hdsMockServer.respondWith(zippedReport).atUri("rest/application/analysis/" + scanId);
    reportDataStore.downloadReport(application, scanId, (sid, r, aid) -> {
    });

    reportService.refreshHostedComponentAfterEvaluation(
        outerComponent, repository, application, application.getId(), scanId, stageTypeId, false);

    ProxyRepositoryComponent updatedOuter =
        proxyRepositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(), outerPathname);
    assertThat(updatedOuter.getComponentCount())
        .as("always-collapse gate forces the outer row's componentCount to 1 even when outer is UNKNOWN")
        .isEqualTo(1);

    List<ProxyRepositoryPolicyViolation> remaining = proxyRepositoryPolicyViolationDAO
        .getActiveByRepositoryIdAndPathnameOrInnerPathnames(repository.getId(), outerPathname);
    assertThat(remaining)
        .as("stale inner-pathname violation is deleted once the gate collapses the component")
        .noneMatch(v -> innerPathname.equals(v.getPathname()));
  }

  // ---- Helpers -------------------------------------------------------------

  /**
   * Manual Re-Evaluate and Continuous Monitoring both re-upload the original binary's scan, reading it
   * via {@code ScanPersistenceService.getScan(hrc.getId(), scanId)}. The upload path must therefore
   * retain the scan under the HRC's own id once HDS has assigned the canonical scan id.
   */
  @Test
  public void executeJob_retainsScanUnderHrcOwnerIdForReEvaluation() throws Exception {
    Organization org = tempEntity.newOrganizationWithRepositoryManager("test-org-retain-scan");
    String pathname = "com/example/lib/1.0/retained-1.0.jar";

    HostedComponentScanQueue job = insertPendingJobWithScanXmlForOrg(
        org, "comp-retain",
        "pkg:maven/com.example/retained@1.0.0", null,
        pathname, "retain_hash_001", "maven2");

    String scanId = "scan-id-retained";
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId(scanId);
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);
    URL zippedReport = ReportHelper.zipReport("/ScanServiceTest/report", tempDir);
    hdsMockServer.respondWith(zippedReport).atUri("rest/application/analysis/" + scanId);

    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(15, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));

    HostedRepositoryComponent hrc = getHrc(job.getRepositoryId(), pathname);
    assertThat(hrc).isNotNull();

    ScanEntity retained = scanPersistenceService.getScan(hrc.getId(), scanId);
    assertThat(retained.exists())
        .as("scan retained under (hrc.getId()=%s, scanId=%s) for re-evaluation", hrc.getId(), scanId)
        .isTrue();
  }

  /**
   * The upload path writes no scan state onto {@code proxy_repository_component}: a hosted
   * artifact's evaluation history lives in {@code policy_evaluation} under its
   * {@link com.sonatype.insight.brain.model.repository.HostedRepositoryComponent} owner, which is
   * where both continuous monitoring and Manual Re-Evaluate read it from. Leaving {@code scan_id}
   * untouched keeps the Firewall-owned table free of hosted-pipeline writes.
   */
  @Test
  public void executeJob_doesNotStampScanIdOnProxyRepositoryComponent() throws Exception {
    Organization org = tempEntity.newOrganizationWithRepositoryManager("test-org-no-stamp-scan-id");
    String pathname = "com/example/lib/1.0/unstamped-1.0.jar";
    String hash = "stamp_hash_001";

    HostedComponentScanQueue job = insertPendingJobWithScanXmlForOrg(
        org, "comp-stamp",
        "pkg:maven/com.example/stamped@1.0.0", null,
        pathname, hash, "maven2");

    // The Firewall evaluation path inserts this row when the artifact is first proxied, leaving
    // scan_id null. The hosted upload must leave it that way.
    tempEntity.newRepositoryComponent(
        repositoryDAO.getByIdNotNull(job.getRepositoryId()), pathname, MatchState.EXACT, hash);
    assertThat(
        proxyRepositoryComponentDAO.getByRepositoryIdAndPathname(job.getRepositoryId(), pathname).getScanId())
            .as("precondition: the Firewall-inserted row starts with a null scan_id")
            .isNull();

    String scanId = "scan-id-stamped";
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId(scanId);
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);
    URL zippedReport = ReportHelper.zipReport("/ScanServiceTest/report", tempDir);
    hdsMockServer.respondWith(zippedReport).atUri("rest/application/analysis/" + scanId);

    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(15, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));

    ProxyRepositoryComponent component =
        proxyRepositoryComponentDAO.getByRepositoryIdAndPathname(job.getRepositoryId(), pathname);
    assertThat(component)
        .as("proxy_repository_component row for pathname=%s", pathname)
        .isNotNull();
    assertThat(component.getScanId())
        .as("hosted upload leaves scan_id alone; the scan pointer lives in policy_evaluation")
        .isNull();

    // The evaluation the hosted pipeline actually wrote is HRC-owned and carries the scan id.
    HostedRepositoryComponent hrc;
    try (TransactionContext tx = hostedRepositoryComponentDAO.createTransactionContext()) {
      hrc = hostedRepositoryComponentDAO.getByRepositoryIdAndPathname(tx, job.getRepositoryId(), pathname);
    }
    assertThat(hrc)
        .as("upload resolves an HRC owner for the artifact")
        .isNotNull();
    assertThat(policyEvaluationDAO.getLastByOwnerIdAndScanId(hrc.getId(), scanId))
        .as("the scan pointer continuous monitoring reads: policy_evaluation under the HRC owner")
        .isNotNull();
  }

  /**
   * select2-3.2.jar as reported inside {@code /ScanServiceTest/report}'s bom.json — an inner
   * component of the outer scanned artifact, carrying zero CVEs in that fixture's security.json.
   */
  private static final String INNER_PATHNAME = "select2-3.2.jar";

  private static final String INNER_HASH = "f2e35e4a21f07d25710f";

  private static final String NEW_INNER_CVE = "CVE-2026-NEW-INNER-CVE";

  @Test
  public void manualReEvaluate_surfacesNewlyDisclosedInnerComponentVulnerability() throws Exception {
    String outerPathname = "tomcat-util-5.5.23.jar";
    String outerHash = "1249e25aebb15358bedd";
    HostedComponentScanQueue job = insertPendingJobWithScanXml(
        "repo-manual-inner-cve", "comp-manual-inner-cve",
        "pkg:maven/tomcat/tomcat-util@5.5.23", null,
        outerPathname, outerHash, "maven2");

    String scanId = "scan-manual-inner-cve";
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId(scanId);
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);
    URL zippedReport = ReportHelper.zipReport("/ScanServiceTest/report", tempDir);
    hdsMockServer.respondWith(zippedReport).atUri("rest/application/analysis/" + scanId);

    consumer.disableForTesting = false;
    consumer.run();
    await().atMost(15, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));
    consumer.disableForTesting = true;

    HostedRepositoryComponent hrc = getHrc(job.getRepositoryId(), outerPathname);
    assertThat(hrc).as("hosted_repository_component row for the outer artifact").isNotNull();

    tempEntity.newPolicy(hrc, 5, com.sonatype.insight.brain.model.policy.LogicalOperator.AND,
        new com.sonatype.insight.brain.model.policy.Condition(
            com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType.ID,
            ">=", "0"));

    // Before: the inner component's hash carries no active violation at the initial scan.
    assertThat(policyViolationDAO.getActiveByOwnerIdAndStageIdAndHash(
        hrc.getId(), ComplianceStageType.ID, INNER_HASH))
            .as("no violation for the inner component before the new CVE is disclosed")
            .isEmpty();

    // HDS discloses a new CVE against the inner component. Manual Re-Evaluate re-uploads the
    // stored scan under a fresh temporary scanId (ReportService#reUploadScanToHds), fetches the
    // analysis report keyed on that fresh id, then moves the resulting report back under the
    // original scanId — so both the upload receipt and the analysis report need re-stubbing.
    hdsMockServer.reset();
    String reUploadScanId = "scan-manual-inner-cve-reupload";
    ScanReceipt reUploadReceipt = new ScanReceipt();
    reUploadReceipt.setScanId(reUploadScanId);
    hdsMockServer.respondWith(reUploadReceipt).atUri(ScanUploader.HDS_PATH);
    URL updatedReport =
        ReportHelper.zipReport("/HostedRepoInnerCveDiscoveryTest/report-with-new-inner-cve", tempDir);
    hdsMockServer.respondWith(updatedReport).atUri("rest/application/analysis/" + reUploadScanId);

    reportService.reevaluateHostedComponent(hrc.getId(), scanId);

    List<com.sonatype.insight.brain.model.policy.PolicyViolation> after =
        policyViolationDAO.getActiveByOwnerIdAndStageIdAndHash(hrc.getId(), ComplianceStageType.ID, INNER_HASH);
    policyViolationDAO.loadConstraintFacts(after);
    assertThat(after)
        .as("violation for the newly-disclosed inner CVE should exist after Re-Evaluate")
        .anyMatch(v -> v.getConstraintFacts().stream().anyMatch(f -> f.toString().contains(NEW_INNER_CVE)));
  }

  @Test
  public void continuousMonitoring_surfacesNewlyDisclosedInnerComponentVulnerability() throws Exception {
    String outerPathname = "tomcat-util-5.5.23.jar";
    String outerHash = "1249e25aebb15358bedd";

    // Continuous monitoring only processes hosted repositories (RepositoryContinuousMonitoring-
    // FlowProcessor drops any other type), so this test seeds a hosted repository directly rather
    // than the proxy-type default used by the plain insertPendingJobWithScanXml(String, ...) overload.
    Repository repository = enableMonitoring(
        tempEntity.newHostedRepository(tempEntity.newRepositoryManager(), "repo-cm-inner-cve", "maven2", false));
    HostedComponentScanQueue job = insertPendingJobWithScanXml(
        repository, "comp-cm-inner-cve",
        "pkg:maven/tomcat/tomcat-util@5.5.23", null,
        outerPathname, outerHash, "maven2");

    String initialScanId = "scan-cm-inner-cve-initial";
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId(initialScanId);
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);
    URL zippedReport = ReportHelper.zipReport("/ScanServiceTest/report", tempDir);
    hdsMockServer.respondWith(zippedReport).atUri("rest/application/analysis/" + initialScanId);

    consumer.disableForTesting = false;
    consumer.run();
    await().atMost(15, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));
    consumer.disableForTesting = true;

    HostedRepositoryComponent hrc = getHrc(job.getRepositoryId(), outerPathname);
    assertThat(hrc).as("hosted_repository_component row for the outer artifact").isNotNull();

    tempEntity.newPolicy(hrc, 5, com.sonatype.insight.brain.model.policy.LogicalOperator.AND,
        new com.sonatype.insight.brain.model.policy.Condition(
            com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType.ID,
            ">=", "0"));

    // Continuous monitoring's per-component lookup (getByRepositoryIdAndHash) reads a
    // proxy_repository_component row keyed on (repositoryId, hash). The hosted upload path does not
    // write that table, so seed the row the way Firewall would when it proxied the same artifact,
    // then set the identified-outer verdict this scenario needs.
    ProxyRepositoryComponent outer = tempEntity.newRepositoryComponent(
        repository, outerPathname, MatchState.EXACT, outerHash);
    outer.setMatchStateId(MatchState.EXACT.getId());
    outer.setScanId(initialScanId);
    outer.setLastEvaluationStage(ComplianceStageType.ID);
    try (TransactionContext tx = proxyRepositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      proxyRepositoryComponentDAO.update(tx, outer);
      tx.commit();
    }

    // Before: the inner component's hash carries no active violation at the initial scan.
    assertThat(policyViolationDAO.getActiveByOwnerIdAndStageIdAndHash(
        hrc.getId(), ComplianceStageType.ID, INNER_HASH))
            .as("no violation for the inner component before the new CVE is disclosed")
            .isEmpty();

    // HDS discloses a new CVE against the inner component. CM re-uploads the cloned scan under a
    // FRESH scanId, so the mocked upload receipt and the updated report are both keyed on that
    // fresh id rather than the original.
    hdsMockServer.reset();
    String freshScanId = "scan-cm-inner-cve-fresh";
    ScanReceipt freshReceipt = new ScanReceipt();
    freshReceipt.setScanId(freshScanId);
    hdsMockServer.respondWith(freshReceipt).atUri(ScanUploader.HDS_PATH);
    URL updatedReport =
        ReportHelper.zipReport("/HostedRepoInnerCveDiscoveryTest/report-with-new-inner-cve", tempDir);
    hdsMockServer.respondWith(updatedReport).atUri("rest/application/analysis/" + freshScanId);

    com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringQueueItem queueItem =
        tempEntity.newContinuousMonitoringHostedRepoQueueItem(job.getRepositoryId(), outerHash, 0L);
    continuousMonitoringFlowProcessor.process(queueItem);

    // After: asserted on the HRC's owner id (not the original or fresh scanId), matching how
    // every other caller of policy_violation discovers CM's evaluation.
    await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
      List<com.sonatype.insight.brain.model.policy.PolicyViolation> after =
          policyViolationDAO.getActiveByOwnerIdAndStageIdAndHash(hrc.getId(), ComplianceStageType.ID, INNER_HASH);
      policyViolationDAO.loadConstraintFacts(after);
      assertThat(after)
          .as("violation for the newly-disclosed inner CVE should exist after a CM cycle")
          .anyMatch(v -> v.getConstraintFacts().stream().anyMatch(f -> f.toString().contains(NEW_INNER_CVE)));
    });
  }

  /**
   * Re-Evaluate for a hosted-repo artifact is served by
   * {@code rest/report/hostedRepositoryComponent/{hrcId}/{scanId}/reevaluatePolicy}, whose
   * {@code hrcId} path parameter is the owner id that {@code policy_evaluation} rows are keyed on.
   * Driving the resource handler — rather than {@code ReportService} directly — is what proves the
   * REST surface reaches the HRC-owned evaluation path.
   */
  @Test
  public void hostedReevaluateResource_reEvaluatesUnderHrcOwner() throws Exception {
    Organization org = tempEntity.newOrganizationWithRepositoryManager("test-org-reeval-resource");
    String pathname = "com/example/lib/1.0/reeval-resource-1.0.jar";

    HostedComponentScanQueue job = insertPendingJobWithScanXmlForOrg(
        org, "comp-reeval-resource",
        "pkg:maven/com.example/reeval@1.0.0", null,
        pathname, "reeval_resource_hash", "maven2");

    String scanId = "scan-reeval-resource";
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId(scanId);
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);
    URL zippedReport = ReportHelper.zipReport("/ScanServiceTest/report", tempDir);
    hdsMockServer.respondWith(zippedReport).atUri("rest/application/analysis/" + scanId);

    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(15, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));

    HostedRepositoryComponent hrc = getHrc(job.getRepositoryId(), pathname);
    assertThat(hrc).isNotNull();
    assertThat(policyEvaluationDAO.getLastByOwnerIdAndScanId(hrc.getId(), scanId))
        .as("the upload keys policy_evaluation on the HRC, which is the owner id the resource takes")
        .isNotNull();

    hdsMockServer.reset();
    String reUploadScanId = "scan-reeval-resource-reupload";
    ScanReceipt reUploadReceipt = new ScanReceipt();
    reUploadReceipt.setScanId(reUploadScanId);
    hdsMockServer.respondWith(reUploadReceipt).atUri(ScanUploader.HDS_PATH);
    hdsMockServer.respondWith(ReportHelper.zipReport("/ScanServiceTest/report", tempDir))
        .atUri("rest/application/analysis/" + reUploadScanId);

    Response response = hostedRepositoryComponentReportResource.reevaluatePolicy(hrc.getId(), scanId);

    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    assertThat(policyEvaluationDAO.getLastByOwnerIdAndScanId(hrc.getId(), scanId))
        .as("re-evaluation keeps the canonical scanId under the HRC owner")
        .isNotNull();
  }

  /**
   * A hosted artifact that the Firewall never proxied gets no {@code proxy_repository_component} row
   * from the hosted upload path: that table is Firewall-owned, and the artifact's identity lives on
   * {@code hosted_repository_component}. The evaluation must still complete, so the missing row costs
   * only the Components list — which CLM-45066 repoints at the HRC table.
   */
  @Test
  public void executeJob_createsNoFirewallComponentRowForAHostedOnlyArtifact() throws Exception {
    Organization org = tempEntity.newOrganizationWithRepositoryManager("test-org-no-proxy-row");
    String pathname = "com/example/lib/1.0/hosted-only-1.0.jar";
    String hash = "hash_noproxy_01";

    HostedComponentScanQueue job = insertPendingJobWithScanXmlForOrg(
        org, "comp-noproxy",
        "pkg:maven/com.example/hosted-only@1.0.0", null,
        pathname, hash, "maven2");

    String scanId = "scan-id-noproxy";
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId(scanId);
    hdsMockServer.respondWith(receipt).atUri(ScanUploader.HDS_PATH);
    URL zippedReport = ReportHelper.zipReport("/ScanServiceTest/report", tempDir);
    hdsMockServer.respondWith(zippedReport).atUri("rest/application/analysis/" + scanId);

    consumer.disableForTesting = false;
    consumer.run();

    await().atMost(15, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(queueDAO.getById(job.getId()).getStatus())
            .isEqualTo(HostedComponentScanQueueDAO.Status.COMPLETED.name()));

    assertThat(proxyRepositoryComponentDAO.getByRepositoryIdAndPathname(job.getRepositoryId(), pathname))
        .as("the hosted path must not create a Firewall component row for this pathname")
        .isNull();

    // The evaluation still happened — under the HRC owner, which is where every hosted reader looks.
    HostedRepositoryComponent hrc;
    try (TransactionContext tx = hostedRepositoryComponentDAO.createTransactionContext()) {
      hrc = hostedRepositoryComponentDAO.getByRepositoryIdAndPathname(tx, job.getRepositoryId(), pathname);
    }
    assertThat(hrc)
        .as("the artifact's identity lives on hosted_repository_component instead")
        .isNotNull();
    assertThat(policyEvaluationDAO.getLastByOwnerIdAndScanId(hrc.getId(), scanId))
        .as("the scan was evaluated and its pointer recorded under the HRC owner")
        .isNotNull();

    // The Components API still answers — it simply does not list this artifact. The UI keeps calling
    // it until CLM-45066 repoints it, so it must return a page rather than raise.
    assertThat(proxyRepositoryComponentDAO.getByRepositoryIdPaged(job.getRepositoryId(), null, 50, 0))
        .as("the hosted artifact is absent from the Components page, which still renders")
        .noneMatch(c -> pathname.equals(c.getPathname()));
  }

  /**
   * The handler passes {@code getByIdNotNull(hrcId).getId()} rather than {@code hrcId} itself, so an
   * unknown id must 404 from the DAO before any evaluation work starts. Pins that the lookup is
   * load-bearing: passing the path parameter straight through would reach
   * {@code reevaluateHostedComponent} and surface as a different error from a different layer.
   */
  @Test
  public void hostedReevaluateResource_rejectsUnknownHrcIdBeforeEvaluating() {
    assertThatExceptionOfType(com.sonatype.insight.error.exception.NotFoundException.class)
        .isThrownBy(() -> hostedRepositoryComponentReportResource.reevaluatePolicy("no-such-hrc", "no-such-scan"))
        .withMessageContaining("no-such-hrc");
  }

  private HostedRepositoryComponent getHrc(final String repositoryId, final String pathname) {
    try (TransactionContext tx = hostedRepositoryComponentDAO.createTransactionContext()) {
      return hostedRepositoryComponentDAO.getByRepositoryIdAndPathname(tx, repositoryId, pathname);
    }
  }

  /**
   * Seeds an outer {@code proxy_repository_component} row with the given match state, as if an
   * earlier evaluation cycle had already identified (or failed to identify) it — the mirror
   * step's collapse gate reads this pre-existing row rather than producing it itself.
   */
  private ProxyRepositoryComponent seedOuterComponent(
      final Repository repository,
      final String pathname,
      final MatchState matchState,
      final String hash,
      final ComponentIdentifier componentIdentifier,
      final String scanId,
      final int initialComponentCount)
  {
    ProxyRepositoryComponent outer = tempEntity.newRepositoryComponent(
        repository.getId(), matchState, pathname, hash, componentIdentifier, false);
    outer.setScanId(scanId);
    outer.setComponentCount(initialComponentCount);
    try (TransactionContext tx = proxyRepositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      proxyRepositoryComponentDAO.update(tx, outer);
      tx.commit();
    }
    return outer;
  }

  /**
   * Seeds a pre-existing inner-pathname (outerPath + "!/" + innerPath) violation for the gate's cleanup step to delete.
   */
  private ProxyRepositoryPolicyViolation seedInnerViolation(
      final Repository repository,
      final Application application,
      final String innerPathname,
      final String hash,
      final ComponentIdentifier componentIdentifier)
  {
    Policy policy = tempEntity.newPolicy(application, 5);
    return tempEntity.newRepositoryPolicyViolation(repository, policy, innerPathname, componentIdentifier, hash);
  }

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
    return insertPendingJobWithScanXml(repo, componentId, purl, policyEvaluationStage, pathname, sha1, format);
  }

  /**
   * Variant of {@link #insertPendingJobWithScanXml(String, String, String, String, String, String, String)}
   * that accepts an already-created repository, so callers needing a specific {@code RepositoryType}
   * (e.g. hosted, for Continuous Monitoring) can seed it themselves before the job is queued.
   */
  private HostedComponentScanQueue insertPendingJobWithScanXml(
      final Repository repo,
      final String componentId,
      final String purl,
      final String policyEvaluationStage,
      final String pathname,
      final String sha1,
      final String format) throws Exception
  {
    tempEntity.newRepositoryComponent(repo.getId());

    // Write minimal scan XML as plain bytes (Jersey decompresses gzip before writing to temp file)
    String scanXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<scan version=\"2.24\">\n" +
        "<repository id=\"" + repo.getId() + "\" name=\"" + repo.getPublicId() + "\" format=\"" + format + "\"/>\n" +
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
