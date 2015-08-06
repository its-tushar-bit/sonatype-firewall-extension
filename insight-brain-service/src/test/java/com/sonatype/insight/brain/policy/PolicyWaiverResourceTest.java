/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dto.ApplicableContext;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.policy.PolicyWaiverResource.AppliedWaivers;
import com.sonatype.insight.brain.policy.PolicyWaiverResource.WaiversByOwner;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class PolicyWaiverResourceTest
    extends AbstractResourceTest
{
  private HttpRequest restRequest(OwnerType ownerType, String ownerId) {
    return restRequest().path(PolicyWaiverResource.SERVICE_PATH).parameter(ownerType, ownerId);
  }

  @Test
  public void testCRUD_Application() throws Exception {
    String appPublicId = "PolicyWaiverResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(appPublicId);

    testCRUD(OwnerType.APPLICATION, appPublicId, application.getId());
  }

  @Test
  public void testCRUD_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization("PolicyWaiverResourceTest");

    testCRUD(OwnerType.ORGANIZATION, organization.getId(), organization.getId());
  }

  private void testCRUD(OwnerType ownerType, String ownerPublicId, String ownerId) throws Exception {
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

    testDelete_OwnerIdMismatch(OwnerType.APPLICATION, appPublicId1, application1.getId(), appPublicId2);
  }

  @Test
  public void testDelete_OwnerIdMismatch_Organization() throws Exception {
    Organization organization1 = tempEntity.newOrganization("PolicyWaiverResourceTest1");
    Organization organization2 = tempEntity.newOrganization("PolicyWaiverResourceTest2");

    testDelete_OwnerIdMismatch(OwnerType.ORGANIZATION, organization1.getId(), organization1.getId(),
        organization2.getId());
  }

  private void assertWaiversByOwner(Owner owner, String policyId, String waiverComment, WaiversByOwner actual) {
    assertThat(actual.ownerId, is(owner.getPublicId()));
    assertThat(actual.ownerName, is(owner.getName()));
    assertThat(actual.ownerType, is(owner.getType()));
    assertThat(actual.waivers, hasSize(1));
    assertPolicyWaiver(policyId, owner.getPublicId(), waiverComment, actual.waivers.get(0));
  }

  @Test
  public void testGetPolicyWaiversByHash() throws Exception {
    Organization org = tempEntity.newOrganization("PolicyWaiverResourceTest1");
    Organization parentOrg = new OrganizationDAO().getById(org.getParentOrganizationId());
    String appPublicId = "PolicyWaiverResourceTest_AppId1";
    Application app = tempEntity.newApplication("PolicyWaiverResourceTest AppId1", appPublicId, org.getId());
    Policy policy = tempEntity.newPolicy(parentOrg.getId(), "My policy");
    String hash = "12345678901234567890";

    // Verify application level
    tempEntity.newWaiver(hash, policy.getId(), app.getId(), "My comment");
    HttpResponse response = restRequest(OwnerType.APPLICATION, app.getPublicId()).path("component", hash).get();
    assertResponseStatus(200, response);
    AppliedWaivers waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner, hasSize(1));
    assertWaiversByOwner(app, policy.getId(), "My comment", waivers.waiversByOwner.get(0));
    response = restRequest(OwnerType.ORGANIZATION, org.getId()).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner, hasSize(0));
    response = restRequest(OwnerType.ORGANIZATION, parentOrg.getId()).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner, hasSize(0));

    // Verify organization level
    tempEntity.newWaiver(hash, policy.getId(), org.getId(), "My comment");
    response = restRequest(OwnerType.APPLICATION, app.getPublicId()).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner, hasSize(2));
    assertWaiversByOwner(app, policy.getId(), "My comment", waivers.waiversByOwner.get(0));
    assertWaiversByOwner(org, policy.getId(), "My comment", waivers.waiversByOwner.get(1));
    response = restRequest(OwnerType.ORGANIZATION, org.getId()).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner, hasSize(1));
    assertWaiversByOwner(org, policy.getId(), "My comment", waivers.waiversByOwner.get(0));
    response = restRequest(OwnerType.ORGANIZATION, parentOrg.getId()).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner, hasSize(0));

    // Verify parent organization level
    tempEntity.newWaiver(hash, policy.getId(), org.getParentOrganizationId(), "My comment");
    response = restRequest(OwnerType.APPLICATION, app.getPublicId()).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner, hasSize(3));
    assertWaiversByOwner(app, policy.getId(), "My comment", waivers.waiversByOwner.get(0));
    assertWaiversByOwner(org, policy.getId(), "My comment", waivers.waiversByOwner.get(1));
    assertWaiversByOwner(parentOrg, policy.getId(), "My comment", waivers.waiversByOwner.get(2));
    response = restRequest(OwnerType.ORGANIZATION, org.getId()).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner, hasSize(2));
    assertWaiversByOwner(org, policy.getId(), "My comment", waivers.waiversByOwner.get(0));
    assertWaiversByOwner(parentOrg, policy.getId(), "My comment", waivers.waiversByOwner.get(1));
    response = restRequest(OwnerType.ORGANIZATION, parentOrg.getId()).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner, hasSize(1));
    assertWaiversByOwner(parentOrg, policy.getId(), "My comment", waivers.waiversByOwner.get(0));
  }

  private void testDelete_OwnerIdMismatch(OwnerType ownerType, String ownerPublicId1, String ownerId1,
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

    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId).path("YettiId").delete();
    assertResponseStatus(404, response);
    Assert.assertEquals("Cannot find a policy waiver with ID YettiId.", response.getBodyText());
  }

  @Test
  public void testDelete_Nonexistent_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization("PolicyWaiverResourceTest");

    HttpResponse response = restRequest(OwnerType.ORGANIZATION, organization.getId()).path("YettiId").delete();
    assertResponseStatus(404, response);
    Assert.assertEquals("Cannot find a policy waiver with ID YettiId.", response.getBodyText());
  }

  private void assertPolicyWaiver(String policyId, String ownerId, String comment, PolicyWaiver actual) {
    assertEquals(policyId, actual.getPolicyId());
    assertEquals(ownerId, actual.getOwnerId());
    assertEquals(comment, actual.getComment());
  }

  @Test
  public void testGetApplicableContexts() throws Exception {
    String appPublicId = "testGetApplicableContexts";
    Organization org = tempEntity.newOrganization();
    Organization parentOrg = new OrganizationDAO().getById(org.getParentOrganizationId());
    Application app = tempEntity.newApplication(appPublicId, org.getId());

    // Verify application level
    Policy policy = tempEntity.newPolicy(app.getId(), "App Policy");
    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable/context", policy.getId())
        .get();
    assertResponseStatus(200, response);
    ApplicableContext result = response.getBody(ApplicableContext.class);
    assertApplicableContext(app, result);
    assertThat(result.getChildren(), is(nullValue()));

    // Verify organization level
    policy = tempEntity.newPolicy(org.getId(), "Org Policy");
    response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable/context", policy.getId()).get();
    assertResponseStatus(200, response);
    result = response.getBody(ApplicableContext.class);
    assertApplicableContext(org, result);
    assertThat(result.getChildren(), hasSize(1));
    ApplicableContext childContext = result.getChildren().get(0);
    assertApplicableContext(app, childContext);
    assertThat(childContext.getChildren(), is(nullValue()));

    // Verify parent organization level
    policy = tempEntity.newPolicy(parentOrg.getId(), "Parent Org Policy");
    response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable/context", policy.getId()).get();
    assertResponseStatus(200, response);
    result = response.getBody(ApplicableContext.class);
    assertApplicableContext(parentOrg, result);
    assertThat(result.getChildren(), hasSize(1));
    childContext = result.getChildren().get(0);
    assertApplicableContext(org, childContext);
    assertThat(childContext.getChildren(), hasSize(1));
    childContext = childContext.getChildren().get(0);
    assertApplicableContext(app, childContext);
    assertThat(childContext.getChildren(), is(nullValue()));
  }

  @Test
  public void testGetApplicableContexts_PolicyNotApplicable() throws Exception {
    String appPublicId = "testGetApplicableContextsPolicyNotApplicable";
    tempEntity.newApplicationWithParent(appPublicId);
    Application otherApp = tempEntity.newApplicationWithParent("otherApp");

    Policy policy = tempEntity.newPolicy(otherApp.getId(), "Policy");
    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable/context", policy.getId())
        .get();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText(), is("Cannot find a policy with ID " + policy.getId()
        + " for application public ID " + appPublicId));
  }

  private void assertApplicableContext(Owner owner, ApplicableContext actual) {
    assertNotNull(actual);
    assertEquals(owner.getPublicId(), actual.getId());
    assertEquals(owner.getName(), actual.getName());
    assertEquals(owner.getType(), actual.getType());
  }
}
