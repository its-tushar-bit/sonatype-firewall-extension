/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import com.sonatype.insight.brain.features.LicensedFeature;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.InsightWork;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.8
 */
@Named
public class PolicyMonitor
{
  private static final Logger log = LoggerFactory.getLogger(PolicyMonitor.class);

  private final InsightWork work;

  private final ScanUploader uploader;

  private final ScanPolicyEvaluator scanPolicyEvaluator;

  private final PolicyAlertNotifier policyAlertNotifier;

  private final ProductLicense productLicense;

  private final AuditRecorder auditRecorder;

  @Inject
  public PolicyMonitor(InsightWork work,
                       ScanUploader uploader,
                       ScanPolicyEvaluator scanPolicyEvaluator,
                       PolicyAlertNotifier policyAlertNotifier,
                       ProductLicense productLicense,
                       AuditRecorder auditRecorder)
  {
    this.work = work;
    this.uploader = uploader;
    this.scanPolicyEvaluator = scanPolicyEvaluator;
    this.policyAlertNotifier = policyAlertNotifier;
    this.productLicense = productLicense;
    this.auditRecorder = auditRecorder;
  }

  public void run() {
    // not licensed, back on outta here
    if (!productLicense.hasFeature(LicensedFeature.POLICY_MONITORING)) {
      log.debug("Ending task, not licensed for Policy Monitoring.");
      return;
    }

    log.info("Starting policy monitoring");

    long start = System.currentTimeMillis();

    List<PolicyMonitoring> policyMonitorings = new PolicyMonitoringDAO().getAll();
    if (policyMonitorings.isEmpty()) {
      log.info("Policy monitoring was not configured for any applications or organizations.");
      return;
    }

    Map<String, PolicyMonitoring> policyMonitoringsByOwnerId = new LinkedHashMap<>();
    for (PolicyMonitoring policyMonitoring : policyMonitorings) {
      policyMonitoringsByOwnerId.put(policyMonitoring.getOwnerId(), policyMonitoring);
    }

    OwnerDAO ownerDAO = new OwnerDAO();
    List<Application> apps = new ApplicationDAO().getAll();
    for (Application app : apps) {
      PolicyMonitoring policyMonitoring = null;
      for (Owner owner : ownerDAO.walkHierarchy(app)) {
        policyMonitoring = policyMonitoringsByOwnerId.get(owner.getId());
        if (policyMonitoring != null) {
          break;
        }
      }

      if (policyMonitoring == null) {
        continue;
      }

      try (AuditSession session = auditRecorder.recordSystemEvent(AuditEvent.EVALUATE_APPLICATION)) {
        try {
          AuditData.get().setApplication(app);
          evaluate(app, policyMonitoring);
        }
        catch (InterruptedException e) {
          AuditData.get().setException(e);
          log.error(e.getMessage(), e);
          Thread.currentThread().interrupt();
          return;
        }
        catch (IOException | RuntimeException e) {
          AuditData.get().setException(e);
          log.error("Failed policy monitoring for application '{}': {}", app.getName(), e.getMessage(), e);
        }
      }
    }

    log.info("Policy monitoring evaluated in {} ms", System.currentTimeMillis() - start);
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
      newScanId = uploadScan(tempScanFile, app);
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
    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluateForMonitoring(app, newScanId, stage);
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

  private String uploadScan(File tempScanFile, Application app) throws IOException, InterruptedException {
    ScanReceipt scanReceipt = uploader.upload(tempScanFile, app);
    scanReceipt.waitForReport();
    String scanId = scanReceipt.getScanId();
    Files.move(tempScanFile.toPath(), work.getScanFile(app.getId(), scanId).toPath());

    return scanId;
  }
}
