/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api;

import java.util.ArrayList;
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

import com.sonatype.insight.brain.api.dto.ApiOrganizationDTO;
import com.sonatype.insight.brain.api.dto.ApiOrganizationListDTO;
import com.sonatype.insight.brain.api.dto.ApiRoleMemberMappingDTO;
import com.sonatype.insight.brain.api.dto.ApiRoleMemberMappingListDTO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.organization.OrganizationService;
import com.sonatype.insight.brain.security.ApplicableMembershipMappings;
import com.sonatype.insight.brain.security.Member;
import com.sonatype.insight.brain.security.MembershipMappingService;
import com.sonatype.insight.brain.utils.IdUtils;


/**
 * @since 1.11.0
 */
@Named
@Path(PublicApiPaths.ORG_SERVICE_PATH)
public class ApiOrganizationResource
{

  public static final String ROLE_MEMBERS_PATH = "{organizationId}/roleMembers";

  private final OrganizationService organizationService;

  private final MembershipMappingService membershipMappingService;

  private final ApiMemberMappingAdapter apiMemberMappingAdapter;

  @Inject
  public ApiOrganizationResource(final OrganizationService organizationService,
      final MembershipMappingService membershipMappingService,
      final ApiMemberMappingAdapter apiMemberMappingAdapter)
  {
    this.organizationService = organizationService;
    this.membershipMappingService = membershipMappingService;
    this.apiMemberMappingAdapter = apiMemberMappingAdapter;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApiOrganizationListDTO getAllOrganizations() {
    final List<Organization> organizations = organizationService.getAll();
    return convert(organizations);
  }

  @GET
  @Path(ROLE_MEMBERS_PATH)
  @Produces({MediaType.APPLICATION_JSON})
  public ApiRoleMemberMappingListDTO getApplicableMembershipMappings(
      @PathParam("organizationId") final String organizationId)
  {
    final ApplicableMembershipMappings mappings = membershipMappingService
        .getApplicableMembershipMappingsByInternalId(IdUtils.TYPE_ORGANIZATION, organizationId);
    return apiMemberMappingAdapter.convert(mappings, IdUtils.TYPE_ORGANIZATION);
  }

  @PUT
  @Path(ROLE_MEMBERS_PATH)
  @Consumes({MediaType.APPLICATION_JSON})
  public void setMembershipMappingForRole(
      @PathParam("organizationId") final String organizationId,
      final ApiRoleMemberMappingDTO roleMemberMappingDTO)
  {
    final List<Member> memberList = apiMemberMappingAdapter.convert(roleMemberMappingDTO.getMembers());
    membershipMappingService
        .setMembershipMappingForRoleByInternalId(IdUtils.TYPE_ORGANIZATION, organizationId,
            roleMemberMappingDTO.getRoleId(), memberList);
  }

  private ApiOrganizationListDTO convert(final List<Organization> organizations) {
    final List<ApiOrganizationDTO> dtoList = new ArrayList<>(organizations.size());
    for (final Organization organization : organizations) {
      dtoList.add(convert(organization));
    }
    ApiOrganizationListDTO organizationListDTO = new ApiOrganizationListDTO();
    organizationListDTO.setOrganizations(dtoList);
    return organizationListDTO;
  }

  private ApiOrganizationDTO convert(final Organization organization) {
    final ApiOrganizationDTO dto = new ApiOrganizationDTO();
    dto.setId(organization.getId());
    dto.setName(organization.getName());
    return dto;
  }
}
