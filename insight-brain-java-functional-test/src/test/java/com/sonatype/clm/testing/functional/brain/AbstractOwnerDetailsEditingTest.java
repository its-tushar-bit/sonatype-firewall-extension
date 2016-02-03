/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.OwnerDetailTreeView;
import com.sonatype.clm.testing.functional.elements.OwnerDetailTreeView.OwnerDetailTreeViewGroup;
import com.sonatype.clm.testing.functional.elements.OwnerDetailTreeView.OwnerDetailTreeViewGroup.OwnerDetailTreeViewItem;
import com.sonatype.clm.testing.functional.pages.AccessEditorPage;
import com.sonatype.clm.testing.functional.pages.ApplicationCategoryEditorPage;
import com.sonatype.clm.testing.functional.pages.CategoryEditorPage;
import com.sonatype.clm.testing.functional.pages.LTGEditorPage;
import com.sonatype.clm.testing.functional.pages.LabelEditorPage;
import com.sonatype.clm.testing.functional.pages.MonitoredStageEditorPage;
import com.sonatype.clm.testing.functional.pages.OwnerDetailsEditingPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.tag.Tag;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.back;
import static com.codeborne.selenide.Selenide.open;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public abstract class AbstractOwnerDetailsEditingTest
    extends AbstractFunctionalTest
{
  private final static List<Role> ROLES = new RoleDAO().getApplicationRoles();

  private Owner currentOwner;

  private Label label;

  private LicenseThreatGroup ltg;

  private Tag category;

  @BeforeClass
  public static void boot() {
    open(ReportListPage.URL);
    loginAsAdmin();
  }

  public void init(Owner currentOwner) {
    this.currentOwner = currentOwner;
    label = tempEntity.newLabel(currentOwner.getId());

    if (currentOwner.getType().equals(OwnerType.ORGANIZATION)) {
      ltg = tempEntity.newLicenseThreatGroup(currentOwner.getId());
      category = tempEntity.newTag(currentOwner.getId());
    }

    tempEntity.newMembershipMapping(currentOwner.getId(), ROLES.get(0).getId(), "admin");

    open(OwnerDetailsEditingPage.url(currentOwner.getType().toString(), currentOwner.getPublicId()));
  }

  @Test
  public void testOwnerTreeViewDetails() {
    OwnerDetailTreeView.header().shouldBe(visible).shouldHave(text(currentOwner.getName()));
    assertThat(OwnerDetailTreeView.headerHref(),
        containsString(OwnerSummaryPage.url(currentOwner.getType().toString(), currentOwner.getPublicId())));

    testRouting_ApplicationCategories(OwnerDetailTreeView.applicationCategoryGroup());
    testRouting_Policies(OwnerDetailTreeView.policyGroup());
    testRouting_ComponentLabels(OwnerDetailTreeView.componentLabelGroup());
    testRouting_LicenseThreatGroups(OwnerDetailTreeView.LTGGroup());
    testRouting_Access(OwnerDetailTreeView.accessGroup());
  }

  private void testRouting_ApplicationCategories(OwnerDetailTreeViewGroup detailGroup) {
    detailGroup.twisty().shouldBe(visible).shouldBe(CLM.EXPANDED);
    detailGroup.twisty().click();
    detailGroup.twisty().shouldBe(visible).shouldBe(CLM.COLLAPSED);

    if (currentOwner.getType().equals(OwnerType.ORGANIZATION)) {
      detailGroup.items().shouldHaveSize(3);
      detailGroup.item(1).root().shouldBe(visible).click();
      detailGroup.item(1).root().shouldBe(CLM.SELECTED);
      waitUntilUrl(CategoryEditorPage.urlToCreate(currentOwner.getPublicId()));

      back();

      detailGroup.item(2).root().shouldBe(visible).shouldHave(text(category.getName())).click();
      detailGroup.item(2).root().shouldBe(CLM.SELECTED);
      detailGroup.item(2).icon().shouldBe(visible).shouldHave(cssClass(category.getColor().toString()));
      waitUntilUrl(CategoryEditorPage.urlToEdit(currentOwner.getPublicId(), category.getId()));

      back();
    }
    else {
      detailGroup.items().shouldHaveSize(2);
      detailGroup.item(1).root().shouldBe(visible).shouldHave(CLM.DISABLED).click();

      // Click should not redirect
      waitUntilUrl(OwnerDetailsEditingPage.url(currentOwner.getType().toString(), currentOwner.getPublicId()));

      tempEntity.newTag(currentOwner.getParentOwnerId());
      refresh();

      detailGroup.twisty().shouldBe(visible).shouldHave(CLM.EXPANDED);
      detailGroup.twisty().click();
      detailGroup.twisty().shouldBe(visible).shouldHave(CLM.COLLAPSED);
      detailGroup.items().shouldHaveSize(2);
      detailGroup.item(1).root().shouldBe(visible).shouldNotHave(CLM.DISABLED).click();

      waitUntilUrl(ApplicationCategoryEditorPage.urlToEdit(currentOwner.getPublicId()));

      back();
    }

    detailGroup.twisty().click();
    detailGroup.twisty().shouldBe(visible).shouldBe(CLM.EXPANDED);
  }

  private void testRouting_Policies(OwnerDetailTreeViewGroup detailGroup) {
    detailGroup.twisty().shouldBe(visible).shouldBe(CLM.EXPANDED);
    detailGroup.twisty().click();
    detailGroup.twisty().shouldBe(visible).shouldBe(CLM.COLLAPSED);
    testMonitoring(detailGroup);
    detailGroup.twisty().click();
    detailGroup.twisty().shouldBe(visible).shouldBe(CLM.EXPANDED);
  }

  private void testMonitoring(OwnerDetailTreeViewGroup detailGroup) {
    OwnerDetailTreeViewItem monitoredStage = detailGroup.item(detailGroup.items().size() - 1);
    monitoredStage.icon().shouldBe(visible);
    monitoredStage.root().shouldBe(visible).click();
    monitoredStage.root().shouldBe(CLM.SELECTED);
    waitUntilUrl(MonitoredStageEditorPage.url(currentOwner.getType().toString(), currentOwner.getPublicId()));
    back();
  }

  private void testRouting_ComponentLabels(OwnerDetailTreeViewGroup detailGroup) {
    detailGroup.twisty().shouldBe(visible).shouldBe(CLM.EXPANDED);
    detailGroup.twisty().click();
    detailGroup.twisty().shouldBe(visible).shouldBe(CLM.COLLAPSED);

    detailGroup.items().shouldHaveSize(3);
    detailGroup.item(1).root().shouldBe(visible).click();
    detailGroup.item(1).root().shouldBe(CLM.SELECTED);
    waitUntilUrl(LabelEditorPage.urlToCreate(currentOwner.getType().toString(), currentOwner.getPublicId()));

    back();

    detailGroup.item(2).root().shouldBe(visible).shouldHave(text(label.getLabel())).click();
    detailGroup.item(2).root().shouldBe(CLM.SELECTED);
    detailGroup.item(2).icon().shouldBe(visible).shouldHave(cssClass(label.getColor().toString()));
    waitUntilUrl(LabelEditorPage
        .urlToEdit(currentOwner.getType().toString(), currentOwner.getPublicId(), label.getId()));

    back();

    detailGroup.twisty().click();
    detailGroup.twisty().shouldBe(visible).shouldBe(CLM.EXPANDED);
  }

  private void testRouting_LicenseThreatGroups(OwnerDetailTreeViewGroup detailGroup) {
    detailGroup.twisty().shouldBe(visible).shouldBe(CLM.EXPANDED);
    detailGroup.twisty().click();
    detailGroup.twisty().shouldBe(visible).shouldBe(CLM.COLLAPSED);

    if (currentOwner.getType().equals(OwnerType.ORGANIZATION)) {
      detailGroup.items().shouldHaveSize(3);
      detailGroup.item(1).root().shouldBe(visible).click();
      detailGroup.item(1).root().shouldBe(CLM.SELECTED);
      waitUntilUrl(LTGEditorPage.urlToCreate(currentOwner.getPublicId()));

      back();

      detailGroup.item(2).root().shouldBe(visible).shouldHave(text(ltg.getName())).click();
      detailGroup.item(2).root().shouldBe(CLM.SELECTED);
      waitUntilUrl(LTGEditorPage.urlToEdit(currentOwner.getPublicId(), ltg.getId()));

      back();
    }

    detailGroup.twisty().click();
    detailGroup.twisty().shouldBe(visible).shouldBe(CLM.EXPANDED);
  }

  private void testRouting_Access(OwnerDetailTreeViewGroup detailGroup) {
    detailGroup.twisty().shouldBe(visible).shouldBe(CLM.EXPANDED);
    detailGroup.twisty().click();
    detailGroup.twisty().shouldBe(visible).shouldBe(CLM.COLLAPSED);

    detailGroup.items().shouldHaveSize(3);
    detailGroup.item(1).root().shouldBe(visible).click();
    detailGroup.item(1).root().shouldBe(CLM.SELECTED);
    waitUntilUrl(AccessEditorPage.urlToCreate(currentOwner.getType().toString(), currentOwner.getPublicId()));

    back();

    detailGroup.item(2).root().shouldBe(visible).shouldHave(text(ROLES.get(0).getName())).click();
    detailGroup.item(2).root().shouldBe(CLM.SELECTED);
    detailGroup.item(2).icon().shouldBe(visible);
    waitUntilUrl(AccessEditorPage.urlToEdit(currentOwner.getType().toString(), currentOwner.getPublicId(), ROLES.get(0)
        .getId()));

    for (int i = 1; i < ROLES.size(); i++) {
      tempEntity.newMembershipMapping(currentOwner.getId(), ROLES.get(i).getId(), "admin");
    }
    refresh();
    detailGroup.item(1).root().shouldBe(visible).shouldBe(CLM.DISABLED);

    back();

    detailGroup.twisty().click();
    detailGroup.twisty().shouldBe(visible).shouldBe(CLM.EXPANDED);
  }
}
