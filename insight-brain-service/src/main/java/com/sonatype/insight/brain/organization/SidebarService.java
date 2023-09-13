/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyOrganizationDTO;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.MembershipMappingService;

/**
 * @since 1.18.0
 */
@Named
class SidebarService
{
  private final TagDAO tagDAO;

  private final PolicyDAO policyDAO;

  private final LabelDAO labelDAO;

  private final LicenseThreatGroupDAO licenseThreatGroupDAO;

  private final MembershipMappingService membershipMappingService;

  private final OrganizationService organizationService;

  private final ApplicationService applicationService;

  @Inject
  public SidebarService(
      final TagDAO tagDAO,
      final PolicyDAO policyDAO,
      final LabelDAO labelDAO,
      final LicenseThreatGroupDAO licenseThreatGroupDAO,
      final MembershipMappingService membershipMappingService,
      final OrganizationService organizationService,
      final ApplicationService applicationService)
  {
    this.tagDAO = tagDAO;
    this.policyDAO = policyDAO;
    this.labelDAO = labelDAO;
    this.licenseThreatGroupDAO = licenseThreatGroupDAO;
    this.membershipMappingService = membershipMappingService;
    this.organizationService = organizationService;
    this.applicationService = applicationService;
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
      ownerDetailsDTO.tags = new ArrayList<>();
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
    List<Organization> orgs = organizationService.getAll();
    List<Application> apps = applicationService.getApplicationsOrderedByName();

    OwnerHierarchy hierarchy = createOrganizationHierarchy(orgs, apps);

    if (hierarchy.root() != null) {
      calculateChildrenSize(hierarchy, hierarchy.root());
      ownerHierarchyDTO.ownersMap = hierarchy.asHashMap();
      ownerHierarchyDTO.topParentOrganizationId = hierarchy.root().id;
    }

    return ownerHierarchyDTO;
  }

  private OwnerHierarchy createOrganizationHierarchy(List<Organization> orgs, List<Application> apps) {
    OwnerHierarchy hierarchy = new OwnerHierarchy(orgs, apps);
    return hierarchy;
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
        calculateChildrenSize(hierarchy, hierarchy.getOrganizationById(id));
      }
      subOrgsSize += childOrgIds.stream().mapToInt(id -> hierarchy.getOrganizationById(id).subOrgs).sum();
      totalApps += childOrgIds.stream().mapToInt(id -> hierarchy.getOrganizationById(id).totalApps).sum();
    }
    organization.subOrgs = subOrgsSize;
    organization.totalApps = totalApps;
  }
}
