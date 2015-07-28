/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dto.ApplicableContext;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.policy.PolicyWaiverResource.AppliedWaivers;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.utils.IdUtils;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class PolicyWaiverResourceTest
    extends AbstractResourceTest
{
  private HttpRequest restRequest(String ownerType, String ownerId) {
    return restRequest().path(PolicyWaiverResource.SERVICE_PATH).parameter(ownerType, ownerId);
  }

  @Test
  public void testCRUD_Application() throws Exception {
    String appPublicId = "PolicyWaiverResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(appPublicId);

    testCRUD(IdUtils.TYPE_APPLICATION, appPublicId, application.getId());
  }

  @Test
  public void testCRUD_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization("PolicyWaiverResourceTest");

    testCRUD(IdUtils.TYPE_ORGANIZATION, organization.getId(), organization.getId());
  }

  private void testCRUD(String ownerType, String ownerPublicId, String ownerId) throws Exception {
    Policy policy = tempEntity.newPolicy(ownerId, "PolicyWaiverResourceTest");
    String policyId = policy.getId();

    // Create
    PolicyWaiver policyWaiver = new PolicyWaiver("12345678901234567890", policyId, null /* ownerId */, "My comment");
    HttpResponse response = restRequest(ownerType, ownerPublicId).body(policyWaiver).post();
    assertResponseStatus(200, response);
    policyWaiver = response.getBody(PolicyWaiver.class);
    assertPolicyWaiver(policyId, ownerId, "My comment", policyWaiver);

    // Get
    response = restRequest(ownerType, ownerPublicId).path("component", policyWaiver.getHash()).get();
    assertResponseStatus(200, response);
    AppliedWaivers policyWaivers = response.getBody(AppliedWaivers.class);
    assertNotNull(policyWaivers);
    assertNotNull(policyWaivers.waiversByOwner);
    assertEquals(1, policyWaivers.waiversByOwner.size());
    assertEquals(1, policyWaivers.waiversByOwner.get(0).waivers.size());
    assertPolicyWaiver(policyId, ownerPublicId, "My comment", policyWaivers.waiversByOwner.get(0).waivers.get(0));

    // Delete
    response = restRequest(ownerType, ownerPublicId).path(policyWaiver.getId()).delete();
    assertResponseStatus(204, response);

    // Get
    response = restRequest(ownerType, ownerPublicId).path("component", policyWaiver.getHash()).get();
    assertResponseStatus(200, response);
    policyWaivers = response.getBody(AppliedWaivers.class);
    assertNotNull(policyWaivers);
    assertNotNull(policyWaivers.waiversByOwner);
    assertEquals(0, policyWaivers.waiversByOwner.size());
  }

  @Test
  public void testDelete_OwnerIdMismatch_Application() throws Exception {
    String appPublicId1 = "PolicyWaiverResourceTest_AppId1";
    Application application1 = tempEntity.newApplicationWithParent(appPublicId1);
    String appPublicId2 = "PolicyWaiverResourceTest_AppId2";
    tempEntity.newApplicationWithParent(appPublicId2);

    testDelete_OwnerIdMismatch(IdUtils.TYPE_APPLICATION, appPublicId1, application1.getId(), appPublicId2);
  }

  @Test
  public void testDelete_OwnerIdMismatch_Organization() throws Exception {
    Organization organization1 = tempEntity.newOrganization("PolicyWaiverResourceTest1");
    Organization organization2 = tempEntity.newOrganization("PolicyWaiverResourceTest2");

    testDelete_OwnerIdMismatch(IdUtils.TYPE_ORGANIZATION, organization1.getId(), organization1.getId(),
        organization2.getId());
  }

  @Test
  public void testGetPolicyWaiversByHash() throws Exception {
    Organization organization = tempEntity.newOrganization("PolicyWaiverResourceTest1");
    String appPublicId = "PolicyWaiverResourceTest_AppId1";
    Application application = tempEntity.newApplication("PolicyWaiverResourceTest AppId1", appPublicId, organization.getId());
    Policy policy = createPolicy(IdUtils.TYPE_ORGANIZATION, organization.getId());
    String hash = "12345678901234567890";

    tempEntity.newWaiver(hash, policy.getId(), application.getId(), "My comment");

    HttpResponse response = restRequest(IdUtils.TYPE_APPLICATION, application.getPublicId()).path("component", hash).get();
    assertResponseStatus(200, response);
    AppliedWaivers waivers = response.getBody(AppliedWaivers.class);
    assertNotNull(waivers);
    assertNotNull(waivers.waiversByOwner);
    assertEquals(1, waivers.waiversByOwner.size());
    assertEquals(appPublicId, waivers.waiversByOwner.get(0).ownerId);
    assertEquals(application.getName(), waivers.waiversByOwner.get(0).ownerName);
    assertEquals(IdUtils.TYPE_APPLICATION, waivers.waiversByOwner.get(0).ownerType);
    assertEquals(1, waivers.waiversByOwner.get(0).waivers.size());
    assertPolicyWaiver(policy.getId(), application.getPublicId(), "My comment",
        waivers.waiversByOwner.get(0).waivers.get(0));

    tempEntity.newWaiver("12345678901234567890", policy.getId(), organization.getId(), "My comment");

    response = restRequest(IdUtils.TYPE_APPLICATION, application.getPublicId()).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertNotNull(waivers);
    assertNotNull(waivers.waiversByOwner);
    assertEquals(2, waivers.waiversByOwner.size());
    assertEquals(appPublicId, waivers.waiversByOwner.get(0).ownerId);
    assertEquals(application.getName(), waivers.waiversByOwner.get(0).ownerName);
    assertEquals(IdUtils.TYPE_APPLICATION, waivers.waiversByOwner.get(0).ownerType);
    assertEquals(1, waivers.waiversByOwner.get(0).waivers.size());
    assertEquals(organization.getId(), waivers.waiversByOwner.get(1).ownerId);
    assertEquals(organization.getName(), waivers.waiversByOwner.get(1).ownerName);
    assertEquals(IdUtils.TYPE_ORGANIZATION, waivers.waiversByOwner.get(1).ownerType);
    assertEquals(1, waivers.waiversByOwner.get(1).waivers.size());
    assertPolicyWaiver(policy.getId(), application.getPublicId(), "My comment",
        waivers.waiversByOwner.get(0).waivers.get(0));
    assertPolicyWaiver(policy.getId(), organization.getId(), "My comment", waivers.waiversByOwner.get(1).waivers.get(0));

    response = restRequest(IdUtils.TYPE_ORGANIZATION, organization.getId()).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertNotNull(waivers);
    assertNotNull(waivers.waiversByOwner);
    assertEquals(1, waivers.waiversByOwner.size());
    assertPolicyWaiver(policy.getId(), organization.getId(), "My comment", waivers.waiversByOwner.get(0).waivers.get(0));
  }

  private void testDelete_OwnerIdMismatch(String ownerType, String ownerPublicId1, String ownerId1,
      String ownerPublicId2) throws Exception
  {
    Policy policy = tempEntity.newPolicy(ownerId1, "PolicyWaiverResourceTest");
    String policyId = policy.getId();
    PolicyWaiver policyWaiver = new PolicyWaiver("12345678901234567890", policyId, null /* ownerId */, "My comment");
    HttpResponse response = restRequest(ownerType, ownerPublicId1).body(policyWaiver).post();
    assertResponseStatus(200, response);
    policyWaiver = response.getBody(PolicyWaiver.class);

    response = restRequest(ownerType, ownerPublicId2).path(policyWaiver.getId()).delete();
    assertResponseStatus(404, response);
    Assert.assertEquals("Cannot find a policy waiver with ID " + policyWaiver.getId() + " for " + ownerType + " ID "
        + ownerPublicId2, response.getBodyText());
    // Verify that the policy waiver was not deleted
    PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();
    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getByOwnerId(ownerId1);
    assertEquals(1, policyWaivers.size());
    assertPolicyWaiver(policyId, ownerId1, "My comment", policyWaivers.get(0));
  }

  @Test
  public void testDelete_Nonexistent_Application() throws Exception {
    String appPublicId = "PolicyWaiverResourceTest_AppId";
    tempEntity.newApplicationWithParent(appPublicId);

    HttpResponse response = restRequest(IdUtils.TYPE_APPLICATION, appPublicId).path("YettiId").delete();
    assertResponseStatus(404, response);
    Assert.assertEquals("Cannot find a policy waiver with ID YettiId.", response.getBodyText());
  }

  @Test
  public void testDelete_Nonexistent_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization("PolicyWaiverResourceTest");

    HttpResponse response = restRequest(IdUtils.TYPE_ORGANIZATION, organization.getId()).path("YettiId").delete();
    assertResponseStatus(404, response);
    Assert.assertEquals("Cannot find a policy waiver with ID YettiId.", response.getBodyText());
  }

  private void assertPolicyWaiver(String policyId, String ownerId, String comment, PolicyWaiver actual) {
    assertEquals(policyId, actual.getPolicyId());
    assertEquals(ownerId, actual.getOwnerId());
    assertEquals(comment, actual.getComment());
  }

  @Test
  public void testGetApplicableContexts_Application() throws Exception {
    String appPublicId = "testGetApplicableContexts_Application";
    Application application = tempEntity.newApplicationWithParent(appPublicId);

    // Create a policy for the application
    Policy policy = createPolicy(IdUtils.TYPE_APPLICATION, appPublicId);

    HttpResponse response = restRequest("application", appPublicId).path("applicable/context", policy.getId()).get();
    assertResponseStatus(200, response);
    ApplicableContext result = response.getBody(ApplicableContext.class);
    assertApplicableContext(appPublicId, application.getName(), OwnerType.APPLICATION, result);
  }

  @Test
  public void testGetApplicableContexts_Organization() throws Exception {
    String appPublicId = "testGetApplicableContexts_Organization";
    Application application = tempEntity.newApplicationWithParent(appPublicId);
    Organization organization = new OrganizationDAO().getByIdNotNull(application.getOrganizationId());

    // Create a policy for the organization
    Policy policy = createPolicy(IdUtils.TYPE_ORGANIZATION, application.getOrganizationId());

    HttpResponse response = restRequest("application", appPublicId).path("applicable/context", policy.getId()).get();
    assertResponseStatus(200, response);
    ApplicableContext result = response.getBody(ApplicableContext.class);
    assertApplicableContext(organization.getId(), organization.getName(), OwnerType.ORGANIZATION, result);
    assertNotNull(result.getChildren());
    assertEquals(1, result.getChildren().size());
    ApplicableContext childContext = result.getChildren().get(0);
    assertApplicableContext(appPublicId, application.getName(), OwnerType.APPLICATION, childContext);
    assertNull(childContext.getChildren());
  }

  private void assertApplicableContext(String id, String name, OwnerType type, ApplicableContext actual) {
    assertNotNull(actual);
    assertEquals(id, actual.getId());
    assertEquals(name, actual.getName());
    assertEquals(type, actual.getType());
  }

  private Policy createPolicy(String ownerType, String ownerId) throws Exception {
    Constraint constraint = new Constraint(null, "Constraint name 1", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "present"));
    Policy policy = new Policy(null, "Policy Name 1");
    policy.addConstraint(constraint);
    policy.addAction(BuildStageType.ID, new Action(FailActionType.ID));
    HttpResponse response = restRequest().path(PolicyResource.SERVICE_PATH).parameter(ownerType, ownerId).body(policy)
        .post();
    assertResponseStatus(200, response);
    policy = response.getBody(Policy.class);
    return policy;
  }
}
