/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
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
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.telemetry.PullRequestCommentTelemetry;
import com.sonatype.nexus.scm.SourceControlProvider;

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
      Map<ComponentIdentifier, String> remediationVersionMap,
      List<PullRequestLineCommentDTO> pullRequestLineComments,
      GitRepositoryInfo gitRepositoryInfo,
      int pullRequestNumber, 
      PolicyEvaluation sourceCommitPolicyEvaluation,
      PolicyEvaluation baseBranchPolicyEvaluation,
      PullRequestCommentTelemetry telemetry) throws IOException
  {
    ReportEntry reportEntry = reportService.getBomForPolicyEvaluation(sourceCommitPolicyEvaluation);
    Application application = applicationDAO.getById(sourceCommitPolicyEvaluation.getApplicationId());
    PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(reportEntry, sourceCommitPolicyEvaluation, baseBranchPolicyEvaluation,
            policyViolationDiff, remediationVersionMap, pullRequestLineComments, gitRepositoryInfo, pullRequestNumber,
            application, baseUrl.getConfigured());

    Optional<String> optionalString = details.renderTemplateAndGetContents();
    telemetry.newViolationsComponentCount = details.getNewViolationsComponentCount();
    telemetry.clearedViolationsComponentCount = details.getClearedViolationsComponentCount();

    return optionalString;
  }

  /**
   * Creates the PR line comment markup text based on the supplied diff and policy evaluations
   * Optionally embed html in the response (assumes that the caller knows whether or not the underlying
   * SCM supports this).
   */
  public Optional<String> createLineMarkup(
      final List<PolicyViolation> violations,
      final String componentNameAndVersion,
      final String suggestedVersion, 
      final SourceControlProvider provider)
  {
    PullRequestLineFeedback details =
        new PullRequestLineFeedback(violations, componentNameAndVersion, baseUrl.getConfigured(), suggestedVersion);
    return details.renderTemplateAndGetContents(provider);
  }
}
