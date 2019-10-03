/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Named;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.json.store.UncheckedIOException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.75
 */
@Named
public class ThirdPartyComponentDAO
{
  public static final String THIRD_PARTY_BOM_JSON_FILENAME = "thirdparty-bom.json";

  public static final String THIRD_PARTY_SECURITY_JSON_FILENAME = "thirdparty-security.json";

  private static final Logger log = LoggerFactory.getLogger(ThirdPartyComponentDAO.class);

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Prepare and return a collection of ThirdPartyReportComponentDTOs mapped against component hash.
   * The hash is whats received by HDS.
   *
   * @return A Map of ThirdPartyReportComponentDTOs identifiable against hash.
   */
  public Map<String, ThirdPartyReportComponentDTO> getData(final File reportFile) {
    if (reportFile == null) {
      return null;
    }
    log.debug("Reading third party report data from file {}", reportFile.getAbsolutePath());

    Map<String, ThirdPartyReportComponentDTO> reportData = new HashMap<>();
    try {
      final ReportEntry tpBomEntry = Report.getEntry(reportFile, THIRD_PARTY_BOM_JSON_FILENAME);
      final List<ThirdPartyBillOfMaterialsRowDTO> bomRows =
          readData(tpBomEntry, new TypeReference<List<ThirdPartyBillOfMaterialsRowDTO>>() { });
      if (bomRows != null && !bomRows.isEmpty()) {
        ReportEntry tpSecurityReportEntry = Report.getEntry(reportFile, THIRD_PARTY_SECURITY_JSON_FILENAME);
        final List<ThirdPartyHealthCheckReportSecurityRowDTO> securityRows =
            readData(tpSecurityReportEntry, new TypeReference<List<ThirdPartyHealthCheckReportSecurityRowDTO>>() { });
        prepareComponentData(bomRows, securityRows, reportData);
      }
    }
    catch (Exception e) {
      log.error("error attempting to read third party data from report {}", reportFile.getAbsolutePath(), e);
    }
    return reportData;
  }

  public void applyThirdPartyComponentSummary(
      final List<Component> thirdPartyIdentifiedComponents,
      final File reportFile)
  {
    try {
      if (!thirdPartyIdentifiedComponents.isEmpty()) {
        updateSummaryCounts(reportFile, thirdPartyIdentifiedComponents.size());
        updateDataCounts(reportFile, thirdPartyIdentifiedComponents.size());
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void updateSummaryCounts(final File reportFile, final int thirdPartyComponentCount)
      throws IOException
  {
    String filename = "summary.json";
    final ObjectNode summary = loadJson(Report.getEntry(reportFile, filename).buf);
    long knownArtifactCount = summary.path("knownArtifactCount").asLong(0);

    summary.put("knownArtifactCount", knownArtifactCount + thirdPartyComponentCount);
    Report.putEntry(reportFile, filename, JsonUtils.generate(summary));
  }

  private void updateDataCounts(final File reportFile, final int thirdPartyComponentCount)
      throws IOException
  {
    String filename = "data.json";
    final ObjectNode summary = loadJson(Report.getEntry(reportFile, filename).buf);
    long knownArtifactCount = summary.path("knownArtifactCount").asLong(0);
    long exactlyMatchedComponentCount = summary.path("exactlyMatchedComponentCount").asLong(0);

    summary.put("knownArtifactCount", knownArtifactCount + thirdPartyComponentCount);
    summary.put("exactlyMatchedComponentCount", exactlyMatchedComponentCount + thirdPartyComponentCount);
    Report.putEntry(reportFile, filename, JsonUtils.generate(summary));
  }

  private void prepareComponentData(
      final List<ThirdPartyBillOfMaterialsRowDTO> bomRows,
      final List<ThirdPartyHealthCheckReportSecurityRowDTO> securityRows,
      final Map<String, ThirdPartyReportComponentDTO> reportData)
  {
    for (ThirdPartyBillOfMaterialsRowDTO bomRow : bomRows) {
      final ThirdPartyReportComponentDTO dto = new ThirdPartyReportComponentDTO(bomRow);
      if (securityRows != null && !securityRows.isEmpty()) {
        dto.securityRows.addAll(
            securityRows.stream().filter(row -> row.componentIdentifier.equals(dto.componentIdentifier))
                .collect(Collectors.toList()));
      }
      reportData.put(bomRow.hash, dto);
    }
  }

  private <T> T readData(ReportEntry reportEntry, TypeReference<T> type) throws IOException {
    if (reportEntry != null) {
      JsonNode bomNode = loadJson(reportEntry.buf);
      JsonNode rootNode = bomNode.get("aaData");
      return MAPPER.readValue(rootNode.traverse(), type);
    }
    return null;
  }

  private <T extends ContainerNode<?>> T loadJson(final byte[] data) {
    if (data == null) {
      return null;
    }
    try {
      return JsonUtils.parse(data);
    }
    catch (final IOException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
