/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.git.utils.PullRequestBranchNameGenerator;
import com.sonatype.insight.brain.hds.ComponentRemediationService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.DependencyType;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.security.PermissionService;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.nexus.iq.manager.PullRequestExecutor;

import jakarta.inject.Inject;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManualPullRequestService
{
  protected static final Logger log = LoggerFactory.getLogger(ManualPullRequestService.class);

  private final SourceControlDAO sourceControlDAO;

  private final StageTypeService stageTypeService;

  private final PermissionService permissionService;

  private final PullRequestExecutor pullRequestExecutor;

  private final ManualPullRequestFeatureCheck manualPullRequestFeatureCheck;

  private final PasswordHandler passwordHandler;

  private final PullRequestBranchNameGenerator pullRequestBranchNameGenerator;

  private final ComponentRemediationService componentRemediationService;

  private final RemediationPullRequestEligibilityService remediationPullRequestEligibilityService;

  private final TenantUtil tenantUtil;

  private final GitHubAppDAO gitHubAppDAO;

  @Inject
  public ManualPullRequestService(
      SourceControlDAO sourceControlDAO,
      StageTypeService stageTypeService,
      PermissionService permissionService,
      PullRequestExecutor pullRequestExecutor,
      ManualPullRequestFeatureCheck manualPullRequestFeatureCheck,
      PasswordHandler passwordHandler,
      PullRequestBranchNameGenerator pullRequestBranchNameGenerator,
      ComponentRemediationService componentRemediationService,
      RemediationPullRequestEligibilityService remediationPullRequestEligibilityService,
      TenantUtil tenantUtil,
      GitHubAppDAO gitHubAppDAO)
  {
    this.sourceControlDAO = sourceControlDAO;
    this.stageTypeService = stageTypeService;
    this.permissionService = permissionService;
    this.pullRequestExecutor = pullRequestExecutor;
    this.manualPullRequestFeatureCheck = manualPullRequestFeatureCheck;
    this.passwordHandler = passwordHandler;
    this.pullRequestBranchNameGenerator = pullRequestBranchNameGenerator;
    this.componentRemediationService = componentRemediationService;
    this.remediationPullRequestEligibilityService = remediationPullRequestEligibilityService;
    this.tenantUtil = tenantUtil;
    this.gitHubAppDAO = gitHubAppDAO;
  }

  /**
   * Check if a manual pull request is possible for the given component within its context.
   *
   * @param componentIdentifier the component identifier
   * @param stageId the stage ID
   * @param dependencyType the dependency type
   * @param owner the owner
   * @param remediationDto the remediation DTO
   * @return the reason why a manual pull request is not possible, if any
   */
  public Optional<ManualPullRequestImpossibilityReason> isManualPullRequestPossible(
      final ComponentIdentifier componentIdentifier,
      final String stageId,
      final DependencyType dependencyType,
      final Owner owner,
      final ApiComponentRemediationValueDTO remediationDto)
  {
    if (tenantUtil.isMultiTenant()) {
      return Optional.of(ManualPullRequestImpossibilityReason.NOT_SUPPORTED_FOR_MTIQ);
    }

    if (owner.getType() != OwnerType.APPLICATION) {
      return Optional.of(ManualPullRequestImpossibilityReason.UNSUPPORTED_OWNER_TYPE);
    }

    Set<Permission> grantedPermissions = permissionService.validatePermission(
        SecurityUtils.getSubject(),
        owner.getType(),
        owner.getId(),
        Collections.singleton(Permission.CREATE_PULL_REQUESTS));

    if (!grantedPermissions.contains(Permission.CREATE_PULL_REQUESTS)) {
      return Optional.of(ManualPullRequestImpossibilityReason.INSUFFICIENT_PERMISSIONS);
    }

    if (remediationDto == null) {
      return Optional.of(ManualPullRequestImpossibilityReason.NO_REMEDIATION_VERSION_AVAILABLE);
    }

    if (!isFormatSupportedForPullRequest(componentIdentifier)) {
      return Optional.of(ManualPullRequestImpossibilityReason.UNSUPPORTED_FORMAT);
    }

    Optional<ApiVersionChangeOptionDTO> suggestedVersionChange =
        componentRemediationService.getApplicableVersionChangeFromAllType(remediationDto.suggestedVersionChange,
            remediationDto.versionChanges);

    String currentVersion = componentIdentifier.get(ComponentIdentifier.VERSION);

    if (currentVersion == null || currentVersion.isEmpty()) {
      return Optional.of(ManualPullRequestImpossibilityReason.NO_REMEDIATION_VERSION_AVAILABLE);
    }

    if (suggestedVersionChange.isEmpty()) {
      return Optional.of(ManualPullRequestImpossibilityReason.NO_REMEDIATION_VERSION_AVAILABLE);
    }

    String suggestedVersion = suggestedVersionChange.get().getData().getComponent().componentIdentifier.getCoordinates()
        .get(ComponentIdentifier.VERSION);

    ComparableVersion comparableCurrentVersion = new ComparableVersion(currentVersion);
    ComparableVersion comparableSuggestedVersion = new ComparableVersion(suggestedVersion);
    if (comparableCurrentVersion.compareTo(comparableSuggestedVersion) == 0) {
      return Optional.of(ManualPullRequestImpossibilityReason.NO_REMEDIATION_VERSION_AVAILABLE);
    }

    if (!isSupportedStage(stageId)) {
      return Optional.of(ManualPullRequestImpossibilityReason.UNSUPPORTED_STAGE);
    }
    if (!isDirectDependency(dependencyType)) {
      return Optional.of(ManualPullRequestImpossibilityReason.UNSUPPORTED_DEPENDENCY_TYPE);
    }

    Application application = (Application) owner;
    String branchName =
        pullRequestBranchNameGenerator.getBranchName(application, componentIdentifier, suggestedVersion);
    if (remediationPullRequestEligibilityService.isRemediationWaitingOrDone(application.getId(), branchName)) {
      return Optional.of(ManualPullRequestImpossibilityReason.REMEDIATION_EVENT_EXISTS);
    }

    SourceControl sourceControl =
        sourceControlDAO.buildCompositeSourceControlInApplication(owner.getId());

    GitRepositoryInfo gitRepositoryInfo =
        SourceControlUtils.getGitRepositoryInfoForApplicationStatic(sourceControl, owner.getId());

    if (gitRepositoryInfo != null) {
      if (SourceControl.AuthenticationType.GITHUB_APP.equals(gitRepositoryInfo.authenticationType)) {
        log.debug("GitHub App authentication detected for owner: {}", owner.getId());
        GitHubApp gitHubApp = gitHubAppDAO.getNearestGitHubApp(owner.getId());

        if (gitHubApp == null) {
          log.warn("No GitHub App found in hierarchy for owner: {}", owner.getId());
          return Optional.of(ManualPullRequestImpossibilityReason.SCM_NOT_CONFIGURED);
        }

        String ownerId = gitHubApp.getOwnerId();
        if (ownerId == null) {
          log.warn("GitHub App has null ownerId for owner: {}", owner.getId());
          return Optional.of(ManualPullRequestImpossibilityReason.SCM_NOT_CONFIGURED);
        }

        gitRepositoryInfo.authOwnerId = ownerId;
        log.debug("Using authOwnerId: {} for owner: {}", gitRepositoryInfo.authOwnerId, owner.getId());

        if (gitHubApp.getInstallationId() == null) {
          log.warn("GitHub App missing installationId at authOwnerId: {}", gitRepositoryInfo.authOwnerId);
          return Optional.of(ManualPullRequestImpossibilityReason.SCM_NOT_CONFIGURED);
        }

        log.debug("GitHub App found with installationId: {} at authOwnerId: {}",
            gitHubApp.getInstallationId(), gitRepositoryInfo.authOwnerId);
      }
      else {
        Optional<String> decryptedToken = decryptToken(sourceControl.getToken());
        if (decryptedToken.isPresent()) {
          gitRepositoryInfo.token = decryptedToken.get();
        }
        else {
          return Optional.of(ManualPullRequestImpossibilityReason.SCM_NOT_CONFIGURED);
        }
      }
    }
    return manualPullRequestFeatureCheck.isManualPullRequestFeatureSupported(gitRepositoryInfo);
  }

  private boolean isSupportedStage(String stageId) {
    return !Stage.ID_DEVELOP.equals(stageId) &&
        stageTypeService.getLicensedStageTypes(StageTypeService.LIFECYCLE_CONTEXT)
            .stream()
            .anyMatch(stageType -> stageType.getId().equals(stageId));
  }

  private boolean isDirectDependency(DependencyType dependencyType) {
    return DependencyType.DIRECT.equals(dependencyType);
  }

  private boolean isFormatSupportedForPullRequest(ComponentIdentifier componentIdentifier) {
    return componentIdentifier.getFormat() != null &&
        pullRequestExecutor.isSupportedFormat(componentIdentifier.getFormat());
  }

  private Optional<String> decryptToken(final String encryptedToken) {
    try {
      return Optional.ofNullable(passwordHandler.decryptPassword(encryptedToken));
    }
    catch (Exception e) {
      log.error("Unable to decrypt SourceControl token", e);
      return Optional.empty();
    }
  }
}
