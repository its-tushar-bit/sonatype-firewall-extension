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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.ApplicationStageView;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.ApplicationView;

import com.google.common.base.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ApplicationRiskService
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationRiskService.class);

  private static final String SECRET_JOIN_STRING = "$";

  private final ApplicationService applicationService;

  private final PolicyViolationLoader policyViolationLoader;

  private final DashboardUtils dashboardUtils;

  @Inject
  public ApplicationRiskService(ApplicationService applicationService,
                                PolicyViolationLoader policyViolationLoader,
                                DashboardUtils dashboardUtils)
  {
    this.applicationService = applicationService;
    this.policyViolationLoader = policyViolationLoader;
    this.dashboardUtils = dashboardUtils;
  }

  /**
   * @since 1.11.0
   */
  public DashboardResultsDTO<ApplicationRiskScoreDTO> getApplicationRisks(final Set<String> organizationIds,
                                                                          final Set<String> applicationIds,
                                                                          final Set<String> stageIds,
                                                                          final Set<String> tagIds,
                                                                          final PolicyThreatCategoryFilter policyThreatCategoryFilter,
                                                                          final PolicyThreatLevelFilter policyThreatLevelFilter,
                                                                          final PolicyViolationStateFilter policyViolationStateFilter,
                                                                          final String orderBy,
                                                                          final int maxResults)
  {
    dashboardUtils.validateDashboardLicensed();

    long start = System.currentTimeMillis();

    ApplicationRiskScoreDTOComparator applicationRiskComparator = new ApplicationRiskScoreDTOComparator(orderBy);
    List<Application> appsToSearch = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(organizationIds,
        applicationIds, tagIds);
    log.debug("Loaded {} applications", appsToSearch.size());
    Set<StageType> stageTypes = dashboardUtils.getStageTypes(stageIds);
    Predicate<PolicyViolation> filter = dashboardUtils.buildViolationFilter(policyThreatCategoryFilter,
        policyThreatLevelFilter, policyViolationStateFilter);

    Collection<ApplicationView> appViews = policyViolationLoader.getViolations(appsToSearch, stageTypes, false, filter);

    List<ApplicationRiskScoreDTO> applicationRiskScoreDTOs = createApplicationRiskScores(appViews);
    Collections.sort(applicationRiskScoreDTOs, applicationRiskComparator);
    DashboardResultsDTO<ApplicationRiskScoreDTO> result = new DashboardResultsDTO<>();
    result.numResults = applicationRiskScoreDTOs.size();
    result.dashboardResults = 
        applicationRiskScoreDTOs.subList(0, Math.min(applicationRiskScoreDTOs.size(), maxResults));

    log.debug("getApplicationRisks finished in {} ms", System.currentTimeMillis() - start);

    return result;
  }

  private List<ApplicationRiskScoreDTO> createApplicationRiskScores(Collection<ApplicationView> appViews) {
    List<ApplicationRiskScoreDTO> applicationRiskScores = new ArrayList<>(appViews.size());
    for (ApplicationView appView : appViews) {
      ApplicationRiskScoreDTO applicationRiskScore = new ApplicationRiskScoreDTO(appView.getApplication().getName(),
          appView.getApplication().getPublicId());

      updateTotalApplicationRisks(applicationRiskScore, appView.getStageViews());
      if (applicationRiskScore.totalApplicationRisk.totalRisk <= 0) {
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
    final Map<String, PolicyViolation> compHashToViolation = new HashMap<>();
    for (ApplicationStageView appStageView : appStageViews) {
      for (final PolicyViolation violation1 : appStageView.getFilteredViolations()) {
        String vioHash = violation1.getPolicyId() + SECRET_JOIN_STRING + violation1.getHash();
        PolicyViolation existing = compHashToViolation.get(vioHash);
        if (existing == null) {
          // first time we see a violation, we make it
          compHashToViolation.put(vioHash, violation1);
        }
        else if (violation1.getTime().after(existing.getTime())) {
          // we have a newer violation, update existing
          compHashToViolation.put(vioHash, violation1);
        }
      }
    }

    // update the total risks based on the deduped risks
    for (final PolicyViolation violation : compHashToViolation.values()) {
      updateRisk(applicationRiskScore.totalApplicationRisk, violation.getThreatLevel());
    }
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
