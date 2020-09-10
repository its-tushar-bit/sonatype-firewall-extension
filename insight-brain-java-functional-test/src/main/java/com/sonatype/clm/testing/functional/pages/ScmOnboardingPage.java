/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

public class ScmOnboardingPage
    extends BasicElement<ScmOnboardingPage>
{
  public static String url() {
    return BaseUrl.resolvePageUrl("/onboarding");
  }

  private static final String ROOT_SELECTOR = "#scm-onboarding-root";

  public ScmOnboardingPage() {
    super(ROOT_SELECTOR);
  }

  public SelenideElement featureFlagError() {
    return child("#scm-onboarding-feature-flag-disabled-error");
  }

  public SelenideElement permissionDeniedError() {
    return child("#scm-onboarding-insufficient-permissions-error");
  }
}
