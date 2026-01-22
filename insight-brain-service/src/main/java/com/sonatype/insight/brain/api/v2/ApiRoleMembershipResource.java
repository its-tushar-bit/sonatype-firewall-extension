/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicableMembershipMappingsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiMemberWithDetailsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingListDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.security.ApplicableMembershipMappings;
import com.sonatype.insight.brain.security.Member;
import com.sonatype.insight.brain.security.MembershipMappingService;
import com.sonatype.insight.brain.utils.IdUtils;

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
  private final MembershipMappingService membershipMappingService;

  private final ApiBulkMembershipMappingAdapter bulkAdapter;

  private final IdUtils idUtils;

  public static final String APPLICATION_OR_ORGANIZATION = "{ownerType: application|organization}/{internalOwnerId}" +
      "/role/{roleId}/{memberType: (?i:user|group)}/{memberName}";

  public static final String NON_GLOBAL_OWNER_TYPES =
      "{ownerType: application|organization|repository_manager|repository}/{internalOwnerId}";

  static final String GLOBAL_OR_REPOSITORY_CONTAINER =
      "{ownerType: global|repository_container}/role/{roleId}/{memberType: (?i:user|group)}/{memberName}";

  @Inject
  public ApiRoleMembershipResource(
      final MembershipMappingService membershipMappingService,
      final ApiBulkMembershipMappingAdapter bulkAdapter,
      final IdUtils idUtils)
  {
    this.membershipMappingService = membershipMappingService;
    this.bulkAdapter = bulkAdapter;
    this.idUtils = idUtils;
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
      responseCode = "204",
      description = "The specified roleId has been has been granted to the user or user group for the requested " +
          "context."
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
      @PathParam("memberName") String memberName,
      @Parameter(description = "If true, attempts to validate if the specified user or group exists " +
          "before assigning the role.\n" +
          "If false or omitted, the request behaves as before (no validation check).")
      @QueryParam("validateMember") @DefaultValue("false") boolean validateMember)
  {
    membershipMappingService.grantRoleMembership(ownerType, internalOwnerId, roleId, memberType, memberName,
        validateMember);
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
      responseCode = "204",
      description =
          "The specified role has been granted to the users or user groups on the given context."
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
      description = "The specified role has been revoked from the user or user group"
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
      description = "The specified role has been revoked from the user or user group."
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

  /**
   * Retrieve all role memberships with full member details and role metadata.
   *
   * @since 1.197.0
   */
  @GET
  @Path(NON_GLOBAL_OWNER_TYPES + "/roles")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = """
      Use this method to retrieve all role memberships with full details including role names, descriptions, and \
      member information organized by owner (for inheritance display).

      Permissions required: Edit system configuration and users for a global context or view IQ elements for a \
      non-global context""")
  @ApiResponse(
      responseCode = "200",
      description = """
          The response contains all roles with their members organized by owner, including inherited members from \
          parent organizations or repository hierarchies. Also includes a flag indicating whether group search is 
          enabled.""",
      useReturnTypeSchema = true
  )
  public ApiApplicableMembershipMappingsDTO getBulkRoleMembershipsNonGlobal(
      @Parameter(description = "Enter the ownerType for which you want to retrieve role memberships.",
          required = true)
      @PathParam("ownerType") final OwnerType ownerType,
      @Parameter(description = "Enter the corresponding id for the ownerType specified above. " +
          "For applications, use the public ID. For organizations, repositories, and repository managers, use the " +
          "internal ID.",
          required = true)
      @PathParam("internalOwnerId") final String ownerIdOrPublicId)
  {
    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerIdOrPublicId);
    ApplicableMembershipMappings internal = membershipMappingService.getApplicableMembershipMappings(ownerType,
        internalOwnerId);
    return bulkAdapter.toApiDTO(internal);
  }

  /**
   * Retrieve all role memberships for global or repository_container context.
   *
   * @since 1.197.0
   */
  @GET
  @Path("{ownerType: global|repository_container}/roles")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = """
      Use this method to retrieve all role memberships for global or repository container context with full details \
      including role names, descriptions, and member information.

      Permissions required: Edit system configuration and users for a global context or view IQ elements for a \
      non-global context""")
  @ApiResponse(
      responseCode = "200",
      description = """
          The response contains all roles with their members. Also includes a flag indicating whether group search \
          is enabled.""",
      useReturnTypeSchema = true
  )
  public ApiApplicableMembershipMappingsDTO getBulkRoleMembershipsGlobalOrRepositoryContainer(
      @Parameter(description = "Enter the value for ownerType.", required = true)
      @PathParam("ownerType") final OwnerType ownerType)
  {
    String contextId = OwnerType.REPOSITORY_CONTAINER.equals(ownerType)
        ? RepositoryContainer.REPOSITORY_CONTAINER_ID
        : null;
    ApplicableMembershipMappings internal = membershipMappingService.getApplicableMembershipMappings(ownerType,
        contextId);
    return bulkAdapter.toApiDTO(internal);
  }

  /**
   * Set all members for a specific role in one atomic operation.
   * This replaces all existing members for the role with the provided list.
   *
   * @since 1.197.0
   */
  @PUT
  @Path(NON_GLOBAL_OWNER_TYPES + "/role/{roleId}/members")
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_ROLE_MEMBERSHIP)
  @Operation(description = """
      Use this method to set all members for a specific role. This operation atomically replaces all existing \
      members for the role with the provided list.

      Permissions required: Edit access control""")
  @ApiResponse(
      responseCode = "204",
      description = "The role membership has been successfully updated with the provided members."
  )
  public void setBulkRoleMembersNonGlobal(
      @Parameter(description = "Enter the ownerType for which you want to set role members.",
          required = true)
      @PathParam("ownerType") final OwnerType ownerType,
      @Parameter(description = "Enter the id associated with the ownerType specified above. " +
          "For applications, use the public ID. For organizations, repositories, and repository managers, use the " +
          "internal ID.",
          required = true)
      @PathParam("internalOwnerId") final String ownerIdOrPublicId,
      @Parameter(description = """
          Enter the roleId for the role whose members should be set.

          Use the Roles REST API for roleIds and descriptions.""", required = true)
      @PathParam("roleId") final String roleId,
      @Parameter(description = "List of members to assign to this role. Provide an empty list to remove all members.",
          required = true)
      final List<ApiMemberWithDetailsDTO> members)
  {
    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerIdOrPublicId);
    List<Member> internalMembers = bulkAdapter.toInternalMembers(members);
    Map<String, List<Member>> membersByRoleId = new HashMap<>();
    membersByRoleId.put(roleId, internalMembers);
    membershipMappingService.setMembershipMappings(ownerType, internalOwnerId, membersByRoleId);
  }

  /**
   * Set all members for a specific role in global or repository_container context.
   *
   * @since 1.197.0
   */
  @PUT
  @Path("{ownerType: global|repository_container}/role/{roleId}/members")
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_ROLE_MEMBERSHIP)
  @Operation(description = """
      Use this method to set all members for a specific role in the global or repository container context. This \
      operation atomically replaces all existing members for the role with the provided list.

      Permissions required: Edit system configuration and users for a global context or edit access control for a \
      non-global context""")
  @ApiResponse(
      responseCode = "204",
      description = "The role membership has been successfully updated with the provided members."
  )
  public void setBulkRoleMembersGlobalOrRepositoryContainer(
      @Parameter(description = "Enter the ownerType.",
          required = true)
      @PathParam("ownerType") final OwnerType ownerType,
      @Parameter(description = """
          Enter the roleId for the role whose members should be set.

          Use the Roles REST API for roleIds and descriptions.""", required = true)
      @PathParam("roleId") final String roleId,
      @Parameter(description = "List of members to assign to this role. Provide an empty list to remove all members.",
          required = true)
      final List<ApiMemberWithDetailsDTO> members)
  {
    List<Member> internalMembers = bulkAdapter.toInternalMembers(members);
    Map<String, List<Member>> membersByRoleId = new HashMap<>();
    membersByRoleId.put(roleId, internalMembers);

    String contextId = OwnerType.REPOSITORY_CONTAINER.equals(ownerType)
        ? RepositoryContainer.REPOSITORY_CONTAINER_ID
        : null;
    membershipMappingService.setMembershipMappings(ownerType, contextId, membersByRoleId);
  }
}
