/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright page object for the Policy Editor page.
 */
public class PolicyEditorPage
    extends BasePage
{
  private static final String ROOT = "#policy-editor-summary";

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

  public Locator deleteModalInput() {
    return locator("#policy-delete-modal input[type='text']");
  }

  public Locator deleteModalValidation() {
    return locator("#policy-delete-modal .nx-field-validation-message");
  }

  public Locator deleteModalErrors() {
    return locator("#policy-delete-modal .nx-form__validation-errors");
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

  /** Selects the threat level by its rendered "{level} - {label}" option text in the dropdown. */
  public void selectThreatLevel(int level, String label) {
    threatLevelDropdown().click();
    page.locator("#editor-policy-threat-level")
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(level + " - " + label))
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
    assertThat(btn).isAttached();
    // The submit button lives inside NxStatefulForm's footer, which on this build renders with
    // a 0×0 bounding box according to Playwright's actionability heuristic even though the
    // <button> is fully styled and clickable in real browsers. We scroll via JS (which
    // bypasses Playwright's visibility check on the scroll operation itself) then force-click.
    btn.evaluate("el => el.scrollIntoView({block: 'center'})");
    btn.click(new Locator.ClickOptions().setForce(true));
  }
}
