/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

/**
 * Playwright page object for the License Threat Group feature.
 * <p>
 * Covers two surfaces:
 * <ul>
 * <li><b>LTG summary tile</b> (owner summary) — entry point via "Add a Threat Group" button and
 * post-save group-list verification. Tile id: {@code owner-pill-ltgs}.</li>
 * <li><b>LTG editor</b> — create mode URL ends with {@code …/licenseThreatGroup} ("Create" submit
 * button); edit mode URL ends with {@code …/licenseThreatGroup/{ltgId}} ("Update" button).</li>
 * </ul>
 */
public class LicenseThreatGroupEditorPage
    extends BasePage
{
  /** URL fragment that identifies the create-LTG route. */
  public static final String CREATE_LTG_URL_FRAGMENT = "/licenseThreatGroup";

  /** URL fragment that identifies the edit-LTG route ({@code …/licenseThreatGroup/{ltgId}}). */
  public static final String EDIT_LTG_URL_FRAGMENT = "/licenseThreatGroup/";

  public LicenseThreatGroupEditorPage() {
    super();
  }

  /** License Threat Groups summary tile on the owner summary ({@code id="owner-pill-ltgs"}). */
  public Locator ltgTile() {
    return locator("#owner-pill-ltgs");
  }

  /**
   * Clicks the table row for the given group in the LTG tile, navigating to the edit-LTG route.
   * <p>
   * {@code ApplicableLicenseThreatGroupTable.jsx} renders each row with {@code isClickable} and
   * {@code clickAccessibleLabel={"Edit " + name + " License Threat Group"}}. RSC's
   * {@code NxTable.Row} with {@code isClickable} renders a hidden button carrying that label.
   * After calling this, wait for the URL to contain {@link #EDIT_LTG_URL_FRAGMENT}.
   */
  public void clickLtgRowInTile(String groupName) {
    ltgTile()
        .getByRole(AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName("Edit " + groupName + " License Threat Group"))
        .click();
  }

  /**
   * Clicks the "Add a Threat Group" button ({@code id="add-ltg-button"}) in the LTG tile.
   * Dispatches the Redux {@code goToCreateLTG} action, navigating the SPA to
   * {@code …/licenseThreatGroup}. After calling this, wait for the URL to change before
   * interacting with the editor.
   */
  public void clickAddThreatGroupButton() {
    ltgTile().locator("#add-ltg-button").click();
  }

  /**
   * A cell in the local LTG table that contains the given group name.
   * {@code ApplicableLicenseThreatGroupTable} renders group names in NxTable.Cell elements
   * inside the {@code .iq-ltg-table-local-section} tbody.
   */
  public Locator ltgCellInTile(String groupName) {
    return ltgTile()
        .locator(".iq-ltg-table-local-section")
        .getByRole(AriaRole.CELL)
        .filter(new Locator.FilterOptions().setHasText(groupName))
        .first();
  }

  /** Level-1 page heading: "License Threat Group Settings". */
  public Locator heading() {
    return locator("h1");
  }

  /**
   * Group Name text input — {@code NxFormGroup id="editor-label-name"}.
   * NxFormGroup wraps the input; scoped to the form group to avoid ambiguity.
   */
  public Locator groupNameInput() {
    return locator("#editor-label-name").getByRole(AriaRole.TEXTBOX);
  }

  /**
   * Toggle button for the {@code ThreatDropdownSelector}
   * ({@code .iq-threat-dropdown-selector .nx-dropdown__toggle}).
   * Click this to open the threat level dropdown.
   */
  public Locator threatDropdownToggle() {
    return locator(".iq-threat-dropdown-selector .nx-dropdown__toggle");
  }

  /**
   * A specific option inside the open threat level dropdown.
   * Options are {@code .nx-dropdown-button} elements with text {@code "{level} - {categoryName}"}.
   */
  public Locator threatDropdownOption(int level) {
    return locator(".iq-threat-dropdown-selector .nx-dropdown-button")
        .filter(new Locator.FilterOptions().setHasText(level + " -"));
  }

  /**
   * The available-licenses side of the transfer list
   * ({@code NxTransferList id="editor-ltg-included-licenses"}).
   * Available items are in the first {@code .nx-transfer-list__half}.
   */
  public Locator availableLicensesSide() {
    return locator("#editor-ltg-included-licenses .nx-transfer-list__half").first();
  }

  /** Filter text input on the available licenses side of the transfer list. */
  public Locator availableLicensesFilter() {
    return availableLicensesSide().getByRole(AriaRole.TEXTBOX);
  }

  /** First license item ({@code .nx-transfer-list__item}) in the available licenses side. */
  public Locator firstAvailableLicenseItem() {
    return availableLicensesSide().locator(".nx-transfer-list__item").first();
  }

  /**
   * Submit button of the LTG editor form, scoped to {@code #license-threat-group-editor}.
   * The delete confirmation modal also contains a {@code .nx-form__submit-btn}; scoping to
   * the editor tile ID ensures this locator always resolves to exactly one element.
   */
  public Locator submitButton() {
    return locator("#license-threat-group-editor .nx-form__submit-btn");
  }

  /**
   * Delete button in the editor form footer ({@code id="delete-ltg-button"}).
   * Only present in edit mode ({@code ltgId} is set in the Redux state).
   */
  public Locator deleteButton() {
    return locator("#delete-ltg-button");
  }

  /**
   * Delete confirmation modal ({@code NxModal id="ltg-config-delete-modal"}).
   * Only rendered when {@code isDeleteModalOpen} state is true.
   */
  public Locator deleteModal() {
    return locator("#ltg-config-delete-modal");
  }

  /**
   * Submit button inside the delete confirmation modal.
   * The modal's {@code NxStatefulForm} uses {@code submitBtnText="Delete"}, which renders as
   * {@code .nx-form__submit-btn} scoped inside the modal.
   */
  public Locator confirmDeleteButton() {
    return deleteModal().locator(".nx-form__submit-btn");
  }

  public void typeGroupName(String name) {
    groupNameInput().fill(name);
  }

  /**
   * Opens the threat level dropdown and clicks the option for the given level.
   * Level must be 0–10. Option text is "{level} - {categoryName}".
   */
  public void selectThreatLevel(int level) {
    threatDropdownToggle().click();
    threatDropdownOption(level).click();
  }

  /**
   * Types the given text into the available licenses filter, then clicks the first matching
   * license item to move it to the selected (Included Licenses) side.
   */
  public void addFirstLicenseMatchingFilter(String filterText) {
    availableLicensesFilter().fill(filterText);
    firstAvailableLicenseItem().click();
  }

  public void submit() {
    submitButton().click();
  }

  /**
   * Clicks the Delete button in the editor form footer to open the delete confirmation modal.
   * Only available in edit mode.
   */
  public void clickDeleteButton() {
    deleteButton().click();
  }

  /**
   * Clicks the "Delete" submit button inside the delete confirmation modal.
   * After a successful delete, Redux dispatches {@code goToCreateLTG()}, navigating the SPA
   * to the create-LTG route ({@code …/licenseThreatGroup}).
   */
  public void clickConfirmDelete() {
    confirmDeleteButton().click();
  }

  /**
   * Waits for the {@code NxSubmitMask} success overlay to appear then auto-dismiss.
   */
  public void waitForSaveSuccess() {
    Locator successMask = page.locator(".nx-submit-mask--success");
    successMask.waitFor(new Locator.WaitForOptions()
        .setState(WaitForSelectorState.VISIBLE)
        .setTimeout(PlaywrightTiming.BRIEF_UI_TRANSITION_MS));
    successMask.waitFor(new Locator.WaitForOptions()
        .setState(WaitForSelectorState.HIDDEN)
        .setTimeout(PlaywrightTiming.BRIEF_UI_TRANSITION_MS));
  }
}
