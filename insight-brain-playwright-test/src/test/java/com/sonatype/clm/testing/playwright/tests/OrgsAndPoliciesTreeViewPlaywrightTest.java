/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.testing.functional.utils.NameSupplierDictionary;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.OwnersTreePage;
import com.sonatype.clm.testing.playwright.pages.OwnersTreePageAssertions;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import org.junit.Before;
import org.junit.Test;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import org.junit.experimental.categories.Category;

/**
 * Playwright migration of the Selenide {@code OrgsAndPoliciesTreeViewLimitedPermissionTest}.
 *
 * <p>
 * Each test follows a Given / When / Then shape:
 * <ul>
 * <li>{@link #seedHierarchyAndLoginAsDeveloper()} seeds a per-test org tree
 * ({@link #ORGS_PER_LEVEL} orgs × {@link #TREE_DEPTH} levels × {@link #APPS_PER_ORG} apps),
 * grants a fresh developer user the developer role on every app that lives directly under a
 * top-level org, and lands on the dashboard logged in as that developer.</li>
 * <li>The test body navigates to the inheritance tree-view and asserts the page renders the
 * permitted subtree, then clicks into the first clickable owner.</li>
 * </ul>
 *
 * <p>
 * Selectors live in {@link OwnersTreePage} (and {@link OwnerSummaryPage} for the destination
 * owner-summary). See {@code PLAYWRIGHT_TEST_AUTHORING_GUIDE.md}.
 */
public class OrgsAndPoliciesTreeViewPlaywrightTest
    extends AbstractIqUiTest
{
  private static final int ORGS_PER_LEVEL = 2;

  private static final int TREE_DEPTH = 3;

  private static final int APPS_PER_ORG = 3;

  private static final String EXPECTED_PAGE_HEADING = "Inheritance Hierarchy";

  private User developerUser;

  private final List<Application> applicationsWithPermission = new ArrayList<>();

  // --------------- @Before ---------------

  @Before
  public void seedHierarchyAndLoginAsDeveloper() {
    seedHierarchyAndDeveloperPermissions();
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLoginAt(DashboardPage.url(),
        developerUser.getUsername(),
        TemporaryEntity.USER_PASSWORD_CLEAR);
  }

  // --------------- @Test methods ---------------

  /**
   * A developer with permission on a subset of applications can navigate to the inheritance
   * tree-view, see at least one owner row, and click into the first clickable owner to land on
   * its owner-summary page.
   */
  @Test
  @Category(SanityTest.class)
  public void testOwnerTree_limitedPermission() {
    OwnersTreePage treePage = new OwnersTreePage();
    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();

    // Given: developer is authenticated and lands on the inheritance tree-view.
    playwrightRefreshOrOpen(OwnersTreePage.url());

    // Then: the page header and at least one tree row render for the permitted subtree.
    new OwnersTreePageAssertions(treePage).shouldBeVisibleWithAtLeastOneItem();
    assertThat(treePage.pageHeading()).hasText(EXPECTED_PAGE_HEADING);

    // When: the user clicks the first clickable owner in the tree.
    treePage.clickFirstClickableOwner();

    // Then: the owner-summary container for that owner is rendered.
    assertThat(ownerSummary.container()).isVisible();
  }

  // --------------- Backend seed methods ---------------

  private void seedHierarchyAndDeveloperPermissions() {
    ApplicationDAO applicationDAO = lookup(ApplicationDAO.class);
    List<Organization> organizations = tempEntity.newRelatedOrganizationsAsList(
        ORGS_PER_LEVEL,
        TREE_DEPTH,
        APPS_PER_ORG,
        new NameSupplierDictionary());

    organizations.stream()
        .filter(org -> Organization.ROOT_ORGANIZATION_ID.equals(org.getParentOrganizationId()))
        .forEach(
            rootChild -> applicationsWithPermission.addAll(applicationDAO.getByOrganizationId(rootChild.getId())));
    developerUser = tempEntity.newUser();
    applicationsWithPermission.forEach(
        app -> tempEntity.newMembershipMapping(app.getId(), Role.DEVELOPER_ROLE_ID, developerUser.getUsername()));
  }

}
