/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.io.ByteArrayInputStream;
import java.nio.file.FileAlreadyExistsException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.HostedComponentScanQueueDAO;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.ProxyRepositoryPolicyViolationDAO;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.report.LifecycleReport;
import com.sonatype.insight.brain.report.LifecycleReportPersistenceService;
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
   * hosted gate stamps {@code componentCount = 1} while {@code proxy_repository_policy_violation}
   * still holds the inner rows — producing an internal inconsistency where the header pill
   * reads "1 COMPONENT" but the body table shows 5 rows.
   * <p>
   * <b>Pypi</b> source distributions ship {@code setup.py}/{@code pyproject.toml} plus example
   * scripts that declare transitive dependencies (Flask 3.0.0's tarball carries
   * {@code examples/celery/requirements.txt} etc.). For {@code Flask 3.0.0.tar.gz} the LC
   * application Evaluate File path surfaces 91 components (Flask + Werkzeug + Jinja2 + click +
   * 87 transitives) with real security violations; without this carveout the hosted gate
   * collapses to 1 component with only Flask's own 2 Security-Medium violations, hiding CVEs
   * on Werkzeug, Jinja2, click, etc. Same rationale as go/pub — the manifest inside the
   * archive is what iq-cli/LC uses as the identification source, not a "drop these" hint.
   * <p>
   * <b>Npm</b>: HDS identifies inner components for {@code .tgz} packages via the manifest
   * inside the archive (the {@code package.json} declares dependencies that HDS resolves).
   * For npm tarballs where nested identification lands (npm confirmed via HDS per CLM-40943
   * comment thread), the collapse-to-1 gate would hide those inner findings; keeping npm in
   * this set preserves them so the hosted-repo report stays aligned with the LC application
   * report for the same {@code .tgz}. Npm packages where HDS returns no nested components
   * still collapse naturally because {@code effectiveComponentCount == 1}.
   * <p>
   * Formats NOT in this set fall under the collapse path: identified outer → 1 component,
   * outer's own violations. This matches iq-cli for rubygems gem (whose Gemfile.lock-derived
   * entries iq-cli drops), maven jar, r tarball.
   * <p>
   * Default for any new/unknown format is the SAFER "collapse" path. If a future format
   * (cargo, yum, docker) turns out to have nuget-like nested binaries, add it here. The set
   * is small and explicit on purpose — easier to audit than the inverse.
   */
  private static final Set<String> KEEP_NESTED_FORMATS_FOR_IDENTIFIED_OUTER =
      Set.of("nuget", "go", "pub", "npm", "pypi");

  /**
   * CLM-40943 follow-up (2026-06-27): formats whose dependency graph is itself the source of
   * truth — i.e. the manifest inside the archive (go.mod for go, pubspec.yaml for pub) is
   * what LC's iq-cli / Evaluate File path uses to identify transitive components, not a
   * "drop these" hint. For these formats the {@code dependency:}-prefixed pathname filter is
   * bypassed so the mirrored {@code proxy_repository_policy_violation} rows and the stamped
   * {@code component_count} match the LC application report.
   * <p>
   * Default for any format NOT in this set: the filter applies (manifest-derived entries are
   * treated as scanner noise, matching the original CLM-40943 design for rubygems / pypi /
   * npm where manifest lockfiles inflate the count past what iq-cli reports).
   */
  private static final Set<String> KEEP_DEPENDENCY_DERIVED_COMPONENTS_FORMATS = Set.of("go", "pub");

  /**
   * CLM-42119: formats whose iq-cli scanner treats the outer artifact as opaque regardless
   * of HDS's identification verdict. For these formats hosted-repo must ALSO collapse to a
   * single outer-component view even when HDS returned MatchState.UNKNOWN — otherwise a
   * Continuous-Monitoring scan of a custom archive drills into HDS's expanded view (vendored
   * gems, manifest-derived transitives) while the same file scanned by iq-cli surfaces
   * exactly one row.
   * <p>
   * Format-side rationale:
   * <ul>
   * <li><b>rubygems</b>: iq-cli does not unpack {@code .gem} archives. Vendored gems inside
   * ({@code vendor/cache/*.gem}) and {@code Gemfile.lock}-declared transitives are
   * invisible to iq-cli's single-file scan. A CM scan of {@code bundled-gem-app-1.0.0.gem}
   * that HDS didn't recognize would otherwise report 5 components / risk 656 while an
   * equivalent LC app scan of the same file reports 1 component / risk 2.</li>
   * </ul>
   * <p>
   * When a format is in this set the identified-outer collapse gate fires unconditionally,
   * bypassing the drill-down path for UNKNOWN outers. Symmetric to
   * {@link #KEEP_NESTED_FORMATS_FOR_IDENTIFIED_OUTER} which does the opposite: forces
   * drill-down even for identified outers where iq-cli DOES surface inners (nuget, npm, ...).
   */
  private static final Set<String> ALWAYS_COLLAPSE_TO_OUTER_FORMATS = Set.of("rubygems");

  private final ApiConfigurationService apiConfigurationService;

  private final HostedComponentScanQueueDAO scanQueueDAO;

  private final Provider<ScanPersistenceService> scanPersistenceServiceProvider;

  private final Provider<ScanUploader> scanUploaderProvider;

  private final RepositoryDAO repositoryDAO;

  private final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  private final ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO;

  private final Provider<RepositoryPolicyEvaluator> repositoryPolicyEvaluatorProvider;

  private final ApplicationForHostedRepositoryComponentService applicationForHostedComponentService;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final PolicyDAO policyDAO;

  private final Provider<ReportDataStore> reportDataStoreProvider;

  private final LifecycleReportPersistenceService lifecycleReportPersistenceService;

  // CLM-40943: dependencies for the bom-driven nested-component mirror.
  private final Provider<ScanPolicyEvaluator> scanPolicyEvaluatorProvider;

  private final PolicyViolationDAO policyViolationDAO;

  private final OwnerComponentDAO applicationComponentDAO;

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
      final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO,
      final ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO,
      final Provider<RepositoryPolicyEvaluator> repositoryPolicyEvaluatorProvider,
      final ApplicationForHostedRepositoryComponentService applicationForHostedComponentService,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final PolicyDAO policyDAO,
      final Provider<ReportDataStore> reportDataStoreProvider,
      final LifecycleReportPersistenceService lifecycleReportPersistenceService,
      final Provider<ScanPolicyEvaluator> scanPolicyEvaluatorProvider,
      final PolicyViolationDAO policyViolationDAO,
      final OwnerComponentDAO applicationComponentDAO,
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
    this.proxyRepositoryComponentDAO = proxyRepositoryComponentDAO;
    this.proxyRepositoryPolicyViolationDAO = proxyRepositoryPolicyViolationDAO;
    this.repositoryPolicyEvaluatorProvider = repositoryPolicyEvaluatorProvider;
    this.applicationForHostedComponentService = applicationForHostedComponentService;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.policyDAO = policyDAO;
    this.reportDataStoreProvider = reportDataStoreProvider;
    this.lifecycleReportPersistenceService = lifecycleReportPersistenceService;
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

    // A queued job can outlive its repo's monitoring being disabled or the repo being deleted; drop it
    // rather than scan a stale repo (CLM-42122).
    Repository repository = repositoryDAO.getById(repositoryId);
    if (repository == null) {
      log.info("Hosted component scan: repository {} no longer exists; dropping scan job id={}.",
          repositoryId, job.getId());
      return;
    }
    if (!repository.isMonitoringEnabled()) {
      log.info("Hosted component scan: repository {} monitoring disabled; dropping scan job id={}.",
          repositoryId, job.getId());
      return;
    }

    ScanEntity scanEntity = scanPersistenceServiceProvider.get().getScanByName(repositoryId, job.getScanFileId());
    if (scanEntity == null) {
      throw new IllegalStateException(
          "Scan file not found: repositoryId=" + repositoryId + ", scanFileId=" + job.getScanFileId());
    }

    // The scanner emits one <dir> per archive it recognised. For a single-jar upload that's one
    // entry; for an archive-of-archives upload (e.g. a .zip containing multiple .jar files) it's
    // one entry per inner artifact. The first <dir> is always the outer artifact (the thing the
    // user uploaded); any subsequent <dir>s are inner artifacts the scanner discovered inside it.
    // We process the outer artifact as the single proxy_repository_component row (so the Components page
    // shows one row per uploaded artifact) but pass ALL components — outer + inners — into the
    // policy evaluator so violations get persisted for every inner pathname too. Inner-pathname
    // proxy_repository_component rows are deleted post-eval so the Components page stays clean; the
    // inner-pathname proxy_repository_policy_violation rows survive and feed the synthesised
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

    // CLM-42079: NXRM's scan-queue payload sends stage in inconsistent casing/format —
    // "RELEASE", "release", "BUILD", "STAGE_RELEASE" (underscore), and occasionally NULL.
    // IQ's canonical stage IDs are all-lowercase with hyphens ("release", "build",
    // "stage-release"). A raw toLowerCase() lets "RELEASE"/"release" through unchanged but
    // leaves "STAGE_RELEASE" as "stage_release" (INVALID — hyphen, not underscore) which
    // silently mis-routes policy evaluation. NULL falls back to compliance stage today, which
    // masks NXRM configuration issues in telemetry (Dariush 2026-07-01: 29,952 stage-release
    // vs 124 build in production despite build being the configured stage). normalizeStage()
    // canonicalizes the value and logs at WARN when the fallback fires so ops can spot the
    // NXRM-side gap.
    String stage = normalizeStage(job.getPolicyEvaluationStage(), job.getId());

    if (componentInfos.isEmpty()) {
      // No usable <dir> in the scan file. Still upload via the repository pipeline so HDS has the
      // raw scan record (matches today's behaviour for non-archive scans that fail to parse), then
      // bail — no application, no evaluation, no report.
      ScanReceipt scanReceipt = scanUploaderProvider.get()
          .upload(scanEntity, repository, stage, null, null, true);
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
      scanReceipt = scanUploaderProvider.get().upload(scanEntity, repository, stage, null, null, true);
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
    // one proxy_repository_component row + one batch of proxy_repository_policy_violation rows per request
    // entry. We delete the inner proxy_repository_component rows immediately afterwards (see below) so
    // the Components page only ever shows the outer artifact.
    evaluatePolicies(job, repositoryId, componentInfos, stage);

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
    // proxy_repository_policy_violation row for the outer artifact, because the scanner emits a
    // single <dir> for archives whose container format it cannot crack natively (npm .tgz,
    // pypi sdist/wheel, helm charts, go module zips, etc.) and HDS's firewall purpose returns
    // only outer-scope match data.
    //
    // The Lifecycle-evaluation pipeline already does the right thing for the same scan: it
    // runs full Drools policy evaluation against every component HDS identified in bom.json
    // (outer + every nested), producing one policy_violation row per (component × policy).
    // Hosted-repo doesn't normally invoke that path because its persistence boundary is
    // proxy_repository_policy_violation, not policy_violation.
    //
    // Solution: invoke ScanPolicyEvaluator on the synthetic application IQ already created
    // for this hosted upload — same Drools logic that runs for "Evaluate a binary" in the UI —
    // then mirror each resulting policy_violation row into proxy_repository_policy_violation,
    // skipping the row that corresponds to the outer (evaluatePolicies already handled that
    // and the existing data is the source of truth for quarantine + firewall behaviour).
    //
    // Format-agnostic: any format HDS identifies nested components for (npm confirmed; pypi,
    // helm, go, nuget, rubygems pending HDS support per format) gets per-inner violations.
    // Must run AFTER persistApplicationLinkedReportFiles so report.zip is on disk —
    // ScanPolicyEvaluator reads bom.json/security.json/licenses.json from it.
    if (application != null && scanReceipt != null) {
      mirrorNestedComponentViolationsFromApplicationEvaluation(
          job.getId(),
          repositoryId,
          outerComponentInfo.pathname(),
          outerComponentInfo.hash(),
          application,
          scanReceipt.getScanId(),
          stage,
          job.getComponentId());
    }

    // CLM-41693 / CLM-42079: Emit APPLICATION_EVALUATION_COMPONENT_COUNTS telemetry with
    // scan_trigger_type=HOSTED_REPOSITORY_SCANNING so telemetry consumers can distinguish
    // hosted repository scans from other scan trigger types (CLI, IDE, WEB_UI, etc.).
    //
    // Fires AFTER mirrorNestedComponentViolationsFromApplicationEvaluation so we can read the
    // final component_count that the UI will display — same authoritative value the Hosted
    // Repository build report shows in its "N COMPONENTS" header. This closes the discrepancy
    // reported in CLM-42079 where telemetry emitted the raw scanner count (e.g. 6 for an
    // ansible.tar.gz with 5 unknown nested files) while the report showed 1 (identified-outer
    // gate collapsed to a single component).
    //
    // Guarded on application != null because the application-id is a required attribute of this
    // telemetry event and is not available on the repository-upload fallback path above
    // (uploadForRepository). scanReceipt is guaranteed non-null after either upload branch (both
    // throw on failure), but kept as a defensive guard against future refactors.
    if (application != null && scanReceipt != null) {
      int effectiveCount = readEffectiveComponentCount(repositoryId, outerComponentInfo.pathname(),
          componentInfos.size());
      sendHostedScanEvaluationTelemetry(
          scanReceipt.getScanId(), application.getId(), stage, componentInfos, effectiveCount);
    }

    if (componentInfos.size() > 1) {
      deleteInnerRepositoryComponentRows(repositoryId, componentInfos);
      log.info("Processed archive-of-archives scan for job id={}: outer pathname={} retained, {} inner rows deleted",
          job.getId(), outerComponentInfo.pathname(), componentInfos.size() - 1);
    }
  }

  /**
   * Returns the authoritative {@code proxy_repository_component.component_count} for the outer artifact
   * — the value the Hosted Repository UI header displays as "N COMPONENTS". Used by telemetry
   * to keep {@code number_of_components} attribute in lockstep with what users see in the report.
   * <p>
   * Falls back to {@code fallbackCount} (typically the scanner's {@code componentInfos.size()})
   * when the row is missing or the column is NULL — either would only happen on a race with a
   * concurrent delete or before the stamp landed, both indicating something already went wrong
   * upstream where the fallback is a reasonable last-resort value that matches historical
   * telemetry behaviour.
   */
  private int readEffectiveComponentCount(
      final String repositoryId,
      final String pathname,
      final int fallbackCount)
  {
    try {
      ProxyRepositoryComponent row = proxyRepositoryComponentDAO.getByRepositoryIdAndPathname(repositoryId, pathname);
      if (row != null && row.getComponentCount() != null) {
        return row.getComponentCount();
      }
    }
    catch (Exception e) {
      log.warn("Failed to read final component_count for telemetry pathname={}: {}; falling back to {}",
          pathname, e.getMessage(), fallbackCount);
    }
    return fallbackCount;
  }

  /**
   * Eagerly raises {@code component_count} on the outer artifact's {@code proxy_repository_component}
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
    try (TransactionContext tx = proxyRepositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      proxyRepositoryComponentDAO.raiseComponentCountIfHigher(tx, repositoryId, pathname, count);
      tx.commit();
    }
    catch (Exception e) {
      log.warn("Failed to eagerly raise component_count={} for pathname={}: {}",
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
    try (TransactionContext tx = proxyRepositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      proxyRepositoryComponentDAO.stampComponentCount(tx, repositoryId, pathname, count);
      tx.commit();
    }
    catch (Exception e) {
      log.warn("Failed to stamp component_count={} for pathname={}: {}",
          count, pathname, e.getMessage(), e);
    }
  }

  /**
   * Canonicalizes the raw {@code policy_evaluation_stage} column value pulled from the NXRM
   * scan-queue payload into an IQ canonical stage id.
   * <p>
   * NXRM historically sends stage in inconsistent shapes (per production DB survey
   * 2026-07-01): {@code RELEASE} / {@code release} (upper/lower case), {@code BUILD},
   * {@code STAGE_RELEASE} (underscore instead of hyphen), and occasionally {@code NULL}.
   * IQ's own stage IDs (see {@code Stage.ID_BUILD}, {@code Stage.ID_STAGE_RELEASE} etc.) are
   * all lower-case with hyphens. Feeding an un-normalized value like {@code stage_release}
   * (underscore) into policy evaluation silently mis-routes the scan because it doesn't
   * match any registered stage id.
   * <p>
   * Normalization steps:
   * <ol>
   * <li>NULL / blank → {@link ComplianceStageType#ID} fallback (existing behaviour, but now
   * explicitly logged at WARN so ops can correlate with NXRM configs that aren't
   * propagating the stage — root cause of the 29,952 stage-release vs 124 build
   * telemetry imbalance Dariush flagged).</li>
   * <li>Lowercase — {@code RELEASE} → {@code release}, {@code BUILD} → {@code build}.</li>
   * <li>Replace underscores with hyphens — {@code stage_release} → {@code stage-release}.
   * This is the fix for the {@code STAGE_RELEASE} value NXRM sometimes sends.</li>
   * </ol>
   * <p>
   * Non-goal: this method does NOT validate the canonicalized value against known
   * {@code Stage.ID_*} constants. The downstream policy evaluator already handles unknown
   * stage ids gracefully (treats them as no-match); adding validation here would risk
   * failing scans on stage values that IQ silently tolerates today.
   */
  @VisibleForTesting
  static String normalizeStage(final String rawStage, final String jobId) {
    if (rawStage == null || rawStage.isBlank()) {
      log.warn("CLM-42079: Hosted scan job id={} has NULL/blank policy_evaluation_stage; "
          + "defaulting to {}. This usually indicates NXRM is not sending the configured stage "
          + "on the /rest/repositories/hosted/scan payload.", jobId, ComplianceStageType.ID);
      return ComplianceStageType.ID;
    }
    return rawStage.toLowerCase().replace('_', '-');
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
   * Stamps {@code scanId} on {@code repository_component}, persists the policy_evaluation row, and downloads
   * the HDS report bundle so the report link is clickable in the UI.
   *
   * @param scanReceipt non-null receipt produced by a successful
   *          {@link com.sonatype.insight.brain.hds.ScanUploader} upload — the upload either returns
   *          a receipt or throws, so callers don't need to null-guard
   */
  private void persistApplicationLinkedReportFiles(
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

  private void saveReportFiles(final Application application, final String pathname, final String scanId) {
    try {
      // Download HDS report zip so the report page works immediately on first open.
      // Reuse the returned LifecycleReport for bom.json — avoids re-opening the zip.
      LifecycleReport downloadedReport = null;
      try {
        downloadedReport = reportDataStoreProvider.get().downloadReport(application, scanId, (sid, r, aid) -> {
        });
      }
      catch (FileAlreadyExistsException ignored) {
        // concurrent call already downloaded it — fine
      }

      // Patch bom.json displayName — HDS omits it for repository scans; PDF generator requires it.
      // Also dedupe bom.aaData for identified rows with duplicate (format+coords) identity —
      // HDS occasionally returns two matchState=exact rows for the same component (the npm
      // self-mirror pattern: HDS's content-hash entry + the file-SHA1 entry both surface for
      // an npm tarball uploaded to a hosted repo). LC's iq-cli already dedupes via
      // ScanPolicyEvaluator so its report shows 1 row; deduping here aligns the hosted view.
      // The dedupe is keep-first, which preserves aaData[0].hash — the join key extractBomOuterHash
      // reads below and patchBomKeepOuterOnly uses for the outer-gate trim.
      // Keep patched bytes to reuse for component count — avoids a second bom.json fetch.
      byte[] patchedBom = null;
      try {
        LifecycleReport reportToRead = downloadedReport != null
            ? downloadedReport
            : reportDataStoreProvider.get().getLifecycleReport(application, scanId);
        ReportEntry bomEntry = reportToRead != null ? reportToRead.getEntry("bom.json") : null;
        if (bomEntry != null) {
          byte[] displayNamed = HostedReportFileBuilder.patchBomDisplayName(bomEntry.buf);
          patchedBom = HostedReportFileBuilder.dedupeBomIdentifiedRows(displayNamed);
          lifecycleReportPersistenceService.saveReportFile(application.getId(), scanId, "bom.json",
              new ByteArrayInputStream(patchedBom));
        }
      }
      catch (Exception ex) {
        log.warn("Failed to patch/dedupe bom.json for scanId={}: {}", scanId, ex.getMessage());
      }

      // Save policythreats.json only — HDS data.json has the real totalArtifactCount
      // (number of internal components found inside the artifact). Overriding it with
      // our generated version (hardcoded to 1) would mask the true component count.
      ProxyRepositoryComponent comp = proxyRepositoryComponentDAO.getByScanId(scanId);
      // For an archive-of-archives upload the evaluator persisted N policy_violation rows: one
      // batch keyed on the outer pathname (outer.zip) plus one batch per inner pathname
      // (outer.zip!/inner.jar). The Components page only shows the outer row, so the synthesised
      // policythreats.json that backs the outer's report must include violations from BOTH the
      // outer pathname AND any inner pathname under it.
      List<ProxyRepositoryPolicyViolation> violations = List.of();
      if (comp != null && comp.getPathname() != null) {
        violations = proxyRepositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathnameOrInnerPathnames(
            comp.getRepositoryId(), comp.getPathname());
        Repository repositoryForFormat = repositoryDAO.getById(comp.getRepositoryId());
        String repoFormatForOuterFilter =
            repositoryForFormat != null ? repositoryForFormat.getFormat() : null;
        violations = HostedReportFileBuilder.excludeOuterViolationsForFormat(
            comp, violations, repoFormatForOuterFilter,
            HostedReportFileBuilder.resolveComponentUnknownPolicy(policyDAO, application.getId()));
      }
      // CLM-40943: extract bom.json's outer hash so policythreats.json + data.json use the same
      // hash bom.json carries for the outer entry. For npm/nuget/pub formats the file SHA1 (which
      // is what ProxyRepositoryPolicyViolation.hash stores) differs from HDS's identification hash
      // (what bom.json carries), and the LC Application Report body table joins on bom's hash.
      // Without aligning, the body shows the outer with zero violations attached even when the
      // pill header reports many. Formats whose file SHA1 already equals HDS's hash (maven, pypi,
      // rubygems, conda, helm, r) get the same hash from both sources — no-op for those.
      String bomOuterHashOverride = extractBomOuterHash(patchedBom);
      for (String fileName : List.of("policythreats.json")) {
        byte[] content = HostedReportFileBuilder.build(fileName, comp, violations, bomOuterHashOverride);
        lifecycleReportPersistenceService.saveReportFile(application.getId(), scanId, fileName,
            new ByteArrayInputStream(content));
      }

      // CLM-42117/42118/42119/42120/41737 (was CLM-40943): the full patchDataJsonPolicyCounts
      // recompute is intentionally NOT called here — recomputing policyCounts[] from IQ-side
      // rolled-up violations diverged from HDS's view for bundled archives (CLM-42119 rubygems)
      // and inflated the Critical/Severe/Moderate pills. HDS's raw policyCounts[] flows through
      // untouched so those threat pills match a same-file Lifecycle scan.
      //
      // policyComponentCount is different — HDS OMITS the field entirely for non-nested single
      // artifacts (Maven, PyPI single, RubyGems single, R CRAN). When absent, the frontend
      // header pill "Affecting N components" (ReportStatusBar.jsx:21,90) falls back to 0 even
      // when violations exist. The if-absent stamp below is scoped to that single field and
      // uses the same violation-dedup as policythreats.json so the two files stay consistent
      // by construction. It no-ops for formats where HDS already wrote the field.
      try {
        LifecycleReport reportForPatch = downloadedReport != null
            ? downloadedReport
            : reportDataStoreProvider.get().getLifecycleReport(application, scanId);
        ReportEntry dataEntryForPatch = reportForPatch != null ? reportForPatch.getEntry("data.json") : null;
        if (dataEntryForPatch != null && dataEntryForPatch.buf != null) {
          byte[] patched = HostedReportFileBuilder.patchDataJsonPolicyComponentCountIfAbsent(
              dataEntryForPatch.buf, comp, violations, bomOuterHashOverride);
          if (patched != dataEntryForPatch.buf) {
            lifecycleReportPersistenceService.saveReportFile(application.getId(), scanId, "data.json",
                new ByteArrayInputStream(patched));
          }
        }
      }
      catch (Exception ex) {
        log.warn("Failed to patch data.json.policyComponentCount for scanId={}: {}",
            scanId, ex.getMessage());
      }

      // CLM-42117 (npm 200%-identified fix): patch data.json.knownArtifactCount to reflect
      // the deduped bom's known-match count. HDS's raw data.json occasionally reports
      // knownArtifactCount greater than totalArtifactCount for formats that produce the
      // duplicate-bom-row pattern (npm content-hash + file-SHA1 both surface as
      // matchState=exact for the same coordinate — e.g. dot-prop-4.2.0.tgz gives total=1 but
      // known=2). The dedupe pass on bom.json above already collapses the duplicate rows;
      // this patch aligns data.json so the header's "N% identified" percentage matches the
      // deduped bom and never exceeds 100%.
      //
      // Reads from patchedBom (which has already been deduped) so the count reflects the
      // trimmed aaData. No-op when knownArtifactCount already equals the deduped count.
      if (patchedBom != null) {
        try {
          LifecycleReport reportForKnown = downloadedReport != null
              ? downloadedReport
              : reportDataStoreProvider.get().getLifecycleReport(application, scanId);
          ReportEntry dataEntryForKnown = reportForKnown != null ? reportForKnown.getEntry("data.json") : null;
          if (dataEntryForKnown != null && dataEntryForKnown.buf != null) {
            int dedupedKnown = HostedReportFileBuilder.countKnownMatchesInBom(patchedBom);
            byte[] patched = HostedReportFileBuilder.patchDataJsonKnownArtifactCountOnly(
                dataEntryForKnown.buf, dedupedKnown);
            if (patched != dataEntryForKnown.buf) {
              lifecycleReportPersistenceService.saveReportFile(application.getId(), scanId, "data.json",
                  new ByteArrayInputStream(patched));
            }
          }
        }
        catch (Exception ex) {
          log.warn("Failed to patch data.json.knownArtifactCount for scanId={}: {}",
              scanId, ex.getMessage());
        }
      }

      // CLM-42118 (follow-up to CLM-41737): stamp component_count from HDS's
      // data.json.totalArtifactCount — the same field the drill-in Build Report header
      // renders — so the Hosted Repos list COMPONENTS column and the Build Report agree.
      //
      // Prior behavior read bom.json.aaData.length via raiseComponentCountIfHigher, which
      // (a) can disagree with totalArtifactCount when HDS expands manifest-derived deps into
      // data.json but not into bom (rubygems, npm: list-page 2 vs report-page 5), and (b) is
      // raise-only, so the scanner's over-count from executeJob's eager stamp wins when it
      // exceeds HDS's true component count (pub .tar.gz: 39 file entries scanned vs 4
      // identified → list-page 39 vs report-page 4).
      //
      // Unconditional stamp is safe here because HDS is authoritative once its report has
      // downloaded successfully; the scanner-count eager stamp is only meaningful as a
      // fallback for the case where HDS's report never arrives (S3 latency, etc.), and in
      // that case control never reaches this line — the enclosing try's outer catch has
      // already logged the download/read failure.
      if (comp != null) {
        try {
          LifecycleReport reportForCount = downloadedReport != null
              ? downloadedReport
              : reportDataStoreProvider.get().getLifecycleReport(application, scanId);
          ReportEntry dataEntry = reportForCount != null ? reportForCount.getEntry("data.json") : null;
          if (dataEntry != null && dataEntry.buf != null) {
            JsonNode dataJson = MAPPER.readTree(dataEntry.buf);
            JsonNode totalNode = dataJson.path("totalArtifactCount");
            if (totalNode.isNumber() && totalNode.asInt() >= 0) {
              int hdsCount = totalNode.asInt();
              try (TransactionContext tx = proxyRepositoryComponentDAO.createTransactionContext()) {
                tx.begin();
                proxyRepositoryComponentDAO.stampComponentCount(
                    tx, comp.getRepositoryId(), comp.getPathname(), hdsCount);
                tx.commit();
              }
            }
          }
        }
        catch (Exception ex) {
          log.warn("Failed to stamp component_count from data.json.totalArtifactCount for scanId={}: {}",
              scanId, ex.getMessage());
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
   * Rewrite {@code policythreats.json} and patch all four collapse-related fields in
   * {@code data.json} — {@code totalArtifactCount}, {@code knownArtifactCount},
   * {@code policyComponentCount}, {@code policyCounts[]} — from the outer-only violations that
   * survive the collapse gate's DB cleanup. The gate reports the artifact as a single component,
   * but {@code saveReportFiles} had already written both files earlier using the pre-collapse
   * violation set (outer + all inner-pathname rows). Without this rewrite the drill-in
   * "Aggregate by component" table and the "Affecting N components" pill would still show the
   * pre-collapse counts even though the outer's row now reads "1 COMPONENT" — an internal
   * inconsistency between the header pill and the body.
   * <p>
   * All four {@code data.json} fields are patched in a single read-modify-write cycle so the
   * caller can skip the standalone {@code patchDataJsonTotalArtifactCount} call it would
   * otherwise make — halving the {@code data.json} I/O on the collapse path.
   * <p>
   * When the surviving outer-violation set is empty (e.g., outer had no own violations and all
   * inners were deleted), we still emit zeros into {@code data.json} directly — the shared
   * {@link HostedReportFileBuilder#patchDataJsonPolicyCounts} is a no-op on empty input and
   * would otherwise leave HDS's pre-collapse counts intact.
   * <p>
   * <b>Sequencing contract:</b> caller must have committed the inner-pathname
   * {@code proxy_repository_policy_violation} cleanup transaction before invoking this method —
   * otherwise the outer-only read below would still see the pre-cleanup rows.
   */
  private void rebuildPolicyThreatsAfterCollapse(
      final Application application,
      final String scanId,
      final String repositoryId,
      final String outerPathname,
      final int knownArtifactCount) throws Exception
  {
    ProxyRepositoryComponent outerComp =
        proxyRepositoryComponentDAO.getByRepositoryIdAndPathname(repositoryId, outerPathname);
    if (outerComp == null || outerComp.getPathname() == null) {
      log.warn("Cannot rebuild policythreats.json for collapse gate — no proxy_repository_component row for "
          + "repositoryId={} outerPathname={}; drill-in report may show pre-collapse rows",
          repositoryId, outerPathname);
      return;
    }
    List<ProxyRepositoryPolicyViolation> outerViolations = proxyRepositoryPolicyViolationDAO
        .getActiveByRepositoryIdAndPathnameOrInnerPathnames(repositoryId, outerComp.getPathname());
    Repository repositoryForFormat = repositoryDAO.getById(repositoryId);
    String repoFormatForOuterFilter = repositoryForFormat != null ? repositoryForFormat.getFormat() : null;
    outerViolations = HostedReportFileBuilder.excludeOuterViolationsForFormat(
        outerComp, outerViolations, repoFormatForOuterFilter,
        HostedReportFileBuilder.resolveComponentUnknownPolicy(policyDAO, application.getId()));
    long nonWaivedCount = outerViolations.stream().filter(v -> !v.isWaived()).count();
    String bomOuterHashOverride = null;
    LifecycleReport report = reportDataStoreProvider.get().getLifecycleReport(application, scanId);
    if (report != null) {
      ReportEntry bomEntry = report.getEntry("bom.json");
      if (bomEntry != null && bomEntry.buf != null) {
        // Pathname-aware lookup: for UNKNOWN outers where HDS's aaData[0] is a matched inner
        // (e.g. custom .gem bundling actionpack), this returns the outer's own entry so the
        // downstream policythreats.json join produces the outer's identity, not an inner's.
        bomOuterHashOverride = extractBomHashForOuter(bomEntry.buf, outerComp.getPathname());
      }
    }
    if (bomOuterHashOverride == null) {
      // No bom entry matched the outer's pathname AND aaData[0] was empty. HostedReportFileBuilder
      // .build will fall back to outerComp.hash (the file SHA1), which is correct for rubygems.
      log.debug("policythreats.json rebuild falling back to outerComp.hash (bom.json absent or "
          + "outer pathname not found in bom.aaData) for scanId={} format={}",
          scanId, repoFormatForOuterFilter);
    }
    byte[] content = HostedReportFileBuilder.build(
        "policythreats.json", outerComp, outerViolations, bomOuterHashOverride);
    ReportEntry existingPolicyThreats = report != null ? report.getEntry("policythreats.json") : null;
    if (existingPolicyThreats == null || existingPolicyThreats.buf == null
        || !Arrays.equals(existingPolicyThreats.buf, content))
    {
      lifecycleReportPersistenceService.saveReportFile(application.getId(), scanId, "policythreats.json",
          new ByteArrayInputStream(content));
      log.debug("Rebuilt policythreats.json with {} non-waived outer-only violation(s) for scanId={}",
          nonWaivedCount, scanId);
    }
    if (report != null) {
      ReportEntry dataEntry = report.getEntry("data.json");
      if (dataEntry != null && dataEntry.buf != null) {
        // Merged data.json patch: artifact-count fields first, then policy-count fields on the
        // resulting bytes. Both applied in-memory then written once, replacing the standalone
        // patchDataJsonTotalArtifactCount call the gate would otherwise make.
        byte[] withArtifactCounts =
            HostedReportFileBuilder.patchDataJsonTotalArtifactCount(dataEntry.buf, 1, knownArtifactCount);
        byte[] patchedData = outerViolations.isEmpty()
            ? HostedReportFileBuilder.zeroDataJsonPolicyCounts(withArtifactCounts)
            : HostedReportFileBuilder.patchDataJsonPolicyCounts(
                withArtifactCounts, outerComp, outerViolations, bomOuterHashOverride);
        if (patchedData != dataEntry.buf) {
          lifecycleReportPersistenceService.saveReportFile(application.getId(), scanId, "data.json",
              new ByteArrayInputStream(patchedData));
          log.debug("Patched data.json (totalArtifactCount=1, knownArtifactCount={}, "
              + "policyComponentCount+policyCounts from {} non-waived outer-only violation(s)) "
              + "for scanId={}", knownArtifactCount, nonWaivedCount, scanId);
        }
      }
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
      final String outerHash,
      final String outerPathname) throws Exception
  {
    LifecycleReport report = reportDataStoreProvider.get().getLifecycleReport(application, scanId);
    if (report == null) {
      return;
    }
    ReportEntry bomEntry = report.getEntry("bom.json");
    if (bomEntry == null || bomEntry.buf == null) {
      return;
    }
    String keepHash = extractBomHashForOuter(bomEntry.buf, outerPathname);
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
    lifecycleReportPersistenceService.saveReportFile(application.getId(), scanId, "bom.json",
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
   * <b>Single-arg behavior</b>: assumes every counted component is a known match — appropriate
   * for the identified-outer collapse gate where the outer's matchState was verified before
   * invoking. For paths where the counted components may include {@code matchState=unknown}
   * entries (helm chart of a custom operator, proprietary archive), use the four-arg overload
   * that accepts an explicit {@code knownCount} so the "% identified" percentage in the header
   * reflects the true match-state distribution.
   * <p>
   * <b>Single-arg behavior</b>: assumes every counted component is a known match — appropriate
   * for the identified-outer collapse gate where the outer's matchState was verified before
   * invoking. For paths where the counted components may include {@code matchState=unknown}
   * entries (helm chart of a custom operator, proprietary archive), use the four-arg overload
   * that accepts an explicit {@code knownCount} so the "% identified" percentage in the header
   * reflects the true match-state distribution.
   * <p>
   * No-op when {@code directCount} is negative or {@code data.json} can't be located on disk
   * (the report stays with HDS's original numbers — fail-soft).
   */
  private void patchDataJsonTotalArtifactCount(
      final Application application,
      final String scanId,
      final int directCount) throws Exception
  {
    patchDataJsonTotalArtifactCount(application, scanId, directCount, directCount);
  }

  /**
   * Overload that stamps an explicit {@code knownArtifactCount} independent of
   * {@code totalArtifactCount}. Used by the CM re-eval path where the outer's matchState may
   * be {@code unknown} (Component-Unknown) — writing the same value for both would falsely
   * report "100% identified" in the UI when the sole physical component is not identified.
   * <p>
   * {@code knownCount &lt; 0} is treated as "unknown, fall back to caller's directCount" for
   * back-compat with callers that don't have a bom to inspect; callers that DO have a bom
   * should pass the count from {@link HostedReportFileBuilder#countKnownMatchesInBom}.
   */
  private void patchDataJsonTotalArtifactCount(
      final Application application,
      final String scanId,
      final int directCount,
      final int knownCount) throws Exception
  {
    if (directCount < 0) {
      return;
    }
    LifecycleReport report = reportDataStoreProvider.get().getLifecycleReport(application, scanId);
    if (report == null) {
      return;
    }
    ReportEntry dataEntry = report.getEntry("data.json");
    if (dataEntry == null || dataEntry.buf == null) {
      return;
    }
    int effectiveKnown = knownCount < 0 ? directCount : knownCount;
    byte[] patched = HostedReportFileBuilder.patchDataJsonTotalArtifactCount(
        dataEntry.buf, directCount, effectiveKnown);
    if (patched == dataEntry.buf) {
      return;
    }
    lifecycleReportPersistenceService.saveReportFile(application.getId(), scanId, "data.json",
        new ByteArrayInputStream(patched));
    log.debug("Patched data.json.totalArtifactCount={} knownArtifactCount={} for scanId={}",
        directCount, effectiveKnown, scanId);
  }

  /**
   * CLM-40943: extract the outer artifact's hash from bom.json's first {@code aaData[]} entry.
   * That hash is HDS's identification hash for the outer component, which for npm/nuget/pub
   * formats differs from the file SHA1 stored on {@code proxy_repository_policy_violation.hash}. We
   * thread it through to {@link HostedReportFileBuilder} so the synthesised
   * {@code policythreats.json} (and the patched {@code data.json} policy counts) carry the
   * same hash bom.json carries — that's the key the LC Application Report body joins on.
   * <p>
   * Returns {@code null} when bom is null/unparseable or has no {@code aaData[0].hash} —
   * fail-soft so the call site falls back to today's behaviour (use
   * {@code ProxyRepositoryPolicyViolation.hash}) for any format we haven't accounted for.
   */
  static String extractBomOuterHash(final byte[] patchedBom) {
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

  /** Returns the bom aaData hash whose pathnames[] contains {@code outerPathname}; falls back to aaData[0].hash. */
  static String extractBomHashForOuter(final byte[] patchedBom, final String outerPathname) {
    if (patchedBom == null || patchedBom.length == 0) {
      return null;
    }
    if (outerPathname == null || outerPathname.isEmpty()) {
      return extractBomOuterHash(patchedBom);
    }
    try {
      JsonNode bomJson = MAPPER.readTree(patchedBom);
      JsonNode aaData = bomJson.path("aaData");
      if (!aaData.isArray() || aaData.isEmpty()) {
        return null;
      }
      for (JsonNode entry : aaData) {
        JsonNode pathnames = entry.path("pathnames");
        if (!pathnames.isArray()) {
          continue;
        }
        for (JsonNode pn : pathnames) {
          if (outerPathname.equals(pn.asText(""))) {
            String hash = entry.path("hash").asText("");
            if (!hash.isEmpty()) {
              return hash;
            }
          }
        }
      }
      return extractBomOuterHash(patchedBom);
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
   * Sentinel for missing {@code format()} in scan component infos. Matches
   * {@code ScanPolicyEvaluator.UNKNOWN} so the resulting {@code number_of_unknown_components}
   * telemetry attribute key is identical across hosted-repo and regular scan paths — telemetry
   * consumers aggregating across scan types must not split on a sentinel-case difference.
   */
  private static final String TELEMETRY_UNKNOWN_FORMAT = "unknown";

  /**
   * Emits {@code APPLICATION_EVALUATION_COMPONENT_COUNTS} telemetry with
   * {@code scan_trigger_type=HOSTED_REPOSITORY_SCANNING} for hosted repository scans.
   * <p>
   * Hosted repo scans go through {@link RepositoryPolicyEvaluator} (not {@code ScanPolicyEvaluator}),
   * so this event must be emitted explicitly from this path to satisfy CLM-41693: enabling
   * telemetry consumers to distinguish hosted repository scans from other trigger types
   * (CLI, IDE, WEB_UI, etc.) using a single telemetry event.
   */
  @VisibleForTesting
  void sendHostedScanEvaluationTelemetry(
      final String scanId,
      final String applicationId,
      final String stage,
      final List<ScanComponentInfo> componentInfos,
      final int effectiveComponentCount)
  {
    try {
      // CLM-42079: cap the per-format grouping to effectiveComponentCount so telemetry matches
      // what the UI shows in the Hosted Repository report ("1 COMPONENT" for identified-outer
      // collapse; N for keep-nested formats like nuget/go/pub; scanner count for unidentified).
      // Attribute the count to the OUTER's format when collapsing — that's the artifact users
      // interact with, and matches the Application Report which also shows a single format row.
      //
      // The `effectiveComponentCount == 1` guard is deliberately strict (not `<= 1`): a 0 or
      // negative value indicates the DB read fell back or returned a zero-stamped row, and we do
      // NOT want to fabricate a `{outerFormat: 1}` in that case — that would over-count what the
      // UI shows as zero. Falls through to the empty-map path below, which produces a payload
      // with only `number_of_components=0` and no per-format keys.
      Map<String, Long> componentCounts;
      if (effectiveComponentCount == 1 && !componentInfos.isEmpty()) {
        // Collapsed view: 1 row keyed on the outer's format.
        String outerFormat = componentInfos.get(0).format() != null
            ? componentInfos.get(0).format()
            : TELEMETRY_UNKNOWN_FORMAT;
        componentCounts = Collections.singletonMap(outerFormat, 1L);
      }
      else if (effectiveComponentCount <= 0) {
        // Zero (or negative — a defensive fallback readEffectiveComponentCount could theoretically
        // produce) means the authoritative DB view has no components for this scan. Emit an empty
        // per-format map so downstream number_of_components sums to 0, matching the UI.
        componentCounts = Collections.emptyMap();
      }
      else {
        // Non-collapsed: group all scanned components by format. Groupings from componentInfos
        // always sum to componentInfos.size(). When effectiveComponentCount is smaller (nuget /
        // go / pub scans where ScanPolicyEvaluator dedupes below the scanner's view, or after
        // the inner-row delete step in executeJob) we cap the aggregate to effectiveComponentCount
        // by preferring the outer's format for the surplus — that keeps
        // number_of_components == UI-shown value, matches the identified-outer collapse pattern
        // above, and only affects per-format distribution (which for keep-nested formats is
        // dominated by the outer's format anyway).
        Map<String, Long> rawGrouping = componentInfos.stream()
            .map(info -> info.format() != null ? info.format() : TELEMETRY_UNKNOWN_FORMAT)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        long rawSum = rawGrouping.values().stream().mapToLong(Long::longValue).sum();
        if (rawSum > effectiveComponentCount) {
          // Cap the total. Attribute the reduction to the outer's format (or fall back to any
          // over-represented key if the outer isn't in the grouping — shouldn't happen but keeps
          // the cap safe).
          String outerFormat = componentInfos.get(0).format() != null
              ? componentInfos.get(0).format()
              : TELEMETRY_UNKNOWN_FORMAT;
          long surplus = rawSum - effectiveComponentCount;
          Map<String, Long> capped = new java.util.LinkedHashMap<>(rawGrouping);
          long outerCount = capped.getOrDefault(outerFormat, 0L);
          if (outerCount >= surplus) {
            long adjusted = outerCount - surplus;
            if (adjusted == 0) {
              capped.remove(outerFormat);
            }
            else {
              capped.put(outerFormat, adjusted);
            }
          }
          else {
            // Outer alone can't absorb the surplus — drain proportionally starting from the
            // largest bucket to avoid producing negative counts. Cheap and rare path.
            long remaining = surplus;
            for (Map.Entry<String, Long> e : new java.util.ArrayList<>(capped.entrySet())) {
              if (remaining <= 0) {
                break;
              }
              long v = e.getValue();
              long take = Math.min(v, remaining);
              long left = v - take;
              if (left == 0) {
                capped.remove(e.getKey());
              }
              else {
                capped.put(e.getKey(), left);
              }
              remaining -= take;
            }
          }
          log.debug("Capped per-format grouping from raw={} to effectiveCount={} for scanId={}",
              rawSum, effectiveComponentCount, scanId);
          componentCounts = capped;
        }
        else {
          componentCounts = rawGrouping;
        }
      }
      TelemetryData telemetryData = telemetryUtils.buildApplicationEvaluationTelemetryData(
          scanId, applicationId, stage.toLowerCase(),
          ScanTriggerType.HOSTED_REPOSITORY_SCANNING,
          null, null,
          Collections.singletonMap("component_counts", componentCounts));
      telemetrySender.send(telemetryData);
      log.debug("Sent APPLICATION_EVALUATION_COMPONENT_COUNTS telemetry for hosted scan: "
          + "scanId={}, appId={}, scan_trigger_type=HOSTED_REPOSITORY_SCANNING, count={}",
          scanId, applicationId, effectiveComponentCount);
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
      if (policyEvaluationDAO.getLastByOwnerIdAndScanId(tx, appId, scanId) == null) {
        PolicyEvaluation pe = PolicyEvaluation.createForHostedComponent(appId, stageTypeId, scanId, false);
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
    try (TransactionContext tx = proxyRepositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      proxyRepositoryComponentDAO.stampLastEvaluationStage(tx, repositoryId, pathname, stage);
      tx.commit();
    }
    catch (Exception e) {
      log.warn("Failed to stamp stage for pathname={}: {}", pathname, e.getMessage(), e);
    }
  }

  private void stampScanId(final String repositoryId, final String pathname, final String scanId) {
    try (TransactionContext tx = proxyRepositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      proxyRepositoryComponentDAO.stampScanId(tx, repositoryId, pathname, scanId);
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
    // outer (we delete the inner proxy_repository_component rows next), but downstream code that
    // joins on proxy_repository_policy_violation.component_id — waivers-by-component, quarantine-by-
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
   * {@code proxy_repository_policy_violation} so the existing repository-side UI, queries, and
   * report-building code paths see per-inner findings.
   * <p>
   * <b>Why:</b> the repository-evaluation pipeline (called by {@link #evaluatePolicies}) only
   * writes a single {@code proxy_repository_policy_violation} row for the outer artifact, because the
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
   * {@code proxy_repository_policy_violation}, not {@code policy_violation}.
   * <p>
   * <b>What this does:</b>
   * <ol>
   * <li>Invoke {@link ScanPolicyEvaluator#evaluate} on the synthetic application — same code
   * path "Evaluate a binary" uses, runs all the Drools magic, populates
   * {@code application_component} and {@code policy_violation}.</li>
   * <li>Read back the {@code policy_violation} rows by (application_id, stage_type_id).</li>
   * <li>For each row whose component hash is NOT the outer's hash (the outer is already
   * handled by {@link #evaluatePolicies} above; double-writing would create duplicate
   * {@code proxy_repository_policy_violation} rows under different pathnames), build a synthetic
   * {@code outer!/coords} pathname and insert into {@code proxy_repository_policy_violation} with
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
   * <p>
   * <b>CLM-42080:</b> public entry point takes primitives (not {@link HostedComponentScanQueue}
   * / {@link ScanComponentInfo}) so callers can share this exact drill-down and mirror logic
   * with the initial-scan path. Callers pass {@code scanId} for both {@code jobLogId} and
   * {@code scanId} — the split exists so the initial-scan path can pass
   * {@code HostedComponentScanQueue.getId()} as {@code jobLogId} while carrying its own
   * distinct {@code scanId}.
   * <p>
   * <b>Side effect on re-eval:</b> the identified-outer gate calls
   * {@link #patchBomKeepOuterOnly}, which permanently trims {@code bom.json} to the outer's
   * entry. If a component transitions UNKNOWN → identified between the initial scan and a
   * re-eval (e.g. HDS updated its database), the first re-eval that hits the gate will trim
   * the overlay for the first time. Subsequent re-evals are idempotent (the trim function
   * returns early when its input already matches its output), but the original multi-entry
   * bom cannot be recovered without re-uploading the scan.
   *
   * @param jobLogId opaque id used ONLY for log correlation (initial-scan path passes
   *          {@code HostedComponentScanQueue.getId()}; re-eval path passes the scan id).
   * @param componentIdOrNull NXRM {@code componentId} to stamp on newly-created rows, or
   *          {@code null} on the re-eval path (no queue entry to source it from).
   */
  public void mirrorNestedComponentViolationsFromApplicationEvaluation(
      final String jobLogId,
      final String repositoryId,
      final String outerPathname,
      final String outerHash,
      final Application application,
      final String scanId,
      final String stageTypeId,
      final String componentIdOrNull)
  {
    if (scanId == null || stageTypeId == null) {
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
      // we'd mirror them into proxy_repository_policy_violation and the drill-in Build Report
      // would show "178 VIOLATIONS Affecting 32 components" for what iq-cli reports as
      // "4 violations, 1 component".
      //
      // RepositoryPolicyEvaluator (run earlier in executeJob) already populated
      // proxy_repository_component.match_state_id with HDS's identification verdict for the outer.
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
      ProxyRepositoryComponent outerRow =
          proxyRepositoryComponentDAO.getByRepositoryIdAndPathname(repositoryId, outerPathname);
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
      // CLM-42119: formats whose iq-cli treats the outer as opaque — collapse regardless of
      // HDS's match-state verdict. See ALWAYS_COLLAPSE_TO_OUTER_FORMATS javadoc.
      boolean alwaysCollapseForFormat =
          repoFormat != null && ALWAYS_COLLAPSE_TO_OUTER_FORMATS.contains(repoFormat.toLowerCase());

      if ((outerIdentified && !keepNestedForFormat) || alwaysCollapseForFormat) {
        log.info("Identified-outer gate: outer pathname={} matchState={} format={} alwaysCollapse={} → "
            + "reporting as 1 component, no inner drill-down (matches iq-cli single-file scan behaviour)",
            outerPathname,
            outerRow != null ? outerRow.getMatchStateId() : "null",
            repoFormat,
            alwaysCollapseForFormat);

        // Stamp componentCount = 1. Unconditional UPDATE because the earlier eager stamp and
        // saveReportFiles' bom refinement (both via raiseComponentCountIfHigher) may have
        // raised the column to HDS's expanded count.
        forceComponentCount(repositoryId, outerPathname, 1);

        // Patch data.json.totalArtifactCount=1 so the drill-in Build Report header reads
        // "1 COMPONENT" instead of HDS's expanded view. knownArtifactCount reflects the outer's
        // actual matchState: 1 when outerIdentified (outer's matchState is exact/similar/
        // embedded → header reads "100% identified"), 0 when this gate fired via
        // alwaysCollapseForFormat on an UNKNOWN outer (e.g. rubygems custom .gem → header
        // reads "0% identified", matching what iq-cli reports on the same file).
        //
        // For alwaysCollapseForFormat (rubygems), skip the standalone patch here — the same
        // fields are set inside rebuildPolicyThreatsAfterCollapse alongside the policy-count
        // fields, so we download+parse+save data.json once instead of twice.
        int knownCount = outerIdentified ? 1 : 0;
        if (!alwaysCollapseForFormat) {
          try {
            patchDataJsonTotalArtifactCount(application, scanId, 1, knownCount);
          }
          catch (Exception ex) {
            log.warn(
                "Failed to patch data.json.totalArtifactCount=1 knownArtifactCount={} for identified outer scanId={}: {}",
                knownCount, scanId, ex.getMessage());
          }
        }

        // CLM-40943: trim bom.json.aaData[] to keep only the outer's entry. Without this the
        // drill-in Build Report's body table reads HDS's expanded bom (e.g. 32 rows for a
        // devise.gem) while the header and pills read the gate's "1 COMPONENT" view — an
        // internal UI inconsistency. Trimming bom.json drives every downstream consumer
        // (Application Report body, SBOM exports, Search index) to the same one-entry view
        // iq-cli already produces for the same binary.
        try {
          patchBomKeepOuterOnly(application, scanId, outerHash, outerPathname);
        }
        catch (Exception ex) {
          log.warn("Failed to trim bom.json to outer for identified outer scanId={}: {}",
              scanId, ex.getMessage());
        }

        // Idempotent cleanup: delete any stale inner-pathname rows a prior run (or earlier
        // version of this code) left in proxy_repository_policy_violation. The outer's own row
        // (no "!/" in pathname) is owned by RepositoryPolicyEvaluator and stays.
        try (TransactionContext tx = proxyRepositoryPolicyViolationDAO.createTransactionContext()) {
          tx.begin();
          List<ProxyRepositoryPolicyViolation> existingForOuter = proxyRepositoryPolicyViolationDAO
              .getActiveByRepositoryIdAndPathnameOrInnerPathnames(repositoryId, outerPathname);
          int deleted = 0;
          for (ProxyRepositoryPolicyViolation existing : existingForOuter) {
            String existingPathname = existing.getPathname();
            if (existingPathname != null && existingPathname.contains("!/")) {
              proxyRepositoryPolicyViolationDAO.delete(tx, existing);
              deleted++;
            }
          }
          tx.commit();
          if (deleted > 0) {
            log.info("Identified-outer gate cleanup: deleted {} stale inner-pathname rows for "
                + "outer pathname={}", deleted, outerPathname);
          }
        }

        // saveReportFiles built policythreats.json + data.json earlier in executeJob from the
        // pre-collapse violation set, which included inner-pathname rows. After the DB cleanup
        // above trimmed those rows, the on-disk report files still hold the pre-collapse counts
        // and drill-in rows — the "Aggregate by component" table would show inner rows while the
        // header pill claims one component, and the "Affecting N components" pill would show
        // HDS's expanded count. Rewriting both files here brings every downstream reader (report
        // body, SBOM export, Search index) in line with the collapse-to-outer decision.
        // Scoped to formats in ALWAYS_COLLAPSE_TO_OUTER_FORMATS (rubygems today); other formats
        // that hit the gate via outerIdentified && !keepNestedForFormat (maven, r, conda, helm)
        // take the existing saveReportFiles path unchanged so this PR doesn't widen the blast
        // radius.
        if (alwaysCollapseForFormat) {
          try {
            rebuildPolicyThreatsAfterCollapse(application, scanId, repositoryId, outerPathname, knownCount);
          }
          catch (Exception ex) {
            log.warn("Failed to rebuild policythreats.json for collapse gate scanId={}", scanId, ex);
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
      Stage policyStage = new Stage(stageTypeId.toLowerCase());
      scanPolicyEvaluatorProvider.get()
          .evaluate(application, scanId, policyStage,
              // CLM-42079: keep the trigger type consistent with the policy_evaluation row we
              // write in createPolicyEvaluationRecord() so the internal ScanPolicyEvaluator
              // emission tags the same synthetic-app scan as HOSTED_REPOSITORY_SCANNING in
              // telemetry. Was REPOSITORY_MANAGER before, which silently disagreed with the row
              // this method's caller had just persisted.
              ScanTriggerType.HOSTED_REPOSITORY_SCANNING,
              ClientScanType.SONATYPE,
              false /* skipAutoWaivers */);

      // CLM-42117/42118/42119/42120/41737 (was CLM-40943): the prior synthetic-eval
      // component_count raise + patchDataJsonTotalArtifactCount stamp chain has been removed.
      // component_count now stays at whatever the HDS-bom refinement in saveReportFiles wrote
      // (aaData.length via raiseComponentCountIfHigher), and data.json.totalArtifactCount /
      // knownArtifactCount stay as HDS wrote them — so the Hosted Repos list COMPONENTS
      // column, the drill-in Build Report header, and an equivalent Lifecycle-app scan of the
      // same file all agree.
      //
      // appComponents is still fetched because the mirror loop below uses it to resolve each
      // policy_violation to its component pathnames. keepDependencyDerived is still needed
      // for the mirror loop's filter branch (currently out of scope — see steps 4/5 in the
      // follow-up ticket).
      List<OwnerComponent> appComponents =
          applicationComponentDAO.getByOwnerIdAndStageTypeId(application.getId(), stageTypeId.toLowerCase());

      boolean keepDependencyDerived =
          repoFormat != null && KEEP_DEPENDENCY_DERIVED_COMPONENTS_FORMATS.contains(repoFormat.toLowerCase());

      // Step 2: read back the policy_violation rows the evaluator just wrote, and explicitly
      // load their constraint_facts. PolicyViolationDAO returns rows with constraintFacts
      // unloaded (lazy by default for perf — most callers don't need them); calling
      // getConstraintFacts() on an unloaded row throws IllegalStateException with a message
      // that names the DAO method to fix it. We DO need them: the ProxyRepositoryPolicyViolation
      // constructor requires non-null/non-empty constraintFacts (see AbstractPolicyViolation
      // line ~173) because that's the source of truth for which CVE / license rule actually
      // matched. Loading them in one batch call is the standard pattern (see
      // ScanPolicyEvaluator's own use at lines 590, 653, 772, 1154, 1987).
      List<PolicyViolation> violations =
          policyViolationDAO.getActiveByOwnerIdAndStageId(application.getId(), stageTypeId.toLowerCase());
      if (violations.isEmpty()) {
        log.debug("ScanPolicyEvaluator produced no policy_violation rows for jobLogId={}, app={}, scan={} — "
            + "no inner-component violations to mirror (stale inner rows will still be cleaned up)",
            jobLogId, application.getId(), scanId);
      }
      else {
        policyViolationDAO.loadConstraintFacts(violations);
      }

      // Step 3: build a hash → OwnerComponent map so we can resolve each violation's
      // pathnames (= file paths inside the outer archive). The bom-derived pathnames are the
      // truthful "where inside the outer" — preserve them in the mirrored row's pathname so
      // downstream UI/audit can render exact locations. Re-using appComponents from above.
      Map<String, OwnerComponent> componentByHash = new HashMap<>();
      for (OwnerComponent ac : appComponents) {
        componentByHash.put(ac.getHash(), ac);
      }

      // Step 4: mirror only INNER violations. Skip the outer (already persisted by
      // evaluatePolicies; double-write would create a second proxy_repository_policy_violation row
      // under a different pathname with the same coordinates, breaking dedup downstream).
      // (outerHash is a parameter of this method — no local re-declaration needed.)
      Date now = new Date();
      int mirrored = 0;
      try (TransactionContext tx = proxyRepositoryPolicyViolationDAO.createTransactionContext()) {
        tx.begin();

        // Idempotent re-mirror: delete the inner-pathname rows we previously wrote for this
        // outer before inserting the fresh batch. Without this, a re-evaluation (or a partial
        // mirror retry) leaves stale rows in proxy_repository_policy_violation that either inflate
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
        List<ProxyRepositoryPolicyViolation> existingForOuter = proxyRepositoryPolicyViolationDAO
            .getActiveByRepositoryIdAndPathnameOrInnerPathnames(repositoryId, outerPathname);
        int deletedStale = 0;
        for (ProxyRepositoryPolicyViolation existing : existingForOuter) {
          String existingPathname = existing.getPathname();
          if (existingPathname != null && existingPathname.contains("!/")) {
            proxyRepositoryPolicyViolationDAO.delete(tx, existing);
            deletedStale++;
          }
        }
        if (deletedStale > 0) {
          log.debug("Idempotent re-mirror: deleted {} stale inner-pathname rows for jobLogId={}, "
              + "outer pathname={}", deletedStale, jobLogId, outerPathname);
        }

        int skippedDependencyDerived = 0;
        for (PolicyViolation pv : violations) {
          if (pv.getHash() != null && pv.getHash().equals(outerHash)) {
            // Outer-hash violations are already in proxy_repository_policy_violation under the
            // real outer pathname.
            continue;
          }
          OwnerComponent ac = componentByHash.get(pv.getHash());
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
          // KEEP_DEPENDENCY_DERIVED_COMPONENTS_FORMATS (currently: go, pub). For Go module zips
          // the go.mod-declared transitives ARE the real component graph LC reports — keeping
          // them in agreement with the LC application Evaluate File view.
          if (ac != null && !keepDependencyDerived && !hasDirectIdentificationPathname(ac.getPathnames())) {
            skippedDependencyDerived++;
            continue;
          }
          // Compose the synthetic pathname: outer + "!/" + inner-coordinates-label.
          // displayName / coordinates label is the most readable choice; OwnerComponent
          // carries the full pathnames list (newline-separated text) but we don't need every
          // path — one canonical label per inner component is enough for the UI and matches
          // what the existing policythreats.json builder expects.
          String innerLabel = innerLabelFromComponent(pv, ac);
          String innerPathname = outerPathname + "!/" + innerLabel;

          ProxyRepositoryPolicyViolation rpv = new ProxyRepositoryPolicyViolation(
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
          if (componentIdOrNull != null) {
            rpv.setComponentId(componentIdOrNull);
          }
          proxyRepositoryPolicyViolationDAO.insert(tx, rpv);
          mirrored++;
        }
        tx.commit();
        if (skippedDependencyDerived > 0) {
          log.info("Excluded {} manifest-derived 'dependency:' policy_violation rows from "
              + "proxy_repository_policy_violation mirror for jobLogId={}, app={}, scan={}",
              skippedDependencyDerived, jobLogId, application.getId(), scanId);
        }
      }
      log.info("Mirrored {} inner-component policy_violation rows into proxy_repository_policy_violation "
          + "for jobLogId={}, app={}, scan={} (total app-side violations: {}, outer-hash filtered)",
          mirrored, jobLogId, application.getId(), scanId, violations.size());
    }
    catch (Exception e) {
      log.warn("Nested-component mirror failed for jobLogId={} (outer eval already persisted): {}",
          jobLogId, e.getMessage(), e);
    }
  }

  /**
   * Produce a stable, human-readable inner-component label for the synthetic pathname
   * {@code outer + "!/" + label}. Preference order:
   * <ol>
   * <li>{@code componentIdentifier} → format-coordinates-derived label (e.g. {@code form-data@2.3.3})</li>
   * <li>{@code OwnerComponent.pathnames} → first non-blank entry (the truthful nested path)</li>
   * <li>{@code hash} → opaque fallback</li>
   * </ol>
   */
  private static String innerLabelFromComponent(
      final PolicyViolation violation,
      final OwnerComponent component)
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
   * Removes the {@code proxy_repository_component} rows the evaluator created for inner artifact
   * pathnames (those containing the {@code !/} separator). The inner-pathname
   * {@code proxy_repository_policy_violation} rows are intentionally left in place — they feed the
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
    try (TransactionContext tx = proxyRepositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      proxyRepositoryComponentDAO.deleteByRepositoryIdAndPathnames(tx, repositoryId, innerPathnames);
      tx.commit();
    }
    catch (Exception e) {
      // Don't fail the job — at worst the Components page shows extra inner-pathname rows the
      // user can ignore. The outer row + report are unaffected.
      log.warn("Failed to delete inner proxy_repository_component rows for repositoryId={}: {}",
          repositoryId, e.getMessage(), e);
    }
  }

  /**
   * Stamps {@code component_id} on the outer artifact's {@code proxy_repository_component} row AND on
   * every active violation row whose pathname is either the outer pathname OR an inner-pathname
   * under it ({@code outer.zip!/inner.jar}). The {@code proxy_repository_component} side is intentionally
   * outer-only because the inner proxy_repository_component rows are deleted in
   * {@link #deleteInnerRepositoryComponentRows}; the violation side covers both so future
   * component-id-keyed code paths (waivers, quarantine) match inner-artifact findings too.
   */
  private void stampNxrmComponentIdOnOuterAndInnerPathnames(
      final String repositoryId,
      final String outerPathname,
      final String componentId)
  {
    try (TransactionContext tx = proxyRepositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      proxyRepositoryComponentDAO.stampComponentId(tx, repositoryId, outerPathname, componentId);
      proxyRepositoryPolicyViolationDAO.stampComponentIdOnPathnameOrInnerPathnames(
          tx, repositoryId, outerPathname, componentId);
      tx.commit();
      log.debug("Stamped component_id={} on proxy_repository_component (outer) and "
          + "proxy_repository_policy_violation (outer + inner pathnames) for pathname={}",
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
