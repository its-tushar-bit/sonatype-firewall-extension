/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Manages role membership mappings for authorization.
 *
 * @since 1.7
 */
@Named
@Path(MembershipMappingResource.SERVICE_PATH)
public class MembershipMappingResource
{
  public static final String SERVICE_PATH = "rest/membershipMapping/{ownerType: global|application|organization}/{ownerId}";

  public static final String ROLE_PATH = "role/{roleId}";

  private final MembershipMappingService membershipMappingService;

  @Inject
  public MembershipMappingResource(final MembershipMappingService membershipMappingService) {
    this.membershipMappingService = membershipMappingService;
  }

  /**
   * Gets the applicable membership mappings for a given application/organization, that is including mappings inherited
   * from parent organizations.
   */
  @GET
  @Produces({MediaType.APPLICATION_JSON})
  public ApplicableMembershipMappings getApplicableMembershipMappings(
      @PathParam("ownerType") final String ownerType,
      @PathParam("ownerId") final String ownerId)
  {
    return membershipMappingService.getApplicableMembershipMappingsByPublicId(ownerType, ownerId);
  }

  /**
   * Updates the membership mapping for a given application/organization and role to the given members.
   */
  @PUT
  @Path(ROLE_PATH)
  @Consumes({MediaType.APPLICATION_JSON})
  public void setMembershipMappingForRole(
      @PathParam("ownerType") final String ownerType,
      @PathParam("ownerId") final String ownerId, @PathParam("roleId") final String roleId,
      final List<Member> members)
  {
    membershipMappingService.setMembershipMappingForRoleByPublicId(ownerType, ownerId, roleId, members);
  }
}
