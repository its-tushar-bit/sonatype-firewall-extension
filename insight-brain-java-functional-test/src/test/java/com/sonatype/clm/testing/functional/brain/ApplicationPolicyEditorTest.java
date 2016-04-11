/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.OwnerDetailTreeView;
import com.sonatype.clm.testing.functional.elements.OwnerTreeView;
import com.sonatype.clm.testing.functional.elements.PolicyInheritsToSection;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.PolicyEditorPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.tag.Tag;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.*;
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

  @Test
  public void testParentPolicyChangeReflectedLocally() throws Exception {
    tempEntity.newPolicy(application.getParentOwnerId(), "policyName", 5, new Action(Action.ID_FAIL), StageTypes.BUILD.getId());
    refreshOrOpen(OwnerSummaryPage.url(application.getType().toString(), application.getPublicId()));
    OwnerSummaryPage.SummaryTile.localPolicy("policyName").shouldBe(visible);
    OwnerTreeView.organization(0).treeViewElement().shouldBe(visible).click();
    OwnerSummaryPage.SummaryTile.localPolicy("policyName").shouldBe(visible).click();
    PolicyEditorPage.summarySection().policyName().clear();
    PolicyEditorPage.summarySection().policyName().sendKeys("policyName2");
    PolicyEditorPage.endOfPagePill().click();
    PolicyEditorPage.saveButton().shouldBe(visible).click();
    FormMask.seeAndWaitForDismissal();
    OwnerDetailTreeView.backLink().shouldBe(visible).click();
    OwnerTreeView.organization(0).application(0).shouldBe(visible).click();
    OwnerSummaryPage.SummaryTile.localPolicy("policyName2").shouldBe(visible);
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
