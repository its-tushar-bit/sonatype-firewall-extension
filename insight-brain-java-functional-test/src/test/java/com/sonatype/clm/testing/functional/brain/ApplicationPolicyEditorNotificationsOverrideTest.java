/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.NotificationsSection;
import com.sonatype.clm.testing.functional.elements.NotificationsSection.NotificationItem;
import com.sonatype.clm.testing.functional.elements.NxCheckbox;
import com.sonatype.clm.testing.functional.elements.NxRadio;
import com.sonatype.clm.testing.functional.elements.PolicyInheritsToSection.OverridesConfirmationModal;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.elements.UnsavedModal;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.PolicyEditorPage;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.brain.dataaccess.configuration.webhook.WebhookDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.jira.JiraIssueType;
import com.sonatype.insight.brain.jira.JiraProject;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.notifications.JiraNotification;
import com.sonatype.insight.brain.model.policy.notifications.Notification;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.notifications.WebhookNotification;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class ApplicationPolicyEditorNotificationsOverrideTest
    extends AbstractFunctionalTest
{
  private Organization organization;

  private Application application;

  private Role role1;

  private Role role2;

  private Webhook webhook1;

  private Webhook webhook2;

  private JiraProject jiraProject1;

  private JiraProject jiraProject2;

  private Map<String, String> jiraProjectKeyToRecipient;

  private User userWithoutPermission;

  private NxCheckbox notificationsOverrideCheckbox =
      PolicyEditorPage.inheritanceSection().policyNotificationsOverrideCheckbox();

  private OverridesConfirmationModal overridesConfirmationModal = new OverridesConfirmationModal();

  private NotificationsSection notificationsSection = PolicyEditorPage.notificationsSection();

  private SelenideElement notificationsOverrideSection = notificationsSection.notificationsOverrideSection();

  private NxRadio inheritParentNotifications = notificationsSection.inheritParentNotifications();

  private NxRadio overrideParentNotifications = notificationsSection.overrideParentNotifications();

  private PolicyDAO policyDAO;

  private WebhookDAO webhookDAO;

  private RoleDAO roleDAO;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    loginAsAdmin();
  }

  @Before
  public void before() throws Exception {
    policyDAO = lookup(PolicyDAO.class);
    webhookDAO = lookup(WebhookDAO.class);
    roleDAO = lookup(RoleDAO.class);

    organization = tempEntity.newOrganization("TestOrganization");
    application =
        tempEntity.newApplication(getClass().getSimpleName() + "ȧpp", "TestApplication", organization.getId());
    role1 = tempEntity.newRole("role1", false);
    role2 = tempEntity.newRole("role2", false);
    webhook1 = tempEntity.newWebhook("http://localhost/1", Collections.singleton(WebhookEventType.POLICY_ALERT));
    webhook2 = tempEntity.newWebhook("http://localhost/2", Collections.singleton(WebhookEventType.POLICY_ALERT));
    setupJiraService();
    userWithoutPermission = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(organization.getId(), role.getId(), userWithoutPermission.getUsername());
  }

  private void goToPolicyEditorPage(Owner owner, Policy policy) {
    String url = PolicyEditorPage.urlToEdit(owner, policy.getId());
    refreshOrOpen(url);
    waitUntilUrl(url);
  }

  private void loginAsUserWithoutPermission() {
    login(userWithoutPermission.getUsername(), userWithoutPermission.getPassword());
  }

  @Test
  public void testNotifications_AllowDisallowOverrides_ReflectsStoredPolicy_Enabled() {
    Policy policy = createPolicy(organization, true, createNotifications(), null);
    goToPolicyEditorPage(organization, policy);
    notificationsOverrideCheckbox.shouldBe(enabled).shouldBe(selected);
    eyesWatcher.eyesCheck("Inheritance overrides");
    goToPolicyEditorPage(application, policy);
    notificationsOverrideCheckbox.shouldBe(disabled).shouldBe(selected);
  }

  @Test
  public void testNotifications_AllowDisallowOverrides_ReflectsStoredPolicy_Disabled() {
    Policy policy = createPolicy(organization, false, createNotifications(), null);
    goToPolicyEditorPage(organization, policy);
    notificationsOverrideCheckbox.shouldBe(enabled).shouldNotBe(selected);
    goToPolicyEditorPage(application, policy);
    notificationsOverrideCheckbox.shouldBe(disabled).shouldNotBe(selected);
  }

  @Test
  public void testNotifications_AllowDisallowOverrides_Allowing() {
    Policy policy = createPolicy(organization, false, createNotifications(), null);
    goToPolicyEditorPage(organization, policy);
    notificationsOverrideCheckbox.shouldBe(enabled).shouldNotBe(selected).click();
    notificationsOverrideCheckbox.shouldBe(enabled, selected);
    PolicyEditorPage.savePolicy();
    refresh();
    notificationsOverrideCheckbox.shouldBe(enabled, selected);
    goToPolicyEditorPage(application, policy);
    notificationsOverrideCheckbox.shouldBe(disabled, selected);
    policy = policyDAO.getById(policy.getId());
    assertThat(policy.isPolicyNotificationsOverrideAllowed()).isTrue();
  }

  @Test
  public void testNotifications_AllowDisallowOverrides_Disallowing() {
    Policy policy = createPolicy(organization, true, createNotifications(), null);
    goToPolicyEditorPage(organization, policy);
    notificationsOverrideCheckbox.shouldBe(enabled, selected).click();
    notificationsOverrideCheckbox.shouldBe(enabled).shouldNotBe(selected);
    PolicyEditorPage.savePolicy();
    refresh();
    notificationsOverrideCheckbox.shouldBe(enabled).shouldNotBe(selected);
    goToPolicyEditorPage(application, policy);
    notificationsOverrideCheckbox.shouldBe(disabled).shouldNotBe(selected);
    policy = policyDAO.getById(policy.getId());
    assertThat(policy.isPolicyNotificationsOverrideAllowed()).isFalse();
  }

  @Test
  public void testNotifications_AllowDisallowOverrides_NoPermission() {
    try {
      Policy policy = createPolicy(organization, true, createNotifications(), null);
      goToPolicyEditorPage(organization, policy);
      logout();
      loginAsUserWithoutPermission();
      goToPolicyEditorPage(organization, policy);
      notificationsOverrideCheckbox.shouldBe(disabled).shouldBe(selected);
    }
    finally {
      logout();
      loginAsAdmin();
    }
  }

  @Test
  public void testNotifications_InheritOverride_PolicyOwner() {
    Policy policy = createPolicy(organization, true, createNotifications(), null);
    goToPolicyEditorPage(organization, policy);
    notificationsOverrideSection.shouldNotBe(visible);
  }

  @Test
  public void testNotifications_InheritOverride_NoOverrides() {
    Policy policy = createPolicy(organization, true, createNotifications(), null);
    goToPolicyEditorPage(application, policy);
    inheritParentNotifications.shouldBe(visible);
    inheritParentNotifications.shouldBe(enabled, selected);
    overrideParentNotifications.shouldBe(enabled).shouldNotBe(selected);
    eyesWatcher.eyesCheck("Notifications inherit");
  }

  @Test
  public void testNotifications_InheritOverride_Override_NoChanges() {
    Policy policy = createPolicy(organization, true, createNotifications(), null);
    goToPolicyEditorPage(application, policy);
    inheritParentNotifications.shouldBe(enabled, selected);
    overrideParentNotifications.shouldBe(enabled).shouldNotBe(selected);
    overrideParentNotifications.click();
    inheritParentNotifications.shouldBe(enabled).shouldNotBe(selected);
    overrideParentNotifications.shouldBe(enabled, selected);
    PolicyEditorPage.savePolicy();
    refresh();
    inheritParentNotifications.shouldBe(enabled).shouldNotBe(selected);
    overrideParentNotifications.shouldBe(enabled, selected);

    policy = policyDAO.getById(policy.getId());
    Map<String, Notifications> policyNotificationsOverrides = policy.getPolicyNotificationsOverrides();
    assertThat(policyNotificationsOverrides).isNotNull().containsOnlyKeys(application.getId());
    Notifications overrides = policyNotificationsOverrides.get(application.getId());
    assertThat(policy.getNotifications()).usingRecursiveComparison().isEqualTo(overrides);
  }

  @Test
  public void testNotifications_InheritOverride_Override_WithChanges() {
    Notifications notifications = createNotifications();
    Policy policy = createPolicy(organization, true, notifications, null);
    goToPolicyEditorPage(application, policy);
    inheritParentNotifications.shouldBe(enabled, selected);
    overrideParentNotifications.shouldBe(enabled).shouldNotBe(selected);
    overrideParentNotifications.click();
    inheritParentNotifications.shouldBe(enabled).shouldNotBe(selected);
    overrideParentNotifications.shouldBe(enabled, selected);
    changeNotificationsToOverrides(notifications);
    PolicyEditorPage.savePolicy();
    refresh();
    inheritParentNotifications.shouldBe(enabled).shouldNotBe(selected);
    overrideParentNotifications.shouldBe(enabled, selected);
    ScrollUtil.scrollIntoView(overrideParentNotifications.label());
    eyesWatcher.eyesCheck("Notifications override");

    policy = policyDAO.getById(policy.getId());
    Map<String, Notifications> policyNotificationsOverrides = policy.getPolicyNotificationsOverrides();
    assertThat(policyNotificationsOverrides).isNotNull().containsOnlyKeys(application.getId());
    Notifications overrides = policyNotificationsOverrides.get(application.getId());
    Notifications expectedOverrides = createNotificationsForOverrides();
    assertThat(overrides).usingRecursiveComparison().isEqualTo(expectedOverrides);
  }

  @Test
  public void testNotifications_DisallowOverrides_RemovesOverrides() {
    Policy policy = createPolicy(organization, true, createNotifications(), createOverrides());
    goToPolicyEditorPage(organization, policy);
    notificationsOverrideCheckbox.shouldBe(enabled, selected).click();
    overridesConfirmationModal.shouldBe(visible);
    overridesConfirmationModal.continueButton().shouldBe(visible, enabled).click();
    notificationsOverrideCheckbox.shouldBe(enabled).shouldNotBe(selected);
    PolicyEditorPage.savePolicy();
    policy = policyDAO.getById(policy.getId());
    Map<String, Notifications> policyNotificationsOverrides = policy.getPolicyNotificationsOverrides();
    assertThat(policyNotificationsOverrides).isNull();
  }

  @Test
  public void testNotifications_DisallowOverrides_DisablesInheritOverride() {
    Policy policy = createPolicy(organization, false, createNotifications(), null);
    goToPolicyEditorPage(application, policy);
    inheritParentNotifications.shouldNotBe(enabled).shouldBe(selected);
    overrideParentNotifications.shouldNotBe(enabled, selected);
  }

  @Test
  public void testNotifications_InheritOverride_NoPermission() {
    try {
      Policy policy = createPolicy(organization, true, createNotifications(), null);
      goToPolicyEditorPage(organization, policy);
      logout();
      loginAsUserWithoutPermission();
      goToPolicyEditorPage(application, policy);
      inheritParentNotifications.shouldNotBe(enabled).shouldBe(selected);
      overrideParentNotifications.shouldNotBe(enabled, selected);
    }
    finally {
      logout();
      loginAsAdmin();
    }
  }

  @Test
  public void testNewApplicationPolicy_DoesNotShowInheritance() {
    String url = PolicyEditorPage.urlToCreate(application);
    refreshOrOpen(url);
    waitUntilUrl(url);
    PolicyEditorPage.inheritanceSection().shouldNotBe(visible);
  }

  @Test
  public void testNotifications_AllowDisallowOverrides_UnsavedChanges() {
    Policy policy = createPolicy(organization, true, createNotifications(), null);
    goToPolicyEditorPage(organization, policy);
    notificationsOverrideCheckbox.click();
    SidebarNavigation.dashboardNavigationButton().click();
    UnsavedModal unsavedChangesModal = new UnsavedModal();
    unsavedChangesModal.shouldBe(visible);
    unsavedChangesModal.continueButton().click();
    waitUntilUrl(DashboardPage.urlToViolations());
  }

  @Test
  public void testNotifications_InheritOverride_UnsavedChanges() {
    Policy policy = createPolicy(organization, true, createNotifications(), null);
    goToPolicyEditorPage(application, policy);
    overrideParentNotifications.click();
    SidebarNavigation.dashboardNavigationButton().click();
    UnsavedModal unsavedChangesModal = new UnsavedModal();
    unsavedChangesModal.shouldBe(visible);
    unsavedChangesModal.continueButton().click();
    waitUntilUrl(DashboardPage.urlToViolations());
  }

  private Policy createPolicy(
      Owner owner,
      boolean policyNotificationsOverrideAllowed,
      Notifications notifications,
      Map<String, Notifications> notificationsOverrides)
  {
    Policy policy = tempEntity.newPolicy(owner.getId(), "TestPolicy", 10);
    Constraint constraint = new Constraint("constraint-" + policy.getId(), "First Constraint with One Condition", null);
    constraint.addCondition(new Condition(AgeInDaysConditionType.ID, "older than", "730"));
    policy.setConstraints(Collections.singletonList(constraint));
    policy.setNotifications(notifications);
    policy.setPolicyNotificationsOverrideAllowed(policyNotificationsOverrideAllowed);
    policy.setPolicyNotificationsOverrides(notificationsOverrides);
    policyDAO.update(policy);
    return policy;
  }

  private Notifications createNotifications() {
    String[] allStageIds = getAllStageIdsWithoutCompliance();
    String[] allStageIdsWithoutProxyAndCompliance = getAllStageIdsWithoutProxyAndCompliance();
    Notifications notifications = new Notifications(
        new UserNotification("email1@domain", allStageIds),
        new UserNotification("email2@domain"),
        new RoleNotification(role1.getId(), role1.getName(), allStageIds),
        new RoleNotification(role2.getId(), role2.getName()),
        new WebhookNotification(webhook1.getId(), allStageIdsWithoutProxyAndCompliance),
        new WebhookNotification(webhook2.getId()),
        new JiraNotification("projectKey1", 1, allStageIdsWithoutProxyAndCompliance),
        new JiraNotification("projectKey2", 2));
    // StageIds get desirialized as an unordered HashSet, so to allow recursive comparison ensure the StageIds here
    // are also a HashSet
    notifications.getAllNotifications()
        .forEach(notification -> notification.setStageIds(new HashSet<>(notification.getStageIds())));
    return notifications;
  }

  private Notifications createNotificationsForOverrides() {
    String[] allStageIds = getAllStageIdsWithoutCompliance();
    String[] allStageIdsWithoutProxy = getAllStageIdsWithoutProxyAndCompliance();
    Notifications notifications = new Notifications(
        new UserNotification("email1@domain"),
        new UserNotification("email2@domain", allStageIds),
        new RoleNotification(role1.getId(), role1.getName()),
        new RoleNotification(role2.getId(), role2.getName(), allStageIds),
        new WebhookNotification(webhook1.getId()),
        new WebhookNotification(webhook2.getId(), allStageIdsWithoutProxy),
        new JiraNotification("projectKey1", 1),
        new JiraNotification("projectKey2", 2, allStageIdsWithoutProxy));
    // StageIds get desirialized as an unordered HashSet, so to allow recursive comparison ensure the StageIds here
    // are also a HashSet
    notifications.getAllNotifications()
        .forEach(notification -> notification.setStageIds(new HashSet<>(notification.getStageIds())));
    return notifications;
  }

  private Map<String, Notifications> createOverrides() {
    Map<String, Notifications> overrides = new HashMap<>();
    overrides.put(application.getId(), createNotificationsForOverrides());
    return overrides;
  }

  private void changeNotificationsToOverrides(Notifications notifications) {
    notifications.getAllNotifications().forEach(this::clickAllNotificationStages);
  }

  private void clickAllNotificationStages(Notification notification) {
    NotificationItem notificationItem = toNotificationItem(notification);
    if (!(notification instanceof WebhookNotification || notification instanceof JiraNotification)) {
      notificationItem.proxy().click();
    }
    notificationItem.develop().click();
    notificationItem.source().click();
    notificationItem.build().click();
    notificationItem.stageRelease().click();
    notificationItem.release().click();
    notificationItem.operate().click();
    notificationItem.continuousMonitoring().click();
  }

  private NotificationItem toNotificationItem(Notification notification) {
    if (notification instanceof UserNotification) {
      return NotificationsSection.notificationFor(((UserNotification) notification).getEmailAddress());
    }
    if (notification instanceof RoleNotification) {
      return NotificationsSection.notificationFor(
          roleDAO.getById(((RoleNotification) notification).getRoleId()).getName());
    }
    if (notification instanceof WebhookNotification) {
      return NotificationsSection.notificationFor(
          "Webhook: " + webhookDAO.getById(((WebhookNotification) notification).getWebhookId()).getUrl());
    }
    if (notification instanceof JiraNotification) {
      return NotificationsSection.notificationFor(
          jiraProjectKeyToRecipient.get(((JiraNotification) notification).getProjectKey()));
    }
    throw new IllegalStateException("Unexpected notification type " + notification.getClass().getName());
  }

  private String[] getAllStageIdsWithoutCompliance() {
    Set<String> allStageIdsSet = getAllStageIdsSet();
    allStageIdsSet.remove(StageTypes.COMPLIANCE.getId());
    return allStageIdsSet.toArray(new String[0]);
  }

  private String[] getAllStageIdsWithoutProxyAndCompliance() {
    Set<String> allStageIdsWithoutProxy = getAllStageIdsSet();
    allStageIdsWithoutProxy.remove(StageTypes.PROXY.getId());
    allStageIdsWithoutProxy.remove(StageTypes.COMPLIANCE.getId());
    return allStageIdsWithoutProxy.toArray(new String[0]);
  }

  private Set<String> getAllStageIdsSet() {
    Set<String> allStageIdsAndContinuousMonitoring = StageTypes.getAll()
        .stream()
        .map(StageType::getId)
        .collect(
            Collectors.toCollection(LinkedHashSet::new));
    allStageIdsAndContinuousMonitoring.add(Notification.CONTINUOUS_MONITORING);
    return allStageIdsAndContinuousMonitoring;
  }

  private void setupJiraService() throws IOException {
    jiraProject1 = new JiraProject();
    jiraProject1.setKey("projectKey1");
    jiraProject1.setName("Project One");
    JiraIssueType jiraIssueType1 = new JiraIssueType();
    jiraIssueType1.setId(1);
    jiraIssueType1.setName("Bug");
    jiraProject1.setIssueTypes(Collections.singletonList(jiraIssueType1));

    jiraProject2 = new JiraProject();
    jiraProject2.setKey("projectKey2");
    jiraProject2.setName("Project Two");
    JiraIssueType jiraIssueType2 = new JiraIssueType();
    jiraIssueType2.setId(2);
    jiraIssueType2.setName("Story");
    jiraProject2.setIssueTypes(Collections.singletonList(jiraIssueType2));

    jiraProjectKeyToRecipient = new HashMap<>();
    jiraProjectKeyToRecipient.put(jiraProject1.getKey(), getJiraRecipient(jiraProject1));
    jiraProjectKeyToRecipient.put(jiraProject2.getKey(), getJiraRecipient(jiraProject2));

    when(jiraService.isEnabled()).thenReturn(true);
    doReturn(Arrays.asList(jiraProject1, jiraProject2)).when(jiraService).getProjectsWithAcceptableIssueTypes();
  }

  private String getJiraRecipient(JiraProject jiraProject) {
    return jiraProject.getName() + " (" + jiraProject.getIssueTypes().get(0).getName() + ")";
  }
}
