/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.math.BigDecimal;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.EditRoiConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.RoiConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.RoiConfigurationPageAssertions;
import com.sonatype.insight.brain.dataaccess.roi.RoiConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.roi.RoiConfigurationDefaultValuesDAO;
import com.sonatype.insight.brain.model.roi.CurrencyTypes;
import com.sonatype.insight.brain.model.roi.RoiConfiguration;
import com.sonatype.insight.brain.model.roi.RoiConfigurationDefaultValues;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoiConfigurationPlaywrightTest
    extends AbstractIqUiTest
{
  private static final Logger log = LoggerFactory.getLogger(RoiConfigurationPlaywrightTest.class);

  private static final String NEW_BASELINE_DAYS = "42";

  private static final Integer SEED_BASELINE_DAYS = 30;

  private static final BigDecimal SEED_DAILY_RISK = new BigDecimal("800");

  private RoiConfigurationPage roiPage;

  private RoiConfigurationPageAssertions roiAssertions;

  private EditRoiConfigurationPage editRoiPage;

  @Before
  public void setUp() {
    // Explicitly pin license to PRODUCT_RISK_AND_REMEDIATION — narrows from the default broader set
    // so the ROI page renders with a deterministic license context. Empirically, removing this call
    // makes the Update button intermittently not re-appear after save (likely a license-dependent
    // re-render path).
    setLicensedProducts(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION);

    seedRoiConfigurationDefaultsIfMissing();
    seedRoiConfigurationIfMissing();

    playwrightRefreshOrOpen(RoiConfigurationPage.url());
    playwrightLogin();
    roiPage = new RoiConfigurationPage();
    roiAssertions = new RoiConfigurationPageAssertions(roiPage);
    editRoiPage = new EditRoiConfigurationPage();
  }

  @After
  public void cleanup() {
    playwrightLogout();
    deleteRoiConfiguration();
  }

  private void seedRoiConfigurationDefaultsIfMissing() {
    RoiConfigurationDefaultValuesDAO dao = lookup(RoiConfigurationDefaultValuesDAO.class);
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
          30,
          15,
          new BigDecimal("800"),
          new BigDecimal("400")));
      tx.commit();
    }
  }

  private void seedRoiConfigurationIfMissing() {
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

  private void deleteRoiConfiguration() {
    RoiConfigurationDAO dao = lookup(RoiConfigurationDAO.class);
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      RoiConfiguration current = dao.getByCurrencyType(CurrencyTypes.USD);
      if (current != null) {
        dao.delete(tx, current);
      }
      tx.commit();
    }
    catch (Exception e) {
      log.debug("Failed to delete ROI configuration during cleanup", e);
    }
  }

  @Test
  @Category(RegressionTest.class)
  public void testRoiConfiguration_restoreDefaultsResetsValues() {
    roiPage.editLink().click();

    editRoiPage.baselineDaysInput().clear();
    editRoiPage.baselineDaysInput().fill(NEW_BASELINE_DAYS);
    editRoiPage.updateButton().click();
    editRoiPage.waitUntilSaved();

    editRoiPage.openRestoreDefaultsModal();
    editRoiPage.restoreDefaultsModalRestoreButton().click();
    editRoiPage.waitUntilSaved();

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
    editRoiPage.waitUntilSaved();

    playwrightRefreshOrOpen(RoiConfigurationPage.url());
    roiAssertions.shouldShowBaselineDaysContaining(NEW_BASELINE_DAYS);
  }
}
