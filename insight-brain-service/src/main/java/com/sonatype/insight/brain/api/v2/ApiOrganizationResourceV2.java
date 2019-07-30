/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingListDTO;
import com.sonatype.insight.brain.api.v2.service.ApiOrganizationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.security.ApplicableMembershipMappings;
import com.sonatype.insight.brain.security.Member;
import com.sonatype.insight.brain.security.MembershipMappingService;

import com.codahale.metrics.annotation.Timed;

/**
 *
 * @since 1.11.0
 */
@Named
@Timed
@Path(PublicApiPaths.ORG_RESOURCE_PATH)
public class ApiOrganizationResourceV2
{
  public static final String ROLE_MEMBERS_PATH = "{organizationId}/roleMembers";

  private final ApiOrganizationService apiOrganizationService;

  private final MembershipMappingService membershipMappingService;

  private final ApiMemberMappingAdapter apiMemberMappingAdapter;

  @Inject
  public ApiOrganizationResourceV2(final ApiOrganizationService apiOrganizationService,
                                 final MembershipMappingService membershipMappingService,
                                 final ApiMemberMappingAdapter apiMemberMappingAdapter)
  {
    this.apiOrganizationService = apiOrganizationService;
    this.membershipMappingService = membershipMappingService;
    this.apiMemberMappingAdapter = apiMemberMappingAdapter;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApiOrganizationListDTO getAllOrganizations() {
    return apiOrganizationService.getAll();
  }

  /**
   * @deprecated Replaced in 1.70 with
   *             {@link ApiRoleMembershipResource#getRoleMembershipsApplicationOrOrganization}
   */
  @GET
  @Path(ROLE_MEMBERS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Deprecated
  public ApiRoleMemberMappingListDTO getApplicableMembershipMappings(
      @PathParam("organizationId") final String organizationId)
  {
    final ApplicableMembershipMappings mappings = membershipMappingService.getApplicableMembershipMappings(
        OwnerType.ORGANIZATION, organizationId);
    return apiMemberMappingAdapter.convert(mappings, OwnerType.ORGANIZATION);
  }

  /**
   * @deprecated Replaced in 1.70 with
   *             {@link ApiRoleMembershipResource#grantRoleMembershipApplicationOrOrganization}
   *             and
   *             {@link ApiRoleMembershipResource#revokeRoleMembershipApplicationOrOrganization}
   */
  @PUT
  @Path(ROLE_MEMBERS_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_ROLE_MEMBERSHIP)
  @Deprecated
  public void setMembershipMappingForRole(@PathParam("organizationId") final String organizationId,
                                          final ApiRoleMemberMappingListDTO roleMemberMappingDTOs)
  {
    Map<String, List<Member>> roleToMembers = apiMemberMappingAdapter.convert(roleMemberMappingDTOs);
    membershipMappingService.setMembershipMappings(OwnerType.ORGANIZATION, organizationId, roleToMembers);
  }

  /**
   * @since 1.42
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_ORGANIZATION)
  public ApiOrganizationDTO addOrganization(final ApiOrganizationDTO organizationDTO) {
    return apiOrganizationService.addOrganization(organizationDTO);
  }
}
