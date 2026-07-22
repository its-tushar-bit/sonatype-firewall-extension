/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.reachability;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Named;

import com.sonatype.clm.dto.model.signature.ReachabilityEvidenceDTO;
import com.sonatype.clm.dto.model.signature.ReachabilityEvidenceDTO.ElidedSegmentDTO;
import com.sonatype.clm.dto.model.signature.ReachabilityEvidenceDTO.EvidencePathDTO;
import com.sonatype.clm.dto.model.signature.ReachabilityEvidenceDTO.EvidencePathsDTO;
import com.sonatype.clm.dto.model.signature.ReachabilityEvidenceDTO.GapSegmentDTO;
import com.sonatype.clm.dto.model.signature.ReachabilityEvidenceDTO.MethodSegmentDTO;
import com.sonatype.clm.dto.model.signature.ReachabilityEvidenceDTO.PathSegmentDTO;
import com.sonatype.insight.brain.report.LifecycleReport;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.service.reachability.StoredReachabilityEvidence.ElidedSegment;
import com.sonatype.insight.brain.service.reachability.StoredReachabilityEvidence.EvidencePath;
import com.sonatype.insight.brain.service.reachability.StoredReachabilityEvidence.GapSegment;
import com.sonatype.insight.brain.service.reachability.StoredReachabilityEvidence.MethodSegment;
import com.sonatype.insight.brain.service.reachability.StoredReachabilityEvidence.PathSegment;
import com.sonatype.insight.brain.service.reachability.StoredReachabilityEvidence.VulnerabilityEvidence;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts incoming reachability evidence DTOs into the server-side storage model,
 * enriching method segments with component PURLs from bom.json.
 */
@Named
public class ReachabilityEvidenceEnrichmentService
{
  private static final Logger log = LoggerFactory.getLogger(ReachabilityEvidenceEnrichmentService.class);

  public StoredReachabilityEvidence enrich(ReachabilityEvidenceDTO dto, LifecycleReport report) throws IOException {
    Map<String, String> pathToPurl = buildPathToPurlMap(report);

    Map<String, VulnerabilityEvidence> evidence = new HashMap<>();
    if (dto.evidence != null) {
      for (Map.Entry<String, EvidencePathsDTO> entry : dto.evidence.entrySet()) {
        if (entry.getValue() == null) {
          continue;
        }
        evidence.put(entry.getKey(), convertVulnEvidence(entry.getValue(), pathToPurl));
      }
    }

    return new StoredReachabilityEvidence(evidence);
  }

  private VulnerabilityEvidence convertVulnEvidence(EvidencePathsDTO dto, Map<String, String> pathToPurl) {
    List<EvidencePath> paths = new ArrayList<>();
    if (dto.paths != null) {
      for (EvidencePathDTO pathDto : dto.paths) {
        if (pathDto == null) {
          continue;
        }
        paths.add(convertPath(pathDto, pathToPurl));
      }
    }
    return new VulnerabilityEvidence(paths, dto.truncated);
  }

  private EvidencePath convertPath(EvidencePathDTO dto, Map<String, String> pathToPurl) {
    List<PathSegment> segments = new ArrayList<>();
    if (dto.segments != null) {
      for (PathSegmentDTO segDto : dto.segments) {
        if (segDto == null) {
          continue;
        }
        PathSegment converted = convertSegment(segDto, pathToPurl);
        if (converted != null) {
          segments.add(converted);
        }
      }
    }
    return new EvidencePath(segments);
  }

  private PathSegment convertSegment(PathSegmentDTO dto, Map<String, String> pathToPurl) {
    if (dto instanceof MethodSegmentDTO method) {
      String component = method.filePath != null ? pathToPurl.get(method.filePath) : null;
      return new MethodSegment(method.method, method.filePath, component);
    }
    if (dto instanceof GapSegmentDTO) {
      return new GapSegment();
    }
    if (dto instanceof ElidedSegmentDTO elided) {
      return new ElidedSegment(elided.count);
    }
    return null; // Unknown segment type from a newer scanner — skip gracefully
  }

  private Map<String, String> buildPathToPurlMap(LifecycleReport report) throws IOException {
    Map<String, String> map = new HashMap<>();

    ReportEntry bomEntry = report.getEntry(LifecycleReport.ReportFile.BOM_JSON.getName());
    if (bomEntry == null) {
      return map;
    }

    JsonNode bomJson = JsonUtils.parse(bomEntry.buf);
    JsonNode aaData = bomJson.path("aaData");
    if (!aaData.isArray()) {
      return map;
    }

    for (JsonNode component : aaData) {
      String packageUrl = component.path("packageUrl").asText(null);
      if (packageUrl == null || packageUrl.isEmpty()) {
        continue;
      }

      JsonNode pathnames = component.path("pathnames");
      if (pathnames.isArray()) {
        for (JsonNode pathname : pathnames) {
          String path = pathname.asText(null);
          if (path != null && !path.isEmpty()) {
            map.put(path, packageUrl);
          }
        }
      }
    }

    log.debug("Built path-to-PURL map with {} entries", map.size());
    return map;
  }
}
