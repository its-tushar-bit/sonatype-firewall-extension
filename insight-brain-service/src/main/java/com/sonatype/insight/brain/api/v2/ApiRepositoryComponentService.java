/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.repository.HostedComponentScanQueueDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.repository.ProxyRepositoryComponentDeleteService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for hosted repository component cleanup operations.
 *
 * @since 1.203
 */
@Named
@Singleton
public class ApiRepositoryComponentService
{
  private static final Logger log = LoggerFactory.getLogger(ApiRepositoryComponentService.class);

  private static final int DELETE_BATCH_SIZE = 100;

  private final RepositoryManagerDAO repositoryManagerDAO;

  private final RepositoryDAO repositoryDAO;

  private final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  private final HostedComponentScanQueueDAO hostedComponentScanQueueDAO;

  private final ProxyRepositoryComponentDeleteService proxyRepositoryComponentDeleteService;

  @Inject
  public ApiRepositoryComponentService(
      final RepositoryManagerDAO repositoryManagerDAO,
      final RepositoryDAO repositoryDAO,
      final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO,
      final HostedComponentScanQueueDAO hostedComponentScanQueueDAO,
      final ProxyRepositoryComponentDeleteService proxyRepositoryComponentDeleteService)
  {
    this.repositoryManagerDAO = repositoryManagerDAO;
    this.repositoryDAO = repositoryDAO;
    this.proxyRepositoryComponentDAO = proxyRepositoryComponentDAO;
    this.hostedComponentScanQueueDAO = hostedComponentScanQueueDAO;
    this.proxyRepositoryComponentDeleteService = proxyRepositoryComponentDeleteService;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void deleteComponents(
      final String repositoryManagerInstanceId,
      final List<String> componentIds)
  {
    if (componentIds == null || componentIds.isEmpty()) {
      return;
    }

    RepositoryManager repositoryManager = repositoryManagerDAO.getByInstanceIdNotNull(repositoryManagerInstanceId);

    List<String> uniqueComponentIds = componentIds.stream()
        .filter(Objects::nonNull)
        .distinct()
        .collect(Collectors.toList());

    // Validate all components upfront before deleting any, to avoid partial deletes on failure.
    // componentIds are the short NXRM-assigned IDs stored in the component_id column, not the
    // internal proxy_repository_component_id PK — use getByNxrmComponentId for the lookup.
    List<ProxyRepositoryComponent> components = uniqueComponentIds.stream()
        .map(componentId -> {
          ProxyRepositoryComponent component = proxyRepositoryComponentDAO.getByNxrmComponentId(componentId);
          if (component == null) {
            throw new NotFoundException("ProxyRepositoryComponent with ID " + componentId + " does not exist.");
          }
          Repository repository = repositoryDAO.getByIdNotNull(component.getRepositoryId());
          if (!repositoryManager.getId().equals(repository.getRepositoryManagerId())) {
            throw new NotFoundException(
                "Component " + componentId + " not found for repository manager " + repositoryManagerInstanceId);
          }
          if (repository.getRepositoryType() != RepositoryType.hosted) {
            throw new NotFoundException(
                "Component " + componentId + " does not belong to a hosted repository");
          }
          return component;
        })
        .collect(Collectors.toList());

    log.info("Deleting {} hosted repository components for RM {}", components.size(),
        repositoryManagerInstanceId);

    List<String> nxrmAssetIds = components.stream()
        .map(ProxyRepositoryComponent::getComponentId)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    try (TransactionContext tx = hostedComponentScanQueueDAO.createTransactionContext()) {
      tx.begin();
      warnIfAnyInProgressJobsExist(tx, nxrmAssetIds);
      int deleted = hostedComponentScanQueueDAO.deletePendingByComponentIds(tx, nxrmAssetIds);
      tx.commit();
      if (deleted > 0) {
        log.debug("Deleted {} pending scan queue entries for {} components", deleted, nxrmAssetIds.size());
      }
    }
    components.forEach(proxyRepositoryComponentDeleteService::deleteComponent);

    log.info("Deleted {} hosted repository components for RM {}", components.size(),
        repositoryManagerInstanceId);
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void deleteRepositoryComponents(
      final String repositoryManagerInstanceId,
      final List<String> repositoryPublicIds)
  {
    if (repositoryPublicIds == null || repositoryPublicIds.isEmpty()) {
      return;
    }

    repositoryManagerDAO.getByInstanceIdNotNull(repositoryManagerInstanceId);

    List<String> uniqueRepositoryPublicIds = repositoryPublicIds.stream()
        .filter(Objects::nonNull)
        .distinct()
        .collect(Collectors.toList());

    // Validate all repositories upfront before deleting any, to avoid partial deletes on failure
    List<Repository> repositories = uniqueRepositoryPublicIds.stream()
        .map(publicId -> {
          Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicIdNotNull(
              repositoryManagerInstanceId, publicId);
          if (repository.getRepositoryType() != RepositoryType.hosted) {
            throw new NotFoundException("Repository " + publicId + " is not a hosted repository");
          }
          return repository;
        })
        .collect(Collectors.toList());

    for (Repository repository : repositories) {
      log.info("Deleting components from hosted repository {}:{}",
          repositoryManagerInstanceId, repository.getPublicId());

      // Purge the full PENDING backlog first; the per-component loop below misses not-yet-evaluated
      // entries that have no proxy_repository_component row (CLM-42122).
      int purged = hostedComponentScanQueueDAO.deletePendingByRepositoryId(repository.getId());
      if (purged > 0) {
        log.info("Purged {} pending scan queue entries for hosted repository {}:{}", purged,
            repositoryManagerInstanceId, repository.getPublicId());
      }

      int totalDeleted = 0;
      List<ProxyRepositoryComponent> batch;
      do {
        try (TransactionContext tx = hostedComponentScanQueueDAO.createTransactionContext()) {
          // Always fetch from offset 0 — prior batches have already been deleted from the table
          batch = proxyRepositoryComponentDAO.getByRepositoryId(tx, repository.getId(), DELETE_BATCH_SIZE, 0);
          if (batch.isEmpty()) {
            break;
          }
          List<String> batchComponentIds = batch.stream()
              .map(ProxyRepositoryComponent::getComponentId)
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
          tx.begin();
          warnIfAnyInProgressJobsExist(tx, batchComponentIds);
          int deleted = hostedComponentScanQueueDAO.deletePendingByComponentIds(tx, batchComponentIds);
          tx.commit();
          if (deleted > 0) {
            log.debug("Deleted {} pending scan queue entries for repository {}:{}", deleted,
                repositoryManagerInstanceId, repository.getPublicId());
          }
        }
        batch.forEach(proxyRepositoryComponentDeleteService::deleteComponent);
        totalDeleted += batch.size();
      }
      while (batch.size() == DELETE_BATCH_SIZE);

      log.info("Deleted {} components from hosted repository {}:{}",
          totalDeleted, repositoryManagerInstanceId, repository.getPublicId());
    }
  }

  private void warnIfAnyInProgressJobsExist(
      final TransactionContext tx,
      final List<String> componentIds)
  {
    if (hostedComponentScanQueueDAO.hasInProgressByComponentIds(tx, componentIds)) {
      log.warn(
          "One or more components have an IN_PROGRESS scan job at deletion time. The worker will attempt to record results for deleted components and fail.");
    }
  }
}
