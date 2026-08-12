/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentViolationListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiHostedRepositoryComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiHostedRepositoryComponentListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiQueueStatsDTO;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.HostedComponentScanQueueDAO;
import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO.HrcWithOwnerComponent;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.repository.hosted.ApplicationForHostedRepositoryComponentService;
import com.sonatype.insight.brain.utils.ThreatLevel;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class ApiRepositoryComponentsService
{
  private static final int DEFAULT_PAGE_SIZE = 25;

  private final HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final HostedComponentScanQueueDAO hostedComponentScanQueueDAO;

  private final RepositoryDAO repositoryDAO;

  private final RepositoryManagerDAO repositoryManagerDAO;

  private final OwnerComponentDAO ownerComponentDAO;

  @Inject
  public ApiRepositoryComponentsService(
      final HostedRepositoryComponentDAO hostedRepositoryComponentDAO,
      final PolicyViolationDAO policyViolationDAO,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final HostedComponentScanQueueDAO hostedComponentScanQueueDAO,
      final RepositoryDAO repositoryDAO,
      final RepositoryManagerDAO repositoryManagerDAO,
      final OwnerComponentDAO ownerComponentDAO)
  {
    this.hostedRepositoryComponentDAO = hostedRepositoryComponentDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.hostedComponentScanQueueDAO = hostedComponentScanQueueDAO;
    this.repositoryDAO = repositoryDAO;
    this.repositoryManagerDAO = repositoryManagerDAO;
    this.ownerComponentDAO = ownerComponentDAO;
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
    int total = hostedRepositoryComponentDAO.countByRepositoryIdWithFilter(repositoryId, filter);
    int offset = (int) Math.min((long) (page - 1) * effectivePageSize, Integer.MAX_VALUE);
    List<HrcWithOwnerComponent> paged =
        hostedRepositoryComponentDAO.getByRepositoryIdPaged(repositoryId, filter, effectivePageSize, offset);

    String repositoryPublicId = repository.getPublicId();
    Map<String, String> lastScanIds = lookupLastScanIds(paged);
    Map<String, List<PolicyViolation>> violationsByOwner = lookupViolationsBatched(paged);
    Map<String, Integer> componentCountsByOwner = lookupComponentCountsBatched(paged);
    List<ApiHostedRepositoryComponentDTO> dtos = paged.stream()
        .map(row -> toComponentDTO(
            row,
            violationsFor(row, violationsByOwner),
            scanIdFor(row, lastScanIds),
            repositoryPublicId,
            componentCountFor(row, componentCountsByOwner)))
        .toList();

    ApiHostedRepositoryComponentListDTO result = new ApiHostedRepositoryComponentListDTO();
    result.components = dtos;
    result.totalCount = total;
    result.page = page;
    result.pageSize = effectivePageSize;
    result.repositoryPublicId = repositoryPublicId;
    result.hasNextPage = offset + paged.size() < total;
    result.hasQueuedScans = hostedComponentScanQueueDAO.hasQueuedScans(repositoryId);
    return result;
  }

  public ApiHostedRepositoryComponentDTO getComponent(
      String repositoryManagerId,
      String repositoryId,
      String componentId)
  {
    Repository repository = validateRepositoryBelongsToManager(repositoryManagerId, repositoryId);
    HrcWithOwnerComponent row = findComponent(repositoryId, componentId);
    String scanId = scanIdFor(row, lookupLastScanIds(List.of(row)));
    Integer componentCount = componentCountFor(row, lookupComponentCountsBatched(List.of(row)));
    return toComponentDTO(row, getViolationsFor(row), scanId, repository.getPublicId(), componentCount);
  }

  public ApiComponentViolationListDTO getViolations(
      String repositoryManagerId,
      String repositoryId,
      String componentId)
  {
    validateRepositoryBelongsToManager(repositoryManagerId, repositoryId);
    HrcWithOwnerComponent row = findComponent(repositoryId, componentId);
    List<PolicyViolation> violations = getViolationsFor(row);

    int maxThreat = violations.stream().mapToInt(PolicyViolation::getThreatLevel).max().orElse(0);
    List<ApiComponentViolationDTO> dtos = violations.stream()
        .map(ApiRepositoryComponentsService::toViolationDTO)
        .toList();

    ApiComponentViolationListDTO result = new ApiComponentViolationListDTO();
    result.violations = dtos;
    result.totalViolations = dtos.size();
    result.maxThreatLevel = maxThreat;
    return result;
  }

  private static ApiComponentViolationDTO toViolationDTO(PolicyViolation v) {
    ApiComponentViolationDTO dto = new ApiComponentViolationDTO();
    dto.id = v.getId();
    dto.policyId = v.getPolicyId();
    dto.policyName = v.getPolicyName();
    dto.threatLevel = v.getThreatLevel();
    dto.threatCategory = v.getThreatCategory() != null ? v.getThreatCategory().name() : null;
    dto.actionTypeId = v.getActionTypeId();
    dto.waived = v.isWaived();
    return dto;
  }

  public ApiQueueStatsDTO getQueueStats(String repositoryManagerId, String repositoryId) {
    validateRepositoryBelongsToManager(repositoryManagerId, repositoryId);
    try (TransactionContext tx = hostedComponentScanQueueDAO.createTransactionContext()) {
      Map<String, Integer> counts =
          hostedComponentScanQueueDAO.countByRepositoryIdGroupedByStatus(tx, repositoryId);
      ApiQueueStatsDTO dto = new ApiQueueStatsDTO();
      dto.pending = counts.getOrDefault(HostedComponentScanQueueDAO.Status.PENDING.name(), 0);
      dto.processing = counts.getOrDefault(HostedComponentScanQueueDAO.Status.IN_PROGRESS.name(), 0);
      dto.completed = counts.getOrDefault(HostedComponentScanQueueDAO.Status.COMPLETED.name(), 0);
      dto.failed = counts.getOrDefault(HostedComponentScanQueueDAO.Status.FAILED.name(), 0);
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

  private HrcWithOwnerComponent findComponent(String repositoryId, String componentId) {
    HrcWithOwnerComponent row = hostedRepositoryComponentDAO.getByRepositoryIdAndComponentId(repositoryId, componentId);
    if (row == null) {
      throw new NotFoundException("Component not found: " + componentId);
    }
    return row;
  }

  private static boolean isPinned(HrcWithOwnerComponent row) {
    return row.ownerComponent() != null && row.ownerComponent().getStageTypeId() != null;
  }

  private static String pinKey(String hrcId, String stageTypeId) {
    return hrcId + '|' + stageTypeId;
  }

  private record PinKeys(Set<String> hrcIds, Set<String> stageIds)
  {
  }

  private static PinKeys collectPinKeys(List<HrcWithOwnerComponent> rows) {
    Set<String> hrcIds = new HashSet<>();
    Set<String> stageIds = new HashSet<>();
    for (HrcWithOwnerComponent row : rows) {
      if (isPinned(row)) {
        hrcIds.add(row.hrc().getId());
        stageIds.add(row.ownerComponent().getStageTypeId());
      }
    }
    return new PinKeys(hrcIds, stageIds);
  }

  // Stage-scoped: keyed by (hrcId | stageTypeId). forMonitoring PEs may have a null scan_id.
  private Map<String, String> lookupLastScanIds(List<HrcWithOwnerComponent> rows) {
    PinKeys keys = collectPinKeys(rows);
    Map<String, String> byHrcAndStage = new HashMap<>();
    policyEvaluationDAO.getLastByOwnerIdsAndStageIds(keys.hrcIds(), keys.stageIds())
        .stream()
        .filter(pe -> pe.getScanId() != null)
        .forEach(pe -> byHrcAndStage.put(pinKey(pe.getOwnerId(), pe.getStageTypeId()), pe.getScanId()));
    return byHrcAndStage;
  }

  private static String scanIdFor(HrcWithOwnerComponent row, Map<String, String> byOwnerAndStage) {
    if (!isPinned(row)) {
      return null;
    }
    return byOwnerAndStage.get(pinKey(row.hrc().getId(), row.ownerComponent().getStageTypeId()));
  }

  private Map<String, Integer> lookupComponentCountsBatched(List<HrcWithOwnerComponent> rows) {
    PinKeys keys = collectPinKeys(rows);
    return ownerComponentDAO.getCountsByOwnerIdsAndStageTypeIds(keys.hrcIds(), keys.stageIds());
  }

  private static Integer componentCountFor(
      HrcWithOwnerComponent row,
      Map<String, Integer> byOwnerAndStage)
  {
    if (!isPinned(row)) {
      return null;
    }
    return byOwnerAndStage.get(pinKey(row.hrc().getId(), row.ownerComponent().getStageTypeId()));
  }

  private ApiHostedRepositoryComponentDTO toComponentDTO(
      HrcWithOwnerComponent row,
      List<PolicyViolation> violations,
      String scanId,
      String repositoryPublicId,
      Integer componentCount)
  {
    HostedRepositoryComponent hrc = row.hrc();
    OwnerComponent oc = row.ownerComponent();

    int maxThreat = 0, critical = 0, severe = 0, moderate = 0;
    for (PolicyViolation v : violations) {
      int level = v.getThreatLevel();
      if (level > maxThreat) {
        maxThreat = level;
      }
      ThreatLevel t = ThreatLevel.from(level);
      if (t == ThreatLevel.CRITICAL) {
        critical++;
      }
      else if (t == ThreatLevel.SEVERE) {
        severe++;
      }
      else if (t == ThreatLevel.MODERATE) {
        moderate++;
      }
    }

    ApiHostedRepositoryComponentDTO dto = new ApiHostedRepositoryComponentDTO();
    dto.id = hrc.getId();
    dto.pathname = hrc.getPathname();
    dto.displayName = deriveDisplayName(hrc.getPathname(), oc);
    dto.hash = oc != null ? oc.getHash() : hrc.getHash();
    dto.matchStateId = oc != null ? oc.getMatchStateId() : null;
    dto.lastEvaluationTime = oc != null && oc.getTime() != null ? oc.getTime().getTime() : null;
    dto.quarantined = false;
    dto.violationCount = violations.size();
    dto.criticalViolationCount = critical;
    dto.severeViolationCount = severe;
    dto.moderateViolationCount = moderate;
    dto.maxThreatLevel = maxThreat;
    dto.componentIdentifier = oc != null ? oc.getComponentIdentifier() : null;
    dto.scanId = scanId;
    dto.applicationPublicId = scanId != null && repositoryPublicId != null && hrc.getPathname() != null
        ? ApplicationForHostedRepositoryComponentService.generatePublicId(repositoryPublicId, hrc.getPathname())
        : null;
    dto.stageTypeId = oc != null ? oc.getStageTypeId() : null;
    dto.componentCount = componentCount;
    return dto;
  }

  private static String deriveDisplayName(String pathname, OwnerComponent oc) {
    if (oc != null && oc.getComponentIdentifier() != null) {
      return ComponentDisplayNameUtil.fromIdentifier(oc.getComponentIdentifier()).toString();
    }
    if (pathname == null) {
      return null;
    }
    return pathname.substring(pathname.lastIndexOf('/') + 1) + " (" + pathname + ")";
  }

  // policy_violation.owner_id is the HRC id (ScanPolicyEvaluator invoked with hrc); scope by stage.
  private List<PolicyViolation> getViolationsFor(HrcWithOwnerComponent row) {
    return violationsFor(row, lookupViolationsBatched(List.of(row)));
  }

  private Map<String, List<PolicyViolation>> lookupViolationsBatched(List<HrcWithOwnerComponent> rows) {
    Set<String> hrcIds = new HashSet<>();
    Set<String> stageIds = new HashSet<>();
    for (HrcWithOwnerComponent row : rows) {
      if (isPinned(row)) {
        hrcIds.add(row.hrc().getId());
        stageIds.add(row.ownerComponent().getStageTypeId());
      }
    }
    return policyViolationDAO.getActiveByOwnerIdsAndStageIdsGrouped(hrcIds, stageIds);
  }

  private static List<PolicyViolation> violationsFor(
      HrcWithOwnerComponent row,
      Map<String, List<PolicyViolation>> byOwnerAndStage)
  {
    if (!isPinned(row)) {
      return List.of();
    }
    return byOwnerAndStage.getOrDefault(pinKey(row.hrc().getId(), row.ownerComponent().getStageTypeId()), List.of());
  }
}
