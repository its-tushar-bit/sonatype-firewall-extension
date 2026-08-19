/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.microsoft.playwright.Route;
import com.sonatype.clm.testing.playwright.utils.TestCredentials;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.OrgsAndPoliciesSidebarComponent;
import com.sonatype.clm.testing.playwright.pages.OrgsAndPoliciesSidebarComponentAssertions;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.OwnersTreePage;
import com.sonatype.clm.testing.playwright.pages.OwnersTreePageAssertions;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class OrgsAndPoliciesTreeViewPlaywrightTest
    extends AbstractIqUiTest
{
  private static final int APPS_PER_ORG = 3;

  private static final String EXPECTED_PAGE_HEADING = "Inheritance Hierarchy";

  private static final String SIDEBAR_REST_PATH = "/rest/sidebar";

  private OwnersTreePage treePage;

  private OwnersTreePageAssertions treeAssertions;

  private OwnerSummaryPage ownerSummary;

  private OrgsAndPoliciesSidebarComponent sidebar;

  private OrgsAndPoliciesSidebarComponentAssertions sidebarAssertions;

  private User developerUser;

  private Application adminTreeApp;

  private final List<Application> applicationsWithPermission = new ArrayList<>();

  @BeforeEach
  public void seedHierarchyAndLoginAsDeveloper() {
    treePage = new OwnersTreePage();
    treeAssertions = new OwnersTreePageAssertions(treePage);
    ownerSummary = new OwnerSummaryPage();
    sidebar = new OrgsAndPoliciesSidebarComponent();
    sidebarAssertions = new OrgsAndPoliciesSidebarComponentAssertions(sidebar);

    seedHierarchyAndDeveloperPermissions();
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLoginAt(DashboardPage.url(),
        developerUser.getUsername(),
        TemporaryEntity.USER_PASSWORD_CLEAR);
  }

  @Test
  @Tag("sanity")
  public void testOwnerTree_limitedPermission() {
    playwrightRefreshOrOpen(OwnersTreePage.url());

    treeAssertions.shouldBeVisibleWithAtLeastOneItem();
    assertThat(treePage.pageHeading()).hasText(EXPECTED_PAGE_HEADING);

    treePage.clickFirstClickableOwner();

    assertThat(ownerSummary.container()).isVisible();
  }

  @Test
  @Tag("regression")
  public void testTreeView_expandCollapseAndNavigateAsAdmin() {
    seedAdminTree();

    playwrightLogout();
    playwrightLoginAdminAt(OwnersTreePage.url());

    treeAssertions.shouldBeVisibleWithAtLeastOneItem();

    assertThat(treePage.expandedCollapsibleNodes()).not().hasCount(0);

    treePage.collapseAllButton().click();
    assertThat(treePage.collapsedNodes()).not().hasCount(0);

    treePage.expandAllButton().click();
    assertThat(treePage.collapsedNodes()).hasCount(0);

    treePage.clickFirstOrganizationNode();
    assertThat(ownerSummary.container()).isVisible();

    page.goBack();
    treeAssertions.shouldBeVisibleWithAtLeastOneItem();
    treePage.expandAllButton().click();
    treePage.clickFirstApplicationNode();
    assertThat(ownerSummary.container()).isVisible();
  }

  @Test
  @Tag("regression")
  public void testTreeView_backButtonText_dynamicFromOrgAndFallback() {
    Organization adminOrg = seedAdminTree();

    String orgPublicId = adminOrg.getPublicId();
    String orgName = adminOrg.getName();

    playwrightLogout();
    playwrightLoginAdminAt(OwnerSummaryPage.url(orgPublicId));
    sidebarAssertions.shouldBeVisibleWithSelectedOwner();
    sidebar.openTreeView();

    treeAssertions.shouldBeVisibleWithAtLeastOneItem();
    assertThat(treePage.backButton()).isVisible();
    assertThat(treePage.backButton()).containsText(orgName);
  }

  @Test
  @Tag("regression")
  public void testTreeView_backButtonDestination_fromApplicationPage() {
    seedAdminTree();

    String appPublicId = adminTreeApp.getPublicId();

    playwrightLogout();
    playwrightLoginAdminAt(OwnerSummaryPage.applicationUrl(appPublicId));
    sidebarAssertions.shouldBeVisibleWithSelectedOwner();
    sidebar.openTreeView();

    treeAssertions.shouldBeVisibleWithAtLeastOneItem();
    assertThat(treePage.backButton()).isVisible();
    treePage.backButton().click();
    playwrightWaitUntilUrlContains(OwnerSummaryPage.applicationUrl(appPublicId));
  }

  @Test
  @Tag("regression")
  public void testTreeView_backButtonDestination_sbomAndFirewallContexts() {
    Organization adminOrg = seedAdminTree();

    String orgPublicId = adminOrg.getPublicId();

    playwrightLogout();
    playwrightLoginAdminAt(OwnerSummaryPage.sbomUrl(orgPublicId));
    if (sidebar.container().isVisible() && sidebar.isTreeViewLinkVisible()) {
      sidebar.openTreeView();
      assertThat(treePage.backButton()).isVisible();
      treePage.backButton().click();
      playwrightWaitUntilUrlContains(OwnerSummaryPage.sbomUrl(orgPublicId));
    }

    playwrightRefreshOrOpen(OwnerSummaryPage.firewallUrl(orgPublicId));
    if (sidebar.container().isVisible() && sidebar.isTreeViewLinkVisible()) {
      sidebar.openTreeView();
      assertThat(treePage.backButton()).isVisible();
      treePage.backButton().click();
      playwrightWaitUntilUrlContains(OwnerSummaryPage.firewallUrl(orgPublicId));
    }
  }

  @Test
  @Tag("regression")
  public void testTreeView_loadError_retryReloadsTree() {
    playwrightLogout();
    page.route(Pattern.compile(".*" + SIDEBAR_REST_PATH + ".*"),
        route -> route.fulfill(new Route.FulfillOptions().setStatus(500)));
    playwrightLoginAt(OwnersTreePage.url(),
        TestCredentials.ADMIN_USERNAME, TestCredentials.ADMIN_PASSWORD);

    treeAssertions.shouldShowLoadError();

    page.unrouteAll();
    treePage.retryButton().click();
    treeAssertions.shouldShowTreeContent();
  }

  private void seedHierarchyAndDeveloperPermissions() {
    String suffix = TemporaryEntity.uuid();
    developerUser = tempEntity.newUser();
    Organization topOrg = tempEntity.newOrganization("TreeTestOrg-" + suffix);
    for (int i = 0; i < APPS_PER_ORG; i++) {
      Application app = tempEntity.newApplication(
          "TreeApp-" + suffix + "-" + i, "tree-app-" + suffix + "-" + i, topOrg.getId());
      applicationsWithPermission.add(app);
      tempEntity.newMembershipMapping(
          app.getId(), Role.DEVELOPER_ROLE_ID, developerUser.getUsername());
    }
  }

  private Organization seedAdminTree() {
    String suffix = TemporaryEntity.uuid();
    Organization org = tempEntity.newOrganization("AdminTreeOrg-" + suffix);
    adminTreeApp = tempEntity.newApplication("AdminTreeApp-" + suffix, "admin-tree-app-" + suffix, org.getId());
    return org;
  }

}
