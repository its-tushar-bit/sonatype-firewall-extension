/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.oauth2;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OAuth2ConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.oauth2.OAuth2Configuration;
import com.sonatype.insight.brain.security.UserSessionResource;
import com.sonatype.insight.brain.security.UserSessionResource.AuthenticationStatus;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class OnPremJwtTokenTest
    extends AbstractResourceTest
{
  private OAuth2ConfigurationDAO oAuth2ConfigurationDAO;

  private JWTGenerator jwtGenerator = new JWTGenerator();

  @Before
  public void setUp() {
    oAuth2ConfigurationDAO = lookup(OAuth2ConfigurationDAO.class);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(UserSessionResource.RESOURCE_PATH);
  }

  protected HttpRequest requestWithToken(String token) {
    return restRequest().header("Authorization", String.format("Bearer %s", token));
  }

  @Test
  public void testValidJWT() throws Exception {
    final String sub = "bob";
    final String issuer = "https://an-idp.com";
    final String username = "bob-the-ruler";
    final String firstName = "Bob";
    final String lastName = "Sanders";
    final String email = "bob@company.com";
    final List<String> groups = Arrays.asList("admin", "dev", "other");

    // Enable OAuth feature
    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(true);

    // Create JWT token
    Map<String, Object> claims = jwtGenerator.getCustomClaims(username, firstName, lastName, email, groups);
    String token = jwtGenerator.generateJWT(sub, issuer, claims);

    // Configure OAuth
    configureOAuth2(issuer, null);

    HttpResponse response = requestWithToken(token).get();

    assertResponseStatus(200, response);
    AuthenticationStatus status = response.getBody(AuthenticationStatus.class);
    assertThat(status.isAuthenticated()).isTrue();
    assertThat(status.getUsername()).isEqualTo(username);
    assertThat(status.getDisplayName()).isEqualTo("Bob Sanders");
    assertThat(status.getGroups()).containsAll(groups);
  }

  @Test
  public void testExpiredJWT() throws Exception {
    final String sub = "bob";
    final String issuer = "https://another-idp.com";
    final String username = "bob-the-ruler";
    final String firstName = "Bob";
    final String lastName = "Sanders";
    final String email = "bob@company.com";
    final List<String> groups = Arrays.asList("admin", "dev", "other");

    // Enable OAuth feature
    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(true);

    // Create JWT token
    Map<String, Object> claims = jwtGenerator.getCustomClaims(username, firstName, lastName, email, groups);
    Instant fiveMinutesAgo = Instant.now().minusSeconds(300);
    String token = jwtGenerator.generateJWT(sub, issuer, claims, fiveMinutesAgo);

    // Configure OAuth
    configureOAuth2(issuer, null);

    HttpResponse response = requestWithToken(token).get();

    assertResponseStatus(401, response);
  }

  @Test
  public void testValidJWT_WithExactMatchClaims() throws Exception {
    final String sub = "bob";
    final String issuer = "https://an-idp.com";
    final String username = "bob-the-ruler";
    final String firstName = "Bob";
    final String lastName = "Sanders";
    final String email = "bob@company.com";
    final String exactMatchClaimValue = "my-value";
    final String exactMatchClaim = "my-custom-claim";
    final List<String> groups = Arrays.asList("admin", "dev", "other");

    // Enable OAuth feature
    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(true);

    // Create JWT token
    Map<String, Object> claims = jwtGenerator.getCustomClaims(username, firstName, lastName, email, groups);
    claims.put(exactMatchClaim, exactMatchClaimValue);
    String token = jwtGenerator.generateJWT(sub, issuer, claims);

    // Configure OAuth
    Map<String, String> exactMatchClaims = new HashMap<>();
    exactMatchClaims.put(exactMatchClaim, exactMatchClaimValue);
    configureOAuth2(issuer, exactMatchClaims);

    HttpResponse response = requestWithToken(token).get();

    assertResponseStatus(200, response);
    AuthenticationStatus status = response.getBody(AuthenticationStatus.class);
    assertThat(status.isAuthenticated()).isTrue();
    assertThat(status.getUsername()).isEqualTo(username);
    assertThat(status.getDisplayName()).isEqualTo("Bob Sanders");
    assertThat(status.getGroups()).containsAll(groups);
  }

  @Test
  public void testValidJWT_WithExactMatchClaims_WrongClaimValue() throws Exception {
    final String sub = "bob";
    final String issuer = "https://an-idp.com";
    final String username = "bob-the-ruler";
    final String firstName = "Bob";
    final String lastName = "Sanders";
    final String email = "bob@company.com";
    final String exactMatchClaimValue = "my-value";
    final String exactMatchClaim = "my-custom-claim";
    final List<String> groups = Arrays.asList("admin", "dev", "other");

    // Enable OAuth feature
    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(true);

    // Create JWT token
    Map<String, Object> claims = jwtGenerator.getCustomClaims(username, firstName, lastName, email, groups);
    claims.put(exactMatchClaim, exactMatchClaimValue);
    String token = jwtGenerator.generateJWT(sub, issuer, claims);

    // Configure OAuth
    Map<String, String> exactMatchClaims = new HashMap<>();
    exactMatchClaims.put(exactMatchClaim, "wrong-value");
    configureOAuth2(issuer, exactMatchClaims);

    HttpResponse response = requestWithToken(token).get();

    assertResponseStatus(401, response);
  }

  private void configureOAuth2(String issuer, Map<String, String> exactMatchClaims) {
    OAuth2Configuration oAuth2Configuration = new OAuth2Configuration(issuer, jwtGenerator.getJwsAlgorithm(), null,
        jwtGenerator.getJWKSetString());
    oAuth2Configuration.setUsernameClaim(JWTGenerator.USERNAME_CLAIM);
    oAuth2Configuration.setFirstNameClaim(JWTGenerator.FIRST_NAME_CLAIM);
    oAuth2Configuration.setLastNameClaim(JWTGenerator.LAST_NAME_CLAIM);
    oAuth2Configuration.setEmailClaim(JWTGenerator.EMAIL_CLAIM);
    oAuth2Configuration.setGroupsClaim(JWTGenerator.GROUPS_CLAIM);

    if (exactMatchClaims != null) {
      oAuth2Configuration.setExactMatchClaims(exactMatchClaims);
    }

    oAuth2ConfigurationDAO.insert(oAuth2Configuration);
  }
}
