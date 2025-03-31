/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.hds.ComponentRemediationService;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.component.DependencyType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.security.PermissionService;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.git.utils.VersionRemediationTitleGenerator;
import com.sonatype.nexus.iq.manager.PullRequestExecutor;

import jakarta.inject.Inject;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManualPullRequestService
{
  protected static final Logger log = LoggerFactory.getLogger(ManualPullRequestService.class);

  private final SourceControlEventDAO sourceControlEventDAO;

  private final SourceControlDAO sourceControlDAO;

  private final StageTypeService stageTypeService;

  private final PermissionService permissionService;

  private final PullRequestExecutor pullRequestExecutor;

  private final ManualPullRequestFeatureCheck manualPullRequestFeatureCheck;

  private final PasswordHandler passwordHandler;

  private final RemediationBranchNamePrefixGenerator remediationBranchNamePrefixGenerator =
      new RemediationBranchNamePrefixGenerator();

  private final VersionRemediationTitleGenerator versionRemediationTitleGenerator =
      new VersionRemediationTitleGenerator();

  @Inject
  public ManualPullRequestService(
      SourceControlEventDAO sourceControlEventDAO,
      SourceControlDAO sourceControlDAO,
      StageTypeService stageTypeService, PermissionService permissionService,
      PullRequestExecutor pullRequestExecutor,
      ManualPullRequestFeatureCheck manualPullRequestFeatureCheck,
      PasswordHandler passwordHandler)
  {
    this.sourceControlEventDAO = sourceControlEventDAO;
    this.sourceControlDAO = sourceControlDAO;
    this.stageTypeService = stageTypeService;
    this.permissionService = permissionService;
    this.pullRequestExecutor = pullRequestExecutor;
    this.manualPullRequestFeatureCheck = manualPullRequestFeatureCheck;
    this.passwordHandler = passwordHandler;
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
    Set<Permission> grantedPermissions = permissionService.validatePermission(
        SecurityUtils.getSubject(),
        owner.getType(),
        owner.getId(),
        Collections.singleton(Permission.CREATE_PULL_REQUESTS));

    if (!grantedPermissions.contains(Permission.CREATE_PULL_REQUESTS)) {
      return Optional.of(ManualPullRequestImpossibilityReason.INSUFFICIENT_PERMISSIONS);
    }

    Optional<String> suggestedVersion = getSuggestedVersion(remediationDto);
    if (suggestedVersion.isEmpty()) {
      return Optional.of(ManualPullRequestImpossibilityReason.NO_REMEDIATION_VERSION_AVAILABLE);
    }

    if (!isSupportedStage(stageId)) {
      return Optional.of(ManualPullRequestImpossibilityReason.UNSUPPORTED_STAGE);
    }
    if (!isDirectDependency(dependencyType)) {
      return Optional.of(ManualPullRequestImpossibilityReason.UNSUPPORTED_DEPENDENCY_TYPE);
    }
    if (!isFormatSupportedForPullRequest(componentIdentifier)) {
      return Optional.of(ManualPullRequestImpossibilityReason.UNSUPPORTED_FORMAT);
    }

    String branchName = generateBranchName(owner, componentIdentifier, suggestedVersion.get());
    if (doesRemediationEventExist(owner, branchName)) {
      return Optional.of(ManualPullRequestImpossibilityReason.REMEDIATION_EVENT_EXISTS);
    }

    SourceControl sourceControl =
        sourceControlDAO.buildCompositeSourceControlInApplication(owner.getId());

    GitRepositoryInfo
        gitRepositoryInfo = SourceControlUtils.getGitRepositoryInfoForApplicationStatic(sourceControl, owner.getId());
    if (gitRepositoryInfo != null) {
      Optional<String> decryptedToken = decryptToken(sourceControl.getToken());
      if (decryptedToken.isPresent()) {
        gitRepositoryInfo.token = decryptedToken.get();
      }
      else {
        return Optional.of(ManualPullRequestImpossibilityReason.SCM_NOT_CONFIGURED);
      }
    }
    return manualPullRequestFeatureCheck.isManualPullRequestFeatureSupported(gitRepositoryInfo);
  }

  private Optional<String> getSuggestedVersion(ApiComponentRemediationValueDTO remediationDto) {
    if (remediationDto == null ||
        (remediationDto.versionChanges.isEmpty() && remediationDto.suggestedVersionChange == null)) {
      return Optional.empty();
    }

    if (remediationDto.suggestedVersionChange != null) {
      return extractVersion(remediationDto.suggestedVersionChange.getData());
    }

    return remediationDto.versionChanges.stream()
        .sorted(Comparator.comparingInt(v -> ComponentRemediationService.PREFERABLE_TYPE_ORDER.indexOf(v.getType())))
        .flatMap(versionChange -> extractVersion(versionChange.getData()).stream())
        .findFirst();
  }

  private Optional<String> extractVersion(ApiComponentChangeActionDTO changeAction) {
    return Optional.ofNullable(changeAction)
        .map(ApiComponentChangeActionDTO::getComponent)
        .map(component -> component.componentIdentifier)
        .map(ApiComponentIdentifierDTOV2::getCoordinates)
        .map(coordinates -> coordinates.get(ComponentIdentifier.VERSION));
  }

  private String generateBranchName(Owner owner, ComponentIdentifier componentIdentifier, String suggestedVersion) {
    String branchPrefix = remediationBranchNamePrefixGenerator.generatePrefixForApplication(owner.getId());
    return versionRemediationTitleGenerator.generateBranchNameForVersionRemediation(branchPrefix, componentIdentifier,
        suggestedVersion);
  }

  private boolean isSupportedStage(String stageId) {
    return !Stage.ID_DEVELOP.equals(stageId) &&
        stageTypeService.getLicensedStageTypes(StageTypeService.LIFECYCLE_CONTEXT).stream()
            .anyMatch(stageType -> stageType.getId().equals(stageId));
  }

  private boolean isDirectDependency(DependencyType dependencyType) {
    return DependencyType.DIRECT.equals(dependencyType);
  }

  private boolean isFormatSupportedForPullRequest(ComponentIdentifier componentIdentifier) {
    return componentIdentifier.getFormat() != null &&
        pullRequestExecutor.isSupportedFormat(componentIdentifier.getFormat());
  }

  private boolean doesRemediationEventExist(Owner owner, String branchName) {
    return sourceControlEventDAO.hasRemediationEventForBranch(owner.getId(), branchName);
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
