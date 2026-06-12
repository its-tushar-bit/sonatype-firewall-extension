/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.back;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.insight.brain.model.Color.dark_blue;
import static com.sonatype.insight.brain.model.Color.dark_red;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.policy.conditions.DataSourceConditionType.HAS_NO_SUPPORT_FOR;
import static com.sonatype.insight.brain.model.policy.conditions.DataSourceConditionType.HAS_SUPPORT_FOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.WebElementCondition;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ActionsSection;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.ConstraintSection;
import com.sonatype.clm.testing.functional.elements.ConstraintSection.ConstraintEditSection;
import com.sonatype.clm.testing.functional.elements.ConstraintSection.ConstraintEditSection.AgeConditionEditSection;
import com.sonatype.clm.testing.functional.elements.ConstraintSection.ConstraintEditSection.CoordinatesCondition;
import com.sonatype.clm.testing.functional.elements.ConstraintSection.ConstraintEditSection.DropdownConditionEditSection;
import com.sonatype.clm.testing.functional.elements.ConstraintSection.ConstraintEditSection.InputConditionEditSection;
import com.sonatype.clm.testing.functional.elements.ConstraintSection.ConstraintSummary;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.NotificationsSection;
import com.sonatype.clm.testing.functional.elements.NotificationsSection.AddNotificationItem;
import com.sonatype.clm.testing.functional.elements.NxDeleteModal;
import com.sonatype.clm.testing.functional.elements.NxFormSelect;
import com.sonatype.clm.testing.functional.elements.NxFormSelect.Option;
import com.sonatype.clm.testing.functional.elements.OwnerDetailSidebar;
import com.sonatype.clm.testing.functional.elements.PolicyInheritsToSection;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.elements.SummarySection;
import com.sonatype.clm.testing.functional.elements.ThreatDropdownSelector;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.elements.UnsavedModal;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.PolicyEditorPage;
import com.sonatype.clm.testing.functional.utils.InputUtils;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.jira.JiraIssueType;
import com.sonatype.insight.brain.jira.JiraProject;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.ComponentDataSource;
import com.sonatype.insight.brain.model.component.HygieneRating;
import com.sonatype.insight.brain.model.component.IntegrityRating;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.ConditionType;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.AbstractConditionType;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ComponentEndOfLifeConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.DataSourceConditionType;
import com.sonatype.insight.brain.model.policy.conditions.DependencyTypeConditionType;
import com.sonatype.insight.brain.model.policy.conditions.HygieneRatingConditionType;
import com.sonatype.insight.brain.model.policy.conditions.IdentificationSourceConditionType;
import com.sonatype.insight.brain.model.policy.conditions.IntegrityRatingConditionType;
import com.sonatype.insight.brain.model.policy.conditions.KevStatusConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseStatusConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupLevelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.conditions.PackageUrlConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ProprietaryConditionType;
import com.sonatype.insight.brain.model.policy.conditions.RelativePopularityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityCategoryConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;
import com.sonatype.insight.brain.model.policy.notifications.JiraNotification;
import com.sonatype.insight.brain.model.policy.notifications.Notification;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.organization.OrganizationService;
import com.sonatype.insight.brain.product.license.LicensedConditionTypesListener;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.AdditionalAnswers;

public abstract class AbstractPolicyEditorTest
    extends AbstractFunctionalTest
{
  protected static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  protected static final String COMPLIANCE = new ComplianceStageType().getId();

  private Owner currentOwner;

  private PolicyDAO policyDAO;

  private LicenseThreatGroupDAO licenseThreatGroupDAO;

  private RoleDAO roleDAO;

  private JiraProject jiraProject;

  @BeforeClass
  public static void boot() {
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    loginAsAdmin();
  }

  @Before
  public void setUp() {
    policyDAO = lookup(PolicyDAO.class);
    licenseThreatGroupDAO = lookup(LicenseThreatGroupDAO.class);
    roleDAO = lookup(RoleDAO.class);

    // Manually trigger the LicensedConditionTypesListener to enable condition types
    // This is necessary because functional tests use TestProductLicenseManager which bypasses
    // the normal license change notification system. This must be done in @Before (not static block)
    // because the condition-type bootstrap resets all condition types to their default
    // enabled/disabled state during dependency injection.
    LicensedConditionTypesListener listener = lookup(LicensedConditionTypesListener.class);
    listener.productLicenseChanged();
  }

  private static void assertCondition(
      Condition actualCondition,
      String expectedType,
      String expectedOp,
      String expectedValue)
  {
    assertThat(actualCondition.getConditionTypeId()).isEqualTo(expectedType);
    assertThat(actualCondition.getOperator()).isEqualTo(expectedOp);
    assertThat(actualCondition.getValue()).isEqualTo(expectedValue);
  }

  private void setupJiraService() throws IOException {
    jiraProject = new JiraProject();
    jiraProject.setKey("key1");
    jiraProject.setName("Project One");
    JiraIssueType jiraIssueType = new JiraIssueType();
    jiraIssueType.setId(1);
    jiraIssueType.setName("Bug");
    jiraProject.setIssueTypes(Collections.singletonList(jiraIssueType));

    when(jiraService.isEnabled()).thenReturn(true);
    doReturn(Collections.singletonList(jiraProject)).when(jiraService).getProjectsWithAcceptableIssueTypes();
  }

  protected void init(Owner currentOwner) {
    this.currentOwner = currentOwner;
    refreshOrOpen(OwnerSummaryPage.url(currentOwner));
    OwnerSummaryPage.summaryTile().name().shouldHave(text(currentOwner.getName()));
  }

  @Test
  public void testCreatePolicy() {
    setFeatures(LicensedFeature.RELEASE_INTEGRITY,
        LicensedFeature.HYGIENE,
        LicensedFeature.POLICY_MONITORING,
        LicensedFeature.ENFORCEMENT,
        LicensedFeature.NOTIFICATIONS,
        LicensedFeature.WEBHOOKS_FOR_APPLICATIONS,
        LicensedFeature.DASHBOARD,
        LicensedFeature.POLICY_MANAGEMENT,
        LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.COMPONENT_LABELS,
        LicensedFeature.CUSTOM_POLICIES);

    tempEntity.newLicenseThreatGroup(ROOT_ORGANIZATION_ID, "Banned", 10);
    tempEntity.newLicenseThreatGroup(ROOT_ORGANIZATION_ID, "Liberal", 0);

    if (OwnerType.ORGANIZATION.equals(currentOwner.getType())) {
      tempEntity.newTag(currentOwner.getId(), "PolicyEditorTest category");
    }
    Label sampleLabel = tempEntity.newLabel(currentOwner.getId(), "Sample Label");
    Webhook webhook =
        tempEntity.newWebhookWithSecret("http://localhost", Collections.singleton(WebhookEventType.POLICY_ALERT));
    Webhook webhookWithDescription = tempEntity.newWebhookWithSecret("http://localhost",
        Collections.singleton(WebhookEventType.POLICY_ALERT), "description");

    refreshOrOpen(OwnerSummaryPage.url(currentOwner));

    // add extra timeout for visibility of the form as this has occasionally caused test instability
    OwnerSummaryPage.policyTile().getElement().shouldBe(visible, Duration.ofSeconds(5));
    OwnerSummaryPage.policyTile().addPolicyButton().click();

    assertNewPolicyStateIsCorrect();
    testCreatePolicy_summarySection();
    testCreatePolicy_inheritanceSection();
    testCreatePolicy_actionsSection();
    testCreatePolicy_notificationsSection();
    testCreatePolicy_constraintSection();
    PolicyEditorPage.savePolicy();

    // make sure we reset back to clean state
    assertNewPolicyStateIsCorrect();

    Policy newPolicy = getPolicyByName("New Policy");

    assertThat(newPolicy).isNotNull();
    assertThat(newPolicy.getConstraints()).hasSize(1);
    Constraint constraint = newPolicy.getConstraints().get(0);
    assertThat(constraint.getName()).isEqualTo("New Constraint");
    assertThat(constraint.getOperator()).isEqualTo(LogicalOperator.OR);

    assertThat(constraint.getConditions()).hasSize(30);
    assertCondition(constraint.getConditions().get(0), AgeInDaysConditionType.ID, "older than",
        Integer.toString(3 * 365));
    assertCondition(constraint.getConditions().get(1), CoordinatesConditionType.ID, "match",
        "maven:org.apache:tomcat:5.0.28:jar:javadoc");
    assertCondition(constraint.getConditions().get(2), CoordinatesConditionType.ID, "do not match",
        "a-name:jquery::1.0.28");
    assertCondition(constraint.getConditions().get(3), CoordinatesConditionType.ID, "match",
        "pypi:MarkupSafe:1.1.0:cp37:tar.gz");
    assertCondition(constraint.getConditions().get(4), LabelConditionType.ID, "is", sampleLabel.getId());
    assertCondition(constraint.getConditions().get(5),
        LicenseConditionType.ID, "is", "ABBYY-RTR-SDK-LA");
    assertCondition(constraint.getConditions().get(6), LicenseStatusConditionType.ID, "is not",
        LicenseOverrideStatus.CONFIRMED.name());
    assertCondition(constraint.getConditions().get(7), LicenseThreatGroupConditionType.ID, "is",
        licenseThreatGroupDAO.getByName("Liberal").get(0).getId());
    assertCondition(constraint.getConditions().get(8),
        LicenseThreatGroupLevelConditionType.ID, ">=", "5");
    assertCondition(constraint.getConditions().get(9),
        SecurityVulnerabilitySeverityConditionType.ID, ">", "1");
    assertCondition(constraint.getConditions().get(10), SecurityVulnerabilityStatusConditionType.ID, "is",
        SecurityVulnerabilityOverrideStatus.NOT_APPLICABLE.name());
    assertCondition(constraint.getConditions().get(11),
        RelativePopularityConditionType.ID, "=", "50");
    assertCondition(constraint.getConditions().get(12), MatchStateConditionType.ID, "is not",
        MatchState.UNKNOWN.getId());
    assertCondition(constraint.getConditions().get(13),
        ProprietaryConditionType.ID, "is false", null);
    assertCondition(constraint.getConditions().get(14), IdentificationSourceConditionType.ID, "is not",
        IdentificationSource.MANUAL.getId());
    assertCondition(constraint.getConditions().get(15), PackageUrlConditionType.ID, "matches",
        "pkg:maven/g/a@v?classifier=*&type=jar");
    assertCondition(constraint.getConditions().get(16), PackageUrlConditionType.ID, "matches",
        "pkg:maven/*/a@*?classifier=*&type=jar");
    assertCondition(constraint.getConditions().get(17), PackageUrlConditionType.ID, "does not match",
        "pkg:npm/a@v");
    assertCondition(constraint.getConditions().get(18), PackageUrlConditionType.ID, "matches",
        "pkg:pypi/*/*/a@*?extension=*&qualifier=*");
    assertCondition(constraint.getConditions().get(19),
        PackageUrlConditionType.ID, "matches", "pkg:golang/*/*/a@*");
    assertCondition(constraint.getConditions().get(20), PackageUrlConditionType.ID, "matches",
        "pkg:conan/*/*/a@*?channel=*");
    assertCondition(constraint.getConditions().get(21),
        PackageUrlConditionType.ID, "matches", "pkg:golang/*/*/*@*");
    assertCondition(constraint.getConditions().get(22), HygieneRatingConditionType.ID, "is not",
        HygieneRating.getById("4").getId());
    assertCondition(constraint.getConditions().get(23), DataSourceConditionType.ID, HAS_NO_SUPPORT_FOR,
        ComponentDataSource.getById("identity").getId());
    assertCondition(constraint.getConditions().get(24),
        DependencyTypeConditionType.ID, "is not", "transitive");
    assertCondition(constraint.getConditions().get(25),
        SecurityVulnerabilityCategoryConditionType.ID, "is not",
        "configuration");
    assertCondition(constraint.getConditions().get(26), IntegrityRatingConditionType.ID, "is not",
        IntegrityRating.getById("1").getId());
    assertCondition(constraint.getConditions().get(27),
        DependencyTypeConditionType.ID, "is", "innersource");
    assertCondition(constraint.getConditions().get(28),
        ComponentEndOfLifeConditionType.ID, "is false", null);
    assertThat(newPolicy.getActions().get(Stage.ID_BUILD)).isEqualTo("warn");

    assertThat(newPolicy.getNotifications().getUserNotifications()).hasSize(1);
    assertThat(newPolicy.getNotifications().getUserNotifications().get(0).getEmailAddress())
        .isEqualTo("aaa@sonatype.com");
    assertThat(newPolicy.getNotifications().getUserNotifications().get(0).getStageIds())
        .containsExactlyInAnyOrder(Stage.ID_BUILD);

    assertThat(newPolicy.getNotifications().getRoleNotifications()).hasSize(1);
    assertThat(newPolicy.getNotifications().getRoleNotifications().get(0).getStageIds())
        .containsExactlyInAnyOrder(Notification.CONTINUOUS_MONITORING);

    assertThat(newPolicy.getNotifications().getWebhookNotifications()).hasSize(2);
    assertThat(newPolicy.getNotifications().getWebhookNotifications().get(0).getWebhookId()).isEqualTo(webhook.getId());
    assertThat(newPolicy.getNotifications().getWebhookNotifications().get(0).getStageIds())
        .containsExactlyInAnyOrder(Stage.ID_STAGE_RELEASE);
    assertThat(newPolicy.getNotifications().getWebhookNotifications().get(1).getWebhookId())
        .isEqualTo(webhookWithDescription.getId());

    testCreatePolicy_navigatingAwayWithUnsavedData();
  }

  @Test
  public void testEditPolicy() {
    String ownerId = currentOwner.getId();
    Tag[] categories = createCategories(
        OwnerType.ORGANIZATION.equals(currentOwner.getType()) ? ownerId : currentOwner.getParentOwnerId());
    Policy policy = createPolicy(ownerId, categories, false);

    refresh();

    OwnerSummaryPage.policyTile().localPolicyList().row(1).click();
    assertEditPolicyStateIsCorrect(policy, categories[0], categories[1], false);

    testEditPolicy_summarySection(policy.getId());
    testEditPolicy_inheritanceSection();
    testEditPolicy_constraintSection(policy);
    testEditPolicy_actionsSection(policy);
    testEditPolicy_notificationsSection(policy);
    testDeletePolicy(policyDAO.getById(policy.getId()));
  }

  // This test is for CLM-37950 to check that we don't get an error
  // i.e. "An error occurred loading data. Organization with ID null does not exist."
  // instead of the expected policy tile
  @Test
  public void testNavigateToOrgsAndPoliciesFromEditPolicy() {
    tempEntity.newPolicy(ROOT_ORGANIZATION_ID);
    refresh();
    SidebarNavigation.policiesNavigationButton().click();
    OwnerSummaryPage.policyTile().localPolicyList().shouldBe(visible).row(1).shouldBe(visible).click();
    OrganizationService organizationService = lookup(OrganizationService.class);
    OrganizationService delayedOrganizationService =
        mock(OrganizationService.class, AdditionalAnswers.delegatesTo(organizationService));
    doAnswer(invocation -> {
      // Delay the REST call to /rest/organization/null to ensure it's the last request that gets fulfilled
      Selenide.sleep(2000);
      return organizationService.getOrganization("null");
    }).when(delayedOrganizationService).getOrganization("null");
    mocks.put(OrganizationService.class, delayedOrganizationService);

    SidebarNavigation.policiesNavigationButton().click();
    // Make sure the delayed call completes
    Selenide.sleep(3000);

    OwnerSummaryPage.policyTile().shouldBe(visible);
  }

  @Test
  public void testFoundation_Firewall() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION, ProductLicenseDetails.PRODUCT_FIREWALL);

    String ownerId = currentOwner.getId();
    Tag[] categories = createCategories(
        OwnerType.ORGANIZATION.equals(currentOwner.getType()) ? ownerId : currentOwner.getParentOwnerId());
    Policy policy = createPolicy(ownerId, categories, false);

    refresh();

    OwnerSummaryPage.policyTile().localPolicyList().row(1).click();
    assertEditPolicyStateIsCorrect(policy, categories[0], categories[1], false, true, true, false, true);
  }

  @Test
  public void testFoundation() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);

    String ownerId = currentOwner.getId();
    Tag[] categories = createCategories(
        OwnerType.ORGANIZATION.equals(currentOwner.getType()) ? ownerId : currentOwner.getParentOwnerId());
    Policy policy = createPolicy(ownerId, categories, false);

    refresh();

    OwnerSummaryPage.policyTile().localPolicyList().row(1).click();
    assertEditPolicyStateIsCorrect(policy, categories[0], categories[1], false, true, true, true, true);
  }

  @Test
  public void testLifecycle_rendersCorrectlyWithSbomManagerSupport() {
    setFeatures(LicensedFeature.POLICY_MANAGEMENT, LicensedFeature.POLICY_READ_ONLY,
        LicensedFeature.POLICY_GRANDFATHERING, LicensedFeature.ENFORCEMENT, LicensedFeature.SBOM_MANAGER,
        LicensedFeature.ORGS_AND_APPS, LicensedFeature.NOTIFICATIONS, LicensedFeature.CUSTOM_POLICIES);
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS, ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);

    String ownerId = currentOwner.getId();
    Tag[] categories = createCategories(
        OwnerType.ORGANIZATION.equals(currentOwner.getType()) ? ownerId : currentOwner.getParentOwnerId());
    createPolicy(ownerId, categories, false);

    refreshOrOpen(OwnerSummaryPage.url(currentOwner));

    OwnerSummaryPage.policyTile().localPolicyList().row(1).click();

    SummarySection summarySection = PolicyEditorPage.summarySection();
    summarySection.policyName().input().shouldBe(visible, enabled);
    summarySection.threatLevel().shouldNotHave(cssClass("disabled"));
    summarySection.legacyViolationCheckbox().shouldBe(visible, enabled);

    if (OwnerType.ORGANIZATION.equals(currentOwner.getType())) {
      PolicyInheritsToSection inheritanceSection = PolicyEditorPage.inheritanceSection();
      inheritanceSection.allChildrenInheritRadio().shouldBe(visible, enabled);
    }

    ConstraintSection constraintsSection = PolicyEditorPage.constraintSection();
    constraintsSection.addConstraintButton().shouldBe(visible, enabled);
    constraintsSection.constraintSummary(0).editConstraintButton().shouldBe(visible, enabled);

    PolicyEditorPage.actionsSection().title().shouldBe(visible);
    PolicyEditorPage.actionsSection().headers().get(0).shouldNotHave(text(COMPLIANCE));
    PolicyEditorPage.actionsSection().table().shouldBe(visible);

    PolicyEditorPage.notificationsSection()
        .headers()
        .get(0)
        .shouldBe(visible)
        .shouldNotHave(text(COMPLIANCE));
    PolicyEditorPage.deleteButton().shouldBe(visible);
    OwnerDetailSidebar.policyGroup().shouldBe(visible);
  }

  @Test
  public void testJIRA() throws IOException {
    setupJiraService();

    refreshOrOpen(OwnerSummaryPage.url(currentOwner));
    OwnerSummaryPage.policyTile().addPolicyButton().click();

    PolicyEditorPage.summarySection().policyName().input().val("New Policy");

    ConstraintEditSection newConstraint = PolicyEditorPage.constraintSection().constraintEditor(0);
    newConstraint.name().val("New Constraint");

    AgeConditionEditSection ageCondition = newConstraint.ageCondition(0);
    ageCondition.value().age().val("3");

    AddNotificationItem addNotification = NotificationsSection.addNotification();

    // add jira notifications
    addNotification.notificationType().chooseOption("JIRA");
    addNotification.addButton().shouldBe(disabled);

    addNotification.issueType()
        .shouldBe(visible)
        .shouldBe(disabled)
        .shouldHave(AddNotificationItem.ISSUE_TYPE_NEEDS_PROJECT);
    addNotification.project().shouldBe(visible).selectedItem().click();
    addNotification.project().listItems().findBy(text(jiraProject.getName())).click();
    addNotification.addButton().shouldBe(disabled);

    addNotification.issueType().shouldBe(visible).selectedItem().click();
    addNotification.issueType().listItems().findBy(text(jiraProject.getIssueTypes().get(0).getName())).click();
    addNotification.addButton().shouldNotBe(disabled).click();
    addNotification.addButton().shouldBe(disabled);
    addNotification.project().shouldBe(visible);
    addNotification.issueType()
        .shouldBe(visible)
        .shouldBe(disabled)
        .shouldHave(AddNotificationItem.ISSUE_TYPE_NEEDS_PROJECT);

    addNotification.project().shouldHave(text("No applicable projects available."));

    // Uncomment when fixing CLM-18677
    // NotificationsSection.notifications().shouldHave(texts("Project One (Bug)"));
    PolicyEditorPage.savePolicy();

    // verify persisted policy
    Policy policy = getPolicyByName("New Policy");
    List<JiraNotification> notifications = policy.getNotifications().getJiraNotifications();
    assertThat(notifications).hasSize(1);
    assertThat(notifications.get(0).getProjectKey()).isEqualTo("key1");
    assertThat(notifications.get(0).getIssueTypeId()).isEqualTo(1);

    refreshOrOpen(PolicyEditorPage.urlToEdit(currentOwner, policy.getId()));

    NotificationsSection.notificationFor("Project One (Bug)").proxy().hover();
    Tooltip.get()
        .shouldBe(visible)
        .shouldHave(text("Jira notifications are not available for policy violations at Proxy stage."));
    NotificationsSection.notificationFor("Project One (Bug)").deleteButton().click();
    NotificationsSection.notifications()
        .shouldHave(size(1))
        .get(0)
        .shouldHave(text("No notifications configured"));

    PolicyEditorPage.savePolicy();

    policy = getPolicyByName("New Policy");
    assertThat(policy.getNotifications().getJiraNotifications()).isEmpty();
  }

  private Policy getPolicyByName(String policyName) {
    for (Policy p : policyDAO.getByOwnerId(currentOwner.getId())) {
      if (p.getName().equals(policyName)) {
        return p;
      }
    }
    return null;
  }

  private void testCreatePolicy_navigatingAwayWithUnsavedData() {
    HashMap<Class<? extends ConditionType>, Option> conditionTypesOptionMap = conditionsToOptionMap();

    String editorUrl = WebDriverRunner.getWebDriver().getCurrentUrl();
    UnsavedModal unsavedModal = new UnsavedModal();

    ConstraintEditSection constraintEditor = PolicyEditorPage.constraintSection().constraintEditor(0);

    // make sure certain fields are making the editor dirty
    ScrollUtil.scrollIntoView(PolicyEditorPage.constraintSection().header());
    constraintEditor.addConditionButton().click();
    handleUnsavedChangesDialog(unsavedModal, editorUrl);
    constraintEditor.condition(1).deleteConditionButton().click();
    constraintEditor.condition(0)
        .type()
        .chooseOptionWithHidden(conditionTypesOptionMap.get(CoordinatesConditionType.class));
    handleUnsavedChangesDialog(unsavedModal, editorUrl);
    constraintEditor.condition(0)
        .type()
        .chooseOptionWithHidden(conditionTypesOptionMap.get(AgeInDaysConditionType.class));

    // Assert no Modal appears when the editor is clean
    unsavedModal.shouldBe(hidden);
    SidebarNavigation.dashboardNavigationButton().shouldBe(visible, enabled).click();
    unsavedModal.shouldBe(hidden);
    waitUntilUrl(DashboardPage.url());
    DashboardPage.dashboardContainer().shouldBe(visible);

    back();
    waitUntilUrl(editorUrl);

    PolicyEditorPage.constraintSection().constraintEditor(0).ageCondition(0).value().age().val("10");

    handleUnsavedChangesDialog(unsavedModal, editorUrl);

    SidebarNavigation.dashboardNavigationButton().click();
    unsavedModal.continueButton().shouldBe(visible).click();
    waitUntilUrl(DashboardPage.url());
    DashboardPage.dashboardContainer().shouldBe(visible);

    back();
    waitUntilUrl(editorUrl);
    DashboardPage.dashboardContainer().shouldBe(hidden);
  }

  private void handleUnsavedChangesDialog(UnsavedModal unsavedModal, String url) {
    // Assert Modal appears when the editor is dirty and continues to new page
    SidebarNavigation.dashboardNavigationButton().click();
    unsavedModal.cancelButton().shouldBe(visible).click();
    waitUntilUrl(url);
    DashboardPage.dashboardContainer().shouldBe(hidden);
  }

  private Tag[] createCategories(String ownerId) {
    Tag category1 = tempEntity.newTag(ownerId, "Cat_1", dark_blue);
    Tag category2 = tempEntity.newTag(ownerId, "Cat_2", dark_red);
    return new Tag[]{category1, category2};
  }

  private Policy createPolicy(String ownerId, Tag[] categories, boolean policyActionsOverrideAllowed) {
    Policy policy = tempEntity.newPolicy(ownerId, "original name", 1);
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

    policy.setConstraints(Arrays.asList(constraint1, constraint2, constraint3));

    policy.setPolicyActionsOverrideAllowed(policyActionsOverrideAllowed);

    if (OwnerType.ORGANIZATION.equals(currentOwner.getType())) {
      tempEntity.newPolicyTag(policy.getId(), categories[0].getId());
    }

    policy.setAction(Stage.ID_DEVELOP, Action.ID_WARN);
    policy.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policy.getNotifications()
        .add(
            new UserNotification("test@foo.com", Stage.ID_BUILD, Notification.CONTINUOUS_MONITORING));
    String roleName = "Developer";
    policy.getNotifications().add(new RoleNotification(roleDAO.getByName(roleName).getId(), roleName, Stage.ID_BUILD));

    tempEntity.newLicenseThreatGroup(currentOwner.getId(), "my LTG 2", 10);

    policyDAO.update(policy);
    return policy;
  }

  private void testEditPolicy_summarySection(String policyId) {
    Policy policy = policyDAO.getById(policyId);
    // Sanity check to verify that the initial value is as expected.
    assertThat(policy.isLegacyViolationAllowed()).isFalse();

    SummarySection summary = PolicyEditorPage.summarySection();
    summary.policyName().input().val("updated name");
    summary.legacyViolationCheckbox().shouldBe(visible).shouldNotBe(selected);
    summary.legacyViolationCheckbox().click();
    PolicyEditorPage.savePolicy();

    changeThreatLevel(6);
    PolicyEditorPage.savePolicy();

    refresh();

    PolicyEditorPage.title().shouldHave(text("Policy Settings"));
    summary.policyName().input().shouldBe(visible).shouldHave(value("updated name"));
    summary.legacyViolationCheckbox().shouldBe(visible).shouldBe(selected);
    ThreatDropdownSelector.selectedThreatLabel().shouldBe(text("6"));

    Policy updatedPolicy = policyDAO.getById(policy.getId());
    assertThat(updatedPolicy.getName()).isEqualTo("updated name");
    assertThat(updatedPolicy.isLegacyViolationAllowed()).isTrue();
    assertThat(updatedPolicy.getThreatLevel()).isEqualTo(6);
  }

  private void testEditPolicy_constraintSection(Policy policy) {
    testEditPolicy_constraintSection_summaries(policy);
    testEditPolicy_constraintSection_editors(policy);
  }

  private void testEditPolicy_constraintSection_summaries(Policy policy) {
    List<Constraint> constraints = policy.getConstraints();
    ConstraintSection constraintSection = PolicyEditorPage.constraintSection();
    constraintSection.addConstraintButton().shouldBe(visible, enabled);
    constraintSection.constraintSummaries().shouldHave(size(constraints.size()));

    ConstraintSection.ConstraintSummary constraintSummary1 = constraintSection.constraintSummary(0);
    constraintSummary1.name().shouldHave(text(constraints.get(0).getName()));

    List<Condition> conditions = constraints.get(0).getConditions();
    constraintSummary1.subheader()
        .shouldHave(ConstraintSection.ConstraintSummary
            .subheaderText(conditions.size(), constraints.get(0).getOperator().toString()));
    constraintSummary1.conditions().shouldHave(size(conditions.size()));
    constraintSummary1.deleteConstraintButton().shouldBe(visible, enabled);
    constraintSummary1.editConstraintButton().shouldBe(visible, enabled);

    constraintSummary1.condition(0).shouldHave(text("Age older than 2 Years"));
    constraintSummary1.conditionUnsupportedMessages().shouldHave(size(0));

    ConstraintSection.ConstraintSummary constraintSummary2 = constraintSection.constraintSummary(1);
    constraintSummary2.name().shouldHave(text(constraints.get(1).getName()));

    conditions = constraints.get(1).getConditions();
    constraintSummary2.subheader()
        .shouldHave(ConstraintSection.ConstraintSummary
            .subheaderText(conditions.size(), constraints.get(1).getOperator().toString()));
    constraintSummary2.conditions().shouldHave(size(conditions.size()));
    constraintSummary2.deleteConstraintButton().shouldBe(visible, enabled);
    constraintSummary2.editConstraintButton().shouldBe(visible, enabled);

    constraintSummary2.condition(0).shouldHave(text("License Threat Group is my LTG"));
    constraintSummary2.condition(1).shouldHave(text("Label is my Label"));
    constraintSummary2.conditionUnsupportedMessages().shouldHave(size(0));

    ConstraintSection.ConstraintSummary constraintSummary3 = constraintSection.constraintSummary(2);
    constraintSummary3.name().shouldHave(text(constraints.get(2).getName()));

    conditions = constraints.get(2).getConditions();
    constraintSummary3.subheader()
        .shouldHave(ConstraintSection.ConstraintSummary
            .subheaderText(conditions.size(), constraints.get(2).getOperator().toString()));
    constraintSummary3.conditions().shouldHave(size(conditions.size()));
    constraintSummary3.deleteConstraintButton().shouldBe(visible, enabled);
    constraintSummary3.editConstraintButton().shouldBe(visible, enabled);

    constraintSummary3.condition(0).shouldHave(text("Relative Popularity (Percentage) less than 50"));
    constraintSummary3.condition(1).shouldHave(text("Coordinates do not match maven:blah:blah:blah"));
    constraintSummary3.conditionUnsupportedMessages().shouldHave(size(0));
  }

  private void testEditPolicy_constraintSection_editors(Policy policy) {
    HashMap<Class<? extends ConditionType>, Option> conditionTypesOptionMap = conditionsToOptionMap();

    List<Constraint> constraints = policy.getConstraints();
    ConstraintSection constraintSection = PolicyEditorPage.constraintSection();

    constraintSection.constraintEditors().shouldHave(size(0));
    constraintSection.constraintSummary(0).editConstraintButton().shouldBe(visible, enabled).click();
    constraintSection.constraintEditors().shouldHave(size(1));

    ConstraintEditSection constraintEdit = constraintSection.constraintEditor(0);
    constraintEdit.conditionUnsupportedMessages().shouldHave(size(0));

    constraintEdit.operator().shouldHave(text("all"));
    constraintEdit.name().shouldHave(value(constraints.get(0).getName())).val("New Constraint Name");
    PolicyEditorPage.savePolicy();

    policy = policyDAO.getById(policy.getId());
    assertThat(policy.getConstraints().get(0).getName()).isEqualTo("New Constraint Name");

    constraintEdit.conditions().shouldHave(size(1));
    constraintEdit.ageCondition(0).deleteConditionButton().shouldBe(visible, disabled);
    constraintEdit.ageCondition(0).value().age().shouldHave(value("2")).val("3");
    constraintEdit.ageCondition(0).value().modifier().shouldHave(text("Years")).click();
    constraintEdit.ageCondition(0).value().modifier().listItem(2).shouldHave(text("Months")).click();
    constraintEdit.ageCondition(0).operator().shouldHave(text("older than")).click();
    constraintEdit.ageCondition(0).operator().listItem(1).shouldHave(text("younger than")).click();
    PolicyEditorPage.savePolicy();
    ScrollUtil.scrollIntoView(constraintSection.header());

    Condition updatedAgeCondition = policyDAO.getById(policy.getId())
        .getConstraints()
        .get(0)
        .getConditions()
        .get(0);
    assertThat(updatedAgeCondition.getConditionTypeId()).isEqualTo(AgeInDaysConditionType.ID);
    assertThat(updatedAgeCondition.getValue()).isEqualTo(Integer.toString(3 * 30));
    assertThat(updatedAgeCondition.getOperator()).isEqualTo("younger than");

    constraintEdit.addConditionButton().shouldBe(visible, enabled).click();
    constraintEdit.conditions().shouldHave(size(2));
    constraintEdit.condition(1)
        .type()
        .chooseOptionWithHidden(conditionTypesOptionMap.get(LicenseThreatGroupConditionType.class));
    constraintEdit.dropdownCondition(1).operator().shouldHave(text("is")).click();
    constraintEdit.dropdownCondition(1).operator().listItem(1).shouldHave(text("is not")).click();
    constraintEdit.dropdownCondition(1).value().shouldHave(text("my LTG")).click();
    constraintEdit.dropdownCondition(1).value().listItem(1).shouldHave(text("my LTG 2")).click();
    PolicyEditorPage.savePolicy();
    ScrollUtil.scrollIntoView(constraintSection.header());

    constraints = policyDAO.getById(policy.getId()).getConstraints();
    assertThat(constraints.get(0).getConditions()).hasSize(2);

    Condition ltgCondition = constraints.get(0).getConditions().get(1);
    assertThat(ltgCondition.getConditionTypeId()).isEqualTo(LicenseThreatGroupConditionType.ID);
    assertThat(ltgCondition.getValue())
        .isEqualTo(licenseThreatGroupDAO.getByOwnerIdAndName(currentOwner.getId(), "my LTG 2").getId());
    assertThat(ltgCondition.getOperator()).isEqualTo("is not");

    constraintEdit.addConditionButton().shouldBe(visible, enabled).click();
    constraintEdit.conditions().shouldHave(size(3));
    constraintEdit.condition(2).type().shouldHave(text("Age"));

    CoordinatesCondition coordConditionEditor = constraintEdit.coordinatesCondition(2);
    coordConditionEditor.type().chooseOptionWithHidden(conditionTypesOptionMap.get(CoordinatesConditionType.class));
    coordConditionEditor.setOperator("do not match");
    coordConditionEditor.groupId().val("com.eclipse.*");
    coordConditionEditor.artifactId().val("*");
    coordConditionEditor.version().val("*");
    PolicyEditorPage.savePolicy();
    ScrollUtil.scrollIntoView(constraintSection.header());

    constraints = policyDAO.getById(policy.getId()).getConstraints();
    assertThat(constraints.get(0).getConditions()).hasSize(3);

    Condition coordinatesCondition = constraints.get(0).getConditions().get(2);
    assertThat(coordinatesCondition.getConditionTypeId()).isEqualTo(CoordinatesConditionType.ID);
    assertThat(coordinatesCondition.getValue()).isEqualTo("maven:com.eclipse.*:*:*:*:*");
    assertThat(coordinatesCondition.getOperator()).isEqualTo("do not match");

    constraintEdit.condition(2)
        .type()
        .chooseOptionWithHidden(conditionTypesOptionMap.get(SecurityVulnerabilitySeverityConditionType.class));
    constraintEdit.condition(2).operator().shouldHave(text("=")).click();
    constraintEdit.condition(2).operator().listItem(1).shouldHave(text("<"));
    constraintEdit.condition(2).operator().listItem(2).shouldHave(text("<="));
    constraintEdit.condition(2).operator().listItem(3).shouldHave(text(">"));
    constraintEdit.condition(2).operator().listItem(4).shouldHave(text(">=")).click();
    constraintEdit.inputCondition(2).value().val("1");
    PolicyEditorPage.savePolicy();
    ScrollUtil.scrollIntoView(constraintSection.header());

    constraints = policyDAO.getById(policy.getId()).getConstraints();
    assertThat(constraints.get(0).getConditions()).hasSize(3);

    Condition securityVulnerabilityCondition = constraints.get(0).getConditions().get(2);
    assertThat(securityVulnerabilityCondition.getConditionTypeId())
        .isEqualTo(SecurityVulnerabilitySeverityConditionType.ID);
    assertThat(securityVulnerabilityCondition.getValue()).isEqualTo("1");
    assertThat(securityVulnerabilityCondition.getOperator()).isEqualTo(">=");

    constraintEdit.conditionUnsupportedMessages().shouldHave(size(0));

    // Check that severity can be set to 0 as well
    constraintEdit.inputCondition(2).value().val("0");
    PolicyEditorPage.savePolicy();
    ScrollUtil.scrollIntoView(constraintSection.header());

    constraintEdit.condition(0).deleteConditionButton().shouldBe(visible, enabled);
    constraintEdit.condition(1).deleteConditionButton().shouldBe(visible, enabled);
    constraintEdit.condition(2).deleteConditionButton().shouldBe(visible, enabled).click();
    constraintEdit.conditions().shouldHave(size(2));

    constraintEdit.condition(1).deleteConditionButton().shouldBe(visible, enabled);
    constraintEdit.condition(0).deleteConditionButton().shouldBe(visible, enabled).click();
    constraintEdit.conditions().shouldHave(size(1));

    constraintEdit.condition(0).deleteConditionButton().shouldBe(visible, disabled);
    PolicyEditorPage.savePolicy();
    ScrollUtil.scrollIntoView(constraintSection.header());

    constraints = policyDAO.getById(policy.getId()).getConstraints();
    assertThat(constraints.get(0).getConditions()).hasSize(1);
  }

  @Test
  public void testDisabledHygieneRatingPolicyConditions() {
    setMissingFeatures(LicensedFeature.HYGIENE, LicensedFeature.SBOM_MANAGER);
    String ownerId = currentOwner.getId();
    Policy policy = createDisabledHygieneRatingPolicyConditions(ownerId);
    refresh();

    OwnerSummaryPage.policyTile().localPolicyList().row(1).click();
    waitUntilUrl(PolicyEditorPage.urlToEdit(currentOwner, policy.getId()));

    testDisabledPolicy_constraintSectionConditions_summaries(policy,
        new String[]{"Hygiene Rating is Exemplar", "Hygiene Rating is Laggard"},
        "Hygiene Rating condition is not supported by your license. Please revise the constraint.");
    testDisabledPolicy_constraintSectionConditions_editors(policy,
        new String[]{"Hygiene Rating", "Hygiene Rating"},
        "Hygiene Rating condition is not supported by your license. Please revise the constraint.",
        "Hygiene Rating", "is", "1");
  }

  @Test
  public void testDisabledIntegrityRatingPolicyConditions() {
    setMissingFeatures(LicensedFeature.RELEASE_INTEGRITY, LicensedFeature.SBOM_MANAGER);
    String ownerId = currentOwner.getId();
    Policy policy = createDisabledIntegrityRatingPolicyConditions(ownerId);
    refresh();

    OwnerSummaryPage.policyTile().localPolicyList().row(1).click();
    waitUntilUrl(PolicyEditorPage.urlToEdit(currentOwner, policy.getId()));

    testDisabledPolicy_constraintSectionConditions_summaries(policy,
        new String[]{"Integrity Rating is Suspicious", "Integrity Rating is Normal"},
        "Integrity Rating condition is not supported by your license. Please revise the constraint.");
    testDisabledPolicy_constraintSectionConditions_editors(policy,
        new String[]{"Integrity Rating", "Integrity Rating"},
        "Integrity Rating condition is not supported by your license. Please revise the constraint.",
        "Integrity Rating", "is", "1");
  }

  private Policy createDisabledHygieneRatingPolicyConditions(String ownerId) {
    Policy policy = tempEntity.newPolicy(ownerId, "original name", 1);
    Constraint constraint1 = new Constraint(policy.getId() + "1", "First Constraint with One Condition", null);
    constraint1.addCondition(new Condition(HygieneRatingConditionType.ID, "is", "1"));
    Constraint constraint2 = new Constraint(policy.getId() + "2", "Second Constraint with Two Conditions",
        LogicalOperator.AND);
    constraint2.addCondition(new Condition(HygieneRatingConditionType.ID, "is", "1"));
    constraint2.addCondition(new Condition(HygieneRatingConditionType.ID, "is", "4"));

    policy.setConstraints(Arrays.asList(constraint1, constraint2));

    policyDAO.update(policy);
    return policy;
  }

  private Policy createDisabledIntegrityRatingPolicyConditions(String ownerId) {
    Policy policy = tempEntity.newPolicy(ownerId, "original name", 1);
    Constraint constraint1 = new Constraint(policy.getId() + "1", "First Constraint with One Condition", null);
    constraint1.addCondition(new Condition(IntegrityRatingConditionType.ID, "is", "1"));
    Constraint constraint2 = new Constraint(policy.getId() + "2", "Second Constraint with Two Conditions",
        LogicalOperator.AND);
    constraint2.addCondition(new Condition(IntegrityRatingConditionType.ID, "is", "1"));
    constraint2.addCondition(new Condition(IntegrityRatingConditionType.ID, "is", "0"));

    policy.setConstraints(Arrays.asList(constraint1, constraint2));

    policyDAO.update(policy);
    return policy;
  }

  private void testDisabledPolicy_constraintSectionConditions_summaries(
      Policy policy,
      String[] expectedSummaryTexts,
      String expectedWarningMessage)
  {
    List<Constraint> constraints = policy.getConstraints();
    ConstraintSection constraintSection = PolicyEditorPage.constraintSection();
    constraintSection.addConstraintButton().shouldBe(visible, enabled);
    constraintSection.constraintSummaries().shouldHave(size(constraints.size()));

    ConstraintSection.ConstraintSummary constraintSummary1 = constraintSection.constraintSummary(0);
    constraintSummary1.name().shouldHave(text(constraints.get(0).getName()));

    List<Condition> conditions = constraints.get(0).getConditions();
    constraintSummary1.subheader()
        .shouldHave(ConstraintSection.ConstraintSummary
            .subheaderText(conditions.size(), constraints.get(0).getOperator().toString()));
    constraintSummary1.conditions().shouldHave(size(conditions.size()));
    constraintSummary1.deleteConstraintButton().shouldBe(visible, enabled);
    constraintSummary1.editConstraintButton().shouldBe(visible, enabled);

    constraintSummary1.conditionUnsupportedMessages().shouldHave(size(1));
    assertPolicySummary_constraintSectionDisabled(constraintSummary1, 0, expectedSummaryTexts[0],
        expectedWarningMessage);

    ConstraintSection.ConstraintSummary constraintSummary2 = constraintSection.constraintSummary(1);
    constraintSummary2.name().shouldHave(text(constraints.get(1).getName()));

    conditions = constraints.get(1).getConditions();
    constraintSummary2.subheader()
        .shouldHave(ConstraintSection.ConstraintSummary
            .subheaderText(conditions.size(), constraints.get(1).getOperator().toString()));
    constraintSummary2.conditions().shouldHave(size(conditions.size()));
    constraintSummary2.deleteConstraintButton().shouldBe(visible, enabled);
    constraintSummary2.editConstraintButton().shouldBe(visible, enabled);

    constraintSummary2.conditionUnsupportedMessages().shouldHave(size(1));
    assertPolicySummary_constraintSectionDisabled(constraintSummary2, 0, expectedSummaryTexts[0],
        expectedWarningMessage);
    assertPolicySummary_constraintSectionDisabled(constraintSummary2, 1, expectedSummaryTexts[1],
        expectedWarningMessage);
  }

  private void assertPolicySummary_constraintSectionDisabled(
      final ConstraintSummary constraintSummary,
      final int conditionIndex,
      final String expectedSummaryText,
      final String expectedWarningMessage)
  {
    constraintSummary.conditionUnsupportedMessage().shouldHave(text(expectedWarningMessage));
    constraintSummary.condition(conditionIndex).shouldHave(text(expectedSummaryText));
  }

  private void testDisabledPolicy_constraintSectionConditions_editors(
      Policy policy,
      String[] expectedConditionTexts,
      String expectedWarningText,
      String dropdownConditionType,
      String dropdownConditionOperator,
      String dropdownConditionValue)
  {
    ConstraintSection constraintSection = PolicyEditorPage.constraintSection();

    constraintSection.constraintEditors().shouldHave(size(0));
    constraintSection.constraintSummary(0).editConstraintButton().shouldBe(visible, enabled).click();
    constraintSection.constraintEditors().shouldHave(size(1));

    ConstraintEditSection constraintEdit = constraintSection.constraintEditor(0);

    constraintEdit.operator().shouldHave(text("all"));

    policy = policyDAO.getById(policy.getId());
    assertThat(policy.getConstraints().get(0).getName()).isEqualTo("First Constraint with One Condition");

    constraintEdit.conditions().shouldHave(size(1));
    constraintEdit.conditionUnsupportedMessages().shouldHave(size(1));
    assertPolicyEditor_constraintSectionDisabled(constraintEdit, 0, expectedConditionTexts[0],
        expectedWarningText);
    constraintEdit.dropdownCondition(0).deleteConditionButton().shouldBe(visible, disabled);
    constraintEdit.dropdownCondition(0).type().shouldBe(value(dropdownConditionType));
    constraintEdit.dropdownCondition(0).operator().shouldBe(value(dropdownConditionOperator));
    constraintEdit.dropdownCondition(0).value().shouldBe(value(dropdownConditionValue));

    constraintSection.constraintSummary(1).editConstraintButton().shouldBe(visible, enabled).click();
    constraintSection.constraintEditors().shouldHave(size(2));

    constraintEdit = constraintSection.constraintEditor(1);
    constraintEdit.conditionUnsupportedMessages().shouldHave(size(1));
    assertPolicyEditor_constraintSectionDisabled(constraintEdit, 0, expectedConditionTexts[0],
        expectedWarningText);
    assertPolicyEditor_constraintSectionDisabled(constraintEdit, 1, expectedConditionTexts[1],
        expectedWarningText);
  }

  private void assertPolicyEditor_constraintSectionDisabled(
      final ConstraintEditSection constraintEditSection,
      final int conditionIndex,
      final String expectedConditionText,
      final String expectedWarningMessage)
  {
    constraintEditSection.conditionUnsupportedMessage().shouldHave(text(expectedWarningMessage));
    constraintEditSection.condition(conditionIndex).type().shouldBe(value(expectedConditionText));
  }

  private void testEditPolicy_notificationsSection(Policy policy) {
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
    // should be last
    NotificationsSection.notifications().shouldHave(size(3));
    // Uncomment when fixing CLM-18677
    // NotificationsSection.notifications().get(2).shouldHave(text("aaa@sonatype.com"));

    // duplicate email validation
    addNotification.email().val("aaa@sonatype.com").shouldHave(attribute("aria-invalid", "true"));
    addNotification.addButton().shouldBe(disabled);

    // add role notifications
    addNotification.notificationType().chooseOption("Role");
    addNotification.email().shouldNot(exist);
    addNotification.role().shouldBe(visible).chooseOption("Application Evaluator");
    addNotification.addButton().shouldNotBe(disabled).click();
    addNotification.addButton().shouldBe(disabled);
    assertThat(addNotification.role().selectedItem().getText()).isEqualTo("-- Select Role --");
    addNotification.role().listItems().findBy(text("Application Evaluator")).shouldNot(exist);
    // should be last
    // Uncomment when fixing CLM-18677
    // NotificationsSection.notifications().get(3).shouldHave(text("Application Evaluator"));

    // Uncomment when fixing CLM-18677
    // NotificationsSection.notifications()
    // .shouldHave(texts("Developer", "test@foo.com", "aaa@sonatype.com", "Application Evaluator"));

    // switch from Role to Email notification type - email input should be empty
    addNotification.role().shouldBe(visible).chooseOption("Owner");
    addNotification.notificationType().chooseOption("Email");
    addNotification.email().shouldBe(empty);

    // delete one and save
    NotificationsSection.notificationFor("test@foo.com").deleteButton().click();
    NotificationsSection.notifications().shouldHave(size(3));
    PolicyEditorPage.savePolicy();
    ScrollUtil.scrollIntoView(PolicyEditorPage.notificationsSection().header());
    NotificationsSection.notifications().shouldHave(size(3));
    // Uncomment when fixing CLM-18677
    // "aaa@sonatype.com" should be first after save
    // NotificationsSection.notifications().get(0).shouldHave(text("aaa@sonatype.com"));
    // "Application Evaluator" should be second after save
    // NotificationsSection.notifications().get(1).shouldHave(text("Application Evaluator"));

    policy = policyDAO.getById(policy.getId());
    assertThat(policy.getNotifications().getRoleNotifications()).hasSize(2);
    assertThat(policy.getNotifications().getUserNotifications()).hasSize(1);
    Notifications notifications = policy.getNotifications().getApplicable(Stage.ID_BUILD, false);
    assertThat(notifications.getUserNotifications()).hasSize(0);
    assertThat(notifications.getRoleNotifications()).hasSize(1);

    // check 'operate' and 'continuousMonitoring' stages
    NotificationsSection.notificationFor("aaa@sonatype.com").operate().click();
    NotificationsSection.notificationFor("Application Evaluator").continuousMonitoring().click();
    PolicyEditorPage.savePolicy();
    ScrollUtil.scrollIntoView(PolicyEditorPage.notificationsSection().header());
    policy = policyDAO.getById(policy.getId());
    assertThat(policy.getNotifications().getApplicable(Stage.ID_OPERATE, false).getUserNotifications()).hasSize(1);
    assertThat(policy.getNotifications().getApplicable(Stage.ID_OPERATE, true).getRoleNotifications()).hasSize(1);

    // test "All roles are being notified." message
    addNotification.notificationType().chooseOption("Role");
    addNotification.role().shouldBe(visible).chooseOption("Owner");
    addNotification.addButton().shouldNotBe(disabled).click();
    addNotification.role().shouldBe(visible).chooseOption("Component Evaluator");
    addNotification.addButton().shouldNotBe(disabled).click();
    addNotification.role().shouldBe(visible).chooseOption("Legal Reviewer");
    addNotification.addButton().shouldNotBe(disabled).click();
    addNotification.role().shouldHave(text("All roles are being notified."));
    PolicyEditorPage.savePolicy();
    addNotification.notificationType().chooseOption("Role");
    assertThat(addNotification.role().selectedItem().getText()).isEqualTo("All roles are being notified.");
    NotificationsSection.notificationFor("Owner").deleteButton().click();
    addNotification.role().listItems().get(1).shouldHave(text("Owner"));

    // test "No notifications configured" message
    NotificationsSection.notificationFor("Component Evaluator").deleteButton().click();
    NotificationsSection.notificationFor("aaa@sonatype.com").deleteButton().click();
    NotificationsSection.notificationFor("Developer").deleteButton().click();
    NotificationsSection.notificationFor("Application Evaluator").deleteButton().click();
    NotificationsSection.notificationFor("Legal Reviewer").deleteButton().click();
    NotificationsSection.notifications().get(0).shouldHave(text("No notifications configured"));
  }

  private void testEditPolicy_actionsSection(Policy policy) {
    ActionsSection actionsTable = PolicyEditorPage.actionsSection();

    actionsTable.quarantineWarningMessage().shouldNotBe(visible);
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

    policy = policyDAO.getById(policy.getId());
    assertThat(policy.getActions().get(Stage.ID_BUILD)).isEqualTo(Action.ID_FAIL);
    assertThat(policy.getActions().get(Stage.ID_DEVELOP)).isEqualTo(Action.ID_WARN);
    assertThat(policy.getActions().get(Stage.ID_PROXY)).isEqualTo(Action.ID_WARN);
    assertThat(policy.getActions().get(Stage.ID_OPERATE)).isEqualTo(Action.ID_FAIL);
    assertThat(policy.getActions().get(Stage.ID_STAGE_RELEASE)).isNull();
    assertThat(policy.getActions().get(Stage.ID_RELEASE)).isNull();
  }

  private void testDeletePolicy(Policy policy) {
    ScrollUtil.scrollIntoView(PolicyEditorPage.footer());
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

    assertNewPolicyStateIsCorrect();
    assertThat(policyDAO.getById(policy.getId())).isNull();
  }

  public void testCreatePolicy_summarySection() {
    SummarySection summary = PolicyEditorPage.summarySection();

    // add extra timeout for visibility of the form as this has occasionally caused test instability
    PolicyEditorPage.summarySection().getElement().shouldBe(visible, Duration.ofSeconds(5));
    summary.policyName().input().val("New Policy");

    changeThreatLevel(9);
  }

  public void testCreatePolicy_constraintSection() {
    HashMap<Class<? extends ConditionType>, Option> conditionTypesOptionMap = conditionsToOptionMap();

    ConstraintSection constraintSection = PolicyEditorPage.constraintSection();
    constraintSection.addConstraintButton().shouldBe(visible, enabled);

    constraintSection.constraintEditors().shouldHave(size(1));

    ConstraintEditSection newConstraint = constraintSection.constraintEditor(0);
    newConstraint.conditionUnsupportedMessages().shouldHave(size(0));
    newConstraint.name().shouldBe(empty).val("New Constraint");
    newConstraint.operator().shouldHave(text("any"));
    newConstraint.conditions().shouldHave(size(1));

    AgeConditionEditSection ageCondition = newConstraint.ageCondition(0);
    ageCondition.deleteConditionButton().shouldBe(visible, disabled);
    ageCondition.type().shouldHave(text("Age"));
    ageCondition.operator().shouldHave(text("older than"));
    ageCondition.value().modifier().shouldHave(text("Years"));
    ageCondition.value().age().shouldBe(empty).val("3");

    newConstraint.addConditionButton().click();
    CoordinatesCondition coordsCondition = newConstraint.coordinatesCondition(1);

    coordsCondition.type().chooseOptionWithHidden(conditionTypesOptionMap.get(CoordinatesConditionType.class));
    coordsCondition.format().shouldHave(text("maven"));

    // Check initial values
    coordsCondition.groupId().shouldHave(value(""));
    coordsCondition.artifactId().shouldHave(value(""));
    coordsCondition.version().shouldHave(value(""));
    coordsCondition.extension().shouldHave(value("*"));
    coordsCondition.classifier().shouldHave(value("*"));

    // With everything set to wildcard, if we set any except classifier to empty we can't save
    coordsCondition.groupId().val("*");
    coordsCondition.artifactId().val("*");
    coordsCondition.version().val("*");
    toggleAndCheckSave(coordsCondition.groupId());
    toggleAndCheckSave(coordsCondition.artifactId());
    toggleAndCheckSave(coordsCondition.version());
    toggleAndCheckSave(coordsCondition.extension());

    // With everything set to wildcard, if we set classifier to empty we can still save
    InputUtils.clearInput(coordsCondition.classifier());
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    // With everything set to wildcard, if we set classifier back to a wildcard we can still save
    coordsCondition.classifier().val("*");
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    // With everything set to specific values, if we set any except classifier to empty we can't save
    InputUtils.clearInput(coordsCondition.groupId());
    coordsCondition.groupId().val("org.apache");

    InputUtils.clearInput(coordsCondition.artifactId());
    coordsCondition.artifactId().val("tomcat");

    InputUtils.clearInput(coordsCondition.version());
    coordsCondition.version().val("5.0.28");

    InputUtils.clearInput(coordsCondition.extension());
    coordsCondition.extension().val("jar");

    InputUtils.clearInput(coordsCondition.classifier());
    coordsCondition.classifier().val("javadoc");

    toggleAndCheckSave(coordsCondition.groupId());
    toggleAndCheckSave(coordsCondition.artifactId());
    toggleAndCheckSave(coordsCondition.version());
    toggleAndCheckSave(coordsCondition.extension());

    // With everything set to specific values, if we set classifier to empty we can still save
    InputUtils.clearInput(coordsCondition.classifier());
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    // With everything set to specific values, if we set classifier back to a value we can still save
    coordsCondition.classifier().val("javadoc");
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    newConstraint.addConditionButton().click();
    coordsCondition = newConstraint.coordinatesCondition(2);
    coordsCondition.type().chooseOptionWithHidden(conditionTypesOptionMap.get(CoordinatesConditionType.class));
    coordsCondition.setOperator("do not match");

    NxFormSelect format = coordsCondition.format();
    format.click();
    format.listItem(1).click();
    format.shouldHave(text("a-name"));

    // Check initial values
    coordsCondition.name().shouldHave(value(""));
    coordsCondition.qualifier().shouldHave(value("*"));
    coordsCondition.version().shouldHave(value(""));

    // With everything set to wildcard, if we set any except qualifier to empty we can't save
    coordsCondition.name().val("*");
    coordsCondition.qualifier().val("*");
    coordsCondition.version().val("*");
    toggleAndCheckSave(coordsCondition.name());
    toggleAndCheckSave(coordsCondition.version());

    // With everything set to wildcard, if we set qualifier to empty we can still save
    InputUtils.clearInput(coordsCondition.qualifier());
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    // With everything set to wildcard, if we set qualifier back to a wildcard we can still save
    coordsCondition.qualifier().val("*");
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    // With everything set to specific values, if we set any except qualifier to empty we can't save
    coordsCondition.name().val("log4net");
    coordsCondition.qualifier().val("Framework 3.5");
    coordsCondition.version().val("2.0.5");
    toggleAndCheckSave(coordsCondition.name());
    toggleAndCheckSave(coordsCondition.version());

    // With everything set to specific values, if we set qualifier to empty we can still save
    InputUtils.clearInput(coordsCondition.qualifier());
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    // With everything set to specific values, if we set qualifier back to a specific value we can still save
    coordsCondition.qualifier().val("Framework 3.5");
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    InputUtils.clearInput(coordsCondition.name());
    coordsCondition.name().val("jquery");

    InputUtils.clearInput(coordsCondition.qualifier());
    coordsCondition.version().val("1.0.28");

    newConstraint.addConditionButton().click();
    coordsCondition = newConstraint.coordinatesCondition(3);
    coordsCondition.type().chooseOptionWithHidden(conditionTypesOptionMap.get(CoordinatesConditionType.class));
    format = coordsCondition.format();
    format.click();
    format.listItem(15).click();
    format.shouldHave(text("pypi"));

    // Check initial values
    coordsCondition.name().shouldHave(value(""));
    coordsCondition.version().shouldHave(value(""));
    coordsCondition.qualifier().shouldHave(value("*"));
    coordsCondition.extension().shouldHave(value("*"));

    // With everything set to wildcard, if we set any except qualifier to empty we can't save
    coordsCondition.name().val("*");
    coordsCondition.version().val("*");
    coordsCondition.qualifier().val("*");
    coordsCondition.extension().val("*");
    toggleAndCheckSave(coordsCondition.name());
    toggleAndCheckSave(coordsCondition.version());

    // With everything set to wildcard, if we set extension and qualifier to empty we can still save
    InputUtils.clearInput(coordsCondition.extension());
    InputUtils.clearInput(coordsCondition.qualifier());
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    // With everything set to wildcard, if we set qualifier back to a wildcard we can still save
    coordsCondition.qualifier().val("*");
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    // With everything set to wildcard, if we set extension back to a wildcard we can still save
    coordsCondition.extension().val("*");
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    // With everything set to specific values, if we set any except qualifier to empty we can't save
    coordsCondition.name().val("MarkupSafe");
    coordsCondition.version().val("1.1.0");
    coordsCondition.qualifier().val("cp37");
    coordsCondition.extension().val("tar.gz");
    toggleAndCheckSave(coordsCondition.name());
    toggleAndCheckSave(coordsCondition.version());

    // With everything set to specific values, if we set qualifier to empty we can still save
    InputUtils.clearInput(coordsCondition.qualifier());
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    // With everything set to specific values, if we set extension to empty we can still save
    InputUtils.clearInput(coordsCondition.extension());
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    // With everything set to specific values, if we set qualifier back to a specific value we can still save
    coordsCondition.qualifier().val("cp37");
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    // With everything set to specific values, if we set extension back to a specific value we can still save
    coordsCondition.extension().val("tar.gz");
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    DropdownConditionEditSection labelCondition =
        addDropdownCondition(newConstraint, LabelConditionType.class, 4, conditionTypesOptionMap);
    labelCondition.operator().shouldHave(text("is"));
    labelCondition.value().shouldBe(text("Sample Label"));

    DropdownConditionEditSection licenseCondition =
        addDropdownCondition(newConstraint, LicenseConditionType.class, 5, conditionTypesOptionMap);
    licenseCondition.operator().shouldHave(text("is"));
    licenseCondition.value().shouldHave(text("0BSD")).click();
    licenseCondition.value().listItem(10).shouldHave(text("ABBYY-RTR-SDK-LA")).click();

    DropdownConditionEditSection licenseStatus =
        addDropdownCondition(newConstraint, LicenseStatusConditionType.class, 6, conditionTypesOptionMap);
    licenseStatus.operator().shouldHave(text("is")).click();
    licenseStatus.operator().listItem(1).shouldHave(text("is not")).click();
    licenseStatus.value().shouldHave(text("Open")).click();
    licenseStatus.value().listItem(4).shouldHave(text("Confirmed")).click();

    DropdownConditionEditSection licenseThreatGroup =
        addDropdownCondition(newConstraint, LicenseThreatGroupConditionType.class, 7, conditionTypesOptionMap);
    licenseThreatGroup.operator().shouldHave(text("is"));
    licenseThreatGroup.value().shouldHave(text("Banned")).click();
    licenseThreatGroup.value().listItem(1).shouldHave(text("Liberal")).click();

    InputConditionEditSection licenseThreatGroupLevel =
        addInputCondition(newConstraint, LicenseThreatGroupLevelConditionType.class, 8, conditionTypesOptionMap);
    licenseThreatGroupLevel.operator().shouldHave(text("<=")).click();
    licenseThreatGroupLevel.operator().listItem(1).shouldHave(text(">=")).click();
    licenseThreatGroupLevel.value().val("5");

    InputConditionEditSection securityVulnerabilitySeverity =
        addInputCondition(newConstraint, SecurityVulnerabilitySeverityConditionType.class, 9, conditionTypesOptionMap);
    securityVulnerabilitySeverity.operator().shouldHave(text("=")).click();
    securityVulnerabilitySeverity.operator().listItem(3).shouldHave(text(">")).click();
    securityVulnerabilitySeverity.value().val("1");

    DropdownConditionEditSection securityVulnerabilityStatus = addDropdownCondition(newConstraint,
        SecurityVulnerabilityStatusConditionType.class, 10, conditionTypesOptionMap);
    securityVulnerabilityStatus.operator().shouldHave(text("is"));
    securityVulnerabilityStatus.value().shouldHave(text("Open")).click();
    securityVulnerabilityStatus.value().listItem(2).shouldHave(text("Not Applicable")).click();

    InputConditionEditSection relativePopularity =
        addInputCondition(newConstraint, RelativePopularityConditionType.class, 11, conditionTypesOptionMap);
    relativePopularity.operator().shouldHave(text("="));
    relativePopularity.value().val("50");

    newConstraint.addConditionButton().click();
    DropdownConditionEditSection matchState = newConstraint.dropdownCondition(12);
    matchState.type().chooseOptionWithHidden(conditionTypesOptionMap.get(MatchStateConditionType.class));
    matchState.operator().shouldHave(text("is")).click();
    matchState.operator().listItem(1).shouldHave(text("is not")).click();
    matchState.value().shouldHave(text(MatchState.EXACT.getName())).click();
    matchState.value().listItem(3).shouldHave(text(MatchState.UNKNOWN.getName())).click();

    newConstraint.addConditionButton().click();
    DropdownConditionEditSection proprietary = newConstraint.dropdownCondition(13);
    proprietary.type().chooseOptionWithHidden(conditionTypesOptionMap.get(ProprietaryConditionType.class));
    proprietary.operator().shouldHave(text("is true")).click();
    proprietary.operator().listItem(1).shouldHave(text("is false")).click();

    newConstraint.addConditionButton().click();
    DropdownConditionEditSection identificationSource = newConstraint.dropdownCondition(14);
    identificationSource.type()
        .chooseOptionWithHidden(conditionTypesOptionMap.get(IdentificationSourceConditionType.class));
    identificationSource.operator().shouldHave(text("is")).click();
    identificationSource.operator().listItem(1).shouldHave(text("is not")).click();
    identificationSource.value().shouldHave(text(IdentificationSource.SONATYPE.getName())).click();
    identificationSource.value().listItem(2).shouldHave(text(IdentificationSource.MANUAL.getName())).click();

    newConstraint.addConditionButton().click();
    InputConditionEditSection packageUrlCondition = newConstraint.inputCondition(15);
    packageUrlCondition.type().chooseOptionWithHidden(conditionTypesOptionMap.get(PackageUrlConditionType.class));
    packageUrlCondition.operator().shouldHave(text("matches"));
    packageUrlCondition.value().shouldBe(empty).val("pkg:maven/g/a@v?type=jar");
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    newConstraint.addConditionButton().click();
    packageUrlCondition = newConstraint.inputCondition(16);
    packageUrlCondition.type().chooseOptionWithHidden(conditionTypesOptionMap.get(PackageUrlConditionType.class));
    packageUrlCondition.operator().shouldHave(text("matches"));
    packageUrlCondition.value().shouldBe(empty).val("pkg:maven/*/a@*?type=jar");
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    newConstraint.addConditionButton().click();
    packageUrlCondition = newConstraint.inputCondition(17);
    packageUrlCondition.type().chooseOptionWithHidden(conditionTypesOptionMap.get(PackageUrlConditionType.class));
    packageUrlCondition.operator().click().listItem(1).click();
    packageUrlCondition.operator().shouldHave(text("does not match"));
    packageUrlCondition.value().shouldBe(empty).val("pkg:npm/a@v");
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    newConstraint.addConditionButton().click();
    packageUrlCondition = newConstraint.inputCondition(18);
    packageUrlCondition.type().chooseOptionWithHidden(conditionTypesOptionMap.get(PackageUrlConditionType.class));
    packageUrlCondition.value().shouldBe(empty).val("pkg:pypi/*/*/a@*?type=jar");
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    newConstraint.addConditionButton().click();
    packageUrlCondition = newConstraint.inputCondition(19);
    packageUrlCondition.type().chooseOptionWithHidden(conditionTypesOptionMap.get(PackageUrlConditionType.class));
    packageUrlCondition.value().shouldBe(empty).val("pkg:golang/*/*/a@*");
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    newConstraint.addConditionButton().click();
    packageUrlCondition = newConstraint.inputCondition(20);
    packageUrlCondition.type().chooseOptionWithHidden(conditionTypesOptionMap.get(PackageUrlConditionType.class));
    packageUrlCondition.value().shouldBe(empty).val("pkg:conan/*/*/a@*");
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    newConstraint.addConditionButton().click();
    packageUrlCondition = newConstraint.inputCondition(21);
    packageUrlCondition.type().chooseOptionWithHidden(conditionTypesOptionMap.get(PackageUrlConditionType.class));
    packageUrlCondition.value().shouldBe(empty).val("pkg:golang/*/*/*@*");
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    newConstraint.addConditionButton().click();
    packageUrlCondition = newConstraint.inputCondition(22);
    packageUrlCondition.type().chooseOptionWithHidden(conditionTypesOptionMap.get(PackageUrlConditionType.class));
    packageUrlCondition.value().shouldBe(empty).val("pkg:*/*/*/*@*");
    packageUrlCondition.deleteConditionButton().click();
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    newConstraint.addConditionButton().click();
    DropdownConditionEditSection hygieneRating = newConstraint.dropdownCondition(22);
    hygieneRating.type().chooseOptionWithHidden(conditionTypesOptionMap.get(HygieneRatingConditionType.class));
    hygieneRating.operator().shouldHave(text("is")).click();
    hygieneRating.operator().listItem(1).shouldHave(text("is not")).click();
    hygieneRating.value().shouldHave(text("Exemplar")).click();
    hygieneRating.value().listItem(1).shouldHave(text("Laggard")).click();
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    newConstraint.addConditionButton().click();
    DropdownConditionEditSection dataSourceCondition = newConstraint.dropdownCondition(23);
    dataSourceCondition.type().chooseOptionWithHidden(conditionTypesOptionMap.get(DataSourceConditionType.class));
    dataSourceCondition.operator().shouldHave(text(HAS_SUPPORT_FOR)).click();
    dataSourceCondition.operator().listItem(1).shouldHave(text(HAS_NO_SUPPORT_FOR)).click();
    dataSourceCondition.value().shouldHave(text("license")).click();
    dataSourceCondition.value().listItem(1).shouldHave(text("identity")).click();
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    newConstraint.addConditionButton().click();
    DropdownConditionEditSection dependencyType = newConstraint.dropdownCondition(24);
    dependencyType.type().chooseOptionWithHidden(conditionTypesOptionMap.get(DependencyTypeConditionType.class));
    dependencyType.operator().shouldHave(text("is")).click();
    dependencyType.operator().listItem(1).shouldHave(text("is not")).click();
    dependencyType.value().shouldHave(text("Direct")).click();
    dependencyType.value().listItem(1).shouldHave(text("Transitive")).click();
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    newConstraint.addConditionButton().click();
    DropdownConditionEditSection vulnerabilityCategory = newConstraint.dropdownCondition(25);
    vulnerabilityCategory.type()
        .chooseOptionWithHidden(conditionTypesOptionMap.get(SecurityVulnerabilityCategoryConditionType.class));
    vulnerabilityCategory.operator().shouldHave(text("is")).click();
    vulnerabilityCategory.operator().listItem(1).shouldHave(text("is not")).click();
    SidebarNavigation.closeNavigationSidebar();
    vulnerabilityCategory.value().shouldHave(text("Configuration")).click();
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    newConstraint.addConditionButton().click();
    DropdownConditionEditSection integrityRating = newConstraint.dropdownCondition(26);
    integrityRating.type().chooseOptionWithHidden(conditionTypesOptionMap.get(IntegrityRatingConditionType.class));
    integrityRating.operator().shouldHave(text("is")).click();
    integrityRating.operator().listItem(1).shouldHave(text("is not")).click();
    integrityRating.value().shouldHave(text("Normal")).click();
    integrityRating.value().listItem(1).shouldHave(text("Suspicious")).click();
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    newConstraint.addConditionButton().click();
    DropdownConditionEditSection dependencyInnerSourceType = newConstraint.dropdownCondition(27);
    dependencyInnerSourceType.type()
        .chooseOptionWithHidden(conditionTypesOptionMap.get(DependencyTypeConditionType.class));
    dependencyInnerSourceType.operator().shouldHave(text("is"));
    dependencyInnerSourceType.value().shouldHave(text("Direct")).click();
    dependencyInnerSourceType.value().listItem(2).shouldHave(text("InnerSource")).click();
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    newConstraint.addConditionButton().click();
    DropdownConditionEditSection endOfLife = newConstraint.dropdownCondition(28);
    endOfLife.type().chooseOptionWithHidden(conditionTypesOptionMap.get(ComponentEndOfLifeConditionType.class));
    endOfLife.operator().shouldHave(text("is true")).click();
    endOfLife.operator().listItem(1).shouldHave(text("is false")).click();

    newConstraint.addConditionButton().click();
    DropdownConditionEditSection kevStatus = newConstraint.dropdownCondition(29);
    kevStatus.type().chooseOptionWithHidden(conditionTypesOptionMap.get(KevStatusConditionType.class));
    kevStatus.operator().shouldHave(text("is")).click();
    kevStatus.value().shouldHave(text("Known to be exploited")).click();
    kevStatus.value().listItem(1).shouldHave(text("Not listed")).click();
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);

    newConstraint.conditionUnsupportedMessages().shouldHave(size(0));
  }

  private void toggleAndCheckSave(final SelenideElement element) {
    final String value = element.val();
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);
    InputUtils.clearInput(element);
    element.val(value);
    PolicyEditorPage.saveButton().shouldNotHave(DISABLED);
  }

  private void testCreatePolicy_actionsSection() {
    // header names
    ActionsSection actionsTable = PolicyEditorPage.actionsSection();
    ScrollUtil.scrollIntoView(actionsTable.headers().first());
    actionsTable.headers().shouldHave(texts("PROXY", "DEVELOP", "SOURCE", "BUILD", "STAGE", "RELEASE", "OPERATE"));

    // make an actual change
    actionsTable.build().warnRadio().click();

    actionsTable.quarantineWarningMessage().shouldNotBe(visible);
    actionsTable.proxy().failRadio().click();
    actionsTable.proxy().failRadio().shouldBe(selected);
    actionsTable.proxy().warnRadio().shouldNotBe(selected);
    actionsTable.proxy().noActionRadio().shouldNotBe(selected);
    actionsTable.quarantineWarningMessage().shouldNotBe(visible);
    actionsTable.proxy().noActionRadio().click();
    actionsTable.quarantineWarningMessage().shouldNotBe(visible);
  }

  private void testCreatePolicy_notificationsSection() {
    ScrollUtil.scrollIntoView(PolicyEditorPage.notificationsSection().header());
    NotificationsSection.notifications().get(0).shouldHave(text("No notifications configured"));

    // add role notifications
    AddNotificationItem addNotification = NotificationsSection.addNotification();
    addNotification.notificationType().chooseOption("Role");
    addNotification.role().shouldBe(visible).chooseOption("Application Evaluator");
    addNotification.addButton().shouldNotBe(disabled).click();

    // add email notifications
    addNotification.notificationType().chooseOption("Email");
    addNotification.addButton().shouldBe(disabled);
    addNotification.email().val("aaa@sonatype.com").shouldNotHave(cssClass("ng-invalid")).shouldBe(visible);
    addNotification.addButton().shouldNotBe(disabled).click();
    addNotification.addButton().shouldBe(disabled);
    addNotification.email().shouldBe(empty);

    // add webhook notifications
    addNotification.notificationType().chooseOption("Webhook");
    addNotification.addButton().shouldBe(disabled);
    addNotification.webhook().shouldBe(visible).chooseOption("http://localhost");
    addNotification.addButton().shouldNotBe(disabled).click();

    // add webhook with description
    addNotification.notificationType().chooseOption("Webhook");
    addNotification.addButton().shouldBe(disabled);
    addNotification.webhook().shouldBe(visible).chooseOption("description");
    addNotification.addButton().shouldNotBe(disabled).click();

    // Uncomment after fixing CLM-18677. The notification recipient column is currently completely
    // squeezed out of the visible layout causing these checks to fail
    // NotificationsSection.notifications().get(0).shouldHave(text("Application Evaluator"));
    // NotificationsSection.notifications().get(1).shouldHave(text("aaa@sonatype.com"));
    // NotificationsSection.notifications().get(2).shouldHave(text("Webhook: http://localhost"));
    // NotificationsSection.notifications().get(3).shouldHave(text("Webhook: description"));

    // check stages
    NotificationsSection.notificationFor("aaa@sonatype.com").build().click();
    NotificationsSection.notificationFor("Application Evaluator").continuousMonitoring().click();
    NotificationsSection.notificationFor("Webhook: http://localhost").stageRelease().click();
    NotificationsSection.notificationFor("Webhook: http://localhost")
        .proxy()
        .input()
        .shouldHave(attribute("disabled", "true"));
    NotificationsSection.notificationFor("Webhook: http://localhost").proxy().hover();
    Tooltip.get()
        .shouldBe(visible)
        .shouldHave(text("Webhooks are not available for policy violations at Proxy stage."));
    NotificationsSection.notificationFor("Webhook: description").proxy().input().shouldHave(attribute("disabled"));
    NotificationsSection.notificationFor("Webhook: description").proxy().hover();
    Tooltip.get()
        .shouldBe(visible)
        .shouldHave(text("Webhooks are not available for policy violations at Proxy stage."));
  }

  private void assertNewPolicyStateIsCorrect() {
    waitUntilUrl(PolicyEditorPage.urlToCreate(currentOwner));
    PolicyEditorPage.title().shouldHave(text("Policy Settings"));

    assertNewPolicyStateIsCorrect_summarySection();
    assertNewPolicyStateIsCorrect_inheritanceSection();
    assertNewPolicyStateIsCorrect_constraintSection();
    assertNewPolicyStateIsCorrect_actionsSection();
    assertNewPolicyStateIsCorrect_notificationsSection();

    PolicyEditorPage.deleteButton().shouldNot(exist);
  }

  private void assertNewPolicyStateIsCorrect_summarySection() {
    SummarySection summary = PolicyEditorPage.summarySection();
    summary.policyName().input().shouldBe(visible, empty);
    summary.policyName().inputWrapper().shouldHave(CLM.RSC_PRISTINE);

    summary.policyName().input().val("$$$"); // invalid characters
    summary.policyName().errorMessage().shouldHave(text("Use valid characters"));

    summary.policyName().input().val("1  2"); // double spaces
    summary.policyName().errorMessage().shouldHave(text("No leading, trailing or double spaces or tabs"));

    summary.policyName().input().val("Acceptable Name");
    summary.policyName().errorMessage().shouldNotBe(visible);

    InputUtils.clearInput(summary.policyName().input());

    assertThreatDropdownSelectorState(PolicyEditorPage.DEFAULT_THREAT_LEVEL, false);
  }

  private void assertNewPolicyStateIsCorrect_constraintSection() {
    SelenideElement constraintName = PolicyEditorPage.constraintSection().constraintEditor(0).name();

    constraintName.shouldBe(visible, empty);
    PolicyEditorPage.constraintSection().getInputValidationElement(constraintName).shouldNotBe(visible);

    constraintName.val(" ");
    PolicyEditorPage.constraintSection()
        .getInputValidationElement(constraintName)
        .shouldHave(text("Must be non-empty"));

    InputUtils.clearInput(constraintName);

    constraintName.val("$ Anything  !s Accept@ble :)   ");
    PolicyEditorPage.constraintSection().getInputValidationElement(constraintName).shouldNotBe(visible);

    InputUtils.clearInput(constraintName);
  }

  private void assertNewPolicyStateIsCorrect_notificationsSection() {
    ElementsCollection notifications = NotificationsSection.notifications();
    notifications.shouldHave(size(1));
    notifications.get(0).shouldHave(text("No notifications Configured"));

    AddNotificationItem addNotification = NotificationsSection.addNotification();
    addNotification.addButton().shouldBe(disabled);
    addNotification.role().shouldNot(exist);
    addNotification.email().shouldBe(empty);

    addNotification.notificationType().chooseOption("Role");
    addNotification.email().shouldNot(exist);
    addNotification.role().listItems().shouldHave(size(6));
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
    assertEditPolicyStateIsCorrect(policy, category1, category2, isReadOnly, false, false, false, false);
  }

  private void assertEditPolicyStateIsCorrect(
      Policy policy,
      Tag category1,
      Tag category2,
      boolean isReadOnly,
      boolean actionsReadOnly,
      boolean notificationsReadOnly,
      boolean proxyActionReadOnly,
      boolean legacyViolationReadOnly)
  {
    waitUntilUrl(PolicyEditorPage.urlToEdit(currentOwner, policy.getId()));
    PolicyEditorPage.title().shouldHave(text("Policy Settings"));

    assertEditPolicyStateIsCorrect_summarySection(policy, isReadOnly, legacyViolationReadOnly);
    assertEditPolicyStateIsCorrect_inheritanceSection(category1, category2, isReadOnly);
    assertEditPolicyStateIsCorrect_actionsSection(isReadOnly, actionsReadOnly, proxyActionReadOnly);
    assertEditPolicyStateIsCorrect_notificationsSection(isReadOnly, notificationsReadOnly, proxyActionReadOnly);
    PolicyEditorPage.deleteButton().shouldBe(visible, isReadOnly ? disabled : enabled);
  }

  private void assertEditPolicyStateIsCorrect_summarySection(
      Policy policy,
      boolean isReadOnly,
      boolean legacyViolationReadOnly)
  {
    SummarySection summary = PolicyEditorPage.summarySection();
    summary.policyName().input().shouldBe(visible, isReadOnly ? disabled : enabled).shouldHave(value(policy.getName()));
    summary.policyName().inputWrapper().shouldHave(CLM.RSC_PRISTINE);
    assertThreatDropdownSelectorState(policy.getThreatLevel(), isReadOnly);

    WebElementCondition disabledOrEnabled =
        isReadOnly || legacyViolationReadOnly ? disabled : enabled;
    summary.legacyViolationCheckbox().shouldBe(visible, disabledOrEnabled).shouldNotBe(selected);
    if (legacyViolationReadOnly) {
      String expectedText = "Legacy Violations are not supported by your license";
      PolicyEditorPage.disabledLegacyViolationMessage().shouldBe(text(expectedText));
    }
    else {
      PolicyEditorPage.disabledLegacyViolationMessage().shouldBe(hidden);
    }
  }

  private void assertEditPolicyStateIsCorrect_actionsSection(
      boolean isReadOnly,
      boolean actionsReadOnly,
      boolean proxyActionReadOnly)
  {
    ScrollUtil.scrollIntoView(PolicyEditorPage.actionsSection().header());

    WebElementCondition disabledOrEnabled;
    if (OwnerType.REPOSITORY_CONTAINER.equals(currentOwner.getType())) {
      disabledOrEnabled = enabled;
    }
    else {
      disabledOrEnabled = isReadOnly || actionsReadOnly ? disabled : enabled;
    }

    ActionsSection actionsTable = PolicyEditorPage.actionsSection();

    // Policy actions for Developer and Build are set to Warn and Fail, respectively.
    ActionsSection.Stage develop = actionsTable.develop();
    develop.failRadio().shouldBe(visible, disabledOrEnabled).shouldNotBe(selected);
    develop.warnRadio().shouldBe(selected, visible, disabledOrEnabled);
    develop.noActionRadio().shouldBe(visible, disabledOrEnabled).shouldNotBe(selected);

    // For firewall with foundation proxy should be enabled
    disabledOrEnabled = isReadOnly || proxyActionReadOnly ? disabled : enabled;
    ActionsSection.Stage proxy = actionsTable.proxy();
    proxy.failRadio().shouldBe(visible, disabledOrEnabled).shouldNotBe(selected);
    proxy.warnRadio().shouldBe(visible, disabledOrEnabled).shouldNotBe(selected);
    proxy.noActionRadio().shouldBe(selected, visible, disabledOrEnabled);

    actionsTable.build().failRadio().shouldBe(selected);

    // The rest of the stages should have no-action selected
    actionsTable.operate().noActionRadio().input().shouldBe(selected);
    actionsTable.release().noActionRadio().input().shouldBe(selected);
    actionsTable.stageRelease().noActionRadio().input().shouldBe(selected);

    if (actionsReadOnly) {
      String expectedText = !proxyActionReadOnly
          ? "Only Proxy Actions are supported with your Firewall product license."
          : "Actions are not supported by your product license.";
      PolicyEditorPage.disabledActionsMessage().shouldBe(text(expectedText));
    }
    else {
      PolicyEditorPage.disabledActionsMessage().shouldBe(hidden);
    }
  }

  private void assertEditPolicyStateIsCorrect_notificationsSection(
      final boolean isReadOnly,
      boolean notificationsReadOnly,
      boolean proxyActionReadOnly)
  {
    AddNotificationItem addNotificationItem = NotificationsSection.addNotification();

    if (isReadOnly || (notificationsReadOnly && proxyActionReadOnly)) {
      addNotificationItem.notificationType().shouldBe(disabled);
      addNotificationItem.email().shouldBe(disabled);
    }
    else {
      addNotificationItem.notificationType().shouldNotBe(disabled);
      assertThat(addNotificationItem.notificationType().selectedItem().getText()).isEqualTo("Email");
      addNotificationItem.email().shouldBe(enabled);
    }
    addNotificationItem.role().shouldNot(exist);
    addNotificationItem.addButton().shouldBe(disabled);

    WebElementCondition disabledOrEnabled =
        isReadOnly || notificationsReadOnly ? disabled : enabled;

    // Uncomment when fixing CLM-18677
    // NotificationsSection.notifications().shouldHave(size(2)
    // .shouldHave(texts("Developer", "test@foo.com"));
    NotificationsSection.notificationFor("Developer").build().input().shouldBe(selected, disabledOrEnabled);
    NotificationsSection.notificationFor("test@foo.com").build().input().shouldBe(selected, disabledOrEnabled);
    NotificationsSection.notificationFor("test@foo.com")
        .continuousMonitoring()
        .input()
        .shouldBe(selected, disabledOrEnabled);

    // check the tooltip on just one of the checkboxes
    NotificationsSection.notificationFor("Developer").build().hover();
    if (notificationsReadOnly) {
      Tooltip.get().shouldBe(visible).shouldHave(text("Notifications are not supported by your license"));
    }
    else {
      Tooltip.get().shouldNotBe(visible);
    }

    // For firewall with foundation proxy should be enabled
    disabledOrEnabled = isReadOnly || proxyActionReadOnly ? disabled : enabled;
    NotificationsSection.notificationFor("Developer").proxy().input().shouldNotBe(selected).shouldBe(disabledOrEnabled);
    NotificationsSection.notificationFor("test@foo.com")
        .proxy()
        .input()
        .shouldNotBe(selected)
        .shouldBe(disabledOrEnabled);

    if (notificationsReadOnly) {
      String expectedText =
          !proxyActionReadOnly
              ? "Only Proxy Notifications are supported with your Firewall product license."
              : "Notifications are not supported by your product license.";
      PolicyEditorPage.disabledNotificationsMessage().shouldHave(text(expectedText));
    }
    else {
      PolicyEditorPage.disabledNotificationsMessage().shouldBe(hidden);
    }
  }

  private void assertThreatDropdownSelectorState(int selectedThreatLevel, boolean isReadOnly) {
    ThreatDropdownSelector.root().shouldBe(visible);
    if (isReadOnly) {
      ThreatDropdownSelector.dropdownButton().shouldBe(visible).shouldHave(DISABLED);
      ThreatDropdownSelector.threatLevelList().shouldBe(hidden);
    }
    else {
      ThreatDropdownSelector.dropdownButton().shouldBe(visible, enabled).click();
      ThreatDropdownSelector.threatLevelList().shouldBe(visible);

      ThreatDropdownSelector.threatLevelListItems()
          .shouldHave(size(ThreatDropdownSelector.IQ_NUM_THREAT_LEVELS));

      for (int i = 0; i < ThreatDropdownSelector.IQ_NUM_THREAT_LEVELS; i++) {
        ThreatDropdownSelector.threatLevelListItem(i).shouldBe(visible).shouldHave(text(Integer.toString(10 - i)));
      }

      ThreatDropdownSelector.selectedThreatLabel()
          .shouldBe(visible, text(Integer.toString(selectedThreatLevel)))
          .click();
    }
  }

  private void changeThreatLevel(int threatLevel) {
    ThreatDropdownSelector.dropdownButton().shouldBe(visible, enabled).click();
    ThreatDropdownSelector.threatLevelListItem(10 - threatLevel).shouldBe(visible).click();
    ThreatDropdownSelector.selectedThreatLabel().shouldHave(text(String.valueOf(threatLevel)));
  }

  private DropdownConditionEditSection addDropdownCondition(
      ConstraintEditSection constraint,
      Class<? extends AbstractConditionType> conditionType,
      int row,
      HashMap<Class<? extends ConditionType>, Option> conditionTypesOptionMap)
  {
    constraint.addConditionButton().click();
    DropdownConditionEditSection condition = constraint.dropdownCondition(row);
    condition.type().chooseOptionWithHidden(conditionTypesOptionMap.get(conditionType));
    return condition;
  }

  private InputConditionEditSection addInputCondition(
      ConstraintEditSection constraint,
      Class<? extends AbstractConditionType> conditionType,
      int row,
      HashMap<Class<? extends ConditionType>, Option> conditionTypesOptionMap)
  {
    constraint.addConditionButton().click();
    InputConditionEditSection condition = constraint.inputCondition(row);
    condition.type().chooseOptionWithHidden(conditionTypesOptionMap.get(conditionType));
    return condition;
  }

  protected abstract void assertNewPolicyStateIsCorrect_inheritanceSection();

  protected abstract void testCreatePolicy_inheritanceSection();

  protected abstract void testEditPolicy_inheritanceSection();

  protected abstract void assertEditPolicyStateIsCorrect_inheritanceSection(
      Tag category1,
      Tag category2,
      boolean isReadOnly);

  private HashMap<Class<? extends ConditionType>, Option> conditionsToOptionMap() {
    HashMap<Class<? extends ConditionType>, Option> map = new HashMap<>();

    int i = 0;
    for (ConditionType conditionType : ConditionTypes.getAll()) {
      if (conditionType.isEnabled()) {
        map.put(conditionType.getClass(), new Option(i, conditionType.getName()));
        i++;
      }
    }

    return map;
  }
}
