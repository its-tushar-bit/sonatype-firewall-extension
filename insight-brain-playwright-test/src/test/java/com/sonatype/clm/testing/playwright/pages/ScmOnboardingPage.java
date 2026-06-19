/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

public class ScmOnboardingPage
    extends BasePage
{
  private static final String ROOT = "#scm-onboarding-container";

  public ScmOnboardingPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/onboarding";
  }

  public static String urlForOrg(String organizationId) {
    return "/assets/index.html#/onboarding/" + organizationId;
  }

  public Locator container() {
    return locator(ROOT);
  }
}
