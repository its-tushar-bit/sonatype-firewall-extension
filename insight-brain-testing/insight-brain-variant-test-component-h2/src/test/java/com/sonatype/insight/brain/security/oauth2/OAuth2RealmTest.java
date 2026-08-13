/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.oauth2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OAuth2ConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.security.OAuth2GroupDAO;
import com.sonatype.insight.brain.dataaccess.security.OAuth2UserDAO;
import com.sonatype.insight.brain.model.configuration.oauth2.OAuth2Configuration;
import com.sonatype.insight.brain.model.security.OAuth2Group;
import com.sonatype.insight.brain.model.security.OAuth2User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.subject.PrincipalCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class OAuth2RealmTest
    extends AbstractComponentH2Test
{
  @Inject
  private OAuth2Realm realm;

  @Inject
  private JWTGenerator jwtGenerator;

  @Inject
  private OAuth2ConfigurationDAO oAuth2ConfigurationDAO;

  @Inject
  private OAuth2UserDAO oAuth2UserDAO;

  @Inject
  private OAuth2GroupDAO oAuth2GroupDAO;

  @BeforeEach
  public void enableFeature() {
    enableSsoWithOAuth2();
  }

  @Test
  public void testDoGetAuthenticationInfo() {
    final String sub = "bob";
    final String issuer = "https://an-idp.com";
    final String username = "bob-the-ruler";
    final String firstName = "Bob";
    final String lastName = "Sanders";
    final String email = "bob@company.com";
    final List<String> groups = Arrays.asList("admin", "dev", "other");

    Map<String, Object> claims =
        getCustomClaimsAndConfigureOAuth2(issuer, username, firstName, lastName, email, groups);
    String token = jwtGenerator.generateJWT(sub, issuer, claims);
    ShiroJsonWebToken shiroJsonWebToken = new ShiroJsonWebToken(token);

    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(shiroJsonWebToken);

    assertAuthenticationInfoIsTheExpected(username, "Bob Sanders", groups, shiroJsonWebToken, authenticationInfo);
    assertOAuth2UserIsTheExpected(username, firstName, lastName, email, groups);
    assertOAuth2GroupsAreCreated(groups);
  }

  @Test
  public void testDoGetAuthenticationInfo_WithDefaultOidcClaims() {
    final String sub = "bob";
    final String issuer = "https://an-idp.com";
    final String username = "bob-the-ruler";
    final String firstName = "Bob";
    final String lastName = "Sanders";
    final String email = "bob@company.com";
    final List<String> groups = Arrays.asList("admin", "dev", "other");

    tempEntity.newOAuth2Configuration(issuer, jwtGenerator.getJwsAlgorithm(), null, jwtGenerator.getJWKSetString());
    Map<String, Object> claims = jwtGenerator.getStandardCustomClaims(username, firstName, lastName, email, groups);
    String token = jwtGenerator.generateJWT(sub, issuer, claims);
    ShiroJsonWebToken shiroJsonWebToken = new ShiroJsonWebToken(token);

    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(shiroJsonWebToken);

    assertAuthenticationInfoIsTheExpected(username, "Bob Sanders", groups, shiroJsonWebToken, authenticationInfo);
    assertOAuth2UserIsTheExpected(username, firstName, lastName, email, groups);
    assertOAuth2GroupsAreCreated(groups);
  }

  @Test
  public void testDoGetAuthenticationInfo_DisplayNameIsUsername() {
    final String sub = "bob";
    final String issuer = "https://an-idp.com";
    final String username = "bob-the-ruler";
    final String firstName = "";
    final String lastName = "";
    final String email = "bob@company.com";
    final List<String> groups = Arrays.asList("admin", "dev", "other");

    Map<String, Object> claims =
        getCustomClaimsAndConfigureOAuth2(issuer, username, firstName, lastName, email, groups);
    String token = jwtGenerator.generateJWT(sub, issuer, claims);
    ShiroJsonWebToken shiroJsonWebToken = new ShiroJsonWebToken(token);

    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(shiroJsonWebToken);

    assertAuthenticationInfoIsTheExpected(username, username, groups, shiroJsonWebToken, authenticationInfo);
    assertOAuth2UserIsTheExpected(username, firstName, lastName, email, groups);
    assertOAuth2GroupsAreCreated(groups);
  }

  @Test
  public void testDoGetAuthenticationInfo_UsernameIsTheEmail() {
    final String sub = "bob";
    final String issuer = "https://an-idp.com";
    final String email = "bob@company.com";
    final List<String> groups = Arrays.asList("admin", "dev", "other");

    Map<String, Object> claims =
        getCustomClaimsAndConfigureOAuth2(issuer, "", "", "", email, groups);
    String token = jwtGenerator.generateJWT(sub, issuer, claims);
    ShiroJsonWebToken shiroJsonWebToken = new ShiroJsonWebToken(token);

    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(shiroJsonWebToken);

    assertAuthenticationInfoIsTheExpected(email, email, groups, shiroJsonWebToken, authenticationInfo);
    assertOAuth2UserIsTheExpected(email, "", "", email, groups);
    assertOAuth2GroupsAreCreated(groups);
  }

  @Test
  public void testDoGetAuthenticationInfo_UsernameIsTheSubject() {
    final String sub = "bob";
    final String issuer = "https://an-idp.com";
    final List<String> groups = Arrays.asList("admin", "dev", "other");

    Map<String, Object> claims =
        getCustomClaimsAndConfigureOAuth2(issuer, "", "", "", "", groups);
    String token = jwtGenerator.generateJWT(sub, issuer, claims);
    ShiroJsonWebToken shiroJsonWebToken = new ShiroJsonWebToken(token);

    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(shiroJsonWebToken);

    assertAuthenticationInfoIsTheExpected(sub, sub, groups, shiroJsonWebToken, authenticationInfo);
    assertOAuth2UserIsTheExpected(sub, "", "", "", groups);
    assertOAuth2GroupsAreCreated(groups);
  }

  @Test
  public void testDoGetAuthenticationInfo_NoGroups() {
    final String sub = "bob";
    final String issuer = "https://an-idp.com";
    final List<String> groups = new ArrayList<>();

    Map<String, Object> claims =
        getCustomClaimsAndConfigureOAuth2(issuer, "", "", "", "", groups);
    String token = jwtGenerator.generateJWT(sub, issuer, claims);
    ShiroJsonWebToken shiroJsonWebToken = new ShiroJsonWebToken(token);

    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(shiroJsonWebToken);

    assertAuthenticationInfoIsTheExpected(sub, sub, groups, shiroJsonWebToken, authenticationInfo);
    assertOAuth2UserIsTheExpected(sub, "", "", "", groups);
    assertThat(oAuth2GroupDAO.getAll()).isEmpty();
  }

  @Test
  public void testDoGetAuthenticationInfo_NoOAuth2Configuration_SubjectIsUsername() {
    final String sub = "bob";
    final String issuer = "https://an-idp.com";

    String token = jwtGenerator.generateJWT(sub, issuer);
    ShiroJsonWebToken shiroJsonWebToken = new ShiroJsonWebToken(token);

    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(shiroJsonWebToken);

    assertAuthenticationInfoIsTheExpected(sub, sub, Arrays.asList("(all-authenticated-users)"), shiroJsonWebToken,
        authenticationInfo);
    assertThat(oAuth2UserDAO.getByUsername(sub)).isNull();
    assertThat(oAuth2GroupDAO.getAll()).isEmpty();
  }

  private static void assertAuthenticationInfoIsTheExpected(
      final String username,
      final String displayName,
      final List<String> groups,
      final ShiroJsonWebToken shiroJsonWebToken,
      final AuthenticationInfo authenticationInfo)
  {
    PrincipalCollection principalCollection = authenticationInfo.getPrincipals();
    Iterator<?> principalIterator = principalCollection.iterator();
    Object principal = principalIterator.next();

    assertThat(principal).isInstanceOf(UserPrincipal.class);
    UserPrincipal userPrincipal = (UserPrincipal) principal;
    assertThat(userPrincipal.getRealmId()).isEqualTo(OAuth2Realm.ID);
    assertThat(userPrincipal.getUsername()).isEqualTo(username);
    assertThat(userPrincipal.getDisplayName()).isEqualTo(displayName);
    assertThat(userPrincipal.getMembership()).containsAll(groups);
    assertThat(authenticationInfo.getCredentials()).isEqualTo(shiroJsonWebToken.getCredentials());
  }

  private Map<String, Object> getCustomClaimsAndConfigureOAuth2(
      final String issuer,
      final String username,
      final String firstName,
      final String lastName,
      final String email,
      final List<String> groups)
  {
    OAuth2Configuration oAuth2Configuration = new OAuth2Configuration(issuer, jwtGenerator.getJwsAlgorithm(), null,
        jwtGenerator.getJWKSetString());
    oAuth2Configuration.setUsernameClaim(JWTGenerator.USERNAME_CLAIM);
    oAuth2Configuration.setFirstNameClaim(JWTGenerator.FIRST_NAME_CLAIM);
    oAuth2Configuration.setLastNameClaim(JWTGenerator.LAST_NAME_CLAIM);
    oAuth2Configuration.setEmailClaim(JWTGenerator.EMAIL_CLAIM);
    oAuth2Configuration.setGroupsClaim(JWTGenerator.GROUPS_CLAIM);
    oAuth2ConfigurationDAO.insert(oAuth2Configuration);

    return jwtGenerator.getCustomClaims(username, firstName, lastName, email, groups);
  }

  private void assertOAuth2GroupsAreCreated(final List<String> groups) {
    List<OAuth2Group> oAuth2Groups = oAuth2GroupDAO.getAll();
    assertThat(oAuth2Groups.stream().map(OAuth2Group::getName)).contains(groups.toArray(new String[0]));
  }

  private void assertOAuth2UserIsTheExpected(
      final String username,
      final String firstName,
      final String lastName,
      final String email,
      final List<String> groups)
  {
    OAuth2User oAuth2User = oAuth2UserDAO.getByUsername(username);
    assertThat(oAuth2User).isNotNull();
    assertThat(oAuth2User.getId()).isNotNull();
    assertThat(oAuth2User.getFirstName()).isEqualTo(firstName);
    assertThat(oAuth2User.getLastName()).isEqualTo(lastName);
    assertThat(oAuth2User.getEmail()).isEqualTo(email);
    assertThat(oAuth2User.getGroups()).contains(groups.toArray(new String[0]));
  }
}
