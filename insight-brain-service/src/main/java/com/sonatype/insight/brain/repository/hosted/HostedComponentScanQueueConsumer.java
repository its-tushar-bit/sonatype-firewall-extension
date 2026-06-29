/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.io.ByteArrayInputStream;
import java.nio.file.FileAlreadyExistsException;
import java.util.Date;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.service.AdminTask;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ConfigurationListener;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.HostedComponentScanQueueDAO;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.report.ApplicationReport;
import com.sonatype.insight.brain.report.ApplicationReportPersistenceService;
import com.sonatype.insight.brain.report.ReportDataStore;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.repository.HostedComponentScanQueue;
import com.sonatype.insight.brain.queue.AbstractPollDispatchQueueConsumer;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.service.ApplicationLifecycle;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.telemetry.model.TelemetryData;

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

  /**
   * CLM-40943 (per Dariush Slack 2026-06-26 "match the CLI scan as though you just passed it
   * the binary"): formats whose published artifacts legitimately contain real binary inner
   * components that iq-cli also surfaces. For these formats the identified-outer gate in
   * {@link #mirrorNestedComponentViolationsFromApplicationEvaluation} does NOT collapse to 1;
   * the existing {@code ScanPolicyEvaluator} + mirror path runs, matching what iq-cli
   * produces for the same single-file binary.
   * <p>
   * <b>Nuget</b> is the canonical example: a {@code .nupkg} ships framework-fanout DLLs
   * (cs/zh-Hans/zh-Hant/pt-BR/... localization resources, roslyn3.11 + roslyn4.0 analyzers).
   * For {@code System.Text.Json.9.0.0.nupkg}, iq-cli single-file scan returns 21 components
   * with distinct hashes — that's the truth, hosted must report 21 too.
   * <p>
   * <b>Go</b> module zips ship the full transitive dependency tree via {@code go.mod} +
   * vendored sources (or proxy-resolved deps). For {@code gin-gonic/gin v1.7.0.zip} the
   * iq-cli / LC application Evaluate File path surfaces 7 components (gin + its direct
   * imports); for {@code go-resty/resty/v2 v2.7.0.zip} it surfaces 33 components. Without
   * this carveout the hosted-repo gate collapses identified Go zips to a single-component
   * view that hides every dependency CVE — empirically confirmed against single-hosted-test-go
   * 2026-06-27.
   * <p>
   * <b>Pub (Dart/Flutter)</b> packages ship {@code pubspec.yaml} declaring their dependencies.
   * For {@code vulnerable_test 1.0.0.tar.gz} the LC application path surfaces 5 components
   * (vulnerable_test + archive + http + http_parser + path); without this carveout the
   * hosted gate stamps {@code componentCount = 1} while {@code repository_policy_violation}
   * still holds the inner rows — producing an internal inconsistency where the header pill
   * reads "1 COMPONENT" but the body table shows 5 rows.
   * <p>
   * Formats NOT in this set fall under the collapse path: identified outer → 1 component,
   * outer's own violations. This matches iq-cli for rubygems gem (whose Gemfile.lock-derived
   * entries iq-cli drops), pypi wheel/sdist, npm tgz, maven jar, r tarball.
   * <p>
   * Default for any new/unknown format is the SAFER "collapse" path. If a future format
   * (cargo, yum, docker) turns out to have nuget-like nested binaries, add it here. The set
   * is small and explicit on purpose — easier to audit than the inverse.
   */
  private static final Set<String> KEEP_NESTED_FORMATS_FOR_IDENTIFIED_OUTER =
      Set.of("nuget", "go", "pub", "npm");

  /**
   * CLM-40943 follow-up (2026-06-27): formats whose dependency graph is itself the source of
   * truth — i.e. the manifest inside the archive (go.mod for go, pubspec.yaml for pub) is
   * what LC's iq-cli / Evaluate File path uses to identify transitive components, not a
   * "drop these" hint. For these formats the {@code dependency:}-prefixed pathname filter is
   * bypassed so the mirrored {@code repository_policy_violation} rows and the stamped
   * {@code component_count} match the LC application report.
   * <p>
   * Default for any format NOT in this set: the filter applies (manifest-derived entries are
   * treated as scanner noise, matching the original CLM-40943 design for rubygems / pypi /
   * npm where manifest lockfiles inflate the count past what iq-cli reports).
   */
  private static final Set<String> KEEP_DEPENDENCY_DERIVED_COMPONENTS_FORMATS = Set.of("go", "pub");

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

  // CLM-40943: dependencies for the bom-driven nested-component mirror.
  private final Provider<ScanPolicyEvaluator> scanPolicyEvaluatorProvider;

  private final PolicyViolationDAO policyViolationDAO;

  private final ApplicationComponentDAO applicationComponentDAO;

  private final TelemetryUtils telemetryUtils;

  private final TelemetrySender telemetrySender;

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
      final Provider<ScanPolicyEvaluator> scanPolicyEvaluatorProvider,
      final PolicyViolationDAO policyViolationDAO,
      final ApplicationComponentDAO applicationComponentDAO,
      final TelemetryUtils telemetryUtils,
      final TelemetrySender telemetrySender,
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
    this.scanPolicyEvaluatorProvider = scanPolicyEvaluatorProvider;
    this.policyViolationDAO = policyViolationDAO;
    this.applicationComponentDAO = applicationComponentDAO;
    this.telemetryUtils = telemetryUtils;
    this.telemetrySender = telemetrySender;
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

    // CLM-41693: Emit APPLICATION_EVALUATION_COMPONENT_COUNTS telemetry with
    // scan_trigger_type=HOSTED_REPOSITORY_SCANNING so telemetry consumers can distinguish
    // hosted repository scans from other scan trigger types (CLI, IDE, WEB_UI, etc.).
    //
    // Guarded on application != null because the application-id is a required attribute of this
    // telemetry event and is not available on the repository-upload fallback path above
    // (uploadForRepository). That fallback fires only when the synthetic application could not
    // be created (logged at WARN — see the else branch above) and is a system-error condition,
    // not a normal flow. scanReceipt is guaranteed non-null after either upload branch (both
    // throw on failure), but kept as a defensive guard against future refactors of the upload
    // contract. Tracked under Phase 2 (CLM-40999) if telemetry needs to cover the fallback path.
    if (application != null && scanReceipt != null) {
      sendHostedScanEvaluationTelemetry(
          scanReceipt.getScanId(), application.getId(), stage, componentInfos);
    }

    stampStage(repositoryId, outerComponentInfo.pathname(), stage.toLowerCase());

    // Eagerly raise component_count from the scanner's view (componentInfos.size()). On runtimes
    // where the HDS report's bom.json is not immediately available after downloadReport (S3-
    // backed tenant storage with network latency), the bom-based count stamp inside
    // saveReportFiles silently no-ops and the column would otherwise stay NULL forever. This
    // eager raise guarantees a useful floor value lands every time. Both this call and the later
    // saveReportFiles refinement go through raiseComponentCountIfHigher, so neither path can
    // regress the column to a smaller value — including on re-scans where the scanner count is
    // smaller than a previously HDS-refined value.
    stampComponentCount(repositoryId, outerComponentInfo.pathname(), componentInfos.size());

    persistApplicationLinkedReportFiles(
        repositoryId, scanEntity, outerComponentInfo, application, scanReceipt, stage);

    // CLM-40943: Nested-component policy violations via ScanPolicyEvaluator + mirror.
    //
    // The repository-evaluation pipeline (evaluatePolicies above) only writes a single
    // repository_policy_violation row for the outer artifact, because the scanner emits a
    // single <dir> for archives whose container format it cannot crack natively (npm .tgz,
    // pypi sdist/wheel, helm charts, go module zips, etc.) and HDS's firewall purpose returns
    // only outer-scope match data.
    //
    // The Lifecycle-evaluation pipeline already does the right thing for the same scan: it
    // runs full Drools policy evaluation against every component HDS identified in bom.json
    // (outer + every nested), producing one policy_violation row per (component × policy).
    // Hosted-repo doesn't normally invoke that path because its persistence boundary is
    // repository_policy_violation, not policy_violation.
    //
    // Solution: invoke ScanPolicyEvaluator on the synthetic application IQ already created
    // for this hosted upload — same Drools logic that runs for "Evaluate a binary" in the UI —
    // then mirror each resulting policy_violation row into repository_policy_violation,
    // skipping the row that corresponds to the outer (evaluatePolicies already handled that
    // and the existing data is the source of truth for quarantine + firewall behaviour).
    //
    // Format-agnostic: any format HDS identifies nested components for (npm confirmed; pypi,
    // helm, go, nuget, rubygems pending HDS support per format) gets per-inner violations.
    // Must run AFTER persistApplicationLinkedReportFiles so report.zip is on disk —
    // ScanPolicyEvaluator reads bom.json/security.json/licenses.json from it.
    if (application != null && scanReceipt != null) {
      mirrorNestedComponentViolationsFromApplicationEvaluation(
          job, repositoryId, outerComponentInfo, application, scanReceipt.getScanId(), stage);
    }

    if (componentInfos.size() > 1) {
      deleteInnerRepositoryComponentRows(repositoryId, componentInfos);
      log.info("Processed archive-of-archives scan for job id={}: outer pathname={} retained, {} inner rows deleted",
          job.getId(), outerComponentInfo.pathname(), componentInfos.size() - 1);
    }
  }

  /**
   * Eagerly raises {@code component_count} on the outer artifact's {@code repository_component}
   * row to the scanner's {@code componentInfos.size()} — but ONLY if that value is higher than
   * whatever is already stored (or the column is NULL). The same atomic-monotonic semantics that
   * {@code saveReportFiles}'s HDS-bom refinement uses, applied here too: this means a re-scan
   * cannot transiently regress a column from an HDS-refined-up value (e.g. 185) back to the
   * scanner's smaller count (e.g. 3) and then have to be re-raised. The first scan establishes a
   * floor; subsequent scans only ever raise. Failures are logged but don't fail the job.
   */
  private void stampComponentCount(
      final String repositoryId,
      final String pathname,
      final int count)
  {
    try (TransactionContext tx = repositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      repositoryComponentDAO.raiseComponentCountIfHigher(tx, repositoryId, pathname, count);
      tx.commit();
    }
    catch (Exception e) {
      log.warn("Failed to eagerly raise component_count={} for pathname={}: {}",
          count, pathname, e.getMessage(), e);
    }
  }

  /**
   * CLM-40943: unconditionally writes {@code component_count} on the outer artifact's
   * {@code repository_component} row from the synthetic application's post-evaluation view.
   * After {@code ScanPolicyEvaluator.evaluate()} runs, {@code application_component} holds one
   * row per distinct component HDS identified for this scan (outer + every inner). That count
   * is the same number LC's application report shows for the same scan, so it is the source
   * of truth — overwriting both the scanner-based eager stamp ({@code executeJob}) and the
   * HDS-bom refinement in {@code saveReportFiles}.
   * <p>
   * Uses the DAO's {@code stampComponentCount} (unconditional {@code UPDATE}) rather than
   * {@code raiseComponentCountIfHigher} because the earlier stamps can be both higher than
   * the truth (scanner's file-list expansion for .gem / .tar.gz) and lower than the truth
   * (early HDS firewall-purpose bom for .nupkg). Failures are logged but don't fail the job —
   * the prior stamps are still a usable approximation.
   * <p>
   * Known limitation (tracked separately): for archives whose payload includes a dependency
   * manifest the hosted-side scanner parses (e.g. {@code Gemfile.lock} inside a .gem,
   * {@code requirements.txt} inside a sdist), the synthetic-app's bom will contain manifest-
   * derived transitive entries that LC's iq-cli scan does not extract by default. In those
   * cases the stamped count will exceed LC's count.
   */
  private void setComponentCountFromSyntheticEval(
      final String repositoryId,
      final String pathname,
      final int count)
  {
    try (TransactionContext tx = repositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      // Raise-only rather than unconditional stamp: ScanPolicyEvaluator occasionally persists
      // application_component rows with stage_type_id = null on re-evaluations, which makes the
      // stage-filtered directCount come back as 0. The bom.json refinement in saveReportFiles
      // already wrote the authoritative HDS count via raiseComponentCountIfHigher; preserving
      // it when the synth-eval value is smaller keeps the Hosted Repos list page in sync with
      // the drill-in Build Report (both ultimately track bom.json's aaData.length).
      repositoryComponentDAO.raiseComponentCountIfHigher(tx, repositoryId, pathname, count);
      tx.commit();
    }
    catch (Exception e) {
      log.warn("Failed to raise component_count={} from synthetic-app eval for pathname={}: {}",
          count, pathname, e.getMessage(), e);
    }
  }

  /**
   * Unconditional stamp used only by the identified-outer collapse gate. The collapse path
   * intentionally lowers component_count to 1, so it must NOT route through the raise-only
   * helper above — it would be a no-op against the prior bom.json refinement value.
   */
  private void forceComponentCount(
      final String repositoryId,
      final String pathname,
      final int count)
  {
    try (TransactionContext tx = repositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      repositoryComponentDAO.stampComponentCount(tx, repositoryId, pathname, count);
      tx.commit();
    }
    catch (Exception e) {
      log.warn("Failed to stamp component_count={} for pathname={}: {}",
          count, pathname, e.getMessage(), e);
    }
  }

  /**
   * CLM-40943: predicate used to exclude manifest-derived 'dependency:' components from the
   * count stamped onto the outer artifact. insight-scanner prefixes a pathname with
   * {@code "dependency:"} when the corresponding component was discovered via dependency-
   * manifest extraction (Gemfile.lock entry, requirements.txt entry, package-lock.json entry,
   * etc.) rather than via direct binary identification. LC's iq-cli scan does not surface
   * these as top-level components, so honoring the same convention on the hosted side keeps
   * the two views in agreement.
   * <p>
   * Returns {@code true} when the component has at least one non-{@code "dependency:"}
   * pathname — meaning the component was identified through some direct-binary path in
   * addition to (or instead of) manifest extraction. Returns {@code false} only when every
   * recorded pathname is manifest-derived.
   * <p>
   * Null / empty pathnames are treated as direct (kept) — a component with no recorded
   * pathname has no manifest evidence to filter on, so we err on the side of inclusion.
   */
  private static boolean hasDirectIdentificationPathname(final java.util.List<String> pathnames) {
    if (pathnames == null || pathnames.isEmpty()) {
      return true;
    }
    for (String p : pathnames) {
      if (p != null && !p.startsWith("dependency:")) {
        return true;
      }
    }
    return false;
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

      // CLM-41693: Emit APPLICATION_EVALUATION_COMPONENT_COUNTS telemetry for the sync enforcement
      // path so synchronous hosted scans appear alongside async-queue hosted scans in the telemetry
      // stream with scan_trigger_type=HOSTED_REPOSITORY_SCANNING. Without this, real-time enforcement
      // scans would be silently under-counted.
      sendHostedScanEvaluationTelemetry(
          scanReceipt.getScanId(), application.getId(), stage, List.of(componentInfo));
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
      // CLM-40943: extract bom.json's outer hash so policythreats.json + data.json use the same
      // hash bom.json carries for the outer entry. For npm/nuget/pub formats the file SHA1 (which
      // is what RepositoryPolicyViolation.hash stores) differs from HDS's identification hash
      // (what bom.json carries), and the LC Application Report body table joins on bom's hash.
      // Without aligning, the body shows the outer with zero violations attached even when the
      // pill header reports many. Formats whose file SHA1 already equals HDS's hash (maven, pypi,
      // rubygems, conda, helm, r) get the same hash from both sources — no-op for those.
      String bomOuterHashOverride = extractBomOuterHash(patchedBom);
      for (String fileName : List.of("policythreats.json")) {
        byte[] content = HostedReportFileBuilder.build(fileName, comp, violations, bomOuterHashOverride);
        applicationReportPersistenceService.saveReportFile(application.getId(), scanId, fileName,
            new ByteArrayInputStream(content));
      }

      // CLM-40943 — patch data.json's policyComponentCount + policyCounts to reflect the
      // rolled-up inner violations. The HDS-supplied data.json is based on HDS's own view of
      // the scan and never sees the inner-pathname violations the IQ-side RepositoryPolicyEvaluator
      // persisted under synthetic `outer!/inner.jar` paths. Without this patch the report header
      // pill reads "N VIOLATIONS Affecting 0 components" even when the threat list below shows N
      // distinct inner components. Dedups byte-identical inner jars by hash to match the
      // application-evaluation path's count (see HostedReportFileBuilder.patchDataJsonPolicyCounts).
      try {
        patchDataJsonPolicyCounts(application, scanId, comp, violations, bomOuterHashOverride);
      }
      catch (Exception ex) {
        log.warn("Failed to patch data.json policyComponentCount for scanId={}: {}",
            scanId, ex.getMessage());
      }

      // Refine component_count from HDS bom.json → aaData.length. bom.json lists every component
      // HDS found inside the artifact (the artifact itself plus all nested/bundled dependencies),
      // which is more accurate than the scanner's <dir> count that was eagerly stamped in
      // executeJob. The refinement runs through {@code raiseComponentCountIfHigher} which is an
      // atomic conditional UPDATE — the "only raise, never lower" check is enforced at the SQL
      // level, not in app code, so there is no read-then-write window where a transient
      // smaller bom count could regress a correctly-stamped row.
      if (comp != null && patchedBom != null) {
        try {
          JsonNode bomJson = MAPPER.readTree(patchedBom);
          JsonNode aaData = bomJson.path("aaData");
          int bomCount = aaData.isArray() ? aaData.size() : 1;
          try (TransactionContext tx = repositoryComponentDAO.createTransactionContext()) {
            tx.begin();
            repositoryComponentDAO.raiseComponentCountIfHigher(
                tx, comp.getRepositoryId(), comp.getPathname(), bomCount);
            tx.commit();
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

  /**
   * Reads the HDS-supplied {@code bom.json} from the report zip, delegates to
   * {@link HostedReportFileBuilder#patchBomKeepOuterOnly} to trim {@code aaData[]} to only
   * the outer's entry, then writes the patched bytes back via the overlay persistence
   * service. Used by the identified-outer gate so the drill-in Build Report's body table
   * matches the iq-cli single-file output (e.g. devise.gem → 1 row instead of HDS's 32-row
   * Gemfile.lock-expanded view).
   * <p>
   * No-op when {@code outerHash} is null/empty or {@code bom.json} can't be located on disk
   * — fail-soft.
   */
  private void patchBomKeepOuterOnly(
      final Application application,
      final String scanId,
      final String outerHash) throws Exception
  {
    ApplicationReport report = reportDataStoreProvider.get().getApplicationReport(application, scanId);
    if (report == null) {
      return;
    }
    ReportEntry bomEntry = report.getEntry("bom.json");
    if (bomEntry == null || bomEntry.buf == null) {
      return;
    }
    // CLM-40943: match-by-content. HDS gives us TWO different hashes for the same outer
    // component depending on the format:
    // • for npm/nuget/pub and many rubygems → bom carries an HDS metadata hash that differs
    // from the file SHA1 (RepositoryComponent.hash);
    // • for maven/pypi/r/conda/helm/most rubygems → both hashes are equal.
    // policythreats.json was already written by saveReportFiles using bom's own first-entry
    // hash (via extractBomOuterHash), so to keep the bom→policythreats join intact we MUST
    // trim bom by that same first-entry hash, not by RepositoryComponent.hash (which is
    // always the file SHA1).
    String keepHash = extractBomOuterHash(bomEntry.buf);
    if (keepHash == null) {
      // Fall back to the caller-supplied hash if bom is unparseable or has no aaData.
      keepHash = outerHash;
    }
    if (keepHash == null || keepHash.isEmpty()) {
      return;
    }
    byte[] patched = HostedReportFileBuilder.patchBomKeepOuterOnly(bomEntry.buf, keepHash);
    if (patched == bomEntry.buf) {
      return;
    }
    applicationReportPersistenceService.saveReportFile(application.getId(), scanId, "bom.json",
        new ByteArrayInputStream(patched));
    log.debug("Trimmed bom.json to outer-only (hash={}) for scanId={}", keepHash, scanId);
  }

  /**
   * Reads the HDS-supplied {@code data.json} from the report zip, delegates to
   * {@link HostedReportFileBuilder#patchDataJsonTotalArtifactCount} to overwrite
   * {@code totalArtifactCount} + {@code knownArtifactCount} with the post-eval direct-
   * identification count, then writes the patched bytes back via the overlay persistence
   * service. Keeps the drill-in Build Report header in agreement with the Hosted Repos list
   * COMPONENTS column for the same outer artifact.
   * <p>
   * No-op when {@code directCount} is negative or {@code data.json} can't be located on disk
   * (the report stays with HDS's original numbers — fail-soft).
   */
  private void patchDataJsonTotalArtifactCount(
      final Application application,
      final String scanId,
      final int directCount) throws Exception
  {
    if (directCount < 0) {
      return;
    }
    ApplicationReport report = reportDataStoreProvider.get().getApplicationReport(application, scanId);
    if (report == null) {
      return;
    }
    ReportEntry dataEntry = report.getEntry("data.json");
    if (dataEntry == null || dataEntry.buf == null) {
      return;
    }
    byte[] patched = HostedReportFileBuilder.patchDataJsonTotalArtifactCount(dataEntry.buf, directCount);
    if (patched == dataEntry.buf) {
      return;
    }
    applicationReportPersistenceService.saveReportFile(application.getId(), scanId, "data.json",
        new ByteArrayInputStream(patched));
    log.debug("Patched data.json.totalArtifactCount={} for scanId={}", directCount, scanId);
  }

  /**
   * Reads the HDS-supplied {@code data.json} from the report zip, delegates to
   * {@link HostedReportFileBuilder#patchDataJsonPolicyCounts} to recompute
   * {@code policyComponentCount} and {@code policyCounts[]} from the rolled-up
   * {@code RepositoryPolicyViolation} rows, then writes the patched bytes back via the overlay
   * persistence service. Mirrors what {@code ScanPolicyEvaluator.updateDataJson} does for the
   * application-evaluation path — see the helper's Javadoc for the algorithm.
   * <p>
   * No-op when {@code violations} is empty (nothing to roll up) or when {@code data.json} can't
   * be located on disk (the report stays with HDS's original numbers — fail-soft).
   */
  private void patchDataJsonPolicyCounts(
      final Application application,
      final String scanId,
      final RepositoryComponent outerComponent,
      final List<RepositoryPolicyViolation> violations) throws Exception
  {
    patchDataJsonPolicyCounts(application, scanId, outerComponent, violations, null);
  }

  private void patchDataJsonPolicyCounts(
      final Application application,
      final String scanId,
      final RepositoryComponent outerComponent,
      final List<RepositoryPolicyViolation> violations,
      final String outerHashOverride) throws Exception
  {
    if (violations == null || violations.isEmpty()) {
      return;
    }
    ApplicationReport report = reportDataStoreProvider.get().getApplicationReport(application, scanId);
    if (report == null) {
      return;
    }
    ReportEntry dataEntry = report.getEntry("data.json");
    if (dataEntry == null || dataEntry.buf == null) {
      return;
    }
    byte[] patched = HostedReportFileBuilder.patchDataJsonPolicyCounts(
        dataEntry.buf, outerComponent, violations, outerHashOverride);
    if (patched == dataEntry.buf) {
      return;
    }
    applicationReportPersistenceService.saveReportFile(application.getId(), scanId, "data.json",
        new ByteArrayInputStream(patched));
    log.debug("Patched data.json policy counts for scanId={}", scanId);
  }

  /**
   * CLM-40943: extract the outer artifact's hash from bom.json's first {@code aaData[]} entry.
   * That hash is HDS's identification hash for the outer component, which for npm/nuget/pub
   * formats differs from the file SHA1 stored on {@code repository_policy_violation.hash}. We
   * thread it through to {@link HostedReportFileBuilder} so the synthesised
   * {@code policythreats.json} (and the patched {@code data.json} policy counts) carry the
   * same hash bom.json carries — that's the key the LC Application Report body joins on.
   * <p>
   * Returns {@code null} when bom is null/unparseable or has no {@code aaData[0].hash} —
   * fail-soft so the call site falls back to today's behaviour (use
   * {@code RepositoryPolicyViolation.hash}) for any format we haven't accounted for.
   */
  private static String extractBomOuterHash(final byte[] patchedBom) {
    if (patchedBom == null || patchedBom.length == 0) {
      return null;
    }
    try {
      JsonNode bomJson = MAPPER.readTree(patchedBom);
      JsonNode aaData = bomJson.path("aaData");
      if (!aaData.isArray() || aaData.isEmpty()) {
        return null;
      }
      String hash = aaData.get(0).path("hash").asText("");
      return hash.isEmpty() ? null : hash;
    }
    catch (Exception e) {
      return null;
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

  /**
   * Emits {@code APPLICATION_EVALUATION_COMPONENT_COUNTS} telemetry with
   * {@code scan_trigger_type=HOSTED_REPOSITORY_SCANNING} for hosted repository scans.
   * <p>
   * Hosted repo scans go through {@link RepositoryPolicyEvaluator} (not {@code ScanPolicyEvaluator}),
   * so this event must be emitted explicitly from this path to satisfy CLM-41693: enabling
   * telemetry consumers to distinguish hosted repository scans from other trigger types
   * (CLI, IDE, WEB_UI, etc.) using a single telemetry event.
   */
  /**
   * Sentinel for missing {@code format()} in scan component infos. Matches
   * {@code ScanPolicyEvaluator.UNKNOWN} so the resulting {@code number_of_unknown_components}
   * telemetry attribute key is identical across hosted-repo and regular scan paths — telemetry
   * consumers aggregating across scan types must not split on a sentinel-case difference.
   */
  private static final String TELEMETRY_UNKNOWN_FORMAT = "unknown";

  @VisibleForTesting
  void sendHostedScanEvaluationTelemetry(
      final String scanId,
      final String applicationId,
      final String stage,
      final List<ScanComponentInfo> componentInfos)
  {
    try {
      Map<String, Long> componentCounts = componentInfos.stream()
          .map(info -> info.format() != null ? info.format() : TELEMETRY_UNKNOWN_FORMAT)
          .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
      TelemetryData telemetryData = telemetryUtils.buildApplicationEvaluationTelemetryData(
          scanId, applicationId, stage.toLowerCase(),
          ScanTriggerType.HOSTED_REPOSITORY_SCANNING,
          null, null,
          Collections.singletonMap("component_counts", componentCounts));
      telemetrySender.send(telemetryData);
      log.debug("Sent APPLICATION_EVALUATION_COMPONENT_COUNTS telemetry for hosted scan: "
          + "scanId={}, appId={}, scan_trigger_type=HOSTED_REPOSITORY_SCANNING", scanId, applicationId);
    }
    catch (Exception e) {
      // Telemetry is auxiliary; swallow exceptions so a telemetry failure cannot abort a hosted
      // repo scan. This intentionally differs from ScanPolicyEvaluator.sendEvaluationTelemetry,
      // which propagates failures — that path runs synchronously inside a request lifecycle where
      // a propagated failure is recoverable by the caller. Hosted scans are async/queue-driven
      // and a failure here would mark the whole scan job FAILED and burn retries on a telemetry
      // hiccup (HDS blip, queue full) that has nothing to do with the scan itself. See CLM-41693.
      log.warn("Failed to send hosted scan evaluation telemetry scanId={} appId={}: {}",
          scanId, applicationId, e.getMessage(), e);
    }
  }

  private void createPolicyEvaluationRecord(final String appId, final String scanId, final String stageTypeId) {
    try (TransactionContext tx = policyEvaluationDAO.createTransactionContext()) {
      tx.begin();
      if (policyEvaluationDAO.getLastByApplicationIdAndScanId(tx, appId, scanId) == null) {
        PolicyEvaluation pe = new PolicyEvaluation(
            appId, stageTypeId.toLowerCase(), scanId, false, false, "system",
            ScanTriggerType.HOSTED_REPOSITORY_SCANNING, null);
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
   * Drive nested-component policy violations via {@link ScanPolicyEvaluator} (the Lifecycle
   * "Evaluate a binary" path) on the synthetic application IQ already created for this hosted
   * upload, then mirror the resulting {@code policy_violation} rows into
   * {@code repository_policy_violation} so the existing repository-side UI, queries, and
   * report-building code paths see per-inner findings.
   * <p>
   * <b>Why:</b> the repository-evaluation pipeline (called by {@link #evaluatePolicies}) only
   * writes a single {@code repository_policy_violation} row for the outer artifact, because the
   * scanner emits a single {@code
   *
  <dir>
   * } for archives whose container format it cannot crack
   * natively (npm .tgz, pypi sdist/wheel, helm charts, go module zips, etc.) and the HDS
   * "firewall" purpose returns only outer-scope match data. The Lifecycle-evaluation pipeline
   * already does the right thing for the exact same scan — it runs full Drools policy
   * evaluation against every component HDS identified in bom.json (outer + every nested),
   * producing one {@code policy_violation} row per (component × policy). Hosted-repo doesn't
   * normally invoke that path because its persistence boundary is
   * {@code repository_policy_violation}, not {@code policy_violation}.
   * <p>
   * <b>What this does:</b>
   * <ol>
   * <li>Invoke {@link ScanPolicyEvaluator#evaluate} on the synthetic application — same code
   * path "Evaluate a binary" uses, runs all the Drools magic, populates
   * {@code application_component} and {@code policy_violation}.</li>
   * <li>Read back the {@code policy_violation} rows by (application_id, stage_type_id).</li>
   * <li>For each row whose component hash is NOT the outer's hash (the outer is already
   * handled by {@link #evaluatePolicies} above; double-writing would create duplicate
   * {@code repository_policy_violation} rows under different pathnames), build a synthetic
   * {@code outer!/coords} pathname and insert into {@code repository_policy_violation} with
   * the policy fields copied verbatim — same policyId, policyName, threatLevel,
   * threatCategory, hash, componentIdentifier, constraintFacts.</li>
   * </ol>
   * <p>
   * <b>Format-agnostic:</b> whatever HDS identifies inside the outer (npm, pypi, helm, nuget,
   * rubygems, go, ...) gets full CVE/policy treatment for free. No per-format scaffolding
   * required on our side — that lives entirely in HDS.
   * <p>
   * <b>Failure mode:</b> non-fatal. If the Drools eval fails or no inner rows are produced
   * (HDS didn't identify nested components for this format), the outer-only behaviour from
   * {@link #evaluatePolicies} is unaffected — we lose only the inner-component breakout.
   * Recoverable on the next CM sweep.
   */
  private void mirrorNestedComponentViolationsFromApplicationEvaluation(
      final HostedComponentScanQueue job,
      final String repositoryId,
      final ScanComponentInfo outer,
      final Application application,
      final String scanId,
      final String stage)
  {
    if (scanId == null) {
      return;
    }
    try {
      // CLM-40943: Identified-outer gate (per Ross @ Slack 2026-06-26).
      //
      // When HDS identified the outer artifact (the user uploaded a known component like
      // devise-4.4.0.gem, lodash-4.17.21.tgz, jackson-databind-2.16.1.jar from a public
      // registry), the hosted-repo scan must mirror what `iq-cli <single-file>` produces for
      // that same binary: one component (the outer), the outer's own violations, no drill-
      // down into the archive's payload. HDS's `policy-evaluation` purpose for an identified
      // outer would otherwise expand the archive's full dependency tree — manifest entries
      // (Gemfile.lock, requirements.txt), declared transitive gems, etc. — and Drools would
      // produce one policy_violation row per (component × policy) for the entire tree, then
      // we'd mirror them into repository_policy_violation and the drill-in Build Report
      // would show "178 VIOLATIONS Affecting 32 components" for what iq-cli reports as
      // "4 violations, 1 component".
      //
      // RepositoryPolicyEvaluator (run earlier in executeJob) already populated
      // repository_component.match_state_id with HDS's identification verdict for the outer.
      // We read it here without re-running the evaluator. UNKNOWN means HDS couldn't
      // identify the outer (a proprietary archive, a maven fat-jar of jars, an internal
      // package) — in that case drill-down is the right behaviour: invoke
      // ScanPolicyEvaluator.evaluate() and mirror as before. Anything else (EXACT, SIMILAR,
      // EMBEDDED) means the outer is a known component; emit one row, outer's own
      // violations only, no drill-down.
      //
      // The two not-yet-confirmed cases (matchStateId UNKNOWN at outer but
      // ScanPolicyEvaluator might find inner identified components, vs. matchStateId not-
      // UNKNOWN at outer but the outer contains further unknown wrappers around known
      // components) are handled symmetrically: drill or skip purely on the outer's verdict.
      // Ross retracted the deeper "stop at first known component" rule pending HDS-side
      // discussion (Slack 2026-06-26 22:29), so we keep this simple one-level decision.
      RepositoryComponent outerRow =
          repositoryComponentDAO.getByRepositoryIdAndPathname(repositoryId, outer.pathname());
      boolean outerIdentified = outerRow != null
          && outerRow.getMatchStateId() != null
          && !MatchState.UNKNOWN.getId().equalsIgnoreCase(outerRow.getMatchStateId());

      // CLM-40943 — format carveout: nuget (and any future format with nuget-like nested
      // binaries) MUST NOT collapse. iq-cli on system.text.json.9.0.0.nupkg reports 21
      // components (framework-fanout DLLs that physically ship inside the .nupkg) and the
      // hosted-repo must match that. Without this carveout the gate would over-collapse and
      // hide 20 real binaries — a regression confirmed in safety-test screenshots
      // 2026-06-27. See KEEP_NESTED_FORMATS_FOR_IDENTIFIED_OUTER javadoc for the rationale.
      Repository repository = repositoryDAO.getById(repositoryId);
      String repoFormat = repository != null ? repository.getFormat() : null;
      boolean keepNestedForFormat =
          repoFormat != null && KEEP_NESTED_FORMATS_FOR_IDENTIFIED_OUTER.contains(repoFormat.toLowerCase());

      if (outerIdentified && !keepNestedForFormat) {
        log.info("Identified-outer gate: outer pathname={} matchState={} format={} → reporting as "
            + "1 component, no inner drill-down (matches iq-cli single-file scan behaviour)",
            outer.pathname(), outerRow.getMatchStateId(), repoFormat);

        // Stamp componentCount = 1. Unconditional UPDATE because the earlier eager stamp and
        // saveReportFiles' bom refinement (both via raiseComponentCountIfHigher) may have
        // raised the column to HDS's expanded count.
        forceComponentCount(repositoryId, outer.pathname(), 1);

        // Patch data.json.totalArtifactCount=1 + knownArtifactCount=1 so the drill-in Build
        // Report header reads "1 COMPONENT, 100% of all components identified" instead of
        // HDS's expanded view.
        try {
          patchDataJsonTotalArtifactCount(application, scanId, 1);
        }
        catch (Exception ex) {
          log.warn("Failed to patch data.json.totalArtifactCount=1 for identified outer scanId={}: {}",
              scanId, ex.getMessage());
        }

        // CLM-40943: trim bom.json.aaData[] to keep only the outer's entry. Without this the
        // drill-in Build Report's body table reads HDS's expanded bom (e.g. 32 rows for a
        // devise.gem) while the header and pills read the gate's "1 COMPONENT" view — an
        // internal UI inconsistency. Trimming bom.json drives every downstream consumer
        // (Application Report body, SBOM exports, Search index) to the same one-entry view
        // iq-cli already produces for the same binary.
        try {
          patchBomKeepOuterOnly(application, scanId, outer.hash());
        }
        catch (Exception ex) {
          log.warn("Failed to trim bom.json to outer for identified outer scanId={}: {}",
              scanId, ex.getMessage());
        }

        // Idempotent cleanup: delete any stale inner-pathname rows a prior run (or earlier
        // version of this code) left in repository_policy_violation. The outer's own row
        // (no "!/" in pathname) is owned by RepositoryPolicyEvaluator and stays.
        try (TransactionContext tx = repositoryPolicyViolationDAO.createTransactionContext()) {
          tx.begin();
          List<RepositoryPolicyViolation> existingForOuter = repositoryPolicyViolationDAO
              .getActiveByRepositoryIdAndPathnameOrInnerPathnames(repositoryId, outer.pathname());
          int deleted = 0;
          for (RepositoryPolicyViolation existing : existingForOuter) {
            String existingPathname = existing.getPathname();
            if (existingPathname != null && existingPathname.contains("!/")) {
              repositoryPolicyViolationDAO.delete(tx, existing);
              deleted++;
            }
          }
          tx.commit();
          if (deleted > 0) {
            log.info("Identified-outer gate cleanup: deleted {} stale inner-pathname rows for "
                + "outer pathname={}", deleted, outer.pathname());
          }
        }
        return;
      }

      // Step 1: run full Drools policy evaluation through the Lifecycle pipeline on the
      // synthetic application. This reads bom.json / security.json / licenses.json from the
      // already-downloaded report.zip and produces one policy_violation row per
      // (component × policy) — including for the inner components HDS identified. The return
      // value is intentionally unused; we read the persisted rows back via the DAO in step 2
      // so the same code path works for both first-time and re-evaluation runs.
      Stage policyStage = new Stage(stage.toLowerCase());
      scanPolicyEvaluatorProvider.get()
          .evaluate(application, scanId, policyStage,
              ScanTriggerType.REPOSITORY_MANAGER,
              ClientScanType.SONATYPE,
              false /* skipAutoWaivers */);

      // CLM-40943: stamp component_count from the synthetic application's authoritative view.
      // After ScanPolicyEvaluator.evaluate() returns, application_component holds one row per
      // distinct component HDS identified for this scan (outer + every inner). That count is
      // the same number LC's application report shows for the same scan, so it is the source
      // of truth — overwriting both the earlier scanner-based eager stamp (executeJob:307)
      // and the HDS-bom refinement in saveReportFiles. We use the DAO's stampComponentCount
      // (unconditional UPDATE) rather than raiseComponentCountIfHigher because the prior
      // stamps can be both higher (scanner's file-list view for .gem, .tar.gz, etc.) and
      // lower (early HDS firewall-purpose bom for nuget) than the truth.
      List<ApplicationComponent> appComponents =
          applicationComponentDAO.getByApplicationIdAndStageTypeId(application.getId(), stage.toLowerCase());

      // CLM-40943: align with LC's count by excluding components identified solely via
      // manifest extraction (Gemfile.lock, requirements.txt, package-lock.json, etc.). The
      // insight-scanner library tags each pathname with a "dependency:" prefix when the entry
      // was derived from a manifest file inside an archive payload, vs. no prefix when the
      // component was binary-identified directly. LC's iq-cli scan does not promote manifest-
      // derived entries to top-level components, so honoring the same convention here keeps
      // the hosted-side row's component_count in agreement with the LC application report
      // for the same artifact. A component with at least one direct-identification pathname
      // is kept; a component whose every pathname is "dependency:..." is excluded.
      // CLM-40943 follow-up: for formats where the dependency graph IS the source of truth
      // (currently: go — go.mod-derived transitives are real components per LC), keep every
      // application_component including manifest-derived ones. For other formats, exclude
      // dependency-only entries to match iq-cli's "physical contents only" view.
      boolean keepDependencyDerived =
          repoFormat != null && KEEP_DEPENDENCY_DERIVED_COMPONENTS_FORMATS.contains(repoFormat.toLowerCase());
      int directCount = keepDependencyDerived
          ? appComponents.size()
          : (int) appComponents.stream()
              .filter(ac -> hasDirectIdentificationPathname(ac.getPathnames()))
              .count();
      int dependencyDerived = appComponents.size() - directCount;
      if (dependencyDerived > 0) {
        log.info("Excluded {} manifest-derived 'dependency:' components from componentCount stamp "
            + "for pathname={} (kept {} direct-identification components)",
            dependencyDerived, outer.pathname(), directCount);
      }
      setComponentCountFromSyntheticEval(repositoryId, outer.pathname(), directCount);

      // CLM-40943: keep the Build Report header in sync with the Hosted Repos list COMPONENTS
      // column. data.json.totalArtifactCount drives the "X COMPONENTS, 100% of all components
      // identified" pill at the top of the drill-in report; without this patch HDS's value (the
      // full bom expansion, including manifest-derived entries) leaks through and the header
      // disagrees with the outer's componentCount we just stamped above. Re-uses the same
      // directCount → both views end up showing the identical number for the same artifact.
      try {
        patchDataJsonTotalArtifactCount(application, scanId, directCount);
      }
      catch (Exception ex) {
        log.warn("Failed to patch data.json.totalArtifactCount={} for scanId={}: {}",
            directCount, scanId, ex.getMessage());
      }

      // Step 2: read back the policy_violation rows the evaluator just wrote, and explicitly
      // load their constraint_facts. PolicyViolationDAO returns rows with constraintFacts
      // unloaded (lazy by default for perf — most callers don't need them); calling
      // getConstraintFacts() on an unloaded row throws IllegalStateException with a message
      // that names the DAO method to fix it. We DO need them: the RepositoryPolicyViolation
      // constructor requires non-null/non-empty constraintFacts (see AbstractPolicyViolation
      // line ~173) because that's the source of truth for which CVE / license rule actually
      // matched. Loading them in one batch call is the standard pattern (see
      // ScanPolicyEvaluator's own use at lines 590, 653, 772, 1154, 1987).
      List<PolicyViolation> violations =
          policyViolationDAO.getActiveByApplicationIdAndStageId(application.getId(), stage.toLowerCase());
      if (violations.isEmpty()) {
        log.debug("ScanPolicyEvaluator produced no policy_violation rows for job id={}, app={}, scan={} — "
            + "no inner-component violations to mirror", job.getId(), application.getId(), scanId);
        return;
      }
      policyViolationDAO.loadConstraintFacts(violations);

      // Step 3: build a hash → ApplicationComponent map so we can resolve each violation's
      // pathnames (= file paths inside the outer archive). The bom-derived pathnames are the
      // truthful "where inside the outer" — preserve them in the mirrored row's pathname so
      // downstream UI/audit can render exact locations. Re-using appComponents from above.
      Map<String, ApplicationComponent> componentByHash = new HashMap<>();
      for (ApplicationComponent ac : appComponents) {
        componentByHash.put(ac.getHash(), ac);
      }

      // Step 4: mirror only INNER violations. Skip the outer (already persisted by
      // evaluatePolicies; double-write would create a second repository_policy_violation row
      // under a different pathname with the same coordinates, breaking dedup downstream).
      String outerHash = outer.hash();
      Date now = new Date();
      int mirrored = 0;
      try (TransactionContext tx = repositoryPolicyViolationDAO.createTransactionContext()) {
        tx.begin();

        // Idempotent re-mirror: delete the inner-pathname rows we previously wrote for this
        // outer before inserting the fresh batch. Without this, a re-evaluation (or a partial
        // mirror retry) leaves stale rows in repository_policy_violation that either inflate
        // counts (re-eval doubles inner pathnames) or under-report (a prior partial mirror
        // left only some inners persisted). The component-list summary pill sums raw rows
        // without hash-dedup, so the discrepancy surfaces there even when the drill-in report
        // (which dedups via HostedReportFileBuilder.patchDataJsonPolicyCounts) reads correctly.
        //
        // We only delete rows whose pathname contains the "!/" separator under this outer —
        // the outer's own row (no separator) is owned by RepositoryPolicyEvaluator and must
        // not be touched here. The existing
        // getActiveByRepositoryIdAndPathnameOrInnerPathnames helper returns the outer row +
        // all inners under it; we filter to inners-only before deleting.
        List<RepositoryPolicyViolation> existingForOuter = repositoryPolicyViolationDAO
            .getActiveByRepositoryIdAndPathnameOrInnerPathnames(repositoryId, outer.pathname());
        int deletedStale = 0;
        for (RepositoryPolicyViolation existing : existingForOuter) {
          String existingPathname = existing.getPathname();
          if (existingPathname != null && existingPathname.contains("!/")) {
            repositoryPolicyViolationDAO.delete(tx, existing);
            deletedStale++;
          }
        }
        if (deletedStale > 0) {
          log.debug("Idempotent re-mirror: deleted {} stale inner-pathname rows for job id={}, "
              + "outer pathname={}", deletedStale, job.getId(), outer.pathname());
        }

        int skippedDependencyDerived = 0;
        for (PolicyViolation pv : violations) {
          if (pv.getHash() != null && pv.getHash().equals(outerHash)) {
            // Outer-hash violations are already in repository_policy_violation under the
            // real outer pathname.
            continue;
          }
          ApplicationComponent ac = componentByHash.get(pv.getHash());
          // CLM-40943: skip mirroring violations whose component was discovered solely via
          // dependency-manifest extraction (Gemfile.lock entry, requirements.txt entry,
          // package-lock.json entry, etc. — insight-scanner tags those pathnames with a
          // "dependency:" prefix). Ross's guidance: hosted-repo scan should report only what
          // is physically present inside the artifact, not what its manifest files declare as
          // dependencies. Mirroring these inflates the outer's pill counts and drill-in
          // violations list with declared-but-not-present components (e.g. devise.gem's
          // Gemfile.lock listing activerecord/railties). A component with at least one direct-
          // identification pathname is kept; one whose every pathname is "dependency:..." is
          // excluded. Symmetrical to the dependency: filter applied to componentCount above.
          //
          // 2026-06-27 follow-up: bypass the filter for formats listed in
          // KEEP_DEPENDENCY_DERIVED_COMPONENTS_FORMATS (currently: go). For Go module zips
          // the go.mod-declared transitives ARE the real component graph LC reports — keeping
          // them in agreement with the LC application Evaluate File view.
          if (ac != null && !keepDependencyDerived && !hasDirectIdentificationPathname(ac.getPathnames())) {
            skippedDependencyDerived++;
            continue;
          }
          // Compose the synthetic pathname: outer + "!/" + inner-coordinates-label.
          // displayName / coordinates label is the most readable choice; ApplicationComponent
          // carries the full pathnames list (newline-separated text) but we don't need every
          // path — one canonical label per inner component is enough for the UI and matches
          // what the existing policythreats.json builder expects.
          String innerLabel = innerLabelFromComponent(pv, ac);
          String innerPathname = outer.pathname() + "!/" + innerLabel;

          RepositoryPolicyViolation rpv = new RepositoryPolicyViolation(
              repositoryId,
              innerPathname,
              now,
              pv.getPolicyId(),
              pv.getPolicyName(),
              pv.getThreatLevel(),
              pv.getThreatCategory(),
              pv.getHash(),
              pv.getComponentIdentifier(),
              pv.getConstraintFacts());
          rpv.setId(UUID.randomUUID().toString().replace("-", ""));
          if (pv.getActionTypeId() != null) {
            rpv.setActionTypeId(pv.getActionTypeId());
          }
          if (pv.getPolicyWaiverId() != null) {
            rpv.setWaived(true);
            rpv.setPolicyWaiverId(pv.getPolicyWaiverId());
            rpv.setPolicyWaiverComment(pv.getPolicyWaiverComment());
            rpv.setWaiveTime(pv.getWaiveTime());
          }
          // Stamp the NXRM componentId so component-keyed UI features (waivers, quarantine
          // history) match the inner rows the same way they match the outer.
          if (job.getComponentId() != null) {
            rpv.setComponentId(job.getComponentId());
          }
          repositoryPolicyViolationDAO.insert(tx, rpv);
          mirrored++;
        }
        tx.commit();
        if (skippedDependencyDerived > 0) {
          log.info("Excluded {} manifest-derived 'dependency:' policy_violation rows from "
              + "repository_policy_violation mirror for job id={}, app={}, scan={}",
              skippedDependencyDerived, job.getId(), application.getId(), scanId);
        }
      }
      log.info("Mirrored {} inner-component policy_violation rows into repository_policy_violation "
          + "for job id={}, app={}, scan={} (total app-side violations: {}, outer-hash filtered)",
          mirrored, job.getId(), application.getId(), scanId, violations.size());
    }
    catch (Exception e) {
      log.warn("Nested-component mirror failed for job id={} (outer eval already persisted): {}",
          job.getId(), e.getMessage(), e);
    }
  }

  /**
   * Produce a stable, human-readable inner-component label for the synthetic pathname
   * {@code outer + "!/" + label}. Preference order:
   * <ol>
   * <li>{@code componentIdentifier} → format-coordinates-derived label (e.g. {@code form-data@2.3.3})</li>
   * <li>{@code ApplicationComponent.pathnames} → first non-blank entry (the truthful nested path)</li>
   * <li>{@code hash} → opaque fallback</li>
   * </ol>
   */
  private static String innerLabelFromComponent(
      final PolicyViolation violation,
      final ApplicationComponent component)
  {
    ComponentIdentifier ci = violation.getComponentIdentifier();
    if (ci != null && ci.getCoordinates() != null && !ci.getCoordinates().isEmpty()) {
      // Best label: format-aware coordinate composition. The exact shape doesn't matter for
      // correctness (the "!/" separator already disambiguates) but a readable label improves
      // every downstream UI that shows the pathname.
      String packageId = ci.getCoordinates().get("packageId");
      String artifactId = ci.getCoordinates().get("artifactId");
      String name = packageId != null ? packageId : artifactId;
      String version = ci.getCoordinates().get("version");
      if (name != null && version != null) {
        return name + "@" + version;
      }
      if (name != null) {
        return name;
      }
    }
    if (component != null && component.getPathnames() != null && !component.getPathnames().isEmpty()) {
      // application_component.pathnames is the list of file paths inside the outer; the first
      // one is the most-specific evidence of where this inner lives.
      String first = component.getPathnames().get(0);
      if (first != null && !first.isBlank()) {
        return first.trim();
      }
    }
    return violation.getHash() != null ? violation.getHash() : "unknown";
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
