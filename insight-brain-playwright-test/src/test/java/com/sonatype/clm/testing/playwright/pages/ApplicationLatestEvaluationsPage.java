/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Playwright page object for the Application Latest Evaluations page rendered by
 * {@code applicationLatestEvaluations/ApplicationLatestEvaluationsPage.jsx}. Route:
 * {@code /applicationLatestEvaluations/{applicationPublicId}/stage/{stageId}}.
 */
public class ApplicationLatestEvaluationsPage
    extends BasePage
{
  private static final String ROOT = "#application-latest-evaluations-page";

  public ApplicationLatestEvaluationsPage() {
    super();
  }

  public static String url(String applicationPublicId, String stageId) {
    return "/assets/index.html#/applicationLatestEvaluations/" + applicationPublicId + "/stage/" + stageId;
  }

  public Locator container() {
    return locator(ROOT);
  }

  /** Page H1: "{App Name} Latest Evaluations" — accessible name comes from the H1 text. */
  public Locator pageHeading() {
    return container().getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(1));
  }

  /**
   * Stage-label description block underneath the H1 ("Stage: {stage}"). Anchored on the
   * {@code .iq-application-latest-evaluations__stage-label} class because the surrounding
   * {@code NxPageTitle.Description} renders a bare {@code <div>} and the label itself is a
   * bare {@code <span>} — neither has an ARIA role, so no accessible-name alternative exists.
   */
  public Locator stageDescription() {
    return locator(".iq-application-latest-evaluations__stage-label");
  }

  /** Latest-evaluations table inside the tile. */
  public Locator evaluationsTable() {
    return container().getByRole(AriaRole.TABLE);
  }
}
