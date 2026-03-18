/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.sbom.SbomComponentInfoTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.model.ItemContentType;
import com.sonatype.insight.scan.model.ProjectScanItem;
import com.sonatype.insight.telemetry.model.TelemetryData;

import com.google.gson.Gson;
import com.neuvector.model.ModuleCve;
import com.neuvector.model.ScanModule;
import com.neuvector.model.ScanRepoReportData;
import com.neuvector.model.Vulnerability;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Component.Type;
import org.cyclonedx.model.vulnerability.Vulnerability.Affect;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating.Severity;
import org.cyclonedx.model.vulnerability.Vulnerability.Source;
import org.cyclonedx.parsers.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.sbom.SbomSpecification.CYCLONEDX;

public class ContainerResultHandler
    extends SbomResultHandler
{
  private static final Logger log = LoggerFactory.getLogger(ContainerResultHandler.class);

  public static final String SONATYPE_CONTAINER = "Sonatype-Container";

  private final SbomComponentInfoTelemetry componentInfoTelemetry;

  private ProductLicense productLicense;

  public ContainerResultHandler(
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
      final ProductLicense productLicense)
  {
    super(thirdPartyFileDAO, fileCoordinatePersister, thirdPartyCoordinateSecurityDAO,
        thirdPartyCoordinateLicenseDAO, thirdPartySbomMetadataDAO, multiLicenseDAO, thirdPartyVexDAO, telemetryUtils,
        telemetrySender, thirdPartyScanContext);
    this.componentInfoTelemetry = new SbomComponentInfoTelemetry();
    this.productLicense = productLicense;
  }

  @Override
  public FilteredThirdPartyContent handleAndFilterContents(
      ThirdPartyScanContent content,
      ThirdPartyFile thirdPartyFile)
  {
    try {
      if (!StringUtils.isBlank(content.getContent())) {
        Pair<Bom, Boolean> bomAndIsValid = parseBom(content);
        Bom sourceBom = bomAndIsValid.getLeft();
        boolean isValid = bomAndIsValid.getRight();
        Bom targetBom = new Bom();
        List<ProjectScanItem> moduleDependencies = new ArrayList<>();
        log.info("Processing container analysis content");
        processSbom(content.getPath(), sourceBom, targetBom, thirdPartyFile, moduleDependencies, isValid);

        if (targetBom.getComponents() != null && targetBom.getComponents().isEmpty()) {
          return new FilteredThirdPartyContent(content.getContent(), moduleDependencies);
        }

        String filteredSbomContent = generateFilteredSbom(targetBom);
        componentInfoTelemetry.setSpec(CYCLONEDX.name());
        componentInfoTelemetry.setSpecVersion(sourceBom.getSpecVersion());
        componentInfoTelemetry.setHasDependencies(!moduleDependencies.isEmpty());
        TelemetryData thirdPartyScanComponentInfoTelemetryData =
            telemetryUtils.buildThirdPartyScanComponentInfoTelemetryData(componentInfoTelemetry, true, true);
        telemetrySender.send(thirdPartyScanComponentInfoTelemetryData);
        return new FilteredThirdPartyContent(filteredSbomContent, moduleDependencies);

      }
      return new FilteredThirdPartyContent(content.getContent());
    }
    catch (Exception e) {
      throw new RuntimeException("Error filtering container file " + content.getPath(), e);
    }
  }

  @Override
  Pair<Bom, Boolean> parseBom(final ThirdPartyScanContent content) throws RuntimeException {
    if (thirdPartyScanContext != null && thirdPartyScanContext.getContainerImageSbomSpecification() == CYCLONEDX) {
      if (StringUtils.isBlank(content.getContent())) {
        throw new BadRequestException("Empty content for container image");
      }
      try {
        Bom bom = new JsonParser().parse(content.getContent().getBytes(StandardCharsets.UTF_8));
        componentInfoTelemetry.setContentType(SbomFormat.JSON.name());
        return Pair.of(bom, true);
      }
      catch (ParseException e) {
        throw new BadRequestException("Invalid content for container image", e);
      }
    }

    ScanRepoReportData scanRepoReportData = new Gson().fromJson(content.getContent(), ScanRepoReportData.class);
    componentInfoTelemetry.setContentType(SbomFormat.JSON.name());

    Bom bom = new Bom();
    bom.setVulnerabilities(new ArrayList<>());

    Map<String, Vulnerability> cveVulnerabilityMap = new HashMap<>();
    Vulnerability[] vulnerabilities =
        ArrayUtils.nullToEmpty(scanRepoReportData.getReport().getVulnerabilities(), Vulnerability[].class);
    for (Vulnerability vulnerability : vulnerabilities) {
      String name = vulnerability.getName();
      cveVulnerabilityMap.put(name, vulnerability);
    }

    Set<Component> componentsToAdd = new LinkedHashSet<>();
    ScanModule[] modules = ArrayUtils.nullToEmpty(scanRepoReportData.getReport().getModules(), ScanModule[].class);
    Map<String, org.cyclonedx.model.vulnerability.Vulnerability> cveCycloneDxVulnMap = new HashMap<>();
    Map<String, Set<String>> vulnerabilityAffectsMap = new HashMap<>();

    for (ScanModule module : modules) {
      String resourceId = module.getName();

      ComponentIdentifier componentIdentifier;
      PackageUrlIdentifier packageUrlIdentifier;

      if (thirdPartyScanContext != null
          && thirdPartyScanContext.getContainerItemContentType() == ItemContentType.CONTAINER_URI_SONATYPE
          && SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.isEnabled()
          && productLicense.hasFeature(LicensedFeature.CONTAINER_IMAGES_EVALUATION))
      {
        componentIdentifier = getCorrespondingComponentIdentifier(module, resourceId);
        packageUrlIdentifier = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);
      }
      else {
        componentIdentifier = ComponentIdentifier.createContainerCoordinates(
            module.getSource(), resourceId, module.getVersion());
        packageUrlIdentifier = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);
      }

      String bomRef = UUID.randomUUID().toString();
      Component component = new Component();
      component.setGroup(module.getSource());
      component.setName(resourceId);
      component.setVersion(module.getVersion());
      component.setType(Type.FILE);
      component.setPurl(packageUrlIdentifier.getPackageUrl());
      component.setBomRef(bomRef);
      componentsToAdd.add(component);
      componentInfoTelemetry.incrementCoordinateCount();

      ModuleCve[] moduleCves = module.getCves();
      if (moduleCves != null) {
        for (ModuleCve moduleCve : moduleCves) {
          String cve = moduleCve.getName();
          Vulnerability vulnerability = cveVulnerabilityMap.get(cve);

          org.cyclonedx.model.vulnerability.Vulnerability cycloneDxVulnerability =
              new org.cyclonedx.model.vulnerability.Vulnerability();

          if (vulnerability != null) {
            cycloneDxVulnerability.setId(vulnerability.getName());
            cycloneDxVulnerability.setDescription(vulnerability.getDescription());

            Rating rating = new Rating();
            rating.setScore((double) vulnerability.getScore_v3());
            rating.setSeverity(Severity.fromString(vulnerability.getSeverity().toLowerCase(Locale.ROOT)));
            rating.setVector(vulnerability.getVectors_v3());
            cycloneDxVulnerability.setRatings(Collections.singletonList(rating));

            Source source = new Source();
            source.setName(IdentificationSource.SONATYPE_CONTAINER.getName());
            source.setUrl(getUrl(vulnerability));
            cycloneDxVulnerability.setSource(source);

            cycloneDxVulnerability.setRecommendation(vulnerability.getFixed_version());
            cycloneDxVulnerability.setBomRef(cve);
          }
          cveCycloneDxVulnMap.putIfAbsent(cve, cycloneDxVulnerability);
          org.cyclonedx.model.vulnerability.Vulnerability vuln = cveCycloneDxVulnMap.get(cve);
          List<Affect> affects = vuln.getAffects();
          if (affects == null) {
            vuln.setAffects(new ArrayList<>());
          }
          vulnerabilityAffectsMap.putIfAbsent(vuln.getId(), new HashSet<>());
          if (!vulnerabilityAffectsMap.get(vuln.getId()).contains(bomRef)) {
            vulnerabilityAffectsMap.get(vuln.getId()).add(bomRef);
            Affect affect = new Affect();
            affect.setRef(bomRef);
            vuln.getAffects().add(affect);
          }
        }
      }
    }
    bom.getVulnerabilities().addAll(cveCycloneDxVulnMap.values());
    bom.setComponents(new ArrayList<>(componentsToAdd));
    return Pair.of(bom, true);
  }

  private String getUrl(final Vulnerability vulnerability) {
    try {
      return new URL(vulnerability.getLink()).toString();
    }
    catch (MalformedURLException e) {
      log.debug(e.getMessage(), e);
    }
    return null;
  }

  private ComponentIdentifier getCorrespondingComponentIdentifier(
      final ScanModule module,
      final String resourceId)
  {
    switch (module.getSource()) {
      case "jar": {
        String[] parts = resourceId.split(":");
        String groupId = parts[0];
        String artifactId = parts[1];
        return ComponentIdentifier.createMavenCoordinates(groupId, artifactId, module.getVersion());
      }
      case ".NET": {
        String name = resourceId.split(":")[1];
        return ComponentIdentifier.createNugetCoordinates(name, module.getVersion());
      }
      case "golang": {
        String name = resourceId.split(":")[1];
        return ComponentIdentifier.createGolangCoordinates(name, module.getVersion());
      }
      case "npm":
        return ComponentIdentifier.createNpmCoordinates(resourceId, module.getVersion());
      case "python": {
        String name = resourceId.split(":")[1];
        return ComponentIdentifier.createPypiCoordinates(name, module.getVersion(), null, null);
      }
      case "ruby": {
        String name = resourceId.split(":")[1];
        return ComponentIdentifier.createRubyGemsCoordinates(name, module.getVersion(), null);
      }
      case "php": {
        String[] parts = resourceId.split(":");
        String vendor = parts[1];
        String name = parts[2];
        return ComponentIdentifier.createComposerCoordinates(
            vendor, name, module.getVersion());
      }
      case "Wordpress":
        return ComponentIdentifier.createCpeCoordinates(
            "wordpress", "wordpress", module.getVersion());
      default:
        return ComponentIdentifier.createContainerCoordinates(
            module.getSource(), resourceId, module.getVersion());
    }
  }

  @Override
  String determineThirdPartyIdentificationSource(final String contentPath) {
    return SONATYPE_CONTAINER;
  }

  @Override
  ThirdPartyCoordinateSecurity parseVulnerability(
      final org.cyclonedx.model.vulnerability.Vulnerability vulnerability,
      final String fileCoordinateId)
  {
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity =
        super.parseVulnerability(vulnerability, fileCoordinateId);
    thirdPartyCoordinateSecurity.setFixedBy(thirdPartyCoordinateSecurity.getRecommendations());
    return thirdPartyCoordinateSecurity;
  }
}
