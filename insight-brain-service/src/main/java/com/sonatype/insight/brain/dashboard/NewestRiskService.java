/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDigester;

import com.google.common.base.Predicate;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

@Named
public class NewestRiskService
{
  private static final Logger log = LoggerFactory.getLogger(NewestRiskService.class);

  static final int NEWEST_RISK_TIME_RANGE_IN_DAYS = 30;

  private final ApplicationService applicationService;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final DashboardUtils dashboardUtils;

  @Inject
  public NewestRiskService(ApplicationService applicationService,
                           PolicyEvaluationDAO policyEvaluationDAO,
                           PolicyViolationDAO policyViolationDAO,
                           DashboardUtils dashboardUtils)
  {
    this.applicationService = applicationService;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.dashboardUtils = dashboardUtils;
  }

  private Map<String, PolicyEvaluation> getLastPolicyEvaluationsByAppIdAndStageTypeId(List<Application> applications,
                                                                                      Set<StageType> stageTypes)
  {
    Set<String> appIds = dashboardUtils.getApplicationIds(applications);
    Set<String> stageTypeIds = dashboardUtils.getStageTypeIds(stageTypes);

    Map<String, PolicyEvaluation> lastPolicyEvaluationsMap = new HashMap<>();
    List<PolicyEvaluation> lastPolicyEvaluations = policyEvaluationDAO.getLastByApplicationIdsAndStageIds(appIds,
        stageTypeIds);
    for (PolicyEvaluation lastPolicyEvaluation : lastPolicyEvaluations) {
      lastPolicyEvaluationsMap.put(lastPolicyEvaluation.getApplicationId() + lastPolicyEvaluation.getStageTypeId(),
          lastPolicyEvaluation);
    }
    return lastPolicyEvaluationsMap;
  }

  /**
   * Gets the "newest" risk matching the specified filter criteria. Empty or null filter criteria generally means
   * "all available" violations for that aspect.
   */
  public List<NewestRiskDTO> getNewestRisks(Set<String> applicationIds,
                                            Set<String> stageIds,
                                            Set<String> tagIds,
                                            PolicyThreatCategoryFilter policyThreatCategoryFilter,
                                            PolicyThreatLevelFilter policyThreatLevelFilter,
                                            int maxResults)
  {
    dashboardUtils.validateDashboardLicensed();

    long start = System.currentTimeMillis();

    List<Application> applications = applicationService.getApplicationsByIdsAndTagIds(applicationIds, tagIds);
    log.debug("getNewestRisks: Found {} applications filtered by appIds={} and tagIds={} in {} ms.",
        applications.size(), !isEmpty(applicationIds), !isEmpty(tagIds), System.currentTimeMillis() - start);

    Set<StageType> stageTypes = dashboardUtils.getStageTypes(stageIds);
    Predicate<PolicyViolation> filter = dashboardUtils.buildViolationFilter(policyThreatCategoryFilter,
        policyThreatLevelFilter);

    Map<String, PolicyEvaluation> lastPolicyEvaluationsByAppIdAndStageTypeId = getLastPolicyEvaluationsByAppIdAndStageTypeId(
        applications, stageTypes);

    List<NewestRiskDTO> result = new ArrayList<>();
    int policyEvaluationCount = 0;
    int policyViolationCount = 0;

    for (Application app : applications) {
      List<PolicyViolation> allUniqueAppPolicyViolations = new ArrayList<>();
      Map<PolicyViolation, NewestRiskDTO> newestRiskDTOsByPolicyViolation = new HashMap<>();

      for (StageType stageType : stageTypes) {
        PolicyEvaluation policyEvaluation = lastPolicyEvaluationsByAppIdAndStageTypeId.get(app.getId()
            + stageType.getId());
        if (policyEvaluation == null) {
          continue;
        }
        policyEvaluationCount++;

        List<PolicyViolation> policyViolations = policyViolationDAO.getActiveByEvaluationId(policyEvaluation.getId());
        policyViolations = dashboardUtils.filter(policyViolations, filter);
        if (policyViolations.isEmpty()) {
          continue;
        }
        policyViolationCount += policyViolations.size();

        Map<PolicyViolation, PolicyViolation> firstOccurrencePolicyViolationsByLastPolicyViolations = getFirstOccurrencePolicyViolationsForLastPolicyViolations(
            app.getId(), stageType.getId(), policyViolations);

        PolicyViolationDiff diff = PolicyViolationDigester.digestPolicyViolations(allUniqueAppPolicyViolations,
            policyViolations);
        for (PolicyViolation policyViolation : diff.getAppeared()) {
          PolicyViolation firstOccurrencePolicyViolation = firstOccurrencePolicyViolationsByLastPolicyViolations
              .get(policyViolation);
          NewestRiskDTO newestRiskDTO = createNewestRiskDTO(app, stageType, policyViolation,
              firstOccurrencePolicyViolation.getTime().getTime(), policyEvaluation.getScanId());
          newestRiskDTOsByPolicyViolation.put(policyViolation, newestRiskDTO);
          result.add(newestRiskDTO);
        }
        for (Entry<PolicyViolation, PolicyViolation> samePolicyViolationEntry : diff.getSame().entrySet()) {
          NewestRiskDTO newestRiskDTO = newestRiskDTOsByPolicyViolation.get(samePolicyViolationEntry.getKey());
          PolicyViolation policyViolation = samePolicyViolationEntry.getValue();
          PolicyViolation firstOccurrencePolicyViolation = firstOccurrencePolicyViolationsByLastPolicyViolations
              .get(policyViolation);
          addToNewestRiskDTO(newestRiskDTO, stageType, policyViolation, firstOccurrencePolicyViolation.getTime()
              .getTime(), policyEvaluation.getScanId());
        }

        allUniqueAppPolicyViolations.addAll(diff.getAppeared());
      }
      for (NewestRiskDTO newestRiskDTO : newestRiskDTOsByPolicyViolation.values()) {
        padStageDetails(newestRiskDTO);
      }
    }

    result = filter(result);
    Collections.sort(result, NewestRiskDTOComparator.INSTANCE);
    result.subList(Math.min(result.size(), maxResults), result.size()).clear();
    log.debug("getNewestRisks: Processed {} policy evaluations and {} policy violations.", policyEvaluationCount,
        policyViolationCount);

    log.debug("getNewestRisks finished in {} ms", System.currentTimeMillis() - start);

    return result;
  }

  private Map<PolicyViolation, PolicyViolation> getFirstOccurrencePolicyViolationsForLastPolicyViolations(String appId,
                                                                                                          String stageTypeId,
                                                                                                          List<PolicyViolation> lastPolicyViolations)
  {
    Map<PolicyViolation, PolicyViolation> result = new LinkedHashMap<>();

    List<PolicyViolation> firstOccurrences = policyViolationDAO.getFirstOccurrenceByApplicationIdAndStageTypeId(appId,
        stageTypeId);
    PolicyViolationDiff diff = PolicyViolationDigester.digestPolicyViolations(lastPolicyViolations, firstOccurrences);
    for (Entry<PolicyViolation, PolicyViolation> samePolicyViolationEntry : diff.getSame().entrySet()) {
      result.put(samePolicyViolationEntry.getKey(), samePolicyViolationEntry.getValue());
    }
    for (PolicyViolation policyViolation : diff.getCleared()) {
      // PolicyViolation without a corresponding FirstOccurrencePolicyViolation. This can happen only if data was
      // missing during migration?
      result.put(policyViolation, policyViolation);
    }

    return result;
  }

  private NewestRiskDTO createNewestRiskDTO(Application app,
                                            StageType stageType,
                                            PolicyViolation policyViolation,
                                            long time,
                                            String scanId)
  {
    NewestRiskDTO newestRiskDTO = new NewestRiskDTO();
    newestRiskDTO.applicationPublicId = app.getPublicId();
    newestRiskDTO.applicationName = app.getName();
    newestRiskDTO.threatLevel = policyViolation.getThreatLevel();
    newestRiskDTO.time = time;
    newestRiskDTO.policyId = policyViolation.getPolicyId();
    newestRiskDTO.policyName = policyViolation.getPolicyName();
    newestRiskDTO.hash = policyViolation.getHash();
    newestRiskDTO.displayName = ComponentDisplayNameUtil.fromPolicyViolation(policyViolation);
    newestRiskDTO.pathnames = policyViolation.getPathnames();

    StageDetailDTO stageDetailDTO = new StageDetailDTO();
    stageDetailDTO.stageTypeId = stageType.getId();
    stageDetailDTO.actionTypeId = policyViolation.getActionTypeId();
    stageDetailDTO.time = time;
    stageDetailDTO.scanId = scanId;
    newestRiskDTO.stageDetails.add(stageDetailDTO);

    return newestRiskDTO;
  }

  private void addToNewestRiskDTO(NewestRiskDTO newestRiskDTO,
                                  StageType stageType,
                                  PolicyViolation policyViolation,
                                  long time,
                                  String scanId)
  {
    if (newestRiskDTO.time < policyViolation.getTime().getTime()) {
      newestRiskDTO.displayName = ComponentDisplayNameUtil.fromPolicyViolation(policyViolation);
      newestRiskDTO.pathnames = policyViolation.getPathnames();
    }

    if (newestRiskDTO.time < time) {
      newestRiskDTO.time = time;
    }

    StageDetailDTO stageDetailDTO = new StageDetailDTO();
    stageDetailDTO.stageTypeId = stageType.getId();
    stageDetailDTO.actionTypeId = policyViolation.getActionTypeId();
    stageDetailDTO.time = time;
    stageDetailDTO.scanId = scanId;
    newestRiskDTO.stageDetails.add(stageDetailDTO);
  }

  /**
   * Filters the given newestRiskDTOs to those that are newer than NEWEST_RISK_TIME_RANGE_IN_DAYS
   */
  private static List<NewestRiskDTO> filter(List<NewestRiskDTO> newestRiskDTOs) {
    List<NewestRiskDTO> filtered = new ArrayList<>();
    long filterFromTime = new DateTime().minusDays(NEWEST_RISK_TIME_RANGE_IN_DAYS).getMillis();
    for (NewestRiskDTO newestRiskDTO : newestRiskDTOs) {
      if (newestRiskDTO.time > filterFromTime) {
        filtered.add(newestRiskDTO);
      }
    }
    return filtered;
  }

  /**
   * Add an 'empty' record for each missing stage we need to show in the UI.
   */
  private void padStageDetails(final NewestRiskDTO newestRiskDTO) {
    Set<String> seenStages = new HashSet<>();
    for (StageDetailDTO stageDetail : newestRiskDTO.stageDetails) {
      seenStages.add(stageDetail.stageTypeId);
    }
    for (StageType stageType : dashboardUtils.getStageTypes(null)) {
      if (!seenStages.contains(stageType.getId())) {
        StageDetailDTO emptyStageDetails = new StageDetailDTO();
        emptyStageDetails.stageTypeId = stageType.getId();
        newestRiskDTO.stageDetails.add(emptyStageDetails);
      }
    }
  }
}
