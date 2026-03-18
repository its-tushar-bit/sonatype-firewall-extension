/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.bulkwaiver;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ApplicationReportFilter;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.elements.componentdetails.PolicyViolationDetailPopover;
import com.sonatype.clm.testing.functional.pages.*;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;

import com.codeborne.selenide.ElementsCollection;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;
import static org.assertj.core.api.Assertions.assertThat;

public class BulkWaivePageTest
    extends AbstractFunctionalTest
{
  public static final String SCAN_ID = "e16caf35769f4b3186a7e416d34c2797";

  private Application app;

  private PolicyWaiverDAO policyWaiverDAO;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void start() throws IOException {
    policyWaiverDAO = lookup(PolicyWaiverDAO.class);

    URL referencePolicyUrl = getClass().getResource("/reference-policies-v3.json");
    PolicyExportResult referencePolicies = JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);
    PolicyImportExport policyImportExport = lookup(PolicyImportExport.class);

    Organization org = tempEntity.newOrganization();
    policyImportExport.importOrganization(org, referencePolicies);
    app = tempEntity.newApplication("BulkWaivePageTest", "BulkWaivePageTest", org.getId());
    URL zippedReport = ReportHelper.zipReport("/canned-reports/large-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    TestReportEvaluator evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, baseUrlFromTest, work);
    evaluator.evaluatePolicy();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
  }

  @Test
  public void testBulkWaiveButtonDisplayed() {
    Button bulkWaiveButton = new Button("#bulk-waive");
    bulkWaiveButton.shouldBe(visible);
  }

  @Test
  public void testClickingBulkWaiveButtonNavigatesToBulkWaivePage() {
    Button bulkWaiveButton = new Button("#bulk-waive");
    waitForBulkWaiveButtonReady();
    bulkWaiveButton.click();
    BulkWaivePage.waitUntilSpinnersGone();

    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    bulkWaivePage.shouldBe(visible);
  }

  @Test
  public void testBulkWaivePageTitle() {
    navigateToBulkWaivePage();

    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    bulkWaivePage.tileHeaderTitle().shouldBe(visible).shouldHave(text("Choose violations to Waive"));

    BulkWaiveTitle bulkWaiveTitle = new BulkWaiveTitle();
    bulkWaiveTitle.shouldBe(visible);
    bulkWaiveTitle.title().shouldHave(text("Bulk Waiver"));
    bulkWaiveTitle.subtitle().shouldHave(text(app.getName() + " Build Report"));
  }

  @Test
  public void testBulkWaivePageTableHeaders() {
    navigateToBulkWaivePage();

    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    BulkWaivePage.BulkWaiveTable table = bulkWaivePage.table();

    table.selectAllCheckbox().shouldBe(visible);
    table.threatHeaderCell().shouldBe(visible).shouldHave(text("Threat"));
    table.policyHeaderCell().shouldBe(visible).shouldHave(text("Policy"));
    table.componentHeaderCell().shouldBe(visible).shouldHave(text("Component"));
    table.conditionHeaderCell().shouldBe(visible).shouldHave(text("Condition"));
  }

  @Test
  public void testBulkWaivePageHasViolationRows() {
    navigateToBulkWaivePage();

    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    ElementsCollection rows = bulkWaivePage.table().rows();
    rows.shouldHave(sizeGreaterThan(0));
  }

  @Test
  public void testViolationRowElements() {
    navigateToBulkWaivePage();

    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    BulkWaivePage.BulkWaiveTableRow firstRow = bulkWaivePage.table().row(1);

    firstRow.checkbox().shouldBe(visible);
    firstRow.threatLevel().shouldBe(visible);
    firstRow.policyName().shouldBe(visible);
    firstRow.componentName().shouldBe(visible);
    firstRow.condition().shouldBe(visible);
  }

  @Test
  public void testCheckboxSelection() {
    navigateToBulkWaivePage();

    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    BulkWaivePage.BulkWaiveTableRow firstRow = bulkWaivePage.table().row(1);

    firstRow.clickCheckbox();
    bulkWaivePage.selectedCountMessage().shouldHave(text("1 violation selected"));
    bulkWaivePage.nextButton().shouldBe(enabled);
  }

  @Test
  public void testSelectMultipleViolations() {
    navigateToBulkWaivePage();

    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    BulkWaivePage.BulkWaiveTable table = bulkWaivePage.table();

    table.row(1).clickCheckbox();
    table.row(2).clickCheckbox();
    table.row(3).clickCheckbox();

    bulkWaivePage.selectedCountMessage().shouldHave(text("3 violations selected"));
    bulkWaivePage.nextButton().shouldBe(enabled);
  }

  @Test
  public void testSelectAllCheckbox() {
    navigateToBulkWaivePage();

    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    BulkWaivePage.BulkWaiveTable table = bulkWaivePage.table();

    table.selectAllCheckbox().click();

    bulkWaivePage.selectedCountMessage().shouldHave(text("66 violations selected"));
    bulkWaivePage.nextButton().shouldBe(enabled);
  }

  @Test
  public void testFooterButtonsInitialState() {
    navigateToBulkWaivePage();

    BulkWaivePage bulkWaivePage = new BulkWaivePage();

    bulkWaivePage.selectedCountMessage().shouldHave(text("0 violations selected"));
    bulkWaivePage.cancelButton().shouldBe(visible).shouldBe(enabled);
    bulkWaivePage.nextButton().shouldBe(visible).shouldBe(disabled);
  }

  @Test
  public void testCancelButton() {
    navigateToBulkWaivePage();

    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    bulkWaivePage.cancelButton().click();

    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
    ApplicationReportPage reportPage = new ApplicationReportPage();
    reportPage.shouldBe(visible);
  }

  @Test
  public void testFilterInputs() {
    navigateToBulkWaivePage();

    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    BulkWaivePage.BulkWaiveTable table = bulkWaivePage.table();

    table.policyNameFilter().shouldBe(visible);
    table.componentNameFilter().shouldBe(visible);
  }

  @Test
  public void testPolicyNameFilter() {
    navigateToBulkWaivePage();

    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    BulkWaivePage.BulkWaiveTable table = bulkWaivePage.table();

    table.policyNameFilter().setValue("Security");

    table.rows().shouldHave(sizeGreaterThan(0));
    int filteredRowCount = table.rows().size();

    if (filteredRowCount > 0) {
      table.row(1).policyName().shouldHave(text("Security"));
    }
  }

  @Test
  public void testClickingRowOpensViolationDetailsPopover() {
    navigateToBulkWaivePage();

    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    BulkWaivePage.BulkWaiveTableRow firstRow = bulkWaivePage.table().row(0);

    firstRow.clickRow();

    PolicyViolationDetailPopover popover = new PolicyViolationDetailPopover();
    popover.shouldBe(visible);
    popover.headerPopoverTitle().shouldBe(visible);
  }

  @Test
  public void testFilterButtonOpensFilterPopover() {
    navigateToBulkWaivePage();

    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    ApplicationReportFilter filterPanel = new ApplicationReportFilter();

    filterPanel.shouldNotBe(visible);
    bulkWaivePage.filterToggleButton().click();
    filterPanel.shouldBe(visible);
  }

  @Test
  public void testFilterPopoverDoesNotHaveViolationStateFilter() {
    navigateToBulkWaivePage();

    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    bulkWaivePage.filterToggleButton().click();

    ApplicationReportFilter filterPanel = new ApplicationReportFilter();
    filterPanel.shouldBe(visible);
    filterPanel.violationStateFilter().shouldNot(exist);
  }

  @Test
  public void testFilterWithHiddenSelections() {
    navigateToBulkWaivePage();

    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    BulkWaivePage.BulkWaiveTable table = bulkWaivePage.table();

    table.row(0).clickCheckbox();
    table.row(1).clickCheckbox();
    table.row(2).clickCheckbox();

    bulkWaivePage.selectedCountMessage().shouldHave(text("3 violations selected"));

    table.policyNameFilter().setValue("Arch");

    table.row(0).clickCheckbox();

    bulkWaivePage.selectedCountMessage().shouldHave(text("4 violations selected (3 hidden)"));
  }

  @Test
  public void testClickingNextNavigatesToWaiverConfigurationPage() {
    navigateToBulkWaivePage();

    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    BulkWaivePage.BulkWaiveTable table = bulkWaivePage.table();

    // Select a violation to enable the next button
    table.row(0).clickCheckbox();
    bulkWaivePage.nextButton().shouldBe(enabled);
    bulkWaivePage.nextButton().click();
    WaiverConfigurationPage.waitUntilSpinnersGone();

    WaiverConfigurationPage configPage = new WaiverConfigurationPage();
    configPage.shouldBe(visible);
  }

  @Test
  public void testWaiverConfigurationPageTitle() {
    navigateToBulkWaivePage();
    selectViolationAndNavigateToConfiguration();

    WaiverConfigurationPage configPage = new WaiverConfigurationPage();
    configPage.tileHeaderTitle().shouldBe(visible).shouldHave(text("Waiver Configuration"));

    BulkWaiveTitle bulkWaiveTitle = new BulkWaiveTitle();
    bulkWaiveTitle.shouldBe(visible);
    bulkWaiveTitle.title().shouldHave(text("Bulk Waiver"));
    bulkWaiveTitle.subtitle().shouldHave(text(app.getName() + " Build Report"));
  }

  @Test
  public void testWaiverConfigurationPageElements() {
    navigateToBulkWaivePage();
    selectViolationAndNavigateToConfiguration();

    WaiverConfigurationPage configPage = new WaiverConfigurationPage();

    configPage.scopeDropdown().shouldBe(visible);
    configPage.exactComponentRadio().shouldBe(visible);
    configPage.allVersionsRadio().shouldBe(visible);
    configPage.expirationSelect().shouldBe(visible);
    configPage.reasonSelect().shouldBe(visible);
    configPage.commentsTextarea().shouldBe(visible);
  }

  @Test
  public void testWaiverConfigurationPageButtons() {
    navigateToBulkWaivePage();
    selectViolationAndNavigateToConfiguration();

    WaiverConfigurationPage configPage = new WaiverConfigurationPage();

    configPage.cancelButton().shouldBe(visible).shouldBe(enabled);
    configPage.backButton().shouldBe(visible).shouldBe(enabled);
    configPage.nextButton().shouldBe(visible).shouldBe(disabled);
  }

  @Test
  public void testWaiverConfigurationBackButton() {
    navigateToBulkWaivePage();
    selectViolationAndNavigateToConfiguration();

    WaiverConfigurationPage configPage = new WaiverConfigurationPage();
    configPage.backButton().click();

    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    bulkWaivePage.shouldBe(visible);
  }

  @Test
  public void testWaiverConfigurationCancelButton() {
    navigateToBulkWaivePage();
    selectViolationAndNavigateToConfiguration();

    WaiverConfigurationPage configPage = new WaiverConfigurationPage();
    configPage.cancelButton().click();

    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
    ApplicationReportPage reportPage = new ApplicationReportPage();
    reportPage.shouldBe(visible);
  }

  @Test
  public void testWaiverConfigurationScopeDropdownHasOptions() {
    navigateToBulkWaivePage();
    selectViolationAndNavigateToConfiguration();

    WaiverConfigurationPage configPage = new WaiverConfigurationPage();
    configPage.scopeDropdown().listItems().shouldHave(sizeGreaterThan(0));
  }

  @Test
  public void testWaiverScopeReflectedInConfirmationPage() {
    navigateToBulkWaivePage();
    selectViolationAndNavigateToConfiguration();

    WaiverConfigurationPage configPage = new WaiverConfigurationPage();

    // Complete configuration with default scope (application)
    configPage.expirationSelect().selectOptionContainingText("Never");
    configPage.nextButton().click();

    // Verify scope is shown on confirmation page
    WaiverConfirmationPage confirmationPage = new WaiverConfirmationPage();
    confirmationPage.scopeValue().shouldBe(visible);
    // Scope value should contain the application name
    confirmationPage.scopeValue().shouldHave(text(app.getName()));
  }

  @Test
  public void testWaiverScopePersistedInDatabase() {
    navigateToBulkWaivePage();

    // Select violations
    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    BulkWaivePage.BulkWaiveTable table = bulkWaivePage.table();
    table.row(0).clickCheckbox();
    bulkWaivePage.nextButton().click();

    // Configure waiver with default scope (application)
    WaiverConfigurationPage configPage = new WaiverConfigurationPage();
    configPage.expirationSelect().selectOptionContainingText("Never");
    configPage.nextButton().click();

    // Submit waiver
    WaiverConfirmationPage confirmationPage = new WaiverConfirmationPage();
    confirmationPage.submitButton().click();
    NxSubmitMask.seeAndWaitForDismissal();

    // Verify waivers were created with correct scope (application ID)
    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(waivers).isNotEmpty();

    // All waivers should have the application ID as owner
    assertThat(waivers).allSatisfy(waiver -> assertThat(waiver.getOwnerId()).isEqualTo(app.getId()));
  }

  @Test
  public void testWaiverConfigurationExpirationOptions() {
    navigateToBulkWaivePage();
    selectViolationAndNavigateToConfiguration();

    WaiverConfigurationPage configPage = new WaiverConfigurationPage();
    configPage.expirationSelect().shouldBe(visible);

    // Verify we have expiration options
    configPage.expirationSelect().$$("option").shouldHave(sizeGreaterThan(0));
  }

  @Test
  public void testWaiverConfigurationReasonOptions() {
    navigateToBulkWaivePage();
    selectViolationAndNavigateToConfiguration();

    WaiverConfigurationPage configPage = new WaiverConfigurationPage();
    configPage.reasonSelect().shouldBe(visible);

    // Verify we have reason options
    configPage.reasonSelect().$$("option").shouldHave(sizeGreaterThan(0));
  }

  @Test
  public void testClickingNextOnConfigurationNavigatesToConfirmationPage() {
    navigateToBulkWaivePage();
    selectViolationAndNavigateToConfiguration();

    WaiverConfigurationPage configPage = new WaiverConfigurationPage();
    configPage.expirationSelect().selectOptionContainingText("Never");
    configPage.nextButton().click();

    WaiverConfirmationPage confirmationPage = new WaiverConfirmationPage();
    confirmationPage.shouldBe(visible);
  }

  @Test
  public void testWaiverConfirmationPageTitle() {
    navigateToBulkWaivePage();
    selectViolationAndNavigateToConfirmation();

    WaiverConfirmationPage confirmationPage = new WaiverConfirmationPage();
    confirmationPage.tileHeaderTitle().shouldBe(visible).shouldHave(text("Confirmation"));

    BulkWaiveTitle bulkWaiveTitle = new BulkWaiveTitle();
    bulkWaiveTitle.shouldBe(visible);
    bulkWaiveTitle.title().shouldHave(text("Bulk Waiver"));
    bulkWaiveTitle.subtitle().shouldHave(text(app.getName() + " Build Report"));
  }

  @Test
  public void testWaiverConfirmationPageShowsViolationsBeingWaived() {
    navigateToBulkWaivePage();

    // Select 3 violations
    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    BulkWaivePage.BulkWaiveTable table = bulkWaivePage.table();
    table.row(0).clickCheckbox();
    table.row(1).clickCheckbox();
    table.row(2).clickCheckbox();
    bulkWaivePage.nextButton().click();
    WaiverConfigurationPage.waitUntilSpinnersGone();

    WaiverConfigurationPage configPage = new WaiverConfigurationPage();
    configPage.expirationSelect().selectOptionContainingText("Never");
    configPage.nextButton().click();

    WaiverConfirmationPage confirmationPage = new WaiverConfirmationPage();
    confirmationPage.violationsBeingWaivedValue().shouldBe(visible);
    confirmationPage.violationsBeingWaivedValue().shouldHave(text("3 total violations"));
  }

  @Test
  public void testWaiverConfirmationPageShowsPolicyViolationsThreatCounter() {
    navigateToBulkWaivePage();
    selectViolationAndNavigateToConfirmation();

    WaiverConfirmationPage confirmationPage = new WaiverConfirmationPage();
    confirmationPage.policyViolationsThreatCounter().shouldBe(visible);
  }

  @Test
  public void testWaiverConfirmationPageShowsScope() {
    navigateToBulkWaivePage();
    selectViolationAndNavigateToConfirmation();

    WaiverConfirmationPage confirmationPage = new WaiverConfirmationPage();
    confirmationPage.scopeValue().shouldBe(visible);
    // Scope should show application or organization name
    confirmationPage.scopeValue().shouldNotHave(text("--"));
  }

  @Test
  public void testWaiverConfirmationPageShowsComponents() {
    navigateToBulkWaivePage();
    selectViolationAndNavigateToConfirmation();

    WaiverConfirmationPage confirmationPage = new WaiverConfirmationPage();
    confirmationPage.componentsValue().shouldBe(visible);
    // Should show "Exact" or "All Versions"
    confirmationPage.componentsValue().shouldHave(text("Exact").or(text("All Versions")));
  }

  @Test
  public void testWaiverConfirmationPageShowsExpiration() {
    navigateToBulkWaivePage();
    selectViolationAndNavigateToConfirmation();

    WaiverConfirmationPage confirmationPage = new WaiverConfirmationPage();
    confirmationPage.expirationValue().shouldBe(visible);
    // Default should be "Never"
    confirmationPage.expirationValue().shouldHave(text("Never"));
  }

  @Test
  public void testWaiverConfirmationPageShowsReason() {
    navigateToBulkWaivePage();
    selectViolationAndNavigateToConfirmation();

    WaiverConfirmationPage confirmationPage = new WaiverConfirmationPage();
    confirmationPage.reasonValue().shouldBe(visible);
  }

  @Test
  public void testWaiverConfirmationPageShowsComment() {
    navigateToBulkWaivePage();
    selectViolationAndNavigateToConfirmation();

    WaiverConfirmationPage confirmationPage = new WaiverConfirmationPage();
    confirmationPage.commentValue().shouldBe(visible);
  }

  @Test
  public void testWaiverConfirmationPageButtons() {
    navigateToBulkWaivePage();
    selectViolationAndNavigateToConfirmation();

    WaiverConfirmationPage confirmationPage = new WaiverConfirmationPage();
    confirmationPage.cancelButton().shouldBe(visible).shouldBe(enabled);
    confirmationPage.backButton().shouldBe(visible).shouldBe(enabled);
    confirmationPage.submitButton().shouldBe(visible).shouldBe(enabled);
  }

  @Test
  public void testWaiverConfirmationBackButton() {
    navigateToBulkWaivePage();
    selectViolationAndNavigateToConfirmation();

    WaiverConfirmationPage confirmationPage = new WaiverConfirmationPage();
    confirmationPage.backButton().click();

    WaiverConfigurationPage configPage = new WaiverConfigurationPage();
    configPage.shouldBe(visible);
  }

  @Test
  public void testWaiverConfirmationCancelButton() {
    navigateToBulkWaivePage();
    selectViolationAndNavigateToConfirmation();

    WaiverConfirmationPage confirmationPage = new WaiverConfirmationPage();
    confirmationPage.cancelButton().click();

    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
    ApplicationReportPage reportPage = new ApplicationReportPage();
    reportPage.shouldBe(visible);
  }

  @Test
  public void testMixedViolationsAlertNotShownWithExactComponent() {
    navigateToBulkWaivePage();

    // Select multiple violations to increase chances of having mixed violations
    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    BulkWaivePage.BulkWaiveTable table = bulkWaivePage.table();
    table.selectAllCheckbox().click();
    bulkWaivePage.nextButton().click();
    WaiverConfigurationPage.waitUntilSpinnersGone();

    WaiverConfigurationPage configPage = new WaiverConfigurationPage();
    // Select "Exact Component" (default)
    configPage.exactComponentRadio().click();
    configPage.expirationSelect().selectOptionContainingText("Never");
    configPage.nextButton().click();

    WaiverConfirmationPage confirmationPage = new WaiverConfirmationPage();
    // Alert should not be visible when "Exact Component" is selected
    confirmationPage.mixedViolationsAlert().shouldNotBe(visible);
  }

  @Test
  public void testMixedViolationsAlertShownWithAllVersions() {
    navigateToBulkWaivePage();

    // Select multiple violations to ensure we have mixed violations
    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    BulkWaivePage.BulkWaiveTable table = bulkWaivePage.table();
    table.selectAllCheckbox().click();
    bulkWaivePage.nextButton().click();
    WaiverConfigurationPage.waitUntilSpinnersGone();

    WaiverConfigurationPage configPage = new WaiverConfigurationPage();
    // Select "All Versions"
    configPage.allVersionsRadio().click();
    configPage.expirationSelect().selectOptionContainingText("Never");
    configPage.nextButton().click();

    WaiverConfirmationPage confirmationPage = new WaiverConfirmationPage();

    // Alert should be visible when "All Versions" is selected with mixed violations
    confirmationPage.mixedViolationsAlert().shouldBe(visible);
    confirmationPage.mixedViolationsAlert()
        .shouldHave(text("The selected violations contain unknown/unclaimed" +
            " components. When \"All Versions\" is selected, the bulk waiver will only apply to identified components."));
  }

  @Test
  public void testAllVersionsDisabledForOnlyUnknownComponents() {
    navigateToBulkWaivePage();

    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    BulkWaivePage.BulkWaiveTable table = bulkWaivePage.table();

    // Try to find and select a violation with "Unknown" component names
    table.policyNameFilter().setValue("unknown");
    table.row(0).clickCheckbox();

    bulkWaivePage.nextButton().click();
    WaiverConfigurationPage.waitUntilSpinnersGone();

    WaiverConfigurationPage configPage = new WaiverConfigurationPage();

    // If all selected violations are unknown, "Exact Component" should be selected
    // and "All Versions" should be disabled
    configPage.exactComponentRadio().shouldHave(cssClass("tm-checked"));
    configPage.allVersionsRadio().shouldHave(cssClass("nx-radio-checkbox--disabled"));
  }

  @Test
  public void testBulkWaiverSubmit() {
    navigateToBulkWaivePage();

    // Select multiple violations
    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    BulkWaivePage.BulkWaiveTable table = bulkWaivePage.table();
    table.row(0).clickCheckbox();
    table.row(1).clickCheckbox();
    table.row(2).clickCheckbox();
    bulkWaivePage.nextButton().click();

    // Configure waiver with expiration, reason, and comments
    WaiverConfigurationPage configPage = new WaiverConfigurationPage();
    configPage.expirationSelect().selectOptionContainingText("30 Days");
    configPage.reasonSelect().selectOptionContainingText("Not exploitable");
    configPage.commentsTextarea().setValue("Component is not exploitable in our use case");
    configPage.nextButton().click();

    // Verify configuration shows correctly on confirmation page
    WaiverConfirmationPage confirmationPage = new WaiverConfirmationPage();
    confirmationPage.expirationValue().shouldHave(text("30 days"));
    confirmationPage.reasonValue().shouldHave(text("Not exploitable"));
    confirmationPage.commentValue().shouldHave(text("Component is not exploitable in our use case"));

    // Submit waiver
    confirmationPage.submitButton().click();
    NxSubmitMask.seeAndWaitForDismissal();

    // Should navigate back to application report page
    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
    ApplicationReportPage reportPage = new ApplicationReportPage();
    reportPage.shouldBe(visible);

    // Verify waivers were created in database
    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(waivers)
        .isNotEmpty()
        .hasSizeGreaterThanOrEqualTo(3); // At least 3 waivers for 3 violations

    // Store first waiver's config to compare with others
    Date firstExpiryTime = waivers.get(0).getExpiryTime();
    String firstReasonId = waivers.get(0).getWaiverReasonId();

    // Verify all waivers have the same configuration
    assertThat(waivers).allSatisfy(waiver -> {
      assertThat(waiver.getExpiryTime()).isNotNull(); // Has expiration
      assertThat(waiver.getExpiryTime()).isEqualTo(firstExpiryTime); // Same expiration time
      assertThat(waiver.getWaiverReasonId()).isNotNull(); // Has reason
      assertThat(waiver.getWaiverReasonId()).isEqualTo(firstReasonId); // Same reason
      assertThat(waiver.getComment()).isEqualTo("Component is not exploitable in our use case"); // Same comment
      assertThat(waiver.getOwnerId()).isEqualTo(app.getId()); // Same scope (application)
    });
  }

  @Test
  public void testBulkWaiverSubmitWithoutWaivePermission() {
    // Create a user without WAIVE_POLICY_VIOLATIONS permission
    createUser();
    grantPermissions(getUsername(), app.getId(), Permission.READ);

    // log out as admin and log in as a user created above
    logout();
    login();

    // This test bypasses the button workflow in order to test that the form submit throws a permissions error
    // For app users without the WAIVE_POLICY_VIOLATIONS, the "Bulk Waive" button is not rendered so they would not
    // have a way to get to this page through the application without navigating to the page directly via URL
    refreshOrOpen(BulkWaivePage.url(app.getPublicId(), SCAN_ID));
    BulkWaivePage.waitUntilSpinnersGone();

    // Select violations
    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    BulkWaivePage.BulkWaiveTable table = bulkWaivePage.table();
    table.row(0).clickCheckbox();
    bulkWaivePage.nextButton().click();
    WaiverConfigurationPage.waitUntilSpinnersGone();

    // Configure waiver
    WaiverConfigurationPage configPage = new WaiverConfigurationPage();
    configPage.expirationSelect().selectOptionContainingText("Never");
    configPage.commentsTextarea().setValue("Test comment");
    configPage.nextButton().click();

    // Attempt to submit
    WaiverConfirmationPage confirmationPage = new WaiverConfirmationPage();
    confirmationPage.submitButton().click();
    NxSubmitMask.seeAndWaitForDismissal();

    // Verify that submit error is shown
    confirmationPage.submitError().shouldBe(visible);
    confirmationPage.submitError().shouldHave(text("Insufficient permissions"));

    // Cleanup: logout and login back as admin
    logout();
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Test
  public void testComponentDetailsPageBulkWaiverSubmit() {
    // Wait until we're on component details page
    String hash = "197d803ab63dd3523d9d";
    refreshOrOpen(ComponentDetailsPage.urlToOverview(app, SCAN_ID, hash));

    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.shouldBe(visible);
    componentDetailsPage.violationsTab().click();

    // Click on Bulk Waive button from component details page
    Button bulkWaiveButton = new Button("#bulk-waive");
    bulkWaiveButton.shouldBe(visible);
    waitForBulkWaiveButtonReady();
    bulkWaiveButton.click();
    BulkWaivePage.waitUntilSpinnersGone();

    // Verify we're on the CDP bulk waive page
    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    bulkWaivePage.shouldBe(visible);
    bulkWaivePage.table().constraintHeaderCell().shouldBe(visible);
    bulkWaivePage.table().componentHeaderCell().shouldNotBe(visible);

    BulkWaiveTitle bulkWaiveTitle = new BulkWaiveTitle();
    bulkWaiveTitle.shouldBe(visible);
    bulkWaiveTitle.title().shouldHave(text("Bulk Waiver"));
    bulkWaiveTitle.subtitle().shouldHave(text("org.springframework.security : spring-security-web : 3.2.4.RELEASE"));

    // Verify violations are filtered to only this component
    BulkWaivePage.BulkWaiveTable table = bulkWaivePage.table();
    ElementsCollection rows = table.rows();
    rows.shouldHave(size(3));

    // Select violations
    table.row(0).clickCheckbox();
    table.row(1).clickCheckbox();
    bulkWaivePage.nextButton().click();
    WaiverConfigurationPage.waitUntilSpinnersGone();

    // Configure waiver
    WaiverConfigurationPage configPage = new WaiverConfigurationPage();
    configPage.expirationSelect().selectOptionContainingText("14 Days");
    configPage.reasonSelect().selectOptionContainingText("Mitigated externally");
    configPage.commentsTextarea().setValue("CDP-level bulk waiver test");
    configPage.nextButton().click();

    // Verify configuration on confirmation page
    WaiverConfirmationPage confirmationPage = new WaiverConfirmationPage();
    confirmationPage.expirationValue().shouldHave(text("14 days"));
    confirmationPage.reasonValue().shouldHave(text("Mitigated externally"));
    confirmationPage.commentValue().shouldHave(text("CDP-level bulk waiver test"));

    // Submit waiver
    confirmationPage.submitButton().click();
    NxSubmitMask.seeAndWaitForDismissal();

    // Should navigate back to component details page
    waitUntilUrl(ComponentDetailsPage.urlToViolations(app, SCAN_ID, hash));

    // Verify waivers were created in database
    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(waivers)
        .isNotEmpty()
        .hasSizeGreaterThanOrEqualTo(2); // At least 2 waivers for 2 violations

    // Store first waiver's config to compare with others
    Date firstExpiryTime = waivers.get(0).getExpiryTime();
    String firstReasonId = waivers.get(0).getWaiverReasonId();

    // Verify all waivers have the same configuration
    assertThat(waivers).allSatisfy(waiver -> {
      assertThat(waiver.getExpiryTime()).isNotNull(); // Has expiration
      assertThat(waiver.getExpiryTime()).isEqualTo(firstExpiryTime); // Same expiration time
      assertThat(waiver.getWaiverReasonId()).isNotNull(); // Has reason
      assertThat(waiver.getWaiverReasonId()).isEqualTo(firstReasonId); // Same reason
      assertThat(waiver.getComment()).isEqualTo("CDP-level bulk waiver test"); // Same comment
      assertThat(waiver.getOwnerId()).isEqualTo(app.getId()); // Same scope (application)
    });
  }

  @Test
  public void testPrioritiesPageBulkWaiverSubmit() {
    // Navigate to priorities page first
    refreshOrOpen(PrioritiesPage.url(app.getPublicId(), SCAN_ID));

    PrioritiesPage prioritiesPage = new PrioritiesPage();
    prioritiesPage.shouldBe(visible);

    // Click on a component row to navigate to priorities-level CDP
    ElementsCollection rows = prioritiesPage.prioritiesTableRows();
    rows.shouldHave(sizeGreaterThan(0));
    prioritiesPage.rowComponentLink(2).click(); // 3rd component has multiple violations

    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.shouldBe(visible);
    componentDetailsPage.violationsTab().click();

    // Click on Bulk Waive button
    Button bulkWaiveButton = new Button("#bulk-waive");
    bulkWaiveButton.shouldBe(visible);
    waitForBulkWaiveButtonReady();
    bulkWaiveButton.click();
    BulkWaivePage.waitUntilSpinnersGone();

    // Verify we're on the bulk waive page
    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    bulkWaivePage.shouldBe(visible);

    BulkWaivePage.BulkWaiveTable table = bulkWaivePage.table();

    // Select violation
    table.row(0).clickCheckbox();
    table.row(1).clickCheckbox();

    bulkWaivePage.nextButton().click();
    WaiverConfigurationPage.waitUntilSpinnersGone();

    // Configure waiver
    WaiverConfigurationPage configPage = new WaiverConfigurationPage();
    configPage.expirationSelect().selectOptionContainingText("7 Days");
    configPage.reasonSelect().selectOptionContainingText("Researching");
    configPage.commentsTextarea().setValue("Priorities page bulk waiver test");
    configPage.nextButton().click();

    // Verify configuration on confirmation page
    WaiverConfirmationPage confirmationPage = new WaiverConfirmationPage();
    confirmationPage.expirationValue().shouldHave(text("7 days"));
    confirmationPage.reasonValue().shouldHave(text("Researching"));
    confirmationPage.commentValue().shouldHave(text("Priorities page bulk waiver test"));

    // Submit waiver
    confirmationPage.submitButton().click();
    NxSubmitMask.seeAndWaitForDismissal();

    // Verify waivers were created in database
    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(app.getId());
    assertThat(waivers)
        .isNotEmpty()
        .hasSize(2);

    // Store first waiver's config to compare with others
    Date firstExpiryTime = waivers.get(0).getExpiryTime();
    String firstReasonId = waivers.get(0).getWaiverReasonId();

    // Verify all waivers have the same configuration
    assertThat(waivers).allSatisfy(waiver -> {
      assertThat(waiver.getExpiryTime()).isNotNull(); // Has expiration
      assertThat(waiver.getExpiryTime()).isEqualTo(firstExpiryTime); // Same expiration time
      assertThat(waiver.getWaiverReasonId()).isNotNull(); // Has reason
      assertThat(waiver.getWaiverReasonId()).isEqualTo(firstReasonId); // Same reason
      assertThat(waiver.getComment()).isEqualTo("Priorities page bulk waiver test"); // Same comment
      assertThat(waiver.getOwnerId()).isEqualTo(app.getId()); // Same scope (application)
    });
  }

  @Test
  public void testBackNavigationPreservesStateAndSelectionChange() {
    navigateToBulkWaivePage();

    // Select one violation
    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    BulkWaivePage.BulkWaiveTable table = bulkWaivePage.table();
    table.row(0).clickCheckbox();
    bulkWaivePage.selectedCountMessage().shouldHave(text("1 violation selected"));
    bulkWaivePage.nextButton().click();
    WaiverConfigurationPage.waitUntilSpinnersGone();

    // Configure waiver with specific values
    WaiverConfigurationPage configPage = new WaiverConfigurationPage();
    configPage.expirationSelect().selectOptionContainingText("30 Days");
    configPage.reasonSelect().selectOptionContainingText("Not exploitable");
    configPage.commentsTextarea().setValue("Test configuration preservation");
    configPage.nextButton().click();

    // Verify we're on confirmation page
    WaiverConfirmationPage confirmationPage = new WaiverConfirmationPage();
    confirmationPage.shouldBe(visible);
    confirmationPage.expirationValue().shouldHave(text("30 days"));
    confirmationPage.reasonValue().shouldHave(text("Not exploitable"));
    confirmationPage.commentValue().shouldHave(text("Test configuration preservation"));

    // Click back to configuration page - configuration should be preserved
    confirmationPage.backButton().click();
    configPage.shouldBe(visible);
    configPage.expirationSelect().getSelectedOption().shouldHave(text("30 Days"));
    configPage.reasonSelect().getSelectedOption().shouldHave(text("Not exploitable"));
    configPage.commentsTextarea().shouldHave(value("Test configuration preservation"));

    // Click back to selection page - selection should be preserved
    configPage.backButton().click();
    bulkWaivePage.shouldBe(visible);
    bulkWaivePage.selectedCountMessage().shouldHave(text("1 violation selected"));
    table.row(0).checkbox().shouldHave(cssClass("tm-checked")); // First row should still be checked

    // Select one more violation (now 2 total)
    table.row(1).clickCheckbox();
    bulkWaivePage.selectedCountMessage().shouldHave(text("2 violations selected"));
    bulkWaivePage.nextButton().click();
    WaiverConfigurationPage.waitUntilSpinnersGone();

    // Configuration should be reset when selection changes
    configPage.shouldBe(visible);
    configPage.expirationSelect().getSelectedOption().shouldNotHave(text("30 Days"));
    configPage.expirationSelect().getSelectedOption().shouldHave(text("Select"));

    configPage.reasonSelect().getSelectedOption().shouldNotHave(text("Not exploitable"));
    configPage.reasonSelect().getSelectedOption().shouldHave(text("Select"));

    configPage.commentsTextarea().shouldNotHave(value("Test configuration preservation"));
    configPage.commentsTextarea().shouldHave(value(""));
  }

  private void selectViolationAndNavigateToConfirmation() {
    selectViolationAndNavigateToConfiguration();
    WaiverConfigurationPage configPage = new WaiverConfigurationPage();
    // Select an expiration time to enable the Next button
    configPage.expirationSelect().selectOptionContainingText("Never");
    configPage.nextButton().click();
  }

  private void navigateToBulkWaivePage() {
    Button bulkWaiveButton = new Button("#bulk-waive");
    waitForBulkWaiveButtonReady();
    bulkWaiveButton.click();
    BulkWaivePage.waitUntilSpinnersGone();
  }

  /**
   * Wait for the bulk waive button's permission check spinner to disappear.
   * The button shows a spinner while checking WAIVE_POLICY_VIOLATIONS permission.
   */
  private void waitForBulkWaiveButtonReady() {
    $(".nx-loading-spinner__icon").shouldNotBe(visible, Duration.ofSeconds(10));
  }

  private void selectViolationAndNavigateToConfiguration() {
    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    BulkWaivePage.BulkWaiveTable table = bulkWaivePage.table();
    table.row(0).clickCheckbox();
    bulkWaivePage.nextButton().click();
    WaiverConfigurationPage.waitUntilSpinnersGone();
  }

  @Test
  public void testPreviousSubmissionErrorClearedOnReEntry() {
    // Create a user without WAIVE_POLICY_VIOLATIONS permission to trigger submit error
    createUser();
    grantPermissions(getUsername(), app.getId(), Permission.READ);

    logout();
    login();

    refreshOrOpen(BulkWaivePage.url(app.getPublicId(), SCAN_ID));
    BulkWaivePage.waitUntilSpinnersGone();

    BulkWaivePage bulkWaivePage = new BulkWaivePage();
    bulkWaivePage.table().row(0).clickCheckbox();
    bulkWaivePage.nextButton().click();
    WaiverConfigurationPage.waitUntilSpinnersGone();

    WaiverConfigurationPage configPage = new WaiverConfigurationPage();
    configPage.expirationSelect().selectOptionContainingText("30 Days");
    configPage.nextButton().click();

    WaiverConfirmationPage confirmationPage = new WaiverConfirmationPage();
    confirmationPage.submitButton().click();
    NxSubmitMask.seeAndWaitForDismissal();

    confirmationPage.submitError().shouldBe(visible);
    confirmationPage.submitError().shouldHave(text("Insufficient permissions"));

    confirmationPage.backButton().click();
    configPage.expirationSelect().selectOptionContainingText("60 Days");
    configPage.nextButton().click();

    confirmationPage.submitError().shouldNotBe(visible);

    // Cleanup: logout and login back as admin
    logout();
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }
}
