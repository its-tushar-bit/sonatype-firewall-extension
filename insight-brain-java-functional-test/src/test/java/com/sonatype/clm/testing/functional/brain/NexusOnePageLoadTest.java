/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.LoginModal;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.NexusOnePage;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

/**
 * Verifies that the Nexus One SPA loads and renders its hello-world routes.
 */
public class NexusOnePageLoadTest
    extends AbstractFunctionalTest
{
  @Before
  public void enableNexusOneUiForTest() {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);
    ensureLoggedInOnClassicShell();
  }

  private static void ensureLoggedInOnClassicShell() {
    hardreset();
    refreshOrOpen(IndexPage.url());
    LoginModal loginModal = new LoginModal();
    if (loginModal.getElement().is(visible)) {
      loginAsAdmin();
      return;
    }
    if (MainHeader.loginButton().is(visible)) {
      MainHeader.loginButton().click();
      loginAsAdmin();
      return;
    }
    MainHeader.userMenu().dropdownToggle().shouldBe(visible);
  }

  @Test
  public void testNexusOneSpaLoads() {
    refreshOrOpen(NexusOnePage.url("/hello1"));
    NexusOnePage page = new NexusOnePage();
    page.shouldBe(visible);
    page.heading().shouldHave(text("Hello World 1"));
  }

  @Test
  public void testNexusOneRoutesWork() {
    refreshOrOpen(NexusOnePage.url("/hello2"));
    NexusOnePage page = new NexusOnePage();
    page.shouldBe(visible);
    page.heading().shouldHave(text("Hello World 2"));
  }
}
