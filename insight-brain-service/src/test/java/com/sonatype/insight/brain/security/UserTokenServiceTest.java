/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiUserTokenDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiUserTokenExistsDTO;
import com.sonatype.insight.brain.configuration.ldap.TestLdapServer;
import com.sonatype.insight.brain.dataaccess.security.UserTokenDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

public class UserTokenServiceTest
    extends AbstractComponentTest
{
  private final Date december30 = new GregorianCalendar(2019, Calendar.DECEMBER, 30).getTime();

  private final Date december29 = new GregorianCalendar(2019, Calendar.DECEMBER, 29).getTime();

  private final Date december28 = new GregorianCalendar(2019, Calendar.DECEMBER, 28).getTime();

  private final Date december27 = new GregorianCalendar(2019, Calendar.DECEMBER, 27).getTime();

  @Rule
  public TestLdapServer embeddedTestLdapServer = new TestLdapServer();

  @Inject
  private UserTokenService userTokenService;

  @Inject
  private UserTokenDAO userTokenDAO;

  @Test
  public void testCreateUserToken_InternalUser() {
    String username = "JohnDoe";
    tempEntity.newUser(username);
    when(subject.getPrincipal()).thenReturn(new UserPrincipal(username, "John Doe", InternalRealm.ID));

    Date start = new Date();
    ApiUserTokenDTO apiUserTokenDTO = userTokenService.createUserToken();
    Date end = new Date();

    assertThat(apiUserTokenDTO.userCode).hasSize(8);
    assertThat(apiUserTokenDTO.passCode).hasSize(44);

    UserToken persistedToken = userTokenDAO.getByUsernameAndRealmId(username, InternalRealm.ID);
    assertThat(persistedToken).isNotNull();
    assertThat(persistedToken.getUsername()).isEqualTo(username);
    assertThat(persistedToken.getUserCode()).isEqualTo(apiUserTokenDTO.userCode);
    assertThat(persistedToken.getPassCode()).isNotNull();
    assertThat(persistedToken.getRealmId()).isEqualTo(InternalRealm.ID);
    assertThat(persistedToken.getCreateTime()).isBetween(start, end, true, true);
    assertThat(persistedToken.isInternalUser()).isTrue();

    assertThat(persistedToken.getPassCode()).isNotEqualTo(apiUserTokenDTO.passCode);
  }

  @Test
  public void testCreateUserToken_LDAPUser() {
    String username = "JohnDoe";
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    String realmId = ldapServer.getId();
    try {
      when(subject.getPrincipal()).thenReturn(new UserPrincipal(username, "John Doe", realmId));

      Date start = new Date();
      ApiUserTokenDTO apiUserTokenDTO = userTokenService.createUserToken();
      Date end = new Date();

      assertThat(apiUserTokenDTO.userCode).hasSize(8);
      assertThat(apiUserTokenDTO.passCode).hasSize(44);

      UserToken persistedToken = userTokenDAO.getByUsernameAndRealmId(username, realmId);
      assertThat(persistedToken).isNotNull();
      assertThat(persistedToken.getUsername()).isEqualTo(username);
      assertThat(persistedToken.getUserCode()).isEqualTo(apiUserTokenDTO.userCode);
      assertThat(persistedToken.getPassCode()).isNotNull();
      assertThat(persistedToken.getRealmId()).isEqualTo(realmId);
      assertThat(persistedToken.getCreateTime()).isBetween(start, end, true, true);
      assertThat(persistedToken.isInternalUser()).isFalse();

      assertThat(persistedToken.getPassCode()).isNotEqualTo(apiUserTokenDTO.passCode);
    }
    finally {
      userTokenDAO.delete(userTokenDAO.getByUsernameAndRealmId(username, realmId));
    }
  }

  @Test
  public void testCreateUserToken_ReverseProxyUser() {
    testCreateUserToken_NotAllowedRealm(ReverseProxyRealm.ID);
  }

  @Test
  public void testCreateUserToken_SAMLUser() {
    testCreateUserToken_NotAllowedRealm(SamlRealm.ID);
  }

  private void testCreateUserToken_NotAllowedRealm(String realmId) {
    String username = "JohnDoe";
    try {
      when(subject.getPrincipal()).thenReturn(new UserPrincipal(username, "John Doe", realmId));

      assertThatThrownBy(() -> userTokenService.createUserToken()) //
          .isInstanceOf(BadRequestException.class) //
          .hasMessage("The login method that has been utilized for authentication"
              + " does not support the creation of user tokens");
    }
    finally {
      userTokenDAO.delete(userTokenDAO.getByUsernameAndRealmId(username, realmId));
    }
  }

  @Test
  public void testCreateUserToken_UserTokenAlreadyExists() {
    String username = "JohnDoe";
    tempEntity.newUser(username);
    tempEntity.newUserToken(username, InternalRealm.ID);
    when(subject.getPrincipal()).thenReturn(new UserPrincipal("JohnDoe", "John Doe", InternalRealm.ID));
    assertThatThrownBy(() -> userTokenService.createUserToken())
        .isInstanceOf(BadRequestException.class).hasMessage("UserToken already exists for user: JohnDoe");
  }

  @Test
  public void testCreateUserToken_UserAuthenticatedWithUserToken() {
    String username = "JohnDoe";
    tempEntity.newUser(username);
    tempEntity.newUserToken(username, InternalRealm.ID);
    when(subject.getPrincipal()).thenReturn(new UserPrincipal("JohnDoe", "John Doe", UserTokenRealm.ID));
    assertThatThrownBy(() -> userTokenService.createUserToken()).isInstanceOf(BadRequestException.class)
        .hasMessage("UserToken already exists for user: JohnDoe");
  }

  @Test
  public void testPurgeUserTokens() throws Exception {
    embeddedTestLdapServer.start();
    embeddedTestLdapServer.loadData("/" + getClass().getSimpleName() + "/ldap_users.ldif");

    LdapServer ldapServer = tempEntity.newLdapServer("test");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedTestLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    // Token for internal user, should not be purged.
    UserToken userTokenInternalUser = tempEntity.newUserToken("JohnDoe", InternalRealm.ID);
    // Token for existing LDAP user, should not be purged.
    UserToken userTokenLdapUserValid = tempEntity.newUserToken("testuser", ldapServer.getId());
    // Token for non-existing LDAP user, should be purged.
    UserToken userTokenLdapUseInvalid = tempEntity.newUserToken("no-such-user", ldapServer.getId());
    userTokenService.purgeUserTokens();

    assertThat(userTokenDAO.getById(userTokenInternalUser.getId())).isNotNull();
    assertThat(userTokenDAO.getById(userTokenLdapUserValid.getId())).isNotNull();
    assertThat(userTokenDAO.getById(userTokenLdapUseInvalid.getId())).isNull();
  }

  @Test
  public void testDeleteCurrentUserToken() {
    String username = "user-a";
    when(subject.getPrincipal()).thenReturn(new UserPrincipal(username, "Administrator", InternalRealm.ID));
    UserToken userToken = tempEntity.newUserToken(username, InternalRealm.ID);
    userTokenService.deleteCurrentUserToken();

    assertThat(userTokenDAO.getById(userToken.getId())).isNull();
  }

  @Test
  public void testDeleteCurrentUserToken_NonExistentUserToken() {
    String username = "user-a";
    when(subject.getPrincipal()).thenReturn(new UserPrincipal(username, "Administrator", InternalRealm.ID));

    assertThatThrownBy(() -> userTokenService.deleteCurrentUserToken()).isInstanceOf(NotFoundException.class)
        .hasMessage("No user token found for user: " + username);
  }

  @Test
  public void testGetUserTokensCreatedBetween() {
    tempEntity.newUserToken("foo", december27);
    UserToken bar = tempEntity.newUserToken("bar", december28);
    tempEntity.newUserToken("baz", december29);
    tempEntity.newUserToken("qux", december30);

    List<ApiUserTokenDTO> userTokenDTOs = userTokenService.getUserTokensCreatedBetween("2019-12-28", "2019-12-28");
    assertThat(userTokenDTOs)
        .extracting(apiUserTokenDTO -> apiUserTokenDTO.userCode)
        .containsExactlyInAnyOrder(bar.getUserCode());

    // Assert passcode are not returned
    assertThat(userTokenDTOs)
        .extracting(apiUserTokenDTO -> apiUserTokenDTO.passCode)
        .filteredOn(Objects::nonNull)
        .isEmpty();
  }

  @Test
  public void testGetUserTokensCreatedBetween_MustHandleNullArguments() {
    UserToken foo = tempEntity.newUserToken("foo", december27);
    UserToken bar = tempEntity.newUserToken("bar", december28);
    UserToken baz = tempEntity.newUserToken("baz", december29);
    UserToken qux = tempEntity.newUserToken("qux", december30);

    // Assert all user tokens are returned with user code
    List<ApiUserTokenDTO> userTokenDTOs = userTokenService.getUserTokensCreatedBetween(null, null);
    assertThat(userTokenDTOs)
        .extracting(apiUserTokenDTO -> apiUserTokenDTO.userCode)
        .containsExactlyInAnyOrder(foo.getUserCode(), bar.getUserCode(), baz.getUserCode(), qux.getUserCode());

    // Assert passcode are not returned
    assertThat(userTokenDTOs)
        .extracting(apiUserTokenDTO -> apiUserTokenDTO.passCode)
        .filteredOn(Objects::nonNull)
        .isEmpty();
  }

  @Test
  public void testGetUserTokensCreatedBetween_CanNotParseCreatedAfter() {
    assertThatThrownBy(() -> userTokenService.getUserTokensCreatedBetween("foo", null))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Could not parse: foo. Expected format is: yyyy-MM-dd.");
  }

  @Test
  public void testGetUserTokensCreatedBetween_CanNotParseCreatedBefore() {
    assertThatThrownBy(() -> userTokenService.getUserTokensCreatedBetween(null, "bar"))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Could not parse: bar. Expected format is: yyyy-MM-dd.");
  }

  @Test
  public void testDeleteUserTokenByUserCode() {
    UserToken userToken = tempEntity.newUserToken("john", InternalRealm.ID);
    userTokenService.deleteUserTokenByUserCode(userToken.getUserCode());
    assertThat(userTokenDAO.getById(userToken.getId())).isNull();
  }

  @Test
  public void testDeleteUserTokenByUserCode_TokenDoesNotExist() {
    assertThatThrownBy(() -> userTokenService.deleteUserTokenByUserCode("absent")).isInstanceOf(NotFoundException.class)
        .hasMessage("Cannot find a user token with user code: absent");
  }

  @Test
  public void testUserTokenExistsForCurrentUser() {
    String username = "user-a";
    when(subject.getPrincipal()).thenReturn(new UserPrincipal(username, "UserA", InternalRealm.ID));

    ApiUserTokenExistsDTO apiUserTokenExistsDTO = userTokenService.userTokenExistsForCurrentUser();
    assertThat(apiUserTokenExistsDTO).isNotNull();
    assertThat(apiUserTokenExistsDTO.userTokenExists).isFalse();

    UserToken userToken = tempEntity.newUserToken(username, InternalRealm.ID);
    apiUserTokenExistsDTO = userTokenService.userTokenExistsForCurrentUser();
    assertThat(apiUserTokenExistsDTO).isNotNull();
    assertThat(apiUserTokenExistsDTO.userTokenExists).isTrue();

    userTokenService.deleteUserTokenByUserCode(userToken.getUserCode());
    apiUserTokenExistsDTO = userTokenService.userTokenExistsForCurrentUser();
    assertThat(apiUserTokenExistsDTO).isNotNull();
    assertThat(apiUserTokenExistsDTO.userTokenExists).isFalse();
  }
}
