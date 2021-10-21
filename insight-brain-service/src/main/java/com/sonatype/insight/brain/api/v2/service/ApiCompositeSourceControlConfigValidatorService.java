/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import javax.inject.Inject;

import com.sonatype.insight.brain.git.ConfigurationValidationResult;
import com.sonatype.insight.brain.git.GitApiFactory;
import com.sonatype.insight.brain.git.GitClientFactory;
import com.sonatype.insight.brain.git.PullRequestRepositoryValidator;
import com.sonatype.insight.brain.git.SourceControlSshService;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.git.utils.api.GitException;
import com.sonatype.nexus.git.utils.api.NativeGitApi;
import com.sonatype.nexus.iq.manager.RepositorySyncCommand;
import com.sonatype.nexus.iq.manager.RepositorySyncExecutor;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.model.ValidationResult;

import org.apache.commons.lang3.StringUtils;

/**
 * Validates the Source Control config for a given application
 */
public class ApiCompositeSourceControlConfigValidatorService
{
  private final SourceControlUtils sourceControlUtils;

  private final GitClientFactory gitClientFactory;

  private final GitApiFactory gitApiFactory;

  private final PullRequestRepositoryValidator pullRequestRepositoryValidator;

  private final SourceControlSshService sourceControlSshService;

  @Inject
  public ApiCompositeSourceControlConfigValidatorService(
      SourceControlUtils sourceControlUtils,
      GitClientFactory gitClientFactory,
      GitApiFactory gitApiFactory,
      PullRequestRepositoryValidator pullRequestRepositoryValidator,
      SourceControlSshService sourceControlSshService
  )
  {
    this.sourceControlUtils = sourceControlUtils;
    this.gitClientFactory = gitClientFactory;
    this.gitApiFactory = gitApiFactory;
    this.pullRequestRepositoryValidator = pullRequestRepositoryValidator;
    this.sourceControlSshService = sourceControlSshService;
  }

  @Authorize(permission = Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION)
  public ConfigurationValidationResult validateSourceControlConfig(String applicationId) {
    ConfigurationValidationResult result = new ConfigurationValidationResult();
    GitRepositoryInfo gitInfo = sourceControlUtils.getGitRepositoryInfoForApplication(applicationId);
    if (gitInfo == null) {
      result.setConfigurationComplete(new ValidationResult(false, "Some required values are missing or unsaved"));
      return result;
    }
    result.setConfigurationComplete(new ValidationResult(true));

    try {
      if (!pullRequestRepositoryValidator.isInternalRepository(gitInfo) &&
          !pullRequestRepositoryValidator.isPrivateRepository(gitInfo)) {
        result.setRepoPrivate(new ValidationResult(false,
            "Repository must be private or internal to enable all SCM features. " +
                "Support for public repositories is limited."));
      }
      else {
        result.setRepoPrivate(new ValidationResult(true));
      }
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

    validateSshConfiguration(applicationId, result, gitInfo);

    return result;
  }

  private void validateSshConfiguration(String applicationId, ConfigurationValidationResult result,
                                        GitRepositoryInfo gitInfo)
  {
    if (!Boolean.TRUE.equals(gitInfo.sshEnabled)) {
      // SSH disabled, no need to perform further validation
      return;
    }

    // attempt to auto-populate SSH URL as it is not user-editable
    if (StringUtils.isEmpty(gitInfo.sshRepositoryUrl)) {
      sourceControlSshService.verifySshUrlAndUpdateIfNeeded(applicationId);
      GitRepositoryInfo updatedGitInfo = sourceControlUtils.getGitRepositoryInfoForApplication(applicationId);
      if (updatedGitInfo != null) {
        gitInfo.sshRepositoryUrl = updatedGitInfo.sshRepositoryUrl;
      }
    }
    if (StringUtils.isEmpty(gitInfo.sshRepositoryUrl)) {
      result.setSshConfiguration(new ValidationResult(false, "Unable to determine the SSH URL."));
      return;
    }

    // native git is mandatory for SSH
    GitApi gitApi;
    try {
      gitApi = gitApiFactory.createGitApi(gitInfo);
      if (!(gitApi instanceof NativeGitApi)) {
        result.setSshConfiguration(new ValidationResult(false, "SSH requires native git. It is either not configured "
            + "or the 'git' executable has not been found"));
        return;
      }
    }
    catch (IllegalArgumentException e) {
      result.setSshConfiguration(new ValidationResult(false, e.getMessage()));
      return;
    }

    // the real test: see if we can successfully pull or clone
    File repositoryDirectory = sourceControlUtils.getCheckoutDirectory(applicationId);
    RepositorySyncCommand syncCommand = new RepositorySyncCommand(gitApi, gitInfo.baseBranch, null,
        repositoryDirectory);
    try {
      new RepositorySyncExecutor().execute(syncCommand);
      result.setSshConfiguration(new ValidationResult(true));
    }
    catch (GitException e) {
      result.setSshConfiguration(new ValidationResult(false, "Unable to clone a repository using SSH, check that " +
          "your SSH keys are configured properly and available to IQ. Full error: " +
          e.getMessage()));
    }
    catch (Exception e) {
      result.setSshConfiguration(new ValidationResult(false,
          "Unable to clone a repository using SSH: " + e.getMessage()));
    }
  }
}
