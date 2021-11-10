/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.elements.DeleteModal;
import com.sonatype.clm.testing.functional.elements.Dropdown.Option;
import com.sonatype.clm.testing.functional.elements.ErrorBox;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.pages.SourceControlEditorPage;
import com.sonatype.clm.testing.functional.pages.SourceControlEditorPage.MetricsTableRow;
import com.sonatype.clm.testing.functional.pages.SourceControlEditorPage.TestResults;
import com.sonatype.clm.testing.functional.pages.SourceControlRepositoryUrlUpdateModal;
import com.sonatype.insight.brain.git.EnhancedPullRequestResult;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.telemetry.SourceControlPullRequestMetrics;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.nexus.iq.manager.PullRequestResult;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.codeborne.selenide.CollectionCondition;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.matchText;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.clm.testing.functional.pages.SourceControlEditorPage.metricsTable;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControl.FAKE_SECRET_KEY;
import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationSourceControlEditorTest
    extends AbstractSourceControlEditorTest
{
  public static final CollectionCondition CONFIG_TEST_NAMES = texts(
      "Is the configuration complete?",
      "Is the repository private?",
      "Does the token have sufficient permissions?");

  public static final CollectionCondition CONFIG_TEST_SSH_NAMES = texts(
      "Is the configuration complete?",
      "Is the repository private?",
      "Does the token have sufficient permissions?",
      "Is SSH configured fully?");

  private Application application;

  private static final String REPOSITORY_URL = "https://a.com/b/c";

  private static final String REPOSITORY_SSH_URL = "git@a.com:b/c.git";

  private static final String BITBUCKET_REPOSITORY_URL = "https://bitbucket.org/org/repo.git";

  private static final String BITBUCKET_REPOSITORY_URL_SANITIZED = "https://bitbucket.org/org/repo";

  private static final String AZURE_REPO_URL = "https://dev.azure.com/org/prj/_git/app";

  private static final String SSH_REPOSITORY_URL = "ssh://a.com/b/c";

  @Override
  @Before
  public void init() {
    super.init();
    application = tempEntity.newApplicationWithParent(getClass().getSimpleName() + "ȧpp", YE_OLE_APPLICATION,
        YE_OLE_ORGANIZATION);
    organization = organizationDAO.getById(application.getOrganizationId());
  }

  @Test
  public void testSourceControlEditor() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    verifyStartNoSourceControl();

    eyesWatcher.eyesCheck("Source Control Editor Application Default State");

    assertSourceControlDoesNotExist(rootOrganization.getId());
    assertSourceControlDoesNotExist(organization.getId());
    assertSourceControlDoesNotExist(application.getId());
    metricsTable().shouldNotBe(visible);
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

    SourceControlEditorPage.advancedSettings().shouldBe(visible);
    SourceControlEditorPage.advancedSectionRule().shouldBe(visible);
    SourceControlEditorPage.token().shouldBe(visible, enabled);
    SourceControlEditorPage.tokenInheritRadio().label().shouldHave(text("Inherit (Not Configured)"));
    SourceControlEditorPage.tokenInheritRadio().shouldBe(visible, disabled);
    SourceControlEditorPage.tokenInheritRadio().shouldNotBe(selected);
    SourceControlEditorPage.tokenInheritRadio().label().hover();
    assertToolTip("Access token cannot be inherited. No inheritable access token defined.");
    SourceControlEditorPage.tokenOverrideRadio().label().shouldHave(text("Override"));
    SourceControlEditorPage.tokenOverrideRadio().shouldBe(visible, enabled);
    SourceControlEditorPage.tokenOverrideRadio().shouldBe(selected);
    SourceControlEditorPage.tokenWarning().shouldBe(visible);
    SourceControlEditorPage.repositoryUrl().shouldHave(text(""));

    eyesWatcher.eyesCheck("Source Control Editor Default State With Provider");
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
    SourceControlEditorPage.advancedSettings().shouldNotBe(visible);
    SourceControlEditorPage.advancedSettingsTree().click();
    SourceControlEditorPage.advancedSettings().shouldBe(visible);
    SourceControlEditorPage.token().shouldBe(visible, disabled);
    SourceControlEditorPage.tokenInheritRadio().label()
        .shouldHave(text(String.format("Inherit from %s", rootOrganization.getName())));
    SourceControlEditorPage.tokenInheritRadio().shouldBe(visible, enabled, selected);
    SourceControlEditorPage.tokenOverrideRadio().label().shouldHave(text("Override"));
    SourceControlEditorPage.tokenOverrideRadio().shouldBe(visible, enabled);
    SourceControlEditorPage.tokenOverrideRadio().shouldNotBe(selected);
    SourceControlEditorPage.tokenWarning().shouldNotBe(visible);
    SourceControlEditorPage.repositoryUrl().shouldHave(text(""));
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
    SourceControlEditorPage.advancedSettings().shouldNotBe(visible);
    SourceControlEditorPage.advancedSettingsTree().click();
    SourceControlEditorPage.advancedSettings().shouldBe(visible);
    SourceControlEditorPage.token().shouldNotBe(visible);
    SourceControlEditorPage.credentialsToken().shouldBe(visible, disabled);
    SourceControlEditorPage.credentialsUsername().shouldBe(visible, disabled);

    SourceControlEditorPage.credentialsInheritRadio().label()
        .shouldHave(text(String.format("Inherit from %s (%s)", rootOrganization.getName(), rootUsername)));
    SourceControlEditorPage.credentialsInheritRadio().shouldBe(visible, enabled, selected);
    SourceControlEditorPage.credentialsOverrideRadio().label().shouldHave(text("Override"));
    SourceControlEditorPage.credentialsOverrideRadio().shouldBe(visible, enabled);
    SourceControlEditorPage.credentialsOverrideRadio().shouldNotBe(selected);
    SourceControlEditorPage.repositoryUrl().shouldHave(text(""));
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
    SourceControlEditorPage.advancedSettings().shouldNotBe(visible);
    SourceControlEditorPage.advancedSettingsTree().click();
    SourceControlEditorPage.advancedSettings().shouldBe(visible);
    SourceControlEditorPage.token().shouldBe(visible, disabled);
    SourceControlEditorPage.tokenInheritRadio().label()
        .shouldHave(text(String.format("Inherit from %s", organization.getName())));
    SourceControlEditorPage.tokenInheritRadio().shouldBe(visible, enabled, selected);
    SourceControlEditorPage.tokenOverrideRadio().label().shouldHave(text("Override"));
    SourceControlEditorPage.tokenOverrideRadio().shouldBe(visible, enabled);
    SourceControlEditorPage.tokenOverrideRadio().shouldNotBe(selected);
    SourceControlEditorPage.tokenWarning().shouldNotBe(visible);
    SourceControlEditorPage.repositoryUrl().shouldHave(text(""));
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
    SourceControlEditorPage.advancedSettings().shouldNotBe(visible);
    SourceControlEditorPage.advancedSettingsTree().click();
    SourceControlEditorPage.advancedSettings().shouldBe(visible);
    SourceControlEditorPage.token().shouldNotBe(visible);
    SourceControlEditorPage.credentialsToken().shouldBe(visible, disabled);
    SourceControlEditorPage.credentialsUsername().shouldBe(visible, disabled);

    SourceControlEditorPage.credentialsInheritRadio().label()
        .shouldHave(text(String.format("Inherit from %s (%s)", organization.getName(), orgUsername)));
    SourceControlEditorPage.credentialsInheritRadio().shouldBe(visible, enabled, selected);
    SourceControlEditorPage.credentialsOverrideRadio().label().shouldHave(text("Override"));
    SourceControlEditorPage.credentialsOverrideRadio().shouldBe(visible, enabled);
    SourceControlEditorPage.credentialsOverrideRadio().shouldNotBe(selected);
    SourceControlEditorPage.repositoryUrl().shouldHave(text(""));

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
    SourceControlEditorPage.advancedSettings().shouldNotBe(visible);
    SourceControlEditorPage.advancedSettingsTree().click();
    SourceControlEditorPage.advancedSettings().shouldBe(visible);
    SourceControlEditorPage.token().shouldBe(visible, enabled);
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));
    SourceControlEditorPage.tokenInheritRadio().label()
        .shouldHave(text(String.format("Inherit from %s", organization.getName())));
    SourceControlEditorPage.tokenInheritRadio().shouldBe(visible, enabled);
    SourceControlEditorPage.tokenInheritRadio().shouldNotBe(selected);
    SourceControlEditorPage.tokenOverrideRadio().label().shouldHave(text("Override"));
    SourceControlEditorPage.tokenOverrideRadio().shouldBe(visible, enabled);
    SourceControlEditorPage.tokenOverrideRadio().shouldBe(selected);
    SourceControlEditorPage.providerWarning().shouldNotBe(visible);
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

    // when: we add source control configs
    tempEntity.newSourceControl(rootOrganization.getId(), null, TOKEN, SourceControlProvider.GITHUB);
    tempEntity.newSourceControl(organization.getId(), null, TOKEN, null);
    tempEntity.newSourceControl(application.getId(), REPOSITORY_URL, TOKEN, null);
    refresh();

    // and: we test configs again
    SourceControlEditorPage.testConfigButton().click();

    // then: we see new test results
    final TestResults completeTestResults = SourceControlEditorPage.testResults();
    completeTestResults.title().shouldHave(text("Configuration Test Results"));
    completeTestResults.rows().shouldHave(CONFIG_TEST_NAMES);

    // and: the token failed to checkout due to a bad host in url
    completeTestResults.rows().get(2).shouldHave(text("unknown host"));

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
    tempEntity.newSourceControl(rootOrganization.getId(), null, TOKEN, SourceControlProvider.GITHUB);
    tempEntity.newSourceControl(
        new SourceControl.Builder()
            .setOwnerId(application.getId())
            .setRepositoryUrl(REPOSITORY_URL)
            .setRepositorySshUrl(REPOSITORY_SSH_URL)
            .setSshEnabled(true)
            .build());

    // given: we go to the source control editor
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    // when: we click the test config
    SourceControlEditorPage.testConfigButton().click();

    // then: we see the test results
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

    SourceControlEditorPage.tokenOverrideRadio().shouldBe(selected);
    SourceControlEditorPage.tokenInheritRadio().shouldNotBe(selected, enabled);
    SourceControlEditorPage.token().shouldBe(enabled);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("There are no changes to update.");

    SourceControlEditorPage.token().click();
    SourceControlEditorPage.token().setValue(TOKEN);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("Unable to update: fields with invalid or missing data.");

    SourceControlEditorPage.repositoryUrl().setValue(REPOSITORY_URL);
    SourceControlEditorPage.saveButton().shouldBe(enabled);
    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.tokenOverrideRadio().shouldBe(selected);
    SourceControlEditorPage.tokenInheritRadio().shouldNotBe(selected, enabled);
    SourceControlEditorPage.repositoryUrl().shouldHave(value(REPOSITORY_URL));
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));

    tempEntity.newSourceControl(organization.getId(), null, TOKEN, null);
    refresh();

    SourceControlEditorPage.advancedSettings().shouldNotBe(visible);
    SourceControlEditorPage.advancedSettingsTree().click();
    SourceControlEditorPage.advancedSettings().shouldBe(visible);
    metricsTable().shouldBe(visible);
    assertThat(metricsTable().rowCount()).isEqualTo(1);
    assertThat(metricsTable().getRow(0)).extracting(MetricsTableRow::isEmpty).isEqualTo(true);

    SourceControlEditorPage.tokenOverrideRadio().shouldBe(selected);
    SourceControlEditorPage.tokenInheritRadio().shouldNotBe(selected, disabled);
    SourceControlEditorPage.repositoryUrl().shouldHave(value(REPOSITORY_URL));
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("There are no changes to update.");

    SourceControlEditorPage.tokenInheritRadio().click();
    SourceControlEditorPage.tokenOverrideRadio().shouldNotBe(selected);
    SourceControlEditorPage.tokenInheritRadio().shouldBe(selected);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.saveButton().shouldNotHave(DISABLED);

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("There are no changes to update.");
    SourceControlEditorPage.deleteButton().shouldBe(enabled);
    SourceControlEditorPage.token().shouldHave(value(""));
    SourceControlEditorPage.tokenOverrideRadio().shouldNotBe(selected);
    SourceControlEditorPage.tokenInheritRadio().shouldBe(selected, enabled);
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

    SourceControlEditorPage.credentialsOverrideRadio().shouldBe(selected);
    SourceControlEditorPage.credentialsInheritRadio().shouldNotBe(selected, enabled);
    SourceControlEditorPage.credentialsToken().shouldBe(enabled);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("There are no changes to update.");

    SourceControlEditorPage.credentialsToken().click();
    SourceControlEditorPage.credentialsToken().setValue(TOKEN);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("There are no changes to update.");

    SourceControlEditorPage.credentialsUsername().click();
    SourceControlEditorPage.credentialsUsername().setValue("appuser");
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("Unable to update: fields with invalid or missing data.");

    SourceControlEditorPage.repositoryUrl().setValue(BITBUCKET_REPOSITORY_URL);
    SourceControlEditorPage.saveButton().shouldBe(enabled);
    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.advancedSettings().shouldNotBe(visible);
    SourceControlEditorPage.advancedSettingsTree().click();
    SourceControlEditorPage.advancedSettings().shouldBe(visible);

    SourceControlEditorPage.providerInheritRadio().shouldBe(selected);
    SourceControlEditorPage.credentialsOverrideRadio().shouldBe(enabled);
    SourceControlEditorPage.credentialsInheritRadio().shouldNotBe(selected, enabled);
    SourceControlEditorPage.repositoryUrl().shouldHave(value(BITBUCKET_REPOSITORY_URL_SANITIZED));
    SourceControlEditorPage.credentialsToken().shouldHave(value(FAKE_SECRET_KEY));

    final String baseBranch = null;
    tempEntity.newSourceControl(organization.getId(), null, "orgUser", TOKEN, null, null, null, baseBranch, null);

    refresh();

    SourceControlEditorPage.advancedSettings().shouldNotBe(visible);
    SourceControlEditorPage.advancedSettingsTree().click();
    SourceControlEditorPage.advancedSettings().shouldBe(visible);

    SourceControlEditorPage.credentialsInheritRadio().shouldNotBe(selected, disabled);
    SourceControlEditorPage.repositoryUrl().shouldHave(value(BITBUCKET_REPOSITORY_URL_SANITIZED));
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("There are no changes to update.");

    SourceControlEditorPage.credentialsInheritRadio().click();
    SourceControlEditorPage.credentialsOverrideRadio().shouldNotBe(selected);
    SourceControlEditorPage.credentialsInheritRadio().shouldBe(selected);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.saveButton().shouldNotHave(DISABLED);

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("There are no changes to update.");
    SourceControlEditorPage.deleteButton().shouldBe(enabled);
    SourceControlEditorPage.credentialsToken().shouldHave(value(""));
    SourceControlEditorPage.credentialsOverrideRadio().shouldNotBe(selected);
    SourceControlEditorPage.credentialsInheritRadio().shouldBe(selected, enabled);
    SourceControlEditorPage.credentialsToken().shouldBe(disabled);
    metricsTable().shouldBe(visible);

    eyesWatcher.eyesCheck("Source Control Editor Save State With Bitbucket and Overridden Credentials");
  }

  @Test
  public void testSourceControlEditor_azureCredentials() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));
    verifyStartNoSourceControl();
    tempEntity
        .newSourceControl(rootOrganization.getId(), null, null, null, SourceControlProvider.AZURE, true,
            false, "master", null);
    refresh();

    SourceControlEditorPage.credentialsOverrideRadio().shouldBe(selected);
    SourceControlEditorPage.credentialsInheritRadio().shouldNotBe(selected, enabled);
    SourceControlEditorPage.credentialsToken().shouldBe(enabled);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("There are no changes to update.");

    SourceControlEditorPage.credentialsToken().click();
    SourceControlEditorPage.credentialsToken().setValue(TOKEN);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("There are no changes to update.");

    SourceControlEditorPage.credentialsUsername().click();
    SourceControlEditorPage.credentialsUsername().setValue("appuser");
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("Unable to update: fields with invalid or missing data.");

    SourceControlEditorPage.repositoryUrl().setValue(AZURE_REPO_URL);
    SourceControlEditorPage.saveButton().shouldBe(enabled);
    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.advancedSettings().shouldNotBe(visible);
  }

  @Test
  public void testSourceControlEditor_updateFailure() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));
    verifyStartNoSourceControl();
    tempEntity.newSourceControl(rootOrganization.getId(), null, TOKEN, SourceControlProvider.GITHUB);
    refresh();

    SourceControlEditorPage.advancedSettings().shouldNotBe(visible);
    SourceControlEditorPage.advancedSettingsTree().click();
    SourceControlEditorPage.advancedSettings().shouldBe(visible);

    SourceControlEditorPage.tokenOverrideRadio().click();
    SourceControlEditorPage.token().setValue(TOKEN);
    SourceControlEditorPage.repositoryUrl().setValue(REPOSITORY_URL);

    //Create the entry to make the insert fail
    tempEntity.newSourceControl(application.getId(), REPOSITORY_URL, null, null);
    SourceControlEditorPage.saveButton().click();

    FormMask.seeAndWaitForDismissal();

    final ErrorBox errorBox = PAGE.error();
    errorBox.retryButton().shouldBe(visible, enabled);
    errorBox.shouldHave(text("SourceControl already exists for application with id: " + application.getPublicId()));
    assertSourceControl(application.getId(), REPOSITORY_URL, null, null);

    eyesWatcher.eyesCheck("Source Control Editor update Failed");

    //Delete the entry to resolve error condition
    deleteSourceControl(application.getId());

    errorBox.retryButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));
    SourceControlEditorPage.tokenOverrideRadio().shouldBe(selected);
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
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.repositoryUrl().shouldHave(value(SSH_REPOSITORY_URL));
  }

  @Test
  public void testSourceControlEditor_ExpandCollapseAdvancedSection() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    verifyStartNoSourceControl();

    assertSourceControlDoesNotExist(rootOrganization.getId());
    assertSourceControlDoesNotExist(organization.getId());
    assertSourceControlDoesNotExist(application.getId());

    SourceControlEditorPage.advancedSettings().shouldBe(visible);
    eyesWatcher.eyesCheck("Source Control Editor - Advanced controls expanded");

    tempEntity.newSourceControl(rootOrganization.getId(), null, null, SourceControlProvider.GITHUB);

    refresh();

    assertSourceControlDoesNotExist(organization.getId());
    assertSourceControlDoesNotExist(application.getId());

    SourceControlEditorPage.advancedSettings().shouldBe(visible);
    SourceControlEditorPage.advancedSettingsTree().click();
    SourceControlEditorPage.advancedSettings().shouldBe(visible);
    SourceControlEditorPage.token().setValue(TOKEN);
    SourceControlEditorPage.advancedSettingsTree().click();
    SourceControlEditorPage.advancedSettings().shouldNotBe(visible);

    eyesWatcher.eyesCheck("Source Control Editor - Advanced controls Collapsed");

    tempEntity.newSourceControl(organization.getId(), null, TOKEN, null);
    refresh();

    SourceControlEditorPage.advancedSettings().shouldNotBe(visible);
    SourceControlEditorPage.advancedSettingsTree().click();
    SourceControlEditorPage.advancedSettings().shouldBe(visible);
    SourceControlEditorPage.tokenOverrideRadio().click();
    SourceControlEditorPage.advancedSettingsTree().click();
    SourceControlEditorPage.advancedSettings().shouldBe(visible);
    SourceControlEditorPage.token().setValue(TOKEN);
    SourceControlEditorPage.advancedSettingsTree().click();
    SourceControlEditorPage.advancedSettings().shouldNotBe(visible);
    SourceControlEditorPage.advancedSettingsTree().click();
    SourceControlEditorPage.advancedSettings().shouldBe(visible);
  }

  @Test
  public void testSourceControlEditor_tokenWarning() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    verifyStartNoSourceControl();

    assertSourceControlDoesNotExist(rootOrganization.getId());
    assertSourceControlDoesNotExist(organization.getId());
    assertSourceControlDoesNotExist(application.getId());

    SourceControlEditorPage.tokenWarning().shouldBe(visible);
    eyesWatcher.eyesCheck("Source Control Editor - Token and provider warnings visible");

    tempEntity.newSourceControl(rootOrganization.getId(), null, null, SourceControlProvider.GITHUB);

    refresh();

    assertSourceControlDoesNotExist(organization.getId());
    assertSourceControlDoesNotExist(application.getId());

    SourceControlEditorPage.tokenWarning().shouldBe(visible);
    SourceControlEditorPage.token().setValue(TOKEN);
    SourceControlEditorPage.tokenWarning().shouldBe(visible);

    SourceControlEditorPage.saveButton().hover();
    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.tokenWarning().shouldBe(visible);

    tempEntity.newSourceControl(organization.getId(), null, TOKEN, null);
    refresh();

    SourceControlEditorPage.advancedSettings().shouldNotBe(visible);
    SourceControlEditorPage.advancedSettingsTree().click();
    SourceControlEditorPage.advancedSettings().shouldBe(visible);
    SourceControlEditorPage.tokenWarning().shouldNotBe(visible);
    SourceControlEditorPage.tokenOverrideRadio().click();
    SourceControlEditorPage.tokenWarning().shouldNotBe(visible);

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.tokenWarning().shouldNotBe(visible);
  }

  @Test
  public void testSourceControlEditor_defaultBranch() {

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    verifyStartNoSourceControl();
    SourceControlEditorPage.baseBranchInheritRadio().shouldBe(selected, disabled);
    SourceControlEditorPage.baseBranchInheritRadio().shouldHave(text("Inherit (Not Configured)"));
    SourceControlEditorPage.baseBranchOverrideRadio().shouldNotBe(selected, enabled);
    SourceControlEditorPage.baseBranchInput().shouldHave(value(""));
    SourceControlEditorPage.baseBranchInput().shouldBe(disabled);
    SourceControlEditorPage.saveButton().shouldHave(DISABLED);

    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.GITHUB, true, true, "master");
    tempEntity.newSourceControl(application.getId(), REPOSITORY_URL, null, null, null, null, null);
    refresh();

    SourceControlEditorPage.advancedSettings().shouldNotBe(visible);
    SourceControlEditorPage.advancedElementsTrigger().click();
    SourceControlEditorPage.advancedSettings().shouldBe(visible);

    SourceControlEditorPage.baseBranchInheritRadio().shouldBe(selected, enabled);
    SourceControlEditorPage.baseBranchInheritRadio()
        .shouldHave(text(String.format("Inherit from %s (%s)", rootOrganization.getName(), "master")));
    SourceControlEditorPage.baseBranchOverrideRadio().shouldNotBe(selected, disabled);
    SourceControlEditorPage.baseBranchInput().shouldHave(value(""));
    SourceControlEditorPage.baseBranchInput().shouldBe(disabled);
    SourceControlEditorPage.saveButton().shouldHave(DISABLED);

    SourceControlEditorPage.baseBranchOverrideRadio().click();
    SourceControlEditorPage.baseBranchOverrideRadio().shouldBe(selected);
    SourceControlEditorPage.baseBranchInput().shouldBe(enabled);
    SourceControlEditorPage.baseBranchInput().shouldHave(value(""));
    SourceControlEditorPage.saveButton().shouldHave(DISABLED);
    SourceControlEditorPage.baseBranchInput().click();
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("Unable to update: fields with invalid or missing data.");

    SourceControlEditorPage.advancedElementsTrigger().click();
    SourceControlEditorPage.advancedSettings().shouldBe(visible);

    SourceControlEditorPage.baseBranchInput().setValue("develop");
    SourceControlEditorPage.baseBranchInput().shouldHave(value("develop"));
    SourceControlEditorPage.saveButton().shouldNotHave(DISABLED);

    SourceControlEditorPage.advancedElementsTrigger().click();
    SourceControlEditorPage.advancedSettings().shouldNotBe(visible);
    SourceControlEditorPage.advancedElementsTrigger().click();
    SourceControlEditorPage.advancedSettings().shouldBe(visible);

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.advancedSettings().shouldNotBe(visible);
    SourceControlEditorPage.advancedElementsTrigger().click();
    SourceControlEditorPage.advancedSettings().shouldBe(visible);
    SourceControlEditorPage.baseBranchInput().shouldHave(value("develop"));
  }

  @Test
  public void testSourceControlEditor_delete() {
    tempEntity.newSourceControl(rootOrganization.getId(), null, TOKEN, SourceControlProvider.GITHUB);
    tempEntity.newSourceControl(organization.getId(), null, TOKEN, null);
    tempEntity.newSourceControl(application.getId(), REPOSITORY_URL, TOKEN, null);

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    verifyStartWithSourceControl();

    SourceControlEditorPage.deleteButton().click();

    DeleteModal.root().shouldBe(visible);
    DeleteModal.header().shouldHave(text("Reset Source Control"));
    DeleteModal.body().shouldHave(text("You are about to reset the Source Control configuration for application " +
        "Ye Ole Application. This action cannot be undone."));

    DeleteModal.continueButton().click();
    FormMask.seeAndWaitForDismissal();
    DeleteModal.root().shouldBe(hidden);

    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.deleteButton().shouldBe(disabled);
    assertSourceControlDoesNotExist(application.getId());
  }

  @Test
  public void testSourceControlEditor_deleteFailure() {
    tempEntity.newSourceControl(rootOrganization.getId(), null, TOKEN, SourceControlProvider.GITHUB);
    tempEntity.newSourceControl(organization.getId(), null, TOKEN, null);
    tempEntity.newSourceControl(application.getId(), REPOSITORY_URL, TOKEN, null);

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    verifyStartWithSourceControl();
    SourceControlEditorPage.deleteButton().click();

    DeleteModal.root().shouldBe(visible);
    DeleteModal.header().shouldHave(text("Reset Source Control"));
    DeleteModal.body().shouldHave(text("You are about to reset the Source Control configuration for application " +
        "Ye Ole Application. This action cannot be undone."));

    // delete entry to create error condition
    deleteSourceControl(application.getId());

    DeleteModal.continueButton().click();

    DeleteModal.error()
        .shouldHave(text("Cannot find SourceControl for application with id: " + application.getPublicId()));
    DeleteModal.retryButton().shouldBe(visible, enabled);

    // recreate the entry to resolve error condition
    tempEntity.newSourceControl(application.getId(), REPOSITORY_URL, TOKEN, null);

    DeleteModal.retryButton().click();

    FormMask.seeAndWaitForDismissal();
    DeleteModal.root().shouldBe(hidden);

    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.deleteButton().shouldBe(disabled);
    assertSourceControlDoesNotExist(application.getId());
  }

  @Test
  public void testSourceControlEditor_LicensingAwareNotificationOnly() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));
    setLicensedProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    refresh();

    SourceControlEditorPage.advancedElementsTrigger().click();

    verifyNotificationFeaturesOnly();
    SourceControlEditorPage.saveButton().shouldHave(DISABLED);

    eyesWatcher.eyesCheck("Source Control Editor - application configurations disabled, no automation");

    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.GITLAB, true, true, "master");

    refresh();
    SourceControlEditorPage.advancedElementsTrigger().click();

    verifyNotificationFeaturesOnly();
    SourceControlEditorPage.saveButton().shouldHave(DISABLED);
  }

  @Test
  public void testSourceControlEditor_LicensingAwareNoLicense() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    refresh();

    SourceControlEditorPage.root().shouldBe(visible);
    SourceControlEditorPage.title().shouldHave(text("Source Control Configuration"));
    SourceControlEditorPage.subTitle().shouldHave(text(String
        .format("Configures the integration with an external SCM for the %s application", application.getName())));
    SourceControlEditorPage.form().shouldNotBe(visible);
    SourceControlEditorPage.notSupported().shouldBe(visible);
    SourceControlEditorPage.notSupported().shouldHave(text("Source Control is not supported by your license"));
    eyesWatcher.eyesCheck("Source Control Editor - No License");
  }

  @Test
  public void testSourceControlEditor_metricsTable() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, "token", SourceControlProvider.GITHUB);
    tempEntity.newSourceControl(application.getId(), REPOSITORY_URL, null, null);
    addSourceControlPullRequestResults();
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));
    SourceControlEditorPage.advancedSettingsTree().click();

    metricsTable().scrollIntoView();
    metricsTable().shouldBe(visible);

    assertThat(metricsTable().rowCount()).isEqualTo(2);

    MetricsTableRow row1 = metricsTable().getRow(0);
    assertThat(row1.isPopulated()).isTrue();
    assertThat(row1.title()).isEqualTo("Bump bar to 1.1");
    assertThat(row1.created()).isTrue();
    assertThat(row1.errors()).isEqualTo("false");
    assertThat(row1.totalTime()).isEqualTo("0");
    assertThat(row1.started()).isNotEmpty();

    MetricsTableRow row2 = metricsTable().getRow(1);
    assertThat(row2.isPopulated()).isTrue();
    assertThat(row2.title()).isEqualTo("Bump bar to 1.2");
    assertThat(row2.created()).isFalse();
    assertThat(row2.errors()).isEqualTo("true");
    assertThat(row2.totalTime()).isEqualTo("0");
    assertThat(row2.started()).isNotEmpty();
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
    SourceControlEditorPage.saveButton().shouldBe(enabled);
    SourceControlEditorPage.saveButton().click();

    // then the confirmation modal is shown
    eyesWatcher.eyesCheck("Source Control Editor - Show confirmation dialog for updating repo url");
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
    SourceControlEditorPage.advancedSettingsTree().click();
    SourceControlEditorPage.remediationPullRequestsEnableRadio().click();
    SourceControlEditorPage.saveButton().shouldBe(enabled);
    SourceControlEditorPage.saveButton().click();

    // then the confirmation modal is shown
    SourceControlRepositoryUrlUpdateModal.root().shouldBe(hidden);
    FormMask.seeAndWaitForDismissal();
    SourceControlRepositoryUrlUpdateModal.root().shouldBe(hidden);
  }

  @Test
  public void testSourceControlEditor_providerAtOrg_tokenAtRoot_updateToken() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));
    verifyStartNoSourceControl();
    // given root with a token & provider, and a suborg with a provider but NO token
    tempEntity.newSourceControl(rootOrganization.getId(), null, TOKEN, SourceControlProvider.GITHUB);
    tempEntity.newSourceControl(organization.getId(), null, null, SourceControlProvider.GITLAB);
    refresh();

    // then the token at the root is 'hidden' by the provider at the suborg. Token is required
    SourceControlEditorPage.tokenInheritRadio().shouldBe(visible, disabled);
    SourceControlEditorPage.tokenInheritRadio().shouldHave(text("Inherit (Not Configured)"));
    SourceControlEditorPage.tokenOverrideRadio().shouldBe(visible, enabled, selected);
    SourceControlEditorPage.token().shouldBe(visible, enabled);
    SourceControlEditorPage.providerOverrideRadio().shouldBe(visible, enabled);
    SourceControlEditorPage.providerInheritRadio().shouldBe(visible, selected, enabled);
    SourceControlEditorPage.providerInheritRadio()
        .shouldHave(text("Inherit from " + organization.getName() + " (GitLab)"));

    // update is disabled because no fields have been updated
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("There are no changes to update.");

    // when we set a repository URL
    SourceControlEditorPage.repositoryUrl().setValue(REPOSITORY_URL);

    // then save is still disabled because we are missing some required values
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.saveButton().hover();
    // need to introduce a delay?
    //assertToolTip("Unable to update: fields with invalid or missing data.");

    // when we set the token
    SourceControlEditorPage.token().click();
    SourceControlEditorPage.token().setValue(TOKEN);

    // then the save button should be enabled
    SourceControlEditorPage.saveButton().shouldBe(enabled);

    // when we save the current changes
    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    // then the advanced should be hidden as all required values have been entered
    SourceControlEditorPage.advancedSettings().shouldNotBe(visible);
    SourceControlEditorPage.advancedSettingsTree().click();
    SourceControlEditorPage.advancedSettings().shouldBe(visible);

    // and the forms values are set correctly
    SourceControlEditorPage.tokenOverrideRadio().shouldBe(selected);
    SourceControlEditorPage.tokenInheritRadio().shouldNotBe(selected, enabled);
    SourceControlEditorPage.repositoryUrl().shouldHave(value(REPOSITORY_URL));
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));
    SourceControlEditorPage.providerInheritRadio().shouldBe(selected);

    // when we switch to bitbucket as a provider
    SourceControlEditorPage.credentialsUsername().shouldNotBe(visible);
    SourceControlEditorPage.providerOverrideRadio().click();
    SourceControlEditorPage.provider().chooseOption(new Option(1, "bitbucket"));

    // then we should see the username/token credentials input fields
    SourceControlEditorPage.credentialsInheritRadio().shouldNotBe(visible);
    SourceControlEditorPage.credentialsOverrideRadio().shouldNotBe(visible);
    SourceControlEditorPage.credentialsUsername().shouldBe(visible, enabled);

    // and the page should block updates until the username is provided
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("Unable to update: fields with invalid or missing data.");

    // when we provide a username
    SourceControlEditorPage.credentialsUsername().setValue("myuser");

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
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.saveButton().hover();
    //assertToolTip("Unable to update: fields with invalid or missing data.");

    // when we set a repository URL
    SourceControlEditorPage.repositoryUrl().setValue(REPOSITORY_URL);

    // then save is enabled
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), enabled);

    // when we override the provider
    SourceControlEditorPage.advancedSettingsTree().click();
    SourceControlEditorPage.providerOverrideRadio().click();
    SourceControlEditorPage.provider().chooseOption(new Option(2, "github"));

    // then the token is required
    SourceControlEditorPage.saveButton().hover();
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    assertToolTip("Unable to update: fields with invalid or missing data.");

    // when we provide the token
    SourceControlEditorPage.token().setValue("my token value");

    // then save is enabled
    SourceControlEditorPage.saveButton().shouldBe(enabled);
  }

  @Override
  void verifyStartNoSourceControl() {
    SourceControlEditorPage.root().shouldBe(visible);
    SourceControlEditorPage.title().shouldHave(text("Source Control Configuration"));
    SourceControlEditorPage.subTitle().shouldHave(text(String
        .format("Configures the integration with an external SCM for the %s application", application.getName())));

    SourceControlEditorPage.repositoryUrlControls().shouldBe(visible);

    SourceControlEditorPage.provider().shouldBe(visible);
    SourceControlEditorPage.token().shouldBe(visible, disabled);

    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.deleteButton().shouldBe(disabled);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("There are no changes to update.");
    SourceControlEditorPage.testConfigButton().shouldBe(visible, enabled);

    SourceControlEditorPage.tokenInheritRadio().label().shouldHave(text("Inherit (Not Configured)"));
    SourceControlEditorPage.tokenInheritRadio().shouldBe(visible, disabled);
    SourceControlEditorPage.tokenOverrideRadio().shouldBe(selected);
    SourceControlEditorPage.tokenOverrideRadio().label().shouldHave(text("Override"));
    SourceControlEditorPage.tokenOverrideRadio().shouldBe(visible, disabled);
    SourceControlEditorPage.tokenWarning().shouldBe(visible);

    SourceControlEditorPage.providerWarning().shouldBe(visible);
    SourceControlEditorPage.repositoryUrl().shouldHave(text(""));
    SourceControlEditorPage.repositoryUrl().shouldBe(visible, disabled);
    SourceControlEditorPage.advancedSettingsTree().shouldBe(visible);
    SourceControlEditorPage.advancedSettings().shouldBe(visible);

    SourceControlEditorPage.sshEnabledToggle().shouldNotExist();
    SourceControlEditorPage.sshEnabledDisableRadio().shouldBe(visible, disabled);
    SourceControlEditorPage.sshEnabledEnableRadio().shouldBe(visible, disabled);
    SourceControlEditorPage.sshEnabledInheritRadio().shouldBe(visible, disabled, selected);
    SourceControlEditorPage.sshEnabledInheritRadio().shouldHave(text("Inherit (Not Configured)"));

    SourceControlEditorPage.remediationPullRequestsToggle().shouldNotExist();
    SourceControlEditorPage.remediationPullRequestsDisableRadio().shouldBe(visible, disabled, selected);
    SourceControlEditorPage.remediationPullRequestsEnableRadio().shouldBe(visible, disabled);
    SourceControlEditorPage.remediationPullRequestsInheritRadio().shouldBe(visible, disabled);
    SourceControlEditorPage.remediationPullRequestsInheritRadio().shouldHave(text("Inherit (Not Configured)"));
    SourceControlEditorPage.remediationPullRequestNotSupportedAlert().shouldNotBe(visible);

    SourceControlEditorPage.pullRequestCommentingToggle().shouldNotExist();
    SourceControlEditorPage.pullRequestCommentingDisableRadio().shouldBe(visible, disabled);
    SourceControlEditorPage.pullRequestCommentingEnableRadio().shouldBe(visible, disabled, selected);
    SourceControlEditorPage.pullRequestCommentingInheritRadio().shouldBe(visible, disabled);
    SourceControlEditorPage.pullRequestCommentingInheritRadio().shouldHave(text("Inherit (Not Configured)"));
    SourceControlEditorPage.pullRequestCommentingNotSupportedAlert().shouldNotBe(visible);

    SourceControlEditorPage.sourceControlEvaluationsToggle().shouldNotExist();
    SourceControlEditorPage.sourceControlEvaluationsDisableRadio().shouldBe(visible, disabled);
    SourceControlEditorPage.sourceControlEvaluationsEnableRadio().shouldBe(visible, disabled, selected);
    SourceControlEditorPage.sourceControlEvaluationsInheritRadio().shouldBe(visible, disabled);
    SourceControlEditorPage.sourceControlEvaluationsInheritRadio().shouldHave(text("Inherit (Not Configured)"));
    SourceControlEditorPage.sourceControlEvaluationsNotSupportedAlert().shouldNotBe(visible);

    SourceControlEditorPage.baseBranchInheritRadio().shouldBe(visible, disabled, selected);
    SourceControlEditorPage.baseBranchInheritRadio().shouldHave(text("Inherit (Not Configured)"));
    SourceControlEditorPage.baseBranchOverrideRadio().shouldBe(visible, disabled);
    SourceControlEditorPage.baseBranchOverrideRadio().shouldHave(text("Override"));
    SourceControlEditorPage.baseBranchInput().shouldBe(visible, disabled);
    SourceControlEditorPage.baseBranchInput().shouldHave(value(""));
    SourceControlEditorPage.defaultBranchNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.advancedSectionRule().shouldBe(visible);
  }

  @Override
  void verifyStartWithSourceControl() {
    verifyStartWithSourceControl(false);
  }

  private void verifyStartWithSourceControlInherited() {
    verifyStartWithSourceControl(true);
  }

  private void verifyStartWithSourceControl(boolean inherited) {
    SourceControlEditorPage.root().shouldBe(visible);
    SourceControlEditorPage.title().shouldHave(text("Source Control Configuration"));
    SourceControlEditorPage.subTitle().shouldHave(text(String
        .format("Configures the integration with an external SCM for the %s application", application.getName())));
    SourceControlEditorPage.repositoryUrlControls().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.deleteButton().shouldBe(inherited ? disabled : enabled);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("There are no changes to update.");
    SourceControlEditorPage.testConfigButton().shouldBe(visible);
    SourceControlEditorPage.providerWarning().shouldNotBe(visible);
    SourceControlEditorPage.advancedSettingsTree().shouldBe(visible);
    SourceControlEditorPage.repositoryUrl().shouldBe(visible, enabled);
    SourceControlEditorPage.defaultBranchNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.remediationPullRequestNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.pullRequestCommentingNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.sourceControlEvaluationsNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.advancedSectionRule().shouldNotBe(visible);
  }

  private void addSourceControlPullRequestResults() {
    SourceControlPullRequestMetrics metrics =
        testCLMServer.getCLMServer().getInstance(SourceControlPullRequestMetrics.class);
    PullRequestResult success = new PullRequestResult();
    success.setSuccessful(true);
    metrics.addResult(application.getId(),
        new EnhancedPullRequestResult(success, new Date(), ComponentIdentifier
            .createMavenCoordinates("foo", "bar", "1.0"), "Bump bar to 1.1", false));
    PullRequestResult failure = new PullRequestResult();
    failure.setSuccessful(false);
    metrics.addResult(application.getId(),
        new EnhancedPullRequestResult(failure, new Date(), ComponentIdentifier
            .createMavenCoordinates("foo", "bar", "1.1"), "Bump bar to 1.2", true));
  }
}
