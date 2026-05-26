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
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO;
import com.sonatype.insight.brain.model.EnumIntegerTable;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolationComparable;
import com.sonatype.insight.brain.model.successmetrics.PolicyViolationAggregation;
import com.sonatype.insight.brain.model.successmetrics.TimePeriod;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationComparator;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.utils.ThreatLevel;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.TreeMultimap;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.api.v2.service.ApiConfigurationService.INVALID_SUCCESS_METRIC_STAGE_ID_ERROR_MSG;
import static com.sonatype.insight.brain.model.successmetrics.TimePeriod.MONTH;
import static com.sonatype.insight.brain.model.successmetrics.TimePeriod.WEEK;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * @since 1.31
 */
@Named
@Singleton
public class PolicyViolationAggregationService
{
  private static final Logger log = LoggerFactory.getLogger(PolicyViolationAggregationService.class);

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final PolicyViolationAggregationDAO violationAggregationDAO;

  private final StageTypeService stageTypeService;

  private final ClusterLockManager clusterLockManager;

  private final Configuration configuration;

  @Inject
  public PolicyViolationAggregationService(
      final StageTypeService stageTypeService,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final PolicyViolationDAO policyViolationDAO,
      final PolicyViolationAggregationDAO violationAggregationDAO,
      final ClusterLockManager clusterLockManager,
      final Configuration configuration)
  {
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.violationAggregationDAO = violationAggregationDAO;
    this.stageTypeService = stageTypeService;
    this.clusterLockManager = clusterLockManager;
    this.configuration = configuration;
  }

  public Set<String> getStageTypeIds() {
    return stageTypeService.getValidSuccessMetricsStageTypeIds();
  }

  /**
   * Update PolicyViolationAggregation rows for each of the specified applications. This determines the most recent
   * time period that was already aggregated for each application and then creates all necessary aggregations
   * for the time periods since then.
   */
  public void generatePolicyViolationAggregations(
      Set<String> applicationIds,
      DateTime currentDateTime,
      boolean includeLatestData)
  {
    log.debug("Starting update of Policy Violation Aggregations for {} applications", applicationIds.size());

    long start = System.currentTimeMillis();

    final String configuredSuccessMetricsStageId =
        ApiConfigurationService.normalizeSuccessMetricsStageId(configuration.getSuccessMetricsStageId());

    // this is also validated when the user sets the value, but we'll check again, here, in case
    // the licensed stages changed after it was set
    if (nonNull(configuredSuccessMetricsStageId) && !getStageTypeIds().contains(configuredSuccessMetricsStageId)) {
      throw new BadRequestException(String.format(
          INVALID_SUCCESS_METRIC_STAGE_ID_ERROR_MSG,
          configuredSuccessMetricsStageId,
          SystemConfigurationProperty.SUCCESS_METRICS_STAGE_ID,
          getStageTypeIds()));
    }

    final Set<String> stageTypeIds = isNull(configuredSuccessMetricsStageId)
        ? getStageTypeIds()
        : Sets.newHashSet(configuredSuccessMetricsStageId);

    for (String applicationId : applicationIds) {
      try (ClusterLock clusterLock = clusterLockManager.createForPolicyViolationAggregations(applicationId)) {
        clusterLock.lock();

        generatePolicyViolationAggregations(applicationId, currentDateTime, stageTypeIds, includeLatestData);
      }
    }

    long finish = System.currentTimeMillis();
    log.debug("Finished update of Policy Violation Aggregations in {} ms", finish - start);
  }

  /**
   * Generate the newer PolicyViolationAggregation rows for the given application
   */
  private void generatePolicyViolationAggregations(
      String applicationId,
      DateTime currentDateTime,
      Set<String> stageTypeIds,
      boolean includeLatestData)
  {
    log.trace("Generating Violation Aggregations for {}", applicationId);
    LocalDate currentDate = currentDateTime.toLocalDate();

    Map<TimePeriod, LocalDate> aggregationStarts = new EnumMap<>(TimePeriod.class);
    Map<TimePeriod, Table<PolicyThreatCategory, ThreatLevel, Integer>> openViolationCountsMap = new EnumMap<>(
        TimePeriod.class);

    for (TimePeriod timePeriod : TimePeriod.values()) {
      LocalDate startOfCurrentTimePeriod = withDayOfTimePeriod(currentDate, timePeriod, 1);

      PolicyViolationAggregation mostRecentPriorAggregation = violationAggregationDAO
          .getMostRecentByApplicationIdAndTimePeriod(applicationId, timePeriod);

      if (isPartial(mostRecentPriorAggregation)) {
        mostRecentPriorAggregation = updatePartialAggregation(mostRecentPriorAggregation, currentDateTime,
            applicationId, stageTypeIds, includeLatestData, timePeriod);
      }

      LocalDate startOfNewAggregation = getNewAggregationStartingPoint(timePeriod, mostRecentPriorAggregation,
          applicationId);

      if (startOfNewAggregation != null) {
        boolean upToDate = isAggregationUpToDate(mostRecentPriorAggregation, startOfNewAggregation,
            startOfCurrentTimePeriod, includeLatestData);

        if (!upToDate) {
          aggregationStarts.put(timePeriod, startOfNewAggregation);
          Table<PolicyThreatCategory, ThreatLevel, Integer> openCounts =
              mostRecentPriorAggregation == null
                  ? new EnumIntegerTable<>(PolicyThreatCategory.class,
                      ThreatLevel.class)
                  : mostRecentPriorAggregation.getOpenAsTable();
          openViolationCountsMap.put(timePeriod, openCounts);
        }
      }
    }

    if (aggregationStarts.isEmpty()) {
      // No aggregations to create/update
      return;
    }

    // Use the earliest startOfNewAggregation
    LocalDate monthStartDate = aggregationStarts.get(MONTH);
    LocalDate weekStartDate = aggregationStarts.get(WEEK);
    LocalDate eventsStartDate = Ordering.natural().nullsLast().min(monthStartDate, weekStartDate);
    List<ProcessableEvaluationEvent> events = createSortedEvaluationEvents(applicationId, stageTypeIds,
        localDateToTimestamp(eventsStartDate), currentDateTime.toDate());

    Map<TimePeriod, ResultsWrapper> results = new EnumMap<>(TimePeriod.class);

    for (TimePeriod timePeriod : TimePeriod.values()) {
      // keep a running tally of open counts
      Table<PolicyThreatCategory, ThreatLevel, Integer> openCounts = openViolationCountsMap.get(timePeriod);
      results.put(timePeriod, new ResultsWrapper(openCounts));

      LocalDate startOfNewAggregation = aggregationStarts.get(timePeriod);

      if (startOfNewAggregation != null) {
        LocalDate startOfNextAggregation = plusTimePeriod(startOfNewAggregation, timePeriod, 1);

        ResultsWrapper result = results.get(timePeriod);
        if (!events.isEmpty()) {
          for (ProcessableEvaluationEvent event : events) {
            LocalDate eventTime = new LocalDate(event.time);

            if (eventTime.isBefore(startOfNewAggregation)) {
              // event is from before the current aggregation, ignore
              continue;
            }
            else {
              while (eventTime.compareTo(startOfNextAggregation) >= 0) {
                // event is too recent for the current aggregation record, start a new one
                saveViolationAggregation(applicationId, startOfNewAggregation, null, result, timePeriod);
                startOfNewAggregation = startOfNextAggregation;
                startOfNextAggregation = plusTimePeriod(startOfNewAggregation, timePeriod, 1);

                result = new ResultsWrapper(openCounts);
                results.put(timePeriod, result);
              }
              event.process(result, timePeriod);
            }
          }
        }

        LocalDate startOfCurrentTimePeriod = withDayOfTimePeriod(currentDate, timePeriod, 1);

        // insert the last aggregation from the loop above and any others necessary to bring things up to the
        // start of the current time period
        while (includeLatestData
            ? startOfNewAggregation.compareTo(currentDate) <= 0
            : startOfNewAggregation.compareTo(startOfCurrentTimePeriod) < 0)
        {
          DateTime endDateTime = null;
          if (includeLatestData && startOfCurrentTimePeriod.equals(startOfNewAggregation)) {
            endDateTime = currentDateTime;
          }
          saveViolationAggregation(applicationId, startOfNewAggregation, endDateTime, result, timePeriod);
          startOfNewAggregation = startOfNextAggregation;
          startOfNextAggregation = plusTimePeriod(startOfNewAggregation, timePeriod, 1);
          result = new ResultsWrapper(openCounts);
          results.put(timePeriod, result);
        }
      }
    }
  }

  /**
   * Get the date at which the first new aggregation of this application should start
   *
   * @param timePeriod The TimePeriod in which to do the calculation (weeks or months)
   * @param mostRecentPriorAggregation The most recent existing PolicyViolationAggregation, if any
   * @param applicationId The id of the application to check
   * @return a LocalDate for the starting point of the first new aggregation that should be generated for this
   *         application, or null if this application has never had any evaluations, implying that no aggregations
   *         should
   *         be generated
   */
  private LocalDate getNewAggregationStartingPoint(
      TimePeriod timePeriod,
      PolicyViolationAggregation mostRecentPriorAggregation,
      String applicationId)
  {
    // start the next new aggregation at the beginning of the time period after the last aggregation, or at the
    // beginning of the time period of the first evaluation if there aren't any aggregations for this app yet
    Optional<PolicyEvaluation> oldestEvaluation =
        Optional.ofNullable(policyEvaluationDAO.getOldestByApplicationId(applicationId));

    Optional<LocalDate> startOfMostRecentPriorAggregation =
        Optional.ofNullable(mostRecentPriorAggregation).map(agg -> new LocalDate(agg.getTimePeriodStart()));

    Optional<LocalDate> oldestEvaluationDate = oldestEvaluation.map(eval -> new LocalDate(eval.getTime()));
    Optional<LocalDate> startOfFirstAggregation =
        oldestEvaluationDate.map(date -> withDayOfTimePeriod(date, timePeriod, 1));

    Optional<LocalDate> startOfNewAggregation = startOfMostRecentPriorAggregation
        .map(startOfMostRecent -> plusTimePeriod(startOfMostRecent, timePeriod, 1)) //
        .map(Optional::of) //
        .orElse(startOfFirstAggregation);

    return startOfNewAggregation.orElse(null);
  }

  private boolean isAggregationUpToDate(
      PolicyViolationAggregation mostRecentPriorAggregation,
      LocalDate startOfNewAggregation,
      LocalDate startOfCurrentTimePeriod,
      boolean includeLatestData)
  {
    return includeLatestData && isPartial(mostRecentPriorAggregation) ||
        !includeLatestData && startOfNewAggregation.compareTo(startOfCurrentTimePeriod) >= 0;
  }

  private LocalDate withDayOfTimePeriod(LocalDate dateTime, TimePeriod timePeriod, int dayOf) {
    return dateTime.withField(timePeriod.getDateTimeFieldType(), dayOf);
  }

  private LocalDate plusTimePeriod(LocalDate dateTime, TimePeriod timePeriod, int timePeriods) {
    return dateTime.plus(timePeriod.getPeriod(timePeriods));
  }

  private boolean isPartial(PolicyViolationAggregation mostRecentPriorAggregation) {
    return mostRecentPriorAggregation != null && mostRecentPriorAggregation.getTimePeriodEnd() != null;
  }

  /**
   * Updates the supplied partial aggregation with latest data. If we have a full month's worth of data then the
   * aggregation's timePeriodEnd will be removed, making it a regular aggregation.
   */
  private PolicyViolationAggregation updatePartialAggregation(
      PolicyViolationAggregation partialAggregation,
      DateTime currentTime,
      String applicationId,
      Set<String> stageTypeIds,
      boolean includeLatestData,
      TimePeriod timePeriod)
  {
    LocalDate startOfNextAggregation = withDayOfTimePeriod(
        plusTimePeriod(new LocalDate(partialAggregation.getTimePeriodStart()), timePeriod, 1), timePeriod, 1);
    boolean isLastAggregation = currentTime.isBefore(startOfNextAggregation.toDateTimeAtStartOfDay());

    if (!includeLatestData && isLastAggregation) {
      return partialAggregation; // no need to update current month/week
    }

    Date from = partialAggregation.getTimePeriodEnd();
    Date upTo = withDayOfTimePeriod(plusTimePeriod(new LocalDate(from), timePeriod, 1), timePeriod, 1)
        .toDateTimeAtStartOfDay()
        .toDate();
    List<ProcessableEvaluationEvent> events = createSortedEvaluationEvents(applicationId, stageTypeIds, from, upTo);

    ResultsWrapper results = recreateResults(partialAggregation);

    if (!events.isEmpty()) {
      for (ProcessableEvaluationEvent event : events) {
        event.process(results, timePeriod);
      }
    }
    LocalDate timePeriodStart = new LocalDate(partialAggregation.getTimePeriodStart());
    DateTime timePeriodEnd = isLastAggregation ? currentTime : null;
    return saveViolationAggregation(applicationId, timePeriodStart, timePeriodEnd, results, partialAggregation.getId(),
        timePeriod);
  }

  private ResultsWrapper recreateResults(PolicyViolationAggregation partialAggregation) {
    MttrStats mttrStats = recreateMttrStats(partialAggregation);
    int evaluationCount = partialAggregation.getEvaluationCount();

    Table<PolicyThreatCategory, ThreatLevel, Integer> discoveredCounts = partialAggregation.getDiscoveredAsTable();
    Table<PolicyThreatCategory, ThreatLevel, Integer> fixedCounts = partialAggregation.getFixedAsTable();
    Table<PolicyThreatCategory, ThreatLevel, Integer> waivedCounts = partialAggregation.getWaivedAsTable();
    Table<PolicyThreatCategory, ThreatLevel, Integer> openCounts = partialAggregation.getOpenAsTable();
    return new ResultsWrapper(mttrStats, evaluationCount, discoveredCounts, fixedCounts, waivedCounts, openCounts);
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

  private List<ProcessableEvaluationEvent> createSortedEvaluationEvents(
      String applicationId,
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
    policyViolationDAO.loadConstraintFacts(violations);

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

      boolean isWaivedInTimeframe = violation.getWaiveTime() != null && violation.getWaiveTime().compareTo(upTo) < 0;
      boolean isFixedInTimeframe = violation.getFixTime() != null && violation.getFixTime().compareTo(upTo) < 0;
      Date resolveTime =
          isWaivedInTimeframe ? violation.getWaiveTime() : isFixedInTimeframe ? violation.getFixTime() : null;

      if (resolveTime != null &&
          !isViolationWithUnresolvedDuplicate(violation, resolveTime, violationMap.get(violation)))
      {
        events.add(new ViolationResolvedInStageEvent(violation, resolveTime, isWaivedInTimeframe));
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
   *          time period that these evaluationEvents traverse
   * @param evaluationEvents a chronologically ordered list of EvaluationEvents
   */
  private List<ProcessableEvaluationEvent> handleViolationStages(
      Collection<PolicyViolation> initialActiveViolations,
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
            ProcessableViolationEvent outputEvent =
                event.isWaived
                    ? new ViolationWaivedEvent(violation, firstOccurrence, event.time)
                    : new ViolationFixedEvent(violation, firstOccurrence, event.time);
            retval.add(outputEvent);
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
   *         collection, was opened
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

  private boolean isViolationWithUnresolvedDuplicate(
      PolicyViolation violation,
      Date resolveTime,
      List<PolicyViolation> equalViolations)
  {
    for (PolicyViolation equalViolation : equalViolations) {
      if (violation != equalViolation && equalViolation.getStageTypeId().equals(violation.getStageTypeId())
          && isViolationUnresolved(equalViolation, resolveTime))
      {
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

  private PolicyViolationAggregation saveViolationAggregation(
      String applicationId,
      LocalDate timePeriodStart,
      DateTime timePeriodEnd,
      ResultsWrapper results,
      TimePeriod timePeriod)
  {
    return saveViolationAggregation(applicationId, timePeriodStart, timePeriodEnd, results, null, timePeriod);
  }

  private PolicyViolationAggregation saveViolationAggregation(
      String applicationId,
      LocalDate timePeriodStart,
      DateTime timePeriodEnd,
      ResultsWrapper results,
      String aggregationToUpdateId,
      TimePeriod timePeriod)
  {
    PolicyViolationAggregation aggregation = new PolicyViolationAggregation(applicationId, //
        localDateToTimestamp(timePeriodStart), //
        timePeriodEnd == null ? null : timePeriodEnd.toDate(), //
        timePeriod, //
        results.mttrStats.mttrLowThreatStats, //
        results.mttrStats.mttrModerateThreatStats, //
        results.mttrStats.mttrSevereThreatStats, //
        results.mttrStats.mttrCriticalThreatStats, //
        results.discoveredCounts, //
        results.fixedCounts, //
        results.waivedCounts, //
        results.openCounts, //
        results.evaluationCount);

    if (aggregationToUpdateId != null) {
      aggregation.setId(aggregationToUpdateId);
      violationAggregationDAO.update(aggregation);
    }
    else {
      violationAggregationDAO.insert(aggregation);
    }
    return aggregation;
  }
}
