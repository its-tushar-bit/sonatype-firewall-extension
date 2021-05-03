/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.util.Collections;
import java.util.HashMap;
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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.cyclonedx.model.vulnerability.Rating;
import org.cyclonedx.model.vulnerability.Vulnerability10;
import org.cyclonedx.model.vulnerability.Vulnerability10.Advisory;
import org.cyclonedx.model.vulnerability.Vulnerability10.Recommendation;
import org.cyclonedx.model.vulnerability.Vulnerability10.Source;
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
          return generateFilteredSbom(sourceBom.getSpecVersion(), targetBom);
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
      ThirdPartyScanContent content,
      Bom generateBomFromFile,
      Bom targetBom,
      ThirdPartyFile thirdPartyFile) throws MalformedPackageURLException
  {
    final Map<String, String> hashFileCoordinateIdMap = new HashMap<>();
    String identificationSource = getTruncatedIdentificationSource(determineIdentificationSource(content.getPath()));
    try (TransactionContext tx = thirdPartyFileDAO.createTransactionContext()) {
      tx.begin();
      for (Component component : generateBomFromFile.getComponents()) {
        processComponent(component, thirdPartyFile.getId(), targetBom, hashFileCoordinateIdMap, identificationSource,
            tx);
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
      Component component,
      String thirdPartyFileId,
      Bom targetBom,
      Map<String, String> hashFileCoordinateIdMap,
      String identificationSource,
      TransactionContext tx) throws MalformedPackageURLException
  {
    String packageUrl = component.getPurl();
    try {
      if (StringUtils.isNotBlank(packageUrl)) {
        PackageUrlIdentifier packageUrlIdentifier = new PackageUrlIdentifier(packageUrl);
        if (StringUtils.isNoneBlank(packageUrlIdentifier.getName(), packageUrlIdentifier.getVersion())) {
          processPurlComponent(component, packageUrlIdentifier, thirdPartyFileId, targetBom,
              hashFileCoordinateIdMap, identificationSource, tx);
        }
        else {
          log.warn("PackageUrl is not valid {}", packageUrl);
          processComponentFromHashOrCoordinates(thirdPartyFileId, targetBom, hashFileCoordinateIdMap,
              identificationSource, tx, component);
        }
      }
      else {
        processComponentFromHashOrCoordinates(thirdPartyFileId, targetBom, hashFileCoordinateIdMap,
            identificationSource, tx, component);
      }
    }
    catch (InvalidPackageURLException e) {
      log.warn("Fallback to coordinates due to invalid purl: {}", packageUrl);
      processComponentFromHashOrCoordinates(thirdPartyFileId, targetBom, hashFileCoordinateIdMap, identificationSource,
          tx, component);
    }
  }

  private void processComponentFromHashOrCoordinates(
      final String thirdPartyFileId,
      final Bom targetBom,
      final Map<String, String> hashFileCoordinateIdMap,
      final String identificationSource,
      final TransactionContext tx,
      final Component component) throws MalformedPackageURLException
  {
    String name = StringUtils.isNotBlank(component.getName()) ? component.getName() : MISSING_COMPONENT_NAME;
    String version = component.getVersion();
    Component sbomComponent = createComponent(component, name, version);

    if (StringUtils.isNotBlank(version)) {
      String sha1 = getSha1(component);
      if (StringUtils.isNotBlank(sha1)) {
        processSha1Component(sbomComponent,
            StringUtils.truncate(sha1, 0, HashHelper.MAX_LENGTH), targetBom);
      }
      else {
        PackageUrlIdentifier packageUrlIdentifier =
            new PackageUrlIdentifier(getPackageUrlFromCoordinates(component, name));
        processPurlComponent(sbomComponent, packageUrlIdentifier, thirdPartyFileId, targetBom,
            hashFileCoordinateIdMap, identificationSource, tx);
      }
    }
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
    Component sbomComponent =
        createComponent(component, packageUrlIdentifier.getName(), packageUrlIdentifier.getVersion());
    sbomComponent.setPurl(ThirdPartyScanResultUtils.getTruncatedPurl(packageUrlIdentifier.getPackageUrl()));
    saveComponent(thirdPartyFileId, hashFileCoordinateIdMap, componentIdentifier, identificationSource, targetBom,
        sbomComponent, component, tx);
  }

  private Component createComponent(Component component, String name, String version) {
    Component sbomComponent = new Component();
    sbomComponent.setType(component.getType());
    sbomComponent.setName(name);
    sbomComponent.setVersion(version);
    return sbomComponent;
  }

  private void processSha1Component(Component sbomComponent, String sha1, Bom targetBom) {
    sbomComponent.setHashes(Collections.singletonList(new Hash(Algorithm.SHA1, sha1)));
    targetBom.addComponent(sbomComponent);
  }

  private ComponentIdentifier resolveComponentIdentifier(PackageUrlIdentifier packageUrlIdentifier) {
    PackageURLBuilder packageURLBuilder = PackageURLBuilder.aPackageURL();
    packageURLBuilder.withType(ThirdPartyScanResultUtils.getValidFormat(packageUrlIdentifier.getFormat()));
    packageURLBuilder.withName(ThirdPartyScanResultUtils.getTruncatedName(packageUrlIdentifier.getName()));
    packageURLBuilder.withVersion(ThirdPartyScanResultUtils.getTruncatedVersion(packageUrlIdentifier.getVersion()));

    if (packageUrlIdentifier.getNamespace() != null) {
      packageURLBuilder.withNamespace(packageUrlIdentifier.getNamespace());
    }

    Map<String, String> qualifiers = packageUrlIdentifier.getQualifiers();
    for (Entry<String, String> entry : qualifiers.entrySet()) {
      packageURLBuilder.withQualifier(entry.getKey(), entry.getValue());
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
      Component sbomComponent,
      Component component,
      TransactionContext tx)
  {
    String fakeHash = ThirdPartyScanResultUtils.hash(
        componentIdentifier.getFormat() + ":" + StringUtils.join(componentIdentifier.getCoordinates().values(), ":"));
    if (!hashFileCoordinateIdMap.containsKey(fakeHash)) {
      ThirdPartyFileCoordinate fileCoordinate = new ThirdPartyFileCoordinate(fakeHash, identificationSource,
          componentIdentifier.getFormat(), sbomComponent.getName(), sbomComponent.getVersion(), thirdPartyFileId);
      fileCoordinate.setPackageUrl(sbomComponent.getPurl());
      thirdPartyFileCoordinateDAO.insert(tx, fileCoordinate);
      hashFileCoordinateIdMap.put(fakeHash, fileCoordinate.getId());
      saveLicenses(component.getLicenseChoice(), fileCoordinate.getId(), sbomComponent.getPurl(), tx);
      saveVulnerabilities(component.getExtensions(), fileCoordinate.getId(), tx);
      targetBom.addComponent(sbomComponent);
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

  private String generateFilteredSbom(String specVersion, Bom sbom)
      throws ParserConfigurationException, GeneratorException
  {
    BomXmlGenerator generator =
        BomGeneratorFactory.createXml(ThirdPartyUtils.CYCLONEDX_ACCEPTED_VERSIONS.get(specVersion), sbom);
    generator.generate();
    return generator.toXmlString();
  }
}
