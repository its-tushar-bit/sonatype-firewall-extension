/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.policy;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.thirdparty.SbomStatus;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import org.apache.commons.lang3.StringUtils;

@Named
public class SbomPolicyService
{
  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final ThirdPartyScanDAO thirdPartyScanDAO;

  private final ReportService reportService;

  @Inject
  public SbomPolicyService(
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final ThirdPartyScanDAO thirdPartyScanDAO,
      final  ReportService reportService
  )
  {
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.thirdPartyScanDAO = thirdPartyScanDAO;
    this.reportService = reportService;
  }

  @Authorize(permission = Permission.READ)
  public PolicyThreats getPolicyViolations(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String sbomVersion) throws IOException
  {
    ReportEntry policyThreatsReportEntry = getPolicyViolationsReportEntry(applicationId, sbomVersion);
    return policyThreatsReportEntry != null ? JsonUtils.parse(policyThreatsReportEntry.buf, PolicyThreats.class) : null;
  }

  @Authorize(permission = Permission.READ)
  public ReportEntry getPolicyViolationsReportEntry(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String sbomVersion)
  {
    String scanId = getScanIdForPolicyViolation(applicationId, sbomVersion);
    return reportService.processBrowseReport(applicationId, scanId, "policythreats.json");
  }

  @Authorize(permission = Permission.READ)
  public JsonNode getPolicyViolationsJsonNodeByFileCoordinateId(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String sbomVersion,
      String fileCoordinateId,
      ReportEntry policyThreatsReportEntry) throws IOException
  {
    if (StringUtils.isBlank(fileCoordinateId)) {
      throw new BadRequestException("fileCoordinateId cannot be null or empty");
    }

    ReportEntry policyViolationsReportEntry = policyThreatsReportEntry != null ? policyThreatsReportEntry
        : getPolicyViolationsReportEntry(applicationId, sbomVersion);

    if (policyViolationsReportEntry == null) {
      return null;
    }

    String scanId = getScanIdForPolicyViolation(applicationId, sbomVersion);
    String bomComponentHash = findComponentHashInReport(applicationId, scanId, fileCoordinateId);

    if (StringUtils.isBlank(bomComponentHash)) {
      return null;
    }

    ContainerNode<?> policyThreatsReportJson = JsonUtils.parse(policyViolationsReportEntry.buf);
    JsonNode aaData = policyThreatsReportJson.get("aaData");

    for (JsonNode policyThreatsComponentNode : aaData) {
      if (bomComponentHash.equals(policyThreatsComponentNode.get("hash").asText())) {
        return policyThreatsComponentNode;
      }
    }

    return null;
  }

  private String findComponentHashInReport(
      String applicationId,
      String scanId,
      String fileCoordinateId) throws IOException
  {
    ReportEntry bomReportEntry = reportService.processBrowseReport(applicationId, scanId, "bom.json");
    if (bomReportEntry == null) {
      return null;
    }

    ContainerNode<?> bomReportJson = JsonUtils.parse(bomReportEntry.buf);
    JsonNode bomReportAaData = bomReportJson.get("aaData");

    for (JsonNode bomComponentNode : bomReportAaData) {
      JsonNode sonatypeIdentifierNode = bomComponentNode.get("sonatypeIdentifier");

      if (sonatypeIdentifierNode != null && !sonatypeIdentifierNode.isNull()) {
        String sonatypeIdentifier = sonatypeIdentifierNode.asText();

        if (fileCoordinateId.equals(sonatypeIdentifier)) {
          return bomComponentNode.get("hash").asText();
        }
      }
    }
    return null;
  }

  private String getScanIdForPolicyViolation(String applicationId, String sbomVersion) {
    ThirdPartySbomMetadata thirdPartySbomMetadata =
        thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(applicationId, sbomVersion);
    if (thirdPartySbomMetadata == null
        || !thirdPartySbomMetadata.getStatus().replace("\n", "").equalsIgnoreCase(SbomStatus.ACTIVE.name())) {
      throw new NotFoundException(
          String.format("Cannot find version %s for application with ID %s.", sbomVersion, applicationId));
    }
    ThirdPartyScan thirdPartyScan =
        thirdPartyScanDAO.getByThirdPartyFileId(thirdPartySbomMetadata.getThirdPartyFileId());
    return thirdPartyScan.getScanId();
  }

  @Authorize(permission = Permission.READ)
  public PolicyThreats.Component getPolicyViolationsByFileCoordinateId(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String sbomVersion,
      String fileCoordinateId) throws IOException
  {
    JsonNode jsonNode =
        getPolicyViolationsJsonNodeByFileCoordinateId(applicationId, sbomVersion, fileCoordinateId, null);
    return jsonNode != null ? JsonUtils.asPojo(jsonNode, PolicyThreats.Component.class) : null;
  }
}
