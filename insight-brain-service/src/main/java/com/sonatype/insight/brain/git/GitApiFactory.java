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

  private static final boolean NATIVE_GIT_ENABLED_BY_DEFAULT = false;

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
    if (gitImplFromConfig != null) {
      if (gitImplFromConfig.equalsIgnoreCase(JGIT)) {
        return new JGitApi(gitInfo.repositoryUrl, gitInfo.token);
      }
      else if (gitImplFromConfig.equalsIgnoreCase(NATIVE_GIT)) {
        if (!isNativeGitAvailable()) {
          log.warn("System is configured to use native git, but the git executable was not found on the system path");
        }
        return new NativeGitApi(gitInfo.repositoryUrl, gitInfo.token);
      }
      else {
        log.error("Unknown option '{}' for configuration 'sourceControl.gitImplementation'. Available options: {}, {}",
            gitImplFromConfig, NATIVE_GIT, JGIT);
      }
    }

    // TODO remove guard once Native Git support has been tested more thoroughly
    if (NATIVE_GIT_ENABLED_BY_DEFAULT) {
      return isNativeGitAvailable() ?
          new NativeGitApi(gitInfo.repositoryUrl, gitInfo.token) :
          new JGitApi(gitInfo.repositoryUrl, gitInfo.token);
    }
    return new JGitApi(gitInfo.repositoryUrl, gitInfo.token);
  }

  @VisibleForTesting
  boolean isNativeGitAvailable() {
    return NativeGitUtils.isNativeGitAvailable();
  }
}
