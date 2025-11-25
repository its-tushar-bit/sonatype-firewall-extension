/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingException;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
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
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.UserTokenService;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.76
 */
@Named
@Timed
@Path(PublicApiPaths.USER_TOKEN_RESOURCE_PATH_V2)
@Tag(name = "User Tokens",
    description = "Use this REST API to manage user tokens.")
public class ApiUserTokenResource
{
  private final UserTokenService userTokenService;

  public static final String CURRENT_USER = "currentUser";

  public static final String CURRENT_USER_HAS_TOKEN = CURRENT_USER + "/hasToken";

  public static final String USER_CODE = "userCode/{userCode}";

  public static final String USERNAME = "{username}";

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
  @Operation(description = "Use this method to check if a user token has been issued to the logged in user." +
      "\n" +
      "\n" +
      "Permissions required: None ",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response contains a `userTokenExists` field.",
              useReturnTypeSchema = true)
      }
  )
  public ApiUserTokenExistsDTO getUserTokenExistsForCurrentUser() {
    return userTokenService.userTokenExistsForCurrentUser();
  }

  /**
   * @since 1.198
   */
  @GET
  @Path(CURRENT_USER + "/createTime")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve the creation time of the user token" +
      " for the currently logged in user." +
      "\n" +
      "\n" +
      "Permissions required: None",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response contains the token creation time in ISO format.",
              useReturnTypeSchema = true),
          @ApiResponse(responseCode = "404",
              description = "User token does not exist for the current user.")
      })
  public Date getCurrentUserTokenCreateTime() {
    return userTokenService.getCurrentUserTokenCreateTime();
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
  @Operation(description = "Use this method to retrieve user tokens created within a date range, in the supported " +
      "IQ Server realms." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response contains a list of user tokens, each containing a `userCode`, `username` " +
                  "and the name of the IQ server `realm`.",
              useReturnTypeSchema = true)
      })
  public List<ApiUserTokenDTO> getUserTokensByCreatedBetweenAndRealmId(
      @Parameter(description = "Enter the start date for the date range in `yyyy-mm-dd` format.")
      @QueryParam("createdAfter") String createdAfter,
      @Parameter(description = "Enter the end date for the date range in `yyyy-mm-dd` format.")
      @QueryParam("createdBefore") String createdBefore,
      @Parameter(description = "Enter the `realmId`. Possible values are `Internal`, `SAML` , `OAUTH2`, and " +
          "`Crowd`.")
      @DefaultValue(User.INTERNAL_REALM_ID) @QueryParam("realm") String realmId)
  {
    return userTokenService.getUserTokensCreatedBetweenAndRealmId(createdAfter, createdBefore, realmId);
  }

  /**
   * @since 1.133
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(USERNAME)
  @Operation(description = "Use this method to retrieve a user token by specifying a username and realmId." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response contains the `userCode`, `username` " +
                  "and the name of the IQ server `realm`.",
              useReturnTypeSchema = true)
      })
  public ApiUserTokenDTO getUserTokenByUsernameAndRealmId(
      @Parameter(description = "Enter the username.", required = true)
      @PathParam("username") String username,
      @Parameter(description = "Enter the realmId. Possible values are `Internal`, `SAML` , `OAUTH2` , and " +
          "`Crowd`.")
      @QueryParam("realm") @DefaultValue("Internal") String realmId)
  {
    return userTokenService.getUserTokenByUsernameAndRealmId(username, realmId);
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path(CURRENT_USER)
  @Audited(AuditEvent.CREATE_USER_TOKEN)
  @Operation(description = "Use this method to generate a user token for the currently logged in user." +
      "\n" +
      "\n" +
      "Permissions required: None",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response contains the generated user token consisting of `userCode`, `username` " +
                  "`passCode`, and the IQ Server `realm`.",
              useReturnTypeSchema = true)
      })
  public ApiUserTokenDTO createUserToken() {
    return userTokenService.createUserToken();
  }

  @DELETE
  @Path(PURGE)
  @Audited(AuditEvent.PURGE_USER_TOKENS)
  @Operation(description = "Use this method to delete all existing LDAP user tokens." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
          @ApiResponse(responseCode = "204",
              description = "User tokens purged successfully.")
      })
  public void purgeUserTokens() throws NamingException {
    userTokenService.purgeUserTokens();
  }

  @DELETE
  @Path(CURRENT_USER)
  @Audited(AuditEvent.DELETE_USER_TOKEN)
  @Operation(description = "Use this method to delete an existing user token for the currently logged in user." +
      "\n" +
      "\n" +
      "Permissions required: None",
      responses = {
          @ApiResponse(responseCode = "204",
              description = "User token deleted successfully.")
      })
  public void deleteCurrentUserToken() {
    userTokenService.deleteCurrentUserToken();
  }

  /**
   * @since 1.87
   */
  @DELETE
  @Path(USER_CODE)
  @Audited(AuditEvent.DELETE_USER_TOKEN)
  @Operation(description = "Use this method to delete an existing user token by specifying the userCode." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
          @ApiResponse(responseCode = "204",
              description = "User token deleted successfully.")
      })
  public void deleteUserTokenByUserCode(
      @Parameter(description = "Enter the `userCode` to be deleted.")
      @PathParam("userCode") String userCode)
  {
    userTokenService.deleteUserTokenByUserCode(userCode);
  }
}
