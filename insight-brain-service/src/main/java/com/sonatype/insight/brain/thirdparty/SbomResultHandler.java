/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.utils.Xpp3Util;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.purl.InvalidPackageURLException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.cyclonedx.BomGenerator;
import org.cyclonedx.BomGeneratorFactory;
import org.cyclonedx.CycloneDxSchema.Version;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SbomResultHandler
    implements ThirdPartyScanResultHandler
{
  private static final Logger log = LoggerFactory.getLogger(SbomResultHandler.class);

  private final ThirdPartyFileDAO thirdPartyFileDAO = new ThirdPartyFileDAO();

  private final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO = new ThirdPartyFileCoordinateDAO();

  private final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO = new ThirdPartyCoordinateSecurityDAO();

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
    XmlPullParser parser = new MXParser();
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
    parser.setInput(new StringReader(content.getContent()));

    String identificationSource = determineIdentificationSource(content.getPath());
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
      Xpp3Dom packageUrl = component.getChild("purl");
      if (packageUrl != null && !StringUtils.isBlank(packageUrl.getValue())) {
        processPurlComponent(component, packageUrl, thirdPartyFileId, sbom, hashFileCoordinateIdMap,
            identificationSource, tx);
      }
    }
    catch (InvalidPackageURLException e) {
      log.error("Error processing SBOM component, invalid purl", e);
    }
    catch (Exception e) {
      log.error("Error processing SBOM component", e);
    }
  }

  private void processPurlComponent(
      Xpp3Dom component,
      Xpp3Dom packageUrl,
      String thirdPartyFileId,
      Bom sbom,
      Map<String, String> hashFileCoordinateIdMap,
      String identificationSource,
      TransactionContext tx)
  {
    PackageUrlIdentifier packageUrlIdentifier = new PackageUrlIdentifier(packageUrl.getValue());

    if (StringUtils.isNoneBlank(packageUrlIdentifier.getName(), packageUrlIdentifier.getVersion())) {
      ComponentIdentifier componentIdentifier = getComponentIdentifier(packageUrlIdentifier);
      Component sbomComponent = new Component();
      sbomComponent.setType(Component.Type.valueOf(component.getAttribute("type").toUpperCase()));
      sbomComponent.setName(packageUrlIdentifier.getName());
      sbomComponent.setVersion(packageUrlIdentifier.getVersion());
      sbomComponent.setPurl(packageUrl.getValue());
      sbomComponent.setName(component.getChild("name").getValue());
      String fileCoordinateId = saveComponent(thirdPartyFileId, hashFileCoordinateIdMap, componentIdentifier,
          packageUrlIdentifier, packageUrl.getValue(), identificationSource, tx);
      saveVulnerabilities(component.getChild("vulnerabilities"), fileCoordinateId, tx);
      sbom.addComponent(sbomComponent);
    }
    else {
      log.error("PackageUrl is not valid {}", packageUrl.getValue());
    }
  }

  private String saveComponent(
      String thirdPartyFileId,
      Map<String, String> hashFileCoordinateIdMap,
      ComponentIdentifier componentIdentifier,
      PackageUrlIdentifier packageUrlIdentifier,
      String purl,
      String identificationSource,
      TransactionContext tx)
  {
    String fakeHash =
        ThirdPartyScanResultUtils.hash(componentIdentifier.getFormat() + ":"
            + StringUtils.join(componentIdentifier.getCoordinates().values(), ":"));
    if (!hashFileCoordinateIdMap.containsKey(fakeHash)) {
      ThirdPartyFileCoordinate fileCoordinate = new ThirdPartyFileCoordinate(fakeHash, identificationSource,
          componentIdentifier.getFormat(), packageUrlIdentifier.getName(), packageUrlIdentifier.getVersion(),
          thirdPartyFileId);
      fileCoordinate.setPackageUrl(purl);
      thirdPartyFileCoordinateDAO.insert(tx, fileCoordinate);
      hashFileCoordinateIdMap.put(fakeHash, fileCoordinate.getId());
      return fileCoordinate.getId();
    }
    return hashFileCoordinateIdMap.get(fakeHash);
  }

  private ComponentIdentifier getComponentIdentifier(
      PackageUrlIdentifier packageUrlIdentifier) throws InvalidPackageURLException
  {
    try {
      return packageUrlIdentifier.toComponentIdentifier();
    }
    catch (InvalidComponentIdentifierException e) {
      log.debug("Fallback to identification using generic component identifier");
      return packageUrlIdentifier.toGenericComponentIdentifier();
    }
  }

  private void saveVulnerabilities(Xpp3Dom vulnerabilities, String fileCoordinateId, TransactionContext tx) {
    Set<String> vulnerabilityMap = new HashSet<>();
    if (vulnerabilities != null) {
      for (Xpp3Dom vulnerability : vulnerabilities.getChildren()) {
        if (vulnerability != null) {
          String refId = vulnerability.getChild("id").getValue();
          if (!vulnerabilityMap.contains(refId)) {
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
    coordinateSecurity.setFileCoordinateId(fileCoordinateId);
    Xpp3Dom cwes = vulnerability.getChild("cwes");
    coordinateSecurity.setCwes(getList(cwes));

    Xpp3Dom recommendations = vulnerability.getChild("recommendations");
    coordinateSecurity.setRecommendations(getList(recommendations));

    Xpp3Dom advisories = vulnerability.getChild("advisories");
    coordinateSecurity.setAdvisories(getList(advisories));

    Xpp3Dom ratingsElements = vulnerability.getChild("ratings");
    if (ratingsElements != null) {
      Xpp3Dom[] ratings = ratingsElements.getChildren();
      if (ratings != null && ratings.length > 0) {
        Xpp3Dom rating = ratings[0];

        coordinateSecurity.setAttackVector(getValueFromTag(rating, "vector"));
        coordinateSecurity.setRatingMethod(getValueFromTag(rating, "method"));
        coordinateSecurity.setSeverityDescription(getValueFromTag(rating, "severity"));

        if (rating.getChild("score") != null && rating.getChild("score").getChild("base") != null) {
          float severity = Float.parseFloat(rating.getChild("score").getChild("base").getValue());
          coordinateSecurity.setSeverity(severity);
          validVulnerability = true;
        }
      }
    }

    Xpp3Dom source = vulnerability.getChild("source");
    if (source != null) {
      coordinateSecurity.setVulnerabilitySource(source.getAttribute("name"));
      coordinateSecurity.setLink(getValueFromTag(source, "url"));
    }
    coordinateSecurity.setRefId(refId);
    coordinateSecurity.setDescription(getValueFromTag(vulnerability, "description"));
    if (validVulnerability) {
      thirdPartyCoordinateSecurityDAO.insert(tx, coordinateSecurity);
    }
  }

  private String getValueFromTag(Xpp3Dom element, String name) {
    if (element.getChild(name) != null) {
      return element.getChild(name).getValue();
    }
    return null;
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
