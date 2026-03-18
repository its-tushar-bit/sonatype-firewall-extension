/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.SecurityVulnerabilityDetails;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.vulnerability.VulnerabilityExploitRiskData;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.report.ApplicationReport;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.HealthCheckReportRowDTO;
import com.sonatype.insight.scan.HealthCheckReportSecurityRowDTO;
import com.sonatype.insight.scan.ThirdPartyHealthCheckReportSecurityRowDTO;
import com.sonatype.insight.scan.ThirdPartyVulnerabilityExploitabilityExchangeRowDTO;
import com.sonatype.insight.util.MetadataRecorderUtils;
import com.sonatype.insight.vulnerability.model.BulkSecurityVulnerabilityDataDTO;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.Weigher;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Table;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.THIRD_PARTY_BOM_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.THIRD_PARTY_LICENSE_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.THIRD_PARTY_SECURITY_JSON;
import static com.sonatype.insight.brain.report.DependencyResolver.MATCH_STATE;
import static java.lang.System.currentTimeMillis;

/**
 * @since 1.76
 */
@Named
public class ThirdPartyComponentDAO
{
  private static final Logger log = LoggerFactory.getLogger(ThirdPartyComponentDAO.class);

  public static final ObjectMapper MAPPER = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  // For testing visibility
  final TenantReference<Cache<String, Table<String, ComponentIdentifier, ThirdPartyReportComponentDTO>>> componentCache;

  private static final Comparator<ComparableVersion> comparator = ComparableVersion::compareTo;

  private Provider<ReportService> reportServiceProvider;

  private final HdsClient client;

  private static final String HDS_BULK_VULN_DATA_PATH = "/rest/vulnerability/details/json";

  @Inject
  public ThirdPartyComponentDAO(final Provider<ReportService> reportServiceProvider, final HdsClient client) {
    this.reportServiceProvider = reportServiceProvider;
    this.client = client;
    componentCache = new TenantReference<>(() -> CacheBuilder.newBuilder()
        .expireAfterAccess(1, TimeUnit.DAYS)
        .maximumWeight(100000)
        .weigher(
            (Weigher<String, Table<String, ComponentIdentifier, ThirdPartyReportComponentDTO>>) (key, value) -> value
                .size())
        .build());
  }

  /**
   * Prepare and return a collection of ThirdPartyReportComponentDTOs mapped against component hash.
   * The hash is whats received by HDS.
   *
   * @return A Map of ThirdPartyReportComponentDTOs identifiable against hash.
   */
  public Map<String, ThirdPartyReportComponentDTO> getData(final ApplicationReport applicationReport) {
    if (applicationReport == null) {
      return null;
    }
    log.debug("Reading third party report data from {}", applicationReport.getLocation());

    Map<String, ThirdPartyReportComponentDTO> reportData = new HashMap<>();
    try {
      final ReportEntry tpBomEntry = applicationReport.getEntry(THIRD_PARTY_BOM_JSON.getName());
      final List<ThirdPartyBillOfMaterialsRowDTO> bomRows =
          readData(tpBomEntry, new TypeReference<>()
          {
          });
      if (bomRows != null && !bomRows.isEmpty()) {
        ReportEntry tpSecurityReportEntry = applicationReport.getEntry(THIRD_PARTY_SECURITY_JSON.getName());
        final List<ThirdPartyHealthCheckReportSecurityRowDTO> securityRows =
            readData(tpSecurityReportEntry, new TypeReference<>()
            {
            });
        ReportEntry tpLicenseReportEntry = applicationReport.getEntry(THIRD_PARTY_LICENSE_JSON.getName());
        final List<ThirdPartyLicenseRowDTO> licenseRows =
            readData(tpLicenseReportEntry, new TypeReference<>()
            {
            });
        prepareComponentData(bomRows, securityRows, licenseRows, reportData);
      }
    }
    catch (Exception e) {
      log.error("error attempting to read third party data from report {}", applicationReport.getLocation(), e);
    }
    return reportData;
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

  public ComponentDetails resolveComponentDetails(
      final String appId,
      final ComponentIdentifier identifier,
      final String scanId)
  {
    return resolveComponentDetails(findComponent(appId, identifier, scanId));
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

  private ThirdPartyHealthCheckReportSecurityRowDTO findThirdPartyHealthCheckReportSecurityRowDTO(
      final ComponentIdentifier identifier,
      final String appId,
      final String scanId,
      final String refId)
  {
    ThirdPartyReportComponentDTO dto = findComponent(appId, identifier, scanId);

    if (dto != null) {
      return dto.securityRows.stream()
          .filter(row -> row.reference.equals(refId))
          .findFirst()
          .orElseThrow(() -> new NotFoundException("Vulnerability with refid: " + refId + " not found."));
    }

    throw new NotFoundException("Vulnerability with refid: " + refId + " not found.");
  }

  /**
   * @deprecated Replaced in 1.86 with {@link #getVulnerabilityData}, which is consumed by the API resource
   *             ApiVulnerabilityDetailsResourceV2. This code path must remain until the legacy ("old style")
   *             application report is removed.
   */
  @Deprecated
  public SecurityVulnerabilityDetails getSecurityVulnerabilityDetailsByIdentifier(
      final ComponentIdentifier identifier,
      final String appId,
      final String scanId,
      final String refId)
  {
    ThirdPartyHealthCheckReportSecurityRowDTO dto =
        findThirdPartyHealthCheckReportSecurityRowDTO(identifier, appId, scanId, refId);
    return new SecurityVulnerabilityDetails(dto.source, refId, ThirdPartySecurityVulnerabilityRenderer.renderHtml(dto));
  }

  public SecurityVulnerabilityData getVulnerabilityData(
      final ComponentIdentifier identifier,
      final String appId,
      final String scanId,
      final String refId)
  {
    ThirdPartyHealthCheckReportSecurityRowDTO dto =
        findThirdPartyHealthCheckReportSecurityRowDTO(identifier, appId, scanId, refId);
    return ThirdPartyVulnerabilityDataAdapter.map(dto);
  }

  private ThirdPartyReportComponentDTO findComponent(
      final String appId,
      final ComponentIdentifier identifier,
      final String scanId)
  {
    Table<String, ComponentIdentifier, ThirdPartyReportComponentDTO> scannedComponents =
        componentCache.get().getIfPresent(scanId);

    if (scannedComponents == null) {
      final Map<String, ThirdPartyReportComponentDTO> data =
          getData(reportServiceProvider.get().getReport(appId, scanId));
      if (data == null || data.isEmpty()) {
        return null;
      }

      scannedComponents = HashBasedTable.create();
      for (Entry<String, ThirdPartyReportComponentDTO> thirdPartyDataEntry : data.entrySet()) {
        scannedComponents.put(thirdPartyDataEntry.getKey(), thirdPartyDataEntry.getValue().componentIdentifier,
            thirdPartyDataEntry.getValue());
      }
      componentCache.get().put(scanId, scannedComponents);
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
    componentDetails.setAnalyzerFeatures(MetadataRecorderUtils.fromThirdParty(null));
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

  public void updateReport(
      ContainerNode<?> bomJsonData,
      ContainerNode<?> licensesJsonData,
      ContainerNode<?> securityJsonData,
      ContainerNode<?> dataJson,
      ContainerNode<?> summaryJsonData,
      ApplicationReport applicationReport)
  {
    long startTimeReport = currentTimeMillis();
    log.debug("Begin updating report for third party");
    int knownArtifactCount = summaryJsonData.path("knownArtifactCount").asInt();
    int exactlyMatchedComponentCount = dataJson.path("exactlyMatchedComponentCount").asInt();
    boolean isExploitRiskDataLookupEnabled = SystemConfigurationPropertyFeature.THIRD_PARTY_KEV_LOOKUP.isEnabled();

    Map<String, ThirdPartyReportComponentDTO> thirdPartyReportComponentDataByHash = null;
    ArrayNode bomArray = (ArrayNode) bomJsonData.get("aaData");
    Set<String> hashesToUpdate = new HashSet<>();
    Map<String, List<JsonNode>> unmatchedRefToNodeMap = new HashMap<>();

    for (int i = 0; i < bomArray.size(); i++) {
      JsonNode bomNode = bomArray.get(i);
      String matchStateString = bomNode.get(MATCH_STATE).asText();
      MatchState matchState = MatchState.getById(matchStateString);
      if (MatchState.UNKNOWN.equals(matchState)) {
        thirdPartyReportComponentDataByHash =
            getThirdPartyReportComponentDataByHash(applicationReport, thirdPartyReportComponentDataByHash);
        String hash = JsonUtils.getNullableString(bomNode.get("hash"));
        ThirdPartyReportComponentDTO thirdPartyReportComponentDTO = thirdPartyReportComponentDataByHash.get(hash);
        if (thirdPartyReportComponentDTO != null) {
          JsonNode tpNode = MAPPER.valueToTree(thirdPartyReportComponentDTO.bomRow);
          mergeNodes(bomNode, tpNode);
          bomArray.set(i, tpNode);
          knownArtifactCount++;
          exactlyMatchedComponentCount++;

          // Security vulnerabilities
          if (!thirdPartyReportComponentDTO.securityRows.isEmpty()) {
            ArrayNode securityJsonArray = (ArrayNode) securityJsonData.get("aaData");
            for (ThirdPartyHealthCheckReportSecurityRowDTO securityRowDTO : thirdPartyReportComponentDTO.securityRows) {
              JsonNode nodeToAdd = JsonUtils.asTree(convert(securityRowDTO));
              securityJsonArray.add(nodeToAdd);
              String ref = securityRowDTO.reference;
              if (isExploitRiskDataLookupEnabled) {
                hashesToUpdate.add(securityRowDTO.hash);
                unmatchedRefToNodeMap.computeIfAbsent(ref, k -> new ArrayList<>()).add(nodeToAdd);
              }
            }
          }

          // Licenses
          if (!thirdPartyReportComponentDTO.licensesRow.declaredLicenses.isEmpty()) {
            ArrayNode licenseJsonArray = (ArrayNode) licensesJsonData.get("aaData");
            HealthCheckReportRowDTO licenseDTO =
                new HealthCheckReportRowDTO(thirdPartyReportComponentDTO.componentIdentifier, hash);
            licenseDTO.declaredLicenses = thirdPartyReportComponentDTO.licensesRow.declaredLicenses.stream()
                .map(license -> license.name)
                .collect(Collectors.toSet());
            licenseJsonArray.add(JsonUtils.asTree(licenseDTO));
          }
        }
      }
    }

    thirdPartyReportComponentDataByHash =
        getThirdPartyReportComponentDataByHash(applicationReport, thirdPartyReportComponentDataByHash);
    addVexToSecurityData(securityJsonData, thirdPartyReportComponentDataByHash);

    long startTimeAddKev = currentTimeMillis();
    log.debug("Begin updating security.json for KEV.");
    if (isExploitRiskDataLookupEnabled) {
      addExploitRiskDataToSecurityData(hashesToUpdate, unmatchedRefToNodeMap);
    }
    log.debug("Finished updating security.json for KEV and EPSS in: {} ms", (currentTimeMillis() - startTimeAddKev));

    ObjectNode dataObjectNode = (ObjectNode) dataJson;
    dataObjectNode.put("exactlyMatchedComponentCount", exactlyMatchedComponentCount);
    dataObjectNode.put("knownArtifactCount", knownArtifactCount);

    ((ObjectNode) summaryJsonData).put("knownArtifactCount", knownArtifactCount);
    log.debug("Finished updating report for third party in: {} ms", (currentTimeMillis() - startTimeReport));
  }

  private void addExploitRiskDataToSecurityData(
      Set<String> hashesToUpdate,
      Map<String, List<JsonNode>> unmatchedRefToNodeMap)
  {
    if (unmatchedRefToNodeMap.isEmpty()) {
      return;
    }

    Set<String> refIdsForExploitRiskDataLookup = new HashSet<>(unmatchedRefToNodeMap.keySet());

    Map<String, VulnerabilityExploitRiskData> exploitRiskDataMap =
        processRefIdsInBatches(refIdsForExploitRiskDataLookup);

    for (Map.Entry<String, List<JsonNode>> entry : unmatchedRefToNodeMap.entrySet()) {
      String refId = entry.getKey();
      List<JsonNode> nodes = entry.getValue();
      VulnerabilityExploitRiskData exploitRiskData = exploitRiskDataMap.get(refId);

      for (JsonNode node : nodes) {
        enrichNodeWithExploitRiskDataIfNeeded(node, exploitRiskData, hashesToUpdate);
      }
    }
  }

  private void enrichNodeWithExploitRiskDataIfNeeded(
      JsonNode nodeToUpdate,
      VulnerabilityExploitRiskData exploitRiskData,
      Set<String> hashesToUpdate)
  {
    if (!hashesToUpdate.contains(nodeToUpdate.get("hash").textValue())) {
      return;
    }

    if (exploitRiskData == null) {
      return;
    }

    if (exploitRiskData.kevData() != null) {
      ((ObjectNode) nodeToUpdate).set("kevData", JsonUtils.asTree(exploitRiskData.kevData()));
    }

    if (exploitRiskData.epssData() != null) {
      ((ObjectNode) nodeToUpdate).set("epssData", JsonUtils.asTree(exploitRiskData.epssData()));
    }
  }

  private Map<String, ThirdPartyReportComponentDTO> getThirdPartyReportComponentDataByHash(
      ApplicationReport applicationReport,
      Map<String, ThirdPartyReportComponentDTO> thirdPartyReportComponentDataByHash)
  {
    if (thirdPartyReportComponentDataByHash == null) {
      thirdPartyReportComponentDataByHash = getData(applicationReport);
    }
    return thirdPartyReportComponentDataByHash;
  }

  private void addVexToSecurityData(
      ContainerNode<?> securityJsonData,
      Map<String, ThirdPartyReportComponentDTO> thirdPartyReportComponentDataByHash)
  {
    if (thirdPartyReportComponentDataByHash != null) {
      // data to update with VEX
      Map<String, List<JsonNode>> vulnComponentListMap = new HashMap<>();

      ArrayNode aaData = (ArrayNode) securityJsonData.get("aaData");
      for (JsonNode componentNodes : aaData) {
        String reference = componentNodes.get("reference").textValue();
        if (vulnComponentListMap.get(reference) == null) {
          vulnComponentListMap.put(reference, new ArrayList<>());
        }
        vulnComponentListMap.get(reference).add(componentNodes);
      }

      for (ThirdPartyReportComponentDTO currentComponent : thirdPartyReportComponentDataByHash.values()) {
        List<ThirdPartyHealthCheckReportSecurityRowDTO> securityRows = currentComponent.securityRows;
        for (ThirdPartyHealthCheckReportSecurityRowDTO securityRow : securityRows) {
          ThirdPartyVulnerabilityExploitabilityExchangeRowDTO analysis = securityRow.analysis;
          if (analysis != null) {
            String reference = securityRow.reference;
            List<JsonNode> vulnNodeList = vulnComponentListMap.get(reference);
            if (vulnNodeList != null) {
              for (JsonNode vulnNode : vulnNodeList) {
                ((ObjectNode) vulnNode).putIfAbsent("analysis",
                    JsonUtils.asTree(JsonUtils.asTree(securityRow.analysis)));
              }
            }
          }
        }
      }
    }
  }

  private void mergeNodes(final JsonNode bomNode, final JsonNode tpNode) {
    final ObjectNode tpObjectNode = (ObjectNode) tpNode;
    tpObjectNode.replace("filenames", bomNode.get("filenames"));
    tpObjectNode.replace("pathnames", bomNode.get("pathnames"));
    tpObjectNode.replace("analyzerFeatures", bomNode.get("analyzerFeatures"));

    bomNode.properties().forEach(entry -> {
      if (!tpObjectNode.has(entry.getKey())) {
        tpObjectNode.set(entry.getKey(), entry.getValue());
      }
    });
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
            securityRows.stream()
                .filter(row -> row.componentIdentifier.equals(dto.componentIdentifier))
                .collect(Collectors.toList()));
      }
      if (licenseRows != null && !licenseRows.isEmpty()) {
        licenseRows.stream()
            .filter(row -> row.componentIdentifier.equals(dto.componentIdentifier))
            .findFirst()
            .ifPresent(license -> dto.licensesRow = license);
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
      final ThirdPartyHealthCheckReportSecurityRowDTO secRow)
  {
    SecurityVulnerability securityVulnerability =
        new SecurityVulnerability(secRow.reference, secRow.source, secRow.score, secRow.description);
    securityVulnerability.setUrl(secRow.url);
    securityVulnerability.setIdentificationSource(secRow.identificationSource);
    securityVulnerability.setResearchType(secRow.researchType);
    securityVulnerability.setDetectionType(secRow.detectionType);
    return securityVulnerability;
  }

  private HealthCheckReportSecurityRowDTO convert(ThirdPartyHealthCheckReportSecurityRowDTO securityRow) {
    HealthCheckReportSecurityRowDTO result =
        new HealthCheckReportSecurityRowDTO(securityRow.componentIdentifier, securityRow.hash);
    result.source = securityRow.source;
    result.reference = securityRow.reference;
    result.score = securityRow.score;
    result.url = securityRow.url;
    result.summary = securityRow.description;
    result.analysis = securityRow.analysis;
    result.cvssVectorString = securityRow.cvssVectorString;
    result.cvssVectorSource = securityRow.cvssVectorSource;
    result.identificationSource = securityRow.identificationSource;
    result.researchType = securityRow.researchType;
    result.detectionType = securityRow.detectionType;
    return result;
  }

  public ApiComponentRemediationValueDTO getSuggestedRemmediation(
      String appId,
      ComponentIdentifier componentIdentifier,
      String scanId)
  {
    ApiComponentRemediationValueDTO componentRemediationDto = new ApiComponentRemediationValueDTO();
    ThirdPartyReportComponentDTO component = findComponent(appId, componentIdentifier, scanId);

    if (component != null) {
      boolean emptyVersions =
          component.securityRows.stream().anyMatch(securityRow -> StringUtils.isBlank(securityRow.fixedVersion));

      if (!emptyVersions) {
        Set<ComparableVersion> fixedVersions = component.securityRows.stream()
            .map(securityRow -> new ComparableVersion(securityRow.fixedVersion))
            .collect(Collectors.toSet());

        Optional<ComparableVersion> fixedVersion = fixedVersions.stream().max(comparator::compare);

        if (fixedVersion.isPresent()) {
          ComponentIdentifier suggestedComponent =
              componentIdentifier.createAlternativeVersion(fixedVersion.get().toString());

          ApiComponentDTOV2 componentDTOV2 = new ApiComponentDTOV2();
          componentDTOV2.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(suggestedComponent);
          componentDTOV2.packageUrl = PackageUrlIdentifier.toPackageUrl(suggestedComponent);
          ComponentDisplayName componentDisplayName =
              ComponentDisplayNameUtil.fromIdentifier(suggestedComponent);
          componentDTOV2.displayName = componentDisplayName != null ? componentDisplayName.toString() : null;
          componentDTOV2.proprietary = null; // not applicable
          componentDTOV2.thirdParty = true;
          ApiVersionChangeOptionDTO changeOptionType = new ApiVersionChangeOptionDTO(
              ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, new ApiComponentChangeActionDTO(componentDTOV2));
          componentRemediationDto.versionChanges.add(changeOptionType);
          return componentRemediationDto;
        }
      }
    }
    return componentRemediationDto;
  }

  private Map<String, VulnerabilityExploitRiskData> getExploitRiskDataFromHDS(Set<String> refIds) {
    try {
      long startTime = currentTimeMillis();
      log.debug("Begin HDS call for: {} refIds", refIds.size());

      BulkSecurityVulnerabilityDataDTO dto =
          client.post(BulkSecurityVulnerabilityDataDTO.class, HDS_BULK_VULN_DATA_PATH, refIds);

      log.debug("HDS call finished for {} refIds. Elapsed: {} ms",
          refIds.size(), (currentTimeMillis() - startTime));

      if (dto == null || dto.getVulnerabilities() == null) {
        return Collections.emptyMap();
      }

      return dto.getVulnerabilities()
          .entrySet()
          .stream()
          .filter(entry -> entry.getValue() != null &&
              (entry.getValue().kevData != null || entry.getValue().epssData != null))
          .collect(Collectors.toMap(
              Map.Entry::getKey,
              entry -> new VulnerabilityExploitRiskData(entry.getValue().kevData, entry.getValue().epssData)));
    }
    catch (Exception e) {
      log.error("Failed to retrieve KEV and EPSS data from HDS: {}", e.getMessage(), e);

      return Collections.emptyMap();
    }
  }

  private Map<String, VulnerabilityExploitRiskData> processRefIdsInBatches(Set<String> allRefIds) {
    final int BATCH_SIZE = 1000;
    Map<String, VulnerabilityExploitRiskData> combinedResults = new HashMap<>();

    long startTime = currentTimeMillis();
    log.debug("Begin HDS batch call.");

    Iterables.partition(allRefIds, BATCH_SIZE)
        .forEach(refIdBatch -> combinedResults.putAll(getExploitRiskDataFromHDS(new HashSet<>(refIdBatch))));

    log.debug("Finished all HDS batch calls in {} ms.", (currentTimeMillis() - startTime));

    return combinedResults;
  }
}
