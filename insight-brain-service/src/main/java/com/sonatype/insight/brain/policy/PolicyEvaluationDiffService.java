/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import static com.sonatype.insight.brain.report.LifecycleReport.ReportFile.BOM_JSON;
import static com.sonatype.insight.brain.report.LifecycleReport.ReportFile.POLICY_ALERTS;

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
import com.sonatype.insight.brain.report.LifecycleReport;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.collections4.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.82.0
 */
@Named
@Singleton
public class PolicyEvaluationDiffService
{
  private static final Logger log = LoggerFactory.getLogger(PolicyEvaluationDiffService.class);

  private final ComponentLoaderFactory componentLoaderFactory;

  private final ReportService reportService;

  @Inject
  public PolicyEvaluationDiffService(
      final ComponentLoaderFactory componentLoaderFactory,
      final ReportService reportService)
  {
    this.componentLoaderFactory = componentLoaderFactory;
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
   * Creates a PolicyViolationDiff by comparing the policy violations between the two specified policy evaluations.
   * Only the policy violations with threat level >= minimumThreatLevel are included in the diff.
   *
   * @param fromEvaluation The policy evaluation to compare from.
   * @param toEvaluation The policy evaluation to compare to.
   * @param minimumThreatLevel The minimum threat level to filter policy violations.
   * @return An Optional containing the PolicyViolationDiff, or an empty Optional if the diff could not be created.
   */
  public Optional<PolicyViolationDiff<PolicyViolation>> createPolicyViolationDiff(
      final PolicyEvaluation fromEvaluation,
      final PolicyEvaluation toEvaluation,
      final int minimumThreatLevel)
  {
    return createPolicyViolationDiff(fromEvaluation, toEvaluation, minimumThreatLevel, false);
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
    final LifecycleReport fromLifecycleReport = getReportByPolicyEvaluation(fromEvaluation);
    if (fromLifecycleReport == null) {
      log.debug(
          "Could not find report file for 'from' scan report with commit {}, " +
              "policy evaluation id {} and application id {}",
          fromEvaluation.getCommitHash(), fromEvaluation.getId(), fromEvaluation.getApplicationId());
      return Optional.empty();
    }

    final LifecycleReport toLifecycleReport = getReportByPolicyEvaluation(toEvaluation);
    if (toLifecycleReport == null) {
      log.debug(
          "Could not find report file for 'to' scan report with commit {}, " +
              "policy evaluation id {} and application id {}",
          toEvaluation.getCommitHash(), toEvaluation.getId(), toEvaluation.getApplicationId());
      return Optional.empty();
    }

    try {
      final ReportEntry fromReportEntry = fromLifecycleReport.getEntry(POLICY_ALERTS.getName());
      if (fromReportEntry == null) {
        log.debug(
            "Could not find policy alerts for 'from' scan report with commit {}, " +
                "policy evaluation id {}, application id {} and scan report {}",
            fromEvaluation.getCommitHash(), fromEvaluation.getId(), fromEvaluation.getApplicationId(),
            fromLifecycleReport.getLocation());
        return Optional.empty();
      }
      final ReportEntry toReportEntry = toLifecycleReport.getEntry(POLICY_ALERTS.getName());
      if (toReportEntry == null) {
        log.debug(
            "Could not find policy alerts for 'to' scan report with commit {}, " +
                "policy evaluation id {}, application id {} and scan report {}",
            toEvaluation.getCommitHash(), toEvaluation.getId(), toEvaluation.getApplicationId(),
            toLifecycleReport.getLocation());
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
        Set<ComponentIdentifierAndHashComparable> fromComponents = loadComponentsFromReport(fromLifecycleReport);
        Set<ComponentIdentifierAndHashComparable> toComponents = loadComponentsFromReport(toLifecycleReport);
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

  private LifecycleReport getReportByPolicyEvaluation(final PolicyEvaluation policyEvaluation) {
    if (policyEvaluation != null) {
      try {
        LifecycleReport applicationReport =
            reportService.getReport(policyEvaluation.getApplicationId(), policyEvaluation.getScanId());
        if (applicationReport.exists()) {
          return applicationReport;
        }
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      catch (NotFoundException e) {
        log.warn("Report not found", e);
        return null;
      }
    }
    return null;
  }

  private Set<ComponentIdentifierAndHashComparable> loadComponentsFromReport(
      LifecycleReport applicationReport) throws IOException
  {
    ReportEntry bomReportEntry = applicationReport.getEntry(BOM_JSON.getName());
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
