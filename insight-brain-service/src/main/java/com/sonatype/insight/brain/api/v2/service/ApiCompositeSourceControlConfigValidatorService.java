/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.sonatype.insight.brain.git.ConfigurationValidationResult;
import com.sonatype.insight.brain.git.GitApiFactory;
import com.sonatype.insight.brain.git.GitClientFactory;
import com.sonatype.insight.brain.git.ScmRepoVisibilityService;
import com.sonatype.insight.brain.git.SourceControlSshService;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.git.utils.api.GitApi;
import com.sonatype.nexus.git.utils.api.NativeGitApi;
import com.sonatype.nexus.iq.manager.RepositorySyncCommand;
import com.sonatype.nexus.iq.manager.RepositorySyncExecutor;
import com.sonatype.nexus.scm.InvalidRepositoryUrlException;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.model.ValidationResult;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.File;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates the Source Control config for a given application
 */
@Named
@Singleton
public class ApiCompositeSourceControlConfigValidatorService
{
  private static final Logger log = LoggerFactory.getLogger(ApiCompositeSourceControlConfigValidatorService.class);

  private final SourceControlUtils sourceControlUtils;

  private final GitClientFactory gitClientFactory;

  private final GitApiFactory gitApiFactory;

  private final ScmRepoVisibilityService scmRepoVisibilityService;

  private final SourceControlSshService sourceControlSshService;

  // Only meant to be used in functional tests
  public static boolean disableSshForFunctionalTest = false;

  @Inject
  public ApiCompositeSourceControlConfigValidatorService(
      SourceControlUtils sourceControlUtils,
      GitClientFactory gitClientFactory,
      GitApiFactory gitApiFactory,
      ScmRepoVisibilityService scmRepoVisibilityService,
      SourceControlSshService sourceControlSshService)
  {
    this.sourceControlUtils = sourceControlUtils;
    this.gitClientFactory = gitClientFactory;
    this.gitApiFactory = gitApiFactory;
    this.scmRepoVisibilityService = scmRepoVisibilityService;
    this.sourceControlSshService = sourceControlSshService;
  }

  @Authorize(permission = Permission.READ)
  public ConfigurationValidationResult validateSourceControlConfig(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId)
  {
    ConfigurationValidationResult result = new ConfigurationValidationResult();
    GitRepositoryInfo gitInfo = sourceControlUtils.getGitRepositoryInfoForApplication(applicationId);
    if (gitInfo == null) {
      result.setConfigurationComplete(new ValidationResult(false, "Some required values are missing or unsaved"));
      return result;
    }
    result.setConfigurationComplete(new ValidationResult(true));

    try {
      boolean isPrivateRepository = scmRepoVisibilityService.isPrivateRepository(gitInfo);
      boolean isInternalRepository = scmRepoVisibilityService.isInternalRepository(gitInfo);
      if (!isPrivateRepository && !isInternalRepository) {
        result.setRepoPrivate(new ValidationResult(false,
            "Repository must be private or internal to enable all SCM features. " +
                "Support for public repositories is limited."));
      }
      else {
        result.setRepoPrivate(new ValidationResult(true));
      }

      boolean isScmAllowedOnPublicRepositories = scmRepoVisibilityService.isScmAllowedOnPublicRepositories();
      if (!isScmAllowedOnPublicRepositories || isPrivateRepository || isInternalRepository) {
        result.setRepoPublic(new ValidationResult(false));
      }
      else {
        result.setRepoPublic(new ValidationResult(true));
      }
    }
    catch (Exception e) {
      // Don't propagate the exception message because it may contain server details that can help an attacker mount an
      // attack. See https://sonatype.atlassian.net/browse/CLM-29901.
      log.debug("Unable to determine if repository is private for app ID {}: {}", applicationId, e.getMessage(), e);
      result.setRepoPrivate(new ValidationResult(false, "Unable to determine if repository is private."));
      result.setRepoPublic(new ValidationResult(false, "Unable to determine if repository is public."));
    }

    try {
      GitApiClient gitApiClient = gitClientFactory.createApiClient(gitInfo);
      result.setTokenPermissions(gitApiClient.validateTokenPermissions());
    }
    catch (InvalidRepositoryUrlException e) {
      log.debug("Invalid repository URL for app ID {}: {}", applicationId, e.getMessage(), e);
      result.setTokenPermissions(new ValidationResult(false,
          "Unable to validate the repository URL. Please verify the URL and credentials are correct."));
    }
    catch (HttpResponseException e) {
      log.debug("HTTP error testing permissions for app ID {}: {} {}", applicationId, e.getStatusCode(),
          e.getMessage(), e);
      switch (e.getStatusCode()) {
        case HttpStatus.SC_UNAUTHORIZED:
          result.setTokenPermissions(new ValidationResult(false,
              "Authentication failed. Please verify your credentials."));
          break;
        case HttpStatus.SC_FORBIDDEN:
          // The SCM client now ships an auth-strategy-aware reason phrase (PAT vs. GitHub
          // App). Surface it directly so a GitHub App user is pointed at App installation
          // and permissions instead of PAT "scopes" terminology. Use getReasonPhrase()
          // rather than getMessage() so we don't leak "status code: NNN, reason phrase: ".
          //
          // Security contract: this call site assumes the SCM client constructs the reason
          // phrase itself as a curated, user-safe string and does NOT echo a verbatim
          // upstream provider response (which could include server-side details). The
          // surrounding catch blocks deliberately suppress exception messages for that
          // reason; this branch is only safe so long as the SCM client upholds its end of
          // the contract. If a future SCM client change ever passes through a raw upstream
          // body, fall back to the hard-coded message instead.
          String reason = e.getReasonPhrase();
          result.setTokenPermissions(new ValidationResult(false,
              (reason != null && !reason.isBlank())
                  ? reason
                  : "Insufficient permissions. Please verify the token has the required scopes."));
          break;
        default:
          result.setTokenPermissions(new ValidationResult(false,
              "Unable to validate permissions due to an unexpected server response."));
          break;
      }
    }
    catch (Exception e) {
      // Don't propagate the exception message because it may contain server details that can help an attacker mount an
      // attack. See https://sonatype.atlassian.net/browse/CLM-29901.
      log.debug("Unable to test permissions for app ID {}: {}", applicationId, e.getMessage(), e);
      result.setTokenPermissions(new ValidationResult(false, "Unable to test permissions."));
    }

    if (Boolean.FALSE.equals(disableSshForFunctionalTest)) {
      validateSshConfiguration(applicationId, result, gitInfo);
    }

    return result;
  }

  private void validateSshConfiguration(
      String applicationId,
      ConfigurationValidationResult result,
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
      // Don't propagate the exception message because it may contain server details that can help an attacker mount an
      // attack. See https://sonatype.atlassian.net/browse/CLM-29901.
      log.debug("Source control SSH config validation for app ID {} failed: {}", applicationId, e.getMessage(), e);
      result.setSshConfiguration(new ValidationResult(false, "Unable to clone a repository using SSH, check that "
          + "your SSH keys are configured properly and available to IQ."));
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
    catch (Exception e) {
      // Don't propagate the exception message because it may contain server details that can help an attacker mount an
      // attack. See https://sonatype.atlassian.net/browse/CLM-29901.
      log.debug("Source control SSH config validation for app ID {} failed: {}", applicationId, e.getMessage(), e);
      result.setSshConfiguration(new ValidationResult(false, "Unable to clone a repository using SSH, check that " +
          "your SSH keys are configured properly and available to IQ."));
    }
  }
}
