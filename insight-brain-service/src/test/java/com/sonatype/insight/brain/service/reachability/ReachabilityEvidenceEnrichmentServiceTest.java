/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.reachability;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import com.sonatype.clm.dto.model.signature.ReachabilityEvidenceDTO;
import com.sonatype.clm.dto.model.signature.ReachabilityEvidenceDTO.ElidedSegmentDTO;
import com.sonatype.clm.dto.model.signature.ReachabilityEvidenceDTO.EvidencePathDTO;
import com.sonatype.clm.dto.model.signature.ReachabilityEvidenceDTO.EvidencePathsDTO;
import com.sonatype.clm.dto.model.signature.ReachabilityEvidenceDTO.GapSegmentDTO;
import com.sonatype.clm.dto.model.signature.ReachabilityEvidenceDTO.MethodSegmentDTO;
import com.sonatype.clm.dto.model.signature.ReachabilityEvidenceDTO.PathSegmentDTO;
import com.sonatype.insight.brain.report.ApplicationReport;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.service.reachability.StoredReachabilityEvidence.ElidedSegment;
import com.sonatype.insight.brain.service.reachability.StoredReachabilityEvidence.GapSegment;
import com.sonatype.insight.brain.service.reachability.StoredReachabilityEvidence.MethodSegment;

import org.junit.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ReachabilityEvidenceEnrichmentServiceTest
{
  private static final String VULN_ID = "CVE-2023-1234";

  private final ReachabilityEvidenceEnrichmentService service = new ReachabilityEvidenceEnrichmentService();

  @Test
  public void testMethodSegmentGetsPurl() throws Exception {
    ReachabilityEvidenceDTO dto = createDto(methodSeg("com.example.App.main()V", "lib/jackson.jar"));
    ApplicationReport report = mockReport(Map.of("lib/jackson.jar", "pkg:maven/com.fasterxml/jackson@2.13.0"));

    StoredReachabilityEvidence result = service.enrich(dto, report);

    MethodSegment seg = (MethodSegment) result.evidence().get(VULN_ID).paths().get(0).segments().get(0);
    assertThat(seg.component()).isEqualTo("pkg:maven/com.fasterxml/jackson@2.13.0");
  }

  @Test
  public void testUnmappablePathLeavesComponentNull() throws Exception {
    ReachabilityEvidenceDTO dto = createDto(methodSeg("com.example.App.main()V", "unknown.jar"));
    ApplicationReport report = mockReport(Map.of("lib/jackson.jar", "pkg:maven/x/y@1.0"));

    StoredReachabilityEvidence result = service.enrich(dto, report);

    MethodSegment seg = (MethodSegment) result.evidence().get(VULN_ID).paths().get(0).segments().get(0);
    assertThat(seg.component()).isNull();
  }

  @Test
  public void testGapSegmentPassedThrough() throws Exception {
    ReachabilityEvidenceDTO dto =
        createDto(methodSeg("a.B.c()V", "a.jar"), new GapSegmentDTO(), methodSeg("d.E.f()V", "d.jar"));
    ApplicationReport report = mockReport(Map.of());

    StoredReachabilityEvidence result = service.enrich(dto, report);

    var segments = result.evidence().get(VULN_ID).paths().get(0).segments();
    assertThat(segments).hasSize(3);
    assertThat(segments.get(1)).isInstanceOf(GapSegment.class);
  }

  @Test
  public void testElidedSegmentPassedThrough() throws Exception {
    ReachabilityEvidenceDTO dto =
        createDto(methodSeg("a.B.c()V", "a.jar"), elidedSeg(5), methodSeg("d.E.f()V", "d.jar"));
    ApplicationReport report = mockReport(Map.of());

    StoredReachabilityEvidence result = service.enrich(dto, report);

    var segments = result.evidence().get(VULN_ID).paths().get(0).segments();
    assertThat(segments.get(1)).isInstanceOf(ElidedSegment.class);
    assertThat(((ElidedSegment) segments.get(1)).count()).isEqualTo(5);
  }

  @Test
  public void testMissingBomJson() throws Exception {
    ReachabilityEvidenceDTO dto = createDto(methodSeg("a.B.c()V", "a.jar"));
    ApplicationReport report = Mockito.mock(ApplicationReport.class);
    when(report.getEntry("bom.json")).thenReturn(null);

    StoredReachabilityEvidence result = service.enrich(dto, report);

    MethodSegment seg = (MethodSegment) result.evidence().get(VULN_ID).paths().get(0).segments().get(0);
    assertThat(seg.component()).isNull();
  }

  // Helpers

  private ReachabilityEvidenceDTO createDto(PathSegmentDTO... segments) {
    EvidencePathDTO path = new EvidencePathDTO();
    path.segments = Arrays.asList(segments);
    EvidencePathsDTO paths = new EvidencePathsDTO();
    paths.paths = Collections.singletonList(path);
    paths.truncated = true;
    ReachabilityEvidenceDTO dto = new ReachabilityEvidenceDTO();
    dto.evidence = Collections.singletonMap(VULN_ID, paths);
    return dto;
  }

  private PathSegmentDTO methodSeg(String method, String filePath) {
    MethodSegmentDTO seg = new MethodSegmentDTO();
    seg.method = method;
    seg.filePath = filePath;
    return seg;
  }

  private PathSegmentDTO elidedSeg(int count) {
    ElidedSegmentDTO seg = new ElidedSegmentDTO();
    seg.count = count;
    return seg;
  }

  private ApplicationReport mockReport(Map<String, String> pathToPurl) throws Exception {
    StringBuilder bomJson = new StringBuilder("{\"aaData\":[");
    boolean first = true;
    for (Map.Entry<String, String> entry : pathToPurl.entrySet()) {
      if (!first)
        bomJson.append(",");
      first = false;
      bomJson.append("{\"packageUrl\":\"")
          .append(entry.getValue())
          .append("\",\"pathnames\":[\"")
          .append(entry.getKey())
          .append("\"]}");
    }
    bomJson.append("]}");

    ApplicationReport report = Mockito.mock(ApplicationReport.class);
    ReportEntry bomEntry = new ReportEntry("bom.json", System.currentTimeMillis(), bomJson.toString().getBytes());
    when(report.getEntry("bom.json")).thenReturn(bomEntry);
    return report;
  }
}
