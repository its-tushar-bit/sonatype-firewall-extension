/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.github;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEvent;
import com.sonatype.nexus.github.GitHubApiClient;
import com.sonatype.nexus.github.model.ProjectUri;
import com.sonatype.nexus.github.model.Status;
import com.sonatype.nexus.github.model.StatusRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class GitHubApiService
{
  private static final Logger log = LoggerFactory.getLogger(GitHubApiService.class);

  private static final String IQ_POLICY_EVALUATION = "IQ Policy Evaluation";

  private final ApiSourceControlService sourceControlService;

  private final BaseUrl baseUrl;

  private final ApplicationDAO applicationDAO;

  private final GitHubApiClientFactory gitHubApiClientFactory;

  @Inject
  public GitHubApiService(
      final ApiSourceControlService sourceControlService,
      final BaseUrl baseUrl,
      final ApplicationDAO applicationDAO,
      final GitHubApiClientFactory gitHubApiClientFactory)
  {
    this.sourceControlService = sourceControlService;
    this.baseUrl = baseUrl;
    this.applicationDAO = applicationDAO;
    this.gitHubApiClientFactory = gitHubApiClientFactory;
  }

  /**
   * Responds to the application evaluation event by sending a GitHub status message indicating the evaluation outcome
   * and component counts if a commit hash was send with the policy evaluation request.
   */
  public void maybeRespond(final ApplicationEvaluationEvent event) {
    if (null != event.commitHash) {
      SourceControl sourceControl = sourceControlService.getSourceControlByApplicationIdDecrypted(event.ownerId);
      if (null != sourceControl) {
        ProjectUri projectUri = new ProjectUri(sourceControl.getRepositoryUrl());
        GitHubApiClient gitHubApiClient =
            gitHubApiClientFactory.create(sourceControl.getRepositoryUrl(), sourceControl.getToken());
        StatusRequest statusRequest = createStatusRequest(event);
        log.debug("Creating GitHub commit status for repository: {}, commit hash: {}, with outcome: {}, state: {}",
            projectUri.getUrl(), event.commitHash, event.outcome, statusRequest.state);
        try {
          Status status = gitHubApiClient
              .createStatus(projectUri.getOrganization(), projectUri.getProject(), event.commitHash, statusRequest);
          log.debug("Status response from api url: {}, creator: {}", status.url, status.creator.login);
        }
        catch (IOException e) {
          log.error("Failed to update status for applicationId: {}, repository: {}, commitHash: {}, " +
                  "triggered by policyEvaluationId: {}",
              event.ownerId, sourceControl.getRepositoryUrl(), event.commitHash, event.policyEvaluationId, e);
        }
      }
    }
  }

  private StatusRequest createStatusRequest(final ApplicationEvaluationEvent event) {
    StatusRequest statusRequest = new StatusRequest();
    statusRequest.state = getState(event);
    statusRequest.context = IQ_POLICY_EVALUATION;
    statusRequest.description = createStatusMessage(event);
    statusRequest.targetUrl = getReportUrl(event.ownerId, event.reportId);
    return statusRequest;
  }

  private String getReportUrl(final String ownerId, final String scanId) {
    Application application = applicationDAO.getByIdNotNull(ownerId);
    String reportPath = UserInterfaceLinksResource.getReportUrl(application.getPublicId(), scanId);
    reportPath = addGitHubSourceQuery(reportPath);
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setReportUrl(reportPath);
    return scanReceipt.resolveReportUrl(baseUrl.get());
  }

  private String addGitHubSourceQuery(final String reportPath) {
    return UriBuilder.fromPath(reportPath).queryParam("source", "github").toString();
  }

  private static String getState(final ApplicationEvaluationEvent event) {
    switch (event.outcome) {
      case ApplicationEvaluationEvent.ACTION_ID_NONE:
        return "success";
      case Action.ID_WARN:
        return "pending";
      default:
        return "failure";
    }
  }

  private static String createStatusMessage(final ApplicationEvaluationEvent event) {
    return String.format("Components: Critical: %d, Severe: %d, Moderate: %d", event.criticalComponentCount,
        event.severeComponentCount, event.moderateComponentCount);
  }
}
