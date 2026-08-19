/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.util.regex.Pattern;

import com.microsoft.playwright.assertions.LocatorAssertions;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.ContinuousMonitoringRegressionPage;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.model.Organization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Regression tests for the Continuous Monitoring editor. */
public class ContinuousMonitoringRegressionPlaywrightTest
    extends AbstractIqUiTest
{
  private static final LocatorAssertions.IsVisibleOptions VISIBLE_OPTS =
      new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  private static final LocatorAssertions.HasTextOptions TEXT_OPTS =
      new LocatorAssertions.HasTextOptions().setTimeout(PlaywrightTiming.SLOW_ELEMENT_TIMEOUT_MS);

  private static final LocatorAssertions.IsCheckedOptions CHECKED_OPTS =
      new LocatorAssertions.IsCheckedOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  private static final LocatorAssertions.IsEnabledOptions ENABLED_OPTS =
      new LocatorAssertions.IsEnabledOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  private static final LocatorAssertions.HasCountOptions COUNT_OPTS =
      new LocatorAssertions.HasCountOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  private static final String MONITORING_URL_FRAGMENT = "/monitoring";

  private static final String BUILD_STAGE = "Build";

  private static final String DO_NOT_MONITOR = "Do not monitor";

  private static final Pattern NO_CHANGES_TO_SAVE = Pattern.compile(".*There are no changes to save\\..*");

  /** 6 CLI stages (Develop, Source, Build, Stage Release, Release, Operate) + 1 inherit/no-monitor option. */
  private static final int MONITORING_RADIO_COUNT = 7;

  private static final Pattern INHERIT_FROM_PATTERN = Pattern.compile("Inherit from.*");

  private static final Pattern PAGE_DESCRIPTION_PATTERN = Pattern.compile(
      ".*Keep daily visibility on applications.*Violation notifications can be configured per policy\\..*");

  @BeforeEach
  public void openDashboardAndLoginAsAdmin() {
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
  }

  private void navigateToOrgMonitoring(String orgId) {
    navigateAndWaitForUrl(
        OwnerSummaryPage.editOrganizationUrl(orgId, MONITORING_URL_FRAGMENT),
        MONITORING_URL_FRAGMENT);
  }

  @Test
  @Tag("regression")
  public void testEditorRendersWithRadioButtonsAndFieldset() {
    Organization org = tempEntity.newOrganization();
    navigateToOrgMonitoring(org.getId());
    ContinuousMonitoringRegressionPage regPage = new ContinuousMonitoringRegressionPage();

    assertThat(regPage.pageHeading()).isVisible(VISIBLE_OPTS);
    assertThat(regPage.monitoringStageFieldset()).isVisible(VISIBLE_OPTS);
    assertThat(regPage.allMonitoringStageRadioLabels()).hasCount(MONITORING_RADIO_COUNT, COUNT_OPTS);
    assertThat(regPage.radioLabelForStage(BUILD_STAGE)).isVisible(VISIBLE_OPTS);
  }

  @Test
  @Tag("regression")
  public void testFirstRadioOption_doNotMonitorAtRootOrg_inheritFromParentAtChildOrg() {
    navigateToOrgMonitoring(Organization.ROOT_ORGANIZATION_ID);
    ContinuousMonitoringRegressionPage regPage = new ContinuousMonitoringRegressionPage();
    assertThat(regPage.radioLabelForStage(DO_NOT_MONITOR)).isVisible(VISIBLE_OPTS);
    assertThat(regPage.allMonitoringStageRadioLabels()).hasCount(MONITORING_RADIO_COUNT, COUNT_OPTS);

    Organization childOrg = tempEntity.newOrganization();
    // Both URLs share "/monitoring" — force navigation with playwrightRefreshOrOpen.
    playwrightRefreshOrOpen(OwnerSummaryPage.editOrganizationUrl(childOrg.getId(), MONITORING_URL_FRAGMENT));
    playwrightWaitUntilUrlContains(childOrg.getId());
    assertThat(regPage.radioLabelForStage(INHERIT_FROM_PATTERN)).isVisible(VISIBLE_OPTS);
    assertThat(regPage.allMonitoringStageRadioLabels()).hasCount(MONITORING_RADIO_COUNT, COUNT_OPTS);
  }

  @Test
  @Tag("regression")
  public void testSelectDoNotMonitorAndSave_removesMonitoring() {
    tempEntity.newPolicyMonitoring(Organization.ROOT_ORGANIZATION_ID, Stage.ID_BUILD);
    navigateToOrgMonitoring(Organization.ROOT_ORGANIZATION_ID);
    ContinuousMonitoringRegressionPage regPage = new ContinuousMonitoringRegressionPage();

    assertThat(regPage.radioInputForStage(BUILD_STAGE)).isChecked(CHECKED_OPTS);

    regPage.radioLabelForStage(DO_NOT_MONITOR).click();
    assertThat(regPage.updateButton()).isEnabled(ENABLED_OPTS);

    regPage.clickUpdateAndWaitForSave();

    assertThat(regPage.radioInputForStage(BUILD_STAGE)).not().isChecked(CHECKED_OPTS);

    playwrightRefreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    assertThat(regPage.monitoringTileStageLink()).hasText(DO_NOT_MONITOR, TEXT_OPTS);
  }

  @Test
  @Tag("regression")
  public void testUpdateButton_noChangesGuard_showsValidationError() {
    Organization org = tempEntity.newOrganization();
    navigateToOrgMonitoring(org.getId());
    ContinuousMonitoringRegressionPage regPage = new ContinuousMonitoringRegressionPage();

    regPage.updateButton().click();
    assertThat(regPage.formValidationErrors()).isVisible(VISIBLE_OPTS);
    assertThat(regPage.formValidationErrors()).hasText(NO_CHANGES_TO_SAVE, TEXT_OPTS);
  }

  @Test
  @Tag("regression")
  public void testPageDescriptionTextVisible() {
    Organization org = tempEntity.newOrganization();
    navigateToOrgMonitoring(org.getId());
    ContinuousMonitoringRegressionPage regPage = new ContinuousMonitoringRegressionPage();

    assertThat(regPage.pageDescription()).isVisible(VISIBLE_OPTS);
    assertThat(regPage.pageDescription()).hasText(PAGE_DESCRIPTION_PATTERN, TEXT_OPTS);
  }
}
