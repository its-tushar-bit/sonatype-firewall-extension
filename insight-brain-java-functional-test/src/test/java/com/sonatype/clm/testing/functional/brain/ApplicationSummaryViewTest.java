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
import com.sonatype.clm.testing.functional.elements.CategoryTile.CategoryTileAppContext;
import com.sonatype.clm.testing.functional.elements.EvaluateApplicationModal;
import com.sonatype.clm.testing.functional.elements.LabelTile;
import com.sonatype.clm.testing.functional.elements.LicenseThreatGroupTile;
import com.sonatype.clm.testing.functional.elements.RemoveModal;
import com.sonatype.clm.testing.functional.elements.SelectContactModal;
import com.sonatype.clm.testing.functional.elements.ThreatGroupTileSimpleList;
import com.sonatype.clm.testing.functional.elements.TileSimpleList;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.tag.Tag;

import com.codeborne.selenide.WebDriverRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.codeborne.selenide.CollectionCondition.empty;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ApplicationSummaryViewTest
    extends AbstractSummaryViewTest
{
  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  private static final String YE_OLE_APPLICATION = "Ye Ole Application";

  private Application application;

  @Before
  public void init() {
    application = tempEntity.newApplicationWithParent(getClass().getSimpleName(), YE_OLE_APPLICATION,
        YE_OLE_ORGANIZATION);

    super.init(application);
  }

  @Test
  public void testApplicationContact() {
    User tempUser = tempEntity.newUser();
    OwnerSummaryPage.SummaryTile.contact().shouldNotHave(text(tempUser.calculateDisplayName()));
    // open and close the contact modal
    ActionDropDown.actionButton().click();
    ActionDropDown.selectContact().shouldBe(visible).click();
    SelectContactModal.body().shouldBe(visible);
    SelectContactModal.header().shouldHave(SelectContactModal.headerText());
    SelectContactModal.users().shouldHaveSize(0);
    SelectContactModal.searchButton().shouldBe(disabled);
    SelectContactModal.cancelButton().shouldBe(visible).click();
    SelectContactModal.body().shouldNotBe(visible);
    OwnerSummaryPage.SummaryTile.contact().shouldNotHave(text(tempUser.calculateDisplayName()));
    // wildcard search returns all users
    ActionDropDown.actionButton().click();
    ActionDropDown.selectContact().click();
    SelectContactModal.searchBox().shouldBe(visible).val("*");
    SelectContactModal.searchButton().shouldBe(enabled).click();
    SelectContactModal.users().shouldHaveSize(2).shouldHave(texts("Admin Builtin", tempUser.calculateDisplayName()));
    // wildcard suffix search narrows search results
    SelectContactModal.searchBox().val(tempUser.getFirstName() + "*");
    SelectContactModal.searchButton().click();
    SelectContactModal.users().shouldHaveSize(1).shouldHave(texts(tempUser.calculateDisplayName()));
    // update contact
    SelectContactModal.updateButton().shouldHave(DISABLED);
    SelectContactModal.userRadio(tempUser.calculateDisplayName()).click();
    SelectContactModal.updateButton().shouldNotHave(DISABLED).click();
    SelectContactModal.body().shouldNotBe(visible);
    OwnerSummaryPage.SummaryTile.contact().shouldHave(text(tempUser.calculateDisplayName()));
    // attempt removal but cancel out of confirmation dialog
    ActionDropDown.actionButton().click();
    ActionDropDown.selectContact().shouldBe(visible).click();
    SelectContactModal.currentUserLabel().shouldHave(text(tempUser.calculateDisplayName()));
    SelectContactModal.searchBox().val("preserves modal state");
    SelectContactModal.removeButton().shouldBe(visible, enabled).click();
    SelectContactModal.body().shouldNotBe(visible);
    RemoveModal.body().shouldBe(visible).shouldHave(RemoveModal.bodyText(tempUser.calculateDisplayName()));
    RemoveModal.header().shouldHave(RemoveModal.headerText("Contact"));
    RemoveModal.cancelButton().click();
    RemoveModal.body().shouldNotBe(visible);
    SelectContactModal.body().shouldBe(visible);
    SelectContactModal.searchBox().shouldHave(value("preserves modal state"));
    // remove contact
    SelectContactModal.removeButton().click();
    RemoveModal.continueButton().click();
    RemoveModal.body().shouldNotBe(visible);
    SelectContactModal.body().shouldNotBe(visible);
    OwnerSummaryPage.SummaryTile.contact().shouldNotHave(text(tempUser.calculateDisplayName()));
  }

  @Override
  @Test
  public void testReportLinks() {
    List<StageType> stages = new ArrayList<>();
    stages.add(StageTypes.BUILD);
    stages.add(StageTypes.STAGE_RELEASE);
    stages.add(StageTypes.RELEASE);
    stages.add(StageTypes.OPERATE);

    ActionDropDown.actionButton().click();
    ActionDropDown.reportLinks().shouldHaveSize(stages.size());

    for (int i = 0; i < ActionDropDown.reportLinks().size(); i++) {
      ActionDropDown.reportLink(i).shouldBe(visible, CLM.DISABLED)
          .shouldHave(ActionDropDown.reportLinkText(stages.get(i).getName()));
    }

    List<PolicyEvaluation> policyEvaluations = new ArrayList<>();

    for (StageType stage : stages) {
      policyEvaluations.add(tempEntity.newPolicyEvaluation(application.getId(), stage.getId(), stage.getId()
          + "FakeScanID"));
    }

    refresh();

    ActionDropDown.actionButton().click();
    ActionDropDown.reportLinks().shouldHaveSize(policyEvaluations.size());

    for (int i = 0; i < ActionDropDown.reportLinks().size(); i++) {
      ActionDropDown.reportLink(i).shouldBe(visible).shouldNotBe(CLM.DISABLED)
          .shouldHave(ActionDropDown.reportLinkText(stages.get(i).getName()));

      ActionDropDown.reportLink(i).followLink();
      switchToWindow(1);

      waitUntilUrl(ActionDropDown.reportLinkUrl(application.getPublicId(), policyEvaluations.get(i).getScanId()));

      WebDriverRunner.getWebDriver().close();
      switchToWindow(0);

      waitUntilUrl(OwnerSummaryPage.url(OwnerType.APPLICATION.toString(), application.getPublicId()));

      ActionDropDown.actionButton().click();
    }
  }

  @Test
  public void testLTGTile_NoLocal() {
    int hierarchySize = getHierarchySize(application.getId());

    LicenseThreatGroupTile ltgTile = new LicenseThreatGroupTile();
    ltgTile.subHeader().shouldBe(visible).shouldHave(LabelTile.subHeaderText(application.getName()));
    ltgTile.newButton().shouldNotBe(visible);

    ltgTile.ltgLists().shouldHaveSize(hierarchySize);

    for (int i = 0; i < ltgTile.ltgLists().size(); i++) {
      ThreatGroupTileSimpleList list = ltgTile.ltgList(i);

      if (i != hierarchySize - 1) {
        list.ownerName().shouldNotBe(visible);
        list.emptyDescriptor().shouldNotBe(visible);
        list.elements().shouldBe(empty);
      }
      else {
        list.ownerName().shouldBe(visible);
        list.emptyDescriptor().shouldNotBe(visible);
        list.elements().shouldHaveSize(LicenseThreatGroupDAO.DEFAULT_LICENSE_THREAT_GROUP_COUNT);
      }
    }
  }

  @Override
  @Test
  public void testApplicationCategoryTile() {
    testApplicationCategoryTile_noneDefined();

    Tag category = tempEntity.newTag(application.getParentOwnerId(), "Test Tag", Color.blue);
    refresh();

    testApplicationCategoryTile_Empty();
    testApplicationCategoryTile_WithAppliedCategory(category);
  }

  private void testApplicationCategoryTile_noneDefined() {
    CategoryTile categoryTile = new CategoryTileAppContext();
    categoryTile.subHeader().shouldBe(visible).shouldHave(categoryTile.subHeaderText(application.getName()));
    categoryTile.newButton().shouldBe(visible).shouldHave(categoryTile.buttonText()).shouldHave(CLM.DISABLED);

    categoryTile.categoryLists().shouldHaveSize(1);

    TileSimpleList appliedCategoryList = categoryTile.categoryList(0);

    appliedCategoryList.emptyDescriptor().shouldBe(visible).shouldHave(CategoryTileAppContext.NO_CATEGORIES_DEFINED);
    appliedCategoryList.elements().shouldBe(empty);
  }

  private void testApplicationCategoryTile_Empty() {
    CategoryTile categoryTile = new CategoryTileAppContext();
    categoryTile.subHeader().shouldBe(visible).shouldHave(categoryTile.subHeaderText(application.getName()));
    categoryTile.newButton().shouldBe(visible, enabled).shouldHave(categoryTile.buttonText());

    categoryTile.categoryLists().shouldHaveSize(1);

    TileSimpleList appliedCategoryList = categoryTile.categoryList(0);

    appliedCategoryList.emptyDescriptor().shouldBe(visible).shouldHave(categoryTile.emptyListDescriptorText());
    appliedCategoryList.elements().shouldBe(empty);
  }

  private void testApplicationCategoryTile_WithAppliedCategory(Tag category) {
    tempEntity.newApplicationTag(application.getId(), category.getId());

    refresh();

    CategoryTile categoryTile = new CategoryTileAppContext();
    categoryTile.subHeader().shouldBe(visible).shouldHave(categoryTile.subHeaderText(application.getName()));
    categoryTile.newButton().shouldBe(visible, enabled).shouldHave(categoryTile.buttonText());

    categoryTile.categoryLists().shouldHaveSize(1);

    TileSimpleList appliedCategoryList = categoryTile.categoryList(0);

    appliedCategoryList.emptyDescriptor().shouldNotBe(visible);
    appliedCategoryList.elements().shouldHaveSize(1);
    appliedCategoryList.element(0).name().shouldBe(visible).shouldHave(text(category.getName()));
    appliedCategoryList.element(0).description().shouldBe(visible).shouldHave(text(category.getDescription()));
    appliedCategoryList.element(0).icon().shouldBe(visible).shouldHave(cssClass(category.getColor().toString()));
    appliedCategoryList.element(0).chevron().shouldNotBe(visible);
  }

  @Override
  @Test
  public void testActionDropDown() {
    super.testActionDropDown();

    testEvaluateApplicationBinary();
  }

  private void testEvaluateApplicationBinary() {
    File tempFile = null;

    try {
      tempFile = tmpDir.newFile("mockApplicationBinary.war");
    }
    catch (IOException e) {
      fail("Could not create temporary mock binary to evaluate. " + e.getMessage());
    }
    finally {
      if (tempFile != null) {
        testCLMServer.getInsightServer().setResponseForURI("rest/application/analysis",
            "{\"scanId\": \"blah\", \"timeToReport\": 0}", 200);
        testCLMServer.getInsightServer().setResponseForURI("rest/application/analysis/blah",
            getClass().getResource("/AppEvalReport/report.zip"), 200);

        ActionDropDown.actionButton().click();
        ActionDropDown.evaluateBinaryButton().shouldBe(visible).click();

        EvaluateApplicationModal.root().shouldBe(visible);
        EvaluateApplicationModal.fileInput().shouldBe(visible).sendKeys(tempFile.getAbsolutePath());
        EvaluateApplicationModal.stageDropdown().selectedItem().shouldBe(EvaluateApplicationModal.defaultStageText())
            .click();
        EvaluateApplicationModal.stageDropdown().listItems().shouldHaveSize(4);

        EvaluateApplicationModal.stageDropdown().listItem(2).shouldHave(text(StageTypes.RELEASE.getName()));
        EvaluateApplicationModal.stageDropdown().listItem(2).click();
        EvaluateApplicationModal.stageDropdown().selectedItem().shouldBe(text(StageTypes.RELEASE.getName()));

        EvaluateApplicationModal.notifyRadioButtons().yes().shouldBe(visible, selected);
        EvaluateApplicationModal.notifyRadioButtons().no().shouldBe(visible).shouldNotBe(selected);

        EvaluateApplicationModal.cancelButton().shouldBe(visible, enabled);
        EvaluateApplicationModal.uploadButton().shouldBe(visible, enabled).click();

        EvaluateApplicationModal.bundleFileName().shouldBe(text(tempFile.getName()));
        EvaluateApplicationModal.bundleAppName().shouldBe(text(application.getName()));
        EvaluateApplicationModal.bundleStageName().shouldBe(text(StageTypes.RELEASE.getName()));

        // Give a maximum of 1 minute for the file to be uploaded
        EvaluateApplicationModal.evaluateBundleStatus().waitUntil(text("Done"), 60000);

        EvaluateApplicationModal.closeButton().shouldBe(visible, enabled);

        PolicyEvaluation policyEvaluations = new PolicyEvaluationDAO().getLastByApplicationIdAndStageId(
            application.getId(), StageTypes.RELEASE.getId());

        assertThat(policyEvaluations, notNullValue());

        EvaluateApplicationModal.viewReportButton().shouldBe(visible, enabled).click();

        switchToWindow(1);

        waitUntilUrl(ActionDropDown.reportLinkUrl(application.getPublicId(), policyEvaluations.getScanId()));

        WebDriverRunner.getWebDriver().close();
        switchToWindow(0);
      }
    }
  }
}
