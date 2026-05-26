/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Date;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestDAO;
import com.sonatype.insight.brain.metrics.ScmOperationMetrics;
import com.sonatype.insight.brain.metrics.ScmTimerContext;
import com.sonatype.insight.brain.scm.event.PullRequestCommentingLogger;
import com.sonatype.insight.brain.scm.event.SourceControlEventLoggerFactory;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestSource;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestState;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl.AuthenticationType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
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

import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.OUTCOME_FAILURE;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.OUTCOME_SUCCESS;
import static com.sonatype.insight.brain.scm.event.AbstractSourceControlEventLogger.SourceControlEventData.forError;
import static com.sonatype.insight.brain.scm.event.AbstractSourceControlEventLogger.SourceControlEventData.forPullRequest;
import static com.sonatype.insight.brain.scm.event.SourceControlEventType.API_ERROR;
import static com.sonatype.insight.brain.scm.event.SourceControlEventType.PR_CREATED;

/**
 * Execute the end-to-end process to clone a repository, attempt to apply remediation changes to the file tree, followed
 * by pushing the changes to a newly created PullRequest.
 */
@Named
@Singleton
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

  private final SourceControlEventDAO sourceControlEventDAO;

  private final SourceControlEventLoggerFactory scmEventLoggerFactory;

  private final ScmOperationMetrics scmOperationMetrics;

  private final PullRequestFailureCategorizer pullRequestFailureCategorizer;

  @Inject
  public PullRequestTask(
      final GitClientFactory gitClientFactory,
      final SourceControlPullRequestMetrics metrics,
      final GitApiFactory gitApiFactory,
      final AuditRecorder auditRecorder,
      final SourceControlUtils sourceControlUtils,
      final Configuration configuration,
      final SourceControlPullRequestDAO sourceControlPullRequestDAO,
      final SourceControlEventDAO sourceControlEventDAO,
      final SourceControlEventLoggerFactory scmEventLoggerFactory,
      final ScmOperationMetrics scmOperationMetrics,
      final PullRequestFailureCategorizer pullRequestFailureCategorizer)
  {
    this.gitClientFactory = gitClientFactory;
    this.metrics = metrics;
    this.gitApiFactory = gitApiFactory;
    this.auditRecorder = auditRecorder;
    this.sourceControlUtils = sourceControlUtils;
    this.configuration = configuration;
    this.sourceControlPullRequestDAO = sourceControlPullRequestDAO;
    this.sourceControlEventDAO = sourceControlEventDAO;
    this.scmEventLoggerFactory = scmEventLoggerFactory;
    this.scmOperationMetrics = scmOperationMetrics;
    this.pullRequestFailureCategorizer = pullRequestFailureCategorizer;
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
        gitRepositoryInfo);

    File checkoutDir = null;
    Date start = new Date();
    ScmTimerContext timerContext = null;
    // Held outside the try so the catch block can record the auth that was resolved (if any) when an exception
    // escapes the SCM call. Null when the throw happened before auth resolution (e.g., checkout directory issue).
    ResolvedAuthContext resolvedAuthContextForCatch = null;
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

      ResolvedAuthContext authContext = gitClientFactory.resolveAuthContext(gitRepositoryInfo);
      resolvedAuthContextForCatch = authContext;

      timerContext = scmOperationMetrics.startPrCreationTimer(gitRepositoryInfo.provider.name());
      PullRequestResult pullRequestResult = pullRequestExecutor.execute(command);

      Date commandFinishedTime = new Date();

      EnhancedPullRequestResult enhancedResult = new EnhancedPullRequestResult(
          pullRequestResult,
          start,
          pullRequestRemediationDetails.getToBeRemediated(),
          pullRequestRemediationDetails.getTitle(),
          false,
          pullRequestRemediationDetails.isManualPullRequest());

      metrics.addResult(applicationId, enhancedResult);

      String outcome = pullRequestResult.isSuccessful() ? OUTCOME_SUCCESS : OUTCOME_FAILURE;
      // Soft-failure path: pullRequestExecutor.execute returned a non-successful result rather than throwing,
      // so there is no provider exception to categorize. Passing null yields UNKNOWN_PROVIDER_ERROR, which is
      // honest — categorical specificity would require restructuring the executor's API to surface a typed failure.
      String failureReason = pullRequestResult.isSuccessful()
          ? null
          : pullRequestFailureCategorizer.categorize(null);

      try (AuditSession auditSession = auditRecorder.recordSystemEvent(AuditEvent.CREATE_PULL_REQUEST)) {
        AuditData.get()
            .setApplication(pullRequestRemediationDetails.getApp())
            .setScanId(pullRequestRemediationDetails.getScanId())
            .setStageId(pullRequestRemediationDetails.getStage())
            .setComponentIdentifier(pullRequestRemediationDetails.getToBeRemediated())
            .setData("authenticationType", authContext.getAuthenticationTypeName())
            .setData("authOwnerId", authContext.getAuthOwnerId())
            .setData("outcome", outcome);
        if (authContext.getAuthenticationType() == AuthenticationType.GITHUB_APP) {
          AuditData.get()
              .setData("githubAppId", authContext.getGithubAppIdAsString())
              .setData("installationId", authContext.getInstallationIdAsString());
        }
        if (pullRequestResult.isSuccessful()) {
          AuditData.get().setData("pullRequestUrl", pullRequestResult.getPullRequestUrl());
        }
        else {
          AuditData.get().setData("failureReason", failureReason);
        }
      }

      if (pullRequestResult.isSuccessful()) {
        SourceControlEvent eventForTrace = pullRequestRemediationDetails.getSourceControlEvent();
        if (eventForTrace != null) {
          eventForTrace.setAuthenticationType(authContext.getAuthenticationTypeName());
          eventForTrace.setAuthOwnerId(authContext.getAuthOwnerId());
          if (authContext.getAuthenticationType() == AuthenticationType.GITHUB_APP) {
            eventForTrace.setGithubAppId(authContext.getGithubAppIdAsString());
            eventForTrace.setInstallationId(authContext.getInstallationIdAsString());
          }
          eventForTrace.setOutcome(outcome);
          eventForTrace.setFailureReason(failureReason);
        }
        else if (pullRequestRemediationDetails.getSourceControlEventId() != null) {
          try {
            sourceControlEventDAO.overwriteTraceFields(
                pullRequestRemediationDetails.getSourceControlEventId(),
                authContext.getAuthenticationTypeName(),
                authContext.getAuthOwnerId(),
                authContext.getGithubAppIdAsString(),
                authContext.getInstallationIdAsString(),
                outcome,
                failureReason);
          }
          catch (Exception traceFailure) {
            log.warn("Failed to persist auth trace onto source_control_event for application '{}'",
                applicationId, traceFailure);
          }
        }

        scmOperationMetrics.recordPrCreationCompleted(timerContext);
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
        sourceControlPullRequest.setSourceControlEventId(pullRequestRemediationDetails.getSourceControlEventId());
        sourceControlPullRequest.setAuthenticationType(authContext.getAuthenticationTypeName());
        sourceControlPullRequest.setAuthOwnerId(authContext.getAuthOwnerId());
        if (authContext.getAuthenticationType() == AuthenticationType.GITHUB_APP) {
          sourceControlPullRequest.setGithubAppId(authContext.getGithubAppIdAsString());
          sourceControlPullRequest.setInstallationId(authContext.getInstallationIdAsString());
        }
        sourceControlPullRequestDAO.insert(sourceControlPullRequest);

        scmEventLogger.add(PR_CREATED,
            forPullRequest(String.valueOf(sourceControlPullRequest.getPullRequestId()))
                .withTraceContext(
                    authContext.getAuthenticationTypeName(),
                    authContext.getAuthOwnerId(),
                    authContext.getGithubAppIdAsString(),
                    authContext.getInstallationIdAsString(),
                    outcome,
                    failureReason));
        scmEventLogger.log();
      }
      else {
        scmEventLogger.add(API_ERROR, forError("Pull request creation failed: " + enhancedResult.getReasoning()));
        scmEventLogger.log();
        throw new SourceControlException(
            "Pull request creation failed: " + enhancedResult.getReasoning(),
            enhancedResult.getCategory());
      }

      log.info("Pull request task completed for application '{}': {}", applicationId, pullRequestResult);
      return pullRequestResult;
    }
    catch (Exception e) {
      scmOperationMetrics.recordPrCreationFailed(timerContext);
      // Standard error log uses the structured logging slot for the throwable — DO NOT inline e.getMessage()
      // anywhere downstream that gets persisted or shipped to HDS / structured event logs.
      log.error("Failed to execute pull request, cleaning pull request directory", e);

      String failureReason = pullRequestFailureCategorizer.categorize(e);

      // Structured event log: emit the categorical token only, never the raw exception message.
      String authTypeForCatch = resolvedAuthContextForCatch == null
          ? null
          : resolvedAuthContextForCatch.getAuthenticationTypeName();
      String authOwnerForCatch = resolvedAuthContextForCatch == null
          ? null
          : resolvedAuthContextForCatch.getAuthOwnerId();
      String githubAppIdForCatch = resolvedAuthContextForCatch == null
          ? null
          : resolvedAuthContextForCatch.getGithubAppIdAsString();
      String installationIdForCatch = resolvedAuthContextForCatch == null
          ? null
          : resolvedAuthContextForCatch.getInstallationIdAsString();

      scmEventLogger.add(API_ERROR,
          forError("Pull request creation failed")
              .withTraceContext(authTypeForCatch, authOwnerForCatch, githubAppIdForCatch, installationIdForCatch,
                  OUTCOME_FAILURE, failureReason));
      scmEventLogger.log();

      // Persist trace onto the SourceControlEvent row so the audit chain joins back even on hard failure.
      try {
        if (pullRequestRemediationDetails.getSourceControlEventId() != null) {
          sourceControlEventDAO.overwriteTraceFields(
              pullRequestRemediationDetails.getSourceControlEventId(),
              authTypeForCatch,
              authOwnerForCatch,
              githubAppIdForCatch,
              installationIdForCatch,
              OUTCOME_FAILURE,
              failureReason);
        }
      }
      catch (Exception traceFailure) {
        log.warn("Failed to persist auth trace onto source_control_event for application '{}'",
            applicationId, traceFailure);
      }

      sourceControlUtils.deleteCheckoutDirectory(pullRequestRemediationDetails.getApp());
      metrics.addResult(applicationId, new EnhancedPullRequestResult(new PullRequestResult(), start,
          pullRequestRemediationDetails.getToBeRemediated(),
          pullRequestRemediationDetails.getTitle(), true, pullRequestRemediationDetails.isManualPullRequest()));
      PullRequestFailureCategory category;
      String message;
      if (e instanceof SourceControlException) {
        SourceControlException sce = (SourceControlException) e;
        category = sce.getCategory() != null ? sce.getCategory() : PullRequestFailureCategory.SCM_ERROR;
        // Preserve the inner message when it carries an actionable category like
        // MANIFEST_COMPONENT_NOT_FOUND, so the UI tooltip/reason stays user-actionable.
        message = sce.getMessage();
      }
      else {
        category = PullRequestFailureCategory.SCM_ERROR;
        message = "Failed to execute pull request for application '" + applicationId + "'";
      }
      throw new SourceControlException(message, category, e);
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
        sourceControlConfiguration.isUseUsernameInRepositoryCloneUrl())
    {
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
