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
import com.sonatype.insight.brain.service.SourceControlConfig;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.git.utils.api.JGitApi;
import com.sonatype.nexus.git.utils.api.NativeGitApi;
import com.sonatype.nexus.git.utils.api.NativeGitUtils;

import com.google.common.annotations.VisibleForTesting;
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

  @Inject
  public GitApiFactory(final InsightConfig insightConfig) {
    this.sourceControlConfig = Objects
        .requireNonNull(insightConfig.getSourceControl(), "sourceControl in InSightConfig cannot be null");
  }

  @VisibleForTesting
  GitApi createGitApi(final GitRepositoryInfo gitInfo) {
    String gitImplFromConfig = sourceControlConfig.getGitImplementation();
    String gitExecutable = sourceControlConfig.getGitExecutable();
    if (gitImplFromConfig != null) {
      if (gitImplFromConfig.equalsIgnoreCase(JGIT)) {
        return new JGitApi(gitInfo.repositoryUrl, gitInfo.token);
      }
      else if (gitImplFromConfig.equalsIgnoreCase(NATIVE_GIT)) {
        if (!isNativeGitAvailable(gitExecutable)) {
          String messageSuffix = gitExecutable != null ? "at configured path: " + gitExecutable : "on the path";
          log.warn("System is configured to use native git, but the git executable was not found {}. Defaulting to " +
              "use {} implementation", messageSuffix, JGIT);
          return new JGitApi(gitInfo.repositoryUrl, gitInfo.token);
        }
        return new NativeGitApi(gitInfo.repositoryUrl, gitInfo.token, gitExecutable);
      }
      else {
        log.error("Unknown option '{}' for configuration 'sourceControl.gitImplementation'. Available options: {}, {}",
            gitImplFromConfig, NATIVE_GIT, JGIT);
      }
    }

    return isNativeGitAvailable(gitExecutable) ?
        new NativeGitApi(gitInfo.repositoryUrl, gitInfo.token, gitExecutable) :
        new JGitApi(gitInfo.repositoryUrl, gitInfo.token);
  }

  /**
   * @param gitExecutable fully qualified path to a git executable, may be null in which case git will attempt to find
   *                     an executable in the PATH
   */
  @VisibleForTesting
  boolean isNativeGitAvailable(String gitExecutable) { 
    return NativeGitUtils.isNativeGitAvailable(gitExecutable);
  }
}
