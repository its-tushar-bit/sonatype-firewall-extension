/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.UnlicensedPath;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.shiro.SecurityUtils;

@Path(PermissionResource.SERVICE_PATH)
@UnlicensedPath
@Named
public class PermissionResource
{
  public static final String SERVICE_PATH = "/rest/user/permissions/{ownerType: global|application|organization}/{ownerId}";

  private final PermissionService permissionService;

  @Inject
  public PermissionResource(final PermissionService permissionService) {
    this.permissionService = permissionService;
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Set<Permission> validatePermission(@PathParam("ownerType") final String ownerType,
                                            @PathParam("ownerId") final String ownerId, Set<Permission> permissions)
  {
    if (permissions == null || permissions.isEmpty()) {
      throw new BadRequestException("Must specify permissions to check.");
    }

    return permissionService.hasPermissions(SecurityUtils.getSubject(), ownerType, ownerId, permissions);
  }
}
