/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.CrowdConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.CrowdConfigurationPageAssertions;
import com.sonatype.clm.testing.playwright.pages.OidcConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.OidcConfigurationPageAssertions;
import com.sonatype.insight.brain.api.v2.service.ApiOidcConfigurationService;
import com.sonatype.insight.brain.dataaccess.configuration.crowd.CrowdConfigurationDAO;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class OidcAndCrowdConfigurationPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String TEST_CLIENT_ID = "pw-oidc-client-id";

  private static final String TEST_CLIENT_SECRET = "pw-oidc-client-secret";

  private static final String TEST_IDP_ISSUER = "https://idp.example.com";

  private static final String TEST_AUTH_URL = "https://idp.example.com/authorize";

  private static final String TEST_TOKEN_URL = "https://idp.example.com/token";

  private static final String TEST_JWS_ALGORITHM = "RS256";

  private static final String TEST_JWKS_URL = "https://idp.example.com/.well-known/jwks.json";

  private static final String TEST_CROWD_SERVER_URL = "http://crowd.example.com:8095/crowd";

  private static final String TEST_CROWD_APP_NAME = "pw-crowd-app";

  private static final String TEST_CROWD_APP_PASSWORD = "pw-crowd-pass";

  private OidcConfigurationPage oidcPage;

  private OidcConfigurationPageAssertions oidcAssertions;

  private CrowdConfigurationPage crowdPage;

  private CrowdConfigurationPageAssertions crowdAssertions;

  @Before
  public void setUp() {
    playwrightRefreshOrOpen(OidcConfigurationPage.url());
    playwrightLogin();
    oidcPage = new OidcConfigurationPage();
    oidcAssertions = new OidcConfigurationPageAssertions(oidcPage);
    crowdPage = new CrowdConfigurationPage();
    crowdAssertions = new CrowdConfigurationPageAssertions(crowdPage);
  }

  @After
  public void cleanup() {
    playwrightLogout();
    try {
      lookup(ApiOidcConfigurationService.class).deleteOidcConfiguration();
    }
    catch (NotFoundException ignored) {
    }
    lookup(CrowdConfigurationDAO.class).delete();
  }

  @Test
  @Category(RegressionTest.class)
  public void testOidcConfigurationPageRenders() {
    navigateToOidcPage();

    oidcAssertions.shouldRenderPageLayout();
  }

  @Test
  @Category(RegressionTest.class)
  public void testOidcSaveConfiguration() {
    navigateToOidcPage();

    fillOidcRequiredFields();

    oidcAssertions.shouldShowSaveButtonEnabled();
    oidcPage.saveButton().click();
    waitForSubmitMaskSuccess();
    oidcAssertions.shouldShowDeleteButtonEnabled();

    navigateToOidcPage();
    assertThat(oidcPage.clientId()).hasValue(TEST_CLIENT_ID);
    oidcAssertions.shouldShowDeleteButtonEnabled();
  }

  @Test
  @Category(RegressionTest.class)
  public void testOidcDeleteConfiguration() {
    navigateToOidcPage();

    fillOidcRequiredFields();
    oidcPage.saveButton().click();
    waitForSubmitMask();

    navigateToOidcPage();
    oidcAssertions.shouldShowDeleteButtonEnabled();
    oidcPage.deleteButton().click();

    oidcAssertions.shouldShowDeleteModal();
    oidcPage.deleteModalSubmitButton().click();
    waitForSubmitMask();

    navigateToOidcPage();
    oidcAssertions.shouldShowDeleteButtonDisabled();
  }

  private void navigateToOidcPage() {
    playwrightRefreshOrOpen(OidcConfigurationPage.url());
    assertThat(oidcPage.saveButton()).isVisible();
  }

  private void fillOidcRequiredFields() {
    oidcPage.clientId().fill(TEST_CLIENT_ID);
    oidcPage.clientSecret().fill(TEST_CLIENT_SECRET);
    oidcPage.idpIssuer().fill(TEST_IDP_ISSUER);
    oidcPage.authorizationUrl().fill(TEST_AUTH_URL);
    oidcPage.tokenUrl().fill(TEST_TOKEN_URL);
    oidcPage.jwsAlgorithm().fill(TEST_JWS_ALGORITHM);
    oidcPage.jwksUrl().fill(TEST_JWKS_URL);
  }

  @Test
  @Category(RegressionTest.class)
  public void testCrowdConfigurationPageRenders() {
    navigateToCrowdPage();

    crowdAssertions.shouldRenderPageLayout();
  }

  @Test
  @Category(RegressionTest.class)
  public void testCrowdSaveConfiguration() {
    navigateToCrowdPage();

    crowdPage.serverUrl().fill(TEST_CROWD_SERVER_URL);
    crowdPage.applicationName().fill(TEST_CROWD_APP_NAME);
    crowdPage.applicationPassword().fill(TEST_CROWD_APP_PASSWORD);

    crowdAssertions.shouldShowSaveButtonEnabled();
    crowdPage.saveButton().click();
    waitForSubmitMaskSuccess();
    crowdAssertions.shouldShowDeleteButtonEnabled();

    navigateToCrowdPage();
    assertThat(crowdPage.serverUrl()).hasValue(TEST_CROWD_SERVER_URL);
    crowdAssertions.shouldShowDeleteButtonEnabled();
  }

  @Test
  @Category(RegressionTest.class)
  public void testCrowdDeleteConfiguration() {
    navigateToCrowdPage();

    crowdPage.serverUrl().fill(TEST_CROWD_SERVER_URL);
    crowdPage.applicationName().fill(TEST_CROWD_APP_NAME);
    crowdPage.applicationPassword().fill(TEST_CROWD_APP_PASSWORD);
    crowdPage.saveButton().click();
    waitForSubmitMask();

    navigateToCrowdPage();
    crowdAssertions.shouldShowDeleteButtonEnabled();
    crowdPage.deleteButton().click();

    crowdAssertions.shouldShowDeleteModal();
    crowdPage.deleteModalSubmitButton().click();
    waitForSubmitMask();

    navigateToCrowdPage();
    crowdAssertions.shouldShowDeleteButtonDisabled();
  }

  private void navigateToCrowdPage() {
    playwrightRefreshOrOpen(CrowdConfigurationPage.url());
    assertThat(crowdPage.saveButton()).isVisible();
  }
}
