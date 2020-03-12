/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import com.sonatype.insight.brain.api.v2.dto.ApiUserTokenDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.configuration.ldap.LdapService;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.security.UserTokenDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.RandomStringUtils;
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

  private final PasswordService passwordService;

  private final LdapService ldapService;

  private final CurrentUser currentUser;

  @Inject
  public UserTokenService(
      UserTokenDAO userTokenDAO,
      PasswordService passwordService,
      LdapService ldapService,
      CurrentUser currentUser)
  {
    this.userTokenDAO = userTokenDAO;
    this.passwordService = passwordService;
    this.ldapService = ldapService;
    this.currentUser = currentUser;
  }

  public ApiUserTokenDTO createUserToken() {
    UserPrincipal userPrincipal = currentUser.getUserPrincipal();
    String username = userPrincipal.getUsername();

    if (UserTokenRealm.ID.equals(userPrincipal.getRealmId())) {
      // The user authenticated using a user token... so the user token already exists for this user.
      throw new BadRequestException("UserToken already exists for user: " + username);
    }

    String realmId = userPrincipal.getRealmId();
    if (userTokenDAO.getByUsernameAndRealmId(username, realmId) != null) {
      throw new BadRequestException("UserToken already exists for user: " + username);
    }

    if (!isRealmAllowed(realmId)) {
      throw new BadRequestException(
          "The login method that has been utilized for authentication does not support the creation of user tokens");
    }

    String userCode;
    // We should also ensure that the userCode generated is unique.
    // We can't have two user tokens with the same userCode.
    do {
      userCode = RandomStringUtils.randomAlphanumeric(8);
    }
    while (userTokenDAO.getByUserCode(userCode) != null);

    String passCode = RandomStringUtils.randomAlphanumeric(44);
    String hashed = passwordService.hashPassword(passCode);

    UserToken userToken = new UserToken();
    userToken.setUsername(username);
    userToken.setUserCode(userCode);
    userToken.setPassCode(hashed);
    userToken.setRealmId(realmId);

    audit(userToken);
    userTokenDAO.insert(userToken);

    ApiUserTokenDTO apiUserTokenDTO = new ApiUserTokenDTO();
    apiUserTokenDTO.userCode = userToken.getUserCode();
    apiUserTokenDTO.passCode = passCode;

    return apiUserTokenDTO;
  }

  private boolean isRealmAllowed(String realmId) {
    if (InternalRealm.ID.equals(realmId)) {
      return true;
    }

    return new LdapServerDAO().getById(realmId) != null;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public List<ApiUserTokenDTO> getUserTokensCreatedBetween(String createdAfter, String createdBefore) {
    log.debug("Querying user tokens with createTime between {} and {}.", createdAfter, createdBefore);
    return userTokenDAO
        .getByCreateDateBetween(parse(createdAfter), parse(createdBefore))
        .stream()
        .map(userToken -> new ApiUserTokenDTO(userToken.getUserCode()))
        .collect(Collectors.toList());
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void purgeUserTokens() throws NamingException {
    Map<String, LdapServer> ldapServersById =
        new LdapServerDAO().getAll().stream().collect(Collectors.toMap(LdapServer::getId, Function.identity()));

    for (UserToken userToken : userTokenDAO.getAllNotInternal()) {
      String username = userToken.getUsername();
      try {
        ldapService.getUserByName(ldapServersById.get(userToken.getRealmId()), username);
      }
      catch (NameNotFoundException e) {
        try (AuditSession auditSession =
            AuditData.get().recordSubEvent(AuditEvent.DELETE_USER_TOKEN, true /* independent */)) {
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
}
