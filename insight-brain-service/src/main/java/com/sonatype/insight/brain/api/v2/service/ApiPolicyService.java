/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiPolicyDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyOwnerType;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.organization.OrganizationService;
import com.sonatype.insight.brain.repository.RepositoryService;

/**
 * @since 1.12.0
 */
@Named
public class ApiPolicyService
{
  private final PolicyDAO policyDAO;

  private final ApplicationService applicationService;

  private final OrganizationService organizationService;

  private final RepositoryService repositoryService;

  private final OwnerDAO ownerDAO;

  @Inject
  public ApiPolicyService(
      final PolicyDAO policyDAO,
      final ApplicationService applicationService,
      final OrganizationService organizationService,
      final RepositoryService repositoryService,
      final OwnerDAO ownerDAO)
  {
    this.policyDAO = policyDAO;
    this.applicationService = applicationService;
    this.organizationService = organizationService;
    this.repositoryService = repositoryService;
    this.ownerDAO = ownerDAO;
  }

  public ApiPolicyListDTO getPolicies() {
    ApiPolicyListDTO policyListDTO = new ApiPolicyListDTO();
    policyListDTO.policies = filterPolicies();
    return policyListDTO;
  }

  private List<ApiPolicyDTO> filterPolicies() {
    List<ApiPolicyDTO> apiPolicyList = new ArrayList<>();
    Set<String> applicationIds = new HashSet<>();
    Set<String> organizationIds = new HashSet<>();
    Set<String> repositoryManagerIds = new HashSet<>();
    Set<String> repositoryIds = new HashSet<>();
    // There is only a single repository container but left as a set to be consistent with the other ids
    Set<String> repositoryContainerId = new HashSet<>();
    getFilteredOwnerIds(applicationIds, organizationIds, repositoryIds, repositoryManagerIds, repositoryContainerId);

    List<Policy> appPolicies = policyDAO.getByOwnerIds(applicationIds);
    apiPolicyList.addAll(ApiPolicyAdapter.convert(appPolicies, ApiPolicyOwnerType.APPLICATION));
    List<Policy> orgPolicies = policyDAO.getByOwnerIds(organizationIds);
    apiPolicyList.addAll(ApiPolicyAdapter.convert(orgPolicies, ApiPolicyOwnerType.ORGANIZATION));

    List<Policy> repositoryContainerPolicies = policyDAO.getByOwnerIds(repositoryContainerId);
    apiPolicyList.addAll(ApiPolicyAdapter.convert(repositoryContainerPolicies,
        ApiPolicyOwnerType.REPOSITORY_CONTAINER));
    List<Policy> repositoryManagerPolicies = policyDAO.getByOwnerIds(repositoryManagerIds);
    apiPolicyList.addAll(ApiPolicyAdapter.convert(repositoryManagerPolicies, ApiPolicyOwnerType.REPOSITORY_MANAGER));
    List<Policy> repositoryPolicies = policyDAO.getByOwnerIds(repositoryIds);
    apiPolicyList.addAll(ApiPolicyAdapter.convert(repositoryPolicies, ApiPolicyOwnerType.REPOSITORY));

    return apiPolicyList;
  }

  private void getFilteredOwnerIds(
      Set<String> applicationIds,
      Set<String> organizationIds,
      Set<String> repositoryIds,
      Set<String> repositoryManagerIds,
      Set<String> repositoryContainerIds)
  {
    // Add the apps that the user has permissions to
    for (Application application : applicationService.getApplications()) {
      applicationIds.add(application.getId());
      // Since the user has permission to the app,
      // add the org hierarchy the app belongs to even if they don't have permissions to the orgs themselves)
      addOrganizationIds(application.getOrganizationId(), organizationIds);
    }

    // Now add the orgs that the user has permissions to
    for (Organization organization : organizationService.getAll()) {
      organizationIds.add(organization.getId());
      // as with apps, also add any parent orgs regardless of explicit permission
      addOrganizationIds(organization.getParentOrganizationId(), organizationIds);
    }

    // Add the repositories that the user has permissions to
    for (Repository repository: repositoryService.getRepositoriesWithReadPermission()) {
      repositoryIds.add(repository.getId());
      // Need to add the repository manager and its hierarchy
      repositoryManagerIds.add(repository.getRepositoryManagerId());
      repositoryContainerIds.add(RepositoryContainer.REPOSITORY_CONTAINER_ID);
      addOrganizationIds(Organization.ROOT_ORGANIZATION_ID, organizationIds);
    }

    // Add the repository managers that the user has permissions to
    for (RepositoryManager repositoryManager : repositoryService.getRepositoryManagers()) {
      repositoryManagerIds.add(repositoryManager.getId());
      repositoryContainerIds.add(RepositoryContainer.REPOSITORY_CONTAINER_ID);
      addOrganizationIds(Organization.ROOT_ORGANIZATION_ID, organizationIds);
    }

    // Add the repository container if the user has permission
    if (repositoryService.checkReadPermissionRepositoryContainer()) {
      repositoryContainerIds.add(RepositoryContainer.REPOSITORY_CONTAINER_ID);
      addOrganizationIds(Organization.ROOT_ORGANIZATION_ID, organizationIds);
    }
  }

  private void addOrganizationIds(String organizationId, Set<String> organizationIds) {
    if (organizationId != null && organizationIds.add(organizationId)) {
      for (Owner owner : ownerDAO.walkHierarchy(organizationId)) {
        if (owner.getParentOwnerId() == null || !organizationIds.add(owner.getParentOwnerId())) {
          break;
        }
      }
    }
  }
}
