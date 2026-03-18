/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.container.images;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.containerimages.ContainerImagePolicyViolationSummaryDTO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.repository.ContainerImageSummaryDTO;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.error.exception.BadRequestException;

@Named
public class ContainerImageReportService
{
  private final ApplicationDAO applicationDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final RepositoryDAO repositoryDAO;

  @Inject
  public ContainerImageReportService(
      RepositoryDAO repositoryDAO,
      ApplicationDAO applicationDAO,
      PolicyViolationDAO policyViolationDAO)
  {
    this.applicationDAO = applicationDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.repositoryDAO = repositoryDAO;
  }

  @Authorize(permission = Permission.READ)
  public ContainerImageSummaryDTO getContainerImagesSummary(@AuthzContext(Key.REPOSITORY_ID) String repositoryId) {
    return getContainerImagesSummaryNoAuthz(repositoryId);
  }

  public ContainerImageSummaryDTO getContainerImagesSummaryNoAuthz(String repositoryId) {

    ContainerImageSummaryDTO summary = new ContainerImageSummaryDTO();

    Repository repository = repositoryDAO.getByIdNotNull(repositoryId);

    if (repository.getRepositoryType() != RepositoryType.proxy || !"docker".equals(repository.getFormat())) {
      throw new BadRequestException("Repository must be of type proxy and format docker");
    }

    long repositoryContainerImageCount =
        applicationDAO.getApplicationsCountByOrganizationId(repository.getRelatedOrganizationId());

    ContainerImagePolicyViolationSummaryDTO result =
        policyViolationDAO.getContainerImagePolicyViolationSummaryForRepository(repositoryId);

    summary.totalContainerImageCount = repositoryContainerImageCount;
    summary.totalContainerImageViolationCount = result.getCriticalPolicyViolationsCount() +
        result.getSeverePolicyViolationsCount() +
        result.getModeratePolicyViolationsCount();
    summary.criticalViolationCount = result.getCriticalPolicyViolationsCount();
    summary.severeViolationCount = result.getSeverePolicyViolationsCount();
    summary.moderateViolationCount = result.getModeratePolicyViolationsCount();
    summary.affectedContainerImageCount = result.getAffectedContainerImagesCount();
    summary.quarantinedContainerImageCount = repository.isQuarantineEnabled()
        ? result.getQuarantinedContainerImagesCount()
        : 0;

    return summary;
  }
}
