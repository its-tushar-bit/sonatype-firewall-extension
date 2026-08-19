/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.container.images;

import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.repository.ContainerImageSummaryDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ContainerImageReportResourceTest
{
  private IqTestContext ctx;

  private RepositoryDAO repositoryDAO;

  private OrganizationDAO organizationDAO;

  private PolicyViolationDAO policyViolationDAO;

  @BeforeEach
  void setUp() {
    repositoryDAO = ctx.lookup(RepositoryDAO.class);
    policyViolationDAO = ctx.lookup(PolicyViolationDAO.class);
    organizationDAO = ctx.lookup(OrganizationDAO.class);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(ContainerImageReportResource.RESOURCE_PATH);
  }

  @Test
  void testGetContainerImagesSummary() throws Exception {
    Repository repository = ctx.tempEntity().newRepository("publicId");
    Organization organization = ctx.tempEntity().newOrganization("org");
    organization.setRelatedRepositoryId(repository.getId());
    repository.setRelatedOrganizationId(organization.getId());
    repository.setFormat("docker");
    repository.setQuarantineEnabled(true);
    repositoryDAO.update(repository);
    organizationDAO.update(organization);
    // Container Image applications
    Application application1 = ctx.tempEntity().newApplication("app1", "appPublicId1", organization.getId());
    Application application2 = ctx.tempEntity().newApplication("app2", "appPublicId2", organization.getId());
    ctx.tempEntity().newApplication("app3", "appPublicId3", organization.getId());

    // policy evaluation
    PolicyEvaluation policyEvaluation1 =
        ctx.tempEntity().newPolicyEvaluation(application1.getId(), "proxy", "scanId1");
    PolicyEvaluation policyEvaluation2 =
        ctx.tempEntity().newPolicyEvaluation(application2.getId(), "proxy", "scanId2");

    // policy for policy violation
    Policy policy1 = ctx.tempEntity().newPolicy(application1.getId(), "policy1");
    Policy policy2 = ctx.tempEntity().newPolicy(application1.getId(), "policy2");
    Policy policy3 = ctx.tempEntity().newPolicy(application1.getId(), "policy3");
    Policy policy4 = ctx.tempEntity().newPolicy(application1.getId(), "policy4");

    Policy policy5 = ctx.tempEntity().newPolicy(application2.getId(), "policy5");
    Policy policy6 = ctx.tempEntity().newPolicy(application2.getId(), "policy6");

    // create policy violations app 1
    PolicyViolation policyViolation1 = ctx.tempEntity().newPolicyViolation(policyEvaluation1, policy1);
    PolicyViolation policyViolation2 = ctx.tempEntity().newPolicyViolation(policyEvaluation1, policy2);
    PolicyViolation policyViolation3 = ctx.tempEntity().newPolicyViolation(policyEvaluation1, policy3);
    PolicyViolation policyViolation4 = ctx.tempEntity().newPolicyViolation(policyEvaluation1, policy4);
    policyViolation1.setThreatLevel(10);
    policyViolation2.setThreatLevel(8);
    policyViolation3.setThreatLevel(10);
    policyViolation4.setThreatLevel(5);
    policyViolation1.setActionTypeId("fail");

    // create policy violations app 1
    PolicyViolation policyViolation5 = ctx.tempEntity().newPolicyViolation(policyEvaluation2, policy5);
    PolicyViolation policyViolation6 = ctx.tempEntity().newPolicyViolation(policyEvaluation2, policy6);
    policyViolation5.setThreatLevel(10);
    policyViolation6.setThreatLevel(2);

    policyViolationDAO.update(policyViolation1);
    policyViolationDAO.update(policyViolation2);
    policyViolationDAO.update(policyViolation3);
    policyViolationDAO.update(policyViolation4);
    policyViolationDAO.update(policyViolation5);
    policyViolationDAO.update(policyViolation6);

    HttpResponse response = restRequest()
        .path(ContainerImageReportResource.SUMMARY)
        .parameter(repository.getId())
        .get();
    ctx.assertResponseStatus(200, response);
    ContainerImageSummaryDTO summary = response.getBody(ContainerImageSummaryDTO.class);
    assertThat(summary).isNotNull();
    assertThat(summary.totalContainerImageCount).isEqualTo(3);
    assertThat(summary.totalContainerImageViolationCount).isEqualTo(6);
    assertThat(summary.quarantinedContainerImageCount).isEqualTo(1);
    assertThat(summary.criticalViolationCount).isEqualTo(4);
    assertThat(summary.severeViolationCount).isEqualTo(1);
    assertThat(summary.moderateViolationCount).isEqualTo(1);
    assertThat(summary.affectedContainerImageCount).isEqualTo(2);
  }
}
