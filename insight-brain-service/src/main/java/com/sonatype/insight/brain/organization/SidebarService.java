/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.OwnerListDTO.SidebarApplicationDTO;
import com.sonatype.insight.brain.organization.OwnerListDTO.SidebarOrganizationDTO;
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

  private final OrganizationDAO organizationDAO;

  private final ApplicationService applicationService;

  @Inject
  public SidebarService(final TagDAO tagDAO,
                        final PolicyDAO policyDAO,
                        final LabelDAO labelDAO,
                        final LicenseThreatGroupDAO licenseThreatGroupDAO,
                        final MembershipMappingService membershipMappingService,
                        final OrganizationService organizationService,
                        final OrganizationDAO organizationDAO,
                        final ApplicationService applicationService)
  {
    this.tagDAO = tagDAO;
    this.policyDAO = policyDAO;
    this.labelDAO = labelDAO;
    this.licenseThreatGroupDAO = licenseThreatGroupDAO;
    this.membershipMappingService = membershipMappingService;
    this.organizationService = organizationService;
    this.organizationDAO = organizationDAO;
    this.applicationService = applicationService;
  }

  @Authorize(permission = Permission.READ)
  OwnerDetailsDTO getOwnerDetails(@AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
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

  public OwnerListDTO getOwnerList() {
    OwnerListDTO ownerListDTO = new OwnerListDTO();
    ownerListDTO.organizations = new ArrayList<>();

    List<Organization> organizations = organizationService.getAll();
    List<Application> applications = applicationService.getApplications();

    Map<String, SidebarOrganizationDTO> organizationMap = new HashMap<>();
    for (Organization organization : organizations) {
      SidebarOrganizationDTO sidebarOrganizationDTO = new SidebarOrganizationDTO();
      sidebarOrganizationDTO.id = organization.getId();
      sidebarOrganizationDTO.name = organization.getName();
      sidebarOrganizationDTO.applications = new ArrayList<>();

      organizationMap.put(sidebarOrganizationDTO.id, sidebarOrganizationDTO);
      ownerListDTO.organizations.add(sidebarOrganizationDTO);
    }

    for (Application application : applications) {
      SidebarOrganizationDTO sidebarOrganizationDTO = organizationMap.get(application.getOrganizationId());
      if (sidebarOrganizationDTO == null) {
        Organization organization = organizationDAO.getByIdNotNull(application.getOrganizationId());

        sidebarOrganizationDTO = new SidebarOrganizationDTO();
        sidebarOrganizationDTO.id = organization.getId();
        sidebarOrganizationDTO.name = organization.getName();
        sidebarOrganizationDTO.synthetic = true;
        sidebarOrganizationDTO.applications = new ArrayList<>();

        organizationMap.put(sidebarOrganizationDTO.id, sidebarOrganizationDTO);
        ownerListDTO.organizations.add(sidebarOrganizationDTO);
      }

      SidebarApplicationDTO sidebarApplicationDTO = new SidebarApplicationDTO();
      sidebarApplicationDTO.id = application.getId();
      sidebarApplicationDTO.publicId = application.getPublicId();
      sidebarApplicationDTO.name = application.getName();
      sidebarApplicationDTO.organizationId = application.getOrganizationId();

      sidebarOrganizationDTO.applications.add(sidebarApplicationDTO);
    }

    // TODO INT-6135 add this code back after emergency release is done
    /*
    try {
      for (SidebarOrganizationDTO sidebarOrganizationDTO : organizationMap.values()) {
        for (SidebarApplicationDTO sidebarApplicationDTO : sidebarOrganizationDTO.applications) {
          ApiCompositeSourceControlDTO scDto =
              compositeSourceControlService.getCompositeSourceControlByOwner(OwnerType.APPLICATION,
                  sidebarApplicationDTO.id);
          if (scDto != null) {
            sidebarApplicationDTO.provider = scDto.provider.value != null ? scDto.provider.value
              : scDto.provider.parentValue;
            sidebarApplicationDTO.repositoryUrl = scDto.repositoryUrl;
          }
        }
      }
    } catch (UnauthorizedException | InvalidLicenseException e) {
      // the Source Control service has different permissions so may throw Auth exceptions. We don't want to fail the
      // entire call, instead we can just leave the SC values as null
      log.debug("Unable to retrieve source control details for the Sidebar, leaving the values as default: {}",
          e.getMessage());
    }
    */
    return ownerListDTO;
  }
}
