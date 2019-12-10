/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.SecurityVulnerabilityDetails;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.json.store.UncheckedIOException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.Weigher;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.76
 */
@Named
public class ThirdPartyComponentDAO
{
  public static final String THIRD_PARTY_BOM_JSON_FILENAME = "thirdparty-bom.json";

  public static final String THIRD_PARTY_SECURITY_JSON_FILENAME = "thirdparty-security.json";

  public static final String THIRD_PARTY_LICENSE_JSON_FILENAME = "thirdparty-license.json";

  private static final Logger log = LoggerFactory.getLogger(ThirdPartyComponentDAO.class);

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final InsightWork work;

  private final Cache<String, Table<String, ComponentIdentifier, ThirdPartyReportComponentDTO>> componentCache;

  @Inject
  public ThirdPartyComponentDAO(final InsightWork work) {
    this.work = work;
    componentCache = CacheBuilder.newBuilder()
        .expireAfterAccess(1, TimeUnit.DAYS)
        .maximumWeight(100000)
        .weigher((Weigher<String, Table<String, ComponentIdentifier, ThirdPartyReportComponentDTO>>) (key, value) ->
            value.size())
        .build();
  }

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
        ReportEntry tpLicenseReportEntry = Report.getEntry(reportFile, THIRD_PARTY_LICENSE_JSON_FILENAME);
        final List<ThirdPartyLicenseRowDTO> licenseRows =
            readData(tpLicenseReportEntry, new TypeReference<List<ThirdPartyLicenseRowDTO>>() {});
        prepareComponentData(bomRows, securityRows, licenseRows, reportData);
      }
    }
    catch (Exception e) {
      log.error("error attempting to read third party data from report {}", reportFile.getAbsolutePath(), e);
    }
    return reportData;
  }

  public void applyIdentifiedComponentUpdates(
      final List<ThirdPartyBillOfMaterialsRowDTO> thirdPartyIdentifiedComponents,
      final File reportFile)
  {
    try {
      if (!thirdPartyIdentifiedComponents.isEmpty()) {
        updateBom(thirdPartyIdentifiedComponents, reportFile);
        updateSummaryCounts(reportFile, thirdPartyIdentifiedComponents.size());
        updateDataCounts(reportFile, thirdPartyIdentifiedComponents.size());
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * This returns only the identified component by this component identifier as we have no means of collecting all
   * known versions of it
   */
  public ComponentDetailsList getAllVersions(
      final String appId,
      final ComponentIdentifier identifier,
      final String scanId)
  {
    final ComponentDetails component = resolveComponentDetails(findComponent(appId, identifier, scanId));
    ComponentDetailsList componentDetailsList = new ComponentDetailsList();
    if (component != null) {
      componentDetailsList.setList(Collections.singletonList(component));
    }
    else {
      componentDetailsList.setList(Collections.emptyList());
    }
    return componentDetailsList;
  }

  public NamedComponentDetails getComponentDetailsByIdentifier(
      final ComponentIdentifier identifier,
      final String appId,
      final String scanId)
  {
    return resolveComponentDetails(findComponent(appId, identifier, scanId));
  }

  public ComponentSummary getComponentSummary(
      final ComponentIdentifier identifier,
      final String appId,
      final String scanId)
  {
    if (findComponent(appId, identifier, scanId) != null) {
      return ComponentSummary.create(true);
    }
    return ComponentSummary.create(false);
  }

  public SecurityVulnerabilityDetails getSecurityVulnerabilityDetailsByIdentifier(
      final ComponentIdentifier identifier,
      final String appId,
      final String scanId,
      final String refId)
  {
    ThirdPartyReportComponentDTO dto = findComponent(appId, identifier, scanId);

    if (dto != null) {
      return dto.securityRows.stream()
        .filter(row -> row.reference.equals(refId))
        .map(row -> new SecurityVulnerabilityDetails(
            row.source, refId, ThirdPartySecurityVulnerabilityRenderer.renderHtml(row)))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Vulnerability with refid: " + refId + " not found."));
    }

    throw new NotFoundException("Vulnerability with refid: " + refId + " not found.");
  }

  private ThirdPartyReportComponentDTO findComponent(
      final String appId,
      final ComponentIdentifier identifier,
      final String scanId)
  {
    Table<String, ComponentIdentifier, ThirdPartyReportComponentDTO> scannedComponents =
        componentCache.getIfPresent(scanId);

    if (scannedComponents == null) {
      final Map<String, ThirdPartyReportComponentDTO> data = getData(work.getReportFile(appId, scanId));
      if (data == null || data.isEmpty()) {
        return null;
      }

      scannedComponents = HashBasedTable.create();
      for (Entry<String, ThirdPartyReportComponentDTO> thirdPartyDataEntry : data.entrySet()) {
        scannedComponents.put(thirdPartyDataEntry.getKey(), thirdPartyDataEntry.getValue().componentIdentifier,
            thirdPartyDataEntry.getValue());
      }
      componentCache.put(scanId, scannedComponents);
    }

    final Map<String, ThirdPartyReportComponentDTO> detailsByIdentifier = scannedComponents.column(identifier);
    if (!detailsByIdentifier.isEmpty()) {
      return detailsByIdentifier.values().iterator().next();
    }
    return null;
  }

  private NamedComponentDetails resolveComponentDetails(final ThirdPartyReportComponentDTO componentDTO) {
    if (componentDTO == null) {
      return null;
    }

    final NamedComponentDetails componentDetails = new NamedComponentDetails();
    componentDetails.setComponentIdentifier(componentDTO.componentIdentifier);
    componentDetails.setHash(componentDTO.bomRow.hash);
    componentDetails.setMatchState(componentDTO.bomRow.matchState);
    componentDetails.setIdentificationSource(componentDTO.bomRow.identificationSource);
    componentDetails.setSecurityVulnerabilities(
        componentDTO.securityRows.stream().map(this::toSecurityVulnerability).collect(Collectors.toList()));
    componentDetails.setDeclaredLicenses(
        componentDTO.licensesRow.declaredLicenses.stream().map(this::getLicense).collect(Collectors.toSet()));
    return componentDetails;
  }

  private License getLicense(ThirdPartyLicenseDTO licenseRow) {
    License license = new License();
    license.setLicenseId(licenseRow.id);
    if (StringUtils.isNotBlank(licenseRow.name)) {
      license.setLicenseName(licenseRow.name);
    }
    else {
      license.setLicenseName(licenseRow.id);
    }
    return license;
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

  private void updateBom(
      final List<ThirdPartyBillOfMaterialsRowDTO> thirdPartyIdentifiedComponents,
      final File reportFile) throws IOException
  {
    final ContainerNode<?> bom = JsonUtils.parse(Report.getEntry(reportFile, "bom.json").buf);
    final ArrayNode bomArray = (ArrayNode) bom.get("aaData");
    final ArrayNode thirdPartyBomArray = MAPPER.valueToTree(thirdPartyIdentifiedComponents);

    for (JsonNode tpNode : thirdPartyBomArray) {
      for (int i = 0; i < bomArray.size(); i++) {
        final JsonNode bomNode = bomArray.get(i);
        if (tpNode.path("hash").asText().equals(bomNode.path("hash").asText())) {
          mergeNodes(bomNode, tpNode);
          bomArray.set(i, tpNode);
          break;
        }
      }
    }

    Report.putEntry(reportFile, "bom.json", JsonUtils.generate(JsonUtils.aaData(bomArray)));
  }

  private void mergeNodes(final JsonNode bomNode, final JsonNode tpNode) {
    final ObjectNode tpObjectNode = (ObjectNode) tpNode;
    tpObjectNode.replace("filenames", bomNode.get("filenames"));
    tpObjectNode.replace("pathnames", bomNode.get("pathnames"));
    ComponentDisplayNameUtil.injectDisplayName(tpObjectNode);
  }

  private void prepareComponentData(
      final List<ThirdPartyBillOfMaterialsRowDTO> bomRows,
      final List<ThirdPartyHealthCheckReportSecurityRowDTO> securityRows,
      final List<ThirdPartyLicenseRowDTO> licenseRows,
      final Map<String, ThirdPartyReportComponentDTO> reportData)
  {
    for (ThirdPartyBillOfMaterialsRowDTO bomRow : bomRows) {
      final ThirdPartyReportComponentDTO dto = new ThirdPartyReportComponentDTO(bomRow);
      if (securityRows != null && !securityRows.isEmpty()) {
        dto.securityRows.addAll(
            securityRows.stream().filter(row -> row.componentIdentifier.equals(dto.componentIdentifier))
                .collect(Collectors.toList()));
      }
      if (licenseRows != null && !licenseRows.isEmpty()) {
        licenseRows.stream().filter(row -> row.componentIdentifier.equals(dto.componentIdentifier)).findFirst()
            .ifPresent(license -> dto.licensesRow = license);
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

  private SecurityVulnerability toSecurityVulnerability(
      final ThirdPartyHealthCheckReportSecurityRowDTO secRow)
  {
    SecurityVulnerability securityVulnerability =
        new SecurityVulnerability(secRow.reference, secRow.source, secRow.score, secRow.description);
    securityVulnerability.setUrl(secRow.url);
    return securityVulnerability;
  }
}
