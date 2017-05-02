/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
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
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

@Named
public class DashboardFilterService
{
  private static final Logger log = LoggerFactory.getLogger(DashboardFilterService.class);

  public static final String ACTIVE_FILTER_NAME = "";

  private final OwnerDAO ownerDAO;

  private final ApplicationDAO applicationDAO;

  private final ApplicationComponentDAO applicationComponentDAO;

  private final ApplicationService applicationService;

  private final PolicyDAO policyDAO;

  private final DashboardFilterDAO dashboardFilterDAO;

  private final CurrentUser currentUser;

  private final DashboardUtils dashboardUtils;

  private final InsightConfig insightConfig;

  @Inject
  public DashboardFilterService(ApplicationDAO applicationDAO,
                                ApplicationComponentDAO applicationComponentDAO,
                                ApplicationService applicationService,
                                PolicyDAO policyDAO,
                                DashboardFilterDAO dashboardFilterDAO,
                                CurrentUser currentUser,
                                DashboardUtils dashboardUtils,
                                OwnerDAO ownerDAO,
                                InsightConfig insightConfig)
  {
    this.ownerDAO = ownerDAO;
    this.applicationDAO = applicationDAO;
    this.applicationComponentDAO = applicationComponentDAO;
    this.applicationService = applicationService;
    this.policyDAO = policyDAO;
    this.dashboardFilterDAO = dashboardFilterDAO;
    this.currentUser = currentUser;
    this.dashboardUtils = dashboardUtils;
    this.insightConfig = insightConfig;
  }

  /**
   * @since 1.24.0
   */
  public NamedDashboardFilterDTO getActiveDashboardFilterForCurrentUser() throws IOException {
    dashboardUtils.validateDashboardLicensed();

    String username = currentUser.getUsername();
    DashboardFilter dashboardFilter = dashboardFilterDAO.getByUsernameAndName(username, "");
    if (dashboardFilter == null) {
      return createDefaultNamedDashboardFilterDTO();
    }
    DashboardFilterDTO dto = JsonUtils.parse(dashboardFilter.getFilter(), DashboardFilterDTO.class);

    pruneUnauthorizedApplicationIds(dto);

    NamedDashboardFilterDTO namedDashboardFilterDTO = new NamedDashboardFilterDTO();
    namedDashboardFilterDTO.name = ACTIVE_FILTER_NAME;
    namedDashboardFilterDTO.needsAcknowledgement = insightConfig.isNeedsAcknowledgementOfInitialDashboardFilter()
        && !dashboardFilter.isAcknowledged() && dashboardFilter.getBasedOnFilterName() == null;
    namedDashboardFilterDTO.filter = dto;
    namedDashboardFilterDTO.basedOnFilterName = dashboardFilter.getBasedOnFilterName();

    return namedDashboardFilterDTO;
  }

  /**
   * @since 1.24.0
   */
  public List<NamedDashboardFilterDTO> getNamedDashboardFiltersForCurrentUser() throws IOException {
    dashboardUtils.validateDashboardLicensed();

    String username = currentUser.getUsername();
    List<DashboardFilter> dashboardFilters = dashboardFilterDAO.getNamedFiltersByUsername(username);
    
    List<NamedDashboardFilterDTO> namedDashboardFilterDTOs = new ArrayList<>();
    for (DashboardFilter dashboardFilter : dashboardFilters) {
      DashboardFilterDTO dto = JsonUtils.parse(dashboardFilter.getFilter(), DashboardFilterDTO.class);

      NamedDashboardFilterDTO namedDashboardFilterDTO = new NamedDashboardFilterDTO();
      namedDashboardFilterDTO.name = dashboardFilter.getName();
      namedDashboardFilterDTO.needsAcknowledgement = false;
      namedDashboardFilterDTO.filter = dto;

      pruneUnauthorizedApplicationIds(dto);
      namedDashboardFilterDTOs.add(namedDashboardFilterDTO);
    }
    return namedDashboardFilterDTOs;
  }

  private NamedDashboardFilterDTO createDefaultNamedDashboardFilterDTO() {
    DashboardFilterDTO dashboardFilterDTO = new DashboardFilterDTO();
    dashboardFilterDTO.applicationFilters = new ArrayList<>();
    dashboardFilterDTO.organizationFilters = new ArrayList<>();
    // Threat levels of 0 or 1 are intended to be informational only, and therefore are
    // not pertinent to assessing the "real" risk of a given Application or component
    dashboardFilterDTO.minPolicyThreatLevel = 2;
    dashboardFilterDTO.maxPolicyThreatLevel = 10;
    dashboardFilterDTO.stageTypeFilters = new ArrayList<>();
    dashboardFilterDTO.policyThreatCategoryFilters = new ArrayList<>();
    dashboardFilterDTO.tagFilters = new ArrayList<>();
    
    NamedDashboardFilterDTO namedDashboardFilterDTO = new NamedDashboardFilterDTO();
    namedDashboardFilterDTO.name = ACTIVE_FILTER_NAME;
    namedDashboardFilterDTO.needsAcknowledgement = insightConfig.isNeedsAcknowledgementOfInitialDashboardFilter();
    namedDashboardFilterDTO.filter = dashboardFilterDTO;
    return namedDashboardFilterDTO;
  }

  /**
   * @since 1.24.0
   */
  public NamedDashboardFilterDTO createOrUpdateDashboardFilterForCurrentUser(NamedDashboardFilterDTO namedDashboardFilterDTO) {
    dashboardUtils.validateDashboardLicensed();

    String username = currentUser.getUsername();

    if (!ACTIVE_FILTER_NAME.equals(namedDashboardFilterDTO.name)) {
      // Create or update the named filter
      DashboardFilter dashboardFilter = new DashboardFilter();
      dashboardFilter.setUsername(username);
      dashboardFilter.setFilter(JsonUtils.format(namedDashboardFilterDTO.filter));
      dashboardFilter.setName(namedDashboardFilterDTO.name);
      dashboardFilter.setAcknowledged(insightConfig.isNeedsAcknowledgementOfInitialDashboardFilter());

      DashboardFilter existingDashboardFilter = dashboardFilterDAO.getByUsernameAndName(username,
          namedDashboardFilterDTO.name);

      if (existingDashboardFilter == null) {
        dashboardFilterDAO.insert(dashboardFilter);
      }
      else {
        dashboardFilter.setId(existingDashboardFilter.getId());
        dashboardFilterDAO.update(dashboardFilter);
      }
    }

    createOrUpdateActiveFilter(namedDashboardFilterDTO, username);
    namedDashboardFilterDTO.needsAcknowledgement = false;
    return namedDashboardFilterDTO;
  }

  private void createOrUpdateActiveFilter(NamedDashboardFilterDTO namedDashboardFilterDTO, String username) {
    DashboardFilter newActiveFilter = new DashboardFilter();
    newActiveFilter.setUsername(username);
    newActiveFilter.setFilter(JsonUtils.format(namedDashboardFilterDTO.filter));
    newActiveFilter.setName(ACTIVE_FILTER_NAME);
    newActiveFilter.setAcknowledged(insightConfig.isNeedsAcknowledgementOfInitialDashboardFilter());

    if (namedDashboardFilterDTO.basedOnFilterName != null) {
      newActiveFilter.setBasedOnFilterName(namedDashboardFilterDTO.basedOnFilterName);
    }
    else if (!namedDashboardFilterDTO.name.equals(ACTIVE_FILTER_NAME)) {
      newActiveFilter.setBasedOnFilterName(namedDashboardFilterDTO.name);
    }

    DashboardFilter existingActiveFilter = dashboardFilterDAO.getByUsernameAndName(username, ACTIVE_FILTER_NAME);
    if (existingActiveFilter != null) {
      newActiveFilter.setId(existingActiveFilter.getId());
      dashboardFilterDAO.update(newActiveFilter);
    }
    else {
      dashboardFilterDAO.insert(newActiveFilter);
    }
  }

  /**
   * @since 1.24.0
   */
  public List<DashboardFilterErrorResponseDTO> deleteDashboardFiltersForCurrentUserByFilterName(List<String> filterNames) {
    dashboardUtils.validateDashboardLicensed();

    if (isEmpty(filterNames)) {
      throw new BadRequestException("Filter names cannot be null or empty.");
    }
    List<DashboardFilterErrorResponseDTO> errorMessages = new ArrayList<>();
    String username = currentUser.getUsername();
    DashboardFilter appliedDashboardFilter = dashboardFilterDAO.getByUsernameAndName(username, ACTIVE_FILTER_NAME);
    for (String filterName : filterNames) {
      try {
        DashboardFilter dashboardFilter = dashboardFilterDAO.getByUsernameAndName(username, filterName);
        if (dashboardFilter != null) {
          if (appliedDashboardFilter != null && filterName.equals(appliedDashboardFilter.getBasedOnFilterName())) {
            appliedDashboardFilter.setBasedOnFilterName(null);
            dashboardFilterDAO.update(appliedDashboardFilter);
          }
          dashboardFilterDAO.delete(dashboardFilter);
        }
        else {
          String errorMessage = "Cannot find a filter with name " + filterName + " for user " + username + ".";
          errorMessages.add(new DashboardFilterErrorResponseDTO(filterName, errorMessage, 404));
        }
      }
      catch (Exception exception) {
        String errorMessage =
            "An exception occurred while trying to find or delete filter name " + filterName + " for user " + username +
                ".";
        errorMessages.add(new DashboardFilterErrorResponseDTO(filterName, errorMessage, 500));
        log.error(errorMessage, exception);
      }
    }
    return errorMessages;
  }
  
  /**
   * Calculates how many of the entities accessible to the current user are matched by the specified dashboard filter
   * settings.
   */
  public FilterSummaryDTO getFilterSummary(Set<String> organizationIds,
                                           Set<String> applicationIds,
                                           Set<String> stageIds,
                                           Set<String> tagIds,
                                           PolicyThreatCategoryFilter policyThreatCategoryFilter,
                                           PolicyThreatLevelFilter policyThreatLevelFilter)
  {
    dashboardUtils.validateDashboardLicensed();

    long start = System.currentTimeMillis();

    FilterSummaryDTO summary = new FilterSummaryDTO();

    Collection<Application> matchedApplications = applicationService
        .getApplicationsByIdsAndOrganizationIdsAndTagIds(organizationIds, applicationIds, tagIds);
    log.debug("getFilterSummary: Found {} applications filtered by appIds={} and tagIds={} in {} ms.",
        matchedApplications.size(), !isEmpty(applicationIds), !isEmpty(tagIds), System.currentTimeMillis() - start);
    summary.matchedApplications = matchedApplications.size();

    Collection<StageType> matchedStageTypes = dashboardUtils.getStageTypes(stageIds);
    summary.matchedComponents = applicationComponentDAO.getUniqueCountByApplicationIdsAndStageTypeIds(
        dashboardUtils.getApplicationIds(matchedApplications), dashboardUtils.getStageTypeIds(matchedStageTypes));

    Collection<Policy> matchedPolicies = policyDAO.getByOwnerIds(getPolicyOwnerIds(matchedApplications));

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
      if (policyOwnerIds.add(app.getOrganizationId())) {
        for (Owner owner : ownerDAO.walkHierarchy(app.getOrganizationId())) {
          if (owner.getParentOwnerId() == null || !policyOwnerIds.add(owner.getParentOwnerId())) {
            break;
          }
        }
      }
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
