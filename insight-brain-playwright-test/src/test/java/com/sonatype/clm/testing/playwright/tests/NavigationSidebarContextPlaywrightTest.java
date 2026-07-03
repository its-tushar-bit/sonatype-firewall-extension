/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.microsoft.playwright.Locator;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.OrgsAndPoliciesSidebarComponent;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.SidebarComponent;
import com.sonatype.clm.testing.playwright.pages.ViolationDetailsRegressionPage;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Regression tests for context-specific sidebar navigation. */
public class NavigationSidebarContextPlaywrightTest
    extends AbstractIqUiTest
{
  /**
   * SidebarNavViolationList renders in violation detail context; the selected item is visible.
   */
  @Test
  @Category(RegressionTest.class)
  public void testViolationContextSidebar_sidebarNavListRenders() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Policy policy = tempEntity.newPolicy(
        Organization.ROOT_ORGANIZATION_ID, "SidebarNavPolicy", 7);
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(
        app.getId(), StageTypes.BUILD.getId(), TemporaryEntity.uuid());
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy);

    // ?type=violation&sidebarReference=filter triggers SidebarNavList to call
    // loadFilter('violations', true), populating #sidebar-nav-list with violations.
    playwrightRefreshOrOpen(ViolationDetailsRegressionPage.url(violation.getId())
        + "?type=violation&sidebarReference=filter");
    playwrightLogin();

    ViolationDetailsRegressionPage detailsPage = new ViolationDetailsRegressionPage();
    assertThat(detailsPage.container()).isVisible();
    assertThat(detailsPage.sidebarNavList()).not().hasCount(0);
    assertThat(detailsPage.sidebarNavSelectedItem()).hasCount(1);
  }

  /**
   * Organizations collapsible in Orgs &amp; Policies sidebar toggles: open by default, collapses, re-expands.
   */
  @Test
  @Category(RegressionTest.class)
  public void testOrgsAndPoliciesSidebar_organizationsCategoryCollapsesAndExpands() {
    tempEntity.newOrganization(); // ensures ≥1 child link in #organizations-collapsible

    playwrightRefreshOrOpen(OwnerSummaryPage.url(Organization.ROOT_ORGANIZATION_ID));
    playwrightLogin();
    new SidebarComponent().clickPoliciesNavigation();

    OrgsAndPoliciesSidebarComponent sidebar = new OrgsAndPoliciesSidebarComponent();
    assertThat(sidebar.container()).isVisible();

    Locator orgLinks = sidebar.organizationLinks();
    assertThat(orgLinks.first()).isVisible();

    // Scoped to #organizations-collapsible to avoid matching Application/Repository triggers.
    // ID selector unavoidable: NxCollapsibleItems trigger button has no accessible name.
    Locator orgTrigger = sidebar.organizationsGroup().locator(".nx-collapsible-items__trigger");

    orgTrigger.click();
    assertThat(orgLinks.first()).not().isVisible();

    orgTrigger.click();
    assertThat(orgLinks.first()).isVisible();
  }
}
