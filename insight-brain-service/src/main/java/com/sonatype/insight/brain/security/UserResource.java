/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.Console;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.ldap.LdapUser;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.subject.Subject;

/**
 * @since 1.7
 */
@Named
@Path(UserResource.SERVICE_PATH)
public class UserResource
{
  public static final String SERVICE_PATH = "rest/user";

  public static final String PASSWORD_PATH = "/{userId}/password";

  static final String FAKE_PASSWORD = "#~FAKE~CLM~PASSWORD~#";

  private final CLMRealm clmRealm;

  private final SessionDAO sessionDAO;

  private final LdapManager ldapManager;

  @Inject
  public UserResource(CLMRealm clmRealm, SessionDAO sessionDAO, LdapManager ldapManager) {
    this.clmRealm = clmRealm;
    this.sessionDAO = sessionDAO;
    this.ldapManager = ldapManager;
  }

  @GET
  @Path("query")
  @Produces({ MediaType.APPLICATION_JSON })
  @Authorize(permission = Permission.ADMIN)
  public List<UserQueryDTO> findUsers(@QueryParam("q") String query) throws NamingException {
    if (StringUtils.isEmpty(query)) {
      throw new BadRequestException("No search term specified.");
    }

    List<UserQueryDTO> users = new ArrayList<UserQueryDTO>() {
      @Override
      public boolean contains(Object o) {
        if (o instanceof UserQueryDTO) {
          UserQueryDTO checkDTO = (UserQueryDTO)o;
          for (UserQueryDTO userQueryDTO : this) {
            if (userQueryDTO.username.equals(checkDTO.username)) {
              return true;
            }
          }
        }
        return false;
      }
    };
    UserDAO dao = new UserDAO();
    for (User user : dao.findUsersByName(query)) {
      UserQueryDTO u = new UserQueryDTO(user.getUsername(), user.getFirstName(), user.getLastName(), user.getEmail(), "CLM");
      users.add(u);
    }

    if (ldapManager.isLdapEnabled()) {
      String ldapName = ldapManager.getLdapName();
      for (LdapUser user : ldapManager.findUsersByName(query, 100)) {
        UserQueryDTO u = new UserQueryDTO(user.getUsername(), user.getRealName(), null, user.getEmail(), ldapName);
        // Users are shaded by any user from a higher up realm that has the same username
        if (!users.contains(u)) {
          users.add(u);
        }
      }
    }
    return users;
  }

  @GET
  @Produces({ MediaType.APPLICATION_JSON })
  @Authorize(permission = Permission.ADMIN)
  public List<User> getAll() {
    List<User> users = new UserDAO().getAll();
    for (User user : users) {
      clearUserPassword(user);
    }
    return users;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.ADMIN)
  public User addUser(User user) {
    user.setId(null);
    user.setPassword(clmRealm.encryptPassword(user.getPassword()));
    new UserDAO().insert(user);

    clearUserPassword(user);

    return user;
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.ADMIN)
  public User updateUser(User user) {
    UserDAO dao = new UserDAO();

    if (FAKE_PASSWORD.equals(user.getPassword())) {
      // We don't have a new password, so we need to retrieve the existing one and fill it in the user object to be
      // updated.
      User existingUser = dao.getByIdNotNull(user.getId());
      user.setPassword(existingUser.getPassword());
    }
    else {
      // We have a new password, encrypt it.
      user.setPassword(clmRealm.encryptPassword(user.getPassword()));
    }
    dao.update(user);

    clearUserPassword(user);

    return user;
  }

  @DELETE
  @Path("{userId}")
  @Authorize(permission = Permission.ADMIN)
  public void deleteUser(@PathParam("userId") String userId) {
    UserDAO dao = new UserDAO();

    User user = dao.getByIdNotNull(userId);
    String username = SecurityUtils.getSubject().getPrincipal().toString();
    if (user.getUsername().equalsIgnoreCase(username)) {
      throw new BadRequestException("Cannot delete the currently logged in user.");
    }

    dao.delete(user);

    for (Session session : sessionDAO.getActiveSessions()) {
      Subject subject = new Subject.Builder().session(session).buildSubject();
      Object principal = subject.getPrincipal();
      //if the principal is null, then session either has an anonymous Subject, 
      //or the subject has already been invalidated by shiro
      if (principal != null && user.getUsername().equalsIgnoreCase(principal.toString())) {
        subject.logout();
      }
    }
  }

  @PUT
  @Path(PASSWORD_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  // Requires only authentication, no authorization.
  public void changePassword(@PathParam("userId") String userId, ChangePasswordDTO password) {
    UserDAO dao = new UserDAO();
    User user = dao.getByIdNotNull(userId);
    
    //validate the old password first
    try {
      SecurityUtils.getSecurityManager().authenticate(new UsernamePasswordToken(user.getUsername(), password.oldPassword));
    }
    catch (AuthenticationException e) {
      throw new BadRequestException("Invalid credentials supplied.");
    }

    user.setPassword(clmRealm.encryptPassword(password.newPassword));

    dao.update(user);
  }

  private void clearUserPassword(User user) {
    user.setPassword(FAKE_PASSWORD);
  }

  public static final class ChangePasswordDTO
  {
    public String oldPassword;
    public String newPassword;
  }

  public static class UserQueryDTO {
    public String username;
    public String firstName;
    public String lastName;
    public String email;
    public String realm;

    public UserQueryDTO() {
    }

    public UserQueryDTO(final String username, final String firstName, final String lastName,
                        final String email, final String realm)
    {
      this.username = username;
      this.firstName = firstName;
      this.lastName = lastName;
      this.email = email;
      this.realm = realm;
    }
  }
}
