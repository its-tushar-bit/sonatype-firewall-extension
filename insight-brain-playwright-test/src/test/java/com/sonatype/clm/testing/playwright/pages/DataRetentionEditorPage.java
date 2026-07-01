/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/** Page object for the org-level Data Retention editor. */
public class DataRetentionEditorPage
    extends BasePage
{
  static final String DATA_RETENTION_URL_SUFFIX = "/data-retention";

  private static final String ROOT = "#retention-editor";

  public DataRetentionEditorPage() {
    super();
  }

  public static String url(String orgId) {
    return "/assets/index.html#/management/edit/organization/" + orgId + DATA_RETENTION_URL_SUFFIX;
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator pageHeading() {
    return page.getByRole(AriaRole.HEADING,
        new Page.GetByRoleOptions().setName("Data Retention").setExact(true));
  }

  public Locator updateButton() {
    return locator(ROOT).getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Update"));
  }

  /** NxFieldset for the build stage — {@code <fieldset role=group>} named by its legend. */
  private Locator buildStageFieldset() {
    return locator(ROOT).getByRole(AriaRole.GROUP,
        new Locator.GetByRoleOptions().setName("Build"));
  }

  /** "Custom" radio in the build-stage fieldset. */
  public Locator customRadioForBuildStage() {
    return buildStageFieldset()
        .getByRole(AriaRole.RADIO, new Locator.GetByRoleOptions().setName("Custom"));
  }

  /**
   * NxRadio hides the native {@code <input>} (position:absolute, 1 px) so Playwright cannot
   * pointer-click it. {@code el.click()} dispatches the DOM click directly, which fires the
   * native change event React's onChange picks up.
   */
  public void clickCustomRadioForBuildStage() {
    customRadioForBuildStage().evaluate("el => el.click()");
  }

  /** The build fieldset has both an "Age" and a "No." (count) textbox; placeholder disambiguates. */
  public Locator buildStageAgeInput() {
    return buildStageFieldset().getByPlaceholder("Age");
  }

  /** Field-level validation alert rendered by NxTextInput when the age value is invalid. */
  public Locator buildStageAgeValidationError() {
    return buildStageFieldset().getByRole(AriaRole.ALERT);
  }

  /** Unit dropdown (NxFormSelect → native {@code <select>}, role=combobox) for the build-stage age modifier. */
  public Locator buildStageAgeUnitSelect() {
    return buildStageFieldset().getByRole(AriaRole.COMBOBOX);
  }
}
