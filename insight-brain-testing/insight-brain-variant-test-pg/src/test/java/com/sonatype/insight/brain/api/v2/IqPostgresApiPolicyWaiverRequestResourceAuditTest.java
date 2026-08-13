/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestOptionsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestReviewDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequestStatus;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.policy.ConstraintFactDTO;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.test.LogOutput;

import org.apache.commons.lang.time.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverRequestResource.POLICY_VIOLATION_ID_PATH;
import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverRequestResource.POLICY_WAIVER_REQUEST_ID_PATH;
import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverRequestResource.POLICY_WAIVER_REQUEST_REVIEW_PATH;
import static org.assertj.core.api.Assertions.assertThat;

@IqPostgresTest
class IqPostgresApiPolicyWaiverRequestResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  private PolicyWaiverRequestDAO policyWaiverRequestDAO;

  private PolicyWaiverDAO policyWaiverDAO;

  private OrganizationDAO organizationDAO;

  private RepositoryDAO repositoryDAO;

  private Policy policy;

  private User unauthorizedUser;

  @BeforeEach
  void setUpPolicyViolation() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();

    policyWaiverRequestDAO = ctx.lookup(PolicyWaiverRequestDAO.class);
    policyWaiverDAO = ctx.lookup(PolicyWaiverDAO.class);
    organizationDAO = ctx.lookup(OrganizationDAO.class);
    repositoryDAO = ctx.lookup(RepositoryDAO.class);
    policy = ctx.tempEntity().newPolicy();
  }

  @AfterEach
  void after() {
    logOutput.tearDown();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public String getUnauthorizedUsername() {
    return unauthorizedUser.getUsername();
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.POLICY_WAIVER_REQUEST_PATH);
  }

  @Test
  void testAddPolicyWaiverRequestByPolicyViolationId_Application() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    Application app = ctx.tempEntity().newApplication(org.getId());
    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, "testScanId");
    PolicyViolation policyViolation = ctx.tempEntity().newPolicyViolation(policyEvaluation, policy);

    testAddPolicyWaiverRequestByPolicyViolationId(app, policyViolation);
  }

  @Test
  void testAddPolicyWaiverRequestByPolicyViolationId_Organization() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    Application app = ctx.tempEntity().newApplication(org.getId());
    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, "testScanId");
    PolicyViolation policyViolation = ctx.tempEntity().newPolicyViolation(policyEvaluation, policy);

    testAddPolicyWaiverRequestByPolicyViolationId(org, policyViolation);
  }

  @Test
  void testAddPolicyWaiverRequestByPolicyViolationId_Application_Unauthorized() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    Application app = ctx.tempEntity().newApplication(org.getId());
    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, "testScanId");
    PolicyViolation policyViolation = ctx.tempEntity().newPolicyViolation(policyEvaluation, policy);

    testAddPolicyWaiverRequestByPolicyViolationId_Unauthorized(app, policyViolation);
  }

  @Test
  void testAddPolicyWaiverRequestByPolicyViolationId_Organization_Unauthorized() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    Application app = ctx.tempEntity().newApplication(org.getId());
    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, "testScanId");
    PolicyViolation policyViolation = ctx.tempEntity().newPolicyViolation(policyEvaluation, policy);

    testAddPolicyWaiverRequestByPolicyViolationId_Unauthorized(org, policyViolation);
  }

  @Test
  void testAddPolicyWaiverRequestByPolicyViolationId_Repository() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity().newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    testAddPolicyWaiverRequestByPolicyViolationId(repository, proxyRepositoryPolicyViolation);
  }

  @Test
  void testAddPolicyWaiverRequestByPolicyViolationId_Repository_Unauthorized() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity().newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    testAddPolicyWaiverRequestByPolicyViolationId_Unauthorized(repository, proxyRepositoryPolicyViolation);
  }

  @Test
  void testAddPolicyWaiverRequestByPolicyViolationId_RepositoryManager() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager);
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity().newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    testAddPolicyWaiverRequestByPolicyViolationId(repositoryManager, proxyRepositoryPolicyViolation);
  }

  @Test
  void testAddPolicyWaiverRequestByPolicyViolationId_RepositoryManager_Unauthorized() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager);
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity().newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    testAddPolicyWaiverRequestByPolicyViolationId_Unauthorized(repositoryManager, proxyRepositoryPolicyViolation);
  }

  @Test
  void testAddPolicyWaiverRequestByPolicyViolationId_RepositoryContainer() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    policy = ctx.tempEntity().newPolicy(Organization.ROOT_ORGANIZATION_ID);
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity().newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    testAddPolicyWaiverRequestByPolicyViolationId(RepositoryContainer.SINGLETON, proxyRepositoryPolicyViolation);
  }

  @Test
  void testAddPolicyWaiverRequestByPolicyViolationId_RepositoryContainer_Unauthorized() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    policy = ctx.tempEntity().newPolicy(Organization.ROOT_ORGANIZATION_ID);
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity().newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    testAddPolicyWaiverRequestByPolicyViolationId_Unauthorized(RepositoryContainer.SINGLETON,
        proxyRepositoryPolicyViolation);
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
    ctx.assertResponseStatus(200, response);

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
    ctx.assertResponseStatus(200, response);

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
    ctx.assertResponseStatus(403, response);

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
    ctx.assertResponseStatus(403, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER_REQUEST, "unauthorized");
    assertOwnerData(auditDTO, owner);
  }

  @Test
  void testReviewPolicyWaiverRequest_Application() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    Application app = ctx.tempEntity().newApplication(org.getId());
    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, "testScanId");
    PolicyViolation policyViolation = ctx.tempEntity().newPolicyViolation(policyEvaluation, policy);

    testReviewPolicyWaiverRequest(app, policyViolation);
  }

  @Test
  void testReviewPolicyWaiverRequest_Application_Unauthorized() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    Application app = ctx.tempEntity().newApplication(org.getId());
    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, "testScanId");
    PolicyViolation policyViolation = ctx.tempEntity().newPolicyViolation(policyEvaluation, policy);

    testReviewPolicyWaiverRequest_Unauthorized(app, policyViolation);
  }

  @Test
  void testReviewPolicyWaiverRequest_Organization() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    Application app = ctx.tempEntity().newApplication(org.getId());
    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, "testScanId");
    PolicyViolation policyViolation = ctx.tempEntity().newPolicyViolation(policyEvaluation, policy);

    testReviewPolicyWaiverRequest(org, policyViolation);
  }

  @Test
  void testReviewPolicyWaiverRequest_Organization_Unauthorized() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    Application app = ctx.tempEntity().newApplication(org.getId());
    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, "testScanId");
    PolicyViolation policyViolation = ctx.tempEntity().newPolicyViolation(policyEvaluation, policy);

    testReviewPolicyWaiverRequest_Unauthorized(org, policyViolation);
  }

  @Test
  void testReviewPolicyWaiverRequest_Repository() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity().newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());
    ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    policyWaiverRequestOptionsDTO.comment = "waiver comment";

    testReviewPolicyWaiverRequest(repository, proxyRepositoryPolicyViolation);
  }

  @Test
  void testReviewPolicyWaiverRequest_Repository_Unauthorized() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity().newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    testReviewPolicyWaiverRequest_Unauthorized(repository, proxyRepositoryPolicyViolation);
  }

  @Test
  void testReviewPolicyWaiverRequest_RepositoryManager() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager);
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity().newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());
    ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    policyWaiverRequestOptionsDTO.comment = "waiver comment";

    testReviewPolicyWaiverRequest(repositoryManager, proxyRepositoryPolicyViolation);
  }

  @Test
  void testReviewPolicyWaiverRequest_RepositoryManager_Unauthorized() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager);
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity().newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    testReviewPolicyWaiverRequest_Unauthorized(repositoryManager, proxyRepositoryPolicyViolation);
  }

  @Test
  void testReviewPolicyWaiverRequest_RepositoryContainer() throws Exception {
    // Container-image waiver requests are parked under REPOSITORY_CONTAINER_ID but must resolve
    // to a real container-image application on approval — applyContainerImageWaivers validates
    // the underlying app has a docker-proxy repo link and active fail-stage violations.
    Application containerImageApp = newContainerImageApplication();
    policy = ctx.tempEntity().newPolicy(Organization.ROOT_ORGANIZATION_ID);
    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(containerImageApp.getId(), ProxyStageType.ID, "auditScanIdContainer");
    PolicyViolation policyViolation = ctx.tempEntity()
        .newPolicyViolation(policyEvaluation, policy, policy.getThreatLevel(),
            PolicyThreatCategory.SECURITY, "g1", "a1", "v1", "hashContainerAudit", FailActionType.ID);

    // Seed a container-image waiver request directly, as would be produced by
    // addContainerImagePolicyWaiverRequest. The public add endpoint requires the violation to
    // walk up to the caller's scope; container-image apps live under an organization (not the
    // repository container hierarchy), so we bypass the add here and exercise only the review.
    PolicyWaiverRequest policyWaiverRequest = ctx.tempEntity()
        .newPolicyWaiverRequest(
            new PolicyWaiverRequest().setOwnerId(RepositoryContainer.REPOSITORY_CONTAINER_ID)
                .setPolicyId(policy.getId())
                .setPolicyViolationId(policyViolation.getId())
                .setHash(policyViolation.getHash())
                .setConstraintFacts(policyViolation.getConstraintFacts())
                .setComponentMatchStrategy(ComponentMatcherStrategyForWaiver.EXACT_COMPONENT)
                .setComment("waiver comment")
                .setStatus(PolicyWaiverRequestStatus.REQUESTED));

    testReviewSeededPolicyWaiverRequest(RepositoryContainer.SINGLETON, policyWaiverRequest.getId());
  }

  @Test
  void testReviewPolicyWaiverRequest_RepositoryContainer_Unauthorized() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    policy = ctx.tempEntity().newPolicy(Organization.ROOT_ORGANIZATION_ID);
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity().newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    testReviewPolicyWaiverRequest_Unauthorized(RepositoryContainer.SINGLETON, proxyRepositoryPolicyViolation);
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
    ctx.assertResponseStatus(200, response);
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
    ctx.assertResponseStatus(200, response);

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
    ctx.assertResponseStatus(200, response);
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
    // Depending on the owner type, an unauthorized principal may either be rejected by the
    // authorization check (403) or have the waiver request filtered out of their scoped view (404).
    int status = response.getStatusCode();
    assertThat(status).as(
        "URI:" + response.getUrl() + ", StatusText:" + response.getStatusText() + ", ResponseBody:" +
            response.getBodyText())
        .isIn(403, 404);

    String expectedError = status == 404 ? "not-found" : "unauthorized";
    AuditDTO auditDTO = assertAuditLog(AuditEvent.REVIEW_WAIVER_REQUEST, expectedError, getUnauthorizedUsername());
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

  @Test
  void testUpdatePolicyWaiverRequest_Application() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    Application app = ctx.tempEntity().newApplication(org.getId());
    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, "testScanId");
    PolicyViolation policyViolation = ctx.tempEntity().newPolicyViolation(policyEvaluation, policy);

    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(OwnerType.APPLICATION, app.getId(), policyViolation.getId())
            .body(new ApiPolicyWaiverRequestOptionsDTO("waiver comment",
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, null, null, false), MediaType.APPLICATION_JSON)
            .post();
    ctx.assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);

    testUpdatePolicyWaiverRequest(app, apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  void testUpdatePolicyWaiverRequest_Application_Unauthorized() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    Application app = ctx.tempEntity().newApplication(org.getId());
    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, "testScanId");
    PolicyViolation policyViolation = ctx.tempEntity().newPolicyViolation(policyEvaluation, policy);

    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(OwnerType.APPLICATION, app.getId(), policyViolation.getId())
            .body(new ApiPolicyWaiverRequestOptionsDTO("waiver comment",
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, null, null, false), MediaType.APPLICATION_JSON)
            .post();
    ctx.assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);

    testUpdatePolicyWaiverRequest_Unauthorized(app, apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  void testUpdatePolicyWaiverRequest_Organization() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    Application app = ctx.tempEntity().newApplication(org.getId());
    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, "testScanId");
    PolicyViolation policyViolation = ctx.tempEntity().newPolicyViolation(policyEvaluation, policy);

    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(OwnerType.APPLICATION, app.getId(), policyViolation.getId())
            .body(new ApiPolicyWaiverRequestOptionsDTO("waiver comment",
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, null, null, false), MediaType.APPLICATION_JSON)
            .post();
    ctx.assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);

    testUpdatePolicyWaiverRequest(org, apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  void testUpdatePolicyWaiverRequest_Organization_Unauthorized() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    Application app = ctx.tempEntity().newApplication(org.getId());
    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, "testScanId");
    PolicyViolation policyViolation = ctx.tempEntity().newPolicyViolation(policyEvaluation, policy);

    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(OwnerType.APPLICATION, app.getId(), policyViolation.getId())
            .body(new ApiPolicyWaiverRequestOptionsDTO("waiver comment",
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, null, null, false), MediaType.APPLICATION_JSON)
            .post();
    ctx.assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);

    testUpdatePolicyWaiverRequest_Unauthorized(org, apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  void testUpdatePolicyWaiverRequest_Repository() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity().newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(OwnerType.REPOSITORY, repository.getId(), proxyRepositoryPolicyViolation.getId())
            .body(new ApiPolicyWaiverRequestOptionsDTO("waiver comment",
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, null, null, false), MediaType.APPLICATION_JSON)
            .post();
    ctx.assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);

    testUpdatePolicyWaiverRequest(repository, apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  void testUpdatePolicyWaiverRequest_Repository_Unauthorized() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity().newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(OwnerType.REPOSITORY, repository.getId(), proxyRepositoryPolicyViolation.getId())
            .body(new ApiPolicyWaiverRequestOptionsDTO("waiver comment",
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, null, null, false), MediaType.APPLICATION_JSON)
            .post();
    ctx.assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);

    testUpdatePolicyWaiverRequest_Unauthorized(repository, apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  void testUpdatePolicyWaiverRequest_RepositoryManager_Unauthorized() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager);
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity().newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(OwnerType.REPOSITORY, repository.getId(), proxyRepositoryPolicyViolation.getId())
            .body(new ApiPolicyWaiverRequestOptionsDTO("waiver comment",
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, null, null, false), MediaType.APPLICATION_JSON)
            .post();
    ctx.assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);

    testUpdatePolicyWaiverRequest_Unauthorized(repositoryManager, apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  void testUpdatePolicyWaiverRequest_RepositoryManager() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager);
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity().newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(OwnerType.REPOSITORY, repository.getId(), proxyRepositoryPolicyViolation.getId())
            .body(new ApiPolicyWaiverRequestOptionsDTO("waiver comment",
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, null, null, false), MediaType.APPLICATION_JSON)
            .post();
    ctx.assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);

    testUpdatePolicyWaiverRequest(repositoryManager, apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  void testUpdatePolicyWaiverRequest_RepositoryContainer() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager);
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity().newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(OwnerType.REPOSITORY, repository.getId(), proxyRepositoryPolicyViolation.getId())
            .body(new ApiPolicyWaiverRequestOptionsDTO("waiver comment",
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, null, null, false), MediaType.APPLICATION_JSON)
            .post();
    ctx.assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);

    testUpdatePolicyWaiverRequest(RepositoryContainer.SINGLETON, apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  void testUpdatePolicyWaiverRequest_RepositoryContainer_Unauthorized() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager);
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        ctx.tempEntity().newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    HttpResponse response =
        restRequest().path(POLICY_VIOLATION_ID_PATH)
            .parameter(OwnerType.REPOSITORY, repository.getId(), proxyRepositoryPolicyViolation.getId())
            .body(new ApiPolicyWaiverRequestOptionsDTO("waiver comment",
                ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, null, null, false), MediaType.APPLICATION_JSON)
            .post();
    ctx.assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);

    testUpdatePolicyWaiverRequest_Unauthorized(RepositoryContainer.SINGLETON,
        apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  // CLM-41741: requester-only withdraw of pending waiver requests.

  @Test
  void testWithdrawPolicyWaiverRequest_Application() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    Application app = ctx.tempEntity().newApplication(org.getId());
    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanIdWAudit");
    PolicyViolation policyViolation = ctx.tempEntity().newPolicyViolation(policyEvaluation, policy);

    testWithdrawPolicyWaiverRequest(app, policyViolation);
  }

  @Test
  void testWithdrawPolicyWaiverRequest_Organization() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    Application app = ctx.tempEntity().newApplication(org.getId());
    PolicyEvaluation policyEvaluation =
        ctx.tempEntity().newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanIdWAudit");
    PolicyViolation policyViolation = ctx.tempEntity().newPolicyViolation(policyEvaluation, policy);

    testWithdrawPolicyWaiverRequest(org, policyViolation);
  }

  private void testWithdrawPolicyWaiverRequest(Owner owner, AbstractPolicyViolation policyViolation) throws Exception {
    // Submit a request first so we have something to withdraw.
    HttpResponse submit = restRequest().path(POLICY_VIOLATION_ID_PATH)
        .parameter(owner.getType(), owner.getId(), policyViolation.getId())
        .body(new ApiPolicyWaiverRequestOptionsDTO("waiver comment",
            ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, null, null, false), MediaType.APPLICATION_JSON)
        .post();
    ctx.assertResponseStatus(200, submit);
    ApiPolicyWaiverRequestDTO created = submit.getBody(ApiPolicyWaiverRequestDTO.class);

    // Capture the entity BEFORE delete so we can assert against its fields after withdrawal
    // (the row is hard-deleted and assertPolicyWaiverRequestData(auditDTO) without a 2nd arg
    // would otherwise try to look it up by id and fail).
    PolicyWaiverRequest snapshot = policyWaiverRequestDAO.getByIdNotNull(created.policyWaiverRequestId);

    HttpResponse withdraw = restRequest().path(POLICY_WAIVER_REQUEST_ID_PATH)
        .parameter(owner.getType(), owner.getId(), created.policyWaiverRequestId)
        .delete();
    ctx.assertResponseStatus(204, withdraw);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.WITHDRAW_WAIVER_REQUEST, null);
    assertPolicyWaiverRequestData(auditDTO, snapshot);
    assertOwnerData(auditDTO, owner);
  }

  /**
   * Reviews a pre-seeded policy waiver request (skipping the public add endpoint) and asserts
   * the resulting audit trail. Used for the REPOSITORY_CONTAINER path, where the add endpoint
   * requires the violation to walk up to the caller's scope but the container-image approval
   * flow requires a real container-image application as the underlying violation owner.
   * <p>
   * The container-image approval creates one per-component waiver plus one image-level summary
   * waiver, so two CREATE_WAIVER sub-events are emitted.
   * <p>
   * The REVIEW_WAIVER_REQUEST audit runs under the request's addressing owner (REPOSITORY_CONTAINER
   * for container waivers). The two CREATE_WAIVER sub-events emitted from inside
   * {@code applyContainerImageWaivers} carry the policy-waiver payload (policyId, policyWaiverId,
   * componentHash, isForContainerImage flags) but not the outer owner-typed audit fields; asserting
   * the payload is sufficient here.
   */
  private void testReviewSeededPolicyWaiverRequest(
      Owner reviewOwner,
      String policyWaiverRequestId) throws Exception
  {
    ApiPolicyWaiverRequestReviewDTO apiPolicyWaiverRequestReviewDTO = new ApiPolicyWaiverRequestReviewDTO();
    apiPolicyWaiverRequestReviewDTO.comment = "updated waiver comment";
    apiPolicyWaiverRequestReviewDTO.expiryTime = DateUtils.addDays(new Date(), 2);
    apiPolicyWaiverRequestReviewDTO.matcherStrategy = ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
    apiPolicyWaiverRequestReviewDTO.status = PolicyWaiverRequestStatus.APPROVED.name();
    HttpResponse response = restRequest().path(POLICY_WAIVER_REQUEST_REVIEW_PATH)
        .parameter(reviewOwner.getType(), reviewOwner.getId(), policyWaiverRequestId)
        .body(apiPolicyWaiverRequestReviewDTO, MediaType.APPLICATION_JSON)
        .post();
    ctx.assertResponseStatus(200, response);

    AuditDTO reviewAudit = assertAuditLog(AuditEvent.REVIEW_WAIVER_REQUEST, null);
    assertPolicyWaiverRequestData(reviewAudit);
    assertOwnerData(reviewAudit, reviewOwner);

    // Two CREATE_WAIVER sub-events: one per-component waiver and one image-level summary.
    List<AuditDTO> createAudits = assertAuditLogs(AuditEvent.CREATE_WAIVER, 2, null);
    for (AuditDTO auditDTO : createAudits) {
      assertPolicyWaiverData(auditDTO);
    }
  }

  /**
   * Creates an Application wired up like a real container image: its Organization has a
   * relatedRepositoryId pointing to a docker-proxy repository, matching what
   * {@link com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverService} validates.
   */
  private Application newContainerImageApplication() {
    Organization containerOrg = ctx.tempEntity().newOrganization();
    Repository repository = ctx.tempEntity()
        .newRepository(ctx.tempEntity().newRepositoryManager(),
            "docker-repo-" + containerOrg.getId(), RepositoryType.proxy, "docker");
    repository.setRelatedOrganizationId(containerOrg.getId());
    repositoryDAO.update(repository);
    containerOrg.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(containerOrg);
    return ctx.tempEntity().newApplication(containerOrg.getId());
  }

  private static final class TestLogOutput
      extends LogOutput
  {
    TestLogOutput(String... loggerNames) {
      super(loggerNames);
    }

    void tearDown() {
      after();
    }
  }
}
