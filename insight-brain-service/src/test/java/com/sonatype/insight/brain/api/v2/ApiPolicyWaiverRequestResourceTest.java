/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Date;

import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestOptionsDTO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverResource.BY_POLICY_VIOLATION_ID_PATH;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiPolicyWaiverRequestResourceTest
    extends AbstractResourceTest
{
  private RepositoryManagerDAO repositoryManagerDAO;

  private PolicyWaiverRequestDAO policyWaiverRequestDAO;

  @Before
  public void setUp() {
    repositoryManagerDAO = lookup(RepositoryManagerDAO.class);
    policyWaiverRequestDAO = lookup(PolicyWaiverRequestDAO.class);
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
    PolicyViolation policyViolation =
        tempEntity.newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");

    Date expiryDate = DateUtils.addDays(new Date(), 1);
    ApiPolicyWaiverRequestOptionsDTO apiPolicyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    apiPolicyWaiverRequestOptionsDTO.comment = "waiver comment";
    apiPolicyWaiverRequestOptionsDTO.expiryTime = expiryDate;
    apiPolicyWaiverRequestOptionsDTO.matcherStrategy = ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
    HttpResponse response = restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), policyViolation.getId())
        .body(apiPolicyWaiverRequestOptionsDTO, MediaType.APPLICATION_JSON).post();

    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);
    assertPolicyWaiverRequestDTO(app.getId(), policy, policyViolation, "waiver comment", policyViolation.getHash(),
        expiryDate, ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, apiPolicyWaiverRequestDTO);
    assertPolicyWaiverRequest(app.getId(), policy, policyViolation, "waiver comment", policyViolation.getHash(),
        expiryDate, ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_Organization() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Policy policy = tempEntity.newPolicy(app);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation policyViolation =
        tempEntity.newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");

    Date expiryDate = DateUtils.addDays(new Date(), 1);
    ApiPolicyWaiverRequestOptionsDTO apiPolicyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    apiPolicyWaiverRequestOptionsDTO.comment = "waiver comment";
    apiPolicyWaiverRequestOptionsDTO.expiryTime = expiryDate;
    apiPolicyWaiverRequestOptionsDTO.matcherStrategy = ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
    HttpResponse response = restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, org.getId(), policyViolation.getId())
        .body(apiPolicyWaiverRequestOptionsDTO, MediaType.APPLICATION_JSON).post();

    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);
    assertPolicyWaiverRequestDTO(org.getId(), policy, policyViolation, "waiver comment", policyViolation.getHash(),
        expiryDate, ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, apiPolicyWaiverRequestDTO);
    assertPolicyWaiverRequest(org.getId(), policy, policyViolation, "waiver comment", policyViolation.getHash(),
        expiryDate, ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_NoRequestBody() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    PolicyViolation policyViolation =
        tempEntity.newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");

    HttpResponse response = restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), policyViolation.getId()).post();

    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);
    assertPolicyWaiverRequestDTO(app.getId(), policy, policyViolation, null, policyViolation.getHash(), null,
        ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, apiPolicyWaiverRequestDTO);
    assertPolicyWaiverRequest(app.getId(), policy, policyViolation, null, policyViolation.getHash(), null,
        ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_Repository() throws Exception {
    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);

    RepositoryPolicyViolation policyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    Date expiryDate = DateUtils.addDays(new Date(), 1);
    ApiPolicyWaiverRequestOptionsDTO apiPolicyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    apiPolicyWaiverRequestOptionsDTO.comment = "waiver comment";
    apiPolicyWaiverRequestOptionsDTO.expiryTime = expiryDate;
    apiPolicyWaiverRequestOptionsDTO.matcherStrategy = ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
    HttpResponse response = restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.REPOSITORY, repository.getId(), policyViolation.getId())
        .body(apiPolicyWaiverRequestOptionsDTO, MediaType.APPLICATION_JSON).post();

    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);
    assertPolicyWaiverRequestDTO(repository.getId(), policy, policyViolation, "waiver comment",
        policyViolation.getHash(), expiryDate, ComponentMatcherStrategyForWaiver.EXACT_COMPONENT,
        apiPolicyWaiverRequestDTO);
    assertPolicyWaiverRequest(repository.getId(), policy, policyViolation, "waiver comment", policyViolation.getHash(),
        expiryDate, ComponentMatcherStrategyForWaiver.EXACT_COMPONENT, apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_RepositoryManager() throws Exception {
    Repository repository = tempEntity.newRepository();
    RepositoryManager repositoryManager = repositoryManagerDAO.getById(repository.getRepositoryManagerId());
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);

    RepositoryPolicyViolation policyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    Date expiryDate = DateUtils.addDays(new Date(), 1);
    ApiPolicyWaiverRequestOptionsDTO apiPolicyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    apiPolicyWaiverRequestOptionsDTO.comment = "waiver comment";
    apiPolicyWaiverRequestOptionsDTO.expiryTime = expiryDate;
    apiPolicyWaiverRequestOptionsDTO.matcherStrategy = ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
    HttpResponse response = restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(), policyViolation.getId())
        .body(apiPolicyWaiverRequestOptionsDTO, MediaType.APPLICATION_JSON).post();

    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);
    assertPolicyWaiverRequestDTO(repositoryManager.getId(), policy, policyViolation, "waiver comment",
        policyViolation.getHash(), expiryDate, ComponentMatcherStrategyForWaiver.EXACT_COMPONENT,
        apiPolicyWaiverRequestDTO);
    assertPolicyWaiverRequest(repositoryManager.getId(), policy, policyViolation, "waiver comment",
        policyViolation.getHash(), expiryDate, ComponentMatcherStrategyForWaiver.EXACT_COMPONENT,
        apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  @Test
  public void testAddPolicyWaiverRequestByPolicyViolationId_RepositoryContainer() throws Exception {
    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);

    RepositoryPolicyViolation policyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getId(), policy.getThreatLevel());

    Date expiryDate = DateUtils.addDays(new Date(), 1);
    ApiPolicyWaiverRequestOptionsDTO apiPolicyWaiverRequestOptionsDTO = new ApiPolicyWaiverRequestOptionsDTO();
    apiPolicyWaiverRequestOptionsDTO.comment = "waiver comment";
    apiPolicyWaiverRequestOptionsDTO.expiryTime = expiryDate;
    apiPolicyWaiverRequestOptionsDTO.matcherStrategy = ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
    HttpResponse response = restRequest().path(BY_POLICY_VIOLATION_ID_PATH)
        .parameter(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID, policyViolation.getId())
        .body(apiPolicyWaiverRequestOptionsDTO, MediaType.APPLICATION_JSON).post();

    assertResponseStatus(200, response);
    ApiPolicyWaiverRequestDTO apiPolicyWaiverRequestDTO = response.getBody(ApiPolicyWaiverRequestDTO.class);
    assertPolicyWaiverRequestDTO(RepositoryContainer.REPOSITORY_CONTAINER_ID, policy, policyViolation, "waiver comment",
        policyViolation.getHash(), expiryDate, ComponentMatcherStrategyForWaiver.EXACT_COMPONENT,
        apiPolicyWaiverRequestDTO);
    assertPolicyWaiverRequest(RepositoryContainer.REPOSITORY_CONTAINER_ID, policy, policyViolation, "waiver comment",
        policyViolation.getHash(), expiryDate, ComponentMatcherStrategyForWaiver.EXACT_COMPONENT,
        apiPolicyWaiverRequestDTO.policyWaiverRequestId);
  }

  private void assertPolicyWaiverRequest(
      String ownerId,
      Policy policy,
      AbstractPolicyViolation abstractPolicyViolation,
      String comment,
      String hash,
      Date expiryTime,
      ComponentMatcherStrategyForWaiver matcherStrategy,
      String policyWaiverRequestId)
  {
    PolicyWaiverRequest policyWaiverRequest = policyWaiverRequestDAO.getById(policyWaiverRequestId);
    assertThat(policyWaiverRequest.getOwnerId()).isEqualTo(ownerId);
    assertThat(policyWaiverRequest.getHash()).isEqualTo(hash);
    assertThat(policyWaiverRequest.getComment()).isEqualTo(comment);
    assertThat(policyWaiverRequest.getPolicyId()).isEqualTo(policy.getId());
    assertThat(policyWaiverRequest.getRequestTime()).isNotNull();
    assertThat(policyWaiverRequest.getExpiryTime()).isEqualTo(expiryTime);
    assertThat(policyWaiverRequest.getComponentMatchStrategy()).isEqualTo(matcherStrategy);
    assertThat(policyWaiverRequest.getConstraintFactsJson())
        .isEqualTo(abstractPolicyViolation.getConstraintFactsJson());
  }

  private void assertPolicyWaiverRequestDTO(
      String ownerId,
      Policy policy,
      AbstractPolicyViolation abstractPolicyViolation,
      String comment,
      String hash,
      Date expiryTime,
      ComponentMatcherStrategyForWaiver matcherStrategy,
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
  }
}
