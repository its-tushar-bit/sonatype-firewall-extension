/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.CascadeComponentProgressDTO;
import com.sonatype.insight.brain.api.v2.dto.CascadeReevaluateTicketDTO;
import com.sonatype.insight.brain.api.v2.dto.CascadeStatusResponseDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.repository.ReevaluateCascadeProgressDAO;
import com.sonatype.insight.brain.dataaccess.repository.ReevaluateCascadeRequestDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeProgress;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeRequest;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeRequestStatus;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.repository.CascadeReevaluationTask;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.utils.ExecutorThreadPools;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.repository.ReevaluateCascadeProgressStatus.FAILED;

/**
 * Service for managing cascade re-evaluation operations across repository hierarchies.
 *
 * @since 1.196
 */
@Named
@Singleton
public class ApiFirewallCascadeService
{
  private static final Logger log = LoggerFactory.getLogger(ApiFirewallCascadeService.class);

  private final ReevaluateCascadeRequestDAO reevaluateCascadeRequestDAO;

  private final ReevaluateCascadeProgressDAO reevaluateCascadeProgressDAO;

  private final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  private final RepositoryDAO repositoryDAO;

  private final CurrentUser currentUser;

  private final RepositoryPolicyEvaluator repositoryPolicyEvaluator;

  private final ProductLicense productLicense;

  @Inject
  public ApiFirewallCascadeService(
      final ReevaluateCascadeRequestDAO reevaluateCascadeRequestDAO,
      final ReevaluateCascadeProgressDAO reevaluateCascadeProgressDAO,
      final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO,
      final RepositoryDAO repositoryDAO,
      final CurrentUser currentUser,
      final RepositoryPolicyEvaluator repositoryPolicyEvaluator,
      final ProductLicense productLicense)
  {
    this.reevaluateCascadeRequestDAO = reevaluateCascadeRequestDAO;
    this.reevaluateCascadeProgressDAO = reevaluateCascadeProgressDAO;
    this.proxyRepositoryComponentDAO = proxyRepositoryComponentDAO;
    this.repositoryDAO = repositoryDAO;
    this.currentUser = currentUser;
    this.repositoryPolicyEvaluator = repositoryPolicyEvaluator;
    this.productLicense = productLicense;
  }

  /**
   * Initiates a cascade re-evaluation for the given component hash across all accessible repositories.
   */
  public CascadeReevaluateTicketDTO initiateCascadeReevaluation(final String componentHash) {
    checkProductLicense();
    checkEvaluateComponentPermission(RepositoryContainer.SINGLETON);
    validateInputs(componentHash);

    String currentUsername = currentUser.getUserPrincipal().getUsername();

    ReevaluateCascadeRequest cascadeRequest =
        new ReevaluateCascadeRequest(componentHash, currentUsername, ReevaluateCascadeRequestStatus.PENDING);

    reevaluateCascadeRequestDAO.insert(cascadeRequest);

    String cascadeRequestId = cascadeRequest.getId();

    launchAsyncProcessing(cascadeRequestId, componentHash);

    CascadeReevaluateTicketDTO responseDTO = new CascadeReevaluateTicketDTO();
    responseDTO.statusUrl = PublicApiPaths.FIREWALL_CASCADE_REEVALUATE_PATH + "/status/" + cascadeRequestId;

    log.info("Initiated cascade re-evaluation for component {}. Request ID: {}",
        componentHash, cascadeRequestId);

    return responseDTO;
  }

  private void validateInputs(final String componentHash) {
    if (StringUtils.isBlank(componentHash)) {
      throw new BadRequestException("Component hash is required");
    }
  }

  private void launchAsyncProcessing(final String cascadeRequestId, final String componentHash) {
    Executor executor = ExecutorThreadPools.getInstance().getThreadPool(ExecutorThreadPools.ThreadPools.GENERAL);

    AuditData.get()
        .continueAsync(executor,
            new CascadeReevaluationTask(cascadeRequestId, componentHash,
                reevaluateCascadeProgressDAO, reevaluateCascadeRequestDAO,
                proxyRepositoryComponentDAO, repositoryPolicyEvaluator));
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  void checkEvaluateComponentPermission(@SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.OWNER) Owner owner) {
  }

  private void checkProductLicense() {
    if (!productLicense.hasFeature(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE) ||
        !productLicense.hasFeature(LicensedFeature.RELEASE_INTEGRITY))
    {
      throw new InvalidLicenseException();
    }
  }

  /**
   * Gets the status of a cascade re-evaluation request.
   */
  public CascadeStatusResponseDTO getCascadeStatus(final String requestId) {
    checkProductLicense();
    checkEvaluateComponentPermission(RepositoryContainer.SINGLETON);

    if (StringUtils.isBlank(requestId)) {
      throw new BadRequestException("requestId is required");
    }

    // Validate the cascade request exists
    ReevaluateCascadeRequest cascadeRequest = reevaluateCascadeRequestDAO.getById(requestId);
    if (cascadeRequest == null) {
      throw new NotFoundException("Cascade request not found: " + requestId);
    }

    // Get all progress entries for this request
    List<ReevaluateCascadeProgress> progressEntries = reevaluateCascadeProgressDAO.getByRequestId(requestId);

    // Batch fetch repository manager IDs to avoid N+1 queries
    Set<String> repositoryIds = progressEntries.stream()
        .map(ReevaluateCascadeProgress::getRepositoryId)
        .collect(Collectors.toSet());

    Map<String, String> repositoryToManagerIdMap = repositoryIds.isEmpty()
        ? Map.of()
        : repositoryDAO.getByIds(repositoryIds)
            .stream()
            .collect(Collectors.toMap(Repository::getId, Repository::getRepositoryManagerId));

    // Create response DTO
    CascadeStatusResponseDTO response = new CascadeStatusResponseDTO();
    response.referenceComponentHash = cascadeRequest.getComponentReferenceHash();

    // Separate completed/failed entries from pending entries
    for (ReevaluateCascadeProgress progress : progressEntries) {
      CascadeComponentProgressDTO componentProgress = createComponentProgressDTO(progress, repositoryToManagerIdMap);

      switch (progress.getStatus()) {
        case PENDING:
          response.pending.add(componentProgress);
          break;
        case COMPLETED:
          response.evaluated.add(componentProgress);
          break;
        case FAILED:
          response.failed.add(componentProgress);
          break;
        default:
          log.warn("Progress status not recognized: {}", progress.getStatus());
      }
    }

    // Determine overall status
    response.status = cascadeRequest.getStatus();

    return response;
  }

  private CascadeComponentProgressDTO createComponentProgressDTO(
      ReevaluateCascadeProgress progress,
      Map<String, String> repositoryToManagerIdMap)
  {
    CascadeComponentProgressDTO dto = new CascadeComponentProgressDTO();
    dto.repositoryId = progress.getRepositoryId();
    dto.componentId = progress.getProxyRepositoryComponentId();
    if (progress.getStatus() == FAILED) {
      dto.quarantined = null;
    }
    else {
      dto.quarantined = progress.isQuarantined();
    }
    // Look up repository manager ID from pre-fetched map
    dto.repositoryManagerId = repositoryToManagerIdMap.get(progress.getRepositoryId());

    return dto;
  }
}
