/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

/**
 * Playwright page object for the IQ Server user-details modal.
 *
 * <p>
 * The modal is opened via {@link HeaderComponent#userDetailsButton()} and displays the
 * currently signed-in user's username, display name, and group memberships.
 */
public class UserDetailsModal
    extends BasePage
{
  public UserDetailsModal() {
    super();
  }

  public Locator container() {
    return locator("#user-details-modal");
  }

  public Locator username() {
    return locator("#user-details-modal-username");
  }

  public Locator displayName() {
    return locator("#user-details-modal-display-name");
  }

  public Locator groups() {
    return locator("#user-details-modal-groups");
  }

  public Locator closeButton() {
    return locator("#user-details-modal-close");
  }
}
