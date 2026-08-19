/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/** Final step of the bulk-waive flow, reached after {@link WaiverConfigurationPage}. */
public class WaiverConfirmationPage
    extends BasePage
{
  public static final String URL_FRAGMENT = "/waiverConfirmation";

  public WaiverConfirmationPage() {
    super();
  }

  public static String url(String appPublicId, String scanId) {
    return "/assets/index.html#/applicationReport/" + appPublicId + "/" + scanId + URL_FRAGMENT;
  }

  /** Narrowed by the "Confirmation" tile heading to avoid collisions with other {@code main} regions. */
  public Locator container() {
    return page.getByRole(AriaRole.MAIN)
        .filter(new Locator.FilterOptions().setHasText("Confirmation"));
  }

  public Locator submitButton() {
    return container().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Submit").setExact(true));
  }
}
