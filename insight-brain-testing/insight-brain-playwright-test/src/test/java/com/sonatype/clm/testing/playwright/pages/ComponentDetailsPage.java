/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.sonatype.insight.brain.model.Application;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class ComponentDetailsPage
    extends BasePage
{
  private static final String ROOT = ".nx-page-main.iq-component-details-page";

  private static final String BASE_URL = "/assets/index.html#/applicationReport/";

  public static final String URL_FRAGMENT = "/componentDetails/";

  private static final Locator.GetByRoleOptions CLAIM_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("Claim");

  private static final Locator.GetByRoleOptions REVOKE_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("Revoke");

  public ComponentDetailsPage() {
    super();
  }

  public static String url(Application app, String scanId, String hash) {
    return urlToOverview(app, scanId, hash);
  }

  public static String urlToOverview(Application app, String scanId, String hash) {
    return BASE_URL + app.getPublicId() + "/" + scanId + "/componentDetails/" + hash + "/overview";
  }

  public static String urlToViolations(Application app, String scanId, String hash) {
    return BASE_URL + app.getPublicId() + "/" + scanId + "/componentDetails/" + hash + "/violations";
  }

  public static String urlToSecurity(Application app, String scanId, String hash) {
    return BASE_URL + app.getPublicId() + "/" + scanId + "/componentDetails/" + hash + "/security";
  }

  public static String urlToLegal(Application app, String scanId, String hash) {
    return BASE_URL + app.getPublicId() + "/" + scanId + "/componentDetails/" + hash + "/legal";
  }

  public static String urlToLabels(Application app, String scanId, String hash) {
    return BASE_URL + app.getPublicId() + "/" + scanId + "/componentDetails/" + hash + "/labels";
  }

  public static String urlToAudit(Application app, String scanId, String hash) {
    return BASE_URL + app.getPublicId() + "/" + scanId + "/componentDetails/" + hash + "/audit";
  }

  public Locator container() {
    return locator(ROOT);
  }

  public void waitForReady() {
    container().waitFor();
  }

  public Locator title() {
    return locator("#component-details-title");
  }

  public Locator unknownComponentAlert() {
    return locator(ROOT + " .iq-component-details-unknown-component-alert");
  }

  public Locator proprietaryAlert() {
    return locator("#proprietary-component-matched-alert");
  }

  public Locator tabs() {
    return locator(ROOT + " .nx-tab");
  }

  public Locator header() {
    return locator(ROOT + " .component-details-header");
  }

  public Locator headerReportInfo() {
    return locator(ROOT + " .component-details-header__reportinfo");
  }

  public Locator headerTags() {
    return locator(ROOT + " .component-details-header__tags");
  }

  public Locator footer() {
    return locator(ROOT + " .iq-page-footer");
  }

  public Locator prevLink() {
    return locator(ROOT + " .iq-page-footer .iq-pagination-link__prev");
  }

  public Locator nextLink() {
    return locator(ROOT + " .iq-page-footer .iq-pagination-link__next");
  }

  public Locator pageCounter() {
    return locator(ROOT + " .iq-page-footer .iq-page-counter");
  }

  public Locator violationsTabContent() {
    return locator("#component-details-violations-tab-content");
  }

  public Locator securityTabContent() {
    return locator("#component-details-security-tab-content");
  }

  public Locator labelsTabContent() {
    return locator("#component-details-labels-tab-content");
  }

  public Locator auditTabContent() {
    return locator("#component-details-audit-tab-content");
  }

  public Locator vulnerabilitiesTable() {
    return locator(ROOT + " .iq-policy-vulnerability-table");
  }

  public Locator vulnerabilityRows() {
    return locator(ROOT + " .iq-policy-vulnerability-table .iq-vulnerabilities-row");
  }

  public Locator headerTitle() {
    return locator("#component-details-title");
  }

  public Locator componentDetailsTabs() {
    return page.getByRole(AriaRole.TABLIST,
        new Page.GetByRoleOptions().setName("Component detail tabs"))
        .locator(".nx-tab");
  }

  public Locator componentDetailsTab(String tabLabel) {
    return componentDetailsTabs()
        .filter(new Locator.FilterOptions().setHasText(tabLabel));
  }

  public Locator backButton() {
    return container().locator(".nx-back-button").getByRole(AriaRole.LINK);
  }

  public Locator securityTabPanel() {
    return locator("#component-details-security-tab-content");
  }

  public Locator iqVulnerabilityTable() {
    return locator(".iq-policy-vulnerability-table");
  }

  public Locator iqVulnerabilityTableBodyRows() {
    return locator(".iq-policy-vulnerability-table tbody tr");
  }

  public Locator legalTabPanel() {
    return locator("#component-details-legal-tab-content");
  }

  public Locator licenseDetectionsTile() {
    return locator("#component-details-legal-license-detections-tile");
  }

  public Locator violationsTileTitle() {
    return locator("#violations__tile__title");
  }

  public Locator vulnerabilitiesTileTitle() {
    return locator("#component-details-vulnerabilities-title");
  }

  public Locator legalLicenseDetectionsTile() {
    return licenseDetectionsTile();
  }

  public Locator overviewComponentInformationTile() {
    return locator("#overview-component-information-tile");
  }

  public Locator versionExplorerTile() {
    return locator("#overview-component-risk-remediation-tile");
  }

  /**
   * {@code
   *
  <h2 class="nx-h2">Version Explorer</h2>} header inside the tile above.
   */
  public Locator versionExplorerTileHeader() {
    return locator("#overview-component-risk-remediation-tile .nx-tile-header__title .nx-h2");
  }

  public Locator recommendedVersionsList() {
    return locator("#overview-component-risk-remediation-tile .iq-recommended-version .nx-list");
  }

  public Locator recommendationActionButtons(int index) {
    return recommendedVersionsList().locator(".nx-list__item").nth(index).locator(".nx-list__actions .nx-btn");
  }

  public Locator compareVersionsTable() {
    return locator("#compare-versions-table");
  }

  public Locator compareVersionsVersionRowCells() {
    return compareVersionsTable().locator("#version .nx-cell");
  }

  public Locator labelsTileTitle() {
    return locator("#iq-manage-labels__tile__title");
  }

  public Locator auditLogTable() {
    return locator("#audit-log-table");
  }

  public Locator componentInformationTileHeader() {
    return overviewComponentInformationTile().getByRole(AriaRole.HEADING);
  }

  public Locator componentInformationMatchState() {
    return overviewComponentInformationTile().getByText("Match State")
        .locator("xpath=following-sibling::dd");
  }

  public Locator componentInformationWebsite() {
    return overviewComponentInformationTile().getByRole(AriaRole.LINK)
        .filter(
            new Locator.FilterOptions().setHasText("http"));
  }

  public Locator viewCoordinatesButton() {
    return overviewComponentInformationTile().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("View Coordinates"));
  }

  public Locator auditLogTableRows() {
    return auditLogTable().locator("tbody").getByRole(AriaRole.ROW);
  }

  public Locator auditLogTableHeaderCells() {
    return auditLogTable().locator("thead th");
  }

  public Locator auditLogEmptyMessage() {
    return auditLogTable().getByRole(AriaRole.CELL)
        .filter(
            new Locator.FilterOptions().setHasText("No data"));
  }

  public Locator paginationFooter() {
    return locator("#component-details-footer");
  }

  public Locator paginationPrevLink() {
    return paginationFooter().getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Previous"));
  }

  public Locator paginationNextLink() {
    return paginationFooter().getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Next"));
  }

  public Locator paginationCounter() {
    return paginationFooter().locator(".iq-page-counter");
  }

  public Locator policyViolationsTable() {
    return locator("#iq-policy-violations-table");
  }

  public Locator policyViolationRows() {
    return policyViolationsTable().locator("tbody tr.iq-policy-violation-row");
  }

  public Locator policyViolationDetailsPopover() {
    return locator("#component-details-policy-violations-popover");
  }

  public Locator popoverViolationPage() {
    return policyViolationDetailsPopover().locator("#violation-page");
  }

  public Locator popoverBackButton() {
    return policyViolationDetailsPopover().locator(".nx-back-button").getByRole(AriaRole.LINK);
  }

  public Locator popoverCloseButton() {
    return policyViolationDetailsPopover()
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Close"));
  }

  public Locator popoverSection() {
    return policyViolationDetailsPopover().locator(".iq-violation-details-popover-section").first();
  }

  public Locator popoverTab(String accessibleName) {
    return policyViolationDetailsPopover().getByRole(AriaRole.TAB,
        new Locator.GetByRoleOptions().setName(accessibleName));
  }

  public Locator popoverTab(Pattern accessibleNamePattern) {
    return policyViolationDetailsPopover().getByRole(AriaRole.TAB,
        new Locator.GetByRoleOptions().setName(accessibleNamePattern));
  }

  /**
   * MenuBarBackButton has no accessible name — scoped under {@code #violation-page} inside the
   * popover to avoid matching the popover's own chevron/close button.
   * TODO(a11y): once MenuBarBackButton gains an accessible name, switch to getByRole(LINK, ...).
   */
  public Locator popoverPageLevelBackButton() {
    return popoverViolationPage().locator(":scope > .nx-back-button");
  }

  public Locator popoverAddWaiverButton() {
    return policyViolationDetailsPopover().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Add Waiver"));
  }

  public Locator popoverRequestWaiverDropdownToggle() {
    return policyViolationDetailsPopover().locator(".nx-segmented-btn__dropdown-btn");
  }

  public Locator popoverRequestWaiverMenuItem() {
    return policyViolationDetailsPopover().getByRole(AriaRole.BUTTON)
        .filter(
            new Locator.FilterOptions().setHasText("Request Waiver"));
  }

  public Locator vulnerabilityDetailsPopover() {
    return locator("#component-details-vulnerability-details-popover");
  }

  public Locator securityVulnerabilityOverrideForm() {
    return vulnerabilityDetailsPopover().locator(".iq-security-vulnerability-override-form");
  }

  public Locator vulnerabilityStatusDropdown() {
    return vulnerabilityDetailsPopover().getByRole(AriaRole.COMBOBOX);
  }

  public Locator vulnerabilityOverrideCommentsInput() {
    return vulnerabilityDetailsPopover().getByRole(AriaRole.TEXTBOX);
  }

  public Locator vulnerabilityOverrideSaveButton() {
    return vulnerabilityDetailsPopover()
        .getByRole(AriaRole.BUTTON, CommonButtonOptions.SAVE_BUTTON_OPTS);
  }

  public Locator availableLabelTags() {
    return locator(".iq-transfer-list .iq-transfer-list__half").first()
        .locator("label");
  }

  public Locator appliedLabelTags() {
    return locator(".iq-transfer-list .iq-transfer-list__half").last()
        .locator("label");
  }

  public Locator applyLabelModal() {
    return locator("#iq-apply-label-modal");
  }

  public Locator applyLabelModalHeading() {
    return applyLabelModal().getByRole(AriaRole.HEADING);
  }

  public Locator applyLabelScopeDropdown() {
    return applyLabelModal().getByRole(AriaRole.COMBOBOX);
  }

  public Locator applyLabelModalCancelButton() {
    return applyLabelModal().getByRole(AriaRole.BUTTON, CommonButtonOptions.CANCEL_BUTTON_OPTS);
  }

  public Locator removeLabelModal() {
    return page.getByRole(AriaRole.DIALOG)
        .filter(
            new Locator.FilterOptions().setHasText("Remove Label"));
  }

  public Locator removeLabelModalCancelButton() {
    return removeLabelModal().getByRole(AriaRole.BUTTON, CommonButtonOptions.CANCEL_BUTTON_OPTS);
  }

  public Locator claimForm() {
    return locator("#component-details-claim");
  }

  public Locator claimGroupIdField() {
    return claimForm().getByLabel("Group ID");
  }

  public Locator claimArtifactIdField() {
    return claimForm().getByLabel("Artifact ID");
  }

  public Locator claimVersionField() {
    return claimForm().getByLabel("Version");
  }

  public Locator claimExtensionField() {
    return claimForm().getByLabel("Extension");
  }

  public Locator claimSubmitButton() {
    return claimForm().getByRole(AriaRole.BUTTON, CLAIM_BUTTON_OPTS);
  }

  public Locator claimCancelButton() {
    return claimForm().getByRole(AriaRole.BUTTON, CommonButtonOptions.CANCEL_BUTTON_OPTS);
  }

  public Locator claimRevokeButton() {
    return claimForm().getByRole(AriaRole.BUTTON, REVOKE_BUTTON_OPTS);
  }

  public Locator revokeClaimModal() {
    return page.getByRole(AriaRole.DIALOG)
        .filter(
            new Locator.FilterOptions().setHasText("Revoke"));
  }

  public Locator revokeClaimModalConfirmButton() {
    return revokeClaimModal().getByRole(AriaRole.BUTTON, REVOKE_BUTTON_OPTS);
  }

  public static String urlToClaim(Application app, String scanId, String hash) {
    return BASE_URL + app.getPublicId() + "/" + scanId + "/componentDetails/" + hash + "/claim";
  }

  public void clickComponentDetailsTab(String tabLabel) {
    componentDetailsTab(tabLabel).click();
  }

  public void navigateBackToApplicationReport() {
    backButton().click();
    if (!page.url().contains("/applicationReport/")) {
      page.goBack();
    }
  }

  public void compareRecommendationAndAssertVersions(String currentVersion, String recommendedVersion) {
    assertThat(recommendedVersionsList()).isVisible();
    Locator actions = recommendationActionButtons(0);
    assertThat(actions.last()).isVisible();
    actions.last().click();

    assertThat(compareVersionsTable()).isVisible();
    Locator cells = compareVersionsVersionRowCells();
    assertThat(cells.nth(1)).containsText(currentVersion);
    assertThat(cells.nth(2)).containsText(recommendedVersion);
  }
}
