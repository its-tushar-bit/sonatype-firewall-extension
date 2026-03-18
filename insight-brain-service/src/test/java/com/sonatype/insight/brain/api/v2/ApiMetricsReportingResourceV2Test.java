/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiMetricsReportingQueryDTOV2;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.successmetrics.TimePeriod;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsTestUtils;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsTestUtils.FakeDateRule;

import org.apache.commons.io.IOUtils;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiMetricsReportingResourceV2Test
    extends AbstractResourceTest
{
  private LocalDate today;

  private Application app;

  @Rule
  public FakeDateRule fakeDateRule = new FakeDateRule();

  private SuccessMetricsTestUtils successMetricsTestUtils;

  @Before
  public void setup() {
    today = new LocalDate();
    Organization org = tempEntity.newOrganizationWithSpecificId("orgId", "orgName");
    app = tempEntity.newApplicationWithSpecificId("appId", "appName", "appPublicId", org.getId());

    PolicyViolationDAO policyViolationDAO = lookup(PolicyViolationDAO.class);
    successMetricsTestUtils = new SuccessMetricsTestUtils(policyViolationDAO);
    successMetricsTestUtils.createPolicyViolation(app, today, tempEntity);
  }

  @After
  public void after() {
    ApiMetricsReportingResourceV2.chunkSize = ApiMetricsReportingResourceV2.DEFAULT_CHUNK_SIZE;
  }

  @Test
  public void testJsonApi() throws Exception {
    HttpResponse response = createRequest("application/json").post();
    assertResponseStatus(200, response);

    List<Map<String, Object>> results = response.getBodyList();
    assertThat(results).hasSize(1);

    Map<String, Object> result = results.get(0);
    assertThat(result.get("applicationId")).isEqualTo(app.getId());

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> aggregations = (List<Map<String, Object>>) result.get("aggregations");
    assertThat(aggregations).hasSize(1);
    assertThat(aggregations.get(0).get("timePeriodStart")).isEqualTo("2017-11-01");
  }

  @Test
  public void testJsonApi_NullQuery() throws Exception {
    HttpResponse httpResponse = restRequest().path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2)
        .path(ApiMetricsReportingResourceV2.PATH)
        .header("Accept", "application/json")
        .body(null)
        .post();

    assertResponseStatus(400, httpResponse);
    assertThat(httpResponse.getBodyText()).isEqualTo("Request parameters must be defined");
  }

  @Test
  public void testJsonApi_NullTimePeriod() throws Exception {
    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(null, "2018-02", "2018-02",
        Collections.emptySet(), Collections.emptySet());

    HttpResponse httpResponse = restRequest().path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2)
        .path(ApiMetricsReportingResourceV2.PATH)
        .header("Accept", "application/json")
        .body(queryDTO)
        .post();

    assertResponseStatus(400, httpResponse);
    assertThat(httpResponse.getBodyText()).isEqualTo("timePeriod must be defined");
  }

  @Test
  public void testJsonApi_NullFirstTimePeriod() throws Exception {
    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, null, "2018-02",
        Collections.emptySet(), Collections.emptySet());

    HttpResponse httpResponse = restRequest().path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2)
        .path(ApiMetricsReportingResourceV2.PATH)
        .header("Accept", "application/json")
        .body(queryDTO)
        .post();

    assertResponseStatus(400, httpResponse);
    assertThat(httpResponse.getBodyText()).isEqualTo("firstTimePeriod must be defined");
  }

  @Test
  public void testJsonApi_LastTimePeriodBeforeFirst() throws Exception {
    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, "2018-03", "2018-02",
        Collections.emptySet(), Collections.emptySet());

    HttpResponse httpResponse = restRequest().path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2)
        .path(ApiMetricsReportingResourceV2.PATH)
        .header("Accept", "application/json")
        .body(queryDTO)
        .post();

    assertResponseStatus(400, httpResponse);
    assertThat(httpResponse.getBodyText()).isEqualTo("lastTimePeriod must not be before firstTimePeriod");
  }

  @Test
  public void testCsvApi_NullQuery() throws Exception {
    HttpResponse httpResponse = restRequest().path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2)
        .path(ApiMetricsReportingResourceV2.PATH)
        .header("Accept", "text/csv")
        .body(null)
        .post();

    assertResponseStatus(400, httpResponse);
    assertThat(httpResponse.getBodyText()).isEqualTo("Request parameters must be defined");
  }

  @Test
  public void testCsvApi_NullTimePeriod() throws Exception {
    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(null, "2018-02", "2018-02",
        Collections.emptySet(), Collections.emptySet());

    HttpResponse httpResponse = restRequest().path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2)
        .path(ApiMetricsReportingResourceV2.PATH)
        .header("Accept", "text/csv")
        .body(queryDTO)
        .post();

    assertResponseStatus(400, httpResponse);
    assertThat(httpResponse.getBodyText()).isEqualTo("timePeriod must be defined");
  }

  @Test
  public void testCsvApi_NullFirstTimePeriod() throws Exception {
    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, null, "2018-02",
        Collections.emptySet(), Collections.emptySet());

    HttpResponse httpResponse = restRequest().path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2)
        .path(ApiMetricsReportingResourceV2.PATH)
        .header("Accept", "text/csv")
        .body(queryDTO)
        .post();

    assertResponseStatus(400, httpResponse);
    assertThat(httpResponse.getBodyText()).isEqualTo("firstTimePeriod must be defined");
  }

  @Test
  public void testCsvApi_LastTimePeriodBeforeFirst() throws Exception {
    ApiMetricsReportingQueryDTOV2 queryDTO = new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, "2018-03", "2018-02",
        Collections.emptySet(), Collections.emptySet());

    HttpResponse httpResponse = restRequest().path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2)
        .path(ApiMetricsReportingResourceV2.PATH)
        .header("Accept", "text/csv")
        .body(queryDTO)
        .post();

    assertResponseStatus(400, httpResponse);
    assertThat(httpResponse.getBodyText()).isEqualTo("lastTimePeriod must not be before firstTimePeriod");
  }

  @Test
  public void testJsonApi_Multiple() throws Exception {
    successMetricsTestUtils.createPolicyViolation(tempEntity.newApplicationWithParent(), today, tempEntity);
    ApiMetricsReportingQueryDTOV2 queryDto = new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, "2017-11", "2017-11",
        null, null);

    ApiMetricsReportingResourceV2.chunkSize = 1;
    HttpResponse response = restRequest().path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2)
        .path(ApiMetricsReportingResourceV2.PATH)
        .header("Accept", "application/json")
        .body(queryDto)
        .post();

    List<Map<String, Object>> results = response.getBodyList();
    assertThat(results).hasSize(2);
  }

  @Test
  public void testCsvApi() throws Exception {
    HttpResponse response = createRequest("text/csv").post();
    assertResponseStatus(200, response);

    String responseText = response.getBodyText();
    String expectedCSV = IOUtils.toString(getClass().getResource("/ApiMetricsReportingResourceV2Test/expected.csv"),
        StandardCharsets.UTF_8).replace("\r\n", "\n");

    assertThat(responseText).isEqualTo(expectedCSV);
  }

  @Test
  public void testCsvApi_Multiple() throws Exception {
    Application app2 =
        tempEntity.newApplicationWithSpecificId("appId2", "appName2", "appPublicId2", app.getOrganizationId());
    successMetricsTestUtils.createPolicyViolation(app2, today, tempEntity);
    ApiMetricsReportingQueryDTOV2 queryDto = new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, "2017-11", "2017-11",
        null, null);

    ApiMetricsReportingResourceV2.chunkSize = 1;
    HttpResponse response = restRequest().path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2)
        .path(ApiMetricsReportingResourceV2.PATH)
        .header("Accept", "text/csv")
        .body(queryDto)
        .post();

    String responseText = response.getBodyText();
    String expectedCsv = IOUtils.toString(getClass()
        .getResource("/ApiMetricsReportingResourceV2Test/expected_multiple.csv"), StandardCharsets.UTF_8);
    assertThat(responseText).isEqualToIgnoringNewLines(expectedCsv);
  }

  private HttpRequest createRequest(String acceptType) {
    return super.restRequest() //
        .path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2) //
        .path(ApiMetricsReportingResourceV2.PATH) //
        .header("Accept", acceptType) //
        .body(makeQueryDTO());
  }

  private ApiMetricsReportingQueryDTOV2 makeQueryDTO() {
    return new ApiMetricsReportingQueryDTOV2(TimePeriod.MONTH, "2017-11", "2017-11",
        Collections.singleton(app.getId()), null);
  }
}
