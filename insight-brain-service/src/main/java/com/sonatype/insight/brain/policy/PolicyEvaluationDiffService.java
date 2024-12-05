/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoader;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.model.policy.ComponentIdentifierAndHashComparable;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.policy.evaluator.ComponentIdentifierAndHashComparator;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertUtil;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDigester;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.report.ReportUtils;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.commons.collections4.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.82.0
 */
public class PolicyEvaluationDiffService
{
  private static final Logger log = LoggerFactory.getLogger(PolicyEvaluationDiffService.class);

  private final ComponentLoaderFactory componentLoaderFactory;

  private final ReportUtils reportUtils;

  private final ReportService reportService;

  @Inject
  public PolicyEvaluationDiffService(
      final ComponentLoaderFactory componentLoaderFactory,
      final ReportUtils reportUtils,
      final ReportService reportService)
  {
    this.componentLoaderFactory = componentLoaderFactory;
    this.reportUtils = reportUtils;
    this.reportService = reportService;
  }

  /**
   * Creates a PolicyViolationDiff by comparing the policy violations between the two specified policy evaluations.
   */
  public Optional<PolicyViolationDiff<PolicyViolation>> createPolicyViolationDiff(
      final PolicyEvaluation fromEvaluation,
      final PolicyEvaluation toEvaluation)
  {
    return createPolicyViolationDiff(fromEvaluation, toEvaluation, 0, false);
  }

  /**
   * Creates a PolicyViolationDiff by comparing the components between the two specified policy evaluations.
   * The policy violations that are the same are not populated in the diff (not needed at this time).
   * Only the policy violations with threat level >= minimumThreatLevel are included in the diff.
   */
  public Optional<PolicyViolationDiff<PolicyViolation>> createPolicyViolationDiffByComponents(
      final PolicyEvaluation fromEvaluation,
      final PolicyEvaluation toEvaluation,
      final int minimumThreatLevel)
  {
    return createPolicyViolationDiff(fromEvaluation, toEvaluation, minimumThreatLevel, true);
  }

  private Optional<PolicyViolationDiff<PolicyViolation>> createPolicyViolationDiff(
      final PolicyEvaluation fromEvaluation,
      final PolicyEvaluation toEvaluation,
      final int minimumThreatLevel,
      boolean byComponents)
  {
    final Report fromReportFile = getReportByPolicyEvaluation(fromEvaluation);
    if (fromReportFile == null) {
      log.debug(
          "Could not find report file for 'from' scan report with commit {}, " +
              "policy evaluation id {} and application id {}",
          fromEvaluation.getCommitHash(), fromEvaluation.getId(), fromEvaluation.getApplicationId());
      return Optional.empty();
    }

    final Report toReportFile = getReportByPolicyEvaluation(toEvaluation);
    if (toReportFile == null) {
      log.debug(
          "Could not find report file for 'to' scan report with commit {}, " +
              "policy evaluation id {} and application id {}",
          toEvaluation.getCommitHash(), toEvaluation.getId(), toEvaluation.getApplicationId());
      return Optional.empty();
    }

    try {
      final ReportEntry fromReportEntry = reportUtils.getEntry(fromReportFile,
          ScanPolicyEvaluator.POLICY_ALERTS_FILENAME);
      if (fromReportEntry == null) {
        log.debug(
            "Could not find policy alerts for 'from' scan report with commit {}, " +
                "policy evaluation id {}, application id {} and scan report {}",
            fromEvaluation.getCommitHash(), fromEvaluation.getId(), fromEvaluation.getApplicationId(),
            fromReportFile.getLocation());
        return Optional.empty();
      }
      final ReportEntry toReportEntry = reportUtils.getEntry(toReportFile,
          ScanPolicyEvaluator.POLICY_ALERTS_FILENAME);
      if (toReportEntry == null) {
        log.debug(
            "Could not find policy alerts for 'to' scan report with commit {}, " +
                "policy evaluation id {}, application id {} and scan report {}",
            toEvaluation.getCommitHash(), toEvaluation.getId(), toEvaluation.getApplicationId(),
            toReportFile.getLocation());
        return Optional.empty();
      }

      List<PolicyAlert> fromAlerts = Arrays.asList(JsonUtils.parse(fromReportEntry.buf, PolicyAlert[].class));
      List<PolicyAlert> toAlerts = Arrays.asList(JsonUtils.parse(toReportEntry.buf, PolicyAlert[].class));
      List<PolicyViolation> fromViolations =
          PolicyAlertUtil.getPolicyViolationsFromAlertsAndEvaluation(fromEvaluation, fromAlerts, minimumThreatLevel);
      List<PolicyViolation> toViolations =
          PolicyAlertUtil.getPolicyViolationsFromAlertsAndEvaluation(toEvaluation, toAlerts, minimumThreatLevel);

      PolicyViolationDiff<PolicyViolation> policyViolationDiff;
      if (byComponents) {
        Set<ComponentIdentifierAndHashComparable> fromComponents = loadComponentsFromReport(fromReportFile);
        Set<ComponentIdentifierAndHashComparable> toComponents = loadComponentsFromReport(toReportFile);
        Set<ComponentIdentifierAndHashComparable> addedComponents = SetUtils.difference(toComponents, fromComponents);
        Set<ComponentIdentifierAndHashComparable> removedComponents = SetUtils.difference(fromComponents, toComponents);

        policyViolationDiff = new PolicyViolationDiff<>();
        policyViolationDiff.addAppeared(filterPolicyViolationsForComponents(toViolations, addedComponents));
        policyViolationDiff.addCleared(filterPolicyViolationsForComponents(fromViolations, removedComponents));
      }
      else {
        policyViolationDiff = PolicyViolationDigester.digestPolicyViolations(fromViolations, toViolations);
      }
      return Optional.of(policyViolationDiff);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Report getReportByPolicyEvaluation(final PolicyEvaluation policyEvaluation) {
    if (policyEvaluation != null) {
      try {
        Report reportFile =
            reportService.getReport(policyEvaluation.getApplicationId(), policyEvaluation.getScanId());
        if (reportFile.exists()) {
          return reportFile;
        }
      }
      catch (NotFoundException e) {
        log.warn("Report not found", e);
        return null;
      }
    }
    return null;
  }

  private Set<ComponentIdentifierAndHashComparable> loadComponentsFromReport(Report reportFile)
      throws IOException
  {
    ReportEntry bomReportEntry = reportUtils.getEntry(reportFile, ReportUtils.BOM_JSON_FILENAME);
    ComponentLoader componentLoader = componentLoaderFactory.createComponentLoader(null);
    Set<ComponentIdentifierAndHashComparable> result = new TreeSet<>(ComponentIdentifierAndHashComparator.COMPARATOR);
    result.addAll(componentLoader.getAll(null /* license data */, null /* security data */, bomReportEntry.buf,
        null /* dependency data */));
    return result;
  }

  private List<PolicyViolation> filterPolicyViolationsForComponents(
      List<PolicyViolation> policyViolations,
      Set<ComponentIdentifierAndHashComparable> components)
  {
    List<PolicyViolation> result = new ArrayList<>();
    for (PolicyViolation policyViolation : policyViolations) {
      // Both PolicyViolation and Component implement ComponentIdentifierAndHashComparable,
      // so we can compare the component hash and identifier across components and policy violations.
      if (components.contains(policyViolation)) {
        result.add(policyViolation);
      }
    }

    return result;
  }
}
