/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DeleteModal;
import com.sonatype.clm.testing.functional.elements.PopoverViolations;
import com.sonatype.clm.testing.functional.elements.SummarySection;
import com.sonatype.clm.testing.functional.elements.ThreatLevelSelector;
import com.sonatype.clm.testing.functional.pages.OrganizationManagementPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage.SummaryTile;
import com.sonatype.clm.testing.functional.pages.PolicyEditorPage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.tag.Tag;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.open;
import static com.sonatype.clm.testing.functional.elements.CLM.disabledClass;
import static com.sonatype.insight.brain.model.Color.blue;
import static com.sonatype.insight.brain.model.Color.red;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public abstract class AbstractPolicyEditorTest
    extends AbstractFunctionalTest
{
  private Owner currentOwner;

  protected static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  private PolicyDAO policyDAO = new PolicyDAO();

  @BeforeClass
  public static void boot() {
    refreshOrOpen(OrganizationManagementPage.URL);
    loginAsAdmin();
  }

  protected void init(Owner currentOwner) {
    this.currentOwner = currentOwner;
    open(OwnerSummaryPage.url(currentOwner.getType().toString(), currentOwner.getPublicId()));
  }

  @Test
  public void testCreatePolicy() {
    open(OwnerSummaryPage.url(currentOwner.getType().toString(), currentOwner.getPublicId()));
    SummaryTile.addPolicyButton().click();

    assertNewPolicyStateIsCorrect();
    // TODO: Edit data and save needs CLM-5287 Policy editor - constraints section
  }

  @Test
  public void testEditPolicy() {
    Policy policy = tempEntity.newPolicy(currentOwner.getId(), "original name", 1);
    Tag category1 = null;
    Tag category2 = null;
    if (OwnerType.ORGANIZATION.equals(currentOwner.getType())) {
      category1 = tempEntity.newTag(currentOwner.getId(), "Cat_1", blue);
      category2 = tempEntity.newTag(currentOwner.getId(), "Cat_2", red);
      tempEntity.newPolicyTag(policy.getId(), category1.getId());
    }

    refresh();

    SummaryTile.localPolicy(policy.getName()).click();
    assertEditPolicyStateIsCorrect(policy, category1, category2);

    testEditPolicy_summarySection();
    testEditPolicy_inheritanceSection();
    testDeletePolicy(policyDAO.getById(policy.getId()));
  }

  private void testEditPolicy_summarySection() {
    SummarySection summary = PolicyEditorPage.summarySection();
    summary.policyName().val("updated name");
    PolicyEditorPage.saveButton().shouldNotHave(disabledClass()).click();

    changeThreatLevel(6);
    PolicyEditorPage.saveButton().shouldNotHave(disabledClass()).click();

    refresh();

    PolicyEditorPage.title().shouldHave(text("Edit"));
    summary.policyName().shouldBe(visible).shouldHave(value("updated name"));
    ThreatLevelSelector.selectedThreatLevel().shouldBe(text("6"));
    PolicyEditorPage.saveButton().shouldHave(disabledClass());
  }

  private void testDeletePolicy(Policy policy) {
    PolicyEditorPage.deleteButton().shouldBe(visible, enabled).click();

    DeleteModal.root().shouldBe(visible);
    DeleteModal.header().shouldHave(DeleteModal.headerText("Policy"));
    DeleteModal.body().shouldHave(DeleteModal.bodyText(policy.getName()));

    DeleteModal.continueButton().click();
    DeleteModal.root().shouldNotBe(visible);

    assertNewPolicyStateIsCorrect();
    assertThat(policyDAO.getById(policy.getId()), is(nullValue()));
  }

  private void assertNewPolicyStateIsCorrect() {
    waitUntilUrl(PolicyEditorPage.urlToCreate(currentOwner.getType().toString(), currentOwner.getPublicId()));
    PolicyEditorPage.title().shouldHave(text("New"));

    assertNewPolicyStateIsCorrect_summarySection();
    assertNewPolicyStateIsCorrect_inheritanceSection();

    PolicyEditorPage.saveButton().shouldHave(disabledClass());
    PolicyEditorPage.deleteButton().shouldNot(exist);
  }

  private void assertNewPolicyStateIsCorrect_summarySection() {
    SummarySection summary = PolicyEditorPage.summarySection();
    summary.policyName().shouldBe(visible, empty).shouldHave(cssClass("initial-value"));

    summary.policyName().val("$$$"); // invalid characters
    PopoverViolations.on(summary.policyName()).shouldShowInvalidCharactersError();

    summary.policyName().val("Acceptable Name");
    PopoverViolations.on(summary.policyName()).shouldNotExist();

    summary.policyName().clear();

    assertThreatLevelSelectorState(PolicyEditorPage.DEFAULT_THREAT_LEVEL);
  }

  private void assertEditPolicyStateIsCorrect(Policy policy, Tag category1, Tag category2) {
    waitUntilUrl(
        PolicyEditorPage.urlToEdit(currentOwner.getType().toString(), currentOwner.getPublicId(), policy.getId()));
    PolicyEditorPage.title().shouldHave(text("Edit"));

    assertEditPolicyStateIsCorrect_summarySection(policy);
    assertEditPolicyStateIsCorrect_inheritanceSection(category1, category2);
    PolicyEditorPage.saveButton().shouldHave(disabledClass());
  }

  private void assertEditPolicyStateIsCorrect_summarySection(Policy policy) {
    SummarySection summary = PolicyEditorPage.summarySection();
    summary.policyName().shouldBe(visible).shouldHave(cssClass("initial-value")).shouldHave(
        value(policy.getName()));
    assertThreatLevelSelectorState(policy.getThreatLevel());
  }

  private void assertThreatLevelSelectorState(int selectedThreatLevel) {
    ThreatLevelSelector.root().shouldBe(visible);
    ThreatLevelSelector.caretButton().shouldBe(visible, enabled).click();
    ThreatLevelSelector.threatLevelList().shouldBe(visible);
    ThreatLevelSelector.threatLevelListItems().shouldHaveSize(ThreatLevelSelector.NUM_THREAT_LEVELS);

    for (int i = 0; i < ThreatLevelSelector.NUM_THREAT_LEVELS; i++) {
      ThreatLevelSelector.threatLevelListItem(i).shouldBe(visible);
      assertThat(Integer.parseInt(ThreatLevelSelector.threatLevelListItem(i).text()), is(10 - i));
    }

    ThreatLevelSelector.selectedThreatLevel().shouldBe(visible, text(Integer.toString(selectedThreatLevel)))
        .click();
  }

  private void changeThreatLevel(int threatLevel) {
    ThreatLevelSelector.caretButton().shouldBe(visible, enabled).click();
    ThreatLevelSelector.threatLevelListItem(10 - threatLevel).click();
    assertThat(Integer.parseInt(ThreatLevelSelector.selectedThreatLevel().text()), is(threatLevel));
  }

  protected abstract void assertNewPolicyStateIsCorrect_inheritanceSection();

  protected abstract void testEditPolicy_inheritanceSection();

  protected abstract void assertEditPolicyStateIsCorrect_inheritanceSection(Tag category1, Tag category2);
}
