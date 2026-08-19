/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class UserActivityDetailsPage
    extends BasePage
{
  private static final String DETAILS_TABLE = "#user-activity-details-table";

  public UserActivityDetailsPage() {
    super();
  }

  public static String url(String username) {
    return "/assets/index.html#/users/activity/" + username;
  }

  public Locator detailsTable() {
    return locator(DETAILS_TABLE);
  }

  public Locator pageHeading(String username) {
    return page.getByRole(AriaRole.HEADING,
        new Page.GetByRoleOptions().setName(username + " Activity ("));
  }

  public Locator activityDetailsTileHeading() {
    return page.getByRole(AriaRole.HEADING,
        new Page.GetByRoleOptions().setName("Activity Details").setExact(true));
  }

  public Locator exportActivityButton() {
    return page.getByRole(AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName("Export Activity"));
  }
}
