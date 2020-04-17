/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.policy.evaluator.PullRequestRemediationDetails;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.telemetry.SourceControlPullRequestMetrics;
import com.sonatype.nexus.iq.manager.PullRequestCommand;
import com.sonatype.nexus.iq.manager.PullRequestCommandBuilder;
import com.sonatype.nexus.iq.manager.PullRequestExecutor;
import com.sonatype.nexus.iq.manager.PullRequestResult;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Execute the end-to-end process to clone a repository, attempt to apply remediation changes to the file tree,
 * followed by pushing the changes to a newly created PullRequest.
 */
public class PullRequestTask
    extends GitRepositoryTask
    implements Runnable
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestTask.class);

  public static final String DEFAULT_COMMITTER = "Nexus IQ";

  public static final String DEFAULT_COMMITTER_EMAIL = "\"<>\"";

  private final GitClientFactory gitClientFactory;

  private final GitApiFactory gitApiFactory;

  private final SourceControlPullRequestMetrics metrics;

  private PullRequestRemediationDetails pullRequestRemediationDetails;

  private AuditRecorder auditRecorder;

  private PullRequestExecutor pullRequestExecutor;

  private final SourceControlUtils sourceControlUtils;

  @Inject
  public PullRequestTask(
      final GitClientFactory gitClientFactory,
      final InsightConfig insightConfig,
      final FileCleaner fileCleaner,
      final SourceControlPullRequestMetrics metrics,
      final GitApiFactory gitApiFactory,
      final AuditRecorder auditRecorder,
      final SourceControlUtils sourceControlUtils)
  {
    super(insightConfig, fileCleaner);
    this.gitClientFactory = gitClientFactory;
    this.metrics = metrics;
    this.gitApiFactory = gitApiFactory;
    this.auditRecorder = auditRecorder;
    this.sourceControlUtils = sourceControlUtils;
  }

  public void init(
      PullRequestRemediationDetails pullRequestRemediationDetails,
      PullRequestExecutor pullRequestExecutor)
  {
    this.pullRequestRemediationDetails = pullRequestRemediationDetails;
    this.pullRequestExecutor = pullRequestExecutor;
  }

  @Override
  public void run() {
    if (pullRequestRemediationDetails == null) {
      log.error("Missing required PullRequestRemediationDetails");
      return;
    }
    File checkoutDir = null;
    try {
      String applicationId = pullRequestRemediationDetails.getApp().getId();
      log.info("Pull request task initiated for application '{}'", applicationId);
      GitRepositoryInfo gitInfo = sourceControlUtils.getGitRepositoryInfoForApplication(applicationId);

      checkoutDir = getCheckoutDirectory(pullRequestRemediationDetails.getApp().getPublicId(), applicationId, gitInfo);

      PullRequestCommand command = new PullRequestCommandBuilder()
          .withRepositoryDirectory(checkoutDir)
          .withBaseBranch(gitInfo.baseBranch)
          .withPullRequestBranchName(pullRequestRemediationDetails.getPullRequestBranchName())
          .withCommitMessage(pullRequestRemediationDetails.getTitle())
          .withCommitter(DEFAULT_COMMITTER)
          .withCommitterEmail(DEFAULT_COMMITTER_EMAIL)
          .withPullRequestContent(pullRequestRemediationDetails.getContents())
          .withPullRequestTitle(pullRequestRemediationDetails.getTitle())
          .withRemediationTarget(pullRequestRemediationDetails.getToBeRemediated())
          .withRemediationVersion(pullRequestRemediationDetails.getRemediatedVersion())
          .withGitApiClient(gitClientFactory.createApiClient(gitInfo))
          .withGitApi(gitApiFactory.createGitApi(gitInfo))
          .build();

      PullRequestResult result = pullRequestExecutor.execute(command);
      metrics.addResult(applicationId, result);

      try (AuditSession auditSession = auditRecorder.recordSystemEvent(AuditEvent.CREATE_PULL_REQUEST)) {
        AuditData.get()
            .setApplication(pullRequestRemediationDetails.getApp())
            .setScanId(pullRequestRemediationDetails.getScanId())
            .setStageId(pullRequestRemediationDetails.getStage())
            .setComponentIdentifier(pullRequestRemediationDetails.getToBeRemediated())
            .setData("pullRequestUrl", result.getPullRequestUrl());
      }
      log.info("Pull request task completed for application '{}': {}", applicationId, result);
    }
    catch (Exception e) {
      log.error("Failed to execute pull request, cleaning pull request directory", e);
      cleanDirectory(checkoutDir);
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational at
      // this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(1);
    }
  }
}

