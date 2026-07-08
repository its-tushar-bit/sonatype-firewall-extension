/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.util.List;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.CrowdConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.CrowdConfigurationPageAssertions;
import com.sonatype.clm.testing.playwright.pages.OidcConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.OidcConfigurationPageAssertions;
import com.sonatype.insight.brain.api.v2.service.ApiOidcConfigurationService;
import com.sonatype.insight.brain.dataaccess.configuration.crowd.CrowdConfigurationDAO;
import com.sonatype.insight.error.exception.NotFoundException;

import com.microsoft.playwright.Locator;
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

  private static final String TEST_JWKS_JSON = "{\"keys\":[]}";

  private static final String TEST_USERNAME_CLAIM = "preferred_username";

  private static final String TEST_EMAIL_CLAIM = "email";

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

  /**
   * The 6 required OIDC inputs carry {@code aria-required="true"}. Runs at the Playwright layer
   * (rather than Jest) so the attribute is asserted against the real rendered DOM, past
   * {@code OidcConfigurationForm}'s feature-flag branching.
   */
  @Test
  @Category(RegressionTest.class)
  public void testOidcForm_requiredFieldsHaveAriaRequiredTrue() {
    navigateToOidcPage();

    List<Locator> required = List.of(
        oidcPage.clientId(), oidcPage.clientSecret(), oidcPage.idpIssuer(),
        oidcPage.authorizationUrl(), oidcPage.tokenUrl(), oidcPage.jwsAlgorithm());
    required.forEach(field -> assertThat(field).hasAttribute("aria-required", "true"));
  }

  /**
   * The optional OIDC inputs and User-Attribute-Mapping claim inputs carry
   * {@code aria-required="false"}.
   */
  @Test
  @Category(RegressionTest.class)
  public void testOidcForm_optionalFieldsHaveAriaRequiredFalse() {
    navigateToOidcPage();

    List<Locator> optional = List.of(
        oidcPage.jwksUrl(), oidcPage.jwksJson(),
        oidcPage.authorizationCustomParamsJson(), oidcPage.tokenRequestCustomParamsJson(),
        oidcPage.usernameClaim(), oidcPage.emailClaim(), oidcPage.firstNameClaim(),
        oidcPage.lastNameClaim(), oidcPage.groupsClaim(), oidcPage.exactMatchClaimsJson());
    optional.forEach(field -> assertThat(field).hasAttribute("aria-required", "false"));
  }

  /**
   * Clearing a required OIDC field (Client ID) and clicking Save must NOT persist the config. The
   * Delete button remains disabled — a deterministic UI signal that the invalid save was blocked.
   */
  @Test
  @Category(RegressionTest.class)
  public void testOidcRequiredField_emptyBlocksSubmit() {
    navigateToOidcPage();
    fillOidcRequiredFields();
    oidcPage.clientId().fill("");

    oidcPage.saveButton().click();

    oidcAssertions.shouldShowDeleteButtonDisabled();
  }

  /**
   * Saves a config, edits Client ID, clicks Cancel, and asserts the field reverts to the saved
   * value. Uses the real save path (not {@code page.route}) so the saved state is DB-persisted.
   */
  @Test
  @Category(RegressionTest.class)
  public void testOidcCancel_restoresSavedValues() {
    navigateToOidcPage();
    fillOidcRequiredFields();
    saveOidcAndAwaitPersisted();

    navigateToOidcPage();
    assertThat(oidcPage.clientId()).hasValue(TEST_CLIENT_ID);

    oidcPage.clientId().fill("modified-client-id");
    oidcPage.cancelButton().click();

    assertThat(oidcPage.clientId()).hasValue(TEST_CLIENT_ID);
  }

  /** Delete button is disabled on a fresh (unsaved) OIDC form and enabled after saving a config. */
  @Test
  @Category(RegressionTest.class)
  public void testOidcDelete_disabledBeforeSave_enabledAfterSave() {
    navigateToOidcPage();
    oidcAssertions.shouldShowDeleteButtonDisabled();

    fillOidcRequiredFields();
    saveOidcAndAwaitPersisted();

    navigateToOidcPage();
    oidcAssertions.shouldShowDeleteButtonEnabled();
  }

  /**
   * User Attribute Mapping claims (all optional) persist across a reload when supplied alongside
   * the required fields.
   */
  @Test
  @Category(RegressionTest.class)
  public void testOidcUserAttributeClaims_persistAcrossReload() {
    navigateToOidcPage();
    fillOidcRequiredFields();
    oidcPage.usernameClaim().fill(TEST_USERNAME_CLAIM);
    oidcPage.emailClaim().fill(TEST_EMAIL_CLAIM);
    saveOidcAndAwaitPersisted();

    navigateToOidcPage();
    assertThat(oidcPage.usernameClaim()).hasValue(TEST_USERNAME_CLAIM);
    assertThat(oidcPage.emailClaim()).hasValue(TEST_EMAIL_CLAIM);
  }

  /**
   * JWKS URL and JWKS JSON are mutually optional: config is valid with URL only OR JSON only.
   */
  @Test
  @Category(RegressionTest.class)
  public void testOidc_jwksUrlAndJwksJson_mutuallyOptional() {
    navigateToOidcPage();
    fillOidcRequiredFields();
    saveOidcAndAwaitPersisted();

    navigateToOidcPage();
    oidcPage.jwksUrl().fill("");
    oidcPage.jwksJson().fill(TEST_JWKS_JSON);
    saveOidcAndAwaitPersisted();

    navigateToOidcPage();
    assertThat(oidcPage.jwksUrl()).hasValue("");
    assertThat(oidcPage.jwksJson()).hasValue(TEST_JWKS_JSON);
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

  /**
   * Clicks Save and blocks until the save landed. {@code waitForSubmitMaskSuccess()} is
   * best-effort on fast paths; the Delete button flipping to enabled is a deterministic UI
   * signal that the config now exists on the server.
   */
  private void saveOidcAndAwaitPersisted() {
    oidcPage.saveButton().click();
    waitForSubmitMaskSuccess();
    oidcAssertions.shouldShowDeleteButtonEnabled();
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
