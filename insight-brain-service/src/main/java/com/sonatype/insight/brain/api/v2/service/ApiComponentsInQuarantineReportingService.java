/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentsInQuarantineDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentPolicyViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentsInQuarantineDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryDTO;
import com.sonatype.insight.brain.audit.AuditData;
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
  private static final String QUARANTINED_COMPONENTS_AUDIT_KEY = "numberOfQuarantinedComponents";

  private final RepositoryService repositoryService;

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  private final ApiPolicyViolationAdapter apiPolicyViolationAdapter;

  @Inject
  public ApiComponentsInQuarantineReportingService(
      final RepositoryService repositoryService,
      final RepositoryComponentDAO repositoryComponentDAO,
      final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      final ApiPolicyViolationAdapter apiPolicyViolationAdapter)
  {
    this.repositoryService = repositoryService;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.apiPolicyViolationAdapter = apiPolicyViolationAdapter;
  }

  public ApiComponentsInQuarantineDTO getComponentsInQuarantine() {
    List<RepositoryDTO> repositoryDTOs = repositoryService.getRepositories().repositories;

    if (repositoryDTOs == null) {
      AuditData.get().setData(QUARANTINED_COMPONENTS_AUDIT_KEY, 0);
      return new ApiComponentsInQuarantineDTO();
    }

    return buildApiComponentsInQuarantineDTO(repositoryDTOs);
  }

  private ApiComponentsInQuarantineDTO buildApiComponentsInQuarantineDTO(List<RepositoryDTO> repositoryDTOs) {
    long numOfQuarantinedComponents = 0L;
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
        numOfQuarantinedComponents += repositoryComponentPolicyViolationDTOs.size();
        repositoryComponentsInQuarantineDTO.components = repositoryComponentPolicyViolationDTOs;
        componentsInQuarantineDTO.componentsInQuarantine.add(repositoryComponentsInQuarantineDTO);
      }
    }

    AuditData.get().setData(QUARANTINED_COMPONENTS_AUDIT_KEY, numOfQuarantinedComponents);
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
        policyViolationDTOV2List.add(apiPolicyViolationAdapter.convert(repositoryPolicyViolation));
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
    ComponentDisplayName componentDisplayName =
        ComponentDisplayNameUtil.fromIdentifier(repositoryComponent.getComponentIdentifier());
    repositoryComponentDTO.displayName = componentDisplayName != null ? componentDisplayName.toString() : null;
    repositoryComponentDTO.hash = repositoryComponent.getHash();
    repositoryComponentDTO.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(
        repositoryComponent.getComponentIdentifier());
    repositoryComponentDTO.quarantineId = repositoryComponent.getId();
    repositoryComponentDTO.quarantineTime = repositoryComponent.getQuarantineTime();
    repositoryComponentDTO.quarantineReleaseTime = repositoryComponent.getUnquarantineTime();
    return repositoryComponentDTO;
  }
}
