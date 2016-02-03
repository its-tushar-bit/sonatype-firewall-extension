/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
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

  @Inject
  public SidebarService(final TagDAO tagDAO,
                        final PolicyDAO policyDAO,
                        final LabelDAO labelDAO,
                        final LicenseThreatGroupDAO licenseThreatGroupDAO,
                        final MembershipMappingService membershipMappingService)
  {
    this.tagDAO = tagDAO;
    this.policyDAO = policyDAO;
    this.labelDAO = labelDAO;
    this.licenseThreatGroupDAO = licenseThreatGroupDAO;
    this.membershipMappingService = membershipMappingService;
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
}
