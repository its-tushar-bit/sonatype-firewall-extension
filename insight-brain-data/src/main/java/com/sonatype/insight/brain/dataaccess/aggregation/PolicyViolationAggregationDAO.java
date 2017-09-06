/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.aggregation;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractAggregationSqlDAO;
import com.sonatype.insight.brain.model.aggregation.PolicyViolationAggregation;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.joda.time.LocalDate;

/**
 * @since 1.31
 */
public class PolicyViolationAggregationDAO
    extends AbstractAggregationSqlDAO<PolicyViolationAggregation>
{
  private static final int NUM_MONTHS = 12;

  @Override
  public PolicyViolationAggregation getById(String id) {
    String sQuery = "SELECT entity FROM PolicyViolationAggregation entity WHERE entity.id = ?1";
    return get(sQuery, id);
  }

  public PolicyViolationAggregation getMostRecentByApplicationId(String applicationId) {
    String sQuery = "SELECT entity FROM PolicyViolationAggregation entity" + //
        " WHERE entity.applicationId = ?1" + //
        " ORDER BY entity.timePeriodStart DESC";

    return createQuery(sQuery, applicationId).forceSingleResult().get();
  }

  /**
   * Delete partial aggregations generated while in PoC mode.
   *
   * @return Number of deleted aggregations.
   * @since 1.36
   */
  public int deletePartialMonthsUpTo(String applicationId, LocalDate timePeriodEnd) {
    String sQuery = "SELECT entity FROM PolicyViolationAggregation entity" + //
        " WHERE entity.applicationId = ?1 AND entity.timePeriodEnd < ?2";
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      List<PolicyViolationAggregation> partialMonths = getList(tx, sQuery, applicationId, timePeriodEnd.toDate());
      for (PolicyViolationAggregation partialMonth : partialMonths) {
        delete(tx, partialMonth);
      }
      tx.commit();
      return partialMonths.size();
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
  }

  /**
   * Get MTTR monthly averages for the specified applications, for the 12 most recent months
   */
  public List<MttrMonth> getMttrMonthlyAverages(Set<String> applicationIds) {
    // compute an overall average MTTR for these applications for each month
    String sQuery = "SELECT agg.timePeriodStart, " + //
        " SUM(agg.mttrLowThreat * agg.resolvedCountLowThreat) / SUM(agg.resolvedCountLowThreat)," + //
        " SUM(agg.mttrModerateThreat * agg.resolvedCountModerateThreat) / SUM(agg.resolvedCountModerateThreat)," + //
        " SUM(agg.mttrSevereThreat * agg.resolvedCountSevereThreat) / SUM(agg.resolvedCountSevereThreat)," + //
        " SUM(agg.mttrCriticalThreat * agg.resolvedCountCriticalThreat) / SUM(agg.resolvedCountCriticalThreat)," + //
        " SUM(agg.resolvedCountLowThreat), SUM(agg.resolvedCountModerateThreat), " + //
        " SUM(agg.resolvedCountSevereThreat), SUM(agg.resolvedCountCriticalThreat)" + //
        " FROM PolicyViolationAggregation agg" + //
        " WHERE agg.applicationId IN (?1)" + //
        " GROUP BY agg.timePeriodStart" + //
        " ORDER BY agg.timePeriodStart DESC";

    try (TransactionContext tx = createTransactionContext()) {
      javax.persistence.Query query = tx.createQuery(sQuery);
      query.setParameter(1, applicationIds);
      query.setMaxResults(NUM_MONTHS);

      @SuppressWarnings("unchecked")
      List<Object[]> results = query.getResultList();
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

        MttrMonth mttrMonth = new MttrMonth((Date) row[0], mttrLowThreat == null ? null : mttrLowThreat.longValue(),
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
  }

  /**
   * Get discovered violations monthly averages for the specified applications, for the 12 most recent months
   */
  public List<AverageMonth> getMonthlyAverages(Set<String> applicationIds) {
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
        " GROUP BY agg.timePeriodStart" + //
        " ORDER BY agg.timePeriodStart DESC";

    try (TransactionContext tx = createTransactionContext()) {
      javax.persistence.Query query = tx.createQuery(sQuery);
      query.setParameter(1, applicationIds);
      query.setMaxResults(NUM_MONTHS);

      @SuppressWarnings("unchecked")
      List<Object[]> results = query.getResultList();
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
  }

  /**
   * @return the number of applications from the specified set which had at least one evaluation in the last 12 months
   */
  public int getActiveApplicationCount(Set<String> applicationIds) {
    String sQuery = "SELECT COUNT(DISTINCT agg.applicationId)" + //
        " FROM PolicyViolationAggregation agg" + //
        " WHERE agg.applicationId IN (?1) AND agg.timePeriodStart >= ?2 AND agg.evaluationCount > 0";

    return getSingle(Number.class, sQuery, applicationIds, getAggregationQueryStartDate()).intValue();
  }

  public static class ApplicationCountsByThreat
  {
    public int countAnyThreat = 0;
    public int countAnyCriticalThreat = 0;

    public int countSecurityThreat = 0;
    public int countSecurityCriticalThreat = 0;

    public int countLicenseThreat = 0;
    public int countLicenseCriticalThreat = 0;

    public int countQualityThreat = 0;
    public int countQualityCriticalThreat = 0;

    public int countOtherThreat = 0;
    public int countOtherCriticalThreat = 0;
  }

  public ApplicationCountsByThreat getApplicationCountsByThreatByApplicationIds(Set<String> applicationIds) {
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
        " GROUP BY agg.applicationId";

    try (TransactionContext tx = createTransactionContext()) {
      javax.persistence.Query query = tx.createQuery(sQuery);
      query.setParameter(1, applicationIds);
      query.setParameter(2, getAggregationQueryStartDate());

      @SuppressWarnings("unchecked")
      List<Object[]> results = query.getResultList();

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
  }

  private Date getAggregationQueryStartDate() {
    return new LocalDate().withDayOfMonth(1).minusMonths(NUM_MONTHS).toDate();
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
}
