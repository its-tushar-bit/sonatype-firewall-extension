/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import com.sonatype.insight.brain.model.Application;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static com.microsoft.playwright.options.WaitForSelectorState.HIDDEN;
import static com.microsoft.playwright.options.WaitForSelectorState.VISIBLE;

/**
 * Playwright page object for the Application Report page.
 */
public class ApplicationReportPage
    extends BasePage
{
  private static final String APP_REPORT = "#app-report";

  private static final String THREAT_INDICATORS = APP_REPORT + " .iq-threat-indicators";

  private static final String COVERAGE_INDICATOR = APP_REPORT + " .iq-coverage-indicator";

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

  public Locator title() {
    return container().getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(1));
  }

  public Locator policyViolationsTable() {
    return locator("#iq-report-container #iq-violation-table");
  }

  public Locator violationRows() {
    return locator("#app-report .nx-table-container tbody tr");
  }

  public Locator componentFilter() {
    return locator("#report-component-name-filter");
  }

  public Locator loadButton() {
    return byRole(AriaRole.BUTTON, "Load More Results");
  }

  /**
   * Application report main container ({@code <main id="app-report">}). Use as a readiness
   * gate before asserting on the report header / table — those render asynchronously after
   * report data is fetched.
   */
  public Locator appReportMain() {
    return locator("#app-report");
  }

  /**
   * Title shown in the application report header (policy tab).
   * <p>
   * Markup (see {@code applicationReport/ReportTitle.jsx}):
   * {@code <main id="app-report"> ... <div class="nx-page-title">
   * <h1 class="nx-h1">{appName} {reportTitle}</h1>}.
   * Scoped to {@code #app-report} so we don't accidentally match other {@code .nx-h1} on the page
   * (e.g. modal headings, banners).
   */
  public Locator reportHeaderTitle() {
    return locator("#app-report .nx-page-title h1.nx-h1");
  }

  /** First row in the application report result list (opens component details). */
  public Locator firstApplicationReportResultRow() {
    return applicationReportResultRows().first();
  }

  /** Result rows in the application report table (each row opens component details). */
  public Locator applicationReportResultRows() {
    // Current app report table rows are rendered in NxTable tbody under #app-report.
    return locator("#app-report .nx-table-container tbody tr");
  }

  public void openFirstComponentFromReport() {
    firstApplicationReportResultRow().click();
  }

  public void openComponentFromReportRow(int rowIndex) {
    applicationReportResultRows().nth(rowIndex).click();
  }

  // --------------- Threat indicators ---------------

  public Locator threatIndicatorsCritical() {
    return locator(THREAT_INDICATORS + " .nx-small-threat-counter--critical");
  }

  public Locator threatIndicatorsSevere() {
    return locator(THREAT_INDICATORS + " .nx-small-threat-counter--severe");
  }

  public Locator threatIndicatorsModerate() {
    return locator(THREAT_INDICATORS + " .nx-small-threat-counter--moderate");
  }

  /** "N VIOLATIONS" caption below the threat counters. */
  public Locator threatIndicatorsCaption() {
    return locator(THREAT_INDICATORS + " .iq-caption__text");
  }

  /** "Affecting N components" sub-caption. */
  public Locator threatIndicatorsSubCaption() {
    return locator(THREAT_INDICATORS + " .iq-caption__sub-text");
  }

  // --------------- Coverage indicator ---------------

  public Locator coverageCaption() {
    return locator(COVERAGE_INDICATOR + " .iq-caption__text");
  }

  public Locator coverageSubCaption() {
    return locator(COVERAGE_INDICATOR + " .iq-caption__sub-text");
  }

  // --------------- Table header controls ---------------

  public Locator componentNameFilter() {
    return locator("#report-component-name-filter");
  }

  // --------------- Actions / navigation ---------------

  public Locator backButton() {
    return locator(".nx-back-button a");
  }

  public Locator reevaluateButton() {
    return locator("#reevaluate-report-button");
  }

  public Locator fullReevaluateButton() {
    return locator("#full-reevaluate-report-button");
  }

  public Locator reevaluationStatusModal() {
    return locator("#iq-reevaluation-status-modal");
  }

  public Locator reevaluationOptionsModal() {
    return locator("#iq-reevaluation-options-modal");
  }

  public Locator aggregateByComponentToggle() {
    return locator("#report-aggregate-by-component-toggle");
  }

  /**
   * Clicks reevaluate, waits for the options modal, clicks Full Re-evaluate,
   * then waits for the status modal to appear and dismiss.
   */
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
}
