/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Extension of {@link FirewallPage} with locators added for regression test coverage.
 * Kept separate to avoid merge conflicts on the pre-existing {@link FirewallPage}.
 */
public class FirewallRegressionPage
    extends FirewallPage
{
  public FirewallRegressionPage() {
    super();
  }

  /**
   * Component-name {@code NxFilterInput} ({@code id="firewall-quarantine-table--component-name"}).
   * ID selector used because {@code NxFilterInput} renders a controlled {@code <input>} inside a
   * labelled wrapper; the accessible name is on the wrapper, not the {@code <input>} itself.
   */
  public Locator componentNameFilter() {
    return locator("#firewall-quarantine-table--component-name");
  }

  /**
   * Clear button inside the component-name {@code NxFilterInput}.
   * Prefer clicking this over {@code fill("")} — {@code NxFilterInput} is React-controlled and
   * {@code fill("")} does not reliably trigger the {@code onChange} handler.
   */
  public Locator componentNameFilterClearButton() {
    return locator(".nx-filter-input:has(#firewall-quarantine-table--component-name) .nx-btn--clear");
  }

  /**
   * Repository-ID {@code NxFilterInput} ({@code id="firewall-quarantine-table--repository-public-id"}).
   * ID selector used for the same reason as {@link #componentNameFilter()}.
   */
  public Locator repositoryFilter() {
    return locator("#firewall-quarantine-table--repository-public-id");
  }

  /**
   * Clear button inside the repository {@code NxFilterInput}.
   * Prefer clicking this over {@code fill("")} for the same reason as
   * {@link #componentNameFilterClearButton()}.
   */
  public Locator repositoryFilterClearButton() {
    return locator(".nx-filter-input:has(#firewall-quarantine-table--repository-public-id) .nx-btn--clear");
  }

  /**
   * "Quarantine Time" dropdown toggle
   * ({@code #firewall-quarantine-table--select-quarantine-time button.nx-dropdown__toggle}).
   * ID+class selector used because the accessible name is on the {@code NxDropdown} wrapper, not the inner toggle
   * button.
   */
  public Locator quarantineTimeFilterToggle() {
    return locator("#firewall-quarantine-table--select-quarantine-time button.nx-dropdown__toggle");
  }

  public Locator quarantineTimeFilterOption(String label) {
    return locator("#firewall-quarantine-table--select-quarantine-time .nx-dropdown-menu button.nx-dropdown-button")
        .filter(new Locator.FilterOptions().setHasText(label));
  }

  public Locator policyFilterToggle() {
    return locator("#firewall-quarantine-table--select-policy button.nx-dropdown__toggle");
  }

  public Locator policyFilterOption(String policyName) {
    return locator("#firewall-quarantine-table--select-policy label.nx-checkbox")
        .filter(new Locator.FilterOptions().setHasText(policyName));
  }

  public Locator policyNameColumnHeader() {
    return locator("#policyName-header button");
  }

  public Locator quarantineTimeColumnHeader() {
    return locator("#quarantineTime-header button");
  }

  public Locator quarantineTablePolicyNameCell(int rowIndex) {
    return locator(
        "#iq-firewall-quarantine-table-body tr:nth-child(" + (rowIndex + 1)
            + ") .iq-policy-cell .nx-truncate-ellipsis");
  }

  /** All policy-name cells in the quarantine table body — use with {@code allInnerTexts()} for sort assertions. */
  public Locator quarantineTablePolicyNameCells() {
    return locator("#iq-firewall-quarantine-table-body tr .iq-policy-cell .nx-truncate-ellipsis");
  }

  public Locator quarantineTableThreatLevelCell(int rowIndex) {
    return locator(
        "#iq-firewall-quarantine-table-body tr:nth-child(" + (rowIndex + 1) + ") td:nth-child(1)");
  }

  public Locator metricsDetailsButton(String metricId) {
    return locator("#firewall-metrics-content-" + metricId)
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("See details below"));
  }

  /**
   * Quarantine table refresh button ({@code id="firewall-quarantine-table--refresh-button"}).
   * ID selector used because this icon button has no visible text and no accessible label.
   */
  public Locator quarantineRefreshButton() {
    return locator("#firewall-quarantine-table--refresh-button");
  }

  /**
   * "Last updated" timestamp cell in the quarantine table ({@code .iq-firewall-table__time}).
   * CSS class selector used because the timestamp td element has no ARIA role or accessible label.
   */
  public Locator quarantineTimestamp() {
    return locator(".iq-firewall-table__time");
  }

  public Locator containerQuarantineTableRows() {
    return locator("#iq-firewall-container-quarantine-table-body tr");
  }

  public Locator containerQuarantineRefreshButton() {
    return locator("#firewall-container-quarantine-table--refresh-button");
  }

  public Locator containerQuarantineTimestamp() {
    return locator("#firewall-container-quarantine-table .iq-firewall-table__time");
  }

  public Locator containerQuarantineTablePolicyCell(int rowIndex) {
    return locator(
        "#iq-firewall-container-quarantine-table-body tr:nth-child(" + (rowIndex + 1)
            + ") .iq-policy-cell .nx-truncate-ellipsis");
  }

  public Locator containerWaiverTableRows() {
    return locator(".iq-container-image-waivers-table tbody tr");
  }

  /** Threat-level cell (first column) within a given quarantine table row locator. */
  public Locator quarantineThreatLevelCellIn(Locator row) {
    return row.locator("td:nth-child(1)");
  }

  /** Expiry cell (third column) within a given container waiver table row locator. */
  public Locator containerWaiverExpiryCellIn(Locator row) {
    return row.locator("td:nth-child(3)");
  }

  /** Component details link within the given quarantine table row locator. */
  public Locator componentDetailsLinkInRow(Locator row) {
    return row.locator("#iq-firewall-quarantine-table--component-details-page");
  }

  /** Repository view link within the given quarantine table row locator. */
  public Locator quarantineTableRepoLinkInRow(Locator row) {
    return row.locator("#iq-firewall-quarantine-table--repo-view-link");
  }

  public static String containerQuarantineTabUrl() {
    return "/assets/index.html#/firewall/dashboard/containers/quarantine";
  }

  public static String waiversContainersApprovedUrl() {
    return "/assets/index.html#/firewall/waivers/containers/approved";
  }

  public void openContainerSubTab(String subTabId) {
    tab("containers").click();
    tab(subTabId).click();
  }

  /** {@code FirewallBulkWaivePage} container ({@code id="fw-bulk-waive-page-main"}). */
  public Locator bulkWaivePageContainer() {
    return locator("#fw-bulk-waive-page-main");
  }

  /** "Select all" checkbox label on the BulkWaivePage — click target ({@code checkboxId="select-all"}). */
  public Locator bulkWaiveSelectAllCheckbox() {
    return locator("#fw-bulk-waive-page-main label.nx-checkbox:has(#select-all)");
  }

  /** Hidden input inside the "Select all" NxCheckbox — use for {@code isEnabled()} wait only, not for clicking. */
  public Locator bulkWaiveSelectAllCheckboxInput() {
    return locator("#fw-bulk-waive-page-main #select-all");
  }

  /** "Next" button on the BulkWaivePage — disabled until at least one violation is selected. */
  public Locator bulkWaivePageNextButton() {
    return locator("#fw-bulk-waive-page-main")
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Next"));
  }

  /** {@code FirewallBulkWaiveConfigurationPage} container. */
  public Locator bulkWaiveConfigPageContainer() {
    return locator(".fw-bulk-waiver-configuration-page");
  }

  /** Waiver expiration select ({@code id="fw-bulk-waiver-expiry-select"}). */
  public Locator bulkWaiveExpirySelect() {
    return locator("#fw-bulk-waiver-expiry-select");
  }

  /** "Next" button on the ConfigurationPage — disabled until scope and expiry are set. */
  public Locator bulkWaiveConfigPageNextButton() {
    return locator(".fw-bulk-waiver-configuration-page")
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Next"));
  }

  /** {@code FirewallBulkWaiveConfirmationPage} container. */
  public Locator bulkWaiveConfirmationPageContainer() {
    return locator(".fw-bulk-waiver-confirmation-page");
  }

  /** "Confirmation" section heading on the ConfirmationPage. */
  public Locator bulkWaiveConfirmationHeading() {
    return locator(".fw-bulk-waiver-confirmation-page")
        .getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(2).setName("Confirmation"));
  }

  private static final String ENTERPRISE_REPORTING_ROOT = "#enterprise-reporting-landing-page";

  public Locator enterpriseReportingContainer() {
    return locator(ENTERPRISE_REPORTING_ROOT);
  }

  public Locator enterpriseDashboardCard(String dashboardId) {
    return locator(ENTERPRISE_REPORTING_ROOT + " #enterprise-reporting-dashboard-" + dashboardId);
  }

  public Locator openDashboardButton(String dashboardId) {
    return enterpriseDashboardCard(dashboardId)
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Open Dashboard"));
  }

  private static final String AUTO_UNQUARANTINE_ROOT = "#firewall-auto-unquarantine-page";

  /** Back navigation button on the Auto-Unquarantine page. */
  public Locator autoUnquarantineBackButton() {
    return locator(AUTO_UNQUARANTINE_ROOT)
        .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Back"));
  }

  /** "Auto Released (Month to Date)" metric card ({@code id="firewall-auto-release-quarantine-mtd"}). */
  public Locator autoUnquarantineMtdCard() {
    return locator("#firewall-auto-release-quarantine-mtd");
  }

  /** "Auto Released (Year to Date)" metric card ({@code id="firewall-auto-release-quarantine-ytd"}). */
  public Locator autoUnquarantineYtdCard() {
    return locator("#firewall-auto-release-quarantine-ytd");
  }

  /**
   * "Quarantine Date" sortable column header cell ({@code id="quarantineTime-header"}).
   * Use for {@code aria-sort} assertions after clicking the sort button.
   */
  public Locator autoUnquarantineQuarantineDateHeader() {
    return locator("#quarantineTime-header");
  }

  /** Sort button inside the "Quarantine Date" column header — click target. */
  public Locator autoUnquarantineQuarantineDateSortButton() {
    return locator("#quarantineTime-header button");
  }

  /**
   * "Date Cleared" sortable column header cell ({@code id="releaseQuarantineTime-header"}).
   * Use for {@code aria-sort} assertions after clicking the sort button.
   */
  public Locator autoUnquarantineDateClearedHeader() {
    return locator("#releaseQuarantineTime-header");
  }

  /** Sort button inside the "Date Cleared" column header — click target. */
  public Locator autoUnquarantineDateClearedSortButton() {
    return locator("#releaseQuarantineTime-header button");
  }

  /**
   * Empty-state cell rendered by {@code NxTableBody} when the auto-unquarantine list is empty
   * ({@code "No data found."} inside {@code #iq-firewall-auto-unquarantine-table-body}).
   */
  public Locator autoUnquarantineTableEmptyMessage() {
    return locator("#iq-firewall-auto-unquarantine-table-body .nx-cell--meta-info");
  }

  private static final String CONTAINER_IMAGE_WAIVER_ROOT = ".add-firewall-container-image-waiver-page";

  public static String addContainerImageWaiverUrl(String publicId, String scanId) {
    return "/assets/index.html#/firewall/containerReport/" + publicId + "/" + scanId
        + "/policy/addContainerImageWaiver";
  }

  /** Root {@code <NxPageMain>} of {@code AddContainerImageWaiverPage}. */
  public Locator addContainerImageWaiverPageContainer() {
    return locator(CONTAINER_IMAGE_WAIVER_ROOT);
  }

  /** "Waiver Configuration" tile heading ({@code id="container-waiver-config-header"}). */
  public Locator containerWaiverConfigHeading() {
    return locator("#container-waiver-config-header");
  }

  /** Waiver expiration select ({@code id="add-container-image-waiver-expiration-select"}). */
  public Locator addContainerImageWaiverExpirationSelect() {
    return locator("#add-container-image-waiver-expiration-select");
  }

  /** Waiver reason select ({@code id="add-container-image-waiver-reason-select"}). */
  public Locator addContainerImageWaiverReasonSelect() {
    return locator("#add-container-image-waiver-reason-select");
  }

  private static final String QUARANTINED_COMPONENT_REPORT_ROOT = "#quarantined-component-report";

  public static String quarantinedComponentReportUrl(String token) {
    return "/assets/index.html#/repositories/quarantinedComponent/" + token;
  }

  /** Root {@code <main>} of {@code QuarantinedComponentReport} ({@code id="quarantined-component-report"}). */
  public Locator quarantinedComponentReportContainer() {
    return locator(QUARANTINED_COMPONENT_REPORT_ROOT);
  }

  /** Page heading "Quarantined Component View" (level-1 heading). */
  public Locator quarantinedComponentReportHeading() {
    return locator(QUARANTINED_COMPONENT_REPORT_ROOT)
        .getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(1));
  }

  /**
   * Component overview tile ({@code .iq-quarantine-report-component-overview-tile}),
   * which shows the component name, quarantine status, quarantine reason (policy violations count),
   * and "First Quarantined" date.
   */
  public Locator quarantinedComponentOverviewTile() {
    return locator(".iq-quarantine-report-component-overview-tile");
  }

  private static final String FIREWALL_ONBOARDING_ROOT = "#firewall-onboarding-page";

  public static String firewallOnboardingUrl() {
    return "/assets/index.html#/firewallOnboarding";
  }

  /** Root {@code <NxPageMain>} of {@code FirewallOnboardingPage} ({@code id="firewall-onboarding-page"}). */
  public Locator firewallOnboardingPageContainer() {
    return locator(FIREWALL_ONBOARDING_ROOT);
  }

  /** "Get Started" button on the WelcomeScreen ({@code id="get-started-button"}). */
  public Locator firewallOnboardingGetStartedButton() {
    return locator(FIREWALL_ONBOARDING_ROOT + " #get-started-button");
  }

  /**
   * Modal root ({@code id="unsaved-modal"}).
   * Shared by the generic {@code UnsavedChangesModal} and {@code IncompleteConfigurationModal}.
   */
  public Locator unsavedChangesModal() {
    return locator("#unsaved-modal");
  }

  /** "Cancel" button ({@code id="unsaved-changes-modal-cancel-button"}). */
  public Locator unsavedChangesModalCancelButton() {
    return locator("#unsaved-changes-modal-cancel-button");
  }

  /** "Continue" button ({@code id="unsaved-changes-modal-continue-button"}). */
  public Locator unsavedChangesModalContinueButton() {
    return locator("#unsaved-changes-modal-continue-button");
  }
}
