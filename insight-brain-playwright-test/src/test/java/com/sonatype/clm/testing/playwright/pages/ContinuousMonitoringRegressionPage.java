/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Regression-only locators and actions for the Continuous Monitoring editor.
 *
 * <p>
 * Do NOT add methods to any existing page object.
 */
public class ContinuousMonitoringRegressionPage
    extends BasePage
{
  private static final String POLICY_MONITORING_API = "/rest/policyMonitoring/";

  public ContinuousMonitoringRegressionPage() {
    super();
  }

  /** H1 heading "Continuous Monitoring" rendered by NxH1 inside NxPageTitle. */
  public Locator pageHeading() {
    return byRole(AriaRole.HEADING, "Continuous Monitoring");
  }

  /**
   * NxPageTitle description container ({@code .nx-page-title__description}).
   * CSS class used because the element is a bare {@code
   *
  <p>
   * } with no ARIA role or accessible
   * name; {@code getByText()} would require matching the full prose and would be fragile.
   * {@code .nx-page-title__description} is the stable class from RSC's NxPageTitle component.
   */
  public Locator pageDescription() {
    return locator(".nx-page-title__description");
  }

  /**
   * NxFieldset container for the monitoring stage radios (GROUP role, legend = "Monitoring Stage").
   * The fieldset itself is visible; the individual radio inputs inside it are CSS-hidden.
   */
  public Locator monitoringStageFieldset() {
    return byRole(AriaRole.GROUP, "Monitoring Stage");
  }

  /**
   * Visible label wrapper for the NxRadio identified by exact stage name text.
   * NxRadio hides the native input — click and visibility assertions must target the
   * label element ({@code label:has(input[name='monitor'])}), not the input.
   */
  public Locator radioLabelForStage(String stageName) {
    return locator("label:has(input[name='monitor'])").filter(
        new Locator.FilterOptions().setHasText(stageName));
  }

  /**
   * Visible label wrapper for the NxRadio matched by a pattern against the visible stage text.
   * Use to target the "Inherit from X" option when the parent-org name is variable
   * (e.g. {@code Pattern.compile("Inherit from.*")}).
   */
  public Locator radioLabelForStage(Pattern stageName) {
    return locator("label:has(input[name='monitor'])").filter(
        new Locator.FilterOptions().setHasText(stageName));
  }

  /**
   * All NxRadio label wrappers in the monitoring stage fieldset, in document order.
   * The list always contains one entry per CLI stage plus one "Do not monitor" / "Inherit from X"
   * option prepended by the UI (7 labels total for a standard IQ Server instance).
   */
  public Locator allMonitoringStageRadioLabels() {
    return locator("label:has(input[name='monitor'])");
  }

  /**
   * NxList.LinkItem (rendered as {@code <a>}) inside the Continuous Monitoring summary tile
   * on the Owner Summary page. Text is the active monitored stage name (e.g. "Build") or the
   * inherit/no-monitor label (e.g. "Do not monitor", "Inherit from Root Organization (Build)").
   * ID selector used because NxList renders as a bare {@code
   *
  <ul>
   * } with no ARIA role or accessible
   * name; {@code getByRole(LIST)} would match every list on the page. The ID scopes the link
   * query to this specific tile.
   */
  public Locator monitoringTileStageLink() {
    return locator("#continuous-monitoring").getByRole(AriaRole.LINK);
  }

  /**
   * Native radio input for the given stage name, used for isChecked assertions only.
   * The input is visually hidden but its checked state is readable by Playwright.
   */
  public Locator radioInputForStage(String stageName) {
    return byRole(AriaRole.RADIO, stageName);
  }

  /** "Update" submit button (NxStatefulForm submitBtnText="Update"). */
  public Locator updateButton() {
    return byRole(AriaRole.BUTTON, "Update");
  }

  /**
   * NxStatefulForm validation errors alert (role="alert" name="form validation errors").
   * Becomes visible after the user clicks Update with no changes.
   */
  public Locator formValidationErrors() {
    return byRole(AriaRole.ALERT, "form validation errors");
  }

  /**
   * Clicks Update and waits for the DELETE/PUT response (HTTP 200 or 204) from the
   * policy-monitoring API. Keeps the API URL out of the test body.
   */
  public void clickUpdateAndWaitForSave() {
    page.waitForResponse(
        response -> response.url().contains(POLICY_MONITORING_API)
            && (response.status() == 200 || response.status() == 204),
        () -> updateButton().click());
  }
}
