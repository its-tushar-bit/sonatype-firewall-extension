/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.elements.AssociationEditor.AssociationEditorElement;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.InheritanceSection;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage.SummaryTile;
import com.sonatype.clm.testing.functional.pages.PolicyEditorPage;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.tag.Tag;

import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.open;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.clm.testing.functional.elements.InheritanceSection.ALL_TEXT_ROOT_ORG;
import static com.sonatype.clm.testing.functional.elements.InheritanceSection.allRadioText;
import static com.sonatype.clm.testing.functional.elements.InheritanceSection.specifiedRadioText;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

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
    InheritanceSection inheritance = PolicyEditorPage.inheritanceSection();

    PolicyEditorPage.inhertancePill().shouldBe(visible);
    inheritance.root.shouldBe(visible);
    inheritance.allChildrenInheritRadio().shouldBe(visible).shouldBe(selected);
    inheritance.allChildrenInheritRadio().shouldHave(allRadioText(organization.getName()));
    inheritance.specifiedChildrenInheritRadio().shouldBe(visible).shouldNotBe(selected);
    inheritance.specifiedChildrenInheritRadio().shouldHave(specifiedRadioText(organization.getName()));
    inheritance.associationEditor().root().shouldNotBe(visible);
  }

  @Override
  protected void testEditPolicy_inheritanceSection() {
    InheritanceSection inheritance = PolicyEditorPage.inheritanceSection();
    inheritance.allChildrenInheritRadio().click();
    inheritance.associationEditor().root().shouldNotBe(visible);
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED).click();
    // wait 800ms for mask to go away
    FormMask.root().shouldNotBe(visible);

    inheritance.allChildrenInheritRadio().shouldBe(selected);
    inheritance.specifiedChildrenInheritRadio().shouldNotBe(selected).click();
    inheritance.associationEditor().root().shouldBe(visible);
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED).click();
    // wait 800ms for mask to go away
    FormMask.root().shouldNotBe(visible);

    inheritance.allChildrenInheritRadio().shouldNotBe(selected);
    inheritance.specifiedChildrenInheritRadio().shouldBe(selected);
    inheritance.associationEditor().root().shouldBe(visible);
    inheritance.associationEditor().item(1, 0).checkBox().click();
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED).click();
    // wait 800ms for mask to go away
    FormMask.root().shouldNotBe(visible);

    PolicyEditorPage.saveButton().shouldHave(DISABLED);

    refresh();

    inheritance.allChildrenInheritRadio().shouldNotBe(selected);
    inheritance.specifiedChildrenInheritRadio().shouldBe(selected);
    inheritance.associationEditor().item(0, 0).checkBox().shouldBe(selected);
    inheritance.associationEditor().item(1, 0).checkBox().shouldBe(selected);
  }

  @Override
  protected void assertEditPolicyStateIsCorrect_inheritanceSection(Tag category1, Tag category2, boolean isReadOnly) {
    InheritanceSection inheritance = PolicyEditorPage.inheritanceSection();

    inheritance.allChildrenInheritRadio().input().shouldBe(visible, isReadOnly ? disabled : enabled).shouldNotBe(selected);
    inheritance.allChildrenInheritRadio().label().shouldHave(allRadioText(organization.getName()));
    inheritance.specifiedChildrenInheritRadio().input().shouldBe(visible).shouldBe(selected);
    inheritance.specifiedChildrenInheritRadio().label().shouldHave(specifiedRadioText(organization.getName()));
    inheritance.associationEditor().root().shouldBe(visible);

    inheritance.associationEditor().rows().shouldHaveSize(2);
    assertThat(inheritance.associationEditor().columnCount(), is(equalTo(1)));
    AssociationEditorElement category1Item = inheritance.associationEditor().item(0, 0);
    category1Item.checkBox().input().shouldBe(visible, selected, isReadOnly ? disabled : enabled);
    category1Item.description().shouldBe(visible).shouldHave(text(category1.getName()));
    category1Item.icon().shouldBe(visible).shouldHave(cssClass(category1.getColor().toValue()));

    AssociationEditorElement category2Item = inheritance.associationEditor().item(1, 0);
    category2Item.checkBox().input().shouldBe(visible, isReadOnly ? disabled : enabled).shouldNotBe(selected);
    category2Item.description().shouldBe(visible).shouldHave(text(category2.getName()));
    category2Item.icon().shouldBe(visible).shouldHave(cssClass(category2.getColor().toValue()));
  }

  @Test
  public void testRootOrgPolicyHasProperInheritedText() {
    open(OwnerSummaryPage.url(OwnerType.ORGANIZATION.toString(), Organization.ROOT_ORGANIZATION_ID));

    SummaryTile.addPolicyButton().click();

    PolicyEditorPage.inheritanceSection().allChildrenInheritRadio().label().shouldHave(ALL_TEXT_ROOT_ORG);
  }
}
