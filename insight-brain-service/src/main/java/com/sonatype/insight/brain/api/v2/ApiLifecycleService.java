/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.google.common.primitives.Ints;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.api.v2.dto.ApiLifecycleRepositoryManagerDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiLifecycleRepositoryManagerDTO.ConnectionStatus;
import com.sonatype.insight.brain.api.v2.dto.ApiLifecycleRepositoryManagerListDTO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;

/**
 * Service for Lifecycle API operations
 *
 * @since 1.198
 */
@Named
@Singleton
public class ApiLifecycleService
{
  private final RepositoryManagerDAO repositoryManagerDAO;

  private final RepositoryDAO repositoryDAO;

  private final RepositoryComponentDAO repositoryComponentDAO;

  @Inject
  public ApiLifecycleService(
      final RepositoryManagerDAO repositoryManagerDAO,
      final RepositoryDAO repositoryDAO,
      final RepositoryComponentDAO repositoryComponentDAO)
  {
    this.repositoryManagerDAO = repositoryManagerDAO;
    this.repositoryDAO = repositoryDAO;
    this.repositoryComponentDAO = repositoryComponentDAO;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public ApiLifecycleRepositoryManagerListDTO getRepositoryManagers() {
    List<RepositoryManager> repositoryManagers = repositoryManagerDAO.getAll();

    List<Repository> allHostedRepositories =
        repositoryDAO.getByRepositoryType(RepositoryType.hosted);

    Map<String, Long> hostedRepoCountByRmId = allHostedRepositories.stream()
        .filter(Repository::isMonitoringEnabled)
        .filter(r -> r.getRepositoryManagerId() != null)
        .collect(Collectors.groupingBy(Repository::getRepositoryManagerId, Collectors.counting()));

    // Group monitored hosted repos by RM ID for last-activity computation
    Map<String, List<Repository>> reposByRmId = allHostedRepositories.stream()
        .filter(Repository::isMonitoringEnabled)
        .filter(r -> r.getRepositoryManagerId() != null)
        .collect(Collectors.groupingBy(Repository::getRepositoryManagerId));

    // Fetch last scan times for monitored repo IDs in one query
    List<String> allRepoIds = reposByRmId.values()
        .stream()
        .flatMap(List::stream)
        .map(Repository::getId)
        .filter(id -> id != null)
        .collect(Collectors.toList());
    Map<String, Date> lastScanTimeByRepoId = repositoryComponentDAO.getLastScanTimesByRepositoryIds(allRepoIds);

    // Compute last activity time per RM: MAX(lastScanTime, lastManualConfigureTime) across all repos
    Map<String, Long> lastActivityTimeByRmId = reposByRmId.entrySet()
        .stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> entry.getValue()
                .stream()
                .mapToLong(repo -> {
                  long scanTime = repo.getId() != null && lastScanTimeByRepoId.containsKey(repo.getId())
                      ? lastScanTimeByRepoId.get(repo.getId()).getTime()
                      : 0L;
                  long configureTime = repo.getLastManualConfigureTime() != null
                      ? repo.getLastManualConfigureTime().getTime()
                      : 0L;
                  return Math.max(scanTime, configureTime);
                })
                .max()
                .orElse(0L)));

    List<ApiLifecycleRepositoryManagerDTO> dtos = repositoryManagers.stream()
        .map(rm -> {
          int hostedRepoCount = Ints.saturatedCast(hostedRepoCountByRmId.getOrDefault(rm.getId(), 0L));
          Long lastActivity = lastActivityTimeByRmId.getOrDefault(rm.getId(), 0L);
          return toDTO(rm, hostedRepoCount, lastActivity > 0 ? lastActivity : null);
        })
        .collect(Collectors.toList());

    return new ApiLifecycleRepositoryManagerListDTO(dtos);
  }

  private ApiLifecycleRepositoryManagerDTO toDTO(
      final RepositoryManager rm,
      final int hostedRepoCount,
      final Long lastActivityTime)
  {
    ApiLifecycleRepositoryManagerDTO dto = new ApiLifecycleRepositoryManagerDTO();

    dto.id = rm.getId();
    dto.name = rm.getRawName();
    dto.instanceId = rm.getInstanceId();
    dto.baseUrl = rm.getBaseUrl();
    dto.hostedRepositoryCount = hostedRepoCount;
    dto.connectionStatus = determineConnectionStatus(rm);
    dto.lastActivityTime = lastActivityTime;

    return dto;
  }

  private ConnectionStatus determineConnectionStatus(RepositoryManager rm) {
    return rm.isConfigured() ? ConnectionStatus.CONNECTED : ConnectionStatus.DISCONNECTED;
  }
}
