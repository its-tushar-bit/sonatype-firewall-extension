/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.github;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEvent;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.nexus.github.GitHubApiClient;
import com.sonatype.nexus.github.model.ProjectUri;
import com.sonatype.nexus.github.model.StatusRequest;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class GitHubApiService
{
  private static final Logger log = LoggerFactory.getLogger(GitHubApiService.class);

  private static final String IQ_POLICY_EVALUATION = "IQ Policy Evaluation";

  private final ApiSourceControlService sourceControlService;

  private final InsightWork work;

  private final ReportService reportService;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final BaseUrl baseUrl;

  private final ApplicationDAO applicationDAO;

  private final GitHubApiClientFactory gitHubApiClientFactory;

  @Inject
  public GitHubApiService(
      final ApiSourceControlService sourceControlService,
      final InsightWork work,
      final ReportService reportService,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final BaseUrl baseUrl,
      final ApplicationDAO applicationDAO,
      final GitHubApiClientFactory gitHubApiClientFactory)
  {
    this.sourceControlService = sourceControlService;
    this.work = work;
    this.reportService = reportService;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.baseUrl = baseUrl;
    this.applicationDAO = applicationDAO;
    this.gitHubApiClientFactory = gitHubApiClientFactory;
  }

  /**
   * Responds to the application evaluation event by sending a GitHub status message indicating the evaluation outcome
   * and component counts if a commit hash was send with the policy evaluation request.
   */
  public void maybeRespond(final ApplicationEvaluationEvent event) throws IOException {
    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getById(event.policyEvaluationId);
    File report = reportService.getReport(work, policyEvaluation.getApplicationId(), policyEvaluation.getScanId());
    String commitHash = extractCommitHash(report);

    if (null != commitHash) {
      SourceControl sourceControl = sourceControlService.getSourceControlByApplicationIdDecrypted(event.ownerId);
      if (sourceControl == null) {
        return;
      }
      ProjectUri projectUri = new ProjectUri(sourceControl.getRepositoryUrl());
      GitHubApiClient gitHubApiClient =
          gitHubApiClientFactory.create(sourceControl.getRepositoryUrl(), sourceControl.getToken());
      StatusRequest statusRequest = createStatusRequest(event, policyEvaluation);
      log.debug("Creating GitHub commit status for repository: {}, commit hash: {}, with outcome: {}, state: {}",
          projectUri.getUrl(), commitHash, event.outcome, statusRequest.state);
      gitHubApiClient.createStatus(projectUri.getOrganization(), projectUri.getProject(), commitHash, statusRequest);
    }
  }

  private StatusRequest createStatusRequest(
      final ApplicationEvaluationEvent event,
      final PolicyEvaluation policyEvaluation)
  {
    StatusRequest statusRequest = new StatusRequest();
    statusRequest.state = getState(event);
    statusRequest.context = IQ_POLICY_EVALUATION;
    statusRequest.description = createStatusMessage(event);
    statusRequest.targetUrl = getReportUrl(policyEvaluation);
    return statusRequest;
  }

  private String getReportUrl(final PolicyEvaluation policyEvaluation) {
    Application application = applicationDAO.getByIdNotNull(policyEvaluation.getApplicationId());
    String reportPath =
        UserInterfaceLinksResource.getReportUrl(application.getPublicId(), policyEvaluation.getScanId());
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setReportUrl(reportPath);
    return scanReceipt.resolveReportUrl(baseUrl.get());
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

  private String extractCommitHash(File reportFile) throws IOException {
    ReportEntry dataReportEntry = Report.getEntry(reportFile, Report.DATA_JSON_FILENAME);
    ObjectNode data = JsonUtils.parse(dataReportEntry.buf);
    return data.path("commitHash").asText(null);
  }
}
