/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.UnlicensedPath;
import com.sonatype.insight.error.exception.BadRequestException;

import com.codahale.metrics.annotation.Timed;
import org.apache.shiro.SecurityUtils;

@Path(PermissionResource.RESOURCE_PATH)
@UnlicensedPath
@Named
@Timed
public class PermissionResource
{
  public static final String RESOURCE_PATH = "rest/user/permissions";

  public static final String OWNER_CONTEXT_PATH =
      "{ownerType: global|application|organization|repository|repository_manager}/{ownerId}";

  public static final String PUBLIC_APPLICATION_ID_PATH = "application/publicId/{publicApplicationId}";

  public static final String SINGLETON_OWNER_CONTEXT_PATH = "{ownerType: repository_container}";

  private final PermissionService permissionService;

  @Inject
  public PermissionResource(final PermissionService permissionService) {
    this.permissionService = permissionService;
  }

  @PUT
  @Path(OWNER_CONTEXT_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Set<Permission> validatePermission(
      @PathParam("ownerType") final OwnerType ownerType,
      @PathParam("ownerId") final String ownerId,
      Set<Permission> permissions)
  {
    if (permissions == null || permissions.isEmpty()) {
      throw new BadRequestException("Must specify permissions to check.");
    }

    return permissionService.validatePermission(SecurityUtils.getSubject(), ownerType, ownerId, permissions);
  }

  @PUT
  @Path(PUBLIC_APPLICATION_ID_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Set<Permission> validatePermissionForPublicApplicationId(
      @PathParam("publicApplicationId") final String publicApplicationId,
      Set<Permission> permissions)
  {
    return permissionService.validatePermissionForPublicApplicationId(SecurityUtils.getSubject(), publicApplicationId,
        permissions);
  }

  @PUT
  @Path(SINGLETON_OWNER_CONTEXT_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Set<Permission> validatePermission(
      @PathParam("ownerType") final OwnerType ownerType,
      Set<Permission> permissions)
  {
    return validatePermission(ownerType, RepositoryContainer.REPOSITORY_CONTAINER_ID, permissions);
  }
}
