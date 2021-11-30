/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreatsAdapter;
import com.sonatype.insight.brain.repository.RepositoryPolicyThreatDTO;
import com.sonatype.insight.brain.repository.RepositoryPolicyViolationDTO;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

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

  public RepositoryPolicyThreatDTO getQuarantinedComponentPolicyViolations(final String token) {

    final String repositoryComponentId = quarantinedComponentAccessManager.getRepositoryComponentIdFromToken(token);
    final RepositoryComponent repositoryComponent = repositoryComponentDAO.getById(repositoryComponentId);
    final List<RepositoryPolicyViolation> policyViolations = getQuarantinedPolicyViolations(repositoryComponent);
    if (policyViolations.size() == 0) {
      log.error("Could not find policy violations causing quarantine for the component with repository component id {}",
          repositoryComponentId);
      throw new NotFoundException("No policy violations causing quarantine exist for the requested component.");
    }

    final RepositoryPolicyThreatDTO repositoryPolicyThreatDTO = new RepositoryPolicyThreatDTO();
    repositoryPolicyThreatDTO.activePolicyViolations =
        policyViolations.stream().sorted(Comparator.comparingInt(RepositoryPolicyViolation::getThreatLevel).reversed())
            .map(this::getRepositoryPolicyViolationDto).collect(Collectors.toList());

    return repositoryPolicyThreatDTO;
  }

  private RepositoryPolicyViolationDTO getRepositoryPolicyViolationDto(
      RepositoryPolicyViolation policyViolation)
  {
    final RepositoryPolicyViolationDTO policyViolationDto = new RepositoryPolicyViolationDTO();

    policyViolationDto.policyId = policyViolation.getPolicyId();
    policyViolationDto.policyName = policyViolation.getPolicyName();
    policyViolationDto.policyThreatLevel = policyViolation.getThreatLevel();
    policyViolationDto.constraints =
        PolicyThreatsAdapter.toPolicyThreatsPolicyConstraints(policyViolation.getConstraintFacts());
    policyViolationDto.blocksUnquarantine = Action.ID_FAIL.equals(policyViolation.getActionTypeId());
    policyViolationDto.constraintFactsJson = policyViolation.getConstraintFactsJson();

    return policyViolationDto;
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

  private List<RepositoryPolicyViolation> getQuarantinedPolicyViolations(RepositoryComponent repositoryComponent) {
    String repositoryId = repositoryComponent.getRepositoryId();
    String pathName = repositoryComponent.getPathname();
    return repositoryPolicyViolationDAO.getByRepositoryIdAndPathnameAndActionAndNotWaived(
        repositoryId,
        pathName,
        Action.ID_FAIL);
  }
}
