/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.elements.IqAssociationEditor;
import com.sonatype.clm.testing.functional.elements.IqAssociationEditor.AssociationEditorElement;
import com.sonatype.clm.testing.functional.elements.PolicyInheritsToSection;
import com.sonatype.clm.testing.functional.pages.PolicyEditorPage;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.tag.Tag;

import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.PolicyInheritsToSection.allRadioText;
import static com.sonatype.clm.testing.functional.elements.PolicyInheritsToSection.specifiedRadioText;

public class ApplicationPolicyEditorTest
    extends AbstractPolicyEditorTest
{
  private static final String YE_OLE_APPLICATION = "Ye Ole Application";

  private RoleDAO roleDAO;

  private Application application;

  @Before
  public void init() {
    roleDAO = lookup(RoleDAO.class);

    //note the ȧ being used to force a character to be encoded
    application = tempEntity.newApplicationWithParent(getClass().getSimpleName() + "ȧpp", YE_OLE_APPLICATION,
        YE_OLE_ORGANIZATION);

    super.init(application);
  }

  @Test
  public void testInheritedPolicyWithoutParentalPermission() {
    try {
      logout();

      // Create a policy with a tag
      Policy policy = tempEntity.newPolicy(application.getParentOwnerId(), "policyName");

      tempEntity.newTag(application.getParentOwnerId(), "Unchecked Tag"); // visible but not used
      Tag checkedTag = tempEntity.newTag(application.getParentOwnerId(), "Checked Tag");
      tempEntity.newPolicyTag(policy.getId(), checkedTag.getId());
      tempEntity.newApplicationTag(application.getId(), checkedTag.getId());

      // user with only permission to view the app
      String username = "foo";
      tempEntity.newUser(username);
      tempEntity.newMembershipMapping(application.getId(), roleDAO.getByName("Owner").getId(), username);

      login(username, TemporaryEntity.USER_PASSWORD_CLEAR);
      refreshOrOpen(PolicyEditorPage.urlToEdit(application, policy.getId()));
      refresh(); // because the page has already loaded the store the policy doesn't exist

      IqAssociationEditor categoryEditor = PolicyEditorPage.inheritanceSection().associationEditor();
      categoryEditor.rows().shouldHave(size(2));
      assertCategory(categoryEditor.item(0), "Checked Tag", true);
      assertCategory(categoryEditor.item(1), "Unchecked Tag", false);

      eyesWatcher.eyesCheck("Summary, inheritance, and constraints states are correct");

      // scroll to the actions section
      ScrollUtil.scrollIntoView(PolicyEditorPage.actionsSection().header());
      PolicyEditorPage.actionsSection().develop().noActionRadio().shouldBe(visible);

      eyesWatcher.eyesCheck("Actions and notifications states are correct");
    }
    finally {
      logout();
      refresh();
      // login
      loginAsAdmin();
    }
  }

  private void assertCategory(AssociationEditorElement categoryElement, String categoryName, boolean checked) {
    categoryElement.description().shouldHave(text(categoryName));
    if (checked) {
      categoryElement.checkBox().input().shouldBe(selected);
    }
    else {
      categoryElement.checkBox().input().shouldNotBe(selected);
    }
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

      inheritance.allChildrenInheritRadio().shouldBe(visible, selected, disabled);
      inheritance.allChildrenInheritRadio().shouldHave(allRadioText(YE_OLE_ORGANIZATION));
      inheritance.specifiedChildrenInheritRadio().shouldBe(visible).shouldNotBe(selected);
      inheritance.specifiedChildrenInheritRadio().shouldHave(specifiedRadioText(YE_OLE_ORGANIZATION));
      inheritance.associationEditor().shouldBe(hidden);
      inheritance.policyActionsOverrideCheckbox().shouldBe(visible).shouldNotBe(selected);
    }
    else {
      assertInheritanceSectionDoesNotExist();
    }
  }

  private void assertInheritanceSectionDoesNotExist() {
    PolicyEditorPage.inheritanceSection().shouldBe(hidden);
  }
}
