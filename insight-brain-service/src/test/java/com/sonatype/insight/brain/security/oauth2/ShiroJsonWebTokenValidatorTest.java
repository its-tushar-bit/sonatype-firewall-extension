/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.oauth2;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OAuth2ConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.oauth2.OAuth2Configuration;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.http.HttpHeaders;
import org.apache.shiro.authz.AuthorizationException;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ShiroJsonWebTokenValidatorTest
    extends AbstractComponentTest
{
  @Rule
  public WireMockRule idpServer = new WireMockRule(wireMockConfig().dynamicPort());

  @Inject
  private JWTGenerator jwtGenerator;

  @Inject
  private ShiroJsonWebTokenValidator shiroJsonWebTokenVerifier;

  @Inject
  private OAuth2ConfigurationDAO oAuth2ConfigurationDAO;

  @Test
  public void testIsTokenValid_fromJwks() {
    final String sub = "bob";
    final String issuer = "https://an-idp.com";

    tempEntity.newOAuth2Configuration(issuer, jwtGenerator.getJwsAlgorithm(), "", jwtGenerator.getJWKSetString());
    String token = jwtGenerator.generateJWT(sub, issuer);
    ShiroJsonWebToken shiroJsonWebToken = new ShiroJsonWebToken(token);

    assertThat(shiroJsonWebTokenVerifier.isTokenValid(shiroJsonWebToken)).isTrue();
  }

  @Test
  public void testIsTokenValid_fromJwksUrl() {
    final String sub = "bob";
    final String issuer = "https://another-idp.com";

    // Stub IDP Server with JWKS URL
    String url = String.format("%s/jwks.json", idpServer.baseUrl());
    idpServer.stubFor(get(urlPathEqualTo("/jwks.json"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody(jwtGenerator.getJWKSetString())));

    tempEntity.newOAuth2Configuration(issuer, jwtGenerator.getJwsAlgorithm(), url, "");
    String token = jwtGenerator.generateJWT(sub, issuer);
    ShiroJsonWebToken shiroJsonWebToken = new ShiroJsonWebToken(token);

    assertThat(shiroJsonWebTokenVerifier.isTokenValid(shiroJsonWebToken)).isTrue();
  }

  @Test
  public void testIsTokenValid_ExpiredToken() {
    final String sub = "bob";
    final String issuer = "https://an-idp.com";

    tempEntity.newOAuth2Configuration(issuer, jwtGenerator.getJwsAlgorithm(), "", jwtGenerator.getJWKSetString());
    Instant fiveMinutesAgo = Instant.now().minusSeconds(300);
    String token = jwtGenerator.generateJWT(sub, issuer, fiveMinutesAgo);
    ShiroJsonWebToken shiroJsonWebToken = new ShiroJsonWebToken(token);

    assertThat(shiroJsonWebTokenVerifier.isTokenValid(shiroJsonWebToken)).isFalse();
  }

  @Test
  public void testIsTokenValid_WithExactMatchClaims() {
    final String sub = "bob";
    final String issuer = "https://an-idp.com";
    final String orgId = "my-org";
    final String orgIdClaim = "org_id";

    OAuth2Configuration oAuth2Configuration = new OAuth2Configuration(issuer, jwtGenerator.getJwsAlgorithm(), null,
        jwtGenerator.getJWKSetString());

    // Exact match claims
    Map<String, String> exactMatchClaims = new HashMap<>();
    exactMatchClaims.put(orgIdClaim, orgId);
    oAuth2Configuration.setExactMatchClaims(exactMatchClaims);
    oAuth2ConfigurationDAO.insert(oAuth2Configuration);

    Map<String, Object> claims = new HashMap<>();
    claims.put(orgIdClaim, orgId);
    String token = jwtGenerator.generateJWT(sub, issuer, claims);
    ShiroJsonWebToken shiroJsonWebToken = new ShiroJsonWebToken(token);

    assertThat(shiroJsonWebTokenVerifier.isTokenValid(shiroJsonWebToken)).isTrue();
  }

  @Test
  public void testIsTokenValid_WithExactMatchClaims_WrongClaimValue() {
    final String sub = "bob";
    final String issuer = "https://an-idp.com";
    final String orgId = "my-org";
    final String orgIdClaim = "org_id";

    OAuth2Configuration oAuth2Configuration = new OAuth2Configuration(issuer, jwtGenerator.getJwsAlgorithm(), null,
        jwtGenerator.getJWKSetString());

    Map<String, String> exactMatchClaims = new HashMap<>();
    exactMatchClaims.put(orgIdClaim, orgId);
    oAuth2Configuration.setExactMatchClaims(exactMatchClaims);
    oAuth2ConfigurationDAO.insert(oAuth2Configuration);

    Map<String, Object> claims = new HashMap<>();
    claims.put(orgIdClaim, orgId);
    String token = jwtGenerator.generateJWT(sub, issuer, claims);
    ShiroJsonWebToken shiroJsonWebToken = new ShiroJsonWebToken(token);

    assertThat(shiroJsonWebTokenVerifier.isTokenValid(shiroJsonWebToken)).isTrue();
  }

  @Test
  public void testIsTokenValid_ThrowErrorIfConfigurationIsNotAvailable() {
    final String sub = "bob";
    final String issuer = "https://an-idp.com";

    String token = jwtGenerator.generateJWT(sub, issuer);
    ShiroJsonWebToken shiroJsonWebToken = new ShiroJsonWebToken(token);

    assertThatThrownBy(() -> shiroJsonWebTokenVerifier.isTokenValid(shiroJsonWebToken)).isInstanceOf(
        AuthorizationException.class)
        .hasMessage(ShiroJsonWebTokenValidator.NO_OAUTH_2_CONFIGURATION_AVAILABLE);
  }
}
