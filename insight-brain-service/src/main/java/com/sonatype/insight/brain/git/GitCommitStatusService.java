/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEvent;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.GitApiClient.StateType;
import com.sonatype.nexus.scm.api.model.Status;
import com.sonatype.nexus.scm.api.model.StatusRequest;

import com.google.common.base.Strings;
import com.google.common.eventbus.Subscribe;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class GitCommitStatusService
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(GitCommitStatusService.class);

  private static final String IQ_POLICY_EVALUATION = "IQ Policy Evaluation";

  private final BaseUrl baseUrl;

  private final ApplicationDAO applicationDAO;

  private final GitClientFactory gitClientFactory;

  private final ProductLicense productLicense;

  private final SourceControlUtils sourceControlUtils;

  private final SourceControlEventPublisher sourceControlEventPublisher;

  private final AsyncEventBus asyncEventBus;

  @Inject
  public GitCommitStatusService(
      final SourceControlUtils sourceControlUtils,
      final BaseUrl baseUrl,
      final ApplicationDAO applicationDAO,
      final GitClientFactory gitClientFactory,
      ProductLicense productLicense,
      SourceControlEventPublisher sourceControlEventPublisher,
      AsyncEventBus asyncEventBus)
  {
    this.baseUrl = baseUrl;
    this.applicationDAO = applicationDAO;
    this.gitClientFactory = gitClientFactory;
    this.productLicense = productLicense;
    this.sourceControlUtils = sourceControlUtils;
    this.sourceControlEventPublisher = sourceControlEventPublisher;
    this.asyncEventBus = asyncEventBus;
  }

  @Subscribe
  public void onApplicationEvaluation(final ApplicationEvaluationEvent event) {
    if (!productLicense.hasFeature(LicensedFeature.NOTIFICATIONS)) {
      log.debug("License does not support Source Control notifications feature");
      return;
    }
    if (Strings.isNullOrEmpty(event.commitHash)) {
      return;
    }

    GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(event.ownerId);

    if (null == gitRepositoryInfo || null == gitRepositoryInfo.provider ||
        Strings.isNullOrEmpty(gitRepositoryInfo.token)) {
      log.debug("The git repository information could not be found for application with id {}. " +
          "scm status could not be created.", event.ownerId);
      return;
    }

    sourceControlEventPublisher.publishEvent(
        new SourceControlEvent()
            .setEventType(SourceControlEvent.STATUS_UPDATE_EVENT)
            .setEventPriority(SourceControlEvent.EVENT_PRIORITY_HIGHER)
            .setApplicationId(event.ownerId)
            .setPolicyEvaluationId(event.policyEvaluationId)
            .setPolicyEvaluationOutcome(event.outcome)
            .setCommitHash(event.commitHash)
            .setScanId(event.reportId)
            .withComponentCounts(event.criticalComponentCount, event.severeComponentCount, event.moderateComponentCount)
            .setInitiator(event.initiator)
            .setStageTypeId(event.stageTypeId)
    );
  }

  public void onSendCommitStatus(SourceControlEvent event) {
    GitRepositoryInfo gitRepositoryInfo =
        sourceControlUtils.getGitRepositoryInfoForApplication(event.getApplicationId());

    if (null == gitRepositoryInfo || null == gitRepositoryInfo.provider ||
        Strings.isNullOrEmpty(gitRepositoryInfo.token)) {
      log.debug("The git repository information could not be found for application with id {}, " +
          "scm status could not be created.", event.getApplicationId());
      return;
    }

    GitApiClient gitApiClient = gitClientFactory.createApiClient(gitRepositoryInfo);

    StatusRequest statusRequest = createStatusRequest(event, gitApiClient, gitRepositoryInfo.provider);

    log.debug("Creating a {} commit status for repository: {}, commit hash: {}, with outcome: {}, state: {}",
        gitRepositoryInfo.provider, gitApiClient.getProjectUri().getUrl(),
        event.getCommitHash(), event.getPolicyEvaluationOutcome(), statusRequest.getState());
    try {
      Status status = gitApiClient.createStatus(event.getCommitHash(), statusRequest);
      log.info(
          "Commit status sent for repository: {}, commit hash: {}, evaluation outcome: {}, state: {}, response: {}",
          gitApiClient.getProjectUri().getUrl(), event.getCommitHash(), event.getPolicyEvaluationOutcome(),
          statusRequest.getState(), status
      );
    }
    catch (Exception e) {
      log.error("Failed to update status for applicationId: {}, repository: {}, commitHash: {}, " +
              "triggered by policyEvaluationId: {}, reason: {}",
          event.getApplicationId(), gitRepositoryInfo.repositoryUrl, event.getCommitHash(),
          event.getPolicyEvaluationId(), e.getMessage(), e);
    }
  }

  private StatusRequest createStatusRequest(
      final SourceControlEvent event,
      final GitApiClient gitApiClient,
      final SourceControlProvider provider)
  {
    return gitApiClient.createStatusRequest(
        getState(event, gitApiClient),
        IQ_POLICY_EVALUATION,
        createStatusMessage(event),
        getReportUrl(event.getApplicationId(), event.getScanId(), provider));
  }

  private String getReportUrl(
      final String ownerId,
      final String scanId,
      final SourceControlProvider provider)
  {
    Application application = applicationDAO.getByIdNotNull(ownerId);
    String reportPath = UserInterfaceLinksHelper.getReportUrl(application.getPublicId(), scanId);
    reportPath = addSourceQuery(reportPath, provider);
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setReportUrl(reportPath);
    return scanReceipt.resolveReportUrl(baseUrl.get());
  }

  private String addSourceQuery(
      final String reportPath,
      final SourceControlProvider provider)
  {
    return UriBuilder.fromPath(reportPath).queryParam("source", provider.toString()).toString();
  }

  private static String getState(
      final SourceControlEvent event,
      final GitApiClient gitApiClient)
  {
    switch (event.getPolicyEvaluationOutcome()) {
      case ApplicationEvaluationEvent.ACTION_ID_NONE:
      case Action.ID_WARN:
        return gitApiClient.getState(StateType.SUCCESS);
      default:
        return gitApiClient.getState(StateType.FAILURE);
    }
  }

  private static String createStatusMessage(final SourceControlEvent event) {
    return String.format("Components: Critical: %d, Severe: %d, Moderate: %d", event.getCriticalComponentCount(),
        event.getSevereComponentCount(), event.getModerateComponentCount());
  }

  @Override
  public void start() throws Exception {
    asyncEventBus.register(this);
  }

  @Override
  public void stop() throws Exception {
    asyncEventBus.unregister(this);
  }
}
