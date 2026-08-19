/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.WaivedComponentUpgradesPage;
import com.sonatype.clm.testing.playwright.pages.WaivedComponentUpgradesPageAssertions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class WaivedComponentUpgradesPlaywrightTest
    extends AbstractIqUiTest
{
  private WaivedComponentUpgradesPage configPage;

  private WaivedComponentUpgradesPageAssertions configAssertions;

  @BeforeEach
  public void setUp() {
    playwrightRefreshOrOpen(WaivedComponentUpgradesPage.url());
    playwrightLogin();
    configPage = new WaivedComponentUpgradesPage();
    configAssertions = new WaivedComponentUpgradesPageAssertions(configPage);
  }

  @AfterEach
  public void cleanup() {
    playwrightLogout();
    tempEntity.deleteSystemConfigurationProperty("waivedComponentUpgradeMonitoringEnabled");
  }

  @Test
  @Tag("regression")
  public void testWaivedComponentUpgradesPage_renders() {
    configAssertions.shouldRenderPageLayout();
  }

  @Test
  @Tag("regression")
  public void testWaivedComponentUpgradesToggle_persistsAcrossReload() {
    boolean initiallyEnabled = configPage.monitoringToggleInput().isChecked();
    configPage.monitoringToggle().click();
    configPage.updateButton().click();
    waitForSubmitMaskSuccess();

    playwrightRefreshOrOpen(WaivedComponentUpgradesPage.url());
    if (initiallyEnabled) {
      configAssertions.shouldHaveMonitoringToggleUnchecked();
    }
    else {
      configAssertions.shouldHaveMonitoringToggleChecked();
    }
  }
}
