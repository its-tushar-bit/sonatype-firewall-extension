/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.model.security.Role;

/**
 * @since 1.15.0
 */
@Path(RoleResource.SERVICE_PATH)
@Named
public class RoleResource
{
  public static final String SERVICE_PATH = "rest/security/roles";

  private final RoleService roleService;

  @Inject
  public RoleResource(final RoleService roleService) {
    this.roleService = roleService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<Role> getAllRoles() {
    return roleService.getAllRoles();
  }
}
