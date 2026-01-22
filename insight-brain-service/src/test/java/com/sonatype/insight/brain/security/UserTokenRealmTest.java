/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.security.UserTokenDAO;
import com.sonatype.insight.brain.model.security.Group;
import com.sonatype.insight.brain.model.security.OAuth2User;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.oauth2.OAuth2Realm;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;

import com.atlassian.crowd.exception.UserNotFoundException;
import com.google.inject.Binder;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.PrincipalCollection;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.USER_TOKEN_DEFAULT_EXPIRATION_DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserTokenRealmTest
    extends AbstractComponentTest
{
  @Inject
  private UserTokenRealm realm;

  @Inject
  private PasswordService passwordService;

  @Inject
  private UserTokenDAO userTokenDAO;

  @Inject
  private Configuration configuration;

  @Mock
  private ProductLicense mockProductLicense;

  @Mock
  private CrowdClientFactory mockCrowdClientFactory;

  @Override
  public void configure(Binder binder) {
    binder.bind(ProductLicense.class).toInstance(mockProductLicense);
    binder.bind(CrowdClientFactory.class).toInstance(mockCrowdClientFactory);
    super.configure(binder);
  }

  @Test
  public void testDoGetAuthenticationInfo() {
    String username = "JohnDoe";
    String userTokenPassword = "TestPassword";
    String hashedUserTokenPassword = passwordService.encryptPassword(userTokenPassword);
    User user = tempEntity.newUser(username);
    UserToken userToken = tempEntity.newUserToken(username, "TestUserCode", hashedUserTokenPassword, InternalRealm.ID);

    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(userToken.getUserCode(), userTokenPassword);
    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(usernamePasswordToken);
    PrincipalCollection principalCollection = authenticationInfo.getPrincipals();
    Iterator<?> principalIterator = principalCollection.iterator();
    Object principal = principalIterator.next();
    assertThat(principal).isEqualTo(new UserPrincipal(username, user.calculateDisplayName(), UserTokenRealm.ID));
    assertThat(principalIterator.hasNext()).isFalse();
    assertThat(principalCollection.getRealmNames()).containsExactlyInAnyOrder(realm.getName());
    assertThat(authenticationInfo.getCredentials()).isEqualTo(hashedUserTokenPassword);
  }

  @Test
  public void testDoGetAuthenticationInfo_NoUserToken() {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("Yeti", "TestPassword");
    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(usernamePasswordToken);
    assertThat(authenticationInfo).isNull();
  }

  @Test
  public void testDoGetAuthenticationInfo_WrongPassword() {
    String username = "JohnDoe";
    String userTokenPassword = "TestPassword";
    String hashedUserTokenPassword = passwordService.encryptPassword(userTokenPassword);
    User user = tempEntity.newUser(username);
    UserToken userToken = tempEntity.newUserToken(username, "TestUserCode", hashedUserTokenPassword, InternalRealm.ID);

    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(userToken.getUserCode(), "WrongPassword");
    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(usernamePasswordToken);
    PrincipalCollection principalCollection = authenticationInfo.getPrincipals();
    Iterator<?> principalIterator = principalCollection.iterator();
    Object principal = principalIterator.next();
    assertThat(principal).isEqualTo(new UserPrincipal(username, user.calculateDisplayName(), UserTokenRealm.ID));
    assertThat(principalIterator.hasNext()).isFalse();
    assertThat(principalCollection.getRealmNames()).containsExactlyInAnyOrder(realm.getName());
    // The credentials must be the hashed password from the db, not the (wrong) password or its hash passed in as param.
    assertThat(authenticationInfo.getCredentials()).isEqualTo(hashedUserTokenPassword);
  }

  @Test
  public void testDoGetAuthenticationInfo_NullUserName() {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(null /* username */, (char[]) null);
    assertThatExceptionOfType(AuthenticationException.class).isThrownBy(
        () -> realm.doGetAuthenticationInfo(usernamePasswordToken)).withMessage("The username is required");
  }

  @Test
  public void testDoGetAuthenticationInfo_EmptyUserName() {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(" " /* username */, (char[]) null);
    assertThatExceptionOfType(AuthenticationException.class).isThrownBy(
        () -> realm.doGetAuthenticationInfo(usernamePasswordToken)).withMessage("The username is required");
  }

  /**
   * testing public api
   * principal is populated with the username
   * expect that public access preserves the information populated privately; note we expect this, but don't really
   * care since we don't use the extra info
   */
  @Test
  public void testGetAuthenticationInfo() {
    String username = "JohnDoe";
    String userTokenPassword = "TestPassword";
    String hashedUserTokenPassword = passwordService.encryptPassword(userTokenPassword);
    User user = tempEntity.newUser(username);
    UserToken userToken = tempEntity.newUserToken(username, "TestUserCode", hashedUserTokenPassword, InternalRealm.ID);

    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(userToken.getUserCode(), userTokenPassword);
    AuthenticationInfo authenticationInfo = realm.getAuthenticationInfo(usernamePasswordToken);
    PrincipalCollection principalCollection = authenticationInfo.getPrincipals();
    Iterator<?> principalIterator = principalCollection.iterator();
    Object principal = principalIterator.next();
    assertThat(principal).isEqualTo(new UserPrincipal(username, user.calculateDisplayName(), UserTokenRealm.ID));
    assertThat(principalIterator.hasNext()).isFalse();
    assertThat(principalCollection.getRealmNames()).containsExactlyInAnyOrder(realm.getName());
    assertThat(authenticationInfo.getCredentials()).isEqualTo(hashedUserTokenPassword);
  }

  /**
   * testing public api
   * found credentials are checked, bad credentials indicated with exception
   * our class should not throw a different exception
   */
  @Test
  public void testGetAuthenticationInfo_WrongPassword() {
    String username = "JohnDoe";
    String userTokenPassword = "TestPassword";
    String hashedUserTokenPassword = passwordService.encryptPassword(userTokenPassword);
    tempEntity.newUser(username);
    UserToken userToken = tempEntity.newUserToken(username, "TestUserCode", hashedUserTokenPassword, InternalRealm.ID);

    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(userToken.getUserCode(), "WrongPassword");
    assertThatExceptionOfType(IncorrectCredentialsException.class).isThrownBy(
        () -> realm.getAuthenticationInfo(usernamePasswordToken));
  }

  @Test
  public void testGetAuthenticationInfo_Saml() {
    enableSsoWithSaml();

    String userTokenPassword = "TestPassword";
    String hashedUserTokenPassword = passwordService.encryptPassword(userTokenPassword);
    SamlUser samlUser = tempEntity.newSamlUser();
    UserToken userToken =
        tempEntity.newUserToken(samlUser.getUsername(), "TestUserCode", hashedUserTokenPassword, SamlRealm.ID);
    UsernamePasswordToken usernamePasswordToken =
        new UsernamePasswordToken(userToken.getUserCode(), userTokenPassword);

    AuthenticationInfo authenticationInfo = realm.getAuthenticationInfo(usernamePasswordToken);

    PrincipalCollection principalCollection = authenticationInfo.getPrincipals();
    Iterator<?> principalIterator = principalCollection.iterator();
    Object principal = principalIterator.next();
    Set<String> expectedGroups = new LinkedHashSet<>(samlUser.getGroups());
    expectedGroups.add(Group.AUTHENTICATED_USERS_GROUP_ID);
    assertThat(principal).usingRecursiveComparison().isEqualTo(
        new UserPrincipal(samlUser.getUsername(), samlUser.calculateDisplayName(), UserTokenRealm.ID, expectedGroups));
    assertThat(principalIterator.hasNext()).isFalse();
    assertThat(principalCollection.getRealmNames()).containsExactlyInAnyOrder(realm.getName());
    assertThat(authenticationInfo.getCredentials()).isEqualTo(hashedUserTokenPassword);
  }

  @Test
  public void testGetAuthenticationInfo_Saml_WrongPassword() {
    enableSsoWithSaml();

    String userTokenPassword = "TestPassword";
    String hashedUserTokenPassword = passwordService.encryptPassword(userTokenPassword);
    SamlUser samlUser = tempEntity.newSamlUser();
    UserToken userToken =
        tempEntity.newUserToken(samlUser.getUsername(), "TestUserCode", hashedUserTokenPassword, SamlRealm.ID);
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(userToken.getUserCode(), "WrongPassword");

    assertThatExceptionOfType(IncorrectCredentialsException.class).isThrownBy(
        () -> realm.getAuthenticationInfo(usernamePasswordToken));
  }

  @Test
  public void testDoGetAuthenticationInfo_Saml() {
    enableSsoWithSaml();

    String userTokenPassword = "TestPassword";
    String hashedUserTokenPassword = passwordService.encryptPassword(userTokenPassword);
    SamlUser samlUser = tempEntity.newSamlUser();
    UserToken userToken =
        tempEntity.newUserToken(samlUser.getUsername(), "TestUserCode", hashedUserTokenPassword, SamlRealm.ID);
    UsernamePasswordToken usernamePasswordToken =
        new UsernamePasswordToken(userToken.getUserCode(), userTokenPassword);

    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(usernamePasswordToken);

    PrincipalCollection principalCollection = authenticationInfo.getPrincipals();
    Iterator<?> principalIterator = principalCollection.iterator();
    Object principal = principalIterator.next();
    Set<String> expectedGroups = new LinkedHashSet<>(samlUser.getGroups());
    expectedGroups.add(Group.AUTHENTICATED_USERS_GROUP_ID);
    assertThat(principal).usingRecursiveComparison().isEqualTo(
        new UserPrincipal(samlUser.getUsername(), samlUser.calculateDisplayName(), UserTokenRealm.ID, expectedGroups));
    assertThat(principalIterator.hasNext()).isFalse();
    assertThat(principalCollection.getRealmNames()).containsExactlyInAnyOrder(realm.getName());
    assertThat(authenticationInfo.getCredentials()).isEqualTo(hashedUserTokenPassword);
  }

  @Test
  public void testDoGetAuthenticationInfo_Saml_WrongPassword() {
    enableSsoWithSaml();

    String userTokenPassword = "TestPassword";
    String hashedUserTokenPassword = passwordService.encryptPassword(userTokenPassword);
    SamlUser samlUser = tempEntity.newSamlUser();
    UserToken userToken =
        tempEntity.newUserToken(samlUser.getUsername(), "TestUserCode", hashedUserTokenPassword, SamlRealm.ID);
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(userToken.getUserCode(), "WrongPassword");

    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(usernamePasswordToken);

    PrincipalCollection principalCollection = authenticationInfo.getPrincipals();
    Iterator<?> principalIterator = principalCollection.iterator();
    Object principal = principalIterator.next();
    Set<String> expectedGroups = new LinkedHashSet<>(samlUser.getGroups());
    expectedGroups.add(Group.AUTHENTICATED_USERS_GROUP_ID);
    assertThat(principal).usingRecursiveComparison().isEqualTo(
        new UserPrincipal(samlUser.getUsername(), samlUser.calculateDisplayName(), UserTokenRealm.ID, expectedGroups));
    assertThat(principalIterator.hasNext()).isFalse();
    assertThat(principalCollection.getRealmNames()).containsExactlyInAnyOrder(realm.getName());
    assertThat(authenticationInfo.getCredentials()).isEqualTo(hashedUserTokenPassword);
  }

  @Test
  public void testGetAuthenticationInfo_OAuth2() {
    enableSsoWithOAuth2();

    String userTokenPassword = "TestPassword";
    String hashedUserTokenPassword = passwordService.encryptPassword(userTokenPassword);
    OAuth2User oauth2User = tempEntity.newOAuth2User();
    UserToken userToken =
        tempEntity.newUserToken(oauth2User.getUsername(), "TestUserCode", hashedUserTokenPassword, OAuth2Realm.ID);
    UsernamePasswordToken usernamePasswordToken =
        new UsernamePasswordToken(userToken.getUserCode(), userTokenPassword);

    AuthenticationInfo authenticationInfo = realm.getAuthenticationInfo(usernamePasswordToken);

    PrincipalCollection principalCollection = authenticationInfo.getPrincipals();
    Iterator<?> principalIterator = principalCollection.iterator();
    Object principal = principalIterator.next();
    Set<String> expectedGroups = new LinkedHashSet<>(oauth2User.getGroups());
    expectedGroups.add(Group.AUTHENTICATED_USERS_GROUP_ID);
    assertThat(principal).usingRecursiveComparison().isEqualTo(
        new UserPrincipal(oauth2User.getUsername(), oauth2User.calculateDisplayName(), UserTokenRealm.ID,
            expectedGroups));
    assertThat(principalIterator.hasNext()).isFalse();
    assertThat(principalCollection.getRealmNames()).containsExactlyInAnyOrder(realm.getName());
    assertThat(authenticationInfo.getCredentials()).isEqualTo(hashedUserTokenPassword);
  }

  @Test
  public void testGetAuthenticationInfo_OAuth2_WrongPassword() {
    enableSsoWithOAuth2();

    String userTokenPassword = "TestPassword";
    String hashedUserTokenPassword = passwordService.encryptPassword(userTokenPassword);
    OAuth2User oauth2User = tempEntity.newOAuth2User();
    UserToken userToken =
        tempEntity.newUserToken(oauth2User.getUsername(), "TestUserCode", hashedUserTokenPassword, OAuth2Realm.ID);
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(userToken.getUserCode(), "WrongPassword");

    assertThatExceptionOfType(IncorrectCredentialsException.class).isThrownBy(
        () -> realm.getAuthenticationInfo(usernamePasswordToken));
  }

  @Test
  public void testDoGetAuthenticationInfo_OAuth2() {
    enableSsoWithOAuth2();

    String userTokenPassword = "TestPassword";
    String hashedUserTokenPassword = passwordService.encryptPassword(userTokenPassword);
    OAuth2User oauth2User = tempEntity.newOAuth2User();
    UserToken userToken =
        tempEntity.newUserToken(oauth2User.getUsername(), "TestUserCode", hashedUserTokenPassword, OAuth2Realm.ID);
    UsernamePasswordToken usernamePasswordToken =
        new UsernamePasswordToken(userToken.getUserCode(), userTokenPassword);

    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(usernamePasswordToken);

    PrincipalCollection principalCollection = authenticationInfo.getPrincipals();
    Iterator<?> principalIterator = principalCollection.iterator();
    Object principal = principalIterator.next();
    Set<String> expectedGroups = new LinkedHashSet<>(oauth2User.getGroups());
    expectedGroups.add(Group.AUTHENTICATED_USERS_GROUP_ID);
    assertThat(principal).usingRecursiveComparison().isEqualTo(
        new UserPrincipal(oauth2User.getUsername(), oauth2User.calculateDisplayName(), UserTokenRealm.ID,
            expectedGroups));
    assertThat(principalIterator.hasNext()).isFalse();
    assertThat(principalCollection.getRealmNames()).containsExactlyInAnyOrder(realm.getName());
    assertThat(authenticationInfo.getCredentials()).isEqualTo(hashedUserTokenPassword);
  }

  @Test
  public void testDoGetAuthenticationInfo_OAuth2_WrongPassword() {
    enableSsoWithOAuth2();

    String userTokenPassword = "TestPassword";
    String hashedUserTokenPassword = passwordService.encryptPassword(userTokenPassword);
    OAuth2User oauth2User = tempEntity.newOAuth2User();
    UserToken userToken =
        tempEntity.newUserToken(oauth2User.getUsername(), "TestUserCode", hashedUserTokenPassword, OAuth2Realm.ID);
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(userToken.getUserCode(), "WrongPassword");

    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(usernamePasswordToken);

    PrincipalCollection principalCollection = authenticationInfo.getPrincipals();
    Iterator<?> principalIterator = principalCollection.iterator();
    Object principal = principalIterator.next();
    Set<String> expectedGroups = new LinkedHashSet<>(oauth2User.getGroups());
    expectedGroups.add(Group.AUTHENTICATED_USERS_GROUP_ID);
    assertThat(principal).usingRecursiveComparison().isEqualTo(
        new UserPrincipal(oauth2User.getUsername(), oauth2User.calculateDisplayName(), UserTokenRealm.ID,
            expectedGroups));
    assertThat(principalIterator.hasNext()).isFalse();
    assertThat(principalCollection.getRealmNames()).containsExactlyInAnyOrder(realm.getName());
    assertThat(authenticationInfo.getCredentials()).isEqualTo(hashedUserTokenPassword);
  }

  @Test
  public void testDoGetCrowdRealmAuthenticationInfo_Crowd_NullCrowdClient() {
    String userTokenPassword = "TestPassword";
    String hashedUserTokenPassword = passwordService.encryptPassword(userTokenPassword);
    UserToken userToken = tempEntity.newUserToken("username", "TestUserCode", hashedUserTokenPassword, CrowdRealm.ID);
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(userToken.getUserCode(), userTokenPassword);

    assertThat(realm.doGetAuthenticationInfo(usernamePasswordToken)).isNull();
    verify(mockCrowdClientFactory).createCrowdClient();
  }

  @Test
  public void testDoGetCrowdRealmAuthenticationInfo_Crowd() throws Exception {
    String userTokenPassword = "TestPassword";
    String hashedUserTokenPassword = passwordService.encryptPassword(userTokenPassword);
    UserToken userToken = tempEntity.newUserToken("username", "TestUserCode", hashedUserTokenPassword, CrowdRealm.ID);
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(userToken.getUserCode(), userTokenPassword);
    CrowdClient mockCrowdClient = mock(CrowdClient.class);
    UserPrincipal mockUserPrincipal = mock(UserPrincipal.class);
    when(mockCrowdClient.getUser(any(UserToken.class))).thenReturn(mockUserPrincipal);
    when(mockCrowdClientFactory.createCrowdClient()).thenReturn(mockCrowdClient);

    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(usernamePasswordToken);

    assertThat(authenticationInfo.getCredentials()).isEqualTo(hashedUserTokenPassword);
    UserPrincipal userPrincipal = getUserPrincipal(authenticationInfo);
    assertThat(userPrincipal).isEqualTo(mockUserPrincipal);
    verify(mockCrowdClientFactory).createCrowdClient();
    ArgumentCaptor<UserToken> userTokenArgumentCaptor = ArgumentCaptor.forClass(UserToken.class);
    verify(mockCrowdClient).getUser(userTokenArgumentCaptor.capture());
    assertThat(userTokenArgumentCaptor.getValue()).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(userToken);
  }

  @Test
  public void testDoGetCrowdRealmAuthenticationInfo_Crowd_UserNotFound() throws Exception {
    String userTokenPassword = "TestPassword";
    String hashedUserTokenPassword = passwordService.encryptPassword(userTokenPassword);
    UserToken userToken = tempEntity.newUserToken("username", "TestUserCode", hashedUserTokenPassword, CrowdRealm.ID);
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(userToken.getUserCode(), userTokenPassword);
    CrowdClient mockCrowdClient = mock(CrowdClient.class);
    when(mockCrowdClient.getUser(any(UserToken.class))).thenThrow(
        new UserNotFoundException(userToken.getUsername()));
    when(mockCrowdClientFactory.createCrowdClient()).thenReturn(mockCrowdClient);

    assertThatExceptionOfType(AuthenticationException.class).isThrownBy(
        () -> realm.doGetAuthenticationInfo(usernamePasswordToken)).withMessageContaining("Invalid user token.");
    assertThat(userTokenDAO.getById(userToken.getId())).isNull();
  }
  
  @Test
  public void testDoGetCrowdRealmAuthenticationInfo_Crowd_Error() throws Exception {
    String userTokenPassword = "TestPassword";
    String hashedUserTokenPassword = passwordService.encryptPassword(userTokenPassword);
    UserToken userToken = tempEntity.newUserToken("username", "TestUserCode", hashedUserTokenPassword, CrowdRealm.ID);
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(userToken.getUserCode(), userTokenPassword);
    CrowdClient mockCrowdClient = mock(CrowdClient.class);
    when(mockCrowdClient.getUser(any(UserToken.class))).thenThrow(new RuntimeException("SomeError"));
    when(mockCrowdClientFactory.createCrowdClient()).thenReturn(mockCrowdClient);

    assertThatExceptionOfType(AuthenticationException.class).isThrownBy(
        () -> realm.doGetAuthenticationInfo(usernamePasswordToken)).withMessageContaining(
        String.format("Could not authenticate the '%s' Crowd user with their '%s' user token.", userToken.getUsername(),
            userToken.getUserCode())).withStackTraceContaining("SomeError");
  }

  @Test
  public void testGetAuthenticationInfo_ExpiredToken() {
    tempEntity.newSystemConfigurationProperty(USER_TOKEN_DEFAULT_EXPIRATION_DAYS, "30");
    configuration.configurationChanged(
        Set.of(USER_TOKEN_DEFAULT_EXPIRATION_DAYS));
    UserToken userToken = createExpiredUserToken("JohnDoe");
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(userToken.getUserCode(), "TestPassword");
    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> realm.getAuthenticationInfo(usernamePasswordToken))
        .withMessage("User token has expired. Please generate a new token.");
  }

  @Test
  public void testGetAuthenticationInfo_NonExpiredToken() {
    UserToken userToken = createNonExpiredUserToken("JohnDoe");
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(userToken.getUserCode(), "TestPassword");
    AuthenticationInfo authenticationInfo = realm.getAuthenticationInfo(usernamePasswordToken);
    assertThat(authenticationInfo).isNotNull();
  }

  @Test
  public void testDoGetAuthenticationInfo_ExpiredToken() {
    tempEntity.newSystemConfigurationProperty(USER_TOKEN_DEFAULT_EXPIRATION_DAYS, "30");
    configuration.configurationChanged(Set.of(USER_TOKEN_DEFAULT_EXPIRATION_DAYS));
    UserToken userToken = createExpiredUserToken("JaneDoe");
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(userToken.getUserCode(), "TestPassword");
    assertThatExceptionOfType(ExpiredUserTokenException.class)
        .isThrownBy(() -> realm.doGetAuthenticationInfo(usernamePasswordToken))
        .withMessage("User token has expired. Please generate a new token.");
  }

  private UserPrincipal getUserPrincipal(AuthenticationInfo authenticationInfo) {
    assertThat(authenticationInfo).isInstanceOf(SimpleAuthenticationInfo.class);
    SimpleAuthenticationInfo simpleAuthenticationInfo = (SimpleAuthenticationInfo) authenticationInfo;
    assertThat(simpleAuthenticationInfo.getPrincipals()).isNotEmpty();
    assertThat(simpleAuthenticationInfo.getPrincipals().getRealmNames()).containsExactly(realm.getName());
    Object primaryPrincipal = simpleAuthenticationInfo.getPrincipals().getPrimaryPrincipal();
    assertThat(primaryPrincipal).isInstanceOf(UserPrincipal.class);
    return (UserPrincipal) primaryPrincipal;
  }

  private UserToken createExpiredUserToken(String username) {
    String hashedPassword = passwordService.encryptPassword("TestPassword");
    tempEntity.newUser(username);
    // Create token with create_time set to 31 days ago (expired with 30 day default expiration)
    Instant createTime = Instant.now().minus(31, ChronoUnit.DAYS);
    return tempEntity.newUserToken(username, "TestUserCode", hashedPassword, InternalRealm.ID,
        Date.from(createTime));
  }

  private UserToken createNonExpiredUserToken(String username) {
    String hashedPassword = passwordService.encryptPassword("TestPassword");
    tempEntity.newUser(username);
    // Token created with current time, so it won't be expired (within 30 day default expiration)
    return tempEntity.newUserToken(username, "TestUserCode", hashedPassword, InternalRealm.ID);
  }
}
