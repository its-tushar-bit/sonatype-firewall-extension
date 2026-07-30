/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.AriaRole;

public class PrioritiesPage
    extends BasePage
{
  public PrioritiesPage() {
    super();
  }

  public static String url(String appPublicId, String scanId) {
    return "/assets/index.html#/developer/priorities/" + appPublicId + "/" + scanId;
  }

  // componentNameFilter query param is a documented page feature (same mechanism as the in-page search input), not a
  // test workaround. Encode the value to keep '&', '=', and spaces from mis-routing.
  public static String url(String appPublicId, String scanId, String componentNameFilter) {
    return url(appPublicId, scanId)
        + "?componentNameFilter=" + URLEncoder.encode(componentNameFilter, StandardCharsets.UTF_8);
  }

  // ID anchor — scope to the priorities table specifically so this doesn't pick up any future
  // table elsewhere on the page. Same pattern as DeveloperRiskTablePage.
  public Locator container() {
    return locator("#iq-priorities-table");
  }

  public Locator rows() {
    return container().locator("tbody").getByRole(AriaRole.ROW);
  }

  public Locator rowByArtifactId(String artifactId) {
    return container().locator("tbody")
        .getByRole(AriaRole.ROW)
        .filter(new Locator.FilterOptions().setHasText(artifactId));
  }

  public Locator buildActionCell(Locator row) {
    return row.locator("div.iq-priorities-table__build-action");
  }

  public Locator expiredWaiverIcon(Locator row) {
    return buildActionCell(row).locator(".iq-expired-waiver-icon");
  }

  public Locator soonToExpireWaiverIcon(Locator row) {
    return buildActionCell(row).locator(".iq-soon-to-expire-waiver-icon");
  }

  public Locator recommendationCell(Locator row) {
    return row.locator("div.iq-priorities-table__recommendation");
  }

  /**
   * Stubs the per-row recommendation fetch with a no-upgrade payload so the recommendation cell
   * deterministically resolves to the "Waive violations" branch. In non-bulk mode every row fetches
   * {@code allVersions}, but the embedded HdsMockServer only stubs the bulk version-scoring path, so
   * the per-row fetch otherwise 404s and the cell falls through to "Investigate". Call before opening
   * the page; callers unroute in {@code @After} via {@code page.unrouteAll()}.
   */
  public void stubNoUpgradeRecommendations() {
    page.route(Pattern.compile(".*/allVersions(\\?.*)?$"),
        route -> route.fulfill(new Route.FulfillOptions()
            .setStatus(200)
            .setContentType("application/json")
            .setBody("{\"allVersions\":[],\"remediation\":{}}")));
  }

  /** Button that opens the Create Pull Request modal for a component row. */
  public Locator createPrTriggerButton() {
    return byRole(AriaRole.BUTTON, "Create PR");
  }

  public Locator pageMain() {
    return page.getByRole(AriaRole.MAIN);
  }

  /** Scoped under {@link #pageMain()} so future pages reusing this test-id don't collide. */
  public Locator pageHeader() {
    return pageMain().getByTestId("iq-priorities-page-summary-section");
  }

  public Locator pageHeaderTitle() {
    return pageHeader().getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(2));
  }

  public Locator breadcrumbLink(String linkText) {
    return pageHeader().getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(linkText));
  }

  public Locator metadataRowTriggeredByLabel() {
    return pageHeader().getByText("Triggered by:");
  }

  public Locator viewDropdownButton() {
    return pageHeader().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("View"));
  }

  public Locator viewDropdownLifecycleReportLink() {
    return pageHeader().getByRole(AriaRole.LINK,
        new Locator.GetByRoleOptions().setName("Lifecycle Report"));
  }

  public Locator viewDropdownDependenciesLink() {
    return pageHeader().getByRole(AriaRole.LINK,
        new Locator.GetByRoleOptions().setName("Dependencies"));
  }

  public void openViewDropdown() {
    viewDropdownButton().click();
  }

  /**
   * TODO(a11y): frontend renders this as an unlabelled clickable SVG; switch to
   * {@code getByRole(BUTTON, "Copy commit hash")} once an accessible name is added.
   */
  public Locator commitCopyButton() {
    return pageHeader().locator(".iq-priorities-page-copy-commit-btn");
  }

  /** NxTooltip title portals outside the page header — scope by role=tooltip. */
  public Locator commitCopyTooltipCopiedText() {
    return page.getByRole(AriaRole.TOOLTIP).getByText("Copied");
  }

  public Locator componentFilterInput() {
    return page.getByPlaceholder("Filter by component");
  }

  /** NxToggle: click via the label. {@link #failWarnToggleInput()} for {@code isChecked()}. */
  public Locator failWarnToggleLabel() {
    return nxToggleLabel("Fail/Warn Policy Actions only");
  }

  /** NxToggle: hidden {@code role="switch"} input for state assertions. */
  public Locator failWarnToggleInput() {
    return nxToggleInput("Fail/Warn Policy Actions only");
  }

  /**
   * Column header inside the priorities NxTable.
   * {@code
   *
  <th>} elements have implicit role {@code columnheader} per ARIA, but
   * {@code getByRole(COLUMNHEADER)} does not resolve reliably inside an RSC NxTable scope
   * (same workaround as {@code FirewallRegressionPage#autoUnquarantineComponentHeader}) —
   * filtered by role {@code CELL} + exact accessible name instead.
   */
  public Locator columnHeaderByText(String label) {
    return container().locator("thead")
        .getByRole(AriaRole.CELL,
            new Locator.GetByRoleOptions().setName(label).setExact(true));
  }

  public Locator dependencyIndicatorByTitle(String tooltipTitle) {
    return container().getByTitle(tooltipTitle);
  }

  /**
   * The empty-state message cell rendered by {@code NxTable.Body emptyMessage}
   * ({@code tbody .nx-cell--meta-info}) when the active component-name filter produces no matches.
   * The text reads "No Results" (see {@code PrioritiesPageTable.jsx#getEmptyMessage}).
   */
  public Locator emptyStateMessage() {
    return container().locator("tbody .nx-cell--meta-info");
  }

  /**
   * License lock screen scoped to {@link #pageMain()} — {@code LicenseLockScreen} REPLACES the
   * priorities table container, so {@link #container()} is not present when the alert renders
   * (see {@code prioritiesPage/PrioritiesPage.jsx} — the switch is at the {@code PageContents}
   * level, both branches nested inside {@code NxPageMain}). Scoping under {@code role=MAIN}
   * avoids collisions with global banner alerts sharing similar text.
   */
  public Locator licenseLockScreen() {
    return pageMain().getByRole(AriaRole.ALERT)
        .filter(
            new Locator.FilterOptions().setHasText("Sonatype Developer is not enabled."));
  }

}
