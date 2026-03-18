/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.elements.DeleteModal;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.NxFormSelect;
import com.sonatype.clm.testing.functional.elements.NxFormSelect.Option;
import com.sonatype.clm.testing.functional.pages.SourceControlEditorPage;
import com.sonatype.clm.testing.functional.utils.FormUtils;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.codeborne.selenide.Selenide;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import static com.codeborne.selenide.CollectionCondition.allMatch;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.RSC_TERTIARY_DISABLED;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControl.FAKE_SECRET_KEY;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static com.sonatype.nexus.scm.SourceControlProvider.GITLAB;

public class OrganizationSourceControlEditorTest
    extends AbstractSourceControlEditorTest
{
  @Override
  @Before
  public void init() {
    super.init();
    organization = tempEntity.newOrganization(YE_OLE_ORGANIZATION);
  }

  @Test
  public void testSourceControlEditor() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartNoSourceControl();

    assertSourceControlDoesNotExist(rootOrganization.getId());
    assertSourceControlDoesNotExist(organization.getId());
  }

  @Test
  public void testSourceControlEditor_EmptySourceControlWithProviderOnRoot() {
    assertSourceControlDoesNotExist(rootOrganization.getId());
    assertSourceControlDoesNotExist(organization.getId());

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartNoSourceControl();

    tempEntity.newSourceControl(rootOrganization.getId(), null, null, SourceControlProvider.GITHUB);

    refresh();

    assertSourceControlDoesNotExist(organization.getId());

    verifyStartWithSourceControlInherited();
    SourceControlEditorPage.token().shouldBe(visible, disabled);
    SourceControlEditorPage.credentialsFieldset().shouldBe(visible);
    SourceControlEditorPage.credentialsFieldset().labels().get(0).shouldHave(text("Inherit (Not Configured)"));
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldBe(enabled);
    SourceControlEditorPage.credentialsFieldset().labels().get(1).shouldHave(text("Override"));
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldBe(enabled).shouldNotBe(selected);
  }

  @Test
  public void testSourceControlEditor_OrganizationSourceControlExistsInheritToken() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartNoSourceControl();

    assertSourceControlDoesNotExist(rootOrganization.getId());
    assertSourceControlDoesNotExist(organization.getId());

    tempEntity.newSourceControl(rootOrganization.getId(), null, null, SourceControlProvider.GITHUB);
    tempEntity.newSourceControl(organization.getId(), null, null, null);

    refresh();

    verifyStartWithSourceControl();
    SourceControlEditorPage.token().shouldBe(visible, disabled);
    SourceControlEditorPage.credentialsFieldset().shouldBe(visible);
    SourceControlEditorPage.credentialsFieldset().labels().get(0).shouldHave(text("Inherit (Not Configured)"));
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldBe(enabled);
    SourceControlEditorPage.credentialsFieldset().labels().get(1).shouldHave(text("Override"));
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldBe(enabled).shouldNotBe(selected);
  }

  @Test
  public void testSourceControlEditor_OrganizationSourceControlExistsOverrideToken() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartNoSourceControl();

    assertSourceControlDoesNotExist(rootOrganization.getId());
    assertSourceControlDoesNotExist(organization.getId());

    tempEntity.newSourceControl(rootOrganization.getId(), null, null, SourceControlProvider.GITHUB);
    tempEntity.newSourceControl(organization.getId(), null, TOKEN, null);

    refresh();

    verifyStartWithSourceControl();
    SourceControlEditorPage.token().shouldBe(visible, enabled);
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));
    SourceControlEditorPage.credentialsFieldset().shouldBe(visible);
    SourceControlEditorPage.credentialsFieldset().labels().get(0).shouldHave(text("Inherit (Not Configured)"));
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldBe(enabled);
    SourceControlEditorPage.credentialsFieldset().labels().get(1).shouldHave(text("Override"));
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldBe(enabled).shouldBe(selected);
  }

  @Test
  public void testSourceControlEditor_updateToken() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));
    verifyStartNoSourceControl();
    tempEntity.newSourceControl(rootOrganization.getId(), null, "Root_org_token", SourceControlProvider.GITHUB);
    refresh();

    SourceControlEditorPage.credentialsFieldset().labels().get(1).click();
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldBe(selected);
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldNotBe(selected);
    SourceControlEditorPage.token().shouldBe(enabled);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.saveButton().click();
    FormUtils.getAlertElement(SourceControlEditorPage.root())
        .shouldBe(visible)
        .shouldHave(
            text(FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX + " Unable to save: fields with invalid or missing data"));

    SourceControlEditorPage.token().setValue(TOKEN);
    eyesWatcher.eyesCheck("Source Control Editor Update With Token Provided");

    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldBe(selected);
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldNotBe(selected);
    SourceControlEditorPage.saveButton().shouldBe(enabled).shouldHave(text("Update"));
    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.saveButton().click();
    FormUtils.getAlertElement(SourceControlEditorPage.root())
        .shouldBe(visible)
        .shouldHave(text(FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX + " There are no changes to save."));
    SourceControlEditorPage.resetButton().shouldBe(enabled);
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldBe(selected);
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldNotBe(selected);
  }

  @Test
  public void testSourceControlEditor_bitbucketOverrideCredentials() {
    final String rootUsername = "rootusername";

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartNoSourceControl();

    assertSourceControlDoesNotExist(rootOrganization.getId());
    assertSourceControlDoesNotExist(organization.getId());

    tempEntity
        .newSourceControl(rootOrganization.getId(), null, rootUsername, TOKEN, SourceControlProvider.BITBUCKET, true,
            false, "master", null);
    tempEntity.newSourceControl(organization.getId(), null, null, null);

    refresh();

    verifyStartWithSourceControl();
    SourceControlEditorPage.token().shouldBe(visible, disabled);
    SourceControlEditorPage.credentialsFieldset().shouldBe(visible);
    SourceControlEditorPage.credentialsFieldset()
        .labels()
        .shouldHave(texts("Inherit from Root Organization", "Override"));
    SourceControlEditorPage.credentialsFieldset().radioInputs().forEach(input -> input.shouldBe(enabled));
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldNotBe(selected);

    SourceControlEditorPage.credentialsFieldset().labels().get(1).click();
    SourceControlEditorPage.username().shouldBe(visible, enabled);
    SourceControlEditorPage.token().shouldBe(visible, enabled);
    SourceControlEditorPage.username().setValue("orgusername");
    SourceControlEditorPage.token().setValue("orgsecrettoken");

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));
  }

  @Test
  public void testSourceControlEditor_azureInherit() {
    final String rootUsername = "rootusername";

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartNoSourceControl();

    assertSourceControlDoesNotExist(rootOrganization.getId());
    assertSourceControlDoesNotExist(organization.getId());

    // given root starts with Azure
    tempEntity
        .newSourceControl(rootOrganization.getId(), null, rootUsername, TOKEN, SourceControlProvider.AZURE, true,
            false, "master", null);
    tempEntity.newSourceControl(organization.getId(), null, null, null);

    refresh();

    // then we should see the username for credentials
    verifyStartWithSourceControl();
    SourceControlEditorPage.token().shouldBe(visible, disabled);
    SourceControlEditorPage.credentialsFieldset().shouldBe(visible);
    SourceControlEditorPage.credentialsFieldset()
        .labels()
        .shouldHave(texts("Inherit from Root Organization", "Override"));
    SourceControlEditorPage.credentialsFieldset().radioInputs().forEach(input -> input.shouldBe(enabled));
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldNotBe(selected);
    SourceControlEditorPage.credentialsFieldset().labels().get(1).click();
    SourceControlEditorPage.username().setValue("orgusername");
    SourceControlEditorPage.token().setValue("orgsecrettoken");

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));
  }

  @Test
  public void testSourceControlEditor_azureOverride() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartNoSourceControl();

    assertSourceControlDoesNotExist(rootOrganization.getId());
    assertSourceControlDoesNotExist(organization.getId());

    // given root starts with non-Azure
    tempEntity
        .newSourceControl(rootOrganization.getId(), null, null, TOKEN, SourceControlProvider.GITHUB, true,
            false, "master", null);
    tempEntity.newSourceControl(organization.getId(), null, null, null);

    refresh();

    // then we should see not see the username
    verifyStartWithSourceControl();
    SourceControlEditorPage.username().shouldNotBe(visible);
    SourceControlEditorPage.token().shouldBe(visible);

    // when we switch to azure as a provider
    SourceControlEditorPage.providerFieldset().labels().get(1).click();
    SourceControlEditorPage.providerSelect().chooseOption(new Option(1, "Azure DevOps"));

    // then the credentials should be shown with no option to inherit
    SourceControlEditorPage.username().shouldBe(visible, enabled);
    SourceControlEditorPage.token().shouldBe(visible, enabled);
    SourceControlEditorPage.credentialsFieldset().labels().forEach(label -> label.shouldNotBe(visible));
  }

  @Test
  public void testSourceControlEditor_azureOverrideBitbucket() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartNoSourceControl();

    assertSourceControlDoesNotExist(rootOrganization.getId());
    assertSourceControlDoesNotExist(organization.getId());

    // given root starts with Bitbucket which has a user & token, same as Azure
    tempEntity
        .newSourceControl(rootOrganization.getId(), null, "username", TOKEN, SourceControlProvider.BITBUCKET, true,
            false, "master", null);
    tempEntity.newSourceControl(organization.getId(), null, null, null);

    refresh();

    // then we should see username & token fields
    verifyStartWithSourceControl();
    SourceControlEditorPage.username().shouldBe(visible, disabled);
    SourceControlEditorPage.token().shouldBe(visible, disabled);

    // when we switch to azure as a provider
    SourceControlEditorPage.providerFieldset().labels().get(1).click();
    SourceControlEditorPage.providerSelect().shouldBe(visible, enabled);
    SourceControlEditorPage.providerSelect().chooseOption(new Option(1, "Azure DevOps"));

    // then the credentials should be shown with no option to inherit and should be enabled
    SourceControlEditorPage.username().shouldBe(visible, enabled);
    SourceControlEditorPage.token().shouldBe(visible, enabled);
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldNotBe(visible);
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldNotBe(visible);
  }

  @Test
  public void testSourceControlEditor_defaultBranch() {

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartNoSourceControl();
    SourceControlEditorPage.baseBranchFieldset().radioInputs().forEach(input -> input.shouldBe(disabled));
    SourceControlEditorPage.baseBranchFieldset().radioInputs().get(0).shouldBe(selected);
    SourceControlEditorPage.baseBranchFieldset()
        .labels()
        .shouldHave(texts("Inherit (Not Configured)", "Override"));
    SourceControlEditorPage.baseBranchInput().shouldHave(value(""));
    SourceControlEditorPage.baseBranchInput().shouldBe(disabled);

    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.GITHUB, true, true, "master");
    refresh();

    SourceControlEditorPage.baseBranchFieldset()
        .radioInputs()
        .should(allMatch("is enabled", WebElement::isEnabled));
    SourceControlEditorPage.baseBranchFieldset().radioInputs().get(0).shouldBe(selected);
    SourceControlEditorPage.baseBranchFieldset()
        .labels()
        .shouldHave(texts(String.format("Inherit from %s", rootOrganization.getName()), "Override"));
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

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.baseBranchInput().shouldHave(value("develop"));
  }

  @Test
  public void testSourceControlEditor_delete() {
    tempEntity.newSourceControl(rootOrganization.getId(), null, null, GITHUB);
    tempEntity.newSourceControl(organization.getId(), null, null, null);

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartWithSourceControl();

    SourceControlEditorPage.resetButton().click();

    DeleteModal.root().shouldBe(visible);
    DeleteModal.header().shouldHave(text("Reset Source Control"));
    DeleteModal.body()
        .shouldHave(text("You are about to reset the Source Control configuration for " +
            "Ye Ole Organization. This action cannot be undone."));

    DeleteModal.continueButton().click();
    FormMask.seeAndWaitForDismissal();
    DeleteModal.root().shouldBe(hidden);

    SourceControlEditorPage.providerSelect().shouldBe(visible, disabled);
    SourceControlEditorPage.token().shouldBe(visible, disabled);
    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.resetButton().shouldBe(disabled);
    assertSourceControlDoesNotExist(organization.getId());
  }

  @Test
  public void testSourceControlEditor_deleteFailure() {
    tempEntity.newSourceControl(rootOrganization.getId(), null, null, GITHUB);
    tempEntity.newSourceControl(organization.getId(), null, null, null);

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartWithSourceControl();
    SourceControlEditorPage.resetButton().click();

    DeleteModal.root().shouldBe(visible);
    DeleteModal.header().shouldHave(text("Reset Source Control"));
    DeleteModal.body()
        .shouldHave(text("You are about to reset the Source Control configuration for " +
            "Ye Ole Organization. This action cannot be undone."));

    // delete entry to create error condition
    deleteSourceControl(organization.getId());

    DeleteModal.continueButton().click();

    DeleteModal.error()
        .shouldHave(text("An error occurred saving data. Cannot find SourceControl for organization with id: "
            + organization.getId()));
    DeleteModal.retryButton().shouldBe(visible, enabled);

    // recreate the entry to resolve error condition
    tempEntity.newSourceControl(organization.getId(), null, null, null);

    DeleteModal.retryButton().click();

    FormMask.seeAndWaitForDismissal();
    DeleteModal.root().shouldBe(hidden);

    SourceControlEditorPage.providerSelect().shouldBe(visible, disabled);
    SourceControlEditorPage.token().shouldBe(visible, disabled);
    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.resetButton().shouldBe(disabled);
    assertSourceControlDoesNotExist(organization.getId());
  }

  @Test
  public void testSourceControlEditor_LicensingAwareNotificationsAndSourceControlOnly() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));
    setFeatures(LicensedFeature.NOTIFICATIONS, LicensedFeature.SOURCE_CONTROL);
    refresh();

    verifyNotificationFeaturesOnly();

    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, GITLAB, true, true, "master");

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
    SourceControlEditorPage.subTitle()
        .shouldHave(text("Configures the integration with an external SCM for " + organization.getName()));
    SourceControlEditorPage.form().shouldNotBe(visible);
    SourceControlEditorPage.notSupported().shouldBe(visible);
    SourceControlEditorPage.notSupported().shouldHave(text("Source Control is not supported by your license"));
  }

  @Test
  public void testSourceControlEditor_overrideProvider() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getPublicId()));
    verifyStartNoSourceControl();

    // when start with a token & provider at root
    tempEntity.newSourceControl(rootOrganization.getId(), null, TOKEN, SourceControlProvider.GITHUB);
    refresh();

    // then elements show inherit status
    SourceControlEditorPage.credentialsFieldset().shouldBe(visible);
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldBe(selected);
    SourceControlEditorPage.credentialsFieldset()
        .labels()
        .get(0)
        .shouldHave(text("Inherit from Root Organization"));
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldBe(enabled);
    SourceControlEditorPage.token().shouldBe(visible, disabled);
    SourceControlEditorPage.providerFieldset().radioInputs().get(1).shouldBe(enabled);
    SourceControlEditorPage.providerFieldset().radioInputs().get(0).shouldBe(selected, enabled);
    SourceControlEditorPage.providerFieldset()
        .labels()
        .get(0)
        .shouldHave(text("Inherit from Root Organization"));

    // when change provider to bitbucket
    SourceControlEditorPage.username().shouldNotBe(visible);
    SourceControlEditorPage.providerFieldset().labels().get(1).click();
    SourceControlEditorPage.providerSelect().shouldBe(visible, enabled);
    SourceControlEditorPage.providerSelect().chooseOption(new NxFormSelect.Option(2, "Bitbucket"));

    // then credentials are shown with no inherit/override radio buttons
    SourceControlEditorPage.credentialsFieldset().labels().forEach(label -> label.shouldNotBe(visible));
    SourceControlEditorPage.username().shouldBe(visible, enabled);

    // when change to gitlab (no user name required)
    SourceControlEditorPage.providerSelect().chooseOption(new NxFormSelect.Option(4, "GitLab"));

    // then username is not show, and token inherit radio buttons also not shown
    SourceControlEditorPage.credentialsFieldset().labels().forEach(label -> label.shouldNotBe(visible));
    SourceControlEditorPage.username().shouldNotBe(visible);
    SourceControlEditorPage.credentialsFieldset().labels().forEach(label -> label.shouldNotBe(visible));
    SourceControlEditorPage.token().shouldBe(visible, enabled);
    SourceControlEditorPage.token().setValue(TOKEN);

    SourceControlEditorPage.saveButton().shouldBe(enabled);
    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    // then token inherit still not shown and value is set to be the fake
    SourceControlEditorPage.credentialsFieldset().labels().forEach(label -> label.shouldNotBe(visible));
    SourceControlEditorPage.token().shouldBe(visible, enabled);
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));
  }

  @Test
  public void testSourceControlEditor_manualPullRequests() {
    refresh();
    Selenide.sleep(1000);

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));
    verifyStartNoSourceControl();
    SourceControlEditorPage.manualPullRequestsFieldset().toggle().shouldNotBe(visible);
    SourceControlEditorPage.manualPullRequestsFieldset().shouldBe(visible);
    SourceControlEditorPage.manualPullRequestsFieldset()
        .radioInputs()
        .forEach(input -> input.shouldBe(disabled));
    SourceControlEditorPage.manualPullRequestsFieldset()
        .labels()
        .shouldHave(texts("Inherit (Not Configured)", "Enabled", "Disabled"));

    // root org source control with manual pull requests enabled
    tempEntity.newSourceControl(
        rootOrganization.getId(), null, null, null, TOKEN, SourceControlProvider.GITHUB, false, true, "main", null,
        true, true, null, null, true, true, null);

    // org source control
    tempEntity.newSourceControl(organization.getId(), null, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null);

    refresh();

    SourceControlEditorPage.manualPullRequestsFieldset().shouldBe(visible);
    SourceControlEditorPage.manualPullRequestsFieldset().radioInputs().get(0).shouldBe(selected);
    SourceControlEditorPage.manualPullRequestsFieldset().radioInputs().get(0).shouldBe(enabled);
    SourceControlEditorPage.manualPullRequestsFieldset()
        .labels()
        .get(0)
        .shouldHave(text("Inherit from Root Organization"));

    // override to Disabled
    SourceControlEditorPage.manualPullRequestsFieldset().labels().get(2).click();

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.manualPullRequestsFieldset().radioInputs().get(2).shouldBe(selected);
    assertSourceControlManualPullRequest(organization.getId(), false);

    // override to Enabled
    SourceControlEditorPage.manualPullRequestsFieldset().labels().get(1).click();

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.manualPullRequestsFieldset().radioInputs().get(1).shouldBe(selected);
    assertSourceControlManualPullRequest(organization.getId(), true);

    // back to inherit
    SourceControlEditorPage.manualPullRequestsFieldset().labels().get(0).click();

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.manualPullRequestsFieldset().radioInputs().get(0).shouldBe(selected);
    assertSourceControlManualPullRequest(organization.getId(), null);
  }

  @Test
  public void testSourceControlEditor_innerSourceAutomatedUpdates() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));
    verifyStartNoSourceControl();
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().toggle().shouldNotBe(visible);
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().shouldBe(visible);
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset()
        .radioInputs()
        .forEach(input -> input.shouldBe(disabled));
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset()
        .labels()
        .shouldHave(texts("Inherit (Not Configured)", "Enabled", "Disabled"));
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().radioInputs().get(1).shouldBe(selected);

    // root org source control with InnerSource automated updates enabled
    tempEntity.newSourceControl(
        rootOrganization.getId(), null, null, null, TOKEN, SourceControlProvider.GITHUB, false, true, "main", null,
        true, true, null, null, true, true, true);

    // org source control
    tempEntity.newSourceControl(organization.getId(), null, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null);

    refresh();

    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().shouldBe(visible);
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().radioInputs().get(0).shouldBe(selected);
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().radioInputs().get(0).shouldBe(enabled);
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset()
        .labels()
        .get(0)
        .shouldHave(text("Inherit from Root Organization"));

    // override to Disabled
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().labels().get(2).click();

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().radioInputs().get(2).shouldBe(selected);
    assertSourceControlInnerSourceAutomatedUpdates(organization.getId(), false);

    // override to Enabled
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().labels().get(1).click();

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().radioInputs().get(1).shouldBe(selected);
    assertSourceControlInnerSourceAutomatedUpdates(organization.getId(), true);

    // back to inherit
    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().labels().get(0).click();

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.innerSourceAutomatedUpdatesFieldset().radioInputs().get(0).shouldBe(selected);
    assertSourceControlInnerSourceAutomatedUpdates(organization.getId(), null);
  }

  @Override
  protected void verifyStartNoSourceControl() {
    System.out.println("verifyStartNoSourceControl: Organization id: " + organization.getId());
    SourceControlEditorPage.root().shouldBe(visible);
    SourceControlEditorPage.title().shouldHave(text("Source Control Configuration"));
    SourceControlEditorPage.subTitle()
        .shouldHave(text("Configures the integration with an external SCM for " + organization.getName()));
    SourceControlEditorPage.providerFieldset().radioInputs().forEach(input -> input.shouldBe(enabled));
    SourceControlEditorPage.token().shouldBe(visible);
    SourceControlEditorPage.repositoryUrlControls().shouldNotBe(visible);
    SourceControlEditorPage.tokenWarning().shouldNotBe(visible);

    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.resetButton().shouldBe(RSC_TERTIARY_DISABLED);
    SourceControlEditorPage.saveButton().click();
    FormUtils.getAlertElement(SourceControlEditorPage.root())
        .shouldBe(visible)
        .shouldHave(text(FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX + " There are no changes to save."));

    SourceControlEditorPage.credentialsFieldset().shouldBe(visible);
    SourceControlEditorPage.credentialsFieldset().labels().get(0).shouldHave(text("Inherit (Not Configured)"));
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(0).shouldBe(disabled);

    SourceControlEditorPage.credentialsFieldset().labels().get(1).shouldHave(text("Override"));
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldBe(disabled);

    SourceControlEditorPage.token().shouldBe(disabled);

    SourceControlEditorPage.repositoryUrlControls().shouldNotBe(visible);

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
        .shouldHave(text("Configures the integration with an external SCM for " + organization.getName()));
    SourceControlEditorPage.providerFieldset().radioInputs().forEach(input -> input.shouldBe(enabled));
    SourceControlEditorPage.providerFieldset().labels().get(0).shouldBe(text("Inherit from Root Organization"));
    SourceControlEditorPage.providerFieldset().radioInputs().get(0).shouldBe(selected);
    SourceControlEditorPage.providerSelect().shouldBe(visible, disabled);
    SourceControlEditorPage.repositoryUrlControls().shouldNotBe(visible);
    SourceControlEditorPage.tokenWarning().shouldNotBe(visible);
    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.resetButton().shouldBe(inherited ? disabled : enabled);
    SourceControlEditorPage.saveButton().click();
    FormUtils.getAlertElement(SourceControlEditorPage.root())
        .shouldBe(visible)
        .shouldHave(text(FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX + " There are no changes to save."));
    SourceControlEditorPage.repositoryUrlControls().shouldNotBe(visible);
    SourceControlEditorPage.defaultBranchNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.remediationPullRequestNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.pullRequestCommentingNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.sourceControlEvaluationsNotSupportedAlert().shouldNotBe(visible);
  }
}
