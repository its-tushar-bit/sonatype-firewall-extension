/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.spdx;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.SbomIdentityUtils;
import com.sonatype.insight.brain.model.thirdpartyscans.ResolvedLicenseDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.sbom.utils.SbomCommonUtils;
import com.sonatype.insight.purl.InvalidPackageURLException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.file.SbomProcessingException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Component.Type;
import org.cyclonedx.model.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.ModelCopyManager;
import org.spdx.library.SpdxModelFactory;
import org.spdx.library.model.v3_0_1.core.Agent;
import org.spdx.library.model.v3_0_1.core.CreationInfo;
import org.spdx.library.model.v3_0_1.core.Element;
import org.spdx.library.model.v3_0_1.core.ExternalIdentifier;
import org.spdx.library.model.v3_0_1.core.ExternalIdentifierType;
import org.spdx.library.model.v3_0_1.core.ExternalRef;
import org.spdx.library.model.v3_0_1.core.ExternalRefType;
import org.spdx.library.model.v3_0_1.core.Relationship;
import org.spdx.library.model.v3_0_1.core.RelationshipType;
import org.spdx.library.model.v3_0_1.core.SpdxDocument;
import org.spdx.library.model.v3_0_1.security.VexAffectedVulnAssessmentRelationship;
import org.spdx.library.model.v3_0_1.security.VexFixedVulnAssessmentRelationship;
import org.spdx.library.model.v3_0_1.security.VexJustificationType;
import org.spdx.library.model.v3_0_1.security.VexNotAffectedVulnAssessmentRelationship;
import org.spdx.library.model.v3_0_1.security.VexUnderInvestigationVulnAssessmentRelationship;
import org.spdx.library.model.v3_0_1.security.Vulnerability;
import org.spdx.library.model.v3_0_1.ai.AIPackage;
import org.spdx.library.model.v3_0_1.dataset.DatasetPackage;
import org.spdx.library.model.v3_0_1.simplelicensing.LicenseExpression;
import org.spdx.library.model.v3_0_1.software.SpdxPackage;
import org.spdx.storage.simple.InMemSpdxStore;
import org.spdx.v3jsonldstore.JsonLDStore;

import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityDetectionType.OTHER;

@Named
@Singleton
public class Spdx3VersionHandler
{
  private static final Logger log = LoggerFactory.getLogger(Spdx3VersionHandler.class);

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static final String SPEC_VERSION = "3.0";

  private static final String UNKNOWN = "unknown";

  public ParsedSpdxResult parse(String content, SbomFormat format) throws SbomProcessingException {
    try {
      SpdxDocument document = deserialize(content);

      List<Pair<ComponentIdentifier, Component>> resolvedComponents = new ArrayList<>();
      List<ThirdPartyCoordinateSecurity> vulnerabilities = new ArrayList<>();
      List<Dependency> dependencies = new ArrayList<>();
      List<ThirdPartyVulnerabilityExploitabilityExchange> vexAnnotations = new ArrayList<>();
      Map<String, Set<String>> vulnToPackageUris = new HashMap<>();
      List<Set<String>> vexAffectedPackageUris = new ArrayList<>();

      List<?> allObjects = SpdxModelFactory.getSpdxObjects(
          document.getModelStore(),
          document.getCopyManager(),
          null,
          null,
          document.getSpecVersion()).toList();

      processAllObjects(allObjects, resolvedComponents, vulnerabilities, dependencies, vexAnnotations,
          vulnToPackageUris, vexAffectedPackageUris);
      String unsupportedProfiles = doExtractUnsupportedProfiles(content);
      String rootComponentRef = extractRootComponentRef(document, resolvedComponents);

      return new ParsedSpdxResult(
          resolvedComponents,
          dependencies,
          vulnerabilities,
          vexAnnotations,
          SPEC_VERSION,
          unsupportedProfiles,
          vulnToPackageUris,
          vexAffectedPackageUris,
          rootComponentRef);
    }
    catch (InvalidSPDXAnalysisException | IOException e) {
      throw new SbomProcessingException("Failed to parse SPDX 3.0 document", e);
    }
  }

  public List<ThirdPartyVulnerabilityExploitabilityExchange> parseVex(
      String content,
      SbomFormat format) throws SbomProcessingException
  {
    try {
      SpdxDocument document = deserialize(content);
      return doParseVex(document);
    }
    catch (InvalidSPDXAnalysisException | IOException e) {
      throw new SbomProcessingException("Failed to parse SPDX 3.0 VEX", e);
    }
  }

  public byte[] generate(SpdxGenerationContext context) throws InvalidSPDXAnalysisException {
    try {
      SpdxModelFactory.init();
      InMemSpdxStore baseStore = new InMemSpdxStore();
      ModelCopyManager copyManager = new ModelCopyManager();
      JsonLDStore store = new JsonLDStore(baseStore);
      store.setPretty(true);

      String appName = context.applicationName() != null ? context.applicationName() : UNKNOWN;
      String sbomVer = context.sbomVersion() != null ? context.sbomVersion() : UNKNOWN;
      if (StringUtils.isBlank(context.documentUri())) {
        throw new IllegalArgumentException("documentUri is required for SPDX 3.0 document generation");
      }
      String documentUri = context.documentUri();

      CreationInfo creationInfo =
          new CreationInfo.CreationInfoBuilder(
              store, documentUri + "/creationInfo", copyManager)
                  .setSpecVersion("3.0.1")
                  .setCreated(ZonedDateTime.now(ZoneOffset.UTC)
                      .truncatedTo(ChronoUnit.SECONDS)
                      .format(DateTimeFormatter.ISO_INSTANT))
                  .build();

      Agent toolAgent =
          new Agent.AgentBuilder(
              store, documentUri + "/agent/sonatype-sbom-manager", copyManager)
                  .setCreationInfo(creationInfo)
                  .setName("Sonatype SBOM Manager")
                  .build();
      creationInfo.getCreatedBys().add(toolAgent);

      SpdxDocument document = new SpdxDocument.SpdxDocumentBuilder(
          store, documentUri, copyManager)
              .setCreationInfo(creationInfo)
              .setName(appName)
              .build();

      Map<String, SpdxPackage> packagesByCoordinateId = new HashMap<>();
      SpdxPackage rootPackage = null;
      SpdxPackage firstPackage = null;
      for (ThirdPartyFileCoordinate comp : context.components()) {
        String pkgUri = documentUri + "/package/" + comp.getId();
        String pkgName = comp.getName() != null ? comp.getName() : "NOASSERTION";
        SpdxPackage.SpdxPackageBuilder pkgBuilder = new SpdxPackage.SpdxPackageBuilder(
            store, pkgUri, copyManager)
                .setCreationInfo(creationInfo)
                .setName(pkgName);

        if (comp.getVersion() != null) {
          pkgBuilder.setPackageVersion(comp.getVersion());
        }
        if (comp.getPackageUrl() != null) {
          pkgBuilder.setPackageUrl(comp.getPackageUrl());
        }

        SpdxPackage pkg = pkgBuilder.build();
        packagesByCoordinateId.put(comp.getId(), pkg);
        document.getElements().add(pkg);
        if (firstPackage == null) {
          firstPackage = pkg;
        }
        if (context.rootComponentRef() != null
            && context.rootComponentRef().equals(comp.getComponentRef()))
        {
          rootPackage = pkg;
        }
      }

      SpdxPackage root = rootPackage != null ? rootPackage : firstPackage;
      if (root != null) {
        document.getRootElements().add(root);
      }
      else {
        document.getRootElements().add(document);
      }

      generateVulnerabilitiesAndVex(store, copyManager, document, creationInfo, documentUri, context,
          packagesByCoordinateId);
      generateLicenseRelationships(store, copyManager, document, creationInfo, documentUri, context,
          packagesByCoordinateId);

      if (StringUtils.isNotBlank(context.companionCdxFilename())) {
        ExternalRef cdxRef = new ExternalRef.ExternalRefBuilder(
            store, documentUri + "/externalRef/cyclonedx", copyManager)
                .addLocator("file://" + context.companionCdxFilename())
                .setExternalRefType(ExternalRefType.COMPONENT_ANALYSIS_REPORT)
                .setContentType("application/vnd.cyclonedx+json")
                .setComment("Companion CycloneDX BOM included in this archive")
                .build();
        document.getExternalRefs().add(cdxRef);
      }

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      store.serialize(baos, document);

      if (context.extendedProfileElements() != null) {
        return mergeExtendedProfileElements(baos.toByteArray(), context.extendedProfileElements());
      }
      return baos.toByteArray();
    }
    catch (IOException e) {
      throw new InvalidSPDXAnalysisException("Failed to serialize SPDX 3.0 JSON-LD", e);
    }
  }

  private byte[] mergeExtendedProfileElements(byte[] serializedJsonLd, String extendedProfileElements) {
    try {
      JsonNode blob = OBJECT_MAPPER.readTree(extendedProfileElements);
      JsonNode elements = blob.get("elements");
      if (elements == null || !elements.isArray() || elements.isEmpty()) {
        return serializedJsonLd;
      }

      JsonNode rootNode = OBJECT_MAPPER.readTree(serializedJsonLd);
      if (!(rootNode instanceof ObjectNode root)) {
        return serializedJsonLd;
      }
      JsonNode graph = root.get("@graph");
      if (graph == null || !graph.isArray()) {
        return serializedJsonLd;
      }

      ArrayNode graphArray = (ArrayNode) graph;

      Set<String> existingIds = new HashSet<>();
      for (JsonNode existing : graphArray) {
        if (existing.has("@id")) {
          existingIds.add(existing.get("@id").asText());
        }
      }

      Map<String, String> renamedBlankNodes = new HashMap<>();
      for (JsonNode element : elements) {
        String id = element.has("@id") ? element.get("@id").asText() : null;
        if (id != null && id.startsWith("_:") && existingIds.contains(id)) {
          renamedBlankNodes.put(id, "_:ext" + id.substring(2));
        }
      }

      for (JsonNode element : elements) {
        String id = element.has("@id") ? element.get("@id").asText() : null;
        if (id != null && existingIds.contains(id) && !renamedBlankNodes.containsKey(id)) {
          continue;
        }
        if (renamedBlankNodes.isEmpty()) {
          graphArray.add(element);
        }
        else {
          graphArray.add(remapBlankNodeReferences(element, renamedBlankNodes));
        }
      }

      return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(root);
    }
    catch (IOException e) {
      log.warn("Failed to merge extended profile elements into SPDX 3.0 export, returning without them", e);
      return serializedJsonLd;
    }
  }

  private JsonNode remapBlankNodeReferences(JsonNode node, Map<String, String> renamedBlankNodes) {
    if (node.isTextual()) {
      String mapped = renamedBlankNodes.get(node.asText());
      return mapped != null ? OBJECT_MAPPER.getNodeFactory().textNode(mapped) : node;
    }
    else if (node.isObject()) {
      ObjectNode result = OBJECT_MAPPER.createObjectNode();
      node.fields().forEachRemaining(entry -> {
        JsonNode value = entry.getValue();
        if ("@id".equals(entry.getKey()) && value.isTextual()) {
          String mapped = renamedBlankNodes.get(value.asText());
          result.set(entry.getKey(), mapped != null
              ? OBJECT_MAPPER.getNodeFactory().textNode(mapped)
              : value);
        }
        else {
          result.set(entry.getKey(), remapBlankNodeReferences(value, renamedBlankNodes));
        }
      });
      return result;
    }
    else if (node.isArray()) {
      ArrayNode result = OBJECT_MAPPER.createArrayNode();
      for (JsonNode child : node) {
        result.add(remapBlankNodeReferences(child, renamedBlankNodes));
      }
      return result;
    }
    return node;
  }

  private void generateVulnerabilitiesAndVex(
      JsonLDStore store,
      ModelCopyManager copyManager,
      SpdxDocument document,
      CreationInfo creationInfo,
      String documentUri,
      SpdxGenerationContext context,
      Map<String, SpdxPackage> packagesByCoordinateId) throws InvalidSPDXAnalysisException
  {
    if (context.vulnerabilities() == null || context.vulnerabilities().isEmpty()) {
      return;
    }

    Map<String, Vulnerability> vulnsByRefId = new HashMap<>();

    for (ThirdPartyCoordinateSecurity security : context.vulnerabilities()) {
      String refId = security.getRefId();
      if (StringUtils.isBlank(refId)) {
        continue;
      }

      if (!vulnsByRefId.containsKey(refId)) {
        String vulnUri = documentUri + "/vulnerability/" +
            URLEncoder.encode(refId, StandardCharsets.UTF_8);
        Vulnerability.VulnerabilityBuilder vulnBuilder = new Vulnerability.VulnerabilityBuilder(
            store, vulnUri, copyManager)
                .setCreationInfo(creationInfo)
                .setName(refId);

        if (StringUtils.isNotBlank(security.getDescription())) {
          vulnBuilder.setDescription(security.getDescription());
        }

        Vulnerability vuln = vulnBuilder.build();

        ExternalIdentifierType extIdType = resolveExternalIdentifierType(refId);
        String extIdUri = vulnUri + "/externalId/" + extIdType.name().toLowerCase();
        ExternalIdentifier extId = new ExternalIdentifier.ExternalIdentifierBuilder(
            store, extIdUri, copyManager)
                .setExternalIdentifierType(extIdType)
                .setIdentifier(refId)
                .build();
        vuln.getExternalIdentifiers().add(extId);

        vulnsByRefId.put(refId, vuln);
        document.getElements().add(vuln);
      }
    }

    Map<String, ThirdPartyVulnerabilityExploitabilityExchange> vexBySecurityId = new HashMap<>();
    if (context.vexAnnotations() != null) {
      for (ThirdPartyVulnerabilityExploitabilityExchange vex : context.vexAnnotations()) {
        if (StringUtils.isNotBlank(vex.getCoordinateSecurityId()) && StringUtils.isNotBlank(vex.getState())) {
          vexBySecurityId.put(vex.getCoordinateSecurityId(), vex);
        }
      }
    }

    int vexCounter = 0;
    for (ThirdPartyCoordinateSecurity security : context.vulnerabilities()) {
      String refId = security.getRefId();
      if (StringUtils.isBlank(refId)) {
        continue;
      }

      Vulnerability vuln = vulnsByRefId.get(refId);
      SpdxPackage pkg = packagesByCoordinateId.get(security.getFileCoordinateId());
      if (vuln == null || pkg == null) {
        continue;
      }

      ThirdPartyVulnerabilityExploitabilityExchange vexData = vexBySecurityId.get(security.getId());
      String state = vexData != null ? vexData.getState() : "affected";
      String vexUri = documentUri + "/vex/" + (++vexCounter);

      Element vexElement = createVexRelationship(store, copyManager, creationInfo, vexUri, vuln, pkg, state, vexData);
      if (vexElement != null) {
        document.getElements().add(vexElement);
      }
    }
  }

  private Element createVexRelationship(
      JsonLDStore store,
      ModelCopyManager copyManager,
      CreationInfo creationInfo,
      String vexUri,
      Vulnerability vuln,
      SpdxPackage pkg,
      String state,
      ThirdPartyVulnerabilityExploitabilityExchange vexData) throws InvalidSPDXAnalysisException
  {
    String detail = vexData != null ? vexData.getDetail() : null;

    return switch (state) {
      case "not_affected", "false_positive" -> {
        VexNotAffectedVulnAssessmentRelationship notAffected =
            new VexNotAffectedVulnAssessmentRelationship.VexNotAffectedVulnAssessmentRelationshipBuilder(
                store, vexUri, copyManager)
                    .setCreationInfo(creationInfo)
                    .setFrom(vuln)
                    .addTo(pkg)
                    .build();
        if (vexData != null && StringUtils.isNotBlank(vexData.getJustification())) {
          VexJustificationType jt = mapDbJustificationToSpdx3(vexData.getJustification());
          if (jt != null) {
            notAffected.setJustificationType(jt);
          }
        }
        if (StringUtils.isNotBlank(detail)) {
          notAffected.setImpactStatement(detail);
        }
        yield notAffected;
      }
      case "in_triage", "under_investigation" -> {
        VexUnderInvestigationVulnAssessmentRelationship underInvestigation =
            new VexUnderInvestigationVulnAssessmentRelationship.VexUnderInvestigationVulnAssessmentRelationshipBuilder(
                store, vexUri, copyManager)
                    .setCreationInfo(creationInfo)
                    .setFrom(vuln)
                    .addTo(pkg)
                    .build();
        if (StringUtils.isNotBlank(detail)) {
          underInvestigation.setStatusNotes(detail);
        }
        yield underInvestigation;
      }
      case "resolved", "resolved_with_pedigree", "fixed" -> {
        VexFixedVulnAssessmentRelationship fixed =
            new VexFixedVulnAssessmentRelationship.VexFixedVulnAssessmentRelationshipBuilder(
                store, vexUri, copyManager)
                    .setCreationInfo(creationInfo)
                    .setFrom(vuln)
                    .addTo(pkg)
                    .build();
        if (StringUtils.isNotBlank(detail)) {
          fixed.setStatusNotes(detail);
        }
        yield fixed;
      }
      default -> {
        VexAffectedVulnAssessmentRelationship affected =
            new VexAffectedVulnAssessmentRelationship.VexAffectedVulnAssessmentRelationshipBuilder(
                store, vexUri, copyManager)
                    .setCreationInfo(creationInfo)
                    .setFrom(vuln)
                    .addTo(pkg)
                    .build();
        if (vexData != null && StringUtils.isNotBlank(vexData.getResponse())) {
          affected.setActionStatement(mapDbResponseToActionStatement(vexData.getResponse()));
        }
        if (StringUtils.isNotBlank(detail)) {
          affected.setDescription(detail);
        }
        yield affected;
      }
    };
  }

  private VexJustificationType mapDbJustificationToSpdx3(String justification) {
    return switch (justification) {
      case "code_not_present" -> VexJustificationType.VULNERABLE_CODE_NOT_PRESENT;
      case "code_not_reachable" -> VexJustificationType.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH;
      case "protected_at_runtime", "protected_at_perimeter" -> VexJustificationType.VULNERABLE_CODE_CANNOT_BE_CONTROLLED_BY_ADVERSARY;
      case "protected_by_compiler", "protected_by_mitigating_control" -> VexJustificationType.INLINE_MITIGATIONS_ALREADY_EXIST;
      case "requires_configuration", "requires_dependency", "requires_environment" -> VexJustificationType.VULNERABLE_CODE_CANNOT_BE_CONTROLLED_BY_ADVERSARY;
      default -> null;
    };
  }

  private String mapDbResponseToActionStatement(String response) {
    return switch (response) {
      case "update" -> "Update to a newer version";
      case "rollback" -> "Rollback to a previous version";
      case "workaround_available" -> "Workaround available";
      case "will_not_fix" -> "Will not fix";
      case "can_not_fix" -> "Can not fix";
      default -> response;
    };
  }

  public String extractUnsupportedProfileElements(String content, SbomFormat format) {
    return doExtractUnsupportedProfiles(content);
  }

  private SpdxDocument deserialize(String content) throws InvalidSPDXAnalysisException, IOException {
    SpdxModelFactory.init();
    InMemSpdxStore baseStore = new InMemSpdxStore();
    ModelCopyManager copyManager = new ModelCopyManager();
    JsonLDStore store = new JsonLDStore(baseStore);

    try (InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
      Object result = store.deSerialize(is, true);
      if (!(result instanceof SpdxDocument)) {
        throw new InvalidSPDXAnalysisException(
            "SPDX 3.0 document root is not an SpdxDocument (found " +
                (result != null ? result.getClass().getSimpleName() : "null") + ")");
      }
      return (SpdxDocument) result;
    }
  }

  private void generateLicenseRelationships(
      JsonLDStore store,
      ModelCopyManager copyManager,
      SpdxDocument document,
      CreationInfo creationInfo,
      String documentUri,
      SpdxGenerationContext context,
      Map<String, SpdxPackage> packagesByCoordinateId) throws InvalidSPDXAnalysisException
  {
    if (context.licensesByCoordinateId() == null || context.licensesByCoordinateId().isEmpty()) {
      return;
    }

    int licenseCount = 0;
    for (Map.Entry<String, Set<ResolvedLicenseDTO>> entry : context.licensesByCoordinateId().entrySet()) {
      String coordinateId = entry.getKey();
      Set<ResolvedLicenseDTO> licenses = entry.getValue();
      SpdxPackage pkg = packagesByCoordinateId.get(coordinateId);
      if (pkg == null || licenses.isEmpty()) {
        continue;
      }

      String expression = licenses.stream()
          .map(ResolvedLicenseDTO::licenseId)
          .filter(StringUtils::isNotBlank)
          .sorted()
          .reduce((a, b) -> a + " AND " + b)
          .orElse(null);
      if (expression == null) {
        continue;
      }

      String licenseUri = documentUri + "/license/" + coordinateId;
      LicenseExpression licenseExpr = new LicenseExpression.LicenseExpressionBuilder(
          store, licenseUri, copyManager)
              .setCreationInfo(creationInfo)
              .setLicenseExpression(expression)
              .build();
      document.getElements().add(licenseExpr);

      String relUri = documentUri + "/relationship/declaredLicense/" + coordinateId;
      Relationship licenseRel = new Relationship.RelationshipBuilder(
          store, relUri, copyManager)
              .setCreationInfo(creationInfo)
              .setFrom(pkg)
              .addTo(licenseExpr)
              .setRelationshipType(RelationshipType.HAS_DECLARED_LICENSE)
              .build();
      document.getElements().add(licenseRel);
      licenseCount++;
    }

    if (licenseCount > 0) {
      log.debug("SPDX 3.0 export: generated {} declared license relationships", licenseCount);
    }
  }

  private String extractRootComponentRef(
      SpdxDocument document,
      List<Pair<ComponentIdentifier, Component>> resolvedComponents)
  {
    Collection<Element> rootElements = document.getRootElements();
    if (rootElements.isEmpty()) {
      return null;
    }

    Set<String> rootUris = new HashSet<>();
    for (Element rootEl : rootElements) {
      if (rootEl instanceof SpdxPackage) {
        rootUris.add(rootEl.getObjectUri());
      }
    }

    for (Pair<ComponentIdentifier, Component> resolved : resolvedComponents) {
      Component component = resolved.getRight();
      if (component.getBomRef() != null && rootUris.contains(component.getBomRef())) {
        return SbomIdentityUtils.getComponentRef(component);
      }
    }
    return null;
  }

  private void processAllObjects(
      List<?> allObjects,
      List<Pair<ComponentIdentifier, Component>> resolvedComponents,
      List<ThirdPartyCoordinateSecurity> vulnerabilities,
      List<Dependency> dependencies,
      List<ThirdPartyVulnerabilityExploitabilityExchange> vexAnnotations,
      Map<String, Set<String>> vulnToPackageUris,
      List<Set<String>> vexAffectedPackageUris)
  {
    for (Object obj : allObjects) {
      if (obj instanceof SpdxPackage spdxPackage
          && !(spdxPackage instanceof AIPackage)
          && !(spdxPackage instanceof DatasetPackage))
      {
        processPackage(spdxPackage, resolvedComponents);
      }
      else if (obj instanceof Vulnerability vuln) {
        processVulnerability(vuln, vulnerabilities);
      }
      else if (obj instanceof Relationship rel) {
        processRelationship(rel, dependencies, vexAnnotations, vulnToPackageUris, vexAffectedPackageUris);
      }
    }
  }

  private void processPackage(
      SpdxPackage spdxPackage,
      List<Pair<ComponentIdentifier, Component>> resolvedComponents)
  {
    try {
      Pair<ComponentIdentifier, Component> resolved = resolvePackage(spdxPackage);
      if (resolved != null) {
        resolvedComponents.add(resolved);
      }
    }
    catch (Exception e) {
      try {
        log.warn("Error resolving SPDX 3.0 package: {}", spdxPackage.getName().orElse("unknown"), e);
      }
      catch (InvalidSPDXAnalysisException ex) {
        log.warn("Error resolving SPDX 3.0 package", e);
      }
    }
  }

  private void processVulnerability(
      Vulnerability vuln,
      List<ThirdPartyCoordinateSecurity> vulnerabilities)
  {
    try {
      ThirdPartyCoordinateSecurity security = new ThirdPartyCoordinateSecurity();
      vuln.getName().ifPresent(security::setRefId);
      vuln.getDescription().ifPresent(security::setDescription);

      for (ExternalIdentifier extId : vuln.getExternalIdentifiers()) {
        ExternalIdentifierType idType = extId.getExternalIdentifierType();
        if (idType == ExternalIdentifierType.CVE) {
          security.setRefId(extId.getIdentifier());
          security.setVulnerabilitySource("NVD");
        }
        else if (idType == ExternalIdentifierType.SECURITY_OTHER) {
          security.setRefId(extId.getIdentifier());
        }
      }

      security.setDetectionType(OTHER.getId());
      if (StringUtils.isNotBlank(security.getRefId())) {
        vulnerabilities.add(security);
      }
    }
    catch (InvalidSPDXAnalysisException e) {
      log.debug("Error extracting SPDX 3.0 vulnerability", e);
    }
  }

  private void processRelationship(
      Relationship rel,
      List<Dependency> dependencies,
      List<ThirdPartyVulnerabilityExploitabilityExchange> vexAnnotations,
      Map<String, Set<String>> vulnToPackageUris,
      List<Set<String>> vexAffectedPackageUris)
  {
    try {
      if (rel.getRelationshipType() == RelationshipType.DEPENDS_ON) {
        Element from = rel.getFrom();
        if (from != null) {
          Dependency dep = new Dependency(from.getObjectUri());
          for (Element to : rel.getTos()) {
            dep.addDependency(new Dependency(to.getObjectUri()));
          }
          dependencies.add(dep);
        }
      }
      else if (rel instanceof VexAffectedVulnAssessmentRelationship) {
        extractVexFromRelationship(rel, "affected", vexAnnotations, vulnToPackageUris, vexAffectedPackageUris);
      }
      else if (rel instanceof VexNotAffectedVulnAssessmentRelationship) {
        extractVexFromRelationship(rel, "not_affected", vexAnnotations, vulnToPackageUris, vexAffectedPackageUris);
      }
      else if (rel instanceof VexUnderInvestigationVulnAssessmentRelationship) {
        extractVexFromRelationship(rel, "under_investigation", vexAnnotations, vulnToPackageUris,
            vexAffectedPackageUris);
      }
      else if (rel instanceof VexFixedVulnAssessmentRelationship) {
        extractVexFromRelationship(rel, "fixed", vexAnnotations, vulnToPackageUris, vexAffectedPackageUris);
      }
    }
    catch (InvalidSPDXAnalysisException e) {
      log.debug("Error extracting SPDX 3.0 relationship", e);
    }
  }

  private void extractVexFromRelationship(
      Relationship rel,
      String state,
      List<ThirdPartyVulnerabilityExploitabilityExchange> vexAnnotations,
      Map<String, Set<String>> vulnToPackageUris,
      List<Set<String>> vexAffectedPackageUris) throws InvalidSPDXAnalysisException
  {
    Element fromElement = rel.getFrom();
    String refId = null;

    if (fromElement instanceof Vulnerability vuln) {
      for (ExternalIdentifier extId : vuln.getExternalIdentifiers()) {
        if (extId.getExternalIdentifierType() == ExternalIdentifierType.CVE) {
          refId = extId.getIdentifier();
          break;
        }
      }
      if (refId == null) {
        refId = vuln.getName().orElse(null);
      }
    }

    if (StringUtils.isNotBlank(refId)) {
      ThirdPartyVulnerabilityExploitabilityExchange vex = new ThirdPartyVulnerabilityExploitabilityExchange();
      vex.setState(state);
      vex.setRefId(refId);

      if (rel instanceof VexNotAffectedVulnAssessmentRelationship notAffected) {
        notAffected.getJustificationType().ifPresent(jt -> vex.setJustification(mapSpdx3JustificationToDb(jt)));
        notAffected.getImpactStatement().ifPresent(vex::setDetail);
      }
      else if (rel instanceof VexAffectedVulnAssessmentRelationship affected) {
        String actionStatement = affected.getActionStatement();
        if (StringUtils.isNotBlank(actionStatement)) {
          vex.setResponse(mapSpdx3ActionStatementToDb(actionStatement));
        }
      }

      if (vex.getDetail() == null) {
        rel.getDescription().ifPresent(vex::setDetail);
      }

      vexAnnotations.add(vex);

      Set<String> thisVexPackageUris = new HashSet<>();
      Set<String> packageUris = vulnToPackageUris.computeIfAbsent(refId, k -> new HashSet<>());
      for (Element to : rel.getTos()) {
        String uri = to.getObjectUri();
        packageUris.add(uri);
        thisVexPackageUris.add(uri);
      }
      vexAffectedPackageUris.add(thisVexPackageUris);
    }
  }

  private String mapSpdx3JustificationToDb(VexJustificationType justificationType) {
    return switch (justificationType) {
      case COMPONENT_NOT_PRESENT -> "code_not_present";
      case VULNERABLE_CODE_NOT_PRESENT -> "code_not_present";
      case VULNERABLE_CODE_NOT_IN_EXECUTE_PATH -> "code_not_reachable";
      case VULNERABLE_CODE_CANNOT_BE_CONTROLLED_BY_ADVERSARY -> "protected_at_runtime";
      case INLINE_MITIGATIONS_ALREADY_EXIST -> "protected_by_mitigating_control";
    };
  }

  private String mapSpdx3ActionStatementToDb(String actionStatement) {
    String lower = actionStatement.toLowerCase();
    if (lower.contains("update") || lower.contains("upgrade")) {
      return "update";
    }
    else if (lower.contains("rollback")) {
      return "rollback";
    }
    else if (lower.contains("workaround")) {
      return "workaround_available";
    }
    else if (lower.contains("will not fix")) {
      return "will_not_fix";
    }
    else if (lower.contains("can not fix") || lower.contains("cannot fix")) {
      return "can_not_fix";
    }
    return null;
  }

  private Pair<ComponentIdentifier, Component> resolvePackage(
      SpdxPackage spdxPackage) throws InvalidSPDXAnalysisException
  {
    String purl = extractPurl(spdxPackage);
    String cpe = extractCpe(spdxPackage);
    Optional<String> nameOpt = spdxPackage.getName();
    Optional<String> versionOpt = spdxPackage.getPackageVersion();

    if (StringUtils.isNotBlank(purl)) {
      try {
        purl = ensureMavenTypeQualifier(purl);
        PackageUrlIdentifier purlId = new PackageUrlIdentifier(purl);
        if (StringUtils.isNoneBlank(purlId.getName(), purlId.getVersion())) {
          Component component = buildComponent(spdxPackage, purlId, cpe);
          try {
            ComponentIdentifier componentId = SbomCommonUtils.getComponentIdentifier(purlId, component);
            return Pair.of(componentId, component);
          }
          catch (RuntimeException e) {
            log.debug("Incomplete component identifier from purl: {}", purl, e);
            return Pair.of(null, component);
          }
        }
      }
      catch (InvalidPackageURLException e) {
        log.debug("Invalid purl in SPDX 3.0 package: {}", purl, e);
      }
    }

    if (StringUtils.isNotBlank(cpe)) {
      PackageUrlIdentifier purlFromCpe = SbomCommonUtils.getPackageUrlIdentifierFromCpe(cpe);
      if (purlFromCpe != null && StringUtils.isNoneBlank(purlFromCpe.getName(), purlFromCpe.getVersion())) {
        Component component = buildComponent(spdxPackage, purlFromCpe, cpe);
        try {
          ComponentIdentifier componentId = SbomCommonUtils.getComponentIdentifier(purlFromCpe, component);
          return Pair.of(componentId, component);
        }
        catch (RuntimeException e) {
          log.debug("Incomplete component identifier from CPE: {}", cpe, e);
          return Pair.of(null, component);
        }
      }
    }

    if (nameOpt.isPresent() && versionOpt.isPresent() && StringUtils.isNotBlank(versionOpt.get())) {
      String name = nameOpt.get();
      String version = versionOpt.get();
      Component component = new Component();
      component.setType(Type.LIBRARY);
      component.setName(name);
      component.setVersion(version);
      return Pair.of(null, component);
    }

    return null;
  }

  private Component buildComponent(
      SpdxPackage spdxPackage,
      PackageUrlIdentifier purlId,
      String cpe) throws InvalidSPDXAnalysisException
  {
    Component component = new Component();
    component.setType(Type.LIBRARY);
    spdxPackage.getName().ifPresent(component::setName);
    spdxPackage.getPackageVersion().ifPresent(component::setVersion);
    component.setPurl(purlId.getPackageUrl());

    if (StringUtils.isNotBlank(cpe)) {
      component.setCpe(cpe);
    }

    component.setBomRef(spdxPackage.getObjectUri());
    return component;
  }

  private String extractPurl(SpdxPackage spdxPackage) throws InvalidSPDXAnalysisException {
    Optional<String> packageUrl = spdxPackage.getPackageUrl();
    if (packageUrl.isPresent() && StringUtils.isNotBlank(packageUrl.get())) {
      return packageUrl.get();
    }

    Collection<ExternalIdentifier> extIds = spdxPackage.getExternalIdentifiers();
    for (ExternalIdentifier extId : extIds) {
      if (extId.getExternalIdentifierType() == ExternalIdentifierType.PACKAGE_URL) {
        return extId.getIdentifier();
      }
    }
    return null;
  }

  private String extractCpe(SpdxPackage spdxPackage) throws InvalidSPDXAnalysisException {
    Collection<ExternalIdentifier> extIds = spdxPackage.getExternalIdentifiers();
    for (ExternalIdentifier extId : extIds) {
      if (extId.getExternalIdentifierType() == ExternalIdentifierType.CPE23) {
        return extId.getIdentifier();
      }
    }
    return null;
  }

  static String ensureMavenTypeQualifier(String purl) {
    if (purl != null && purl.startsWith("pkg:maven/") && !purl.contains("type=")) {
      return purl + (purl.contains("?") ? "&type=jar" : "?type=jar");
    }
    return purl;
  }

  private static ExternalIdentifierType resolveExternalIdentifierType(String refId) {
    if (refId == null) {
      return ExternalIdentifierType.OTHER;
    }
    if (refId.startsWith("CVE-")) {
      return ExternalIdentifierType.CVE;
    }
    if (refId.startsWith("GHSA-")) {
      return ExternalIdentifierType.SECURITY_OTHER;
    }
    return ExternalIdentifierType.SECURITY_OTHER;
  }

  private List<ThirdPartyVulnerabilityExploitabilityExchange> doParseVex(
      SpdxDocument document) throws InvalidSPDXAnalysisException
  {
    List<ThirdPartyVulnerabilityExploitabilityExchange> vexList = new ArrayList<>();
    List<?> allObjects = SpdxModelFactory.getSpdxObjects(
        document.getModelStore(),
        document.getCopyManager(),
        null,
        null,
        document.getSpecVersion()).toList();

    for (Object obj : allObjects) {
      if (obj instanceof Relationship rel) {
        processRelationship(rel, new ArrayList<>(), vexList, new HashMap<>(), new ArrayList<>());
      }
    }
    return vexList;
  }

  private String doExtractUnsupportedProfiles(String content) {
    try {
      JsonNode root = OBJECT_MAPPER.readTree(content);
      JsonNode graph = root.get("@graph");
      if (graph == null || !graph.isArray()) {
        return null;
      }

      ArrayNode unsupportedElements = OBJECT_MAPPER.createArrayNode();
      Set<String> unsupportedBlankNodeRefs = new HashSet<>();
      Set<String> supportedBlankNodeRefs = new HashSet<>();

      for (JsonNode element : graph) {
        if (isUnsupportedProfileElement(element)) {
          unsupportedElements.add(element);
          collectBlankNodeReferences(element, unsupportedBlankNodeRefs);
        }
        else {
          collectBlankNodeReferences(element, supportedBlankNodeRefs);
        }
      }

      if (unsupportedElements.isEmpty()) {
        return null;
      }

      Set<String> exclusiveBlankNodeRefs = new HashSet<>(unsupportedBlankNodeRefs);
      exclusiveBlankNodeRefs.removeAll(supportedBlankNodeRefs);

      if (!exclusiveBlankNodeRefs.isEmpty()) {
        for (JsonNode element : graph) {
          String id = element.has("@id") ? element.get("@id").asText() : null;
          if (id != null && exclusiveBlankNodeRefs.contains(id) && !isUnsupportedProfileElement(element)) {
            unsupportedElements.add(element);
          }
        }
      }

      ObjectNode result = OBJECT_MAPPER.createObjectNode();
      result.set("elements", unsupportedElements);
      return OBJECT_MAPPER.writeValueAsString(result);
    }
    catch (JsonProcessingException e) {
      log.debug("Error extracting unsupported profile elements from SPDX 3.0 document", e);
      return null;
    }
  }

  private void collectBlankNodeReferences(JsonNode node, Set<String> refs) {
    if (node.isTextual()) {
      String text = node.asText();
      if (text.startsWith("_:")) {
        refs.add(text);
      }
    }
    else if (node.isObject()) {
      node.fields().forEachRemaining(entry -> {
        if (!"@id".equals(entry.getKey())) {
          collectBlankNodeReferences(entry.getValue(), refs);
        }
      });
    }
    else if (node.isArray()) {
      for (JsonNode child : node) {
        collectBlankNodeReferences(child, refs);
      }
    }
  }

  private boolean isUnsupportedProfileElement(JsonNode element) {
    JsonNode typeNode = element.has("@type")
        ? element.get("@type")
        : element.has("type") ? element.get("type") : null;
    if (typeNode == null) {
      return false;
    }
    if (typeNode.isArray()) {
      for (JsonNode t : typeNode) {
        if (matchesUnsupportedProfile(t.asText())) {
          return true;
        }
      }
      return false;
    }
    return matchesUnsupportedProfile(typeNode.asText());
  }

  private boolean matchesUnsupportedProfile(String type) {
    return type.startsWith("ai_") || type.contains("/AI/")
        || type.startsWith("dataset_") || type.contains("/Dataset/")
        || type.startsWith("build_") || type.contains("/Build/");
  }
}
