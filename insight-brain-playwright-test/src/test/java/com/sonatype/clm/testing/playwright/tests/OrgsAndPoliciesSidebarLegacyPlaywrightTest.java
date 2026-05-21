/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.OwnerDetailSidebarComponent;
import com.sonatype.clm.testing.playwright.pages.OwnerDetailSidebarComponentAssertions;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPageAssertions;
import com.sonatype.clm.testing.playwright.pages.SidebarComponent;
import com.sonatype.clm.testing.playwright.pages.SidebarComponentAssertions;

import org.junit.Before;
import org.junit.Test;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import org.junit.experimental.categories.Category;

/**
 * Playwright test for the global "Orgs and Policies" navigation flow on the Root Organization:
 * <ol>
 * <li>Navigate to the Root Org owner-summary view.</li>
 * <li>Click the global sidebar "Orgs and Policies" button (id {@code #policies-navigation-button},
 * label "Orgs and Policies").</li>
 * <li>Click the Policies tile "Add a Policy" button on the owner summary.</li>
 * <li>Verify the {@code #owner-detail-sidebar} shows every baseline group/link defined by
 * {@code OwnerDetailSidebar.jsx}: Application Categories, Policies, Legacy Violations,
 * Continuous Monitoring, Proprietary Components, Component Labels, License Threat Groups,
 * Source Control, Access, Auto-Waivers. The Public Data Sources link is excluded because
 * it is gated on {@code isCpeMatchingSupported} which is not enabled in the default test
 * license — see {@link OwnerDetailSidebarComponent#shouldShowAllRootOrgLabels()}.</li>
 * </ol>
 *
 * <p>
 * Selectors live in {@link SidebarComponent}, {@link OwnerSummaryPage}, and
 * {@link OwnerDetailSidebarComponent}. Visible labels live on
 * {@link OwnerDetailSidebarComponent} as {@code LABEL_*} constants so renames touch one place.
 * See {@code PLAYWRIGHT_TEST_AUTHORING_GUIDE.md}.
 */
public class OrgsAndPoliciesSidebarLegacyPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String EXPECTED_ADD_POLICY_URL_FRAGMENT = "/policy";

  // --------------- @Before ---------------

  @Before
  public void openRootOrgAsAdmin() {
    playwrightRefreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    playwrightLogin();
  }

  // --------------- @Test methods ---------------

  /**
   * Walk the global sidebar -> Add a Policy flow and verify that the resulting page exposes the
   * full set of owner-detail-sidebar labels for the Root Organization.
   */
  @Test
  @Category(SanityTest.class)
  public void testOrgsAndPoliciesNavigation_RevealsOwnerDetailSidebarLabels() {
    SidebarComponent sidebar = new SidebarComponent();
    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    OwnerDetailSidebarComponent detailSidebar = new OwnerDetailSidebarComponent();

    // Given: Root Org owner summary is loaded (set up in @Before) and the global sidebar is visible.
    new SidebarComponentAssertions(sidebar).shouldBeVisible();
    new OwnerSummaryPageAssertions(ownerSummary).shouldBeVisible();

    // When: the user clicks the global sidebar "Orgs and Policies" button. Re-clicking when
    // already inside /management is a no-op for navigation but verifies the button stays
    // interactive across both the view and edit shells.
    sidebar.clickPoliciesNavigation();

    // And: clicks the Policies-tile "Add a Policy" button to enter the editor shell, where the
    // owner-detail sidebar mounts on the left.
    assertThat(ownerSummary.addPolicyButton()).isVisible();
    ownerSummary.addPolicyButton().click();
    page.waitForURL("**" + EXPECTED_ADD_POLICY_URL_FRAGMENT + "**");

    // Then: the owner-detail sidebar mounts on the left with all expected labels for a Root Org.
    new OwnerDetailSidebarComponentAssertions(detailSidebar).shouldShowAllRootOrgLabels();
  }
}
