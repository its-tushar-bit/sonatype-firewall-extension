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
import com.sonatype.insight.brain.model.Organization;

/**
 * Page object for the SBOM Manager Continuous Monitoring editor (SbomContinuousMonitoringEditor.jsx).
 *
 * <p>
 * Important DOM note: the title block ({@code #sbom-manager-continuous-monitoring}) contains only
 * the page heading and the Learn More button. The tile, form, toggle, and Update button are
 * rendered as siblings *outside* that block, so most locators here are page-scoped rather than
 * scoped through {@link #pageRoot()}.
 */
public class SbomContinuousMonitoringPage
    extends BasePage
{
  public static final String MONITORING_URL_FRAGMENT = "/monitoring";

  public static final String SBOM_ORG_EDIT_URL_PREFIX = "/sbomManager/management/edit/organization/";

  /**
   * Source URL the Learn More button dispatches via {@code window.open}; matches the value in
   * {@code SbomContinuousMonitoringEditor.jsx}.
   */
  public static final String LEARN_MORE_SOURCE_URL = "https://links.sonatype.com/products/sbom/docs/monitoring";

  private static final String PAGE_ROOT_ID = "sbom-manager-continuous-monitoring";

  /** Toggle's visible label flips between "Enabled" and "Disabled" depending on its checked state. */
  private static final Pattern TOGGLE_LABEL_TEXT = Pattern.compile("^(Enabled|Disabled)$");

  public SbomContinuousMonitoringPage() {
    super();
  }

  public static String url(Organization organization) {
    return url(organization.getId());
  }

  public static String url(String organizationId) {
    return "/assets/index.html#" + SBOM_ORG_EDIT_URL_PREFIX + organizationId + MONITORING_URL_FRAGMENT;
  }

  /** The page title block — heading and Learn-More button only. */
  public Locator pageRoot() {
    return locator("#" + PAGE_ROOT_ID);
  }

  public Locator pageHeading() {
    return pageRoot().getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(1));
  }

  public Locator learnMoreButton() {
    return pageRoot().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Learn more"));
  }

  /**
   * The NxToggle's underlying {@code <input role="switch">}. Use this for state assertions
   * ({@code isChecked}, {@code hasAccessibleName}). The input is CSS-hidden so it can't be
   * clicked directly — clicks go through {@link #toggleSwitchLabel()}.
   */
  public Locator toggleSwitch() {
    return page.getByRole(AriaRole.SWITCH);
  }

  /**
   * Clickable label for the toggle — the visible "Enabled" / "Disabled" text inside the
   * wrapping {@code <label>}. Clicking it forwards to the hidden switch input.
   */
  public Locator toggleSwitchLabel() {
    return page.getByText(TOGGLE_LABEL_TEXT);
  }

  public Locator submitButton() {
    return page.getByRole(AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName("Update"));
  }

  /**
   * NxStatefulForm does NOT disable Update on {@code isDirty=false}; it routes the click here.
   * NxErrorAlert renders as {@code role="alert"} with {@code aria-label="form validation errors"}.
   */
  public Locator noChangesValidationError() {
    return page.getByRole(AriaRole.ALERT,
        new Page.GetByRoleOptions().setName("form validation errors"));
  }
}
