/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics;

import java.util.Collection;
import java.util.HashMap;
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

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.organization.OrganizationService;
import com.sonatype.insight.brain.repository.RepositoryService;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.security.AuthzFilter.Context;
import com.sonatype.insight.brain.security.AuthorizationChecker;
import com.sonatype.insight.brain.security.CurrentUser;

import org.apache.commons.collections4.CollectionUtils;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;

/**
 * Resolves the owner subtree used to scope waiver SQL counts (CLM-40927).
 * <p>
 * Mirrors {@link com.sonatype.insight.brain.dashboard.PolicyWaiverService} /
 * {@link com.sonatype.insight.brain.dashboard.DashboardPolicyWaiverRequestService} owner resolution so metrics
 * waivers match the classic dashboard RBAC model. Returns an empty set when the caller has no readable owners
 * (fail closed).
 */
@Named
@Singleton
public class DashboardMetricsWaiverScopeService
{
  private static final Collector<Owner, ?, Map<String, Owner>> ownerCollector =
      Collectors.toMap(Owner::getId, Function.identity(), (existing, replacement) -> existing);

  private final OrganizationDAO organizationDAO;

  private final OrganizationService organizationService;

  private final OwnerDAO ownerDAO;

  private final RepositoryManagerDAO repositoryManagerDAO;

  private final RepositoryService repositoryService;

  private final AuthorizationChecker authorizationChecker;

  private final CurrentUser currentUser;

  @Inject
  public DashboardMetricsWaiverScopeService(
      OrganizationDAO organizationDAO,
      OrganizationService organizationService,
      OwnerDAO ownerDAO,
      RepositoryManagerDAO repositoryManagerDAO,
      RepositoryService repositoryService,
      AuthorizationChecker authorizationChecker,
      CurrentUser currentUser)
  {
    this.organizationDAO = organizationDAO;
    this.organizationService = organizationService;
    this.ownerDAO = ownerDAO;
    this.repositoryManagerDAO = repositoryManagerDAO;
    this.repositoryService = repositoryService;
    this.authorizationChecker = authorizationChecker;
    this.currentUser = currentUser;
  }

  /**
   * Returns owner ids the current user may count waivers for. Empty when none (fail closed).
   * <p>
   * Applies {@code organizationIds} / {@code applicationIds} / {@code tagIds} from the metrics request.
   * Organization filters are expanded to descendant org ids (inclusive), consistent with index-backed metrics.
   * {@code stageIds} are not applicable to waivers and are ignored.
   */
  public Set<String> resolveAccessibleOwnerIds(DashboardMetricsRequestDTO request) {
    Set<String> requestOrganizationIds = request == null ? null : request.organizationIds;
    Set<String> organizationIds = expandOrganizationFilter(requestOrganizationIds);
    if (requestOrganizationIds != null && !requestOrganizationIds.isEmpty()
        && organizationIds != null && organizationIds.isEmpty())
    {
      return Set.of();
    }
    Set<String> applicationIds = request == null ? null : request.applicationIds;
    Set<String> tagIds = request == null ? null : request.tagIds;

    return resolveOwners(organizationIds, applicationIds, tagIds).keySet();
  }

  private Set<String> expandOrganizationFilter(Set<String> organizationIds) {
    if (organizationIds == null || organizationIds.isEmpty()) {
      return organizationIds;
    }
    return organizationDAO.getAllChildOrganizationIds(organizationIds);
  }

  private Map<String, Owner> resolveOwners(
      Set<String> organizationIds,
      Set<String> applicationIds,
      Set<String> tagIds)
  {
    Map<String, Owner> owners = new HashMap<>();

    BooleanSupplier isOwnerFilterEmpty = () -> CollectionUtils.isEmpty(organizationIds)
        && CollectionUtils.isEmpty(applicationIds)
        && CollectionUtils.isEmpty(tagIds);

    Map<String, ? extends Owner> lifeCycleOwners =
        getApplicationsAndOrgs(applicationIds, tagIds, organizationIds, isOwnerFilterEmpty);
    List<Repository> repositories = getRepositories(isOwnerFilterEmpty);
    List<RepositoryManager> repositoryManagers = getRepositoryManagers(
        repositories.stream().map(Repository::getId).collect(Collectors.toSet()), isOwnerFilterEmpty);

    owners.putAll(lifeCycleOwners);
    owners.putAll(repositories.stream().collect(ownerCollector));
    owners.putAll(repositoryManagers.stream().collect(ownerCollector));

    if (shouldIncludeRepositoryContainer(repositories, isOwnerFilterEmpty)) {
      owners.put(RepositoryContainer.REPOSITORY_CONTAINER_ID, RepositoryContainer.SINGLETON);
      owners.computeIfAbsent(ROOT_ORGANIZATION_ID, organizationDAO::getById);
    }

    return owners;
  }

  private Map<String, ? extends Owner> getApplicationsAndOrgs(
      Set<String> applicationIds,
      Set<String> tagIds,
      Set<String> organizationIds,
      BooleanSupplier isOwnerFilterEmpty)
  {
    Collection<? extends Owner> readableOwners =
        getApplicationsAndOrgsFilteredByAuthz(applicationIds, tagIds, organizationIds, isOwnerFilterEmpty);

    Map<String, Organization> parentOrgs =
        organizationService.getAllParentOrgsNoAuthz(readableOwners, null, null);

    Map<String, Owner> ownersByIdMap = readableOwners.stream().collect(ownerCollector);
    ownersByIdMap.putAll(parentOrgs);
    return ownersByIdMap;
  }

  private Collection<? extends Owner> getApplicationsAndOrgsFilteredByAuthz(
      Set<String> applicationIds,
      Set<String> tagIds,
      Set<String> organizationIds,
      BooleanSupplier isOwnerFilterEmpty)
  {
    List<? extends Owner> owners;
    if (isOwnerFilterEmpty.getAsBoolean()) {
      owners = ownerDAO.getAllAppsAndOrgs();
    }
    else {
      owners = ownerDAO.getOwnersByAppTagsAndOrgs(applicationIds, tagIds, organizationIds);
    }
    UserPrincipal user = currentUser.getUserPrincipal();
    return authorizationChecker.filterByPermission(user, Permission.READ, owners);
  }

  private List<Repository> getRepositories(BooleanSupplier isOwnerFilterEmpty) {
    if (isOwnerFilterEmpty.getAsBoolean()) {
      return repositoryService.getRepositoriesWithReadPermissionByIds(null);
    }
    return List.of();
  }

  @AuthzFilter(permission = Permission.READ, context = Context.REPOSITORY_MANAGER)
  List<RepositoryManager> getRepositoryManagersWithReadPermission(List<RepositoryManager> repositoryManagers) {
    return repositoryManagers;
  }

  private List<RepositoryManager> getRepositoryManagers(Set<String> repositoryIds, BooleanSupplier isOwnerFilterEmpty) {
    if (CollectionUtils.isNotEmpty(repositoryIds)) {
      return repositoryManagerDAO.getByRepositoryIds(repositoryIds);
    }
    if (isOwnerFilterEmpty.getAsBoolean()) {
      return getRepositoryManagersWithReadPermission(repositoryManagerDAO.getAll());
    }
    return List.of();
  }

  private boolean shouldIncludeRepositoryContainer(
      List<Repository> repositories,
      BooleanSupplier isOwnerFilterEmpty)
  {
    if (!repositories.isEmpty()) {
      return true;
    }
    return isOwnerFilterEmpty.getAsBoolean() && repositoryService.checkReadPermissionRepositoryContainer();
  }
}
