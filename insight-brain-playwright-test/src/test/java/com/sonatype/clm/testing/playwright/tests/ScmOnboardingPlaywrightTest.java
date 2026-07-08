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

/**
 * Regression coverage for the SCM Onboarding page ({@code /assets/index.html#/onboarding}).
 * Covers the slices reachable without a live SCM provider; deeper repository/import flows
 * are deferred to a follow-up ticket.
 */
public class ScmOnboardingPlaywrightTest
    extends AbstractIqUiTest
{
  private ScmOnboardingPage onboardingPage;

  private ScmOnboardingPageAssertions assertions;

  @Test
  @Category(RegressionTest.class)
  public void testScmOnboardingOrg_renders() {
    Organization org = tempEntity.newOrganization();

    playwrightRefreshOrOpen(ScmOnboardingPage.urlForOrg(org.getId()));
    playwrightLogin();

    onboardingPage = new ScmOnboardingPage();
    assertions = new ScmOnboardingPageAssertions(onboardingPage);
    assertions.shouldShowContainer();
  }

  @Test
  @Category(RegressionTest.class)
  public void testScmOnboarding_targetOrgDropdownAndNewOrganizationButton() {
    Organization org = tempEntity.newOrganization();
    playwrightRefreshOrOpen(ScmOnboardingPage.urlForOrg(org.getId()));
    playwrightLogin();

    onboardingPage = new ScmOnboardingPage();
    assertions = new ScmOnboardingPageAssertions(onboardingPage);

    assertions.shouldShowContainer();
    assertions.shouldShowTargetOrganizationDropdown();
    assertions.shouldShowNewOrganizationButton();
  }

  @Test
  @Category(RegressionTest.class)
  public void testScmOnboarding_missingScmTokenError() {
    Organization org = tempEntity.newOrganization();
    playwrightRefreshOrOpen(ScmOnboardingPage.urlForOrg(org.getId()));
    playwrightLogin();

    onboardingPage = new ScmOnboardingPage();
    assertions = new ScmOnboardingPageAssertions(onboardingPage);

    assertions.shouldShowContainer();
    assertions.shouldShowScmTokenNotConfiguredError();
  }

}
