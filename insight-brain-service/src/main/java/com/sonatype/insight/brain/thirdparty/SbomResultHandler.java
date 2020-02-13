/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.utils.Xpp3Util;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.purl.InvalidPackageURLException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.github.packageurl.PackageURLBuilder;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.cyclonedx.BomGenerator;
import org.cyclonedx.BomGeneratorFactory;
import org.cyclonedx.CycloneDxSchema.Version;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Hash;
import org.cyclonedx.model.Hash.Algorithm;
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

  private final ThirdPartyFileDAO thirdPartyFileDAO = new ThirdPartyFileDAO();

  private final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO = new ThirdPartyFileCoordinateDAO();

  private final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO = new ThirdPartyCoordinateSecurityDAO();

  private final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO = new ThirdPartyCoordinateLicenseDAO();

  @Override
  public String handleAndFilterContents(ThirdPartyScanContent content, ThirdPartyFile thirdPartyFile) {
    try {
      if (!StringUtils.isBlank(content.getContent())) {
        Bom sbom = new Bom();
        log.info("Processing SBOM content");
        processSbom(content, sbom, thirdPartyFile);

        if (sbom.getComponents() != null && sbom.getComponents().isEmpty()) {
          return content.getContent();
        }
        else {
          return generateFilteredSbom(sbom);
        }
      }
      return content.getContent();
    }
    catch (Exception e) {
      throw new RuntimeException("Error filtering sbom file", e);
    }
  }

  private void processSbom(
      ThirdPartyScanContent content,
      Bom sbom,
      ThirdPartyFile thirdPartyFile) throws XmlPullParserException, IOException
  {
    final Map<String, String> hashFileCoordinateIdMap = new HashMap<>();
    Stack<String> elementNameStack = new Stack<>();
    XmlPullParser parser = ThirdPartyUtils.getXmlParser(new StringReader(content.getContent()));

    String identificationSource = getTruncatedIdentificationSource(determineIdentificationSource(content.getPath()));
    try (TransactionContext tx = thirdPartyFileDAO.createTransactionContext()) {
      tx.begin();
      int eventType = parser.getEventType();
      while (eventType != XmlPullParser.END_DOCUMENT) {
        if (eventType == XmlPullParser.START_TAG) {
          String elementName = parser.getName();
          if ("component".equals(elementName)) {
            processComponent(parser, thirdPartyFile.getId(), sbom, hashFileCoordinateIdMap, identificationSource, tx);
          }
          else {
            elementNameStack.push(elementName);
          }
        }
        else if (eventType == XmlPullParser.END_TAG) {
          String beginName = elementNameStack.pop();
          String endName = parser.getName();
          if (!beginName.equals(endName)) {
            throw new XmlPullParserException("End tag '" + endName + "' does not match start tag '" + beginName + "'.");
          }
        }
        eventType = parser.next();
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
      XmlPullParser parser,
      String thirdPartyFileId,
      Bom sbom,
      Map<String, String> hashFileCoordinateIdMap,
      String identificationSource,
      TransactionContext tx)
  {
    try {
      Xpp3Dom component = Xpp3Util.loadElement("component", parser);
      processComponent(component, thirdPartyFileId, sbom, hashFileCoordinateIdMap, identificationSource, tx);
    }
    catch (InvalidPackageURLException | InvalidComponentIdentifierException e) {
      log.error("Error processing SBOM component, invalid purl", e);
    }
    catch (Exception e) {
      log.error("Error processing SBOM component", e);
    }
  }

  private void processComponent(
      Xpp3Dom component,
      String thirdPartyFileId,
      Bom sbom,
      Map<String, String> hashFileCoordinateIdMap,
      String identificationSource,
      TransactionContext tx) throws MalformedPackageURLException
  {
    String packageUrl = getValueFromTag(component, "purl");
    try {
      if (StringUtils.isNotBlank(packageUrl)) {
        PackageUrlIdentifier packageUrlIdentifier = new PackageUrlIdentifier(packageUrl);
        if (StringUtils.isNoneBlank(packageUrlIdentifier.getName(), packageUrlIdentifier.getVersion())) {
          processPurlComponent(component, packageUrlIdentifier, thirdPartyFileId, sbom,
              hashFileCoordinateIdMap, identificationSource, tx);
        }
        else {
          log.warn("PackageUrl is not valid {}", packageUrl);
          processComponentFromHashOrCoordinates(thirdPartyFileId, sbom, hashFileCoordinateIdMap, identificationSource,
              tx, component);
        }
      }
      else {
        processComponentFromHashOrCoordinates(thirdPartyFileId, sbom, hashFileCoordinateIdMap, identificationSource, tx,
            component);
      }
    }
    catch (InvalidPackageURLException e) {
      log.warn("Fallback to coordinates due to invalid purl: {}", packageUrl);
      processComponentFromHashOrCoordinates(thirdPartyFileId, sbom, hashFileCoordinateIdMap, identificationSource, tx,
          component);
    }
  }

  private void processComponentFromHashOrCoordinates(
      final String thirdPartyFileId,
      final Bom sbom,
      final Map<String, String> hashFileCoordinateIdMap,
      final String identificationSource,
      final TransactionContext tx,
      final Xpp3Dom component) throws MalformedPackageURLException
  {
    String name = getValueFromTag(component, "name");
    String version = getValueFromTag(component, "version");

    if (StringUtils.isNoneBlank(name, version)) {
      String sha1 = getSha1(component);
      if (StringUtils.isNotBlank(sha1)) {
        processSha1Component(createComponent(component, name, version),
            StringUtils.truncate(sha1, 0, HashHelper.MAX_LENGTH), sbom);
      }
      else {
        PackageUrlIdentifier packageUrlIdentifier =
            new PackageUrlIdentifier(getPackageUrlFromCoordinates(component, name, version));
        processPurlComponent(component, packageUrlIdentifier, thirdPartyFileId, sbom,
            hashFileCoordinateIdMap, identificationSource, tx);
      }
    }
  }

  private String getPackageUrlFromCoordinates(Xpp3Dom component, String name, String version)
      throws MalformedPackageURLException
  {
    String group = getValueFromTag(component, "group");
    String publisher = getValueFromTag(component, "publisher");

    PackageURLBuilder packageURLBuilder = PackageURLBuilder.aPackageURL()
        .withType(component.getAttribute("type"))
        .withName(name)
        .withVersion(version);
    if (StringUtils.isNotBlank(group)) {
      packageURLBuilder.withNamespace(group);
    }
    if (StringUtils.isNotBlank(publisher)) {
      packageURLBuilder.withQualifier("publisher", publisher);
    }
    return packageURLBuilder.build().toString();
  }

  private String getSha1(Xpp3Dom component) {
    List<Xpp3Dom> hashes = getValuesFromTag(component, "hashes");
    Xpp3Dom alg = hashes.stream().filter(h -> h.getAttribute("alg").equals("SHA-1")).findFirst().orElse(null);
    return alg != null ? alg.getValue() : null;
  }

  private void processPurlComponent(
      Xpp3Dom component,
      PackageUrlIdentifier packageUrlIdentifier,
      String thirdPartyFileId,
      Bom sbom,
      Map<String, String> hashFileCoordinateIdMap,
      String identificationSource,
      TransactionContext tx)
  {
    ComponentIdentifier componentIdentifier = resolveComponentIdentifier(packageUrlIdentifier);
    packageUrlIdentifier = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);
    Component sbomComponent =
        createComponent(component, packageUrlIdentifier.getName(), packageUrlIdentifier.getVersion());
    sbomComponent.setPurl(packageUrlIdentifier.getPackageUrl());
    saveComponent(thirdPartyFileId, hashFileCoordinateIdMap, componentIdentifier, packageUrlIdentifier,
        identificationSource, component, sbom, sbomComponent, tx);
  }

  private Component createComponent(Xpp3Dom component, String name, String version) {
    Component sbomComponent = new Component();
    sbomComponent.setType(Component.Type.valueOf(component.getAttribute("type").toUpperCase()));
    sbomComponent.setName(name);
    sbomComponent.setVersion(version);
    return sbomComponent;
  }

  private void processSha1Component(Component sbomComponent, String sha1, Bom sbom) {
    sbomComponent.setHashes(Arrays.asList(new Hash(Algorithm.SHA1, sha1)));
    sbom.addComponent(sbomComponent);
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
      PackageUrlIdentifier packageUrlIdentifier,
      String identificationSource,
      Xpp3Dom component,
      Bom sbom,
      Component sbomComponent,
      TransactionContext tx)
  {
    String fakeHash = ThirdPartyScanResultUtils.hash(
        componentIdentifier.getFormat() + ":" + StringUtils.join(componentIdentifier.getCoordinates().values(), ":"));
    if (!hashFileCoordinateIdMap.containsKey(fakeHash)) {
      ThirdPartyFileCoordinate fileCoordinate =
          new ThirdPartyFileCoordinate(fakeHash, identificationSource, componentIdentifier.getFormat(),
              packageUrlIdentifier.getName(), packageUrlIdentifier.getVersion(), thirdPartyFileId);
      fileCoordinate.setPackageUrl(packageUrlIdentifier.getPackageUrl());
      thirdPartyFileCoordinateDAO.insert(tx, fileCoordinate);
      hashFileCoordinateIdMap.put(fakeHash, fileCoordinate.getId());

      saveLicenses(component.getChild("licenses"), fileCoordinate.getId(), packageUrlIdentifier.getPackageUrl(), tx);
      saveVulnerabilities(component.getChild("vulnerabilities"), fileCoordinate.getId(), tx);
      sbom.addComponent(sbomComponent);
    }
  }

  private void saveVulnerabilities(Xpp3Dom vulnerabilities, String fileCoordinateId, TransactionContext tx) {
    Set<String> vulnerabilityMap = new HashSet<>();
    if (vulnerabilities != null) {
      for (Xpp3Dom vulnerability : vulnerabilities.getChildren()) {
        if (vulnerability != null) {
          String refId = vulnerability.getChild("id").getValue();
          if (StringUtils.isNotBlank(refId) && !vulnerabilityMap.contains(refId)) {
            saveVulnerability(vulnerability, fileCoordinateId, refId, tx);
            vulnerabilityMap.add(refId);
          }
        }
      }
    }
  }

  private void saveVulnerability(Xpp3Dom vulnerability, String fileCoordinateId, String refId, TransactionContext tx) {
    boolean validVulnerability = false;
    ThirdPartyCoordinateSecurity coordinateSecurity = new ThirdPartyCoordinateSecurity();

    Xpp3Dom ratingsElements = vulnerability.getChild("ratings");
    if (ratingsElements != null) {
      Xpp3Dom[] ratings = ratingsElements.getChildren();
      if (ratings != null && ratings.length > 0) {
        Xpp3Dom rating = ratings[0];

        coordinateSecurity.setAttackVector(getTruncatedAttackVector(getValueFromTag(rating, "vector")));
        coordinateSecurity.setRatingMethod(getTruncatedRatingMethod(getValueFromTag(rating, "method")));
        coordinateSecurity.setSeverityDescription(getTruncatedSeverityDescription(getValueFromTag(rating, "severity")));

        Xpp3Dom score = rating.getChild("score");
        if (score != null) {
          String severityValue = getValueFromTag(score, "base");
          if (StringUtils.isNotBlank(severityValue)) {
            float severity = Float.parseFloat(severityValue);
            coordinateSecurity.setSeverity(severity);
            validVulnerability = true;
          }
        }
      }
    }

    if (validVulnerability) {
      coordinateSecurity.setFileCoordinateId(fileCoordinateId);
      Xpp3Dom cwes = vulnerability.getChild("cwes");
      coordinateSecurity.setCwes(getList(cwes));

      Xpp3Dom recommendations = vulnerability.getChild("recommendations");
      coordinateSecurity.setRecommendations(getList(recommendations));

      Xpp3Dom advisories = vulnerability.getChild("advisories");
      coordinateSecurity.setAdvisories(getList(advisories));

      Xpp3Dom source = vulnerability.getChild("source");
      if (source != null) {
        coordinateSecurity.setVulnerabilitySource(getTruncatedVulnerabilitySource(source.getAttribute("name")));
        coordinateSecurity.setLink(getTruncatedLink(getValueFromTag(source, "url")));
      }
      coordinateSecurity.setRefId(getTruncatedRefId(refId));
      coordinateSecurity.setDescription(getValueFromTag(vulnerability, "description"));

      thirdPartyCoordinateSecurityDAO.insert(tx, coordinateSecurity);
    }
  }

  private void saveLicenses(
      Xpp3Dom licenses,
      String fileCoordinateId,
      String packageUrl,
      TransactionContext tx)
  {
    Set<String> licenseMap = new HashSet<>();
    if (licenses != null) {
      Xpp3Dom[] children = licenses.getChildren();
      if (children.length > 0 ) {
        for (Xpp3Dom license : licenses.getChildren()) {
          if (license != null) {
            Xpp3Dom licenseInfo = license.getChild("id");
            if (licenseInfo != null) {
              String licenseId = licenseInfo.getValue();
              if (StringUtils.isNotBlank(licenseId) && !licenseMap.contains(licenseId)) {
                saveLicense(license, fileCoordinateId, licenseId, tx);
                licenseMap.add(licenseId);
              }
            }
            else {
              log.debug("Component with packageUrl {} has license with null/empty ID", packageUrl);
            }
          }
        }
      }
      else {
        log.debug("Found empty licenses element for Component with packageUrl {}", packageUrl);
      }
    }
    else {
      log.debug("No licenses provided for Component with packageUrl {}", packageUrl);
    }
  }

  private void saveLicense(Xpp3Dom license, String fileCoordinateId, String licenseId, TransactionContext tx) {
    ThirdPartyCoordinateLicense coordinateLicense = new ThirdPartyCoordinateLicense();
    coordinateLicense.setFileCoordinateId(fileCoordinateId);

    coordinateLicense.setLicenseId(licenseId);
    coordinateLicense.setName(getValueFromTag(license, "name"));
    coordinateLicense.setUrl(getValueFromTag(license, "url"));
    thirdPartyCoordinateLicenseDAO.insert(tx, coordinateLicense);
  }

  private String getValueFromTag(Xpp3Dom element, String name) {
    if (element.getChild(name) != null) {
      return element.getChild(name).getValue();
    }
    return null;
  }

  private List<Xpp3Dom> getValuesFromTag(Xpp3Dom element, String name) {
    if (element.getChild(name) != null) {
      return Arrays.asList(element.getChild(name).getChildren());
    }
    return Collections.emptyList();
  }

  private String getList(Xpp3Dom element) {
    if (element != null) {
      StringBuilder list = new StringBuilder();
      for (Xpp3Dom child : element.getChildren()) {
        list.append(child.getValue()).append(ThirdPartySecurityVulnerabilityRenderer.LIST_SEPARATOR);
      }
      return list.toString();
    }
    return null;
  }

  private String generateFilteredSbom(Bom sbom) throws ParserConfigurationException, TransformerException {
    BomGenerator generator = BomGeneratorFactory.create(Version.VERSION_11, sbom);
    generator.generate();
    return generator.toXmlString();
  }
}
