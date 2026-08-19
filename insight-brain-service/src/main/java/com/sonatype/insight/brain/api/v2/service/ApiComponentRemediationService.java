/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiDependencyTreeNodeDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiBulkComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiBulkComponentRemediationResultDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.hds.ComponentDetailsDTO;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
import com.sonatype.insight.brain.hds.ComponentDetailsLoaderFactory;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.hds.ComponentRemediationService;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.tenancy.TenantThreadPoolExecutor;
import com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.purl.InvalidPackageURLException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.lang3.StringUtils;

import static com.sonatype.insight.brain.telemetry.NonBreakingRecommendationTelemetryStats.SourceEndpoint.API_COMPONENT_REMEDIATION;

/**
 * @since 1.64
 */
@Named
public class ApiComponentRemediationService
{
  /**
   * Upper bound on components per bulk request. Requests exceeding this are rejected with HTTP 400 before
   * any work is submitted — cheap sanity check so a caller can't POST an unreasonable body.
   */
  static final int MAX_BULK_COMPONENTS = 200;

  /**
   * Number of worker threads created for a single bulk request. Each request gets its own executor of this
   * size; the executor is shut down before the request returns. There is no cross-request thread pool —
   * concurrent bulk requests each spin up their own workers, matching how the single-component endpoint
   * behaves under concurrent clients (no shared throttle either).
   */
  static final int BULK_REMEDIATION_PARALLELISM = 8;

  /**
   * Wall-clock ceiling on the entire bulk request (not per component). Once this elapses while awaiting
   * results, the request thread stops waiting, cancels remaining futures, and returns HTTP 503. Prevents
   * a stuck HDS call from hanging the whole request forever. 5 minutes is generous — the healthy-path
   * cost for 200 components on 8 workers at ~200 ms each is ~5 seconds; this budget only ever fires
   * when downstream calls are genuinely stuck.
   */
  static final Duration BULK_REQUEST_DEADLINE = Duration.ofMinutes(5);

  private final ComponentInfoService componentInfoService;

  private final ComponentRemediationService componentRemediationService;

  private final HdsClient hdsClient;

  private final ThirdPartyComponentDAO thirdPartyComponentDAO;

  private final ApplicationDAO applicationDAO;

  private final OwnerDAO ownerDAO;

  private final ComponentDetailsLoaderFactory componentDetailsLoaderFactory;

  private final IdUtils idUtils;

  private final ApiReportDataServiceV2 apiReportDataServiceV2;

  @Inject
  public ApiComponentRemediationService(
      ComponentInfoService componentInfoService,
      ComponentRemediationService componentRemediationService,
      HdsClient hdsClient,
      ThirdPartyComponentDAO thirdPartyComponentDAO,
      ApplicationDAO applicationDAO,
      OwnerDAO ownerDAO,
      ComponentDetailsLoaderFactory componentDetailsLoaderFactory,
      IdUtils idUtils,
      ApiReportDataServiceV2 apiReportDataServiceV2)
  {
    this.componentInfoService = componentInfoService;
    componentInfoService.setToolName("ci");
    this.componentRemediationService = componentRemediationService;
    this.hdsClient = hdsClient;
    this.thirdPartyComponentDAO = thirdPartyComponentDAO;
    this.applicationDAO = applicationDAO;
    this.ownerDAO = ownerDAO;
    this.componentDetailsLoaderFactory = componentDetailsLoaderFactory;
    this.idUtils = idUtils;
    this.apiReportDataServiceV2 = apiReportDataServiceV2;
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  public ApiComponentRemediationDTO getSuggestedRemediationForComponent(
      ApiComponentDTOV2 componentDTO,
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId,
      String stageId,
      final String identificationSource,
      final String scanId,
      final Boolean includeParentRemediation)
  {
    return getSuggestedRemediationForComponentNoAuthz(componentDTO, ownerType, ownerId, stageId, identificationSource,
        scanId, includeParentRemediation, false);
  }

  /**
   * Bulk variant of {@link #getSuggestedRemediationForComponent}. Behaves as the single-component
   * endpoint would if a caller made N calls in parallel — same authorization, same per-component
   * validation, same result shape — but consolidated into one request. Each request runs its N
   * components across {@value #BULK_REMEDIATION_PARALLELISM} worker threads created for the request
   * and shut down when the request returns.
   * <p>
   * Only per-component <em>input-validation</em> failures — those thrown as
   * {@link InvalidComponentException} (a null entry, missing componentIdentifier / packageUrl,
   * malformed identifier or purl, HDS reporting the component as unknown) — are reported as per-item
   * errors in the {@code error} field. Batch-level failures (invalid {@code stageId}, invalid
   * {@code scanId} for repositories, oversized batch, unauthorized owner, license issues), any other
   * {@code BadRequestException}, downstream failures, and the batch-deadline timeout propagate as HTTP
   * errors rather than per-item errors.
   * <p>
   * No cross-request throttling: like the single-component endpoint, this method makes no attempt to
   * limit total server load from concurrent bulk callers. Expected usage is one bulk request at a time
   * per client.
   *
   * @since 1.205
   */
  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  public ApiBulkComponentRemediationDTO getSuggestedRemediationForComponentsBulk(
      List<ApiComponentDTOV2> componentDTOs,
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId,
      final String stageId,
      final String identificationSource,
      final String scanId,
      final Boolean includeParentRemediation)
  {
    // ---- Batch-level validation, on the request thread, before any work is submitted ----
    if (componentDTOs == null || componentDTOs.isEmpty()) {
      throw new BadRequestException("At least one component must be supplied.");
    }
    if (componentDTOs.size() > MAX_BULK_COMPONENTS) {
      throw new BadRequestException("At most " + MAX_BULK_COMPONENTS + " components may be supplied per bulk request; "
          + "received " + componentDTOs.size() + ".");
    }
    final String effectiveStageId = validateBatchLevelParams(ownerType, stageId, scanId);

    // A per-request executor: N workers live only for the duration of this call. Using
    // TenantThreadPoolExecutor (rather than plain Executors.newFixedThreadPool) captures the current
    // tenant and Shiro Subject on this request thread and re-binds them on each worker before the
    // task runs — without it, MTIQ workers would read the wrong tenant's cached data.
    ThreadFactory threadFactory = new ThreadFactoryBuilder()
        .setDaemon(true)
        .setNameFormat("bulk-remediation-%d")
        .build();
    ThreadPoolExecutor executor = new TenantThreadPoolExecutor(
        BULK_REMEDIATION_PARALLELISM,
        BULK_REMEDIATION_PARALLELISM,
        0L, TimeUnit.SECONDS,
        // Unbounded queue: submission never rejects, so we can queue the whole batch up front and let
        // workers drain it. Bounded by MAX_BULK_COMPONENTS in practice.
        new LinkedBlockingQueue<>(),
        threadFactory,
        new ThreadPoolExecutor.AbortPolicy(),
        "bulk_component_remediation",
        "ApiBulkComponentRemediation");

    try {
      List<Future<ApiBulkComponentRemediationResultDTO>> futures = new ArrayList<>(componentDTOs.size());
      for (final ApiComponentDTOV2 componentDTO : componentDTOs) {
        if (componentDTO == null) {
          // Cleaner than letting a null fall through to validateRequest. The completed-future wrapper
          // preserves input order in the results list.
          futures.add(CompletableFuture.completedFuture(
              new ApiBulkComponentRemediationResultDTO((ApiComponentDTOV2) null,
                  "Component must not be null.")));
          continue;
        }
        futures.add(executor.submit(() -> {
          try {
            ApiComponentRemediationDTO remediation = getSuggestedRemediationForComponentNoAuthz(componentDTO, ownerType,
                ownerId, effectiveStageId, identificationSource, scanId, includeParentRemediation, false);
            return new ApiBulkComponentRemediationResultDTO(componentDTO,
                remediation == null ? null : remediation.remediation);
          }
          catch (InvalidComponentException | InvalidPackageURLException e) {
            // Only per-component input-validation failures become per-item errors. Anything else
            // (batch-level BadRequestException, downstream failures, unexpected exceptions) propagates
            // through ExecutionException and fails the whole batch — the correct HTTP response, not
            // N identical 200-response per-item errors. InvalidPackageURLException is caught alongside
            // InvalidComponentException because the single-component endpoint lets it propagate (its
            // @HttpStatusCode(400) is what surfaces the 400) — here we treat it as another per-item
            // input error rather than a batch failure.
            return new ApiBulkComponentRemediationResultDTO(componentDTO, e.getMessage());
          }
        }));
      }

      // Walk futures in input order under a wall-clock deadline. Bounds worst-case latency if a
      // component's downstream call hangs.
      final long deadlineNanos = System.nanoTime() + BULK_REQUEST_DEADLINE.toNanos();
      List<ApiBulkComponentRemediationResultDTO> results = new ArrayList<>(componentDTOs.size());
      try {
        for (Future<ApiBulkComponentRemediationResultDTO> f : futures) {
          long remainingNanos = deadlineNanos - System.nanoTime();
          if (remainingNanos <= 0) {
            throw new TimeoutException("Bulk request exceeded deadline of " + BULK_REQUEST_DEADLINE);
          }
          results.add(f.get(remainingNanos, TimeUnit.NANOSECONDS));
        }
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Interrupted while evaluating bulk component remediation.", e);
      }
      catch (ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException) {
          throw (RuntimeException) cause;
        }
        throw new RuntimeException(cause);
      }
      catch (TimeoutException e) {
        throw new WebApplicationException(
            "Bulk component remediation exceeded the " + BULK_REQUEST_DEADLINE.toSeconds() + "s deadline.",
            Response.status(Status.SERVICE_UNAVAILABLE).build());
      }
      return new ApiBulkComponentRemediationDTO(results);
    }
    finally {
      // Terminates worker threads. shutdownNow also drops any tasks still in the queue and interrupts
      // in-flight tasks (best-effort — Apache HttpClient sockets may not honor the interrupt, but the
      // executor threads themselves die once their current task returns).
      executor.shutdownNow();
    }
  }

  /**
   * Validates parameters that apply to the entire batch and are constant across every component. Runs on
   * the request thread so batch-level failures produce a clean HTTP 400 rather than being swallowed into
   * per-item errors.
   *
   * @return the effective stageId to use for evaluation (may differ from the input for repositories,
   *         which default to {@link ProxyStageType#ID}).
   */
  private String validateBatchLevelParams(final OwnerType ownerType, String stageId, final String scanId) {
    if (OwnerType.REPOSITORY.equals(ownerType)) {
      // scanId is never allowed for repositories, regardless of stageId. Check first so we don't
      // accidentally short-circuit the check when stageId is omitted.
      if (!StringUtils.isBlank(scanId)) {
        throw new BadRequestException("The scan ID is not allowed for repositories.");
      }
      if (stageId == null) {
        return ProxyStageType.ID;
      }
      if (!ProxyStageType.ID.equals(stageId)) {
        throw new BadRequestException("Invalid stage ID for repositories: " + stageId + ".");
      }
      return stageId;
    }
    if (stageId != null && StageTypes.getById(stageId) == null) {
      throw new BadRequestException("Invalid stage ID: " + stageId + ".");
    }
    return stageId;
  }

  /**
   * ownerId-only entry for callers that have already checked authorization; resolves the
   * {@link OwnerType} via {@link OwnerDAO}. Returns null for an unknown ownerId.
   */
  public ApiComponentRemediationDTO getSuggestedRemediationForComponentNoAuthz(
      ApiComponentDTOV2 componentDTO,
      final String ownerId,
      String stageId,
      final String identificationSource,
      final String scanId,
      final Boolean includeParentRemediation,
      final boolean stableVersionsOnly)
  {
    Owner owner = ownerDAO.getById(ownerId);
    if (owner == null) {
      return null;
    }
    return getSuggestedRemediationForComponentNoAuthz(componentDTO, owner.getType(), ownerId, stageId,
        identificationSource, scanId, includeParentRemediation, stableVersionsOnly);
  }

  /**
   * Prefer {@link #getSuggestedRemediationForComponent}
   * Use only when doing operations that have already checked authorization or running in a task
   * which does not have session/user attached
   */
  public ApiComponentRemediationDTO getSuggestedRemediationForComponentNoAuthz(
      ApiComponentDTOV2 componentDTO,
      final OwnerType ownerType,
      final String ownerId,
      String stageId,
      final String identificationSource,
      final String scanId,
      final Boolean includeParentRemediation,
      final boolean stableVersionsOnly)
  {
    ApiDependencyTreeSearcher apiDependencyTreeSearcher = new ApiDependencyTreeSearcher();
    if (OwnerType.REPOSITORY.equals(ownerType)) {
      if (stageId == null) {
        stageId = ProxyStageType.ID;
      }
      else if (!ProxyStageType.ID.equals(stageId)) {
        throw new BadRequestException("Invalid stage ID for repositories: " + stageId + ".");
      }

      if (!StringUtils.isBlank(scanId)) {
        throw new BadRequestException("The scan ID is not allowed for repositories.");
      }
    }
    else if (stageId != null && StageTypes.getById(stageId) == null) {
      throw new BadRequestException("Invalid stage ID: " + stageId + ".");
    }

    boolean includeParentRem = includeParentRemediation != null && includeParentRemediation;

    boolean isThirdPartySource =
        IdentificationSource.isThirdPartyIdentificationSource(identificationSource);

    ComponentIdentifier componentIdentifier = validateRequest(componentDTO, isThirdPartySource);

    ComponentSummary componentSummary;

    if (scanId != null && isThirdPartySource) {
      componentSummary = thirdPartyComponentDAO.getComponentSummary(componentIdentifier, ownerId, scanId);
    }
    else {
      componentSummary = getComponentSummary(componentIdentifier);
    }

    // Do not allow an empty or invalid version at this time. Per-component input error — thrown as
    // InvalidComponentException so the bulk endpoint can surface it as a per-item error instead of
    // failing the whole batch.
    if (!componentSummary.isKnown()) {
      throw new InvalidComponentException("Invalid Component Identifier or packageUrl");
    }

    Owner owner = idUtils.getOwnerNotNull(ownerType, ownerId);
    // For performance, it's very important to use only one instance of ComponentDetailsLoader.
    // See https://sonatype.atlassian.net/browse/CLM-28129
    ComponentDetailsLoader componentDetailsLoader = componentDetailsLoaderFactory.newInstance(owner);

    List<ComponentDetailsDTO> dtos = new ArrayList<>();
    Map<ComponentIdentifier, List<ComponentDetailsDTO>> parentComponentsToVersionsMap = new HashMap<>();

    List<ComponentIdentifier> directParentComponentIdentifiers = Collections.emptyList();

    if (includeParentRem) {
      directParentComponentIdentifiers =
          getDirectParentComponentIdentifiers(apiDependencyTreeSearcher, componentDTO, ownerType, ownerId, scanId,
              componentIdentifier);
    }

    if (directParentComponentIdentifiers.isEmpty()) {
      dtos = componentInfoService.getComponentDetailsForAllVersionsNoAuth(owner, componentIdentifier, stageId,
          identificationSource, scanId, null, componentDetailsLoader, stableVersionsOnly).getLeft();
    }
    else {
      Map<ComponentIdentifier, List<ComponentDetailsDTO>> componentDetailsForAllVersions =
          componentInfoService.getComponentDetailsForAllVersionsNoAuthBulk(owner, directParentComponentIdentifiers,
              stageId, scanId, componentDetailsLoader, stableVersionsOnly);
      parentComponentsToVersionsMap =
          mapComponentsAllVersionsFromBulk(componentDetailsForAllVersions, directParentComponentIdentifiers);
    }

    ApiComponentRemediationValueDTO remediationValueDto;
    if (isThirdPartySource) {
      remediationValueDto = thirdPartyComponentDAO.getSuggestedRemmediation(owner.getId(), componentIdentifier, scanId);
    }
    else {
      if (parentComponentsToVersionsMap.isEmpty()) {
        remediationValueDto = componentRemediationService.getSuggestedRemediation(componentIdentifier, dtos, owner,
            stageId, componentDetailsLoader, API_COMPONENT_REMEDIATION);

        if (includeParentRem && apiDependencyTreeSearcher.isDirectNode()) {
          remediationValueDto.versionChanges.forEach(it -> it.setDirectDependency(true));
        }
      }
      else {
        remediationValueDto =
            componentRemediationService.getSuggestedRemediationForTransitive(parentComponentsToVersionsMap,
                componentDTO.componentIdentifier, owner, stageId, componentDetailsLoader);
      }
    }

    return remediationValueDto == null ? null : new ApiComponentRemediationDTO(remediationValueDto);
  }

  private ComponentIdentifier validateRequest(ApiComponentDTOV2 componentDTO, boolean isThirdParty) {
    if (componentDTO == null || (componentDTO.componentIdentifier == null && componentDTO.packageUrl == null)) {
      throw new InvalidComponentException("One of either componentIdentifier or packageUrl must be supplied.");
    }

    if (componentDTO.componentIdentifier != null) {
      return validateComponentIdentifier(componentDTO, isThirdParty);
    }
    else {
      return validatePackageUrl(componentDTO);
    }
  }

  private ComponentIdentifier validateComponentIdentifier(ApiComponentDTOV2 componentDTO, boolean isThirdParty) {
    if (componentDTO.componentIdentifier == null) {
      throw new InvalidComponentException("ComponentIdentifier must be supplied.");
    }
    try {
      ComponentIdentifier componentIdentifier = componentDTO.componentIdentifier.toComponentIdentifier();
      if (!isThirdParty) {
        // The complete identifier is not required to determine the suggested remediation for third party components
        componentIdentifier.ensureComplete();
      }
      return componentIdentifier;
    }
    catch (InvalidComponentIdentifierException e) {
      throw new InvalidComponentException(e.getMessage(), e);
    }
  }

  private ComponentIdentifier validatePackageUrl(ApiComponentDTOV2 componentDTO) {
    return new PackageUrlIdentifier(componentDTO.packageUrl).ensureCompleteIdentifier();
  }

  private ComponentSummary getComponentSummary(final ComponentIdentifier componentIdentifier) {
    Map<String, String> queryParams = Collections.singletonMap("componentIdentifier",
        ComponentIdentifierAdapter.toJson(componentIdentifier));
    return hdsClient.get(ComponentSummary.class, "rest/component/summary", queryParams);
  }

  /**
   * Using a list of component identifiers, and a map of package identifiers to a list of
   * details for versions of this package, produce a map from the identifier itself to the
   * details.
   *
   * Note that the pkgBasedMap only contains versions greater than or equal to the component
   * identifier in the provided componentIdentifiers list. We never look back.
   *
   * @param pkgBasedMap Map of component identifiers *without* versions to details lists
   * @param componentIdentifiers List of identifiers we are checking
   * @return A map from a versioned component identifier to details of versions greater or
   *         equal to the key's version.
   */
  public Map<ComponentIdentifier, List<ComponentDetailsDTO>> mapComponentsAllVersionsFromBulk(
      Map<ComponentIdentifier, List<ComponentDetailsDTO>> pkgBasedMap,
      List<ComponentIdentifier> componentIdentifiers)
  {
    Map<ComponentIdentifier, List<ComponentDetailsDTO>> resultMap = new HashMap<>();
    for (ComponentIdentifier identifier : componentIdentifiers) {
      ComponentIdentifier packageIdentifier = identifier.createAlternativeVersion(null);
      if (pkgBasedMap.containsKey(packageIdentifier)) {
        resultMap.computeIfAbsent(identifier, k -> new ArrayList<>()).addAll(pkgBasedMap.get(packageIdentifier));
      }
    }

    return resultMap;
  }

  private List<ComponentIdentifier> getDirectParentComponentIdentifiers(
      final ApiDependencyTreeSearcher apiDependencyTreeSearcher,
      final ApiComponentDTOV2 componentDTO,
      final OwnerType ownerType,
      final String ownerId,
      final String scanId,
      final ComponentIdentifier componentIdentifier)
  {
    List<ComponentIdentifier> directParentComponentIdentifiers = Collections.emptyList();

    if (ownerType.equals(OwnerType.APPLICATION) && scanId != null && componentIdentifier.isMaven()) {

      Application application = applicationDAO.getByIdNotNull(ownerId);
      try {
        ApiDependencyTreeNodeDTO dependencyTree = apiReportDataServiceV2.getDependencyTreeNoAuth(application, scanId);
        Set<ApiDependencyTreeNodeDTO> directParents =
            apiDependencyTreeSearcher.findAllDirectParents(dependencyTree, componentDTO.componentIdentifier);
        if (!directParents.isEmpty()) {
          directParentComponentIdentifiers = directParents.stream()
              .map(node -> node.getComponentIdentifier().toComponentIdentifier())
              .distinct()
              .collect(Collectors.toList());
        }
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return directParentComponentIdentifiers;
  }
}
