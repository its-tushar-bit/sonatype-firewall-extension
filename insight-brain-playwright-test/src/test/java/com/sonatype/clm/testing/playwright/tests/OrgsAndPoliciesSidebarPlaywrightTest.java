/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.util.List;
import java.util.Map;

import com.sonatype.clm.testing.functional.utils.NameSupplierDictionary;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.OrgsAndPoliciesSidebarComponent;
import com.sonatype.clm.testing.playwright.pages.OrgsAndPoliciesSidebarComponentAssertions;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.UnsavedChangesModalComponent;
import com.sonatype.insight.brain.model.Organization;

import com.microsoft.playwright.Locator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import org.junit.experimental.categories.Category;

/**
 * Playwright migration of the Selenide {@code OrgsAndPoliciesSidebarTest}.
 * <p>
 * Exercises the global "Orgs and Policies" summary sidebar ({@code OwnerSideNav.jsx}, root
 * {@code .iq-orgs-and-policies-summary-sidebar}) on the Root Organization owner-summary view:
 * <ol>
 * <li><b>{@code testSidebarLoadsWithOrganizations}</b> — sidebar mounts with the
 * {@code .iq-selected-org} header and the seeded child organizations are rendered as
 * {@code role="menuitem"} links inside {@code #organizations-collapsible}.</li>
 * <li><b>{@code testSidebarNavigationToChildOrg}</b> — clicking the first child-org link
 * navigates to that org's owner-summary URL and the displayed selected-owner updates.</li>
 * <li><b>{@code testAddApplicationFromSidebar}</b> — opening the "Add Application" split-button
 * on a non-synthetic owner and clicking "New Application" launches the
 * {@code <NxModal id="owner-editor">} with the {@code "New Application"} heading.</li>
 * </ol>
 *
 * <p>
 * All locators live on {@link OrgsAndPoliciesSidebarComponent} and {@link OwnerSummaryPage}.
 * See {@code PLAYWRIGHT_TEST_AUTHORING_GUIDE.md}.
 */
public class OrgsAndPoliciesSidebarPlaywrightTest
    extends AbstractIqUiTest
{
  private static final int TOTAL_DEPTH = 2;

  private static final int ROOT_CHILD_ORG_COUNT = 3;

  private static final int LEAF_CHILD_ORG_COUNT = 3;

  private static final int MIN_EXPECTED_SIDEBAR_ORG_LINKS = 3;

  private static final String EXPECTED_OWNER_EDITOR_HEADING = "New Application";

  private static final String EXPECTED_OWNER_EDITOR_NAME_LABEL = "Application Name";

  private Map<Integer, List<Organization>> organizationsByDepth;

  // --------------- @Before / @After ---------------

  @Before
  public void seedTreeAndOpenRootOrgAsAdmin() {
    seedRelatedOrganizations();
    playwrightRefreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    playwrightLogin();
  }

  private void seedRelatedOrganizations() {
    organizationsByDepth = tempEntity.newRelatedOrganizationsAsMap(
        null,
        ROOT_CHILD_ORG_COUNT,
        TOTAL_DEPTH,
        LEAF_CHILD_ORG_COUNT,
        new NameSupplierDictionary());
  }

  /**
   * First child org seeded directly under Root.
   * <p>
   * {@code TemporaryEntity.newRelatedOrganizationsAsMap} keys the returned map by the
   * <em>recursion depth</em> (not tree depth): the recursion starts at {@code totalDepth - 1}
   * for the children of {@code parentOrg} and decrements as it descends, so the orgs created
   * directly under Root land at the highest key ({@code totalDepth - 1}) and the leaves land
   * at key {@code 0}.
   */
  private Organization firstChildOrg() {
    return organizationsByDepth.get(TOTAL_DEPTH - 1).get(0);
  }

  @After
  public void dismissUnsavedChangesIfOpen() {
    new UnsavedChangesModalComponent().continueIfOpen();
  }

  // --------------- @Test methods ---------------

  /**
   * The summary sidebar mounts with both the selected-owner header and at least
   * {@link #MIN_EXPECTED_SIDEBAR_ORG_LINKS} child-org links inside
   * {@code #organizations-collapsible}.
   */
  @Test
  @Category(SanityTest.class)
  public void testSidebarLoadsWithOrganizations() {
    OrgsAndPoliciesSidebarComponent sidebar = new OrgsAndPoliciesSidebarComponent();
    OrgsAndPoliciesSidebarComponentAssertions sidebarAssertions =
        new OrgsAndPoliciesSidebarComponentAssertions(sidebar);
    sidebarAssertions.shouldBeVisibleWithSelectedOwner();
    assertThat(sidebar.organizationsGroup()).isVisible();
    assertThat(sidebar.organizationLinks().first()).isVisible();
    int linkCount = sidebar.organizationLinks().count();
    if (linkCount < MIN_EXPECTED_SIDEBAR_ORG_LINKS) {
      throw new AssertionError(
          "Expected at least " + MIN_EXPECTED_SIDEBAR_ORG_LINKS
              + " org links in #organizations-collapsible but found " + linkCount);
    }
  }

  /**
   * Clicking the first child-org link in the sidebar navigates to that org's
   * owner-summary URL and the displayed selected-owner updates accordingly.
   */
  @Test
  @Category(SanityTest.class)
  public void testSidebarNavigationToChildOrg() {
    OrgsAndPoliciesSidebarComponent sidebar = new OrgsAndPoliciesSidebarComponent();
    OrgsAndPoliciesSidebarComponentAssertions sidebarAssertions =
        new OrgsAndPoliciesSidebarComponentAssertions(sidebar);
    Organization firstChildOrg = firstChildOrg();
    String expectedUrlSuffix = "/management/view/organization/" + firstChildOrg.getPublicId();

    Locator firstLink = sidebar.organizationLinks().first();
    assertThat(firstLink).isVisible();
    firstLink.click();
    page.waitForURL("**" + expectedUrlSuffix);
    sidebarAssertions.shouldBeVisibleWithSelectedOwner();
  }

  /**
   * Opening the "Add Application" split-button on a non-synthetic child org and clicking
   * "New Application" mounts the owner-editor modal with the {@code "New Application"} heading
   * and the {@code editor-owner-name} text input.
   */
  @Test
  @Category(SanityTest.class)
  public void testAddApplicationFromSidebar() {
    Organization firstChildOrg = firstChildOrg();
    playwrightRefreshOrOpen(OwnerSummaryPage.url(firstChildOrg));

    OrgsAndPoliciesSidebarComponent sidebar = new OrgsAndPoliciesSidebarComponent();
    new OrgsAndPoliciesSidebarComponentAssertions(sidebar).shouldBeVisibleWithSelectedOwner();

    Locator modal = sidebar.openNewApplicationModal();
    assertThat(modal).containsText(EXPECTED_OWNER_EDITOR_HEADING);
    assertThat(modal.locator("#editor-owner-name")).containsText(EXPECTED_OWNER_EDITOR_NAME_LABEL);
  }
}
