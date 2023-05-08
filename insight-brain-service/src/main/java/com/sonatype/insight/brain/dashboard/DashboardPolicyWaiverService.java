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
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.organization.OrganizationService;
import com.sonatype.insight.brain.repository.RepositoryService;

import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.dashboard.ExpirationDate.ALL;
import static com.sonatype.insight.brain.dashboard.ExpirationDate.NEVER;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;

@Named
public class DashboardPolicyWaiverService
{
  private static final Logger log = LoggerFactory.getLogger(DashboardPolicyWaiverService.class);

  private final DashboardUtils dashboardUtils;

  private final ApplicationService applicationService;

  private final OrganizationService organizationService;

  private final OrganizationDAO organizationDAO;

  private final PolicyDAO policyDAO;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final RepositoryService repositoryService;

  private final Collector<Owner, ?, Map<String, Owner>> ownerCollector =
      Collectors.toMap(Owner::getId, Function.identity(), (existing, replacement) -> existing);

  @Inject
  public DashboardPolicyWaiverService(
      DashboardUtils dashboardUtils,
      ApplicationService applicationService,
      OrganizationService organizationService,
      PolicyDAO policyDAO,
      PolicyWaiverDAO policyWaiverDAO,
      OrganizationDAO organizationDAO,
      RepositoryService repositoryService)
  {
    this.dashboardUtils = dashboardUtils;
    this.applicationService = applicationService;
    this.organizationService = organizationService;
    this.policyDAO = policyDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.organizationDAO = organizationDAO;
    this.repositoryService = repositoryService;
  }

  public DashboardResultsDTO<DashboardPolicyWaiverDTO> getDashboardPolicyWaivers(final RisksFilterDTO risksFilterDTO) {
    return getDashboardPolicyWaivers(risksFilterDTO, false);
  }

  public DashboardResultsDTO<DashboardPolicyWaiverDTO> getDashboardPolicyWaiversForExport(
      final RisksFilterDTO risksFilterDTO)
  {
    return getDashboardPolicyWaivers(risksFilterDTO, true);
  }

  private DashboardResultsDTO<DashboardPolicyWaiverDTO> getDashboardPolicyWaivers(
      final RisksFilterDTO risksFilterDTO, boolean includeDetails)
  {
    dashboardUtils.validateDashboardLicensedAndEnabled();

    long start = System.currentTimeMillis();

    Set<String> organizationIds = risksFilterDTO.organizationIds;
    Set<String> applicationIds = risksFilterDTO.applicationIds;
    Set<String> tagIds = risksFilterDTO.tagIds;
    PolicyThreatCategoryFilter policyThreatCategories = risksFilterDTO.policyThreatCategories;
    PolicyThreatLevelFilter policyThreatLevelRange = risksFilterDTO.policyThreatLevelRange;
    ExpirationDate expirationDate = risksFilterDTO.expirationDate;
    String orderBy = risksFilterDTO.orderBy;

    // Verify orderBy early to prevent costly operations if it fails
    DashboardPolicyWaiverDTOComparator dashboardPolicyWaiverDTOComparator = verifyOrderByAndBuildComparator(orderBy);

    Map<String, Owner> owners = getOwners(organizationIds, applicationIds, tagIds, risksFilterDTO.repositoryIds);
    Map<String, Policy> filteredPoliciesById = getFilteredPoliciesById(policyThreatCategories, policyThreatLevelRange);
    DashboardPolicyWaiverDTOAdapter dtoAdapter =
        new DashboardPolicyWaiverDTOAdapter(filteredPoliciesById, owners, includeDetails);

    Predicate<PolicyWaiver> filteringExpirationDate = getFilteringPredicateForExpirationDates(expirationDate);
    Predicate<PolicyWaiver> filteringPredicate =
        getFilteringPredicateForPolicyWaivers(owners.keySet()).and(filteringExpirationDate);

    List<DashboardPolicyWaiverDTO> filteredWaiverDTOs = new ArrayList<>();
    for (Policy policy : filteredPoliciesById.values()) {
      List<PolicyWaiver> policyWaivers = expirationDate.equals(ALL) ?
          policyWaiverDAO.getByPolicyId(policy.getId())
          : policyWaiverDAO.getActiveByPolicyId(policy.getId());
      List<DashboardPolicyWaiverDTO> partialDTOs =
          filterPolicyWaiversAndBuildDTOs(policyWaivers, filteringPredicate, dtoAdapter);
      filteredWaiverDTOs.addAll(partialDTOs);
    }

    log.debug("getDashboardPolicyWaivers: Found {} waivers after filters", filteredWaiverDTOs.size());

    filteredWaiverDTOs.sort(dashboardPolicyWaiverDTOComparator);

    DashboardResultsDTO<DashboardPolicyWaiverDTO> resultsDTO = new DashboardResultsDTO<>();
    resultsDTO.numResults = filteredWaiverDTOs.size();

    if (filteredWaiverDTOs.isEmpty()) {
      resultsDTO.dashboardResults = new ArrayList<>();
    }
    else {
      int page = risksFilterDTO.page;
      List<List<DashboardPolicyWaiverDTO>> pages = Lists.partition(filteredWaiverDTOs, risksFilterDTO.pageSize);
      resultsDTO.dashboardResults = page >= pages.size() ? new ArrayList<>() : pages.get(page);
    }

    AuditData.get().setData("resultRecordCount", resultsDTO.numResults);
    log.debug("getDashboardPolicyWaivers: Finished in {} ms", System.currentTimeMillis() - start);

    return resultsDTO;
  }

  private DashboardPolicyWaiverDTOComparator verifyOrderByAndBuildComparator(final String orderBy) {
    // the comparator constructor validates the order by and throws exception if invalid
    return new DashboardPolicyWaiverDTOComparator(orderBy);
  }

  private Map<String, Owner> getOwners(
      final Set<String> organizationIds,
      final Set<String> applicationIds,
      final Set<String> tagIds,
      final Set<String> repositoryIds)
  {
    Map<String, Owner> owners = new HashMap<>();
    BooleanSupplier isOwnerFilterEmpty = () ->
        CollectionUtils.isEmpty(organizationIds)
          && CollectionUtils.isEmpty(applicationIds)
          && CollectionUtils.isEmpty(tagIds)
          && CollectionUtils.isEmpty(repositoryIds);
    Predicate<Set<Repository>> reposAreNotEmptyOrIsOnlyRepoContainer = repos ->
        !repos.isEmpty() || (CollectionUtils.isNotEmpty(repositoryIds)
            && repositoryIds.contains(RepositoryContainer.REPOSITORY_CONTAINER_ID));
    BooleanSupplier filtersAreEmptyAndRepoContainerReadPermission = () ->
        isOwnerFilterEmpty.getAsBoolean()
            && repositoryService.checkReadPermissionRepositoryContainer();
    Predicate<Set<Repository>> shouldAddRepoContainer = repos ->
        reposAreNotEmptyOrIsOnlyRepoContainer.test(repos)
            || filtersAreEmptyAndRepoContainerReadPermission.getAsBoolean();

    List<Application> applications = getApplications(applicationIds, tagIds, isOwnerFilterEmpty);
    Map<String, Organization> appsParentOrgs = organizationService.getAllParentOrgsNoAuthz(applications);
    List<Organization> organizations = getOrganizations(organizationIds, isOwnerFilterEmpty);
    Map<String, Organization> orgsParentOrgs = organizationService
        .getAllParentOrgsNoAuthz(organizations, appsParentOrgs);
    Set<Repository> repositories = getRepositories(repositoryIds, isOwnerFilterEmpty);

    owners.putAll(applications.stream().collect(ownerCollector));
    owners.putAll(appsParentOrgs);
    owners.putAll(organizations.stream().collect(ownerCollector));
    owners.putAll(orgsParentOrgs);
    owners.putAll(repositories.stream().collect(ownerCollector));

    if (shouldAddRepoContainer.test(repositories)) {
      owners.put(RepositoryContainer.REPOSITORY_CONTAINER_ID, RepositoryContainer.SINGLETON);
      owners.computeIfAbsent(ROOT_ORGANIZATION_ID, organizationDAO::getById);
    }

    AuditData.get().setData("filteredOwnersCount", owners.size());
    log.debug("getDashboardPolicyWaivers: Found {} owners to filter policy waivers", owners.size());

    return owners;
  }

  private List<Application> getApplications(
      Set<String> applicationIds,
      Set<String> tagIds,
      BooleanSupplier isOwnerFilterEmpty)
  {
    List<Application> applications = Collections.emptyList();
    if (isOwnerFilterEmpty.getAsBoolean()
        || (CollectionUtils.isNotEmpty(applicationIds)
        || CollectionUtils.isNotEmpty(tagIds))) {
      applications = applicationService.getOwnerApplicationsByIdsOrTagIds(applicationIds, tagIds);
    }
    return applications;
  }

  private List<Organization> getOrganizations(
      Set<String> organizationIds,
      BooleanSupplier isOwnerFilterEmpty)
  {
    List<Organization> allOrgs;
    List<Organization> organizations = Collections.emptyList();
    if (isOwnerFilterEmpty.getAsBoolean()) {
      organizations = organizationService.getAll();
    }
    else if (CollectionUtils.isNotEmpty(organizationIds)) {
      allOrgs = organizationService.getAll();
      organizations = allOrgs.stream()
          .filter(organization -> organizationIds.contains(organization.getId()))
          .collect(Collectors.toList());
    }
    return organizations;
  }

  private Set<Repository> getRepositories(
      Set<String> repositoryIds,
      BooleanSupplier isOwnerFilterEmpty)
  {
    Set<Repository> repositories = Collections.emptySet();
    if (isOwnerFilterEmpty.getAsBoolean() || CollectionUtils.isNotEmpty(repositoryIds)) {
      repositories = repositoryService.getRepositoriesByIds(repositoryIds);
    }
    return repositories;
  }

  private Map<String, Policy> getFilteredPoliciesById(
      final PolicyThreatCategoryFilter policyThreatCategories, final PolicyThreatLevelFilter policyThreatLevelRange)
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

  private Predicate<PolicyWaiver> getFilteringPredicateForPolicyWaivers(final Set<String> ownerIds) {
    // To use more filters add "and" conditions between new predicates
    return policyWaiver -> ownerIds.contains(policyWaiver.getOwnerId());
  }

  private Predicate<PolicyWaiver> getFilteringPredicateForExpirationDates(final ExpirationDate expirationDate) {
    if (expirationDate == NEVER) {
      return policyWaiver -> policyWaiver.getExpiryTime() == null;
    }
    else if (expirationDate == ALL) {
      return policyWaiver -> true;
    }

    final Instant expiration =
        Instant.now()
            .plus(expirationDate.getDays(), ChronoUnit.DAYS)
            // add one day to allow waivers expiring on last day to filter in
            .plus(1, ChronoUnit.DAYS)
            .truncatedTo(ChronoUnit.DAYS);

    return policyWaiver -> policyWaiver.getExpiryTime() != null &&
        !policyWaiver.getExpiryTime().toInstant().truncatedTo(ChronoUnit.DAYS).isAfter(expiration);
  }

  private List<DashboardPolicyWaiverDTO> filterPolicyWaiversAndBuildDTOs(
      final List<PolicyWaiver> policyWaivers,
      final Predicate<PolicyWaiver> filter,
      final DashboardPolicyWaiverDTOAdapter dtoAdapter)
  {
    return policyWaivers.stream().filter(filter).map(dtoAdapter::toDto).collect(Collectors.toList());
  }
}
