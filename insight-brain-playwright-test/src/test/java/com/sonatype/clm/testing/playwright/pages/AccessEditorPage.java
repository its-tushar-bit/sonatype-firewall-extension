/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.SelectOption;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * Playwright page object for owner/application Access editor (Add/Edit Role flow).
 * <p>
 * New mode (URL ends {@code …/access}) renders "New Role" heading + role dropdown + "Create" button.
 * Edit mode (URL ends {@code …/access/{roleId}}) renders "Edit Role" heading + role-name subtitle +
 * no dropdown + "Update" button.
 */
public class AccessEditorPage
    extends BasePage
{
  private static final String ROOT = "#create-edit-access-page";

  private static final String FORM = "#access-add-members-form";

  private static final String ORG_EDIT_URL_BASE = "/assets/index.html#/management/edit/organization/";

  /** URL fragment shared by both add and edit access sub-routes. */
  public static final String ADD_ACCESS_URL_FRAGMENT = "/access";

  public AccessEditorPage() {
    super();
  }

  /** Deep-link URL for the "New Role" access editor on an organization. */
  public static String newAccessUrl(String orgId) {
    return ORG_EDIT_URL_BASE + orgId + ADD_ACCESS_URL_FRAGMENT;
  }

  /** Deep-link URL for the "Edit Role" access editor for a specific role on an organization. */
  public static String editAccessUrl(String orgId, String roleId) {
    return newAccessUrl(orgId) + "/" + roleId;
  }

  public Locator root() {
    return locator(ROOT);
  }

  public Locator form() {
    return locator(FORM);
  }

  /** Level-1 heading rendered by {@code NxH1} inside {@code NxPageTitle}. */
  public Locator heading() {
    return locator(ROOT).getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(1));
  }

  /** Role-name subtitle rendered by {@code NxPageTitle.Subtitle} (h2) in edit mode. */
  public Locator headingSubtitle() {
    return locator(ROOT).getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(2));
  }

  public Locator roleSelect() {
    return locator(FORM + " select");
  }

  /**
   * The {@code NxSearchDropdown} text input. RSC renders it with {@code role="searchbox"} and no
   * explicit {@code aria-label}; {@code AriaRole.SEARCHBOX} scoped to the form is the only stable
   * semantic anchor.
   */
  public Locator searchInput() {
    return locator(FORM).getByRole(AriaRole.SEARCHBOX);
  }

  /**
   * All result items in the {@code NxSearchDropdown} results menu. RSC renders each item as
   * {@code <button role="menuitem">} so {@code AriaRole.MENUITEM} is the correct semantic selector.
   * Callers filter by visible text before clicking.
   */
  public Locator searchResults() {
    return locator(FORM).getByRole(AriaRole.MENUITEM);
  }

  public Locator associatedMembers() {
    return locator(FORM + " .nx-transfer-list__item");
  }

  // ---------------------------------------------------------------------------------------------
  // "Add an External Group" controls — rendered by AccessPage.jsx when group search is disabled
  // (e.g. SAML configured in MTIQ). See {@code #associate-group-form-group}.
  // ---------------------------------------------------------------------------------------------

  public Locator addGroupFormGroup() {
    return locator("#associate-group-form-group");
  }

  public Locator addGroupInput() {
    return locator("#add-associate-group-input");
  }

  public Locator addGroupButton() {
    return locator("#add-associate-group-btn");
  }

  public Locator addGroupSublabel() {
    return addGroupFormGroup().locator(".nx-sub-label");
  }

  /**
   * LDAP group-search-disabled info alert ({@code #ldap-servers-alert}). Only rendered for on-prem
   * ({@code !isMultiTenant}); absent in MTIQ.
   */
  public Locator ldapServersAlert() {
    return locator("#ldap-servers-alert");
  }

  public void addExternalGroup(String groupName) {
    addGroupInput().fill(groupName);
    addGroupButton().click();
  }

  /**
   * The associated-members {@code NxTransferListHalf} container ({@code .nx-transfer-list__half}).
   * {@code AccessPage} renders {@code NxTransferListHalf} directly — not wrapped in
   * {@code NxTransferList} — so there is no {@code .nx-transfer-list} root element on this page.
   * The footer text ("{N} User(s) and {M} Group(s) Added") lives in
   * {@code .nx-transfer-list__footer} inside this fieldset.
   */
  public Locator associatedTransferList() {
    return locator(FORM + " .nx-transfer-list__half");
  }

  public Locator submitError() {
    return locator(FORM + " .nx-form__submit-error");
  }

  /**
   * RSC NxStatefulForm validation-error alert — visible only after a submit attempt when
   * the form has errors ({@code nx-form--show-validation-errors nx-form--has-validation-errors}).
   */
  public Locator validationErrors() {
    return locator(FORM).getByRole(AriaRole.ALERT,
        new Locator.GetByRoleOptions().setName("form validation errors"));
  }

  /**
   * The primary submit button. Matches "Create" (new mode) or "Update" (edit mode) via
   * {@code .or()}, since {@code NxStatefulForm} renders different button text per mode.
   * Scoped to the form root container because {@code NxStatefulForm} renders its footer outside
   * the {@code <form>} element.
   */
  public Locator submitButton() {
    Locator create = locator(FORM).getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Create"));
    Locator update = locator(FORM).getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Update"));
    return create.or(update);
  }

  /** Tertiary "Delete" button rendered in edit mode only. */
  public Locator deleteButton() {
    return locator(ROOT).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Delete"));
  }

  /**
   * {@code DeleteAccessModal} dialog. Accessible name {@code "Delete Role"} is resolved from
   * {@code aria-labelledby} pointing to the {@code NxH2} heading inside {@code NxModal.Header}.
   */
  public Locator deleteModal() {
    return byRole(AriaRole.DIALOG, "Delete Role");
  }

  /** "Continue" submit button inside the {@code DeleteAccessModal}. */
  public Locator deleteModalContinueButton() {
    return deleteModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Continue"));
  }

  /** "Cancel" button inside the {@code DeleteAccessModal}. */
  public Locator deleteModalCancelButton() {
    return deleteModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel"));
  }

  public void clickDelete() {
    deleteButton().click();
  }

  public void confirmDeleteModal() {
    deleteModalContinueButton().click();
    deleteModal().waitFor(new Locator.WaitForOptions()
        .setState(WaitForSelectorState.HIDDEN)
        .setTimeout(PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS));
  }

  public void cancelDeleteModal() {
    deleteModalCancelButton().click();
  }

  public void selectRole(String roleName) {
    roleSelect().selectOption(new SelectOption().setLabel(roleName));
  }

  /**
   * Clicks the remove control on the "Associated Members" item matching {@code memberName},
   * moving the member back to the available side.
   * <p>
   * RSC {@code NxTransferListHalf} renders each item as {@code <div role="group">} containing a
   * {@code <label class="nx-transfer-list__select">} (not a {@code <button>}). The label has no
   * accessible role, so {@code getByRole} cannot target it — the CSS class is the only stable anchor.
   */
  public void removeAssociatedMember(String memberName) {
    associatedMembers()
        .filter(new Locator.FilterOptions().setHasText(memberName))
        .first()
        .locator(".nx-transfer-list__select")
        .click();
  }

  public void searchFor(String query) {
    searchInput().fill(query);
    searchInput().click();
  }

  public void searchAndSelectUser(String query) {
    searchInput().fill(query);
    searchInput().click();
    searchResults().filter(new Locator.FilterOptions().setHasText(query)).first().click();
  }

  public void submit() {
    submitButton().click();
  }
}
