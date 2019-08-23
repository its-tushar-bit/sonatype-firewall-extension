/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.nexus.scm.api.model.Status;
import com.sonatype.nexus.scm.api.model.StatusRequest;
import com.sonatype.nexus.scm.github.GitHubApiClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to send notifications of policy alerts to Source Code Management
 * systems like github
 */
public class PolicyAlertScmNotifier
{
  private static final Logger log = LoggerFactory.getLogger(PolicyAlertScmNotifier.class);

  private final InsightWork work;

  private final ReportService reportService;

  private final ApiSourceControlService sourceControlService;

  @Inject
  public PolicyAlertScmNotifier(final ReportService reportService, final InsightWork work,
                                final ApiSourceControlService sourceControlService)
  {
    this.reportService = reportService;
    this.work = work;
    this.sourceControlService = sourceControlService;
  }

  public void sendNotifications(final Application app,
                                final String scanId,
                                final Stage stage,
                                final List<PolicyNotification> policyNotifications,
                                final int grandfatheredPolicyViolationCount,
                                final String targetUrl)
  {
    try {
      final File reportFile = reportService.fetchReport(work, app, scanId);
      String sha = extractCommitHash(reportFile);
      ApiSourceControlDTO apiSourceControlDTO = sourceControlService.getSourceControlByApplicationId(app.getId());

      Configuration apiConfiguration = new Configuration();
      apiConfiguration.setServerUrl(app.getId());
      GitHubApiClient apiClient = new GitHubApiClient(apiConfiguration, apiSourceControlDTO.repositoryUrl,
          apiSourceControlDTO.token);

      StatusRequest statusRequest = apiClient.createStatusRequest("state", "IQ", "Eval Report", targetUrl);
      Status status  = apiClient.createStatus(sha, statusRequest);

      // TODO use this data to construct a PR

      log.debug("Construct an send a PR using (status {}, stage {}, policyNotifications {}, grandfatheredCount {}",
          status, stage, grandfatheredPolicyViolationCount);
    }
    catch (IOException e) {
      // TODO full implementation
      e.printStackTrace();
    }
  }

  private String extractCommitHash(File reportFile) throws IOException {
    ReportEntry dataReportEntry = Report.getEntry(reportFile, Report.DATA_JSON_FILENAME);
    if (null == dataReportEntry) {
      return null;
    }
    return JsonUtils.parse(dataReportEntry.buf).path("commitHash").asText(null);
  }
}
