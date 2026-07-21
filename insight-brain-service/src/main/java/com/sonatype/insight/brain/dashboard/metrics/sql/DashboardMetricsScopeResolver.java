/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics.sql;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dashboard.metrics.DashboardMetricsRequestDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.organization.OrganizationService;
import com.sonatype.insight.brain.repository.RepositoryService;
import com.sonatype.insight.brain.security.AuthzFilter.Context;
import com.sonatype.insight.brain.security.AuthorizationChecker;
import com.sonatype.insight.brain.security.CurrentUser;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.dashboard.metrics.sql.ResolvedScope.DenyReason.NO_ACCESS;
import static com.sonatype.insight.brain.dashboard.metrics.sql.ResolvedScope.DenyReason.RESOLUTION_FAILED;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;

/**
 * Resolves the authorization-scoped owner population used by dashboard SQL metrics.
 * <p>
 * The owner, repository, hierarchy, and authorization paths intentionally mirror the established
 * waiver scope resolution so SQL-backed dashboard metrics cannot widen RBAC scope.
 */
@Named
@Singleton
public class DashboardMetricsScopeResolver
{
  private static final Logger log = LoggerFactory.getLogger(DashboardMetricsScopeResolver.class);

  private static final Collector<Owner, ?, Map<String, Owner>> OWNER_COLLECTOR =
      Collectors.toMap(Owner::getId, Function.identity(), (existing, replacement) -> existing);

  private final OrganizationDAO organizationDAO;

  private final ApplicationDAO applicationDAO;

  private final OrganizationService organizationService;

  private final OwnerDAO ownerDAO;

  private final RepositoryManagerDAO repositoryManagerDAO;

  private final RepositoryService repositoryService;

  private final AuthorizationChecker authorizationChecker;

  private final CurrentUser currentUser;

  private final DashboardMetricsSqlTelemetry telemetry;

  @Inject
  public DashboardMetricsScopeResolver(
      final OrganizationDAO organizationDAO,
      final ApplicationDAO applicationDAO,
      final OrganizationService organizationService,
      final OwnerDAO ownerDAO,
      final RepositoryManagerDAO repositoryManagerDAO,
      final RepositoryService repositoryService,
      final AuthorizationChecker authorizationChecker,
      final CurrentUser currentUser,
      final DashboardMetricsSqlTelemetry telemetry)
  {
    this.organizationDAO = organizationDAO;
    this.applicationDAO = applicationDAO;
    this.organizationService = organizationService;
    this.ownerDAO = ownerDAO;
    this.repositoryManagerDAO = repositoryManagerDAO;
    this.repositoryService = repositoryService;
    this.authorizationChecker = authorizationChecker;
    this.currentUser = currentUser;
    this.telemetry = telemetry;
  }

  public ResolvedScope resolve(final DashboardMetricsRequestDTO request) {
    long startedAt = System.nanoTime();
    ResolvedScope scope = resolveFailClosed(request);
    if (scope.denyReason() == RESOLUTION_FAILED) {
      recordScopeResolutionFailure();
    }
    recordScopeResolution(System.nanoTime() - startedAt, scope.kind());
    return scope;
  }

  private void recordScopeResolutionFailure() {
    try {
      telemetry.recordScopeResolutionFailure();
    }
    catch (RuntimeException ignored) {
      // Telemetry is strictly best-effort and must not affect resolved scope.
    }
  }

  private void recordScopeResolution(final long durationNanos, final ResolvedScope.Kind kind) {
    try {
      telemetry.recordScopeResolution(durationNanos, kind);
    }
    catch (RuntimeException ignored) {
      // Telemetry is strictly best-effort and must not affect resolved scope.
    }
  }

  private ResolvedScope resolveFailClosed(final DashboardMetricsRequestDTO request) {
    try {
      Set<String> requestedOrganizationIds = request == null ? null : request.organizationIds;
      Set<String> expandedOrganizationIds = expandOrganizationFilter(requestedOrganizationIds);
      if (hasValues(requestedOrganizationIds) && CollectionUtils.isEmpty(expandedOrganizationIds)) {
        return ResolvedScope.denyAll(NO_ACCESS);
      }

      Set<String> applicationIds = request == null ? null : request.applicationIds;
      Set<String> tagIds = request == null ? null : request.tagIds;
      Set<String> daoTagIds = tagIds == null ? null : new HashSet<>(tagIds);
      OwnerResolution ownerResolution =
          resolveOwners(expandedOrganizationIds, applicationIds, daoTagIds);
      Map<String, Owner> owners = ownerResolution.owners();
      if (owners.isEmpty()) {
        return ResolvedScope.denyAll(NO_ACCESS);
      }

      Set<String> readableOrganizationIds = resolveAuthorizedOrganizationIds(
          ownerResolution.lifeCycleOwners(),
          expandedOrganizationIds,
          applicationIds,
          tagIds);
      Set<String> readableApplicationIds =
          resolveAuthorizedApplicationIds(owners.values(), expandedOrganizationIds);
      UserPrincipal user = currentUser.getUserPrincipal();
      boolean hasSupportedFilters =
          hasValues(requestedOrganizationIds) || hasValues(applicationIds) || hasValues(tagIds);

      return new ResolvedScope(
          !hasSupportedFilters && authorizationChecker.isPermitted(user, Permission.READ, Map.of())
              ? ResolvedScope.Kind.GLOBAL
              : ResolvedScope.Kind.RESTRICTED,
          null,
          owners.keySet(),
          ownerResolution.directOwnerIds(),
          readableOrganizationIds,
          readableApplicationIds,
          hasSupportedFilters);
    }
    catch (RuntimeException e) {
      log.error("Dashboard metric scope resolution failed closed", e);
      return ResolvedScope.denyAll(RESOLUTION_FAILED);
    }
  }

  private Set<String> resolveAuthorizedOrganizationIds(
      final Collection<? extends Owner> lifeCycleOwners,
      final Set<String> expandedOrganizationIds,
      final Set<String> applicationIds,
      final Set<String> tagIds)
  {
    // When app/tag filters are absent, lifeCycleOwners already came from the same owner DAO query
    // this method would run — reuse them to avoid a second getAllAppsAndOrgs / org-filter fetch.
    if (!hasValues(applicationIds) && !hasValues(tagIds)) {
      return filterOrganizations(
          ownerIdsOfType(lifeCycleOwners, OwnerType.ORGANIZATION),
          expandedOrganizationIds);
    }

    List<? extends Owner> candidates = hasValues(expandedOrganizationIds)
        ? ownerDAO.getOwnersByAppTagsAndOrgs(null, null, expandedOrganizationIds)
        : ownerDAO.getAllAppsAndOrgs();
    Collection<? extends Owner> readableOwners =
        authorizationChecker.filterByPermission(currentUser.getUserPrincipal(), Permission.READ, candidates);
    return filterOrganizations(
        ownerIdsOfType(readableOwners, OwnerType.ORGANIZATION),
        expandedOrganizationIds);
  }

  private Set<String> resolveAuthorizedApplicationIds(
      final Collection<? extends Owner> owners,
      final Set<String> expandedOrganizationIds)
  {
    Set<String> applicationIds = ownerIdsOfType(owners, OwnerType.APPLICATION);
    if (hasValues(expandedOrganizationIds)) {
      List<Application> organizationApplications = applicationDAO.getByOrganizationIds(expandedOrganizationIds);
      Collection<? extends Owner> readableApplications = authorizationChecker.filterByPermission(
          currentUser.getUserPrincipal(), Permission.READ, organizationApplications);
      applicationIds.addAll(ownerIdsOfType(readableApplications, OwnerType.APPLICATION));
    }
    return applicationIds;
  }

  private Set<String> expandOrganizationFilter(final Set<String> organizationIds) {
    if (!hasValues(organizationIds)) {
      return organizationIds;
    }
    return organizationDAO.getAllChildOrganizationIds(organizationIds);
  }

  private OwnerResolution resolveOwners(
      final Set<String> organizationIds,
      final Set<String> applicationIds,
      final Set<String> tagIds)
  {
    Map<String, Owner> owners = new HashMap<>();
    Set<String> directOwnerIds = new HashSet<>();
    BooleanSupplier isOwnerFilterEmpty = () -> CollectionUtils.isEmpty(organizationIds)
        && CollectionUtils.isEmpty(applicationIds)
        && CollectionUtils.isEmpty(tagIds);

    Collection<? extends Owner> lifeCycleOwners =
        getApplicationsAndOrgsFilteredByAuthz(applicationIds, tagIds, organizationIds, isOwnerFilterEmpty);
    Map<String, Organization> parentOrgs =
        organizationService.getAllParentOrgsNoAuthz(lifeCycleOwners, null, null);
    List<Repository> repositories = getRepositories(isOwnerFilterEmpty);
    List<RepositoryManager> repositoryManagers = getRepositoryManagers(
        repositories.stream().map(Repository::getId).collect(Collectors.toSet()), isOwnerFilterEmpty);

    owners.putAll(lifeCycleOwners.stream().collect(OWNER_COLLECTOR));
    owners.putAll(parentOrgs);
    owners.putAll(repositories.stream().collect(OWNER_COLLECTOR));
    owners.putAll(repositoryManagers.stream().collect(OWNER_COLLECTOR));
    directOwnerIds.addAll(lifeCycleOwners.stream().map(Owner::getId).collect(Collectors.toSet()));
    directOwnerIds.addAll(repositories.stream().map(Owner::getId).collect(Collectors.toSet()));
    directOwnerIds.addAll(repositoryManagers.stream().map(Owner::getId).collect(Collectors.toSet()));
    if (shouldIncludeRepositoryContainer(repositories, isOwnerFilterEmpty)) {
      owners.put(RepositoryContainer.REPOSITORY_CONTAINER_ID, RepositoryContainer.SINGLETON);
      owners.computeIfAbsent(ROOT_ORGANIZATION_ID, organizationDAO::getById);
      directOwnerIds.add(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    }
    return new OwnerResolution(owners, directOwnerIds, lifeCycleOwners);
  }

  private Collection<? extends Owner> getApplicationsAndOrgsFilteredByAuthz(
      final Set<String> applicationIds,
      final Set<String> tagIds,
      final Set<String> organizationIds,
      final BooleanSupplier isOwnerFilterEmpty)
  {
    List<? extends Owner> owners = isOwnerFilterEmpty.getAsBoolean()
        ? ownerDAO.getAllAppsAndOrgs()
        : ownerDAO.getOwnersByAppTagsAndOrgs(applicationIds, tagIds, organizationIds);
    return authorizationChecker.filterByPermission(currentUser.getUserPrincipal(), Permission.READ, owners);
  }

  private List<Repository> getRepositories(final BooleanSupplier isOwnerFilterEmpty) {
    return isOwnerFilterEmpty.getAsBoolean()
        ? repositoryService.getRepositoriesWithReadPermissionByIds(null)
        : List.of();
  }

  private List<RepositoryManager> getRepositoryManagers(
      final Set<String> repositoryIds,
      final BooleanSupplier isOwnerFilterEmpty)
  {
    if (CollectionUtils.isNotEmpty(repositoryIds)) {
      return repositoryManagerDAO.getByRepositoryIds(repositoryIds);
    }
    if (!isOwnerFilterEmpty.getAsBoolean()) {
      return List.of();
    }
    Collection<RepositoryManager> readableManagers = authorizationChecker.filterByPermission(
        currentUser.getUserPrincipal(),
        Permission.READ,
        repositoryManagerDAO.getAll(),
        Context.REPOSITORY_MANAGER);
    return List.copyOf(readableManagers);
  }

  private boolean shouldIncludeRepositoryContainer(
      final List<Repository> repositories,
      final BooleanSupplier isOwnerFilterEmpty)
  {
    return !repositories.isEmpty()
        || (isOwnerFilterEmpty.getAsBoolean() && repositoryService.checkReadPermissionRepositoryContainer());
  }

  private static Set<String> filterOrganizations(
      final Set<String> organizationIds,
      final Set<String> expandedOrganizationIds)
  {
    if (!hasValues(expandedOrganizationIds)) {
      return organizationIds;
    }
    return organizationIds.stream().filter(expandedOrganizationIds::contains).collect(Collectors.toSet());
  }

  private static Set<String> ownerIdsOfType(final Collection<? extends Owner> owners, final OwnerType type) {
    return owners.stream()
        .filter(owner -> owner.getType() == type)
        .map(Owner::getId)
        .collect(Collectors.toSet());
  }

  private static boolean hasValues(final Set<String> ids) {
    return CollectionUtils.isNotEmpty(ids);
  }

  private record OwnerResolution(
      Map<String, Owner> owners,
      Set<String> directOwnerIds,
      Collection<? extends Owner> lifeCycleOwners)
  {
  }
}
