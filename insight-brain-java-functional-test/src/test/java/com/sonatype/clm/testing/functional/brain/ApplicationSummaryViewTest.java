/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.testing.functional.elements.ActionDropDown;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.CategoryTile;
import com.sonatype.clm.testing.functional.elements.ChangeApplicationIdDialog;
import com.sonatype.clm.testing.functional.elements.Dropdown;
import com.sonatype.clm.testing.functional.elements.EvaluateApplicationModal;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.LabelTile;
import com.sonatype.clm.testing.functional.elements.LicenseThreatGroupTile;
import com.sonatype.clm.testing.functional.elements.MoveApplicationDialog;
import com.sonatype.clm.testing.functional.elements.OwnerEditorDialog;
import com.sonatype.clm.testing.functional.elements.OwnerTreeView;
import com.sonatype.clm.testing.functional.elements.OwnerTreeView.OrganizationNode;
import com.sonatype.clm.testing.functional.elements.RemoveModal;
import com.sonatype.clm.testing.functional.elements.SelectContactModal;
import com.sonatype.clm.testing.functional.elements.ThreatGroupTileSimpleList;
import com.sonatype.clm.testing.functional.elements.TileSimpleList;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.empty;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.*;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationSummaryViewTest
    extends AbstractSummaryViewTest
{
  private static final String YE_OLE_APPLICATION = "Ye Ole Application";

  private Application application;

  @Before
  public void init() {
    //note the ȧ being used to force a character to be encoded
    application = tempEntity.newApplicationWithParent(getClass().getSimpleName() + "ȧpp", YE_OLE_APPLICATION,
        YE_OLE_ORGANIZATION);

    super.init(application);
  }

  @Test
  public void testApplicationContact() {
    User tempUser = tempEntity.newUser();
    OwnerSummaryPage.summaryTile().contact().shouldNotHave(text(tempUser.calculateDisplayName()));
    // open and close the contact modal
    ActionDropDown.actionButton().click();
    ActionDropDown.selectContact().shouldBe(visible).click();
    SelectContactModal.body().shouldBe(visible);
    SelectContactModal.header().shouldHave(SelectContactModal.headerText());
    SelectContactModal.users().shouldHaveSize(0);
    SelectContactModal.searchButton().shouldBe(disabled);
    SelectContactModal.cancelButton().shouldBe(visible).click();
    SelectContactModal.body().shouldBe(hidden);
    OwnerSummaryPage.summaryTile().contact().shouldNotHave(text(tempUser.calculateDisplayName()));
    // wildcard search returns all users
    ActionDropDown.actionButton().click();
    ActionDropDown.selectContact().click();
    SelectContactModal.searchBox().shouldBe(visible).val("*");
    SelectContactModal.searchButton().shouldBe(enabled).click();
    SelectContactModal.users().shouldHaveSize(2).shouldHave(texts("Admin Builtin", tempUser.calculateDisplayName()));

    eyesWatcher.eyesCheck("Search results");

    // wildcard suffix search narrows search results
    SelectContactModal.searchBox().val(tempUser.getFirstName() + "*");
    SelectContactModal.searchButton().click();
    SelectContactModal.users().shouldHaveSize(1).shouldHave(texts(tempUser.calculateDisplayName()));
    // update contact
    SelectContactModal.updateButton().shouldHave(DISABLED);
    SelectContactModal.userRadio(tempUser.calculateDisplayName()).click();
    SelectContactModal.updateButton().shouldNotHave(DISABLED).click();
    SelectContactModal.body().shouldBe(hidden);
    OwnerSummaryPage.summaryTile().contact().shouldHave(text(tempUser.calculateDisplayName()));
    eyesWatcher.eyesCheck("Contact selected");
    // attempt removal but cancel out of confirmation dialog
    ActionDropDown.actionButton().click();
    ActionDropDown.selectContact().shouldBe(visible).click();
    SelectContactModal.currentUserLabel().shouldHave(text(tempUser.calculateDisplayName()));
    SelectContactModal.searchBox().val("preserves modal state");
    SelectContactModal.removeButton().shouldBe(visible, enabled).click();
    SelectContactModal.body().shouldBe(hidden);
    RemoveModal.body().shouldBe(visible).shouldHave(RemoveModal.bodyText(tempUser.calculateDisplayName()));
    RemoveModal.header().shouldHave(RemoveModal.headerText("Contact"));
    RemoveModal.cancelButton().click();
    RemoveModal.body().shouldBe(hidden);
    SelectContactModal.body().shouldBe(visible);
    SelectContactModal.searchBox().shouldHave(value("preserves modal state"));
    // remove contact
    SelectContactModal.removeButton().click();
    RemoveModal.continueButton().click();
    FormMask.seeAndWaitForDismissal();
    RemoveModal.body().shouldBe(hidden);
    SelectContactModal.body().shouldBe(hidden);
    OwnerSummaryPage.summaryTile().contact().shouldNotHave(text(tempUser.calculateDisplayName()));
  }

  @Test
  public void testApplicationContact_withEditApplicationName() {
    User tempUser = tempEntity.newUser();
    OwnerSummaryPage.summaryTile().name().shouldHave(text(YE_OLE_APPLICATION));

    // open the contact modal
    ActionDropDown.actionButton().click();
    ActionDropDown.selectContact().shouldBe(visible).click();
    SelectContactModal.body().shouldBe(visible);
    SelectContactModal.searchBox().shouldBe(focused).val(tempUser.getFirstName() + "*");
    SelectContactModal.searchButton().click();
    SelectContactModal.users().shouldHaveSize(1).shouldHave(texts(tempUser.calculateDisplayName()));
    // update contact
    SelectContactModal.userRadio(tempUser.calculateDisplayName()).click();
    eyesWatcher.eyesCheck();
    SelectContactModal.updateButton().shouldNotHave(DISABLED).click();
    SelectContactModal.body().shouldBe(hidden);
    OwnerSummaryPage.summaryTile().contact().shouldHave(text(tempUser.calculateDisplayName()));

    // edit the application name
    String shortTypeName = "App";
    String newAppName = "New Name";
    ActionDropDown.actionButton().click();
    OwnerEditorDialog.root().shouldBe(hidden);
    ActionDropDown.editOwner().shouldHave(text(shortTypeName)).click();
    OwnerEditorDialog.root().shouldBe(visible);
    OwnerEditorDialog.title().shouldHave(text(OwnerType.APPLICATION.toString()));
    OwnerEditorDialog.name().val(newAppName);
    OwnerEditorDialog.saveButton().shouldNotBe(DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    OwnerSummaryPage.summaryTile().name().shouldHave(text(newAppName));
    OwnerSummaryPage.summaryTile().contact().shouldBe(visible).shouldHave(text(tempUser.calculateDisplayName()));
  }

  @Override
  @Test
  public void testReportLinks() {
    List<StageType> stages = new ArrayList<>();
    stages.add(StageTypes.BUILD);
    stages.add(StageTypes.STAGE_RELEASE);
    stages.add(StageTypes.RELEASE);
    stages.add(StageTypes.OPERATE);

    final int stagesSize = stages.size();

    ActionDropDown.actionButton().click();
    ActionDropDown.reportLinks().shouldHaveSize(stagesSize);

    for (int i = 0; i < stagesSize; i++) {
      ActionDropDown.reportLink(i).shouldBe(visible, CLM.DISABLED)
          .shouldHave(ActionDropDown.reportLinkText(stages.get(i).getName()));
    }

    List<PolicyEvaluation> policyEvaluations = new ArrayList<>();

    for (StageType stage : stages) {
      policyEvaluations.add(tempEntity.newPolicyEvaluation(application.getId(), stage.getId(), stage.getId()
          + "FakeScanID"));
    }

    refresh();

    final int policyEvaluationsSize = policyEvaluations.size();
    ActionDropDown.actionButton().click();
    ActionDropDown.reportLinks().shouldHaveSize(policyEvaluationsSize);

    for (int i = 0; i < policyEvaluationsSize; i++) {
      ActionDropDown.reportLink(i).shouldBe(visible).shouldNotBe(CLM.DISABLED)
          .shouldHave(ActionDropDown.reportLinkText(stages.get(i).getName()));

      ActionDropDown.reportLink(i).followLink();
      Selenide.switchTo().window(1);

      waitUntilUrl(ApplicationReportPage.url(application, policyEvaluations.get(i).getScanId()));

      WebDriverRunner.getWebDriver().close();
      Selenide.switchTo().window(0);

      waitUntilUrl(OwnerSummaryPage.url(application));

      ActionDropDown.actionButton().click();
    }
  }

  @Test
  public void testLTGTile_NoLocal() {
    int hierarchySize = getHierarchySize(application.getId());

    LicenseThreatGroupTile ltgTile = OwnerSummaryPage.licenseThreatGroupTile();
    ltgTile.subHeader().shouldBe(visible).shouldHave(LabelTile.subHeaderText(application.getName()));
    ltgTile.newButton().shouldBe(hidden);

    ltgTile.ltgLists().shouldHaveSize(hierarchySize);

    for (int i = 0; i < hierarchySize; i++) {
      ThreatGroupTileSimpleList list = ltgTile.ltgList(i);

      if (i != hierarchySize - 1) {
        list.ownerName().shouldBe(hidden);
        list.emptyDescriptor().shouldBe(hidden);
        list.elements().shouldBe(empty);
      }
      else {
        list.ownerName().shouldBe(visible);
        list.emptyDescriptor().shouldBe(hidden);
        list.elements().shouldHaveSize(LicenseThreatGroupDataHelper.TEST_LICENSE_THREAT_GROUP_COUNT);
      }
    }
  }

  @Override
  @Test
  public void testApplicationCategoryTile() {
    testApplicationCategoryTile_noneDefined();

    Tag category = tempEntity.newTag(application.getParentOwnerId(), "Test Tag", Color.dark_blue);
    refresh();

    testApplicationCategoryTile_Empty();
    testApplicationCategoryTile_WithAppliedCategory(category);
  }

  private void testApplicationCategoryTile_noneDefined() {
    CategoryTile categoryTile = OwnerSummaryPage.categoryTile();
    categoryTile.subHeader().shouldBe(visible).shouldHave(CategoryTile.subHeaderText(application));
    categoryTile.newButton().shouldBe(visible).shouldHave(CategoryTile.buttonText(application), CLM.DISABLED);

    categoryTile.categoryLists().shouldHaveSize(1);

    TileSimpleList appliedCategoryList = categoryTile.categoryList(0);

    appliedCategoryList.emptyDescriptor().shouldBe(visible).shouldHave(CategoryTile.noneDefinedText());
    appliedCategoryList.elements().shouldBe(empty);
  }

  private void testApplicationCategoryTile_Empty() {
    CategoryTile categoryTile = OwnerSummaryPage.categoryTile();
    categoryTile.subHeader().shouldBe(visible).shouldHave(CategoryTile.subHeaderText(application));
    categoryTile.newButton().shouldBe(visible, enabled).shouldHave(CategoryTile.buttonText(application));

    categoryTile.categoryLists().shouldHaveSize(1);

    TileSimpleList appliedCategoryList = categoryTile.categoryList(0);

    appliedCategoryList.emptyDescriptor().shouldBe(visible).shouldHave(CategoryTile.noneAssignedText());
    appliedCategoryList.elements().shouldBe(empty);
  }

  private void testApplicationCategoryTile_WithAppliedCategory(Tag category) {
    tempEntity.newApplicationTag(application.getId(), category.getId());

    refresh();

    CategoryTile categoryTile = OwnerSummaryPage.categoryTile();
    categoryTile.subHeader().shouldBe(visible).shouldHave(CategoryTile.subHeaderText(application));
    categoryTile.newButton().shouldBe(visible, enabled).shouldHave(CategoryTile.buttonText(application));

    categoryTile.categoryLists().shouldHaveSize(1);

    TileSimpleList appliedCategoryList = categoryTile.categoryList(0);

    appliedCategoryList.emptyDescriptor().shouldBe(hidden);
    appliedCategoryList.elements().shouldHaveSize(1);
    appliedCategoryList.element(0).name().shouldBe(visible).shouldHave(text(category.getName()));
    appliedCategoryList.element(0).description().shouldBe(visible).shouldHave(text(category.getDescription()));
    appliedCategoryList.element(0).icon().shouldBe(visible).shouldHave(cssClass(category.getColor().toValue()));
    appliedCategoryList.element(0).chevron().shouldBe(hidden);
  }

  @Override
  @Test
  public void testActionDropDown() {
    super.testActionDropDown();

    testEvaluateApplicationBinary(true);
  }

  @Test
  public void testEvaluateApplicationBinary_Foundation() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    refresh();

    testEvaluateApplicationBinary(false);
  }

  @Test
  public void testMoveApplicationLink() {
    MoveApplicationDialog moveAppModal = new MoveApplicationDialog();
    moveAppModal.shouldBe(hidden);
    ActionDropDown.actionButton().click();
    ActionDropDown.moveApplication().shouldBe(visible).shouldHave(text("Move " + application.getName())).click();
    moveAppModal.shouldBe(visible);
  }

  @Test
  public void testChangeApplicationId() {
    ChangeApplicationIdDialog changeApplicationIdDialog = new ChangeApplicationIdDialog();
    changeApplicationIdDialog.shouldBe(hidden);
    ActionDropDown.actionButton().click();
    ActionDropDown.changeApplicationId().shouldBe(visible).shouldNotBe(DISABLED).click();
    changeApplicationIdDialog.shouldBe(visible);
    changeApplicationIdDialog.currentId().shouldHave(text(application.getPublicId()));
    changeApplicationIdDialog.newId().shouldBe(Condition.empty).shouldHave(CLM.PRISTINE);
    changeApplicationIdDialog.changeButton().shouldBe(disabled);

    // current id is not a valid input
    changeApplicationIdDialog.newId().val(application.getPublicId());
    changeApplicationIdDialog.newId().shouldHave(cssClass("ng-invalid"));
    popoverViolations(changeApplicationIdDialog.newId()).shouldBe(visible);
    // use invalid characters and assert the violation popover message
    String invalidCharsMessage = "Use valid characters: alphanumeric, \"_\", \".\" or \"-\"";
    changeApplicationIdDialog.newId().val("*");
    popoverViolations(changeApplicationIdDialog.newId()).shouldHave(text(invalidCharsMessage)).shouldBe(visible);
    // assert that the popover violation message for spaces is the same as invalid characters.
    changeApplicationIdDialog.newId().val("Spaced ID");
    popoverViolations(changeApplicationIdDialog.newId()).shouldHave(text(invalidCharsMessage)).shouldBe(visible);

    // now change the id to a new, valid one
    changeApplicationIdDialog.newId().val("newAppId");
    changeApplicationIdDialog.newId().shouldNotHave(cssClass("ng-invalid"));
    popoverViolations(changeApplicationIdDialog.newId()).shouldNotBe(visible);
    changeApplicationIdDialog.changeButton().shouldBe(enabled).click();
    FormMask.seeAndWaitForDismissal();
    changeApplicationIdDialog.shouldBe(hidden);
    waitUntilUrl(OwnerSummaryPage.url(OwnerType.APPLICATION, "newAppId"));
    OwnerSummaryPage.summaryTile().publicId().shouldHave(text("newAppId"));
    // check that sidebar app link is updated
    OrganizationNode organizationNode = OwnerTreeView.organization(0);
    organizationNode.treeViewElement().click();
    waitUntilNotUrl(OwnerSummaryPage.url(OwnerType.APPLICATION, "newAppId"));
    organizationNode.application(0).click();
    waitUntilUrl(OwnerSummaryPage.url(OwnerType.APPLICATION, "newAppId"));

    // log in as a user that doesn't have permission to change the id of this app
    createUser();
    grantPermissions(getUsername(), application.getId(), Permission.READ);

    logout();
    login();

    try {
      refreshOrOpen(OwnerSummaryPage.url(OwnerType.APPLICATION, "newAppId"));
      ActionDropDown.actionButton().click();
      ActionDropDown.changeApplicationId().shouldBe(visible).shouldHave(DISABLED).click();
      changeApplicationIdDialog.shouldBe(hidden);
    }
    finally {
      logout();
      loginAsAdmin();
    }
  }

  private void testEvaluateApplicationBinary(boolean isNotificationsAllowed) {
    File tempFile = null;

    try {
      tempFile = tempDir.newFile("mockApplicationBinary.war");
    }
    catch (IOException e) {
      throw new AssertionError("Could not create temporary mock binary to evaluate. ", e);
    }
    finally {
      if (tempFile != null) {
        testCLMServer.getHdsServer().setResponseForURI("rest/application/analysis",
            "{\"scanId\": \"blah\", \"timeToReport\": 0}", 200);
        testCLMServer.getHdsServer().setResponseForURI("rest/application/analysis/blah",
            getClass().getResource("/AppEvalReport/report.zip"), 200);

        ActionDropDown.actionButton().click();
        ActionDropDown.evaluateBinaryButton().shouldBe(visible).shouldNotBe(DISABLED).click();

        EvaluateApplicationModal modal = new EvaluateApplicationModal();
        modal.shouldBe(visible);
        modal.fileInput().shouldBe(visible).sendKeys(tempFile.getAbsolutePath());

        Dropdown stageDropdown = modal.stageDropdown();
        stageDropdown.selectedItem().shouldHave(text(EvaluateApplicationModal.SELECT_STAGE_TEXT)).click();
        stageDropdown.listItems().shouldHaveSize(4).shouldHave(texts(StageTypes.BUILD.getName(),
            StageTypes.STAGE_RELEASE.getName(), StageTypes.RELEASE.getName(), StageTypes.OPERATE.getName()));

        stageDropdown.listItem(2).shouldHave(textCaseSensitive(StageTypes.RELEASE.getName())).click();
        stageDropdown.selectedItem().shouldBe(textCaseSensitive(StageTypes.RELEASE.getName()));

        if (!isNotificationsAllowed) {
          EvaluateApplicationModal.disabledNotificationsMessage()
              .shouldBe(text("Notifications are not supported by your license."));
        }
        else {
          EvaluateApplicationModal.disabledNotificationsMessage().shouldBe(hidden);
        }
        Condition disabledOrEnabled = !isNotificationsAllowed ? disabled : enabled;
        modal.notifyRadioButtons().yes().shouldBe(visible, selected, disabledOrEnabled);
        modal.notifyRadioButtons().no().shouldBe(visible, disabledOrEnabled).shouldNotBe(selected);

        modal.cancelButton().shouldBe(visible, enabled);
        modal.uploadButton().shouldBe(visible, enabled).click();

        modal.bundleFileName().shouldBe(text(tempFile.getName()));
        modal.bundleAppName().shouldBe(text(application.getName()));
        modal.bundleStageName().shouldBe(textCaseSensitive(StageTypes.RELEASE.getName()));

        // Give a maximum of 1 minute for the file to be uploaded
        modal.evaluateBundleStatus().waitUntil(text("Done"), 60000);

        modal.closeButton().shouldBe(visible, enabled);

        PolicyEvaluation policyEvaluations = new PolicyEvaluationDAO().getLastByApplicationIdAndStageId(
            application.getId(), StageTypes.RELEASE.getId());

        assertThat(policyEvaluations).isNotNull();

        modal.viewReportButton().shouldBe(visible, enabled).click();

        Selenide.switchTo().window(1);

        waitUntilUrl(ApplicationReportPage.url(application, policyEvaluations.getScanId()));

        WebDriverRunner.getWebDriver().close();
        Selenide.switchTo().window(0);
      }
    }
  }

  @Test
  public void testEvaluateBinaryBtnDisabledWithoutPermissions() {
    // log in as a user that doesn't have permission to evaluate this app
    createUser();
    grantPermissions(getUsername(), application.getId(), Permission.READ);

    logout();
    login();

    try {
      refreshOrOpen(OwnerSummaryPage.url(application));
      ActionDropDown.actionButton().click();
      ActionDropDown.evaluateBinaryButton().shouldBe(visible).shouldHave(DISABLED).hover();
      Tooltip.get().shouldBe(visible).shouldHave(text("Insufficient permissions to evaluate application"));
      ActionDropDown.evaluateBinaryButton().click();
      new EvaluateApplicationModal().shouldBe(hidden);
    }
    finally {
      logout();
      loginAsAdmin();
    }
  }

  @Test
  public void testImportPolicy_NotAvailable() {
    ActionDropDown.actionButton().click();
    ActionDropDown.importPoliciesButton().shouldNotBe(visible);
  }

  @Test
  public void testDataRetentionTile() {
    OwnerSummaryPage.summaryTile().dataRetentionButton().shouldNot(exist);
    OwnerSummaryPage.dataRetentionTile().shouldNot(exist);
  }
}
