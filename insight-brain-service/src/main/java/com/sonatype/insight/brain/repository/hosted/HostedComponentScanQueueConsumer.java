/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.io.ByteArrayInputStream;
import java.nio.file.FileAlreadyExistsException;
import java.util.List;
import java.util.Set;

import jakarta.annotation.Nonnull;
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

  /**
   * Soft warning threshold for the per-archive component count. We don't enforce a hard cap
   * because silently truncating inner findings would hide real CVEs. The number is large enough
   * to never trip on normal application archives (~5–50 inner jars) but small enough that
   * anything above it is worth a logged note for ops correlation.
   */
  private static final int HIGH_COMPONENT_COUNT_THRESHOLD = 500;

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

    // The scanner emits one <dir> per archive it recognised. For a single-jar upload that's one
    // entry; for an archive-of-archives upload (e.g. a .zip containing multiple .jar files) it's
    // one entry per inner artifact. The first <dir> is always the outer artifact (the thing the
    // user uploaded); any subsequent <dir>s are inner artifacts the scanner discovered inside it.
    // We process the outer artifact as the single repository_component row (so the Components page
    // shows one row per uploaded artifact) but pass ALL components — outer + inners — into the
    // policy evaluator so violations get persisted for every inner pathname too. Inner-pathname
    // repository_component rows are deleted post-eval so the Components page stays clean; the
    // inner-pathname repository_policy_violation rows survive and feed the synthesised
    // policythreats.json so drilling into the outer's report shows per-inner findings.
    List<ScanComponentInfo> componentInfos = ScanXmlParser.extractComponentInfos(scanEntity);
    // Visibility for outsized archives: a normal hosted upload produces a handful of inner
    // components; anything north of HIGH_COMPONENT_COUNT_THRESHOLD is unusual and worth flagging
    // (could be a deeply nested archive, a recursive zip, or a misconfigured scanner). We don't
    // truncate the list — silently dropping inner findings would hide real CVEs and make the
    // gap impossible to debug from logs alone — but we want ops to see the count so they can
    // correlate with downstream timing/memory pressure.
    if (componentInfos.size() > HIGH_COMPONENT_COUNT_THRESHOLD) {
      log.warn("Archive scan job id={} unpacked into {} components (threshold={}). Processing all but "
          + "this is unusual; investigate if it correlates with degraded eval performance.",
          job.getId(), componentInfos.size(), HIGH_COMPONENT_COUNT_THRESHOLD);
    }

    String stage = job.getPolicyEvaluationStage() != null
        ? job.getPolicyEvaluationStage()
        : ComplianceStageType.ID;

    if (componentInfos.isEmpty()) {
      // No usable <dir> in the scan file. Still upload via the repository pipeline so HDS has the
      // raw scan record (matches today's behaviour for non-archive scans that fail to parse), then
      // bail — no application, no evaluation, no report.
      ScanReceipt scanReceipt = scanUploaderProvider.get()
          .uploadForRepository(scanEntity, repositoryId, stage, null, true);
      log.warn("Could not extract any component info from scan file for job id={}; uploaded via repository pipeline"
          + " (scanId={}) and skipping policy evaluation.",
          job.getId(), scanReceipt.getScanId());
      return;
    }

    ScanComponentInfo outerComponentInfo = componentInfos.get(0);

    // Get or create the synthetic application keyed on the OUTER pathname only — one app per
    // uploaded artifact, matching today's UX (the Components page lists the artifact once).
    com.sonatype.insight.brain.model.Application application =
        applicationForHostedComponentService.getOrCreateApplication(repositoryId, outerComponentInfo.pathname());

    ScanReceipt scanReceipt;
    if (application != null) {
      scanReceipt = scanUploaderProvider.get().upload(scanEntity, application, stage, null, null, true);
      log.debug("Uploaded scan via application pipeline, job id={}, scanId={}, appPublicId={}, pathname={}",
          job.getId(), scanReceipt.getScanId(), application.getPublicId(), outerComponentInfo.pathname());
    }
    else {
      scanReceipt = scanUploaderProvider.get().uploadForRepository(scanEntity, repositoryId, stage, null, true);
      // WARN, not DEBUG — getOrCreateApplication should always succeed in normal operation. A
      // null return means the synthetic-application creation failed (org missing, permission
      // issue, race during cleanup), and the scan falls back to the repository pipeline which
      // generates a different report shape downstream. Ops needs to see this in production logs
      // to correlate with missing per-component reports in the UI.
      log.warn("No synthetic application for hosted scan, job id={}, scanId={}, pathname={}; "
          + "falling back to repository upload pipeline (per-component report links will be unavailable).",
          job.getId(), scanReceipt.getScanId(), outerComponentInfo.pathname());
    }

    // Evaluate ALL components (outer + every inner) in a single request. The evaluator persists
    // one repository_component row + one batch of repository_policy_violation rows per request
    // entry. We delete the inner repository_component rows immediately afterwards (see below) so
    // the Components page only ever shows the outer artifact.
    evaluatePolicies(job, repositoryId, componentInfos, stage);

    stampStage(repositoryId, outerComponentInfo.pathname(), stage.toLowerCase());

    if (componentInfos.size() > 1) {
      deleteInnerRepositoryComponentRows(repositoryId, componentInfos);
      log.info("Processed archive-of-archives scan for job id={}: outer pathname={} retained, {} inner rows deleted",
          job.getId(), outerComponentInfo.pathname(), componentInfos.size() - 1);
    }

    persistApplicationLinkedReportFiles(
        repositoryId, scanEntity, outerComponentInfo, application, scanReceipt, stage);
  }

  /**
   * Persists the synthetic-application linkage and the HDS report files for an evaluated component.
   * <p>
   * Extracted from {@link #executeJob} so the synchronous enforcement allow path
   * ({@code HostedComponentEvaluationService.evaluateSynchronously}) can keep parity: stamp
   * {@code scanId} on {@code repository_component}, persist the policy_evaluation row, and download
   * the HDS report bundle so the report link is clickable in the UI.
   * <p>
   * Called by both paths (async queue consumer + sync enforcement) so a fix here applies to both.
   * <p>
   * Public so {@link #persistApplicationForSyncAllowPath} (same package) and
   * {@code HostedComponentEvaluationService} (different package, sync allow path entry) can both
   * delegate here. Not part of any external API contract.
   *
   * @param scanReceipt non-null receipt produced by a successful
   *          {@link com.sonatype.insight.brain.hds.ScanUploader} upload — the upload either returns
   *          a receipt or throws, so callers don't need to null-guard
   */
  public void persistApplicationLinkedReportFiles(
      final String repositoryId,
      final ScanEntity scanEntity,
      final ScanComponentInfo componentInfo,
      final com.sonatype.insight.brain.model.Application application,
      @Nonnull final ScanReceipt scanReceipt,
      final String stage)
  {
    if (componentInfo == null) {
      return;
    }
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

  /**
   * Sync-enforcement entry point that mirrors the async queue consumer's application-linked persistence.
   * <p>
   * Called by {@code HostedComponentEvaluationService.evaluateSynchronously} after an allow verdict
   * has been computed and {@code repository_component}/{@code repository_policy_violation} rows have
   * been persisted by the evaluator. This step ensures a synthetic {@code application} row exists,
   * re-uploads the scan via the application pipeline so HDS regenerates per-application report files,
   * and stamps the {@code scanId} so the UI Report link becomes clickable.
   * <p>
   * <b>Note on the double HDS upload:</b> the sync enforcement path already uploaded the scan via
   * {@code uploadForRepository} before the evaluation. This method performs a second upload via
   * {@code upload(scanEntity, application, ...)} so HDS keys the report bundle to the synthetic
   * application's id (not the repository id) — that's what makes the per-component Report link
   * resolvable in the UI. The duplicate upload is acceptable because HDS de-dupes identical scan
   * payloads; the marginal latency on the sync path is small compared to the initial upload + dual
   * evaluation. Collapsing the two uploads would require restructuring the sync block-path
   * semantics (block must NOT create an application row), so we keep them separate intentionally.
   * <p>
   * Failures here are logged but not rethrown — the enforcement verdict has already been returned
   * to NXRM and must not be invalidated by an audit-only persistence hiccup.
   * <p>
   * <b>Archive-of-archives fan-out scope (CLM-40943):</b> this sync allow path takes a single
   * {@link ScanComponentInfo} (the outer artifact) and does NOT walk inner archive entries. So
   * for a {@code .zip} containing inner {@code .jar}s uploaded through the sync enforcement
   * path, only the outer artifact is evaluated synchronously; inner-jar findings surface on the
   * next async monitoring cycle (which DOES fan out — see {@link #executeJob}). Sync-side
   * fan-out is intentionally deferred per the original CLM-40943 design (synchronous
   * enforcement keeps a single allow/block verdict on the outer binary's hash); a follow-up
   * ticket would be needed to make sync evaluation symmetric with the async path.
   */
  public void persistApplicationForSyncAllowPath(
      final String repositoryId,
      final ScanEntity scanEntity,
      final ScanComponentInfo componentInfo,
      final String stage)
  {
    if (componentInfo == null) {
      return;
    }
    try {
      com.sonatype.insight.brain.model.Application application = applicationForHostedComponentService
          .getOrCreateApplication(repositoryId, componentInfo.pathname());
      if (application == null) {
        log.warn(
            "Could not get/create synthetic application for repositoryId={} pathname={} (sync allow path), report navigation will not be available",
            repositoryId, componentInfo.pathname());
        return;
      }
      ScanReceipt scanReceipt = scanUploaderProvider.get()
          .upload(scanEntity, application, stage, null, null, true);
      log.debug("Re-uploaded scan via application pipeline (sync allow path), scanId={}, appPublicId={}",
          scanReceipt.getScanId(), application.getPublicId());
      persistApplicationLinkedReportFiles(repositoryId, scanEntity, componentInfo, application, scanReceipt, stage);
    }
    catch (Exception e) {
      // componentInfo is guaranteed non-null by the early-return guard at the top of this method.
      log.warn("Failed to persist application linkage for sync allow path repositoryId={} pathname={}: {}",
          repositoryId, componentInfo.pathname(), e.getMessage(), e);
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
      // For an archive-of-archives upload the evaluator persisted N policy_violation rows: one
      // batch keyed on the outer pathname (outer.zip) plus one batch per inner pathname
      // (outer.zip!/inner.jar). The Components page only shows the outer row, so the synthesised
      // policythreats.json that backs the outer's report must include violations from BOTH the
      // outer pathname AND any inner pathname under it.
      List<RepositoryPolicyViolation> violations = List.of();
      if (comp != null && comp.getPathname() != null) {
        violations = repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathnameOrInnerPathnames(
            comp.getRepositoryId(), comp.getPathname());
      }
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
      final String repositoryId,
      final List<ScanComponentInfo> componentInfos,
      final String stageTypeId)
  {
    Repository repository = repositoryDAO.getById(repositoryId);
    if (repository == null) {
      throw new IllegalStateException(
          "Repository not found for policy evaluation: repositoryId=" + repositoryId
              + ", job id=" + job.getId());
    }

    RepositoryComponentEvaluationDataRequestList request =
        new RepositoryComponentEvaluationDataRequestList("INITIAL_SCAN");
    for (ScanComponentInfo info : componentInfos) {
      request.components.add(new RepositoryComponentEvaluationDataRequest(
          info.format() != null ? info.format() : repository.getFormat(),
          info.pathname(),
          info.hash()));
    }

    repositoryPolicyEvaluatorProvider.get()
        .evaluate(repository, request, false /* withQuarantine */, null, stageTypeId);
    log.debug("Policy evaluation completed for job id={}, components={} (outer + {} inner)",
        job.getId(), componentInfos.size(),
        Math.max(0, componentInfos.size() - 1));

    // Stamp NXRM componentId on the outer pathname AND on every inner-pathname violation row
    // (outer.zip!/inner.jar) the evaluator just persisted. The Components page is keyed on the
    // outer (we delete the inner repository_component rows next), but downstream code that
    // joins on repository_policy_violation.component_id — waivers-by-component, quarantine-by-
    // component — needs the column populated on the inner rows too. Skipping this would create
    // a silent gap where component-keyed waivers don't match inner-artifact violations.
    if (job.getComponentId() != null && !componentInfos.isEmpty()) {
      ScanComponentInfo outer = componentInfos.get(0);
      stampNxrmComponentIdOnOuterAndInnerPathnames(repositoryId, outer.pathname(), job.getComponentId());
    }
  }

  /**
   * Removes the {@code repository_component} rows the evaluator created for inner artifact
   * pathnames (those containing the {@code !/} separator). The inner-pathname
   * {@code repository_policy_violation} rows are intentionally left in place — they feed the
   * outer artifact's synthesised {@code policythreats.json} so drilling into the outer report
   * surfaces every inner finding.
   */
  private void deleteInnerRepositoryComponentRows(
      final String repositoryId,
      final List<ScanComponentInfo> componentInfos)
  {
    if (componentInfos.size() <= 1) {
      return;
    }
    // Collect inner-pathname list once; the DAO batches into IN-clause chunks internally so an
    // archive with N inner artifacts costs ⌈N/threshold⌉ DELETEs, not 2N (the old SELECT+DELETE
    // per inner). Cascades to quarantined_component_access via the same IN clause.
    List<String> innerPathnames = new java.util.ArrayList<>(componentInfos.size() - 1);
    for (int i = 1; i < componentInfos.size(); i++) {
      innerPathnames.add(componentInfos.get(i).pathname());
    }
    try (TransactionContext tx = repositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      repositoryComponentDAO.deleteByRepositoryIdAndPathnames(tx, repositoryId, innerPathnames);
      tx.commit();
    }
    catch (Exception e) {
      // Don't fail the job — at worst the Components page shows extra inner-pathname rows the
      // user can ignore. The outer row + report are unaffected.
      log.warn("Failed to delete inner repository_component rows for repositoryId={}: {}",
          repositoryId, e.getMessage(), e);
    }
  }

  /**
   * Stamps {@code component_id} on the outer artifact's {@code repository_component} row AND on
   * every active violation row whose pathname is either the outer pathname OR an inner-pathname
   * under it ({@code outer.zip!/inner.jar}). The {@code repository_component} side is intentionally
   * outer-only because the inner repository_component rows are deleted in
   * {@link #deleteInnerRepositoryComponentRows}; the violation side covers both so future
   * component-id-keyed code paths (waivers, quarantine) match inner-artifact findings too.
   */
  private void stampNxrmComponentIdOnOuterAndInnerPathnames(
      final String repositoryId,
      final String outerPathname,
      final String componentId)
  {
    try (TransactionContext tx = repositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      repositoryComponentDAO.stampComponentId(tx, repositoryId, outerPathname, componentId);
      repositoryPolicyViolationDAO.stampComponentIdOnPathnameOrInnerPathnames(
          tx, repositoryId, outerPathname, componentId);
      tx.commit();
      log.debug("Stamped component_id={} on repository_component (outer) and "
          + "repository_policy_violation (outer + inner pathnames) for pathname={}",
          componentId, outerPathname);
    }
    catch (Exception e) {
      log.warn("Failed to stamp component_id for pathname={}: {}", outerPathname, e.getMessage(), e);
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
