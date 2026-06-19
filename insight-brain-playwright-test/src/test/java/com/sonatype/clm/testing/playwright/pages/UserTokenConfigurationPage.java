/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Playwright page object for the User Tokens Configuration screen
 * ({@code Configuration → User Tokens}, frontend
 * {@code configuration/userTokensConfiguration/UserTokensConfiguration.jsx}).
 */
public class UserTokenConfigurationPage
    extends BasePage
{
  private static final String ROOT = "#user-tokens-configuration";

  private static final String TILE = "#user-tokens-configuration-tile";

  public UserTokenConfigurationPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/userTokensConfiguration";
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator pageHeading() {
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("User Tokens").setExact(true));
  }

  public Locator tile() {
    return locator(TILE);
  }

  public Locator tileHeading() {
    return tile().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Token Configuration").setExact(true));
  }

  public Locator enableUserTokensToggle() {
    return nxToggleLabel(tile(), "Enable User Tokens");
  }

  public Locator enableUserTokensToggleInput() {
    return nxToggleInput(tile(), "Enable User Tokens");
  }

  public Locator expirationToggle() {
    return nxToggleLabel(tile(), "Enable User Token Expiration");
  }

  public Locator expirationToggleInput() {
    return nxToggleInput(tile(), "Enable User Token Expiration");
  }

  public Locator expiryDaysInput() {
    return byLabel("User Token Expiry");
  }

  public Locator updateButton() {
    return tile().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Update").setExact(true));
  }

  public Locator cancelButton() {
    return tile().getByRole(AriaRole.BUTTON, CommonButtonOptions.CANCEL_BUTTON_OPTS);
  }
}
