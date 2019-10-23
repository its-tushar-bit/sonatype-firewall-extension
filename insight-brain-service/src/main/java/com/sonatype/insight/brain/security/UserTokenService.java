/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiUserTokenDTO;
import com.sonatype.insight.brain.dataaccess.security.UserTokenDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.shiro.SecurityUtils;

/**
 * @since 1.76
 */
@Named
public class UserTokenService
{
  private final UserTokenDAO userTokenDAO;

  private final PasswordService passwordService;

  @Inject
  public UserTokenService(UserTokenDAO userTokenDAO, PasswordService passwordService) {
    this.userTokenDAO = userTokenDAO;
    this.passwordService = passwordService;
  }

  public ApiUserTokenDTO createUserToken() {
    UserPrincipal user = (UserPrincipal) SecurityUtils.getSubject().getPrincipal();
    String username = user.getUsername();

    if (userTokenDAO.getByUsername(username) != null) {
      throw new BadRequestException("UserToken already exists for user: " + username);
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
    userToken.setInternalUser(user.isInternalUser());
    userTokenDAO.insert(userToken);

    ApiUserTokenDTO apiUserTokenDTO = new ApiUserTokenDTO();
    apiUserTokenDTO.userCode = userToken.getUserCode();
    apiUserTokenDTO.passCode = passCode;

    return apiUserTokenDTO;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void deleteUserToken(String username) {
    deleteUserTokenByUsername(username);
  }

  public void deleteCurrentUserToken() {
    deleteUserTokenByUsername(((UserPrincipal) SecurityUtils.getSubject().getPrincipal()).getUsername());
  }

  private void deleteUserTokenByUsername(String username) {
    UserToken userToken = userTokenDAO.getByUsername(username);
    if (userToken == null) {
      throw new NotFoundException("No user token found for user: " + username);
    }
    userTokenDAO.delete(userToken);
  }
}
