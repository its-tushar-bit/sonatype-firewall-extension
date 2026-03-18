/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.Optional;
import java.util.SortedMap;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestCommentDAO;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestComment;
import com.sonatype.insight.brain.policy.PolicyEvaluationDiffService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature.PR_LINE_COMMENTING_BITBUCKET_ON_NO_CHANGE;

@Named
@Singleton
public class PullRequestCommentingService
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestCommentingService.class);

  /**
   * Policy violations with a threat level below this threshold are filleted out of the policy violation diff
   */
  public static final int MINIMUM_THREAT_LEVEL = 2;

  private final PullRequestCommentCreator pullRequestCommentCreator;

  private final SourceControlPullRequestCommentDAO pullRequestCommentDAO;

  private final PolicyEvaluationDiffService policyEvaluationDiffService;

  private final PullRequestCommentingRemediationService commentingRemediationService;

  private final Provider<PullRequestCommentingHashBuilder> hashBuilderProvider;

  private final SourceControlComponentLoader sourceControlComponentLoader;

  @Inject
  public PullRequestCommentingService(
      final PullRequestCommentCreator pullRequestCommentCreator,
      final Provider<PullRequestCommentingHashBuilder> hashBuilderProvider,
      final PolicyEvaluationDiffService policyEvaluationDiffService,
      final SourceControlPullRequestCommentDAO pullRequestCommentDAO,
      final PullRequestCommentingRemediationService commentingRemediationService,
      final SourceControlComponentLoader sourceControlComponentLoader)
  {
    this.pullRequestCommentCreator = pullRequestCommentCreator;
    this.hashBuilderProvider = hashBuilderProvider;
    this.policyEvaluationDiffService = policyEvaluationDiffService;
    this.pullRequestCommentDAO = pullRequestCommentDAO;
    this.commentingRemediationService = commentingRemediationService;
    this.sourceControlComponentLoader = sourceControlComponentLoader;
  }

  public void doCreateOrUpdatePullRequestComment(PullRequestPolicyEvaluationsDTO dto) {
    SourceControlPullRequestComment existingPullRequestComment = pullRequestCommentDAO
        .getByApplicationIdAndPullRequestIdWithoutComponent(dto.getApplicationId(), dto.getPullRequestNumber());

    try {
      Optional<PolicyViolationDiff<PolicyViolation>> policyViolationDiff = policyEvaluationDiffService
          .createPolicyViolationDiffByComponents(
              dto.getTargetPolicyEvaluation(),
              dto.getFeatureBranchPolicyEvaluation(),
              MINIMUM_THREAT_LEVEL);

      if (policyViolationDiff.isPresent()) {
        // retrieve suggested remediation map for components in the appeared violation list
        SortedMap<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap = commentingRemediationService
            .getRemediationVersionMap(policyViolationDiff.get().getAppeared(), dto.getApplicationId());

        // calculate comment content hash
        String contentHash = hashBuilderProvider.get()
            .withPolicyViolationDiff(policyViolationDiff.get())
            .withRemediationVersionMap(remediationVersionMap)
            .generateHash();

        if (existingPullRequestComment == null) { // new PR comment
          if (policyViolationDiff.get().hasAppeared() || policyViolationDiff.get().hasCleared() ||
              SourceControlProvider.BITBUCKET == dto.getGitRepositoryInfo().getProvider())
          {
            pullRequestCommentCreator
                .createPullRequestComment(dto, policyViolationDiff.get(), remediationVersionMap, contentHash);
          }
          else {
            log.info("No added or cleared violations in policy evaluation diff, and no previous PR comments for " +
                "application '{}' pull request '{}'.", dto.getApplicationId(), dto.getPullRequestNumber());
          }
        }
        else { // existing PR comment
          if (!contentHash.equals(existingPullRequestComment.getContentHash())) {
            pullRequestCommentCreator.updatePullRequestComment(dto, existingPullRequestComment,
                policyViolationDiff.get(), remediationVersionMap, contentHash);
          }
          else {
            log.info("Policy evaluations have not changed for application '{}' pull request '{}'.",
                dto.getApplicationId(), dto.getPullRequestNumber());

            // CLM-35694: handle unchanged content for Bitbucket
            if (null != dto.getGitRepositoryInfo()
                && SourceControlProvider.BITBUCKET == dto.getGitRepositoryInfo().getProvider())
            {
              handleBitbucketPullRequestWhenContentUnchanged(dto, existingPullRequestComment,
                  policyViolationDiff.get(), remediationVersionMap, contentHash);
            }
          }
        }
      }
      else {
        log.info("Unable to get the policy evaluation diff for application '{}' pull request '{}'.",
            dto.getApplicationId(), dto.getPullRequestNumber());
      }
    }
    catch (SourceControlException e) {
      throw e;
    }
    catch (Exception e) {
      throw new SourceControlException("Failed to create/update PR comment - reason: " + e.getMessage(), e);
    }
  }

  /**
   * CLM-35694 - handle Bitbucket Code Insights creation when content hash hasn't changed.
   */
  private void handleBitbucketPullRequestWhenContentUnchanged(
      final PullRequestPolicyEvaluationsDTO dto,
      final SourceControlPullRequestComment existingPullRequestComment,
      final PolicyViolationDiff<PolicyViolation> policyViolationDiff,
      final SortedMap<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap,
      final String contentHash)
  {
    try {
      if (PR_LINE_COMMENTING_BITBUCKET_ON_NO_CHANGE.isEnabled()) {
        log.debug("Updating pull request comments for Bitbucket pull request");
        pullRequestCommentCreator.updatePullRequestComment(dto, existingPullRequestComment,
            policyViolationDiff, remediationVersionMap, contentHash);
        return;
      }

      log.debug("Only handling post actions, no comments updated for Bitbucket pull request");

      SourceControlComponentDetails componentDetails = sourceControlComponentLoader
          .getSourceControlComponentDetails(dto.getApplicationId(),
              dto.getFeatureBranchPolicyEvaluation().getScanId());

      // Invoke post-comment actions without updating pull request comments
      pullRequestCommentCreator.handlePostCommentActions(dto, policyViolationDiff, componentDetails, null);

    }
    catch (Exception e) {
      log.error(
          "Failed to handle Bitbucket Code Insights when content unchanged for application '{}' pull request '{}': {}",
          dto.getApplicationId(), dto.getPullRequestNumber(), e.getMessage(), e);
    }
  }
}
