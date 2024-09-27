/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.List;

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

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.15.0
 */
@Path(RoleResource.RESOURCE_PATH)
@Named
@Timed
public class RoleResource
{
  public static final String RESOURCE_PATH = "rest/security/roles";

  public static final String ROLE_ID_PATH = "{roleId}";

  public static final String NEW_PATH = "new";

  private final RoleService roleService;

  @Inject
  public RoleResource(final RoleService roleService) {
    this.roleService = roleService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<RoleDTO> getAllRoles() {
    return roleService.getAllRoles();
  }

  @Path(ROLE_ID_PATH)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public RoleDTO getRoleById(@PathParam("roleId") final String roleId) {
    return roleService.getRoleById(roleId);
  }

  @Path(NEW_PATH)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public RoleDTO getTemplateForNewRole() {
    return roleService.getTemplateForNewRole();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_ROLE)
  public RoleDTO addRole(RoleDTO role) {
    return roleService.addRole(role);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_ROLE)
  public RoleDTO updateRole(RoleDTO role) {
    return roleService.updateRole(role);
  }

  @DELETE
  @Path(ROLE_ID_PATH)
  @Audited(AuditEvent.DELETE_ROLE)
  public void deleteRole(@PathParam("roleId") String roleId) {
    roleService.deleteRole(roleId);
  }
}
