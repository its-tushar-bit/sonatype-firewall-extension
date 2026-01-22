/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.successmetrics;

import java.util.Date;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractAggregationSqlDAO;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.successmetrics.PolicyViolationAggregation;
import com.sonatype.insight.brain.model.successmetrics.TimePeriod;
import com.sonatype.insight.brain.utils.ThreatLevel;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.annotations.VisibleForTesting;
import org.joda.time.LocalDate;

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

  @Override
  public PolicyViolationAggregation getById(String id) {
    String sQuery = "SELECT entity FROM PolicyViolationAggregation entity WHERE entity.id = ?1";
    return get(sQuery, id);
  }

  @VisibleForTesting
  public List<PolicyViolationAggregation> getByTimePeriod(TimePeriod timePeriod) {
    String sQuery = "SELECT entity FROM PolicyViolationAggregation entity WHERE entity.timePeriod = ?1";
    return getList(sQuery, timePeriod);
  }

  public PolicyViolationAggregation getMostRecentByApplicationIdAndTimePeriod(String applicationId,
                                                                              TimePeriod timePeriod)
  {
    String sQuery = "SELECT entity FROM PolicyViolationAggregation entity" + //
        " WHERE entity.applicationId = ?1" + //
        " AND entity.timePeriod = ?2" + //
        " ORDER BY entity.timePeriodStart DESC";

    return createQuery(sQuery, applicationId, timePeriod).forceSingleResult().get();
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

    MttrMonth(Date monthStart,
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

    public AverageMonth(Date timePeriodStart,
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

    public AverageThreatCategoryMonth(double averageDiscoveredLowThreat,
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

    public ViolationCountPeriod(Date periodStart,
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
    // compute an overall average MTTR for these applications for each month
    String aggResolvedCountTemplate = "(agg.fixedCountSecurity%1$sThreat + agg.fixedCountLicense%1$sThreat" +
        " + agg.fixedCountQuality%1$sThreat + agg.fixedCountOther%1$sThreat + agg.waivedCountSecurity%1$sThreat" +
        " + agg.waivedCountLicense%1$sThreat + agg.waivedCountQuality%1$sThreat + agg.waivedCountOther%1$sThreat)";
    String resolvedCountLow = String.format(aggResolvedCountTemplate, "Low");
    String resolvedCountModerate = String.format(aggResolvedCountTemplate, "Moderate");
    String resolvedCountSevere = String.format(aggResolvedCountTemplate, "Severe");
    String resolvedCountCritical = String.format(aggResolvedCountTemplate, "Critical");

    String sQuery = "SELECT agg.timePeriodStart, " + //
        " SUM(agg.mttrLowThreat * " + resolvedCountLow + ") / SUM(" + resolvedCountLow + ")," + //
        " SUM(agg.mttrModerateThreat * " + resolvedCountModerate + ") / SUM(" + resolvedCountModerate + ")," + //
        " SUM(agg.mttrSevereThreat * " + resolvedCountSevere + ") / SUM(" + resolvedCountSevere + ")," + //
        " SUM(agg.mttrCriticalThreat * " + resolvedCountCritical + ") / SUM(" + resolvedCountCritical + ")," + //
        " SUM(" + resolvedCountLow + "), SUM(" + resolvedCountModerate + "), " + //
        " SUM(" + resolvedCountSevere + "), SUM(" + resolvedCountCritical + ")" + //
        " FROM PolicyViolationAggregation agg" + //
        " WHERE agg.applicationId IN (?1)" + //
        " AND agg.timePeriod = ?2" + //
        (includeLatestData ? "" : " AND agg.timePeriodEnd IS NULL") + //
        " GROUP BY agg.timePeriodStart" + //
        " ORDER BY agg.timePeriodStart DESC";

    List<Object[]> results = new Query<Object[]>(sQuery, applicationIds, MONTH).setMaxResults(NUM_PERIODS).getList();
    LinkedList<MttrMonth> retval = new LinkedList<>();

    for (Object[] row : results) {
      Number mttrLowThreat = (Number) row[1];
      Number mttrModerateThreat = (Number) row[2];
      Number mttrSevereThreat = (Number) row[3];
      Number mttrCriticalThreat = (Number) row[4];
      Number resolvedCountLowThreat = (Number) row[5];
      Number resolvedCountModerateThreat = (Number) row[6];
      Number resolvedCountSevereThreat = (Number) row[7];
      Number resolvedCountCriticalThreat = (Number) row[8];

      MttrMonth mttrMonth = new MttrMonth((Date) row[0],
          mttrLowThreat == null ? null : mttrLowThreat.longValue(),
          mttrModerateThreat == null ? null : mttrModerateThreat.longValue(),
          mttrSevereThreat == null ? null : mttrSevereThreat.longValue(),
          mttrCriticalThreat == null ? null : mttrCriticalThreat.longValue(), resolvedCountLowThreat.intValue(),
          resolvedCountModerateThreat.intValue(), resolvedCountSevereThreat.intValue(),
          resolvedCountCriticalThreat.intValue());

      // months come out in descending order to make result limiting easier. Add to the front of the retval
      // list in order to reverse order
      retval.push(mttrMonth);
    }

    return retval;
  }

  /**
   * Get discovered violations monthly averages for the specified applications, for the 12 most recent months
   */
  public List<AverageMonth> getMonthlyAverages(Set<String> applicationIds, boolean includeLatestData) {
    // compute an overall average discovered for these applications for each month
    String sQuery = "SELECT agg.timePeriodStart, " + //
        " SUM(CASE WHEN agg.evaluationCount > 0 THEN 1 ELSE 0 END)," + //
        " SUM(agg.discoveredCountSecurityLowThreat)," + //
        " SUM(agg.discoveredCountSecurityModerateThreat)," + //
        " SUM(agg.discoveredCountSecuritySevereThreat)," + //
        " SUM(agg.discoveredCountSecurityCriticalThreat)," + //
        " SUM(agg.discoveredCountLicenseLowThreat)," + //
        " SUM(agg.discoveredCountLicenseModerateThreat)," + //
        " SUM(agg.discoveredCountLicenseSevereThreat)," + //
        " SUM(agg.discoveredCountLicenseCriticalThreat)," + //
        " SUM(agg.discoveredCountQualityLowThreat)," + //
        " SUM(agg.discoveredCountQualityModerateThreat)," + //
        " SUM(agg.discoveredCountQualitySevereThreat)," + //
        " SUM(agg.discoveredCountQualityCriticalThreat)," + //
        " SUM(agg.discoveredCountOtherLowThreat)," + //
        " SUM(agg.discoveredCountOtherModerateThreat)," + //
        " SUM(agg.discoveredCountOtherSevereThreat)," + //
        " SUM(agg.discoveredCountOtherCriticalThreat)," + //
        " SUM(agg.evaluationCount)" + //
        " FROM PolicyViolationAggregation agg" + //
        " WHERE agg.applicationId IN (?1)" + //
        " AND agg.timePeriod = ?2" + //
        (includeLatestData ? "" : " AND agg.timePeriodEnd IS NULL") + //
        " GROUP BY agg.timePeriodStart" + //
        " ORDER BY agg.timePeriodStart DESC";

    List<Object[]> results = new Query<Object[]>(sQuery, applicationIds, MONTH).setMaxResults(NUM_PERIODS).getList();
    LinkedList<AverageMonth> retval = new LinkedList<>();

    for (Object[] row : results) {
      int numNonZeroEvalAggregations = ((Number) row[1]).intValue();
      double discoveredCountSecurityLowThreat = ((Number) row[2]).doubleValue();
      double discoveredCountSecurityModerateThreat = ((Number) row[3]).doubleValue();
      double discoveredCountSecuritySevereThreat = ((Number) row[4]).doubleValue();
      double discoveredCountSecurityCriticalThreat = ((Number) row[5]).doubleValue();
      double discoveredCountLicenseLowThreat = ((Number) row[6]).doubleValue();
      double discoveredCountLicenseModerateThreat = ((Number) row[7]).doubleValue();
      double discoveredCountLicenseSevereThreat = ((Number) row[8]).doubleValue();
      double discoveredCountLicenseCriticalThreat = ((Number) row[9]).doubleValue();
      double discoveredCountQualityLowThreat = ((Number) row[10]).doubleValue();
      double discoveredCountQualityModerateThreat = ((Number) row[11]).doubleValue();
      double discoveredCountQualitySevereThreat = ((Number) row[12]).doubleValue();
      double discoveredCountQualityCriticalThreat = ((Number) row[13]).doubleValue();
      double discoveredCountOtherLowThreat = ((Number) row[14]).doubleValue();
      double discoveredCountOtherModerateThreat = ((Number) row[15]).doubleValue();
      double discoveredCountOtherSevereThreat = ((Number) row[16]).doubleValue();
      double discoveredCountOtherCriticalThreat = ((Number) row[17]).doubleValue();
      int evaluationCount = ((Number) row[18]).intValue();

      AverageMonth averageMonth = new AverageMonth((Date) row[0],
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

  /**
   * @return the number of applications from the specified set which had at least one evaluation in the last 12 months
   */
  public int getActiveApplicationCount(Set<String> applicationIds, boolean includeLatestData) {
    String sQuery = "SELECT COUNT(DISTINCT agg.applicationId)" + //
        " FROM PolicyViolationAggregation agg" + //
        " WHERE agg.applicationId IN (?1)" +
        " AND agg.timePeriodStart >= ?2" +
        " AND agg.timePeriod = ?3" + //
        (includeLatestData ? "" : " AND agg.timePeriodEnd IS NULL") + //
        " AND agg.evaluationCount > 0";

    return getSingle(Number.class, sQuery, applicationIds, getAggregationQueryStartDate(MONTH), MONTH).intValue();
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

  public ApplicationCountsByThreat getApplicationCountsByThreatByApplicationIds(Set<String> applicationIds,
                                                                                boolean includeLatestData)
  {
    // query that returns the summed discovered violation counts across the past
    // year for a given app in each row
    String sQuery = "SELECT" + //
        "  SUM(agg.discoveredCountSecurityLowThreat)," + //
        "  SUM(agg.discoveredCountSecurityModerateThreat)," + //
        "  SUM(agg.discoveredCountSecuritySevereThreat)," + //
        "  SUM(agg.discoveredCountSecurityCriticalThreat)," + //
        "  SUM(agg.discoveredCountLicenseLowThreat)," + //
        "  SUM(agg.discoveredCountLicenseModerateThreat)," + //
        "  SUM(agg.discoveredCountLicenseSevereThreat)," + //
        "  SUM(agg.discoveredCountLicenseCriticalThreat)," + //
        "  SUM(agg.discoveredCountQualityLowThreat)," + //
        "  SUM(agg.discoveredCountQualityModerateThreat)," + //
        "  SUM(agg.discoveredCountQualitySevereThreat)," + //
        "  SUM(agg.discoveredCountQualityCriticalThreat)," + //
        "  SUM(agg.discoveredCountOtherLowThreat)," + //
        "  SUM(agg.discoveredCountOtherModerateThreat)," + //
        "  SUM(agg.discoveredCountOtherSevereThreat)," + //
        "  SUM(agg.discoveredCountOtherCriticalThreat)" + //
        " FROM PolicyViolationAggregation agg" + //
        " WHERE agg.applicationId IN (?1) AND agg.timePeriodStart >= ?2" + //
        " AND agg.timePeriod = ?3" + //
        (includeLatestData ? "" : " AND agg.timePeriodEnd IS NULL") + //
        " GROUP BY agg.applicationId";

    List<Object[]> results =
        new Query<Object[]>(sQuery, applicationIds, getAggregationQueryStartDate(MONTH), MONTH).getList();

    ApplicationCountsByThreat applicationCountsByThreat = new ApplicationCountsByThreat();

    for (Object[] row : results) {
      int securityLowThreat = ((Number) row[0]).intValue();
      int securityModerateThreat = ((Number) row[1]).intValue();
      int securitySevereThreat = ((Number) row[2]).intValue();
      int securityCriticalThreat = ((Number) row[3]).intValue();
      int licenseLowThreat = ((Number) row[4]).intValue();
      int licenseModerateThreat = ((Number) row[5]).intValue();
      int licenseSevereThreat = ((Number) row[6]).intValue();
      int licenseCriticalThreat = ((Number) row[7]).intValue();
      int qualityLowThreat = ((Number) row[8]).intValue();
      int qualityModerateThreat = ((Number) row[9]).intValue();
      int qualitySevereThreat = ((Number) row[10]).intValue();
      int qualityCriticalThreat = ((Number) row[11]).intValue();
      int otherLowThreat = ((Number) row[12]).intValue();
      int otherModerateThreat = ((Number) row[13]).intValue();
      int otherSevereThreat = ((Number) row[14]).intValue();
      int otherCriticalThreat = ((Number) row[15]).intValue();

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

  private Date getAggregationQueryStartDate(TimePeriod timePeriod) {
    return new LocalDate().withField(timePeriod.getDateTimeFieldType(), 1).minus(timePeriod.getPeriod(NUM_PERIODS))
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
    String sQuery = "SELECT entity FROM PolicyViolationAggregation entity WHERE entity.applicationId = ?1";
    return getList(tx, sQuery, applicationId);
  }

  public List<ViolationCountPeriod> getViolationCountsByApplicationIds(Set<String> applicationIds,
                                                                       boolean includeLatestData)
  {
    String sQuery = "SELECT" + //
        "  SUM(agg.discoveredCountSecurityLowThreat)," + //
        "  SUM(agg.discoveredCountSecurityModerateThreat)," + //
        "  SUM(agg.discoveredCountSecuritySevereThreat)," + //
        "  SUM(agg.discoveredCountSecurityCriticalThreat)," + //
        "  SUM(agg.discoveredCountLicenseLowThreat)," + //
        "  SUM(agg.discoveredCountLicenseModerateThreat)," + //
        "  SUM(agg.discoveredCountLicenseSevereThreat)," + //
        "  SUM(agg.discoveredCountLicenseCriticalThreat)," + //
        "  SUM(agg.discoveredCountQualityLowThreat)," + //
        "  SUM(agg.discoveredCountQualityModerateThreat)," + //
        "  SUM(agg.discoveredCountQualitySevereThreat)," + //
        "  SUM(agg.discoveredCountQualityCriticalThreat)," + //
        "  SUM(agg.discoveredCountOtherLowThreat)," + //
        "  SUM(agg.discoveredCountOtherModerateThreat)," + //
        "  SUM(agg.discoveredCountOtherSevereThreat)," + //
        "  SUM(agg.discoveredCountOtherCriticalThreat), " + //
        "  SUM(agg.fixedCountSecurityLowThreat)," + //
        "  SUM(agg.fixedCountSecurityModerateThreat)," + //
        "  SUM(agg.fixedCountSecuritySevereThreat)," + //
        "  SUM(agg.fixedCountSecurityCriticalThreat)," + //
        "  SUM(agg.fixedCountLicenseLowThreat)," + //
        "  SUM(agg.fixedCountLicenseModerateThreat)," + //
        "  SUM(agg.fixedCountLicenseSevereThreat)," + //
        "  SUM(agg.fixedCountLicenseCriticalThreat)," + //
        "  SUM(agg.fixedCountQualityLowThreat)," + //
        "  SUM(agg.fixedCountQualityModerateThreat)," + //
        "  SUM(agg.fixedCountQualitySevereThreat)," + //
        "  SUM(agg.fixedCountQualityCriticalThreat)," + //
        "  SUM(agg.fixedCountOtherLowThreat)," + //
        "  SUM(agg.fixedCountOtherModerateThreat)," + //
        "  SUM(agg.fixedCountOtherSevereThreat)," + //
        "  SUM(agg.fixedCountOtherCriticalThreat), " + //
        "  SUM(agg.waivedCountSecurityLowThreat)," + //
        "  SUM(agg.waivedCountSecurityModerateThreat)," + //
        "  SUM(agg.waivedCountSecuritySevereThreat)," + //
        "  SUM(agg.waivedCountSecurityCriticalThreat)," + //
        "  SUM(agg.waivedCountLicenseLowThreat)," + //
        "  SUM(agg.waivedCountLicenseModerateThreat)," + //
        "  SUM(agg.waivedCountLicenseSevereThreat)," + //
        "  SUM(agg.waivedCountLicenseCriticalThreat)," + //
        "  SUM(agg.waivedCountQualityLowThreat)," + //
        "  SUM(agg.waivedCountQualityModerateThreat)," + //
        "  SUM(agg.waivedCountQualitySevereThreat)," + //
        "  SUM(agg.waivedCountQualityCriticalThreat)," + //
        "  SUM(agg.waivedCountOtherLowThreat)," + //
        "  SUM(agg.waivedCountOtherModerateThreat)," + //
        "  SUM(agg.waivedCountOtherSevereThreat)," + //
        "  SUM(agg.waivedCountOtherCriticalThreat), " + //
        "  agg.timePeriodStart" + //
        " FROM PolicyViolationAggregation agg" + //
        " WHERE agg.applicationId IN (?1)" + //
        "  AND agg.timePeriodStart >= ?2" + //
        "  AND agg.timePeriod = ?3" + //
        (includeLatestData ? "" : " AND agg.timePeriodEnd IS NULL") + //
        " GROUP BY agg.timePeriodStart" + //
        " ORDER BY agg.timePeriodStart DESC";

    LinkedList<ViolationCountPeriod> countPeriods = new LinkedList<>();

    List<Object[]> periods = new Query<Object[]>(sQuery, applicationIds, getAggregationQueryStartDate(WEEK), WEEK)
        .setMaxResults(NUM_PERIODS).getList();

    for (Object[] period : periods) {
      ViolationCountPeriod countPeriod = new ViolationCountPeriod((Date) period[48], getDiscoveredCounts(period),
          getFixedCounts(period), getWaivedCounts(period));
      countPeriods.push(countPeriod);
    }

    return countPeriods;
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
        int count = ((Number) period[4 * categoryIndex + threatLevelIndex + countTypeOffset]).intValue();
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

  public LinkedList<OpenViolationCountsWeek> getOpenViolationsCountsByApplicationIds(Set<String> applicationIds,
                                                                                     boolean includeLatestData)
  {
    String sQuery = "SELECT" + //
        "  SUM(agg.openCountSecurityLowThreat) + SUM(agg.openCountSecurityModerateThreat) + " + //
        "    SUM(agg.openCountSecuritySevereThreat) + SUM(agg.openCountSecurityCriticalThreat)," + //
        "  SUM(agg.openCountLicenseLowThreat) + SUM(agg.openCountLicenseModerateThreat) +" + //
        "    SUM(agg.openCountLicenseSevereThreat) + SUM(agg.openCountLicenseCriticalThreat)," + //
        "  SUM(agg.openCountQualityLowThreat) + SUM(agg.openCountQualityModerateThreat) +" + //
        "    SUM(agg.openCountQualitySevereThreat) + SUM(agg.openCountQualityCriticalThreat)," + //
        "  SUM(agg.openCountOtherLowThreat) + SUM(agg.openCountOtherModerateThreat) +" + //
        "    SUM(agg.openCountOtherSevereThreat) + SUM(agg.openCountOtherCriticalThreat), " + //
        "  agg.timePeriodStart" + //
        " FROM PolicyViolationAggregation agg" + //
        " WHERE agg.applicationId IN (?1)" + //
        "  AND agg.timePeriodStart >= ?2" + //
        "  AND agg.timePeriod = ?3" + //
        (includeLatestData ? "" : " AND agg.timePeriodEnd IS NULL") + //
        " GROUP BY agg.timePeriodStart" + //
        " ORDER BY agg.timePeriodStart DESC";

    final LinkedList<OpenViolationCountsWeek> openViolationCountsWeeks = new LinkedList<>();

    List<Object[]> periods = new Query<Object[]>(sQuery, applicationIds, getAggregationQueryStartDate(WEEK), WEEK)
        .setMaxResults(NUM_PERIODS).getList();

    for (Object[] period : periods) {
      Map<PolicyThreatCategory, Integer> violationTotalsWeek = new EnumMap<>(PolicyThreatCategory.class);
      violationTotalsWeek.put(SECURITY, ((Number) period[0]).intValue());
      violationTotalsWeek.put(LICENSE, ((Number) period[1]).intValue());
      violationTotalsWeek.put(QUALITY, ((Number) period[2]).intValue());
      violationTotalsWeek.put(OTHER, ((Number) period[3]).intValue());
      openViolationCountsWeeks.push(new OpenViolationCountsWeek((Date) period[4], violationTotalsWeek));
    }

    return openViolationCountsWeeks;
  }

  /**
   * Return all PolicyViolationAggregations for the given applications in the given time range. The returned list
   * is sorted by applicationId and then by timePeriodStart.
   *
   * @param applicationIds The ids of the applications to look up
   * @param timePeriod the TimePeriod (e.g. WEEKLY or MONTHLY)
   * @param startDate the earliest timePeriodStart value to look up
   * @param endDate the latest timePeriodStart to look up (exclusive).  If null, all aggregations after startDate,
   * including the current partial aggregation, are included
   */
  public List<PolicyViolationAggregation> getByApplicationIdsAndTimePeriodBounds(Set<String> applicationIds,
                                                                                 TimePeriod timePeriod,
                                                                                 Date startDate,
                                                                                 Date endDate)
  {
    String sQuery = "SELECT entity FROM PolicyViolationAggregation entity" +
        " WHERE entity.applicationId IN (?1)" +
        "  AND entity.timePeriod = ?2" +
        "  AND entity.timePeriodStart >= ?3" +
        (endDate != null ? " AND entity.timePeriodStart < ?4" : "") +
        " ORDER BY entity.applicationId ASC, entity.timePeriodStart ASC";

    if (endDate == null) {
      return getList(sQuery, applicationIds, timePeriod, startDate);
    }
    else {
      return getList(sQuery, applicationIds, timePeriod, startDate, endDate);
    }
  }

  public List<PolicyViolationAggregation> getAll() {
    String sQuery = "SELECT entity FROM PolicyViolationAggregation entity";
    return getList(sQuery);
  }
}
