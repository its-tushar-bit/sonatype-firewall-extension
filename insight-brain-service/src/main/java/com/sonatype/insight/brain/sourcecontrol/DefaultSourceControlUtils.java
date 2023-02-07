/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.git.GitClientFactory;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.bitbucket.BitbucketApiClientUtils;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.nexus.scm.SourceControlProvider.BITBUCKET;

@Named
@Singleton
public class DefaultSourceControlUtils implements SourceControlUtils
{
  private static final Logger log = LoggerFactory.getLogger(DefaultSourceControlUtils.class);

  public static final String DEFAULT_BASE_BRANCH = "master";

  private final ApiSourceControlService sourceControlService;

  private final ApplicationDAO applicationDAO;

  private final InsightWork insightWork;

  private final FileCleaner fileCleaner;

  private final GitClientFactory gitClientFactory;

  @Inject
  public DefaultSourceControlUtils(
      ApiSourceControlService sourceControlService,
      ApplicationDAO applicationDAO,
      InsightWork insightWork,
      FileCleaner fileCleaner,
      GitClientFactory gitClientFactory)
  {
    this.sourceControlService = sourceControlService;
    this.applicationDAO = applicationDAO;
    this.insightWork = insightWork;
    this.fileCleaner = fileCleaner;
    this.gitClientFactory = gitClientFactory;
  }

  /**
   * Returns a {@link GitRepositoryInfo} object with provider and token sourced from the organization hierarchy
   * if not available on the application SourceControl record
   *
   * @param applicationId The id of the application for which the information needs to be retrieved
   * @return The git repository information for the given application id
   */
  @Override
  public GitRepositoryInfo getGitRepositoryInfoForApplication(String applicationId) {
    SourceControl sourceControl = sourceControlService.getSourceControlByOwnerDecrypted(applicationId);
    if (sourceControl == null) {
      return null;
    }

    GitRepositoryInfo gitRepositoryInfo = new GitRepositoryInfo(
        sourceControl.getRepositoryUrl(), sourceControl.getNormalizedRepositoryUrl(),
        sourceControl.getRepositorySshUrl(), sourceControl.getUsername(), sourceControl.getToken(),
        sourceControl.getProvider(), sourceControl.getBaseBranch(), sourceControl.getRemediationPullRequestsEnabled(),
        sourceControl.getStatusChecksEnabled(),
        sourceControl.getPullRequestCommentingEnabled(), sourceControl.getSourceControlEvaluationsEnabled(),
        sourceControl.getSshEnabled(), sourceControl.getSourceControlScanTarget());

    if (!gitRepositoryInfo.isDataComplete()) {
      // check at sub-organization level for missing fields
      Application application = applicationDAO.getById(sourceControl.getOwnerId());
      if (application != null && application.getOrganizationId() != null) {
        SourceControl orgSourceControl =
            sourceControlService.getSourceControlByOwnerDecrypted(application.getOrganizationId());
        populateGitRepositoryInformationFromOrganization(gitRepositoryInfo, orgSourceControl);
      }

      // check at root organization level for any missing field
      populateGitRepositoryInformationFromRootOrganization(gitRepositoryInfo);
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
  @Override
  public boolean isScmEnabled(final String applicationId) {
    return isScmEnabled(getGitRepositoryInfoForApplication(applicationId));
  }

  @Override
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

    if (gitRepositoryInfo.statusChecksEnabled == null) {
      gitRepositoryInfo.statusChecksEnabled = orgSourceControl.getStatusChecksEnabled();
    }

    if (gitRepositoryInfo.remediationPullRequestsEnabled == null) {
      gitRepositoryInfo.remediationPullRequestsEnabled = orgSourceControl.getRemediationPullRequestsEnabled();
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

    if (gitRepositoryInfo.pullRequestCommentingEnabled == null) {
      gitRepositoryInfo.pullRequestCommentingEnabled = orgSourceControl.getPullRequestCommentingEnabled();
    }

    if (gitRepositoryInfo.sourceControlEvaluationsEnabled == null) {
      gitRepositoryInfo.sourceControlEvaluationsEnabled = orgSourceControl.getSourceControlEvaluationsEnabled();
    }

    if (gitRepositoryInfo.sshEnabled == null) {
      gitRepositoryInfo.sshEnabled = orgSourceControl.getSshEnabled();
    }

    if (gitRepositoryInfo.sourceControlScanTarget == null) {
      gitRepositoryInfo.sourceControlScanTarget = orgSourceControl.getSourceControlScanTarget();
    }
  }

  private void populateGitRepositoryInformationFromRootOrganization(final GitRepositoryInfo gitRepositoryInfo) {
    // if there are missing fields, check at the root organization level
    if (!gitRepositoryInfo.isDataComplete()) {
      SourceControl rootOrgSourceControl =
          sourceControlService.getSourceControlByOwnerDecrypted(Organization.ROOT_ORGANIZATION_ID);
      populateGitRepositoryInformationFromOrganization(gitRepositoryInfo, rootOrgSourceControl);
    }
  }

  /**
   * Checks whether the checkout directory exists. If so, it is returned; otherwise it is created.
   */
  @Override
  public File getCheckoutDirectory(Application app) {
    return getCheckoutDirectory(app.getId());
  }

  /**
   * Checks whether the checkout directory exists. If so, it is returned; otherwise it is created.
   */
  @Override
  public File getCheckoutDirectory(String appId) {
    File checkoutDir = insightWork.getSourceControlDir(appId);

    if (checkoutDir.exists()) {
      log.debug("Using existing directory for pull request task: {}", checkoutDir.getAbsolutePath());
    }
    else {
      try {
        Files.createDirectories(checkoutDir.toPath());
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      log.debug("Created new directory for pull request task: {}", checkoutDir.getAbsolutePath());
    }
    return checkoutDir;
  }

  @Override
  public void deleteCheckoutDirectory(Application app) {
    deleteCheckoutDirectory(app.getId());
  }

  @Override
  public void deleteCheckoutDirectory(String appId) {
    File checkoutDir = insightWork.getSourceControlDir(appId);
    try {
      fileCleaner.delete(checkoutDir);
    }
    catch (FileDeletionException e) {
      log.error("Failed to remove checkout directory '{}': {}", checkoutDir.getAbsolutePath(), e.getMessage(), e);
    }
  }

  /**
   * Pull request commenting features are not yet supported for Bitbucket cloud so provide logic to recognize any
   * repositories in that SCM.
   */
  @Override
  public boolean isBitbucketCloud(GitRepositoryInfo gitRepositoryInfo) {
    return gitRepositoryInfo.provider.equals(BITBUCKET) &&
        BitbucketApiClientUtils.isCloudHosted(gitRepositoryInfo.normalizedRepositoryUrl);
  }

  @Override
  public String getScmUserIdForApplication(String applicationId) {
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfoForApplication(applicationId);
    GitApiClient gitApiClient = gitClientFactory.createApiClient(gitRepositoryInfo);

    return gitApiClient.getUserId();
  }

  /**
   * Returns a {@link GitRepositoryInfo} object with provider and token sourced from the organization hierarchy
   *
   * @param orgId The id of the organization for which the information needs to be retrieved
   * @param repoUrl The repository URL for which the information needs to be retrieved
   * @param provider The SCM provider for which the information needs to be retrieved
   * @return The git repository information for the given information
   */
  @Override
  public GitRepositoryInfo getGitRepositoryInfoForRepository(
      String orgId,
      String repoUrl,
      SourceControlProvider provider)
  {
    GitRepositoryInfo gitRepositoryInfo = new GitRepositoryInfo();
    gitRepositoryInfo.repositoryUrl = repoUrl;
    gitRepositoryInfo.normalizedRepositoryUrl = SourceControl.normalizeRepositoryUrl(repoUrl);
    gitRepositoryInfo.provider = provider;

    // check at sub-organization level for missing fields
    SourceControl orgSourceControl = sourceControlService.getSourceControlByOwnerDecrypted(orgId);
    populateGitRepositoryInformationFromOrganization(gitRepositoryInfo, orgSourceControl);
    populateGitRepositoryInformationFromRootOrganization(gitRepositoryInfo);

    return gitRepositoryInfo;
  }
}
