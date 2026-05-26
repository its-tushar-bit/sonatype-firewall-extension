/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiDependencyTreeNodeDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.hds.ComponentDetailsDTO;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
import com.sonatype.insight.brain.hds.ComponentDetailsLoaderFactory;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.hds.ComponentRemediationService;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.lang3.StringUtils;

import static com.sonatype.insight.brain.telemetry.NonBreakingRecommendationTelemetryStats.SourceEndpoint.API_COMPONENT_REMEDIATION;

/**
 * @since 1.64
 */
@Named
public class ApiComponentRemediationService
{
  private final ComponentInfoService componentInfoService;

  private final ComponentRemediationService componentRemediationService;

  private final HdsClient hdsClient;

  private final ThirdPartyComponentDAO thirdPartyComponentDAO;

  private final ApplicationDAO applicationDAO;

  private final ComponentDetailsLoaderFactory componentDetailsLoaderFactory;

  private final IdUtils idUtils;

  private final ApiReportDataServiceV2 apiReportDataServiceV2;

  @Inject
  public ApiComponentRemediationService(
      ComponentInfoService componentInfoService,
      ComponentRemediationService componentRemediationService,
      HdsClient hdsClient,
      ThirdPartyComponentDAO thirdPartyComponentDAO,
      ApplicationDAO applicationDAO,
      ComponentDetailsLoaderFactory componentDetailsLoaderFactory,
      IdUtils idUtils,
      ApiReportDataServiceV2 apiReportDataServiceV2)
  {
    this.componentInfoService = componentInfoService;
    componentInfoService.setToolName("ci");
    this.componentRemediationService = componentRemediationService;
    this.hdsClient = hdsClient;
    this.thirdPartyComponentDAO = thirdPartyComponentDAO;
    this.applicationDAO = applicationDAO;
    this.componentDetailsLoaderFactory = componentDetailsLoaderFactory;
    this.idUtils = idUtils;
    this.apiReportDataServiceV2 = apiReportDataServiceV2;
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  public ApiComponentRemediationDTO getSuggestedRemediationForComponent(
      ApiComponentDTOV2 componentDTO,
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId,
      String stageId,
      final String identificationSource,
      final String scanId,
      final Boolean includeParentRemediation)
  {
    return getSuggestedRemediationForComponentNoAuthz(componentDTO, ownerType, ownerId, stageId, identificationSource,
        scanId, includeParentRemediation, false);
  }

  /**
   * Prefer {@link #getSuggestedRemediationForComponent}
   * Use only when doing operations that have already checked authorization or running in a task
   * which does not have session/user attached
   */
  public ApiComponentRemediationDTO getSuggestedRemediationForComponentNoAuthz(
      ApiComponentDTOV2 componentDTO,
      final OwnerType ownerType,
      final String ownerId,
      String stageId,
      final String identificationSource,
      final String scanId,
      final Boolean includeParentRemediation,
      final boolean stableVersionsOnly)
  {
    ApiDependencyTreeSearcher apiDependencyTreeSearcher = new ApiDependencyTreeSearcher();
    if (OwnerType.REPOSITORY.equals(ownerType)) {
      if (stageId == null) {
        stageId = ProxyStageType.ID;
      }
      else if (!ProxyStageType.ID.equals(stageId)) {
        throw new BadRequestException("Invalid stage ID for repositories: " + stageId + ".");
      }

      if (!StringUtils.isBlank(scanId)) {
        throw new BadRequestException("The scan ID is not allowed for repositories.");
      }
    }
    else if (stageId != null && StageTypes.getById(stageId) == null) {
      throw new BadRequestException("Invalid stage ID: " + stageId + ".");
    }

    boolean includeParentRem = includeParentRemediation != null && includeParentRemediation;

    boolean isThirdPartySource =
        IdentificationSource.isThirdPartyIdentificationSource(identificationSource);

    ComponentIdentifier componentIdentifier = validateRequest(componentDTO, isThirdPartySource);

    ComponentSummary componentSummary;

    if (scanId != null && isThirdPartySource) {
      componentSummary = thirdPartyComponentDAO.getComponentSummary(componentIdentifier, ownerId, scanId);
    }
    else {
      componentSummary = getComponentSummary(componentIdentifier);
    }

    // Do not allow an empty or invalid version at this time
    if (!componentSummary.isKnown()) {
      throw new BadRequestException("Invalid Component Identifier or packageUrl");
    }

    Owner owner = idUtils.getOwnerNotNull(ownerType, ownerId);
    // For performance, it's very important to use only one instance of ComponentDetailsLoader.
    // See https://sonatype.atlassian.net/browse/CLM-28129
    ComponentDetailsLoader componentDetailsLoader = componentDetailsLoaderFactory.newInstance(owner);

    List<ComponentDetailsDTO> dtos = new ArrayList<>();
    Map<ComponentIdentifier, List<ComponentDetailsDTO>> parentComponentsToVersionsMap = new HashMap<>();

    List<ComponentIdentifier> directParentComponentIdentifiers = Collections.emptyList();

    if (includeParentRem) {
      directParentComponentIdentifiers =
          getDirectParentComponentIdentifiers(apiDependencyTreeSearcher, componentDTO, ownerType, ownerId, scanId,
              componentIdentifier);
    }

    if (directParentComponentIdentifiers.isEmpty()) {
      dtos = componentInfoService.getComponentDetailsForAllVersionsNoAuth(owner, componentIdentifier, stageId,
          identificationSource, scanId, null, componentDetailsLoader, stableVersionsOnly).getLeft();
    }
    else {
      Map<ComponentIdentifier, List<ComponentDetailsDTO>> componentDetailsForAllVersions =
          componentInfoService.getComponentDetailsForAllVersionsNoAuthBulk(owner, directParentComponentIdentifiers,
              stageId, scanId, componentDetailsLoader, stableVersionsOnly);
      parentComponentsToVersionsMap =
          mapComponentsAllVersionsFromBulk(componentDetailsForAllVersions, directParentComponentIdentifiers);
    }

    ApiComponentRemediationValueDTO remediationValueDto;
    if (isThirdPartySource) {
      remediationValueDto = thirdPartyComponentDAO.getSuggestedRemmediation(owner.getId(), componentIdentifier, scanId);
    }
    else {
      if (parentComponentsToVersionsMap.isEmpty()) {
        remediationValueDto = componentRemediationService.getSuggestedRemediation(componentIdentifier, dtos, owner,
            stageId, componentDetailsLoader, API_COMPONENT_REMEDIATION);

        if (includeParentRem && apiDependencyTreeSearcher.isDirectNode()) {
          remediationValueDto.versionChanges.forEach(it -> it.setDirectDependency(true));
        }
      }
      else {
        remediationValueDto =
            componentRemediationService.getSuggestedRemediationForTransitive(parentComponentsToVersionsMap,
                componentDTO.componentIdentifier, owner, stageId, componentDetailsLoader);
      }
    }

    return remediationValueDto == null ? null : new ApiComponentRemediationDTO(remediationValueDto);
  }

  private ComponentIdentifier validateRequest(ApiComponentDTOV2 componentDTO, boolean isThirdParty) {
    if (componentDTO == null || (componentDTO.componentIdentifier == null && componentDTO.packageUrl == null)) {
      throw new BadRequestException("One of either componentIdentifier or packageUrl must be supplied.");
    }

    if (componentDTO.componentIdentifier != null) {
      return validateComponentIdentifier(componentDTO, isThirdParty);
    }
    else {
      return validatePackageUrl(componentDTO);
    }
  }

  private ComponentIdentifier validateComponentIdentifier(ApiComponentDTOV2 componentDTO, boolean isThirdParty) {
    if (componentDTO.componentIdentifier == null) {
      throw new BadRequestException("ComponentIdentifier must be supplied.");
    }
    try {
      ComponentIdentifier componentIdentifier = componentDTO.componentIdentifier.toComponentIdentifier();
      if (!isThirdParty) {
        // The complete identifier is not required to determine the suggested remediation for third party components
        componentIdentifier.ensureComplete();
      }
      return componentIdentifier;
    }
    catch (InvalidComponentIdentifierException e) {
      throw new BadRequestException(e.getMessage(), e);
    }
  }

  private ComponentIdentifier validatePackageUrl(ApiComponentDTOV2 componentDTO) {
    return new PackageUrlIdentifier(componentDTO.packageUrl).ensureCompleteIdentifier();
  }

  private ComponentSummary getComponentSummary(final ComponentIdentifier componentIdentifier) {
    Map<String, String> queryParams = Collections.singletonMap("componentIdentifier",
        ComponentIdentifierAdapter.toJson(componentIdentifier));
    return hdsClient.get(ComponentSummary.class, "rest/component/summary", queryParams);
  }

  /**
   * Using a list of component identifiers, and a map of package identifiers to a list of
   * details for versions of this package, produce a map from the identifier itself to the
   * details.
   *
   * Note that the pkgBasedMap only contains versions greater than or equal to the component
   * identifier in the provided componentIdentifiers list. We never look back.
   *
   * @param pkgBasedMap Map of component identifiers *without* versions to details lists
   * @param componentIdentifiers List of identifiers we are checking
   * @return A map from a versioned component identifier to details of versions greater or
   *         equal to the key's version.
   */
  public Map<ComponentIdentifier, List<ComponentDetailsDTO>> mapComponentsAllVersionsFromBulk(
      Map<ComponentIdentifier, List<ComponentDetailsDTO>> pkgBasedMap,
      List<ComponentIdentifier> componentIdentifiers)
  {
    Map<ComponentIdentifier, List<ComponentDetailsDTO>> resultMap = new HashMap<>();
    for (ComponentIdentifier identifier : componentIdentifiers) {
      ComponentIdentifier packageIdentifier = identifier.createAlternativeVersion(null);
      if (pkgBasedMap.containsKey(packageIdentifier)) {
        resultMap.computeIfAbsent(identifier, k -> new ArrayList<>()).addAll(pkgBasedMap.get(packageIdentifier));
      }
    }

    return resultMap;
  }

  private List<ComponentIdentifier> getDirectParentComponentIdentifiers(
      final ApiDependencyTreeSearcher apiDependencyTreeSearcher,
      final ApiComponentDTOV2 componentDTO,
      final OwnerType ownerType,
      final String ownerId,
      final String scanId,
      final ComponentIdentifier componentIdentifier)
  {
    List<ComponentIdentifier> directParentComponentIdentifiers = Collections.emptyList();

    if (ownerType.equals(OwnerType.APPLICATION) && scanId != null && componentIdentifier.isMaven()) {

      Application application = applicationDAO.getByIdNotNull(ownerId);
      try {
        ApiDependencyTreeNodeDTO dependencyTree =
            apiReportDataServiceV2.getDependencyTreeNoAuth(application.getPublicId(), scanId);
        Set<ApiDependencyTreeNodeDTO> directParents =
            apiDependencyTreeSearcher.findAllDirectParents(dependencyTree, componentDTO.componentIdentifier);
        if (!directParents.isEmpty()) {
          directParentComponentIdentifiers = directParents.stream()
              .map(node -> node.getComponentIdentifier().toComponentIdentifier())
              .distinct()
              .collect(Collectors.toList());
        }
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return directParentComponentIdentifiers;
  }
}
