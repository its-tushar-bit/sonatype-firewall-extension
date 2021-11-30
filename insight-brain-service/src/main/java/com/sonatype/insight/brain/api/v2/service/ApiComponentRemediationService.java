/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.hds.ComponentDetailsDTO;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.hds.ComponentRemediationService;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

/**
 * @since 1.64
 */
@Named
public class ApiComponentRemediationService
{
  private final ApplicationDAO applicationDAO = new ApplicationDAO();

  private final ComponentInfoService componentInfoService;

  private final ComponentRemediationService componentRemediationService;

  private final HdsClient hdsClient;

  private final ThirdPartyComponentDAO thirdPartyComponentDAO;

  @Inject
  public ApiComponentRemediationService(
      ComponentInfoService componentInfoService,
      ComponentRemediationService componentRemediationService,
      HdsClient hdsClient,
      ThirdPartyComponentDAO thirdPartyComponentDAO)
  {
    this.componentInfoService = componentInfoService;
    componentInfoService.setToolName("ci");
    this.componentRemediationService = componentRemediationService;
    this.hdsClient = hdsClient;
    this.thirdPartyComponentDAO = thirdPartyComponentDAO;
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  public ApiComponentRemediationDTO getSuggestedRemediationForComponent(
      ApiComponentDTOV2 componentDTO,
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId,
      final String stageId)
  {
    return getSuggestedRemediationForComponentNoAuth(componentDTO, ownerType, ownerId, stageId, null, null);
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  public ApiComponentRemediationDTO getSuggestedRemediationForComponent(
      ApiComponentDTOV2 componentDTO,
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId,
      final String stageId,
      final String identificationSource,
      final String scanId)
  {
    return getSuggestedRemediationForComponentNoAuth(componentDTO, ownerType, ownerId, stageId, identificationSource,
        scanId);
  }

  public ApiComponentRemediationDTO getSuggestedRemediationForComponentNoAuth(
      ApiComponentDTOV2 componentDTO,
      final OwnerType ownerType,
      final String ownerId,
      final String stageId,
      final String identificationSource,
      final String scanId)
  {
    if (stageId != null && StageTypes.getById(stageId) == null) {
      throw new BadRequestException("Invalid stage: " + stageId + ".");
    }

    String publicOwnerId = ownerId;
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

    if (ownerType.equals(OwnerType.APPLICATION)) {
      publicOwnerId = applicationDAO.getByIdNotNull(ownerId).getPublicId();
    }

    List<ComponentDetailsDTO> dtos = componentInfoService
        .getComponentDetailsForAllVersionsNoAuth(ownerType, publicOwnerId, componentIdentifier, stageId,
            identificationSource, scanId, null).getLeft();

    ApiComponentRemediationValueDTO remediationValueDto;
    if (isThirdPartySource) {
      Owner owner = IdUtils.getOwnerNotNull(ownerType, publicOwnerId);
      remediationValueDto = thirdPartyComponentDAO.getSuggestedRemmediation(owner.getId(), componentIdentifier, scanId);
    }
    else {
      remediationValueDto = componentRemediationService.getSuggestedRemediation(componentIdentifier, dtos, ownerType,
          ownerId, stageId);
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
}
