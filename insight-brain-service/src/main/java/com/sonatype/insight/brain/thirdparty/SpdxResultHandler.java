/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;

import org.apache.commons.codec.digest.DigestUtils;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.SbomIdentityUtils;
import com.sonatype.insight.brain.sbom.export.SbomExportUtils;
import com.sonatype.insight.brain.sbom.spdx.ParsedSpdxResult;
import com.sonatype.insight.brain.sbom.spdx.Spdx3VersionHandler;
import com.sonatype.insight.brain.sbom.utils.SbomCommonUtils;
import com.sonatype.insight.brain.sbom.utils.SbomFileDetector;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
import com.sonatype.insight.brain.sbom.utils.SbomSpdxUtils;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.purl.InvalidPackageURLException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.file.SbomProcessingException;
import com.sonatype.insight.scan.file.SbomValidationException;
import com.sonatype.insight.scan.file.ThirdPartyUtils;
import com.sonatype.insight.scan.model.ProjectScanItem;
import com.sonatype.insight.telemetry.model.TelemetryData;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURLBuilder;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Component.Type;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.Hash;
import org.cyclonedx.model.Hash.Algorithm;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Swid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.model.v2.ExternalRef;
import org.spdx.library.model.v2.ModelObjectV2;
import org.spdx.library.model.v2.Relationship;
import org.spdx.library.model.v2.SpdxDocument;
import org.spdx.library.model.v2.SpdxPackage;
import org.spdx.library.model.v2.enumerations.ChecksumAlgorithm;
import org.spdx.library.model.v2.enumerations.ReferenceCategory;
import org.spdx.library.model.v2.enumerations.RelationshipType;
import org.spdx.library.model.v2.license.AnyLicenseInfo;
import org.spdx.library.model.v2.license.SpdxNoAssertionLicense;
import org.spdx.library.model.v2.license.SpdxNoneLicense;

import static com.sonatype.insight.brain.sbom.SbomSpecification.SPDX;
import static com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils.PROPERTY_COMPONENT_REF;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.getResearchTypeForThirdPartyVulnerability;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.getTruncatedThirdPartyIdentificationSource;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityDetectionType.OTHER;

public class SpdxResultHandler
    extends SbomResultHandler
    implements ThirdPartyScanResultHandler
{
  private static final Logger log = LoggerFactory.getLogger(SpdxResultHandler.class);

  private final Spdx3VersionHandler spdx3VersionHandler;

  public SpdxResultHandler(
      final ThirdPartyFileDAO thirdPartyFileDAO,
      final DuplicateAwareThirdPartyFileCoordinatePersister fileCoordinatePersister,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final MultiLicenseDAO multiLicenseDAO,
      final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVexDAO,
      final TelemetryUtils telemetryUtils,
      final TelemetrySender telemetrySender,
      final ThirdPartyScanContext thirdPartyScanContext,
      final Spdx3VersionHandler spdx3VersionHandler)
  {
    super(thirdPartyFileDAO, fileCoordinatePersister, thirdPartyCoordinateSecurityDAO,
        thirdPartyCoordinateLicenseDAO, thirdPartySbomMetadataDAO, multiLicenseDAO, thirdPartyVexDAO, telemetryUtils,
        telemetrySender, thirdPartyScanContext);
    this.spdx3VersionHandler = spdx3VersionHandler;
  }

  @Override
  public FilteredThirdPartyContent handleAndFilterContents(
      final ThirdPartyScanContent content,
      final ThirdPartyFile thirdPartyFile)
  {
    try {
      if (StringUtils.isNotBlank(content.getContent())) {
        if (SbomFileDetector.looksLikeSpdx3JsonLd(content.getContent())) {
          return handleSpdx3Content(content, thirdPartyFile);
        }
        return handleSpdx2Content(content, thirdPartyFile);
      }

      return new FilteredThirdPartyContent(content.getContent());
    }
    catch (Exception e) {
      throw new RuntimeException("Error filtering SPDX file " + content.getPath(), e);
    }
  }

  private FilteredThirdPartyContent handleSpdx3Content(
      final ThirdPartyScanContent content,
      final ThirdPartyFile thirdPartyFile) throws SbomProcessingException
  {
    String extension = FilenameUtils.getExtension(content.getPath());
    SbomFormat sbomFormat = SbomFormat.forString(extension.toLowerCase(Locale.ROOT));
    // SPDX 3.0 is always JSON-LD; fallback if extension is unrecognized (e.g., .jsonld)
    if (sbomFormat == null) {
      sbomFormat = SbomFormat.JSON;
    }
    componentInfoTelemetry.setContentType(sbomFormat.name());

    log.info("Processing SPDX 3.0 content for file: {}", content.getPath());

    ParsedSpdxResult parsed = spdx3VersionHandler.parse(content.getContent(), sbomFormat);

    long identifiedCount = parsed.resolvedComponents().stream().filter(p -> p.getLeft() != null).count();
    log.info("SPDX 3.0 parsed: {} total components, {} with identifiers",
        parsed.resolvedComponents().size(), identifiedCount);

    Bom targetBom = new Bom();
    List<ProjectScanItem> moduleDependencies = new ArrayList<>();

    try (TransactionContext tx = thirdPartyFileDAO.createTransactionContext()) {
      tx.begin();
      String thirdPartyIdentificationSource =
          getTruncatedThirdPartyIdentificationSource(determineThirdPartyIdentificationSource(content.getPath()));

      int persistedCount = 0;
      Map<String, String> bomRefToFileCoordinateId = new HashMap<>();
      for (Pair<ComponentIdentifier, Component> resolved : parsed.resolvedComponents()) {
        if (resolved.getLeft() != null) {
          ComponentIdentifier componentIdentifier = resolved.getLeft();
          Component component = resolved.getRight();
          String hash = getOrCreateFakeHash(component, componentIdentifier);
          ThirdPartyFileCoordinate fileCoordinate = new ThirdPartyFileCoordinate(
              hash, thirdPartyIdentificationSource, componentIdentifier.getFormat(),
              component.getName(), component.getVersion(), thirdPartyFile.getId());
          fileCoordinate.setPackageUrl(component.getPurl());
          if (component.getCpe() != null) {
            fileCoordinate.setCpe(component.getCpe());
          }
          fileCoordinate.setComponentRef(SbomIdentityUtils.getComponentRef(component));
          fileCoordinate.setIdentificationSources(SbomMetadataUtils.SBOM_IDENTIFICATION_SOURCE);
          componentInfoTelemetry.incrementEcosystemCount(fileCoordinate.getFormat());
          fileCoordinate = fileCoordinatePersister.persist(tx, fileCoordinate);
          targetBom.addComponent(component);
          persistedCount++;
          if (component.getBomRef() != null) {
            bomRefToFileCoordinateId.put(component.getBomRef(), fileCoordinate.getId());
          }
        }
        else {
          Component component = resolved.getRight();
          targetBom.addComponent(component);
          log.debug("SPDX 3.0 component filtered for matching only with hash information: {}", component.getName());
        }
      }
      log.info("SPDX 3.0 persisted {} components to thirdPartyFile {}", persistedCount, thirdPartyFile.getId());

      persistSpdx3Vulnerabilities(parsed, bomRefToFileCoordinateId, tx);
      persistSpdx3Vex(parsed, bomRefToFileCoordinateId, tx);

      if (thirdPartyScanContext != null
          && (parsed.unsupportedProfiles() != null || parsed.rootComponentRef() != null))
      {
        ThirdPartySbomMetadata sbomMetadata =
            thirdPartySbomMetadataDAO.getByThirdPartyFileId(thirdPartyFile.getId());
        if (sbomMetadata != null) {
          if (parsed.unsupportedProfiles() != null) {
            sbomMetadata.setExtendedProfileElements(parsed.unsupportedProfiles());
          }
          if (parsed.rootComponentRef() != null) {
            sbomMetadata.setRootComponentRef(parsed.rootComponentRef());
          }
          thirdPartySbomMetadataDAO.update(sbomMetadata);
        }
        else {
          log.warn(
              "SPDX 3.0: sbomMetadata not found for thirdPartyFile {}, extended profile elements will not be persisted",
              thirdPartyFile.getId());
        }
      }

      tx.commit();
    }

    processSpdx3DependencyGraph(parsed.dependencies(), targetBom, moduleDependencies, thirdPartyFile);

    componentInfoTelemetry.setSpec(SPDX.name());
    componentInfoTelemetry.setSpecVersion("3.0");
    componentInfoTelemetry.setHasDependencies(!moduleDependencies.isEmpty());

    TelemetryData thirdPartyScanComponentInfoTelemetryData =
        telemetryUtils.buildThirdPartyScanComponentInfoTelemetryData(componentInfoTelemetry,
            SystemConfigurationPropertyFeature.SKIP_SBOM_IMPORT_VALIDATION.isEnabled(), true);
    telemetrySender.send(thirdPartyScanComponentInfoTelemetryData);

    String sbomContent =
        CollectionUtils.isEmpty(targetBom.getComponents()) ? content.getContent() : generateFilteredSbom(targetBom);
    return new FilteredThirdPartyContent(sbomContent, moduleDependencies, false);
  }

  private void persistSpdx3Vulnerabilities(
      final ParsedSpdxResult parsed,
      final Map<String, String> bomRefToFileCoordinateId,
      final TransactionContext tx)
  {
    if (parsed.vulnerabilities().isEmpty()) {
      return;
    }

    Map<String, Set<String>> vulnToPackageUris = parsed.vulnerabilityToPackageUris();
    int persistedVulnCount = 0;

    for (ThirdPartyCoordinateSecurity vuln : parsed.vulnerabilities()) {
      Set<String> affectedUris = vulnToPackageUris.getOrDefault(vuln.getRefId(), Set.of());
      for (String packageUri : affectedUris) {
        String fileCoordinateId = bomRefToFileCoordinateId.get(packageUri);
        if (fileCoordinateId != null) {
          ThirdPartyCoordinateSecurity vulnCopy = new ThirdPartyCoordinateSecurity(
              fileCoordinateId, vuln.getRefId(), null, vuln.getDescription(), null, 0.0f, null);
          vulnCopy.setVulnerabilitySource(vuln.getVulnerabilitySource());
          vulnCopy.setDetectionType(vuln.getDetectionType());
          vulnCopy.setIdentificationSources(IdentificationSource.SBOM.getId());
          if (thirdPartyScanContext != null) {
            vulnCopy.setSbomMetadataId(thirdPartyScanContext.getSbomMetadataId());
          }
          thirdPartyCoordinateSecurityDAO.insertSafely(tx, vulnCopy);
          persistedVulnCount++;
        }
      }
    }

    log.info("SPDX 3.0 persisted {} vulnerability records (from {} unique vulnerabilities)",
        persistedVulnCount, parsed.vulnerabilities().size());
  }

  private void persistSpdx3Vex(
      final ParsedSpdxResult parsed,
      final Map<String, String> bomRefToFileCoordinateId,
      final TransactionContext tx)
  {
    if (parsed.vexAnnotations().isEmpty()) {
      return;
    }

    List<Set<String>> vexAffectedPackageUris = parsed.vexAffectedPackageUris();
    int persistedVexCount = 0;

    for (int i = 0; i < parsed.vexAnnotations().size(); i++) {
      ThirdPartyVulnerabilityExploitabilityExchange vex = parsed.vexAnnotations().get(i);
      Set<String> affectedUris = i < vexAffectedPackageUris.size() ? vexAffectedPackageUris.get(i) : Set.of();
      for (String packageUri : affectedUris) {
        String fileCoordinateId = bomRefToFileCoordinateId.get(packageUri);
        if (fileCoordinateId != null) {
          ThirdPartyCoordinateSecurity existing =
              thirdPartyCoordinateSecurityDAO.getByFileCoordinateIdAndRefId(tx, fileCoordinateId, vex.getRefId());
          if (existing != null) {
            ThirdPartyVulnerabilityExploitabilityExchange vexCopy =
                new ThirdPartyVulnerabilityExploitabilityExchange(
                    existing.getId(), vex.getRefId(), vex.getState(),
                    vex.getJustification(), vex.getResponse(), vex.getDetail());
            thirdPartyVexDAO.saveOrUpdate(tx, vexCopy);
            componentInfoTelemetry.incrementVulnerabilitiesWithVexInfoCount();
            persistedVexCount++;
          }
        }
      }
    }

    log.info("SPDX 3.0 persisted {} VEX records (from {} unique VEX annotations)",
        persistedVexCount, parsed.vexAnnotations().size());
  }

  private void processSpdx3DependencyGraph(
      final List<Dependency> dependencies,
      final Bom targetBom,
      final List<ProjectScanItem> moduleDependencies,
      final ThirdPartyFile thirdPartyFile)
  {
    try {
      if (CollectionUtils.isEmpty(dependencies) || CollectionUtils.isEmpty(targetBom.getComponents())) {
        return;
      }

      // Identify root: a dependency ref that is never a "to" target of another dependency
      Set<String> allTargets = new HashSet<>();
      for (Dependency dep : dependencies) {
        if (dep.getDependencies() != null) {
          for (Dependency child : dep.getDependencies()) {
            allTargets.add(child.getRef());
          }
        }
      }

      // Find a root component: appears as a dependency "from" but never as a target
      Component rootComponent = null;
      for (Dependency dep : dependencies) {
        if (dep.getRef() != null && !allTargets.contains(dep.getRef())) {
          for (Component comp : targetBom.getComponents()) {
            if (dep.getRef().equals(comp.getBomRef())) {
              rootComponent = comp;
              break;
            }
          }
          if (rootComponent != null) {
            break;
          }
        }
      }

      if (rootComponent == null) {
        return;
      }

      Metadata metadata = new Metadata();
      metadata.setComponent(rootComponent);
      targetBom.setMetadata(metadata);

      Pair<Dependency, String> rootModuleAndRef = resolveRootModuleAndRef(dependencies, targetBom);
      if (rootModuleAndRef != null) {
        processValidDependencyGraph(rootModuleAndRef, thirdPartyFile, targetBom,
            moduleDependencies, dependencies);
      }
    }
    catch (Exception e) {
      log.warn("Error processing SPDX 3.0 dependency graph", e);
    }
  }

  private FilteredThirdPartyContent handleSpdx2Content(
      final ThirdPartyScanContent content,
      final ThirdPartyFile thirdPartyFile) throws Exception
  {
    Pair<SpdxDocument, Boolean> spdxDocumentAndIsValid = parseSpdxContent(content);
    SpdxDocument spdxDocument = spdxDocumentAndIsValid.getLeft();
    boolean isValid = spdxDocumentAndIsValid.getRight();
    Bom targetBom = new Bom();
    List<ProjectScanItem> moduleDependencies = new ArrayList<>();

    log.info("Processing SPDX content for file: {}", content.getPath());
    processSpdxDocument(content.getPath(), spdxDocument, targetBom, thirdPartyFile, moduleDependencies, isValid);
    componentInfoTelemetry.setSpec(SPDX.name());
    componentInfoTelemetry.setSpecVersion(spdxDocument.getSpecVersion());
    componentInfoTelemetry.setHasDependencies(!moduleDependencies.isEmpty());

    TelemetryData thirdPartyScanComponentInfoTelemetryData =
        telemetryUtils.buildThirdPartyScanComponentInfoTelemetryData(componentInfoTelemetry,
            SystemConfigurationPropertyFeature.SKIP_SBOM_IMPORT_VALIDATION.isEnabled(), isValid);
    telemetrySender.send(thirdPartyScanComponentInfoTelemetryData);

    String sbomContent =
        CollectionUtils.isEmpty(targetBom.getComponents()) ? content.getContent() : generateFilteredSbom(targetBom);
    return new FilteredThirdPartyContent(sbomContent, moduleDependencies, !isValid);
  }

  private Pair<SpdxDocument, Boolean> parseSpdxContent(
      final ThirdPartyScanContent content) throws SbomProcessingException
  {
    String extension = FilenameUtils.getExtension(content.getPath());
    SbomFormat sbomFormat = SbomFormat.forString(extension.toLowerCase(Locale.ROOT));
    componentInfoTelemetry.setContentType(sbomFormat.name());

    Boolean isValid = thirdPartyScanContext == null ? null : thirdPartyScanContext.isValid();

    if (isValid == null) {
      try {
        return Pair.of(ThirdPartyUtils.parseAndValidateSpdx(content.getContent(), sbomFormat), true);
      }
      catch (SbomValidationException e) {
        if (SystemConfigurationPropertyFeature.SKIP_SBOM_IMPORT_VALIDATION.isEnabled()) {
          return Pair.of(ThirdPartyUtils.parseSpdxWithNoValidation(content.getContent(), sbomFormat), false);
        }
        else {
          throw e;
        }
      }
    }
    else if (isValid) {
      return Pair.of(ThirdPartyUtils.parseAndValidateSpdx(content.getContent(), sbomFormat), true);
    }
    else {
      return Pair.of(ThirdPartyUtils.parseSpdxWithNoValidation(content.getContent(), sbomFormat), false);
    }
  }

  private void processSpdxDocument(
      final String contentPath,
      final SpdxDocument spdxDocument,
      final Bom targetBom,
      final ThirdPartyFile thirdPartyFile,
      final List<ProjectScanItem> moduleDependencies,
      final boolean isValid) throws InvalidSPDXAnalysisException
  {
    String thirdPartyIdentificationSource =
        getTruncatedThirdPartyIdentificationSource(determineThirdPartyIdentificationSource(contentPath));
    try (TransactionContext tx = thirdPartyFileDAO.createTransactionContext()) {
      tx.begin();
      String rootPackageId = collectFilteredMetadata(spdxDocument, targetBom);
      Map<String, String> componentRefs = new HashMap<>();
      processComponents(spdxDocument, targetBom, componentRefs, rootPackageId, thirdPartyIdentificationSource,
          thirdPartyFile, tx, isValid);
      tx.commit();
    }
    if (isValid) {
      processDependencyGraph(spdxDocument, targetBom, moduleDependencies, thirdPartyFile);
    }
  }

  private void processComponents(
      final SpdxDocument spdxDocument,
      final Bom targetBom,
      final Map<String, String> componentRefs,
      final String rootPackageId,
      final String thirdPartyIdentificationSource,
      final ThirdPartyFile thirdPartyFile,
      final TransactionContext tx,
      final boolean isValid) throws InvalidSPDXAnalysisException
  {
    List<? extends ModelObjectV2> items = getSpdxPackages(spdxDocument);
    if (!items.isEmpty()) {
      Set<ComponentIdentifier> resolvedComponents = new HashSet<>();
      for (ModelObjectV2 item : items) {
        SpdxPackage spdxPackage = (SpdxPackage) item;
        processSpdxPackage(spdxPackage, thirdPartyFile.getId(), targetBom, thirdPartyIdentificationSource,
            resolvedComponents, componentRefs, rootPackageId, tx, isValid);
      }
    }
  }

  private List<? extends ModelObjectV2> getSpdxPackages(
      final SpdxDocument spdxDocument) throws InvalidSPDXAnalysisException
  {
    return SbomSpdxUtils.getAllPackages(spdxDocument);
  }

  private void processSpdxPackage(
      final SpdxPackage spdxPackage,
      final String thirdPartyFileId,
      final Bom targetBom,
      final String thirdPartyIdentificationSource,
      final Set<ComponentIdentifier> resolvedComponents,
      final Map<String, String> componentRefs,
      final String rootPackageId,
      final TransactionContext tx,
      final boolean isValid) throws InvalidSPDXAnalysisException
  {
    try {
      Pair<ComponentIdentifier, Component> resolvedComponent = getResolvedComponent(spdxPackage, rootPackageId);
      if (resolvedComponent != null) {
        String componentRef = DigestUtils.sha1Hex(spdxPackage.getId());
        resolvedComponent.getRight()
            .addProperty(SbomExportUtils.createCycloneDxProperty(PROPERTY_COMPONENT_REF, componentRef));
        ComponentIdentifier componentIdentifier = resolvedComponent.getLeft();
        if (componentIdentifier == null) {
          targetBom.addComponent(resolvedComponent.getRight());
          log.debug("Component filtered for matching only with hash information {}", resolvedComponent.getRight());
        }
        else if (resolvedComponents.add(componentIdentifier)) {
          PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).ensureCompleteIdentifier();
          String coordinateId = saveComponent(thirdPartyFileId, thirdPartyIdentificationSource, spdxPackage,
              resolvedComponent, componentRef, tx, isValid);
          if (StringUtils.isNotBlank(spdxPackage.getId())) {
            componentRefs.put(spdxPackage.getId(), coordinateId);
          }
          targetBom.addComponent(resolvedComponent.getRight());
        }
      }
      else {
        log.debug("Error processing component due to insufficient information: id {}, name {}, version {}",
            spdxPackage.getId(), spdxPackage.getName(), spdxPackage.getVersionInfo().orElse(""));
      }
    }
    catch (InvalidPackageURLException e) {
      log.debug("Component {} {} is missing coordinates. {}", spdxPackage.getName(),
          spdxPackage.getVersionInfo().orElse(""), e.getMessage().replace(" for given format", ""), e);
    }
    catch (Exception e) {
      log.debug("Error processing component due to insufficient information: id {}, name {}, version {}",
          spdxPackage.getId(), spdxPackage.getName(), spdxPackage.getVersionInfo().orElse(""), e);
    }
  }

  private String saveComponent(
      final String thirdPartyFileId,
      final String thirdPartyIdentificationSource,
      final SpdxPackage spdxPackage,
      final Pair<ComponentIdentifier, Component> resolvedComponent,
      final String componentRef,
      final TransactionContext tx,
      final boolean isValid) throws InvalidSPDXAnalysisException, JsonProcessingException
  {
    Component component = resolvedComponent.getRight();
    ComponentIdentifier componentIdentifier;
    Optional<String> purlOptional = getPurl(spdxPackage);
    if (purlOptional.isPresent()) {
      PackageUrlIdentifier packageUrlIdentifier = resolvePackageUrl(purlOptional.get());
      componentIdentifier = packageUrlIdentifier.toComponentIdentifier();
    }
    else {
      componentIdentifier = resolvedComponent.getLeft();
    }

    String hash = getOrCreateFakeHash(component, componentIdentifier);
    ThirdPartyFileCoordinate fileCoordinate = new ThirdPartyFileCoordinate(hash, thirdPartyIdentificationSource,
        componentIdentifier.getFormat(), component.getName(), component.getVersion(), thirdPartyFileId);
    fileCoordinate.setPackageUrl(component.getPurl());
    if (component.getCpe() != null) {
      fileCoordinate.setCpe(component.getCpe());
    }
    if (component.getSwid() != null) {
      fileCoordinate.setSwid(ThirdPartyComponentDAO.MAPPER.writeValueAsString(component.getSwid()));
    }
    if (StringUtils.isNotEmpty(componentRef)) {
      fileCoordinate.setComponentRef(componentRef);
    }
    fileCoordinate.setIdentificationSources(SbomMetadataUtils.SBOM_IDENTIFICATION_SOURCE);
    componentInfoTelemetry.incrementEcosystemCount(fileCoordinate.getFormat());
    fileCoordinate = fileCoordinatePersister.persist(tx, fileCoordinate);

    if (isValid) {
      saveLicenses(spdxPackage, fileCoordinate.getId(), component.getPurl(), tx);
      saveVulnerabilities(spdxPackage, fileCoordinate.getId(), component.getPurl(), tx);
    }

    return fileCoordinate.getId();
  }

  private void saveVulnerabilities(
      final SpdxPackage spdxPackage,
      final String fileCoordinateId,
      final String packageUrl,
      final TransactionContext tx) throws InvalidSPDXAnalysisException
  {
    Collection<ExternalRef> externalRefs = spdxPackage.getExternalRefs();
    Set<String> processedVulnerabilityIds = new HashSet<>();
    for (ExternalRef externalRef : externalRefs) {
      if (externalRef.getReferenceCategory() == ReferenceCategory.SECURITY) {
        ThirdPartyCoordinateSecurity coordinateSecurity = parseVulnerability(externalRef, fileCoordinateId);
        if (coordinateSecurity != null) {
          if (thirdPartyScanContext != null) {
            coordinateSecurity.setSbomMetadataId(thirdPartyScanContext.getSbomMetadataId());
          }
          if (processedVulnerabilityIds.add(coordinateSecurity.getRefId())) {
            thirdPartyCoordinateSecurityDAO.insertSafely(tx, coordinateSecurity);
          }
          else {
            log.debug("Component with packageUrl {} has duplicate vulnerability with ID {}", packageUrl,
                coordinateSecurity.getRefId());
          }
        }
      }
    }
  }

  private ThirdPartyCoordinateSecurity parseVulnerability(
      final ExternalRef externalRef,
      final String fileCoordinateId) throws InvalidSPDXAnalysisException
  {
    Pair<String, String> vulnSource = SbomSpdxUtils.getRefIdAndSourceForVulnerability(externalRef);
    if (vulnSource != null) {
      return createThirdPartyCoordinateSecurity(fileCoordinateId, vulnSource.getKey(),
          externalRef.getReferenceLocator(),
          vulnSource.getValue());
    }
    return null;
  }

  private ThirdPartyCoordinateSecurity createThirdPartyCoordinateSecurity(
      final String fileCoordinateId,
      final String refId,
      final String link,
      final String source)
  {
    ThirdPartyCoordinateSecurity coordinateSecurity =
        new ThirdPartyCoordinateSecurity(fileCoordinateId, refId, null, null, link, 0.0f, null);
    coordinateSecurity.setVulnerabilitySource(source);
    coordinateSecurity.setResearchType(getResearchTypeForThirdPartyVulnerability(
        coordinateSecurity.getVulnerabilitySource(), coordinateSecurity.getRefId()));
    coordinateSecurity.setDetectionType(OTHER.getId());
    coordinateSecurity.setIdentificationSources(IdentificationSource.SBOM.getId());
    return coordinateSecurity;
  }

  private void saveLicenses(
      final SpdxPackage spdxPackage,
      final String fileCoordinateId,
      final String packageUrl,
      final TransactionContext tx) throws InvalidSPDXAnalysisException
  {
    AnyLicenseInfo license = spdxPackage.getLicenseConcluded(); // preferred
    if (license instanceof SpdxNoAssertionLicense) {
      license = spdxPackage.getLicenseDeclared(); // fallback
    }
    if (license instanceof SpdxNoAssertionLicense) {
      log.debug("No licenses provided for Component with packageUrl {}", packageUrl);
    }
    else {
      if (license instanceof SpdxNoneLicense) {
        log.debug("Found empty licenses element for Component with packageUrl {}", packageUrl);
      }
      else {
        Map<String, String> processedLicenses = new HashMap<>();
        try {
          spdxLicenseExpressionUtil.parseLicenses(license, processedLicenses, packageUrl);
          for (Entry<String, String> licenseEntry : processedLicenses.entrySet()) {
            ThirdPartyCoordinateLicense coordinateLicense =
                new ThirdPartyCoordinateLicense(fileCoordinateId, licenseEntry.getKey(), licenseEntry.getValue(), null);
            coordinateLicense.setIdentificationSources(IdentificationSource.SBOM.getId());
            thirdPartyCoordinateLicenseDAO.insertSafely(tx, coordinateLicense);
            componentInfoTelemetry.incrementValidLicensesCount();
          }
        }
        catch (InvalidSPDXAnalysisException ex) {
          componentInfoTelemetry.incrementInvalidLicensesCount();
          throw ex;
        }
      }
    }
  }

  private Pair<ComponentIdentifier, Component> getResolvedComponent(
      final SpdxPackage spdxPackage,
      final String rootPackageId) throws InvalidSPDXAnalysisException, MalformedPackageURLException
  {
    Optional<String> purlOptional = getPurl(spdxPackage);
    String cpe = SbomSpdxUtils.getCpe(spdxPackage);

    try {
      if (purlOptional.isPresent()) {
        String packageUrl = purlOptional.get();
        PackageUrlIdentifier packageUrlIdentifier = resolvePackageUrl(packageUrl);
        if (packageUrlIdentifier != null) {
          packageUrlIdentifier.ensureCompleteIdentifier();
          if (StringUtils.isNoneBlank(packageUrlIdentifier.getName(), packageUrlIdentifier.getVersion())) {
            componentInfoTelemetry.incrementPurlCount();
            return createComponent(spdxPackage, packageUrlIdentifier, rootPackageId, cpe);
          }
        }
        else {
          log.debug("PackageUrl is not valid {}", packageUrl);
        }
      }
    }
    catch (InvalidPackageURLException e) {
      log.debug("Invalid purl: {}", purlOptional.orElse(""), e);
    }
    catch (InvalidComponentIdentifierException e) {
      log.debug("Invalid Component Identifier for provided purl {}", purlOptional.orElse(""), e);
    }

    if (StringUtils.isNotBlank(cpe)) {
      PackageUrlIdentifier packageUrlIdentifier = SbomCommonUtils.getPackageUrlIdentifierFromCpe(cpe);
      if (packageUrlIdentifier != null &&
          StringUtils.isNoneBlank(packageUrlIdentifier.getName(), packageUrlIdentifier.getVersion()))
      {
        return createComponent(spdxPackage, packageUrlIdentifier, rootPackageId, cpe);
      }
    }
    return processComponentFromHashOrCoordinates(spdxPackage, rootPackageId);
  }

  private Pair<ComponentIdentifier, Component> createComponent(
      final SpdxPackage spdxPackage,
      final PackageUrlIdentifier packageUrlIdentifier,
      final String rootPackageId,
      final String cpe) throws InvalidSPDXAnalysisException
  {
    Component component = new Component();
    component.setType(spdxPackage.getId().equals(rootPackageId) ? Type.APPLICATION : Type.LIBRARY);
    component.setBomRef(spdxPackage.getId());

    if (StringUtils.isNotBlank(cpe)) {
      componentInfoTelemetry.incrementCpeCount();
      component.setCpe(cpe);
    }

    Optional<Swid> swidOptional = SbomSpdxUtils.getSwid(spdxPackage);
    if (swidOptional.isPresent()) {
      component.setSwid(swidOptional.get());
      componentInfoTelemetry.incrementSwidCount();
    }

    final Optional<String> sha1Optional = SbomSpdxUtils.getChecksum(spdxPackage, ChecksumAlgorithm.SHA1);
    boolean hasHash = sha1Optional.isPresent();
    if (hasHash) {
      setSha1Property(sha1Optional.get(), component);
    }

    ComponentIdentifier componentIdentifier = SbomCommonUtils.getComponentIdentifier(packageUrlIdentifier, component);
    // Process sha-256 only when BFS is enabled
    if (SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.isEnabled()) {
      SbomSpdxUtils.getChecksum(spdxPackage, ChecksumAlgorithm.SHA256)
          .ifPresent(
              v -> component.addHash(new Hash(Algorithm.SHA_256, v)));
    }
    return Pair.of(componentIdentifier, component);
  }

  /**
   * Collect the component hash (SHA1) and coordinates, if possible. The hash has priority, if exists.
   */
  private Pair<ComponentIdentifier, Component> processComponentFromHashOrCoordinates(
      final SpdxPackage spdxPackage,
      final String rootPackageId) throws InvalidSPDXAnalysisException, MalformedPackageURLException
  {
    boolean isRootPackage = spdxPackage.getId().equals(rootPackageId);

    // try using the SPDX package name and version
    String name = spdxPackage.getName().orElse(MISSING_COMPONENT_NAME);
    String version = spdxPackage.getVersionInfo().orElse("");
    if (StringUtils.isNotBlank(version)) {
      componentInfoTelemetry.incrementCoordinateCount();
      PackageUrlIdentifier packageUrlIdentifier = resolvePackageUrl(
          getPackageUrlFromCoordinates(name, version, isRootPackage));
      return createComponent(spdxPackage, packageUrlIdentifier, rootPackageId, null);
    }
    else {
      // This scenario is only possible when only the hash is sent without coordinates nor purl
      Optional<String> sha1Optional = SbomSpdxUtils.getChecksum(spdxPackage, ChecksumAlgorithm.SHA1);
      if (sha1Optional.isPresent()) {
        componentInfoTelemetry.incrementHashCount();
        Component component = new Component();
        component.setType(isRootPackage ? Type.APPLICATION : Type.LIBRARY);
        component.setBomRef(spdxPackage.getId());
        spdxPackage.getName().ifPresent(component::setName);
        setSha1Property(sha1Optional.get(), component);
        return Pair.of(null, component);
      }
    }
    return null;
  }

  private String getPackageUrlFromCoordinates(
      String name,
      final String version,
      boolean isRootPackage) throws MalformedPackageURLException
  {
    String group = null;
    if (name.contains(":")) {
      // some SPDX generators set the name value as 'group:name' because there's no other placeholder for the group
      final String[] parts = name.split(":");
      if (parts.length == 2) {
        group = parts[0];
        name = parts[1];
      }
    }
    PackageURLBuilder packageURLBuilder = PackageURLBuilder.aPackageURL()
        .withType(PackageUrlIdentifier.GENERIC_FORMAT)
        .withName(name)
        .withVersion(version);
    if (StringUtils.isNotBlank(group)) {
      packageURLBuilder.withNamespace(group);
    }
    packageURLBuilder.withQualifier(PURL_BOM_TYPE, isRootPackage
        ? Type.APPLICATION.getTypeName()
        : Type.LIBRARY.getTypeName());
    return packageURLBuilder.build().toString();
  }

  @VisibleForTesting
  void processDependencyGraph(
      final SpdxDocument spdxDocument,
      final Bom targetBom,
      final List<ProjectScanItem> moduleDependencies,
      final ThirdPartyFile thirdPartyFile)
  {
    try {
      if (CollectionUtils.isNotEmpty(targetBom.getComponents())) {
        List<Dependency> bomDependencies = getDependencyList(spdxDocument);
        if (CollectionUtils.isNotEmpty(bomDependencies)) {
          Pair<Dependency, String> rootModuleAndRef =
              resolveRootModuleAndRef(bomDependencies, targetBom);
          if (rootModuleAndRef != null) {
            processValidDependencyGraph(rootModuleAndRef, thirdPartyFile, targetBom,
                moduleDependencies, bomDependencies);
          }
          else {
            log.debug("Unable to process dependency graph. The root component of the bom {} cannot be determined",
                thirdPartyFile.getFilename());
          }
        }
      }
    }
    catch (Exception e) {
      log.warn("There was an error processing dependency graph", e);
    }
  }

  private List<Dependency> getDependencyList(final SpdxDocument spdxDocument) throws InvalidSPDXAnalysisException {
    Map<String, Dependency> dependencyMap = new HashMap<>();

    // relationships are attached to packages in the SPDX object model
    List<? extends ModelObjectV2> items = getSpdxPackages(spdxDocument);
    for (ModelObjectV2 item : items) {
      SpdxPackage spdxPackage = (SpdxPackage) item;
      Collection<Relationship> relationships = spdxPackage.getRelationships();
      for (Relationship relationship : relationships) {
        if (relationship.getRelationshipType() == RelationshipType.DESCRIBES ||
            relationship.getRelatedSpdxElement().isEmpty())
        {
          continue;
        }
        String refId1 = spdxPackage.getId();
        String refId2 = relationship.getRelatedSpdxElement().get().getId();
        Dependency dependency1 = dependencyMap.computeIfAbsent(refId1, Dependency::new);
        Dependency dependency2 = dependencyMap.computeIfAbsent(refId2, Dependency::new);

        switch (relationship.getRelationshipType()) {
          case DEPENDS_ON:
            dependency1.addDependency(dependency2);
            break;
          case DEPENDENCY_OF:
          case BUILD_DEPENDENCY_OF:
          case DEV_DEPENDENCY_OF:
          case OPTIONAL_DEPENDENCY_OF:
          case PROVIDED_DEPENDENCY_OF:
          case RUNTIME_DEPENDENCY_OF:
          case TEST_DEPENDENCY_OF:
            dependency2.addDependency(dependency1);
            break;
          default:
        }
      }
    }
    return new ArrayList<>(dependencyMap.values());
  }

  /**
   * Collects metadata
   *
   * @return the ID of the root package, if any; otherwise, it returns an empty string.
   */
  private String collectFilteredMetadata(
      final SpdxDocument spdxDocument,
      final Bom targetBom) throws InvalidSPDXAnalysisException
  {
    String rootElementId = "";
    Metadata metadata = new Metadata();
    SpdxPackage documentDescribesPackage = SbomSpdxUtils.getRootPackage(spdxDocument);
    if (documentDescribesPackage != null) {
      rootElementId = documentDescribesPackage.getId();
      Component component = new Component();
      component.setType(Type.APPLICATION);
      component.setBomRef(rootElementId);
      documentDescribesPackage.getName().ifPresent(name -> {
        if (name.contains(":")) {
          final String[] parts = name.split(":");
          component.setGroup(parts[0]);
          component.setName(parts[1]);
        }
        else {
          component.setName(name);
        }
      });
      documentDescribesPackage.getVersionInfo().ifPresent(component::setVersion);
      getPurl(documentDescribesPackage).ifPresent(component::setPurl);
      metadata.setComponent(component);
    }
    if (spdxDocument.getCreationInfo() != null) {
      final String created = spdxDocument.getCreationInfo().getCreated();
      if (StringUtils.isNotBlank(created)) {
        try {
          DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
          metadata.setTimestamp(dateFormat.parse(created));
        }
        catch (ParseException e) {
          log.warn("Cannot parse creation date: {}", created);
        }
      }
    }
    targetBom.setMetadata(metadata);
    return rootElementId;
  }

  private Optional<String> getPurl(final SpdxPackage spdxPackage) throws InvalidSPDXAnalysisException {
    final Collection<ExternalRef> externalRefs = spdxPackage.getExternalRefs();
    for (ExternalRef externalRef : externalRefs) {
      if (externalRef.getReferenceCategory() == ReferenceCategory.PACKAGE_MANAGER &&
          externalRef.getReferenceType().getIndividualURI().endsWith("/purl"))
      {
        return Optional.of(externalRef.getReferenceLocator());
      }
    }
    return Optional.empty();
  }

  @VisibleForTesting
  @Override
  String determineThirdPartyIdentificationSource(final String contentPath) {
    String fileName =
        StringUtils.contains(contentPath, "/") ? StringUtils.substringAfterLast(contentPath, "/") : contentPath;
    String thirdPartyIdentificationSource = RegExUtils.removePattern(fileName, "\\.(?i)spdx\\.(xml|json)(?i)$");
    if (StringUtils.isBlank(thirdPartyIdentificationSource) ||
        StringUtils.endsWithIgnoreCase(thirdPartyIdentificationSource, "spdx.xml") ||
        StringUtils.endsWithIgnoreCase(thirdPartyIdentificationSource, "spdx.json"))
    {
      return "Third-Party";
    }
    else {
      return thirdPartyIdentificationSource;
    }
  }
}
