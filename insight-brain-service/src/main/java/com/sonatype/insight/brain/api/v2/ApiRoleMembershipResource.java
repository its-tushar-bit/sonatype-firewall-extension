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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.70
 */
@Named
@Timed
@Tag(name = "Role Memberships",
    description = "Use this REST API to manage authorizations for users or user groups." +
        "\n" +
        "\n" +
        "You can view existing role assignments and " +
        "grant or revoke user authorization on organizations, applications and repositories.")
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
  @Operation(description = "Use this method to grant a role to a user or user group for the specified application or " +
      "organization." +
      "\n" +
      "\n" +
      "Permissions required: Edit access control")
  @ApiResponse(
      responseCode = "200",
      description = "The specified roleId has been has been granted to the user or user group for the requested " +
          "context.",
      useReturnTypeSchema = true
  )
  public void grantRoleMembershipApplicationOrOrganization(
      @Parameter(description = "Enter the value for the ownerType for which you want to grant the role.",
          required = true)
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the value for the internalId associated with the ownerType specified above.",
          required = true)
      @PathParam("internalOwnerId") String internalOwnerId,
      @Parameter(description = "Enter the roleId for the role to be granted." +
          "\n" +
          "\n" +
          "Use the Roles REST API for roleIds and descriptions.", required = true)
      @PathParam("roleId") String roleId,
      @Parameter(description = "Enter the value for memberType, to specify a user or a user group.", required = true)
      @PathParam("memberType") MemberType memberType,
      @Parameter(description = "Enter the value for memberName. This can be a username or group name depending upon " +
          "the value of memberType above.", required = true)
      @PathParam("memberName") String memberName)
  {
    membershipMappingService.grantRoleMembership(ownerType, internalOwnerId, roleId, memberType, memberName);
  }

  @PUT
  @Path(GLOBAL_OR_REPOSITORY_CONTAINER)
  @Audited(AuditEvent.GRANT_ROLE_MEMBERSHIP)
  @Operation(description = "Use this method to grant a role to a user or user group globally or on all " +
      "repositories." +
      "\n" +
      "\n" +
      "Permissions required: Edit system configuration and users for a global context or edit access control for a " +
      "non-global context")
  @ApiResponse(
      responseCode = "200",
      description =
          "The specified role has been granted to the users or user groups on the given context.",
      useReturnTypeSchema = true
  )
  public void grantRoleMembershipGlobalOrRepositoryContainer(
      @Parameter(description = "Enter the value for the ownerType for which you want to grant the role.",
          required = true)
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the roleId for the role to be granted." +
          "\n" +
          "\n" +
          "Use the Roles REST API for roleIds and descriptions.", required = true)
      @PathParam("roleId") String roleId,
      @Parameter(description = "Enter the value for memberType, to specify a user or a user group.", required = true)
      @PathParam("memberType") MemberType memberType,
      @Parameter(description = "Enter the value for memberName. This can be a username or group name depending upon " +
          "the value of memberType above.", required = true)
      @PathParam("memberName") String memberName)
  {
    membershipMappingService.grantRoleMembership(ownerType, null, roleId, memberType, memberName);
  }

  @GET
  @Path("{ownerType: application|organization}/{internalOwnerId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve the users, user groups and the corresponding role Ids." +
      "\n" +
      "\n" +
      "Permissions required: Edit system configuration and users for a global context or view IQ elements for a " +
      "non-global context")
  @ApiResponse(
      responseCode = "200",
      description = "The response contains the assigned role Ids, users and user groups for the application or " +
          "organization requested. It also includes members who inherit a role based on the " +
          "organization hierarchy.", useReturnTypeSchema = true)

  public ApiRoleMemberMappingListDTO getRoleMembershipsApplicationOrOrganization(
      @Parameter(description = "Enter the ownerType for which you want to retrieve users and their role Ids.",
          required = true)
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the corresponding id for the ownerType specified above.",
          required = true)
      @PathParam("internalOwnerId") String internalOwnerId)
  {
    return membershipMappingService.getRoleMembershipsOmitEmpty(ownerType, internalOwnerId);
  }

  @GET
  @Path("{ownerType: global|repository_container}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve all users and roles globally or for all repositories." +
      "\n" +
      "\n" +
      "Permissions required: Edit system configuration and users for a global context or view IQ elements for a " +
      "non-global context")
  @ApiResponse(
      responseCode = "200",
      description =
          "The response contains all role Ids and the corresponding users/user groups assigned to them, for " +
              "the ownerType specified. It also includes members who inherit a role based on the organization" +
              " hierarchy.",
      useReturnTypeSchema = true
  )
  public ApiRoleMemberMappingListDTO getRoleMembershipsGlobalOrRepositoryContainer(
      @Parameter(description = "Enter the value for ownerType. Using `global` will return the users and groups who " +
          "have been assigned the administrator role.", required = true)
      @PathParam("ownerType") OwnerType ownerType)
  {
    return membershipMappingService.getRoleMembershipsOmitEmpty(ownerType, null);
  }

  @DELETE
  @Path(APPLICATION_OR_ORGANIZATION)
  @Audited(AuditEvent.REVOKE_ROLE_MEMBERSHIP)
  @Operation(description = "Use this method to revoke a role from a user or user group, on a specific application or " +
      "organization." +
      "\n" +
      "\n" +
      "Permissions required: Edit access control")
  @ApiResponse(
      responseCode = "204",
      description = "The specified role has been revoked from the user or user group",
      useReturnTypeSchema = true
  )
  public void revokeRoleMembershipApplicationOrOrganization(
      @Parameter(description = "Enter the value for the ownerType for which you want to revoke the role. Using " +
          "`global` will revoke the administrator role.", required = true)
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the internalId associated with the ownerType specified above.", required = true)
      @PathParam("internalOwnerId") String internalOwnerId,
      @Parameter(description = "Enter the roleId for the role to be revoked.", required = true)
      @PathParam("roleId") String roleId,
      @Parameter(description = "Enter the value for memberType, to specify a user or a user group.", required = true)
      @PathParam("memberType") MemberType memberType,
      @Parameter(description = "Enter the value for memberName. This can be a username or group name depending upon " +
          "the value of memberType above.", required = true)
      @PathParam("memberName") String memberName)
  {
    membershipMappingService.revokeRoleMembership(ownerType, internalOwnerId, roleId, memberType, memberName);
  }

  @DELETE
  @Path(GLOBAL_OR_REPOSITORY_CONTAINER)
  @Audited(AuditEvent.REVOKE_ROLE_MEMBERSHIP)
  @Operation(description = "Use this method to revoke roles globally or on all repositories." +
      "\n" +
      "\n" +
      "Permissions required: Edit system configuration and users for a global context or view IQ elements for a " +
      "non-global context")
  @ApiResponse(
      responseCode = "204",
      description = "The specified role has been revoked from the user or user group.",
      useReturnTypeSchema = true
  )
  public void revokeRoleMembershipGlobalOrRepositoryContainer(
      @Parameter(description = "Enter the value for ownerType. Using " +
          "`global` will revoke the administrator role.", required = true)
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the roleId for the role to be revoked.", required = true)
      @PathParam("roleId") String roleId,
      @Parameter(description = "Enter the value for memberType, to specify a user or a user group.", required = true)
      @PathParam("memberType") MemberType memberType,
      @Parameter(description = "Enter the value for memberName. This can be a username or group name depending upon " +
          "the value of memberType above.", required = true)
      @PathParam("memberName") String memberName)
  {
    membershipMappingService.revokeRoleMembership(ownerType, null, roleId, memberType, memberName);
  }
}
