/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.ScmOnboardingPage;
import com.sonatype.clm.testing.playwright.pages.ScmOnboardingPageAssertions;
import com.sonatype.insight.brain.model.Organization;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ScmOnboardingPlaywrightTest
    extends AbstractIqUiTest
{
  @Test
  @Category(RegressionTest.class)
  public void testScmOnboardingOrg_renders() {
    Organization org = tempEntity.newOrganization();

    playwrightRefreshOrOpen(ScmOnboardingPage.urlForOrg(org.getId()));
    playwrightLogin();

    ScmOnboardingPage onboardingPage = new ScmOnboardingPage();
    new ScmOnboardingPageAssertions(onboardingPage).shouldShowContainer();
  }
}
