/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.clm.testing.playwright.pages.ApplicationCategoryEditorPage;
import com.sonatype.clm.testing.playwright.pages.ApplicationCategoryEditorPageAssertions;
import com.sonatype.clm.testing.playwright.pages.AutoWaiversPage;
import com.sonatype.clm.testing.playwright.pages.AutoWaiversPageAssertions;
import com.sonatype.clm.testing.playwright.pages.ComponentLabelEditorPage;
import com.sonatype.clm.testing.playwright.pages.ComponentLabelEditorPageAssertions;
import com.sonatype.clm.testing.playwright.pages.LicenseThreatGroupEditorPage;
import com.sonatype.clm.testing.playwright.pages.LicenseThreatGroupEditorPageAssertions;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPageAssertions;
import com.sonatype.clm.testing.playwright.pages.PolicyEditorPage;
import com.sonatype.clm.testing.playwright.pages.UnsavedChangesModalComponent;
import com.sonatype.clm.testing.playwright.testdatamanager.TestDataManager;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class OrgsAndPoliciesAutoWaiversPlaywrightTest
    extends AbstractIqUiTest
{
  private record Data(
      String pageHeading,
      String pageSubtitle,
      String tableSectionHeading,
      String emptyTableMessage,
      String newAutoWaiverButtonText,
      String newAutoWaiverModalHeading,
      String validationErrorNoneSelected,
      String upgradePathCheckboxLabel,
      String reachabilityCheckboxLabel,
      String maxConfigurationsTooltip,
      String inheritedGroupHeaderPrefix,
      String editInheritedTooltip,
      String deleteInheritedTooltip,
      String deleteModalWarningText,
      String deleteModalUndoneText,
      String previewButtonText,
      String enterpriseBannerText,
      String previewRowScope,
      String detailsHeading,
      String noPathForwardScope,
      String notReachableScope,
      int maxLocalAutoWaivers,
      String editModalHeadingPattern,
      int defaultThreatLevel)
  {
  }

  private static final Data DATA = TestDataManager.load("auto-waivers", Data.class);

  private static final LocatorAssertions.IsVisibleOptions VISIBLE_OPTS =
      new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  private AutoWaiversPage autoWaiversPage;

  private AutoWaiversPageAssertions assertions;

  @BeforeEach
  public void enableAutoWaiversFeature() {
    autoWaiversPage = new AutoWaiversPage();
    assertions = new AutoWaiversPageAssertions(autoWaiversPage);

    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(true);
    setFeatures(
        LicensedFeature.POLICY_MANAGEMENT,
        LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.AUTO_WAIVER_MANAGEMENT,
        LicensedFeature.DEVELOPER_DASHBOARD);
  }

  @AfterEach
  public void resetAutoWaiversFeature() {
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(false);
  }

  @Test
  @Tag("regression")
  public void testAutoWaiversPage_rendersPageWithTableAndNewButton() {
    Organization org = seedOrgWithOneWaiver();
    playwrightRefreshOrOpen(AutoWaiversPage.url(org.getPublicId()));
    playwrightLogin();

    assertions.shouldRenderPage();
    assertions.shouldShowTableColumns();

    Locator firstRow = autoWaiversPage.tableRows().first();
    assertThat(firstRow).isVisible();
    assertions.shouldShowRowContent(firstRow, DATA.defaultThreatLevel(), DATA.noPathForwardScope());
  }

  @Test
  @Tag("regression")
  public void testAutoWaiversPage_licenseGateShowsLockScreen() {
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(false);

    Organization org = seedOrgWithNoWaivers();
    playwrightRefreshOrOpen(AutoWaiversPage.url(org.getPublicId()));
    playwrightLogin();

    assertions.shouldShowMissingLicenseAlert();
  }

  @Test
  @Tag("regression")
  public void testAutoWaiversPage_enterprisePreviewMode() {
    setFeatures(
        LicensedFeature.POLICY_MANAGEMENT,
        LicensedFeature.POLICY_READ_ONLY);

    Organization org = seedOrgWithNoWaivers();
    playwrightRefreshOrOpen(AutoWaiversPage.url(org.getPublicId()));
    playwrightLogin();

    assertions.shouldShowEnterprisePreviewMode();
  }

  @Test
  @Tag("regression")
  public void testAutoWaiversPage_createModalValidationAndHappyPath() {
    Organization org = seedOrgWithNoWaivers();
    playwrightRefreshOrOpen(AutoWaiversPage.url(org.getPublicId()));
    playwrightLogin();

    autoWaiversPage.newAutoWaiverButton().click();
    autoWaiversPage.modal().waitFor();

    assertions.shouldShowNewAutoWaiverModal();
    assertions.shouldShowScopeDropdownDisabled();
    assertions.shouldShowValidationErrorAfterCreateAttempt();

    autoWaiversPage.upgradePathCheckbox().click();
    assertions.shouldShowScopeDropdownDisabled();

    autoWaiversPage.createButton().click();

    assertions.shouldHideModal();
    assertions.shouldShowWaiverRows();
    assertions.shouldShowRowContent(autoWaiversPage.tableRows().first(), DATA.defaultThreatLevel(),
        DATA.noPathForwardScope());

    autoWaiversPage.newAutoWaiverButton().click();
    autoWaiversPage.modal().waitFor();
    autoWaiversPage.upgradePathCheckbox().click();
    autoWaiversPage.cancelButton().click();

    UnsavedChangesModalComponent unsavedModal = new UnsavedChangesModalComponent();
    assertThat(unsavedModal.container()).isVisible();
    unsavedModal.continueButton().click();
    assertions.shouldHideModal();
  }

  @Test
  @Tag("regression")
  public void testAutoWaiversPage_maxThreeWaiversDisablesNewButton() {
    Organization org = seedOrgWithNoWaivers();
    seedMaxLocalWaivers(org.getId());

    playwrightRefreshOrOpen(AutoWaiversPage.url(org.getPublicId()));
    playwrightLogin();

    autoWaiversPage.container().waitFor();
    assertions.shouldShowNewAutoWaiverButtonDisabled(DATA.maxLocalAutoWaivers());
    assertions.shouldShowMaxConfigTooltip(DATA.maxConfigurationsTooltip());
  }

  @Test
  @Tag("regression")
  public void testAutoWaiversPage_inheritedWaiversGroupedAndDeleteRestricted() {
    Organization parentOrg = seedOrgWithOneWaiver();
    Organization childOrg = tempEntity.newOrganization("child-org-aw-" + System.nanoTime(), parentOrg);

    playwrightRefreshOrOpen(AutoWaiversPage.url(childOrg.getPublicId()));
    playwrightLogin();

    autoWaiversPage.container().waitFor();

    assertions.shouldShowInheritedGroupHeader(parentOrg.getName());

    Locator inheritedRow = autoWaiversPage.tableRows().first();
    assertThat(inheritedRow).isVisible();
    assertions.shouldShowDisabledDeleteOnInheritedRow(inheritedRow);
    assertions.shouldShowDeleteTooltipOnInheritedRow(inheritedRow, DATA.deleteInheritedTooltip());
  }

  @Test
  @Tag("regression")
  public void testAutoWaiversPage_deleteLocalWaiverWithConfirmationModal() {
    Organization org = seedOrgWithOneWaiver();

    playwrightRefreshOrOpen(AutoWaiversPage.url(org.getPublicId()));
    playwrightLogin();

    autoWaiversPage.container().waitFor();

    assertions.deleteFirstWaiverAndCancel();
    assertions.deleteFirstWaiverAndConfirm();

    assertions.shouldShowEmptyState(DATA.emptyTableMessage());
  }

  @Test
  @Tag("regression")
  public void testAutoWaiversPage_viewEditNavigatesToDetailsRoute() {
    Organization parentOrg = seedOrgWithOneWaiver();
    Organization childOrg = tempEntity.newOrganization("child-org-ve-" + System.nanoTime(), parentOrg);
    seedLocalWaiver(childOrg.getId(), 7, true, false);

    playwrightRefreshOrOpen(AutoWaiversPage.url(childOrg.getPublicId()));
    playwrightLogin();

    autoWaiversPage.container().waitFor();

    Locator localRow = autoWaiversPage.tableRows().first();

    assertions.clickViewEditAndWaitForDetails(localRow);

    assertions.shouldShowDetailsEditAndDeleteButtons();

    assertions.clickEditAndWaitForEditModal();
    autoWaiversPage.upgradePathCheckbox().click();
    assertions.submitEditModalAndWaitForDetails();

    playwrightRefreshOrOpen(AutoWaiversPage.url(childOrg.getPublicId()));
    autoWaiversPage.container().waitFor();

    assertions.shouldShowInheritedGroupHeader(parentOrg.getName());
    Locator inheritedRow = autoWaiversPage.tableRows().nth(1);
    assertions.clickViewEditAndWaitForDetails(inheritedRow);

    assertions.shouldShowDetailsEditAndDeleteButtonsDisabledForInherited(
        DATA.editInheritedTooltip(), DATA.deleteInheritedTooltip());
  }

  // Distinct from testAutoWaiversPage_enterprisePreviewMode: that test sets only POLICY_MANAGEMENT +
  // POLICY_READ_ONLY; this one enables ALL features except AUTO_WAIVER_MANAGEMENT, exercising the
  // enterprise-gating path when the full feature set is otherwise licensed.
  @Test
  @Tag("regression")
  public void testEnterpriseGating_autoWaiversPage_showsPreviewBanner() {
    setMissingFeatures(LicensedFeature.AUTO_WAIVER_MANAGEMENT);
    Organization org = tempEntity.newOrganization("tier-gate-aw-" + System.nanoTime());
    playwrightRefreshOrOpen(AutoWaiversPage.url(org.getPublicId()));
    playwrightLogin();
    assertions.shouldShowEnterprisePreviewMode();
  }

  @Test
  @Tag("regression")
  public void testEnterpriseGating_policyEditor_showsLockIcon() {
    setMissingFeatures(LicensedFeature.CUSTOM_POLICIES);
    Organization org = tempEntity.newOrganization("tier-gate-pe-" + System.nanoTime());
    Policy policy = tempEntity.newPolicy(org.getId(), "tier-gate-policy", 5);
    PolicyEditorPage policyEditor = new PolicyEditorPage();
    playwrightRefreshOrOpen(PolicyEditorPage.url(org, policy));
    playwrightLogin();
    assertThat(policyEditor.customModeButtonLockIcon()).isVisible(VISIBLE_OPTS);
  }

  @Test
  @Tag("regression")
  public void testEnterpriseGating_componentLabels_showsAddButtonInPreviewMode() {
    setMissingFeatures(LicensedFeature.CUSTOM_COMPONENT_LABELS);
    Organization org = tempEntity.newOrganization("tier-gate-cl-" + System.nanoTime());
    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    playwrightRefreshOrOpen(OwnerSummaryPage.url(org.getId()));
    playwrightLogin();
    new OwnerSummaryPageAssertions(ownerSummary).shouldBeVisible();
    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_COMPONENT_LABELS);
    new ComponentLabelEditorPageAssertions(new ComponentLabelEditorPage()).shouldShowAddLabelButtonInPreviewMode();
  }

  @Test
  @Tag("regression")
  public void testEnterpriseGating_licenseThreatGroups_showsAddButtonInPreviewMode() {
    setMissingFeatures(LicensedFeature.CUSTOM_LICENSE_THREAT_GROUPS);
    Organization org = tempEntity.newOrganization("tier-gate-ltg-" + System.nanoTime());
    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    playwrightRefreshOrOpen(OwnerSummaryPage.url(org.getId()));
    playwrightLogin();
    new OwnerSummaryPageAssertions(ownerSummary).shouldBeVisible();
    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_LICENSE_THREAT_GROUPS);
    new LicenseThreatGroupEditorPageAssertions(new LicenseThreatGroupEditorPage())
        .shouldShowAddThreatGroupButtonInPreviewMode();
  }

  @Test
  @Tag("regression")
  public void testEnterpriseGating_applicationCategories_showsAddButtonInPreviewMode() {
    setMissingFeatures(LicensedFeature.CUSTOM_APPLICATION_CATEGORIES);
    Organization org = tempEntity.newOrganization("tier-gate-ac-" + System.nanoTime());
    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    playwrightRefreshOrOpen(OwnerSummaryPage.url(org.getId()));
    playwrightLogin();
    new OwnerSummaryPageAssertions(ownerSummary).shouldBeVisible();
    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_APP_CATEGORIES);
    new ApplicationCategoryEditorPageAssertions(new ApplicationCategoryEditorPage())
        .shouldShowAddCategoryButtonInPreviewMode();
  }

  private Organization seedOrgWithNoWaivers() {
    return tempEntity.newOrganization("aw-org-" + System.nanoTime());
  }

  private Organization seedOrgWithOneWaiver() {
    Organization org = tempEntity.newOrganization("aw-org-" + System.nanoTime());
    tempEntity.newAutoPolicyWaiver(org.getId(), 7, false, true);
    return org;
  }

  private void seedLocalWaiver(String ownerId, int threatLevel, boolean reachable, boolean pathForward) {
    tempEntity.newAutoPolicyWaiver(ownerId, threatLevel, reachable, pathForward);
  }

  private void seedMaxLocalWaivers(String ownerId) {
    tempEntity.newAutoPolicyWaiver(ownerId, 7, true, true);
    tempEntity.newAutoPolicyWaiver(ownerId, 6, true, false);
    tempEntity.newAutoPolicyWaiver(ownerId, 5, false, true);
  }
}
