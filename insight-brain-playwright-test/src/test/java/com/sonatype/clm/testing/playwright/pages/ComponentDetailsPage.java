/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.sonatype.insight.brain.model.Application;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright page object for the Component Details page.
 */
public class ComponentDetailsPage
    extends BasePage
{
  private static final String ROOT = ".nx-page-main.iq-component-details-page";

  private static final String BASE_URL = "/assets/index.html#/applicationReport/";

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

  // Footer pagination
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

  // Tab content
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

  // Vulnerabilities
  public Locator vulnerabilitiesTable() {
    return locator(ROOT + " .iq-policy-vulnerability-table");
  }

  public Locator vulnerabilityRows() {
    return locator(ROOT + " .iq-policy-vulnerability-table .iq-vulnerabilities-row");
  }

  /**
   * Visible header title on component details (overview).
   * <p>
   * Markup (see {@code componentDetails/ComponentDetails.jsx}):
   * {@code <Title id="component-details-title">{componentDetails.name}</Title>}.
   */
  public Locator headerTitle() {
    return locator("#component-details-title");
  }

  /**
   * Tab buttons in the component details chrome (Violations, Security, Legal, …).
   * <p>
   * Scoped to {@code role=tablist[name="Component detail tabs"]} so we don't pick up other
   * tab lists on the page (NxTabList renders {@code aria-label="Component detail tabs"} —
   * see {@code componentDetails/ComponentDetailsTabs.jsx}).
   */
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
    return locator(".nx-back-button");
  }

  /** Security tab panel root rendered by NxTabPanel for tabId="security". */
  public Locator securityTabPanel() {
    return locator("#component-details-security-tab-content");
  }

  /** Vulnerability table on the Security tab (matches frontend markup in VulnerabilitiesTable.jsx). */
  public Locator iqVulnerabilityTable() {
    return locator(".iq-policy-vulnerability-table");
  }

  public Locator iqVulnerabilityTableBodyRows() {
    return locator(".iq-policy-vulnerability-table tbody tr");
  }

  /** Legal tab panel root rendered by NxTabPanel for tabId="legal". */
  public Locator legalTabPanel() {
    return locator("#component-details-legal-tab-content");
  }

  /** Root section for license detections on the Legal tab (see LicenseDetectionsTile.jsx). */
  public Locator licenseDetectionsTile() {
    return locator("#component-details-legal-license-detections-tile");
  }

  /** Title element rendered inside the Violations tab tile. */
  public Locator violationsTileTitle() {
    return locator("#violations__tile__title");
  }

  /** Title element rendered inside the Vulnerabilities tile on the Security tab. */
  public Locator vulnerabilitiesTileTitle() {
    return locator("#component-details-vulnerabilities-title");
  }

  /** Alias for {@link #licenseDetectionsTile()} (tab navigation assertions). */
  public Locator legalLicenseDetectionsTile() {
    return licenseDetectionsTile();
  }

  /** Component information tile rendered on the Overview tab. */
  public Locator overviewComponentInformationTile() {
    return locator("#overview-component-information-tile");
  }

  /**
   * Risk Remediation / Version Explorer tile on the Overview tab (see
   * {@code componentDetails/overview/riskRemediation/RiskRemediation.jsx}). Renders only when
   * the Version Explorer fetch returned a usable componentidentifier — components without one
   * (e.g. unknown / proprietary matches) intentionally suppress this tile.
   */
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

  /**
   * Recommended-versions list inside the Risk Remediation tile
   * ({@code .iq-recommended-version}).
   */
  public Locator recommendedVersionsList() {
    return locator("#overview-component-risk-remediation-tile .iq-recommended-version .nx-list");
  }

  /**
   * Action buttons (Select / Compare) for the recommendation at {@code index} (0-based).
   */
  public Locator recommendationActionButtons(int index) {
    return recommendedVersionsList().locator(".nx-list__item").nth(index).locator(".nx-list__actions .nx-btn");
  }

  /** Compare Versions table that appears after clicking the Compare button. */
  public Locator compareVersionsTable() {
    return locator("#compare-versions-table");
  }

  /** Row of version cells (current version, recommended version) in the Compare Versions table. */
  public Locator compareVersionsVersionRowCells() {
    return compareVersionsTable().locator("#version .nx-cell");
  }

  /** Manage labels tile title rendered on the Labels tab. */
  public Locator labelsTileTitle() {
    return locator("#iq-manage-labels__tile__title");
  }

  /** Audit log table rendered on the Audit Log tab. */
  public Locator auditLogTable() {
    return locator("#audit-log-table");
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

  /**
   * Clicks the Compare button (last action of the first recommendation) and asserts the
   * Compare Versions table renders with the given current and recommended version cells.
   * Mirrors the legacy Selenide
   * {@code VersionGraphTest#testVersionGraph_debugComponent_CompareVersionButton}.
   */
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
