/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.testing.functional.elements.ActionDropDown;
import com.sonatype.clm.testing.functional.elements.CategoryTile;
import com.sonatype.clm.testing.functional.elements.CategoryTile.CategoryTileAppContext;
import com.sonatype.clm.testing.functional.elements.LicenseThreatGroupTile;
import com.sonatype.clm.testing.functional.elements.LabelTile;
import com.sonatype.clm.testing.functional.elements.ThreatGroupTileSimpleList;
import com.sonatype.clm.testing.functional.elements.TileSimpleList;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.tag.Tag;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.empty;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class ApplicationSummaryViewTest
    extends AbstractSummaryViewTest
{

  private static final String YE_OLE_APPLICATION = "Ye Ole Application";

  private Application application;

  @Before
  public void init() {
    application = tempEntity.newApplicationWithParent(getClass().getSimpleName(), YE_OLE_APPLICATION,
        YE_OLE_ORGANIZATION);

    super.init(application);
  }

  // This is rudimentary as CLM-4836 deals with editing the contact
  @Test
  public void testApplicationContact() {
    User user = tempEntity.newUser();
    Application testApp = tempEntity.newApplication("testApp", "testApp", application.getOrganizationId(), user.getUsername());
    refresh();
    super.init(testApp);
    String contactDisplayName = user.calculateDisplayName();
    OwnerSummaryPage.SummaryTile.contact().shouldHave(text(contactDisplayName));
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
      ActionDropDown.reportLink(i).shouldBe(visible, ActionDropDown.disabled())
          .shouldHave(ActionDropDown
              .reportLinkText(stages.get(i).getName()));
    }

    List<PolicyEvaluation> policyEvaluations = new ArrayList<>();

    for (StageType stage : stages) {
      policyEvaluations.add(
          tempEntity.newPolicyEvaluation(application.getId(), stage.getId(), stage.getId() + "FakeScanID"));
    }

    refresh();

    ActionDropDown.actionButton().click();
    ActionDropDown.reportLinks().shouldHaveSize(policyEvaluations.size());

    for (int i = 0; i < ActionDropDown.reportLinks().size(); i++) {
      ActionDropDown.reportLink(i).shouldBe(visible).shouldNotBe(ActionDropDown.disabled()).shouldHave(
          ActionDropDown.reportLinkText(stages.get(i).getName()));

      ActionDropDown.reportLink(i).followLink();
      Selenide.switchTo().window(1);

      assertThat(WebDriverRunner.getWebDriver().getCurrentUrl(),
          equalTo(ActionDropDown.reportLinkUrl(application.getPublicId(), policyEvaluations.get(i).getScanId())));

      WebDriverRunner.getWebDriver().close();
      Selenide.switchTo().window(0);
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
    testApplicationCategoryTile_Empty();
    testApplicationCategoryTile_WithAppliedCategory();
  }

  private void testApplicationCategoryTile_Empty() {
    CategoryTile categoryTile = new CategoryTileAppContext();
    categoryTile.subHeader().shouldBe(visible).shouldHave(categoryTile.subHeaderText(application.getName()));
    categoryTile.newButton().shouldBe(visible, enabled).shouldHave(categoryTile.buttonText());

    categoryTile.categoryLists().shouldHaveSize(1);

    TileSimpleList appliedCategoryList = categoryTile.categoryList(0);

    appliedCategoryList.emptyDescriptor().shouldBe(visible)
        .shouldHave(categoryTile.emptyListDescriptorText());
    appliedCategoryList.elements().shouldBe(empty);
  }

  private void testApplicationCategoryTile_WithAppliedCategory() {
    Tag category = tempEntity.newTag(application.getParentOwnerId(), "Test Tag", Color.blue);
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
}
