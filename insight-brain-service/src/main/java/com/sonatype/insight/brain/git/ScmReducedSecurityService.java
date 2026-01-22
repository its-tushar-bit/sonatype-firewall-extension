/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.license.model.LicensedFeature;

/**
 * We have a special license flag to allow SCM actions on public repositories (See
 * {@link LicensedFeature#ALLOW_SCM_ON_PUBLIC_REPOS}). With this flag on, and when a PR action occurs
 * (create/comment/etc...), we do NOT want to leak security violation details such as CVEs into public repositories.
 * <p>
 * This service can be used to determine if the security data should be reduced or not. Higher level SCM code will have
 * already used the license flag to determine if SCM features are allowed on public repositories. When SCM content is
 * being produced, then for any repo that is public we will want to reduce the security data.
 */
@Named
@Singleton
public class ScmReducedSecurityService
{
  private final ScmRepoVisibilityService scmRepoVisibilityService;

  private final SourceControlUtils sourceControlUtils;

  @Inject
  public ScmReducedSecurityService(
      final ScmRepoVisibilityService scmRepoVisibilityService,
      final SourceControlUtils sourceControlUtils)
  {
    this.scmRepoVisibilityService = scmRepoVisibilityService;
    this.sourceControlUtils = sourceControlUtils;
  }

  /**
   * Return if the security data should be reduced or not. This method assumes that if it is being called, then the
   * repository is either private, or it is public <B>AND</B> has the ALLOW_SCM_ON_PUBLIC_REPOS feature enabled. So we
   * return false if the repo is private (no need to reduce security data), else we return true as then we must be in a
   * situation where the repo is public and the flag is enabled.
   */
  public boolean isReducedSecurityData(final String applicationId) {
    GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(applicationId);

    // return false if the repo is private, else return true
    return !scmRepoVisibilityService.isPrivateRepository(gitRepositoryInfo);
  }
}
