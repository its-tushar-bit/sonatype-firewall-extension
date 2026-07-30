/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Date;
import java.util.EnumSet;
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
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.security.PermissionService;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for Lifecycle API operations
 *
 * @since 1.198
 */
@Named
@Singleton
public class ApiLifecycleService
{
  private static final Logger log = LoggerFactory.getLogger(ApiLifecycleService.class);

  private final RepositoryManagerDAO repositoryManagerDAO;

  private final RepositoryDAO repositoryDAO;

  private final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  private final PermissionService permissionService;

  @Inject
  public ApiLifecycleService(
      final RepositoryManagerDAO repositoryManagerDAO,
      final RepositoryDAO repositoryDAO,
      final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO,
      final PermissionService permissionService)
  {
    this.repositoryManagerDAO = repositoryManagerDAO;
    this.repositoryDAO = repositoryDAO;
    this.proxyRepositoryComponentDAO = proxyRepositoryComponentDAO;
    this.permissionService = permissionService;
  }

  public ApiLifecycleRepositoryManagerListDTO getRepositoryManagers() {
    requireAccess();
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
    Map<String, Date> lastScanTimeByRepoId = proxyRepositoryComponentDAO.getLastScanTimesByRepositoryIds(allRepoIds);

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

  /**
   * Authorize the caller to view the Hosted Repository Scanning configuration.
   * <p>
   * Allows System Administrator (via {@link Permission#CONFIGURE_SYSTEM} at global scope)
   * and Lifecycle roles — Policy Administrator, Owner, Developer, plus any custom role
   * granted {@link Permission#READ} — that hold READ on any owner (root org, org, or
   * application).
   * <p>
   * CONFIGURE_SYSTEM is a global permission so it's checked at the global context.
   * READ is non-global, so we check whether the user holds it on at least one owner
   * (root org / org / application) — matching the access model the Lifecycle UI uses.
   */
  private void requireAccess() {
    if (!isAuthenticated()) {
      throw new UnauthenticatedException("Authentication required");
    }
    if (!isAuthorized()) {
      throw new UnauthorizedException("Insufficient permissions");
    }
  }

  private boolean isAuthenticated() {
    try {
      return SecurityUtils.getSubject().isAuthenticated();
    }
    catch (Exception e) {
      log.debug("Error checking authentication status", e);
      return false;
    }
  }

  private boolean isAuthorized() {
    try {
      Subject subject = SecurityUtils.getSubject();
      return hasConfigureSystem(subject) || hasReadOnAnyOwner(subject);
    }
    catch (Exception e) {
      log.debug("Error checking authorization", e);
      return false;
    }
  }

  private boolean hasConfigureSystem(Subject subject) {
    return !permissionService.validatePermission(
        subject, OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID,
        EnumSet.of(Permission.CONFIGURE_SYSTEM)).isEmpty();
  }

  private boolean hasReadOnAnyOwner(Subject subject) {
    UserPrincipal user = (UserPrincipal) subject.getPrincipal();
    if (user == null) {
      return false;
    }
    return !permissionService.getContextIdsForUserWithPermission(user, Permission.READ).isEmpty();
  }
}
