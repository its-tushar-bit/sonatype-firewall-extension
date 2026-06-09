/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.microsoft.playwright.Locator;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.AutoWaiversPage;
import com.sonatype.clm.testing.playwright.pages.AutoWaiversPageAssertions;
import com.sonatype.clm.testing.playwright.pages.UnsavedChangesModalComponent;
import com.sonatype.clm.testing.playwright.testdatamanager.TestDataManager;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

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

  private AutoWaiversPage autoWaiversPage;

  private AutoWaiversPageAssertions assertions;

  @Before
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

  @After
  public void resetAutoWaiversFeature() {
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(false);
  }

  @Test
  @Category(RegressionTest.class)
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
  @Category(RegressionTest.class)
  public void testAutoWaiversPage_licenseGateShowsLockScreen() {
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(false);

    Organization org = seedOrgWithNoWaivers();
    playwrightRefreshOrOpen(AutoWaiversPage.url(org.getPublicId()));
    playwrightLogin();

    assertions.shouldShowMissingLicenseAlert();
  }

  @Test
  @Category(RegressionTest.class)
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
  @Category(RegressionTest.class)
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
  @Category(RegressionTest.class)
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
  @Category(RegressionTest.class)
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
  @Category(RegressionTest.class)
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
  @Category(RegressionTest.class)
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
