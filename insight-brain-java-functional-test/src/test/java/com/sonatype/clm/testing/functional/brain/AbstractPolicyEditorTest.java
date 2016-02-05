/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.Arrays;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.NotifyAction;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ActionItemList;
import com.sonatype.clm.testing.functional.elements.ActionItemList.ActionItem;
import com.sonatype.clm.testing.functional.elements.ActionItemList.AddNotificationItem;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.ConstraintSection;
import com.sonatype.clm.testing.functional.elements.ConstraintSection.ConstraintEditSection;
import com.sonatype.clm.testing.functional.elements.ConstraintSection.ConstraintEditSection.AgeConditionEditSection;
import com.sonatype.clm.testing.functional.elements.DeleteModal;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.PopoverViolations;
import com.sonatype.clm.testing.functional.elements.SummarySection;
import com.sonatype.clm.testing.functional.elements.ThreatLevelSelector;
import com.sonatype.clm.testing.functional.pages.OrganizationManagementPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage.SummaryTile;
import com.sonatype.clm.testing.functional.pages.PolicyEditorPage;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.tag.Tag;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.open;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.insight.brain.model.Color.blue;
import static com.sonatype.insight.brain.model.Color.red;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.notNullValue;
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
    testCreatePolicy_summarySection();
    testCreatePolicy_actionsNotificationsSection();
    testCreatePolicy_constraintSection();
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();

    Policy newPolicy = null;
    for (Policy p : policyDAO.getByOwnerId(currentOwner.getId())) {
      if (p.getName().equals("New Policy")) {
        newPolicy = p;
        break;
      }
    }

    assertThat(newPolicy, is(notNullValue()));
    assertThat(newPolicy.getConstraints(), is(notNullValue()));
    assertThat(newPolicy.getConstraints().size(), is(1));
    Constraint constraint = newPolicy.getConstraints().get(0);
    assertThat(constraint.getName(), is("New Constraint"));
    assertThat(constraint.getOperator(), is(LogicalOperator.OR));
    assertThat(constraint.getConditions().size(), is(1));
    Condition condition = constraint.getConditions().get(0);
    assertThat(condition.getConditionTypeId(), is("AgeInDays"));
    assertThat(condition.getOperator(), is("older than"));
    assertThat(condition.getValue(), is(Integer.toString(3 * 365)));

    List<Action> buildActions = newPolicy.getActions(Stage.ID_BUILD);
    assertThat(buildActions, hasSize(1));
    Action testEmail = buildActions.get(0);
    assertThat(testEmail.getActionTypeId(), is(Action.ID_NOTIFY));
    assertThat(testEmail.getTarget(), is("test@sonatype.com"));

    List<NotifyAction> monitorActions = newPolicy.getMonitorNotifyActions();
    assertThat(monitorActions, hasSize(1));
    Action devRole = monitorActions.get(0);
    assertThat(devRole.getActionTypeId(), is(Action.ID_NOTIFY));
    assertThat(devRole.getTargetType(), is("role"));
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

    policy.addAction(Stage.ID_DEVELOP, new Action(Action.ID_WARN));
    policy.addAction(Stage.ID_BUILD, new Action(Action.ID_FAIL));
    policy.addAction(Stage.ID_BUILD, new Action(Action.ID_NOTIFY, "test@foo.com"));
    Action notifyDeveloperAction = new Action(Action.ID_NOTIFY);
    notifyDeveloperAction.setTargetType("role");
    notifyDeveloperAction.setTarget(new RoleDAO().getByName("Developer").getId());
    policy.addAction(Stage.ID_BUILD, notifyDeveloperAction);
    policy.addMonitorNotifyAction(new NotifyAction("test@foo.com", null));

    tempEntity.newLicenseThreatGroup(currentOwner.getId(), "my LTG 2", 10);

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
    testEditPolicy_constraintSection(policy);
    testEditPolicy_actionsNotificationsSection(policy);
    testDeletePolicy(policyDAO.getById(policy.getId()));
  }

  private void testEditPolicy_summarySection() {
    SummarySection summary = PolicyEditorPage.summarySection();
    summary.policyName().val("updated name");
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED).click();

    FormMask.seeAndWaitForDismissal();
    changeThreatLevel(6);
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED).click();

    refresh();

    PolicyEditorPage.title().shouldHave(text("Edit"));
    summary.policyName().shouldBe(visible).shouldHave(value("updated name"));
    ThreatLevelSelector.selectedThreatLevel().shouldBe(text("6"));
    PolicyEditorPage.saveButton().shouldHave(DISABLED);
  }

  private void testEditPolicy_constraintSection(Policy policy) {
    testEditPolicy_constraintSection_summaries(policy);
    testEditPolicy_constraintSection_editors(policy);
  }

  private void testEditPolicy_constraintSection_summaries(Policy policy) {
    List<Constraint> constraints = policy.getConstraints();
    ConstraintSection constraintSection = PolicyEditorPage.constraintSection();
    constraintSection.addConstraintButton().shouldBe(visible, enabled);
    constraintSection.constraintSummaries().shouldHaveSize(constraints.size());

    ConstraintSection.ConstraintSummary constraintSummary1 = constraintSection.constraintSummary(0);
    constraintSummary1.name().shouldHave(text(constraints.get(0).getName()));

    List<Condition> conditions = constraints.get(0).getConditions();
    constraintSummary1.subheader()
        .shouldHave(ConstraintSection.ConstraintSummary
            .subheaderText(conditions.size(), constraints.get(0).getOperator().toString()));
    constraintSummary1.conditions().shouldHaveSize(conditions.size());
    constraintSummary1.deleteConstraintButton().shouldBe(visible, enabled);
    constraintSummary1.editConstraintButton().shouldBe(visible, enabled);

    constraintSummary1.condition(0).shouldHave(text("Age older than 2 Years"));

    ConstraintSection.ConstraintSummary constraintSummary2 = constraintSection.constraintSummary(1);
    constraintSummary2.name().shouldHave(text(constraints.get(1).getName()));

    conditions = constraints.get(1).getConditions();
    constraintSummary2.subheader()
        .shouldHave(ConstraintSection.ConstraintSummary
            .subheaderText(conditions.size(), constraints.get(1).getOperator().toString()));
    constraintSummary2.conditions().shouldHaveSize(conditions.size());
    constraintSummary2.deleteConstraintButton().shouldBe(visible, enabled);
    constraintSummary2.editConstraintButton().shouldBe(visible, enabled);

    constraintSummary2.condition(0).shouldHave(text("License Threat Group is my LTG"));
    constraintSummary2.condition(1).shouldHave(text("Label is my Label"));

    ConstraintSection.ConstraintSummary constraintSummary3 = constraintSection.constraintSummary(2);
    constraintSummary3.name().shouldHave(text(constraints.get(2).getName()));

    conditions = constraints.get(2).getConditions();
    constraintSummary3.subheader()
        .shouldHave(ConstraintSection.ConstraintSummary
            .subheaderText(conditions.size(), constraints.get(2).getOperator().toString()));
    constraintSummary3.conditions().shouldHaveSize(conditions.size());
    constraintSummary3.deleteConstraintButton().shouldBe(visible, enabled);
    constraintSummary3.editConstraintButton().shouldBe(visible, enabled);

    constraintSummary3.condition(0).shouldHave(text("Relative Popularity (Percentage) less than 50"));
    constraintSummary3.condition(1).shouldHave(text("Coordinates (GAV) do not match blah:blah:blah"));
  }

  private void testEditPolicy_constraintSection_editors(Policy policy) {
    List<Constraint> constraints = policy.getConstraints();
    ConstraintSection constraintSection = PolicyEditorPage.constraintSection();

    constraintSection.constraintEditors().shouldHaveSize(0);
    constraintSection.constraintSummary(0).editConstraintButton().shouldBe(visible, enabled).click();
    constraintSection.constraintEditors().shouldHaveSize(1);

    ConstraintEditSection constraintEdit = constraintSection.constraintEditor(0);

    PolicyEditorPage.saveButton().shouldHave(DISABLED);
    constraintEdit.operator().selectedItem().shouldHave(text("all"));
    constraintEdit.name().shouldHave(value(constraints.get(0).getName())).val("New Constraint Name");
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();

    policy = policyDAO.getById(policy.getId());
    assertThat(policy.getConstraints().get(0).getName(), is("New Constraint Name"));

    constraintEdit.conditions().shouldHaveSize(1);
    PolicyEditorPage.saveButton().shouldHave(DISABLED);
    constraintEdit.ageCondition(0).deleteConditionButton().shouldBe(visible, disabled);
    constraintEdit.ageCondition(0).value().age().shouldHave(value("2")).val("3");
    constraintEdit.ageCondition(0).value().modifier().selectedItem().shouldHave(text("Years")).click();
    constraintEdit.ageCondition(0).value().modifier().listItem(1).shouldHave(text("Months")).click();
    constraintEdit.ageCondition(0).operator().selectedItem().shouldHave(text("older than")).click();
    constraintEdit.ageCondition(0).operator().listItem(1).shouldHave(text("younger than")).click();
    PolicyEditorPage.createPill().click();
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    PolicyEditorPage.constraintsPill().click();

    Condition updatedAgeCondition = policyDAO.getById(policy.getId()).getConstraints().get(0).getConditions().get(0);
    assertThat(updatedAgeCondition.getConditionTypeId(), is("AgeInDays"));
    assertThat(updatedAgeCondition.getValue(), is(Integer.toString(3 * 30)));
    assertThat(updatedAgeCondition.getOperator(), is("younger than"));

    PolicyEditorPage.saveButton().shouldHave(DISABLED);
    constraintEdit.addConditiontButton().shouldBe(visible, enabled).click();
    constraintEdit.conditions().shouldHaveSize(2);
    constraintEdit.condition(1).type().selectedItem().shouldHave(text("Age")).click();
    constraintEdit.condition(1).type().listItem(3).shouldHave(text("License Threat Group")).click();
    constraintEdit.dropdownCondition(1).operator().selectedItem().shouldHave(text("is")).click();
    constraintEdit.dropdownCondition(1).operator().listItem(1).shouldHave(text("is not")).click();
    constraintEdit.dropdownCondition(1).value().selectedItem().shouldHave(text("my LTG")).click();
    constraintEdit.dropdownCondition(1).value().listItem(1).shouldHave(text("my LTG 2")).click();
    PolicyEditorPage.createPill().click();
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    PolicyEditorPage.constraintsPill().click();

    constraints = policyDAO.getById(policy.getId()).getConstraints();
    assertThat(constraints.get(0).getConditions().size(), is(2));

    Condition ltgCondition = constraints.get(0).getConditions().get(1);
    assertThat(ltgCondition.getConditionTypeId(), is("License Threat Group"));
    assertThat(ltgCondition.getValue(),
        is(new LicenseThreatGroupDAO().getByOwnerIdAndName(currentOwner.getId(), "my LTG 2").getId()));
    assertThat(ltgCondition.getOperator(), is("is not"));

    PolicyEditorPage.saveButton().shouldHave(DISABLED);
    constraintEdit.addConditiontButton().shouldBe(visible, enabled).click();
    constraintEdit.conditions().shouldHaveSize(3);
    constraintEdit.condition(2).type().selectedItem().shouldHave(text("Age")).click();
    constraintEdit.condition(2).type().listItem(11).shouldHave(text("Coordinates (GAV)")).click();
    constraintEdit.inputCondition(2).operator().selectedItem().shouldHave(text("match")).click();
    constraintEdit.inputCondition(2).operator().listItem(1).shouldHave(text("do not match")).click();
    constraintEdit.inputCondition(2).value().shouldBe(empty).val("com.eclipse.*");
    PolicyEditorPage.createPill().click();
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    PolicyEditorPage.constraintsPill().click();

    constraints = policyDAO.getById(policy.getId()).getConstraints();
    assertThat(constraints.get(0).getConditions().size(), is(3));

    Condition coordinatesCondition = constraints.get(0).getConditions().get(2);
    assertThat(coordinatesCondition.getConditionTypeId(), is("Coordinates"));
    assertThat(coordinatesCondition.getValue(), is("com.eclipse.*"));
    assertThat(coordinatesCondition.getOperator(), is("do not match"));

    PolicyEditorPage.saveButton().shouldHave(DISABLED);
    constraintEdit.condition(2).type().selectedItem().shouldHave(text("Coordinates (GAV)")).click();
    constraintEdit.condition(2).type().listItem(5).shouldHave(text("Security Vulnerability")).click();
    constraintEdit.condition(2).operator().selectedItem().shouldHave(text("present")).click();
    constraintEdit.condition(2).operator().listItem(1).shouldHave(text("absent")).click();
    PolicyEditorPage.createPill().click();
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    PolicyEditorPage.constraintsPill().click();

    constraints = policyDAO.getById(policy.getId()).getConstraints();
    assertThat(constraints.get(0).getConditions().size(), is(3));

    Condition securityVulnerabilityCondition = constraints.get(0).getConditions().get(2);
    assertThat(securityVulnerabilityCondition.getConditionTypeId(), is("SecurityVulnerability"));
    assertThat(securityVulnerabilityCondition.getValue(), isEmptyOrNullString());
    assertThat(securityVulnerabilityCondition.getOperator(), is("absent"));

    constraintEdit.condition(0).deleteConditionButton().shouldBe(visible, enabled);
    constraintEdit.condition(1).deleteConditionButton().shouldBe(visible, enabled);
    constraintEdit.condition(2).deleteConditionButton().shouldBe(visible, enabled).click();
    constraintEdit.conditions().shouldHaveSize(2);

    constraintEdit.condition(1).deleteConditionButton().shouldBe(visible, enabled);
    constraintEdit.condition(0).deleteConditionButton().shouldBe(visible, enabled).click();
    constraintEdit.conditions().shouldHaveSize(1);

    constraintEdit.condition(0).deleteConditionButton().shouldBe(visible, disabled);
    PolicyEditorPage.createPill().click();
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    PolicyEditorPage.constraintsPill().click();

    constraints = policyDAO.getById(policy.getId()).getConstraints();
    assertThat(constraints.get(0).getConditions().size(), is(1));
  }

  private void testEditPolicy_actionsNotificationsSection(Policy policy) {
    testEditPolicy_actionsNotificationsSection_stageActions(policy);
    testEditPolicy_actionsNotificationsSection_notifications(policy);
    testEditPolicy_actionsNotificationsSection_monitoring(policy);
  }

  private void testEditPolicy_actionsNotificationsSection_notifications(Policy policy) {
    PolicyEditorPage.actionsAndNotificationsPill().click();
    ActionItemList actionItemList = PolicyEditorPage.actionsNotificationsSection().actionItemList();

    actionItemList.build().twisty().shouldHave(ActionItem.EXPANDED).click();
    actionItemList.build().twisty().shouldHave(ActionItem.COLLAPSED);

    PolicyEditorPage.saveButton().shouldHave(DISABLED);

    // test add notifications
    AddNotificationItem buildNotification = actionItemList.build().addNotification();
    buildNotification.addButton().shouldHave(DISABLED);
    buildNotification.notificationType().selectedItem().shouldHave(text("Email"));
    buildNotification.email().val("test@sonatype.com").shouldBe(visible);
    buildNotification.role().root().shouldNot(exist);
    buildNotification.addButton().shouldNotHave(DISABLED).click();
    buildNotification.addButton().shouldHave(DISABLED);
    buildNotification.email().shouldBe(empty);

    buildNotification.notificationType().selectedItem().click();
    buildNotification.notificationType().listItem(1).click();
    buildNotification.email().shouldNot(exist);
    buildNotification.role().root().shouldBe(visible);
    buildNotification.role().selectedItem().click();
    buildNotification.role().listItems().findBy(text("Application Evaluator")).click();
    buildNotification.addButton().shouldNotHave(DISABLED).click();
    buildNotification.addButton().shouldHave(DISABLED);
    buildNotification.role().selectedItem().shouldHave(text("None Selected"));

    actionItemList.build().notifications().shouldHaveSize(4);
    actionItemList.build().notifications().shouldHave(
        texts("test@foo.com", "Developer", "test@sonatype.com", "Application Evaluator"));

    actionItemList.build().getNotificationByName("test@foo.com").deleteButton().click();
    actionItemList.build().notifications().shouldHaveSize(3);
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    actionItemList.build().notificationCount().shouldHave(text("3"));

    actionItemList.build().twisty().shouldHave(ActionItem.COLLAPSED).click();
    actionItemList.build().twisty().shouldHave(ActionItem.EXPANDED);

    policy = policyDAO.getById(policy.getId());
    List<Action> actions = policy.getActions(Stage.ID_BUILD);
    assertThat(actions, hasSize(4)); // first is 'Fail'
    Action roleAction = actions.get(1);
    assertThat(roleAction.getActionTypeId(), is(Action.ID_NOTIFY));
    assertThat(roleAction.getTargetType(), is("role"));
    Action testEmail = actions.get(2);
    assertThat(testEmail.getActionTypeId(), is(Action.ID_NOTIFY));
    assertThat(testEmail.getTarget(), is("test@sonatype.com"));
    Action devRole = actions.get(3);
    assertThat(devRole.getActionTypeId(), is(Action.ID_NOTIFY));
    assertThat(devRole.getTargetType(), is("role"));
  }

  private void testEditPolicy_actionsNotificationsSection_monitoring(Policy policy) {
    PolicyEditorPage.actionsAndNotificationsPill().click();
    ActionItemList actionItemList = PolicyEditorPage.actionsNotificationsSection().actionItemList();

    actionItemList.continuousMonitoring().twisty().shouldHave(ActionItem.EXPANDED).click();
    actionItemList.continuousMonitoring().twisty().shouldHave(ActionItem.COLLAPSED);

    PolicyEditorPage.saveButton().shouldHave(DISABLED);

    // test add notifications
    AddNotificationItem monitoringNotification = actionItemList.continuousMonitoring().addNotification();

    monitoringNotification.addButton().shouldHave(DISABLED);
    monitoringNotification.notificationType().selectedItem().shouldHave(text("Email"));
    monitoringNotification.email().val("test@sonatype.com").shouldBe(visible);
    monitoringNotification.role().root().shouldNot(exist);
    monitoringNotification.addButton().shouldNotHave(DISABLED).click();
    monitoringNotification.addButton().shouldHave(DISABLED);
    monitoringNotification.email().shouldBe(empty);

    monitoringNotification.notificationType().selectedItem().click();
    monitoringNotification.notificationType().listItem(1).click();
    monitoringNotification.email().shouldNot(exist);
    monitoringNotification.role().root().shouldBe(visible);
    monitoringNotification.role().selectedItem().click();
    monitoringNotification.role().listItems().findBy(text("Application Evaluator")).click();
    monitoringNotification.addButton().shouldNotHave(DISABLED).click();
    monitoringNotification.addButton().shouldHave(DISABLED);
    monitoringNotification.role().selectedItem().shouldHave(text("None Selected"));

    actionItemList.continuousMonitoring().notifications().shouldHaveSize(3);
    actionItemList.continuousMonitoring().notifications().shouldHave(
        texts("test@foo.com", "test@sonatype.com", "Application Evaluator"));

    actionItemList.continuousMonitoring().getNotificationByName("test@foo.com").deleteButton().click();
    actionItemList.continuousMonitoring().notifications().shouldHaveSize(2);
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    actionItemList.continuousMonitoring().notificationCount().shouldHave(text("2"));
    actionItemList.continuousMonitoring().twisty().shouldHave(ActionItem.COLLAPSED).click();
    actionItemList.continuousMonitoring().twisty().shouldHave(ActionItem.EXPANDED);

    policy = policyDAO.getById(policy.getId());
    List<NotifyAction> actions = policy.getMonitorNotifyActions();
    assertThat(actions, hasSize(2));

    Action testEmail = actions.get(0);
    assertThat(testEmail.getActionTypeId(), is(Action.ID_NOTIFY));
    assertThat(testEmail.getTarget(), is("test@sonatype.com"));
    Action devRole = actions.get(1);
    assertThat(devRole.getActionTypeId(), is(Action.ID_NOTIFY));
    assertThat(devRole.getTargetType(), is("role"));
  }

  private void testEditPolicy_actionsNotificationsSection_stageActions(Policy policy) {
    ActionItemList actionItemList = PolicyEditorPage.actionsNotificationsSection().actionItemList();
    assertActionItemListNames(actionItemList);
    assertActionItemListHasBuildNotifications(actionItemList);
    PolicyEditorPage.saveButton().shouldHave(DISABLED);

    // Policy actions for Developer and Build are set to Warn and Fail, respectively.
    actionItemList.develop().failRadio().shouldNotBe(selected);
    actionItemList.develop().warnRadio().shouldBe(selected);
    actionItemList.develop().noActionRadio().shouldNotBe(selected);

    actionItemList.build().failRadio().shouldBe(selected);
    actionItemList.build().warnRadio().shouldNotBe(selected);
    actionItemList.build().noActionRadio().shouldNotBe(selected);


    // The rest of the stages should have no-action selected
    actionItemList.proxy().failRadio().shouldNotBe(selected);
    actionItemList.proxy().warnRadio().shouldNotBe(selected);
    actionItemList.proxy().noActionRadio().shouldBe(selected);
    actionItemList.operate().failRadio().shouldNotBe(selected);
    actionItemList.operate().warnRadio().shouldNotBe(selected);
    actionItemList.operate().noActionRadio().shouldBe(selected);
    actionItemList.release().failRadio().shouldNotBe(selected);
    actionItemList.release().warnRadio().shouldNotBe(selected);
    actionItemList.release().noActionRadio().shouldBe(selected);
    actionItemList.stageRelease().failRadio().shouldNotBe(selected);
    actionItemList.stageRelease().warnRadio().shouldNotBe(selected);
    actionItemList.stageRelease().noActionRadio().shouldBe(selected);

    // Set proxy to warn, operate to fail
    actionItemList.proxy().warnRadio().click();
    actionItemList.proxy().warnRadio().shouldBe(selected);
    actionItemList.proxy().failRadio().shouldNotBe(selected);
    actionItemList.proxy().noActionRadio().shouldNotBe(selected);

    actionItemList.operate().noActionRadio().click();
    actionItemList.operate().noActionRadio().shouldBe(selected);
    actionItemList.operate().warnRadio().shouldNotBe(selected);
    actionItemList.operate().failRadio().shouldNotBe(selected);

    actionItemList.operate().failRadio().click();
    actionItemList.operate().failRadio().shouldBe(selected);
    actionItemList.operate().warnRadio().shouldNotBe(selected);
    actionItemList.operate().noActionRadio().shouldNotBe(selected);

    // Save and verify changes via backend
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    PolicyEditorPage.saveButton().shouldHave(DISABLED);

    policy = policyDAO.getById(policy.getId());
    assertThat(policy.getActions(Stage.ID_BUILD).get(0).getActionTypeId(), is(Action.ID_FAIL));
    assertThat(policy.getActions(Stage.ID_DEVELOP).get(0).getActionTypeId(), is(Action.ID_WARN));
    assertThat(policy.getActions(Stage.ID_PROXY).get(0).getActionTypeId(), is(Action.ID_WARN));
    assertThat(policy.getActions(Stage.ID_OPERATE).get(0).getActionTypeId(), is(Action.ID_FAIL));
    assertThat(policy.getActions(Stage.ID_STAGE_RELEASE), is(nullValue()));
    assertThat(policy.getActions(Stage.ID_RELEASE), is(nullValue()));
  }

  private void testDeletePolicy(Policy policy) {
    PolicyEditorPage.createPill().click();
    PolicyEditorPage.deleteButton().shouldBe(visible, enabled).click();

    DeleteModal.root().shouldBe(visible);
    DeleteModal.header().shouldHave(DeleteModal.headerText("Policy"));
    DeleteModal.body().shouldHave(DeleteModal.bodyText(policy.getName()));

    DeleteModal.continueButton().click();
    DeleteModal.root().shouldNotBe(visible);

    assertNewPolicyStateIsCorrect();
    assertThat(policyDAO.getById(policy.getId()), is(nullValue()));
  }

  public void testCreatePolicy_summarySection() {
    SummarySection summary = PolicyEditorPage.summarySection();
    summary.policyName().val("New Policy");
    PolicyEditorPage.saveButton().shouldHave(DISABLED);

    changeThreatLevel(6);
    PolicyEditorPage.saveButton().shouldHave(DISABLED);
  }

  public void testCreatePolicy_constraintSection() {
    ConstraintSection constraintSection = PolicyEditorPage.constraintSection();
    constraintSection.addConstraintButton().shouldBe(visible, enabled);

    constraintSection.constraintEditors().shouldHaveSize(1);

    ConstraintEditSection newConstraint = constraintSection.constraintEditor(0);
    newConstraint.name().shouldBe(empty).val("New Constraint");
    PolicyEditorPage.saveButton().shouldHave(DISABLED);
    newConstraint.operator().selectedItem().shouldHave(text("any"));
    newConstraint.conditions().shouldHaveSize(1);

    AgeConditionEditSection ageCondition = newConstraint.ageCondition(0);
    ageCondition.deleteConditionButton().shouldBe(visible, disabled);
    ageCondition.type().selectedItem().shouldHave(text("Age"));
    ageCondition.operator().selectedItem().shouldHave(text("older than"));
    ageCondition.value().modifier().selectedItem().shouldHave(text("Years"));
    ageCondition.value().age().shouldBe(empty).val("3");
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);
  }

  private void testCreatePolicy_actionsNotificationsSection() {
    ActionItemList actionItemList = PolicyEditorPage.actionsNotificationsSection().actionItemList();
    assertActionItemListNames(actionItemList);
    assertActionItemListNoNotifications(actionItemList);

    actionItemList.build().twisty().shouldHave(ActionItem.EXPANDED).click();
    actionItemList.build().twisty().shouldHave(ActionItem.COLLAPSED);

    PolicyEditorPage.saveButton().shouldHave(DISABLED);

    AddNotificationItem buildNotification = actionItemList.build().addNotification();
    buildNotification.addButton().shouldHave(DISABLED);
    buildNotification.notificationType().selectedItem().shouldHave(text("Email"));
    buildNotification.email().val("test@sonatype.com").shouldBe(visible);
    buildNotification.role().root().shouldNot(exist);
    buildNotification.addButton().shouldNotHave(DISABLED).click();
    buildNotification.addButton().shouldHave(DISABLED);
    PolicyEditorPage.saveButton().shouldHave(DISABLED); // disabled because constraint has not been populated

    actionItemList.build().twisty().click();
    actionItemList.continuousMonitoring().twisty().shouldHave(ActionItem.EXPANDED).click();
    actionItemList.continuousMonitoring().twisty().shouldHave(ActionItem.COLLAPSED);

    AddNotificationItem monitoringNotification = actionItemList.continuousMonitoring().addNotification();
    monitoringNotification.addButton().shouldHave(DISABLED);
    monitoringNotification.notificationType().selectedItem().shouldHave(text("Email")).click();
    monitoringNotification.notificationType().listItem(1).click();
    monitoringNotification.email().shouldNot(exist);
    monitoringNotification.role().root().shouldBe(visible);
    monitoringNotification.role().selectedItem().click();
    monitoringNotification.role().listItems().findBy(text("Application Evaluator")).click();
    monitoringNotification.addButton().shouldNotHave(DISABLED).click();
    monitoringNotification.addButton().shouldHave(DISABLED);
  }

  private void assertNewPolicyStateIsCorrect() {
    waitUntilUrl(PolicyEditorPage.urlToCreate(currentOwner.getType().toString(), currentOwner.getPublicId()));
    PolicyEditorPage.title().shouldHave(text("New"));

    assertNewPolicyStateIsCorrect_summarySection();
    assertNewPolicyStateIsCorrect_inheritanceSection();

    PolicyEditorPage.saveButton().shouldHave(DISABLED);
    PolicyEditorPage.deleteButton().shouldNot(exist);
  }

  private void assertNewPolicyStateIsCorrect_summarySection() {
    SummarySection summary = PolicyEditorPage.summarySection();
    summary.policyName().shouldBe(visible, empty).shouldHave(CLM.INITIAL_VALUE);

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
    PolicyEditorPage.saveButton().shouldHave(DISABLED);
  }

  private void assertEditPolicyStateIsCorrect_summarySection(Policy policy) {
    SummarySection summary = PolicyEditorPage.summarySection();
    summary.policyName().shouldBe(visible).shouldHave(CLM.INITIAL_VALUE).shouldHave(value(policy.getName()));
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

  private void assertActionItemListNames(ActionItemList actionItemList) {
    actionItemList.proxy().name().shouldHave(text("Proxy"));
    actionItemList.develop().name().shouldHave(text("Develop"));
    actionItemList.build().name().shouldHave(text("Build"));
    actionItemList.stageRelease().name().shouldHave(text("Stage Release"));
    actionItemList.release().name().shouldHave(text("Release"));
    actionItemList.operate().name().shouldHave(text("Operate"));
    actionItemList.continuousMonitoring().name().shouldHave(text("Continuous Monitoring"));
  }

  private void assertActionItemListNoNotifications(ActionItemList actionItemList) {
    assertActionItemListNoNotifications_allButBuildAndMonitor(actionItemList);
    actionItemList.build().notificationCount().shouldHave(text("0"));
    actionItemList.continuousMonitoring().notificationCount().shouldHave(text("0"));
  }

  private void assertActionItemListHasBuildNotifications(ActionItemList actionItemList) {
    assertActionItemListNoNotifications_allButBuildAndMonitor(actionItemList);
    actionItemList.build().notificationCount().shouldHave(text("2"));
    actionItemList.continuousMonitoring().notificationCount().shouldHave(text("1"));
  }

  private void assertActionItemListNoNotifications_allButBuildAndMonitor(ActionItemList actionItemList) {
    actionItemList.proxy().notificationCount().shouldHave(text("0"));
    actionItemList.develop().notificationCount().shouldHave(text("0"));
    actionItemList.stageRelease().notificationCount().shouldHave(text("0"));
    actionItemList.release().notificationCount().shouldHave(text("0"));
    actionItemList.operate().notificationCount().shouldHave(text("0"));
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
