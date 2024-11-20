/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.scan.ScanContext;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.telemetry.model.TelemetryData;

import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.24
 */
@Named
public class ScanHandler
{
  private static final Logger log = LoggerFactory.getLogger(ScanHandler.class);

  private final InsightWork work;

  private final ApplicationDAO appDAO;

  private final ScanUploadService scanUploadService;

  @Inject
  public ScanHandler(
      InsightWork work,
      ApplicationDAO appDAO,
      ScanUploadService scanUploadService)
  {
    this.work = work;
    this.appDAO = appDAO;
    this.scanUploadService = scanUploadService;
  }

  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  ScanReceipt handle(
      HttpServletRequest httpRequest,
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      ClientScanType clientScanType) throws IOException
  {
    Application app = appDAO.getByPublicIdNotNull(applicationPublicId);
    File tempScanFile = createTempScanFile(httpRequest, app);
    return handle(tempScanFile, app, clientScanType, null /* thirdPartyScanTelemetryData */, null /* stageTypeId */,
        HdsClient.getClientUserAgent(httpRequest));
  }

  public ScanReceipt handle(
      File tempScanFile,
      Application app,
      ClientScanType clientScanType,
      TelemetryData thirdPartyScanTelemetryData,
      String stageTypeId,
      String clientUserAgent) throws IOException
  {
    return handle(tempScanFile, app, clientScanType, thirdPartyScanTelemetryData, stageTypeId, clientUserAgent, null);
  }

  public ScanReceipt handle(
      File tempScanFile,
      Application app,
      ClientScanType clientScanType,
      TelemetryData thirdPartyScanTelemetryData,
      String stageTypeId,
      String clientUserAgent,
      String scanRequestId)
      throws IOException
  {
    return handle(tempScanFile, app, clientScanType, thirdPartyScanTelemetryData, stageTypeId, clientUserAgent,
        scanRequestId, new ScanContext.Builder().isValid(true).build());
  }

  public ScanReceipt handle(
      File tempScanFile,
      Application app,
      ClientScanType clientScanType,
      TelemetryData thirdPartyScanTelemetryData,
      String stageTypeId,
      String clientUserAgent,
      String scanRequestId,
      ScanContext scanContext)
      throws IOException
  {
    long start = System.currentTimeMillis();
    log.debug("Received {} scan for application public id {}.", clientScanType, app.getPublicId());

    try {
      ScanReceipt scanReceipt =
          scanUploadService.upload(tempScanFile, app, stageTypeId, clientScanType, clientUserAgent,
              thirdPartyScanTelemetryData, scanRequestId, scanContext);
      File scanFile = work.getScanFile(app.getId(), scanReceipt.getScanId());
      FileUtils.rename(tempScanFile, scanFile);

      log.debug("Handled {} scan id {} for application public id {} in {} ms.", clientScanType, scanReceipt.getScanId(),
          app.getPublicId(), System.currentTimeMillis() - start);

      return scanReceipt;
    }
    catch (Exception e) {
      try {
        Files.deleteIfExists(tempScanFile.toPath());
      }
      catch (IOException fileDeleteException) {
        log.warn(fileDeleteException.getMessage(), fileDeleteException);
      }

      throw e;
    }
  }

  public File createTempScanFile(HttpServletRequest httpRequest, Application app) throws IOException {
    File tempScanFile = createTempScanFile(app);

    try {
      saveScanFromHttpRequest(httpRequest, tempScanFile);
    }
    catch (Exception e) {
      try {
        Files.deleteIfExists(tempScanFile.toPath());
      }
      catch (IOException fileDeleteException) {
        log.warn(fileDeleteException.getMessage(), fileDeleteException);
      }

      throw e;
    }

    return tempScanFile;
  }

  private void saveScanFromHttpRequest(HttpServletRequest httpRequest, File scanFile) throws IOException {
    try (ServletInputStream is = httpRequest.getInputStream(); FileOutputStream os = new FileOutputStream(scanFile)) {
      IOUtils.copy(is, os);
    }
  }

  private File createTempScanFile(Application app) throws IOException {
    File scanDir = work.getScanDir(app.getId());
    Files.createDirectories(scanDir.toPath());

    return FileUtils.createTempFile("temp-", ".xml.gz", scanDir);
  }
}
