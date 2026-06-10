/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.util.regex.Pattern;

import com.microsoft.playwright.assertions.LocatorAssertions;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPageAssertions;
import com.sonatype.clm.testing.playwright.pages.PolicyEditorPage;
import com.sonatype.clm.testing.playwright.pages.UnsavedChangesModalComponent;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 *
 * <p>
 * Each test follows a Given/When/Then shape:
 * <ul>
 * <li>{@link #seedOrgAndOpenAsAdmin()} seeds a per-test {@link Organization} (name is suffixed
 * with a UUID so parallel runs cannot collide) and lands on the owner summary logged-in as
 * admin.</li>
 * <li>The test body either seeds a {@link Policy} via {@link com.sonatype.insight.brain.dataaccess.TemporaryEntity}
 * or opens the new-policy editor, navigates to the {@link PolicyEditorPage}, and asserts
 * editor state via page-object locators.</li>
 * </ul>
 *
 * <p>
 * Selectors live in {@link PolicyEditorPage}.
 */
public class OrganizationPolicyEditorPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String ORGANIZATION_NAME_PREFIX = "Ye Ole Organization";

  private static final String POLICY_NAME = "Test Policy";

  private static final String INHERITANCE_POLICY_NAME = "Test Inheritance";

  private static final int POLICY_THREAT_LEVEL = 5;

  private static final String CREATED_POLICY_NAME = "Test Critical CVE Policy";

  private static final int CREATED_POLICY_THREAT_LEVEL = 10;

  private static final String CREATED_POLICY_THREAT_LABEL = "Critical";

  private static final String CREATED_POLICY_CONSTRAINT_NAME = "Critical CVE constraint";

  private static final int CREATED_POLICY_AGE_IN_DAYS = 30;

  private static final String NEW_POLICY_HEADING = "Policy Settings";

  private Organization organization;

  // --------------- @Before ---------------

  @Before
  public void seedOrgAndOpenAsAdmin() {
    String orgName = ORGANIZATION_NAME_PREFIX + "-" + TemporaryEntity.uuid();
    organization = tempEntity.newOrganization(orgName);

    playwrightRefreshOrOpen(OwnerSummaryPage.url(organization));
    playwrightLogin();
  }

  // --------------- @Test methods ---------------

  @Test
  @Category(SanityTest.class)
  public void testPolicyEditorLoads() {
    // Given: an existing policy on the seeded organization.
    Policy policy = tempEntity.newPolicy(organization.getId(), POLICY_NAME, POLICY_THREAT_LEVEL);

    // When: the editor URL for that policy is opened.
    playwrightRefreshOrOpen(PolicyEditorPage.url(organization, policy));

    // Then: the editor renders and pre-fills the policy name.
    PolicyEditorPage editorPage = new PolicyEditorPage();
    assertThat(editorPage.container()).isVisible();
    assertThat(editorPage.policyName()).hasValue(POLICY_NAME);
  }

  @Test
  @Category(SanityTest.class)
  public void testNewPolicyCreation() {
    // Given/When: the new-policy editor URL for the seeded org is opened.
    playwrightRefreshOrOpen(PolicyEditorPage.newPolicyUrl(organization));

    // Then: the editor renders empty with the threat-level dropdown ready for input.
    PolicyEditorPage editorPage = new PolicyEditorPage();
    assertThat(editorPage.container()).isVisible();
    assertThat(editorPage.policyName()).isEmpty();
    assertThat(editorPage.threatLevelDropdown()).isVisible();
  }

  @Test
  @Category(SanityTest.class)
  public void testInheritanceSection() {
    // Given: an existing policy on the seeded organization. The inheritance section is always
    // rendered for organization-owned policies (see EditPolicyInheritance.jsx).
    Policy policy =
        tempEntity.newPolicy(organization.getId(), INHERITANCE_POLICY_NAME, POLICY_THREAT_LEVEL);
    // When: the editor URL is opened.
    playwrightRefreshOrOpen(PolicyEditorPage.url(organization, policy));

    // Then: both the editor container and the inheritance section render.
    PolicyEditorPage editorPage = new PolicyEditorPage();
    LocatorAssertions.IsVisibleOptions visibleTimeout =
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);
    assertThat(editorPage.container()).isVisible(visibleTimeout);
    assertThat(editorPage.inheritanceSection()).isVisible(visibleTimeout);
  }

  /**
   * End-to-end policy creation flow starting from the owner summary's "Add a Policy" button:
   * verifies that the new-policy form renders all four editor sections, accepts a name + threat
   * level, persists on submit, and that the new policy appears in the owner-summary policies list
   * after a page refresh.
   *
   * <p>
   * Note: IQ does not expose a top-level "policy type" dropdown — type is implicit from the
   * selected condition type. The default constraint and condition that ship with the form are
   * intentionally left untouched here; deeper constraint/condition wiring (e.g. switching to
   * "Security Vulnerability Severity") is exercised by other tests via {@code tempEntity} seeding
   * to keep this UI flow focused on the create round-trip.
   */
  @Test
  @Category(SanityTest.class)
  public void testCreatePolicy_endToEndFromOwnerSummary() {
    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    PolicyEditorPage editorPage = new PolicyEditorPage();

    // Given: the user is on the owner summary and clicks "Add a Policy" on the Policies tile.
    // After the SPA route change we wait for network-idle so the policy + constraint slices
    // finish their initial loads before our subsequent fills (otherwise the redux store's
    // currentPolicy gets re-seeded and blows away anything we typed).
    ownerSummary.openPoliciesSectionFromNavPills();
    ownerSummary.addPolicyButton().click();
    assertThat(editorPage.firstConstraintName()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));

    // Then: the new-policy form renders with all four editor sections (Summary, Inheritance,
    // Constraints, Actions, Notifications) and the heading reads "Policy Settings". Wait for the
    // first constraint's name input to become visible — that signals the constraint slice has
    // finished loading (loadConstraint resolves) and the SPA won't blow away our fills.
    assertThat(editorPage.container()).isVisible();
    assertThat(editorPage.pageHeading()).hasText(NEW_POLICY_HEADING);
    assertThat(editorPage.policyName()).isEmpty();
    assertThat(editorPage.threatLevelDropdown()).isVisible();
    assertThat(editorPage.inheritanceSection()).isVisible();
    assertThat(editorPage.constraintsSection()).isVisible();
    assertThat(editorPage.actionsSection()).isVisible();
    assertThat(editorPage.notificationsSection()).isVisible();
    assertThat(editorPage.firstConstraintName()).isVisible();

    // When: the user fills the policy name + threat level + the default constraint (the form
    // ships with one empty AgeInDays constraint that must be valid before NxStatefulForm allows
    // submit) and clicks Create.
    editorPage.policyName().fill(CREATED_POLICY_NAME);
    editorPage.selectThreatLevel(CREATED_POLICY_THREAT_LEVEL, CREATED_POLICY_THREAT_LABEL);
    editorPage.fillDefaultConstraint(CREATED_POLICY_CONSTRAINT_NAME, CREATED_POLICY_AGE_IN_DAYS);
    editorPage.clickSubmit();
    waitForSubmitMask();

    // Then: after refreshing the owner summary, the new policy appears in the policies tile.
    // Re-open explicitly because Create may leave the editor on /policy or redirect to /policy/<id>
    // depending on build state — the list is the user-visible proof of persistence.
    playwrightRefreshOrOpen(OwnerSummaryPage.url(organization));
    ownerSummary.openPoliciesSectionFromNavPills();
    assertThat(ownerSummary.policiesTileRowByName(CREATED_POLICY_NAME)).isVisible();
  }

  /**
   * Validation cases for the new-policy editor — combined into one {@code @Test} since both
   * exercise the same form on the same seeded org and only the input differs.
   *
   * <p>
   * <strong>Case 1 — Missing name (client-side):</strong> fill a valid threat level + valid
   * default constraint but leave the policy name empty, then click Create. NxStatefulForm marks
   * the form invalid (the policy name is required) and surfaces the
   * {@code .nx-form__validation-errors} banner instead of redirecting.
   *
   * <p>
   * <strong>Case 2 — Duplicate name (client-side):</strong> seed a policy with a known name on
   * the org, fill the editor with the same name + valid threat + valid constraint, click Create.
   * Duplicate detection runs client-side ({@code policyNameValidator} / siblings list), so the
   * form surfaces {@code .nx-form__validation-errors} and the URL stays on {@code /policy}.
   *
   * <p>
   * Note: NxStatefulForm only renders {@code .nx-form__validation-errors} in the visible state
   * after the form has been dirtied (any field touched). The cases below always touch fields
   * before submitting, which is also how a real user would interact with the form.
   */
  @Test
  @Category(SanityTest.class)
  public void testNewPolicyValidation_missingNameAndDuplicateName() {
    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    PolicyEditorPage editorPage = new PolicyEditorPage();

    // Given: the user opens the new-policy editor for the seeded organization. Wait for the
    // first constraint name to render so subsequent fills are not raced by the SPA's async
    // policy-slice / constraint-slice initialization.
    playwrightRefreshOrOpen(PolicyEditorPage.newPolicyUrl(organization));
    assertThat(editorPage.container()).isVisible();
    assertThat(editorPage.firstConstraintName()).isVisible();

    // Case 1 — When: everything is filled except the policy name, and the user clicks Create.
    editorPage.selectThreatLevel(CREATED_POLICY_THREAT_LEVEL, CREATED_POLICY_THREAT_LABEL);
    editorPage.fillDefaultConstraint(CREATED_POLICY_CONSTRAINT_NAME, CREATED_POLICY_AGE_IN_DAYS);
    editorPage.clickSubmit();
    waitForSubmitMask();

    // Case 1 — Then: the URL stays on /policy and the page surfaces a "policy name is required"
    // error. NxStatefulForm in this build routes the 400 response through its loadError slot
    // rather than its submitError slot, so we assert on visible text rather than a specific
    // error-element class.
    assertThat(page).hasURL(Pattern.compile(".*/policy$"));
    assertThat(page.getByText("The policy name is required").first()).isVisible();
    playwrightRefreshOrOpen(OwnerSummaryPage.url(organization));
    // Filled fields (threat level + constraint) create "unsaved changes". The SPA router shows
    // an "Unsaved Changes" modal when navigating away; dismiss it so the owner summary renders.
    new UnsavedChangesModalComponent().dismissIfAppearsWithin(PlaywrightTiming.BRIEF_UI_TRANSITION_MS);
    // playwrightRefreshOrOpen only waits for the document LOAD state; the SPA still needs to
    // hydrate the owner-summary route (NavPills.jsx) before the nav-pill testid is in the DOM.
    // Without this gate the subsequent click flakes with a 30 s timeout on cold backends.
    new OwnerSummaryPageAssertions(ownerSummary).shouldBeVisible();
    ownerSummary.openPoliciesSectionFromNavPills();
    assertThat(ownerSummary.policiesTile().getByText("No local policies defined")).isVisible();

    // Case 2 — Given: a duplicate-name seed exists on the same organization.
    String duplicateName = "Dup Policy " + TemporaryEntity.uuid();
    tempEntity.newPolicy(organization.getId(), duplicateName, POLICY_THREAT_LEVEL);
    // Case 2 — When: the empty-name rejection from case 1 leaves NxStatefulForm in a load-error
    // state (the form children are replaced by an error+Retry banner). Reload the editor URL to
    // get a clean form, then fill name + threat + constraint and submit.
    playwrightRefreshOrOpen(PolicyEditorPage.newPolicyUrl(organization));
    assertThat(editorPage.firstConstraintName()).isVisible();
    editorPage.policyName().fill(duplicateName);
    editorPage.selectThreatLevel(CREATED_POLICY_THREAT_LEVEL, CREATED_POLICY_THREAT_LABEL);
    editorPage.fillDefaultConstraint(CREATED_POLICY_CONSTRAINT_NAME, CREATED_POLICY_AGE_IN_DAYS);
    editorPage.clickSubmit();
    waitForSubmitMask();

    // Case 2 — Then: the form did not redirect (still on /policy) and the validation summary
    // is visible. After refreshing the owner summary, exactly one row for the duplicate name
    // appears in the policies tile (the seeded policy — no second row was created).
    assertThat(page).hasURL(Pattern.compile(".*/policy$"));
    assertThat(editorPage.validationErrors()).isVisible();
    playwrightRefreshOrOpen(OwnerSummaryPage.url(organization));
    // Filled fields (name + threat level + constraint) create "unsaved changes". Dismiss the
    // "Unsaved Changes" modal if the SPA router raises it before continuing.
    new UnsavedChangesModalComponent().dismissIfAppearsWithin(PlaywrightTiming.BRIEF_UI_TRANSITION_MS);
    new OwnerSummaryPageAssertions(ownerSummary).shouldBeVisible();
    ownerSummary.openPoliciesSectionFromNavPills();
    assertThat(ownerSummary.policiesTileRowByName(duplicateName)).hasCount(1);
  }

}
