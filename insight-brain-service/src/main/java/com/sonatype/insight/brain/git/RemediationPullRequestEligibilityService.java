/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to check eligibility for remediation pull requests and manual pull requests
 */
@Named
@Singleton
public class RemediationPullRequestEligibilityService
{
  private static final Logger log = LoggerFactory.getLogger(RemediationPullRequestEligibilityService.class);

  private final RemediationPullRequestFeatureCheck remediationPullRequestFeatureCheck;

  private final ManualPullRequestFeatureCheck manualPullRequestFeatureCheck;

  private final PullRequestRemediationService pullRequestRemediationService;

  private final StageTypeService stageTypeService;

  private final SourceControlUtils sourceControlUtils;

  private final SourceControlEventDAO sourceControlEventDAO;

  @Inject
  public RemediationPullRequestEligibilityService(
      final RemediationPullRequestFeatureCheck remediationPullRequestFeatureCheck,
      final ManualPullRequestFeatureCheck manualPullRequestFeatureCheck,
      final PullRequestRemediationService pullRequestRemediationService,
      final StageTypeService stageTypeService,
      final SourceControlUtils sourceControlUtils,
      final SourceControlEventDAO sourceControlEventDAO)
  {
    this.remediationPullRequestFeatureCheck = remediationPullRequestFeatureCheck;
    this.manualPullRequestFeatureCheck = manualPullRequestFeatureCheck;
    this.pullRequestRemediationService = pullRequestRemediationService;
    this.stageTypeService = stageTypeService;
    this.sourceControlUtils = sourceControlUtils;
    this.sourceControlEventDAO = sourceControlEventDAO;
  }

  public boolean isEligibleForAutoPullRequest(
      final Application app,
      final Stage stage,
      final ComponentIdentifier componentIdentifier,
      final boolean isInnerSourceComponent,
      final boolean isDirectDependency,
      final String scannedBranchName)
  {
    try {
      if (!isEligibleForPullRequest(app, stage, componentIdentifier, isDirectDependency)) {
        return false;
      }

      GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(app.getId());
      if (gitRepositoryInfo == null) {
        return false;
      }

      if (isScannedBranchNonDefault(scannedBranchName, gitRepositoryInfo.baseBranch)) {
        log.debug("PR not eligible for application '{}': scanned branch '{}' differs from default branch '{}'",
            app.getPublicId(), scannedBranchName, gitRepositoryInfo.baseBranch);
        return false;
      }

      return remediationPullRequestFeatureCheck.isPullRequestFeatureSupported(app, gitRepositoryInfo,
          isInnerSourceComponent);
    }
    catch (Exception e) {
      log.debug("Error checking eligibility for auto PR for application '{}' component '{}'",
          app.getPublicId(), componentIdentifier, e);
      throw e;
    }
  }

  public boolean isEligibleForManualPullRequest(
      final Application app,
      final Stage stage,
      final ComponentIdentifier componentIdentifier,
      final boolean isDirectDependency,
      final String scannedBranchName)
  {
    try {
      if (!isEligibleForPullRequest(app, stage, componentIdentifier, isDirectDependency)) {
        return false;
      }

      GitRepositoryInfo gitRepositoryInfo = sourceControlUtils.getGitRepositoryInfoForApplication(app.getId());
      if (gitRepositoryInfo == null) {
        return false;
      }

      if (isScannedBranchNonDefault(scannedBranchName, gitRepositoryInfo.baseBranch)) {
        log.debug("PR not eligible for application '{}': scanned branch '{}' differs from default branch '{}'",
            app.getPublicId(), scannedBranchName, gitRepositoryInfo.baseBranch);
        return false;
      }

      return manualPullRequestFeatureCheck.isManualPullRequestFeatureSupported(gitRepositoryInfo).isEmpty();
    }
    catch (Exception e) {
      log.debug("Error checking eligibility for manual PR for application '{}' component '{}'",
          app.getPublicId(), componentIdentifier, e);
      throw e;
    }
  }

  /**
   * Check if a remediation SourceControlEvent (automated or manual) exists for the given application id and branch name
   * in a new, pending, or completed state.
   */
  public boolean isRemediationWaitingOrDone(final String applicationId, final String branchName) {
    boolean exists = sourceControlEventDAO.hasWaitingOrCompleteRemediationEvent(applicationId, branchName);

    if (exists) {
      log.debug("{} branch already exists for application '{}'", branchName, applicationId);
    }
    return exists;
  }

  private boolean isFormatSupported(final String format) {
    boolean supported = pullRequestRemediationService.isFormatSupportedForPullRequestRemediation(format);
    if (!supported) {
      log.debug("Format '{}' is not supported for remediation", format);
    }
    return supported;
  }

  private boolean isStageSupported(final Stage stage) {
    if (Stage.ID_DEVELOP.equals(stage.getStageTypeId())) {
      return false;
    }

    return stageTypeService.getLicensedStageTypes(StageTypeService.LIFECYCLE_CONTEXT)
        .stream()
        .anyMatch(stageType -> stageType.getId().equals(stage.getStageTypeId()));
  }

  /**
   * Performs common eligibility checks for both auto and manual pull requests, excluding the branch existence check
   */
  private boolean isEligibleForPullRequest(
      final Application app,
      final Stage stage,
      final ComponentIdentifier componentIdentifier,
      final boolean isDirectDependency)
  {
    if (app == null || stage == null || componentIdentifier == null) {
      log.debug("One or more required parameters is null");
      return false;
    }

    if (!isStageSupported(stage)) {
      log.debug("Pull Request not supported for the stage '{}' for application '{}'",
          stage.getStageTypeId(), app.getPublicId());
      return false;
    }

    if (!isDirectDependency) {
      log.debug("Component '{}' is not a direct dependency.", componentIdentifier);
      return false;
    }

    return isFormatSupported(componentIdentifier.getFormat());
  }

  static boolean isScannedBranchNonDefault(final String scannedBranchName, final String baseBranch) {
    if (StringUtils.isBlank(scannedBranchName) || StringUtils.isBlank(baseBranch)) {
      return false;
    }
    String normalizedScanned = stripRefsHeadsPrefix(scannedBranchName);
    String normalizedDefault = stripRefsHeadsPrefix(baseBranch);
    return !normalizedScanned.equals(normalizedDefault);
  }

  private static String stripRefsHeadsPrefix(final String branchName) {
    if (branchName != null && branchName.startsWith("refs/heads/")) {
      return branchName.substring("refs/heads/".length());
    }
    return branchName != null ? branchName : "";
  }
}
