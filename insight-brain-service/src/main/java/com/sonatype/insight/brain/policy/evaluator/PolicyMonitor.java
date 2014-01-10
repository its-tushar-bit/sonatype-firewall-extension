/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.saas.ScanUploader;
import com.sonatype.insight.brain.service.InsightWork;

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

  private final PolicyEvaluationUtils policyEvaluationUtils;

  private final PolicyAlertNotifier policyAlertNotifier;

  @Inject
  public PolicyMonitor(InsightWork work, ScanUploader uploader, PolicyEvaluationUtils policyEvaluationUtils,
      PolicyAlertNotifier policyAlertNotifier)
  {
    this.work = work;
    this.uploader = uploader;
    this.policyEvaluationUtils = policyEvaluationUtils;
    this.policyAlertNotifier = policyAlertNotifier;
  }

  public void run() {
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

    List<Application> apps = new ApplicationDAO().getAll();
    for (Application app : apps) {
      PolicyMonitoring policyMonitoring = policyMonitoringsByOwnerId.get(app.getId());
      if (policyMonitoring == null) {
        if (app.getOrganizationId() == null) {
          continue;
        }

        policyMonitoring = policyMonitoringsByOwnerId.get(app.getOrganizationId());
        if (policyMonitoring == null) {
          continue;
        }
      }

      try {
        evaluate(app, policyMonitoring);
      }
      catch (InterruptedException e) {
        log.error(e.getMessage(), e);
        return;
      }
      catch (IOException | RuntimeException e) {
        log.error("Failed policy monitoring for application '{}': {}", app.getName(), e.getMessage(), e);
      }
    }

    log.info("Policy monitoring evaluated in {} ms", System.currentTimeMillis() - start);
  }

  private void evaluate(Application app, PolicyMonitoring policyMonitoring) throws IOException, InterruptedException {
    long start = System.currentTimeMillis();

    log.info("Policy monitoring is enabled for application '{}' and stage '{}'", app.getName(),
        policyMonitoring.getStageTypeId());

    PolicyEvaluationLog policyEvaluationLog = new PolicyEvaluationLog(work.getAuditDir(app.getId()));
    PolicyEvaluation policyEvaluation = policyEvaluationLog.lastPrimaryByStage(policyMonitoring.getStageTypeId());
    if (policyEvaluation == null) {
      log.info("There is nothing to monitor for application '{}' because there is no scan for stage '{}'",
          app.getName(), policyMonitoring.getStageTypeId());
      return;
    }

    String scanId = policyEvaluation.getScanId();
    File scanFile = work.getScanFile(app.getId(), scanId);
    ScanReceipt scanReceipt = uploader.upload(scanFile, app.getPublicId(), "rest/ci/scan");
    if (scanReceipt.getTimeToReport() != null) {
      Thread.sleep(scanReceipt.getTimeToReport() * 1000);
    }

    Stage stage = new Stage(policyMonitoring.getStageTypeId());
    List<PolicyAlert> oldAlerts;
    try {
      oldAlerts = policyEvaluationUtils.findLastPolicyAlertsForMonitoring(app.getId(), scanId);
    }
    catch (final Exception e) {
      // don't abort sending notifications if old results are corrupt or missing, just means full digest will be sent
      log.warn("Cannot load last policy evaluation results for app id {}", app.getPublicId(), e);
      oldAlerts = Collections.emptyList();
    }

    PolicyEvaluationResult policyEvaluationResult = policyEvaluationUtils.evaluateForMonitoring(app.getPublicId(),
        scanId, stage);
    List<PolicyAlert> newAlerts = policyEvaluationResult.getAlerts();
    policyAlertNotifier.sendNotifications(app.getPublicId(), app.getId(), scanId, stage, newAlerts, oldAlerts);

    log.debug("Policy monitoring evaluated for application '{}' in {} ms", app.getName(), System.currentTimeMillis()
        - start);
  }
}
