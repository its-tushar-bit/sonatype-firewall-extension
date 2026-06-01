/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.LoginModal;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.NexusOnePage;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.webdriver;
import static com.codeborne.selenide.WebDriverConditions.urlContaining;

/**
 * Verifies {@link com.sonatype.insight.brain.landing.NexusOneIndexAccessFilter} redirects
 * to the classic shell when the master flag is OFF or the caller is anonymous.
 */
public class NexusOneIndexRedirectTest
    extends AbstractFunctionalTest
{
  @Before
  public void ensurePreviewNexusOneUiDisabled() {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);
  }

  @After
  public void tearDownPreviewFlag() {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);
  }

  @Test
  public void testAnonymousNexusOneIndexRedirectsToClassicShell() {
    hardreset();
    refreshOrOpen(NexusOnePage.url());
    waitUntilClassicIndexShell();
    waitUntilLoginDialogAppears();
    new LoginModal().shouldBe(visible);
  }

  @Test
  public void testAuthenticatedNexusOneIndexRedirectsWhenFlagOff() {
    hardreset();
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();

    refreshOrOpen(NexusOnePage.url());

    waitUntilClassicIndexShell();
    new NexusOnePage().shouldNotBe(visible);
  }

  /**
   * Waits for the server filter (or client gate) to land on the classic index. We use
   * {@code urlContaining} for the path because the classic SPA may append a hash route
   * immediately after load; {@link #waitUntilUrl(String)} would race that navigation.
   * When the URL is still the bare index, it equals {@link IndexPage#url()}.
   */
  private static void waitUntilClassicIndexShell() {
    webdriver().shouldHave(urlContaining("/assets/index.html"));
    webdriver().shouldNotHave(urlContaining("nexus-one"));
  }
}
