/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.SbomTaxonomy;

import org.apache.commons.lang3.StringUtils;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.sbom.SbomComponentInfoTelemetry;
import com.sonatype.insight.SbomIdentityUtils;
import com.sonatype.insight.brain.sbom.export.SbomExportException;
import com.sonatype.insight.brain.sbom.export.SbomExportParams;
import com.sonatype.insight.brain.sbom.export.SbomExportUtils;
import com.sonatype.insight.brain.sbom.utils.SbomCommonUtils;
import com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
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
import com.sonatype.insight.util.SbomUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.github.packageurl.PackageURLBuilder;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclonedx.Version;
import org.cyclonedx.exception.GeneratorException;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.ExtensibleType;
import org.cyclonedx.model.Extension;
import org.cyclonedx.model.Hash;
import org.cyclonedx.model.License;
import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.Swid;
import org.cyclonedx.model.component.evidence.Occurrence;
import org.cyclonedx.model.license.Expression;
import org.cyclonedx.model.vulnerability.Rating;
import org.cyclonedx.model.vulnerability.Vulnerability;
import org.cyclonedx.model.vulnerability.Vulnerability.Affect;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating.Method;
import org.cyclonedx.model.vulnerability.Vulnerability10;
import org.cyclonedx.model.vulnerability.Vulnerability10.Advisory;
import org.cyclonedx.model.vulnerability.Vulnerability10.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.model.v2.license.AnyLicenseInfo;
import org.spdx.library.model.v2.license.InvalidLicenseStringException;
import org.spdx.library.LicenseInfoFactory;
import us.springett.parsers.cpe.util.Validate;

import static com.sonatype.insight.brain.sbom.SbomSpecification.CYCLONEDX;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.getResearchTypeForThirdPartyVulnerability;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.getTruncatedAttackVector;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.getTruncatedLink;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.getTruncatedRatingMethod;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.getTruncatedRefId;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.getTruncatedSeverityDescription;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.getTruncatedThirdPartyIdentificationSource;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.getTruncatedVulnerabilitySource;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityDetectionType.OTHER;

public class SbomResultHandler
    implements ThirdPartyScanResultHandler
{
  private static final Logger log = LoggerFactory.getLogger(SbomResultHandler.class);

  protected static final String MISSING_COMPONENT_NAME = "[Not Provided]";

  public static final String PURL_BOM_TYPE = "sbom_type";

  protected final ThirdPartyFileDAO thirdPartyFileDAO;

  protected final DuplicateAwareThirdPartyFileCoordinatePersister fileCoordinatePersister;

  protected final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  protected final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  protected final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  protected final MultiLicenseDAO multiLicenseDAO;

  protected final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVexDAO;

  protected final TelemetryUtils telemetryUtils;

  protected final TelemetrySender telemetrySender;

  protected final SpdxLicenseExpressionUtil spdxLicenseExpressionUtil;

  protected final SbomComponentInfoTelemetry componentInfoTelemetry;

  protected final ThirdPartyScanContext thirdPartyScanContext;

  public SbomResultHandler(
      final ThirdPartyFileDAO thirdPartyFileDAO,
      final DuplicateAwareThirdPartyFileCoordinatePersister fileCoordinatePersister,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final MultiLicenseDAO multiLicenseDAO,
      final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVexDAO,
      final TelemetryUtils telemetryUtils,
      final TelemetrySender telemetrySender,
      final ThirdPartyScanContext thirdPartyScanContext)
  {
    this.thirdPartyFileDAO = thirdPartyFileDAO;
    this.fileCoordinatePersister = fileCoordinatePersister;
    this.thirdPartyCoordinateSecurityDAO = thirdPartyCoordinateSecurityDAO;
    this.thirdPartyCoordinateLicenseDAO = thirdPartyCoordinateLicenseDAO;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.multiLicenseDAO = multiLicenseDAO;
    this.thirdPartyVexDAO = thirdPartyVexDAO;
    this.telemetryUtils = telemetryUtils;
    this.telemetrySender = telemetrySender;
    this.thirdPartyScanContext = thirdPartyScanContext;
    this.componentInfoTelemetry = new SbomComponentInfoTelemetry();
    this.spdxLicenseExpressionUtil = new SpdxLicenseExpressionUtil(multiLicenseDAO);
  }

  @Override
  public FilteredThirdPartyContent handleAndFilterContents(
      ThirdPartyScanContent content,
      ThirdPartyFile thirdPartyFile)
  {
    try {
      if (!StringUtils.isBlank(content.getContent()) && ThirdPartyUtils.looksLikeCycloneDX(content.getContent())) {
        Pair<Bom, Boolean> bomAndIsValid = parseBom(content);
        Bom sourceBom = bomAndIsValid.getLeft();
        boolean isValid = bomAndIsValid.getRight();
        Bom targetBom = new Bom();
        List<ProjectScanItem> moduleDependencies = new ArrayList<>();

        log.info("Processing SBOM content for file: {}", content.getPath());
        processSbom(content.getPath(), sourceBom, targetBom, thirdPartyFile, moduleDependencies, isValid);
        componentInfoTelemetry.setSpec(CYCLONEDX.name());
        componentInfoTelemetry.setSpecVersion(sourceBom.getSpecVersion());
        componentInfoTelemetry.setHasDependencies(!moduleDependencies.isEmpty());

        TelemetryData thirdPartyScanComponentInfoTelemetryData =
            telemetryUtils.buildThirdPartyScanComponentInfoTelemetryData(componentInfoTelemetry,
                SystemConfigurationPropertyFeature.SKIP_SBOM_IMPORT_VALIDATION.isEnabled(), isValid);
        telemetrySender.send(thirdPartyScanComponentInfoTelemetryData);

        String sbomContent = targetBom.getComponents() != null && targetBom.getComponents().isEmpty()
            ? content.getContent()
            : generateFilteredSbom(targetBom);
        return new FilteredThirdPartyContent(sbomContent, moduleDependencies, !isValid);
      }

      return new FilteredThirdPartyContent(content.getContent());
    }
    catch (Exception e) {
      throw new RuntimeException("Error filtering sbom file " + content.getPath(), e);
    }
  }

  // visible for testing
  Pair<Bom, Boolean> parseBom(final ThirdPartyScanContent content) throws SbomProcessingException {
    String extension = FilenameUtils.getExtension(content.getPath());
    SbomFormat sbomFormat = SbomFormat.forString(extension.toLowerCase(Locale.ROOT));
    componentInfoTelemetry.setContentType(sbomFormat.name());

    Boolean isValid = thirdPartyScanContext == null ? null : thirdPartyScanContext.isValid();

    if (isValid == null) {
      try {
        return Pair.of(ThirdPartyUtils.parseAndValidateCycloneDx(content.getContent(), sbomFormat), true);
      }
      catch (SbomValidationException e) {
        if (SystemConfigurationPropertyFeature.SKIP_SBOM_IMPORT_VALIDATION.isEnabled()) {
          return Pair.of(ThirdPartyUtils.parseCycloneDxWithNoValidation(content.getContent(), sbomFormat), false);
        }
        else {
          throw e;
        }
      }
    }
    else if (isValid) {
      return Pair.of(ThirdPartyUtils.parseAndValidateCycloneDx(content.getContent(), sbomFormat), true);
    }
    else {
      return Pair.of(ThirdPartyUtils.parseCycloneDxWithNoValidation(content.getContent(), sbomFormat), false);
    }
  }

  void processSbom(
      final String contentPath,
      final Bom sourceBom,
      final Bom targetBom,
      final ThirdPartyFile thirdPartyFile,
      final List<ProjectScanItem> dependencyGraph,
      final boolean isValid)
  {
    String thirdPartyIdentificationSource =
        getTruncatedThirdPartyIdentificationSource(determineThirdPartyIdentificationSource(contentPath));

    if (sourceBom.getMetadata() != null && sourceBom.getMetadata().getProperties() != null) {
      String originalFileNameProperty = this.getPropertyAsString(sourceBom.getMetadata().getProperties(),
          SbomTaxonomy.CDX_ORIGINAL_FILE_PROPERTY_NAME);
      if (StringUtils.isNotEmpty(originalFileNameProperty)) {
        ThirdPartySbomMetadata sbomMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(thirdPartyFile.getId());
        if (sbomMetadata != null) {
          sbomMetadata.setOriginalBinaryFileName(originalFileNameProperty);
          thirdPartySbomMetadataDAO.update(sbomMetadata);
        }
      }
    }

    try (TransactionContext tx = thirdPartyFileDAO.createTransactionContext()) {
      tx.begin();
      targetBom.setMetadata(getFilteredMetadata(sourceBom));

      Map<String, String> componentRefs = new HashMap<>();
      processComponents(sourceBom, targetBom, componentRefs, thirdPartyIdentificationSource, thirdPartyFile, tx,
          isValid);
      if (isValid) {
        processVulnerabilities(sourceBom, targetBom, componentRefs, tx);
      }
      tx.commit();
    }
    if (isValid) {
      processDependencyGraph(sourceBom, targetBom, dependencyGraph, thirdPartyFile);
    }
  }

  private void processComponents(
      final Bom sourceBom,
      final Bom targetBom,
      final Map<String, String> componentRefs,
      final String thirdPartyIdentificationSource,
      final ThirdPartyFile thirdPartyFile,
      final TransactionContext tx,
      final boolean isValid)
  {
    if (CollectionUtils.isNotEmpty(sourceBom.getComponents())) {
      String specVersion = sourceBom.getSpecVersion();
      List<ComponentIdentifier> resolvedComponentsByPurl = new ArrayList<>();
      Set<String> resolvedComponentByHash = new HashSet<>();
      for (Component component : sourceBom.getComponents()) {
        processComponent(component, thirdPartyFile.getId(), targetBom, thirdPartyIdentificationSource,
            resolvedComponentsByPurl, resolvedComponentByHash, componentRefs, specVersion, tx, isValid);
      }
    }
  }

  private void processVulnerabilities(
      final Bom sourceBom,
      final Bom targetBom,
      final Map<String, String> componentRefs,
      final TransactionContext tx)
  {
    if (CollectionUtils.isNotEmpty(targetBom.getComponents()) &&
        CollectionUtils.isNotEmpty(sourceBom.getVulnerabilities()) && !componentRefs.isEmpty())
    {
      for (Vulnerability vulnerability : sourceBom.getVulnerabilities()) {
        try {
          List<Affect> affects = vulnerability.getAffects();
          for (Affect affect : affects) {
            if (StringUtils.isNotBlank(affect.getRef()) && componentRefs.containsKey(affect.getRef())) {
              String fileCoordinateId = componentRefs.get(affect.getRef());
              saveVulnerability(vulnerability, fileCoordinateId, tx);
            }
            else {
              log.debug("Vulnerability with ID {} does not have a {} so it can't be parsed", vulnerability.getId(),
                  StringUtils.isBlank(affect.getRef()) ? "ref" : "matching component");
            }
          }
        }
        catch (Exception e) {
          log.debug("There was an error parsing Vulnerability with ID {}", vulnerability.getId());
        }
      }
    }
  }

  private void saveVulnerability(
      final Vulnerability vulnerability,
      final String fileCoordinateId,
      final TransactionContext tx)
  {
    ThirdPartyCoordinateSecurity coordinateSecurity = parseVulnerability(vulnerability, fileCoordinateId);
    if (coordinateSecurity != null) {
      if (thirdPartyScanContext != null) {
        coordinateSecurity.setSbomMetadataId(thirdPartyScanContext.getSbomMetadataId());
      }
      coordinateSecurity = thirdPartyCoordinateSecurityDAO.insertSafely(tx, coordinateSecurity);
      vulnerabilityExploitabilityExchangeSave(vulnerability, coordinateSecurity, tx);
    }
  }

  private void vulnerabilityExploitabilityExchangeSave(
      Vulnerability vulnerability,
      ThirdPartyCoordinateSecurity coordinateSecurity,
      TransactionContext tx)
  {
    ThirdPartyVulnerabilityExploitabilityExchange vex =
        parseVulnerabilityExploitability(vulnerability, coordinateSecurity.getId());
    if (vex != null) {
      componentInfoTelemetry.incrementVulnerabilitiesWithVexInfoCount();
      thirdPartyVexDAO.saveOrUpdate(tx, vex);
    }
  }

  // visible for testing
  String determineThirdPartyIdentificationSource(final String contentPath) {
    String fileName =
        StringUtils.contains(contentPath, "/") ? StringUtils.substringAfterLast(contentPath, "/") : contentPath;
    String thirdPartyIdentificationSource = RegExUtils.removePattern(fileName,
        "(-(?i)bom\\.(xml|json)(?i)|\\.(?i)(cdx|spdx)\\.(xml|json)(?i))$");
    if (StringUtils.isBlank(thirdPartyIdentificationSource) ||
        StringUtils.endsWithIgnoreCase(thirdPartyIdentificationSource, "bom.xml") ||
        StringUtils.endsWithIgnoreCase(thirdPartyIdentificationSource, "bom.json") ||
        StringUtils.endsWithIgnoreCase(thirdPartyIdentificationSource, "cdx.xml") ||
        StringUtils.endsWithIgnoreCase(thirdPartyIdentificationSource, "cdx.json") ||
        StringUtils.endsWithIgnoreCase(thirdPartyIdentificationSource, "spdx.xml") ||
        StringUtils.endsWithIgnoreCase(thirdPartyIdentificationSource, "spdx.json"))
    {
      return "Third-Party";
    }
    else {
      return thirdPartyIdentificationSource;
    }
  }

  private void processComponent(
      final Component sourceComponent,
      final String thirdPartyFileId,
      final Bom targetBom,
      final String thirdPartyIdentificationSource,
      final List<ComponentIdentifier> resolvedComponents,
      final Set<String> resolvedComponentsByHash,
      final Map<String, String> componentRefs,
      final String schemaVersion,
      final TransactionContext tx,
      final boolean isValid)
  {
    try {
      Pair<ComponentIdentifier, Component> resolvedComponent = getResolvedComponent(sourceComponent);
      if (resolvedComponent != null) {
        String componentRef = SbomIdentityUtils.getComponentRef(sourceComponent);
        String sonatypeSha1 = SbomUtils.getSonatypeSha1FromProperties(sourceComponent);
        resolvedComponent.getRight()
            .addProperty(SbomExportUtils.createCycloneDxProperty(SbomCycloneDxUtils.PROPERTY_COMPONENT_REF,
                componentRef));
        ComponentIdentifier componentIdentifier = resolvedComponent.getLeft();
        if (componentIdentifier == null) {
          targetBom.addComponent(resolvedComponent.getRight());
          log.debug("Component filtered for matching only with hash information {}", resolvedComponent.getRight());
        }
        else if (!resolvedComponents.contains(componentIdentifier) ||
        // we only allow duplicate purls if they have different sonatype truncated sha1 hashes
            (StringUtils.isNotBlank(sonatypeSha1) && resolvedComponentsByHash.add(sonatypeSha1)))
        {
          resolvedComponents.add(componentIdentifier);
          PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).ensureCompleteIdentifier();
          String coordinateId =
              saveComponent(thirdPartyFileId, thirdPartyIdentificationSource, sourceComponent, componentRef,
                  resolvedComponent, schemaVersion, tx, isValid);
          if (StringUtils.isNotBlank(sourceComponent.getBomRef())) {
            componentRefs.put(sourceComponent.getBomRef(), coordinateId);
          }
          targetBom.addComponent(resolvedComponent.getRight());
        }
      }
      else {
        log.debug("Error processing component due to insufficient information: bom-ref {}, name {}, version {}",
            sourceComponent.getBomRef(), sourceComponent.getName(), sourceComponent.getVersion());
      }
    }
    catch (InvalidPackageURLException e) {
      log.debug("Component {} {} is missing coordinates. {}", sourceComponent.getName(), sourceComponent.getVersion(),
          e.getMessage().replace(" for given format", ""), e);
    }
    catch (Exception e) {
      log.debug("Error processing component due to insufficient information: bom-ref {}, name {}, version {}",
          sourceComponent.getBomRef(), sourceComponent.getName(), sourceComponent.getVersion(), e);
    }
  }

  private Pair<ComponentIdentifier, Component> getResolvedComponent(
      final Component sourceComponent) throws MalformedPackageURLException
  {
    String packageUrl = sourceComponent.getPurl();
    try {
      if (StringUtils.isNotBlank(packageUrl)) {
        PackageUrlIdentifier packageUrlIdentifier = resolvePackageUrl(packageUrl);
        if (packageUrlIdentifier != null &&
            StringUtils.isNoneBlank(packageUrlIdentifier.getName(), packageUrlIdentifier.getVersion()))
        {
          componentInfoTelemetry.incrementPurlCount();
          return createComponent(sourceComponent, packageUrlIdentifier);
        }
        else {
          log.debug("PackageUrl is not valid {}", packageUrl);
        }
      }
    }
    catch (InvalidPackageURLException e) {
      log.debug("Invalid purl: {}", packageUrl, e);
    }
    catch (InvalidComponentIdentifierException e) {
      log.debug("Invalid Component Identifier for provided purl {}", packageUrl, e);
    }

    String cpe = sourceComponent.getCpe();
    if (StringUtils.isNotBlank(cpe)) {
      PackageUrlIdentifier packageUrlIdentifier = SbomCommonUtils.getPackageUrlIdentifierFromCpe(cpe);
      if (packageUrlIdentifier != null &&
          StringUtils.isNoneBlank(packageUrlIdentifier.getName(), packageUrlIdentifier.getVersion()))
      {
        return createComponent(sourceComponent, packageUrlIdentifier);
      }
    }

    Swid swid = sourceComponent.getSwid();
    if (swid != null) {
      PackageUrlIdentifier packageUrlIdentifier = SbomIdentityUtils.buildPackageUrlFromSwid(swid);
      if (SbomIdentityUtils.packageUrlIdentifierHasMandatoryCoordinates(packageUrlIdentifier)) {
        componentInfoTelemetry.incrementSwidCount();
        return createComponent(sourceComponent, packageUrlIdentifier);
      }
    }

    return processComponentFromHashOrCoordinates(sourceComponent);
  }

  private Pair<ComponentIdentifier, Component> processComponentFromHashOrCoordinates(
      final Component sourceComponent) throws MalformedPackageURLException
  {
    String name =
        StringUtils.isNotBlank(sourceComponent.getName()) ? sourceComponent.getName() : MISSING_COMPONENT_NAME;
    String version = sourceComponent.getVersion();
    if (StringUtils.isNotBlank(version)) {
      PackageUrlIdentifier packageUrlIdentifier =
          resolvePackageUrl(getPackageUrlFromCoordinates(sourceComponent, name));
      componentInfoTelemetry.incrementCoordinateCount();
      return createComponent(sourceComponent, packageUrlIdentifier);
    }
    else {
      // This scenario is only possible when only the hash is sent without coordinates nor purl
      String sha1 = SbomUtils.getSha1(sourceComponent);
      if (StringUtils.isNotBlank(sha1)) {
        Component component = new Component();
        component.setName(name);
        component.setType(sourceComponent.getType());
        component.setBomRef(sourceComponent.getBomRef());
        setHashes(sourceComponent, component);
        componentInfoTelemetry.incrementHashCount();
        return Pair.of(null, component);
      }
      else {
        log.debug("Component with invalid information, name {} and version {}", sourceComponent.getName(),
            sourceComponent.getVersion());
      }
    }
    return null;
  }

  private String getPackageUrlFromCoordinates(Component component, String name) throws MalformedPackageURLException {
    String group = component.getGroup();
    String publisher = component.getPublisher();

    PackageURLBuilder packageURLBuilder = PackageURLBuilder.aPackageURL()
        .withType(PackageUrlIdentifier.GENERIC_FORMAT)
        .withName(name)
        .withVersion(component.getVersion());
    if (StringUtils.isNotBlank(group)) {
      packageURLBuilder.withNamespace(group);
    }
    if (StringUtils.isNotBlank(publisher)) {
      packageURLBuilder.withQualifier("publisher", publisher);
    }

    packageURLBuilder.withQualifier(PURL_BOM_TYPE, component.getType().getTypeName());

    return packageURLBuilder.build().toString();
  }

  private Pair<ComponentIdentifier, Component> createComponent(
      final Component sourceComponent,
      final PackageUrlIdentifier packageUrlIdentifier)
  {
    ComponentIdentifier componentIdentifier;
    Component component = new Component();

    component.setType(sourceComponent.getType());
    component.setBomRef(sourceComponent.getBomRef());
    SbomCycloneDxUtils.addSonatypeOriginalPurl(sourceComponent.getPurl(), component);

    String cpe = sourceComponent.getCpe();
    if (StringUtils.isNotBlank(cpe)) {
      if (Validate.cpe(cpe).isValid()) {
        component.setCpe(sourceComponent.getCpe());
        componentInfoTelemetry.incrementCpeCount();
      }
      else {
        log.debug("Skipping invalid CPE {} for component with name {}, it's invalid", cpe,
            sourceComponent.getName());
      }
    }

    component.setSwid(sourceComponent.getSwid());

    setHashes(sourceComponent, component);

    componentIdentifier = SbomCommonUtils.getComponentIdentifier(packageUrlIdentifier, component);

    // Process sha-256 only when BFS is enabled
    if (SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.isEnabled()) {
      Hash sha256 = SbomUtils.getSha256(sourceComponent);
      if (sha256 != null) {
        component.addHash(sha256);
      }
    }

    return Pair.of(componentIdentifier, component);
  }

  private void setHashes(final Component sourceComponent, final Component newComponent) {
    String sha1 = SbomUtils.getSha1(sourceComponent);
    if (StringUtils.isNotBlank(sha1)) {
      setSha1Property(sha1, newComponent);
    }
    if (CollectionUtils.isNotEmpty(sourceComponent.getHashes())) {
      newComponent.setHashes(sourceComponent.getHashes());
    }
  }

  protected void setSha1Property(final String sha1, final Component component) {
    Property property = new Property();
    property.setName(SbomTaxonomy.CDX_SONATYPE_SHA1_PROPERTY_NAME);
    property.setValue(StringUtils.truncate(sha1, 0, HashHelper.MAX_LENGTH));
    component.addProperty(property);
  }

  protected PackageUrlIdentifier resolvePackageUrl(String packageUrlIdentifier) {
    try {
      PackageUrlIdentifier sourcePurl = new PackageUrlIdentifier(packageUrlIdentifier);

      PackageURLBuilder packageURLBuilder = PackageURLBuilder.aPackageURL();
      packageURLBuilder.withType(ThirdPartyScanResultUtils.getValidFormat(sourcePurl.getFormat()));
      packageURLBuilder.withName(ThirdPartyScanResultUtils.getTruncatedName(sourcePurl.getName()));
      packageURLBuilder.withVersion(ThirdPartyScanResultUtils.getTruncatedVersion(sourcePurl.getVersion()));

      if (sourcePurl.getNamespace() != null) {
        packageURLBuilder.withNamespace(sourcePurl.getNamespace());
      }

      Map<String, String> qualifiers = sourcePurl.getQualifiers();

      if (sourcePurl.getFormat().equalsIgnoreCase(ComponentIdentifier.FORMAT_MAVEN) &&
          StringUtils.isBlank(qualifiers.get(PackageUrlIdentifier.PURL_MAVEN_EXTENSION)))
      {
        qualifiers.put(PackageUrlIdentifier.PURL_MAVEN_EXTENSION, "jar");
      }

      for (Entry<String, String> entry : qualifiers.entrySet()) {
        packageURLBuilder.withQualifier(entry.getKey(), entry.getValue());
      }

      PackageURL packageUrl = packageURLBuilder.build();
      return new PackageUrlIdentifier(packageUrl.canonicalize());
    }
    catch (MalformedPackageURLException e) {
      throw new InvalidPackageURLException(e.getMessage(), e);
    }
  }

  private String saveComponent(
      final String thirdPartyFileId,
      final String thirdPartyIdentificationSource,
      final Component sourceComponent,
      final String componentRef,
      final Pair<ComponentIdentifier, Component> resolvedComponent,
      final String schemaVersion,
      final TransactionContext tx,
      final boolean isValid) throws JsonProcessingException
  {
    Component component = resolvedComponent.getRight();
    ComponentIdentifier componentIdentifier;
    if (component.getPurl() != null) {
      PackageUrlIdentifier packageUrlIdentifier = resolvePackageUrl(component.getPurl());
      componentIdentifier = packageUrlIdentifier.toComponentIdentifier();
    }
    else {
      componentIdentifier = resolvedComponent.getLeft();
    }

    String hash = getOrCreateFakeHash(component, componentIdentifier);
    ThirdPartyFileCoordinate fileCoordinate = new ThirdPartyFileCoordinate(
        StringUtils.truncate(hash, 0, HashHelper.MAX_LENGTH), thirdPartyIdentificationSource,
        componentIdentifier.getFormat(), component.getName(), component.getVersion(), thirdPartyFileId);
    fileCoordinate.setPackageUrl(component.getPurl());
    fileCoordinate.setCpe(component.getCpe());
    fileCoordinate.setComponentRef(componentRef);
    Swid swid = component.getSwid();
    if (swid != null) {
      fileCoordinate.setSwid(ThirdPartyComponentDAO.MAPPER.writeValueAsString(swid));
    }
    fileCoordinate.setIdentificationSources(SbomMetadataUtils.SBOM_IDENTIFICATION_SOURCE);
    if (sourceComponent.getEvidence() != null &&
        CollectionUtils.isNotEmpty(sourceComponent.getEvidence().getOccurrences()))
    {
      fileCoordinate.setOccurrencesList(
          sourceComponent.getEvidence().getOccurrences().stream().map(Occurrence::getLocation).toList());
    }
    if (CollectionUtils.isNotEmpty(sourceComponent.getProperties())) {
      fileCoordinate.setFilenames(getComponentPropertyAsString(sourceComponent,
          SbomTaxonomy.CDX_MATCH_FILENAMES_PROPERTY_NAME));
      fileCoordinate.setMatchStateId(getComponentPropertyAsString(sourceComponent,
          SbomTaxonomy.CDX_MATCH_STATE_PROPERTY_NAME));
      if (StringUtils.isEmpty(fileCoordinate.getMatchStateId())) {
        // Fallback to legacy property
        fileCoordinate.setMatchStateId(
            getComponentPropertyAsString(sourceComponent, SbomTaxonomy.LEGACY_MATCH_STATE_PROPERTY_NAME));
      }
    }
    componentInfoTelemetry.incrementEcosystemCount(fileCoordinate.getFormat());
    fileCoordinate = fileCoordinatePersister.persist(tx, fileCoordinate);
    if (isValid) {
      saveLicenses(sourceComponent.getLicenses(), fileCoordinate.getId(), component.getPurl(), tx);
      saveVulnerabilitiesExtension(sourceComponent.getExtensions(), fileCoordinate.getId(), schemaVersion, tx);
    }
    return fileCoordinate.getId();
  }

  private String getComponentPropertyAsString(Component component, String propertyName) {
    return component.getProperties()
        .stream()
        .filter(property -> propertyName.equals(property.getName()))
        .map(Property::getValue)
        .findFirst()
        .orElse(null);
  }

  protected String getOrCreateFakeHash(Component component, ComponentIdentifier componentIdentifier) {
    String sha1 = SbomUtils.getSha1(component);
    if (StringUtils.isEmpty(sha1)) {
      // generate a hash from the component identifier
      String coordinateString =
          componentIdentifier.getFormat() + ":" + StringUtils.join(componentIdentifier.getCoordinates().values(), ":");
      sha1 = ThirdPartyScanResultUtils.hash(coordinateString);
    }
    return sha1;
  }

  private void saveVulnerabilitiesExtension(
      final Map<String, Extension> extensions,
      final String fileCoordinateId,
      final String bomVersion,
      final TransactionContext tx)
  {
    // Vulnerability extension is unsupported from 1.4+
    if (MapUtils.isNotEmpty(extensions) &&
        (!Version.VERSION_14.getVersionString().equals(bomVersion) || StringUtils.isBlank(bomVersion)))
    {
      Extension vulnerabilityExtension = extensions.get(SbomCycloneDxUtils.VULNERABILITY_KEY);
      if (vulnerabilityExtension != null && CollectionUtils.isNotEmpty(vulnerabilityExtension.getExtensions())) {
        Set<String> vulnerabilityMap = new HashSet<>();
        for (ExtensibleType extensibleType : vulnerabilityExtension.getExtensions()) {
          processVulnerabilityExtension(extensibleType, fileCoordinateId, vulnerabilityMap, tx);
        }
      }
    }
  }

  private void processVulnerabilityExtension(
      final ExtensibleType extensibleType,
      final String fileCoordinateId,
      final Set<String> vulnerabilityMap,
      final TransactionContext tx)
  {
    if (extensibleType instanceof Vulnerability10 vulnerability) {
      String refId = vulnerability.getId();
      if (StringUtils.isNotBlank(refId) && !vulnerabilityMap.contains(refId)) {
        saveVulnerabilityExtension(vulnerability, fileCoordinateId, tx);
        vulnerabilityMap.add(refId);
      }
    }
  }

  private void saveVulnerabilityExtension(
      final Vulnerability10 vulnerability,
      final String fileCoordinateId,
      final TransactionContext tx)
  {
    ThirdPartyCoordinateSecurity coordinateSecurity = parseVulnerabilityExtension(vulnerability, fileCoordinateId);
    if (coordinateSecurity != null) {
      if (thirdPartyScanContext != null) {
        coordinateSecurity.setSbomMetadataId(thirdPartyScanContext.getSbomMetadataId());
      }
      coordinateSecurity.setIdentificationSources(IdentificationSource.SBOM.getId());
      thirdPartyCoordinateSecurityDAO.insertSafely(tx, coordinateSecurity);
    }
  }

  // Visible for testing
  ThirdPartyCoordinateSecurity parseVulnerabilityExtension(
      final Vulnerability10 vulnerability,
      final String fileCoordinateId)
  {
    List<Rating> ratingsElements = vulnerability.getRatings();
    if (CollectionUtils.isNotEmpty(ratingsElements)) {
      ThirdPartyCoordinateSecurity coordinateSecurity = new ThirdPartyCoordinateSecurity();
      Rating rating = ratingsElements.get(0);
      Double baseScore = getBaseScore(rating);
      if (baseScore != null) {
        coordinateSecurity.setSeverity(baseScore);
        if (rating.getVector() != null) {
          coordinateSecurity.setAttackVector(getTruncatedAttackVector(rating.getVector()));
        }
        if (rating.getMethod() != null) {
          coordinateSecurity.setRatingMethod(getTruncatedRatingMethod(rating.getMethod().name()));
        }
        if (rating.getSeverity() != null) {
          coordinateSecurity
              .setSeverityDescription(getTruncatedSeverityDescription(rating.getSeverity().getSeverityName()));
        }
        coordinateSecurity.setFileCoordinateId(fileCoordinateId);
        if (vulnerability.getCwes() != null) {
          coordinateSecurity.setCwes(
              vulnerability.getCwes()
                  .stream()
                  .filter(cwe -> cwe.getText() != null)
                  .map(cwe -> cwe.getText().toString())
                  .collect(Collectors.joining(ThirdPartyVulnerabilityDataAdapter.LIST_SEPARATOR)));
        }
        if (vulnerability.getRecommendations() != null) {
          coordinateSecurity.setRecommendations(
              vulnerability.getRecommendations()
                  .stream()
                  .map(r -> StringUtils.normalizeSpace(r.getText()))
                  .collect(Collectors.joining(ThirdPartyVulnerabilityDataAdapter.LIST_SEPARATOR)));
        }
        if (vulnerability.getAdvisories() != null) {
          coordinateSecurity.setAdvisories(
              vulnerability.getAdvisories()
                  .stream()
                  .map(Advisory::getText)
                  .collect(Collectors.joining(ThirdPartyVulnerabilityDataAdapter.LIST_SEPARATOR)));
        }
        Source source = vulnerability.getSource();
        if (source != null) {
          coordinateSecurity.setVulnerabilitySource(getTruncatedVulnerabilitySource(source.getName()));
          if (source.getUrl() != null) {
            coordinateSecurity.setLink(getTruncatedLink(source.getUrl().toString()));
          }
        }
        coordinateSecurity.setRefId(getTruncatedRefId(vulnerability.getId()));
        coordinateSecurity.setDescription(StringUtils.normalizeSpace(vulnerability.getDescription()));
        return coordinateSecurity;
      }
    }
    return null;
  }

  @VisibleForTesting
  ThirdPartyCoordinateSecurity parseVulnerability(
      final Vulnerability vulnerability,
      final String fileCoordinateId)
  {
    List<Vulnerability.Rating> ratingsElements = vulnerability.getRatings();
    if (CollectionUtils.isNotEmpty(ratingsElements)) {
      ThirdPartyCoordinateSecurity coordinateSecurity = new ThirdPartyCoordinateSecurity();

      Vulnerability.Rating rating = getValidRating(ratingsElements);
      if (rating == null) {
        rating = ratingsElements.get(0);
      }

      if (rating != null) {
        Double baseScore = rating.getScore();
        if (baseScore != null) {
          coordinateSecurity.setSeverity(baseScore);
          if (rating.getVector() != null) {
            coordinateSecurity.setAttackVector(getTruncatedAttackVector(rating.getVector()));
          }
          if (rating.getMethod() != null) {
            coordinateSecurity.setRatingMethod(getTruncatedRatingMethod(rating.getMethod().getMethodName()));
          }
          if (rating.getSeverity() != null) {
            coordinateSecurity
                .setSeverityDescription(getTruncatedSeverityDescription(rating.getSeverity().getSeverityName()));
          }
          coordinateSecurity.setFileCoordinateId(fileCoordinateId);
          if (vulnerability.getCwes() != null) {
            coordinateSecurity.setCwes(vulnerability.getCwes()
                .stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.joining(ThirdPartyVulnerabilityDataAdapter.LIST_SEPARATOR)));
          }
          if (vulnerability.getRecommendation() != null) {
            coordinateSecurity.setRecommendations(StringUtils.normalizeSpace(vulnerability.getRecommendation()));
          }
          if (vulnerability.getAdvisories() != null) {
            String advisory = vulnerability.getAdvisories()
                .stream()
                .map(adv -> adv.getTitle() + ThirdPartyVulnerabilityDataAdapter.ADVISORY_SEPARATOR + adv.getUrl())
                .collect(Collectors.joining(ThirdPartyVulnerabilityDataAdapter.LIST_SEPARATOR));
            coordinateSecurity.setAdvisories(advisory);
          }
          Vulnerability.Source source = vulnerability.getSource();
          if (source != null) {
            coordinateSecurity.setVulnerabilitySource(getTruncatedVulnerabilitySource(source.getName()));
            if (source.getUrl() != null) {
              coordinateSecurity.setLink(getTruncatedLink(source.getUrl()));
            }
          }
          coordinateSecurity.setRefId(getTruncatedRefId(vulnerability.getId()));
          coordinateSecurity.setResearchType(getResearchTypeForThirdPartyVulnerability(
              coordinateSecurity.getVulnerabilitySource(), coordinateSecurity.getRefId()));
          coordinateSecurity.setDetectionType(OTHER.getId());
          coordinateSecurity.setDescription(StringUtils.normalizeSpace(vulnerability.getDescription()));
          coordinateSecurity.setIdentificationSources(IdentificationSource.SBOM.getId());
          return coordinateSecurity;
        }
      }
    }
    else {
      log.debug("Vulnerability with ID {} does not have a valid rating, it can't be parsed", vulnerability.getId());
    }
    return null;
  }

  @VisibleForTesting
  ThirdPartyVulnerabilityExploitabilityExchange parseVulnerabilityExploitability(
      final Vulnerability vulnerability,
      final String coordinateSecurityId)
  {

    Vulnerability.Analysis analysis = vulnerability.getAnalysis();
    ThirdPartyVulnerabilityExploitabilityExchange vex = null;

    if (analysis != null) {
      vex = new ThirdPartyVulnerabilityExploitabilityExchange();

      Optional.ofNullable(analysis.getState())
          .map(Vulnerability.Analysis.State::getStateName)
          .ifPresent(vex::setState);

      Optional.ofNullable(analysis.getJustification())
          .map(Vulnerability.Analysis.Justification::getJustificationName)
          .ifPresent(vex::setJustification);

      String responses = Optional.ofNullable(analysis.getResponses())
          .orElseGet(Collections::emptyList)
          .stream()
          .map(Vulnerability.Analysis.Response::getResponseName)
          .collect(Collectors.joining(ThirdPartyVulnerabilityDataAdapter.LIST_SEPARATOR));
      vex.setResponse(responses);

      Optional.ofNullable(analysis.getDetail()).ifPresent(vex::setDetail);

      vex.setRefId(vulnerability.getId());
      vex.setCoordinateSecurityId(coordinateSecurityId);
    }

    return vex;
  }

  Vulnerability.Rating getValidRating(List<Vulnerability.Rating> ratings) {
    Vulnerability.Rating validRating = null;
    for (Vulnerability.Rating rating : ratings) {
      if (rating.getScore() != null) {
        Vulnerability.Source source = rating.getSource();
        if (source != null && StringUtils.isNotBlank(source.getName())) {
          validRating = rating;
          if (source.getName().toLowerCase(Locale.ROOT).equals("nvd") &&
              (rating.getMethod() == Method.CVSSV31 || rating.getMethod() == Method.CVSSV3 ||
                  rating.getMethod() == Method.CVSSV4))
          {
            break;
          }
        }
      }
    }
    return validRating;
  }

  private Double getBaseScore(final Rating rating) {
    if (rating.getScore() != null) {
      Double scoreBase = rating.getScore().getBase();
      if (scoreBase != null && scoreBase > 0) {
        return scoreBase;
      }
    }
    return null;
  }

  private void saveLicenses(
      LicenseChoice licenseChoice,
      String fileCoordinateId,
      String packageUrl,
      TransactionContext tx)
  {
    if (licenseChoice == null) {
      log.debug("No licenses provided for Component with packageUrl {}", packageUrl);
    }
    else {
      Set<String> processedLicenseIds = new HashSet<>();
      if (CollectionUtils.isEmpty(licenseChoice.getLicenses())) {
        log.debug("Found empty licenses element for Component with packageUrl {}", packageUrl);
      }
      else {
        for (License license : licenseChoice.getLicenses()) {
          String licenseId = license.getId();
          String licenseName = license.getName();
          MultiLicense sonatypeLicense = getSonatypeLicense(license);
          if (sonatypeLicense != null) {
            licenseId = sonatypeLicense.getId();
            licenseName = sonatypeLicense.getShortDisplayName();
          }
          if (StringUtils.isNotBlank(licenseId)) {
            if (processedLicenseIds.add(licenseId)) {
              saveLicense(licenseId, licenseName, license.getUrl(), fileCoordinateId, tx);
            }
            else {
              log.debug("Component with packageUrl {} has duplicate license with ID {}", packageUrl, licenseId);
            }
          }
          else {
            log.debug("Component with packageUrl {} has license with null/empty ID", packageUrl);
          }
        }
      }

      // Process license expressions
      Expression expression = licenseChoice.getExpression();
      if (expression != null && StringUtils.isNotEmpty(expression.getValue())) {
        AnyLicenseInfo anyLicenseInfo;
        try {
          anyLicenseInfo = LicenseInfoFactory.parseSPDXLicenseStringCompatV2(expression.getValue());
        }
        catch (InvalidLicenseStringException | org.spdx.core.DefaultStoreNotInitializedException e) {
          componentInfoTelemetry.incrementInvalidLicensesCount();
          log.debug("Failed to parse spdx license string: {} for: {}.", expression, packageUrl);
          return;
        }
        HashMap<String, String> processedLicenses = new HashMap<>();
        try {
          spdxLicenseExpressionUtil.parseLicenses(anyLicenseInfo, processedLicenses, packageUrl);
        }
        catch (InvalidSPDXAnalysisException e) {
          componentInfoTelemetry.incrementInvalidLicensesCount();
          throw new RuntimeException(e);
        }
        componentInfoTelemetry.incrementValidLicensesCount();
        for (Entry<String, String> licenseEntry : processedLicenses.entrySet()) {
          ThirdPartyCoordinateLicense coordinateLicense =
              new ThirdPartyCoordinateLicense(fileCoordinateId, licenseEntry.getKey(), licenseEntry.getValue(), null);
          coordinateLicense.setIdentificationSources(IdentificationSource.SBOM.getId());
          thirdPartyCoordinateLicenseDAO.insertSafely(tx, coordinateLicense);
        }
      }
    }
  }

  private MultiLicense getSonatypeLicense(License license) {
    if (StringUtils.isNotBlank(license.getId())) {
      return multiLicenseDAO.getByIdNoReload(license.getId());
    }
    if (StringUtils.isNotBlank(license.getName())) {
      return multiLicenseDAO.getByNameNoReload(license.getName());
    }
    return null;
  }

  private void saveLicense(
      String licenseId,
      String licenseName,
      String licenseUrl,
      String fileCoordinateId,
      TransactionContext tx)
  {
    ThirdPartyCoordinateLicense coordinateLicense = new ThirdPartyCoordinateLicense();
    coordinateLicense.setFileCoordinateId(fileCoordinateId);
    coordinateLicense.setLicenseId(licenseId);
    coordinateLicense.setName(licenseName);
    coordinateLicense.setUrl(licenseUrl);
    coordinateLicense.setIdentificationSources(IdentificationSource.SBOM.getId());
    thirdPartyCoordinateLicenseDAO.insertSafely(tx, coordinateLicense);
  }

  // visible for testing
  void processDependencyGraph(
      final Bom sourceBom,
      final Bom targetBom,
      final List<ProjectScanItem> moduleDependencies,
      final ThirdPartyFile thirdPartyFile)
  {
    try {
      if (CollectionUtils.isNotEmpty(targetBom.getComponents())) {
        List<Dependency> bomDependencies = sourceBom.getDependencies();
        if (CollectionUtils.isNotEmpty(bomDependencies)) {
          Pair<Dependency, String> rootModuleAndRef = resolveRootModuleAndRef(bomDependencies, targetBom);
          if (rootModuleAndRef != null) {
            processValidDependencyGraph(rootModuleAndRef, thirdPartyFile, targetBom, moduleDependencies,
                bomDependencies);
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

  protected void processValidDependencyGraph(
      final Pair<Dependency, String> rootModuleAndRef,
      final ThirdPartyFile thirdPartyFile,
      final Bom targetBom,
      final List<ProjectScanItem> moduleDependencies,
      final List<Dependency> bomDependencies)
  {
    ProjectScanItem project = new ProjectScanItem("sbom", rootModuleAndRef.getRight());
    project.setPath(thirdPartyFile.getFilename());
    Dependency rootDependency = rootModuleAndRef.getLeft();

    if (CollectionUtils.isNotEmpty(rootDependency.getDependencies())) {
      Map<String, String> bomRefsToPurls = new HashMap<>();

      Map<String, Pair<Boolean, List<com.sonatype.insight.scan.model.Dependency>>> directDeps =
          getValidDirectDependencies(rootDependency, targetBom, bomRefsToPurls);

      resolveSbomDependenciesAndTypes(rootDependency, thirdPartyFile, directDeps, bomRefsToPurls, targetBom,
          bomDependencies);
      constructProjectDependencyGraph(directDeps, project);
      moduleDependencies.add(project);
    }
  }

  protected Map<String, Pair<Boolean, List<com.sonatype.insight.scan.model.Dependency>>> getValidDirectDependencies(
      final Dependency rootDependency,
      final Bom targetBom,
      final Map<String, String> bomRefsToPurls)
  {
    Map<String, Pair<Boolean, List<com.sonatype.insight.scan.model.Dependency>>> directDeps =
        new HashMap<>();

    for (Dependency dependency : rootDependency.getDependencies()) {
      // only dependencies that has a component with a valid purl are loaded
      String purl = getPurlForDependency(dependency, bomRefsToPurls, targetBom);
      if (purl != null) {
        directDeps.put(purl, Pair.of(true, new ArrayList<>()));
      }
    }
    return directDeps;
  }

  protected Pair<Dependency, String> resolveRootModuleAndRef(
      final List<Dependency> dependencies,
      final Bom targetBom)
  {
    // Check we have the metadata component to compare
    if (targetBom.getMetadata() == null) {
      return null;
    }
    Component metadataComponent = targetBom.getMetadata().getComponent();
    if (metadataComponent == null) {
      return null;
    }

    for (Dependency dependency : dependencies) {
      // Check we have the dep ref to compare
      if (StringUtils.isBlank(dependency.getRef())) {
        continue;
      }
      // If the dep ref matches the metadata component purl just return the latter
      if (dependency.getRef().equalsIgnoreCase(metadataComponent.getPurl())) {
        return Pair.of(dependency, resolvePackageUrl(metadataComponent.getPurl()).getPackageUrl());
      }
      // Otherwise, if the dep ref matches the metadata component bom ref (it could be a purl or a UUID)
      if (dependency.getRef().equalsIgnoreCase(metadataComponent.getBomRef())) {
        // Return the metadata component purl if it exists
        if (StringUtils.isNotBlank(metadataComponent.getPurl())) {
          return Pair.of(dependency, resolvePackageUrl(metadataComponent.getPurl()).getPackageUrl());
        }
        // Otherwise, just return the dep ref
        if (isPurl(dependency.getRef())) {
          return Pair.of(dependency, resolvePackageUrl(dependency.getRef()).getPackageUrl());
        }
        return Pair.of(dependency, dependency.getRef());
      }
    }
    return null;
  }

  private String getPurlForDependency(
      final Dependency dependency,
      final Map<String, String> bomRefsToPurls,
      final Bom targetBom)
  {
    String ref = dependency.getRef();
    if (isPurl(ref)) {
      return resolvePackageUrl(ref).getPackageUrl();
    }

    if (bomRefsToPurls.isEmpty()) {
      populateComponentPurlsWithBomRef(targetBom.getComponents(), bomRefsToPurls);
    }
    return bomRefsToPurls.get(ref);
  }

  private void populateComponentPurlsWithBomRef(
      final List<Component> components,
      final Map<String, String> bomRefPurlMap)
  {
    for (Component component : components) {
      if (StringUtils.isNoneBlank(component.getBomRef(), component.getPurl()) && !isPurl(component.getBomRef())) {
        bomRefPurlMap.put(component.getBomRef(), resolvePackageUrl(component.getPurl()).getPackageUrl());

        if (component.getComponents() != null) {
          populateComponentPurlsWithBomRef(component.getComponents(), bomRefPurlMap);
        }
      }
    }
  }

  private Metadata getFilteredMetadata(final Bom sourceBom) {
    // making sure we copy only identity data and nothing else
    Metadata filtered = null;
    Metadata metadata = sourceBom.getMetadata();
    if (metadata != null && metadata.getComponent() != null) {
      filtered = new Metadata();
      filtered.setTimestamp(metadata.getTimestamp());
      Component component = new Component();
      component.setType(metadata.getComponent().getType());
      component.setBomRef(metadata.getComponent().getBomRef());
      component.setName(metadata.getComponent().getName());
      component.setGroup(metadata.getComponent().getGroup());
      component.setVersion(metadata.getComponent().getVersion());
      setRootPurl(metadata.getComponent(), component);
      filtered.setComponent(component);
    }
    return filtered;
  }

  private void setRootPurl(Component componentSource, Component componentTarget) {
    if (StringUtils.isNotBlank(componentSource.getPurl())) {
      componentTarget.setPurl(componentSource.getPurl());
    }
    else if (StringUtils.isNoneBlank(componentSource.getName(), componentSource.getVersion())) {
      try {
        PackageURLBuilder builder = PackageURLBuilder.aPackageURL();
        builder.withType(ComponentIdentifier.FORMAT_GENERIC)
            .withName(componentSource.getName())
            .withVersion(componentSource.getVersion());
        if (StringUtils.isNotBlank(componentSource.getGroup())) {
          builder.withNamespace(componentSource.getGroup());
        }
        componentTarget.setPurl(builder.build().canonicalize());
      }
      catch (Exception e) {
        log.debug("Error building generic purl from metadata", e);
      }
    }
  }

  private void constructProjectDependencyGraph(
      final Map<String, Pair<Boolean, List<com.sonatype.insight.scan.model.Dependency>>> dependencyGraph,
      final ProjectScanItem project)
  {
    for (Entry<String, Pair<Boolean, List<com.sonatype.insight.scan.model.Dependency>>> depEntry : dependencyGraph
        .entrySet())
    {
      com.sonatype.insight.scan.model.Dependency dependency = new com.sonatype.insight.scan.model.Dependency();
      dependency.setId(depEntry.getKey());
      dependency.setDirect(depEntry.getValue().getLeft());
      depEntry.getValue().getRight().forEach(dependency::addDependency);
      project.addDependency(dependency);
    }
  }

  private void resolveSbomDependenciesAndTypes(
      final Dependency rootDependency,
      final ThirdPartyFile thirdPartyFile,
      final Map<String, Pair<Boolean, List<com.sonatype.insight.scan.model.Dependency>>> dependencyGraph,
      final Map<String, String> bomRefPurlMap,
      final Bom targetBom,
      final List<Dependency> bomDependencies)
  {
    Map<String, Dependency> bomDependenciesMap = getFullDependencyMap(bomRefPurlMap, bomDependencies, targetBom);

    bomDependencies.iterator().forEachRemaining(bomDependency -> {
      if (bomDependency == rootDependency) {
        return;
      }

      String ref = getPurlForDependency(bomDependency, bomRefPurlMap, targetBom);
      Pair<Boolean, List<com.sonatype.insight.scan.model.Dependency>> dependencyPair =
          getProjectDependencyForRef(ref, dependencyGraph, bomDependenciesMap);
      if (dependencyPair != null) {
        copyChildDependenciesForDependency(thirdPartyFile, dependencyGraph, bomDependency, dependencyPair,
            bomRefPurlMap, targetBom);
      }
      else {
        log.debug("Unsupported dependency graph in sbom. Missing parent reference for the child dependency {}",
            bomDependency.getRef());
      }
    });
  }

  private Map<String, Dependency> getFullDependencyMap(
      final Map<String, String> bomRefPurlMap,
      final List<Dependency> bomDependencies,
      final Bom targetBom)
  {
    return bomDependencies.stream()
        .map(dep -> new AbstractMap.SimpleEntry<>(getPurlForDependency(dep, bomRefPurlMap, targetBom), dep))
        .filter(entry -> entry.getKey() != null)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (d1, d2) -> d1));
  }

  private Pair<Boolean, List<com.sonatype.insight.scan.model.Dependency>> getProjectDependencyForRef(
      final String ref,
      final Map<String, Pair<Boolean, List<com.sonatype.insight.scan.model.Dependency>>> dependencyGraph,
      final Map<String, Dependency> fullDependencyMap)
  {
    Pair<Boolean, List<com.sonatype.insight.scan.model.Dependency>> dependencyPair;
    dependencyPair = dependencyGraph.get(ref);
    if (dependencyPair == null && fullDependencyMap.containsKey(ref)) {
      dependencyPair = Pair.of(false, new ArrayList<>());
      dependencyGraph.put(ref, dependencyPair);
    }
    return dependencyPair;
  }

  private void copyChildDependenciesForDependency(
      final ThirdPartyFile thirdPartyFile,
      final Map<String, Pair<Boolean, List<com.sonatype.insight.scan.model.Dependency>>> dependencyGraph,
      final Dependency bomDependency,
      final Pair<Boolean, List<com.sonatype.insight.scan.model.Dependency>> dependencyPair,
      final Map<String, String> bomRefsToPurls,
      final Bom targetBom)
  {
    IterableUtils.forEach(bomDependency.getDependencies(), bomChild -> {
      com.sonatype.insight.scan.model.Dependency child = new com.sonatype.insight.scan.model.Dependency();
      if (StringUtils.isNotBlank(bomChild.getRef())) {

        String ref = getPurlForDependency(bomChild, bomRefsToPurls, targetBom);
        if (ref != null) {
          child.setId(ref);
          dependencyPair.getValue().add(child);
          if (!dependencyGraph.containsKey(ref)) {
            dependencyGraph.put(ref, Pair.of(false, new ArrayList<>()));
          }
        }
      }
      else {
        log.debug("invalid purl dependency {} in bom {}", bomChild.getRef(), thirdPartyFile.getFilename());
      }
    });
  }

  private boolean isPurl(final String ref) {
    try {
      return new PackageUrlIdentifier(ref).getPackageUrl() != null;
    }
    catch (InvalidPackageURLException e) {
      return false;
    }
  }

  String generateFilteredSbom(Bom sbom) {
    String defaultVersionString = SbomExportParams.ExportSpecification.DEFAULT.getVersion();
    Optional<Version> defaultVersionOptional = SbomCycloneDxUtils.getVersionFromString(defaultVersionString);
    Version defaultVersion = defaultVersionOptional.orElse(Version.VERSION_16);
    try {
      return BomGeneratorFactory.createJson(defaultVersion, sbom).toJsonString();
    }
    catch (GeneratorException e) {
      throw new SbomExportException("An error occurred while trying to parse the SBOM's content to JSON string", e);
    }
  }

  private String getPropertyAsString(List<Property> listProperties, String propertyName) {
    if (listProperties != null && !listProperties.isEmpty()) {
      return listProperties.stream()
          .filter(property -> propertyName.equals(property.getName()))
          .map(Property::getValue)
          .findFirst()
          .orElse(null);
    }
    else {
      return null;
    }
  }
}
