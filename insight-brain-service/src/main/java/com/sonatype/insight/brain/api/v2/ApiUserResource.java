/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiUserDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiUserListDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.UserService;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.70
 */
@Named
@Timed
@Path(value = PublicApiPaths.USER_RESOURCE_PATH_V2)
@Tag(name = "Users",
    description = "Use this REST API to manage users.")
public class ApiUserResource
{
  public static final String USERNAME_PATH = "{username}";

  private final UserService userService;

  @Inject
  public ApiUserResource(UserService userService) {
    this.userService = userService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve user details for all users." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains user details. Passwords are excluded for security.",
            useReturnTypeSchema = true)
      })
  public ApiUserListDTO getAll(
      @Parameter(
          description = "Enter the `realm`. Allowed values are `Internal`,`OAUTH2`, and `SAML`.") @DefaultValue(User.INTERNAL_REALM_ID) @QueryParam("realm") String realmId)
  {
    return userService.getAllApiUserDTOs(realmId);
  }

  @GET
  @Path(USERNAME_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve user details for the specified user." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains details for the specified user.",
            useReturnTypeSchema = true)
      })
  public ApiUserDTO get(
      @Parameter(description = "Enter the username.", required = true) @PathParam("username") String username,
      @Parameter(description = "Enter the `realm`. Allowed values are `Internal`,`OAUTH2`, and " +
          "`SAML`.") @DefaultValue(User.INTERNAL_REALM_ID) @QueryParam("realm") String realmId)
  {
    return userService.getApiUserDTOByUsernameAndRealmId(username, realmId);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_USER)
  @Operation(description = "Use this method to create a new user." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
        @ApiResponse(responseCode = "204",
            description = "User created successfully.")
      })
  public void add(
      @RequestBody(description = "Specify the user details for the new user to be created. All fields " +
          "except `realm` are required.") ApiUserDTO userDTO)
  {
    userService.addUser(userDTO);
  }

  @PUT
  @Path(USERNAME_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_USER)
  @Operation(description = "Use this method to update user details for an existing internal user, by specifying " +
      "the username." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "User details updated successfully.",
            useReturnTypeSchema = true)
      })
  public ApiUserDTO update(
      @Parameter(description = "Enter the username.") @PathParam("username") String username,
      @RequestBody(description = "Specify the user details to be updated. Any unspecified field will remain " +
          "unchanged. Username, password, and realm cannot be updated.") ApiUserDTO userDTO)
  {
    return userService.updateUser(username, userDTO);
  }

  @DELETE
  @Path(USERNAME_PATH)
  @Audited(AuditEvent.DELETE_USER)
  @Operation(description = "Use this method to delete an existing user." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
        @ApiResponse(responseCode = "204",
            description = "User deleted successfully.")
      })
  public void delete(
      @Parameter(description = "Enter the username to be deleted.",
          required = true) @PathParam("username") String username,
      @Parameter(
          description = "Enter the `realm`. Allowed values are `Internal`,`OAUTH2`, and `SAML`.") @QueryParam("realm") @DefaultValue("Internal") String realmId)
  {
    userService.deleteUserByRealmIdAndUsername(realmId, username);
  }
}
