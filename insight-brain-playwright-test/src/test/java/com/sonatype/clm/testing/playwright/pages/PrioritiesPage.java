/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class PrioritiesPage
    extends BasePage
{
  public PrioritiesPage() {
    super();
  }

  public static String url(String appPublicId, String scanId) {
    return "/assets/index.html#/developer/priorities/" + appPublicId + "/" + scanId;
  }

  // componentNameFilter query param is a documented page feature (same mechanism as the in-page search input), not a
  // test workaround. Encode the value to keep '&', '=', and spaces from mis-routing.
  public static String url(String appPublicId, String scanId, String componentNameFilter) {
    return url(appPublicId, scanId)
        + "?componentNameFilter=" + URLEncoder.encode(componentNameFilter, StandardCharsets.UTF_8);
  }

  // ID anchor — scope to the priorities table specifically so this doesn't pick up any future
  // table elsewhere on the page. Same pattern as DeveloperRiskTablePage.
  public Locator container() {
    return locator("#iq-priorities-table");
  }

  public Locator rows() {
    return container().locator("tbody").getByRole(AriaRole.ROW);
  }

  public Locator rowByArtifactId(String artifactId) {
    return container().locator("tbody")
        .getByRole(AriaRole.ROW)
        .filter(new Locator.FilterOptions().setHasText(artifactId));
  }

  public Locator buildActionCell(Locator row) {
    return row.locator("div.iq-priorities-table__build-action");
  }

  public Locator expiredWaiverIcon(Locator row) {
    return buildActionCell(row).locator(".iq-expired-waiver-icon");
  }

  public Locator soonToExpireWaiverIcon(Locator row) {
    return buildActionCell(row).locator(".iq-soon-to-expire-waiver-icon");
  }

  public Locator recommendationCell(Locator row) {
    return row.locator("div.iq-priorities-table__recommendation");
  }

}
