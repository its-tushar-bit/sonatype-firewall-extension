/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ExternalLinkModal;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.GettingStartedPage;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import org.junit.After;
import org.junit.Test;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

public class ExternalLinkTest
    extends AbstractFunctionalTest
{
  @After
  public void before() {
    testCLMServer.getHdsServer().respondWith("alive").atUri("ping");
  }

  @After
  public void after() {
    testCLMServer.getCLMServer().getConfiguration().setExternalHyperlinksAllowed(true);
    logout();
  }

  @Test
  public void testExternalLinks_Enabled() {
    refreshOrOpen(GettingStartedPage.url());
    loginAsAdmin();
    GettingStartedPage gettingStartedPage = new GettingStartedPage();

    gettingStartedPage.systemSetup().shouldBe(visible);
    gettingStartedPage.docLink(0).shouldBe(visible).click();
    assertThat(WebDriverRunner.getAndCheckWebDriver().getWindowHandles()).hasSize(2);
    Selenide.switchTo().window(1).close();
    Selenide.switchTo().window(0);
  }

  @Test
  public void testExternalLinks_Disabled() {
    testCLMServer.getCLMServer().getConfiguration().setExternalHyperlinksAllowed(false);
    refreshOrOpen(GettingStartedPage.url());
    loginAsAdmin();
    GettingStartedPage gettingStartedPage = new GettingStartedPage();
    gettingStartedPage.systemSetup().shouldBe(visible);
    gettingStartedPage.docLink(0).shouldBe(visible).click();
    assertThat(WebDriverRunner.getAndCheckWebDriver().getWindowHandles()).hasSize(1);
    ExternalLinkModal modal = new ExternalLinkModal();
    modal.shouldBe(visible).body().shouldHave(text("http://links.sonatype.com/products/nxiq/doc/requirements"));
    modal.closeButton().click();
    modal.shouldBe(hidden);

    // Sanity check, make sure internal links still work
    SidebarNavigation.dashboardNavigationButton().click();
    DashboardPage.dashboardContainer().shouldBe(visible);
  }

  @Test
  public void testExternalLinks_Disabled_Icon() {
    testCLMServer.getCLMServer().getConfiguration().setExternalHyperlinksAllowed(false);
    refreshOrOpen(GettingStartedPage.url());
    loginAsAdmin();
    GettingStartedPage gettingStartedPage = new GettingStartedPage();
    gettingStartedPage.systemSetup().shouldBe(visible);
    gettingStartedPage.docLinkIcon(0).shouldBe(visible).click();
    assertThat(WebDriverRunner.getAndCheckWebDriver().getWindowHandles()).hasSize(1);
    ExternalLinkModal modal = new ExternalLinkModal();
    modal.shouldBe(visible).body().shouldHave(text("http://links.sonatype.com/products/nxiq/doc/requirements"));
    modal.closeButton().click();
    modal.shouldBe(hidden);
  }
}
