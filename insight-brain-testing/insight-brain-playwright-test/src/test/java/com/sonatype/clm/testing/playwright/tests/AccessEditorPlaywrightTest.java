/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.AccessEditorPage;
import com.sonatype.clm.testing.playwright.pages.AccessEditorPageAssertions;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPageAssertions;
import com.sonatype.clm.testing.playwright.utils.TestCredentials;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Role;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/** Playwright regression tests for the Access editor page. */
public class AccessEditorPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String ORG_NAME = "Access Editor Test Org";

  private AccessEditorPage editorPage;

  private AccessEditorPageAssertions assertions;

  private OwnerSummaryPageAssertions ownerSummaryAssertions;

  @Before
  public void openDashboardAndLoginAsAdmin() {
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
    editorPage = new AccessEditorPage();
    assertions = new AccessEditorPageAssertions(editorPage);
    ownerSummaryAssertions = new OwnerSummaryPageAssertions(new OwnerSummaryPage());
  }

  /** Verify both deletion paths: Delete button and empty-members submit both open a confirmation modal. */
  @Test
  @Category(RegressionTest.class)
  public void testDeleteRoleAssignment_confirmationModalAndNavigatesAway() {
    Organization org = tempEntity.newOrganization(ORG_NAME);
    tempEntity.newMembershipMapping(org.getId(), Role.DEVELOPER_ROLE_ID, TestCredentials.ADMIN_USERNAME);

    navigateAndWaitForUrl(
        AccessEditorPage.editAccessUrl(org.getId(), Role.DEVELOPER_ROLE_ID),
        AccessEditorPage.ADD_ACCESS_URL_FRAGMENT + "/");
    assertions.shouldBeVisible();
    assertions.shouldBeInEditMode(RoleDAO.DEVELOPER);

    // Path 1: Delete button → modal opens → Cancel closes without deleting
    editorPage.clickDelete();
    assertions.shouldShowDeleteModal();
    editorPage.cancelDeleteModal();
    assertions.shouldNotShowDeleteModal();
    assertions.shouldBeInEditMode(RoleDAO.DEVELOPER);

    // Path 2: Remove all members → Update triggers modal → Cancel → re-submit → Continue deletes
    editorPage.removeAssociatedMember(TestCredentials.ADMIN_DISPLAY_NAME);
    editorPage.submit();
    assertions.shouldShowDeleteModal();
    editorPage.cancelDeleteModal();
    assertions.shouldNotShowDeleteModal();

    editorPage.submit();
    assertions.shouldShowDeleteModal();
    editorPage.confirmDeleteModal();

    // Use playwrightRefreshOrOpen rather than navigateAndWaitForUrl: ORG_URL_FRAGMENT also matches
    // the access editor URL (.../organization/{id}/access/{roleId}), so the fragment check would
    // return immediately while the browser is still on the editor page after modal confirmation.
    playwrightRefreshOrOpen(OwnerSummaryPage.url(org.getId()));
    ownerSummaryAssertions.shouldNotHaveLocalAccessRole(RoleDAO.DEVELOPER);
  }

  /** Verify New Role mode (new assignment) and Edit Role mode (existing assignment) render correctly. */
  @Test
  @Category(RegressionTest.class)
  public void testNewMode_showsNewRoleHeading_editMode_showsEditRoleHeading() {
    Organization org = tempEntity.newOrganization(ORG_NAME);

    navigateAndWaitForUrl(AccessEditorPage.newAccessUrl(org.getId()), AccessEditorPage.ADD_ACCESS_URL_FRAGMENT);
    assertions.shouldBeVisible();
    assertions.shouldBeInNewMode();

    tempEntity.newMembershipMapping(org.getId(), Role.DEVELOPER_ROLE_ID, TestCredentials.ADMIN_USERNAME);

    navigateAndWaitForUrl(
        AccessEditorPage.editAccessUrl(org.getId(), Role.DEVELOPER_ROLE_ID),
        AccessEditorPage.ADD_ACCESS_URL_FRAGMENT + "/");
    assertions.shouldBeVisible();
    assertions.shouldBeInEditMode(RoleDAO.DEVELOPER);
  }

  /** Verify the transfer-list add/remove flow updates the Associated Members list and footer count. */
  @Test
  @Category(RegressionTest.class)
  public void testTransferList_addMember_appearsInAssociatedMembers() {
    Organization org = tempEntity.newOrganization(ORG_NAME);

    navigateAndWaitForUrl(AccessEditorPage.newAccessUrl(org.getId()), AccessEditorPage.ADD_ACCESS_URL_FRAGMENT);
    assertions.shouldBeVisible();

    editorPage.searchAndSelectUser(TestCredentials.ADMIN_DISPLAY_NAME);
    assertions.shouldHaveAssociatedMember(TestCredentials.ADMIN_DISPLAY_NAME);
    assertions.shouldHaveTransferListFooter("1 User and 0 Groups Added");

    editorPage.removeAssociatedMember(TestCredentials.ADMIN_DISPLAY_NAME);
    assertions.shouldHaveTransferListFooter("0 Users and 0 Groups Added");
  }

  /** Verify wildcard search returns matching users including the built-in admin account. */
  @Test
  @Category(RegressionTest.class)
  public void testSearchInput_returnsMatchingUsers() {
    Organization org = tempEntity.newOrganization(ORG_NAME);

    navigateAndWaitForUrl(AccessEditorPage.newAccessUrl(org.getId()), AccessEditorPage.ADD_ACCESS_URL_FRAGMENT);
    assertions.shouldBeVisible();

    editorPage.searchFor("*");

    assertions.shouldShowSearchResultContaining(TestCredentials.ADMIN_USERNAME);
  }

  /** Verify that clicking Update without changes triggers the "no changes to save" validation message. */
  @Test
  @Category(RegressionTest.class)
  public void testEditMode_noChangesMade_submitShowsValidationError() {
    Organization org = tempEntity.newOrganization(ORG_NAME);
    tempEntity.newMembershipMapping(org.getId(), Role.DEVELOPER_ROLE_ID, TestCredentials.ADMIN_USERNAME);

    navigateAndWaitForUrl(
        AccessEditorPage.editAccessUrl(org.getId(), Role.DEVELOPER_ROLE_ID),
        AccessEditorPage.ADD_ACCESS_URL_FRAGMENT + "/");
    assertions.shouldBeVisible();
    assertions.shouldBeInEditMode(RoleDAO.DEVELOPER);

    editorPage.submit();
    assertions.shouldShowValidationErrors();
  }

  /**
   * Verify that attempting to submit in new mode with a role selected but no members added
   * triggers a form validation error and blocks the save.
   */
  @Test
  @Category(RegressionTest.class)
  public void testNewMode_withRoleSelected_noMembersAdded_submitShowsValidationError() {
    Organization org = tempEntity.newOrganization(ORG_NAME);

    navigateAndWaitForUrl(AccessEditorPage.newAccessUrl(org.getId()), AccessEditorPage.ADD_ACCESS_URL_FRAGMENT);
    assertions.shouldBeVisible();
    assertions.shouldBeInNewMode();

    editorPage.selectRole(RoleDAO.DEVELOPER);
    editorPage.submit();

    assertions.shouldShowValidationErrors();
  }
}
