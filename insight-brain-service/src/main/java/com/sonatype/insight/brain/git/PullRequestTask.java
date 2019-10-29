/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import com.sonatype.insight.brain.policy.evaluator.PullRequestRemediationDetails;
import com.sonatype.insight.brain.service.InsightConfig;
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
    implements Runnable
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestTask.class);

  public static final String DEFAULT_COMMITTER = "Nexus IQ";

  public static final String DEFAULT_COMMITTER_EMAIL = "<>";

  private final GitClientFactory gitClientFactory;

  private final GitApiService gitApiService;

  private final FileCleaner fileCleaner;

  private final InsightConfig insightConfig;
  
  private final SourceControlPullRequestMetrics metrics;

  private PullRequestRemediationDetails pullRequestRemediationDetails;

  @Inject
  public PullRequestTask(
      final GitApiService gitApiService,
      final GitClientFactory gitClientFactory,
      final InsightConfig insightConfig,
      final FileCleaner fileCleaner, 
      final SourceControlPullRequestMetrics metrics)
  {
    this.gitApiService = gitApiService;
    this.gitClientFactory = gitClientFactory;
    this.insightConfig = insightConfig;
    this.fileCleaner = fileCleaner;
    this.metrics = metrics;
  }
  
  public void init(PullRequestRemediationDetails pullRequestRemediationDetails) {
    this.pullRequestRemediationDetails = pullRequestRemediationDetails;
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
      GitRepositoryInfo gitInfo = gitApiService.getGitRepositoryInfoForApplication(applicationId);

      checkoutDir = new File(insightConfig.getSourceControl().getCloneDirectory(), applicationId);
      if (checkoutDir.exists()) {
        log.debug("Using existing directory for pull request: {}", checkoutDir.getAbsolutePath());
      }
      else {
        boolean created = checkoutDir.mkdirs();
        log.debug("Created new directory for pull request: {} result was {}", checkoutDir.getAbsolutePath(), created);
      }

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
          .withGitApiClient(gitClientFactory.create(gitInfo))
          .withGitApi(gitApiService.createGitApi(gitInfo))
          .build();

      PullRequestResult result = new PullRequestExecutor().execute(command);
      metrics.addResult(applicationId, result);
      log.info("Pull request complete: {}", result);
    }
    catch (Exception e) {
      log.error("Failed to execute pull request, cleaning pull request directory", e);
      try {
        if (null != checkoutDir) {
          fileCleaner.delete(checkoutDir);
        }
      }
      catch (FileDeletionException ex) {
        log.error("Failed to remove checkout directory", ex);
      }
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

