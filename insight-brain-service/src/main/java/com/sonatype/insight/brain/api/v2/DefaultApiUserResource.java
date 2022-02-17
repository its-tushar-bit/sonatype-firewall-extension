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
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiUserDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiUserListDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.UserService;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.70
 */
@Named
@Timed
@Path(value = PublicApiPaths.USER_RESOURCE_PATH_V2)
public class DefaultApiUserResource implements ApiUserResource
{
  public static final String USERNAME_PATH = "{username}";

  private final UserService userService;

  @Inject
  public DefaultApiUserResource(UserService userService) {
    this.userService = userService;
  }

  @Override
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApiUserListDTO getAll(
      @DefaultValue(User.INTERNAL_REALM_ID)
      @QueryParam("realm") String realmId )
  {
    return userService.getAllApiUserDTOs(realmId);
  }

  @Override
  @GET
  @Path(USERNAME_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiUserDTO get(@PathParam("username") String username,
                        @DefaultValue(User.INTERNAL_REALM_ID)
                        @QueryParam("realm") String realmId)
  {
    return userService.getApiUserDTOByUsernameAndRealmId(username, realmId);
  }

  @Override
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_USER)
  public void add(ApiUserDTO userDTO) {
    userService.addUser(userDTO);
  }

  @Override
  @PUT
  @Path(USERNAME_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_USER)
  public ApiUserDTO update(@PathParam("username") String username, ApiUserDTO userDTO) {
    return userService.updateUser(username, userDTO);
  }

  @Override
  @DELETE
  @Path(USERNAME_PATH)
  @Audited(AuditEvent.DELETE_USER)
  public void delete(
      @PathParam("username") String username,
      @QueryParam("realm") @DefaultValue("Internal") String realmId)
  {
    userService.deleteUserByRealmIdAndUsername(realmId, username);
  }
}
