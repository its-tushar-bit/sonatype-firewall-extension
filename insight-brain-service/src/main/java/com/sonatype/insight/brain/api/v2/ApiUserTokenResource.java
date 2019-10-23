/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiUserTokenDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.security.UserTokenService;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.76
 */
@Named
@Timed
@Path(PublicApiPaths.USER_TOKEN_RESOURCE_PATH_V2)
public class ApiUserTokenResource
{
  private final UserTokenService userTokenService;

  public static final String CURRENT_USER = "currentUser";

  private static final String USERS = "users";

  public static final String DELETE_BY_USERNAME = USERS + "/{username}";
  
  @Inject
  public ApiUserTokenResource(UserTokenService userTokenService) {
    this.userTokenService = userTokenService;
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path(CURRENT_USER)
  @Audited(AuditEvent.CREATE_USER_TOKEN)
  public ApiUserTokenDTO createUserToken() {
    return userTokenService.createUserToken();
  }

  @DELETE
  @Path(DELETE_BY_USERNAME)
  @Audited(AuditEvent.DELETE_USER_TOKEN)
  public void deleteUserToken(@PathParam("username") String username) {
    userTokenService.deleteUserToken(username);
  }

  @DELETE
  @Path(CURRENT_USER)
  @Audited(AuditEvent.DELETE_USER_TOKEN)
  public void deleteCurrentUserToken() {
    userTokenService.deleteCurrentUserToken();
  }
}
