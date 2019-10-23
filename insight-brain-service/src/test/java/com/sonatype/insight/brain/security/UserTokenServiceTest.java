/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Date;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiUserTokenDTO;
import com.sonatype.insight.brain.dataaccess.security.UserTokenDAO;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

public class UserTokenServiceTest
    extends AbstractComponentTest
{
  @Inject
  private UserTokenService userTokenService;

  @Inject
  private UserTokenDAO userTokenDAO;

  @Test
  public void testCreateUserToken_InternalUser() {
    String username = "JohnDoe";
    tempEntity.newUser(username);
    assertThat(userTokenDAO.getByUsername(username)).isNull();
    when(subject.getPrincipal()).thenReturn(new UserPrincipal(username, "John Doe", true));

    Date start = new Date();
    ApiUserTokenDTO apiUserTokenDTO = userTokenService.createUserToken();
    Date end = new Date();

    assertThat(apiUserTokenDTO.userCode).hasSize(8);
    assertThat(apiUserTokenDTO.passCode).hasSize(44);

    UserToken persistedToken = userTokenDAO.getByUsername(username);
    assertThat(persistedToken).isNotNull();
    assertThat(persistedToken.getUsername()).isEqualTo(username);
    assertThat(persistedToken.getUsernameLowercase()).isEqualTo("johndoe");
    assertThat(persistedToken.getUserCode()).isEqualTo(apiUserTokenDTO.userCode);
    assertThat(persistedToken.getPassCode()).isNotNull();
    assertThat(persistedToken.getCreateTime()).isBetween(start, end, true, true);
    assertThat(persistedToken.isInternalUser()).isTrue();

    assertThat(persistedToken.getPassCode()).isNotEqualTo(apiUserTokenDTO.passCode);
  }

  @Test
  public void testCreateUserToken_ExternalUser() {
    String username = "JohnDoe";
    try {
      assertThat(userTokenDAO.getByUsername(username)).isNull();
      when(subject.getPrincipal()).thenReturn(new UserPrincipal(username, "John Doe", false));

      Date start = new Date();
      ApiUserTokenDTO apiUserTokenDTO = userTokenService.createUserToken();
      Date end = new Date();

      assertThat(apiUserTokenDTO.userCode).hasSize(8);
      assertThat(apiUserTokenDTO.passCode).hasSize(44);

      UserToken persistedToken = userTokenDAO.getByUsername(username);
      assertThat(persistedToken).isNotNull();
      assertThat(persistedToken.getUsername()).isEqualTo(username);
      assertThat(persistedToken.getUsernameLowercase()).isEqualTo("johndoe");
      assertThat(persistedToken.getUserCode()).isEqualTo(apiUserTokenDTO.userCode);
      assertThat(persistedToken.getPassCode()).isNotNull();
      assertThat(persistedToken.getCreateTime()).isBetween(start, end, true, true);
      assertThat(persistedToken.isInternalUser()).isFalse();

      assertThat(persistedToken.getPassCode()).isNotEqualTo(apiUserTokenDTO.passCode);
    }
    finally {
      userTokenDAO.delete(userTokenDAO.getByUsername(username));
    }
  }

  @Test
  public void testCreateUserToken_TokenExistsCaseInsensitive() {
    tempEntity.newUserToken("johndoe", true);
    when(subject.getPrincipal()).thenReturn(new UserPrincipal("JohnDoe", "John Doe", true));
    assertThatThrownBy(() -> userTokenService.createUserToken())
        .isInstanceOf(BadRequestException.class).hasMessage("UserToken already exists for user: JohnDoe");
  }

  @Test
  public void testDeleteUserToken() {
    String username = "user-a";
    tempEntity.newUser(username);
    UserToken userToken = tempEntity.newUserToken(username, true);
    userTokenService.deleteUserToken(username);

    assertThat(userTokenDAO.getById(userToken.getId())).isNull();
  }

  @Test
  public void testDeleteUserToken_NonExistentUserToken() {
    String username = "user-a";
    assertThatThrownBy(() -> userTokenService.deleteUserToken(username)).isInstanceOf(NotFoundException.class)
        .hasMessage("No user token found for user: " + username);
  }

  @Test
  public void testDeleteCurrentUserToken() {
    String username = "user-a";
    when(subject.getPrincipal()).thenReturn(new UserPrincipal(username, "Administrator", true));
    UserToken userToken = tempEntity.newUserToken(username, true);
    userTokenService.deleteCurrentUserToken();

    assertThat(userTokenDAO.getById(userToken.getId())).isNull();
  }

  @Test
  public void testDeleteCurrentUserToken_NonExistentUserToken() {
    String username = "user-a";
    when(subject.getPrincipal()).thenReturn(new UserPrincipal(username, "Administrator", true));

    assertThatThrownBy(() -> userTokenService.deleteCurrentUserToken()).isInstanceOf(NotFoundException.class)
        .hasMessage("No user token found for user: " + username);
  }
}
