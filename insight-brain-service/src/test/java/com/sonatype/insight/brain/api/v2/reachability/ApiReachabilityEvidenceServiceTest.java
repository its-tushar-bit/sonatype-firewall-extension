/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.reachability;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sonatype.insight.brain.report.LifecycleReport;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.service.reachability.StoredReachabilityEvidence;
import com.sonatype.insight.brain.service.reachability.StoredReachabilityEvidence.EvidencePath;
import com.sonatype.insight.brain.service.reachability.StoredReachabilityEvidence.GapSegment;
import com.sonatype.insight.brain.service.reachability.StoredReachabilityEvidence.MethodSegment;
import com.sonatype.insight.brain.service.reachability.StoredReachabilityEvidence.PathSegment;
import com.sonatype.insight.brain.service.reachability.StoredReachabilityEvidence.VulnerabilityEvidence;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class ApiReachabilityEvidenceServiceTest
{
  private static final String APP_ID = "test-app-id";

  private static final String SCAN_ID = "test-scan-id";

  private static final String VULN_ID = "CVE-2023-35116";

  @Mock
  private ReportService reportService;

  @Mock
  private LifecycleReport report;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private ApiReachabilityEvidenceService service;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    service = new ApiReachabilityEvidenceService(reportService, objectMapper);
  }

  @Test
  public void testNoReportEntry_returnsNull() throws IOException {
    when(reportService.getReport(APP_ID, SCAN_ID)).thenReturn(report);
    when(report.getEntry(anyString())).thenReturn(null);

    assertThat(service.getEvidenceForVulnerability(APP_ID, SCAN_ID, VULN_ID)).isNull();
  }

  @Test
  public void testVulnNotInEvidence_returnsNull() throws IOException {
    when(reportService.getReport(APP_ID, SCAN_ID)).thenReturn(report);
    mockStoredEvidence(new StoredReachabilityEvidence(
        Map.of("CVE-OTHER", new VulnerabilityEvidence(Collections.emptyList(), false))));

    assertThat(service.getEvidenceForVulnerability(APP_ID, SCAN_ID, VULN_ID)).isNull();
  }

  @Test
  public void testReturnsPathSegments() throws IOException {
    when(reportService.getReport(APP_ID, SCAN_ID)).thenReturn(report);

    EvidencePath path = new EvidencePath(List.of(
        new MethodSegment("com.example.Entry.main()V", "/app.jar", null),
        new GapSegment(),
        new MethodSegment("com.vuln.Sink.bad()V", "/vuln.jar", "pkg:maven/com.vuln/vuln@1.0")));
    mockStoredEvidence(new StoredReachabilityEvidence(
        Map.of(VULN_ID, new VulnerabilityEvidence(List.of(path), true))));

    ApiReachabilityEvidenceResponse result = service.getEvidenceForVulnerability(APP_ID, SCAN_ID, VULN_ID);

    assertThat(result).isNotNull();
    assertThat(result.vulnerabilityId()).isEqualTo(VULN_ID);
    assertThat(result.paths()).hasSize(1);
    List<PathSegment> segments = result.paths().get(0).segments();
    assertThat(segments).hasSize(3);
    assertThat(segments.get(0)).isInstanceOf(MethodSegment.class);
    assertThat(segments.get(1)).isInstanceOf(GapSegment.class);
    assertThat(segments.get(2)).isInstanceOf(MethodSegment.class);
    assertThat(((MethodSegment) segments.get(2)).component()).isEqualTo("pkg:maven/com.vuln/vuln@1.0");
  }

  private void mockStoredEvidence(StoredReachabilityEvidence stored) throws IOException {
    byte[] json = objectMapper.writeValueAsBytes(stored);
    ReportEntry entry = new ReportEntry(
        LifecycleReport.ReportFile.REACHABILITY_EVIDENCE_JSON.getName(),
        System.currentTimeMillis(), json);
    when(report.getEntry(eq(LifecycleReport.ReportFile.REACHABILITY_EVIDENCE_JSON.getName()))).thenReturn(entry);
  }
}
