/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.reachability;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.model.Owner;
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
    return getEvidenceForVulnerabilityInternal(applicationId, scanId, vulnerabilityId,
        reportService.getReport(applicationId, scanId));
  }

  /**
   * Owner-scoped overload used by the HRC-scoped sibling resource. Authorization is enforced
   * at the resource layer via {@code @AuthzContext} on the {@code hrcId} / {@code publicId}
   * path params.
   */
  public ApiReachabilityEvidenceResponse getEvidenceForVulnerability(
      final Owner owner,
      final String scanId,
      final String vulnerabilityId) throws IOException
  {
    return getEvidenceForVulnerabilityInternal(owner.getId(), scanId, vulnerabilityId,
        reportService.getReport(owner, scanId));
  }

  private ApiReachabilityEvidenceResponse getEvidenceForVulnerabilityInternal(
      final String ownerIdForLogs,
      final String scanId,
      final String vulnerabilityId,
      final LifecycleReport report) throws IOException
  {
    ReportEntry entry = report.getEntry(
        LifecycleReport.ReportFile.REACHABILITY_EVIDENCE_JSON.getName());

    if (entry == null || entry.buf == null) {
      log.debug("No reachability evidence found for owner={}, scan={}", ownerIdForLogs, scanId);
      return null;
    }

    StoredReachabilityEvidence evidence;
    try {
      evidence = objectMapper.readValue(entry.buf, StoredReachabilityEvidence.class);
    }
    catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      log.warn("Corrupt reachability evidence for owner={}, scan={}", ownerIdForLogs, scanId, e);
      return null;
    }

    VulnerabilityEvidence vulnEvidence = evidence.evidence() != null
        ? evidence.evidence().get(vulnerabilityId)
        : null;
    if (vulnEvidence == null) {
      log.debug("Vulnerability {} not found in evidence for owner={}, scan={}",
          vulnerabilityId, ownerIdForLogs, scanId);
      return null;
    }
    return new ApiReachabilityEvidenceResponse(
        vulnerabilityId,
        vulnEvidence.paths() != null ? vulnEvidence.paths() : java.util.Collections.emptyList(),
        vulnEvidence.truncated());
  }
}
