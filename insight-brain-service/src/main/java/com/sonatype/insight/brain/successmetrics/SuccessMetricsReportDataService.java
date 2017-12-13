/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.successmetrics.SuccessMetricsReportDataDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO.ApplicationCountsByThreat;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO.AverageMonth;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO.MttrMonth;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReport;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReportData;
import com.sonatype.insight.brain.organization.ApplicationService;

import com.google.common.annotations.VisibleForTesting;
import org.joda.time.DateTime;
import org.joda.time.YearMonth;

import static com.sonatype.insight.brain.successmetrics.ApplicationCountsDTO.ThreatCategoryApplicationCount;
import static com.sonatype.insight.brain.successmetrics.AverageDiscoveredPolicyViolationsDTO.ThreatCategoryPolicyViolationsDTO;

/**
 * @since 1.39
 */
@Named
@Singleton
public class SuccessMetricsReportDataService
{
  private final ApplicationService applicationService;

  private final PolicyViolationAggregationService policyViolationAggregationService;

  private final SuccessMetricsReportService successMetricsReportService;

  private final SuccessMetricsReportDataDAO successMetricsReportDataDAO;

  private final PolicyViolationAggregationDAO violationAggregationDAO;

  @Inject
  public SuccessMetricsReportDataService(ApplicationService applicationService,
                                         PolicyViolationAggregationService policyViolationAggregationService,
                                         SuccessMetricsReportService successMetricsReportService,
                                         SuccessMetricsReportDataDAO successMetricsReportDataDAO,
                                         PolicyViolationAggregationDAO violationAggregationDAO)
  {
    this.applicationService = applicationService;
    this.policyViolationAggregationService = policyViolationAggregationService;
    this.successMetricsReportService = successMetricsReportService;
    this.successMetricsReportDataDAO = successMetricsReportDataDAO;
    this.violationAggregationDAO = violationAggregationDAO;
  }

  /**
   * @since 1.39
   */
  SuccessMetricsChartDataDTO getChartData(String successMetricsReportId) {
    SuccessMetricsReport report = successMetricsReportService
        .findSuccessMetricsReportByIdForCurrentUser(successMetricsReportId);

    boolean includeLatestData = report.getIncludeLatestData();

    Set<String> applicationIdsToQuery = getApplicationIdsToQuery(report.getScopeOrganizationIds(),
        report.getScopeApplicationIds());

    DateTime currentDateTime = new DateTime();

    SuccessMetricsReportData reportData = successMetricsReportDataDAO.getById(successMetricsReportId);
    DateTime lastUpdated = includeLatestData ? currentDateTime
        : currentDateTime.withDayOfMonth(1).millisOfDay().withMinimumValue();
    Date lastUpdatedDate = lastUpdated.toDate();

    if (reportData == null) {
      policyViolationAggregationService.generatePolicyViolationAggregations(applicationIdsToQuery, currentDateTime,
          includeLatestData);

      reportData = createSuccessMetricsReportData(successMetricsReportId, lastUpdatedDate, applicationIdsToQuery,
          includeLatestData);

      successMetricsReportDataDAO.insert(reportData);
    }
    else if (isReportDataOutOfDate(reportData, includeLatestData, currentDateTime.toDateTime(),
        applicationIdsToQuery)) {
      policyViolationAggregationService.generatePolicyViolationAggregations(applicationIdsToQuery, currentDateTime,
          includeLatestData);

      reportData = createSuccessMetricsReportData(successMetricsReportId, lastUpdatedDate, applicationIdsToQuery,
          includeLatestData);
      successMetricsReportDataDAO.update(reportData);
    }

    SuccessMetricsChartDataDTO result = new SuccessMetricsChartDataDTO();
    result.mttrs = getMttrs(reportData);
    result.averages = getAverages(reportData);
    result.applicationCounts = getApplicationCounts(applicationIdsToQuery, reportData);
    result.lastUpdated = reportData.getLastUpdated();
    result.monthCount = reportData.getMonthCount();

    return result;
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
   * Checks to see if the specified report data is out of date. It is not out of date if it is more recent than
   * the most recent aggregation that would be part of it and contains exactly the applications that would now be
   * part of it
   */
  @VisibleForTesting
  static boolean isReportDataOutOfDate(SuccessMetricsReportData reportData,
                                       boolean includeLatestData,
                                       DateTime currentDateTime,
                                       Set<String> applicationIdsToInclude)
  {
    if (reportData == null) {
      return true;
    }
    else {
      // The time that the aggregations would be updated to if run right now
      DateTime aggregationUpdateTime = includeLatestData ? currentDateTime
          : currentDateTime.withDayOfMonth(1).withTimeAtStartOfDay();

      DateTime reportDataLastUpdated = new DateTime(reportData.getLastUpdated());

      Set<String> reportDataIncludedApplicationIds = reportData.getIncludedApplicationIds();

      return reportDataLastUpdated.isBefore(aggregationUpdateTime)
          || !reportDataIncludedApplicationIds.equals(applicationIdsToInclude);
    }
  }

  private SuccessMetricsReportData createSuccessMetricsReportData(String successMetricsReportId,
                                                                  Date lastUpdated,
                                                                  Set<String> applicationIdsToQuery,
                                                                  boolean includeLatestData)
  {
    SuccessMetricsReportData successMetricsReportData = new SuccessMetricsReportData();
    successMetricsReportData.setId(successMetricsReportId);
    successMetricsReportData.setIncludedApplicationIds(applicationIdsToQuery);
    successMetricsReportData.setLastUpdated(lastUpdated);

    populateMttrData(successMetricsReportData, applicationIdsToQuery, includeLatestData);
    populateAveragesData(successMetricsReportData, applicationIdsToQuery, includeLatestData);
    populateApplicationCountsData(successMetricsReportData, applicationIdsToQuery, includeLatestData);

    int activeApplicationCount = violationAggregationDAO.getActiveApplicationCount(applicationIdsToQuery,
        includeLatestData);

    successMetricsReportData.setActiveApplicationCount(activeApplicationCount);

    return successMetricsReportData;
  }

  private void populateMttrData(SuccessMetricsReportData successMetricsReportData,
                                Set<String> applicationIdsToQuery,
                                boolean includeLatestData)
  {
    List<MttrMonth> mttrMonths = violationAggregationDAO.getMttrMonthlyAverages(applicationIdsToQuery,
        includeLatestData);

    Iterator<MttrMonth> mttrMonthIterator = mttrMonths.iterator();

    for (int monthIndex = 13 - mttrMonths.size(); monthIndex < 13; monthIndex++) {
      MttrMonth mttrMonth = mttrMonthIterator.next();

      successMetricsReportData.setMttrMonthAll(monthIndex, getOverallMttr(mttrMonth));
      successMetricsReportData.setMttrMonthCritical(monthIndex, getCriticalMttr(mttrMonth));
      successMetricsReportData.setMttrMonthTimePeriodStart(monthIndex, mttrMonth.monthStart);
    }
  }

  /**
   * @return the overall Mean Time to Resolution represented by this MttrMonth. Null if this MttrMonth didn't
   * record any resolutions
   */
  private static Integer getOverallMttr(MttrMonth mttrMonth) {
    int totalResolved = mttrMonth.resolvedCountLowThreat + mttrMonth.resolvedCountModerateThreat
        + mttrMonth.resolvedCountSevereThreat + mttrMonth.resolvedCountCriticalThreat;
    double mttrLowThreat = mttrMonth.mttrLowThreat != null ? mttrMonth.mttrLowThreat.doubleValue() : 0;
    double mttrModerateThreat = mttrMonth.mttrModerateThreat != null ? mttrMonth.mttrModerateThreat.doubleValue() : 0;
    double mttrSevereThreat = mttrMonth.mttrSevereThreat != null ? mttrMonth.mttrSevereThreat.doubleValue() : 0;
    double mttrCriticalThreat = mttrMonth.mttrCriticalThreat != null ? mttrMonth.mttrCriticalThreat.doubleValue() : 0;
    // NOTE: SuccessMetricsReportData values are in seconds while MttrMonth values are in milliseconds hence the
    // division by 1000
    if (totalResolved != 0) {
      // combine MTTRs for all threat levels using a weighted average
      return (int) ((mttrLowThreat * mttrMonth.resolvedCountLowThreat + //
          mttrModerateThreat * mttrMonth.resolvedCountModerateThreat + //
          mttrSevereThreat * mttrMonth.resolvedCountSevereThreat + //
          mttrCriticalThreat * mttrMonth.resolvedCountCriticalThreat) / totalResolved / 1000);
    }
    else {
      return null;
    }
  }

  private static Integer getCriticalMttr(MttrMonth mttrMonth) {
    return mttrMonth.mttrCriticalThreat != null ? (int) (mttrMonth.mttrCriticalThreat.doubleValue() / 1000) : null;
  }

  private void populateAveragesData(SuccessMetricsReportData successMetricsReportData,
                                    Set<String> applicationIdsToQuery,
                                    boolean includeLatestData)
  {
    List<AverageMonth> queryResults = violationAggregationDAO.getMonthlyAverages(applicationIdsToQuery,
        includeLatestData);
    List<Integer> evaluationCounts = new ArrayList<>(12);
    List<Double> securityViolationCounts = new ArrayList<>(12);
    List<Double> securityCriticalViolationCounts = new ArrayList<>(12);
    List<Double> licenseViolationCounts = new ArrayList<>(12);
    List<Double> licenseCriticalViolationCounts = new ArrayList<>(12);
    List<Double> qualityViolationCounts = new ArrayList<>(12);
    List<Double> qualityCriticalViolationCounts = new ArrayList<>(12);
    List<Double> otherViolationCounts = new ArrayList<>(12);
    List<Double> otherCriticalViolationCounts = new ArrayList<>(12);

    for (AverageMonth averageMonth : queryResults) {
      evaluationCounts.add(averageMonth.evaluationCount);

      securityViolationCounts.add(averageMonth.security.getSum());
      securityCriticalViolationCounts.add(averageMonth.security.averageDiscoveredCriticalThreat);

      licenseViolationCounts.add(averageMonth.license.getSum());
      licenseCriticalViolationCounts.add(averageMonth.license.averageDiscoveredCriticalThreat);

      qualityViolationCounts.add(averageMonth.quality.getSum());
      qualityCriticalViolationCounts.add(averageMonth.quality.averageDiscoveredCriticalThreat);

      otherViolationCounts.add(averageMonth.other.getSum());
      otherCriticalViolationCounts.add(averageMonth.other.averageDiscoveredCriticalThreat);
    }

    double securityCriticalPolicyViolationsPerApplication = divideOrZero(sumDoubles(securityCriticalViolationCounts),
        queryResults.size());
    double securityPolicyViolationsPerApplication = divideOrZero(sumDoubles(securityViolationCounts),
        queryResults.size());
    double licenseCriticalPolicyViolationsPerApplication = divideOrZero(sumDoubles(licenseCriticalViolationCounts),
        queryResults.size());
    double licensePolicyViolationsPerApplication = divideOrZero(sumDoubles(licenseViolationCounts),
        queryResults.size());
    double qualityCriticalPolicyViolationsPerApplication = divideOrZero(sumDoubles(qualityCriticalViolationCounts),
        queryResults.size());
    double qualityPolicyViolationsPerApplication = divideOrZero(sumDoubles(qualityViolationCounts),
        queryResults.size());
    double otherCriticalPolicyViolationsPerApplication = divideOrZero(sumDoubles(otherCriticalViolationCounts),
        queryResults.size());
    double otherPolicyViolationsPerApplication = divideOrZero(sumDoubles(otherViolationCounts), queryResults.size());

    successMetricsReportData.setEvaluationsPerMonth(divideOrZero(sumIntegers(evaluationCounts), queryResults.size()));

    successMetricsReportData
        .setSecurityCriticalPolicyViolationsPerApplication(securityCriticalPolicyViolationsPerApplication);
    successMetricsReportData.setSecurityPolicyViolationsPerApplication(securityPolicyViolationsPerApplication);

    successMetricsReportData
        .setLicenseCriticalPolicyViolationsPerApplication(licenseCriticalPolicyViolationsPerApplication);
    successMetricsReportData.setLicensePolicyViolationsPerApplication(licensePolicyViolationsPerApplication);

    successMetricsReportData
        .setQualityCriticalPolicyViolationsPerApplication(qualityCriticalPolicyViolationsPerApplication);
    successMetricsReportData.setQualityPolicyViolationsPerApplication(qualityPolicyViolationsPerApplication);

    successMetricsReportData
        .setOtherCriticalPolicyViolationsPerApplication(otherCriticalPolicyViolationsPerApplication);
    successMetricsReportData.setOtherPolicyViolationsPerApplication(otherPolicyViolationsPerApplication);

    successMetricsReportData.setTotalCriticalPolicyViolationsPerApplication(
        securityCriticalPolicyViolationsPerApplication + licenseCriticalPolicyViolationsPerApplication
            + qualityCriticalPolicyViolationsPerApplication + otherCriticalPolicyViolationsPerApplication);

    successMetricsReportData.setTotalPolicyViolationsPerApplication(
        securityPolicyViolationsPerApplication + licensePolicyViolationsPerApplication
            + qualityPolicyViolationsPerApplication + otherPolicyViolationsPerApplication);

    successMetricsReportData.setMonthCount(queryResults.size());
  }

  private static double sumDoubles(Collection<Double> numbers) {
    double retval = 0.0;

    for (Double number : numbers) {
      retval += number;
    }

    return retval;
  }

  private static int sumIntegers(Collection<Integer> numbers) {
    int retval = 0;

    for (Integer number : numbers) {
      retval += number;
    }

    return retval;
  }

  private static double divideOrZero(double numerator, int denominator) {
    return denominator == 0 ? 0.0 : numerator / denominator;
  }

  private void populateApplicationCountsData(SuccessMetricsReportData successMetricsReportData,
                                             Set<String> applicationIdsToQuery,
                                             boolean includeLatestData)
  {
    ApplicationCountsByThreat applicationCounts = violationAggregationDAO
        .getApplicationCountsByThreatByApplicationIds(applicationIdsToQuery, includeLatestData);

    successMetricsReportData.setApplicationsWithViolationsTotal(applicationCounts.countAnyThreat);
    successMetricsReportData.setApplicationsWithCriticalViolationsTotal(applicationCounts.countAnyCriticalThreat);

    successMetricsReportData.setApplicationsWithViolationsSecurity(applicationCounts.countSecurityThreat);
    successMetricsReportData
        .setApplicationsWithCriticalViolationsSecurity(applicationCounts.countSecurityCriticalThreat);

    successMetricsReportData.setApplicationsWithViolationsLicense(applicationCounts.countLicenseThreat);
    successMetricsReportData.setApplicationsWithCriticalViolationsLicense(applicationCounts.countLicenseCriticalThreat);

    successMetricsReportData.setApplicationsWithViolationsQuality(applicationCounts.countQualityThreat);
    successMetricsReportData.setApplicationsWithCriticalViolationsQuality(applicationCounts.countQualityCriticalThreat);

    successMetricsReportData.setApplicationsWithViolationsOther(applicationCounts.countOtherThreat);
    successMetricsReportData.setApplicationsWithCriticalViolationsOther(applicationCounts.countOtherCriticalThreat);
  }

  /**
   * @return the list of MttrDTOs which will be empty if the MTTR functionality is disabled
   * (ie, due to being in PoC mode)
   */
  private static List<MttrDTO> getMttrs(SuccessMetricsReportData successMetricsReportData) {
    List<MttrDTO> retval = new ArrayList<>(PolicyViolationAggregationDAO.NUM_MONTHS);

    for (int month = 1; month <= PolicyViolationAggregationDAO.NUM_MONTHS; month++) {
      Integer mttrMonthAll = successMetricsReportData.getMttrMonthAll(month);
      Integer mttrMonthCritical = successMetricsReportData.getMttrMonthCritical(month);
      Date timePeriodStart = successMetricsReportData.getMttrMonthTimePeriodStart(month);

      if (timePeriodStart != null) {
        String timePeriodName = new YearMonth(timePeriodStart).monthOfYear().getAsShortText(Locale.US);
        MttrDTO dto = new MttrDTO();

        dto.timePeriodName = timePeriodName;
        dto.criticalMttrInSeconds = mttrMonthCritical;
        dto.mttrInSeconds = mttrMonthAll;

        retval.add(dto);
      }
    }

    return retval;
  }

  private static AverageDiscoveredPolicyViolationsDTO getAverages(SuccessMetricsReportData successMetricsReportData) {
    ThreatCategoryPolicyViolationsDTO totalViolations = new ThreatCategoryPolicyViolationsDTO(
        successMetricsReportData.getTotalPolicyViolationsPerApplication(),
        successMetricsReportData.getTotalCriticalPolicyViolationsPerApplication());

    ThreatCategoryPolicyViolationsDTO securityViolations = new ThreatCategoryPolicyViolationsDTO(
        successMetricsReportData.getSecurityPolicyViolationsPerApplication(),
        successMetricsReportData.getSecurityCriticalPolicyViolationsPerApplication());

    ThreatCategoryPolicyViolationsDTO licenseViolations = new ThreatCategoryPolicyViolationsDTO(
        successMetricsReportData.getLicensePolicyViolationsPerApplication(),
        successMetricsReportData.getLicenseCriticalPolicyViolationsPerApplication());

    ThreatCategoryPolicyViolationsDTO qualityViolations = new ThreatCategoryPolicyViolationsDTO(
        successMetricsReportData.getQualityPolicyViolationsPerApplication(),
        successMetricsReportData.getQualityCriticalPolicyViolationsPerApplication());

    ThreatCategoryPolicyViolationsDTO otherViolations = new ThreatCategoryPolicyViolationsDTO(
        successMetricsReportData.getOtherPolicyViolationsPerApplication(),
        successMetricsReportData.getOtherCriticalPolicyViolationsPerApplication());

    return new AverageDiscoveredPolicyViolationsDTO(successMetricsReportData.getEvaluationsPerMonth(), totalViolations,
        securityViolations, licenseViolations, qualityViolations, otherViolations);
  }

  private static ApplicationCountsDTO getApplicationCounts(Set<String> applicationIds,
                                                           SuccessMetricsReportData successMetricsReportData)
  {
    ThreatCategoryApplicationCount totalCount = new ThreatCategoryApplicationCount(
        successMetricsReportData.getApplicationsWithViolationsTotal(),
        successMetricsReportData.getApplicationsWithCriticalViolationsTotal());

    ThreatCategoryApplicationCount securityCount = new ThreatCategoryApplicationCount(
        successMetricsReportData.getApplicationsWithViolationsSecurity(),
        successMetricsReportData.getApplicationsWithCriticalViolationsSecurity());

    ThreatCategoryApplicationCount licenseCount = new ThreatCategoryApplicationCount(
        successMetricsReportData.getApplicationsWithViolationsLicense(),
        successMetricsReportData.getApplicationsWithCriticalViolationsLicense());

    ThreatCategoryApplicationCount qualityCount = new ThreatCategoryApplicationCount(
        successMetricsReportData.getApplicationsWithViolationsQuality(),
        successMetricsReportData.getApplicationsWithCriticalViolationsQuality());

    ThreatCategoryApplicationCount otherCount = new ThreatCategoryApplicationCount(
        successMetricsReportData.getApplicationsWithViolationsOther(),
        successMetricsReportData.getApplicationsWithCriticalViolationsOther());

    return new ApplicationCountsDTO(applicationIds.size(), successMetricsReportData.getActiveApplicationCount(),
        totalCount, securityCount, licenseCount, qualityCount, otherCount);
  }
}
