/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.component;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class QuarantinedComponentService
{
  private final QuarantinedComponentAccessManager quarantinedComponentAccessManager;

  private final RepositoryDAO repositoryDAO;

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  private static final Logger log = LoggerFactory.getLogger(QuarantinedComponentService.class);

  @Inject
  public QuarantinedComponentService(
      final DbQuarantinedComponentAccessManager quarantinedComponentAccessManager,
      final RepositoryDAO repositoryDAO,
      final RepositoryComponentDAO repositoryComponentDAO,
      final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO
  )
  {
    this.quarantinedComponentAccessManager = quarantinedComponentAccessManager;
    this.repositoryDAO = repositoryDAO;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
  }

  public QuarantinedComponentDto getQuarantinedComponent(final String token) {
    final QuarantinedComponentDto quarantinedComponentDto = new QuarantinedComponentDto();
    quarantinedComponentDto.repositoryComponentId =
        quarantinedComponentAccessManager.getRepositoryComponentIdFromToken(token);
    quarantinedComponentDto.success = true;
    return quarantinedComponentDto;
  }

  public QuarantinedComponentOverviewDto getQuarantinedComponentOverview(final String token) {
    final String repositoryComponentId = quarantinedComponentAccessManager.getRepositoryComponentIdFromToken(token);
    RepositoryComponent repositoryComponent = repositoryComponentDAO.getById(repositoryComponentId);

    final QuarantinedComponentOverviewDto quarantinedComponentOverviewDto = new QuarantinedComponentOverviewDto();
    quarantinedComponentOverviewDto.componentDisplayName = getComponentDisplayName(repositoryComponent);
    quarantinedComponentOverviewDto.isQuarantined = repositoryComponent.isQuarantined();
    quarantinedComponentOverviewDto.quarantinedPolicyViolationsCount =
        getQuarantinedPolicyViolationsCount(repositoryComponent);
    quarantinedComponentOverviewDto.repositoryName = getRepositoryName(repositoryComponent);
    quarantinedComponentOverviewDto.quarantinedDate = repositoryComponent.getQuarantineTime();
    quarantinedComponentOverviewDto.cataloguedDate = repositoryComponent.getTime();

    return quarantinedComponentOverviewDto;
  }

  private String getComponentDisplayName(RepositoryComponent repositoryComponent) {
    ComponentIdentifier componentIdentifier = repositoryComponent.getComponentIdentifier();
    if (componentIdentifier == null) {
      log.error("Component Identifier for the quarantined component with repository component id {} is null",
          repositoryComponent.getId());
      throw new BadRequestException("The component identifier for the requested component does not exist.");
    }
    return ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString();
  }

  private int getQuarantinedPolicyViolationsCount(RepositoryComponent repositoryComponent) {
    String repositoryId = repositoryComponent.getRepositoryId();
    String pathName = repositoryComponent.getPathname();
    return repositoryPolicyViolationDAO.getQuarantinedPolicyViolationsCountByRepositoryIdAndPathname(
        repositoryId,
        pathName);
  }

  private String getRepositoryName(RepositoryComponent repositoryComponent) {
    String repositoryId = repositoryComponent.getRepositoryId();
    Repository repository = repositoryDAO.getById(repositoryId);
    return repository.getPublicId();
  }
}
