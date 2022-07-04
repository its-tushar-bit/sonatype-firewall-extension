/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.purl.InvalidPackageURLException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.model.ProjectScanItem;
import com.sonatype.insight.util.SbomUtils;

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
import org.cyclonedx.BomGeneratorFactory;
import org.cyclonedx.CycloneDxSchema.Version;
import org.cyclonedx.exception.GeneratorException;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.generators.xml.BomXmlGenerator;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.BomReference;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.ExtensibleType;
import org.cyclonedx.model.Extension;
import org.cyclonedx.model.License;
import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.vulnerability.Rating;
import org.cyclonedx.model.vulnerability.Vulnerability;
import org.cyclonedx.model.vulnerability.Vulnerability.Affect;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating.Method;
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

  private final MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();

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
        log.info("Processing SBOM content for file: {}", content.getPath());
        processSbom(content.getPath(), sourceBom, targetBom, thirdPartyFile, moduleDependencies);
        if (targetBom.getComponents() != null && targetBom.getComponents().isEmpty()) {
          return new FilteredThirdPartyContent(content.getContent(), moduleDependencies);
        }
        else {
          return new FilteredThirdPartyContent(generateFilteredSbom(targetBom), moduleDependencies);
        }
      }
      return new FilteredThirdPartyContent(content.getContent());
    }
    catch (Exception e) {
      throw new RuntimeException("Error filtering sbom file " + content.getPath(), e);
    }
  }

  //visible for testing
  Bom parseBom(final ThirdPartyScanContent content) throws ParseException, IOException {
    String extension = FilenameUtils.getExtension(content.getPath());
    return ThirdPartyUtils.parseAndValidateSbom(content.getContent(), extension);
  }

  void processSbom(
      final String contentPath,
      final Bom sourceBom,
      final Bom targetBom,
      final ThirdPartyFile thirdPartyFile,
      final List<ProjectScanItem> dependencyGraph)
  {
    String identificationSource = getTruncatedIdentificationSource(determineIdentificationSource(contentPath));
    try (TransactionContext tx = thirdPartyFileDAO.createTransactionContext()) {
      tx.begin();
      targetBom.setMetadata(getFilteredMetadata(sourceBom));

      Map<String, String> componentRefs = new HashMap<>();
      processComponents(sourceBom, targetBom, componentRefs, identificationSource, thirdPartyFile, tx);
      processVulnerabilities(sourceBom, targetBom, componentRefs, tx);
      tx.commit();
    }
    processDependencyGraph(sourceBom, targetBom, dependencyGraph, thirdPartyFile);
  }

  private void processComponents(
      final Bom sourceBom,
      final Bom targetBom,
      final Map<String, String> componentRefs,
      final String identificationSource,
      final ThirdPartyFile thirdPartyFile,
      final TransactionContext tx)
  {
    if (CollectionUtils.isNotEmpty(sourceBom.getComponents())) {
      String specVersion = sourceBom.getSpecVersion();
      Set<ComponentIdentifier> resolvedComponents = new HashSet<>();
      for (Component component : sourceBom.getComponents()) {
        processComponent(component, thirdPartyFile.getId(), targetBom, identificationSource, resolvedComponents,
            componentRefs, specVersion, tx);
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
        CollectionUtils.isNotEmpty(sourceBom.getVulnerabilities()) && !componentRefs.isEmpty()) {
      for (Vulnerability vulnerability : sourceBom.getVulnerabilities()) {
        try {
          List<Affect> affects = vulnerability.getAffects();
          for (Affect affect : affects) {
            if (StringUtils.isNotBlank(affect.getRef()) && componentRefs.containsKey(affect.getRef())) {
              String fileCoordinateId = componentRefs.get(affect.getRef());
              saveVulnerability(vulnerability, fileCoordinateId, tx);
            }
            else {
              log.debug("Vulnerability with ID {} does not have a " + (StringUtils.isBlank(affect.getRef()) ? "ref" :
                      "matching component") + " so it can't be parsed", vulnerability.getId());
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
      thirdPartyCoordinateSecurityDAO.insert(tx, coordinateSecurity);
    }
  }

  //visible for testing
  String determineIdentificationSource(final String contentPath) {
    String fileName = StringUtils.contains(contentPath, "/") ?
        StringUtils.substringAfterLast(contentPath, "/") : contentPath;
    String identificationSource = RegExUtils.removePattern(fileName, "-(?i)bom\\.(xml|json)(?i)$");
    if (StringUtils.isBlank(identificationSource) || StringUtils.endsWithIgnoreCase(identificationSource, "bom.xml") ||
        StringUtils.endsWithIgnoreCase(identificationSource, "bom.json")) {
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
      final Map<String, String> componentRefs,
      final String schemaVersion,
      final TransactionContext tx)
  {
    try {
      Pair<ComponentIdentifier, Component> resolvedComponent = getResolvedComponent(sourceComponent);
      if (resolvedComponent != null) {
        ComponentIdentifier componentIdentifier = resolvedComponent.getLeft();
        if (componentIdentifier == null) {
          targetBom.addComponent(resolvedComponent.getRight());
        }
        else if (resolvedComponents.add(componentIdentifier)) {
          PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).ensureCompleteIdentifier();
          String coordinateId =
              saveComponent(thirdPartyFileId, identificationSource, sourceComponent, resolvedComponent, schemaVersion,
                  tx);
          if (StringUtils.isNotBlank(sourceComponent.getBomRef())) {
            componentRefs.put(sourceComponent.getBomRef(), coordinateId);
          }
          targetBom.addComponent(resolvedComponent.getRight());
        }
      }
    }
    catch (InvalidPackageURLException e) {
      log.debug("Component {} {} is missing coordinates. " + e.getMessage().replace(" for given format", ""),
          sourceComponent.getName(), sourceComponent.getVersion(), e);
    }
    catch (Exception e) {
      log.debug("Error processing component : {} {}", sourceComponent.getName(), sourceComponent.getVersion(), e);
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
          log.debug("PackageUrl is not valid {}", packageUrl);
        }
      }
    }
    catch (InvalidPackageURLException e) {
      log.debug("Fallback to coordinates due to invalid purl: {}", packageUrl);
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
      String sha1 = SbomUtils.getSha1(sourceComponent);
      if (StringUtils.isNotBlank(sha1)) {
        Component component = new Component();
        component.setType(sourceComponent.getType());
        component.setBomRef(sourceComponent.getBomRef());
        setHash(sha1, component);
        return Pair.of(null, component);
      }
      else {
        log.debug("Component with invalid information, name {} and version {}", sourceComponent.getName(),
            sourceComponent.getVersion());
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

  private Pair<ComponentIdentifier, Component> createComponent(
      final Component sourceComponent,
      final PackageUrlIdentifier packageUrlIdentifier,
      final boolean coordinates)
  {
    ComponentIdentifier componentIdentifier;
    Component component = new Component();
    component.setType(sourceComponent.getType());
    component.setBomRef(sourceComponent.getBomRef());

    String sha1 = SbomUtils.getSha1(sourceComponent);
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
    String namespace = packageUrlIdentifier.getNamespace();
    if (StringUtils.isNotBlank(namespace)) {
      component.setGroup(namespace);
    }

    return Pair.of(componentIdentifier, component);
  }

  private void setHash(final String sha1, final Component component) {
    Property property = new Property();
    property.setName(SbomUtils.SONATYPE_HASH_PROPERTY_NAME);
    property.setValue(StringUtils.truncate(sha1, 0, HashHelper.MAX_LENGTH));
    component.addProperty(property);
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

      if (sourcePurl.getFormat().equalsIgnoreCase(ComponentIdentifier.FORMAT_MAVEN) &&
          StringUtils.isBlank(qualifiers.get(PackageUrlIdentifier.PURL_MAVEN_EXTENSION))) {
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
      final String identificationSource,
      final Component sourceComponent,
      final Pair<ComponentIdentifier, Component> resolvedComponent,
      final String schemaVersion,
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
    saveVulnerabilitiesExtension(sourceComponent.getExtensions(), fileCoordinate.getId(), schemaVersion, tx);
    return fileCoordinate.getId();
  }

  private void saveVulnerabilitiesExtension(
      final Map<String, Extension> extensions,
      final String fileCoordinateId,
      final String bomVersion,
      final TransactionContext tx)
  {
    //Vulnerability extension is unsupported from 1.4+
    if (MapUtils.isNotEmpty(extensions) &&
        (!Version.VERSION_14.getVersionString().equals(bomVersion) || StringUtils.isBlank(bomVersion))) {
      Extension vulnerabilityExtension = extensions.get(VULNERABILITY_KEY);
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
    if (extensibleType instanceof Vulnerability10) {
      Vulnerability10 vulnerability = (Vulnerability10) extensibleType;
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
      thirdPartyCoordinateSecurityDAO.insert(tx, coordinateSecurity);
    }
  }

  //Visible for testing
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
                  .collect(Collectors.joining(ThirdPartyVulnerabilityDataAdapter.LIST_SEPARATOR)));
        }
        if (vulnerability.getRecommendations() != null) {
          coordinateSecurity.setRecommendations(
              vulnerability.getRecommendations().stream().map(Recommendation::getText)
                  .collect(Collectors.joining(ThirdPartyVulnerabilityDataAdapter.LIST_SEPARATOR)));
        }
        if (vulnerability.getAdvisories() != null) {
          coordinateSecurity.setAdvisories(
              vulnerability.getAdvisories().stream().map(Advisory::getText)
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
        coordinateSecurity.setDescription(vulnerability.getDescription());
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
          coordinateSecurity.setSeverity(baseScore.floatValue());
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
            coordinateSecurity.setCwes(vulnerability.getCwes().stream().filter(Objects::nonNull).map(Object::toString)
                .collect(Collectors.joining(ThirdPartyVulnerabilityDataAdapter.LIST_SEPARATOR)));
          }
          if (vulnerability.getRecommendation() != null) {
            coordinateSecurity.setRecommendations(vulnerability.getRecommendation());
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
          coordinateSecurity.setDescription(vulnerability.getDescription());
          return coordinateSecurity;
        }
      }
    }
    else {
      log.debug("Vulnerability with ID {} does not have a valid rating, it can't be parsed", vulnerability.getId());
    }
    return null;
  }

  Vulnerability.Rating getValidRating(List<Vulnerability.Rating> ratings) {
    Vulnerability.Rating validRating = null;
    for (Vulnerability.Rating rating : ratings) {
      if (rating.getScore() != null) {
        Vulnerability.Source source = rating.getSource();
        if (source != null && StringUtils.isNotBlank(source.getName())) {
          validRating = rating;
          if (source.getName().toLowerCase(Locale.ROOT).equals("nvd") &&
              (rating.getMethod() == Method.CVSSV31 || rating.getMethod() == Method.CVSSV3)) {
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
      if (CollectionUtils.isEmpty(licenseChoice.getLicenses())) {
        log.debug("Found empty licenses element for Component with packageUrl {}", packageUrl);
      }
      else {
        Set<String> processedLicenseIds = new HashSet<>();
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
    thirdPartyCoordinateLicenseDAO.insert(tx, coordinateLicense);
  }

  //visible for testing
  void processDependencyGraph(
      final Bom sourceBom,
      final Bom targetBom,
      final List<ProjectScanItem> moduleDependencies,
      final ThirdPartyFile thirdPartyFile)
  {
    try {
      if (CollectionUtils.isNotEmpty(targetBom.getComponents())) {
        List<Dependency> dependencies = sourceBom.getDependencies();
        if (CollectionUtils.isNotEmpty(dependencies)) {
          Iterator<Dependency> dependencyItr = dependencies.iterator();
          Dependency rootModule = dependencyItr.next();
          String moduleRef = resolveModuleRef(rootModule, targetBom);
          if (moduleRef != null) {
            processValidDependencyGraph(moduleRef, thirdPartyFile, rootModule, targetBom, dependencyItr,
                moduleDependencies, dependencies);
          }
          else {
            log.debug(String.format("Unable to process dependency graph. " +
                "The root component of the bom %s cannot be determined", thirdPartyFile.getFilename()));
          }
        }
      }
    }
    catch (Exception e) {
      log.warn("There was an error processing dependency graph", e);
    }
  }

  private void processValidDependencyGraph(
      final String moduleRef,
      final ThirdPartyFile thirdPartyFile,
      final Dependency rootModule,
      final Bom targetBom,
      final Iterator<Dependency> dependencyItr,
      final List<ProjectScanItem> moduleDependencies,
      final List<Dependency> dependencies)
  {
    Map<String, Pair<Boolean, List<com.sonatype.insight.scan.model.Dependency>>> dependencyGraph =
        new HashMap<>();
    ProjectScanItem project = new ProjectScanItem("sbom", moduleRef);
    project.setPath(thirdPartyFile.getFilename());

    if (CollectionUtils.isNotEmpty(rootModule.getDependencies())) {
      Map<String, String> bomRefsToPurls = new HashMap<>();
      //direct dependencies
      for (Dependency dependency : rootModule.getDependencies()) {
        String ref = getPurlForDependency(dependency, bomRefsToPurls, targetBom);
        if (ref != null) {
          dependencyGraph.put(ref, Pair.of(true, new ArrayList<>()));
        }
      }
      Map<String, Dependency> dependencyMap =
          dependencies.stream().collect(Collectors.toMap(BomReference::getRef, dep -> dep, (d1, d2) -> d1));

      resolveSbomDependenciesAndTypes(thirdPartyFile, dependencyItr, dependencyGraph, bomRefsToPurls, targetBom,
          dependencyMap);
      constructProjectDependencyGraph(dependencyGraph, project);
      moduleDependencies.add(project);
    }
  }

  private String resolveModuleRef(final Dependency firstDep, final Bom targetBom) {
    // Check we have the first dep ref to compare
    if (StringUtils.isBlank(firstDep.getRef())) {
      return null;
    }

    // Check we have the metadata component to compare
    if (targetBom.getMetadata() == null) {
      return null;
    }
    Component metadataComponent = targetBom.getMetadata().getComponent();
    if (metadataComponent == null) {
      return null;
    }

    // If the first dep ref matches the metadata component purl just return the latter
    if (firstDep.getRef().equalsIgnoreCase(metadataComponent.getPurl())) {
      return resolvePackageUrl(metadataComponent.getPurl()).getPackageUrl();
    }
    // Otherwise, if the first dep ref matches the metadata component bom ref (it could be a purl or a UUID)
    if (firstDep.getRef().equalsIgnoreCase(metadataComponent.getBomRef())) {
      // Return the metadata component purl if it exists
      if (StringUtils.isNotBlank(metadataComponent.getPurl())) {
        return resolvePackageUrl(metadataComponent.getPurl()).getPackageUrl();
      }
      // Otherwise, just return the first dep ref
      if (isPurl(firstDep.getRef())) {
        return resolvePackageUrl(firstDep.getRef()).getPackageUrl();
      }
      return firstDep.getRef();
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
    //making sure we copy only identity data and nothing else
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
        builder.withType(ComponentIdentifier.FORMAT_GENERIC).withName(componentSource.getName())
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
    for (Entry<String, Pair<Boolean, List<com.sonatype.insight.scan.model.Dependency>>> depEntry :
        dependencyGraph.entrySet()) {
      com.sonatype.insight.scan.model.Dependency dependency = new com.sonatype.insight.scan.model.Dependency();
      dependency.setId(depEntry.getKey());
      dependency.setDirect(depEntry.getValue().getLeft());
      depEntry.getValue().getRight().forEach(dependency::addDependency);
      project.addDependency(dependency);
    }
  }

  private void resolveSbomDependenciesAndTypes(
      final ThirdPartyFile thirdPartyFile,
      final Iterator<Dependency> dependencyItr,
      final Map<String, Pair<Boolean, List<com.sonatype.insight.scan.model.Dependency>>> dependencyGraph,
      final Map<String, String> bomRefPurlMap,
      final Bom targetBom,
      final Map<String, Dependency> fullDependencyMap)
  {
    dependencyItr.forEachRemaining(dependency -> {
      String ref = getPurlForDependency(dependency, bomRefPurlMap, targetBom);
      Pair<Boolean, List<com.sonatype.insight.scan.model.Dependency>> dependencyPair =
          getProjectDependencyForRef(ref, dependencyGraph, fullDependencyMap);
      if (dependencyPair != null) {
        copyChildDependenciesForDependency(thirdPartyFile, dependencyGraph, dependency, dependencyPair, bomRefPurlMap,
            targetBom);
      }
      else {
        log.debug(String.format(
            "Unsupported dependency graph in sbom. Missing parent reference for the child dependency %s",
            dependency.getRef()));
      }
    });
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
      final Dependency dependency,
      final Pair<Boolean, List<com.sonatype.insight.scan.model.Dependency>> dependencyPair,
      final Map<String, String> bomRefsToPurls,
      final Bom targetBom)
  {
    IterableUtils.forEach(dependency.getDependencies(), bomChild -> {
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
        log.debug(
            String.format("invalid purl dependency %s in bom %s", bomChild.getRef(), thirdPartyFile.getFilename()));
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

  String generateFilteredSbom(Bom sbom)
      throws ParserConfigurationException, GeneratorException
  {
    BomXmlGenerator generator = BomGeneratorFactory.createXml(Version.VERSION_14, sbom);
    generator.generate();
    return generator.toXmlString();
  }
}
