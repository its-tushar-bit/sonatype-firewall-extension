/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @since 1.15.0
 *
 * TODO - Remove this after UI refactored to use RoleResource
 */
@Path(RolePermissionResource.SERVICE_PATH)
@Named
public class RolePermissionResource
{
  public static final String SERVICE_PATH = "rest/security/permissions";

  private final RolePermissionService rolePermissionService;

  @Inject
  public RolePermissionResource(final RolePermissionService rolePermissionService) {
    this.rolePermissionService = rolePermissionService;
  }

  @Path("{roleId}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public RolePermissionDTO getPermissionsForRole(@PathParam("roleId") final String roleId) {
    return rolePermissionService.getPermissionsForRole(roleId);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public RolePermissionDTO getPermissionsForNewCustomRole() {
    return rolePermissionService.getPermissionsForNewCustomRole();
  }
}
