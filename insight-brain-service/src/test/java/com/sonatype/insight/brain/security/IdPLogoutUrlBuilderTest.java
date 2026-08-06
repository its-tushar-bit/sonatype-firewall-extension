/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrl;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.LOGOUT_AUTH0_ON_LOGOUT;
import static org.assertj.core.api.Assertions.assertThat;

public class IdPLogoutUrlBuilderTest
    extends AbstractComponentTest
{
  @Inject
  private BaseUrl baseUrl;

  @Inject
  public IdPLogoutUrlBuilder idPLogoutUrlBuilder;

  @Before
  public void before() {
    setBaseUrl("http://localhost:8070");
  }

  @After
  public void exit() {
    baseUrl.release();
  }

  @Test
  public void testBuildIdPLogoutUrl_shouldReturnAuth0LogoutFromSamlConfiguration() {
    samlConfigurationService.insert(tempEntity.newSamlConfiguration(auth0IdpXml(), null));

    tempEntity.newSystemConfigurationProperty(LOGOUT_AUTH0_ON_LOGOUT, "true");

    URI logoutURI = idPLogoutUrlBuilder.buildIdPLogoutUrl();
    assertThat(logoutURI).hasToString(
        "https://idp-entity-id/v2/logout?client_id=rfCvE9qbgAu0ASBCCwe8QZugsAJzf1TK&returnTo=http://localhost:8070/");
  }

  @Test
  public void testBuildIdPLogoutUrl_shouldReturnAuth0LogoutFromOidcConfiguration() {
    String clientId = "client-id";
    String issuer = "https://an-idp.com";
    tempEntity.newOidcConfiguration(issuer, clientId, "client-secret", "https://an-idp.com/authorize",
        "https://an-idp.com/tokens");

    tempEntity.newSystemConfigurationProperty(LOGOUT_AUTH0_ON_LOGOUT, "true");
    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(true);

    URI logoutURI = idPLogoutUrlBuilder.buildIdPLogoutUrl();
    assertThat(logoutURI).hasToString(
        String.format("%s/v2/logout?client_id=%s&returnTo=http://localhost:8070/", issuer, clientId));
  }

  @Test
  public void testBuildIdPLogoutUrl_shouldReturnAuth0LogoutFromSamlConfigIfOAuth2NotEnabled() {
    // Oidc configuration
    String clientId = "client-id";
    String issuer = "https://an-idp.com";
    tempEntity.newOidcConfiguration(issuer, clientId, "client-secret", "https://an-idp.com/authorize",
        "https://an-idp.com/tokens");

    // Saml configuration
    samlConfigurationService.insert(tempEntity.newSamlConfiguration(auth0IdpXml(), null));

    tempEntity.newSystemConfigurationProperty(LOGOUT_AUTH0_ON_LOGOUT, "true");

    URI logoutURI = idPLogoutUrlBuilder.buildIdPLogoutUrl();
    assertThat(logoutURI).hasToString(
        "https://idp-entity-id/v2/logout?client_id=rfCvE9qbgAu0ASBCCwe8QZugsAJzf1TK&returnTo=http://localhost:8070/");
  }

  @Test
  public void testBuildIdPLogoutUrl_shouldReturnNullIfOssConfigurationIsNotSet() {

    tempEntity.newSystemConfigurationProperty(LOGOUT_AUTH0_ON_LOGOUT, "true");

    URI logoutURI = idPLogoutUrlBuilder.buildIdPLogoutUrl();
    assertThat(logoutURI).isNull();
  }

  @Test
  public void testBuildIdPLogoutUrl_shouldReturnNullIfLogoutAuth0OnLogoutPropertyIsFalse() {

    tempEntity.newSystemConfigurationProperty(LOGOUT_AUTH0_ON_LOGOUT, "false");

    URI logoutURI = idPLogoutUrlBuilder.buildIdPLogoutUrl();
    assertThat(logoutURI).isNull();
  }

  protected String auth0IdpXml() {
    try {
      return IOUtils.toString(
          getClass().getResourceAsStream("/IdPLogoutUrlBuilderTest/identity-provider-metadata.xml"),
          StandardCharsets.UTF_8);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
