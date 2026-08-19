/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.development.prioritization;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.development.prioritization.DevelopmentPrioritizationComponentInfoDAO;
import com.sonatype.insight.brain.dataaccess.development.prioritization.DevelopmentPrioritizationDAO;
import com.sonatype.insight.brain.development.prioritization.dto.PrioritizationRemediationVersionDTO;
import com.sonatype.insight.brain.hds.ComponentDetailsDTO;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
import com.sonatype.insight.brain.hds.ComponentDetailsLoaderFactory;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.hds.ComponentRemediationService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.prioritization.DevelopmentPrioritization;
import com.sonatype.insight.brain.model.prioritization.DevelopmentPrioritizationComponentInfo;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;

import static com.sonatype.insight.brain.git.PullRequestCommentingRemediationService.VERSION_KEY;

@Named
@Singleton
public class DevelopmentPrioritizationRemediationService
{
  private final ApplicationDAO applicationDAO;

  private final DevelopmentPrioritizationComponentInfoDAO developmentPrioritizationComponentInfoDAO;

  private final DevelopmentPrioritizationDAO developmentPrioritizationDAO;

  private final ComponentDetailsLoaderFactory componentDetailsLoaderFactory;

  private final ComponentInfoService componentInfoService;

  private final ComponentRemediationService componentRemediationService;

  @Inject
  public DevelopmentPrioritizationRemediationService(
      final ApplicationDAO applicationDAO,
      final ComponentDetailsLoaderFactory componentDetailsLoaderFactory,
      final ComponentInfoService componentInfoService,
      final ComponentRemediationService componentRemediationService,
      final DevelopmentPrioritizationComponentInfoDAO developmentPrioritizationComponentInfoDAO,
      final DevelopmentPrioritizationDAO developmentPrioritizationDAO)
  {
    this.applicationDAO = applicationDAO;
    this.componentDetailsLoaderFactory = componentDetailsLoaderFactory;
    this.componentInfoService = componentInfoService;
    this.componentRemediationService = componentRemediationService;
    this.developmentPrioritizationComponentInfoDAO = developmentPrioritizationComponentInfoDAO;
    this.developmentPrioritizationDAO = developmentPrioritizationDAO;
  }

  public void fetchAndPersistRemediationRecommendations(
      List<ComponentIdentifier> componentIdentifiers,
      String scanId,
      String appId,
      Stage stage)
  {
    Map<ComponentIdentifier, PrioritizationRemediationVersionDTO> remediationVersions =
        getRemediationVersions(componentIdentifiers, appId, stage.getStageName(), scanId);
    persistRemediationRecommendations(remediationVersions, scanId);
  }

  void persistRemediationRecommendations(
      Map<ComponentIdentifier, PrioritizationRemediationVersionDTO> remediationVersions,
      String scanId)
  {
    DevelopmentPrioritization scanPrioritization = new DevelopmentPrioritization(scanId);
    try (TransactionContext tx = developmentPrioritizationDAO.createTransactionContext()) {
      tx.begin();
      developmentPrioritizationDAO.insert(tx, scanPrioritization);
      String parentId = scanPrioritization.getId();
      List<DevelopmentPrioritizationComponentInfo> componentInfoList = remediationVersions.entrySet()
          .stream()
          .map(entry -> convertRemediationVersionsEntry(entry, parentId, scanId))
          .collect(Collectors.toList());
      developmentPrioritizationComponentInfoDAO.insertBatch(tx, componentInfoList);
      tx.commit();
    }
  }

  private static DevelopmentPrioritizationComponentInfo convertRemediationVersionsEntry(
      Entry<ComponentIdentifier, PrioritizationRemediationVersionDTO> remediationVersionEntry,
      final String parentId,
      final String scanId)
  {
    ComponentIdentifier componentIdentifier = remediationVersionEntry.getKey();
    PrioritizationRemediationVersionDTO remediationVersion = remediationVersionEntry.getValue();
    return new DevelopmentPrioritizationComponentInfo(parentId, scanId, componentIdentifier.toSyntheticHash(),
        remediationVersion.getRemediationType(), remediationVersion.getVersion());
  }

  /**
   * Get a prioritized list of remediation versions for the specified component identifier.
   *
   * This is distinct from the previous method because it explicitly disables the advanced strategy.
   * A story/bug report has been created to explore the purpose of this change, because we might
   * be able to unify the methods if this isn't so.
   *
   * https://sonatype.atlassian.net/browse/SDEV-1597
   */
  public Map<ComponentIdentifier, PrioritizationRemediationVersionDTO> getRemediationVersions(
      List<ComponentIdentifier> componentIdentifiers,
      String appInternalId,
      String stage,
      String scanId)
  {
    if (componentIdentifiers.isEmpty()) {
      return Collections.emptyMap();
    }
    Application app = applicationDAO.getByIdNotNull(appInternalId);
    ComponentDetailsLoader componentDetailsLoader = componentDetailsLoaderFactory.newInstance(app);

    Map<ComponentIdentifier, List<ComponentDetailsDTO>> componentDetailsForAllVersionsNoAuthBulk =
        componentInfoService.getComponentDetailsForAllVersionsNoAuthBulk(app,
            componentIdentifiers, stage, scanId, componentDetailsLoader, true);

    return componentIdentifiers.stream().map(componentIdentifier -> {
      ComponentIdentifier pkgIdentifier = componentIdentifier.createAlternativeVersion(null);
      List<ComponentDetailsDTO> componentDetailsDTOs = componentDetailsForAllVersionsNoAuthBulk.get(pkgIdentifier);

      // FIXME: Should the strategy actually be false? I suspect that it should not be and that we should
      // change this code to use getSuggestedRemediation instead that will use the advanced strategy
      // if applicable. https://sonatype.atlassian.net/browse/SDEV-1597
      ApiComponentRemediationValueDTO remediationValueDto =
          componentRemediationService.getSuggestedSelectedRemediation(componentIdentifier, componentDetailsDTOs,
              app, stage, componentDetailsLoader, false);

      if (remediationValueDto != null) {
        Optional<ApiVersionChangeOptionDTO> versionChangeDTO =
            getRecommendedVersionChange(remediationValueDto.versionChanges);

        final PrioritizationRemediationVersionDTO prioritizationRemediationVersionDTO =
            getPrioritizationRemediationVersionDTO(versionChangeDTO);

        return Maps.immutableEntry(componentIdentifier, prioritizationRemediationVersionDTO);
      }
      return Maps.immutableEntry(componentIdentifier, (PrioritizationRemediationVersionDTO) null);
    })
        .filter(entrySet -> Objects.nonNull(entrySet.getValue()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }

  private PrioritizationRemediationVersionDTO getPrioritizationRemediationVersionDTO(
      Optional<ApiVersionChangeOptionDTO> versionChangeDTO)
  {
    if (!versionChangeDTO.isPresent()) {
      return null;
    }

    ApiComponentIdentifierDTOV2 identifierDTOV2 = versionChangeDTO.get()
        .getData()
        .getComponent().componentIdentifier;

    ComponentIdentifier remediationComponentIdentifier =
        new ComponentIdentifier(identifierDTOV2.getFormat(), identifierDTOV2.getCoordinates());

    return new PrioritizationRemediationVersionDTO(remediationComponentIdentifier.getCoordinates().get(VERSION_KEY),
        versionChangeDTO.get().getType());
  }

  @VisibleForTesting
  Optional<ApiVersionChangeOptionDTO> getRecommendedVersionChange(
      List<ApiVersionChangeOptionDTO> versionChanges)
  {
    if (versionChanges.isEmpty()) {
      return Optional.empty();
    }

    return first(Arrays.asList(
        getVersionChangeOptional(versionChanges, ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES),
        getVersionChangeOptional(versionChanges, ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS),
        getVersionChangeOptional(versionChanges, ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES),
        getVersionChangeOptional(versionChanges, ApiVersionChangeOptionType.NEXT_NON_FAILING)));
  }

  private static Optional<ApiVersionChangeOptionDTO> getVersionChangeOptional(
      List<ApiVersionChangeOptionDTO> versionChanges,
      ApiVersionChangeOptionType versionChangeOptionType)
  {
    return versionChanges.stream().filter(vChange -> vChange.getType() == versionChangeOptionType).findFirst();
  }

  private static Optional<ApiVersionChangeOptionDTO> first(List<Optional<ApiVersionChangeOptionDTO>> optionals) {
    return optionals.stream()
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }
}
