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
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.UserService.ChangePasswordDTO;
import com.sonatype.insight.brain.security.UserService.FindMembersDTO;

/**
 * @since 1.7
 */
@Named
@Path(UserResource.SERVICE_PATH)
public class UserResource
{
  public static final String SERVICE_PATH = "rest/user";

  private static final String MY_PASSWORD_PATH = "/password";

  public static final String PASSWORD_PATH = "/{userId}/password";
  
  public static final String RESET_PASSWORD_PATH = "/{userId}/reset";

  private final UserService userService;

  @Inject
  public UserResource(UserService userService)
  {
    this.userService = userService;
  }

  /**
   * Retrieves a list of users that can be used to assign role-to-user memberships for an application or organization.
   */
  @GET
  @Path("{ownerType: application|organization}/{ownerId}/query")
  @Produces(MediaType.APPLICATION_JSON)
  public FindMembersDTO findMembersForNonGlobalRoles(@PathParam("ownerType") String ownerType,
      @PathParam("ownerId") String ownerId, @QueryParam("q") String query,
      @QueryParam("groups") @DefaultValue("true") boolean groupsEnabled)
  {
    return userService.findMembersForNonGlobalRoles(ownerType, ownerId, query, groupsEnabled);
  }

  /**
   * Retrieves a list of users that can be used to assign role-to-user memberships for global roles.
   * 
   * @since 1.15.0
   */
  @GET
  @Path("global/{notUsed}/query")
  @Produces(MediaType.APPLICATION_JSON)
  public FindMembersDTO findMembersForGlobalRoles(@QueryParam("q") String query,
      @QueryParam("groups") @DefaultValue("true") boolean groupsEnabled)
  {
    return userService.findMembersForGlobalRoles(query, groupsEnabled);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<User> getAll() {
    return userService.getAll();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public User addUser(User user) {
    return userService.addUser(user);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public User updateUser(User user) {
    return userService.updateUser(user);
  }

  @DELETE
  @Path("{userId}")
  public void deleteUser(@PathParam("userId") String userId) {
    userService.deleteUser(userId);
  }

  @PUT
  @Path(MY_PASSWORD_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  public void changeMyPassword(ChangePasswordDTO password) {
    userService.changeMyPassword(password);
  }
  
  @PUT
  @Path(RESET_PASSWORD_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ChangePasswordDTO resetPassword(@PathParam("userId") String userId) {
    return userService.resetPassword(userId);
  }
}
