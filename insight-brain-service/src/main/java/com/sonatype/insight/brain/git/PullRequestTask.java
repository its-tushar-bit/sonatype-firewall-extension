/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Locale;

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

import static java.nio.charset.StandardCharsets.UTF_8;

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

  private static final int HASH_LENGTH = 6;

  private static final int DIRECTORY_LENGTH = 30;

  private final GitClientFactory gitClientFactory;

  private final GitApiService gitApiService;

  private final GitApiFactory gitApiFactory;

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
      final SourceControlPullRequestMetrics metrics,
      final GitApiFactory gitApiFactory)
  {
    this.gitApiService = gitApiService;
    this.gitClientFactory = gitClientFactory;
    this.insightConfig = insightConfig;
    this.fileCleaner = fileCleaner;
    this.metrics = metrics;
    this.gitApiFactory = gitApiFactory;
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
      log.info("Pull request task initiated for application '{}'", applicationId);
      GitRepositoryInfo gitInfo = gitApiService.getGitRepositoryInfoForApplication(applicationId);

      checkoutDir = new File(
          insightConfig.getSourceControl().getCloneDirectory(),
          toSafePathname(applicationId, gitInfo.baseBranch));

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
          .withGitApi(gitApiFactory.createGitApi(gitInfo))
          .build();

      PullRequestResult result = new PullRequestExecutor().execute(command);
      metrics.addResult(applicationId, result);
      log.info("Pull request task completed for application '{}': {}", applicationId, result);
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

  /**
   * Creates a human readable string from a git branch that can be used as a path on (hopefully) all operating systems.
   * Truncates length to <i>PATH_LENGTH</i> to try to avoid max path length limitations.
   * Appends a hash to the path to reduce the probability of path name conflicts.
   * @param appId application id
   * @param branch branch
   * @return url and filename safe string
   */
  private String toSafePathname(final String appId, final String branch) {
    String name = appId + "-" + branch;
    String safePathname = name.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9\\-]", "");
    return safePathname.length() > DIRECTORY_LENGTH
        ? safePathname.substring(0, DIRECTORY_LENGTH - (1 + HASH_LENGTH)) + "-" + truncatedHashOf(name)
        : safePathname;
  }

  /**
   * Creates a truncated hash for the given input which is safe to use in URLs and filenames in the following way:
   * Uses RFC 4648 base64 format for URL and filenames which replaces '+' with '-' and '/' with '_' and removes padding.
   * Only uses lower case characters because not all file systems are case sensitive.
   * @param input input
   * @return hash value
   */
  private String truncatedHashOf(final String input) {
    try {
      byte[] hash = Base64.getUrlEncoder().encode(MessageDigest.getInstance("SHA1").digest(input.getBytes(UTF_8)));
      return new String(hash, UTF_8).replaceAll("=", "").substring(0, HASH_LENGTH).toLowerCase();
    }
    catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Unable to create SHA1 hash for checkout directory.", e);
    }
  }
}

