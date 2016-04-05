/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.elements.PolicyInheritsToSection;
import com.sonatype.clm.testing.functional.pages.PolicyEditorPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.tag.Tag;

import org.junit.Before;

import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.PolicyInheritsToSection.allRadioText;
import static com.sonatype.clm.testing.functional.elements.PolicyInheritsToSection.specifiedRadioText;

public class ApplicationPolicyEditorTest
    extends AbstractPolicyEditorTest
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

  @Override
  protected void assertNewPolicyStateIsCorrect_inheritanceSection() {
    assertInheritanceSectionDoesNotExist();
  }

  @Override
  protected void testCreatePolicy_inheritanceSection() {
    assertInheritanceSectionDoesNotExist();
  }

  @Override
  protected void testEditPolicy_inheritanceSection() {
    assertInheritanceSectionDoesNotExist();
  }

  @Override
  protected void assertEditPolicyStateIsCorrect_inheritanceSection(Tag category1, Tag category2, boolean isReadOnly) {
    if (isReadOnly) {
      PolicyInheritsToSection inheritance = PolicyEditorPage.inheritanceSection();

      inheritance.allChildrenInheritRadio().input().shouldBe(visible, selected, disabled);
      inheritance.allChildrenInheritRadio().label().shouldHave(allRadioText(YE_OLE_ORGANIZATION));
      inheritance.specifiedChildrenInheritRadio().input().shouldBe(visible).shouldNotBe(selected);
      inheritance.specifiedChildrenInheritRadio().label().shouldHave(specifiedRadioText(YE_OLE_ORGANIZATION));
      inheritance.associationEditor().shouldNotBe(visible);
    }
    else {
      assertInheritanceSectionDoesNotExist();
    }
  }

  private void assertInheritanceSectionDoesNotExist() {
    PolicyEditorPage.inheritanceSection().shouldNotBe(visible);
    PolicyEditorPage.inhertancePill().shouldNotBe(visible);
  }
}
