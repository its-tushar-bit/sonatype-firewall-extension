/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.successmetrics;

import java.math.BigDecimal;
import java.util.Date;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractAggregationSqlDAO;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.successmetrics.PolicyViolationAggregation;
import com.sonatype.insight.brain.model.successmetrics.TimePeriod;
import com.sonatype.insight.brain.utils.ThreatLevel;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.annotations.VisibleForTesting;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.joda.time.LocalDate;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.aggregation.tables.PolicyViolationAggregation.POLICY_VIOLATION_AGGREGATION;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.LICENSE;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.OTHER;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.QUALITY;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.SECURITY;
import static com.sonatype.insight.brain.model.successmetrics.TimePeriod.MONTH;
import static com.sonatype.insight.brain.model.successmetrics.TimePeriod.WEEK;

/**
 * @since 1.31
 */
@Named
@Singleton
public class PolicyViolationAggregationDAO
    extends AbstractAggregationSqlDAO<PolicyViolationAggregation>
{
  public static final int NUM_PERIODS = 12;

  @Inject
  public PolicyViolationAggregationDAO(final AggregationDataStore aggregationDataStore) {
    super(aggregationDataStore);
  }

  @VisibleForTesting
  public List<PolicyViolationAggregation> getByTimePeriod(TimePeriod timePeriod) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(POLICY_VIOLATION_AGGREGATION)
          .where(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD.eq(timePeriod.name()))
          .fetchInto(PolicyViolationAggregation.class);
    }
  }

  public PolicyViolationAggregation getMostRecentByApplicationIdAndTimePeriod(
      String applicationId,
      TimePeriod timePeriod)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(POLICY_VIOLATION_AGGREGATION)
          .where(POLICY_VIOLATION_AGGREGATION.APPLICATION_ID.eq(applicationId))
          .and(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD.eq(timePeriod.name()))
          .orderBy(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD_START.desc())
          .limit(1)
          .fetchOneInto(PolicyViolationAggregation.class);
    }
  }

  public static class MttrMonth
  {
    public final Date monthStart;

    // in milliseconds
    public final Long mttrLowThreat;

    public final Long mttrModerateThreat;

    public final Long mttrSevereThreat;

    public final Long mttrCriticalThreat;

    public final int resolvedCountLowThreat;

    public final int resolvedCountModerateThreat;

    public final int resolvedCountSevereThreat;

    public final int resolvedCountCriticalThreat;

    MttrMonth(
        Date monthStart,
        Long mttrLowThreat,
        Long mttrModerateThreat,
        Long mttrSevereThreat,
        Long mttrCriticalThreat,
        int resolvedCountLowThreat,
        int resolvedCountModerateThreat,
        int resolvedCountSevereThreat,
        int resolvedCountCriticalThreat)
    {
      this.monthStart = monthStart;

      this.mttrLowThreat = mttrLowThreat;
      this.mttrModerateThreat = mttrModerateThreat;
      this.mttrSevereThreat = mttrSevereThreat;
      this.mttrCriticalThreat = mttrCriticalThreat;

      this.resolvedCountLowThreat = resolvedCountLowThreat;
      this.resolvedCountModerateThreat = resolvedCountModerateThreat;
      this.resolvedCountSevereThreat = resolvedCountSevereThreat;
      this.resolvedCountCriticalThreat = resolvedCountCriticalThreat;
    }
  }

  public static class AverageMonth
  {
    public Date timePeriodStart;

    public AverageThreatCategoryMonth security;

    public AverageThreatCategoryMonth license;

    public AverageThreatCategoryMonth quality;

    public AverageThreatCategoryMonth other;

    public final int evaluationCount;

    public AverageMonth(
        Date timePeriodStart,
        AverageThreatCategoryMonth security,
        AverageThreatCategoryMonth license,
        AverageThreatCategoryMonth quality,
        AverageThreatCategoryMonth other,
        int evaluationCount)
    {
      this.timePeriodStart = timePeriodStart;
      this.security = security;
      this.license = license;
      this.quality = quality;
      this.other = other;

      this.evaluationCount = evaluationCount;
    }
  }

  public static class AverageThreatCategoryMonth
  {
    public final double averageDiscoveredLowThreat;

    public final double averageDiscoveredModerateThreat;

    public final double averageDiscoveredSevereThreat;

    public final double averageDiscoveredCriticalThreat;

    public AverageThreatCategoryMonth(
        double averageDiscoveredLowThreat,
        double averageDiscoveredModerateThreat,
        double averageDiscoveredSevereThreat,
        double averageDiscoveredCriticalThreat)
    {
      this.averageDiscoveredLowThreat = averageDiscoveredLowThreat;
      this.averageDiscoveredModerateThreat = averageDiscoveredModerateThreat;
      this.averageDiscoveredSevereThreat = averageDiscoveredSevereThreat;
      this.averageDiscoveredCriticalThreat = averageDiscoveredCriticalThreat;
    }

    public double getSum() {
      return averageDiscoveredLowThreat + averageDiscoveredModerateThreat + averageDiscoveredSevereThreat
          + averageDiscoveredCriticalThreat;
    }
  }

  public static class ViolationCountPeriod
  {
    public final Date periodStart;

    public final Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> discoveredCounts;

    public final Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> fixedCounts;

    public final Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> waivedCounts;

    public ViolationCountPeriod(
        Date periodStart,
        Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> discoveredCounts,
        Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> fixedCounts,
        Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> waivedCounts)
    {
      this.periodStart = periodStart;
      this.discoveredCounts = discoveredCounts;
      this.fixedCounts = fixedCounts;
      this.waivedCounts = waivedCounts;
    }
  }

  public static class OpenViolationCountsWeek
  {
    public final Date weekStart;

    public Map<PolicyThreatCategory, Integer> openViolationCounts;

    OpenViolationCountsWeek(Date weekStart, Map<PolicyThreatCategory, Integer> openViolationCounts) {
      this.weekStart = weekStart;
      this.openViolationCounts = openViolationCounts;
    }
  }

  /**
   * Get MTTR monthly averages for the specified applications, for the 12 most recent months
   */
  public List<MttrMonth> getMttrMonthlyAverages(Set<String> applicationIds, boolean includeLatestData) {
    try (TransactionContext tx = createTransactionContext()) {
      // Define resolved count fields for each threat level
      var resolvedCountLow = POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_SECURITY_LOW_THREAT
          .plus(POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_LICENSE_LOW_THREAT)
          .plus(POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_QUALITY_LOW_THREAT)
          .plus(POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_OTHER_LOW_THREAT)
          .plus(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_SECURITY_LOW_THREAT)
          .plus(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_LICENSE_LOW_THREAT)
          .plus(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_QUALITY_LOW_THREAT)
          .plus(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_OTHER_LOW_THREAT);

      var resolvedCountModerate = POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_SECURITY_MODERATE_THREAT
          .plus(POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_LICENSE_MODERATE_THREAT)
          .plus(POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_QUALITY_MODERATE_THREAT)
          .plus(POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_OTHER_MODERATE_THREAT)
          .plus(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_SECURITY_MODERATE_THREAT)
          .plus(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_LICENSE_MODERATE_THREAT)
          .plus(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_QUALITY_MODERATE_THREAT)
          .plus(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_OTHER_MODERATE_THREAT);

      var resolvedCountSevere = POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_SECURITY_SEVERE_THREAT
          .plus(POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_LICENSE_SEVERE_THREAT)
          .plus(POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_QUALITY_SEVERE_THREAT)
          .plus(POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_OTHER_SEVERE_THREAT)
          .plus(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_SECURITY_SEVERE_THREAT)
          .plus(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_LICENSE_SEVERE_THREAT)
          .plus(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_QUALITY_SEVERE_THREAT)
          .plus(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_OTHER_SEVERE_THREAT);

      var resolvedCountCritical = POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_SECURITY_CRITICAL_THREAT
          .plus(POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_LICENSE_CRITICAL_THREAT)
          .plus(POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_QUALITY_CRITICAL_THREAT)
          .plus(POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_OTHER_CRITICAL_THREAT)
          .plus(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_SECURITY_CRITICAL_THREAT)
          .plus(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_LICENSE_CRITICAL_THREAT)
          .plus(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_QUALITY_CRITICAL_THREAT)
          .plus(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_OTHER_CRITICAL_THREAT);

      Condition whereCondition = POLICY_VIOLATION_AGGREGATION.APPLICATION_ID.in(applicationIds)
          .and(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD.eq(MONTH.name()));
      if (!includeLatestData) {
        whereCondition = whereCondition.and(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD_END.isNull());
      }

      Result<?> results = tx.dsl()
          .select(
              POLICY_VIOLATION_AGGREGATION.TIME_PERIOD_START,
              DSL.sum(POLICY_VIOLATION_AGGREGATION.MTTR_LOW_THREAT.mul(resolvedCountLow))
                  .div(DSL.nullif(DSL.sum(resolvedCountLow), DSL.val(BigDecimal.ZERO))),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.MTTR_MODERATE_THREAT.mul(resolvedCountModerate))
                  .div(DSL.nullif(DSL.sum(resolvedCountModerate), DSL.val(BigDecimal.ZERO))),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.MTTR_SEVERE_THREAT.mul(resolvedCountSevere))
                  .div(DSL.nullif(DSL.sum(resolvedCountSevere), DSL.val(BigDecimal.ZERO))),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.MTTR_CRITICAL_THREAT.mul(resolvedCountCritical))
                  .div(DSL.nullif(DSL.sum(resolvedCountCritical), DSL.val(BigDecimal.ZERO))),
              DSL.sum(resolvedCountLow),
              DSL.sum(resolvedCountModerate),
              DSL.sum(resolvedCountSevere),
              DSL.sum(resolvedCountCritical))
          .from(POLICY_VIOLATION_AGGREGATION)
          .where(whereCondition)
          .groupBy(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD_START)
          .orderBy(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD_START.desc())
          .limit(NUM_PERIODS)
          .fetch();

      LinkedList<MttrMonth> retval = new LinkedList<>();

      for (Record row : results) {
        Date timePeriodStart = row.get(0, Date.class);
        Number mttrLowThreat = row.get(1, Number.class);
        Number mttrModerateThreat = row.get(2, Number.class);
        Number mttrSevereThreat = row.get(3, Number.class);
        Number mttrCriticalThreat = row.get(4, Number.class);
        Number resolvedCountLowThreat = row.get(5, Number.class);
        Number resolvedCountModerateThreat = row.get(6, Number.class);
        Number resolvedCountSevereThreat = row.get(7, Number.class);
        Number resolvedCountCriticalThreat = row.get(8, Number.class);

        MttrMonth mttrMonth = new MttrMonth(timePeriodStart,
            mttrLowThreat == null ? null : mttrLowThreat.longValue(),
            mttrModerateThreat == null ? null : mttrModerateThreat.longValue(),
            mttrSevereThreat == null ? null : mttrSevereThreat.longValue(),
            mttrCriticalThreat == null ? null : mttrCriticalThreat.longValue(),
            resolvedCountLowThreat == null ? 0 : resolvedCountLowThreat.intValue(),
            resolvedCountModerateThreat == null ? 0 : resolvedCountModerateThreat.intValue(),
            resolvedCountSevereThreat == null ? 0 : resolvedCountSevereThreat.intValue(),
            resolvedCountCriticalThreat == null ? 0 : resolvedCountCriticalThreat.intValue());

        // months come out in descending order to make result limiting easier. Add to the front of the retval
        // list in order to reverse order
        retval.push(mttrMonth);
      }

      return retval;
    }
  }

  /**
   * Get discovered violations monthly averages for the specified applications, for the 12 most recent months
   */
  public List<AverageMonth> getMonthlyAverages(Set<String> applicationIds, boolean includeLatestData) {
    try (TransactionContext tx = createTransactionContext()) {
      Condition whereCondition = POLICY_VIOLATION_AGGREGATION.APPLICATION_ID.in(applicationIds)
          .and(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD.eq(MONTH.name()));
      if (!includeLatestData) {
        whereCondition = whereCondition.and(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD_END.isNull());
      }

      Result<?> results = tx.dsl()
          .select(
              POLICY_VIOLATION_AGGREGATION.TIME_PERIOD_START,
              DSL.sum(DSL.when(POLICY_VIOLATION_AGGREGATION.EVALUATION_COUNT.gt(0), 1).otherwise(0)),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_SECURITY_LOW_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_SECURITY_MODERATE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_SECURITY_SEVERE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_SECURITY_CRITICAL_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_LICENSE_LOW_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_LICENSE_MODERATE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_LICENSE_SEVERE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_LICENSE_CRITICAL_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_QUALITY_LOW_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_QUALITY_MODERATE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_QUALITY_SEVERE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_QUALITY_CRITICAL_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_OTHER_LOW_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_OTHER_MODERATE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_OTHER_SEVERE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_OTHER_CRITICAL_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.EVALUATION_COUNT))
          .from(POLICY_VIOLATION_AGGREGATION)
          .where(whereCondition)
          .groupBy(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD_START)
          .orderBy(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD_START.desc())
          .limit(NUM_PERIODS)
          .fetch();

      LinkedList<AverageMonth> retval = new LinkedList<>();

      for (Record row : results) {
        Date timePeriodStart = row.get(0, Date.class);
        int numNonZeroEvalAggregations = row.get(1, Number.class).intValue();
        double discoveredCountSecurityLowThreat = row.get(2, Number.class).doubleValue();
        double discoveredCountSecurityModerateThreat = row.get(3, Number.class).doubleValue();
        double discoveredCountSecuritySevereThreat = row.get(4, Number.class).doubleValue();
        double discoveredCountSecurityCriticalThreat = row.get(5, Number.class).doubleValue();
        double discoveredCountLicenseLowThreat = row.get(6, Number.class).doubleValue();
        double discoveredCountLicenseModerateThreat = row.get(7, Number.class).doubleValue();
        double discoveredCountLicenseSevereThreat = row.get(8, Number.class).doubleValue();
        double discoveredCountLicenseCriticalThreat = row.get(9, Number.class).doubleValue();
        double discoveredCountQualityLowThreat = row.get(10, Number.class).doubleValue();
        double discoveredCountQualityModerateThreat = row.get(11, Number.class).doubleValue();
        double discoveredCountQualitySevereThreat = row.get(12, Number.class).doubleValue();
        double discoveredCountQualityCriticalThreat = row.get(13, Number.class).doubleValue();
        double discoveredCountOtherLowThreat = row.get(14, Number.class).doubleValue();
        double discoveredCountOtherModerateThreat = row.get(15, Number.class).doubleValue();
        double discoveredCountOtherSevereThreat = row.get(16, Number.class).doubleValue();
        double discoveredCountOtherCriticalThreat = row.get(17, Number.class).doubleValue();
        int evaluationCount = row.get(18, Number.class).intValue();

        AverageMonth averageMonth = new AverageMonth(timePeriodStart,
            new AverageThreatCategoryMonth(divideOrZero(discoveredCountSecurityLowThreat, numNonZeroEvalAggregations),
                divideOrZero(discoveredCountSecurityModerateThreat, numNonZeroEvalAggregations),
                divideOrZero(discoveredCountSecuritySevereThreat, numNonZeroEvalAggregations),
                divideOrZero(discoveredCountSecurityCriticalThreat, numNonZeroEvalAggregations)),
            new AverageThreatCategoryMonth(divideOrZero(discoveredCountLicenseLowThreat, numNonZeroEvalAggregations),
                divideOrZero(discoveredCountLicenseModerateThreat, numNonZeroEvalAggregations),
                divideOrZero(discoveredCountLicenseSevereThreat, numNonZeroEvalAggregations),
                divideOrZero(discoveredCountLicenseCriticalThreat, numNonZeroEvalAggregations)),
            new AverageThreatCategoryMonth(divideOrZero(discoveredCountQualityLowThreat, numNonZeroEvalAggregations),
                divideOrZero(discoveredCountQualityModerateThreat, numNonZeroEvalAggregations),
                divideOrZero(discoveredCountQualitySevereThreat, numNonZeroEvalAggregations),
                divideOrZero(discoveredCountQualityCriticalThreat, numNonZeroEvalAggregations)),
            new AverageThreatCategoryMonth(divideOrZero(discoveredCountOtherLowThreat, numNonZeroEvalAggregations),
                divideOrZero(discoveredCountOtherModerateThreat, numNonZeroEvalAggregations),
                divideOrZero(discoveredCountOtherSevereThreat, numNonZeroEvalAggregations),
                divideOrZero(discoveredCountOtherCriticalThreat, numNonZeroEvalAggregations)),
            evaluationCount);
        // months come out in descending order to make result limiting easier. Add to the front of the retval
        // list in order to reverse order
        retval.push(averageMonth);
      }
      return retval;
    }
  }

  /**
   * @return the number of applications from the specified set which had at least one evaluation in the last 12 months
   */
  public int getActiveApplicationCount(Set<String> applicationIds, boolean includeLatestData) {
    try (TransactionContext tx = createTransactionContext()) {
      Date startDate = getAggregationQueryStartDate(MONTH);

      Condition whereCondition = POLICY_VIOLATION_AGGREGATION.APPLICATION_ID.in(applicationIds)
          .and(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD_START.greaterOrEqual(startDate))
          .and(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD.eq(MONTH.name()))
          .and(POLICY_VIOLATION_AGGREGATION.EVALUATION_COUNT.gt(0));

      if (!includeLatestData) {
        whereCondition = whereCondition.and(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD_END.isNull());
      }

      Record1<Integer> result = tx.dsl()
          .select(DSL.countDistinct(POLICY_VIOLATION_AGGREGATION.APPLICATION_ID))
          .from(POLICY_VIOLATION_AGGREGATION)
          .where(whereCondition)
          .fetchOne();

      return result != null ? result.value1() : 0;
    }
  }

  public static class ApplicationCountsByThreat
  {
    public int countAnyThreat;

    public int countAnyCriticalThreat;

    public int countSecurityThreat;

    public int countSecurityCriticalThreat;

    public int countLicenseThreat;

    public int countLicenseCriticalThreat;

    public int countQualityThreat;

    public int countQualityCriticalThreat;

    public int countOtherThreat;

    public int countOtherCriticalThreat;
  }

  public ApplicationCountsByThreat getApplicationCountsByThreatByApplicationIds(
      Set<String> applicationIds,
      boolean includeLatestData)
  {
    try (TransactionContext tx = createTransactionContext()) {
      Date startDate = getAggregationQueryStartDate(MONTH);

      Condition whereCondition = POLICY_VIOLATION_AGGREGATION.APPLICATION_ID.in(applicationIds)
          .and(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD_START.greaterOrEqual(startDate))
          .and(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD.eq(MONTH.name()));

      if (!includeLatestData) {
        whereCondition = whereCondition.and(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD_END.isNull());
      }

      Result<?> results = tx.dsl()
          .select(
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_SECURITY_LOW_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_SECURITY_MODERATE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_SECURITY_SEVERE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_SECURITY_CRITICAL_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_LICENSE_LOW_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_LICENSE_MODERATE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_LICENSE_SEVERE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_LICENSE_CRITICAL_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_QUALITY_LOW_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_QUALITY_MODERATE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_QUALITY_SEVERE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_QUALITY_CRITICAL_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_OTHER_LOW_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_OTHER_MODERATE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_OTHER_SEVERE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_OTHER_CRITICAL_THREAT))
          .from(POLICY_VIOLATION_AGGREGATION)
          .where(whereCondition)
          .groupBy(POLICY_VIOLATION_AGGREGATION.APPLICATION_ID)
          .fetch();

      ApplicationCountsByThreat applicationCountsByThreat = new ApplicationCountsByThreat();

      for (Record row : results) {
        int securityLowThreat = getIntValue(row.get(0, Number.class));
        int securityModerateThreat = getIntValue(row.get(1, Number.class));
        int securitySevereThreat = getIntValue(row.get(2, Number.class));
        int securityCriticalThreat = getIntValue(row.get(3, Number.class));
        int licenseLowThreat = getIntValue(row.get(4, Number.class));
        int licenseModerateThreat = getIntValue(row.get(5, Number.class));
        int licenseSevereThreat = getIntValue(row.get(6, Number.class));
        int licenseCriticalThreat = getIntValue(row.get(7, Number.class));
        int qualityLowThreat = getIntValue(row.get(8, Number.class));
        int qualityModerateThreat = getIntValue(row.get(9, Number.class));
        int qualitySevereThreat = getIntValue(row.get(10, Number.class));
        int qualityCriticalThreat = getIntValue(row.get(11, Number.class));
        int otherLowThreat = getIntValue(row.get(12, Number.class));
        int otherModerateThreat = getIntValue(row.get(13, Number.class));
        int otherSevereThreat = getIntValue(row.get(14, Number.class));
        int otherCriticalThreat = getIntValue(row.get(15, Number.class));

        int securityThreat = securityLowThreat + securityModerateThreat +
            securitySevereThreat + securityCriticalThreat;

        int licenseThreat = licenseLowThreat + licenseModerateThreat +
            licenseSevereThreat + licenseCriticalThreat;

        int qualityThreat = qualityLowThreat + qualityModerateThreat +
            qualitySevereThreat + qualityCriticalThreat;

        int otherThreat = otherLowThreat + otherModerateThreat +
            otherSevereThreat + otherCriticalThreat;

        int totalThreat = securityThreat + licenseThreat + qualityThreat + otherThreat;

        int totalCriticalThreat = securityCriticalThreat + licenseCriticalThreat +
            qualityCriticalThreat + otherCriticalThreat;

        if (securityThreat > 0) {
          applicationCountsByThreat.countSecurityThreat++;
        }
        if (securityCriticalThreat > 0) {
          applicationCountsByThreat.countSecurityCriticalThreat++;
        }
        if (licenseThreat > 0) {
          applicationCountsByThreat.countLicenseThreat++;
        }
        if (licenseCriticalThreat > 0) {
          applicationCountsByThreat.countLicenseCriticalThreat++;
        }
        if (qualityThreat > 0) {
          applicationCountsByThreat.countQualityThreat++;
        }
        if (qualityCriticalThreat > 0) {
          applicationCountsByThreat.countQualityCriticalThreat++;
        }
        if (otherThreat > 0) {
          applicationCountsByThreat.countOtherThreat++;
        }
        if (otherCriticalThreat > 0) {
          applicationCountsByThreat.countOtherCriticalThreat++;
        }
        if (totalThreat > 0) {
          applicationCountsByThreat.countAnyThreat++;
        }
        if (totalCriticalThreat > 0) {
          applicationCountsByThreat.countAnyCriticalThreat++;
        }
      }

      return applicationCountsByThreat;
    }
  }

  private static int getIntValue(Number number) {
    return number != null ? number.intValue() : 0;
  }

  private Date getAggregationQueryStartDate(TimePeriod timePeriod) {
    return new LocalDate().withField(timePeriod.getDateTimeFieldType(), 1)
        .minus(timePeriod.getPeriod(NUM_PERIODS))
        .toDate();
  }

  private double divideOrZero(double numerator, int denominator) {
    if (denominator == 0) {
      return 0.0;
    }
    return numerator / denominator;
  }

  public void deleteByApplicationId(TransactionContext tx, String applicationId) {
    List<PolicyViolationAggregation> aggregations = getByApplicationId(tx, applicationId);
    for (PolicyViolationAggregation aggregation : aggregations) {
      delete(tx, aggregation);
    }
  }

  List<PolicyViolationAggregation> getByApplicationId(String applicationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationId(tx, applicationId);
    }
  }

  private List<PolicyViolationAggregation> getByApplicationId(TransactionContext tx, String applicationId) {
    return tx.dsl()
        .selectFrom(POLICY_VIOLATION_AGGREGATION)
        .where(POLICY_VIOLATION_AGGREGATION.APPLICATION_ID.eq(applicationId))
        .fetchInto(PolicyViolationAggregation.class);
  }

  public List<ViolationCountPeriod> getViolationCountsByApplicationIds(
      Set<String> applicationIds,
      boolean includeLatestData)
  {
    try (TransactionContext tx = createTransactionContext()) {
      Date startDate = getAggregationQueryStartDate(WEEK);

      Condition whereCondition = POLICY_VIOLATION_AGGREGATION.APPLICATION_ID.in(applicationIds)
          .and(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD_START.greaterOrEqual(startDate))
          .and(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD.eq(WEEK.name()));

      if (!includeLatestData) {
        whereCondition = whereCondition.and(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD_END.isNull());
      }

      Result<Record> results = tx.dsl()
          .select(
              // Discovered counts (indices 0-15)
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_SECURITY_LOW_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_SECURITY_MODERATE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_SECURITY_SEVERE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_SECURITY_CRITICAL_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_LICENSE_LOW_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_LICENSE_MODERATE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_LICENSE_SEVERE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_LICENSE_CRITICAL_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_QUALITY_LOW_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_QUALITY_MODERATE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_QUALITY_SEVERE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_QUALITY_CRITICAL_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_OTHER_LOW_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_OTHER_MODERATE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_OTHER_SEVERE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.DISCOVERED_COUNT_OTHER_CRITICAL_THREAT),
              // Fixed counts (indices 16-31)
              DSL.sum(POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_SECURITY_LOW_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_SECURITY_MODERATE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_SECURITY_SEVERE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_SECURITY_CRITICAL_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_LICENSE_LOW_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_LICENSE_MODERATE_THREAT))
          .select(
              DSL.sum(POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_LICENSE_SEVERE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_LICENSE_CRITICAL_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_QUALITY_LOW_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_QUALITY_MODERATE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_QUALITY_SEVERE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_QUALITY_CRITICAL_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_OTHER_LOW_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_OTHER_MODERATE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_OTHER_SEVERE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.FIXED_COUNT_OTHER_CRITICAL_THREAT),
              // Waived counts (indices 32-47)
              DSL.sum(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_SECURITY_LOW_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_SECURITY_MODERATE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_SECURITY_SEVERE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_SECURITY_CRITICAL_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_LICENSE_LOW_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_LICENSE_MODERATE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_LICENSE_SEVERE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_LICENSE_CRITICAL_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_QUALITY_LOW_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_QUALITY_MODERATE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_QUALITY_SEVERE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_QUALITY_CRITICAL_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_OTHER_LOW_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_OTHER_MODERATE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_OTHER_SEVERE_THREAT),
              DSL.sum(POLICY_VIOLATION_AGGREGATION.WAIVED_COUNT_OTHER_CRITICAL_THREAT),
              // Time period start (index 48)
              POLICY_VIOLATION_AGGREGATION.TIME_PERIOD_START)
          .from(POLICY_VIOLATION_AGGREGATION)
          .where(whereCondition)
          .groupBy(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD_START)
          .orderBy(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD_START.desc())
          .limit(NUM_PERIODS)
          .fetch();

      LinkedList<ViolationCountPeriod> countPeriods = new LinkedList<>();

      for (Record period : results) {
        Object[] periodArray = period.intoArray();
        Date timePeriodStart = (Date) periodArray[48];
        ViolationCountPeriod countPeriod = new ViolationCountPeriod(timePeriodStart, getDiscoveredCounts(periodArray),
            getFixedCounts(periodArray), getWaivedCounts(periodArray));
        countPeriods.push(countPeriod);
      }

      return countPeriods;
    }
  }

  private Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> getDiscoveredCounts(Object[] period) {
    return getCounts(period, 0);
  }

  private Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> getFixedCounts(Object[] period) {
    return getCounts(period, 16);
  }

  private Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> getWaivedCounts(Object[] period) {
    return getCounts(period, 32);
  }

  private Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> getCounts(Object[] period, int countTypeOffset) {
    Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> result = allZeroCounts();
    for (int categoryIndex = 0; categoryIndex < 4; categoryIndex++) {
      for (int threatLevelIndex = 0; threatLevelIndex < 4; threatLevelIndex++) {
        PolicyThreatCategory category = PolicyThreatCategory.values()[categoryIndex];
        ThreatLevel level = ThreatLevel.values()[threatLevelIndex];
        Number countNumber = (Number) period[4 * categoryIndex + threatLevelIndex + countTypeOffset];
        int count = countNumber != null ? countNumber.intValue() : 0;
        result.get(category).put(level, count);
      }
    }
    return result;
  }

  private Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> allZeroCounts() {
    EnumMap<PolicyThreatCategory, Map<ThreatLevel, Integer>> result = new EnumMap<>(PolicyThreatCategory.class);
    for (PolicyThreatCategory category : PolicyThreatCategory.values()) {
      result.put(category, new EnumMap<>(ThreatLevel.class));
      for (ThreatLevel level : ThreatLevel.values()) {
        result.get(category).put(level, 0);
      }
    }
    return result;
  }

  public LinkedList<OpenViolationCountsWeek> getOpenViolationsCountsByApplicationIds(
      Set<String> applicationIds,
      boolean includeLatestData)
  {
    try (TransactionContext tx = createTransactionContext()) {
      Date startDate = getAggregationQueryStartDate(WEEK);

      Condition whereCondition = POLICY_VIOLATION_AGGREGATION.APPLICATION_ID.in(applicationIds)
          .and(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD_START.greaterOrEqual(startDate))
          .and(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD.eq(WEEK.name()));

      if (!includeLatestData) {
        whereCondition = whereCondition.and(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD_END.isNull());
      }

      var securityTotal = DSL.sum(POLICY_VIOLATION_AGGREGATION.OPEN_COUNT_SECURITY_LOW_THREAT)
          .plus(DSL.sum(POLICY_VIOLATION_AGGREGATION.OPEN_COUNT_SECURITY_MODERATE_THREAT))
          .plus(DSL.sum(POLICY_VIOLATION_AGGREGATION.OPEN_COUNT_SECURITY_SEVERE_THREAT))
          .plus(DSL.sum(POLICY_VIOLATION_AGGREGATION.OPEN_COUNT_SECURITY_CRITICAL_THREAT));

      var licenseTotal = DSL.sum(POLICY_VIOLATION_AGGREGATION.OPEN_COUNT_LICENSE_LOW_THREAT)
          .plus(DSL.sum(POLICY_VIOLATION_AGGREGATION.OPEN_COUNT_LICENSE_MODERATE_THREAT))
          .plus(DSL.sum(POLICY_VIOLATION_AGGREGATION.OPEN_COUNT_LICENSE_SEVERE_THREAT))
          .plus(DSL.sum(POLICY_VIOLATION_AGGREGATION.OPEN_COUNT_LICENSE_CRITICAL_THREAT));

      var qualityTotal = DSL.sum(POLICY_VIOLATION_AGGREGATION.OPEN_COUNT_QUALITY_LOW_THREAT)
          .plus(DSL.sum(POLICY_VIOLATION_AGGREGATION.OPEN_COUNT_QUALITY_MODERATE_THREAT))
          .plus(DSL.sum(POLICY_VIOLATION_AGGREGATION.OPEN_COUNT_QUALITY_SEVERE_THREAT))
          .plus(DSL.sum(POLICY_VIOLATION_AGGREGATION.OPEN_COUNT_QUALITY_CRITICAL_THREAT));

      var otherTotal = DSL.sum(POLICY_VIOLATION_AGGREGATION.OPEN_COUNT_OTHER_LOW_THREAT)
          .plus(DSL.sum(POLICY_VIOLATION_AGGREGATION.OPEN_COUNT_OTHER_MODERATE_THREAT))
          .plus(DSL.sum(POLICY_VIOLATION_AGGREGATION.OPEN_COUNT_OTHER_SEVERE_THREAT))
          .plus(DSL.sum(POLICY_VIOLATION_AGGREGATION.OPEN_COUNT_OTHER_CRITICAL_THREAT));

      Result<?> results = tx.dsl()
          .select(securityTotal, licenseTotal, qualityTotal, otherTotal,
              POLICY_VIOLATION_AGGREGATION.TIME_PERIOD_START)
          .from(POLICY_VIOLATION_AGGREGATION)
          .where(whereCondition)
          .groupBy(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD_START)
          .orderBy(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD_START.desc())
          .limit(NUM_PERIODS)
          .fetch();

      final LinkedList<OpenViolationCountsWeek> openViolationCountsWeeks = new LinkedList<>();

      for (Record period : results) {
        Map<PolicyThreatCategory, Integer> violationTotalsWeek = new EnumMap<>(PolicyThreatCategory.class);
        violationTotalsWeek.put(SECURITY, getIntValue(period.get(0, Number.class)));
        violationTotalsWeek.put(LICENSE, getIntValue(period.get(1, Number.class)));
        violationTotalsWeek.put(QUALITY, getIntValue(period.get(2, Number.class)));
        violationTotalsWeek.put(OTHER, getIntValue(period.get(3, Number.class)));
        Date timePeriodStart = period.get(4, Date.class);
        openViolationCountsWeeks.push(new OpenViolationCountsWeek(timePeriodStart, violationTotalsWeek));
      }

      return openViolationCountsWeeks;
    }
  }

  /**
   * Return all PolicyViolationAggregations for the given applications in the given time range. The returned list is
   * sorted by applicationId and then by timePeriodStart.
   *
   * @param applicationIds The ids of the applications to look up
   * @param timePeriod the TimePeriod (e.g. WEEKLY or MONTHLY)
   * @param startDate the earliest timePeriodStart value to look up
   * @param endDate the latest timePeriodStart to look up (exclusive). If null, all aggregations after
   *          startDate, including the current partial aggregation, are included
   */
  public List<PolicyViolationAggregation> getByApplicationIdsAndTimePeriodBounds(
      Set<String> applicationIds,
      TimePeriod timePeriod,
      Date startDate,
      Date endDate)
  {
    try (TransactionContext tx = createTransactionContext()) {
      Condition whereCondition = POLICY_VIOLATION_AGGREGATION.APPLICATION_ID.in(applicationIds)
          .and(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD.eq(timePeriod.name()))
          .and(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD_START.greaterOrEqual(startDate));

      if (endDate != null) {
        whereCondition = whereCondition.and(POLICY_VIOLATION_AGGREGATION.TIME_PERIOD_START.lessThan(endDate));
      }

      return tx.dsl()
          .selectFrom(POLICY_VIOLATION_AGGREGATION)
          .where(whereCondition)
          .orderBy(POLICY_VIOLATION_AGGREGATION.APPLICATION_ID.asc(),
              POLICY_VIOLATION_AGGREGATION.TIME_PERIOD_START.asc())
          .fetchInto(PolicyViolationAggregation.class);
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return POLICY_VIOLATION_AGGREGATION;
  }

  @Override
  public Class<PolicyViolationAggregation> getEntityClass() {
    return PolicyViolationAggregation.class;
  }
}
