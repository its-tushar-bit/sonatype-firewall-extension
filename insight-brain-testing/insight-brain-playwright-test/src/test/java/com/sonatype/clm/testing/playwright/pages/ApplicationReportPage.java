/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.sonatype.insight.brain.model.Application;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static com.microsoft.playwright.options.WaitForSelectorState.HIDDEN;
import static com.microsoft.playwright.options.WaitForSelectorState.VISIBLE;

public class ApplicationReportPage
    extends BasePage
{
  private static final Pattern BACK_BUTTON_PATTERN = Pattern.compile("Back to |All Reports");

  private static final Pattern REEVALUATE_BUTTON_PATTERN =
      Pattern.compile("Re-Evaluate Report|Re-Evaluate Container");

  private static final Pattern VIOLATION_CAPTION_PATTERN = Pattern.compile("VIOLATION");

  private static final Pattern VIOLATION_SUBCAPTION_PATTERN = Pattern.compile("Affecting .+ component");

  private static final Pattern COMPONENT_CAPTION_PATTERN = Pattern.compile("COMPONENT");

  private static final Pattern COVERAGE_SUBCAPTION_PATTERN = Pattern.compile("% of all components");

  private static final Pattern WAIVER_INDICATOR_PATTERN =
      Pattern.compile("Active Waiver|Waived Violation|Unapplied Waiver");

  private static final Pattern THREAT_NUMBER_PATTERN = Pattern.compile("^\\d+$");

  private static final Locator.GetByRoleOptions OPTIONS_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("Options");

  private static final Locator.GetByRoleOptions VIEW_DEPENDENCY_TREE_OPTS =
      new Locator.GetByRoleOptions().setName("View Dependency Tree");

  private static final Locator.GetByRoleOptions VIEW_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("View");

  private static final Locator.GetByRoleOptions CUSTOMIZE_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("Customize");

  private static final Locator.GetByRoleOptions BACK_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName(BACK_BUTTON_PATTERN);

  private static final Locator.GetByRoleOptions REEVALUATE_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName(REEVALUATE_BUTTON_PATTERN);

  private static final Locator.GetByRoleOptions FULL_REEVALUATE_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("Re-Evaluate").setExact(true);

  private static final Locator.GetByRoleOptions VIOLATION_CAPTION_OPTS =
      new Locator.GetByRoleOptions().setLevel(3).setName(VIOLATION_CAPTION_PATTERN);

  private static final Locator.GetByRoleOptions COMPONENT_CAPTION_OPTS =
      new Locator.GetByRoleOptions().setLevel(3).setName(COMPONENT_CAPTION_PATTERN);

  private static final Locator.GetByRoleOptions H1_OPTS =
      new Locator.GetByRoleOptions().setLevel(1);

  private static final Locator.GetByRoleOptions UNSCANNABLE_HEADING_OPTS =
      new Locator.GetByRoleOptions().setName("Unscannable Components");

  public ApplicationReportPage() {
    super();
  }

  public static String url(Application app, String scanId) {
    return url(app.getPublicId(), scanId);
  }

  public static String url(String appPublicId, String scanId) {
    return "/assets/index.html#/applicationReport/" + appPublicId + "/" + scanId + "/policy";
  }

  public Locator container() {
    return locator("#iq-report-container");
  }

  public Locator violationRows() {
    return appReportMain().getByRole(AriaRole.TABLE).locator("tbody").getByRole(AriaRole.ROW);
  }

  public Locator componentFilter() {
    return appReportMain().getByPlaceholder("component name");
  }

  public Locator policyNameFilter() {
    return appReportMain().getByPlaceholder("policy name");
  }

  /**
   * CSS-class anchored: RSC composes the column header's accessible name as
   * {@code "Threat <sortDir>"} (and stamps an SVG layer that leaks into name calculation), so a
   * role+name locator is fragile across sort states. {@code aria-sort} is asserted by callers.
   */
  public Locator threatColumnHeader() {
    return appReportMain().locator("th.iq-app-report__threat-cell");
  }

  public Locator loadButton() {
    return byRole(AriaRole.BUTTON, "Load More Results");
  }

  public Locator appReportMain() {
    return locator("#app-report");
  }

  public Locator reportHeaderTitle() {
    return appReportMain().getByRole(AriaRole.HEADING, H1_OPTS);
  }

  public Locator firstApplicationReportResultRow() {
    return applicationReportResultRows().first();
  }

  public Locator applicationReportResultRows() {
    return appReportMain().getByRole(AriaRole.TABLE).locator("tbody").getByRole(AriaRole.ROW);
  }

  public void openFirstComponentFromReport() {
    firstApplicationReportResultRow().click();
  }

  public void openComponentFromReportRow(int rowIndex) {
    applicationReportResultRows().nth(rowIndex).click();
  }

  public Locator threatIndicatorsCritical() {
    return appReportMain().getByTitle("Critical");
  }

  public Locator threatIndicatorsSevere() {
    return appReportMain().getByTitle("Severe");
  }

  public Locator threatIndicatorsModerate() {
    return appReportMain().getByTitle("Moderate");
  }

  public Locator threatIndicatorsCaption() {
    return appReportMain().getByRole(AriaRole.HEADING, VIOLATION_CAPTION_OPTS);
  }

  public Locator threatIndicatorsSubCaption() {
    return appReportMain().getByText(VIOLATION_SUBCAPTION_PATTERN);
  }

  public Locator coverageCaption() {
    return appReportMain().getByRole(AriaRole.HEADING, COMPONENT_CAPTION_OPTS);
  }

  public Locator coverageSubCaption() {
    return appReportMain().getByText(COVERAGE_SUBCAPTION_PATTERN);
  }

  public Locator reevaluateButton() {
    return appReportMain().getByRole(AriaRole.BUTTON, REEVALUATE_BUTTON_OPTS);
  }

  public Locator fullReevaluateButton() {
    return reevaluationOptionsModal().getByRole(AriaRole.BUTTON, FULL_REEVALUATE_BUTTON_OPTS);
  }

  public Locator reevaluationStatusModal() {
    return locator("#iq-reevaluation-status-modal");
  }

  public Locator reevaluationOptionsModal() {
    return byRole(AriaRole.DIALOG).filter(
        new Locator.FilterOptions().setHasText("Re-Evaluate Report"));
  }

  public Locator aggregateByComponentToggle() {
    return appReportMain().getByText("Aggregate by component");
  }

  /**
   * The toggle's underlying {@code <input role="switch">}, for {@code isChecked()} assertions.
   * Use {@link #aggregateByComponentToggle()} for clicks.
   */
  public Locator aggregateByComponentToggleInput() {
    return appReportMain().getByRole(AriaRole.SWITCH,
        new Locator.GetByRoleOptions().setName("Aggregate by component"));
  }

  /** "N transitive violation(s)" badge — substring match covers singular and plural. */
  public Locator transitiveViolationsBadgeIn(Locator row) {
    return row.getByText("transitive violation");
  }

  public Locator legacyIndicatorTagIn(Locator row) {
    return row.getByText("Legacy", new Locator.GetByTextOptions().setExact(true));
  }

  public Locator filterToggleButton() {
    return appReportMain().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Filter").setExact(true));
  }

  /** Portaled to {@code .nx-page} outside {@link #appReportMain()}, so lookup is page-scoped. */
  public Locator filterPopover() {
    return locator("#iq-component-filter-popover");
  }

  public Locator violationStateFilter() {
    return filterPopover().locator("#violation-state-filter");
  }

  public Locator violationStateOption(String optionName) {
    return filterPopover().getByText(optionName, new Locator.GetByTextOptions().setExact(true));
  }

  public void triggerFullReevaluationAndWait() {
    assertThat(reevaluateButton()).isVisible();
    reevaluateButton().click();
    reevaluationOptionsModal().waitFor(new Locator.WaitForOptions().setState(VISIBLE));
    assertThat(fullReevaluateButton()).isEnabled();
    fullReevaluateButton().click();
    reevaluationOptionsModal().waitFor(new Locator.WaitForOptions().setState(HIDDEN));
    reevaluationStatusModal().waitFor(new Locator.WaitForOptions().setState(VISIBLE));
    reevaluationStatusModal().waitFor(new Locator.WaitForOptions().setState(HIDDEN));
  }

  public Locator optionsDropdown() {
    return appReportMain().getByRole(AriaRole.BUTTON, OPTIONS_BUTTON_OPTS);
  }

  public Locator viewVulnerabilitiesLink() {
    return byRole(AriaRole.LINK, "View vulnerabilities");
  }

  public Locator viewRawDataLink() {
    return byRole(AriaRole.LINK, "View raw data");
  }

  public Locator viewDependencyTreeButton() {
    return appReportMain().getByRole(AriaRole.BUTTON, VIEW_DEPENDENCY_TREE_OPTS);
  }

  public void navigateToDependencyTree() {
    viewDependencyTreeButton().click();
  }

  public void navigateToVulnerabilities() {
    optionsDropdown().click();
    viewVulnerabilitiesLink().click();
  }

  public void navigateToRawData() {
    optionsDropdown().click();
    viewRawDataLink().click();
  }

  public Locator backButton() {
    return appReportMain().getByRole(AriaRole.LINK, BACK_BUTTON_OPTS);
  }

  public Locator unscannableComponentsAlert() {
    return appReportMain().getByRole(AriaRole.ALERT)
        .filter(
            new Locator.FilterOptions().setHasText("unscannable components"));
  }

  public Locator unscannableViewButton() {
    return unscannableComponentsAlert().getByRole(AriaRole.BUTTON, VIEW_BUTTON_OPTS);
  }

  public Locator unscannedComponentsModal() {
    return page.getByRole(AriaRole.DIALOG)
        .filter(
            new Locator.FilterOptions().setHasText("Unscannable Components"));
  }

  public Locator unscannedComponentsModalHeader() {
    return unscannedComponentsModal().getByRole(AriaRole.HEADING, UNSCANNABLE_HEADING_OPTS);
  }

  public Locator unscannedComponentsModalCloseButton() {
    return unscannedComponentsModal().getByRole(AriaRole.BUTTON, CommonButtonOptions.CLOSE_BUTTON_OPTS);
  }

  public Locator policyTypeFilterWarning() {
    return appReportMain().getByText("Policy Types filter introduced in release 61");
  }

  public Locator oldReportWarning() {
    return appReportMain().getByText("generated with an older version of IQ");
  }

  public Locator reevaluationErrorAlert() {
    return appReportMain().getByRole(AriaRole.ALERT);
  }

  public Locator dependencyTreeContainer() {
    return byRole(AriaRole.MAIN).filter(
        new Locator.FilterOptions().setHas(byRole(AriaRole.HEADING, "Dependency Tree")));
  }

  public Locator vulnerabilitiesContainer() {
    return locator("#application-report-vulnerabilities");
  }

  public Locator vulnerabilityRows() {
    return vulnerabilitiesContainer().getByRole(AriaRole.TABLE)
        .locator("tbody")
        .getByRole(AriaRole.ROW);
  }

  public Locator vulnerabilityCustomizeButton() {
    return vulnerabilityRows().first().getByRole(AriaRole.BUTTON, CUSTOMIZE_BUTTON_OPTS);
  }

  public Locator vulnerabilityRefIdLink() {
    return vulnerabilityRows().first().getByRole(AriaRole.LINK).first();
  }

  public Locator violationRowThreatNumber(Locator row) {
    return row.getByRole(AriaRole.CELL).nth(0).getByText(THREAT_NUMBER_PATTERN);
  }

  /**
   * Sibling of {@link #violationRowThreatNumber} that returns every row's number in one
   * {@code allInnerTexts()} round-trip, avoiding {@code count()}+{@code nth(i)} TOCTOU races.
   */
  public Locator violationRowThreatNumbers() {
    return violationRows().getByRole(AriaRole.CELL).nth(0).getByText(THREAT_NUMBER_PATTERN);
  }

  /**
   * CSS-class anchored: {@code ActiveWaiversIndicator} renders a plain {@code <div>} with no
   * {@code role}, {@code aria-label}, or {@code aria-labelledby}, so role/label queries are not
   * viable. The class name is the only stable hook.
   */
  public Locator violationRowWaivedIndicator(Locator row) {
    return row.locator(".iq-waiver-indicator");
  }

  public Locator violationRowThreatIndicator(Locator row) {
    return row.getByRole(AriaRole.CELL).nth(0).locator(".nx-threat-indicator");
  }

  public Locator violationRowPolicyName(Locator row) {
    return row.getByRole(AriaRole.CELL).nth(1);
  }

  public Locator violationRowComponentName(Locator row) {
    return row.getByRole(AriaRole.CELL).nth(2);
  }

  public void waitForLoadingSpinnerHidden() {
    appReportMain().getByRole(AriaRole.STATUS)
        .waitFor(
            new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
  }

  public Locator rawDataContainer() {
    return locator("#application-report-raw-data");
  }

  public Locator policyViolationGroupHeaders() {
    return locator("#app-report .iq-violation-table-category-header," +
        " #app-report .iq-policy-violation-group-header");
  }

  public Locator waivedViolationsIndicator() {
    return appReportMain().getByText(WAIVER_INDICATOR_PATTERN);
  }

  public Locator firstWaivedViolationsIndicator() {
    return waivedViolationsIndicator().first();
  }

  public Locator violationRowForComponent(String componentNameSubstring) {
    return appReportMain().getByRole(AriaRole.TABLE)
        .locator("tbody")
        .getByRole(AriaRole.ROW)
        .filter(
            new Locator.FilterOptions().setHasText(componentNameSubstring));
  }

  public Locator violationRowsForPolicy(String policyName) {
    return violationRows().filter(new Locator.FilterOptions().setHasText(policyName));
  }

  public Locator violationRowForComponentWithPolicy(String componentNameSubstring, String policyName) {
    return violationRows()
        .filter(new Locator.FilterOptions().setHasText(componentNameSubstring))
        .filter(new Locator.FilterOptions().setHasText(policyName));
  }
}
