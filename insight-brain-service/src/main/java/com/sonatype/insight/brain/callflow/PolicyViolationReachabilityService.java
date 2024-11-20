/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.callflow;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.ReachabilityStatus;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreatsAdapter;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.clm.dto.model.policy.TriggerReference.Type.SECURITY_VULNERABILITY_REFID;

@Named
public class PolicyViolationReachabilityService
{
  private static final Logger logger = LoggerFactory.getLogger(PolicyViolationReachabilityService.class);

  private final PolicyViolationDAO policyViolationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  @Inject
  public PolicyViolationReachabilityService(
      PolicyViolationDAO policyViolationDAO,
      PolicyEvaluationDAO policyEvaluationDAO)
  {
    this.policyViolationDAO = policyViolationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
  }

  public void updateReachabilityStatusForPolicyViolations(
      final String applicationId,
      final String reportId,
      final Map<PackageUrlIdentifier, Set<String>> reachableVulnerabilitiesByPurlIdentifiers,
      final File reportFile) throws IOException
  {
    logger.info("Updating policy violations with reachability data for applicationId: {}, reportId: {}", applicationId,
        reportId);

    String stageId = getStageIdFromReportId(applicationId, reportId);
    if (stageId == null) {
      throw new BadRequestException("No stage id found for the report id: " + reportId);
    }

    List<PolicyViolation> policyViolations =
        policyViolationDAO.getUnfixedByApplicationIdAndStageId(applicationId, stageId);
    policyViolationDAO.loadConstraintFacts(policyViolations);
    logger.debug("Retrieved {} unfixed policy violations for applicationId: {}, stageId: {}", policyViolations.size(),
        applicationId, stageId);
    updateMavenSecurityViolationsReachableStatus(policyViolations, reachableVulnerabilitiesByPurlIdentifiers);

    PolicyThreats policyThreats = PolicyThreatsAdapter.createPolicyThreats(policyViolations, null, null);
    Report.putEntry(reportFile, Report.POLICY_THREATS, JsonUtils.generate(policyThreats));

    logger.info("Finished updating policy violations with reachability data for applicationId: {}, reportId: {}",
        applicationId, reportId);
  }

  private String getStageIdFromReportId(String applicationId, String reportId) {
    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(applicationId, reportId);
    if (policyEvaluation != null) {
      return policyEvaluation.getStageTypeId();
    }
    return null;
  }

  private void updateMavenSecurityViolationsReachableStatus(
      List<PolicyViolation> policyViolations,
      Map<PackageUrlIdentifier, Set<String>> reachableVulnerabilitiesByPurlIdentifiers)
  {
    List<PolicyViolation> filteredPolicyViolations =
        policyViolations.stream().filter(this::isMavenSecurityViolation).toList();

    try (TransactionContext tx = policyViolationDAO.createTransactionContext()) {
      tx.begin();
      filteredPolicyViolations.forEach(policyViolation -> {
        updateReachabilityStatus(policyViolation, reachableVulnerabilitiesByPurlIdentifiers);
        policyViolationDAO.update(tx, policyViolation);
      });
      tx.commit();
    }
  }

  private boolean isMavenSecurityViolation(PolicyViolation policyViolation) {
    return PolicyThreatCategory.SECURITY.equals(policyViolation.getThreatCategory()) &&
        policyViolation.getComponentIdentifier().isMaven();
  }

  private void updateReachabilityStatus(
      PolicyViolation policyViolation,
      Map<PackageUrlIdentifier, Set<String>> reachableVulnerabilitiesByPurlIdentifiers)
  {
    boolean isReachable = isVulnerabilityReachable(policyViolation, reachableVulnerabilitiesByPurlIdentifiers);
    policyViolation
        .setReachabilityStatus(isReachable ? ReachabilityStatus.REACHABLE : ReachabilityStatus.NON_REACHABLE);
  }

  private boolean isVulnerabilityReachable(
      PolicyViolation policyViolation,
      Map<PackageUrlIdentifier, Set<String>> reachableVulnerabilitiesByPurlIdentifiers)
  {
    return policyViolation.getConstraintFacts().stream()
        .flatMap(constraintFact -> constraintFact.getConditionFacts().stream())
        .map(ConditionFact::getReference)
        .filter(Objects::nonNull)
        .filter(triggerReference -> triggerReference.getType().equals(SECURITY_VULNERABILITY_REFID))
        .map(TriggerReference::getValue)
        .filter(value -> value != null && !value.isEmpty())
        .anyMatch(vulnerabilityId -> isVulnerabilityReachable(vulnerabilityId, policyViolation,
            reachableVulnerabilitiesByPurlIdentifiers));
  }

  private boolean isVulnerabilityReachable(
      String vulnerabilityId,
      PolicyViolation policyViolation,
      Map<PackageUrlIdentifier, Set<String>> vulnerabilitiesByPurlIdentifiers)
  {
    Set<String> vulnerabilities = vulnerabilitiesByPurlIdentifiers.get(
        PackageUrlIdentifier.fromComponentIdentifier(policyViolation.getComponentIdentifier()));

    if (vulnerabilities == null) {
      return false;
    }

    return vulnerabilities.stream()
        .map(String::toLowerCase)
        .collect(Collectors.toSet())
        .contains(vulnerabilityId.toLowerCase());
  }
}
