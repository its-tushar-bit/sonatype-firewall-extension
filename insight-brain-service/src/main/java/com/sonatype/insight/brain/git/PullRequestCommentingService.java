/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.Optional;
import java.util.SortedMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestCommentDAO;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestComment;
import com.sonatype.insight.brain.policy.PolicyEvaluationDiffService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  @Inject
  public PullRequestCommentingService(
      final PullRequestCommentCreator pullRequestCommentCreator,
      final Provider<PullRequestCommentingHashBuilder> hashBuilderProvider,
      final PolicyEvaluationDiffService policyEvaluationDiffService,
      final SourceControlPullRequestCommentDAO pullRequestCommentDAO,
      final PullRequestCommentingRemediationService commentingRemediationService)
  {
    this.pullRequestCommentCreator = pullRequestCommentCreator;
    this.hashBuilderProvider = hashBuilderProvider;
    this.policyEvaluationDiffService = policyEvaluationDiffService;
    this.pullRequestCommentDAO = pullRequestCommentDAO;
    this.commentingRemediationService = commentingRemediationService;
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
              SourceControlProvider.BITBUCKET == dto.getGitRepositoryInfo().getProvider()) {
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
}
