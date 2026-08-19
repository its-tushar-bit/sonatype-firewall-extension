/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.telemetry.TelemetryReceiptService.TelemetryReceiptDTO;
import com.sonatype.insight.brain.telemetry.TelemetryReceiptService.TelemetryReceiptsDTO;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;

import com.codahale.metrics.annotation.Timed;
import com.google.gson.GsonBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

@Named
@Timed
@Path(TelemetryReceiptResource.TELEMETRY_RECEIPTS_PATH)
public class TelemetryReceiptResource
{
  public static final String TELEMETRY_RECEIPTS_PATH = "rest/telemetry/receipts";

  private static final String TELEMETRY_RECEIPTS_ENABLE_PATH = "enable";

  private static final String TELEMETRY_RECEIPTS_DISABLE_PATH = "disable";

  private final TelemetryReceiptService receiptService;

  @Inject
  public TelemetryReceiptResource(TelemetryReceiptService receiptService) {
    this.receiptService = receiptService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public Response getTelemetryReceipts(@QueryParam("detail") @DefaultValue("") String purposesToDetail) {
    List<String> purposes = StringUtils.isEmpty(purposesToDetail)
        ? Collections.emptyList()
        : Arrays.stream(purposesToDetail.split(","))
            .map(p -> p.trim().toUpperCase())
            .filter(s -> !s.isEmpty())
            .toList();

    var json = telemetryReceiptsToJson(receiptService.getReceipts(purposes));
    return Response.ok()
        .entity(json)
        .header("Cache-Control", "no-cache")
        .build();
  }

  @GET
  @Path(TELEMETRY_RECEIPTS_DISABLE_PATH)
  @Produces(MediaType.TEXT_PLAIN)
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public String disableTelemetryReceipts() {
    receiptService.disable();
    return "Telemetry receipts disabled";
  }

  @GET
  @Path(TELEMETRY_RECEIPTS_ENABLE_PATH)
  @Produces(MediaType.TEXT_PLAIN)
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public String enableTelemetryReceipts(
      @QueryParam("hours") @DefaultValue("1") int hours)
  {
    var enabledHours = receiptService.enable(hours);
    return String.format("Telemetry receipts enabled for %d hour(s)", enabledHours);
  }

  private String telemetryReceiptsToJson(TelemetryReceiptsDTO receiptsDTO) {
    var rootMap = new LinkedHashMap<String, Object>();

    rootMap.put("document", "Telemetry Receipts");
    rootMap.put("revision", "1.0");
    rootMap.put("requestTime", LocalDateTime.now().format(ISO_LOCAL_DATE_TIME));
    rootMap.put("usingProdHds", receiptsDTO.isUsingProdHds());
    rootMap.put("isLocalEnv", receiptsDTO.isLocalEnv());

    if (null == receiptsDTO.captureStartTime()) {
      rootMap.put("status", "disabled");
    }
    else {
      rootMap.put("captureStartTime", receiptsDTO.captureStartTime().format(ISO_LOCAL_DATE_TIME));
      rootMap.put("captureStopTime", receiptsDTO.captureExpirationTime().format(ISO_LOCAL_DATE_TIME));
    }

    addDetailPurposes(rootMap, receiptsDTO.detailPurposes());
    addReceipts(rootMap, receiptsDTO);

    return new GsonBuilder().setPrettyPrinting().create().toJson(rootMap);
  }

  private void addDetailPurposes(Map<String, Object> parentMap, Set<TelemetryPurpose> purposes) {
    var detailPurposesList = new ArrayList<String>();
    parentMap.put("detailingPurposeCodes", detailPurposesList);

    if (CollectionUtils.isNotEmpty(purposes)) {
      for (var purpose : purposes) {
        detailPurposesList.add(purpose.name());
      }
    }
    else {
      detailPurposesList.add("none");
    }
  }

  private void addReceipts(Map<String, Object> parentMap, TelemetryReceiptsDTO receiptsDTO) {
    var receiptsList = new ArrayList<Map<String, Object>>();
    parentMap.put("receipts", receiptsList);

    for (var receipt : receiptsDTO.telemetryReceipts()) {
      var receiptMap = new LinkedHashMap<String, Object>();
      receiptsList.add(receiptMap);

      receiptMap.put("submitTime", receipt.submitTime().format(ISO_LOCAL_DATE_TIME));
      receiptMap.put("queueTimeMs", receipt.queueTimeMs());
      receiptMap.put("httpTimeMs", receipt.httpTimeMs());
      receiptMap.put("errorMessage", receipt.errorMessage());
      receiptMap.put("recordCount", receipt.dataByPurpose().values().stream().mapToInt(List::size).sum());

      addTelemetryData(receiptMap, receipt, receiptsDTO.detailPurposes());
    }
  }

  private void addTelemetryData(
      Map<String, Object> receiptMap,
      TelemetryReceiptDTO receipt,
      Set<TelemetryPurpose> detailPurposes)
  {
    var telemetryMap = new LinkedHashMap<String, Object>();
    receiptMap.put("telemetry", telemetryMap);

    // iterate over the telemetry data that was grouped by purpose
    for (var entry : receipt.dataByPurpose().entrySet()) {
      var purpose = entry.getKey();
      var values = entry.getValue();

      // if the purpose is in the detail purposes, add the telemetry data attributes
      if (detailPurposes.contains(purpose)) {
        var detailedData = new ArrayList<Map<String, Object>>();
        telemetryMap.put(purpose.name(), detailedData);

        for (var data : values) {
          var attributeMap = new LinkedHashMap<String, Object>();
          detailedData.add(attributeMap);

          data.getAttributes()
              .entrySet()
              .stream()
              .sorted(Map.Entry.comparingByKey())
              .forEach(e -> attributeMap.put(e.getKey(), e.getValue()));
        }
      }
      else {
        // otherwise just add the count
        telemetryMap.put(purpose.name(), values.size());
      }
    }
  }
}
