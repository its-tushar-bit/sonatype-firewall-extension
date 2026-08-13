/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.InnerSourceRepositoryEditorPage;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.RepositoryFormat;
import com.sonatype.insight.license.model.LicensedFeature;

import com.microsoft.playwright.assertions.LocatorAssertions.HasAttributeOptions;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;

/**
 * Regression tests for the Inner Source Repository Configuration editor
 * ({@code /repositoryBaseConfigurations} route under the org/app edit shell).
 * Requires the {@code INNER_SOURCE_REPOSITORIES} license feature.
 * <p>
 * Tests that need {@code allowChange=false} seed a parent org with
 * {@code allowRepositoryConnectionOverride=false} via {@link OrganizationDAO#update} and create
 * the target org as a child of that parent — no endpoint mocking required.
 */
public class InnerSourceRepositoryEditorPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String ORG_PREFIX = "ISRepo-Org";

  private static final String APP_PREFIX = "ISRepo-App";

  private static final String APP_PUBLIC_ID_PREFIX = "isrepo-app-";

  private static final String REPO_BASE_URL = "http://artifactory.example.com/";

  private static final String REPO_USERNAME = "test-user";

  private static final char[] REPO_PASSWORD = "test-pass".toCharArray();

  @Before
  public void enableInnerSourceAndLogin() {
    setFeatures(LicensedFeature.INNER_SOURCE_REPOSITORIES);
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
  }

  @Test
  @Category(RegressionTest.class)
  public void testEditorRendersRadios_inheritHiddenAtRootOrg() {
    Organization childOrg = tempEntity.newOrganization(ORG_PREFIX + "-" + TemporaryEntity.uuid());
    InnerSourceRepositoryEditorPage editor = new InnerSourceRepositoryEditorPage();

    navigateAndWaitForUrl(
        InnerSourceRepositoryEditorPage.orgUrl(ROOT_ORGANIZATION_ID),
        ROOT_ORGANIZATION_ID + InnerSourceRepositoryEditorPage.URL_FRAGMENT);

    assertThat(editor.container()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    assertThat(editor.inheritRadio().label()).isHidden();
    assertThat(editor.disableRadio().label()).isVisible();
    assertThat(editor.enableRadio().label()).isVisible();

    navigateAndWaitForUrl(
        InnerSourceRepositoryEditorPage.orgUrl(childOrg.getId()),
        childOrg.getId() + InnerSourceRepositoryEditorPage.URL_FRAGMENT);

    assertThat(editor.container()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    assertThat(editor.inheritRadio().label()).isVisible();
    assertThat(editor.disableRadio().label()).isVisible();
    assertThat(editor.enableRadio().label()).isVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testAllowOverride_visibleForOrg_hiddenForApp_andLockedByParent() {
    String suffix = TemporaryEntity.uuid();
    Organization lockedParent = tempEntity.newOrganization(ORG_PREFIX + "-lp-" + suffix);
    lockedParent.setAllowRepositoryConnectionOverride(false);
    lookup(OrganizationDAO.class).update(lockedParent);

    Organization org = tempEntity.newOrganization(ORG_PREFIX + "-aov-" + suffix, lockedParent);
    Application app = tempEntity.newApplication(
        APP_PREFIX + "-aov-" + suffix,
        APP_PUBLIC_ID_PREFIX + "aov-" + suffix,
        org.getId());
    InnerSourceRepositoryEditorPage editor = new InnerSourceRepositoryEditorPage();

    navigateAndWaitForUrl(
        InnerSourceRepositoryEditorPage.orgUrl(org.getId()),
        org.getId() + InnerSourceRepositoryEditorPage.URL_FRAGMENT);

    assertThat(editor.container()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    assertThat(editor.allowOverrideCheckbox().label()).isVisible();

    navigateAndWaitForUrl(
        InnerSourceRepositoryEditorPage.appUrl(app.getPublicId()),
        app.getPublicId() + InnerSourceRepositoryEditorPage.URL_FRAGMENT);

    assertThat(editor.container()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    assertThat(editor.allowOverrideCheckbox().label()).isHidden();

    navigateAndWaitForUrl(
        InnerSourceRepositoryEditorPage.orgUrl(org.getId()),
        org.getId() + InnerSourceRepositoryEditorPage.URL_FRAGMENT);

    assertThat(editor.container()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    assertThat(editor.lockedByParentAlert()).isVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testAddButton_hiddenWhenDisable_andEmptyStateWhenEnable() {
    Organization org = tempEntity.newOrganization(ORG_PREFIX + "-ab-" + TemporaryEntity.uuid());
    InnerSourceRepositoryEditorPage editor = new InnerSourceRepositoryEditorPage();

    navigateAndWaitForUrl(
        InnerSourceRepositoryEditorPage.orgUrl(org.getId()),
        org.getId() + InnerSourceRepositoryEditorPage.URL_FRAGMENT);

    assertThat(editor.container()).isVisible(PlaywrightTiming.VISIBLE_OPTS);

    editor.disableRadio().label().click();
    assertThat(editor.addRepositoryButton()).isHidden();

    editor.enableRadio().label().click();
    assertThat(editor.addRepositoryButton()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    assertThat(editor.addRepositoryButton()).hasAttribute("aria-disabled", "true");
    assertThat(editor.emptyRepositoryListMessage()).isVisible();

    editor.addRepositoryButton().hover();
    assertThat(editor.addButtonNotEnabledTooltip()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
  }

  @Test
  @Category(RegressionTest.class)
  public void testAddButton_ariaDisabledWithLockedTooltip_whenAllowChangeFalse() {
    String suffix = TemporaryEntity.uuid();
    Organization lockedParent = tempEntity.newOrganization(ORG_PREFIX + "-lp-lock-" + suffix);
    lockedParent.setAllowRepositoryConnectionOverride(false);
    lookup(OrganizationDAO.class).update(lockedParent);

    Organization org = tempEntity.newOrganization(ORG_PREFIX + "-lock-" + suffix, lockedParent);
    org.setRepositoryConnectionEnabled(true);
    lookup(OrganizationDAO.class).update(org);

    InnerSourceRepositoryEditorPage editor = new InnerSourceRepositoryEditorPage();

    navigateAndWaitForUrl(
        InnerSourceRepositoryEditorPage.orgUrl(org.getId()),
        org.getId() + InnerSourceRepositoryEditorPage.URL_FRAGMENT);

    assertThat(editor.container()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    assertThat(editor.lockedByParentAlert()).isVisible();
    assertThat(editor.addRepositoryButton()).isVisible();
    assertThat(editor.addRepositoryButton()).hasAttribute("aria-disabled", "true");

    editor.addRepositoryButton().hover();
    assertThat(editor.addButtonLockedByParentTooltip()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
  }

  @Test
  @Category(RegressionTest.class)
  public void testEditRepositoryModal_opensWithExistingValues() {
    Organization org = tempEntity.newOrganization(ORG_PREFIX + "-edit-" + TemporaryEntity.uuid());
    tempEntity.newRepositoryConnection(
        org.getId(), REPO_BASE_URL, RepositoryFormat.GENERIC, REPO_USERNAME, REPO_PASSWORD);
    InnerSourceRepositoryEditorPage editor = new InnerSourceRepositoryEditorPage();

    navigateAndWaitForUrl(
        InnerSourceRepositoryEditorPage.orgUrl(org.getId()),
        org.getId() + InnerSourceRepositoryEditorPage.URL_FRAGMENT);

    assertThat(editor.container()).isVisible(PlaywrightTiming.VISIBLE_OPTS);

    editor.enableRadio().label().click();
    editor.submitButton().click();
    waitForSubmitMask();
    assertThat(editor.editButtonForRepository(0))
        .hasAttribute("aria-disabled", "false",
            new HasAttributeOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));

    editor.editButtonForRepository(0).click();

    assertThat(editor.configModal()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    assertThat(editor.configModalHeading("Edit")).isVisible();
    assertThat(editor.configModalBaseUrlInput()).hasValue(REPO_BASE_URL);
    assertThat(editor.configModalUpdateButton()).isVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testDeleteRepositoryModal_warningShownAndConfirmRemovesEntry() {
    Organization org = tempEntity.newOrganization(ORG_PREFIX + "-del-" + TemporaryEntity.uuid());
    tempEntity.newRepositoryConnection(
        org.getId(), REPO_BASE_URL, RepositoryFormat.GENERIC, REPO_USERNAME, REPO_PASSWORD);
    InnerSourceRepositoryEditorPage editor = new InnerSourceRepositoryEditorPage();

    navigateAndWaitForUrl(
        InnerSourceRepositoryEditorPage.orgUrl(org.getId()),
        org.getId() + InnerSourceRepositoryEditorPage.URL_FRAGMENT);

    assertThat(editor.container()).isVisible(PlaywrightTiming.VISIBLE_OPTS);

    editor.enableRadio().label().click();
    editor.submitButton().click();
    waitForSubmitMask();
    assertThat(editor.deleteButtonForRepository(0))
        .hasAttribute("aria-disabled", "false",
            new HasAttributeOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));

    editor.deleteButtonForRepository(0).click();

    assertThat(editor.deleteModal()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    assertThat(editor.deleteModalWarningAlert()).isVisible();
    assertThat(editor.deleteModalOkButton()).isVisible();
    assertThat(editor.deleteModalCancelButton()).isVisible();

    editor.deleteModalOkButton().click();
    waitForSubmitMask();

    assertThat(editor.deleteModal()).isHidden();
    assertThat(editor.emptyRepositoryListMessage()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
  }

  @Test
  @Category(RegressionTest.class)
  public void testAddRepositoryModal_fieldsAndAuthToggle_testButtonDisabledWhenUrlEmpty() {
    Organization org = tempEntity.newOrganization(ORG_PREFIX + "-add-" + TemporaryEntity.uuid());
    InnerSourceRepositoryEditorPage editor = new InnerSourceRepositoryEditorPage();

    navigateAndWaitForUrl(
        InnerSourceRepositoryEditorPage.orgUrl(org.getId()),
        org.getId() + InnerSourceRepositoryEditorPage.URL_FRAGMENT);

    assertThat(editor.container()).isVisible(PlaywrightTiming.VISIBLE_OPTS);

    editor.enableRadio().label().click();
    editor.submitButton().click();
    waitForSubmitMask();
    assertThat(editor.addRepositoryButton())
        .hasAttribute("aria-disabled", "false",
            new HasAttributeOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));

    editor.addRepositoryButton().click();

    assertThat(editor.configModal()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    assertThat(editor.configModalHeading("Add")).isVisible();
    assertThat(editor.configModalCreateButton()).isVisible();
    assertThat(editor.configModalFormatSelect()).isVisible();
    assertThat(editor.configModalBaseUrlInput()).isVisible();
    assertThat(editor.configModalAnonymousRadio().label()).isVisible();
    assertThat(editor.configModalCredentialsRadio().label()).isVisible();

    editor.configModalAnonymousRadio().label().click();
    assertThat(editor.configModalAuthFieldset()).isHidden();

    editor.configModalCredentialsRadio().label().click();
    assertThat(editor.configModalAuthFieldset()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
    assertThat(editor.configModalUsernameInput()).isVisible();
    assertThat(editor.configModalPasswordInput()).isVisible();

    assertThat(editor.configModalBaseUrlInput()).hasValue("");
    assertThat(editor.configModalTestButton()).hasAttribute("aria-disabled", "true");
  }

  @Test
  @Category(RegressionTest.class)
  public void testSaveConfiguration_submitMaskShown() {
    Organization org = tempEntity.newOrganization(ORG_PREFIX + "-save-" + TemporaryEntity.uuid());
    tempEntity.newRepositoryConnection(
        org.getId(), REPO_BASE_URL, RepositoryFormat.GENERIC, REPO_USERNAME, REPO_PASSWORD);
    InnerSourceRepositoryEditorPage editor = new InnerSourceRepositoryEditorPage();

    navigateAndWaitForUrl(
        InnerSourceRepositoryEditorPage.orgUrl(org.getId()),
        org.getId() + InnerSourceRepositoryEditorPage.URL_FRAGMENT);

    assertThat(editor.container()).isVisible(PlaywrightTiming.VISIBLE_OPTS);

    editor.enableRadio().label().click();
    editor.submitButton().click();
    waitForSubmitMask();
    waitForSubmitMaskSuccess();

    assertThat(editor.container()).isVisible(PlaywrightTiming.VISIBLE_OPTS);
  }
}
