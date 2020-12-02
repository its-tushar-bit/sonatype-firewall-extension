/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Set;

import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingListDTO;

/**
 * Resource for API Organizations
 */
public interface ApiOrganizationResourceV2
{
  ApiOrganizationListDTO getOrganizations(Set<String> organizationNames);

  /**
   * @since 1.81
   */
  ApiOrganizationDTO getOrganization(String organizationId);

  /**
   * @deprecated Replaced in 1.70 with
   * {@link ApiRoleMembershipResource#getRoleMembershipsApplicationOrOrganization}
   */
  @Deprecated
  ApiRoleMemberMappingListDTO getApplicableMembershipMappings(String organizationId);

  /**
   * @deprecated Replaced in 1.70 with
   * {@link ApiRoleMembershipResource#grantRoleMembershipApplicationOrOrganization}
   * and
   * {@link ApiRoleMembershipResource#revokeRoleMembershipApplicationOrOrganization}
   */
  @Deprecated
  void setMembershipMappingForRole(String organizationId, ApiRoleMemberMappingListDTO roleMemberMappingDTOs);

  /**
   * @since 1.42
   */
  ApiOrganizationDTO addOrganization(ApiOrganizationDTO organizationDTO);
}
