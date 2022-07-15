/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.model.sourcecontrol.GitImplementation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.git.utils.api.JGitApi;
import com.sonatype.nexus.git.utils.api.NativeGitApi;
import com.sonatype.nexus.git.utils.api.NativeGitUtils;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class GitApiFactory 
{
  private static final Logger log = LoggerFactory.getLogger(GitApiFactory.class);

  private final Configuration configuration;

  private final InsightWork insightWork;

  @Inject
  public GitApiFactory(Configuration configuration, InsightWork insightWork) {
    this.configuration = configuration;
    this.insightWork = insightWork;
  }

  public GitApi createGitApi(final GitRepositoryInfo gitInfo) {
    SourceControlConfiguration sourceControlConfiguration = configuration.getSourceControlConfigurationOrDefault();
    GitImplementation gitImplFromConfig = sourceControlConfiguration.getGitImplementation();
    String gitExecutable = sourceControlConfiguration.getGitExecutable();
    int gitTimeoutSeconds = sourceControlConfiguration.getGitTimeoutSeconds();
    String cloneUrl = getCloneUrl(gitInfo);
    boolean isSsh = Boolean.TRUE.equals(gitInfo.getSshEnabled());
    if (gitImplFromConfig != null) {
      if (GitImplementation.JAVA.equals(gitImplFromConfig)) {
        return creatJGitIfAllowed(gitTimeoutSeconds, gitInfo, cloneUrl, isSsh);
      }
      else if (GitImplementation.NATIVE.equals(gitImplFromConfig)) {
        if (!isNativeGitAvailable(gitExecutable)) {
          String messageSuffix = gitExecutable != null ? "at configured path: " + gitExecutable : "on the path";
          log.warn("System is configured to use native git, but the git executable was not found {}. Defaulting to " +
              "use {} implementation", messageSuffix, GitImplementation.JAVA);
          return creatJGitIfAllowed(gitTimeoutSeconds, gitInfo, cloneUrl, isSsh);
        }
        return creatNativeGitApi(gitTimeoutSeconds, gitInfo, cloneUrl, gitExecutable);
      }
      else {
        log.error("Unknown option '{}' for configuration 'sourceControl.gitImplementation'. Available options: {}, {}",
            gitImplFromConfig, GitImplementation.NATIVE, GitImplementation.JAVA);
      }
    }

    if (isNativeGitAvailable(gitExecutable)) {
      return creatNativeGitApi(gitTimeoutSeconds, gitInfo, cloneUrl, gitExecutable);
    }
    return creatJGitIfAllowed(gitTimeoutSeconds, gitInfo, cloneUrl, isSsh);
  }

  private NativeGitApi creatNativeGitApi(
      int gitTimeoutSeconds,
      GitRepositoryInfo gitInfo,
      String cloneUrl,
      String gitExecutable)
  {
    NativeGitApi nativeGitApi;
    if (gitTimeoutSeconds > 0) {
      nativeGitApi = new NativeGitApi(gitTimeoutSeconds, cloneUrl, gitInfo.token, gitInfo.username, gitExecutable);
    }
    else {
      nativeGitApi = new NativeGitApi(cloneUrl, gitInfo.token, gitInfo.username, gitExecutable);
    }
    nativeGitApi.setTempDirectory(insightWork.getTemporaryDirectory());
    return nativeGitApi;
  }

  private JGitApi creatJGitIfAllowed(int gitTimeoutSeconds, GitRepositoryInfo gitInfo, String cloneUrl, boolean isSsh) {
    if (isSsh) {
      throw new IllegalArgumentException(String.format("Application with URL %s is configured to use SSH with JGit " +
          "which is not a supported combination. Update the system to use native git or disable SSH for this " +
          "application", cloneUrl));
    }
    if (gitTimeoutSeconds > 0) {
      return new JGitApi(gitTimeoutSeconds, cloneUrl, gitInfo.token, gitInfo.username);
    }
    else {
      return new JGitApi(cloneUrl, gitInfo.token, gitInfo.username);
    }
  }

  /**
   * @param gitExecutable fully qualified path to a git executable, may be null in which case git will attempt to find
   *                      an executable in the PATH
   */
  @VisibleForTesting
  boolean isNativeGitAvailable(String gitExecutable) {
    return NativeGitUtils.isNativeGitAvailable(gitExecutable);
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
