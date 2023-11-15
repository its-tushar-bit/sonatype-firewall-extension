/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.utils.IdUtils;

import com.codahale.metrics.annotation.Timed;

/**
 * Manages role membership mappings for authorization.
 *
 * @since 1.7
 */
@Named
@Timed
@Path(MembershipMappingResource.RESOURCE_PATH)
public class MembershipMappingResource
{
  static final String RESOURCE_PATH = "rest/membershipMapping";

  static final String APPLICABLE_MAPPINGS_PATH =
      "{ownerType: global|application|organization|repository_manager|repository}/{ownerId}";

  static final String SINGLETON_APPLICABLE_MAPPINGS_PATH = "{ownerType: repository_container}";

  static final String ROLE_PATH = APPLICABLE_MAPPINGS_PATH + "/role/{roleId}";

  static final String SINGLETON_ROLE_PATH = SINGLETON_APPLICABLE_MAPPINGS_PATH + "/role/{roleId}";

  private final MembershipMappingService membershipMappingService;

  @Inject
  public MembershipMappingResource(final MembershipMappingService membershipMappingService) {
    this.membershipMappingService = membershipMappingService;
  }

  /**
   * Gets the applicable role membership mappings for a given application/organization or at global level (including
   * mappings inherited from parent organizations).
   */
  @GET
  @Path(APPLICABLE_MAPPINGS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ApplicableMembershipMappings getApplicableMembershipMappings(@PathParam("ownerType") final OwnerType ownerType,
                                                                      @PathParam("ownerId") final String ownerId)
  {
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);
    return membershipMappingService.getApplicableMembershipMappings(ownerType, internalOwnerId);
  }

  /**
   * Updates the role membership mappings for a given application/organization or global level.
   */
  @PUT
  @Path(ROLE_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_ROLE_MEMBERSHIP)
  public void setMembershipMappingForRole(@PathParam("ownerType") final OwnerType ownerType,
                                          @PathParam("ownerId") final String ownerId,
                                          @PathParam("roleId") final String roleId,
                                          final List<Member> members)
  {
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);
    Map<String, List<Member>> membersByRoleId = new HashMap<>();
    membersByRoleId.put(roleId, members);
    membershipMappingService.setMembershipMappings(ownerType, internalOwnerId, membersByRoleId);
  }

  /**
   * @since 1.18
   */
  @GET
  @Path(SINGLETON_APPLICABLE_MAPPINGS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ApplicableMembershipMappings getApplicableMembershipMappings(
      @PathParam("ownerType") final OwnerType ownerType)
  {
    return getApplicableMembershipMappings(ownerType, null);
  }

  /**
   * @since 1.18
   */
  @PUT
  @Path(SINGLETON_ROLE_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_ROLE_MEMBERSHIP)
  public void setMembershipMappingForRole(@PathParam("ownerType") final OwnerType ownerType,
                                          @PathParam("roleId") final String roleId,
                                          final List<Member> members)
  {
    setMembershipMappingForRole(ownerType, null, roleId, members);
  }
}
