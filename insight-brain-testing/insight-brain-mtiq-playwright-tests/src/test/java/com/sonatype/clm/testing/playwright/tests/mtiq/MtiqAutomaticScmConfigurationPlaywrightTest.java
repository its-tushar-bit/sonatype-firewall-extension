/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import com.sonatype.clm.testing.playwright.categories.MtiqTest;
import com.sonatype.clm.testing.playwright.mtiq.AbstractMtiqUiTest;
import com.sonatype.clm.testing.playwright.pages.AutomaticSourceControlConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.AutomaticSourceControlConfigurationPageAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import com.microsoft.playwright.assertions.LocatorAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Category(MtiqTest.class)
public class MtiqAutomaticScmConfigurationPlaywrightTest
    extends AbstractMtiqUiTest
{
  private AutomaticSourceControlConfigurationPage scmPage;

  private AutomaticSourceControlConfigurationPageAssertions scmAssertions;

  @Before
  public void loginAsAdmin() {
    // MTIQFeatureService.setConfigurationBasedFeatures() sets AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_ENABLED=true
    // at tenant provisioning. Each fresh tenant starts with the toggle ON — both tests verify this
    // before acting, so no manual DAO reset is needed.
    playwrightLoginAdminAt(AutomaticSourceControlConfigurationPage.url());
    scmPage = new AutomaticSourceControlConfigurationPage();
    scmAssertions = new AutomaticSourceControlConfigurationPageAssertions(scmPage);
  }

  @Test
  public void testMtiqAutomaticScm_toggleAndUpdatePersistsAcrossReload() {
    scmAssertions.shouldHaveToggleChecked();

    scmPage.toggleLabel().click();
    assertThat(scmPage.updateButton()).isEnabled(
        new LocatorAssertions.IsEnabledOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    scmPage.updateButton().click();
    waitForSubmitMaskSuccess();
    scmAssertions.shouldHaveCancelButtonDisabled();

    playwrightRefreshOrOpen(AutomaticSourceControlConfigurationPage.url());
    scmAssertions.shouldRenderFormLayout();
    scmAssertions.shouldHaveToggleUnchecked();
  }

  @Test
  public void testMtiqAutomaticScm_cancelTracksDirtyAndResetsToggle() {
    scmAssertions.shouldRenderFormLayout();
    scmAssertions.shouldHaveToggleChecked();
    scmAssertions.shouldHaveCancelButtonDisabled();

    scmPage.toggleLabel().click();
    scmAssertions.shouldHaveToggleUnchecked();
    scmAssertions.shouldHaveCancelButtonEnabled();

    scmPage.cancelButton().click();
    scmAssertions.shouldHaveToggleChecked();
    scmAssertions.shouldHaveCancelButtonDisabled();
  }
}
