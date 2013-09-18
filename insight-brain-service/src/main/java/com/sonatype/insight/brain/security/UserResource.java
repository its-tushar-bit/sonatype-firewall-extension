/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Arrays;
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

import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.security.User;

/**
 * @since 1.7
 */
@Named
@Path(UserResource.SERVICE_PATH)
public class UserResource
{
  public static final String SERVICE_PATH = "rest/user/";

  static final String FAKE_PASSWORD = "#~FAKE~CLM~PASSWORD~#";

  private final CLMRealm clmRealm;

  @Inject
  public UserResource(CLMRealm clmRealm) {
    this.clmRealm = clmRealm;
  }

  @GET
  @Produces({ MediaType.APPLICATION_JSON })
  public List<User> getAll()
  {
    List<User> users = new UserDAO().getAll();
    for (User user : users) {
      clearUserPassword(user);
    }
    return users;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public User addUser(User user) {
    user.setId(null);
    if (user.getPasswordHash() != null) {
      user.setPasswordHash(clmRealm.encryptPassword(user.getPasswordHash()).toCharArray());
    }
    new UserDAO().insert(user);

    clearUserPassword(user);

    return user;
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public User updateUser(User user) {
    UserDAO dao = new UserDAO();

    if (Arrays.equals(FAKE_PASSWORD.toCharArray(), user.getPasswordHash())) {
      // We don't have a new password, so we need to retrieve the existing one and fill it in the user object to be
      // updated.
      User existingUser = dao.getByIdNotNull(user.getId());
      user.setPasswordHash(existingUser.getPasswordHash());
    }
    else if (user.getPasswordHash() != null) {
      // We have a new password, encrypt it.
      user.setPasswordHash(clmRealm.encryptPassword(user.getPasswordHash()).toCharArray());
    }
    dao.update(user);

    clearUserPassword(user);

    return user;
  }

  @DELETE
  @Path("{userId}")
  public void deleteUser(@PathParam("userId") String userId)
  {
    UserDAO dao = new UserDAO();

    User user = dao.getByIdNotNull(userId);

    dao.delete(user);
  }

  private void clearUserPassword(User user) {
    user.clearPassword();
    user.setPasswordHash(FAKE_PASSWORD.toCharArray());
  }
}
