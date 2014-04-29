/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingException;
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

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final Logger log = LoggerFactory.getLogger(UserResource.class);

  static final String FAKE_PASSWORD = "#~FAKE~CLM~PASSWORD~#";

  private final CLMRealm clmRealm;

  private final SessionDAO sessionDAO;

  private static final ApplicationDAO applicationDAO = new ApplicationDAO();

  private final UserDirectory userDirectory;

  @Inject
  public UserResource(CLMRealm clmRealm, SessionDAO sessionDAO, UserDirectory userDirectory)
  {
    this.clmRealm = clmRealm;
    this.sessionDAO = sessionDAO;
    this.userDirectory = userDirectory;
  }

  /**
   * Retrieves a list of users that can be used to assign role-to-user memberships for an application or organization.
   */
  @GET
  @Path("{ownerType: global|application|organization}/{ownerId}/query")
  @Produces({ MediaType.APPLICATION_JSON })
  @Authorize(permission = Permission.WRITE)
  public FindMembersDTO findMembers(@AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId, @QueryParam("q") String query,
      @QueryParam("groups") @DefaultValue("true") boolean groupsEnabled)
  {
    if (StringUtils.isEmpty(query)) {
      throw new BadRequestException("No search term specified.");
    }

    String connectionError = null;
    UserDirectory.QueryResult result = userDirectory.getMembersByQuery(query, groupsEnabled);
    if (result.getException() instanceof NamingException) {
      log.error("Unable to connect to LDAP server.", result.getException());
      connectionError = "LDAP error, displaying local users only.";
    }
    else if (result.hasException()) {
      log.error("An error occurred while attempting to access full user directories.", result.getException());
      connectionError = "Unable to access full user directories, attempting to display local users only.";
    }

    return new FindMembersDTO(result.get(), connectionError);
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
    try {
      if (!userDirectory.isLdapUser(user)) {
        removeApplicationContact(user);
      }
    }
    catch (NamingException e) {
      log.error(e.getMessage(), e);
    }

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
  @Path(MY_PASSWORD_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  public void changeMyPassword(ChangePasswordDTO password) {
    UserDAO dao = new UserDAO();

    UserPrincipal principal = (UserPrincipal) SecurityUtils.getSubject().getPrincipal();

    User user = dao.getByUsername(principal.username.trim());
    if (user == null) {
      throw new NotFoundException("Could not find user with username " + principal.username);
    }

    // validate the old password first
    try {
      SecurityUtils.getSecurityManager().authenticate(
          new UsernamePasswordToken(user.getUsername(), password.oldPassword));
    }
    catch (AuthenticationException e) {
      throw new BadRequestException("Current password is wrong.");
    }

    user.setPassword(clmRealm.encryptPassword(password.newPassword));

    dao.update(user);
  }
  
  @PUT
  @Path(RESET_PASSWORD_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.ADMIN)
  public ChangePasswordDTO resetPassword(@PathParam("userId") String userId) {
    UserDAO dao = new UserDAO();
    User user = dao.getByIdNotNull(userId);

    String password = RandomStringUtils.randomAlphanumeric(12);

    user.setPassword(clmRealm.encryptPassword(password));

    dao.update(user);
    
    ChangePasswordDTO dto = new ChangePasswordDTO();
    dto.newPassword = password;
    
    return dto;
  }

  private void clearUserPassword(User user) {
    user.setPassword(FAKE_PASSWORD);
  }

  public static final class ChangePasswordDTO
  {
    public String oldPassword;
    public String newPassword;
  }

  // Remove the contact from the applications that have it
  private void removeApplicationContact(final User user) {
    final String userName = user.getUsername();
    final List<Application> applicationList = applicationDAO.getByContactInternalName(userName);
    for (final Application application : applicationList) {
      application.setContactInternalName(null);
      applicationDAO.update(application);
    }
  }

  public static class FindMembersDTO
  {
    private List<Member> members;
    private String error;

    public List<Member> getMembers() {
      return members;
    }

    public void setMembers(final List<Member> members) {
      this.members = members;
    }

    public String getError() {
      return error;
    }

    public void setError(final String error) {
      this.error = error;
    }

    public FindMembersDTO() {
    }

    public FindMembersDTO(List<Member> members, String error) {
      this.members = members;
      this.error = error;
    }
  }
}
