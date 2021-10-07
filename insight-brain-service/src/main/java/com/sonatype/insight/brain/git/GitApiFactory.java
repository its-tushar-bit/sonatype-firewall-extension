/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.service.SourceControlConfig;
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
  public static final String NATIVE_GIT = "native";

  public static final String JGIT = "java";

  private static final Logger log = LoggerFactory.getLogger(GitApiFactory.class);

  private final SourceControlConfig sourceControlConfig;

  private final InsightWork insightWork;

  @Inject
  public GitApiFactory(final InsightConfig insightConfig, InsightWork insightWork) {
    this.sourceControlConfig = Objects
        .requireNonNull(insightConfig.getSourceControl(), "sourceControl in InsightConfig cannot be null");
    this.insightWork = insightWork;
  }

  public GitApi createGitApi(final GitRepositoryInfo gitInfo) {
    String gitImplFromConfig = sourceControlConfig.getGitImplementation();
    String gitExecutable = sourceControlConfig.getGitExecutable();
    String cloneUrl = getCloneUrl(gitInfo);
    boolean isSsh = Boolean.TRUE.equals(gitInfo.getSshEnabled());
    if (gitImplFromConfig != null) {
      if (gitImplFromConfig.equalsIgnoreCase(JGIT)) {
        return creatJGitIfAllowed(gitInfo, cloneUrl, isSsh);
      }
      else if (gitImplFromConfig.equalsIgnoreCase(NATIVE_GIT)) {
        if (!isNativeGitAvailable(gitExecutable)) {
          String messageSuffix = gitExecutable != null ? "at configured path: " + gitExecutable : "on the path";
          log.warn("System is configured to use native git, but the git executable was not found {}. Defaulting to " +
              "use {} implementation", messageSuffix, JGIT);
          return creatJGitIfAllowed(gitInfo, cloneUrl, isSsh);
        }
        NativeGitApi nativeGitApi =
            new NativeGitApi(cloneUrl, gitInfo.token, gitInfo.username, gitExecutable);
        nativeGitApi.setTempDirectory(insightWork.getTemporaryDirectory());
        return nativeGitApi;
      }
      else {
        log.error("Unknown option '{}' for configuration 'sourceControl.gitImplementation'. Available options: {}, {}",
            gitImplFromConfig, NATIVE_GIT, JGIT);
      }
    }

    if (isNativeGitAvailable(gitExecutable)) {
      NativeGitApi nativeGitApi =
          new NativeGitApi(cloneUrl, gitInfo.token, gitInfo.username, gitExecutable);
      nativeGitApi.setTempDirectory(insightWork.getTemporaryDirectory());
      return nativeGitApi;
    }
    return creatJGitIfAllowed(gitInfo, cloneUrl, isSsh);
  }

  private JGitApi creatJGitIfAllowed(GitRepositoryInfo gitInfo, String cloneUrl, boolean isSsh) {
    if (isSsh) {
      throw new IllegalArgumentException(String.format("Application with URL %s is configured to use SSH with JGit " +
          "which is not a supported combination. Update the system to use native git or disable SSH for this " +
          "application", cloneUrl));
    }
    return new JGitApi(cloneUrl, gitInfo.token, gitInfo.username);
  }

  /**
   * @param gitExecutable fully qualified path to a git executable, may be null in which case git will attempt to find
   *                     an executable in the PATH
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
