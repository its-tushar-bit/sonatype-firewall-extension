/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import com.microsoft.playwright.Locator;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.BulkWaivePage;
import com.sonatype.clm.testing.playwright.pages.BulkWaivePageAssertions;
import com.sonatype.clm.testing.playwright.pages.WaiverConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.WaiverConfigurationPageAssertions;
import com.sonatype.clm.testing.playwright.pages.WaiverConfirmationPage;
import com.sonatype.clm.testing.playwright.testdatamanager.TestDataManager;
import com.sonatype.clm.testing.playwright.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.license.model.LicensedFeature;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class BulkWaivePlaywrightTest
    extends AbstractIqUiTest
{
  private record Data(
      String orgNamePrefix,
      String appNamePrefix,
      String appIdPrefix,
      String cannedReportClasspathDir,
      String scanId,
      String policyName,
      int policyThreatLevel,
      String cdpComponentHash,
      String noConditionPolicyName,
      String noResultsFilterText,
      String selectedCountZero,
      String selectedCountSingular,
      String selectedCountPlural,
      String noConditionText,
      String constraintColumnHeaderText,
      String componentColumnHeaderText,
      String enterpriseBannerText,
      String policyNameFilterPlaceholder,
      String constraintNameFilterPlaceholder,
      String componentNameFilterPlaceholder)
  {
  }

  private static final Data DATA = TestDataManager.load("bulk-waive", Data.class);

  private BulkWaivePage bulkWaivePage;

  private BulkWaivePageAssertions assertions;

  private WaiverConfigurationPage configPage;

  private WaiverConfigurationPageAssertions configAssertions;

  private Application application;

  private String resolvedPolicyName;

  @Before
  public void seedEntitiesAndLogin() throws IOException {
    bulkWaivePage = new BulkWaivePage();
    assertions = new BulkWaivePageAssertions(bulkWaivePage);
    configPage = new WaiverConfigurationPage();
    configAssertions = new WaiverConfigurationPageAssertions(configPage);

    seedDb();
    playwrightLoginAdminAt(BulkWaivePage.url(application.getPublicId(), DATA.scanId()));
  }

  /** Restore feature set so {@code setMissingFeature} calls don't leak across the JVM session. */
  @After
  public void restoreFeatures() {
    setFeatures(LicensedFeature.values());
  }

  @Test
  @Category(RegressionTest.class)
  public void testBulkWaive_pageRendersSelectAllAndProceed() {
    assertions.shouldBeVisible();

    int rowCount = bulkWaivePage.violationRows().count();
    bulkWaivePage.selectAllCheckbox().click();
    assertions.shouldHaveSelectAllChecked(true);

    for (int i = 0; i < rowCount; i++) {
      assertThat(bulkWaivePage.violationRowCheckboxInput(i)).isChecked();
    }
    assertThat(bulkWaivePage.selectionCountLabel()).containsText(DATA.selectedCountPlural());

    assertions.shouldHaveNextButtonEnabled();
    bulkWaivePage.nextButton().click();
    page.waitForURL("**/waiverConfiguration");
    configAssertions.shouldBeVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testBulkWaive_cdpMode_constraintColumnAndHashFilter() {
    assertions.shouldBeVisible();
    int allRowsCount = bulkWaivePage.violationRows().count();

    String cdpUrl = BulkWaivePage.cdpUrl(application.getPublicId(), DATA.scanId(), DATA.cdpComponentHash());
    playwrightRefreshOrOpen(cdpUrl);

    assertions.shouldBeVisible();

    assertions.shouldShowConstraintOrComponentColumnHeader(DATA.constraintColumnHeaderText());

    assertThat(bulkWaivePage.constraintNameFilter()).isVisible();
    assertions.shouldHaveFilterPlaceholder(
        bulkWaivePage.constraintNameFilter(), DATA.constraintNameFilterPlaceholder());

    assertThat(bulkWaivePage.violationRows().first()).isVisible();
    int cdpRowsCount = bulkWaivePage.violationRows().count();
    Assertions.assertThat(cdpRowsCount)
        .as("CDP mode should show fewer rows than all violations")
        .isGreaterThan(0)
        .isLessThan(allRowsCount);
  }

  @Test
  @Category(RegressionTest.class)
  public void testBulkWaive_enterpriseBanner_shownWhenBulkWaiversNotLicensed() {
    setMissingFeature(LicensedFeature.BULK_WAIVERS);

    playwrightRefreshOrOpen(BulkWaivePage.url(application.getPublicId(), DATA.scanId()));

    assertThat(bulkWaivePage.container()).isVisible();

    assertions.shouldShowEnterpriseBannerWithText(DATA.enterpriseBannerText());
    assertions.shouldHaveBannerFlushTopClass();
  }

  @Test
  @Category(RegressionTest.class)
  public void testBulkWaive_selectAllDeselectAll_countFooterAndNextDisabled() {
    assertions.shouldBeVisible();

    assertions.shouldHaveNextButtonDisabled();

    bulkWaivePage.selectAllCheckbox().click();
    assertions.shouldHaveSelectAllChecked(true);
    assertions.shouldHaveNextButtonEnabled();
    assertThat(bulkWaivePage.selectionCountLabel()).containsText(DATA.selectedCountPlural());

    bulkWaivePage.selectAllCheckbox().click();
    assertions.shouldHaveSelectAllChecked(false);
    assertions.shouldHaveNextButtonDisabled();
    assertions.shouldShowSelectionCount(DATA.selectedCountZero());

    bulkWaivePage.firstRowCheckbox().click();
    assertions.shouldShowSelectionCount(DATA.selectedCountSingular());
    assertions.shouldHaveNextButtonEnabled();

    bulkWaivePage.selectAllCheckbox().click();
    assertions.shouldHaveSelectAllChecked(true);
    int totalRows = bulkWaivePage.violationRows().count();

    bulkWaivePage.policyNameFilter().fill(DATA.noResultsFilterText());
    assertThat(bulkWaivePage.selectionCountLabel()).containsText("(" + totalRows + " hidden)");
  }

  @Test
  @Category(RegressionTest.class)
  public void testBulkWaive_rowClickOpensPopover_checkboxClickTogglesOnly() {
    assertions.shouldBeVisible();

    assertions.shouldNotShowViolationDetailsPopover();

    Locator firstRow = bulkWaivePage.violationRows().first();
    firstRow.click();
    assertions.shouldShowViolationDetailsPopover();

    bulkWaivePage.violationDetailsPopoverCloseButton().click();
    assertions.shouldNotShowViolationDetailsPopover();

    bulkWaivePage.firstRowCheckbox().click();
    assertions.shouldNotShowViolationDetailsPopover();
    assertions.shouldHaveNextButtonEnabled();
  }

  @Test
  @Category(RegressionTest.class)
  public void testBulkWaive_filters_popoverAndInlinePolicyNameFilter() {
    assertions.shouldBeVisible();

    bulkWaivePage.filtersToggleButton().click();
    assertions.shouldShowReportFilterPopover();

    bulkWaivePage.reportFilterPopoverCloseButton().click();
    assertThat(bulkWaivePage.reportFilterPopover()).isHidden();

    assertThat(bulkWaivePage.policyNameFilter()).isVisible();
    assertions.shouldHaveFilterPlaceholder(
        bulkWaivePage.policyNameFilter(), DATA.policyNameFilterPlaceholder());
    bulkWaivePage.policyNameFilter().fill(resolvedPolicyName);
    assertThat(bulkWaivePage.violationRows().first()).isVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testBulkWaive_noResultsEmptyState() {
    assertions.shouldBeVisible();

    bulkWaivePage.policyNameFilter().fill(DATA.noResultsFilterText());

    assertions.shouldShowNoResultsRow();
  }

  @Test
  @Category(RegressionTest.class)
  public void testBulkWaive_sortColumns_threatColumnTogglesSortDirection() {
    assertions.shouldBeVisible();

    Locator threatHeader = bulkWaivePage.threatColumnHeader();

    threatHeader.click();
    assertThat(threatHeader).hasAttribute("aria-sort", "ascending");

    threatHeader.click();
    assertThat(threatHeader).hasAttribute("aria-sort", "descending");
  }

  /**
   * Full bulk-waive submission: select all → Next → fill scope/expiry/reason → Next →
   * Submit. Waivers only take effect on the next evaluation, so the strongest immediate UI
   * signal of success is the redirect away from {@code /waiverConfirmation}.
   */
  @Test
  @Category(RegressionTest.class)
  public void testBulkWaive_completeWaiverFormAndSubmit() {
    assertions.shouldBeVisible();
    assertThat(bulkWaivePage.violationRows()).not().hasCount(0);

    bulkWaivePage.selectAllCheckbox().click();
    assertions.shouldHaveSelectAllChecked(true);
    assertions.shouldHaveNextButtonEnabled();
    bulkWaivePage.nextButton().click();
    page.waitForURL("**" + WaiverConfigurationPage.URL_FRAGMENT);
    configAssertions.shouldBeVisible();

    configPage.selectFirstAvailableScope();
    configPage.expirySelect().selectOption("never");
    configPage.selectFirstAvailableReason();
    configPage.commentsTextarea().fill("automated bulk-waive smoke");

    configAssertions.shouldHaveNextButtonEnabled();
    configPage.nextButton().click();

    page.waitForURL("**" + WaiverConfirmationPage.URL_FRAGMENT);
    new WaiverConfirmationPage().submitButton().click();

    page.waitForURL(url -> !url.contains(WaiverConfirmationPage.URL_FRAGMENT));

    List<PolicyWaiver> waivers = lookup(PolicyWaiverDAO.class).getByOwnerId(application.getId());
    Assertions.assertThat(waivers)
        .as("Bulk waive should have persisted at least one waiver for the application")
        .isNotEmpty();
  }

  private void seedDb() throws IOException {
    String suffix = TemporaryEntity.uuid();
    String orgName = DATA.orgNamePrefix() + "-" + suffix;
    String appName = DATA.appNamePrefix() + "-" + suffix;
    String appPublicId = DATA.appIdPrefix() + "-" + suffix;
    resolvedPolicyName = DATA.policyName() + "-" + suffix;

    Organization org = tempEntity.newOrganization(orgName);
    application = tempEntity.newApplication(appName, appPublicId, org.getId());

    Policy policy = new Policy(null, resolvedPolicyName);
    policy.setThreatLevel(DATA.policyThreatLevel());
    policy.setOwnerId(Organization.ROOT_ORGANIZATION_ID);
    Constraint constraint = new Constraint(null, "pw-bw-constraint", LogicalOperator.AND);
    constraint.setConditions(Collections.singletonList(
        new Condition("MatchState", "is", "exact")));
    policy.setConstraints(Collections.singletonList(constraint));
    tempEntity.newPolicy(policy);

    URL zippedReport = ReportHelper.zipReport(DATA.cannedReportClasspathDir(), tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    new TestReportEvaluator(application, DATA.scanId(), zippedReport, baseUrlFromTest, work)
        .evaluatePolicy();
  }
}
