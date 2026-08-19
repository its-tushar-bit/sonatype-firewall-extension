/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.sbom.utils.SbomCommonUtils;
import com.sonatype.insight.brain.scan.ScanContext;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.thirdparty.SbomScanType;
import com.sonatype.insight.brain.thirdparty.ThirdPartyScanContext;
import com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultsProcessor;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.telemetry.model.TelemetryData;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ScanUploadService
{
  private static final Logger log = LoggerFactory.getLogger(ScanUploadService.class);

  private final ScanUploader uploader;

  private final ScanPersistenceService scanPersistenceService;

  private final ThirdPartyScanResultsProcessor scanResultsProcessor;

  private final ThirdPartyScanDAO thirdPartyScanDAO;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final TelemetrySender telemetrySender;

  @Inject
  public ScanUploadService(
      final ThirdPartyScanResultsProcessor scanResultsProcessor,
      final ScanUploader uploader,
      final ThirdPartyScanDAO thirdPartyScanDAO,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final ScanPersistenceService scanPersistenceService,
      final TelemetrySender telemetrySender)
  {
    this.scanResultsProcessor = scanResultsProcessor;
    this.uploader = uploader;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.scanPersistenceService = scanPersistenceService;
    this.thirdPartyScanDAO = thirdPartyScanDAO;
    this.telemetrySender = telemetrySender;
  }

  public ScanReceipt upload(
      ScanEntity scanEntity,
      Owner owner,
      String stageTypeId,
      ClientScanType clientScanType,
      String clientUserAgent,
      TelemetryData thirdPartyScanTelemetryData,
      String scanRequestId,
      boolean isWebUIRequest) throws IOException
  {
    return upload(
        scanEntity,
        owner,
        stageTypeId,
        clientScanType,
        clientUserAgent,
        thirdPartyScanTelemetryData,
        scanRequestId,
        null,
        isWebUIRequest);
  }

  public ScanReceipt upload(
      ScanEntity scanEntity,
      Owner owner,
      String stageTypeId,
      ClientScanType clientScanType,
      String clientUserAgent,
      TelemetryData thirdPartyScanTelemetryData,
      String scanRequestId,
      ScanContext scanContext,
      boolean isWebUIRequest) throws IOException
  {
    if (scanEntity == null || owner == null) {
      throw new IllegalArgumentException("scanFile and owner is required for scan uploads");
    }
    ThirdPartyScanContext thirdPartyScanContext =
        getThirdPartyScanContext(scanRequestId, scanEntity, owner, stageTypeId, scanContext);
    if (thirdPartyScanTelemetryData != null) {
      thirdPartyScanTelemetryData.put("scan_file_type", thirdPartyScanContext.getScanType().name());
    }
    ScanReceipt scanReceipt;
    if (ClientScanType.SONATYPE_THIRD_PARTY.equals(clientScanType)) {
      scanReceipt =
          filterAndUpload(scanEntity, owner, stageTypeId, clientUserAgent, thirdPartyScanContext,
              thirdPartyScanTelemetryData, isWebUIRequest);
    }
    else {
      scanReceipt = uploader.upload(scanEntity, owner, stageTypeId, clientUserAgent, thirdPartyScanContext,
          isWebUIRequest);
      if (ComplianceStageType.ID.equals(stageTypeId) && StringUtils.isNotEmpty(scanRequestId)) {
        thirdPartyScanDAO.updateScanIdForScanRequest(scanRequestId, scanReceipt.getScanId());
        saveFilteredScanFileIfNeeded(thirdPartyScanContext, scanEntity);
        telemetrySender.send(thirdPartyScanTelemetryData);
      }
    }
    return scanReceipt;
  }

  // visible for testing
  ScanReceipt filterAndUpload(
      ScanEntity scanEntity,
      Owner owner,
      String stageTypeId,
      String clientUserAgent,
      ThirdPartyScanContext thirdPartyScanContext,
      TelemetryData thirdPartyScanTelemetryData,
      boolean isWebUIRequest) throws IOException
  {
    ScanEntity tempScanEntity = scanPersistenceService.createTempScan(owner.getId());

    String scanRequestId =
        scanResultsProcessor.filterAndSaveData(scanEntity, tempScanEntity, thirdPartyScanContext,
            thirdPartyScanTelemetryData);
    ScanReceipt scanReceipt = uploader.upload(tempScanEntity, owner, stageTypeId, clientUserAgent,
        thirdPartyScanContext, isWebUIRequest);
    thirdPartyScanDAO.updateScanIdForScanRequest(scanRequestId, scanReceipt.getScanId());
    saveContainerUriPaths(stageTypeId, thirdPartyScanContext);
    saveFilteredScanFileIfNeeded(thirdPartyScanContext, tempScanEntity);
    try {
      scanPersistenceService.deleteScan(tempScanEntity);
    }
    catch (IOException e) {
      log.error("Unable to remove temporary scan file {}", tempScanEntity.getLocation(), e);
    }
    return scanReceipt;
  }

  @VisibleForTesting
  void saveContainerUriPaths(final String stageTypeId, final ThirdPartyScanContext thirdPartyScanContext) {
    if (stageTypeId.equals(StageTypes.COMPLIANCE.getId())) {
      List<String> containerUriPaths = thirdPartyScanContext.getContainerUriPaths();
      if (!containerUriPaths.isEmpty()) {
        String concatenatedPaths = String.join(",", containerUriPaths);
        ThirdPartySbomMetadata sbomMetadata =
            thirdPartySbomMetadataDAO.getById(thirdPartyScanContext.getSbomMetadataId());
        sbomMetadata.setOriginalBinaryFileName(concatenatedPaths);
        thirdPartySbomMetadataDAO.update(sbomMetadata);
      }
    }
  }

  // visible for testing
  void saveFilteredScanFileIfNeeded(final ThirdPartyScanContext scanContext, final ScanEntity filteredScanEntity) {
    if (scanContext == null) {
      return;
    }
    // we need to save the filtered scan files only in the case of SBOM binary scans or if at least one sbom is saved
    // During the SBOM manager import
    // For Binary scans the filtered scan file is saved at the merge time with HDS results
    if (scanContext.isSbomSavedForScan() && !SbomScanType.BINARY.equals(scanContext.getScanType())) {
      ThirdPartyScan tpScan = thirdPartyScanDAO.getById(scanContext.getThirdPartyScanId());
      if (tpScan != null) {
        ScanEntity scanEntityCopy = scanPersistenceService.getScanByName(scanContext.getApplicationId(),
            SbomCommonUtils.newFilteredScanFileName(tpScan.getScanId()));
        try {
          scanPersistenceService.copyScanFile(filteredScanEntity, scanEntityCopy);
          tpScan.setFilteredScanFile(scanEntityCopy.getName());
          thirdPartyScanDAO.update(tpScan);
        }
        catch (IOException e) {
          log.error("Error saving filtered scan file {}", scanEntityCopy.getName(), e);
        }
      }
    }
  }

  private ThirdPartyScanContext getThirdPartyScanContext(
      final String scanRequestId,
      final ScanEntity scanEntity,
      final Owner owner,
      final String stageTypeId,
      final ScanContext scanContext)
  {
    if (scanRequestId != null && ComplianceStageType.ID.equals(stageTypeId)) {
      ThirdPartyScan scan = thirdPartyScanDAO.getSingleByScanRequestId(scanRequestId);
      if (scan != null) {
        ThirdPartySbomMetadata sbomMetadata =
            thirdPartySbomMetadataDAO.getByThirdPartyFileId(scan.getThirdPartyFileId());
        ThirdPartyScanContext thirdPartyScanContext =
            new ThirdPartyScanContext(scanRequestId, owner.getId(), SbomScanType.valueOf(sbomMetadata.getScanType()),
                scanEntity, stageTypeId);
        thirdPartyScanContext.setThirdPartyFileId(sbomMetadata.getThirdPartyFileId());
        thirdPartyScanContext.setSbomFileName(sbomMetadata.getFilename());
        thirdPartyScanContext.setApplicationVersion(sbomMetadata.getSbomVersion());
        thirdPartyScanContext.setSbomMetadataId(sbomMetadata.getId());
        thirdPartyScanContext.setThirdPartyScanId(scan.getId());
        thirdPartyScanContext.setIsValid(sbomMetadata.getIsValid());

        return thirdPartyScanContext;
      }
    }

    String newScanRequestId = UUID.randomUUID().toString().replace("-", "");
    ThirdPartyScanContext thirdPartyScanContext =
        new ThirdPartyScanContext(newScanRequestId, owner.getId(), SbomScanType.SBOM, scanEntity, stageTypeId);
    if (scanContext != null) {
      thirdPartyScanContext.setApplicationVersion(scanContext.applicationVersion());
      thirdPartyScanContext.setIsValid(scanContext.isValid());
      thirdPartyScanContext.setContainerImageSbomSpecification(scanContext.containerImageSbomSpecification());

      var sbomMetadataId = scanContext.sbomMetadataId();
      if (sbomMetadataId != null) {
        thirdPartyScanContext.setSbomMetadataId(sbomMetadataId);
        thirdPartyScanContext.markSbomSavedForScan();
      }
    }
    return thirdPartyScanContext;
  }
}
