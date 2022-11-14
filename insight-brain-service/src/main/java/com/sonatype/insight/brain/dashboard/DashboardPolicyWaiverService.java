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
import java.util.Objects;
import java.util.Set;
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
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.organization.OrganizationService;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.dashboard.ExpirationDate.ALL;
import static com.sonatype.insight.brain.dashboard.ExpirationDate.NEVER;

@Named
public class DashboardPolicyWaiverService
{
  private static final Logger log = LoggerFactory.getLogger(DashboardPolicyWaiverService.class);

  private final DashboardUtils dashboardUtils;

  private final ApplicationService applicationService;

  private final OrganizationService organizationService;

  private final PolicyDAO policyDAO;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final OrganizationDAO organizationDAO;

  @Inject
  public DashboardPolicyWaiverService(
      DashboardUtils dashboardUtils,
      ApplicationService applicationService,
      OrganizationService organizationService,
      PolicyDAO policyDAO,
      PolicyWaiverDAO policyWaiverDAO,
      OrganizationDAO organizationDAO)
  {
    this.dashboardUtils = dashboardUtils;
    this.applicationService = applicationService;
    this.organizationService = organizationService;
    this.policyDAO = policyDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.organizationDAO = organizationDAO;
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
    int maxResults = risksFilterDTO.maxResults;

    // Verify orderBy early to prevent costly operations if it fails
    DashboardPolicyWaiverDTOComparator dashboardPolicyWaiverDTOComparator = verifyOrderByAndBuildComparator(orderBy);

    Map<String, Owner> owners = getOwners(organizationIds, applicationIds, tagIds);
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
    resultsDTO.dashboardResults = filteredWaiverDTOs.subList(0, Math.min(filteredWaiverDTOs.size(), maxResults));

    AuditData.get().setData("resultRecordCount", resultsDTO.numResults);
    log.debug("getDashboardPolicyWaivers: Finished in {} ms", System.currentTimeMillis() - start);

    return resultsDTO;
  }

  private DashboardPolicyWaiverDTOComparator verifyOrderByAndBuildComparator(final String orderBy) {
    // the comparator constructor validates the order by and throws exception if invalid
    return new DashboardPolicyWaiverDTOComparator(orderBy);
  }

  private Map<String, Owner> getOwners(
      final Set<String> organizationIds, final Set<String> applicationIds, final Set<String> tagIds)
  {
    Map<String, Owner> owners = new HashMap<>();
    Collector<Owner, ?, Map<String, Owner>> ownerCollector =
        Collectors.toMap(Owner::getId, Function.identity(), (owner1, owner2) -> owner1);

    final Map<String, Owner> allOrganizations = organizationDAO.getAll().stream().collect(ownerCollector);

    Runnable setAuditData = () -> {
      AuditData.get().setData("filteredOwnersCount", owners.size());
      log.debug("getDashboardPolicyWaivers: Found {} owners to filter policy waivers", owners.size());
    };

    if (CollectionUtils.isEmpty(organizationIds)
        && CollectionUtils.isEmpty(applicationIds)
        && CollectionUtils.isEmpty(tagIds)) {
      List<Application> applications = applicationService.getApplications();
      owners.putAll(applications.stream().collect(ownerCollector));
      owners.putAll(getAllOrganizationsForFilteredApplications(ownerCollector, allOrganizations, applications));
      includeRootOrgInOwnersIfApplicable(owners, allOrganizations);

      setAuditData.run();

      return owners;
    }

    Predicate<Owner> filterOrgsById = (Owner organization) -> organizationIds.contains(organization.getId());

    List<Application> applications = Collections.emptyList();
    if (!CollectionUtils.isEmpty(applicationIds) || !CollectionUtils.isEmpty(tagIds)) {
      applications = applicationService
          .getApplicationsByIdsAndOrganizationIdsAndTagIds(null, applicationIds, tagIds);
    }

    if (!applications.isEmpty()) {
      owners.putAll(applications.stream().collect(ownerCollector));
      owners.putAll(getAllOrganizationsForFilteredApplications(ownerCollector, allOrganizations, applications));
    }
    if (!CollectionUtils.isEmpty(organizationIds)) {
      owners.putAll(organizationService.getAll().stream().filter(filterOrgsById).collect(ownerCollector));
    }
    includeRootOrgInOwnersIfApplicable(owners, allOrganizations);

    setAuditData.run();

    return owners;
  }

  private void includeRootOrgInOwnersIfApplicable(
      final Map<String, Owner> owners,
      final Map<String, Owner> allOrganizations)
  {
    if (!owners.isEmpty()) {
      owners.put(Organization.ROOT_ORGANIZATION_ID, allOrganizations.get(Organization.ROOT_ORGANIZATION_ID));
    }
  }

  private Map<String, Owner> getAllOrganizationsForFilteredApplications(
      final Collector<Owner, ?, Map<String, Owner>> ownerCollector,
      final Map<String, Owner> allOrganizations,
      final List<Application> applications)
  {
    return applications.stream().map(application -> allOrganizations.get(application.getOrganizationId()))
        .filter(Objects::nonNull).collect(ownerCollector);
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
