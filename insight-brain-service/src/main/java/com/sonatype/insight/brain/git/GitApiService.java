/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

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
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEvent;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.GitApiClient.StateType;
import com.sonatype.nexus.scm.api.model.Status;
import com.sonatype.nexus.scm.api.model.StatusRequest;

import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class GitApiService
{
  private static final Logger log = LoggerFactory.getLogger(GitApiService.class);

  private static final String IQ_POLICY_EVALUATION = "IQ Policy Evaluation";

  public static final String DEFAULT_BASE_BRANCH = "master";

  private final ApiSourceControlService sourceControlService;

  private final BaseUrl baseUrl;

  private final ApplicationDAO applicationDAO;

  private final GitClientFactory gitClientFactory;

  private final ProductLicense productLicense;

  @Inject
  public GitApiService(
      final ApiSourceControlService sourceControlService,
      final BaseUrl baseUrl,
      final ApplicationDAO applicationDAO,
      final GitClientFactory gitClientFactory,
      ProductLicense productLicense)
  {
    this.sourceControlService = sourceControlService;
    this.baseUrl = baseUrl;
    this.applicationDAO = applicationDAO;
    this.gitClientFactory = gitClientFactory;
    this.productLicense = productLicense;
  }

  /**
   * Responds to the application evaluation event by sending a SCM provider specific status message indicating
   * the evaluation outcome and component counts if a commit hash was send with the policy evaluation request.
   */
  public void maybeRespond(final ApplicationEvaluationEvent event) {
    if (!productLicense.hasFeature(LicensedFeature.NOTIFICATIONS)) {
      log.debug("License does not support Source Control notifications feature");
      return;
    }
    if (Strings.isNullOrEmpty(event.commitHash)) {
      return;
    }

    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfoForApplication(event.ownerId);

    if (null == gitRepositoryInfo || null == gitRepositoryInfo.provider ||
        Strings.isNullOrEmpty(gitRepositoryInfo.token)) {
      log.error("The git repository information could not be found for application with id {}, " +
          "scm status could not be created.", event.ownerId);
      return;
    }

    GitApiClient gitApiClient = gitClientFactory.create(gitRepositoryInfo);
    StatusRequest statusRequest = createStatusRequest(event, gitApiClient, gitRepositoryInfo.provider);
    log.debug("Creating a {} commit status for repository: {}, commit hash: {}, with outcome: {}, state: {}",
        gitRepositoryInfo.provider, gitApiClient.getProjectUri().getUrl(),
        event.commitHash, event.outcome, statusRequest.getState());
    try {
      Status status = gitApiClient.createStatus(event.commitHash, statusRequest);
      log.debug("Status response from api url: {}, creator: {}",
          status.getTargetUrl(), status.getUser().getUsername());
    }
    catch (IOException e) {
      log.error("Failed to update status for applicationId: {}, repository: {}, commitHash: {}, " +
              "triggered by policyEvaluationId: {}",
          event.ownerId, gitRepositoryInfo.repositoryUrl, event.commitHash, event.policyEvaluationId, e);
    }
  }

  /**
   * Returns a {@link GitRepositoryInfo} object with provider and token sourced from the organization hierarchy
   * if not available on the application SourceControl record
   *
   * @param applicationId The id of the application for which the information needs to be retrieved
   * @return The git repository information for the given application id
   */
  public GitRepositoryInfo getGitRepositoryInfoForApplication(String applicationId) {
    SourceControl sourceControl = sourceControlService.getSourceControlByOwnerDecrypted(applicationId);
    if (sourceControl == null) {
      return null;
    }

    GitRepositoryInfo gitRepositoryInfo =
        new GitRepositoryInfo(sourceControl.getRepositoryUrl(), sourceControl.getToken(), sourceControl.getProvider(),
            sourceControl.getBaseBranch(), sourceControl.getEnablePullRequests(),
            sourceControl.getEnableStatusChecks());

    if (!isGitRepoInfoComplete(gitRepositoryInfo)) {
      // check at sub-organization level for missing fields
      Application application = applicationDAO.getById(sourceControl.getOwnerId());
      if (application != null && application.getOrganizationId() != null) {
        SourceControl orgSourceControl =
            sourceControlService.getSourceControlByOwnerDecrypted(application.getOrganizationId());
        populateGitRepositoryInformationFromOrganization(gitRepositoryInfo, orgSourceControl);
      }

      if (!isGitRepoInfoComplete(gitRepositoryInfo)) {
        // fields are still missing, check at the root organization level
        SourceControl rootOrgSourceControl =
            sourceControlService.getSourceControlByOwnerDecrypted(Organization.ROOT_ORGANIZATION_ID);
        populateGitRepositoryInformationFromOrganization(gitRepositoryInfo, rootOrgSourceControl);
      }
    }

    // TODO remove this check when Aquila has enforced a default branch at the root org level
    if (Strings.isNullOrEmpty(gitRepositoryInfo.baseBranch)) {
      gitRepositoryInfo.baseBranch = DEFAULT_BASE_BRANCH;
    }

    return gitRepositoryInfo;
  }

  /**
   * Determines if source control is enabled for an application. That is <code>true</code> if:<ul>
   * <li>the app record exists and it has repository URL populated,
   * <li>the root org records exists and it has the provider populated,
   * <li>there is a token provided somewhere in the hierarchy, starting from the app record.</ul>
   * @param applicationId application ID
   * @return <code>true</code> if all above conditions are met; <code>false</code> otherwise.
   */
  public boolean isScmEnabled(final String applicationId) {
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfoForApplication(applicationId);
    if (gitRepositoryInfo == null) {
      return false;
    }
    return StringUtils.isNotBlank(gitRepositoryInfo.repositoryUrl)
        && gitRepositoryInfo.provider != null
        && StringUtils.isNotBlank(gitRepositoryInfo.token);
  }

  private boolean isGitRepoInfoComplete(final GitRepositoryInfo gitRepositoryInfo) {
    return gitRepositoryInfo.isDataComplete();
  }

  private void populateGitRepositoryInformationFromOrganization(
      final GitRepositoryInfo gitRepositoryInfo,
      final SourceControl orgSourceControl)
  {
    if (orgSourceControl == null) {
      // not required, so org-level source control may be null
      return;
    }

    if (gitRepositoryInfo.enableStatusChecks == null) {
      gitRepositoryInfo.enableStatusChecks = orgSourceControl.getEnableStatusChecks();
    }

    if (gitRepositoryInfo.enablePullRequests == null) {
      gitRepositoryInfo.enablePullRequests = orgSourceControl.getEnablePullRequests();
    }

    if (Strings.isNullOrEmpty(gitRepositoryInfo.token)) {
      gitRepositoryInfo.token = orgSourceControl.getToken();
    }

    if (Strings.isNullOrEmpty(gitRepositoryInfo.baseBranch)) {
      gitRepositoryInfo.baseBranch = orgSourceControl.getBaseBranch();
    }

    if (gitRepositoryInfo.provider == null) {
      gitRepositoryInfo.provider = orgSourceControl.getProvider();
    }
  }

  private StatusRequest createStatusRequest(final ApplicationEvaluationEvent event,
                                            final GitApiClient gitApiClient,
                                            final SourceControlProvider provider)
  {
    return gitApiClient.createStatusRequest(
        getState(event, gitApiClient),
        IQ_POLICY_EVALUATION,
        createStatusMessage(event),
        getReportUrl(event.ownerId, event.reportId, provider));
  }

  private String getReportUrl(final String ownerId,
                              final String scanId,
                              final SourceControlProvider provider)
  {
    Application application = applicationDAO.getByIdNotNull(ownerId);
    String reportPath = UserInterfaceLinksResource.getReportUrl(application.getPublicId(), scanId);
    reportPath = addSourceQuery(reportPath, provider);
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setReportUrl(reportPath);
    return scanReceipt.resolveReportUrl(baseUrl.get());
  }

  private String addSourceQuery(final String reportPath,
                                final SourceControlProvider provider)
  {
    return UriBuilder.fromPath(reportPath).queryParam("source", provider.toString()).toString();
  }

  private static String getState(final ApplicationEvaluationEvent event,
                                 final GitApiClient gitApiClient)
  {
    switch (event.outcome) {
      case ApplicationEvaluationEvent.ACTION_ID_NONE:
      case Action.ID_WARN:
        return gitApiClient.getState(StateType.SUCCESS);
      default:
        return gitApiClient.getState(StateType.FAILURE);
    }
  }

  private static String createStatusMessage(final ApplicationEvaluationEvent event) {
    return String.format("Components: Critical: %d, Severe: %d, Moderate: %d", event.criticalComponentCount,
        event.severeComponentCount, event.moderateComponentCount);
  }
}
