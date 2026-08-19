/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.LICENSE;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.OTHER;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.QUALITY;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.SECURITY;
import static com.sonatype.insight.brain.utils.ThreatLevel.CRITICAL;
import static com.sonatype.insight.brain.utils.ThreatLevel.LOW;
import static com.sonatype.insight.brain.utils.ThreatLevel.MODERATE;
import static com.sonatype.insight.brain.utils.ThreatLevel.SEVERE;

import com.sonatype.insight.brain.api.v2.dto.ApiMetricsReportingAggregationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiMetricsReportingDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiMetricsReportingFlattenedDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiMetricsReportingQueryDTOV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditService;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.successmetrics.PolicyViolationAggregation;
import com.sonatype.insight.brain.model.successmetrics.TimePeriod;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.successmetrics.PolicyViolationAggregationService;
import com.sonatype.insight.error.exception.BadRequestException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * @since 1.52
 */
@Named
@Singleton
public class ApiMetricsReportingServiceV2
{
  private static final Map<TimePeriod, DateParser> inputDateParsers = new EnumMap<>(TimePeriod.class);

  static {
    inputDateParsers.put(TimePeriod.WEEK, new WeekParser());
    inputDateParsers.put(TimePeriod.MONTH, new MonthParser());
  }

  private final DateTimeFormatter outputDateFormatter = ISODateTimeFormat.yearMonthDay();

  private final PolicyViolationAggregationDAO policyViolationAggregationDAO;

  private final OrganizationDAO organizationDAO;

  private final ApplicationService applicationService;

  private final PolicyViolationAggregationService policyViolationAggregationService;

  private final AuditService auditService;

  @Inject
  public ApiMetricsReportingServiceV2(
      final ApplicationService applicationService,
      final PolicyViolationAggregationService policyViolationAggregationService,
      final PolicyViolationAggregationDAO policyViolationAggregationDAO,
      final OrganizationDAO organizationDAO,
      final AuditService auditService)
  {
    this.policyViolationAggregationDAO = policyViolationAggregationDAO;
    this.organizationDAO = organizationDAO;
    this.applicationService = applicationService;
    this.policyViolationAggregationService = policyViolationAggregationService;
    this.auditService = auditService;
  }

  public void validate(ApiMetricsReportingQueryDTOV2 queryDTO) {
    validateRequiredFields(queryDTO);
    DateParser dateParser = inputDateParsers.get(queryDTO.timePeriod);
    validateDateOrdering(dateParser.parse(queryDTO.firstTimePeriod),
        Optional.ofNullable(queryDTO.lastTimePeriod).map(dateParser::parse));
  }

  public List<Application> getApplications(ApiMetricsReportingQueryDTOV2 queryDTO) {
    return applicationService
        .getApplicationsByIdsAndOrganizationIdsAndTagIds(queryDTO.organizationIds, queryDTO.applicationIds, null);
  }

  List<ApiMetricsReportingDTOV2> getMetrics(ApiMetricsReportingQueryDTOV2 queryDTO) {
    return getMetrics(queryDTO, new DateTime(), getApplications(queryDTO));
  }

  public List<ApiMetricsReportingDTOV2> getMetrics(
      ApiMetricsReportingQueryDTOV2 queryDTO,
      DateTime now,
      List<Application> applications)
  {
    TimePeriod timePeriod = queryDTO.timePeriod;
    DateParser dateParser = inputDateParsers.get(timePeriod);

    LocalDate beginningOfCurrentTimePeriod = now.toLocalDate().withField(timePeriod.getDateTimeFieldType(), 1);

    LocalDate firstTimePeriod = dateParser.parse(queryDTO.firstTimePeriod);
    Optional<LocalDate> lastTimePeriod = Optional.ofNullable(queryDTO.lastTimePeriod).map(dateParser::parse);

    // DAO expects endDate to be exclusive, ie it should be the beginning of the first time period that we don't want
    Optional<LocalDate> endDate = lastTimePeriod.map(localDate -> localDate.plus(timePeriod.getPeriod(1)));
    boolean includeLatestData = endDate.map(ed -> ed.isAfter(beginningOfCurrentTimePeriod)).orElse(true);

    Map<String, Application> applicationsById = applications.stream()
        .collect(Collectors.toMap(Application::getId, Function.identity()));

    Set<String> applicationIds = applicationsById.keySet();

    policyViolationAggregationService.generatePolicyViolationAggregations(applicationIds, now, includeLatestData);

    List<PolicyViolationAggregation> aggregations =
        policyViolationAggregationDAO.getByApplicationIdsAndTimePeriodBounds(applicationIds, timePeriod,
            firstTimePeriod.toDate(), endDate.map(LocalDate::toDate).orElse(null));

    return makeDTOs(aggregations, applicationsById);
  }

  public void auditExportMetricsReport(
      final ApiMetricsReportingQueryDTOV2 queryDTO,
      final List<Application> applications)
  {
    Map<String, Application> applicationsById = applications.stream()
        .collect(Collectors.toMap(Application::getId, Function.identity()));
    DateParser dateParser = inputDateParsers.get(queryDTO.timePeriod);
    LocalDate startDate = dateParser.parse(queryDTO.firstTimePeriod);
    Optional<LocalDate> endDate = Optional.ofNullable(queryDTO.lastTimePeriod)
        .map(dateParser::parse)
        .map(localDate -> localDate.plus(queryDTO.timePeriod.getPeriod(1)));
    AuditData.get()
        .setData("beginDate", startDate.toString())
        .setData("endDate", endDate.orElse(LocalDate.now()).toString())
        .setData("selectedOrganizations", auditService.getSelectedOrganizationsById(queryDTO.organizationIds))
        .setData("selectedApplications",
            auditService.getSelectedApplicationsById(queryDTO.applicationIds, queryDTO.organizationIds,
                applicationsById))
        .setData("inspectedApplicationCount", applicationsById.size());
  }

  /**
   * Same as getMetrics, but returns the data in flattened DTOs
   */
  List<ApiMetricsReportingFlattenedDTOV2> getFlattenedMetrics(ApiMetricsReportingQueryDTOV2 queryDTO) {
    return getMetrics(queryDTO).stream().flatMap(this::flattenDTO).collect(Collectors.toList());
  }

  public List<ApiMetricsReportingFlattenedDTOV2> getFlattenedMetrics(
      ApiMetricsReportingQueryDTOV2 queryDTO,
      DateTime now,
      List<Application> applications)
  {
    return getMetrics(queryDTO, now, applications).stream()
        .flatMap(this::flattenDTO)
        .collect(Collectors.toList());
  }

  /**
   * Validate that all required fields are present on the queryDTO, and throw BadRequestException if not
   */
  private void validateRequiredFields(ApiMetricsReportingQueryDTOV2 queryDTO) {
    if (queryDTO == null) {
      throw new BadRequestException("Request parameters must be defined");
    }

    if (queryDTO.timePeriod == null) {
      throw new BadRequestException("timePeriod must be defined");
    }

    if (queryDTO.firstTimePeriod == null) {
      throw new BadRequestException("firstTimePeriod must be defined");
    }
  }

  private void validateDateOrdering(LocalDate firstTimePeriod, Optional<LocalDate> lastTimePeriod) {
    lastTimePeriod.ifPresent(last -> {
      if (last.isBefore(firstTimePeriod)) {
        throw new BadRequestException("lastTimePeriod must not be before firstTimePeriod");
      }
    });
  }

  /**
   * Gather up the PolicyViolationAggregations into ApiMetricsReportingDTOV2s. Each DTO represents one application, with
   * one or more aggregations.
   *
   * @param aggregations The aggregations to convert into DTOs
   * @param applicationsById map of applications indexed by their id. Since the calling method has already fetched them
   *          all, this prevents this method from having to re-fetch them one at a time
   */
  private List<ApiMetricsReportingDTOV2> makeDTOs(
      List<PolicyViolationAggregation> aggregations,
      Map<String, Application> applicationsById)
  {
    Map<String, List<PolicyViolationAggregation>> aggregationsByApplicationId = aggregations.stream()
        .collect(Collectors.groupingBy(PolicyViolationAggregation::getApplicationId));

    // map of all organizations that have been retrieved so far, so we don't have to re-retrieve them
    Map<String, Organization> organizationsById = new HashMap<>();

    return aggregationsByApplicationId.entrySet().stream().map(entry -> {
      String applicationId = entry.getKey();
      List<PolicyViolationAggregation> thisAppsAggregations = entry.getValue();

      Application application = applicationsById.get(applicationId);

      Organization organization = organizationsById.computeIfAbsent(application.getOrganizationId(),
          organizationDAO::getById);

      List<ApiMetricsReportingAggregationDTOV2> aggregationDTOs = thisAppsAggregations.stream()
          .map(this::makeAggregationDTO)
          .collect(Collectors.toList());

      return new ApiMetricsReportingDTOV2(application.getId(), application.getPublicId(),
          application.getName(), organization.getId(), organization.getName(), aggregationDTOs);
    }).collect(Collectors.toList());
  }

  private ApiMetricsReportingAggregationDTOV2 makeAggregationDTO(PolicyViolationAggregation aggregation) {
    return new ApiMetricsReportingAggregationDTOV2(
        outputDateFormatter.print(aggregation.getTimePeriodStart().getTime()), //
        aggregation.getMttrLowThreat(), //
        aggregation.getMttrModerateThreat(), //
        aggregation.getMttrSevereThreat(), //
        aggregation.getMttrCriticalThreat(), //
        aggregation.getDiscoveredAsTable().rowMap(), //
        aggregation.getFixedAsTable().rowMap(), //
        aggregation.getWaivedAsTable().rowMap(), //
        aggregation.getOpenAsTable().rowMap(), //
        aggregation.getEvaluationCount());
  }

  private Stream<ApiMetricsReportingFlattenedDTOV2> flattenDTO(ApiMetricsReportingDTOV2 inputDTO) {
    return inputDTO.aggregations.stream()
        .map(aggregationDTO -> new ApiMetricsReportingFlattenedDTOV2( //
            inputDTO.applicationId, //
            inputDTO.applicationPublicId, //
            inputDTO.applicationName, //
            inputDTO.organizationId, //
            inputDTO.organizationName, //
            aggregationDTO.timePeriodStart, //
            aggregationDTO.mttrLowThreat, //
            aggregationDTO.mttrModerateThreat, //
            aggregationDTO.mttrSevereThreat, //
            aggregationDTO.mttrCriticalThreat, //
            aggregationDTO.evaluationCount, //

            aggregationDTO.discoveredCounts.get(SECURITY).get(LOW), //
            aggregationDTO.discoveredCounts.get(SECURITY).get(MODERATE), //
            aggregationDTO.discoveredCounts.get(SECURITY).get(SEVERE), //
            aggregationDTO.discoveredCounts.get(SECURITY).get(CRITICAL), //
            aggregationDTO.discoveredCounts.get(LICENSE).get(LOW), //
            aggregationDTO.discoveredCounts.get(LICENSE).get(MODERATE), //
            aggregationDTO.discoveredCounts.get(LICENSE).get(SEVERE), //
            aggregationDTO.discoveredCounts.get(LICENSE).get(CRITICAL), //
            aggregationDTO.discoveredCounts.get(QUALITY).get(LOW), //
            aggregationDTO.discoveredCounts.get(QUALITY).get(MODERATE), //
            aggregationDTO.discoveredCounts.get(QUALITY).get(SEVERE), //
            aggregationDTO.discoveredCounts.get(QUALITY).get(CRITICAL), //
            aggregationDTO.discoveredCounts.get(OTHER).get(LOW), //
            aggregationDTO.discoveredCounts.get(OTHER).get(MODERATE), //
            aggregationDTO.discoveredCounts.get(OTHER).get(SEVERE), //
            aggregationDTO.discoveredCounts.get(OTHER).get(CRITICAL), //

            aggregationDTO.fixedCounts.get(SECURITY).get(LOW), //
            aggregationDTO.fixedCounts.get(SECURITY).get(MODERATE), //
            aggregationDTO.fixedCounts.get(SECURITY).get(SEVERE), //
            aggregationDTO.fixedCounts.get(SECURITY).get(CRITICAL), //
            aggregationDTO.fixedCounts.get(LICENSE).get(LOW), //
            aggregationDTO.fixedCounts.get(LICENSE).get(MODERATE), //
            aggregationDTO.fixedCounts.get(LICENSE).get(SEVERE), //
            aggregationDTO.fixedCounts.get(LICENSE).get(CRITICAL), //
            aggregationDTO.fixedCounts.get(QUALITY).get(LOW), //
            aggregationDTO.fixedCounts.get(QUALITY).get(MODERATE), //
            aggregationDTO.fixedCounts.get(QUALITY).get(SEVERE), //
            aggregationDTO.fixedCounts.get(QUALITY).get(CRITICAL), //
            aggregationDTO.fixedCounts.get(OTHER).get(LOW), //
            aggregationDTO.fixedCounts.get(OTHER).get(MODERATE), //
            aggregationDTO.fixedCounts.get(OTHER).get(SEVERE), //
            aggregationDTO.fixedCounts.get(OTHER).get(CRITICAL), //

            aggregationDTO.waivedCounts.get(SECURITY).get(LOW), //
            aggregationDTO.waivedCounts.get(SECURITY).get(MODERATE), //
            aggregationDTO.waivedCounts.get(SECURITY).get(SEVERE), //
            aggregationDTO.waivedCounts.get(SECURITY).get(CRITICAL), //
            aggregationDTO.waivedCounts.get(LICENSE).get(LOW), //
            aggregationDTO.waivedCounts.get(LICENSE).get(MODERATE), //
            aggregationDTO.waivedCounts.get(LICENSE).get(SEVERE), //
            aggregationDTO.waivedCounts.get(LICENSE).get(CRITICAL), //
            aggregationDTO.waivedCounts.get(QUALITY).get(LOW), //
            aggregationDTO.waivedCounts.get(QUALITY).get(MODERATE), //
            aggregationDTO.waivedCounts.get(QUALITY).get(SEVERE), //
            aggregationDTO.waivedCounts.get(QUALITY).get(CRITICAL), //
            aggregationDTO.waivedCounts.get(OTHER).get(LOW), //
            aggregationDTO.waivedCounts.get(OTHER).get(MODERATE), //
            aggregationDTO.waivedCounts.get(OTHER).get(SEVERE), //
            aggregationDTO.waivedCounts.get(OTHER).get(CRITICAL), //

            aggregationDTO.openCountsAtTimePeriodEnd.get(SECURITY).get(LOW), //
            aggregationDTO.openCountsAtTimePeriodEnd.get(SECURITY).get(MODERATE), //
            aggregationDTO.openCountsAtTimePeriodEnd.get(SECURITY).get(SEVERE), //
            aggregationDTO.openCountsAtTimePeriodEnd.get(SECURITY).get(CRITICAL), //
            aggregationDTO.openCountsAtTimePeriodEnd.get(LICENSE).get(LOW), //
            aggregationDTO.openCountsAtTimePeriodEnd.get(LICENSE).get(MODERATE), //
            aggregationDTO.openCountsAtTimePeriodEnd.get(LICENSE).get(SEVERE), //
            aggregationDTO.openCountsAtTimePeriodEnd.get(LICENSE).get(CRITICAL), //
            aggregationDTO.openCountsAtTimePeriodEnd.get(QUALITY).get(LOW), //
            aggregationDTO.openCountsAtTimePeriodEnd.get(QUALITY).get(MODERATE), //
            aggregationDTO.openCountsAtTimePeriodEnd.get(QUALITY).get(SEVERE), //
            aggregationDTO.openCountsAtTimePeriodEnd.get(QUALITY).get(CRITICAL), //
            aggregationDTO.openCountsAtTimePeriodEnd.get(OTHER).get(LOW), //
            aggregationDTO.openCountsAtTimePeriodEnd.get(OTHER).get(MODERATE), //
            aggregationDTO.openCountsAtTimePeriodEnd.get(OTHER).get(SEVERE), //
            aggregationDTO.openCountsAtTimePeriodEnd.get(OTHER).get(CRITICAL)));
  }

  /**
   * A small wrapper class around a joda DateTimeFormatter that performs parsing and does error handling. Used via its
   * subclasses WeekParser and MonthParser
   */
  private abstract static class DateParser
  {
    private final DateTimeFormatter formatter;

    protected DateParser(DateTimeFormatter formatter) {
      this.formatter = formatter;
    }

    public abstract String getParseErrorMessage(String invalidDateStr);

    public LocalDate parse(String input) {
      try {
        return formatter.parseLocalDate(input);
      }
      catch (IllegalArgumentException e) {
        throw new BadRequestException(getParseErrorMessage(input), e);
      }
    }
  }

  private static class WeekParser
      extends DateParser
  {
    public WeekParser() {
      super(ISODateTimeFormat.weekyearWeek());
    }

    @Override
    public String getParseErrorMessage(String invalidDateStr) {
      return "'" + invalidDateStr + "' does not match expected ISO 8601 date format for WEEK timePeriods: xxxx-'W'ww";
    }
  }

  private static class MonthParser
      extends DateParser
  {
    public MonthParser() {
      super(ISODateTimeFormat.yearMonth());
    }

    @Override
    public String getParseErrorMessage(String invalidDateStr) {
      return "'" + invalidDateStr + "' does not match expected ISO 8601 date format for MONTH timePeriods: yyyy-MM";
    }
  }
}
