/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.model.ProjectScanItem;

import com.google.gson.Gson;
import com.neuvector.model.ModuleCve;
import com.neuvector.model.ScanModule;
import com.neuvector.model.ScanRepoReportData;
import com.neuvector.model.Vulnerability;
import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.BomGeneratorFactory;
import org.cyclonedx.CycloneDxSchema.Version;
import org.cyclonedx.generators.xml.BomXmlGenerator;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Component.Type;
import org.cyclonedx.model.vulnerability.Vulnerability.Affect;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating.Severity;
import org.cyclonedx.model.vulnerability.Vulnerability.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContainerResultHandler
    extends SbomResultHandler
{
  private static final Logger log = LoggerFactory.getLogger(ContainerResultHandler.class);

  public static final String SONATYPE_CONTAINER = "Sonatype-Container";

  public ContainerResultHandler(
      final ThirdPartyFileDAO thirdPartyFileDAO,
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO,
      final MultiLicenseDAO multiLicenseDAO,
      final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVexDAO,
      final TelemetryUtils telemetryUtils,
      final TelemetrySender telemetrySender)
  {
    super(thirdPartyFileDAO, thirdPartyFileCoordinateDAO, thirdPartyCoordinateSecurityDAO,
        thirdPartyCoordinateLicenseDAO, multiLicenseDAO, thirdPartyVexDAO, telemetryUtils, telemetrySender);
  }

  @Override
  public FilteredThirdPartyContent handleAndFilterContents(
      ThirdPartyScanContent content,
      ThirdPartyFile thirdPartyFile)
  {
    try {
      if (!StringUtils.isBlank(content.getContent())) {
        Bom sourceBom = parseBom(content);
        Bom targetBom = new Bom();
        List<ProjectScanItem> moduleDependencies = new ArrayList<>();
        log.info("Processing container analysis content");
        processSbom(content.getPath(), sourceBom, targetBom, thirdPartyFile, moduleDependencies);

        if (targetBom.getComponents() != null && targetBom.getComponents().isEmpty()) {
          return new FilteredThirdPartyContent(content.getContent(), moduleDependencies);
        }
        BomXmlGenerator generator = BomGeneratorFactory.createXml(Version.VERSION_14, targetBom);
        generator.generate();
        return new FilteredThirdPartyContent(generator.toXmlString(), moduleDependencies);
      }
      return new FilteredThirdPartyContent(content.getContent());
    }
    catch (Exception e) {
      throw new RuntimeException("Error filtering container file " + content.getPath(), e);
    }
  }

  @Override
  Bom parseBom(final ThirdPartyScanContent content) throws RuntimeException {
    ScanRepoReportData scanRepoReportData = new Gson().fromJson(content.getContent(), ScanRepoReportData.class);

    Bom bom = new Bom();
    bom.setVulnerabilities(new ArrayList<>());

    Map<String, Vulnerability> cveVulnerabilityMap = new HashMap<>();
    Vulnerability[] vulnerabilities = scanRepoReportData.getReport().getVulnerabilities();
    for (Vulnerability vulnerability : vulnerabilities) {
      String name = vulnerability.getName();
      cveVulnerabilityMap.put(name, vulnerability);
    }

    Set<Component> componentsToAdd = new LinkedHashSet<>();
    ScanModule[] modules = scanRepoReportData.getReport().getModules();
    Map<String, org.cyclonedx.model.vulnerability.Vulnerability> cveCycloneDxVulnMap = new HashMap<>();
    Map<String, Set<String>> vulnerabilityAffectsMap = new HashMap<>();

    for (ScanModule module : modules) {
      String resourceId = module.getName();

      ComponentIdentifier componentIdentifier =
          ComponentIdentifier.createContainerCoordinates(module.getSource(), resourceId, module.getVersion());
      PackageUrlIdentifier packageUrlIdentifier = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);

      Component component = new Component();
      component.setGroup(module.getSource());
      component.setName(resourceId);
      component.setVersion(module.getVersion());
      component.setType(Type.FILE);
      component.setPurl(packageUrlIdentifier.getPackageUrl());
      component.setBomRef(packageUrlIdentifier.getPackageUrl());
      componentsToAdd.add(component);

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
            source.setUrl(getUrl(vulnerability).toString());
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
          String packageUrl = packageUrlIdentifier.getPackageUrl();
          if (!vulnerabilityAffectsMap.get(vuln.getId()).contains(packageUrl)) {
            vulnerabilityAffectsMap.get(vuln.getId()).add(packageUrl);
            Affect affect = new Affect();
            affect.setRef(packageUrl);
            vuln.getAffects().add(affect);
          }
        }
      }
    }
    bom.getVulnerabilities().addAll(cveCycloneDxVulnMap.values());
    bom.setComponents(new ArrayList<>(componentsToAdd));
    return bom;
  }

  private URL getUrl(final Vulnerability vulnerability) {
    try {
      return new URL(vulnerability.getLink());
    }
    catch (MalformedURLException e) {
      log.debug(e.getMessage(), e);
    }
    return null;
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
