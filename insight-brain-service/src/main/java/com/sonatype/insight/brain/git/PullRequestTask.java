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

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
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
import org.apache.commons.lang3.StringUtils;
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

  private static final int PUBLIC_ID_LENGTH = 25;

  private static final int BRANCH_LENGTH = 15;

  private static final String PATH_REGEX = "[^a-z0-9\\-]";

  private final GitClientFactory gitClientFactory;

  private final GitApiFactory gitApiFactory;

  private final FileCleaner fileCleaner;

  private final InsightConfig insightConfig;

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
    this.gitClientFactory = gitClientFactory;
    this.insightConfig = insightConfig;
    this.fileCleaner = fileCleaner;
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

      checkoutDir = new File(
          insightConfig.getSourceControl().getCloneDirectory(),
          toSafePathname(pullRequestRemediationDetails.getApp().getPublicId(), gitInfo.baseBranch, applicationId));

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
   * Creates a human readable path name from an IQ application public ID and branch, along with a hash of the
   * application ID. This can be used for cloning as a path on (hopefully) all operating systems. The path takes the
   * form of:<BR><BR>
   * {@code <application public id>-<branch name>-<hash of application id>}<BR><BR>
   * The application public ID is truncated after {@link #PUBLIC_ID_LENGTH} and the branch name is truncated after
   * {@link #BRANCH_LENGTH}. The hash of the app ID is appended to reduce the probability of path name conflicts. The
   * application public ID and branch name are both lower-cased and sanitized to only include alphanumeric characters
   * and dashes.
   * @return url and filename safe string
   */
  private String toSafePathname(final String applicationPublicId, final String branchName, final String applicationId) {
    String safeAppPublicId = applicationPublicId.toLowerCase(Locale.ENGLISH).replaceAll(PATH_REGEX, "");
    String safeBranchName = branchName.toLowerCase(Locale.ENGLISH).replaceAll(PATH_REGEX, "");

    safeAppPublicId = StringUtils.substring(safeAppPublicId, 0, PUBLIC_ID_LENGTH);
    safeBranchName = StringUtils.substring(safeBranchName, 0, BRANCH_LENGTH);

    return String.join("-", safeAppPublicId, safeBranchName, truncatedHashOf(applicationId));
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

