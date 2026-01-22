/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.naming.NamingException;

import com.sonatype.insight.brain.api.v2.dto.ApiUserDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiUserListDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.15.0
 */
@Named
public class UserService
    extends AbstractUserService
{
  private static final Logger log = LoggerFactory.getLogger(UserService.class);

  static final String FAKE_PASSWORD = "#~FAKE~PASSWORD~#";

  private static final String ADMIN_DEFAULT_PASSWORD = "admin123";

  private final InternalRealm clmRealm;

  private final PasswordService passwordService;

  private final UserDirectory userDirectory;

  private final Configuration configuration;

  private final UserDAO userDAO;

  private final ApplicationDAO applicationDAO;

  @Inject
  public UserService(
      InternalRealm clmRealm,
      PasswordService passwordService,
      SessionDAO sessionDAO,
      UserDAO userDAO,
      SsoUserService ssoUserService,
      ApplicationDAO applicationDAO,
      UserDirectory userDirectory,
      Configuration configuration,
      CurrentUser currentUser,
      DefaultWebSessionManager defaultWebSessionManager)
  {
    super(sessionDAO, defaultWebSessionManager, currentUser, ssoUserService);
    this.clmRealm = clmRealm;
    this.passwordService = passwordService;
    this.userDAO = userDAO;
    this.applicationDAO = applicationDAO;
    this.userDirectory = userDirectory;
    this.configuration = configuration;
  }

  // Authorization is checked in findMembersForNonGlobalRoles and findMembersForGlobalRoles
  FindMembersDTO findMembersForRoles(OwnerType ownerType, String ownerId, String query, boolean groupsEnabled) {
    if (OwnerType.GLOBAL.equals(ownerType)) {
      return findMembersForGlobalRoles(query, groupsEnabled);
    }
    else {
      return findMembersForNonGlobalRoles(ownerType, ownerId, query, groupsEnabled);
    }
  }

  /**
   * Retrieves a list of users that can be used to assign role-to-user memberships for an application or organization.
   */
  @Authorize(permission = Permission.EDIT_ACCESS_CONTROL)
  protected FindMembersDTO findMembersForNonGlobalRoles(
      @SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.ID) String ownerId,
      String query,
      boolean groupsEnabled)
  {
    return findMembers(query, groupsEnabled);
  }

  /**
   * Retrieves a list of users that can be used to assign role-to-user memberships for global roles.
   */
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  protected FindMembersDTO findMembersForGlobalRoles(String query, boolean groupsEnabled) {
    return findMembers(query, groupsEnabled);
  }

  private FindMembersDTO findMembers(String query, boolean groupsEnabled) {
    if (StringUtils.isEmpty(query)) {
      throw new BadRequestException("No search term specified.");
    }

    String connectionError = null;
    UserDirectory.QueryResult result = userDirectory.getMembersByQuery(query, groupsEnabled);
    if (result.getException() instanceof NamingException) {
      log.error("Unable to connect to LDAP server.", result.getException());
      connectionError = "LDAP error, displaying partial results.";
    }
    else if (result.hasException()) {
      log.error("An error occurred while attempting to access full user directories.", result.getException());
      connectionError = "Unable to access full user directories, displaying partial results.";
    }

    return new FindMembersDTO(result.get(), connectionError);
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  List<User> getAll() {
    List<User> users = userDAO.getAll();
    for (User user : users) {
      clearUserPassword(user);
    }
    return users;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  User addUser(User user) {
    if (user == null) {
      throw new BadRequestException("No user details specified.");
    }
    user.setId(null);
    user.setPassword(passwordService.hashPassword(user.getPassword()));
    userDAO.insert(user);
    auditUser(user);

    clearUserPassword(user);

    return user;
  }

  /**
   * Updates the data for an internal user. This method cannot be used to update: - the password - the password is
   * changed via the {@link UserService#resetPassword(String)} method; - the username - the username is used as an ID in
   * {@link MembershipMapping}s.
   */
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  User updateUser(User user) {
    if (!FAKE_PASSWORD.equals(user.getPassword())) {
      throw new BadRequestException("Cannot change user password.");
    }
    // We don't have the password, so we need to retrieve the existing one and fill it in the user object to be updated.
    User existingUser = userDAO.getByIdNotNull(user.getId());
    user.setPassword(existingUser.getPassword());

    if (!existingUser.getUsername().equals(user.getUsername())) {
      throw new BadRequestException("Cannot change username.");
    }

    userDAO.update(user);
    auditUser(user);

    clearUserPassword(user);

    return user;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  void deleteUser(String userId) {
    deleteInternalUser(userDAO.getByIdNotNull(userId));
  }

  private void deleteInternalUser(User user) {
    validateUserToDeleteIsNotCurrentlyLoggedIn(User.INTERNAL_REALM_ID, user.getUsername());
    userDAO.delete(user);
    auditUser(user);
    try {
      if (!userDirectory.isLdapUser(user)) {
        removeApplicationContact(user);
      }
    }
    catch (NamingException e) {
      log.error(e.getMessage(), e);
    }
    logoutUser(User.INTERNAL_REALM_ID, user.getUsername());
  }

  private void deleteSsoUser(SsoUser ssoUser) {
    validateUserToDeleteIsNotCurrentlyLoggedIn(ssoUser.getRealmId(), ssoUser.getUsername());
    deleteUser(ssoUser);
  }

  private void auditUser(User user) {
    auditUser(User.INTERNAL_REALM_ID, user.getUsername(), user.getFirstName(), user.getLastName(), user.getEmail());
  }

  void changeMyPassword(ChangePasswordDTO password) {
    String username = currentUser.getUsername();

    User user = userDAO.getByUsername(username.trim());
    if (user == null) {
      throw new NotFoundException("Could not find user with username " + username);
    }

    // validate the old password first
    try {
      SecurityUtils.getSecurityManager().authenticate(
          new UsernamePasswordToken(user.getUsername(), password.oldPassword));
    }
    catch (AuthenticationException e) {
      throw new BadRequestException("Current password is wrong.");
    }

    user.setPassword(passwordService.hashPassword(password.newPassword));

    userDAO.update(user);
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  ChangePasswordDTO resetPassword(String userId) {
    User user = userDAO.getByIdNotNull(userId);
    AuditData.get().setData("username", user.getUsername());

    String password = RandomStringUtils.secure().nextAlphanumeric(12);

    user.setPassword(passwordService.hashPassword(password));

    userDAO.update(user);

    ChangePasswordDTO dto = new ChangePasswordDTO();
    dto.newPassword = password;

    return dto;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public boolean shouldDisplayDefaultPasswordWarning() {
    if (!configuration.isEnableDefaultPasswordWarning()) {
      return false;
    }

    try {
      AuthenticationInfo adminAuthInfo =
          clmRealm.getAuthenticationInfo(new UsernamePasswordToken(User.ADMIN_USERNAME, ADMIN_DEFAULT_PASSWORD));

      // if adminAuthInfo is null, then the admin user doesn't even exist so we shouldn't show the warning
      return adminAuthInfo != null;
    }
    catch (AuthenticationException e) {
      // the current password is not the default - so no warning
      return false;
    }
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

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public ApiUserListDTO getAllApiUserDTOs(String realmId) {
    ApiUserListDTO apiUserListDTO = new ApiUserListDTO();
    if (ssoUserService.isSsoRealm(realmId)) {
      apiUserListDTO.users = ssoUserService.getAll().stream().map(this::convert).collect(Collectors.toList());
    }
    else {
      apiUserListDTO.users = userDAO.getAll().stream()
          .map(user -> convert(user))
          .collect(Collectors.toList());
    }
    return apiUserListDTO;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public ApiUserDTO getApiUserDTOByUsernameAndRealmId(String username, String realmId) {
    if (ssoUserService.isSsoRealm(realmId)) {
      return convert(ssoUserService.getByUsernameNotNull(username));
    }
    else {
      return convert(userDAO.getByUsernameNotNull(username));
    }
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void addUser(ApiUserDTO userDTO) {
    User user = convert(userDTO);
    addUser(user);
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public ApiUserDTO updateUser(String username, ApiUserDTO userDTO) {
    if (userDTO == null) {
      throw new BadRequestException("No user details specified.");
    }
    if (userDTO.username != null && !userDTO.username.equals(username)) {
      throw new BadRequestException("Cannot change username.");
    }
    if (userDTO.password != null) {
      throw new BadRequestException("Cannot change user password.");
    }
    User user = userDAO.getByUsernameNotNull(username);
    if (userDTO.firstName != null) {
      user.setFirstName(userDTO.firstName);
    }
    if (userDTO.lastName != null) {
      user.setLastName(userDTO.lastName);
    }
    if (userDTO.email != null) {
      user.setEmail(userDTO.email);
    }
    userDAO.update(user);
    auditUser(user);
    return convert(user);
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void deleteUserByRealmIdAndUsername(String realmId, String username) {
    if (ssoUserService.isSsoRealm(realmId)) {
      deleteSsoUser(ssoUserService.getByUsernameNotNull(username));
    }
    else {
      deleteInternalUser(userDAO.getByUsernameNotNull(username));
    }
  }

  private ApiUserDTO convert(SsoUser user) {
    ApiUserDTO userDTO = new ApiUserDTO();
    userDTO.username = user.getUsername();
    userDTO.firstName = user.getFirstName();
    userDTO.lastName = user.getLastName();
    userDTO.email = user.getEmail();
    userDTO.realm = user.getRealmId();
    return userDTO;
  }

  private ApiUserDTO convert(User user) {
    ApiUserDTO userDTO = new ApiUserDTO();
    userDTO.username = user.getUsername();
    // exclude password
    userDTO.firstName = user.getFirstName();
    userDTO.lastName = user.getLastName();
    userDTO.email = user.getEmail();
    userDTO.realm = User.INTERNAL_REALM_ID;

    return userDTO;
  }

  private User convert(ApiUserDTO userDTO) {
    if (userDTO == null) {
      return null;
    }
    User user = new User();
    // exclude id
    user.setUsername(userDTO.username);
    user.setPassword(userDTO.password);
    user.setFirstName(userDTO.firstName);
    user.setLastName(userDTO.lastName);
    user.setEmail(userDTO.email);
    return user;
  }
}
