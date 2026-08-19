/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditService;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationComparator;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.OwnerStageView;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.OwnerView;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractApplicationRiskService
    implements ApplicationRiskService
{
  private static final Logger log = LoggerFactory.getLogger(AbstractApplicationRiskService.class);

  private final ApplicationService applicationService;

  private final OrganizationDAO organizationDAO;

  private final PolicyViolationLoader policyViolationLoader;

  private final DashboardUtils dashboardUtils;

  private final AuditService auditService;

  public AbstractApplicationRiskService(
      final ApplicationService applicationService,
      final OrganizationDAO organizationDAO,
      final PolicyViolationLoader policyViolationLoader,
      final DashboardUtils dashboardUtils,
      final AuditService auditService)
  {
    this.applicationService = applicationService;
    this.organizationDAO = organizationDAO;
    this.policyViolationLoader = policyViolationLoader;
    this.dashboardUtils = dashboardUtils;
    this.auditService = auditService;
  }

  @Override
  public ApplicationRiskScoreDTO getRiskForOwner(
      final Owner owner,
      final Set<StageType> stageTypes)
  {
    final OwnerView appView =
        policyViolationLoader.getViolations(
            Collections.singleton(owner),
            stageTypes,
            false,
            null,
            null,
            null)
            .stream()
            .findFirst()
            .orElse(null);

    return createApplicationRiskScore(appView, true);
  }

  private List<ApplicationRiskScoreDTO> getRiskForProvidedApps(
      final List<Application> appsToSearch,
      final Set<String> stageIds,
      final PolicyThreatCategoryFilter policyThreatCategoryFilter,
      final PolicyThreatLevelFilter policyThreatLevelFilter,
      final PolicyViolationStateFilter policyViolationStateFilter,
      final boolean includeZeroRiskApplications)
  {
    final Set<StageType> stageTypes = dashboardUtils.getStageTypes(stageIds);

    final Collection<OwnerView> appViews =
        policyViolationLoader.getViolations(appsToSearch, stageTypes, false, policyThreatLevelFilter,
            policyThreatCategoryFilter, policyViolationStateFilter);

    return createApplicationRiskScores(appViews, includeZeroRiskApplications);
  }

  /**
   * @since 1.11.0
   */
  @Override
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
    dashboardUtils.validateDashboardLicensedAndEnabledForApplications();

    long start = System.currentTimeMillis();

    ApplicationRiskScoreDTOComparator applicationRiskComparator = new ApplicationRiskScoreDTOComparator(orderBy);
    List<Application> appsToSearch =
        applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(organizationIds, applicationIds, tagIds);
    log.debug("Loaded {} applications", appsToSearch.size());

    AuditData.get() //
        .setData("selectedOrganizations", auditService.getSelectedOrganizationsById(organizationIds)) //
        .setData("selectedApplications",
            auditService.getSelectedApplicationsById(applicationIds, organizationIds, appsToSearch)) //
        .setSelectedApplicationCategories(auditService.getSelectedApplicationCategoriesById(tagIds)) //
        .setData("inspectedApplicationCount", appsToSearch.size());

    List<ApplicationRiskScoreDTO> applicationRiskScoreDTOs = getRiskForProvidedApps(
        appsToSearch,
        stageIds,
        policyThreatCategoryFilter,
        policyThreatLevelFilter,
        policyViolationStateFilter,
        false);

    applicationRiskScoreDTOs.sort(applicationRiskComparator);
    DashboardResultsDTO<ApplicationRiskScoreDTO> result = new DashboardResultsDTO<>();

    if (applicationRiskScoreDTOs.isEmpty()) {
      result.dashboardResults = new ArrayList<>();
    }
    else {
      List<List<ApplicationRiskScoreDTO>> pages = Lists.partition(applicationRiskScoreDTOs, pageSize);
      result.dashboardResults = page >= pages.size() ? new ArrayList<>() : pages.get(page);
      result.hasNextPage = pages.size() > (page + 1);
    }

    log.debug("getApplicationRisks finished in {} ms", System.currentTimeMillis() - start);

    return result;
  }

  @Override
  public DashboardResultsDTO<ApplicationRiskScoreDTO> getApplicationRiskCards(
      final Set<String> organizationIds,
      final Set<String> applicationIds,
      final Set<String> stageIds,
      final Set<String> tagIds,
      final PolicyThreatCategoryFilter policyThreatCategoryFilter,
      final PolicyThreatLevelFilter policyThreatLevelFilter,
      final PolicyViolationStateFilter policyViolationStateFilter)
  {
    dashboardUtils.validateDashboardLicensedAndEnabledForApplications();

    List<Application> appsToSearch =
        applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(organizationIds, applicationIds, tagIds);

    List<ApplicationRiskScoreDTO> cards = getRiskForProvidedApps(
        appsToSearch,
        stageIds,
        policyThreatCategoryFilter,
        policyThreatLevelFilter,
        policyViolationStateFilter,
        true);

    DashboardResultsDTO<ApplicationRiskScoreDTO> result = new DashboardResultsDTO<>();
    result.dashboardResults = cards;
    result.hasNextPage = false;
    return result;
  }

  private List<ApplicationRiskScoreDTO> createApplicationRiskScores(
      final Collection<OwnerView> appViews,
      final boolean includeZeroRiskApplications)
  {
    // Bulk path feeds CSV export; reject non-Application owners at the boundary.
    for (OwnerView appView : appViews) {
      if (appView != null && appView.getOwner() != null && appView.getApplication() == null) {
        throw new IllegalStateException(
            "createApplicationRiskScores requires Application-typed owners; got "
                + appView.getOwner().getClass().getSimpleName()
                + " (id=" + appView.getOwner().getId() + ")");
      }
    }
    List<ApplicationRiskScoreDTO> applicationRiskScores = new ArrayList<>(appViews.size());
    for (OwnerView appView : appViews) {
      final ApplicationRiskScoreDTO applicationRiskScore = createApplicationRiskScore(
          appView,
          includeZeroRiskApplications);

      if (applicationRiskScore != null) {
        applicationRiskScores.add(applicationRiskScore);
      }
    }
    return applicationRiskScores;
  }

  private ApplicationRiskScoreDTO createApplicationRiskScore(
      final OwnerView appView,
      final boolean returnNullAndSkipStageViewCalculationsWhenRiskIsZero)
  {
    // We must limit ourselves only to the organization name in order to avoid leaking other information
    // to users which may not have READ access to organization details. Organization names can still be
    // shown in exports similar to how we show organization names in the sidebar via the SidebarService.
    // Also store the org names once fetched to avoid multiple fetches incurring a performance penalty.
    if (appView == null || appView.getOwner() == null) {
      return null;
    }

    final Application application = appView.getApplication();
    final ApplicationRiskScoreDTO applicationRiskScore;
    if (application != null) {
      String organizationId = application.getOrganizationId();
      String orgName = organizationDAO.getByIdNotNull(organizationId).getName();
      applicationRiskScore = new ApplicationRiskScoreDTO(orgName, organizationId,
          application.getName(), application.getPublicId(), application.getId());
    }
    else {
      // Owner-only DTO — only totalApplicationRisk.totalRisk is safe to read.
      applicationRiskScore = new ApplicationRiskScoreDTO(null, null, null, null, appView.getOwner().getId());
    }

    updateTotalApplicationRisks(applicationRiskScore, appView.getStageViews());

    if (!returnNullAndSkipStageViewCalculationsWhenRiskIsZero
        && applicationRiskScore.totalApplicationRisk.totalRisk <= 0)
    {
      return null;
    }

    for (OwnerStageView appStageView : appView.getStageViews()) {
      if (!appStageView.getFilteredViolations().isEmpty()) {
        StageRiskScoreDTO stageRiskScore = new StageRiskScoreDTO(appStageView.getStageType().getId());
        stageRiskScore.stageTypeName = appStageView.getStageType().getName();
        stageRiskScore.scanId = appStageView.getLastEvaluation().getScanId();
        if (appStageView.getLastEvaluation().getTime() != null) {
          stageRiskScore.evaluationTime = appStageView.getLastEvaluation().getTime().getTime();
        }
        for (PolicyViolation violation : appStageView.getFilteredViolations()) {
          updateRisk(stageRiskScore.risk, violation.getThreatLevel());
        }
        applicationRiskScore.addStageRiskScore(stageRiskScore);
      }
    }

    List<OwnerStageView> stagesByLatestEval = sortByLastEvaluationTimeDescending(appView.getStageViews());
    if (!stagesByLatestEval.isEmpty()) {
      PolicyEvaluation latest = stagesByLatestEval.get(0).getLastEvaluation();
      if (latest != null && latest.getTime() != null) {
        applicationRiskScore.lastEvaluationTime = latest.getTime().getTime();
      }
    }

    return applicationRiskScore;
  }

  private void updateTotalApplicationRisks(
      final ApplicationRiskScoreDTO applicationRiskScore,
      final Collection<OwnerStageView> appStageViews)
  {
    // squish down any dupes we have across stages
    final SortedSet<PolicyViolation> dedupedViolations = new TreeSet<>(PolicyViolationComparator.COMPARATOR);
    for (OwnerStageView appStageView : sortByLastEvaluationTimeDescending(appStageViews)) {
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

  private List<OwnerStageView> sortByLastEvaluationTimeDescending(
      Collection<OwnerStageView> appStageViews)
  {
    List<OwnerStageView> sorted = new ArrayList<>(appStageViews);
    sorted.sort((appStageView1, appStageView2) -> {
      PolicyEvaluation eval1 = appStageView1.getLastEvaluation();
      PolicyEvaluation eval2 = appStageView2.getLastEvaluation();
      boolean eval1Missing = eval1 == null || eval1.getTime() == null;
      boolean eval2Missing = eval2 == null || eval2.getTime() == null;
      if (eval1Missing || eval2Missing) {
        return eval1Missing == eval2Missing ? 0 : (eval1Missing ? 1 : -1);
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
}
