/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.elements.ErrorBox;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.pages.SourceControlEditorPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.sonatype.plexus.components.cipher.PlexusCipherException;

import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControl.FAKE_SECRET_KEY;

public class ApplicationSourceControlEditorTest
    extends AbstractSourceControlEditorTest
{
  private Application application;

  private static final String REPOSITORY_URL = "http://a.com/b/c";

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

    eyesWatcher.eyesCheck("Source Control Editor Default State");

    assertSourceControlDoesNotExist(rootOrganization.getId());
    assertSourceControlDoesNotExist(organization.getId());
    assertSourceControlDoesNotExist(application.getId());
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

    verifyStartWithSourceControl();
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

    eyesWatcher.eyesCheck("Source Control Editor Default State With Provider and Inherited Token");
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

    verifyStartWithSourceControl();
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

    eyesWatcher.eyesCheck("Source Control Editor Default State With Provider and Inherited Token");
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

    eyesWatcher.eyesCheck("Source Control Editor Default State Valid Token");
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
    eyesWatcher.eyesCheck("Source Control Editor Update With Missing Token");
    assertToolTip("There are no changes to update.");

    SourceControlEditorPage.token().click();
    SourceControlEditorPage.token().setValue(TOKEN);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.saveButton().hover();
    eyesWatcher.eyesCheck("Source Control Editor Update With Missing URL");
    assertToolTip("Unable to update: fields with invalid or missing data.");

    SourceControlEditorPage.repositoryUrl().setValue(REPOSITORY_URL);
    SourceControlEditorPage.saveButton().shouldBe(enabled);
    eyesWatcher.eyesCheck("Source Control Editor Update Valid Data");
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

    eyesWatcher.eyesCheck("Source Control Editor Update - Changed token to inherit");

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.saveButton().hover();
    eyesWatcher.eyesCheck("Source Control Editor Update - After save with inherit");
    assertToolTip("There are no changes to update.");
    SourceControlEditorPage.deleteButton().shouldNotBe(visible);
    SourceControlEditorPage.token().shouldHave(value(""));
    SourceControlEditorPage.tokenOverrideRadio().shouldNotBe(selected);
    SourceControlEditorPage.tokenInheritRadio().shouldBe(selected, enabled);
    SourceControlEditorPage.token().shouldBe(disabled);
  }

  @Test
  public void testSourceControlEditor_updateFailure() throws PlexusCipherException {
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
    errorBox.shouldHave(text("SourceControl already exists for application with id: " + application.getId()));
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
    eyesWatcher.eyesCheck("Source Control Editor - Token warning visible");
    SourceControlEditorPage.token().setValue(TOKEN);
    SourceControlEditorPage.tokenWarning().shouldBe(visible);

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.tokenWarning().shouldBe(visible);
    eyesWatcher.eyesCheck("Source Control Editor - Token warning visible");

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

    eyesWatcher.eyesCheck("Source Control Editor - update enabled");

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.advancedSettings().shouldNotBe(visible);
    SourceControlEditorPage.advancedElementsTrigger().click();
    SourceControlEditorPage.advancedSettings().shouldBe(visible);
    SourceControlEditorPage.baseBranchInput().shouldHave(value("develop"));
  }

  @Test
  public void testSourceControlEditor_gitlabDisablePRFeature() {
    SourceControl rootSourceControl = tempEntity
        .newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.GITHUB, true, true, "master");

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

    verifyStartWithSourceControl();

    rootSourceControl.setProvider(SourceControlProvider.GITLAB);
    sourceControlDAO.update(rootSourceControl);

    refresh();
    SourceControlEditorPage.advancedElementsTrigger().click();
    SourceControlEditorPage.defaultBranchNotSupportedAlert().shouldBe(visible);
    SourceControlEditorPage.defaultBranchNotSupportedAlert()
        .shouldHave(text("This feature is not currently supported for GitLab"));
    SourceControlEditorPage.pullRequestNotSupportedAlert().shouldBe(visible);
    SourceControlEditorPage.pullRequestNotSupportedAlert()
        .shouldHave(text("This feature is not currently supported for GitLab"));
    SourceControlEditorPage.pullRequestsInheritRadio().shouldNotBe(visible);
    SourceControlEditorPage.pullRequestsEnableRadio().shouldNotBe(visible);
    SourceControlEditorPage.pullRequestsDisableRadio().shouldNotBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(DISABLED);

    eyesWatcher.eyesCheck("Source Control Editor - Pull requests disabled for gitlab");
  }

  @Test
  public void testSourceControlEditor_LicensingAwareNotificationOnly() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));
    setLicensedProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    refresh();

    SourceControlEditorPage.advancedElementsTrigger().click();
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

    eyesWatcher.eyesCheck("Source Control Editor - Pull requests disabled no licence");

    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.GITLAB, true, true, "master");

    refresh();
    SourceControlEditorPage.advancedElementsTrigger().click();
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

    eyesWatcher.eyesCheck("Source Control Editor - Pull requests disabled no licence");
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

  @Override
  void verifyStartNoSourceControl() {
    SourceControlEditorPage.root().shouldBe(visible);
    SourceControlEditorPage.title().shouldHave(text("Source Control Configuration"));
    SourceControlEditorPage.subTitle().shouldHave(text(String
        .format("Configures the integration with an external SCM for the %s application", application.getName())));
    SourceControlEditorPage.provider().shouldNotBe(visible);
    SourceControlEditorPage.token().shouldBe(visible, disabled);
    SourceControlEditorPage.repositoryUrlControls().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.deleteButton().shouldNotBe(visible);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("There are no changes to update.");
    SourceControlEditorPage.tokenInheritRadio().label().shouldHave(text("Inherit (Not Configured)"));
    SourceControlEditorPage.tokenInheritRadio().shouldBe(visible, disabled);
    SourceControlEditorPage.tokenInheritRadio().shouldNotBe(selected);
    SourceControlEditorPage.tokenOverrideRadio().label().shouldHave(text("Override"));
    SourceControlEditorPage.tokenOverrideRadio().shouldBe(visible, disabled, selected);
    SourceControlEditorPage.providerWarning().shouldBe(visible);
    SourceControlEditorPage.tokenWarning().shouldBe(visible);
    SourceControlEditorPage.repositoryUrl().shouldHave(text(""));
    SourceControlEditorPage.repositoryUrl().shouldBe(visible, disabled);
    SourceControlEditorPage.advancedSettingsTree().shouldBe(visible);
    SourceControlEditorPage.advancedSettings().shouldBe(visible);
    SourceControlEditorPage.pullRequestsDisableRadio().shouldBe(visible, disabled);
    SourceControlEditorPage.pullRequestsEnableRadio().shouldBe(visible, disabled);
    SourceControlEditorPage.pullRequestsInheritRadio().shouldBe(visible, disabled, selected);
    SourceControlEditorPage.pullRequestsInheritRadio().shouldHave(text("Inherit (Not Configured)"));
    SourceControlEditorPage.baseBranchInheritRadio().shouldBe(visible, disabled, selected);
    SourceControlEditorPage.baseBranchInheritRadio().shouldHave(text("Inherit (Not Configured)"));
    SourceControlEditorPage.baseBranchOverrideRadio().shouldBe(visible, disabled);
    SourceControlEditorPage.baseBranchOverrideRadio().shouldHave(text("Override"));
    SourceControlEditorPage.baseBranchInput().shouldBe(visible, disabled);
    SourceControlEditorPage.baseBranchInput().shouldHave(value(""));
    SourceControlEditorPage.defaultBranchNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.pullRequestNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.advancedSectionRule().shouldBe(visible);
  }

  @Override
  void verifyStartWithSourceControl() {
    SourceControlEditorPage.root().shouldBe(visible);
    SourceControlEditorPage.title().shouldHave(text("Source Control Configuration"));
    SourceControlEditorPage.subTitle().shouldHave(text(String
        .format("Configures the integration with an external SCM for the %s application", application.getName())));
    SourceControlEditorPage.provider().shouldNotBe(visible);
    SourceControlEditorPage.repositoryUrlControls().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.deleteButton().shouldNotBe(visible);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("There are no changes to update.");
    SourceControlEditorPage.providerWarning().shouldNotBe(visible);
    SourceControlEditorPage.advancedSettingsTree().shouldBe(visible);
    SourceControlEditorPage.repositoryUrl().shouldBe(visible, enabled);
    SourceControlEditorPage.defaultBranchNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.pullRequestNotSupportedAlert().shouldNotBe(visible);
    SourceControlEditorPage.advancedSectionRule().shouldNotBe(visible);
  }
}
