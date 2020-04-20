/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.policy.evaluator.PullRequestFeedbackDetails;
import com.sonatype.insight.brain.policy.evaluator.PullRequestLineFeedback;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.service.BaseUrl;

@Named
@Singleton
public class PullRequestFeedbackMarkupService
{
  private final ApplicationDAO applicationDAO;

  private final ReportService reportService;

  private final BaseUrl baseUrl;

  @Inject
  public PullRequestFeedbackMarkupService(
      final ApplicationDAO applicationDAO,
      final ReportService reportService,
      final BaseUrl baseUrl)
  {
    this.applicationDAO = applicationDAO;
    this.reportService = reportService;
    this.baseUrl = baseUrl;
  }

  /**
   * Creates the PR overall comment markup text based on the supplied diff and policy evaluations
   */
  public Optional<String> createMarkup(
      PolicyViolationDiff<PolicyViolation> policyViolationDiff,
      PolicyEvaluation sourceCommitPolicyEvaluation,
      PolicyEvaluation baseBranchPolicyEvaluation) throws IOException
  {
    ReportEntry reportEntry = reportService.getBomForPolicyEvaluation(sourceCommitPolicyEvaluation);
    Application application = applicationDAO.getById(sourceCommitPolicyEvaluation.getApplicationId());
    PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(reportEntry, sourceCommitPolicyEvaluation, baseBranchPolicyEvaluation,
            policyViolationDiff, application, baseUrl.getConfigured());

    return details.renderTemplateAndGetContents();
  }

  /**
   * Creates the PR line comment markup text based on the supplied diff and policy evaluations
   */
  public Optional<String> createLineMarkup(
      final List<PolicyViolation> violations,
      final String componentNameAndVersion,
      final String suggestedVersion)
  {
    PullRequestLineFeedback details =
        new PullRequestLineFeedback(violations, componentNameAndVersion, baseUrl.getConfigured(), suggestedVersion);
    return details.renderTemplateAndGetContents();
  }
}
