/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.telemetry.model.TelemetryData;

import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ThirdPartyScanService
{
  private static final Logger log = LoggerFactory.getLogger(ThirdPartyScanService.class);

  private final ScanUploader uploader;

  private final InsightWork work;

  private final ThirdPartyScanResultsProcessor scanResultsProcessor;

  @Inject
  public ThirdPartyScanService(
      ThirdPartyScanResultsProcessor scanResultsProcessor,
      ScanUploader uploader,
      InsightWork work)
  {
    this.scanResultsProcessor = scanResultsProcessor;
    this.uploader = uploader;
    this.work = work;
  }

  public ScanReceipt filterAndUpload(
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
        scanResultsProcessor.filterAndSaveData(scanFile, tempScanFile, scanDir, thirdPartyScanTelemetryData);
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
