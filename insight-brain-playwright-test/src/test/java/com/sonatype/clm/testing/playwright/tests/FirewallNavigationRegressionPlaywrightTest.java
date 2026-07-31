/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.util.regex.Pattern;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.AdministratorsPage;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.OrgsAndPoliciesSidebarComponent;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.RolesPage;
import com.sonatype.clm.testing.playwright.pages.SidebarComponent;
import com.sonatype.clm.testing.playwright.pages.UserManagementPage;
import com.sonatype.clm.testing.playwright.pages.WebhookEditorPage;
import com.sonatype.clm.testing.playwright.pages.WebhookListPage;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;

/**
 * Regression tests for Firewall-specific navigation: the "Repos and Policies" sidebar button,
 * the owner management tree, and Firewall system-preference pages
 * (Users, Roles, Administrators, Webhooks).
 */
public class FirewallNavigationRegressionPlaywrightTest
    extends AbstractIqUiTest
{
  @Before
  public void loginToDashboard() {
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
  }

  @Test
  @Category(RegressionTest.class)
  public void testFirewallReposAndPoliciesSidebar_displaysOwnerManagementTree() {
    String firewallMgmtUrl = OwnerSummaryPage.firewallUrl(ROOT_ORGANIZATION_ID);
    navigateAndWaitForUrl(firewallMgmtUrl, routeOf(firewallMgmtUrl));

    SidebarComponent sidebar = new SidebarComponent();
    OrgsAndPoliciesSidebarComponent ownerSidebar = new OrgsAndPoliciesSidebarComponent();

    assertThat(sidebar.firewallRepositoriesButton()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    sidebar.firewallRepositoriesButton().click();

    assertThat(page).hasURL(Pattern.compile(".*" + routeOf(firewallMgmtUrl) + ".*"));
    assertThat(ownerSidebar.container()).isVisible(PlaywrightTiming.VISIBLE_OPTS);

    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    assertThat(ownerSummary.policiesTile()).isVisible();
    assertThat(ownerSummary.accessTile()).isVisible();
    assertThat(ownerSummary.sourceControlTile()).isVisible();
    assertThat(ownerSummary.autoWaiversTile()).isVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testFirewallSystemPreferences_usersRolesAdminsWebhooksPagesAccessible() {
    navigateAndWaitForUrl(UserManagementPage.firewallUrl(), routeOf(UserManagementPage.firewallUrl()));
    assertThat(new UserManagementPage().container()).isVisible(PlaywrightTiming.VISIBLE_OPTS);

    navigateAndWaitForUrl(RolesPage.firewallUrl(), routeOf(RolesPage.firewallUrl()));
    assertThat(new RolesPage().container()).isVisible(PlaywrightTiming.VISIBLE_OPTS);

    navigateAndWaitForUrl(AdministratorsPage.firewallUrl(), routeOf(AdministratorsPage.firewallUrl()));
    assertThat(new AdministratorsPage().container()).isVisible(PlaywrightTiming.VISIBLE_OPTS);

    navigateAndWaitForUrl(WebhookListPage.firewallUrl(), routeOf(WebhookListPage.firewallUrl()));
    assertThat(new WebhookListPage().container()).isVisible(PlaywrightTiming.VISIBLE_OPTS);

    navigateAndWaitForUrl(WebhookEditorPage.firewallCreateUrl(), routeOf(WebhookEditorPage.firewallCreateUrl()));
    WebhookEditorPage webhookEditor = new WebhookEditorPage();
    assertThat(webhookEditor.eventTypesFieldset()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    assertThat(webhookEditor.eventTypeCheckbox("Policy Evaluation")).isVisible();
  }

  private static String routeOf(String pageUrl) {
    int idx = pageUrl.indexOf("#/");
    return idx >= 0 ? pageUrl.substring(idx + 2) : pageUrl;
  }
}
