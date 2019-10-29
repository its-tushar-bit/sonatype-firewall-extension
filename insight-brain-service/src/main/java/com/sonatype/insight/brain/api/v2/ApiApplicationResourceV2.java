/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingListDTO;
import com.sonatype.insight.brain.api.v2.service.ApiApplicationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dataaccess.InvalidApplicationException;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.organization.ApplicationCloneService;
import com.sonatype.insight.brain.security.ApplicableMembershipMappings;
import com.sonatype.insight.brain.security.Member;
import com.sonatype.insight.brain.security.MembershipMappingService;

import com.codahale.metrics.annotation.Timed;
import org.codehaus.plexus.util.StringUtils;

/**
 *
 * @since 1.11.0
 */
@Named
@Timed
@Path(PublicApiPaths.APP_RESOURCE_PATH)
public class ApiApplicationResourceV2
{
  /**
   * Internal application Id
   */
  public static final String APPLICATION_ID = "{applicationId}";

  public static final String ROLE_PATH = "/roles";

  public static final String ROLE_MEMBERS_PATH = APPLICATION_ID + "/roleMembers";

  // NOTE: more specific path param name than applicationId to avoid default handling by AuditContainerRequestFilter
  public static final String CLONE_PATH = "{sourceApplicationId}/clone";

  private final ApiApplicationService apiApplicationService;

  private final MembershipMappingService membershipMappingService;

  private final ApiMemberMappingAdapter apiMemberMappingAdapter;

  private final ApplicationCloneService applicationCloneService;

  @Inject
  public ApiApplicationResourceV2(final ApiApplicationService apiApplicationService,
                                final MembershipMappingService membershipMappingService,
      final ApiMemberMappingAdapter apiMemberMappingAdapter,
      final ApplicationCloneService applicationCloneService)
  {
    this.apiApplicationService = apiApplicationService;
    this.membershipMappingService = membershipMappingService;
    this.apiMemberMappingAdapter = apiMemberMappingAdapter;
    this.applicationCloneService = applicationCloneService;
  }

  @GET
  @Path(APPLICATION_ID)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiApplicationDTO getApplication(@PathParam("applicationId") final String applicationId) {
    return apiApplicationService.getApplicationById(applicationId);
  }

  /**
   * Get the application DTO list filtered by the set of publicIds.
   * If the publicIds is empty then all applications are returned.
   *
   * @param publicIds The set of public ids to filter on (cannot be null)
   * @return The application DTO list found
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApiApplicationListDTO getApplications(@QueryParam("publicId") final Set<String> publicIds) {
    return apiApplicationService.getApplicationDTOs(publicIds);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_APPLICATION)
  public ApiApplicationDTO addApplication(final ApiApplicationDTO applicationDTO) {
    return apiApplicationService.addApplication(applicationDTO);
  }

  @PUT
  @Path(APPLICATION_ID)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_APPLICATION)
  public ApiApplicationDTO updateApplication(final ApiApplicationDTO applicationDTO,
                                             @PathParam("applicationId") final String applicationId)
  {
    if (StringUtils.isBlank(applicationDTO.id)) {
      applicationDTO.id = applicationId;
    }

    if (!applicationId.equals(applicationDTO.id)) {
      throw new InvalidApplicationException("The applicationId=" + applicationId
          + " provided in the url did not match the id=" + applicationDTO.id + " provided in the json.");
    }
    return apiApplicationService.updateApplication(applicationDTO);
  }

  /**
   * @deprecated Replaced in 1.70 with {@link ApiRoleResource#getRoles()}
   */
  @GET
  @Path(ROLE_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Deprecated
  public ApiRoleListDTO getApplicationRoles() {
    return apiApplicationService.getApplicationRoles();
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
      @PathParam("applicationId") final String applicationId)
  {
    final ApplicableMembershipMappings mappings = membershipMappingService.getApplicableMembershipMappings(
        OwnerType.APPLICATION, applicationId);
    return apiMemberMappingAdapter.convert(mappings, OwnerType.APPLICATION);
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
  public void setMembershipMappingForRole(@PathParam("applicationId") final String applicationId,
                                          final ApiRoleMemberMappingListDTO roleMemberMappingDTOs)
  {
    Map<String, List<Member>> roleToMembers = apiMemberMappingAdapter.convert(roleMemberMappingDTOs);
    membershipMappingService.setMembershipMappings(OwnerType.APPLICATION, applicationId, roleToMembers);
  }

  @DELETE
  @Path(APPLICATION_ID)
  @Audited(AuditEvent.DELETE_APPLICATION)
  public void deleteApplication(@PathParam("applicationId") final String applicationId) throws IOException {
    apiApplicationService.deleteApplication(applicationId);
  }

  @POST
  @Path(CLONE_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_APPLICATION)
  public ApiApplicationDTO cloneApplication(
      @PathParam("sourceApplicationId") String sourceApplicationId,
      @QueryParam("clonedApplicationName") String clonedApplicationName,
      @QueryParam("clonedApplicationPublicId") String clonedApplicationPublicId)
  {
    return applicationCloneService.cloneApplication(sourceApplicationId, clonedApplicationName,
        clonedApplicationPublicId);
  }
}
