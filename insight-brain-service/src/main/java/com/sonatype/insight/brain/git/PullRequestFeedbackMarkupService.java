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
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.development.prioritization.DevelopmentPrioritiesUtilsService;
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
import static com.sonatype.nexus.scm.SourceControlProvider.GITLAB;

@Named
@Singleton
public class PullRequestFeedbackMarkupService
{
  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final BaseUrl iqBaseUrl;

  private final ComponentFeedbackContextFactory contextFactory;

  private final ScmReducedSecurityService scmReducedSecurityService;

  @Inject
  public PullRequestFeedbackMarkupService(
      final ApplicationDAO applicationDAO,
      final OrganizationDAO organizationDAO,
      final BaseUrl iqBaseUrl,
      final ComponentFeedbackContextFactory componentFeedbackContextFactory,
      final ScmReducedSecurityService scmReducedSecurityService)
  {
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.iqBaseUrl = iqBaseUrl;
    this.contextFactory = componentFeedbackContextFactory;
    this.scmReducedSecurityService = scmReducedSecurityService;
  }

  /**
   * Creates the PR overall comment markup text based on the supplied diff and policy evaluations
   */
  public Optional<String> createMarkup(
      final PolicyViolationDiff<PolicyViolation> policyViolationDiff,
      final Map<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap,
      final List<PullRequestLineCommentDTO> pullRequestLineComments,
      final GitRepositoryInfo gitRepositoryInfo,
      final int pullRequestNumber,
      final PolicyEvaluation sourceCommitPolicyEvaluation,
      final PolicyEvaluation baseBranchPolicyEvaluation,
      final SourceControlComponentDetails componentDetails,
      final PullRequestCommentTelemetry telemetry,
      final boolean scmImprovementsEnabled,
      final DevelopmentPrioritiesUtilsService developmentPrioritiesUtilsService) throws IOException
  {
    Application application = applicationDAO.getById(sourceCommitPolicyEvaluation.getApplicationId());
    boolean reducedSecurityData = scmReducedSecurityService.isReducedSecurityData(application.getId());
    PullRequestFeedbackDetails details =
        new PullRequestFeedbackDetails(componentDetails, sourceCommitPolicyEvaluation, baseBranchPolicyEvaluation,
            policyViolationDiff, remediationVersionMap, pullRequestLineComments, gitRepositoryInfo, pullRequestNumber,
            application, iqBaseUrl.getConfigured(), scmImprovementsEnabled, organizationDAO,
            developmentPrioritiesUtilsService, reducedSecurityData);

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
      final String featureBranchScanId,
      final boolean scmImprovementsEnabled)
  {
    final String applicationPublicId = applicationDAO.getByIdNotNull(applicationId).getPublicId();
    boolean reducedSecurityData = scmReducedSecurityService.isReducedSecurityData(applicationId);
    final boolean supportsHtml = provider.supportsEmbeddedHtmlInMarkdown(scmBaseUrl);
    // Refer to https://sonatype.atlassian.net/browse/SDEV-365 for why we need the `supportsHtml` condition
    if (supportsHtml &&
            (provider == GITHUB || provider == GITLAB)) {
      final ComponentFeedbackContext context = contextFactory.build(provider,
              violations,
              componentNameAndVersion,
              remediationVersion,
              applicationPublicId,
              featureBranchScanId,
              iqBaseUrl.getConfigured(),
              scmImprovementsEnabled ? codeSuggestion : Optional.empty(),
              reducedSecurityData
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
                      codeSuggestion,
                      scmImprovementsEnabled,
                      organizationDAO,
                      reducedSecurityData);
      return details.renderTemplateAndGetContents(provider);
    }
  }
}
