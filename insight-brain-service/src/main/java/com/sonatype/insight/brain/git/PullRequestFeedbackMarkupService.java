/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.policy.PolicyEvaluationDiffService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.policy.evaluator.PullRequestFeedbackDetails;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.service.BaseUrl;

@Named
@Singleton
public class PullRequestFeedbackMarkupService
{
  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluationDiffService policyEvaluationDiffService;

  private final ReportService reportService;

  private final BaseUrl baseUrl;

  @Inject
  public PullRequestFeedbackMarkupService(
      final ApplicationDAO applicationDAO,
      final PolicyEvaluationDiffService policyEvaluationDiffService,
      final ReportService reportService,
      final BaseUrl baseUrl)
  {
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDiffService = policyEvaluationDiffService;
    this.reportService = reportService;
    this.baseUrl = baseUrl;
  }

  /**
   * computes the policy evaluation diff between the two given policy evaluations and creates the SCM markup text
   * iff new violations have appeared
   */
  public Optional<String> createMarkupIfNewViolationsHaveAppeared(
      PolicyEvaluation sourceCommitPolicyEvaluation,
      PolicyEvaluation baseBranchPolicyEvaluation) throws IOException
  {
    Optional<String> result = Optional.empty();
    Optional<PolicyViolationDiff<PolicyViolation>> policyViolationDiff = policyEvaluationDiffService
        .createPolicyViolationDiff(baseBranchPolicyEvaluation, sourceCommitPolicyEvaluation);
    if (policyViolationDiff.isPresent() && policyViolationDiff.get().hasAppeared()) {
      ReportEntry reportEntry = reportService.getBomForPolicyEvaluation(sourceCommitPolicyEvaluation);
      Application application = applicationDAO.getById(sourceCommitPolicyEvaluation.getApplicationId());
      PullRequestFeedbackDetails details = new PullRequestFeedbackDetails(
          reportEntry, baseBranchPolicyEvaluation, policyViolationDiff.get(), application, baseUrl.getConfigured());
      result = details.getContents();
    }
    return result;
  }
}
