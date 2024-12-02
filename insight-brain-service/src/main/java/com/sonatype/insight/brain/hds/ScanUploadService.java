/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.sbom.utils.SbomCommonUtils;
import com.sonatype.insight.brain.scan.ScanContext;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.thirdparty.SbomScanType;
import com.sonatype.insight.brain.thirdparty.ThirdPartyScanContext;
import com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultsProcessor;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.telemetry.model.TelemetryData;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ScanUploadService
{
  private static final Logger log = LoggerFactory.getLogger(ScanUploadService.class);

  private final ScanUploader uploader;

  private final InsightWork work;

  private final ThirdPartyScanResultsProcessor scanResultsProcessor;

  private final ThirdPartyScanDAO thirdPartyScanDAO;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  @Inject
  public ScanUploadService(
      ThirdPartyScanResultsProcessor scanResultsProcessor,
      ScanUploader uploader,
      final ThirdPartyScanDAO thirdPartyScanDAO,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      InsightWork work)
  {
    this.scanResultsProcessor = scanResultsProcessor;
    this.uploader = uploader;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.work = work;
    this.thirdPartyScanDAO = thirdPartyScanDAO;
  }

  public ScanReceipt upload(
      File scanFile,
      Application app,
      String stageTypeId,
      ClientScanType clientScanType,
      String clientUserAgent,
      TelemetryData thirdPartyScanTelemetryData,
      String scanRequestId) throws IOException
  {
    return upload(
        scanFile,
        app,
        stageTypeId,
        clientScanType,
        clientUserAgent,
        thirdPartyScanTelemetryData,
        scanRequestId,
        null
    );
  }

  public ScanReceipt upload(
      File scanFile,
      Application app,
      String stageTypeId,
      ClientScanType clientScanType,
      String clientUserAgent,
      TelemetryData thirdPartyScanTelemetryData,
      String scanRequestId,
      ScanContext scanContext) throws IOException
  {
    if (scanFile == null || app == null) {
      throw new IllegalArgumentException("scanFile and application is required for scan uploads");
    }
    ThirdPartyScanContext thirdPartyScanContext =
        getThirdPartyScanContext(scanRequestId, scanFile, app, stageTypeId, scanContext);
    if (thirdPartyScanTelemetryData != null) {
      thirdPartyScanTelemetryData.put("scan_file_type", thirdPartyScanContext.getScanType().name());
    }
    ScanReceipt scanReceipt;
    if (ClientScanType.SONATYPE_THIRD_PARTY.equals(clientScanType)) {
      scanReceipt =
          filterAndUpload(scanFile, app, stageTypeId, clientUserAgent, thirdPartyScanContext,
              thirdPartyScanTelemetryData);
    }
    else {
      scanReceipt = uploader.upload(scanFile, app, stageTypeId, clientUserAgent, thirdPartyScanContext);
      if (ComplianceStageType.ID.equals(stageTypeId) && StringUtils.isNotEmpty(scanRequestId)) {
        thirdPartyScanDAO.updateScanIdForScanRequest(scanRequestId, scanReceipt.getScanId());
        saveFilteredScanFileIfNeeded(thirdPartyScanContext, scanFile);
      }
    }
    return scanReceipt;
  }

  //visible for testing
  ScanReceipt filterAndUpload(
      File scanFile,
      Application app,
      String stageTypeId,
      String clientUserAgent,
      ThirdPartyScanContext thirdPartyScanContext,
      TelemetryData thirdPartyScanTelemetryData)
      throws IOException
  {
    File scanDir = work.getScanDir(app.getId());
    File tempScanFile = FileUtils.createTempFile("tmp-", ".xml.gz", scanDir);

    String scanRequestId =
        scanResultsProcessor.filterAndSaveData(scanFile, tempScanFile, scanDir, thirdPartyScanContext,
            thirdPartyScanTelemetryData);
    ScanReceipt scanReceipt = uploader.upload(tempScanFile, app, stageTypeId, clientUserAgent, thirdPartyScanContext);
    thirdPartyScanDAO.updateScanIdForScanRequest(scanRequestId, scanReceipt.getScanId());
    saveFilteredScanFileIfNeeded(thirdPartyScanContext, tempScanFile);
    try {
      Files.delete(tempScanFile.toPath());
    }
    catch (IOException e) {
      log.error("Unable to remove temporary scan file {}", tempScanFile.toPath());
    }
    return scanReceipt;
  }

  //visible for testing
  void saveFilteredScanFileIfNeeded(final ThirdPartyScanContext scanContext, final File filteredScanFile) {
    if (scanContext == null) {
      return;
    }
    //we need to save the filtered scan files only in the case of SBOM binary scans or if at least one sbom is saved
    // During the SBOM manager import
    // For Binary scans the filtered scan file is saved at the merge time with HDS results
    if (scanContext.isSbomSavedForScan() && !SbomScanType.BINARY.equals(scanContext.getScanType())) {
      ThirdPartyScan tpScan = thirdPartyScanDAO.getById(scanContext.getThirdPartyScanId());
      if (tpScan != null) {
        File scanFileCopy = new File(work.getScanDir(scanContext.getApplicationId()),
            SbomCommonUtils.newFilteredScanFileName(tpScan.getScanId()));
        try {
          Files.copy(filteredScanFile.toPath(), scanFileCopy.toPath(), StandardCopyOption.REPLACE_EXISTING);
          tpScan.setFilteredScanFile(scanFileCopy.getName());
          thirdPartyScanDAO.update(tpScan);
        }
        catch (IOException e) {
          log.error("Error saving filtered scan file {}", scanFileCopy.getName(), e);
        }
      }
    }
  }

  private ThirdPartyScanContext getThirdPartyScanContext(
      final String scanRequestId,
      final File scanFile,
      final Application app,
      final String stageTypeId,
      final ScanContext scanContext)
  {
    if (scanRequestId != null && ComplianceStageType.ID.equals(stageTypeId)) {
      ThirdPartyScan scan = thirdPartyScanDAO.getSingleByScanRequestId(scanRequestId);
      if (scan != null) {
        ThirdPartySbomMetadata sbomMetadata =
            thirdPartySbomMetadataDAO.getByThirdPartyFileId(scan.getThirdPartyFileId());
        ThirdPartyScanContext thirdPartyScanContext =
            new ThirdPartyScanContext(scanRequestId, app.getId(), SbomScanType.valueOf(sbomMetadata.getScanType()),
                scanFile, stageTypeId);
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
        new ThirdPartyScanContext(newScanRequestId, app.getId(), SbomScanType.SBOM, scanFile, stageTypeId);
    if (scanContext != null) {
      thirdPartyScanContext.setApplicationVersion(scanContext.applicationVersion());
      thirdPartyScanContext.setIsValid(scanContext.isValid());
    }
    else {
      thirdPartyScanContext.setIsValid(true);
    }
    return thirdPartyScanContext;
  }
}
