/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.elements.DeleteModal;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.NxFormSelect;
import com.sonatype.clm.testing.functional.elements.NxFormSelect.Option;
import com.sonatype.clm.testing.functional.pages.SourceControlEditorPage;
import com.sonatype.clm.testing.functional.pages.SourceControlEditorPage.MetricsTableRow;
import com.sonatype.clm.testing.functional.pages.SourceControlEditorPage.TestResults;
import com.sonatype.clm.testing.functional.pages.SourceControlRepositoryUrlUpdateModal;
import com.sonatype.clm.testing.functional.utils.FormUtils;
import com.sonatype.insight.brain.api.v2.service.ApiCompositeSourceControlConfigValidatorService;
import com.sonatype.insight.brain.git.EnhancedPullRequestResult;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.telemetry.SourceControlPullRequestMetrics;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.nexus.iq.manager.PullRequestResult;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebElementsCondition;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.http.HttpHeaders;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.clm.testing.functional.pages.SourceControlEditorPage.metricsTable;
import static com.sonatype.insight.brain.git.EnhancedPullRequestResult.EXCEPTION_MESSAGE;
import static com.sonatype.insight.brain.git.EnhancedPullRequestResult.FAILURE_MESSAGE;
import static com.sonatype.insight.brain.git.EnhancedPullRequestResult.SUCCESS_MESSAGE;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControl.FAKE_SECRET_KEY;
import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationSourceControlEditorTest
    extends AbstractSourceControlEditorTest
{
  private static final WebElementsCondition CONFIG_TEST_NAMES = texts(
      "Configuration complete",
      "Private repository",
      "Sufficient token permissions");

  private static final WebElementsCondition CONFIG_TEST_SSH_NAMES = texts(
      "Configuration complete",
      "Private repository",
      "Sufficient token permissions",
      "SSH configuration complete");

  private Application application;

  private static final String REPOSITORY_URL = "https://a.com/b/c";

  private static final String REPOSITORY_PATH = "/b/c";

  private static final String REPOSITORY_SSH_URL = "git@a.com:b/c.git";

  private static final String BITBUCKET_REPOSITORY_URL = "https://bitbucket.org/org/repo.git";

  private static final String BITBUCKET_REPOSITORY_URL_SANITIZED = "https://bitbucket.org/org/repo";

  private static final String AZURE_REPO_URL = "https://dev.azure.com/org/prj/_git/app";

  private static final String SSH_REPOSITORY_URL = "ssh://a.com/b/c";

  @Rule
  public final WireMockRule gitService = new WireMockRule(wireMockConfig().dynamicPort());

  @Before
  public void setup() {
    gitService.stubFor(get(urlPathEqualTo("/api/v3/user"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{\"username\":\"foo\"}")));
    gitService.stubFor(get(urlPathMatching("/api/v3/repos/.*/.*"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{ \"private\": false }")));
  }

  @After
  public void after() {
    ApiCompositeSourceControlConfigValidatorService.disableSshForFunctionalTest = false;
  }

  @Override
  @Before
  public void init() {
    super.init();
    application = tempEntity.newApplicationWithParent(getClass().getSimpleName() + "ȧpp", YE_OLE_APPLICATION,
        YE_OLE_ORGANIZATION);
    organization = organizationDAO.getById(application.getOrganizationId());
  }

  private void mockValidationRequest(final String repoPath) {
    gitService.stubFor(post(urlPathEqualTo("/api/v3/repos" + repoPath + "/pulls"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withStatus(422)
            .withStatusMessage("Unprocessable Entity")));
  }

  @Test
  public void testSourceControlEditor() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));
    verifyStartNoSourceControl();

    assertSourceControlDoesNotExist(rootOrganization.getId());
    assertSourceControlDoesNotExist(organization.getId());
    assertSourceControlDoesNotExist(application.getId());
    metricsTable().shouldBe(visible);
  }

  @Test
  public void testSourceControlEditor_EmptySourceControlWithProviderOnRoot() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    verifyStartNoSourceControl();

    assertSourceControlDoesNotExist(rootOrganization.getId());
    assertSourceControlDoesNotExist(organization.getId());
    assertSourceControlDoesNotExist(application.getId());

    tempEntity.newSourceControl(rootOrganization.getId(), null, null, SourceControlProvider.GITHUB);

    refresh();

    assertSourceControlDoesNotExist(organization.getId());
    assertSourceControlDoesNotExist(application.getId());

    SourceControlEditorPage.token().shouldBe(visible, enabled);
    SourceControlEditorPage.credentialsFieldset()
        .mainLabel()
        .shouldHave(attribute("title", "Access token cannot be inherited. No inheritable access token defined."));
    SourceControlEditorPage.credentialsFieldset()
        .labels()
        .shouldHave(texts("Inherit (Not Configured)", "Override"));
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldBe(disabled);
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldBe(selected, enabled);
    SourceControlEditorPage.tokenWarning().shouldBe(visible);
    SourceControlEditorPage.repositoryUrl().shouldBe(empty);
  }

  @Test
  public void testSourceControlEditor_EmptySourceControlRootWithProviderAndToken() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    verifyStartNoSourceControl();

    assertSourceControlDoesNotExist(rootOrganization.getId());
    assertSourceControlDoesNotExist(organization.getId());
    assertSourceControlDoesNotExist(application.getId());

    tempEntity.newSourceControl(rootOrganization.getId(), null, TOKEN, SourceControlProvider.GITHUB);

    refresh();

    assertSourceControlDoesNotExist(organization.getId());
    assertSourceControlDoesNotExist(application.getId());

    verifyStartWithSourceControlInherited();
    SourceControlEditorPage.token().shouldBe(visible, disabled);
    SourceControlEditorPage.credentialsFieldset().shouldBe(visible);
    SourceControlEditorPage.credentialsFieldset()
        .labels()
        .shouldHave(
            CollectionCondition.texts(String.format("Inherit from %s", rootOrganization.getName()), "Override"));
    SourceControlEditorPage.credentialsFieldset().radioInputs().forEach(input -> input.shouldBe(enabled));
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldBe(selected);
    SourceControlEditorPage.tokenWarning().shouldNotBe(visible);
    SourceControlEditorPage.repositoryUrl().shouldBe(empty);
  }

  @Test
  public void testSourceControlEditor_bitbucketEmptySourceControlRootWithProviderAndToken() {
    final String rootUsername = "rootusername";

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    verifyStartNoSourceControl();

    assertSourceControlDoesNotExist(rootOrganization.getId());
    assertSourceControlDoesNotExist(organization.getId());
    assertSourceControlDoesNotExist(application.getId());

    tempEntity
        .newSourceControl(rootOrganization.getId(), null, rootUsername, TOKEN, SourceControlProvider.BITBUCKET, true,
            false, "master", null);

    refresh();

    assertSourceControlDoesNotExist(organization.getId());
    assertSourceControlDoesNotExist(application.getId());

    verifyStartWithSourceControlInherited();
    SourceControlEditorPage.token().shouldBe(visible, disabled);
    SourceControlEditorPage.username().shouldBe(visible, disabled);

    SourceControlEditorPage.credentialsFieldset().shouldBe(visible);
    SourceControlEditorPage.credentialsFieldset()
        .labels()
        .shouldHave(
            CollectionCondition.texts(String.format("Inherit from %s", rootOrganization.getName()), "Override"));
    SourceControlEditorPage.credentialsFieldset().radioInputs().forEach(input -> input.shouldBe(enabled));
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldBe(selected);
    SourceControlEditorPage.repositoryUrl().shouldBe(empty);
  }

  @Test
  public void testSourceControlEditor_EmptySourceControlRootWithProviderOrgWithToken() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    verifyStartNoSourceControl();

    assertSourceControlDoesNotExist(rootOrganization.getId());
    assertSourceControlDoesNotExist(organization.getId());
    assertSourceControlDoesNotExist(application.getId());

    tempEntity.newSourceControl(rootOrganization.getId(), null, TOKEN, SourceControlProvider.GITHUB);
    tempEntity.newSourceControl(organization.getId(), null, TOKEN, null);

    refresh();

    assertSourceControlDoesNotExist(application.getId());

    verifyStartWithSourceControlInherited();
    SourceControlEditorPage.token().shouldBe(visible, disabled);
    SourceControlEditorPage.credentialsFieldset().shouldBe(visible);
    SourceControlEditorPage.credentialsFieldset()
        .labels()
        .shouldHave(
            CollectionCondition.texts(String.format("Inherit from %s", organization.getName()), "Override"));
    SourceControlEditorPage.credentialsFieldset().radioInputs().forEach(input -> input.shouldBe(enabled));
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldBe(selected);
    SourceControlEditorPage.tokenWarning().shouldNotBe(visible);
    SourceControlEditorPage.repositoryUrl().shouldBe(empty);
  }

  @Test
  public void testSourceControlEditor_bitbucketEmptySourceControlRootWithProviderOrgWithToken() {
    final String rootUsername = "rootusername";
    final String orgUsername = "orguser";
    final String baseBranch = null;

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    verifyStartNoSourceControl();

    assertSourceControlDoesNotExist(rootOrganization.getId());
    assertSourceControlDoesNotExist(organization.getId());
    assertSourceControlDoesNotExist(application.getId());

    tempEntity
        .newSourceControl(rootOrganization.getId(), null, rootUsername, TOKEN, SourceControlProvider.BITBUCKET, true,
            false, "master", null);
    tempEntity.newSourceControl(organization.getId(), null, orgUsername, TOKEN, null, true, false, baseBranch, null);

    refresh();

    assertSourceControlDoesNotExist(application.getId());

    verifyStartWithSourceControlInherited();
    SourceControlEditorPage.token().shouldBe(visible, disabled);
    SourceControlEditorPage.username().shouldBe(visible, disabled);

    SourceControlEditorPage.credentialsFieldset().shouldBe(visible);
    SourceControlEditorPage.credentialsFieldset()
        .labels()
        .shouldHave(
            CollectionCondition.texts(String.format("Inherit from %s", organization.getName()), "Override"));
    SourceControlEditorPage.credentialsFieldset().radioInputs().forEach(input -> input.shouldBe(enabled));
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldBe(selected);
    SourceControlEditorPage.repositoryUrl().shouldBe(empty);

    eyesWatcher.eyesCheck("Source Control Editor Default State With Bitbucket and Inherited Credentials");
  }

  @Test
  public void testSourceControlEditor_OrganizationSourceControlExistsWithURLAndToken() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    verifyStartNoSourceControl();

    assertSourceControlDoesNotExist(rootOrganization.getId());
    assertSourceControlDoesNotExist(organization.getId());
    assertSourceControlDoesNotExist(application.getId());

    tempEntity.newSourceControl(rootOrganization.getId(), null, TOKEN, SourceControlProvider.GITHUB);
    tempEntity.newSourceControl(organization.getId(), null, TOKEN, null);
    tempEntity.newSourceControl(application.getId(), REPOSITORY_URL, TOKEN, null);

    refresh();

    verifyStartWithSourceControl();
    SourceControlEditorPage.token().shouldBe(visible, enabled);
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));
    SourceControlEditorPage.credentialsFieldset().shouldBe(visible);
    SourceControlEditorPage.credentialsFieldset()
        .labels()
        .shouldHave(
            CollectionCondition.texts(String.format("Inherit from %s", organization.getName()), "Override"));
    SourceControlEditorPage.credentialsFieldset().radioInputs().forEach(input -> input.shouldBe(enabled));
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldBe(selected);
    SourceControlEditorPage.repositoryUrl().shouldHave(value(REPOSITORY_URL));
  }

  @Test
  public void testSourceControlEditor_testConfiguration_noSsh() {
    // given: we go to the source control editor
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    // when: we click the test config
    SourceControlEditorPage.testConfigButton().click();

    // then: we see the test results
    final TestResults testResults = SourceControlEditorPage.testResults();
    testResults.title().shouldHave(text("Configuration Test Results"));
    testResults.rows().shouldHave(CONFIG_TEST_NAMES);
    // and: the config is incomplete
    testResults.rows().get(0).shouldHave(matchText("required.*missing"));

    mockValidationRequest(REPOSITORY_PATH);
    // when: we add source control configs
    tempEntity.newSourceControl(rootOrganization.getId(), null, TOKEN, SourceControlProvider.GITHUB);
    tempEntity.newSourceControl(organization.getId(), null, TOKEN, null);
    tempEntity.newSourceControl(application.getId(), gitService.baseUrl() + REPOSITORY_PATH, TOKEN, null);
    refresh();

    // and: we test configs again
    SourceControlEditorPage.testConfigButton().click();

    // then: we see new test results
    final TestResults completeTestResults = SourceControlEditorPage.testResults();
    completeTestResults.title().shouldHave(text("Configuration Test Results"));
    completeTestResults.rows().shouldHave(CONFIG_TEST_NAMES);

    eyesWatcher.eyesCheck("Source Control Editor Test Config Results");

    // when: we make a change and save
    SourceControlEditorPage.repositoryUrl().setValue(REPOSITORY_URL + "-changed");
    SourceControlEditorPage.saveButton().shouldBe(enabled);
    SourceControlEditorPage.saveButton().click();

    SourceControlRepositoryUrlUpdateModal.root().shouldBe(visible);
    SourceControlRepositoryUrlUpdateModal.continueButton().click();
    FormMask.seeAndWaitForDismissal();
    SourceControlRepositoryUrlUpdateModal.root().shouldBe(hidden);

    // then the test results are hidden
    SourceControlEditorPage.testResultsElement().shouldNot(exist);
  }

  @Test
  public void testSourceControlEditor_testConfiguration_sshEnabled() {
    // given Source Control with SSH Enabled
    mockValidationRequest(REPOSITORY_PATH);
    ApiCompositeSourceControlConfigValidatorService.disableSshForFunctionalTest = true;
    tempEntity.newSourceControl(rootOrganization.getId(), null, TOKEN, SourceControlProvider.GITHUB);
    tempEntity.newSourceControl(
        new SourceControl.Builder()
            .setOwnerId(application.getId())
            .setRepositoryUrl(gitService.baseUrl() + REPOSITORY_PATH)
            .setRepositorySshUrl(REPOSITORY_SSH_URL)
            .setSshEnabled(true)
            .build());

    // given: we go to the source control editor
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    // when: we click the test config
    SourceControlEditorPage.testConfigButton().click();

    // then: we see the test results with no validation errors
    SourceControlEditorPage.validationError().shouldNotBe(visible);
    final TestResults testResults = SourceControlEditorPage.testResults();
    testResults.title().shouldHave(text("Configuration Test Results"));
    testResults.rows().shouldHave(CONFIG_TEST_SSH_NAMES);
  }

  @Test
  public void testSourceControlEditor_updateToken() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));
    verifyStartNoSourceControl();
    tempEntity.newSourceControl(rootOrganization.getId(), null, null, SourceControlProvider.GITHUB);
    refresh();

    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldBe(selected);
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldNotBe(selected, enabled);
    SourceControlEditorPage.token().shouldBe(enabled);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.saveButton().click();
    FormUtils.getAlertElement(SourceControlEditorPage.root())
        .shouldBe(visible)
        .shouldHave(text(FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX + " There are no changes to save."));

    SourceControlEditorPage.token().click();
    SourceControlEditorPage.token().setValue(TOKEN);
    SourceControlEditorPage.saveButton().shouldNotBe(visible);

    SourceControlEditorPage.repositoryUrl().setValue(REPOSITORY_URL);
    SourceControlEditorPage.saveButton().shouldBe(visible, enabled);
    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldBe(selected);
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldNotBe(selected, enabled);
    SourceControlEditorPage.repositoryUrl().shouldHave(value(REPOSITORY_URL));
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));

    tempEntity.newSourceControl(organization.getId(), null, TOKEN, null);
    refresh();
    metricsTable().shouldBe(visible);
    assertThat(metricsTable().rowCount()).isEqualTo(1);
    assertThat(metricsTable().getRow(0)).extracting(MetricsTableRow::isEmpty).isEqualTo(true);

    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldBe(selected);
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldNotBe(selected, disabled);
    SourceControlEditorPage.repositoryUrl().shouldHave(value(REPOSITORY_URL));
    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.saveButton().click();
    FormUtils.getAlertElement(SourceControlEditorPage.root())
        .shouldBe(visible)
        .shouldHave(text(FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX + " There are no changes to save."));

    SourceControlEditorPage.credentialsFieldset().labels().get(0).click();
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldNotBe(selected);
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldBe(selected);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.saveButton().shouldNotHave(DISABLED);

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.saveButton().click();
    FormUtils.getAlertElement(SourceControlEditorPage.root())
        .shouldBe(visible)
        .shouldHave(text(FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX + " There are no changes to save."));
    SourceControlEditorPage.resetButton().shouldBe(enabled);
    SourceControlEditorPage.token().shouldHave(value(""));
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldNotBe(selected);
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldBe(selected, enabled);
    SourceControlEditorPage.token().shouldBe(disabled);
  }

  @Test
  public void testSourceControlEditor_bitbucketUpdateCredentials() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));
    verifyStartNoSourceControl();
    tempEntity
        .newSourceControl(rootOrganization.getId(), null, null, null, SourceControlProvider.BITBUCKET, true,
            false, "master", null);
    refresh();

    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldBe(selected);
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldNotBe(selected, enabled);
    SourceControlEditorPage.token().shouldBe(enabled);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.saveButton().click();
    FormUtils.getAlertElement(SourceControlEditorPage.root())
        .shouldBe(visible)
        .shouldHave(text(FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX + " There are no changes to save."));

    SourceControlEditorPage.token().click();
    SourceControlEditorPage.token().setValue(TOKEN);
    SourceControlEditorPage.saveButton().shouldNotBe(visible);

    SourceControlEditorPage.username().click();
    SourceControlEditorPage.username().setValue("appuser");
    SourceControlEditorPage.saveButton().shouldNotBe(visible);

    SourceControlEditorPage.repositoryUrl().setValue(BITBUCKET_REPOSITORY_URL);
    SourceControlEditorPage.saveButton().shouldBe(enabled);
    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.providerFieldset().radioInputs().get(0).shouldBe(selected);
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldBe(enabled);
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldNotBe(selected, enabled);
    SourceControlEditorPage.repositoryUrl().shouldHave(value(BITBUCKET_REPOSITORY_URL_SANITIZED));
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));

    final String baseBranch = null;
    tempEntity.newSourceControl(organization.getId(), null, "orgUser", TOKEN, null, null, null, baseBranch, null);

    refresh();

    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldNotBe(selected, disabled);
    SourceControlEditorPage.repositoryUrl().shouldHave(value(BITBUCKET_REPOSITORY_URL_SANITIZED));
    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.saveButton().click();
    FormUtils.getAlertElement(SourceControlEditorPage.root())
        .shouldBe(visible)
        .shouldHave(text(FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX + " There are no changes to save."));

    SourceControlEditorPage.credentialsFieldset().labels().get(0).click();
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldBe(selected);
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldNotBe(selected);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"));

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().click();
    FormUtils.getAlertElement(SourceControlEditorPage.root())
        .shouldBe(visible)
        .shouldHave(text(FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX + " There are no changes to save."));
    SourceControlEditorPage.resetButton().shouldBe(enabled);
    SourceControlEditorPage.token().shouldHave(value(""));
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldNotBe(selected);
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldBe(selected, enabled);
    SourceControlEditorPage.token().shouldBe(disabled);
    metricsTable().shouldBe(visible);
  }

  @Test
  public void testSourceControlEditor_azureCredentials() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));
    verifyStartNoSourceControl();
    tempEntity
        .newSourceControl(rootOrganization.getId(), null, null, null, SourceControlProvider.AZURE, true,
            false, "master", null);
    refresh();

    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldBe(selected);
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldNotBe(selected, enabled);
    SourceControlEditorPage.token().shouldBe(enabled);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.saveButton().click();
    FormUtils.getAlertElement(SourceControlEditorPage.root())
        .shouldBe(visible)
        .shouldHave(text(FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX + " There are no changes to save."));

    SourceControlEditorPage.token().click();
    SourceControlEditorPage.token().setValue(TOKEN);
    SourceControlEditorPage.saveButton().shouldNotBe(visible);

    SourceControlEditorPage.username().click();
    SourceControlEditorPage.username().setValue("appuser");
    SourceControlEditorPage.saveButton().shouldNotBe(visible);

    SourceControlEditorPage.repositoryUrl().setValue(AZURE_REPO_URL);
    SourceControlEditorPage.saveButton().shouldBe(enabled);
    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();
  }

  @Test
  public void testSourceControlEditor_updateFailure() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));
    verifyStartNoSourceControl();
    tempEntity.newSourceControl(rootOrganization.getId(), null, TOKEN, SourceControlProvider.GITHUB);
    refresh();

    SourceControlEditorPage.credentialsFieldset().labels().get(1).click();
    SourceControlEditorPage.token().setValue(TOKEN);
    SourceControlEditorPage.repositoryUrl().setValue(REPOSITORY_URL);

    // Create the entry to make the insert fail
    tempEntity.newSourceControl(application.getId(), REPOSITORY_URL, null, null);
    SourceControlEditorPage.saveButton().click();

    FormMask.seeAndWaitForDismissal();

    FormUtils.getErrorElement(SourceControlEditorPage.root())
        .shouldBe(visible)
        .shouldHave(text(
            FormUtils.DEFAULT_ERROR_SAVING_DATA_PREFIX +
                " SourceControl already exists for application with id: " + application.getPublicId()));
    assertSourceControl(application.getId(), REPOSITORY_URL, null, null);

    // Delete the entry to resolve error condition
    deleteSourceControl(application.getId());

    FormUtils.getRetryButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldBe(selected);
    SourceControlEditorPage.repositoryUrl().shouldHave(value(REPOSITORY_URL));
  }

  @Test
  public void testSourceControlEditor_updateWithSshUrl() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));
    verifyStartNoSourceControl();
    tempEntity.newSourceControl(rootOrganization.getId(), null, TOKEN, SourceControlProvider.GITHUB);
    refresh();

    SourceControlEditorPage.repositoryUrl().setValue(SSH_REPOSITORY_URL);

    SourceControlEditorPage.saveButton().click();
    FormUtils.getAlertElement(SourceControlEditorPage.root())
        .shouldBe(visible)
        .shouldHave(text(
            FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX + " Unable to save: fields with invalid or missing data"));
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldNotBe(visible);
    SourceControlEditorPage.repositoryUrl().shouldHave(value(SSH_REPOSITORY_URL));
  }

  @Test
  public void testSourceControlEditor_tokenWarning() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    verifyStartNoSourceControl();

    assertSourceControlDoesNotExist(rootOrganization.getId());
    assertSourceControlDoesNotExist(organization.getId());
    assertSourceControlDoesNotExist(application.getId());

    SourceControlEditorPage.tokenWarning().shouldBe(visible);

    tempEntity.newSourceControl(rootOrganization.getId(), null, null, SourceControlProvider.GITHUB);

    refresh();

    assertSourceControlDoesNotExist(organization.getId());
    assertSourceControlDoesNotExist(application.getId());

    SourceControlEditorPage.tokenWarning().shouldBe(visible);
    SourceControlEditorPage.token().setValue(TOKEN);
    SourceControlEditorPage.tokenWarning().shouldNotBe(visible);

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.tokenWarning().shouldNotBe(visible);

    tempEntity.newSourceControl(organization.getId(), null, TOKEN, null);
    refresh();

    SourceControlEditorPage.tokenWarning().shouldNotBe(visible);
    SourceControlEditorPage.credentialsFieldset().labels().get(1).click();
    SourceControlEditorPage.tokenWarning().shouldNotBe(visible);

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.tokenWarning().shouldNotBe(visible);
  }

  @Test
  public void testSourceControlEditor_defaultBranch() {

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    verifyStartNoSourceControl();

    SourceControlEditorPage.baseBranchFieldset().shouldBe(visible);
    SourceControlEditorPage.baseBranchFieldset()
        .radioInputs()
        .forEach(input -> input.shouldBe(disabled));
    SourceControlEditorPage.baseBranchFieldset().radioInputs().get(0).shouldBe(selected);
    SourceControlEditorPage.baseBranchFieldset()
        .labels()
        .shouldHave(texts("Inherit (Not Configured)", "Override"));
    SourceControlEditorPage.baseBranchInput().shouldHave(value(""));
    SourceControlEditorPage.baseBranchInput().shouldBe(disabled);

    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.GITHUB, true, true, "master");
    tempEntity.newSourceControl(application.getId(), REPOSITORY_URL, null, null, null, null, null);
    refresh();

    SourceControlEditorPage.baseBranchFieldset().shouldBe(visible);
    SourceControlEditorPage.baseBranchFieldset().radioInputs().forEach(input -> input.shouldBe(enabled));
    SourceControlEditorPage.baseBranchFieldset().radioInputs().get(0).shouldBe(selected);
    SourceControlEditorPage.baseBranchFieldset()
        .labels()
        .shouldHave(texts(String.format("Inherit from %s", rootOrganization.getName()),
            "Override"));
    SourceControlEditorPage.baseBranchInput().shouldHave(value(""));
    SourceControlEditorPage.baseBranchInput().shouldBe(disabled);

    SourceControlEditorPage.baseBranchFieldset().labels().get(1).click();
    SourceControlEditorPage.baseBranchFieldset().radioInputs().get(1).shouldBe(selected);
    SourceControlEditorPage.baseBranchInput().shouldBe(enabled);
    SourceControlEditorPage.baseBranchInput().shouldHave(value(""));
    SourceControlEditorPage.baseBranchInput().click();
    SourceControlEditorPage.saveButton().click();
    FormUtils.getAlertElement(SourceControlEditorPage.root())
        .shouldBe(visible)
        .shouldHave(text(
            FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX + " Unable to save: fields with invalid or missing data"));

    SourceControlEditorPage.baseBranchInput().setValue("develop");
    SourceControlEditorPage.baseBranchInput().shouldHave(value("develop"));
    SourceControlEditorPage.saveButton().shouldNotHave(DISABLED);

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.baseBranchInput().shouldHave(value("develop"));
  }

  @Test
  public void testSourceControlEditor_delete() {
    tempEntity.newSourceControl(rootOrganization.getId(), null, TOKEN, SourceControlProvider.GITHUB);
    tempEntity.newSourceControl(organization.getId(), null, TOKEN, null);
    tempEntity.newSourceControl(application.getId(), REPOSITORY_URL, TOKEN, null);

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    verifyStartWithSourceControl();

    SourceControlEditorPage.resetButton().click();

    DeleteModal.root().shouldBe(visible);
    DeleteModal.header().shouldHave(text("Reset Source Control"));
    DeleteModal.body()
        .shouldHave(text("You are about to reset the Source Control configuration for " +
            "Ye Ole Application. This action cannot be undone."));

    DeleteModal.continueButton().click();
    FormMask.seeAndWaitForDismissal();
    DeleteModal.root().shouldBe(hidden);

    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.resetButton().shouldBe(disabled);
    assertSourceControlDoesNotExist(application.getId());
  }

  @Test
  public void testSourceControlEditor_deleteFailure() {
    tempEntity.newSourceControl(rootOrganization.getId(), null, TOKEN, SourceControlProvider.GITHUB);
    tempEntity.newSourceControl(organization.getId(), null, TOKEN, null);
    tempEntity.newSourceControl(application.getId(), REPOSITORY_URL, TOKEN, null);

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    verifyStartWithSourceControl();
    SourceControlEditorPage.resetButton().click();

    DeleteModal.root().shouldBe(visible);
    DeleteModal.header().shouldHave(text("Reset Source Control"));
    DeleteModal.body()
        .shouldHave(text("You are about to reset the Source Control configuration for " +
            "Ye Ole Application. This action cannot be undone."));

    // delete entry to create error condition
    deleteSourceControl(application.getId());

    DeleteModal.continueButton().click();

    DeleteModal.error()
        .shouldHave(text(
            "An error occurred saving data. Cannot find SourceControl for application with id: " +
                application.getPublicId()));
    DeleteModal.retryButton().shouldBe(visible, enabled);

    // recreate the entry to resolve error condition
    tempEntity.newSourceControl(application.getId(), REPOSITORY_URL, TOKEN, null);

    DeleteModal.retryButton().click();

    FormMask.seeAndWaitForDismissal();
    DeleteModal.root().shouldBe(hidden);

    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.resetButton().shouldBe(disabled);
    assertSourceControlDoesNotExist(application.getId());
  }

  @Test
  public void testSourceControlEditor_LicensingAwareNotificationsAndSourceControlOnly() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));
    setFeatures(LicensedFeature.NOTIFICATIONS, LicensedFeature.SOURCE_CONTROL);
    refresh();

    verifyNotificationFeaturesOnly();

    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.GITLAB, true, true, "master");

    refresh();
    verifyNotificationFeaturesOnly();
  }

  @Test
  public void testSourceControlEditor_LicensingAwareNoLicense() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    refresh();

    SourceControlEditorPage.root().shouldBe(visible);
    SourceControlEditorPage.title().shouldHave(text("Source Control Configuration"));
    SourceControlEditorPage.subTitle()
        .shouldHave(text("Configures the integration with an external SCM for Ye Ole Application"));
    SourceControlEditorPage.form().shouldNotBe(visible);
    SourceControlEditorPage.notSupported().shouldBe(visible);
    SourceControlEditorPage.notSupported().shouldHave(text("Source control is not supported by your license"));
  }

  @Test
  public void testSourceControlEditor_metricsTable() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, "token", SourceControlProvider.GITHUB);
    tempEntity.newSourceControl(application.getId(), REPOSITORY_URL, null, null);
    addSourceControlPullRequestResults();
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    metricsTable().scrollIntoView();
    metricsTable().shouldBe(visible);

    assertThat(metricsTable().rowCount()).isEqualTo(3);

    MetricsTableRow row1 = metricsTable().getRow(0);
    row1.title().shouldHave(exactText("Bump bar to 1.1"));
    row1.statusIcon().shouldHave(cssClass("fa-circle-check"));
    row1.statusIcon().hover();
    row1.statusIconTooltip().should(exist).shouldHave(exactText(String.format(SUCCESS_MESSAGE, "foo : bar : 1.0")));
    row1.totalTime().shouldHave(exactText("0"));
    row1.started().shouldNotBe(empty);

    MetricsTableRow row2 = metricsTable().getRow(1);
    row2.title().shouldHave(exactText("Bump bar to 1.2"));
    row2.statusIcon().shouldHave(cssClass("fa-circle-xmark"));
    row2.statusIcon().hover();
    row2.statusIconTooltip().should(exist).shouldHave(exactText(EXCEPTION_MESSAGE));
    row2.totalTime().shouldHave(exactText("0"));
    row2.started().shouldNotBe(empty);

    MetricsTableRow row3 = metricsTable().getRow(2);
    row3.title().shouldHave(exactText("Bump bar to 1.4"));
    row3.statusIcon().shouldHave(cssClass("fa-exclamation-triangle"));
    row3.statusIcon().hover();
    row3.statusIconTooltip().should(exist).shouldHave(exactText(String.format(FAILURE_MESSAGE, "foo : bar : 1.3")));
    row3.totalTime().shouldHave(exactText("0"));
    row3.started().shouldNotBe(empty);
  }

  @Test
  public void testSourceControlEditor_testUpdateRepositoryUrl() {
    // given: we set up source control and go to the source control editor
    tempEntity.newSourceControl(rootOrganization.getId(), null, TOKEN, SourceControlProvider.GITHUB);
    tempEntity.newSourceControl(organization.getId(), null, TOKEN, null);
    tempEntity.newSourceControl(application.getId(), REPOSITORY_URL, TOKEN, null);
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    // when: we make a change to repo url and save
    SourceControlEditorPage.repositoryUrl().setValue(REPOSITORY_URL + "-changed");
    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().click();

    // then the confirmation modal is shown
    SourceControlRepositoryUrlUpdateModal.root().shouldBe(visible);
    SourceControlRepositoryUrlUpdateModal.continueButton().click();
    FormMask.seeAndWaitForDismissal();
    SourceControlRepositoryUrlUpdateModal.root().shouldBe(hidden);
  }

  @Test
  public void testSourceControlEditor_testUpdateNotRepoUrl() {
    // given: we set up source control and go to the source control editor
    tempEntity.newSourceControl(rootOrganization.getId(), null, TOKEN, SourceControlProvider.GITHUB);
    tempEntity.newSourceControl(organization.getId(), null, TOKEN, null);
    tempEntity.newSourceControl(application.getId(), REPOSITORY_URL, TOKEN, null);
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    // when: we make a change to repo url and save
    SourceControlEditorPage.remediationPullRequestsFieldset().labels().get(1).click();
    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().click();

    SourceControlRepositoryUrlUpdateModal.root().shouldBe(hidden);
    FormMask.seeAndWaitForDismissal();
    SourceControlRepositoryUrlUpdateModal.root().shouldBe(hidden);
  }

  @Test
  public void testSourceControlEditor_providerAtOrg_tokenAtRoot_updateToken() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));
    verifyStartNoSourceControl();
    // given root with a token & provider, and a suborg with a provider, token should be always required in the frontend
    tempEntity.newSourceControl(rootOrganization.getId(), null, TOKEN, SourceControlProvider.GITHUB);
    tempEntity.newSourceControl(organization.getId(), null, TOKEN, SourceControlProvider.GITLAB);
    refresh();

    // then the token at the root is 'hidden' by the provider at the suborg. Token is required
    SourceControlEditorPage.credentialsFieldset().shouldBe(visible);
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldBe(enabled, selected);
    SourceControlEditorPage.credentialsFieldset()
        .labels()
        .get(0)
        .shouldHave(text("Inherit from "
            + organization.getName()));
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldBe(enabled);
    SourceControlEditorPage.token().shouldBe(visible, disabled);
    SourceControlEditorPage.providerFieldset().shouldBe(visible);
    SourceControlEditorPage.providerFieldset().radioInputs().get(1).shouldBe(enabled);
    SourceControlEditorPage.providerFieldset().radioInputs().get(0).shouldBe(selected, enabled);
    SourceControlEditorPage.providerFieldset()
        .labels()
        .get(0)
        .shouldHave(text("Inherit from " + organization.getName()));

    // update fails because no fields have been updated
    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.saveButton().click();
    FormUtils.getAlertElement(SourceControlEditorPage.root())
        .shouldBe(visible)
        .shouldHave(text(FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX + " There are no changes to save."));

    // when we set a repository URL
    SourceControlEditorPage.repositoryUrl().setValue(REPOSITORY_URL);

    // then save is enabled because token is inherited
    SourceControlEditorPage.saveButton().shouldBe(visible);

    // when we set the token
    SourceControlEditorPage.credentialsFieldset().labels().get(1).click();
    SourceControlEditorPage.token().click();
    SourceControlEditorPage.token().setValue(TOKEN);

    // then the save button should be enabled
    SourceControlEditorPage.saveButton().shouldBe(visible);

    // when we save the current changes
    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    // and the forms values are set correctly
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldBe(selected);
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldNotBe(selected);
    SourceControlEditorPage.repositoryUrl().shouldHave(value(REPOSITORY_URL));
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));
    SourceControlEditorPage.providerFieldset().radioInputs().get(0).shouldBe(selected);

    // when we switch to bitbucket as a provider
    SourceControlEditorPage.username().shouldNotBe(visible);
    SourceControlEditorPage.providerFieldset().labels().get(1).click();
    SourceControlEditorPage.providerSelect().chooseOption(new Option(2, "Bitbucket"));

    // then we should see the username/token credentials input fields
    SourceControlEditorPage.credentialsFieldset().shouldBe(visible);
    SourceControlEditorPage.username().shouldBe(visible, enabled);

    // and the page should block updates until the username is provided
    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.saveButton().click();
    FormUtils.getAlertElement(SourceControlEditorPage.root())
        .shouldBe(visible)
        .shouldHave(text(
            FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX + " Unable to save: fields with invalid or missing data"));

    // when we provide a username
    SourceControlEditorPage.username().setValue("myuser");

    // then updates are enabled again
    SourceControlEditorPage.saveButton().shouldBe(enabled);
  }

  @Test
  public void testSourceControlEditor_overrideProvider() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));
    verifyStartNoSourceControl();
    // given root with a token & provider, and a suborg with a provider and token
    tempEntity.newSourceControl(rootOrganization.getId(), null, TOKEN, SourceControlProvider.GITHUB);
    tempEntity.newSourceControl(organization.getId(), null, TOKEN, SourceControlProvider.GITLAB);
    refresh();

    // then the URL is required
    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.saveButton().click();
    FormUtils.getAlertElement(SourceControlEditorPage.root())
        .shouldBe(visible)
        .shouldHave(text(FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX + " There are no changes to save."));
    SourceControlEditorPage.saveButton().shouldNotBe(visible);

    // when we set a repository URL
    SourceControlEditorPage.repositoryUrl().setValue(REPOSITORY_URL);

    // then save is enabled
    SourceControlEditorPage.saveButton().shouldBe(visible);

    // when we override the provider
    SourceControlEditorPage.providerFieldset().labels().get(1).click();
    SourceControlEditorPage.providerSelect().chooseOption(new NxFormSelect.Option(3, "Github"));

    // then the token is required
    SourceControlEditorPage.saveButton().click();
    SourceControlEditorPage.saveButton().shouldNotBe(visible);
    FormUtils.getAlertElement(SourceControlEditorPage.root())
        .shouldBe(visible)
        .shouldHave(text(
            FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX + " Unable to save: fields with invalid or missing data"));

    // when we provide the token
    SourceControlEditorPage.token().setValue("my token value");

    // then save is enabled
    SourceControlEditorPage.saveButton().shouldBe(enabled);
  }

  @Test
  public void testSourceControlEditor_manualPullRequests() {
    refresh();
    Selenide.sleep(1000);

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    verifyStartNoSourceControl();
    SourceControlEditorPage.manualPullRequestsFieldset().toggle().shouldNotBe(visible);
    SourceControlEditorPage.manualPullRequestsFieldset().shouldBe(visible);
    SourceControlEditorPage.manualPullRequestsFieldset()
        .radioInputs()
        .forEach(input -> input.shouldBe(disabled));
    SourceControlEditorPage.manualPullRequestsFieldset().radioInputs().get(1).shouldBe(selected);
    SourceControlEditorPage.manualPullRequestsFieldset()
        .labels()
        .shouldHave(texts("Inherit (Not Configured)", "Enabled", "Disabled"));

    assertSourceControlDoesNotExist(rootOrganization.getId());
    assertSourceControlDoesNotExist(organization.getId());
    assertSourceControlDoesNotExist(application.getId());

    // root organization source control with manual pull requests enabled
    tempEntity.newSourceControl(
        rootOrganization.getId(), null, null, null, TOKEN, SourceControlProvider.GITHUB, false, true, "main", null,
        true, true, null, null, true, true, null);

    refresh();

    // manual pull requests is inherited from root
    SourceControlEditorPage.manualPullRequestsFieldset().shouldBe(visible);
    SourceControlEditorPage.manualPullRequestsFieldset().radioInputs().get(0).shouldBe(selected);
    SourceControlEditorPage.manualPullRequestsFieldset()
        .labels()
        .shouldHave(texts(String.format("Inherit from %s", rootOrganization.getName()),
            "Enabled", "Disabled"));
    assertSourceControlDoesNotExist(application.getId());

    // application source control
    tempEntity.newSourceControl(application.getId(), REPOSITORY_URL, TOKEN, null);
    refresh();

    // Override manual pull requests setting
    SourceControlEditorPage.manualPullRequestsFieldset().labels().get(1).click();
    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.manualPullRequestsFieldset().radioInputs().get(1).shouldBe(selected);
    assertSourceControlManualPullRequest(application.getId(), true);

    // disable it
    SourceControlEditorPage.manualPullRequestsFieldset().labels().get(2).click();
    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.manualPullRequestsFieldset().radioInputs().get(2).shouldBe(selected);
    assertSourceControlManualPullRequest(application.getId(), false);

    // Back to inherit
    SourceControlEditorPage.manualPullRequestsFieldset().labels().get(0).click();
    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.manualPullRequestsFieldset().radioInputs().get(0).shouldBe(selected);
    assertSourceControlManualPullRequest(application.getId(), null);
  }

  @Test
  public void testSourceControlEditor_innerSourceAutomatedUpdates() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    verifyStartNoSourceControl();
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().toggle().shouldNotBe(visible);
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().shouldBe(visible);
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset()
        .radioInputs()
        .forEach(input -> input.shouldBe(disabled));
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().radioInputs().get(1).shouldBe(selected);
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset()
        .labels()
        .shouldHave(texts("Inherit (Not Configured)", "Enabled", "Disabled"));

    assertSourceControlDoesNotExist(rootOrganization.getId());
    assertSourceControlDoesNotExist(organization.getId());
    assertSourceControlDoesNotExist(application.getId());

    // root organization source control with InnerSource automated updates enabled
    tempEntity.newSourceControl(
        rootOrganization.getId(), null, null, null, TOKEN, SourceControlProvider.GITHUB, false, true, "main", null,
        true, true, null, null, true, true, true);

    refresh();

    // InnerSource automated updates is inherited from root
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().shouldBe(visible);
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().radioInputs().get(0).shouldBe(selected);
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset()
        .labels()
        .shouldHave(texts(String.format("Inherit from %s", rootOrganization.getName()),
            "Enabled", "Disabled"));
    assertSourceControlDoesNotExist(application.getId());

    // application source control
    tempEntity.newSourceControl(application.getId(), REPOSITORY_URL, TOKEN, null);
    refresh();

    // override InnerSource automated updates setting
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().labels().get(1).click();
    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().radioInputs().get(1).shouldBe(selected);
    assertSourceControlInnerSourceAutomatedUpdates(application.getId(), true);

    // disable it
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().labels().get(2).click();
    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().radioInputs().get(2).shouldBe(selected);
    assertSourceControlInnerSourceAutomatedUpdates(application.getId(), false);

    // back to inherit
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().labels().get(0).click();
    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().radioInputs().get(0).shouldBe(selected);
    assertSourceControlManualPullRequest(application.getId(), null);
  }

  @Override
  protected void verifyStartNoSourceControl() {
    System.out.println("verifyStartNoSourceControl: Application id: " + application.getId() + ", public id: "
        + application.getPublicId());
    SourceControlEditorPage.root().shouldBe(visible);
    SourceControlEditorPage.title().shouldHave(text("Source Control Configuration"));
    SourceControlEditorPage.subTitle()
        .shouldHave(text("Configures the integration with an external SCM for " + application.getName()));

    SourceControlEditorPage.repositoryUrlControls().shouldBe(visible);

    SourceControlEditorPage.providerSelect().shouldBe(visible);
    SourceControlEditorPage.token().shouldBe(visible, disabled);

    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.saveButton().click();
    FormUtils.getAlertElement(SourceControlEditorPage.root())
        .shouldBe(visible)
        .shouldHave(text(FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX + " There are no changes to save."));
    SourceControlEditorPage.testConfigButton().shouldBe(visible);

    SourceControlEditorPage.credentialsFieldset().shouldBe(visible);
    SourceControlEditorPage.credentialsFieldset().radioInputs().forEach(input -> input.shouldBe(disabled));
    SourceControlEditorPage.credentialsFieldset()
        .labels()
        .shouldHave(texts("Inherit (Not Configured)", "Override"));
    SourceControlEditorPage.tokenWarning().shouldBe(visible);

    SourceControlEditorPage.repositoryUrl().shouldBe(empty);
    SourceControlEditorPage.repositoryUrl().shouldBe(visible, enabled);

    SourceControlEditorPage.sshEnabledFieldset().toggle().shouldNotBe(visible);
    SourceControlEditorPage.sshEnabledFieldset().shouldBe(visible);
    SourceControlEditorPage.sshEnabledFieldset().radioInputs().forEach(input -> input.shouldBe(disabled));
    SourceControlEditorPage.sshEnabledFieldset().radioInputs().get(0).shouldBe(selected);
    SourceControlEditorPage.sshEnabledFieldset()
        .labels()
        .shouldHave(texts("Inherit (Not Configured)", "Enabled", "Disabled"));

    SourceControlEditorPage.remediationPullRequestsFieldset().toggle().shouldNotBe(visible);
    SourceControlEditorPage.remediationPullRequestsFieldset().shouldBe(visible);
    SourceControlEditorPage.remediationPullRequestsFieldset()
        .radioInputs()
        .forEach(input -> input.shouldBe(disabled));
    SourceControlEditorPage.remediationPullRequestsFieldset().radioInputs().get(2).shouldBe(selected);
    SourceControlEditorPage.remediationPullRequestsFieldset()
        .labels()
        .shouldHave(texts("Inherit (Not Configured)", "Enabled", "Disabled"));
    // SourceControlEditorPage.remediationPullRequestNotSupportedAlert().shouldNotBe(visible); TODO CLM-26277

    SourceControlEditorPage.pullRequestCommentingFieldset().toggle().shouldNotBe(visible);
    SourceControlEditorPage.pullRequestCommentingFieldset().shouldBe(visible);
    SourceControlEditorPage.pullRequestCommentingFieldset()
        .radioInputs()
        .forEach(input -> input.shouldBe(disabled));
    SourceControlEditorPage.pullRequestCommentingFieldset().radioInputs().get(1).shouldBe(selected);
    SourceControlEditorPage.pullRequestCommentingFieldset()
        .labels()
        .shouldHave(texts("Inherit (Not Configured)", "Enabled", "Disabled"));
    // SourceControlEditorPage.pullRequestCommentingNotSupportedAlert().shouldNotBe(visible); TODO CLM-26277

    SourceControlEditorPage.sourceControlEvaluationsFieldset().toggle().shouldNotBe(visible);
    SourceControlEditorPage.sourceControlEvaluationsFieldset().shouldBe(visible);
    SourceControlEditorPage.sourceControlEvaluationsFieldset()
        .radioInputs()
        .forEach(input -> input.shouldBe(disabled));
    SourceControlEditorPage.sourceControlEvaluationsFieldset().radioInputs().get(1).shouldBe(selected);
    SourceControlEditorPage.sourceControlEvaluationsFieldset()
        .labels()
        .shouldHave(texts("Inherit (Not Configured)", "Enabled", "Disabled"));
    // SourceControlEditorPage.sourceControlEvaluationsNotSupportedAlert().shouldNotBe(visible); TODO CLM-26277

    SourceControlEditorPage.automatedCommitFeedbackFieldset().toggle().shouldNotBe(visible);
    SourceControlEditorPage.automatedCommitFeedbackFieldset().shouldBe(visible);
    SourceControlEditorPage.automatedCommitFeedbackFieldset()
        .radioInputs()
        .forEach(input -> input.shouldBe(disabled));
    SourceControlEditorPage.automatedCommitFeedbackFieldset().radioInputs().get(1).shouldBe(selected);
    SourceControlEditorPage.automatedCommitFeedbackFieldset()
        .labels()
        .shouldHave(texts("Inherit (Not Configured)", "Enabled", "Disabled"));
    // SourceControlEditorPage.sourceControlEvaluationsNotSupportedAlert().shouldNotBe(visible); TODO CLM-26277

    SourceControlEditorPage.baseBranchFieldset().shouldBe(visible);
    SourceControlEditorPage.baseBranchFieldset()
        .radioInputs()
        .forEach(input -> input.shouldBe(disabled));
    SourceControlEditorPage.baseBranchFieldset().radioInputs().get(0).shouldBe(selected);
    SourceControlEditorPage.baseBranchFieldset()
        .labels()
        .shouldHave(texts("Inherit (Not Configured)", "Override"));
    SourceControlEditorPage.baseBranchInput().shouldBe(visible, disabled);
    SourceControlEditorPage.baseBranchInput().shouldHave(value(""));
    // SourceControlEditorPage.defaultBranchNotSupportedAlert().shouldNotBe(visible); TODO CLM-26277

    SourceControlEditorPage.manualPullRequestsFieldset().toggle().shouldNotBe(visible);
    SourceControlEditorPage.manualPullRequestsFieldset().shouldBe(visible);
    SourceControlEditorPage.manualPullRequestsFieldset()
        .radioInputs()
        .forEach(input -> input.shouldBe(disabled));
    SourceControlEditorPage.manualPullRequestsFieldset().radioInputs().get(1).shouldBe(selected);
    SourceControlEditorPage.manualPullRequestsFieldset()
        .labels()
        .shouldHave(texts("Inherit (Not Configured)", "Enabled", "Disabled"));
    // SourceControlEditorPage.defaultBranchNotSupportedAlert().shouldNotBe(visible); TODO CLM-26277

    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().toggle().shouldNotBe(visible);
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().shouldBe(visible);
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset()
        .radioInputs()
        .forEach(input -> input.shouldBe(disabled));
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().radioInputs().get(1).shouldBe(selected);
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset()
        .labels()
        .shouldHave(texts("Inherit (Not Configured)", "Enabled", "Disabled"));
    // SourceControlEditorPage.defaultBranchNotSupportedAlert().shouldNotBe(visible); TODO CLM-26277
  }

  @Override
  protected void verifyStartWithSourceControl() {
    verifyStartWithSourceControl(false);
  }

  private void verifyStartWithSourceControlInherited() {
    verifyStartWithSourceControl(true);
  }

  private void verifyStartWithSourceControl(boolean inherited) {
    SourceControlEditorPage.root().shouldBe(visible);
    SourceControlEditorPage.title().shouldHave(text("Source Control Configuration"));
    SourceControlEditorPage.subTitle()
        .shouldHave(text("Configures the integration with an external SCM for " + application.getName()));
    SourceControlEditorPage.repositoryUrlControls().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.resetButton().shouldBe(inherited ? disabled : enabled);
    SourceControlEditorPage.saveButton().click();
    FormUtils.getAlertElement(SourceControlEditorPage.root())
        .shouldBe(visible)
        .shouldHave(text(FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX + " There are no changes to save."));
    SourceControlEditorPage.testConfigButton().shouldBe(visible);
    SourceControlEditorPage.repositoryUrl().shouldBe(visible, enabled);
    SourceControlEditorPage.defaultBranchNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.remediationPullRequestNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.pullRequestCommentingNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.sourceControlEvaluationsNotSupportedAlert().shouldNotBe(visible);
  }

  private void addSourceControlPullRequestResults() {
    SourceControlPullRequestMetrics metrics =
        testCLMServer.getCLMServer().getInstance(SourceControlPullRequestMetrics.class);
    PullRequestResult success = new PullRequestResult();
    success.setSuccessful(true);
    metrics.addResult(application.getId(),
        new EnhancedPullRequestResult(success, new Date(System.currentTimeMillis() - 1000), ComponentIdentifier
            .createMavenCoordinates("foo", "bar", "1.0"), "Bump bar to 1.1", false));

    PullRequestResult failure = new PullRequestResult();
    failure.setSuccessful(false);
    metrics.addResult(application.getId(),
        new EnhancedPullRequestResult(failure, new Date(System.currentTimeMillis() - 2000), ComponentIdentifier
            .createMavenCoordinates("foo", "bar", "1.1"), "Bump bar to 1.2", true));

    PullRequestResult warning = new PullRequestResult();
    warning.setSuccessful(false);
    metrics.addResult(application.getId(),
        new EnhancedPullRequestResult(warning, new Date(System.currentTimeMillis() - 3000), ComponentIdentifier
            .createMavenCoordinates("foo", "bar", "1.3"), "Bump bar to 1.4", false));
  }
}
