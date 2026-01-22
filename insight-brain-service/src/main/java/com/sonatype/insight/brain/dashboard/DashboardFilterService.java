/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class DashboardFilterService
{
  private static final Logger log = LoggerFactory.getLogger(DashboardFilterService.class);

  public static final String ACTIVE_FILTER_NAME = "";

  private final ApplicationDAO applicationDAO;

  private final DashboardFilterDAO dashboardFilterDAO;

  private final CurrentUser currentUser;

  private final DashboardUtils dashboardUtils;

  private final Configuration configuration;

  @Inject
  public DashboardFilterService(ApplicationDAO applicationDAO,
                                DashboardFilterDAO dashboardFilterDAO,
                                CurrentUser currentUser,
                                DashboardUtils dashboardUtils,
                                Configuration configuration)
  {
    this.applicationDAO = applicationDAO;
    this.dashboardFilterDAO = dashboardFilterDAO;
    this.currentUser = currentUser;
    this.dashboardUtils = dashboardUtils;
    this.configuration = configuration;
  }

  /**
   * @since 1.24.0
   */
  public NamedDashboardFilterDTO getActiveDashboardFilterForCurrentUser() throws IOException {
    dashboardUtils.validateDashboardLicensedAndEnabled();

    String username = currentUser.getUsername();
    String realmId = currentUser.getRealmId();
    DashboardFilter dashboardFilter = getNewOrLegacyDashboardFilter(username, realmId, ACTIVE_FILTER_NAME);
    if (dashboardFilter == null) {
      return createDefaultNamedDashboardFilterDTO();
    }
    DashboardFilterDTO dto = JsonUtils.parse(dashboardFilter.getFilter(), DashboardFilterDTO.class);

    pruneUnauthorizedApplicationIds(dto);

    NamedDashboardFilterDTO namedDashboardFilterDTO = new NamedDashboardFilterDTO();
    namedDashboardFilterDTO.name = ACTIVE_FILTER_NAME;
    namedDashboardFilterDTO.needsAcknowledgement = configuration.isNeedsAcknowledgementOfInitialDashboardFilter()
        && !dashboardFilter.isAcknowledged() && dashboardFilter.getBasedOnFilterName() == null;
    namedDashboardFilterDTO.filter = dto;
    namedDashboardFilterDTO.basedOnFilterName = dashboardFilter.getBasedOnFilterName();

    return namedDashboardFilterDTO;
  }

  /**
   * @since 1.24.0
   */
  public List<NamedDashboardFilterDTO> getNamedDashboardFiltersForCurrentUser() throws IOException {
    dashboardUtils.validateDashboardLicensedAndEnabled();

    String username = currentUser.getUsername();
    String realmId = currentUser.getRealmId();
    List<DashboardFilter> dashboardFilters = new ArrayList<>();
    dashboardFilters.addAll(dashboardFilterDAO.getNamedFiltersByUsernameAndRealmId(username, realmId));
    dashboardFilters.addAll(dashboardFilterDAO.getLegacyNamedFiltersByUsername(username));
    
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
    dashboardFilterDTO.repositoryFilters = new ArrayList<>();
    // Threat levels of 0 or 1 are intended to be informational only, and therefore are
    // not pertinent to assessing the "real" risk of a given Application or component
    dashboardFilterDTO.minPolicyThreatLevel = 2;
    dashboardFilterDTO.maxPolicyThreatLevel = 10;
    dashboardFilterDTO.stageTypeFilters = new ArrayList<>();
    dashboardFilterDTO.policyThreatCategoryFilters = new ArrayList<>();
    dashboardFilterDTO.tagFilters = new ArrayList<>();
    
    NamedDashboardFilterDTO namedDashboardFilterDTO = new NamedDashboardFilterDTO();
    namedDashboardFilterDTO.name = ACTIVE_FILTER_NAME;
    namedDashboardFilterDTO.needsAcknowledgement = configuration.isNeedsAcknowledgementOfInitialDashboardFilter();
    namedDashboardFilterDTO.filter = dashboardFilterDTO;
    return namedDashboardFilterDTO;
  }

  /**
   * @since 1.24.0
   */
  public NamedDashboardFilterDTO createOrUpdateDashboardFilterForCurrentUser(
      NamedDashboardFilterDTO namedDashboardFilterDTO)
  {
    dashboardUtils.validateDashboardLicensedAndEnabled();

    String username = currentUser.getUsername();
    String realmId = currentUser.getRealmId();
    String filterName = namedDashboardFilterDTO.name;

    if (!ACTIVE_FILTER_NAME.equals(namedDashboardFilterDTO.name)) {
      // Create or update the named filter
      DashboardFilter dashboardFilter = new DashboardFilter();
      dashboardFilter.setUsername(username);
      dashboardFilter.setRealmId(realmId);
      dashboardFilter.setFilter(JsonUtils.format(namedDashboardFilterDTO.filter));
      dashboardFilter.setName(filterName);
      dashboardFilter.setAcknowledged(configuration.isNeedsAcknowledgementOfInitialDashboardFilter());

      DashboardFilter existingDashboardFilter = getNewOrLegacyDashboardFilter(username, realmId, filterName);
      if (existingDashboardFilter == null) {
        dashboardFilterDAO.insert(dashboardFilter);
      }
      else {
        dashboardFilter.setId(existingDashboardFilter.getId());
        dashboardFilterDAO.update(dashboardFilter);
      }
      auditDashboardFilter(dashboardFilter);
    }

    createOrUpdateActiveFilter(namedDashboardFilterDTO, username, realmId);
    namedDashboardFilterDTO.needsAcknowledgement = false;
    return namedDashboardFilterDTO;
  }

  private void createOrUpdateActiveFilter(
      NamedDashboardFilterDTO namedDashboardFilterDTO,
      String username,
      String realmId)
  {
    DashboardFilter newActiveFilter = new DashboardFilter();
    newActiveFilter.setUsername(username);
    newActiveFilter.setRealmId(realmId);
    newActiveFilter.setFilter(JsonUtils.format(namedDashboardFilterDTO.filter));
    newActiveFilter.setName(ACTIVE_FILTER_NAME);
    newActiveFilter.setAcknowledged(configuration.isNeedsAcknowledgementOfInitialDashboardFilter());

    if (namedDashboardFilterDTO.basedOnFilterName != null) {
      newActiveFilter.setBasedOnFilterName(namedDashboardFilterDTO.basedOnFilterName);
    }
    else if (!namedDashboardFilterDTO.name.equals(ACTIVE_FILTER_NAME)) {
      newActiveFilter.setBasedOnFilterName(namedDashboardFilterDTO.name);
    }

    DashboardFilter existingActiveFilter = getNewOrLegacyDashboardFilter(username, realmId, ACTIVE_FILTER_NAME);
    if (existingActiveFilter != null) {
      newActiveFilter.setId(existingActiveFilter.getId());
      dashboardFilterDAO.update(newActiveFilter);
    }
    else {
      dashboardFilterDAO.insert(newActiveFilter);
    }
    if (ACTIVE_FILTER_NAME.equals(namedDashboardFilterDTO.name)) {
      auditDashboardFilter(newActiveFilter);
    }
  }

  private void auditDashboardFilter(final DashboardFilter dashboardFilter) {
    AuditData.get().setData("filterId", dashboardFilter.getId())
        .setData("filterName",
            dashboardFilter.getName().equals(ACTIVE_FILTER_NAME) ? "(active)" : dashboardFilter.getName());
  }

  private DashboardFilter getNewOrLegacyDashboardFilter(String username, String realmId, String filterName) {
    DashboardFilter dashboardFilter = dashboardFilterDAO.getByUsernameAndRealmIdAndName(username, realmId, filterName);
    if (dashboardFilter == null) {
      dashboardFilter = dashboardFilterDAO.getLegacyByUsernameAndName(username, filterName);
    }
    return dashboardFilter;
  }

  /**
   * @since 1.95.0
   */
  public void deleteDashboardFilterForCurrentUserByFilterName(String filterName) {
    dashboardUtils.validateDashboardLicensedAndEnabled();

    if (filterName == null) {
      throw new BadRequestException("Filter name cannot be null.");
    }
    String username = currentUser.getUsername();
    String realmId = currentUser.getRealmId();
    DashboardFilter appliedDashboardFilter = getNewOrLegacyDashboardFilter(username, realmId, ACTIVE_FILTER_NAME);

    try {
      DashboardFilter dashboardFilter = getNewOrLegacyDashboardFilter(username, realmId, filterName);

      if (dashboardFilter != null) {
        if (appliedDashboardFilter != null && filterName.equals(appliedDashboardFilter.getBasedOnFilterName())) {
          appliedDashboardFilter.setBasedOnFilterName(null);
          appliedDashboardFilter.setRealmId(realmId);
          dashboardFilterDAO.update(appliedDashboardFilter);
        }
        dashboardFilterDAO.delete(dashboardFilter);
        auditDashboardFilter(dashboardFilter);
      }
      else {
        String errorMessage = "Cannot find a filter with name " + filterName + " for user " + username + ".";
        throw new NotFoundException(errorMessage);
      }
    }
    catch (Exception exception) {
      String errorMessage =
          "An exception occurred while trying to find or delete filter name " + filterName + " for user " +
              username + ".";
      log.error(errorMessage, exception);
      throw exception;
    }
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
