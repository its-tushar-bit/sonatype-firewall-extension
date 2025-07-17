/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.IOException;
import java.io.OutputStream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.scan.ScanContext;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.telemetry.model.TelemetryData;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.24
 */
@Named
public class ScanHandler
{
  private static final Logger log = LoggerFactory.getLogger(ScanHandler.class);

  private final ApplicationDAO appDAO;

  private final ScanUploadService scanUploadService;

  private final ScanPersistenceService scanPersistenceService;

  @Inject
  public ScanHandler(
      final ApplicationDAO appDAO,
      final ScanUploadService scanUploadService,
      final ScanPersistenceService scanPersistenceService)
  {
    this.appDAO = appDAO;
    this.scanUploadService = scanUploadService;
    this.scanPersistenceService = scanPersistenceService;
  }

  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  ScanReceipt handle(
      HttpServletRequest httpRequest,
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      ClientScanType clientScanType) throws IOException
  {
    Application app = appDAO.getByPublicIdNotNull(applicationPublicId);
    ScanEntity tempScanEntity = createTempScanFile(httpRequest, app);
    return handle(tempScanEntity, app, clientScanType, null /* thirdPartyScanTelemetryData */, null /* stageTypeId */,
        HdsClient.getClientUserAgent(httpRequest));
  }

  public ScanReceipt handle(
      ScanEntity tempScanEntity,
      Application app,
      ClientScanType clientScanType,
      TelemetryData thirdPartyScanTelemetryData,
      String stageTypeId,
      String clientUserAgent) throws IOException
  {
    return handle(tempScanEntity, app, clientScanType, thirdPartyScanTelemetryData, stageTypeId, clientUserAgent, null);
  }

  public ScanReceipt handle(
      ScanEntity tempScanEntity,
      Application app,
      ClientScanType clientScanType,
      TelemetryData thirdPartyScanTelemetryData,
      String stageTypeId,
      String clientUserAgent,
      String scanRequestId)
      throws IOException
  {
    return handle(tempScanEntity, app, clientScanType, thirdPartyScanTelemetryData, stageTypeId, clientUserAgent,
        scanRequestId, new ScanContext.Builder().isValid(true).build());
  }

  public ScanReceipt handle(
      ScanEntity tempScanEntity,
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
          scanUploadService.upload(tempScanEntity, app, stageTypeId, clientScanType, clientUserAgent,
              thirdPartyScanTelemetryData, scanRequestId, scanContext);
      scanPersistenceService.moveTempScan(tempScanEntity, app.getId(), scanReceipt.getScanId());

      log.debug("Handled {} scan id {} for application public id {} in {} ms.", clientScanType, scanReceipt.getScanId(),
          app.getPublicId(), System.currentTimeMillis() - start);

      return scanReceipt;
    }
    catch (Exception e) {
      try {
        scanPersistenceService.deleteScan(tempScanEntity);
      }
      catch (IOException ioException) {
        log.warn("Failure when deleting temp scan", ioException);
      }

      throw e;
    }
  }

  public ScanEntity createTempScanFile(HttpServletRequest httpRequest, Application app) throws IOException {
    ScanEntity tempScanEntity = scanPersistenceService.createTempScan(app.getId());

    try {
      saveScanFromHttpRequest(httpRequest, tempScanEntity);
    }
    catch (Exception e) {
      try {
        scanPersistenceService.deleteScan(tempScanEntity);
      }
      catch (IOException ioException) {
        log.warn("Failure when deleting temp scan", ioException);
      }

      throw e;
    }

    return tempScanEntity;
  }

  private void saveScanFromHttpRequest(HttpServletRequest httpRequest, ScanEntity scanEntity) throws IOException {
    try (ServletInputStream is = httpRequest.getInputStream(); OutputStream os = scanEntity.getOutputStream()) {
      IOUtils.copy(is, os);
    }
  }
}
