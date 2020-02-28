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
import com.sonatype.clm.testing.functional.pages.PolicyEditorPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.tag.Tag;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.back;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractOwnerDetailsEditingTest
    extends AbstractFunctionalTest
{
  private static final List<Role> ROLES = new RoleDAO().getApplicationRoles();

  private Owner currentOwner;

  private Label label;

  private LicenseThreatGroup[] ltgs;

  private Policy[] policies;

  private Tag category;

  @BeforeClass
  public static void boot() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  public void init(Owner currentOwner) {
    this.currentOwner = currentOwner;
    label = tempEntity.newLabel(currentOwner.getId());

    if (currentOwner.getType().equals(OwnerType.ORGANIZATION)) {
      ltgs = new LicenseThreatGroup[]{
          tempEntity.newLicenseThreatGroup(currentOwner.getId(), "Bar", 1),
          tempEntity.newLicenseThreatGroup(currentOwner.getId(), "Foo", 10)
      };
      policies = new Policy[]{
          tempEntity.newPolicy(currentOwner.getId(), "Bar", 1),
          tempEntity.newPolicy(currentOwner.getId(), "Foo", 10)
      };
      category = tempEntity.newTag(currentOwner.getId());
    }

    tempEntity.newMembershipMapping(currentOwner.getId(), ROLES.get(0).getId(), "admin");

    refreshOrOpen(OwnerDetailsEditingPage.url(currentOwner));
  }

  @Test
  public void testOwnerTreeViewDetails() {
    assertThat(OwnerDetailTreeView.headerHref()).contains(OwnerSummaryPage.url(currentOwner));

    if (!OwnerType.REPOSITORY_CONTAINER.equals(currentOwner.getType())) {
      OwnerDetailTreeView.header().shouldBe(visible).shouldHave(text(currentOwner.getName()));
      testRouting_ApplicationCategories(OwnerDetailTreeView.applicationCategoryGroup());
      testRouting_Policies(OwnerDetailTreeView.policyGroup());
      testRouting_ComponentLabels(OwnerDetailTreeView.componentLabelGroup());
      testRouting_LicenseThreatGroups(OwnerDetailTreeView.ltgGroup());
      testRouting_Access(OwnerDetailTreeView.accessGroup());
    }
    else {
      OwnerDetailTreeView.header().shouldBe(visible).shouldHave(text("Repositories"));
      OwnerDetailTreeView.applicationCategoryGroup().shouldBe(hidden);
      OwnerDetailTreeView.policyGroup().shouldBe(hidden);
      OwnerDetailTreeView.componentLabelGroup().shouldBe(hidden);
      OwnerDetailTreeView.ltgGroup().shouldBe(hidden);
      OwnerDetailTreeView.accessGroup().shouldBe(visible);
      testRouting_Access(OwnerDetailTreeView.accessGroup());
    }
  }

  @After
  public void cleanup() {
    MembershipMappingDAO membershipMappingDAO = new MembershipMappingDAO();
    membershipMappingDAO
        .delete(membershipMappingDAO.getByContextIdAndRoleId(currentOwner.getId(), ROLES.get(0).getId()).get(0));
  }

  private void testRouting_ApplicationCategories(OwnerDetailTreeViewGroup detailGroup) {
    detailGroup.twisty().shouldBe(visible).shouldBe(CLM.EXPANDED);
    detailGroup.twisty().click();
    detailGroup.twisty().shouldBe(visible).shouldBe(CLM.COLLAPSED);

    if (currentOwner.getType().equals(OwnerType.ORGANIZATION)) {
      detailGroup.items().shouldHaveSize(2);
      detailGroup.item(0).shouldBe(visible).click();
      detailGroup.item(0).shouldBe(CLM.SELECTED);
      waitUntilUrl(CategoryEditorPage.urlToCreate(currentOwner.getPublicId()));

      back();

      detailGroup.item(1).shouldBe(visible).shouldHave(text(category.getName())).click();
      detailGroup.item(1).shouldBe(CLM.SELECTED);
      detailGroup.item(1).icon().shouldBe(visible).shouldHave(cssClass(category.getColor().toString()));
      waitUntilUrl(CategoryEditorPage.urlToEdit(currentOwner.getPublicId(), category.getId()));

      back();
    }
    else {
      detailGroup.items().shouldHaveSize(1);
      detailGroup.item(0).shouldBe(visible).shouldHave(CLM.DISABLED).click();

      // Click should not redirect
      waitUntilUrl(OwnerDetailsEditingPage.url(currentOwner));

      tempEntity.newTag(currentOwner.getParentOwnerId());
      refresh();

      detailGroup.twisty().shouldBe(visible).shouldHave(CLM.EXPANDED);
      detailGroup.twisty().click();
      detailGroup.twisty().shouldBe(visible).shouldHave(CLM.COLLAPSED);
      detailGroup.items().shouldHaveSize(1);
      detailGroup.item(0).shouldBe(visible).shouldNotHave(CLM.DISABLED).click();

      waitUntilUrl(ApplicationCategoryEditorPage.urlToEdit(currentOwner));

      back();
    }

    detailGroup.twisty().click();
    detailGroup.twisty().shouldBe(visible).shouldBe(CLM.EXPANDED);
  }

  private void testRouting_Policies(OwnerDetailTreeViewGroup detailGroup) {
    detailGroup.twisty().shouldBe(visible).shouldBe(CLM.EXPANDED);
    detailGroup.twisty().click();
    detailGroup.twisty().shouldBe(visible).shouldBe(CLM.COLLAPSED);
    if (currentOwner.getType().equals(OwnerType.ORGANIZATION)) {
      detailGroup.items().shouldHaveSize(6);
      detailGroup.item(0).shouldBe(visible).click();
      detailGroup.item(0).shouldBe(CLM.SELECTED);
      waitUntilUrl(PolicyEditorPage.urlToCreate(currentOwner));

      back();

      detailGroup.item(1).shouldBe(visible).shouldHave(text(policies[1].getName())).click();
      detailGroup.item(1).shouldBe(CLM.SELECTED);
      waitUntilUrl(PolicyEditorPage.urlToEdit(currentOwner, policies[1].getId()));
      PolicyEditorPage.saveButton().shouldBe(CLM.DISABLED);

      back();

      detailGroup.item(2).shouldBe(visible).shouldHave(text(policies[0].getName())).click();
      detailGroup.item(2).shouldBe(CLM.SELECTED);
      waitUntilUrl(PolicyEditorPage.urlToEdit(currentOwner, policies[0].getId()));
      PolicyEditorPage.saveButton().shouldBe(CLM.DISABLED);

      back();
    }
    testMonitoring(detailGroup);
    detailGroup.twisty().click();
    detailGroup.twisty().shouldBe(visible).shouldBe(CLM.EXPANDED);
  }

  private void testMonitoring(OwnerDetailTreeViewGroup detailGroup) {
    OwnerDetailTreeViewItem monitoredStage = detailGroup.item(detailGroup.items().size() - 2);
    monitoredStage.icon().shouldBe(visible);
    monitoredStage.shouldBe(visible).click();
    monitoredStage.shouldBe(CLM.SELECTED);
    waitUntilUrl(MonitoredStageEditorPage.url(currentOwner));
    back();
  }

  private void testRouting_ComponentLabels(OwnerDetailTreeViewGroup detailGroup) {
    detailGroup.twisty().shouldBe(visible).shouldBe(CLM.EXPANDED);
    detailGroup.twisty().click();
    detailGroup.twisty().shouldBe(visible).shouldBe(CLM.COLLAPSED);

    detailGroup.items().shouldHaveSize(2);
    detailGroup.item(0).shouldBe(visible).click();
    detailGroup.item(0).shouldBe(CLM.SELECTED);
    waitUntilUrl(LabelEditorPage.urlToCreate(currentOwner));

    back();

    detailGroup.item(1).shouldBe(visible).shouldHave(text(label.getLabel())).click();
    detailGroup.item(1).shouldBe(CLM.SELECTED);
    detailGroup.item(1).icon().shouldBe(visible).shouldHave(cssClass(label.getColor().toValue()));
    waitUntilUrl(LabelEditorPage.urlToEdit(currentOwner, label.getId()));

    back();

    detailGroup.twisty().click();
    detailGroup.twisty().shouldBe(visible).shouldBe(CLM.EXPANDED);
  }

  private void testRouting_LicenseThreatGroups(OwnerDetailTreeViewGroup detailGroup) {
    if (!currentOwner.getType().equals(OwnerType.APPLICATION)) {
      detailGroup.twisty().shouldBe(visible).shouldBe(CLM.EXPANDED);
      detailGroup.twisty().click();
      detailGroup.twisty().shouldBe(visible).shouldBe(CLM.COLLAPSED);

      detailGroup.items().shouldHaveSize(3);
      detailGroup.item(0).shouldBe(visible).click();
      detailGroup.item(0).shouldBe(CLM.SELECTED);
      waitUntilUrl(LTGEditorPage.urlToCreate(currentOwner));

      back();

      detailGroup.item(1).shouldBe(visible).shouldHave(text(ltgs[1].getName())).click();
      detailGroup.item(1).shouldBe(CLM.SELECTED);
      waitUntilUrl(LTGEditorPage.urlToEdit(currentOwner, ltgs[1].getId()));

      back();

      detailGroup.item(2).shouldBe(visible).shouldHave(text(ltgs[0].getName())).click();
      detailGroup.item(2).shouldBe(CLM.SELECTED);
      waitUntilUrl(LTGEditorPage.urlToEdit(currentOwner, ltgs[0].getId()));

      back();

      detailGroup.twisty().click();
      detailGroup.twisty().shouldBe(visible).shouldBe(CLM.EXPANDED);
    }
    else {
      detailGroup.twisty().shouldBe(hidden);
    }
  }

  private void testRouting_Access(OwnerDetailTreeViewGroup detailGroup) {
    detailGroup.twisty().shouldBe(visible).shouldBe(CLM.EXPANDED);
    detailGroup.twisty().click();
    detailGroup.twisty().shouldBe(visible).shouldBe(CLM.COLLAPSED);

    detailGroup.items().shouldHaveSize(2);
    detailGroup.item(0).shouldBe(visible).click();
    detailGroup.item(0).shouldBe(CLM.SELECTED);
    waitUntilUrl(AccessEditorPage.urlToCreate(currentOwner));

    back();

    detailGroup.item(1).shouldBe(visible).shouldHave(text(ROLES.get(0).getName())).click();
    detailGroup.item(1).shouldBe(CLM.SELECTED);
    detailGroup.item(1).icon().shouldBe(visible);
    waitUntilUrl(AccessEditorPage.urlToEdit(currentOwner, ROLES.get(0).getId()));

    for (int i = 1; i < ROLES.size(); i++) {
      tempEntity.newMembershipMapping(currentOwner.getId(), ROLES.get(i).getId(), "admin");
    }
    refresh();
    detailGroup.item(0).shouldBe(visible).shouldBe(CLM.DISABLED);

    back();

    detailGroup.twisty().click();
    detailGroup.twisty().shouldBe(visible).shouldBe(CLM.EXPANDED);
  }
}
