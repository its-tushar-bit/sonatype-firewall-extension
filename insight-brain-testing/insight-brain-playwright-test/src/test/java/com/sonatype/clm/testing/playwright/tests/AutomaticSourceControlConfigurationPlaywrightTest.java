/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.AutomaticSourceControlConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.AutomaticSourceControlConfigurationPageAssertions;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticSourceControlConfigurationDAO;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Feature-flag-gate note: the row's original claim ("Access when
 * {@code selectIsAutomaticScmConfigurationEnabled=false} — {@code LicenseLockScreen} renders
 * instead of the form") does NOT match the code — the route (see {@code configuration/route.js})
 * mounts the form component unconditionally; only the sidebar menu link is feature-flag-gated
 * ({@code SystemPreferencesMenu.jsx}). See divergences log.
 */
public class AutomaticSourceControlConfigurationPlaywrightTest
    extends AbstractIqUiTest
{
  private AutomaticSourceControlConfigurationPage scmPage;

  private AutomaticSourceControlConfigurationPageAssertions scmAssertions;

  @Before
  public void openAsAdmin() {
    // Seed the toggle to a known-off state so every test starts identical — avoids branching
    // assertions on the toggle's live value. DAO write is fork-wide state; @After resets it.
    lookup(AutomaticSourceControlConfigurationDAO.class).setSourceControlConfigurationEnabled(false);

    playwrightLoginAdminAt(AutomaticSourceControlConfigurationPage.url());
    scmPage = new AutomaticSourceControlConfigurationPage();
    scmAssertions = new AutomaticSourceControlConfigurationPageAssertions(scmPage);
  }

  @After
  public void resetSourceControlConfiguration() {
    // Reset any mutation the test made — the DAO write is fork-wide state shared across tests.
    lookup(AutomaticSourceControlConfigurationDAO.class).setSourceControlConfigurationEnabled(false);
  }

  @Test
  @Category(RegressionTest.class)
  public void testAutomaticScm_pageRendersWithFormControls() {
    scmAssertions.shouldRenderFormLayout();
    scmAssertions.shouldHaveToggleUnchecked();
  }

  @Test
  @Category(RegressionTest.class)
  public void testAutomaticScm_cancelTracksDirtyAndResetsToggle() {
    scmAssertions.shouldRenderFormLayout();
    scmAssertions.shouldHaveToggleUnchecked();
    scmAssertions.shouldHaveCancelButtonDisabled();

    scmPage.toggleLabel().click();
    scmAssertions.shouldHaveToggleChecked();
    scmAssertions.shouldHaveCancelButtonEnabled();

    scmPage.cancelButton().click();
    scmAssertions.shouldHaveToggleUnchecked();
    scmAssertions.shouldHaveCancelButtonDisabled();
  }

  @Test
  @Category(RegressionTest.class)
  public void testAutomaticScm_toggleAndUpdatePersistsAcrossReload() {
    scmAssertions.shouldHaveToggleUnchecked();

    scmPage.toggleLabel().click();
    assertThat(scmPage.updateButton()).isEnabled();
    scmPage.updateButton().click();
    waitForSubmitMaskSuccess();
    scmAssertions.shouldHaveCancelButtonDisabled();

    playwrightRefreshOrOpen(AutomaticSourceControlConfigurationPage.url());
    scmAssertions.shouldRenderFormLayout();
    scmAssertions.shouldHaveToggleChecked();
  }
}
