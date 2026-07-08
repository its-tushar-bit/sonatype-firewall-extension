/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.ZscalerConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.ZscalerConfigurationPageAssertions;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.configuration.ZScalerConfigurationDAO;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Route;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Regression coverage for the Zscaler Configuration page ({@code /firewall/zscalerConfig},
 * firewall-license gated).
 */
public class ZscalerConfigurationPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String TEST_USERNAME = "pw-zscaler-user";

  private static final String TEST_PASSWORD = "pw-zscaler-pass";

  private static final String TEST_HOSTNAME = "https://zsapi.example.com";

  /** Zscaler API keys must be exactly 12 characters — enforced by client-side validation. */
  private static final String TEST_API_KEY = "pwzscalerkey";

  private static final String RESTRICTED_USER_PREFIX = "pw-zscaler-viewer";

  private static final String TEST_CONFIG_ENDPOINT = "**/api/v2/config/zscaler/testConfig";

  private ZscalerConfigurationPage zscalerPage;

  private ZscalerConfigurationPageAssertions assertions;

  private boolean switchedToRestrictedUser;

  @Before
  public void openZscalerPage() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FIREWALL);
    playwrightRefreshOrOpen(ZscalerConfigurationPage.url());
    playwrightLogin();

    zscalerPage = new ZscalerConfigurationPage();
    assertions = new ZscalerConfigurationPageAssertions(zscalerPage);
  }

  @After
  public void cleanup() {
    if (switchedToRestrictedUser) {
      playwrightHardreset();
    }
    lookup(ZScalerConfigurationDAO.class).delete();
  }

  /**
   * Required-field validation blocks submit; EULA required; Configured Formats multi-select
   * lists Maven/Npm/Nuget/Pypi; EULA link points to the Zscaler EULA URL.
   */
  @Test
  @Category(RegressionTest.class)
  public void testZscalerConfiguration_requiredFieldsAndFormatsDropdown() {
    assertions.shouldRenderPageLayout();

    assertions.shouldShowCancelButtonDisabled();

    assertThat(zscalerPage.eulaLink())
        .hasAttribute("href", "https://links.sonatype.com/products/firewall/docs/zscaler/zscaler-eula");

    zscalerPage.configuredFormatsToggle().click();
    assertThat(zscalerPage.configuredFormatOption("Maven")).isVisible();
    assertThat(zscalerPage.configuredFormatOption("Npm")).isVisible();
    assertThat(zscalerPage.configuredFormatOption("Nuget")).isVisible();
    assertThat(zscalerPage.configuredFormatOption("Pypi")).isVisible();
  }

  /**
   * Test Configuration button is enabled once all fields including password are supplied.
   * Uses {@code page.route()} to simulate a 500 from the testConfig endpoint (no real Zscaler
   * target is available in the embedded test server); the resulting LoadError alert renders
   * with a "Retry" affordance.
   */
  @Test
  @Category(RegressionTest.class)
  public void testZscalerConfiguration_testConfigButtonStates() {
    fillRequiredFields();

    assertions.shouldShowTestConfigButtonEnabled();

    page.route(TEST_CONFIG_ENDPOINT, route -> route.fulfill(new Route.FulfillOptions()
        .setStatus(500)
        .setContentType("application/json")
        .setBody("{\"message\":\"Test Zscaler configuration failed.\"}")));
    zscalerPage.testConfigButton().click();
    assertions.shouldShowTestConfigError();
    page.unroute(TEST_CONFIG_ENDPOINT);
  }

  /**
   * After an existing config, editing any field surfaces the password-reentry sublabel. Scoped
   * to the password field's own NxFormGroup so we don't false-match the same string in the
   * (hidden) form-level validation-error alert.
   */
  @Test
  @Category(RegressionTest.class)
  public void testZscalerConfiguration_passwordReenterSublabelOnEdit() {
    fillRequiredFields();
    selectMavenFormat();
    zscalerPage.eulaCheckbox().click();
    zscalerPage.saveButton().click();
    waitForSubmitMaskSuccess();

    playwrightRefreshOrOpen(ZscalerConfigurationPage.url());
    // Edit the Username so the form dirties without triggering hostname-shape validation
    // ("Only base URL allowed - no paths or trailing slashes" — see ZscalerConfigFieldValidation).
    zscalerPage.username().fill(TEST_USERNAME + "-edited");

    Locator passwordFormGroup = page.locator(".nx-form-group")
        .filter(new Locator.FilterOptions().setHas(zscalerPage.password()));
    assertThat(passwordFormGroup.locator(".nx-sub-label"))
        .hasText("Password must be re-entered when any fields are modified.");
  }

  /**
   * Save persists config; on subsequent load the button label is "Update"; Cancel is
   * disabled when the form is clean.
   */
  @Test
  @Category(RegressionTest.class)
  public void testZscalerConfiguration_saveAndCancelDisabledWhenClean() {
    assertions.shouldShowCancelButtonDisabled();

    fillRequiredFields();
    selectMavenFormat();
    zscalerPage.eulaCheckbox().click();
    zscalerPage.saveButton().click();
    waitForSubmitMaskSuccess();

    playwrightRefreshOrOpen(ZscalerConfigurationPage.url());
    assertThat(zscalerPage.hostname()).hasValue(TEST_HOSTNAME);
    assertThat(zscalerPage.saveButton()).hasText("Update");
    assertions.shouldShowCancelButtonDisabled();
    assertions.shouldShowDeleteButtonEnabled();
  }

  /**
   * Delete Configuration modal: Cancel preserves config, Confirm deletes it; button is
   * disabled pre-config.
   */
  @Test
  @Category(RegressionTest.class)
  public void testZscalerConfiguration_deleteModalCancelAndConfirm() {
    assertions.shouldShowDeleteButtonDisabled();

    fillRequiredFields();
    selectMavenFormat();
    zscalerPage.eulaCheckbox().click();
    zscalerPage.saveButton().click();
    waitForSubmitMaskSuccess();

    playwrightRefreshOrOpen(ZscalerConfigurationPage.url());
    assertions.shouldShowDeleteButtonEnabled();

    zscalerPage.deleteButton().click();
    assertions.shouldShowDeleteModal();
    zscalerPage.deleteModalCancelButton().click();
    assertions.shouldShowDeleteModalHidden();
    assertions.shouldShowDeleteButtonEnabled();

    zscalerPage.deleteButton().click();
    assertions.shouldShowDeleteModal();
    zscalerPage.deleteModalConfirmButton().click();
    waitForSubmitMaskSuccess();

    playwrightRefreshOrOpen(ZscalerConfigurationPage.url());
    assertions.shouldShowDeleteButtonDisabled();
  }

  /** Non-admin users see the auth-error message (isAuthorized=false), not the form. */
  @Test
  @Category(RegressionTest.class)
  public void testZscalerConfiguration_authGuardForNonAdminUser() {
    String restrictedUser = RESTRICTED_USER_PREFIX + "-" + TemporaryEntity.uuid();
    Role viewOnlyRole = tempEntity.newRole(true, Permission.VIEW_ROLES);
    tempEntity.newUser(restrictedUser, "View", "Only", restrictedUser + "@test.local");
    tempEntity.newMembershipMapping(
        MembershipMapping.GLOBAL_CONTEXT_ID, viewOnlyRole.getId(), restrictedUser);

    playwrightLogout();
    switchedToRestrictedUser = true;
    playwrightLogin(restrictedUser, TemporaryEntity.USER_PASSWORD_CLEAR);
    playwrightRefreshOrOpen(ZscalerConfigurationPage.url());

    assertions.shouldShowAuthErrorMessage();
  }

  private void fillRequiredFields() {
    zscalerPage.username().fill(TEST_USERNAME);
    zscalerPage.password().fill(TEST_PASSWORD);
    // Blur so NxTextInput marks the password non-pristine — Test-Config gate requires
    // !passwordState.isPristine.
    zscalerPage.password().blur();
    zscalerPage.hostname().fill(TEST_HOSTNAME);
    zscalerPage.apiKey().fill(TEST_API_KEY);
  }

  private void selectMavenFormat() {
    zscalerPage.configuredFormatsToggle().click();
    // Click the visible label rather than the CSS-hidden input.
    zscalerPage.configuredFormatOption("Maven").click();
    zscalerPage.configuredFormatsToggle().click();
  }
}
