/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;

/**
 * Playwright page object for the Policy Editor page.
 */
public class PolicyEditorPage
    extends BasePage
{
  private static final String ROOT = "#policy-editor-summary";

  public static final String EDIT_URL_FRAGMENT = "/management/edit/";

  public static final String SBOM_MANAGER_EDIT_URL_FRAGMENT = "/sbomManager/management/edit/";

  public PolicyEditorPage() {
    super();
  }

  public static String newPolicyUrl(Owner owner) {
    boolean isOrgOrApp = owner.getType().equals(OwnerType.ORGANIZATION) ||
        owner.getType().equals(OwnerType.APPLICATION);
    String ownerId = isOrgOrApp ? owner.getPublicId() : owner.getId();
    return "/assets/index.html#/management/edit/" + owner.getType().name().toLowerCase() + "/" + ownerId + "/policy";
  }

  public static String url(Owner owner, Policy policy) {
    return newPolicyUrl(owner) + "/" + policy.getId();
  }

  /**
   * URL for the policy editor under the SBOM Manager route prefix
   * ({@code sbomManager.management.edit.<ownerType>.policy}). Navigating here sets
   * {@code selectIsSbomManager = true}, which suppresses the Delete button and the
   * Actions section, and renders the "Switch to Lifecycle" NxInfoAlert instead.
   */
  public static String sbomManagerUrl(Owner owner, Policy policy) {
    boolean isOrgOrApp = owner.getType().equals(OwnerType.ORGANIZATION) ||
        owner.getType().equals(OwnerType.APPLICATION);
    String ownerId = isOrgOrApp ? owner.getPublicId() : owner.getId();
    return "/assets/index.html#/sbomManager/management/edit/" +
        owner.getType().name().toLowerCase() + "/" + ownerId + "/policy/" + policy.getId();
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator policyName() {
    return page.locator("#policy-edit-summary").getByLabel("Policy Name");
  }

  public Locator threatLevelDropdown() {
    return locator("#editor-policy-threat-level .nx-btn");
  }

  public Locator policyTitle() {
    return locator("#policy-editor-summary h1");
  }

  public Locator footer() {
    return locator(".nx-footer");
  }

  public Locator saveButton() {
    return locator(".nx-form__submit-btn");
  }

  public Locator deletePolicyButton() {
    return locator("#delete-policy-button");
  }

  public Locator actionsDisabledMessage() {
    return locator("#actions-disabled-message");
  }

  public Locator notificationsDisabledMessage() {
    return locator("#notifications-disabled-message");
  }

  public Locator legacyViolationDisabledMessage() {
    return locator("#legacy-violation-disabled-message");
  }

  public Locator legacyViolationCheckbox() {
    return locator("#editor-legacy-violation-checkbox");
  }

  public Locator alertContent() {
    return locator(".nx-alert__content");
  }

  public Locator lifecycleLink() {
    return locator(".policy-editor-lifecycle-link");
  }

  /** The delete-policy modal container. NxModal portals to body level, not inside ROOT. */
  public Locator deleteModal() {
    return locator("#policy-delete-modal");
  }

  /** Bulleted consequences list inside the delete modal (NxList bulleted). */
  public Locator deleteModalConsequencesList() {
    return locator("#policy-delete-modal .nx-list--bulleted");
  }

  public Locator deleteModalInput() {
    return locator("#policy-delete-modal input[type='text']");
  }

  public Locator deleteModalValidation() {
    return locator("#policy-delete-modal .nx-field-validation-message");
  }

  public Locator deleteModalErrors() {
    return locator("#policy-delete-modal .nx-form__validation-errors");
  }

  /** "Confirm Deletion" submit button inside the delete modal. */
  public Locator deleteModalConfirmButton() {
    return locator("#policy-delete-modal .nx-form__submit-btn");
  }

  /** Cancel button inside the delete modal. */
  public Locator deleteModalCancelButton() {
    return locator("#policy-delete-modal .nx-form__cancel-btn");
  }

  /**
   * The inline NxInfoAlert rendered by PolicyEditor when {@code isSbomManager = true}
   * (i.e. when the page is accessed via the {@code /sbomManager/...} route prefix).
   * Contains a link to manage policies in Lifecycle or Repository Firewall.
   * Scoped inside {@link #ROOT} ({@code #policy-editor-summary}).
   */
  public Locator sbomManagerInfoAlert() {
    return container().locator(".nx-alert--info");
  }

  /**
   * "Default" mode button in the enterprise feature-gate toggle (rendered when
   * {@code hasCustomPolicies} is false and the editor is open for an existing policy).
   */
  public Locator defaultModeButton() {
    return container().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Default"));
  }

  /**
   * "Custom" mode button in the enterprise feature-gate toggle. Wrapped in an NxTooltip with
   * title "Enterprise Feature" and contains a lock icon.
   */
  public Locator customModeButton() {
    return container().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Custom"));
  }

  /**
   * The FontAwesome lock icon ({@code data-icon="lock"}) rendered inside the Custom mode button.
   * Asserts the enterprise feature gate is visually indicated on the button.
   */
  public Locator customModeButtonLockIcon() {
    return customModeButton().locator("[data-icon='lock']");
  }

  /**
   * The "Enterprise Feature" NxTooltip content that appears when hovering the Custom mode button.
   * NxTooltip portals to body level, so this is scoped to the page rather than the container.
   */
  public Locator enterpriseFeatureTooltip() {
    return page.getByText("Enterprise Feature", new Page.GetByTextOptions().setExact(true));
  }

  /**
   * The NxInfoAlert rendered inside the policy form when the editor is in enterprise preview
   * mode (i.e. {@code isFeatureGated=true && isEnterprisePreviewMode=true} or a new-policy form
   * without the custom-policies entitlement). Text: "This is an Enterprise feature. Changes
   * can't be saved."
   */
  public Locator enterprisePreviewAlert() {
    return container().locator(".nx-alert--info")
        .filter(new Locator.FilterOptions().setHasText("Enterprise feature"));
  }

  /**
   * "Back" button rendered by PolicyEditor on the new-policy form.
   * Calls {@code window.history.back()}, returning the browser to the previous page without
   * submitting.
   */
  public Locator backButton() {
    return container().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Back"));
  }

  /**
   * "Override parent actions" radio in the Override Status fieldset, rendered when the inherited
   * policy has {@code policyActionsOverrideAllowed=true}. Selecting this enables the actions
   * table and dispatches an override save (not a full policy save) on submit.
   */
  public Locator overrideParentActionsRadio() {
    return actionsSection().getByRole(AriaRole.RADIO,
        new Locator.GetByRoleOptions().setName("Override parent actions"));
  }

  /**
   * First radio button in the "Warn" row of the edit-policy-actions table.
   * Used to set the first pipeline stage's action to Warn when testing action overrides.
   */
  public Locator actionsTableWarnRowFirstRadio() {
    return actionsSection().locator("#edit-policy-actions-table tr")
        .filter(new Locator.FilterOptions().setHasText("Warn"))
        .getByRole(AriaRole.RADIO)
        .first();
  }

  /**
   * Clicks the "Override parent actions" radio.
   * NxRadio hides the native {@code <input>} (position:absolute, 1 px) so Playwright cannot
   * pointer-click it even with {@code force:true}. {@code el.click()} dispatches the DOM click
   * directly, which fires the native change event that React's synthetic onChange picks up.
   */
  public void clickOverrideParentActionsRadio() {
    overrideParentActionsRadio().evaluate("el => el.click()");
  }

  /**
   * Clicks the first radio in the "Warn" row of the actions table.
   * Same hidden-input constraint as {@link #clickOverrideParentActionsRadio()}.
   */
  public void clickActionsTableWarnRowFirstRadio() {
    actionsTableWarnRowFirstRadio().evaluate("el => el.click()");
  }

  /**
   * Inheritance section of the policy editor. Always rendered for organization-owned policies
   * (see EditPolicyInheritance.jsx, root element id="policy-edit-inheritance").
   */
  public Locator inheritanceSection() {
    return locator("#policy-edit-inheritance");
  }

  /** Constraints section root (see ConstraintsEditor.jsx, id="policy-edit-constraints"). */
  public Locator constraintsSection() {
    return locator("#policy-edit-constraints");
  }

  /** Actions section root (see PolicyActionsEditor.jsx, id="policy-edit-actions"). */
  public Locator actionsSection() {
    return locator("#policy-edit-actions");
  }

  /** Notifications section root (see PolicyNotificationsEditor.jsx, id="policy-edit-notifications"). */
  public Locator notificationsSection() {
    return locator("#policy-edit-notifications");
  }

  /** H1 page heading in the Summary card — always {@code "Policy Settings"} (see PolicyEditor.jsx). */
  public Locator pageHeading() {
    return locator(ROOT + " h1");
  }

  /** Server-side submit-error alert (rendered by NxStatefulForm when save fails). */
  public Locator submitErrorAlert() {
    return locator(".nx-form__submit-error");
  }

  /** Client-side validation-errors tooltip wrapper (rendered by NxStatefulForm when invalid). */
  public Locator validationErrors() {
    return locator(".nx-form__validation-errors");
  }

  /** NxSubmitMask success overlay — transient element, visible for ~800 ms after a save. */
  public Locator saveSuccessMask() {
    return locator(".nx-submit-mask--success");
  }

  /**
   * Selects the threat level by its rendered "{level} - {label}" option. {@code setExact(true)}
   * because the dropdown toggle re-labels to the selected option once open, which would
   * substring-collide with the new option (e.g. selected "5 - Severe" matches "4 - Severe").
   */
  public void selectThreatLevel(int level, String label) {
    threatLevelDropdown().click();
    locator("#editor-policy-threat-level")
        .getByRole(AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName(level + " - " + label).setExact(true))
        .click();
  }

  /**
   * The first (default) constraint's "Constraint Name" input scoped under the constraints
   * section. We target via {@code getByLabel} because the EditableConstraint passes its {@code
   * id} prop into NxTextInput; depending on the RSC version the id may end up on the wrapper
   * div rather than the inner {@code <input>}, while the {@code <label for=...>} association is
   * always stable.
   */
  public Locator firstConstraintName() {
    return constraintsSection().getByLabel("Constraint Name").first();
  }

  /**
   * The first (default) condition's "Age" value input on a fresh new-policy form (the default
   * condition is {@code AgeInDays / older than / <empty>}). Targeted via the placeholder so we
   * don't depend on the RSC NxTextInput DOM nesting.
   */
  public Locator firstConditionAgeValue() {
    return constraintsSection().getByPlaceholder("Age").first();
  }

  /**
   * Fills the constraint-name + default condition-value for the first (default) constraint that
   * ships with a new-policy form. Use this when the test only needs a valid form to submit and
   * doesn't care about the specific condition type — the default is {@code AgeInDays}.
   */
  public void fillDefaultConstraint(String name, int ageDays) {
    firstConstraintName().fill(name);
    firstConditionAgeValue().fill(String.valueOf(ageDays));
  }

  /**
   * Clicks the "Confirm Deletion" button then waits for the modal to auto-close.
   * The modal closes once the server responds with a 204 (NxStatefulForm dismisses on success).
   */
  public void confirmDeleteAndWaitForModalClose() {
    deleteModalConfirmButton().click(new Locator.ClickOptions().setForce(true));
    deleteModal().waitFor(new Locator.WaitForOptions()
        .setState(WaitForSelectorState.HIDDEN)
        .setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  /** Clicks Cancel then waits for the modal to dismiss without deleting. */
  public void cancelDeleteAndWaitForModalClose() {
    deleteModalCancelButton().click();
    deleteModal().waitFor(new Locator.WaitForOptions()
        .setState(WaitForSelectorState.HIDDEN)
        .setTimeout(PlaywrightTiming.BRIEF_UI_TRANSITION_MS));
  }

  /**
   * Waits for the new-policy form to be fully ready for input after the SPA route change triggered
   * by clicking "Add a Policy". Waits for the first constraint-name input to become visible — that
   * signals the constraint Redux slice has finished loading and the SPA won't re-seed the form.
   *
   * <p>
   * Call this immediately after {@code addPolicyButton().click()} before filling any field.
   */
  public void waitForNewPolicyFormReady() {
    firstConstraintName().waitFor();
  }

  /**
   * Click the "Create"/"Update" submit button rendered by {@code NxStatefulForm}.
   *
   * <p>
   * Notes on resilience:
   * <ul>
   * <li>The policy editor renders inside an {@code NxLoadWrapper}, so we first wait for the
   * form footer (which contains the submit button) to be attached to the DOM.</li>
   * <li>The button can sit below the viewport on tall forms and Playwright's auto-wait checks
   * visibility but does not auto-scroll, so we scroll into view first.</li>
   * <li>We click the button via {@code .click()} (no force) — auto-wait for visible/enabled
   * gives us the right blocking semantics: an invalid form will leave the button visible
   * and clickable, with NxStatefulForm short-circuiting the submit handler and surfacing
   * a validation tooltip via {@code .nx-form__validation-errors}.</li>
   * </ul>
   */
  public void clickSubmit() {
    // Scope to the policy-editor root so an open delete-modal's submit button can never match.
    Locator btn = locator(ROOT + " .nx-form__submit-btn").first();
    btn.waitFor();
    // The submit button lives inside NxStatefulForm's footer, which on this build renders with
    // a 0×0 bounding box according to Playwright's actionability heuristic even though the
    // <button> is fully styled and clickable in real browsers. We scroll via JS (which
    // bypasses Playwright's visibility check on the scroll operation itself) then force-click.
    btn.evaluate("el => el.scrollIntoView({block: 'center'})");
    btn.click(new Locator.ClickOptions().setForce(true));
  }
}
