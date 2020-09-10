/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.configuration;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.ScmOnboardingPage;
import com.sonatype.insight.brain.model.security.User;

import com.codeborne.selenide.Selenide;
import org.junit.After;
import org.junit.Test;

import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Condition.hidden;
import static com.google.common.collect.ImmutableMap.of;
import static com.sonatype.insight.brain.service.InsightConfig.Feature.MANIFEST_SCAN;

public class ScmOnboardingTest
    extends AbstractFunctionalTest
{
  @After
  public void clearCookies() {
    Selenide.clearBrowserCookies();
  }

  @Test
  public void testFeatureIsDisabled() {
    // given the feature flag is false
    testCLMServer.getCLMServer().getConfiguration().setExperimentalFeatures(of(MANIFEST_SCAN.getFlag(), false));
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();

    // when we open the onboarding page as admin
    refreshOrOpen(ScmOnboardingPage.url());
    loginAsAdmin();

    // then the onboarding page is disabled
    scmOnboardingPage.featureFlagError().shouldBe(visible).shouldNotBe(empty);
  }

  @Test
  public void testFeatureIsEnabled() {
    // given the feature flag is true
    testCLMServer.getCLMServer().getConfiguration().setExperimentalFeatures(of(MANIFEST_SCAN.getFlag(), true));
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();

    // when we open the onboarding page as admin
    refreshOrOpen(ScmOnboardingPage.url());
    loginAsAdmin();

    // then the feature flag error is hidden
    scmOnboardingPage.featureFlagError().shouldBe(hidden);
  }

  @Test
  public void testFeatureIsNotAllowed() {
    // given the feature flag is true
    testCLMServer.getCLMServer().getConfiguration().setExperimentalFeatures(of(MANIFEST_SCAN.getFlag(), true));
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    User user = tempEntity.newUser();

    // when we open the onboarding page as admin
    refreshOrOpen(ScmOnboardingPage.url());
    login(user.getUsername(), user.getPassword());

    // then a permission denied error is shown
    scmOnboardingPage.permissionDeniedError().shouldBe(visible).shouldNotBe(empty);
  }
}
