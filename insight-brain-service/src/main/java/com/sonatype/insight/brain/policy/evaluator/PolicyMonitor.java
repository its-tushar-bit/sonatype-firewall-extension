/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.service.consumption.ConsumptionContext;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.hds.ScanUploadService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.policy.evaluator.queue.EvaluationQueueConfig;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.report.LifecycleReport;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.PageIterator;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.brain.tenancy.TenantThreadPoolExecutor;
import com.sonatype.insight.brain.utils.DefaultExecutorThreadPools;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.scan.model.ClientScanType;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.report.LifecycleReport.ReportFile.THIRD_PARTY_BOM_JSON;

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

  private static final int DEFAULT_PAGE_SIZE = 10_000;

  private ExecutorService executorService;

  private final ScanPolicyEvaluator scanPolicyEvaluator;

  private final PolicyAlertNotifier policyAlertNotifier;

  private final ProductLicense productLicense;

  private final AuditRecorder auditRecorder;

  private final ScanUploadService scanUploadService;

  private final PolicyMonitoringDAO policyMonitoringDAO;

  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final ThirdPartyScanDAO thirdPartyScanDAO;

  private final Configuration configuration;

  private final ShutdownHandler shutdownHandler;

  private final TelemetrySender telemetrySender;

  private final TelemetryUtils telemetryUtils;

  private final ReportService reportService;

  private final ScanPersistenceService scanPersistenceService;

  private final ApiConfigurationService apiConfigurationService;

  @Inject
  public PolicyMonitor(
      final ScanPolicyEvaluator scanPolicyEvaluator,
      final PolicyAlertNotifier policyAlertNotifier,
      final ProductLicense productLicense,
      final AuditRecorder auditRecorder,
      final ScanUploadService scanUploadService,
      final PolicyMonitoringDAO policyMonitoringDAO,
      final ApplicationDAO applicationDAO,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final ThirdPartyScanDAO thirdPartyScanDAO,
      final Configuration configuration,
      final ShutdownHandler shutdownHandler,
      final TelemetrySender telemetrySender,
      final TelemetryUtils telemetryUtils,
      final ReportService reportService,
      final ScanPersistenceService scanPersistenceService,
      final ApiConfigurationService apiConfigurationService)
  {
    this.scanPolicyEvaluator = scanPolicyEvaluator;
    this.policyAlertNotifier = policyAlertNotifier;
    this.productLicense = productLicense;
    this.auditRecorder = auditRecorder;
    this.scanUploadService = scanUploadService;
    this.policyMonitoringDAO = policyMonitoringDAO;
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.thirdPartyScanDAO = thirdPartyScanDAO;
    this.configuration = configuration;
    this.shutdownHandler = shutdownHandler;
    this.telemetrySender = telemetrySender;
    this.telemetryUtils = telemetryUtils;
    this.reportService = reportService;
    this.scanPersistenceService = scanPersistenceService;
    this.apiConfigurationService = apiConfigurationService;
    log.debug("Created a new PolicyMonitor for tenant {}", TenantThreadLocal.getTenant());
  }

  // Visible for testing
  ExecutorService getExecutorService() {
    return executorService;
  }

  private ExecutorService initThreadPool(Configuration configuration) {
    int maxThreadCount = POLICY_MONITOR_THREADS_MAX;
    int threadCount = POLICY_MONITOR_THREADS_DEFAULT;

    Integer saasPolicyMonitorPoolSize = configuration.getSaasPolicyMonitorPoolSize();
    if (saasPolicyMonitorPoolSize != null && saasPolicyMonitorPoolSize > 0) {
      maxThreadCount = saasPolicyMonitorPoolSize;
      threadCount = saasPolicyMonitorPoolSize;
    }

    int finalThreadCount = DefaultExecutorThreadPools.getThreadCount(
        POLICY_MONITOR_THREADS_MIN,
        maxThreadCount,
        threadCount,
        "insight.threads.monitor");
    TenantThreadPoolExecutor tenantThreadPoolExecutor = new TenantThreadPoolExecutor(
        finalThreadCount,
        finalThreadCount,
        5L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(),
        new ThreadFactoryBuilder().setNameFormat("insight-thread-monitor-%d").build(),
        new AbortPolicy(),
        "policy_monitor",
        getClass().getSimpleName());
    tenantThreadPoolExecutor.allowCoreThreadTimeOut(true);

    return tenantThreadPoolExecutor;
  }

  public void run() {
    log.info("Starting policy monitoring for tenant {}", TenantThreadLocal.getTenant());

    long start = System.currentTimeMillis();

    if (policyMonitoringDAO.getCount() == 0) {
      log.info("Policy monitoring was not configured for any applications, organizations, or repositories.");
      return;
    }

    try {
      executorService = initThreadPool(configuration);
      shutdownHandler.add(executorService);

      evaluateApplications(StageTypes.getAll()
          .stream()
          .filter(stageType -> stageType != StageTypes.COMPLIANCE)
          .toArray(StageType[]::new));
      EvaluationQueueConfig evaluationQueueConfig = getEvaluationQueueConfig();
      if (!evaluationQueueConfig.enabled()) {
        log.debug("Evaluation queue disabled, executing compliance stage evaluation.");
        evaluateApplications(StageTypes.COMPLIANCE);
      }
      else {
        log.debug("Evaluation queue enabled, skipping compliance stage evaluation.");
      }
    }
    finally {
      executorService.shutdown();
      shutdownHandler.remove(executorService);
    }

    log.info("Policy monitoring evaluated in {} ms for tenant {}", System.currentTimeMillis() - start,
        TenantThreadLocal.getTenant());
  }

  private void evaluateApplications(final StageType... stageTypes) {
    if (!isLicensedForApplications(productLicense)) {
      log.debug("Not licensed for Application Policy Monitoring.");
      return;
    }
    log.debug("Licensed for Application Policy Monitoring.");

    Iterator<ApplicationWithPolicyMonitoring> monitoredApps = createApplicationWithPolicyMonitoringIterator(stageTypes);

    log.info("Starting policy monitoring of applications (page size: {})", DEFAULT_PAGE_SIZE);
    long start = System.currentTimeMillis();

    List<CompletableFuture<Void>> futures = new ArrayList<>();
    Set<String> stagesDetectedDuringScan = new HashSet<>();
    long appsUnderContinuousMonitoringCount = 0;

    while (monitoredApps.hasNext()) {
      ApplicationWithPolicyMonitoring applicationWithPolicyMonitoring = monitoredApps.next();
      Application app = applicationWithPolicyMonitoring.app();
      PolicyMonitoring policyMonitoring = applicationWithPolicyMonitoring.policyMonitoring();

      if (policyMonitoring == null || !Stage.isValidStageTypeId(policyMonitoring.getStageTypeId())) {
        continue;
      }

      appsUnderContinuousMonitoringCount++;
      final PolicyMonitoring finalPolicyMonitoring = policyMonitoring;
      stagesDetectedDuringScan.add(finalPolicyMonitoring.getStageTypeId());

      CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
        try (
            ConsumptionContext.Scope consumptionCtx =
                ConsumptionContext.scopeBackgroundJob(productLicense, app.getId());
            AuditSession session = auditRecorder.recordSystemEvent(AuditEvent.EVALUATE_APPLICATION))
        {
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
      }, executorService);
      futures.add(future);
    }
    futures.forEach(CompletableFuture::join);

    long timeElapsed = System.currentTimeMillis() - start;
    log.info("Finished policy monitoring {} applications in {} ms", appsUnderContinuousMonitoringCount, timeElapsed);
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

    PolicyEvaluation lastPrimaryPolicyEvaluation = policyEvaluationDAO.getLastPrimaryByOwnerIdAndStageId(
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
    ScanEntity tempScanEntity = scanPersistenceService.getScan(app.getId(), "tmp-" + UUID.randomUUID());

    String newScanId = null;
    try {
      cloneScanFile(tempScanEntity, app, lastPrimaryPolicyEvaluation);
      boolean hasThirdPartyContent = hasThirdPartyScanContent(lastPrimaryPolicyEvaluation.getOwnerId(),
          lastPrimaryPolicyEvaluation.getScanId());
      newScanId = uploadScan(tempScanEntity, app, policyMonitoring.getStageTypeId(), hasThirdPartyContent);
    }
    catch (Exception e) {
      try {
        scanPersistenceService.deleteScan(tempScanEntity);
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
  void evaluateSbomManagerComplianceStage(
      Application app,
      PolicyMonitoring policyMonitoring) throws IOException, InterruptedException
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

    ScanEntity filteredScanEntity =
        scanPersistenceService.getScanByName(app.getId(), latestSbomVersionScan.getFilteredScanFile());
    if (!filteredScanEntity.exists()) {
      AuditData.get().setEvent(null);
      log.debug("Missing filtered scan file {} for application id {} and sbom version {}",
          latestSbomVersionScan.getFilteredScanFile(), app.getId(), sbomMetadata.getSbomVersion());
      return;
    }

    latestSbomVersionScan.setPreviousScanId(latestSbomVersionScan.getScanId());
    String newScanId = uploadFilteredScanForComplianceStage(filteredScanEntity, app);

    // Update scanId on third party scan table so we can associate the scan with the new evaluation/report
    latestSbomVersionScan.setScanId(newScanId);
    thirdPartyScanDAO.update(latestSbomVersionScan);

    // Evaluate policies and send notifications
    Stage stage = new Stage(policyMonitoring.getStageTypeId());
    PolicyEvaluation lastPolicyEvaluation = policyEvaluationDAO.getLastByOwnerIdAndScanId(app.getId(),
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
      ScanEntity tempScanEntity,
      Application app,
      PolicyEvaluation lastPrimaryPolicyEvaluation) throws IOException
  {
    String lastScanId = lastPrimaryPolicyEvaluation.getScanId();
    do {
      ScanEntity lastScanEntity = scanPersistenceService.getScan(app.getId(), lastScanId);
      try {
        scanPersistenceService.copyScanFile(lastScanEntity, tempScanEntity);
        break;
      }
      catch (Exception e) {
        // Each policy evaluation deletes the scan file for the previous evaluation, which may cause this exception.
        // If there is a newer scan file, try again.
        PolicyEvaluation newLastPrimaryPolicyEvaluation = policyEvaluationDAO
            .getLastPrimaryByOwnerIdAndStageId(app.getId(), lastPrimaryPolicyEvaluation.getStageTypeId());
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

  private String uploadFilteredScanForComplianceStage(
      ScanEntity filteredScanEntity,
      Application app) throws IOException, InterruptedException
  {
    ScanReceipt scanReceipt =
        scanUploadService.upload(filteredScanEntity, app, ComplianceStageType.ID, ClientScanType.SONATYPE, null, null,
            null, false);
    scanReceipt.waitForReport();
    return scanReceipt.getScanId();
  }

  private String uploadScan(
      ScanEntity tempScanEntity,
      Application app,
      String stageTypeId,
      boolean hasThirdPartyContent) throws IOException, InterruptedException
  {
    ClientScanType clientScanType =
        hasThirdPartyContent ? ClientScanType.SONATYPE_THIRD_PARTY : ClientScanType.SONATYPE;
    ScanReceipt scanReceipt =
        scanUploadService.upload(tempScanEntity, app, stageTypeId, clientScanType, null, null, null, false);
    scanReceipt.waitForReport();
    String scanId = scanReceipt.getScanId();
    scanPersistenceService.moveTempScan(tempScanEntity, app.getId(), scanId);

    return scanId;
  }

  private boolean hasThirdPartyScanContent(String appId, String scanId) {
    try {
      LifecycleReport applicationReport = reportService.getReport(appId, scanId);
      return applicationReport.getEntry(THIRD_PARTY_BOM_JSON.getName()) != null;
    }
    catch (IOException e) {
      log.debug("Error fetching report data for app id {} scan id {}", appId, scanId);
      return false;
    }
  }

  private static boolean isLicensedForApplications(ProductLicense productLicense) {
    return productLicense.hasFeature(LicensedFeature.POLICY_MONITORING);
  }

  static boolean isLicensed(ProductLicense productLicense) {
    return isLicensedForApplications(productLicense);
  }

  private record ApplicationWithPolicyMonitoring(Application app, PolicyMonitoring policyMonitoring)
  {
  }

  private Iterator<ApplicationWithPolicyMonitoring> createApplicationWithPolicyMonitoringIterator(
      final StageType... stageTypes)
  {
    return new PageIterator<>(1, DEFAULT_PAGE_SIZE, (page, pageSize) -> {
      List<Application> apps = applicationDAO.getAll(page, pageSize);

      if (apps.isEmpty()) {
        return List.of();
      }

      Set<String> appIds = apps.stream().map(Application::getId).collect(Collectors.toSet());
      Map<String, PolicyMonitoring> map = policyMonitoringDAO.getByOwnerIdsAndStageTypeIdsWithInheritance(appIds,
          Arrays.stream(stageTypes).map(StageType::getId).toArray(String[]::new));

      return apps.stream()
          .map(app -> new ApplicationWithPolicyMonitoring(app, map.get(app.getId())))
          .toList();
    });
  }

  private EvaluationQueueConfig getEvaluationQueueConfig() {
    return (EvaluationQueueConfig) apiConfigurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.EVALUATION_QUEUE_CONFIG);
  }
}
