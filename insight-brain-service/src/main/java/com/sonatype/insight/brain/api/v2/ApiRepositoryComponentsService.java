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
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentViolationDTO;
import com.sonatype.insight.brain.utils.ThreatLevel;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentViolationListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiHostedRepositoryComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiHostedRepositoryComponentListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiQueueStatsDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.repository.hosted.ApplicationForHostedRepositoryComponentService;
import com.sonatype.insight.brain.dataaccess.repository.HostedComponentScanQueueDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.report.ApplicationReport;
import com.sonatype.insight.brain.report.ReportDataStore;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ApiRepositoryComponentsService
{
  private static final Logger log = LoggerFactory.getLogger(ApiRepositoryComponentsService.class);

  private static final int DEFAULT_PAGE_SIZE = 25;

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  private final HostedComponentScanQueueDAO hostedComponentScanQueueDAO;

  private final RepositoryDAO repositoryDAO;

  private final RepositoryManagerDAO repositoryManagerDAO;

  // CLM-40943: dependencies for the policythreats.json-backed component pill counts.
  private final ApplicationDAO applicationDAO;

  private final Provider<ReportDataStore> reportDataStoreProvider;

  @Inject
  public ApiRepositoryComponentsService(
      final RepositoryComponentDAO repositoryComponentDAO,
      final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      final HostedComponentScanQueueDAO hostedComponentScanQueueDAO,
      final RepositoryDAO repositoryDAO,
      final RepositoryManagerDAO repositoryManagerDAO,
      final ApplicationDAO applicationDAO,
      final Provider<ReportDataStore> reportDataStoreProvider)
  {
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.hostedComponentScanQueueDAO = hostedComponentScanQueueDAO;
    this.repositoryDAO = repositoryDAO;
    this.repositoryManagerDAO = repositoryManagerDAO;
    this.applicationDAO = applicationDAO;
    this.reportDataStoreProvider = reportDataStoreProvider;
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

    // Load active violations for only the page's pathnames in one IN query. CLM-40943: each
    // outer artifact may have inner-pathname violations (`outer.zip!/inner.jar`) that the
    // archive-of-archives evaluator persisted under synthetic inner pathnames; roll those up
    // under their outer so the per-row violation count reflects all inner CVEs, not just the
    // outer's own "component-unknown" violation.
    List<String> pathnames = paged.stream()
        .map(RepositoryComponent::getPathname)
        .filter(p -> p != null)
        .collect(Collectors.toList());
    Map<String, List<RepositoryPolicyViolation>> violationsByPathname =
        repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathnamesOrInnerPathnames(
            repositoryId, pathnames)
            .stream()
            .filter(v -> v.getPathname() != null)
            .collect(Collectors.groupingBy(this::outerPathnameOf));

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
    // CLM-40943: include inner-pathname violations under this outer artifact.
    List<RepositoryPolicyViolation> violations = c.getPathname() != null
        ? repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathnameOrInnerPathnames(
            repositoryId, c.getPathname())
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
    // CLM-40943: include inner-pathname violations under this outer artifact.
    List<RepositoryPolicyViolation> violations = c.getPathname() != null
        ? repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathnameOrInnerPathnames(
            repositoryId, c.getPathname())
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

  /**
   * Returns the outer pathname for a {@code RepositoryPolicyViolation}: the pathname unchanged
   * if there is no {@code "!/"} marker, otherwise everything before the first {@code "!/"}. Used
   * to group inner-pathname violations (`outer.zip!/inner.jar`) under their outer artifact's
   * Components-page row so each row reflects the rolled-up violation count.
   */
  private String outerPathnameOf(final RepositoryPolicyViolation v) {
    String pathname = v.getPathname();
    if (pathname == null) {
      return null;
    }
    int sep = pathname.indexOf("!/");
    return sep < 0 ? pathname : pathname.substring(0, sep);
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

    // CLM-40943: Component-pill counts now read from policythreats.json (the same file the
    // drill-in report renders), so the Components page pill and the drill-in pill cannot
    // disagree. The DB-side repository_policy_violation table holds the raw evaluator output
    // (one row per pathname × policy × CVE-constraint), which inflates pill counts whenever a
    // component has multiple CVEs against the same policy or when nested-component mirroring
    // synthesised multiple inner pathnames for the same logical inner.
    //
    // We look up the synthetic application + scanId for this outer artifact, read the
    // policythreats.json overlay that HostedComponentScanQueueConsumer wrote, and tally
    // {@code aaData[*].activeViolations[*].policyThreatLevel} into CRITICAL/SEVERE/MODERATE
    // buckets — exact-threat-level histogram, same data the drill-in pill displays.
    //
    // Fail-soft: if the report file isn't on disk yet (eval still running, or older outer
    // without a synthetic app), fall back to the legacy raw-row tally so the page renders.
    int[] pillCounts = computePillCountsFromPolicyThreats(c, repositoryPublicId);
    int critical;
    int severe;
    int moderate;
    if (pillCounts != null) {
      critical = pillCounts[0];
      severe = pillCounts[1];
      moderate = pillCounts[2];
    }
    else {
      // Legacy fallback (raw row count). Used when policythreats.json is unavailable.
      critical =
          (int) violations.stream().filter(v -> ThreatLevel.from(v.getThreatLevel()) == ThreatLevel.CRITICAL).count();
      severe =
          (int) violations.stream().filter(v -> ThreatLevel.from(v.getThreatLevel()) == ThreatLevel.SEVERE).count();
      moderate =
          (int) violations.stream().filter(v -> ThreatLevel.from(v.getThreatLevel()) == ThreatLevel.MODERATE).count();
    }

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

  /**
   * Reads {@code policythreats.json} for this hosted outer artifact and returns the
   * CRITICAL/SEVERE/MODERATE pill counts the drill-in renders. Returns {@code null} if any of
   * the lookup steps fail so the caller can fall back to the legacy row-count logic.
   * <p>
   * The pill count is the exact-threat-level histogram of all active violations across all
   * components in {@code aaData}, bucketed into ThreatLevel categories. This is the same view
   * the drill-in pill shows, so the Components page and the drill-in cannot disagree.
   */
  private int[] computePillCountsFromPolicyThreats(
      final RepositoryComponent c,
      final String repositoryPublicId)
  {
    String scanId = c.getScanId();
    String pathname = c.getPathname();
    if (scanId == null || pathname == null || repositoryPublicId == null) {
      return null;
    }
    String appPublicId = ApplicationForHostedRepositoryComponentService.generatePublicId(repositoryPublicId, pathname);
    if (appPublicId == null) {
      return null;
    }
    try {
      Application app = applicationDAO.getByPublicId(appPublicId);
      if (app == null) {
        return null;
      }
      ApplicationReport report = reportDataStoreProvider.get().getApplicationReport(app, scanId);
      ReportEntry entry = report != null ? report.getEntry("policythreats.json") : null;
      if (entry == null || entry.buf == null || entry.buf.length == 0) {
        return null;
      }
      JsonNode aaData = MAPPER.readTree(entry.buf).path("aaData");
      if (!aaData.isArray()) {
        return null;
      }
      int critical = 0;
      int severe = 0;
      int moderate = 0;
      for (JsonNode component : aaData) {
        JsonNode active = component.path("activeViolations");
        if (!active.isArray()) {
          continue;
        }
        for (JsonNode violation : active) {
          int tl = violation.path("policyThreatLevel").asInt(0);
          ThreatLevel bucket = ThreatLevel.from(tl);
          if (bucket == ThreatLevel.CRITICAL) {
            critical++;
          }
          else if (bucket == ThreatLevel.SEVERE) {
            severe++;
          }
          else if (bucket == ThreatLevel.MODERATE) {
            moderate++;
          }
        }
      }
      return new int[]{critical, severe, moderate};
    }
    catch (Exception e) {
      // Fall back to legacy tally. The report may not be on disk yet (eval still running),
      // or the synthetic application/scan link may not be established for older components.
      log.debug("Falling back to legacy pill count for repositoryComponent={}, pathname={}: {}",
          c.getId(), pathname, e.getMessage());
      return null;
    }
  }
}
