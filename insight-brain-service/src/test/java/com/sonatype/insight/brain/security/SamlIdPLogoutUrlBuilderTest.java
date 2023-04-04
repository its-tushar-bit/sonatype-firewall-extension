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
import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrl;

import org.codehaus.plexus.util.IOUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.LOGOUT_AUTH0_ON_LOGOUT;
import static org.assertj.core.api.Assertions.assertThat;

public class SamlIdPLogoutUrlBuilderTest
    extends AbstractComponentTest
{
  @Inject
  public SamlDeploymentManager samlDeploymentManager;

  @Inject
  private BaseUrl baseUrl;

  @Inject
  public SamlIdPLogoutUrlBuilder samlIdPLogoutUrlBuilder;

  @Before
  public void before() {
    setBaseUrl("http://localhost:8070");
  }

  @After
  public void exit() {
    baseUrl.release();
  }

  @Test
  public void testBuildIdPLogoutUrl_shouldReturnProperLogoutURLForAuth0() {
    tempEntity.newSamlConfiguration(auth0IdpXml(), null);
    samlDeploymentManager.updateFromConfiguration();

    tempEntity.newSystemConfigurationProperty(LOGOUT_AUTH0_ON_LOGOUT, "true");

    URI logoutURI = samlIdPLogoutUrlBuilder.buildIdPLogoutUrl();
    assertThat(logoutURI.toString()).isEqualTo(
        "https://idp-entity-id/v2/logout?client_id=rfCvE9qbgAu0ASBCCwe8QZugsAJzf1TK&returnTo=http://localhost:8070/");
  }

  @Test
  public void testBuildIdPLogoutUrl_shouldReturnNullIfSAMLConfigurationIsNotSet() {
    // Ensure the config is re-read
    samlDeploymentManager.register();

    tempEntity.newSystemConfigurationProperty(LOGOUT_AUTH0_ON_LOGOUT, "true");

    URI logoutURI = samlIdPLogoutUrlBuilder.buildIdPLogoutUrl();
    assertThat(logoutURI).isNull();
  }

  @Test
  public void testBuildIdPLogoutUrl_shouldReturnNullIfLogoutAuth0OnLogoutPropertyIsFalse() {
    // Ensure the config is re-read
    samlDeploymentManager.register();

    tempEntity.newSystemConfigurationProperty(LOGOUT_AUTH0_ON_LOGOUT, "false");

    URI logoutURI = samlIdPLogoutUrlBuilder.buildIdPLogoutUrl();
    assertThat(logoutURI).isNull();
  }

  protected String auth0IdpXml() {
    try {
      return IOUtil.toString(
          getClass().getResourceAsStream("/SamlIdPLogoutUrlBuilderTest/identity-provider-metadata.xml"),
          StandardCharsets.UTF_8.toString());
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
