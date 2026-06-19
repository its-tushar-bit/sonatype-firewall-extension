/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright page object for the IQ Server main header and its sub-menus.
 * {@code SystemConfigMenu}, {@code HelpMenu}, and {@code NotificationMenu} elements.
 */
public class HeaderComponent
    extends BasePage
{
  public HeaderComponent() {
    super();
  }

  public Locator menuBar() {
    return locator("#menu-bar");
  }

  public Locator mainHeader() {
    return locator("main-header");
  }

  public Locator loginButton() {
    // byRole(BUTTON,"Sign in") matches both this header button and the login modal submit button
    // when the modal is open (strict-mode violation). Use the stable header button id.
    return locator("#header-login-button");
  }

  public Locator userMenu() {
    return locator("#user-menu");
  }

  public Locator userName() {
    return userMenu().locator("#user-name");
  }

  public Locator userMenuDropdownToggle() {
    return byRole(AriaRole.BUTTON, "Manage User Account");
  }

  public Locator changePasswordButton() {
    return byRole(AriaRole.BUTTON, "Change Password");
  }

  public Locator manageUserTokenButton() {
    return byRole(AriaRole.BUTTON, "Manage User Token");
  }

  public Locator userDetailsButton() {
    return locator("#user-details");
  }

  public Locator logoutButton() {
    return byRole(AriaRole.BUTTON, "Logout");
  }

  /**
   * Open the user dropdown menu and click logout.
   */
  public void logout() {
    userMenuDropdownToggle().click();
    logoutButton().click();
  }

  /**
   * Open the user-account dropdown and wait for its menu items to become actionable.
   *
   * <p>
   * The dropdown is rendered by {@code NxStatefulNavigationDropdown}, which animates the menu
   * panel in. Clicking the toggle and immediately interacting with a menu item used to race
   * the open animation and (under parallel-fork load) intermittently click while the menu was
   * still detached/re-rendering — leading to the menu item click being silently dropped and
   * downstream "modal never appeared" timeouts. Waiting for {@link #manageUserTokenButton()}
   * to be visible before returning gives the panel time to mount and stabilize.
   */
  public void openUserMenu() {
    userMenuDropdownToggle().click();
    assertThat(manageUserTokenButton())
        .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  public void openChangePasswordModal() {
    openUserMenu();
    changePasswordButton().click();
  }

  /**
   * Open the Manage User Token modal from the user menu and wait for the modal element
   * itself to appear in the DOM before returning.
   *
   * <p>
   * The modal is rendered conditionally on the redux flag {@code isUserTokenModalVisible}
   * (see {@code UserMenu.jsx}, line 84). Returning before the modal mounts forces every
   * caller to either redo the wait or race against the modal's own boot — historically
   * caused "{@code waiting for locator(\"#user-token-modal #generate-user-token\")} to be
   * visible" timeouts because the modal hadn't even mounted yet (so the child button could
   * not exist). Waiting here for the user-token dialog keeps that contract local.
   */
  public void openManageUserTokenModal() {
    openUserMenu();
    manageUserTokenButton().click();
    // NxModal does not set aria-labelledby in this RSC version, so the dialog has no accessible
    // name and getByRole(DIALOG,"Manage User Token") never resolves. Use the stable id.
    assertThat(locator("#user-token-modal")).isVisible();
  }

  public Locator systemConfigMenuButton() {
    return locator("#system-configuration-menu button");
  }

  public void navigateToSystemPreference(String menuItemLabel) {
    systemConfigMenuButton().click();
    Locator menuItem = locator("#system-configuration-menu")
        .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(menuItemLabel).setExact(true));
    assertThat(menuItem).isVisible();
    menuItem.click();
  }

}
