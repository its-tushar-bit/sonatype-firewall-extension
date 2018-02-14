/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dashboard.DashboardUtils;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationResolutionStateDAO;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolationComparable;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.successmetrics.PolicyViolationAggregation;
import com.sonatype.insight.brain.model.successmetrics.PolicyViolationResolutionState;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationComparator;
import com.sonatype.insight.brain.utils.ThreatLevel;

import com.google.common.cache.CacheBuilder;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;
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
class PolicyViolationAggregationService
{
  private static final Logger log = LoggerFactory.getLogger(PolicyViolationAggregationService.class);

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final PolicyViolationAggregationDAO violationAggregationDAO;

  private final PolicyViolationResolutionStateDAO policyViolationResolutionStateDAO;

  private final ConcurrentMap<String, Lock> applicationIdLocks = CacheBuilder.newBuilder().weakValues()
      .<String, Lock> build().asMap();

  private final Set<String> stageTypeIds;

  @Inject
  public PolicyViolationAggregationService(StageTypeService stageTypeService,
                                           PolicyEvaluationDAO policyEvaluationDAO,
                                           PolicyViolationDAO policyViolationDAO,
                                           PolicyViolationAggregationDAO violationAggregationDAO,
                                           PolicyViolationResolutionStateDAO policyViolationResolutionStateDAO,
                                           DashboardUtils dashboardUtils)
  {
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.violationAggregationDAO = violationAggregationDAO;
    this.policyViolationResolutionStateDAO = policyViolationResolutionStateDAO;

    List<StageType> stageTypes = new ArrayList<>();

    for (StageType stageType : stageTypeService.getLicensedStageTypes()) {
      if (!StageTypes.isIgnoredForPolicyViolationAggregation(stageType.getId())) {
        stageTypes.add(stageType);
      }
    }
    stageTypeIds = dashboardUtils.getStageTypeIds(stageTypes);
  }

  /**
   * Update PolicyViolationAggregation rows for each of the specified applications. This determines the most recent
   * time period that was already aggregated for each application and then creates all necessary aggregations
   * for the time periods since then.
   */
  void generatePolicyViolationAggregations(Set<String> applicationIds,
                                           DateTime currentDateTime,
                                           boolean includeLatestData)
  {
    log.debug("Starting update of Policy Violation Aggregations for {} applications", applicationIds.size());

    long start = System.currentTimeMillis();

    for (String applicationId : applicationIds) {
      Lock lock = acquireLockForApplication(applicationId);

      try {
        generatePolicyViolationAggregations(applicationId, currentDateTime, includeLatestData);
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
                                                   DateTime currentDateTime,
                                                   boolean includeLatestData)
  {
    log.trace("Generating Violation Aggregations for {}", applicationId);

    PolicyViolationAggregation mostRecentPriorAggregation = violationAggregationDAO
        .getMostRecentByApplicationId(applicationId);

    if (isPartial(mostRecentPriorAggregation)) {
      mostRecentPriorAggregation = updatePartialAggregation(mostRecentPriorAggregation, currentDateTime, applicationId,
          includeLatestData);
    }

    LocalDate currentDate = currentDateTime.toLocalDate();
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

    if (includeLatestData && isPartial(mostRecentPriorAggregation) ||
        !includeLatestData && startOfNewAggregation.compareTo(startOfCurrentMonth) >= 0) {
      // aggregations are already up to date
      return;
    }

    List<EvaluationEvent> events = createSortedEvaluationEvents(applicationId, stageTypeIds,
        localDateToTimestamp(startOfNewAggregation), currentDateTime.toDate());

    MttrStats mttrStats = new MttrStats();
    DiscoveredStats discoveredStats = new DiscoveredStats();

    if (!events.isEmpty()) {
      SortedMap<PolicyViolationComparable, PolicyViolationResolutionState> resolutionStates =
          getPolicyViolationResolutionStates(applicationId);
      for (EvaluationEvent event : events) {
        while (new LocalDate(event.time).compareTo(startOfNextAggregation) >= 0) {
          // event is too recent for the current aggregation record, start a new one
          saveViolationAggregation(applicationId, startOfNewAggregation, null, mttrStats, discoveredStats);
          startOfNewAggregation = startOfNextAggregation;
          startOfNextAggregation = startOfNewAggregation.plusMonths(1);

          mttrStats = new MttrStats();
          discoveredStats = new DiscoveredStats();
        }
        event.process(mttrStats, discoveredStats, resolutionStates);
      }
    }

    // insert the last aggregation from the loop above and any others necessary to bring things up to the
    // start of the current month
    while (includeLatestData ?
        startOfNewAggregation.compareTo(currentDate) <= 0 : startOfNewAggregation.compareTo(startOfCurrentMonth) < 0) {
      DateTime endDateTime = null;
      if (includeLatestData && currentDate.withDayOfMonth(1).equals(startOfNewAggregation)) {
        endDateTime = currentDateTime;
      }
      saveViolationAggregation(applicationId, startOfNewAggregation, endDateTime, mttrStats, discoveredStats);
      startOfNewAggregation = startOfNextAggregation;
      startOfNextAggregation = startOfNewAggregation.plusMonths(1);
      mttrStats = new MttrStats();
      discoveredStats = new DiscoveredStats();
    }
  }

  private boolean isPartial(PolicyViolationAggregation mostRecentPriorAggregation) {
    return mostRecentPriorAggregation != null && mostRecentPriorAggregation.getTimePeriodEnd() != null;
  }

  /**
   * Updates the supplied partial aggregation with latest data. If we have a full month's worth of data then the
   * aggregation's timePeriodEnd will be removed, making it a regular aggregation.
   */
  private PolicyViolationAggregation updatePartialAggregation(PolicyViolationAggregation partialAggregation,
                                                              DateTime currentTime,
                                                              String applicationId,
                                                              boolean includeLatestData)
  {
    LocalDate startOfNextAggregation = new LocalDate(partialAggregation.getTimePeriodStart()).plusMonths(1)
        .withDayOfMonth(1);
    boolean isLastAggregation = currentTime.isBefore(startOfNextAggregation.toDateTimeAtStartOfDay());

    if (!includeLatestData && isLastAggregation) {
      return partialAggregation; // no need to update current month
    }

    Date from = partialAggregation.getTimePeriodEnd();
    Date upTo = new LocalDate(from).plusMonths(1).withDayOfMonth(1).toDateTimeAtStartOfDay().toDate();

    List<EvaluationEvent> events = createSortedEvaluationEvents(applicationId, stageTypeIds, from, upTo);

    MttrStats mttrStats = recreateMttrStats(partialAggregation);
    DiscoveredStats discoveredStats = recreateDiscoveredStats(partialAggregation);

    if (!events.isEmpty()) {
      SortedMap<PolicyViolationComparable, PolicyViolationResolutionState> resolutionStates =
          getPolicyViolationResolutionStates(applicationId);
      for (EvaluationEvent event : events) {
        event.process(mttrStats, discoveredStats, resolutionStates);
      }
    }
    LocalDate timePeriodStart = new LocalDate(partialAggregation.getTimePeriodStart());
    DateTime timePeriodEnd = isLastAggregation ? currentTime : null;
    return saveViolationAggregation(applicationId, timePeriodStart, timePeriodEnd, mttrStats, discoveredStats,
        partialAggregation.getId());
  }

  private DiscoveredStats recreateDiscoveredStats(PolicyViolationAggregation mostRecentPriorAggregation) {
    DiscoveredStats result = new DiscoveredStats();
    for (PolicyThreatCategory category : PolicyThreatCategory.values()) {
      Map<ThreatLevel, Integer> threatLevelToCounts = new HashMap<>();
      for (ThreatLevel threatLevel : ThreatLevel.values()) {
        threatLevelToCounts.put(threatLevel, mostRecentPriorAggregation.getDiscoveredCount(category, threatLevel));
      }
      result.threatCategoryToThreatLevelToCounts.put(category, threatLevelToCounts);
    }
    result.evaluationCount = mostRecentPriorAggregation.getEvaluationCount();
    return result;
  }

  /**
   * Aggregations only persist the MTTR values, not the individual numbers they are a product of.
   * This is a naive attempt to recreate the MTTRStats that only works because we currently persist the arithmetic mean.
   */
  private MttrStats recreateMttrStats(PolicyViolationAggregation mostRecentPriorAggregation) {
    MttrStats result = new MttrStats();
    for (int i = 0; i < mostRecentPriorAggregation.getResolvedCountLowThreat(); i++) {
      result.mttrLowThreatStats.addValue(mostRecentPriorAggregation.getMttrLowThreat());
    }
    for (int i = 0; i < mostRecentPriorAggregation.getResolvedCountModerateThreat(); i++) {
      result.mttrModerateThreatStats.addValue(mostRecentPriorAggregation.getMttrModerateThreat());
    }
    for (int i = 0; i < mostRecentPriorAggregation.getResolvedCountSevereThreat(); i++) {
      result.mttrSevereThreatStats.addValue(mostRecentPriorAggregation.getMttrSevereThreat());
    }
    for (int i = 0; i < mostRecentPriorAggregation.getResolvedCountCriticalThreat(); i++) {
      result.mttrCriticalThreatStats.addValue(mostRecentPriorAggregation.getMttrCriticalThreat());
    }
    return result;
  }

  private List<EvaluationEvent> createSortedEvaluationEvents(String applicationId,
                                                             Set<String> stageTypeIds,
                                                             Date from,
                                                             Date upTo)
  {
    List<PolicyEvaluation> evaluations = policyEvaluationDAO.getBetweenDatesByApplicationIdAndStageIds(from, upTo,
        applicationId, stageTypeIds);
    if (evaluations.isEmpty()) {
      return Collections.emptyList();
    }

    List<PolicyViolation> violations = policyViolationDAO.getActiveByApplicationIdAndStageIdsAndTimeRange(applicationId,
        stageTypeIds, from, upTo);

    List<EvaluationEvent> events = new ArrayList<>(evaluations.size() + violations.size() * 2);
    for (PolicyEvaluation evaluation : evaluations) {
      events.add(new EvaluationPerformedEvent(evaluation));
    }
    for (PolicyViolation violation : violations) {
      if (from.compareTo(violation.getOpenTime()) <= 0) {
        events.add(new ViolationDiscoveredEvent(violation));
      }
      Date resolveTime = violation.getWaiveTime();
      if (resolveTime == null) {
        resolveTime = violation.getFixTime();
      }
      if (resolveTime != null && resolveTime.compareTo(upTo) < 0) {
        events.add(new ViolationResolvedEvent(violation, resolveTime));
      }
    }
    events.sort(null);
    return events;
  }

  private abstract class EvaluationEvent
      implements Comparable<EvaluationEvent>
  {
    final Date time;

    EvaluationEvent(Date time) {
      this.time = time;
    }

    @Override
    public int compareTo(EvaluationEvent other) {
      return time.compareTo(other.time);
    }

    abstract void process(MttrStats mttrStats,
                          DiscoveredStats discoveredStats,
                          SortedMap<PolicyViolationComparable, PolicyViolationResolutionState> resolutionStates);
  }

  private class EvaluationPerformedEvent
      extends EvaluationEvent
  {
    public EvaluationPerformedEvent(PolicyEvaluation evaluation) {
      super(evaluation.getTime());
    }

    @Override
    void process(MttrStats mttrStats,
                 DiscoveredStats discoveredStats,
                 SortedMap<PolicyViolationComparable, PolicyViolationResolutionState> resolutionStates)
    {
      discoveredStats.incrementEvaluationCount();
    }
  }

  private class ViolationDiscoveredEvent
      extends EvaluationEvent
  {
    private final PolicyViolation violation;

    ViolationDiscoveredEvent(PolicyViolation violation) {
      super(violation.getOpenTime());
      this.violation = violation;
    }

    @Override
    void process(MttrStats mttrStats,
                 DiscoveredStats discoveredStats,
                 SortedMap<PolicyViolationComparable, PolicyViolationResolutionState> resolutionStates)
    {
      PolicyViolationResolutionState newResolutionState = new PolicyViolationResolutionState(violation);
      PolicyViolationResolutionState oldResolutionState = resolutionStates.putIfAbsent(newResolutionState,
          newResolutionState);
      if (oldResolutionState == null) {
        newResolutionState.setStageTypeById(violation.getStageTypeId());
        discoveredStats.addViolation(newResolutionState);
        policyViolationResolutionStateDAO.insert(newResolutionState);
      }
      else {
        oldResolutionState.setStageTypeById(violation.getStageTypeId());
        policyViolationResolutionStateDAO.update(oldResolutionState);
      }
    }
  }

  private class ViolationResolvedEvent
      extends EvaluationEvent
  {
    private final PolicyViolation violation;

    ViolationResolvedEvent(PolicyViolation violation, Date resolveTime) {
      super(resolveTime);
      this.violation = violation;
    }

    @Override
    void process(MttrStats mttrStats,
                 DiscoveredStats discoveredStats,
                 SortedMap<PolicyViolationComparable, PolicyViolationResolutionState> resolutionStates)
    {
      PolicyViolationResolutionState resolutionState = resolutionStates.get(violation);
      resolutionState.setStageTypeById(violation.getStageTypeId(), false);
      if (resolutionState.isClearedInAllStages()) {
        resolutionStates.remove(resolutionState);
        policyViolationResolutionStateDAO.delete(resolutionState);
        mttrStats.addViolation(violation, resolutionState.getFirstOccurrenceTime(), time);
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
   * Get the PolicyViolationResolutionState entries for the specified applicationId, as a map based on
   * PolicyViolationComparator
   */
  private SortedMap<PolicyViolationComparable, PolicyViolationResolutionState> getPolicyViolationResolutionStates(String applicationId) {
    SortedMap<PolicyViolationComparable, PolicyViolationResolutionState> resolutionStates = new TreeMap<>(
        PolicyViolationComparator.COMPARATOR);
    for (PolicyViolationResolutionState resolutionState : policyViolationResolutionStateDAO
        .getByApplicationId(applicationId)) {
      resolutionStates.put(resolutionState, resolutionState);
    }
    return resolutionStates;
  }

  private PolicyViolationAggregation saveViolationAggregation(String applicationId,
                                        LocalDate timePeriodStart,
                                        DateTime timePeriodEnd,
                                        MttrStats mttrStats,
                                        DiscoveredStats discoveredStats)
  {
    return saveViolationAggregation(applicationId, timePeriodStart, timePeriodEnd, mttrStats, discoveredStats, null);
  }

  private PolicyViolationAggregation saveViolationAggregation(String applicationId,
                                        LocalDate timePeriodStart,
                                        DateTime timePeriodEnd,
                                        MttrStats mttrStats,
                                        DiscoveredStats discoveredStats,
                                        String aggregationToUpdateId)
  {
    PolicyViolationAggregation aggregation = new PolicyViolationAggregation(applicationId, //
        localDateToTimestamp(timePeriodStart), //
        timePeriodEnd == null ? null : timePeriodEnd.toDate(), //
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

    if (aggregationToUpdateId != null) {
      aggregation.setId(aggregationToUpdateId);
      violationAggregationDAO.update(aggregation);
    } else {
      violationAggregationDAO.insert(aggregation);
    }
    return aggregation;
  }
}
