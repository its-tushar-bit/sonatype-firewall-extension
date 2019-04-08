/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.hds.ComponentDetailsDTO;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.error.exception.BadRequestException;

/**
 * @since 1.64
 */
@Named
public class ApiComponentRemediationService
{
  private ApplicationDAO applicationDAO = new ApplicationDAO();

  private final ComponentInfoService componentInfoService;

  @Inject
  public ApiComponentRemediationService(ComponentInfoService componentInfoService) {
    this.componentInfoService = componentInfoService;
    componentInfoService.setToolName("ci");
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  public ApiComponentRemediationDTO getSuggestedRemediationForComponent(
      ApiComponentDTOV2 componentDTO,
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId)
  {
    String publicOwnerId = ownerId;

    validateComponentIdentifier(componentDTO);
    ComponentIdentifier componentIdentifier = new ComponentIdentifier(
        componentDTO.componentIdentifier.getFormat(), componentDTO.componentIdentifier.getCoordinates());

    if (ownerType.equals(OwnerType.APPLICATION)) {
      publicOwnerId = applicationDAO.getByIdNotNull(ownerId).getPublicId();
    }

    List<ComponentDetailsDTO> dtos = componentInfoService
        .getComponentDetailsForAllVersionsNoAuth(ownerType, publicOwnerId, componentIdentifier);
    ApiComponentRemediationDTO componentRemediationDto = new ApiComponentRemediationDTO();
    boolean versionReached = false;
    for (ComponentDetailsDTO dto : dtos) {
      if (!versionReached && dto.componentIdentifier.equals(componentIdentifier)) {
        // see if the supplied version is violation-free
        if (dto.violatedPolicyCount == 0) {
          componentRemediationDto.remediation.versionChanges
              .add(createVersionChangeOption(dto, ApiVersionChangeOptionType.CURRENT));
          break;
        }
        versionReached = true;
      }
      if (versionReached && dto.violatedPolicyCount == 0) {
        componentRemediationDto.remediation.versionChanges
            .add(createVersionChangeOption(dto, ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS));
        break;
      }
    }
    return componentRemediationDto;
  }

  private ApiVersionChangeOptionDTO createVersionChangeOption(ComponentDetailsDTO dto,
                                                              ApiVersionChangeOptionType apiVersionChangeOptionType)
  {
    ApiComponentDTOV2 componentDTOV2 = new ApiComponentDTOV2();
    componentDTOV2.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(dto.componentIdentifier);
    componentDTOV2.proprietary = null; // not applicable
    return new ApiVersionChangeOptionDTO(apiVersionChangeOptionType, new ApiComponentChangeActionDTO(componentDTOV2));
  }

  private void validateComponentIdentifier(ApiComponentDTOV2 componentDTO) {
    if (componentDTO.componentIdentifier == null) {
      throw new BadRequestException("ComponentIdentifier must be supplied.");
    }

    try {
      ComponentIdentifier componentIdentifier = new ComponentIdentifier(componentDTO.componentIdentifier.getFormat(),
          componentDTO.componentIdentifier.getCoordinates());
      componentIdentifier.ensureComplete();
    }
    catch (InvalidComponentIdentifierException e) {
      throw new BadRequestException(e.getMessage(), e);
    }
  }
}
