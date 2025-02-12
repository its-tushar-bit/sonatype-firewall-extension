/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.callflow;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.experimental.PurlIdentifiersWithVulnerabilities;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreatsAdapter;
import com.sonatype.insight.brain.report.ApplicationReport;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.report.ApplicationReport.POLICY_THREATS_FILENAME;
import static com.sonatype.insight.brain.callflow.PolicyViolationReachabilityHelper.filterOnReachableSecurityViolations;
import static com.sonatype.insight.brain.callflow.PolicyViolationReachabilityHelper.updateReachabilityStatus;

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
      final PurlIdentifiersWithVulnerabilities purlIdentifiersWithVulnerabilities,
      final ApplicationReport applicationReport) throws IOException
  {
    updateReachabilityStatusForPolicyViolations(
        purlIdentifiersWithVulnerabilities.getApplicationId(),
        purlIdentifiersWithVulnerabilities.getScanId(),
        purlIdentifiersWithVulnerabilities.getVulnerabilitiesByPurlIdentifiers(),
        applicationReport
    );
  }

  public void updateReachabilityStatusForPolicyViolations(
      final String applicationId,
      final String reportId,
      final Map<PackageUrlIdentifier, Set<String>> reachableVulnerabilitiesByPurlIdentifiers,
      final ApplicationReport applicationReport) throws IOException
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

    updateReachableSecurityViolationsReachableStatus(policyViolations, reachableVulnerabilitiesByPurlIdentifiers);

    PolicyThreats policyThreats = PolicyThreatsAdapter.createPolicyThreats(policyViolations, null, null);
    applicationReport.putEntry(POLICY_THREATS_FILENAME, JsonUtils.generate(policyThreats));

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

  private void updateReachableSecurityViolationsReachableStatus(
      final List<PolicyViolation> policyViolations,
      final Map<PackageUrlIdentifier, Set<String>> reachableVulnerabilitiesByPurlIdentifiers)
  {
    // only update reachable security violation
    List<PolicyViolation> reachableSecurityViolations = filterOnReachableSecurityViolations(policyViolations);

    if (reachableSecurityViolations.isEmpty()) {
      return;
    }

    try (TransactionContext tx = policyViolationDAO.createTransactionContext()) {
      tx.begin();

      reachableSecurityViolations.forEach(policyViolation -> {
        updateReachabilityStatus(policyViolation, reachableVulnerabilitiesByPurlIdentifiers);
        policyViolationDAO.update(tx, policyViolation);
      });

      tx.commit();
    }
  }
}
