/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.model.sourcecontrol.GitImplementation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.git.utils.api.JGitApi;
import com.sonatype.nexus.git.utils.api.NativeGitApi;
import com.sonatype.nexus.git.utils.api.NativeGitUtils;
import com.sonatype.nexus.git.utils.api.NativeGitUtilsProvider;
import com.sonatype.nexus.scm.github.auth.GitHubAppAuthStrategy;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class GitApiFactory
{

  private static final Logger log = LoggerFactory.getLogger(GitApiFactory.class);

  /** CLM-39124 - Username for bitbucket cloning **/
  public static final String X_BITBUCKET_API_TOKEN_AUTH = "x-bitbucket-api-token-auth";

  private final Configuration configuration;

  private final InsightWork insightWork;

  private final PasswordHandler passwordHandler;

  private final GitHubAppDAO githubAppDAO;

  private final InsightProxy insightProxy;

  private final GitHubAppAuthStrategyCache authStrategyCache;

  private final SourceControlUtils sourceControlUtils;

  private final NativeGitUtilsProvider nativeGitUtilsProvider = new NativeGitUtilsProvider();

  @Inject
  public GitApiFactory(
      final Configuration configuration,
      final InsightWork insightWork,
      final PasswordHandler passwordHandler,
      final GitHubAppDAO githubAppDAO,
      final InsightProxy insightProxy,
      final GitHubAppAuthStrategyCache authStrategyCache,
      final SourceControlUtils sourceControlUtils)
  {
    this.configuration = configuration;
    this.insightWork = insightWork;
    this.passwordHandler = passwordHandler;
    this.githubAppDAO = githubAppDAO;
    this.insightProxy = insightProxy;
    this.authStrategyCache = authStrategyCache;
    this.sourceControlUtils = sourceControlUtils;
  }

  public GitApi createGitApi(final GitRepositoryInfo gitInfo) {
    SourceControlConfiguration sourceControlConfiguration = configuration.getSourceControlConfigurationOrDefault();
    GitImplementation gitImplFromConfig = sourceControlConfiguration.getGitImplementation();
    String gitExecutable = sourceControlConfiguration.getGitExecutable();
    int gitTimeoutSeconds = sourceControlConfiguration.getGitTimeoutSeconds();
    String gpgSigningKey = sourceControlConfiguration.getGpgSigningKey();
    String gpgPassphrase = decryptGpgPassphrase(sourceControlConfiguration.getGpgPassphrase());
    String cloneUrl = getCloneUrl(gitInfo);
    boolean isSsh = Boolean.TRUE.equals(gitInfo.getSshEnabled());
    if (gitImplFromConfig != null) {
      if (GitImplementation.JAVA.equals(gitImplFromConfig)) {
        return creatJGitIfAllowed(gitTimeoutSeconds, gitInfo, cloneUrl, isSsh, gpgSigningKey, gpgPassphrase);
      }
      else if (GitImplementation.NATIVE.equals(gitImplFromConfig)) {
        if (!isNativeGitAvailable(gitExecutable)) {
          String messageSuffix = gitExecutable != null ? "at configured path: " + gitExecutable : "on the path";
          log.warn("System is configured to use native git, but the git executable was not found {}. Defaulting to " +
              "use {} implementation", messageSuffix, GitImplementation.JAVA);
          return creatJGitIfAllowed(gitTimeoutSeconds, gitInfo, cloneUrl, isSsh, gpgSigningKey, gpgPassphrase);
        }
        return creatNativeGitApi(gitTimeoutSeconds, gitInfo, cloneUrl, gitExecutable, gpgSigningKey, gpgPassphrase);
      }
      else {
        log.error("Unknown option '{}' for configuration 'sourceControl.gitImplementation'. Available options: {}, {}",
            gitImplFromConfig, GitImplementation.NATIVE, GitImplementation.JAVA);
      }
    }

    if (isNativeGitAvailable(gitExecutable)) {
      return creatNativeGitApi(gitTimeoutSeconds, gitInfo, cloneUrl, gitExecutable, gpgSigningKey, gpgPassphrase);
    }
    return creatJGitIfAllowed(gitTimeoutSeconds, gitInfo, cloneUrl, isSsh, gpgSigningKey, gpgPassphrase);
  }

  /**
   * Resolves the authentication token for Git operations.
   * If GitHub App authentication is configured, generates an installation token using the cached strategy.
   * Otherwise, falls back to the PAT token from gitInfo.
   *
   * @param gitInfo repository information including authentication details
   * @return authentication token (GitHub App installation token or PAT)
   */
  private String resolveAuthenticationToken(final GitRepositoryInfo gitInfo) {
    if (SourceControl.AuthenticationType.GITHUB_APP.equals(gitInfo.authenticationType)) {
      if (StringUtils.isBlank(gitInfo.githubAppId)) {
        throw new IllegalArgumentException(
            "GitHub App authentication is configured but no GitHub App ID found for authentication lookup. "
                + "Repository: " + gitInfo.normalizedRepositoryUrl
                + ". Please ensure a GitHub App is registered at the application or parent organization level.");
      }

      log.info("Using GitHub App authentication for repository cloning (githubAppId: {})", gitInfo.githubAppId);

      GitHubAppAuthStrategy authStrategy = authStrategyCache.getOrCreate(gitInfo.githubAppId);
      try {
        return authStrategy.getInstallationToken().getToken();
      }
      catch (IOException e) {
        throw new UncheckedIOException("Failed to get installation token for githubAppId: " + gitInfo.githubAppId, e);
      }
    }

    return gitInfo.token;
  }

  private NativeGitApi creatNativeGitApi(
      int gitTimeoutSeconds,
      GitRepositoryInfo gitInfo,
      String cloneUrl,
      String gitExecutable,
      String gpgSigningKey,
      String gpgPassphrase)
  {
    // Resolve authentication token (PAT or GitHub App)
    String authToken = resolveAuthenticationToken(gitInfo);
    String username = getEffectiveUsername(gitInfo);

    NativeGitApi nativeGitApi;
    if (gitTimeoutSeconds > 0) {
      nativeGitApi = new NativeGitApi(gitTimeoutSeconds, cloneUrl, authToken, username, gitExecutable,
          gpgSigningKey, gpgPassphrase);
    }
    else {
      nativeGitApi = new NativeGitApi(cloneUrl, authToken, username, gitExecutable,
          gpgSigningKey, gpgPassphrase);
    }
    nativeGitApi.setTempDirectory(insightWork.getTemporaryDirectory());
    return nativeGitApi;
  }

  private JGitApi creatJGitIfAllowed(
      int gitTimeoutSeconds,
      GitRepositoryInfo gitInfo,
      String cloneUrl,
      boolean isSsh,
      String gpgSigningKey,
      String gpgPassphrase)
  {
    if (isSsh) {
      throw new IllegalArgumentException(String.format("Application with URL %s is configured to use SSH with JGit " +
          "which is not a supported combination. Update the system to use native git or disable SSH for this " +
          "application", cloneUrl));
    }

    // Resolve authentication token (PAT or GitHub App)
    String authToken = resolveAuthenticationToken(gitInfo);
    String username = getEffectiveUsername(gitInfo);

    char[] passphrase = Optional.ofNullable(gpgPassphrase)
        .map(String::toCharArray)
        .orElse(null);

    if (gitTimeoutSeconds > 0) {
      return new JGitApi(gitTimeoutSeconds, cloneUrl, authToken, username, gpgSigningKey, passphrase);
    }
    else {
      return new JGitApi(cloneUrl, authToken, username, gpgSigningKey, passphrase);
    }
  }

  /**
   * @param gitExecutable fully qualified path to a git executable, may be null in which case git will attempt to find
   *          an executable in the PATH
   */
  @VisibleForTesting
  boolean isNativeGitAvailable(String gitExecutable) {
    NativeGitUtils nativeGitUtils = nativeGitUtilsProvider.get();
    return nativeGitUtils.isNativeGitAvailable(gitExecutable);
  }

  private String decryptGpgPassphrase(String encryptedGpgPassphrase) {
    if (encryptedGpgPassphrase == null) {
      return null;
    }
    return passwordHandler.decryptPassword(encryptedGpgPassphrase);
  }

  private String getEffectiveUsername(final GitRepositoryInfo gitInfo) {
    if (gitInfo.provider != null && sourceControlUtils.isBitbucketCloud(gitInfo)) {
      return X_BITBUCKET_API_TOKEN_AUTH;
    }

    return gitInfo.username != null ? gitInfo.username : "x-access-token";
  }

  private String getCloneUrl(final GitRepositoryInfo gitRepositoryInfo) {
    if (Boolean.TRUE.equals(gitRepositoryInfo.getSshEnabled())) {
      if (StringUtils.isEmpty(gitRepositoryInfo.getSshRepositoryUrl())) {
        // SSH is enabled, but there is no SSH URL
        throw new RuntimeException(String.format("SSH is enabled for repository '%s' but no SSH clone URL was " +
            "present. Check logs for errors retreiving the SSH URL. It will be attempted to be retrieved again on " +
            "the next SCM operation.", gitRepositoryInfo.getRepositoryUrl()));
      }
      return gitRepositoryInfo.getSshRepositoryUrl();
    }
    return gitRepositoryInfo.getRepositoryUrl();
  }
}
