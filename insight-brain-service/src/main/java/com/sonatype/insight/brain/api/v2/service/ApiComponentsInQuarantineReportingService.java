/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentsInQuarantineDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentPolicyViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentsInQuarantineDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.repository.RepositoryService;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 * @since 1.77
 */
@Named
@Singleton
public class ApiComponentsInQuarantineReportingService
{
  private static final String QUARANTINED_COMPONENTS_AUDIT_KEY = "numberOfQuarantinedComponents";

  private final RepositoryService repositoryService;

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  @Inject
  public ApiComponentsInQuarantineReportingService(
      final RepositoryService repositoryService,
      final RepositoryComponentDAO repositoryComponentDAO,
      final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO)
  {
    this.repositoryService = repositoryService;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
  }

  public ApiComponentsInQuarantineDTO getComponentsInQuarantine() {
    List<Repository> repositories = repositoryService.getRepositoriesWithReadPermission();

    if (repositories == null) {
      AuditData.get().setData(QUARANTINED_COMPONENTS_AUDIT_KEY, 0);
      return new ApiComponentsInQuarantineDTO();
    }

    return buildApiComponentsInQuarantineDTO(repositories);
  }

  private ApiComponentsInQuarantineDTO buildApiComponentsInQuarantineDTO(List<Repository> repositories) {
    long numOfQuarantinedComponents = 0L;
    ApiComponentsInQuarantineDTO componentsInQuarantineDTO = new ApiComponentsInQuarantineDTO();

    for (Repository repository : repositories) {
      ApiRepositoryComponentsInQuarantineDTO repositoryComponentsInQuarantineDTO =
          new ApiRepositoryComponentsInQuarantineDTO();

      repositoryComponentsInQuarantineDTO.repository = ApiRepositoryAdapter.convert(repository);

      List<RepositoryComponent> repositoryComponents = repositoryComponentDAO
          .getQuarantinedByRepositoryId(repository.getId());
      List<ApiRepositoryComponentPolicyViolationDTO> repositoryComponentPolicyViolationDTOs = new ArrayList<>();
      buildApiRepositoryComponentPolicyViolationDTOs(repository, repositoryComponents,
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
      Repository repository,
      List<RepositoryComponent> repositoryComponents,
      List<ApiRepositoryComponentPolicyViolationDTO> repositoryComponentPolicyViolationDTOs)
  {
    for (RepositoryComponent repositoryComponent : repositoryComponents) {
      ApiRepositoryComponentPolicyViolationDTO repositoryComponentPolicyViolationDTO =
          new ApiRepositoryComponentPolicyViolationDTO();

      repositoryComponentPolicyViolationDTO.component = convertEntityToDTO(repositoryComponent);

      List<ApiPolicyViolationDTOV2> policyViolationDTOV2List = new ArrayList<>();
      List<RepositoryPolicyViolation> repositoryPolicyViolations = repositoryPolicyViolationDAO
          .getByRepositoryIdAndPathnameAndActionAndNotWaived(repository.getId(),
              repositoryComponent.getPathname(), Action.ID_FAIL);
      repositoryPolicyViolationDAO.loadConstraintFacts(repositoryPolicyViolations);
      for (RepositoryPolicyViolation repositoryPolicyViolation : repositoryPolicyViolations) {
        policyViolationDTOV2List.add(ApiPolicyViolationAdapter.convert(repositoryPolicyViolation));
      }
      repositoryComponentPolicyViolationDTO.policyViolations = policyViolationDTOV2List;

      repositoryComponentPolicyViolationDTOs.add(repositoryComponentPolicyViolationDTO);
    }
  }

  private ApiRepositoryComponentDTO convertEntityToDTO(RepositoryComponent repositoryComponent) {
    ApiRepositoryComponentDTO repositoryComponentDTO = new ApiRepositoryComponentDTO();
    repositoryComponentDTO.packageUrl = PackageUrlIdentifier.toPackageUrl(
        repositoryComponent.getComponentIdentifier());
    repositoryComponentDTO.displayName = repositoryComponent.getDisplayName();
    repositoryComponentDTO.hash = repositoryComponent.getHash();
    repositoryComponentDTO.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(
        repositoryComponent.getComponentIdentifier());
    repositoryComponentDTO.quarantineId = repositoryComponent.getId();
    repositoryComponentDTO.quarantineTime = repositoryComponent.getQuarantineTime();
    repositoryComponentDTO.quarantineReleaseTime = repositoryComponent.getUnquarantineTime();
    return repositoryComponentDTO;
  }
}
