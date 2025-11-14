/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

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

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleListDTO;
import com.sonatype.insight.brain.api.v2.service.ApiRoleAdapter;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.security.RoleDTO;
import com.sonatype.insight.brain.security.RoleService;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.70
 */
@Named
@Timed
@Tag(name = "Roles",
    description = "Roles provide sets of permissions that grant access to the functionality in the user interface, " +
        "through integrations, and when using REST APIs." +
        "\n" +
        "\n" +
        "Permissions are granted by assigning users or groups to the " +
        "system roles or at the various levels in the organizational hierarchy: root organization, " +
        "repository managers, and applications and organizations." +
        "\n" +
        "\n" +
        "Use this REST API to manage roles.")
@Path(value = PublicApiPaths.ROLE_RESOURCE_PATH_V2)
public class ApiRoleResource
{
  private RoleService roleService;

  @Inject
  public ApiRoleResource(RoleService roleService) {
    this.roleService = roleService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to view the role IDs, role names and descriptions." +
      "\n" +
      "\n" +
      "Permissions required: View All Roles")
  @ApiResponse(responseCode = "200",
      description = "The response contains the role IDs, role names and descriptions.",
      useReturnTypeSchema = true)
  public ApiRoleListDTO getRoles() {
    return roleService.getRolesAsApiRoleListDTO();
  }

  @Path("{roleId}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve details for a specific role, including its permissions." +
      "\n" +
      "\n" +
      "Permissions required: View All Roles")
  @ApiResponse(responseCode = "200",
      description = "The response contains the role details including permissions.",
      useReturnTypeSchema = true)
  public ApiRoleDTO getRoleById(@PathParam("roleId") final String roleId) {
    RoleDTO roleDTO = roleService.getRoleById(roleId);
    return ApiRoleAdapter.convertToDTO(roleDTO);
  }

  @Path("new")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve a template for creating a new custom role, " +
      "including all available permissions that can be assigned." +
      "\n" +
      "\n" +
      "Permissions required: Edit Roles")
  @ApiResponse(responseCode = "200",
      description = "The response contains a role template with available permissions.",
      useReturnTypeSchema = true)
  public ApiRoleDTO getTemplateForNewRole() {
    RoleDTO roleDTO = roleService.getTemplateForNewRole();
    return ApiRoleAdapter.convertToDTO(roleDTO);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_ROLE)
  @Operation(description = "Use this method to create a new custom role with specified permissions." +
      "\n" +
      "\n" +
      "Permissions required: Edit Roles")
  @ApiResponse(responseCode = "200",
      description = "The role was created successfully. The response contains the created role details.",
      useReturnTypeSchema = true)
  public ApiRoleDTO addRole(final ApiRoleDTO apiRoleDTO) {
    RoleDTO roleDTO = ApiRoleAdapter.convertFromDTO(apiRoleDTO);
    RoleDTO createdRole = roleService.addRole(roleDTO);
    return ApiRoleAdapter.convertToDTO(createdRole);
  }

  @Path("{roleId}")
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_ROLE)
  @Operation(description = "Use this method to update an existing custom role and its permissions." +
      "\n" +
      "\n" +
      "Permissions required: Edit Roles")
  @ApiResponse(responseCode = "200",
      description = "The role was updated successfully. The response contains the updated role details.",
      useReturnTypeSchema = true)
  public ApiRoleDTO updateRole(@PathParam("roleId") final String roleId, final ApiRoleDTO apiRoleDTO) {
    RoleDTO roleDTO = ApiRoleAdapter.convertFromDTO(apiRoleDTO);
    RoleDTO updatedRole = roleService.updateRole(roleDTO);
    return ApiRoleAdapter.convertToDTO(updatedRole);
  }

  @DELETE
  @Path("{roleId}")
  @Audited(AuditEvent.DELETE_ROLE)
  @Operation(description = "Use this method to delete a custom role." +
      "\n" +
      "\n" +
      "Permissions required: Edit Roles")
  @ApiResponse(responseCode = "204",
      description = "The role was deleted successfully.")
  public void deleteRole(@PathParam("roleId") final String roleId) {
    roleService.deleteRole(roleId);
  }
}
