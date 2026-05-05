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

import com.google.common.primitives.Ints;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.api.v2.dto.ApiLifecycleRepositoryManagerDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiLifecycleRepositoryManagerDTO.ConnectionStatus;
import com.sonatype.insight.brain.api.v2.dto.ApiLifecycleRepositoryManagerListDTO;
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

  @Inject
  public ApiLifecycleService(
      final RepositoryManagerDAO repositoryManagerDAO,
      final RepositoryDAO repositoryDAO)
  {
    this.repositoryManagerDAO = repositoryManagerDAO;
    this.repositoryDAO = repositoryDAO;
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

    List<ApiLifecycleRepositoryManagerDTO> dtos = repositoryManagers.stream()
        .map(rm -> toDTO(rm, Ints.saturatedCast(hostedRepoCountByRmId.getOrDefault(rm.getId(), 0L))))
        .collect(Collectors.toList());

    return new ApiLifecycleRepositoryManagerListDTO(dtos);
  }

  private ApiLifecycleRepositoryManagerDTO toDTO(RepositoryManager rm, int hostedRepoCount) {
    ApiLifecycleRepositoryManagerDTO dto = new ApiLifecycleRepositoryManagerDTO();

    dto.instanceId = rm.getInstanceId();
    dto.baseUrl = rm.getBaseUrl();
    dto.hostedRepositoryCount = hostedRepoCount;
    dto.connectionStatus = determineConnectionStatus(rm);

    return dto;
  }

  private ConnectionStatus determineConnectionStatus(RepositoryManager rm) {
    return rm.isConfigured() ? ConnectionStatus.CONNECTED : ConnectionStatus.DISCONNECTED;
  }
}
