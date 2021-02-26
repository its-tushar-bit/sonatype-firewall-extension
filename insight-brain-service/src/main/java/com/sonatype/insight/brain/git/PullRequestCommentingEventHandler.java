/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEvent;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.common.eventbus.Subscribe;
import io.dropwizard.lifecycle.Managed;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class PullRequestCommentingEventHandler
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestCommentingEventHandler.class);

  /**
   * Policy violations with a threat level below this threshold are filleted out of the policy violation diff
   */
  public static final int MINIMUM_THREAT_LEVEL = 2;

  private final PullRequestCommentingService pullRequestCommentingService;

  private final SourceControlUtils sourceControlUtils;

  private final SourceControlEventPublisher sourceControlEventPublisher;

  private final AsyncEventBus asyncEventBus;

  private final ProductLicense productLicense;

  private final InsightConfig insightConfig;

  private final PullRequestPolicyEvaluationResolver pullRequestPolicyEvaluationResolver;

  @Inject
  public PullRequestCommentingEventHandler(
      final PullRequestCommentingService pullRequestCommentingService,
      final SourceControlUtils sourceControlUtils,
      final SourceControlEventPublisher sourceControlEventPublisher,
      final AsyncEventBus asyncEventBus,
      final ProductLicense productLicense,
      final InsightConfig insightConfig,
      final PullRequestPolicyEvaluationResolver pullRequestPolicyEvaluationResolver)
  {
    this.pullRequestCommentingService = pullRequestCommentingService;
    this.sourceControlUtils = sourceControlUtils;
    this.sourceControlEventPublisher = sourceControlEventPublisher;
    this.asyncEventBus = asyncEventBus;
    this.productLicense = productLicense;
    this.insightConfig = insightConfig;
    this.pullRequestPolicyEvaluationResolver = pullRequestPolicyEvaluationResolver;
  }

  @Override
  public void start() throws Exception {
    asyncEventBus.register(this);
  }

  @Override
  public void stop() throws Exception {
    asyncEventBus.unregister(this);
  }

  /**
   * This method is for the 'immediate flow' for pull request commenting of policy violation diffs between the
   * development branch commit that triggered the policy evaluation (which then issued this event) and the most recently
   * available policy evaluation for the source control configured base branch for the associated application.
   *
   * @param event ApplicationEvaluation event that triggered this call
   */
  @Subscribe
  public void onApplicationEvaluation(ApplicationEvaluationEvent event) {
    if (!insightConfig.isFeatureEnabled(Feature.PR_COMMENTING)) {
      return;
    }
    if (!checkLicense()) {
      log.debug("License does not support SourceControl automation features");
      return;
    }

    if (eventHasCommitHashAndScmIsEnabled(event)) {
      String applicationId = event.ownerId;
      GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(applicationId);

      if (!gitRepositoryInfo.provider.supportsPullRequestCommenting()) {
        log.debug("'{}' not currently supported for pull request commenting", gitRepositoryInfo.provider.toString());
      }
      else {
        SourceControlEvent sourceControlEvent = new SourceControlEvent()
            .forApplicationEvaluation()
            .setApplicationId(applicationId)
            .setCommitHash(event.commitHash)
            .setPolicyEvaluationId(event.policyEvaluationId)
            .setInitiator(event.initiator)
            .setCreateTime(new Date());

        sourceControlEventPublisher.publishEvent(sourceControlEvent);
        log.debug("Persisted source control event '{}' for application '{}' and commit '{}'",
            sourceControlEvent.getEventType(), applicationId, event.commitHash);
      }
    }
  }

  public void onApplicationEvaluation(SourceControlEvent event) {
    String applicationId = event.getApplicationId();
    GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(applicationId);

    List<PullRequestPolicyEvaluationsDTO> pullRequestPolicyEvaluationsDTOs =
        pullRequestPolicyEvaluationResolver.resolveForPolicyEvaluation(applicationId, gitRepositoryInfo,
            event.getPolicyEvaluationId(), event.getCommitHash());

    pullRequestPolicyEvaluationsDTOs.forEach(pullRequestCommentingService::doCreateOrUpdatePullRequestComment);
  }

  public void onDiscoveredPullRequest(SourceControlEvent event) {
    String applicationId = event.getApplicationId();
    GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(applicationId);

    PullRequestPolicyEvaluationsDTO pullRequestPolicyEvaluationsDTO = pullRequestPolicyEvaluationResolver
        .resolveForPullRequest(applicationId, gitRepositoryInfo, event.getPullRequestNumber(), event.getBranchName(),
            event.getCommitHash());

    if (null != pullRequestPolicyEvaluationsDTO) {
      pullRequestCommentingService.doCreateOrUpdatePullRequestComment(pullRequestPolicyEvaluationsDTO);
    }
  }

  private boolean eventHasCommitHashAndScmIsEnabled(ApplicationEvaluationEvent event) {
    boolean isOk = true;
    String applicationId = event.ownerId;
    if (StringUtils.isBlank(event.commitHash)) {
      log.debug(
          "no commit hash : skipping PR commenting for application '{}' with policy evaluation '{}'",
          applicationId,
          event.policyEvaluationId);
      isOk = false;
    }
    else if (!sourceControlUtils.isScmEnabled(applicationId)) {
      log.debug(
          "scm disabled : skipping PR commenting for application '{}' with policy evaluation '{}'",
          applicationId,
          event.policyEvaluationId);
      isOk = false;
    }
    return isOk;
  }

  private boolean checkLicense() {
    return productLicense.hasFeature(LicensedFeature.AUTOMATION);
  }
}
