/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.RepositoryResultsForImageContainerDto;
import com.sonatype.insight.brain.api.v2.dto.RepositoryResultsForImageContainerRequestDto;
import com.sonatype.insight.brain.api.v2.dto.RepositoryResultsForImageContainerRequestDto.ViolationStateFilter;
import com.sonatype.insight.brain.api.v2.dto.RepositoryResultsForImageContainerResponseDto;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.LastPolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsForImageContainerFilter.SortField;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsForImageContainerFilter.SortField.SortableField;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqPostgresTest
class IqPostgresApiRepositoryResultsForImageContainerResourceTest
{
  private IqTestContext ctx;

  private RepositoryDAO repositoryDAO;

  private OrganizationDAO organizationDAO;

  private LastPolicyEvaluationDAO lastPolicyEvaluationDAO;

  private PolicyViolationDAO policyViolationDAO;

  @BeforeEach
  void setup() {
    repositoryDAO = ctx.lookup(RepositoryDAO.class);
    lastPolicyEvaluationDAO = ctx.lookup(LastPolicyEvaluationDAO.class);
    policyViolationDAO = ctx.lookup(PolicyViolationDAO.class);
    organizationDAO = ctx.lookup(OrganizationDAO.class);
  }

  @Test
  void testGetDetails() throws Exception {
    final RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    final Repository repository = ctx.tempEntity().newRepository(repositoryManager, "publicId");

    // Repository organization
    final Organization organization = ctx.tempEntity().newOrganization("org1");
    organization.setRelatedRepositoryId(repository.getId());
    repository.setRelatedOrganizationId(organization.getId());
    repositoryDAO.update(repository);
    organizationDAO.update(organization);

    // Container Image applications
    Application application1 = ctx.tempEntity().newApplication("app1", "appPublicId1", organization.getId());
    Application application2 = ctx.tempEntity().newApplication("app2", "appPublicId2", organization.getId());

    // policy evaluation
    PolicyEvaluation policyEvaluation1 = ctx.tempEntity().newPolicyEvaluation(application1.getId(), "proxy", "scanId1");
    PolicyEvaluation policyEvaluation2 = ctx.tempEntity().newPolicyEvaluation(application2.getId(), "proxy", "scanId2");

    // last policy evaluation
    lastPolicyEvaluationDAO.getByOwnerIdAndStageTypeId(application1.getId(), "proxy");
    lastPolicyEvaluationDAO.getByOwnerIdAndStageTypeId(application2.getId(), "proxy");

    // policy for policy violation
    Policy policy1 = ctx.tempEntity().newPolicy(application1.getId(), "policy1");
    Policy policy2 = ctx.tempEntity().newPolicy(application1.getId(), "policy2");
    Policy policy3 = ctx.tempEntity().newPolicy(application1.getId(), "policy3");
    Policy policy4 = ctx.tempEntity().newPolicy(application1.getId(), "policy4");
    Policy policy5 = ctx.tempEntity().newPolicy(application1.getId(), "policy5");
    Policy policy6 = ctx.tempEntity().newPolicy(application1.getId(), "policy6");

    // create policy violations
    PolicyViolation policyViolation1 = ctx.tempEntity().newPolicyViolation(policyEvaluation1, policy1);
    PolicyViolation policyViolation2 = ctx.tempEntity().newPolicyViolation(policyEvaluation1, policy2);
    PolicyViolation policyViolation3 = ctx.tempEntity().newPolicyViolation(policyEvaluation1, policy3);
    PolicyViolation policyViolation4 = ctx.tempEntity().newPolicyViolation(policyEvaluation1, policy4);

    PolicyViolation policyViolation5 = ctx.tempEntity().newPolicyViolation(policyEvaluation2, policy5);
    PolicyViolation policyViolation6 = ctx.tempEntity().newPolicyViolation(policyEvaluation2, policy6);

    policyViolation1.setThreatLevel(10);
    policyViolation2.setThreatLevel(8);
    policyViolation3.setThreatLevel(10);
    policyViolation4.setThreatLevel(5);
    policyViolation5.setThreatLevel(10);
    policyViolation6.setThreatLevel(2);

    policyViolationDAO.update(policyViolation1);
    policyViolationDAO.update(policyViolation2);
    policyViolationDAO.update(policyViolation3);
    policyViolationDAO.update(policyViolation4);
    policyViolationDAO.update(policyViolation5);
    policyViolationDAO.update(policyViolation6);

    SortField sortField1 = new SortField();
    sortField1.sortableField = SortableField.POLICY_THREAT_LEVEL;
    sortField1.sortPriority = 1;
    sortField1.asc = true;

    SortField sortField2 = new SortField();
    sortField2.sortableField = SortableField.OBJECT_NAME;
    sortField2.sortPriority = 2;
    sortField2.asc = true;

    RepositoryResultsForImageContainerRequestDto detailsRequest = new RepositoryResultsForImageContainerRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
    detailsRequest.violationStateFilters = ImmutableList.of(ViolationStateFilter.VIOLATION_STATE_ALL);
    detailsRequest.sortFields = Arrays.asList(sortField1, sortField2);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.REPOSITORY_RESULTS_FOR_IMAGE_CONTAINER_PATH,
            ApiRepositoryResultsForImageContainerResource.IMAGE_CONTAINER_PATH)
        .parameter(OwnerType.REPOSITORY, repository.getId())
        .body(detailsRequest)
        .post();

    ctx.assertResponseStatus(200, response);

    RepositoryResultsForImageContainerResponseDto result =
        response.getBody(RepositoryResultsForImageContainerResponseDto.class);
    List<RepositoryResultsForImageContainerDto> repositoryResultsDetails = result.repositoryResultsDetails;

    assertThat(repositoryResultsDetails).isNotNull();
    assertThat(repositoryResultsDetails).hasSize(6);
    assertThat(repositoryResultsDetails.get(0).threatLevel).isEqualTo(2);
    assertThat(repositoryResultsDetails.get(1).threatLevel).isEqualTo(5);
    assertThat(repositoryResultsDetails.get(2).threatLevel).isEqualTo(8);
    assertThat(repositoryResultsDetails.get(3).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(4).threatLevel).isEqualTo(10);
    assertThat(repositoryResultsDetails.get(5).threatLevel).isEqualTo(10);

    assertThat(repositoryResultsDetails.get(0).objectName).isEqualTo("app2");
    assertThat(repositoryResultsDetails.get(1).objectName).isEqualTo("app1");
    assertThat(repositoryResultsDetails.get(2).objectName).isEqualTo("app1");
    assertThat(repositoryResultsDetails.get(3).objectName).isEqualTo("app1");
    assertThat(repositoryResultsDetails.get(4).objectName).isEqualTo("app1");
    assertThat(repositoryResultsDetails.get(5).objectName).isEqualTo("app2");

    assertThat(repositoryResultsDetails.get(0).applicationPublicId).isEqualTo("appPublicId2");
    assertThat(repositoryResultsDetails.get(1).applicationPublicId).isEqualTo("appPublicId1");
    assertThat(repositoryResultsDetails.get(2).applicationPublicId).isEqualTo("appPublicId1");
    assertThat(repositoryResultsDetails.get(3).applicationPublicId).isEqualTo("appPublicId1");
    assertThat(repositoryResultsDetails.get(4).applicationPublicId).isEqualTo("appPublicId1");
    assertThat(repositoryResultsDetails.get(5).applicationPublicId).isEqualTo("appPublicId2");

    assertThat(repositoryResultsDetails.get(0).policyName).isEqualTo("policy6");
    assertThat(repositoryResultsDetails.get(1).policyName).isEqualTo("policy4");
    assertThat(repositoryResultsDetails.get(2).policyName).isEqualTo("policy2");
    assertThat(repositoryResultsDetails.get(3).policyName).isEqualTo("policy1");
    assertThat(repositoryResultsDetails.get(4).policyName).isEqualTo("policy3");
    assertThat(repositoryResultsDetails.get(5).policyName).isEqualTo("policy5");

    assertThat(repositoryResultsDetails.get(0).scanId).isEqualTo("scanId2");
    assertThat(repositoryResultsDetails.get(1).scanId).isEqualTo("scanId1");
    assertThat(repositoryResultsDetails.get(2).scanId).isEqualTo("scanId1");
    assertThat(repositoryResultsDetails.get(3).scanId).isEqualTo("scanId1");
    assertThat(repositoryResultsDetails.get(4).scanId).isEqualTo("scanId1");
    assertThat(repositoryResultsDetails.get(5).scanId).isEqualTo("scanId2");
  }
}
