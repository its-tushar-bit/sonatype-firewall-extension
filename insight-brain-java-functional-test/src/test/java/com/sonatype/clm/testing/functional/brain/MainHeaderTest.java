/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.DashboardFilters;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.AgeFilter;
import com.sonatype.clm.testing.functional.elements.HelpMenu;
import com.sonatype.clm.testing.functional.elements.LoginModal;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.SystemConfigMenu;
import com.sonatype.clm.testing.functional.elements.UserMenu;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class MainHeaderTest
    extends AbstractFunctionalTest
{
  @Before
  public void before() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @After
  public void after() {
    // logout if not already logged out
    hardreset();
  }

  @Test
  public void testLoggedInUserName() {
    MainHeader.userMenu().dropdownToggle().click();
    MainHeader.userMenu().userName().shouldBe(visible).shouldHave(text("Admin BuiltIn"));
  }

  @Test
  public void testUserMenuLinks() {
    UserMenu userMenu = MainHeader.userMenu();
    userMenu.changePassword().shouldNotBe(visible);
    userMenu.manageUserToken().shouldNotBe(visible);
    userMenu.userDetails().shouldNotBe(visible);
    userMenu.logout().shouldNotBe(visible);

    userMenu.dropdownToggle().shouldBe(visible).click();
    userMenu.changePassword().shouldBe(visible);
    userMenu.manageUserToken().shouldBe(visible);
    userMenu.userDetails().shouldBe(visible);
    userMenu.logout().shouldBe(visible);
  }

  @Test
  public void testUserMenuDisplayThemeLink() {
    refresh();

    UserMenu userMenu = MainHeader.userMenu();
    userMenu.dropdownToggle().shouldBe(visible).click();
    userMenu.displayTheme().shouldBe(visible);
  }

  @Test
  public void testLoginButton() {
    LoginModal loginModal = new LoginModal();
    logout();

    MainHeader.loginButton().shouldNotBe(visible);
    loginModal.vulnerabilityLookupLink().click();

    MainHeader.loginButton().shouldBe(visible).click();
    loginModal.shouldBe(visible);
    MainHeader.loginButton().shouldBe(visible);

    loginAsAdmin();
    MainHeader.loginButton().shouldNotBe(visible);

    logout();
    MainHeader.loginButton().shouldNotBe(visible);
  }

  @Test
  public void testSystemDropdowns() {
    refreshOrOpen(DashboardPage.urlToViolations());

    DashboardPage.filterToggle().shouldBe(visible).click();
    AgeFilter ageFilter = DashboardFilters.ageFilter();
    ageFilter.shouldBe(visible);
    ageFilter.twisty().click();
    ageFilter.past90days().shouldNotBe(selected).click();
    DashboardFilters.closeButton().shouldBe(CLM.DISABLED);
    DashboardPage.violationsView().results().mask().shouldBe(visible);

    UserMenu userMenu = MainHeader.userMenu();
    userMenu.dropdownToggle().click();
    userMenu.userDetails().shouldBe(visible);
    userMenu.logout().shouldBe(visible);

    SystemConfigMenu sysConfigMenu = MainHeader.systemConfigMenu();
    sysConfigMenu.dropdownToggle().click();
    sysConfigMenu.successMetrics().shouldBe(visible);

    eyesWatcher.eyesCheck("Top Nav Dropdown not hidden");

    HelpMenu helpMenu = MainHeader.helpMenu();
    helpMenu.dropdownToggle().click();
    helpMenu.supportLink().shouldBe(visible);
  }
}
