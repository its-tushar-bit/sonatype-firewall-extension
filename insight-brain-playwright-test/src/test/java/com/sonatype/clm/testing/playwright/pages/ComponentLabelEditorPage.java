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
 * Playwright page object for the Component Labels feature.
 * <p>
 * Covers two surfaces:
 * <ul>
 * <li><b>Labels tile</b> (owner summary) — entry point via "Add a Label" button and post-save
 * label-list verification.</li>
 * <li><b>Label editor</b> — create mode URL ends with {@code …/label} ("Create" submit button);
 * edit mode URL ends with {@code …/label/{labelId}} ("Update" submit button).</li>
 * </ul>
 */
public class ComponentLabelEditorPage
    extends BasePage
{
  /** URL fragment that identifies the create-label route. */
  public static final String CREATE_LABEL_URL_FRAGMENT = "/label";

  /** URL fragment that identifies the edit-label route ({@code …/label/{labelId}}). */
  public static final String EDIT_LABEL_URL_FRAGMENT = "/label/";

  public ComponentLabelEditorPage() {
    super();
  }

  /** Deep-link URL for the Create Component Label editor on an organization. */
  public static String createLabelUrl(String orgId) {
    return "/assets/index.html#/management/edit/organization/" + orgId + "/label";
  }

  /** Component Labels tile on the owner summary ({@code id="owner-pill-comp-labels"}). */
  public Locator componentLabelsTile() {
    return locator("#owner-pill-comp-labels");
  }

  /**
   * Clicks the "Add a Label" button ({@code id="add-label-button"}) in the Component Labels tile.
   * Dispatches the Redux {@code goToCreateLabel} action, navigating the SPA to {@code …/label}.
   * After calling this, wait for the URL to change before interacting with the editor.
   */
  public void clickAddLabelButton() {
    componentLabelsTile().locator("#add-label-button").click();
  }

  /**
   * Clicks the link for an existing label in the "Local to" section of the Component Labels tile.
   * {@code NxList.LinkItem} renders the label as an {@code <a>} element; clicking it navigates
   * the SPA to the edit-label route ({@code …/label/{labelId}}).
   * After calling this, wait for the URL to contain {@link #EDIT_LABEL_URL_FRAGMENT}.
   */
  public void clickLabelInTile(String labelName) {
    componentLabelsLocalListItem(labelName).getByRole(AriaRole.LINK).click();
  }

  /**
   * A list item in the "Local to {ownerName}" section of the Component Labels tile that contains
   * the given label name. Used to confirm a newly created label was saved successfully.
   */
  public Locator componentLabelsLocalListItem(String labelName) {
    return componentLabelsTile()
        .getByRole(AriaRole.LISTITEM)
        .filter(new Locator.FilterOptions().setHasText(labelName))
        .first();
  }

  /**
   * Delete modal ({@code NxModal id="label-config-delete-modal"}).
   * Only rendered when the delete confirmation dialog is open.
   */
  public Locator deleteModal() {
    return locator("#label-config-delete-modal");
  }

  /** Level-1 page heading: "Component Label Settings". */
  public Locator heading() {
    return locator("h1");
  }

  /**
   * Label Name text input — {@code NxFormGroup id="editor-label-name"}.
   * NxFormGroup wraps the input; scoped to the form group to avoid ambiguity if other
   * textboxes exist on the page.
   */
  public Locator labelNameInput() {
    return locator("#editor-label-name").getByRole(AriaRole.TEXTBOX);
  }

  /**
   * Description textarea — {@code NxFormGroup id="editor-label-description"}.
   * NxTextInput with {@code type="textarea"} renders a {@code <textarea>} with ARIA role textbox.
   */
  public Locator descriptionInput() {
    return locator("#editor-label-description").getByRole(AriaRole.TEXTBOX);
  }

  /**
   * A specific color swatch within the NxColorPicker ({@code id="editor-label-color-picker"}).
   * <p>
   * NxColorPicker renders each color as a {@code label} element with CSS class
   * {@code .nx-selectable-color--{rscColorName}}. CSS-class selection is used here because
   * the color swatches are visual-only labels with no reliable accessible name for role-based
   * locators (justified exception per LOCATOR_MIGRATION.md §3).
   * <p>
   * Valid RSC color names (keys of {@code rscToBackendColorMap} in {@code util.js}):
   * {@code turquoise}, {@code orange}, {@code yellow}, {@code kiwi}, {@code sky},
   * {@code blue}, {@code purple}, {@code pink}, {@code red}, {@code indigo}.
   */
  public Locator colorSwatch(String rscColorName) {
    return locator("#editor-label-color-picker .nx-selectable-color--" + rscColorName);
  }

  /**
   * Submit button of the main editor form, scoped to the {@code NxTile} container.
   * <p>
   * The delete confirmation modal ({@code NxModal}) also contains an {@code NxStatefulForm}
   * that renders a {@code .nx-form__submit-btn}. Scoping to {@code .nx-tile} ensures this
   * locator always resolves to exactly one element (the editor form's button) even while the
   * delete modal is simultaneously open on the page.
   */
  public Locator submitButton() {
    return locator(".nx-tile .nx-form__submit-btn");
  }

  /**
   * Delete button in the editor form footer ({@code id="delete-label-button"}).
   * Only present in edit mode when {@code custom-component-labels} feature is enabled
   * ({@code !isFeatureGated && labelId} condition in {@code CreateComponentLabel.jsx}).
   */
  public Locator deleteButton() {
    return locator("#delete-label-button");
  }

  /**
   * Submit button inside the delete confirmation modal.
   * The modal's {@code NxStatefulForm} uses {@code submitBtnText="Delete"}, which renders as
   * {@code .nx-form__submit-btn} scoped inside the modal.
   */
  public Locator confirmDeleteButton() {
    return deleteModal().locator(".nx-form__submit-btn");
  }

  public void typeLabelName(String name) {
    labelNameInput().fill(name);
  }

  public void typeDescription(String description) {
    descriptionInput().fill(description);
  }

  /**
   * Clicks the color swatch for the given RSC color name.
   * See {@link #colorSwatch(String)} for the list of valid RSC color names.
   */
  public void selectColor(String rscColorName) {
    colorSwatch(rscColorName).click();
  }

  public void submit() {
    submitButton().click();
  }

  /**
   * Clicks the Delete button in the editor form footer to open the delete confirmation modal.
   * Only available in edit mode with the {@code custom-component-labels} feature enabled.
   */
  public void clickDeleteButton() {
    deleteButton().click();
  }

  /**
   * Clicks the "Delete" submit button inside the delete confirmation modal.
   * After a successful delete, Redux dispatches {@code goToCreateLabel()}, navigating the SPA
   * to the create-label route ({@code …/label}) — wait for {@link #shouldBeInCreateMode()} before
   * proceeding (referenced via {@link ComponentLabelEditorPageAssertions}).
   */
  public void clickConfirmDelete() {
    confirmDeleteButton().click();
  }

  /**
   * Waits for the {@code NxSubmitMask} success overlay to appear then auto-dismiss.
   * {@code NxStatefulForm} sets {@code submitMaskState=true} on successful save; RSC shows the
   * checkmark overlay for ~800 ms ({@code SUCCESS_VISIBLE_TIME_MS}) then fires the timer action
   * to reset the state. This method confirms the full success lifecycle.
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
