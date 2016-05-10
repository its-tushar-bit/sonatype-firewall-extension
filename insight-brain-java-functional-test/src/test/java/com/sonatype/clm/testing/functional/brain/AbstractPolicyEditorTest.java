/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.Arrays;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ActionsSection;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.ConstraintSection;
import com.sonatype.clm.testing.functional.elements.ConstraintSection.ConstraintEditSection;
import com.sonatype.clm.testing.functional.elements.ConstraintSection.ConstraintEditSection.AgeConditionEditSection;
import com.sonatype.clm.testing.functional.elements.DeleteModal;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.NotificationsSection;
import com.sonatype.clm.testing.functional.elements.NotificationsSection.AddNotificationItem;
import com.sonatype.clm.testing.functional.elements.PopoverViolations;
import com.sonatype.clm.testing.functional.elements.SummarySection;
import com.sonatype.clm.testing.functional.elements.ThreatLevelSelector;
import com.sonatype.clm.testing.functional.elements.UnsavedModal;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.OrganizationManagementPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage.SummaryTile;
import com.sonatype.clm.testing.functional.pages.PolicyEditorPage;
import com.sonatype.clm.testing.functional.utils.ConditionUtils;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.notifications.Notification;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.tag.Tag;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.focused;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.back;
import static com.codeborne.selenide.Selenide.open;
import static com.sonatype.clm.testing.functional.elements.ActionsSection.activeClass;
import static com.sonatype.clm.testing.functional.elements.ActionsSection.warnClass;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.insight.brain.model.Color.dark_blue;
import static com.sonatype.insight.brain.model.Color.dark_red;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
    if (OwnerType.ORGANIZATION.equals(currentOwner.getType())) {
      tempEntity.newTag(currentOwner.getId());
    }
    open(OwnerSummaryPage.url(currentOwner.getType().toString(), currentOwner.getPublicId()));
    SummaryTile.addPolicyButton().click();

    assertNewPolicyStateIsCorrect();
    testCreatePolicy_summarySection();
    testCreatePolicy_inheritanceSection();
    testCreatePolicy_actionsSection();
    testCreatePolicy_notificationsSection();
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

    assertThat(newPolicy.getActions().get(Stage.ID_BUILD), is("warn"));

    assertThat(newPolicy.getNotifications().getUserNotifications(), hasSize(1));
    assertThat(newPolicy.getNotifications().getUserNotifications().get(0).getEmailAddress(), is("aaa@sonatype.com"));
    assertThat(newPolicy.getNotifications().getUserNotifications().get(0).getStageIds(),
        containsInAnyOrder(com.sonatype.clm.dto.model.policy.Stage.ID_BUILD));

    assertThat(newPolicy.getNotifications().getRoleNotifications(), hasSize(1));
    assertThat(newPolicy.getNotifications().getRoleNotifications().get(0).getStageIds(),
        containsInAnyOrder(Notification.CONTINUOUS_MONITORING));
    
    testCreatePolicy_navigatingAwayWithUnsavedData();
  }

  @Test
  public void testEditPolicy() {
    String ownerId = currentOwner.getId();
    Tag[] categories = createCategories(
        OwnerType.ORGANIZATION.equals(currentOwner.getType()) ? ownerId : currentOwner.getParentOwnerId());
    Policy policy = createPolicy(ownerId, categories);

    refresh();

    SummaryTile.localPolicy(policy.getName()).click();
    assertEditPolicyStateIsCorrect(policy, categories[0], categories[1], false);

    testEditPolicy_summarySection();
    testEditPolicy_inheritanceSection();
    testEditPolicy_constraintSection(policy);
    testEditPolicy_actionsSection(policy);
    testEditPolicy_notificationsSection(policy);
    testDeletePolicy(policyDAO.getById(policy.getId()));
  }

  @Test
  public void testDisabledPolicy() {
    String inheritedOwnerId = currentOwner.getParentOwnerId();
    Tag[] categories = createCategories(inheritedOwnerId);

    //add a new tag to the existing org as well, so that we can validate that viewing a disabled policy from a parent
    //doesn't include tags from the child
    if (OwnerType.ORGANIZATION.equals(currentOwner.getType())) {
      tempEntity.newTag(currentOwner.getId());
    }
    Policy policy = createPolicy(inheritedOwnerId, categories);

    refresh();

    SummaryTile.localPolicy(policy.getName()).click();
    assertEditPolicyStateIsCorrect(policy, categories[0], categories[1], true);
  }

  private void testCreatePolicy_navigatingAwayWithUnsavedData(){
    String editorUrl = WebDriverRunner.getWebDriver().getCurrentUrl();
    UnsavedModal unsavedModal = new UnsavedModal();

    ConstraintEditSection constraintEditor = PolicyEditorPage.constraintSection().constraintEditor(0);

    //make sure certain fields are making the editor dirty
    PolicyEditorPage.constraintsPill().click();
    constraintEditor.addConditiontButton().click();
    handleUnsavedChangesDialog(unsavedModal, editorUrl);
    constraintEditor.condition(1).deleteConditionButton().click();
    constraintEditor.condition(0).type().selectedItem().click();
    constraintEditor.condition(0).type().listItem(0).click();
    handleUnsavedChangesDialog(unsavedModal, editorUrl);
    constraintEditor.condition(0).type().selectedItem().click();
    constraintEditor.condition(0).type().listItem(9).click();
    PolicyEditorPage.actionsPill().click();
    //CLM-6366
    //proxyAction.twisty().click();
    //proxyAction.addNotification().email().val("someemail@email.com");
    //proxyAction.addNotification().addButton().click();
    //handleUnsavedChangesDialog(unsavedModal, editorUrl);
    //proxyAction.getNotification(1).deleteButton().click();

    // Assert no Modal appears when the editor is clean
    unsavedModal.shouldNotBe(visible);
    MainHeader.dashboardNavigationButton().shouldBe(visible, enabled).click();
    unsavedModal.shouldNotBe(visible);
    waitUntilUrl(DashboardPage.URL);
    DashboardPage.body().shouldBe(visible);

    back();
    waitUntilUrl(editorUrl);

    PolicyEditorPage.constraintSection().constraintEditor(0).ageCondition(0).value().age().val("10");

    handleUnsavedChangesDialog(unsavedModal, editorUrl);

    MainHeader.dashboardNavigationButton().click();
    unsavedModal.continueButton().shouldBe(visible).click();
    waitUntilUrl(DashboardPage.URL);
    DashboardPage.body().shouldBe(visible);

    back();
    waitUntilUrl(editorUrl);
    DashboardPage.body().shouldNotBe(visible);
  }

  private void handleUnsavedChangesDialog(UnsavedModal unsavedModal, String url) {
    // Assert Modal appears when the editor is dirty and continues to new page
    MainHeader.dashboardNavigationButton().click();
    unsavedModal.cancelButton().shouldBe(visible).click();
    waitUntilUrl(url);
    DashboardPage.body().shouldNotBe(visible);
  }

  private Tag[] createCategories(String ownerId) {
    Tag category1 = tempEntity.newTag(ownerId, "Cat_1", dark_blue);
    Tag category2 = tempEntity.newTag(ownerId, "Cat_2", dark_red);
    return new Tag[]{category1, category2};
  }

  private Policy createPolicy(String ownerId, Tag[] categories) {
    Policy policy = tempEntity.newPolicy(ownerId, "original name", 1);
    Constraint constraint1 = new Constraint(policy.getId() + "1", "First Constraint with One Condition", null);
    constraint1.addCondition(new Condition("AgeInDays", "older than", "730"));
    Constraint constraint2 = new Constraint(policy.getId() + "2", "Second Constraint with Two Conditions",
        LogicalOperator.AND);
    constraint2.addCondition(
        new Condition("License Threat Group", "is", tempEntity.newLicenseThreatGroup(ownerId, "my LTG", 5).getId()));
    constraint2.addCondition(new Condition("Label", "is", tempEntity.newLabel(ownerId, "my Label").getId()));
    Constraint constraint3 = new Constraint(policy.getId() + "3", "Third Constraint with Two Conditions",
        LogicalOperator.OR);
    constraint3.addCondition(new Condition("RelativePopularity", "<", "50"));
    constraint3.addCondition(new Condition("Coordinates", "do not match", "blah:blah:blah"));

    policy.setConstraints(Arrays.asList(constraint1, constraint2, constraint3));

    if (OwnerType.ORGANIZATION.equals(currentOwner.getType())) {
      tempEntity.newPolicyTag(policy.getId(), categories[0].getId());
    }

    policy.setAction(Stage.ID_DEVELOP, Action.ID_WARN);
    policy.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policy.getNotifications().add(
        new UserNotification("test@foo.com", Stage.ID_BUILD, Notification.CONTINUOUS_MONITORING));
    policy.getNotifications().add(new RoleNotification(new RoleDAO().getByName("Developer").getId(), Stage.ID_BUILD));

    tempEntity.newLicenseThreatGroup(currentOwner.getId(), "my LTG 2", 10);

    policyDAO.update(policy);
    return policy;
  }

  private void testEditPolicy_summarySection() {
    SummarySection summary = PolicyEditorPage.summarySection();
    summary.policyName().val("updated name");
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED).click();

    FormMask.seeAndWaitForDismissal();
    changeThreatLevel(6);
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED).click();

    FormMask.seeAndWaitForDismissal();

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
    PolicyEditorPage.endOfPagePill().click();
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
    PolicyEditorPage.endOfPagePill().click();
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
    PolicyEditorPage.endOfPagePill().click();
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
    PolicyEditorPage.endOfPagePill().click();
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
    PolicyEditorPage.endOfPagePill().click();
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    PolicyEditorPage.constraintsPill().click();

    constraints = policyDAO.getById(policy.getId()).getConstraints();
    assertThat(constraints.get(0).getConditions().size(), is(1));
  }

  private void testEditPolicy_notificationsSection(Policy policy) {
    PolicyEditorPage.notificationsPill().click();

    // add email notifications
    AddNotificationItem addNotification = NotificationsSection.addNotification();
    addNotification.addButton().shouldHave(DISABLED);
    addNotification.notificationType().selectedItem().shouldHave(text("Email"));
    addNotification.email().val("validation_test").shouldHave(cssClass("ng-invalid"));
    addNotification.addButton().shouldHave(DISABLED);
    addNotification.email().val("aaa@sonatype.com").shouldNotHave(cssClass("ng-invalid")).shouldBe(visible);
    addNotification.role().shouldNot(exist);
    addNotification.addButton().shouldNotHave(DISABLED).click();
    addNotification.addButton().shouldHave(DISABLED);
    addNotification.email().shouldBe(empty);
    // should be last
    NotificationsSection.notifications().shouldHaveSize(3);
    NotificationsSection.notifications().get(2).shouldHave(text("aaa@sonatype.com"));

    // duplicate email validation
    addNotification.email().val("aaa@sonatype.com").shouldHave(cssClass("ng-invalid"));
    addNotification.addButton().shouldHave(DISABLED);

    // add role notifications
    addNotification.notificationType().selectedItem().click();
    addNotification.notificationType().listItem(1).click();
    addNotification.email().shouldNot(exist);
    addNotification.role().shouldBe(visible).selectedItem().click();
    addNotification.role().listItems().findBy(text("Application Evaluator")).click();
    addNotification.addButton().shouldNotHave(DISABLED).click();
    addNotification.addButton().shouldHave(DISABLED);
    addNotification.role().selectedItem().shouldHave(text("-- Select Role --"));
    addNotification.role().listItems().findBy(text("Application Evaluator")).shouldNot(exist);
    // should be last
    NotificationsSection.notifications().get(3).shouldHave(text("Application Evaluator"));

    NotificationsSection.notifications()
        .shouldHave(texts("Developer", "test@foo.com", "aaa@sonatype.com", "Application Evaluator"));

    // delete one and save
    NotificationsSection.notificationFor("test@foo.com").deleteButton().click();
    NotificationsSection.notifications().shouldHaveSize(3);
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    NotificationsSection.notifications().shouldHaveSize(3);
    // "aaa@sonatype.com" should be first after save
    NotificationsSection.notifications().get(0).shouldHave(text("aaa@sonatype.com"));
    // "Application Evaluator" should be second after save
    NotificationsSection.notifications().get(1).shouldHave(text("Application Evaluator"));

    policy = policyDAO.getById(policy.getId());
    assertThat(policy.getNotifications().getRoleNotifications(), hasSize(2));
    assertThat(policy.getNotifications().getUserNotifications(), hasSize(1));
    Notifications notifications = policy.getNotifications().getApplicable(Stage.ID_BUILD, false);
    assertThat(notifications.getUserNotifications(), hasSize(0));
    assertThat(notifications.getRoleNotifications(), hasSize(1));

    // check 'operate' and 'continuousMonitoring' stages
    NotificationsSection.notificationFor("aaa@sonatype.com").operate().click();
    NotificationsSection.notificationFor("Application Evaluator").continuousMonitoring().click();
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    policy = policyDAO.getById(policy.getId());
    assertThat(policy.getNotifications().getApplicable(Stage.ID_OPERATE, false).getUserNotifications(), hasSize(1));
    assertThat(policy.getNotifications().getApplicable(Stage.ID_OPERATE, true).getRoleNotifications(), hasSize(1));

    // test "All roles are being notified." message
    addNotification.notificationType().selectedItem().click();
    addNotification.notificationType().listItem(1).click();
    addNotification.role().shouldBe(visible).selectedItem().click();
    addNotification.role().listItems().findBy(text("Owner")).click();
    addNotification.addButton().shouldNotHave(DISABLED).click();
    addNotification.role().shouldBe(visible).selectedItem().click();
    addNotification.role().listItems().findBy(text("Component Evaluator")).click();
    addNotification.addButton().shouldNotHave(DISABLED).click();
    addNotification.role().shouldHave(text("All roles are being notified."));
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    addNotification.role().shouldHave(text("All roles are being notified."));
    NotificationsSection.notificationFor("Owner").deleteButton().click();
    addNotification.role().shouldBe(visible).selectedItem().click();
    addNotification.role().listItems().get(0).shouldHave(text("Owner"));

    // test "No notifications configured" message
    NotificationsSection.notificationFor("Component Evaluator").deleteButton().click();
    NotificationsSection.notificationFor("aaa@sonatype.com").deleteButton().click();
    NotificationsSection.notificationFor("Developer").deleteButton().click();
    NotificationsSection.notificationFor("Application Evaluator").deleteButton().click();
    NotificationsSection.notifications().get(0).shouldHave(text("No notifications configured"));

  }

  private void testEditPolicy_actionsSection(Policy policy) {
    ActionsSection actionsTable = PolicyEditorPage.actionsSection();

    // Set proxy to warn, operate to fail
    actionsTable.proxy().warnRadio().click();
    actionsTable.proxy().warnRadio().shouldBe(selected);
    actionsTable.proxy().failRadio().shouldNotBe(selected);
    actionsTable.proxy().noActionRadio().shouldNotBe(selected);

    actionsTable.operate().noActionRadio().click();
    actionsTable.operate().noActionRadio().shouldBe(selected);
    actionsTable.operate().warnRadio().shouldNotBe(selected);
    actionsTable.operate().failRadio().shouldNotBe(selected);

    actionsTable.operate().failRadio().click();
    actionsTable.operate().failRadio().shouldBe(selected);
    actionsTable.operate().warnRadio().shouldNotBe(selected);
    actionsTable.operate().noActionRadio().shouldNotBe(selected);

    // Save and verify changes via backend
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    PolicyEditorPage.saveButton().shouldHave(DISABLED);

    policy = policyDAO.getById(policy.getId());
    assertThat(policy.getActions().get(Stage.ID_BUILD), is(Action.ID_FAIL));
    assertThat(policy.getActions().get(Stage.ID_DEVELOP), is(Action.ID_WARN));
    assertThat(policy.getActions().get(Stage.ID_PROXY), is(Action.ID_WARN));
    assertThat(policy.getActions().get(Stage.ID_OPERATE), is(Action.ID_FAIL));
    assertThat(policy.getActions().get(Stage.ID_STAGE_RELEASE), is(nullValue()));
    assertThat(policy.getActions().get(Stage.ID_RELEASE), is(nullValue()));
  }

  private void testDeletePolicy(Policy policy) {
    PolicyEditorPage.endOfPagePill().click();
    PolicyEditorPage.deleteButton().shouldBe(visible, enabled).click();

    DeleteModal.root().shouldBe(visible);
    DeleteModal.header().shouldHave(DeleteModal.headerText("Policy"));
    DeleteModal.body().shouldHave(DeleteModal.bodyText(policy.getName()));

    DeleteModal.continueButton().click();
    FormMask.seeAndWaitForDismissal();
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

  private void testCreatePolicy_actionsSection() {
    // header names
    ActionsSection actionsTable = PolicyEditorPage.actionsSection();
    actionsTable.headers().shouldHave(texts("PROXY", "DEVELOP", "BUILD", "STAGE", "RELEASE", "OPERATE"));

    // column hover
    ConditionUtils.shouldNotHave(actionsTable.build().cells(), activeClass());
    actionsTable.build().warnRadio().label().hover();
    ConditionUtils.shouldHave(actionsTable.build().cells(), activeClass());
    actionsTable.proxy().warnRadio().label().hover();
    ConditionUtils.shouldNotHave(actionsTable.build().cells(), activeClass());
    ConditionUtils.shouldHave(actionsTable.proxy().cells(), activeClass());

    // make an actual change and check that warn icon appears
    SelenideElement icon = actionsTable.build().warnRadio().label().$("i");
    icon.shouldNotHave(warnClass());
    actionsTable.build().warnRadio().click();
    icon.shouldHave(warnClass());
  }

  private void testCreatePolicy_notificationsSection() {
    PolicyEditorPage.notificationsPill().click();
    NotificationsSection notificationsSection = PolicyEditorPage.notificationsSection();
    notificationsSection.notifications().get(0).shouldHave(text("No notifications configured"));

    // add role notifications
    AddNotificationItem addNotification = NotificationsSection.addNotification();
    addNotification.notificationType().selectedItem().click();
    addNotification.notificationType().listItem(1).click();
    addNotification.role().shouldBe(visible).selectedItem().click();
    addNotification.role().listItems().findBy(text("Application Evaluator")).click();
    addNotification.addButton().shouldNotHave(DISABLED).click();

    // add email notifications
    addNotification.notificationType().selectedItem().click();
    addNotification.notificationType().listItem(0).click();
    addNotification.addButton().shouldHave(DISABLED);
    addNotification.email().val("aaa@sonatype.com").shouldNotHave(cssClass("ng-invalid")).shouldBe(visible);
    addNotification.addButton().shouldNotHave(DISABLED).click();
    addNotification.addButton().shouldHave(DISABLED);
    addNotification.email().shouldBe(empty);

    NotificationsSection.notifications().get(0).shouldHave(text("Application Evaluator"));
    NotificationsSection.notifications().get(1).shouldHave(text("aaa@sonatype.com"));

    // check stages
    NotificationsSection.notificationFor("aaa@sonatype.com").build().click();
    NotificationsSection.notificationFor("Application Evaluator").continuousMonitoring().click();
  }

  private void assertNewPolicyStateIsCorrect() {
    waitUntilUrl(PolicyEditorPage.urlToCreate(currentOwner.getType().toString(), currentOwner.getPublicId()));
    PolicyEditorPage.title().shouldHave(text("New"));

    assertNewPolicyStateIsCorrect_summarySection();
    assertNewPolicyStateIsCorrect_inheritanceSection();
    assertNewPolicyStateIsCorrect_constraintSection();
    assertNewPolicyStateIsCorrect_actionsSection();

    PolicyEditorPage.saveButton().shouldHave(DISABLED);
    PolicyEditorPage.deleteButton().shouldNot(exist);
  }

  private void assertNewPolicyStateIsCorrect_summarySection() {
    SummarySection summary = PolicyEditorPage.summarySection();
    summary.policyName().shouldBe(visible, empty, focused).shouldHave(CLM.INITIAL_VALUE);

    summary.policyName().val("$$$"); // invalid characters
    PopoverViolations.on(summary.policyName()).shouldShowInvalidCharactersError();

    summary.policyName().val("1  2"); // double spaces
    PopoverViolations.on(summary.policyName()).shouldShowInvalidSpacingError();

    summary.policyName().val("Acceptable Name");
    PopoverViolations.on(summary.policyName()).shouldNotExist();

    summary.policyName().clear();

    assertThreatLevelSelectorState(PolicyEditorPage.DEFAULT_THREAT_LEVEL, false);
  }

  private void assertNewPolicyStateIsCorrect_constraintSection() {
    SelenideElement constraintName = PolicyEditorPage.constraintSection().constraintEditor(0).name();

    constraintName.shouldBe(visible, empty).shouldHave(CLM.INITIAL_VALUE);
    PopoverViolations.on(constraintName).shouldNotExist();

    constraintName.val(" ");
    PopoverViolations.on(constraintName).shouldShowRequiredError();

    constraintName.val("$ Anything  !s Accept@ble :)   ");
    PopoverViolations.on(constraintName).shouldNotExist();

    constraintName.clear();
  }

  private void assertNewPolicyStateIsCorrect_actionsSection() {
    ActionsSection actionsSection = PolicyEditorPage.actionsSection();
    actionsSection.proxy().noActionRadio().shouldBe(enabled, selected);
    actionsSection.develop().noActionRadio().shouldBe(enabled, selected);
    actionsSection.build().noActionRadio().shouldBe(enabled, selected);
    actionsSection.stageRelease().noActionRadio().shouldBe(enabled, selected);
    actionsSection.release().noActionRadio().shouldBe(enabled, selected);
    actionsSection.operate().noActionRadio().shouldBe(enabled, selected);
  }

  private void assertEditPolicyStateIsCorrect(Policy policy, Tag category1, Tag category2, boolean isReadOnly) {
    waitUntilUrl(
        PolicyEditorPage.urlToEdit(currentOwner.getType().toString(), currentOwner.getPublicId(), policy.getId()));
    PolicyEditorPage.title().shouldHave(text(isReadOnly ? "View" : "Edit"));

    assertEditPolicyStateIsCorrect_summarySection(policy, isReadOnly);
    assertEditPolicyStateIsCorrect_inheritanceSection(category1, category2, isReadOnly);
    assertEditPolicyStateIsCorrect_actionsSection(isReadOnly);
    assertEditPolicyStateIsCorrect_notificationsSection(isReadOnly);
    PolicyEditorPage.saveButton().shouldHave(DISABLED);
    PolicyEditorPage.deleteButton().shouldBe(visible, isReadOnly ? disabled : enabled);
  }

  private void assertEditPolicyStateIsCorrect_summarySection(Policy policy, boolean isReadOnly) {
    SummarySection summary = PolicyEditorPage.summarySection();
    summary.policyName().shouldBe(visible, isReadOnly ? disabled : enabled).shouldHave(CLM.INITIAL_VALUE)
        .shouldHave(value(policy.getName()));
    assertThreatLevelSelectorState(policy.getThreatLevel(), isReadOnly);
  }

  private void assertEditPolicyStateIsCorrect_actionsSection(boolean isReadOnly) {
    PolicyEditorPage.actionsPill().click();

    com.codeborne.selenide.Condition disabledOrEnabled = isReadOnly ? disabled : enabled;
    ActionsSection actionsTable = PolicyEditorPage.actionsSection();

    // Policy actions for Developer and Build are set to Warn and Fail, respectively.
    ActionsSection.Stage develop = actionsTable.develop();
    develop.failRadio().input().shouldBe(visible, disabledOrEnabled).shouldNotBe(selected);
    develop.warnRadio().input().shouldBe(selected, visible, disabledOrEnabled);
    develop.noActionRadio().input().shouldBe(visible, disabledOrEnabled).shouldNotBe(selected);

    actionsTable.build().failRadio().shouldBe(selected);

    // The rest of the stages should have no-action selected
    actionsTable.proxy().noActionRadio().input().shouldBe(selected);
    actionsTable.operate().noActionRadio().input().shouldBe(selected);
    actionsTable.release().noActionRadio().input().shouldBe(selected);
    actionsTable.stageRelease().noActionRadio().input().shouldBe(selected);
  }

  private void assertEditPolicyStateIsCorrect_notificationsSection(final boolean isReadOnly) {
    NotificationsSection notifications = PolicyEditorPage.notificationsSection();

    AddNotificationItem addNotificationItem = notifications.addNotification();

    if (isReadOnly) {
      addNotificationItem.notificationType().shouldHave(CLM.DISABLED);
      addNotificationItem.email().shouldBe(disabled);
    }
    else {
      addNotificationItem.notificationType().shouldNotHave(CLM.DISABLED);
      addNotificationItem.notificationType().selectedItem().shouldBe(visible).shouldHave(text("Email"));
      addNotificationItem.email().shouldBe(enabled);
    }
    addNotificationItem.role().shouldNot(exist);
    addNotificationItem.addButton().shouldBe(disabled);

    notifications.notifications().shouldHaveSize(2).shouldHave(texts("Developer", "test@foo.com"));
    notifications.notificationFor("Developer").build().shouldBe(selected);
    notifications.notificationFor("test@foo.com").build().shouldBe(selected);
    notifications.notificationFor("test@foo.com").continuousMonitoring().shouldBe(selected);
  }

  private void assertThreatLevelSelectorState(int selectedThreatLevel, boolean isReadOnly) {
    ThreatLevelSelector.root().shouldBe(visible);
    if (isReadOnly) {
      ThreatLevelSelector.caretButton().shouldBe(visible).shouldHave(DISABLED);
      ThreatLevelSelector.threatLevelList().shouldNotBe(visible);
    }
    else {
      ThreatLevelSelector.caretButton().shouldBe(visible, enabled).click();
      ThreatLevelSelector.threatLevelList().shouldBe(visible);

      ThreatLevelSelector.threatLevelListItems().shouldHaveSize(ThreatLevelSelector.NUM_THREAT_LEVELS);

      for (int i = 0; i < ThreatLevelSelector.NUM_THREAT_LEVELS; i++) {
        ThreatLevelSelector.threatLevelListItem(i).shouldBe(visible).shouldHave(text(Integer.toString(10 - i)));
      }

      ThreatLevelSelector.selectedThreatLevel().shouldBe(visible, text(Integer.toString(selectedThreatLevel)))
          .click();
    }
  }


  private void changeThreatLevel(int threatLevel) {
    ThreatLevelSelector.caretButton().shouldBe(visible, enabled).click();
    ThreatLevelSelector.threatLevelListItem(10 - threatLevel).click();
    ThreatLevelSelector.selectedThreatLevel().shouldHave(text(String.valueOf(threatLevel)));
  }

  protected abstract void assertNewPolicyStateIsCorrect_inheritanceSection();

  protected abstract void testCreatePolicy_inheritanceSection();

  protected abstract void testEditPolicy_inheritanceSection();

  protected abstract void assertEditPolicyStateIsCorrect_inheritanceSection(Tag category1, Tag category2,
      boolean isReadOnly);
}
