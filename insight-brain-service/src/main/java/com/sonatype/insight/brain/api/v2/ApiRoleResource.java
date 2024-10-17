/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleListDTO;
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
    description = "Roles provide sets of permissions that grant access to the functionality in the user interface," +
        "through integrations, and when using REST APIs." +
        "\n" +
        "\n" +
        "Permissions are granted by assigning users or groups to the " +
        "system roles or at the various levels in the organizational hierarchy: root organization, " +
        "repository managers, and applications and organizations." +
        "\n" +
        "\n" +
        "Use this REST API to retrieve role IDs, role names and descriptions.")
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
}
