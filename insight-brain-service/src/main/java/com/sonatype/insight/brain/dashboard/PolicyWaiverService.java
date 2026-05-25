/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.dashboard;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverReasonDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.OrganizationService;
import com.sonatype.insight.brain.policy.PolicyWaiverResource;
import com.sonatype.insight.brain.repository.RepositoryService;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.security.AuthzFilter.Context;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.dashboard.ExpirationDate.ALL;
import static com.sonatype.insight.brain.dashboard.ExpirationDate.AUTO;
import static com.sonatype.insight.brain.dashboard.ExpirationDate.EXPIRED;
import static com.sonatype.insight.brain.dashboard.ExpirationDate.IN_OVER_90_DAYS;
import static com.sonatype.insight.brain.dashboard.ExpirationDate.NEVER;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class PolicyWaiverService
{
  private static final Logger log = LoggerFactory.getLogger(PolicyWaiverService.class);

  private static final Collector<Owner, ?, Map<String, Owner>> ownerCollector =
      Collectors.toMap(Owner::getId, Function.identity(), (existing, replacement) -> existing);

  private final DashboardUtils dashboardUtils;

  private final PolicyDAO policyDAO;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final AutoPolicyWaiverDAO autoPolicyWaiverDAO;

  private final RepositoryService repositoryService;

  private final OrganizationDAO organizationDAO;

  private final OrganizationService organizationService;

  private final OwnerDAO ownerDAO;

  private final PolicyWaiverReasonDAO policyWaiverReasonDAO;

  private final RepositoryManagerDAO repositoryManagerDAO;

  @Inject
  public PolicyWaiverService(
      final DashboardUtils dashboardUtils,
      final PolicyDAO policyDAO,
      final PolicyWaiverDAO policyWaiverDAO,
      final AutoPolicyWaiverDAO autoPolicyWaiverDAO,
      final OrganizationDAO organizationDAO,
      final RepositoryService repositoryService,
      final OrganizationService organizationService,
      final OwnerDAO ownerDAO,
      final PolicyWaiverReasonDAO policyWaiverReasonDAO,
      final RepositoryManagerDAO repositoryManagerDAO)
  {
    this.dashboardUtils = dashboardUtils;
    this.policyDAO = policyDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.autoPolicyWaiverDAO = autoPolicyWaiverDAO;
    this.organizationDAO = organizationDAO;
    this.repositoryService = repositoryService;
    this.organizationService = organizationService;
    this.ownerDAO = ownerDAO;
    this.policyWaiverReasonDAO = policyWaiverReasonDAO;
    this.repositoryManagerDAO = repositoryManagerDAO;
  }

  public DashboardResultsDTO<DashboardPolicyWaiverDTO> getDashboardPolicyWaivers(final RisksFilterDTO risksFilterDTO) {
    return getDashboardPolicyWaivers(risksFilterDTO, false, false);
  }

  public DashboardResultsDTO<DashboardPolicyWaiverDTO> getDashboardPolicyWaiversForExport(
      final RisksFilterDTO risksFilterDTO)
  {
    return getDashboardPolicyWaiversForExport(risksFilterDTO, false);
  }

  public DashboardResultsDTO<DashboardPolicyWaiverDTO> getDashboardPolicyWaiversForExport(
      final RisksFilterDTO risksFilterDTO,
      boolean includeAutoWaivers)
  {
    return getDashboardPolicyWaivers(risksFilterDTO, true, includeAutoWaivers);
  }

  public DashboardResultsDTO<DashboardPolicyWaiverDTO> getDashboardPolicyWaivers(
      final RisksFilterDTO risksFilterDTO,
      boolean includeAutoWaivers)
  {
    return getDashboardPolicyWaivers(risksFilterDTO, false, includeAutoWaivers);
  }

  private DashboardResultsDTO<DashboardPolicyWaiverDTO> getDashboardPolicyWaivers(
      final RisksFilterDTO risksFilterDTO,
      final boolean includeDetails,
      final boolean includeAutoWaivers)
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
    final Set<String> policyWaiverReasonIds = risksFilterDTO.policyWaiverReasonIds;

    // Check if Firewall text filters are provided
    boolean hasComponentNameFilter = StringUtils.isNotBlank(risksFilterDTO.componentName);
    boolean hasRepositoryPublicIdFilter = StringUtils.isNotBlank(risksFilterDTO.repositoryPublicId);

    // Verify orderBy early to prevent costly operations if it fails
    DashboardPolicyWaiverDTOComparator dashboardPolicyWaiverDTOComparator = verifyOrderByAndBuildComparator(orderBy);

    Map<String, Owner> owners = getOwners(organizationIds, applicationIds, tagIds, risksFilterDTO.repositoryIds);
    Map<String, Policy> filteredPoliciesById = getFilteredPoliciesById(policyThreatCategories, policyThreatLevelRange);
    DashboardPolicyWaiverDTOAdapter dtoAdapter =
        new DashboardPolicyWaiverDTOAdapter(filteredPoliciesById, owners, includeDetails);

    Predicate<PolicyWaiver> filteringPredicate =
        getFilteringPredicateForPolicyWaivers(owners.keySet())
            .and(getFilteringPredicateForExpirationDates(expirationDate))
            .and(getFilteringPredicateForWaiverReasons(policyWaiverReasonIds))
            .and(getFilteringPredicateForContainerImageComponent());

    // we want to fetch this once per re-request outside any loops so we don't go to the database too often
    // from a memory standpoint it should not pose a problem, it is presenetly hard coded at 7 rows, custom entries
    // may eventually be possible, but it is highly unlikely to ever grow very large
    final var waiverReasonIdToWaiverReason = policyWaiverReasonDAO.getPolicyWaiverReasonIdToPolicyWaiverReasonMap();

    // Use optimized SQL path when Firewall text filters are provided — pagination is handled by the DAO
    if (hasComponentNameFilter || hasRepositoryPublicIdFilter) {
      DashboardResultsDTO<DashboardPolicyWaiverDTO> resultsDTO =
          getFilteredWaiversForDashboardWithTextFilters(
              risksFilterDTO,
              owners,
              filteredPoliciesById,
              dtoAdapter,
              waiverReasonIdToWaiverReason,
              filteringPredicate,
              dashboardPolicyWaiverDTOComparator);
      log.debug("getDashboardPolicyWaivers: Finished in {} ms (text-filter path)", System.currentTimeMillis() - start);
      return resultsDTO;
    }

    List<DashboardPolicyWaiverDTO> filteredWaiverDTOs = new ArrayList<>();

    // Existing path for regular requests without text filters
    for (Policy policy : filteredPoliciesById.values()) {
      List<PolicyWaiver> policyWaivers = (expirationDate.equals(ALL) || expirationDate == EXPIRED)
          ? policyWaiverDAO.getByPolicyId(policy.getId())
          : policyWaiverDAO.getActiveByPolicyId(policy.getId());
      List<DashboardPolicyWaiverDTO> partialDTOs =
          filterPolicyWaiversAndBuildDTOs(policyWaivers, filteringPredicate, dtoAdapter, waiverReasonIdToWaiverReason);
      filteredWaiverDTOs.addAll(partialDTOs);
    }

    // auto waiver
    if (includeAutoWaivers && dashboardUtils.isAutoWaiverFeatureFlagEnabled()) {
      Predicate<AutoPolicyWaiver> autoPolicyWaiverPredicate = getFilteringPredicateForAutoPolicyWaivers(owners.keySet())
          .and(getFilteringPredicateForAutoWaiverThreatLevel(policyThreatLevelRange))
          .and(getFilteringPredicateForAutoWaiverThreatCategory(policyThreatCategories))
          .and(getFilteringPredicateForAutoWaiverExpirationDates(expirationDate));
      List<AutoPolicyWaiver> autoPolicyWaivers = autoPolicyWaiverDAO.getAll();
      filteredWaiverDTOs.addAll(
          filterAutoPolicyWaiversAndBuildDTOs(autoPolicyWaivers, autoPolicyWaiverPredicate, dtoAdapter));
    }

    log.debug("getDashboardPolicyWaivers: Found {} waivers after filters", filteredWaiverDTOs.size());

    filteredWaiverDTOs.sort(dashboardPolicyWaiverDTOComparator);

    DashboardResultsDTO<DashboardPolicyWaiverDTO> resultsDTO = new DashboardResultsDTO<>();
    if (filteredWaiverDTOs.isEmpty()) {
      resultsDTO.dashboardResults = new ArrayList<>();
    }
    else {
      int page = risksFilterDTO.page;
      List<List<DashboardPolicyWaiverDTO>> pages = Lists.partition(filteredWaiverDTOs, risksFilterDTO.pageSize);
      resultsDTO.dashboardResults = page >= pages.size() ? new ArrayList<>() : pages.get(page);
      resultsDTO.hasNextPage = pages.size() > (page + 1);
    }

    log.debug("getDashboardPolicyWaivers: Finished in {} ms", System.currentTimeMillis() - start);

    return resultsDTO;
  }

  /**
   * Gets filtered waivers when Firewall text filters (componentName or repositoryPublicId) are provided.
   * Pagination is performed at the database level using LIMIT/OFFSET; the DAO returns pageSize+1 rows
   * so we can detect hasNextPage without a separate COUNT query.
   */
  private DashboardResultsDTO<DashboardPolicyWaiverDTO> getFilteredWaiversForDashboardWithTextFilters(
      final RisksFilterDTO risksFilterDTO,
      final Map<String, Owner> owners,
      final Map<String, Policy> filteredPoliciesById,
      final DashboardPolicyWaiverDTOAdapter dtoAdapter,
      final Map<String, PolicyWaiverReason> waiverReasonIdToWaiverReason,
      final Predicate<PolicyWaiver> filteringPredicate,
      final DashboardPolicyWaiverDTOComparator comparator)
  {
    ExpirationDate expirationDate = risksFilterDTO.expirationDate;
    boolean activeOnly = !expirationDate.equals(ALL) && expirationDate != EXPIRED;
    boolean neverExpiringOnly = expirationDate == NEVER;
    int pageSize = risksFilterDTO.pageSize;

    Date expiryNotBefore = null;
    Date expiryNotAfter = null;

    if (!neverExpiringOnly && expirationDate != ALL) {
      if (expirationDate == EXPIRED) {
        expiryNotAfter = Date.from(Instant.now().truncatedTo(ChronoUnit.DAYS));
      }
      else if (expirationDate == IN_OVER_90_DAYS) {
        expiryNotBefore = Date.from(
            Instant.now().plus(90, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS));
      }
      else if (expirationDate.getDays() != null && expirationDate.getDays() > 0) {
        expiryNotAfter = Date.from(
            Instant.now()
                .plus(expirationDate.getDays(), ChronoUnit.DAYS)
                .plus(1, ChronoUnit.DAYS)
                .truncatedTo(ChronoUnit.DAYS));
      }
    }

    List<PolicyWaiver> policyWaivers;
    try (TransactionContext tx = policyWaiverDAO.createTransactionContext()) {
      policyWaivers = policyWaiverDAO.getFilteredForDashboard(
          tx,
          owners.keySet(),
          risksFilterDTO.componentName,
          risksFilterDTO.repositoryPublicId,
          activeOnly,
          expiryNotBefore,
          expiryNotAfter,
          neverExpiringOnly,
          filteredPoliciesById.keySet(),
          risksFilterDTO.page,
          pageSize,
          risksFilterDTO.orderBy);
    }

    // Remaining in-memory predicates: owner scope, waiver reasons, container-image exclusion,
    // and policy threat category/level. Expiration date filtering is now handled by the DAO.
    List<DashboardPolicyWaiverDTO> allDtos = filterPolicyWaiversAndBuildDTOs(
        policyWaivers,
        filteringPredicate.and(waiver -> filteredPoliciesById.containsKey(waiver.getPolicyId())),
        dtoAdapter,
        waiverReasonIdToWaiverReason);

    allDtos.sort(comparator);
    boolean hasNextPage = allDtos.size() > pageSize;
    List<DashboardPolicyWaiverDTO> dtos = hasNextPage ? allDtos.subList(0, pageSize) : allDtos;

    DashboardResultsDTO<DashboardPolicyWaiverDTO> resultsDTO = new DashboardResultsDTO<>();
    resultsDTO.dashboardResults = dtos;
    resultsDTO.hasNextPage = hasNextPage;
    return resultsDTO;
  }

  private Map<String, Owner> getOwners(
      final Set<String> organizationIds,
      final Set<String> applicationIds,
      final Set<String> tagIds,
      final Set<String> repositoryIds)
  {
    final Map<String, Owner> owners = new HashMap<>();

    final BooleanSupplier isOwnerFilterEmpty = () -> CollectionUtils.isEmpty(organizationIds)
        && CollectionUtils.isEmpty(applicationIds)
        && CollectionUtils.isEmpty(tagIds)
        && CollectionUtils.isEmpty(repositoryIds);

    final Map<String, ? extends Owner> lifeCycleOwners =
        getApplicationsAndOrgs(applicationIds, tagIds, organizationIds, isOwnerFilterEmpty);
    final List<Repository> repositories = getRepositories(repositoryIds, isOwnerFilterEmpty);
    final List<RepositoryManager> repositoryManagers = getRepositoryManagers(
        repositories.stream().map(Repository::getId).collect(Collectors.toSet()), isOwnerFilterEmpty);

    owners.putAll(lifeCycleOwners);
    owners.putAll(repositories.stream().collect(ownerCollector));
    owners.putAll(repositoryManagers.stream().collect(ownerCollector));

    final Predicate<List<Repository>> shouldAddRepoContainer =
        shouldAddRepoContainerPredicate(repositoryIds, isOwnerFilterEmpty);

    if (shouldAddRepoContainer.test(repositories)) {
      owners.put(RepositoryContainer.REPOSITORY_CONTAINER_ID, RepositoryContainer.SINGLETON);
      owners.computeIfAbsent(ROOT_ORGANIZATION_ID, organizationDAO::getById);
    }

    AuditData.get().setData("filteredOwnersCount", owners.size());
    log.debug("getDashboardPolicyWaivers: Found {} owners to filter policy waivers", owners.size());

    return owners;
  }

  private List<Repository> getRepositories(
      Set<String> repositoryIds,
      BooleanSupplier isOwnerFilterEmpty)
  {
    if (isOwnerFilterEmpty.getAsBoolean() || CollectionUtils.isNotEmpty(repositoryIds)) {
      return repositoryService.getRepositoriesWithReadPermissionByIds(repositoryIds);
    }
    return Collections.emptyList();
  }

  private DashboardPolicyWaiverDTOComparator verifyOrderByAndBuildComparator(final String orderBy) {
    // the comparator constructor validates the order by and throws exception if invalid
    return new DashboardPolicyWaiverDTOComparator(orderBy);
  }

  private Map<String, Policy> getFilteredPoliciesById(
      final PolicyThreatCategoryFilter policyThreatCategories,
      final PolicyThreatLevelFilter policyThreatLevelRange)
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

  private Predicate<PolicyWaiver> getFilteringPredicateForContainerImageComponent() {
    return policyWaiver -> !policyWaiver.isForContainerImageComponent();
  }

  private Predicate<PolicyWaiver> getFilteringPredicateForWaiverReasons(final Set<String> policyWaiverReasonIds) {
    final boolean shouldIncludeNoReason = nonNull(policyWaiverReasonIds) && policyWaiverReasonIds.contains("no-reason");

    return policyWaiver -> isNull(policyWaiverReasonIds) ||
        policyWaiverReasonIds.isEmpty() ||
        (shouldIncludeNoReason && isNull(policyWaiver.getWaiverReasonId())) ||
        policyWaiverReasonIds.contains(policyWaiver.getWaiverReasonId());
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
    else if (expirationDate == EXPIRED) {
      final Instant now = Instant.now().truncatedTo(ChronoUnit.DAYS);
      return policyWaiver -> policyWaiver.getExpiryTime() != null &&
          !policyWaiver.getExpiryTime().toInstant().truncatedTo(ChronoUnit.DAYS).isAfter(now);
    }
    else if (expirationDate == IN_OVER_90_DAYS) {
      // Waivers expiring more than 90 days from now (lower bound only)
      final Instant lowerBound = Instant.now().plus(90, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
      return policyWaiver -> policyWaiver.getExpiryTime() != null &&
          policyWaiver.getExpiryTime().toInstant().truncatedTo(ChronoUnit.DAYS).isAfter(lowerBound);
    }
    else if (expirationDate == AUTO) {
      // Auto-waivers are AutoPolicyWaiver instances handled separately; no regular PolicyWaiver should pass this filter
      return policyWaiver -> false;
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
      final DashboardPolicyWaiverDTOAdapter dtoAdapter,
      final Map<String, PolicyWaiverReason> waiverReasonIdToWaiverReason)
  {
    return policyWaivers.stream()
        .filter(filter)
        .map((policyWaiver) -> {
          return dtoAdapter.toDto(
              policyWaiver,
              waiverReasonIdToWaiverReason.get(policyWaiver.getWaiverReasonId()));
        })
        .collect(Collectors.toList());
  }

  private Predicate<AutoPolicyWaiver> getFilteringPredicateForAutoPolicyWaivers(final Set<String> ownerIds) {
    return autoPolicyWaiver -> ownerIds.contains(autoPolicyWaiver.getOwnerId());
  }

  private Predicate<AutoPolicyWaiver> getFilteringPredicateForAutoWaiverThreatLevel(
      final PolicyThreatLevelFilter policyThreatLevelRange)
  {
    if (policyThreatLevelRange == null) {
      return autoPolicyWaiver -> true;
    }
    return autoPolicyWaiver -> policyThreatLevelRange.test(autoPolicyWaiver.getThreatLevel());
  }

  private Predicate<AutoPolicyWaiver> getFilteringPredicateForAutoWaiverThreatCategory(
      final PolicyThreatCategoryFilter policyThreatCategories)
  {
    if (policyThreatCategories == null) {
      return autoPolicyWaiver -> true;
    }
    return autoPolicyWaiver -> policyThreatCategories.test(PolicyThreatCategory.SECURITY) &&
        autoPolicyWaiver.hasReachability();
  }

  private Predicate<AutoPolicyWaiver> getFilteringPredicateForAutoWaiverExpirationDates(
      final ExpirationDate expirationDate)
  {
    if (expirationDate == AUTO || expirationDate == ALL) {
      return autoPolicyWaiver -> true;
    }
    else {
      return autoPolicyWaiver -> false;
    }
  }

  private List<DashboardPolicyWaiverDTO> filterAutoPolicyWaiversAndBuildDTOs(
      final List<AutoPolicyWaiver> autoPolicyWaivers,
      final Predicate<AutoPolicyWaiver> filter,
      final DashboardPolicyWaiverDTOAdapter dtoAdapter)
  {
    return autoPolicyWaivers.stream().filter(filter).map(dtoAdapter::toDto).collect(Collectors.toList());
  }

  protected Map<String, ? extends Owner> getApplicationsAndOrgs(
      Set<String> applicationIds,
      Set<String> tagIds,
      Set<String> organizationIds,
      BooleanSupplier isOwnerFilterEmpty)
  {
    final List<? extends Owner> owners = getApplicationsAndOrgsFilteredByAuthz(
        applicationIds,
        tagIds,
        organizationIds,
        isOwnerFilterEmpty);

    // the parent orgs are not filtered by permissions because we need access to parents of any org the app the user
    // does have permission to
    final Map<String, Organization> parentOrgs = organizationService
        .getAllParentOrgsNoAuthz(owners, null, null);

    final Map<String, Owner> ownersByIdMap = owners.stream().collect(ownerCollector);

    ownersByIdMap.putAll(parentOrgs);

    return ownersByIdMap;
  }

  public List<PolicyWaiverResource.PolicyWaiverDTO> getExpiredWaivers(
      String ownerId,
      String hash,
      UnaryOperator<String> policyNameLoader,
      ComponentIdentifier componentIdentifier,
      Map<String, PolicyWaiverReason> policyWaiverReasonMap)
  {
    PackageUrlIdentifier purl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);
    List<PolicyWaiver> waivers =
        policyWaiverDAO.getExpiredToComponentIncludingAllVersions(ownerId, hash, purl);
    List<PolicyWaiverResource.PolicyWaiverDTO> dtos = new ArrayList<>(waivers.size());
    for (PolicyWaiver waiver : waivers) {
      dtos.add(mapPolicyWaiverToDTO(waiver, policyNameLoader, policyWaiverReasonMap));
    }
    return dtos;
  }

  public PolicyWaiverResource.PolicyWaiverDTO mapPolicyWaiverToDTO(
      PolicyWaiver waiver,
      UnaryOperator<String> policyNameLoader,
      Map<String, PolicyWaiverReason> policyWaiverReasonMap)
  {
    PolicyWaiverResource.PolicyWaiverDTO dto = new PolicyWaiverResource.PolicyWaiverDTO();
    dto.setComment(waiver.getComment());
    dto.setCreateTime(waiver.getCreateTime());
    dto.setHash(waiver.getHash());
    dto.setId(waiver.getId());
    dto.setOwnerId(waiver.getOwnerId());
    dto.setPolicyId(waiver.getPolicyId());
    dto.policyName = policyNameLoader.apply(dto.getPolicyId());
    dto.setConstraintFactsJson(waiver.getConstraintFactsJson());
    dto.setConstraintFacts(waiver.getConstraintFacts());
    dto.setCreatorId(waiver.getCreatorId());
    dto.setCreatorName(waiver.getCreatorName());
    dto.setAssociatedPackageUrl(waiver.getAssociatedPackageUrl());
    dto.setComponentMatchStrategy(waiver.getComponentMatchStrategy());
    dto.setExpireWhenRemediationAvailable(waiver.isExpireWhenRemediationAvailable());
    dto.setExpiryTime(waiver.getExpiryTime());
    dto.setForContainerImage(waiver.isForContainerImage());
    dto.setForContainerImageComponent(waiver.isForContainerImageComponent());
    if (waiver.getWaiverReasonId() != null) {
      dto.policyWaiverReasonId = waiver.getWaiverReasonId();
      dto.reasonText = policyWaiverReasonMap.get(waiver.getWaiverReasonId()).getReasonText();
    }
    return dto;
  }

  // visible for @AuthzFilter
  @AuthzFilter(permission = Permission.READ, context = Context.APPLICATION_OR_ORGANIZATION)
  protected List<? extends Owner> getApplicationsAndOrgsFilteredByAuthz(
      Set<String> applicationIds,
      Set<String> tagIds,
      Set<String> organizationIds,
      BooleanSupplier isOwnerFilterEmpty)
  {

    // When all filters are empty, including repositories we get all orgs
    if (isOwnerFilterEmpty.getAsBoolean()) {
      return ownerDAO.getAllAppsAndOrgs();
    }
    else {
      return ownerDAO.getOwnersByAppTagsAndOrgs(
          applicationIds, tagIds, organizationIds);
    }
  }

  @AuthzFilter(permission = Permission.READ, context = Context.REPOSITORY_MANAGER)
  List<RepositoryManager> getRepositoryManagersWithReadPermission(List<RepositoryManager> repositoryManagers) {
    return repositoryManagers;
  }

  private List<RepositoryManager> getRepositoryManagers(Set<String> repositoryIds, BooleanSupplier isOwnerFilterEmpty) {
    if (isOwnerFilterEmpty.getAsBoolean()) {
      return getRepositoryManagersWithReadPermission(repositoryManagerDAO.getAll());
    }
    if (CollectionUtils.isNotEmpty(repositoryIds)) {
      // We need all parent repository managers for the repositories the user has read permission for,
      // regardless of the permissions on repository managers.
      return repositoryManagerDAO.getByRepositoryIds(repositoryIds);
    }
    return Collections.emptyList();
  }

  private Predicate<List<Repository>> shouldAddRepoContainerPredicate(
      final Set<String> repositoryIds,
      final BooleanSupplier isOwnerFilterEmpty)
  {
    final Predicate<List<Repository>> reposAreNotEmptyOrIsOnlyRepoContainer = repos -> !repos.isEmpty() ||
        (CollectionUtils.isNotEmpty(repositoryIds) &&
            repositoryIds.contains(RepositoryContainer.REPOSITORY_CONTAINER_ID));
    final BooleanSupplier filtersAreEmptyAndRepoContainerReadPermission =
        () -> isOwnerFilterEmpty.getAsBoolean() && repositoryService.checkReadPermissionRepositoryContainer();

    return repos -> reposAreNotEmptyOrIsOnlyRepoContainer.test(repos) ||
        filtersAreEmptyAndRepoContainerReadPermission.getAsBoolean();
  }
}
