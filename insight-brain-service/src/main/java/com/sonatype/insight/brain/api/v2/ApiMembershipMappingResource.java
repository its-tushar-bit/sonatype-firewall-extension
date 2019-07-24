/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.security.MembershipMappingService;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.70
 */
@Named
@Timed
@Path(PublicApiPaths.MEMBERSHIP_MAPPING_PATH_V2)
public class ApiMembershipMappingResource
{
  private MembershipMappingService membershipMappingService;

  @Inject
  public ApiMembershipMappingResource(MembershipMappingService membershipMappingService) {
    this.membershipMappingService = membershipMappingService;
  }

  @PUT
  @Path("{ownerType: application|organization}/{ownerId}/role/{roleId}/{memberType: user|group}/{memberName}")
  public void grantMembershipMappingApplicationOrOrganization(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId,
      @PathParam("roleId") String roleId,
      @PathParam("memberType") MemberType memberType,
      @PathParam("memberName") String memberName)
  {
    membershipMappingService.grantMembershipMapping(ownerType, ownerId, roleId, memberType, memberName);
  }

  @PUT
  @Path("{ownerType: global|repository_container}/role/{roleId}/{memberType: user|group}/{memberName}")
  public void grantMembershipMappingGlobalOrRepositoryContainer(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("roleId") String roleId,
      @PathParam("memberType") MemberType memberType,
      @PathParam("memberName") String memberName)
  {
    String ownerId;

    if (ownerType == OwnerType.GLOBAL) {
      ownerId = MembershipMapping.GLOBAL_CONTEXT_ID;
    }
    else {
      ownerId = RepositoryContainer.REPOSITORY_CONTAINER_ID;
    }

    membershipMappingService.grantMembershipMapping(ownerType, ownerId, roleId, memberType, memberName);
  }
}
