/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiMetricsReportingAggregationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiMetricsReportingDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiMetricsReportingFlattenedDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiMetricsReportingQueryDTOV2;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDataHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.successmetrics.TimePeriod;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsTestUtils.FakeDateRule;
import com.sonatype.insight.error.exception.BadRequestException;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Rule;
import org.junit.Test;

import static com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDataHelper.discovered;
import static com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDataHelper.fixed;
import static com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDataHelper.open;
import static com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDataHelper.openWithSampleData;
import static com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDataHelper.waived;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ApiMetricsReportingServiceV2Test
    extends AbstractComponentTest
{
  @Inject
  private ApiMetricsReportingServiceV2 service;

  @Rule
  public FakeDateRule fakeDateRule = new FakeDateRule();

  @Test
  public void testGetMetrics_NullTimePeriod() {
    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(null, "2018-02", "2018-02",
        Collections.emptySet(), Collections.emptySet());

    try {
      service.getMetrics(queryDTO);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("timePeriod must be defined"));
    }
  }

  @Test
  public void testGetMetrics_NullFirstTimePeriod() {
    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, null, "2018-02",
        Collections.emptySet(), Collections.emptySet());

    try {
      service.getMetrics(queryDTO);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("firstTimePeriod must be defined"));
    }
  }

  @Test
  public void testGetMetrics_LastTimePeriodBeforeFirst() {
    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, "2018-03", "2018-02",
        Collections.emptySet(), Collections.emptySet());

    try {
      service.getMetrics(queryDTO);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("lastTimePeriod must not be before firstTimePeriod"));
    }
  }

  @Test
  public void testGetMetrics_WeekTimePeriod_InvalidFirstTimePeriod() {
    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.WEEK, "2018-03", null,
        Collections.emptySet(), Collections.emptySet());

    try {
      service.getMetrics(queryDTO);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(),
          is("'2018-03' does not match expected ISO 8601 date format for WEEK timePeriods: xxxx-'W'ww"));
    }
  }

  @Test
  public void testGetMetrics_WeekTimePeriod_InvalidLastTimePeriod() {
    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.WEEK, "2018-W03", "2018-04",
        Collections.emptySet(), Collections.emptySet());

    try {
      service.getMetrics(queryDTO);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(),
          is("'2018-04' does not match expected ISO 8601 date format for WEEK timePeriods: xxxx-'W'ww"));
    }
  }

  @Test
  public void testGetMetrics_MonthTimePeriod_InvalidFirstTimePeriod() {
    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, "2018-W03", null,
        Collections.emptySet(), Collections.emptySet());

    try {
      service.getMetrics(queryDTO);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(),
          is("'2018-W03' does not match expected ISO 8601 date format for MONTH timePeriods: yyyy-MM"));
    }
  }

  @Test
  public void testGetMetrics_MonthTimePeriod_InvalidLastTimePeriod() {
    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, "2018-03", "2018-W04",
        Collections.emptySet(), Collections.emptySet());

    try {
      service.getMetrics(queryDTO);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(),
          is("'2018-W04' does not match expected ISO 8601 date format for MONTH timePeriods: yyyy-MM"));
    }
  }

  @Test
  public void testGetMetrics_FullData_Monthly() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    Organization organization = new OrganizationDAO().getById(PolicyViolationAggregationDataHelper.ORG_ID);

    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, "2017-10", null,
        Collections.emptySet(), Collections.emptySet());

    List<ApiMetricsReportingDTOV2> results = service.getMetrics(queryDTO);

    assertMonthlyData(results, organization, null, null);
  }

  @Test
  public void testGetMetrics_FullData_Weekly() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    Organization organization = new OrganizationDAO().getById(PolicyViolationAggregationDataHelper.ORG_ID);

    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.WEEK, "2017-W48", null,
        Collections.emptySet(), Collections.emptySet());

    List<ApiMetricsReportingDTOV2> results = service.getMetrics(queryDTO);

    assertWeeklyData(results, organization, null, null);
  }

  @Test
  public void testGetMetrics_UpperBound_Monthly() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    Organization organization = new OrganizationDAO().getById(PolicyViolationAggregationDataHelper.ORG_ID);

    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, "2017-10", "2017-11",
        Collections.emptySet(), Collections.emptySet());

    List<ApiMetricsReportingDTOV2> results = service.getMetrics(queryDTO);

    assertMonthlyData(results, organization, 2, null);
  }

  @Test
  public void testGetMetrics_UpperBound_Weekly() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    Organization organization = new OrganizationDAO().getById(PolicyViolationAggregationDataHelper.ORG_ID);

    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.WEEK, "2017-W48", "2017-W49",
        Collections.emptySet(), Collections.emptySet());

    List<ApiMetricsReportingDTOV2> results = service.getMetrics(queryDTO);

    assertWeeklyData(results, organization, 2, null);
  }

  @Test
  public void testGetMetrics_ByOrg_Monthly() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);
    createOtherOrgWithViolations();

    Organization organization = new OrganizationDAO().getById(PolicyViolationAggregationDataHelper.ORG_ID);

    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, "2017-10", null,
        Collections.emptySet(), Collections.singleton(PolicyViolationAggregationDataHelper.ORG_ID));

    List<ApiMetricsReportingDTOV2> results = service.getMetrics(queryDTO);

    assertMonthlyData(results, organization, null, null);
  }

  @Test
  public void testGetMetrics_ByOrg_Weekly() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);
    createOtherOrgWithViolations();

    Organization organization = new OrganizationDAO().getById(PolicyViolationAggregationDataHelper.ORG_ID);

    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.WEEK, "2017-W48", null,
        Collections.emptySet(), Collections.singleton(PolicyViolationAggregationDataHelper.ORG_ID));

    List<ApiMetricsReportingDTOV2> results = service.getMetrics(queryDTO);

    assertWeeklyData(results, organization, null, null);
  }

  @Test
  public void testGetMetrics_ByApp_Monthly() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);
    Set<String> applicationIds = Collections.singleton(PolicyViolationAggregationDataHelper.APPLICATION_IDS[0]);

    Organization organization = new OrganizationDAO().getById(PolicyViolationAggregationDataHelper.ORG_ID);

    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, "2017-10", null,
        applicationIds, Collections.emptySet());

    List<ApiMetricsReportingDTOV2> results = service.getMetrics(queryDTO);

    assertMonthlyData(results, organization, null, applicationIds);
  }

  @Test
  public void testGetMetrics_ByApp_Weekly() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);
    Set<String> applicationIds = Collections.singleton(PolicyViolationAggregationDataHelper.APPLICATION_IDS[0]);

    Organization organization = new OrganizationDAO().getById(PolicyViolationAggregationDataHelper.ORG_ID);

    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.WEEK, "2017-W48", null,
        applicationIds, Collections.emptySet());

    List<ApiMetricsReportingDTOV2> results = service.getMetrics(queryDTO);

    assertWeeklyData(results, organization, null, applicationIds);
  }

  @Test
  public void testGetMetrics_NullAppsOrgs_Monthly() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    Organization organization = new OrganizationDAO().getById(PolicyViolationAggregationDataHelper.ORG_ID);

    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, "2017-10", null,
        null, null);

    List<ApiMetricsReportingDTOV2> results = service.getMetrics(queryDTO);

    assertMonthlyData(results, organization, null, null);
  }

  @Test
  public void testGetMetrics_NullAppsOrgs_Weekly() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    Organization organization = new OrganizationDAO().getById(PolicyViolationAggregationDataHelper.ORG_ID);

    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.WEEK, "2017-W48", null,
        null, null);

    List<ApiMetricsReportingDTOV2> results = service.getMetrics(queryDTO);

    assertWeeklyData(results, organization, null, null);
  }

  @Test
  public void testGetMetrics_GeneratesAggregations() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Policy policy = tempEntity.newPolicy(app.getId(), "test policy", 10);
    LocalDate now = new LocalDate();

    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan1",
        now.minusMonths(1).toDate());
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan1",
        now.minusMonths(1).plusDays(1).toDate());

    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, null, null, "unknown component");

    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, "2017-11", null,
        null, null);

    violation1.setFixTime(eval2.getTime());
    new PolicyViolationDAO().update(violation1);

    List<ApiMetricsReportingDTOV2> results = service.getMetrics(queryDTO);
    assertThat(results, hasSize(1));

    ApiMetricsReportingDTOV2 actualDTO = results.get(0);
    ApiMetricsReportingDTOV2 expectedDTO =
        new ApiMetricsReportingDTOV2(app.getId(), app.getPublicId(), app.getName(), org.getId(), org.getName(),
            Arrays.asList( //
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-11-01", //
                    null, null, null, 86400000L, //
                    discovered().security(0, 0, 0, 1).asMap(), //
                    fixed().security(0, 0, 0, 1).asMap(), //
                    waived().asMap(), //
                    open().asMap(), //
                    2 // evaluationCount
                ),
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-12-01", //
                    null, null, null, null, //
                    discovered().asMap(), //
                    fixed().asMap(), //
                    waived().asMap(), //
                    open().asMap(), //
                    0 // evaluationCount
                )
            )
        );

    assertDTO(actualDTO, expectedDTO);
  }

  @Test
  public void testGetFlattenedMetrics_NullTimePeriod() {
    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(null, "2018-02", "2018-02",
        Collections.emptySet(), Collections.emptySet());

    try {
      service.getFlattenedMetrics(queryDTO);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("timePeriod must be defined"));
    }
  }

  @Test
  public void testGetFlattenedMetrics_NullFirstTimePeriod() {
    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, null, "2018-02",
        Collections.emptySet(), Collections.emptySet());

    try {
      service.getFlattenedMetrics(queryDTO);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("firstTimePeriod must be defined"));
    }
  }

  @Test
  public void testGetFlattenedMetrics_LastTimePeriodBeforeFirst() {
    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, "2018-03", "2018-02",
        Collections.emptySet(), Collections.emptySet());

    try {
      service.getFlattenedMetrics(queryDTO);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("lastTimePeriod must not be before firstTimePeriod"));
    }
  }

  @Test
  public void testGetFlattenedMetrics_WeekTimePeriod_InvalidFirstTimePeriod() {
    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.WEEK, "2018-03", null,
        Collections.emptySet(), Collections.emptySet());

    try {
      service.getFlattenedMetrics(queryDTO);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(),
          is("'2018-03' does not match expected ISO 8601 date format for WEEK timePeriods: xxxx-'W'ww"));
    }
  }

  @Test
  public void testGetFlattenedMetrics_WeekTimePeriod_InvalidLastTimePeriod() {
    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.WEEK, "2018-W03", "2018-04",
        Collections.emptySet(), Collections.emptySet());

    try {
      service.getFlattenedMetrics(queryDTO);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(),
          is("'2018-04' does not match expected ISO 8601 date format for WEEK timePeriods: xxxx-'W'ww"));
    }
  }

  @Test
  public void testGetFlattenedMetrics_MonthTimePeriod_InvalidFirstTimePeriod() {
    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, "2018-W03", null,
        Collections.emptySet(), Collections.emptySet());

    try {
      service.getFlattenedMetrics(queryDTO);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(),
          is("'2018-W03' does not match expected ISO 8601 date format for MONTH timePeriods: yyyy-MM"));
    }
  }

  @Test
  public void testGetFlattenedMetrics_MonthTimePeriod_InvalidLastTimePeriod() {
    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, "2018-03", "2018-W04",
        Collections.emptySet(), Collections.emptySet());

    try {
      service.getFlattenedMetrics(queryDTO);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(),
          is("'2018-W04' does not match expected ISO 8601 date format for MONTH timePeriods: yyyy-MM"));
    }
  }

  @Test
  public void testGetFlattenedMetrics_FullData_Monthly() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    Organization organization = new OrganizationDAO().getById(PolicyViolationAggregationDataHelper.ORG_ID);

    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, "2017-10", null,
        Collections.emptySet(), Collections.emptySet());

    List<ApiMetricsReportingFlattenedDTOV2> results = service.getFlattenedMetrics(queryDTO);

    assertFlattenedMonthlyData(results, organization, null, null);
  }

  @Test
  public void testGetFlattenedMetrics_FullData_Weekly() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    Organization organization = new OrganizationDAO().getById(PolicyViolationAggregationDataHelper.ORG_ID);

    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.WEEK, "2017-W48", null,
        Collections.emptySet(), Collections.emptySet());

    List<ApiMetricsReportingFlattenedDTOV2> results = service.getFlattenedMetrics(queryDTO);

    assertFlattenedWeeklyData(results, organization, null, null);
  }

  @Test
  public void testGetFlattenedMetrics_UpperBound_Monthly() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    Organization organization = new OrganizationDAO().getById(PolicyViolationAggregationDataHelper.ORG_ID);

    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, "2017-10", "2017-11",
        Collections.emptySet(), Collections.emptySet());

    List<ApiMetricsReportingFlattenedDTOV2> results = service.getFlattenedMetrics(queryDTO);

    assertFlattenedMonthlyData(results, organization, 2, null);
  }

  @Test
  public void testGetFlattenedMetrics_UpperBound_Weekly() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    Organization organization = new OrganizationDAO().getById(PolicyViolationAggregationDataHelper.ORG_ID);

    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.WEEK, "2017-W48", "2017-W49",
        Collections.emptySet(), Collections.emptySet());

    List<ApiMetricsReportingFlattenedDTOV2> results = service.getFlattenedMetrics(queryDTO);

    assertFlattenedWeeklyData(results, organization, 2, null);
  }

  @Test
  public void testGetFlattenedMetrics_ByOrg_Monthly() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);
    createOtherOrgWithViolations();

    Organization organization = new OrganizationDAO().getById(PolicyViolationAggregationDataHelper.ORG_ID);

    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, "2017-10", null,
        Collections.emptySet(), Collections.singleton(PolicyViolationAggregationDataHelper.ORG_ID));

    List<ApiMetricsReportingFlattenedDTOV2> results = service.getFlattenedMetrics(queryDTO);

    assertFlattenedMonthlyData(results, organization, null, null);
  }

  @Test
  public void testGetFlattenedMetrics_ByOrg_Weekly() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);
    createOtherOrgWithViolations();

    Organization organization = new OrganizationDAO().getById(PolicyViolationAggregationDataHelper.ORG_ID);

    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.WEEK, "2017-W48", null,
        Collections.emptySet(), Collections.singleton(PolicyViolationAggregationDataHelper.ORG_ID));

    List<ApiMetricsReportingFlattenedDTOV2> results = service.getFlattenedMetrics(queryDTO);

    assertFlattenedWeeklyData(results, organization, null, null);
  }

  @Test
  public void testGetFlattenedMetrics_ByApp_Monthly() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);
    Set<String> applicationIds = Collections.singleton(PolicyViolationAggregationDataHelper.APPLICATION_IDS[0]);

    Organization organization = new OrganizationDAO().getById(PolicyViolationAggregationDataHelper.ORG_ID);

    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, "2017-10", null,
        applicationIds, Collections.emptySet());

    List<ApiMetricsReportingFlattenedDTOV2> results = service.getFlattenedMetrics(queryDTO);

    assertFlattenedMonthlyData(results, organization, null, applicationIds);
  }

  @Test
  public void testGetFlattenedMetrics_ByApp_Weekly() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);
    Set<String> applicationIds = Collections.singleton(PolicyViolationAggregationDataHelper.APPLICATION_IDS[0]);

    Organization organization = new OrganizationDAO().getById(PolicyViolationAggregationDataHelper.ORG_ID);

    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.WEEK, "2017-W48", null,
        applicationIds, Collections.emptySet());

    List<ApiMetricsReportingFlattenedDTOV2> results = service.getFlattenedMetrics(queryDTO);

    assertFlattenedWeeklyData(results, organization, null, applicationIds);
  }

  @Test
  public void testGetFlattenedMetrics_NullAppsOrgs_Monthly() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    Organization organization = new OrganizationDAO().getById(PolicyViolationAggregationDataHelper.ORG_ID);

    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, "2017-10", null,
        null, null);

    List<ApiMetricsReportingFlattenedDTOV2> results = service.getFlattenedMetrics(queryDTO);

    assertFlattenedMonthlyData(results, organization, null, null);
  }

  @Test
  public void testGetFlattenedMetrics_NullAppsOrgs_Weekly() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    Organization organization = new OrganizationDAO().getById(PolicyViolationAggregationDataHelper.ORG_ID);

    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.WEEK, "2017-W48", null,
        null, null);

    List<ApiMetricsReportingFlattenedDTOV2> results = service.getFlattenedMetrics(queryDTO);

    assertFlattenedWeeklyData(results, organization, null, null);
  }

  @Test
  public void testGetFlattenedMetrics_GeneratesAggregations() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Policy policy = tempEntity.newPolicy(app.getId(), "test policy", 10);
    LocalDate now = new LocalDate();

    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan1",
        now.minusMonths(1).toDate());
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan1",
        now.minusMonths(1).plusDays(1).toDate());

    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, null, null, "unknown component");

    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, "2017-11", null,
        null, null);

    violation1.setFixTime(eval2.getTime());
    new PolicyViolationDAO().update(violation1);

    List<ApiMetricsReportingFlattenedDTOV2> results = service.getFlattenedMetrics(queryDTO);
    assertThat(results, hasSize(2));

    List<ApiMetricsReportingFlattenedDTOV2> expectedDTOs = Arrays.asList(
        new ApiMetricsReportingFlattenedDTOV2(app.getId(), app.getPublicId(), app.getName(), org.getId(), org.getName(),
            "2017-11-01", //
            null, null, null, 86400000L, //
            2, // evaluationCount
            0, 0, 0, 1, // discovered security
            0, 0, 0, 0, // discovered license
            0, 0, 0, 0, // discovered quality
            0, 0, 0, 0, // discovered other
            0, 0, 0, 1, // fixed security
            0, 0, 0, 0, // fixed license
            0, 0, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 0, 0, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            0, 0, 0, 0, // open security
            0, 0, 0, 0, // open license
            0, 0, 0, 0, // open quality
            0, 0, 0, 0  // open other
        ),
        new ApiMetricsReportingFlattenedDTOV2(app.getId(), app.getPublicId(), app.getName(), org.getId(), org.getName(),
            "2017-12-01", //
            null, null, null, null, //
            0, // evaluationCount
            0, 0, 0, 0, // discovered security
            0, 0, 0, 0, // discovered license
            0, 0, 0, 0, // discovered quality
            0, 0, 0, 0, // discovered other
            0, 0, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            0, 0, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 0, 0, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            0, 0, 0, 0, // open security
            0, 0, 0, 0, // open license
            0, 0, 0, 0, // open quality
            0, 0, 0, 0  // open other
        )
    );

    assertThat(results, hasSize(expectedDTOs.size()));

    for (int i = 0; i < results.size(); i++) {
      assertFlattenedDTO(results.get(i), expectedDTOs.get(i));
    }
  }

  private void createOtherOrgWithViolations() {
    Application application = tempEntity.newApplicationWithParent();
    DateTime now = new DateTime();

    Policy policy = tempEntity.newPolicy("policy");
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan1",
        now.minusMonths(1).toDate());
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan1",
        now.minusDays(1).toDate());

    PolicyViolation violation = tempEntity.newPolicyViolation(eval1, policy);

    violation.setFixTime(eval2.getTime());

    new PolicyViolationDAO().update(violation);
  }

  private void assertMonthlyData(List<ApiMetricsReportingDTOV2> results,
                                 Organization organization,
                                 Integer numTimePeriods,
                                 Set<String> applicationIds) {
    if (numTimePeriods == null) {
      numTimePeriods = 3;
    }

    List<ApiMetricsReportingDTOV2> expected = Arrays.asList(
        new ApiMetricsReportingDTOV2("1", "1-publicId", "app-1", organization.getId(), organization.getName(),
            Arrays.asList( //
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-10-01", //
                    null, null, null, null, // MTTRs
                    discovered().security(0, 0, 1, 0).license(0, 0, 2, 1).quality(1, 0, 0, 2).other(0, 0, 0, 3).asMap(),
                    fixed().asMap(), //
                    waived().asMap(), //
                    openWithSampleData().asMap(), //
                    1 // evaluationCount
                ),
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-11-01", //
                    1000L, 2000L, 3000L, 4000L, // MTTRs
                    discovered().security(0, 0, 2, 0).quality(3, 0, 0, 0).other(0, 0, 0, 2).asMap(), //
                    fixed().quality(1, 1, 1, 1).asMap(), //
                    waived().asMap(), //
                    openWithSampleData().asMap(), //
                    1 // evaluationCount
                ),
                new ApiMetricsReportingAggregationDTOV2(
                    "2017-12-01", //
                    3000L, 4000L, 5000L, 6000L, // MTTRs
                    discovered().security(1, 2, 3, 4).license(1, 2, 3, 4).quality(1, 2, 3, 4).other(1, 2, 3, 4).asMap(),
                    fixed().security(1, 1, 0, 0).asMap(), //
                    waived().security(0, 0, 1, 1).asMap(), //
                    openWithSampleData().asMap(), //
                    1 // evaluationCount
                )
            ).subList(0, numTimePeriods)
        ),
        new ApiMetricsReportingDTOV2("2", "2-publicId", "app-2", organization.getId(), organization.getName(),
            Arrays.asList( //
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-10-01", //
                    null, null, 5000L, null, //
                    discovered().asMap(), //
                    fixed().security(0, 0, 1, 0).asMap(), //
                    waived().asMap(), //
                    openWithSampleData().asMap(), //
                    0
                ),
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-11-01", //
                    5000L, 6000L, 7000L, 8000L, //
                    discovered().security(0, 0, 2, 0).other(0, 0, 0, 3).asMap(), //
                    fixed().asMap(), //
                    waived().quality(1, 1, 1, 1).asMap(), //
                    openWithSampleData().asMap(), //
                    2
                ),
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-12-01", //
                    5000L, 6000L, 7000L, 8000L, //
                    discovered().security(3, 4, 5, 6).license(3, 4, 5, 6).quality(3, 4, 5, 6).other(3, 4, 5, 6).asMap(),
                    fixed().security(1, 1, 0, 0).asMap(), //
                    waived().security(0, 0, 1, 1).asMap(), //
                    openWithSampleData().asMap(), //
                    3
                )
            ).subList(0, numTimePeriods)
        ),
        new ApiMetricsReportingDTOV2("3", "3-publicId", "app-3", organization.getId(), organization.getName(),
            Arrays.asList( //
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-10-01", //
                    null, null, null, null, //
                    discovered().asMap(), //
                    fixed().asMap(), //
                    waived().asMap(), //
                    openWithSampleData().asMap(), //
                    0
                ),
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-11-01", //
                    9000L, 10000L, 11000L, 12000L, //
                    discovered().security(0, 0, 2, 0).license(0, 0, 0, 3).quality(0, 0, 1, 0).other(0, 0, 0, 5).asMap(),
                    fixed().security(1, 1, 0, 0).asMap(), //
                    waived().security(0, 0, 1, 1).asMap(), //
                    openWithSampleData().asMap(), //
                    3
                ),
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-12-01", //
                    null, null, null, null, //
                    discovered().asMap(), //
                    fixed().asMap(), //
                    waived().asMap(), //
                    openWithSampleData().asMap(), //
                    0
                )
            ).subList(0, numTimePeriods)
        ),
        new ApiMetricsReportingDTOV2("4", "4-publicId", "app-4", organization.getId(), organization.getName(),
            Arrays.asList( //
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-10-01", //
                    null, null, null, null, //
                    discovered().asMap(), //
                    fixed().asMap(), //
                    waived().asMap(), //
                    openWithSampleData().asMap(), //
                    0
                ),
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-11-01", //
                    13000L, 14000L, 15000L, 16000L, //
                    discovered().security(0, 0, 2, 0).license(0, 0, 0, 1).quality(0, 0, 0, 4).other(0, 0, 0, 2).asMap(),
                    fixed().quality(1, 1, 0, 0).asMap(), //
                    waived().security(0, 0, 1, 1).asMap(), //
                    openWithSampleData().asMap(), //
                    1
                ),
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-12-01", //
                    null, null, null, null, //
                    discovered().asMap(), //
                    fixed().asMap(), //
                    waived().asMap(), //
                    openWithSampleData().asMap(), //
                    0
                )
            ).subList(0, numTimePeriods)
        ),
        new ApiMetricsReportingDTOV2("5", "5-publicId", "app-5", organization.getId(), organization.getName(),
            Arrays.asList( //
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-10-01", //
                    null, null, null, null, //
                    discovered().asMap(), //
                    fixed().asMap(), //
                    waived().asMap(), //
                    open().asMap(), //
                    0
                ),
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-11-01", //
                    null, null, null, null, //
                    discovered().asMap(), //
                    fixed().asMap(), //
                    waived().asMap(), //
                    open().asMap(), //
                    0
                ),
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-12-01", //
                    null, null, null, null, //
                    discovered().asMap(), //
                    fixed().asMap(), //
                    waived().asMap(), //
                    open().asMap(), //
                    0
                )
            ).subList(0, numTimePeriods)
        )
        // no aggregations for app 6
    );

    assertDTOs(results, expected, applicationIds);
  }

  private void assertFlattenedMonthlyData(List<ApiMetricsReportingFlattenedDTOV2> results,
                                          Organization organization,
                                          Integer numTimePeriods,
                                          Set<String> applicationIds)
  {
    if (numTimePeriods == null) {
      numTimePeriods = 3;
    }

    List<ApiMetricsReportingFlattenedDTOV2> app1Expected = Arrays.asList(
        new ApiMetricsReportingFlattenedDTOV2("1", "1-publicId", "app-1", organization.getId(), organization.getName(),
            "2017-10-01", //
            null, null, null, null, // MTTRs
            1, // evaluationCount
            0, 0, 1, 0, // discovered security
            0, 0, 2, 1, // discovered license
            1, 0, 0, 2, // discovered quality
            0, 0, 0, 3, // discovered other
            0, 0, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            0, 0, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 0, 0, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            1, 0, 1, 1, // open security
            0, 3, 2, 1, // open license
            5, 0, 0, 0, // open quality
            0, 3, 0, 2  // open other
        ),
        new ApiMetricsReportingFlattenedDTOV2("1", "1-publicId", "app-1", organization.getId(), organization.getName(),
            "2017-11-01", //
            1000L, 2000L, 3000L, 4000L, // MTTRs
            1, // evaluationCount
            0, 0, 2, 0, // discovered security
            0, 0, 0, 0, // discovered license
            3, 0, 0, 0, // discovered quality
            0, 0, 0, 2, // discovered other
            0, 0, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            1, 1, 1, 1, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 0, 0, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            1, 0, 1, 1, // open security
            0, 3, 2, 1, // open license
            5, 0, 0, 0, // open quality
            0, 3, 0, 2  // open other
        ),
        new ApiMetricsReportingFlattenedDTOV2("1", "1-publicId", "app-1", organization.getId(), organization.getName(),
            "2017-12-01", //
            3000L, 4000L, 5000L, 6000L, // MTTRs
            1, // evaluationCount
            1, 2, 3, 4, // discovered security
            1, 2, 3, 4, // discovered license
            1, 2, 3, 4, // discovered quality
            1, 2, 3, 4, // discovered other
            1, 1, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            0, 0, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 1, 1, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            1, 0, 1, 1, // open security
            0, 3, 2, 1, // open license
            5, 0, 0, 0, // open quality
            0, 3, 0, 2  // open other
          )
      ).subList(0, numTimePeriods);


    List<ApiMetricsReportingFlattenedDTOV2> app2Expected = Arrays.asList(
        new ApiMetricsReportingFlattenedDTOV2("2", "2-publicId", "app-2", organization.getId(), organization.getName(),
            "2017-10-01", //
            null, null, 5000L, null, //
            0, //
            0, 0, 0, 0, // discovered security
            0, 0, 0, 0, // discovered license
            0, 0, 0, 0, // discovered quality
            0, 0, 0, 0, // discovered other
            0, 0, 1, 0, // fixed security
            0, 0, 0, 0, // fixed license
            0, 0, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 0, 0, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            1, 0, 1, 1, // open security
            0, 3, 2, 1, // open license
            5, 0, 0, 0, // open quality
            0, 3, 0, 2  // open other
        ),
        new ApiMetricsReportingFlattenedDTOV2("2", "2-publicId", "app-2", organization.getId(), organization.getName(),
            "2017-11-01", //
            5000L, 6000L, 7000L, 8000L, //
            2, //
            0, 0, 2, 0, // discovered security
            0, 0, 0, 0, // discovered license
            0, 0, 0, 0, // discovered quality
            0, 0, 0, 3, // discovered other
            0, 0, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            0, 0, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 0, 0, // waived security
            0, 0, 0, 0, // waived license
            1, 1, 1, 1, // waived quality
            0, 0, 0, 0, // waived other
            1, 0, 1, 1, // open security
            0, 3, 2, 1, // open license
            5, 0, 0, 0, // open quality
            0, 3, 0, 2  // open other
        ),
        new ApiMetricsReportingFlattenedDTOV2("2", "2-publicId", "app-2", organization.getId(), organization.getName(),
            "2017-12-01", //
            5000L, 6000L, 7000L, 8000L, //
            3, //
            3, 4, 5, 6, // discovered security
            3, 4, 5, 6, // discovered license
            3, 4, 5, 6, // discovered quality
            3, 4, 5, 6, // discovered other
            1, 1, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            0, 0, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 1, 1, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            1, 0, 1, 1, // open security
            0, 3, 2, 1, // open license
            5, 0, 0, 0, // open quality
            0, 3, 0, 2  // open other
        )
    ).subList(0, numTimePeriods);

    List<ApiMetricsReportingFlattenedDTOV2> app3Expected = Arrays.asList(
        new ApiMetricsReportingFlattenedDTOV2("3", "3-publicId", "app-3", organization.getId(), organization.getName(),
            "2017-10-01", //
            null, null, null, null, //
            0, //
            0, 0, 0, 0, // discovered security
            0, 0, 0, 0, // discovered license
            0, 0, 0, 0, // discovered quality
            0, 0, 0, 0, // discovered other
            0, 0, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            0, 0, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 0, 0, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            1, 0, 1, 1, // open security
            0, 3, 2, 1, // open license
            5, 0, 0, 0, // open quality
            0, 3, 0, 2  // open other
        ),
        new ApiMetricsReportingFlattenedDTOV2("3", "3-publicId", "app-3", organization.getId(), organization.getName(),
            "2017-11-01", //
            9000L, 10000L, 11000L, 12000L, //
            3, //
            0, 0, 2, 0, // discovered security
            0, 0, 0, 3, // discovered license
            0, 0, 1, 0, // discovered quality
            0, 0, 0, 5, // discovered other
            1, 1, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            0, 0, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 1, 1, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            1, 0, 1, 1, // open security
            0, 3, 2, 1, // open license
            5, 0, 0, 0, // open quality
            0, 3, 0, 2  // open other
        ),
        new ApiMetricsReportingFlattenedDTOV2("3", "3-publicId", "app-3", organization.getId(), organization.getName(),
            "2017-12-01", //
            null, null, null, null, //
            0, //
            0, 0, 0, 0, // discovered security
            0, 0, 0, 0, // discovered license
            0, 0, 0, 0, // discovered quality
            0, 0, 0, 0, // discovered other
            0, 0, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            0, 0, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 0, 0, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            1, 0, 1, 1, // open security
            0, 3, 2, 1, // open license
            5, 0, 0, 0, // open quality
            0, 3, 0, 2  // open other
        )
    ).subList(0, numTimePeriods);


    List<ApiMetricsReportingFlattenedDTOV2> app4Expected = Arrays.asList(
        new ApiMetricsReportingFlattenedDTOV2("4", "4-publicId", "app-4", organization.getId(), organization.getName(),
            "2017-10-01", //
            null, null, null, null, //
            0, //
            0, 0, 0, 0, // discovered security
            0, 0, 0, 0, // discovered license
            0, 0, 0, 0, // discovered quality
            0, 0, 0, 0, // discovered other
            0, 0, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            0, 0, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 0, 0, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            1, 0, 1, 1, // open security
            0, 3, 2, 1, // open license
            5, 0, 0, 0, // open quality
            0, 3, 0, 2  // open other
        ),
        new ApiMetricsReportingFlattenedDTOV2("4", "4-publicId", "app-4", organization.getId(), organization.getName(),
            "2017-11-01", //
            13000L, 14000L, 15000L, 16000L, //
            1, //
            0, 0, 2, 0, // discovered security
            0, 0, 0, 1, // discovered license
            0, 0, 0, 4, // discovered quality
            0, 0, 0, 2, // discovered other
            0, 0, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            1, 1, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 1, 1, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            1, 0, 1, 1, // open security
            0, 3, 2, 1, // open license
            5, 0, 0, 0, // open quality
            0, 3, 0, 2  // open other
        ),
        new ApiMetricsReportingFlattenedDTOV2("4", "4-publicId", "app-4", organization.getId(), organization.getName(),
            "2017-12-01", //
            null, null, null, null, //
            0, //
            0, 0, 0, 0, // discovered security
            0, 0, 0, 0, // discovered license
            0, 0, 0, 0, // discovered quality
            0, 0, 0, 0, // discovered other
            0, 0, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            0, 0, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 0, 0, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            1, 0, 1, 1, // open security
            0, 3, 2, 1, // open license
            5, 0, 0, 0, // open quality
            0, 3, 0, 2  // open other
        )
    ).subList(0, numTimePeriods);


    List<ApiMetricsReportingFlattenedDTOV2> app5Expected = Arrays.asList(
        new ApiMetricsReportingFlattenedDTOV2("5", "5-publicId", "app-5", organization.getId(), organization.getName(),
            "2017-10-01", //
            null, null, null, null, //
            0, //
            0, 0, 0, 0, // discovered security
            0, 0, 0, 0, // discovered license
            0, 0, 0, 0, // discovered quality
            0, 0, 0, 0, // discovered other
            0, 0, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            0, 0, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 0, 0, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            0, 0, 0, 0, // open security
            0, 0, 0, 0, // open license
            0, 0, 0, 0, // open quality
            0, 0, 0, 0  // open other
        ),
        new ApiMetricsReportingFlattenedDTOV2("5", "5-publicId", "app-5", organization.getId(), organization.getName(),
            "2017-11-01", //
            null, null, null, null, //
            0, //
            0, 0, 0, 0, // discovered security
            0, 0, 0, 0, // discovered license
            0, 0, 0, 0, // discovered quality
            0, 0, 0, 0, // discovered other
            0, 0, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            0, 0, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 0, 0, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            0, 0, 0, 0, // open security
            0, 0, 0, 0, // open license
            0, 0, 0, 0, // open quality
            0, 0, 0, 0  // open other
        ),
        new ApiMetricsReportingFlattenedDTOV2("5", "5-publicId", "app-5", organization.getId(), organization.getName(),
            "2017-12-01", //
            null, null, null, null, //
            0, //
            0, 0, 0, 0, // discovered security
            0, 0, 0, 0, // discovered license
            0, 0, 0, 0, // discovered quality
            0, 0, 0, 0, // discovered other
            0, 0, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            0, 0, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 0, 0, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            0, 0, 0, 0, // open security
            0, 0, 0, 0, // open license
            0, 0, 0, 0, // open quality
            0, 0, 0, 0  // open other
        )
    ).subList(0, numTimePeriods);

    // no aggregations for app 6

    Map<String, List<ApiMetricsReportingFlattenedDTOV2>> flattenedDTOsByApplicationId = getFlattenedDTOGroups(results);
    Map<String, List<ApiMetricsReportingFlattenedDTOV2>> expectedDTOsByApplicationId =
        buildExpectedDTOGroups(applicationIds, app1Expected, app2Expected, app3Expected, app4Expected, app5Expected);

    assertFlattenedDTOGroups(flattenedDTOsByApplicationId, expectedDTOsByApplicationId);
  }

  private void assertWeeklyData(List<ApiMetricsReportingDTOV2> results,
                                Organization organization,
                                Integer numTimePeriods,
                                Set<String> applicationIds)
  {
    if (numTimePeriods == null) {
      numTimePeriods = 3;
    }

    List<ApiMetricsReportingDTOV2> expected = Arrays.asList(
        new ApiMetricsReportingDTOV2("1", "1-publicId", "app-1", organization.getId(), organization.getName(),
            Arrays.asList( //
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-11-27", //
                    null, null, null, null, // MTTRs
                    discovered().security(0, 0, 1, 0).license(0, 0, 2, 1).quality(1, 0, 0, 2).other(0, 0, 0, 3).asMap(),
                    fixed().asMap(), //
                    waived().asMap(), //
                    openWithSampleData().asMap(), //
                    1 // evaluationCount
                ),
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-12-04", //
                    2000L, 3000L, 4000L, 5000L, // MTTRs
                    discovered().security(0, 0, 2, 0).quality(3, 0, 0, 0).other(0, 0, 0, 2).asMap(), //
                    fixed().quality(1, 1, 1, 1).asMap(), //
                    waived().asMap(), //
                    openWithSampleData().asMap(), //
                    1 // evaluationCount
                ),
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-12-11", //
                    4000L, 5000L, 6000L, 7000L, // MTTRs
                    discovered().security(1, 2, 3, 4).license(1, 2, 3, 4).quality(1, 2, 3, 4).other(1, 2, 3, 4).asMap(),
                    fixed().security(1, 1, 0, 0).asMap(), //
                    waived().security(0, 0, 1, 1).asMap(), //
                    openWithSampleData().asMap(), //
                    1 // evaluationCount
                )
            ).subList(0, numTimePeriods)
        ),
        new ApiMetricsReportingDTOV2("2", "2-publicId", "app-2", organization.getId(), organization.getName(),
            Arrays.asList( //
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-11-27", //
                    null, null, 2500L, null, //
                    discovered().asMap(), //
                    fixed().security(0, 0, 1, 0).asMap(), //
                    waived().asMap(), //
                    openWithSampleData().asMap(), //
                    0
                ),
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-12-04", //
                    6000L, 7000L, 8000L, 9000L, //
                    discovered().security(0, 0, 2, 0).other(0, 0, 0, 3).asMap(), //
                    fixed().asMap(), //
                    waived().quality(1, 1, 1, 1).asMap(), //
                    openWithSampleData().asMap(), //
                    2
                ),
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-12-11", //
                    6000L, 7000L, 8000L, 9000L, //
                    discovered().security(3, 4, 5, 6).license(3, 4, 5, 6).quality(3, 4, 5, 6).other(3, 4, 5, 6).asMap(),
                    fixed().security(1, 1, 0, 0).asMap(), //
                    waived().security(0, 0, 1, 1).asMap(), //
                    openWithSampleData().asMap(), //
                    3
                )
            ).subList(0, numTimePeriods)
        ),
        new ApiMetricsReportingDTOV2("3", "3-publicId", "app-3", organization.getId(), organization.getName(),
            Arrays.asList( //
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-11-27", //
                    null, null, null, null, //
                    discovered().asMap(), //
                    fixed().asMap(), //
                    waived().asMap(), //
                    openWithSampleData().asMap(), //
                    0
                ),
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-12-04", //
                    10000L, 11000L, 12000L, 13000L, //
                    discovered().security(0, 0, 2, 0).license(0, 0, 0, 3).quality(0, 0, 1, 0).other(0, 0, 0, 5).asMap(),
                    fixed().security(1, 1, 0, 0).asMap(), //
                    waived().security(0, 0, 1, 1).asMap(), //
                    openWithSampleData().asMap(), //
                    3
                ),
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-12-11", //
                    null, null, null, null, //
                    discovered().asMap(), //
                    fixed().asMap(), //
                    waived().asMap(), //
                    openWithSampleData().asMap(), //
                    0
                )
            ).subList(0, numTimePeriods)
        ),
        new ApiMetricsReportingDTOV2("4", "4-publicId", "app-4", organization.getId(), organization.getName(),
            Arrays.asList( //
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-11-27", //
                    null, null, null, null, //
                    discovered().asMap(), //
                    fixed().asMap(), //
                    waived().asMap(), //
                    openWithSampleData().asMap(), //
                    0
                ),
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-12-04", //
                    14000L, 15000L, 16000L, 17000L, //
                    discovered().security(0, 0, 2, 0).license(0, 0, 0, 1).quality(0, 0, 0, 4).other(0, 0, 0, 2).asMap(),
                    fixed().quality(1, 1, 0, 0).asMap(), //
                    waived().security(0, 0, 1, 1).asMap(), //
                    openWithSampleData().asMap(), //
                    1
                ),
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-12-11", //
                    null, null, null, null, //
                    discovered().asMap(), //
                    fixed().asMap(), //
                    waived().asMap(), //
                    openWithSampleData().asMap(), //
                    0
                )
            ).subList(0, numTimePeriods)
        ),
        new ApiMetricsReportingDTOV2("5", "5-publicId", "app-5", organization.getId(), organization.getName(),
            Arrays.asList( //
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-11-27", //
                    null, null, null, null, //
                    discovered().asMap(), //
                    fixed().asMap(), //
                    waived().asMap(), //
                    open().asMap(), //
                    0
                ),
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-12-04", //
                    null, null, null, null, //
                    discovered().asMap(), //
                    fixed().asMap(), //
                    waived().asMap(), //
                    open().asMap(), //
                    0
                ),
                new ApiMetricsReportingAggregationDTOV2( //
                    "2017-12-11", //
                    null, null, null, null, //
                    discovered().asMap(), //
                    fixed().asMap(), //
                    waived().asMap(), //
                    open().asMap(), //
                    0
                )
            ).subList(0, numTimePeriods)
        )
        // no aggregations for app 6
    );

    assertDTOs(results, expected, applicationIds);
  }

  private void assertFlattenedWeeklyData(List<ApiMetricsReportingFlattenedDTOV2> results,
                                 Organization organization,
                                 Integer numTimePeriods,
                                 Set<String> applicationIds) {
    if (numTimePeriods == null) {
      numTimePeriods = 3;
    }

    List<ApiMetricsReportingFlattenedDTOV2> app1Expected = Arrays.asList(
        new ApiMetricsReportingFlattenedDTOV2("1", "1-publicId", "app-1", organization.getId(), organization.getName(),
            "2017-11-27", //
            null, null, null, null, // MTTRs
            1, // evaluationCount
            0, 0, 1, 0, // discovered security
            0, 0, 2, 1, // discovered license
            1, 0, 0, 2, // discovered quality
            0, 0, 0, 3, // discovered other
            0, 0, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            0, 0, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 0, 0, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            1, 0, 1, 1, // open security
            0, 3, 2, 1, // open license
            5, 0, 0, 0, // open quality
            0, 3, 0, 2  // open other
        ),
        new ApiMetricsReportingFlattenedDTOV2("1", "1-publicId", "app-1", organization.getId(), organization.getName(),
            "2017-12-04", //
            2000L, 3000L, 4000L, 5000L, // MTTRs
            1, // evaluationCount
            0, 0, 2, 0, // discovered security
            0, 0, 0, 0, // discovered license
            3, 0, 0, 0, // discovered quality
            0, 0, 0, 2, // discovered other
            0, 0, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            1, 1, 1, 1, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 0, 0, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            1, 0, 1, 1, // open security
            0, 3, 2, 1, // open license
            5, 0, 0, 0, // open quality
            0, 3, 0, 2  // open other
        ),
        new ApiMetricsReportingFlattenedDTOV2("1", "1-publicId", "app-1", organization.getId(), organization.getName(),
            "2017-12-11", //
            4000L, 5000L, 6000L, 7000L, // MTTRs
            1, // evaluationCount
            1, 2, 3, 4, // discovered security
            1, 2, 3, 4, // discovered license
            1, 2, 3, 4, // discovered quality
            1, 2, 3, 4, // discovered other
            1, 1, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            0, 0, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 1, 1, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            1, 0, 1, 1, // open security
            0, 3, 2, 1, // open license
            5, 0, 0, 0, // open quality
            0, 3, 0, 2  // open other
          )
      ).subList(0, numTimePeriods);


    List<ApiMetricsReportingFlattenedDTOV2> app2Expected = Arrays.asList(
        new ApiMetricsReportingFlattenedDTOV2("2", "2-publicId", "app-2", organization.getId(), organization.getName(),
            "2017-11-27", //
            null, null, 2500L, null, //
            0, //
            0, 0, 0, 0, // discovered security
            0, 0, 0, 0, // discovered license
            0, 0, 0, 0, // discovered quality
            0, 0, 0, 0, // discovered other
            0, 0, 1, 0, // fixed security
            0, 0, 0, 0, // fixed license
            0, 0, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 0, 0, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            1, 0, 1, 1, // open security
            0, 3, 2, 1, // open license
            5, 0, 0, 0, // open quality
            0, 3, 0, 2  // open other
        ),
        new ApiMetricsReportingFlattenedDTOV2("2", "2-publicId", "app-2", organization.getId(), organization.getName(),
            "2017-12-04", //
            6000L, 7000L, 8000L, 9000L, //
            2, //
            0, 0, 2, 0, // discovered security
            0, 0, 0, 0, // discovered license
            0, 0, 0, 0, // discovered quality
            0, 0, 0, 3, // discovered other
            0, 0, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            0, 0, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 0, 0, // waived security
            0, 0, 0, 0, // waived license
            1, 1, 1, 1, // waived quality
            0, 0, 0, 0, // waived other
            1, 0, 1, 1, // open security
            0, 3, 2, 1, // open license
            5, 0, 0, 0, // open quality
            0, 3, 0, 2  // open other
        ),
        new ApiMetricsReportingFlattenedDTOV2("2", "2-publicId", "app-2", organization.getId(), organization.getName(),
            "2017-12-11", //
            6000L, 7000L, 8000L, 9000L, //
            3, //
            3, 4, 5, 6, // discovered security
            3, 4, 5, 6, // discovered license
            3, 4, 5, 6, // discovered quality
            3, 4, 5, 6, // discovered other
            1, 1, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            0, 0, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 1, 1, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            1, 0, 1, 1, // open security
            0, 3, 2, 1, // open license
            5, 0, 0, 0, // open quality
            0, 3, 0, 2  // open other
        )
    ).subList(0, numTimePeriods);

    List<ApiMetricsReportingFlattenedDTOV2> app3Expected = Arrays.asList(
        new ApiMetricsReportingFlattenedDTOV2("3", "3-publicId", "app-3", organization.getId(), organization.getName(),
            "2017-11-27", //
            null, null, null, null, //
            0, //
            0, 0, 0, 0, // discovered security
            0, 0, 0, 0, // discovered license
            0, 0, 0, 0, // discovered quality
            0, 0, 0, 0, // discovered other
            0, 0, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            0, 0, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 0, 0, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            1, 0, 1, 1, // open security
            0, 3, 2, 1, // open license
            5, 0, 0, 0, // open quality
            0, 3, 0, 2  // open other
        ),
        new ApiMetricsReportingFlattenedDTOV2("3", "3-publicId", "app-3", organization.getId(), organization.getName(),
            "2017-12-04", //
            10000L, 11000L, 12000L, 13000L, //
            3, //
            0, 0, 2, 0, // discovered security
            0, 0, 0, 3, // discovered license
            0, 0, 1, 0, // discovered quality
            0, 0, 0, 5, // discovered other
            1, 1, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            0, 0, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 1, 1, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            1, 0, 1, 1, // open security
            0, 3, 2, 1, // open license
            5, 0, 0, 0, // open quality
            0, 3, 0, 2  // open other
        ),
        new ApiMetricsReportingFlattenedDTOV2("3", "3-publicId", "app-3", organization.getId(), organization.getName(),
            "2017-12-11", //
            null, null, null, null, //
            0, //
            0, 0, 0, 0, // discovered security
            0, 0, 0, 0, // discovered license
            0, 0, 0, 0, // discovered quality
            0, 0, 0, 0, // discovered other
            0, 0, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            0, 0, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 0, 0, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            1, 0, 1, 1, // open security
            0, 3, 2, 1, // open license
            5, 0, 0, 0, // open quality
            0, 3, 0, 2  // open other
        )
    ).subList(0, numTimePeriods);


    List<ApiMetricsReportingFlattenedDTOV2> app4Expected = Arrays.asList(
        new ApiMetricsReportingFlattenedDTOV2("4", "4-publicId", "app-4", organization.getId(), organization.getName(),
            "2017-11-27", //
            null, null, null, null, //
            0, //
            0, 0, 0, 0, // discovered security
            0, 0, 0, 0, // discovered license
            0, 0, 0, 0, // discovered quality
            0, 0, 0, 0, // discovered other
            0, 0, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            0, 0, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 0, 0, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            1, 0, 1, 1, // open security
            0, 3, 2, 1, // open license
            5, 0, 0, 0, // open quality
            0, 3, 0, 2  // open other
        ),
        new ApiMetricsReportingFlattenedDTOV2("4", "4-publicId", "app-4", organization.getId(), organization.getName(),
            "2017-12-04", //
            14000L, 15000L, 16000L, 17000L, //
            1, //
            0, 0, 2, 0, // discovered security
            0, 0, 0, 1, // discovered license
            0, 0, 0, 4, // discovered quality
            0, 0, 0, 2, // discovered other
            0, 0, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            1, 1, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 1, 1, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            1, 0, 1, 1, // open security
            0, 3, 2, 1, // open license
            5, 0, 0, 0, // open quality
            0, 3, 0, 2  // open other
        ),
        new ApiMetricsReportingFlattenedDTOV2("4", "4-publicId", "app-4", organization.getId(), organization.getName(),
            "2017-12-11", //
            null, null, null, null, //
            0, //
            0, 0, 0, 0, // discovered security
            0, 0, 0, 0, // discovered license
            0, 0, 0, 0, // discovered quality
            0, 0, 0, 0, // discovered other
            0, 0, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            0, 0, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 0, 0, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            1, 0, 1, 1, // open security
            0, 3, 2, 1, // open license
            5, 0, 0, 0, // open quality
            0, 3, 0, 2  // open other
        )
    ).subList(0, numTimePeriods);


    List<ApiMetricsReportingFlattenedDTOV2> app5Expected = Arrays.asList(
        new ApiMetricsReportingFlattenedDTOV2("5", "5-publicId", "app-5", organization.getId(), organization.getName(),
            "2017-11-27", //
            null, null, null, null, //
            0, //
            0, 0, 0, 0, // discovered security
            0, 0, 0, 0, // discovered license
            0, 0, 0, 0, // discovered quality
            0, 0, 0, 0, // discovered other
            0, 0, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            0, 0, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 0, 0, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            0, 0, 0, 0, // open security
            0, 0, 0, 0, // open license
            0, 0, 0, 0, // open quality
            0, 0, 0, 0  // open other
        ),
        new ApiMetricsReportingFlattenedDTOV2("5", "5-publicId", "app-5", organization.getId(), organization.getName(),
            "2017-12-04", //
            null, null, null, null, //
            0, //
            0, 0, 0, 0, // discovered security
            0, 0, 0, 0, // discovered license
            0, 0, 0, 0, // discovered quality
            0, 0, 0, 0, // discovered other
            0, 0, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            0, 0, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 0, 0, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            0, 0, 0, 0, // open security
            0, 0, 0, 0, // open license
            0, 0, 0, 0, // open quality
            0, 0, 0, 0  // open other
        ),
        new ApiMetricsReportingFlattenedDTOV2("5", "5-publicId", "app-5", organization.getId(), organization.getName(),
            "2017-12-11", //
            null, null, null, null, //
            0, //
            0, 0, 0, 0, // discovered security
            0, 0, 0, 0, // discovered license
            0, 0, 0, 0, // discovered quality
            0, 0, 0, 0, // discovered other
            0, 0, 0, 0, // fixed security
            0, 0, 0, 0, // fixed license
            0, 0, 0, 0, // fixed quality
            0, 0, 0, 0, // fixed other
            0, 0, 0, 0, // waived security
            0, 0, 0, 0, // waived license
            0, 0, 0, 0, // waived quality
            0, 0, 0, 0, // waived other
            0, 0, 0, 0, // open security
            0, 0, 0, 0, // open license
            0, 0, 0, 0, // open quality
            0, 0, 0, 0  // open other
        )
    ).subList(0, numTimePeriods);

    // no aggregations for app 6

    Map<String, List<ApiMetricsReportingFlattenedDTOV2>> flattenedDTOsByApplicationId = getFlattenedDTOGroups(results);
    Map<String, List<ApiMetricsReportingFlattenedDTOV2>> expectedDTOsByApplicationId =
        buildExpectedDTOGroups(applicationIds, app1Expected, app2Expected, app3Expected, app4Expected, app5Expected);

    assertFlattenedDTOGroups(flattenedDTOsByApplicationId, expectedDTOsByApplicationId);
  }

  private void assertDTO(ApiMetricsReportingDTOV2 actual, ApiMetricsReportingDTOV2 expected) {
    assertThat(actual.applicationId, is(expected.applicationId));
    assertThat(actual.applicationName, is(expected.applicationName));
    assertThat(actual.applicationPublicId, is(expected.applicationPublicId));
    assertThat(actual.organizationId, is(expected.organizationId));
    assertThat(actual.organizationName, is(expected.organizationName));

    assertThat(actual.aggregations, hasSize(expected.aggregations.size()));
    for (int i = 0; i < actual.aggregations.size(); i++) {
      ApiMetricsReportingAggregationDTOV2 actualAgg = actual.aggregations.get(i);
      ApiMetricsReportingAggregationDTOV2 expectedAgg = expected.aggregations.get(i);

      assertAggregation(actualAgg, expectedAgg);
    }
  }

  private void assertAggregation(ApiMetricsReportingAggregationDTOV2 actual,
                                 ApiMetricsReportingAggregationDTOV2 expected)
  {
    assertThat(actual.timePeriodStart, is(expected.timePeriodStart));
    assertThat(actual.mttrLowThreat, is(expected.mttrLowThreat));
    assertThat(actual.mttrModerateThreat, is(expected.mttrModerateThreat));
    assertThat(actual.mttrSevereThreat, is(expected.mttrSevereThreat));
    assertThat(actual.mttrCriticalThreat, is(expected.mttrCriticalThreat));
    assertThat(actual.discoveredCounts, is(expected.discoveredCounts));
    assertThat(actual.fixedCounts, is(expected.fixedCounts));
    assertThat(actual.waivedCounts, is(expected.waivedCounts));
    assertThat(actual.evaluationCount, is(expected.evaluationCount));
    assertThat(actual.openCountsAtTimePeriodEnd, is(expected.openCountsAtTimePeriodEnd));
  }

  private void assertFlattenedDTO(ApiMetricsReportingFlattenedDTOV2 actual,
                                  ApiMetricsReportingFlattenedDTOV2 expected)
  {
    assertThat(actual.applicationId, is(expected.applicationId));
    assertThat(actual.applicationPublicId, is(expected.applicationPublicId));
    assertThat(actual.applicationName, is(expected.applicationName));
    assertThat(actual.organizationId, is(expected.organizationId));
    assertThat(actual.organizationName, is(expected.organizationName));
    assertThat(actual.timePeriodStart, is(expected.timePeriodStart));
    assertThat(actual.mttrLowThreat, is(expected.mttrLowThreat));
    assertThat(actual.mttrModerateThreat, is(expected.mttrModerateThreat));
    assertThat(actual.mttrSevereThreat, is(expected.mttrSevereThreat));
    assertThat(actual.mttrCriticalThreat, is(expected.mttrCriticalThreat));
    assertThat(actual.evaluationCount, is(expected.evaluationCount));
    assertThat(actual.discoveredCountSecurityLow, is(expected.discoveredCountSecurityLow));
    assertThat(actual.discoveredCountSecurityModerate, is(expected.discoveredCountSecurityModerate));
    assertThat(actual.discoveredCountSecuritySevere, is(expected.discoveredCountSecuritySevere));
    assertThat(actual.discoveredCountSecurityCritical, is(expected.discoveredCountSecurityCritical));
    assertThat(actual.discoveredCountLicenseLow, is(expected.discoveredCountLicenseLow));
    assertThat(actual.discoveredCountLicenseModerate, is(expected.discoveredCountLicenseModerate));
    assertThat(actual.discoveredCountLicenseSevere, is(expected.discoveredCountLicenseSevere));
    assertThat(actual.discoveredCountLicenseCritical, is(expected.discoveredCountLicenseCritical));
    assertThat(actual.discoveredCountQualityLow, is(expected.discoveredCountQualityLow));
    assertThat(actual.discoveredCountQualityModerate, is(expected.discoveredCountQualityModerate));
    assertThat(actual.discoveredCountQualitySevere, is(expected.discoveredCountQualitySevere));
    assertThat(actual.discoveredCountQualityCritical, is(expected.discoveredCountQualityCritical));
    assertThat(actual.discoveredCountOtherLow, is(expected.discoveredCountOtherLow));
    assertThat(actual.discoveredCountOtherModerate, is(expected.discoveredCountOtherModerate));
    assertThat(actual.discoveredCountOtherSevere, is(expected.discoveredCountOtherSevere));
    assertThat(actual.discoveredCountOtherCritical, is(expected.discoveredCountOtherCritical));
    assertThat(actual.fixedCountSecurityLow, is(expected.fixedCountSecurityLow));
    assertThat(actual.fixedCountSecurityModerate, is(expected.fixedCountSecurityModerate));
    assertThat(actual.fixedCountSecuritySevere, is(expected.fixedCountSecuritySevere));
    assertThat(actual.fixedCountSecurityCritical, is(expected.fixedCountSecurityCritical));
    assertThat(actual.fixedCountLicenseLow, is(expected.fixedCountLicenseLow));
    assertThat(actual.fixedCountLicenseModerate, is(expected.fixedCountLicenseModerate));
    assertThat(actual.fixedCountLicenseSevere, is(expected.fixedCountLicenseSevere));
    assertThat(actual.fixedCountLicenseCritical, is(expected.fixedCountLicenseCritical));
    assertThat(actual.fixedCountQualityLow, is(expected.fixedCountQualityLow));
    assertThat(actual.fixedCountQualityModerate, is(expected.fixedCountQualityModerate));
    assertThat(actual.fixedCountQualitySevere, is(expected.fixedCountQualitySevere));
    assertThat(actual.fixedCountQualityCritical, is(expected.fixedCountQualityCritical));
    assertThat(actual.fixedCountOtherLow, is(expected.fixedCountOtherLow));
    assertThat(actual.fixedCountOtherModerate, is(expected.fixedCountOtherModerate));
    assertThat(actual.fixedCountOtherSevere, is(expected.fixedCountOtherSevere));
    assertThat(actual.fixedCountOtherCritical, is(expected.fixedCountOtherCritical));
    assertThat(actual.waivedCountSecurityLow, is(expected.waivedCountSecurityLow));
    assertThat(actual.waivedCountSecurityModerate, is(expected.waivedCountSecurityModerate));
    assertThat(actual.waivedCountSecuritySevere, is(expected.waivedCountSecuritySevere));
    assertThat(actual.waivedCountSecurityCritical, is(expected.waivedCountSecurityCritical));
    assertThat(actual.waivedCountLicenseLow, is(expected.waivedCountLicenseLow));
    assertThat(actual.waivedCountLicenseModerate, is(expected.waivedCountLicenseModerate));
    assertThat(actual.waivedCountLicenseSevere, is(expected.waivedCountLicenseSevere));
    assertThat(actual.waivedCountLicenseCritical, is(expected.waivedCountLicenseCritical));
    assertThat(actual.waivedCountQualityLow, is(expected.waivedCountQualityLow));
    assertThat(actual.waivedCountQualityModerate, is(expected.waivedCountQualityModerate));
    assertThat(actual.waivedCountQualitySevere, is(expected.waivedCountQualitySevere));
    assertThat(actual.waivedCountQualityCritical, is(expected.waivedCountQualityCritical));
    assertThat(actual.waivedCountOtherLow, is(expected.waivedCountOtherLow));
    assertThat(actual.waivedCountOtherModerate, is(expected.waivedCountOtherModerate));
    assertThat(actual.waivedCountOtherSevere, is(expected.waivedCountOtherSevere));
    assertThat(actual.waivedCountOtherCritical, is(expected.waivedCountOtherCritical));
    assertThat(actual.openCountAtTimePeriodEndSecurityLow, is(expected.openCountAtTimePeriodEndSecurityLow));
    assertThat(actual.openCountAtTimePeriodEndSecurityModerate, is(expected.openCountAtTimePeriodEndSecurityModerate));
    assertThat(actual.openCountAtTimePeriodEndSecuritySevere, is(expected.openCountAtTimePeriodEndSecuritySevere));
    assertThat(actual.openCountAtTimePeriodEndSecurityCritical, is(expected.openCountAtTimePeriodEndSecurityCritical));
    assertThat(actual.openCountAtTimePeriodEndLicenseLow, is(expected.openCountAtTimePeriodEndLicenseLow));
    assertThat(actual.openCountAtTimePeriodEndLicenseModerate, is(expected.openCountAtTimePeriodEndLicenseModerate));
    assertThat(actual.openCountAtTimePeriodEndLicenseSevere, is(expected.openCountAtTimePeriodEndLicenseSevere));
    assertThat(actual.openCountAtTimePeriodEndLicenseCritical, is(expected.openCountAtTimePeriodEndLicenseCritical));
    assertThat(actual.openCountAtTimePeriodEndQualityLow, is(expected.openCountAtTimePeriodEndQualityLow));
    assertThat(actual.openCountAtTimePeriodEndQualityModerate, is(expected.openCountAtTimePeriodEndQualityModerate));
    assertThat(actual.openCountAtTimePeriodEndQualitySevere, is(expected.openCountAtTimePeriodEndQualitySevere));
    assertThat(actual.openCountAtTimePeriodEndQualityCritical, is(expected.openCountAtTimePeriodEndQualityCritical));
    assertThat(actual.openCountAtTimePeriodEndOtherLow, is(expected.openCountAtTimePeriodEndOtherLow));
    assertThat(actual.openCountAtTimePeriodEndOtherModerate, is(expected.openCountAtTimePeriodEndOtherModerate));
    assertThat(actual.openCountAtTimePeriodEndOtherSevere, is(expected.openCountAtTimePeriodEndOtherSevere));
    assertThat(actual.openCountAtTimePeriodEndOtherCritical, is(expected.openCountAtTimePeriodEndOtherCritical));
  }

  /**
   * The flattened DTO list is supposed to be grouped by application id though not necessarily sorted by application
   * id. This method ensures that that is true and separates the groups by application, returning the separated
   * lists in a map indexed by application id
   */
  private Map<String, List<ApiMetricsReportingFlattenedDTOV2>> getFlattenedDTOGroups(List<ApiMetricsReportingFlattenedDTOV2> dtos) {
    // applicationIds that have already been seen not including the current one
    Set<String> idsAlreadySeen = new HashSet<>();
    String currentAppId = null;

    Map<String, List<ApiMetricsReportingFlattenedDTOV2>> retval = new HashMap<>();

    for (ApiMetricsReportingFlattenedDTOV2 dto : dtos) {
      if (!dto.applicationId.equals(currentAppId)) {
        if (currentAppId != null) {
          idsAlreadySeen.add(currentAppId);
        }

        currentAppId = dto.applicationId;
      }

      // if it's an id that we've already seen then this item fails the grouping requirement
      assertThat(idsAlreadySeen, not(contains(currentAppId)));

      retval.computeIfAbsent(currentAppId, k -> new ArrayList<>()).add(dto);
    }

    return retval;
  }

  private Map<String, List<ApiMetricsReportingFlattenedDTOV2>> buildExpectedDTOGroups(Set<String> applicationIds,
                                                                                      List<ApiMetricsReportingFlattenedDTOV2> app1Expected,
                                                                                      List<ApiMetricsReportingFlattenedDTOV2> app2Expected,
                                                                                      List<ApiMetricsReportingFlattenedDTOV2> app3Expected,
                                                                                      List<ApiMetricsReportingFlattenedDTOV2> app4Expected,
                                                                                      List<ApiMetricsReportingFlattenedDTOV2> app5Expected)
  {
    Map<String, List<ApiMetricsReportingFlattenedDTOV2>> expectedDTOsByApplicationId = new HashMap<>();

    if (applicationIds == null || applicationIds.contains(PolicyViolationAggregationDataHelper.APPLICATION_IDS[0])) {
      expectedDTOsByApplicationId.put(PolicyViolationAggregationDataHelper.APPLICATION_IDS[0], app1Expected);
    }
    if (applicationIds == null || applicationIds.contains(PolicyViolationAggregationDataHelper.APPLICATION_IDS[1])) {
      expectedDTOsByApplicationId.put(PolicyViolationAggregationDataHelper.APPLICATION_IDS[1], app2Expected);
    }
    if (applicationIds == null || applicationIds.contains(PolicyViolationAggregationDataHelper.APPLICATION_IDS[2])) {
      expectedDTOsByApplicationId.put(PolicyViolationAggregationDataHelper.APPLICATION_IDS[2], app3Expected);
    }
    if (applicationIds == null || applicationIds.contains(PolicyViolationAggregationDataHelper.APPLICATION_IDS[3])) {
      expectedDTOsByApplicationId.put(PolicyViolationAggregationDataHelper.APPLICATION_IDS[3], app4Expected);
    }
    if (applicationIds == null || applicationIds.contains(PolicyViolationAggregationDataHelper.APPLICATION_IDS[4])) {
      expectedDTOsByApplicationId.put(PolicyViolationAggregationDataHelper.APPLICATION_IDS[4], app5Expected);
    }

    return expectedDTOsByApplicationId;
  }

  private void assertDTOs(List<ApiMetricsReportingDTOV2> actual,
                          List<ApiMetricsReportingDTOV2> expected,
                          Set<String> applicationIds)
  {
    if (applicationIds != null) {
      expected = expected.stream().filter(dto -> applicationIds.contains(dto.applicationId))
          .collect(Collectors.toList());
    }

    assertThat(actual, hasSize(expected.size()));

    for (ApiMetricsReportingDTOV2 expectedDTO : expected) {
      // results have no guaranteed application ordering
      ApiMetricsReportingDTOV2 matchingResult = actual.stream()
          .filter(r -> r.applicationId.equals(expectedDTO.applicationId))
          .findFirst().orElse(null);

      assertThat(matchingResult, is(notNullValue()));

      assertDTO(matchingResult, expectedDTO);
    }
  }

  private void assertFlattenedDTOGroups(Map<String, List<ApiMetricsReportingFlattenedDTOV2>> actual,
                                        Map<String, List<ApiMetricsReportingFlattenedDTOV2>> expected)
  {
    assertThat(actual.keySet(), hasSize(expected.size()));
    for (Map.Entry<String, List<ApiMetricsReportingFlattenedDTOV2>> entry : expected.entrySet()) {
      String applicationId = entry.getKey();
      List<ApiMetricsReportingFlattenedDTOV2> expectedDTOGroup = entry.getValue();
      List<ApiMetricsReportingFlattenedDTOV2> actualDTOGroup = actual.get(applicationId);

      assertThat(actualDTOGroup, hasSize(expectedDTOGroup.size()));
      for (int i = 0; i < actualDTOGroup.size(); i++) {
        assertFlattenedDTO(actualDTOGroup.get(i), expectedDTOGroup.get(i));
      }
    }
  }
}
