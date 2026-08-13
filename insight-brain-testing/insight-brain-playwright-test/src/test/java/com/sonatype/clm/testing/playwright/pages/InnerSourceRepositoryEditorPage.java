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

/**
 * Page object for the Inner Source Repository Configuration editor
 * ({@code /repositoryBaseConfigurations} under the org/app edit shell).
 * <p>
 * License-gated: tests must call {@code setFeatures(LicensedFeature.INNER_SOURCE_REPOSITORIES)}.
 * Conditional elements: "Inherit" radio hidden at root org; "Allow Override" hidden for apps;
 * locked-by-parent alert shown when {@code allowChange=false}.
 */
public class InnerSourceRepositoryEditorPage
    extends BasePage
{
  public static final String URL_FRAGMENT = "/repositoryBaseConfigurations";

  private static final String ADD_BUTTON_NOT_ENABLED_TOOLTIP_TEXT =
      "Must update to Enable to add a repository connection.";

  private static final String ADD_BUTTON_LOCKED_BY_PARENT_TOOLTIP_TEXT =
      "Parent organizations must Allow Override.";

  private static final Locator.GetByRoleOptions CREATE_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("Create").setExact(true);

  private static final Locator.GetByRoleOptions UPDATE_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("Update").setExact(true);

  private static final Locator.GetByRoleOptions OK_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("OK").setExact(true);

  private static final Locator.GetByRoleOptions CANCEL_BUTTON_OPTS =
      new Locator.GetByRoleOptions().setName("Cancel").setExact(true);

  public InnerSourceRepositoryEditorPage() {
    super();
  }

  public static String orgUrl(String orgId) {
    return OwnerSummaryPage.editOrganizationUrl(orgId, URL_FRAGMENT);
  }

  public static String appUrl(String appPublicId) {
    return OwnerSummaryPage.editApplicationUrl(appPublicId, URL_FRAGMENT);
  }

  public Locator container() {
    return page.getByRole(AriaRole.MAIN);
  }

  // scoped to avoid collisions with modal overlays
  public Locator form() {
    return locator("#innersource-repository-base-configurations-form");
  }

  // NxInfoAlert has no ARIA role; CSS class + text filter required
  public Locator lockedByParentAlert() {
    return form()
        .locator(".nx-alert--info")
        .filter(new Locator.FilterOptions().setHasText("cannot be overridden"));
  }

  public NxRadioComponent inheritRadio() {
    return new NxRadioComponent("Inherit", form());
  }

  public NxRadioComponent disableRadio() {
    return new NxRadioComponent("Disable", form());
  }

  public NxRadioComponent enableRadio() {
    // At non-root orgs the label is "Enable and Override Repository Connections"; Pattern matches both.
    return new NxRadioComponent(Pattern.compile("^\\s*Enable"), form());
  }

  public NxCheckboxComponent allowOverrideCheckbox() {
    return new NxCheckboxComponent("Allow Override", form());
  }

  public Locator addRepositoryButton() {
    return locator("#innersource-repository-base-configurations-add-button");
  }

  public Locator emptyRepositoryListMessage() {
    return form().getByText("No InnerSource repository connections are configured");
  }

  public Locator editButtonForRepository(int index) {
    return form()
        .locator("li.nx-list__item")
        .nth(index)
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Edit Repository Configuration"));
  }

  public Locator deleteButtonForRepository(int index) {
    return form()
        .locator("li.nx-list__item")
        .nth(index)
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Delete Repository Configuration"));
  }

  // scoped inside form() to avoid matching modal submit buttons
  public Locator submitButton() {
    return form().getByRole(AriaRole.BUTTON, UPDATE_BUTTON_OPTS);
  }

  public Locator configModal() {
    return locator("#innersource-repository-configuration-modal");
  }

  public Locator configModalHeading(String name) {
    return configModal().getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setName(name));
  }

  public Locator configModalCreateButton() {
    return configModal().getByRole(AriaRole.BUTTON, CREATE_BUTTON_OPTS);
  }

  public Locator configModalUpdateButton() {
    return configModal().getByRole(AriaRole.BUTTON, UPDATE_BUTTON_OPTS);
  }

  public Locator configModalFormatSelect() {
    return configModal().getByRole(AriaRole.COMBOBOX,
        new Locator.GetByRoleOptions().setName("Repository Format"));
  }

  // setExact(false) — NxFormGroup's sublabel text is inside <label>, making the full text longer
  public Locator configModalBaseUrlInput() {
    return configModal().getByLabel("Repository Base URL",
        new Locator.GetByLabelOptions().setExact(false));
  }

  public NxRadioComponent configModalAnonymousRadio() {
    return new NxRadioComponent("Allow Anonymous Access", configModal());
  }

  public NxRadioComponent configModalCredentialsRadio() {
    return new NxRadioComponent("Enter Username and Password", configModal());
  }

  public Locator configModalAuthFieldset() {
    return locator("#innersource-repository-configuration-modal-authentication");
  }

  // Scoped to auth fieldset — "Enter Username and Password" radio is outside this fieldset
  // and would ambiguously match "Username" at modal scope with setExact(false)
  public Locator configModalUsernameInput() {
    return configModalAuthFieldset()
        .getByLabel("Username", new Locator.GetByLabelOptions().setExact(false));
  }

  // Scoped to auth fieldset for same reason as configModalUsernameInput
  public Locator configModalPasswordInput() {
    return configModalAuthFieldset()
        .getByLabel("Password", new Locator.GetByLabelOptions().setExact(false));
  }

  public Locator configModalTestButton() {
    return configModal().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Test Configuration"));
  }

  // NxTooltip portals to body — shown when Enable radio selected but save not yet submitted
  public Locator addButtonNotEnabledTooltip() {
    return page.getByText(ADD_BUTTON_NOT_ENABLED_TOOLTIP_TEXT,
        new Page.GetByTextOptions().setExact(true));
  }

  // NxTooltip portals to body — shown when allowChange=false (parent locked override)
  public Locator addButtonLockedByParentTooltip() {
    return page.getByText(ADD_BUTTON_LOCKED_BY_PARENT_TOOLTIP_TEXT,
        new Page.GetByTextOptions().setExact(true));
  }

  public Locator deleteModal() {
    return locator("#innersource-repository-configuration-delete-modal");
  }

  // NxWarningAlert has no ARIA role; CSS class + text filter required
  public Locator deleteModalWarningAlert() {
    return deleteModal()
        .locator(".nx-alert--warning")
        .filter(new Locator.FilterOptions().setHasText("disable querying your configured repository"));
  }

  // scoped to deleteModal() to avoid matching other OK buttons
  public Locator deleteModalOkButton() {
    return deleteModal().getByRole(AriaRole.BUTTON, OK_BUTTON_OPTS);
  }

  // scoped to deleteModal() to avoid matching other Cancel buttons
  public Locator deleteModalCancelButton() {
    return deleteModal().getByRole(AriaRole.BUTTON, CANCEL_BUTTON_OPTS);
  }
}
