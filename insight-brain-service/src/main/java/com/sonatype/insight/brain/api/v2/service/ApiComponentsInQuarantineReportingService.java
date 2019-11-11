/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentsInQuarantineDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentPolicyViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentsInQuarantineDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryDTO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dto.repository.RepositoryDTO;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.repository.RepositoryService;
import com.sonatype.insight.purl.PackageUrlIdentifier;

/**
 * @since 1.77
 */
public class ApiComponentsInQuarantineReportingService
{
  private final RepositoryService repositoryService;

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  private final PolicyViolationAdapter policyViolationAdapter;

  @Inject
  public ApiComponentsInQuarantineReportingService(
      final RepositoryService repositoryService,
      final RepositoryComponentDAO repositoryComponentDAO,
      final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      final PolicyViolationAdapter policyViolationAdapter)
  {
    this.repositoryService = repositoryService;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.policyViolationAdapter = policyViolationAdapter;
  }

  public ApiComponentsInQuarantineDTO getComponentsInQuarantine() {
    List<RepositoryDTO> repositoryDTOs = repositoryService.getRepositories().repositories;

    if (repositoryDTOs == null) {
      return new ApiComponentsInQuarantineDTO();
    }

    return buildApiComponentsInQuarantineDTO(repositoryDTOs);
  }

  private ApiComponentsInQuarantineDTO buildApiComponentsInQuarantineDTO(List<RepositoryDTO> repositoryDTOs) {
    ApiComponentsInQuarantineDTO componentsInQuarantineDTO = new ApiComponentsInQuarantineDTO();

    for (RepositoryDTO repositoryDTO : repositoryDTOs) {
      ApiRepositoryComponentsInQuarantineDTO repositoryComponentsInQuarantineDTO =
          new ApiRepositoryComponentsInQuarantineDTO();

      repositoryComponentsInQuarantineDTO.repository = convertRepositoryDTOToApiRepositoryDTO(repositoryDTO);

      List<RepositoryComponent> repositoryComponents = repositoryComponentDAO
          .getQuarantinedByRepositoryId(repositoryDTO.repository.getId());
      List<ApiRepositoryComponentPolicyViolationDTO> repositoryComponentPolicyViolationDTOs = new ArrayList<>();
      buildApiRepositoryComponentPolicyViolationDTOs(repositoryDTO, repositoryComponents,
          repositoryComponentPolicyViolationDTOs);

      if (!repositoryComponentPolicyViolationDTOs.isEmpty()) {
        repositoryComponentsInQuarantineDTO.components = repositoryComponentPolicyViolationDTOs;
        componentsInQuarantineDTO.componentsInQuarantine.add(repositoryComponentsInQuarantineDTO);
      }
    }

    return componentsInQuarantineDTO;
  }

  private void buildApiRepositoryComponentPolicyViolationDTOs(
      RepositoryDTO repositoryDTO,
      List<RepositoryComponent> repositoryComponents,
      List<ApiRepositoryComponentPolicyViolationDTO> repositoryComponentPolicyViolationDTOs)
  {
    for (RepositoryComponent repositoryComponent : repositoryComponents) {
      ApiRepositoryComponentPolicyViolationDTO repositoryComponentPolicyViolationDTO =
          new ApiRepositoryComponentPolicyViolationDTO();

      repositoryComponentPolicyViolationDTO.component = convertEntityToDTO(repositoryComponent);

      List<ApiPolicyViolationDTOV2> policyViolationDTOV2List = new ArrayList<>();
      List<RepositoryPolicyViolation> repositoryPolicyViolations = repositoryPolicyViolationDAO
          .getByRepositoryIdAndPathnameAndActionAndNotWaived(repositoryDTO.repository.getId(),
              repositoryComponent.getPathname(), Action.ID_FAIL);
      for (RepositoryPolicyViolation repositoryPolicyViolation : repositoryPolicyViolations) {
        policyViolationDTOV2List.add(convertEntityToDTO(repositoryPolicyViolation));
      }
      repositoryComponentPolicyViolationDTO.policyViolations = policyViolationDTOV2List;

      repositoryComponentPolicyViolationDTOs.add(repositoryComponentPolicyViolationDTO);
    }
  }

  private ApiRepositoryDTO convertRepositoryDTOToApiRepositoryDTO(RepositoryDTO repositoryDTO) {
    ApiRepositoryDTO apiRepository = new ApiRepositoryDTO();
    apiRepository.repositoryId = repositoryDTO.repository.getId();
    apiRepository.publicId = repositoryDTO.repository.getPublicId();
    apiRepository.format = repositoryDTO.repository.getFormat();
    return apiRepository;
  }

  private ApiRepositoryComponentDTO convertEntityToDTO(RepositoryComponent repositoryComponent) {
    ApiRepositoryComponentDTO repositoryComponentDTO = new ApiRepositoryComponentDTO();
    repositoryComponentDTO.packageUrl = PackageUrlIdentifier.toPackageUrl(
        repositoryComponent.getComponentIdentifier());
    repositoryComponentDTO.hash = repositoryComponent.getHash();
    repositoryComponentDTO.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(
        repositoryComponent.getComponentIdentifier());
    repositoryComponentDTO.quarantineTime = repositoryComponent.getQuarantineTime();
    repositoryComponentDTO.quarantineReleaseTime = repositoryComponent.getUnquarantineTime();
    return repositoryComponentDTO;
  }

  private ApiPolicyViolationDTOV2 convertEntityToDTO(RepositoryPolicyViolation repositoryPolicyViolation) {
    ApiPolicyViolationDTOV2 policyViolationDTOV2 = new ApiPolicyViolationDTOV2();
    policyViolationDTOV2.policyId = repositoryPolicyViolation.getPolicyId();
    policyViolationDTOV2.policyName = repositoryPolicyViolation.getPolicyName();
    policyViolationDTOV2.threatLevel = repositoryPolicyViolation.getThreatLevel();
    policyViolationDTOV2.policyViolationId = repositoryPolicyViolation.getId();
    policyViolationDTOV2.constraintViolations = policyViolationAdapter.convert(repositoryPolicyViolation);
    return policyViolationDTOV2;
  }
}
