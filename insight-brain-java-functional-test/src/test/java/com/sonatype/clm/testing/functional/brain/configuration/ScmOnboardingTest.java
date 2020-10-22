/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.configuration;

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
import static org.assertj.core.api.Assertions.assertThat;

public class ScmOnboardingTest
    extends AbstractFunctionalTest
{
  public static final String CI_PROJECT_1_GIT = "https\\:\\/\\/github\\.com\\/depshield-ci\\/ci-project-1\\.git";

  public static final String REPOSITORY_P_2_GIT = "https\\:\\/\\/github\\.com\\/sonatype-nexus-community\\/nexus-repo" +
      "sitory-p2\\.git";

  public static final String REPOSITORY_PUPPET_GIT = "https\\:\\/\\/github\\.com\\/sonatype-nexus-community\\/nexus-r" +
      "epository-puppet\\.git";

  public static final String REPOSITORY_TERRAFORM_GIT = "https\\:\\/\\/github\\.com\\/sonatype-nexus-community\\/nexu" +
      "s-repository-terraform\\.git";

  public static final String REPOSITORY_VGO_GIT = "https\\:\\/\\/github\\.com\\/sonatype-nexus-community\\/nexus-repo" +
      "sitory-vgo\\.git";

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
    assertThat(scmOnboardingPage.resultsTableNamespace().texts()).containsAnyOf("depshield-ci",
        "sonatype-nexus-community");
    assertThat(scmOnboardingPage.resultsTableProject().texts()).containsExactlyInAnyOrder("ci-project-1",
        "ci-project-16", "create-react-app", "nexus-repository-pw", "nexus-repository-puppet",
        "nexus-repository-terraform", "nexus-repository-vgo", "nexus-scripting-examples",
        "nexus-webhook-example-collection", "nxrm-cli", "ossindex-gradle-plugin", "oysteR", "prime-nexus-proxy-repos");
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

  @Test
  public void testSelection_all() {
    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    Organization org = tempEntity.newOrganization("Test Org");
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    refreshOrOpen(ScmOnboardingPage.url() + "/" + org.getId());
    loginAsAdmin();

    // when select all is clicked
    scmOnboardingPage.resultsTableSelectAll().parent().waitUntil(visible, 5000);
    scmOnboardingPage.resultsTableSelectAll().parent().click();

    // then selected count is updated
    scmOnboardingPage.selectedRepositoryCount().shouldBe(text("13"));

    // when select all is clicked again (delected)
    scmOnboardingPage.resultsTableSelectAll().parent().click();

    // then selected count is updated
    scmOnboardingPage.selectedRepositoryCount().shouldBe(text("0"));
  }

  @Test
  public void testSelection_subset() {
    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    Organization org = tempEntity.newOrganization("Test Org");
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    refreshOrOpen(ScmOnboardingPage.url() + "/" + org.getId());
    loginAsAdmin();

    // when select all is clicked while a filter is active
    scmOnboardingPage.resultsTableSelectAll().parent().waitUntil(visible, 5000);
    scmOnboardingPage.projectFilter().setValue("ci-");
    scmOnboardingPage.resultsTableSelectAll().parent().click();

    // then selected count is updated with the number of filtered repositories
    scmOnboardingPage.selectedRepositoryCount().shouldBe(text("2"));
    assertThat(scmOnboardingPage.resultsTableProject().texts()).containsExactlyInAnyOrder("ci-project-1",
        "ci-project-16");
  }

  @Test
  public void testSelection_subset_replaces_previous_selection() {
    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    Organization org = tempEntity.newOrganization("Test Org");
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    refreshOrOpen(ScmOnboardingPage.url() + "/" + org.getId());
    loginAsAdmin();

    // and given that select all is clicked while a filter is active
    scmOnboardingPage.resultsTableSelectAll().parent().waitUntil(visible, 5000);
    scmOnboardingPage.projectFilter().setValue("ci-");
    scmOnboardingPage.resultsTableSelectAll().parent().click();
    scmOnboardingPage.selectedRepositoryCount().shouldBe(text("2"));

    // when a new selection is made
    scmOnboardingPage.projectFilter().setValue("nexus");
    scmOnboardingPage.resultsTableSelectAll().parent().click(); // uncheck box
    scmOnboardingPage.resultsTableSelectAll().parent().click(); // check box
    scmOnboardingPage.selectedRepositoryCount().shouldBe(text("7"));
    assertThat(scmOnboardingPage.resultsTableProject().texts()).containsExactlyInAnyOrder(
        "nexus-repository-pw", "nexus-repository-puppet", "nexus-repository-terraform",
        "nexus-repository-vgo", "nexus-scripting-examples", "nexus-webhook-example-collection",
        "prime-nexus-proxy-repos");
  }

  @Test
  public void testSelection_filter_change_replaces_previous_selection() {
    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    Organization org = tempEntity.newOrganization("Test Org");
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    refreshOrOpen(ScmOnboardingPage.url() + "/" + org.getId());
    loginAsAdmin();

    // and given that select all is clicked while a filter is active
    scmOnboardingPage.resultsTableSelectAll().parent().waitUntil(visible, 5000);
    scmOnboardingPage.projectFilter().setValue("nexus");
    scmOnboardingPage.resultsTableSelectAll().parent().click();
    scmOnboardingPage.selectedRepositoryCount().shouldBe(text("7"));
    assertThat(scmOnboardingPage.resultsTableProject().texts()).containsExactlyInAnyOrder(
        "nexus-repository-pw", "nexus-repository-puppet", "nexus-repository-terraform",
        "nexus-repository-vgo", "nexus-scripting-examples", "nexus-webhook-example-collection",
        "prime-nexus-proxy-repos");

    // when a new selection is made
    scmOnboardingPage.projectFilter().setValue("nexus-repository");

    // then the selected count is updated
    scmOnboardingPage.selectedRepositoryCount().shouldBe(text("4"));

    // and the result table contains exactly 4 projects
    assertThat(scmOnboardingPage.resultsTableProject().texts()).containsExactlyInAnyOrder(
        "nexus-repository-pw", "nexus-repository-puppet", "nexus-repository-terraform",
        "nexus-repository-vgo");

    // and the repositories checkboxes are selected
    assertThat(scmOnboardingPage.selectionCheckboxById(REPOSITORY_P_2_GIT).isSelected()).isTrue();
    assertThat(scmOnboardingPage.selectionCheckboxById(REPOSITORY_PUPPET_GIT).isSelected()).isTrue();
    assertThat(scmOnboardingPage.selectionCheckboxById(REPOSITORY_TERRAFORM_GIT).isSelected()).isTrue();
    assertThat(scmOnboardingPage.selectionCheckboxById(REPOSITORY_VGO_GIT).isSelected()).isTrue();

    // when the filter is changed
    scmOnboardingPage.projectFilter().setValue("i");

    // then the selections remain selected
    assertThat(scmOnboardingPage.selectionCheckboxById(REPOSITORY_P_2_GIT).isSelected()).isTrue();
    assertThat(scmOnboardingPage.selectionCheckboxById(REPOSITORY_PUPPET_GIT).isSelected()).isTrue();
    assertThat(scmOnboardingPage.selectionCheckboxById(REPOSITORY_TERRAFORM_GIT).isSelected()).isTrue();
    assertThat(scmOnboardingPage.selectionCheckboxById(REPOSITORY_VGO_GIT).isSelected()).isTrue();

    // and other repositories remain deselected
    assertThat(scmOnboardingPage.selectionCheckboxById(CI_PROJECT_1_GIT).isSelected()).isFalse();

    // and the selected count is unchanged
    scmOnboardingPage.selectedRepositoryCount().shouldBe(text("4"));
  }

  @Test
  public void testSelection_select_individual_row() {
    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    Organization org = tempEntity.newOrganization("Test Org");
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    refreshOrOpen(ScmOnboardingPage.url() + "/" + org.getId());
    loginAsAdmin();

    // when a repository is clicked
    scmOnboardingPage.resultsTableSelectAll().parent().waitUntil(visible, 5000);
    scmOnboardingPage.selectionCheckboxById(CI_PROJECT_1_GIT).parent().click();

    // then the checkbox is selected
    assertThat(scmOnboardingPage.selectionCheckboxById(CI_PROJECT_1_GIT).isSelected()).isTrue();
  }
}
