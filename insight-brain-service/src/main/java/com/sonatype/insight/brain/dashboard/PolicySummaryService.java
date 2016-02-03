/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

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
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

@Named
public class PolicySummaryService
{
  private static final Logger log = LoggerFactory.getLogger(PolicySummaryService.class);

  static final int POLICY_SUMMARY_WEEKS = 12;

  private static final long ONE_WEEK_IN_MILLISECS = 7L * 24 * 3600 * 1000;

  private final ApplicationService applicationService;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final DashboardUtils dashboardUtils;

  @Inject
  public PolicySummaryService(ApplicationService applicationService,
                              PolicyEvaluationDAO policyEvaluationDAO,
                              PolicyViolationDAO policyViolationDAO,
                              DashboardUtils dashboardUtils)
  {
    this.applicationService = applicationService;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.dashboardUtils = dashboardUtils;
  }

  private static class PolicyViolationHistory
  {
    private static class Data
    {
      private Set<String> stageTypeIds = new LinkedHashSet<>();

      private Date firstOccurrenceTime;
    }

    private Map<PolicyViolation, Data> dataByViolation = new LinkedHashMap<>();

    void addViolationWithStageType(PolicyViolation policyViolation, String stageTypeId) {
      Data data = new Data();
      data.stageTypeIds.add(stageTypeId);
      data.firstOccurrenceTime = policyViolation.getTime();
      dataByViolation.put(policyViolation, data);
    }

    void addStageTypeToViolation(PolicyViolation policyViolation, String stageTypeId) {
      dataByViolation.get(policyViolation).stageTypeIds.add(stageTypeId);
    }

    /**
     * Returns true if this operation clears all stage types for the given policy violation, which effectively means
     * that the policy violation was fixed for all stages.
     */
    boolean removeStageTypeFromViolation(PolicyViolation policyViolation, String stageTypeId) {
      Data data = dataByViolation.get(policyViolation);
      data.stageTypeIds.remove(stageTypeId);
      if (data.stageTypeIds.isEmpty()) {
        dataByViolation.remove(policyViolation);
        return true;
      }
      return false;
    }

    void replacePolicyViolation(PolicyViolation oldViolation, PolicyViolation newViolation) {
      Data data = dataByViolation.remove(oldViolation);
      dataByViolation.put(newViolation, data);
    }

    Collection<PolicyViolation> getPolicyViolations() {
      return Collections.unmodifiableCollection(dataByViolation.keySet());
    }

    Date getPolicyViolationFirstOccurrenceTime(PolicyViolation policyViolation) {
      return dataByViolation.get(policyViolation).firstOccurrenceTime;
    }
  }

  private void addWeekViolation(int weekIndex, List<Integer> weeklyDeltas) {
    addWeekViolation(weekIndex, weeklyDeltas, 1);
  }

  private void addWeekViolation(int weekIndex, List<Integer> weeklyDeltas, int delta) {
    if (weekIndex >= 0) {
      weeklyDeltas.set(weekIndex, weeklyDeltas.get(weekIndex) + delta);
    }
  }

  private int getPolicySummaryWeekFromTime(long now, long time) {
    return POLICY_SUMMARY_WEEKS - (int) ((now - time) / ONE_WEEK_IN_MILLISECS) - 1;
  }

  /**
   * Gets the policy summary matching the specified filter criteria. Empty or null filter criteria generally means
   * "all available" violations for that aspect.
   */
  public PolicySummaryDTO getPolicySummary(Set<String> applicationIds,
                                           Set<String> stageIds,
                                           Set<String> tagIds,
                                           PolicyThreatCategoryFilter policyThreatCategoryFilter,
                                           PolicyThreatLevelFilter policyThreatLevelFilter)
  {
    dashboardUtils.validateDashboardLicensed();

    long start = System.currentTimeMillis();

    List<Application> applications = applicationService.getApplicationsByIdsAndTagIds(applicationIds, tagIds);
    log.debug("getPolicySummary: Found {} applications filtered by appIds={} and tagIds={} in {} ms.",
        applications.size(), !isEmpty(applicationIds), !isEmpty(tagIds), System.currentTimeMillis() - start);

    Set<StageType> stageTypes = dashboardUtils.getStageTypes(stageIds);
    Set<String> stageTypeIds = dashboardUtils.getStageTypeIds(stageTypes);
    Predicate<PolicyViolation> filter = dashboardUtils.buildViolationFilter(policyThreatCategoryFilter,
        policyThreatLevelFilter);
    Long now = System.currentTimeMillis();

    PolicySummaryDTO result = new PolicySummaryDTO();
    result.timestamp = now;
    for (int iWeek = 0; iWeek < POLICY_SUMMARY_WEEKS; iWeek++) {
      result.weeklyDeltaNew.add(0);
      result.weeklyDeltaWaived.add(0);
      result.weeklyDeltaFixed.add(0);
    }

    DescriptiveStatistics ageWaivedStatistics = new DescriptiveStatistics();
    DescriptiveStatistics ageFixedStatistics = new DescriptiveStatistics();
    DescriptiveStatistics ageUnresolvedStatistics = new DescriptiveStatistics();
    int policyEvaluationCount = 0;
    int policyViolationCount = 0;
    for (Application app : applications) {
      PolicyViolationHistory policyViolationHistory = new PolicyViolationHistory();

      List<PolicyEvaluation> policyEvaluations = policyEvaluationDAO.getByApplicationIdAndStageIds(app.getId(),
          stageTypeIds);
      policyEvaluationCount += policyEvaluations.size();
      for (PolicyEvaluation policyEvaluation : policyEvaluations) {
        if (policyEvaluation.getTime().getTime() > now) {
          // This policy evaluation is after we started calculating the policy summary. In order to be consistent,
          // ignore it.
          continue;
        }

        int weekIndex = getPolicySummaryWeekFromTime(now, policyEvaluation.getTime().getTime());

        List<PolicyViolation> policyViolations = policyViolationDAO.getByEvaluationId(policyEvaluation.getId());
        policyViolationCount += policyViolations.size();
        policyViolations = dashboardUtils.filter(policyViolations, filter);

        PolicyViolationDiff diff = PolicyViolationDigester.digestPolicyViolations(
            policyViolationHistory.getPolicyViolations(), policyViolations);
        for (PolicyViolation policyViolation : diff.getAppeared()) {
          policyViolationHistory.addViolationWithStageType(policyViolation, policyEvaluation.getStageTypeId());
          result.totalNew++;
          addWeekViolation(weekIndex, result.weeklyDeltaNew);
          if (policyViolation.isWaived()) {
            result.totalWaived++;
            addWeekViolation(weekIndex, result.weeklyDeltaWaived);
          }
        }
        for (Entry<PolicyViolation, PolicyViolation> samePolicyViolationEntry : diff.getSame().entrySet()) {
          final PolicyViolation newViolation = samePolicyViolationEntry.getValue();
          final PolicyViolation oldViolation = samePolicyViolationEntry.getKey();

          policyViolationHistory.addStageTypeToViolation(oldViolation, policyEvaluation.getStageTypeId());

          if (newViolation.isWaived() && !oldViolation.isWaived()) {
            result.totalWaived++;
            addWeekViolation(weekIndex, result.weeklyDeltaWaived);
            policyViolationHistory.replacePolicyViolation(oldViolation, newViolation);
          }
          else if (!newViolation.isWaived() && oldViolation.isWaived()) {
            result.totalWaived--;
            addWeekViolation(weekIndex, result.weeklyDeltaWaived, -1);
            policyViolationHistory.replacePolicyViolation(oldViolation, newViolation);
            long policyViolationAgeSinceWaived = newViolation.getTime().getTime() - oldViolation.getTime().getTime();
            ageWaivedStatistics.addValue(policyViolationAgeSinceWaived);
          }
        }
        for (PolicyViolation policyViolation : diff.getCleared()) {
          Date policyViolationFirstOccurrenceTime = policyViolationHistory
              .getPolicyViolationFirstOccurrenceTime(policyViolation);
          if (policyViolationHistory.removeStageTypeFromViolation(policyViolation, policyEvaluation.getStageTypeId())) {
            result.totalFixed++;
            addWeekViolation(weekIndex, result.weeklyDeltaFixed);
            if (policyViolation.isWaived()) {
              result.totalWaived--;
              addWeekViolation(weekIndex, result.weeklyDeltaWaived, -1);
              long policyViolationAgeSinceWaived = policyEvaluation.getTime().getTime()
                  - policyViolation.getTime().getTime();
              ageWaivedStatistics.addValue(policyViolationAgeSinceWaived);
            }
            long policyViolationAgeWhenFixed = policyEvaluation.getTime().getTime()
                - policyViolationFirstOccurrenceTime.getTime();
            ageFixedStatistics.addValue(policyViolationAgeWhenFixed);
          }
        }
      }

      // Calculate age statistics for waived and unresolved policy violations
      for (PolicyViolation policyViolation : policyViolationHistory.getPolicyViolations()) {
        if (policyViolation.isWaived()) {
          long policyViolationAgeSinceWaived = now - policyViolation.getTime().getTime();
          ageWaivedStatistics.addValue(policyViolationAgeSinceWaived);
        }
        else {
          Date policyViolationFirstOccurrenceTime = policyViolationHistory
              .getPolicyViolationFirstOccurrenceTime(policyViolation);
          long policyViolationAge = now - policyViolationFirstOccurrenceTime.getTime();
          ageUnresolvedStatistics.addValue(policyViolationAge);
        }
      }
    }

    log.debug("getPolicySummary: Processed {} policy evaluations and {} policy violations.", policyEvaluationCount,
        policyViolationCount);

    result.currentUnresolved = result.totalNew - result.totalWaived - result.totalFixed;
    for (int iWeek = 0; iWeek < POLICY_SUMMARY_WEEKS; iWeek++) {
      result.weeklyDeltaUnresolved.add(result.weeklyDeltaNew.get(iWeek) - result.weeklyDeltaWaived.get(iWeek)
          - result.weeklyDeltaFixed.get(iWeek));
    }

    if (ageWaivedStatistics.getN() > 0) {
      result.ageAverageWaived = (long) ageWaivedStatistics.getMean();
      result.agePercentile90Waived = (long) ageWaivedStatistics.getPercentile(90);
    }
    if (ageFixedStatistics.getN() > 0) {
      result.ageAverageFixed = (long) ageFixedStatistics.getMean();
      result.agePercentile90Fixed = (long) ageFixedStatistics.getPercentile(90);
    }
    if (ageUnresolvedStatistics.getN() > 0) {
      result.ageAverageUnresolved = (long) ageUnresolvedStatistics.getMean();
      result.agePercentile90Unresolved = (long) ageUnresolvedStatistics.getPercentile(90);
    }

    log.debug("getPolicySummary finished in {} ms", System.currentTimeMillis() - start);

    return result;
  }
}
