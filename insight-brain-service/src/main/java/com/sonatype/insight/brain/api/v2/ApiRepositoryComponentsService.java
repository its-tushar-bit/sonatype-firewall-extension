/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentViolationDTO;
import com.sonatype.insight.brain.utils.ThreatLevel;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentViolationListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiHostedRepositoryComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiHostedRepositoryComponentListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiQueueStatsDTO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.repository.hosted.ApplicationForHostedRepositoryComponentService;
import com.sonatype.insight.brain.dataaccess.repository.HostedComponentScanQueueDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class ApiRepositoryComponentsService
{
  private static final int DEFAULT_PAGE_SIZE = 25;

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  private final HostedComponentScanQueueDAO hostedComponentScanQueueDAO;

  private final RepositoryDAO repositoryDAO;

  private final RepositoryManagerDAO repositoryManagerDAO;

  @Inject
  public ApiRepositoryComponentsService(
      final RepositoryComponentDAO repositoryComponentDAO,
      final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      final HostedComponentScanQueueDAO hostedComponentScanQueueDAO,
      final RepositoryDAO repositoryDAO,
      final RepositoryManagerDAO repositoryManagerDAO)
  {
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.hostedComponentScanQueueDAO = hostedComponentScanQueueDAO;
    this.repositoryDAO = repositoryDAO;
    this.repositoryManagerDAO = repositoryManagerDAO;
  }

  public ApiHostedRepositoryComponentListDTO getComponents(
      String repositoryManagerId,
      String repositoryId,
      int page,
      int pageSize,
      String filter)
  {
    if (page < 1) {
      throw new BadRequestException("page must be >= 1");
    }
    Repository repository = validateRepositoryBelongsToManager(repositoryManagerId, repositoryId);
    int effectivePageSize = pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE;
    int total = repositoryComponentDAO.countByRepositoryIdWithFilter(repositoryId, filter);
    int offset = (int) Math.min((long) (page - 1) * effectivePageSize, Integer.MAX_VALUE);
    List<RepositoryComponent> paged = repositoryComponentDAO.getByRepositoryIdPaged(
        repositoryId, filter, effectivePageSize, offset);

    // Load active violations for only the page's pathnames in one IN query
    List<String> pathnames = paged.stream()
        .map(RepositoryComponent::getPathname)
        .filter(p -> p != null)
        .collect(Collectors.toList());
    Map<String, List<RepositoryPolicyViolation>> violationsByPathname =
        repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathnames(repositoryId, pathnames)
            .stream()
            .filter(v -> v.getPathname() != null)
            .collect(Collectors.groupingBy(RepositoryPolicyViolation::getPathname));

    String repositoryPublicId = repository != null ? repository.getPublicId() : null;
    List<ApiHostedRepositoryComponentDTO> dtos = paged.stream()
        .map(c -> toComponentDTO(c, violationsByPathname.getOrDefault(c.getPathname(), List.of()), repositoryPublicId))
        .collect(Collectors.toList());
    ApiHostedRepositoryComponentListDTO result = new ApiHostedRepositoryComponentListDTO();
    result.components = dtos;
    result.totalCount = total;
    result.page = page;
    result.pageSize = effectivePageSize;
    result.repositoryPublicId = repositoryPublicId;
    result.hasNextPage = offset + paged.size() < total;
    result.hasQueuedScans = repositoryComponentDAO.getRepositoryIdsWithQueuedScans(List.of(repositoryId))
        .contains(repositoryId);
    return result;
  }

  public ApiHostedRepositoryComponentDTO getComponent(
      String repositoryManagerId,
      String repositoryId,
      String componentId)
  {
    Repository repo = validateRepositoryBelongsToManager(repositoryManagerId, repositoryId);
    RepositoryComponent c = findComponent(repositoryId, componentId);
    List<RepositoryPolicyViolation> violations = c.getPathname() != null
        ? repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathname(repositoryId, c.getPathname())
        : List.of();
    return toComponentDTO(c, violations, repo != null ? repo.getPublicId() : null);
  }

  public ApiComponentViolationListDTO getViolations(
      String repositoryManagerId,
      String repositoryId,
      String componentId)
  {
    validateRepositoryBelongsToManager(repositoryManagerId, repositoryId);
    RepositoryComponent c = findComponent(repositoryId, componentId);
    List<RepositoryPolicyViolation> violations = c.getPathname() != null
        ? repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathname(repositoryId, c.getPathname())
        : List.of();
    int maxThreat = violations.stream().mapToInt(RepositoryPolicyViolation::getThreatLevel).max().orElse(0);

    List<ApiComponentViolationDTO> dtos = violations.stream().map(v -> {
      ApiComponentViolationDTO dto = new ApiComponentViolationDTO();
      dto.id = v.getId();
      dto.policyId = v.getPolicyId();
      dto.policyName = v.getPolicyName();
      dto.threatLevel = v.getThreatLevel();
      dto.threatCategory = v.getThreatCategory() != null ? v.getThreatCategory().name() : null;
      dto.actionTypeId = v.getActionTypeId();
      dto.waived = v.isWaived();
      return dto;
    }).collect(Collectors.toList());

    ApiComponentViolationListDTO result = new ApiComponentViolationListDTO();
    result.violations = dtos;
    result.totalViolations = dtos.size();
    result.maxThreatLevel = maxThreat;
    return result;
  }

  public ApiQueueStatsDTO getQueueStats(String repositoryManagerId, String repositoryId) {
    validateRepositoryBelongsToManager(repositoryManagerId, repositoryId);
    try (TransactionContext tx = hostedComponentScanQueueDAO.createTransactionContext()) {
      Map<String, Integer> counts =
          hostedComponentScanQueueDAO.countByRepositoryIdGroupedByStatus(tx, repositoryId);
      ApiQueueStatsDTO dto = new ApiQueueStatsDTO();
      dto.pending = counts.getOrDefault("PENDING", 0);
      dto.processing = counts.getOrDefault("IN_PROGRESS", 0);
      dto.completed = counts.getOrDefault("COMPLETED", 0);
      dto.failed = counts.getOrDefault("FAILED", 0);
      dto.total = dto.pending + dto.processing + dto.completed + dto.failed;
      return dto;
    }
  }

  private Repository validateRepositoryBelongsToManager(String repositoryManagerInstanceId, String repositoryId) {
    RepositoryManager manager = repositoryManagerDAO.getByInstanceId(repositoryManagerInstanceId);
    if (manager == null) {
      throw new NotFoundException("Repository manager not found: " + repositoryManagerInstanceId);
    }
    Repository repository = repositoryDAO.getById(repositoryId);
    if (repository == null || !manager.getId().equals(repository.getRepositoryManagerId())) {
      throw new NotFoundException(
          "Repository " + repositoryId + " not found in manager " + repositoryManagerInstanceId);
    }
    return repository;
  }

  private RepositoryComponent findComponent(String repositoryId, String componentId) {
    RepositoryComponent component =
        repositoryComponentDAO.getByRepositoryIdAndComponentId(repositoryId, componentId);
    if (component == null) {
      throw new NotFoundException("Component not found: " + componentId);
    }
    return component;
  }

  private ApiHostedRepositoryComponentDTO toComponentDTO(
      RepositoryComponent c,
      List<RepositoryPolicyViolation> violations,
      String repositoryPublicId)
  {
    int maxThreat = violations.stream().mapToInt(RepositoryPolicyViolation::getThreatLevel).max().orElse(0);
    int critical =
        (int) violations.stream().filter(v -> ThreatLevel.from(v.getThreatLevel()) == ThreatLevel.CRITICAL).count();
    int severe =
        (int) violations.stream().filter(v -> ThreatLevel.from(v.getThreatLevel()) == ThreatLevel.SEVERE).count();
    int moderate =
        (int) violations.stream().filter(v -> ThreatLevel.from(v.getThreatLevel()) == ThreatLevel.MODERATE).count();

    ApiHostedRepositoryComponentDTO dto = new ApiHostedRepositoryComponentDTO();
    dto.id = c.getId();
    dto.pathname = c.getPathname();
    dto.displayName = c.getDisplayName();
    dto.hash = c.getHash();
    dto.matchStateId = c.getMatchStateId();
    dto.lastEvaluationTime = c.getLastEvaluationTime() != null ? c.getLastEvaluationTime().getTime() : null;
    dto.quarantined = c.getQuarantineTime() != null && c.getUnquarantineTime() == null;
    dto.violationCount = violations.size();
    dto.criticalViolationCount = critical;
    dto.severeViolationCount = severe;
    dto.moderateViolationCount = moderate;
    dto.maxThreatLevel = maxThreat;
    dto.componentIdentifier = c.getComponentIdentifier();
    dto.scanId = c.getScanId();
    dto.applicationPublicId = c.getScanId() != null
        ? ApplicationForHostedRepositoryComponentService.generatePublicId(repositoryPublicId, c.getPathname())
        : null;
    dto.stageTypeId = c.getLastEvaluationStage();
    dto.componentCount = c.getComponentCount();
    return dto;
  }
}
