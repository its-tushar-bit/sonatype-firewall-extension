/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.policy.PolicyResource.ApplicablePolicies;
import com.sonatype.insight.brain.policy.PolicyResource.PoliciesByOwner;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyResourceTest
    extends AbstractResourceTest
{
  private static final PolicyDAO policyDAO = new PolicyDAO();

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
        .part("file", "file", createImportBody()).post();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Organization with ID " + orgId + " does not exist.");
  }

  @Test
  public void testCRUD_ApplicationLevel() throws Exception {
    String applicationPublicId = "PolicyResourceTest_testCRUD";
    tempEntity.newApplicationWithParent(applicationPublicId);

    testCRUD(OwnerType.APPLICATION, applicationPublicId);
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
  public void testUpdatePolicy_DifferentOwnerId() throws Exception {
    Organization ownerOrg = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(ownerOrg);

    Organization otherOrg = tempEntity.newOrganization();
    policy.setOwnerId(otherOrg.getId());

    HttpResponse response = restRequest(OwnerType.ORGANIZATION, otherOrg.getId()).body(policy).put();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo("Cannot find a policy with id " + policy.getId() + " for owner id " + otherOrg.getId());
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

    // Get all policies
    response = restRequest(ownerType, ownerId).get();
    assertResponseStatus(200, response);
    policies = response.getBody(Policy[].class);
    assertThat(policies).isEmpty();
  }

  @Test
  public void testAddPolicy_InvalidPolicy_AppLevel() throws Exception {
    String applicationPublicId = "PolicyResourceTest_testCreateInvalidPolicy";
    tempEntity.newApplicationWithParent(applicationPublicId);
    testAddPolicy_InvalidPolicy(OwnerType.APPLICATION, applicationPublicId);
  }

  @Test
  public void testAddPolicy_InvalidPolicy_OrgLevel() throws Exception {
    String orgId = tempEntity.newOrganization("test").getId();
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
  }

  @Test
  public void testUpdatePolicy_InvalidPolicy_AppLevel() throws Exception {
    String applicationPublicId = "PolicyResourceTest_testUpdateInvalidPolicy";
    Application app = tempEntity.newApplicationWithParent(applicationPublicId);
    testUpdatePolicy_InvalidPolicy(OwnerType.APPLICATION, app.getId(), app.getPublicId());
  }

  @Test
  public void testUpdatePolicy_InvalidPolicy_OrgLevel() throws Exception {
    String orgId = tempEntity.newOrganization("test").getId();
    testUpdatePolicy_InvalidPolicy(OwnerType.ORGANIZATION, orgId, orgId);
  }

  @Test
  public void testUpdateOverrides_Add_ForActions() throws Exception {
    Policy policy = tempEntity.newPolicy();
    policy.setPolicyActionsOverrideAllowed(true);
    policyDAO.update(policy);

    String applicationPublicId = "PolicyResourceTest_testUpdateOverrides_Add_ForActions";
    Application app = tempEntity.newApplicationWithParent(applicationPublicId);

    Map<String, String> actionsOverride = new LinkedHashMap<>();
    actionsOverride.put("stage-release", "fail");
    actionsOverride.put("release", "fail");
    actionsOverride.put("build", "warn");

    HttpResponse response = restRequest(OwnerType.APPLICATION, applicationPublicId)
        .path(policy.getId(), "overrides")
        .body(new PolicyOverridesDTO(actionsOverride))
        .put();

    assertResponseStatus(200, response);
    final Policy updatedPolicy = response.getBody(Policy.class);
    Map<String, String> savedActionsOverride = updatedPolicy.getPolicyActionsOverrides().get(app.getId());
    assertThat(savedActionsOverride).isNotNull();
    assertThat(savedActionsOverride.size()).isEqualTo(3);
    assertThat(savedActionsOverride.get("stage-release")).isEqualTo("fail");
    assertThat(savedActionsOverride.get("release")).isEqualTo("fail");
    assertThat(savedActionsOverride.get("build")).isEqualTo("warn");
  }

  @Test
  public void testUpdateOverrides_Add_ForNotifications() throws Exception {
    Policy policy = tempEntity.newPolicy();
    policy.setPolicyNotificationsOverrideAllowed(true);
    policyDAO.update(policy);
    String applicationPublicId = "PolicyResourceTest_testUpdateOverrides_Add_ForNotifications";
    Application app = tempEntity.newApplicationWithParent(applicationPublicId);
    Notifications notificationsOverride = new Notifications();
    notificationsOverride.add(new UserNotification("app@domain.com", BuildStageType.ID));

    HttpResponse response = restRequest(OwnerType.APPLICATION, applicationPublicId)
        .path(policy.getId(), "overrides")
        .body(new PolicyOverridesDTO(notificationsOverride))
        .put();

    assertResponseStatus(200, response);
    Policy updatedPolicy = response.getBody(Policy.class);
    Notifications savedNotificationsOverride = updatedPolicy.getPolicyNotificationsOverrides().get(app.getId());
    assertThat(notificationsOverride).usingRecursiveComparison().isEqualTo(savedNotificationsOverride);
  }

  @Test
  public void testUpdateOverrides_Add_ForActions_policyActionsOverrideIsDisabled() throws Exception {
    Policy policy = tempEntity.newPolicy();

    String applicationPublicId =
        "PolicyResourceTest_testUpdateOverrides_Add_ForActions_policyActionsOverrideIsDisabled";
    tempEntity.newApplicationWithParent(applicationPublicId);

    Map<String, String> actionsOverride = new LinkedHashMap<>();
    actionsOverride.put("stage-release", "fail");
    actionsOverride.put("release", "fail");
    actionsOverride.put("build", "warn");

    HttpResponse response = restRequest(OwnerType.APPLICATION, applicationPublicId)
        .path(policy.getId(), "overrides")
        .body(new PolicyOverridesDTO(actionsOverride))
        .put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .isEqualTo("Actions override is not allowed for policy with id " + policy.getId());
  }

  @Test
  public void testUpdateOverrides_Add_ForNotifications_policyNotificationsOverrideIsDisabled() throws Exception {
    Policy policy = tempEntity.newPolicy();
    String applicationPublicId =
        "PolicyResourceTest_testUpdateOverrides_Add_ForNotifications_policyNotificationsOverrideIsDisabled";
    tempEntity.newApplicationWithParent(applicationPublicId);
    Notifications notificationsOverride = new Notifications();
    notificationsOverride.add(new UserNotification("app@domain.com", BuildStageType.ID));

    HttpResponse response = restRequest(OwnerType.APPLICATION, applicationPublicId)
        .path(policy.getId(), "overrides")
        .body(new PolicyOverridesDTO(notificationsOverride))
        .put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(
        "Notifications override is not allowed for policy with id " + policy.getId());
  }

  @Test
  public void testUpdateOverrides_Add_invalidPolicyId() throws Exception {
    String applicationPublicId = "PolicyResourceTest_testUpdateOverrides_Add_invalidPolicyId";
    tempEntity.newApplicationWithParent(applicationPublicId);

    HttpResponse response = restRequest(OwnerType.APPLICATION, applicationPublicId)
        .path("123", "overrides")
        .body(new PolicyOverridesDTO(new LinkedHashMap<>()))
        .put();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("PolicyInternal with ID 123 does not exist.");
  }

  @Test
  public void testUpdateOverrides_nullJson() throws Exception {
    String applicationPublicId = "PolicyResourceTest_testUpdateOverrides_Add_noOverrides";
    Application application = tempEntity.newApplicationWithParent(applicationPublicId);
    Policy policy = tempEntity.newPolicy(application.getParentOwnerId());

    HttpResponse response = restRequest(OwnerType.APPLICATION, applicationPublicId)
        .path(policy.getId(), "overrides")
        .put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("A policy overrides configuration must be specified.");
  }

  @Test
  public void testUpdateOverrides_noOverrides() throws Exception {
    String applicationPublicId = "PolicyResourceTest_testUpdateOverrides_Add_noOverrides";
    Application application = tempEntity.newApplicationWithParent(applicationPublicId);
    Policy policy = tempEntity.newPolicy(application.getParentOwnerId());

    HttpResponse response = restRequest(OwnerType.APPLICATION, applicationPublicId)
        .path(policy.getId(), "overrides")
        .body(new ObjectMapper().createObjectNode())
        .put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("A policy overrides configuration must be specified.");
  }

  @Test
  public void testUpdateOverrides_badJson() throws Exception {
    String applicationPublicId = "PolicyResourceTest_testUpdateOverrides_Add_noOverrides";
    Application application = tempEntity.newApplicationWithParent(applicationPublicId);
    Policy policy = tempEntity.newPolicy(application.getParentOwnerId());
    ObjectNode objectNode = new ObjectMapper().createObjectNode();
    objectNode.put("actions", 1);

    HttpResponse response = restRequest(OwnerType.APPLICATION, applicationPublicId)
        .path(policy.getId(), "overrides")
        .body(objectNode)
        .put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(
        "The given JSON cannot be deserialized into a policy overrides configuration.");
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
  public void testUpdateOverrides_Delete_ForActions() throws Exception {
    Policy policy = tempEntity.newPolicy();
    String applicationPublicId = "PolicyResourceTest_testUpdateOverrides_Delete_ForActions";
    Organization testOrg = tempEntity.newOrganization("TestOrganization");
    Application testApp = tempEntity.newApplication(applicationPublicId, testOrg.getId());

    Map<String, String> applicationActionsOverrides = new LinkedHashMap<>();
    applicationActionsOverrides.put("stage-release", "fail");
    applicationActionsOverrides.put("release", "fail");
    applicationActionsOverrides.put("build", "warn");

    Map<String, String> organizationActionsOverrides = new LinkedHashMap<>();
    organizationActionsOverrides.put("stage-release", "warn");
    organizationActionsOverrides.put("release", "warn");

    policy.addPolicyActionsOverride(testApp.getId(), applicationActionsOverrides);
    policy.addPolicyActionsOverride(testOrg.getId(), organizationActionsOverrides);
    policyDAO.update(policy);

    HttpResponse response =
        restRequest(OwnerType.APPLICATION, applicationPublicId)
            .path(policy.getId(), "overrides")
            .body(createDeleteBody(true, false))
            .put();

    assertResponseStatus(200, response);
    final Policy updatedPolicy = response.getBody(Policy.class);
    assertThat(updatedPolicy.getPolicyActionsOverrides())
        .isNotNull()
        .hasSize(1)
        .containsEntry(testOrg.getId(), organizationActionsOverrides);

    Policy policyOnDB = new PolicyDAO().getById(policy.getId());
    assertThat(policyOnDB.getPolicyActionsOverrides())
        .isNotNull()
        .hasSize(1)
        .containsEntry(testOrg.getId(), organizationActionsOverrides);
  }

  @Test
  public void testUpdateOverrides_Delete_ForNotifications() throws Exception {
    Policy policy = tempEntity.newPolicy();
    String applicationPublicId = "PolicyResourceTest_testUpdateOverrides_Delete_ForNotifications";
    Organization testOrg = tempEntity.newOrganization("TestOrganization");
    Application testApp = tempEntity.newApplication(applicationPublicId, testOrg.getId());

    Notifications applicationNotificationsOverride = new Notifications();
    applicationNotificationsOverride.add(new UserNotification("app@domain.com", BuildStageType.ID));

    Notifications organizationNotificationsOverride = new Notifications();
    organizationNotificationsOverride.add(new UserNotification("org@domain.com", ReleaseStageType.ID));

    policy.addPolicyNotificationsOverride(testApp.getId(), applicationNotificationsOverride);
    policy.addPolicyNotificationsOverride(testOrg.getId(), organizationNotificationsOverride);
    policyDAO.update(policy);

    HttpResponse response =
        restRequest(OwnerType.APPLICATION, applicationPublicId)
            .path(policy.getId(), "overrides")
            .body(createDeleteBody(false, true))
            .put();

    assertResponseStatus(200, response);
    Policy updatedPolicy = response.getBody(Policy.class);
    assertThat(updatedPolicy.getPolicyNotificationsOverrides())
        .isNotNull()
        .hasSize(1)
        .containsEntry(testOrg.getId(), organizationNotificationsOverride);

    Policy policyOnDB = new PolicyDAO().getById(policy.getId());
    assertThat(policyOnDB.getPolicyNotificationsOverrides())
        .isNotNull()
        .hasSize(1)
        .containsEntry(testOrg.getId(), organizationNotificationsOverride);
  }

  @Test
  public void testUpdateOverrides_Delete_ForActions_OverrideDoesNotExist() throws Exception {
    Policy policy = tempEntity.newPolicy();
    String applicationPublicId = "PolicyResourceTest_testUpdateOverrides_Delete_ForActions_OverrideDoesNotExist";
    Organization testOrg = tempEntity.newOrganization("TestOrganization");
    tempEntity.newApplication(applicationPublicId, testOrg.getId());

    Map<String, String> organizationActionsOverrides = new LinkedHashMap<>();
    organizationActionsOverrides.put("stage-release", "warn");
    organizationActionsOverrides.put("release", "warn");

    policy.addPolicyActionsOverride(testOrg.getId(), organizationActionsOverrides);
    policyDAO.update(policy);

    HttpResponse response =
        restRequest(OwnerType.APPLICATION, applicationPublicId)
            .path(policy.getId(), "overrides")
            .body(createDeleteBody(true, false))
            .put();

    assertResponseStatus(200, response);
    final Policy updatedPolicy = response.getBody(Policy.class);
    assertThat(updatedPolicy.getPolicyActionsOverrides())
        .isNotNull()
        .hasSize(1)
        .containsEntry(testOrg.getId(), organizationActionsOverrides);

    Policy policyOnDB = new PolicyDAO().getById(policy.getId());
    assertThat(policyOnDB.getPolicyActionsOverrides())
        .isNotNull()
        .hasSize(1)
        .containsEntry(testOrg.getId(), organizationActionsOverrides);
  }

  @Test
  public void testUpdateOverrides_Delete_ForNotifications_OverrideDoesNotExist() throws Exception {
    Policy policy = tempEntity.newPolicy();
    String applicationPublicId = "PolicyResourceTest_testUpdateOverrides_Delete_ForNotifications_OverrideDoesNotExist";
    Organization testOrg = tempEntity.newOrganization("TestOrganization");
    tempEntity.newApplication(applicationPublicId, testOrg.getId());

    Notifications organizationNotificationsOverride = new Notifications();
    organizationNotificationsOverride.add(new UserNotification("org@domain.com", ReleaseStageType.ID));

    policy.addPolicyNotificationsOverride(testOrg.getId(), organizationNotificationsOverride);
    policyDAO.update(policy);

    HttpResponse response =
        restRequest(OwnerType.APPLICATION, applicationPublicId)
            .path(policy.getId(), "overrides")
            .body(createDeleteBody(false, true))
            .put();

    assertResponseStatus(200, response);
    Policy updatedPolicy = response.getBody(Policy.class);
    assertThat(updatedPolicy.getPolicyNotificationsOverrides())
        .isNotNull()
        .hasSize(1)
        .containsEntry(testOrg.getId(), organizationNotificationsOverride);

    Policy policyOnDB = new PolicyDAO().getById(policy.getId());
    assertThat(policyOnDB.getPolicyNotificationsOverrides())
        .isNotNull()
        .hasSize(1)
        .containsEntry(testOrg.getId(), organizationNotificationsOverride);
  }

  private void testUpdatePolicy_InvalidPolicy(OwnerType ownerType, String ownerId, String publicOwnerid)
      throws Exception
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
  }

  private void assertPoliciesByOwner(String ownerId,
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
  public void testGetApplicablePolicies_OrgsAndApps() throws Exception {
    // Create an organization and an application
    String orgName = "testGetApplicablePoliciesOrg";
    Organization org = tempEntity.newOrganization(orgName);
    String orgId = org.getId();
    Organization parentOrg = new OrganizationDAO().getById(org.getParentOrganizationId());
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
    Organization rootOrg = new OrganizationDAO().getById(Organization.ROOT_ORGANIZATION_ID);

    // Verify the applicable policies for the repository container
    HttpResponse response = restRequest(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID)
        .path("applicable").get();
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
        .path("applicable").get();
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
        .path("applicable").get();
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
    Organization rootOrg = new OrganizationDAO().getById(Organization.ROOT_ORGANIZATION_ID);

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
  public void testGetApplicablePolicies_FilteredByTag() throws Exception {
    // Create an organization and an application
    Organization org = tempEntity.newOrganization();
    Organization parentOrg = new OrganizationDAO().getById(org.getParentOrganizationId());
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
        .part("file", "garbage.png", "garbage").post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("The file you selected failed to upload correctly, are you certain "
        + "it is a properly formatted policy import json file?");
  }

  @Test
  public void testImportPolicies_JsonFileIncorrectFormat() throws Exception {
    Organization org = tempEntity.newOrganization();
    HttpResponse response = restRequest(OwnerType.ORGANIZATION, org.getId()).path("import")
        .part("file", "badPolicy.json", "{\"badJson\":\"noClosingBraces\"").post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("The file you selected failed to upload correctly, are you certain "
        + "it is a properly formatted policy import json file?");
  }

  @Test
  public void testDeletePolicy_OwnerIdMismatch() throws Exception {
    Organization org = tempEntity.newOrganization("testDeletePolicyOwnerIdMismatch");
    String appPublicId1 = "PolicyResourceTest_AppId1";
    Application app1 = tempEntity.newApplication(appPublicId1, org.getId());
    String appPublicId2 = "PolicyResourceTest_AppId2";
    tempEntity.newApplication(appPublicId2, org.getId());
    Policy policy = tempEntity.newPolicy(app1);

    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId2).path(policy.getId()).delete();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo("Cannot find a policy with ID " + policy.getId() + " for application ID " + appPublicId2);
    // Verify that the policy was not deleted
    assertThat(new PolicyDAO().getById(policy.getId())).isNotNull();
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

    new OrganizationDAO().delete(fromOrg);

    // Import
    Organization toOrg = tempEntity.newOrganization();
    response = restRequest(OwnerType.ORGANIZATION, toOrg.getId()).path("import")
        .part("file", "policyExportResult.json", policyExportResult).post();
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
        .part("file", "file", createImportBody()).post();

    // policy import to applications is no longer supported
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Importing policies into an application is no longer supported.");
  }

  @Test
  public void testImportPolicies_NoPolicies() throws Exception {
    PolicyExportResult policyExportResult = new PolicyExportResult();

    HttpResponse response = restRequest(OwnerType.ORGANIZATION, tempEntity.newOrganization().getId()).path("import")
        .part("file", "file", policyExportResult).post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .isEqualTo("The file you selected failed to upload correctly, the policy file needs to have at least one "
            + "policy defined.");
  }

  @Test
  public void testImportPolicies_AppImportNoPolicies() throws Exception {
    PolicyExportResult emptyPolicyExport = new PolicyExportResult();

    HttpResponse response = restRequest(OwnerType.APPLICATION, tempEntity.newApplicationWithParent().getId())
        .path("import").part("file", "file", emptyPolicyExport).post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Importing policies into an application is no longer supported.");
  }

  @Test
  public void testCreatePolicy_PolicyNotificationsOverrideAllowed() throws Exception {
    testCreatePolicy_PolicyNotificationsOverrideAllowed(
        new OrganizationDAO().getById(Organization.ROOT_ORGANIZATION_ID));
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
}
