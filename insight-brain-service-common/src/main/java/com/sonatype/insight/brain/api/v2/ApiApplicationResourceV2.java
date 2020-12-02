/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;
import java.util.Set;

import com.sonatype.insight.brain.api.v2.dto.*;

/**
 * Resource for API Application
 */
public interface ApiApplicationResourceV2
{
  ApiApplicationDTO getApplication(String applicationId);

  /**
   * Get the application DTO list filtered by the set of publicIds.
   * If the publicIds is empty then all applications are returned.
   *
   * @param publicIds The set of public ids to filter on (cannot be null)
   * @return The application DTO list found
   */
  ApiApplicationListDTO getApplications(Set<String> publicIds);

  /**
   * @since 1.102
   */
  ApiApplicationListDTO getApplicationsByOrganizationId(String organizationId);

  ApiApplicationDTO addApplication(ApiApplicationDTO applicationDTO);

  ApiApplicationDTO updateApplication(ApiApplicationDTO applicationDTO,
                                      String applicationId);

  /**
   * @deprecated Replaced in 1.70 with {@link ApiRoleResource#getRoles()}
   */
  @Deprecated
  ApiRoleListDTO getApplicationRoles();

  /**
   * @deprecated Replaced in 1.70 with
   * {@link ApiRoleMembershipResource#getRoleMembershipsApplicationOrOrganization}
   */
  @Deprecated
  ApiRoleMemberMappingListDTO getApplicableMembershipMappings(String applicationId);

  /**
   * @deprecated Replaced in 1.70 with
   * {@link ApiRoleMembershipResource#grantRoleMembershipApplicationOrOrganization}
   * and
   * {@link ApiRoleMembershipResource#revokeRoleMembershipApplicationOrOrganization}
   */
  @Deprecated
  void setMembershipMappingForRole(String applicationId,
                                   ApiRoleMemberMappingListDTO roleMemberMappingDTOs);

  void deleteApplication(String applicationId) throws IOException;

  ApiApplicationDTO cloneApplication(String sourceApplicationId,
                                     String clonedApplicationName,
                                     String clonedApplicationPublicId);

  ApiMoveApplicationResponseDTOV2 moveApplication(String applicationId, String organizationId);
}
