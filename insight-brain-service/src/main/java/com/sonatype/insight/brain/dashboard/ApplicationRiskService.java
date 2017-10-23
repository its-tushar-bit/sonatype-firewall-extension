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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.model.HasStringId;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ApplicationRiskService
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationRiskService.class);

  private static final String SECRET_JOIN_STRING = "$";

  private final ApplicationService applicationService;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final DashboardUtils dashboardUtils;

  @Inject
  public ApplicationRiskService(ApplicationService applicationService,
                                PolicyEvaluationDAO policyEvaluationDAO,
                                PolicyViolationDAO policyViolationDAO,
                                DashboardUtils dashboardUtils)
  {
    this.applicationService = applicationService;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.policyViolationDAO = policyViolationDAO;
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

    List<PolicyEvaluation> evaluations = policyEvaluationDAO.getLastByApplicationIdsAndStageIds(
        dashboardUtils.getApplicationIds(appsToSearch), dashboardUtils.getStageTypeIds(stageTypes));
    log.debug("Loaded {} policy evaluations", evaluations.size());

    Map<String, PolicyEvaluation> policyEvaluationsById = mapCollectionById(evaluations);
    Map<String, List<PolicyViolation>> policyViolationsByAppId = createAllPolicyViolations(filter, evaluations,
        policyEvaluationsById);

    Iterable<ApplicationRiskScoreDTO> applicationRisks = createApplicationRiskScores(appsToSearch, stageTypes,
        policyEvaluationsById, policyViolationsByAppId);

    List<ApplicationRiskScoreDTO> applicationRiskScoreDTOs = filterApplicationRiskScore(applicationRisks);
    Collections.sort(applicationRiskScoreDTOs, applicationRiskComparator);
    DashboardResultsDTO<ApplicationRiskScoreDTO> result = new DashboardResultsDTO<>();
    result.numResults = applicationRiskScoreDTOs.size();
    result.dashboardResults = 
        applicationRiskScoreDTOs.subList(0, Math.min(applicationRiskScoreDTOs.size(), maxResults));

    log.debug("getApplicationRisks finished in {} ms", System.currentTimeMillis() - start);

    return result;
  }

  private Map<String, List<PolicyViolation>> createAllPolicyViolations(final Predicate<PolicyViolation> filter,
                                                                       final List<PolicyEvaluation> evaluations,
                                                                       final Map<String, PolicyEvaluation> policyEvaluationsById)
  {
    Map<String, List<PolicyViolation>> violationsByAppId = new HashMap<>();
    for (PolicyViolation violation : getPolicyViolations(evaluations, filter)) {
      PolicyEvaluation sourceEvaluation = policyEvaluationsById.get(violation.getPolicyEvaluationId());
      List<PolicyViolation> violations = violationsByAppId.get(sourceEvaluation.getApplicationId());
      if (violations == null) {
        violations = new ArrayList<>();
        violationsByAppId.put(sourceEvaluation.getApplicationId(), violations);
      }
      violations.add(violation);
    }
    return violationsByAppId;
  }

  private Iterable<ApplicationRiskScoreDTO> createApplicationRiskScores(final List<Application> appsToSearch,
                                                                        final Set<StageType> stagesToSearch,
                                                                        final Map<String, PolicyEvaluation> policyEvaluationsById,
                                                                        final Map<String, List<PolicyViolation>> violationsByAppId)
  {
    List<ApplicationRiskScoreDTO> applicationRiskScores = new ArrayList<>();
    for (Application application : appsToSearch) {
      ApplicationRiskScoreDTO applicationRisk = new ApplicationRiskScoreDTO(application.getName(),
          application.getPublicId());

      List<PolicyViolation> violationsForApp = violationsByAppId.get(application.getId());
      if (violationsForApp != null) {
        Map<String, StageRiskScoreDTO> stageRiskScoresByStageTypeId = new LinkedHashMap<>();
        for (StageType stageType : stagesToSearch) {
          // this merely establishes the order of stages within the map
          stageRiskScoresByStageTypeId.put(stageType.getId(), null);
        }
        for (PolicyViolation violation : violationsForApp) {
          PolicyEvaluation currentPolicyEvaluation = policyEvaluationsById.get(violation.getPolicyEvaluationId());
          updateStageRisk(stageRiskScoresByStageTypeId, violation, currentPolicyEvaluation.getStageTypeId(),
              currentPolicyEvaluation.getScanId());
        }
        for (StageRiskScoreDTO stageRiskScore : stageRiskScoresByStageTypeId.values()) {
          if (stageRiskScore != null) {
            applicationRisk.addStageRiskScore(stageRiskScore);
          }
        }

        updateTotalApplicationRisks(applicationRisk, violationsForApp);
      }
      applicationRiskScores.add(applicationRisk);
    }

    return applicationRiskScores;
  }

  private <T extends HasStringId> Map<String, T> mapCollectionById(Collection<T> col) {
    Map<String, T> result = new HashMap<>();
    for (T item : col) {
      result.put(item.getId(), item);
    }
    return result;
  }

  private List<PolicyViolation> getPolicyViolations(final List<PolicyEvaluation> evaluations,
                                                    final Predicate<PolicyViolation> violationFilter)
  {
    Set<String> evaluationIds = Sets.newHashSet(Iterables.transform(evaluations, DashboardUtils.hasIdIdSelector));
    List<PolicyViolation> violations = policyViolationDAO.getByEvaluationIds(evaluationIds);
    log.debug("Loaded {} policy violations", violations.size());
    return dashboardUtils.filter(violations, violationFilter);
  }

  private void updateTotalApplicationRisks(final ApplicationRiskScoreDTO applicationRiskScore,
                                           final List<PolicyViolation> violationsForApp)
  {
    // squish down any dupes we have across stages
    final Map<String, PolicyViolation> compHashToViolation = new HashMap<>();
    for (final PolicyViolation violation1 : violationsForApp) {
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

    // update the total risks based on the deduped risks
    for (final PolicyViolation violation : compHashToViolation.values()) {
      updateRisk(applicationRiskScore.totalApplicationRisk, violation.getThreatLevel());
    }
  }

  private void updateStageRisk(Map<String, StageRiskScoreDTO> stageRiskScoresByStageTypeId,
                               PolicyViolation violation,
                               String stageTypeId,
                               String scanId)
  {
    StageRiskScoreDTO currentStageRiskScore = stageRiskScoresByStageTypeId.get(stageTypeId);
    if (currentStageRiskScore == null) {
      StageType stage = StageTypes.getById(stageTypeId);
      currentStageRiskScore = new StageRiskScoreDTO(stage.getId());
      currentStageRiskScore.stageTypeName = stage.getName();
      currentStageRiskScore.scanId = scanId;
      stageRiskScoresByStageTypeId.put(stageTypeId, currentStageRiskScore);
    }
    updateRisk(currentStageRiskScore.risk, violation.getThreatLevel());
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

  /**
   * @param applicationRisks - Risks we want to filter.
   * @return the risks filtered. Any guys with a Risk of 0 are removed.
   */
  private List<ApplicationRiskScoreDTO> filterApplicationRiskScore(final Iterable<ApplicationRiskScoreDTO> applicationRisks)
  {
    List<ApplicationRiskScoreDTO> filteredApplicationRiskScores = Lists.newArrayList(Iterables.filter(applicationRisks,
        new Predicate<ApplicationRiskScoreDTO>()
        {

          @Override
          public boolean apply(@Nullable final ApplicationRiskScoreDTO input) {
            return input != null && input.totalApplicationRisk.totalRisk > 0;
          }
        }));

    return filteredApplicationRiskScores;
  }
}
