/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.util.regex.Pattern;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.ApplicationCategoryEditorPage;
import com.sonatype.clm.testing.playwright.pages.ApplicationCategoryEditorPageAssertions;
import com.sonatype.clm.testing.playwright.pages.AssignAppCategoryPage;
import com.sonatype.clm.testing.playwright.pages.AssignAppCategoryPageAssertions;
import com.sonatype.clm.testing.playwright.pages.LegacyViolationGrantModalPage;
import com.sonatype.clm.testing.playwright.pages.LegacyViolationGrantModalPageAssertions;
import com.sonatype.clm.testing.playwright.pages.LegacyViolationsEditorPage;
import com.sonatype.clm.testing.playwright.pages.LegacyViolationsEditorPageAssertions;
import com.sonatype.clm.testing.playwright.pages.RevokeLegacyViolationModalPage;
import com.sonatype.clm.testing.playwright.pages.RevokeLegacyViolationModalPageAssertions;
import com.sonatype.clm.testing.playwright.pages.ComponentLabelEditorPage;
import com.sonatype.clm.testing.playwright.pages.ComponentLabelEditorPageAssertions;
import com.sonatype.clm.testing.playwright.pages.LicenseThreatGroupEditorPage;
import com.sonatype.clm.testing.playwright.pages.LicenseThreatGroupEditorPageAssertions;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.HeaderComponent;
import com.sonatype.clm.testing.playwright.pages.HeaderComponentAssertions;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPageAssertions;
import com.sonatype.clm.testing.playwright.pages.OwnersTreePage;
import com.sonatype.clm.testing.playwright.pages.OwnersTreePageAssertions;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.microsoft.playwright.Route;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Organization and application regression tests (edge cases, error paths, UI creation flows).
 * <p>
 * Sanity tests live in {@link OrganizationPlaywrightTest}.
 * <p>
 * Login + session are handled by {@link #openDashboardAndLoginAsAdmin()}.
 * UI assertions live on {@link OwnerSummaryPage} per
 * {@code PLAYWRIGHT_TEST_AUTHORING_GUIDE.md} §4–§5.
 */
public class OrganizationRegressionPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String APP_REPO_URL_ORG_NAME = "App Repo URL Test Org";

  private static final String APP_REPO_URL_APP_NAME = "App Repo URL Test App";

  private static final String APP_REPO_URL_APP_PUBLIC_ID = "app-repo-url-test-pw";

  private static final String APP_REPO_URL = "https://github.com/sonatype-test/app-repo-url-pw";

  private static final String APP_REPO_TOKEN = "test-token-pw";

  private static final String CHILD_ORG_ACTIONS_ORG_NAME = "Child Org Actions Test Org";

  private static final String APP_ACTIONS_ORG_NAME = "App Actions Test Org";

  private static final String APP_ACTIONS_APP_NAME = "App Actions Test App";

  private static final String APP_ACTIONS_APP_PUBLIC_ID = "app-actions-test";

  private static final String EDIT_ORG_NAME_ORG_NAME = "Edit Org Name Test Org";

  private static final String EDIT_ORG_NAME_NEW_NAME = "Edit Org Name Updated";

  private static final String CHILD_ORG_TILES_ORG_NAME = "Child Org Tiles Test Org";

  private static final String CREATE_ORG_VIA_UI_ORG_NAME = "Create Via UI Test Org";

  private static final String CREATE_APP_VIA_UI_ORG_NAME = "Create App Via UI Test Org";

  private static final String CREATE_APP_VIA_UI_APP_NAME = "Create App Via UI Test App";

  private static final String CREATE_APP_VIA_UI_APP_PUBLIC_ID = "create-app-via-ui";

  private static final String CREATE_COMPONENT_LABEL_ORG_NAME = "Component Label Create Test Org";

  private static final String CREATE_COMPONENT_LABEL_NAME = "pw-test-label";

  private static final String CREATE_COMPONENT_LABEL_COLOR = "turquoise";

  private static final String CREATE_COMPONENT_LABEL_DESCRIPTION = "Playwright automation test label";

  private static final String EDIT_COMPONENT_LABEL_ORG_NAME = "Component Label Edit Test Org";

  private static final String EDIT_COMPONENT_LABEL_ORIGINAL_NAME = "pw-edit-label-original";

  private static final String EDIT_COMPONENT_LABEL_UPDATED_NAME = "pw-edit-label-updated";

  private static final String EDIT_COMPONENT_LABEL_UPDATED_COLOR = "sky";

  private static final String DELETE_COMPONENT_LABEL_ORG_NAME = "Component Label Delete Test Org";

  private static final String DELETE_COMPONENT_LABEL_NAME = "pw-delete-label";

  private static final String CREATE_LTG_ORG_NAME = "LTG Create Test Org";

  private static final String CREATE_LTG_GROUP_NAME = "pw-test-ltg";

  private static final int CREATE_LTG_THREAT_LEVEL = 8;

  private static final String CREATE_LTG_LICENSE_FILTER = "MIT";

  private static final String EDIT_LTG_ORG_NAME = "LTG Edit Test Org";

  private static final String EDIT_LTG_ORIGINAL_NAME = "pw-edit-ltg-original";

  private static final String EDIT_LTG_UPDATED_NAME = "pw-edit-ltg-updated";

  private static final String EDIT_LTG_LICENSE_FILTER = "Apache";

  private static final String DELETE_LTG_ORG_NAME = "LTG Delete Test Org";

  private static final String DELETE_LTG_GROUP_NAME = "pw-delete-ltg";

  private static final String CREATE_CATEGORY_ORG_NAME = "App Category Create Test Org";

  private static final String CREATE_CATEGORY_NAME = "pw-test-category";

  private static final String CREATE_CATEGORY_DESCRIPTION = "Playwright automation test category";

  private static final String CREATE_CATEGORY_COLOR = "purple";

  private static final String EDIT_CATEGORY_ORG_NAME = "App Category Edit Test Org";

  private static final String EDIT_CATEGORY_ORIGINAL_NAME = "pw-edit-category-original";

  private static final String EDIT_CATEGORY_UPDATED_NAME = "pw-edit-category-updated";

  private static final String DELETE_CATEGORY_ORG_NAME = "App Category Delete Test Org";

  private static final String DELETE_CATEGORY_NAME = "pw-delete-category";

  private static final String ASSIGN_CATEGORY_ORG_NAME = "App Category Assign Test Org";

  private static final String ASSIGN_CATEGORY_APP_NAME = "App Category Assign Test App";

  private static final String ASSIGN_CATEGORY_APP_PUBLIC_ID = "assign-category-test-pw";

  private static final String ASSIGN_CATEGORY_ITEM_NAME = "pw-assign-category";

  private static final String LEGACY_VIOLATIONS_ORG_NAME = "Legacy Violations Form Test Org";

  private static final String LEGACY_VIOLATIONS_APP_NAME = "Legacy Violations Form Test App";

  private static final String LEGACY_VIOLATIONS_APP_PUBLIC_ID = "legacy-violations-test-pw";

  private static final String LV_SAVE_ORG_NAME = "LV Save Status Test Org";

  private static final String LV_OVERRIDE_PARENT_ORG_NAME = "LV Override Parent Test Org";

  private static final String LV_OVERRIDE_CHILD_ORG_NAME = "LV Override Child Test Org";

  private static final String LV_GRANT_REVOKE_ORG_NAME = "LV Grant Revoke Test Org";

  private static final String LV_GRANT_REVOKE_APP_NAME = "LV Grant Revoke Test App";

  private static final String LV_GRANT_REVOKE_APP_PUBLIC_ID = "lv-grant-revoke-test-pw";

  private OwnerSummaryPage ownerSummary;

  private OwnerSummaryPageAssertions assertions;

  private OwnersTreePage ownersTree;

  private OwnersTreePageAssertions ownersTreeAssertions;

  private HeaderComponent header;

  private HeaderComponentAssertions headerAssertions;

  @Before
  public void openDashboardAndLoginAsAdmin() {
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
    ownerSummary = new OwnerSummaryPage();
    assertions = new OwnerSummaryPageAssertions(ownerSummary);
    ownersTree = new OwnersTreePage();
    ownersTreeAssertions = new OwnersTreePageAssertions(ownersTree);
    header = new HeaderComponent();
    headerAssertions = new HeaderComponentAssertions(header);
  }

  private void assertRootOrgOwnerSummaryVisible() {
    headerAssertions.shouldBeLoggedIn();
    assertions.shouldBeVisible();
  }

  /**
   * The Actions dropdown on a child organization shows Move, Delete, Edit
   * Name/Icon, Copy Org ID, and Import Policies. These items are absent on the root org because
   * {@code ActionDropdown.jsx} gates Move and Delete on {@code !isRootOrg}.
   */
  @Test
  @Category(RegressionTest.class)
  public void testChildOrgActionsDropdownOptions() {
    Organization childOrg = tempEntity.newOrganization(CHILD_ORG_ACTIONS_ORG_NAME);

    navigateAndWaitForUrl(OwnerSummaryPage.url(childOrg.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerActionsDropdown();
    assertions.shouldShowChildOrganizationActionsMenu();
  }

  /**
   * The Actions dropdown on an application shows app-specific options:
   * Copy App ID, Select Contact, Edit App Name/Icon, Change App ID, Move, Delete, and Evaluate a File.
   */
  @Test
  @Category(RegressionTest.class)
  public void testApplicationActionsDropdownOptions() {
    Organization org = tempEntity.newOrganization(APP_ACTIONS_ORG_NAME);
    tempEntity.newApplication(APP_ACTIONS_APP_NAME, APP_ACTIONS_APP_PUBLIC_ID, org.getId());

    navigateAndWaitForUrl(OwnerSummaryPage.applicationUrl(APP_ACTIONS_APP_PUBLIC_ID),
        OwnerSummaryPage.APP_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerActionsDropdown();
    assertions.shouldShowApplicationActionsMenu();
  }

  /**
   * Clicking "Edit Org Name / Icon" from the Actions dropdown opens the Edit Organization
   * modal ({@code OwnerModal.jsx}, id="owner-editor"); entering a new name and clicking Update saves
   * the change and the owner summary heading reflects the new name.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEditOrgName_updatesHeading() {
    Organization org = tempEntity.newOrganization(EDIT_ORG_NAME_ORG_NAME);

    navigateAndWaitForUrl(OwnerSummaryPage.url(org.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerActionsDropdown();
    ownerSummary.clickEditOwnerNameMenuItem();
    assertions.shouldShowEditOwnerModal();

    ownerSummary.typeOwnerName(EDIT_ORG_NAME_NEW_NAME);
    ownerSummary.submitEditOwnerModal();

    assertions.shouldShowOwnerName(EDIT_ORG_NAME_NEW_NAME);
  }

  /**
   * Navigating to a child organization summary renders the standard tile set: Policies,
   * Proprietary Component Configuration, Component Labels, License Threat Groups, and Access are
   * unconditionally present; feature-gated tiles (Legacy Violations, Continuous Monitoring,
   * Source Control, Auto-Waivers, InnerSource Repository) are verified only when licensed.
   */
  @Test
  @Category(RegressionTest.class)
  public void testChildOrgSummaryTiles() {
    Organization org = tempEntity.newOrganization(CHILD_ORG_TILES_ORG_NAME);

    navigateAndWaitForUrl(OwnerSummaryPage.url(org.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    assertions.shouldBeVisible();
    assertions.shouldShowOwnerName(CHILD_ORG_TILES_ORG_NAME);

    ownerSummary.openPoliciesSectionFromNavPills();
    assertions.shouldShowPoliciesTile();

    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_PROPRIETARY_COMPONENTS);
    assertions.shouldShowProprietaryComponentsTile();

    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_COMPONENT_LABELS);
    assertions.shouldShowComponentLabelsTile();

    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_LICENSE_THREAT_GROUPS);
    assertions.shouldShowLicenseThreatGroupsTile();

    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_ACCESS);
    assertions.shouldShowAccessTile();

    // Feature-gated tiles: only verify when the nav pill is present
    if (ownerSummary.isFeaturePillPresent(OwnerSummaryPage.OWNER_PILL_LEGACY_VIOLATIONS)) {
      ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_LEGACY_VIOLATIONS);
      assertions.shouldShowLegacyViolationsTile();
    }
    if (ownerSummary.isFeaturePillPresent(OwnerSummaryPage.OWNER_PILL_CONTINUOUS_MONITORING)) {
      ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_CONTINUOUS_MONITORING);
      assertions.shouldShowContinuousMonitoringTile();
    }
    if (ownerSummary.isFeaturePillPresent(OwnerSummaryPage.OWNER_PILL_SOURCE_CONTROL)) {
      ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_SOURCE_CONTROL);
      assertions.shouldShowSourceControlTile();
    }
    if (ownerSummary.isFeaturePillPresent(OwnerSummaryPage.OWNER_PILL_AUTO_WAIVERS)) {
      ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_AUTO_WAIVERS);
      assertions.shouldShowAutoWaiversTile();
    }
    if (ownerSummary.isFeaturePillPresent(OwnerSummaryPage.OWNER_PILL_INNERSOURCE_REPOSITORY)) {
      ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_INNERSOURCE_REPOSITORY);
      assertions.shouldShowInnerSourceRepositoryTile();
    }
  }

  /**
   * Full UI creation flow: navigate to root org summary, click "Add New Organization" in
   * the sidebar, enter a name, click Create; the new org appears in the owners tree and its owner
   * summary is accessible. Note: the sidebar Add New Organization flow does NOT redirect to the new
   * org — the SPA stays on the root org summary after creation.
   */
  @Test
  @Category(RegressionTest.class)
  public void testCreateOrganization_viaUiForm() {
    navigateAndWaitForUrl(OwnerSummaryPage.urlToRootOrg(), OwnerSummaryPage.ORG_URL_FRAGMENT);
    assertRootOrgOwnerSummaryVisible();

    ownerSummary.clickAddOrganizationButton();
    assertions.shouldShowNewOrganizationModal();

    ownerSummary.typeOwnerName(CREATE_ORG_VIA_UI_ORG_NAME);
    ownerSummary.submitNewOwnerModal();

    // Sidebar Add New Organization stays on root org — navigate to the tree to verify creation.
    playwrightNavigateTo(OwnersTreePage.url());
    ownersTreeAssertions.shouldContainItemWithText(CREATE_ORG_VIA_UI_ORG_NAME);
    ownersTree.clickItemWithText(CREATE_ORG_VIA_UI_ORG_NAME);
    assertions.shouldHaveUrlContaining(OwnerSummaryPage.ORG_URL_FRAGMENT);
    assertions.shouldShowOwnerName(CREATE_ORG_VIA_UI_ORG_NAME);
  }

  /**
   * Attempting to create an organization with an empty name: clicking Create without
   * entering a name shows the NxStatefulForm validation errors banner (name is required).
   */
  @Test
  @Category(RegressionTest.class)
  public void testCreateOrganization_emptyNameShowsValidation() {
    navigateAndWaitForUrl(OwnerSummaryPage.urlToRootOrg(), OwnerSummaryPage.ORG_URL_FRAGMENT);
    assertRootOrgOwnerSummaryVisible();

    ownerSummary.clickAddOrganizationButton();
    assertions.shouldShowNewOrganizationModal();

    ownerSummary.clickCreateOwnerButton();
    assertions.shouldShowNewOwnerModalNameRequiredError();
  }

  /**
   * Full UI application creation flow: navigate to a child org summary, click the sidebar
   * "Add Application → New Application" control, enter name and public ID, click Create; the SPA
   * redirects directly to the new app's owner summary (app creation always redirects, unlike org
   * creation). The app also appears in the owners tree.
   */
  @Test
  @Category(RegressionTest.class)
  public void testCreateApplication_viaUiForm() {
    Organization org = tempEntity.newOrganization(CREATE_APP_VIA_UI_ORG_NAME);

    navigateAndWaitForUrl(OwnerSummaryPage.url(org.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.clickAddNewApplicationButton();
    assertions.shouldShowNewApplicationModal();

    ownerSummary.typeOwnerName(CREATE_APP_VIA_UI_APP_NAME);
    ownerSummary.typeApplicationPublicId(CREATE_APP_VIA_UI_APP_PUBLIC_ID);
    ownerSummary.submitNewOwnerModal();

    // App creation always redirects to the new application owner summary.
    assertions.shouldHaveUrlContaining(OwnerSummaryPage.APP_URL_FRAGMENT);
    assertions.shouldShowOwnerName(CREATE_APP_VIA_UI_APP_NAME);

    playwrightNavigateTo(OwnersTreePage.url());
    ownersTreeAssertions.shouldContainItemWithText(CREATE_APP_VIA_UI_APP_NAME);
  }

  /**
   * SCM Repository URL visible for app with URL configured.
   *
   * <p>
   * Seeds an application with a GitHub source-control config (provider + repository URL), navigates to
   * its owner summary, and asserts the URL is displayed with a provider icon.
   */
  @Test
  @Category(RegressionTest.class)
  public void testAppRepositoryUrlVisibleWithProviderIcon() {
    Organization org = tempEntity.newOrganization(APP_REPO_URL_ORG_NAME);
    Application app = tempEntity.newApplication(APP_REPO_URL_APP_NAME, APP_REPO_URL_APP_PUBLIC_ID, org.getId());
    tempEntity.newSourceControl(app.getId(), APP_REPO_URL, APP_REPO_TOKEN, SourceControlProvider.GITHUB);

    navigateAndWaitForUrl(OwnerSummaryPage.applicationUrl(APP_REPO_URL_APP_PUBLIC_ID),
        OwnerSummaryPage.APP_URL_FRAGMENT);
    assertions.shouldBeVisible();
    assertions.shouldShowRepositoryUrl(APP_REPO_URL);
  }

  /**
   * Verify the full Create Component Label flow.
   * <p>
   * From the org owner summary, click "Add a Label" in the Component Labels tile to open the
   * label editor. Enter a name, select a color, enter a description, and save. Assert the
   * {@code NxSubmitMask} success overlay appears and dismisses, then navigate back to the owner
   * summary and confirm the new label appears in the "Local to" section of the Component Labels
   * tile.
   * <p>
   * Requires the custom component labels enterprise feature to be enabled (same precondition
   * as the {@code testChildOrgSummaryTiles} assertion for the "Add a Label" button).
   */
  @Test
  @Category(RegressionTest.class)
  public void testCreateComponentLabel_appearsInLabelsTile() {
    Organization org = tempEntity.newOrganization(CREATE_COMPONENT_LABEL_ORG_NAME);

    navigateAndWaitForUrl(OwnerSummaryPage.url(org.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_COMPONENT_LABELS);
    assertions.shouldShowComponentLabelsTile();

    // ComponentLabelEditorPage handles both the tile entry point and the editor form.
    ComponentLabelEditorPage editor = new ComponentLabelEditorPage();
    ComponentLabelEditorPageAssertions editorAssertions = new ComponentLabelEditorPageAssertions(editor);
    editor.clickAddLabelButton();
    playwrightWaitUntilUrlContains(ComponentLabelEditorPage.CREATE_LABEL_URL_FRAGMENT);

    editorAssertions.shouldBeVisible();
    editorAssertions.shouldBeInCreateMode();

    editor.typeLabelName(CREATE_COMPONENT_LABEL_NAME);
    editor.selectColor(CREATE_COMPONENT_LABEL_COLOR);
    editor.typeDescription(CREATE_COMPONENT_LABEL_DESCRIPTION);
    editor.submit();
    editor.waitForSaveSuccess();

    // Navigate back to owner summary and verify the label is in the Component Labels tile
    navigateAndWaitForUrl(OwnerSummaryPage.url(org.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_COMPONENT_LABELS);
    editorAssertions.shouldHaveLocalComponentLabel(CREATE_COMPONENT_LABEL_NAME);
  }

  /**
   * Verify the full Edit Component Label flow.
   * <p>
   * Seeds a label directly in the DB, navigates to the org owner summary, and clicks the label
   * link in the Component Labels tile to open the editor in edit mode. Changes the name and
   * color, saves, asserts the {@code NxSubmitMask} success overlay appears and dismisses, then
   * navigates back to the owner summary and confirms the updated name persists in the tile.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEditComponentLabel_updatedValuesPersistInLabelsTile() {
    Organization org = tempEntity.newOrganization(EDIT_COMPONENT_LABEL_ORG_NAME);
    tempEntity.newLabel(org.getId(), EDIT_COMPONENT_LABEL_ORIGINAL_NAME, null, Color.orange);

    navigateAndWaitForUrl(OwnerSummaryPage.url(org.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_COMPONENT_LABELS);
    assertions.shouldShowComponentLabelsTile();

    // Click the existing label link to open the editor in edit mode.
    ComponentLabelEditorPage editor = new ComponentLabelEditorPage();
    ComponentLabelEditorPageAssertions editorAssertions = new ComponentLabelEditorPageAssertions(editor);
    editor.clickLabelInTile(EDIT_COMPONENT_LABEL_ORIGINAL_NAME);
    playwrightWaitUntilUrlContains(ComponentLabelEditorPage.EDIT_LABEL_URL_FRAGMENT);

    editorAssertions.shouldBeVisible();
    editorAssertions.shouldBeInEditMode();

    editor.typeLabelName(EDIT_COMPONENT_LABEL_UPDATED_NAME);
    editor.selectColor(EDIT_COMPONENT_LABEL_UPDATED_COLOR);
    editor.submit();
    editor.waitForSaveSuccess();

    // Navigate back to owner summary and verify the updated label name is in the tile.
    navigateAndWaitForUrl(OwnerSummaryPage.url(org.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_COMPONENT_LABELS);
    editorAssertions.shouldHaveLocalComponentLabel(EDIT_COMPONENT_LABEL_UPDATED_NAME);
  }

  /**
   * Verify the full Delete Component Label flow.
   * <p>
   * Seeds a label directly in the DB, navigates to the org owner summary, and clicks the label
   * link in the Component Labels tile to open the editor in edit mode. Clicks the Delete button
   * to open the delete confirmation modal, confirms the deletion, then navigates back to the
   * owner summary and verifies the label is no longer present in the tile.
   * <p>
   * After a successful delete, Redux dispatches {@code goToCreateLabel()}, navigating the SPA
   * to the create-label route ({@code …/label}). The test anchors on {@code shouldBeInCreateMode()}
   * before navigating back to confirm the delete completed and the SPA has settled.
   */
  @Test
  @Category(RegressionTest.class)
  public void testDeleteComponentLabel_removedFromLabelsTile() {
    Organization org = tempEntity.newOrganization(DELETE_COMPONENT_LABEL_ORG_NAME);
    tempEntity.newLabel(org.getId(), DELETE_COMPONENT_LABEL_NAME, null, Color.orange);

    navigateAndWaitForUrl(OwnerSummaryPage.url(org.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_COMPONENT_LABELS);
    assertions.shouldShowComponentLabelsTile();

    // Click the label link to open the editor in edit mode.
    ComponentLabelEditorPage editor = new ComponentLabelEditorPage();
    ComponentLabelEditorPageAssertions editorAssertions = new ComponentLabelEditorPageAssertions(editor);
    editor.clickLabelInTile(DELETE_COMPONENT_LABEL_NAME);
    playwrightWaitUntilUrlContains(ComponentLabelEditorPage.EDIT_LABEL_URL_FRAGMENT);

    editorAssertions.shouldBeVisible();
    editorAssertions.shouldBeInEditMode();

    // Open and confirm the delete modal.
    editor.clickDeleteButton();
    editorAssertions.shouldShowDeleteModal();
    editor.clickConfirmDelete();

    // Wait for create mode to confirm the delete completed and navigation settled.
    editorAssertions.shouldBeInCreateMode();

    // Navigate back to owner summary and verify the label is gone from the tile.
    navigateAndWaitForUrl(OwnerSummaryPage.url(org.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_COMPONENT_LABELS);
    editorAssertions.shouldNotHaveLocalComponentLabel(DELETE_COMPONENT_LABEL_NAME);
  }

  /**
   * Verify the full Edit License Threat Group flow.
   * <p>
   * Seeds an LTG directly in the DB, navigates to the org owner summary, and clicks the group row
   * in the License Threat Groups tile to open the editor in edit mode. Changes the name and adds
   * a license via the transfer list filter, saves, asserts the {@code NxSubmitMask} success overlay
   * appears and dismisses (edit mode stays on the same page), then navigates back to the owner
   * summary and confirms the updated name persists in the tile.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEditLicenseThreatGroup_updatedValuesPersistInTile() {
    Organization org = tempEntity.newOrganization(EDIT_LTG_ORG_NAME);
    tempEntity.newLicenseThreatGroup(org.getId(), EDIT_LTG_ORIGINAL_NAME, 5);

    navigateAndWaitForUrl(OwnerSummaryPage.url(org.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_LICENSE_THREAT_GROUPS);
    assertions.shouldShowLicenseThreatGroupsTile();

    // Click the LTG row — RSC NxTable clickable row dispatches goToEditLTG(), navigating to
    // …/licenseThreatGroup/{ltgId}. LicenseThreatGroupEditorPage is constructed before the click
    // (ThreadLocal page — lazy locators remain valid after SPA navigation).
    LicenseThreatGroupEditorPage editor = new LicenseThreatGroupEditorPage();
    LicenseThreatGroupEditorPageAssertions editorAssertions = new LicenseThreatGroupEditorPageAssertions(editor);
    editor.clickLtgRowInTile(EDIT_LTG_ORIGINAL_NAME);
    playwrightWaitUntilUrlContains(LicenseThreatGroupEditorPage.EDIT_LTG_URL_FRAGMENT);

    editorAssertions.shouldBeVisible();
    editorAssertions.shouldBeInEditMode();

    editor.typeGroupName(EDIT_LTG_UPDATED_NAME);
    editor.addFirstLicenseMatchingFilter(EDIT_LTG_LICENSE_FILTER);
    editor.submit();
    editor.waitForSaveSuccess();

    // Navigate back to owner summary and verify the updated group name is in the LTG tile.
    // playwrightRefresh() bypasses the Redux cache so the tile re-fetches from the server,
    // confirming the edit was persisted (not just held in client-side state).
    navigateAndWaitForUrl(OwnerSummaryPage.url(org.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    playwrightRefresh();
    assertions.shouldBeVisible();
    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_LICENSE_THREAT_GROUPS);
    editorAssertions.shouldHaveLocalThreatGroup(EDIT_LTG_UPDATED_NAME);
  }

  /**
   * Verify the full Delete License Threat Group flow.
   * <p>
   * Seeds an LTG directly in the DB, navigates to the org owner summary, and clicks the group row
   * in the License Threat Groups tile to open the editor in edit mode. Clicks the Delete button
   * to open the delete confirmation modal, confirms the deletion, then navigates back to the owner
   * summary and verifies the group is no longer present in the tile.
   * <p>
   * After a successful delete, Redux dispatches {@code goToCreateLTG()}, navigating the SPA to
   * the create-LTG route ({@code …/licenseThreatGroup}). The test anchors on
   * {@code shouldBeInCreateMode()} before navigating back to confirm the delete completed and
   * the SPA has settled.
   */
  @Test
  @Category(RegressionTest.class)
  public void testDeleteLicenseThreatGroup_removedFromTile() {
    Organization org = tempEntity.newOrganization(DELETE_LTG_ORG_NAME);
    tempEntity.newLicenseThreatGroup(org.getId(), DELETE_LTG_GROUP_NAME, 5);

    navigateAndWaitForUrl(OwnerSummaryPage.url(org.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_LICENSE_THREAT_GROUPS);
    assertions.shouldShowLicenseThreatGroupsTile();

    // Click the LTG row to open the editor in edit mode.
    LicenseThreatGroupEditorPage editor = new LicenseThreatGroupEditorPage();
    LicenseThreatGroupEditorPageAssertions editorAssertions = new LicenseThreatGroupEditorPageAssertions(editor);
    editor.clickLtgRowInTile(DELETE_LTG_GROUP_NAME);
    playwrightWaitUntilUrlContains(LicenseThreatGroupEditorPage.EDIT_LTG_URL_FRAGMENT);

    editorAssertions.shouldBeVisible();
    editorAssertions.shouldBeInEditMode();

    // Open and confirm the delete modal.
    editor.clickDeleteButton();
    editorAssertions.shouldShowDeleteModal();
    editor.clickConfirmDelete();

    // After delete, Redux dispatches goToCreateLTG() — SPA navigates to …/licenseThreatGroup.
    // Wait for create mode to confirm the delete completed and navigation settled.
    editorAssertions.shouldBeInCreateMode();

    // Navigate back to owner summary and verify the group is gone from the LTG tile.
    navigateAndWaitForUrl(OwnerSummaryPage.url(org.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_LICENSE_THREAT_GROUPS);
    editorAssertions.shouldNotHaveLocalThreatGroup(DELETE_LTG_GROUP_NAME);
  }

  /**
   * Verify the full Create Application Category flow.
   * <p>
   * From the org owner summary, click "Add a Category" in the Application Categories tile to open
   * the category editor in create mode. Enter a name, description (required), and select a color,
   * then save. Assert the {@code NxSubmitMask} success overlay appears and dismisses (SPA stays
   * on the create-category page after save), then navigate back to the owner summary and confirm
   * the new category appears in the "Local to" section of the Application Categories tile.
   * <p>
   * Requires the {@code custom-application-categories} enterprise feature to be enabled.
   */
  @Test
  @Category(RegressionTest.class)
  public void testCreateApplicationCategory_appearsInCategoriesTile() {
    Organization org = tempEntity.newOrganization(CREATE_CATEGORY_ORG_NAME);

    navigateAndWaitForUrl(OwnerSummaryPage.url(org.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_APP_CATEGORIES);
    assertions.shouldShowApplicationCategoriesTile();

    // Click "Add a Category" — Redux dispatches goToCreateCategory(), navigating to /category.
    // ApplicationCategoryEditorPage is constructed before the click (ThreadLocal page — lazy locators).
    ApplicationCategoryEditorPage editor = new ApplicationCategoryEditorPage();
    ApplicationCategoryEditorPageAssertions editorAssertions = new ApplicationCategoryEditorPageAssertions(editor);
    editor.clickAddCategoryButton();
    playwrightWaitUntilUrlContains(ApplicationCategoryEditorPage.CREATE_CATEGORY_URL_FRAGMENT);

    editorAssertions.shouldBeVisible();
    editorAssertions.shouldBeInCreateMode();

    editor.typeCategoryName(CREATE_CATEGORY_NAME);
    editor.typeDescription(CREATE_CATEGORY_DESCRIPTION);
    editor.selectColor(CREATE_CATEGORY_COLOR);
    editor.submit();
    editor.waitForSaveSuccess();

    // Navigate back to owner summary and verify the category is in the tile.
    navigateAndWaitForUrl(OwnerSummaryPage.url(org.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_APP_CATEGORIES);
    editorAssertions.shouldHaveLocalCategory(CREATE_CATEGORY_NAME);
  }

  /**
   * Verify the full Edit Application Category flow.
   * <p>
   * Seeds a category (Tag) directly in the DB, navigates to the org owner summary, and clicks
   * the category link in the Application Categories tile to open the editor in edit mode.
   * Changes the name, saves, asserts the {@code NxSubmitMask} success overlay appears and
   * dismisses (SPA stays on the edit page), then navigates back to the owner summary and
   * confirms the updated name persists in the tile.
   * <p>
   * Application categories are stored as {@code Tag} entities; seeded via
   * {@code tempEntity.newTag(orgId, name, description, color)}.
   * <p>
   * Requires the {@code custom-application-categories} enterprise feature to be enabled.
   */
  @Test
  @Category(RegressionTest.class)
  public void testEditApplicationCategory_updatedNamePersistsInTile() {
    Organization org = tempEntity.newOrganization(EDIT_CATEGORY_ORG_NAME);
    tempEntity.newTag(org.getId(), EDIT_CATEGORY_ORIGINAL_NAME, "Test description", Color.orange);

    navigateAndWaitForUrl(OwnerSummaryPage.url(org.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_APP_CATEGORIES);
    assertions.shouldShowApplicationCategoriesTile();

    // Click the existing category link to open the editor in edit mode.
    // ApplicationCategoryEditorPage is constructed before the click (ThreadLocal page — lazy locators).
    ApplicationCategoryEditorPage editor = new ApplicationCategoryEditorPage();
    ApplicationCategoryEditorPageAssertions editorAssertions = new ApplicationCategoryEditorPageAssertions(editor);
    editor.clickCategoryInTile(EDIT_CATEGORY_ORIGINAL_NAME);
    playwrightWaitUntilUrlContains(ApplicationCategoryEditorPage.EDIT_CATEGORY_URL_FRAGMENT);

    editorAssertions.shouldBeVisible();
    editorAssertions.shouldBeInEditMode();

    editor.typeCategoryName(EDIT_CATEGORY_UPDATED_NAME);
    editor.submit();
    editor.waitForSaveSuccess();

    // Navigate back to owner summary and verify the updated name is in the tile.
    navigateAndWaitForUrl(OwnerSummaryPage.url(org.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_APP_CATEGORIES);
    editorAssertions.shouldHaveLocalCategory(EDIT_CATEGORY_UPDATED_NAME);
  }

  /**
   * Verify the full Delete Application Category flow.
   * <p>
   * Seeds a category (Tag) directly in the DB, navigates to the org owner summary, and clicks
   * the category link in the Application Categories tile to open the editor in edit mode.
   * Clicks the Delete button to open the delete confirmation modal, confirms the deletion by
   * clicking "Continue", then navigates back to the owner summary and verifies the category is
   * no longer present in the tile.
   * <p>
   * After a successful delete, Redux dispatches {@code goToCreateCategory()}, navigating the
   * SPA to the create-category route ({@code …/category}). The test anchors on
   * {@code shouldBeInCreateMode()} before navigating back to confirm the delete completed and
   * the SPA has settled.
   * <p>
   * Requires the {@code custom-application-categories} enterprise feature to be enabled.
   */
  @Test
  @Category(RegressionTest.class)
  public void testDeleteApplicationCategory_removedFromTile() {
    Organization org = tempEntity.newOrganization(DELETE_CATEGORY_ORG_NAME);
    tempEntity.newTag(org.getId(), DELETE_CATEGORY_NAME, "Test description", Color.orange);

    navigateAndWaitForUrl(OwnerSummaryPage.url(org.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_APP_CATEGORIES);
    assertions.shouldShowApplicationCategoriesTile();

    // Click the category link to open the editor in edit mode.
    ApplicationCategoryEditorPage editor = new ApplicationCategoryEditorPage();
    ApplicationCategoryEditorPageAssertions editorAssertions = new ApplicationCategoryEditorPageAssertions(editor);
    editor.clickCategoryInTile(DELETE_CATEGORY_NAME);
    playwrightWaitUntilUrlContains(ApplicationCategoryEditorPage.EDIT_CATEGORY_URL_FRAGMENT);

    editorAssertions.shouldBeVisible();
    editorAssertions.shouldBeInEditMode();

    // Open and confirm the delete modal ("Continue" is the modal's submit button text).
    editor.clickDeleteButton();
    editorAssertions.shouldShowDeleteModal();
    editor.clickConfirmDelete();

    // After delete, Redux dispatches goToCreateCategory() — SPA navigates to …/category.
    // Wait for create mode to confirm the delete completed and navigation settled.
    editorAssertions.shouldBeInCreateMode();

    // Navigate back to owner summary and verify the category is gone from the tile.
    navigateAndWaitForUrl(OwnerSummaryPage.url(org.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_APP_CATEGORIES);
    editorAssertions.shouldNotHaveLocalCategory(DELETE_CATEGORY_NAME);
  }

  /**
   * Verify the full Assign Category to Application flow.
   * <p>
   * Seeds an org with a category (Tag) and an application, navigates to the application owner
   * summary, and clicks "Assign a Category" in the Application Categories tile. On the
   * {@code AssignAppCategory} page, checks the seeded category and saves. Navigates back to the
   * application owner summary and verifies the category appears in the "Assigned" section of
   * the tile.
   * <p>
   * Application categories are stored as {@code Tag} entities; seeded via
   * {@code tempEntity.newTag(orgId, name, description, color)}.
   * <p>
   * Requires the {@code custom-application-categories} enterprise feature to be enabled.
   */
  @Test
  @Category(RegressionTest.class)
  public void testAssignCategoryToApplication_categoryAppearsOnAppSummary() {
    Organization org = tempEntity.newOrganization(ASSIGN_CATEGORY_ORG_NAME);
    tempEntity.newTag(org.getId(), ASSIGN_CATEGORY_ITEM_NAME, "Test description", Color.light_purple);
    tempEntity.newApplication(ASSIGN_CATEGORY_APP_NAME, ASSIGN_CATEGORY_APP_PUBLIC_ID, org.getId());

    navigateAndWaitForUrl(OwnerSummaryPage.applicationUrl(ASSIGN_CATEGORY_APP_PUBLIC_ID),
        OwnerSummaryPage.APP_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_APP_CATEGORIES);

    // Click "Assign a Category" — on application view the tile shows this button (pen icon).
    // Same id="add-category-button" as on the org view; dispatches goToAssignCategories().
    ApplicationCategoryEditorPage categoryTile = new ApplicationCategoryEditorPage();
    categoryTile.clickAddCategoryButton();
    playwrightWaitUntilUrlContains(
        ASSIGN_CATEGORY_APP_PUBLIC_ID + ApplicationCategoryEditorPage.CREATE_CATEGORY_URL_FRAGMENT);

    // On the Assign Application Categories page, check the category and save.
    AssignAppCategoryPage assignPage = new AssignAppCategoryPage();
    AssignAppCategoryPageAssertions assignAssertions = new AssignAppCategoryPageAssertions(assignPage);
    assignAssertions.shouldBeVisible();

    assignPage.checkCategory(ASSIGN_CATEGORY_ITEM_NAME);
    assignPage.submit();
    assignPage.waitForSaveSuccess();

    // Navigate back to app summary and verify the category appears in the Assigned section.
    navigateAndWaitForUrl(OwnerSummaryPage.applicationUrl(ASSIGN_CATEGORY_APP_PUBLIC_ID),
        OwnerSummaryPage.APP_URL_FRAGMENT);
    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_APP_CATEGORIES);
    new ApplicationCategoryEditorPageAssertions(categoryTile).shouldHaveAssignedCategory(ASSIGN_CATEGORY_ITEM_NAME);
  }

  /**
   * Verify Legacy Violations form renders conditionally
   * based on context — child org, root org, and application.
   * <p>
   * <b>Step 2 (child org):</b> All three radios visible (Inherit from parent, Enabled, Disabled)
   * and "Allow configuration to be overridden…" checkbox visible.<br>
   * <b>Step 3 (root org):</b> "Inherit from parent" radio NOT rendered ({@code !isRootOrg} gate);
   * only Enabled and Disabled shown; checkbox still visible.<br>
   * <b>Step 4 (application):</b> "Allow configuration to be overridden…" checkbox NOT rendered
   * ({@code !isApp} gate in {@code LegacyViolationsEditor.jsx}).
   */
  @Test
  @Category(RegressionTest.class)
  public void testLegacyViolationsForm_radiosAndCheckboxConditionallyRendered() {
    Organization childOrg = tempEntity.newOrganization(LEGACY_VIOLATIONS_ORG_NAME);
    tempEntity.newApplication(LEGACY_VIOLATIONS_APP_NAME, LEGACY_VIOLATIONS_APP_PUBLIC_ID, childOrg.getId());

    LegacyViolationsEditorPage editor = new LegacyViolationsEditorPage();
    LegacyViolationsEditorPageAssertions editorAssertions = new LegacyViolationsEditorPageAssertions(editor);

    // Step 2: Child org — all 3 radios + Allow Override checkbox visible.
    navigateAndWaitForUrl(
        OwnerSummaryPage.editOrganizationUrl(childOrg.getId(),
            LegacyViolationsEditorPage.LEGACY_VIOLATIONS_URL_FRAGMENT),
        LegacyViolationsEditorPage.LEGACY_VIOLATIONS_URL_FRAGMENT);
    editorAssertions.shouldBeVisible();
    Assume.assumeTrue("Legacy Violations feature not available in this environment", editor.isFormRendered());
    editorAssertions.shouldShowAllThreeRadios();
    editorAssertions.shouldShowAllowOverrideCheckbox();

    // Step 3: Root org — "Inherit from parent" NOT shown; Enabled + Disabled visible; checkbox visible.
    // Use ROOT_ORGANIZATION_ID as the URL fragment — the child and root org pages both contain
    // LEGACY_VIOLATIONS_URL_FRAGMENT, so using that fragment alone would short-circuit navigation.
    navigateAndWaitForUrl(
        OwnerSummaryPage.editOrganizationUrl(Organization.ROOT_ORGANIZATION_ID,
            LegacyViolationsEditorPage.LEGACY_VIOLATIONS_URL_FRAGMENT),
        Organization.ROOT_ORGANIZATION_ID);
    editorAssertions.shouldBeVisible();
    editorAssertions.shouldShowOnlyEnabledAndDisabledRadios();
    editorAssertions.shouldShowAllowOverrideCheckbox();

    // Step 4: Application — "Allow configuration to be overridden…" NOT shown.
    navigateAndWaitForUrl(
        OwnerSummaryPage.editApplicationUrl(LEGACY_VIOLATIONS_APP_PUBLIC_ID,
            LegacyViolationsEditorPage.LEGACY_VIOLATIONS_URL_FRAGMENT),
        LEGACY_VIOLATIONS_APP_PUBLIC_ID + LegacyViolationsEditorPage.LEGACY_VIOLATIONS_URL_FRAGMENT);
    editorAssertions.shouldBeVisible();
    editorAssertions.shouldNotShowAllowOverrideCheckbox();
  }

  /**
   * Verify the full Create License Threat Group flow.
   * <p>
   * From the org owner summary, click "Add a Threat Group" in the License Threat Groups tile to
   * open the LTG editor in create mode. Enter a group name, select a threat level, add a license
   * via the transfer list filter, and save. Assert the success overlay appears and dismisses,
   * then navigate back to the owner summary and confirm the new group appears in the local
   * section of the License Threat Groups tile.
   */
  @Test
  @Category(RegressionTest.class)
  public void testCreateLicenseThreatGroup_appearsInTile() {
    Organization org = tempEntity.newOrganization(CREATE_LTG_ORG_NAME);

    navigateAndWaitForUrl(OwnerSummaryPage.url(org.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_LICENSE_THREAT_GROUPS);
    assertions.shouldShowLicenseThreatGroupsTile();

    // Click "Add a Threat Group" — Redux dispatches goToCreateLTG(), navigating to /licenseThreatGroup.
    LicenseThreatGroupEditorPage editor = new LicenseThreatGroupEditorPage();
    LicenseThreatGroupEditorPageAssertions editorAssertions = new LicenseThreatGroupEditorPageAssertions(editor);
    editor.clickAddThreatGroupButton();
    playwrightWaitUntilUrlContains(LicenseThreatGroupEditorPage.CREATE_LTG_URL_FRAGMENT);

    editorAssertions.shouldBeVisible();
    editorAssertions.shouldBeInCreateMode();

    editor.typeGroupName(CREATE_LTG_GROUP_NAME);
    editor.selectThreatLevel(CREATE_LTG_THREAT_LEVEL);
    editor.addFirstLicenseMatchingFilter(CREATE_LTG_LICENSE_FILTER);
    editor.submit();
    editor.waitForSaveSuccess();

    // Navigate back to owner summary and verify the group is in the LTG tile.
    navigateAndWaitForUrl(OwnerSummaryPage.url(org.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_LICENSE_THREAT_GROUPS);
    editorAssertions.shouldHaveLocalThreatGroup(CREATE_LTG_GROUP_NAME);
  }

  /**
   * Clicking Update without making any changes
   * does not trigger a save — instead the form reveals a "no changes to save" validation error.
   * <p>
   * RSC's {@code NxStatefulForm} does NOT disable the HTML submit button.
   * When {@code isDirty = false}, {@code validationErrors = MSG_NO_CHANGES_TO_SAVE}.
   * Clicking Update calls {@code NxStatefulForm.onSubmit} which sets
   * {@code showValidationErrors = true} (making the validation alert visible) and
   * skips the actual save handler entirely.
   */
  @Test
  @Category(RegressionTest.class)
  public void testLegacySaveStatus_updateDisabledWhenNoChanges() {
    Organization org = tempEntity.newOrganization(LV_SAVE_ORG_NAME);

    LegacyViolationsEditorPage editor = new LegacyViolationsEditorPage();
    LegacyViolationsEditorPageAssertions editorAssertions = new LegacyViolationsEditorPageAssertions(editor);

    navigateAndWaitForUrl(
        OwnerSummaryPage.editOrganizationUrl(org.getId(), LegacyViolationsEditorPage.LEGACY_VIOLATIONS_URL_FRAGMENT),
        LegacyViolationsEditorPage.LEGACY_VIOLATIONS_URL_FRAGMENT);
    editorAssertions.shouldBeVisible();
    Assume.assumeTrue("Legacy Violations feature not available in this environment", editor.isFormRendered());

    // Click Update without any changes — validation error must appear; no save occurs.
    editor.submit();
    editorAssertions.shouldShowNoChangesValidationError();
  }

  /**
   * When the Legacy Violations feature is not licensed,
   * the editor shows an {@code NxErrorAlert} instead of the form controls.
   * <p>
   * Route interception strips {@code "policy-grandfathering"} from the
   * {@code GET /rest/product/features} response, forcing {@code selectIsLegacyViolationSupported}
   * to {@code false} regardless of the actual server configuration. A full page reload after
   * registering the intercept resets the Redux store so {@code fetchProductFeaturesIfNeeded}
   * re-fires through the intercept (the features may already be cached from the {@code @Before}
   * dashboard navigation, in which case a simple SPA navigate would reuse the cached value).
   */
  @Test
  @Category(RegressionTest.class)
  public void testLegacyViolationsLicenseGate_errorAlertShownWhenNotSupported() {
    // Intercept GET /rest/product/features (with optional ?timestamp=... query param added by the SPA).
    // Return an empty features array so "policy-grandfathering" is absent, forcing
    // selectIsLegacyViolationSupported=false and the license-gate NxErrorAlert to render.
    // Pattern anchors to the end of the path segment — the ?timestamp= query param is optional.
    page.route(Pattern.compile(".*/rest/product/features([?#][^/]*)?$"),
        route -> route.fulfill(new Route.FulfillOptions()
            .setStatus(200)
            .setContentType("application/json")
            .setBody("[]")));

    navigateAndWaitForUrl(
        OwnerSummaryPage.editOrganizationUrl(Organization.ROOT_ORGANIZATION_ID,
            LegacyViolationsEditorPage.LEGACY_VIOLATIONS_URL_FRAGMENT),
        LegacyViolationsEditorPage.LEGACY_VIOLATIONS_URL_FRAGMENT);

    // Full reload resets the Redux store; fetchProductFeaturesIfNeeded re-fires and the
    // route intercept strips "policy-grandfathering" so the license gate activates.
    page.reload();

    LegacyViolationsEditorPage editor = new LegacyViolationsEditorPage();
    LegacyViolationsEditorPageAssertions editorAssertions = new LegacyViolationsEditorPageAssertions(editor);
    editorAssertions.shouldBeVisible();
    editorAssertions.shouldShowLicenseErrorAlert();
  }

  /**
   * Enabling "Allow configuration to be overridden" on a
   * parent org lets the child org select "Inherit from parent" without restriction.
   * <p>
   * <b>Step 1:</b> Navigate to the parent org's Legacy Violations — ensure Allow Override is
   * checked, then save.<br>
   * <b>Step 2:</b> Navigate to the child org's Legacy Violations — verify no
   * "parent cannot override" alert is shown and the Inherit from parent radio is available.
   */
  @Test
  @Category(RegressionTest.class)
  public void testLegacyAllowOverride_childCanInheritOrOverride() {
    Organization parentOrg = tempEntity.newOrganization(LV_OVERRIDE_PARENT_ORG_NAME);
    Organization childOrg = tempEntity.newOrganization(LV_OVERRIDE_CHILD_ORG_NAME, parentOrg);

    LegacyViolationsEditorPage editor = new LegacyViolationsEditorPage();
    LegacyViolationsEditorPageAssertions editorAssertions = new LegacyViolationsEditorPageAssertions(editor);

    // Step 1: On parent org — select Enabled and force Allow Override on.
    // uncheckAllowOverride() + ensureAllowOverrideChecked() guarantees a real toggle regardless
    // of the server default, so the form is always dirty before submit.
    navigateAndWaitForUrl(
        OwnerSummaryPage.editOrganizationUrl(parentOrg.getId(),
            LegacyViolationsEditorPage.LEGACY_VIOLATIONS_URL_FRAGMENT),
        LegacyViolationsEditorPage.LEGACY_VIOLATIONS_URL_FRAGMENT);
    editorAssertions.shouldBeVisible();
    Assume.assumeTrue("Legacy Violations feature not available in this environment", editor.isFormRendered());
    editor.clickEnabledRadio();
    // DB default for allow_legacy_violation_override is TRUE, so uncheckAllowOverride() first
    // guarantees a real state transition (true→false→true) regardless of the server default.
    // The form is always dirty before submit, making Step 2's assertion non-trivially verified.
    editor.uncheckAllowOverride();
    editor.ensureAllowOverrideChecked();
    editor.submit();
    editor.waitForSaveSuccess();

    // Step 2: On child org — Inherit from parent radio is available; no override-blocked alert.
    navigateAndWaitForUrl(
        OwnerSummaryPage.editOrganizationUrl(childOrg.getId(),
            LegacyViolationsEditorPage.LEGACY_VIOLATIONS_URL_FRAGMENT),
        LegacyViolationsEditorPage.LEGACY_VIOLATIONS_URL_FRAGMENT);
    editorAssertions.shouldBeVisible();
    editorAssertions.shouldShowAllThreeRadios();
    editorAssertions.shouldNotShowParentOverrideDisabledAlert();
  }

  /**
   * Grant and revoke legacy violation status via the
   * application owner summary Actions dropdown.
   * <p>
   * The "Legacy existing violations" menu item is only active when {@code isLegacyViolationEnabled}
   * is {@code true} ({@code selectCalculatedEnabled} from the legacyViolation Redux slice).
   * We first set the org to "Enabled" via the LV editor so the app inherits
   * {@code calculatedEnabled = true} and the Grant menu item is not disabled.
   * <p>
   * <b>Steps 1–3:</b> Navigate to app summary, open Actions, click "Legacy existing violations",
   * confirm in the modal — grants legacy status (PUT request sent).<br>
   * <b>Steps 4–6:</b> Reopen Actions, click "Revoke legacy status", confirm in the modal —
   * revokes legacy status (PUT request sent).
   */
  @Test
  @Category(RegressionTest.class)
  public void testGrantAndRevokeLegacyStatus_viaActionsDropdown() {
    Organization org = tempEntity.newOrganization(LV_GRANT_REVOKE_ORG_NAME);
    tempEntity.newApplication(LV_GRANT_REVOKE_APP_NAME, LV_GRANT_REVOKE_APP_PUBLIC_ID, org.getId());

    // Enable LV on the org so the app inherits calculatedEnabled = true, which is required
    // for the "Legacy existing violations" Actions menu item to be active (not disabled).
    LegacyViolationsEditorPage orgEditor = new LegacyViolationsEditorPage();
    LegacyViolationsEditorPageAssertions orgEditorAssertions = new LegacyViolationsEditorPageAssertions(orgEditor);
    navigateAndWaitForUrl(
        OwnerSummaryPage.editOrganizationUrl(org.getId(), LegacyViolationsEditorPage.LEGACY_VIOLATIONS_URL_FRAGMENT),
        LegacyViolationsEditorPage.LEGACY_VIOLATIONS_URL_FRAGMENT);
    orgEditorAssertions.shouldBeVisible();
    Assume.assumeTrue("Legacy Violations feature not available in this environment", orgEditor.isFormRendered());
    orgEditor.clickEnabledRadio();
    orgEditor.submit();
    orgEditor.waitForSaveSuccess();

    // Navigate to app owner summary.
    navigateAndWaitForUrl(OwnerSummaryPage.applicationUrl(LV_GRANT_REVOKE_APP_PUBLIC_ID),
        OwnerSummaryPage.APP_URL_FRAGMENT + LV_GRANT_REVOKE_APP_PUBLIC_ID);
    assertions.shouldBeVisible();

    // Steps 1–3: Grant legacy status.
    ownerSummary.openOwnerActionsDropdown();
    ownerSummary.clickGrantLegacyViolationsMenuItem();

    LegacyViolationGrantModalPage grantModal = new LegacyViolationGrantModalPage();
    LegacyViolationGrantModalPageAssertions grantAssertions = new LegacyViolationGrantModalPageAssertions(grantModal);
    grantAssertions.shouldBeVisible();
    grantModal.submit();
    grantModal.waitForModalToClose();

    // Steps 4–6: Revoke legacy status (navigate back, reopen Actions).
    navigateAndWaitForUrl(OwnerSummaryPage.applicationUrl(LV_GRANT_REVOKE_APP_PUBLIC_ID),
        OwnerSummaryPage.APP_URL_FRAGMENT + LV_GRANT_REVOKE_APP_PUBLIC_ID);
    assertions.shouldBeVisible();

    ownerSummary.openOwnerActionsDropdown();
    ownerSummary.clickRevokeLegacyViolationMenuItem();

    RevokeLegacyViolationModalPage revokeModal = new RevokeLegacyViolationModalPage();
    RevokeLegacyViolationModalPageAssertions revokeAssertions =
        new RevokeLegacyViolationModalPageAssertions(revokeModal);
    revokeAssertions.shouldBeVisible();
    revokeModal.submit();
    revokeModal.waitForModalToClose();
  }
}
