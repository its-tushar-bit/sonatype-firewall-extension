/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO.ApplicationCountsByThreat;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO.AverageMonth;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO.MttrMonth;
import com.sonatype.insight.brain.dataaccess.successmetrics.SuccessMetricsReportDataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReport;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReportData;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.utils.DateUtils;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Ordering;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.LICENSE;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.OTHER;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.QUALITY;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.SECURITY;
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

  private final DateTimeFormatter formatter = DateTimeFormat.forPattern("dd MMM").withLocale(Locale.ENGLISH);

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
        : latest(currentDateTime.withDayOfMonth(1), currentDateTime.withDayOfWeek(1)).millisOfDay().withMinimumValue();
    Date lastUpdatedDate = lastUpdated.toDate();

    if (reportData == null) {
      policyViolationAggregationService.generatePolicyViolationAggregations(applicationIdsToQuery, currentDateTime,
          includeLatestData);

      reportData = createSuccessMetricsReportData(successMetricsReportId, lastUpdatedDate, applicationIdsToQuery,
          includeLatestData, currentDateTime);

      successMetricsReportDataDAO.insert(reportData);
    }
    else if (isReportDataOutOfDate(reportData, includeLatestData, currentDateTime.toDateTime(),
        applicationIdsToQuery)) {
      policyViolationAggregationService.generatePolicyViolationAggregations(applicationIdsToQuery, currentDateTime,
          includeLatestData);

      reportData = createSuccessMetricsReportData(successMetricsReportId, lastUpdatedDate, applicationIdsToQuery,
          includeLatestData, currentDateTime);
      successMetricsReportDataDAO.update(reportData);
    }

    try {
      return JsonUtils.parse(reportData.getChartDataJson(), SuccessMetricsChartDataDTO.class);
    }
    catch (IOException e) {
      throw new IllegalStateException("Could not parse Success Metrics chart data.", e);
    }
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
          : latest(currentDateTime.withDayOfMonth(1), currentDateTime.withDayOfWeek(1)).withTimeAtStartOfDay();

      DateTime reportDataLastUpdated = new DateTime(reportData.getLastUpdated());

      Set<String> reportDataIncludedApplicationIds = reportData.getIncludedApplicationIds();

      return reportDataLastUpdated.isBefore(aggregationUpdateTime)
          || !reportDataIncludedApplicationIds.equals(applicationIdsToInclude);
    }
  }

  private SuccessMetricsReportData createSuccessMetricsReportData(String successMetricsReportId,
                                                                  Date lastUpdated,
                                                                  Set<String> applicationIdsToQuery,
                                                                  boolean includeLatestData,
                                                                  DateTime currentDateTime)
  {
    SuccessMetricsReportData successMetricsReportData = new SuccessMetricsReportData();
    successMetricsReportData.setId(successMetricsReportId);
    successMetricsReportData.setIncludedApplicationIds(applicationIdsToQuery);
    successMetricsReportData.setLastUpdated(lastUpdated);

    SuccessMetricsChartDataDTO chartDataDTO = new SuccessMetricsChartDataDTO();
    int activeApplicationCount = violationAggregationDAO.getActiveApplicationCount(applicationIdsToQuery,
        includeLatestData);
    populateMttrData(chartDataDTO, applicationIdsToQuery, includeLatestData);
    populateAveragesData(chartDataDTO, applicationIdsToQuery, includeLatestData);
    populateApplicationCountsData(chartDataDTO, applicationIdsToQuery, activeApplicationCount, includeLatestData);
    populateViolationCountsData(chartDataDTO, applicationIdsToQuery, includeLatestData);
    populateViolationsByCategory(chartDataDTO, applicationIdsToQuery, includeLatestData, currentDateTime);
    chartDataDTO.lastUpdated = lastUpdated;
    successMetricsReportData.setChartDataJson(JsonUtils.format(chartDataDTO));

    successMetricsReportData.setActiveApplicationCount(activeApplicationCount);

    return successMetricsReportData;
  }

  private void populateMttrData(SuccessMetricsChartDataDTO chartDataDTO,
                                Set<String> applicationIdsToQuery,
                                boolean includeLatestData)
  {
    chartDataDTO.mttrs = violationAggregationDAO.getMttrMonthlyAverages(applicationIdsToQuery, includeLatestData)
        .stream().map(
            mttrMonth -> new MttrDTO(
                new YearMonth(mttrMonth.monthStart).monthOfYear().getAsShortText(Locale.US),
                getOverallMttr(mttrMonth),
                getCriticalMttr(mttrMonth)))
        .collect(Collectors.toList());
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

  private void populateAveragesData(SuccessMetricsChartDataDTO chartDataDTO,
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

    chartDataDTO.monthCount = queryResults.size();

    ThreatCategoryPolicyViolationsDTO totalViolations = new ThreatCategoryPolicyViolationsDTO(
        securityPolicyViolationsPerApplication + licensePolicyViolationsPerApplication
            + qualityPolicyViolationsPerApplication + otherPolicyViolationsPerApplication,
        securityCriticalPolicyViolationsPerApplication + licenseCriticalPolicyViolationsPerApplication
            + qualityCriticalPolicyViolationsPerApplication + otherCriticalPolicyViolationsPerApplication);

    ThreatCategoryPolicyViolationsDTO securityViolations = new ThreatCategoryPolicyViolationsDTO(
        securityPolicyViolationsPerApplication, securityCriticalPolicyViolationsPerApplication);

    ThreatCategoryPolicyViolationsDTO licenseViolations = new ThreatCategoryPolicyViolationsDTO(
        licensePolicyViolationsPerApplication, licenseCriticalPolicyViolationsPerApplication);

    ThreatCategoryPolicyViolationsDTO qualityViolations = new ThreatCategoryPolicyViolationsDTO(
        qualityPolicyViolationsPerApplication, qualityCriticalPolicyViolationsPerApplication);

    ThreatCategoryPolicyViolationsDTO otherViolations = new ThreatCategoryPolicyViolationsDTO(
        otherPolicyViolationsPerApplication, otherCriticalPolicyViolationsPerApplication);

    chartDataDTO.averages = new AverageDiscoveredPolicyViolationsDTO(
        divideOrZero(sumIntegers(evaluationCounts), queryResults.size()), totalViolations, securityViolations,
        licenseViolations, qualityViolations, otherViolations);
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

  private void populateApplicationCountsData(SuccessMetricsChartDataDTO chartDataDTO,
                                             Set<String> applicationIdsToQuery,
                                             int activeApplicationCount,
                                             boolean includeLatestData)
  {
    ApplicationCountsByThreat applicationCounts = violationAggregationDAO
        .getApplicationCountsByThreatByApplicationIds(applicationIdsToQuery, includeLatestData);

    ThreatCategoryApplicationCount totalCount = new ThreatCategoryApplicationCount(
        applicationCounts.countAnyThreat, applicationCounts.countAnyCriticalThreat);

    ThreatCategoryApplicationCount securityCount = new ThreatCategoryApplicationCount(
        applicationCounts.countSecurityThreat, applicationCounts.countSecurityCriticalThreat);

    ThreatCategoryApplicationCount licenseCount = new ThreatCategoryApplicationCount(
        applicationCounts.countLicenseThreat, applicationCounts.countLicenseCriticalThreat);

    ThreatCategoryApplicationCount qualityCount = new ThreatCategoryApplicationCount(
        applicationCounts.countQualityThreat, applicationCounts.countQualityCriticalThreat);

    ThreatCategoryApplicationCount otherCount = new ThreatCategoryApplicationCount(
        applicationCounts.countOtherThreat, applicationCounts.countOtherCriticalThreat);

    chartDataDTO.applicationCounts = new ApplicationCountsDTO(applicationIdsToQuery.size(), activeApplicationCount,
        totalCount, securityCount, licenseCount, qualityCount, otherCount);
  }

  private void populateViolationsByCategory(SuccessMetricsChartDataDTO dto,
                                            Set<String> applicationIdsToQuery,
                                            boolean includeLatestData,
                                            DateTime currentDateTime)
  {
    dto.violationsByCategoryWeeks = violationAggregationDAO
        .getOpenViolationsCountsByApplicationIds(applicationIdsToQuery, includeLatestData).stream().map(
            week -> new ViolationsByCategoryDTO(getAdjustedOpenViolationCountsDate(week.weekStart),
                week.openViolationCounts.get(SECURITY), week.openViolationCounts.get(LICENSE),
                week.openViolationCounts.get(QUALITY), week.openViolationCounts.get(OTHER)))
        .collect(Collectors.toCollection(LinkedList::new));

    if (dto.violationsByCategoryWeeks.isEmpty()) {
      return;
    }

    padWeeks(dto.violationsByCategoryWeeks, includeLatestData, currentDateTime);
  }

  private void padWeeks(List<ViolationsByCategoryDTO> violationsByCategoryWeeks,
                        boolean includeLatestData,
                        DateTime currentDateTime)
  {
    // Add missing weeks to return a full 12 weeks worth of data
    LocalDate weekStart = new LocalDate(currentDateTime.withDayOfWeek(1));
    while (violationsByCategoryWeeks.size() < 12) {
      // figure out the missing/padded week and add it to the list
      int weeksToAdjust = includeLatestData ? violationsByCategoryWeeks.size() - 1 : violationsByCategoryWeeks.size();
      LocalDate missingWeek = weekStart.minusWeeks(weeksToAdjust);
      violationsByCategoryWeeks.add(0,
          new ViolationsByCategoryDTO(formatter.print(missingWeek), null, null, null, null));
    }
  }

  private void populateViolationCountsData(SuccessMetricsChartDataDTO chartDataDTO,
                                           Set<String> applicationIdsToQuery,
                                           boolean includeLatestData)
  {
    chartDataDTO.violationCounts = violationAggregationDAO
        .getViolationCountsByApplicationIds(applicationIdsToQuery, includeLatestData).stream().map(week -> {
          ViolationCountsDTO violationCountsDTO = new ViolationCountsDTO();

          DateTime periodStart = new DateTime(week.periodStart);
          String monthName = periodStart.monthOfYear().getAsText(Locale.US);
          int dayOfMonth = periodStart.getDayOfMonth();
          String dayOfMonthOrdinal = dayOfMonth + DateUtils.getDayOfMonthSuffix(dayOfMonth);

          violationCountsDTO.timePeriodName = "Week of " + monthName + " " + dayOfMonthOrdinal;
          violationCountsDTO.discoveredCounts = week.discoveredCounts;
          violationCountsDTO.fixedCounts = week.fixedCounts;
          violationCountsDTO.waivedCounts = week.waivedCounts;

          return violationCountsDTO;
        })
        .collect(Collectors.toList());
  }

  /**
   * Open counts are calculated at a specific point in time (a snapshot) as opposed to discovered/fixed/waived counts 
   * that represent the number of respective events that occurred during a given time period. The snapshot we use for 
   * open counts is taken at the end of the time period (week) or technically at the beginning of the next time period. 
   * Here we make those adjustments.
   */
  private String getAdjustedOpenViolationCountsDate(Date weekStart) {
    LocalDate weekStartAsLocalDate = new LocalDate(weekStart);
    if (weekStartAsLocalDate.equals(new LocalDate().withDayOfWeek(1))) {
      return "now";
    }
    return formatter.print(weekStartAsLocalDate.plusWeeks(1));
  }

  private static DateTime latest(DateTime a, DateTime b)
  {
    return Ordering.natural().max(a, b);
  }
}
