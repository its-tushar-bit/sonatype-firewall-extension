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

/**
 * @since 1.70
 */
@Named
@Timed
@Path(value = PublicApiPaths.ROLE_RESOURCE_PATH_V2)
public class DefaultApiRoleResource implements ApiRoleResource
{
  private RoleService roleService;

  @Inject
  public DefaultApiRoleResource(RoleService roleService) {
    this.roleService = roleService;
  }

  @Override
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApiRoleListDTO getRoles() {
    return roleService.getRolesAsApiRoleListDTO();
  }
}
