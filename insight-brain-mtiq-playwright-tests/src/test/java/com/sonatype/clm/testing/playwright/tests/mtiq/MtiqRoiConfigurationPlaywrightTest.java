/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import java.math.BigDecimal;

import com.sonatype.clm.testing.playwright.categories.MtiqTest;
import com.sonatype.clm.testing.playwright.mtiq.AbstractMtiqUiTest;
import com.sonatype.clm.testing.playwright.pages.EditRoiConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.EditRoiConfigurationPageAssertions;
import com.sonatype.clm.testing.playwright.pages.RoiConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.RoiConfigurationPageAssertions;
import com.sonatype.insight.brain.dataaccess.roi.RoiConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.roi.RoiConfigurationDefaultValuesDAO;
import com.sonatype.insight.brain.model.roi.CurrencyTypes;
import com.sonatype.insight.brain.model.roi.RoiConfiguration;
import com.sonatype.insight.brain.model.roi.RoiConfigurationDefaultValues;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * MTIQ — ROI Configuration view and edit pages: license-gated section visibility, edit-page
 * field rendering, validation, restore-defaults flow, and save persistence.
 */
@Category(MtiqTest.class)
public class MtiqRoiConfigurationPlaywrightTest
    extends AbstractMtiqUiTest
{
  private static final String NEW_BASELINE_DAYS = "42";

  private static final String NEW_DAILY_RISK = "1200";

  private static final Integer SEED_BASELINE_DAYS = 30;

  private static final BigDecimal SEED_DAILY_RISK = new BigDecimal("800");

  private static final String USD_CURRENCY_LABEL = "United States Dollar (USD)";

  private RoiConfigurationPage roiPage;

  private RoiConfigurationPageAssertions roiAssertions;

  private EditRoiConfigurationPage editRoiPage;

  private EditRoiConfigurationPageAssertions editRoiAssertions;

  @Before
  public void setUp() {
    seedRoiConfigurationDefaults();
    seedRoiConfiguration();
    roiPage = new RoiConfigurationPage();
    roiAssertions = new RoiConfigurationPageAssertions(roiPage);
    editRoiPage = new EditRoiConfigurationPage();
    editRoiAssertions = new EditRoiConfigurationPageAssertions(editRoiPage);
  }

  @Test
  public void testRoiConfiguration_lifecycleMetricsSectionRendersWithLifecycleLicense() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    playwrightLoginAdminAt(RoiConfigurationPage.url());

    roiAssertions.shouldRenderPageLayout();
    assertThat(roiPage.lifecycleMetricsHeading()).isVisible();
    assertThat(roiPage.baselineDaysValue()).isVisible();
    assertThat(roiPage.dailyRiskValue()).isVisible();
    assertThat(roiPage.container().getByText(USD_CURRENCY_LABEL)).isVisible();
  }

  @Test
  public void testRoiConfiguration_firewallMetricsSectionRendersWithFirewallLicense() {
    // Backend /rest/roiConfiguration requires Lifecycle entitlement; frontend gates the
    // Firewall Metrics section on selectHasFirewallLicense. Both licenses are needed.
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FIREWALL, ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    playwrightLoginAdminAt(RoiConfigurationPage.url());

    roiAssertions.shouldShowFirewallMetricsSection();
  }

  @Test
  public void testEditRoiConfiguration_pageRendersWithLifecycleFieldsAndInfoAlert() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    playwrightLoginAdminAt(RoiConfigurationPage.url());
    roiPage.editLink().click();

    assertThat(editRoiPage.container()).isVisible();
    assertThat(editRoiPage.pageHeading()).isVisible();
    assertThat(editRoiPage.backButton()).isVisible();
    assertThat(editRoiPage.infoAlertText()).isVisible();
    assertThat(editRoiPage.lifecycleMetricsHeading()).isVisible();
    assertThat(editRoiPage.baselineDaysInput()).isVisible();
    assertThat(editRoiPage.dailyRiskInput()).isVisible();
  }

  @Test
  public void testEditRoiConfiguration_firewallInputsRenderWithFirewallLicense() {
    // Backend /rest/roiConfiguration requires Lifecycle entitlement; frontend gates the
    // Firewall inputs on selectHasFirewallLicense. Both licenses are needed.
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FIREWALL, ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    playwrightLoginAdminAt(RoiConfigurationPage.url());
    roiPage.editLink().click();

    editRoiAssertions.shouldShowFirewallInputs();
  }

  @Test
  public void testEditRoiConfiguration_firewallFieldValidationShowsErrorAndHidesUpdateButton() {
    // Firewall fields are not seeded so they start empty; fill first ensures the input is
    // "touched" before clearing to guarantee the change event fires.
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FIREWALL, ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    playwrightLoginAdminAt(RoiConfigurationPage.url());
    roiPage.editLink().click();

    editRoiPage.malwareAttacksPreventedInput().fill("100");
    editRoiPage.malwareAttacksPreventedInput().clear();
    assertThat(editRoiPage.validationErrorAlert()).isVisible();
    assertThat(editRoiPage.updateButton()).isHidden();

    editRoiPage.namespaceAttacksPreventedInput().fill("100");
    editRoiPage.namespaceAttacksPreventedInput().clear();
    assertThat(editRoiPage.validationErrorAlert()).isVisible();
    assertThat(editRoiPage.updateButton()).isHidden();

    editRoiPage.safeComponentsAutoSelectedInput().fill("100");
    editRoiPage.safeComponentsAutoSelectedInput().clear();
    assertThat(editRoiPage.validationErrorAlert()).isVisible();
    assertThat(editRoiPage.updateButton()).isHidden();
  }

  /** Update button is unmounted (not disabled) when {@code hasValidationError} is true — hence {@code isHidden()}. */
  @Test
  public void testEditRoiConfiguration_validationErrorHidesUpdateButton() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    playwrightLoginAdminAt(RoiConfigurationPage.url());
    roiPage.editLink().click();

    editRoiPage.baselineDaysInput().clear();
    assertThat(editRoiPage.validationErrorAlert()).isVisible();
    assertThat(editRoiPage.updateButton()).isHidden();
  }

  @Test
  public void testEditRoiConfiguration_cancelNavigatesBackToViewPage() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    playwrightLoginAdminAt(RoiConfigurationPage.url());
    roiPage.editLink().click();

    editRoiPage.cancelLink().click();
    assertThat(roiPage.container()).isVisible();
    assertThat(roiPage.pageHeading()).isVisible();
  }

  /** Cancel path — Confirm path is covered by {@link #testRoiConfiguration_restoreDefaultsResetsValues()}. */
  @Test
  public void testEditRoiConfiguration_restoreDefaultsModalCancelPreservesValues() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    playwrightLoginAdminAt(RoiConfigurationPage.url());
    roiPage.editLink().click();

    editRoiPage.baselineDaysInput().clear();
    editRoiPage.baselineDaysInput().fill(NEW_BASELINE_DAYS);

    editRoiPage.openRestoreDefaultsModal();
    editRoiAssertions.shouldShowRestoreDefaultsModal();
    editRoiPage.restoreDefaultsModalCancelButton().click();

    assertThat(editRoiPage.restoreDefaultsModal()).isHidden();
    assertThat(editRoiPage.baselineDaysInput()).hasValue(NEW_BASELINE_DAYS);
  }

  @Test
  public void testRoiConfiguration_restoreDefaultsResetsValues() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    playwrightLoginAdminAt(RoiConfigurationPage.url());
    roiPage.editLink().click();

    editRoiPage.baselineDaysInput().clear();
    editRoiPage.baselineDaysInput().fill(NEW_BASELINE_DAYS);
    editRoiPage.dailyRiskInput().clear();
    editRoiPage.dailyRiskInput().fill(NEW_DAILY_RISK);
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

  /**
   * JSX has no post-save transition — form stays on /edit; value persistence verified by
   * navigating back to the view page after save.
   */
  @Test
  public void testEditRoiConfiguration_updateSavesAndValuePersistsAcrossReload() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    playwrightLoginAdminAt(RoiConfigurationPage.url());
    roiPage.editLink().click();

    editRoiPage.baselineDaysInput().clear();
    editRoiPage.baselineDaysInput().fill(NEW_BASELINE_DAYS);
    editRoiPage.updateButton().click();
    editRoiAssertions.shouldHaveSettledAfterSave();

    assertThat(editRoiPage.container()).isVisible();
    assertThat(editRoiPage.baselineDaysInput()).hasValue(NEW_BASELINE_DAYS);

    playwrightRefreshOrOpen(RoiConfigurationPage.url());
    roiAssertions.shouldShowBaselineDaysContaining(NEW_BASELINE_DAYS);
  }

  private void seedRoiConfigurationDefaults() {
    RoiConfigurationDefaultValuesDAO dao = lookup(RoiConfigurationDefaultValuesDAO.class);
    // MTIQ tenant provisioning may already seed a USD row — skip insert if present.
    if (dao.getByCurrencyType(CurrencyTypes.USD) != null) {
      return;
    }
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insert(tx, new RoiConfigurationDefaultValues(
          CurrencyTypes.USD,
          new BigDecimal("4350000"),
          new BigDecimal("500000"),
          new BigDecimal("35000"),
          new BigDecimal("10000"),
          new BigDecimal("25000"),
          new BigDecimal("5000"),
          30, // default baseline days (what restore-defaults reverts to)
          15,
          new BigDecimal("800"), // default daily risk — matches SEED_DAILY_RISK and on-prem seed
          new BigDecimal("400")));
      tx.commit();
    }
  }

  private void seedRoiConfiguration() {
    RoiConfigurationDAO dao = lookup(RoiConfigurationDAO.class);
    if (dao.getByCurrencyType(CurrencyTypes.USD) != null) {
      return;
    }
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.insert(tx, new RoiConfiguration(CurrencyTypes.USD, SEED_BASELINE_DAYS, SEED_DAILY_RISK));
      tx.commit();
    }
  }
}
