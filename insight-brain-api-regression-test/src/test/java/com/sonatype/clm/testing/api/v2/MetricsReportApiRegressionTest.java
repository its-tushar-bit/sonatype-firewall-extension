/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.clm.testing.api.categories.ApiRegressionTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiMetricsReportingResourceV2;
import com.sonatype.insight.brain.api.v2.dto.ApiMetricsReportingQueryDTOV2;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.successmetrics.TimePeriod;

import java.util.Collections;
import java.util.Set;

import jakarta.ws.rs.core.MediaType;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * API regression suite for {@code POST /api/v2/reports/metrics}
 * ({@link ApiMetricsReportingResourceV2}). Both the JSON variant and the CSV variant
 * (routed by {@code Accept} header on the same path) are covered — they were the source of
 * a Sev-1 export outage (CLM-38045: ALB idle timeout from delayed CSV output) and a broken-
 * pipeline regression (CLM-38675: invalid {@code Content-Encoding: utf-8} header). Streaming
 * shape is not asserted here — this suite guards contract, not throughput.
 */
@Category(ApiRegressionTest.class)
public class MetricsReportApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String METRICS_PATH =
      PublicApiPaths.REPORTS_RESOURCE_PATH_V2 + ApiMetricsReportingResourceV2.PATH;

  /** Wide time range guaranteed to include any seeded application state. */
  private static final String FIRST_PERIOD = "2018-01";

  private static final String LAST_PERIOD = "2099-12";

  /**
   * The JSON and CSV variants of this endpoint are two JAX-RS methods on the same path,
   * disambiguated only by the {@code Accept} header. Without an explicit {@code Accept:
   * application/json} the resource-selection default routes to the CSV method — so we set the
   * header explicitly here, guarding against the CI-plugin regression path where clients that
   * omit {@code Accept} silently start receiving CSV bodies where they expect JSON.
   */
  @Test
  public void testJsonMetrics_success_returns200() throws Exception {
    Organization org = tempEntity.newOrganization(uniqueName("Api Metrics"));
    Application app = tempEntity.newApplication(uniqueId("api-metrics"), org.getId());
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, uniqueId("api-metrics-scan"));

    ApiMetricsReportingQueryDTOV2 query = new ApiMetricsReportingQueryDTOV2(
        TimePeriod.MONTH, FIRST_PERIOD, LAST_PERIOD,
        Set.of(app.getPublicId()), Collections.emptySet());

    HttpResponse response = apiRequest()
        .path(METRICS_PATH)
        .header("Accept", MediaType.APPLICATION_JSON)
        .body(query, MediaType.APPLICATION_JSON)
        .post();
    assertResponseStatus(200, response);
    // JSON body is a streaming top-level array (see writer.writeValuesAsArray(os) in
    // ApiMetricsReportingResourceV2#getMetrics). Success metrics require both the policy
    // evaluation and a computed metrics row (which the underlying SuccessMetricsService only
    // materializes for completed evaluations), so an empty [] is a valid response for a
    // seeded-but-not-computed app. The regression guard here is that the JSON variant is
    // routed (not the CSV variant, whose body would start with a CSV header line).
    assertThatJson(response.getBodyText()).isArray();
  }

  /**
   * Regression guard: null request body must return a specific 400 message. Silent 200 or
   * NPE-based 500 would break the "fail fast on bad input" contract that CI plugins rely on.
   */
  @Test
  public void testJsonMetrics_nullQuery_returns400() throws Exception {
    HttpResponse response = apiRequest()
        .path(METRICS_PATH)
        .header("Accept", MediaType.APPLICATION_JSON)
        .body(null, MediaType.APPLICATION_JSON)
        .post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("must be defined");
  }

  @Test
  public void testJsonMetrics_nullTimePeriod_returns400() throws Exception {
    ApiMetricsReportingQueryDTOV2 query = new ApiMetricsReportingQueryDTOV2(
        null, FIRST_PERIOD, LAST_PERIOD, Collections.emptySet(), Collections.emptySet());

    HttpResponse response = apiPostJson(METRICS_PATH, query);
    assertResponseStatus(400, response);
    // Fuller fragment so a future validation-order flip doesn't let the analogous
    // "firstTimePeriod must be defined" branch pass this test by accident.
    assertThat(response.getBodyText()).containsIgnoringCase("timePeriod must be defined");
  }

  @Test
  public void testJsonMetrics_nullFirstTimePeriod_returns400() throws Exception {
    ApiMetricsReportingQueryDTOV2 query = new ApiMetricsReportingQueryDTOV2(
        TimePeriod.MONTH, null, LAST_PERIOD, Collections.emptySet(), Collections.emptySet());

    HttpResponse response = apiPostJson(METRICS_PATH, query);
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("firstTimePeriod");
  }

  @Test
  public void testJsonMetrics_lastBeforeFirst_returns400() throws Exception {
    ApiMetricsReportingQueryDTOV2 query = new ApiMetricsReportingQueryDTOV2(
        TimePeriod.MONTH, "2020-03", "2020-02", Collections.emptySet(), Collections.emptySet());

    HttpResponse response = apiPostJson(METRICS_PATH, query);
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("must not be before");
  }

  /**
   * CSV variant is a separate JAX-RS method on the same path, routed by the {@code Accept}
   * header. Guards against regression of the CSV export streaming path (CLM-38045). Asserts
   * body shape (starts with the CSV header) so that a routing regression which silently
   * fell back to the JSON method would fail this test, not pass with 200.
   */
  @Test
  public void testCsvMetrics_success_returns200_withCsvBody() throws Exception {
    Organization org = tempEntity.newOrganization(uniqueName("Api Metrics Csv"));
    Application app = tempEntity.newApplication(uniqueId("api-metrics-csv"), org.getId());
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, uniqueId("api-metrics-csv-scan"));

    ApiMetricsReportingQueryDTOV2 query = new ApiMetricsReportingQueryDTOV2(
        TimePeriod.MONTH, FIRST_PERIOD, LAST_PERIOD,
        Set.of(app.getPublicId()), Collections.emptySet());

    HttpResponse response = apiRequest()
        .path(METRICS_PATH)
        .header("Accept", "text/csv")
        .body(query, MediaType.APPLICATION_JSON)
        .post();
    assertResponseStatus(200, response);
    // CSV header emitted by ApiMetricsReportingResourceV2#writeCsv starts with applicationId.
    // A JSON-routing regression would produce a body starting with "[", failing this check.
    assertThat(response.getBodyText()).startsWith("applicationId");
  }

  @Test
  public void testJsonMetrics_unauthenticated_returns401() throws Exception {
    ApiMetricsReportingQueryDTOV2 query = new ApiMetricsReportingQueryDTOV2(
        TimePeriod.MONTH, FIRST_PERIOD, LAST_PERIOD, Collections.emptySet(), Collections.emptySet());
    HttpResponse response = anonApiPostJson(METRICS_PATH, query);
    assertResponseStatus(401, response);
  }

  /**
   * The CSV variant is a separate JAX-RS method on the same path (routed by {@code Accept:
   * text/csv}) — its Shiro annotation could regress independently of the JSON variant, so
   * cover both explicitly. Same regression-guard rationale as the per-verb auth tests on
   * {@code ApiLegalAttributionReportTemplateResourceV2}.
   */
  @Test
  public void testCsvMetrics_unauthenticated_returns401() throws Exception {
    ApiMetricsReportingQueryDTOV2 query = new ApiMetricsReportingQueryDTOV2(
        TimePeriod.MONTH, FIRST_PERIOD, LAST_PERIOD, Collections.emptySet(), Collections.emptySet());
    HttpResponse response = anonApiRequest()
        .path(METRICS_PATH)
        .header("Accept", "text/csv")
        .body(query, MediaType.APPLICATION_JSON)
        .post();
    assertResponseStatus(401, response);
  }
}
