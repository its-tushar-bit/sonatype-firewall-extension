/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.aggregation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.aggregation.AverageDiscoveredPolicyViolationsDTO.AverageDiscoveredThreatCategoryPolicyViolationsDTO;
import com.sonatype.insight.brain.dashboard.DashboardUtils;
import com.sonatype.insight.brain.dataaccess.aggregation.PolicyViolationAggregationDAO;
import com.sonatype.insight.brain.dataaccess.aggregation.PolicyViolationAggregationDAO.ApplicationCountsByThreat;
import com.sonatype.insight.brain.dataaccess.aggregation.PolicyViolationAggregationDAO.AverageMonth;
import com.sonatype.insight.brain.dataaccess.aggregation.PolicyViolationResolutionStateDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.aggregation.PolicyViolationAggregation;
import com.sonatype.insight.brain.model.aggregation.PolicyViolationResolutionState;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolationComparable;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationComparator;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDigester;
import com.sonatype.insight.brain.utils.ThreatLevel;

import com.google.common.cache.CacheBuilder;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.LICENSE;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.OTHER;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.QUALITY;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.SECURITY;
import static com.sonatype.insight.brain.utils.ThreatLevel.CRITICAL;
import static com.sonatype.insight.brain.utils.ThreatLevel.LOW;
import static com.sonatype.insight.brain.utils.ThreatLevel.MODERATE;
import static com.sonatype.insight.brain.utils.ThreatLevel.SEVERE;

/**
 * @since 1.31
 */
@Named
@Singleton
public class PolicyViolationAggregationService
{
  private static final Logger log = LoggerFactory.getLogger(PolicyViolationAggregationService.class);

  private final ApplicationService applicationService;

  private final StageTypeService stageTypeService;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final PolicyViolationAggregationDAO violationAggregationDAO;

  private final PolicyViolationResolutionStateDAO policyViolationResolutionStateDAO;

  private final DashboardUtils dashboardUtils;

  private final ConcurrentMap<String, Lock> applicationIdLocks = CacheBuilder.newBuilder().weakValues()
      .<String, Lock> build().asMap();

  @Inject
  public PolicyViolationAggregationService(ApplicationService applicationService,
                                           StageTypeService stageTypeService,
                                           PolicyEvaluationDAO policyEvaluationDAO,
                                           PolicyViolationDAO policyViolationDAO,
                                           PolicyViolationAggregationDAO violationAggregationDAO,
                                           PolicyViolationResolutionStateDAO policyViolationResolutionStateDAO,
                                           DashboardUtils dashboardUtils)
  {
    this.applicationService = applicationService;
    this.stageTypeService = stageTypeService;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.violationAggregationDAO = violationAggregationDAO;
    this.policyViolationResolutionStateDAO = policyViolationResolutionStateDAO;
    this.dashboardUtils = dashboardUtils;
  }

  /**
   * @return the list of MttrDTOs which will be empty if the MTTR functionality is disabled
   * (ie, due to being in PoC mode)
   */
  public List<MttrDTO> getMttrs(Set<String> organizationIds, Set<String> applicationIds) {
    if (isInPoCMode()) {
      return Arrays.asList();
    }

    Set<String> applicationIdsToQuery = getApplicationIdsToQuery(organizationIds, applicationIds);
    // make sure the aggregations data is up to date before we query it
    generatePolicyViolationAggregations(applicationIdsToQuery);

    List<PolicyViolationAggregationDAO.MttrMonth> queryResults = violationAggregationDAO
        .getMttrMonthlyAverages(applicationIdsToQuery);

    List<MttrDTO> retval = new ArrayList<>(queryResults.size());
    for (PolicyViolationAggregationDAO.MttrMonth mttrMonth : queryResults) {
      MttrDTO dto = new MttrDTO();
      int totalResolved = mttrMonth.resolvedCountLowThreat + mttrMonth.resolvedCountModerateThreat
          + mttrMonth.resolvedCountSevereThreat + mttrMonth.resolvedCountCriticalThreat;
      double mttrLowThreat = mttrMonth.mttrLowThreat != null ? mttrMonth.mttrLowThreat.doubleValue() : 0;
      double mttrModerateThreat = mttrMonth.mttrModerateThreat != null ? mttrMonth.mttrModerateThreat.doubleValue() : 0;
      double mttrSevereThreat = mttrMonth.mttrSevereThreat != null ? mttrMonth.mttrSevereThreat.doubleValue() : 0;
      double mttrCriticalThreat = mttrMonth.mttrCriticalThreat != null ? mttrMonth.mttrCriticalThreat.doubleValue() : 0;

      // NOTE: DTO values are in seconds while MttrMonth values are in milliseconds hence the division by 1000
      if (totalResolved != 0) {
        // combine MTTRs for all threat levels using a weighted average
        dto.mttrInSeconds = (int) ((mttrLowThreat * mttrMonth.resolvedCountLowThreat + //
            mttrModerateThreat * mttrMonth.resolvedCountModerateThreat + //
            mttrSevereThreat * mttrMonth.resolvedCountSevereThreat + //
            mttrCriticalThreat * mttrMonth.resolvedCountCriticalThreat) / totalResolved / 1000);
      }
      // else leave it null

      dto.timePeriodStart = mttrMonth.monthStart;
      dto.criticalMttrInSeconds = mttrMonth.mttrCriticalThreat != null
          ? (int) (mttrMonth.mttrCriticalThreat.doubleValue() / 1000) : null;

      retval.add(dto);
    }

    return retval;
  }

  SuccessMetricsAveragesDTO getAverages(Set<String> organizationIds, Set<String> applicationIds) {
    Set<String> applicationIdsToQuery = getApplicationIdsToQuery(organizationIds, applicationIds);
    // make sure the aggregations data is up to date before we query it
    generatePolicyViolationAggregations(applicationIdsToQuery);

    List<AverageMonth> queryResults = violationAggregationDAO.getMonthlyAverages(applicationIdsToQuery);

    List<AverageDiscoveredPolicyViolationsDTO> averageDiscoveredPolicyViolations = new ArrayList<>(queryResults.size());
    for (AverageMonth averageMonth : queryResults) {
      AverageDiscoveredPolicyViolationsDTO dto = new AverageDiscoveredPolicyViolationsDTO();
      dto.timePeriodStart = averageMonth.timePeriodStart;
      dto.security = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(averageMonth.security.averageDiscoveredLowThreat,
          averageMonth.security.averageDiscoveredModerateThreat, averageMonth.security.averageDiscoveredSevereThreat,
          averageMonth.security.averageDiscoveredCriticalThreat);
      dto.license = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(averageMonth.license.averageDiscoveredLowThreat,
          averageMonth.license.averageDiscoveredModerateThreat, averageMonth.license.averageDiscoveredSevereThreat,
          averageMonth.license.averageDiscoveredCriticalThreat);
      dto.quality = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(averageMonth.quality.averageDiscoveredLowThreat,
          averageMonth.quality.averageDiscoveredModerateThreat, averageMonth.quality.averageDiscoveredSevereThreat,
          averageMonth.quality.averageDiscoveredCriticalThreat);
      dto.other = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(averageMonth.other.averageDiscoveredLowThreat,
          averageMonth.other.averageDiscoveredModerateThreat, averageMonth.other.averageDiscoveredSevereThreat,
          averageMonth.other.averageDiscoveredCriticalThreat);
      dto.evaluationCount = averageMonth.evaluationCount;
      averageDiscoveredPolicyViolations.add(dto);
    }

    int activeApplications = violationAggregationDAO.getActiveApplicationCount(applicationIdsToQuery);

    return new SuccessMetricsAveragesDTO(activeApplications, averageDiscoveredPolicyViolations);
  }

  ApplicationCountsDTO getApplicationCounts(Set<String> organizationIds, Set<String> applicationIds) {
    Set<String> applicationIdsToQuery = getApplicationIdsToQuery(organizationIds, applicationIds);
    generatePolicyViolationAggregations(applicationIdsToQuery);

    ApplicationCountsByThreat applicationCounts = violationAggregationDAO
        .getApplicationCountsByThreatByApplicationIds(applicationIdsToQuery);

    ApplicationCountsDTO retval = new ApplicationCountsDTO();
    retval.totalApplications = applicationIdsToQuery.size();
    retval.activeApplications = violationAggregationDAO.getActiveApplicationCount(applicationIdsToQuery);

    retval.total = new ApplicationCountsDTO.ThreatCategoryApplicationCount( //
        applicationCounts.countAnyThreat, //
        applicationCounts.countAnyCriticalThreat);
    retval.security = new ApplicationCountsDTO.ThreatCategoryApplicationCount( //
        applicationCounts.countSecurityThreat, //
        applicationCounts.countSecurityCriticalThreat);
    retval.license = new ApplicationCountsDTO.ThreatCategoryApplicationCount( //
        applicationCounts.countLicenseThreat, //
        applicationCounts.countLicenseCriticalThreat);
    retval.quality = new ApplicationCountsDTO.ThreatCategoryApplicationCount( //
        applicationCounts.countQualityThreat, //
        applicationCounts.countQualityCriticalThreat);
    retval.other = new ApplicationCountsDTO.ThreatCategoryApplicationCount( //
        applicationCounts.countOtherThreat, //
        applicationCounts.countOtherCriticalThreat);

    return retval;
  }

  private boolean isInPoCMode() {
    Set<String> applicableStageTypes = new HashSet<>();
    for (StageType stageType : StageTypes.getAll()) {
      if (!StageTypes.isIgnoredForPolicyViolationAggregation(stageType.getId())) {
        applicableStageTypes.add(stageType.getId());
      }
    }

    PolicyEvaluation oldestEvaluation = policyEvaluationDAO.getOldest(applicableStageTypes);
    if (oldestEvaluation == null) {
      return true;
    }
    LocalDate oldestEvaluationDate = new LocalDate(oldestEvaluation.getTime());
    LocalDate startOfPreviousMonth = new LocalDate().withDayOfMonth(1).minusMonths(1);
    return oldestEvaluationDate.compareTo(startOfPreviousMonth) >= 0;
  }

  private Set<String> getApplicationIdsToQuery(Set<String> organizationIds, Set<String> applicationIds) {
    Collection<Application> applicationsToQuery = applicationService
        .getApplicationsByIdsAndOrganizationIdsAndTagIds(organizationIds, applicationIds, null);

    Set<String> applicationIdsToQuery = new HashSet<>();
    for (Application app : applicationsToQuery) {
      applicationIdsToQuery.add(app.getId());
    }
    return applicationIdsToQuery;
  }

  /**
   * Update PolicyViolationAggregation rows for each of the specified applications. This determines the most recent
   * time period that was already aggregated for each application and then creates all necessary aggregations
   * for the time periods since then.
   */
  private void generatePolicyViolationAggregations(Set<String> applicationIds) {
    log.debug("Starting update of Policy Violation Aggregations for {} applications", applicationIds.size());

    long start = System.currentTimeMillis();

    // current date, in the local timezone
    LocalDate currentDate = new LocalDate();

    List<StageType> stageTypes = new ArrayList<>();
    for (StageType stageType : stageTypeService.getLicensedStageTypes()) {
      if (!StageTypes.isIgnoredForPolicyViolationAggregation(stageType.getId())) {
        stageTypes.add(stageType);
      }
    }
    Set<String> stageTypeIds = dashboardUtils.getStageTypeIds(stageTypes);

    for (String applicationId : applicationIds) {
      Lock lock = acquireLockForApplication(applicationId);

      try {
        generatePolicyViolationAggregations(applicationId, stageTypeIds, currentDate);
      }
      finally {
        lock.unlock();
      }
    }

    long finish = System.currentTimeMillis();
    log.debug("Finished update of Policy Violation Aggregations in {} ms", finish - start);
  }

  private Lock acquireLockForApplication(String applicationId) {
    Lock lock = applicationIdLocks.get(applicationId);
    if (lock == null) {
      final Lock newLock = new ReentrantLock();
      lock = applicationIdLocks.putIfAbsent(applicationId, newLock);
      if (lock == null) {
        lock = newLock;
      }
    }

    lock.lock();

    return lock;
  }

  /**
   * A container for DescriptiveStatistics objects for MTTRs at each threat level category
   */
  private static class MttrStats
  {
    final DescriptiveStatistics mttrLowThreatStats = new DescriptiveStatistics();
    final DescriptiveStatistics mttrModerateThreatStats = new DescriptiveStatistics();
    final DescriptiveStatistics mttrSevereThreatStats = new DescriptiveStatistics();
    final DescriptiveStatistics mttrCriticalThreatStats = new DescriptiveStatistics();

    void addViolation(PolicyViolationComparable violation,
                      Date violationFirstOccurrenceTimestamp,
                      Date violationResolutionTimestamp)
    {
      long timeToResolve = violationResolutionTimestamp.getTime() - violationFirstOccurrenceTimestamp.getTime();
      int threatLevel = violation.getThreatLevel();
      DescriptiveStatistics statsToUpdate;

      // NOTE: the thresholds between the different threat level categories are codified in at least four different
      // places throughout the app and ought to be centralized
      if (threatLevel >= 8) {
        statsToUpdate = mttrCriticalThreatStats;
      }
      else if (threatLevel >= 4) {
        statsToUpdate = mttrSevereThreatStats;
      }
      else if (threatLevel >= 2) {
        statsToUpdate = mttrModerateThreatStats;
      }
      else {
        statsToUpdate = mttrLowThreatStats;
      }

      statsToUpdate.addValue(timeToResolve);
    }
  }

  private static class DiscoveredStats
  {
    final Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> threatCategoryToThreatLevelToCounts = new EnumMap<>(
        PolicyThreatCategory.class);
    int evaluationCount = 0;

    public DiscoveredStats() {
      for (PolicyThreatCategory threatCategory : PolicyThreatCategory.values()) {
        EnumMap<ThreatLevel, Integer> threatLevelToCount = new EnumMap<>(ThreatLevel.class);
        threatCategoryToThreatLevelToCounts.put(threatCategory, threatLevelToCount);
        for (ThreatLevel threatLevel : ThreatLevel.values()) {
          threatLevelToCount.put(threatLevel, 0);
        }
      }
    }

    void addViolation(PolicyViolationResolutionState violation) {
      ThreatLevel threatLevel = ThreatLevel.from(violation.getThreatLevel());
      PolicyThreatCategory threatCategory = violation.getThreatCategory();
      threatCategoryToThreatLevelToCounts.get(threatCategory)
          .put(threatLevel, getCount(threatCategory, threatLevel) + 1);
    }

    int getCount(PolicyThreatCategory threatCategory, ThreatLevel threatLevel) {
      return threatCategoryToThreatLevelToCounts.get(threatCategory).get(threatLevel);
    }
    
    public void incrementEvaluationCount() {
      evaluationCount++;
    }

    public int getEvaluationCount() {
      return evaluationCount;
    }
  }

  /**
   * Generate the newer PolicyViolationAggregation rows for the given application
   */
  private void generatePolicyViolationAggregations(String applicationId,
                                                   Set<String> allStageTypeIds,
                                                   LocalDate currentDate)
  {
    int numDeletedPartialMonths = violationAggregationDAO.deletePartialMonthsUpTo(applicationId, currentDate);
    if (numDeletedPartialMonths != 0) {
      policyViolationResolutionStateDAO.deleteByApplicationId(applicationId);
    }
    boolean pocMode = isInPoCMode();

    log.trace("Generating Violation Aggregations for {}", applicationId);

    PolicyViolationAggregation mostRecentPriorAggregation = violationAggregationDAO
        .getMostRecentByApplicationId(applicationId);

    LocalDate startOfCurrentMonth = currentDate.withDayOfMonth(1);

    // start the next new aggregation at the beginning of the month after the last aggregation, or at the beginning
    // of the month of the first evaluation if there aren't any aggregations for this app yet
    PolicyEvaluation oldestEvaluation = policyEvaluationDAO.getOldestByApplicationId(applicationId);
    LocalDate oldestEvaluationDate = oldestEvaluation == null ? null : new LocalDate(oldestEvaluation.getTime());
    LocalDate startOfFirstAggregation =
        oldestEvaluationDate == null ? startOfCurrentMonth : oldestEvaluationDate.withDayOfMonth(1);
    LocalDate startOfNewAggregation = mostRecentPriorAggregation == null ? startOfFirstAggregation : new LocalDate(
        mostRecentPriorAggregation.getTimePeriodStart()).plusMonths(1);
    LocalDate startOfNextAggregation = startOfNewAggregation.plusMonths(1);

    if (pocMode) {
      if (mostRecentPriorAggregation != null &&
          currentDate.toDate().equals(mostRecentPriorAggregation.getTimePeriodEnd())) {
        // aggregations are already done up to today
        return;
      }
      else if (oldestEvaluation == null || currentDate.toDate().equals(oldestEvaluationDate)) {
        // no evaluations or oldest evaluation is within current day
        return;
      }
    }
    else {
      if (startOfNewAggregation.compareTo(startOfCurrentMonth) >= 0) {
        // aggregations are already done up to this month
        return;
      }
    }

    List<PolicyEvaluation> policyEvaluations = policyEvaluationDAO.getSinceDateByApplicationIdAndStageIds(
        localDateToTimestamp(startOfNewAggregation), applicationId, allStageTypeIds);

    MttrStats mttrStats = new MttrStats();
    DiscoveredStats discoveredStats = new DiscoveredStats();

    if (!policyEvaluations.isEmpty()) {

      SortedSet<PolicyViolationResolutionState> resolutionStates = getPolicyViolationResolutionStates(applicationId);

      for (PolicyEvaluation evaluation : policyEvaluations) {
        LocalDate evaluationDate = new LocalDate(evaluation.getTime());

        if (pocMode) {
          if (evaluationDate.compareTo(currentDate) >= 0) {
            // evaluation is within current day. Skip it and the rest
            break;
          }
        }
        else {
          if (evaluationDate.compareTo(startOfCurrentMonth) >= 0) {
            // evaluation is within current month. Skip it and the rest
            break;
          }
        }

        while (evaluationDate.compareTo(startOfNextAggregation) >= 0) {
          // evaluation is too recent for the current evaluation record, start a new one
          saveViolationAggregation(applicationId, startOfNewAggregation, pocMode ? currentDate : null, mttrStats,
              discoveredStats);
          startOfNewAggregation = startOfNextAggregation;
          startOfNextAggregation = startOfNewAggregation.plusMonths(1);

          mttrStats = new MttrStats();
          discoveredStats = new DiscoveredStats();
        }

        processPolicyEvaluation(evaluation, mttrStats, discoveredStats, resolutionStates, applicationId);
      }
    }

    // insert the last aggregation from the loop above and any others necessary to bring things up to the
    // start of the current month
    while (startOfNewAggregation.compareTo(pocMode ? currentDate : startOfCurrentMonth) < 0) {
      saveViolationAggregation(applicationId, startOfNewAggregation, pocMode ? currentDate : null, mttrStats,
          discoveredStats);
      startOfNewAggregation = startOfNextAggregation;
      startOfNextAggregation = startOfNewAggregation.plusMonths(1);
      mttrStats = new MttrStats();
      discoveredStats = new DiscoveredStats();
    }
  }

  private void processPolicyEvaluation(PolicyEvaluation evaluation,
                                       MttrStats mttrStats,
                                       DiscoveredStats discoveredStats,
                                       SortedSet<PolicyViolationResolutionState> resolutionStates,
                                       String applicationId)
  {
    Date evaluationTimestamp = evaluation.getTime();
    String stageTypeId = evaluation.getStageTypeId();

    // get active violations only. The aggregations treat waived and non-existant equivalently
    Collection<PolicyViolationResolutionState> resolutionStatesForThisEvaluation = createPolicyViolationResolutionStatesForEvaluation(
        evaluation.getId(), applicationId);
    PolicyViolationDiff<PolicyViolationResolutionState> diff = PolicyViolationDigester
        .digestPolicyViolations(resolutionStates, resolutionStatesForThisEvaluation);

    discoveredStats.incrementEvaluationCount();
    for (PolicyViolationResolutionState resolutionState : diff.getAppeared()) {
      resolutionState.setStageTypeById(stageTypeId);
      boolean addedToResolutionState = resolutionStates.add(resolutionState);

      if (addedToResolutionState) {
        discoveredStats.addViolation(resolutionState);
        policyViolationResolutionStateDAO.insert(resolutionState);
      }
    }

    for (PolicyViolationResolutionState resolutionState : diff.getSame().keySet()) {
      resolutionState.setStageTypeById(stageTypeId);
      policyViolationResolutionStateDAO.update(resolutionState);
    }

    for (PolicyViolationResolutionState resolutionState : diff.getCleared()) {
      resolutionState.setStageTypeById(stageTypeId, false);

      if (resolutionState.isClearedInAllStages()) {
        resolutionStates.remove(resolutionState);
        policyViolationResolutionStateDAO.delete(resolutionState);

        Date policyViolationFirstOccurrenceTimestamp = resolutionState.getFirstOccurrenceTime();
        mttrStats.addViolation(resolutionState, policyViolationFirstOccurrenceTimestamp, evaluationTimestamp);
      }
    }
  }

  private static Date localDateToTimestamp(LocalDate date) {
    if (date == null) {
      return null;
    }
    return date.toDateTimeAtStartOfDay().toDate();
  }

  /**
   * Get the PolicyViolationResolutionState entries for the specified applicationId, as a set based on
   * PolicyViolationComparator
   */
  private SortedSet<PolicyViolationResolutionState> getPolicyViolationResolutionStates(String applicationId) {
    SortedSet<PolicyViolationResolutionState> retval = new TreeSet<>(PolicyViolationComparator.COMPARATOR);
    retval.addAll(policyViolationResolutionStateDAO.getByApplicationId(applicationId));

    return retval;
  }

  /**
   * @return PolicyViolations for the specified PolicyEvaluation, represented as PolicyViolationResolutionState objects
   */
  private Collection<PolicyViolationResolutionState> createPolicyViolationResolutionStatesForEvaluation(String evaluationId,
                                                                                                        String applicationId)
  {
    Collection<PolicyViolation> violations = policyViolationDAO.getActiveByEvaluationId(evaluationId);
    List<PolicyViolationResolutionState> resolutionStates = new ArrayList<>(violations.size());

    for (PolicyViolation violation : violations) {
      resolutionStates.add(new PolicyViolationResolutionState(applicationId, violation));
    }

    return resolutionStates;
  }

  private void saveViolationAggregation(String applicationId,
                                        LocalDate timePeriodStart,
                                        LocalDate timePeriodEnd,
                                        MttrStats mttrStats,
                                        DiscoveredStats discoveredStats)
  {
    PolicyViolationAggregation aggregation = new PolicyViolationAggregation(applicationId, //
        localDateToTimestamp(timePeriodStart), //
        localDateToTimestamp(timePeriodEnd), //
        mttrStats.mttrLowThreatStats, //
        mttrStats.mttrModerateThreatStats, //
        mttrStats.mttrSevereThreatStats, //
        mttrStats.mttrCriticalThreatStats, //
        discoveredStats.getCount(SECURITY, LOW), //
        discoveredStats.getCount(SECURITY, MODERATE), //
        discoveredStats.getCount(SECURITY, SEVERE), //
        discoveredStats.getCount(SECURITY, CRITICAL), //
        discoveredStats.getCount(LICENSE, LOW), //
        discoveredStats.getCount(LICENSE, MODERATE), //
        discoveredStats.getCount(LICENSE, SEVERE), //
        discoveredStats.getCount(LICENSE, CRITICAL), //
        discoveredStats.getCount(QUALITY, LOW), //
        discoveredStats.getCount(QUALITY, MODERATE), //
        discoveredStats.getCount(QUALITY, SEVERE), //
        discoveredStats.getCount(QUALITY, CRITICAL), //
        discoveredStats.getCount(OTHER, LOW), //
        discoveredStats.getCount(OTHER, MODERATE), //
        discoveredStats.getCount(OTHER, SEVERE), //
        discoveredStats.getCount(OTHER, CRITICAL), //
        discoveredStats.getEvaluationCount());

    violationAggregationDAO.insert(aggregation);
  }
}
