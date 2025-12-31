/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.elements.DeleteModal;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.NxFormSelect.Option;
import com.sonatype.clm.testing.functional.pages.SourceControlEditorPage;
import com.sonatype.clm.testing.functional.utils.FormUtils;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.codeborne.selenide.Selenide;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
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
    SourceControlEditorPage.saveButton().shouldNot(visible);

    SourceControlEditorPage.providerSelect().chooseOption(new Option(3, "Github"));

    SourceControlEditorPage.root().shouldBe(visible);
    SourceControlEditorPage.providerSelect().shouldBe(visible, enabled);
    SourceControlEditorPage.token().shouldBe(visible, enabled);
    SourceControlEditorPage.saveButton().shouldNotBe(visible);

    assertSourceControlDoesNotExist(ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testSourceControlEditor_create() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartNoSourceControl();

    SourceControlEditorPage.providerSelect().chooseOption(new Option(3, "Github"));

    SourceControlEditorPage.token().shouldBe(enabled).setValue("secret_key");

    SourceControlEditorPage.pullRequestCommentingFieldset().toggle().shouldNotBe(disabled).shouldBe(selected);

    SourceControlEditorPage.remediationPullRequestsFieldset().toggle().shouldNotBe(disabled, selected);

    SourceControlEditorPage.sourceControlEvaluationsFieldset().toggle().shouldNotBe(disabled).shouldBe(selected);

    SourceControlEditorPage.automatedCommitFeedbackFieldset().toggle().shouldNotBe(disabled).shouldBe(selected);

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.resetButton().shouldBe(enabled);
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));

    assertSourceControl(ROOT_ORGANIZATION_ID, null, "secret_key", GITHUB);
  }

  @Test
  public void testSourceControlEditor_update() {
    tempEntity
        .newSourceControl(ROOT_ORGANIZATION_ID, GITHUB, TOKEN, null, "master", PR_COMMENTING_ON, REMEDIATION_PR_ON,
            SOURCE_EVALS_ON, STATUS_UPDATES_ON);

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartWithSourceControl();

    SourceControlEditorPage.providerSelect().chooseOption(new Option(4, "GitLab"));

    SourceControlEditorPage.token().shouldBe(enabled);

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.resetButton().shouldBe(enabled);
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));
    assertSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.GITLAB);
  }

  @Test
  public void testSourceControlEditor_delete() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, GITHUB);

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartWithSourceControl();

    SourceControlEditorPage.resetButton().click();

    DeleteModal.root().shouldBe(visible);
    DeleteModal.header().shouldHave(text("Reset Source Control"));
    DeleteModal.body().shouldHave(text("You are about to reset the Source Control configuration for " +
        "Root Organization. This action cannot be undone."));

    DeleteModal.continueButton().click();
    FormMask.seeAndWaitForDismissal();
    DeleteModal.root().shouldBe(hidden);

    SourceControlEditorPage.providerSelect().shouldBe(visible, enabled);
    SourceControlEditorPage.token().shouldBe(visible, disabled);
    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Create"));
    SourceControlEditorPage.resetButton().shouldBe(disabled);
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
    FormUtils.getAlertElement(SourceControlEditorPage.root()).shouldBe(visible)
        .shouldHave(text(FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX + " There are no changes to save."));

    SourceControlEditorPage.baseBranchInput().setValue("develop");

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.resetButton().shouldBe(visible);
    SourceControlEditorPage.baseBranchInput().shouldHave(value("develop"));
  }

  @Test
  public void testSourceControlEditor_azureShowCredentials() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, GITHUB, true, true, "master");

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartWithSourceControl();

    // when we select Azure as a provider
    SourceControlEditorPage.providerSelect().chooseOption(new Option(1, "Azure DevOps"));

    // then credentials are shown
    SourceControlEditorPage.credentialsFieldset().labels().get(0).shouldNotBe(visible);
    SourceControlEditorPage.username().shouldBe(visible);
    SourceControlEditorPage.token().shouldBe(visible);

    // when we set username and token
    SourceControlEditorPage.username().setValue("myusername");
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));
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
    SourceControlEditorPage.providerSelect().chooseOption(new Option(2, "Bitbucket"));

    // Click out to avoid showing the open select on the visual check
    SourceControlEditorPage.title().click();
    eyesWatcher.eyesCheck("Source Control Editor Bitbucket Default State");

    SourceControlEditorPage.credentialsFieldset().labels().get(0).shouldNotBe(visible);
    SourceControlEditorPage.username().shouldBe(visible);
    SourceControlEditorPage.token().shouldBe(visible);

    SourceControlEditorPage.username().setValue("myusername");

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.resetButton().shouldBe(enabled);
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));
    assertSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.BITBUCKET);
  }

  @Test
  public void testSourceControlEditor_bitbucketRequiresCredentials() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, GITHUB, true, true, "master");

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartWithSourceControl();
    SourceControlEditorPage.providerSelect().chooseOption(new Option(2, "Bitbucket"));

    SourceControlEditorPage.credentialsFieldset().labels().get(0).shouldNotBe(visible);
    SourceControlEditorPage.username().shouldBe(visible);
    SourceControlEditorPage.token().shouldBe(visible);

    // clear token and username, submit is enabled
    SourceControlEditorPage.token().setValue("");

    // fill username only
    SourceControlEditorPage.username().setValue("myusername");

    // fill username & token
    SourceControlEditorPage.token().setValue(TOKEN);
  }

  @Test
  public void testSourceControlEditor_LicensingAwareNotificationsAndSourceControlOnly() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));
    setFeatures(LicensedFeature.NOTIFICATIONS, LicensedFeature.SOURCE_CONTROL);
    refresh();

    rootOrgVerifyNotificationFeaturesOnly();

    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.GITLAB, true, true, "master");

    refresh();

    rootOrgVerifyNotificationFeaturesOnly();
  }

  @Test
  public void testSourceControlEditor_LicensingAwareNoLicense() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    refresh();

    SourceControlEditorPage.root().shouldBe(visible);
    SourceControlEditorPage.title().shouldHave(text("Source Control Configuration"));
    SourceControlEditorPage.subTitle().shouldHave(text(String
        .format("Configures the integration with an external SCM for %s", organization.getName())));
    SourceControlEditorPage.form().shouldNotBe(visible);
    SourceControlEditorPage.notSupported().shouldBe(visible);
    SourceControlEditorPage.notSupported().shouldHave(text("Source Control is not supported by your license"));
  }

  @Test
  public void testSourceControlEditor_manualPullRequests() {
    refresh();
    Selenide.sleep(1000);

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartNoSourceControl();
    SourceControlEditorPage.manualPullRequestsFieldset().shouldBe(visible);
    SourceControlEditorPage.manualPullRequestsFieldset().toggle().shouldBe(disabled).shouldBe(selected);
    SourceControlEditorPage.manualPullRequestsFieldset().labels().forEach(label -> label.shouldNotBe(visible));

    SourceControlEditorPage.providerSelect().chooseOption(new Option(3, "Github"));
    SourceControlEditorPage.token().shouldBe(enabled).setValue("secret_key");

    SourceControlEditorPage.manualPullRequestsFieldset().toggleControl().shouldBe(enabled).click();

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();
    assertSourceControlManualPullRequest(organization.getId(), false);

    //enable manual pull requests
    SourceControlEditorPage.manualPullRequestsFieldset().toggleControl().shouldBe(enabled).click();

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.manualPullRequestsFieldset().toggle().shouldBe(selected);
    assertSourceControlManualPullRequest(organization.getId(), true);
  }

  @Test
  public void testSourceControlEditor_innerSourceAutomatedUpdates() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartNoSourceControl();
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().shouldBe(visible);
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().toggle().shouldBe(disabled)
        .shouldBe(selected);
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().labels().forEach(label -> label.shouldNotBe(visible));

    tempEntity
        .newSourceControl(ROOT_ORGANIZATION_ID, GITHUB, TOKEN, null, "master", PR_COMMENTING_ON, REMEDIATION_PR_ON,
            SOURCE_EVALS_ON, STATUS_UPDATES_ON);
    refresh();
    verifyStartWithSourceControl();

    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().shouldBe(visible);
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().toggle().shouldBe(enabled)
        .shouldBe(selected);
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().labels().forEach(label -> label.shouldNotBe(visible));
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().toggleControl().shouldBe(enabled).click();

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();
    assertSourceControlInnerSourceAutomatedUpdates(organization.getId(), false);

    // enable InnerSource automated updates
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().toggleControl().shouldBe(enabled).click();

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().toggle().shouldBe(selected);
    assertSourceControlInnerSourceAutomatedUpdates(organization.getId(), true);
  }

  @Override
  protected void verifyStartNoSourceControl() {
    SourceControlEditorPage.root().shouldBe(visible);
    SourceControlEditorPage.title().shouldHave(text("Source Control Configuration"));
    SourceControlEditorPage.subTitle().shouldHave(
        text(String.format("Configures the integration with an external SCM for %s", rootOrganization.getName())));
    SourceControlEditorPage.providerSelect().shouldBe(visible, enabled);
    SourceControlEditorPage.providerSelect().chooseOption(new Option(0, "-- Not Configured --"));
    SourceControlEditorPage.token().shouldBe(visible, disabled);
    SourceControlEditorPage.repositoryUrlControls().shouldNotBe(visible);
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldNotBe(visible);
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldNotBe(visible);
    SourceControlEditorPage.tokenWarning().shouldNotBe(visible);
    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Create"));
    SourceControlEditorPage.resetButton().shouldBe(disabled);
    SourceControlEditorPage.saveButton().click();
    FormUtils.getAlertElement(SourceControlEditorPage.root()).shouldBe(visible)
        .shouldHave(text(
            FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX + " There are no changes to save."));
    SourceControlEditorPage.repositoryUrlControls().shouldNotBe(visible);

    SourceControlEditorPage.sshEnabledFieldset().shouldBe(visible);
    SourceControlEditorPage.sshEnabledFieldset().toggle().shouldBe(disabled).shouldNotBe(selected);
    SourceControlEditorPage.sshEnabledFieldset().labels().forEach(label -> label.shouldNotBe(visible));

    SourceControlEditorPage.remediationPullRequestsFieldset().shouldBe(visible);
    SourceControlEditorPage.remediationPullRequestsFieldset().toggle().shouldBe(disabled)
        .shouldNotBe(selected);
    SourceControlEditorPage.remediationPullRequestNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.remediationPullRequestsFieldset().labels().forEach(label -> label.shouldNotBe(visible));

    SourceControlEditorPage.pullRequestCommentingFieldset().shouldBe(visible);
    SourceControlEditorPage.pullRequestCommentingFieldset().toggle().shouldBe(disabled, selected);
    SourceControlEditorPage.pullRequestCommentingNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.pullRequestCommentingFieldset().labels().forEach(label -> label.shouldNotBe(visible));

    SourceControlEditorPage.sourceControlEvaluationsFieldset().shouldBe(visible);
    SourceControlEditorPage.sourceControlEvaluationsFieldset().toggle().shouldBe(disabled, selected);
    SourceControlEditorPage.sourceControlEvaluationsNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.sourceControlEvaluationsFieldset().labels().forEach(label -> label.shouldNotBe(visible));

    SourceControlEditorPage.automatedCommitFeedbackFieldset().shouldBe(visible);
    SourceControlEditorPage.automatedCommitFeedbackFieldset().toggle().shouldBe(disabled, selected);
    SourceControlEditorPage.sourceControlEvaluationsNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.automatedCommitFeedbackFieldset().labels().forEach(label -> label.shouldNotBe(visible));

    SourceControlEditorPage.baseBranchFieldset().labels().forEach(label -> label.shouldNotBe(visible));
    SourceControlEditorPage.baseBranchInput().shouldBe(visible, disabled);
    SourceControlEditorPage.baseBranchInput().shouldHave(value("main"));
    SourceControlEditorPage.defaultBranchNotSupportedAlert().shouldNotBe(visible);

    SourceControlEditorPage.manualPullRequestsFieldset().shouldBe(visible);
    SourceControlEditorPage.manualPullRequestsFieldset().toggle().shouldBe(disabled, selected);
    SourceControlEditorPage.sourceControlEvaluationsNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.manualPullRequestsFieldset().labels().forEach(label -> label.shouldNotBe(visible));

    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().shouldBe(visible);
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().toggle().shouldBe(disabled, selected);
    SourceControlEditorPage.sourceControlEvaluationsNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().labels().forEach(label -> label.shouldNotBe(visible));
  }

  @Override
  protected void verifyStartWithSourceControl() {
    SourceControlEditorPage.root().shouldBe(visible);
    SourceControlEditorPage.title().shouldHave(text("Source Control Configuration"));
    SourceControlEditorPage.subTitle().shouldHave(
        text(String.format("Configures the integration with an external SCM for %s", rootOrganization.getName())));
    SourceControlEditorPage.providerSelect().shouldBe(visible, enabled);
    SourceControlEditorPage.providerSelect().shouldHave(text("GitHub"));
    SourceControlEditorPage.token().shouldBe(visible, enabled);
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));
    SourceControlEditorPage.repositoryUrlControls().shouldNotBe(visible);
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldNotBe(visible);
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldNotBe(visible);
    SourceControlEditorPage.tokenWarning().shouldNotBe(visible);
    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.resetButton().shouldBe(enabled);
    SourceControlEditorPage.saveButton().click();
    FormUtils.getAlertElement(SourceControlEditorPage.root()).shouldBe(visible)
        .shouldHave(text(FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX + " There are no changes to save."));
    SourceControlEditorPage.repositoryUrlControls().shouldNotBe(visible);
    SourceControlEditorPage.defaultBranchNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.remediationPullRequestNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.pullRequestCommentingNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.sourceControlEvaluationsNotSupportedAlert().shouldNotBe(visible);
  }

  private void deleteRootOrgSourceControl() {
    final SourceControl sourceControl = sourceControlDAO.getByOwnerId(ROOT_ORGANIZATION_ID);
    sourceControlDAO.delete(sourceControl);
  }
}
