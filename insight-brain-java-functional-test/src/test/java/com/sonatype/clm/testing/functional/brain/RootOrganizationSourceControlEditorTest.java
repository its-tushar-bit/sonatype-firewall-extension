/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.elements.DeleteModal;
import com.sonatype.clm.testing.functional.elements.Dropdown.Option;
import com.sonatype.clm.testing.functional.elements.ErrorBox;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.pages.SourceControlEditorPage;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.matchesText;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControl.FAKE_SECRET_KEY;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;

public class RootOrganizationSourceControlEditorTest
    extends AbstractSourceControlEditorTest
{
  private static final boolean PR_COMMENTING_ON = true;

  private static final boolean REMEDIATION_PR_ON = true;

  private static final boolean SOURCE_EVALS_ON = true;

  private static final boolean STATUS_UPDATES_ON = true;

  @Override
  @Before
  public void init() {
    super.init();
    organization = rootOrganization;
  }

  @Test
  public void testSourceControlEditor() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartNoSourceControl();
    SourceControlEditorPage.saveButton().shouldHave(text("Create"), DISABLED);

    eyesWatcher.eyesCheck("Source Control Editor Root Default State");

    SourceControlEditorPage.provider().chooseOption(new Option(2, "github"));

    SourceControlEditorPage.root().shouldBe(visible);
    SourceControlEditorPage.provider().shouldBe(visible, enabled);
    SourceControlEditorPage.token().shouldBe(visible, enabled);
    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Create"));
    SourceControlEditorPage.saveButton().shouldNotHave(DISABLED);

    eyesWatcher.eyesCheck("Source Control Editor Root Controls Enabled");

    assertSourceControlDoesNotExist(ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testSourceControlEditor_create() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartNoSourceControl();

    SourceControlEditorPage.provider().chooseOption(new Option(2, "github"));

    SourceControlEditorPage.token().shouldBe(enabled);

    SourceControlEditorPage.pullRequestCommentingToggle().shouldNotBeDisabled();
    SourceControlEditorPage.pullRequestCommentingToggle().shouldBeOn();

    SourceControlEditorPage.remediationPullRequestsToggle().shouldNotBeDisabled();
    SourceControlEditorPage.remediationPullRequestsToggle().shouldBeOff();

    SourceControlEditorPage.sourceControlEvaluationsToggle().shouldNotBeDisabled();
    SourceControlEditorPage.sourceControlEvaluationsToggle().shouldBeOn();

    SourceControlEditorPage.saveButton().shouldNotHave(DISABLED);

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.deleteButton().shouldBe(enabled);
    SourceControlEditorPage.token().shouldHave(value(""));

    assertSourceControl(ROOT_ORGANIZATION_ID, null, null, GITHUB);
  }

  @Test
  public void testSourceControlEditor_createFailure() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    SourceControlEditorPage.provider().chooseOption(new Option(2, "GitHub"));

    SourceControlEditorPage.token().shouldBe(enabled);

    SourceControlEditorPage.remediationPullRequestsToggle().shouldBeOff();
    SourceControlEditorPage.pullRequestCommentingToggle().shouldBeOn();
    SourceControlEditorPage.sourceControlEvaluationsToggle().shouldBeOn();

    SourceControlEditorPage.saveButton().shouldNotHave(DISABLED);

    //Create an entry to create error condition
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.GITLAB);
    SourceControlEditorPage.saveButton().click();

    final ErrorBox errorBox = PAGE.error();
    errorBox.retryButton().shouldBe(visible, enabled);
    errorBox.shouldHave(text("SourceControl already exists for organization with id: ROOT_ORGANIZATION_ID"));
    assertSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.GITLAB);

    //Delete the entry to resolve error condition
    deleteRootOrgSourceControl();

    errorBox.retryButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.deleteButton().shouldBe(enabled);
    SourceControlEditorPage.token().shouldHave(value(""));
    assertSourceControl(ROOT_ORGANIZATION_ID, null, null, GITHUB);
  }

  @Test
  public void testSourceControlEditor_update() {
    tempEntity
        .newSourceControl(ROOT_ORGANIZATION_ID, GITHUB, TOKEN, null, "master", PR_COMMENTING_ON, REMEDIATION_PR_ON,
            SOURCE_EVALS_ON, STATUS_UPDATES_ON);

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartWithSourceControl();

    SourceControlEditorPage.provider().chooseOption(new Option(3, "gitlab"));

    SourceControlEditorPage.token().shouldBe(enabled);
    SourceControlEditorPage.saveButton().shouldNotHave(DISABLED);

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.deleteButton().shouldBe(enabled);
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));
    assertSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.GITLAB);
  }

  @Test
  public void testSourceControlEditor_updateFailure() {
    tempEntity
        .newSourceControl(ROOT_ORGANIZATION_ID, GITHUB, TOKEN, null, "master", PR_COMMENTING_ON, REMEDIATION_PR_ON,
            SOURCE_EVALS_ON, STATUS_UPDATES_ON);

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartWithSourceControl();
    SourceControlEditorPage.provider().chooseOption(new Option(3, "GitLab"));
    SourceControlEditorPage.token().shouldBe(enabled);
    SourceControlEditorPage.pullRequestCommentingToggle().shouldExist();
    SourceControlEditorPage.remediationPullRequestsToggle().shouldExist();
    SourceControlEditorPage.sourceControlEvaluationsToggle().shouldExist();
    SourceControlEditorPage.saveButton().shouldNotHave(DISABLED);

    //Delete the entry to create the failure
    deleteRootOrgSourceControl();
    SourceControlEditorPage.saveButton().click();

    final ErrorBox errorBox = PAGE.error();
    errorBox.retryButton().shouldBe(visible, enabled);
    errorBox.shouldHave(text("Cannot find SourceControl for organization with id: ROOT_ORGANIZATION_ID"));
    assertSourceControlDoesNotExist(ROOT_ORGANIZATION_ID);

    //Create the entry to resolve error condition
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, GITHUB);

    errorBox.retryButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.deleteButton().shouldBe(enabled);
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));
    assertSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.GITLAB);
  }

  @Test
  public void testSourceControlEditor_delete() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, GITHUB);

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartWithSourceControl();

    SourceControlEditorPage.deleteButton().click();

    eyesWatcher.eyesCheck("Source Control Editor Delete Modal");

    DeleteModal.root().shouldBe(visible);
    DeleteModal.header().shouldHave(text("Reset Source Control"));
    DeleteModal.body().shouldHave(text("You are about to reset the Source Control configuration for organization " +
        "Root Organization. This action cannot be undone."));

    DeleteModal.continueButton().click();
    FormMask.seeAndWaitForDismissal();
    DeleteModal.root().shouldBe(hidden);

    SourceControlEditorPage.provider().shouldBe(visible, enabled);
    SourceControlEditorPage.token().shouldBe(visible, disabled);
    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Create"), DISABLED);
    SourceControlEditorPage.deleteButton().shouldBe(disabled);
    assertSourceControlDoesNotExist(ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testSourceControlEditor_deleteFailure() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, GITHUB);

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartWithSourceControl();
    SourceControlEditorPage.deleteButton().click();

    DeleteModal.root().shouldBe(visible);
    DeleteModal.header().shouldHave(text("Reset Source Control"));
    DeleteModal.body().shouldHave(text("You are about to reset the Source Control configuration for organization Root" +
        " Organization. This action cannot be undone."));

    //Delete entry to create error condition
    deleteRootOrgSourceControl();

    DeleteModal.continueButton().click();

    DeleteModal.error().shouldHave(text("Cannot find SourceControl for organization with id: ROOT_ORGANIZATION_ID"));
    DeleteModal.retryButton().shouldBe(visible, enabled);

    //Recreate the entry to resolve error condition
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, GITHUB);

    DeleteModal.retryButton().click();

    FormMask.seeAndWaitForDismissal();
    DeleteModal.root().shouldBe(hidden);

    SourceControlEditorPage.provider().shouldBe(visible, enabled);
    SourceControlEditorPage.token().shouldBe(visible, disabled);
    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Create"), DISABLED);
    SourceControlEditorPage.deleteButton().shouldBe(disabled);
    assertSourceControlDoesNotExist(ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testSourceControlEditor_defaultBranch() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, GITHUB, true, true, "master");

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartWithSourceControl();
    SourceControlEditorPage.baseBranchInput().click();
    SourceControlEditorPage.baseBranchInput().shouldBe(enabled);
    SourceControlEditorPage.baseBranchInput().shouldHave(value("master"));
    SourceControlEditorPage.baseBranchInput().setValue("");
    SourceControlEditorPage.saveButton().shouldHave(DISABLED);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("Unable to update: fields with invalid or missing data.");

    SourceControlEditorPage.baseBranchInput().setValue("develop");
    SourceControlEditorPage.saveButton().shouldNotHave(DISABLED);

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.deleteButton().shouldBe(visible);
    SourceControlEditorPage.baseBranchInput().shouldHave(value("develop"));
  }

  @Test
  public void testSourceControlEditor_azureShowCredentials() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, GITHUB, true, true, "master");

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartWithSourceControl();

    // when we select Azure as a provider
    SourceControlEditorPage.provider().chooseOption(new Option(0, "Azure DevOps"));
    eyesWatcher.eyesCheck("Source Control Editor Azure Default State");

    // then credentials are shown
    SourceControlEditorPage.credentialsInheritRadio().shouldNotBe(visible);
    SourceControlEditorPage.credentialsUsername().shouldBe(visible);
    SourceControlEditorPage.credentialsToken().shouldBe(visible);
    SourceControlEditorPage.token().shouldNotBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(DISABLED);

    // when we set username and token
    SourceControlEditorPage.credentialsUsername().setValue("myusername");
    SourceControlEditorPage.credentialsToken().shouldHave(value(FAKE_SECRET_KEY));
    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    // then the SC record should be Azure
    assertSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.AZURE);
  }

  @Test
  public void testSourceControlEditor_bitbucketShowCredentialsFeature() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, GITHUB, true, true, "master");

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartWithSourceControl();
    SourceControlEditorPage.provider().chooseOption(new Option(1, "Bitbucket"));

    eyesWatcher.eyesCheck("Source Control Editor Bitbucket Default State");

    SourceControlEditorPage.credentialsInheritRadio().shouldNotBe(visible);
    SourceControlEditorPage.credentialsUsername().shouldBe(visible);
    SourceControlEditorPage.credentialsToken().shouldBe(visible);
    SourceControlEditorPage.token().shouldNotBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(DISABLED);

    SourceControlEditorPage.credentialsUsername().setValue("myusername");
    SourceControlEditorPage.saveButton().shouldNotHave(DISABLED);

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.deleteButton().shouldBe(enabled);
    SourceControlEditorPage.credentialsToken().shouldHave(value(FAKE_SECRET_KEY));
    assertSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.BITBUCKET);
  }

  @Test
  public void testSourceControlEditor_bitbucketRequiresCredentials() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, GITHUB, true, true, "master");

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartWithSourceControl();
    SourceControlEditorPage.provider().chooseOption(new Option(1, "Bitbucket"));

    SourceControlEditorPage.credentialsInheritRadio().shouldNotBe(visible);
    SourceControlEditorPage.credentialsUsername().shouldBe(visible);
    SourceControlEditorPage.credentialsToken().shouldBe(visible);
    SourceControlEditorPage.token().shouldNotBe(visible);

    // do NOT set the value in username, leave token filled from previous setting
    SourceControlEditorPage.saveButton().shouldHave(DISABLED);

    // clear token and username, submit is enabled
    SourceControlEditorPage.credentialsToken().setValue("");
    SourceControlEditorPage.saveButton().shouldNotHave(DISABLED);

    // fill username only
    SourceControlEditorPage.credentialsUsername().setValue("myusername");
    SourceControlEditorPage.saveButton().shouldHave(DISABLED);

    // fill username & token
    SourceControlEditorPage.credentialsToken().setValue(TOKEN);
    SourceControlEditorPage.saveButton().shouldNotHave(DISABLED);
  }

  @Test
  public void testSourceControlEditor_LicensingAwareNotificationOnly() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));
    setLicensedProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    refresh();

    verifyNotificationFeaturesOnly();

    SourceControlEditorPage.saveButton().shouldHave(DISABLED);

    eyesWatcher.eyesCheck("Source Control Editor - root org configurations disabled, no license");

    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.GITLAB, true, true, "master");

    refresh();

    verifyNotificationFeaturesOnly();
  }

  @Test
  public void testSourceControlEditor_LicensingAwareNoLicense() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    refresh();

    SourceControlEditorPage.root().shouldBe(visible);
    SourceControlEditorPage.title().shouldHave(text("Source Control Configuration"));
    SourceControlEditorPage.subTitle().shouldHave(text(String
        .format("Configures the integration with an external SCM for the %s", organization.getName())));
    SourceControlEditorPage.form().shouldNotBe(visible);
    SourceControlEditorPage.notSupported().shouldBe(visible);
    SourceControlEditorPage.notSupported().shouldHave(text("Source Control is not supported by your license"));
    eyesWatcher.eyesCheck("Source Control Editor - No License");
  }

  @Override
  void verifyStartNoSourceControl() {
    SourceControlEditorPage.root().shouldBe(visible);
    SourceControlEditorPage.title().shouldHave(text("Source Control Configuration"));
    SourceControlEditorPage.subTitle().shouldHave(
        text(String.format("Configures the integration with an external SCM for the %s", rootOrganization.getName())));
    SourceControlEditorPage.provider().shouldBe(visible, enabled);
    SourceControlEditorPage.provider().shouldHave(matchesText("-- Not Configured --"));
    SourceControlEditorPage.token().shouldBe(visible, disabled);
    SourceControlEditorPage.repositoryUrlControls().shouldNotBe(visible);
    SourceControlEditorPage.tokenOverrideRadio().shouldNotBe(visible);
    SourceControlEditorPage.tokenInheritRadio().shouldNotBe(visible);
    SourceControlEditorPage.tokenWarning().shouldNotBe(visible);
    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Create"), DISABLED);
    SourceControlEditorPage.deleteButton().shouldBe(disabled);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("Unable to create: fields with invalid or missing data.");
    SourceControlEditorPage.advancedSettingsTree().shouldNotBe(visible);
    SourceControlEditorPage.repositoryUrlControls().shouldNotBe(visible);
    SourceControlEditorPage.advancedSettings().shouldBe(visible);

    SourceControlEditorPage.sshEnabledToggle().shouldBeDisabled();
    SourceControlEditorPage.sshEnabledToggle().shouldBeOff();
    SourceControlEditorPage.sshEnabledDisableRadio().shouldNotBe(visible);
    SourceControlEditorPage.sshEnabledEnableRadio().shouldNotBe(visible);
    SourceControlEditorPage.sshEnabledInheritRadio().shouldNotBe(visible);

    SourceControlEditorPage.remediationPullRequestsToggle().shouldBeDisabled();
    SourceControlEditorPage.remediationPullRequestsToggle().shouldBeOff();
    SourceControlEditorPage.remediationPullRequestNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.remediationPullRequestsDisableRadio().shouldNotBe(visible);
    SourceControlEditorPage.remediationPullRequestsEnableRadio().shouldNotBe(visible);
    SourceControlEditorPage.remediationPullRequestsInheritRadio().shouldNotBe(visible);

    SourceControlEditorPage.pullRequestCommentingToggle().shouldBeDisabled();
    SourceControlEditorPage.pullRequestCommentingToggle().shouldBeOn();
    SourceControlEditorPage.pullRequestCommentingNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.pullRequestCommentingDisableRadio().shouldNotBe(visible);
    SourceControlEditorPage.pullRequestCommentingEnableRadio().shouldNotBe(visible);
    SourceControlEditorPage.pullRequestCommentingInheritRadio().shouldNotBe(visible);

    SourceControlEditorPage.sourceControlEvaluationsToggle().shouldBeDisabled();
    SourceControlEditorPage.sourceControlEvaluationsToggle().shouldBeOn();
    SourceControlEditorPage.sourceControlEvaluationsNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.sourceControlEvaluationsDisableRadio().shouldNotBe(visible);
    SourceControlEditorPage.sourceControlEvaluationsEnableRadio().shouldNotBe(visible);
    SourceControlEditorPage.sourceControlEvaluationsInheritRadio().shouldNotBe(visible);

    SourceControlEditorPage.baseBranchInheritRadio().shouldNotBe(visible);
    SourceControlEditorPage.baseBranchOverrideRadio().shouldNotBe(visible);
    SourceControlEditorPage.baseBranchInput().shouldBe(visible, disabled);
    SourceControlEditorPage.baseBranchInput().shouldHave(value("master"));
    SourceControlEditorPage.defaultBranchNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.advancedSectionRule().shouldNotBe(visible);
  }

  @Override
  void verifyStartWithSourceControl() {
    SourceControlEditorPage.root().shouldBe(visible);
    SourceControlEditorPage.title().shouldHave(text("Source Control Configuration"));
    SourceControlEditorPage.subTitle().shouldHave(
        text(String.format("Configures the integration with an external SCM for the %s", rootOrganization.getName())));
    SourceControlEditorPage.provider().shouldBe(visible, enabled);
    SourceControlEditorPage.provider().shouldHave(matchesText("GitHub"));
    SourceControlEditorPage.token().shouldBe(visible, enabled);
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));
    SourceControlEditorPage.repositoryUrlControls().shouldNotBe(visible);
    SourceControlEditorPage.tokenOverrideRadio().shouldNotBe(visible);
    SourceControlEditorPage.tokenInheritRadio().shouldNotBe(visible);
    SourceControlEditorPage.tokenWarning().shouldNotBe(visible);
    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.deleteButton().shouldBe(enabled);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("There are no changes to update.");
    SourceControlEditorPage.advancedSettingsTree().shouldNotBe(visible);
    SourceControlEditorPage.repositoryUrlControls().shouldNotBe(visible);
    SourceControlEditorPage.advancedSettings().shouldBe(visible);
    SourceControlEditorPage.defaultBranchNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.remediationPullRequestNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.pullRequestCommentingNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.sourceControlEvaluationsNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.advancedSectionRule().shouldNotBe(visible);
  }
}
