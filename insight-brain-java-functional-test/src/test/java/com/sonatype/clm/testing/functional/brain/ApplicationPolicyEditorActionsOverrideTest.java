/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ActionsSection;
import com.sonatype.clm.testing.functional.elements.NxCheckbox;
import com.sonatype.clm.testing.functional.elements.NxTooltip;
import com.sonatype.clm.testing.functional.elements.PolicyInheritsToSection;
import com.sonatype.clm.testing.functional.elements.PolicyInheritsToSection.OverridesConfirmationModal;
import com.sonatype.clm.testing.functional.elements.PolicyTile;
import com.sonatype.clm.testing.functional.elements.PolicyTileList;
import com.sonatype.clm.testing.functional.elements.SummarySection;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.PolicyEditorPage;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.conditions.RelativePopularityConditionType;
import com.sonatype.insight.brain.model.policy.notifications.Notification;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;

import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationPolicyEditorActionsOverrideTest
    extends AbstractFunctionalTest
{
  private static final String TEST_ORGANIZATION_PUBLIC_ID = "TestOrganization";

  private static final String TEST_APPLICATION_PUBLIC_ID = "TestApplication";

  private RoleDAO roleDAO;

  private PolicyDAO policyDAO;

  private Owner currentOwner;

  private Organization organization;

  private Application application;

  @BeforeClass
  public static void boot() {
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    loginAsAdmin();
  }

  @Before
  public void init() {
    roleDAO = lookup(RoleDAO.class);
    policyDAO = lookup(PolicyDAO.class);

    organization = tempEntity.newOrganization(TEST_ORGANIZATION_PUBLIC_ID);
    application = tempEntity.newApplication(getClass().getSimpleName() + "ȧpp", TEST_APPLICATION_PUBLIC_ID,
        organization.getId());
    // We always start at application summary page
    goToOwnerSummaryPage(application);
  }

  @Test
  public void testActionsOverrideEnabled() {
    String inheritedOwnerId = currentOwner.getParentOwnerId();
    Map<String, String> mockActions = new HashMap<>();
    mockActions.put(Stage.ID_DEVELOP, Action.ID_WARN);
    mockActions.put(Stage.ID_BUILD, Action.ID_WARN);
    assertThat(inheritedOwnerId).isEqualTo(organization.getId());
    Policy policy =
        createPolicy(inheritedOwnerId, "ORGANIZATION POLICY", 10, true, mockActions, Collections.emptyMap());

    refresh();

    OwnerSummaryPage.policyTile().policyList(1).row(1).click();
    waitUntilUrl(PolicyEditorPage.urlToEdit(currentOwner, policy.getId()));

    SummarySection summarySection = PolicyEditorPage.summarySection();
    assertThat(summarySection.policyName().input().getValue()).isEqualTo(policy.getName());

    PolicyInheritsToSection inheritanceSection = PolicyEditorPage.inheritanceSection();
    inheritanceSection.policyActionsOverrideCheckbox().shouldBe(disabled).shouldBe(visible).shouldBe(selected);

    ActionsSection actionsTable = PolicyEditorPage.actionsSection();
    ScrollUtil.scrollIntoView(actionsTable.title());

    assertThat(actionsTable.paragraph().text())
        .isEqualTo(
            "Action overrides have been enabled for this policy. Modifying actions will only affect this level.");

    actionsTable.quarantineWarningMessage().shouldNotBe(visible);

    actionsTable.actionsOverrideSection().shouldBe(visible);
    actionsTable.inheritParentActions().shouldBe(selected);

    testActionsState(actionsTable, disabled);

    actionsTable.overrideParentActions().click();
    actionsTable.overrideParentActions().shouldBe(selected);

    testActionsState(actionsTable, enabled);

    actionsTable.proxy().failRadio().click();
    actionsTable.proxy().failRadio().shouldBe(selected);
    actionsTable.proxy().warnRadio().shouldNotBe(selected);
    actionsTable.proxy().noActionRadio().shouldNotBe(selected);
    actionsTable.quarantineWarningMessage().shouldNotBe(visible);

    // Set proxy to warn, operate to fail
    actionsTable.proxy().warnRadio().click();
    actionsTable.proxy().warnRadio().shouldBe(selected);
    actionsTable.proxy().failRadio().shouldNotBe(selected);
    actionsTable.proxy().noActionRadio().shouldNotBe(selected);
    actionsTable.quarantineWarningMessage().shouldNotBe(visible);

    actionsTable.operate().noActionRadio().click();
    actionsTable.operate().noActionRadio().shouldBe(selected);
    actionsTable.operate().warnRadio().shouldNotBe(selected);
    actionsTable.operate().failRadio().shouldNotBe(selected);

    actionsTable.operate().failRadio().click();
    actionsTable.operate().failRadio().shouldBe(selected);
    actionsTable.operate().warnRadio().shouldNotBe(selected);
    actionsTable.operate().noActionRadio().shouldNotBe(selected);

    // Save and verify changes via backend
    PolicyEditorPage.savePolicy();

    ScrollUtil.scrollIntoView(actionsTable.title());

    policy = policyDAO.getById(policy.getId());

    Map<String, String> actions = policy.getActions();
    assertThat(actions)
        .containsEntry(Stage.ID_BUILD, Action.ID_WARN)
        .containsEntry(Stage.ID_DEVELOP, Action.ID_WARN);

    assertThat(actions.get(Stage.ID_PROXY)).isNull();
    assertThat(actions.get(Stage.ID_OPERATE)).isNull();
    assertThat(actions.get(Stage.ID_STAGE_RELEASE)).isNull();
    assertThat(actions.get(Stage.ID_RELEASE)).isNull();

    Map<String, String> policyActionsOverrides = policy
        .getPolicyActionsOverrides()
        .entrySet()
        .iterator()
        .next()
        .getValue();

    assertThat(policyActionsOverrides)
        .containsEntry(Stage.ID_BUILD, Action.ID_WARN)
        .containsEntry(Stage.ID_DEVELOP, Action.ID_WARN)
        .containsEntry(Stage.ID_PROXY, Action.ID_WARN)
        .containsEntry(Stage.ID_OPERATE, Action.ID_FAIL);

    assertThat(actions.get(Stage.ID_STAGE_RELEASE)).isNull();
    assertThat(actions.get(Stage.ID_RELEASE)).isNull();

    eyesWatcher.eyesCheck("override policy actions");
  }

  @Test
  public void testActionsOverrideDisabled() {
    String inheritedOwnerId = currentOwner.getParentOwnerId();
    Map<String, String> actions = new HashMap<>();
    actions.put(Stage.ID_DEVELOP, Action.ID_WARN);
    actions.put(Stage.ID_BUILD, Action.ID_FAIL);
    assertThat(inheritedOwnerId).isEqualTo(organization.getId());
    Policy policy = createPolicy(inheritedOwnerId, "ORGANIZATION POLICY", 10, false, actions, Collections.emptyMap());

    refresh();

    OwnerSummaryPage.policyTile().policyList(1).row(1).click();
    waitUntilUrl(PolicyEditorPage.urlToEdit(currentOwner, policy.getId()));

    SummarySection summarySection = PolicyEditorPage.summarySection();
    assertThat(summarySection.policyName().input().getValue()).isEqualTo(policy.getName());

    PolicyInheritsToSection inheritanceSection = PolicyEditorPage.inheritanceSection();
    inheritanceSection.policyActionsOverrideCheckbox().shouldBe(disabled).shouldBe(visible).shouldNotBe(selected);

    ActionsSection actionsTable = PolicyEditorPage.actionsSection();
    ScrollUtil.scrollIntoView(actionsTable.title());

    assertThat(actionsTable.paragraph().text())
        .isEqualTo("Action overrides have been disabled for this policy.");

    actionsTable.actionsOverrideSection().shouldBe(visible);

    actionsTable.inheritParentActions().shouldBe(selected, disabled);
    actionsTable.overrideParentActions().shouldBe(disabled).shouldNotBe(selected);

    testActionsState(actionsTable, disabled);

    PolicyEditorPage.saveButton().shouldHave(DISABLED);
  }

  @Test
  public void testActionsOverridesAreRemoved() {
    String inheritedOwnerId = currentOwner.getParentOwnerId();
    Map<String, String> actions = new HashMap<>();
    actions.put(Stage.ID_DEVELOP, Action.ID_WARN);
    actions.put(Stage.ID_BUILD, Action.ID_WARN);

    Map<String, String> overrides = new HashMap<>();
    overrides.put(Stage.ID_DEVELOP, Action.ID_NOTIFY);
    overrides.put(Stage.ID_BUILD, Action.ID_NOTIFY);
    overrides.put(Stage.ID_RELEASE, Action.ID_FAIL);
    overrides.put(Stage.ID_OPERATE, Action.ID_FAIL);

    Map<String, Map<String, String>> overrideByOwner = new HashMap<>();
    overrideByOwner.put(currentOwner.getId(), overrides);
    overrideByOwner.put(inheritedOwnerId, overrides);
    assertThat(inheritedOwnerId).isEqualTo(organization.getId());
    Policy policy = createPolicy(inheritedOwnerId, "ORGANIZATION POLICY", 10, true, actions, overrideByOwner);

    goToOwnerSummaryPage(organization);

    OwnerSummaryPage.policyTile().policyList(0).row(1).click();
    waitUntilUrl(PolicyEditorPage.urlToEdit(currentOwner, policy.getId()));

    SummarySection summarySection = PolicyEditorPage.summarySection();
    assertThat(summarySection.policyName().input().getValue()).isEqualTo(policy.getName());

    PolicyInheritsToSection inheritanceSection = PolicyEditorPage.inheritanceSection();
    NxCheckbox policyActionsOverride = inheritanceSection.policyActionsOverrideCheckbox();
    policyActionsOverride.shouldNotBe(disabled).shouldBe(visible).shouldBe(selected);

    policyActionsOverride.click();
    OverridesConfirmationModal overridesConfirmationModal = new OverridesConfirmationModal();
    overridesConfirmationModal.shouldBe(visible);
    overridesConfirmationModal.continueButton().shouldBe(enabled).click();
    overridesConfirmationModal.shouldNotBe(visible);
    policyActionsOverride.shouldBe(visible).shouldNotBe(selected);
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);
    PolicyEditorPage.savePolicy();

    // Save and verify changes via backend
    Policy updatedPolicy = policyDAO.getById(policy.getId());
    assertThat(updatedPolicy.getPolicyActionsOverrides()).isNull();
  }

  @Test
  public void testActionsOverrideIsVisibleOnSummaryPage() {
    String inheritedOwnerId = currentOwner.getParentOwnerId();
    Map<String, String> actions = new HashMap<>();
    actions.put(Stage.ID_DEVELOP, Action.ID_WARN);
    actions.put(Stage.ID_BUILD, Action.ID_WARN);

    Map<String, String> overrides = new HashMap<>();
    overrides.put(Stage.ID_DEVELOP, Action.ID_NOTIFY);
    overrides.put(Stage.ID_BUILD, Action.ID_NOTIFY);
    overrides.put(Stage.ID_RELEASE, Action.ID_FAIL);
    overrides.put(Stage.ID_OPERATE, Action.ID_FAIL);

    Map<String, Map<String, String>> overrideByOwner = new HashMap<>();
    overrideByOwner.put(currentOwner.getId(), overrides);
    assertThat(inheritedOwnerId).isEqualTo(organization.getId());
    assertThat(currentOwner.getId()).isEqualTo(application.getId());
    Policy policy = createPolicy(inheritedOwnerId, "ORGANIZATION POLICY", 10, true, actions, overrideByOwner);

    refresh();

    PolicyTile policyTile = OwnerSummaryPage.policyTile();

    PolicyTileList inheritedPolicyEntry = policyTile.policyList(1);
    PolicyTileList.PolicyTileListElement policyElement = inheritedPolicyEntry.row(1);
    assertThat(policy).isNotNull();
    policyElement.name().shouldHave(text("*" + policy.getName()));

    SelenideElement overrideAsterisk = policyTile.policyOverrideAsterisk();
    overrideAsterisk.hover();

    NxTooltip tooltip = new NxTooltip();
    tooltip.shouldBe(visible).shouldHave(text("Policy Actions are overridden"));

    eyesWatcher.eyesCheck("owner summary view with overridden policies", false, false);
  }

  @Test
  public void testActionsOverrideIsLoadedForApplicationPolicy() {
    String inheritedOwnerId = currentOwner.getParentOwnerId();
    Map<String, String> actions = new HashMap<>();
    actions.put(Stage.ID_DEVELOP, Action.ID_WARN);
    actions.put(Stage.ID_BUILD, Action.ID_WARN);

    Map<String, String> overrides = new HashMap<>();
    overrides.put(Stage.ID_DEVELOP, Action.ID_NOTIFY);
    overrides.put(Stage.ID_BUILD, Action.ID_NOTIFY);
    overrides.put(Stage.ID_RELEASE, Action.ID_FAIL);
    overrides.put(Stage.ID_OPERATE, Action.ID_FAIL);

    Map<String, Map<String, String>> overrideByOwner = new HashMap<>();
    overrideByOwner.put(currentOwner.getId(), overrides);
    assertThat(inheritedOwnerId).isEqualTo(organization.getId());
    assertThat(currentOwner.getId()).isEqualTo(application.getId());
    Policy policy = createPolicy(inheritedOwnerId, "ORGANIZATION POLICY", 10, true, actions, overrideByOwner);

    refresh();

    OwnerSummaryPage.policyTile().policyList(1).row(1).click();
    waitUntilUrl(PolicyEditorPage.urlToEdit(currentOwner, policy.getId()));

    SummarySection summarySection = PolicyEditorPage.summarySection();
    assertThat(summarySection.policyName().input().getValue()).isEqualTo(policy.getName());

    PolicyInheritsToSection inheritanceSection = PolicyEditorPage.inheritanceSection();
    inheritanceSection.policyActionsOverrideCheckbox().shouldBe(disabled).shouldBe(visible).shouldBe(selected);

    ActionsSection actionsTable = PolicyEditorPage.actionsSection();
    ScrollUtil.scrollIntoView(actionsTable.title());

    actionsTable.actionsOverrideSection().shouldBe(visible);
    actionsTable.overrideParentActions().shouldBe(selected);

    actionsTable.develop().warnRadio().shouldNotBe(selected);
    actionsTable.develop().failRadio().shouldNotBe(selected);
    actionsTable.develop().noActionRadio().shouldNotBe(selected);

    actionsTable.build().warnRadio().shouldNotBe(selected);
    actionsTable.build().failRadio().shouldNotBe(selected);
    actionsTable.build().noActionRadio().shouldNotBe(selected);

    actionsTable.release().warnRadio().shouldNotBe(selected);
    actionsTable.release().failRadio().shouldBe(selected);
    actionsTable.release().noActionRadio().shouldNotBe(selected);

    actionsTable.operate().warnRadio().shouldNotBe(selected);
    actionsTable.operate().failRadio().shouldBe(selected);
    actionsTable.operate().noActionRadio().shouldNotBe(selected);

    actionsTable.inheritParentActions().click();
    actionsTable.inheritParentActions().shouldBe(selected);

    testActionsState(actionsTable, disabled);

    actionsTable.proxy().failRadio().shouldNotBe(selected);
    actionsTable.proxy().noActionRadio().shouldBe(selected);
    actionsTable.proxy().warnRadio().shouldNotBe(selected);

    actionsTable.develop().warnRadio().shouldBe(selected);
    actionsTable.develop().failRadio().shouldNotBe(selected);
    actionsTable.develop().noActionRadio().shouldNotBe(selected);

    actionsTable.source().warnRadio().shouldNotBe(selected);
    actionsTable.source().failRadio().shouldNotBe(selected);
    actionsTable.source().noActionRadio().shouldBe(selected);

    actionsTable.build().warnRadio().shouldBe(selected);
    actionsTable.build().failRadio().shouldNotBe(selected);
    actionsTable.build().noActionRadio().shouldNotBe(selected);

    actionsTable.release().warnRadio().shouldNotBe(selected);
    actionsTable.release().failRadio().shouldNotBe(selected);
    actionsTable.release().noActionRadio().shouldBe(selected);

    actionsTable.operate().warnRadio().shouldNotBe(selected);
    actionsTable.operate().failRadio().shouldNotBe(selected);
    actionsTable.operate().noActionRadio().shouldBe(selected);

    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);
  }

  @Test
  public void testUpdateButtonNotDisabledForInheritedPolicyWithActionsOverridesOnly() {
    String inheritedOwnerId = currentOwner.getParentOwnerId();
    Map<String, String> actions = new HashMap<>();
    actions.put(Stage.ID_DEVELOP, Action.ID_WARN);
    actions.put(Stage.ID_BUILD, Action.ID_WARN);
    assertThat(inheritedOwnerId).isEqualTo(organization.getId());
    Policy policy = createPolicy(inheritedOwnerId, "ORGANIZATION POLICY", 10, true, actions, Collections.emptyMap());

    refresh();

    OwnerSummaryPage.policyTile().policyList(1).row(1).click();
    waitUntilUrl(PolicyEditorPage.urlToEdit(currentOwner, policy.getId()));

    PolicyInheritsToSection inheritanceSection = PolicyEditorPage.inheritanceSection();
    inheritanceSection.policyActionsOverrideCheckbox()
        .shouldBe(disabled)
        .shouldBe(visible)
        .shouldBe(selected);

    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    ActionsSection actionsTable = PolicyEditorPage.actionsSection();
    ScrollUtil.scrollIntoView(actionsTable.title());

    actionsTable.actionsOverrideSection().shouldBe(visible);
    actionsTable.overrideParentActions().click();
    actionsTable.overrideParentActions().shouldBe(selected);

    PolicyEditorPage.savePolicy();

    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);
  }

  @Test
  public void testUpdateButtonEnabledForInheritedPolicyWithNotificationOverridesOnly() {
    String inheritedOwnerId = currentOwner.getParentOwnerId();
    Map<String, String> actions = new HashMap<>();
    actions.put(Stage.ID_DEVELOP, Action.ID_WARN);
    actions.put(Stage.ID_BUILD, Action.ID_WARN);
    assertThat(inheritedOwnerId).isEqualTo(organization.getId());

    Policy policy = createPolicy(inheritedOwnerId, "ORGANIZATION POLICY", 10, false, actions, Collections.emptyMap());
    policy.setPolicyNotificationsOverrideAllowed(true);
    policyDAO.update(policy);

    refresh();

    OwnerSummaryPage.policyTile().policyList(1).row(1).click();
    waitUntilUrl(PolicyEditorPage.urlToEdit(currentOwner, policy.getId()));

    PolicyInheritsToSection inheritanceSection = PolicyEditorPage.inheritanceSection();
    inheritanceSection.policyNotificationsOverrideCheckbox()
        .shouldBe(disabled)
        .shouldBe(visible)
        .shouldBe(selected);

    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);
  }

  @Test
  public void testUpdateButtonEnabledForInheritedPolicyWithBothOverridesAllowed() {
    String inheritedOwnerId = currentOwner.getParentOwnerId();
    Map<String, String> actions = new HashMap<>();
    actions.put(Stage.ID_DEVELOP, Action.ID_WARN);
    actions.put(Stage.ID_BUILD, Action.ID_WARN);
    assertThat(inheritedOwnerId).isEqualTo(organization.getId());

    // both overrides allowed
    Policy policy = createPolicy(inheritedOwnerId, "ORGANIZATION POLICY", 10, true, actions, Collections.emptyMap());
    policy.setPolicyNotificationsOverrideAllowed(true);
    policyDAO.update(policy);

    refresh();

    OwnerSummaryPage.policyTile().policyList(1).row(1).click();
    waitUntilUrl(PolicyEditorPage.urlToEdit(currentOwner, policy.getId()));

    PolicyInheritsToSection inheritanceSection = PolicyEditorPage.inheritanceSection();
    inheritanceSection.policyActionsOverrideCheckbox()
        .shouldBe(disabled)
        .shouldBe(visible)
        .shouldBe(selected);
    inheritanceSection.policyNotificationsOverrideCheckbox()
        .shouldBe(disabled)
        .shouldBe(visible)
        .shouldBe(selected);

    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    ActionsSection actionsTable = PolicyEditorPage.actionsSection();
    ScrollUtil.scrollIntoView(actionsTable.title());

    actionsTable.actionsOverrideSection().shouldBe(visible);
    actionsTable.overrideParentActions().click();
    actionsTable.overrideParentActions().shouldBe(selected);

    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);
  }

  private void goToOwnerSummaryPage(Owner currentOwner) {
    this.currentOwner = currentOwner;
    refreshOrOpen(OwnerSummaryPage.url(currentOwner));
    OwnerSummaryPage.summaryTile().name().shouldHave(text(currentOwner.getName()));
  }

  private Policy createPolicy(
      String ownerId,
      String name,
      int threatLevel,
      boolean policyActionsOverrideAllowed,
      Map<String, String> actions,
      Map<String, Map<String, String>> actionsOverrides)
  {
    Policy policy = tempEntity.newPolicy(ownerId, name, threatLevel);
    policy.setConstraints(createConstraints(ownerId, policy));
    policy.setPolicyActionsOverrideAllowed(policyActionsOverrideAllowed);
    policy.setPolicyActionsOverrides(actionsOverrides);

    actions.forEach(policy::setAction);
    policy.getNotifications()
        .add(
            new UserNotification("test@foo.com", Stage.ID_BUILD, Notification.CONTINUOUS_MONITORING));
    String roleName = "Developer";
    policy.getNotifications().add(new RoleNotification(roleDAO.getByName(roleName).getId(), roleName, Stage.ID_BUILD));

    tempEntity.newLicenseThreatGroup(currentOwner.getId(), "my LTG 2", 10);

    policyDAO.update(policy);
    return policy;
  }

  private List<Constraint> createConstraints(String ownerId, Policy policy) {
    Constraint constraint1 = new Constraint(policy.getId() + "1", "First Constraint with One Condition", null);
    constraint1.addCondition(new Condition(AgeInDaysConditionType.ID, "older than", "730"));
    Constraint constraint2 = new Constraint(policy.getId() + "2", "Second Constraint with Two Conditions",
        LogicalOperator.AND);
    constraint2.addCondition(new Condition(LicenseThreatGroupConditionType.ID, "is",
        tempEntity.newLicenseThreatGroup(ownerId, "my LTG", 5).getId()));
    constraint2
        .addCondition(new Condition(LabelConditionType.ID, "is", tempEntity.newLabel(ownerId, "my Label").getId()));
    Constraint constraint3 = new Constraint(policy.getId() + "3", "Third Constraint with Two Conditions",
        LogicalOperator.OR);
    constraint3.addCondition(new Condition(RelativePopularityConditionType.ID, "<", "50"));
    constraint3.addCondition(new Condition(CoordinatesConditionType.ID, "do not match", "maven:blah:blah:blah"));
    return Arrays.asList(constraint1, constraint2, constraint3);
  }

  public void testActionsState(ActionsSection actionsTable, com.codeborne.selenide.WebElementCondition condition) {
    actionsTable.proxy().failRadio().shouldBe(condition);
    actionsTable.proxy().noActionRadio().shouldBe(condition);
    actionsTable.proxy().warnRadio().shouldBe(condition);

    actionsTable.build().failRadio().shouldBe(condition);
    actionsTable.build().noActionRadio().shouldBe(condition);
    actionsTable.build().warnRadio().shouldBe(condition);

    actionsTable.develop().failRadio().shouldBe(condition);
    actionsTable.develop().noActionRadio().shouldBe(condition);
    actionsTable.develop().warnRadio().shouldBe(condition);

    actionsTable.source().failRadio().shouldBe(condition);
    actionsTable.source().noActionRadio().shouldBe(condition);
    actionsTable.source().warnRadio().shouldBe(condition);

    actionsTable.stageRelease().failRadio().shouldBe(condition);
    actionsTable.stageRelease().noActionRadio().shouldBe(condition);
    actionsTable.stageRelease().warnRadio().shouldBe(condition);

    actionsTable.release().failRadio().shouldBe(condition);
    actionsTable.release().noActionRadio().shouldBe(condition);
    actionsTable.release().warnRadio().shouldBe(condition);

    actionsTable.operate().failRadio().shouldBe(condition);
    actionsTable.operate().noActionRadio().shouldBe(condition);
    actionsTable.operate().warnRadio().shouldBe(condition);
  }
}
