/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.pages.PolicyEditorPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.tag.Tag;

import org.junit.Before;

import static com.codeborne.selenide.Condition.visible;

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
  protected void testEditPolicy_inheritanceSection() {
    assertInheritanceSectionDoesNotExist();
  }

  @Override
  protected void assertEditPolicyStateIsCorrect_inheritanceSection(Tag category1, Tag category2, boolean isReadOnly) {
    assertInheritanceSectionDoesNotExist();
  }

  private void assertInheritanceSectionDoesNotExist() {
    PolicyEditorPage.inheritanceSection().shouldNotBe(visible);
    PolicyEditorPage.inhertancePill().shouldNotBe(visible);
  }
}
