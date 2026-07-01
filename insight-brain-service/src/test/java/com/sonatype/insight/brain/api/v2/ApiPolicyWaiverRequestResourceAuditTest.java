/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Date;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestOptionsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestReviewDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
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
import com.sonatype.insight.brain.policy.ConstraintFactDTO;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverRequestResource.POLICY_VIOLATION_ID_PATH;
import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverRequestResource.POLICY_WAIVER_REQUEST_ID_PATH;
import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverRequestResource.POLICY_WAIVER_REQUEST_REVIEW_PATH;

public class ApiPolicyWaiverRequestResourceAuditTest
    extends AbstractAuditTest
{
  private PolicyWaiverRequestDAO policyWaiverRequestDAO;

  private PolicyWaiverDAO policyWaiverDAO;

  private Policy policy;

  @Before
  public void setUpPolicyViolation() {
    policyWaiverRequestDAO = lookup(PolicyWaiverRequestDAO.class);
    policyWaiverDAO = lookup(PolicyWaiverDAO.class);
    policy = tempEntity.newPolicy();
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_Application() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "testScanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    testAddPolicyWaiverRequestByPolicyViolationId(app, policyViolation);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_Organization() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "testScanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    testAddPolicyWaiverRequestByPolicyViolationId(org, policyViolation);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_Application_Unauthorized() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "testScanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    testAddPolicyWaiverRequestByPolicyViolationId_Unauthorized(app, policyViolation);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_Organization_Unauthorized() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "testScanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    testAddPolicyWaiverRequestByPolicyViolationId_Unauthorized(org, policyViolation);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_Repository() throws Exception {
    Repository repository = tempEntity.newRepository();
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    testAddPolicyWaiverRequestByPolicyViolationId(repository, repositoryPolicyViolation);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_Repository_Unauthorized() throws Exception {
    Repository repository = tempEntity.newRepository();
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    testAddPolicyWaiverRequestByPolicyViolationId_Unauthorized(repository, repositoryPolicyViolation);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_RepositoryManager() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager);
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    testAddPolicyWaiverRequestByPolicyViolationId(repositoryManager, repositoryPolicyViolation);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_RepositoryManager_Unauthorized() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager);
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    testAddPolicyWaiverRequestByPolicyViolationId_Unauthorized(repositoryManager, repositoryPolicyViolation);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_RepositoryContainer() throws Exception {
    Repository repository = tempEntity.newRepository();
    policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    testAddPolicyWaiverRequestByPolicyViolationId(RepositoryContainer.SINGLETON, repositoryPolicyViolation);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_RepositoryContainer_Unauthorized() throws Exception {
    Repository repository = tempEntity.newRepository();
    policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    testAddPolicyWaiverRequestByPolicyViolationId_Unauthorized(RepositoryContainer.SINGLETON,
        repositoryPolicyViolation);
  }

  private void testAddPolicyWaiverRequestByPolicyViolationId(
      Owner owner,
      AbstractPolicyViolation policyViolation) throws Exception
  {
    ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    policyWaiverRequestOptionsDTO.comment = "waiver comment";
    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(owner.getType(), owner.getId(), policyViolation.getId())
            .body(policyWaiverRequestOptionsDTO, MediaType.APPLICATION_JSON)
            .post();
    assertResponseStatus(200, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER_REQUEST, null);
    assertPolicyWaiverRequestData(auditDTO);
    assertOwnerData(auditDTO, owner);
  }

  private void testUpdatePolicyWaiverRequest(Owner owner, String policyWaiverRequestId) throws Exception {
    ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    policyWaiverRequestOptionsDTO.comment = "waiver comment";
    HttpResponse response = restRequest().path(POLICY_WAIVER_REQUEST_ID_PATH)
        .parameter(owner.getType(), owner.getId(), policyWaiverRequestId)
        .body(policyWaiverRequestOptionsDTO, MediaType.APPLICATION_JSON)
        .put();
    assertResponseStatus(200, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_WAIVER_REQUEST, null);
    assertPolicyWaiverRequestData(auditDTO);
    assertOwnerData(auditDTO, owner);
  }

  private void testUpdatePolicyWaiverRequest_Unauthorized(Owner owner, String policyWaiverRequestId) throws Exception {
    ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    policyWaiverRequestOptionsDTO.comment = "waiver comment";
    HttpResponse response = restRequest().path(POLICY_WAIVER_REQUEST_ID_PATH)
        .parameter(owner.getType(), owner.getId(), policyWaiverRequestId)
        .with(unauthorizedUser())
        .body(policyWaiverRequestOptionsDTO, MediaType.APPLICATION_JSON)
        .put();
    assertResponseStatus(403, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_WAIVER_REQUEST, "unauthorized");
    assertOwnerData(auditDTO, owner);
  }

  private void testAddPolicyWaiverRequestByPolicyViolationId_Unauthorized(
      Owner owner,
      AbstractPolicyViolation policyViolation) throws Exception
  {
    ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    policyWaiverRequestOptionsDTO.comment = "waiver comment";
    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(owner.getType(), owner.getId(), policyViolation.getId())
            .with(unauthorizedUser())
            .body(policyWaiverRequestOptionsDTO, MediaType.APPLICATION_JSON)
            .post();
    assertResponseStatus(403, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER_REQUEST, "unauthorized");
    assertOwnerData(auditDTO, owner);
  }

  @Test
  public void testReviewPolicyWaiverRequest_Application() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "testScanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    testReviewPolicyWaiverRequest(app, policyViolation);
  }

  @Test
  public void testReviewPolicyWaiverRequest_Application_Unauthorized() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "testScanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    testReviewPolicyWaiverRequest_Unauthorized(app, policyViolation);
  }

  @Test
  public void testReviewPolicyWaiverRequest_Organization() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "testScanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    testReviewPolicyWaiverRequest(org, policyViolation);
  }

  @Test
  public void testReviewPolicyWaiverRequest_Organization_Unauthorized() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "testScanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    testReviewPolicyWaiverRequest_Unauthorized(org, policyViolation);
  }

  @Test
  public void testReviewPolicyWaiverRequest_Repository() throws Exception {
    Repository repository = tempEntity.newRepository();
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());
    ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    policyWaiverRequestOptionsDTO.comment = "waiver comment";

    testReviewPolicyWaiverRequest(repository, repositoryPolicyViolation);
  }

  @Test
  public void testReviewPolicyWaiverRequest_Repository_Unauthorized() throws Exception {
    Repository repository = tempEntity.newRepository();
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    testReviewPolicyWaiverRequest_Unauthorized(repository, repositoryPolicyViolation);
  }

  @Test
  public void testReviewPolicyWaiverRequest_RepositoryManager() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager);
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());
    ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    policyWaiverRequestOptionsDTO.comment = "waiver comment";

    testReviewPolicyWaiverRequest(repositoryManager, repositoryPolicyViolation);
  }

  @Test
  public void testReviewPolicyWaiverRequest_RepositoryManager_Unauthorized() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager);
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    testReviewPolicyWaiverRequest_Unauthorized(repositoryManager, repositoryPolicyViolation);
  }

  @Test
  public void testReviewPolicyWaiverRequest_RepositoryContainer() throws Exception {
    Repository repository = tempEntity.newRepository();
    policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());
    ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    policyWaiverRequestOptionsDTO.comment = "waiver comment";

    testReviewPolicyWaiverRequest(RepositoryContainer.SINGLETON, repositoryPolicyViolation);
  }

  @Test
  public void testReviewPolicyWaiverRequest_RepositoryContainer_Unauthorized() throws Exception {
    Repository repository = tempEntity.newRepository();
    policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    testReviewPolicyWaiverRequest_Unauthorized(RepositoryContainer.SINGLETON, repositoryPolicyViolation);
  }

  private void testReviewPolicyWaiverRequest(Owner owner, AbstractPolicyViolation policyViolation) throws Exception {
    // Add a policy waiver request
    ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    policyWaiverRequestOptionsDTO.comment = "waiver comment";
    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(owner.getType(), owner.getId(), policyViolation.getId())
            .body(policyWaiverRequestOptionsDTO, MediaType.APPLICATION_JSON)
            .post();
    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);
    String policyWaiverRequestId = apiPolicyWaiverRequestDTO.policyWaiverRequestId;

    // Review/approve the policy waiver request
    ApiPolicyWaiverRequestReviewDTO apiPolicyWaiverRequestReviewDTO = new ApiPolicyWaiverRequestReviewDTO();
    apiPolicyWaiverRequestReviewDTO.comment = "updated waiver comment";
    Date updatedExpiryDate = DateUtils.addDays(new Date(), 2);
    apiPolicyWaiverRequestReviewDTO.expiryTime = updatedExpiryDate;
    apiPolicyWaiverRequestReviewDTO.matcherStrategy = ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
    apiPolicyWaiverRequestReviewDTO.status = PolicyWaiverRequestStatus.APPROVED.name();
    response = restRequest().path(POLICY_WAIVER_REQUEST_REVIEW_PATH)
        .parameter(owner.getType(), owner.getId(), policyWaiverRequestId)
        .body(apiPolicyWaiverRequestReviewDTO, MediaType.APPLICATION_JSON)
        .post();
    assertResponseStatus(200, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REVIEW_WAIVER_REQUEST, null);
    assertPolicyWaiverRequestData(auditDTO);
    assertOwnerData(auditDTO, owner);
    auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER, null);
    assertPolicyWaiverData(auditDTO);
    assertOwnerData(auditDTO, owner);
  }

  private void testReviewPolicyWaiverRequest_Unauthorized(
      Owner owner,
      AbstractPolicyViolation policyViolation) throws Exception
  {
    // Add a policy waiver request
    ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    policyWaiverRequestOptionsDTO.comment = "waiver comment";
    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(owner.getType(), owner.getId(), policyViolation.getId())
            .body(policyWaiverRequestOptionsDTO, MediaType.APPLICATION_JSON)
            .post();
    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);
    String policyWaiverRequestId = apiPolicyWaiverRequestDTO.policyWaiverRequestId;

    // Review/approve the policy waiver request
    ApiPolicyWaiverRequestReviewDTO apiPolicyWaiverRequestReviewDTO = new ApiPolicyWaiverRequestReviewDTO();
    apiPolicyWaiverRequestReviewDTO.comment = "updated waiver comment";
    Date updatedExpiryDate = DateUtils.addDays(new Date(), 2);
    apiPolicyWaiverRequestReviewDTO.expiryTime = updatedExpiryDate;
    apiPolicyWaiverRequestReviewDTO.matcherStrategy = ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
    apiPolicyWaiverRequestReviewDTO.status = PolicyWaiverRequestStatus.APPROVED.name();
    response = restRequest().path(POLICY_WAIVER_REQUEST_REVIEW_PATH)
        .parameter(owner.getType(), owner.getId(), policyWaiverRequestId)
        .with(unauthorizedUser())
        .body(apiPolicyWaiverRequestReviewDTO, MediaType.APPLICATION_JSON)
        .post();
    assertResponseStatus(403, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REVIEW_WAIVER_REQUEST, "unauthorized");
    assertOwnerData(auditDTO, owner);
  }

  private void assertPolicyWaiverRequestData(AuditDTO auditDTO) {
    String policyWaiverRequestId = (String) auditDTO.data.get("policyWaiverRequestId");
    PolicyWaiverRequest policyWaiverRequest = policyWaiverRequestDAO.getByIdNotNull(policyWaiverRequestId);
    assertPolicyWaiverRequestData(auditDTO, policyWaiverRequest);
  }

  private void assertPolicyWaiverRequestData(AuditDTO auditDTO, PolicyWaiverRequest policyWaiverRequest) {
    assertCustomData(auditDTO, "policyId", policyWaiverRequest.getPolicyId());
    assertCustomData(auditDTO, "policyName", getPolicyDAO().getById(policyWaiverRequest.getPolicyId()).getName());
    assertCustomData(auditDTO, "policyWaiverRequestId", policyWaiverRequest.getId());
    assertCustomData(auditDTO, "comment", policyWaiverRequest.getComment());
    assertCustomData(auditDTO, "componentHash", policyWaiverRequest.getHash());
    assertCustomObject(auditDTO, "policyConstraints",
        policyWaiverRequest.getConstraintFacts().stream().map(ConstraintFactDTO::new).collect(Collectors.toList()));
  }

  private void assertPolicyWaiverData(AuditDTO auditDTO) {
    String policyWaiverId = (String) auditDTO.data.get("policyWaiverId");
    PolicyWaiver policyWaiver = policyWaiverDAO.getByIdNotNull(policyWaiverId);
    assertPolicyWaiverData(auditDTO, policyWaiver);
  }

  private void assertPolicyWaiverData(AuditDTO auditDTO, PolicyWaiver policyWaiver) {
    assertCustomData(auditDTO, "policyId", policyWaiver.getPolicyId());
    assertCustomData(auditDTO, "policyName", getPolicyDAO().getById(policyWaiver.getPolicyId()).getName());
    assertCustomData(auditDTO, "policyWaiverId", policyWaiver.getId());
    assertCustomData(auditDTO, "comment", policyWaiver.getComment());
    assertCustomData(auditDTO, "componentHash", policyWaiver.getHash());
    if (policyWaiver.getConstraintFacts() == null) {
      assertCustomData(auditDTO, "policyConstraints", null);
    }
    else {
      assertCustomObject(auditDTO, "policyConstraints",
          policyWaiver.getConstraintFacts().stream().map(ConstraintFactDTO::new).collect(Collectors.toList()));
    }
  }

  @Override
  public HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.POLICY_WAIVER_REQUEST_PATH);
  }

  @Test
  public void testUpdatePolicyWaiverRequest_Application() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "testScanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(OwnerType.APPLICATION, app.getId(), policyViolation.getId())
            .body(new ApiPolicyWaiverRequestOptionsDTO("waiver comment",
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, null, null, false), MediaType.APPLICATION_JSON)
            .post();
    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);

    testUpdatePolicyWaiverRequest(app, apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  public void testUpdatePolicyWaiverRequest_Application_Unauthorized() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "testScanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(OwnerType.APPLICATION, app.getId(), policyViolation.getId())
            .body(new ApiPolicyWaiverRequestOptionsDTO("waiver comment",
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, null, null, false), MediaType.APPLICATION_JSON)
            .post();
    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);

    testUpdatePolicyWaiverRequest_Unauthorized(app, apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  public void testUpdatePolicyWaiverRequest_Organization() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "testScanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(OwnerType.APPLICATION, app.getId(), policyViolation.getId())
            .body(new ApiPolicyWaiverRequestOptionsDTO("waiver comment",
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, null, null, false), MediaType.APPLICATION_JSON)
            .post();
    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);

    testUpdatePolicyWaiverRequest(org, apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  public void testUpdatePolicyWaiverRequest_Organization_Unauthorized() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "testScanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(OwnerType.APPLICATION, app.getId(), policyViolation.getId())
            .body(new ApiPolicyWaiverRequestOptionsDTO("waiver comment",
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, null, null, false), MediaType.APPLICATION_JSON)
            .post();
    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);

    testUpdatePolicyWaiverRequest_Unauthorized(org, apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  public void testUpdatePolicyWaiverRequest_Repository() throws Exception {
    Repository repository = tempEntity.newRepository();
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(OwnerType.REPOSITORY, repository.getId(), repositoryPolicyViolation.getId())
            .body(new ApiPolicyWaiverRequestOptionsDTO("waiver comment",
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, null, null, false), MediaType.APPLICATION_JSON)
            .post();
    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);

    testUpdatePolicyWaiverRequest(repository, apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  public void testUpdatePolicyWaiverRequest_Repository_Unauthorized() throws Exception {
    Repository repository = tempEntity.newRepository();
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(OwnerType.REPOSITORY, repository.getId(), repositoryPolicyViolation.getId())
            .body(new ApiPolicyWaiverRequestOptionsDTO("waiver comment",
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, null, null, false), MediaType.APPLICATION_JSON)
            .post();
    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);

    testUpdatePolicyWaiverRequest_Unauthorized(repository, apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  public void testUpdatePolicyWaiverRequest_RepositoryManager_Unauthorized() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager);
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(OwnerType.REPOSITORY, repository.getId(), repositoryPolicyViolation.getId())
            .body(new ApiPolicyWaiverRequestOptionsDTO("waiver comment",
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, null, null, false), MediaType.APPLICATION_JSON)
            .post();
    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);

    testUpdatePolicyWaiverRequest_Unauthorized(repositoryManager, apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  public void testUpdatePolicyWaiverRequest_RepositoryManager() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager);
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(OwnerType.REPOSITORY, repository.getId(), repositoryPolicyViolation.getId())
            .body(new ApiPolicyWaiverRequestOptionsDTO("waiver comment",
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, null, null, false), MediaType.APPLICATION_JSON)
            .post();
    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);

    testUpdatePolicyWaiverRequest(repositoryManager, apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  public void testUpdatePolicyWaiverRequest_RepositoryContainer() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager);
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(OwnerType.REPOSITORY, repository.getId(), repositoryPolicyViolation.getId())
            .body(new ApiPolicyWaiverRequestOptionsDTO("waiver comment",
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, null, null, false), MediaType.APPLICATION_JSON)
            .post();
    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);

    testUpdatePolicyWaiverRequest(RepositoryContainer.SINGLETON, apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  public void testUpdatePolicyWaiverRequest_RepositoryContainer_Unauthorized() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager);
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(OwnerType.REPOSITORY, repository.getId(), repositoryPolicyViolation.getId())
            .body(new ApiPolicyWaiverRequestOptionsDTO("waiver comment",
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, null, null, false), MediaType.APPLICATION_JSON)
            .post();
    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);

    testUpdatePolicyWaiverRequest_Unauthorized(RepositoryContainer.SINGLETON,
        apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  // CLM-41741: requester-only withdraw of pending waiver requests.

  @Test
  public void testWithdrawPolicyWaiverRequest_Application() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanIdWAudit");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    testWithdrawPolicyWaiverRequest(app, policyViolation);
  }

  @Test
  public void testWithdrawPolicyWaiverRequest_Organization() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanIdWAudit");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    testWithdrawPolicyWaiverRequest(org, policyViolation);
  }

  private void testWithdrawPolicyWaiverRequest(Owner owner, AbstractPolicyViolation policyViolation) throws Exception {
    // Submit a request first so we have something to withdraw.
    HttpResponse submit = restRequest().path(POLICY_VIOLATION_ID_PATH)
        .parameter(owner.getType(), owner.getId(), policyViolation.getId())
        .body(new ApiPolicyWaiverRequestOptionsDTO("waiver comment",
            ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, null, null, false), MediaType.APPLICATION_JSON)
        .post();
    assertResponseStatus(200, submit);
    ApiPolicyWaiverRequestDTO created = submit.getBody(ApiPolicyWaiverRequestDTO.class);

    // Capture the entity BEFORE delete so we can assert against its fields after withdrawal
    // (the row is hard-deleted and assertPolicyWaiverRequestData(auditDTO) without a 2nd arg
    // would otherwise try to look it up by id and fail).
    PolicyWaiverRequest snapshot = policyWaiverRequestDAO.getByIdNotNull(created.policyWaiverRequestId);

    HttpResponse withdraw = restRequest().path(POLICY_WAIVER_REQUEST_ID_PATH)
        .parameter(owner.getType(), owner.getId(), created.policyWaiverRequestId)
        .delete();
    assertResponseStatus(204, withdraw);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.WITHDRAW_WAIVER_REQUEST, null);
    assertPolicyWaiverRequestData(auditDTO, snapshot);
    assertOwnerData(auditDTO, owner);
  }
}
