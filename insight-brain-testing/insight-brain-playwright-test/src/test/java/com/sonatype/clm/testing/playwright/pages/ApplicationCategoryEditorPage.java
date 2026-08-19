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
 * Playwright page object for the Application Category feature.
 * <p>
 * Covers two surfaces:
 * <ul>
 * <li><b>Categories tile</b> (owner summary) — entry point via "Add a Category" button and
 * post-save category-list verification. Tile id: {@code owner-pill-app-categories}.</li>
 * <li><b>Category editor</b> — create mode URL ends with {@code …/category} ("Create" submit
 * button); edit mode URL ends with {@code …/category/{categoryId}} ("Update" button).</li>
 * </ul>
 */
public class ApplicationCategoryEditorPage
    extends BasePage
    implements TierGatedEditorPage
{
  /** URL fragment that identifies the create-category route. */
  public static final String CREATE_CATEGORY_URL_FRAGMENT = "/category";

  /** URL fragment that identifies the edit-category route ({@code …/category/{categoryId}}). */
  public static final String EDIT_CATEGORY_URL_FRAGMENT = "/category/";

  public ApplicationCategoryEditorPage() {
    super();
  }

  /** Application Categories tile on the owner summary ({@code id="owner-pill-app-categories"}). */
  public Locator categoriesTile() {
    return locator("#owner-pill-app-categories");
  }

  /**
   * Clicks the link for an existing category in the "Local to" section of the tile.
   * {@code ApplicationCategoriesTile.jsx} renders org-local categories as {@code NxList.LinkItem}
   * elements containing the category name. Clicking navigates the SPA to the edit-category route
   * ({@code …/category/{categoryId}}).
   * After calling this, wait for the URL to contain {@link #EDIT_CATEGORY_URL_FRAGMENT}.
   */
  public void clickCategoryInTile(String categoryName) {
    categoryLocalListItem(categoryName).getByRole(AriaRole.LINK).click();
  }

  /**
   * Tile action button. Three rendered texts: "Add a Category" (org view, Enterprise),
   * "Preview Add a Category" (org view, Pro tier), "Assign a Category" (application view).
   * Each state is a literal accessible-name match — no regex needed.
   */
  public Locator addCategoryButton() {
    Locator enterprise = categoriesTile().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Add a Category").setExact(true));
    Locator pro = categoriesTile().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Preview Add a Category").setExact(true));
    Locator assign = categoriesTile().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Assign a Category").setExact(true));
    return enterprise.or(pro).or(assign);
  }

  /**
   * Clicks the "Add a Category" button ({@code id="add-category-button"}) in the tile.
   * Dispatches the Redux {@code goToCreateCategory} action, navigating the SPA to
   * {@code …/category}. After calling this, wait for the URL to change before interacting
   * with the editor.
   */
  public void clickAddCategoryButton() {
    addCategoryButton().click();
  }

  public Locator readOnlyView() {
    return page.getByTestId("application-category-readonly-view");
  }

  public Locator readOnlyCategoryName() {
    return readOnlyView().getByTestId("category-name");
  }

  // --- TierGatedEditorPage ---

  @Override
  public Locator addEntityButton() {
    return addCategoryButton();
  }

  @Override
  public Locator readOnlyEntityView() {
    return readOnlyView();
  }

  @Override
  public Locator readOnlyEntityName() {
    return readOnlyCategoryName();
  }

  /**
   * A list item in the "Local to {ownerName}" section of the Application Categories tile that
   * contains the given category name. Used to confirm a newly created category was saved.
   * <p>
   * {@code ApplicationCategoriesTile.jsx} renders org-local categories as NxList.LinkItem
   * elements containing the category name text.
   */
  public Locator categoryLocalListItem(String categoryName) {
    return categoriesTile()
        .getByRole(AriaRole.LISTITEM)
        .filter(new Locator.FilterOptions().setHasText(categoryName))
        .first();
  }

  /** Level-1 page heading: "Application Category Settings". */
  public Locator heading() {
    return locator("h1");
  }

  /**
   * Category Name text input — {@code NxFormGroup id="editor-category-name"}.
   * NxFormGroup wraps the input; scoped to avoid ambiguity.
   */
  public Locator nameInput() {
    return locator("#editor-category-name").getByRole(AriaRole.TEXTBOX);
  }

  /**
   * Brief Description textarea — {@code NxFormGroup id="editor-category-description"}.
   * {@code NxTextInput type="textarea"} renders a {@code <textarea>} with ARIA role textbox.
   */
  public Locator descriptionInput() {
    return locator("#editor-category-description").getByRole(AriaRole.TEXTBOX);
  }

  /**
   * A specific color swatch within the NxColorPicker ({@code id="editor-category-color-picker"}).
   * <p>
   * Valid RSC color names: {@code turquoise}, {@code orange}, {@code yellow}, {@code kiwi},
   * {@code sky}, {@code blue}, {@code purple}, {@code pink}, {@code red}, {@code indigo}.
   */
  public Locator colorSwatch(String rscColorName) {
    return locator("#editor-category-color-picker .nx-selectable-color--" + rscColorName);
  }

  /**
   * Submit button of the category editor form, scoped to {@code #create-edit-category}.
   * The delete confirmation modal also contains a {@code .nx-form__submit-btn}; scoping to
   * the form ID ensures this locator always resolves to exactly one element.
   */
  @Override
  public Locator submitButton() {
    return locator("#create-edit-category .nx-form__submit-btn");
  }

  /**
   * Delete button in the editor form footer ({@code id="delete-category-button"}).
   * Only present in edit mode when {@code custom-application-categories} feature is enabled
   * ({@code categoryId && !isFeatureGated} condition in {@code CreateEditApplicationCategory.jsx}).
   */
  @Override
  public Locator deleteButton() {
    return locator("#delete-category-button");
  }

  /**
   * Delete confirmation modal ({@code NxModal id="category-delete-modal"}).
   * Only rendered when {@code isDeleteModalOpen} state is true.
   */
  public Locator deleteModal() {
    return locator("#category-delete-modal");
  }

  /**
   * Submit button inside the delete confirmation modal.
   * The modal's {@code NxStatefulForm} uses {@code submitBtnText="Continue"} (not "Delete"),
   * scoped inside the modal to avoid ambiguity with the editor form's submit button.
   */
  public Locator confirmDeleteButton() {
    return deleteModal().locator(".nx-form__submit-btn");
  }

  public void typeCategoryName(String name) {
    nameInput().fill(name);
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
   * Only available in edit mode with the {@code custom-application-categories} feature enabled.
   */
  public void clickDeleteButton() {
    deleteButton().click();
  }

  /**
   * Clicks the "Continue" submit button inside the delete confirmation modal.
   * After a successful delete, Redux dispatches {@code goToCreateCategory()}, navigating
   * the SPA to the create-category route ({@code …/category}).
   */
  public void clickConfirmDelete() {
    confirmDeleteButton().click();
  }

  /**
   * Waits for the {@code NxSubmitMask} success overlay to appear then auto-dismiss.
   * After a successful CREATE, the SPA stays on the create-category page (no navigation).
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
