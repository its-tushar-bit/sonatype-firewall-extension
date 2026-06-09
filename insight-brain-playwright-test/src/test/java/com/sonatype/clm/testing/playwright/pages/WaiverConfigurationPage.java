/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class WaiverConfigurationPage
    extends BasePage
{
  private static final Locator.GetByRoleOptions SCOPE_GROUP_OPTS =
      new Locator.GetByRoleOptions().setName("Scope");

  private static final Locator.GetByRoleOptions EXPIRY_GROUP_OPTS =
      new Locator.GetByRoleOptions().setName("Waiver Expiration");

  private static final Locator.GetByRoleOptions REASON_GROUP_OPTS =
      new Locator.GetByRoleOptions().setName("Reason");

  public WaiverConfigurationPage() {
    super();
  }

  public static String url(String appPublicId, String scanId) {
    return "/assets/index.html#/applicationReport/" + appPublicId + "/" + scanId + "/waiverConfiguration";
  }

  public Locator container() {
    return page.getByRole(AriaRole.MAIN)
        .filter(
            new Locator.FilterOptions().setHasText("Waiver configuration"));
  }

  public Locator scopeDropdown() {
    return container().getByRole(AriaRole.GROUP, SCOPE_GROUP_OPTS).getByRole(AriaRole.COMBOBOX);
  }

  public Locator exactComponentRadio() {
    return container().getByText("Exact");
  }

  public Locator allVersionsRadio() {
    return container().getByText("All Versions");
  }

  public Locator expirySelect() {
    return container().getByRole(AriaRole.GROUP, EXPIRY_GROUP_OPTS).getByRole(AriaRole.COMBOBOX);
  }

  public Locator customExpiryDateInput() {
    return container().getByLabel("set custom expiration date").getByRole(AriaRole.TEXTBOX);
  }

  public Locator expirationDaysDiffMessage() {
    return container().getByText("This waiver will expire");
  }

  public Locator reasonSelect() {
    return container().getByRole(AriaRole.GROUP, REASON_GROUP_OPTS).getByRole(AriaRole.COMBOBOX);
  }

  public Locator commentsTextarea() {
    return container().getByRole(AriaRole.TEXTBOX);
  }

  public Locator confirmationPageContainer() {
    return page.getByRole(AriaRole.MAIN);
  }

  public Locator confirmationPageBackButton() {
    return confirmationPageContainer().getByRole(AriaRole.BUTTON, CommonButtonOptions.BACK_BUTTON_OPTS);
  }

  public Locator mixedViolationsAlert() {
    return container().getByRole(AriaRole.ALERT)
        .filter(
            new Locator.FilterOptions().setHasText("unknown/unclaimed components"));
  }

  public Locator enterpriseBanner() {
    return container().locator(".iq-enterprise-full-width-banner");
  }

  public Locator nextButton() {
    return container().getByRole(AriaRole.BUTTON, CommonButtonOptions.NEXT_BUTTON_OPTS);
  }

  public Locator backButton() {
    return container().getByRole(AriaRole.BUTTON, CommonButtonOptions.BACK_BUTTON_OPTS);
  }

  public Locator cancelButton() {
    return container().getByRole(AriaRole.BUTTON, CommonButtonOptions.CANCEL_BUTTON_OPTS);
  }

}
