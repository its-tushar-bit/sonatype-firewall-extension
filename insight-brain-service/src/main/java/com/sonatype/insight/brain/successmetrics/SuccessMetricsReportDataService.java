/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.component.ComponentDisplayFilename;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO.ApplicationCountsByThreat;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO.AverageMonth;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO.MttrMonth;
import com.sonatype.insight.brain.dataaccess.successmetrics.SuccessMetricsReportDataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.HasComponentId;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReport;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReportData;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.successmetrics.ApplicationCountsDTO.ThreatCategoryApplicationCount;
import com.sonatype.insight.brain.successmetrics.AverageDiscoveredPolicyViolationsDTO.ThreatCategoryPolicyViolationsDTO;
import com.sonatype.insight.brain.successmetrics.ComponentCountsDTO.ComponentCountDTO;
import com.sonatype.insight.brain.utils.DateUtils;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.RollbackException;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.LICENSE;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.OTHER;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.QUALITY;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.SECURITY;

/**
 * @since 1.39
 */
@Named
@Singleton
public class SuccessMetricsReportDataService
{
  private static final int COMPONENT_COUNT_LIMIT = 5;

  private final ApplicationService applicationService;

  private final ApplicationComponentDAO applicationComponentDAO;

  private final StageTypeService stageTypeService;

  private final PolicyViolationAggregationService policyViolationAggregationService;

  private final SuccessMetricsReportService successMetricsReportService;

  private final SuccessMetricsReportDataDAO successMetricsReportDataDAO;

  private final PolicyViolationAggregationDAO violationAggregationDAO;

  private final DateTimeFormatter formatter = DateTimeFormat.forPattern("dd MMM").withLocale(Locale.ENGLISH);

  private final PolicyViolationDAO policyViolationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  @Inject
  public SuccessMetricsReportDataService(
      ApplicationService applicationService,
      ApplicationComponentDAO applicationComponentDAO,
      StageTypeService stageTypeService,
      PolicyViolationAggregationService policyViolationAggregationService,
      SuccessMetricsReportService successMetricsReportService,
      SuccessMetricsReportDataDAO successMetricsReportDataDAO,
      PolicyViolationAggregationDAO violationAggregationDAO,
      PolicyViolationDAO policyViolationDAO,
      PolicyEvaluationDAO policyEvaluationDAO)
  {
    this.applicationService = applicationService;
    this.applicationComponentDAO = applicationComponentDAO;
    this.stageTypeService = stageTypeService;
    this.policyViolationAggregationService = policyViolationAggregationService;
    this.successMetricsReportService = successMetricsReportService;
    this.successMetricsReportDataDAO = successMetricsReportDataDAO;
    this.violationAggregationDAO = violationAggregationDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
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
    auditViewSuccessMetricsReport(report, applicationIdsToQuery.size());

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

      try {
        successMetricsReportDataDAO.insert(reportData);
      }
      catch (RollbackException e) {
        if (e.getCause() instanceof EntityExistsException) {
          // race condition, another thread just created it
          reportData = successMetricsReportDataDAO.getById(successMetricsReportId);
        }
        else {
          throw e;
        }
      }
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

  private void auditViewSuccessMetricsReport(SuccessMetricsReport report, final int inspectedApplicationCount) {
    AuditData.get().setSuccessMetricsReport(report).setData("inspectedApplicationCount", inspectedApplicationCount);
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

  private static DateTime latest(DateTime a, DateTime b) {
    return Ordering.natural().max(a, b);
  }

  public ComponentCountsDTO getComponentCounts(String successMetricsReportId) {
    SuccessMetricsReport report = successMetricsReportService
        .findSuccessMetricsReportByIdForCurrentUser(successMetricsReportId);
    Set<String> applicationIdsToQuery = getApplicationIdsToQuery(report.getScopeOrganizationIds(),
        report.getScopeApplicationIds());
    auditViewSuccessMetricsReport(report, applicationIdsToQuery.size());

    // beginning of last month a year ago
    Date sinceDate = new LocalDate().withDayOfMonth(1).minusMonths(13).toDate();

    Set<String> stageTypeIds = new HashSet<>();
    for (StageType stageType : stageTypeService.getLicensedStageTypes()) {
      if (!StageTypes.isIgnoredForPolicyViolationAggregation(stageType.getId())) {
        stageTypeIds.add(stageType.getId());
      }
    }

    Map<String, ComponentInfo> componentApplicationCounts = getComponentApplicationCounts(
        applicationIdsToQuery, stageTypeIds, sinceDate);

    Map<String, ComponentInfo> componentViolationCounts = getComponentViolationCounts(applicationIdsToQuery,
        stageTypeIds, sinceDate);

    List<ComponentInfo> topComponentApplicationCounts = Ordering.natural()
        .greatestOf(componentApplicationCounts.values(), COMPONENT_COUNT_LIMIT);

    List<ComponentInfo> topComponentViolationCounts = Ordering.natural()
        .greatestOf(componentViolationCounts.values(), COMPONENT_COUNT_LIMIT);

    ComponentCountsDTO retval = new ComponentCountsDTO();
    retval.componentsInTheMostApplications = toComponentCountDTOs(topComponentApplicationCounts);
    retval.componentsWithTheMostViolations = toComponentCountDTOs(topComponentViolationCounts);
    retval.componentsPerApplication = getAverageComponentCountPerApplication(applicationIdsToQuery.size(),
        componentApplicationCounts.values());

    return retval;
  }

  /**
   * A container to hold a component's hash, display name, and a count together.
   * For efficiency, the display name is computed lazily from the HasComponentId.
   */
  private static class ComponentInfo
      implements Comparable<ComponentInfo>
  {
    private final HasComponentId hasComponentId;

    public final String hash;

    private final ComponentDisplayFilename componentDisplayFilename = new ComponentDisplayFilename();

    // the return value of the getDisplayName method, cached here
    private String displayName;

    private int count = 1;

    public ComponentInfo(HasComponentId hasComponentId, String hash) {
      this.hasComponentId = hasComponentId;
      this.hash = hash;
    }

    public void addPathnames(Collection<String> pathnames) {
      componentDisplayFilename.addPathnames(pathnames);
    }

    public void incrementCount() {
      count++;
    }

    public int getCount() {
      return count;
    }

    /**
     * @return the displayname from the ComponentIdentifier, or the most common pathname basename, or "Unknown"
     */
    public String getDisplayName() {
      if (displayName == null) {
        ComponentIdentifier componentIdentifier = hasComponentId.getComponentIdentifier();

        if (componentIdentifier != null) {
          displayName = ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString();
        }
        else {
          displayName = componentDisplayFilename.getFilename().orElse("Unknown");
        }
      }

      return displayName;
    }

    @Override
    // sort by count, ascending, and then by displayName string, descending
    public int compareTo(ComponentInfo other) {
      int countDiff = this.getCount() - other.getCount();

      if (countDiff != 0) {
        return countDiff;
      }
      else {
        return other.getDisplayName().compareToIgnoreCase(this.getDisplayName());
      }
    }
  }

  /**
   * @return a map from hash to ComponentInfo where the ComponentInfo counts are counts of the number of applications
   * in which the component is present. Only evaluations more recent than the passed-in date are considered.
   */
  private Map<String, ComponentInfo> getComponentApplicationCounts(Set<String> applicationIds,
                                                                   Set<String> stageTypeIds,
                                                                   Date date)
  {
    List<ApplicationComponent> applicationComponents =
        applicationComponentDAO.getByApplicationIdsAndStageTypeIdsSince(applicationIds, stageTypeIds, date);

    Multimap<String, String> seenAppIdsByComponentHash = HashMultimap.create();
    Map<String, ComponentInfo> retval = new HashMap<>();

    for (ApplicationComponent applicationComponent : applicationComponents) {
      String hash = applicationComponent.getHash();
      String applicationId = applicationComponent.getApplicationId();

      if (seenAppIdsByComponentHash.containsEntry(hash, applicationId)) {
        // avoid double-counting multiple stages for the same app
        continue;
      }

      ComponentInfo componentInfo = retval.get(hash);

      if (componentInfo == null) {
        componentInfo = new ComponentInfo(applicationComponent, hash);
        retval.put(hash, componentInfo);
      }
      else {
        componentInfo.incrementCount();
      }

      componentInfo.addPathnames(applicationComponent.getPathnames());

      seenAppIdsByComponentHash.put(hash, applicationId);
    }

    return retval;
  }

  private List<ComponentCountDTO> toComponentCountDTOs(Collection<ComponentInfo> componentInfos) {
    List<ComponentCountDTO> retval = new ArrayList<>(componentInfos.size());

    for (ComponentInfo componentInfo : componentInfos) {
      ComponentCountDTO dto = new ComponentCountDTO();

      dto.componentDisplayName = componentInfo.getDisplayName();
      dto.hash = componentInfo.hash;
      dto.count = componentInfo.getCount();

      retval.add(dto);
    }

    return retval;
  }

  /**
   * @return a map from Component Id to total violation count in the specified applications.  Only applications
   * with an evaluation more recent than the specified date are included.
   */
  private Map<String, ComponentInfo> getComponentViolationCounts(Set<String> applicationIds,
                                                                 Set<String> stageTypeIds,
                                                                 Date date)
  {
    Map<String, ComponentInfo> retval = new HashMap<>();

    for (PolicyEvaluation evaluation : policyEvaluationDAO
        .getLastByApplicationIdsAndStageIds(applicationIds, stageTypeIds)) {
      if (evaluation == null || evaluation.getTime().compareTo(date) < 0) {
        continue;
      }

      Collection<PolicyViolation> violations = policyViolationDAO
          .getActiveByApplicationIdAndStageId(evaluation.getApplicationId(), evaluation.getStageTypeId());

      for (PolicyViolation violation : violations) {
        String hash = violation.getHash();

        ComponentInfo componentInfo = retval.get(hash);

        if (componentInfo == null) {
          componentInfo = new ComponentInfo(violation, hash);
          retval.put(hash, componentInfo);
        }
        else {
          componentInfo.incrementCount();
        }

        if (violation.getFilename() != null) {
          componentInfo.addPathnames(Collections.singleton(violation.getFilename()));
        }
      }
    }

    return retval;
  }

  private int getAverageComponentCountPerApplication(int applicationCount,
                                                     Collection<ComponentInfo> applicationCountComponentInfos)
  {
    int totalComponentApplicationCounts = 0;
    for (ComponentInfo componentInfo : applicationCountComponentInfos) {
      totalComponentApplicationCounts += componentInfo.getCount();
    }

    if (applicationCount == 0) {
      return 0;
    }

    return totalComponentApplicationCounts / applicationCount;
  }
}
