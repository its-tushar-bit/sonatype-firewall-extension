/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ActionsSection;
import com.sonatype.clm.testing.functional.elements.ConstraintSection;
import com.sonatype.clm.testing.functional.elements.ConstraintSection.ConstraintEditSection;
import com.sonatype.clm.testing.functional.elements.ConstraintSection.ConstraintEditSection.AgeConditionEditSection;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.NotificationsSection;
import com.sonatype.clm.testing.functional.elements.NotificationsSection.AddNotificationItem;
import com.sonatype.clm.testing.functional.elements.NxBreadcrumb;
import com.sonatype.clm.testing.functional.elements.NxDeleteModal;
import com.sonatype.clm.testing.functional.elements.PolicyInheritsToSection;
import com.sonatype.clm.testing.functional.elements.PolicyTileList;
import com.sonatype.clm.testing.functional.elements.PolicyTileList.PolicyTileListElement;
import com.sonatype.clm.testing.functional.elements.RepositoriesSummaryTile;
import com.sonatype.clm.testing.functional.elements.SummarySection;
import com.sonatype.clm.testing.functional.elements.ThreatDropdownSelector;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPageWithLimitedVisibility;
import com.sonatype.clm.testing.functional.pages.PolicyEditorPage;
import com.sonatype.clm.testing.functional.pages.RepositoriesSummaryPage;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.User;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryContainerPolicyEditorTest
    extends AbstractFunctionalTest
{
  private final RepositoryContainer repositoryContainer = RepositoryContainer.SINGLETON;

  private RoleDAO roleDAO;

  private OrganizationDAO organizationDAO;

  private PolicyDAO policyDAO;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(RepositoriesSummaryPage.url());
    loginAsAdmin();
  }

  @Before
  public void init() {
    roleDAO = lookup(RoleDAO.class);
    organizationDAO = lookup(OrganizationDAO.class);
    policyDAO = lookup(PolicyDAO.class);

    refreshOrOpen(RepositoriesSummaryPage.url());
  }

  @Test
  public void testQuarantineWarningOnCreatePolicy() {
    RepositoriesSummaryPage.policyTile().addPolicyButton().click();

    SummarySection summary = PolicyEditorPage.summarySection();
    summary.policyName().input().val("New Policy");

    assertActionsSectionIsInCorrectState();

    ThreatDropdownSelector.dropdownButton().shouldBe(visible, enabled).click();
    ThreatDropdownSelector.threatLevelListItem(1).shouldBe(visible).click();
    ThreatDropdownSelector.selectedThreatLabel().shouldHave(text(String.valueOf(9)));

    PolicyEditorPage.actionsSection().quarantineWarningMessage().shouldNotBe(visible);
    PolicyEditorPage.actionsSection().proxy().failRadio().click();
    PolicyEditorPage.actionsSection().quarantineWarningMessage().shouldBe(visible);

    ScrollUtil.awaitEndOfScrolling(PolicyEditorPage.actionsSection().quarantineWarningMessage().scrollIntoView(true));

    PolicyEditorPage.actionsSection().build().warnRadio().click();

    ConstraintSection constraintSection = PolicyEditorPage.constraintSection();
    ConstraintEditSection newConstraint = constraintSection.constraintEditor(0);
    newConstraint.name().shouldBe(empty).val("New Constraint");
    AgeConditionEditSection ageCondition = newConstraint.ageCondition(0);
    ageCondition.value().age().shouldBe(empty).val("3");

    eyesWatcher.eyesCheck("Repository Managers New Policy");

    PolicyEditorPage.savePolicy();
    Policy newPolicy = policyDAO.getByOwnerIdAndName(RepositoryContainer.REPOSITORY_CONTAINER_ID, "New Policy");
    PolicyEditorPage.actionsSection().quarantineWarningMessage().shouldNotBe(visible);
    assertThat(newPolicy).isNotNull();
    assertThat(newPolicy.getActions()).containsEntry(Stage.ID_PROXY, "fail");
  }

  @Test
  public void testCreateDuplicatedPolicyFails() {
    RepositoriesSummaryPage.policyTile().addPolicyButton().click();

    SummarySection summary = PolicyEditorPage.summarySection();
    summary.policyName().input().val("New Policy");

    assertActionsSectionIsInCorrectState();

    ThreatDropdownSelector.dropdownButton().shouldBe(visible, enabled).click();
    ThreatDropdownSelector.threatLevelListItem(1).shouldBe(visible).click();
    ThreatDropdownSelector.selectedThreatLabel().shouldHave(text(String.valueOf(9)));

    PolicyEditorPage.actionsSection().quarantineWarningMessage().shouldNotBe(visible);
    PolicyEditorPage.actionsSection().proxy().warnRadio().click();

    ScrollUtil.awaitEndOfScrolling(PolicyEditorPage.saveButton().scrollIntoView(true));

    PolicyEditorPage.actionsSection().build().warnRadio().click();

    ConstraintSection constraintSection = PolicyEditorPage.constraintSection();
    ConstraintEditSection newConstraint = constraintSection.constraintEditor(0);
    newConstraint.name().shouldBe(empty).val("New Constraint");
    AgeConditionEditSection ageCondition = newConstraint.ageCondition(0);
    ageCondition.value().age().shouldBe(empty).val("3");

    eyesWatcher.eyesCheck("Repository Managers New Policy");

    PolicyEditorPage.savePolicy();

    refresh();

    summary.policyName().input().val("New Policy");
    ThreatDropdownSelector.dropdownButton().shouldBe(visible, enabled).click();
    ThreatDropdownSelector.threatLevelListItem(1).shouldBe(visible).click();
    ThreatDropdownSelector.selectedThreatLabel().shouldHave(text(String.valueOf(9)));

    PolicyEditorPage.actionsSection().quarantineWarningMessage().shouldNotBe(visible);
    PolicyEditorPage.actionsSection().proxy().warnRadio().click();

    ScrollUtil.awaitEndOfScrolling(PolicyEditorPage.saveButton().scrollIntoView(true));

    PolicyEditorPage.actionsSection().build().warnRadio().click();

    constraintSection.constraintEditor(0);
    newConstraint.name().shouldBe(empty).val("New Constraint");
    newConstraint.ageCondition(0);
    ageCondition.value().age().shouldBe(empty).val("3");
    PolicyEditorPage.savePolicy();

    PolicyEditorPage.alert().shouldHave(text("There were validation errors"));
  }

  @Test
  public void testReturnsToCreatePolicyAfterRemovePolicy() {
    Policy policy = createPolicy();
    refresh();

    RepositoriesSummaryPage.policyTile().policyLists().shouldHave(size(2));
    PolicyTileList policyList = RepositoriesSummaryPage.policyTile().policyList(0);
    policyList.emptyDescriptor().shouldBe(hidden);
    policyList.ownerName().shouldBe(visible).shouldHave(text("Local"));
    PolicyTileListElement policyElement = policyList.row(1);
    policyElement.name().shouldBe(visible).shouldHave(text("original name"));
    policyElement.proxy().shouldBe(visible).shouldHave(text("fail"));
    policyElement.click();

    SummarySection summary = PolicyEditorPage.summarySection();
    ScrollUtil.awaitEndOfScrolling(PolicyEditorPage.deleteButton());
    PolicyEditorPage.deleteButton().shouldBe(visible, enabled).click();

    NxDeleteModal deleteModal = new NxDeleteModal("#policy-delete-modal");
    deleteModal.shouldBe(visible);
    deleteModal.header().shouldHave(text("Policy"));
    deleteModal.alertContent().shouldHave(text(policy.getName()));
    PolicyEditorPage.deleteConfirmationInput().shouldBe(visible).val("WRONG");
    PolicyEditorPage.deleteConfirmationError().shouldHave(text("Must type DELETE to confirm"));

    deleteModal.submitButton().click();
    PolicyEditorPage.deleteConfirmationFormError().shouldBe(visible).shouldHave(text("Required fields are missing"));

    PolicyEditorPage.deleteConfirmationInput().clear();
    PolicyEditorPage.deleteConfirmationInput().val("DELETE");
    PolicyEditorPage.deleteConfirmationError().shouldBe(hidden);

    deleteModal.submitButton().click();
    FormMask.seeAndWaitForDismissal();
    deleteModal.shouldBe(hidden);

    summary.policyName().input().shouldBe(empty);
    assertThat(policyDAO.getById(policy.getId())).isNull();
  }

  @Test
  public void testUpdatePolicy() {
    createPolicy();
    refresh();

    RepositoriesSummaryPage.policyTile().policyLists()
        .shouldHave(size(2)); // include inherited policies
    PolicyTileList policyList = RepositoriesSummaryPage.policyTile().policyList(0);
    policyList.row(1).click();

    SummarySection summary = PolicyEditorPage.summarySection();
    summary.policyName().input().val("Updated Policy");

    assertActionsSectionIsInCorrectState();

    ThreatDropdownSelector.dropdownButton().shouldBe(visible, enabled).click();
    ThreatDropdownSelector.threatLevelListItem(2).shouldBe(visible).click();
    ThreatDropdownSelector.selectedThreatLabel().shouldHave(text(String.valueOf(8)));

    ConstraintSection constraintSection = PolicyEditorPage.constraintSection();
    constraintSection.addConstraintButton().click();
    ConstraintEditSection newConstraint = constraintSection.constraintEditor(1);
    newConstraint.name().val("New Constraint");

    AgeConditionEditSection ageCondition = newConstraint.ageCondition(0);
    ageCondition.value().age().shouldBe(empty).val("1");

    eyesWatcher.eyesCheck("Repository Managers Edit Policy");

    PolicyEditorPage.savePolicy();

    Policy updatedPolicy =
        policyDAO.getByOwnerIdAndName(RepositoryContainer.REPOSITORY_CONTAINER_ID, "Updated Policy");
    assertThat(updatedPolicy).isNotNull();
    assertThat(updatedPolicy.getActions().size()).isEqualTo(1);
    assertThat(updatedPolicy.getActions().get(Stage.ID_PROXY)).isEqualTo("fail");
    assertThat(updatedPolicy.getThreatLevel()).isEqualTo(8);
    assertThat(updatedPolicy.getConstraints()).hasSize(2);
    testConstraint(updatedPolicy.getConstraints().get(0), "First Constraint with One Condition", "AgeInDays",
        "older than", "730");
    testConstraint(updatedPolicy.getConstraints().get(1), "New Constraint", "AgeInDays", "older than", "365");
  }

  @Test
  public void testCreatePolicyWithUserNotification() {
    RepositoriesSummaryPage.policyTile().addPolicyButton().click();

    SummarySection summary = PolicyEditorPage.summarySection();
    summary.policyName().input().val("New Policy");

    ThreatDropdownSelector.dropdownButton().shouldBe(visible, enabled).click();
    ThreatDropdownSelector.threatLevelListItem(1).shouldBe(visible).click();
    ThreatDropdownSelector.selectedThreatLabel().shouldHave(text(String.valueOf(9)));

    assertActionsSectionIsInCorrectState();
    PolicyEditorPage.actionsSection().quarantineWarningMessage().shouldNotBe(visible);
    PolicyEditorPage.actionsSection().proxy().failRadio().click();
    PolicyEditorPage.actionsSection().quarantineWarningMessage().shouldBe(visible);

    ScrollUtil.awaitEndOfScrolling(PolicyEditorPage.actionsSection().quarantineWarningMessage().scrollIntoView(true));

    PolicyEditorPage.actionsSection().build().warnRadio().click();

    ConstraintSection constraintSection = PolicyEditorPage.constraintSection();
    ConstraintEditSection newConstraint = constraintSection.constraintEditor(0);
    newConstraint.name().shouldBe(empty).val("New Constraint");
    AgeConditionEditSection ageCondition = newConstraint.ageCondition(0);
    ageCondition.value().age().shouldBe(empty).val("3");

    ScrollUtil.scrollIntoView(PolicyEditorPage.notificationsSection().header());

    AddNotificationItem addNotification = NotificationsSection.addNotification();
    addNotification.errorBox().shouldBe(hidden);

    // add email notifications
    addNotification.addButton().shouldBe(disabled);
    addNotification.notificationType().chooseOption("Email");
    addNotification.email().val("validation_test").shouldHave(attribute("aria-invalid"));
    addNotification.addButton().shouldBe(disabled);
    addNotification.email().val("aaa@sonatype.com").shouldNotHave(attribute("aria-invalid", "true")).shouldBe(visible);
    addNotification.role().shouldNot(exist);
    addNotification.addButton().shouldNotBe(disabled).click();
    addNotification.addButton().shouldBe(disabled);
    addNotification.email().shouldBe(empty);

    NotificationsSection.notifications().shouldHave(size(1));
    NotificationsSection.notifications().get(0).shouldHave(text("aaa@sonatype.com"));

    PolicyEditorPage.savePolicy();
    Policy newPolicy = policyDAO.getByOwnerIdAndName(RepositoryContainer.REPOSITORY_CONTAINER_ID, "New Policy");

    PolicyEditorPage.actionsSection().quarantineWarningMessage().shouldNotBe(visible);
    assertThat(newPolicy).isNotNull();
    assertThat(newPolicy.getNotifications().getUserNotifications()).hasSize(1);
    testUserNotification(newPolicy.getNotifications().getUserNotifications().get(0), "aaa@sonatype.com");
  }

  @Test
  public void testCreatePolicyWithRoleNotification() {
    RepositoriesSummaryPage.policyTile().addPolicyButton().click();

    SummarySection summary = PolicyEditorPage.summarySection();
    summary.policyName().input().val("New Policy");

    ThreatDropdownSelector.dropdownButton().shouldBe(visible, enabled).click();
    ThreatDropdownSelector.threatLevelListItem(1).shouldBe(visible).click();
    ThreatDropdownSelector.selectedThreatLabel().shouldHave(text(String.valueOf(9)));

    assertActionsSectionIsInCorrectState();
    PolicyEditorPage.actionsSection().quarantineWarningMessage().shouldNotBe(visible);
    PolicyEditorPage.actionsSection().proxy().failRadio().click();
    PolicyEditorPage.actionsSection().quarantineWarningMessage().shouldBe(visible);

    ScrollUtil.awaitEndOfScrolling(PolicyEditorPage.actionsSection().quarantineWarningMessage().scrollIntoView(true));

    PolicyEditorPage.actionsSection().build().warnRadio().click();

    ConstraintSection constraintSection = PolicyEditorPage.constraintSection();
    ConstraintEditSection newConstraint = constraintSection.constraintEditor(0);
    newConstraint.name().shouldBe(empty).val("New Constraint");
    AgeConditionEditSection ageCondition = newConstraint.ageCondition(0);
    ageCondition.value().age().shouldBe(empty).val("3");

    ScrollUtil.scrollIntoView(PolicyEditorPage.notificationsSection().header());

    AddNotificationItem addNotification = NotificationsSection.addNotification();
    addNotification.errorBox().shouldBe(hidden);

    // add role notifications
    addNotification.notificationType().chooseOption("Role");
    addNotification.email().shouldNot(exist);
    addNotification.role().shouldBe(visible).chooseOption("Policy Administrator");
    addNotification.addButton().shouldNotBe(disabled).click();
    addNotification.addButton().shouldBe(disabled);
    assertThat(addNotification.role().selectedItem().getText()).isEqualTo("-- Select Role --");
    addNotification.role().listItems().findBy(text("Policy Administrator")).shouldNot(exist);

    PolicyEditorPage.savePolicy();
    Policy newPolicy = policyDAO.getByOwnerIdAndName(RepositoryContainer.REPOSITORY_CONTAINER_ID, "New Policy");
    PolicyEditorPage.actionsSection().quarantineWarningMessage().shouldNotBe(visible);
    assertThat(newPolicy).isNotNull();
    assertThat(newPolicy.getNotifications().getRoleNotifications()).hasSize(1);

    String roleName = "Policy Administrator";
    RoleNotification roleNotification = new RoleNotification(roleDAO.getByName(roleName).getId(), roleName);
    testRoleNotification(newPolicy.getNotifications().getRoleNotifications().get(0), roleNotification.getRoleId());
  }

  @Test
  public void testBackButtonReturnsToRepositoriesSummary() {
    RepositoriesSummaryPage.policyTile().addPolicyButton().click();
    NxBreadcrumb breadcrumb = new NxBreadcrumb();
    breadcrumb.links().get(1).click();

    RepositoriesSummaryTile summaryTile = RepositoriesSummaryPage.summaryTile();
    summaryTile.name().shouldBe(visible).shouldHave(text("Repository Managers"));
  }

  @Test
  public void testLegacyViolationIsDisabledInAddPageAndEditPage() {
    createPolicy();
    refresh();

    // Check new policy page
    RepositoriesSummaryPage.policyTile().addPolicyButton().click();
    PolicyEditorPage.legacyViolationCheckbox().is(hidden);

    NxBreadcrumb breadcrumb = new NxBreadcrumb();
    breadcrumb.links().get(1).click();

    // Check update policy page
    PolicyTileList policyList = RepositoriesSummaryPage.policyTile().policyList(0);
    policyList.row(1).click();
    PolicyEditorPage.legacyViolationCheckbox().is(hidden);
  }

  @Test
  public void testUnauthorizedUserCannotAddAndEditPolicy() {
    try {
      User user = tempEntity.newUser("username", "john", "doe", "john@doe");
      refreshOrOpen(DashboardPage.url());
      logout();
      login(user.getUsername(), user.getPassword());
      refreshOrOpen(OwnerSummaryPageWithLimitedVisibility.baseUrl());
      RepositoriesSummaryPage.getErrorAlert().shouldHave(text("Insufficient permissions"));
    }
    finally {
      logout();
      refreshOrOpen(OwnerSummaryPageWithLimitedVisibility.baseUrl());
      loginAsAdmin();
    }
  }

  @Test
  public void testUnauthorizedFirewallUserCannotAddAndEditPolicy() {
    try {
      User user = tempEntity.newUser("username", "john", "doe", "john@doe");
      refreshOrOpen(DashboardPage.url());
      logout();
      login(user.getUsername(), user.getPassword());
      refreshOrOpen(RepositoriesSummaryPage.url());
      RepositoriesSummaryPage.getFirewallPermissionBanner().shouldBe(visible);
    }
    finally {
      logout();
      RepositoriesSummaryPage.getFirewallPermissionBanner().shouldNotBe(visible);
      loginAsAdmin();
    }
  }

  @Test
  public void testInheritedPolicies_DisableUpdates() {
    Owner parentOwner = organizationDAO.getByIdNotNull(ROOT_ORGANIZATION_ID);
    Policy policy = tempEntity.newPolicy(parentOwner.getId(), "Policy 1 " + parentOwner.getName(), 10,
        Action.ID_FAIL, Stage.ID_BUILD, null);
    policy.setNotifications(new Notifications(new UserNotification("email1@domain", Stage.ID_PROXY)));
    List<Policy> inheritedPolicies = Arrays.asList(policy);

    refresh();

    PolicyTileList policyTileList = RepositoriesSummaryPage.policyTile().policyList(1);
    policyTileList.rows().shouldHave(size(inheritedPolicies.size() + 1));
    policyTileList.row(1).click();
    waitUntilUrl(PolicyEditorPage.urlToEdit(OwnerType.REPOSITORY_CONTAINER,
        RepositoryContainer.REPOSITORY_CONTAINER_ID, inheritedPolicies.get(0).getId()));

    //Summary Section
    PolicyEditorPage.summarySection().policyName().input().shouldBe(visible, disabled);
    PolicyEditorPage.summarySection().threatLevel().shouldBe(visible).shouldHave(DISABLED);

    //Inheritance section
    PolicyInheritsToSection inheritance = PolicyEditorPage.inheritanceSection();

    ScrollUtil.scrollIntoView(PolicyEditorPage.inheritanceSection().header());
    inheritance.shouldBe(visible);
    inheritance.allChildrenInheritRadio().shouldBe(visible, disabled);
    inheritance.specifiedChildrenInheritRadio().shouldBe(visible, disabled);
    inheritance.policyActionsOverrideCheckbox().shouldBe(visible, disabled);
    inheritance.policyActionsOverrideCheckbox().label().shouldHave(
        text("Allow action overrides at organization, application and repositories levels")
    );
    inheritance.policyNotificationsOverrideCheckbox().shouldBe(visible, disabled);
    inheritance.policyNotificationsOverrideCheckbox().label().shouldHave(
        text("Allow notification overrides at organization, application and repositories levels")
    );

    eyesWatcher.eyesCheck("Policy Editor Inheritance section at repository container level for root org policy");

    //Constraints Section
    ConstraintSection constraintSection = PolicyEditorPage.constraintSection();
    constraintSection.addConstraintButton().shouldBe(visible, disabled);

    //Actions Section
    ActionsSection actionsSection = PolicyEditorPage.actionsSection();

    ScrollUtil.scrollIntoView(PolicyEditorPage.actionsSection().header());
    actionsSection.inheritParentActions().shouldBe(visible, disabled);
    actionsSection.overrideParentActions().shouldBe(visible, disabled);
    actionsSection.proxy().noActionRadio().shouldBe(visible, disabled);
    actionsSection.proxy().warnRadio().shouldBe(visible, disabled);
    actionsSection.proxy().failRadio().shouldBe(visible, disabled);
    actionsSection.develop().noActionRadio().shouldBe(visible, disabled);
    actionsSection.develop().warnRadio().shouldBe(visible, disabled);
    actionsSection.develop().failRadio().shouldBe(visible, disabled);
    actionsSection.source().noActionRadio().shouldBe(visible, disabled);
    actionsSection.source().warnRadio().shouldBe(visible, disabled);
    actionsSection.source().failRadio().shouldBe(visible, disabled);
    actionsSection.build().noActionRadio().shouldBe(visible, disabled);
    actionsSection.build().warnRadio().shouldBe(visible, disabled);
    actionsSection.build().failRadio().shouldBe(visible, disabled);
    actionsSection.stageRelease().noActionRadio().shouldBe(visible, disabled);
    actionsSection.stageRelease().warnRadio().shouldBe(visible, disabled);
    actionsSection.stageRelease().failRadio().shouldBe(visible, disabled);
    actionsSection.release().noActionRadio().shouldBe(visible, disabled);
    actionsSection.release().warnRadio().shouldBe(visible, disabled);
    actionsSection.release().failRadio().shouldBe(visible, disabled);
    actionsSection.operate().noActionRadio().shouldBe(visible, disabled);
    actionsSection.operate().warnRadio().shouldBe(visible, disabled);
    actionsSection.operate().failRadio().shouldBe(visible, disabled);

    //Notifications Section
    NotificationsSection notificationsSection = PolicyEditorPage.notificationsSection();

    notificationsSection.notificationsOverrideSection().shouldBe(visible);
    notificationsSection.inheritParentNotifications().shouldBe(visible, disabled);
    notificationsSection.overrideParentNotifications().shouldBe(visible, disabled);

    PolicyEditorPage.saveButton().shouldBe(visible);
  }

  private Policy createPolicy() {
    Policy policy = tempEntity.newPolicy(repositoryContainer.getId(), "original name", 1);
    Constraint constraint1 = new Constraint(policy.getId() + "1", "First Constraint with One Condition", null);
    constraint1.addCondition(new Condition(AgeInDaysConditionType.ID, "older than", "730"));
    policy.setConstraints(Collections.singletonList(constraint1));

    policy.setAction(Stage.ID_PROXY, Action.ID_FAIL);

    policyDAO.update(policy);
    return policy;
  }

  public void testConstraint(
      Constraint constraint,
      String constraintName,
      String conditionTypeId,
      String operator,
      String value)
  {
    assertThat(constraint.getName()).isEqualTo(constraintName);
    assertThat(constraint.getConditions().get(0).getConditionTypeId()).isEqualTo(conditionTypeId);
    assertThat(constraint.getConditions().get(0).getOperator()).isEqualTo(operator);
    assertThat(constraint.getConditions().get(0).getValue()).isEqualTo(value);
  }

  public void testUserNotification(UserNotification userNotification, String emailAddress) {
    assertThat(userNotification.getEmailAddress()).isEqualTo(emailAddress);
  }

  public void testRoleNotification(RoleNotification roleNotification, String roleId) {
    assertThat(roleNotification.getRoleId()).isEqualTo(roleId);
  }

  private void assertActionsSectionIsInCorrectState() {
    ActionsSection actionsSection = PolicyEditorPage.actionsSection();
    actionsSection.proxy().noActionRadio().shouldBe(enabled);
    actionsSection.develop().noActionRadio().shouldBe(disabled);
    actionsSection.build().noActionRadio().shouldBe(disabled);
    actionsSection.stageRelease().noActionRadio().shouldBe(disabled);
    actionsSection.release().noActionRadio().shouldBe(disabled);
    actionsSection.operate().noActionRadio().shouldBe(disabled);
  }
}
