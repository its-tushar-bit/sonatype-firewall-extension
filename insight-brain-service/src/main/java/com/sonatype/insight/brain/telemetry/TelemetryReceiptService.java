/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.apache.commons.collections4.CollectionUtils;

@Named
@Singleton
public class TelemetryReceiptService
{
  private static final TelemetryReceipt NO_RECEIPT = new TelemetryReceipt(List.of());

  private static final String LOCALHOST_URL = "http://localhost";

  private static final String PROD_HDS_URL = "https://clm.sonatype.com";

  private final Configuration configuration;

  private final TenantReference<LocalDateTime> telemetryCaptureExpirationTime = new TenantReference<>();

  private final TenantReference<LocalDateTime> telemetryCaptureStartTime = new TenantReference<>();

  private final TenantReference<List<TelemetryReceipt>> telemetryReceipts = new TenantReference<>(ArrayList::new);

  @Inject
  public TelemetryReceiptService(Configuration configuration) {
    this.configuration = configuration;
  }

  public void disable() {
    clearAll();
  }

  public int enable(int hours) {
    if (!isForLocalhost()) {
      return 0;
    }

    final var captureHours = hours > 24 ? 24 : (Math.max(hours, 1));
    final var now = LocalDateTime.now();
    telemetryCaptureStartTime.set(now);
    final var expirationTime = now.plusHours(captureHours);
    telemetryCaptureExpirationTime.set(expirationTime);

    return captureHours;
  }

  public TelemetryReceiptsDTO getReceipts(List<String> purposesToDetail) {
    var receiptDTOS = new ArrayList<TelemetryReceiptDTO>();
    Set<TelemetryPurpose> detailPurposes = Set.of();

    if (isForLocalhost()) {
      detailPurposes = toPurposeCodeList(purposesToDetail);
      var zone = ZoneId.systemDefault();

      for (TelemetryReceipt receipt : telemetryReceipts.get()) {
        var submitTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(receipt.submitTimeMs), zone
        );

        var queueTimeMs = receipt.sentTimeMs > 0 ? receipt.sentTimeMs - receipt.submitTimeMs : -1;
        var httpTimeMs = receipt.completeTimeMs > 0 ? receipt.completeTimeMs - receipt.sentTimeMs : -1;
        var errorMessage = receipt.errorResult != null
            ? receipt.errorResult.getMessage()
            : null;

        // group raw TelemetryData by purpose
        var telemetryDataByPurpose = receipt.telemetryData.stream()
            .collect(Collectors.groupingBy(
                TelemetryData::getPurpose,
                () -> new TreeMap<>(Comparator.comparing(TelemetryPurpose::name)),
                Collectors.toList()
            ));

        receiptDTOS.add(new TelemetryReceiptDTO(
            submitTime, queueTimeMs, httpTimeMs, errorMessage, telemetryDataByPurpose
        ));
      }
    }

    return new TelemetryReceiptsDTO(
        telemetryCaptureStartTime.get(),
        telemetryCaptureExpirationTime.get(),
        isUsingProdHds(),
        isForLocalhost(),
        detailPurposes,
        receiptDTOS
    );
  }

  public TelemetryReceipt onTelemetrySubmitted(List<TelemetryData> telemetryData) {
    if (!isForLocalhost() || !shouldCapture(telemetryData)) {
      return NO_RECEIPT;
    }

    return addReceiptFor(telemetryData);
  }

  private boolean isForLocalhost() {
    if (null == configuration) {
      return false; // configuration not set, cannot be localhost
    }
    var baseUrl = configuration.getBaseUrl();
    return null != baseUrl && baseUrl.trim().toLowerCase().startsWith(LOCALHOST_URL);
  }

  private boolean isUsingProdHds() {
    if (null == configuration) {
      return false;
    }
    var hdsUrl = configuration.getHdsUrl();
    return null != hdsUrl && hdsUrl.trim().toLowerCase().startsWith(PROD_HDS_URL.toLowerCase());
  }

  private Set<TelemetryPurpose> toPurposeCodeList(List<String> purposesToDetail) {
    if (CollectionUtils.isEmpty(purposesToDetail)) {
      return Collections.emptySet();
    }

    var purposes = new HashSet<TelemetryPurpose>();

    for (String purpose : purposesToDetail) {
      try {
        purposes.add(TelemetryPurpose.valueOf(purpose.toUpperCase()));
      }
      catch (IllegalArgumentException e) {
        // Ignore invalid purpose
      }
    }

    return purposes;
  }

  private TelemetryReceipt addReceiptFor(List<TelemetryData> telemetryData) {
    var telemetryReceipt = new TelemetryReceipt(telemetryData);
    telemetryReceipts.get().add(telemetryReceipt);
    return telemetryReceipt;
  }

  private boolean shouldCapture(List<TelemetryData> telemetryData) {
    return CollectionUtils.isNotEmpty(telemetryData) && !isCaptureExpired();
  }

  private boolean isCaptureExpired() {
    var expiration = telemetryCaptureExpirationTime.get();
    return null == expiration || expiration.isBefore(LocalDateTime.now());
  }

  private void clearAll() {
    telemetryCaptureStartTime.remove();
    telemetryCaptureExpirationTime.remove();
    telemetryReceipts.get().clear();
  }

  public record TelemetryReceiptsDTO(LocalDateTime captureStartTime,
                                     LocalDateTime captureExpirationTime,
                                     boolean isUsingProdHds,
                                     boolean isLocalEnv,
                                     Set<TelemetryPurpose> detailPurposes,
                                     List<TelemetryReceiptDTO> telemetryReceipts)
  {
  }

  public record TelemetryReceiptDTO(LocalDateTime submitTime,
                                    long queueTimeMs,
                                    long httpTimeMs,
                                    String errorMessage,
                                    Map<TelemetryPurpose, List<TelemetryData>> dataByPurpose)
  {
  }

  public static class TelemetryReceipt
  {
    private final List<TelemetryData> telemetryData;

    private final long submitTimeMs = System.currentTimeMillis();

    private long sentTimeMs;

    private long completeTimeMs;

    private Exception errorResult;

    public TelemetryReceipt(List<TelemetryData> telemetryData) {
      this.telemetryData = telemetryData;
    }

    public void markInError(Exception error) {
      errorResult = error;
      completeTimeMs = System.currentTimeMillis();
    }

    public void markSending() {
      sentTimeMs = System.currentTimeMillis();
    }

    public void markSent() {
      completeTimeMs = System.currentTimeMillis();
    }
  }
}
