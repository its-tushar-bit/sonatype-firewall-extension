/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.policy;

import java.io.IOException;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import static com.sonatype.insight.brain.report.LifecycleReport.ReportFile.BOM_JSON;
import static com.sonatype.insight.brain.report.LifecycleReport.ReportFile.POLICY_THREATS;

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
      final ReportService reportService)
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
    return reportService.processBrowseReport(applicationId, scanId, POLICY_THREATS.getName());
  }

  @Authorize(permission = Permission.READ)
  public JsonNode getPolicyViolationsJsonNodeByComponentRefOrHash(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String sbomVersion,
      String componentRef,
      String fileCoordinateId,
      String hash,
      ReportEntry policyThreatsReportEntry,
      ReportEntry bomReportEntry) throws IOException
  {
    if (StringUtils.isAllBlank(componentRef, fileCoordinateId, hash)) {
      throw new BadRequestException("componentRef, fileCoordinateId and hash cannot be both null or empty.");
    }

    ReportEntry policyViolationsReportEntry = policyThreatsReportEntry != null
        ? policyThreatsReportEntry
        : getPolicyViolationsReportEntry(applicationId, sbomVersion);

    if (policyViolationsReportEntry == null) {
      return null;
    }

    String scanId = getScanIdForPolicyViolation(applicationId, sbomVersion);
    String bomComponentHash =
        findComponentHashInReport(applicationId, scanId, componentRef, fileCoordinateId, hash, bomReportEntry);

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
      String componentRef,
      String fileCoordinateId,
      String hash,
      ReportEntry bomReportEntry) throws IOException
  {
    if (bomReportEntry == null) {
      bomReportEntry = reportService.processBrowseReport(applicationId, scanId, BOM_JSON.getName());
    }
    if (bomReportEntry == null) {
      return null;
    }

    ContainerNode<?> bomReportJson = JsonUtils.parse(bomReportEntry.buf);
    JsonNode bomReportAaData = bomReportJson.get("aaData");
    String matchingHash = null;

    for (JsonNode bomComponentNode : bomReportAaData) {
      JsonNode componentRefsNode = bomComponentNode.get(SbomCycloneDxUtils.PROPERTY_COMPONENT_REFS);
      JsonNode hashNode = bomComponentNode.get("hash");

      if (isJsonNodeNotNull(hashNode)) {
        // Match first by componentRefs
        if (isJsonNodeNotNull(componentRefsNode)) {
          List<String> bomNodeComponentRefs = JsonUtils.getStringListFromArray(componentRefsNode);
          // given that we consolidate any possible multiple componentRefs into a single componentRef during merge
          // we can safely assume that the first componentRef in the list is the one we are looking for
          if (CollectionUtils.isNotEmpty(bomNodeComponentRefs) && bomNodeComponentRefs.get(0).equals(componentRef)) {
            return hashNode.asText();
          }
        }

        // Fallback to Match by sonatypeIdentifier for old sbom reports
        JsonNode sonatypeIdentifierNode = bomComponentNode.get(SbomCycloneDxUtils.PROPERTY_SONATYPE_IDENTIFIER);
        if (isJsonNodeNotNull(sonatypeIdentifierNode)
            && StringUtils.isNotBlank(fileCoordinateId)
            && fileCoordinateId.equals(sonatypeIdentifierNode.asText()))
        {
          return hashNode.asText();
        }
        // Fallback match by hash
        else if (StringUtils.isNotBlank(hash) && matchingHash == null && hash.equals(hashNode.asText())) {
          matchingHash = hashNode.asText();
        }
      }
    }

    return matchingHash;
  }

  private boolean isJsonNodeNotNull(JsonNode jsonNode) {
    return jsonNode != null && !jsonNode.isNull();
  }

  private String getScanIdForPolicyViolation(String applicationId, String sbomVersion) {
    ThirdPartySbomMetadata thirdPartySbomMetadata =
        thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(applicationId, sbomVersion);
    if (thirdPartySbomMetadata == null
        || !ThirdPartySbomMetadataStatus.ACTIVE.equals(thirdPartySbomMetadata.getStatus()))
    {
      throw new NotFoundException(
          String.format("Cannot find version %s for application with ID %s.", sbomVersion, applicationId));
    }
    ThirdPartyScan thirdPartyScan =
        thirdPartyScanDAO.getByThirdPartyFileId(thirdPartySbomMetadata.getThirdPartyFileId());
    return thirdPartyScan.getScanId();
  }

  @Authorize(permission = Permission.READ)
  public PolicyThreats.Component getPolicyViolationsByFileCoordinateIdOrHash(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) String applicationId,
      String sbomVersion,
      String componentRef,
      String fileCoordinateId,
      String hash,
      ReportEntry policyViolationsReportEntry,
      ReportEntry bomReportEntry) throws IOException
  {
    JsonNode jsonNode =
        getPolicyViolationsJsonNodeByComponentRefOrHash(applicationId, sbomVersion, componentRef, fileCoordinateId,
            hash, policyViolationsReportEntry, bomReportEntry);
    return jsonNode != null ? JsonUtils.asPojo(jsonNode, PolicyThreats.Component.class) : null;
  }
}
