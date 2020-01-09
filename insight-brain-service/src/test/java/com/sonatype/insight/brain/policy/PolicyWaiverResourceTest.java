/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.dto.ApplicableContext;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.policy.PolicyWaiverResource.AppliedWaivers;
import com.sonatype.insight.brain.policy.PolicyWaiverResource.PolicyWaiverDTO;
import com.sonatype.insight.brain.policy.PolicyWaiverResource.WaiversByOwner;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyWaiverResourceTest
    extends AbstractResourceTest
{
  private HttpRequest restRequest(OwnerType ownerType, String ownerId) {
    return restRequest().path(PolicyWaiverResource.RESOURCE_PATH).parameter(ownerType, ownerId);
  }

  @Test
  public void testCRUD_Application() throws Exception {
    String appPublicId = "PolicyWaiverResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(appPublicId);
    String constraintFactsJson = JsonUtils.writeUnformatted(Collections.singletonList(new ConstraintFact()));

    testCRUD(OwnerType.APPLICATION, appPublicId, application.getId(), constraintFactsJson);
  }

  @Test
  public void testCRUD_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization("PolicyWaiverResourceTest");
    String constraintFactsJson = JsonUtils.writeUnformatted(Collections.singletonList(new ConstraintFact()));

    testCRUD(OwnerType.ORGANIZATION, organization.getId(), organization.getId(), constraintFactsJson);
  }

  @Test
  public void testCRUD_Repository() throws Exception {
    Repository repository = tempEntity.newRepository("foo");
    String constraintFactsJson = JsonUtils.writeUnformatted(Collections.singletonList(new ConstraintFact()));

    testCRUD(OwnerType.REPOSITORY, repository.getId(), repository.getId(), constraintFactsJson);
  }

  @Test
  public void testCRUD_NullConstraintFacts() throws Exception {
    String appPublicId = "PolicyWaiverResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(appPublicId);
    String policyId = createPolicy(application.getId()).getId();

    PolicyWaiver policyWaiver = new PolicyWaiver("12345678901234567890", policyId, null /* ownerId */, "My comment");
    HttpResponse response = restRequest(OwnerType.APPLICATION, application.getPublicId()).body(policyWaiver).post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Policy waiver must have constraint facts.");
  }

  private void testCRUD(OwnerType ownerType, String ownerPublicId, String ownerId, String constraintFactsJson)
      throws Exception
  {
    String policyId = createPolicy(ownerId).getId();

    // Create
    PolicyWaiver policyWaiver = new PolicyWaiver("12345678901234567890", policyId, null /* ownerId */, "My comment");
    policyWaiver.setConstraintFactsJson(constraintFactsJson);
    HttpResponse response = restRequest(ownerType, ownerPublicId).body(policyWaiver).post();
    assertResponseStatus(200, response);
    policyWaiver = response.getBody(PolicyWaiver.class);
    assertPolicyWaiver(policyId, ownerId, "My comment", policyWaiver);
    assertThat(new PolicyWaiverDAO().getById(policyWaiver.getId()).getConstraintFactsJson())
        .isEqualTo(constraintFactsJson);

    // Get
    response = restRequest(ownerType, ownerPublicId).path("component", policyWaiver.getHash()).get();
    assertResponseStatus(200, response);
    AppliedWaivers policyWaivers = response.getBody(AppliedWaivers.class);
    assertThat(policyWaivers).isNotNull();
    assertThat(policyWaivers.waiversByOwner).hasSize(1);
    assertThat(policyWaivers.waiversByOwner.get(0).waivers).hasSize(1);
    assertPolicyWaiverDTO(policyId, ownerPublicId, "My comment", policyWaivers.waiversByOwner.get(0).waivers.get(0));

    // Delete
    response = restRequest(ownerType, ownerPublicId).path(policyWaiver.getId()).delete();
    assertResponseStatus(204, response);

    // Get
    response = restRequest(ownerType, ownerPublicId).path("component", policyWaiver.getHash()).get();
    assertResponseStatus(200, response);
    policyWaivers = response.getBody(AppliedWaivers.class);
    assertThat(policyWaivers).isNotNull();
    assertThat(policyWaivers.waiversByOwner).isEmpty();
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

  @Test
  public void testDelete_OwnerIdMismatch_Repository() throws Exception {
    Repository repository1 = tempEntity.newRepository("PolicyWaiverResourceTest1");
    Repository repository2 = tempEntity.newRepository("PolicyWaiverResourceTest2");

    testDelete_OwnerIdMismatch(OwnerType.REPOSITORY, repository1.getId(), repository1.getId(), repository2.getId());
  }

  private void assertWaiversByOwner(Owner owner, String policyId, String waiverComment, WaiversByOwner actual) {
    String expectedOwnerId = OwnerType.APPLICATION.equals(owner.getType()) ? owner.getPublicId() : owner.getId();
    assertThat(actual.ownerId).isEqualTo(expectedOwnerId);
    assertThat(actual.ownerName).isEqualTo(owner.getName());
    assertThat(actual.ownerType).isEqualTo(owner.getType());
    assertThat(actual.waivers).hasSize(1);
    assertPolicyWaiverDTO(policyId, expectedOwnerId, waiverComment, actual.waivers.get(0));
  }

  @Test
  public void testGetPolicyWaiversByHash_Application() throws Exception {
    testGetPolicyWaiversByHash(tempEntity.newApplicationWithParent("PolicyWaiverResourceTest_AppId1"));
  }

  @Test
  public void testGetPolicyWaiversByHash_Repository() throws Exception {
    testGetPolicyWaiversByHash(tempEntity.newRepository());
  }

  private void testGetPolicyWaiversByHash(Owner owner) throws Exception {
    OwnerDAO ownerDAO = new OwnerDAO();
    Owner parent = ownerDAO.getById(owner.getParentOwnerId());
    Owner grandparent = ownerDAO.getById(parent.getParentOwnerId());

    Policy policy = tempEntity.newPolicy(grandparent);
    String hash = "12345678901234567890";

    String restId = OwnerType.APPLICATION.equals(owner.getType()) ? owner.getPublicId() : owner.getId();

    // Verify owner level
    tempEntity.newWaiver(hash, policy.getId(), owner.getId(), "My comment");
    HttpResponse response = restRequest(owner.getType(), restId).path("component", hash).get();
    assertResponseStatus(200, response);
    AppliedWaivers waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(1);
    assertWaiversByOwner(owner, policy.getId(), "My comment", waivers.waiversByOwner.get(0));
    response = restRequest(parent.getType(), parent.getId()).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(0);
    response = restRequest(grandparent.getType(), grandparent.getId()).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(0);

    // Verify parent owner level
    tempEntity.newWaiver(hash, policy.getId(), parent.getId(), "My comment");
    response = restRequest(owner.getType(), restId).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(2);
    assertWaiversByOwner(owner, policy.getId(), "My comment", waivers.waiversByOwner.get(0));
    assertWaiversByOwner(parent, policy.getId(), "My comment", waivers.waiversByOwner.get(1));
    response = restRequest(parent.getType(), parent.getId()).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(1);
    assertWaiversByOwner(parent, policy.getId(), "My comment", waivers.waiversByOwner.get(0));
    response = restRequest(grandparent.getType(), grandparent.getId()).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(0);

    // Verify grandparent organization level
    tempEntity.newWaiver(hash, policy.getId(), grandparent.getId(), "My comment");
    response = restRequest(owner.getType(), restId).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(3);
    assertWaiversByOwner(owner, policy.getId(), "My comment", waivers.waiversByOwner.get(0));
    assertWaiversByOwner(parent, policy.getId(), "My comment", waivers.waiversByOwner.get(1));
    assertWaiversByOwner(grandparent, policy.getId(), "My comment", waivers.waiversByOwner.get(2));
    response = restRequest(parent.getType(), parent.getId()).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(2);
    assertWaiversByOwner(parent, policy.getId(), "My comment", waivers.waiversByOwner.get(0));
    assertWaiversByOwner(grandparent, policy.getId(), "My comment", waivers.waiversByOwner.get(1));
    response = restRequest(grandparent.getType(), grandparent.getId()).path("component", hash).get();
    assertResponseStatus(200, response);
    waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(1);
    assertWaiversByOwner(grandparent, policy.getId(), "My comment", waivers.waiversByOwner.get(0));
  }

  @Test
  public void testGetPolicyWaiversByHash_WaiverOnNoLongerApplicablePolicy() throws Exception {
    Organization org = tempEntity.newOrganization();
    Tag tag1 = tempEntity.newTag(org.getId());
    Tag tag2 = tempEntity.newTag(org.getId());
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newApplicationTag(app.getId(), tag1.getId());
    Policy policy = tempEntity.newPolicy(org);
    PolicyTag policyTag = tempEntity.newPolicyTag(policy.getId(), tag1.getId());

    tempEntity.newWaiver("hash", policy.getId(), app.getId(), "Test Comment");

    // update policy tags so it no longer applies to the application
    new PolicyTagDAO().delete(policyTag);
    tempEntity.newPolicyTag(policy.getId(), tag2.getId());

    HttpResponse response = restRequest(OwnerType.APPLICATION, app.getPublicId()).path("component", "hash").get();
    assertResponseStatus(200, response);
    AppliedWaivers waivers = response.getBody(AppliedWaivers.class);
    assertThat(waivers.waiversByOwner).hasSize(1);
    assertWaiversByOwner(app, policy.getId(), "Test Comment", waivers.waiversByOwner.get(0));
  }

  private void testDelete_OwnerIdMismatch(OwnerType ownerType,
                                          String ownerPublicId1,
                                          String ownerId1,
                                          String ownerPublicId2) throws Exception
  {
    String policyId = createPolicy(ownerId1).getId();

    PolicyWaiver policyWaiver = new PolicyWaiver("12345678901234567890", policyId, null /* ownerId */,
        Collections.singletonList(new ConstraintFact("id", "name", "operator")), "My comment");
    HttpResponse response = restRequest(ownerType, ownerPublicId1).body(policyWaiver).post();
    assertResponseStatus(200, response);
    policyWaiver = response.getBody(PolicyWaiver.class);

    response = restRequest(ownerType, ownerPublicId2).path(policyWaiver.getId()).delete();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo(
        "Cannot find a policy waiver with ID " + policyWaiver.getId() + " for " + ownerType + " ID " + ownerPublicId2);
    // Verify that the policy waiver was not deleted
    PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();
    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getByOwnerId(ownerId1);
    assertThat(policyWaivers).hasSize(1);
    assertPolicyWaiver(policyId, ownerId1, "My comment", policyWaivers.get(0));
  }

  @Test
  public void testDelete_Nonexistent_Application() throws Exception {
    String appPublicId = "PolicyWaiverResourceTest_AppId";
    tempEntity.newApplicationWithParent(appPublicId);

    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId).path("YettiId").delete();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Cannot find a policy waiver with ID YettiId.");
  }

  @Test
  public void testDelete_Nonexistent_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization("PolicyWaiverResourceTest");

    HttpResponse response = restRequest(OwnerType.ORGANIZATION, organization.getId()).path("YettiId").delete();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Cannot find a policy waiver with ID YettiId.");
  }

  @Test
  public void testDelete_Nonexistent_Repository() throws Exception {
    Repository repository = tempEntity.newRepository("PolicyWaiverResourceTest");

    HttpResponse response = restRequest(OwnerType.REPOSITORY, repository.getId()).path("YettiId").delete();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Cannot find a policy waiver with ID YettiId.");
  }

  @Test
  public void testDelete_Nonexistent_RepositoryContainer() throws Exception {
    HttpResponse response = restRequest(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.SINGLETON.getId()).path(
        "YettiId").delete();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Cannot find a policy waiver with ID YettiId.");
  }

  private void assertPolicyWaiverDTO(String policyId, String ownerId, String comment, PolicyWaiverDTO actual) {
    assertPolicyWaiver(policyId, ownerId, comment, actual);
    assertThat(actual.policyName).isEqualTo(new PolicyDAO().getById(policyId).getName());
  }

  private void assertPolicyWaiver(String policyId, String ownerId, String comment, PolicyWaiver actual) {
    assertThat(actual.getPolicyId()).isEqualTo(policyId);
    assertThat(actual.getOwnerId()).isEqualTo(ownerId);
    assertThat(actual.getComment()).isEqualTo(comment);
  }

  @Test
  public void testGetApplicableContexts() throws Exception {
    String appPublicId = "testGetApplicableContexts";
    Organization org = tempEntity.newOrganization();
    Organization parentOrg = new OrganizationDAO().getById(org.getParentOrganizationId());
    Application app = tempEntity.newApplication(appPublicId, org.getId());

    // Verify application level
    Policy policy = tempEntity.newPolicy(app);
    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable/context", policy.getId())
        .get();
    assertResponseStatus(200, response);
    ApplicableContext result = response.getBody(ApplicableContext.class);
    assertApplicableContext(app, result);
    assertThat(result.getChildren()).isNull();

    // Verify organization level
    policy = tempEntity.newPolicy(org);
    response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable/context", policy.getId()).get();
    assertResponseStatus(200, response);
    result = response.getBody(ApplicableContext.class);
    assertApplicableContext(org, result);
    assertThat(result.getChildren()).hasSize(1);
    ApplicableContext childContext = result.getChildren().get(0);
    assertApplicableContext(app, childContext);
    assertThat(childContext.getChildren()).isNull();

    // Verify parent organization level
    policy = tempEntity.newPolicy(parentOrg);
    response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable/context", policy.getId()).get();
    assertResponseStatus(200, response);
    result = response.getBody(ApplicableContext.class);
    assertApplicableContext(parentOrg, result);
    assertThat(result.getChildren()).hasSize(1);
    childContext = result.getChildren().get(0);
    assertApplicableContext(org, childContext);
    assertThat(childContext.getChildren()).hasSize(1);
    childContext = childContext.getChildren().get(0);
    assertApplicableContext(app, childContext);
    assertThat(childContext.getChildren()).isNull();
  }

  @Test
  public void testGetApplicableContexts_Repository() throws Exception {
    Repository repository = tempEntity.newRepository();

    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    HttpResponse response = restRequest(OwnerType.REPOSITORY, repository.getId()).path("applicable/context",
        policy.getId()).get();
    assertResponseStatus(200, response);
    ApplicableContext result = response.getBody(ApplicableContext.class);

    LinkedList<Owner> ownerHierarchy = new LinkedList<>();
    for (Owner owner : new OwnerDAO().walkHierarchy(repository)) {
      ownerHierarchy.push(owner);
    }

    ApplicableContext childContext = result;
    while (!ownerHierarchy.isEmpty()) {
      Owner context = ownerHierarchy.pop();
      assertApplicableContext(context, childContext);
      if (ownerHierarchy.isEmpty()) {
        assertThat(childContext.getChildren()).isNull();
      }
      else {
        assertThat(childContext.getChildren()).hasSize(1);
        childContext = childContext.getChildren().get(0);
      }
    }
  }

  @Test
  public void testGetApplicableContexts_PolicyNotApplicable() throws Exception {
    String appPublicId = "testGetApplicableContextsPolicyNotApplicable";
    tempEntity.newApplicationWithParent(appPublicId);
    Application otherApp = tempEntity.newApplicationWithParent("otherApp");

    Policy policy = tempEntity.newPolicy(otherApp);
    HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId).path("applicable/context", policy.getId())
        .get();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Cannot find a policy with ID " + policy.getId()
        + " for application public ID " + appPublicId);
  }

  @Test
  public void testGetApplicableContexts_PolicyNotApplicable_Repository() throws Exception {
    Repository repository = tempEntity.newRepository();
    Application otherApp = tempEntity.newApplicationWithParent("anApp");

    Policy policy = tempEntity.newPolicy(otherApp);
    HttpResponse response = restRequest(OwnerType.REPOSITORY, repository.getId()).path("applicable/context",
        policy.getId()).get();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Cannot find a policy with ID " + policy.getId()
        + " for repository public ID " + repository.getPublicId());
  }

  private void assertApplicableContext(Owner owner, ApplicableContext actual) {
    assertThat(actual).isNotNull();
    assertThat(actual.getId())
        .isEqualTo(OwnerType.APPLICATION.equals(owner.getType()) ? owner.getPublicId() : owner.getId());
    assertThat(actual.getName()).isEqualTo(owner.getName());
    assertThat(actual.getType()).isEqualTo(owner.getType());
  }

  // not all owner types are valid for policy creation
  private Policy createPolicy(String ownerId) {
    for (Owner owner : new OwnerDAO().walkHierarchy(ownerId)) {
      if (isPolicyApplicable(owner.getType())) {
        return tempEntity.newPolicy(owner);
      }
    }
    throw new IllegalStateException("No valid policy targets found");
  }

  private boolean isPolicyApplicable(OwnerType candidate) {
    return OwnerType.ORGANIZATION.equals(candidate) || OwnerType.APPLICATION.equals(candidate);
  }
}
