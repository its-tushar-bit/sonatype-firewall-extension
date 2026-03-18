/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import com.sonatype.insight.brain.api.v2.dto.ApiUserTokenDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiUserTokenExistsDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.configuration.ldap.LdapService;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.security.UserTokenDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.76
 */
@Named
public class UserTokenService
{
  private static final Logger log = LoggerFactory.getLogger(UserTokenService.class);

  private final UserTokenDAO userTokenDAO;

  private final SsoUserService ssoUserService;

  private final PasswordService passwordService;

  private final LdapService ldapService;

  private final CurrentUser currentUser;

  private final LdapServerDAO ldapServerDAO;

  private final Configuration configuration;

  @Inject
  public UserTokenService(
      UserTokenDAO userTokenDAO,
      SsoUserService ssoUserService,
      LdapServerDAO ldapServerDAO,
      PasswordService passwordService,
      LdapService ldapService,
      CurrentUser currentUser,
      Configuration configuration)
  {
    this.userTokenDAO = userTokenDAO;
    this.ssoUserService = ssoUserService;
    this.ldapServerDAO = ldapServerDAO;
    this.passwordService = passwordService;
    this.ldapService = ldapService;
    this.currentUser = currentUser;
    this.configuration = configuration;
  }

  public ApiUserTokenDTO createUserToken() {
    UserPrincipal userPrincipal = currentUser.getUserPrincipal();
    String username = userPrincipal.getUsername();
    String realmId = userPrincipal.getRealmId();

    if (UserTokenRealm.ID.equals(realmId)) {
      // The user authenticated using a user token... so the user token already exists for this user.
      throw new BadRequestException("UserToken already exists for user: " + username);
    }

    if (userTokenDAO.getByUsernameAndRealmId(username, realmId) != null) {
      throw new BadRequestException("UserToken already exists for user: " + username);
    }

    if (!isRealmAllowed(realmId)) {
      throw new BadRequestException(
          "The login method that has been utilized for authentication does not support the creation of user tokens");
    }

    if (ssoUserService.isSsoRealm(realmId) && ssoUserService.getByUsername(username) == null) {
      throw new BadRequestException(
          "Unable to get user session details, you must relogin before generating a user token.");
    }

    try (TransactionContext tx = userTokenDAO.createTransactionContext()) {
      tx.begin();

      String userCode;
      // We should also ensure that the userCode generated is unique.
      // We can't have two user tokens with the same userCode.
      do {
        userCode = RandomStringUtils.secure().nextAlphanumeric(8);
      }
      while (userTokenDAO.getByUserCode(tx, userCode) != null);

      String passCode = RandomStringUtils.secure().nextAlphanumeric(44);
      String hashed = passwordService.hashPassword(passCode);

      UserToken userToken = new UserToken();
      userToken.setUsername(username);
      userToken.setUserCode(userCode);
      userToken.setPassCode(hashed);
      userToken.setRealmId(realmId);

      audit(userToken);
      userTokenDAO.insert(tx, userToken);
      tx.commit();
      ApiUserTokenDTO apiUserTokenDTO = new ApiUserTokenDTO();
      apiUserTokenDTO.userCode = userToken.getUserCode();
      apiUserTokenDTO.passCode = passCode;

      return apiUserTokenDTO;
    }
  }

  public ApiUserTokenExistsDTO userTokenExistsForCurrentUser() {
    UserPrincipal userPrincipal = currentUser.getUserPrincipal();
    if (userPrincipal == null) {
      throw new UnauthenticatedException();
    }
    ApiUserTokenExistsDTO apiUserTokenExistsDTO = new ApiUserTokenExistsDTO();
    apiUserTokenExistsDTO.userTokenExists =
        userTokenDAO.userTokenExists(userPrincipal.getUsername(), userPrincipal.getRealmId());
    return apiUserTokenExistsDTO;
  }

  public Date getCurrentUserTokenCreateTime() {
    UserPrincipal userPrincipal = currentUser.getUserPrincipal();
    if (userPrincipal == null) {
      throw new UnauthenticatedException();
    }

    UserToken userToken = userTokenDAO.getByUsernameAndRealmId(userPrincipal.getUsername(), userPrincipal.getRealmId());
    if (userToken == null) {
      throw new NotFoundException("User token does not exist for user: " + userPrincipal.getUsername());
    }

    return userToken.getCreateTime();
  }

  private boolean isRealmAllowed(String realmId) {
    if (InternalRealm.ID.equals(realmId)) {
      return true;
    }
    if (ssoUserService.isSsoRealm(realmId)) {
      return true;
    }
    if (CrowdRealm.ID.equals(realmId) && hasCrowdUserTokenSupport()) {
      return true;
    }
    return ldapServerDAO.getById(realmId) != null;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public List<ApiUserTokenDTO> getUserTokensCreatedBetweenAndRealmId(
      String createdAfter,
      String createdBefore,
      String realmId)
  {
    realmId = normalizeRealmId(realmId);

    log.debug("Querying user tokens with createTime between {} and {} and realm {}.", createdAfter, createdBefore,
        realmId);
    return userTokenDAO
        .getByCreateDateBetweenAndRealmId(parse(createdAfter), parse(createdBefore), realmId)
        .stream()
        .map(this::createApiUserTokenDTO)
        .collect(Collectors.toList());
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public ApiUserTokenDTO getUserTokenByUsernameAndRealmId(String username, String realmId) {
    if (StringUtils.isBlank(username)) {
      throw new BadRequestException("A username is required.");
    }
    realmId = normalizeRealmId(realmId);
    UserToken userToken = userTokenDAO.getByUsernameAndRealmId(username, realmId);
    if (userToken == null) {
      throw new NotFoundException(
          "No user token found for " + realmId + " user " + username + ".");
    }
    if (isTokenExpired(userToken)) {
      throw new UnauthorizedException(
          "User token for " + realmId + " user " + username + " has expired.");
    }
    return createApiUserTokenDTO(userToken);
  }

  private String normalizeRealmId(String realmId) {
    if (ssoUserService.isSsoRealm(realmId)) {
      return ssoUserService.normalizeRealmId(realmId);
    }
    if (hasCrowdUserTokenSupport() && CrowdRealm.ID.equalsIgnoreCase(realmId)) {
      return CrowdRealm.ID;
    }
    return User.INTERNAL_REALM_ID;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void purgeUserTokens() throws NamingException {
    Map<String, LdapServer> ldapServersById =
        ldapServerDAO.getAll().stream().collect(Collectors.toMap(LdapServer::getId, Function.identity()));

    for (UserToken userToken : userTokenDAO.getAllLdap()) {
      String username = userToken.getUsername();
      try {
        ldapService.getUserByName(ldapServersById.get(userToken.getRealmId()), username);
      }
      catch (NameNotFoundException e) {
        try (AuditSession auditSession =
            AuditData.get().recordSubEvent(AuditEvent.DELETE_USER_TOKEN, true /* independent */))
        {
          deleteAndAuditUserToken(userToken);
        }
        log.info("The '{}' user token was created for the '{}' LDAP user, which doesn't exist anymore."
            + " The user token was deleted.", userToken.getUserCode(), username);
      }
    }
  }

  public void deleteCurrentUserToken() {
    UserPrincipal userPrincipal = currentUser.getUserPrincipal();
    String username = userPrincipal.getUsername();
    UserToken userToken = userTokenDAO.getByUsernameAndRealmId(username, userPrincipal.getRealmId());
    if (userToken == null) {
      throw new NotFoundException("No user token found for user: " + username);
    }
    deleteAndAuditUserToken(userToken);
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void deleteUserTokenByUserCode(String userCode) {
    UserToken userToken = userTokenDAO.getByUserCode(userCode);

    if (userToken == null) {
      throw new NotFoundException("Cannot find a user token with user code: " + userCode);
    }

    deleteAndAuditUserToken(userToken);
  }

  public void deleteAndAuditUserToken(UserToken userToken) {
    audit(userToken);
    userTokenDAO.delete(userToken);
  }

  private void audit(UserToken userToken) {
    AuditData.get()
        .setData("username", userToken.getUsername())
        .setData("userCode", userToken.getUserCode());
  }

  private Date parse(String dateString) {
    if (dateString == null) {
      return null;
    }
    try {
      return new SimpleDateFormat("yyyy-MM-dd").parse(dateString);
    }
    catch (ParseException e) {
      throw new BadRequestException(String.format("Could not parse: %s. Expected format is: yyyy-MM-dd.", dateString));
    }
  }

  private boolean hasCrowdUserTokenSupport() {
    return SystemConfigurationPropertyFeature.CROWD_INTEGRATION.isEnabled();
  }

  /**
   * Check if a user token has expired based on configured expiration days.
   *
   * @param userToken the user token to check
   * @return true if the token has expired, false otherwise
   */
  public boolean isTokenExpired(final UserToken userToken) {
    int configuredExpirationDays = getConfiguredExpirationDays();
    if (configuredExpirationDays <= 0 || userToken.getCreateTime() == null) {
      return false; // No expiration configured or no create time
    }

    Instant expirationInstant = Objects.requireNonNull(calculateExpirationDate(userToken)).toInstant();
    return Instant.now().isAfter(expirationInstant);
  }

  private int getConfiguredExpirationDays() {
    Integer expirationDays = configuration.getUserTokenDefaultExpirationDays();

    if (expirationDays == null) {
      return 0; // No expiration configured
    }

    return expirationDays;
  }

  private Date calculateExpirationDate(UserToken userToken) {
    int expiryDays = getConfiguredExpirationDays();
    if (expiryDays <= 0 || userToken.getCreateTime() == null) {
      return null; // No expiration configured
    }
    Instant expirationInstant = userToken.getCreateTime().toInstant().plus(expiryDays, ChronoUnit.DAYS);
    return Date.from(expirationInstant);
  }

  private ApiUserTokenDTO createApiUserTokenDTO(UserToken userToken) {
    ApiUserTokenDTO apiUserTokenDTO = new ApiUserTokenDTO();
    apiUserTokenDTO.userCode = userToken.getUserCode();
    apiUserTokenDTO.username = userToken.getUsername();
    apiUserTokenDTO.realm = userToken.getRealmId();
    apiUserTokenDTO.createTime = userToken.getCreateTime();
    apiUserTokenDTO.lastAccessTime = userToken.getLastAccessTime();
    apiUserTokenDTO.expirationDate = calculateExpirationDate(userToken);
    return apiUserTokenDTO;
  }
}
