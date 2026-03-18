/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.NxBreadcrumb;
import com.sonatype.clm.testing.functional.elements.OwnerDetailSidebar;
import com.sonatype.clm.testing.functional.elements.OwnerDetailSidebar.OwnerDetailSidebarGroup;
import com.sonatype.clm.testing.functional.pages.AccessEditorPage;
import com.sonatype.clm.testing.functional.pages.ApplicationCategoryEditorPage;
import com.sonatype.clm.testing.functional.pages.CategoryEditorPage;
import com.sonatype.clm.testing.functional.pages.LTGEditorPage;
import com.sonatype.clm.testing.functional.pages.LabelEditorPage;
import com.sonatype.clm.testing.functional.pages.MonitoredStageEditorPage;
import com.sonatype.clm.testing.functional.pages.OwnerDetailsEditingPage;
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

import com.codeborne.selenide.SelenideElement;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.back;

public abstract class AbstractOwnerDetailsEditingTest
    extends AbstractFunctionalTest
{
  private List<Role> applicationRoles;

  private RoleDAO roleDAO;

  private MembershipMappingDAO membershipMappingDAO;

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

  @Before
  public void setUp() {
    roleDAO = lookup(RoleDAO.class);
    membershipMappingDAO = lookup(MembershipMappingDAO.class);
    applicationRoles = roleDAO.getApplicationRoles();
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

    tempEntity.newMembershipMapping(currentOwner.getId(), applicationRoles.get(0).getId(), "admin");

    refreshOrOpen(OwnerDetailsEditingPage.url(currentOwner));
  }

  @Test
  public void testOwnerTreeViewDetails() {
    NxBreadcrumb breadcrumb = new NxBreadcrumb();
    breadcrumb.currentOwnerForEditPage().shouldHave(text(currentOwner.getName()));

    OwnerDetailSidebar.header().shouldBe(visible).shouldHave(text(currentOwner.getName()));

    if (!OwnerType.REPOSITORY_CONTAINER.equals(currentOwner.getType())) {
      testRouting_ApplicationCategories(OwnerDetailSidebar.applicationCategoryGroup());
      testRouting_Policies(OwnerDetailSidebar.policyGroup());
      testRouting_ComponentLabels(OwnerDetailSidebar.componentLabelGroup());
      testRouting_LicenseThreatGroups(OwnerDetailSidebar.ltgGroup());
      testRouting_Access(OwnerDetailSidebar.accessGroup());
      testRouting_Monitoring(OwnerDetailSidebar.continuousMonitoring());
    }
    else {
      OwnerDetailSidebar.header().shouldBe(visible).shouldHave(text("Repository Managers"));
      OwnerDetailSidebar.applicationCategoryGroup().shouldBe(hidden);
      OwnerDetailSidebar.policyGroup().shouldBe(visible);
      OwnerDetailSidebar.componentLabelGroup().shouldBe(hidden);
      OwnerDetailSidebar.ltgGroup().shouldBe(hidden);
      OwnerDetailSidebar.accessGroup().shouldBe(visible);
      testRouting_Policies(OwnerDetailSidebar.policyGroup());
      testRouting_Access(OwnerDetailSidebar.accessGroup());
    }
  }

  @After
  public void cleanup() {
    membershipMappingDAO
        .delete(
            membershipMappingDAO.getByContextIdAndRoleId(currentOwner.getId(), applicationRoles.get(0).getId()).get(0));
  }

  private void testRouting_ApplicationCategories(OwnerDetailSidebarGroup detailGroup) {
    NxBreadcrumb breadcrumb = new NxBreadcrumb();

    detailGroup.title().parent().shouldBe(visible).shouldHave(cssClass("nx-collapsible-items--collapsed"));
    detailGroup.title().click();
    detailGroup.title().parent().shouldBe(visible).shouldHave(cssClass("nx-collapsible-items--expanded"));

    if (currentOwner.getType().equals(OwnerType.ORGANIZATION)) {
      detailGroup.items().shouldHave(size(2));
      detailGroup.item(0).shouldBe(visible).click();
      detailGroup.item(0).shouldBe(CLM.SELECTED);
      waitUntilUrl(CategoryEditorPage.urlToCreate(currentOwner.getPublicId()));

      breadcrumb.current().shouldHave(text("Organization Category"));

      back();

      detailGroup.item(1).shouldBe(visible).shouldHave(text(category.getName())).click();
      detailGroup.item(1).shouldBe(CLM.SELECTED);
      waitUntilUrl(CategoryEditorPage.urlToEdit(currentOwner.getPublicId(), category.getId()));

      back();
    }
    else {
      detailGroup.items().shouldHave(size(1));
      detailGroup.item(0).shouldBe(visible).shouldHave(CLM.DISABLED).click();

      // Click should not redirect
      waitUntilUrl(OwnerDetailsEditingPage.url(currentOwner));

      tempEntity.newTag(currentOwner.getParentOwnerId());
      refresh();

      detailGroup.title().parent().shouldBe(visible).shouldHave(cssClass("nx-collapsible-items--collapsed"));
      detailGroup.title().click();
      detailGroup.title().parent().shouldBe(visible).shouldHave(cssClass("nx-collapsible-items--expanded"));
      detailGroup.items().shouldHave(size(1));
      detailGroup.item(0).shouldBe(visible).shouldNotHave(CLM.DISABLED).click();

      waitUntilUrl(ApplicationCategoryEditorPage.urlToEdit(currentOwner));

      breadcrumb.current().shouldHave(text("Application Categories"));
      back();
    }

    detailGroup.title().click();
    detailGroup.title().parent().shouldBe(visible).shouldHave(cssClass("nx-collapsible-items--collapsed"));
  }

  private void testRouting_Policies(OwnerDetailSidebarGroup detailGroup) {
    detailGroup.title().parent().shouldBe(visible).shouldHave(cssClass("nx-collapsible-items--collapsed"));
    detailGroup.title().click();
    detailGroup.title().parent().shouldBe(visible).shouldHave(cssClass("nx-collapsible-items--expanded"));
    if (currentOwner.getType().equals(OwnerType.ORGANIZATION)) {
      detailGroup.items().shouldHave(size(3));
      detailGroup.item(0).shouldBe(visible).click();
      detailGroup.item(0).shouldBe(CLM.SELECTED);
      waitUntilUrl(PolicyEditorPage.urlToCreate(currentOwner));

      back();

      detailGroup.item(1).shouldBe(visible).shouldHave(text(policies[1].getName())).click();
      detailGroup.item(1).shouldBe(CLM.SELECTED);
      waitUntilUrl(PolicyEditorPage.urlToEdit(currentOwner, policies[1].getId()));

      NxBreadcrumb breadcrumb = new NxBreadcrumb();
      breadcrumb.current().shouldHave(text("Organization Policy"));

      back();

      detailGroup.item(2).shouldBe(visible).shouldHave(text(policies[0].getName())).click();
      detailGroup.item(2).shouldBe(CLM.SELECTED);
      waitUntilUrl(PolicyEditorPage.urlToEdit(currentOwner, policies[0].getId()));

      back();
    }
    detailGroup.title().click();
    detailGroup.title().parent().shouldBe(visible).shouldHave(cssClass("nx-collapsible-items--collapsed"));
  }

  private void testRouting_Monitoring(SelenideElement continuousMonitoringLink) {
    continuousMonitoringLink.shouldBe(visible).click();
    continuousMonitoringLink.shouldBe(CLM.SELECTED);
    waitUntilUrl(MonitoredStageEditorPage.url(currentOwner));

    NxBreadcrumb breadcrumb = new NxBreadcrumb();
    if (currentOwner.getType().equals(OwnerType.ORGANIZATION)) {
      breadcrumb.current().shouldHave(text("Organization Continuous Monitoring"));
    }
    else {
      breadcrumb.current().shouldHave(text("Application Continuous Monitoring"));
    }

    back();
  }

  private void testRouting_ComponentLabels(OwnerDetailSidebarGroup detailGroup) {
    detailGroup.title().parent().shouldBe(visible).shouldHave(cssClass("nx-collapsible-items--collapsed"));
    detailGroup.title().click();
    detailGroup.title().parent().shouldBe(visible).shouldHave(cssClass("nx-collapsible-items--expanded"));

    detailGroup.items().shouldHave(size(2));
    detailGroup.item(0).shouldBe(visible).click();
    detailGroup.item(0).shouldBe(CLM.SELECTED);
    waitUntilUrl(LabelEditorPage.urlToCreate(currentOwner));

    back();

    detailGroup.item(1).shouldBe(visible).shouldHave(text(label.getLabel())).click();
    detailGroup.item(1).shouldBe(CLM.SELECTED);
    waitUntilUrl(LabelEditorPage.urlToEdit(currentOwner, label.getId()));

    NxBreadcrumb breadcrumb = new NxBreadcrumb();
    if (currentOwner.getType().equals(OwnerType.ORGANIZATION)) {
      breadcrumb.current().shouldHave(text("Organization Labels"));
    }
    else {
      breadcrumb.current().shouldHave(text("Application Labels"));
    }

    back();
    detailGroup.title().click();
    detailGroup.title().parent().shouldBe(visible).shouldHave(cssClass("nx-collapsible-items--collapsed"));
  }

  private void testRouting_LicenseThreatGroups(OwnerDetailSidebarGroup detailGroup) {
    if (!currentOwner.getType().equals(OwnerType.APPLICATION)) {
      detailGroup.title().parent().shouldBe(visible).shouldHave(cssClass("nx-collapsible-items--collapsed"));
      detailGroup.title().click();
      detailGroup.title().parent().shouldBe(visible).shouldHave(cssClass("nx-collapsible-items--expanded"));

      detailGroup.items().shouldHave(size(3));
      detailGroup.item(0).shouldBe(visible).click();
      detailGroup.item(0).shouldBe(CLM.SELECTED);
      waitUntilUrl(LTGEditorPage.urlToCreate(currentOwner));

      back();

      detailGroup.item(1).shouldBe(visible).shouldHave(text(ltgs[1].getName())).click();
      detailGroup.item(1).shouldBe(CLM.SELECTED);
      waitUntilUrl(LTGEditorPage.urlToEdit(currentOwner, ltgs[1].getId()));

      NxBreadcrumb breadcrumb = new NxBreadcrumb();
      breadcrumb.current().shouldHave(text("Organization License Threat Group"));

      back();

      detailGroup.item(2).shouldBe(visible).shouldHave(text(ltgs[0].getName())).click();
      detailGroup.item(2).shouldBe(CLM.SELECTED);
      waitUntilUrl(LTGEditorPage.urlToEdit(currentOwner, ltgs[0].getId()));

      back();

      detailGroup.title().click();
      detailGroup.title().parent().shouldBe(visible).shouldHave(cssClass("nx-collapsible-items--collapsed"));
    }
    else {
      detailGroup.title().parent().shouldBe(hidden);
    }
  }

  private void testRouting_Access(OwnerDetailSidebarGroup detailGroup) {
    detailGroup.title().parent().shouldBe(visible).shouldHave(cssClass("nx-collapsible-items--collapsed"));
    detailGroup.title().click();
    detailGroup.title().parent().shouldBe(visible).shouldHave(cssClass("nx-collapsible-items--expanded"));

    detailGroup.items().shouldHave(size(2));
    detailGroup.item(0).shouldBe(visible).click();
    detailGroup.item(0).shouldBe(CLM.SELECTED);
    waitUntilUrl(AccessEditorPage.urlToCreate(currentOwner));

    back();
    detailGroup.item(1).shouldBe(visible).shouldHave(text(applicationRoles.get(0).getName())).click();
    detailGroup.item(1).shouldBe(CLM.SELECTED);
    waitUntilUrl(AccessEditorPage.urlToEdit(currentOwner, applicationRoles.get(0).getId()));

    NxBreadcrumb breadcrumb = new NxBreadcrumb();
    if (currentOwner.getType().equals(OwnerType.ORGANIZATION)) {
      breadcrumb.current().shouldHave(text("Organization Access"));
    }

    if (currentOwner.getType().equals(OwnerType.APPLICATION)) {
      breadcrumb.current().shouldHave(text("Application Access"));
    }

    for (int i = 1; i < applicationRoles.size(); i++) {
      tempEntity.newMembershipMapping(currentOwner.getId(), applicationRoles.get(i).getId(), "admin");
    }
    refresh();
    detailGroup.item(0).shouldBe(visible).shouldBe(CLM.DISABLED);

    back();

    detailGroup.title().click();
    detailGroup.title().parent().shouldBe(visible).shouldHave(cssClass("nx-collapsible-items--collapsed"));
  }
}
