/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
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

  static final String APPLICATION_OR_ORGANIZATION =
      "{ownerType: application|organization}/{internalOwnerId}/role/{roleId}/{memberType: user|group}/{memberName}";

  static final String GLOBAL_OR_REPOSITORY_CONTAINER =
      "{ownerType: global|repository_container}/role/{roleId}/{memberType: user|group}/{memberName}";

  @Inject
  public ApiMembershipMappingResource(MembershipMappingService membershipMappingService) {
    this.membershipMappingService = membershipMappingService;
  }

  @PUT
  @Path(APPLICATION_OR_ORGANIZATION)
  @Audited(AuditEvent.GRANT_ROLE_MEMBERSHIP)
  public void grantMembershipMappingApplicationOrOrganization(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") String internalOwnerId,
      @PathParam("roleId") String roleId,
      @PathParam("memberType") MemberType memberType,
      @PathParam("memberName") String memberName)
  {
    membershipMappingService.grantMembershipMapping(ownerType, internalOwnerId, roleId, memberType, memberName);
  }

  @PUT
  @Path(GLOBAL_OR_REPOSITORY_CONTAINER)
  @Audited(AuditEvent.GRANT_ROLE_MEMBERSHIP)
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

  @DELETE
  @Path(APPLICATION_OR_ORGANIZATION)
  @Audited(AuditEvent.REVOKE_ROLE_MEMBERSHIP)
  public void revokeMembershipMappingApplicationOrOrganization(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") String internalOwnerId,
      @PathParam("roleId") String roleId,
      @PathParam("memberType") MemberType memberType,
      @PathParam("memberName") String memberName)
  {
    membershipMappingService.revokeMembershipMapping(ownerType, internalOwnerId, roleId, memberType, memberName);
  }

  @DELETE
  @Path(GLOBAL_OR_REPOSITORY_CONTAINER)
  @Audited(AuditEvent.REVOKE_ROLE_MEMBERSHIP)
  public void revokeMembershipMappingGlobalOrRepositoryContainer(
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

    membershipMappingService.revokeMembershipMapping(ownerType, ownerId, roleId, memberType, memberName);
  }
}
