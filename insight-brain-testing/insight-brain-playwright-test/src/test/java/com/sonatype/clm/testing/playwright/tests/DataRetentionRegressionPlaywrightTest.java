/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.DataRetentionRegressionPage;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.model.Organization;

import com.microsoft.playwright.assertions.LocatorAssertions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Regression tests for the Data Retention editor. */
public class DataRetentionRegressionPlaywrightTest
    extends AbstractIqUiTest
{
  private static final LocatorAssertions.IsVisibleOptions VISIBLE_OPTS =
      new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  private static final LocatorAssertions.IsCheckedOptions CHECKED_OPTS =
      new LocatorAssertions.IsCheckedOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  private static final LocatorAssertions.IsEnabledOptions ENABLED_OPTS =
      new LocatorAssertions.IsEnabledOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  private static final String DATA_RETENTION_URL_FRAGMENT = "/data-retention";

  private static final String DEVELOP_STAGE = "develop";

  @BeforeEach
  public void openDashboardAndLoginAsAdmin() {
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
  }

  private void navigateToOrgDataRetention(String orgId) {
    navigateAndWaitForUrl(
        OwnerSummaryPage.editOrganizationUrl(orgId, DATA_RETENTION_URL_FRAGMENT),
        DATA_RETENTION_URL_FRAGMENT);
  }

  /**
   * Verifies the editor renders its H1 heading, both content sections, and the Update button.
   * Navigates via the Owner Summary tile Edit button rather than a direct deep-link so that
   * tile rendering is also exercised.
   */
  @Test
  @Tag("regression")
  public void testEditorRendersWithFormAndSections() {
    Organization org = tempEntity.newOrganization();

    navigateAndWaitForUrl(OwnerSummaryPage.url(org.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    DataRetentionRegressionPage regPage = new DataRetentionRegressionPage();
    regPage.clickDataRetentionEditButton();
    playwrightWaitUntilUrlContains(DATA_RETENTION_URL_FRAGMENT);

    assertThat(regPage.pageHeading()).isVisible(VISIBLE_OPTS);
    assertThat(regPage.applicationReportsSection()).isVisible(VISIBLE_OPTS);
    assertThat(regPage.successMetricsSection()).isVisible(VISIBLE_OPTS);
    assertThat(regPage.updateButton()).isVisible(VISIBLE_OPTS);
  }

  /**
   * Verifies the no-changes guard and that a saved change persists after reload.
   * Child org stages start with {@code inheritPolicy=true} so "Don't Purge" is a valid
   * change without requiring text input.
   */
  @Test
  @Tag("regression")
  public void testNoChangesGuard_andSaveChangePersistsOnReload() {
    Organization childOrg = tempEntity.newOrganization();
    navigateToOrgDataRetention(childOrg.getId());
    DataRetentionRegressionPage regPage = new DataRetentionRegressionPage();

    regPage.updateButton().click();
    assertThat(regPage.formValidationErrors()).isVisible(VISIBLE_OPTS);

    regPage.doNotPurgeLabelForStage(DEVELOP_STAGE).click();
    assertThat(regPage.updateButton()).isEnabled(ENABLED_OPTS);

    regPage.clickUpdateAndWaitForSave();

    page.reload();
    playwrightWaitUntilUrlContains(DATA_RETENTION_URL_FRAGMENT);
    assertThat(regPage.doNotPurgeRadioForStage(DEVELOP_STAGE)).isChecked(CHECKED_OPTS);

    regPage.updateButton().click();
    assertThat(regPage.formValidationErrors()).isVisible(VISIBLE_OPTS);
  }
}
