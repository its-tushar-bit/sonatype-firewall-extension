/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Date;
import javax.inject.Inject;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestDAO;
import com.sonatype.insight.brain.scm.event.PullRequestCommentingLogger;
import com.sonatype.insight.brain.scm.event.SourceControlEventLoggerFactory;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestSource;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestState;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequest;
import com.sonatype.insight.brain.policy.evaluator.PullRequestRemediationDetails;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.telemetry.SourceControlPullRequestMetrics;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.iq.manager.PullRequestCommand;
import com.sonatype.nexus.iq.manager.PullRequestCommandBuilder;
import com.sonatype.nexus.iq.manager.PullRequestExecutor;
import com.sonatype.nexus.iq.manager.PullRequestResult;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.scm.event.AbstractSourceControlEventLogger.SourceControlEventData.forError;
import static com.sonatype.insight.brain.scm.event.AbstractSourceControlEventLogger.SourceControlEventData.forPullRequest;
import static com.sonatype.insight.brain.scm.event.SourceControlEventType.API_ERROR;
import static com.sonatype.insight.brain.scm.event.SourceControlEventType.PR_CREATED;

/**
 * Execute the end-to-end process to clone a repository, attempt to apply remediation changes to the file tree, followed
 * by pushing the changes to a newly created PullRequest.
 */
public class PullRequestTask
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestTask.class);

  public static final String DEFAULT_COMMITTER = "NexusIQ";

  private final GitClientFactory gitClientFactory;

  private final GitApiFactory gitApiFactory;

  private final SourceControlPullRequestMetrics metrics;

  private final AuditRecorder auditRecorder;

  private final SourceControlUtils sourceControlUtils;

  private final Configuration configuration;

  private final SourceControlPullRequestDAO sourceControlPullRequestDAO;

  private final SourceControlEventLoggerFactory scmEventLoggerFactory;

  @Inject
  public PullRequestTask(
      final GitClientFactory gitClientFactory,
      final SourceControlPullRequestMetrics metrics,
      final GitApiFactory gitApiFactory,
      final AuditRecorder auditRecorder,
      final SourceControlUtils sourceControlUtils,
      final Configuration configuration,
      final SourceControlPullRequestDAO sourceControlPullRequestDAO,
      final SourceControlEventLoggerFactory scmEventLoggerFactory)
  {
    this.gitClientFactory = gitClientFactory;
    this.metrics = metrics;
    this.gitApiFactory = gitApiFactory;
    this.auditRecorder = auditRecorder;
    this.sourceControlUtils = sourceControlUtils;
    this.configuration = configuration;
    this.sourceControlPullRequestDAO = sourceControlPullRequestDAO;
    this.scmEventLoggerFactory = scmEventLoggerFactory;
  }

  public PullRequestResult run(
      PullRequestRemediationDetails pullRequestRemediationDetails,
      PullRequestExecutor pullRequestExecutor)
  {
    if (pullRequestRemediationDetails == null) {
      throw new IllegalArgumentException("PullRequestRemediationDetails cannot be null");
    }
    if (pullRequestExecutor == null) {
      throw new IllegalArgumentException("PullRequestExecutor cannot be null");
    }
    String applicationId = pullRequestRemediationDetails.getApp().getId();
    GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(applicationId);
    SourceControlConfiguration sourceControlConfiguration = configuration.getSourceControlConfigurationOrDefault();
    maybeUpdateRepoUrlWithUsername(sourceControlConfiguration, gitRepositoryInfo);

    PullRequestCommentingLogger scmEventLogger = scmEventLoggerFactory.newLogger(
        new Date(),
        pullRequestRemediationDetails.getApp(),
        pullRequestRemediationDetails.getApp().getOrganization(),
        gitRepositoryInfo
    );

    File checkoutDir = null;
    Date start = new Date();
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forSystem()) {
      log.info("Pull request task initiated for application '{}', remediation target: [{}]",
          applicationId, pullRequestRemediationDetails.getToBeRemediated());

      checkoutDir = sourceControlUtils.getCheckoutDirectory(pullRequestRemediationDetails.getApp());

      PullRequestCommand command = new PullRequestCommandBuilder()
          .withRepositoryDirectory(checkoutDir)
          .withBaseBranch(gitRepositoryInfo.baseBranch)
          .withPullRequestBranchName(pullRequestRemediationDetails.getPullRequestBranchName())
          .withCommitMessage(pullRequestRemediationDetails.getTitle())
          .withCommitter(getCommitterUsername(sourceControlConfiguration))
          .withCommitterEmail(getCommitterEmail(sourceControlConfiguration))
          .withPullRequestContent(pullRequestRemediationDetails.getContents())
          .withPullRequestTitle(pullRequestRemediationDetails.getTitle())
          .withRemediationTarget(pullRequestRemediationDetails.getToBeRemediated())
          .withRemediationVersion(pullRequestRemediationDetails.getRemediatedVersion())
          .withGitApiClient(gitClientFactory.createApiClient(gitRepositoryInfo))
          .withGitApi(gitApiFactory.createGitApi(gitRepositoryInfo))
          .build();

      PullRequestResult pullRequestResult = pullRequestExecutor.execute(command);

      Date commandFinishedTime = new Date();

      EnhancedPullRequestResult enhancedResult = new EnhancedPullRequestResult(
          pullRequestResult,
          start,
          pullRequestRemediationDetails.getToBeRemediated(),
          pullRequestRemediationDetails.getTitle(),
          false,
          pullRequestRemediationDetails.isManualPullRequest()
      );

      metrics.addResult(applicationId, enhancedResult);

      try (AuditSession auditSession = auditRecorder.recordSystemEvent(AuditEvent.CREATE_PULL_REQUEST)) {
        AuditData.get()
            .setApplication(pullRequestRemediationDetails.getApp())
            .setScanId(pullRequestRemediationDetails.getScanId())
            .setStageId(pullRequestRemediationDetails.getStage())
            .setComponentIdentifier(pullRequestRemediationDetails.getToBeRemediated())
            .setData("pullRequestUrl", pullRequestResult.getPullRequestUrl());
      }

      if (pullRequestResult.isSuccessful()) {
        SourceControlPullRequest sourceControlPullRequest = new SourceControlPullRequest();
        sourceControlPullRequest.setRepositoryUrl(gitRepositoryInfo.repositoryUrl);
        sourceControlPullRequest.setPullRequestId(Integer.parseInt(pullRequestResult.getPullRequestUrl()
            .substring(pullRequestResult.getPullRequestUrl().lastIndexOf("/") + 1)));
        sourceControlPullRequest.setHeadCommitHash(pullRequestResult.getHeadRef());
        sourceControlPullRequest.setBranchName(pullRequestRemediationDetails.getPullRequestBranchName());
        sourceControlPullRequest.setBaseBranchName(gitRepositoryInfo.baseBranch);
        sourceControlPullRequest.setCreateTime(commandFinishedTime);
        sourceControlPullRequest.setLastCheckTime(commandFinishedTime);
        sourceControlPullRequest.setLastDetectedUpdateTime(commandFinishedTime);
        sourceControlPullRequest.setState(PullRequestState.OPEN);
        PullRequestSource pullRequestSource;
        if (pullRequestRemediationDetails.isManualPullRequest()) {
          if (pullRequestRemediationDetails.isInnerSource()) {
            pullRequestSource = PullRequestSource.MANUAL_INNER_SOURCE;
          }
          else {
            pullRequestSource = PullRequestSource.MANUAL;
          }
        }
        else {
          if (pullRequestRemediationDetails.isInnerSource()) {
            pullRequestSource = PullRequestSource.AUTOMATIC_INNER_SOURCE;
          }
          else {
            pullRequestSource = PullRequestSource.AUTOMATIC;
          }
        }
        sourceControlPullRequest.setSource(pullRequestSource);
        sourceControlPullRequestDAO.insert(sourceControlPullRequest);

        scmEventLogger.add(PR_CREATED, forPullRequest(String.valueOf(sourceControlPullRequest.getPullRequestId())));
        scmEventLogger.log();
      }
      else {
        scmEventLogger.add(API_ERROR, forError("Pull request creation failed: " + enhancedResult.getReasoning()));
        scmEventLogger.log();
        throw new SourceControlException("Pull request creation failed: " + enhancedResult.getReasoning());
      }

      log.info("Pull request task completed for application '{}': {}", applicationId, pullRequestResult);
      return pullRequestResult;
    }
    catch (Exception e) {
      log.error("Failed to execute pull request, cleaning pull request directory", e);
      scmEventLogger.add(API_ERROR, forError("Failed to execute pull request: " + e.getMessage()));
      scmEventLogger.log();

      sourceControlUtils.deleteCheckoutDirectory(pullRequestRemediationDetails.getApp());
      metrics.addResult(applicationId, new EnhancedPullRequestResult(new PullRequestResult(), start,
          pullRequestRemediationDetails.getToBeRemediated(),
          pullRequestRemediationDetails.getTitle(), true, pullRequestRemediationDetails.isManualPullRequest()));
      throw new RuntimeException("Failed to execute pull request for application '" + applicationId + "'", e);
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational at
      // this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(1);
    }
    return null;
  }

  private void maybeUpdateRepoUrlWithUsername(
      final SourceControlConfiguration sourceControlConfiguration,
      final GitRepositoryInfo gitRepositoryInfo)
  {
    // This is designed for the Bitbucket Server 'Verified Committer' feature but is ultimately an agnostic way to add
    // the username to the repo URL. Only will work on SCMs that require username.
    if (gitRepositoryInfo.getProvider().requiresUsername() &&
        sourceControlConfiguration.isUseUsernameInRepositoryCloneUrl()) {
      try {
        gitRepositoryInfo.repositoryUrl = setUserInfoToUrl(gitRepositoryInfo.repositoryUrl, gitRepositoryInfo.username);
        gitRepositoryInfo.normalizedRepositoryUrl =
            setUserInfoToUrl(gitRepositoryInfo.normalizedRepositoryUrl, gitRepositoryInfo.username);
      }
      catch (URISyntaxException e) {
        log.error("Unable to add username to repository URL", e);
      }
    }
  }

  private String setUserInfoToUrl(final String repositoryUrl, final String username) throws URISyntaxException {
    URIBuilder builder = new URIBuilder(repositoryUrl).setUserInfo(username);
    return builder.build().toString();
  }

  private String getCommitterUsername(SourceControlConfiguration sourceControlConfiguration) {
    return sourceControlConfiguration.getCommitUsername() != null
        ? sourceControlConfiguration.getCommitUsername()
        : DEFAULT_COMMITTER;
  }

  private String getCommitterEmail(SourceControlConfiguration sourceControlConfiguration) {
    return sourceControlConfiguration.getCommitEmail() != null
        ? sourceControlConfiguration.getCommitEmail()
        : GitApi.DEFAULT_COMMITTER_EMAIL;
  }
}
