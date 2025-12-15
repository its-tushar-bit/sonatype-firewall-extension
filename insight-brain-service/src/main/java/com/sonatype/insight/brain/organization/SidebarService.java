/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyOrganizationDTO;
import com.sonatype.insight.brain.repository.RepositoryService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.MembershipMappingService;

/**
 * @since 1.18.0
 */
@Named
public class SidebarService
{
  private final TagDAO tagDAO;

  private final PolicyDAO policyDAO;

  private final LabelDAO labelDAO;

  private final LicenseThreatGroupDAO licenseThreatGroupDAO;

  private final OrganizationDAO organizationDAO;

  private final RepositoryManagerDAO repositoryManagerDAO;

  private final MembershipMappingService membershipMappingService;

  private final OrganizationService organizationService;

  private final ApplicationService applicationService;

  private final RepositoryService repositoryService;

  @Inject
  public SidebarService(
      final TagDAO tagDAO,
      final PolicyDAO policyDAO,
      final LabelDAO labelDAO,
      final LicenseThreatGroupDAO licenseThreatGroupDAO,
      final OrganizationDAO organizationDAO,
      final RepositoryManagerDAO repositoryManagerDAO,
      final MembershipMappingService membershipMappingService,
      final OrganizationService organizationService,
      final ApplicationService applicationService,
      final RepositoryService repositoryService)
  {
    this.tagDAO = tagDAO;
    this.policyDAO = policyDAO;
    this.labelDAO = labelDAO;
    this.licenseThreatGroupDAO = licenseThreatGroupDAO;
    this.organizationDAO = organizationDAO;
    this.repositoryManagerDAO = repositoryManagerDAO;
    this.membershipMappingService = membershipMappingService;
    this.organizationService = organizationService;
    this.applicationService = applicationService;
    this.repositoryService = repositoryService;
  }

  @Authorize(permission = Permission.READ)
  OwnerDetailsDTO getOwnerDetails(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) String internalOwnerId)
  {
    OwnerDetailsDTO ownerDetailsDTO = new OwnerDetailsDTO();

    if (OwnerType.ORGANIZATION.equals(ownerType)) {
      ownerDetailsDTO.tags = tagDAO.getByOrganizationId(internalOwnerId);
    }
    else {
      ownerDetailsDTO.tags = Collections.emptyList();
    }
    ownerDetailsDTO.policies = policyDAO.getByOwnerId(internalOwnerId);
    ownerDetailsDTO.labels = labelDAO.getByOwnerId(internalOwnerId);
    ownerDetailsDTO.licenseThreatGroups = licenseThreatGroupDAO.getByOwnerId(internalOwnerId);
    ownerDetailsDTO.roles = membershipMappingService.getApplicableMembershipMappings(ownerType, internalOwnerId);

    return ownerDetailsDTO;
  }

  public OwnerHierarchyDTO getOwnerList() {
    OwnerHierarchyDTO ownerHierarchyDTO = new OwnerHierarchyDTO();
    ownerHierarchyDTO.ownersMap = new HashMap<>();
    List<Organization> orgs = organizationService.getAllWithoutRelatedRepositories();
    List<Application> apps = applicationService.getApplicationsWithoutRelatedRepositoriesOrderedByName();
    List<RepositoryManager> repositoryManagers = repositoryService.getRepositoryManagers();
    List<Repository> repositories = repositoryService.getRepositoriesWithReadPermission();

    OwnerHierarchy hierarchy = createOrganizationHierarchy(orgs, apps, repositoryManagers, repositories);

    OwnerHierarchyOrganizationDTO hierarchyRoot = hierarchy.root();
    if (hierarchyRoot != null) {
      calculateChildrenSize(hierarchy, hierarchyRoot);
      ownerHierarchyDTO.ownersMap = hierarchy.asHashMap();
      ownerHierarchyDTO.topParentOrganizationId = hierarchyRoot.id;
    }

    return ownerHierarchyDTO;
  }

  private OwnerHierarchy createOrganizationHierarchy(
      final List<Organization> orgs,
      final List<Application> apps,
      final List<RepositoryManager> repositoryManagers,
      final List<Repository> repositories)
  {
    return new OwnerHierarchy(orgs, apps, repositoryManagers, repositories, organizationDAO, repositoryManagerDAO);
  }

  private void calculateChildrenSize(OwnerHierarchy hierarchy, OwnerHierarchyOrganizationDTO organization) {
    List<String> childOrgIds = organization.organizationIds;

    int subOrgsSize = childOrgIds.size();
    int totalApps;
    if (organization.applicationIds == null) {
      totalApps = 0;
    }
    else {
      totalApps = organization.applicationIds.size();
    }

    if (!childOrgIds.isEmpty()) {
      for (String id : childOrgIds) {
        OwnerHierarchyOrganizationDTO organizationById = hierarchy.getOrganizationById(id);
        if (organizationById != null) {
          calculateChildrenSize(hierarchy, organizationById);
        }
      }
      subOrgsSize += childOrgIds.stream()
          .map(hierarchy::getOrganizationById)
          .filter(Objects::nonNull)
          .mapToInt(org -> org.subOrgs)
          .sum();
      totalApps += childOrgIds.stream()
          .map(hierarchy::getOrganizationById)
          .filter(Objects::nonNull)
          .mapToInt(org -> org.totalApps)
          .sum();
    }
    organization.subOrgs = subOrgsSize;
    organization.totalApps = totalApps;
  }
}
