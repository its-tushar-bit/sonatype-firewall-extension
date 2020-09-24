/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.configuration;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.ScmOnboardingPage;
import com.sonatype.clm.testing.functional.pages.ScmOnboardingPage.OrganizationsDropdown;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.User;

import com.codeborne.selenide.Selenide;
import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.google.common.collect.ImmutableMap.of;
import static com.sonatype.insight.brain.service.InsightConfig.Feature.SCM_ONBOARDING;

public class ScmOnboardingTest
    extends AbstractFunctionalTest
{
  @Before
  public void setup() {
    testCLMServer.getCLMServer().getConfiguration().setExperimentalFeatures(of(SCM_ONBOARDING.getFlag(), true));
  }

  @After
  public void clearCookies() {
    Selenide.clearBrowserCookies();
  }

  @Test
  public void testFeatureIsDisabled() {
    // given the feature flag is false
    testCLMServer.getCLMServer().getConfiguration().setExperimentalFeatures(of(SCM_ONBOARDING.getFlag(), false));
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();

    // when we open the onboarding page as admin
    refreshOrOpen(ScmOnboardingPage.url());
    loginAsAdmin();

    // then the onboarding page is disabled
    scmOnboardingPage.featureFlagError().shouldBe(visible).shouldNotBe(empty);
    scmOnboardingPage.permissionDeniedError().shouldBe(hidden);
  }

  @Test
  public void testFeatureIsEnabled() {
    // given the onboarding page
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();

    // when we open the onboarding page as admin
    refreshOrOpen(ScmOnboardingPage.url());
    loginAsAdmin();

    // then the feature flag error is hidden
    scmOnboardingPage.featureFlagError().shouldBe(hidden);
    scmOnboardingPage.permissionDeniedError().shouldBe(hidden);
  }

  @Test
  public void testFeatureIsNotAllowed() {
    // given the onboarding page and a new user
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    User user = tempEntity.newUser();

    // when we open the onboarding page as unprivileged user
    refreshOrOpen(ScmOnboardingPage.url());
    login(user.getUsername(), user.getPassword());

    // then a permission denied error is shown
    scmOnboardingPage.permissionDeniedError().shouldBe(visible).shouldNotBe(empty);
    scmOnboardingPage.featureFlagError().shouldBe(hidden);
  }

  @Test
  public void testPopulatesOrganizations() {
    // given the onboarding page
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();

    // and organizations exist
    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization();
    OrganizationsDropdown organizationsDropdown = scmOnboardingPage.organizationsDropdown();

    // when we open the onboarding page as admin
    refreshOrOpen(ScmOnboardingPage.url());
    loginAsAdmin();

    // then organizations dropdown is populated
    organizationsDropdown.openMenuButton().waitUntil(enabled, 5000);
    organizationsDropdown.selectedOrganization().shouldHave(text("Select"));
    organizationsDropdown.openMenuButton().click();
    organizationsDropdown.openMenuButton().click();  // TODO not sure why it needs 2 clicks. something wrong here!
    organizationsDropdown.dropdownMenu().options().shouldHaveSize(2);
    organizationsDropdown.dropdownMenu().options().containsAll(ImmutableSet.of(org1, org2));
  }

  @Test
  public void testPopulatesRepositories() {
    // given SCM onboarding page
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url());
    loginAsAdmin();

    // when the load button is pressed
    scmOnboardingPage.loadButton().waitUntil(enabled, 5000);
    scmOnboardingPage.loadButton().click();

    // then results are displayed in the table (only basic check, data will be provided in INT-3453)
    scmOnboardingPage.resultsTable().waitUntil(visible, 5000);
    scmOnboardingPage.resultsTableUrl().shouldHaveSize(13);
  }
}
