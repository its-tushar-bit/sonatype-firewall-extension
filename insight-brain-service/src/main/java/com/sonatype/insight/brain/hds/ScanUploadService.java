/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultsProcessor;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.telemetry.model.TelemetryData;

import org.codehaus.plexus.util.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ScanUploadService
{
  private static final Logger log = LoggerFactory.getLogger(ScanUploadService.class);

  private final ScanUploader uploader;

  private final InsightWork work;

  private final ThirdPartyScanResultsProcessor scanResultsProcessor;

  @Inject
  public ScanUploadService(
      ThirdPartyScanResultsProcessor scanResultsProcessor,
      ScanUploader uploader,
      InsightWork work)
  {
    this.scanResultsProcessor = scanResultsProcessor;
    this.uploader = uploader;
    this.work = work;
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
    if (scanFile == null || app == null) {
      throw new IllegalArgumentException("scanFile and application is required for scan uploads");
    }

    ScanReceipt scanReceipt;
    if (ClientScanType.SONATYPE_THIRD_PARTY.equals(clientScanType)) {
      scanReceipt = filterAndUpload(scanFile, app, stageTypeId, clientUserAgent, thirdPartyScanTelemetryData);
    }
    else {
      scanReceipt = uploader.upload(scanFile, app, stageTypeId, clientUserAgent);
      if (ComplianceStageType.ID.equals(stageTypeId) && StringUtils.isNotEmpty(scanRequestId)) {
        scanResultsProcessor.postHandle(scanReceipt.getScanId(), scanRequestId);
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
      TelemetryData thirdPartyScanTelemetryData)
      throws IOException
  {
    File scanDir = work.getScanDir(app.getId());
    File tempScanFile = FileUtils.createTempFile("tmp-", ".xml.gz", scanDir);
    String scanRequestId =
        scanResultsProcessor.filterAndSaveData(scanFile, tempScanFile, scanDir, thirdPartyScanTelemetryData,
            app.getId(), stageTypeId);
    ScanReceipt scanReceipt = uploader.upload(tempScanFile, app, stageTypeId, clientUserAgent);
    scanResultsProcessor.postHandle(scanReceipt.getScanId(), scanRequestId);
    try {
      Files.delete(tempScanFile.toPath());
    }
    catch (IOException e) {
      log.error("Unable to remove temporary scan file {}", tempScanFile.toPath());
    }
    return scanReceipt;
  }
}
