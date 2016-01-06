/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.Arrays;
import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ConstraintSection;
import com.sonatype.clm.testing.functional.elements.DeleteModal;
import com.sonatype.clm.testing.functional.elements.FormMask;
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
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
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
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED_CLASS;
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
    // TODO: Edit data and save needs CLM-5806 - Add Constraints Section
  }

  @Test
  public void testEditPolicy() {
    Policy policy = tempEntity.newPolicy(currentOwner.getId(), "original name", 1);

    Constraint constraint1 = new Constraint(policy.getId() + "1", "First Constraint with One Condition", null);
    constraint1.addCondition(new Condition("AgeInDays", "older than", "730"));
    Constraint constraint2 = new Constraint(policy.getId() + "2", "Second Constraint with Two Conditions",
        LogicalOperator.AND);
    constraint2.addCondition(new Condition("License Threat Group", "is",
        tempEntity.newLicenseThreatGroup(currentOwner.getId(), "my LTG", 5).getId()));
    constraint2.addCondition(
        new Condition("Label", "is", tempEntity.newLabel(currentOwner.getId(), "my Label").getId()));
    Constraint constraint3 = new Constraint(policy.getId() + "3", "Third Constraint with Two Conditions",
        LogicalOperator.OR);
    constraint3.addCondition(new Condition("RelativePopularity", "<", "50"));
    constraint3.addCondition(new Condition("Coordinates", "do not match", "blah:blah:blah"));

    policy.setConstraints(Arrays.asList(constraint1, constraint2, constraint3));

    Tag category1 = null;
    Tag category2 = null;

    if (OwnerType.ORGANIZATION.equals(currentOwner.getType())) {
      category1 = tempEntity.newTag(currentOwner.getId(), "Cat_1", blue);
      category2 = tempEntity.newTag(currentOwner.getId(), "Cat_2", red);
      tempEntity.newPolicyTag(policy.getId(), category1.getId());
    }

    policyDAO.update(policy);
    refresh();

    SummaryTile.localPolicy(policy.getName()).click();
    assertEditPolicyStateIsCorrect(policy, category1, category2);

    testEditPolicy_summarySection();
    testEditPolicy_inheritanceSection();
    testEditPolicy_constraintSection(policy.getConstraints());
    testDeletePolicy(policyDAO.getById(policy.getId()));
  }

  private void testEditPolicy_summarySection() {
    SummarySection summary = PolicyEditorPage.summarySection();
    summary.policyName().val("updated name");
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED_CLASS).click();

    FormMask.root().shouldBe(visible).shouldNotBe(visible);
    changeThreatLevel(6);
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED_CLASS).click();

    refresh();

    PolicyEditorPage.title().shouldHave(text("Edit"));
    summary.policyName().shouldBe(visible).shouldHave(value("updated name"));
    ThreatLevelSelector.selectedThreatLevel().shouldBe(text("6"));
    PolicyEditorPage.saveButton().shouldHave(DISABLED_CLASS);
  }

  private void testEditPolicy_constraintSection(List<Constraint> constraints) {
    ConstraintSection constraintSection = PolicyEditorPage.constraintSection();
    constraintSection.createConstraintButton().shouldBe(visible, enabled);

    // Test Summaries
    constraintSection.constraintSummaries().shouldHaveSize(constraints.size());

    ConstraintSection.ConstraintSummary constraintSummary1 = constraintSection.constraintSummary(0);
    constraintSummary1.name().shouldHave(text(constraints.get(0).getName()));

    List<Condition> conditions = constraints.get(0).getConditions();
    constraintSummary1.subheader()
        .shouldHave(ConstraintSection.ConstraintSummary
            .subheaderText(conditions.size(), constraints.get(0).getOperator().toString()));
    constraintSummary1.conditions().shouldHaveSize(conditions.size());

    constraintSummary1.condition(0).shouldHave(text("Age older than 2 Years"));

    ConstraintSection.ConstraintSummary constraintSummary2 = constraintSection.constraintSummary(1);
    constraintSummary2.name().shouldHave(text(constraints.get(1).getName()));

    conditions = constraints.get(1).getConditions();
    constraintSummary2.subheader()
        .shouldHave(ConstraintSection.ConstraintSummary
            .subheaderText(conditions.size(), constraints.get(1).getOperator().toString()));
    constraintSummary2.conditions().shouldHaveSize(conditions.size());

    constraintSummary2.condition(0).shouldHave(text("License Threat Group is my LTG"));
    constraintSummary2.condition(1).shouldHave(text("Label is my Label"));

    ConstraintSection.ConstraintSummary constraintSummary3 = constraintSection.constraintSummary(2);
    constraintSummary3.name().shouldHave(text(constraints.get(2).getName()));

    conditions = constraints.get(2).getConditions();
    constraintSummary3.subheader()
        .shouldHave(ConstraintSection.ConstraintSummary
            .subheaderText(conditions.size(), constraints.get(2).getOperator().toString()));
    constraintSummary3.conditions().shouldHaveSize(conditions.size());

    constraintSummary3.condition(0).shouldHave(text("Relative Popularity (Percentage) less than 50"));
    constraintSummary3.condition(1).shouldHave(text("Coordinates (GAV) do not match blah:blah:blah"));

    // Test Editing
    // TODO: CLM-5806 - Add Constraints Section
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

    PolicyEditorPage.saveButton().shouldHave(DISABLED_CLASS);
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
    PolicyEditorPage.saveButton().shouldHave(DISABLED_CLASS);
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
