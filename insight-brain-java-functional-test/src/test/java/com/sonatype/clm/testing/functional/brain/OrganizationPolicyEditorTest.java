/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.elements.IqAssociationEditor.AssociationEditorElement;
import com.sonatype.clm.testing.functional.elements.PolicyInheritsToSection;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.PolicyEditorPage;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.tag.Tag;

import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.IqAssociationEditor.MULTI_COLUMN;
import static com.sonatype.clm.testing.functional.elements.PolicyInheritsToSection.ALL_TEXT_ROOT_ORG;
import static com.sonatype.clm.testing.functional.elements.PolicyInheritsToSection.allRadioText;
import static com.sonatype.clm.testing.functional.elements.PolicyInheritsToSection.specifiedRadioText;

public class OrganizationPolicyEditorTest
    extends AbstractPolicyEditorTest
{
  private Organization organization;

  @Before
  public void init() {
    organization = tempEntity.newOrganization(YE_OLE_ORGANIZATION);
    super.init(organization);
  }

  @Override
  protected void assertNewPolicyStateIsCorrect_inheritanceSection() {
    PolicyInheritsToSection inheritance = PolicyEditorPage.inheritanceSection();

    ScrollUtil.scrollIntoView(PolicyEditorPage.inheritanceSection().header());
    inheritance.shouldBe(visible);
    inheritance.allChildrenInheritRadio().shouldBe(visible).shouldBe(selected);
    inheritance.allChildrenInheritRadio().shouldHave(allRadioText(organization.getName()));
    inheritance.specifiedChildrenInheritRadio().shouldBe(visible, enabled).shouldNotBe(selected);
    inheritance.specifiedChildrenInheritRadio().shouldHave(specifiedRadioText(organization.getName()));
    inheritance.associationEditor().shouldBe(hidden);
    inheritance.policyActionsOverrideCheckbox().shouldBe(visible).shouldNotBe(selected);
  }

  @Override
  protected void testCreatePolicy_inheritanceSection() {
    PolicyInheritsToSection inheritance = PolicyEditorPage.inheritanceSection();
    inheritance.allChildrenInheritRadio().click();
    inheritance.associationEditor().shouldBe(hidden);

    inheritance.specifiedChildrenInheritRadio().shouldNotBe(selected).click();
    inheritance.associationEditor().shouldBe(visible);
    inheritance.associationEditor().item(0).checkBox().click();
    inheritance.policyActionsOverrideCheckbox().shouldBe(visible).shouldNotBe(selected);
  }

  @Override
  protected void testEditPolicy_inheritanceSection() {
    PolicyInheritsToSection inheritance = PolicyEditorPage.inheritanceSection();
    inheritance.allChildrenInheritRadio().click();
    inheritance.associationEditor().shouldBe(hidden);
    PolicyEditorPage.savePolicy();

    ScrollUtil.scrollIntoView(inheritance.header());
    inheritance.allChildrenInheritRadio().shouldBe(selected);
    inheritance.specifiedChildrenInheritRadio().shouldNotBe(selected).click();
    inheritance.associationEditor().shouldBe(visible);
    inheritance.associationEditor().item(1).checkBox().click();
    inheritance.policyActionsOverrideCheckbox().shouldBe(visible).shouldNotBe(selected);
    PolicyEditorPage.savePolicy();

    ScrollUtil.scrollIntoView(inheritance.header());
    inheritance.allChildrenInheritRadio().shouldNotBe(selected);
    inheritance.specifiedChildrenInheritRadio().shouldBe(selected);
    inheritance.associationEditor().shouldBe(visible);

    refresh();

    ScrollUtil.scrollIntoView(inheritance.header());
    inheritance.allChildrenInheritRadio().shouldNotBe(selected);
    inheritance.specifiedChildrenInheritRadio().shouldBe(selected);
    inheritance.associationEditor().item(0).checkBox().shouldBe(selected);
    inheritance.associationEditor().item(1).checkBox().shouldBe(selected);
  }

  @Override
  protected void assertEditPolicyStateIsCorrect_inheritanceSection(Tag category1, Tag category2, boolean isReadOnly) {
    PolicyInheritsToSection inheritance = PolicyEditorPage.inheritanceSection();

    inheritance.allChildrenInheritRadio().shouldBe(visible, isReadOnly ? disabled : enabled).shouldNotBe(selected);
    inheritance.allChildrenInheritRadio()
        .shouldHave(
            allRadioText(isReadOnly ? "Root Organization" : organization.getName()));
    inheritance.specifiedChildrenInheritRadio().shouldBe(visible).shouldBe(selected);
    inheritance.specifiedChildrenInheritRadio()
        .shouldHave(specifiedRadioText(isReadOnly ? "Root Organization" : organization.getName()));
    inheritance.associationEditor().shouldBe(visible);

    inheritance.associationEditor().rows().shouldHave(size(2));
    inheritance.associationEditor().shouldNotBe(MULTI_COLUMN);
    inheritance.policyActionsOverrideCheckbox().shouldBe(visible).shouldNotBe(selected);
    AssociationEditorElement category1Item = inheritance.associationEditor().item(0);
    category1Item.checkBox().shouldBe(visible, selected, isReadOnly ? disabled : enabled);
    category1Item.description().shouldBe(visible).shouldHave(text(category1.getName()));
    category1Item.icon().shouldBe(visible).shouldHave(cssClass("nx-selectable-color--blue"));

    AssociationEditorElement category2Item = inheritance.associationEditor().item(1);
    category2Item.checkBox().shouldBe(visible, isReadOnly ? disabled : enabled).shouldNotBe(selected);
    category2Item.description().shouldBe(visible).shouldHave(text(category2.getName()));
    category2Item.icon().shouldBe(visible).shouldHave(cssClass("nx-selectable-color--red"));
  }

  @Test
  public void testRootOrgPolicyHasProperInheritedText() {
    refreshOrOpen(OwnerSummaryPage.url(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID));

    OwnerSummaryPage.policyTile().addPolicyButton().click();

    PolicyEditorPage.inheritanceSection().allChildrenInheritRadio().label().shouldHave(ALL_TEXT_ROOT_ORG);
  }
}
