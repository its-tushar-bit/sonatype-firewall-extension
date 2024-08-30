/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.thirdparty.ThirdPartyScanService;
import com.sonatype.insight.brain.utils.ExecutorThreadPools;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.scan.model.ClientScanType;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO.THIRD_PARTY_BOM_JSON_FILENAME;

/**
 * @since 1.8
 */
@Named
public class PolicyMonitor
{
  private static final Logger log = LoggerFactory.getLogger(PolicyMonitor.class);

  public static final int POLICY_MONITOR_THREADS_DEFAULT = 1;

  private static final int POLICY_MONITOR_THREADS_MIN = 1;

  private static final int POLICY_MONITOR_THREADS_MAX = 20;

  private final ForkJoinPool applicationMonitorForkJoinPool;

  private final InsightWork work;

  private final ScanUploader uploader;

  private final ScanPolicyEvaluator scanPolicyEvaluator;

  private final PolicyAlertNotifier policyAlertNotifier;

  private final ProductLicense productLicense;

  private final AuditRecorder auditRecorder;

  private final ThirdPartyScanService thirdPartyScanService;

  private final PolicyMonitoringDAO policyMonitoringDAO;

  private final OwnerDAO ownerDAO;

  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final ThirdPartyScanDAO thirdPartyScanDAO;

  private final TelemetrySender telemetrySender;

  private final TelemetryUtils telemetryUtils;

  @Inject
  public PolicyMonitor(
      final InsightWork work,
      final ScanUploader uploader,
      final ScanPolicyEvaluator scanPolicyEvaluator,
      final PolicyAlertNotifier policyAlertNotifier,
      final ProductLicense productLicense,
      final AuditRecorder auditRecorder,
      final ThirdPartyScanService thirdPartyScanService,
      final PolicyMonitoringDAO policyMonitoringDAO,
      final OwnerDAO ownerDAO,
      final ApplicationDAO applicationDAO,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final ThirdPartyScanDAO thirdPartyScanDAO,
      final Configuration configuration,
      final ShutdownHandler shutdownHandler,
      final TelemetrySender telemetrySender,
      final TelemetryUtils telemetryUtils)
  {
    this.work = work;
    this.uploader = uploader;
    this.scanPolicyEvaluator = scanPolicyEvaluator;
    this.policyAlertNotifier = policyAlertNotifier;
    this.productLicense = productLicense;
    this.auditRecorder = auditRecorder;
    this.thirdPartyScanService = thirdPartyScanService;
    this.policyMonitoringDAO = policyMonitoringDAO;
    this.ownerDAO = ownerDAO;
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.thirdPartyScanDAO = thirdPartyScanDAO;
    this.applicationMonitorForkJoinPool = initThreadPool(configuration);
    this.telemetrySender = telemetrySender;
    this.telemetryUtils = telemetryUtils;
    shutdownHandler.add(this.applicationMonitorForkJoinPool);
  }

  // Visible for testing
  ForkJoinPool getApplicationMonitorForkJoinPool() {
    return applicationMonitorForkJoinPool;
  }

  private ForkJoinPool initThreadPool(Configuration configuration) {
    int maxThreadCount = POLICY_MONITOR_THREADS_MAX;
    int threadCount = POLICY_MONITOR_THREADS_DEFAULT;

    Integer saasPolicyMonitorPoolSize = configuration.getSaasPolicyMonitorPoolSize();
    if (saasPolicyMonitorPoolSize != null && saasPolicyMonitorPoolSize > 0) {
      maxThreadCount = saasPolicyMonitorPoolSize;
      threadCount = saasPolicyMonitorPoolSize;
    }

    ForkJoinPool threadPool = ExecutorThreadPools.getInstance().createThreadPool(
        POLICY_MONITOR_THREADS_MIN, maxThreadCount, threadCount, "insight.threads.monitor");
    log.info("insight.threads.monitor pool-size: {}", threadPool.getParallelism());

    return threadPool;
  }

  public void run() {
    log.info("Starting policy monitoring");

    long start = System.currentTimeMillis();

    List<PolicyMonitoring> policyMonitorings = policyMonitoringDAO.getAll();
    if (policyMonitorings.isEmpty()) {
      log.info("Policy monitoring was not configured for any applications, organizations, or repositories.");
      return;
    }

    Map<String, PolicyMonitoring> policyMonitoringsByOwnerId = new LinkedHashMap<>();
    for (PolicyMonitoring policyMonitoring : policyMonitorings) {
      policyMonitoringsByOwnerId.put(policyMonitoring.getOwnerId(), policyMonitoring);
    }

    evaluateApplications(policyMonitoringsByOwnerId);

    log.info("Policy monitoring evaluated in {} ms", System.currentTimeMillis() - start);
  }

  private void evaluateApplications(final Map<String, PolicyMonitoring> policyMonitoringsByOwnerId) {
    if (!isLicensedForApplications(productLicense)) {
      log.debug("Not licensed for Application Policy Monitoring.");
      return;
    }
    log.debug("Licensed for Application Policy Monitoring.");

    List<Application> apps = applicationDAO.getAll();
    log.info("Starting policy monitoring of applications");
    long start = System.currentTimeMillis();

    List<CompletableFuture<Void>> futures = new ArrayList<>();
    Set<String> stagesDetectedDuringScan = new HashSet<>();
    long appsUnderContinuousMonitoringCount = 0;

    for (Application app : apps) {
      PolicyMonitoring policyMonitoring = null;
      for (Owner owner : ownerDAO.walkHierarchy(app)) {
        policyMonitoring = policyMonitoringsByOwnerId.get(owner.getId());
        if (policyMonitoring != null) {
          break;
        }
      }

      if (policyMonitoring == null || !Stage.isValidStageTypeId(policyMonitoring.getStageTypeId())) {
        continue;
      }

      appsUnderContinuousMonitoringCount++;
      final PolicyMonitoring finalPolicyMonitoring = policyMonitoring;
      stagesDetectedDuringScan.add(finalPolicyMonitoring.getStageTypeId());

      CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
        try (AuditSession session = auditRecorder.recordSystemEvent(AuditEvent.EVALUATE_APPLICATION)) {
          try {
            AuditData.get().setApplication(app);
            evaluate(app, finalPolicyMonitoring);
          }
          catch (InterruptedException e) {
            AuditData.get().setException(e);
            Thread.currentThread().interrupt();
          }
          catch (IOException | RuntimeException e) {
            AuditData.get().setException(e);
            log.error("Failed policy monitoring for application '{}': {}", app.getName(), e.getMessage(), e);
          }
        }
        return null;
      }, applicationMonitorForkJoinPool);
      futures.add(future);
    }
    futures.forEach(CompletableFuture::join);

    long timeElapsed = System.currentTimeMillis() - start;
    log.info("Finished policy monitoring applications in {} ms", timeElapsed);
    telemetrySender.send(telemetryUtils.buildContinuousMonitoringMetricsAttributes(appsUnderContinuousMonitoringCount,
        timeElapsed / 1000, String.join(",", stagesDetectedDuringScan)));
  }

  @VisibleForTesting
  void evaluate(Application app, PolicyMonitoring policyMonitoring) throws IOException, InterruptedException {
    if (ComplianceStageType.ID.equals(policyMonitoring.getStageTypeId())) {
      evaluateSbomManagerComplianceStage(app, policyMonitoring);
      return;
    }

    long start = System.currentTimeMillis();
    log.info("Policy monitoring is enabled for application '{}' and stage '{}'", app.getName(),
        policyMonitoring.getStageTypeId());

    PolicyEvaluation lastPrimaryPolicyEvaluation = policyEvaluationDAO.getLastPrimaryByApplicationIdAndStageId(
        app.getId(), policyMonitoring.getStageTypeId());
    if (lastPrimaryPolicyEvaluation == null) {
      AuditData.get().setEvent(null);
      log.info("There is nothing to monitor for application '{}' because there is no scan for stage '{}'",
          app.getName(), policyMonitoring.getStageTypeId());
      return;
    }

    // Copy the last scan file to a new scan file that will get a new scan id.
    // The tests assume that the temp file is created in the scan directory for the given app.
    // If the location of the temp files is changed, the tests need to be updated.
    File tempScanFile = work.getScanFile(app.getId(), "tmp-" + UUID.randomUUID());

    String newScanId = null;
    try {
      cloneScanFile(tempScanFile, app, lastPrimaryPolicyEvaluation);
      boolean hasThirdPartyContent = hasThirdPartyScanContent(lastPrimaryPolicyEvaluation.getApplicationId(),
          lastPrimaryPolicyEvaluation.getScanId());
      newScanId = uploadScan(tempScanFile, app, policyMonitoring.getStageTypeId(), hasThirdPartyContent);
    }
    catch (Exception e) {
      try {
        Files.deleteIfExists(tempScanFile.toPath());
      }
      catch (IOException fileDeleteException) {
        log.warn(fileDeleteException.getMessage(), fileDeleteException);
      }

      throw e;
    }

    // Evaluate policies and send notifications
    Stage stage = new Stage(policyMonitoring.getStageTypeId());
    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluateForMonitoring(app, newScanId, stage,
        lastPrimaryPolicyEvaluation.getScanTriggerType(), lastPrimaryPolicyEvaluation.getClientScanType());
    policyAlertNotifier.sendNotifications(app, results);

    log.debug("Policy monitoring evaluated for application '{}' in {} ms", app.getName(),
        System.currentTimeMillis() - start);
  }

  @VisibleForTesting
  void evaluateSbomManagerComplianceStage(Application app, PolicyMonitoring policyMonitoring)
      throws IOException, InterruptedException
  {
    long start = System.currentTimeMillis();
    log.info("SBOM Manager Policy Monitoring is enabled for application '{}' and stage '{}'", app.getName(),
        policyMonitoring.getStageTypeId());

    ThirdPartySbomMetadata sbomMetadata = thirdPartySbomMetadataDAO.getLatestActiveByApplicationId(app.getId());
    if (sbomMetadata == null) {
      AuditData.get().setEvent(null);
      log.debug("No monitorable sbom version for application id {}", app.getId());
      return;
    }
    AuditData.get().setSbomVersion(sbomMetadata, null);

    ThirdPartyScan latestSbomVersionScan = thirdPartyScanDAO.getByThirdPartyFileId(sbomMetadata.getThirdPartyFileId());
    if (latestSbomVersionScan == null || latestSbomVersionScan.getFilteredScanFile() == null) {
      AuditData.get().setEvent(null);
      log.debug("No filtered scan file for application id {} and sbom version {}", app.getId(),
          sbomMetadata.getSbomVersion());
      return;
    }

    File filteredScanFile = new File(work.getScanDir(app.getId()), latestSbomVersionScan.getFilteredScanFile());
    if (!filteredScanFile.exists()) {
      AuditData.get().setEvent(null);
      log.debug("Missing filtered scan file {} for application id {} and sbom version {}",
          latestSbomVersionScan.getFilteredScanFile(), app.getId(), sbomMetadata.getSbomVersion());
      return;
    }

    latestSbomVersionScan.setPreviousScanId(latestSbomVersionScan.getScanId());
    String newScanId = uploadFilteredScanForComplianceStage(filteredScanFile, app);

    // Update scanId on third party scan table so we can associate the scan with the new evaluation/report
    latestSbomVersionScan.setScanId(newScanId);
    thirdPartyScanDAO.update(latestSbomVersionScan);

    // Evaluate policies and send notifications
    Stage stage = new Stage(policyMonitoring.getStageTypeId());
    PolicyEvaluation lastPolicyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(),
        latestSbomVersionScan.getPreviousScanId());
    ScanTriggerType scanTriggerType;
    ClientScanType clientScanType;
    if (lastPolicyEvaluation != null) {
      scanTriggerType = lastPolicyEvaluation.getScanTriggerType();
      clientScanType = lastPolicyEvaluation.getClientScanType();
    }
    else {
      log.debug("No latest policy evaluation for appId {}, scanId {}", app.getId(),
          latestSbomVersionScan.getPreviousScanId());
      scanTriggerType = ScanTriggerType.SBOM_UI;
      clientScanType = ClientScanType.SONATYPE_THIRD_PARTY;
    }
    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluateForMonitoring(app, newScanId, stage,
        scanTriggerType, clientScanType);
    policyAlertNotifier.sendNotifications(app, results);

    log.debug("SBOM Manager Policy Monitoring evaluated for application '{}' in {} ms", app.getName(),
        System.currentTimeMillis() - start);
  }

  private void cloneScanFile(
      File tempScanFile,
      Application app,
      PolicyEvaluation lastPrimaryPolicyEvaluation) throws IOException
  {
    String lastScanId = lastPrimaryPolicyEvaluation.getScanId();
    do {
      File lastScanFile = work.getScanFile(app.getId(), lastScanId);
      try {
        Files.copy(lastScanFile.toPath(), tempScanFile.toPath());
        break;
      }
      catch (Exception e) {
        // Each policy evaluation deletes the scan file for the previous evaluation, which may cause this exception.
        // If there is a newer scan file, try again.
        PolicyEvaluation newLastPrimaryPolicyEvaluation = policyEvaluationDAO
            .getLastPrimaryByApplicationIdAndStageId(app.getId(), lastPrimaryPolicyEvaluation.getStageTypeId());
        if (lastScanId.equals(newLastPrimaryPolicyEvaluation.getScanId())) {
          // There's no newer scan file.
          throw e;
        }

        // Try again with the new scan file.
        lastScanId = newLastPrimaryPolicyEvaluation.getScanId();
      }
    }
    while (true);
  }

  private String uploadFilteredScanForComplianceStage(File filteredScanFile, Application app)
      throws IOException, InterruptedException
  {
    ScanReceipt scanReceipt = uploader.upload(filteredScanFile, app, ComplianceStageType.ID, null /*clientUserAgent*/);
    scanReceipt.waitForReport();
    return scanReceipt.getScanId();
  }

  private String uploadScan(File tempScanFile, Application app, String stageTypeId, boolean hasThirdPartyContent)
      throws IOException, InterruptedException
  {
    ScanReceipt scanReceipt;
    if (hasThirdPartyContent) {
      scanReceipt =
          thirdPartyScanService.filterAndUpload(tempScanFile, app, stageTypeId, null /* clientUserAgent */, null);
    }
    else {
      scanReceipt = uploader.upload(tempScanFile, app, stageTypeId, null /* clientUserAgent */);
    }
    scanReceipt.waitForReport();
    String scanId = scanReceipt.getScanId();
    Files.move(tempScanFile.toPath(), work.getScanFile(app.getId(), scanId).toPath());

    return scanId;
  }

  private boolean hasThirdPartyScanContent(String appId, String scanId) {
    try {
      File file = work.getReportFile(appId, scanId);
      return Report.getEntry(file, THIRD_PARTY_BOM_JSON_FILENAME) != null;
    }
    catch (IOException e) {
      log.debug("effort fetching report data for app id {} scan id {}", appId, scanId);
      return false;
    }
  }

  private static boolean isLicensedForApplications(ProductLicense productLicense) {
    return productLicense.hasFeature(LicensedFeature.POLICY_MONITORING);
  }

  static boolean isLicensed(ProductLicense productLicense) {
    return isLicensedForApplications(productLicense);
  }
}
