/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.component.ComponentDisplayFilename;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDigester;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.82.0
 */
public class PolicyEvaluationDiffService
{
  private static final Logger log = LoggerFactory.getLogger(PolicyEvaluationDiffService.class);

  private final InsightWork work;

  @Inject
  public PolicyEvaluationDiffService(final InsightWork work) {
    this.work = work;
  }

  public Optional<PolicyViolationDiff<PolicyViolation>> createPolicyViolationDiff(
      final PolicyEvaluation fromEvaluation,
      final PolicyEvaluation toEvaluation)
  {
    final File fromReportFile = getReportByPolicyEvaluation(fromEvaluation);
    if (fromReportFile == null) {
      log.debug(
          "Could not find report file for 'from' scan report with commit {}, " +
              "policy evaluation id {} and application id {}",
          fromEvaluation.getCommitHash(), fromEvaluation.getId(), fromEvaluation.getApplicationId());
      return Optional.empty();
    }

    final File toReportFile = getReportByPolicyEvaluation(toEvaluation);
    if (toReportFile == null) {
      log.debug(
          "Could not find report file for 'to' scan report with commit {}, " +
              "policy evaluation id {} and application id {}",
          toEvaluation.getCommitHash(), toEvaluation.getId(), toEvaluation.getApplicationId());
      return Optional.empty();
    }

    try {
      final ReportEntry fromReportEntry = Report.getEntry(fromReportFile,
          ScanPolicyEvaluator.POLICY_ALERTS_FILENAME);
      if (fromReportEntry == null) {
        log.debug(
            "Could not find policy alerts for 'from' scan report with commit {}, " +
                "policy evaluation id {}, application id {} and scan report {}",
            fromEvaluation.getCommitHash(), fromEvaluation.getId(), fromEvaluation.getApplicationId(),
            fromReportFile.getAbsolutePath());
        return Optional.empty();
      }
      final ReportEntry toReportEntry = Report.getEntry(toReportFile,
          ScanPolicyEvaluator.POLICY_ALERTS_FILENAME);
      if (toReportEntry == null) {
        log.debug(
            "Could not find policy alerts for 'to' scan report with commit {}, " +
                "policy evaluation id {}, application id {} and scan report {}",
            toEvaluation.getCommitHash(), toEvaluation.getId(), toEvaluation.getApplicationId(),
            toReportFile.getAbsolutePath());
        return Optional.empty();
      }

      List<PolicyAlert> fromAlerts = Arrays.asList(JsonUtils.parse(fromReportEntry.buf, PolicyAlert[].class));
      List<PolicyAlert> toAlerts = Arrays.asList(JsonUtils.parse(toReportEntry.buf, PolicyAlert[].class));
      List<PolicyViolation> fromViolations = getPolicyViolations(fromEvaluation, fromAlerts);
      List<PolicyViolation> toViolations = getPolicyViolations(toEvaluation, toAlerts);

      return Optional.of(PolicyViolationDigester.digestPolicyViolations(fromViolations, toViolations));
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private File getReportByPolicyEvaluation(final PolicyEvaluation policyEvaluation) {
    if (policyEvaluation != null) {
      File reportFile = work.getReportFile(policyEvaluation.getApplicationId(), policyEvaluation.getScanId());
      if (reportFile.isFile()) {
        return reportFile;
      }
    }
    return null;
  }

  private List<PolicyViolation> getPolicyViolations(
      PolicyEvaluation policyEvaluation,
      List<PolicyAlert> allPolicyAlerts)
  {
    List<PolicyViolation> allViolations = new ArrayList<>();
    for (PolicyAlert policyAlert : allPolicyAlerts) {
      PolicyFact policyFact = policyAlert.getTrigger();
      for (ComponentFact componentFact : policyFact.getComponentFacts()) {
        PolicyViolation policyViolation =
            new PolicyViolation(policyEvaluation, policyFact.getPolicyId(), policyFact.getPolicyName(),
                policyFact.getThreatLevel(), null, componentFact.getHash(), componentFact.getComponentIdentifier(),
                componentFact.getConstraintFacts(), getFilename(componentFact));
        policyViolation.setId(policyFact.getPolicyViolationId());
        allViolations.add(policyViolation);
      }
    }
    return allViolations;
  }

  private String getFilename(ComponentFact componentFact) {
    return new ComponentDisplayFilename().addPathnames(componentFact.getPathnames()).getFilename().orElse(null);
  }
}
