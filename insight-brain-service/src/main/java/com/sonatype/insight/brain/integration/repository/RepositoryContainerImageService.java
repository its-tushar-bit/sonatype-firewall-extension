/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

@Named
@Singleton
public class RepositoryContainerImageService
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryContainerImageService.class);

  private final RepositoryDAO repositoryDAO;

  private final ApplicationDAO applicationDAO;

  private final PolicyViolationDAO policyViolationDAO;

  @Inject
  public RepositoryContainerImageService(
      RepositoryDAO repositoryDAO,
      ApplicationDAO applicationDAO,
      PolicyViolationDAO policyViolationDAO)
  {
    this.repositoryDAO = repositoryDAO;
    this.applicationDAO = applicationDAO;
    this.policyViolationDAO = policyViolationDAO;
  }

  public boolean isContainerImageQuarantined(
      String repositoryManagerInstanceId,
      String repositoryPublicId,
      String containerImagePublicId)
  {
    Repository repository = repositoryDAO
        .getByRepositoryManagerInstanceIdAndPublicIdNotNull(repositoryManagerInstanceId, repositoryPublicId);
    return isContainerImageQuarantined(repository, containerImagePublicId);
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  boolean isContainerImageQuarantined(
      @AuthzContext(Key.REPOSITORY) Repository repository,
      String containerImagePublicId)
  {
    if (repository.getRepositoryType() != RepositoryType.proxy || !"docker".equals(repository.getFormat())) {
      throw new BadRequestException("The repository must be of type proxy and format docker");
    }

    Application application = applicationDAO.getByPublicId(containerImagePublicId);

    if (application == null || !application.getOrganizationId().equals(repository.getRelatedOrganizationId())) {
      throw new NotFoundException("No container image was found with public ID " + containerImagePublicId);
    }

    log.debug("Finding out if the container image {} is in quarantine", containerImagePublicId);

    List<PolicyViolation> activeFailingViolations = policyViolationDAO
        .getActiveByApplicationIdAndStageIdAndActionId(application.getId(), Stage.ID_PROXY, Action.ID_FAIL);

    boolean isInQuarantine = !activeFailingViolations.isEmpty();

    log.debug("Found {} active policy violations failing in at the proxy stage. Quarantine result: {}",
        activeFailingViolations.size(), isInQuarantine);

    return isInQuarantine;
  }
}
