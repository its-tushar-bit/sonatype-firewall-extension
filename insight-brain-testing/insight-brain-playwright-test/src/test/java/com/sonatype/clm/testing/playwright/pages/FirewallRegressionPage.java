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
  }

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

  /**
   * Threat-level cell (first {@code
   *
  <td>}) in the given zero-based quarantine table row.
   * CSS positional selector is used because the threat-level cell contains only an
   * {@code NxThreatIndicator} SVG — it has no accessible name or stable ID that could be
   * targeted with a role-based selector.
   */
  public Locator quarantineTableThreatLevelCell(int rowIndex) {
    return locator(
        "#iq-firewall-quarantine-table-body tr:nth-child(" + (rowIndex + 1) + ") td:nth-child(1)");
  }

  public Locator quarantineTableRepoLink(int rowIndex) {
    return locator(
        "#iq-firewall-quarantine-table-body tr:nth-child(" + (rowIndex + 1)
            + ") #iq-firewall-quarantine-table--repo-view-link");
  }

  public Locator metricsDetailsButton(String metricId) {
    return locator("#firewall-metrics-content-" + metricId)
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("See details below"));
  }

  public Locator quarantineRefreshButton() {
    return locator("#firewall-quarantine-table--refresh-button");
  }

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

  /**
   * All data rows in the container-image waivers table.
   * CSS class selector is used because {@code ContainerImageWaiversTable} renders a plain
   * {@code
   *
  <table>
   * } with no {@code aria-label} — there is no accessible name to target with
   * {@code getByRole(TABLE)}.
   */
  public Locator containerWaiverTableRows() {
    return locator(".iq-container-image-waivers-table tbody tr");
  }

  /** Quarantine sub-tab inside the Containers panel ({@code id="firewall-container-quarantine-tab"}). */
  public Locator containerQuarantineTab() {
    return locator("#firewall-container-quarantine-tab");
  }

  /** Container-report link within the given container quarantine table row. */
  public Locator containerReportLinkInRow(Locator row) {
    return row.locator("#iq-firewall-container-quarantine-table--container-report-link");
  }

  /**
   * "Waive All Fail Policy Violations" button on the container report page
   * ({@code id="add-container-image-waiver-button"}).
   * Enabled only when {@code activeProxyFailedViolationCount > 0}.
   */
  public Locator addContainerImageWaiverButton() {
    return locator("#add-container-image-waiver-button");
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

  /** All component rows in the BulkWaivePage table body (excludes the "Select all" header row). */
  public Locator bulkWaiveComponentRows() {
    return locator("#fw-bulk-waive-page-main tbody tr");
  }

  /** Individual component checkbox label for the given zero-based row index on the BulkWaivePage. */
  public Locator bulkWaiveComponentCheckbox(int rowIndex) {
    return locator("#fw-bulk-waive-page-main tbody tr:nth-child(" + (rowIndex + 1) + ") label.nx-checkbox");
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

  /** "Submit" button on the FirewallBulkWaiveConfirmationPage — submits all selected waivers. */
  public Locator bulkWaiveConfirmButton() {
    return locator(".fw-bulk-waiver-confirmation-page")
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Submit"));
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
   * "Component" column header in the auto-unquarantine history table.
   * Not sortable and has no stable id; {@code getByRole(COLUMNHEADER)} does not resolve reliably
   * inside an RSC NxTable scope — filtered by text content instead.
   */
  public Locator autoUnquarantineComponentHeader() {
    return locator("#pagination-filter-table thead th")
        .filter(new Locator.FilterOptions().setHasText("Component"));
  }

  /**
   * "Repository" column header in the auto-unquarantine history table.
   * Not sortable and has no stable id; {@code getByRole(COLUMNHEADER)} does not resolve reliably
   * inside an RSC NxTable scope — filtered by text content instead.
   */
  public Locator autoUnquarantineRepositoryHeader() {
    return locator("#pagination-filter-table thead th")
        .filter(new Locator.FilterOptions().setHasText("Repository"));
  }

  /**
   * Chevron column header rendered by {@code <NxTableCell chevron />}.
   * RSC sets {@code rowBtnIcon = faChevronRight} when {@code chevron=true}, which applies
   * {@code nx-cell--row-btn} (not {@code nx-cell--chevron}) — see NxTableCell.js:45.
   */
  public Locator autoUnquarantineChevronHeader() {
    return locator("#pagination-filter-table thead .nx-cell--row-btn");
  }

  /** All data rows in the auto-unquarantine history table body. */
  public Locator autoUnquarantineTableRows() {
    return locator("#iq-firewall-auto-unquarantine-table-body tr");
  }

  /** First data row in the auto-unquarantine history table body — click target for row navigation. */
  public Locator autoUnquarantineTableFirstRow() {
    return locator("#iq-firewall-auto-unquarantine-table-body tr").first();
  }

  /**
   * "Quarantine Date" sortable column {@code
   *
  <th>} ({@code id="quarantineTime-header"}).
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
   * "Date Cleared" sortable column {@code
   *
  <th>} ({@code id="releaseQuarantineTime-header"}).
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

  /**
   * Page {@code
   *
  <h1>} heading "Quarantined Component View".
   */
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

  /**
   * "View Component Details" tertiary button inside the component overview tile.
   * This is the waiver entry point — clicking it navigates to the component details page
   * where a waiver can be created. No direct waive action exists on the quarantined report page itself.
   */
  public Locator quarantinedComponentViewDetailsButton() {
    return locator(QUARANTINED_COMPONENT_REPORT_ROOT)
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("View Component Details"));
  }

  /**
   * Toggle button for the global Solution Switcher dropdown ({@code #iq-solution-switcher}).
   * Uses a CSS selector because the toggle has no accessible name — its label is an SVG icon only.
   */
  public Locator solutionSwitcherToggle() {
    return locator("#iq-solution-switcher .nx-dropdown__toggle");
  }

  /**
   * "Repository Firewall" entry inside the open Solution Switcher dropdown.
   * Only visible after {@link #solutionSwitcherToggle()} has been clicked.
   * If the test license does not include a Firewall product, intercept
   * {@code /api/v2/solutions/licensed} with {@code page.route} to inject the entry.
   */
  public Locator solutionSwitcherFirewallLink() {
    return locator("#iq-solution-switcher")
        .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Repository Firewall"));
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

  /**
   * {@code
   *
  <h2>} heading rendered by the generic {@code UnsavedChangesModal}
   * ({@code #unsaved-modal}) — "Unsaved Changes".
   * Note: {@code IncompleteConfigurationModal} is registered in route data but
   * the transition guard in {@code main.js} always opens the generic modal.
   */
  public Locator incompleteConfigModalHeading() {
    return locator("#unsaved-modal .nx-modal-header h2");
  }

  /** {@code NxWarningAlert} inside {@code IncompleteConfigurationModal} body — discards-changes warning text. */
  public Locator incompleteConfigModalWarningAlert() {
    return locator("#unsaved-modal .nx-modal-content .nx-alert--warning");
  }

}
