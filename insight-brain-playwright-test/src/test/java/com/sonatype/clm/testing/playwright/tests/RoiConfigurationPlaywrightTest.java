/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.EditRoiConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.EditRoiConfigurationPageAssertions;
import com.sonatype.clm.testing.playwright.pages.RoiConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.RoiConfigurationPageAssertions;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * ROI Configuration view + Edit pages under a Lifecycle license.
 * Firewall-license variants live in {@link RoiConfigurationFirewallPlaywrightTest}.
 */
public class RoiConfigurationPlaywrightTest
    extends AbstractRoiConfigurationPlaywrightTest
{
  private static final String NEW_BASELINE_DAYS = "42";

  private RoiConfigurationPage roiPage;

  private RoiConfigurationPageAssertions roiAssertions;

  private EditRoiConfigurationPage editRoiPage;

  private EditRoiConfigurationPageAssertions editRoiAssertions;

  @Before
  public void setUp() {
    // Pin to PRODUCT_RISK_AND_REMEDIATION — without it the Update button intermittently fails to
    // re-appear after save (license-dependent re-render path).
    setLicensedProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);

    seedRoiConfigurationDefaultsIfMissing();
    seedRoiConfigurationIfMissing();

    playwrightRefreshOrOpen(RoiConfigurationPage.url());
    playwrightLogin();
    roiPage = new RoiConfigurationPage();
    roiAssertions = new RoiConfigurationPageAssertions(roiPage);
    editRoiPage = new EditRoiConfigurationPage();
    editRoiAssertions = new EditRoiConfigurationPageAssertions(editRoiPage);
  }

  @After
  public void cleanup() {
    playwrightLogout();
    deleteRoiConfiguration();
  }

  @Test
  @Category(RegressionTest.class)
  public void testRoiConfiguration_lifecycleMetricsSectionRendersWithLifecycleLicense() {
    assertThat(roiPage.lifecycleMetricsHeading()).isVisible();
    assertThat(roiPage.baselineDaysValue()).isVisible();
    assertThat(roiPage.dailyRiskValue()).isVisible();
    assertThat(roiPage.container().getByText("United States Dollar (USD)")).isVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testRoiConfiguration_editLinkNavigatesToEditPage() {
    roiPage.editLink().click();

    assertThat(editRoiPage.container()).isVisible();
    assertThat(editRoiPage.pageHeading()).isVisible();
    assertThat(editRoiPage.updateButton()).isVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testEditRoiConfiguration_pageRendersWithLifecycleFieldsAndInfoAlert() {
    roiPage.editLink().click();

    assertThat(editRoiPage.container()).isVisible();
    assertThat(editRoiPage.pageHeading()).isVisible();
    assertThat(editRoiPage.backButton()).isVisible();
    assertThat(editRoiPage.infoAlertText()).isVisible();
    assertThat(editRoiPage.lifecycleMetricsHeading()).isVisible();
    assertThat(editRoiPage.baselineDaysInput()).isVisible();
    assertThat(editRoiPage.dailyRiskInput()).isVisible();
  }

  /** Update button is unmounted (not disabled) when {@code hasValidationError} is true — hence {@code isHidden()}. */
  @Test
  @Category(RegressionTest.class)
  public void testEditRoiConfiguration_validationErrorHidesUpdateButtonAndCancelReturnsToView() {
    roiPage.editLink().click();

    editRoiPage.baselineDaysInput().clear();
    assertThat(editRoiPage.validationErrorAlert()).isVisible();
    assertThat(editRoiPage.updateButton()).isHidden();

    editRoiPage.cancelLink().click();
    assertThat(roiPage.container()).isVisible();
    assertThat(roiPage.pageHeading()).isVisible();
  }

  /** Cancel path — Confirm path is covered by {@link #testRoiConfiguration_restoreDefaultsResetsValues()}. */
  @Test
  @Category(RegressionTest.class)
  public void testEditRoiConfiguration_restoreDefaultsModalCancelPreservesValues() {
    roiPage.editLink().click();

    editRoiPage.baselineDaysInput().clear();
    editRoiPage.baselineDaysInput().fill(NEW_BASELINE_DAYS);

    editRoiPage.openRestoreDefaultsModal();
    editRoiAssertions.shouldShowRestoreDefaultsModal();
    editRoiPage.restoreDefaultsModalCancelButton().click();

    assertThat(editRoiPage.restoreDefaultsModal()).isHidden();
    assertThat(editRoiPage.baselineDaysInput()).hasValue(NEW_BASELINE_DAYS);
  }

  /**
   * JSX has no post-save transition — form stays on /edit; complements
   * {@link #testRoiConfiguration_editPersistsAcrossReload}.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEditRoiConfiguration_updateSavesAndFormRemainsOnEditPage() {
    roiPage.editLink().click();

    editRoiPage.baselineDaysInput().clear();
    editRoiPage.baselineDaysInput().fill(NEW_BASELINE_DAYS);
    editRoiPage.updateButton().click();
    editRoiAssertions.shouldHaveSettledAfterSave();

    assertThat(editRoiPage.container()).isVisible();
    assertThat(editRoiPage.baselineDaysInput()).hasValue(NEW_BASELINE_DAYS);
  }

  @Test
  @Category(RegressionTest.class)
  public void testRoiConfiguration_restoreDefaultsResetsValues() {
    roiPage.editLink().click();

    editRoiPage.baselineDaysInput().clear();
    editRoiPage.baselineDaysInput().fill(NEW_BASELINE_DAYS);
    editRoiPage.updateButton().click();
    editRoiAssertions.shouldHaveSettledAfterSave();

    editRoiPage.openRestoreDefaultsModal();
    editRoiAssertions.shouldShowRestoreDefaultsModal();
    editRoiPage.restoreDefaultsModalRestoreButton().click();
    editRoiAssertions.shouldHaveSettledAfterSave();

    playwrightRefreshOrOpen(RoiConfigurationPage.url());
    roiAssertions.shouldShowBaselineDaysContaining(SEED_BASELINE_DAYS.toString());
    roiAssertions.shouldShowDailyRiskContaining(SEED_DAILY_RISK.toPlainString());
  }

  @Test
  @Category(RegressionTest.class)
  public void testRoiConfiguration_editPersistsAcrossReload() {
    roiPage.editLink().click();

    editRoiPage.baselineDaysInput().clear();
    editRoiPage.baselineDaysInput().fill(NEW_BASELINE_DAYS);
    editRoiPage.updateButton().click();
    editRoiAssertions.shouldHaveSettledAfterSave();

    playwrightRefreshOrOpen(RoiConfigurationPage.url());
    roiAssertions.shouldShowBaselineDaysContaining(NEW_BASELINE_DAYS);
  }
}
