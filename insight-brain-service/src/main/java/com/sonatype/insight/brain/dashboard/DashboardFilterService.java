/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class DashboardFilterService
{
  private static final Logger log = LoggerFactory.getLogger(DashboardFilterService.class);

  private final ApplicationDAO applicationDAO;

  private final ApplicationComponentDAO applicationComponentDAO;

  private final ApplicationService applicationService;

  private final PolicyDAO policyDAO;

  private final DashboardFilterDAO dashboardFilterDAO;

  private final CurrentUser currentUser;

  private final DashboardUtils dashboardUtils;

  @Inject
  public DashboardFilterService(ApplicationDAO applicationDAO, ApplicationComponentDAO applicationComponentDAO,
      ApplicationService applicationService, PolicyDAO policyDAO, DashboardFilterDAO dashboardFilterDAO,
      CurrentUser currentUser, DashboardUtils dashboardUtils)
  {
    this.applicationDAO = applicationDAO;
    this.applicationComponentDAO = applicationComponentDAO;
    this.applicationService = applicationService;
    this.policyDAO = policyDAO;
    this.dashboardFilterDAO = dashboardFilterDAO;
    this.currentUser = currentUser;
    this.dashboardUtils = dashboardUtils;
  }
  /**
   * @since 1.11.0
   */
  public DashboardFilterDTO getDashboardFilterForCurrentUser() throws IOException {
    dashboardUtils.validateDashboardLicensed();

    String username = currentUser.getUsername();
    DashboardFilter dashboardFilter = dashboardFilterDAO.getByUsername(username);
    if (dashboardFilter == null) {
      return createDefaultDashboardFilterForCurrentUser();
    }
    DashboardFilterDTO dto = JsonUtils.parse(dashboardFilter.getFilter(), DashboardFilterDTO.class);

    // prune out any unauthorized applications
    pruneUnauthorizedApplicationIds(dto);
    return dto;
  }

  private DashboardFilterDTO createDefaultDashboardFilterForCurrentUser() {
    DashboardFilterDTO dashboardFilterDTO = new DashboardFilterDTO();
    dashboardFilterDTO.applicationFilters = new ArrayList<>();
    // Threat levels of 0 or 1 are intended to be informational only, and therefore are
    // not pertinent to assessing the "real" risk of a given Application or component
    dashboardFilterDTO.minPolicyThreatLevel = 2;
    dashboardFilterDTO.maxPolicyThreatLevel = 10;
    dashboardFilterDTO.stageTypeFilters = new ArrayList<>();
    dashboardFilterDTO.policyThreatCategoryFilters = new ArrayList<>();
    dashboardFilterDTO.tagFilters = new ArrayList<>();
    return createOrUpdateDashboardFilterForCurrentUser(dashboardFilterDTO);
  }

  /**
   * @since 1.11.0
   */
  public DashboardFilterDTO createOrUpdateDashboardFilterForCurrentUser(DashboardFilterDTO dashboardFilterDTO) {
    dashboardUtils.validateDashboardLicensed();

    String username = currentUser.getUsername();
    DashboardFilter dashboardFilter = new DashboardFilter();
    dashboardFilter.setUsername(username);
    dashboardFilter.setFilter(JsonUtils.format(dashboardFilterDTO));

    DashboardFilter existingDashboardFilter = dashboardFilterDAO.getByUsername(username);
    if (existingDashboardFilter == null) {
      dashboardFilterDAO.insert(dashboardFilter);
    }
    else {
      dashboardFilter.setId(existingDashboardFilter.getId());
      dashboardFilterDAO.update(dashboardFilter);
    }

    return dashboardFilterDTO;
  }

  /**
   * @since 1.11.0
   */
  public void deleteDashboardFilterForCurrentUser() {
    dashboardUtils.validateDashboardLicensed();

    String username = currentUser.getUsername();
    DashboardFilter dashboardFilter = dashboardFilterDAO.getByUsername(username);
    if (dashboardFilter != null) {
      dashboardFilterDAO.delete(dashboardFilter);
    }
  }

  /**
   * Calculates how many of the entities accessible to the current user are matched by the specified dashboard filter
   * settings.
   */
  public FilterSummaryDTO getFilterSummary(Set<String> applicationIds, Set<String> stageIds, Set<String> tagIds,
      PolicyThreatCategoryFilter policyThreatCategoryFilter, PolicyThreatLevelFilter policyThreatLevelFilter)
  {
    dashboardUtils.validateDashboardLicensed();

    long start = System.currentTimeMillis();

    FilterSummaryDTO summary = new FilterSummaryDTO();

    Collection<Application> readableApplications = applicationService.getApplications();
    summary.totalApplications = readableApplications.size();

    Collection<Application> matchedApplications = readableApplications;
    if (!CollectionUtils.isEmpty(applicationIds) || !CollectionUtils.isEmpty(tagIds)) {
      Map<String, Application> appsById = Maps.newHashMapWithExpectedSize(readableApplications.size());
      for (Application app : readableApplications) {
        appsById.put(app.getId(), app);
      }
      if (!CollectionUtils.isEmpty(applicationIds)) {
        appsById.keySet().retainAll(applicationIds);
      }
      if (!CollectionUtils.isEmpty(tagIds)) {
        matchedApplications = applicationDAO.getByIdsAndTagIds(appsById.keySet(), tagIds);
      }
      else {
        matchedApplications = appsById.values();
      }
    }
    summary.matchedApplications = matchedApplications.size();

    Collection<StageType> allStageTypes = dashboardUtils.getStageTypes(null);
    summary.totalComponents = applicationComponentDAO.getUniqueCountByApplicationIdsAndStageTypeIds(
        Collections2.transform(readableApplications, DashboardUtils.hasIdIdSelector),
        dashboardUtils.getStageIds(allStageTypes));
    Collection<StageType> matchedStageTypes = dashboardUtils.getStageTypes(stageIds);
    summary.matchedComponents = applicationComponentDAO.getUniqueCountByApplicationIdsAndStageTypeIds(
        Collections2.transform(matchedApplications, DashboardUtils.hasIdIdSelector),
        dashboardUtils.getStageIds(matchedStageTypes));

    Set<String> readablePolicyOwnerIds = getPolicyOwnerIds(readableApplications);
    List<Policy> readablePolicies = policyDAO.getByOwnerIds(readablePolicyOwnerIds);
    summary.totalPolicies = readablePolicies.size();

    final Set<String> matchedPolicyOwnerIds = getPolicyOwnerIds(matchedApplications);
    Collection<Policy> matchedPolicies = Collections2.filter(readablePolicies, new Predicate<Policy>()
    {
      @Override
      public boolean apply(@Nullable Policy input) {
        return input != null && matchedPolicyOwnerIds.contains(input.getOwnerId());
      }
    });
    Predicate<Policy> policyFilter = buildPolicyFilter(policyThreatCategoryFilter, policyThreatLevelFilter);
    if (policyFilter != null) {
      matchedPolicies = Collections2.filter(matchedPolicies, policyFilter);
    }
    summary.matchedPolicies = matchedPolicies.size();

    log.debug("Calculated filter summary in {} ms", System.currentTimeMillis() - start);

    return summary;
  }

  private Set<String> getPolicyOwnerIds(Collection<Application> applications) {
    Set<String> policyOwnerIds = new HashSet<>(applications.size() * 2);
    for (Application app : applications) {
      policyOwnerIds.add(app.getId());
      policyOwnerIds.add(app.getOrganizationId());
    }
    return policyOwnerIds;
  }

  private Predicate<Policy> buildPolicyFilter(PolicyThreatCategoryFilter threatCategoryFilter,
      PolicyThreatLevelFilter threatLevelFilter)
  {
    if (threatCategoryFilter == null && threatLevelFilter == null) {
      return null;
    }
    else if (threatCategoryFilter != null && threatLevelFilter != null) {
      return Predicates.and(threatCategoryFilter.asPolicyPredicate(), threatLevelFilter.asPolicyPredicate());
    }

    return (threatCategoryFilter != null) ? threatCategoryFilter.asPolicyPredicate() : threatLevelFilter
        .asPolicyPredicate();
  }

  private void pruneUnauthorizedApplicationIds(DashboardFilterDTO dto) {
    List<Application> apps = getApplicationsByIds(dto.applicationFilters);
    dto.applicationFilters.clear();
    for (Application app : apps) {
      dto.applicationFilters.add(app.getId());
    }
  }

  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.APPLICATION)
  protected List<Application> getApplicationsByIds(final List<String> applicationIds) {
    return applicationDAO.getByIds(new LinkedHashSet<>(applicationIds));
  }
}
