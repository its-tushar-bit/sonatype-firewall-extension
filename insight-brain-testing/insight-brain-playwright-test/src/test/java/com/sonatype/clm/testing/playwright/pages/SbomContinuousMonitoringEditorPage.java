/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/** Page object for the SBOM Continuous Monitoring editor. */
public class SbomContinuousMonitoringEditorPage
    extends BasePage
{
  // NxToggle accessible-name flips between "Enabled"/"Disabled"; the input id and the label's
  // className are stable JSX attributes — anchor on those instead of the dynamic ARIA name.
  private static final String TOGGLE_INPUT_ID = "#enable-continuous-monitoring";

  private static final String TOGGLE_LABEL_CLASS = "label.sbom-enable-continuous-monitoring";

  public SbomContinuousMonitoringEditorPage() {
    super();
  }

  public static String orgUrl(String ownerId) {
    return "/assets/index.html#/sbomManager/management/edit/organization/" + ownerId + "/monitoring";
  }

  public Locator container() {
    return locator("#sbom-manager-continuous-monitoring");
  }

  public Locator title() {
    return page.getByRole(AriaRole.HEADING,
        new Page.GetByRoleOptions().setName("Continuous Monitoring").setLevel(1));
  }

  /** Enable/Disable continuous-monitoring toggle — see {@link NxToggle}. */
  public NxToggle enableToggle() {
    return new NxToggle(locator(TOGGLE_INPUT_ID), locator(TOGGLE_LABEL_CLASS));
  }

  public Locator updateButton() {
    return byRole(AriaRole.BUTTON, "Update");
  }

  public Locator learnMoreButton() {
    return byRole(AriaRole.BUTTON, "Learn more");
  }

  /**
   * NxStatefulForm gates its role=alert "form validation errors" banner behind a submit attempt
   * (CSS-hides the alert until then), but the parent {@code <form>} carries
   * {@code nx-form--has-validation-errors} whenever {@code validationErrors} is set on the
   * component. Used here to assert the pristine "no changes to save" state — narrowed to the
   * form that contains the page's toggle, since the form lives in a sibling NxTile outside the
   * page header's container. {@code hasCount(1)} means pristine, {@code hasCount(0)} means dirty.
   */
  public Locator formWithNoChangesValidationErrorClass() {
    return page.locator("form.nx-form.nx-form--has-validation-errors")
        .filter(new Locator.FilterOptions().setHas(enableToggle().label()));
  }
}
