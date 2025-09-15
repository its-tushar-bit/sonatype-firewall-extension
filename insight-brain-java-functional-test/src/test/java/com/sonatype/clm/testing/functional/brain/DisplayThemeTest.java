/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.codeborne.selenide.SelenideElement;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.*;
import com.sonatype.clm.testing.functional.pages.DashboardPage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Selenide.$;

public class DisplayThemeTest
    extends AbstractFunctionalTest
{
  @Before
  public void before() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @After
  public void after() {
    // logout if not already logged out
    hardreset();
  }

  @Test
  public void testDisplayTheme() {
    refresh();

    UserMenu userMenu = MainHeader.userMenu();
    userMenu.dropdownToggle().shouldBe(visible).click();
    userMenu.displayTheme().shouldBe(visible);

    userMenu.displayTheme().click();
    DisplayThemeModal displayThemeModal = new DisplayThemeModal();
    displayThemeModal.shouldBe(visible);

    displayThemeModal.systemSettingRadio().shouldHave(cssClass("tm-checked"));

    SelenideElement htmlRoot = $("html");
    htmlRoot.shouldHave(cssClass("nx-html--enable-color-schemes"));

    displayThemeModal.darkModeRadio().click();
    displayThemeModal.darkModeRadio().shouldHave(cssClass("tm-checked"));
    htmlRoot.shouldHave(cssClass("nx-html--enable-color-schemes"));
    htmlRoot.shouldHave(cssClass("nx-html--dark-mode"));

    displayThemeModal.systemSettingRadio().click();
    displayThemeModal.systemSettingRadio().shouldHave(cssClass("tm-checked"));
    htmlRoot.shouldHave(cssClass("nx-html--enable-color-schemes"));
    htmlRoot.shouldNotHave(cssClass("nx-html--light-mode"));
    htmlRoot.shouldNotHave(cssClass("nx-html--dark-mode"));

    displayThemeModal.closeButton().click();
    displayThemeModal.shouldNotBe(visible);
  }
}
