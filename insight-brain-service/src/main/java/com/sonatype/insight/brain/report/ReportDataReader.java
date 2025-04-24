/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.scan.HealthCheckReportRowDTO;
import com.sonatype.insight.scan.HealthCheckReportSecurityRowDTO;
import com.sonatype.insight.scan.application.BillOfMaterialsRowDTO;
import com.sonatype.insight.util.MetadataRecorderUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.Weigher;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Named
public class ReportDataReader
{
  public static final String BOM_JSON_FILENAME = "bom.json";

  public static final String SECURITY_JSON_FILENAME = "security.json";

  public static final String LICENSE_JSON_FILENAME = "licenses.json";

  private static final Logger log = LoggerFactory.getLogger(ReportDataReader.class);

  public static final ObjectMapper MAPPER = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  // For testing visibility
  final TenantReference<Cache<String, Table<String, ComponentIdentifier, ReportComponentDTO>>>
      componentCache;

  private static final Comparator<ComparableVersion> comparator = ComparableVersion::compareTo;

  private final Provider<ReportService> reportServiceProvider;

  private final MultiLicenseDAO multiLicenseDAO;

  @Inject
  public ReportDataReader(final Provider<ReportService> reportServiceProvider, final MultiLicenseDAO multiLicenseDAO) {
    this.reportServiceProvider = reportServiceProvider;
    this.multiLicenseDAO = multiLicenseDAO;
    componentCache = new TenantReference<>(() -> CacheBuilder.newBuilder()
        .expireAfterAccess(1, TimeUnit.DAYS)
        .maximumWeight(100000)
        .weigher((Weigher<String, Table<String, ComponentIdentifier, ReportComponentDTO>>) (key, value) ->
            value.size())
        .build());
  }

  public NamedComponentDetails getComponentDetailsByIdentifier(
      final ComponentIdentifier identifier,
      final String appId,
      final String scanId)
  {
    return resolveComponentDetails(findComponent(appId, identifier, scanId));
  }

  private Map<String, ReportComponentDTO> getData(String scanId, ApplicationReport applicationReport) {
    if (applicationReport == null) {
      return null;
    }
    log.debug("Reading report data from {}", applicationReport.getLocation());

    Map<String, ReportComponentDTO> reportData = new HashMap<>();
    try {
      final ReportEntry bomEntry = applicationReport.getEntry(BOM_JSON_FILENAME);
      final List<BillOfMaterialsRowDTO> bomRows =
          readData(bomEntry, new TypeReference<>() { });
      if (bomRows != null && !bomRows.isEmpty()) {
        ReportEntry securityReportEntry = applicationReport.getEntry(SECURITY_JSON_FILENAME);
        final List<HealthCheckReportSecurityRowDTO> securityRows =
            readData(securityReportEntry, new TypeReference<>() { });
        ReportEntry licenseReportEntry = applicationReport.getEntry(LICENSE_JSON_FILENAME);
        final List<HealthCheckReportRowDTO> licenseRows =
            readData(licenseReportEntry, new TypeReference<>() { });
        prepareComponentData(bomRows, securityRows, licenseRows, reportData);
      }
    }
    catch (IOException e) {
      throw new RuntimeException("error reading report data for scan with ID: " + scanId, e);
    }

    return reportData;
  }

  private ReportComponentDTO findComponent(
      final String appId,
      final ComponentIdentifier identifier,
      final String scanId)
  {
    Table<String, ComponentIdentifier, ReportComponentDTO> scannedComponents =
        componentCache.get().getIfPresent(scanId);

    if (scannedComponents == null) {
      final Map<String, ReportComponentDTO> data =
          getData(scanId, reportServiceProvider.get().getReport(appId, scanId));
      if (data == null || data.isEmpty()) {
        return null;
      }

      scannedComponents = HashBasedTable.create();
      for (Entry<String, ReportComponentDTO> dataEntry : data.entrySet()) {
        scannedComponents.put(dataEntry.getKey(), dataEntry.getValue().componentIdentifier,
            dataEntry.getValue());
      }
      componentCache.get().put(scanId, scannedComponents);
    }

    final Map<String, ReportComponentDTO> detailsByIdentifier = scannedComponents.column(identifier);
    if (!detailsByIdentifier.isEmpty()) {
      return detailsByIdentifier.values().iterator().next();
    }
    return null;
  }

  private NamedComponentDetails resolveComponentDetails(final ReportComponentDTO componentDTO) {
    if (componentDTO == null) {
      return null;
    }

    final NamedComponentDetails componentDetails = new NamedComponentDetails();
    componentDetails.setComponentIdentifier(componentDTO.componentIdentifier);
    componentDetails.setHash(componentDTO.bomRow.hash);
    componentDetails.setMatchState(componentDTO.bomRow.matchState);
    componentDetails.setIdentificationSource(componentDTO.bomRow.identificationSource);
    componentDetails.setAnalyzerFeatures(MetadataRecorderUtils.fromThirdParty(null));
    componentDetails.setSecurityVulnerabilities(
        componentDTO.securityRows.stream().map(this::toSecurityVulnerability).collect(Collectors.toList()));
    if (componentDTO.licenseRow != null) {
      componentDetails.setDeclaredLicenses(toLicenses(componentDTO.licenseRow.declaredLicenses));
      componentDetails.setObservedLicenses(toLicenses(componentDTO.licenseRow.observedLicenses));
      componentDetails.setEffectiveLicenses(toLicenses(componentDTO.licenseRow.effectiveLicenses));
    }
    else {
      componentDetails.setDeclaredLicenses(Set.of());
      componentDetails.setObservedLicenses(Set.of());
      componentDetails.setEffectiveLicenses(Set.of());
    }
    return componentDetails;
  }

  private @NotNull Set<License> toLicenses(final Set<String> licenses) {
    return licenses != null ? licenses.stream().map(this::getLicense).collect(Collectors.toSet()) : Set.of();
  }

  private License getLicense(String licenseId) {
    MultiLicense multiLicense = multiLicenseDAO.getById(licenseId);
    return multiLicense != null ? new License(multiLicense.getId(), multiLicense.getShortDisplayName()) : new License(
        licenseId, licenseId);
  }

  private void prepareComponentData(
      final List<BillOfMaterialsRowDTO> bomRows,
      final List<HealthCheckReportSecurityRowDTO> securityRows,
      final List<HealthCheckReportRowDTO> licenseRows,
      final Map<String, ReportComponentDTO> reportData)
  {
    for (BillOfMaterialsRowDTO bomRow : bomRows) {
      final ReportComponentDTO dto = new ReportComponentDTO(bomRow);
      if (securityRows != null && !securityRows.isEmpty()) {
        dto.securityRows.addAll(
            securityRows.stream().filter(row -> row.componentIdentifier.equals(dto.componentIdentifier)).toList());
      }
      if (licenseRows != null && !licenseRows.isEmpty()) {
        licenseRows.stream().filter(row -> row.componentIdentifier.equals(dto.componentIdentifier)).findFirst()
            .ifPresent(license -> dto.licenseRow = license);
      }
      reportData.put(bomRow.hash, dto);
    }
  }

  private <T> T readData(ReportEntry reportEntry, TypeReference<T> type) throws IOException {
    if (reportEntry != null) {
      JsonNode bomNode = loadJson(reportEntry.buf);
      JsonNode rootNode = bomNode.get("aaData");
      JsonParser jsonParser = rootNode.traverse();
      if (jsonParser.getCodec() == null) {
        jsonParser.setCodec(MAPPER);
      }
      return MAPPER.readValue(jsonParser, type);
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
      final HealthCheckReportSecurityRowDTO secRow)
  {
    SecurityVulnerability securityVulnerability =
        new SecurityVulnerability(secRow.reference, secRow.source, secRow.score, secRow.summary);
    securityVulnerability.setUrl(secRow.url);
    securityVulnerability.setCvssVector(secRow.cvssVectorString);
    securityVulnerability.setCvssVectorSource(secRow.cvssVectorSource);
    securityVulnerability.setVulnerabilityCategories(secRow.vulnerabilityCategories);
    securityVulnerability.setAliases(secRow.aliases);
    securityVulnerability.setCwe(secRow.cwe);
    return securityVulnerability;
  }
}
