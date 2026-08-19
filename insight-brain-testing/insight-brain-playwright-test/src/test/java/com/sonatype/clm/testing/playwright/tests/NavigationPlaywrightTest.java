/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.HeaderComponent;
import com.sonatype.clm.testing.playwright.pages.HeaderComponentAssertions;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.SidebarComponent;
import com.sonatype.clm.testing.playwright.pages.SidebarComponentAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightWaitUtils;
import com.sonatype.insight.brain.model.Organization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Navigation and sidebar smoke tests using Playwright.
 * <p>
 * Authoring rules: see {@code TestAuthourskill.md}. There is intentionally no JSON fixture for
 * this class — every literal it would need is either a route (lives on the page object per
 * §4) or a URL fragment (now exposed by {@link SidebarComponent} as
 * {@code expectedDashboardUrlFragment()} / {@code expectedPoliciesUrlFragment()}).
 * <p>
 * <b>Default test data:</b> These tests rely on the shared functional-test bootstrap in
 * {@link com.sonatype.clm.testing.playwright.AbstractIqUiTest}: migrated schema,
 * embedded IQ server, default test license, mocked HDS responses, and login as
 * {@code admin} / {@code admin123}. No orgs or applications are required for sidebar
 * visibility.
 */
public class NavigationPlaywrightTest
    extends AbstractIqUiTest
{

  @BeforeEach
  public void openDashboardAsAdmin() {
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
  }

  @Test
  @Tag("sanity")
  public void testSidebarIsVisibleAfterLogin() {
    SidebarComponent sidebar = new SidebarComponent();
    SidebarComponentAssertions sidebarAssertions = new SidebarComponentAssertions(sidebar);
    sidebarAssertions.shouldBeVisible();
    sidebarAssertions.shouldShowDashboard();
  }

  @Test
  @Tag("sanity")
  public void testSidebarToggle() {
    SidebarComponent sidebar = new SidebarComponent();
    SidebarComponentAssertions sidebarAssertions = new SidebarComponentAssertions(sidebar);
    sidebarAssertions.shouldBeVisible();

    sidebar.closeSidebar();
    sidebarAssertions.shouldBeClosed();

    sidebar.openSidebar();
    sidebarAssertions.shouldBeOpen();
  }

  @Test
  @Tag("sanity")
  public void testNavigateToDashboardViaSidebar() {
    // Leave dashboard first so the sidebar click produces a URL change — an SPA may not
    // navigate at all if you click the link for the page you are already on.
    playwrightNavigateTo(OwnerSummaryPage.url(Organization.ROOT_ORGANIZATION_ID));

    SidebarComponent sidebar = new SidebarComponent();
    sidebar.dashboardButton().click();
    PlaywrightWaitUtils.waitForUrl(page, SidebarComponent.expectedDashboardUrlFragment());
  }

  @Test
  @Tag("sanity")
  public void testNavigateToPoliciesViaSidebar() {
    SidebarComponent sidebar = new SidebarComponent();
    sidebar.policiesButton().click();
    PlaywrightWaitUtils.waitForUrl(page, SidebarComponent.expectedPoliciesUrlFragment());
  }

  @Test
  @Tag("sanity")
  public void testHeaderMenuBarIsVisible() {
    HeaderComponent header = new HeaderComponent();
    new HeaderComponentAssertions(header).shouldBeLoggedIn();
    assertThat(header.menuBar()).isVisible();
  }

  @Test
  @Tag("sanity")
  public void testUserMenuShowsUsername() {
    HeaderComponent header = new HeaderComponent();
    header.userMenuDropdownToggle().click();
    assertThat(header.userName()).isVisible();
  }
}
