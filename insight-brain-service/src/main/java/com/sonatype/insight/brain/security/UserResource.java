/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.List;

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

import com.sonatype.insight.brain.api.v2.HasFeature;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.UserService.ChangePasswordDTO;
import com.sonatype.insight.brain.security.UserService.FindMembersDTO;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.7
 */
@Named
@Timed
@Path(UserResource.RESOURCE_PATH)
public class UserResource
{
  public static final String RESOURCE_PATH = "rest/user";

  public static final String MY_PASSWORD_PATH = "/password";

  public static final String RESET_PASSWORD_PATH = "/{userId}/reset";

  private static final String OWNER_TYPE_SEGMENT = "{ownerType: global|application|organization}";

  private static final String SINGLETON_OWNER_TYPE_SEGMENT = "{ownerType: repository_container}";

  private static final String MEMBERS_FOR_OWNER_ROLES = OWNER_TYPE_SEGMENT + "/{ownerId}/query";

  private static final String MEMBERS_FOR_SINGLETON_OWNER_ROLES = SINGLETON_OWNER_TYPE_SEGMENT + "/query";

  public static final String SHOULD_DISPLAY_DEFAULT_PASSWORD_WARNING = "shouldDisplayDefaultPasswordWarning";

  private final UserService userService;

  @Inject
  public UserResource(UserService userService) {
    this.userService = userService;
  }

  /**
   * Retrieves a list of users that can be used to assign role-to-user memberships for a given application/organization
   * or at global level.
   */
  @GET
  @Path(MEMBERS_FOR_OWNER_ROLES)
  @Produces(MediaType.APPLICATION_JSON)
  public FindMembersDTO findMembersForNonGlobalRoles(@PathParam("ownerType") OwnerType ownerType,
                                                     @PathParam("ownerId") String ownerId,
                                                     @QueryParam("q") String query,
                                                     @QueryParam("groups") @DefaultValue("true") boolean groupsEnabled)
  {
    return userService.findMembersForRoles(ownerType, ownerId, query, groupsEnabled);
  }

  /**
   * Retrieves a list of users that can be used to assign role-to-user memberships for singleton owner types.
   *
   * @since 1.18.0
   */
  @GET
  @Path(MEMBERS_FOR_SINGLETON_OWNER_ROLES)
  @Produces(MediaType.APPLICATION_JSON)
  public FindMembersDTO findMembersForRepositoryRoles(@PathParam("ownerType") OwnerType ownerType,
                                                      @QueryParam("q") String query,
                                                      @QueryParam("groups") @DefaultValue("true") boolean groupsEnabled)
  {
    return userService.findMembersForRoles(ownerType, null, query, groupsEnabled);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @HasFeature(SystemConfigurationPropertyFeature.USER_MANAGEMENT_PAGES)
  public List<User> getAll() {
    return userService.getAll();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_USER)
  @HasFeature(SystemConfigurationPropertyFeature.USER_MANAGEMENT_PAGES)
  public User addUser(User user) {
    return userService.addUser(user);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_USER)
  @HasFeature(SystemConfigurationPropertyFeature.USER_MANAGEMENT_PAGES)
  public User updateUser(User user) {
    return userService.updateUser(user);
  }

  @DELETE
  @Path("{userId}")
  @Audited(AuditEvent.DELETE_USER)
  @HasFeature(SystemConfigurationPropertyFeature.USER_MANAGEMENT_PAGES)
  public void deleteUser(@PathParam("userId") String userId) {
    userService.deleteUser(userId);
  }

  @PUT
  @Path(MY_PASSWORD_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_USER_PASSWORD)
  @HasFeature(SystemConfigurationPropertyFeature.USER_MANAGEMENT_PAGES)
  public void changeMyPassword(ChangePasswordDTO password) {
    userService.changeMyPassword(password);
  }

  @PUT
  @Path(RESET_PASSWORD_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.RESET_USER_PASSWORD)
  @HasFeature(SystemConfigurationPropertyFeature.USER_MANAGEMENT_PAGES)
  public ChangePasswordDTO resetPassword(@PathParam("userId") String userId) {
    return userService.resetPassword(userId);
  }

  @GET
  @Path(SHOULD_DISPLAY_DEFAULT_PASSWORD_WARNING)
  @Produces(MediaType.TEXT_PLAIN)
  @HasFeature(SystemConfigurationPropertyFeature.USER_MANAGEMENT_PAGES)
  public boolean shouldDisplayDefaultPasswordWarning() {
    return userService.shouldDisplayDefaultPasswordWarning();
  }
}
