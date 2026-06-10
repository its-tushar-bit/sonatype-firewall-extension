/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.nio.file.Paths;
import java.util.List;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPageAssertions;
import com.sonatype.clm.testing.playwright.pages.OwnersTreePage;
import com.sonatype.clm.testing.playwright.pages.OwnersTreePageAssertions;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Regression tests for the Owner Summary page – Actions dropdown modal flows.
 * <p>
 * This class is fully independent of {@link OrganizationPlaywrightTest} and {@code organization.json}.
 */
public class OwnerSummaryActionsPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String CHANGE_APP_ID_ORG_NAME = "Change App ID Test Org";

  private static final String CHANGE_APP_ID_ORIGINAL_NAME = "Change ID App";

  private static final String CHANGE_APP_ID_ORIGINAL = "change-id-app-orig";

  private static final String CHANGE_APP_ID_DUPLICATE = "change-id-app-dup";

  private static final String CHANGE_APP_ID_NEW_ID = "change-id-app-new";

  private static final String EVALUATE_FILE_ORG_NAME = "Evaluate File Test Org";

  private static final String EVALUATE_FILE_APP_NAME = "Evaluate File App";

  private static final String EVALUATE_FILE_APP_PUBLIC_ID = "evaluate-file-app";

  private static final String EVALUATE_FILE_RESOURCE = "test-data/evaluate-file/xml-apis-1.4.01.jar";

  private static final String IMPORT_POLICIES_ORG_NAME = "Import Policies Org";

  private static final String SELECT_CONTACT_ORG_NAME = "Select Contact Org";

  private static final String SELECT_CONTACT_APP_NAME = "Select Contact App";

  private static final String SELECT_CONTACT_APP_PUBLIC_ID = "select-contact-app";

  private static final String SELECT_CONTACT_USER_NAME = "select-contact-user";

  private static final String SBOM_MODE_ORG_NAME = "SBOM Mode Org";

  private static final String SBOM_MODE_APP_NAME = "SBOM Mode App";

  private static final String SBOM_MODE_APP_PUBLIC_ID = "sbom-mode-app-pw";

  private static final String CANCEL_DELETE_ORG_NAME = "Cancel Delete Test Org";

  private static final String CANCEL_DELETE_APP_ORG_NAME = "Cancel Delete App Test Org";

  private static final String CANCEL_DELETE_APP_NAME = "Cancel Delete Test App";

  private static final String CANCEL_DELETE_APP_PUBLIC_ID = "cancel-delete-test-app-pw";

  private static final String DELETE_APP_ORG_NAME = "Delete App Test Org";

  private static final String DELETE_APP_NAME = "Delete Test App";

  private static final String DELETE_APP_PUBLIC_ID = "delete-test-app-pw";

  private static final String CONFIRM_MOVE_SOURCE_ORG_NAME = "Confirm Move Source Org";

  private static final String CONFIRM_MOVE_TARGET_ORG_NAME = "Confirm Move Target Org";

  private static final String MOVE_APP_ORG_NAME = "Move App Test Org";

  private static final String MOVE_APP_NAME = "Move App Test";

  private static final String MOVE_APP_PUBLIC_ID = "move-app-test";

  private static final String MOVE_APP_TARGET_ORG_NAME = "Move App Target Org";

  private static final String IMPORT_PREVIEW_ORG_NAME = "Import Preview Org";

  private static final String IMPORT_CONFIRM_ORG_NAME = "Import Confirm Org";

  private static final String IMPORT_POLICIES_RESOURCE = "reference-policies-v3.json";

  private static final String SELECT_CONTACT_SAVE_ORG_NAME = "Select Contact Save Org";

  private static final String SELECT_CONTACT_SAVE_APP_NAME = "Select Contact Save App";

  private static final String SELECT_CONTACT_SAVE_APP_PUBLIC_ID = "select-contact-save-app";

  private static final String SELECT_CONTACT_SAVE_USER_NAME = "select-contact-save-user";

  private static final String SELECT_CONTACT_SAVE_USER_FIRST_NAME = "ScUserFirst";

  private static final String SELECT_CONTACT_SAVE_USER_LAST_NAME = "ScUserLast";

  private static final String SELECT_CONTACT_SAVE_USER_DISPLAY_NAME =
      SELECT_CONTACT_SAVE_USER_FIRST_NAME + " " + SELECT_CONTACT_SAVE_USER_LAST_NAME;

  private static final String INVALID_APP_ID_WITH_SPACES = "invalid id with spaces";

  private static final String COPY_ORG_ID_ORG_NAME = "Copy Org ID Test Org";

  private static final List<String> CLIPBOARD_PERMISSIONS = List.of("clipboard-read", "clipboard-write");

  private static final String DUPLICATE_APP_ID_ORG_NAME = "Duplicate App ID Test Org";

  private static final String DUPLICATE_APP_ID_EXISTING_APP_NAME = "Duplicate App Existing";

  private static final String DUPLICATE_APP_ID_EXISTING_PUBLIC_ID = "duplicate-app-id-pw";

  private static final String DUPLICATE_APP_ID_NEW_APP_NAME = "Duplicate App New";

  private static final String SYNTHETIC_ORG_NAME = "Synthetic Org PW";

  private static final String SYNTHETIC_ORG_APP_NAME = "Synthetic Org Child App";

  private static final String SYNTHETIC_ORG_APP_PUBLIC_ID = "synthetic-org-child-app-pw";

  private static final String SYNTHETIC_ORG_LIMITED_USER_NAME = "synthetic-org-limited-user-pw";

  private static final String SYNTHETIC_ORG_LIMITED_USER_FIRST_NAME = "SynLimitedFirst";

  private static final String SYNTHETIC_ORG_LIMITED_USER_LAST_NAME = "SynLimitedLast";

  private static final String TEST_EMAIL_DOMAIN = "@void.com";

  private static final String SYNTHETIC_ORG_EXPECTED_DESCRIPTION =
      "View all organizations and applications on which you have permissions. "
          + "Click on the link for the org or app below to access details.";

  private OwnerSummaryPage ownerSummary;

  private OwnerSummaryPageAssertions assertions;

  private OwnersTreePage ownersTree;

  private OwnersTreePageAssertions ownersTreeAssertions;

  @Before
  public void openDashboardAndLoginAsAdmin() {
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
    ownerSummary = new OwnerSummaryPage();
    assertions = new OwnerSummaryPageAssertions(ownerSummary);
    ownersTree = new OwnersTreePage();
    ownersTreeAssertions = new OwnersTreePageAssertions(ownersTree);
  }

  @After
  public void resetSbomContinuousMonitoringFlag() {
    SystemConfigurationPropertyFeature.SBOM_CONTINUOUS_MONITORING_UI.setEnabled(false);
  }

  /**
   * Cancel closes the modal without deleting the org (URL stays on org summary); a subsequent
   * confirm deletes the org and it no longer appears in the owners tree.
   */

  @Test
  @Category(RegressionTest.class)
  public void testDeleteOrganization_cancelThenConfirm() {
    Organization org = tempEntity.newOrganization(CANCEL_DELETE_ORG_NAME);

    navigateAndWaitForUrl(OwnerSummaryPage.url(org.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerActionsDropdown();
    ownerSummary.clickDeleteOwnerMenuItem();
    assertions.shouldShowDeleteOwnerModal();

    ownerSummary.cancelDeleteOwnerModal();
    assertions.shouldHaveUrlContaining(OwnerSummaryPage.ORG_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerActionsDropdown();
    ownerSummary.clickDeleteOwnerMenuItem();
    assertions.shouldShowDeleteOwnerModal();

    ownerSummary.confirmDeleteOwnerModal();
    assertions.shouldHaveUrlContaining(OwnerSummaryPage.ROOT_ORG_URL_FRAGMENT);

    playwrightNavigateTo(OwnersTreePage.url());
    ownersTreeAssertions.shouldNotContainItemWithText(CANCEL_DELETE_ORG_NAME);
  }

  /**
   * Cancel closes the modal without deleting the app (URL stays on app summary); a subsequent
   * confirm deletes the app and it no longer appears in the owners tree.
   */
  @Test
  @Category(RegressionTest.class)
  public void testDeleteApplication_cancelThenConfirm() {
    Organization parent = tempEntity.newOrganization(CANCEL_DELETE_APP_ORG_NAME);
    tempEntity.newApplication(CANCEL_DELETE_APP_NAME, CANCEL_DELETE_APP_PUBLIC_ID, parent.getId());

    navigateAndWaitForUrl(OwnerSummaryPage.applicationUrl(CANCEL_DELETE_APP_PUBLIC_ID),
        OwnerSummaryPage.APP_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerActionsDropdown();
    ownerSummary.clickDeleteOwnerMenuItem();
    assertions.shouldShowDeleteOwnerModal();

    ownerSummary.cancelDeleteOwnerModal();
    assertions.shouldHaveUrlContaining(OwnerSummaryPage.APP_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerActionsDropdown();
    ownerSummary.clickDeleteOwnerMenuItem();
    assertions.shouldShowDeleteOwnerModal();

    ownerSummary.confirmDeleteOwnerModal();
    assertions.shouldHaveUrlContaining(OwnerSummaryPage.ORG_URL_FRAGMENT);

    playwrightNavigateTo(OwnersTreePage.url());
    ownersTreeAssertions.shouldNotContainItemWithText(CANCEL_DELETE_APP_NAME);
  }

  /**
   * Cancel closes the modal without moving the org (URL stays on org summary); a subsequent
   * confirm moves the org and the summary remains accessible at its original URL.
   */
  @Test
  @Category(RegressionTest.class)
  public void testMoveOrganization_cancelThenConfirm() {
    Organization sourceOrg = tempEntity.newOrganization(CONFIRM_MOVE_SOURCE_ORG_NAME);
    tempEntity.newOrganization(CONFIRM_MOVE_TARGET_ORG_NAME);

    navigateAndWaitForUrl(OwnerSummaryPage.url(sourceOrg.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerActionsDropdown();
    ownerSummary.clickMoveOwnerMenuItem();
    assertions.shouldShowMoveOwnerModal();

    ownerSummary.cancelMoveOwnerModal();
    assertions.shouldHaveUrlContaining(OwnerSummaryPage.ORG_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerActionsDropdown();
    ownerSummary.clickMoveOwnerMenuItem();
    assertions.shouldShowMoveOwnerModal();
    ownerSummary.selectMoveTarget(CONFIRM_MOVE_TARGET_ORG_NAME);
    ownerSummary.confirmAndWaitForMoveOwnerModalToClose();

    assertions.shouldBeVisible();
  }

  /**
   * Cancel closes the modal without moving the app (URL stays on app summary); a subsequent
   * confirm moves the app to the target org and the application summary remains accessible.
   */
  @Test
  @Category(RegressionTest.class)
  public void testMoveApplication_cancelThenConfirm() {
    Organization parent = tempEntity.newOrganization(MOVE_APP_ORG_NAME);
    tempEntity.newOrganization(MOVE_APP_TARGET_ORG_NAME);
    tempEntity.newApplication(MOVE_APP_NAME, MOVE_APP_PUBLIC_ID, parent.getId());

    navigateAndWaitForUrl(OwnerSummaryPage.applicationUrl(MOVE_APP_PUBLIC_ID), OwnerSummaryPage.APP_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerActionsDropdown();
    ownerSummary.clickMoveOwnerMenuItem();
    assertions.shouldShowMoveOwnerModal();

    ownerSummary.cancelMoveOwnerModal();
    assertions.shouldHaveUrlContaining(OwnerSummaryPage.APP_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerActionsDropdown();
    ownerSummary.clickMoveOwnerMenuItem();
    assertions.shouldShowMoveOwnerModal();
    ownerSummary.selectMoveTarget(MOVE_APP_TARGET_ORG_NAME);
    ownerSummary.confirmAndWaitForMoveOwnerModalToClose();

    assertions.shouldBeVisible();
  }

  /**
   * All Change App ID scenarios in one modal session:
   * 1) Invalid-format ID (spaces) surfaces the NxStatefulForm validation-errors banner.
   * 2) Already-used (duplicate) ID surfaces the validation-errors banner.
   * 3) A valid unique new ID submits successfully and the browser URL reflects the new ID.
   * Single seed, single login.
   */
  @Test
  @Category(RegressionTest.class)
  public void testChangeApplicationId_validationAndSuccess() {
    Organization parent = tempEntity.newOrganization(CHANGE_APP_ID_ORG_NAME);
    tempEntity.newApplication(CHANGE_APP_ID_ORIGINAL_NAME, CHANGE_APP_ID_ORIGINAL, parent.getId());
    // Sibling app whose public ID is used to trigger the duplicate-ID validation check.
    tempEntity.newApplication(CHANGE_APP_ID_ORIGINAL_NAME + " Dup", CHANGE_APP_ID_DUPLICATE, parent.getId());

    navigateAndWaitForUrl(OwnerSummaryPage.applicationUrl(CHANGE_APP_ID_ORIGINAL), OwnerSummaryPage.APP_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerActionsDropdown();
    ownerSummary.clickChangeAppIdMenuItem();
    assertions.shouldShowChangeAppIdModal();
    ownerSummary.typeNewApplicationId(INVALID_APP_ID_WITH_SPACES);
    assertions.shouldShowChangeAppIdValidationError();
    ownerSummary.cancelChangeAppIdModal();

    ownerSummary.openOwnerActionsDropdown();
    ownerSummary.clickChangeAppIdMenuItem();
    assertions.shouldShowChangeAppIdModal();
    ownerSummary.typeNewApplicationId(CHANGE_APP_ID_DUPLICATE);
    assertions.shouldShowChangeAppIdValidationError();
    ownerSummary.cancelChangeAppIdModal();

    ownerSummary.openOwnerActionsDropdown();
    ownerSummary.clickChangeAppIdMenuItem();
    assertions.shouldShowChangeAppIdModal();
    ownerSummary.typeNewApplicationId(CHANGE_APP_ID_NEW_ID);
    ownerSummary.confirmChangeAppIdModal();
    assertions.shouldHaveUrlContaining(OwnerSummaryPage.APP_URL_FRAGMENT + CHANGE_APP_ID_NEW_ID);
    assertions.shouldBeVisible();
    assertions.shouldShowOwnerName(CHANGE_APP_ID_ORIGINAL_NAME);
  }

  /**
   * "Evaluate a File" action opens the file-upload modal with a stage selector and Upload button
   * visible; uploading a valid file triggers evaluation and shows the Evaluation Status modal
   * with a progress bar.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEvaluateFile_opensModal() throws Exception {
    Organization parent = tempEntity.newOrganization(EVALUATE_FILE_ORG_NAME);
    tempEntity.newApplication(EVALUATE_FILE_APP_NAME, EVALUATE_FILE_APP_PUBLIC_ID, parent.getId());

    navigateAndWaitForUrl(OwnerSummaryPage.applicationUrl(EVALUATE_FILE_APP_PUBLIC_ID),
        OwnerSummaryPage.APP_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerActionsDropdown();
    ownerSummary.clickEvaluateFileMenuItem();
    assertions.shouldShowEvaluateFileModal();

    ownerSummary.selectEvaluateFileStage(StageTypes.BUILD.getId());
    ownerSummary.uploadEvaluateFile(
        Paths.get(getClass().getClassLoader().getResource(EVALUATE_FILE_RESOURCE).toURI()));
    ownerSummary.submitEvaluateFileModal();

    assertions.shouldShowEvaluationStatusModal();
    ownerSummary.closeEvaluationStatusModal();
  }

  /**
   * "Import Policies" action opens the file-upload modal with a JSON file input and Import button
   * visible; cancelling closes the modal without importing.
   */
  @Test
  @Category(RegressionTest.class)
  public void testImportPolicies_opensModal() {
    Organization org = tempEntity.newOrganization(IMPORT_POLICIES_ORG_NAME);

    navigateAndWaitForUrl(OwnerSummaryPage.url(org.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerActionsDropdown();
    ownerSummary.clickImportPoliciesMenuItem();
    assertions.shouldShowImportPolicyModal();

    ownerSummary.cancelImportPolicyModal();
  }

  /**
   * "Select Contact" action on an application opens the user-search modal with a text input and
   * Save button visible; cancelling closes the modal without saving.
   */
  @Test
  @Category(RegressionTest.class)
  public void testSelectContact_opensModal() {
    Organization parent = tempEntity.newOrganization(SELECT_CONTACT_ORG_NAME);
    tempEntity.newApplication(SELECT_CONTACT_APP_NAME, SELECT_CONTACT_APP_PUBLIC_ID, parent.getId());
    tempEntity.newUser(SELECT_CONTACT_USER_NAME);

    navigateAndWaitForUrl(OwnerSummaryPage.applicationUrl(SELECT_CONTACT_APP_PUBLIC_ID),
        OwnerSummaryPage.APP_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerActionsDropdown();
    ownerSummary.clickSelectContactMenuItem();
    assertions.shouldShowSelectContactModal();

    ownerSummary.cancelSelectContactModal();
  }

  /**
   * In SBOM Manager product context the organization owner summary renders only the SBOM-relevant
   * tiles (Policies, Continuous monitoring, Access) and omits Lifecycle-only tiles.
   * Skipped automatically when the server does not carry an SBOM Manager license.
   */
  @Test
  @Category(RegressionTest.class)
  public void testSbomManagerMode_limitsTiles() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    setFeatures(LicensedFeature.SBOM_MANAGER, LicensedFeature.POLICY_MONITORING);
    SystemConfigurationPropertyFeature.SBOM_CONTINUOUS_MONITORING_UI.setEnabled(true);

    Organization org = tempEntity.newOrganization(SBOM_MODE_ORG_NAME);
    tempEntity.newApplication(SBOM_MODE_APP_NAME, SBOM_MODE_APP_PUBLIC_ID, org.getId());

    // Hard-reset + blank clears session so the SPA fully reinitialises with the new SBOM license.
    playwrightHardresetToBlank();
    playwrightRefreshOrOpen(OwnerSummaryPage.sbomManagerUrl(org.getId()));
    playwrightLogin();
    assertions.shouldHaveUrlContaining(OwnerSummaryPage.SBOM_ORG_URL_FRAGMENT);

    assertions.shouldBeVisible();
    assertions.shouldShowOnlySbomTiles();

    playwrightNavigateTo(OwnerSummaryPage.sbomManagerAppUrl(SBOM_MODE_APP_PUBLIC_ID));
    assertions.shouldHaveUrlContaining(OwnerSummaryPage.APP_URL_FRAGMENT);
    assertions.shouldBeVisible();
    assertions.shouldShowOnlySbomAppTiles();
  }

  /**
   * Delete Application opens the confirmation modal; confirming deletes the app and navigates
   * back to the parent org summary.
   */
  @Test
  @Category(RegressionTest.class)
  public void testDeleteApplication_confirmationAndNavigation() {
    Organization org = tempEntity.newOrganization(DELETE_APP_ORG_NAME);
    tempEntity.newApplication(DELETE_APP_NAME, DELETE_APP_PUBLIC_ID, org.getId());

    navigateAndWaitForUrl(OwnerSummaryPage.applicationUrl(DELETE_APP_PUBLIC_ID), OwnerSummaryPage.APP_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerActionsDropdown();
    ownerSummary.clickDeleteOwnerMenuItem();
    assertions.shouldShowDeleteOwnerModal();
    ownerSummary.confirmDeleteOwnerModal();

    assertions.shouldHaveUrlContaining(OwnerSummaryPage.ORG_URL_FRAGMENT + org.getId());
  }

  /**
   * On servers without the {@code custom-policies} enterprise entitlement, the Import Policies
   * menu item opens a preview-only modal (Enterprise Feature tag, file upload, Close button only —
   * no Import submit button). Always runs in non-enterprise mode by removing the entitlement
   * before the SPA loads.
   */
  @Test
  @Category(RegressionTest.class)
  public void testImportPolicies_previewModeOnNonEnterprise() {
    Organization org = tempEntity.newOrganization(IMPORT_PREVIEW_ORG_NAME);

    // custom-policies in this test. AbstractIqUiTest @After resets the license automatically.
    setMissingFeature(LicensedFeature.CUSTOM_POLICIES);

    // features during reinstall; React's route guard then synchronously redirects on the hash change.
    navigateAndWaitForUrl(OwnerSummaryPage.url(org.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);

    // Hard reload clears the Redux store: the prior navigate was a hash-only change (same index.html
    // as the @Before dashboard URL), so the SPA was not reset and still has features with custom-policies.
    playwrightRefresh();
    assertions.shouldHaveUrlContaining(OwnerSummaryPage.ORG_URL_FRAGMENT);

    assertions.shouldBeVisible();

    ownerSummary.openOwnerActionsDropdown();
    assertions.shouldShowImportPoliciesMenuItemAsPreview();

    ownerSummary.clickImportPoliciesMenuItem();
    assertions.shouldShowImportPoliciesPreviewModal();
    ownerSummary.closeImportPoliciesPreviewModal();
  }

  /**
   * On servers with the {@code custom-policies} enterprise entitlement, uploading a valid policies
   * JSON file shows the file badge (preview) and clicking Import saves the policies and closes the
   * modal. Skipped automatically on non-enterprise servers.
   */
  @Test
  @Category(RegressionTest.class)
  public void testImportPolicies_uploadAndConfirm() throws Exception {
    Organization org = tempEntity.newOrganization(IMPORT_CONFIRM_ORG_NAME);

    navigateAndWaitForUrl(OwnerSummaryPage.url(org.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerActionsDropdown();
    ownerSummary.clickImportPoliciesMenuItem();
    assertions.shouldShowImportPolicyModal();

    ownerSummary.uploadImportPoliciesFile(
        Paths.get(getClass().getClassLoader().getResource(IMPORT_POLICIES_RESOURCE).toURI()));
    assertions.shouldShowImportPoliciesFileSelected(IMPORT_POLICIES_RESOURCE);

    ownerSummary.submitImportPoliciesModal();
    assertions.shouldBeVisible();
  }

  /**
   * Select Contact modal allows searching for a user by username, selecting the result, and
   * saving; after save the contact display name appears in the application owner summary header.
   */
  @Test
  @Category(RegressionTest.class)
  public void testSelectContact_searchAndSave() {
    Organization parent = tempEntity.newOrganization(SELECT_CONTACT_SAVE_ORG_NAME);
    tempEntity.newApplication(SELECT_CONTACT_SAVE_APP_NAME, SELECT_CONTACT_SAVE_APP_PUBLIC_ID, parent.getId());
    tempEntity.newUser(SELECT_CONTACT_SAVE_USER_NAME, SELECT_CONTACT_SAVE_USER_FIRST_NAME,
        SELECT_CONTACT_SAVE_USER_LAST_NAME, SELECT_CONTACT_SAVE_USER_NAME + TEST_EMAIL_DOMAIN);

    navigateAndWaitForUrl(OwnerSummaryPage.applicationUrl(SELECT_CONTACT_SAVE_APP_PUBLIC_ID),
        OwnerSummaryPage.APP_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerActionsDropdown();
    ownerSummary.clickSelectContactMenuItem();
    assertions.shouldShowSelectContactModal();

    // UserDirectory.findUsersByName searches by concat(firstName, ' ', lastName).
    ownerSummary.searchContact(SELECT_CONTACT_SAVE_USER_FIRST_NAME);
    ownerSummary.selectFirstContactResult();
    ownerSummary.saveContact();

    assertions.shouldShowContactName(SELECT_CONTACT_SAVE_USER_DISPLAY_NAME);
  }

  /**
   * Regression: "Copy Org ID to Clipboard" — clicking the menu item writes the organization's
   * internal ID to the system clipboard. Grants clipboard-read permission to the browser context
   * so the test can read back and assert the copied value.
   */
  @Test
  @Category(RegressionTest.class)
  public void testCopyOrgIdToClipboard_verifiesOrgIdCopied() {
    Organization org = tempEntity.newOrganization(COPY_ORG_ID_ORG_NAME);

    context.grantPermissions(CLIPBOARD_PERMISSIONS);

    navigateAndWaitForUrl(OwnerSummaryPage.url(org.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerActionsDropdown();
    ownerSummary.clickCopyOrgIdMenuItem();

    assertions.shouldHaveClipboardText(org.getId());
  }

  /**
   * Regression: Creating an application with an ID already used by another application shows a
   * validation error (ID is already in use) on the form and blocks the Create submit.
   * The duplicate-ID check is performed client-side by {@code duplicationAppIdValidator} in
   * {@code ownerModalSlice.js} against the loaded sibling-apps list; no API call is made.
   */
  @Test
  @Category(RegressionTest.class)
  public void testCreateApplication_duplicateIdBlocksSubmit() {
    Organization org = tempEntity.newOrganization(DUPLICATE_APP_ID_ORG_NAME);
    tempEntity.newApplication(DUPLICATE_APP_ID_EXISTING_APP_NAME, DUPLICATE_APP_ID_EXISTING_PUBLIC_ID, org.getId());

    navigateAndWaitForUrl(OwnerSummaryPage.url(org.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.clickAddNewApplicationButton();
    assertions.shouldShowNewApplicationModal();

    ownerSummary.typeOwnerName(DUPLICATE_APP_ID_NEW_APP_NAME);
    ownerSummary.typeApplicationPublicId(DUPLICATE_APP_ID_EXISTING_PUBLIC_ID);
    assertions.shouldShowCreateApplicationDuplicateIdError();
  }

  /**
   * Regression: Synthetic org — a user who has permission only on a child app (not the parent org)
   * sees {@code InsufficientPermissionOwnerHierarchyTree} when navigating directly to the parent
   * org summary. The standard tile sections (Policies, Access, Continuous Monitoring) must not
   * be rendered.
   */
  @Test
  @Category(RegressionTest.class)
  public void testSyntheticOrg_showsInsufficientPermissionTree() {
    Organization org = tempEntity.newOrganization(SYNTHETIC_ORG_NAME);
    Application app = tempEntity.newApplication(
        SYNTHETIC_ORG_APP_NAME, SYNTHETIC_ORG_APP_PUBLIC_ID, org.getId());
    User limitedUser = tempEntity.newUser(
        SYNTHETIC_ORG_LIMITED_USER_NAME, SYNTHETIC_ORG_LIMITED_USER_FIRST_NAME, SYNTHETIC_ORG_LIMITED_USER_LAST_NAME,
        SYNTHETIC_ORG_LIMITED_USER_NAME + TEST_EMAIL_DOMAIN);
    // Grant developer role on the child app only — NOT on the parent org.
    // This makes the parent org synthetic for the limited user.
    tempEntity.newMembershipMapping(app.getId(), Role.DEVELOPER_ROLE_ID, limitedUser.getUsername());

    // Re-login as the limited user; hardreset + blank forces a full SPA reload so the
    // new session picks up the limited-user's owner hierarchy (not the admin session from @Before).
    playwrightHardresetToBlank();
    playwrightLoginAt(
        OwnerSummaryPage.url(org.getId()),
        limitedUser.getUsername(),
        TemporaryEntity.USER_PASSWORD_CLEAR);

    assertions.shouldShowInsufficientPermissionTree(SYNTHETIC_ORG_EXPECTED_DESCRIPTION);
  }

}
