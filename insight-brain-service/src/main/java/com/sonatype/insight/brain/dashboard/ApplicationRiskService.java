/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.CIApplicationDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditUtils;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.CIApplicationFilter;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationComparator;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.ApplicationStageView;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.ApplicationView;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ApplicationRiskService
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationRiskService.class);

  private final ApplicationService applicationService;

  private final OrganizationDAO organizationDAO;

  private final ApplicationDAO applicationDAO;

  private final PolicyViolationLoader policyViolationLoader;

  private final DashboardUtils dashboardUtils;

  @Inject
  public ApplicationRiskService(ApplicationService applicationService,
                                OrganizationDAO organizationDAO,
                                ApplicationDAO applicationDAO,
                                PolicyViolationLoader policyViolationLoader,
                                DashboardUtils dashboardUtils)
  {
    this.applicationService = applicationService;
    this.organizationDAO = organizationDAO;
    this.applicationDAO = applicationDAO;
    this.policyViolationLoader = policyViolationLoader;
    this.dashboardUtils = dashboardUtils;
  }

  /**
   * @since 1.11.0
   */
  public DashboardResultsDTO<ApplicationRiskScoreDTO> getApplicationRisks(
      final Set<String> organizationIds,
      final Set<String> applicationIds,
      final Set<String> stageIds,
      final Set<String> tagIds,
      final PolicyThreatCategoryFilter policyThreatCategoryFilter,
      final PolicyThreatLevelFilter policyThreatLevelFilter,
      final PolicyViolationStateFilter policyViolationStateFilter,
      final String orderBy,
      int page,
      int pageSize)
  {
    return getApplicationRisks(organizationIds, applicationIds, stageIds, tagIds, policyThreatCategoryFilter,
        policyThreatLevelFilter, policyViolationStateFilter, orderBy, page, pageSize, false, false);
  }

  public DashboardResultsDTO<CIApplicationDTO> getCIApplicationRisk(final CIApplicationFilter filter) {
    checkReadPermission(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);

    if (filter.getPage() < 0 || filter.getPageSize() <= 0) {
      throw new BadRequestException("Page and page size must be greater than 0");
    }

    final List<String> appsWithoutCI =
        applicationDAO.getApplicationsWithoutCITriggeredEvaluations(filter.getSinceUtcTimestamp(),
            filter.getOptionalFilterApplicationNamesBy());
    final DashboardResultsDTO<ApplicationRiskScoreDTO> fullResults =
        getApplicationRisks(Collections.emptySet(), new HashSet<>(appsWithoutCI), Collections.emptySet(),
            Collections.emptySet(), new PolicyThreatCategoryFilter(), new PolicyThreatLevelFilter(0, 10),
            new PolicyViolationStateFilter(), filter.getOptionalOrderBy(), filter.getPage(), filter.getPageSize(),
            true, true);
    final List<CIApplicationDTO> totalRiskResults = fullResults.dashboardResults.stream().map(applicationRiskScoreDTO ->
        new CIApplicationDTO(applicationRiskScoreDTO.applicationId, applicationRiskScoreDTO.applicationName,
            applicationRiskScoreDTO.totalApplicationRisk.totalRisk)).collect(Collectors.toList());

    return new DashboardResultsDTO<>(totalRiskResults, fullResults.numResults);
  }

  private DashboardResultsDTO<ApplicationRiskScoreDTO> getApplicationRisks(
      final Set<String> organizationIds,
      final Set<String> applicationIds,
      final Set<String> stageIds,
      final Set<String> tagIds,
      final PolicyThreatCategoryFilter policyThreatCategoryFilter,
      final PolicyThreatLevelFilter policyThreatLevelFilter,
      final PolicyViolationStateFilter policyViolationStateFilter,
      final String orderBy,
      int page,
      int pageSize,
      boolean includeZeroRisk,
      boolean excludeAllAppsByDefault)
  {
    dashboardUtils.validateDashboardLicensedAndEnabledForApplications();

    long start = System.currentTimeMillis();

    ApplicationRiskScoreDTOComparator applicationRiskComparator = new ApplicationRiskScoreDTOComparator(orderBy);
    List<Application> appsToSearch =
        excludeAllAppsByDefault ? applicationService.getAppsByIds(organizationIds, applicationIds, tagIds) :
            applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(organizationIds, applicationIds, tagIds);
    log.debug("Loaded {} applications", appsToSearch.size());

    AuditData.get() //
        .setData("selectedOrganizations", AuditUtils.getSelectedOrganizationsById(organizationIds)) //
        .setData("selectedApplications",
            AuditUtils.getSelectedApplicationsById(applicationIds, organizationIds, appsToSearch)) //
        .setSelectedApplicationCategories(AuditUtils.getSelectedApplicationCategoriesById(tagIds)) //
        .setData("inspectedApplicationCount", appsToSearch.size());

    Set<StageType> stageTypes = dashboardUtils.getStageTypes(stageIds);

    Collection<ApplicationView> appViews =
        policyViolationLoader.getViolations(appsToSearch, stageTypes, false, policyThreatLevelFilter,
            policyThreatCategoryFilter, policyViolationStateFilter);

    List<ApplicationRiskScoreDTO> applicationRiskScoreDTOs = createApplicationRiskScores(appViews, includeZeroRisk);
    applicationRiskScoreDTOs.sort(applicationRiskComparator);
    DashboardResultsDTO<ApplicationRiskScoreDTO> result = new DashboardResultsDTO<>();
    result.numResults = applicationRiskScoreDTOs.size();

    if (applicationRiskScoreDTOs.isEmpty()) {
      result.dashboardResults = new ArrayList<>();
    }
    else {
      List<List<ApplicationRiskScoreDTO>> pages = Lists.partition(applicationRiskScoreDTOs, pageSize);
      result.dashboardResults = page >= pages.size() ? new ArrayList<>() : pages.get(page);
    }

    AuditData.get().setData("resultRecordCount", result.numResults);

    log.debug("getApplicationRisks finished in {} ms", System.currentTimeMillis() - start);

    return result;
  }

  private List<ApplicationRiskScoreDTO> createApplicationRiskScores(
      Collection<ApplicationView> appViews,
      boolean includeZeroRisk)
  {
    Map<String, String> orgNames = new HashMap<>();
    List<ApplicationRiskScoreDTO> applicationRiskScores = new ArrayList<>(appViews.size());
    for (ApplicationView appView : appViews) {
      // We must limit ourselves only to the organization name in order to avoid leaking other information
      // to users which may not have READ access to organization details. Organization names can still be
      // shown in exports similar to how we show organization names in the sidebar via the SidebarService.
      // Also store the org names once fetched to avoid multiple fetches incurring a performance penalty.
      String orgName = orgNames.computeIfAbsent(appView.getApplication().getOrganizationId(),
          orgId -> organizationDAO.getByIdNotNull(orgId).getName());

      ApplicationRiskScoreDTO applicationRiskScore = new ApplicationRiskScoreDTO(orgName,
          appView.getApplication().getName(), appView.getApplication().getPublicId());

      updateTotalApplicationRisks(applicationRiskScore, appView.getStageViews());
      if (!includeZeroRisk && applicationRiskScore.totalApplicationRisk.totalRisk <= 0) {
        continue;
      }
      applicationRiskScores.add(applicationRiskScore);

      for (ApplicationStageView appStageView : appView.getStageViews()) {
        if (!appStageView.getFilteredViolations().isEmpty()) {
          StageRiskScoreDTO stageRiskScore = new StageRiskScoreDTO(appStageView.getStageType().getId());
          stageRiskScore.stageTypeName = appStageView.getStageType().getName();
          stageRiskScore.scanId = appStageView.getLastEvaluation().getScanId();
          for (PolicyViolation violation : appStageView.getFilteredViolations()) {
            updateRisk(stageRiskScore.risk, violation.getThreatLevel());
          }
          applicationRiskScore.addStageRiskScore(stageRiskScore);
        }
      }
    }
    return applicationRiskScores;
  }

  private void updateTotalApplicationRisks(final ApplicationRiskScoreDTO applicationRiskScore,
                                           final Collection<ApplicationStageView> appStageViews)
  {
    // squish down any dupes we have across stages
    final SortedSet<PolicyViolation> dedupedViolations = new TreeSet<>(PolicyViolationComparator.COMPARATOR);
    for (ApplicationStageView appStageView : sortByLastEvaluationTimeDescending(appStageViews)) {
      for (final PolicyViolation violation : appStageView.getFilteredViolations()) {
        // first time we see a violation, we make it, any later occurrence is from an older evaluation
        dedupedViolations.add(violation);
      }
    }

    // update the total risks based on the deduped risks
    for (final PolicyViolation violation : dedupedViolations) {
      updateRisk(applicationRiskScore.totalApplicationRisk, violation.getThreatLevel());
    }
  }

  private List<ApplicationStageView> sortByLastEvaluationTimeDescending(
      Collection<ApplicationStageView> appStageViews)
  {
    List<ApplicationStageView> sorted = new ArrayList<>(appStageViews);
    sorted.sort((appStageView1, appStageView2) -> {
      PolicyEvaluation eval1 = appStageView1.getLastEvaluation();
      PolicyEvaluation eval2 = appStageView2.getLastEvaluation();
      if (eval1 == null || eval2 == null) {
        return eval1 == eval2 ? 0 : (eval1 == null ? 1 : -1);
      }
      return eval2.getTime().compareTo(eval1.getTime());
    });
    return sorted;
  }

  private void updateRisk(RiskDTO risk, int threatLevel) {
    if (threatLevel >= 8) {
      risk.criticalRisk += threatLevel;
    }
    else if (threatLevel >= 4) {
      risk.severeRisk += threatLevel;
    }
    else if (threatLevel >= 2) {
      risk.moderateRisk += threatLevel;
    }
    else {
      risk.lowRisk += threatLevel;
    }
    risk.totalRisk += threatLevel;
  }

  @Authorize(permission = Permission.READ)
  void checkReadPermission(
      @SuppressWarnings("unused") @AuthzContext(Key.TYPE) OwnerType ownerType,
      @SuppressWarnings("unused") @AuthzContext(Key.ID) String ownerId)
  {
  }
}
