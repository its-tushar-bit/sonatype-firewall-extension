/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Route;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.SbomContinuousMonitoringPage;
import com.sonatype.clm.testing.playwright.pages.SbomContinuousMonitoringPageAssertions;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** SBOM Manager Continuous Monitoring editor. */
public class SbomContinuousMonitoringPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String ORG_NAME_PREFIX = "SbomCmOrg";

  private SbomContinuousMonitoringPage editor;

  private SbomContinuousMonitoringPageAssertions assertions;

  private Organization organization;

  @Before
  public void seedOrgAndOpenEditor() {
    // Editor gated on SBOM_MANAGER + POLICY_MONITORING + SBOM_CONTINUOUS_MONITORING_UI flag.
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    setFeatures(LicensedFeature.SBOM_MANAGER, LicensedFeature.POLICY_MONITORING);
    SystemConfigurationPropertyFeature.SBOM_CONTINUOUS_MONITORING_UI.setEnabled(true);

    organization = tempEntity.newOrganization(ORG_NAME_PREFIX + "-" + TemporaryEntity.uuid());
    editor = new SbomContinuousMonitoringPage();
    assertions = new SbomContinuousMonitoringPageAssertions(editor);

    playwrightHardresetToBlank();
    playwrightRefreshOrOpen(SbomContinuousMonitoringPage.url(organization));
    playwrightLogin();
    page.waitForURL("**" + SbomContinuousMonitoringPage.MONITORING_URL_FRAGMENT);
  }

  /** Reset the system-config flag — license/feature resets are handled by AbstractIqUiTest's @After. */
  @After
  public void disableSbomContinuousMonitoringUi() {
    SystemConfigurationPropertyFeature.SBOM_CONTINUOUS_MONITORING_UI.setEnabled(false);
  }

  @Test
  @Category(RegressionTest.class)
  public void testEditor_rendersWithToggleAndSubmit() {
    assertions.shouldBeVisible();
    assertions.shouldShowToggle();
    assertions.shouldShowUpdateButton();
    assertions.shouldShowLearnMoreButton();
  }

  /** Toggle flips Disabled → Enabled; form becomes dirty so the validation alert clears. */
  @Test
  @Category(RegressionTest.class)
  public void testToggle_enablesAndActivatesUpdateButton() {
    assertions.shouldShowToggleDisabledLabel();
    assertions.shouldShowToggleNotChecked();

    editor.toggleSwitchLabel().click();

    assertions.shouldShowToggleEnabledLabel();
    assertions.shouldShowToggleChecked();
    assertions.shouldNotShowNoChangesValidationError();
  }

  /** Save+reload round-trip in both directions. {@code waitForSubmitMaskSuccess()} prevents reload racing the save. */
  @Test
  @Category(RegressionTest.class)
  public void testToggle_persistsEnabledAndDisabledAfterReload() {
    editor.toggleSwitchLabel().click();
    assertions.shouldShowToggleEnabledLabel();
    editor.submitButton().click();
    waitForSubmitMaskSuccess();

    playwrightRefresh();
    assertions.shouldShowToggleEnabledLabel();
    assertions.shouldShowToggleChecked();

    editor.toggleSwitchLabel().click();
    assertions.shouldShowToggleDisabledLabel();
    editor.submitButton().click();
    waitForSubmitMaskSuccess();

    playwrightRefresh();
    assertions.shouldShowToggleDisabledLabel();
    assertions.shouldShowToggleNotChecked();
  }

  /**
   * NxStatefulForm doesn't disable Update on {@code isDirty=false}; clicking it surfaces the
   * MSG_NO_CHANGES_TO_SAVE alert, which clears once a real change is made.
   */
  @Test
  @Category(RegressionTest.class)
  public void testUpdate_dirtyStateRoutesValidation() {
    editor.submitButton().click();
    assertions.shouldShowNoChangesValidationError();

    editor.toggleSwitchLabel().click();
    assertions.shouldNotShowNoChangesValidationError();
  }

  /**
   * Asserts the Learn More button dispatches a popup to the app-owned source URL
   * ({@code links.sonatype.com/products/sbom/docs/monitoring}). We intercept that URL with a
   * stub response so the test (a) doesn't require outbound internet from CI and (b) doesn't
   * depend on where {@code links.sonatype.com} happens to 301-redirect today. The contract
   * under test is "button opens a popup to the configured source URL" — anything past the
   * redirect is outside the app's control.
   */
  @Test
  @Category(RegressionTest.class)
  public void testLearnMore_opensDocsInNewTab() {
    page.context()
        .route(SbomContinuousMonitoringPage.LEARN_MORE_SOURCE_URL,
            route -> route.fulfill(new Route.FulfillOptions()
                .setStatus(200)
                .setContentType("text/html")
                .setBody("<html><body>stub</body></html>")));

    Page popup = page.waitForPopup(() -> editor.learnMoreButton().click());
    try {
      popup.waitForLoadState();
      assertThat(popup).hasURL(SbomContinuousMonitoringPage.LEARN_MORE_SOURCE_URL);
    }
    finally {
      popup.close();
    }
  }
}
