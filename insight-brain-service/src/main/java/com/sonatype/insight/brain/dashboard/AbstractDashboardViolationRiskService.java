/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditService;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.organization.ApplicationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

abstract class AbstractDashboardViolationRiskService
    implements DashboardViolationRiskService
{
  private static final Logger log = LoggerFactory.getLogger(AbstractDashboardViolationRiskService.class);

  private final ApplicationService applicationService;

  private final DashboardUtils dashboardUtils;

  private final AuditService auditService;

  protected AbstractDashboardViolationRiskService(
      ApplicationService applicationService,
      DashboardUtils dashboardUtils,
      AuditService auditService)
  {
    this.applicationService = applicationService;
    this.dashboardUtils = dashboardUtils;
    this.auditService = auditService;
  }

  @Override
  public DashboardResultsDTO<DashboardViolationRiskDTO> get(
      Set<String> organizationIds,
      Set<String> applicationIds,
      Set<String> stageIds,
      Set<String> tagIds,
      PolicyThreatCategoryFilter policyThreatCategoryFilter,
      PolicyThreatLevelFilter policyThreatLevelFilter,
      PolicyViolationStateFilter policyViolationStateFilter,
      String orderBy,
      Integer maxDaysOld,
      int page,
      int pageSize)
  {
    dashboardUtils.validateDashboardLicensedAndEnabledForApplications();

    validateMaxDaysOld(maxDaysOld);

    long start = System.currentTimeMillis();

    List<Application> applications = getApplications(organizationIds, applicationIds, tagIds);

    AuditData.get() //
        .setData("selectedOrganizations", auditService.getSelectedOrganizationsById(organizationIds)) //
        .setData("selectedApplications",
            auditService.getSelectedApplicationsById(applicationIds, organizationIds, applications)) //
        .setSelectedApplicationCategories(auditService.getSelectedApplicationCategoriesById(tagIds)) //
        .setData("inspectedApplicationCount", applications.size());

    // This also validates the stageIds
    Set<StageType> stageTypes = dashboardUtils.getStageTypes(stageIds);

    Date minDate =
        (maxDaysOld == null) ? null : new Date(Instant.now().minus(Duration.ofDays(maxDaysOld)).toEpochMilli());

    DashboardResultsDTO<DashboardViolationRiskDTO> result =
        load(applications, stageTypes, policyThreatCategoryFilter, policyThreatLevelFilter,
            policyViolationStateFilter, orderBy, minDate, page, pageSize);

    log.debug("get finished in {} ms", System.currentTimeMillis() - start);

    return result;
  }

  protected abstract DashboardResultsDTO<DashboardViolationRiskDTO> load(
      List<Application> applications,
      Set<StageType> stageTypes,
      PolicyThreatCategoryFilter policyThreatCategoryFilter,
      PolicyThreatLevelFilter policyThreatLevelFilter,
      PolicyViolationStateFilter policyViolationStateFilter,
      String orderBy,
      Date minDate,
      int page,
      int pageSize);

  private List<Application> getApplications(
      Set<String> organizationIds,
      Set<String> applicationIds,
      Set<String> tagIds)
  {
    long start = System.currentTimeMillis();

    List<Application> applications =
        applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(organizationIds, applicationIds, tagIds);

    log.debug("load: Found {} applications filtered by appIds={} and tagIds={} in {} ms.",
        applications.size(), !isEmpty(applicationIds), !isEmpty(tagIds), System.currentTimeMillis() - start);

    return applications;
  }

  private void validateMaxDaysOld(Integer maxDaysOld) {
    if (maxDaysOld != null && maxDaysOld < 1) {
      throw new IllegalArgumentException("Max Days Old must be a positive integer");
    }
  }
}
