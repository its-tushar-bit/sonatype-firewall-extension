/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DeleteModal;
import com.sonatype.clm.testing.functional.elements.PopoverViolations;
import com.sonatype.clm.testing.functional.elements.ThreatLevelSelector;
import com.sonatype.clm.testing.functional.pages.OrganizationManagementPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage.SummaryTile;
import com.sonatype.clm.testing.functional.pages.PolicyEditorPage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;

import com.codeborne.selenide.WebDriverRunner;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.disabledClass;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class PolicyEditorTest
    extends AbstractFunctionalTest
{
  private Organization organization;

  private PolicyDAO policyDAO = new PolicyDAO();

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(OrganizationManagementPage.URL);
    loginAsAdmin();
  }

  @Before
  public void init() {
    organization = tempEntity.newOrganization();
    refreshOrOpen(OwnerSummaryPage.url("organization", organization.getId()));
  }

  @Test
  public void testCreatePolicy() {
    SummaryTile.addPolicyButton().click();

    assertNewPolicyStateIsCorrect();
    // TODO: Edit data and save needs CLM-5287 Policy editor - constraints section
  }

  @Test
  public void testEditPolicy() {
    Policy policy = tempEntity.newPolicy(organization.getId(), "original name", 1);
    refresh();

    SummaryTile.localPolicy(policy.getName()).click();
    assertEditPolicyStateIsCorrect(policy);

    testEditPolicy_summarySection();
    testDeletePolicy(policyDAO.getById(policy.getId()));
  }

  private void testEditPolicy_summarySection() {
    PolicyEditorPage.policyName().val("updated name");
    PolicyEditorPage.saveButton().shouldNotHave(disabledClass()).click();

    changeThreatLevel(6);
    PolicyEditorPage.saveButton().shouldNotHave(disabledClass()).click();

    refresh();

    PolicyEditorPage.title().shouldHave(text("Edit"));
    PolicyEditorPage.policyName().shouldBe(visible).shouldHave(value("updated name"));
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
    waitUntilUrl(PolicyEditorPage.urlToCreate(organization.getType().toString(), organization.getId()));
    PolicyEditorPage.title().shouldHave(text("New"));

    assertNewPolicyStateIsCorrect_summarySection();

    PolicyEditorPage.saveButton().shouldHave(disabledClass());
    PolicyEditorPage.deleteButton().shouldNot(exist);
  }

  private void assertNewPolicyStateIsCorrect_summarySection() {
    PolicyEditorPage.policyName().shouldBe(visible, empty).shouldHave(cssClass("initial-value"));

    PolicyEditorPage.policyName().val("$$$"); // invalid characters
    PopoverViolations.on(PolicyEditorPage.policyName()).shouldShowInvalidCharactersError();

    PolicyEditorPage.policyName().val("Acceptable Name");
    PopoverViolations.on(PolicyEditorPage.policyName()).shouldNotExist();

    PolicyEditorPage.policyName().clear();

    assertThreatLevelSelectorState(PolicyEditorPage.DEFAULT_THREAT_LEVEL);
  }

  private void assertEditPolicyStateIsCorrect(Policy policy) {
    waitUntilUrl(PolicyEditorPage.urlToEdit(organization.getType().toString(), organization.getId(), policy.getId()));
    PolicyEditorPage.title().shouldHave(text("Edit"));

    assertEditPolicyStateIsCorrect_summarySection(policy);
    PolicyEditorPage.saveButton().shouldHave(disabledClass());
  }

  private void assertEditPolicyStateIsCorrect_summarySection(Policy policy) {
    PolicyEditorPage.policyName().shouldBe(visible).shouldHave(cssClass("initial-value")).shouldHave(
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
}
