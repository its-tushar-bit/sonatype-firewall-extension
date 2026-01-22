/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverReasonDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.OrganizationService;
import com.sonatype.insight.brain.repository.RepositoryService;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.security.AuthzFilter.Context;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.dashboard.ExpirationDate.ALL;
import static com.sonatype.insight.brain.dashboard.ExpirationDate.NEVER;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class DashboardPolicyWaiverRequestService
{
  private static final Logger log = LoggerFactory.getLogger(DashboardPolicyWaiverRequestService.class);

  private static final Collector<Owner, ?, Map<String, Owner>> ownerCollector =
      Collectors.toMap(Owner::getId, Function.identity(), (existing, replacement) -> existing);

  private final DashboardUtils dashboardUtils;

  private final PolicyDAO policyDAO;

  private final PolicyWaiverRequestDAO policyWaiverRequestDAO;

  private final RepositoryService repositoryService;

  private final OrganizationDAO organizationDAO;

  private final RepositoryManagerDAO repositoryManagerDAO;

  private final OrganizationService organizationService;

  private final OwnerDAO ownerDAO;

  private final PolicyWaiverReasonDAO policyWaiverReasonDAO;

  @Inject
  DashboardPolicyWaiverRequestService(
      final DashboardUtils dashboardUtils,
      final PolicyDAO policyDAO,
      final PolicyWaiverRequestDAO policyWaiverRequestDAO,
      final OrganizationDAO organizationDAO,
      final RepositoryManagerDAO repositoryManagerDAO,
      final RepositoryService repositoryService,
      final OrganizationService organizationService,
      final OwnerDAO ownerDAO,
      final PolicyWaiverReasonDAO policyWaiverReasonDAO)
  {
    this.dashboardUtils = dashboardUtils;
    this.policyDAO = policyDAO;
    this.policyWaiverRequestDAO = policyWaiverRequestDAO;
    this.organizationDAO = organizationDAO;
    this.repositoryManagerDAO = repositoryManagerDAO;
    this.repositoryService = repositoryService;
    this.organizationService = organizationService;
    this.ownerDAO = ownerDAO;
    this.policyWaiverReasonDAO = policyWaiverReasonDAO;
  }

  public DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> getDashboardPolicyWaiverRequests(
      RisksFilterDTO risksFilterDTO)
  {
    return getDashboardPolicyWaiverRequests(risksFilterDTO, false);
  }

  public DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> getDashboardPolicyWaiverRequestsForExport(
      RisksFilterDTO risksFilterDTO)
  {
    return getDashboardPolicyWaiverRequests(risksFilterDTO, true);
  }

  private DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> getDashboardPolicyWaiverRequests(
      RisksFilterDTO risksFilterDTO,
      boolean includeDetails)
  {
    if (risksFilterDTO == null) {
      throw new BadRequestException("Invalid filter supplied for request.");
    }

    dashboardUtils.validateDashboardLicensedAndEnabled();

    long start = System.currentTimeMillis();

    Set<String> organizationIds = risksFilterDTO.organizationIds;
    Set<String> applicationIds = risksFilterDTO.applicationIds;
    Set<String> tagIds = risksFilterDTO.tagIds;
    PolicyThreatCategoryFilter policyThreatCategories = risksFilterDTO.policyThreatCategories;
    PolicyThreatLevelFilter policyThreatLevelRange = risksFilterDTO.policyThreatLevelRange;
    ExpirationDate expirationDate = risksFilterDTO.expirationDate;
    String orderBy = risksFilterDTO.orderBy;
    Set<String> policyWaiverReasonIds = risksFilterDTO.policyWaiverReasonIds;

    // Verify orderBy early to prevent costly operations if it fails
    DashboardPolicyWaiverRequestDTOComparator dashboardPolicyWaiverRequestDTOComparator =
        verifyOrderByAndBuildComparator(orderBy);

    Map<String, Owner> owners = getOwners(organizationIds, applicationIds, tagIds, risksFilterDTO.repositoryIds);
    Map<String, Policy> filteredPoliciesById = getFilteredPoliciesById(policyThreatCategories, policyThreatLevelRange);
    DashboardPolicyWaiverRequestDTOAdapter dtoAdapter =
        new DashboardPolicyWaiverRequestDTOAdapter(filteredPoliciesById, owners, includeDetails);

    Predicate<PolicyWaiverRequest> filteringPredicate = getFilteringPredicateForPolicyWaiverRequests(owners.keySet())
        .and(getFilteringPredicateForExpirationDates(expirationDate))
        .and(getFilteringPredicateForWaiverReasons(policyWaiverReasonIds));

    Map<String, PolicyWaiverReason> waiverReasonIdToWaiverReason =
        policyWaiverReasonDAO.getPolicyWaiverReasonIdToPolicyWaiverReasonMap();

    List<DashboardPolicyWaiverRequestDTO> filteredWaiverRequestDTOs = new ArrayList<>();
    for (Policy policy : filteredPoliciesById.values()) {
      List<PolicyWaiverRequest> policyWaiverRequests =
          expirationDate.equals(ALL) ? policyWaiverRequestDAO.getByPolicyId(policy.getId())
              : policyWaiverRequestDAO.getActiveByPolicyId(policy.getId());
      List<DashboardPolicyWaiverRequestDTO> partialDTOs = filterPolicyWaiverRequestsAndBuildDTOs(policyWaiverRequests,
          filteringPredicate, dtoAdapter, waiverReasonIdToWaiverReason);
      filteredWaiverRequestDTOs.addAll(partialDTOs);
    }

    log.debug("getDashboardPolicyWaiverRequests: Found {} waiver requests after filters",
        filteredWaiverRequestDTOs.size());

    filteredWaiverRequestDTOs.sort(dashboardPolicyWaiverRequestDTOComparator);

    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> resultsDTO = new DashboardResultsDTO<>();
    if (filteredWaiverRequestDTOs.isEmpty()) {
      resultsDTO.dashboardResults = new ArrayList<>();
    }
    else {
      int page = risksFilterDTO.page;
      List<List<DashboardPolicyWaiverRequestDTO>> pages =
          Lists.partition(filteredWaiverRequestDTOs, risksFilterDTO.pageSize);
      resultsDTO.dashboardResults = page >= pages.size() ? new ArrayList<>() : pages.get(page);
      resultsDTO.hasNextPage = pages.size() > (page + 1);
    }

    log.debug("getDashboardPolicyWaiverRequests: Finished in {} ms", System.currentTimeMillis() - start);

    return resultsDTO;
  }

  private Map<String, Owner> getOwners(
      Set<String> organizationIds,
      Set<String> applicationIds,
      Set<String> tagIds,
      Set<String> repositoryIds)
  {
    Map<String, Owner> owners = new HashMap<>();

    boolean isOwnerFilterEmpty = CollectionUtils.isEmpty(organizationIds) && CollectionUtils.isEmpty(applicationIds)
        && CollectionUtils.isEmpty(tagIds) && CollectionUtils.isEmpty(repositoryIds);
    boolean hasRepoContainerReadPermission = repositoryService.checkReadPermissionRepositoryContainer();
    boolean ownerFilterIsEmptyAndRepoContainerReadPermission = isOwnerFilterEmpty && hasRepoContainerReadPermission;

    Map<String, ? extends Owner> lifeCycleOwners =
        getApplicationsAndOrgs(applicationIds, tagIds, organizationIds, isOwnerFilterEmpty);

    List<Repository> repositories = getRepositories(repositoryIds, isOwnerFilterEmpty);
    List<RepositoryManager> repositoryManagers = getRepositoryManagers(
        repositories.stream().map(Repository::getId).collect(Collectors.toSet()), isOwnerFilterEmpty);

    owners.putAll(lifeCycleOwners);
    owners.putAll(repositories.stream().collect(ownerCollector));
    owners.putAll(repositoryManagers.stream().collect(ownerCollector));

    if (ownerFilterIsEmptyAndRepoContainerReadPermission || !repositories.isEmpty()
        || (repositoryIds.contains(RepositoryContainer.REPOSITORY_CONTAINER_ID) && hasRepoContainerReadPermission)) {
      owners.put(RepositoryContainer.REPOSITORY_CONTAINER_ID, RepositoryContainer.SINGLETON);
      owners.computeIfAbsent(ROOT_ORGANIZATION_ID, organizationDAO::getById);
    }

    AuditData.get().setData("filteredOwnersCount", owners.size());
    log.debug("getDashboardPolicyWaiverRequests: Found {} owners to filter policy waiver requests", owners.size());

    return owners;
  }

  private List<Repository> getRepositories(Set<String> repositoryIds, boolean isOwnerFilterEmpty) {
    if (isOwnerFilterEmpty || CollectionUtils.isNotEmpty(repositoryIds)) {
      return repositoryService.getRepositoriesWithReadPermissionByIds(repositoryIds);
    }
    return Collections.emptyList();
  }

  @AuthzFilter(permission = Permission.READ, context = Context.REPOSITORY_MANAGER)
  List<RepositoryManager> filterRepositoryManagersWithReadPermission(List<RepositoryManager> repositoryManagers) {
    return repositoryManagers;
  }

  private List<RepositoryManager> getRepositoryManagers(Set<String> repositoryIds, boolean isOwnerFilterEmpty) {
    if (isOwnerFilterEmpty) {
      return filterRepositoryManagersWithReadPermission(repositoryManagerDAO.getAll());
    }
    if (CollectionUtils.isNotEmpty(repositoryIds)) {
      // We need all parent repository managers for the repositories the user has read permission for,
      // regardless of the permissions on repository managers.
      return repositoryManagerDAO.getByRepositoryIds(repositoryIds);
    }
    return Collections.emptyList();
  }

  private DashboardPolicyWaiverRequestDTOComparator verifyOrderByAndBuildComparator(String orderBy) {
    // the comparator constructor validates the order by and throws exception if invalid
    return new DashboardPolicyWaiverRequestDTOComparator(orderBy);
  }

  private Map<String, Policy> getFilteredPoliciesById(
      PolicyThreatCategoryFilter policyThreatCategories,
      PolicyThreatLevelFilter policyThreatLevelRange)
  {
    Predicate<Policy> filter = x -> true;
    if (policyThreatCategories != null) {
      filter = filter.and(policy -> policyThreatCategories.test(policy.getThreatCategory()));
    }

    if (policyThreatLevelRange != null) {
      filter = filter.and(policy -> policyThreatLevelRange.test(policy.getThreatLevel()));
    }
    return policyDAO.getAll().stream().filter(filter).collect(Collectors.toMap(Policy::getId, Function.identity()));
  }

  private Predicate<PolicyWaiverRequest> getFilteringPredicateForWaiverReasons(Set<String> policyWaiverReasonIds) {
    boolean shouldIncludeNoReason = nonNull(policyWaiverReasonIds) && policyWaiverReasonIds.contains("no-reason");

    return policyWaiverRequest -> isNull(policyWaiverReasonIds) || policyWaiverReasonIds.isEmpty()
        || (shouldIncludeNoReason && isNull(policyWaiverRequest.getWaiverReasonId()))
        || policyWaiverReasonIds.contains(policyWaiverRequest.getWaiverReasonId());
  }

  private Predicate<PolicyWaiverRequest> getFilteringPredicateForPolicyWaiverRequests(Set<String> ownerIds) {
    // To use more filters add "and" conditions between new predicates
    return policyWaiverRequest -> ownerIds.contains(policyWaiverRequest.getOwnerId());
  }

  private Predicate<PolicyWaiverRequest> getFilteringPredicateForExpirationDates(ExpirationDate expirationDate) {
    if (expirationDate == NEVER) {
      return policyWaiverRequest -> policyWaiverRequest.getExpiryTime() == null;
    }
    else if (expirationDate == ALL) {
      return policyWaiverRequest -> true;
    }

    Instant expiration = Instant.now().plus(expirationDate.getDays(), ChronoUnit.DAYS)
        // add one day to allow waivers expiring on last day to filter in
        .plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);

    return policyWaiverRequest -> policyWaiverRequest.getExpiryTime() != null
        && !policyWaiverRequest.getExpiryTime().toInstant().truncatedTo(ChronoUnit.DAYS).isAfter(expiration);
  }

  private List<DashboardPolicyWaiverRequestDTO> filterPolicyWaiverRequestsAndBuildDTOs(
      List<PolicyWaiverRequest> policyWaiverRequests,
      Predicate<PolicyWaiverRequest> filter,
      DashboardPolicyWaiverRequestDTOAdapter dtoAdapter,
      Map<String, PolicyWaiverReason> waiverReasonIdToWaiverReason)
  {
    return policyWaiverRequests.stream().filter(filter).map((policyWaiverRequest) -> {
      return dtoAdapter.toDto(policyWaiverRequest,
          waiverReasonIdToWaiverReason.get(policyWaiverRequest.getWaiverReasonId()));
    }).collect(Collectors.toList());
  }

  protected Map<String, ? extends Owner> getApplicationsAndOrgs(
      Set<String> applicationIds,
      Set<String> tagIds,
      Set<String> organizationIds,
      boolean isOwnerFilterEmpty)
  {
    List<? extends Owner> owners =
        getApplicationsAndOrgsFilteredByAuthz(applicationIds, tagIds, organizationIds, isOwnerFilterEmpty);

    // the parent orgs are not filtered by permissions because we need access to parents of any org the app the user
    // has permission for
    Map<String, Organization> parentOrgs = organizationService.getAllParentOrgsNoAuthz(owners, null, null);

    Map<String, Owner> ownersByIdMap = owners.stream().collect(ownerCollector);

    ownersByIdMap.putAll(parentOrgs);

    return ownersByIdMap;
  }

  // visible for @AuthzFilter
  @AuthzFilter(permission = Permission.READ, context = Context.APPLICATION_OR_ORGANIZATION)
  protected List<? extends Owner> getApplicationsAndOrgsFilteredByAuthz(
      Set<String> applicationIds,
      Set<String> tagIds,
      Set<String> organizationIds,
      boolean isOwnerFilterEmpty)
  {
    // When all filters are empty, including repositories we get all orgs
    if (isOwnerFilterEmpty) {
      return ownerDAO.getAllAppsAndOrgs();
    }
    else {
      return ownerDAO.getOwnersByAppTagsAndOrgs(applicationIds, tagIds, organizationIds);
    }
  }
}
