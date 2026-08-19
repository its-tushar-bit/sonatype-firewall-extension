/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class ApiDocumentationPage
    extends BasePage
{
  private static final String ROOT = "#api-page";

  public ApiDocumentationPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/api";
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator opblockSummaries() {
    return container().locator(".opblock-summary");
  }

  public Locator firstOpblockSummary() {
    return opblockSummaries().first();
  }

  public Locator tryItOutButton() {
    return container().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Try it out").setExact(true)).first();
  }

  public Locator executeButton() {
    return container().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Execute").setExact(true)).first();
  }

  public Locator responseContainer() {
    return container().locator(".responses-wrapper").first();
  }

  public Locator liveResponseStatusCode() {
    return container().locator(".live-responses-table .response-col_status").first();
  }
}
