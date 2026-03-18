/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.SbomIdentityUtils;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.report.pdf.PdfData;
import com.sonatype.insight.brain.sbom.license.ThirdPartyComponentLicenseResolutionService;
import com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils;
import com.sonatype.insight.brain.sbom.utils.SbomSpdxUtils;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.thirdparty.ThirdPartyPersistenceService;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.util.HashUtils;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.exception.GeneratorException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Component.Type;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.Hash;
import org.cyclonedx.model.License;
import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.vulnerability.Vulnerability;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.model.ExternalRef;
import org.spdx.library.model.Relationship;
import org.spdx.library.model.SpdxDocument;
import org.spdx.library.model.SpdxPackage;
import org.spdx.library.model.enumerations.ChecksumAlgorithm;
import org.spdx.library.model.enumerations.RelationshipType;

import static com.sonatype.insight.brain.sbom.utils.SbomSpdxUtils.getChecksum;
import static com.sonatype.insight.brain.sbom.utils.SbomSpdxUtils.getCpe;
import static com.sonatype.insight.brain.sbom.utils.SbomSpdxUtils.getPackageUrlFromCoordinates;
import static com.sonatype.insight.brain.sbom.utils.SbomSpdxUtils.getPurl;
import static com.sonatype.insight.brain.sbom.utils.SbomSpdxUtils.getSwid;
import static com.sonatype.insight.brain.sbom.utils.SbomSpdxUtils.getVulnerabilitiesForPackage;

@Named
public class SpdxToCycloneDxExporter
    extends AbstractCycloneDxExporter
{
  private static final String MISSING_COMPONENT_NAME = "[Not Provided]";

  private Map<String, String> spdxPackageIdsToCdxBomRefs;

  protected Map<String, Component> componentRefToComponent = new HashMap<>();

  @Inject
  protected SpdxToCycloneDxExporter(
      final MultiLicenseDAO multiLicenseDAO,
      final ThirdPartyFileDAO thirdPartyFileDAO,
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO,
      final ThirdPartyScanDAO thirdPartyScanDAO,
      final ApplicationDAO applicationDAO,
      final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO,
      final MigrationTrackerDAO migrationTrackerDAO,
      final BaseUrl baseUrl,
      final IdUtils idUtils,
      final VersionService versionService,
      final ApiReportDataServiceV2 apiReportDataServiceV2,
      final ThirdPartyComponentLicenseResolutionService licenseResolutionService,
      final ThirdPartyPersistenceService thirdPartyPersistenceService)
  {
    super(
        multiLicenseDAO,
        thirdPartyFileDAO,
        thirdPartyFileCoordinateDAO,
        thirdPartyCoordinateSecurityDAO,
        thirdPartyCoordinateLicenseDAO,
        thirdPartyScanDAO,
        applicationDAO,
        thirdPartyVulnerabilityExploitabilityExchangeDAO,
        migrationTrackerDAO,
        baseUrl,
        idUtils,
        versionService,
        apiReportDataServiceV2,
        licenseResolutionService,
        thirdPartyPersistenceService);
  }

  @Override
  public String export() {
    try (InputStream gis = getOriginalSbomContent()) {
      SpdxDocument originalSpdx = SbomSpdxUtils.parseContentStreamNoValidation(gis,
          SbomFormat.forString(exportParams.sbomMetadata.getSpecFormat()));
      Bom baseBomFromSpdx = generateCycloneDxBomFromSpdxDocument(originalSpdx);
      return generateTargetSbomString(mergeCurrentDatabaseState(baseBomFromSpdx, componentRefToComponent));
    }
    catch (IOException | GeneratorException e) {
      throw new SbomExportException(
          String.format("Internal error reading from the original SBOM file for application %s, version %s",
              exportParams.sbomMetadata.getApplicationId(), exportParams.sbomMetadata.getSbomVersion()),
          e);
    }
  }

  @Override
  public PdfData exportPdf() {
    throw new UnsupportedOperationException("PDF export not supported for SBOM exporter");
  }

  public Bom generateCycloneDxBomFromSpdxDocument(SpdxDocument base) {
    Bom target = new Bom();
    setComponentMetadata(target);
    try {
      List<SpdxPackage> packages = SbomSpdxUtils.getAllPackages(base);
      if (CollectionUtils.isNotEmpty(packages)) {
        SpdxPackage rootPackage = SbomSpdxUtils.getRootPackage(base);
        String rootPackageId = rootPackage != null ? rootPackage.getId() : "";
        spdxPackageIdsToCdxBomRefs = mapSpdxPackageIdsToCdxBomRefs(packages);
        setComponents(packages, rootPackageId, target);
        setVulnerabilities(packages, target);
        setDependencies(base, target);
      }
    }
    catch (InvalidSPDXAnalysisException e) {
      throw new SbomExportException(
          String.format("Internal error creating CycloneDX SBOM from the original SPDX SBOM file " +
              "for application %s, version %s", exportParams.sbomMetadata.getApplicationId(),
              exportParams.sbomMetadata.getSbomVersion()),
          e);
    }
    return target;
  }

  private Map<String, String> mapSpdxPackageIdsToCdxBomRefs(List<SpdxPackage> packages) {
    Map<String, String> mappings = new HashMap<>();
    for (SpdxPackage spdxPackage : packages) {
      String spdxPackageId = spdxPackage.getId();
      mappings.put(spdxPackageId, HashUtils.hash(spdxPackageId, HashUtils.SHA1));
    }
    return mappings;
  }

  private void setComponents(List<SpdxPackage> packages, String rootPackageId, final Bom target) {
    for (SpdxPackage spdxPackage : packages) {
      Component newComponent = createCdxBomComponentFromSpdxPackage(spdxPackage, rootPackageId);
      componentRefToComponent.put(SbomIdentityUtils.getComponentRef(spdxPackage), newComponent);
      target.addComponent(newComponent);
    }
  }

  private String resolvePurl(SpdxPackage spdxPackage) {
    try {
      String purlFromBase = getPurl(spdxPackage);
      if (StringUtils.isNotEmpty(purlFromBase) && StringUtils.isNoneBlank(purlFromBase)) {
        return purlFromBase;
      }
      String name = "";
      String version = "";
      try {
        name = spdxPackage.getName().orElse(MISSING_COMPONENT_NAME);
        version = spdxPackage.getVersionInfo().orElse("");
      }
      catch (InvalidSPDXAnalysisException e) {
        log.debug("Cannot read SPDX package name or version with ID {}", spdxPackage.getId());
      }
      return getPackageUrlFromCoordinates(name, version);
    }
    catch (InvalidSPDXAnalysisException e) {
      log.debug("Cannot read PURL for SPDX package with ID {}", spdxPackage.getId(), e);
    }
    return null;
  }

  private Component createCdxBomComponentFromSpdxPackage(
      final SpdxPackage spdxPackage,
      final String rootPackageId)
  {
    Component component = new Component();
    component.setType(spdxPackage.getId().equals(rootPackageId) ? Component.Type.APPLICATION : Component.Type.LIBRARY);
    component.setBomRef(spdxPackageIdsToCdxBomRefs.get(spdxPackage.getId()));
    try {
      final Optional<String> sha1Optional = getChecksum(spdxPackage, ChecksumAlgorithm.SHA1);
      sha1Optional.ifPresent(sha1 -> SbomCycloneDxUtils.addSonatypeTruncatedSha1(sha1, component));
      sha1Optional
          .ifPresent(sha1 -> component.setHashes(Collections.singletonList(new Hash(Hash.Algorithm.SHA1, sha1))));
    }
    catch (InvalidSPDXAnalysisException e) {
      log.debug("Unable to read sha1 from SPDX package with ID {}", spdxPackage.getId(), e);
    }
    String purl = resolvePurl(spdxPackage);
    if (purl != null) {
      PackageUrlIdentifier packageUrlIdentifier = new PackageUrlIdentifier(purl);
      component.setPurl(purl);
      component.setName(packageUrlIdentifier.getName());
      component.setVersion(packageUrlIdentifier.getVersion());
      String namespace = packageUrlIdentifier.getNamespace();
      if (StringUtils.isNotBlank(namespace)) {
        component.setGroup(namespace);
      }
    }
    setCpe(spdxPackage, component);
    setSwid(spdxPackage, component);
    setLicenseInformation(spdxPackage, component);
    return component;
  }

  private void setCpe(SpdxPackage spdxPackage, Component component) {
    try {
      String cpe = getCpe(spdxPackage);
      component.setCpe(cpe);
    }
    catch (InvalidSPDXAnalysisException e) {
      log.debug("Error setting CPE using SPDX package with ID {}", spdxPackage.getId());
    }
  }

  private void setSwid(SpdxPackage spdxPackage, Component component) {
    try {
      getSwid(spdxPackage).ifPresent(component::setSwid);
    }
    catch (InvalidSPDXAnalysisException e) {
      log.debug("Error setting SWID using SPDX package with ID {}", spdxPackage.getId());
    }
  }

  private void setLicenseInformation(SpdxPackage spdxPackage, Component component) {
    LicenseChoice licenseChoice = new LicenseChoice();
    try {
      String licenseExpression = spdxPackage.getLicenseConcluded().toString();
      if (StringUtils.isNotBlank(licenseExpression)) {
        List<License> licenses =
            parseLicenseChoiceExpression(licenseExpression, component.getPurl());
        licenseChoice.setLicenses(licenses);
      }
    }
    catch (InvalidSPDXAnalysisException e) {
      log.debug("Error reading license information for SPDX package with ID {}", spdxPackage.getId(), e);
    }

    if (licenseChoice.getExpression() == null && CollectionUtils.isEmpty(licenseChoice.getLicenses())) {
      // 1.6+ new library won't validate/generate an empty array of licenses and a null expression on a component
      component.setLicenses(null);
    }
    else {
      component.setLicenses(licenseChoice);
    }
  }

  private void setVulnerabilities(
      final List<SpdxPackage> packages,
      final Bom target) throws InvalidSPDXAnalysisException
  {
    List<Vulnerability> targetVulnerabilities = new ArrayList<>();
    for (SpdxPackage spdxPackage : packages) {
      Map<String, ExternalRef> vulnerabilities = getVulnerabilitiesForPackage(spdxPackage);
      for (Map.Entry<String, ExternalRef> entry : vulnerabilities.entrySet()) {
        Vulnerability vulnerability = new Vulnerability();
        vulnerability.setId(entry.getKey());
        Vulnerability.Affect affects = new Vulnerability.Affect();
        affects.setRef(spdxPackageIdsToCdxBomRefs.get(spdxPackage.getId()));
        vulnerability.setAffects(Collections.singletonList(affects));
        ExternalRef externalRef = entry.getValue();
        if (externalRef.getReferenceType().getIndividualURI().equals("advisory")) {
          Vulnerability.Advisory advisory = new Vulnerability.Advisory();
          advisory.setUrl(externalRef.getReferenceLocator());
          vulnerability.setAdvisories(Collections.singletonList(advisory));
        }
        targetVulnerabilities.add(vulnerability);
      }
    }
    target.setVulnerabilities(targetVulnerabilities);
  }

  private void setDependencies(
      final SpdxDocument base,
      final Bom target) throws InvalidSPDXAnalysisException
  {
    SpdxPackage rootSpdxPackage = SbomSpdxUtils.getRootPackage(base);
    // Set root of dependency tree first referencing the Bom Component ref
    // A random UUID will be used as the bom-ref of the new Bom Component
    String rootBomComponentRef = target.getMetadata().getComponent().getBomRef();
    Dependency rootBomDependency = new Dependency(rootBomComponentRef);
    if (hasDependsOnRelationship(rootSpdxPackage)) {
      addChildDependencies(rootBomDependency, rootSpdxPackage);
      target.addDependency(rootBomDependency);
    }
    List<SpdxPackage> directAndTransitiveDependencies = SbomSpdxUtils.getAllPackages(base)
        .stream()
        .filter(pkg -> !pkg.getId().equals(rootSpdxPackage.getId()))
        .toList();
    for (SpdxPackage spdxPackage : directAndTransitiveDependencies) {
      addAvailableDependencies(target, spdxPackage);
    }
  }

  private void addAvailableDependencies(
      Bom cycloneDxSbom,
      SpdxPackage spdxPackage) throws InvalidSPDXAnalysisException
  {
    if (hasDependsOnRelationship(spdxPackage)) {
      Dependency dependency = new Dependency(spdxPackageIdsToCdxBomRefs.get(spdxPackage.getId()));
      addChildDependencies(dependency, spdxPackage);
      cycloneDxSbom.addDependency(dependency);
    }
  }

  private boolean hasDependsOnRelationship(SpdxPackage spdxPackage) {
    try {
      return spdxPackage.getRelationships().stream().anyMatch(relationship -> {
        try {
          return relationship.getRelationshipType().equals(RelationshipType.DEPENDS_ON);
        }
        catch (InvalidSPDXAnalysisException e) {
          log.debug("Error getting relationships", e);
        }
        return false;
      });
    }
    catch (InvalidSPDXAnalysisException e) {
      log.debug("Error getting relationships", e);
    }
    return false;
  }

  private void addChildDependencies(
      final Dependency dependency,
      final SpdxPackage spdxPackage) throws InvalidSPDXAnalysisException
  {
    dependency.setDependencies(new ArrayList<>());
    for (Relationship relationship : spdxPackage.getRelationships()) {
      relationship.getRelatedSpdxElement()
          .ifPresent(
              pkg -> dependency.getDependencies().add(new Dependency(spdxPackageIdsToCdxBomRefs.get(pkg.getId()))));
    }
  }

  private void setComponentMetadata(Bom bom) {
    Metadata bomMetadata = new Metadata();
    Component bomComponent = new Component();
    bomComponent.setType(Type.APPLICATION);
    bomComponent.setName(idUtils.getPublicOwnerId(OwnerType.APPLICATION, exportParams.sbomMetadata
        .getApplicationId()));
    bomComponent.setVersion(exportParams.sbomMetadata.getSbomVersion());
    bomComponent.setBomRef(UUID.randomUUID().toString());
    bomMetadata.setComponent(bomComponent);
    bom.setMetadata(bomMetadata);
  }
}
