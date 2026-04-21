/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.git.GitClientFactory;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
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
public class SourceControlUtils
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlUtils.class);

  public static final String DEFAULT_BASE_BRANCH = "master";

  private final SourceControlDataService sourceControlDataService;

  private final InsightWork insightWork;

  private final FileCleaner fileCleaner;

  private final GitClientFactory gitClientFactory;

  private final GitHubAppDAO gitHubAppDAO;

  @Inject
  public SourceControlUtils(
      SourceControlDataService sourceControlDataService,
      InsightWork insightWork,
      FileCleaner fileCleaner,
      GitClientFactory gitClientFactory,
      GitHubAppDAO gitHubAppDAO)
  {
    this.sourceControlDataService = sourceControlDataService;
    this.insightWork = insightWork;
    this.fileCleaner = fileCleaner;
    this.gitClientFactory = gitClientFactory;
    this.gitHubAppDAO = gitHubAppDAO;
  }

  public GitRepositoryInfo getGitRepositoryInfoForApplication(String applicationId) {
    SourceControl sourceControl = sourceControlDataService.getCompositeSourceControlByOwnerDecrypted(applicationId);
    return getGitRepositoryInfoForApplicationInternal(sourceControl, applicationId);
  }

  public GitRepositoryInfo getGitRepositoryInfoForApplication(SourceControl sourceControl, String applicationId) {
    return getGitRepositoryInfoForApplicationInternal(sourceControl, applicationId);
  }

  /**
   * Internal method that builds GitRepositoryInfo and applies GitHub App authentication if needed.
   */
  private GitRepositoryInfo getGitRepositoryInfoForApplicationInternal(
      SourceControl sourceControl,
      String applicationId)
  {
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfoForApplicationStatic(sourceControl, applicationId);

    if (gitRepositoryInfo != null) {
      if (sourceControl.getAuthenticationType() == SourceControl.AuthenticationType.GITHUB_APP) {
        GitHubApp gitHubApp = gitHubAppDAO.getNearestGitHubApp(applicationId);
        gitRepositoryInfo.authOwnerId = gitHubApp != null ? gitHubApp.getOwnerId() : sourceControl.getOwnerId();
      }
      else {
        gitRepositoryInfo.authOwnerId = sourceControl.getOwnerId();
      }
    }

    return gitRepositoryInfo;
  }

  /**
   * Returns a {@link GitRepositoryInfo} object with provider and token sourced from the organization hierarchy if not
   * available on the application SourceControl record
   *
   * @param applicationId The id of the application for which the information needs to be retrieved
   * @return The git repository information for the given application id
   */
  public static GitRepositoryInfo getGitRepositoryInfoForApplicationStatic(
      SourceControl sourceControl,
      String applicationId)
  {
    // sourceControl.getOwnerId() will be different from the applicationId if no app-level SourceControl record exists
    if (sourceControl == null || !applicationId.equals(sourceControl.getOwnerId())) {
      return null;
    }

    GitRepositoryInfo gitRepositoryInfo = new GitRepositoryInfo(sourceControl.getRepositoryUrl(),
        sourceControl.getNormalizedRepositoryUrl(), sourceControl.getRepositorySshUrl(), sourceControl.getUsername(),
        sourceControl.getToken(), sourceControl.getProvider(), sourceControl.getBaseBranch(),
        sourceControl.getRemediationPullRequestsEnabled(), sourceControl.getManualPullRequestsEnabled(),
        sourceControl.getInnerSourceAutomatedUpdatesEnabled(), sourceControl.getNonGoldenPullRequestsEnabled(),
        sourceControl.getStatusChecksEnabled(), sourceControl.getPullRequestCommentingEnabled(),
        sourceControl.getSourceControlEvaluationsEnabled(), sourceControl.getSshEnabled(),
        sourceControl.getSourceControlScanTarget(), sourceControl.getAuthenticationType(), sourceControl.getOwnerId());

    if (Strings.isNullOrEmpty(gitRepositoryInfo.baseBranch)) {
      gitRepositoryInfo.baseBranch = DEFAULT_BASE_BRANCH;
    }

    return gitRepositoryInfo;
  }

  /**
   * Determines if source control is enabled for an application. That is <code>true</code> if:
   * <ul>
   * <li>the app record exists and it has repository URL populated,
   * <li>the root org records exists and it has the provider populated,
   * <li>there is a token provided somewhere in the hierarchy, starting from the app record.
   * </ul>
   *
   * @param applicationId application ID
   * @return <code>true</code> if all above conditions are met; <code>false</code> otherwise.
   */
  public boolean isScmEnabled(final String applicationId) {
    return isScmEnabled(getGitRepositoryInfoForApplication(applicationId));
  }

  public boolean isScmEnabled(GitRepositoryInfo gitRepositoryInfo) {
    if (null == gitRepositoryInfo || null == gitRepositoryInfo.provider ||
        !AuthenticationValidator.hasValidCredentials(gitRepositoryInfo))
    {
      return false;
    }
    return StringUtils.isNotBlank(gitRepositoryInfo.repositoryUrl)
        && (!gitRepositoryInfo.provider.requiresUsername() || StringUtils.isNotBlank(gitRepositoryInfo.username));
  }

  /**
   * Checks whether the checkout directory exists. If so, it is returned; otherwise it is created.
   */
  public File getCheckoutDirectory(Application app) {
    return getCheckoutDirectory(app.getId());
  }

  /**
   * Checks whether the checkout directory exists. If so, it is returned; otherwise it is created.
   */
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

  public void deleteCheckoutDirectory(Application app) {
    deleteCheckoutDirectory(app.getId());
  }

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
  public boolean isBitbucketCloud(GitRepositoryInfo gitRepositoryInfo) {
    return gitRepositoryInfo.provider.equals(BITBUCKET) &&
        BitbucketApiClientUtils.isCloudHosted(gitRepositoryInfo.normalizedRepositoryUrl);
  }

  public String getScmUserIdForApplication(String applicationId) {
    GitRepositoryInfo gitRepositoryInfo = getGitRepositoryInfoForApplication(applicationId);
    GitApiClient gitApiClient = gitClientFactory.createApiClient(gitRepositoryInfo);

    return gitApiClient.getSynchronizationKey();
  }

  /**
   * Returns a {@link GitRepositoryInfo} object with provider and token sourced from the organization hierarchy
   *
   * @param orgId The id of the organization for which the information needs to be retrieved
   * @param repoUrl The repository URL for which the information needs to be retrieved
   * @param provider The SCM provider for which the information needs to be retrieved
   * @return The git repository information for the given information
   */
  public GitRepositoryInfo getGitRepositoryInfoForRepository(
      String orgId,
      String repoUrl,
      SourceControlProvider provider)
  {
    // check up the organization hierarchy for missing fields
    SourceControl sourceControl = sourceControlDataService.getCompositeSourceControlByOwnerDecrypted(orgId);
    if (sourceControl == null || sourceControl.getOwnerId() == null) {
      return null;
    }
    GitRepositoryInfo gitRepositoryInfo = new GitRepositoryInfo();
    gitRepositoryInfo.repositoryUrl = repoUrl;
    gitRepositoryInfo.normalizedRepositoryUrl = SourceControl.normalizeRepositoryUrl(repoUrl);
    gitRepositoryInfo.provider = provider;
    gitRepositoryInfo.sshRepositoryUrl = sourceControl.getRepositorySshUrl();
    gitRepositoryInfo.username = sourceControl.getUsername();
    gitRepositoryInfo.token = sourceControl.getToken();
    gitRepositoryInfo.baseBranch = sourceControl.getBaseBranch();
    gitRepositoryInfo.remediationPullRequestsEnabled = sourceControl.getRemediationPullRequestsEnabled();
    gitRepositoryInfo.statusChecksEnabled = sourceControl.getStatusChecksEnabled();
    gitRepositoryInfo.pullRequestCommentingEnabled = sourceControl.getPullRequestCommentingEnabled();
    gitRepositoryInfo.sourceControlEvaluationsEnabled = sourceControl.getSourceControlEvaluationsEnabled();
    gitRepositoryInfo.sshEnabled = sourceControl.getSshEnabled();
    gitRepositoryInfo.sourceControlScanTarget = sourceControl.getSourceControlScanTarget();
    gitRepositoryInfo.innerSourceAutomatedUpdatesEnabled = sourceControl.getInnerSourceAutomatedUpdatesEnabled();
    gitRepositoryInfo.manualPullRequestsEnabled = sourceControl.getManualPullRequestsEnabled();
    gitRepositoryInfo.nonGoldenPullRequestsEnabled = sourceControl.getNonGoldenPullRequestsEnabled();
    gitRepositoryInfo.authenticationType = sourceControl.getAuthenticationType();

    if (sourceControl.getAuthenticationType() == SourceControl.AuthenticationType.GITHUB_APP) {
      GitHubApp gitHubApp = gitHubAppDAO.getNearestGitHubApp(orgId);
      gitRepositoryInfo.authOwnerId = gitHubApp != null ? gitHubApp.getOwnerId() : sourceControl.getOwnerId();
    }
    else {
      gitRepositoryInfo.authOwnerId = sourceControl.getOwnerId();
    }

    return gitRepositoryInfo;
  }

  public static boolean isGolden(ApiVersionChangeOptionType remediationType) {
    return ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES.equals(remediationType);
  }
}
