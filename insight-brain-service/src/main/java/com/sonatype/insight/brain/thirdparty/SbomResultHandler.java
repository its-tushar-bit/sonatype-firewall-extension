/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.purl.InvalidPackageURLException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.github.packageurl.PackageURLBuilder;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclonedx.BomGeneratorFactory;
import org.cyclonedx.CycloneDxSchema.Version;
import org.cyclonedx.exception.GeneratorException;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.generators.xml.BomXmlGenerator;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.ExtensibleType;
import org.cyclonedx.model.Extension;
import org.cyclonedx.model.Hash;
import org.cyclonedx.model.Hash.Algorithm;
import org.cyclonedx.model.License;
import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.model.Source;
import org.cyclonedx.model.vulnerability.Rating;
import org.cyclonedx.model.vulnerability.Vulnerability10;
import org.cyclonedx.model.vulnerability.Vulnerability10.Advisory;
import org.cyclonedx.model.vulnerability.Vulnerability10.Recommendation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.getTruncatedAttackVector;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.getTruncatedIdentificationSource;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.getTruncatedLink;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.getTruncatedRatingMethod;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.getTruncatedRefId;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.getTruncatedSeverityDescription;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.getTruncatedVulnerabilitySource;

public class SbomResultHandler
    implements ThirdPartyScanResultHandler
{
  private static final Logger log = LoggerFactory.getLogger(SbomResultHandler.class);

  private static final String MISSING_COMPONENT_NAME = "[Not Provided]";

  private static final String VULNERABILITY_KEY = "vulnerabilities";

  private final ThirdPartyFileDAO thirdPartyFileDAO = new ThirdPartyFileDAO();

  private final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO = new ThirdPartyFileCoordinateDAO();

  private final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO = new ThirdPartyCoordinateSecurityDAO();

  private final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO = new ThirdPartyCoordinateLicenseDAO();

  @Override
  public String handleAndFilterContents(
      ThirdPartyScanContent content,
      ThirdPartyFile thirdPartyFile)
  {
    try {
      if (!StringUtils.isBlank(content.getContent())) {
        Bom sourceBom = parseBom(content);
        Bom targetBom = new Bom();
        log.info("Processing SBOM content");
        processSbom(content, sourceBom, targetBom, thirdPartyFile);
        if (targetBom.getComponents() != null && targetBom.getComponents().isEmpty()) {
          return content.getContent();
        }
        else {
          return generateFilteredSbom(targetBom);
        }
      }
      return content.getContent();
    }
    catch (Exception e) {
      throw new RuntimeException("Error filtering sbom file " + content.getPath(), e);
    }
  }

  //visible for testing
  Bom parseBom(final ThirdPartyScanContent content) throws ParseException, RuntimeException {
    Bom bom = ThirdPartyUtils.parseBom(content.getContent());
    Version version = ThirdPartyUtils.CYCLONEDX_ACCEPTED_VERSIONS.get(bom.getSpecVersion());
    if (version == null) {
      throw new RuntimeException("Cyclone " + bom.getSpecVersion() + " version is not supported");
    }
    return bom;
  }

  private void processSbom(
      final ThirdPartyScanContent content,
      final Bom sourceBom,
      final Bom targetBom,
      final ThirdPartyFile thirdPartyFile)
  {
    String identificationSource = getTruncatedIdentificationSource(determineIdentificationSource(content.getPath()));
    try (TransactionContext tx = thirdPartyFileDAO.createTransactionContext()) {
      tx.begin();
      Set<ComponentIdentifier> resolvedComponents = new HashSet<>();
      for (Component component : sourceBom.getComponents()) {
        processComponent(component, thirdPartyFile.getId(), targetBom, identificationSource, resolvedComponents, tx);
      }
      tx.commit();
    }
  }

  //visible for testing
  String determineIdentificationSource(final String contentPath) {
    String fileName = StringUtils.contains(contentPath, "/") ?
        StringUtils.substringAfterLast(contentPath, "/") : contentPath;
    String identificationSource = RegExUtils.removePattern(fileName, "-(?i)bom.xml(?i)$");
    if (StringUtils.isBlank(identificationSource) || StringUtils.endsWithIgnoreCase(identificationSource, "bom.xml")) {
      return "Third-Party";
    }
    else {
      return identificationSource;
    }
  }

  private void processComponent(
      final Component sourceComponent,
      final String thirdPartyFileId,
      final Bom targetBom,
      final String identificationSource,
      final Set<ComponentIdentifier> resolvedComponents,
      final TransactionContext tx)
  {
    try {
      Pair<ComponentIdentifier, Component> resolvedComponent = getResolvedComponent(sourceComponent);
      if (resolvedComponent != null) {
        ComponentIdentifier componentIdentifier = resolvedComponent.getLeft();
        if (componentIdentifier == null) {
          targetBom.addComponent(resolvedComponent.getRight());
        }
        else if (!resolvedComponents.contains(componentIdentifier)) {
          saveComponent(thirdPartyFileId, identificationSource, sourceComponent, resolvedComponent, tx);
          targetBom.addComponent(resolvedComponent.getRight());
          resolvedComponents.add(componentIdentifier);
        }
      }
    }
    catch (Exception e) {
      log.warn("Error processing component : {}", sourceComponent, e);
    }
  }

  private Pair<ComponentIdentifier, Component> getResolvedComponent(final Component sourceComponent)
      throws MalformedPackageURLException
  {
    String packageUrl = sourceComponent.getPurl();
    try {
      if (StringUtils.isNotBlank(packageUrl)) {
        PackageUrlIdentifier packageUrlIdentifier = resolvePackageUrl(packageUrl);
        if (StringUtils.isNoneBlank(packageUrlIdentifier.getName(), packageUrlIdentifier.getVersion())) {
          return createComponent(sourceComponent, packageUrlIdentifier, false);
        }
        else {
          log.warn("PackageUrl is not valid {}", packageUrl);
        }
      }
    }
    catch (InvalidPackageURLException e) {
      log.warn("Fallback to coordinates due to invalid purl: {}", packageUrl);
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
      return createComponent(sourceComponent, packageUrlIdentifier, true);
    }
    else {
      // This scenario is only possible when only the hash is sent without coordinates nor purl
      String sha1 = getSha1(sourceComponent);
      if (StringUtils.isNotBlank(sha1)) {
        Component component = new Component();
        component.setType(sourceComponent.getType());
        setHash(sha1, component);
        return Pair.of(null, component);
      }
      else {
        log.debug("Component with invalid information {}", sourceComponent);
      }
    }
    return null;
  }

  private String getPackageUrlFromCoordinates(Component component, String  name)
      throws MalformedPackageURLException
  {
    String group = component.getGroup();
    String publisher = component.getPublisher();

    PackageURLBuilder packageURLBuilder = PackageURLBuilder.aPackageURL()
        .withType(component.getType().getTypeName())
        .withName(name)
        .withVersion(component.getVersion());
    if (StringUtils.isNotBlank(group)) {
      packageURLBuilder.withNamespace(group);
    }
    if (StringUtils.isNotBlank(publisher)) {
      packageURLBuilder.withQualifier("publisher", publisher);
    }
    return packageURLBuilder.build().toString();
  }

  private String getSha1(Component component) {
    List<Hash> hashes = component.getHashes();
    if (hashes != null) {
      return hashes.stream()
          .filter(h -> Algorithm.SHA1.getSpec().equals(h.getAlgorithm()))
          .findFirst()
          .map(Hash::getValue)
          .orElse(null);
    }
    return null;
  }

  private Pair<ComponentIdentifier, Component> createComponent(
      final Component sourceComponent,
      final PackageUrlIdentifier packageUrlIdentifier,
      final boolean coordinates)
  {
    ComponentIdentifier componentIdentifier;
    Component component = new Component();
    component.setType(sourceComponent.getType());

    String sha1 = getSha1(sourceComponent);
    boolean hasHash = StringUtils.isNotBlank(sha1);
    if (hasHash) {
      setHash(sha1, component);
    }
    if (!hasHash || !coordinates) {
      component.setPurl(ThirdPartyScanResultUtils.getTruncatedPurl(packageUrlIdentifier.getPackageUrl()));
    }

    componentIdentifier = packageUrlIdentifier.toComponentIdentifier();
    component.setName(packageUrlIdentifier.getName());
    component.setVersion(packageUrlIdentifier.getVersion());

    return Pair.of(componentIdentifier, component);
  }

  private void setHash(final String sha1, final Component component) {
    component.setHashes(
        Collections.singletonList(new Hash(Algorithm.SHA1, StringUtils.truncate(sha1, 0, HashHelper.MAX_LENGTH))));
  }

  private PackageUrlIdentifier resolvePackageUrl(String packageUrlIdentifier) {
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

  private void saveComponent(
      final String thirdPartyFileId,
      final String identificationSource,
      final Component sourceComponent,
      final Pair<ComponentIdentifier, Component> resolvedComponent,
      final TransactionContext tx)
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

    String fakeHash = ThirdPartyScanResultUtils.hash(
        componentIdentifier.getFormat() + ":" + StringUtils.join(componentIdentifier.getCoordinates().values(), ":"));
    ThirdPartyFileCoordinate fileCoordinate = new ThirdPartyFileCoordinate(fakeHash, identificationSource,
        componentIdentifier.getFormat(), component.getName(), component.getVersion(), thirdPartyFileId);
    fileCoordinate.setPackageUrl(component.getPurl());
    thirdPartyFileCoordinateDAO.insert(tx, fileCoordinate);
    saveLicenses(sourceComponent.getLicenseChoice(), fileCoordinate.getId(), component.getPurl(), tx);
    saveVulnerabilities(sourceComponent.getExtensions(), fileCoordinate.getId(), tx);
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
    ThirdPartyCoordinateSecurity coordinateSecurity = parseVulnerability(vulnerability, fileCoordinateId);
    if (coordinateSecurity != null) {
      thirdPartyCoordinateSecurityDAO.insert(tx, coordinateSecurity);
    }
  }

  @VisibleForTesting
  ThirdPartyCoordinateSecurity parseVulnerability(
      final Vulnerability10 vulnerability,
      final String fileCoordinateId)
  {
    List<Rating> ratingsElements = vulnerability.getRatings();
    if (CollectionUtils.isNotEmpty(ratingsElements)) {
      ThirdPartyCoordinateSecurity coordinateSecurity = new ThirdPartyCoordinateSecurity();
      Rating rating = ratingsElements.get(0);
      Double baseScore = getBaseScore(rating);
      if (baseScore != null) {
        coordinateSecurity.setSeverity(baseScore.floatValue());
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
              vulnerability.getCwes().stream().filter(cwe -> cwe.getText() != null).map(cwe -> cwe.getText().toString())
                  .collect(Collectors.joining()));
        }
        if (vulnerability.getRecommendations() != null) {
          coordinateSecurity.setRecommendations(
              vulnerability.getRecommendations().stream().map(Recommendation::getText).collect(Collectors.joining()));
        }
        if (vulnerability.getAdvisories() != null) {
          coordinateSecurity.setAdvisories(
              vulnerability.getAdvisories().stream().map(Advisory::getText).collect(Collectors.joining()));
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
        return coordinateSecurity;
      }
    }
    return null;
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
      if (CollectionUtils.isEmpty(licenseChoice.getLicenses())) {
        log.debug("Found empty licenses element for Component with packageUrl {}", packageUrl);
      }
      else {
        for (License license : licenseChoice.getLicenses()) {
          if (StringUtils.isNotBlank(license.getId())) {
            saveLicense(license, fileCoordinateId, tx);
          }
          else {
            log.debug("Component with packageUrl {} has license with null/empty ID", packageUrl);
          }
        }
      }
    }
  }

  private void saveLicense(License license, String fileCoordinateId, TransactionContext tx) {
    ThirdPartyCoordinateLicense coordinateLicense = new ThirdPartyCoordinateLicense();
    coordinateLicense.setFileCoordinateId(fileCoordinateId);

    coordinateLicense.setLicenseId(license.getId());
    coordinateLicense.setName(license.getName());
    coordinateLicense.setUrl(license.getUrl());
    thirdPartyCoordinateLicenseDAO.insert(tx, coordinateLicense);
  }

  private String generateFilteredSbom(Bom sbom)
      throws ParserConfigurationException, GeneratorException
  {
    BomXmlGenerator generator =
        BomGeneratorFactory.createXml(Version.VERSION_13, sbom);
    generator.generate();
    return generator.toXmlString();
  }
}
