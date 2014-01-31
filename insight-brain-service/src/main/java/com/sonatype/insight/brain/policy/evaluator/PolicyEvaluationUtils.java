/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportDownloader;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class PolicyEvaluationUtils
{
  private static final Logger log = LoggerFactory.getLogger(PolicyEvaluationUtils.class);

  public static final String MONITOR_POLICY_ALERTS_FILENAME = "monitorpolicyalerts.json";

  public static final String PRIMARY_POLICY_ALERTS_FILENAME = "primarypolicyalerts.json";

  public static final String POLICY_ALERTS_FILENAME = "policyalerts.json";

  private final InsightWork work;

  private final ReportDownloader reportDownloader;

  private ApplicationDAO applicationDAO = new ApplicationDAO();

  @Inject
  public PolicyEvaluationUtils(final InsightWork insightWork, final ReportDownloader reportDownloader) {
    this.work = insightWork;
    this.reportDownloader = reportDownloader;
  }

  public PolicyEvaluationResult evaluate(final String applicationPublicId, final String scanId, final Stage stage)
      throws IOException
  {
    return evaluate(applicationPublicId, scanId, stage, false /* forMonitoring */);
  }

  public PolicyEvaluationResult evaluateForMonitoring(String applicationPublicId, String scanId, Stage stage)
      throws IOException
  {
    return evaluate(applicationPublicId, scanId, stage, true /* forMonitoring */);
  }

  private PolicyEvaluationResult evaluate(final String applicationPublicId, final String scanId, final Stage stage,
      boolean forMonitoring) throws IOException
  {
    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    String appId = application.getId();

    final File reportFile = ReportResource.fetchReport(reportDownloader, work, appId, scanId, true);

    final PolicyDAO policyDAO = new PolicyDAO();

    final ReportEntry licenseReportEntry = Report.getEntry(reportFile, "licenses.json");
    final ReportEntry securityReportEntry = Report.getEntry(reportFile, "security.json");
    final ReportEntry bomReportEntry = Report.getEntry(reportFile, "bom.json");

    if (bomReportEntry == null || securityReportEntry == null || licenseReportEntry == null) {
      throw new BadRequestException("Unable to evaluate policy, the scan " + scanId + " could not be processed");
    }

    // add new entry in the rolling log (TODO: populate invoker's details)
    PolicyEvaluationLog evalLog = new PolicyEvaluationLog(work.getAuditDir(appId));
    boolean isReevaluation = (evalLog.lastByScan(scanId) != null);
    evalLog.add(new PolicyEvaluation(stage, scanId, isReevaluation, forMonitoring), "anonymous", "127.0.0.1");

    final List<Component> components = new ComponentDAO().getAll(application, licenseReportEntry.buf,
        securityReportEntry.buf, bomReportEntry.buf);

    final List<PolicyAlert> alerts = new PolicyEvaluator().evaluate(appId, stage, policyDAO, components, forMonitoring);

    byte[] alertsFileContent = JsonUtils.generate(JsonUtils.aaData(alerts));
    if (forMonitoring) {
      Report.putEntry(reportFile, MONITOR_POLICY_ALERTS_FILENAME, alertsFileContent);
    }
    else {
      Report.putEntry(reportFile, POLICY_ALERTS_FILENAME, alertsFileContent);
    }
    if (!isReevaluation) {
      Report.putEntry(reportFile, PRIMARY_POLICY_ALERTS_FILENAME, alertsFileContent);
    }

    Report.putEntry(reportFile, "policythreats.json", JsonUtils.generate(analyzeThreats(alerts)));

    ReportResource.flushReportChanges(appId, scanId); // ensure policy count is recalculated on fetch

    final PolicyEvaluationResult policyEvaluationResult = new PolicyEvaluationResult();
    policyEvaluationResult.setAlerts(alerts);
    calculateCounters(policyEvaluationResult);
    policyEvaluationResult.setReevaluation(isReevaluation);

    return policyEvaluationResult;
  }

  public void calculateCounters(PolicyEvaluationResult policyEvaluationResult) {
    final Map<String, Integer> componentThreatLevels = new HashMap<String, Integer>();
    for (final PolicyAlert alert : policyEvaluationResult.getAlerts()) {
      final PolicyFact trigger = alert.getTrigger();
      final int policyThreatLevel = trigger.getThreatLevel();
      for (final ComponentFact component : trigger.getComponentFacts()) {
        final String id = component.getComponentId();
        final Integer level = componentThreatLevels.get(id);
        if (level == null || level < policyThreatLevel) {
          componentThreatLevels.put(id, policyThreatLevel);
        }
      }
    }
    int criticalCount = 0, severeCount = 0, moderateCount = 0;
    for (final int level : componentThreatLevels.values()) {
      if (level >= 8) {
        criticalCount++;
      }
      else if (level >= 4) {
        severeCount++;
      }
      else if (level >= 2) {
        moderateCount++;
      }
    }

    policyEvaluationResult.setAffectedComponentCount(componentThreatLevels.size());
    policyEvaluationResult.setCriticalComponentCount(criticalCount);
    policyEvaluationResult.setSevereComponentCount(severeCount);
    policyEvaluationResult.setModerateComponentCount(moderateCount);
  }

  public List<PolicyAlert> findLastPolicyAlertsForMonitoring(String appId, String scanId)
      throws IOException
  {
    File reportFile = ReportResource.fetchReport(reportDownloader, work, appId, scanId, true /* waitForReport */);
    ReportEntry reportEntry = Report.getEntry(reportFile, MONITOR_POLICY_ALERTS_FILENAME);
    if (reportEntry == null) {
      reportEntry = getPrimaryPolicyAlertsReportEntry(reportFile, appId, scanId);
    }
    if (reportEntry != null) {
      return Arrays.asList(JsonUtils.parse(reportEntry.buf, PolicyAlert[].class));
    }
    return Collections.emptyList();
  }

  public List<PolicyAlert> findLastPrimaryPolicyAlerts(final String applicationPublicId, String appId, final Stage stage)
      throws IOException
  {
    PolicyEvaluationLog evalLog = new PolicyEvaluationLog(work.getAuditDir(appId));

    // retrieve last known scanId for stage
    PolicyEvaluation lastPrimaryPolicyEvaluation = evalLog.lastPrimaryByStage(stage.getStageTypeId());
    final String lastScanId = (lastPrimaryPolicyEvaluation != null) ? lastPrimaryPolicyEvaluation.getScanId() : null;

    if (!StringUtils.isBlank(lastScanId)) {
      try {
        final File reportFile = ReportResource
            .fetchReport(reportDownloader, work, appId, lastScanId, true /* waitForReport */);
        ReportEntry reportEntry = getPrimaryPolicyAlertsReportEntry(reportFile, appId, lastScanId);
        if (reportEntry != null) {
          return Arrays.asList(JsonUtils.parse(reportEntry.buf, PolicyAlert[].class));
        }
      }
      catch (final Exception e) {
        // don't abort sending notifications if old results are corrupt, just means full digest will be sent
        log.warn("Cannot load last policy evaluation results for app id {}", applicationPublicId, e);
      }
    }
    return Collections.emptyList();
  }

  private ReportEntry getPrimaryPolicyAlertsReportEntry(File reportFile, String appId, String scanId)
      throws IOException
  {
    ReportEntry reportEntry = Report.getEntry(reportFile, PRIMARY_POLICY_ALERTS_FILENAME);
    if (reportEntry == null) {
      // Prior to 1.6, reports did not have a PRIMARY_POLICY_ALERTS_FILENAME, so fall back to
      // POLICY_ALERTS_FILENAME.
      log.info("Could not find {} for app id {}, scan id {}", PRIMARY_POLICY_ALERTS_FILENAME, appId, scanId);
      reportEntry = Report.getEntry(reportFile, POLICY_ALERTS_FILENAME);
    }

    return reportEntry;
  }

  public List<PolicyAlert> findPolicyAlerts(final String appId, final String scanId) throws IOException {
    final File reportFile = ReportResource.getReport(work, appId, scanId);
    if (reportFile != null) {
      final ReportEntry reportEntry = Report.getEntry(reportFile, POLICY_ALERTS_FILENAME);
      if (reportEntry != null) {
        return Arrays.asList(JsonUtils.parse(reportEntry.buf, PolicyAlert[].class));
      }
    }
    return Collections.emptyList();
  }

  private static ObjectNode analyzeThreats(final List<PolicyAlert> policyAlerts) {
    final Map<String, JsonNode> componentThreats = new HashMap<String, JsonNode>();
    for (final PolicyAlert alert : policyAlerts) {
      final PolicyFact trigger = alert.getTrigger();
      final int threatLevel = trigger.getThreatLevel();
      for (final ComponentFact component : trigger.getComponentFacts()) {
        final String id = component.getComponentId();
        ObjectNode threat = (ObjectNode) componentThreats.get(id);
        if (threat == null) {
          threat = JsonUtils.asTree(component);
          threat.remove("constraintFacts");
          componentThreats.put(id, threat);
        }
        if (threatLevel > threat.path("policyThreatLevel").asInt(-1)) {
          threat.put("policyId", trigger.getPolicyId());
          threat.put("policyName", trigger.getPolicyName());
          threat.put("policyThreatLevel", threatLevel);
        }
      }
    }
    return JsonUtils.aaDataNode(componentThreats.values());
  }
}
