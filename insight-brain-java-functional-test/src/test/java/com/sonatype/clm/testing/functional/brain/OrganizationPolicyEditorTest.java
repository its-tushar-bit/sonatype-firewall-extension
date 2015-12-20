/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.elements.AssociationEditor.AssociationEditorElement;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.InheritanceSection;
import com.sonatype.clm.testing.functional.pages.PolicyEditorPage;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.tag.Tag;

import org.junit.Before;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.disabledClass;
import static com.sonatype.clm.testing.functional.elements.InheritanceSection.allRadioText;
import static com.sonatype.clm.testing.functional.elements.InheritanceSection.specifiedRadioText;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class OrganizationPolicyEditorTest extends AbstractPolicyEditorTest
{

  private Organization organization;

  @Before
  public void init() {
    organization = tempEntity.newOrganization(YE_OLE_ORGANIZATION);
    super.init(organization);
  }

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

  protected void testEditPolicy_inheritanceSection() {
    InheritanceSection inheritance = PolicyEditorPage.inheritanceSection();
    inheritance.allChildrenInheritRadio().click();
    inheritance.associationEditor().root().shouldNotBe(visible);
    PolicyEditorPage.saveButton().shouldNotHave(disabledClass()).click();
    // wait 800ms for mask to go away
    FormMask.root().shouldNotBe(visible);

    inheritance.allChildrenInheritRadio().shouldBe(selected);
    inheritance.specifiedChildrenInheritRadio().shouldNotBe(selected).click();
    inheritance.associationEditor().root().shouldBe(visible);
    PolicyEditorPage.saveButton().shouldNotHave(disabledClass()).click();
    // wait 800ms for mask to go away
    FormMask.root().shouldNotBe(visible);

    inheritance.allChildrenInheritRadio().shouldNotBe(selected);
    inheritance.specifiedChildrenInheritRadio().shouldBe(selected);
    inheritance.associationEditor().root().shouldBe(visible);
    inheritance.associationEditor().item(1, 0).checkBox().click();
    PolicyEditorPage.saveButton().shouldNotHave(disabledClass()).click();
    // wait 800ms for mask to go away
    FormMask.root().shouldNotBe(visible);

    PolicyEditorPage.saveButton().shouldHave(disabledClass());

    refresh();

    inheritance.allChildrenInheritRadio().shouldNotBe(selected);
    inheritance.specifiedChildrenInheritRadio().shouldBe(selected);
    inheritance.associationEditor().item(0, 0).checkBox().shouldBe(selected);
    inheritance.associationEditor().item(1, 0).checkBox().shouldBe(selected);
  }

  protected void assertEditPolicyStateIsCorrect_inheritanceSection(Tag category1, Tag category2) {
    InheritanceSection inheritance = PolicyEditorPage.inheritanceSection();

    inheritance.allChildrenInheritRadio().shouldBe(visible).shouldNotBe(selected);
    inheritance.allChildrenInheritRadio().shouldHave(allRadioText(organization.getName()));
    inheritance.specifiedChildrenInheritRadio().shouldBe(visible).shouldBe(selected);
    inheritance.specifiedChildrenInheritRadio().shouldHave(specifiedRadioText(organization.getName()));
    inheritance.associationEditor().root().shouldBe(visible);

    inheritance.associationEditor().rows().shouldHaveSize(2);
    assertThat(inheritance.associationEditor().columnCount(), is(equalTo(1)));
    AssociationEditorElement category1Item = inheritance.associationEditor().item(0, 0);
    category1Item.checkBox().shouldBe(visible).shouldBe(selected);
    category1Item.description().shouldBe(visible).shouldHave(text(category1.getName()));
    category1Item.icon().shouldBe(visible).shouldHave(cssClass(category1.getColor().toValue()));

    AssociationEditorElement category2Item = inheritance.associationEditor().item(1, 0);
    category2Item.checkBox().shouldBe(visible).shouldNotBe(selected);
    category2Item.description().shouldBe(visible).shouldHave(text(category2.getName()));
    category2Item.icon().shouldBe(visible).shouldHave(cssClass(category2.getColor().toValue()));
  }
}

