/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.io.UncheckedIOException;

import javax.inject.Inject;

import com.sonatype.insight.brain.git.ConfigurationValidationResult;
import com.sonatype.insight.brain.git.GitClientFactory;
import com.sonatype.insight.brain.git.PullRequestRepositoryValidator;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.model.ValidationResult;

/**
 * Validates the Source Control config for a given application
 */
public class ApiCompositeSourceControlConfigValidatorService
{
  private final SourceControlUtils sourceControlUtils;

  private final GitClientFactory gitClientFactory;

  private final PullRequestRepositoryValidator pullRequestRepositoryValidator;

  @Inject
  public ApiCompositeSourceControlConfigValidatorService(
      SourceControlUtils sourceControlUtils,
      GitClientFactory gitClientFactory,
      PullRequestRepositoryValidator pullRequestRepositoryValidator
  )
  {
    this.sourceControlUtils = sourceControlUtils;
    this.gitClientFactory = gitClientFactory;
    this.pullRequestRepositoryValidator = pullRequestRepositoryValidator;
  }

  @Authorize(permission = Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION)
  public ConfigurationValidationResult validateSourceControlConfig(String internalOwnerId) {
    ConfigurationValidationResult result = new ConfigurationValidationResult();
    GitRepositoryInfo gitInfo = sourceControlUtils.getGitRepositoryInfoForApplication(internalOwnerId);
    if (gitInfo == null) {
      result.setConfigurationComplete(new ValidationResult(false, "Some required values are missing or unsaved"));
      return result;
    }
    result.setConfigurationComplete(new ValidationResult(true));

    try {
      if (!pullRequestRepositoryValidator.isInternalRepository(gitInfo) &&
          !pullRequestRepositoryValidator.isPrivateRepository(gitInfo)) {
        result.setRepoPrivate(new ValidationResult(false, "Repository must be private or internal"));
        return result;
      }
      result.setRepoPrivate(new ValidationResult(true));
    }
    catch (UncheckedIOException e) {
      result.setRepoPrivate(new ValidationResult(false, "Unable to connect to repo: " + e.getMessage()));
    }
    catch (Exception e) {
      result.setRepoPrivate(new ValidationResult(false, "Unable to determine if repo is private: " + e.getMessage()));
    }

    try {
      GitApiClient gitApiClient = gitClientFactory.createApiClient(gitInfo);
      result.setTokenPermissions(gitApiClient.validateTokenPermissions());
    }
    catch (IOException e) {
      result.setTokenPermissions(new ValidationResult(false, "Unable to test permissions: " + e.getMessage()));
    }

    return result;
  }
}
