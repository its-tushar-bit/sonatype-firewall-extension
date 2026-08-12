/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import static com.sonatype.insight.brain.policy.PolicyResource.NOTIFICATIONS_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.ProprietaryNameConflictConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityCategoryConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.policy.PolicyResource.ApplicablePolicies;
import com.sonatype.insight.brain.policy.PolicyResource.PoliciesByOwner;
import com.sonatype.insight.brain.policy.PolicyResource.ProprietaryNameConflictAndSecurityVulnerabilityCategoryMaliciousCodePolicies;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@Category(SlowTest.class)
public class PolicyResourceTest
    extends AbstractResourceTest
{
  private PolicyDAO policyDAO;

  private OrganizationDAO organizationDAO;

  private TelemetrySender telemetrySender;

  private TelemetryUtils telemetryUtils;

  @Override
  protected List<Class<?>> getTestConfigurationClasses() {
    List<Class<?>> configs = new ArrayList<>(super.getTestConfigurationClasses());
    configs.add(PolicyResourceTestConfiguration.class);
    return configs;
  }

  @Before
  public void setUp() {
    policyDAO = lookup(PolicyDAO.class);
    organizationDAO = lookup(OrganizationDAO.class);
    telemetrySender = lookup(TelemetrySender.class);
    telemetryUtils = lookup(TelemetryUtils.class);
    reset(telemetrySender, telemetryUtils);

    when(telemetryUtils.obfuscate(anyString())).thenAnswer(invocation -> "obfuscated-" + invocation.getArgument(0));
  }

  @TestConfiguration
  static class PolicyResourceTestConfiguration
  {
    @Bean
    @Primary
    public TelemetrySender telemetrySender() {
      return mock(TelemetrySender.class);
    }

    @Bean
    @Primary
    public TelemetryUtils telemetryUtils() {
      return mock(TelemetryUtils.class);
    }
  }

  private HttpRequest restRequest(OwnerType ownerType, String ownerId) {
    return restRequest().path(PolicyResource.RESOURCE_PATH).parameter(ownerType, ownerId);
  }

  private PolicyExportResult createImportBody() {
    PolicyExportResult policyExportResult = new PolicyExportResult();
    policyExportResult.policies = Collections.singletonList(new Policy());

    return policyExportResult;
  }

  @Test
  public void testImportPolicies_OrganizationDoesNotExist() throws Exception {
    String orgId = "OrgDoesNotExist";

    HttpResponse response = restRequest(OwnerType.ORGANIZATION, orgId).path("import")
        .part("file", "file", createImportBody())
        .post();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Organization with ID " + orgId + " does not exist.");
  }

  @Test
  public void testCRUD_ApplicationLevel() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    testCRUD(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test
  public void testCRUD_OrganizationLevel() throws Exception {
    String orgId = tempEntity.newOrganization("test").getId();

    testCRUD(OwnerType.ORGANIZATION, orgId);
  }

  @Test
  public void testCRUD_RepositoryContainerLevel() throws Exception {
    testCRUD(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID);
  }

  @Test
  public void testCRUD_RepositoryManagerLevel() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    testCRUD(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId());
  }

  @Test
  public void testCRUD_RepositoryLevel() throws Exception {
    Repository repository = tempEntity.newRepository();
    testCRUD(OwnerType.REPOSITORY, repository.getId());
  }

  @Test
  public void testUpdatePolicy_DifferentOwnerId() throws Exception {
    Organization ownerOrg = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(ownerOrg);

    Organization otherOrg = tempEntity.newOrganization();
    policy.setOwnerId(otherOrg.getId());

    HttpResponse response = restRequest(OwnerType.ORGANIZATION, otherOrg.getId()).body(policy).put();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo("Cannot find a policy with id " + policy.getId() + " for owner id " + otherOrg.getId());

    ArgumentCaptor<TelemetryData> telemetryCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender, never()).send(telemetryCaptor.capture());
  }

  @Test
  public void testUpdatePolicyNotifications_DifferentOwnerId() throws Exception {
    Organization ownerOrg = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(ownerOrg);

    Organization otherOrg = tempEntity.newOrganization();
    policy.setOwnerId(otherOrg.getId());

    HttpResponse response =
        restRequest(OwnerType.ORGANIZATION, otherOrg.getId()).path(NOTIFICATIONS_PATH).body(policy).put();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo("Cannot find a policy with id " + policy.getId() + " for owner id " + otherOrg.getId());

    ArgumentCaptor<TelemetryData> telemetryCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender, never()).send(telemetryCaptor.capture());
  }

  @Test
  public void testUpdatePolicyNotifications_PolicyNotExists() throws Exception {
    Organization ownerOrg = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(ownerOrg);
    policy.setId("not-exists");

    HttpResponse response =
        restRequest(OwnerType.ORGANIZATION, ownerOrg.getId()).path(NOTIFICATIONS_PATH).body(policy).put();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo("Cannot find a policy with id " + policy.getId() + " for owner id " + ownerOrg.getId());

    ArgumentCaptor<TelemetryData> telemetryCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender, never()).send(telemetryCaptor.capture());
  }

  @Test
  public void testUpdatePolicyNotifications_OnlyNotificationsUpdated() throws Exception {
    Organization ownerOrg = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(ownerOrg);
    Policy originalPolicy = policyDAO.getById(policy.getId());

    policy.setPolicyNotificationsOverrideAllowed(!policy.isPolicyNotificationsOverrideAllowed());
    policy.setPolicyNotificationsOverrides(Map.of(ownerOrg.getId(), new Notifications(
        new UserNotification("test1@email.com", Stage.ID_BUILD, Stage.ID_STAGE_RELEASE, Stage.ID_OPERATE))));
    policy.setNotifications(new Notifications(
        new UserNotification("test2@email.com", Stage.ID_BUILD, Stage.ID_STAGE_RELEASE, Stage.ID_OPERATE)));

    // Fields that should not change
    policy.setName("new-name");
    policy.setThreatLevel(0);
    policy.setLegacyViolationAllowed(!policy.isLegacyViolationAllowed());
    Constraint constraint = new Constraint(TemporaryEntity.uuid(), "test-constraint", LogicalOperator.AND);
    constraint.setConditions(List.of(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0")));
    policy.setConstraints(List.of(constraint));

    HttpResponse response =
        restRequest(OwnerType.ORGANIZATION, ownerOrg.getId()).path(NOTIFICATIONS_PATH).body(policy).put();

    assertResponseStatus(200, response);
    Policy result = response.getBody(Policy.class);

    assertThat(result).isNotNull();

    String[] ignoredFields = ArrayUtils.addAll(JPA.IGNORE_FIELDS, "droolsCode", "policyNotificationsOverrideAllowed",
        "policyNotificationsOverrides", "notifications");
    assertThat(result).usingRecursiveComparison()
        .ignoringFields(ignoredFields)
        .ignoringCollectionOrder()
        .isEqualTo(originalPolicy);

    assertThat(result.isPolicyActionsOverrideAllowed()).isEqualTo(policy.isPolicyActionsOverrideAllowed());
    assertThat(result.getPolicyNotificationsOverrides()).isEqualTo(policy.getPolicyNotificationsOverrides());
    assertThat(result.getNotifications()).isEqualTo(policy.getNotifications());

    ArgumentCaptor<TelemetryData> telemetryCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender, times(1)).send(telemetryCaptor.capture());
    assertTelemetry(telemetryCaptor.getValue(), "UPDATE", ownerOrg.getId(), result);
  }

  private void testCRUD(OwnerType ownerType, String ownerId) throws Exception {
    // Add a policy
    Policy policy = new Policy();
    policy.setName("PolicyResourceTest new policy");
    Constraint constraint = new Constraint();
    constraint.setName("PolicyResourceTest new constraint");
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    HttpResponse response = restRequest(ownerType, ownerId).body(policy).post();
    assertResponseStatus(200, response);
    final Policy policy1 = response.getBody(Policy.class);
    assertThat(policy1.getId()).isNotNull();
    assertThat(policy1.getName()).isEqualTo("PolicyResourceTest new policy");

    // Get all policies
    response = restRequest(ownerType, ownerId).get();
    assertResponseStatus(200, response);
    Policy[] policies = response.getBody(Policy[].class);
    assertThat(policies).hasSize(1);
    assertThat(policies[0].getId()).isEqualTo(policy1.getId());
    assertThat(policies[0].getName()).isEqualTo(policy1.getName());

    // Update a policy
    policy = policies[0];
    policy.setName("PolicyResourceTest updated policy");
    response = restRequest(ownerType, ownerId).body(policy).put();
    assertResponseStatus(200, response);
    final Policy policy2 = response.getBody(Policy.class);
    assertThat(policy2.getName()).isEqualTo("PolicyResourceTest updated policy");

    // Get all policies
    response = restRequest(ownerType, ownerId).get();
    assertResponseStatus(200, response);
    policies = response.getBody(Policy[].class);
    assertThat(policies).hasSize(1);
    assertThat(policies[0].getId()).isEqualTo(policy2.getId());
    assertThat(policies[0].getName()).isEqualTo(policy2.getName());

    // Delete a policy
    policy = policies[0];
    response = restRequest(ownerType, ownerId).path(policy.getId()).delete();
    assertResponseStatus(204, response);

    ArgumentCaptor<TelemetryData> telemetryCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender, times(3)).send(telemetryCaptor.capture());
    List<TelemetryData> capturedTelemetry = telemetryCaptor.getAllValues();
    assertTelemetry(capturedTelemetry.get(0), "CREATE", ownerId, policy1);
    assertTelemetry(capturedTelemetry.get(1), "UPDATE", ownerId, policy2);
    assertTelemetry(capturedTelemetry.get(2), "DELETE", ownerId, policy);

    // Get all policies
    response = restRequest(ownerType, ownerId).get();
    assertResponseStatus(200, response);
    policies = response.getBody(Policy[].class);
    assertThat(policies).isEmpty();
  }

  private void assertTelemetry(TelemetryData sentTelemetry, String action, String ownerId, Policy policy) {
    assertThat(sentTelemetry.getPurpose()).isEqualTo(TelemetryPurpose.POLICY_MAINTENANCE);
    Map<String, Object> attributes = sentTelemetry.getAttributes();
    assertThat(attributes.get("owner_id")).isEqualTo("obfuscated-" + ownerId);
    assertThat(attributes.get("event_action")).isEqualTo(action);
    assertThat(attributes.get("policy_name")).isEqualTo(policy.getName());
    assertThat(attributes.get("threat_level")).isEqualTo(policy.getThreatLevel());
    assertThat(attributes.get("policy_constraints")).usingRecursiveComparison().isEqualTo(policy.getConstraints());
    assertThat(attributes.get("policy_actions"))
        .usingRecursiveComparison()
        .isEqualTo(PolicyMaintenanceTelemetry.getActionsList(Collections.singletonList(policy.getActions())));
    assertThat(attributes.get("policy_actions_overrides"))
        .usingRecursiveComparison()
        .isEqualTo(policy.getPolicyActionsOverrides() != null
            ? PolicyMaintenanceTelemetry.getActionsList(policy.getPolicyActionsOverrides().values())
            : List.of());
    assertThat(attributes.get("policy_notifications"))
        .usingRecursiveComparison()
        .isEqualTo(
            PolicyMaintenanceTelemetry.getNotificationTypes(Collections.singletonList(policy.getNotifications())));
    assertThat(attributes.get("policy_notifications_overrides"))
        .usingRecursiveComparison()
        .isEqualTo(
            policy.getPolicyNotificationsOverrides() != null
                ? PolicyMaintenanceTelemetry.getNotificationTypes(
                    policy.getPolicyNotificationsOverrides().values())
                : Set.of());
  }

  @Test
  public void testAddPolicy_InvalidPolicy_AppLevel() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    testAddPolicy_InvalidPolicy(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test
  public void testAddPolicy_InvalidPolicy_OrgLevel() throws Exception {
    String orgId = tempEntity.newOrganization().getId();
    testAddPolicy_InvalidPolicy(OwnerType.ORGANIZATION, orgId);
  }

  private void testAddPolicy_InvalidPolicy(OwnerType ownerType, String ownerId) throws Exception {
    Policy policy = new Policy();
    policy.setName(null);
    Constraint constraint = new Constraint();
    constraint.setName("PolicyResourceTest new constraint");
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    HttpResponse response = restRequest(ownerType, ownerId).body(policy).post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("The policy name is required.");
    ArgumentCaptor<TelemetryData> telemetryCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender, never()).send(telemetryCaptor.capture());
  }

  @Test
  public void testUpdatePolicy_InvalidPolicy_AppLevel() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    testUpdatePolicy_InvalidPolicy(OwnerType.APPLICATION, app.getId(), app.getPublicId());
  }

  @Test
  public void testUpdatePolicy_InvalidPolicy_OrgLevel() throws Exception {
    String orgId = tempEntity.newOrganization().getId();
    testUpdatePolicy_InvalidPolicy(OwnerType.ORGANIZATION, orgId, orgId);
  }

  @Test
  public void testUpdateOverrides_Add_ForActions_Organization() throws Exception {
    testUpdateOverrides_Add_ForActions(tempEntity.newOrganization());
  }

  @Test
  public void testUpdateOverrides_Add_ForActions_Application() throws Exception {
    testUpdateOverrides_Add_ForActions(tempEntity.newApplicationWithParent());
  }

  @Test
  public void testUpdateOverrides_Add_ForActions_RepositoryContainer() throws Exception {
    testUpdateOverrides_Add_ForActions(RepositoryContainer.SINGLETON);
  }

  @Test
  public void testUpdateOverrides_Add_ForActions_RepositoryManager() throws Exception {
    testUpdateOverrides_Add_ForActions(tempEntity.newRepositoryManager());
  }

  @Test
  public void testUpdateOverrides_Add_ForActions_Repository() throws Exception {
    testUpdateOverrides_Add_ForActions(tempEntity.newRepository());
  }

  private void testUpdateOverrides_Add_ForActions(Owner owner) throws Exception {
    // The policy is create for the root org.
    Policy policy = tempEntity.newPolicy();
    policy.setPolicyActionsOverrideAllowed(true);
    policyDAO.update(policy);

    Map<String, String> actionsOverride = new LinkedHashMap<>();
    actionsOverride.put("stage-release", "fail");
    actionsOverride.put("release", "fail");
    actionsOverride.put("build", "warn");

    String ownerId = OwnerType.APPLICATION.equals(owner.getType()) ? owner.getPublicId() : owner.getId();
    HttpResponse response = restRequest(owner.getType(), ownerId)
        .path(policy.getId(), "overrides")
        .body(new PolicyOverridesDTO(actionsOverride))
        .put();

    assertResponseStatus(200, response);
    final Policy updatedPolicy = response.getBody(Policy.class);
    Map<String, String> savedActionsOverride = updatedPolicy.getPolicyActionsOverrides().get(owner.getId());
    assertThat(savedActionsOverride).isNotNull();
    assertThat(savedActionsOverride.size()).isEqualTo(3);
    assertThat(savedActionsOverride.get("stage-release")).isEqualTo("fail");
    assertThat(savedActionsOverride.get("release")).isEqualTo("fail");
    assertThat(savedActionsOverride.get("build")).isEqualTo("warn");

    ArgumentCaptor<TelemetryData> telemetryCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender, times(1)).send(telemetryCaptor.capture());
    assertTelemetry(telemetryCaptor.getValue(), "UPDATE", ownerId, updatedPolicy);
  }

  @Test
  public void testUpdateOverrides_Add_ForNotifications_Organization() throws Exception {
    testUpdateOverrides_Add_ForNotifications(tempEntity.newOrganization());
  }

  @Test
  public void testUpdateOverrides_Add_ForNotifications_Application() throws Exception {
    testUpdateOverrides_Add_ForNotifications(tempEntity.newApplicationWithParent());
  }

  @Test
  public void testUpdateOverrides_Add_ForNotifications_RepositoryContainer() throws Exception {
    testUpdateOverrides_Add_ForNotifications(RepositoryContainer.SINGLETON);
  }

  @Test
  public void testUpdateOverrides_Add_ForNotifications_RepositoryManager() throws Exception {
    testUpdateOverrides_Add_ForNotifications(tempEntity.newRepositoryManager());
  }

  @Test
  public void testUpdateOverrides_Add_ForNotifications_Repository() throws Exception {
    testUpdateOverrides_Add_ForNotifications(tempEntity.newRepository());
  }

  private void testUpdateOverrides_Add_ForNotifications(Owner owner) throws Exception {
    // The policy is create for the root org.
    Policy policy = tempEntity.newPolicy();
    policy.setPolicyNotificationsOverrideAllowed(true);
    policyDAO.update(policy);
    Notifications notificationsOverride = new Notifications();
    notificationsOverride.add(new UserNotification("app@domain.com", BuildStageType.ID));

    String ownerId = OwnerType.APPLICATION.equals(owner.getType()) ? owner.getPublicId() : owner.getId();
    HttpResponse response = restRequest(owner.getType(), ownerId)
        .path(policy.getId(), "overrides")
        .body(new PolicyOverridesDTO(notificationsOverride))
        .put();

    assertResponseStatus(200, response);
    Policy updatedPolicy = response.getBody(Policy.class);
    Notifications savedNotificationsOverride = updatedPolicy.getPolicyNotificationsOverrides().get(owner.getId());
    assertThat(notificationsOverride).usingRecursiveComparison().isEqualTo(savedNotificationsOverride);

    ArgumentCaptor<TelemetryData> telemetryCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender, times(1)).send(telemetryCaptor.capture());
    assertTelemetry(telemetryCaptor.getValue(), "UPDATE", ownerId, updatedPolicy);
  }

  @Test
  public void testUpdateOverrides_Add_ForActions_policyActionsOverrideIsDisabled() throws Exception {
    Policy policy = tempEntity.newPolicy();

    Application app = tempEntity.newApplicationWithParent();

    Map<String, String> actionsOverride = new LinkedHashMap<>();
    actionsOverride.put("stage-release", "fail");
    actionsOverride.put("release", "fail");
    actionsOverride.put("build", "warn");

    HttpResponse response = restRequest(OwnerType.APPLICATION, app.getPublicId())
        .path(policy.getId(), "overrides")
        .body(new PolicyOverridesDTO(actionsOverride))
        .put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .isEqualTo("Actions override is not allowed for policy with id " + policy.getId());

    ArgumentCaptor<TelemetryData> telemetryCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender, never()).send(telemetryCaptor.capture());
  }

  @Test
  public void testUpdateOverrides_Add_ForNotifications_policyNotificationsOverrideIsDisabled() throws Exception {
    Policy policy = tempEntity.newPolicy();
    Application app = tempEntity.newApplicationWithParent();
    Notifications notificationsOverride = new Notifications();
    notificationsOverride.add(new UserNotification("app@domain.com", BuildStageType.ID));

    HttpResponse response = restRequest(OwnerType.APPLICATION, app.getPublicId())
        .path(policy.getId(), "overrides")
        .body(new PolicyOverridesDTO(notificationsOverride))
        .put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(
        "Notifications override is not allowed for policy with id " + policy.getId());

    ArgumentCaptor<TelemetryData> telemetryCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender, never()).send(telemetryCaptor.capture());
  }

  @Test
  public void testUpdateOverrides_Add_invalidPolicyId() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    HttpResponse response = restRequest(OwnerType.APPLICATION, app.getPublicId())
        .path("123", "overrides")
        .body(new PolicyOverridesDTO(new LinkedHashMap<>()))
        .put();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("PolicyInternal with ID 123 does not exist.");

    ArgumentCaptor<TelemetryData> telemetryCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender, never()).send(telemetryCaptor.capture());
  }

  @Test
  public void testUpdateOverrides_nullJson() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application.getParentOwnerId());

    HttpResponse response =
        restRequest(OwnerType.APPLICATION, application.getPublicId())
            .path(policy.getId(), "overrides")
            .put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("A policy overrides configuration must be specified.");

    ArgumentCaptor<TelemetryData> telemetryCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender, never()).send(telemetryCaptor.capture());
  }

  @Test
  public void testUpdateOverrides_noOverrides() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application.getParentOwnerId());

    HttpResponse response = restRequest(OwnerType.APPLICATION, application.getPublicId())
        .path(policy.getId(), "overrides")
        .body(new ObjectMapper().createObjectNode())
        .put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("A policy overrides configuration must be specified.");

    ArgumentCaptor<TelemetryData> telemetryCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender, never()).send(telemetryCaptor.capture());
  }

  @Test
  public void testUpdateOverrides_badJson() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application.getParentOwnerId());
    ObjectNode objectNode = new ObjectMapper().createObjectNode();
    objectNode.put("actions", 1);

    HttpResponse response = restRequest(OwnerType.APPLICATION, application.getPublicId())
        .path(policy.getId(), "overrides")
        .body(objectNode)
        .put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(
        "The given JSON cannot be deserialized into a policy overrides configuration.");

    ArgumentCaptor<TelemetryData> telemetryCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender, never()).send(telemetryCaptor.capture());
  }

  private JsonNode createDeleteBody(boolean deleteActionsOverride, boolean deleteNotificationsOverride) {
    ObjectNode json = JsonUtils.asTree(new PolicyOverridesDTO());
    if (!deleteActionsOverride) {
      json.remove("actions");
    }
    if (!deleteNotificationsOverride) {
      json.remove("notifications");
    }
    return json;
  }

  @Test
  public void testUpdateOverrides_Delete_ForActions_Organization() throws Exception {
    testUpdateOverrides_Delete_ForActions(tempEntity.newOrganization());
  }

  @Test
  public void testUpdateOverrides_Delete_ForActions_Application() throws Exception {
    testUpdateOverrides_Delete_ForActions(tempEntity.newApplicationWithParent());
  }

  @Test
  public void testUpdateOverrides_Delete_ForActions_RepositoryContainer() throws Exception {
    testUpdateOverrides_Delete_ForActions(RepositoryContainer.SINGLETON);
  }

  @Test
  public void testUpdateOverrides_Delete_ForActions_RepositoryManager() throws Exception {
    testUpdateOverrides_Delete_ForActions(tempEntity.newRepositoryManager());
  }

  @Test
  public void testUpdateOverrides_Delete_ForActions_Repository() throws Exception {
    testUpdateOverrides_Delete_ForActions(tempEntity.newRepository());
  }

  private void testUpdateOverrides_Delete_ForActions(Owner owner) throws Exception {
    // The policy is create for the root org.
    Policy policy = tempEntity.newPolicy();

    Map<String, String> ownerActionsOverrides = new LinkedHashMap<>();
    ownerActionsOverrides.put("stage-release", "fail");
    ownerActionsOverrides.put("release", "fail");
    ownerActionsOverrides.put("build", "warn");

    Map<String, String> parentOwnerActionsOverrides = new LinkedHashMap<>();
    parentOwnerActionsOverrides.put("stage-release", "warn");
    parentOwnerActionsOverrides.put("release", "warn");

    policy.addPolicyActionsOverride(owner.getId(), ownerActionsOverrides);
    policy.addPolicyActionsOverride(owner.getParentOwnerId(), parentOwnerActionsOverrides);
    policyDAO.update(policy);

    String ownerId = OwnerType.APPLICATION.equals(owner.getType()) ? owner.getPublicId() : owner.getId();
    HttpResponse response =
        restRequest(owner.getType(), ownerId)
            .path(policy.getId(), "overrides")
            .body(createDeleteBody(true, false))
            .put();

    assertResponseStatus(200, response);
    final Policy updatedPolicy = response.getBody(Policy.class);
    assertThat(updatedPolicy.getPolicyActionsOverrides())
        .hasSize(1)
        .containsEntry(owner.getParentOwnerId(), parentOwnerActionsOverrides);

    Policy policyOnDB = policyDAO.getById(policy.getId());
    assertThat(policyOnDB.getPolicyActionsOverrides())
        .hasSize(1)
        .containsEntry(owner.getParentOwnerId(), parentOwnerActionsOverrides);

    ArgumentCaptor<TelemetryData> telemetryCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender, times(1)).send(telemetryCaptor.capture());
    assertTelemetry(telemetryCaptor.getValue(), "UPDATE", ownerId, updatedPolicy);
  }

  @Test
  public void testUpdateOverrides_Delete_ForNotifications_Organization() throws Exception {
    testUpdateOverrides_Delete_ForNotifications(tempEntity.newOrganization());
  }

  @Test
  public void testUpdateOverrides_Delete_ForNotifications_Application() throws Exception {
    testUpdateOverrides_Delete_ForNotifications(tempEntity.newApplicationWithParent());
  }

  @Test
  public void testUpdateOverrides_Delete_ForNotifications_RepositoryContainer() throws Exception {
    testUpdateOverrides_Delete_ForNotifications(RepositoryContainer.SINGLETON);
  }

  @Test
  public void testUpdateOverrides_Delete_ForNotifications_RepositoryManager() throws Exception {
    testUpdateOverrides_Delete_ForNotifications(tempEntity.newRepositoryManager());
  }

  @Test
  public void testUpdateOverrides_Delete_ForNotifications_Repository() throws Exception {
    testUpdateOverrides_Delete_ForNotifications(tempEntity.newRepository());
  }

  private void testUpdateOverrides_Delete_ForNotifications(Owner owner) throws Exception {
    // The policy is create for the root org.
    Policy policy = tempEntity.newPolicy();

    Notifications ownerNotificationsOverride = new Notifications();
    ownerNotificationsOverride.add(new UserNotification("app@domain.com", BuildStageType.ID));

    Notifications parentOwnerNotificationsOverride = new Notifications();
    parentOwnerNotificationsOverride.add(new UserNotification("org@domain.com", ReleaseStageType.ID));

    policy.addPolicyNotificationsOverride(owner.getId(), ownerNotificationsOverride);
    policy.addPolicyNotificationsOverride(owner.getParentOwnerId(), parentOwnerNotificationsOverride);
    policyDAO.update(policy);

    String ownerId = OwnerType.APPLICATION.equals(owner.getType()) ? owner.getPublicId() : owner.getId();
    HttpResponse response =
        restRequest(owner.getType(), ownerId)
            .path(policy.getId(), "overrides")
            .body(createDeleteBody(false, true))
            .put();

    assertResponseStatus(200, response);
    Policy updatedPolicy = response.getBody(Policy.class);
    assertThat(updatedPolicy.getPolicyNotificationsOverrides())
        .hasSize(1)
        .containsEntry(owner.getParentOwnerId(), parentOwnerNotificationsOverride);

    Policy policyOnDB = policyDAO.getById(policy.getId());
    assertThat(policyOnDB.getPolicyNotificationsOverrides())
        .hasSize(1)
        .containsEntry(owner.getParentOwnerId(), parentOwnerNotificationsOverride);

    ArgumentCaptor<TelemetryData> telemetryCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender, times(1)).send(telemetryCaptor.capture());
    assertTelemetry(telemetryCaptor.getValue(), "UPDATE", ownerId, updatedPolicy);
  }

  @Test
  public void testUpdateOverrides_Delete_ForActions_OverrideDoesNotExist_Organization() throws Exception {
    testUpdateOverrides_Delete_ForActions_OverrideDoesNotExist(tempEntity.newOrganization());
  }

  @Test
  public void testUpdateOverrides_Delete_ForActions_OverrideDoesNotExist_Application() throws Exception {
    testUpdateOverrides_Delete_ForActions_OverrideDoesNotExist(tempEntity.newApplicationWithParent());
  }

  @Test
  public void testUpdateOverrides_Delete_ForActions_OverrideDoesNotExist_RepositoryContainer() throws Exception {
    testUpdateOverrides_Delete_ForActions_OverrideDoesNotExist(RepositoryContainer.SINGLETON);
  }

  @Test
  public void testUpdateOverrides_Delete_ForActions_OverrideDoesNotExist_RepositoryManager() throws Exception {
    testUpdateOverrides_Delete_ForActions_OverrideDoesNotExist(tempEntity.newRepositoryManager());
  }

  @Test
  public void testUpdateOverrides_Delete_ForActions_OverrideDoesNotExist_Repository() throws Exception {
    testUpdateOverrides_Delete_ForActions_OverrideDoesNotExist(tempEntity.newRepository());
  }

  private void testUpdateOverrides_Delete_ForActions_OverrideDoesNotExist(Owner owner) throws Exception {
    // The policy is create for the root org.
    Policy policy = tempEntity.newPolicy();

    Map<String, String> parentOwnerActionsOverrides = new LinkedHashMap<>();
    parentOwnerActionsOverrides.put("stage-release", "warn");
    parentOwnerActionsOverrides.put("release", "warn");

    policy.addPolicyActionsOverride(owner.getParentOwnerId(), parentOwnerActionsOverrides);
    policyDAO.update(policy);

    String ownerId = OwnerType.APPLICATION.equals(owner.getType()) ? owner.getPublicId() : owner.getId();
    HttpResponse response =
        restRequest(owner.getType(), ownerId)
            .path(policy.getId(), "overrides")
            .body(createDeleteBody(true, false))
            .put();

    assertResponseStatus(200, response);
    final Policy updatedPolicy = response.getBody(Policy.class);
    assertThat(updatedPolicy.getPolicyActionsOverrides())
        .hasSize(1)
        .containsEntry(owner.getParentOwnerId(), parentOwnerActionsOverrides);

    Policy policyOnDB = policyDAO.getById(policy.getId());
    assertThat(policyOnDB.getPolicyActionsOverrides())
        .hasSize(1)
        .containsEntry(owner.getParentOwnerId(), parentOwnerActionsOverrides);

    ArgumentCaptor<TelemetryData> telemetryCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender, times(1)).send(telemetryCaptor.capture());
    assertTelemetry(telemetryCaptor.getValue(), "UPDATE", ownerId, updatedPolicy);
  }

  @Test
  public void testUpdateOverrides_Delete_ForNotifications_OverrideDoesNotExist_Organization() throws Exception {
    testUpdateOverrides_Delete_ForNotifications_OverrideDoesNotExist(tempEntity.newOrganization());
  }

  @Test
  public void testUpdateOverrides_Delete_ForNotifications_OverrideDoesNotExist_Application() throws Exception {
    testUpdateOverrides_Delete_ForNotifications_OverrideDoesNotExist(tempEntity.newApplicationWithParent());
  }

  @Test
  public void testUpdateOverrides_Delete_ForNotifications_OverrideDoesNotExist_RepositoryContainer() throws Exception {
    testUpdateOverrides_Delete_ForNotifications_OverrideDoesNotExist(RepositoryContainer.SINGLETON);
  }

  @Test
  public void testUpdateOverrides_Delete_ForNotifications_OverrideDoesNotExist_RepositoryManager() throws Exception {
    testUpdateOverrides_Delete_ForNotifications_OverrideDoesNotExist(tempEntity.newRepositoryManager());
  }

  @Test
  public void testUpdateOverrides_Delete_ForNotifications_OverrideDoesNotExist_Repository() throws Exception {
    testUpdateOverrides_Delete_ForNotifications_OverrideDoesNotExist(tempEntity.newRepository());
  }

  private void testUpdateOverrides_Delete_ForNotifications_OverrideDoesNotExist(Owner owner) throws Exception {
    // The policy is create for the root org.
    Policy policy = tempEntity.newPolicy();

    Notifications parentOwnerNotificationsOverride = new Notifications();
    parentOwnerNotificationsOverride.add(new UserNotification("org@domain.com", ReleaseStageType.ID));

    policy.addPolicyNotificationsOverride(owner.getParentOwnerId(), parentOwnerNotificationsOverride);
    policyDAO.update(policy);

    String ownerId = OwnerType.APPLICATION.equals(owner.getType()) ? owner.getPublicId() : owner.getId();
    HttpResponse response =
        restRequest(owner.getType(), ownerId)
            .path(policy.getId(), "overrides")
            .body(createDeleteBody(false, true))
            .put();

    assertResponseStatus(200, response);
    Policy updatedPolicy = response.getBody(Policy.class);
    assertThat(updatedPolicy.getPolicyNotificationsOverrides())
        .hasSize(1)
        .containsEntry(owner.getParentOwnerId(), parentOwnerNotificationsOverride);

    Policy policyOnDB = policyDAO.getById(policy.getId());
    assertThat(policyOnDB.getPolicyNotificationsOverrides())
        .hasSize(1)
        .containsEntry(owner.getParentOwnerId(), parentOwnerNotificationsOverride);

    ArgumentCaptor<TelemetryData> telemetryCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender, times(1)).send(telemetryCaptor.capture());
    assertTelemetry(telemetryCaptor.getValue(), "UPDATE", ownerId, updatedPolicy);
  }

  private void testUpdatePolicy_InvalidPolicy(
      OwnerType ownerType,
      String ownerId,
      String publicOwnerid) throws Exception
  {
    // Create a valid policy
    Policy policy = new Policy();
    policy.setOwnerId(ownerId);
    policy.setName("PolicyResourceTest-testUpdateInvalidPolicy");
    Constraint constraint = new Constraint();
    constraint.setName("PolicyResourceTest new constraint");
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    tempEntity.newPolicy(policy);

    // Update invalid policy
    policy.setName(null);
    HttpResponse response = restRequest(ownerType, publicOwnerid).body(policy).put();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("The policy name is required.");

    ArgumentCaptor<TelemetryData> telemetryCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender, never()).send(telemetryCaptor.capture());
  }

  private void assertPoliciesByOwner(
      String ownerId,
      String ownerName,
      OwnerType ownerType,
      int policyCount,
      PoliciesByOwner actual)
  {
    assertThat(actual.ownerId).isEqualTo(ownerId);
    assertThat(actual.ownerName).isEqualTo(ownerName);
    assertThat(actual.ownerType).isEqualTo(ownerType);
    assertThat(actual.policies).hasSize(policyCount);
  }

  @Test
  public void testGetPoliciesWithProprietaryNameConflictAndSecurityVulnerabilityCategoryMaliciousCode() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager);

    // Given a test policy with a condition that should not be included in the filter.
    createTestPolicyWithCondition("test-policy-a", RepositoryContainer.REPOSITORY_CONTAINER_ID, false, false, true);
    createTestPolicyWithCondition("test-policy-b", repositoryManager.getId(), false, false, true);
    createTestPolicyWithCondition("test-policy-c", repository.getId(), false, false, true);
    // And test policies with conditions that should be included in the filter.
    createTestPolicyWithCondition("test-policy-1", RepositoryContainer.REPOSITORY_CONTAINER_ID, false, false, true);
    createTestPolicyWithCondition("test-policy-2", RepositoryContainer.REPOSITORY_CONTAINER_ID, true, false, true);
    createTestPolicyWithCondition("test-policy-3", RepositoryContainer.REPOSITORY_CONTAINER_ID, false, true, true);
    createTestPolicyWithCondition("test-policy-4", RepositoryContainer.REPOSITORY_CONTAINER_ID, true, true, true);

    createTestPolicyWithCondition("test-policy-5", repositoryManager.getId(), false, false, true);
    createTestPolicyWithCondition("test-policy-6", repositoryManager.getId(), true, false, true);
    createTestPolicyWithCondition("test-policy-7", repositoryManager.getId(), false, true, true);
    createTestPolicyWithCondition("test-policy-8", repositoryManager.getId(), true, true, true);

    createTestPolicyWithCondition("test-policy-9", repository.getId(), false, false, true);
    createTestPolicyWithCondition("test-policy-10", repository.getId(), true, false, true);
    createTestPolicyWithCondition("test-policy-11", repository.getId(), false, true, true);
    createTestPolicyWithCondition("test-policy-12", repository.getId(), true, true, true);

    // when the policies are retrieved
    HttpResponse response = restRequest(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID)
        .path("withProprietaryNameConflictAndSecurityVulnerabilityCategoryMaliciousCode")
        .get();
    assertResponseStatus(200, response);
    ProprietaryNameConflictAndSecurityVulnerabilityCategoryMaliciousCodePolicies result =
        response.getBody(ProprietaryNameConflictAndSecurityVulnerabilityCategoryMaliciousCodePolicies.class);

    assertThat(result.proprietaryNameConflictPolicies).hasSize(6);
    assertThat(result.securityVulnerabilityCategoryMaliciousCodePolicies).hasSize(6);

    assertThat(result.proprietaryNameConflictPolicies.stream().map(Policy::getName)).containsExactlyInAnyOrder(
        "test-policy-3", "test-policy-4", "test-policy-7", "test-policy-8", "test-policy-11", "test-policy-12");
    assertThat(result.securityVulnerabilityCategoryMaliciousCodePolicies.stream().map(Policy::getName))
        .containsExactlyInAnyOrder(
            "test-policy-2", "test-policy-4", "test-policy-6", "test-policy-8", "test-policy-10", "test-policy-12");
  }

  @Test
  public void testGetApplicablePolicies_OrgsAndApps() throws Exception {
    // Create an organization and an application
    String orgName = "testGetApplicablePoliciesOrg";
    Organization org = tempEntity.newOrganization(orgName);
    String orgId = org.getId();
    Organization parentOrg = organizationDAO.getById(org.getParentOrganizationId());
    String parentOrgId = parentOrg.getId();
    String parentOrgName = parentOrg.getName();
    String appName = "testGetApplicablePoliciesApp";
    String appPublicId = appName;
    Application app = tempEntity.newApplication(appName, appPublicId, orgId);
    String appId = app.getId();

    // Verify the applicable policies for the application
    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable").get();
    assertResponseStatus(200, response);
    ApplicablePolicies applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertThat(applicablePolicies).isNotNull();
    assertThat(applicablePolicies.policiesByOwner).hasSize(3);
    assertPoliciesByOwner(appId, appName, OwnerType.APPLICATION, 0, applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(orgId, orgName, OwnerType.ORGANIZATION, 0, applicablePolicies.policiesByOwner.get(1));
    assertPoliciesByOwner(parentOrgId, parentOrgName, OwnerType.ORGANIZATION, 0,
        applicablePolicies.policiesByOwner.get(2));

    // Verify the applicable policies for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertThat(applicablePolicies).isNotNull();
    assertThat(applicablePolicies.policiesByOwner).hasSize(2);
    assertPoliciesByOwner(orgId, orgName, OwnerType.ORGANIZATION, 0, applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(parentOrgId, parentOrgName, OwnerType.ORGANIZATION, 0,
        applicablePolicies.policiesByOwner.get(1));

    // Verify the applicable policies for the parent organization
    response = restRequest(OwnerType.ORGANIZATION, parentOrgId).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertThat(applicablePolicies).isNotNull();
    assertThat(applicablePolicies.policiesByOwner).hasSize(1);
    assertPoliciesByOwner(parentOrgId, parentOrgName, OwnerType.ORGANIZATION, 0,
        applicablePolicies.policiesByOwner.get(0));

    // Create a policy for the application
    Policy appPolicy = tempEntity.newPolicy(app);

    // Verify the applicable policies for the application
    response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertThat(applicablePolicies).isNotNull();
    assertThat(applicablePolicies.policiesByOwner).hasSize(3);
    assertPoliciesByOwner(appId, appName, OwnerType.APPLICATION, 1, applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(orgId, orgName, OwnerType.ORGANIZATION, 0, applicablePolicies.policiesByOwner.get(1));
    assertPoliciesByOwner(parentOrgId, parentOrgName, OwnerType.ORGANIZATION, 0,
        applicablePolicies.policiesByOwner.get(2));
    assertThat(applicablePolicies.policiesByOwner.get(0).policies.get(0).getId()).isEqualTo(appPolicy.getId());

    // Verify the applicable policies for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertThat(applicablePolicies).isNotNull();
    assertThat(applicablePolicies.policiesByOwner).hasSize(2);
    assertPoliciesByOwner(orgId, orgName, OwnerType.ORGANIZATION, 0, applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(parentOrgId, parentOrgName, OwnerType.ORGANIZATION, 0,
        applicablePolicies.policiesByOwner.get(1));

    // Verify the applicable policies for the parent organization
    response = restRequest(OwnerType.ORGANIZATION, parentOrgId).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertThat(applicablePolicies).isNotNull();
    assertThat(applicablePolicies.policiesByOwner).hasSize(1);
    assertPoliciesByOwner(parentOrgId, parentOrgName, OwnerType.ORGANIZATION, 0,
        applicablePolicies.policiesByOwner.get(0));

    // Create a policy for the organization
    Policy orgPolicy = tempEntity.newPolicy(org);

    // Verify the applicable policies for the application
    response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertThat(applicablePolicies).isNotNull();
    assertThat(applicablePolicies.policiesByOwner).hasSize(3);
    assertPoliciesByOwner(appId, appName, OwnerType.APPLICATION, 1, applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(orgId, orgName, OwnerType.ORGANIZATION, 1, applicablePolicies.policiesByOwner.get(1));
    assertPoliciesByOwner(parentOrgId, parentOrgName, OwnerType.ORGANIZATION, 0,
        applicablePolicies.policiesByOwner.get(2));
    assertThat(applicablePolicies.policiesByOwner.get(0).policies.get(0).getId()).isEqualTo(appPolicy.getId());
    assertThat(applicablePolicies.policiesByOwner.get(1).policies.get(0).getId()).isEqualTo(orgPolicy.getId());

    // Verify the applicable policies for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertThat(applicablePolicies).isNotNull();
    assertThat(applicablePolicies.policiesByOwner).hasSize(2);
    assertPoliciesByOwner(orgId, orgName, OwnerType.ORGANIZATION, 1, applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(parentOrgId, parentOrgName, OwnerType.ORGANIZATION, 0,
        applicablePolicies.policiesByOwner.get(1));
    assertThat(applicablePolicies.policiesByOwner.get(0).policies.get(0).getId()).isEqualTo(orgPolicy.getId());

    // Verify the applicable policies for the parent organization
    response = restRequest(OwnerType.ORGANIZATION, parentOrgId).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertThat(applicablePolicies).isNotNull();
    assertThat(applicablePolicies.policiesByOwner).hasSize(1);
    assertPoliciesByOwner(parentOrgId, parentOrgName, OwnerType.ORGANIZATION, 0,
        applicablePolicies.policiesByOwner.get(0));

    // Create a policy for the parent organization
    Policy parentOrgPolicy = tempEntity.newPolicy(parentOrg);

    // Verify the applicable policies for the application
    response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertThat(applicablePolicies).isNotNull();
    assertThat(applicablePolicies.policiesByOwner).hasSize(3);
    assertPoliciesByOwner(appId, appName, OwnerType.APPLICATION, 1, applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(orgId, orgName, OwnerType.ORGANIZATION, 1, applicablePolicies.policiesByOwner.get(1));
    assertPoliciesByOwner(parentOrgId, parentOrgName, OwnerType.ORGANIZATION, 1,
        applicablePolicies.policiesByOwner.get(2));
    assertThat(applicablePolicies.policiesByOwner.get(0).policies.get(0).getId()).isEqualTo(appPolicy.getId());
    assertThat(applicablePolicies.policiesByOwner.get(1).policies.get(0).getId()).isEqualTo(orgPolicy.getId());
    assertThat(applicablePolicies.policiesByOwner.get(2).policies.get(0).getId()).isEqualTo(parentOrgPolicy.getId());

    // Verify the applicable policies for the organization
    response = restRequest(OwnerType.ORGANIZATION, orgId).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertThat(applicablePolicies).isNotNull();
    assertThat(applicablePolicies.policiesByOwner).hasSize(2);
    assertPoliciesByOwner(orgId, orgName, OwnerType.ORGANIZATION, 1, applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(parentOrgId, parentOrgName, OwnerType.ORGANIZATION, 1,
        applicablePolicies.policiesByOwner.get(1));
    assertThat(applicablePolicies.policiesByOwner.get(0).policies.get(0).getId()).isEqualTo(orgPolicy.getId());
    assertThat(applicablePolicies.policiesByOwner.get(1).policies.get(0).getId()).isEqualTo(parentOrgPolicy.getId());

    // Verify the applicable policies for the parent organization
    response = restRequest(OwnerType.ORGANIZATION, parentOrgId).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertThat(applicablePolicies).isNotNull();
    assertThat(applicablePolicies.policiesByOwner).hasSize(1);
    assertPoliciesByOwner(parentOrgId, parentOrgName, OwnerType.ORGANIZATION, 1,
        applicablePolicies.policiesByOwner.get(0));
    assertThat(applicablePolicies.policiesByOwner.get(0).policies.get(0).getId()).isEqualTo(parentOrgPolicy.getId());
  }

  @Test
  public void testGetApplicablePolicies_RepositoryContainer() throws Exception {
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);

    // Verify the applicable policies for the repository container
    HttpResponse response = restRequest(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID)
        .path("applicable")
        .get();
    assertResponseStatus(200, response);
    ApplicablePolicies applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertThat(applicablePolicies.policiesByOwner).hasSize(2);
    assertPoliciesByOwner(RepositoryContainer.REPOSITORY_CONTAINER_ID, RepositoryContainer.SINGLETON.getName(),
        OwnerType.REPOSITORY_CONTAINER, 0, applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(rootOrg.getId(), rootOrg.getName(), OwnerType.ORGANIZATION, 0,
        applicablePolicies.policiesByOwner.get(1));

    // Create a policy for the repository container
    Policy repoContainerPolicy = tempEntity.newPolicy(RepositoryContainer.SINGLETON);

    // Verify the applicable policies for the repository container
    response = restRequest(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID)
        .path("applicable")
        .get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertThat(applicablePolicies.policiesByOwner).hasSize(2);
    assertPoliciesByOwner(RepositoryContainer.REPOSITORY_CONTAINER_ID, RepositoryContainer.SINGLETON.getName(),
        OwnerType.REPOSITORY_CONTAINER, 1, applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(rootOrg.getId(), rootOrg.getName(), OwnerType.ORGANIZATION, 0,
        applicablePolicies.policiesByOwner.get(1));
    assertThat(applicablePolicies.policiesByOwner.get(0).policies.get(0).getId())
        .isEqualTo(repoContainerPolicy.getId());

    // Create a policy for the root org
    Policy rootOrgPolicy = tempEntity.newPolicy(rootOrg);

    // Verify the applicable policies for the repository container
    response = restRequest(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID)
        .path("applicable")
        .get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertThat(applicablePolicies.policiesByOwner).hasSize(2);
    assertPoliciesByOwner(RepositoryContainer.REPOSITORY_CONTAINER_ID, RepositoryContainer.SINGLETON.getName(),
        OwnerType.REPOSITORY_CONTAINER, 1, applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(rootOrg.getId(), rootOrg.getName(), OwnerType.ORGANIZATION, 1,
        applicablePolicies.policiesByOwner.get(1));
    assertThat(applicablePolicies.policiesByOwner.get(0).policies.get(0).getId())
        .isEqualTo(repoContainerPolicy.getId());
    assertThat(applicablePolicies.policiesByOwner.get(1).policies.get(0).getId()).isEqualTo(rootOrgPolicy.getId());
  }

  @Test
  public void testGetApplicablePolicies_RepositoryManager() throws Exception {
    // Create a repository manager
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);

    // Verify the applicable policies for the repository manager
    HttpResponse response = restRequest(OwnerType.REPOSITORY_MANAGER, repoManager.getId()).path("applicable").get();
    assertResponseStatus(200, response);
    ApplicablePolicies applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertThat(applicablePolicies.policiesByOwner).hasSize(3);
    assertPoliciesByOwner(repoManager.getId(), repoManager.getName(), OwnerType.REPOSITORY_MANAGER, 0,
        applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(RepositoryContainer.REPOSITORY_CONTAINER_ID, RepositoryContainer.SINGLETON.getName(),
        OwnerType.REPOSITORY_CONTAINER, 0, applicablePolicies.policiesByOwner.get(1));
    assertPoliciesByOwner(rootOrg.getId(), rootOrg.getName(), OwnerType.ORGANIZATION, 0,
        applicablePolicies.policiesByOwner.get(2));

    // Create a policy for the repository manager
    Policy repoManagerPolicy = tempEntity.newPolicy(repoManager);

    // Verify the applicable policies for the repository manager
    response = restRequest(OwnerType.REPOSITORY_MANAGER, repoManager.getId()).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertThat(applicablePolicies.policiesByOwner).hasSize(3);
    assertPoliciesByOwner(repoManager.getId(), repoManager.getName(), OwnerType.REPOSITORY_MANAGER, 1,
        applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(RepositoryContainer.REPOSITORY_CONTAINER_ID, RepositoryContainer.SINGLETON.getName(),
        OwnerType.REPOSITORY_CONTAINER, 0, applicablePolicies.policiesByOwner.get(1));
    assertPoliciesByOwner(rootOrg.getId(), rootOrg.getName(), OwnerType.ORGANIZATION, 0,
        applicablePolicies.policiesByOwner.get(2));
    assertThat(applicablePolicies.policiesByOwner.get(0).policies.get(0).getId()).isEqualTo(repoManagerPolicy.getId());

    // Create a policy for the repository container
    Policy repoContainerPolicy = tempEntity.newPolicy(RepositoryContainer.SINGLETON);

    // Verify the applicable policies for the repository manager
    response = restRequest(OwnerType.REPOSITORY_MANAGER, repoManager.getId()).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertThat(applicablePolicies.policiesByOwner).hasSize(3);
    assertPoliciesByOwner(repoManager.getId(), repoManager.getName(), OwnerType.REPOSITORY_MANAGER, 1,
        applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(RepositoryContainer.REPOSITORY_CONTAINER_ID, RepositoryContainer.SINGLETON.getName(),
        OwnerType.REPOSITORY_CONTAINER, 1, applicablePolicies.policiesByOwner.get(1));
    assertPoliciesByOwner(rootOrg.getId(), rootOrg.getName(), OwnerType.ORGANIZATION, 0,
        applicablePolicies.policiesByOwner.get(2));
    assertThat(applicablePolicies.policiesByOwner.get(0).policies.get(0).getId()).isEqualTo(repoManagerPolicy.getId());
    assertThat(applicablePolicies.policiesByOwner.get(1).policies.get(0).getId())
        .isEqualTo(repoContainerPolicy.getId());

    // Create a policy for the root org
    Policy rootOrgPolicy = tempEntity.newPolicy(rootOrg);

    // Verify the applicable policies for the repository manager
    response = restRequest(OwnerType.REPOSITORY_MANAGER, repoManager.getId()).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertThat(applicablePolicies.policiesByOwner).hasSize(3);
    assertPoliciesByOwner(repoManager.getId(), repoManager.getName(), OwnerType.REPOSITORY_MANAGER, 1,
        applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(RepositoryContainer.REPOSITORY_CONTAINER_ID, RepositoryContainer.SINGLETON.getName(),
        OwnerType.REPOSITORY_CONTAINER, 1, applicablePolicies.policiesByOwner.get(1));
    assertPoliciesByOwner(rootOrg.getId(), rootOrg.getName(), OwnerType.ORGANIZATION, 1,
        applicablePolicies.policiesByOwner.get(2));
    assertThat(applicablePolicies.policiesByOwner.get(0).policies.get(0).getId()).isEqualTo(repoManagerPolicy.getId());
    assertThat(applicablePolicies.policiesByOwner.get(1).policies.get(0).getId())
        .isEqualTo(repoContainerPolicy.getId());
    assertThat(applicablePolicies.policiesByOwner.get(2).policies.get(0).getId()).isEqualTo(rootOrgPolicy.getId());
  }

  @Test
  public void testGetApplicablePolicies_Repository() throws Exception {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newRepository(repoManager, "test");
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);

    // Verify the applicable policies for the repository
    HttpResponse response = restRequest(OwnerType.REPOSITORY, repo.getId()).path("applicable").get();
    assertResponseStatus(200, response);
    ApplicablePolicies applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertThat(applicablePolicies.policiesByOwner).hasSize(4);
    assertPoliciesByOwner(repo.getId(), repo.getName(), OwnerType.REPOSITORY, 0,
        applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(repoManager.getId(), repoManager.getName(), OwnerType.REPOSITORY_MANAGER, 0,
        applicablePolicies.policiesByOwner.get(1));
    assertPoliciesByOwner(RepositoryContainer.REPOSITORY_CONTAINER_ID, RepositoryContainer.SINGLETON.getName(),
        OwnerType.REPOSITORY_CONTAINER, 0, applicablePolicies.policiesByOwner.get(2));
    assertPoliciesByOwner(rootOrg.getId(), rootOrg.getName(), OwnerType.ORGANIZATION, 0,
        applicablePolicies.policiesByOwner.get(3));

    // Create a policy for the repository
    Policy repoPolicy = tempEntity.newPolicy(repo);

    // Verify the applicable policies for the repository
    response = restRequest(OwnerType.REPOSITORY, repo.getId()).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertThat(applicablePolicies.policiesByOwner).hasSize(4);
    assertPoliciesByOwner(repo.getId(), repo.getName(), OwnerType.REPOSITORY, 1,
        applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(repoManager.getId(), repoManager.getName(), OwnerType.REPOSITORY_MANAGER, 0,
        applicablePolicies.policiesByOwner.get(1));
    assertPoliciesByOwner(RepositoryContainer.REPOSITORY_CONTAINER_ID, RepositoryContainer.SINGLETON.getName(),
        OwnerType.REPOSITORY_CONTAINER, 0, applicablePolicies.policiesByOwner.get(2));
    assertPoliciesByOwner(rootOrg.getId(), rootOrg.getName(), OwnerType.ORGANIZATION, 0,
        applicablePolicies.policiesByOwner.get(3));
    assertThat(applicablePolicies.policiesByOwner.get(0).policies.get(0).getId()).isEqualTo(repoPolicy.getId());

    // Create a policy for the repository manager
    Policy repoManagerPolicy = tempEntity.newPolicy(repoManager);

    // Verify the applicable policies for the repository
    response = restRequest(OwnerType.REPOSITORY, repo.getId()).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertThat(applicablePolicies.policiesByOwner).hasSize(4);
    assertPoliciesByOwner(repo.getId(), repo.getName(), OwnerType.REPOSITORY, 1,
        applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(repoManager.getId(), repoManager.getName(), OwnerType.REPOSITORY_MANAGER, 1,
        applicablePolicies.policiesByOwner.get(1));
    assertPoliciesByOwner(RepositoryContainer.REPOSITORY_CONTAINER_ID, RepositoryContainer.SINGLETON.getName(),
        OwnerType.REPOSITORY_CONTAINER, 0, applicablePolicies.policiesByOwner.get(2));
    assertPoliciesByOwner(rootOrg.getId(), rootOrg.getName(), OwnerType.ORGANIZATION, 0,
        applicablePolicies.policiesByOwner.get(3));
    assertThat(applicablePolicies.policiesByOwner.get(0).policies.get(0).getId()).isEqualTo(repoPolicy.getId());
    assertThat(applicablePolicies.policiesByOwner.get(1).policies.get(0).getId()).isEqualTo(repoManagerPolicy.getId());

    // Create a policy for the repository container
    Policy repoContainerPolicy = tempEntity.newPolicy(RepositoryContainer.SINGLETON);

    // Verify the applicable policies for the repository
    response = restRequest(OwnerType.REPOSITORY, repo.getId()).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertThat(applicablePolicies.policiesByOwner).hasSize(4);
    assertPoliciesByOwner(repo.getId(), repo.getName(), OwnerType.REPOSITORY, 1,
        applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(repoManager.getId(), repoManager.getName(), OwnerType.REPOSITORY_MANAGER, 1,
        applicablePolicies.policiesByOwner.get(1));
    assertPoliciesByOwner(RepositoryContainer.REPOSITORY_CONTAINER_ID, RepositoryContainer.SINGLETON.getName(),
        OwnerType.REPOSITORY_CONTAINER, 1, applicablePolicies.policiesByOwner.get(2));
    assertPoliciesByOwner(rootOrg.getId(), rootOrg.getName(), OwnerType.ORGANIZATION, 0,
        applicablePolicies.policiesByOwner.get(3));
    assertThat(applicablePolicies.policiesByOwner.get(0).policies.get(0).getId()).isEqualTo(repoPolicy.getId());
    assertThat(applicablePolicies.policiesByOwner.get(1).policies.get(0).getId()).isEqualTo(repoManagerPolicy.getId());
    assertThat(applicablePolicies.policiesByOwner.get(2).policies.get(0).getId())
        .isEqualTo(repoContainerPolicy.getId());

    // Create a policy for the root org
    Policy rootOrgPolicy = tempEntity.newPolicy(rootOrg);

    // Verify the applicable policies for the repository
    response = restRequest(OwnerType.REPOSITORY, repo.getId()).path("applicable").get();
    assertResponseStatus(200, response);
    applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertThat(applicablePolicies.policiesByOwner).hasSize(4);
    assertPoliciesByOwner(repo.getId(), repo.getName(), OwnerType.REPOSITORY, 1,
        applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(repoManager.getId(), repoManager.getName(), OwnerType.REPOSITORY_MANAGER, 1,
        applicablePolicies.policiesByOwner.get(1));
    assertPoliciesByOwner(RepositoryContainer.REPOSITORY_CONTAINER_ID, RepositoryContainer.SINGLETON.getName(),
        OwnerType.REPOSITORY_CONTAINER, 1, applicablePolicies.policiesByOwner.get(2));
    assertPoliciesByOwner(rootOrg.getId(), rootOrg.getName(), OwnerType.ORGANIZATION, 1,
        applicablePolicies.policiesByOwner.get(3));
    assertThat(applicablePolicies.policiesByOwner.get(0).policies.get(0).getId()).isEqualTo(repoPolicy.getId());
    assertThat(applicablePolicies.policiesByOwner.get(1).policies.get(0).getId()).isEqualTo(repoManagerPolicy.getId());
    assertThat(applicablePolicies.policiesByOwner.get(2).policies.get(0).getId())
        .isEqualTo(repoContainerPolicy.getId());
    assertThat(applicablePolicies.policiesByOwner.get(3).policies.get(0).getId()).isEqualTo(rootOrgPolicy.getId());
  }

  @Test
  public void testGetApplicablePolicies_FilteredByTag() throws Exception {
    // Create an organization and an application
    Organization org = tempEntity.newOrganization();
    Organization parentOrg = organizationDAO.getById(org.getParentOrganizationId());
    Application app = tempEntity.newApplication(org.getId());

    Tag tag1 = tempEntity.newTag(org.getId());
    Tag tag2 = tempEntity.newTag(org.getId());

    // Create a tagged policy for the org that doesn't match an app tag. This policy should not appear in the result.
    Policy orgPolicy1 = tempEntity.newPolicy(org);
    tempEntity.newPolicyTag(orgPolicy1.getId(), tag1.getId());
    // Create another tagged policy for the org that matches an app tag. This policy should appear in the result.
    Policy orgPolicy2 = tempEntity.newPolicy(org);
    tempEntity.newPolicyTag(orgPolicy2.getId(), tag2.getId());

    // Create a tagged policy for the parent org that doesn't match an app tag. This policy should not appear in the
    // result.
    Policy parentOrgPolicy1 = tempEntity.newPolicy(parentOrg);
    tempEntity.newPolicyTag(parentOrgPolicy1.getId(), tag1.getId());
    // Create another tagged policy for the parentorg that matches an app tag. This policy should appear in the result.
    Policy parentOrgPolicy2 = tempEntity.newPolicy(parentOrg);
    tempEntity.newPolicyTag(parentOrgPolicy2.getId(), tag2.getId());

    tempEntity.newApplicationTag(app.getId(), tag2.getId());

    // Verify the applicable policies for the application
    HttpResponse response = restRequest(OwnerType.APPLICATION, app.getPublicId()).path("applicable").get();
    assertResponseStatus(200, response);
    ApplicablePolicies applicablePolicies = response.getBody(ApplicablePolicies.class);
    assertThat(applicablePolicies).isNotNull();
    assertThat(applicablePolicies.policiesByOwner).hasSize(3);
    assertPoliciesByOwner(app.getId(), app.getName(), OwnerType.APPLICATION, 0,
        applicablePolicies.policiesByOwner.get(0));
    assertPoliciesByOwner(org.getId(), org.getName(), OwnerType.ORGANIZATION, 1,
        applicablePolicies.policiesByOwner.get(1));
    assertPoliciesByOwner(parentOrg.getId(), parentOrg.getName(), OwnerType.ORGANIZATION, 1,
        applicablePolicies.policiesByOwner.get(2));
    assertThat(applicablePolicies.policiesByOwner.get(1).policies.get(0).getId()).isEqualTo(orgPolicy2.getId());
    assertThat(applicablePolicies.policiesByOwner.get(2).policies.get(0).getId()).isEqualTo(parentOrgPolicy2.getId());
  }

  @Test
  public void testImportPolicies_NonJsonPolicyFile() throws Exception {
    Organization org = tempEntity.newOrganization();
    HttpResponse response = restRequest(OwnerType.ORGANIZATION, org.getId()).path("import")
        .part("file", "garbage.png", "garbage")
        .post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("The file you selected failed to upload correctly, are you certain "
        + "it is a properly formatted policy import json file?");
  }

  @Test
  public void testImportPolicies_JsonFileIncorrectFormat() throws Exception {
    Organization org = tempEntity.newOrganization();
    HttpResponse response = restRequest(OwnerType.ORGANIZATION, org.getId()).path("import")
        .part("file", "badPolicy.json", "{\"badJson\":\"noClosingBraces\"")
        .post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("The file you selected failed to upload correctly, are you certain "
        + "it is a properly formatted policy import json file?");
  }

  @Test
  public void testDeletePolicy_OwnerIdMismatch() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication(org.getId());
    Application app2 = tempEntity.newApplication(org.getId());
    Policy policy = tempEntity.newPolicy(app1);

    HttpResponse response = restRequest(OwnerType.APPLICATION, app2.getPublicId()).path(policy.getId()).delete();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo("Cannot find a policy with ID " + policy.getId() + " for application ID " + app2.getPublicId());
    // Verify that the policy was not deleted
    assertThat(policyDAO.getById(policy.getId())).isNotNull();
  }

  @Test
  public void testExportImport() throws Exception {
    // Export
    Organization fromOrg = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(fromOrg);

    HttpResponse response = restRequest(OwnerType.ORGANIZATION, fromOrg.getId()).path("export").get();
    assertResponseStatus(200, response);
    PolicyExportResult policyExportResult = response.getBody(PolicyExportResult.class);
    assertThat(policyExportResult).isNotNull();
    assertThat(policyExportResult.policies).hasSize(1);
    assertThat(policyExportResult.policies.get(0).getName()).isEqualTo(policy.getName());

    organizationDAO.delete(fromOrg);

    // Import
    Organization toOrg = tempEntity.newOrganization();
    response = restRequest(OwnerType.ORGANIZATION, toOrg.getId()).path("import")
        .part("file", "policyExportResult.json", policyExportResult)
        .post();
    assertResponseStatus(200, response);
    PolicyImportResult policyImportResult = response.getBody(PolicyImportResult.class);
    assertThat(policyImportResult).isNotNull();
    assertThat(policyImportResult.ownerName).isEqualTo(toOrg.getName());

    List<Policy> policies = policyDAO.getByOwnerId(toOrg.getId());
    assertThat(policies).hasSize(1);
    assertThat(policies.get(0).getName()).isEqualTo(policy.getName());
  }

  @Test
  public void testImportPolicies_AppImportNotSupported() throws Exception {
    HttpResponse response = restRequest(OwnerType.APPLICATION, "foo").path("import")
        .part("file", "file", createImportBody())
        .post();

    // policy import to applications is no longer supported
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Importing policies into an application is no longer supported.");
  }

  @Test
  public void testImportPolicies_NoPolicies() throws Exception {
    PolicyExportResult policyExportResult = new PolicyExportResult();

    HttpResponse response = restRequest(OwnerType.ORGANIZATION, tempEntity.newOrganization().getId()).path("import")
        .part("file", "file", policyExportResult)
        .post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .isEqualTo("The file you selected failed to upload correctly, the policy file needs to have at least one "
            + "policy defined.");
  }

  @Test
  public void testImportPolicies_AppImportNoPolicies() throws Exception {
    PolicyExportResult emptyPolicyExport = new PolicyExportResult();

    HttpResponse response = restRequest(OwnerType.APPLICATION, tempEntity.newApplicationWithParent().getId())
        .path("import")
        .part("file", "file", emptyPolicyExport)
        .post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Importing policies into an application is no longer supported.");
  }

  @Test
  public void testCreatePolicy_PolicyNotificationsOverrideAllowed() throws Exception {
    testCreatePolicy_PolicyNotificationsOverrideAllowed(
        organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID));
    testCreatePolicy_PolicyNotificationsOverrideAllowed(tempEntity.newOrganization());
    testCreatePolicy_PolicyNotificationsOverrideAllowed(tempEntity.newApplicationWithParent());
  }

  private void testCreatePolicy_PolicyNotificationsOverrideAllowed(Owner owner) throws Exception {
    Policy policy = createPolicy();
    policy.setPolicyNotificationsOverrideAllowed(true);

    HttpResponse response;
    List<Policy> result;

    // Create
    response = restRequest(owner.getType(), owner.getPublicId()).body(policy).post();
    assertResponseStatus(200, response);
    result = policyDAO.getByName(policy.getName());
    assertThat(result).hasSize(1);
    policy = result.get(0);
    assertThat(policy.isPolicyNotificationsOverrideAllowed()).isTrue();

    // Update
    policy.setPolicyNotificationsOverrideAllowed(false);
    response = restRequest(owner.getType(), owner.getPublicId()).body(policy).put();
    assertResponseStatus(200, response);
    result = policyDAO.getByName(policy.getName());
    assertThat(result).hasSize(1);
    assertThat(result.get(0).isPolicyNotificationsOverrideAllowed()).isFalse();
  }

  private Policy createPolicy() {
    Policy policy = new Policy();
    policy.setName(UUID.randomUUID().toString().replace("-", ""));
    Constraint constraint = new Constraint();
    constraint.setName("PolicyResourceTest new constraint");
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    return policy;
  }

  private void createTestPolicyWithCondition(
      String name,
      final String ownerId,
      boolean withSecurityVulnerabilityCategoryMaliciousCodeCondition,
      boolean withProprietaryNameConflictCondition,
      boolean withSecurityVulnerabilitySeverityCondition)
  {
    Policy policy = new Policy(name, name);
    policy.setOwnerId(ownerId);
    Constraint constraint = new Constraint("test-constraint", "Test Constraint", LogicalOperator.OR);

    if (withSecurityVulnerabilitySeverityCondition) {
      constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    }
    if (withSecurityVulnerabilityCategoryMaliciousCodeCondition) {
      constraint.addCondition(new Condition(SecurityVulnerabilityCategoryConditionType.ID, "is", "malicious_code"));
    }
    if (withProprietaryNameConflictCondition) {
      constraint.addCondition(new Condition(ProprietaryNameConflictConditionType.ID, "is present"));
    }
    policy.addConstraint(constraint);
    policyDAO.insert(policy);
  }
}
