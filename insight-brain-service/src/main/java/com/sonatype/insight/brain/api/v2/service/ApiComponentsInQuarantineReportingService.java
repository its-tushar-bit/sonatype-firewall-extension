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
import com.sonatype.insight.brain.dataaccess.policy.ProxyRepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
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

  private final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  private final ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO;

  @Inject
  public ApiComponentsInQuarantineReportingService(
      final RepositoryService repositoryService,
      final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO,
      final ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO)
  {
    this.repositoryService = repositoryService;
    this.proxyRepositoryComponentDAO = proxyRepositoryComponentDAO;
    this.proxyRepositoryPolicyViolationDAO = proxyRepositoryPolicyViolationDAO;
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

      List<ProxyRepositoryComponent> repositoryComponents = proxyRepositoryComponentDAO
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
      List<ProxyRepositoryComponent> repositoryComponents,
      List<ApiRepositoryComponentPolicyViolationDTO> repositoryComponentPolicyViolationDTOs)
  {
    for (ProxyRepositoryComponent proxyRepositoryComponent : repositoryComponents) {
      ApiRepositoryComponentPolicyViolationDTO repositoryComponentPolicyViolationDTO =
          new ApiRepositoryComponentPolicyViolationDTO();

      repositoryComponentPolicyViolationDTO.component = convertEntityToDTO(proxyRepositoryComponent);

      List<ApiPolicyViolationDTOV2> policyViolationDTOV2List = new ArrayList<>();
      List<ProxyRepositoryPolicyViolation> proxyRepositoryPolicyViolations = proxyRepositoryPolicyViolationDAO
          .getByRepositoryIdAndPathnameAndActionAndNotWaived(repository.getId(),
              proxyRepositoryComponent.getPathname(), Action.ID_FAIL);
      proxyRepositoryPolicyViolationDAO.loadConstraintFacts(proxyRepositoryPolicyViolations);
      for (ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation : proxyRepositoryPolicyViolations) {
        policyViolationDTOV2List.add(ApiPolicyViolationAdapter.convert(proxyRepositoryPolicyViolation));
      }
      repositoryComponentPolicyViolationDTO.policyViolations = policyViolationDTOV2List;

      repositoryComponentPolicyViolationDTOs.add(repositoryComponentPolicyViolationDTO);
    }
  }

  private ApiRepositoryComponentDTO convertEntityToDTO(ProxyRepositoryComponent proxyRepositoryComponent) {
    ApiRepositoryComponentDTO repositoryComponentDTO = new ApiRepositoryComponentDTO();
    repositoryComponentDTO.packageUrl = PackageUrlIdentifier.toPackageUrl(
        proxyRepositoryComponent.getComponentIdentifier());
    repositoryComponentDTO.displayName = proxyRepositoryComponent.getDisplayName();
    repositoryComponentDTO.hash = proxyRepositoryComponent.getHash();
    repositoryComponentDTO.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(
        proxyRepositoryComponent.getComponentIdentifier());
    repositoryComponentDTO.quarantineId = proxyRepositoryComponent.getId();
    repositoryComponentDTO.quarantineTime = proxyRepositoryComponent.getQuarantineTime();
    repositoryComponentDTO.quarantineReleaseTime = proxyRepositoryComponent.getUnquarantineTime();
    return repositoryComponentDTO;
  }
}
