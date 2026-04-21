/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiCompositeSourceControlDTO;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiCompositeValueDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.git.IqForScmLicenseChecker;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.CipherFactory;
import com.sonatype.insight.brain.security.EncryptionKeyStore;
import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.sourcecontrol.SourceControl.FAKE_SECRET_KEY;

@Named
@Singleton
public class ApiCompositeSourceControlService
{
  private static final Logger log = LoggerFactory.getLogger(ApiCompositeSourceControlService.class);

  private final SourceControlDAO sourceControlDAO;

  private final GitHubAppDAO gitHubAppDAO;

  private final ApplicationDAO applicationDAO;

  private final IqForScmLicenseChecker licenseChecker;

  private final OrganizationDAO organizationDAO;

  private final OwnerDAO ownerDAO;

  private final PlexusCipher plexusCipher;

  private final EncryptionKeyStore encryptionKeyStore;

  @Inject
  public ApiCompositeSourceControlService(
      final SourceControlDAO sourceControlDAO,
      final GitHubAppDAO gitHubAppDAO,
      final ApplicationDAO applicationDAO,
      final IqForScmLicenseChecker licenseChecker,
      final OrganizationDAO organizationDAO,
      final OwnerDAO ownerDAO,
      final EncryptionKeyStore encryptionKeyStore)
  {
    this.sourceControlDAO = sourceControlDAO;
    this.gitHubAppDAO = gitHubAppDAO;
    this.applicationDAO = applicationDAO;
    this.licenseChecker = licenseChecker;
    this.organizationDAO = organizationDAO;
    this.ownerDAO = ownerDAO;
    this.plexusCipher = CipherFactory.createCipher();
    this.encryptionKeyStore = encryptionKeyStore;
  }

  @Authorize(permission = Permission.READ)
  public ApiCompositeSourceControlDTO getCompositeSourceControlByOwner(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId)
  {
    return getCompositeSourceControlByOwner(ownerType, ownerId, true);
  }

  /**
   * IQ-internal ONLY helper function to return the composite SC DTO with the
   * token fully decrypted
   */
  public ApiCompositeSourceControlDTO getCompositeSourceControlByOwnerDecrypted(
      final OwnerType ownerType,
      final String ownerId)
  {
    return getCompositeSourceControlByOwner(ownerType, ownerId, false);
  }

  /**
   * Retrieves the composite source control, given an owner (org or app)
   *
   * @param ownerType type of owner (org or app)
   * @param ownerId internal ID of the owner
   * @param obscureToken true if the token should be obscured, ie: if it is returned to the frontend
   *          or anywhere outside of IQ
   * @return the populated DTO
   */
  @Authorize(permission = Permission.READ)
  private ApiCompositeSourceControlDTO getCompositeSourceControlByOwner(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId,
      final boolean obscureToken)
  {
    checkLicense();

    List<String> hierarchy;
    if (ownerType.equals(OwnerType.APPLICATION)) {
      Application application = applicationDAO.getByIdNotNull(ownerId);
      hierarchy = ownerDAO.getOwnerIds(application.getId());
    }
    else {
      Organization organization = organizationDAO.getByIdNotNull(ownerId);
      hierarchy = ownerDAO.getOwnerIds(organization.getId());
    }
    return getCompositeSourceControlFromHierarchyIds(
        ownerType,
        ownerId,
        hierarchy.subList(1, hierarchy.size()),
        obscureToken);
  }

  private ApiCompositeSourceControlDTO getCompositeSourceControlFromHierarchyIds(
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId,
      final List<String> ancestorsId,
      final boolean obscureToken)
  {
    ApiCompositeSourceControlDTO dto = new ApiCompositeSourceControlDTO();
    dto.ownerId = ownerId;

    Optional<SourceControl> sourceControl = Optional.ofNullable(sourceControlDAO.getByOwnerId(ownerId));
    sourceControl.ifPresent(sc -> {
      dto.id = sc.getId();
      setTokenValueForReturn(sc, obscureToken);
    });

    List<String> ancestorsNameHierarchy = new ArrayList<>(ancestorsId.size());
    Map<String, SourceControl> ancestorsSourceControlMap = new HashMap<>();
    SourceControl defaultSourceControl = new SourceControl.Builder().build();

    Map<String, GitHubApp> ancestorsGitHubAppsByOwnerId = ancestorsId.isEmpty()
        ? Map.of()
        : gitHubAppDAO.getByOwnerIds(ancestorsId);

    log.debug("getCompositeSourceControl - ownerId: {}, ancestorsId: {}, ancestorsGitHubAppsByOwnerId.size: {}",
        ownerId, ancestorsId, ancestorsGitHubAppsByOwnerId.size());

    Map<String, GitHubApp> ancestorsGitHubAppMap = new HashMap<>();

    if (!ancestorsId.isEmpty()) {
      for (int i = 0; i < ancestorsId.size(); i++) {
        Optional<SourceControl> ancestorSourceControl;
        String ancestorName;
        String ancestorId = ancestorsId.get(i);
        ancestorSourceControl = Optional.ofNullable(sourceControlDAO.getByOwnerId(ancestorId));
        ancestorSourceControl.ifPresent(sc -> setTokenValueForReturn(sc, obscureToken));
        ancestorName = organizationDAO.getByIdNotNull(ancestorId).getName();

        GitHubApp ancestorGitHubApp = ancestorsGitHubAppsByOwnerId.get(ancestorId);
        log.debug("Processing ancestor - ancestorId: {}, ancestorName: {}, has GitHubApp: {}",
            ancestorId, ancestorName, ancestorGitHubApp != null);
        if (ancestorGitHubApp != null) {
          ancestorsGitHubAppMap.put(ancestorName, ancestorGitHubApp);
        }

        ancestorsNameHierarchy.add(i, ancestorName);
        ancestorsSourceControlMap.put(ancestorName, ancestorSourceControl.orElse(defaultSourceControl));
      }
    }

    log.debug("Final ancestorsGitHubAppMap - keys: {}", ancestorsGitHubAppMap.keySet());

    collateCompositeSourceControl(dto, sourceControl.orElse(defaultSourceControl), ancestorsNameHierarchy,
        ancestorsSourceControlMap, ancestorsGitHubAppMap, ownerType);
    return dto;
  }

  private void collateCompositeSourceControl(
      final ApiCompositeSourceControlDTO dto,
      final SourceControl sourceControl,
      final List<String> ancestorsNameHierarchy,
      final Map<String, SourceControl> ancestorsSourceControl,
      final Map<String, GitHubApp> ancestorsGitHubApp,
      final OwnerType ownerType)
  {

    // If parent and grandParent are set, this means it is an application.
    if (ownerType.equals(OwnerType.APPLICATION)) {
      dto.repositoryUrl = sourceControl.getRepositoryUrl();
    }

    dto.provider = collateCompositeDTO(
        sourceControl,
        ancestorsNameHierarchy,
        ancestorsSourceControl,
        sc -> sc.getProvider() == null ? null : sc.getProvider().toString());

    dto.username = collateCompositeDTO(
        sourceControl,
        ancestorsNameHierarchy,
        ancestorsSourceControl,
        SourceControl::getUsername);

    dto.token = collateCompositeDTO(
        sourceControl,
        ancestorsNameHierarchy,
        ancestorsSourceControl,
        SourceControl::getToken);

    dto.baseBranch = collateCompositeDTO(
        sourceControl,
        ancestorsNameHierarchy,
        ancestorsSourceControl,
        SourceControl::getBaseBranch);

    dto.remediationPullRequestsEnabled = collateCompositeDTO(
        sourceControl,
        ancestorsNameHierarchy,
        ancestorsSourceControl,
        SourceControl::getRemediationPullRequestsEnabled);

    dto.statusChecksEnabled = collateCompositeDTO(
        sourceControl,
        ancestorsNameHierarchy,
        ancestorsSourceControl,
        SourceControl::getStatusChecksEnabled);

    dto.pullRequestCommentingEnabled = collateCompositeDTO(
        sourceControl,
        ancestorsNameHierarchy,
        ancestorsSourceControl,
        SourceControl::getPullRequestCommentingEnabled);

    dto.sourceControlEvaluationsEnabled = collateCompositeDTO(
        sourceControl,
        ancestorsNameHierarchy,
        ancestorsSourceControl,
        SourceControl::getSourceControlEvaluationsEnabled);

    dto.sourceControlScanTarget = collateCompositeDTO(
        sourceControl,
        ancestorsNameHierarchy,
        ancestorsSourceControl,
        SourceControl::getSourceControlScanTarget);

    dto.sshEnabled = collateCompositeDTO(
        sourceControl,
        ancestorsNameHierarchy,
        ancestorsSourceControl,
        SourceControl::getSshEnabled);

    dto.commitStatusEnabled = collateCompositeDTO(
        sourceControl,
        ancestorsNameHierarchy,
        ancestorsSourceControl,
        SourceControl::getCommitStatusEnabled);

    dto.manualPullRequestsEnabled = collateCompositeDTO(
        sourceControl,
        ancestorsNameHierarchy,
        ancestorsSourceControl,
        SourceControl::getManualPullRequestsEnabled);

    dto.innerSourceAutomatedUpdatesEnabled = collateCompositeDTO(
        sourceControl,
        ancestorsNameHierarchy,
        ancestorsSourceControl,
        SourceControl::getInnerSourceAutomatedUpdatesEnabled);

    dto.nonGoldenPullRequestsEnabled = collateCompositeDTO(
        sourceControl,
        ancestorsNameHierarchy,
        ancestorsSourceControl,
        SourceControl::getNonGoldenPullRequestsEnabled);

    dto.closePrOnFailedChecksEnabled = collateCompositeDTO(
        sourceControl,
        ancestorsNameHierarchy,
        ancestorsSourceControl,
        SourceControl::getClosePrOnFailedChecksEnabled);

    dto.closePrAfterDaysOpenEnabled = collateCompositeDTO(
        sourceControl,
        ancestorsNameHierarchy,
        ancestorsSourceControl,
        SourceControl::getClosePrAfterDaysOpenEnabled);

    dto.closePrAfterDays = collateCompositeDTO(
        sourceControl,
        ancestorsNameHierarchy,
        ancestorsSourceControl,
        SourceControl::getClosePrAfterDays);

    dto.authenticationType = collateCompositeDTO(
        sourceControl,
        ancestorsNameHierarchy,
        ancestorsSourceControl,
        sc -> sc.getAuthenticationType() == null ? null : sc.getAuthenticationType().name());

    dto.githubApps = collateGitHubAppsList(dto.ownerId, ancestorsNameHierarchy, ancestorsGitHubApp);
  }

  private List<ApiCompositeValueDTO<ApiCompositeSourceControlDTO.GitHubAppInfo>> collateGitHubAppsList(
      final String ownerId,
      final List<String> ancestorsNameHierarchy,
      final Map<String, GitHubApp> ancestorsGitHubApp)
  {
    List<GitHubApp> allGitHubApps = gitHubAppDAO.getAllByOwnerId(ownerId);

    log.debug(
        "collateGitHubAppsList - ownerId: {}, allGitHubApps.size: {}, ancestorsNameHierarchy: {}, ancestorsGitHubApp.size: {}",
        ownerId, allGitHubApps.size(), ancestorsNameHierarchy, ancestorsGitHubApp.size());

    if (!allGitHubApps.isEmpty()) {
      return allGitHubApps.stream()
          .map(
              gitHubApp -> collateGitHubAppCompositeDTO(ownerId, ancestorsNameHierarchy, ancestorsGitHubApp, gitHubApp))
          .collect(Collectors.toList());
    }

    for (String ancestorName : ancestorsNameHierarchy) {
      GitHubApp ancestorGitHubApp = ancestorsGitHubApp.get(ancestorName);
      log.debug("Checking ancestor: {}, has GitHubApp: {}", ancestorName, ancestorGitHubApp != null);
      if (ancestorGitHubApp != null) {
        ApiCompositeValueDTO<ApiCompositeSourceControlDTO.GitHubAppInfo> inheritedDto =
            new ApiCompositeValueDTO<>();
        inheritedDto.value = null;
        inheritedDto.parentName = ancestorName;
        inheritedDto.parentValue = buildGitHubAppInfo(ancestorGitHubApp);
        log.debug("Returning inherited GitHub App from ancestor: {}", ancestorName);
        return List.of(inheritedDto);
      }
    }

    log.debug("No GitHub Apps found at any level for ownerId: {}", ownerId);
    return List.of();
  }

  private ApiCompositeValueDTO<ApiCompositeSourceControlDTO.GitHubAppInfo> collateGitHubAppCompositeDTO(
      final String ownerId,
      final List<String> ancestorsNameHierarchy,
      final Map<String, GitHubApp> ancestorsGitHubApp,
      final GitHubApp ownerGitHubApp)
  {
    ApiCompositeValueDTO<ApiCompositeSourceControlDTO.GitHubAppInfo> dto = new ApiCompositeValueDTO<>();

    if (ownerGitHubApp != null) {
      dto.value = buildGitHubAppInfo(ownerGitHubApp);
    }

    for (String ancestorName : ancestorsNameHierarchy) {
      GitHubApp ancestorGitHubApp = ancestorsGitHubApp.get(ancestorName);
      if (ancestorGitHubApp != null) {
        dto.parentName = ancestorName;
        dto.parentValue = buildGitHubAppInfo(ancestorGitHubApp);
        break;
      }
    }

    return dto;
  }

  private ApiCompositeSourceControlDTO.GitHubAppInfo buildGitHubAppInfo(final GitHubApp gitHubApp) {
    ApiCompositeSourceControlDTO.GitHubAppInfo appInfo = new ApiCompositeSourceControlDTO.GitHubAppInfo();
    appInfo.id = gitHubApp.getId();
    appInfo.name = gitHubApp.getSlug();
    appInfo.installationId = gitHubApp.getInstallationId();
    appInfo.accountName = gitHubApp.getGithubOrganizationName();
    appInfo.isActive = gitHubApp.isActive();

    if (gitHubApp.getLastUpdatedAt() != null) {
      appInfo.configurationDate = gitHubApp.getLastUpdatedAt().toInstant().toString();
    }

    return appInfo;
  }

  private <T> ApiCompositeValueDTO<T> collateCompositeDTO(
      SourceControl ownerSourceControl,
      List<String> ancestorsNameHierarchy,
      Map<String, SourceControl> ancestorsSourceControl,
      Function<SourceControl, T> getValueFunction)
  {
    ApiCompositeValueDTO<T> dto = new ApiCompositeValueDTO<>();
    dto.value = getValueFunction.apply(ownerSourceControl);
    for (String ancestorName : ancestorsNameHierarchy) {
      SourceControl ancestorSourceControl = ancestorsSourceControl.get(ancestorName);
      T ancestorValue = getValueFunction.apply(ancestorSourceControl);
      if (null != ancestorValue) {
        dto.parentName = ancestorName;
        dto.parentValue = ancestorValue;
        break;
      }
    }
    return dto;
  }

  private void setTokenValueForReturn(final SourceControl sourceControl, boolean obscureToken) {
    sourceControl.setToken(Strings.isNullOrEmpty(sourceControl.getToken())
        ? null
        : obscureToken ? FAKE_SECRET_KEY : decrypt(sourceControl.getToken()));
  }

  private String decrypt(String value) {
    if (value == null) {
      return null;
    }
    synchronized (plexusCipher) {
      try {
        return plexusCipher.decryptDecorated(value, encryptionKeyStore.getKey());
      }
      catch (PlexusCipherException e) {
        log.error("Unable to decrypt SourceControl token", e);
        throw new IllegalStateException(e);
      }
    }
  }

  private void checkLicense() {
    if (!licenseChecker.isIqForScmSupported()) {
      log.debug("License does not support source control notification or automation features");
      throw new InvalidLicenseException();
    }
  }
}
