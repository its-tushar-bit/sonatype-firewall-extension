/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.telemetry.TelemetryReceiptService.TelemetryReceipt;
import com.sonatype.insight.brain.tenancy.TenantAwareOneTimeRunnable;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryHeader;

import io.dropwizard.lifecycle.Managed;
import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.43.0
 */
@Named
@Singleton
public class TelemetrySender
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(TelemetrySender.class);

  private final HdsClient hdsClient;

  private final TelemetryReceiptService telemetryReceiptService;

  private final VersionService versionService;

  private final TelemetryId telemetryId;

  private final TenantUtil tenantUtil;

  private final BlockingQueue<TenantAwareOneTimeRunnable> submissions = new LinkedBlockingQueue<>();

  private TelemetrySubmitter submitter;

  private static final String MULTIPART_FILE_NAME = "file";

  public static final String PRODUCT_PREFIX = "nexus-iq";

  public static final String FILE_FORMAT = "zip-bundle/1";

  public static final String HEADER_ENTRY_NAME = "header.json";

  public static final String DATA_ENTRY_NAME = "data.json";

  public static final String RESOURCE_PATH = "rest/environment/stats";

  public static final String ZIP_FILENAME = "telemetry.zip";

  @Inject
  public TelemetrySender(
      HdsClient hdsClient,
      VersionService versionService,
      TelemetryId telemetryId,
      TenantUtil tenantUtil,
      TelemetryReceiptService telemetryReceiptService)
  {
    this.hdsClient = hdsClient;
    this.versionService = versionService;
    this.telemetryId = telemetryId;
    this.tenantUtil = tenantUtil;
    this.telemetryReceiptService = telemetryReceiptService;
  }

  @Override
  public void start() {
    if (submitter == null) {
      submitter = new TelemetrySubmitter();
      submitter.start();
    }
  }

  @Override
  public void stop() {
    if (submitter != null) {
      submitter.interrupt();
      submitter = null;
    }
  }

  public void send(TelemetryData telemetryData) {
    send(telemetryData, null /* clientUserAgent */);
  }

  public void send(List<TelemetryData> telemetryData) {
    send(telemetryData, null /* clientUserAgent */);
  }

  public void send(TelemetryData telemetryData, String clientUserAgent) {
    send(Collections.singletonList(telemetryData), clientUserAgent);
  }

  public void send(List<TelemetryData> telemetryData, String clientUserAgent) {
    if (telemetryData.isEmpty()) {
      return;
    }
    try {
      var telemetrySubmission = new TelemetrySubmission(createZip(createHeader(), telemetryData), clientUserAgent);
      var telemetryReceipt = telemetryReceiptService.onTelemetrySubmitted(telemetryData);
      submissions.add(new TenantAwareOneTimeRunnable(() -> submitTelemetry(telemetrySubmission, telemetryReceipt)));
    }
    catch (Exception e) {
      log.debug("Failed to send telemetry.", e);
    }
  }

  private TelemetryHeader createHeader() {
    String product = PRODUCT_PREFIX + "/" + versionService.getVersion();
    String build = versionService.getBuild();
    Date createTime = new Date();
    return new TelemetryHeader(FILE_FORMAT, product, createTime, telemetryId.getId(),
        telemetryId.getClusterId(), build);
  }

  private byte[] createZip(TelemetryHeader telemetryHeader, List<TelemetryData> telemetryData) throws IOException {
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ZipOutputStream zipOutput = new ZipOutputStream(
        bos)) {
      ZipEntry zipEntryHeader = new ZipEntry(HEADER_ENTRY_NAME);
      zipOutput.putNextEntry(zipEntryHeader);
      zipOutput.write(JsonUtils.generate(telemetryHeader));
      ZipEntry zipEntryData = new ZipEntry(DATA_ENTRY_NAME);
      zipOutput.putNextEntry(zipEntryData);
      zipOutput.write(JsonUtils.generate(telemetryData));
      zipOutput.finish();
      return bos.toByteArray();
    }
  }

  static class TelemetrySubmission
  {
    final byte[] zipData;

    final String clientUserAgent;

    TelemetrySubmission(byte[] zipData, String clientUserAgent) {
      this.zipData = zipData;
      this.clientUserAgent = clientUserAgent;
    }
  }

  class TelemetrySubmitter
      extends Thread
  {
    TelemetrySubmitter() {
      setName(getClass().getSimpleName());
      setDaemon(true);
    }

    @Override
    public void run() {
      while (true) {
        try {
          // Verify this thread should always run as `global` tenant for MTIQ and `single` for on-premise
          tenantUtil.validateNoCustomerTenantSet();

          TenantAwareOneTimeRunnable tenantAwareOneTimeRunnable = submissions.take();
          tenantAwareOneTimeRunnable.run();
        }
        catch (InterruptedException e) {
          // interrupt is our signal to quit
          return;
        }
        catch (Exception e) {
          log.debug("Failed to send telemetry.", e);
        }
        catch (Throwable t) {
          // Try to log to stderr before trying the standard logging because the standard logging may not be operational
          // at this point.
          t.printStackTrace();
          log.error(t.getMessage(), t);
          System.exit(2);
        }
      }
    }
  }

  private void submitTelemetry(final TelemetrySubmission telemetrySubmission, final TelemetryReceipt telemetryReceipt) {
    try {
      telemetryReceipt.markSending();
      ContentBody fileBody = new ByteArrayBody(telemetrySubmission.zipData, ZIP_FILENAME);
      HttpEntity httpEntity = MultipartEntityBuilder.create().addPart(MULTIPART_FILE_NAME, fileBody).build();
      hdsClient.post(RESOURCE_PATH, httpEntity, telemetrySubmission.clientUserAgent);
      telemetryReceipt.markSent();
    }
    catch (Exception e) {
      telemetryReceipt.markInError(e);
      log.error("Failed to send telemetry.", e);
      throw(e);
    }
  }
}
