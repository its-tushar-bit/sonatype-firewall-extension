/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dashboard.DashboardUtils;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolationComparable;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.successmetrics.PolicyViolationAggregation;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationComparator;
import com.sonatype.insight.brain.utils.ThreatLevel;

import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
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

  private final StageTypeService stageTypeService;

  private final DashboardUtils dashboardUtils;

  private final ConcurrentMap<String, Lock> applicationIdLocks = CacheBuilder.newBuilder().weakValues()
      .<String, Lock> build().asMap();

  @Inject
  public PolicyViolationAggregationService(StageTypeService stageTypeService,
                                           PolicyEvaluationDAO policyEvaluationDAO,
                                           PolicyViolationDAO policyViolationDAO,
                                           PolicyViolationAggregationDAO violationAggregationDAO,
                                           DashboardUtils dashboardUtils)
  {
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.violationAggregationDAO = violationAggregationDAO;
    this.stageTypeService = stageTypeService;
    this.dashboardUtils = dashboardUtils;
  }

  private Set<String> getStageTypeIds() {
    List<StageType> stageTypes = new ArrayList<>();

    for (StageType stageType : stageTypeService.getLicensedStageTypes()) {
      if (!StageTypes.isIgnoredForPolicyViolationAggregation(stageType.getId())) {
        stageTypes.add(stageType);
      }
    }

    return dashboardUtils.getStageTypeIds(stageTypes);
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
    Set<String> stageTypeIds = getStageTypeIds();

    for (String applicationId : applicationIds) {
      Lock lock = acquireLockForApplication(applicationId);

      try {
        generatePolicyViolationAggregations(applicationId, currentDateTime, stageTypeIds, includeLatestData);
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
   * Generate the newer PolicyViolationAggregation rows for the given application
   */
  private void generatePolicyViolationAggregations(String applicationId,
                                                   DateTime currentDateTime,
                                                   Set<String> stageTypeIds,
                                                   boolean includeLatestData)
  {
    log.trace("Generating Violation Aggregations for {}", applicationId);

    PolicyViolationAggregation mostRecentPriorAggregation = violationAggregationDAO
        .getMostRecentByApplicationId(applicationId);

    if (isPartial(mostRecentPriorAggregation)) {
      mostRecentPriorAggregation = updatePartialAggregation(mostRecentPriorAggregation, currentDateTime, applicationId,
          stageTypeIds, includeLatestData);
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

    List<ProcessableEvaluationEvent> events = createSortedEvaluationEvents(applicationId, stageTypeIds,
        localDateToTimestamp(startOfNewAggregation), currentDateTime.toDate());

    MttrStats mttrStats = new MttrStats();
    DiscoveredStats discoveredStats = new DiscoveredStats();

    if (!events.isEmpty()) {
      for (ProcessableEvaluationEvent event : events) {
        while (new LocalDate(event.time).compareTo(startOfNextAggregation) >= 0) {
          // event is too recent for the current aggregation record, start a new one
          saveViolationAggregation(applicationId, startOfNewAggregation, null, mttrStats, discoveredStats);
          startOfNewAggregation = startOfNextAggregation;
          startOfNextAggregation = startOfNewAggregation.plusMonths(1);

          mttrStats = new MttrStats();
          discoveredStats = new DiscoveredStats();
        }
        event.process(mttrStats, discoveredStats);
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
                                                              Set<String> stageTypeIds,
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

    List<ProcessableEvaluationEvent> events = createSortedEvaluationEvents(applicationId, stageTypeIds, from, upTo);

    MttrStats mttrStats = recreateMttrStats(partialAggregation);
    DiscoveredStats discoveredStats = recreateDiscoveredStats(partialAggregation);

    if (!events.isEmpty()) {
      for (ProcessableEvaluationEvent event : events) {
        event.process(mttrStats, discoveredStats);
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

  private List<ProcessableEvaluationEvent> createSortedEvaluationEvents(String applicationId,
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

    List<PolicyViolation> violationsActiveAtBeginning = violations.stream()
        .filter(v -> v.getOpenTime().compareTo(from) < 0)
        .collect(Collectors.toList());

    SortedMap<PolicyViolationComparable, List<PolicyViolation>> violationMap = new TreeMap<>(
        PolicyViolationComparator.COMPARATOR);
    for (PolicyViolation violation : violations) {
      List<PolicyViolation> equalViolations = violationMap.get(violation);
      if (equalViolations == null) {
        equalViolations = new ArrayList<>(1);
        violationMap.put(violation, equalViolations);
      }
      equalViolations.add(violation);
    }

    List<EvaluationEvent> events = new ArrayList<>(evaluations.size() + violations.size() * 2);
    for (PolicyEvaluation evaluation : evaluations) {
      events.add(new EvaluationPerformedEvent(evaluation));
    }
    for (PolicyViolation violation : violations) {
      if (from.compareTo(violation.getOpenTime()) <= 0) {
        events.add(new ViolationDiscoveredInStageEvent(violation));
      }
      Date resolveTime = getResolveTime(violation);
      if (resolveTime != null && resolveTime.compareTo(upTo) < 0
          && !isViolationWithUnresolvedDuplicate(violation, resolveTime, violationMap.get(violation))) {
        events.add(new ViolationResolvedInStageEvent(violation, resolveTime));
      }
    }
    events.sort(null);

    return handleViolationStages(violationsActiveAtBeginning, events);
  }

  /**
   * Keep track of the stages involved in each ViolationResolvedInStageEvent and ViolationDiscoveredInStageEvent and
   * transform them into ViolationDiscoveredEvent and ViolationResolvedEvents as appropriate. Other types of
   * ProcessableEvaluationEvents are passed through unchanged
   *
   * @param initialActiveViolations collection of PolicyViolations that are already active at the beginning of the
   * time period that these evaluationEvents traverse
   * @param evaluationEvents a chronologically ordered list of EvaluationEvents
   */
  private List<ProcessableEvaluationEvent> handleViolationStages(Collection<PolicyViolation> initialActiveViolations,
                                                                 List<EvaluationEvent> evaluationEvents)
  {
    // A map from violation to the stages in which that violation is currently active
    Multimap<PolicyViolation, String> violationActiveStages = createActiveStagesMap(initialActiveViolations);

    // A map from violation to its first occurrence time across all stages
    Map<PolicyViolation, Date> firstOccurrenceDates = createFirstOccurrencesMap(initialActiveViolations);

    List<ProcessableEvaluationEvent> retval = new LinkedList<>();

    for (EvaluationEvent evaluationEvent : evaluationEvents) {
      if (evaluationEvent instanceof ProcessableEvaluationEvent) {
        retval.add((ProcessableEvaluationEvent) evaluationEvent);
      }
      else if (evaluationEvent instanceof ViolationDiscoveredInStageEvent) {
        PolicyViolation violation = ((ViolationDiscoveredInStageEvent) evaluationEvent).violation;

        // only add a ViolationDiscoveredEvent if it isn't already active in other stages (or in this stage from a
        // duplicate)
        if (!violationActiveStages.containsKey(violation)) {
          retval.add(new ViolationDiscoveredEvent(violation));
        }

        violationActiveStages.put(violation, violation.getStageTypeId());
        firstOccurrenceDates.putIfAbsent(violation, violation.getOpenTime());
      }
      else if (evaluationEvent instanceof ViolationResolvedInStageEvent) {
        ViolationResolvedInStageEvent event = (ViolationResolvedInStageEvent) evaluationEvent;
        PolicyViolation violation = event.violation;

        // NOTE: removed can be false in the case of multiple duplicate violations getting resolved
        // at the same time. After the first one, successive ones should have no effect
        boolean removed = violationActiveStages.remove(violation, violation.getStageTypeId());

        // only add a ViolationResolvedEvent if it isn't still active in other stages
        if (removed && !violationActiveStages.containsKey(violation)) {

          Date firstOccurrence = firstOccurrenceDates.remove(violation);

          if (firstOccurrence == null) {
            throw new IllegalStateException("Unable to find first occurrence of Policy Violation");
          }
          else {
            retval.add(new ViolationResolvedEvent(violation, firstOccurrence, event.time));
          }
        }
      }
      else {
        throw new IllegalArgumentException("Unexpected EvaluationEvent");
      }
    }

    return retval;
  }

  /**
   * From a list of currently-active violations, create a multimap from violation to stages in which it (or an
   * equivalent violation) is currently active.
   */
  private Multimap<PolicyViolation, String> createActiveStagesMap(Collection<PolicyViolation> violations) {
    Multimap<PolicyViolation, String> retval =
        TreeMultimap.create(PolicyViolationComparator.COMPARATOR, Comparator.naturalOrder());

    for (PolicyViolation violation : violations) {
      retval.put(violation, violation.getStageTypeId());
    }

    return retval;
  }

  /**
   * @return a map from violation to the earliest date at which it, or another comparator-equivalent violation in the
   * collection, was opened
   */
  private Map<PolicyViolation, Date> createFirstOccurrencesMap(Collection<PolicyViolation> violations) {
    Map<PolicyViolation, Date> retval = new TreeMap<>(PolicyViolationComparator.COMPARATOR);

    for (PolicyViolation violation : violations) {
      retval.merge(violation, violation.getOpenTime(),
          (date1, date2) -> new Date(Math.min(date1.getTime(), date2.getTime())));
    }

    return retval;
  }

  private Date getResolveTime(PolicyViolation violation) {
    Date resolveTime = violation.getWaiveTime();
    if (resolveTime == null) {
      resolveTime = violation.getFixTime();
    }
    return resolveTime;
  }

  private boolean isViolationUnresolved(PolicyViolation violation, Date timestamp) {
    if (violation.getOpenTime().compareTo(timestamp) <= 0) {
      Date resolveTime = getResolveTime(violation);
      if (resolveTime == null || resolveTime.compareTo(timestamp) > 0) {
        return true;
      }
    }
    return false;
  }

  private boolean isViolationWithUnresolvedDuplicate(PolicyViolation violation,
                                                     Date resolveTime,
                                                     List<PolicyViolation> equalViolations)
  {
    for (PolicyViolation equalViolation : equalViolations) {
      if (violation != equalViolation && equalViolation.getStageTypeId().equals(violation.getStageTypeId())
          && isViolationUnresolved(equalViolation, resolveTime)) {
        return true;
      }
    }
    return false;
  }

  private static Date localDateToTimestamp(LocalDate date) {
    if (date == null) {
      return null;
    }
    return date.toDateTimeAtStartOfDay().toDate();
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
