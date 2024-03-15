/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingListDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.security.MembershipMappingService;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.70
 */
@Named
@Timed
@Path(PublicApiPaths.ROLE_MEMBERSHIP_PATH_V2)
public class ApiRoleMembershipResource
{
  private MembershipMappingService membershipMappingService;

  public static final String APPLICATION_OR_ORGANIZATION =
      "{ownerType: application|organization}/{internalOwnerId}/role/{roleId}/{memberType: user|group}/{memberName}";

  static final String GLOBAL_OR_REPOSITORY_CONTAINER =
      "{ownerType: global|repository_container}/role/{roleId}/{memberType: user|group}/{memberName}";

  @Inject
  public ApiRoleMembershipResource(MembershipMappingService membershipMappingService) {
    this.membershipMappingService = membershipMappingService;
  }

  @PUT
  @Path(APPLICATION_OR_ORGANIZATION)
  @Audited(AuditEvent.GRANT_ROLE_MEMBERSHIP)
  public void grantRoleMembershipApplicationOrOrganization(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") String internalOwnerId,
      @PathParam("roleId") String roleId,
      @PathParam("memberType") MemberType memberType,
      @PathParam("memberName") String memberName)
  {
    membershipMappingService.grantRoleMembership(ownerType, internalOwnerId, roleId, memberType, memberName);
  }

  @PUT
  @Path(GLOBAL_OR_REPOSITORY_CONTAINER)
  @Audited(AuditEvent.GRANT_ROLE_MEMBERSHIP)
  public void grantRoleMembershipGlobalOrRepositoryContainer(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("roleId") String roleId,
      @PathParam("memberType") MemberType memberType,
      @PathParam("memberName") String memberName)
  {
    membershipMappingService.grantRoleMembership(ownerType, null, roleId, memberType, memberName);
  }

  @GET
  @Path("{ownerType: application|organization}/{internalOwnerId}")
  @Produces(MediaType.APPLICATION_JSON)
  public ApiRoleMemberMappingListDTO getRoleMembershipsApplicationOrOrganization(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") String internalOwnerId)
  {
    return membershipMappingService.getRoleMembershipsOmitEmpty(ownerType, internalOwnerId);
  }

  @GET
  @Path("{ownerType: global|repository_container}")
  @Produces(MediaType.APPLICATION_JSON)
  public ApiRoleMemberMappingListDTO getRoleMembershipsGlobalOrRepositoryContainer(
      @PathParam("ownerType") OwnerType ownerType)
  {
    return membershipMappingService.getRoleMembershipsOmitEmpty(ownerType, null);
  }

  @DELETE
  @Path(APPLICATION_OR_ORGANIZATION)
  @Audited(AuditEvent.REVOKE_ROLE_MEMBERSHIP)
  public void revokeRoleMembershipApplicationOrOrganization(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") String internalOwnerId,
      @PathParam("roleId") String roleId,
      @PathParam("memberType") MemberType memberType,
      @PathParam("memberName") String memberName)
  {
    membershipMappingService.revokeRoleMembership(ownerType, internalOwnerId, roleId, memberType, memberName);
  }

  @DELETE
  @Path(GLOBAL_OR_REPOSITORY_CONTAINER)
  @Audited(AuditEvent.REVOKE_ROLE_MEMBERSHIP)
  public void revokeRoleMembershipGlobalOrRepositoryContainer(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("roleId") String roleId,
      @PathParam("memberType") MemberType memberType,
      @PathParam("memberName") String memberName)
  {
    membershipMappingService.revokeRoleMembership(ownerType, null, roleId, memberType, memberName);
  }
}
