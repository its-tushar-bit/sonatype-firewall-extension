/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.microsoft.playwright.Locator;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.PolicyEditorPage;
import com.sonatype.clm.testing.playwright.pages.PolicyEditorPageAssertions;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Organization;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;

/**
 * Playwright test for root-organization specifics of the policy editor: opening / editing / saving policies on the
 * root org, plus the cross-org inheritance flow (root org has no inherited policies; policies
 * created at the root are inherited and read-only at child orgs).
 *
 * <p>
 * Each test follows a Given/When/Then shape:
 * <ul>
 * <li>{@link #seedRootOrgAndOpenAsAdmin()} resolves the bootstrap-created root org and lands on
 * its owner summary logged-in as admin.</li>
 * <li>The test body either seeds policies via {@link com.sonatype.insight.brain.dataaccess.TemporaryEntity}
 * or drives the UI through the policy editor, then asserts via page-object locators.</li>
 * </ul>
 *
 * <p>
 * Selectors live in {@link PolicyEditorPage} and {@link OwnerSummaryPage}.
 */
public class RootOrganizationPolicyEditorPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String CREATED_ROOT_POLICY_NAME = "Root Security Policy";

  private static final int CREATED_ROOT_POLICY_THREAT_LEVEL = 9;

  private static final String CREATED_ROOT_POLICY_THREAT_LABEL = "Critical";

  private static final String CREATED_ROOT_POLICY_CONSTRAINT_NAME = "Root security constraint";

  private static final int CREATED_ROOT_POLICY_AGE_IN_DAYS = 30;

  private static final String INHERITED_POLICY_NAME = "Inheritable Root Policy";

  private static final int INHERITED_POLICY_THREAT_LEVEL = 6;

  private static final String CHILD_ORGANIZATION_NAME_PREFIX = "Child Org";

  private Organization rootOrganization;

  // --------------- @Before ---------------

  @Before
  public void seedRootOrgAndOpenAsAdmin() {
    rootOrganization = lookup(OrganizationDAO.class).getById(ROOT_ORGANIZATION_ID);
    // The default test license already enables Policy Management + Lifecycle. The legacy
    // setFeatures(POLICY_MANAGEMENT, FIREWALL) call here was a holdover from the Selenide
    // migration; it actually *narrows* the license set and breaks the policies-tile data load
    // ("Your IQ Server license does not enable this feature"), so we no longer call it.
    playwrightRefreshOrOpen(OwnerSummaryPage.url(rootOrganization));
    playwrightLogin();
  }

  // --------------- @Test methods ---------------
  // Note: the legacy testNewPolicyEditor / testEditExistingPolicy / testSavePolicy methods
  // (migrated 1:1 from the Selenide RootOrganizationPolicyEditorTest) were removed because they
  // duplicated coverage already provided by OrganizationPolicyEditorPlaywrightTest:
  // - testNewPolicyEditor ⇄ OrganizationPolicyEditorPlaywrightTest#testNewPolicyCreation
  // - testEditExistingPolicy ⇄ OrganizationPolicyEditorPlaywrightTest#testPolicyEditorLoads
  // - testSavePolicy ⇄ OrganizationPolicyEditorPlaywrightTest#testCreatePolicy_endToEndFromOwnerSummary
  // The removed tests asserted nothing root-org-specific; the remaining tests below cover the
  // genuine root-org behavior (no inherited section + child-org inheritance round-trip).

  /**
   * Steps 4-6 of the manual spec: from the root org's owner summary, open the Policies tile,
   * verify there is no "Inherited Policies" section (root has no parent), then click "Add a
   * Policy", create a policy through the full editor flow, and verify the new policy appears in
   * the local section of the policies tile.
   */
  @Test
  @Category(SanityTest.class)
  public void testRootOrgHasNoInheritedPolicies_andCreatePolicyAppearsInList() {
    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    PolicyEditorPage editorPage = new PolicyEditorPage();

    // Given: open the Policies tile on the root org owner summary.
    ownerSummary.openPoliciesSectionFromNavPills();
    assertThat(ownerSummary.policiesTile()).isVisible();

    // Then (root-no-inheritance): the policies tile renders either the local section (when the
    // root has any local policies) or the empty-state "No local policies defined" message; in
    // either case there is NO "Inherited from ..." section because the root org has no parent.
    // (The empty-state message is rendered as an NxList instead of a PoliciesTable when the
    // org has no local policies — see PoliciesTile.jsx, isNoPoliciesDefined branch.)
    assertThat(ownerSummary.policiesTileInheritedSections()).hasCount(0);

    // When: the user clicks "Add a Policy" and fills the editor with a valid policy.
    ownerSummary.addPolicyButton().click();
    assertThat(editorPage.firstConstraintName()).isVisible();
    editorPage.policyName().fill(CREATED_ROOT_POLICY_NAME);
    editorPage.selectThreatLevel(
        CREATED_ROOT_POLICY_THREAT_LEVEL, CREATED_ROOT_POLICY_THREAT_LABEL);
    editorPage.fillDefaultConstraint(
        CREATED_ROOT_POLICY_CONSTRAINT_NAME, CREATED_ROOT_POLICY_AGE_IN_DAYS);
    editorPage.clickSubmit();
    waitForSubmitMask();

    // Then: after refreshing the owner summary, the new policy appears in the policies tile.
    playwrightRefreshOrOpen(OwnerSummaryPage.url(rootOrganization));
    ownerSummary.openPoliciesSectionFromNavPills();
    assertThat(ownerSummary.policiesTileRowByName(CREATED_ROOT_POLICY_NAME)).isVisible();
  }

  /**
   * Step 7 of the manual spec: verify that a policy created at the root org appears in the
   * "Inherited from Root Organization" section of any child org's policies tile, and that
   * opening the inherited policy from the child org shows the read-only "Policy Settings" editor
   * with no Delete button.
   */
  @Test
  @Category(SanityTest.class)
  public void testPolicyAtRootIsInheritedByChildOrg_andIsReadOnlyThere() {
    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    PolicyEditorPage editorPage = new PolicyEditorPage();

    // Given: a policy seeded at the root org and a temp child org under root. The seeded policy
    // is identified downstream by name, so we don't need to keep a reference to the returned
    // Policy — tempEntity owns its cleanup.
    tempEntity.newPolicy(ROOT_ORGANIZATION_ID, INHERITED_POLICY_NAME, INHERITED_POLICY_THREAT_LEVEL);
    Organization childOrg = tempEntity.newOrganization(
        CHILD_ORGANIZATION_NAME_PREFIX + "-" + TemporaryEntity.uuid(),
        rootOrganization);
    // When: navigate to the child org's owner summary and open its policies tile.
    playwrightRefreshOrOpen(OwnerSummaryPage.url(childOrg));
    ownerSummary.openPoliciesSectionFromNavPills();
    assertThat(ownerSummary.policiesTile()).isVisible();

    // Then: an "Inherited from Root Organization" section exists with a row for the parent
    // policy (tile data is loaded asynchronously after the nav-pill scroll).
    Locator inheritedSection = ownerSummary.policiesTileInheritedSectionFor(rootOrganization.getName());
    assertThat(inheritedSection).isVisible();
    assertThat(ownerSummary.policiesTileRowByName(INHERITED_POLICY_NAME)).isVisible();

    // When: click the inherited policy row to open the editor at the child-org level.
    ownerSummary.policiesTileRowByName(INHERITED_POLICY_NAME).click();
    // Inherited policies render ReadOnlyConstraint (not EditableConstraint), so there is no
    // "Constraint Name" input — wait for the editor shell instead.
    assertThat(editorPage.container()).isVisible();

    // Then: the editor renders in inherited / read-only mode (heading "Policy Settings", policy name
    // disabled, no Delete Policy button).
    new PolicyEditorPageAssertions(editorPage).shouldBeInheritedReadOnlyView();
    assertThat(editorPage.policyName()).hasValue(INHERITED_POLICY_NAME);
  }
}
