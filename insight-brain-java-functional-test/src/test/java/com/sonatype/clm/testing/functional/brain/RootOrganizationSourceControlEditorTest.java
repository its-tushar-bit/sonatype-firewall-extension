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
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControl.FAKE_SECRET_KEY;

public class RootOrganizationSourceControlEditorTest
    extends AbstractSourceControlEditorTest
{
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

    eyesWatcher.eyesCheck("Source Control Editor Default State");

    SourceControlEditorPage.provider().chooseOption(new Option(0, "github"));

    SourceControlEditorPage.root().shouldBe(visible);
    SourceControlEditorPage.provider().shouldBe(visible, enabled);
    SourceControlEditorPage.token().shouldBe(visible, enabled);
    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Create"), DISABLED);
    SourceControlEditorPage.pullRequestsEnableRadio().click();
    SourceControlEditorPage.saveButton().shouldNotHave(DISABLED);
    SourceControlEditorPage.deleteButton().shouldNotBe(visible);

    assertSourceControlDoesNotExist(ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testSourceControlEditor_create() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartNoSourceControl();

    SourceControlEditorPage.provider().chooseOption(new Option(0, "github"));

    SourceControlEditorPage.token().shouldBe(enabled);
    SourceControlEditorPage.pullRequestsDisableRadio().shouldBe(enabled);
    SourceControlEditorPage.pullRequestsDisableRadio().shouldNotBe(selected);
    SourceControlEditorPage.pullRequestsEnableRadio().shouldBe(enabled);
    SourceControlEditorPage.pullRequestsEnableRadio().shouldNotBe(selected);
    SourceControlEditorPage.saveButton().shouldHave(DISABLED);

    SourceControlEditorPage.pullRequestsEnableRadio().click();
    SourceControlEditorPage.saveButton().shouldNotHave(DISABLED);

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.deleteButton().shouldBe(visible);
    SourceControlEditorPage.token().shouldHave(value(""));

    assertSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
  }

  @Test
  public void testSourceControlEditor_createFailure() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    SourceControlEditorPage.provider().chooseOption(new Option(0, "GitHub"));

    SourceControlEditorPage.token().shouldBe(enabled);
    SourceControlEditorPage.pullRequestsDisableRadio().shouldBe(enabled);
    SourceControlEditorPage.pullRequestsDisableRadio().shouldNotBe(selected);
    SourceControlEditorPage.pullRequestsEnableRadio().shouldBe(enabled);
    SourceControlEditorPage.pullRequestsEnableRadio().shouldNotBe(selected);
    SourceControlEditorPage.saveButton().shouldHave(DISABLED);

    SourceControlEditorPage.pullRequestsEnableRadio().click();
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
    SourceControlEditorPage.deleteButton().shouldBe(visible);
    SourceControlEditorPage.token().shouldHave(value(""));
    assertSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
  }

  @Test
  public void testSourceControlEditor_update() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.GITHUB, true, true, "master");

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartWithSourceControl();

    SourceControlEditorPage.provider().chooseOption(new Option(1, "gitlab"));

    SourceControlEditorPage.token().shouldBe(enabled);
    SourceControlEditorPage.saveButton().shouldNotHave(DISABLED);

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.deleteButton().shouldBe(visible);
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));
    assertSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.GITLAB);
  }

  @Test
  public void testSourceControlEditor_updateFailure() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.GITHUB, true, true, "master");

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartWithSourceControl();
    SourceControlEditorPage.provider().chooseOption(new Option(1, "GitLab"));
    SourceControlEditorPage.token().shouldBe(enabled);
    SourceControlEditorPage.pullRequestsDisableRadio().shouldNotBe(visible);
    SourceControlEditorPage.pullRequestsEnableRadio().shouldNotBe(visible);
    SourceControlEditorPage.saveButton().shouldNotHave(DISABLED);

    //Delete the entry to create the failure
    deleteRootOrgSourceControl();
    SourceControlEditorPage.saveButton().click();

    final ErrorBox errorBox = PAGE.error();
    errorBox.retryButton().shouldBe(visible, enabled);
    errorBox.shouldHave(text("Cannot find SourceControl for organization with id: ROOT_ORGANIZATION_ID"));
    assertSourceControlDoesNotExist(ROOT_ORGANIZATION_ID);

    //Create the entry to resolve error condition
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.GITHUB);

    errorBox.retryButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.deleteButton().shouldBe(visible);
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));
    assertSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.GITLAB);
  }

  @Test
  public void testSourceControlEditor_delete() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.GITHUB);

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartWithSourceControl();

    SourceControlEditorPage.deleteButton().click();

    eyesWatcher.eyesCheck("Source Control Editor Delete Modal");

    DeleteModal.root().shouldBe(visible);
    DeleteModal.header().shouldHave(DeleteModal.headerText("Source Control"));
    DeleteModal.body().shouldHave(DeleteModal.bodyText(
        "Source Control configuration for organization Root Organization"));

    DeleteModal.continueButton().click();
    FormMask.seeAndWaitForDismissal();
    DeleteModal.root().shouldBe(hidden);

    SourceControlEditorPage.provider().shouldBe(visible, enabled);
    SourceControlEditorPage.token().shouldBe(visible, disabled);
    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Create"), DISABLED);
    SourceControlEditorPage.deleteButton().shouldNotBe(visible);
    assertSourceControlDoesNotExist(ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testSourceControlEditor_deleteFailure() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.GITHUB);

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartWithSourceControl();
    SourceControlEditorPage.deleteButton().click();

    DeleteModal.root().shouldBe(visible);
    DeleteModal.header().shouldHave(DeleteModal.headerText("Source Control"));
    DeleteModal.body().shouldHave(DeleteModal.bodyText(
        "Source Control configuration for organization Root Organization"));

    //Delete entry to create error condition
    deleteRootOrgSourceControl();

    DeleteModal.continueButton().click();

    DeleteModal.error().shouldHave(text("Cannot find SourceControl for organization with id: ROOT_ORGANIZATION_ID"));
    DeleteModal.retryButton().shouldBe(visible, enabled);

    //Recreate the entry to resolve error condition
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.GITHUB);

    DeleteModal.retryButton().click();

    FormMask.seeAndWaitForDismissal();
    DeleteModal.root().shouldBe(hidden);

    SourceControlEditorPage.provider().shouldBe(visible, enabled);
    SourceControlEditorPage.token().shouldBe(visible, disabled);
    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Create"), DISABLED);
    SourceControlEditorPage.deleteButton().shouldNotBe(visible);
    assertSourceControlDoesNotExist(ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testSourceControlEditor_defaultBranch() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.GITHUB, true, true, "master");

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
  public void testSourceControlEditor_gitlabDisablePRFeature() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.GITHUB, true, true, "master");

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartWithSourceControl();
    SourceControlEditorPage.provider().chooseOption(new Option(1, "gitlab"));

    SourceControlEditorPage.pullRequestsEnableRadio().shouldNotBe(visible);
    SourceControlEditorPage.pullRequestsDisableRadio().shouldNotBe(visible);
    SourceControlEditorPage.saveButton().shouldNotHave(DISABLED);
    SourceControlEditorPage.defaultBranchNotSupportedAlert().shouldBe(visible);
    SourceControlEditorPage.defaultBranchNotSupportedAlert()
        .shouldHave(text("This feature is not currently supported for GitLab"));
    SourceControlEditorPage.pullRequestNotSupportedAlert().shouldBe(visible);
    SourceControlEditorPage.pullRequestNotSupportedAlert()
        .shouldHave(text("This feature is not currently supported for GitLab"));

    eyesWatcher.eyesCheck("Source Control Editor - Pull requests disabled for gitlab");

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.deleteButton().shouldBe(visible);
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));
    assertSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.GITLAB);
    SourceControlEditorPage.pullRequestsEnableRadio().shouldNotBe(visible);
    SourceControlEditorPage.pullRequestsDisableRadio().shouldNotBe(visible);
    SourceControlEditorPage.defaultBranchNotSupportedAlert().shouldBe(visible);
    SourceControlEditorPage.defaultBranchNotSupportedAlert()
        .shouldHave(text("This feature is not currently supported for GitLab"));
    SourceControlEditorPage.pullRequestNotSupportedAlert().shouldBe(visible);
    SourceControlEditorPage.pullRequestNotSupportedAlert()
        .shouldHave(text("This feature is not currently supported for GitLab"));
  }

  @Test
  public void testSourceControlEditor_LicensingAwareNotificationOnly() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));
    setLicensedProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    refresh();

    SourceControlEditorPage.defaultBranchNotSupportedAlert().shouldBe(visible);
    SourceControlEditorPage.defaultBranchNotSupportedAlert()
        .shouldHave(text("This feature is not supported by your licence"));
    SourceControlEditorPage.pullRequestNotSupportedAlert().shouldBe(visible);
    SourceControlEditorPage.pullRequestNotSupportedAlert()
        .shouldHave(text("This feature is not supported by your licence"));
    SourceControlEditorPage.pullRequestsInheritRadio().shouldNotBe(visible);
    SourceControlEditorPage.pullRequestsEnableRadio().shouldNotBe(visible);
    SourceControlEditorPage.pullRequestsDisableRadio().shouldNotBe(visible);
    SourceControlEditorPage.baseBranchOverrideRadio().shouldNotBe(visible);
    SourceControlEditorPage.baseBranchInheritRadio().shouldNotBe(visible);
    SourceControlEditorPage.baseBranchInput().shouldNotBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(DISABLED);

    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.GITLAB, true, true, "master");

    refresh();
    SourceControlEditorPage.defaultBranchNotSupportedAlert().shouldBe(visible);
    SourceControlEditorPage.defaultBranchNotSupportedAlert()
        .shouldHave(text("This feature is not supported by your licence"));
    SourceControlEditorPage.pullRequestNotSupportedAlert().shouldBe(visible);
    SourceControlEditorPage.pullRequestNotSupportedAlert()
        .shouldHave(text("This feature is not supported by your licence"));
    SourceControlEditorPage.pullRequestsInheritRadio().shouldNotBe(visible);
    SourceControlEditorPage.pullRequestsEnableRadio().shouldNotBe(visible);
    SourceControlEditorPage.pullRequestsDisableRadio().shouldNotBe(visible);
    SourceControlEditorPage.baseBranchOverrideRadio().shouldNotBe(visible);
    SourceControlEditorPage.baseBranchInheritRadio().shouldNotBe(visible);
    SourceControlEditorPage.baseBranchInput().shouldNotBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(DISABLED);
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
    SourceControlEditorPage.providerWarning().shouldNotBe(visible);
    SourceControlEditorPage.tokenWarning().shouldNotBe(visible);
    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Create"), DISABLED);
    SourceControlEditorPage.deleteButton().shouldNotBe(visible);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("Unable to create: fields with invalid or missing data.");
    SourceControlEditorPage.advancedSettingsTree().shouldNotBe(visible);
    SourceControlEditorPage.repositoryUrlControls().shouldNotBe(visible);
    SourceControlEditorPage.advancedSettings().shouldBe(visible);
    SourceControlEditorPage.pullRequestsDisableRadio().shouldBe(visible, disabled);
    SourceControlEditorPage.pullRequestsEnableRadio().shouldBe(visible, disabled);
    SourceControlEditorPage.pullRequestsInheritRadio().shouldNotBe(visible);
    SourceControlEditorPage.baseBranchInheritRadio().shouldNotBe(visible);
    SourceControlEditorPage.baseBranchOverrideRadio().shouldNotBe(visible);
    SourceControlEditorPage.baseBranchInput().shouldBe(visible, disabled);
    SourceControlEditorPage.baseBranchInput().shouldHave(value("master"));
    SourceControlEditorPage.defaultBranchNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.pullRequestNotSupportedAlert().shouldNotBe(visible);
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
    SourceControlEditorPage.providerWarning().shouldNotBe(visible);
    SourceControlEditorPage.tokenWarning().shouldNotBe(visible);
    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.deleteButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("There are no changes to update.");
    SourceControlEditorPage.advancedSettingsTree().shouldNotBe(visible);
    SourceControlEditorPage.repositoryUrlControls().shouldNotBe(visible);
    SourceControlEditorPage.advancedSettings().shouldBe(visible);
    SourceControlEditorPage.defaultBranchNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.pullRequestNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.advancedSectionRule().shouldNotBe(visible);
  }
}
