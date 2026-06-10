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

/**
 * Playwright regression tests for the Access editor page.
 * <p>
 * Covers two distinct rendering modes driven by the {@code isNew} flag in Redux state:
 * <ul>
 * <li><b>New mode</b> ({@code …/access}): "New Role" heading, role dropdown shown, "Create" submit button.</li>
 * <li><b>Edit mode</b> ({@code …/access/{roleId}}): "Edit Role" heading, role-name subtitle, no dropdown,
 * "Update" submit button.</li>
 * </ul>
 */
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

  /**
   * Verify both deletion paths for a role assignment.
   * <p>
   * <b>Path 1 (Delete button):</b> clicking Delete opens {@code DeleteAccessModal}; Cancel closes
   * the modal without deleting.
   * <br>
   * <b>Path 2 (empty-members submit):</b> removing all members and clicking Update opens the same
   * modal; Cancel closes it; Continue confirms deletion and the role is absent from the Access tile.
   */
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

  /**
   * Verify that the Access editor renders in "New Role" mode when opened for a new assignment,
   * and switches to "Edit Role" mode when opened for an existing role assignment.
   */
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

  /**
   * Verify the transfer-list add/remove flow.
   * Searching and selecting a user moves them to "Associated Members" and updates the footer count.
   * Clicking the remove control on that user removes them and decrements the footer count.
   */
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

  /**
   * Verify that the "Search Users and Groups" input triggers a debounced search and the results
   * dropdown shows matching users. Wildcard {@code *} returns all users, including the built-in
   * admin account.
   */
  @Test
  @Category(RegressionTest.class)
  public void testSearchInput_returnsMatchingUsers() {
    Organization org = tempEntity.newOrganization(ORG_NAME);

    navigateAndWaitForUrl(AccessEditorPage.newAccessUrl(org.getId()), AccessEditorPage.ADD_ACCESS_URL_FRAGMENT);
    assertions.shouldBeVisible();

    editorPage.searchFor("*");

    assertions.shouldShowSearchResultContaining(TestCredentials.ADMIN_USERNAME);
  }

  /**
   * Verify that clicking "Update" without making any changes triggers the
   * {@code MSG_NO_CHANGES_TO_SAVE} validation message.
   * <p>
   * {@code AccessPage} passes {@code MSG_NO_CHANGES_TO_SAVE} ("There are no changes to save.")
   * as {@code validationErrors} to {@code NxStatefulForm} whenever {@code isDirty === false}.
   * {@code NxStatefulForm} blocks the submit and renders the {@code .nx-form__validation-errors}
   * alert only after a submit is attempted, so the test must click Update first.
   */
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
  public void testNewMode_withRoleSelected_noMembersAdded_submitButtonDisabled() {
    Organization org = tempEntity.newOrganization(ORG_NAME);

    navigateAndWaitForUrl(AccessEditorPage.newAccessUrl(org.getId()), AccessEditorPage.ADD_ACCESS_URL_FRAGMENT);
    assertions.shouldBeVisible();
    assertions.shouldBeInNewMode();

    editorPage.selectRole(RoleDAO.DEVELOPER);
    editorPage.submit();

    assertions.shouldShowValidationErrors();
  }
}
