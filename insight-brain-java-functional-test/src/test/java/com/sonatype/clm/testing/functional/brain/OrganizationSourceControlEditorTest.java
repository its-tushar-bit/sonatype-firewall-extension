/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

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
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControl.FAKE_SECRET_KEY;

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
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartNoSourceControl();

    assertSourceControlDoesNotExist(rootOrganization.getId());
    assertSourceControlDoesNotExist(organization.getId());

    tempEntity.newSourceControl(rootOrganization.getId(), null, null, SourceControlProvider.GITHUB);

    refresh();

    assertSourceControlDoesNotExist(organization.getId());

    verifyStartWithSourceControl();
    SourceControlEditorPage.token().shouldBe(visible, disabled);
    SourceControlEditorPage.tokenInheritRadio().label().shouldHave(text("Inherit (Not Configured)"));
    SourceControlEditorPage.tokenInheritRadio().shouldBe(visible, enabled);
    SourceControlEditorPage.tokenOverrideRadio().label().shouldHave(text("Override"));
    SourceControlEditorPage.tokenOverrideRadio().shouldBe(visible, enabled);
    SourceControlEditorPage.tokenOverrideRadio().shouldNotBe(selected);
    SourceControlEditorPage.providerWarning().shouldNotBe(visible);
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
    SourceControlEditorPage.tokenInheritRadio().label().shouldHave(text("Inherit (Not Configured)"));
    SourceControlEditorPage.tokenInheritRadio().shouldBe(visible, enabled);
    SourceControlEditorPage.tokenOverrideRadio().label().shouldHave(text("Override"));
    SourceControlEditorPage.tokenOverrideRadio().shouldBe(visible, enabled);
    SourceControlEditorPage.tokenOverrideRadio().shouldNotBe(selected);
    SourceControlEditorPage.providerWarning().shouldNotBe(visible);
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
    SourceControlEditorPage.tokenInheritRadio().label().shouldHave(text("Inherit (Not Configured)"));
    SourceControlEditorPage.tokenInheritRadio().shouldBe(visible, enabled);
    SourceControlEditorPage.tokenInheritRadio().shouldNotBe(selected);
    SourceControlEditorPage.tokenOverrideRadio().label().shouldHave(text("Override"));
    SourceControlEditorPage.tokenOverrideRadio().shouldBe(visible, enabled, selected);
  }

  @Test
  public void testSourceControlEditor_updateToken() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));
    verifyStartNoSourceControl();
    tempEntity.newSourceControl(rootOrganization.getId(), null, null, SourceControlProvider.GITHUB);
    refresh();

    SourceControlEditorPage.tokenOverrideRadio().click();
    SourceControlEditorPage.tokenOverrideRadio().shouldBe(selected);
    SourceControlEditorPage.tokenInheritRadio().shouldNotBe(selected);
    SourceControlEditorPage.token().shouldBe(enabled);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("There are no changes to update.");

    SourceControlEditorPage.token().setValue(TOKEN);
    eyesWatcher.eyesCheck("Source Control Editor Update With Token Provided");
    SourceControlEditorPage.saveButton().shouldBe(enabled);

    SourceControlEditorPage.tokenInheritRadio().click();
    SourceControlEditorPage.tokenOverrideRadio().shouldNotBe(selected);
    SourceControlEditorPage.tokenInheritRadio().shouldBe(selected);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("There are no changes to update.");

    SourceControlEditorPage.tokenOverrideRadio().click();
    SourceControlEditorPage.tokenOverrideRadio().shouldBe(selected);
    SourceControlEditorPage.tokenInheritRadio().shouldNotBe(selected);
    SourceControlEditorPage.saveButton().shouldNotHave(DISABLED);

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("There are no changes to update.");
    SourceControlEditorPage.deleteButton().shouldNotBe(visible);
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));
    SourceControlEditorPage.tokenOverrideRadio().shouldBe(selected);
    SourceControlEditorPage.tokenInheritRadio().shouldNotBe(selected);

    SourceControlEditorPage.tokenInheritRadio().click();
    SourceControlEditorPage.tokenOverrideRadio().shouldNotBe(selected);
    SourceControlEditorPage.tokenInheritRadio().shouldBe(selected);
    SourceControlEditorPage.token().shouldBe(disabled);
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));
    SourceControlEditorPage.saveButton().shouldNotHave(DISABLED);

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.tokenOverrideRadio().shouldNotBe(selected);
    SourceControlEditorPage.tokenInheritRadio().shouldBe(selected);
    SourceControlEditorPage.token().shouldBe(disabled);
    SourceControlEditorPage.token().shouldHave(value(""));
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("There are no changes to update.");
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
    SourceControlEditorPage.credentialsToken().shouldBe(visible, disabled);
    SourceControlEditorPage.credentialsInheritRadio().label()
        .shouldHave(text("Inherit from Root Organization (" + rootUsername + ")"));
    SourceControlEditorPage.credentialsInheritRadio().shouldBe(visible, enabled);
    SourceControlEditorPage.credentialsOverrideRadio().label().shouldHave(text("Override"));
    SourceControlEditorPage.credentialsOverrideRadio().shouldBe(visible, enabled);
    SourceControlEditorPage.credentialsOverrideRadio().shouldNotBe(selected);
    SourceControlEditorPage.providerWarning().shouldNotBe(visible);

    SourceControlEditorPage.token().shouldNotBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(DISABLED);
    SourceControlEditorPage.credentialsOverrideRadio().click();
    SourceControlEditorPage.credentialsUsername().setValue("orgusername");
    SourceControlEditorPage.saveButton().shouldHave(DISABLED);
    SourceControlEditorPage.credentialsToken().setValue("orgsecrettoken");
    SourceControlEditorPage.saveButton().shouldNotHave(DISABLED);

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.credentialsToken().shouldHave(value(FAKE_SECRET_KEY));
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
    SourceControlEditorPage.credentialsToken().shouldBe(visible, disabled);
    SourceControlEditorPage.credentialsInheritRadio().label()
        .shouldHave(text("Inherit from Root Organization (" + rootUsername + ")"));
    SourceControlEditorPage.credentialsInheritRadio().shouldBe(visible, enabled);
    SourceControlEditorPage.credentialsOverrideRadio().label().shouldHave(text("Override"));
    SourceControlEditorPage.credentialsOverrideRadio().shouldBe(visible, enabled);
    SourceControlEditorPage.credentialsOverrideRadio().shouldNotBe(selected);
    SourceControlEditorPage.providerWarning().shouldNotBe(visible);
    SourceControlEditorPage.token().shouldNotBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(DISABLED);
    SourceControlEditorPage.credentialsOverrideRadio().click();
    SourceControlEditorPage.credentialsUsername().setValue("orgusername");
    SourceControlEditorPage.saveButton().shouldHave(DISABLED);
    SourceControlEditorPage.credentialsToken().setValue("orgsecrettoken");
    SourceControlEditorPage.saveButton().shouldNotHave(DISABLED);

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.credentialsToken().shouldHave(value(FAKE_SECRET_KEY));
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
    SourceControlEditorPage.credentialsToken().shouldNotBe(visible);
    SourceControlEditorPage.credentialsInheritRadio().shouldNotBe(visible);
    SourceControlEditorPage.credentialsInheritRadio().shouldNotBe(visible);
    SourceControlEditorPage.providerWarning().shouldNotBe(visible);
    SourceControlEditorPage.token().shouldBe(visible);

    // when we switch to azure as a provider
    SourceControlEditorPage.credentialsUsername().shouldNotBe(visible);
    SourceControlEditorPage.providerOverrideRadio().click();
    SourceControlEditorPage.provider().chooseOption(new Option(0, "Azure DevOps"));

    // then the credentials should be shown with no option to inherit
    SourceControlEditorPage.credentialsToken().shouldBe(visible, enabled);
    SourceControlEditorPage.credentialsUsername().shouldBe(visible, enabled);
    SourceControlEditorPage.credentialsInheritRadio().shouldNotBe(visible);
    SourceControlEditorPage.credentialsOverrideRadio().shouldNotBe(visible);
    SourceControlEditorPage.providerWarning().shouldNotBe(visible);
    SourceControlEditorPage.token().shouldNotBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(DISABLED);
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

    // then we should see usernaem & token fields
    verifyStartWithSourceControl();
    SourceControlEditorPage.credentialsUsername().shouldBe(visible);
    SourceControlEditorPage.credentialsToken().shouldBe(visible, disabled);
    SourceControlEditorPage.credentialsUsername().shouldBe(visible, disabled);
    SourceControlEditorPage.credentialsInheritRadio().shouldBe(visible);
    SourceControlEditorPage.providerWarning().shouldNotBe(visible);

    // when we switch to azure as a provider
    SourceControlEditorPage.providerOverrideRadio().click();
    SourceControlEditorPage.provider().chooseOption(new Option(0, "Azure DevOps"));

    // then the credentials should be shown with no option to inherit and should be enabled
    SourceControlEditorPage.credentialsToken().shouldBe(visible, enabled);
    SourceControlEditorPage.credentialsUsername().shouldBe(visible, enabled);
    SourceControlEditorPage.credentialsInheritRadio().shouldNotBe(visible);
    SourceControlEditorPage.credentialsOverrideRadio().shouldNotBe(visible);
    SourceControlEditorPage.providerWarning().shouldNotBe(visible);
    SourceControlEditorPage.token().shouldNotBe(visible);
  }

  @Test
  public void testSourceControlEditor_updateFailure() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));
    verifyStartNoSourceControl();
    tempEntity.newSourceControl(rootOrganization.getId(), null, null, SourceControlProvider.GITHUB);
    refresh();

    SourceControlEditorPage.tokenOverrideRadio().click();
    SourceControlEditorPage.token().setValue(TOKEN);

    //Create the entry to make the insert fail
    tempEntity.newSourceControl(organization.getId(), null, null, null);
    SourceControlEditorPage.saveButton().click();

    FormMask.seeAndWaitForDismissal();

    final ErrorBox errorBox = PAGE.error();
    errorBox.retryButton().shouldBe(visible, enabled);
    errorBox.shouldHave(text("SourceControl already exists for organization with id: " + organization.getPublicId()));
    assertSourceControl(organization.getId(), null, null, null);

    //Delete the entry to resolve error condition
    deleteSourceControl(organization.getId());

    errorBox.retryButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));
    SourceControlEditorPage.tokenOverrideRadio().shouldBe(selected);
  }

  @Test
  public void testSourceControlEditor_defaultBranch() {

    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));

    verifyStartNoSourceControl();
    SourceControlEditorPage.baseBranchInheritRadio().shouldBe(selected, disabled);
    SourceControlEditorPage.baseBranchInheritRadio().shouldHave(text("Inherit (Not Configured)"));
    SourceControlEditorPage.baseBranchOverrideRadio().shouldNotBe(selected, enabled);
    SourceControlEditorPage.baseBranchInput().shouldHave(value(""));
    SourceControlEditorPage.baseBranchInput().shouldBe(disabled);
    SourceControlEditorPage.saveButton().shouldHave(DISABLED);

    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.GITHUB, true, true, "master");
    refresh();

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

    SourceControlEditorPage.baseBranchInput().setValue("develop");
    SourceControlEditorPage.baseBranchInput().shouldHave(value("develop"));
    SourceControlEditorPage.saveButton().shouldNotHave(DISABLED);

    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.baseBranchInput().shouldHave(value("develop"));
  }

  @Test
  public void testSourceControlEditor_LicensingAwareNotificationOnly() {
    refreshOrOpen(SourceControlEditorPage.url(OwnerType.ORGANIZATION.toString(), organization.getId()));
    setLicensedProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    refresh();

    verifyNotificationFeaturesOnly();
    SourceControlEditorPage.saveButton().shouldHave(DISABLED);

    eyesWatcher.eyesCheck("Source Control Editor - organization configurations disabled, no license");

    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, TOKEN, SourceControlProvider.GITLAB, true, true, "master");

    refresh();

    verifyNotificationFeaturesOnly();
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
        .format("Configures the integration with an external SCM for the %s organization", organization.getName())));
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
    SourceControlEditorPage.tokenInheritRadio().shouldBe(visible, selected);
    SourceControlEditorPage.tokenInheritRadio().shouldHave(text("Inherit from Root Organization"));
    SourceControlEditorPage.tokenOverrideRadio().shouldBe(visible, enabled);
    SourceControlEditorPage.token().shouldBe(visible, disabled);
    SourceControlEditorPage.providerOverrideRadio().shouldBe(visible, enabled);
    SourceControlEditorPage.providerInheritRadio().shouldBe(visible, selected, enabled);
    SourceControlEditorPage.providerInheritRadio()
        .shouldHave(text("Inherit from Root Organization (GitHub)"));

    // when change provider to bitbucket
    SourceControlEditorPage.credentialsUsername().shouldNotBe(visible);
    SourceControlEditorPage.providerOverrideRadio().click();
    SourceControlEditorPage.provider().shouldBe(visible, enabled);
    SourceControlEditorPage.provider().chooseOption(new Option(1, "bitbucket"));

    // then credentials are shown with no inherit/override radio buttons
    SourceControlEditorPage.tokenInheritRadio().shouldNotBe(visible);
    SourceControlEditorPage.tokenOverrideRadio().shouldNotBe(visible);
    SourceControlEditorPage.credentialsInheritRadio().shouldNotBe(visible);
    SourceControlEditorPage.credentialsOverrideRadio().shouldNotBe(visible);
    SourceControlEditorPage.credentialsUsername().shouldBe(visible, enabled);

    // when change to gitlab (no user name required)
    SourceControlEditorPage.provider().chooseOption(new Option(3, "gitlab"));

    // then username is not show, and token inherit radio buttons also not shown
    SourceControlEditorPage.credentialsInheritRadio().shouldNotBe(visible);
    SourceControlEditorPage.credentialsOverrideRadio().shouldNotBe(visible);
    SourceControlEditorPage.credentialsUsername().shouldNotBe(visible);
    SourceControlEditorPage.tokenInheritRadio().shouldNotBe(visible);
    SourceControlEditorPage.tokenOverrideRadio().shouldNotBe(visible);
    SourceControlEditorPage.token().shouldBe(visible, enabled);

    // when save
    SourceControlEditorPage.saveButton().shouldBe(enabled);
    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    // then see results and token is shown but empty
    SourceControlEditorPage.tokenInheritRadio().shouldNotBe(visible);
    SourceControlEditorPage.tokenOverrideRadio().shouldNotBe(visible);
    SourceControlEditorPage.token().shouldBe(visible, enabled, empty);

    // when set token
    SourceControlEditorPage.token().setValue(TOKEN);
    SourceControlEditorPage.saveButton().shouldBe(enabled);
    SourceControlEditorPage.saveButton().click();
    FormMask.seeAndWaitForDismissal();

    // then token inherit still not shown and value is set to be the fake
    SourceControlEditorPage.tokenInheritRadio().shouldNotBe(visible);
    SourceControlEditorPage.tokenOverrideRadio().shouldNotBe(visible);
    SourceControlEditorPage.token().shouldBe(visible, enabled);
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));
  }

  @Override
  void verifyStartNoSourceControl() {
    SourceControlEditorPage.root().shouldBe(visible);
    SourceControlEditorPage.title().shouldHave(text("Source Control Configuration"));
    SourceControlEditorPage.subTitle().shouldHave(text(String
        .format("Configures the integration with an external SCM for the %s organization", organization.getName())));
    SourceControlEditorPage.providerInheritRadio().shouldBe(visible, selected, enabled);
    SourceControlEditorPage.providerOverrideRadio().shouldBe(visible, enabled);
    SourceControlEditorPage.token().shouldBe(visible, disabled);
    SourceControlEditorPage.repositoryUrlControls().shouldNotBe(visible);
    SourceControlEditorPage.tokenWarning().shouldNotBe(visible);

    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.deleteButton().shouldNotBe(visible);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("There are no changes to update.");

    SourceControlEditorPage.tokenInheritRadio().label().shouldHave(text("Inherit (Not Configured)"));
    SourceControlEditorPage.tokenInheritRadio().shouldBe(visible, disabled, selected);
    SourceControlEditorPage.tokenOverrideRadio().label().shouldHave(text("Override"));
    SourceControlEditorPage.tokenOverrideRadio().shouldBe(visible, disabled);
    SourceControlEditorPage.tokenOverrideRadio().shouldNotBe(selected);

    SourceControlEditorPage.providerWarning().shouldNotBe(visible);
    SourceControlEditorPage.advancedSettingsTree().shouldNotBe(visible);
    SourceControlEditorPage.repositoryUrlControls().shouldNotBe(visible);
    SourceControlEditorPage.advancedSettings().shouldBe(visible);

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
    SourceControlEditorPage.advancedSectionRule().shouldNotBe(visible);
  }

  @Override
  void verifyStartWithSourceControl() {
    SourceControlEditorPage.root().shouldBe(visible);
    SourceControlEditorPage.title().shouldHave(text("Source Control Configuration"));
    SourceControlEditorPage.subTitle().shouldHave(text(String
        .format("Configures the integration with an external SCM for the %s organization", organization.getName())));
    SourceControlEditorPage.providerInheritRadio().label().shouldBe(text("Inherit from Root Organization"));
    SourceControlEditorPage.providerInheritRadio().shouldBe(selected, enabled);
    SourceControlEditorPage.providerOverrideRadio().shouldBe(visible);
    SourceControlEditorPage.provider().shouldBe(visible, enabled);
    SourceControlEditorPage.repositoryUrlControls().shouldNotBe(visible);
    SourceControlEditorPage.tokenWarning().shouldNotBe(visible);
    SourceControlEditorPage.saveButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"), DISABLED);
    SourceControlEditorPage.deleteButton().shouldNotBe(visible);
    SourceControlEditorPage.saveButton().hover();
    assertToolTip("There are no changes to update.");
    SourceControlEditorPage.providerWarning().shouldNotBe(visible);
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
