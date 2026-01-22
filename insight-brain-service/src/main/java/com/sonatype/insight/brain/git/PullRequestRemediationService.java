/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import com.sonatype.insight.brain.model.sourcecontrol.PullRequestSource;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import java.io.IOException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceApplicationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.policy.evaluator.PullRequestRemediationDetails;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.nexus.iq.manager.PullRequestExecutor;
import com.sonatype.nexus.iq.manager.PullRequestResult;
import com.sonatype.nexus.scm.api.GitApiClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Named
@Singleton
public class PullRequestRemediationService
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestRemediationService.class);

  private final PullRequestExecutor pullRequestExecutor;

  private final GitClientFactory gitClientFactory;

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final SourceControlUtils sourceControlUtils;

  private final TelemetryUtils telemetryUtils;

  private final Provider<PullRequestTask> pullRequestTaskProvider;

  private final SourceControlSshService sourceControlSshService;

  private final SourceControlEventDAO sourceControlEventDAO;

  private final ScmReducedSecurityService scmReducedSecurityService;
  
  private final InnerSourceApplicationDAO innerSourceApplicationDAO;

  private final TelemetrySender telemetrySender;

  @Inject
  public PullRequestRemediationService(
      PullRequestExecutor pullRequestExecutor,
      GitClientFactory gitClientFactory,
      ApplicationDAO applicationDAO,
      OrganizationDAO organizationDAO,
      SourceControlUtils sourceControlUtils,
      TelemetryUtils telemetryUtils,
      Provider<PullRequestTask> pullRequestTaskProvider,
      SourceControlSshService sourceControlSshService,
      SourceControlEventDAO sourceControlEventDAO,
      ScmReducedSecurityService scmReducedSecurityService,
      InnerSourceApplicationDAO innerSourceApplicationDAO,
      TelemetrySender telemetrySender)
  {
    this.pullRequestExecutor = pullRequestExecutor;
    this.gitClientFactory = gitClientFactory;
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.sourceControlUtils = sourceControlUtils;
    this.telemetryUtils = telemetryUtils;
    this.pullRequestTaskProvider = pullRequestTaskProvider;
    this.sourceControlSshService = sourceControlSshService;
    this.sourceControlEventDAO = sourceControlEventDAO;
    this.scmReducedSecurityService = scmReducedSecurityService;
    this.innerSourceApplicationDAO = innerSourceApplicationDAO;
    this.telemetrySender = telemetrySender;
  }

  /**
   * Handles the source control event associated with automated/manual remediation pull requests.
   *
   * @param event contains the details needed for pull request generation
   */
  public void onRemediateComponent(SourceControlEvent event) throws IOException {
    GitRepositoryInfo gitRepositoryInfo =
        sourceControlUtils.getGitRepositoryInfoForApplication(event.getApplicationId());
    if (isBranchOnServer(gitRepositoryInfo, event.getBranchName())) {
      log.info("Branch already exists on remote server for remediation [{}]", event.getBranchName());

      throw new SourceControlException(
          "Branch already exists on remote server for remediation: " + event.getBranchName()
      );
    }
    else {
      sourceControlSshService.verifySshUrlAndUpdateIfNeeded(event.getApplicationId());

      Application application = applicationDAO.getById(event.getApplicationId());
      boolean reducedSecurityData = scmReducedSecurityService.isReducedSecurityData(application.getId());
      PullRequestRemediationDetails pullRequestRemediationDetails = new PullRequestRemediationDetails(
          event.getComponentIdentifier(),
          event.getRemediationVersion(),
          event.getBranchName(),
          application,
          event.getScanId(),
          event.getStageTypeId(),
          event.getPullRequestContents(),
          organizationDAO,
          SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT.equals(event.getEventType()),
          reducedSecurityData,
          innerSourceApplicationDAO.getByPackageUrl(PackageUrlIdentifier.fromComponentIdentifier(
              event.getComponentIdentifier().createAlternativeVersion(null))) != null
      );

      PullRequestTask pullRequestTask = pullRequestTaskProvider.get();
      PullRequestResult pullRequestResult =
          pullRequestTask.run(pullRequestRemediationDetails, pullRequestExecutor);
      if (pullRequestResult.isSuccessful()) {
        event.setEventStatusDetails(pullRequestResult.getPullRequestUrl());
        Integer pullRequestNumber = extractPullRequestNumber(pullRequestResult.getPullRequestUrl());
        if (pullRequestNumber != null) {
          event.setPullRequestNumber(pullRequestNumber);
          // Record telemetry for all remediation PRs
          collectAndSendPullRequestTelemetry(event, pullRequestRemediationDetails);
        }
        sourceControlEventDAO.update(event);
      }
    }
  }

  public void onRemediatePullRequestClosing(SourceControlEvent event) throws IOException {
    GitRepositoryInfo gitRepositoryInfo =
        sourceControlUtils.getGitRepositoryInfoForApplication(event.getApplicationId());
    if (isBranchOnServer(gitRepositoryInfo, event.getBranchName())) {
      GitApiClient gitApiClient = gitClientFactory.createApiClient(gitRepositoryInfo);
      Integer prNumber = event.getPullRequestNumber();
      try {
        gitApiClient.createPullRequestComment(prNumber, event.getPullRequestContents());
        gitApiClient.closePullRequest(prNumber);
      }
      catch (Exception e) {
        log.error("Failed to close pull request {} for branch {}: {}", prNumber, event.getBranchName(), e.getMessage());
      }
    }
    else {
      log.info("Branch {} does not exist on remote server for remediation closing.", event.getBranchName());
      throw new SourceControlException(
          "Branch does not exist on remote server for remediation closing: " + event.getBranchName()
      );
    }
  }

  /**
   * Determines whether or not the given component identifier represents a format that is supported for automated
   * pull requests.
   *
   * @return true if the given component identifier is for a format that is supported for automated remediation pull
   * requests; false otherwise
   */
  public boolean isFormatSupportedForPullRequestRemediation(final String format) {
    return isNotBlank(format) && pullRequestExecutor.isSupportedFormat(format);
  }

  /**
   * Uses the SCM API to determine whether or not the given branch currently exists in the git repo.
   *
   * @param gitRepositoryInfo describes the repository to check
   * @param branchName the name of the branch to check
   * @return true if the branch already exists; false otherwise
   */
  private boolean isBranchOnServer(
      final GitRepositoryInfo gitRepositoryInfo,
      final String branchName)
      throws IOException
  {
    return gitClientFactory.createApiClient(gitRepositoryInfo).isBranchOnServer(branchName);
  }

  private Integer extractPullRequestNumber(String url) {
    try {
      String prNumberStr = url.substring(url.lastIndexOf("/") + 1);
      return Integer.parseInt(prNumberStr);
    }
    catch (Exception e) {
      log.warn("Failed to extract pull request number from URL: {}", url, e);
      return null;
    }
  }

  private void collectAndSendPullRequestTelemetry(
      SourceControlEvent event,
      PullRequestRemediationDetails pullRequestRemediationDetails)
  {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.SOURCE_CONTROL_PULL_REQUEST_ACTIVITY);

    telemetryData.put("event_type", "pr_opened");
    telemetryData.put("application_id", telemetryUtils.obfuscate(event.getApplicationId()));
    telemetryData.put("event_time", event.getCreateTime());
    telemetryData.put("pull_request_creation_type",
        pullRequestRemediationDetails.isManualPullRequest()
            ? PullRequestSource.MANUAL.name()
            : PullRequestSource.AUTOMATIC.name());
    telemetryData.put("pull_request_number", event.getPullRequestNumber());

    // Convert boolean golden status to string for consistency
    String pullRequestType = telemetryUtils.convertGoldenStatusToString(event.isGoldenPullRequest());
    telemetryData.put("pull_request_type", pullRequestType);

    telemetryData.put("component_package_url",
        PackageUrlIdentifier.fromComponentIdentifier(event.getComponentIdentifier()).getPackageUrl());

    telemetrySender.send(telemetryData);
  }
}
