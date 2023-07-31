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
import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.git.render.ComponentFeedbackContextFactory;
import com.sonatype.insight.brain.git.render.ComponentFeedbackMDRenderer;
import com.sonatype.insight.brain.git.render.model.ComponentFeedbackContext;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.policy.evaluator.PullRequestFeedbackDetails;
import com.sonatype.insight.brain.policy.evaluator.PullRequestLineFeedback;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.telemetry.PullRequestCommentTelemetry;
import com.sonatype.nexus.scm.SourceControlProvider;

import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;

@Named
@Singleton
public class PullRequestFeedbackMarkupService
{
  private final ApplicationDAO applicationDAO;

  private final BaseUrl iqBaseUrl;

  private final ComponentFeedbackContextFactory contextFactory;

  @Inject
  public PullRequestFeedbackMarkupService(
      final ApplicationDAO applicationDAO,
      final BaseUrl iqBaseUrl,
      final ComponentFeedbackContextFactory componentFeedbackContextFactory)
  {
    this.applicationDAO = applicationDAO;
    this.iqBaseUrl = iqBaseUrl;
    this.contextFactory = componentFeedbackContextFactory;
  }

  /**
   * Creates the PR overall comment markup text based on the supplied diff and policy evaluations
   */
  public Optional<String> createMarkup(
      PolicyViolationDiff<PolicyViolation> policyViolationDiff,
      Map<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap,
      List<PullRequestLineCommentDTO> pullRequestLineComments,
      GitRepositoryInfo gitRepositoryInfo,
      int pullRequestNumber,
      PolicyEvaluation sourceCommitPolicyEvaluation,
      PolicyEvaluation baseBranchPolicyEvaluation,
      SourceControlComponentDetails componentDetails,
      PullRequestCommentTelemetry telemetry) throws IOException
  {
    Application application = applicationDAO.getById(sourceCommitPolicyEvaluation.getApplicationId());
    PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(componentDetails, sourceCommitPolicyEvaluation, baseBranchPolicyEvaluation,
            policyViolationDiff, remediationVersionMap, pullRequestLineComments, gitRepositoryInfo, pullRequestNumber,
            application, iqBaseUrl.getConfigured());

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
      final RemediationVersionDTO remediationVersion,
      final Optional<String> codeSuggestion,
      final SourceControlProvider provider,
      final String scmBaseUrl,
      final String applicationId,
      final String featureBranchScanId)
  {
    final String applicationPublicId = applicationDAO.getByIdNotNull(applicationId).getPublicId();
    final boolean supportsHtml = provider.supportsEmbeddedHtmlInMarkdown(scmBaseUrl);
    // Refer to https://sonatype.atlassian.net/browse/SDEV-365 for why we need the `supportsHtml` condition
    if (SystemConfigurationPropertyFeature.SCM_UX_IMPROVEMENTS.isEnabled() && supportsHtml && provider == GITHUB) {
      final ComponentFeedbackContext context = contextFactory.build(provider,
              violations,
              componentNameAndVersion,
              remediationVersion,
              applicationPublicId,
              featureBranchScanId,
              iqBaseUrl.getConfigured(),
              codeSuggestion
      );
      return ComponentFeedbackMDRenderer.render(context);
    }
    else {
      PullRequestLineFeedback details =
              new PullRequestLineFeedback(
                      violations,
                      componentNameAndVersion,
                      iqBaseUrl.getConfigured(),
                      remediationVersion,
                      scmBaseUrl,
                      applicationPublicId,
                      featureBranchScanId,
                      codeSuggestion);
      return details.renderTemplateAndGetContents(provider);
    }
  }
}
