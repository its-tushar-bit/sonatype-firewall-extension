/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.reachability;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.report.ApplicationReport;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.service.reachability.StoredReachabilityEvidence;
import com.sonatype.insight.brain.service.reachability.StoredReachabilityEvidence.EvidencePath;
import com.sonatype.insight.brain.service.reachability.StoredReachabilityEvidence.GapSegment;
import com.sonatype.insight.brain.service.reachability.StoredReachabilityEvidence.MethodSegment;
import com.sonatype.insight.brain.service.reachability.StoredReachabilityEvidence.VulnerabilityEvidence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.sonatype.insight.brain.api.PublicApiPaths.REACHABILITY_EVIDENCE_RESOURCE_PATH;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ApiReachabilityEvidenceResource} endpoint.
 */
public class ApiReachabilityEvidenceResourceIntegrationTest
    extends AbstractResourceTest
{
  private static final String VULNERABILITY_ID = "CVE-2023-35116";

  private static final String OTHER_VULNERABILITY_ID = "CVE-2023-99999";

  private static final String REPORT_ID = "reportId";

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Before
  public void setUp() {
    // Setup is handled by base class
  }

  @Test
  public void testGetEvidence_success() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    createReportFileWithEvidence(application, VULNERABILITY_ID);

    HttpResponse response = restRequest()
        .path(buildEvidencePath(application.getPublicId(), REPORT_ID, VULNERABILITY_ID))
        .get();

    assertResponseStatus(200, response);

    JsonNode body = objectMapper.readTree(response.getBodyText());
    assertThat(body.get("vulnerabilityId").asText()).isEqualTo(VULNERABILITY_ID);
    assertThat(body.get("truncated").asBoolean()).isFalse();

    JsonNode paths = body.get("paths");
    assertThat(paths).hasSize(1);

    JsonNode path = paths.get(0);
    JsonNode segments = path.get("segments");
    assertThat(segments).hasSize(4);

    // First segment: method (entry point)
    assertThat(segments.get(0).get("type").asText()).isEqualTo("method");
    assertThat(segments.get(0).get("method").asText())
        .isEqualTo("com.example.Main.main([Ljava/lang/String;)V");

    // Second segment: method (at boundary)
    assertThat(segments.get(1).get("type").asText()).isEqualTo("method");
    assertThat(segments.get(1).get("component").asText())
        .isEqualTo("pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.13.0");

    // Third segment: gap
    assertThat(segments.get(2).get("type").asText()).isEqualTo("gap");

    // Fourth segment: method (vulnerable)
    assertThat(segments.get(3).get("type").asText()).isEqualTo("method");
    assertThat(segments.get(3).get("method").asText())
        .isEqualTo("com.fasterxml.jackson.core.JsonParser.vulnerable()V");
    assertThat(segments.get(3).get("component").asText())
        .isEqualTo("pkg:maven/com.fasterxml.jackson.core/jackson-core@2.13.0");

    assertThat(body.get("truncated").asBoolean()).isFalse();
  }

  @Test
  public void testGetEvidence_noEvidence_returns404() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    createReportFileWithoutEvidence(application);

    HttpResponse response = restRequest()
        .path(buildEvidencePath(application.getPublicId(), REPORT_ID, VULNERABILITY_ID))
        .get();

    assertResponseStatus(404, response);
  }

  @Test
  public void testGetEvidence_vulnNotInEvidence_returns404() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    createReportFileWithEvidence(application, VULNERABILITY_ID);

    HttpResponse response = restRequest()
        .path(buildEvidencePath(application.getPublicId(), REPORT_ID, OTHER_VULNERABILITY_ID))
        .get();

    assertResponseStatus(404, response);
  }

  @Test
  public void testGetEvidence_invalidApplication_returns404() throws Exception {
    HttpResponse response = restRequest()
        .path(buildEvidencePath("nonexistent-app", REPORT_ID, VULNERABILITY_ID))
        .get();

    assertResponseStatus(404, response);
  }

  private String buildEvidencePath(String applicationPublicId, String reportId, String vulnerabilityId) {
    return String.format("%s/%s/reachability-evidence",
        REACHABILITY_EVIDENCE_RESOURCE_PATH.replace("{applicationPublicId}", applicationPublicId)
            .replace("{reportId}", reportId),
        vulnerabilityId);
  }

  private void createReportFileWithEvidence(Application application, String vulnerabilityId) throws Exception {
    InsightWork insightWork = getCLMServer().getInstance(InsightWork.class);
    ReportService reportService = getCLMServer().getInstance(ReportService.class);

    File baseReport = ReportTestUtils.zipReportDir("/ApiVulnerabilitySignatureServiceTest/report", tempDir);
    ReportTestUtils.createReportFile(application.getId(), REPORT_ID, baseReport, insightWork);

    // Store already-enriched evidence (the format ApiReachabilityEvidenceService reads)
    StoredReachabilityEvidence stored = createStoredEvidence(vulnerabilityId);
    byte[] evidenceBytes = objectMapper.writeValueAsBytes(stored);

    ApplicationReport report = reportService.getReport(application.getId(), REPORT_ID);
    report.putEntry(ApplicationReport.ReportFile.REACHABILITY_EVIDENCE_JSON.getName(), evidenceBytes);
  }

  private void createReportFileWithoutEvidence(Application application) throws Exception {
    InsightWork insightWork = getCLMServer().getInstance(InsightWork.class);

    File baseReport = ReportTestUtils.zipReportDir("/ApiVulnerabilitySignatureServiceTest/report", tempDir);
    ReportTestUtils.createReportFile(application.getId(), REPORT_ID, baseReport, insightWork);
  }

  private StoredReachabilityEvidence createStoredEvidence(String vulnerabilityId) {
    EvidencePath path = new EvidencePath(List.of(
        new MethodSegment("com.example.Main.main([Ljava/lang/String;)V", "/app/my-app.jar", null),
        new MethodSegment("com.fasterxml.jackson.databind.ObjectMapper.readValue()V",
            "/lib/jackson-databind-2.13.jar",
            "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.13.0"),
        new GapSegment(),
        new MethodSegment("com.fasterxml.jackson.core.JsonParser.vulnerable()V",
            "/lib/jackson-core-2.13.jar",
            "pkg:maven/com.fasterxml.jackson.core/jackson-core@2.13.0")));

    return new StoredReachabilityEvidence(
        Map.of(vulnerabilityId, new VulnerabilityEvidence(List.of(path), false)));
  }
}
