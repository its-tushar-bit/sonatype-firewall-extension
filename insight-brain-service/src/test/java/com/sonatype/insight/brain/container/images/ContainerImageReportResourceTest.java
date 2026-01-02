/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.container.images;

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
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.common.test.SlowTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class ContainerImageReportResourceTest
    extends AbstractResourceTest
{
  private RepositoryDAO repositoryDAO;

  private OrganizationDAO organizationDAO;

  private PolicyViolationDAO policyViolationDAO;

  @Before
  public void setUp() {
    repositoryDAO = lookup(RepositoryDAO.class);
    policyViolationDAO = lookup(PolicyViolationDAO.class);
    organizationDAO = lookup(OrganizationDAO.class);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(ContainerImageReportResource.RESOURCE_PATH);
  }

  @Test
  public void testGetContainerImagesSummary() throws Exception {
    Repository repository = tempEntity.newRepository("publicId");
    Organization organization = tempEntity.newOrganization("org");
    organization.setRelatedRepositoryId(repository.getId());
    repository.setRelatedOrganizationId(organization.getId());
    repository.setFormat("docker");
    repositoryDAO.update(repository);
    organizationDAO.update(organization);
    // Container Image applications
    Application application1 = tempEntity.newApplication("app1", "appPublicId1", organization.getId());
    Application application2 = tempEntity.newApplication("app2", "appPublicId2", organization.getId());
    tempEntity.newApplication("app3", "appPublicId3", organization.getId());

    //policy evaluation
    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(application1.getId(), "proxy", "scanId1");
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(application2.getId(), "proxy", "scanId2");

    //policy for policy violation
    Policy policy1 = tempEntity.newPolicy(application1.getId(), "policy1");
    Policy policy2 = tempEntity.newPolicy(application1.getId(), "policy2");
    Policy policy3 = tempEntity.newPolicy(application1.getId(), "policy3");
    Policy policy4 = tempEntity.newPolicy(application1.getId(), "policy4");

    Policy policy5 = tempEntity.newPolicy(application2.getId(), "policy5");
    Policy policy6 = tempEntity.newPolicy(application2.getId(), "policy6");

    //create policy violations app 1
    PolicyViolation policyViolation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    PolicyViolation policyViolation2 = tempEntity.newPolicyViolation(policyEvaluation1, policy2);
    PolicyViolation policyViolation3 = tempEntity.newPolicyViolation(policyEvaluation1, policy3);
    PolicyViolation policyViolation4 = tempEntity.newPolicyViolation(policyEvaluation1, policy4);
    policyViolation1.setThreatLevel(10);
    policyViolation2.setThreatLevel(8);
    policyViolation3.setThreatLevel(10);
    policyViolation4.setThreatLevel(5);
    policyViolation1.setActionTypeId("fail");

    //create policy violations app 1
    PolicyViolation policyViolation5 = tempEntity.newPolicyViolation(policyEvaluation2, policy5);
    PolicyViolation policyViolation6 = tempEntity.newPolicyViolation(policyEvaluation2, policy6);
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
    assertResponseStatus(200, response);
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
