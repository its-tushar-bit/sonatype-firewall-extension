/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiUserTokenDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiUserTokenExistsDTO;
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

  public static final String CURRENT_USER_HAS_TOKEN = CURRENT_USER + "/hasToken";

  public static final String USER_CODE = "userCode/{userCode}";

  public static final String PURGE = "purge";
  
  @Inject
  public ApiUserTokenResource(UserTokenService userTokenService) {
    this.userTokenService = userTokenService;
  }

  /**
   * @since 1.102
   */
  @GET
  @Path(CURRENT_USER_HAS_TOKEN)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiUserTokenExistsDTO getUserTokenExistsForCurrentUser() {
    return userTokenService.userTokenExistsForCurrentUser();
  }

  /**
   * Only returns ApiUserTokenDTO#userCode populated - passCode is not returned.
   *
   * @param createdAfter  Expected format: yyyy-MM-dd (For example: 2019-09-03)
   * @param createdBefore Expected format: yyyy-MM-dd (For example: 2019-09-03)
   * @since 1.87
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<ApiUserTokenDTO> getUserTokensCreatedBetween(
      @QueryParam("createdAfter") String createdAfter,
      @QueryParam("createdBefore") String createdBefore)
  {
    return userTokenService.getUserTokensCreatedBetween(createdAfter, createdBefore);
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path(CURRENT_USER)
  @Audited(AuditEvent.CREATE_USER_TOKEN)
  public ApiUserTokenDTO createUserToken() {
    return userTokenService.createUserToken();
  }

  @DELETE
  @Path(PURGE)
  @Audited(AuditEvent.PURGE_USER_TOKENS)
  public void purgeUserTokens() throws NamingException {
    userTokenService.purgeUserTokens();
  }

  @DELETE
  @Path(CURRENT_USER)
  @Audited(AuditEvent.DELETE_USER_TOKEN)
  public void deleteCurrentUserToken() {
    userTokenService.deleteCurrentUserToken();
  }

  /**
   * @since 1.87
   */
  @DELETE
  @Path(USER_CODE)
  @Audited(AuditEvent.DELETE_USER_TOKEN)
  public void deleteUserTokenByUserCode(@PathParam("userCode") String userCode) {
    userTokenService.deleteUserTokenByUserCode(userCode);
  }
}
