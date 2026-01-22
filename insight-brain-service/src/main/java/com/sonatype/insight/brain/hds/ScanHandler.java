/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.IOException;
import java.io.OutputStream;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;

import static com.sonatype.insight.brain.scan.ScanResource.WEB_UI_REQUEST_ATTRIBUTE;

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
    boolean isWebUIRequest = httpRequest != null
        && Boolean.TRUE.equals(httpRequest.getAttribute(WEB_UI_REQUEST_ATTRIBUTE));

    return handle(ScanRequest.builder()
        .scanEntity(tempScanEntity)
        .application(app)
        .clientScanType(clientScanType)
        .clientUserAgent(HdsClient.getClientUserAgent(httpRequest))
        .httpRequest(httpRequest)
        .isWebUIRequest(isWebUIRequest)
        .build());
  }

  public ScanReceipt handle(ScanRequest scanRequest) throws IOException {
    long start = System.currentTimeMillis();
    log.debug("Received {} scan for application public id {}.",
              scanRequest.getClientScanType(), scanRequest.getApplication().getPublicId());

    try {
      ScanReceipt scanReceipt = scanUploadService.upload(
          scanRequest.getScanEntity(),
          scanRequest.getApplication(),
          scanRequest.getStageTypeId(),
          scanRequest.getClientScanType(),
          scanRequest.getClientUserAgent(),
          scanRequest.getThirdPartyScanTelemetryData(),
          scanRequest.getScanRequestId(),
          scanRequest.getScanContext(),
          scanRequest.isWebUIRequest());

      scanPersistenceService.moveTempScan(scanRequest.getScanEntity(),
                                          scanRequest.getApplication().getId(),
                                          scanReceipt.getScanId());

      log.debug("Handled {} scan id {} for application public id {} in {} ms.",
                scanRequest.getClientScanType(), scanReceipt.getScanId(),
                scanRequest.getApplication().getPublicId(), System.currentTimeMillis() - start);

      return scanReceipt;
    }
    catch (Exception e) {
      try {
        scanPersistenceService.deleteScan(scanRequest.getScanEntity());
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

  public static class ScanRequest
  {
    private final ScanEntity scanEntity;

    private final Application application;

    private final ClientScanType clientScanType;

    private final TelemetryData thirdPartyScanTelemetryData;

    private final String stageTypeId;

    private final String clientUserAgent;

    private final String scanRequestId;

    private final ScanContext scanContext;

    private final HttpServletRequest httpRequest;

    private final boolean isWebUIRequest;

    private ScanRequest(Builder builder) {
      this.scanEntity = builder.scanEntity;
      this.application = builder.application;
      this.clientScanType = builder.clientScanType;
      this.thirdPartyScanTelemetryData = builder.thirdPartyScanTelemetryData;
      this.stageTypeId = builder.stageTypeId;
      this.clientUserAgent = builder.clientUserAgent;
      this.scanRequestId = builder.scanRequestId;
      this.scanContext = builder.scanContext != null ? builder.scanContext
          : new ScanContext.Builder().isValid(true).build();
      this.httpRequest = builder.httpRequest;
      this.isWebUIRequest = builder.isWebUIRequest;
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder
    {
      private ScanEntity scanEntity;

      private Application application;

      private ClientScanType clientScanType;

      private TelemetryData thirdPartyScanTelemetryData;

      private String stageTypeId;

      private String clientUserAgent;

      private String scanRequestId;

      private ScanContext scanContext;

      private HttpServletRequest httpRequest;

      private boolean isWebUIRequest = false;

      public Builder scanEntity(ScanEntity scanEntity) {
        this.scanEntity = scanEntity;
        return this;
      }

      public Builder application(Application application) {
        this.application = application;
        return this;
      }

      public Builder clientScanType(ClientScanType clientScanType) {
        this.clientScanType = clientScanType;
        return this;
      }

      public Builder thirdPartyScanTelemetryData(TelemetryData thirdPartyScanTelemetryData) {
        this.thirdPartyScanTelemetryData = thirdPartyScanTelemetryData;
        return this;
      }

      public Builder stageTypeId(String stageTypeId) {
        this.stageTypeId = stageTypeId;
        return this;
      }

      public Builder clientUserAgent(String clientUserAgent) {
        this.clientUserAgent = clientUserAgent;
        return this;
      }

      public Builder scanRequestId(String scanRequestId) {
        this.scanRequestId = scanRequestId;
        return this;
      }

      public Builder scanContext(ScanContext scanContext) {
        this.scanContext = scanContext;
        return this;
      }

      public Builder httpRequest(HttpServletRequest httpRequest) {
        this.httpRequest = httpRequest;
        return this;
      }

      public Builder isWebUIRequest(boolean isWebUIRequest) {
        this.isWebUIRequest = isWebUIRequest;
        return this;
      }

      public ScanRequest build() {
        if (scanEntity == null || application == null) {
          throw new IllegalArgumentException("scanEntity and application are required");
        }
        return new ScanRequest(this);
      }
    }

    public ScanEntity getScanEntity() {
      return scanEntity;
    }

    public Application getApplication() {
      return application;
    }

    public ClientScanType getClientScanType() {
      return clientScanType;
    }

    public TelemetryData getThirdPartyScanTelemetryData() {
      return thirdPartyScanTelemetryData;
    }

    public String getStageTypeId() {
      return stageTypeId;
    }

    public String getClientUserAgent() {
      return clientUserAgent;
    }

    public String getScanRequestId() {
      return scanRequestId;
    }

    public ScanContext getScanContext() {
      return scanContext;
    }

    public HttpServletRequest getHttpRequest() {
      return httpRequest;
    }

    public boolean isWebUIRequest() {
      return isWebUIRequest;
    }
  }
}
