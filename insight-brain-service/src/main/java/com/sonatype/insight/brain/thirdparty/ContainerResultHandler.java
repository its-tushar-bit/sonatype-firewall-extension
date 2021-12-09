/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.model.ProjectScanItem;

import com.google.gson.Gson;
import com.neuvector.model.ModuleCve;
import com.neuvector.model.ScanModule;
import com.neuvector.model.ScanRepoReportData;
import com.neuvector.model.Vulnerability;
import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Component.Type;
import org.cyclonedx.model.ExtensibleType;
import org.cyclonedx.model.Extension;
import org.cyclonedx.model.Extension.ExtensionType;
import org.cyclonedx.model.Source;
import org.cyclonedx.model.vulnerability.Rating;
import org.cyclonedx.model.vulnerability.Vulnerability10;
import org.cyclonedx.model.vulnerability.Vulnerability10.Recommendation;
import org.cyclonedx.model.vulnerability.Vulnerability10.Score;
import org.cyclonedx.model.vulnerability.Vulnerability10.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContainerResultHandler extends SbomResultHandler
{
  private static final Logger log = LoggerFactory.getLogger(ContainerResultHandler.class);

  private static final String VULNERABILITY_KEY = "vulnerabilities";

  public static final String SONATYPE_CONTAINER = "Sonatype-Container";

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
        processSbom(content, sourceBom, targetBom, thirdPartyFile, moduleDependencies);

        if (targetBom.getComponents() != null && targetBom.getComponents().isEmpty()) {
          return new FilteredThirdPartyContent(content.getContent(), moduleDependencies);
        }
        return new FilteredThirdPartyContent(generateFilteredSbom(targetBom), moduleDependencies);
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

    Map<String, Vulnerability> cveMap = new HashMap<>();
    Vulnerability[] cves = scanRepoReportData.getReport().getVulnerabilities();
    for (Vulnerability cve : cves) {
      String name = cve.getName();
      cveMap.put(name, cve);
    }

    Set<Component> componentsToAdd = new LinkedHashSet<>();
    ScanModule[] modules = scanRepoReportData.getReport().getModules();
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
      componentsToAdd.add(component);

      ModuleCve[] moduleCves = module.getCves();
      if (moduleCves != null) {
        List<ExtensibleType> extensions = new ArrayList<>();
        for (ModuleCve moduleCve : moduleCves) {
          Vulnerability vulnerability = cveMap.get(moduleCve.getName());
          Vulnerability10 vulnerability10 = new Vulnerability10(component.getGroup(), component.getName());
          if (vulnerability != null) {
            vulnerability10.setId(vulnerability.getName());
            vulnerability10.setDescription(vulnerability.getDescription());

            Score score = new Score();
            Double base = Double.valueOf(vulnerability.getScore_v3());
            score.setBase(base);

            Rating rating = new Rating();
            rating.setScore(score);
            rating.setSeverity(Severity.fromString(vulnerability.getSeverity()));
            rating.setVector(vulnerability.getVectors_v3());
            vulnerability10.setRatings(Arrays.asList(rating));

            Source source = new Source();
            source.setName(IdentificationSource.SONATYPE_CONTAINER.getName());
            source.setUrl(getUrl(vulnerability));
            vulnerability10.setSource(source);

            Recommendation recommendation = new Recommendation();
            recommendation.setText(vulnerability.getFixed_version());
            vulnerability10.setRecommendations(Arrays.asList(recommendation));
          }
          extensions.add(vulnerability10);
        }
        Extension extension = new Extension(ExtensionType.VULNERABILITIES, extensions);
        component.add(VULNERABILITY_KEY, extension);
      }
    }
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
  String determineIdentificationSource(final String contentPath) {
    return SONATYPE_CONTAINER;
  }

  @Override
  ThirdPartyCoordinateSecurity parseVulnerability(
      final Vulnerability10 vulnerability,
      final String fileCoordinateId)
  {
    ThirdPartyCoordinateSecurity coordinateSecurity = super.parseVulnerability(vulnerability, fileCoordinateId);
    coordinateSecurity.setFixedBy(coordinateSecurity.getRecommendations());
    return coordinateSecurity;
  }
}
