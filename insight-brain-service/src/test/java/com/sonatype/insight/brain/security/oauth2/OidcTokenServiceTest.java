/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.oauth2;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.security.OidcTokenDAO;
import com.sonatype.insight.brain.model.security.OidcToken;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.apache.shiro.authc.AuthenticationException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OidcTokenServiceTest
    extends AbstractComponentTest
{
  @Inject
  private JWTGenerator jwtGenerator;

  @Inject
  private OidcTokenService oidcTokenService;

  @Inject
  private OidcTokenDAO oidcTokenDAO;

  @Test
  public void testRegisterOidcToken_WithValidJWTToken() {
    final String sub = "bob";
    final String issuer = "https://an-idp.com";

    String token = jwtGenerator.generateJWT(sub, issuer);
    String tokenId = oidcTokenService.registerOidcToken(token);

    assertThat(tokenId).isNotBlank();
    OidcToken oidcToken = oidcTokenDAO.getById(tokenId);
    assertThat(oidcToken).isNotNull();
    assertThat(oidcToken.getToken()).isEqualTo(token);
  }

  @Test
  public void testRegisterOidcToken_ThrowsErrorWhenTokenIsNull() {
    assertThatThrownBy(() -> oidcTokenService.registerOidcToken(null))
        .isInstanceOf(AuthenticationException.class)
        .hasMessage("OIDC token is empty");
  }

  @Test
  public void testRegisterOidcToken_ThrowsErrorWhenTokenIsBlank() {
    assertThatThrownBy(() -> oidcTokenService.registerOidcToken(""))
        .isInstanceOf(AuthenticationException.class)
        .hasMessage("OIDC token is empty");
  }

  @Test
  public void testGetOidcToken_WithValidJWTToken() {
    final String sub = "bob";
    final String issuer = "https://an-idp.com";

    String token = jwtGenerator.generateJWT(sub, issuer);
    OidcToken oidcToken = new OidcToken(token);
    oidcTokenDAO.insert(oidcToken);

    String foundToken = oidcTokenService.getOidcToken(oidcToken.getId());

    assertThat(foundToken).isNotBlank();
    assertThat(foundToken).isEqualTo(token);
  }

  @Test
  public void testGetOidcToken_ReturnsOidcTokenWhenUsingOidcTokenAsTokenId() {
    final String sub = "bob";
    final String issuer = "https://an-idp.com";

    String token = jwtGenerator.generateJWT(sub, issuer);
    String foundToken = oidcTokenService.getOidcToken(token);

    assertThat(foundToken).isNotBlank();
    assertThat(foundToken).isEqualTo(token);
  }

  @Test
  public void testGetOidcToken_ReturnsBlankWhenTokenIdDoesntExist() {
    String foundToken = oidcTokenService.getOidcToken("invalid-token-id");

    assertThat(foundToken).isBlank();
  }

  @Test
  public void testPullOidcToken_WithValidJWTToken() {
    final String sub = "bob";
    final String issuer = "https://an-idp.com";

    String token = jwtGenerator.generateJWT(sub, issuer);
    OidcToken oidcToken = new OidcToken(token);
    oidcTokenDAO.insert(oidcToken);

    String foundToken = oidcTokenService.pullOidcToken(oidcToken.getId());

    assertThat(foundToken).isNotBlank();
    assertThat(foundToken).isEqualTo(token);
    assertThat(oidcTokenDAO.getById(oidcToken.getId())).isNull();
  }

  @Test
  public void testPullOidcToken_ReturnsOidcTokenWhenUsingOidcTokenAsTokenId() {
    final String sub = "bob";
    final String issuer = "https://an-idp.com";

    String token = jwtGenerator.generateJWT(sub, issuer);
    String foundToken = oidcTokenService.pullOidcToken(token);

    assertThat(foundToken).isNotBlank();
    assertThat(foundToken).isEqualTo(token);
  }

  @Test
  public void testPullOidcToken_ReturnsBlankWhenTokenIdDoesntExist() {
    String foundToken = oidcTokenService.pullOidcToken("invalid-token-id");

    assertThat(foundToken).isBlank();
  }
}
