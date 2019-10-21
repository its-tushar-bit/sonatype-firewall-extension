/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiUserTokenDTO;
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

  @Inject
  public ApiUserTokenResource(UserTokenService userTokenService) {
    this.userTokenService = userTokenService;
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path(CURRENT_USER)
  public ApiUserTokenDTO createUserToken() {
    return userTokenService.createUserToken();
  }
}
