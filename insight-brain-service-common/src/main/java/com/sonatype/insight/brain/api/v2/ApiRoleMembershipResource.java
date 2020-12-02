/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingListDTO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.MemberType;

/**
 * Resource for API Role Membership
 */
public interface ApiRoleMembershipResource
{
  void grantRoleMembershipApplicationOrOrganization(OwnerType ownerType,
                                                    String internalOwnerId,
                                                    String roleId,
                                                    MemberType memberType,
                                                    String memberName);

  void grantRoleMembershipGlobalOrRepositoryContainer(OwnerType ownerType,
                                                      String roleId,
                                                      MemberType memberType,
                                                      String memberName);

  ApiRoleMemberMappingListDTO getRoleMembershipsApplicationOrOrganization(OwnerType ownerType, String internalOwnerId);

  ApiRoleMemberMappingListDTO getRoleMembershipsGlobalOrRepositoryContainer(OwnerType ownerType);

  void revokeRoleMembershipApplicationOrOrganization(OwnerType ownerType,
                                                     String internalOwnerId,
                                                     String roleId,
                                                     MemberType memberType,
                                                     String memberName);

  void revokeRoleMembershipGlobalOrRepositoryContainer(OwnerType ownerType,
                                                       String roleId,
                                                       MemberType memberType,
                                                       String memberName);
}
