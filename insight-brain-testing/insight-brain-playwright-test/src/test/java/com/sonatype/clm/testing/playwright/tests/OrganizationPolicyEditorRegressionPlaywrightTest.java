/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.PolicyEditorPage;
import com.sonatype.clm.testing.playwright.pages.PolicyEditorPageAssertions;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Regression carve-out from {@link OrganizationPolicyEditorPlaywrightTest} (sanity sibling at
 * class-size budget). Covers edge cases, validation paths, and deeper PolicyEditor scenarios.
 */
public class OrganizationPolicyEditorRegressionPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String ORG_NAME_PREFIX = "PolicyEditorRegressionOrg";

  private static final String POLICY_NAME = "Regression Policy";

  private static final int POLICY_THREAT_LEVEL = 5;

  private static final String DELETE_CONFIRMATION_KEYWORD = "DELETE";

  private Organization organization;

  @Before
  public void seedOrgAndOpenAsAdmin() {
    String orgName = ORG_NAME_PREFIX + "-" + TemporaryEntity.uuid();
    organization = tempEntity.newOrganization(orgName);

    navigateAndWaitForUrl(OwnerSummaryPage.url(organization), OwnerSummaryPage.ORG_URL_FRAGMENT);
    playwrightLogin();
  }

  @Test
  @Category(RegressionTest.class)
  public void testDeletePolicy_policyGoneFromPoliciesTile() {
    PolicyEditorPage editorPage = new PolicyEditorPage();
    String deletePolicyName = "Delete Me Policy " + TemporaryEntity.uuid();
    Policy policy =
        tempEntity.newPolicy(organization.getId(), deletePolicyName, POLICY_THREAT_LEVEL);

    navigateAndWaitForUrl(PolicyEditorPage.url(organization, policy), PolicyEditorPage.EDIT_URL_FRAGMENT);
    assertThat(editorPage.container()).isVisible();
    editorPage.deletePolicyButton().click();
    new PolicyEditorPageAssertions(editorPage).shouldShowDeleteModal();
    editorPage.deleteModalInput().fill(DELETE_CONFIRMATION_KEYWORD);
    editorPage.confirmDeleteAndWaitForModalClose();

    navigateAndWaitForUrl(OwnerSummaryPage.url(organization), OwnerSummaryPage.ORG_URL_FRAGMENT);
    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    ownerSummary.openPoliciesSectionFromNavPills();
    assertThat(ownerSummary.policiesTileRowByName(deletePolicyName)).hasCount(0);
  }

  /**
   * The editor enforces "at least one constraint" at the UI layer — the lone constraint's
   * delete button is disabled via {@code cannotBeRemoved} in {@code EditableConstraint.jsx}.
   */
  @Test
  @Category(RegressionTest.class)
  public void testNewPolicy_singleConstraintDeleteButtonDisabled() {
    PolicyEditorPage editorPage = new PolicyEditorPage();

    playwrightRefreshOrOpen(PolicyEditorPage.newPolicyUrl(organization));
    assertThat(editorPage.container()).isVisible();
    assertThat(editorPage.firstConstraintName()).isVisible();

    assertThat(editorPage.deleteConstraintButton(0)).isDisabled();
  }

  @Test
  @Category(RegressionTest.class)
  public void testNewPolicy_multipleConditionsInOneConstraint() {
    String multiConditionPolicyName = "Multi-Condition " + TemporaryEntity.uuid();
    Policy p = new Policy(null, multiConditionPolicyName);
    p.setOwnerId(organization.getId());
    p.setThreatLevel(9);

    Constraint constraint = new Constraint(null, "Multi-Condition constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition("SecurityVulnerabilitySeverity", ">=", "7"));
    constraint.addCondition(new Condition("MatchState", "is", "exact"));
    p.addConstraint(constraint);
    Policy seeded = tempEntity.newPolicy(p);

    navigateAndWaitForUrl(PolicyEditorPage.url(organization, seeded),
        PolicyEditorPage.EDIT_URL_FRAGMENT);
    PolicyEditorPage editorPage = new PolicyEditorPage();
    assertThat(editorPage.container()).isVisible();
    // Assert each condition's summary text appears inside the same constraint card — fails if
    // a future change splits the constraint or drops a condition.
    assertThat(editorPage.readOnlyConstraints()).hasCount(1);
    assertThat(editorPage.readOnlyConstraints().first())
        .containsText("Security Vulnerability Severity");
    assertThat(editorPage.readOnlyConstraints().first()).containsText("Match State");
  }

  /**
   * Asserts the Notifications section heading renders. The full add-recipient-and-persist flow
   * is deferred — populating recipients deterministically is non-trivial.
   */
  @Test
  @Category(RegressionTest.class)
  public void testPolicy_notificationsSectionRenders() {
    Policy policy = tempEntity.newPolicy(organization.getId(), POLICY_NAME, POLICY_THREAT_LEVEL);
    navigateAndWaitForUrl(PolicyEditorPage.url(organization, policy),
        PolicyEditorPage.EDIT_URL_FRAGMENT);

    PolicyEditorPage editorPage = new PolicyEditorPage();
    assertThat(editorPage.container()).isVisible();
    assertThat(editorPage.container()
        .getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setName("Notifications")))
            .isVisible();
  }
}
