/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.container.images;
import com.sonatype.insight.brain.common.test.SlowTest;

import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.repository.ContainerImageSummaryDTO;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ContainerImageReportServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ContainerImageReportService containerImageReportService;

  private RepositoryDAO repositoryDAO;

  private OrganizationDAO organizationDAO;

  private PolicyViolationDAO policyViolationDAO;

  @Before
  public void setup() {
    repositoryDAO = lookup(RepositoryDAO.class);
    policyViolationDAO = lookup(PolicyViolationDAO.class);
    organizationDAO = lookup(OrganizationDAO.class);
  }

  @Test
  public void testGetContainerImagesSummary() {
    Repository repository = tempEntity.newRepository("publicId");
    Organization organization = tempEntity.newOrganization("org");
    organization.setRelatedRepositoryId(repository.getId());
    repository.setRelatedOrganizationId(organization.getId());
    repository.setFormat("docker");
    repository.setQuarantineEnabled(true);
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

    ContainerImageSummaryDTO summary = containerImageReportService.getContainerImagesSummary(repository.getId());

    assertThat(summary.totalContainerImageCount).isEqualTo(3);
    assertThat(summary.totalContainerImageViolationCount).isEqualTo(6);
    assertThat(summary.quarantinedContainerImageCount).isEqualTo(1);
    assertThat(summary.criticalViolationCount).isEqualTo(4);
    assertThat(summary.severeViolationCount).isEqualTo(1);
    assertThat(summary.moderateViolationCount).isEqualTo(1);
    assertThat(summary.affectedContainerImageCount).isEqualTo(2);
  }

  /**
   * NEXUS-50206: Verify that quarantine count is 0 when quarantine is disabled,
   * even when there are policy violations with "Fail" actions.
   */
  @Test
  public void testGetContainerImagesSummary_QuarantineDisabled() {
    Repository repository = tempEntity.newRepository("publicId");
    Organization organization = tempEntity.newOrganization("org");
    organization.setRelatedRepositoryId(repository.getId());
    repository.setRelatedOrganizationId(organization.getId());
    repository.setFormat("docker");
    repository.setQuarantineEnabled(false);
    repositoryDAO.update(repository);
    organizationDAO.update(organization);
    // Container Image application
    Application application1 = tempEntity.newApplication("app1", "appPublicId1", organization.getId());

    //policy evaluation
    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(application1.getId(), "proxy", "scanId1");

    //policy for policy violation
    Policy policy1 = tempEntity.newPolicy(application1.getId(), "policy1");

    //create policy violation with fail action (quarantine)
    PolicyViolation policyViolation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    policyViolation1.setThreatLevel(10);
    policyViolation1.setActionTypeId("fail");
    policyViolationDAO.update(policyViolation1);

    ContainerImageSummaryDTO summary = containerImageReportService.getContainerImagesSummary(repository.getId());

    // Even though there's a fail action violation, quarantine count should be 0 when quarantine is disabled
    assertThat(summary.totalContainerImageCount).isEqualTo(1);
    assertThat(summary.totalContainerImageViolationCount).isEqualTo(1);
    assertThat(summary.quarantinedContainerImageCount).isEqualTo(0);
    assertThat(summary.criticalViolationCount).isEqualTo(1);
    assertThat(summary.affectedContainerImageCount).isEqualTo(1);
  }

  @Test
  public void testGetContainerImagesSummary_RepositoryNoContainerImages() {
    Repository repository = tempEntity.newRepository("publicId");
    Organization organization = tempEntity.newOrganization("org");
    organization.setRelatedRepositoryId(repository.getId());
    repository.setRelatedOrganizationId(organization.getId());
    repository.setFormat("docker");
    repositoryDAO.update(repository);
    organizationDAO.update(organization);

    ContainerImageSummaryDTO summary = containerImageReportService.getContainerImagesSummary(repository.getId());

    assertThat(summary.totalContainerImageCount).isEqualTo(0);
    assertThat(summary.totalContainerImageViolationCount).isEqualTo(0);
    assertThat(summary.quarantinedContainerImageCount).isEqualTo(0);
    assertThat(summary.criticalViolationCount).isEqualTo(0);
    assertThat(summary.severeViolationCount).isEqualTo(0);
    assertThat(summary.moderateViolationCount).isEqualTo(0);
    assertThat(summary.affectedContainerImageCount).isEqualTo(0);
  }

  @Test
  public void testGetContainerImagesSummary_RepositoryIdDoesNotExist() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> containerImageReportService.getContainerImagesSummary("foobar"))
        .withMessage("Repository with ID foobar does not exist.");
  }
  
  @Test
  public void testGetContainerImagesSummary_RepositoryIdIsNotDocker() {
    Repository repository = tempEntity.newRepository("publicId");
    Organization organization = tempEntity.newOrganization("org");
    organization.setRelatedRepositoryId(repository.getId());
    repository.setRelatedOrganizationId(organization.getId());
    repository.setFormat("npm");
    repositoryDAO.update(repository);
    organizationDAO.update(organization);
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> containerImageReportService.getContainerImagesSummary(repository.getId()))
        .withMessage("Repository must be of type proxy and format docker");
  }
  
  @Test
  public void testGetContainerImagesSummary_RepositoryIdIsNotProxy() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(
        repoManager,
        "publicId",
        RepositoryType.hosted,
        "docker",
        false
    );
    Organization organization = tempEntity.newOrganization("org");
    organization.setRelatedRepositoryId(repository.getId());
    repository.setRelatedOrganizationId(organization.getId());
    repositoryDAO.update(repository);
    organizationDAO.update(organization);
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> containerImageReportService.getContainerImagesSummary(repository.getId()))
        .withMessage("Repository must be of type proxy and format docker");
  }
}
