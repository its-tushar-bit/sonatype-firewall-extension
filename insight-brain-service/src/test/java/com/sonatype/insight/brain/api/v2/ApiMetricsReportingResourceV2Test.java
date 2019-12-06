/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiMetricsReportingQueryDTOV2;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.successmetrics.TimePeriod;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsTestUtils;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsTestUtils.FakeDateRule;

import org.apache.commons.io.IOUtils;
import org.joda.time.LocalDate;
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

  @Before
  public void setup() {
    today = new LocalDate();
    Organization org = tempEntity.newOrganizationWithSpecificId("orgId", "orgName");
    app = tempEntity.newApplicationWithSpecificId("appId", "appName", "appPublicId", org.getId());

    SuccessMetricsTestUtils.createPolicyViolation(app, today, tempEntity);
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
  public void testCsvApi() throws Exception {
    HttpResponse response = createRequest("text/csv").post();
    assertResponseStatus(200, response);

    String responseText = response.getBodyText();
    String expectedCSV = IOUtils.toString(getClass().getResource("/ApiMetricsReportingResourceV2Test/expected.csv"),
        "UTF-8").replace("\r\n", "\n");

    assertThat(responseText).isEqualTo(expectedCSV);
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
