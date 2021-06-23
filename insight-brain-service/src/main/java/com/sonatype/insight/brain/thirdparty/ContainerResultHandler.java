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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.purl.InvalidPackageURLException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.github.packageurl.PackageURLBuilder;
import com.google.gson.Gson;
import com.neuvector.model.ModuleCve;
import com.neuvector.model.ScanModule;
import com.neuvector.model.ScanRepoReportData;
import com.neuvector.model.Vulnerability;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
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

import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.getTruncatedAttackVector;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.getTruncatedIdentificationSource;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.getTruncatedLink;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.getTruncatedRefId;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.getTruncatedSeverityDescription;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.getTruncatedVulnerabilitySource;

public class ContainerResultHandler implements ThirdPartyScanResultHandler
{
  private static final Logger log = LoggerFactory.getLogger(ContainerResultHandler.class);

  private static final String VULNERABILITY_KEY = "vulnerabilities";

  public static final String SONATYPE_CONTAINER = "Sonatype-Container";

  private final ThirdPartyFileDAO thirdPartyFileDAO = new ThirdPartyFileDAO();

  private final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO = new ThirdPartyFileCoordinateDAO();

  private final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO = new ThirdPartyCoordinateSecurityDAO();

  @Override
  public String handleAndFilterContents(
      ThirdPartyScanContent content,
      ThirdPartyFile thirdPartyFile)
  {
    try {
      if (!StringUtils.isBlank(content.getContent())) {
        ScanRepoReportData scanRepoReportData = new Gson().fromJson(content.getContent(), ScanRepoReportData.class);
        Bom sourceBom = parseBom(scanRepoReportData);
        Bom targetBom = new Bom();
        log.info("Processing Container content");
        processBom(sourceBom, targetBom, thirdPartyFile);
      }
      return content.getContent();
    }
    catch (Exception e) {
      throw new RuntimeException("Error filtering container file " + content.getPath(), e);
    }
  }

  private Bom parseBom(final ScanRepoReportData scanRepoReportData) throws MalformedURLException {
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
        for (ModuleCve moduleCve : moduleCves) {
          Vulnerability vulnerability = cveMap.get(moduleCve.getName());
          Vulnerability10 vulnerability10 = new Vulnerability10(component.getGroup(), component.getName());
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
          source.setName(IdentificationSource.SONATYPE.getName());
          source.setUrl(new URL(vulnerability.getLink()));
          vulnerability10.setSource(source);

          Recommendation recommendation = new Recommendation();
          recommendation.setText(vulnerability.getFixed_version());
          vulnerability10.setRecommendations(Arrays.asList(recommendation));

          List<ExtensibleType> extensions = new ArrayList<>();
          extensions.add(vulnerability10);
          Extension extension = new Extension(ExtensionType.VULNERABILITIES, extensions);
          component.add(VULNERABILITY_KEY, extension);
        }
      }
    }
    bom.setComponents(new ArrayList<>(componentsToAdd));
    return bom;
  }

  private void processBom(
      Bom generateBomFromFile,
      Bom targetBom,
      ThirdPartyFile thirdPartyFile)
  {
    final Map<String, String> hashFileCoordinateIdMap = new HashMap<>();
    String identificationSource = getTruncatedIdentificationSource(SONATYPE_CONTAINER);
    try (TransactionContext tx = thirdPartyFileDAO.createTransactionContext()) {
      tx.begin();
      for (Component component : generateBomFromFile.getComponents()) {
        processComponent(component, thirdPartyFile.getId(), targetBom, hashFileCoordinateIdMap, identificationSource,
            tx);
      }
      tx.commit();
    }
  }

  private void processComponent(
      Component component,
      String thirdPartyFileId,
      Bom targetBom,
      Map<String, String> hashFileCoordinateIdMap,
      String identificationSource,
      TransactionContext tx)
  {
    String packageUrl = component.getPurl();
    PackageUrlIdentifier packageUrlIdentifier = new PackageUrlIdentifier(packageUrl);
    if (StringUtils.isNoneBlank(packageUrlIdentifier.getName(), packageUrlIdentifier.getVersion())) {
      processPurlComponent(component, packageUrlIdentifier, thirdPartyFileId, targetBom,
          hashFileCoordinateIdMap, identificationSource, tx);
    }
  }

  private void processPurlComponent(
      Component component,
      PackageUrlIdentifier packageUrlIdentifier,
      String thirdPartyFileId,
      Bom targetBom,
      Map<String, String> hashFileCoordinateIdMap,
      String identificationSource,
      TransactionContext tx)
  {
    ComponentIdentifier componentIdentifier = resolveComponentIdentifier(packageUrlIdentifier);
    packageUrlIdentifier = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);
    Component bomComponent =
        createComponent(component, packageUrlIdentifier.getName(), packageUrlIdentifier.getVersion());
    bomComponent.setPurl(ThirdPartyScanResultUtils.getTruncatedPurl(packageUrlIdentifier.getPackageUrl()));
    saveComponent(thirdPartyFileId, hashFileCoordinateIdMap, componentIdentifier, identificationSource, targetBom,
        bomComponent, component, tx);
  }

  private Component createComponent(Component component, String name, String version) {
    Component bomComponent = new Component();
    bomComponent.setType(component.getType());
    bomComponent.setName(name);
    bomComponent.setVersion(version);
    return bomComponent;
  }

  private ComponentIdentifier resolveComponentIdentifier(PackageUrlIdentifier packageUrlIdentifier) {
    PackageURLBuilder packageURLBuilder = PackageURLBuilder.aPackageURL();
    packageURLBuilder.withType(ThirdPartyScanResultUtils.getValidFormat(packageUrlIdentifier.getFormat()));
    packageURLBuilder.withName(ThirdPartyScanResultUtils.getTruncatedName(packageUrlIdentifier.getName()));
    packageURLBuilder.withVersion(ThirdPartyScanResultUtils.getTruncatedVersion(packageUrlIdentifier.getVersion()));

    if (packageUrlIdentifier.getNamespace() != null) {
      packageURLBuilder.withNamespace(packageUrlIdentifier.getNamespace());
    }

    try {
      PackageURL packageUrl = packageURLBuilder.build();
      return new PackageUrlIdentifier(packageUrl.canonicalize()).toComponentIdentifier();
    }
    catch (MalformedPackageURLException e) {
      throw new InvalidPackageURLException(e.getMessage(), e);
    }
  }

  private void saveComponent(
      String thirdPartyFileId,
      Map<String, String> hashFileCoordinateIdMap,
      ComponentIdentifier componentIdentifier,
      String identificationSource,
      Bom targetBom,
      Component bomComponent,
      Component component,
      TransactionContext tx)
  {
    String fakeHash = ThirdPartyScanResultUtils.hash(
        componentIdentifier.getFormat() + ":" + StringUtils.join(componentIdentifier.getCoordinates().values(), ":"));
    if (!hashFileCoordinateIdMap.containsKey(fakeHash)) {
      ThirdPartyFileCoordinate fileCoordinate = new ThirdPartyFileCoordinate(fakeHash, identificationSource,
          componentIdentifier.getFormat(), bomComponent.getName(), bomComponent.getVersion(), thirdPartyFileId);
      fileCoordinate.setPackageUrl(bomComponent.getPurl());
      thirdPartyFileCoordinateDAO.insert(tx, fileCoordinate);
      hashFileCoordinateIdMap.put(fakeHash, fileCoordinate.getId());
      saveVulnerabilities(component.getExtensions(), fileCoordinate.getId(), tx);
      targetBom.addComponent(bomComponent);
    }
  }

  private void saveVulnerabilities(
      Map<String, Extension> extensions,
      String fileCoordinateId,
      TransactionContext tx)
  {
    if (MapUtils.isNotEmpty(extensions)) {
      Extension vulnerabilityExtension = extensions.get(VULNERABILITY_KEY);
      if (vulnerabilityExtension != null && CollectionUtils.isNotEmpty(vulnerabilityExtension.getExtensions())) {
        Set<String> vulnerabilityMap = new HashSet<>();
        for (ExtensibleType extensibleType : vulnerabilityExtension.getExtensions()) {
          if (extensibleType instanceof Vulnerability10) {
            Vulnerability10 vulnerability = (Vulnerability10) extensibleType;
            String refId = vulnerability.getId();
            if (StringUtils.isNotBlank(refId) && !vulnerabilityMap.contains(refId)) {
              saveVulnerability(vulnerability, fileCoordinateId, tx);
              vulnerabilityMap.add(refId);
            }
          }
        }
      }
    }
  }

  private void saveVulnerability(Vulnerability10 vulnerability, String fileCoordinateId, TransactionContext tx) {
    ThirdPartyCoordinateSecurity coordinateSecurity = new ThirdPartyCoordinateSecurity();

    List<Rating> ratingsElements = vulnerability.getRatings();
    if (CollectionUtils.isNotEmpty(ratingsElements)) {
      Rating rating = ratingsElements.get(0);
      Double baseScore = getBaseScore(rating);
      if (baseScore != null) {
        coordinateSecurity.setSeverity(baseScore.floatValue());
        if (rating.getVector() != null) {
          coordinateSecurity.setAttackVector(getTruncatedAttackVector(rating.getVector()));
        }
        if (rating.getSeverity() != null) {
          coordinateSecurity
              .setSeverityDescription(getTruncatedSeverityDescription(rating.getSeverity().getSeverityName()));
        }
        coordinateSecurity.setFileCoordinateId(fileCoordinateId);
        if (vulnerability.getRecommendations() != null) {
          coordinateSecurity.setRecommendations(
              vulnerability.getRecommendations().stream().map(Recommendation::getText).collect(Collectors.joining()));
          coordinateSecurity.setFixedBy(coordinateSecurity.getRecommendations());
        }

        Source source = vulnerability.getSource();
        if (source != null) {
          coordinateSecurity.setVulnerabilitySource(getTruncatedVulnerabilitySource(source.getName()));
          if (source.getUrl() != null) {
            coordinateSecurity.setLink(getTruncatedLink(source.getUrl().toString()));
          }
        }
        coordinateSecurity.setRefId(getTruncatedRefId(vulnerability.getId()));
        coordinateSecurity.setDescription(vulnerability.getDescription());

        thirdPartyCoordinateSecurityDAO.insert(tx, coordinateSecurity);
      }
    }
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
}
