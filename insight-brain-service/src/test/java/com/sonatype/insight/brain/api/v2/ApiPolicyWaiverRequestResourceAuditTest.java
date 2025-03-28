/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.stream.Collectors;

import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestOptionsDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.policy.ConstraintFactDTO;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverRequestResource.BY_POLICY_VIOLATION_ID_PATH;

public class ApiPolicyWaiverRequestResourceAuditTest
    extends AbstractAuditTest
{
  private PolicyWaiverRequestDAO policyWaiverRequestDAO;

  private Policy policy;

  @Before
  public void setUpPolicyViolation() {
    policyWaiverRequestDAO = lookup(PolicyWaiverRequestDAO.class);
    policy = tempEntity.newPolicy();
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_Application() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "testScanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    policyWaiverRequestOptionsDTO.comment = "waiver comment";
    restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), policyViolation.getId())
        .body(policyWaiverRequestOptionsDTO, MediaType.APPLICATION_JSON).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER_REQUEST, null);
    assertPolicyWaiverRequestData(auditDTO);
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_Organization() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "testScanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    policyWaiverRequestOptionsDTO.comment = "waiver comment";
    restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, org.getId(), policyViolation.getId())
        .body(policyWaiverRequestOptionsDTO, MediaType.APPLICATION_JSON).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER_REQUEST, null);
    assertPolicyWaiverRequestData(auditDTO);
    assertOrganizationData(auditDTO, org);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_Application_Unauthorized() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "testScanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    policyWaiverRequestOptionsDTO.comment = "waiver comment";
    restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), policyViolation.getId()).with(unauthorizedUser())
        .body(policyWaiverRequestOptionsDTO, MediaType.APPLICATION_JSON).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER_REQUEST, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_Organization_Unauthorized() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "testScanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    policyWaiverRequestOptionsDTO.comment = "waiver comment";
    restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, org.getId(), policyViolation.getId()).with(unauthorizedUser())
        .body(policyWaiverRequestOptionsDTO, MediaType.APPLICATION_JSON).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER_REQUEST, "unauthorized");
    assertOrganizationData(auditDTO, org);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_Repository() throws Exception {
    Repository repository = tempEntity.newRepository();
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());
    ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    policyWaiverRequestOptionsDTO.comment = "waiver comment";

    restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.REPOSITORY, repository.getId(), repositoryPolicyViolation.getId())
        .body(policyWaiverRequestOptionsDTO, MediaType.APPLICATION_JSON).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER_REQUEST, null);
    assertPolicyWaiverRequestData(auditDTO);
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_Repository_Unauthorized() throws Exception {
    Repository repository = tempEntity.newRepository();
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());
    ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    policyWaiverRequestOptionsDTO.comment = "waiver comment";
    restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.REPOSITORY, repository.getId(), repositoryPolicyViolation.getId()).with(unauthorizedUser())
        .body(policyWaiverRequestOptionsDTO, MediaType.APPLICATION_JSON).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER_REQUEST, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_RepositoryManager() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager);
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());
    ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    policyWaiverRequestOptionsDTO.comment = "waiver comment";

    restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(), repositoryPolicyViolation.getId())
        .body(policyWaiverRequestOptionsDTO, MediaType.APPLICATION_JSON).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER_REQUEST, null);
    assertPolicyWaiverRequestData(auditDTO);
    assertRepositoryManagerData(auditDTO, repositoryManager);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_RepositoryManager_Unauthorized() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager);
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());
    ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    policyWaiverRequestOptionsDTO.comment = "waiver comment";
    restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(), repositoryPolicyViolation.getId())
        .with(unauthorizedUser()).body(policyWaiverRequestOptionsDTO, MediaType.APPLICATION_JSON).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER_REQUEST, "unauthorized");
    assertRepositoryManagerData(auditDTO, repositoryManager);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_RepositoryContainer() throws Exception {
    Repository repository = tempEntity.newRepository();
    policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());
    ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    policyWaiverRequestOptionsDTO.comment = "waiver comment";

    restRequest()
        .path(BY_POLICY_VIOLATION_ID_PATH).parameter(OwnerType.REPOSITORY_CONTAINER,
            RepositoryContainer.REPOSITORY_CONTAINER_ID, repositoryPolicyViolation.getId())
        .body(policyWaiverRequestOptionsDTO, MediaType.APPLICATION_JSON).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER_REQUEST, null);
    assertPolicyWaiverRequestData(auditDTO);
    assertRepositoryContainerData(auditDTO);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_RepositoryContainer_Unauthorized() throws Exception {
    Repository repository = tempEntity.newRepository();
    policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());
    ApiPolicyWaiverRequestOptionsDTO policyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    policyWaiverRequestOptionsDTO.comment = "waiver comment";
    restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID,
            repositoryPolicyViolation.getId())
        .with(unauthorizedUser()).body(policyWaiverRequestOptionsDTO, MediaType.APPLICATION_JSON).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WAIVER_REQUEST, "unauthorized");
    assertRepositoryContainerData(auditDTO);
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

  @Override
  public HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.POLICY_WAIVER_REQUEST_PATH);
  }
}
