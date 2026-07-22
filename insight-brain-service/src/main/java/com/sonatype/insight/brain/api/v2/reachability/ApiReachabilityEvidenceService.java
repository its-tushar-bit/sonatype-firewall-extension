/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.reachability;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.report.LifecycleReport;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.service.reachability.StoredReachabilityEvidence;
import com.sonatype.insight.brain.service.reachability.StoredReachabilityEvidence.VulnerabilityEvidence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for retrieving stored reachability evidence from application reports.
 */
@Named
public class ApiReachabilityEvidenceService
{
  private static final Logger log = LoggerFactory.getLogger(ApiReachabilityEvidenceService.class);

  private final ReportService reportService;

  private final ObjectMapper objectMapper;

  @Inject
  public ApiReachabilityEvidenceService(ReportService reportService, ObjectMapper objectMapper) {
    this.reportService = reportService;
    this.objectMapper = objectMapper;
  }

  /**
   * Get evidence for a specific vulnerability from a report.
   *
   * @return the evidence response, or null if not found
   */
  public ApiReachabilityEvidenceResponse getEvidenceForVulnerability(
      String applicationId,
      String scanId,
      String vulnerabilityId) throws IOException
  {
    LifecycleReport report = reportService.getReport(applicationId, scanId);
    ReportEntry entry = report.getEntry(
        LifecycleReport.ReportFile.REACHABILITY_EVIDENCE_JSON.getName());

    if (entry == null || entry.buf == null) {
      log.debug("No reachability evidence found for app={}, scan={}", applicationId, scanId);
      return null;
    }

    StoredReachabilityEvidence evidence;
    try {
      evidence = objectMapper.readValue(entry.buf, StoredReachabilityEvidence.class);
    }
    catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      log.warn("Corrupt reachability evidence for app={}, scan={}", applicationId, scanId, e);
      return null;
    }

    VulnerabilityEvidence vulnEvidence = evidence.evidence() != null
        ? evidence.evidence().get(vulnerabilityId)
        : null;
    if (vulnEvidence == null) {
      log.debug("Vulnerability {} not found in evidence for app={}, scan={}",
          vulnerabilityId, applicationId, scanId);
      return null;
    }
    return new ApiReachabilityEvidenceResponse(
        vulnerabilityId,
        vulnEvidence.paths() != null ? vulnEvidence.paths() : java.util.Collections.emptyList(),
        vulnEvidence.truncated());
  }
}
