/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditService;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.organization.ApplicationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractComponentRiskService
    implements DashboardComponentRiskService
{
  private static final Logger log = LoggerFactory.getLogger(AbstractComponentRiskService.class);

  protected final ApplicationService applicationService;

  protected final DashboardUtils dashboardUtils;

  protected final AuditService auditService;

  public AbstractComponentRiskService(
      final ApplicationService applicationService,
      final DashboardUtils dashboardUtils,
      final AuditService auditService)
  {
    this.applicationService = applicationService;
    this.dashboardUtils = dashboardUtils;
    this.auditService = auditService;
  }

  @Override
  public DashboardResultsDTO<ComponentRiskDTO> getComponentRisks(
      Set<String> organizationIds,
      Set<String> applicationIds,
      Set<String> stageIds,
      Set<String> tagIds,
      PolicyThreatCategoryFilter policyThreatCategoryFilter,
      PolicyThreatLevelFilter policyThreatLevelFilter,
      PolicyViolationStateFilter policyViolationStateFilter,
      String orderBy,
      int page,
      int pageSize)
  {
    dashboardUtils.validateDashboardLicensedAndEnabledForApplications();

    long start = System.currentTimeMillis();

    List<Application> applications = getApplications(organizationIds, applicationIds, tagIds);

    AuditData.get()
        .setData("selectedOrganizations", auditService.getSelectedOrganizationsById(organizationIds))
        .setData("selectedApplications", auditService.getSelectedApplicationsById(applicationIds, organizationIds))
        .setSelectedApplicationCategories(auditService.getSelectedApplicationCategoriesById(tagIds))
        .setData("inspectedApplicationCount", applications.size());

    DashboardResultsDTO<ComponentRiskDTO> result = load(applications, stageIds, policyThreatCategoryFilter,
        policyThreatLevelFilter, policyViolationStateFilter, orderBy, page, pageSize);

    log.debug("getComponentRisks finished in {} ms", System.currentTimeMillis() - start);

    return result;
  }

  abstract DashboardResultsDTO<ComponentRiskDTO> load(
      List<Application> applications,
      Set<String> stageIds,
      PolicyThreatCategoryFilter policyThreatCategoryFilter,
      PolicyThreatLevelFilter policyThreatLevelFilter,
      PolicyViolationStateFilter policyViolationStateFilter,
      String orderBy,
      int page,
      int pageSize);

  protected List<Application> getApplications(
      Set<String> organizationIds,
      Set<String> applicationIds,
      Set<String> tagIds)
  {
    return applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(organizationIds, applicationIds, tagIds);
  }
}
