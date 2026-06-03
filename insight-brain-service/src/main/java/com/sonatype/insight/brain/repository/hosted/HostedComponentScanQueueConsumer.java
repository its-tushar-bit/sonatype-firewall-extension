/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.nio.file.FileAlreadyExistsException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ConfigurationListener;
import com.sonatype.insight.brain.dataaccess.repository.HostedComponentScanQueueDAO;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.report.ApplicationReport;
import com.sonatype.insight.brain.report.ApplicationReportPersistenceService;
import com.sonatype.insight.brain.report.ReportDataStore;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.repository.HostedComponentScanQueue;
import com.sonatype.insight.brain.queue.AbstractPollDispatchQueueConsumer;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.service.ApplicationLifecycle;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Per-tenant worker pool that processes the hosted component scan queue.
 * <p>
 * Extends {@link AbstractPollDispatchQueueConsumer} for the Poll-and-Dispatch pattern
 * and implements {@link ConfigurationListener} for live configuration updates.
 * Implements {@link AdminTask} for manual triggering via
 * {@code POST /tasks/HostedComponentScanQueueConsumer} on the admin port.
 * <p>
 * Each tenant gets its own isolated thread pool. Within a tenant, jobs are processed serially
 * (1 worker thread by default) — tenants never block each other.
 */
@Named
@Singleton
public class HostedComponentScanQueueConsumer
    extends AbstractPollDispatchQueueConsumer<HostedComponentScanQueue>
    implements ConfigurationListener
{
  private static final Logger log = LoggerFactory.getLogger(HostedComponentScanQueueConsumer.class);

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static final String PATH = "HostedComponentScanQueueConsumer";

  private static final String CONSUMER_NAME = PATH;

  private final ApiConfigurationService apiConfigurationService;

  private final HostedComponentScanQueueDAO scanQueueDAO;

  private final Provider<ScanPersistenceService> scanPersistenceServiceProvider;

  private final Provider<ScanUploader> scanUploaderProvider;

  private final RepositoryDAO repositoryDAO;

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  private final Provider<RepositoryPolicyEvaluator> repositoryPolicyEvaluatorProvider;

  private final ApplicationForHostedRepositoryComponentService applicationForHostedComponentService;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final Provider<ReportDataStore> reportDataStoreProvider;

  private final ApplicationReportPersistenceService applicationReportPersistenceService;

  final TenantReference<HostedComponentScanQueueConfig> configs;

  @Inject
  public HostedComponentScanQueueConsumer(
      final ApiConfigurationService apiConfigurationService,
      final HostedComponentScanQueueDAO scanQueueDAO,
      final Provider<ScanPersistenceService> scanPersistenceServiceProvider,
      final Provider<ScanUploader> scanUploaderProvider,
      final RepositoryDAO repositoryDAO,
      final RepositoryComponentDAO repositoryComponentDAO,
      final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      final Provider<RepositoryPolicyEvaluator> repositoryPolicyEvaluatorProvider,
      final ApplicationForHostedRepositoryComponentService applicationForHostedComponentService,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final Provider<ReportDataStore> reportDataStoreProvider,
      final ApplicationReportPersistenceService applicationReportPersistenceService,
      final ShutdownHandler shutdownHandler)
  {
    super(CONSUMER_NAME, shutdownHandler);
    this.apiConfigurationService = apiConfigurationService;
    this.scanQueueDAO = scanQueueDAO;
    this.scanPersistenceServiceProvider = scanPersistenceServiceProvider;
    this.scanUploaderProvider = scanUploaderProvider;
    this.repositoryDAO = repositoryDAO;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.repositoryPolicyEvaluatorProvider = repositoryPolicyEvaluatorProvider;
    this.applicationForHostedComponentService = applicationForHostedComponentService;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.reportDataStoreProvider = reportDataStoreProvider;
    this.applicationReportPersistenceService = applicationReportPersistenceService;
    this.configs = new TenantReference<>(this::loadConfig);
  }

  public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {
    log.info("Manual request to run {}.", CONSUMER_NAME);
    triggerProcessing();
  }

  @Override
  public void configurationChanged(final Set<String> propertyNames) {
    if (!propertyNames.contains(SystemConfigurationProperty.HOSTED_SCAN_QUEUE_CONFIG)) {
      return;
    }
    HostedComponentScanQueueConfig oldConfig = configs.get();
    HostedComponentScanQueueConfig newConfig = loadConfig();
    configs.set(newConfig);
    handleConfigurationChanged(
        newConfig.workerThreadsPerTenant(),
        newConfig.pollIntervalMilliseconds(),
        newConfig.enabled(),
        oldConfig.pollIntervalMilliseconds(),
        oldConfig.enabled());
  }

  @Override
  protected boolean isEnabled() {
    return configs.get().enabled();
  }

  @Override
  protected void recoverStaleJobs() {
    try {
      int reset = scanQueueDAO.resetInProgressToPending();
      if (reset > 0) {
        log.info("Reset {} stale IN_PROGRESS hosted component scan jobs to PENDING on startup", reset);
      }
    }
    catch (Exception e) {
      log.error("Failed to reset stale hosted component scan jobs on startup", e);
    }
  }

  @Override
  protected List<HostedComponentScanQueue> acquireJobs(final int limit) {
    return scanQueueDAO.acquireNextPendingJobs(limit);
  }

  @Override
  protected String getJobId(final HostedComponentScanQueue job) {
    return job.getId();
  }

  @Override
  protected void executeJob(final HostedComponentScanQueue job) throws Exception {
    String repositoryId = job.getRepositoryId();
    ScanEntity scanEntity = scanPersistenceServiceProvider.get().getScanByName(repositoryId, job.getScanFileId());
    if (scanEntity == null) {
      throw new IllegalStateException(
          "Scan file not found: repositoryId=" + repositoryId + ", scanFileId=" + job.getScanFileId());
    }

    ScanComponentInfo componentInfo = ScanXmlParser.extractComponentInfo(scanEntity);

    String stage = job.getPolicyEvaluationStage() != null
        ? job.getPolicyEvaluationStage()
        : ComplianceStageType.ID;

    // Get or create a synthetic application for this component so HDS generates full report files
    com.sonatype.insight.brain.model.Application application = componentInfo != null
        ? applicationForHostedComponentService.getOrCreateApplication(repositoryId, componentInfo.pathname())
        : null;

    ScanReceipt scanReceipt;
    if (application != null) {
      scanReceipt = scanUploaderProvider.get().upload(scanEntity, application, stage, null, null, true);
      log.debug("Uploaded scan via application pipeline, job id={}, scanId={}, appPublicId={}",
          job.getId(), scanReceipt.getScanId(), application.getPublicId());
    }
    else {
      scanReceipt = scanUploaderProvider.get().uploadForRepository(scanEntity, repositoryId, stage, null, true);
      log.debug("Uploaded scan via repository pipeline, job id={}, scanId={}", job.getId(), scanReceipt.getScanId());
    }

    if (componentInfo == null) {
      log.warn("Could not extract component info from scan file for job id={}, skipping policy evaluation",
          job.getId());
      return;
    }
    evaluatePolicies(job, componentInfo, stage);
    stampStage(repositoryId, componentInfo.pathname(), stage.toLowerCase());
    if (application != null) {
      stampScanId(repositoryId, componentInfo.pathname(), scanReceipt.getScanId());
      storeScanForReEvaluate(scanEntity, application.getId(), scanReceipt.getScanId());
      createPolicyEvaluationRecord(application.getId(), scanReceipt.getScanId(), stage);
      saveReportFiles(application, componentInfo.pathname(), scanReceipt.getScanId());
    }
    else {
      log.warn(
          "Could not get/create synthetic application for repositoryId={} pathname={}, report navigation will not be available",
          repositoryId, componentInfo.pathname());
    }
  }

  private void saveReportFiles(final Application application, final String pathname, final String scanId) {
    try {
      // Download HDS report zip so the report page works immediately on first open.
      // Reuse the returned ApplicationReport for bom.json — avoids re-opening the zip.
      ApplicationReport downloadedReport = null;
      try {
        downloadedReport = reportDataStoreProvider.get().downloadReport(application, scanId, (sid, r, aid) -> {
        });
      }
      catch (FileAlreadyExistsException ignored) {
        // concurrent call already downloaded it — fine
      }

      // Patch bom.json displayName — HDS omits it for repository scans; PDF generator requires it.
      // Keep patched bytes to reuse for component count — avoids a second bom.json fetch.
      byte[] patchedBom = null;
      try {
        ApplicationReport reportToRead = downloadedReport != null
            ? downloadedReport
            : reportDataStoreProvider.get().getApplicationReport(application, scanId);
        ReportEntry bomEntry = reportToRead != null ? reportToRead.getEntry("bom.json") : null;
        if (bomEntry != null) {
          patchedBom = HostedReportFileBuilder.patchBomDisplayName(bomEntry.buf);
          applicationReportPersistenceService.saveReportFile(application.getId(), scanId, "bom.json",
              new ByteArrayInputStream(patchedBom));
        }
      }
      catch (Exception ex) {
        log.warn("Failed to patch bom.json displayName for scanId={}: {}", scanId, ex.getMessage());
      }

      // Save policythreats.json only — HDS data.json has the real totalArtifactCount
      // (number of internal components found inside the artifact). Overriding it with
      // our generated version (hardcoded to 1) would mask the true component count.
      RepositoryComponent comp = repositoryComponentDAO.getByScanId(scanId);
      List<RepositoryPolicyViolation> violations =
          comp != null && comp.getPathname() != null
              ? repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathname(
                  comp.getRepositoryId(), comp.getPathname())
              : List.of();
      for (String fileName : List.of("policythreats.json")) {
        byte[] content = HostedReportFileBuilder.build(fileName, comp, violations);
        applicationReportPersistenceService.saveReportFile(application.getId(), scanId, fileName,
            new ByteArrayInputStream(content));
      }

      // Stamp the real internal component count from HDS bom.json → aaData.length.
      // bom.json lists every component HDS found inside the artifact (the artifact itself
      // plus all nested/bundled dependencies), which is what we want to display.
      // data.json.totalArtifactCount only counts the outer artifact (always 1 for a single upload).
      if (comp != null) {
        try {
          if (patchedBom != null) {
            JsonNode bomJson = MAPPER.readTree(patchedBom);
            JsonNode aaData = bomJson.path("aaData");
            int count = aaData.isArray() ? aaData.size() : 1;
            try (TransactionContext tx =
                repositoryComponentDAO.createTransactionContext())
            {
              tx.begin();
              repositoryComponentDAO.stampComponentCount(tx, comp.getRepositoryId(), comp.getPathname(), count);
              tx.commit();
            }
          }
        }
        catch (Exception ex) {
          log.warn("Failed to stamp component count for scanId={}: {}", scanId, ex.getMessage());
        }
      }

      log.debug("Saved report files for hosted component appId={} scanId={}", application.getId(), scanId);
    }
    catch (Exception e) {
      log.warn("Failed to save report files for hosted component appId={} scanId={}: {}",
          application.getId(), scanId, e.getMessage());
    }
  }

  private void storeScanForReEvaluate(final ScanEntity scanEntity, final String appId, final String scanId) {
    try {
      ScanEntity tempScan = scanPersistenceServiceProvider.get().createTempScan(appId);
      scanPersistenceServiceProvider.get().copyScanFile(scanEntity, tempScan);
      scanPersistenceServiceProvider.get().moveTempScan(tempScan, appId, scanId);
      log.debug("Stored scan for re-evaluate: appId={} scanId={}", appId, scanId);
    }
    catch (Exception e) {
      log.warn("Failed to store scan for re-evaluate appId={} scanId={}: {}", appId, scanId, e.getMessage(), e);
    }
  }

  private void createPolicyEvaluationRecord(final String appId, final String scanId, final String stageTypeId) {
    try (TransactionContext tx = policyEvaluationDAO.createTransactionContext()) {
      tx.begin();
      if (policyEvaluationDAO.getLastByApplicationIdAndScanId(tx, appId, scanId) == null) {
        PolicyEvaluation pe = new PolicyEvaluation(
            appId, stageTypeId.toLowerCase(), scanId, false, false, "system",
            ScanTriggerType.REPOSITORY_MANAGER, null);
        policyEvaluationDAO.insert(tx, pe);
        log.debug("Created policy_evaluation record appId={} scanId={}", appId, scanId);
      }
      tx.commit();
    }
    catch (Exception e) {
      log.warn("Failed to create policy_evaluation record appId={} scanId={}: {}", appId, scanId, e.getMessage(), e);
    }
  }

  private void stampStage(final String repositoryId, final String pathname, final String stage) {
    try (TransactionContext tx = repositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      repositoryComponentDAO.stampLastEvaluationStage(tx, repositoryId, pathname, stage);
      tx.commit();
    }
    catch (Exception e) {
      log.warn("Failed to stamp stage for pathname={}: {}", pathname, e.getMessage(), e);
    }
  }

  private void stampScanId(final String repositoryId, final String pathname, final String scanId) {
    try (TransactionContext tx = repositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      repositoryComponentDAO.stampScanId(tx, repositoryId, pathname, scanId);
      tx.commit();
    }
    catch (Exception e) {
      log.warn("Failed to stamp scan_id for pathname={}: {}", pathname, e.getMessage(), e);
    }
  }

  private void evaluatePolicies(
      final HostedComponentScanQueue job,
      final ScanComponentInfo componentInfo,
      final String stageTypeId)
  {
    Repository repository = repositoryDAO.getById(job.getRepositoryId());
    if (repository == null) {
      throw new IllegalStateException(
          "Repository not found for policy evaluation: repositoryId=" + job.getRepositoryId()
              + ", job id=" + job.getId());
    }

    RepositoryComponentEvaluationDataRequestList request =
        new RepositoryComponentEvaluationDataRequestList("INITIAL_SCAN");
    request.components.add(new RepositoryComponentEvaluationDataRequest(
        componentInfo.format() != null ? componentInfo.format() : repository.getFormat(),
        componentInfo.pathname(),
        componentInfo.hash()));

    repositoryPolicyEvaluatorProvider.get()
        .evaluate(repository, request, false /* withQuarantine */, null, stageTypeId);
    log.debug("Policy evaluation completed for job id={}, pathname={}", job.getId(), componentInfo.pathname());

    if (job.getComponentId() != null) {
      stampNxrmComponentId(job.getRepositoryId(), componentInfo.pathname(), job.getComponentId());
    }
  }

  private void stampNxrmComponentId(
      final String repositoryId,
      final String pathname,
      final String componentId)
  {
    try (TransactionContext tx = repositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      repositoryComponentDAO.stampComponentId(tx, repositoryId, pathname, componentId);
      repositoryPolicyViolationDAO.stampComponentId(tx, repositoryId, pathname, componentId);
      tx.commit();
      log.debug("Stamped component_id={} on repository_component and repository_policy_violation for pathname={}",
          componentId, pathname);
    }
    catch (Exception e) {
      log.warn("Failed to stamp component_id for pathname={}: {}", pathname, e.getMessage(), e);
    }
  }

  @Override
  protected void onJobSuccess(final HostedComponentScanQueue job) {
    scanQueueDAO.completeJob(job.getId());
    try {
      ScanEntity scanEntity =
          scanPersistenceServiceProvider.get().getScanByName(job.getRepositoryId(), job.getScanFileId());
      if (scanEntity != null) {
        scanPersistenceServiceProvider.get().deleteScan(scanEntity);
      }
    }
    catch (Exception e) {
      log.warn("Failed to clean up scan file scanFileId={} for completed job id={}",
          job.getScanFileId(), job.getId(), e);
    }
  }

  @Override
  protected int incrementRetryCount(final HostedComponentScanQueue job) {
    return scanQueueDAO.incrementRetryCount(job.getId());
  }

  @Override
  protected void unacquireJobs(final Set<String> ids) {
    scanQueueDAO.unacquireJobs(ids);
  }

  @Override
  protected void permanentlyFailJob(final HostedComponentScanQueue job, final Exception cause) {
    String errorMessage = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getName();
    scanQueueDAO.failJob(job.getId(), errorMessage);
    try {
      ScanEntity scanEntity =
          scanPersistenceServiceProvider.get().getScanByName(job.getRepositoryId(), job.getScanFileId());
      if (scanEntity != null) {
        scanPersistenceServiceProvider.get().deleteScan(scanEntity);
      }
    }
    catch (Exception e) {
      log.warn("Failed to clean up scan file scanFileId={} for permanently failed job id={}",
          job.getScanFileId(), job.getId(), e);
    }
  }

  @Override
  protected int getWorkerThreadCount() {
    return configs.get().workerThreadsPerTenant();
  }

  @Override
  protected int getMaxQueuedRows() {
    return configs.get().maxQueuedRows();
  }

  @Override
  protected long getPollIntervalMs() {
    return configs.get().pollIntervalMilliseconds();
  }

  @Override
  protected int getMaxRetries() {
    return configs.get().maxRetries();
  }

  @Override
  protected String getConsumerName() {
    return CONSUMER_NAME;
  }

  @Override
  protected String getJitterSeed() {
    return ApplicationLifecycle.getServerInstanceId() + TenantThreadLocal.getTenant().tenantSlug;
  }

  private HostedComponentScanQueueConfig loadConfig() {
    Object raw = apiConfigurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.HOSTED_SCAN_QUEUE_CONFIG);
    return raw instanceof HostedComponentScanQueueConfig cfg
        ? cfg
        : HostedComponentScanQueueConfig.defaultConfig();
  }
}
