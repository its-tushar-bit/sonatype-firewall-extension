/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.hds.ComponentVersionInfoDTO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreatsAdapter;
import com.sonatype.insight.brain.repository.RepositoryPolicyThreatDTO;
import com.sonatype.insight.brain.repository.RepositoryPolicyViolationDTO;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class QuarantinedComponentService
{
  private final QuarantinedComponentAccessManager quarantinedComponentAccessManager;

  private final ComponentInfoService componentInfoService;

  private final RepositoryDAO repositoryDAO;

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  private static final String PATHNAME_SEPARATOR = "/";

  private static final Logger log = LoggerFactory.getLogger(QuarantinedComponentService.class);

  @Inject
  public QuarantinedComponentService(
      final DbQuarantinedComponentAccessManager quarantinedComponentAccessManager,
      final ComponentInfoService componentInfoService,
      final RepositoryDAO repositoryDAO,
      final RepositoryComponentDAO repositoryComponentDAO,
      final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO
  )
  {
    this.quarantinedComponentAccessManager = quarantinedComponentAccessManager;
    this.componentInfoService = componentInfoService;
    this.repositoryDAO = repositoryDAO;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;

    componentInfoService.setToolName("ci");
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
    quarantinedComponentOverviewDto.componentVersion =
        repositoryComponent.getComponentIdentifier().get(ComponentIdentifier.VERSION);
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

    final RepositoryPolicyThreatDTO repositoryPolicyThreatDTO = new RepositoryPolicyThreatDTO();
    repositoryPolicyThreatDTO.activePolicyViolations =
        policyViolations.stream().sorted(Comparator.comparingInt(RepositoryPolicyViolation::getThreatLevel).reversed())
            .map(this::getRepositoryPolicyViolationDto).collect(Collectors.toList());

    return repositoryPolicyThreatDTO;
  }

  public ComponentVersionInfoDTO getQuarantineComponentVersionRemediation(final String token) {
    final String repositoryComponentId = quarantinedComponentAccessManager.getRepositoryComponentIdFromToken(token);
    final RepositoryComponent repositoryComponent = repositoryComponentDAO.getById(repositoryComponentId);

    return componentInfoService.getComponentVersionInfoNoAuth(OwnerType.REPOSITORY,
        repositoryComponent.getRepositoryId(), repositoryComponent.getComponentIdentifier(), Stage.ID_PROXY,
        repositoryComponent.getIdentificationSourceId(), null, null);
  }

  public NamedComponentDetails getComponentVersionDetails(final String token, final HttpServletRequest httpRequest,
                                                          final String version)
      throws IOException
  {
    final String repositoryComponentId = quarantinedComponentAccessManager.getRepositoryComponentIdFromToken(token);
    final RepositoryComponent repositoryComponent = repositoryComponentDAO.getById(repositoryComponentId);
    final Owner owner = IdUtils.getOwnerNotNull(OwnerType.REPOSITORY, repositoryComponent.getRepositoryId());

    ComponentIdentifier componentIdentifier = repositoryComponent.getComponentIdentifier();

    if (version != null && !version.isEmpty()) {
      componentIdentifier = repositoryComponent.getComponentIdentifier().createAlternativeVersion(version);
    }

    return componentInfoService.getComponentDetails(owner, componentIdentifier, MatchState.EXACT.getId(),
        repositoryComponent.getHash(), false, httpRequest);
  }

  public ApiPageResult<String> getQuarantinedComponentOtherVersions(
      final String token, int page, int pageSize, boolean asc)
  {
    if (page <= 0 || pageSize <= 0) {
      throw new BadRequestException("Page and Page size must be greater than 0");
    }

    final String repositoryComponentId = quarantinedComponentAccessManager.getRepositoryComponentIdFromToken(token);
    RepositoryComponent repositoryComponent = repositoryComponentDAO.getById(repositoryComponentId);
    String repositoryId = repositoryComponent.getRepositoryId();
    String pathname = repositoryComponent.getPathname();
    String pathnamePrefix = getPathnamePrefix(pathname);
    int skipCount = (page - 1) * pageSize;

    List<RepositoryComponent> otherVersionComponents = repositoryComponentDAO
        .getOtherVersionRepositoryComponentsByPathnameFilter(repositoryId, pathnamePrefix, pathname);

    Comparator<RepositoryComponent> comparator = Comparator.comparing(component -> new ComparableVersion(
        component.getComponentIdentifier().get(ComponentIdentifier.VERSION)));

    if (!asc) {
      comparator = comparator.reversed();
    }

    List<String> otherVersionComponentDisplayNames =
        otherVersionComponents.stream()
            .sorted(comparator)
            .map(this::getComponentDisplayName)
            .skip(skipCount).limit(pageSize).collect(Collectors.toList());

    return new ApiPageResult<>(otherVersionComponents.size(), page, pageSize, otherVersionComponentDisplayNames);
  }

  String getPathnamePrefix(String pathname) {

    String[] arr = StringUtils.split(pathname, PATHNAME_SEPARATOR);
    if (arr.length > 2) {
      return StringUtils.join(Arrays.copyOf(arr, arr.length - 2), PATHNAME_SEPARATOR) + PATHNAME_SEPARATOR;
    }
    return pathname;
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
