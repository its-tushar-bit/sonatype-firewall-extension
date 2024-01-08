/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
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
import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.HealthCheckReportRowDTO;
import com.sonatype.insight.scan.HealthCheckReportSecurityRowDTO;
import com.sonatype.insight.scan.ThirdPartyHealthCheckReportSecurityRowDTO;
import com.sonatype.insight.scan.ThirdPartyVulnerabilityExploitabilityExchangeRowDTO;
import com.sonatype.insight.util.MetadataRecorderUtils;
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
import com.google.common.collect.Table;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.report.DependencyResolver.MATCH_STATE;

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

  public static final ObjectMapper MAPPER = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private final InsightWork work;

  // For testing visibility
  final TenantReference<Cache<String, Table<String, ComponentIdentifier, ThirdPartyReportComponentDTO>>>
      componentCache;

  private static final Comparator<ComparableVersion> comparator = ComparableVersion::compareTo;

  @Inject
  public ThirdPartyComponentDAO(final InsightWork work) {
    this.work = work;
    componentCache = new TenantReference<>(() -> CacheBuilder.newBuilder()
        .expireAfterAccess(1, TimeUnit.DAYS)
        .maximumWeight(100000)
        .weigher((Weigher<String, Table<String, ComponentIdentifier, ThirdPartyReportComponentDTO>>) (key, value) ->
            value.size())
        .build());
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
      final Map<String, ThirdPartyReportComponentDTO> data = getData(work.getReportFile(appId, scanId));
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
      File reportFile)
  {
    int knownArtifactCount = summaryJsonData.path("knownArtifactCount").asInt();
    int exactlyMatchedComponentCount = dataJson.path("exactlyMatchedComponentCount").asInt();

    Map<String, ThirdPartyReportComponentDTO> thirdPartyReportComponentDataByHash = null;
    ArrayNode bomArray = (ArrayNode) bomJsonData.get("aaData");
    for (int i = 0; i < bomArray.size(); i++) {
      JsonNode bomNode = bomArray.get(i);
      String matchStateString = bomNode.get(MATCH_STATE).asText();
      MatchState matchState = MatchState.getById(matchStateString);
      if (MatchState.UNKNOWN.equals(matchState)) {
        thirdPartyReportComponentDataByHash =
            getThirdPartyReportComponentDataByHash(reportFile, thirdPartyReportComponentDataByHash);
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
              securityJsonArray.add(JsonUtils.asTree(convert(securityRowDTO)));
            }
          }
          
          // Licenses
          if (!thirdPartyReportComponentDTO.licensesRow.declaredLicenses.isEmpty()) {
            ArrayNode licenseJsonArray = (ArrayNode) licensesJsonData.get("aaData");
            HealthCheckReportRowDTO licenseDTO =
                new HealthCheckReportRowDTO(thirdPartyReportComponentDTO.componentIdentifier, hash);
            licenseDTO.declaredLicenses = thirdPartyReportComponentDTO.licensesRow.declaredLicenses.stream()
                .map(license -> license.name).collect(Collectors.toSet());
            licenseJsonArray.add(JsonUtils.asTree(licenseDTO));
          }
        }
      }
    }

    thirdPartyReportComponentDataByHash =
        getThirdPartyReportComponentDataByHash(reportFile, thirdPartyReportComponentDataByHash);
    addVexToSecurityData(securityJsonData, thirdPartyReportComponentDataByHash);

    ObjectNode dataObjectNode = (ObjectNode) dataJson;
    dataObjectNode.put("exactlyMatchedComponentCount", exactlyMatchedComponentCount);
    dataObjectNode.put("knownArtifactCount", knownArtifactCount);

    ((ObjectNode) summaryJsonData).put("knownArtifactCount", knownArtifactCount);
  }

  private Map<String, ThirdPartyReportComponentDTO> getThirdPartyReportComponentDataByHash(
      File reportFile,
      Map<String, ThirdPartyReportComponentDTO> thirdPartyReportComponentDataByHash)
  {
    if (thirdPartyReportComponentDataByHash == null) {
      thirdPartyReportComponentDataByHash = getData(reportFile);
    }
    return thirdPartyReportComponentDataByHash;
  }

  private void addVexToSecurityData(
      ContainerNode<?> securityJsonData,
      Map<String, ThirdPartyReportComponentDTO> thirdPartyReportComponentDataByHash)
  {
    if (thirdPartyReportComponentDataByHash != null) {
      //data to update with VEX
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

    bomNode.fields().forEachRemaining(entry -> {
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
            .map(securityRow -> new ComparableVersion(securityRow.fixedVersion)).collect(Collectors.toSet());

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
}
