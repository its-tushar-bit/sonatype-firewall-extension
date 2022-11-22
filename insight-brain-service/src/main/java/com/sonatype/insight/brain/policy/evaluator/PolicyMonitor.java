/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
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
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.ApiFirewallService;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.ClusterLock;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.repository.RepositoryService;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.thirdparty.ThirdPartyScanService;
import com.sonatype.insight.brain.utils.ExecutorThreadPools;
import com.sonatype.insight.license.model.LicensedFeature;

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

  private static final int APPLICATION_MONITOR_THREADS_MIN = 1;

  private static final int APPLICATION_MONITOR_THREADS_MAX = 20;

  private static final int APPLICATION_MONITOR_THREADS_DEFAULT = 1;

  private final ForkJoinPool applicationMonitorForkJoinPool;

  // derived from https://docs.sonatype.com/display/ADP/Firewall+Auto+Release+Quarantine+Policy+Condition+Types
  static final int MAX_REEVALUATION_DAYS_FOR_AUTO_RELEASED = 14;

  private final InsightWork work;

  private final ScanUploader uploader;

  private final ScanPolicyEvaluator scanPolicyEvaluator;

  private final PolicyAlertNotifier policyAlertNotifier;

  private final ProductLicense productLicense;

  private final AuditRecorder auditRecorder;

  private final ThirdPartyScanService thirdPartyScanService;

  private final RepositoryPolicyEvaluator repositoryPolicyEvaluator;

  private final ApiFirewallService apiFirewallService;

  @Inject
  public PolicyMonitor(
      InsightWork work,
      ScanUploader uploader,
      ScanPolicyEvaluator scanPolicyEvaluator,
      PolicyAlertNotifier policyAlertNotifier,
      ProductLicense productLicense,
      AuditRecorder auditRecorder,
      ThirdPartyScanService thirdPartyScanService,
      RepositoryPolicyEvaluator repositoryPolicyEvaluator,
      ApiFirewallService apiFirewallService)
  {
    this.work = work;
    this.uploader = uploader;
    this.scanPolicyEvaluator = scanPolicyEvaluator;
    this.policyAlertNotifier = policyAlertNotifier;
    this.productLicense = productLicense;
    this.auditRecorder = auditRecorder;
    this.thirdPartyScanService = thirdPartyScanService;
    this.repositoryPolicyEvaluator = repositoryPolicyEvaluator;
    this.apiFirewallService = apiFirewallService;
    this.applicationMonitorForkJoinPool = initThreadPool();
  }

  private ForkJoinPool initThreadPool() {
    ForkJoinPool threadPool = ExecutorThreadPools.getInstance().createThreadPool(
        APPLICATION_MONITOR_THREADS_MIN,
        APPLICATION_MONITOR_THREADS_MAX,
        APPLICATION_MONITOR_THREADS_DEFAULT,
        "insight.threads.monitor");

    log.info("insight.threads.monitor pool-size: {}", threadPool.getParallelism());

    return threadPool;
  }

  public void run() {
    log.info("Starting policy monitoring");

    long start = System.currentTimeMillis();

    List<PolicyMonitoring> policyMonitorings = new PolicyMonitoringDAO().getAll();
    if (policyMonitorings.isEmpty()) {
      log.info("Policy monitoring was not configured for any applications, organizations, or repositories.");
      return;
    }

    Map<String, PolicyMonitoring> policyMonitoringsByOwnerId = new LinkedHashMap<>();
    for (PolicyMonitoring policyMonitoring : policyMonitorings) {
      policyMonitoringsByOwnerId.put(policyMonitoring.getOwnerId(), policyMonitoring);
    }

    evaluateApplications(policyMonitoringsByOwnerId);

    evaluateApplicableQuarantinedRepositoryComponents(policyMonitoringsByOwnerId);

    log.info("Policy monitoring evaluated in {} ms", System.currentTimeMillis() - start);
  }

  private void evaluateApplications(final Map<String, PolicyMonitoring> policyMonitoringsByOwnerId) {
    if (!isLicensedForApplications(productLicense)) {
      log.debug("Not licensed for Application Policy Monitoring.");
      return;
    }
    log.debug("Licensed for Application Policy Monitoring.");

    OwnerDAO ownerDAO = new OwnerDAO();
    List<Application> apps = new ApplicationDAO().getAll();
    log.info("Starting policy monitoring of applications");
    long start = System.currentTimeMillis();

    List<CompletableFuture<Void>> futures = new ArrayList<>();

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
      final PolicyMonitoring finalPolicyMonitoring = policyMonitoring;
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

    log.info("Finished policy monitoring applications in {} ms", System.currentTimeMillis() - start);
  }

  private void evaluateApplicableQuarantinedRepositoryComponents(
      final Map<String, PolicyMonitoring> policyMonitoringsByOwnerId)
  {
    if (!isLicensedForFirewall(productLicense)) {
      log.debug("Not licensed for Firewall Policy Monitoring.");
      return;
    }
    log.debug("Licensed for Firewall Policy Monitoring.");

    final Set<String> autoUnquarantineEnabledConditionTypes =
        apiFirewallService.getAutoUnquarantineEnabledPolicyConditionTypesIds();

    if (autoUnquarantineEnabledConditionTypes.isEmpty()) {
      log.debug("Skipping Firewall Policy Monitoring.  Auto un-quarantine condition types are not enabled.");
      return;
    }

    long start = System.currentTimeMillis();
    log.info("Starting policy monitoring of repositories");

    OwnerDAO ownerDAO = new OwnerDAO();

    List<Repository> repositories = new RepositoryDAO().getAll();
    for (Repository repository : repositories) {
      try (ClusterLock clusterLock = ClusterLock.createForRepositoryReevaluation(repository)) {
        if (clusterLock.tryLock()) {
          log.debug("Starting re-evaluation for repository {}:{} ({})", repository.getRepositoryManagerId(),
              repository.getPublicId(), repository.getId());
          reevaluateRepository(policyMonitoringsByOwnerId, autoUnquarantineEnabledConditionTypes, ownerDAO, repository);
        }
        else {
          log.debug("Skipping, re-evaluation for repository {}:{} ({}) is already in progress",
              repository.getRepositoryManagerId(), repository.getPublicId(), repository.getId());
          clusterLock.unlock();
        }
      }
      catch (Exception e) {
        log.error("An error occurred while re-evaluating repository {}:{} ({})", repository.getRepositoryManagerId(),
            repository.getPublicId(), repository.getId(), e);
        AuditData.get().setException(e);
      }
    }
    log.info("Finished policy monitoring repositories in {} ms", System.currentTimeMillis() - start);
  }

  private void reevaluateRepository(
      final Map<String, PolicyMonitoring> policyMonitoringsByOwnerId,
      final Set<String> autoUnquarantineEnabledConditionTypes,
      final OwnerDAO ownerDAO,
      final Repository repository)
  {
    PolicyMonitoring policyMonitoring = null;
    for (Owner owner : ownerDAO.walkHierarchy(repository)) {
      policyMonitoring = policyMonitoringsByOwnerId.get(owner.getId());
      if (policyMonitoring != null) {
        break;
      }
    }

    if (policyMonitoring == null || !policyMonitoring.getStageTypeId().equals(ProxyStageType.ID)) {
      return;
    }

    log.debug("Getting quarantined components supporting auto un-quarantine of repository {}", repository.getName());
    List<RepositoryComponent> applicableQuarantinedComponents =
        getApplicableQuarantinedComponents(repository, autoUnquarantineEnabledConditionTypes);
    if (applicableQuarantinedComponents.isEmpty()) {
      return;
    }

    log.debug("Starting re-evaluation for {} repository components", applicableQuarantinedComponents.size());
    Iterator<RepositoryComponent> componentIterator = applicableQuarantinedComponents.iterator();
    int totalUnquarantineCount = 0;
    while (componentIterator.hasNext()) {
      try (AuditSession session = auditRecorder.recordSystemEvent(AuditEvent.EVALUATE_REPOSITORY)) {
        totalUnquarantineCount += autoUnquarantineComponents(repository, componentIterator);
      }
    }
    log.info("Auto un-quarantined {} components of repository {}:{} ({})", totalUnquarantineCount,
        repository.getRepositoryManagerId(), repository.getPublicId(), repository.getId());
  }

  private int autoUnquarantineComponents(
      final Repository repository,
      final Iterator<RepositoryComponent> componentIterator)
  {
    RepositoryComponentEvaluationDataRequestList evaluationRequestList =
        getRepositoryEvaluationRequest(repository, componentIterator);
    try {
      auditRepositoryComponentEvaluationList(repository, evaluationRequestList);
      // Part of the policy evaluation, the component is unquarantined if it doesn't have any policy violations that
      // require quarantine.
      RepositoryComponentEvaluationDataList evaluationResults =
          repositoryPolicyEvaluator.evaluateForMonitoring(repository, evaluationRequestList);

      int unquarantinedComponentsCount = (int) evaluationResults.componentEvalResults.stream()
          .filter(componentEvaluationData -> !componentEvaluationData.quarantine).count();
      log.debug("Auto un-quarantined {} of {} components for repository {}", unquarantinedComponentsCount,
          evaluationResults.componentEvalResults.size(), repository.getName());
      return unquarantinedComponentsCount;
    }
    catch (RuntimeException e) {
      AuditData.get().setException(e);
      log.error("Failed policy monitoring for {} components of repository '{}': {}",
          evaluationRequestList.components.size(), repository.getName(), e.getMessage());
    }

    return 0;
  }

  private void auditRepositoryComponentEvaluationList(
      Repository repository,
      RepositoryComponentEvaluationDataRequestList repoComponentEvalList)
  {
    AuditData.get().setRepository(repository).setData("componentCount", repoComponentEvalList.components.size())
        .setData("evaluationCause", RepositoryComponentEvaluationDataRequestList.REEVALUATION);

    AuditData.get().setData("componentCount", repoComponentEvalList.components.size());
    if (repoComponentEvalList.cause != null) {
      AuditData.get().setData("evaluationCause", repoComponentEvalList.cause.replace('_', '-'));
    }
  }

  private List<RepositoryComponent> getApplicableQuarantinedComponents(
      final Repository repository, Set<String> autoUnquarantineEnabledConditionTypes)
  {
    Date minQuarantineDate = Date.from(Instant.now().minus(Duration.ofDays(MAX_REEVALUATION_DAYS_FOR_AUTO_RELEASED)));

    List<RepositoryComponent> quarantinedComponents =
        new RepositoryComponentDAO().getQuarantinedByRepositoryIdAndDate(repository.getId(), minQuarantineDate);

    List<RepositoryComponent> applicableQuarantinedComponents = new ArrayList<>();

    for (RepositoryComponent component : quarantinedComponents) {
      if (shouldCheckForUpdatedConditionTypes(component, autoUnquarantineEnabledConditionTypes)) {
        applicableQuarantinedComponents.add(component);
      }
    }
    return applicableQuarantinedComponents;
  }

  private RepositoryComponentEvaluationDataRequestList getRepositoryEvaluationRequest(
      Repository repository,
      Iterator<RepositoryComponent> componentIterator)
  {
    RepositoryComponentEvaluationDataRequestList evaluationDataRequests =
        new RepositoryComponentEvaluationDataRequestList(RepositoryComponentEvaluationDataRequestList.REEVALUATION);
    int componentCount = 0;
    while (componentIterator.hasNext() && componentCount < RepositoryService.MAX_REPOSITORY_EVALUATION_REQUEST_SIZE) {
      RepositoryComponent component = componentIterator.next();
      RepositoryComponentEvaluationDataRequest evaluationDataRequest =
          new RepositoryComponentEvaluationDataRequest(repository.getFormat(), component.getPathname(),
              component.getHash());
      evaluationDataRequests.components.add(evaluationDataRequest);
      componentCount++;
    }

    return evaluationDataRequests;
  }

  private boolean shouldCheckForUpdatedConditionTypes(
      final RepositoryComponent quarantinedComponent,
      final Set<String> supportedConditionTypes)
  {
    List<RepositoryPolicyViolation> violations = new RepositoryPolicyViolationDAO()
        .getByRepositoryIdAndPathname(quarantinedComponent.getRepositoryId(), quarantinedComponent.getPathname());

    for (RepositoryPolicyViolation violation : violations) {
      for (ConstraintFact constraintFact : violation.getConstraintFacts()) {
        for (ConditionFact conditionFact : constraintFact.getConditionFacts()) {
          if (supportedConditionTypes.contains(conditionFact.getConditionTypeId())) {
            return true;
          }
        }
      }
    }
    return false;
  }

  @VisibleForTesting
  void evaluate(Application app, PolicyMonitoring policyMonitoring) throws IOException, InterruptedException {
    long start = System.currentTimeMillis();

    log.info("Policy monitoring is enabled for application '{}' and stage '{}'", app.getName(),
        policyMonitoring.getStageTypeId());

    PolicyEvaluation lastPrimaryPolicyEvaluation = new PolicyEvaluationDAO()
        .getLastPrimaryByApplicationIdAndStageId(app.getId(), policyMonitoring.getStageTypeId());
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
        lastPrimaryPolicyEvaluation.getScanTriggerType());
    policyAlertNotifier.sendNotifications(app, results);

    log.debug("Policy monitoring evaluated for application '{}' in {} ms", app.getName(),
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
        PolicyEvaluation newLastPrimaryPolicyEvaluation = new PolicyEvaluationDAO()
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

  private static boolean isLicensedForFirewall(ProductLicense productLicense) {
    // not checking for FIREWALL_FOR_ARTIFACTORY at this time
    return productLicense.hasFeature(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE)
        && productLicense.hasFeature(LicensedFeature.RELEASE_INTEGRITY);
  }

  static boolean isLicensed(ProductLicense productLicense) {
    return isLicensedForApplications(productLicense) || isLicensedForFirewall(productLicense);
  }
}
