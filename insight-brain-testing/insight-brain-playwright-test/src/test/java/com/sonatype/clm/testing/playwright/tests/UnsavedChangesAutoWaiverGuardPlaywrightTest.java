/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.AutoWaiversPage;
import com.sonatype.clm.testing.playwright.pages.UnsavedChangesModalComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Regression test for the auto-waiver UnsavedChanges guard — Cancel (Stay) path.
 * AutoWaiverModal manages its own unsaved-changes state (autoWaiverModalSlice), independent of the global modal.
 * The Continue (discard) path is covered by {@code OrgsAndPoliciesAutoWaiversPlaywrightTest}.
 */
public class UnsavedChangesAutoWaiverGuardPlaywrightTest
    extends AbstractIqUiTest
{
  private AutoWaiversPage autoWaiversPage;

  private UnsavedChangesModalComponent unsavedChangesModal;

  @BeforeEach
  public void enableAutoWaiversFeature() {
    autoWaiversPage = new AutoWaiversPage();
    unsavedChangesModal = new UnsavedChangesModalComponent();

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
    unsavedChangesModal.continueIfOpen();
  }

  /** Cancel on UnsavedChangesModal keeps the auto-waiver modal open (stay / continue editing path). */
  @Test
  @Tag("regression")
  public void testAutoWaiverDirtyModal_cancelOnUnsavedChangesKeepsModalOpen() {
    Organization org = tempEntity.newOrganization();
    playwrightRefreshOrOpen(AutoWaiversPage.url(org.getPublicId()));
    playwrightLogin();

    autoWaiversPage.newAutoWaiverButton().click();
    autoWaiversPage.modal().waitFor();

    autoWaiversPage.upgradePathCheckbox().click();

    // Modal's Cancel triggers the local UnsavedChangesModal (not the global one).
    autoWaiversPage.cancelButton().click();
    assertThat(unsavedChangesModal.container()).isVisible();

    unsavedChangesModal.cancelButton().click();
    assertThat(unsavedChangesModal.container()).isHidden();
    assertThat(autoWaiversPage.modal()).isVisible();
  }
}
