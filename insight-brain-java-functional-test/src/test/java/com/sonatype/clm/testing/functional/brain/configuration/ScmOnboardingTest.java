/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.configuration;

import java.util.Arrays;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.ScmOnboardingPage;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.codeborne.selenide.Selenide;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.google.common.collect.ImmutableMap.of;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.service.InsightConfig.Feature.SCM_ONBOARDING;

public class ScmOnboardingTest
    extends AbstractFunctionalTest
{
  @AfterClass
  public static void cleanup() {
    testCLMServer.getCLMServer().getConfiguration().setExperimentalFeatures(of(SCM_ONBOARDING.getFlag(), false));
  }

  @After
  public void clearCookies() {
    Selenide.clearBrowserCookies();
  }

  @Before
  public void setup() {
    // given the feature flag is true
    testCLMServer.getCLMServer().getConfiguration().setExperimentalFeatures(of(Feature.SCM_ONBOARDING.getFlag(), true));
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
  public void testPopulatesRepositories() {
    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url() + "/stubOrgId");
    loginAsAdmin();

    // then results are automatically displayed in the table
    scmOnboardingPage.loadingSpinner().waitWhile(visible, 5000);
    scmOnboardingPage.resultsTable().waitUntil(visible, 5000);
    scmOnboardingPage.repositoryCount().shouldBe(text("13"));
    scmOnboardingPage.selectedTotalCount().shouldBe(text("OF 13 REPOSITORIES"));
    scmOnboardingPage.resultsTableProject().shouldHaveSize(13);
    scmOnboardingPage.resultsTableNamespace().texts().containsAll(
        Arrays.asList("depshield-ci", "sonatype-nexus-community"));
    scmOnboardingPage.resultsTableProject().texts().containsAll(
        Arrays.asList("ci-project-1", "ci-project-16", "create-react-app", "nexus-repository-pw",
            "nexus-repository-puppet", "nexus-repository-terraform", "nexus-repository-vgo",
            "nexus-scripting-examples", "nexus-webhook-example-collection", "nxrm-cli",
            "ossindex-gradle-plugin", "oysteR", "prime-nexus-proxy-repos"));
  }

  @Test
  public void testPageTitleElements() {
    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    Organization org = tempEntity.newOrganization("Test Org");
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    refreshOrOpen(ScmOnboardingPage.url() + "/" + org.getId());
    loginAsAdmin();

    // then the page title block is populated
    scmOnboardingPage.backButton().shouldBe(text("Back to Organization Management"));
    scmOnboardingPage.onboardingPageTitle().shouldBe(text("Import Applications from Github to Test Org"));
  }
}
