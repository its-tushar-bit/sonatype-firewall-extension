/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;

@Named
@Singleton
public class SourceControlUtils
{
  public static final String DEFAULT_BASE_BRANCH = "master";

  private final ApiSourceControlService sourceControlService;

  private final ApplicationDAO applicationDAO;

  @Inject
  public SourceControlUtils(ApiSourceControlService sourceControlService, ApplicationDAO applicationDAO) {
    this.sourceControlService = sourceControlService;
    this.applicationDAO = applicationDAO;
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
        new GitRepositoryInfo(sourceControl.getRepositoryUrl(), sourceControl.getUsername(), sourceControl.getToken(),
            sourceControl.getProvider(), sourceControl.getBaseBranch(), sourceControl.getEnablePullRequests(),
            sourceControl.getEnableStatusChecks());

    if (!gitRepositoryInfo.isDataComplete()) {
      // check at sub-organization level for missing fields
      Application application = applicationDAO.getById(sourceControl.getOwnerId());
      if (application != null && application.getOrganizationId() != null) {
        SourceControl orgSourceControl =
            sourceControlService.getSourceControlByOwnerDecrypted(application.getOrganizationId());
        populateGitRepositoryInformationFromOrganization(gitRepositoryInfo, orgSourceControl);
      }

      if (!gitRepositoryInfo.isDataComplete()) {
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
    return isScmEnabled(getGitRepositoryInfoForApplication(applicationId));
  }

  public boolean isScmEnabled(GitRepositoryInfo gitRepositoryInfo) {
    if (gitRepositoryInfo == null) {
      return false;
    }
    return gitRepositoryInfo.provider != null
        && StringUtils.isNotBlank(gitRepositoryInfo.repositoryUrl)
        && StringUtils.isNotBlank(gitRepositoryInfo.token)
        && (!gitRepositoryInfo.provider.requiresUsername() || StringUtils.isNotBlank(gitRepositoryInfo.username))
        ;
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

    if (Strings.isNullOrEmpty(gitRepositoryInfo.username)) {
      gitRepositoryInfo.username = orgSourceControl.getUsername();
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
}
