/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Regression-only extension of {@link SonatypeDeveloperPage} for Developer Dashboard and Priorities.
 * Divergence: tabs replaced by integration cards; Issue Tracking has no card equivalent.
 */
public class SonatypeDeveloperRegressionPage
    extends SonatypeDeveloperPage
{
  public static final String CI_CD_URL_SEGMENT = "ci-cd";

  public static final String SCM_URL_SEGMENT = "scm";

  public static final String IDE_URL_SEGMENT = "ide";

  public Locator ciCdCardLearnMoreLink() {
    return integrationCard(CI_CARD_NAME).getByRole(AriaRole.LINK);
  }

  public Locator scmCardLearnMoreLink() {
    return integrationCard(SCM_CARD_NAME).getByRole(AriaRole.LINK);
  }

  public Locator ideCardLearnMoreLink() {
    return integrationCard(IDE_CARD_NAME).getByRole(AriaRole.LINK);
  }

  public Locator prioritiesPageSummarySection() {
    return byTestId("iq-priorities-page-summary-section");
  }

  public Locator loadError() {
    return container().locator(".nx-alert--load-error");
  }

  public Locator retryButton() {
    return container().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Retry"));
  }
}
