/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Date;

import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestOptionsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestReviewDTO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequestStatus;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverRequestResource.POLICY_VIOLATION_ID_PATH;
import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverRequestResource.POLICY_WAIVER_REQUEST_ID_PATH;
import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverRequestResource.POLICY_WAIVER_REQUEST_REVIEW_PATH;
import static com.sonatype.insight.brain.model.policy.PolicyWaiverRequestStatus.APPROVED;
import static com.sonatype.insight.brain.model.policy.PolicyWaiverRequestStatus.REJECTED;
import static com.sonatype.insight.brain.model.policy.PolicyWaiverRequestStatus.REQUESTED;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiPolicyWaiverRequestResourceTest
    extends AbstractResourceTest
{
  private RepositoryManagerDAO repositoryManagerDAO;

  private PolicyWaiverRequestDAO policyWaiverRequestDAO;

  private PolicyWaiverDAO policyWaiverDAO;

  @Before
  public void setUp() {
    repositoryManagerDAO = lookup(RepositoryManagerDAO.class);
    policyWaiverRequestDAO = lookup(PolicyWaiverRequestDAO.class);
    policyWaiverDAO = lookup(PolicyWaiverDAO.class);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.POLICY_WAIVER_REQUEST_PATH);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_Application() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    testAddPolicyWaiverRequestByPolicyViolationId(app, policy, policyViolation);
  }

  private void testAddPolicyWaiverRequestByPolicyViolationId(
      Owner owner,
      Policy policy,
      AbstractPolicyViolation policyViolation) throws Exception
  {
    Date expiryDate = DateUtils.addDays(new Date(), 1);
    ApiPolicyWaiverRequestOptionsDTO apiPolicyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    apiPolicyWaiverRequestOptionsDTO.comment = "waiver comment";
    apiPolicyWaiverRequestOptionsDTO.expiryTime = expiryDate;
    apiPolicyWaiverRequestOptionsDTO.matcherStrategy = ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(owner.getType(), owner.getId(), policyViolation.getId())
            .body(apiPolicyWaiverRequestOptionsDTO, MediaType.APPLICATION_JSON)
            .post();

    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);
    assertPolicyWaiverRequestDTO(owner.getId(), policy, policyViolation, "waiver comment", policyViolation.getHash(),
        expiryDate, ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, REQUESTED, apiPolicyWaiverRequestDTO);
    assertPolicyWaiverRequest(owner.getId(), policy, policyViolation, "waiver comment", null, null,
        policyViolation.getHash(), expiryDate, ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, REQUESTED,
        apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  private void testUpdatePolicyWaiverRequest(
      Owner owner,
      Policy policy,
      AbstractPolicyViolation policyViolation,
      String policyWaiverRequestId) throws Exception
  {
    Date expiryDate = DateUtils.addDays(new Date(), 1);
    ApiPolicyWaiverRequestOptionsDTO apiPolicyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    apiPolicyWaiverRequestOptionsDTO.comment = "waiver comment updated";
    apiPolicyWaiverRequestOptionsDTO.expiryTime = expiryDate;
    apiPolicyWaiverRequestOptionsDTO.matcherStrategy = ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
    HttpResponse response = restRequest().path(POLICY_WAIVER_REQUEST_ID_PATH)
        .parameter(owner.getType(), owner.getId(), policyWaiverRequestId)
        .body(apiPolicyWaiverRequestOptionsDTO, MediaType.APPLICATION_JSON)
        .put();

    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);
    assertPolicyWaiverRequestDTO(owner.getId(), policy, policyViolation, "waiver comment updated",
        policyViolation.getHash(), expiryDate, ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, REQUESTED,
        apiPolicyWaiverRequestDTO);
    assertPolicyWaiverRequest(owner.getId(), policy, policyViolation, "waiver comment updated", null, null,
        policyViolation.getHash(), expiryDate, ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, REQUESTED,
        apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_Organization() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Policy policy = tempEntity.newPolicy(app);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    testAddPolicyWaiverRequestByPolicyViolationId(org, policy, policyViolation);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_NoRequestBody() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    HttpResponse response = restRequest().path(POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), policyViolation.getId())
        .post();

    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);
    assertPolicyWaiverRequestDTO(app.getId(), policy, policyViolation, null, policyViolation.getHash(), null,
        ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, REQUESTED, apiPolicyWaiverRequestDTO);
    assertPolicyWaiverRequest(app.getId(), policy, policyViolation, null, null, null, policyViolation.getHash(), null,
        ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, REQUESTED, apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_Repository() throws Exception {
    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);

    RepositoryPolicyViolation policyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    testAddPolicyWaiverRequestByPolicyViolationId(repository, policy, policyViolation);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_RepositoryManager() throws Exception {
    Repository repository = tempEntity.newRepository();
    RepositoryManager repositoryManager = repositoryManagerDAO.getById(repository.getRepositoryManagerId());
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);

    RepositoryPolicyViolation policyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    testAddPolicyWaiverRequestByPolicyViolationId(repositoryManager, policy, policyViolation);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_RepositoryContainer() throws Exception {
    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);

    RepositoryPolicyViolation policyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    testAddPolicyWaiverRequestByPolicyViolationId(RepositoryContainer.SINGLETON, policy, policyViolation);
  }

  @Test
  public void testReviewPolicyWaiverRequest_Application() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    testReviewPolicyWaiverRequest(OwnerType.APPLICATION, app.getId(), policy, policyViolation);
  }

  @Test
  public void testReviewPolicyWaiverRequest_Organization() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Policy policy = tempEntity.newPolicy(app);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    testReviewPolicyWaiverRequest(OwnerType.ORGANIZATION, org.getId(), policy, policyViolation);
  }

  @Test
  public void testReviewPolicyWaiverRequest_Repository() throws Exception {
    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);

    RepositoryPolicyViolation policyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    testReviewPolicyWaiverRequest(OwnerType.REPOSITORY, repository.getId(), policy, policyViolation);
  }

  @Test
  public void testReviewPolicyWaiverRequest_RepositoryManager() throws Exception {
    Repository repository = tempEntity.newRepository();
    RepositoryManager repositoryManager = repositoryManagerDAO.getById(repository.getRepositoryManagerId());
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);

    RepositoryPolicyViolation policyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    testReviewPolicyWaiverRequest(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(), policy, policyViolation);
  }

  @Test
  public void testReviewPolicyWaiverRequest_RepositoryContainer() throws Exception {
    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);

    RepositoryPolicyViolation policyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    testReviewPolicyWaiverRequest(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID, policy,
        policyViolation);
  }

  private void testReviewPolicyWaiverRequest(
      OwnerType ownerType,
      String ownerId,
      Policy policy,
      AbstractPolicyViolation policyViolation) throws Exception
  {
    // Add a policy waiver request
    Date expiryDate = DateUtils.addDays(new Date(), 1);
    ApiPolicyWaiverRequestOptionsDTO apiPolicyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    apiPolicyWaiverRequestOptionsDTO.comment = "waiver comment";
    apiPolicyWaiverRequestOptionsDTO.expiryTime = expiryDate;
    apiPolicyWaiverRequestOptionsDTO.matcherStrategy = ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(ownerType, ownerId, policyViolation.getId())
            .body(apiPolicyWaiverRequestOptionsDTO, MediaType.APPLICATION_JSON)
            .post();
    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);
    String policyWaiverRequestId = apiPolicyWaiverRequestDTO.policyWaiverRequestId;

    // Review/approve the policy waiver request using a different user
    User reviewer =
        tempEntity.newUser("reviewerUsername", "reviewerFirstName", "reviewerLastName", "reviewer@example.com");
    Role role = tempEntity.newRole(false /* global */, Permission.WAIVE_POLICY_VIOLATIONS);
    tempEntity.newMembershipMapping(ownerId, role.getId(), reviewer.getUsername());
    ApiPolicyWaiverRequestReviewDTO apiPolicyWaiverRequestReviewDTO = new ApiPolicyWaiverRequestReviewDTO();
    apiPolicyWaiverRequestReviewDTO.comment = "updated waiver comment";
    Date updatedExpiryDate = DateUtils.addDays(new Date(), 2);
    apiPolicyWaiverRequestReviewDTO.expiryTime = updatedExpiryDate;
    apiPolicyWaiverRequestReviewDTO.matcherStrategy = ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
    apiPolicyWaiverRequestReviewDTO.status = PolicyWaiverRequestStatus.APPROVED.name();
    response =
        restRequest().path(POLICY_WAIVER_REQUEST_REVIEW_PATH)
            .parameter(ownerType, ownerId, policyWaiverRequestId)
            .body(apiPolicyWaiverRequestReviewDTO, MediaType.APPLICATION_JSON)
            .auth(reviewer)
            .post();

    assertResponseStatus(200, response);
    apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);

    assertPolicyWaiverRequestDTO(ownerId, policy, policyViolation, "waiver comment", policyViolation.getHash(),
        expiryDate, ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, APPROVED, apiPolicyWaiverRequestDTO);
    assertPolicyWaiverRequest(ownerId, policy, policyViolation, "waiver comment", "reviewerUsername",
        "reviewerFirstName reviewerLastName", policyViolation.getHash(), expiryDate,
        ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, APPROVED, apiPolicyWaiverRequestDTO.policyWaiverRequestId);
    PolicyWaiverRequest policyWaiverRequest = policyWaiverRequestDAO.getById(policyWaiverRequestId);
    assertPolicyWaiver(ownerId, policy, policyViolation, "updated waiver comment", "reviewerUsername",
        "reviewerFirstName reviewerLastName", null /* hash */, updatedExpiryDate,
        ComponentMatcherStrategyForWaiver.ALL_COMPONENTS, policyWaiverRequest.getPolicyWaiverId());
  }

  @Test
  public void testReviewPolicyWaiverRequest_Reject() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    // Add a policy waiver request
    Date expiryDate = DateUtils.addDays(new Date(), 1);
    ApiPolicyWaiverRequestOptionsDTO apiPolicyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    apiPolicyWaiverRequestOptionsDTO.comment = "waiver comment";
    apiPolicyWaiverRequestOptionsDTO.expiryTime = expiryDate;
    apiPolicyWaiverRequestOptionsDTO.matcherStrategy = ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(app.getType(), app.getId(), policyViolation.getId())
            .body(apiPolicyWaiverRequestOptionsDTO, MediaType.APPLICATION_JSON)
            .post();
    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);
    String policyWaiverRequestId = apiPolicyWaiverRequestDTO.policyWaiverRequestId;

    // Review/reject the policy waiver request using a different user
    User reviewer =
        tempEntity.newUser("reviewerUsername", "reviewerFirstName", "reviewerLastName", "reviewer@example.com");
    Role role = tempEntity.newRole(false /* global */, Permission.WAIVE_POLICY_VIOLATIONS);
    tempEntity.newMembershipMapping(app.getId(), role.getId(), reviewer.getUsername());
    ApiPolicyWaiverRequestReviewDTO apiPolicyWaiverRequestReviewDTO = new ApiPolicyWaiverRequestReviewDTO();
    apiPolicyWaiverRequestReviewDTO.status = REJECTED.name();
    apiPolicyWaiverRequestReviewDTO.rejectionReason = "rejection reason";
    response = restRequest().path(POLICY_WAIVER_REQUEST_REVIEW_PATH)
        .parameter(app.getType(), app.getId(), policyWaiverRequestId)
        .body(apiPolicyWaiverRequestReviewDTO, MediaType.APPLICATION_JSON)
        .auth(reviewer)
        .post();

    assertResponseStatus(200, response);
    apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);

    assertPolicyWaiverRequestDTO(app.getId(), policy, policyViolation, "waiver comment", policyViolation.getHash(),
        expiryDate, ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, REJECTED, apiPolicyWaiverRequestDTO);
    assertPolicyWaiverRequest(app.getId(), policy, policyViolation, "waiver comment", "reviewerUsername",
        "reviewerFirstName reviewerLastName", policyViolation.getHash(), expiryDate,
        ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, REJECTED, apiPolicyWaiverRequestDTO.policyWaiverRequestId);
    PolicyWaiverRequest policyWaiverRequest = policyWaiverRequestDAO.getById(policyWaiverRequestId);
    assertThat(policyWaiverRequest.getRejectionReason()).isEqualTo("rejection reason");
    assertThat(policyWaiverRequest.getReviewerId()).isEqualTo("reviewerUsername");
    assertThat(policyWaiverRequest.getReviewerName()).isEqualTo("reviewerFirstName reviewerLastName");
  }

  @Test
  public void testGetPolicyWaiverRequest_Application() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    testGetPolicyWaiverRequest(OwnerType.APPLICATION, app.getId(), policy, policyViolation);
  }

  @Test
  public void testGetPolicyWaiverRequest_Organization() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Policy policy = tempEntity.newPolicy(app);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    testGetPolicyWaiverRequest(OwnerType.ORGANIZATION, org.getId(), policy, policyViolation);
  }

  @Test
  public void testGetPolicyWaiverRequest_Repository() throws Exception {
    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);

    RepositoryPolicyViolation policyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    testGetPolicyWaiverRequest(OwnerType.REPOSITORY, repository.getId(), policy, policyViolation);
  }

  @Test
  public void testGetPolicyWaiverRequest_RepositoryManager() throws Exception {
    Repository repository = tempEntity.newRepository();
    RepositoryManager repositoryManager = repositoryManagerDAO.getById(repository.getRepositoryManagerId());
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);

    RepositoryPolicyViolation policyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    testGetPolicyWaiverRequest(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(), policy, policyViolation);
  }

  @Test
  public void testGetPolicyWaiverRequest_RepositoryContainer() throws Exception {
    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);

    RepositoryPolicyViolation policyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    testGetPolicyWaiverRequest(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID, policy,
        policyViolation);
  }

  private void testGetPolicyWaiverRequest(
      OwnerType ownerType,
      String ownerId,
      Policy policy,
      AbstractPolicyViolation policyViolation) throws Exception
  {
    // Add a policy waiver request
    Date expiryDate = DateUtils.addDays(new Date(), 1);
    ApiPolicyWaiverRequestOptionsDTO apiPolicyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    apiPolicyWaiverRequestOptionsDTO.comment = "waiver comment";
    apiPolicyWaiverRequestOptionsDTO.expiryTime = expiryDate;
    apiPolicyWaiverRequestOptionsDTO.matcherStrategy = ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(ownerType, ownerId, policyViolation.getId())
            .body(apiPolicyWaiverRequestOptionsDTO, MediaType.APPLICATION_JSON)
            .post();
    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);
    String policyWaiverRequestId = apiPolicyWaiverRequestDTO.policyWaiverRequestId;

    // Get the policy waiver request created above
    response =
        restRequest().path(POLICY_WAIVER_REQUEST_ID_PATH)
            .parameter(ownerType, ownerId, policyWaiverRequestId)
            .get();

    assertResponseStatus(200, response);
    apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);

    assertPolicyWaiverRequestDTO(ownerId, policy, policyViolation, "waiver comment", policyViolation.getHash(),
        expiryDate, ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, REQUESTED, apiPolicyWaiverRequestDTO);
  }

  private void assertPolicyWaiverRequest(
      String ownerId,
      Policy policy,
      AbstractPolicyViolation abstractPolicyViolation,
      String comment,
      String reviewerId,
      String reviewerName,
      String hash,
      Date expiryTime,
      ComponentMatcherStrategyForWaiver matcherStrategy,
      PolicyWaiverRequestStatus status,
      String policyWaiverRequestId)
  {
    PolicyWaiverRequest policyWaiverRequest = policyWaiverRequestDAO.getById(policyWaiverRequestId);
    assertThat(policyWaiverRequest.getOwnerId()).isEqualTo(ownerId);
    assertThat(policyWaiverRequest.getHash()).isEqualTo(hash);
    assertThat(policyWaiverRequest.getComment()).isEqualTo(comment);
    assertThat(policyWaiverRequest.getPolicyId()).isEqualTo(policy.getId());
    assertThat(policyWaiverRequest.getRequestTime()).isNotNull();
    assertThat(policyWaiverRequest.getRequesterId()).isEqualTo("admin");
    assertThat(policyWaiverRequest.getRequesterName()).isEqualTo("Admin BuiltIn");
    assertThat(policyWaiverRequest.getReviewerId()).isEqualTo(reviewerId);
    assertThat(policyWaiverRequest.getReviewerName()).isEqualTo(reviewerName);
    assertThat(policyWaiverRequest.getExpiryTime()).isEqualTo(expiryTime);
    assertThat(policyWaiverRequest.getComponentMatchStrategy()).isEqualTo(matcherStrategy);
    assertThat(policyWaiverRequest.getConstraintFactsJson())
        .isEqualTo(abstractPolicyViolation.getConstraintFactsJson());
    assertThat(policyWaiverRequest.getStatus()).isEqualTo(status);
    assertThat(policyWaiverRequest.getPolicyViolationId()).isEqualTo(abstractPolicyViolation.getId());
  }

  private void assertPolicyWaiverRequestDTO(
      String ownerId,
      Policy policy,
      AbstractPolicyViolation abstractPolicyViolation,
      String comment,
      String hash,
      Date expiryTime,
      ComponentMatcherStrategyForWaiver matcherStrategy,
      PolicyWaiverRequestStatus status,
      ApiPolicyWaiverRequestDTO policyWaiverRequestDTO)
  {
    assertThat(policyWaiverRequestDTO.policyWaiverRequestId).isNotNull();
    assertThat(policyWaiverRequestDTO.scopeOwnerId).isEqualTo(ownerId);
    assertThat(policyWaiverRequestDTO.hash).isEqualTo(hash);
    assertThat(policyWaiverRequestDTO.comment).isEqualTo(comment);
    assertThat(policyWaiverRequestDTO.policyId).isEqualTo(policy.getId());
    assertThat(policyWaiverRequestDTO.policyName).isEqualTo(policy.getName());
    assertThat(policyWaiverRequestDTO.requestTime).isNotNull();
    assertThat(policyWaiverRequestDTO.expiryTime).isEqualTo(expiryTime);
    assertThat(policyWaiverRequestDTO.matcherStrategy).isEqualTo(matcherStrategy);
    assertThat(policyWaiverRequestDTO.constraintFactsJson)
        .isEqualTo(abstractPolicyViolation.getConstraintFactsJson());
    assertThat(policyWaiverRequestDTO.status).isEqualTo(status.name());
  }

  private void assertPolicyWaiver(
      String ownerId,
      Policy policy,
      AbstractPolicyViolation abstractPolicyViolation,
      String comment,
      String reviewerId,
      String reviewerName,
      String hash,
      Date expiryTime,
      ComponentMatcherStrategyForWaiver matcherStrategy,
      String policyWaiverId)
  {
    PolicyWaiver policyWaiverRequest = policyWaiverDAO.getById(policyWaiverId);
    assertThat(policyWaiverRequest.getOwnerId()).isEqualTo(ownerId);
    assertThat(policyWaiverRequest.getHash()).isEqualTo(hash);
    assertThat(policyWaiverRequest.getComment()).isEqualTo(comment);
    assertThat(policyWaiverRequest.getPolicyId()).isEqualTo(policy.getId());
    assertThat(policyWaiverRequest.getCreatorId()).isEqualTo(reviewerId);
    assertThat(policyWaiverRequest.getCreatorName()).isEqualTo(reviewerName);
    assertThat(policyWaiverRequest.getCreateTime()).isNotNull();
    assertThat(policyWaiverRequest.getExpiryTime()).isEqualTo(expiryTime);
    assertThat(policyWaiverRequest.getComponentMatchStrategy()).isEqualTo(matcherStrategy);
    assertThat(policyWaiverRequest.getConstraintFactsJson())
        .isEqualTo(abstractPolicyViolation.getConstraintFactsJson());
  }

  @Test
  public void testUpdatePolicyWaiverRequest_Application() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(OwnerType.APPLICATION, app.getId(), policyViolation.getId())
            .body(new ApiPolicyWaiverRequestOptionsDTO("waiver comment",
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, null, null, false), MediaType.APPLICATION_JSON)
            .post();
    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);

    testUpdatePolicyWaiverRequest(app, policy, policyViolation, apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  public void testUpdatePolicyWaiverRequest_Organization() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Policy policy = tempEntity.newPolicy(app);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(OwnerType.APPLICATION, app.getId(), policyViolation.getId())
            .body(new ApiPolicyWaiverRequestOptionsDTO("waiver comment",
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, null, null, false), MediaType.APPLICATION_JSON)
            .post();
    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);

    testUpdatePolicyWaiverRequest(org, policy, policyViolation, apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  public void testUpdatePolicyWaiverRequest_Repository() throws Exception {
    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);

    RepositoryPolicyViolation policyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(OwnerType.REPOSITORY, repository.getId(), policyViolation.getId())
            .body(new ApiPolicyWaiverRequestOptionsDTO("waiver comment",
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, null, null, false), MediaType.APPLICATION_JSON)
            .post();
    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);

    testUpdatePolicyWaiverRequest(repository, policy, policyViolation, apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  public void testUpdatePolicyWaiverRequest_RepositoryContainer() throws Exception {
    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);

    RepositoryPolicyViolation policyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(OwnerType.REPOSITORY, repository.getId(), policyViolation.getId())
            .body(new ApiPolicyWaiverRequestOptionsDTO("waiver comment",
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, null, null, false), MediaType.APPLICATION_JSON)
            .post();
    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);

    testUpdatePolicyWaiverRequest(RepositoryContainer.SINGLETON, policy, policyViolation,
        apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  public void testUpdatePolicyWaiverRequest_RepositoryManager() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager);
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);

    RepositoryPolicyViolation policyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(OwnerType.REPOSITORY, repository.getId(), policyViolation.getId())
            .body(new ApiPolicyWaiverRequestOptionsDTO("waiver comment",
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, null, null, false), MediaType.APPLICATION_JSON)
            .post();
    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);

    testUpdatePolicyWaiverRequest(repositoryManager, policy, policyViolation,
        apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }
}
