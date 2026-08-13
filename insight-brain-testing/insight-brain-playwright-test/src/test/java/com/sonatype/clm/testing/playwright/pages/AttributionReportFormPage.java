/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class AttributionReportFormPage
    extends BasePage
{
  private static final Locator.GetByRoleOptions GENERATE_REPORT_OPTS =
      new Locator.GetByRoleOptions().setName("Generate Report");

  private static final Page.GetByRoleOptions MANAGE_TEMPLATES_PAGE_OPTS =
      new Page.GetByRoleOptions().setName("Manage Templates");

  private static final Page.GetByRoleOptions ATTRIBUTION_SETTINGS_PAGE_OPTS =
      new Page.GetByRoleOptions().setName("Attribution Report Settings");

  private static final Page.GetByRoleOptions ATTACH_FILES_PAGE_OPTS =
      new Page.GetByRoleOptions().setName("Attach Files");

  public AttributionReportFormPage() {
    super();
  }

  public static String url(String publicAppId, String stageTypeId) {
    return "/assets/index.html#/legal/application/" + publicAppId + "/stage/" + stageTypeId + "/attributionReport";
  }

  public Locator container() {
    return byRole(AriaRole.MAIN);
  }

  public Locator form() {
    return page.getByRole(AriaRole.REGION, ATTRIBUTION_SETTINGS_PAGE_OPTS);
  }

  public Locator generateReportButton() {
    return form().getByRole(AriaRole.BUTTON, GENERATE_REPORT_OPTS);
  }

  public Locator reportTitleInput() {
    return byLabel("Report Title");
  }

  public Locator tableOfContentsCheckbox() {
    return form().locator("label.nx-checkbox", new Locator.LocatorOptions().setHasText("Include Table of Contents"));
  }

  public Locator includeLicenseCheckbox() {
    return form().locator("label.nx-checkbox",
        new Locator.LocatorOptions().setHasText("Include Standard License Text"));
  }

  public Locator appendixCheckbox() {
    return form().locator("label.nx-checkbox", new Locator.LocatorOptions().setHasText("Include Appendix"));
  }

  public Locator manageTemplatesButton() {
    return page.getByRole(AriaRole.LINK, MANAGE_TEMPLATES_PAGE_OPTS);
  }

  public Locator additionalNoticeFilesFieldset() {
    return byText("Additional Notice Files");
  }

  public Locator attachFilesButton() {
    return page.getByRole(AriaRole.BUTTON, ATTACH_FILES_PAGE_OPTS);
  }

}
