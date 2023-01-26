/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.api.v2.dto.ApiUserTokenDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiUserTokenExistsDTO;
import com.sonatype.insight.brain.configuration.ldap.TestLdapServer;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.dataaccess.security.UserTokenDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.inject.Binder;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
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

  @Mock
  private ProductLicense mockProductLicense;

  private SamlUserDAO spySamlUserDAO;

  @Override
  public void configure(Binder binder) {
    spySamlUserDAO = spy(new SamlUserDAO());
    binder.bind(ProductLicense.class).toInstance(mockProductLicense);
    binder.bind(SamlUserDAO.class).toInstance(spySamlUserDAO);
    super.configure(binder);
  }

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
  public void testCreateUserToken_SamlUser_DoesNotExist() {
    SamlUser samlUser = new SamlUser("someUsername", "someFirstName", "someLastName", "someEmail@someDomain.com",
        new LinkedHashSet<>(Arrays.asList("someGroup1", "someGroup2")));
    when(subject.getPrincipal()).thenReturn(
        new UserPrincipal(samlUser.getUsername(), samlUser.calculateDisplayName(), SamlUser.SAML_REALM_ID,
            samlUser.getGroups()));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> userTokenService.createUserToken())
        .withMessageContaining("Unable to get user session details, you must relogin before generating a user token.");
  }

  @Test
  public void testCreateUserToken_SamlUser_Exists() {
    SamlUser samlUser = tempEntity.newSamlUser();
    try {
      when(subject.getPrincipal()).thenReturn(
          new UserPrincipal(samlUser.getUsername(), samlUser.calculateDisplayName(), SamlUser.SAML_REALM_ID,
              samlUser.getGroups()));
      Date start = new Date();

      ApiUserTokenDTO apiUserTokenDTO = userTokenService.createUserToken();

      Date end = new Date();
      assertThat(apiUserTokenDTO.userCode).hasSize(8);
      assertThat(apiUserTokenDTO.passCode).hasSize(44);
      UserToken persistedToken = userTokenDAO.getByUsernameAndRealmId(samlUser.getUsername(), SamlUser.SAML_REALM_ID);
      assertThat(persistedToken).isNotNull();
      assertThat(persistedToken.getUsername()).isEqualTo(samlUser.getUsername());
      assertThat(persistedToken.getUserCode()).isEqualTo(apiUserTokenDTO.userCode);
      assertThat(persistedToken.getPassCode()).isNotNull();
      assertThat(persistedToken.getRealmId()).isEqualTo(SamlUser.SAML_REALM_ID);
      assertThat(persistedToken.getCreateTime()).isBetween(start, end, true, true);
      assertThat(persistedToken.isInternalUser()).isFalse();
      assertThat(persistedToken.getPassCode()).isNotEqualTo(apiUserTokenDTO.passCode);

      assertThat(spySamlUserDAO.getByUsername(samlUser.getUsername())).usingRecursiveComparison().ignoringFields(
          JPA.IGNORE_FIELDS).isEqualTo(samlUser);
      verify(spySamlUserDAO, never()).insert(any(), any());
    }
    finally {
      userTokenDAO.delete(userTokenDAO.getByUsernameAndRealmId(samlUser.getUsername(), SamlUser.SAML_REALM_ID));
    }
  }

  @Test
  public void testCreateUserToken_ReverseProxyUser() {
    testCreateUserToken_NotAllowedRealm(ReverseProxyRealm.ID);
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

  private void assertUserTokens(List<ApiUserTokenDTO> userTokenDTOs, UserToken token) {
    assertThat(userTokenDTOs)
        .hasSize(1)
        .extracting("userCode", "username", "realm")
        .containsExactlyInAnyOrder(tuple(token.getUserCode(), token.getUsername(), token.getRealmId()));

    // Assert passcode are not returned
    assertThat(userTokenDTOs)
        .extracting(apiUserTokenDTO -> apiUserTokenDTO.passCode)
        .filteredOn(Objects::nonNull)
        .isEmpty();
  }

  @Test
  public void testGetUserTokensByCreatedBetweenAndRealmId() {
    tempEntity.newUserToken("foo", User.INTERNAL_REALM_ID, december27);
    UserToken bar = tempEntity.newUserToken("bar", User.INTERNAL_REALM_ID, december28);
    tempEntity.newUserToken("baz", User.INTERNAL_REALM_ID, december29);
    UserToken qux = tempEntity.newUserToken("qux", SamlUser.SAML_REALM_ID, december28);
    tempEntity.newUserToken("sam27", SamlUser.SAML_REALM_ID, december27);
    tempEntity.newUserToken("sam29", SamlUser.SAML_REALM_ID, december29);

    List<ApiUserTokenDTO> userTokenDTOs =
        userTokenService.getUserTokensCreatedBetweenAndRealmId("2019-12-28", "2019-12-28", "iNTeRnaL");
    assertUserTokens(userTokenDTOs, bar);

    userTokenDTOs =
        userTokenService.getUserTokensCreatedBetweenAndRealmId("2019-12-28", "2019-12-28", "unKnOWn");
    assertUserTokens(userTokenDTOs, bar);

    userTokenDTOs =
        userTokenService.getUserTokensCreatedBetweenAndRealmId("2019-12-28", "2019-12-28", null);
    assertUserTokens(userTokenDTOs, bar);

    userTokenDTOs =
        userTokenService.getUserTokensCreatedBetweenAndRealmId("2019-12-28", "2019-12-28", "sAMl");
    assertUserTokens(userTokenDTOs, qux);
  }

  @Test
  public void testGetUserTokensByCreatedBetweenAndRealmId_MustHandleNullArguments() {
    tempEntity.newUserToken("foo", december27);
    UserToken sam1 = tempEntity.newUserToken("sam1", SamlUser.SAML_REALM_ID, december27);
    tempEntity.newUserToken("bar", december28);
    UserToken sam2 = tempEntity.newUserToken("sam2", SamlUser.SAML_REALM_ID, december28);
    tempEntity.newUserToken("baz", december29);
    UserToken sam3 = tempEntity.newUserToken("sam3", SamlUser.SAML_REALM_ID, december29);
    tempEntity.newUserToken("qux", december30);
    UserToken sam4 = tempEntity.newUserToken("sam4", SamlUser.SAML_REALM_ID, december30);

    // Assert all user tokens are returned with user code
    List<ApiUserTokenDTO> userTokenDTOs = userTokenService.getUserTokensCreatedBetweenAndRealmId(null, null, "SAML");
    assertGetUserTokensResults(userTokenDTOs, sam1, sam2, sam3, sam4);
  }

  private void assertGetUserTokensResults(
      final List<ApiUserTokenDTO> userTokenDTOs,
      final UserToken t1,
      final UserToken t2,
      final UserToken t3,
      final UserToken t4)
  {
    assertThat(userTokenDTOs)
        .extracting("userCode", "username")
        .containsExactlyInAnyOrder(tuple(t1.getUserCode(), t1.getUsername()),
            tuple(t2.getUserCode(), t2.getUsername()), tuple(t3.getUserCode(), t3.getUsername()),
            tuple(t4.getUserCode(), t4.getUsername()));

    // Assert passcode are not returned
    assertThat(userTokenDTOs)
        .extracting(apiUserTokenDTO -> apiUserTokenDTO.passCode)
        .filteredOn(Objects::nonNull)
        .isEmpty();
  }

  @Test
  public void testGetUserTokensCreatedBetween_CanNotParseCreatedAfter() {
    assertThatThrownBy(() -> userTokenService.getUserTokensCreatedBetweenAndRealmId("foo", null, null))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Could not parse: foo. Expected format is: yyyy-MM-dd.");
  }

  @Test
  public void testGetUserTokensCreatedBetween_CanNotParseCreatedBefore() {
    assertThatThrownBy(() -> userTokenService.getUserTokensCreatedBetweenAndRealmId(null, "bar", null))
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

  @Test
  public void testGetUserTokenByUsernameAndRealmId_NullUsernameAndNullRealmId() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> userTokenService.getUserTokenByUsernameAndRealmId(null, null))
        .withMessageContaining("A username is required.");
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_NullUsername() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> userTokenService.getUserTokenByUsernameAndRealmId(null, User.INTERNAL_REALM_ID))
        .withMessageContaining("A username is required.");
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_EmptyUsername() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> userTokenService.getUserTokenByUsernameAndRealmId("", User.INTERNAL_REALM_ID))
        .withMessageContaining("A username is required.");
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_BlankUsername() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> userTokenService.getUserTokenByUsernameAndRealmId(" ", User.INTERNAL_REALM_ID))
        .withMessageContaining("A username is required.");
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_CrowdIntegrationDisabled_NullRealmId() {
    testGetUserTokenByUsernameAndRealmId(null);
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_CrowdIntegrationDisabled_UnknownRealmId() {
    testGetUserTokenByUsernameAndRealmId("unknown");
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_CrowdIntegrationDisabled_InternalRealmId() {
    testGetUserTokenByUsernameAndRealmId("InTeRnAl");
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_CrowdIntegrationDisabled_SamlRealmId() {
    testGetUserTokenByUsernameAndRealmId("SaMl");
  }

  private void testGetUserTokenByUsernameAndRealmId(String realmId) {
    SystemConfigurationPropertyFeature.CROWD_INTEGRATION.setEnabled(false);
    String usernameToQuery = "username1";
    tempEntity.newUser("username1");
    UserToken internalUserToken1 =
        tempEntity.newUserToken("username1", "userCode1", "passCode", User.INTERNAL_REALM_ID);
    tempEntity.newUser("username2");
    tempEntity.newUserToken("username2", User.INTERNAL_REALM_ID);
    UserToken samlUserToken1 = tempEntity.newUserToken("username1", "userCode2", "passCode", SamlUser.SAML_REALM_ID);
    tempEntity.newUserToken("username2", "userCode3", "passCode", SamlUser.SAML_REALM_ID);
    tempEntity.newUserToken("username1", "userCode4", "passCode", "other");
    tempEntity.newUserToken("username2", "userCode5", "passCode", "other");

    if (!SamlUser.SAML_REALM_ID.equalsIgnoreCase(realmId)) {
      usernameToQuery = usernameToQuery.toUpperCase(Locale.ENGLISH);
    }

    ApiUserTokenDTO result = userTokenService.getUserTokenByUsernameAndRealmId(usernameToQuery, realmId);

    String expectedRealmId;
    if (SamlUser.SAML_REALM_ID.equalsIgnoreCase(realmId)) {
      assertThat(result.userCode).isEqualTo(samlUserToken1.getUserCode());
      expectedRealmId = SamlUser.SAML_REALM_ID;
    }
    else {
      assertThat(result.userCode).isEqualTo(internalUserToken1.getUserCode());
      expectedRealmId = User.INTERNAL_REALM_ID;
    }
    assertThat(result.passCode).isNull();
    assertThat(result.username).isEqualTo("username1");
    assertThat(result.realm).isEqualTo(expectedRealmId);
  }

  @Test
  public void testCreateUserToken_CrowdUser_CrowdIntegrationFeatureDisabled() {
    SystemConfigurationPropertyFeature.CROWD_INTEGRATION.setEnabled(false);
    testCreateUserToken_NotAllowedRealm(CrowdRealm.ID);
  }

  @Test
  public void testCreateUserToken_CrowdUser() {
    UserPrincipal userPrincipal = new UserPrincipal("username", "displayName", CrowdRealm.ID, Collections.emptySet());
    when(subject.getPrincipal()).thenReturn(userPrincipal);
    Date start = new Date();
    try {
      ApiUserTokenDTO apiUserTokenDTO = userTokenService.createUserToken();

      Date end = new Date();
      assertThat(apiUserTokenDTO.userCode).hasSize(8);
      assertThat(apiUserTokenDTO.passCode).hasSize(44);
      UserToken persistedToken = userTokenDAO.getByUsernameAndRealmId(userPrincipal.getUsername(), CrowdRealm.ID);
      assertThat(persistedToken).isNotNull();
      assertThat(persistedToken.getUsername()).isEqualTo(userPrincipal.getUsername());
      assertThat(persistedToken.getUserCode()).isEqualTo(apiUserTokenDTO.userCode);
      assertThat(persistedToken.getPassCode()).isNotNull();
      assertThat(persistedToken.getRealmId()).isEqualTo(CrowdRealm.ID);
      assertThat(persistedToken.getCreateTime()).isBetween(start, end, true, true);
      assertThat(persistedToken.isInternalUser()).isFalse();
      assertThat(persistedToken.getPassCode()).isNotEqualTo(apiUserTokenDTO.passCode);
    }
    finally {
      userTokenDAO.delete(userTokenDAO.getByUsernameAndRealmId(userPrincipal.getUsername(), CrowdRealm.ID));
    }
  }
  
  @Test
  public void testGetUserTokensCreatedBetweenAndRealmId_Crowd_CrowdIntegrationFeatureDisabled() {
    SystemConfigurationPropertyFeature.CROWD_INTEGRATION.setEnabled(false);
    tempEntity.newUserToken("foo", User.INTERNAL_REALM_ID, december27);
    UserToken internal2 = tempEntity.newUserToken("bar", User.INTERNAL_REALM_ID, december28);
    tempEntity.newUserToken("qux", CrowdRealm.ID, december28);
    tempEntity.newUserToken("baz", User.INTERNAL_REALM_ID, december29);

    List<ApiUserTokenDTO> result =
        userTokenService.getUserTokensCreatedBetweenAndRealmId("2019-12-28", "2019-12-28", User.INTERNAL_REALM_ID);

    assertThat(result).extracting(dto -> dto.username).containsExactly(internal2.getUsername());
    
    result =
        userTokenService.getUserTokensCreatedBetweenAndRealmId("2019-12-28", "2019-12-28", CrowdRealm.ID);
    
    assertThat(result).extracting(dto -> dto.username).containsExactly(internal2.getUsername());
  }

  @Test
  public void testGetUserTokensCreatedBetweenAndRealmId_Crowd() {
    tempEntity.newUserToken("foo", User.INTERNAL_REALM_ID, december27);
    UserToken internal2 = tempEntity.newUserToken("bar", User.INTERNAL_REALM_ID, december28);
    UserToken crowd1 = tempEntity.newUserToken("qux", CrowdRealm.ID, december28);
    tempEntity.newUserToken("baz", User.INTERNAL_REALM_ID, december29);

    List<ApiUserTokenDTO> result =
        userTokenService.getUserTokensCreatedBetweenAndRealmId("2019-12-28", "2019-12-28", User.INTERNAL_REALM_ID);

    assertThat(result).extracting(dto -> dto.username).containsExactly(internal2.getUsername());

    result =
        userTokenService.getUserTokensCreatedBetweenAndRealmId("2019-12-28", "2019-12-28", CrowdRealm.ID);

    assertThat(result).extracting(dto -> dto.username).containsExactly(crowd1.getUsername());
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_Crowd_CrowdIntegrationFeatureDisabled() {
    SystemConfigurationPropertyFeature.CROWD_INTEGRATION.setEnabled(false);
    tempEntity.newUserToken("foo", "1", "pass", User.INTERNAL_REALM_ID);
    tempEntity.newUserToken("bar", "2", "pass", User.INTERNAL_REALM_ID);
    tempEntity.newUserToken("foo", "3", "pass", CrowdRealm.ID);
    tempEntity.newUserToken("bar", "4", "pass", CrowdRealm.ID);
    tempEntity.newUserToken("foo", "5", "pass", "other");
    tempEntity.newUserToken("bar", "6", "pass", "other");

    ApiUserTokenDTO result = userTokenService.getUserTokenByUsernameAndRealmId("foo", CrowdRealm.ID);

    assertThat(result).isNotNull();
    assertThat(result.username).isEqualTo("foo");
    assertThat(result.userCode).isEqualTo("1");
    assertThat(result.realm).isEqualTo(User.INTERNAL_REALM_ID);
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_Crowd() {
    tempEntity.newUserToken("foo", "1", "pass", User.INTERNAL_REALM_ID);
    tempEntity.newUserToken("bar", "2", "pass", User.INTERNAL_REALM_ID);
    tempEntity.newUserToken("foo", "3", "pass", CrowdRealm.ID);
    tempEntity.newUserToken("bar", "4", "pass", CrowdRealm.ID);
    tempEntity.newUserToken("foo", "5", "pass", "other");
    tempEntity.newUserToken("bar", "6", "pass", "other");

    ApiUserTokenDTO result = userTokenService.getUserTokenByUsernameAndRealmId("foo", CrowdRealm.ID);

    assertThat(result).isNotNull();
    assertThat(result.username).isEqualTo("foo");
    assertThat(result.userCode).isEqualTo("3");
    assertThat(result.realm).isEqualTo(CrowdRealm.ID);
  }
}
