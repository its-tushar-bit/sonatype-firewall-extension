/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.api.dto.ApiRoleMemberMappingDTO;
import com.sonatype.insight.brain.api.dto.ApiRoleMemberMappingListDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.security.ApplicableMembershipMappings;
import com.sonatype.insight.brain.security.Member;
import com.sonatype.insight.brain.security.MembershipMappingService;
import com.sonatype.insight.brain.utils.IdUtils;

/**
 * @since 1.11.0
 */
@Named
@Path(PublicApiPaths.APP_SERVICE_PATH)
public class ApiApplicationResource
{
  /**
   * Internal application Id
   */
  public static final String APPLICATION_ID = "{applicationId}";

  public static final String ROLE_MEMBERS_PATH = APPLICATION_ID + "/roleMembers";

  private final ApplicationService applicationService;

  private final MembershipMappingService membershipMappingService;

  private final ApiMemberMappingAdapter apiMemberMappingAdapter;

  private final ApiApplicationAdapter apiApplicationAdapter;

  @Inject
  public ApiApplicationResource(final ApplicationService applicationService,
      final MembershipMappingService membershipMappingService,
      final ApiMemberMappingAdapter apiMemberMappingAdapter,
      final ApiApplicationAdapter apiApplicationAdapter)
  {
    this.applicationService = applicationService;
    this.membershipMappingService = membershipMappingService;
    this.apiMemberMappingAdapter = apiMemberMappingAdapter;
    this.apiApplicationAdapter = apiApplicationAdapter;
  }

  @GET
  @Path(APPLICATION_ID)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiApplicationDTO getApplication(
      @PathParam("applicationId") final String applicationId)
  {
    final Application application = applicationService.getApplicationByIdNotNull(applicationId);
    return apiApplicationAdapter.convert(application);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiApplicationDTO addApplication(final ApiApplicationDTO applicationDTO) {
    final Application application = apiApplicationAdapter.convert(applicationDTO);
    final Application returnedApp = applicationService.addApplication(application);
    return apiApplicationAdapter.convert(returnedApp);
  }

  @GET
  @Path(ROLE_MEMBERS_PATH)
  @Produces({MediaType.APPLICATION_JSON})
  public ApiRoleMemberMappingListDTO getApplicableMembershipMappings(
      @PathParam("applicationId") final String applicationId)
  {
    final ApplicableMembershipMappings mappings = membershipMappingService
        .getApplicableMembershipMappingsByInternalId(IdUtils.TYPE_APPLICATION, applicationId);
    return apiMemberMappingAdapter.convert(mappings, IdUtils.TYPE_APPLICATION);
  }

  @PUT
  @Path(ROLE_MEMBERS_PATH)
  @Consumes({MediaType.APPLICATION_JSON})
  public void setMembershipMappingForRole(
      @PathParam("applicationId") final String applicationId,
      final ApiRoleMemberMappingDTO roleMemberMappingDTO)
  {
    final List<Member> memberList = apiMemberMappingAdapter.convert(roleMemberMappingDTO.members);
    membershipMappingService
        .setMembershipMappingForRoleByInternalId(IdUtils.TYPE_APPLICATION, applicationId,
            roleMemberMappingDTO.roleId, memberList);
  }

  @DELETE
  @Path(APPLICATION_ID)
  public void deleteApplication(@PathParam("applicationId") final String applicationId)
      throws IOException
  {
    applicationService.deleteApplicationById(applicationId);
  }
}
